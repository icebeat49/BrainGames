# BrainGames — Codebase Analysis

*Analysis date: 2026-11-23. Read-only review: no code was changed. Unit tests were run once to confirm the build works.*

Path convention: unless a path starts with `app/`, it is relative to
`app/src/main/java/com/payfacto/braingames/`.

---

## 0. TL;DR

- This is a small, single-module Jetpack Compose app (~50 Kotlin files). It uses Hilt, Navigation-Compose and DataStore, with one `StateFlow` ViewModel per game. The code is clean and easy to read, but every game is built separately. There is **no game registry, no shared grid component, no save/resume, no stats, and no undo**.
- **Hiding Hidden Word is cheap.** No saved games, stats or deep links depend on it. A home-screen card and one route are the only ways in.
- **Hardcore Sudoku is cheap to medium.** Every assist is a single `if`/`when` branch in 3 files. Two features in the spec **don't exist yet**: undo (must be built) and "check on demand" (must be built). Three features you listed as assists to disable also **don't exist**: an error counter, remaining-digit counts on the number pad, and auto-removal of notes in peer cells. There is nothing to turn off for those.
- **The new animal game** can reuse the navigation/difficulty/top-bar/win/sound/theme scaffolding and the Canvas tap-to-cell pattern. Its generator, solver/uniqueness checker, hint engine, combined tap and drag gesture handling, and region rendering all need to be built from scratch.
- **Fix before starting:** puzzles are regenerated on every screen rotation (progress is lost), and the project **is not a git repository**. Make a baseline commit before you change anything.

---

## 1. Architecture

| Aspect | Finding |
|---|---|
| Modules | One Gradle module, `:app`. It is split by package: `core/` (data, di, model, sound, theme, ui) and `feature/` (home, settings, sudoku, kakuro, wordsearch). |
| UI toolkit | 100% Jetpack Compose + Material 3 (BOM `2025.04.01`). There are no XML layouts. Grids are drawn on a `Canvas`, with text drawn through `android.graphics.Paint` / `nativeCanvas`. |
| Navigation | Navigation-Compose `2.9.0` with **string routes** (not the type-safe API). See `core/ui/navigation/Screen.kt` and `AppNavGraph.kt`. |
| DI | Hilt `2.56.1` (KSP). There is only one module, `core/di/DataStoreModule.kt`. Generators and banks are Kotlin `object`s rather than injected classes. |
| State | `@HiltViewModel` + `MutableStateFlow<XxxUiState>` + `collectAsStateWithLifecycle`. UI state is an immutable `data class` that gets copied on each change. |
| Persistence | DataStore Preferences (`brain_games_prefs`), used for settings only. |
| SDK | `minSdk 24`, `targetSdk 36`, `compileSdk 36`, Java/JVM 11. |
| Toolchain | AGP 8.9.0, Kotlin 2.0.21, KSP 2.0.21-1.0.28, **Gradle 9.3.1** (builds, but prints "incompatible with Gradle 10" deprecation warnings). |
| Other deps | Coroutines 1.10.2, `material-icons-extended`. **Lottie 6.6.4 is declared but never used.** `appcompat` is used only for `AppCompatDelegate.setApplicationLocales`. `com.google.android.material` (the Views library) is not needed for a Compose-only app. |
| Entry | `MainActivity` (a `ComponentActivity`) reads prefs, applies the theme and dark mode, and hosts `AppNavGraph`. `BrainGamesApp` is the `@HiltAndroidApp`. |

## 2. How games are registered and launched

There is **no `Game` interface or registry**. A game is wired in by hand in six places:

1. `core/model/GameType.kt` — `enum { SUDOKU, KAKURO, WORD_SEARCH }`
2. `core/ui/navigation/Screen.kt` — one route per game (`sudoku/{difficulty}`, `kakuro/{difficulty}`, `wordsearch/{difficulty}`) plus `difficulty/{gameType}`
3. `core/ui/navigation/AppNavGraph.kt:53-57` — `when (gameType)` maps the game to its route, plus one `composable(...)` block per game (lines 64-98)
4. `core/ui/navigation/DifficultyPickerScreen.kt:36-40` — `when (gameType)` maps the game to its title string
5. `feature/home/HomeScreen.kt:74-93` — three hard-coded `GameCard` calls
6. `strings.xml` — `home_play_*` and `home_desc_*`

The flow is: **Home → `difficulty/{GAME}` → `{game}/{DIFFICULTY}`**. All games share the same 5-level `Difficulty` enum and the same picker. The `when` expressions are exhaustive, so adding a `GameType` produces compile errors at points 3 and 4, which helps. The Home screen is not covered by that check.

No deep links are declared, either in the manifest or as `navDeepLink`.

## 3. Shared vs. game-specific code

| Concern | Shared? | Details |
|---|---|---|
| Top bar | ✅ `core/ui/components/GameTopBar.kt` | Back, Hint (disabled when `hintsRemaining == 0`), New puzzle, Mute. **The Hint button cannot be hidden** because there is no parameter for it. |
| Win screen | ✅ `core/ui/components/WinAnimationOverlay.kt` | Confetti on a Canvas, elapsed time, Play again / Home. |
| Sound | ✅ `core/sound/SoundManager.kt` (Singleton) | It is passed as a **parameter** through `AppNavGraph` into each screen rather than injected. The `.ogg` files are **11 bytes each**, so they are placeholders. |
| Difficulty selection | ✅ `DifficultyPickerScreen` | Generic for all games. Per-game "last difficulty" prefs exist in DataStore but are **never read or written** (dead code). |
| Theme | ✅ `core/theme/*` | 5 palettes × light/dark. `MIDNIGHT` is dark-only. |
| Number pad | ⚠️ Shared by accident | `feature/sudoku/ui/NumberPad.kt` is imported by Kakuro. Its labels `✕` and `✏` have no content description. |
| Grid rendering | ❌ Per game | `SudokuGrid`, `KakuroGrid` and `WordSearchGrid` each draw their own Canvas. The same private `Color.toArgb()` helper is copied 3 times and shadows Compose's built-in one. A new `Paint` is allocated on every draw. Some colours are hard-coded: Kakuro uses `Color.White`/`DarkGray` (which **breaks dark mode**), Sudoku uses `Color.Red`, and Word Search uses `Color.Yellow`. |
| Cell selection | ❌ Per game | Sudoku stores an `Int` index, Kakuro a `Pair<Int,Int>?`, and Word Search tracks drag start and end. |
| Input / gestures | ❌ Per game | Sudoku and Kakuro use `detectTapGestures`. Word Search uses `detectDragGestures` along a straight line (`getCellsOnLine`). The pixel-to-cell maths is copied each time. |
| Timer | ❌ Copied 3× | `while(true){ delay(1000) }` in each ViewModel. It is **not shown during play** (only on the win screen) and **is not paused** when the app goes to the background. |
| Hints | ❌ Copied 3× | `hintsRemaining = 5` is hard-coded in each ViewModel. |
| Mute toggle and pref collection | ❌ Copied 3× | Each `init()` launches a new `preferences.collect`. |
| Save / resume | ❌ None | See §6. |
| Stats | ❌ None | There are no best times, completion counts or streaks. |
| Settings | ✅ `feature/settings` | Theme, dark mode, language (en/fr/system), sound. There are no gameplay settings. |

## 4. Sudoku assist features — where they live

Files: `feature/sudoku/SudokuViewModel.kt` (VM), `feature/sudoku/ui/SudokuGrid.kt` (Grid), `feature/sudoku/ui/SudokuScreen.kt` (Screen), `feature/sudoku/model/SudokuBoard.kt` (Board), `feature/sudoku/ui/NumberPad.kt` (Pad), `core/ui/components/GameTopBar.kt` (TopBar).

| Assist | Exists? | Location | Effort to make configurable |
|---|---|---|---|
| Selected-cell highlight | ✅ | Grid `:62` | Keep in Hardcore, because the player needs to see what they are about to write in. |
| Row/column/box highlight | ✅ | Grid `:51-53, 65-66` | **Trivial.** Gate one `when` branch. |
| Same-number highlight | ✅ | Grid `:54, 63-64` | **Trivial.** Gate one `when` branch. |
| Conflict marking (red cell + red digit) | ✅ | Logic in Board `conflictsAt()` `:13-29`. Drawn at Grid `:78-84` (background) and `:100-102` (text colour). | **Trivial.** Two `if`s. This is rule-based, checking peer duplicates. Nothing ever compares an entry against the solution, so the app never reveals that an entry is wrong unless it creates a duplicate. |
| Hints | ✅ | VM `useHint()` `:125-148` (fills the first wrong or empty cell with the solution). Button at TopBar `:37`. "N hints left" label at Screen `:74-78`. Count reset at VM `:69`. | **Easy.** TopBar needs a new optional parameter (e.g. `onHint: (() -> Unit)?`), the Screen hides the label, and the VM rejects the call. |
| Auto-win check | ✅ | VM `enterNumber` `:103-111` and `useHint` `:137-146` set `showWin` as soon as `board.isComplete()`. | **Easy.** Gate it behind a flag and add an explicit `checkSolution()` action. |
| Note cleanup on entry | ⚠️ Partial | VM `:100-101` clears only **the target cell's** notes when a value is written. Grid `:129` hides notes under a value. Notes in peer cells are **never** removed automatically. | Nothing to disable for peers. Decide whether Hardcore should still clear the cell's own notes. On paper, writing a digit hides the notes underneath, so keeping that behaviour is reasonable. |
| Error counter / mistakes limit | ❌ Does not exist | — | Nothing to disable. |
| Remaining-digit counts on pad | ❌ Does not exist | Pad `:30-41` shows plain 1–9. | Nothing to disable. If Classic mode should get it later, add it behind the same config. |
| Sound feedback on errors | ❌ (Sudoku always plays `TAP`) | Screen `:94-101` | Good: no correctness leaks through sound. Kakuro, by contrast, plays `ERROR` on conflict. |
| Erase | ✅ | VM `clearCell()` `:114-123`, Pad ✕ | Already there. |
| **Undo** | ❌ **Does not exist** | — | **Must be built** (the Hardcore spec requires it). A snapshot stack of `SudokuBoard` is cheap (81 ints plus the notes array). |
| Timer display | ❌ Hidden during play | — | Product decision. |

**Assessment:** every assist is a single branch in the VM, Grid or TopBar, and no assist logic is spread across the codebase. One config object passed into these three places covers everything. The real new work is **undo** and **validate-on-demand**.

## 5. Puzzle generation

### Sudoku — generated on device
- `feature/sudoku/data/SudokuGenerator.kt`. It fills a grid by randomised backtracking, then removes cells in random order, keeping each removal only if `hasUniqueSolution()` still holds. That check is a brute-force solution counter that stops at 2, so **uniqueness is guaranteed**.
- The solver is naive: it fills the first empty cell and does not pick the most constrained cell (no MRV). It is fine at 81 cells, but slow when called hundreds of times.
- **Difficulty** is the number of givens (VE 50–55, E 40–49, M 32–39, H 26–31, X 20–25). For HARD and EXPERT, it also runs `SudokuAnalyzer`, a human-technique grader (naked/hidden singles → swordfish), with up to 50 retries.
  - If no candidate matches, it falls back to an **ungraded** puzzle.
  - EXPERT's check is `level >= X_WING`, which also accepts `UNSOLVABLE`. Those puzzles need guessing beyond swordfish.
  - VE–MEDIUM are not graded, so a MEDIUM puzzle could need an X-Wing.
  - 20–25 givens is rarely reachable by random removal while keeping a unique solution, so EXPERT puzzles usually end up with more givens than the target.
- It runs on `Dispatchers.Default` and shows a spinner meanwhile.
- **No tests.**

### Kakuro — pre-built
- `feature/kakuro/data/KakuroPuzzleBank.kt` loads `res/raw/kakuro_<difficulty>.json`. There are **only 13 puzzles in total** (VE 5, E 3, M 2, H 2, X 1).
- **Difficulty is grid size** (5, 7, 8, 10, 12).
- There is no solver, and nothing verifies that each puzzle has a unique solution.
- The win check validates sums and duplicates, so it accepts alternative solutions, but the hint compares against the stored solution. If a puzzle had two solutions, these two checks could disagree.
- There is no notes mode.

### Hidden Word (Word Search / Mots cachés) — generated on device
- `feature/wordsearch/data/WordSearchGenerator.kt` places words from `res/values[-fr]/word_banks.xml`. It chooses placements that use the fewest new cells, so that exactly `hiddenWord.length` empty cells remain; the hidden word is then written into those cells in reading order. Every cell is used, and there is no random filler. `WordSearchBoard.verify()` enforces this.
- The themes, hidden words and clues come from `PuzzleBank.kt`: Kotlin lists of 15 entries per language, chosen from the **app** context's locale.
- `FallbackPuzzles.kt` holds 10 hard-coded boards (5 sizes × en/fr) for when generation fails.
- **Difficulty is grid size** (8, 10, 12, 14, 16).
- The word pool repeats every word 3×, so the same word can be placed twice.

## 6. Persistence

- **Settings:** DataStore key `brain_games_prefs`, read and written through `core/data/PreferencesDataSource.kt`. The keys are:
  - `color_theme`, `dark_mode`, `language_tag`, `is_muted`
  - `sudoku_difficulty`, `kakuro_difficulty`, `ws_difficulty`, which are **never written** and only read into `AppPreferences`, where nothing uses them
- **In-progress games: not persisted.** The state lives only in the ViewModel, so killing the process loses it.
- **Worse, rotating the screen loses it too.** Each screen calls `viewModel.init(difficulty)` from `LaunchedEffect(difficulty)` (e.g. `SudokuScreen.kt:38-40`). When the activity is recreated, that effect runs again, so `newPuzzle()` replaces the current game. Each call also starts another `preferences.collect` coroutine, so they pile up.
- **Stats:** none.
- **Backup:** `allowBackup=true`, and the `data_extraction_rules.xml` and `backup_rules.xml` templates are left unedited, so preferences are backed up by default.

**What hiding Hidden Word would affect:**

| Item | Impact |
|---|---|
| Saved games | None exist, so nothing to migrate. |
| Stats | None exist. |
| Prefs | `ws_difficulty` is unused. Leave it. |
| Deep links | None. The only ways in are the Home card → `difficulty/WORD_SEARCH` → `wordsearch/{d}`. |
| Strings | `home_play_wordsearch`, `home_desc_wordsearch` and `wordsearch_words_found` plus `word_banks.xml` (en/fr). Keep them all. The code still references them, so lint will not flag them as unused. |
| Tests | `WordSearchCorrectnessTest` keeps working, because it tests the generator directly. |
| APK | The code stays in the APK as long as the nav graph references it, which is fine since we want to keep it. |

## 7. Tests

| Test | Type | Content |
|---|---|---|
| `app/src/test/.../ExampleUnitTest.kt` | JVM | Template (2+2). |
| `app/src/test/.../wordsearch/WordSearchCorrectnessTest.kt` | JVM | Checks that generated boards always pass `verify()` and spell the hidden word, and that all 10 fallback boards pass `verify()`. **Takes ~35 s.** |
| `app/src/androidTest/.../ExampleInstrumentedTest.kt` | Instrumented | Template (package name). |

- There are no Sudoku, Kakuro or ViewModel tests, and no Compose UI tests, even though `ui-test-junit4` is declared.
- No `kotlinx-coroutines-test` or Turbine dependency is declared, so ViewModel tests would need one added.
- Last run (this analysis): **3/3 passed**, BUILD SUCCESSFUL in 4 min 23 s, mostly Gradle startup and configuration.

How to run them:

```powershell
.\gradlew.bat testDebugUnitTest
```
```powershell
.\gradlew.bat connectedDebugAndroidTest   # needs a device/emulator
```

## 8. Localization

- **Languages:** English (`res/values`) and French (`res/values-fr`). These hold `strings.xml` (~45 strings) and `word_banks.xml`. The French word bank has no accents, which looks deliberate so that letters fit the grid.
- **Strings hard-coded in Kotlin:**
  - `WordSearchScreen.kt:180,187,234`: `"Mot caché"`, `"$n lettres restantes"`, `"Indice : $hint"`. These are **French in every locale**.
  - `PuzzleBank.kt` and `FallbackPuzzles.kt` keep their themes, hidden words and clues in Kotlin, with language chosen by `locale.language == "fr"`.
  - `NumberPad.kt` uses `✕`/`✏`.
  - The back buttons in several screens have `contentDescription = null`.
- **Language switching problems:**
  - `MainActivity` is a `ComponentActivity`, not an `AppCompatActivity`. There is no `res/xml/locales_config.xml`, no `android:localeConfig`, and no `AppLocalesMetadataHolderService` with `autoStoreLocales`. As a result, `AppCompatDelegate.setApplicationLocales` probably **does not apply on API 24–32**. It should work on API 33+, but the app will not appear in the system per-app language settings. This needs checking on a real device.
  - `MainActivity.restoreLocale()` uses `runBlocking` on the main thread.

## 9. Code quality and tech debt that affects the planned work

Ordered by how much each item gets in the way:

1. **Not a git repository.** There is no history and no way to roll back. Run `git init` and commit a baseline before any change.
2. **VM init in `LaunchedEffect`:** the game regenerates on rotation and preference collectors pile up (§6). Every new game would copy this bug. The fix is to read the route args through `SavedStateHandle` in the VM (the Hilt nav integration provides it automatically) and start the game in the VM's `init {}`.
3. **No game registry.** Adding the animal game means touching six files by hand (§2), and hiding a game is only possible by deleting a Home card.
4. **Session logic copied 3×** (timer, hints, mute, win). A fourth game would make four copies. Pull it into a shared piece (see §10.3).
5. **No undo anywhere.** Both Hardcore and the animal game need it, so build a generic `UndoStack<T>` in `core`.
6. **No save/resume or stats.** This is not needed for the three tasks, but if the team wants it, add it before the fourth game. Retrofitting it into four VMs later is harder.
7. **Grid rendering is copied per game.** There is a `toArgb` copy in each grid, a new `Paint` on every frame, hard-coded colours that ignore dark mode, and no accessibility semantics on the Canvas grids.
8. **`SoundManager` lifecycle bug.** It is an `@Singleton`, but `MainActivity.onDestroy()` (`:59`) calls `soundManager.release()`. After a rotation, the singleton keeps a released `SoundPool`, so sounds stop. The sound files are also placeholders.
9. **Kakuro stale-state bug.** `KakuroScreen.kt:89` checks `state.board.hasConflicts()` against the board from **before** the number was entered, so the error sound plays one move late.
10. **Dead code and unused dependencies:** Lottie, the per-difficulty prefs, `ExampleUnitTest`, and the Material Views library.
11. **`NumberPad` lives in the `sudoku` package** but is shared. Move it to `core/ui/components` if both games keep using it.
12. **`Difficulty` is one global enum.** The animal game maps it to grid sizes, as Kakuro and Word Search already do, so this works. Just make sure the picker labels still make sense, or show the grid size next to each level, e.g. "Medium · 7×7".

---

## 10. Recommendations

### 10.0 Prerequisites (small, do first)
1. Run `git init` and commit the current state as a baseline.
2. Move ViewModel start-up to `SavedStateHandle` + `init {}`, which fixes the rotation reset (§9.2).
3. Optionally, extract the timer and hint counter into a small shared class. This keeps the next two tasks from adding a fourth copy.

### 10.1 Hiding Hidden Word — feature flag

Use a single source of truth that drives the Home screen and guards the routes:

```kotlin
// core/model/GameType.kt — keep WORD_SEARCH
// core/config/FeatureFlags.kt
object FeatureFlags {
    val enabledGames: Set<GameType> = buildSet {
        add(GameType.SUDOKU); add(GameType.KAKURO)
        if (BuildConfig.FEATURE_WORD_SEARCH) add(GameType.WORD_SEARCH)
    }
    fun isEnabled(game: GameType) = game in enabledGames
}
```

- In `app/build.gradle.kts`, set `buildFeatures { buildConfig = true }` and add `buildConfigField("boolean", "FEATURE_WORD_SEARCH", "false")`. Optionally override it to `true` in `debug`, so the team can still reach the game during development.
- **HomeScreen:** build the cards from a list, e.g. `GameType.entries.filter(FeatureFlags::isEnabled)`, with a `GameType → (title, desc, icon)` mapping. This is the start of a lightweight **game catalog**, which the animal game reuses (see 10.3).
- **AppNavGraph:** keep the `wordsearch/{difficulty}` composable registered and the code compiled. In the `difficulty/{gameType}` and `wordsearch/...` destinations, `popBackStack()` if the game is disabled. Since there are no deep links, this is defence in depth.
- Leave all strings, word banks, tests and the `ws_difficulty` pref in place. No data migration is needed.
- Avoid deleting `GameType.WORD_SEARCH` or using source sets to exclude the code. Both make it harder to bring the game back.

### 10.2 Hardcore Sudoku — one screen driven by a rules object

```kotlin
enum class SudokuMode { CLASSIC, HARDCORE }

data class SudokuAssists(
    val highlightPeers: Boolean,
    val highlightSameNumber: Boolean,
    val showConflicts: Boolean,
    val maxHints: Int,                // 0 = hint button hidden
    val autoDetectWin: Boolean,       // false = player taps "Check"
    val clearCellNotesOnEntry: Boolean,
    // reserved for future Classic features, false today:
    val showRemainingCounts: Boolean = false,
    val autoRemovePeerNotes: Boolean = false,
) {
    companion object {
        val Classic  = SudokuAssists(true,  true,  true,  5, true,  true)
        val Hardcore = SudokuAssists(false, false, false, 0, false, true)
    }
}
```

- **Route:** change it to `sudoku/{difficulty}?mode={mode}`, with `CLASSIC` as the default, so existing navigation keeps working. The VM reads `mode` from `SavedStateHandle` and exposes `assists` in `SudokuUiState`.
- **Mode choice UI:** add a Classic / Hardcore segmented button to `DifficultyPickerScreen`, shown only for Sudoku. Ideally the picker takes an optional `extraOptions` slot so it stays generic. Remember the last-used mode in DataStore.
- **Grid:** `SudokuGrid(board, selectedCell, assists, …)`. Wrap the three highlight/conflict branches (lines 63-66, 78-84, 100-102) in their flags.
- **TopBar:** make the hint optional, either `onHint: (() -> Unit)?` or `showHint: Boolean`. The Screen hides the "hints left" label when `maxHints == 0`.
- **VM:**
  - `useHint()` returns `false` when hints are off, so the rule is enforced in the VM and not just hidden in the UI.
  - `enterNumber` / `useHint` set `showWin` only if `autoDetectWin`.
  - New `checkSolution()` emits `Solved` (shows the win overlay and stops the timer), `Incomplete` or `Incorrect`. **Product decision:** in Hardcore, should "Incorrect" say only *that* the grid is wrong, or also *where*? Pen-and-paper style suggests only *that*. Also decide whether a failed check costs anything.
  - **Undo:** a generic `UndoStack<SudokuBoard>` that pushes before every value/note/erase change. Offer it in both modes.
- **Pad:** add **Undo** and, in Hardcore only, a **Check** button next to ✕/✏.
- **Later:** when stats arrive, record them per mode.
- **Tests:** add VM unit tests for "Hardcore: no hint, no auto-win, check() results, undo" and Grid-free logic tests for `SudokuAssists` gating. This needs `kotlinx-coroutines-test`.

**Estimated size:** ~1 new file, edits to ~6 files, and no forked screen.

### 10.3 New animal grid game

The mechanic, a logic puzzle with one item per row, column and coloured region and no two items touching, is a well-known genre. Use an original name, original animal art and original region palettes.

**What can be reused:**

| Existing piece | Reuse |
|---|---|
| `GameType`, `Screen`, `AppNavGraph`, `DifficultyPickerScreen` | Add `GameType.ANIMALS` + `animals/{difficulty}`. Map `Difficulty` → N, e.g. VE 5, E 6, M 7, H 8, X 9. Show "7×7" on the picker. |
| `GameTopBar` | Back / Hint / New / Mute, as is. |
| `WinAnimationOverlay`, `SoundManager`, theme | As is. |
| Canvas grid + `pointerInput` tap→cell maths (`SudokuGrid`, `KakuroGrid`) | Copy the pattern, or better, extract a small `GridCanvas`/`cellAt(offset)` helper and use it for all games. |
| Drag tracking from `WordSearchGrid` | Use it as a reference only. Its straight-line logic does not fit; the new game needs a freeform drag path. |
| Timer / hints / mute VM logic | Reuse it after the extraction in 10.0. |
| `UndoStack<T>` (built for Hardcore) | Reuse. |
| Generator patterns: `Dispatchers.Default` + spinner (Sudoku); generator + verified fallback bank (Word Search) | Same structure. |

**New pieces to build:**

1. **Model:**
   - `regions: IntArray(N*N)` (region id per cell)
   - `solution: Set<Int>` (cell index per animal)
   - `marks: Array<CellMark>`, where `CellMark = EMPTY | CROSS | QUESTION | ANIMAL`
   - per-region `animalKind` for the art
2. **Rules / validator:**
   - one animal per row, per column and per region
   - no two animals in any of the 8 neighbouring cells
   - a validator that reports violations, for optional live feedback in non-hardcore play
3. **Solver:**
   - constraint propagation: an animal placed eliminates its row, column, region and 8 neighbours; a region, row or column with one candidate left is forced; a region whose candidates all sit in one row or column clears that line elsewhere
   - backtracking with an early-exit solution counter (stop at 2) for the **uniqueness check**
4. **Generator:**
   - (a) place N non-touching animals with a random permutation (one per row and column) and reject diagonal neighbours
   - (b) grow N regions from those seeds by randomised flood fill
   - (c) keep the grid if the solver finds exactly one solution; otherwise reshape regions and retry
   - Run it on `Dispatchers.Default` with an attempt budget, and keep a small verified fallback bank as Word Search does
   - **Difficulty:** mainly N. Optionally also grade by the hardest deduction the logical solver needs, the way `SudokuAnalyzer` does
5. **Hint engine:** prefer a **deduction-based** hint, i.e. "this cell must be X / an animal", taken from the logical solver's first forced step, over simply revealing a solution cell. Fall back to revealing a solution cell if no simple deduction is found.
6. **Gestures:** use one `pointerInput` with `awaitEachGesture` rather than separate tap and drag detectors, which compete with each other.
   - **Tap** (up before the touch slop is exceeded) cycles the cell: empty → ✕ → animal → empty. The exact cycle is a product decision.
   - **Drag** (slop exceeded) puts the gesture into paint mode. If it started on an empty cell, it marks every cell it passes through with ✕, or ? when the toggle is on. Interpolate between pointer samples so fast drags don't skip cells.
   - Never let a drag overwrite an animal.
   - Push **one** undo entry per drag, not one per cell.
7. **"?" toggle:** a mode flag in the UI state. Marks placed while it is on become `QUESTION` instead of `CROSS`.
8. **Region rendering:**
   - a fill colour per region from a palette of 9–10 colours that stay distinct for common colour-blindness, with light and dark variants
   - **thick borders between regions** (draw thick where `regions[a] != regions[b]`), which also helps colour-blind players
   - animal icons as original vector drawables
9. **Win condition:** N animals placed and the validator passes, checked automatically. Decide whether the game should also offer a "manual check" mode, as Hardcore Sudoku does.
10. **Strings (en + fr), tests:**
    - generator always produces unique puzzles for each N
    - solver correctness on known grids
    - gesture → mark state transitions in the VM
    - hint engine never contradicts the solution

**Suggested order:**
1. Prerequisites (10.0)
2. Hide Hidden Word (10.1, ~½ day)
3. Hardcore (10.2, ~1–2 days including undo and tests)
4. Animal game: model + solver + generator with tests first, then the UI and gestures (largest task)
