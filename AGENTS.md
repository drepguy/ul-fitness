# AGENTS.md

## Project
- KMP Android app (`:app`) + shared module (`:shared`) + Ktor server (`:server`) — package `com.example.ul_fitness`, namespace `com.example.ul_fitness`.
- Entry point: `app/src/main/java/com/example/ul_fitness/MainActivity.kt` — Kotlin Compose with `ComponentActivity`, Material3 dark theme, `TrainingApp()` composable.
- AGP 9.3.2, Gradle 9.5.0, compileSdk/targetSdk 37, minSdk 24, `JavaVersion.VERSION_11`.
- Version catalog at `gradle/libs.versions.toml` — add deps there, reference via `libs.*`.
- Git: `https://github.com/drepguy/ul-fitness.git` (`main`) — private; `spec.md:1` is source of truth (Deutsch, KG, RPE 1-10, 2 Studios). `.env` gitignored.

## Build & Verify
- Windows: `.\gradlew.bat <task>` · macOS/Linux: `./gradlew <task>`
- App build: `.\gradlew.bat :app:assembleDebug`
- Server build: `.\gradlew.bat :server:build -x test`
- Both: `.\gradlew.bat :app:assembleDebug :server:build -x test`
- Unit tests (host): `.\gradlew.bat :app:testDebugUnitTest`
- Instrumented tests (device/emulator required): `.\gradlew.bat :app:connectedDebugAndroidTest`
- `gradle.properties` has `org.gradle.configuration-cache=true` — use `--no-configuration-cache` for debugging.
- `local.properties` must point to real SDK (currently `C:\AndroidSDK`). `JAVA_HOME` not on `PATH` — use Android Studio JBR: `$env:JAVA_HOME="C:\Users\<user>\AppData\Local\Programs\Android Studio\jbr"`.
- No lint/typecheck task configured beyond AGP defaults.

## Server (Ktor + Exposed + Flyway + MariaDB)
- Runs on `ai-vm` at `http://192.168.178.8:8080` (VPN-only, Tailscale subnet `192.168.178.0/24`).
- `server/Dockerfile` — multi-stage build (eclipse-temurin:17). Deploy via: `docker compose build --no-cache api && docker compose up -d`.
- `server/src/main/resources/db/migration/V1__init.sql` — schema + seed data (2 gyms, 10 exercises+aliases).
- `server/src/main/resources/db/migration/V2__category_to_varchar.sql` — `category` column changed from `ENUM` to `VARCHAR(20)` for German labels.
- User: `ulrich@ulf.local` / `UlFitness2026!` (`ALLOW_REGISTER=false` after seed).
- JWT: `java-jwt 4.4.0`, secret from env `JWT_SECRET`, access=15min, refresh=30d.

### API (all routes under `/api/v1`, JSON camelCase)
- `POST /auth/register` (403 when `ALLOW_REGISTER=false`), `POST /auth/login` → tokens, `POST /auth/refresh`
- `GET /gyms`, `POST /gyms`, `PUT /gyms/{id}`, `DELETE /gyms/{id}`
- `GET /exercises?gymId=&q=&category=`, `POST /exercises`, `PUT /exercises/{id}`, `DELETE /exercises/{id}`
- `GET /exercises/{id}/aliases`, `PUT /exercises/{id}/aliases`
- `GET /workouts?gymId=&limit=&offset=&since=`, `POST /workouts`, `GET /workouts/{id}`, `PATCH /workouts/{id}`, `PATCH /workouts/{id}/finish`, `DELETE /workouts/{id}`, `POST /workouts/from-last`
- `PUT /workouts/{id}/exercises` — replaces all exercises+sets for a workout (validates ownership)
- `GET /templates?gymId=`, `POST /templates`, `PUT /templates/{id}`, `DELETE /templates/{id}`, `POST /templates/{id}/start`
- `GET /stats/progress?exerciseId=&from=&to=`, `GET /stats/prs?gymId=&limit=`

### Known Issues
- **`call.receive<T>()` broken** — Ktor content negotiation fails for all mutation routes. All routes use `call.receiveText()` + manual `Json.decodeFromString` as workaround.
- **`composeBom` 2024.09.03** — Compose dependencies added but no full training flow yet.

## Structure
- `app/build.gradle.kts` — `android { namespace, compileSdk, defaultConfig }`, Compose BOM + Material3 + activity-compose + navigation-compose
- `shared/build.gradle.kts` — KMP JVM-only (serialization + coroutines), targets: `jvm()`
- `server/build.gradle.kts` — Ktor server (Netty, Exposed, Flyway, HikariCP, MariaDB, JWT, jbcrypt)
- `server/src/main/kotlin/com/example/ul_fitness/` — `Application.kt`, `DatabaseFactory.kt`, `db/Tables.kt`, `routes/*`, `security/JwtConfig.kt`
- `app/src/main/java/com/example/ul_fitness/` — `MainActivity.kt` (NavHost + BottomBar + HomeScreen + ActiveWorkoutScreen + WorkoutDetailScreen + ExerciseCard), `ApiClient.kt`, `LoginScreen.kt`, `ExerciseListScreen.kt`
- `app/src/main/res/` — `drawable/ic_ul_logo.xml`, `layout/activity_main.xml`, `values/colors.xml`, `values*/themes.xml`
- `app/src/main/keepRules/rules.keep` — R8 rules
- `docker-compose.yml` — db (mariadb:11) + api, port `8080` LAN-only
