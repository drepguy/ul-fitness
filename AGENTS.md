# AGENTS.md

## Project
- Single-module Android app (`:app`) — package `com.example.ul_fitness`, namespace `com.example.ul_fitness`.
- Entry point: `app/src/main/java/com/example/ul_fitness/MainActivity.java:5` — launcher `Activity` with `activity_main.xml` (dark-only start screen: centered `ic_ul_logo.xml` + "UL FITNESS" title, `AppCompatDelegate.MODE_NIGHT_YES`).
- AGP 9.3.2, Gradle 9.5.0, compileSdk/targetSdk 37, minSdk 24, `JavaVersion.VERSION_11` (toolchain resolved via foojay `gradle-daemon-jvm.properties` — JDK 25).
- Version catalog at `gradle/libs.versions.toml` — add deps there, reference via `libs.*` in `app/build.gradle.kts`.
- No git repo initialized (`git status` fails). No `opencode.json` / CI / lint config.

## Build & Verify
- Windows: `.\gradlew.bat <task>` · macOS/Linux: `./gradlew <task>`
- Build: `.\gradlew.bat assembleDebug` (release has `optimization { enable = false }`)
- Unit tests (host): `.\gradlew.bat :app:testDebugUnitTest` — single test: `.\gradlew.bat :app:testDebugUnitTest --tests "com.example.ul_fitness.ExampleUnitTest"`
- Instrumented tests (device/emulator required): `.\gradlew.bat :app:connectedDebugAndroidTest` — single: `--tests "com.example.ul_fitness.ExampleInstrumentedTest"`
- `gradle.properties` has `org.gradle.configuration-cache=true` — rerun with `--no-configuration-cache` if diagnosing cache issues.
- `local.properties` must point to real SDK (currently `C:\AndroidSDK`); old template value `C:\Users\Aolrich\...` does not exist on this machine — update `sdk.dir` on other machines; file is gitignored. `JAVA_HOME` not on `PATH` by default — use Android Studio JBR: `$env:JAVA_HOME="C:\Users\<user>\AppData\Local\Programs\Android Studio\jbr"` before `gradlew`.
- No lint/typecheck task configured beyond AGP defaults.

## Structure
- `app/build.gradle.kts:5` — `android { namespace, compileSdk, defaultConfig }`
- `app/src/main/AndroidManifest.xml` — launcher `MainActivity` with `MAIN/LAUNCHER` intent; `android:exported="true"` required for targetSdk 37
- `app/src/main/res/` — `drawable/ic_ul_logo.xml` (hexagon dumbbell logo), `layout/activity_main.xml`, `values/colors.xml` (`background_dark`), `values*/themes.xml` (dark `NoActionBar`)
- `app/src/main/keepRules/rules.keep` — R8 rules (AGP combines all under `keepRules/`)
- `settings.gradle.kts` — `pluginManagement` + `dependencyResolutionManagement` (FAIL_ON_PROJECT_REPOS)
