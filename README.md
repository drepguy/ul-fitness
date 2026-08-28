# UL Fitness

Private Android-Fitness-App (Deutsch, KG, RPE 1-10) — **nur im Tailscale-Tailnet** erreichbar. Trackt Sätze × Wdh × Gewicht je Übung/Gerät, gym-basiert, offline-first, mit Charts.

Kein Public-Hosting: Backend (Ktor + MariaDB) läuft per Docker auf `ai-vm` (`~/ul-fitness`), erreichbar nur via LXC Subnet-Router (`http://ai-vm:8080`, `ssh ulrich@ai-vm`).

> **Source of Truth:** [`spec.md`](spec.md) v0.4.3 — enthält Datenmodell, API, UI-Spec, Deployment. `AGENTS.md` ist kurz und verweist dorthin.

## Features (v1)

* **2 Studios** — `Thomas Sport Center` (Upper) + `All Inclusive Fitness` (Beine/Core), erweiterbar. `Studio → Vorlage/Letztes → Übungen` Flow; Übungen filtern nach Studio.
* **Übungen = Gerät + Bewegung** — 1 Gerät → n Übungen (z. B. `Beinpresse horizontal`/`Wadenpresse horizontal` + `Beinpresse 45°`/`Wadenpresse 45°` = 4), plus `Wadenheber sitzend` je Studio. **17 Icons** (`dumbbell`, `leg_press`, `calf`, `chest_press`, …) + **Alias-Suche** (`Hackschmitt→Hackenschmidt`).
* **Schnell-Logging:** Geist-Satz übernehmen, Chips `−2,5/+2,5/+5`, Numpad `47,5`, RPE 1-10 (`Leicht…Limit`) + Muskelversagen, Notiz, `W` Warmup (ausgegraut, nicht in PR), Pausentimer 90s + Notification `+30s/Überspringen`.
* **Vorlagen beides:** letztes Training kopieren + gespeicherte `workout_templates` pro Studio.
* **Offline-first:** `SqlDelight` Cache + `SyncWorker` (nur wenn Tailnet erreichbar), Charts filtern nach Studio (Vico), PRs.

## Architektur

```
composeApp (Deutsch) ──JWT/HTTP──► ai-vm:8080 (Ktor) ──► db:3306 (MariaDB 11)
   shared (KMP)              http://ai-vm via LXC Subnet-Router (kein Daemon auf ai-vm)
```

Geplant: `settings.gradle.kts → :shared + :composeApp + :server`. Aktuell nur `:app` (Splash).

## Projektstruktur

```
ul-fitness/
├── app/src/main/java/com/example/ul_fitness/MainActivity.java  # heller Splash (dark)
├── app/src/main/res/drawable/ic_ul_logo.xml + ic_exercise_*.xml (17 Icons)
├── gradle/libs.versions.toml, AGP 9.3.2 / Gradle 9.5.0 / SDK 37 / JDK 25
├── spec.md, AGENTS.md, .env.example
└── (geplant) shared/, composeApp/, server/, docker-compose.yml
```

## Voraussetzungen

* Android Studio + JDK 25 (via foojay), `local.properties:sdk.dir=C\:\\AndroidSDK`.
* `ai-vm` Ubuntu mit Docker, Tailscale LXC Subnet-Router freigegeben.

## Android lokal

```powershell
$env:JAVA_HOME="C:\Users\<user>\AppData\Local\Programs\Android Studio\jbr"
.\gradlew.bat assembleDebug
.\gradlew.bat :app:testDebugUnitTest --tests "com.example.ul_fitness.ExampleUnitTest"
# Gerät/Emulator
.\gradlew.bat :app:connectedDebugAndroidTest --tests "com.example.ul_fitness.ExampleInstrumentedTest"
```

## Backend auf ai-vm (nur im Tailnet)

```bash
ssh ulrich@ai-vm
git clone https://github.com/drepguy/ul-fitness.git ~/ul-fitness && cd ~/ul-fitness
cp .env.example .env && $EDITOR .env   # MARIADB_* / JWT_SECRET (32+ chars) / ALLOW_REGISTER=false
docker compose up -d --build
docker compose logs -f api
curl http://ai-vm:8080/api/v1/health  # {"status":"ok"}
# Backup wöchentlich: 0 3 * * 0 docker exec ul-fitness-db-1 mariadb-dump ul_fitness | gzip > backup.sql.gz
```

`.env` ist gitignored.

## Nutzung (kurz)

1. **Training starten** → Studio wählen → `Letztes kopieren` oder `Vorlage`.
2. `+ Übung hinzufügen` → Tab `Dieses Studio`/`Alle`, Alias-Suche, `+ Neu` (Icon + Name + Art + Studio).
3. Satz loggen: Geist tippen, Gewicht/Wdh anpassen (`12x35` Parser), RPE 1-10, `Satz speichern` (haptisch) → Timer.
4. `Beenden` → Verlauf/Fortschritt filtern nach Studio; Verwalten → Studios/Übungen/Aliase.

## Icons

`app/src/main/res/drawable/ic_exercise_*.xml` (Vektor, weiß auf dunkel): `dumbbell`, `barbell`, `leg_press`, `leg_ext`, `leg_curl`, `calf`, `hip_thrust`, `chest_press`, `lat_pull`, `row`, `shoulder_press`, `lateral_raise`, `face_pull`, `bicep_curl`, `triceps`, `ab_machine`, `hyperext`. `icon_key` in `exercises`, wählbar im `Neu anlegen`-Dialog.

## Build & Verify

Siehe `AGENTS.md` für exakte Befehle. `org.gradle.configuration-cache=true` — bei Problemen `--no-configuration-cache`.

## Lizenz

Privat — kein öffentliches Hosting, nur Tailnet.

