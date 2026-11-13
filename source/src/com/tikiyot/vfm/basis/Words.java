package com.tikiyot.vfm;

// מילון המונחים וניסוחי הפעולות. כל מסך לוקח מכאן, כדי שהניסוח לא יסטה.
// שני עולמות: בכונן (תיקייה, קובץ) וברשימות (רשימה, פריט).

public final class Words {

    private Words() { }

    // ---------- מונחים ----------
    public static final String SMART_LIST = "רשימה חכמה";
    public static final String NEW_LIST = "רשימה חדשה";
    /** 173: פריטי התפריט הקבועים בכל מסך רשימה. */
    public static final String FULL_SHORT = "נתונים מלאים / מקוצרים", SORT = "מיון";
    public static final String ALL_LISTS = "כל הרשימות",
            PICK_HERE = "לכאן", PICK_UP = "למעלה", VIEW_FOLDERS = "תיקיות", VIEW_TREE = "עץ",
            SEARCH_LIST = "חיפוש רשימה…",
            NEW_FOLDER = "תיקייה חדשה", RENAME_ON_DRIVE = "שינוי שם בכונן", SEARCH_FOLDER = "חיפוש תיקייה…",
            NO_LISTS_HERE = "אין כאן רשימות", NO_FOLDERS_HERE = "אין כאן תיקיות",
            INSIDE_LIST = "בתוך רשימה זו", INSIDE_FOLDER = "בתוך תיקייה זו",
            NOT_LIST_TARGET = "אי אפשר לבחור את הרשימה הזו כיעד";
    public static final String ROOT = "השורש";

    // ---------- פעולות ברשימות ----------
    public static final String ADD_TO_LIST = "הוספה לרשימה";
    public static final String TO_LIST_WORD = "לרשימה…";   // 1.16.0
    public static final String REMOVE_FROM_LIST = "הסרה מהרשימה";

    public static final String NO_TOUCH = "לא נוגע בקובץ";


    // ---------- פעולות בכונן ----------
    public static final String MOVE = "העברה";
    public static final String COPY = "העתקה";

    // ---------- בחירה מרובה ----------
    public static final String SELECT = "בחירה מרובה";
    public static final String SELECT_ALL = "בחירת הכול";
    public static final String DONE = "סיום";

    public static String selected(int n) {
        return n == 1 ? "פריט אחד נבחר" : n + " נבחרו";
    }

    // ---------- ספירות (131) — "קובץ אחד" / "3 קבצים" ממקום אחד ----------
    public static String files(long n) {
        return n == 1 ? "קובץ אחד" : n + " קבצים";
    }

    public static String folders(int n) {
        return n == 1 ? "תיקייה אחת" : n + " תיקיות";
    }

    public static String items(int n) {
        return n == 1 ? "פריט אחד" : n + " פריטים";
    }

    public static String lists(int n) {
        return n == 1 ? "רשימה אחת" : n + " רשימות";
    }

    /** "ועוד 7" — בסוף רשימת שמות מקוצרת. */
    public static String andMore(int n) {
        return "ועוד " + n;
    }

    // ---------- כפתורים ----------
    public static final String CANCEL = "ביטול", CLOSE = "סגירה", OK = "אישור", STOP = "עצור",
            SAVE = "שמירה", PASTE_HERE = "הדבקה כאן";

    // ---------- הודעות חוזרות (131) ----------
    public static final String NOTHING_SELECTED = "לא נבחר דבר";
    public static final String NEED_NAME = "צריך שם", NAME_OPTIONAL = "שם (לא חובה)";
    public static final String NAME_TAKEN_AT_DEST = "שם תפוס ביעד";
    public static final String LIST_NOT_FOUND = "הרשימה לא נמצאה";
    public static final String FILE_NOT_FOUND = "הקובץ לא נמצא", FOLDER_NOT_FOUND = "התיקייה לא נמצאה";
    public static final String FILE_NOT_AT_PATH = "הקובץ לא נמצא במסלול שלו";
    public static final String NO_TOUCH_FILES = "לא נוגע בקבצים";
    public static final String LOADING_INDEX = "טוען את האינדקס…";
    public static final String CANT_READ_FILE = "לא הצלחתי לקרוא את הקובץ";
    public static final String CANT_CREATE_FOLDER = "לא הצלחתי ליצור את התיקייה";
    public static final String BATCH_MENU = "⭐ פקודות";

    // ---------- נכשל ----------
    public static final String DELETE_FAILED = "המחיקה נכשלה", RENAME_FAILED = "שינוי השם נכשל",
            COPY_FAILED = "ההעתקה נכשלה", MOVE_FAILED = "ההעברה נכשלה", RESTORE_FAILED = "השחזור נכשל",
            SAVE_FAILED = "השמירה נכשלה";

    // ---------- סוגי קבצים (145) ----------
    /** 175: תיקיות ריקות. */
    public static final class Empty {
        private Empty() { }

        public static final String TITLE = "תיקיות ריקות";   // 201: בלי "רק"

        public static String count(int n, int inner) {
            return (n == 1 ? "תיקייה ריקה אחת" : Files.count(n) + " תיקיות ריקות")
                    + (inner > 0 ? " · ועוד " + Files.count(inner) + " בתוכן" : "");
        }

        public static String inside(int n) {
            return n == 1 ? "כולל תיקייה ריקה אחת בתוכה" : "כולל " + n + " תיקיות ריקות בתוכה";
        }
    }

    public static final class Kinds {
        private Kinds() { }

        public static final String TITLE = "סוגי קבצים", ALL = "הכול",   // 198: תיבה ראשונה בין הסוגים
                MORE = "עוד סיומות…", SEARCH = "חיפוש סוג (למשל zip)", NONE_PICKED = "לא נבחר סוג",
                NOT_IN_INDEX = "אין באינדקס";

        public static String picked(String list) {
            return "סיומות: " + list;   // 199: על הכפתור "עוד סיומות…"
        }

        /** "zip (57)". */
        public static String counted(String ext, int n) {
            return ext + " (" + String.format(java.util.Locale.ROOT, "%,d", n) + ")";
        }
    }

    // ---------- "הוספת חיפוש" (150) ----------
    public static final class SearchTabs {
        private SearchTabs() { }

        /** 197: בפס הקטן שבראש הטופס. */
        public static final String[] SHORT = {"חכמה", "כפולים"};
        public static final String KIND_L = "סוג";
    }

    // ---------- תוויות על הצלמית (160) ----------
    public static final class Tags {
        private Tags() { }

        public static final String FOLDER = "תיקייה\nאמיתית", SMART = "רשימה\nחכמה", MANUAL = "רשימה\nידנית",
                INTERNAL = "כונן\nפנימי", CARD = "כרטיס\nזיכרון",
                LINK = "↪\nקישור", CMD = "פקודה", LIVE = "תיקייה\nמתעדכנת";   // 1.17.0
    }

    // ---------- הדבקה ומנהל קבצים ברשימה החכמה (149) ----------
    public static final class Here {
        private Here() { }

        public static final String TO_ALL = "עבור למצב \"כולל הכול\" ולהדביק שם", PICK = "בחירת תיקייה…",
                LATER = "אחר כך", PASTE_PICK = "הדבקה…", PICK_TITLE = "להדביק לתיקייה", SHOW_HIDDEN = "הצגת מוסתרים", HIDE_HIDDEN = "הסתרת מוסתרים";
    }

    // ---------- החיפוש האחד (153) ----------
    public static final class Find {
        private Find() { }

        public static final String SETTINGS = "⚙ הגדרות", SAVE_AS = "💾 שמירה בשם — רשימה חכמה בבית…", SAVE_AS_TITLE = "שמירה בשם — רשימה חכמה בבית",
                ALL_DRIVES = "אין מקורות = כל הכוננים", HINT = "חיפוש…";

        public static String saved(String name) {
            return "נשמר: " + name;
        }
    }

    // ---------- רשימה חכמה בתיקיות (148) ----------
    public static final class Views {
        private Views() { }

        public static final String TITLE = "תצוגה", MENU = "תצוגה…", SOURCES = "המקורות", NO_MATCH_HERE = "אין כאן התאמות",
                NO_ACCESS = "התיקייה לא באינדקס", NOT_FOUND = "⚠ לא נמצאה בכונן — אפשר להוציא את המקור בהגדרות", UP = "⬆";

        public static String matches(int n) {
            return n == 1 ? "התאמה אחת" : n + " התאמות";
        }
    }

    // ---------- רשימות חכמות עם שם (147) ----------
    public static final class Named {
        public static final String PRESETS_ONLY = "תבנית חדשה לא נשמרת בגרסה הזו — רק התבניות המוכנות";
        private Named() { }

        // 154: "הגדרות נפוצות" (המשתמש: "צריך לשנות את השם — במקום 'שמירה בשם' — הגדרות נפוצות")
        public static final String SAVE_AS_TITLE = "שמירה כתבנית", NAME_HINT = "שם, למשל: ספרים מהחודש",
                GONE = "התבנית שהכלי מקשר אליה נמחקה מהקובץ הראשי";

        public static String exists(String name) {
            return "כבר יש תבנית בשם \"" + name + "\"";
        }

        public static final String OVERWRITE = "ההגדרות שלה יוחלפו בהגדרות שבטופס";

        public static final String UPDATE = "לעדכן";
    }

    // ---------- קובצי פקודות (117א) ----------
    public static final class Cmd {
        private Cmd() { }


        /** איך כותבים אותה — נבנה מההגדרה האחת (1.22.5: עם המתגים עצמם, במקום "[מתגים]"). */
        public static String usage(String cmd) {
            Commands.Def d = Commands.get(cmd);
            return d == null ? "" : d.usage();
        }

        // שגיאות בשורה
        public static final String MISSING_PERCENT = "חסר % סוגר";
        public static final String FOLDER_NOT_EMPTY = "התיקייה לא ריקה — כמו ב-bat, צריך RD /S";
        public static final String DEL_S = "DEL /S (גם בתיקיות שבפנים) — לא נתמך עדיין";
        public static final String NO_DEST = "היעד לא קיים או שאינו תיקייה — אפשר ליצור אותו קודם ב-MD";
        /** 1.19.0 */
        public static final String JOURNAL_USAGE = "/יומן:<קובץ> — רק ב־COPY וב־MOVE, עם שם קובץ";
        public static final String JOURNAL_FAILED = " · היומן לא נכתב";
        public static final String KEEP_PATH = " · שומר מסלול";
        /** מ0.25: /תפוס:לדלג — כמה דולגו. מתחיל ב־" · " (שלושה תווים) — מי שמציג לבד חותך. */
        public static String skippedTaken(int n) { return " · דולגו " + n + " (שם תפוס)"; }
        /** מ0.40: /תפוס:לשנות-שם — כמה נשארו במקומם כי יש ביעד קובץ זהה להם. מתחיל ב־" · ". */
        public static String sameAtDest(int n) { return " · " + n + " נשארו במקומם (יש ביעד זהה)"; }
        /** מ0.40: העברה אל התיקייה שהם כבר בה. מתחיל ב־" · ". */
        public static String inPlace(int n) { return " · " + n + " כבר במקום"; }
        public static final String CONTAINS_USAGE = "צורה: /מכיל:<טקסט> — רק קבצים שהטקסט כתוב בהם";
        /** 1.20.0 */
        public static final String NO_COMPARE = "אין השוואה (COMP) בשורה קודמת";
        public static final String COMPARE_BROKEN = "שורה קודמת על אותה השוואה נכשלה — לא ממשיכים עליה";
        /** 1.21.0 */
        /** 1.22.0 */
        public static final String LIST_ONLY_COMP = "/רשימה:<שם> — רק ב־COMP, עם שם רשימה";
        public static final String NEEDS_INDEX = "רשימה כמקור צריכה את האינדקס — הוא עוד לא מוכן. לנסות שוב בעוד רגע";
        public static final String LIST_NO_MOVE = "מקור שהוא רשימה — רק COPY (MOVE היה מוציא קבצים מהמקומות שלהם)";

        public static String smartSkipped(int n) {
            return " · ⚠ " + n + " רשימות חכמות שברשימה לא נמצאו (נמחקו?) — לא נכללו";
        }

        public static String listEmpty(String n) {
            return "הרשימה ריקה: " + n + " — ההשוואה נעצרת (רשימה ריקה אינה \"הכול נמחק\")";
        }

        public static String outside(int n) {
            return " · " + n + " קבצים מחוץ לכוננים — לא נכללו";
        }
        public static final String STATE_ONLY_COPY = "/מצב: — רק ב־COPY (שחזור הוא העתקה; הגיבוי לא משתנה)";
        public static final String STATE_LOGICAL = "בשחזור כותבים את המסלול הלוגי (אחרי שורש הגיבוי), בלי נוכחי או היסטוריה";

        public static String notBackup(String p) {
            return "אינו בתוך גיבוי (אין נוכחי ויומן או היסטוריה): " + p;
        }

        public static String restore(String what, String when, String dest) {
            return "שחזור המצב של " + when + ": " + what + " ← " + dest + " · שומר מסלול";
        }
        public static final String SETS_ONLY_SOURCE = "%רק-במקור% ו־%רק-ביעד% — רק לבדם, כמקור של COPY או MOVE";

        public static String compareNoSource(String p) {
            return "המקור לא נמצא: " + p + " — ההשוואה נעצרת (מקור חסר אינו \"ריק\")";
        }

        public static String compareNoDest(String p) {
            return "היעד לא נמצא: " + p + " — אפשר ליצור אותו קודם ב־MD";
        }

        public static String compared(String a, String b, int onlyA, int onlyB, int same) {
            return "השוואה: " + a + " מול " + b + " — רק במקור " + onlyA + " · רק ביעד " + onlyB + " · זהים " + same;
        }

        public static String overLimit(int n, int total, int pct) {
            return "יוצאים " + n + " מתוך " + total + " קבצים — יותר מ־" + pct + "%. נעצר (אולי המקור לא מחובר?)";
        }

        public static String emptyFolders(int n) {
            return n == 1 ? "תיקייה ריקה אחת" : n + " תיקיות ריקות";
        }
        public static final String REN_WILD = "REN עם * — לא נתמך עדיין";
        /** 1.22.5: עד כאן — "לא נמצא", כאילו אין כזה. */
        public static final String REN_FOLDER = "REN — רק לקובץ; שינוי שם לתיקייה — עדיין לא בפקודות (בתפריט: שינוי שם בכונן)";

        public static String unknownVar(String name) {
            return "המשתנה %" + name + "% לא מוכר";
        }

        public static String switchUnknown(String sw) {
            return "המתג " + sw + " לא הובן";
        }
        /** מ0.40 (ממצא 41): מתג מוכר, במקום שבו הוא לא עושה כלום. cmd — הפקודה, או null — שורה עם רשימה. */
        public static String switchNotHere(String sw, String cmd) {
            return "המתג " + sw + " לא שייך " + (cmd == null ? "לשורה הזו" : "לפקודה \"" + cmd + "\"") + " — הוא לא היה עושה כלום";
        }
        public static final String LIMIT_ONLY_COMPARE = "/עד:N% — רק אחרי \"השווה\", בשורה שהמקור שלה %רק-במקור% או %רק-ביעד%",
                NO_FILTER_ON_COMPARE = "סינון (/תאריך, /גודל, /מכיל) לא חל על מה שההשוואה מצאה (%רק-במקור% / %רק-ביעד%)";

        public static String unknownCommand(String cmd) {
            return "הפקודה " + cmd + " לא מוכרת";
        }

        public static String exists(String name) {
            return "כבר קיים: " + name;
        }

        public static String alreadyHas(String name) {
            return "כבר יש \"" + name + "\"";
        }

        public static String notFound(String src) {
            return "לא נמצא: " + src;
        }

        public static String filterOnFolder(String example) {
            return "הסינון (תאריך או גודל) חל על קבצים — לא על תיקייה שלמה. אפשר: " + example;
        }

        // התיאור בתצוגה המקדימה
        public static String echo(String text) {
            return "הודעה: " + text;
        }

        public static String newFolder(String path) {
            return "תיקייה חדשה: " + path;
        }

        public static String toTrash(String what) {
            return "לסל המחזור: " + what;
        }

        public static String folderToTrash(String path) {
            return toTrash("התיקייה " + path);
        }

        public static String transfer(boolean copy, String what, String dest) {
            return (copy ? "העתקה: " : "העברה: ") + what + " ← " + dest;
        }

        /** 203: COPY … %שיתוף% */
        /** 216: תפריט ⭐ — נגיעה מריצה, נגיעה ארוכה עורכת; ובסוף הרצה שהלכה חלק — הודעה קצרה. */

        public static String doneShort(int lines) {
            return "✓ בוצע" + (lines > 1 ? " (" + lines + " שורות)" : "");
        }

        public static String folderExists(String name) {
            return "התיקייה \"" + name + "\" כבר קיימת — בלי שינוי";
        }

        public static String toShare(String what) {
            return "לשיתוף: " + what + " — יישלחו בסוף, בשליחה אחת (תיקייה — הקבצים שבה כמו שהם)";
        }

        public static final String SHARE_NO_MOVE = "MOVE לשיתוף — לא: אין דרך לדעת שהקבצים הגיעו, ואסור למחוק כאן. "
                + "COPY לשיתוף, ואחר כך — אם צריך — DEL",
                SHARE_ONLY_COPY = "%שיתוף% — רק כיעד של COPY",
                SHARE_NO_PATH = "אחרי %שיתוף% — בלי מסלול",
                HOME_ONLY_START = "%בית% — רק לבדו, אחרי START (למשל: START %בית%)",
                HOME_AT_END = "בסוף — חזרה למסך הבית",
                TO_HOME = "🏠 למסך הבית",
                STAGED = "הוכנו לשליחה";

        /** 203: בסוף ההרצה, לפני חלון השליחה. */
        public static String sending(int n) {
            return "נשלחים " + n + (n == 1 ? " פריט" : " פריטים") + " — לבחור את המכשיר";
        }

        /** "3 קבצים ותיקייה אחת". */
        public static final String AND = " ו";

        public static String rename(String from, String to) {
            return "שינוי שם: " + from + " ← " + to;
        }

        public static String openAtEnd(String path) {
            return "פתיחה בסוף: " + path;
        }

        public static String filtered(int passed, int total) {
            return " (" + passed + " מתוך " + total + " עברו את הסינון)";
        }

        // הדוח
        public static final String NO_FILES = "אין קבצים", TRASHED = "הועברו לסל", COPIED = "הועתקו",
                MOVED = "הועברו", RENAMED = "שונה השם", NOT_DONE = "לא בוצעה", DONE = "בוצע.";

        public static String stoppedBefore(int line) {
            return "נעצר לפני שורה " + line;
        }

        /** 1.25.0: "שורה 12 (סבב 3): …". */
        public static String line(String label, String out) {
            return "שורה " + label + ": " + out;
        }

        public static String skipped(String error) {
            return "דולגה — " + error;
        }

        public static String created(String name) {
            return "נוצרה " + name;
        }

        public static String failed(int n) {
            return " · נכשלו " + n;
        }

        // התפריט והחלונות
        public static final String NOTHING_TO_DO = "אין בקובץ פקודות לביצוע.",
                NO_CHANGE = "לא ישתנה דבר בכונן";

        public static String preview(String file) {
            return "📜 " + file + " — תצוגה מקדימה";
        }

        public static String done(String file) {
            return "📜 " + file + " — בוצע";
        }

        public static String running(String file) {
            return "מריץ את " + file + "…";
        }

        /** 1.25.4: ההכנה (תכנון והדמיה) — לפני התצוגה המקדימה. */
        public static String preparing(String file) {
            return "מכין את " + file + "… (בודק כל שורה לפני ההרצה)";
        }

        public static final String PLAN_FAILED = "תקלה בהכנת קובץ הפקודות", STOPPED_PREPARING = "נעצר — שום דבר לא בוצע",
                RUN_FAILED = "תקלה בהרצה: ";

        public static String openFolder(String name) {
            return "📂 פתיחת " + name;
        }

        /** 186: START על קובץ. */
        public static String openFile(String name) {
            return "▶ פתיחת " + name;
        }

        public static String newestOf(int n) {
            return " (החדש מבין " + n + ")";
        }

        public static String noMatch(String pattern) {
            return "אין קובץ שמתאים ל־" + pattern;
        }

        // העורך (1.23.0: הטופס הישן יצא — הטקסטים של הממשק החדש ב-Words.Ed)
        public static final String PICK = "📁 בחירה";

        /** ההסבר בראש קובץ פקודות חדש. התחביר (פקודות, משתנים, מתגים, דוגמה) — מ-Batch. */
        public static String template(String commands, String vars, String filters, String ask, String skip, String example) {
            return "@ECHO OFF\n"
                    + "REM קובץ פקודות של מנהל קבצים וירטואלי — כמו קובץ bat.\n"
                    + "REM פקודות: " + commands + "\n"
                    + "REM מסלולים: " + vars + "   (או מסלול מלא, או יחסי לתיקייה של הקובץ)\n"
                    + "REM לשיתוף: COPY <מה> %שיתוף% — נשלח בסוף למכשיר אחר\n"
                    + "REM סינון: " + filters + "   ·   ROBOCOPY: /MAXAGE:1  /MINAGE:365  /MAX:  /MIN:\n"
                    + "REM שם תפוס: /Y להחליף (הקודם לסל) · /-Y או " + ask + " · " + skip + "\n"
                    + "REM שמירת מסלול: /S או /מסלול — כל מה שבתיקייה, גם בתיקיות שבתוכה, באותו מסלול ביעד\n"
                    + "REM יומן: /יומן:<קובץ> — כל קובץ שהגיע ליעד נרשם בקובץ, שורה אחרי שורה\n"
                    + "REM השוואה: COMP <מקור> <יעד>, ואחריה COPY %רק-במקור% <יעד> · MOVE %רק-ביעד% <יעד> /עד:20%\n"
                    + "REM שחזור: COPY <גיבוי>[\\תת־תיקייה] <יעד> /מצב:\"26.10.2026 14:30\" — מה שהיה באותו זמן\n"
                    + "REM לפני כל הרצה — תצוגה מקדימה ואישור. DEL מעביר לסל המחזור.\n"
                    + "REM בכאילו — שורה לבדה: הקובץ רץ בהדמיה בלבד ומראה מה היה קורה, בלי לשנות דבר.\n"
                    + "REM          כדי להריץ באמת — למחוק את השורה.\n"
                    + "REM בסוף — חזרה למסך הבית: START %בית%\n"
                    + "\n"
                    + "REM דוגמה:\n"
                    + "REM " + example + "\n";
        }
    }

    // ---------- אופני עבודה (105) ----------
    public static final class WorkModes {
        private WorkModes() { }

        public static final String EXISTS = "כבר יש אופן עבודה בשם הזה";
    }

    // ---------- אריזה ושליחה (116, 117) ----------
    /** 182: תקופת הניסיון של סדר הפעולות של האינדקס. */
    /** 190: עבודה ברקע — חתימות התוכן, ואיתור החסרים (Sig, SigUi) */
    public static final class Bg {
        private Bg() { }

        public static final String TITLE = "עבודה ברקע";
        public static final String RELOCATE_BUTTON = "איתור כל החסרים ברשימות";
        public static final String FAST = "זרז";
        public static final String FASTING = "מזרז…";
        public static final String INDEX_NOT_READY = "האינדקס עוד נטען — נסה בעוד רגע";
        public static final String SEARCHING = "מאתר את החסרים…";
        public static final String NO_MISSING = "אין פריטים חסרים ברשימות";
        public static final String RELOCATE_TITLE = "איתור החסרים ברשימות";
        public static final String CONTINUE = "המשך";
        public static final String UPDATE = "לעדכן";
        public static final String WHICH = "מה לעדכן?";

        private static String pct(int done, int total) {
            return total <= 0 ? "100%" : (100L * done / total) + "%";
        }

        /** על הכפתור בהגדרות. */
        public static String line(int idxDone, int idxTotal, int listDone, int listTotal, boolean running, int added) {
            return "עבודה ברקע: חתימות " + (idxTotal < 0 ? "—" : pct(idxDone, idxTotal))
                    + (listTotal > 0 ? " (ברשימות " + pct(listDone, listTotal) + ")" : "") + (running ? " · רץ" : "")
                    + (added >= 0 ? " · נוסף " + added + "%" : "");
        }

        public static String detail(int idxDone, int idxTotal, int listDone, int listTotal, boolean running, int added) {
            boolean all = idxDone >= idxTotal && listDone >= listTotal;
            return "חתימות תוכן — כל הקבצים: " + Files.count(Math.max(0, idxDone)) + " מתוך " + Files.count(Math.max(0, idxTotal))
                    + " (" + pct(idxDone, idxTotal) + ")"
                    + "\nמהם ברשימות הידניות: " + Files.count(listDone) + " מתוך " + Files.count(listTotal) + " (" + pct(listDone, listTotal) + ")"
                    + (all ? "\n— הכול מוכן." : running ? "\n— רץ עכשיו, במנות קטנות (קודם הרשימות)." : "\n— ימשיך בפתיחה הבאה.")
                    + "\n\nהחתימה מזהה קובץ לפי התוכן — לאיתור קבצים שזזו (גם בשם אחר), למעבר בין מכשירים, "
                    + "ובהמשך לכפולים ולגילוי שיבוש. היא נחוצה לפני שהקובץ זז, ולכן מחושבת מראש."
                    + (added >= 0 ? "\n\nתאריך ההוספה ידוע ל־" + added + "% מהקבצים (השאר — מוסתרים, או שהיו לפני שהתוכנה הכירה אותם)." : "");
        }

        public static String progress(int done, int total, boolean running) {
            return "חתימות: " + Files.count(Math.max(0, done)) + " מתוך " + Files.count(Math.max(0, total))
                    + " (" + pct(done, total) + ")"
                    + (total > done ? "\nנשארו לחשב: " + Files.count(total - done) + " (מה שכבר חושב — לא מחושב שוב)" : "")
                    + (running ? "…" : "\n— הסתיים.");
        }

        public static String summary(int missing, int verified, int nameSize, int several, int differ, int notFound, int sigPercent) {
            StringBuilder sb = new StringBuilder("חסרים ברשימות: " + Files.count(missing));
            if (verified > 0) sb.append("\n✓ אומתו לפי התוכן (חתימה): ").append(Files.count(verified)).append(" — גם אם השם שונה");
            if (nameSize > 0) sb.append("\n≈ לפי שם וגודל בלבד: ").append(Files.count(nameSize)).append(" — אין להם חתימה")
                    .append(several > 0 ? " (" + several + " ביותר ממקום אחד — נבחר הדומה במסלול)" : "");
            if (differ > 0) sb.append("\n✗ באותו שם וגודל, אבל התוכן שונה: ").append(Files.count(differ)).append(" — לא יעודכנו");
            if (notFound > 0) sb.append("\n✗ לא נמצאו: ").append(Files.count(notFound));
            if (sigPercent < 100) sb.append("\n\nחתימות מוכנות ל־").append(sigPercent).append("% מהקבצים שברשימות.");
            return sb.toString();
        }

        public static String allOf(int n) {
            return "הכול (" + Files.count(n) + ") — גם לפי שם וגודל";
        }

        public static String onlyVerified(int n) {
            return "רק מה שאומת לפי התוכן (" + Files.count(n) + ")";
        }
    }

    public static final class Trial {
        private Trial() { }

        public static final String DAILY = "יומית", WEEKLY = "שבועית", SINCE = "מה השתנה מאז",
                FILE = "תקופת ניסיון.txt",                                         // בכונן
                DAILY_ABOUT = "בדיקה מלאה של מה שהמאגר של אנדרואיד לא מכיר (מוסתרים, .nomedia)";

        public static String found(int n) {
            return "נמצאו " + n;
        }
    }

    /** 186: התקנת קובץ apk מתוך התוכנה. */
    public static final class Install {
        private Install() { }

        public static final String ALLOW_FIRST = "בפעם הראשונה: לאשר ל\"מנהל קבצים וירטואלי\" להתקין תוכנות, ולחזור. אחר כך — שוב.",
                NO_SETTINGS = "לא נפתח מסך ההרשאה. אפשר לאשר בהגדרות אנדרואיד: אפליקציות ← גישה מיוחדת ← התקנת אפליקציות לא מוכרות",
                FAILED = "ההתקנה לא נפתחה. הפרטים ביומן";
        public static final String MIME = "application/vnd.android.package-archive";       // תחביר
    }

    /** 181: כפתורי תצוגה מהירה. */
    public static final class Quick {
        private Quick() { }

        public static final String APP = "בלי תיקיית התוכנה", NO_INI = "בלי ini", BOOKS = "רק ספרים",
                MONTH = "רק מהחודש", ADD = "➕", TITLE_NEW = "כפתור חדש", TITLE_EDIT = "עריכת כפתור",
                KIND_FILTER = "🔎 כפתור סינון (מה להציג)", KIND_RUN = "▶ כפתור הרצה — קובץ פקודות",   // 216
                NO_BATCH = "עוד אין קובצי פקודות (⭐ ← ➕ קובץ פקודות חדש)",
                NAME = "שם הכפתור", ADD_COND = "➕ תנאי", NO_CONDS = "אין עדיין תנאים — ➕ תנאי",
                WHAT = "מה בודקים?", KIND = "סוג קובץ", FOLDER = "תיקייה או כונן", WORD = "מילה בשם",
                SIZE = "גודל", DATE = "תאריך (השינוי האחרון)", MUST = "✓ חייב", NOT = "✕ בלי",
                FLIP = "הפיכה (חייב ↔ בלי)", REMOVE = "מחיקת התנאי", EDIT = "עריכה", RIGHT = "הזזה ימינה",
                LEFT = "הזזה שמאלה", DELETE = "מחיקת הכפתור", WORD_HINT = "מילה שבשם הקובץ",
                NEED_COND = "צריך לפחות תנאי אחד";
        public static final String[] SIZES = {">1MB", ">10MB", ">100MB", "<100KB"},                  // תחביר
                DATES = {"יום", "שבוע", "חודש", "שנה"};                                                 // תחביר

        /** "יישארו 1,203" — לפני שבוחרים. */
        public static String leaves(String n) {
            return "יישארו " + n;
        }

        /** "בחיפוש הנוכחי: 4,560 ← 1,203". */
        public static String preview(String before, String after) {
            return "בחיפוש הנוכחי: " + before + " ← " + after;
        }

        public static String files(String what, String n) {
            return what + " · " + n;
        }
    }

    public static final class Send {
        private Send() { }

        public static final String PACK_FAILED = "האריזה נכשלה", UNPACK_FAILED = "הפריסה נכשלה — ייתכן שהקובץ פגום",
                CANT_SAVE = "לא הצלחתי לשמור את הקובץ", UNPACK_DIR = "פריסה",
                NO_TRANSFER_APP = "לא נמצאה תוכנת העברה למכשיר אחר. נפתח חלון השיתוף המלא",
                ALL_APPS = "כל התוכנות…", TO_DEVICE = "שליחה למכשיר", SHARE = "שיתוף",
                SEND_FAILED = "השליחה נכשלה", SHARE_FAILED = "השיתוף נכשל",
                FIXED = "שליחה למכשיר — יעד קבוע", NO_FIXED = "בלי — לבחור בכל פעם";
    }

    /** פעולות בכונן (Physical): מחיקה, העברה, העתקה, שחזור, שינוי שם, פריסה, יצירה */
    public static final class Disk {
        private Disk() { }

        public static final String DRIVE_ROOT = "זה שורש הכונן";
        public static final String IS_TRASH = "זה סל המחזור";
        public static final String IS_APP_FOLDER = "זו תיקיית התוכנה";
        public static final String IS_APP_FILE = "זה קובץ של התוכנה עצמה";
        public static final String NOT_IN_INDEX_REFRESH = "הקובץ לא נמצא באינדקס. כדאי לרענן.";
        public static final String FILE_GONE_REFRESH = "הקובץ כבר לא נמצא. כדאי לרענן.";
        public static final String FILE_CHANGED_REFRESH = "הקובץ השתנה מאז הסריקה האחרונה. כדאי לרענן ולנסות שוב.";
        public static final String FOLDER_GONE_REFRESH = "התיקייה כבר לא נמצאת. כדאי לרענן.";
        public static final String DRIVE_NOT_IN_INDEX = "הכונן לא נמצא באינדקס";
        public static final String DEST_IS_APP = "היעד הוא תיקיית התוכנה";
        public static final String DEST_IS_TRASH = "היעד הוא סל המחזור";
        public static final String NO_ROOM_NEED = "אין מספיק מקום ביעד (צריך ";
        public static final String NO_ROOM_FREE = ", פנוי ";
        public static final String NOT_FOUND_REFRESH = "לא נמצא. כדאי לרענן.";
        public static final String NOT_INTO_ITSELF = "אי אפשר לתוך התיקייה עצמה";
        public static final String ALREADY_HERE = "כבר בתיקייה הזו";
        public static final String CANT_REPLACE_PARENT = "אי אפשר להחליף תיקייה שמכילה את המקור";
        public static final String NO_DEST_FOLDER = "לא נוצרה תיקייה ביעד";
        public static final String NOT_UNDER_ROOT = "הפריט אינו בתוך תיקיית המקור";
        public static final String TOO_BIG_FOR_DEST = "הקובץ גדול מ־4GB, והיעד (FAT32) אינו מקבל קובץ כזה";
        public static final String NO_ORIGINAL_PLACE = "אין מקום מקורי ידוע";
        public static final String FILES_SUFFIX = " קבצים";
        public static final String ONE_FILE_SUFFIX = " קובץ אחד";
        public static final String FEM_SUFFIX = "ה ";
        public static final String IN_LISTS_SUFFIX = " ברשימות";
        public static final String NOT_SUFFIX = " לא: ";
        public static final String STOPPED_MIDWAY = " · בוטל באמצע";
        public static final String AND = " ו";
        public static final String CANT_DELETE = "אי אפשר למחוק את \"";
        public static final String FILES_NOT_IN_INDEX = "הקבצים לא נמצאו באינדקס";
        public static final String LISTS_MARK_DELETED = " · ברשימות יסומן \"נמחק\"";
        public static final String LISTS_REMOVE_FROM = " · יוסר מ־";
        public static final String FILE_TO_TRASH = "הקובץ יועבר ל\".נמחקו\"";
        public static final String FILES_TO_TRASH = "הקבצים יועברו ל\".נמחקו\"";
        public static final String FOLDER_AND = "התיקייה ו־";
        public static final String ITS_FILE = "הקובץ שבה";
        public static final String ITS_FILES = " הקבצים שבה";
        public static final String GO_TO_TRASH = " יועברו ל\".נמחקו\"";
        public static final String ALL_INCLUDING = "הכול, כולל ";
        public static final String IN_FOLDERS_TO_TRASH = " שבתיקיות, יועבר ל\".נמחקו\"";
        public static final String UNDO_FROM_TRASH = "אפשר לשחזר מסל המחזור";
        public static final String UNDO_FOLDER_FROM_TRASH = "אפשר לשחזר מסל המחזור, תיקייה בשלמותה";
        public static final String DELETE_TITLE = "מחיקה מהכונן: ";
        /** 102 */
        public static final String SPACE_ON_EMPTY = " — המקום יתפנה רק בריקון הסל";
        public static final String DELETE_ON_DRIVE = "מחיקה מהכונן";
        public static final String DELETED = "נמחקו";
        public static final String REMOVED = "הוסרו";
        public static final String CANT_MOVE = "אי אפשר להעביר את \"";
        public static final String DEST_APP_NOT = "אי אפשר לבחור את תיקיית התוכנה כיעד";
        public static final String DEST_TRASH_NOT = "אי אפשר לבחור את סל המחזור כיעד";
        public static final String CANT = "אי אפשר ";
        public static final String TO_COPY = "להעתיק";
        public static final String TO_MOVE = "להעביר";
        public static final String FOLDER_INTO_ITSELF = " תיקייה לתוך עצמה";
        public static final String NO_ROOM_IN = "אין מספיק מקום ב\"";
        public static final String NEED = "צריך ";
        public static final String FREE = " · פנוי ";
        public static final String GOT_IT = "הבנתי";
        public static final String SOURCE_UNCHANGED = "המקור לא משתנה";
        public static final String FILE_MOVES = "הקובץ יזוז";
        public static final String FOLDER_MOVES = "התיקייה תזוז";
        public static final String ALL_MOVES = "הכול יזוז";
        public static final String ON_DRIVE_TO = " בכונן אל ";
        public static final String FILES_IN_FOLDERS = " קבצים בתיקיות";
        public static final String ACROSS_DRIVES = "\nבין כוננים: קובץ אחרי קובץ — יועתק, ייבדק, ורק אז יימחק מהמקור";
        public static final String COPIES_NOT_IN_LISTS = "העותקים לא נכנסים לרשימות · שם תפוס — אשאל";
        public static final String LISTS_FOLLOW_OPEN = "הרשימות יעקבו (";
        public static final String LISTS_FOLLOW = "הרשימות יעקבו";
        public static final String TAKEN_ASK = " · שם תפוס — אשאל";
        public static final String COPY_TO = "העתקה ל\"";
        public static final String MOVE_TO = "העברה ל\"";
        public static final String COPYING = "מעתיק…";
        public static final String MOVING = "מעביר…";
        public static final String COPIED = "הועתקו";
        public static final String MOVED = "הועברו";
        public static final String UPDATED = "עודכנו";
        public static final String ALREADY = "\" כבר ";
        public static final String EXISTS_FEM = "קיימת";
        public static final String EXISTS = "קיים";
        public static final String IN_QUOTE = " ב\"";
        public static final String EXISTING = "הקיים: ";
        public static final String INCOMING = "\nהחדש: ";
        public static final String OTHER_NAME_LABEL = "שם אחר:";
        public static final String SAME_FOR_REST = "אותו דבר לכל השאר";
        public static final String MERGE = "מזג עם הקיימת";
        public static final String DELETE_PREVIOUS_FEM = "מחק את הקודמת";
        public static final String OTHER_NAME = "שם אחר";
        public static final String CANCEL_SHORT = "בטל";
        public static final String DELETE_PREVIOUS = "מחק את הקודם";
        public static final String FOLDER_DOT = "תיקייה · ";
        public static final String FILES_DOT = " קבצים · ";
        public static final String STOPPING = "עוצר אחרי הקובץ הנוכחי…";
        public static final String OF = " מתוך ";
        public static final String RESTORE_TITLE = "שחזור ";
        public static final String RESTORE_FILES_BACK = "הקבצים יחזרו למקום המקורי בכונן";
        public static final String UNKNOWN_STAY = " בלי מקום מקורי ידוע יישארו בסל";
        public static final String TAKEN_RESTORED = "שם תפוס מקבל תוספת \"(משוחזר)\"";
        public static final String RESTORE_NOT_LISTS = "לא מחזיר לרשימות — אפשר להוסיף שוב";
        public static final String RESTORE = "שחזור";
        public static final String RESTORED = "שוחזרו";
        public static final String PURGE_TITLE = "מחיקה לצמיתות: ";
        public static final String PURGE_FILES = "הקבצים יימחקו מהכונן";
        public static final String PURGE_GREY = " · פריטים שנשארו ברשימות באפור יוסרו מהן";
        public static final String NO_UNDO = "אי אפשר לשחזר";
        public static final String PURGE = "מחיקה לצמיתות";
        public static final String BAD_NAME = "שם לא תקין";
        public static final String NO_CHAR = "בשם לא יכול להיות התו ";
        public static final String NAME_TOO_LONG = "השם ארוך מדי";
        public static final String ALREADY_IN_FOLDER = "כבר יש בתיקייה \"";
        public static final String CANT_RENAME = "אי אפשר לשנות את השם של \"";
        public static final String FILE_NAME = "שם הקובץ";
        public static final String IN_FOLDER_LINE = "\nבתיקייה ";
        public static final String EXT_CHANGES = "\nהסיומת משתנה (";
        public static final String WITHOUT = "בלי";
        public static final String EXT_WARNING = "): ייתכן שהקובץ ייפתח בתוכנה אחרת, או לא ייפתח";
        public static final String CAN_RENAME_AGAIN = " · אפשר לשנות את השם שוב";
        public static final String RENAME_TITLE = "שינוי שם בכונן: ";
        public static final String RENAME_FILE_ITSELF = "הקובץ עצמו יקבל שם חדש בכונן";
        public static final String RENAME = "שינוי השם";
        public static final String ERROR = "שגיאה";
        public static final String NOT_RENAMED = "השם לא שונה: ";
        public static final String RENAMED_TO = "השם שונה ל\"";
        public static final String UPDATED_SUFFIX = " · עודכנו ";
        public static final String NO_UNPACK_IN_APP = "אי אפשר לפרוס בתוך תיקיית התוכנה";
        public static final String BAD_ZIP = "זה לא קובץ ZIP תקין, או שהוא פגום";
        public static final String NO_ROOM_UNPACK = "אין מספיק מקום לפריסה";
        public static final String UNPACK_TITLE = "פריסה בכונן: ";
        public static final String UNPACK_CREATES = "הקבצים שבקובץ ייווצרו בכונן, בתיקייה חדשה";
        public static final String TO_LINE = "\nאל ";
        public static final String ZIP_STAYS = "קובץ ה-ZIP נשאר כמו שהוא · אפשר למחוק את התיקייה החדשה";
        public static final String UNPACK = "פריסה";
        public static final String UNPACKING = "פורס…";
        public static final String UNPACKED = "נפרסו ";
        public static final String TO_QUOTE = " אל \"";
        public static final String SKIPPED = " דולגו (קיימים, או שם לא תקין)";
        public static final String STOPPED_SUFFIX = " · נעצר באמצע";
        public static final String NO_FOLDER_IN_APP = "אי אפשר ליצור תיקייה בתוך תיקיית התוכנה";
        public static final String ALREADY_HERE_NAME = "כבר יש כאן \"";
        public static final String NO_FILE_IN_APP = "אי אפשר ליצור קובץ בתוך תיקיית התוכנה";
        public static final String ALREADY_NAME = "כבר יש \"";
        public static final String NEW_FILE_TITLE = "קובץ חדש בכונן: ";
        public static final String WILL_CREATE_FOLDER = "ייווצרו בכונן: התיקייה \"";
        public static final String AND_FILE = "\" והקובץ";
        public static final String NEW_FILE_CREATED = "קובץ חדש ייווצר בכונן";
        public static final String INSIDE = "בתוך ";
        public static final String CAN_DELETE_IT = "אפשר למחוק אותו ממנהל הקבצים";
        public static final String CREATE = "יצירה";
        public static final String CANT_CREATE_FILE = "לא הצלחתי ליצור את הקובץ";
        public static final String NEW_FOLDER_TITLE = "תיקייה חדשה בכונן: ";
        public static final String NEW_EMPTY_FOLDER = "תיקייה חדשה ריקה תיווצר בכונן";
        public static final String CAN_DELETE_IT_FEM = "אפשר למחוק אותה ממנהל הקבצים";
        public static final String FOLDER_CREATED = "נוצרה התיקייה \"";
    }

    /** מסך ההגדרות (SettingsScreen) */
    public static final class Prefs {
        private Prefs() { }

        public static final String FILE_ACCESS = "הרשאת גישה לקבצים";
        public static final String REBUILD = "בניית האינדקס מחדש";
        public static final String DELETED_BUTTON = "קובץ שנמחק — מה קורה ברשימות";
        public static final String EXCLUDED_BUTTON = "תיקיות שלא נכנסות לאינדקס — באופן העבודה הזה";
        public static final String QUICK_REFRESH = "רענון מהיר";
        public static final String EDIT_INI = "עריכת קבצי ההגדרות";
        public static final String DEEP_REFRESH = "רענון מעמיק — גודל ותאריך של כל הקבצים";
        public static final String REBUILD_WHAT = "סריקה מלאה של כל הכוננים. בכרטיס זה עשוי לקחת זמן";
        public static final String REBUILD_UNDO = "בינתיים החיפוש ממשיך לעבוד על האינדקס הקיים";
        public static final String BUILD = "בנייה";
        public static final String NOW = "כעת: ";
        public static final String GREY_EXAMPLE = " (\"נמחק · בסל המחזור\")";
        public static final String EXCLUDED_NONE = "אין — הכול נכנס לאינדקס";
        public static final String EXCLUDED_ABOUT = "תיקיות שיוצאו מהאינדקס (למשל מטמון). ייכנס לתוקף בבנייה מחדש.\nלכל אופן עבודה אינדקס משלו — וההגדרה הזו שלו בלבד (באופן אחר — רשימה אחרת; ב\"דוגמאות\" — מתאפסת בחזרה לברירת מחדל).";
        public static final String EXCLUDED_TITLE = "לא לאנדקס";
        public static final String EXCLUDED_SAVED = "נשמר · ייכנס לתוקף בבניית האינדקס מחדש";
        public static final String LEVEL_ABOUT = "\nעדכון האינדקס (לשינויים שנעשו מחוץ לתוכנה; מה שהתוכנה עושה — מתעדכן מיד):";
        public static final String RECOMMENDED = " (מומלץ)";
        public static final String PERIODIC_ABOUT = "רענון גם כל כמה דקות כשהתוכנה פתוחה (למשל בזמן העתקה מהמחשב בכבל):";
        public static final String OFF = "כבוי";
        public static final String EVERY_MINUTE = "כל דקה";
        public static final String EVERY_5 = "כל 5 דקות";
        public static final String EVERY_15 = "כל 15 דקות";
        public static final String DELETED_IS = "קובץ שנמחק, ברשימות: ";
        public static final String ACCESS_IS = "גישה לקבצים: ";
        public static final String INDEX_LOADING = "האינדקס נטען…";
        public static final String IN_INDEX = "באינדקס: ";
        public static final String FILES_DOT = " קבצים · ";
        public static final String FOLDERS_LINE = " תיקיות\n";
        public static final String UPDATED_AT = "עודכן: ";
        public static final String FILES_LINE = " קבצים\n";
        public static final String SCANNING = "סורק את ";
        public static final String INDEX_BUILT = "האינדקס נבנה";
        public static final String CHANGES_DOT = " שינויים · ";
        public static final String MS = " מ\"ש";
        public static final String DEEP_RUNNING = "רענון מעמיק… (כמה שניות)";
        public static final String SECONDS = " שניות";
    }

    /** המסך הראשי (Home): אופני עבודה, כלים, הוספה */
    public static final class Main {
        private Main() { }

        public static final String APP_NAME = "מנהל קבצים וירטואלי";
        public static final String GRANT = "מתן הרשאה";
        public static final String SETTINGS = "הגדרות";
        public static final String ADD = "הוספה";
        public static final String WHAT_TO_ADD = "מה להוסיף";
        /** 1.13.1: ➕ בבית — ארבע שורות (הוכרע 16.10). */
        public static final String ADD_MANUAL = "📋  רשימה ידנית", ADD_SMART = "✨  רשימה חכמה",   // 1.16.1: אותו תפריט
                ADD_CMD = "▶  פקודה";                                                                // גם בכפתור
    }

    /** הרשימה הידנית — מסך הרשימה (DbListScreen) ופעולות הבחירה בו; עד מ0.29 — VirtualScreen */
    public static final class Manual {

        public static final String DESC = "תיאור";
        public static final String DESC_HINT = "למשל: מה יש כאן ולמה";
        private Manual() { }

        public static final String ORDER_DOTS = "↕ הזזה…", ORDER_SHORT = "הזזה";

        public static final String LISTS_DOT = " רשימות · ";
        public static final String ITEMS_SUFFIX = " פריטים";

        public static final String LISTS_SUFFIX = " רשימות";
        public static final String BACKUP_BEFORE_CHANGE = "נשמר גיבוי לפני השינוי, בתיקייה .גיבויים";
        public static final String PATH_UPDATED_IN = "המסלול עודכן ב-";
        public static final String NOTHING_TO_UPDATE = "לא נמצא פריט לעדכון";
    }

    /** הכפולים (DupScreen) */
    public static final class Dup {
        private Dup() { }

        public static final String NAME_SIZE = "שם וגודל";
        public static final String DUPLICATES = "כפולים";
        public static final String DUPS_IN = "כפולים בתוך: ";
        public static final String A_COLON = "א': ";
        public static final String B_COLON = " · ב': ";
        public static final String A_B_SAME = "\nא' וב' זהים — מוצגים הכפולים בתוך המקום הזה";
        public static final String SAME_BY = "קובץ זהה לפי ";
        public static final String MS = " מ\"ש";
        public static final String MARK_HINT = "\n☐ מסמן לפעולה · נגיעה בשורה פותחת את הקובץ";
        public static final String WHOLE_DRIVE = "כל הכונן";
        public static final String SIZE_SIMILAR = "גודל ושם דומה";
        /** 190: לפי התוכן (החתימה), בכל שם */
        public static final String CONTENT = "תוכן";
        /** 195: השילובים החדשים */
        public static final String NAME_CONTENT = "שם ותוכן";
        public static final String CONTENT_SIMILAR = "תוכן ושם דומה";
        public static String unsigned(int n) {
            return "\n" + Files.count(n) + " קבצים באותו גודל לא נבדקו בתוכן (השתנו מאז הבדיקה, או לא נקראו)";
        }

        public static String signing(int n) {
            return "מחשב חתימות ל־" + Files.count(n) + " קבצים באותו גודל…";
        }
        public static final String SIZE = "גודל";
        public static final String NAME = "שם";
        public static final String FILES_CHECKED = " קבצים נבדקו · ";
        public static final String FILES_WITH_COPIES = " קבצים עם עותקים (";
        public static final String COPIES_FREE = " עותקים) · אפשר לפנות ";
        public static final String A = "א'";
        public static final String B = "ב'";
        public static final String IN_A = " בא' · ";
        public static final String IN_B_BOTH = " בב' · בשניהם ";
        public static final String FREE_FROM_A = " שאפשר לפנות מא')";
        public static final String IN_BOTH = "בשניהם (";
        public static final String ONLY_A = "רק בא' (";
        public static final String ONLY_B = "רק בב' (";
        public static final String SAME_HEADER = "✓ זהים — שם וגודל (";
        public static final String DOUBT_HEADER = "≈ בספק — גודל זהה ושם דומה (";
        public static final String NONE_IN_BOTH = "אין קבצים שנמצאים בשני המקומות";
        public static final String NO_DUPS = "לא נמצאו כפולים";
        public static final String ALL_A_IN_B = "כל הקבצים בא' נמצאים גם בב'";
        public static final String ALL_B_IN_A = "כל הקבצים בב' נמצאים גם בא'";
        public static final String LESS = "\n▴ פחות";
        public static final String DETAILS = "  ▾ פרטים";
        public static final String ONE_COPY_B = "עותק אחד בב'";
        public static final String COPIES_B = " עותקים בב'";
        public static final String COPIES = " עותקים";
        public static final String NEW = " · חדש";
        public static final String IN_LIST = " · 📋 מופיע ברשימה: ";
        public static final String SELECTED = "נבחרו ";
        public static final String ALL_COPIES_OF = "כל העותקים של ";
        public static final String GO_SAME_PLACE = " יעברו לאותו מקום";
        public static final String WARN_ALL_COPIES = "⚠ מכל העותקים של ";
        public static final String NONE_LEFT = " לא יישאר אף אחד";
        public static final String PASTE_HINT = " — מנווטים לתיקייה ולוחצים \"הדבקה כאן\"";
        public static final String SORT_MENU = "מיון…";
        public static final String RECOMPUTE = "רענון (חישוב מחדש)";
    }

    /** מסך החיפוש, הרשימה החכמה ומנהל הקבצים (SearchScreen) */
    public static final class Look {
        private Look() { }

        public static final String SEARCH = "חיפוש";
        /** 189: בשורה, כשממוין לפי תאריך ההוספה — התאריך שאחריו הוא של ההוספה. */
        public static final String ADDED = "נוסף";
        public static final String ADDED_UNKNOWN = "נוסף: לא ידוע";
        public static final String REFRESHING = "מרענן את האינדקס…";
        public static final String INDEX_UP_TO_DATE = "האינדקס מעודכן";
        public static final String FOLDERS_REFRESHED = " תיקיות רועננו";
        public static final String FILES_IN_INDEX = " קבצים באינדקס · עודכן ";
        public static final String STILL_LOADING = " · האינדקס עדיין נטען";
        public static final String MS_FORMAT = " · %.1f מ\"ש";
        public static final String LONG_PRESS_HINT = " · נגיעה ארוכה לבחירה מרובה";
        public static final String SETTINGS_TITLE = "הגדרות החיפוש";
        public static final String CURRENT_SETTINGS = "הגדרות החיפוש הנוכחי";
        public static final String IN_PATH_TOO = "לחפש גם במסלול, לא רק בשם";
        public static final String LOOSE_SPELLING = "הרחבת כתיב חסר (היגיון = הגיון)";
    }

    /** עורך קובצי ההגדרות (IniEditScreen) */
    public static final class IniEd {
        private IniEd() { }

        public static final String READ_ONLY_SUFFIX = "  ·  לקריאה בלבד";
        public static final String ADD_LINE = "➕ שורה";
        public static final String NO_SECTION = "ללא מקטע";
        public static final String UNKNOWN_KEPT = " שורות שאינן מוכרות יישמרו כמות שהן";
        public static final String CHANGED_OUTSIDE = "הקובץ השתנה מבחוץ";
        public static final String RELOAD = "טעינה מחדש";
        public static final String OVERWRITE = "דריסה";
        public static final String SAVE_FAILED_LOG = "השמירה נכשלה. הפרטים ביומן";
        public static final String READ_ONLY_COPY = "עותק לקריאה בלבד";
        public static final String KEY = "שם";
        public static final String VALUE = "ערך";
        public static final String EDIT_LINE = "עריכת שורה";
        public static final String EDIT = "עריכה";
        public static final String MOVE_UP = "העברה למעלה";
        public static final String MOVE_DOWN = "העברה למטה";
        public static final String DELETE_LINE = "מחיקת השורה";
        public static final String WHICH_SECTION = "לאיזה מקטע להוסיף";
        public static final String NEW_NAME = "שם חדש";
        public static final String EDIT_BY_HAND = "עריכה ידנית של הקובץ";
    }

    /** שיתוף אל התוכנה (ShareScreen) */
    public static final class ShareIn {

        private ShareIn() { }

        public static final String NEED_APP_FIRST = "צריך קודם לפתוח את התוכנה ולתת הרשאת גישה לקבצים.";
        public static final String NO_FILES_RECEIVED = "לא התקבלו קבצים.";
        public static final String IDENTIFYING = "מזהה ";
        public static final String FILES_DOTS = " קבצים…";
        public static final String CANT_LOCATE = "לא ניתן לזהות את מיקום הקבצים ששותפו.\nהפרטים ביומן.";
        public static final String ADD_N = "להוסיף ";
        public static final String FILES_TO_LIST = " קבצים לרשימה · ";
        public static final String NOT_IDENTIFIED = " לא זוהו)";
        public static final String NO_LISTS_YET = "אין עדיין רשימות.\nאפשר ליצור כלי רשימות במסך הבית, ואז לשתף אליה.";
        public static final String PICK_LIST = "📋 בחירת רשימה…";
    }

    /** סל המחזור (TrashScreen) */
    public static final class Bin {
        private Bin() { }

        public static final String TRASH_TITLE = "סל מחזור";
        public static final String TRASH_SHORT = "סל";
        public static final String TRASH_DOT = "סל מחזור · ";
        public static final String DELETED_FEM = "נמחקה";
        public static final String DELETED = "נמחק";
        public static final String NO_ORIGINAL = " · המקום המקורי לא ידוע";
        public static final String FOLDER_IN_TRASH = "תיקייה בסל — נגיעה ארוכה: שחזור בשלמותה, או מחיקה לצמיתות";
        public static final String TRASH_EMPTY = "הסל ריק.";
        public static final String FILES_SUFFIX = " קבצים";
        public static final String TRASH_HINT = "\nנגיעה פותחת · נגיעה ארוכה בוחרת: שחזור למקום המקורי, או מחיקה לצמיתות";
        public static final String EMPTY_TRASH = "ריקון הסל";
    }

    /** הודעות על הסל. עד מ0.29 — המכלים של המנוע הישן בסל (BinBox, נמחק); OLD_ENGINE — פריט כזה שנשאר בסל */
    public static final class Box {
        private Box() { }
        public static final String OLD_ENGINE = "הוסר במנוע הישן — אין שחזור בגרסה הזו";

        public static final String IN_BIN = "אפשר לשחזר מסל המחזור";
    }

    /** רשימת קובצי ההגדרות (IniListScreen) */
    public static final class IniList {
        private IniList() { }

        public static final String TITLE = "קבצי ההגדרות";
        public static final String ABOUT = "נגיעה פותחת עורך. הקובץ הראשי הוא המקור; העותקים (קובץ לכל כלי) נבנים ממנו.";
        public static final String MISSING_TAP_CREATES = "לא קיים — נגיעה תיצור אותו";
    }

    /** הפס הצף (BarService) */
    public static final class Bar {
        private Bar() { }

        public static final String BAR_CHANNEL = "פס חיפוש";
        public static final String BAR_ACTIVE = "פס החיפוש פעיל";
        public static final String SEARCH_IN = "חיפוש ב-";
        public static final String FILES_DOTS = " קבצים…";
        public static final String LOADING_SHORT = "טוען אינדקס…";
        public static final String LOADING_PARTIAL = "טוען את האינדקס… התוצאות עוד לא מלאות";
        public static final String RESULTS_FORMAT = "%s תוצאות · %.1f מ\"ש";
    }

    /** הליבה (Library): סוגי ההתאמה בחיפוש, מצב הסריקה */
    public static final class Lib {
        private Lib() { }

        /** 187: למה נבנה האינדקס — ביומן. */
        public static final String WHY_REQUEST = "בבקשה", WHY_WEEKLY = "שבועית", WHY_DAILY_FULL = "יומית, אנדרואיד ישן",
                WHY_FIRST = "התחלה — לא היה אינדקס", WHY_NEW_DRIVE = "כונן חדש";

        public static String changes(int n) {
            return n == 1 ? "שינוי אחד" : n + " שינויים";
        }

        public static final String MATCH_FULL = "מלאות";
        public static final String MATCH_NO_PREFIX = "בלי אות השימוש";
        public static final String MATCH_LOOSE = "בכתיב חסר";
        public static final String MATCH_PARTIAL = "חלק מהמילים";
        public static final String MATCH_PARTIAL_HEADER = "מכיל רק חלק מהמילים";
        public static final String SCANNING = "סורק את ";
        public static final String REFRESHING = "מרענן…";
        public static final String SECONDS_FORMAT = "%.1f שניות";
        public static final String READERS_PARALLEL = " קוראים לכל כונן, הכוננים במקביל";
        public static final String SCANNING_ALL = "סורק את כל הכוננים יחד…";
        public static final String NOT_UNDERSTOOD = "לא הובן: ";
    }

    /** מרשם הפעולות (Actions): שמות הפעולות בפס ובתפריט */
    public static final class Act {
        private Act() { }

        public static final String OPEN = "פתיחה";
        public static final String OPEN_WITH = "פתיחה באמצעות…", OPEN_WITH_SHORT = "באמצעות";   // 1.11.0
        public static final String EDIT = "עריכה…", EDIT_SHORT = "עריכה";
        public static final String SHARE_DOTS = "שיתוף…";
        public static final String SEND_DOTS = "שליחה למכשיר…";
        public static final String UNZIP_DOTS = "פריסת ZIP…";
        public static final String COPY_PATH = "העתקת המסלול";
        public static final String MOVE_THEN_PASTE = "העברה (ואז הדבקה)";
        public static final String COPY_THEN_PASTE = "העתקה (ואז הדבקה)";
        public static final String RESTORE_ICON = "↩ שחזור";
        public static final String DELETE_ON_DRIVE = "מחיקה מהכונן";
        public static final String PURGE = "מחיקה לצמיתות";
        public static final String TO_LIST = "לרשימה";
        public static final String RESTORE = "שחזור";
        public static final String SHARE = "שיתוף";
        public static final String TO_DEVICE = "למכשיר";
        public static final String UNZIP = "פריסה";
        public static final String PATH = "מסלול";
        public static final String NAME_ON_DRIVE = "שם בכונן";
        public static final String PASTE_HINT_FOLDER = " — מנווטים לתיקייה ולוחצים \"הדבקה כאן\"";
    }

    /** המיון (Sort) — רק הניסוחים; שמות המיון עצמם נכתבים בקובצי ההגדרות (תחביר) */
    public static final class SortW {
        private SortW() { }

        public static final String SORTED_BY = "ממוין לפי ";
        public static final String BY_MATCH = ", לפי סוג התאמה";
        public static final String NORMAL_DIR = "כיוון רגיל";
        public static final String REVERSE_DIR = "הפוך כיוון";
        public static final String GROUP_BY_MATCH = "קיבוץ לפי סוג התאמה";
        public static final String SORT_NOW = "מיון · כעת: ";
    }

    /** מצבי פריט (FileItem) */
    public static final class Item {
        private Item() { }

        public static final String MISSING_SHORT = "⚠ הקובץ לא נמצא";
        public static final String MISSING_MANY = "חסרים";
        public static final String DELETED = "נמחק · בסל המחזור";
        public static final String DELETED_MANY = "נמחקו · בסל המחזור";
        public static final String OFFLINE = "המדיה הוצאה או פגומה";
        public static final String OFFLINE_MANY = "במדיה שהוצאה או פגומה";
        public static final String IN_TRASH = "בסל המחזור";
        public static final String HIDDEN = "מוסתר";
        public static final String HIDDEN_MANY = "מוסתרים";
        public static final String OUT = "לא עונה עוד לסינון";
        public static final String OUT_MANY = "לא עונים עוד לסינון";
    }

    /** הלוח (Clip) */
    public static final class ClipW {
        private ClipW() { }

        public static final String AND = " ו";
        public static final String TO_COPY = " להעתקה";
        public static final String TO_MOVE = " להעברה";
        public static final String PICK_FOLDER_FIRST = "בוחרים קודם תיקייה";
        public static final String IS_APP_FOLDER = "זו תיקיית התוכנה";
        public static final String IS_TRASH = "זה סל המחזור";
        public static final String NOT_INTO_MOVED = "אי אפשר לתוך התיקייה שמעבירים";
        public static final String ALREADY_HERE = "הם כבר בתיקייה הזו";
    }

    /** הוספה לרשימה (Adder) */
    public static final class AddW {
        private AddW() { }

        public static final String ADDED_N = "נוספו ";
        public static final String ITEMS_DOT = " פריטים · ";
        public static final String THE_FOLDER = "התיקייה \"";
        public static final String FOLDERS_SUFFIX = " תיקיות";
        public static final String AND = " ו־";
        public static final String ADD_N = "הוספת ";
        public static final String TO_LIST = " לרשימה \"";
        /** 1.9.0: ברירת המחדל — תיקייה שמתעדכנת; השתיים שאחריה — צילום של הרגע. */
        public static final String LIVE = "✨ רשימה חכמה של התיקייה (מה שיתווסף לתיקייה — יופיע גם ברשימה)";
        public static final String EACH_LIVE = "✨ כל תיקייה — רשימה חכמה";
        public static final String AS_SUBLIST = "📸 צילום: כרשימת-משנה, עם המבנה (לא מתעדכן)";
        public static final String EACH_AS_SUBLIST = "📸 צילום: כל תיקייה כרשימת-משנה, עם המבנה (לא מתעדכן)";
        public static final String FILES_ONLY = "📸 צילום: הקבצים בלבד, בלי המבנה (לא מתעדכן)";
        public static final String LIVE_ADDED = " רשימות חכמות של תיקיות · ";
        public static final String ADDING = "מוסיף…";
        public static final String LISTS_AND = " רשימות ו-";
    }

    /** עד מ0.29 — תפריט הכלי (ToolMenu, נמחק); נשאר השם בטופס הרשימה (DbListScreen) */
    public static final class ToolM {

        private ToolM() { }

        public static final String NAME = "שם";
    }

    /** כלי הכפולים — הטופס (FnDup); מפתחות וערכים בקובץ ההגדרות — תחביר */
    public static final class DupT {
        private DupT() { }

        public static final String DUPS = "כפולים";
        public static final String WITHIN = "כפולים בתוך מקום אחד";
        public static final String BETWEEN = "השוואה בין שני מקומות";
        public static final String NO_SOURCES_ALL = "אין מקורות = כל הכוננים";
        public static final String NEED_SOURCE = "חובה לבחור לפחות מקור אחד";
        public static final String B_AGAINST = "ב' — מול מה";
        public static final String NAME = "שם";
        public static final String WHAT_WANT = "מה רוצים?";
        public static final String SAME_BY = "קובץ נחשב זהה לפי";
        public static final String A_WHAT = "א' — מה בודקים";
        public static final String WHERE_SEARCH = "איפה מחפשים כפולים";
        public static final String NEED_B = "בהשוואה בין שני מקומות צריך לבחור גם את ב'";
        /** 195 */
        public static final String NAME_SAME_BOX = "שם זהה";
        public static final String NAME_LIKE_BOX = "שם דומה";
        public static final String SIZE_BOX = "גודל זהה";
        public static final String CONTENT_BOX = "תוכן זהה (חתימה — כולל הגודל)";
        public static final String NEED_SOMETHING = "צריך לבחור שם זהה, או גודל, או תוכן";
        public static final String SIMILAR_NEEDS_SIZE = "\"שם דומה\" — רק יחד עם גודל או תוכן";
        public static final String SIMILAR_IS = "שם דומה — אותה סיומת, וכל המילים של שם אחד נמצאות בשם השני, בכל סדר; "
                + "בלי מה שבסוגריים ובלי סימנים. למשל: \"דרך ה' (1).pdf\" ≈ \"דרך ה'.pdf\" · \"רמחל - דרך ה.pdf\" ≈ \"דרך ה רמחל.pdf\".";

        /** במילים: מה ייחשב זהה. */
        public static String explain(int name, boolean size, boolean content) {
            if (name != 1 && !size && !content) return NEED_SOMETHING;
            if (name == 2 && !size && !content) return SIMILAR_NEEDS_SIZE;
            StringBuilder sb = new StringBuilder("ייחשבו זהים: ");
            if (name == 1) sb.append("אותו שם בדיוק");
            if (name == 2) sb.append("שם דומה");
            if (content) sb.append(name == 0 ? "" : ", ").append("אותו תוכן (ולכן גם אותו גודל)").append(name == 0 ? " — בכל שם" : "");
            else if (size) sb.append(name == 0 ? "" : ", ").append("אותו גודל").append(name == 0 ? " — בכל שם ובכל תוכן" : "");
            else sb.append(" — בכל גודל");
            if (!content && size) sb.append("\n(התוכן לא נבדק)");
            if (name == 2) sb.append("\n\n").append(SIMILAR_IS);
            return sb.toString();
        }
    }

    /** כלי הפס הצף (FnBar) */
    public static final class BarT {
        private BarT() { }

        public static final String BAR_NAME = "פס חיפוש צף";
        public static final String NO_PERMISSION = "חסרה הרשאה";
        public static final String NEEDS_OVERLAY = "הפס דורש הרשאת הצגה מעל אפליקציות אחרות.";
        public static final String OPEN_SETTING = "פתיחת ההגדרה";
        public static final String BAR_ON = "פס החיפוש הופעל";
        public static final String CANT_OPEN_PERMISSION = "לא ניתן לפתוח את מסך ההרשאה";
        public static final String IN_PATH_TOO = "לחפש גם במסלול";
        public static final String LOOSE = "הרחבת כתיב חסר";
        public static final String NAME = "שם";
    }

    /** רכיב המקורות (Sources) */
    public static final class Src {
        private Src() { }

        public static final String FOLDER_BTN = "+ תיקייה";   // 200: קצר — שני הכפתורים בשורה עם "כל הכוננים"
        public static final String DRIVE_BTN = "+ כונן";
        public static final String DRIVE = "כונן";
        public static final String CLEAR = "✕ ניקוי";   // 197: מוצג רק כשיש מקורות
        public static final String WHOLE = " (כולו)";
        public static final String ALL_DRIVES = "כל הכוננים";
    }

    /** שליחה והוספה מהירה (Acts) */
    public static final class ActsW {
        private ActsW() { }

        public static final String SEND = "שליחה";
        public static final String NOT_AT_PATH = "הקבצים לא נמצאו במסלול שלהם";
        public static final String NO_ROOM_PACK = "אין מספיק מקום לארוז את התיקיות: צריך ";
        public static final String FREE = ", פנוי ";
        public static final String PACKING = "אורז את התיקיות לשליחה…";
        public static final String SEND_CANCELLED = "השליחה בוטלה";
        public static final String NOT_SENT = " לא נמצאו במסלול שלהם, ולא נשלחו";
        /** 1.10.0: תיקייה — הקבצים כמו שהם, או ארוזה. */
        public static final String FOLDER_HOW = "איך לשלוח את התיקייה?",
                AS_ZIP = "🗜 ארוזה (ZIP) — שומרת על המבנה; בצד השני צריך לפרוס";

        public static String asFiles(int n) {
            return "📄 הקבצים כמו שהם — " + n + " קבצים, גם מתיקיות המשנה (בלי המבנה)";
        }

        public static String tooMany(int n, int max) {
            return n + " קבצים — יותר מ־" + max + ", ולכן התיקייה נשלחת ארוזה (ZIP)";
        }
    }

    /** חלקי ממשק משותפים (Ui) */
    public static final class UiW {
        /** 1.13.1: קובץ פקודות — תווית הסוג, והשורה מתחת לשם (המשתמש: "סימן שרומז שהם אינם אלא קבצי טקסט"). */
        public static final String BATCH_LINE = "פקודות · קובץ טקסט";
        private UiW() { }

        public static final String CANT_OPEN_PERMISSION = "לא ניתן לפתוח את מסך ההרשאה";
        public static final String BATCH_IN_TRASH = "קובץ פקודות שבסל המחזור לא מריצים. אפשר לשחזר אותו ואז להריץ.";
        public static final String UNZIP_HERE = "📂 פריסה כאן…";
        public static final String OPEN_OTHER = "פתיחה בתוכנה אחרת";
        public static final String NO_APP = "אין אפליקציה שיודעת לפתוח קובץ מסוג זה";
        public static final String OPEN_FAILED = "הפתיחה נכשלה. הפרטים ביומן";
        public static final String NO_TEXT_EDITOR = "לא נמצאה אפליקציה לעריכת טקסט";
        public static final String COPIED = "הועתק";
    }

    /** הבורר האחד (PickDialog) */
    public static final class Pick {
        private Pick() { }

        public static final String HIDDEN_NO = "מוסתרות: לא";
        public static final String HIDDEN_IS = "מוסתרות: ";
        public static final String YES = "כן";
        public static final String NO = "לא";
        public static final String VIEW_IS = "תצוגה: ";
        public static final String SHOWING = "מוצגות ";
        public static final String FIRST_NARROW = " הראשונות — אפשר לצמצם את החיפוש";
        public static final String NOT_FOUND = "לא נמצא";
        public static final String INSIDE = " בתוך";
    }

    /** טופס הרשימה החכמה (SmartForm) */
    public static final class SmartF {
        private SmartF() { }

        public static final String DETAILS = "פרטים (צלמית, תיאור, למה)";   // 1.16.0
        public static final String WORDS_OPT = "מילים בשם (לא חובה)";
        /** 197: הטופס הדחוס — תוויות בשורה עם השדה. */
        public static final String NAME_L = "שם", WORDS_L = "מכיל מילים", WORDS_HINT = "לא חובה";
        public static final String FROM_WHERE = "מאיפה (מקורות)";
        public static final String COND_IN_FILE = "התנאי שייכתב בקובץ";
        /** 196: הסידור החדש של הטופס */
        public static final String FROM = "מאיפה";
        public static final String WHAT_SEARCH = "מה מחפשים";
        public static final String HOW_SHOW = "איך מציגים";
        public static final String MORE_FILTERS = "עוד סינון";
        /** 202: מה רואים מתחת לכותרת המקופלת, בלי לפתוח. */
        public static final String MORE_WHAT = "בלי תאריך ובלי גודל", ADV_OWN = "טעינה ושמירה", BY_ADDED = " (הוספה)";

        /** "מאז שבוע", "גדול מ־10MB". */
        public static String phrase(String mode, String value) {
            if (value == null || value.isEmpty()) return mode.trim();
            return mode.endsWith("־") ? mode + value : mode + " " + value;
        }
        public static final String ADVANCED = "תבנית";
        public static final String SORT_L = "מיון";
        public static final String VIEW_L = "תצוגה";
        public static final String ONLY_HERE = "תבנית — נקודת התחלה: טעינה מעתיקה את ההגדרות שלה לכאן; שינוי בתבנית לא משנה רשימות שנוצרו ממנה.";
        public static final String LOAD_SAVED = "📥 לטעון";
        public static final String LOAD_TITLE = "טעינה מתבנית";
        public static final String SAVE_REUSE = "💾 לשמור…";
        public static final String UNLINK = "✂ לנתק — רק לרשימה הזו";
        public static final String NONE_SAVED = "עוד אין תבניות";

        public static String shared(String name) {
            return "הגדרה נפוצה \"" + name + "\"; שינוי כאן ישנה בכל מקום שהיא.";
        }
    }

    /** שורת הסיכום של רשימה (DbListScreen, ListCount); עד מ0.29 — Stats */
    public static final class StatsW {
        private StatsW() { }

        public static final String FILES = " קבצים";
    }

    /** כלי אפליקציה (FnApp) */
    public static final class AppT {
        private AppT() { }

        public static final String APP_REF = "הפניה לאפליקציה אחרת";
        public static final String APP_NOT_FOUND = "האפליקציה לא נמצאה";
        public static final String NO_APPS = "לא נמצאו אפליקציות";
        public static final String PICK_APP = "בחירת אפליקציה";
    }

    /** כלי הסל (FnTrash) */
    public static final class BinT {
        private BinT() { }

        public static final String TRASH = "סל מחזור";
        public static final String NAME = "שם";
    }

    /** כלי ההגדרות (FnSettings) */
    public static final class SetT {
        private SetT() { }

        public static final String NAME = "שם";
        public static final String SETTINGS = "הגדרות";
    }

    /** שמות הכוננים (Drive) */
    public static final class DriveW {
        private DriveW() { }

        public static final String INTERNAL_NAME = "זיכרון פנימי";
        public static final String CARD_NAME = "כרטיס זיכרון";
    }

    /** בורר התיקיות (FolderPicker) */
    public static final class FolderP {
        private FolderP() { }

        public static final String PICK_FOLDER = "בחירת תיקייה";
        public static final String ON_DRIVE_SUFFIX = " בכונן";
        public static final String FOLDER_NAME = "שם התיקייה";
    }

    /** סינון תאריך וגודל — הטופס (FilterForm) */
    public static final class Filter {
        private Filter() { }

        /** 189 */
        public static final String DATE_LABEL_ANY = "תאריך";
        public static final String[] DATE_BY = {"של השינוי האחרון", "של ההוספה (מתי הגיע)"};
        public static final String DATE_HINT = "יום · שבוע · חודש · שנה · 3ימים · 2026-09-01";
        public static final String SIZE_LABEL = "גודל";
    }

    /** קובצי ini (Ini) */
    public static final class IniW {
        private IniW() { }

        public static final String NOT_READ_SAVE_DELAYED = "הקובץ לא נקרא — השמירה נדחתה (";
    }

    /** האינדקס (Index) */
    public static final class Idx {
        private Idx() { }

        public static final String CANT_SAVE_INDEX = "לא ניתן לשמור את האינדקס";
    }

    /** פס הבחירה (SelectBar) */
    public static final class SelBar {
        private SelBar() { }

        public static final String MORE = "עוד";
        /** 1.14.0: כל מה שנוגע בכונן — בתפריט אחד (המשתמש: "יהיה תפריט פיזי, ומשם כל הפיזי — כולל הפיזי שבווירטואלי"). */
        public static final String PHYSICAL = "פיזי", PHYSICAL_TITLE = "פיזי — נוגע בקבצים בכונן";
    }

    /** בורר הרשימות (DbPicker) */
    public static final class ListP {
        private ListP() { }

        public static final String LIST_NAME = "שם הרשימה";
    }

    /** תאריך וגודל — הניסוחים בטופס; יחידות התאריך הן חלק מהתנאי (תחביר) */
    public static final class DateW {
        private DateW() { }

        public static final String NONE = "בלי";
        public static final String SINCE = "מאז";
        public static final String BEFORE_MORE_THAN = "לפני יותר מ־";
        public static final String BIGGER = "גדול מ־";
        public static final String SMALLER = "קטן מ־";
    }

    /** היומן (Log) */
    public static final class LogW {
        private LogW() { }

        public static final String VERSION = "גרסה ";
        /** 102 */
        public static final String TRIMMED = "… (היומן קוצר — נשאר החלק האחרון)";
        public static final String ANDROID = " · אנדרואיד ";
        public static final String ERROR_IN = "שגיאה ב";
        public static final String CRASH = "קריסה";
    }

    /** קבצים — זמן יחסי ושגיאת שמירה (Files) */
    public static final class FilesW {
        private FilesW() { }

        public static final String CANT_SAVE = "לא ניתן לשמור את ";
        public static final String UNKNOWN = "לא ידוע";
        public static final String NOW = "עכשיו";
        public static final String AGO = "לפני ";
        public static final String MINUTES = " דקות";
        public static final String HOURS = " שעות";
    }

    /** 1.9.0: מנהל קבצים בקריאה ישירה מהכונן */
    public static final class DiskW {
        private DiskW() { }

        public static final String TITLE = "קבצים פיזיים · ישר מהכונן";
        public static final String HINT = "קבצים פיזיים";
        public static final String READ_AT = "נקרא מהכונן ";
        public static final String NO_INDEX = "בלי אינדקס";
        public static final String BLOCKED = "אנדרואיד אינו מאפשר לקרוא תיקייה זו (גם לא לתוכנות ניהול קבצים). היא אינה בהכרח ריקה.";
        public static final String UNREADABLE = "התיקייה לא נקראה";
        public static final String NOT_FOUND = "התיקייה לא נמצאה בכונן";
        public static final String EMPTY = "התיקייה ריקה";
        public static final String DRIVES = "כוננים";
        public static final String FOLDERS = " תיקיות";
        public static final String FILES = " קבצים";
        public static final String HIDDEN_COUNT = " מוסתרים";
        // 1.11.0
        public static final String NEW_FOLDER = "תיקייה חדשה כאן";
        public static final String NEW_FOLDER_NAME = "שם התיקייה החדשה";
        public static final String PASTE = "📋 הדבקה כאן";
        public static final String SORT = "מיון…";
        public static final String UP = "⬆ לתיקייה שמעל";
        public static final String DETAILS = "פרטי התיקייה הזו";
        public static final String ZIP = "דחיסת התיקייה הזו ל־ZIP";
        public static final String[] SORTS = {"שם", "תאריך", "גודל", "סוג"};  // תחביר
        public static final String UP_DIR = "עולה", DOWN_DIR = "יורד";  // תחביר
        public static final String ASC = "⬆ עולה", DESC = "⬇ יורד";
        public static final String SORTED = " · ממוין לפי ";
        public static final String CLIP = "📋 ";
        public static final String CLIP_HINT = " · להדבקה — פותחים את התיקייה ונוגעים כאן";
        public static final String CANT_HERE = "כאן אי אפשר להדביק — פתח תיקייה";
        public static final String MODIFIED = "שונה לאחרונה: ";
        public static final String IN_FOLDER = "בתוכה: ";
        public static final String ZIP_TITLE = "דחיסה ל־ZIP: ";
        public static final String ZIP_CREATES = "נוצר קובץ ZIP חדש ליד התיקייה; התיקייה עצמה נשארת";
        public static final String ZIPPING = "דוחס…";
        public static final String ZIPPED = "נוצר: ";
    }

    /** 1.9.0: שורת הכפתורים העליונה */
    public static final class Top {
        private Top() { }

        public static final String HOME = "בית", BACK = "חזרה", SEARCH = "חיפוש", COMMANDS = "פקודות",
                FILES = "קבצים פיזיים", SETTINGS = "הגדרות", ADD = "כפתור";
        public static final String T_LINE = "⌨ שורת פקודה בכתב";
        public static final String NO_BATCH = "אין קובצי פקודות";
        public static final String LINE_HINT = "למשל: פתח רשימה חכמה אחרונים · מצא סוג:ספרים תאריך:שבוע · MOVE …";
        /** 1.13.0 */
        public static final String VIRTUAL = "קבצים וירטואליים", TRASH = "סל המחזור", EDITOR = "עורך הפקודות";

        /** 1.10.0: סוגי המסכים — שמות המקומות של השורות שלהם (גם בקובץ הכפתורים). */
        public static final String S_LIST = "רשימה ידנית", S_HOME = "מסך הבית",
                S_SEARCH = "חיפוש ורשימה חכמה", S_DISK = "קבצים פיזיים", S_TRASH = "סל המחזור",
                S_SETTINGS = "הגדרות", S_BATCH = "עורך הפקודות";
        /** 1.10.0: מה שהיה כפתורים קבועים בתחתית. */
        public static final String NEW_LIST = "רשימה", FROM_DRIVE = "מהכונן", FROM_SEARCH = "מחיפוש";

        public static String notHere(String name) {
            return "\"" + name + "\" — פעולה של מסך אחר, לא של המסך הזה";
        }

        /** מ0.41: השורות — מהרשימות של "שמור לתוכנה"; העריכה שם. */
        public static final String ROWS_TITLE = "השורות — ברשימות של \"שמור לתוכנה\"",
                ROW_HINT = "זו הרשימה של השורה: מה שבה — הכפתורים. ➕ מוסיף פקודה; נגיעה ארוכה — הסרה וסידור";

        public static String rowAll(String side, int n) {
            return "✏ " + side + " " + n + " — בכל המסכים";
        }

        public static String rowHere(String side, int n, String screen) {
            return "✏ " + side + " " + n + " — רק במסך: " + screen;
        }

        public static String rowNew(String side, int n) {
            return "➕ שורה חדשה: " + side + " " + n;
        }
    }

    /** 1.10.0: בכאילו — קובץ פקודות בהדמיה */
    public static final class AsIf {
        private AsIf() { }

        public static final String TITLE = "סקריפט זה רץ במצב הדמיה";
        public static final String ONLY_VIRTUAL = "כל הפעולות יתבצעו באופן וירטואלי בלבד";
        public static final String GO = "הרצה בהדמיה";
        public static final String SCREEN = "תוצאת הדמיה";
        public static final String DONE = "ההדמיה הסתיימה — שום דבר לא השתנה. כדי להריץ באמת: למחוק מהקובץ את השורה \"הדמיה\" (או \"בכאילו\") ולהריץ שוב.";
        public static final String EDIT = "פתיחת הקובץ לעריכה";
        public static final String ECHO = "הודעה מהקובץ";
        public static final String NEW_FOLDER = "תיקייה חדשה · ";
        public static final String WILL_BE = "יהיה ב: ";
        public static final String FROM = " · מ: ";
        public static final String TO_TRASH = "יעבור לסל · ";
        public static final String SHARE = "יישלח למכשיר אחר";
        public static final String AT_END = "ייפתח בסוף";
        public static final String NOTHING = "אין בקובץ שינויים";
    }
    /** 1.12.0: עיצובים עם שם (Design, Looks) */
    public static final class DesignW {
        private DesignW() { }

        public static final String WHY_DEFAULT = "המראה הרגיל של התוכנה: שורות, צלמית קטנה בצד, השם והנתונים לידה";
        public static final String WHY_FILES = "רשת גדולה וברורה, כמו מנהל קבצים שוטף — לרשימה שהיא סביבת עבודה";
        public static final String WHY_WORDS = "בלי צלמיות: אריחים צבעוניים עם מילים, בשני טורים";
        public static final String FILE_NOTE = "אפשר לערוך ידנית. מה שחסר כאן נלקח מ\"עיצוב ברירת מחדל\", ושורה לא מובנת לא נלקחת";
        public static final String FILE_NOTE2 = "רשת: צלמית וריווח באחוזים מרוחב האריח. שורות: באחוזים מגודל השם. משקל: דק / רגיל / בינוני / מודגש. סביב הצלמית: בלי / מסגרת / ריבוע צבעוני";
        public static final String LISTS_NOTE = "צלמית וצבע לכל רשימה, לפי המסלול שלה, למשל: לימוד/הערות = צלמית 📚 · צבע ירוק. בסביבה אחרת — רק כשיש רשימה באותו מסלול";
        public static final String EXISTS = "כבר יש עיצוב בשם הזה";
        public static final String MENU = "🎨 עיצוב";
        public static final String TITLE_LIST = "עיצוב לרשימה הזו";
        public static final String TITLE_HOME = "עיצוב למסך הבית";
        public static final String INHERIT = "↑ כמו הרשימה שמעליה";
        public static final String NOW = "עכשיו: ";
        public static final String FROM_ABOVE = " (יורש מ\"";
        public static final String FROM_HOME = " (יורש ממסך הבית)";
        public static final String EDIT = "📝 עריכת העיצוב הנוכחי בקובץ הטקסט";
        public static final String EDIT_FORM = "✏️ שינוי העיצוב הנוכחי";
        public static final String FORM_TITLE = "שינוי עיצוב · ";
        public static final String GRID = "רשת", COLS = "טורים", ICON_PCT = "צלמית %", GAP_PCT = "ריווח %";
        public static final String SHOW_ICON = "הצג צלמית", FRAME = "מסגרת", FILL = "ריבוע צבעוני", TINT = "צבע רקע לפריט";
        public static final String SHOW_DATA = "הצג נתונים", DATA_SIDE = "נתונים בצד";
        public static final String TEXT_HEAD = "טקסט: גודל · משקל · גופן";
        public static final String T_NAME = "שם", T_DATA = "נתונים", T_TITLE = "כותרת";
        public static final String FIT = "התאם למקום", MIN = "מזערי", LINES = "שורות לשם";
        public static final String SAVE_AS_BTN = "שמירה בשם…";
        public static final String MANAGE = "🎨 עיצובים";
        public static final String HOME_LINE = "🏠 העיצוב של מסך הבית: ";
        public static final String HOME_NONE = "כמו קודם";
        public static final String RENAME = "שינוי שם", DELETE = "מחיקה";
        public static final String DELETED = "העיצוב נמחק";
        public static final String DELETE_ASK = "עותק נשמר בגיבויים. רשימות שהשתמשו בו יקבלו את העיצוב של הרשימה שמעליהן";
        public static final String ICON_SHORT = "צלמית";
        public static final String SAVE_AS = "💾 שמירת העיצוב הנוכחי בשם אחר…";
        public static final String SAVE_AS_HINT = "שם, למשל: מדף ספרים";
        public static final String RESET = "↺ חזרה לברירת מחדל — העיצוב הנוכחי";
        public static final String RESET_DONE = "העיצוב חזר למקור. הצלמיות והצבעים של הרשימות נשארו";
        public static final String NOT_BUILT_IN = "חזרה לברירת מחדל — רק לעיצוב מובנה";
        public static final String FROM_MODE = "  · מתוך ";
        public static final String CHOSEN = "העיצוב: ";
        public static final String COPY_FAILED = "העתקת העיצוב נכשלה";
    }

    /** 1.13.0: פקודות הפתיחה בעברית (Verbs) */
    public static final class Verb {
        private Verb() { }
        public static final String MENU = "תפריט: ";   // 1.17.0
        public static final String MODE = "אופן: ";   // מ0.42
        public static final String TOOL = "כלי: ";   // מ0.19
        /** מ0.40 */
        public static final String NO_OPEN_ONLY = "/בלי-לפתוח — רק ב\"צור רשימה\", \"צור רשימה חכמה\" ו\"עדכן רשימה חכמה\"";

        public static final String SCREEN = "מסך ", LIST = "רשימה: ", SMART = "רשימה חכמה: ", FOLDER = "תיקייה: ",
                FIND = "מצא: ", BACK = "חזרה למסך הקודם", RUN = "הרצה: ";
        public static final String NOT_FOUND = "לא נמצא: ";

        public static String atEnd(String what) {
            return "ייפתח בסוף: " + what;
        }

        public static String unknownScreen(String s) {
            return "מסך לא מוכר: \"" + s + "\" (הבית, חיפוש, פקודות, קבצים פיזיים, קבצים וירטואליים, הגדרות, סל, עורך פקודות, גיבויים, אבחון, תוספים)";
        }

        public static String usage(String verb) {
            if (verb.equals("מצא")) return "שימוש: מצא <תנאי>, למשל: מצא סוג:ספרים תאריך:שבוע";
            if (verb.equals("הרץ")) return "שימוש: הרץ <קובץ פקודות>";
            if (verb.equals("הוסף")) return "שימוש: הוסף מהכונן · הוסף מחיפוש";
            if (verb.equals("צור")) return "שימוש: צור רשימה <שם> · צור רשימה חכמה \"שם\" <תנאי> · צור פקודה · צור אופן \"שם\"";   // 1.22.5: גם צור פקודה; מ0.42: אופן
            if (verb.equals("עדכן")) return "שימוש: עדכן רשימה חכמה \"שם\" <תנאי>";
            if (verb.equals("חזור")) return "שימוש: חזור";                                   // 1.22.5 (עד כאן — השימוש של פתח)
            return "שימוש: פתח מסך <מסך> · פתח רשימה <מסלול> · פתח רשימה חכמה <שם> · פתח תיקייה <מסלול> · פתח תפריט <רשימה>";
        }
    }

    /**
     * 1.22.5: הטקסטים של ההגדרה האחת של שפת הפקודות (Commands) — מה כל פקודה עושה, ושם והסבר לכל פרמטר.
     * מכאן נבנים ההסבר בעורך, השגיאות, והממשק לפקודות.
     */
    public static final class Def {
        private Def() { }

        // פקודות הקבצים — מה הן עושות
        public static final String W_COPY = "העתקת קבצים", W_MOVE = "העברת קבצים או תיקייה", W_DEL = "מחיקה (לסל המחזור)",
                W_REN = "שינוי שם", W_MD = "תיקייה חדשה", W_RD = "מחיקת תיקייה (לסל)",
                W_COMP = "השוואת שתי תיקיות לפי מסלול — אחריה: %רק-במקור% ו־%רק-ביעד% כמקור של COPY / MOVE",
                W_START = "פתיחה בסוף: תיקייה במנהל הקבצים, או קובץ (קובץ apk — התקנה; תבנית — החדש ביותר)",
                W_ECHO = "הודעה שתופיע בדוח", W_REM = "הערה, לא מתבצעת";
        // הפעלים בעברית — מה הם עושים
        public static final String W_OPEN = "פותח בסוף: מסך, רשימה, רשימה חכמה, תיקייה, או תפריט מ\"שמור לתוכנה\"",
                W_CREATE = "יוצר רשימה, רשימה חכמה, או קובץ פקודות חדש", W_UPDATE = "משנה את התנאי של רשימה חכמה",
                W_FIND = "פותח חיפוש עם תנאי", W_BACK = "חוזר למסך הקודם", W_RUN = "מריץ קובץ פקודות אחר",
                W_ADD = "פותח הוספה לרשימה: מהכונן או מחיפוש";
        // שורות כותרת
        public static final String W_AS_IF = "בראש הקובץ: כל השורות רצות בהדמיה, בלי לשנות דבר";

        // שמות הפרמטרים
        public static final String P_SOURCE = "מקור", P_DEST = "יעד", P_FILES = "קבצים", P_FILE = "קובץ", P_NEW_NAME = "שם חדש",
                P_FOLDER = "תיקייה", P_WHAT_OPEN = "מה לפתוח", P_TEXT = "טקסט", P_KEEP = "שמירת מסלול", P_TAKEN = "שם תפוס",
                P_CONTAINS = "מכיל", H_CONTAINS = "רק קבצים שהטקסט הזה כתוב בהם (נבדק אחרון, רק במי שעבר את שאר הסינון)",
                P_JOURNAL = "יומן", P_LIMIT = "עצירה מעל אחוז", P_STATE = "שחזור לפי זמן", P_DATE = "תאריך", P_SIZE = "גודל",
                P_HIDDEN = "גם מוסתרים", P_LIST = "רשימה כמקור", P_NOT_EMPTY = "גם אם אינה ריקה", P_ARG = "מה";
        // ההסברים
        public static final String H_COPY_SRC = "הקבצים או התיקייה שמעתיקים (אפשר תבנית, למשל \\*.pdf), או %רק-במקור% / %רק-ביעד% אחרי COMP",
                H_MOVE_SRC = "הקבצים או התיקייה שמעבירים (אפשר תבנית), או %רק-במקור% / %רק-ביעד% אחרי COMP",
                H_DEST = "התיקייה שאליה הקבצים מגיעים",
                H_DEL_SRC = "קובץ, או תבנית כמו \\*.tmp — עוברים לסל המחזור",
                H_REN_SRC = "הקובץ ששמו משתנה (קובץ אחד, בלי * ו־?)", H_NEW_NAME = "רק השם, בלי מסלול",
                H_MD = "התיקייה שתיווצר, כולל התיקיות שבדרך", H_RD = "התיקייה שעוברת לסל המחזור",
                H_COMP_SRC = "התיקייה הראשונה (או במקומה /רשימה:)", H_COMP_DEST = "התיקייה השנייה",
                H_START = "תיקייה, קובץ, תבנית (החדש ביותר מבין המתאימים), או %בית%",
                H_TEXT = "מה שיופיע בדוח (ECHO), או הסבר לקורא הקובץ (REM)",
                H_KEEP = "כל מה שבתיקייה, גם בתיקיות שבתוכה, נכתב ביעד באותו מבנה. בעברית: /מסלול",
                H_TAKEN = "מה עושים כשביעד כבר יש קובץ באותו שם. /Y — כמו להחליף. לשנות-שם: בשם אחר (2), וקובץ שיש ביעד זהה לו — נשאר במקומו",
                H_JOURNAL = "קובץ טקסט שבו נרשם מיד כל קובץ שהגיע ליעד (מסלול וגודל)",
                H_LIMIT = "אחרי COMP: אם השורה מוציאה יותר מהאחוז הזה מהקבצים — שגיאה, ושום דבר לא זז",
                H_STATE = "המקור הוא גיבוי (נוכחי / היסטוריה / יומן): נכתב המצב של אותו זמן — תאריך, או תאריך ושעה",
                H_DATE = "רק קבצים לפי תאריך, למשל /תאריך:שבוע או /תאריך:<1.1.2026",
                H_SIZE = "רק קבצים לפי גודל, למשל /גודל:>10MB",
                H_HIDDEN = "כולל קבצים ותיקיות מוסתרים (שמתחילים בנקודה)",
                H_LIST = "במקום תיקיית מקור: רשימה ידנית בכל העומק (קבצים, תתי־רשימות, תיקיות מתעדכנות, חכמות)",
                H_NOT_EMPTY = "מסיר גם תיקייה שיש בה קבצים (לסל)",
                H_COMPAT = "תחביר ישן (XCOPY / ROBOCOPY) — מובן, לא מוצג בטופס";
        /** במקום "<מה לפתוח>" בשורת השימוש של START. */
        public static final String U_START = "<תיקייה | קובץ | תבנית, למשל vfm-*.apk>";
        /** 1.23.0: השם הקצר בעברית — לתפריט ולחיפוש ("העתק" מוצא את COPY). השורה בקובץ — כמו שהשפה מכירה. */
        public static final String N_COPY = "העתק", N_MOVE = "העבר", N_DEL = "מחק", N_REN = "שנה שם", N_MD = "צור תיקייה",
                N_RD = "הסר תיקייה", N_COMP = "השווה", N_START = "פתח בסוף", N_ECHO = "הודעה", N_REM = "הערה";
        /** ערך של מתג בשורת השימוש. */
        public static final String V_FILE = "<קובץ>", V_TIME = "<זמן>", V_LIST = "<רשימה>";
    }

    /** 1.23.0: ממשק הפקודות (BatchEditScreen) — claude/mimshak-pkudot.md. */
    public static final class Ed {
        private Ed() { }

        public static final String MINE_SUFFIX = " (שלי)", NONE = "—", FILL = "השלמת התבנית",
                NO_SMART = "אין רשימות חכמות", WHOLE_HOME = "★ כל מה שבבית";

        /** 1.25.2: מה מבקשים במקום הפתוח. */
        public static String holeKind(int kind) {
            switch (kind) {
                case 1: return " — רשימה ידנית (קיימת, או שם חדש — תיווצר)";
                case 2: return " — רשימה חכמה";
                case 3: return " — מספר";
                case 4: return " — כמה רשימות (שורה לכל אחת)";
                case 5: return " — קובץ מתיקיית \"יצוא\" (או מסלול מלא)";
                default: return " — תיקייה";
            }
        }

        public static String fillFirst(String what) {
            return "מלא קודם: " + what;
        }
    }

    /** 1.13.0: שורת "למה" (Why) */
    public static final class WhyW {
        private WhyW() { }

        public static final String PREFIX = "💡 למה: ";
        public static final String KEY_SCREEN = "מסך:";
        public static final String FIELD = "למה (לא חובה)";
        public static final String HINT = "מה המטרה — כדי לזכור גם בעוד חודש";
        public static final String VIRTUAL_FILES = "מהאינדקס — מהיר, עם חיפוש וסינון. פעולה על קובץ (העברה, מחיקה) נעשית גם בכונן, עם אזהרה";
        public static final String DISK_FILES = "ישר מהכונן, כמו כל מנהל קבצים. כל פעולה נעשית בכונן, ואחר כך נשמרת באינדקס";
    }

    /** 1.26.0: מסך ההרצה (claude/hartzat-pkudot.md). */
    public static final class Run {
        private Run() { }

        public static final String TITLE = "הרצה: ", S_FILL = "מילוי", S_WHAT = "מה יקרה", S_RUN = "מריץ", S_SUM = "סיכום",
                NOTHING_TO_FILL = "אין מה למלא", NEXT = "הלאה — מה יקרה", CHECKING = "בודק את השורות…",
                WILL_CREATE = "ייווצרו", WILL_CHANGE = "יועתקו / ישתנו", WILL_DELETE = "יימחקו", DISK = "קבצים בכונן",
                ERRORS = "שגיאות", DETAILS = "פרטים — ", LINES = " שורות", AS_IF = "הדמיה — שום דבר לא יבוצע (\"הדמיה\" בראש הקובץ)",
                SHOW_AS_IF = "הצג את ההדמיה", RUN = "▶ הרץ", CREATED = "נוצרו", CHANGED = "הועתקו / השתנו", DELETED = "נמחקו",
                SKIPPED = "דולגו", TIME = "זמן", FULL_REPORT = "הדוח המלא", JOURNAL = "יומן", NO_DISK = "לא נוגע",
                STOPPED = " — נעצר באמצע", LINES_IN = " שורות: ";

        public static String stage(int n) {
            return "שלב " + n + " מתוך 4";
        }

        public static String checking(int line, int total) {
            return "בודק שורה " + line + " מתוך " + total;
        }

        public static String at(int line, int total, int round) {
            return "שורה " + line + " מתוך " + total + (round > 0 ? " · סבב " + round : "");
        }

        public static String diskLines(int n) {
            return n + " שורות נוגעות בקבצים עצמם";
        }

        public static String ranIn(String time) {
            return "הסתיים · " + time;
        }

        public static String open(String list) {
            return "פתח את \"" + list + "\"";
        }

        /** 1.27.0: הניקוי — אחרי שבדקת. */
        public static final String CLEAN = "🗑 מחק את מה שההרצה יצרה", CLEAN_TITLE = "למחוק את מה שההרצה הזו יצרה?",
                CLEAN_WHAT = "הרשימות האלה, עם כל מה שבתוכן — לסל (אפשר לשחזר משם). הקבצים בכונן לא נוגעים.",
                CLEANED = "נמחקו לסל", INPUT = "קלט", FILE = "קובץ: ";

        public static String exists(String list) {
            return "⚠ " + list + " כבר קיימת — ההעתקות ייכנסו לתוכה, והניקוי שבסוף לא ימחק אותה";
        }
    }

    /** 1.25.0: לולאה ומדידה בקובצי פקודות (claude/pkudot-reshimot.md). */
    public static final class Loop {
        private Loop() { }

        public static final String TIMES_WORD = "פעמים",
                NEED_NUMBER = "אחרי \"חזור\" — מספר הסבבים: חזור 5",
                BAD_LIMIT = "גבול הזמן לא מובן — למשל /עד:30שניות או /עד:2דקות",
                USAGE = "צורה: חזור <מספר> [/עד:<זמן>] … ובסוף: סוף",
                NO_END = "חסרה שורת \"סוף\" ללולאה",
                END_WITHOUT_START = "\"סוף\" בלי \"חזור\" לפניו",
                JOURNAL_FAILED = "⚠ יומן המדידה לא נכתב",
                WHAT_LOOP = "לולאה: השורות שבין \"חזור\" ל\"סוף\" רצות כמה פעמים. %סבב% — מספר הסבב (1, 2, 3…). "
                        + "/עד:30שניות — עוצר כשסבב אחד לקח יותר. העריכה — בשורה עצמה.",
                WHAT_END = "סוף הלולאה שנפתחה ב\"חזור\".",
                WHAT_MEASURE = "כל הקובץ נמדד: זמן לכל שורה, סך הכול, והשורה האיטית. /יומן:\"קובץ\" — נשמר גם לקובץ, בכל ריצה. "
                        + "/עד:2דקות — שורה שלקחה יותר מסומנת ❌ (ונספרת עם \"ודא\" שנכשלו).",
                N_LOOP = "חזור — לולאה", N_END = "סוף לולאה", N_MEASURE = "מדידה",
                N_CHECK = "בדיקה", WHAT_CHECK = "הקובץ רץ במסך ההרצה, בשלבים: מילוי ← מה יקרה (בדיקה של כל שורה, ואישור) ← מריץ ← סיכום שנשאר. בלי השורה הזו — הקובץ רץ מיד.";

        public static String round(int r) {
            return " (סבב " + r + ")";
        }

        /** מ0.45: "מדידה /עד:" — שורה שלקחה יותר מהגבול. */
        public static String tooSlow(long limit) {
            return "❌ חרגה מהזמן (עד " + time(limit) + ") — ";
        }

        public static final String LIMIT_USAGE = "צורה: מדידה /עד:<זמן> (למשל /עד:2דקות)";

        public static String took(long ms) {
            return " · ⏱ " + time(ms);
        }

        public static String time(long ms) {
            return ms < 1000 ? ms + " אלפיות" : String.format(java.util.Locale.ROOT, "%.1f שניות", ms / 1000.0);
        }

        public static String total(long ms, String slowLine, long slowMs) {
            return "⏱ סך הכול " + time(ms) + (slowLine != null ? " · האיטית: שורה " + slowLine + " (" + time(slowMs) + ")" : "");
        }

        public static String tooMany(int max) {
            return "מספר הסבבים — בין 1 ל־" + max;
        }

        public static String tooLong(int max) {
            return "יותר מדי שורות אחרי פריסת הלולאות (עד " + max + ")";
        }

        public static String stopped(int line, int round, long ms) {
            return "⏹ הלולאה שבשורה " + line + " נעצרה אחרי סבב " + round + " — הוא לקח " + time(ms) + ", יותר מהגבול";
        }
    }

    /** 1.24.0: פקודות על רשימות (ListCmd) — claude/pkudot-reshimot.md. */
    public static final class ListCmdW {
        private ListCmdW() { }

        public static final String HOME = "הבית",
                USAGE = "צורה: COPY [ידנית | חכמה | תוכן ידנית | קבצי ידנית | קבצי חכמה] \"שם\" [ידנית | תיקייה] \"יעד\"",
                USAGE_MD = "צורה: MD ידנית \"א\\ב\" — רשימה ידנית (חסרה — נוצרת בכל הדרך)",
                CONTENT_SMART = "\"תוכן\" — רק לרשימה ידנית; לקבצים שרשימה חכמה מוצאת: קבצי חכמה",
                INTO_SMART = "אי אפשר להכניס לתוך רשימה חכמה — היא מוצאת את הקבצים לבד, לפי התנאים שלה",
                INTO_ITSELF = "אי אפשר להכניס רשימה לתוך עצמה (או לתוך רשימה שבתוכה)",
                MOVE_TO_LIST = "העברה לרשימה — אין לה מובן: לרשימה נכנסים רק המסלולים, והקבצים נשארים במקומם. להוספה — COPY",
                HOME_FILES = "לבית אין קבצים משלו — אפשר לבחור רשימה",
                HOME_NO_REMOVE = "את הבית אי אפשר למחוק בפקודה",
                MD_SMART = "רשימה חכמה — יוצרים בטופס שלה; בפקודה — העתקה של חכמה קיימת",
                RD_FILES = "למחיקת הקבצים שברשימה: DEL קבצי ידנית \"שם\"",
                NO_FILTER = "סינון (/תאריך, /גודל) — עדיין לא חל על רשימות; /מכיל: — רק כשיש צד בכונן",
                INDEX_NOT_READY = "האינדקס עוד לא מוכן (או שחלק מהרשימה לא ידוע) — אי אפשר לדעת אילו קבצים ברשימה",
                FOLDER_TO_LIST = "תיקייה שלמה לרשימה — עדיין לא; לקבצים שבה: \"תיקייה\\*.*\"",
                FROM_DISK = " — הקבצים עצמם, מהכונן",
                USAGE_COMP = "צורה: COMP ידנית \"א\" ידנית \"ב\" · COMP חכמה \"א\" חכמה \"ב\" [/תנאים] · COMP קבצי חכמה \"א\" ידנית \"ב\"",
                COMP_FS = "השוואה בין רשימה לתיקייה — עדיין לא; בינתיים: COMP /רשימה:\"שם\" \"תיקייה\"",
                CONDS_SMART = "/תנאים — רק בין שתי רשימות חכמות",
                SAME_CONDS = "התנאים זהים";

        public static String noList(String name) {
            return "אין רשימה ידנית: \"" + name + "\"";
        }

        public static String noSmart(String name) {
            return "אין רשימה חכמה: \"" + name + "\"";
        }

        public static String notYet(String cmd) {
            return "הפקודה " + cmd + " — עדיין לא על רשימות";
        }

        public static String list(String label) {
            return "הרשימה " + label;
        }

        public static String smartList(String label) {
            return "הרשימה החכמה " + label;
        }

        public static String contentOf(int n, String label) {
            return "התוכן של " + label + " (" + n + ")";
        }

        public static String filesOf(String files, String label) {
            return files + " מתוך " + label;
        }

        public static String created(String label) {
            return "רשימה ידנית חדשה: " + label;
        }

        public static String exists(String label) {
            return "הרשימה " + label + " כבר קיימת — בלי שינוי";
        }

        public static String copy(String what, String to) {
            return what + " ← לתוך " + to;
        }

        public static String compared(String a, int onlyA, String b, int onlyB, int both) {
            return "השוואה לפי המסלולים — רק ב" + a + ": " + onlyA + " · רק ב" + b + ": " + onlyB + " · בשתיהן: " + both;
        }

        public static String conds(String a, String onlyA, String b, String onlyB) {
            return "התנאים — רק ב" + a + ": " + onlyA + " · רק ב" + b + ": " + onlyB;
        }

        public static String notIntoSelf(java.util.List<String> paths) {
            StringBuilder sb = new StringBuilder();
            for (String p : paths) sb.append(sb.length() > 0 ? ", " : "").append('"').append(p).append('"');
            return " · בלי " + sb + " (היעד עצמו — לא לתוך עצמו)";
        }

        public static String nowIn(long lists, long items) {
            return " · עכשיו בה: " + lists + " רשימות, " + items + " פריטים";
        }

        public static String copied(int done, int skipped, String why, String to) {
            return "נכנסו ל" + to + ": " + done + (skipped > 0 ? " · לא נכנסו: " + skipped + (why != null ? " (" + why + ")" : "") : "");
        }
    }

    /** מ0.12: פקודות הרשימות על המסד (DbListCmd) */
    public static final class DbCmd {
        public static String updated(String name) { return "עודכנה: " + name; }
        private DbCmd() { }

        // מ0.23: יצוא וייבוא — קובצי פקודות מוכנים ב־⭐
        public static final String TOOL_BAR = "פס צף", TOOL_APP = "פתיחת אפליקציה", TOOL_DUPS = "כפולים בכל הכוננים";   // בכונן (מ0.25)
        /** מ0.41: הפקודה המוכנה שפותחת את העורך האינטראקטיבי — השם הוא גם שם התוסף (כמו שכתוב בעמוד שלו). */
        public static final String TOOL_EDITOR = "עורך אינטראקטיבי";   // בכונן
        /** מ0.42: תוסף האבחון (בא עם התוכנה). */
        public static final String TOOL_DIAG = "אבחון";   // בכונן
        public static final String TOOL_EXPORT = "יצוא רשימה", TOOL_IMPORT = "ייבוא רשימה",   // בכונן
                HOLE_EXPORT = "רשימה-לייצוא", HOLE_FILE = "שם-הקובץ-בתיקיית-יצוא", HOLE_INTO = "לתוך-רשימה";   // תחביר

        public static String toolsAdded(java.util.List<String> names) {
            StringBuilder sb = new StringBuilder();
            for (String n : names) sb.append(sb.length() == 0 ? "" : ", ").append(n);
            return "נוספו לתפריט הפקודות ⭐: " + sb;
        }

        public static final String NO_HOME = "אין בית במסד הזה", SAME_NAME = "כבר יש שם רשימה בשם הזה",
                ALREADY = "כבר בה", MENU_PLUGIN_DIAG = "🔬 אבחון — בדיקות, דוחות, תקלות",
                TEST_MODE = "בדיקות מערכת";  // בכונן

        public static String removed(String what) {
            return what + " — יצא מהרשימות (לפני הביצוע — גיבוי של המסד); הקבצים בכונן לא נוגעים";
        }

        /** מ0.41: בטל לא-משויכים */
        public static final String LOOSE_AT_RUN = "ייבדק בהרצה: קבצים שאינם באף רשימה (קובץ שנמחק בכונן בשורות שלפני — יוצא מהרשימות שלו)";   // מ0.42
        public static final String LOOSE_NONE = "אין במסד קבצים שאינם באף רשימה",
                LOOSE_USAGE = "\"לא-משויכים\" — רק כך: בטל לא-משויכים · ודא לא-משויכים /פריטים:N";

        public static String looseRemoved(int n) {
            return (n == 1 ? "קובץ אחד שאינו באף רשימה — הרישום שלו נמחק" : n + " קבצים שאינם באף רשימה — הרישום שלהם נמחק")
                    + " מהמסד (לפני הביצוע — גיבוי של המסד); הקבצים בכונן לא נוגעים";
        }

        public static final String N_VERIFY = "ודא",  // תחביר
                W_VERIFY = "בדיקה בתוך קובץ פקודות: ✅ או ❌, ולא עוצרת. ודא ידנית \"א\" קיימת · חסרה · /רשימות:2 /פריטים:1 · ודא \"קובץ\" קיים · באינדקס · "
                        + "בסל · /בסל:0 · ודא מסד · ודא אופן \"שם\" חסר · ודא תקין (כל השורות מתחילת הקטע רצו בלי כישלון)",
                H_VERIFY = "הרשימה שבודקים (ידנית \"א\" או חכמה \"א\")",
                P_VERIFY = "מה לוודא", H_VERIFY_WHAT = "קיימת · חסרה · /רשימות:N · /פריטים:N (כמו \"עכשיו בה\" בדוח)",
                VERIFY_DB_ONLY = "ודא — עובדת כשעובדים במסד", NO_DB = "אין מסד לאופן הזה — לפתוח את הבית פעם אחת, ואז להריץ שוב",
                VERIFY_USAGE = "צורה: ודא ידנית \"א\" [קיימת | חסרה] [/רשימות:N] [/פריטים:N]",
                WORD_EXISTS = "קיימת", WORD_MISSING = "חסרה", WORD_EXISTS_M = "קיים", WORD_MISSING_M = "חסר", SW_LISTS = "/רשימות:", SW_ITEMS = "/פריטים:";  // תחביר

        public static final String USAGE_EXPORT = "צורה: COPY ידנית \"א\" קובץ \"*\" (או שם/מסלול) · גם קבצי ידנית \"א\", ידנית \"*\" (הבית — כל האופן)",
                EXPORT_DIR = "יצוא";  // בכונן

        public static String exportHeader(int format, String date, String what, int lists, int files, int skipped) {
            return "יצוא מהמסד · פורמט " + format + " · " + date + " · " + what + " · " + lists + " רשימות, " + files + " קבצים"
                    + (skipped > 0 ? " · לא בפורמט עדיין (חכמות, רעיונות, כותרות): " + skipped : "");
        }

        public static String exportModeName(String mode) {
            return "אופן " + mode;
        }

        public static String exportListName(String name) {
            return "רשימה " + name;
        }

        public static String exportFilesName(String name) {
            return "קבצי רשימה " + name;
        }

        public static String exported(String what, String path, int lists, int files, int skipped) {
            return "יצוא: " + what + " ← " + path + " · " + lists + " רשימות, " + files + " קבצים"
                    + (skipped > 0 ? " · לא נכנסו (חכמות, רעיונות, כותרות — עוד לא בפורמט): " + skipped : "");
        }

        public static final String MODE_PREFIX = "אופן:",  // תחביר
                USAGE_FROM_MODE = "צורה: COPY ידנית \"אופן:שם האופן/א\\ב\" ידנית \"יעד\" (גם תוכן ידנית)",
                ORDER_TITLE = "סידור", ORDER_UP = "⬆ מקום אחד למעלה", ORDER_DOWN = "⬇ מקום אחד למטה",
                ORDER_TOP = "⏫ לראש הרשימה", ORDER_BOTTOM = "⏬ לסוף הרשימה";

        public static String importLater(String path) {
            return "ייבוא מ־" + path + " — ייקרא בהרצה (הקובץ עוד לא קיים — נוצר בשורות שלפני)";
        }

        public static String cantRead(String name) {
            return "לא נמצא קובץ לייבוא בשם \"" + name + "\" (צריך שם של קובץ מתיקיית \"יצוא\", או מסלול מלא)";
        }

        public static String needFullWay(String name) { return "\"" + name + "\" נמצאת בכמה רשימות — צריך לכתוב את הדרך המלאה אליה"; }

        public static String imported(String file, String to, int lists, int files, int bad) {
            return "ייבוא: " + file + " ← לתוך " + to + " · " + lists + " רשימות, " + files + " קבצים" + (bad > 0 ? " · שורות שלא הובנו: " + bad : "");
        }

        public static String fromMode(String mode, String path, String to, int n) {
            return "מהאופן \"" + mode + "\": \"" + path + "\" ← לתוך " + to + " · " + n + " פריטים (עותק עצמאי)";
        }

        public static final String USAGE_REN = "צורה: REN ידנית \"א\\ב\" \"שם חדש\" (גם חכמה \"א\")",
                BAD_NAME = "שם ריק, או עם / או \\ — לא אפשרי לרשימה";

        public static String renamed(String label, String name) {
            return label + " ← השם עכשיו \"" + name + "\"";
        }

        public static String nameTaken(String name) {
            return "כבר יש באותה רשימה \"" + name + "\" — שם תפוס";
        }

        public static final String N_LINK = "קשר",  // תחביר
                W_LINK = "קישור במסד: אותה רשימה (או פריט) גם במקום נוסף — לא עותק. קשר ידנית \"א\" ידנית \"ב\"",
                H_LINK_SRC = "מה לקשר: ידנית / חכמה / תוכן ידנית / ידנית \"א\\*.pdf\"", H_LINK_DEST = "לאיזו רשימה ידנית",
                USAGE_LINK = "צורה: קשר (או MOVE) ידנית \"א\" ידנית \"ב\" · גם חכמה \"א\", תוכן ידנית \"א\", ידנית \"א\\*.pdf\"";

        public static String linked(String what, String to) {
            return what + " ← קושר גם ל" + to;
        }

        public static String moved(String what, String to) {
            return what + " ← הועבר ל" + to;
        }

        public static final String N_CONT = "המשך";  // תחביר
        public static final String N_WRITE = "כתוב", N_WAIT = "המתן", N_BACKUP = "גבה", N_REFRESH = "רענן", N_REPORT = "דוח";  // תחביר
        public static final String W_WRITE = "כותב טקסט לקובץ (יוצר אותו, או מחליף את מה שהיה בו). בלי טקסט — קובץ ריק. /הוסף — מוסיף בסוף",
                W_WAIT = "מחכה כמה שניות לפני השורה הבאה", W_BACKUP = "גיבוי ידני של המסד של האופן (לא נמחק לבד)",
                W_REFRESH = "מרענן את האינדקס, כדי שקבצים חדשים יימצאו. \"רענן עמוק\" — בדיקה מלאה; \"רענן מלא\" — בנייה מחדש",
                W_REPORT = "היומן של ההרצה נכתב גם לקובץ הזה",
                P_SECONDS = "שניות", H_SECONDS = "כמה שניות לחכות (עד 600)", H_WRITE_FILE = "הקובץ שייכתב", P_TEXT_IN = "טקסט",
                H_WRITE_TEXT = "מה לכתוב; \\n — שורה חדשה", P_APPEND = "להוסיף בסוף", H_APPEND = "בלי זה — הטקסט מחליף את מה שבקובץ",
                P_DEEP = "עמוק", H_DEEP = "ריק — רענון מהיר; עמוק — בדיקה מלאה; מלא — בנייה מחדש (/קוראים:N — כמה קוראים לכל כונן)", H_REPORT = "הקובץ שאליו נכתב היומן",
                WAIT_USAGE = "צורה: המתן <שניות> (עד 600)", REPORT_USAGE = "צורה: דוח \"<קובץ>\"", REFRESH_USAGE = "צורה: רענן · רענן עמוק · רענן מלא [/קוראים:N]",
                BACKUP_USAGE = "צורה: גבה (בלי פרמטרים; אחרי \"אופן\" — של אותו אופן)", WRITE_USAGE = "צורה: כתוב \"<קובץ>\" \"<טקסט>\" [/הוסף]",
                WRITE_IS_FOLDER = "זו תיקייה — אי אפשר לכתוב לתוכה כקובץ",
                EXPECT_NEXT = "השורה הבאה צריכה להיכשל", EXPECT_NOTHING = "\"ודא שגיאה\" — אין אחריה שורה לבדוק",
                EXPECT_OK_RUN = "✅ נכשלה, כצפוי", EXPECT_BAD = "❌ הייתה צריכה להיכשל — והצליחה",
                NOTE_NEEDS_LIST = "צריך לכתוב לאיזו רשימה: צור רעיון \"<טקסט>\" \"<רשימה>\"", VERIFY_NOT_CONTAINS = "אינו מכיל";
        public static String waitFor(int n) { return "המתנה של " + n + " שניות"; }
        public static String reportTo(String p) { return "היומן ייכתב גם לקובץ: " + p; }
        public static String reportWritten(String p) { return "היומן נכתב לקובץ: " + p; }
        public static String reportFailed(String why) { return "⚠ היומן לא נכתב לקובץ: " + why; }
        public static String refreshWill(boolean deep) { return deep ? "רענון עמוק של האינדקס" : "רענון של האינדקס"; }
        public static String refreshed(int n) { return "האינדקס רוענן · " + n + " שינויים"; }
        public static final String REBUILD_WILL = "בנייה מחדש של האינדקס (כל הכוננים)";   // מ0.43
        public static String rebuilt(int files, int dirs) { return "האינדקס נבנה מחדש · " + files + " קבצים · " + dirs + " תיקיות"; }
        /** מ0.40: הרענון המהיר שכבר בהכנה — כדי שהשורות שאחריו יראו קבצים חדשים. */
        public static String refreshedInPlan(int n) { return " (כבר בהכנה: " + n + " שינויים — השורות שאחריו רואות קבצים חדשים)"; }
        public static String backupWill(String mode) { return "גיבוי ידני של המסד של האופן \"" + mode + "\""; }
        public static String backedUp(String mode, String names) { return "נעשה גיבוי של האופן \"" + mode + "\": " + names; }
        public static String writeNoFolder(String p) { return "אין תיקייה: " + p; }
        public static String writeWill(String name, int chars, boolean add) {
            return (add ? "יתווספו ל\"" : "ייכתב הקובץ \"") + name + "\" · " + chars + " תווים";
        }
        public static String written(String name, int chars, boolean added) {
            return (added ? "נוספו ל\"" : "נכתב הקובץ \"") + name + "\" · " + chars + " תווים";
        }
        public static String expectOk(String why) { return "✅ נכשלה, כצפוי — " + why; }
        public static String noteAdded(boolean title, String text, String list) {
            return (title ? "כותרת" : "רעיון") + " \"" + text + "\" — נכנס ל\"" + list + "\"";
        }
        public static String verifyFilesIn(int n) { return "קבצים בתיקייה: " + n; }
        public static String verifyContains(String t) { return "מכיל \"" + t + "\""; }
        public static final String W_CONT = "כמה כישלונות מותרים לפני שהקובץ נעצר. בלי השורה הזו — הכישלון הראשון עוצר. המשך — אחד; המשך (5) — חמישה; המשך () — בלי עצירה",
                H_CONT = "(מספר) — כמה כישלונות; () — בלי עצירה; ריק — אחד", P_CONT = "כמה", CONT_USAGE = "צורה: המשך · המשך (5) · המשך ()",
                CONT_ALWAYS = "הקובץ לא ייעצר בגלל כישלונות", NOT_RUN_TITLE = "הקובץ לא רץ — יש בו שגיאה";
        public static String contUpTo(int n) { return n == 1 ? "הקובץ ימשיך אחרי כישלון אחד, וייעצר בשני" : "הקובץ ימשיך אחרי " + n + " כישלונות, וייעצר בבא אחריהם"; }
        public static String notRun(int n) {
            return "שום שורה לא רצה: יש " + n + " שורות שגיאה — יותר מהמותר. לתקן אותן, או להוסיף בראש הקובץ שורת \"המשך\" (המשך — כישלון אחד; המשך (5) — חמישה; המשך () — בלי עצירה)";
        }
        public static final String NO_EXPORT_FILES = "אין קבצים בתיקיית \"יצוא\"";
        public static String stoppedAt(String line, int fails, int allow) {
            return "⛔ נעצר לפני שורה " + line + ": " + fails + " כישלונות, מותר " + allow + ". מה שכבר בוצע — נשאר; השורות מכאן והלאה לא רצו";
        }

        public static final String N_MODE = "אופן", N_CALL = "כלול",  // תחביר
                W_MODE = "מכאן והלאה — הפקודות על המסד של אופן אחר (רק לקובץ הזה; המסך לא מתחלף)",
                H_MODE = "שם אופן עבודה שיש לו מסד", MODE_USAGE = "צורה: אופן \"שם האופן\"",
                W_CALL = "השורות של קובץ פקודות אחר — במקום השורה הזו (כמו CALL); עד 5 בתוך 5",
                H_CALL = "קובץ פקודות (מסלול, או שם בתיקייה של הקובץ הזה)",
                CALL_FAILED = "כלול — הקובץ לא נמצא, או שהוא כבר בתוך עצמו (מעגל), או יותר מ־5 בתוך 5";

        public static String noMode(String m, java.util.List<String> have) {
            StringBuilder sb = new StringBuilder();
            for (String h : have) sb.append(sb.length() > 0 ? ", " : "").append('"').append(h).append('"');
            return "אין מסד לאופן \"" + m + "\" — יש: " + sb + " (השורות שאחרי זה במסד — לא ירוצו)";
        }

        public static String modeNow(String m) {
            return "מכאן והלאה — הפקודות במסד של האופן \"" + m + "\"";
        }

        public static String called(String name, int lines) {
            return "▸ כלול: " + name + " (" + lines + " שורות)";
        }

        /** מ0.43: מה שמזהה את שורת הסיכום של "ודא" (Batch.logLine). */
        public static final String VERIFY_SUM_MARK = " ודא: ";

        public static String logLines(int n) {
            return n + " שורות בדוח";
        }

        public static String verifySummary(int ok, int bad) {
            return (bad == 0 ? "✅" : "❌") + " ודא: " + ok + " עברו · " + bad + " נכשלו";
        }

        public static String verifyWill(String what) {
            return "יבדוק: " + what;
        }

        public static String verifyResult(boolean ok, String what, String found) {
            return (ok ? "✅ " : "❌ ") + what + (ok ? "" : " — בפועל: " + found);
        }

        // מ0.44 — קטע
        public static final String N_SECTION = "קטע",  // תחביר
                W_SECTION = "כותרת בדוח של ההרצה, במקום שלה (\"▸ שם\") — מחלקת דוח ארוך לחלקים. לא משנה דבר",
                H_SECTION = "השם שיופיע בדוח", SECTION_USAGE = "צורה: קטע \"<שם>\"";

        public static String section(String name) {
            return "▸ " + name;
        }

        // מ0.45 — מחק אופן, הסל, ודא תקין, רענן מלא /קוראים:
        // מ0.45+ (המשתמש: "והיה דיבור לשלב אותה בהסר"; claude/bitul-ofen.md): "בטל אופן" — אופן הוא אוסף רשימות, כמו "בטל ידנית".
        // "מחק אופן" (כך נבנה במ0.45) — עדיין נקרא
        public static final String N_DROP_MODE = "בטל אופן", N_DROP_MODE_OLD = "מחק אופן", N_UNTRASH = "שחזר מהסל", N_PURGE = "רוקן סל",  // תחביר
                W_DROP_MODE = "מבטל אופן עבודה: המסד שלו (אם יש בו משהו — גיבוי לפני; ממנו אפשר לשחזר: \"צור אופן\" באותו שם, ואז הגיבויים), "
                        + "ותיקיית ההגדרות שלו — לסל. קבצים בכונן — לא נוגע. לא את האופן הפתוח במסך, ולא את \"שמור לתוכנה\"",
                H_DROP_MODE = "שם האופן", DROP_MODE_USAGE = "צורה: בטל אופן \"<שם>\"",
                W_UNTRASH = "מחזיר מהסל למקום המקורי את מה שנמחק מהמסלול הזה (האחרון שנמחק). שם תפוס — \"(משוחזר)\". לא מחזיר לרשימות",
                H_UNTRASH = "המסלול המקורי — איפה שהיה לפני המחיקה", UNTRASH_USAGE = "צורה: שחזר מהסל \"<מסלול מקורי>\"",
                W_PURGE = "מחיקה לצמיתות מהסל: מה שנמחק מהמסלול הזה (או מתוך התיקייה הזו). בלי מסלול — כל הסל. אין דרך חזרה",
                H_PURGE = "המסלול המקורי; ריק — כל הסל", PURGE_USAGE = "צורה: רוקן סל · רוקן סל \"<מסלול מקורי>\"",
                PURGE_ALL_WILL = "ריקון כל הסל — מחיקה לצמיתות, אין דרך חזרה",
                W_OK_SINCE = "תקין", OK_SINCE_WHAT = "כל השורות מתחילת הקטע רצו בלי כישלון",   // תחביר
                SW_IN_TRASH = "/בסל:", W_IN_TRASH = "בסל",   // תחביר
                SW_READERS = "/קוראים:", READERS_USAGE = "צורה: רענן מלא /קוראים:N (1 עד 16)";   // תחביר

        public static String dropModeWill(String n) { return "יבוטל האופן \"" + n + "\" (המסד שלו — אם יש בו משהו, גיבוי לפני; תיקיית ההגדרות שלו — לסל)"; }
        public static String dropModeNone(String n) { return "אין אופן \"" + n + "\" — אין מה לבטל"; }
        public static String dropModeOpen(String n) { return "האופן \"" + n + "\" פתוח עכשיו — לא בוטל (לעבור לאופן אחר, ואז)"; }
        public static String dropModeSystem(String n) { return "\"" + n + "\" הוא של התוכנה — לא בוטל"; }
        public static String dropModeDone(String n, boolean kept, boolean folder) {
            return "בוטל האופן \"" + n + "\"" + (kept ? " · גיבוי נשמר (\"צור אופן\" באותו שם — ואז הגיבויים)" : " (היה ריק)")
                    + (folder ? " · תיקיית ההגדרות שלו — בסל" : "");
        }
        public static String dropModeFailed(String n, String why) { return "האופן \"" + n + "\" לא בוטל: " + why; }
        public static final String DROP_MODE_NO_BACKUP = "לא נשמר גיבוי — ולכן לא בוטל (הפרטים ביומן)",
                DROP_MODE_FOLDER_KEPT = " · ⚠ תיקיית ההגדרות שלו לא עברה לסל (הפרטים ביומן)";

        public static String untrashWill(String name, boolean now) {
            return "יוחזר מהסל: \"" + name + "\"" + (now ? "" : " (עכשיו אינו בסל — ייבדק בהרצה)");
        }
        public static String untrashed(String name, String renamed) {
            return "הוחזר מהסל: \"" + name + "\"" + (renamed != null ? " · השם היה תפוס — \"" + renamed + "\"" : "");
        }
        public static String notInTrash(String name) { return "אין בסל דבר שנמחק מ\"" + name + "\""; }
        public static String purgeWill(String name) { return "מחיקה לצמיתות מהסל: מה שנמחק מ\"" + name + "\" — אין דרך חזרה"; }
        public static String purged(int n, int failed) {
            return "נמחקו לצמיתות מהסל: " + n + (failed > 0 ? " · " + failed + " לא נמחקו" : "");
        }

        public static String verifyInTrash(String name, int n) {
            return "\"" + name + "\" בסל" + (n >= 0 ? ": " + n : "");
        }
        public static String trashGot(int n) { return "בסל: " + n; }
        public static String okSinceGot(int bad) { return bad + " שורות נכשלו"; }
        public static String verifyMode(String n, boolean want) { return "האופן \"" + n + "\" " + (want ? "קיים" : "חסר"); }
        public static String readersNote(int n) { return " · " + n + " קוראים לכל כונן"; }

        // מ0.42 — ב8: רשימה ששינתה שם — קובצי הפקודות שפונים אליה
        public static final String FOLLOW_FAILED = " · ⚠ קובצי הפקודות שפונים לרשימה לא עודכנו (הפרטים ביומן)";

        public static String followed(java.util.List<String> names) {
            StringBuilder sb = new StringBuilder();
            for (String n : names) sb.append(sb.length() > 0 ? ", " : "").append(n);
            return names.size() == 1 ? " · עודכן קובץ פקודות אחד שפונה לרשימה: " + sb : " · עודכנו " + names.size() + " קובצי פקודות שפונים לרשימה: " + sb;
        }

        // מ0.42 — ודא מסד · ודא … באינדקס · צור אופן
        public static final String VERIFY_DB_WHAT = "המסד — המנוע מול ה־SQLite של המכשיר, ותקינות המסד של כל אופן";

        public static String verifyDbResult(boolean ok, String engine, int good, java.util.List<String> bad) {
            return (ok ? "✅ " : "❌ ") + VERIFY_DB_WHAT + " — המנוע: " + engine + " · מסדים תקינים: " + good
                    + (bad.isEmpty() ? "" : " · לא תקינים: " + bad);
        }

        public static String verifyInIndex(String name) {
            return "\"" + name + "\" באינדקס";
        }

        public static String indexGot(boolean in, boolean onDisk) {
            return (in ? "באינדקס" : "לא באינדקס") + " · בכונן: " + (onDisk ? "קיים" : "לא נמצא") + (onDisk && !in ? " (כדאי \"רענן\")" : "");
        }

        public static String modeBadName(String n) {
            return "שם אופן לא תקין: \"" + n + "\" (בלי : * ? \" < > |)";
        }

        public static String modeExists(String n) {
            return "האופן \"" + n + "\" כבר קיים — בלי שינוי";
        }

        public static String modeWill(String n) {
            return "ייווצר אופן חדש: \"" + n + "\" (מסד ריק עם בית)";
        }

        public static String modeCreated(String n) {
            return "נוצר אופן: \"" + n + "\"";
        }

        public static String modeFailed(String n) {
            return "האופן \"" + n + "\" לא נוצר (אם היו לו גיבויים — לשחזר ממסך הגיבויים). הפרטים ביומן";
        }

        public static final String EDIT = "✎ עריכה", ICON = "צלמית",
                FILE_NO_EDIT = "קובץ — אין מה לערוך כאן; שינוי שם הקובץ עצמו — במסכי הקבצים";

        public static String saved(String name) {
            return "נשמר: \"" + name + "\"";
        }

        public static final String REMOVE_HERE = "🗑 הסרה מכאן", REMOVE_YES = "להסיר";

        public static String removeQuestionN(int n) {
            return n + " פריטים — יוצאים מהרשימה הזו. רשימה שלא נשארת באף מקום — נמחקת עם מה שרק שלה; "
                    + "לפני כן — גיבוי של המסד. הקבצים בכונן לא נוגעים.";
        }

        public static String items(int n) {
            return n + " פריטים";
        }

        public static String homeContent(int n) {
            return "כל מה שבבית (" + n + ")";
        }

        public static String notThere(String label) {
            return "אין רשימה " + label + " — אין מה למחוק, בלי שינוי";
        }

        public static String inDb(String text) {
            return "במסד: " + text;
        }

        public static String beforeFailed(String why) {
            return "הגיבוי לפני הפעולה לא הצליח — לא בוצע: " + why;
        }
    }

    /** 1.14.0: הלוח הווירטואלי — היום DbClip (עד מ0.29 — VClip) */
    public static final class VClipW {
        private VClipW() { }

        public static final String COPY = "🔗 קישור", MOVE = "✂ העברה", DUPLICATE = "📋 העתקה", DUPLICATE_SHORT = "העתקה", COPY_SHORT = "קישור",
                SETTINGS = "⚙ הגדרות",
                PASTED = "הודבקו: ", SKIPPED = " · לא הודבקו: ";
    }

    /** מ0.24: הלוח של המסד (DbClip, DbListScreen) */
    public static final class DbClipW {
        private DbClipW() { }

        public static final String IN_CLIP = "📋 בלוח: ", FROM_DISK = " מהכונן", GONE = "המקור כבר לא קיים",
                ADD_ON = "➕ צירוף ✓", ADD_OFF = "➕ צירוף",
                VIEW_TITLE = "בלוח · נגיעה — הוצאה מהלוח", LINE_COPY = " — להעתקה", LINE_MOVE = " — להעברה",
                DISK_LINE = "📂 מהכונן: ",
                FOLDERS_HOW = "תיקיות מהכונן — איך להכניס לרשימה?",
                ALL_FILES = "📸 רק הקבצים — כולל כל מה שבתתי־התיקיות, בלי המבנה",
                WITH_TREE = "📸 הקבצים עם כל המבנה — תת־רשימה לכל תיקייה",
                ADD_ON_TOAST = "מעכשיו \"העתקה\" ו\"העברה\" מוסיפות ללוח",
                ADD_OFF_TOAST = "מעכשיו \"העתקה\" ו\"העברה\" מחליפות את מה שבלוח",
                ADDING = "מדביק…";

        public static String marked(int n, boolean add) {
            return (add ? "צורפו ללוח: " : "סומנו ללוח: ") + n + " · נווט והדבק";
        }

        public static String otherMode(String m) {
            return "הלוח מהאופן \"" + m + "\" — בין אופנים: בפקודה (COPY \"אופן:…\")";
        }
    }

    /** 1.16.1: תפריט הפקודות האחד (CmdMenu) */
    public static final class CmdM {
        private CmdM() { }

        public static final String TITLE = "⭐ פקודות · נגיעה — הרצה · ארוכה — עריכה",
                FOLDER = "📂 פתיחת תיקייה במנהל הקבצים…", NEW = "➕ פקודה חדשה…",
                ARRANGE = "✎ סידור התפריט (הרשימה)…", SAVE_AS = "שמירה בשם — פקודות המערכת לא משתנות",
                MINE_SUFFIX = " — שלי";
    }

    /** 1.13.1: "פקודה / קיצור דרך" (Shortcuts) */
    public static final class Short {
        private Short() { }

        public static final String NAME_HINT = "שם";
    }

    /** 1.13.0: כלי "פקודה" (FnCommand) */
    public static final class CmdTool {
        private CmdTool() { }

        public static final String TITLE = "פקודה";
        public static final String WHICH = "איזה קובץ פקודות?";
    }

    /** מ0.1: מנוע המסד — מסך המסד (DbScreen), הגיבויים, הייבוא, האבחון. (הבנייה השנייה — Flavor — יצאה במ0.29: יש בנייה אחת.) */
    public static final class Db {
        public static final String MODE_NEW = "➕ אופן חדש…", MODE_NAME = "שם האופן",
                EXAMPLES_RESTORE = "↺ שחזור הדוגמאות", EXAMPLES_RESTORE_GO = "שחזור",
                EXAMPLES_RESTORE_WHAT = "רשימות הדוגמאות באופן \"דוגמאות\" חוזרות למצב ההתחלתי. מה שהוספת להן — יוצא מהן. \"הדוגמאות שלי\", \"שמור לתוכנה\" ורשימות אחרות שהוספת לאופן — נשארות.",
                EXAMPLES_RESTORE_BACK = "לפני השחזור נשמר גיבוי של המסד (הגדרות ← גיבויים)";
        public static String examplesRestored(int n) { return "הדוגמאות שוחזרו · הוחלפו " + n + " רשימות"; }
        private Db() { }

        // מ0.23: בורר הרשימות של המסד
        public static final String PICK_NOT_HOME = "בוחרים רשימה שבתוך הבית",
                NO_DB_HERE = "אין מסד לאופן הזה";

        public static String addedTo(int added, int already, String list) {
            return "נוספו ל\"" + list + "\": " + added + (already > 0 ? " · כבר היו בה: " + already : "") + " · הקבצים עצמם לא זזו";
        }

        public static final String APP_NAME_M = "מנהל קבצים וירטואלי · מ";
        public static final String TITLE = "מסד נתונים";
        /** מ0.9: המראה המוכר. */
        public static final String LIST_GONE = "הרשימה הזו כבר לא קיימת במסד",
                FAMILIAR = "🏠 בית", ADD_IN_LIST = "הוספת קבצים — מתוך רשימה", NO_HOME = "אין בית במסד הזה";

        /** מ0.8: קבצים מהכונן לרשימה. */
        public static final String ADD_FROM_FOLDER = "📂 קבצים מתיקייה…", ADD_FROM_SEARCH = "🔍 קבצים מחיפוש…",
                FOLDER_NO_FILES = "אין קבצים בתיקייה הזו (תתי־תיקיות ונסתרים לא מוצגים)",
                SEARCH_TITLE = "חיפוש קבצים להוספה", SEARCH_HINT = "שם או תנאי, כמו ברשימה חכמה (למשל: סוג:ספרים)",
                SEARCHING = "מחפש…", SEARCH_NONE = "לא נמצאו קבצים",
                ADD_CHOSEN = "הוספה", ADD_ALL = "הוספת הכול", NOTHING_CHOSEN = "לא נבחר דבר";

        public static String filesAdded(int added, int already) {
            return "נוספו " + added + (already > 0 ? " · " + already + " כבר היו ברשימה" : "");
        }

        public static String firstOf(int shown, int total) {
            return "(" + shown + " ראשונים מתוך " + total + ")";
        }
        /** מ0.6: שם רשימת הבית במסד (בתצוגה — עם 🏠). */
        public static final String HOME_NAME = "בית", HOME_MARK = "🏠 ";
        public static final String NOTICE = "המסד של האופן: כל הרשימות שלו. גיבוי — לפני מחיקת רשימה ולפני שחזור, וביציאה אחרי שינוי; בתיקייה \"גיבוי מסד\" שבתיקיית התוכנה. לכל אופן מסד משלו. רשימה חכמה שומרת רק את התנאי — הקבצים נמצאים בכל פתיחה מהאינדקס.";
        public static final String NEED_ACCESS = "כדי לשמור את מונה המזהים בתיקיית הנתונים, התוכנה צריכה הרשאת גישה לכל הקבצים.";
        public static final String ALL_LISTS = "כל הרשימות";
        public static final String CRUMB_SEP = " › ";
        public static final String HINT = "נגיעה: פתיחה · נגיעה ארוכה: עוד פעולות";
        public static final String LIST_NAME = "שם הרשימה";
        public static final String NOTE_TEXT = "הרעיון";
        public static final String NEW_LIST_TITLE = "רשימה חדשה";
        public static final String KIND_NOTE = "רעיון";
        public static final String KIND_GROUP = "חבורה";
        public static final String GROUP_EMPTY = "חבורה ריקה";
        public static final String TRASH = "🗑 סל המחזור";

        public static String groupLabel(String head, int count) {
            return "◆ " + head + " (" + count + ")";
        }
        public static final String UNASSIGNED = "📥 לא משויכים";
        public static final String SMART_NO_COND = "אין תנאי — הרשימה לא מוצאת כלום. אפשר להגדיר אותו בעריכת התנאי.";
        public static final String INDEX_NOT_READY = "האינדקס עוד נבנה — התוצאות יופיעו כשיסתיים. אפשר לחזור בעוד רגע.";

        public static String smartNote(String note) {
            return "⚠ " + note;
        }

        public static final String MODE_PICK = "אופן עבודה (לכל אופן מסד משלו)";

        public static String modeButton(String mode) {
            return "אופן עבודה: " + mode + " ▾";
        }

        public static String modeLine(String name, boolean active) {
            String n = name.equals(Names.RESERVED_ROOT) || name.equals(DbCmd.TEST_MODE) ? "⚙ " + name + " · של התוכנה" : name;   // מ0.41
            return active ? n + " (הפעיל)" : n;
        }

        public static String status(int lists, long last, String code, String info) {
            return Words.lists(lists) + " · מספר זיהוי אחרון " + last + " · קוד מערכת " + code + "\n" + info;
        }

        public static String openFailed(String why) {
            return "פתיחת המסד נכשלה: " + why;
        }

        public static String failed(String why) {
            return "הפעולה נכשלה: " + why;
        }

        public static final String EMPTY_TITLE = "המסד ריק", EMPTY_RESTORE = "לשחזר", EMPTY_NOT_NOW = "לא עכשיו";

        public static String emptyRestore(String mode, String when) {
            return "המסד של האופן \"" + mode + "\" ריק (אולי אחרי התקנה מחדש), ונמצא גיבוי תקין מ־" + when
                    + ". לשחזר ממנו? לפני ההחלפה יוצג מה יש בו.";
        }
        public static final String BACK_TO_LIST = "↩ חזרה לרשימה", ADDING_FILES = "הוספת קבצים לרשימה. אחרי הבחירה — חוזרים אליה.";
        public static final String COPY_REPORT = "📋 העתקה";
        public static final String REPORT_COPIED = "הדוח הועתק. הדביקו אותו לי בשיחה.";

        // ---------- מ0.5: גיבוי ----------

        public static final String BACKUPS = "💾 גיבויים";
        public static final String BACKUPS_EMPTY_SUB = "עוד אין גיבויים — הגיבוי היומי נעשה כשפותחים את המסך";
        public static final String BACKUP_NOW = "💾 גיבוי עכשיו (עותק וגם טקסט)";
        public static final String BACKUPS_CHECKING = "בודק את הגיבויים מול הרישום…";
        public static final String BACKUP_SAVING = "שומר גיבוי…";
        public static final String RESTORE_BUILDING = "בונה מסד מהגיבוי ובודק אותו. המסד הנוכחי עוד לא משתנה…";
        public static final String RESTORE_YES = "♻️ שחזור";
        public static final String RESTORE_FROM_BACKUP = "♻️ שחזור מגיבוי";
        public static final String RESTORED_DB = "המסד שוחזר מהגיבוי.";
        public static final String BACKUP_BAD = "הגיבוי הזה פגום או אינו שלם — אי אפשר לשחזר ממנו.";
        public static final String BACKUP_CHANGED_WARN = "שימו לב: הקובץ הזה שונה מאז שנרשם. התוכנה תבדוק אותו לפני השחזור.";
        public static final String KEPT_DAMAGED_INFO = "זה המסד הפגום, כפי שנשמר לפני שחזור. ממנו לא משחזרים — הוא נשמר רק כדי שלא יאבד דבר.";
        public static final String DAMAGED_HINT = "אפשר לשחזר מגיבוי: \"♻️ שחזור מגיבוי\". המסד הפגום יישמר קודם, כמו שהוא, בתיקיית הגיבויים.";

        public static String backupsSub(int n, String newest) {
            return "עותק יומי כשיש שינוי, ולפני פעולות מוחקות · " + n + " קבצים · האחרון " + newest;
        }

        public static String backupsTitle(String mode) {
            return "גיבויים · " + mode;
        }

        /** שם סוג הגיבוי לתצוגה. */
        public static String backupKind(String kind, String op, boolean text) {
            String k = kind.equals(Backups.DAILY) ? "יומי" : kind.equals(Backups.BEFORE) ? "לפני " + op : kind.equals(Backups.MANUAL) ? "ידני"
                    : kind.equals(Backups.TEXT) ? "טקסט שבועי" : "המסד הפגום";
            return k + (text && !kind.equals(Backups.TEXT) ? " · טקסט" : "");
        }

        /** שורה ברשימת הגיבויים: מצב · סוג · מתי · גודל. state — Backups.OK וכו'. */
        public static String backupEntry(int state, String kind, String when, long bytes) {
            String mark = state == Backups.OK || state == Backups.OK_UNRECORDED ? "✅" : state == Backups.CHANGED ? "⚠️" : state == Backups.BAD ? "❌" : "🗂";
            String note = state == Backups.CHANGED ? " · שונה מאז שנרשם" : state == Backups.BAD ? " · פגום" : state == Backups.OK_UNRECORDED ? " · לא רשום" : "";
            return mark + " " + kind + " · " + when + " · " + kb(bytes) + note;
        }

        public static String kb(long bytes) {
            return ((bytes + 1023) / 1024) + " ק״ב";
        }

        public static String backupSaved(String names, String dir) {
            return "נשמר גיבוי:\n" + names + "\nבתיקייה: " + dir;
        }

        public static String restoreQuestion(String label, int bl, long bi, int cl, long ci, boolean damaged) {
            return "לשחזר מהגיבוי \"" + label + "\"?\n\nבגיבוי: " + Words.lists(bl) + " · " + Words.items((int) bi)
                    + (damaged ? "\nעכשיו: המסד פגום. הוא יישמר כמו שהוא בתיקיית הגיבויים (\"פגום\")."
                    : "\nעכשיו: " + Words.lists(cl) + " · " + Words.items((int) ci)
                    + "\n\nהמסד הנוכחי יישמר קודם בתיקיית הגיבויים (\"לפני שחזור\").")
                    + "\nמספרי זיהוי שכבר ניתנו לא יחזרו.";
        }

        public static String restoreFailed(String why) {
            return "השחזור לא נעשה: " + why + "\nהמסד לא השתנה.";
        }

        public static String backupBeforeFailed(String why) {
            return "הגיבוי לפני הפעולה נכשל: " + why;
        }

        public static String importBackupFailed(String why) {
            return "הגיבוי לפני ההעתקה נכשל, ולכן לא הועתק דבר: " + why;
        }

        public static String damaged(String mode, String why) {
            return "❌ המסד של האופן \"" + mode + "\" פגום, ולכן לא נפתח ולא נכתב בו דבר.\n(" + why + ")";
        }

        public static final String DUMP_TITLE = "מנהל קבצים וירטואלי · מ — גיבוי טקסט של מסד. הפורמט מתועד בקוד (DbDump). לא לערוך ביד: שינוי של תו אחד פוסל את הקובץ.";
        public static final String RECORD_HEAD = "# רישום הגיבויים: שם · בתים · SHA-256 של הקובץ · חתימת התוכן · מתי נעשה. לא לערוך ביד.";

        public static String selfTestOk(int passed, long ms, String info) {
            return "תקין — עברו " + passed + " בדיקות, בלי כישלון.\nזמן: " + ms + " מ״ש\nSQLite: " + info;
        }

        public static String selfTestBad(int passed, java.util.List<String> failed, String info) {
            StringBuilder sb = new StringBuilder("נמצאו כשלונות: " + failed.size() + " (עברו " + passed + ")\n");
            for (String f : failed) sb.append("✗ ").append(f).append('\n');
            return sb.append("SQLite: ").append(info).toString();
        }
    }

    /** מ0.38 — המעבדה (Lab, LabScreen). */
    public static final class Lab {
        // מ0.40: "מעבדה" ← "תוספים" (המשתמש: "הוא כבר לא אמור להיקרא מעבדה, אלא תוסף עורך אינטראקטיבי")
        public static final String UNNAMED = "תוסף",
                TITLE = "🧩 תוספים", EMPTY = "אין עדיין תוספים. עמוד של תוסף עובר לתיקיית התוספים במריץ שלו (למשל \"עורך אינטראקטיבי.vbat\")",
                VERSIONS = "גרסאות", NOT_FOUND = "אין תוסף בשם הזה", ONLY_COMMANDS = "תוסף כותב רק קובצי פקודות",
                NO_FILES_HERE = "אין כאן קבצים", EXISTS_OLD = "יש כבר קובץ בשם הזה — לא נשמר. בגרסה הבאה של העמוד: להחליף, או שם אחר", PICK_FILE = "בחירת קובץ", RUN_FILE = "הרצה מתוסף";
        /** מ0.40: "הוסף תוסף" ו"החלף תוסף" ירדו (כלל יסודי: פונקציות קיימות קודם). */
        public static final String GONE = "הפקודה ירדה במ0.40: עמודי התוספים עוברים לתיקיית התוספים ב\"העבר\" (כמו ב\"עורך אינטראקטיבי.vbat\" החדש), "
                + "\"פתח תוסף\" פותח את החדש, וגרסה אחרת — \"פתח תוסף \"<שם>\" \"<גרסה>\"\" או \"גרסאות\" במסך התוסף";
        public static String noVersion(String exp, String ver) { return "בתוסף \"" + exp + "\" אין גרסה \"" + ver + "\""; }
        public static String version(String ver) { return "גרסה " + ver; }
        /** מ0.40 — המשתמש: "ואם יש יותר חדש — הוא מזוהה בתוסף". */
        public static String higher(String ver) { return "⬆ יש גרסה " + ver; }
        public static String row(String exp, String ver, int n) { return exp + " · גרסה " + ver + (n > 1 ? " (" + n + " גרסאות)" : ""); }
        /** שורה בבחירת הגרסה: הגרסה, ומתי הקובץ הגיע (שתי גרסאות באותו מספר — לפי התאריך). */
        public static String versionRow(String ver, String when, boolean open) { return "גרסה " + ver + " · " + when + (open ? " ✓" : ""); }
    }
}
