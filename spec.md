# UL Fitness — Spec

**Version:** 0.3 — 2026-08-30  
**Status:** Draft (agreed stack: Kotlin Compose Multiplatform + Ktor + MariaDB, Tailscale LXC subnet router; RPE 1-10; 2 gyms + extensible; UI German, KG, `exercise_aliases`)  
**Repo:** `https://github.com/drepguy/ul-fitness.git` (`main`) — single-module `:app` today, to become `:shared` + `:composeApp` + `:server`

---

## 1. Summary

Personal, private fitness app to **track sets × reps × weight per exercise/machine** during workouts, with **RPE 1-10 + notes**, fast gym logging, and **progress visualisation**. No public hosting: data lives on **ai-vm (Ubuntu) MariaDB**, **Docker**, accessed **only via Tailscale tailnet** (existing LXC subnet advertiser). Multi-user capable from day one (you + future friends), offline-first on device.

Current reality: **2 gyms** — **Thomas Sport Center** (Upper Body) and **All Inclusive Fitness** (Legs + Core). User picks **gym → exercises/machines for that gym** (or creates new ones inline) → logs sets. Spec treats gyms first-class and extensible (N gyms), exercises/machines unified but filterable by gym.

Current codebase (`AGENTS.md:3`): AGP 9.3.2 / Gradle 9.5.0 / SDK 37 / JDK 25 (foojay), `MainActivity.java:5` dark splash (`ic_ul_logo.xml` + `UL FITNESS`), `BuildConfig` not yet KMP — greenfield for domain.

## 2. Goals / Non-Goals

**Goals**
- Log a set in <3 taps, one-handed, with sweaty fingers; rest timer keeps you moving.
- **Gym-aware logging:** choose gym at workout start, then picker shows only relevant exercises/machines for that gym (+ search globally); create new exercise/machine inline (with gym binding).
- Reliable offline: log without signal/VPN, sync when back on Tailnet.
- Honest progress charts (e1RM, volume, PRs) per exercise/machine, filterable by gym.
- Private by construction: API/DB never on public internet; VPN is the perimeter.
- Multi-user from schema day one (FK `user_id` everywhere, gyms/exercises per user or system).

**Non-Goals**
- Public distribution, App-Store compliance, analytics, ads.
- WearOS, Health Connect, or third-party sync (Strava) in v1.
- Real-time collaboration, social feed, trainer market.
- Automated plate/rep detection (camera).

## 3. Users

- **Owner (you):** daily driver, defines custom exercises, wants volume/PRs.
- **Future user:** invited friend, isolated data via `user_id`; same UX, no data leakage.

## 4. Tech Stack

| Layer | Choice | Why | Version |
|---|---|---|---|
| App | **Kotlin Compose Multiplatform** (CMP) | Shared `domain/data/ui` now Android-only, later iOS/Desktop; Compose replaces Views+Java | Kotlin 2.1.20, CMP 1.7.3, AGP 9.3.2 |
| App state | `ViewModel` + `Koin 4` + `Navigation Compose`/`Voyager` | KMP-friendly, no Hilt | — |
| App local data | **SQLDelight 2.0** (or Room-KMP 2.6) + `DataStore` + `WorkManager` | SQLDelight is KMP-proven; Room-KMP viable alternative | — |
| App charts | `Vico 2.0` (or `KMPCharts`) | CMP; `MPAndroidChart` is Views-only | — |
| Backend | **Ktor 3.1** (Netty) + `Exposed 0.60` + `HikariCP` + `Flyway` | Kotlin sharing, lightweight, personal | — |
| DB | **MariaDB 11** (Docker) | Required by spec | mariadb:11 |
| Auth | JWT (access 15m + refresh 30d, `bcrypt`) | Multi-user; Tailscale alone not enough for row isolation | — |
| Infra | Docker Compose on ai-vm | Single compose, no public ports | — |
| Net | **Tailscale** (LXC subnet router advertises LAN; `ai-vm` has **no** tailnet daemon; `ssh ulrich@ai-vm` works via subnet) | LXC advertises `ai-vm` subnet; approved in admin console; app reaches `http://ai-vm:8080` via tailnet | — |
| I18n | **German UI**, units **KG** only, **ignore bodyweight** (weight `0` allowed but not tracked) | Logs/specs are German; parser comma-tolerant | — |

## 5. Architecture

```
composeApp (Android, German UI)         ai-vm (Ubuntu, Docker, no Tailscale daemon)
┌─ shared/commonMain ─┐                ┌─ docker-compose.yml ─┐
│ domain/model        │   Ktor client  │ api:ktor:8080 ──► db:3306 (mariadb:11, vol db-data)
│ data/SqlDelight     │ ──JWT/HTTP───► │   Exposed+Flyway+JWT  LAN-only (via LXC subnet)
│ ui/Compose (Vico)   │  http://ai-vm:8080 │  bind 0.0.0.0 but firewall/LAN; ACL via LXC subnet
└─────────────────────┘  (reachable only  └──────────────────────┘
                         when phone on tailnet, subnet route approved)
         ▲ WorkManager SyncWorker (only when tailnet reachable, last-write-wins)
                           LXC (Tailscale subnet router, e.g. 192.168.1.0/24) ──► ai-vm
```

**Repo layout (planned)**
```
settings.gradle.kts → include(":shared", ":composeApp", ":server")
gradle/libs.versions.toml → kotlin, compose-jb, ktor, sqlDelight, vico, koin
shared/  → commonMain(domain, data db/api, ui screens/viewmodels)
composeApp/ → androidMain (ComponentActivity setContent{ UlFitnessApp() })
server/  → src/main/kotlin (ktor app, routes, exposed entities, flyway V1__*.sql), Dockerfile
docker-compose.yml, .env (gitignored: JWT_SECRET, DB_PASSWORD), Caddyfile (optional)
```

## 6. Data Model (MariaDB, Flyway)

```sql
-- V1__init.sql
CREATE TABLE users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  email VARCHAR(255) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT NOW(6)
);
CREATE TABLE gyms (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  owner_id BIGINT NULL, -- NULL = system/seeded gym visible to all; else private to user
  name VARCHAR(120) NOT NULL,
  city VARCHAR(120) NULL, -- optional, for display
  is_system BOOLEAN NOT NULL DEFAULT FALSE,
  created_at DATETIME(6) NOT NULL,
  FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE,
  UNIQUE KEY uq_gym_owner_name(owner_id, name)
);
CREATE TABLE exercises (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  owner_id BIGINT NULL, -- NULL = system exercise
  gym_id BIGINT NULL, -- NULL = available at every gym (e.g. Bench Press, Squat); NOT NULL = machine/equipment only at that gym
  name VARCHAR(120) NOT NULL,
  category ENUM('push','pull','legs','core','full','cardio','other') NOT NULL,
  kind ENUM('free_weight','machine','cable','bodyweight','other') NOT NULL DEFAULT 'free_weight',
  is_system BOOLEAN NOT NULL DEFAULT FALSE,
  created_at DATETIME(6) NOT NULL,
  FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY (gym_id) REFERENCES gyms(id) ON DELETE CASCADE,
  UNIQUE KEY uq_exercise_owner_gym_name(owner_id, gym_id, name)
);
CREATE TABLE workouts (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  gym_id BIGINT NULL, -- nullable for back-compat; v1 requires it
  started_at DATETIME(6) NOT NULL,
  ended_at DATETIME(6) NULL,
  notes TEXT NULL,
  created_at DATETIME(6) NOT NULL,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY (gym_id) REFERENCES gyms(id) ON DELETE SET NULL,
  INDEX idx_workouts_user_started(user_id, started_at),
  INDEX idx_workouts_gym(gym_id)
);
CREATE TABLE workout_exercises (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  workout_id BIGINT NOT NULL,
  exercise_id BIGINT NOT NULL,
  order_idx INT NOT NULL,
  FOREIGN KEY (workout_id) REFERENCES workouts(id) ON DELETE CASCADE,
  FOREIGN KEY (exercise_id) REFERENCES exercises(id)
);
CREATE TABLE sets (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  workout_exercise_id BIGINT NOT NULL,
  set_no INT NOT NULL,
  reps INT NOT NULL CHECK (reps >= 0),
  weight_kg DECIMAL(5,2) NOT NULL CHECK (weight_kg >= 0), -- 0 = bodyweight (e.g. Hyperextension 10x body) but bodyweight is ignored for stats per decision
  is_warmup BOOLEAN NOT NULL DEFAULT FALSE, -- e.g. "0 und 15kg warmup"
  rpe TINYINT NULL CHECK (rpe BETWEEN 1 AND 10),
  is_failure BOOLEAN NOT NULL DEFAULT FALSE,
  note TEXT NULL, -- per-set note e.g. "oberschenkel Innenseite zieht leicht"
  created_at DATETIME(6) NOT NULL,
  FOREIGN KEY (workout_exercise_id) REFERENCES workout_exercises(id) ON DELETE CASCADE
);
CREATE TABLE exercise_aliases (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  exercise_id BIGINT NOT NULL,
  alias VARCHAR(120) NOT NULL, -- e.g. Hackschmitt → Hackenschmidt, Brust → Brustpresse
  created_at DATETIME(6) NOT NULL,
  FOREIGN KEY (exercise_id) REFERENCES exercises(id) ON DELETE CASCADE,
  UNIQUE KEY uq_alias(alias, exercise_id),
  INDEX idx_alias(alias)
);
-- V2 candidate: body_metrics(id,user_id,date,weight_kg,body_fat)
-- seed in V1 (2 gyms + canonical machines/exercises derived from real logs 2025-2026):
-- gyms: Thomas Sport Center (Upper), All Inclusive Fitness (Legs/Core)
-- INSERT INTO gyms(owner_id,name,is_system) VALUES (NULL,'Thomas Sport Center',TRUE),(NULL,'All Inclusive Fitness',TRUE);
-- All Inclusive — Legs/Core seed (kind=machine unless noted):
--   Hackenschmidt, Hip Thrust Machine, Hip Thrust (free_weight), Beinstrecker (Leg Extension),
--   Beinbeuger (Leg Curl), Waden an 45er Presse, Waden horizontal / Beinpresse horizontal (Leg Press horizontal),
--   Bauchmaschine, Hyperextension (bodyweight), Beinpresse, V Squat Machine
-- Thomas Sport Center — Upper seed:
--   Latzug, Brustpresse, Rudern / Rudern Maschine / Rudern Brustgestützt, Brustfly / Chest Fly,
--   Schulterpresse, Seitheben, Face Pulls, Bizeps Hammer Curls / Bizeps am Kabelzug,
--   Trizeps Skull Crush / Trizeps Overhead Kabelzug, Bauch, Waden einseitig,
--   Unterarme innen/außen Curls
-- INSERT INTO exercises(owner_id,gym_id,name,category,kind,is_system) VALUES
--   (NULL,(SELECT id FROM gyms WHERE name='All Inclusive Fitness'),'Hackenschmidt','legs','machine',TRUE),
--   (NULL,(SELECT id FROM gyms WHERE name='All Inclusive Fitness'),'Hip Thrust Machine','legs','machine',TRUE),
--   (NULL,NULL,'Hyperextension','core','bodyweight',TRUE), -- weight 0 = body
--   (NULL,(SELECT id FROM gyms WHERE name='Thomas Sport Center'),'Brustpresse','push','machine',TRUE),
--   (NULL,(SELECT id FROM gyms WHERE name='Thomas Sport Center'),'Latzug','pull','cable',TRUE), ...
```

**Gym modeling:** `gyms` first-class, extensible N. Seed the two current gyms as `is_system` so every user sees them; `Thomas Sport Center` default tag = Upper, `All Inclusive Fitness` = Legs+Core but not enforced — user can do any category at either gym. `exercises.gym_id` distinguishes global free-weights (`NULL`, show at any gym) vs gym-specific machines (e.g. “Chest Press #2” `gym_id=All Inclusive` only appears when that gym is selected; global search can still find it). Validation: `workouts.gym_id` must match or be `NULL` for exercises added to that workout if `exercises.gym_id IS NOT NULL`.

**Alias modeling:** `exercise_aliases` maps typos/variants to canonical exercise (`Hackschmitt→Hackenschmidt`, `Brust→Brustpresse`, `Chest fly→Brustfly`, `Beinpresse horizontal→Beinpresse`). `GET /exercises?q=` searches `exercises.name` **and** `exercise_aliases.alias` (JOIN). Picker shows canonical name with alias hint.

**Conventions:** UTC `DATETIME(6)`, `DECIMAL(5,2)` for **KG only** (allows 999.99, comma `,` normalized to dot before insert). System exercises/machines seeded for both gyms. `weight_kg=0` (bodyweight) stored but **ignored for e1RM/volume** per decision (no bodyweight progression).

**RPE 1-10:** User decision. `1` = minimal effort / warm-up, `10` = limit / could not do one more rep. `is_failure` separate because failure can happen at any RPE; chart can show RPE trend. Null = not recorded (v1 optional but stored). UI shows German labels `Leicht`…`Limit`.

## 7. API (Ktor, `/api/v1`, JSON, JWT bearer except `/health`, `/auth/*`)

**Auth**
- `POST /auth/register {email,password}` → `201 {id}` (invite-only; could disable after initial)
- `POST /auth/login {email,password}` → `200 {accessToken, refreshToken, expiresIn}`
- `POST /auth/refresh {refreshToken}` → `200 {accessToken}`

**Gyms**
- `GET /gyms` → `[{id,name,city,is_system,owner_id}]` (system + mine)
- `POST /gyms {name, city?}` → `201 {id}` (create new gym, `owner_id = me`)
- `PUT /gyms/{id} {name, city}` (only own gyms) → `200`
- `DELETE /gyms/{id}` (only own, fails if referenced by workouts unless `?force` migrates)

**Exercises / Machines** (unified, German names)
- `GET /exercises?gymId=&q=&category=&include_system=true` → `[{id,name,category,kind,gym_id,gym_name,is_system,owner_id,aliases:[...]}]` — when `gymId` set, returns `gym_id IS NULL` (global) **plus** `gym_id = ?` (machines for that gym), sorted machines last. `q` matches `name` **and** `exercise_aliases.alias`.
- `POST /exercises {name,category,kind,gym_id?,aliases?:[string]}` → `201 {id}` — `kind=machine` requires `gym_id`; `kind=free_weight/bodyweight` usually `gym_id=NULL` (allow any gym). Validation: `gym_id` must be system or owned by me.
- `PUT /exercises/{id} {name,category,kind,gym_id}` (only own) → `200`
- `PUT /exercises/{id}/aliases {add:[], remove:[]}` → `200` — manage aliases
- `GET /exercises/{id}/aliases` → `[{alias}]`
- `DELETE /exercises/{id}` (only own, soft if referenced)

**Workouts**
- `POST /workouts {gym_id, started_at, notes?, exercises:[{exerciseId, sets:[{reps,weight_kg,is_warmup?,rpe?,is_failure,note?}]}]}` → `201 {id}` — validates `exercise.gym_id IS NULL OR = workouts.gym_id`; `409` if machine from other gym. `is_warmup` marks warmup sets (not counted in e1RM/PR), `weight_kg=0` means bodyweight.
- `GET /workouts?gymId=&from=&to=&limit=&offset=&since=` (own only) → list, supports `since` for sync; `gymId` filter optional.
- `GET /workouts/{id}` (full graph + `gym` + `gym_name` at top)
- `PATCH /workouts/{id}/finish {ended_at}` / `PATCH /workouts/{id} {gym_id, notes}` (pre-finish)
- `DELETE /workouts/{id}`

**Stats** (gym-aware)
- `GET /stats/progress?exerciseId=&gymId=&period=90d&metric=e1RM|volume|max` → `[{date, value, reps, weight_kg}]` (`e1RM = weight*(1+reps/30)` Epley; volume = Σ `reps*weight`) — `gymId` optional filter.
- `GET /stats/prs?exerciseId=&gymId=` → `{maxWeight:{value,date,gym}, maxE1RM:{}, maxVolume:{}}`

**Conventions:** `401` on bad JWT, `403` on not own resource, `409` on duplicate `(owner,gym,name)` or machine-gym mismatch, `X-Request-Id` logs.

## 8. App Spec

**Screens (Compose) — UI Sprache: Deutsch (per decision), Einheiten KG**
1. **Start** — keep `ic_ul_logo.xml` → Compose vector, `UL FITNESS` title, dark `background_dark #0F0F0F` (unchanged `themes.xml:3`), `AppCompatDelegate.MODE_NIGHT_YES` remains.
2. **Login/Register** — `E-Mail` + `Passwort`, `Anmelden`/`Registrieren`, `DataStore` token, auto-refresh. Fehlermeldungen Deutsch.
3. **Home** — Gym-Schnellwahl (`Thomas Sport Center` / `All Inclusive Fitness` Chips, plus `+` neues Studio), CTA `Training starten`, letzte Trainings nach Studio gruppiert, letzter PR.
4. **Training (Fokus)** — *Einfachheit während des Trainings ist #1:*
   - **Studio-Wahl (erster Schritt):** `Training starten` → Studio wählen (Thomas / All Inclusive / weiter angelegtes). Setzt `workouts.gym_id`, filtert Maschinen. Vor erstem Satz änderbar. Merkt letztes Studio.
   - **Übungs-/Geräte-Picker:** `+ Übung hinzufügen` öffnet Sheet: Tabs `Dieses Studio` (global `gym_id NULL` + Maschinen dieses Studios) und `Alle`. Liste zeigt `Art`-Badge (Gerät vs Freihantel). Alias-Suche findet `Hackschmitt`. Inline `+ Neu anlegen` → Dialog `Name, Kategorie (Push/Pull/Beine/Core...), Art (Freihantel/Gerät/Kabelzug/Eigengewicht), Studio (vorausgefüllt, änderbar), Aliase?` → `POST /exercises`. Optimistisch lokal, Sync-Queue.
   - Pro Übung: vorheriger Satz als Geist (Tippen = Gewicht kopieren), Wiederholungs-Stepper `−/ +` (groß 56dp), Gewichts-Chips `−2,5/ +2,5 / +5` (**KG**, Komma), Numpad-Fallback, `RPE 1-10` Slider (Ticks 1-10, Label `Leicht…Limit`) + `Muskelversagen` Checkbox, pro Satz `Notiz` (aufklappbares Textfeld), `Satz speichern` Haptik. Parser akzeptiert `12x35kg` **oder** Slider.
   - Pausentimer: rund `Vico`/`CircularProgress`, `90s` Default (pro Übung gemerkt), Benachrichtigung `+30s / Überspringen` (`POST_NOTIFICATIONS` SDK 37), startet automatisch nach Log, Vibration bei 0.
   - Swipe zwischen Übungen, sticky Header `Studio • Übung`.
   - `Beenden` → PATCH `ended_at`; offline → Queue. Zusammenfassung `Studio • Dauer • Volumen (KG)`.
5. **Verlauf** — paginierte Liste nach Studio gruppiert (Filter `Alle / Thomas / All Inclusive`), Suche (auch Alias), Wischen Löschen, Bearbeiten (lädt ins Training mit gleichem `gym_id`).
6. **Fortschritt** — Studio-Filter + Übungs-/Geräte-Selektor (nach Studio gefiltert) + `Vico` Liniendiagramm, Zeitraum `4W/12W/1J/Alle`, Metriken `e1RM/Volumen/Max`, PR-Abzeichen, Tabelle Rohsätze mit Studio-Spalte (Warmup ausgegraut, Bodyweight `0` ignoriert für PR/e1RM per Entscheidung).
7. **Verwalten** — zwei Bereiche: **Studios** (`+` anlegen, eigenes umbenennen) und **Übungen/Geräte** (nach Studio filtern, anlegen, Alias pflegen via `PUT /aliases`). System-Studios Thomas/All Inclusive immer sichtbar.

**Data & sync**
- **Offline-first:** `SqlDelight` cache is source of truth; `WorkoutRepository` writes locally, `SyncWorker` (WorkManager, `NetworkType.CONNECTED` + tailnet probe `GET /health`) does `POST` pending + `GET /workouts?since=lastSync`. Conflict: last-write-wins (safe for personal).
- **Reachability check:** `GET http://ai-vm:8080/api/v1/health` (no auth, via LXC subnet route) — if `ai-vm` DNS/`192.168.x.x` via tailnet fails, show `● Offline` Banner (nicht Fehler).
- **BuildConfig:** `API_BASE_URL = "http://ai-vm:8080/api/v1"` (LAN-Name, nur über Tailscale-Subnet erreichbar, kein MagicDNS `*.ts.net` da `ai-vm` selbst kein Tailscale-Daemon hat), per `buildType` generiert; kein öffentlicher Fallback. `ai-vm` per `ssh ulrich@ai-vm` bereits auflösbar (lokales Netz + Subnet-Advertisement).
- **Einheiten:** ausschließlich **KG** (`DECIMAL(5,2)`), Parser normalisiert `47,5` → `47.50`; Eingabe zeigt `kg`.

**Permissions:** `POST_NOTIFICATIONS` (Pausentimer), `INTERNET`; kein `ACTIVITY_RECOGNITION` in v1. Strings Deutsch (`strings.xml` + Compose `stringsDe`).

## 9. Visualization

- Library: `Vico 2.0` (CMP). Single chart per exercise/machine, three toggles:
  - **e1RM** (primary) — `weight*(1+reps/30)`, smoothed 7d avg.
  - **Total volume** — `Σ reps*weight` per session.
  - **Max weight** — top set per session.
- X: date, Y: kg / kg×reps. Markers: PRs with label, failed sets dimmed, gym color dot. Table below chart for raw export (CSV later) with gym column.
- Gym filter chip (`All / Thomas Sport Center / All Inclusive Fitness`) narrows picker and chart; e.g. Leg Press at All Inclusive vs generic Squat shows distinct lines. `GET /stats/progress?gymId=` powers it.

## 10. Tailscale Networking — Subnet Router (kein Daemon auf `ai-vm`)

**Antwort auf deine Frage:** Nein, Tailscale muss **nicht** auf `ai-vm` laufen. Dein **LXC Subnet-Router** reicht völlig — er advertiert das LAN-Subnetz (z. B. `192.168.1.0/24` oder `ai-vm/32`), wird im Admin-Console **approved**, und Tailnet-Geräte erreichen `ai-vm` via `http://ai-vm:8080` (oder `192.168.1.x`) **nur wenn sie im Tailnet sind**. `ssh ulrich@ai-vm` funktioniert deshalb auch nur noch via Tailnet, wenn du die Route aktivierst.

- **LXC:** `tailscale up --advertise-routes=192.168.0.0/24` (oder konkretes Subnetz von `ai-vm`), dann in `https://login.tailscale.com/admin/machines` → `Edit route settings` → `Approve`.
- **ACL (`tailnet policy_tailnet.json`):** `{"src":["tag:ul-fitness-phone","autogroup:member"],"dst":["192.168.0.0/24:8080"]}` (DB `3306` nicht exponieren — nur api via subnet). Kein `0.0.0.0` Public.
- **DNS:** Kein MagicDNS `*.ts.net` für `ai-vm` selbst (daheim kein Daemon → kein `100.x`). App nutzt **`http://ai-vm:8080/api/v1`** (LAN-Name, via Tailnet DNS/Subnet). Phone im Tailnet löst `ai-vm` via lokalen DNS durchs Subnetz. Fallback: `http://<LAN-IP>:8080`.
- **Verschlüsselung:** WireGuard verschlüsselt trotzdem den Tailnet-Tunnel, daher reicht **HTTP** im Tailnet (optional Caddy mit self-signed für `https://ai-vm` wenn gewünscht, aber nicht nötig).
- **Phone:** Tailscale installieren, selbes Tailnet, `Use subnet routes` aktivieren. Ohne Tailnet ist `ai-vm:8080` unerreichbar — genau die gewünschte Private-Only-Garantie.
- **Vorteil kein Daemon auf ai-vm:** Weniger Pflege auf ai-vm; Nachteil: kein `tailscale serve` HTTPS/Auto-Cert, kein `100.x` — deshalb hier `http` + Subnet.

## 11. Security & Private-Only

- **Perimeter:** `api` container lauscht auf `0.0.0.0:8080` aber ist nur via LAN/Subnet erreichbar — kein Port-Forward, kein öffentlicher DNS. `UFW` `deny 8080/tcp` nur für öffentliches Interface (Tailnet/LAN via Subnet bleibt erlaubt); `docker-compose` exponiert DB nicht. Beste Garantie ist **kein öffentliches Routing** + **Tailscale Subnet** als einziges Gateway.
- **App:** JWT in `EncryptedSharedPreferences`/`DataStore`, Refresh. `network_security_config.xml` erlaubt `http://ai-vm` im Tailnet (cleartext, da WireGuard bereits verschlüsselt). `BuildConfig` Check: fails if URL not `ai-vm`/`192.168.*`.
- **Not runnable elsewhere:** Ohne aktives Tailnet + genehmigte Subnet-Route ist `ai-vm:8080` nicht routbar — genau private-only. Kein Funnel/Serve nötig weil kein Daemon auf ai-vm.
- **Secrets:** `.env` (`JWT_SECRET`, `MARIADB_ROOT_PASSWORD`) gitignored (`.gitignore` + `.env`).

## 12. Deployment (ai-vm, Docker)

**Host:** Ubuntu + Docker already. Single `docker-compose.yml` at `~/ul-fitness/` (or `/opt/ul-fitness`).

```yaml
services:
  db:
    image: mariadb:11
    env_file: .env
    environment:
      MARIADB_DATABASE: ul_fitness
    volumes: [db-data:/var/lib/mysql, ./sql:/docker-entrypoint-initdb.d]
    restart: unless-stopped
  api:
    build: ./server
    env_file: .env # DB_URL=jdbc:mariadb://db:3306/ul_fitness, JWT_SECRET, TAILSCALE_HOST
    depends_on: [db]
    network_mode: host # binds to tailscale0 via $HOST
    restart: unless-stopped
    # no public ports
volumes: { db-data: {} }
```

**TLS:** Kein `tailscale serve` auf `ai-vm` möglich (kein Daemon) → **HTTP im Tailnet** (WireGuard verschlüsselt) genügt. Optional Caddy mit self-signed `https://ai-vm` wenn gewünscht.

**Deploy:**
```bash
# on ai-vm (LAN via LXC subnet, works only when you are on tailnet: ssh ulrich@ai-vm)
git clone https://github.com/drepguy/ul-fitness.git && cd ul-fitness
cp .env.example .env && $EDITOR .env
docker compose up -d --build
docker compose logs -f api
curl http://ai-vm:8080/api/v1/health # {"status":"ok"}  # nur im Tailnet/Subnet
# vom Handy (Tailnet an, Subnet genehmigt): http://ai-vm:8080/api/v1/health
```

**Backup:** `docker exec ul-fitness-db-1 mariadb-dump ul_fitness | gzip > backup.sql.gz` (cron weekly to ai-vm volume).

## 13. Build & Run (dev)

```powershell
# Android (emulator, VPN not required for UI; needs Tailnet for sync)
$env:JAVA_HOME="C:\Users\<user>\AppData\Local\Programs\Android Studio\jbr"
.\gradlew.bat :composeApp:assembleDebug
.\gradlew.bat :shared:testDebugUnitTest

# Server
.\gradlew.bat :server:run # local H2/Maria via Testcontainers
docker compose up --build

# Single test
.\gradlew.bat :composeApp:testDebugUnitTest --tests "*WorkoutRepositoryTest*"
```

`gradle/libs.versions.toml` holds `kotlin`, `compose-multiplatform`, `ktor`, `sqlDelight`, `vico`, `koin`.

## 14. Config

- `.env` (gitignored, **KG-only**, **deutsch**): `MARIADB_ROOT_PASSWORD`, `MARIADB_USER`, `MARIADB_PASSWORD`, `JWT_SECRET` (32+ chars), `API_HOST=ai-vm` (LAN-Name via Subnet).
- `local.properties`: `sdk.dir=C\:\\AndroidSDK` (existing, gitignored).
- **Aliase:** `exercise_aliases` Tabelle, Seed z. B. `Hackschmitt→Hackenschmidt`.
- **Bodyweight:** ignoriert für Stats (`weight_kg=0` → nicht in e1RM/Volumen).

Add to `.gitignore`: `.env` (todo — next commit).

## 15. Testing

- Unit: `shared` domain (e1RM), `Flyway` migrations, Ktor routes (`testApplication`).
- Instrumented: Compose `createAndroidComposeRule` for Workout screen (tap log, timer).
- Manual: airplane mode → log 2 sets → reconnect Tailnet → `GET /workouts?since` returns synced.

## 16. Out of Scope (v1)

iOS/Desktop targets (KMP ready but ungenerated), body-metrics, CSV export, routines library, Wear, Health Connect.

## 17. Decisions Log

- RPE `1-10` (user: `1-10`), `is_failure` separate. Covers warm-up to limit. Per-set `is_warmup` added for logs like `0 und 15kg warmup`.
- Multi-user from schema start (FK `user_id`).
- Tailscale over new WireGuard/OpenVPN (existing advertiser).
- KMP + Ktor per user choice (shared Kotlin, no FastAPI).
- Vico over MPAndroidChart (CMP).
- **Gyms first-class (2026-08-29):** `gyms` table, seed `Thomas Sport Center` (Upper) + `All Inclusive Fitness` (Legs/Core) as `is_system`. `exercises.gym_id NULL` = global free-weight, otherwise machine tied to one gym (user wants to choose per-gym machines or create new). `workouts.gym_id` required. Picker filters `global + this-gym machines`, API validates mismatch. Covers your current 2-gym split but extensible N.
- **Machine/exercise input from real logs (2026-08-29):** See Appendix 19; decimal `47,5` comma, `body` weight, `y` typo handling → parser + seed aliases.

## 18. Phases

- **P0 (0.5d):** Confirm MagicDNS name, `docker-compose.yml` health, `GET /health` over Tailnet.
- **P1 (1.5d):** Flyway V1 (gyms + exercises.gym_id + workouts.gym_id + sets.is_warmup), seed `Thomas Sport Center` / `All Inclusive Fitness` + system exercises/machines derived from Appendix 19, JWT, gym/exercise/workout CRUD with warmup/bodyweight support.
- **P2 (2.5d):** CMP scaffold (`shared/composeApp`), SQLDelight (gyms+migrations), Workout screen with **gym picker → exercise/machine picker (filtered) + inline create** + quick `reps×weight` parser (comma/dot, body) + rest timer + RPE 1-10.
- **P3 (1d):** `SyncWorker`, MagicDNS `BuildConfig`, offline banner.
- **P4 (1.5d):** Vico progress + PRs with **gym filter** (warmup excluded from PR/e1RM).
- **P5 (0.5d):** Bind-to-tailnet, pinning, signing, `AGENTS.md` update (add `shared`/`server`, deploy cmd).

---

## 19. Appendix — Real Logs & Derived Requirements

User’s current notes (German, edited for spec). Parsing must be forgiving; app input should be **faster** than typing these strings by hand but **import-compatible** if pasting.

**All Inclusive Fitness (Legs + Core)** — typical session `01.08.2026`:
- `Hackschmitt: 12x35kg, 12x35kg, 12x35kg`
- `Hip thrust machine: 9x20kg, 10x20kg, 9x20kg`
- `Beinstrecker: 13x40kg, 9x47.5kg, 8x47.5kg`
- `Beinbeuger: 12x40kg, 11x47,5kg, 8x47,5kg`
- `Waden an 45er Presse: 13x50kg, 13x75kg, 13x85kg`
- `Bauchmaschine: 14x37,5kg, 9x42,5kg` (sometimes `muss heute noch pausieren` note at workout level)
- `Hyperextension: 10x8kg` (also `10x body`, `12xbodyweight`)

Other variants seen across 01.08–26.08.2026: `Beinpresse horizontal 17x87.5kg`, `Waden horizontal 12x110kg`, `V Squat machine/Beinpresse Ersatz 10x65kg`, `40 und 70kg warmup` / `0 und 15kg warmup` / `50 und 75 warmup`, set notes like `oberschenkel Innenseite zieht leicht. Überlastet?`, `1 Sekunde pause am untersten Punkt`, `muss heute noch pausieren`, `weggelassen wegen muskelkater`, `Deload 1./3. session`, `abbruch` (aborted).

**Thomas Sport Center (Upper)** — typical `21.07.2025` + `02.08–27.08.2026`:
- `Latzug: 10x50kg, 8x50kg, 10x45kg`
- `Brustpresse: 9x41kg, 7x36kg, 7x32kg` (also `Brust: 8x41`)
- `Rudern: 7x60kg, 9x52,5kg` (variants: `Rudern Maschine`, `Rudern Brustgestützt 10x57.5kg`)
- `Brustfly / Chest fly: 13x32, 8x27kg, 15x36kg`
- `Schulterpresse: 8x18kg (austesten)`
- `Bauch: 8x32kg`
- `Waden einseitig: 8x54kg`, `Unterarme innen/außen curls: 11x5kg`
- `Face pulls 10x9kg`, `Seitheben 10x5kg / 6x9kg (Mitglied links anfangen)`, `Bizeps Hammer Curls 10x10kg / Bizeps am Kabelzug 18x27kg`, `Trizeps Skull Crush 13x stange / 15x2.5kg / Trizeps overhead Kabelzug 14x18kg`

**Derived input & catalog requirements**

| Observation | Requirement |
|---|---|
| Two gyms with stable split (Thomas Upper, All Inclusive Legs/Core) | `gyms` seed + gym picker before workout, history grouped by gym; not enforced — user can do any category at either gym. |
| Same movement under many spellings (`Brustpresse`/`Brust`, `Beinpresse`/`Beinpresse horizontal`, `Waden an 45er`/`Waden horizontal`, `Hackschmitt` typo for `Hackenschmidt`, `Chest fly`/`Brustfly`) | Canonical `exercises.name` + **alias column** (future V2 `exercise_aliases` or client-side `alias → canonical` map). Picker shows canonical, search matches alias. Seed includes most frequent aliases below. |
| Machines vs free weights conflated | `kind` enum `free_weight/machine/cable/bodyweight`; `kind=machine` must have `gym_id`. UI badge `G` vs `M`. |
| Weight formats `47,5kg`, `47.5kg`, `50kg`, `stange` (bar), `body`/`bodyweight` | Parser: accept comma **or** dot, optional `kg`, `body` → `weight_kg=0` + `note=bodyweight` or `is_warmup` handling; `stange` → prompt `Bar weight? (default 8kg)` or save as `note=stange`. Store normalized `5.2`. |
| Warmups `0 und 15kg warmup`, `40 und 70kg warmup` | `is_warmup=true` (excluded from charts/PR), rendered ghosted. Quick toggle long-press `W`. |
| Per-set notes in parentheses, per-workout notes (`muss heute noch pausieren`, `Deload`) | `sets.note` + `workouts.notes`; long-press set row to add note. Deload tag = `workouts.notes` contains `Deload`. |
| Varying set counts (2–4 sets, sometimes extra set as replacement) | `+ Add set` unlimited, not fixed 3; previous set ghost. |
| Typos `10y20kg`, double commas | Client parser sanitizes `y→x`, `,,→,`, trims spaces. |
| Quick entry need during workout | Target flow: `Start → pick Thomas → + Brustpresse → 10x41 (numpad) → Log → RPE 8 → auto next set with +2.5kg chip → Finish`. Picker remembers recent for that gym. |

**Canonical seed + aliases (V1 minimal for import)**

```sql
-- Thomas Sport Center
-- Brustpresse (alias: Brust), Latzug, Rudern, Rudern Maschine, Rudern Brustgestützt,
-- Brustfly/Chest Fly, Schulterpresse, Seitheben, Face Pulls,
-- Trizeps Skull Crush, Trizeps Overhead Kabelzug, Bizeps Hammer Curls, Bizeps am Kabelzug,
-- Bauch (alias Bauchmaschine when at Thomas? but keep), Waden einseitig, Unterarme Curls
-- All Inclusive Fitness
-- Hackenschmidt (alias Hackschmitt/Hack Squat), Hip Thrust Machine, Hip Thrust,
-- Beinstrecker, Beinbeuger, Waden an 45er Presse / Waden horizontal, Bauchmaschine,
-- Hyperextension, Beinpresse horizontal / Beinpresse / V Squat Machine
```

Future: bulk import screen `Paste notes → parse → preview → choose gym → create missing machines` to migrate history in one go (out of v1 but parser ready).

---

*Next: implement P0-P1 — scaffold `server`/KMP, `.env`, `docker-compose.yml`, `GET /health` over Tailnet for sign-off before tracking UI.*
