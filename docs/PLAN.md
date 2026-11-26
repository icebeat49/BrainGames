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
3. **Crowns additions**
   - A "crowns placed / total" counter, in both modes.
   - Classic only: a crown that is not part of the solution is marked as wrong, and each one revealed costs one hint.
   - Hardcore unchanged.
4. **Fix Kakuro difficulty** (~3–5 days): the current 13 puzzles can't be re-graded (only 3 have a unique solution), so replace them.
   - Offline generator: layout with target density, digit filler, clues from the fill, uniqueness check (stop at 2 solutions).
   - Technique-graded solver and grader, like Crowns; bank writer test producing ~250 puzzles per level.
   - Serve from the bank with a recent-history picker (reuse `RecentCrownsPuzzles`).
   - Hint consistent with the unique solution, ideally deduction-based.
5. **Save & resume**
   - One save per game, in its own typed DataStore file (`saves/<game>.json`) with kotlinx.serialization and `@Serializable` save DTOs (not the domain classes).
   - Saved after each move and when the app goes to the background, on an application-scoped coroutine scope.
   - `GameSession` gets `snapshot()` / `restore()` for time, hints and win state.
   - "Continue / New game" when opening a game, using the saved difficulty and mode.
   - Deleted on win.
   - Kakuro's save is designed after step 4.
6. **Stats**
   - Per game, per difficulty and per mode (Classic / Hardcore): games played, games won, best time, average time.
   - Stored in a separate typed DataStore (`stats.json`); keep total won time and compute the average.
   - A dedicated Stats screen, opened from the Home screen.
   - Reset per game or all, with confirmation.
7. **Cleanup pass**: Kakuro dark mode, hard-coded text, Follow-ups list. In French, the Settings colour-theme row is too wide ("Minuit" is cut off).
8. **New app icon, then release**: adaptive icon, monochrome layer and Play Store icon, then release to Play internal testing.
9. **Later**: Kakuro Hardcore, Hidden Word, sound.
