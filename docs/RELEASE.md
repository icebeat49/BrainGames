# Releasing BrainGames to Play internal testing

Plan step 8. The steps marked **you** happen in your own accounts and tools: Claude never handles the upload key, its passwords or the Play Console.

## 1. Create the upload key (you, once)

The upload key signs every bundle you send to Play. Keep the file and its passwords somewhere safe (a password manager): losing them means asking Google to reset the key.

Create it outside the repository, for example in `D:\keys`:

```powershell
& "C:\Program Files\Java\jdk-18.0.1\bin\keytool.exe" -genkeypair -v -keystore D:\keys\braingames-upload.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload
```

keytool asks for a password and a name; the name can be yours or the company's.

## 2. Point the build at the key (you, once)

Create `keystore.properties` at the project root (`D:\workspace\BrainGames\keystore.properties`). Git ignores it.

```properties
storeFile=D:/keys/braingames-upload.jks
storePassword=…
keyAlias=upload
keyPassword=…
```

Without this file, release builds are signed with the debug key: fine for testing on a phone, rejected by Play.

## 3. Build the bundle

```powershell
.\gradlew.bat bundleRelease
```

The bundle is `app\build\outputs\bundle\release\app-release.aab`. Before each new upload, raise `versionCode` (and `versionName` if you like) in `app/build.gradle.kts`: Play refuses a version code it has already seen.

## 4. Play Console (you)

1. **Create the app**: name *BrainGames*, default language English (United States), app, free.
2. **Play App Signing**: accept it. Google keeps the app signing key; your key from step 1 is only the upload key.
3. **App content** (Policy section):
   - **Privacy policy**: Play asks for a URL. The app collects nothing, so a short page saying so is enough (draft below).
   - **Data safety**: *No* data collected, *no* data shared. The app has no internet permission; settings, saves and stats stay on the device and in the user's own Android backup.
   - **Ads**: no ads.
   - **App access**: all features available without special access.
   - **Content rating**: questionnaire, category *Puzzle* / games, no violence, no user interaction, no data sharing: expect *Everyone* / PEGI 3.
   - **Target audience**: 13+ is the simplest (choosing under-13 brings the Families policy requirements).
4. **Store listing**, in English and French (drafts below): short description, full description, the icon `store/play_icon_512.png`, a 1024×500 feature graphic, and at least 2 phone screenshots per language.
5. **Testing → Internal testing**: create an email list of testers, create a release, upload the `.aab`, add release notes, roll out. Testers join through the opt-in link the console shows.

## Store listing drafts

**Short description** (80 characters max)

- EN: Sudoku, Kakuro and Crowns: logic puzzles with no ads and no account.
- FR : Sudoku, Kakuro et Couronnes : des casse-têtes logiques, sans pub ni compte.

**Full description**

EN:

> Three logic puzzles in one app, each with five or six levels.
>
> • Sudoku: fill the grid so every row, column and box holds 1 to 9.
> • Kakuro: fill the cells so each run adds up to its clue, with no repeated digit.
> • Crowns: place one crown in every row, column and colour region, with no two crowns touching. From 7×7 to the 12×12 Master level.
>
> Classic mode helps you along with highlights and hints; Hardcore is pen and paper, with a check at the end. Every puzzle has a single solution and can be solved by logic alone.
>
> Your game is saved as you play and waits for you when you come back. Stats follow your games played, wins and best times per level. Light and dark themes, English and French.
>
> No ads, no account, no internet needed.

FR :

> Trois casse-têtes logiques dans une seule application, chacun avec cinq ou six niveaux.
>
> • Sudoku : remplissez la grille pour que chaque ligne, colonne et région contienne 1 à 9.
> • Kakuro : remplissez les cases pour que chaque suite donne la somme indiquée, sans chiffre répété.
> • Couronnes : placez une couronne dans chaque ligne, colonne et région de couleur, sans que deux couronnes se touchent. Du 7×7 jusqu'au niveau Maître en 12×12.
>
> Le mode Classique vous aide avec des surlignages et des indices ; le mode Hardcore, c'est le papier-crayon, avec une vérification à la fin. Chaque grille a une seule solution et se résout par la logique.
>
> Votre partie est enregistrée en jouant et vous attend à votre retour. Les statistiques suivent vos parties jouées, gagnées et vos meilleurs temps par niveau. Thèmes clair et sombre, en français et en anglais.
>
> Sans pub, sans compte, sans connexion Internet.

**Release notes** (first internal test)

- EN: First test version: Sudoku, Kakuro and Crowns.
- FR : Première version de test : Sudoku, Kakuro et Couronnes.

## Privacy policy draft

> BrainGames does not collect, store on a server or share any personal data. The app has no internet access. Your settings, games in progress and statistics are kept on your device and, if you use Android backup, in your own Google account backup, which Google manages. Contact: <your email>.

Host it on any public page you control (a company site page or a public document) and paste the URL in the Play Console.

## Checked before the first upload

- Release build (R8 code and resource shrinking) installed and launched on a Pixel 8: 3.1 MB APK, 5.3 MB bundle.
- Hidden Word is off in release builds; sound is off in every build.
- Backup keeps everything (settings, language, stats, games in progress).
