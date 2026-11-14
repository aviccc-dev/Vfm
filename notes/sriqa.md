# הסריקה — בדיקה מכנית של הקוד (נוצר ב־`source/build/sriqa.py`; לא לערוך ביד)

כל שורה כאן היא **חשוד**, לא באג: המכונה מסמנת איפה כדאי לקרוא. ההכרעה — בקריאה (חלק 2 של הבדיקה הגדולה).

**בקצרה:**
- קטעים שחוזרים: 2 (כ־18 שורות); הגדול: 10 שורות — masachim/IniEditScreen.java:51 ↔ masachim/SearchScreen.java:270
- קטעים דומים במבנה: 35 (כ־225 שורות); הגדול: 10 שורות — masachim/IniEditScreen.java:51 ↔ masachim/SearchScreen.java:270
- דפוסים שחוזרים ב־6 מקומות ומעלה: 1; הנפוץ: 7 מקומות ב־2 קבצים
- בלי פונים: 0 פונקציות (מ־2116), 0 קבועים (מ־1858); רק מהבדיקות: 8
- פונקציות סטטיות בלי פונים (לפי המחלקה): 0; רק מהבדיקות: 10
- מילים במילון בלי שימוש: 0 (מ־1430)
- אותו טקסט פעמיים במילון: 35
- פתיחת מסד בלי סגירה ב־finally: 5
- קבצים: 136 · שורות: 40893 · פונקציות של 90+ שורות: 16
- אזהרות המהדר: deprecation 22, serial 4, rawtypes 1, fallthrough 1

## 1. קטעי קוד שחוזרים (8 שורות קוד זהות ומעלה, בלי הערות ורווחים)

נמצאו 2 קטעים; סך הכול כ־18 שורות כפולות.

| שורות | מקום א | מקום ב |
|---|---|---|
| 10 | `masachim/IniEditScreen.java` 51–62 | `masachim/SearchScreen.java` 270 |
| 8 | `pkudot/DbListCmd.java` 270–278 | `pkudot/DbListCmd.java` 357 |

## 1ב. קטעים דומים — אותו מבנה, שמות ומספרים אחרים (6 שורות ומעלה)

נמצאו 35 קטעים; סך הכול כ־225 שורות. זה המקום לחפש "אותה פונקציה פעמיים".

| שורות | מקום א | מקום ב |
|---|---|---|
| 10 | `masachim/IniEditScreen.java` 51 | `masachim/SearchScreen.java` 270 |
| 8 | `masachim/SettingsScreen.java` 266 | `masachim/SettingsScreen.java` 289 |
| 8 | `pkudot/DbListCmd.java` 270 | `pkudot/DbListCmd.java` 357 |
| 7 | `chomer/indeks/Index.java` 1110 | `chomer/indeks/Index.java` 1123 |
| 7 | `chomer/masad/MemStore.java` 69 | `chomer/masad/MemStore.java` 78 |
| 7 | `chomer/masad/SqlStore.java` 342 | `chomer/masad/SqlStore.java` 357 |
| 7 | `chomer/masad/SqlStore.java` 367 | `chomer/masad/SqlStore.java` 382 |
| 7 | `masachim/BarService.java` 175 | `masachim/IniEditScreen.java` 52 |
| 7 | `masachim/DupScreen.java` 170 | `masachim/IniEditScreen.java` 49 |
| 7 | `masachim/rechivim/FnSettings.java` 22 | `masachim/rechivim/FnTrash.java` 23 |
| 6 | `basis/Files.java` 31 | `chomer/indeks/Sig.java` 29 |
| 6 | `basis/Text.java` 119 | `basis/Text.java` 148 |
| 6 | `chomer/indeks/Compare.java` 87 | `chomer/indeks/Compare.java` 97 |
| 6 | `chomer/indeks/Index.java` 410 | `chomer/indeks/Index.java` 422 |
| 6 | `chomer/indeks/Library.java` 1307 | `chomer/indeks/Library.java` 1406 |
| 6 | `chomer/masad/MemStore.java` 55 | `chomer/masad/MemStore.java` 64 |
| 6 | `chomer/masad/SqlStore.java` 98 | `chomer/masad/SqlStore.java` 122 |
| 6 | `lev/Bridge.java` 342 | `lev/Bridge.java` 388 |
| 6 | `lev/Lab.java` 113 | `pkudot/Shortcuts.java` 59 |
| 6 | `masachim/DbScreen.java` 124 | `masachim/IniListScreen.java` 26 |
| 6 | `masachim/DupScreen.java` 170 | `masachim/IniListScreen.java` 32 |
| 6 | `masachim/DupScreen.java` 785 | `masachim/SearchScreen.java` 956 |
| 6 | `masachim/IniEditScreen.java` 36 | `masachim/RunScreen.java` 69 |
| 6 | `masachim/IniEditScreen.java` 67 | `masachim/SearchScreen.java` 317 |
| 6 | `masachim/SearchScreen.java` 452 | `masachim/SettingsScreen.java` 183 |
| 6 | `masachim/SettingsScreen.java` 41 | `masachim/SettingsScreen.java` 74 |
| 6 | `masachim/rechivim/Actions.java` 115 | `masachim/rechivim/Actions.java` 147 |
| 6 | `masachim/rechivim/CmdMenu.java` 96 | `masachim/rechivim/FnCommand.java` 38 |
| 6 | `masachim/rechivim/DbCmdMenu.java` 45 | `masachim/rechivim/DbVerbsUi.java` 237 |
| 6 | `masachim/rechivim/PhysicalUi.java` 471 | `masachim/rechivim/PhysicalUi.java` 510 |
| 6 | `masachim/rechivim/Select.java` 88 | `masachim/rechivim/Select.java` 103 |
| 6 | `masachim/rechivim/Ui.java` 304 | `masachim/rechivim/Ui.java` 358 |
| 6 | `pkudot/Batch.java` 2408 | `pkudot/Batch.java` 2431 |
| 6 | `pkudot/DbListCmd.java` 579 | `pkudot/DbListCmd.java` 766 |
| 6 | `pkudot/DbListCmd.java` 642 | `pkudot/DbListCmd.java` 675 |

## 1ג. דפוסים שחוזרים הרבה — אותה צורה של 6 שורות ב־6 מקומות ומעלה

נמצאו 1 דפוסים. כל אחד מועמד ל"פונקציה אחת במקום N העתקים".

| מקומות | קבצים | לדוגמה |
|---|---|---|
| 7 | 2 | `basis/Conf.java` 11 · `basis/Conf.java` 12 · `basis/Conf.java` 14 · `basis/Conf.java` 15 · `basis/Conf.java` 16 |

## 2. פונקציות וקבועים שאיש לא פונה אליהם

נבדקו 2116 פונקציות ו־1858 קבועים, לפי השם: פונקציה — ששום "שם(" בקוד אינו קריאה אליה; קבוע — ששמו מופיע רק בהגדרה. שם שמשותף לכמה פונקציות נספר יחד; פונקציה של הגשר (JavascriptInterface) ו־Override — לא נספרות.

**פונקציות (0):** 

**קבועים (0):** 

**רק הבדיקות פונות אליהם (8):** `chomer/indeks/Library.java:57 forTests` · `chomer/indeks/Library.java:355 useMedia` · `chomer/masad/DbMore.java:43 DOWN` · `chomer/masad/IdCounter.java:35 ceiling` · `masachim/rechivim/Actions.java:218 fitting` · `pkudot/CmdTemplates.java:121 copyToMine` · `pkudot/Commands.java:134 switchFor` · `pkudot/Commands.java:292 hebrewText`

## 2ב. פונקציות סטטיות שאיש לא פונה אליהן — לפי המחלקה (גם כששם הפונקציה משותף לאחרות)

**בלי פונים (0):** 

**רק הבדיקות פונות (10):** `basis/Modes.java:73 create` · `chomer/indeks/Library.java:57 forTests` · `chomer/indeks/Sig.java:161 forget` · `chomer/konan/Physical.java:280 move` · `masachim/rechivim/Actions.java:218 fitting` · `masachim/rechivim/Actions.java:224 all` · `pkudot/CmdMatrix.java:223 needed` · `pkudot/CmdTemplates.java:121 copyToMine` · `pkudot/CmdTemplates.java:126 rename` · `pkudot/Commands.java:292 hebrewText`

## 2ג. מילים במילון שאיש לא משתמש בהן (נבדקו 1430, לפי הקבוצה: Words.<קבוצה>.<מילה>)

לא נמצא.

## 3. אותו טקסט פעמיים במילון (קבועים בני 6 תווים ומעלה)

נמצאו 35 טקסטים.

- " קבצים" — FILES_SUFFIX (שורה 622) · FILES_SUFFIX (שורה 969) · FILES (שורה 1294) · FILES (שורה 1430)
- "הגדרות" — SETTINGS (שורה 805) · SETTINGS (שורה 1320) · SETTINGS (שורה 1460) · S_SETTINGS (שורה 1470)
- "קבצים פיזיים" — HINT (שורה 1421) · FILES (שורה 1460) · S_DISK (שורה 1469)
- "רשימה חדשה" — NEW_LIST (שורה 12) · NEW_LIST_TITLE (שורה 2324)
- "כל הרשימות" — ALL_LISTS (שורה 15) · ALL_LISTS (שורה 2319)
- "תיקייה חדשה" — NEW_FOLDER (שורה 18) · W_MD (שורה 1600)
- "לא נבחר דבר" — NOTHING_SELECTED (שורה 72) · NOTHING_CHOSEN (שורה 2306)
- "⚙ הגדרות" — SETTINGS (שורה 153) · SETTINGS (שורה 2225)
- "אין מקורות = כל הכוננים" — ALL_DRIVES (שורה 154) · NO_SOURCES_ALL (שורה 1135)
- "הועתקו" — COPIED (שורה 367) · COPIED (שורה 675)
- "הועברו" — MOVED (שורה 368) · MOVED (שורה 676)
- "שבועית" — WEEKLY (שורה 535) · WHY_WEEKLY (שורה 1009)
- "זה סל המחזור" — IS_TRASH (שורה 602) · IS_TRASH (שורה 1093)
- "זו תיקיית התוכנה" — IS_APP_FOLDER (שורה 603) · IS_APP_FOLDER (שורה 1092)
- ", פנוי " — NO_ROOM_FREE (שורה 613) · FREE (שורה 1204)
- "אפשר לשחזר מסל המחזור" — UNDO_FROM_TRASH (שורה 641) · IN_BIN (שורה 979)
- "מחיקה מהכונן" — DELETE_ON_DRIVE (שורה 646) · DELETE_ON_DRIVE (שורה 1043)
- " קבצים · " — FILES_DOT (שורה 692) · FILES_DOT (שורה 787)
- "מחיקה לצמיתות" — PURGE (שורה 706) · PURGE (שורה 1044)
- "סורק את " — SCANNING (שורה 791) · SCANNING (שורה 1021)
- "כפולים" — DUPLICATES (שורה 836) · DUPS (שורה 1132)
- " — מנווטים לתיקייה ולוחצים \"הדבקה כאן\"" — PASTE_HINT (שורה 889) · PASTE_HINT_FOLDER (שורה 1052)
- "שם חדש" — NEW_NAME (שורה 936) · P_NEW_NAME (שורה 1613)
- " קבצים…" — FILES_DOTS (שורה 948) · FILES_DOTS (שורה 998)
- "סל מחזור" — TRASH_TITLE (שורה 961) · TRASH (שורה 1311)
- " תיקיות" — FOLDERS_SUFFIX (שורה 1105) · FOLDERS (שורה 1429)
- "לא ניתן לפתוח את מסך ההרשאה" — CANT_OPEN_PERMISSION (שורה 1179) · CANT_OPEN_PERMISSION (שורה 1227)
- "שם הרשימה" — LIST_NAME (שורה 1378) · LIST_NAME (שורה 2322)
- "סל המחזור" — TRASH (שורה 1465) · S_TRASH (שורה 1469)
- "עורך הפקודות" — EDITOR (שורה 1465) · S_BATCH (שורה 1470)
- "שינוי שם" — RENAME (שורה 1545) · W_REN (שורה 1600)
- "הרצה: " — RUN (שורה 1569) · TITLE (שורה 1688)
- "אין בית במסד הזה" — NO_HOME (שורה 1892) · NO_HOME (שורה 2299)
- "שם האופן" — H_DROP_MODE (שורה 2098) · MODE_NAME (שורה 2280)
- "📋 העתקה" — DUPLICATE (שורה 2224) · COPY_REPORT (שורה 2371)

## 4. מסד שנפתח, ואין אחריו סגירה ב־finally (ב־120 השורות הבאות)

`chomer/masad/BackupContract.java:115 t` · `chomer/masad/BackupContract.java:428 s` · `chomer/masad/BackupContract.java:432 n` · `masachim/DbListScreen.java:171 store` · `masachim/DbScreen.java:192 store`

## 5. גדלים

**קבצים:** 136 · **שורות:** 40893

**הקבצים הגדולים:** `pkudot/Batch.java` 2515 · `basis/Words.java` 2481 · `chomer/indeks/Library.java` 1464 · `pkudot/DbListCmd.java` 1389 · `chomer/indeks/Index.java` 1145 · `chomer/konan/Physical.java` 1082 · `masachim/SearchScreen.java` 977 · `chomer/masad/SqlStore.java` 977 · `masachim/rechivim/Ui.java` 863 · `masachim/DupScreen.java` 796 · `masachim/DbListScreen.java` 777 · `masachim/DbScreen.java` 752 · `masachim/rechivim/SmartForm.java` 720 · `lev/Bridge.java` 702 · `chomer/masad/Backups.java` 664

**פונקציות של 90 שורות ומעלה (16):** `pkudot/Batch.java:1251 plan` 236 · `masachim/rechivim/SmartForm.java:303 show` 222 · `pkudot/Batch.java:1556 plan` 191 · `pkudot/DbListCmd.java:564 plan` 188 · `pkudot/Batch.java:186 small` 175 · `pkudot/Batch.java:2181 run` 163 · `masachim/BarService.java:156 build` 124 · `masachim/DupScreen.java:134 onCreate` 109 · `masachim/rechivim/PickDialog.java:100 open` 105 · `chomer/indeks/Library.java:876 search` 103 · `masachim/itzuv/DesignUi.java:171 form` 102 · `pkudot/Verbs.java:119 parse` 98 · `masachim/rechivim/QuickBar.java:198 edit` 95 · `masachim/rechivim/FilterForm.java:21 build` 93 · `masachim/rechivim/FnDup.java:39 configure` 92 · `masachim/itzuv/Looks.java:166 apply` 91

**לפי קומה:** basis — 12 קבצים, 3937 שורות · chomer — 46 קבצים, 13405 שורות · pkudot — 13 קבצים, 6143 שורות · lev — 4 קבצים, 1700 שורות · masachim — 61 קבצים, 15708 שורות

## 6. אזהרות המהדר (javac -Xlint:all)

- **deprecation** — 22: BarService 5 · LabScreen 4 · Bridge 3 · Paths 2 · Db 2 · Vols 1 · Resolver 1 · Menus 1
- **serial** — 4: Library 1 · SqlStore 1 · Store 1 · Db 1
- **rawtypes** — 1: Library 1
- **fallthrough** — 1: Physical 1
