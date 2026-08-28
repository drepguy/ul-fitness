# UL Fitness — Spec

**Version:** 0.2 — 2026-08-29  
**Status:** Draft (agreed stack: Kotlin Compose Multiplatform + Ktor + MariaDB, Tailscale LXC advertiser; RPE 1-10; 2 gyms + extensible)  
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
| Infra | Docker Compose on ai-vm + Caddy or `tailscale serve` | Single compose, reproducible | — |
| Net | **Tailscale** (existing LXC advertiser, MagicDNS `*.ts.net`) | No new VPN; `100.x.x.x` only | — |

## 5. Architecture

```
composeApp (Android)                    ai-vm (Ubuntu, Docker)
┌─ shared/commonMain ─┐                ┌─ docker-compose.yml ─┐
│ domain/model        │   Ktor client  │ api:ktor:8080 ──► db:3306 (mariadb:11, vol db-data)
│ data/SqlDelight     │ ──JWT/HTTPS──► │   Exposed+Flyway+JWT  UFW deny 0.0.0.0:8080
│ ui/Compose (Vico)   │  100.x / *.ts.net │  bind = tailscale0 IP only  Tailnet ACL
└─────────────────────┘                └──────────────────────┘
         ▲ WorkManager SyncWorker (only when tailnet reachable, last-write-wins)
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
  weight_kg DECIMAL(5,2) NOT NULL CHECK (weight_kg >= 0),
  rpe TINYINT NULL CHECK (rpe BETWEEN 1 AND 10),
  is_failure BOOLEAN NOT NULL DEFAULT FALSE,
  note TEXT NULL, -- short note per set (e.g. "slow eccentric")
  created_at DATETIME(6) NOT NULL,
  FOREIGN KEY (workout_exercise_id) REFERENCES workout_exercises(id) ON DELETE CASCADE
);
-- V2 candidate: body_metrics(id,user_id,date,weight_kg,body_fat)
-- seed in V1:
-- INSERT INTO gyms(owner_id,name,is_system) VALUES (NULL,'Thomas Sport Center',TRUE),(NULL,'All Inclusive Fitness',TRUE);
-- INSERT INTO exercises(owner_id,gym_id,name,category,kind,is_system) VALUES
--   (NULL,NULL,'Bench Press','push','free_weight',TRUE),
--   (NULL,NULL,'Squat','legs','free_weight',TRUE),
--   (NULL,(SELECT id FROM gyms WHERE name='All Inclusive Fitness'),'Leg Press','legs','machine',TRUE), ...
```

**Gym modeling:** `gyms` first-class, extensible N. Seed the two current gyms as `is_system` so every user sees them; `Thomas Sport Center` default tag = Upper, `All Inclusive Fitness` = Legs+Core but not enforced — user can do any category at either gym. `exercises.gym_id` distinguishes global free-weights (`NULL`, show at any gym) vs gym-specific machines (e.g. “Chest Press #2” `gym_id=All Inclusive` only appears when that gym is selected; global search can still find it). Validation: `workouts.gym_id` must match or be `NULL` for exercises added to that workout if `exercises.gym_id IS NOT NULL`.

**Conventions:** UTC `DATETIME(6)`, `DECIMAL(5,2)` for kg (allows 999.99). System exercises/machines seeded for both gyms.

**RPE 1-10:** User decision. `1` = minimal effort / warm-up, `10` = limit / could not do one more rep. `is_failure` separate because failure can happen at any RPE; chart can show RPE trend. Null = not recorded (v1 optional but stored).

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

**Exercises / Machines** (unified)
- `GET /exercises?gymId=&q=&category=&include_system=true` → `[{id,name,category,kind,gym_id,gym_name,is_system,owner_id}]` — when `gymId` set, returns `gym_id IS NULL` (global) **plus** `gym_id = ?` (machines for that gym), sorted machines last.
- `POST /exercises {name,category,kind,gym_id?}` → `201 {id}` — `kind=machine` requires `gym_id`; `kind=free_weight/bodyweight` usually `gym_id=NULL` (allow any gym). Validation: `gym_id` must be system or owned by me.
- `PUT /exercises/{id} {name,category,kind,gym_id}` (only own) → `200`
- `DELETE /exercises/{id}` (only own, soft if referenced)

**Workouts**
- `POST /workouts {gym_id, started_at, notes?, exercises:[{exerciseId, sets:[{reps,weight_kg,rpe?,is_failure,note?}]}]}` → `201 {id}` — validates `exercise.gym_id IS NULL OR = workouts.gym_id`; `409` if machine from other gym.
- `GET /workouts?gymId=&from=&to=&limit=&offset=&since=` (own only) → list, supports `since` for sync; `gymId` filter optional.
- `GET /workouts/{id}` (full graph + `gym` + `gym_name` at top)
- `PATCH /workouts/{id}/finish {ended_at}` / `PATCH /workouts/{id} {gym_id, notes}` (pre-finish)
- `DELETE /workouts/{id}`

**Stats** (gym-aware)
- `GET /stats/progress?exerciseId=&gymId=&period=90d&metric=e1RM|volume|max` → `[{date, value, reps, weight_kg}]` (`e1RM = weight*(1+reps/30)` Epley; volume = Σ `reps*weight`) — `gymId` optional filter.
- `GET /stats/prs?exerciseId=&gymId=` → `{maxWeight:{value,date,gym}, maxE1RM:{}, maxVolume:{}}`

**Conventions:** `401` on bad JWT, `403` on not own resource, `409` on duplicate `(owner,gym,name)` or machine-gym mismatch, `X-Request-Id` logs.

## 8. App Spec

**Screens (Compose)**
1. **Start** — keep `ic_ul_logo.xml` → Compose vector, `UL FITNESS` title, dark `background_dark #0F0F0F` (unchanged `themes.xml:3`), `AppCompatDelegate.MODE_NIGHT_YES` remains.
2. **Login/Register** — email+password, `DataStore` token, auto-refresh.
3. **Home** — gym quick-picks (`Thomas Sport Center` / `All Inclusive Fitness` chips, plus `+` for new gym), today CTA `Start workout`, recent workouts grouped by gym, last PR snippet.
4. **Workout (focus)** — *ease during workout is #1:*
   - **Gym picker (first step):** `Start workout` → select gym (Thomas / All Inclusive / other created). Choice sets `workouts.gym_id`, determines available machines. Can change before first set. Remembers last gym.
   - **Exercise/Machine picker:** `+ Add exercise` opens sheet: two tabs/search scopes — `This Gym` (global `gym_id NULL` + machines for this gym) and `All` (global search). List shows `kind` badge (machine vs free_weight). Inline `+ Create new` → dialog `Name, Category (push/pull/legs/core...), Kind (free_weight/machine/cable/bodyweight), Gym (pre-filled current, editable)` → `POST /exercises`. Optimistic local insert, queued sync.
   - Per exercise block: previous set ghost (tap to copy weight), reps stepper `−/ +` (big 56dp), weight chips `−2.5/ +2.5 / +5`, numpad fallback, `RPE 1-10` slider (ticks 1-10 labels) + `Failure` checkbox, per-set `note` (expandable `TextField`), `Log set` haptic.
   - Rest timer: circular `Vico`/`CircularProgress`, `90s` default (per-exercise remember), notification `Add 30s / Skip` (`POST_NOTIFICATIONS` on SDK 37), auto-start on log, vibrate at 0.
   - Swipe between exercises, sticky exercise header with `gym_name • exercise_name`.
   - `Finish` → PATCH `ended_at`; offline → queue. Shows summary `Gym • duration • volume`.
5. **History** — paged list grouped by gym (filter chip `All / Thomas / All Inclusive`), search, swipe delete, edit (reload into Workout with same `gym_id`).
6. **Progress** — gym filter + exercise/machine selector (filtered by gym) + `Vico` line chart, period `4W/12W/1Y/All`, metrics `e1RM/Volume/Max`, PR badges, table of raw sets with gym column.
7. **Manage** — two sections: **Gyms** (`+` create, rename own, hides system only via filter) and **Exercises/Machines** (filter by gym chip, create, hide system). Seed management: system Thomas/All Inclusive always visible.

**Data & sync**
- **Offline-first:** `SqlDelight` cache is source of truth; `WorkoutRepository` writes locally, `SyncWorker` (WorkManager, `NetworkType.CONNECTED` + tailnet probe `GET /health`) does `POST` pending + `GET /workouts?since=lastSync`. Conflict: last-write-wins (safe for personal).
- **Reachability check:** `GET https://<MagicDNS>/api/v1/health` (no auth) — if `100.x` DNS fails, show `● Offline` banner (not error).
- **BuildConfig:** `API_BASE_URL = "https://ul-fitness.<tailnet>.ts.net/api/v1"` (or `ai-vm.<tailnet>.ts.net`), generated per buildType; no public fallback.

**Permissions:** `POST_NOTIFICATIONS` (rest timer), `INTERNET`; no `ACTIVITY_RECOGNITION` in v1.

## 9. Visualization

- Library: `Vico 2.0` (CMP). Single chart per exercise/machine, three toggles:
  - **e1RM** (primary) — `weight*(1+reps/30)`, smoothed 7d avg.
  - **Total volume** — `Σ reps*weight` per session.
  - **Max weight** — top set per session.
- X: date, Y: kg / kg×reps. Markers: PRs with label, failed sets dimmed, gym color dot. Table below chart for raw export (CSV later) with gym column.
- Gym filter chip (`All / Thomas Sport Center / All Inclusive Fitness`) narrows picker and chart; e.g. Leg Press at All Inclusive vs generic Squat shows distinct lines. `GET /stats/progress?gymId=` powers it.

## 10. Tailscale Networking

- Existing LXC advertiser already distributes `ai-vm` subnet to tailnet — no new VPN install.
- **ACL (`tailnet policy`):** `{"src":["tag:ul-fitness-phone","autogroup:member"],"dst":["tag:ai-vm:8080,3306"]}` (restrict DB to api only in practice).
- **DNS:** MagicDNS `ai-vm.<tailnet>.ts.net` → `100.x.x.x`; app uses HTTPS. No `0.0.0.0` exposure.
- **Phone:** Install Tailscale, login same tailnet, approve subnet routes.

## 11. Security & Private-Only

- **Perimeter:** `api` container binds **only** to `tailscale0` IP (compose `network_mode: host` + `HOST=100.x` or `ports: ["100.x:8080:8080"]`), `UFW` `deny 8080/tcp` from public; `docker-compose` not exposing DB.
- **App:** JWT stored `EncryptedSharedPreferences`/`DataStore`, refresh. `network_security_config.xml` pins Tailscale CA / uses Tailscale-issued cert (Caddy). `BuildConfig` check fails build if URL not `*.ts.net`/`100.*`.
- **Not runnable elsewhere:** Without tailnet cert + route, API unreachable. Optional extra: validate `Tailscale-User-Login` header server-side.
- **Secrets:** `.env` (`JWT_SECRET`, `MARIADB_ROOT_PASSWORD`) gitignored (`.gitignore:11` already `local.properties`; add `.env`).

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

**TLS:** `tailscale serve --https=443 --bg localhost:8080` **or** Caddy with Tailscale certs. Prefer `tailscale serve` (no extra cert handling).

**Deploy:**
```bash
# on ai-vm over Tailscale SSH
git clone https://github.com/drepguy/ul-fitness.git && cd ul-fitness
cp .env.example .env && $EDITOR .env
docker compose up -d --build
docker compose logs -f api
curl -k https://<MagicDNS>/api/v1/health # {"status":"ok"}
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

- `.env` (gitignored): `MARIADB_ROOT_PASSWORD`, `MARIADB_USER`, `MARIADB_PASSWORD`, `JWT_SECRET` (32+ chars), `HOST_TAILSCALE_IP`.
- `local.properties`: `sdk.dir=C\:\\AndroidSDK` (existing, gitignored).

Add to `.gitignore`: `.env` (todo).

## 15. Testing

- Unit: `shared` domain (e1RM), `Flyway` migrations, Ktor routes (`testApplication`).
- Instrumented: Compose `createAndroidComposeRule` for Workout screen (tap log, timer).
- Manual: airplane mode → log 2 sets → reconnect Tailnet → `GET /workouts?since` returns synced.

## 16. Out of Scope (v1)

iOS/Desktop targets (KMP ready but ungenerated), body-metrics, CSV export, routines library, Wear, Health Connect.

## 17. Decisions Log

- RPE `1-10` (user: `1-10`), `is_failure` separate. Covers warm-up to limit.
- Multi-user from schema start (FK `user_id`).
- Tailscale over new WireGuard/OpenVPN (existing advertiser).
- KMP + Ktor per user choice (shared Kotlin, no FastAPI).
- Vico over MPAndroidChart (CMP).
- **Gyms first-class (2026-08-29):** `gyms` table, seed `Thomas Sport Center` (Upper) + `All Inclusive Fitness` (Legs/Core) as `is_system`. `exercises.gym_id NULL` = global free-weight, otherwise machine tied to one gym (user wants to choose per-gym machines or create new). `workouts.gym_id` required. Picker filters `global + this-gym machines`, API validates mismatch. Covers your current 2-gym split but extensible N.

## 18. Phases

- **P0 (0.5d):** Confirm MagicDNS name, `docker-compose.yml` health, `GET /health` over Tailnet.
- **P1 (1.5d):** Flyway V1 (gyms + exercises.gym_id + workouts.gym_id), seed `Thomas Sport Center` / `All Inclusive Fitness` + system exercises/machines, JWT, gym/exercise/workout CRUD.
- **P2 (2.5d):** CMP scaffold (`shared/composeApp`), SQLDelight (gyms+migrations), Workout screen with **gym picker → exercise/machine picker (filtered) + inline create** + rest timer + RPE 1-10.
- **P3 (1d):** `SyncWorker`, MagicDNS `BuildConfig`, offline banner.
- **P4 (1.5d):** Vico progress + PRs with **gym filter**.
- **P5 (0.5d):** Bind-to-tailnet, pinning, signing, `AGENTS.md` update (add `shared`/`server`, deploy cmd).

---

*Next: implement P0-P1 — scaffold `server`/KMP, `.env`, `docker-compose.yml`, `GET /health` over Tailnet for sign-off before tracking UI.*
