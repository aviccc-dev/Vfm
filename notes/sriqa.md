# הסריקה — בדיקה מכנית של הקוד (נוצר ב־`source/build/sriqa.py`; לא לערוך ביד)

כל שורה כאן היא **חשוד**, לא באג: המכונה מסמנת איפה כדאי לקרוא. ההכרעה — בקריאה (חלק 2 של הבדיקה הגדולה).

**בקצרה:**
- קטעים שחוזרים: 2 (כ־18 שורות); הגדול: 10 שורות — masachim/IniEditScreen.java:51 ↔ masachim/SearchScreen.java:273
- קטעים דומים במבנה: 42 (כ־273 שורות); הגדול: 10 שורות — masachim/IniEditScreen.java:51 ↔ masachim/SearchScreen.java:273
- דפוסים שחוזרים ב־6 מקומות ומעלה: 1; הנפוץ: 7 מקומות ב־2 קבצים
- בלי פונים: 0 פונקציות (מ־2363), 0 קבועים (מ־2026); רק מהבדיקות: 10
- פונקציות סטטיות בלי פונים (לפי המחלקה): 0; רק מהבדיקות: 12
- מילים במילון בלי שימוש: 0 (מ־1563)
- אותו טקסט פעמיים במילון: 36
- פתיחת מסד בלי סגירה ב־finally: 4
- קבצים: 142 · שורות: 45220 · פונקציות של 90+ שורות: 20
- אזהרות המהדר: deprecation 23, serial 4, rawtypes 1, fallthrough 1

## 1. קטעי קוד שחוזרים (8 שורות קוד זהות ומעלה, בלי הערות ורווחים)

נמצאו 2 קטעים; סך הכול כ־18 שורות כפולות.

| שורות | מקום א | מקום ב |
|---|---|---|
| 10 | `masachim/IniEditScreen.java` 51–62 | `masachim/SearchScreen.java` 273 |
| 8 | `pkudot/DbListCmd.java` 340–348 | `pkudot/DbListCmd.java` 431 |

## 1ב. קטעים דומים — אותו מבנה, שמות ומספרים אחרים (6 שורות ומעלה)

נמצאו 42 קטעים; סך הכול כ־273 שורות. זה המקום לחפש "אותה פונקציה פעמיים".

| שורות | מקום א | מקום ב |
|---|---|---|
| 10 | `masachim/IniEditScreen.java` 51 | `masachim/SearchScreen.java` 273 |
| 8 | `masachim/rechivim/DbVerbsUi.java` 327 | `masachim/rechivim/DbVerbsUi.java` 346 |
| 8 | `masachim/rechivim/PhysicalUi.java` 460 | `masachim/rechivim/PhysicalUi.java` 504 |
| 8 | `pkudot/DbListCmd.java` 340 | `pkudot/DbListCmd.java` 431 |
| 7 | `chomer/indeks/Index.java` 1119 | `chomer/indeks/Index.java` 1132 |
| 7 | `chomer/masad/MemStore.java` 69 | `chomer/masad/MemStore.java` 78 |
| 7 | `chomer/masad/SqlStore.java` 342 | `chomer/masad/SqlStore.java` 357 |
| 7 | `chomer/masad/SqlStore.java` 367 | `chomer/masad/SqlStore.java` 382 |
| 7 | `masachim/BarService.java` 132 | `masachim/RunService.java` 69 |
| 7 | `masachim/BarService.java` 175 | `masachim/IniEditScreen.java` 52 |
| 7 | `masachim/DupScreen.java` 170 | `masachim/IniEditScreen.java` 49 |
| 7 | `masachim/rechivim/DbVerbsUi.java` 27 | `masachim/rechivim/DbVerbsUi.java` 41 |
| 7 | `masachim/rechivim/FnSettings.java` 22 | `masachim/rechivim/FnTrash.java` 23 |
| 7 | `pkudot/DbListCmd.java` 1446 | `pkudot/DbListCmd.java` 1539 |
| 7 | `pkudot/DbListCmd.java` 1459 | `pkudot/DbListCmd.java` 1552 |
| 6 | `basis/Files.java` 31 | `chomer/indeks/Sig.java` 29 |
| 6 | `basis/Text.java` 119 | `basis/Text.java` 148 |
| 6 | `chomer/indeks/Compare.java` 87 | `chomer/indeks/Compare.java` 97 |
| 6 | `chomer/indeks/Index.java` 419 | `chomer/indeks/Index.java` 431 |
| 6 | `chomer/indeks/Library.java` 1315 | `chomer/indeks/Library.java` 1414 |
| 6 | `chomer/konan/Physical.java` 420 | `chomer/konan/Physical.java` 453 |
| 6 | `chomer/masad/MemStore.java` 55 | `chomer/masad/MemStore.java` 64 |
| 6 | `chomer/masad/SqlStore.java` 98 | `chomer/masad/SqlStore.java` 122 |
| 6 | `lev/Bridge.java` 353 | `lev/Bridge.java` 399 |
| 6 | `lev/Lab.java` 115 | `pkudot/Shortcuts.java` 59 |
| 6 | `masachim/DbListScreen.java` 425 | `masachim/DbScreen.java` 627 |
| 6 | `masachim/DbScreen.java` 133 | `masachim/IniListScreen.java` 26 |
| 6 | `masachim/DupScreen.java` 170 | `masachim/IniListScreen.java` 32 |
| 6 | `masachim/DupScreen.java` 785 | `masachim/SearchScreen.java` 970 |
| 6 | `masachim/IniEditScreen.java` 36 | `masachim/RunScreen.java` 69 |
| 6 | `masachim/IniEditScreen.java` 67 | `masachim/SearchScreen.java` 320 |
| 6 | `masachim/SearchScreen.java` 458 | `masachim/SettingsScreen.java` 184 |
| 6 | `masachim/SettingsScreen.java` 41 | `masachim/SettingsScreen.java` 74 |
| 6 | `masachim/rechivim/Actions.java` 115 | `masachim/rechivim/Actions.java` 147 |
| 6 | `masachim/rechivim/CmdMenu.java` 96 | `masachim/rechivim/FnCommand.java` 38 |
| 6 | `masachim/rechivim/PhysicalUi.java` 407 | `masachim/rechivim/PhysicalUi.java` 606 |
| 6 | `masachim/rechivim/Select.java` 88 | `masachim/rechivim/Select.java` 103 |
| 6 | `masachim/rechivim/Ui.java` 304 | `masachim/rechivim/Ui.java` 358 |
| 6 | `pkudot/Batch.java` 3253 | `pkudot/Batch.java` 3276 |
| 6 | `pkudot/DbListCmd.java` 659 | `pkudot/DbListCmd.java` 1269 |
| 6 | `pkudot/DbListCmd.java` 723 | `pkudot/DbListCmd.java` 756 |
| 6 | `pkudot/DbListCmd.java` 1469 | `pkudot/DbListCmd.java` 1562 |

## 1ג. דפוסים שחוזרים הרבה — אותה צורה של 6 שורות ב־6 מקומות ומעלה

נמצאו 1 דפוסים. כל אחד מועמד ל"פונקציה אחת במקום N העתקים".

| מקומות | קבצים | לדוגמה |
|---|---|---|
| 7 | 2 | `basis/Conf.java` 11 · `basis/Conf.java` 12 · `basis/Conf.java` 14 · `basis/Conf.java` 15 · `basis/Conf.java` 16 |

## 2. פונקציות וקבועים שאיש לא פונה אליהם

נבדקו 2363 פונקציות ו־2026 קבועים, לפי השם: פונקציה — ששום "שם(" בקוד אינו קריאה אליה; קבוע — ששמו מופיע רק בהגדרה. שם שמשותף לכמה פונקציות נספר יחד; פונקציה של הגשר (JavascriptInterface) ו־Override — לא נספרות.

**פונקציות (0):** 

**קבועים (0):** 

**רק הבדיקות פונות אליהם (10):** `chomer/indeks/Index.java:151 pathsCapacity` · `chomer/indeks/Library.java:57 forTests` · `chomer/indeks/Library.java:347 useMedia` · `chomer/masad/DbMore.java:43 DOWN` · `chomer/masad/IdCounter.java:35 ceiling` · `lev/Manager.java:62 registered` · `masachim/rechivim/Actions.java:218 fitting` · `pkudot/Commands.java:134 switchFor` · `pkudot/DbClip.java:70 anyMove` · `pkudot/DbListCmd.java:1969 nowEdit`

## 2ב. פונקציות סטטיות שאיש לא פונה אליהן — לפי המחלקה (גם כששם הפונקציה משותף לאחרות)

**בלי פונים (0):** 

**רק הבדיקות פונות (12):** `basis/Permit.java:65 isSystem` · `basis/Permit.java:90 strict` · `chomer/indeks/Library.java:57 forTests` · `chomer/indeks/Sig.java:161 forget` · `lev/Manager.java:62 registered` · `masachim/rechivim/Actions.java:218 fitting` · `masachim/rechivim/Actions.java:224 all` · `masachim/rechivim/DbSmartForm.java:93 save` · `pkudot/CmdMatrix.java:251 needed` · `pkudot/DbClip.java:70 anyMove` · `pkudot/DbClip.java:94 paste` · `pkudot/DbListCmd.java:1969 nowEdit`

## 2ג. מילים במילון שאיש לא משתמש בהן (נבדקו 1563, לפי הקבוצה: Words.<קבוצה>.<מילה>)

לא נמצא.

## 3. אותו טקסט פעמיים במילון (קבועים בני 6 תווים ומעלה)

נמצאו 36 טקסטים.

- " קבצים" — FILES_SUFFIX (שורה 712) · FILES_SUFFIX (שורה 1054) · FILES (שורה 1375) · FILES (שורה 1511)
- "הגדרות" — SETTINGS (שורה 892) · SETTINGS (שורה 1401) · SETTINGS (שורה 1540) · S_SETTINGS (שורה 1550)
- "תיקייה חדשה" — NEW_FOLDER (שורה 18) · W_MD (שורה 1681) · MAKE_FOLDER (שורה 2775)
- "לא נבחר דבר" — NOTHING_SELECTED (שורה 72) · NOTHING_SELECTED (שורה 253) · NOTHING_CHOSEN (שורה 2603)
- "שם אחר" — OTHER_NAME (שורה 544) · OTHER_NAME (שורה 778) · OTHER_NAME (שורה 1964)
- "מחיקה לצמיתות" — PURGE (שורה 796) · PURGE (שורה 1129) · FOREVER (שורה 2776)
- "קבצים פיזיים" — HINT (שורה 1502) · FILES (שורה 1540) · S_DISK (שורה 1549)
- "שינוי שם" — RENAME (שורה 1625) · W_REN (שורה 1681) · RENAME (שורה 2775)
- "רשימה חדשה" — NEW_LIST (שורה 12) · NEW_LIST_TITLE (שורה 2617)
- "כל הרשימות" — ALL_LISTS (שורה 15) · ALL_LISTS (שורה 2612)
- "⚙ הגדרות" — SETTINGS (שורה 153) · SETTINGS (שורה 2525)
- "אין מקורות = כל הכוננים" — ALL_DRIVES (שורה 154) · NO_SOURCES_ALL (שורה 1216)
- "הועתקו" — COPIED (שורה 444) · COPIED (שורה 765)
- "הועברו" — MOVED (שורה 445) · MOVED (שורה 766)
- "שבועית" — WEEKLY (שורה 622) · WHY_WEEKLY (שורה 1094)
- "זה סל המחזור" — IS_TRASH (שורה 692) · IS_TRASH (שורה 1178)
- "זו תיקיית התוכנה" — IS_APP_FOLDER (שורה 693) · IS_APP_FOLDER (שורה 1177)
- ", פנוי " — NO_ROOM_FREE (שורה 703) · FREE (שורה 1285)
- "אפשר לשחזר מסל המחזור" — UNDO_FROM_TRASH (שורה 731) · IN_BIN (שורה 1064)
- "מחיקה מהכונן" — DELETE_ON_DRIVE (שורה 736) · DELETE_ON_DRIVE (שורה 1128)
- " קבצים · " — FILES_DOT (שורה 782) · FILES_DOT (שורה 878)
- "סורק את " — SCANNING (שורה 882) · SCANNING (שורה 1106)
- "כפולים" — DUPLICATES (שורה 923) · DUPS (שורה 1213)
- " — מנווטים לתיקייה ולוחצים \"הדבקה כאן\"" — PASTE_HINT (שורה 976) · PASTE_HINT_FOLDER (שורה 1137)
- "שם חדש" — NEW_NAME (שורה 1021) · P_NEW_NAME (שורה 1694)
- " קבצים…" — FILES_DOTS (שורה 1033) · FILES_DOTS (שורה 1083)
- "סל מחזור" — TRASH_TITLE (שורה 1046) · TRASH (שורה 1392)
- " תיקיות" — FOLDERS_SUFFIX (שורה 1188) · FOLDERS (שורה 1510)
- "לא ניתן לפתוח את מסך ההרשאה" — CANT_OPEN_PERMISSION (שורה 1260) · CANT_OPEN_PERMISSION (שורה 1308)
- "שם הרשימה" — LIST_NAME (שורה 1459) · LIST_NAME (שורה 2615)
- "סל המחזור" — TRASH (שורה 1545) · S_TRASH (שורה 1549)
- "עורך הפקודות" — EDITOR (שורה 1545) · S_BATCH (שורה 1550)
- "הרצה: " — RUN (שורה 1650) · TITLE (שורה 1773)
- "אין בית במסד הזה" — NO_HOME (שורה 2052) · NO_HOME (שורה 2596)
- "שם האופן" — H_DROP_MODE (שורה 2371) · MODE_NAME (שורה 2579)
- "📋 העתקה" — DUPLICATE (שורה 2524) · COPY_REPORT (שורה 2664)

## 4. מסד שנפתח, ואין אחריו סגירה ב־finally (ב־120 השורות הבאות)

`chomer/masad/BackupContract.java:115 t` · `chomer/masad/BackupContract.java:428 s` · `chomer/masad/BackupContract.java:432 n` · `masachim/DbListScreen.java:175 store`

## 5. גדלים

**קבצים:** 142 · **שורות:** 45220

**הקבצים הגדולים:** `pkudot/Batch.java` 3360 · `basis/Words.java` 2797 · `pkudot/DbListCmd.java` 2109 · `chomer/indeks/Library.java` 1472 · `chomer/konan/Physical.java` 1272 · `chomer/indeks/Index.java` 1154 · `masachim/SearchScreen.java` 991 · `chomer/masad/SqlStore.java` 977 · `masachim/DbListScreen.java` 934 · `masachim/rechivim/Ui.java` 863 · `masachim/DupScreen.java` 796 · `masachim/DbScreen.java` 753 · `lev/Bridge.java` 722 · `masachim/rechivim/SmartForm.java` 720 · `chomer/masad/Backups.java` 672

**פונקציות של 90 שורות ומעלה (20):** `pkudot/Batch.java:264 small` 313 · `pkudot/Batch.java:1734 plan` 257 · `masachim/rechivim/SmartForm.java:303 show` 222 · `pkudot/DbListCmd.java:639 plan` 216 · `pkudot/Batch.java:2120 plan` 209 · `pkudot/Batch.java:3003 run0` 185 · `pkudot/DbListCmd.java:864 planPicked` 139 · `pkudot/Verbs.java:140 parse` 124 · `masachim/BarService.java:156 build` 124 · `masachim/DupScreen.java:134 onCreate` 109 · `masachim/rechivim/PickDialog.java:100 open` 105 · `chomer/indeks/Library.java:884 search` 103 · `masachim/itzuv/DesignUi.java:171 form` 102 · `masachim/rechivim/QuickBar.java:198 edit` 95 · `masachim/rechivim/FilterForm.java:21 build` 93 · `pkudot/DbListCmd.java:1256 planVerb` 92 · `pkudot/CmdMatrix.java:127 add` 92 · `masachim/rechivim/FnDup.java:39 configure` 92 · `masachim/itzuv/Looks.java:166 apply` 91 · `masachim/rechivim/BatchUi.java:131 preview` 90

**לפי קומה:** basis — 14 קבצים, 4541 שורות · chomer — 48 קבצים, 14450 שורות · pkudot — 13 קבצים, 7756 שורות · lev — 4 קבצים, 2161 שורות · masachim — 63 קבצים, 16312 שורות

## 6. אזהרות המהדר (javac -Xlint:all)

- **deprecation** — 23: BarService 5 · LabScreen 4 · Bridge 3 · Paths 2 · Db 2 · Vols 1 · Resolver 1 · Menus 1
- **serial** — 4: Library 1 · SqlStore 1 · Store 1 · Db 1
- **rawtypes** — 1: Library 1
- **fallthrough** — 1: Physical 1
