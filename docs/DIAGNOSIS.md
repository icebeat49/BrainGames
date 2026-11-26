# BrainGames — Diagnosis (plan step 1)

*Date: 2026-11-26. Read-only: no app code was changed. A throwaway Kakuro solver was run outside the repo (Python, in a scratch folder, not committed).*

Path convention: unless a path starts with `app/`, it is relative to
`app/src/main/java/com/payfacto/braingames/`.

---

## 0. TL;DR

1. **Language option: it does nothing on any Android version.** `AppCompatDelegate.setApplicationLocales()` only has an effect when at least one **AppCompat** activity exists. `MainActivity` is a plain `ComponentActivity`, so the call is silently ignored on Android 13+ *and* on 7–12. The choice is saved in DataStore, so the radio button "sticks", but the app never changes language. The only language the app ever shows is the phone's system language. Three Hidden Word strings are also hard-coded in French.
2. **Kakuro difficulty: the puzzle bank itself is broken, not just mis-graded.** There are 13 hand-made puzzles. Difficulty is only the grid size. My solver found that **only 3 of the 13 have a unique solution** (all three are Very Easy). One Easy puzzle (`e_02`) **has no solution at all**: its stored answer breaks its own clue, so it can never be won. The Medium, Hard and Expert puzzles have **thousands to more than 100 000 solutions** each. They are sparse grids of small blocks, so almost any digits that add up win. That is why "Hard" feels easy. Fix: generate and grade puzzles offline, the way Crowns does.
3. **Saving: nothing is saved to disk for in-progress games.** Each game already writes its full state into `SavedStateHandle`, which survives rotation and process death but **not** swiping the app away or rebooting. kotlinx.serialization is **not** in the project. Recommendation: one typed DataStore file per game, with `@Serializable` save DTOs (not the domain classes). Use a separate stats DataStore with the same technology.

---

## 1. Language option

### 1.1 How it works today

| Piece | Where | What it does |
|---|---|---|
| UI | `feature/settings/SettingsScreen.kt:124-143` | Three radio buttons: `"en"`, `"fr"`, `""` (System). `selected` comes from `prefs.languageTag` (DataStore). |
| Choose | `feature/settings/SettingsViewModel.kt:36-44` | Writes the tag to DataStore (`language_tag`), then calls `AppCompatDelegate.setApplicationLocales(...)`. |
| Restore | `MainActivity.kt:57-62` | In `onCreate`, it reads DataStore with **`runBlocking`** on the main thread. If the tag is non-empty, it calls `setApplicationLocales` again. |
| Manifest | `app/src/main/AndroidManifest.xml` | No `android:localeConfig`, no `AppLocalesMetadataHolderService`, no `configChanges`. |
| Resources | `res/values`, `res/values-fr` | No `res/xml/locales_config.xml`. |
| Activity | `MainActivity : ComponentActivity` | Not an `AppCompatActivity`. No `attachBaseContext` override anywhere. |
| Library | appcompat **1.7.1** | |

### 1.2 Root cause (confirmed in the AppCompat bytecode)

I disassembled `AppCompatDelegate` from the appcompat 1.7.1 AAR in the Gradle cache:

```
setApplicationLocales(locales):
  if SDK_INT >= 33:
      lm = getLocaleManagerForApplication()   // walks sActivityDelegates
      if lm != null: LocaleManager.setApplicationLocales(...)
      // else: nothing
  else:
      sRequestedAppLocales = locales
      applyLocalesToActiveDelegates()           // walks sActivityDelegates

getLocaleManagerForApplication():
  for each delegate in sActivityDelegates: return delegate.context.getSystemService(LOCALE_SERVICE)
  return null
```

`sActivityDelegates` is only filled by `AppCompatActivity` / `AppCompatDialog`. The app has neither, so:

- **Android 13+ (API 33+):** `getLocaleManagerForApplication()` returns `null`, so the framework is never called. **Nothing happens.** The app also doesn't appear under *System settings → Apps → Language*, because there is no `localeConfig`. So the system per-app language can't be used as a workaround either.
- **Android 7–12 (API 24–32):** the locale is kept in a static field and pushed to active AppCompat delegates, of which there are none. AppCompat would normally apply it in `AppCompatActivity.attachBaseContext`, but that never runs. **Nothing happens.**

### 1.3 Answers to the specific questions

| Question | Answer |
|---|---|
| Does the choice survive restarting the app? | The **setting** does: it's in DataStore, so the right radio button is selected after a restart. The **language** never applies, so there is nothing to survive. |
| Do screens refresh immediately? | No, because nothing is applied. If it were fixed with AppCompat, Android recreates the activity. Compose then re-reads `stringResource` and the screens refresh, but see the caveats below. |
| Does "System" work? | Only by accident. The app always follows the system language because no override is ever set. |
| Hard-coded strings | `feature/wordsearch/ui/WordSearchScreen.kt:183,190,237`: `"Mot caché"`, `"$n lettres restantes"`, `"Indice : $hint"`. These are **French in every language**. `NumberPad.kt:57,71` uses `✕`/`✏` symbols, which is fine visually, but there's no content description. Several `contentDescription = null` in `HomeScreen`, `SettingsScreen` and `DifficultyPickerScreen`. |
| Text cached in ViewModels / outside a Context | Crowns and Sudoku are clean. Crowns stores the hint as a `CrownsHint` object and turns it into text with `stringResource` at draw time (`CrownsHintMessage.kt`), and catalog and picker use `@StringRes`. **Hidden Word is not clean:** the theme, hidden word and clue are picked from `PuzzleBank`/`FallbackPuzzles` and the word list from `word_banks.xml`. Each is chosen with the **Application** context's locale (`PuzzleBank.kt:9`, `FallbackPuzzles.kt:18`, `WordBank.kt:8`) at generation time. They are then stored in `SavedStateHandle`. A game in progress keeps its old language, which is acceptable. On API < 33, the Application context will **not** pick up a per-app locale even after the fix (see 1.4). Hidden Word is hidden in release, so this is low priority. |
| `runBlocking` on main thread | `MainActivity.restoreLocale()`, a small cold-start cost. It becomes unnecessary after the fix. |

### 1.4 What a fix needs (for step 2)

Recommended: **switch to the standard AppCompat path** and let the platform or AppCompat store the choice.

1. `MainActivity : AppCompatActivity`. The theme parent is `Theme.Material3.DayNight.NoActionBar`, which already extends an AppCompat theme, so this works as-is. `enableEdgeToEdge`/`setContent` work on `AppCompatActivity`.
2. Add `res/xml/locales_config.xml` (`en`, `fr`) and `android:localeConfig="@xml/locales_config"`. This makes the app appear in the Android 13+ system per-app language screen. Alternative: AGP `androidResources { generateLocaleConfig = true }` plus a `res/resources.properties`.
3. Add the `AppLocalesMetadataHolderService` entry with `autoStoreLocales=true` so AppCompat persists the choice on API < 33. On 33+ the system persists it.
4. Make `AppCompatDelegate.getApplicationLocales()` the single source of truth. Drop the `language_tag` DataStore key and `restoreLocale()`/`runBlocking`, or keep the key only for a one-time migration of the current value. Settings reads the selected radio from `getApplicationLocales()`.
5. Hidden Word: pick its content language from the current locale. Use `AppCompatDelegate.getApplicationLocales()` or the Activity's configuration rather than the Application context, then move the three hard-coded strings to `strings.xml`.
6. Test on one API 33+ device and one API ≤ 32 emulator: switch language, kill the app, reopen, rotate.

Effort: **~½ day** including the device check.

---

## 2. Kakuro difficulty

### 2.1 How puzzles are created and picked

- **No generator, no solver.** `feature/kakuro/data/KakuroPuzzleBank.kt` reads `res/raw/kakuro_<difficulty>.json` and returns a **uniformly random** puzzle from that file (`(0 until n).random()`). There is no recent-history check, so the same puzzle often repeats. There is only one Expert puzzle.
- **Difficulty is only the file name**, which in practice means grid size: VE 5×5, E 7×7, M 8×8, H 10×10, X 12×12. There is no notion of the techniques needed.
- **Win check** (`KakuroBoard.isComplete`): all cells filled, each run sums to its clue with no repeats. It does **not** compare against the stored solution.
- **Hint** (`KakuroViewModel.useHint`): it overwrites the first cell whose value is not the **stored** solution. On a puzzle with many solutions, a hint can "correct" a cell that was valid.

### 2.2 Measurements

A throwaway Python solver parsed every JSON. It checked that the stored solution satisfies the clues, counted the solutions (exact backtracking with run-level constraint propagation) and graded each puzzle:

- **L1:** sum combinations plus singles
- **L2:** full per-run consistency, which covers hidden singles and "last cell = remainder"
- **L3:** one-step contradiction / trial

"Logic-fixed" is how many cells pure logic (up to L3) can determine.

| Difficulty | Id | Grid | White cells | Blocks (connected white areas) | **Solutions** | Logic-fixed | Honest real difficulty |
|---|---|---|---|---|---|---|---|
| Very Easy | ve_01 | 5×5 | 4 | 1×4 | **1** | 4/4 (L1) | Trivial ✅ |
| Very Easy | ve_02 | 5×5 | 4 | 1×4 | **1** | 4/4 (L1) | Trivial ✅ |
| Very Easy | ve_03 | 5×5 | 6 | 1×6 | 4 | 0/6 | Trivial, ambiguous |
| Very Easy | ve_04 | 5×5 | 4 | 1×4 | 3 | 0/4 | Trivial, ambiguous |
| Very Easy | ve_05 | 5×5 | 4 | 1×4 | **1** | 4/4 (L1) | Trivial ✅ |
| Easy | e_01 | 7×7 | 8 | 2×4 | 5 | 4/8 | Very easy, ambiguous |
| Easy | **e_02** | 7×7 | 6 | 1×6 | **0** | — | **Unwinnable.** The stored solution puts 2+7 under a "15" clue. No valid fill exists, so `isComplete()` can never be true and hints lead to a dead end. |
| Easy | e_03 | 7×7 | 9 | 1×9 | 159 | 0/9 | Very easy (almost any fill works) |
| Medium | m_01 | 8×8 | 17 | 9 + 8 | 2 520 | 0/17 | Easy, no logic needed |
| Medium | m_02 | 8×8 | 12 | 1×12 | 1 859 | 0/12 | Easy, no logic needed |
| Hard | h_01 | 10×10 | 24 | 12 + 12 | **> 100 000** | 0/24 | Easy to medium, mostly arithmetic |
| Hard | h_02 | 10×10 | 20 | 1×20 | 13 300 | 0/20 | Easy to medium |
| Expert | ex_01 | 12×12 | 36 | 20 + 16 | **> 1 000** (counting stopped) | 0/36 | Medium at most |

Other observations:

- **Density is very low:** 12–27% white cells. Runs average 2–4.4 cells (max 5). Real Kakuro of comparable "Hard" level is 9×9 to 12×12 with about 55–65% white cells, runs up to 7–9, and one large interlocking area.
- Every white cell is correctly covered by exactly one across run and one down run, and no clue is orphaned, so the **format** is fine. The problem is the content.
- Why "Hard" plays easy or medium: with thousands of valid answers, the player never has to deduce anything. They write any pair or triple that adds up, and the sum-based win check accepts it. Grid size makes the board *look* bigger, but the work is the same small arithmetic.

The solver script is kept outside the repository at
`C:\Users\CDESLA~2\AppData\Local\Temp\claude\a81e6b932ba560e9\1db9a026-cf52-472d-b3ef-57448618395a\scratchpad\kakuro_grade.py` (temporary folder; it may be cleaned up).

### 2.3 Options to fix

| Option | What | Effort | Verdict |
|---|---|---|---|
| A. Re-grade existing puzzles | Re-label the 13 puzzles by measured difficulty. | ~1 h | **Not viable.** Only 3 are unique, and all of them are trivial. Nothing is left to fill Medium–Expert. |
| B. Larger hand-verified bank | Find or hand-make ~50+ puzzles per level, verify uniqueness with a solver test, grade them by technique. | 2–3 days, plus licensing questions for any external source | Still needs the solver and grader from C, and a fixed bank becomes repetitive. Only worth it as a stop-gap. |
| C. **Offline generator + unique-solution check + technique-graded solver** (Crowns pattern) | In `feature/kakuro/engine/`: (1) a layout generator: symmetric black/white pattern with a target density, runs of 2–9, a connected white area, no 1-cell runs; (2) a filler: random digits with no repeats per run, backtracking; (3) clues derived from the fill; (4) an exact solution counter (stop at 2) that rejects non-unique puzzles or tweaks them (add a black cell, re-fill); (5) a technique solver: unique sums, combination intersection, last-cell remainder, hidden single in run, subset/pair reasoning across crossing runs, sum of a block ("area sums"), then trial/contradiction; (6) a grader mapping the hardest technique and step count to `Difficulty`; (7) a JVM test writer that produces `kakuro_bank_<level>.txt` (~250 per level) like `CrownsBankWriter`; (8) a source with the recent-history picker (reuse `RecentCrownsPuzzles`, generalised); (9) tests: every banked puzzle unique, the stored solution valid, the grade matches the file. | **3–5 days** (the Crowns engine is ~1 100 lines plus tests; Kakuro combinatorics are a bit larger) | **Recommended.** |
| D. On-device generation | Same as C, but run on the phone. | +1–2 days on top of C | No. 12×12 unique-solution generation can take seconds, and Crowns already chose a bundled bank for the same reason. |

**Recommendation: C (offline generator → graded bank), done as plan step 4.**

Do this right away, before step 4, because it is cheap: **remove `e_02`** (unwinnable) in step 2 or as a hot-fix.

Fold these into step 4 too:

- Make the hint use a deduction, like Crowns, or at least be consistent with the unique solution.
- Keep the sum-based win check. It is correct once puzzles are unique.
- Fix the hard-coded white/dark-grey colours (dark mode), which are already on the step 7 list.

---

## 3. Saving the current game

### 3.1 What is persisted today

| Data | Where | Survives rotation | Survives process death (system kill) | Survives swipe-away / force-stop / reboot |
|---|---|---|---|---|
| Settings: theme, dark mode, language tag, mute | DataStore Preferences `brain_games_prefs` (`core/data/PreferencesDataSource.kt`) | ✅ | ✅ | ✅ |
| Last mode per game (`<game>_last_mode`) | Same DataStore (`GameModePreferences`) | ✅ | ✅ | ✅ |
| "How to play" seen (`<game>_how_to_play_seen`) | Same DataStore (`HowToPlayPreferences`) | ✅ | ✅ | ✅ |
| Crowns recent puzzles (`crowns_recent_<size>`) | Same DataStore (`RecentCrownsPuzzles`) | ✅ | ✅ | ✅ |
| Per-game difficulty keys (`sudoku_difficulty` …) | Same DataStore | Never written (dead code) | | |
| **In-progress game** (board, undo, timer, hints, win flag) | **`SavedStateHandle` only** (`*BoardSaver`, `CrownsGameSaver`, `GameSession`) | ✅ | ✅ | ❌ **lost** |
| Stats | None | | | |

There's no Room and no file storage. Backup: `allowBackup=true` with the default template rules, so DataStore is backed up.

### 3.2 What a full save needs, per game

`GameSession` (shared) currently keeps the following in `SavedStateHandle`: elapsed ms, hints remaining, playing flag, win flag.

| Field | Sudoku | Crowns | Kakuro | Hidden Word (hidden in release) |
|---|---|---|---|---|
| Puzzle | `solution[81]`, `given[81]` | `size`, `regions[n²]`, `solution[n]` (column per row) | Grid: `rows`, `cols`, per cell Black / Clue(down, across) / Input(solution) | `grid[r][c]` chars, words (word, start, direction), theme, hidden word, clue, hidden-word cells |
| Player entries | `current[81]` | `marks[n²]` (EMPTY/CROSS/QUESTION/CROWN) | `current` per Input cell | `found` per word |
| Notes / marks | `notes[81]` (bitmask) | Included in marks, plus the `isQuestionMode` toggle | **No notes in Kakuro today** | — |
| Mode | CLASSIC / HARDCORE (route arg) | CLASSIC / HARDCORE (route arg) | None today (Hardcore is a "later" item) | None |
| Difficulty | Route arg | Route arg | Route arg | Route arg |
| Hints used | `GameSession` hints remaining | Same | Same | Same |
| Elapsed time | `GameSession.accumulatedMs` (private) | Same | Same | Same |
| Undo history | `List<SudokuMove>` (index, value, notes), max 500 | `List<CrownsBoard>` (full boards), `UNDO_LIMIT` | **No undo in Kakuro today** | — |
| Win state | `GameSession.showWin` (a win deletes the save, so only "solved, overlay pending" matters) | Same, `isSolved` is derived from it | Same | Same |
| Transient, don't save | selected cell, notes-mode toggle, failed-check dialog, error cells | hint display, failed-check, error cells, How-to-play open | selected cell | hint cell, drag |

### 3.3 Serialization readiness

- **kotlinx.serialization is not in the project.** It needs:
  - the `org.jetbrains.kotlin.plugin.serialization` plugin at the Kotlin version (2.0.21)
  - `kotlinx-serialization-json` (1.7.x is compatible with Kotlin 2.0)
  - `androidx.datastore:datastore` (core, typed), next to the existing `datastore-preferences`
  - ProGuard rules (the serialization plugin ships them; R8 is on in release)
- What makes annotating the **domain classes** directly awkward, and why separate `@Serializable` DTOs (`SudokuSave`, `CrownsSave` …) are better:
  - `CrownsPuzzle` is a regular class with **private** arrays and copying getters, plus lazy fields. It has no primary-constructor properties for the plugin to use.
  - `SudokuBoard` uses `Array<Set<Int>>` and a custom `equals`. It is serializable, but the JSON is bulky. The existing bitmask encoding is better.
  - `KakuroCell` is a sealed class with an `object Black`. It is serializable with annotations, but the Kakuro model will be rewritten in step 4.
  - `WordSearchBoard` uses `Array<CharArray>` and `Pair`.
  - `UndoStack<T>` is generic, but it already exposes `toList()` and an `initial` constructor argument, so it's easy to save.
  - `GameSession` keeps `accumulatedMs`, `isPlaying` and the hints in private fields that it writes only to `SavedStateHandle`. It needs a small `snapshot()`/`restore(snapshot)` API.
  - Difficulty and mode come from the **route**, not the VM state. "Continue" must navigate with the saved difficulty and mode, or the VM must take them from the save.
- The existing `*Saver` objects already define compact, Bundle-friendly encodings (IntArrays, bitmasks, ordinals). Reuse the same shapes in the DTOs. A `version: Int` field per save allows schema changes later.

### 3.4 Recommended storage

**One typed DataStore per game, with kotlinx.serialization JSON:** `saves/sudoku.json`, `saves/crowns.json`, `saves/kakuro.json`. Each file holds `GameSave?`, where null means no save.

Why:

- Writes are atomic (temp file + rename), so a kill mid-write never corrupts the save.
- Writes are serialized, it exposes a `Flow` for "Continue" on the picker, it's already a project dependency family, and Hilt wiring follows `DataStoreModule`.
- Room is overkill for one row per game. Raw files would need hand-rolled atomic writes.

How to use it:

- **When to write:** after each move, with the VM calling `store.save(snapshot)`. Run writes on an **application-scoped** `CoroutineScope` (a new Hilt `@ApplicationScope`), not `viewModelScope`, so the last write isn't cancelled when the screen closes. Each save is a few KB, so writing per move is fine. Conflate if moves come faster than writes; there's no need for a debounce.
- **On background:** the screens already call `onScreenHidden()` → `GameSession.onBackground()`. Save there too, because elapsed time changes without moves. Optionally also on `ViewModel.onCleared()`.
- **On win:** clear the file. On "New game": clear it, then write the new puzzle.
- **Keep `SavedStateHandle`** for rotation and fast restore, or drop it in favour of the disk save. Either works. The disk save is the source of truth on cold start.
- **Kakuro:** design its save after step 4, because the model, notes and undo will change. Until then, save the current board only, or skip Kakuro.
- **Backup:** decide whether saves should be in cloud backup. Probably exclude `saves/` and include stats.

**Stats (step 6): same technology, a separate store.** Use `stats.json` (typed DataStore), keyed by `game × difficulty × mode`, holding `played`, `won`, `bestMs`, `totalWonMs`. Store the total won time and compute average = `totalWonMs / won`; don't store a running average.

Reasons to keep it separate from the saves:

- Different lifecycle: a save is deleted on every win, while stats are reset only by the user.
- A schema change or corruption in one doesn't wipe the other.
- A reset is a single-file operation.

At about 3 games × 5 × 2 = 30 small records, Room is not needed. Choose Room only if per-game history (streaks, charts) is wanted later.

---

## 4. Proposed fix plan (ordered by effort, smallest first)

### Language (plan step 2), ~½ day
1. **XS:** move the 3 French strings in `WordSearchScreen` to `strings.xml` (en + fr).
2. **S:** `MainActivity` → `AppCompatActivity`; add `locales_config.xml` and `android:localeConfig`, plus `AppLocalesMetadataHolderService` with `autoStoreLocales`.
3. **S:** Settings reads and writes only through `AppCompatDelegate.get/setApplicationLocales`. Remove `restoreLocale()`/`runBlocking` and migrate or remove `language_tag`.
4. **S:** Hidden Word picks its content language from the app locale, not the Application context.
5. **S:** device check on API 33+ and ≤ 32: switch, rotate, kill and reopen, system per-app setting.

### Kakuro (hot-fix now, plan step 4), ~3–5 days
1. **XS (now):** delete `e_02` from `kakuro_easy.json`. It is unwinnable.
2. **S:** add a recent-history picker so the few puzzles don't repeat back-to-back. Optional, superseded by 4.
3. **M:** Kakuro engine in JVM code with tests: solution counter, technique solver and grader.
4. **M–L:** offline layout generator + filler + uniqueness loop → bank writer test → `kakuro_bank_<level>` resources (~250 per level), served with the recent picker.
5. **S:** deduction-based or solution-consistent hint; Kakuro dark-mode colours (or leave those to step 7).

### Save & resume (plan step 5) and stats (step 6), ~2–3 days + ~1–2 days
1. **S:** add the kotlinx.serialization plugin and runtime, typed DataStore, an `@ApplicationScope` scope, and ProGuard checks.
2. **S:** `GameSession.snapshot()/restore()`; `@Serializable` DTOs per game (Sudoku, Crowns) built from the existing saver encodings, with a `version` field.
3. **M:** a `GameSaveStore<T>` per game: save after each move and on background, clear on win or new game. Load in the VM `init` when continuing.
4. **M:** "Continue / New game" on open (the picker or the game entry), navigating with the saved difficulty and mode.
5. **S (step 6):** a `StatsStore` (separate typed DataStore), recording played (on first move or new puzzle) and won/time (on win), per game × difficulty × mode.
6. **M (step 6):** Stats screen from Home, with reset per game or all and confirmation.
7. Kakuro save after its step 4 rewrite.
