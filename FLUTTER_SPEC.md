# UL Fitness – Flutter Standalone App Spec

> **Zielplattform:** Android (Flutter)  
> **Datenhaltung:** Lokal (SQLite / Drift) — kein Server, kein Internet nötig  
> **Sprache:** Deutsch (UI), Englisch (Code)  
> **Modus:** Single-User — kein Login, kein Auth

---

## 1. Architektur

```
lib/
├── main.dart
├── models/           # Datenbank-Entitys & DTOs
│   ├── gym.dart
│   ├── exercise.dart
│   ├── exercise_alias.dart
│   ├── workout.dart
│   ├── workout_exercise.dart
│   ├── workout_set.dart
│   └── workout_template.dart
├── database/         # Drift (SQLite) Database + DAOs
│   ├── app_database.dart
│   └── daos/
├── screens/          # Alle Bildschirme
│   ├── home_screen.dart
│   ├── active_workout_screen.dart
│   ├── workout_detail_screen.dart
│   ├── exercise_list_screen.dart
│   ├── analyse_screen.dart
│   └── splash_screen.dart
├── widgets/          # Wiederverwendbare Komponenten
│   ├── exercise_card.dart
│   ├── exercise_picker_dialog.dart
│   ├── exercise_edit_dialog.dart
│   ├── finish_dialog.dart
│   ├── rest_timer_overlay.dart
│   ├── simple_line_chart.dart
│   ├── pr_card.dart
│   └── stat_card.dart
├── utils/            # Hilfsfunktionen
│   ├── constants.dart
│   └── formatters.dart
└── theme/            # Farben, TextStyles, Theme
    └── app_theme.dart
```

**Empfohlene Libraries:**
| Library | Zweck |
|---|---|
| `drift` (ehem. moor) | SQLite ORM mit Type-Safety |
| `sqlite3_flutter_libs` | Native SQLite für Android |
| `path_provider` | Datenbank-Pfad |
| `fl_chart` | Diagramme (besser als Canvas-Canvas) oder Custom Canvas |
| `intl` | Datums-/Zahlenformatierung |
| `uuid` | Eindeutige IDs |
| `material3` / `flutter_material_pickers` | DropDowns, DatumsPicker |

---

## 2. Datenbank-Schema (SQLite)

### Tabelle: `gyms`
| Spalte | Typ | Constraints |
|---|---|---|
| `id` | INTEGER | PK, autoincrement |
| `name` | TEXT | NOT NULL |
| `city` | TEXT | NULLABLE |
| `is_system` | INTEGER | NOT NULL, DEFAULT 0 |
| `created_at` | TEXT | NOT NULL (ISO8601) |

### Tabelle: `exercises`
| Spalte | Typ | Constraints |
|---|---|---|
| `id` | INTEGER | PK, autoincrement |
| `gym_id` | INTEGER | NULLABLE, FK → gyms(id) ON DELETE SET NULL |
| `name` | TEXT | NOT NULL |
| `category` | TEXT | NOT NULL, DEFAULT 'Sonstiges' |
| `kind` | TEXT | NOT NULL, DEFAULT 'free_weight' |
| `icon_key` | TEXT | NOT NULL, DEFAULT 'dumbbell' |
| `is_system` | INTEGER | NOT NULL, DEFAULT 0 |
| `created_at` | TEXT | NOT NULL |

### Tabelle: `exercise_aliases`
| Spalte | Typ | Constraints |
|---|---|---|
| `id` | INTEGER | PK, autoincrement |
| `exercise_id` | INTEGER | NOT NULL, FK → exercises(id) ON DELETE CASCADE |
| `alias` | TEXT | NOT NULL |
| `created_at` | TEXT | NOT NULL |

Unique: `(alias, exercise_id)`

### Tabelle: `workouts`
| Spalte | Typ | Constraints |
|---|---|---|
| `id` | INTEGER | PK, autoincrement |
| `gym_id` | INTEGER | NULLABLE, FK → gyms(id) ON DELETE SET NULL |
| `started_at` | TEXT | NOT NULL (ISO8601) |
| `ended_at` | TEXT | NULLABLE (ISO8601) |
| `notes` | TEXT | NULLABLE |
| `created_at` | TEXT | NOT NULL |

Index: `(gym_id, started_at)`

### Tabelle: `workout_exercises`
| Spalte | Typ | Constraints |
|---|---|---|
| `id` | INTEGER | PK, autoincrement |
| `workout_id` | INTEGER | NOT NULL, FK → workouts(id) ON DELETE CASCADE |
| `exercise_id` | INTEGER | NOT NULL, FK → exercises(id) |
| `order_idx` | INTEGER | NOT NULL |

### Tabelle: `workout_sets`
| Spalte | Typ | Constraints |
|---|---|---|
| `id` | INTEGER | PK, autoincrement |
| `workout_exercise_id` | INTEGER | NOT NULL, FK → workout_exercises(id) ON DELETE CASCADE |
| `set_no` | INTEGER | NOT NULL |
| `reps` | INTEGER | NOT NULL, CHECK ≥ 0 |
| `weight_kg` | REAL | NOT NULL, CHECK ≥ 0 |
| `is_warmup` | INTEGER | NOT NULL, DEFAULT 0 |
| `rpe` | INTEGER | NULLABLE, CHECK 1–10 |
| `is_failure` | INTEGER | NOT NULL, DEFAULT 0 |
| `note` | TEXT | NULLABLE |
| `created_at` | TEXT | NOT NULL |

### Tabelle: `workout_templates`
| Spalte | Typ | Constraints |
|---|---|---|
| `id` | INTEGER | PK, autoincrement |
| `gym_id` | INTEGER | NOT NULL, FK → gyms(id) |
| `name` | TEXT | NOT NULL |
| `created_at` | TEXT | NOT NULL |
| `updated_at` | TEXT | NOT NULL |

Unique: `(gym_id, name)`

### Tabelle: `workout_template_exercises`
| Spalte | Typ | Constraints |
|---|---|---|
| `id` | INTEGER | PK, autoincrement |
| `template_id` | INTEGER | NOT NULL, FK → workout_templates(id) ON DELETE CASCADE |
| `exercise_id` | INTEGER | NOT NULL, FK → exercises(id) |
| `order_idx` | INTEGER | NOT NULL |
| `default_sets` | INTEGER | NOT NULL, DEFAULT 3 |
| `default_reps` | INTEGER | NULLABLE |
| `default_weight_kg` | REAL | NULLABLE |

---

## 3. Seed-Daten (beim ersten Start)

### Studios
| ID | Name | Stadt |
|---|---|---|
| 1 | Thomas Sport Center | NULL |
| 2 | All Inclusive Fitness | NULL |

### Übungen (mit Kategorie, Art, Icon, Aliase)

**Beine:**
| Name | Kind | Icon | Aliase |
|---|---|---|---|
| Hackenschmidt | free_weight | leg_press | Hackschmitt, Hack Squat |
| Hip Thrust Machine | machine | leg_press | — |
| Beinpresse horizontal | machine | leg_press | Beinpresse |
| Wadenpresse horizontal | machine | leg_press | — |
| Beinpresse 45° | machine | leg_press | — |
| Wadenpresse 45° | machine | leg_press | — |
| Wadenheber sitzend | machine | leg_press | — |
| Beinstrecker | machine | leg_press | — |
| Beinbeuger | machine | leg_press | — |
| Beinbeuger liegend | machine | leg_press | — |
| Beinpresse | machine | leg_press | — |
| Wadenmaschine | machine | leg_press | — |

**Push:**
| Name | Kind | Icon | Aliase |
|---|---|---|---|
| Brustpresse | machine | bench | Brust |
| Brustfly | machine | bench | Chest fly |
| Schulterpresse | machine | dumbbell | — |
| Seitheben | cable | dumbbell | — |
| Trizeps Skull Crush | free_weight | dumbbell | Skullcrusher |
| Trizeps Kabelzug | cable | cable | — |

**Pull:**
| Name | Kind | Icon | Aliase |
|---|---|---|---|
| Latzug | machine | pull_up | — |
| Rudern | machine | cable | — |
| Face Pulls | cable | cable | — |
| Bizeps Hammer Curls | free_weight | dumbbell | — |
| Bizeps Kabelzug | cable | cable | — |
| Rudern Brustgestützt | machine | cable | — |

**Core:**
| Name | Kind | Icon | Aliase |
|---|---|---|---|
| Hyperextension | machine | bench | — |
| Bauch | machine | dumbbell | — |
| Bauchmaschine | machine | dumbbell | — |

**Unterarme:**
| Name | Kind | Icon | Aliase |
|---|---|---|---|
| Unterarm-Innencurls | free_weight | dumbbell | — |
| Unterarm-Außencurls | free_weight | dumbbell | — |

---

## 4. Bildschirme & Funktionen

### 4.1 Splash Screen
- Logo (SVG/PNG) mit Puls-Animation (Alpha + Scale, 2 Sek.)
- App-Name "UL Fitness"
- Lade-Indikator
- Mindestanzeigezeit: 2 Sekunden

### 4.2 Home Screen (Tab: Training)

**Header:**
- "Willkommen!" (headlineLarge, bold)
- Untertitel: "Bleib stark. Trainier konsequent." (bodyLarge, muted)

**"Training starten" Bereich:**
- Pro Studio eine Card mit:
  - Play-Icon in farbigem Quadrat (primaryContainer)
  - Studio-Name (bold)
  - Stadt (falls vorhanden)
  - Tipp-Callback → navigiert zu ActiveWorkoutScreen

**"Vorlagen" Bereich (NEU):**
- Pro Studio: Vorlagen als horizontale Chips
- Tipp auf Chip → startet Workout aus Vorlage (kopiert Übungsliste ohne Sätze)
- "Neue Vorlage" Chip mit + Icon → öffnet Template-Editor

**"Letzte Trainings" Bereich:**
- Max. 20 Einträge, sortiert nach neuestem
- Pro Card:
  - Studio-Name (bold)
  - Status-Badge: "Fertig" (primary) / "Aktiv" (teal/secondary)
  - Formatierter Startzeitpunkt (dd.MM.yyyy HH:mm)
  - Notizen (falls vorhanden)
  - Tipp-Callback → WorkoutDetailScreen

### 4.3 Aktives Workout Screen

**Header:**
- Studio-Name als headline

**Leerer Zustand:**
- Zentriert: Add-Icon + "Übung hinzufügen um zu starten"

**Übungs-Cards (pro hinzugefügter Übung):**

*Header:*
- Sekundärfarbiges Icon-Quadrat
- Übungsname (bold)
- Kategorie (muted)
- Löschen-Button (Mülleimer, error-farben)

*Satz-Tabelle (in abgerundetem Container):*
- Spaltenüberschriften: "Sat." / "Wdh." / "kg" / "RPE" / (Löschen)
- Pro Satz:
  - Satznummer (bold, primary-farbe)
  - Wiederholungen (OutlinedTextField, numeric, zentriert)
  - Gewicht in kg (OutlinedTextField, decimal, zentriert)
  - RPE 1–10 (OutlinedTextField, numeric, zentriert, Placeholder "-")
  - Warmup-Toggle (Chip: "Aufwärmen") — **NEU**
  - Muskelversagen-Toggle (Chip: "Zum Versagen") — **NEU**
  - Satz-Notiz ( TextField, optional, inline ) — **NEU**
  - Löschen-Button (X-Icon)

*"Satz" Button:*
- TextButton unten rechts
- Fügt neuen Satz hinzu
- **Ghost-Daten:** Beim Hinzufügen wird der letzte gespeicherte Satz dieser Übung (gleiches Studio) als Vorlage kopiert (Reps, Gewicht, RPE)

**Untere Leiste (固定):**
- "+ Übung" OutlinedButton → öffnet ExercisePickerDialog
- "Fertig" / "Abbrechen" Button:
  - Grün (primary) wenn Übungen vorhanden → öffnet FinishDialog
  - Rot (error) wenn leer → zeigt Abbrechen-Dialog
  - Lade-Spinner beim Speichern

**ExercisePickerDialog:**
- Suchfeld ("Suchen...")
- Filtert nach Name UND Aliase (case-insensitive)
- Listenelemente: Übungsname + Kategorie
- Bei Auswahl: lädt Ghost-Daten (letzte Sätze), erstellt ActiveExercise

**FinishDialog:**
- Titel: "Training beenden?"
- Zusammenfassung: "{n} Übungen, {m} Sätze"
- Optionales Notiz-Feld ("Notizen (optional)", max. 3 Zeilen)
- Bestätigen: "Speichern & Beenden"
- Abbrechen: "Weiter trainieren"

**Abbrechen-Dialog:**
- Titel: "Training abbrechen?"
- Bestätigen: "Ja, abbrechen" (error)
- Abbrechen: "Weiter"

**Rest-Timer (NEU: verdrahtet):**
- Button "⏸ Rest" in der unteren Leiste (sichtbar wenn mindestens 1 Satz vorhanden)
- Countdown-Overlay ab 90 Sekunden (konfigurierbar)
- Schnell-Reset-Chips: 30s, 60s, 90s, 120s
- Auto-Schließung bei 0
- Haptisches Feedback beim Erreichen von 0

### 4.4 Workout Detail Screen

**Ansichtsmodus:**
- TopBar: "Training ansehen", Zurück-Pfeil
- Inhalt:
  - "Training" Header + Startdatum + Enddatum (falls vorhanden)
  - Notizen-Card (falls vorhanden)
  - Übungs-Cards (read-only):
    - Übungsname (bold)
    - Tabellen-Header: "#" / "Wdh." / "kg" / "RPE"
    - Pro Satz: Nummer, Wdh., Gewicht, RPE (oder "-")

**Bearbeitungsmodus:**
- TopBar: "Training bearbeiten"
- Inhalt:
  - Notiz-Feld (editierbar)
  - Übungs-Cards (editierbar, gleicher ExerciseCard wie aktives Workout)
  - "+ Übung hinzufügen" OutlinedButton
- Untere Leiste:
  - "Übung +" OutlinedButton → ExercisePickerDialog
  - "Speichern" Button mit Lade-Spinner

**Untere Leiste (Ansicht):**
- "Löschen" OutlinedButton (error-farben) → Löschen-Dialog
- "Bearbeiten" Button → wechselt in Bearbeitungsmodus

**Löschen-Dialog:**
- Titel: "Training löschen?"
- Bestätigen: "Löschen" (error)
- Abbrechen: "Abbrechen"

### 4.5 Übungen Verwalten Screen (Tab: Übungen)

**Header:**
- "Übungen verwalten" (headlineMedium)

**Studio-Auswahl:**
- ExposedDropdownMenuBox mit allen Studios + "Alle Studios"-Option
- Auswahl filtert Übungen

**Suchfeld:**
- "Suchen..." — filtert nach Name und Aliase

**Übungsanzahl:**
- "{n} Übungen" (labelMedium, muted)

**Übungsliste (LazyColumn):**
Pro Übung:
- CircleAvatar: erste 2 Buchstaben des IconKeys (farbig)
- Name (bodyLarge, medium)
- Kategorie (bodySmall, muted)
- Art (bodySmall, primary, übersetzt)
- Aliase (bodySmall, muted) — "Aliases: alias1, alias2"
- Bearbeiten-Button (Stift-Icon)
- Löschen-Button (Mülleimer, error)

**"Neue Übung" Button:**
- Full-width Button unten
- Add-Icon + Text

**ExerciseEditDialog:**
- Titel: "Übung bearbeiten" / "Neue Übung"
- Felder:
  - Name (Required, OutlinedTextField)
  - Kategorie (Dropdown: Brust, Rücken, Beine, Schulter, Arme, Core, Ganzkörper, Cardio, Sonstiges)
  - Art (Dropdown: Maschine, Freigewicht, Kabelzug, Körpergewicht)
  - Icon (Dropdown: Hantel, Beine, Langhantel, Kabelzug, Bank, Klimmzug, Laufband, Fahrrad)
  - Aliase (TextField, kommasepariert)
- Validierung: Name darf nicht leer sein
- Speichern: aktualisiert Übung + diffed Aliase (hinzufügen/entfernen)

**Löschen-Dialog:**
- Titel: "Übung löschen?"
- Text: '"${ex.name}" wirklich löschen?'
- Bestätigen: "Löschen" (error)
- Abbrechen: "Abbrechen"

### 4.6 Analyse Screen (Tab: Analyse)

**Header:**
- "Analyse" (headlineMedium)

**Studio-Filter:**
- ExposedDropdownMenuBox: "Alle Studios" + pro Studio

**Zeitraum-Filter (Chips):**
- 4W (28 Tage), 12W (84 Tage), 6M (180 Tage), 1J (365 Tage), Alle (9999 Tage)

**Übungsauswahl:**
- OutlinedButton ("Übung auswählen..." / gewählte Übung)
- Öffnet ExercisePickerDialog (gefiltert nach gewähltem Studio)

**Metrik-Filter (Chips, wenn Übung gewählt):**
- e1RM, Volumen, Max Gewicht

**Fortschritts-Diagramm (wenn Übung + Daten vorhanden):**
- Card mit Titel: "{Metrik} — {Übungsname}"
- SimpleLineChart:
  - Y-Achse: 5 Horizontal-Linien mit Labels (auto-scaled)
  - Linie: Pfad-basiert, 3px, primary-Farbe
  - Datenpunkte: Kreise (4dp)
  - X-Achse: Datumslabels (vertikal rotiert -90°, auto-gespaced)
  - Einzelpunkt: einzelner Punkt
- "{n} Trainingspunkte" Text

**Persönliche Rekorde (NEU):**
- Section: "Persönliche Rekorde"
- 3 PrCards:
  - "Max Gewicht" — Wert in kg, Datum
  - "Beste e1RM" — Wert in kg, Datum
  - "Max Volumen" — Wert in kg, Datum

**Dashboard-Statistik (NEU):**
- Section: "Dashboard"
- 2 Reihen à 3 StatCards:
  - Reihe 1: "Trainings" / "Sätze" / "Pro Woche"
  - Reihe 2: "Volumen" / "Übungen" / "Zeitraum" (Tage)

**Monatliches Volumen (NEU):**
- Section: "Monatliches Volumen"
- Card mit pro-Monat-Zeilen:
  - Monatslabel (80dp breit)
  - LinearProgressIndicator (proportional zum Maximum)
  - Volumen-Text (z.B. "12345 kg")
  - Workout-Anzahl (z.B. "(8)")

---

## 5. Berechnungen

### e1RM (Estimiertes 1RM)
**Epley-Formel:**
```
e1RM = weight * (1 + reps / 30.0)
```
- Nur für Sätze mit Gewicht > 0 und Requ > 0
- Aufwärm-Sätze werden ausgeschlossen

### Volumen
```
volume = reps * weight_kg
```
- Aufwärm-Sätze werden ausgeschlossen

---

## 6. Kategorien & Kataloge

### Übungskategorien (Deutsch)
`Brust`, `Rücken`, `Beine`, `Schulter`, `Arme`, `Core`, `Ganzkörper`, `Cardio`, `Unterarme`, `Sonstiges`

### Übungsarten
| Key | Label |
|---|---|
| `machine` | Maschine |
| `free_weight` | Freigewicht |
| `cable` | Kabelzug |
| `bodyweight` | Körpergewicht |

### Icons
| Key | Label | Emoji/Idee |
|---|---|---|
| `dumbbell` | Hantel | 🏋️ |
| `leg_press` | Beine | 🦵 |
| `barbell` | Langhantel | — |
| `cable` | Kabelzug | — |
| `bench` | Bank | — |
| `pull_up` | Klimmzug | — |
| `treadmill` | Laufband | — |
| `bike` | Fahrrad | — |

---

## 7. Farbschema (Dark Theme)

| Name | Farbe | Verwendung |
|---|---|---|
| primary | `#BB86FC` | Akzente, aktive Buttons, Satznummern |
| onPrimary | `#000000` | Text auf Primary |
| primaryContainer | `#1A1025` | Icon-Hintergründe |
| secondary | `#03DAC5` | Aktiv-Badges, Warmup-Chips |
| background | `#0C0C0F` | Seitenhintergrund |
| surface | `#141418` | Karten-Hintergrund |
| surfaceVariant | `#1C1C22` | Editor-Container |
| surfaceContainerHigh | `#28282F` | Gym-Cards, BottomBars |
| outline | `#3A3A42` | Trennlinien, Rahmen |
| error | `#FFB4AB` | Löschen-Buttons, Fehler |

---

## 8. Navigation

```
BottomNavigationBar (3 Tabs):
├── Training (Icon: Home)    → HomeScreen
├── Übungen (Icon: List)    → ExerciseListScreen
└── Analyse (Icon: DateRange) → AnalyseScreen

Stack-Navigation (von HomeScreen):
├── HomeScreen
│   ├── Tap Gym → ActiveWorkoutScreen
│   └── Tap Workout → WorkoutDetailScreen
└── WorkoutDetailScreen
    └── (keine tieferen Navigationen)
```

---

## 9. Ghost-Daten (Auto-Fill)

Wenn eine Übung zum Workout hinzugefügt wird:
1. Suche den letzten abgeschlossenen Workout-Eintrag, der diese Übung enthält (gleiches Studio bevorzugt)
2. Kopiere die Sätze dieses Trainings als Vorlage
3. Erster Satz: Requ, Gewicht, RPE aus dem ersten gespeicherten Satz
4. Beim Klick auf "Satz": Nächster Satz aus der Historie
5. Über Historie hinaus: Letzter bekannter Satz wird kopiert

---

## 10. Besondere Features (gegenüber aktuellem Server-App)

| Feature | Status | Beschreibung |
|---|---|---|
| Template-System | NEU | Vorlagen mit Übungsliste + Standard-Sätze/Reps/Gewicht pro Studio |
| Letztes Training kopieren | NEU | "Vorlage aus letztem Training" Button auf HomeScreen |
| Warmup-Toggle | NEU | Pro Satz: "Aufwärmen" Chip, wird aus Statistiken ausgeschlossen |
| Muskelversagen-Toggle | NEU | Pro Satz: "Zum Versagen" Chip |
| Satz-Notiz | NEU | Optionales Textfeld pro Satz |
| Rest-Timer verdrahtet | NEU | Button in unterer Leiste, Countdown-Overlay |
| Kein Login | — | Single-User, direkter Start |
| Komplett offline | — | Kein Internet, keine Server-Abhängigkeit |

---

## 11. Datenformat

### Datumsformatierung
- Anzeige: `dd.MM.yyyy HH:mm` (deutsches Locale)
- Speicherung: ISO 8601 (`2026-08-29T14:30:00`)
- Keine Zeitzonumwandlung nötig (lokal gespeichert)

### Zahlenformat
- Gewicht: `0.0` kg (ein Nachkommastelle)
- Requ: Integer
- RPE: Integer 1–10
- Volumen: Ganzzahl
- e1RM: eine Nachkommastelle

---

## 12. Erster Start & Migration

1. App startet → prüft ob DB existiert
2. Falls nein: Erstelle Tabellen + führe Seed-Daten ein (28 Übungen, 6 Aliase, 2 Studios)
3. Falls ja: Prüfe Versionsnummer und führe Migrationen aus (Drift auto-migration)
4. Zeige Splash-Screen (2 Sek.)
5. Starte direkt auf HomeScreen (kein Login)

---

## 13. Technische Hinweise

### Drift Database
```dart
// Example Entity
class Gyms extends Table {
  IntColumn get id => integer().autoIncrement()();
  TextColumn get name => text().withLength(min: 1, max: 120)();
  TextColumn get city => text().nullable()();
  BoolColumn get isSystem => boolean().withDefault(const Constant(false))();
  DateTimeColumn get createdAt => dateTime()();
}
```

### UI-Komponenten
- Alle Cards: `RoundedCornerShape(14–16.dp)`
- Alle Buttons: `RoundedCornerShape(12.dp)`
- BottomBars: `surfaceContainerHigh` + `shadowElevation = 8.dp`
- TextFields: `OutlinedTextFieldDefaults.colors(focusedBorderColor: primary, unfocusedBorderColor: outline)`
- Status-Badges: `Surface` mit abgerundeten Ecken + halbtransparenter Farbe

---

## 14. Zusammenfassung aller Screens

| Screen | Tab | Beschreibung |
|---|---|---|
| Splash | — | Logo + Animation (2s) |
| HomeScreen | Training | Willkommen, Studios, Vorlagen, letzte Trainings |
| ActiveWorkoutScreen | (Stack) | Workout mit Übungen + Sätzen, Ghost-Daten, Rest-Timer |
| WorkoutDetailScreen | (Stack) | Training ansehen/bearbeiten/löschen |
| ExerciseListScreen | Übungen | Übungen verwalten (CRUD, Suche, Filter) |
| AnalyseScreen | Analyse | Diagramme, Rekorde, Dashboard, Monatsvolumen |

---

## 15. Nicht enthalten (bewusst weggelassen)

- Kein Server/Netzwerk
- Kein Login/Auth
- Kein Multi-User
- Kein Sync
- Keine Push-Notifications
- Kein Export (CSV/PDF) — kann später ergänzt werden
