# UL Fitness — Spec

**Version:** 0.4.3 — 2026-08-30  
**Status:** Draft (agreed stack: Kotlin Compose Multiplatform + Ktor + MariaDB, Tailscale LXC subnet router; RPE 1-10; 2 gyms + extensible; UI Deutsch, KG, `exercise_aliases`; ~/-deploy, single user v1, Vorlagen: beides; 1 Gerät → n Übungen; UI einfach→mächtig; Icons)  
**Repo:** `https://github.com/drepguy/ul-fitness.git` (`main`) — single-module `:app` today, to become `:shared` + `:composeApp` + `:server`

---

## 1. Summary

Personal, private fitness app to **track sets × reps × weight per exercise/machine** during workouts, with **RPE 1-10 + notes**, fast gym logging, and **progress visualisation**. No public hosting: data lives on **ai-vm (Ubuntu) MariaDB**, **Docker**, accessed **only via Tailscale tailnet** (existing LXC subnet advertiser). Multi-user capable from day one (you + future friends), offline-first on device.

Current reality: **2 gyms** — **Thomas Sport Center** (Upper Body) and **All Inclusive Fitness** (Legs + Core). User picks **gym → exercises/machines for that gym** (or creates new ones inline) → logs sets. Spec treats gyms first-class and extensible (N gyms), exercises/machines unified but filterable by gym.

Current codebase (`AGENTS.md:3`): AGP 9.3.2 / Gradle 9.5.0 / SDK 37 / JDK 25 (foojay), `MainActivity.java:5` dark splash (`ic_ul_logo.xml` + `UL FITNESS`), `BuildConfig` not yet KMP — greenfield for domain.

## 2. Goals / Non-Goals

 **Goals**
- Log a set in <3 taps, one-handed, with sweaty fingers; rest timer keeps you moving.
- **Gym-aware logging:** choose gym at workout start, then picker shows only relevant exercises/machines for that gym (+ search globally/aliases); create new exercise/machine inline (with gym binding).
- Reliable offline: log without signal/VPN, sync when back on Tailnet.
- Honest progress charts (e1RM, volume, PRs) per exercise/machine, filterable by gym (Warmup + Bodyweight `0` ignoriert).
- Private by construction: API/DB never on public internet; Subnet-Router ist das Gateway.
- Schema bereit für Multi-User (FK `user_id` überall, `gyms`/`exercises` per user/system) — **v1 aber nur du** (`ALLOW_REGISTER=false`).

**Non-Goals**
- Public distribution, App-Store compliance, analytics, ads.
- WearOS, Health Connect, or third-party sync (Strava) in v1.
- Real-time collaboration, social feed, trainer market.
- Automated plate/rep detection (camera).

## 3. Users

- **Owner (you):** daily driver, defines custom exercises, wants volume/PRs. **v1: nur du** (kein Invite, `POST /auth/register` deaktiviert nach initialem Seed).
- **Future user:** schema bereits `user_id`-isoliert; Einladung später via `register` wieder öffnen — kein Umbau nötig.

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
| Net | **Tailscale** (LXC subnet router advertises LAN; `ai-vm` has **no** tailnet daemon; `ssh ulrich@ai-vm` works via subnet) | LXC advertises `192.168.178.0/24` (approved); app reaches `http://192.168.178.8:8080` via tailnet ( `ai-vm` hostname löst am Handy via Subnet nicht — IP ist Quelle der Wahrheit) | — |
| I18n | **German UI**, units **KG** only, **ignore bodyweight** (weight `0` allowed but not tracked) | Logs/specs are German; parser comma-tolerant | — |

## 5. Architecture

```
composeApp (Android, Deutsch)           ai-vm (192.168.178.8, Ubuntu, Docker, kein Tailscale-Daemon)
┌─ shared/commonMain ─┐                ┌─ ~/ul-fitness/docker-compose.yml ─┐
│ domain/model        │   Ktor client  │ api:ktor:8080 ──► db:3306 (mariadb:11, vol db-data)
│ data/SqlDelight     │ ──JWT/HTTP───► │   Exposed+Flyway+JWT  LAN-only (via LXC Subnet)
│ ui/Compose (Vico)   │  http://192.168.178.8:8080 │  0.0.0.0:8080, kein Port-Forward, kein MagicDNS
└─────────────────────┘  (nur via Tailnet └──────────────────────────────┘
                         + genehmigte Subnet-Route 192.168.178.0/24 erreichbar)
         ▲ WorkManager SyncWorker (nur wenn Tailnet/Subnet erreichbar, last-write-wins)
                           LXC (100.88.114.99, Subnet-Router 192.168.178.0/24) ──► ai-vm (192.168.178.8)
```

 **Repo layout (planned)**
```
settings.gradle.kts → include(":shared", ":composeApp", ":server")
gradle/libs.versions.toml → kotlin, compose-jb, ktor, sqlDelight, vico, koin
shared/  → commonMain(domain, data db/api, ui screens/viewmodels + German strings)
composeApp/ → androidMain (ComponentActivity setContent{ UlFitnessApp() })
server/  → src/main/kotlin (ktor app, routes, exposed entities, flyway V1__*.sql), Dockerfile
docker-compose.yml, .env (gitignored: JWT_SECRET, DB_PASSWORD) — kein Caddy nötig (HTTP im Tailnet)
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
  icon_key VARCHAR(40) NOT NULL DEFAULT 'dumbbell', -- Katalog: dumbbell, barbell, leg_press, leg_ext, leg_curl, calf, hip_thrust, chest_press, lat_pull, row, shoulder_press, lateral_raise, face_pull, bicep_curl, triceps, ab_machine, hyperext
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
CREATE TABLE workout_templates (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  gym_id BIGINT NOT NULL,
  name VARCHAR(120) NOT NULL, -- z.B. "Thomas Upper — Standard"
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY (gym_id) REFERENCES gyms(id) ON DELETE CASCADE,
  UNIQUE KEY uq_template_user_gym_name(user_id, gym_id, name)
);
CREATE TABLE workout_template_exercises (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  template_id BIGINT NOT NULL,
  exercise_id BIGINT NOT NULL,
  order_idx INT NOT NULL,
  default_sets INT NOT NULL DEFAULT 3,
  default_reps INT NULL, -- optional Vorgabe
  default_weight_kg DECIMAL(5,2) NULL,
  FOREIGN KEY (template_id) REFERENCES workout_templates(id) ON DELETE CASCADE,
  FOREIGN KEY (exercise_id) REFERENCES exercises(id) ON DELETE CASCADE
);
-- V2 candidate: body_metrics(id,user_id,date,weight_kg,body_fat)
-- seed in V1 (2 gyms + canonical machines/exercises derived from real logs 2025-2026):
-- gyms: Thomas Sport Center (Upper), All Inclusive Fitness (Legs/Core)
-- INSERT INTO gyms(owner_id,name,is_system) VALUES (NULL,'Thomas Sport Center',TRUE),(NULL,'All Inclusive Fitness',TRUE);
-- All Inclusive — Legs/Core seed (kind=machine unless noted):
--   Hackenschmidt, Hip Thrust Machine, Hip Thrust (free_weight), Beinstrecker (Leg Extension),
--   Beinbeuger (Leg Curl),
--   // WICHTIG: gleiches Gerät → mehrere Übungen (dein Hinweis 30.08):
--   // Beinpresse horizontal (Beine/Quads, machine, All Inclusive),
--   // Wadenpresse horizontal (Waden, machine, All Inclusive),
--   // Beinpresse 45° (Beine/Quads, machine, All Inclusive),
--   // Wadenpresse 45° (Waden, machine, All Inclusive) — also 4 Übungen auf 2 Geräten
--   // + sitzende Wadenheber-Maschine (Waden, All Inclusive),
--   Bauchmaschine, Hyperextension (bodyweight), V Squat Machine
-- Thomas Sport Center — Upper seed:
--   Latzug, Brustpresse, Rudern / Rudern Maschine / Rudern Brustgestützt, Brustfly / Chest Fly,
--   Schulterpresse, Seitheben, Face Pulls, Bizeps Hammer Curls / Bizeps am Kabelzug,
--   Trizeps Skull Crush / Trizeps Overhead Kabelzug, Bauch, Waden einseitig,
--   Unterarme innen/außen Curls,
--   // zusätzlich sitzende Wadenheber + separate Wadenmaschine (Thomas, noch nicht geloggt, aber anlegbar)
--   Wadenheber sitzend (Thomas), Wadenmaschine Thomas
-- INSERT INTO exercises(owner_id,gym_id,name,category,kind,icon_key,is_system) VALUES
--   (NULL,(SELECT id FROM gyms WHERE name='All Inclusive Fitness'),'Hackenschmidt','legs','machine','leg_press',TRUE),
--   (NULL,(SELECT id FROM gyms WHERE name='All Inclusive Fitness'),'Hip Thrust Machine','legs','machine','hip_thrust',TRUE),
--   (NULL,(SELECT id FROM gyms WHERE name='All Inclusive Fitness'),'Beinpresse horizontal','legs','machine','leg_press',TRUE),
--   (NULL,(SELECT id FROM gyms WHERE name='All Inclusive Fitness'),'Wadenpresse horizontal','legs','machine','calf',TRUE),
--   (NULL,(SELECT id FROM gyms WHERE name='All Inclusive Fitness'),'Beinpresse 45°','legs','machine','leg_press',TRUE),
--   (NULL,(SELECT id FROM gyms WHERE name='All Inclusive Fitness'),'Wadenpresse 45°','legs','machine','calf',TRUE),
--   (NULL,(SELECT id FROM gyms WHERE name='All Inclusive Fitness'),'Wadenheber sitzend','legs','machine','calf',TRUE),
--   (NULL,(SELECT id FROM gyms WHERE name='Thomas Sport Center'),'Wadenheber sitzend','legs','machine','calf',TRUE),
--   (NULL,NULL,'Hyperextension','core','bodyweight','hyperext',TRUE), -- weight 0 = body
--   (NULL,(SELECT id FROM gyms WHERE name='Thomas Sport Center'),'Brustpresse','push','machine','chest_press',TRUE),
--   (NULL,(SELECT id FROM gyms WHERE name='Thomas Sport Center'),'Latzug','pull','cable','lat_pull',TRUE),
--   -- weitere: Leg Ext → leg_ext, Leg Curl → leg_curl, Shoulder → shoulder_press, Seitheben → lateral_raise, usw.
```

**Gym modeling:** `gyms` first-class, extensible N. Seed die zwei Studios als `is_system`; `Thomas Sport Center` = Upper, `All Inclusive Fitness` = Beine/Core — nicht erzwungen. Ein **Gerät** (z. B. `Beinpresse horizontal`) kann **mehrere Übungen** hosten — dein Fall: `Beinpresse horizontal` + `Wadenpresse horizontal` und `Beinpresse 45°` + `Wadenpresse 45°` sind **4 Übungen auf 2 Geräten** (`spec.md:179`). Gleiches für `Wadenheber sitzend` (All Inclusive + Thomas je eine Maschine). Generell: **Gerät = physisches Gerät, Übung = Bewegung + Gerät + Studio** — v1 vereint als `exercises` (`gym_id` + `name`). Nutzer kann jederzeit `Geräte/Übungen` anlegen (`POST /exercises`, `kind=machine` braucht `gym_id`), daher flexibel. Validierung: `workouts.gym_id` muss zu `exercises.gym_id` (oder `NULL` für globale) passen; `Beinpresse horizontal (Quads)` und `Wadenpresse horizontal (Waden)` sind trotz gleichem Gerät getrennt trackbar (eigene PRs/Charts).

**Alias modeling:** `exercise_aliases` maps typos/variants to canonical exercise (`Hackschmitt→Hackenschmidt`, `Brust→Brustpresse`, `Chest fly→Brustfly`, `Beinpresse horizontal→Beinpresse`). `GET /exercises?q=` searches `exercises.name` **and** `exercise_aliases.alias` (JOIN). Picker shows canonical name with alias hint.

**Conventions:** UTC `DATETIME(6)`, `DECIMAL(5,2)` for **KG only** (allows 999.99, comma `,` normalized to dot before insert). System exercises/machines seeded for both gyms. `weight_kg=0` (bodyweight) stored but **ignored for e1RM/volume** per decision (no bodyweight progression).

**RPE 1-10:** User decision. `1` = minimal effort / warm-up, `10` = limit / could not do one more rep. `is_failure` separate because failure can happen at any RPE; chart can show RPE trend. Null = not recorded (v1 optional but stored). UI shows German labels `Leicht`…`Limit`.

## 7. API (Ktor, `/api/v1`, JSON, JWT bearer except `/health`, `/auth/*`)

**Auth** — v1: **nur du**, `register` nach Seed deaktiviert (per `ALLOW_REGISTER=false` env)
- `POST /auth/register {email,password}` → `201 {id}` / `403` wenn deaktiviert (nur initial)
- `POST /auth/login {email,password}` → `200 {accessToken, refreshToken, expiresIn}`
- `POST /auth/refresh {refreshToken}` → `200 {accessToken}`

**Gyms**
- `GET /gyms` → `[{id,name,city,is_system,owner_id}]` (system + mine)
- `POST /gyms {name, city?}` → `201 {id}` (create new gym, `owner_id = me`)
- `PUT /gyms/{id} {name, city}` (only own gyms) → `200`
- `DELETE /gyms/{id}` (only own, fails if referenced by workouts unless `?force` migrates)

**Exercises / Machines** (unified, German names, mit Icon)
- `GET /exercises?gymId=&q=&category=&include_system=true` → `[{id,name,category,kind,icon_key,gym_id,gym_name,is_system,owner_id,aliases:[...]}]` — when `gymId` set, returns `gym_id IS NULL` (global) **plus** `gym_id = ?` (machines for that gym), sorted machines last. `q` matches `name` **and** `exercise_aliases.alias`. `icon_key` ∈ Katalog unten.
- `POST /exercises {name,category,kind,icon_key?,gym_id?,aliases?:[string]}` → `201 {id}` — `kind=machine` requires `gym_id`; `icon_key` default `dumbbell` wenn nicht angegeben. Validation: `gym_id` must be system or owned by me.
- `PUT /exercises/{id} {name,category,kind,icon_key,gym_id}` (only own) → `200` — ändert Icon.
- `PUT /exercises/{id}/aliases {add:[], remove:[]}` → `200` — manage aliases
- `GET /exercises/{id}/aliases` → `[{alias}]`
- `DELETE /exercises/{id}` (only own, soft if referenced)

**Icon-Katalog** (`drawable/ic_exercise_<key>.xml`, 16 Keys, dunkles Hexagon wie `ic_ul_logo.xml`):
`dumbbell`, `barbell`, `leg_press`, `leg_ext`, `leg_curl`, `calf`, `hip_thrust`, `chest_press`, `lat_pull`, `row`, `shoulder_press`, `lateral_raise`, `face_pull`, `bicep_curl`, `triceps`, `ab_machine`, `hyperext` (Fallback `dumbbell`). Wahl im `Neu anlegen`-Dialog (Grid, Icon groß), später änderbar.

**Workouts**
- `POST /workouts {gym_id, started_at, notes?, exercises:[{exerciseId, sets:[{reps,weight_kg,is_warmup?,rpe?,is_failure,note?}]}]}` → `201 {id}` — validates `exercise.gym_id IS NULL OR = workouts.gym_id`; `409` if machine from other gym. `is_warmup` marks warmup sets (not counted in e1RM/PR), `weight_kg=0` means bodyweight.
- `GET /workouts?gymId=&from=&to=&limit=&offset=&since=` (own only) → list, supports `since` for sync; `gymId` filter optional.
- `GET /workouts/{id}` (full graph + `gym` + `gym_name` at top)
- `PATCH /workouts/{id}/finish {ended_at}` / `PATCH /workouts/{id} {gym_id, notes}` (pre-finish)
- `DELETE /workouts/{id}`

**Templates** (beides: letztes Training + Vorlagen — per Entscheidung)
- `GET /templates?gymId=` → `[{id,name,gym_id, exercises:[{exerciseId,name,order_idx,default_sets}]}]`
- `POST /templates {gym_id, name, exercises:[{exerciseId, order_idx, default_sets?, default_reps?, default_weight_kg?}]}` → `201`
- `PUT /templates/{id} {name, exercises[...]}` → `200`
- `DELETE /templates/{id}` → `204`
- `POST /templates/{id}/start` → `201 {workoutId}` — legt neues `workouts` aus Vorlage an (kopiert Reihenfolge)
- `POST /workouts/from-last {gym_id}` → `201 {workoutId}` — kopiert letztes Training dieses Studios (falls keine Vorlage)

**Stats** (gym-aware)
- `GET /stats/progress?exerciseId=&gymId=&period=90d&metric=e1RM|volume|max` → `[{date, value, reps, weight_kg}]` (`e1RM = weight*(1+reps/30)` Epley; volume = Σ `reps*weight`) — `gymId` optional filter.
- `GET /stats/prs?exerciseId=&gymId=` → `{maxWeight:{value,date,gym}, maxE1RM:{}, maxVolume:{}}`

 **Conventions:** `401` on bad JWT, `403` on not own resource, `409` on duplicate `(owner,gym,name)` or machine-gym mismatch, `400` on invalid `gym_id`/parsing, `X-Request-Id` logs. Pagination: `limit` default `20` (max `100`), `offset` default `0`; `since` ist ISO-8601 `DATETIME(6)` für Sync.

**Edit-Flow:** Gesetzter Satz ist tippbar → Dialog `Wdh/Gewicht/RPE/Notiz` editierbar, `Speichern` überschreibt `sets` (last-write-wins), `Löschen` entfernt Satz; geleertes `workout_exercises` löscht Block. Kein Soft-Delete nötig v1.

## 8. App Spec — UI: Einfach zuerst, Details auf Knopfdruck (Progressive Disclosure)

**Prinzip:** Standardweg `≤3 Taps/Satz` (große Targets 56dp, Haptik, einhändig), Details (RPE/Notiz/Warmup/Alias/Vorlage) immer ein Tap/Long-Press entfernt. Dunkel `#0F0F0F`, Deutsch.

**Navigation (Bottom, dunkel):** `Start` | `Training` | `Verlauf` | `Fortschritt` | `≡ Verwalten` — `Training` primär (FAB `Training starten`).

**Screens**

1. **Start** — `ic_ul_logo.xml` → Compose Vector, `UL FITNESS` + Akzentlinie (`themes.xml:3`), `MODE_NIGHT_YES`.

2. **Anmelden** — `E-Mail` + `Passwort`, `Anmelden`/`Registrieren`, `DataStore` Token, Auto-Refresh, Fehlermeldungen Deutsch.

3. **Home** — **Gym bestimmt alles** (Logikfix: Vorlage/Letztes sind Studio-gebunden):
   ```
   [Thomas Sport Center] [All Inclusive Fitness] [+ Neues Studio]
   ───────── nach Studio-Wahl ─────────
   Letztes Training dieses Studios kopieren  → POST /workouts/from-last {gym_id}
   Vorlagen dieses Studios: [Upper Standard] [Legs PPL] → POST /templates/{id}/start
   Falls keine: [Leer starten]
   ─────────────────────────────────────
   [ Training starten ]  (merkt letztes Studio)
   Zuletzt: All Inclusive — 26.08 — 6 Übungen
   PR: Beinpresse 45° 65×10
   ```
   Chips oben wählen Gym, darunter erscheinen **nur** passendes `Letztes` + `Vorlagen dieses Studios`.

4. **Training (Fokus)** — *Einfachheit während des Trainings ist #1:*
   ```
   Header sticky: All Inclusive Fitness • Hip Thrust Machine  [⋮]
   ┌─────────────────────────────────────────┐
   │  Geist: 12×30kg (letztes Mal) [Übernehmen] │
   │  Satz 1  12×30kg  RPE 8  [ ]MV  [notiz] ✓ │
   │  Satz 2  10×30kg  RPE 9  [✓]MV  [_____] ○ │
   │  ─────────────────────────────────────── │
   │  Wdh  [ −  10  + ]   Gewicht [ −  30,0kg + ] │
   │  Chips: [−2,5] [+2,5] [+5]  [Numpad]     │
   │  RPE 1····●····10  Leicht──────Limit [?] │
   │  [Notiz…]  (klappt auf)                  │
   │  [ W Warmup ]  [ Satz speichern ★ ]     │
   │  Pausentimer ○ 01:22  [+30s] [Überspringen] │
   └─────────────────────────────────────────┘
   [ + Übung hinzufügen ]  [ Als Vorlage speichern ]
   ```
   - **Studio-Wahl:** Erster Schritt nach `Training starten`, setzt `workouts.gym_id` (vor erstem Satz änderbar).
   - **Übungs-/Geräte-Picker:** `+ Übung hinzufügen` → Tabs `Dieses Studio` (global `gym_id NULL` + Maschinen dieses Studios) und `Alle`. **Zeile: Icon `ic_exercise_<icon_key>` links + Name + Badge `Gerät/Freihantel`**, Alias-Suche `Hackschmitt`. `+ Neu anlegen` → Dialog **Icon wählen (Grid 16)** + `Name, Kategorie, Art, Studio (vorausgefüllt), Aliase?` → `POST /exercises {icon_key}`. Optimistisch lokal.
   - **Pro Übung:** Header `Icon + Name + Gym`. Geist-Tippen füllt Gewicht, Chips/Numpad (`47,5` Komma), RPE-Slider + `Muskelversagen`, `Satz speichern` Haptik, Parser auch `12x35kg`.
   - **Pausentimer:** rund, `90s` pro Übung gemerkt, Notification `+30s/Überspringen` (`POST_NOTIFICATIONS` 37), Vibration 0.
   - **Weitere:** Swipe Übung↔Übung, sticky `Studio • Übung`, `Beenden` → `PATCH ended_at`; offline Queue; Zusammenfassung `Studio • Dauer • Volumen (KG)`; `● Offline` Banner oben wenn `GET http://192.168.178.8:8080/api/v1/health` fehlschlägt.

5. **Verlauf** — paginiert, nach Studio gruppiert (Filter `Alle / Thomas / All Inclusive`), **Zeile mit Icon** + Suche auch Alias, Wischen Löschen, Tippen → gleiches `gym_id` ins Training laden.

6. **Fortschritt** — Studio-Filter + Übungs-Suche (aliasfähig, **mit Icon**) + `Vico` Linie, `4W/12W/1J/Alle`, `e1RM/Volumen/Max`, PR-Abzeichen, Tabelle Rohsätze mit Studio-Spalte (Warmup grau, `0kg` ignoriert).

7. **Verwalten** — **Studios** (`+` anlegen) + **Übungen/Geräte** (nach Studio filtern, **Icon links**, `+ Neu` mit Icon-Wahl, Alias via `PUT /aliases`). System-Studios immer sichtbar, Icon später änderbar `PUT /exercises/{id} {icon_key}`.

**Data & sync**
- **Offline-first:** `SqlDelight` cache source of truth; `WorkoutRepository` lokal, `SyncWorker` (WorkManager + Tailnet-Probe `GET /health`) `POST` pending + `GET ?since=`, last-write-wins. Geräte/Übungen-Anlage optimistisch lokal, aliasfähig.
- **Reachability:** `GET http://192.168.178.8:8080/api/v1/health` (LXC Subnet 192.168.178.0/24, nur im Tailnet — Handy via VPN verifiziert 30.08; `ai-vm` Hostname via Subnet-DNS am Handy nicht auflösbar) — sonst `● Offline` Banner.
- **BuildConfig:** `API_BASE_URL = "http://192.168.178.8:8080/api/v1"` (`ssh ulrich@ai-vm` geht via LAN, aber App nutzt IP — am Handy verifiziert; kein MagicDNS), `KG` Parser `47,5→47.50`.

**Permissions:** `POST_NOTIFICATIONS` (Pausentimer), `INTERNET`; kein `ACTIVITY_RECOGNITION` v1. Strings Deutsch. Targets ≥48dp, TalkBack.

## 9. Visualization

- Library: `Vico 2.0` (CMP). Single chart per exercise/machine, drei Toggles:
  - **e1RM** (primär) — `weight*(1+reps/30)`, 7d geglättet; Warmup (`is_warmup`) + `weight_kg=0` (Bodyweight) **ausgeschlossen**.
  - **Gesamtvolumen** — `Σ reps*weight` pro Einheit.
  - **Maximalgewicht** — schwerster Arbeitssatz pro Einheit.
- X: Datum, Y: kg / kg×Wdh. Marker: PRs gelabelt, `is_failure` blass, Studio-Farbpunkt. Tabelle darunter für Roh-Export (CSV später) mit Studio-Spalte.
- Studio-Filter-Chip (`Alle / Thomas Sport Center / All Inclusive Fitness`) filtert Picker und Chart; z. B. Beinpresse bei All Inclusive vs generisch Kniebeuge eigene Linien. `GET /stats/progress?gymId=` liefert Daten.

## 10. Tailscale Networking — Subnet Router (kein Daemon auf `ai-vm`)

**Antwort auf deine Frage:** Nein, Tailscale muss **nicht** auf `ai-vm` laufen. Dein **LXC Subnet-Router** reicht völlig — er advertiert das LAN-Subnetz (z. B. `192.168.1.0/24` oder `ai-vm/32`), wird im Admin-Console **approved**, und Tailnet-Geräte erreichen `ai-vm` via `http://ai-vm:8080` (oder `192.168.1.x`) **nur wenn sie im Tailnet sind**. `ssh ulrich@ai-vm` funktioniert deshalb auch nur noch via Tailnet, wenn du die Route aktivierst.

- **LXC:** `tailscale up --advertise-routes=192.168.0.0/24` (oder konkretes Subnetz von `ai-vm`), dann in `https://login.tailscale.com/admin/machines` → `Edit route settings` → `Approve`.
- **ACL (`tailnet policy_tailnet.json`):** `{"src":["tag:ul-fitness-phone","autogroup:member"],"dst":["192.168.0.0/24:8080"]}` (DB `3306` nicht exponieren — nur api via subnet). Kein `0.0.0.0` Public.
- **DNS:** Kein MagicDNS `*.ts.net` für `ai-vm` selbst (daheim kein Daemon → kein `100.x`). App nutzt **`http://192.168.178.8:8080/api/v1`** (verifiziert am Handy via VPN 30.08; `http://ai-vm:8080` geht nur lokal/PC, nicht via Handy-Subnet). Fallback: `ai-vm` Hostname.
- **Verschlüsselung:** WireGuard verschlüsselt trotzdem den Tailnet-Tunnel, daher reicht **HTTP** im Tailnet (optional Caddy mit self-signed für `https://ai-vm` wenn gewünscht, aber nicht nötig).
- **Phone:** Tailscale installieren, selbes Tailnet, `Use subnet routes` aktivieren. Ohne Tailnet ist `ai-vm:8080` unerreichbar — genau die gewünschte Private-Only-Garantie.
- **Vorteil kein Daemon auf ai-vm:** Weniger Pflege auf ai-vm; Nachteil: kein `tailscale serve` HTTPS/Auto-Cert, kein `100.x` — deshalb hier `http` + Subnet.

## 11. Security & Private-Only

- **Perimeter:** `api` container lauscht auf `0.0.0.0:8080` aber ist nur via LAN/Subnet erreichbar — kein Port-Forward, kein öffentlicher DNS. `UFW` `deny 8080/tcp` nur für öffentliches Interface (Tailnet/LAN via Subnet bleibt erlaubt); `docker-compose` exponiert DB nicht. Beste Garantie ist **kein öffentliches Routing** + **Tailscale Subnet** als einziges Gateway.
- **App:** JWT in `EncryptedSharedPreferences`/`DataStore`, Refresh. `network_security_config.xml` erlaubt `http://192.168.178.8` (+ `ai-vm`) im Tailnet (cleartext, WireGuard verschlüsselt). `BuildConfig` Check: fails if URL not `192.168.178.8`/`ai-vm`.
- **Not runnable elsewhere:** Ohne aktives Tailnet + genehmigte Subnet-Route ist `ai-vm:8080` nicht routbar — genau private-only. Kein Funnel/Serve nötig weil kein Daemon auf ai-vm.
- **Secrets:** `.env` (`JWT_SECRET`, `MARIADB_ROOT_PASSWORD`) gitignored (`.gitignore` + `.env`).

## 12. Deployment (ai-vm, Docker) — per Entscheidung `~/ul-fitness`

**Host:** Ubuntu + Docker bereits vorhanden. Single `docker-compose.yml` unter **`~/ul-fitness`** (per `ssh ulrich@ai-vm` — einfach, kein `sudo`/`/opt`).

```yaml
services:
  db:
    image: mariadb:11
    env_file: .env
    environment:
      MARIADB_DATABASE: ul_fitness
    volumes: [db-data:/var/lib/mysql]
    restart: unless-stopped
  api:
    build: ./server
    env_file: .env # DB_URL=jdbc:mariadb://db:3306/ul_fitness, JWT_SECRET, ALLOW_REGISTER=false
    depends_on: [db]
    ports: ["8080:8080"] # LAN-only via LXC Subnet, kein Host-Tailscale, kein 0.0.0.0 public
    restart: unless-stopped
volumes: { db-data: {} }
# .env enthält: MARIADB_ROOT_PASSWORD, MARIADB_USER/PASSWORD, JWT_SECRET
```

 **TLS:** Kein `tailscale serve` auf `ai-vm` möglich (kein Daemon) → **HTTP im Tailnet** (`http://192.168.178.8:8080`, verifiziert) genügt. Optional Caddy self-signed später.

**Deploy:**
```bash
# on ai-vm (LAN via LXC subnet, works only when you are on tailnet: ssh ulrich@ai-vm)
git clone https://github.com/drepguy/ul-fitness.git && cd ul-fitness
cp .env.example .env && $EDITOR .env
docker compose up -d --build
docker compose logs -f api
curl http://192.168.178.8:8080/api/v1/health # {"status":"ok"}  # verifiziert PC+Handy via VPN
# vom Handy (Tailnet an, Subnet 192.168.178.0/24): http://192.168.178.8:8080/api/v1/health
```

**Backup:** `docker exec ul-fitness-db-1 mariadb-dump ul_fitness | gzip > backup.sql.gz` (**wöchentlich** per `cron` auf `ai-vm`, per Entscheidung — z. B. `crontab -e` → `0 3 * * 0 docker exec ...`).

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

- `.env` (gitignored, **KG-only**, **deutsch**): `MARIADB_ROOT_PASSWORD`, `MARIADB_USER`, `MARIADB_PASSWORD`, `JWT_SECRET` (32+ chars), `ALLOW_REGISTER=false`, `API_HOST=192.168.178.8` (IP via Subnet, am Handy verifiziert; `ai-vm` nur lokal).
- `local.properties`: `sdk.dir=C\:\\AndroidSDK` (existing, gitignored).
- **Aliase:** `exercise_aliases` Tabelle, Seed z. B. `Hackschmitt→Hackenschmidt`.
- **Bodyweight:** ignoriert für Stats (`weight_kg=0` → nicht in e1RM/Volumen).
- **Erledigt:** `.env` bereits in `.gitignore:12` (Commit `29feeaa`).

## 15. Testing

- Unit: `shared` domain (e1RM), `Flyway` migrations, Ktor routes (`testApplication`).
- Instrumented: Compose `createAndroidComposeRule` for Workout screen (tap log, timer).
- Manual: airplane mode → log 2 sets → reconnect Tailnet → `GET /workouts?since` returns synced.

## 16. Out of Scope (v1)

iOS/Desktop targets (KMP ready but ungenerated), body-metrics, CSV export, routines library, Wear, Health Connect.

## 17. Decisions Log

- RPE `1-10` (user: `1-10`), `is_failure` separate. Covers warm-up to limit. Per-set `is_warmup` added for logs like `0 und 15kg warmup`.
- Multi-user from schema start (FK `user_id`), v1 aber **nur du** ( `ALLOW_REGISTER=false` ).
- Tailscale over new WireGuard/OpenVPN (existing advertiser); `ai-vm` kein Daemon — LXC Subnet-Router reicht, `http://ai-vm:8080`.
- KMP + Ktor per user choice (shared Kotlin, no FastAPI).
- Vico over MPAndroidChart (CMP).
- **Gyms first-class (2026-08-29):** `gyms` table, seed `Thomas Sport Center` (Upper) + `All Inclusive Fitness` (Legs/Core) as `is_system`. `exercises.gym_id NULL` = global free-weight, otherwise machine tied to one gym (user wants to choose per-gym machines or create new). `workouts.gym_id` required. Picker filters `global + this-gym machines`, API validates mismatch. Covers your current 2-gym split but extensible N.
- **Machine/exercise input from real logs (2026-08-29):** See Appendix 19; decimal `47,5` comma, `body` weight (ignoriert), `y` typo handling → parser + `exercise_aliases` Tabelle.
- **Restliche Fragen 2026-08-30:** Deploy `~/ul-fitness` (einfach, `ssh ulrich@ai-vm`), Auth nur du v1, Vorlagen **beides** (letztes kopieren + gespeicherte `workout_templates` je Studio), TLS **HTTP** im Tailnet + **wöchentliches Backup**.
- **1 Gerät → n Übungen (2026-08-30):** Beinpresse horizontal/45° je für Quads + Waden = 4 Übungen auf 2 Geräten; plus sitzende Wadenheber + separate Waden Thomas. Modell `exercises(gym_id,name)` bildet das ab; Nutzer legt jederzeit neue `Geräte/Übungen` an.

## 18. Phases

- **P0 (0.5d):** Subnet-Route `192.168.178.0/24` in LXC approved (verifiziert `tailscale` + Handy `http://192.168.178.8:8080/api/v1/health` → `ok`), `docker-compose.yml` in `~/ul-fitness` health.
- **P1 (1.5d):** Flyway V1 (gyms + exercises.gym_id + workouts.gym_id + sets.is_warmup + exercise_aliases + workout_templates), seed `Thomas Sport Center` / `All Inclusive Fitness` + Aliase + Maschinen aus Anhang 19, JWT (nur du, `ALLOW_REGISTER=false`), gym/exercise/workout/template CRUD.
- **P2 (2.5d):** CMP scaffold (`shared/composeApp`), SQLDelight (gyms/aliases/templates), Trainingsscreen **Gym → Vorlage/Letztes → Übungs-Picker + inline anlegen** + `reps×weight` Parser (Komma) + Pausentimer + RPE 1-10 (Deutsch).
- **P3 (1d):** `SyncWorker`, `BuildConfig http://192.168.178.8:8080`, Offline-Banner.
- **P4 (1.5d):** Vico Fortschritt + PRs mit Studio-Filter (Warmup/Bodyweight ausgeschlossen).
- **P5 (0.5d):** LAN-only, `network_security_config.xml` für `http://192.168.178.8`, Signing, `AGENTS.md` Update (Module `shared`/`server`/`composeApp`, Deploy `ssh ulrich@ai-vm && cd ~/ul-fitness && docker compose up`), wöchentlicher Backup-Cron.

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
