# BrainGames plan

Steps run in order. Do not start a step until it is asked for.
Background for steps 2, 4, 5 and 6 is in [DIAGNOSIS.md](DIAGNOSIS.md).

1. ✅ **Diagnose**: language option, Kakuro difficulty, saving the current game. See `DIAGNOSIS.md`.
2. ✅ **Hide sound and fix the language option** (checked on an Android 8.1 device; Android 13+ not yet checked on a device)
   - Hide sound behind a feature flag (keep the code).
   - Language: `MainActivity` becomes an `AppCompatActivity`; add `locales_config.xml` + `android:localeConfig` and `AppLocalesMetadataHolderService` (`autoStoreLocales`). `AppCompatDelegate` is the single source of truth: drop `restoreLocale()` / `runBlocking` and migrate the `language_tag` pref.
   - Move the 3 hard-coded French strings in `WordSearchScreen` to `strings.xml`; Hidden Word picks its content language from the app locale, not the Application context.
   - Remove the unwinnable Kakuro puzzle `e_02` from `kakuro_easy.json`.
   - Check on an Android 13+ device and an Android ≤ 12 emulator: switch, rotate, kill and reopen, system per-app language setting.
3. ✅ **Crowns additions** (unit-tested; not yet checked on a device)
   - A "crowns placed / total" counter, in both modes.
   - Classic only: a crown that is not part of the solution is marked as wrong, and each one revealed costs one hint. Done automatically as the crown is placed, while hints last; a cell is charged only once per puzzle, and the hint button leaves crowns already shown as wrong to the player.
   - Hardcore unchanged.
4. ✅ **Fix Kakuro difficulty**: replaced the 13 hand-made puzzles with 250 generated puzzles per level (6×6 Very Easy … 10×10 Expert), each unique, solvable by logic and graded by technique (`engine/`, `res/raw/kakuro_bank_*.txt`). The hint fills in the unique solution; it doesn't explain the deduction. Level bands are a first guess: tune after play-testing. Installed on a Pixel 8 (with a temporary Expert bank).
   - Offline generator: layout with target density, digit filler, clues from the fill, uniqueness check (stop at 2 solutions).
   - Technique-graded solver and grader, like Crowns; bank writer test producing ~250 puzzles per level.
   - Serve from the bank with a recent-history picker (reuse `RecentCrownsPuzzles`).
   - Hint consistent with the unique solution, ideally deduction-based.
5. ✅ **Save & resume** (unit-tested; not yet checked on a device): Sudoku, Crowns and Kakuro.
   - One save per game, in its own typed DataStore file (`files/saves/<game>.save`), in `core/save/`.
   - Changed from the first design: the save holds the same values each game already keeps in its `SavedStateHandle` (its saver's keys and `GameSession`'s), in a small versioned binary format. No kotlinx.serialization or DTOs were needed, and each game's existing saver stays the only place that encodes its board.
   - Saved after each move and when the screen is left, on an application-scoped coroutine scope. Deleted on win and replaced by a new puzzle.
   - The picker shows "Continue" (level, mode, time) above the levels; starting a new level over a saved game asks first.
   - A save from another mode or level is never loaded into the wrong game.
   - Saves are included in Android's default cloud backup; decide in step 8 whether to exclude them.
6. ✅ **Stats** (unit-tested; not yet checked on a device): Sudoku, Kakuro and Crowns. Hidden Word doesn't record stats yet.
   - Per game, per difficulty and per mode (Classic / Hardcore): games played, games won, best time, average time. Each game also shows its totals ("12 played · 8 won"), and each mode an "All levels" row.
   - A game counts as played on its first move, once per puzzle (opening it, rotating or Continue don't count it again); a win counts once, with its time.
   - Stored in its own Preferences DataStore file (`stats`), apart from settings and saves; total won time is kept and the average computed.
   - A Stats screen, opened from the chart icon on the Home screen.
   - Reset per game or all, with confirmation.
7. ✅ **Cleanup pass** (unit-tested; not yet checked on a device)
   - Kakuro dark mode: the grid takes its colours from the theme (blocks, clues, cells, selection, lines) instead of fixed white and dark grey. Sudoku's conflicts use the theme's error colour.
   - New look, asked for after the device check: Kakuro and Crowns are drawn as rounded tiles on a board (light grey-blue, or dark navy in dark themes), with fixed vivid colours in every colour theme. Kakuro: amber clue tiles, empty blocks, the selected cell in blue and its row and column tinted. Crowns: 12 vivid region colours with dark marks and crowns, the same small gap between every tile (no region lines).
   - Crowns: a sixth level, Master 12×12 (Crowns only; `GameType.levels`), with its own 250-puzzle bank. In portrait the grid now runs nearly edge to edge (4dp), and asks Android to ignore the back gesture over it (honoured for 200dp of height per edge). The level picker scrolls when its buttons don't fit.
   - Kakuro clue cells (found on the device): the diagonal ran the wrong way and both sums sat on it. Now drawn the usual way: a top-left to bottom-right diagonal, the across sum in the top-right triangle and the down sum in the bottom-left one, clear of the line.
   - Hard-coded text: the back buttons in the level picker and Settings, and the ✕ / ✏ buttons of the number pad, now have labels for screen readers (EN/FR).
   - French Settings: the five colour swatches share the row's width and long names wrap, so "Minuit" is no longer cut off.
   - Follow-ups list, fixed: the win sound no longer replays on rotation; Kakuro's error sound no longer comes one move late; two quick "New puzzle" taps load one puzzle (all games); `.gitattributes` keeps `gradlew` in LF; the template tests are gone; deprecated volume icons replaced.
   - Left as they are: the selected cell and notes mode reset after the app is killed in the background (the board itself is saved); Hidden Word's fixed yellow and grey (step 9); Gradle 10 deprecation warnings, which come from the Android Gradle Plugin 8.9 itself and go away with a plugin upgrade.
8. **New app icon, then release**: adaptive icon, monochrome layer and Play Store icon, then release to Play internal testing.
9. **Later**: Kakuro Hardcore, Hidden Word, sound.
