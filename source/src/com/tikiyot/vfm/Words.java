package com.tikiyot.vfm;

// מילון המונחים וניסוחי הפעולות. כל מסך לוקח מכאן, כדי שהניסוח לא יסטה.
// שני עולמות: בכונן (תיקייה, קובץ) וברשימות (רשימה, פריט).

public final class Words {

    private Words() { }

    // ---------- מונחים ----------
    public static final String LISTS = "רשימות";
    public static final String LIST = "רשימה";
    public static final String SMART_LIST = "רשימה חכמה";
    public static final String MANUAL_LISTS = "רשימות ידניות";
    public static final String NEW_LIST = "רשימה חדשה";
    /** בורר הרשימות (106). */
    /** 173: פריטי התפריט הקבועים בכל מסך רשימה. */
    public static final String FULL_SHORT = "נתונים מלאים / מקוצרים", SORT = "מיון";
    public static final String ALL_LISTS = "כל הרשימות", TOOL_LISTS = "הרשימות של הכלים",
            PICK_HERE = "לכאן", PICK_UP = "למעלה", VIEW_FOLDERS = "תיקיות", VIEW_TREE = "עץ",
            SEARCH_LIST = "חיפוש רשימה…", OTHER_TOOL_LIST = "לרשימה בכלי אחר…",
            NEW_FOLDER = "תיקייה חדשה", RENAME_ON_DRIVE = "שינוי שם בכונן", SEARCH_FOLDER = "חיפוש תיקייה…",
            NO_LISTS_HERE = "אין כאן רשימות", NO_FOLDERS_HERE = "אין כאן תיקיות",
            INSIDE_LIST = "בתוך רשימה זו", INSIDE_FOLDER = "בתוך תיקייה זו",
            NOT_LIST_TARGET = "אי אפשר לבחור את הרשימה הזו כיעד";
    public static final String ROOT = "השורש";

    // ---------- כותרות עולם ----------

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
            SAVE = "שמירה", SAVED = "נשמר", PASTE_HERE = "הדבקה כאן";

    // ---------- הודעות חוזרות (131) ----------
    public static final String NOTHING_SELECTED = "לא נבחר דבר";
    public static final String NEED_NAME = "צריך שם", NAME_OPTIONAL = "שם (לא חובה)";
    public static final String NAME_TAKEN_AT_DEST = "שם תפוס ביעד";
    public static final String LIST_NOT_FOUND = "הרשימה לא נמצאה";
    public static final String FILE_NOT_FOUND = "הקובץ לא נמצא", FOLDER_NOT_FOUND = "התיקייה לא נמצאה";
    public static final String FILE_NOT_AT_PATH = "הקובץ לא נמצא במסלול שלו";
    public static final String NO_TOUCH_FILES = "לא נוגע בקבצים";
    public static final String BACKUP_SAVED = "נשמר גיבוי לפני המחיקה, בתיקייה .גיבויים";
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

        /** לפי הסדר של com.tikiyot.vfm.SearchTabs.KINDS. */
        public static final String[] TABS = {SMART_LIST, "כפולים"};   // 154: "רשימה חכמה" — מול "רשימה ידנית"
        /** 197: בפס הקטן שבראש הטופס. */
        public static final String[] SHORT = {"חכמה", "כפולים"};
        public static final String KIND_L = "סוג";
    }

    // ---------- העתקה בין אופני עבודה (162) ----------
    // ---------- רשימה בתוך רשימה (161) ----------
    public static final class Inner {
        private Inner() { }

        public static final String ADD = "➕ רשימה בתוך הרשימה", COMMON = "מתבנית…",
                MANUAL = "רשימה ידנית קיימת…", NEW = "חדשה…", REMOVE = "הסרה מהרשימה",
                GONE = "כבר לא קיימת — אפשר להסיר אותה", NONE_COMMON = "אין עדיין תבניות",
                ALREADY = "כבר נמצאת כאן";

        public static String removeWhat(boolean local) {
            return local ? "הרשימה החכמה מוסרת (היא שייכת רק לכאן) — לסל" : "רק הקישור מוסר; הרשימה עצמה נשארת";
        }
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
                LATER = "אחר כך", PASTE_PICK = "הדבקה…", PICK_TITLE = "להדביק לתיקייה",
                ADD_FOLDER = "➕ הוספת התיקייה הזו לרשימה", SHOW_HIDDEN = "הצגת מוסתרים", HIDE_HIDDEN = "הסתרת מוסתרים";
    }

    // ---------- החיפוש האחד (153) ----------
    public static final class Find {
        private Find() { }

        public static final String SETTINGS = "⚙ הגדרות", SAVE = "💾 שמירה", SAVE_AS = Named.SAVE_AS,
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
        private Named() { }

        // 154: "הגדרות נפוצות" (המשתמש: "צריך לשנות את השם — במקום 'שמירה בשם' — הגדרות נפוצות")
        public static final String TITLE = "תבניות רשימה חכמה", SAVE_AS = "💾 שמירה כתבנית…",
                SAVE_AS_TITLE = "שמירה כתבנית", NAME_HINT = "שם, למשל: ספרים מהחודש",
                GONE = "התבנית שהכלי מקשר אליה נמחקה מהקובץ הראשי";

        public static String exists(String name) {
            return "כבר יש תבנית בשם \"" + name + "\"";
        }

        public static String overwrite(int tools) {
            return "ההגדרות שלה יוחלפו בהגדרות שבטופס" + (tools > 0 ? ", בכל " + (tools == 1 ? "הכלי שמקשר" : tools + " הכלים שמקשרים") + " אליה" : "");
        }

        public static final String UPDATE = "לעדכן", SAVED_AS = "נשמרה כתבנית";
    }

    // ---------- קובצי פקודות (117א) ----------
    public static final class Cmd {
        private Cmd() { }
        /** 1.13.0 */
        public static final String OPEN_BTN = "פתח…", PROGRAM_BTN = "צלמית";
        public static final String ALREADY_PROGRAM = "יש כבר כותרת (צלמית) בקובץ";
        public static final String HEADER_HELP = "כותרת: צלמית — כאריח בבית הקובץ נראה כתוכנה (צלמית, שם ו\"למה\"; במקום אחר — קובץ טקסט) · הדמיה — הכול רץ בכאילו, בלי לשנות דבר";


        /** מה כל פקודה עושה — מההגדרה האחת (1.22.5: Commands). */
        public static String what(String cmd) {
            Commands.Def d = Commands.get(cmd);
            return d == null ? "" : d.what;
        }

        /** איך כותבים אותה — נבנה מההגדרה האחת (1.22.5: עם המתגים עצמם, במקום "[מתגים]"). */
        public static String usage(String cmd) {
            Commands.Def d = Commands.get(cmd);
            return d == null ? "" : d.usage();
        }

        /** "COPY — העתקת קבצים.  COPY <מקור> <יעד> [מתגים]". */
        public static String help(String cmd) {
            String w = what(cmd);
            return w.isEmpty() ? "" : cmd + " — " + w + ".  " + usage(cmd);
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
        /** 1.20.0 */
        public static final String NO_COMPARE = "אין השוואה (COMP) בשורה קודמת";
        public static final String COMPARE_BROKEN = "שורה קודמת על אותה השוואה נכשלה — לא ממשיכים עליה";
        /** 1.21.0 */
        /** 1.22.0 */
        public static final String LIST_ONLY_COMP = "/רשימה:<שם> — רק ב־COMP, עם שם רשימה";
        public static final String NEEDS_INDEX = "רשימה כמקור צריכה את האינדקס — הוא עוד לא מוכן. לנסות שוב בעוד רגע";
        public static final String LIST_NO_MOVE = "מקור שהוא רשימה — רק COPY (MOVE היה מוציא קבצים מהמקומות שלהם)";

        public static String listNotFound(String n) {
            return "אין רשימה בשם: " + n + " (גם לא בתוך \"שמור לתוכנה\"). אפשר לכתוב את הדרך המלאה, למשל: א / ב";
        }

        public static String smartSkipped(int n) {
            return " · ⚠ " + n + " רשימות חכמות שברשימה לא נמצאו (נמחקו?) — לא נכללו";
        }

        public static String listsSeen(String mode, String top, String reserved) {
            return "\n   באופן \"" + mode + "\" — רשימות: " + (top.isEmpty() ? "אין" : top)
                    + (reserved == null ? " · אין \"שמור לתוכנה\"" : " · בתוך \"שמור לתוכנה\": " + (reserved.isEmpty() ? "אין" : reserved));
        }

        public static String listAmbiguous(String n, String ways) {
            return "יש כמה רשימות בשם " + n + ": " + ways + " — לכתוב את הדרך המלאה";
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

        public static String line(int line, String out) {
            return "שורה " + line + ": " + out;
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
        public static final String EDIT = "✎ עריכה",
                RUN = "הרצה", NOTHING_TO_DO = "אין בקובץ פקודות לביצוע.",
                NO_CHANGE = "לא ישתנה דבר בכונן", CHANGES = "ישתנו קבצים בכונן",
                DELETES_TO_TRASH = " · DEL ו-RD מעבירים לסל המחזור",
                UNDO = "מחיקה — מסל המחזור; העברה והעתקה — הרשימות עוקבות";

        public static String errorLines(int n) {
            return n + " שורות עם שגיאה — ידלגו עליהן · ";
        }

        public static String preview(String file) {
            return "📜 " + file + " — תצוגה מקדימה";
        }

        public static String done(String file) {
            return "📜 " + file + " — בוצע";
        }

        public static String runTitle(String file) {
            return "הרצת " + file;
        }

        public static String running(String file) {
            return "מריץ את " + file + "…";
        }

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
        public static final String SAVE_BTN = "💾 שמירה", PREVIEW_AND_RUN = "▶ תצוגה מקדימה והרצה", PICK = "📁 בחירה",
                SHARE_BTN = "📤 שיתוף", ASK_SAVE = "לשמור את השינויים?",   // 203
                DONT_SAVE = "בלי לשמור", REPLACE_NOTE = " (הקודם לסל)";

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

        public static String fileNamed(String name) {
            return "הקובץ " + name;
        }
    }

    // ---------- אופני עבודה (105) ----------
    public static final class WorkModes {
        private WorkModes() { }

        public static final String SHARE = "📤 שיתוף ההגדרות";               // 158

        // 156: הדוגמאות
        public static final String EXAMPLES_AGAIN = "📚 להחזיר את הדוגמאות", EXAMPLES_BACK = "הדוגמאות הוחזרו",
                EXAMPLES_RESET = "חזרה לברירת מחדל — הדוגמאות", EXAMPLES_RESET_OK = "להחזיר",
                EXAMPLES_RESET_WHAT = "כל אופן הדוגמאות חוזר למצבו הראשון — רשימות, כלים, כפתורים והגדרות (חוץ מ\"הדוגמאות שלי\") — וגם תיקיית התרגול של הפקודות",
                EXAMPLES_RESET_UNDO = "אפשר לשחזר את הקודם מסל המחזור";

        public static final String EXISTS = "כבר יש אופן עבודה בשם הזה", NOT_FOUND = "האופן לא נמצא",
                ACTIVE = "האופן פעיל. עוברים לאופן אחר וממחקים משם", SWITCH_FAILED = "המעבר נכשל";

        public static String moved(String what, String to) {
            return "אופני עבודה: הועבר " + what + " אל \"" + to + "\"";
        }
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

        public static final String FROM_DATE = "מ־";
        public static final String FOUND_ANY = "מצא משהו ";
        public static final String RAN = "רצה ";
        public static final String FOUND_ANY_FEM = ", מצאה משהו ";
        public static final String FILES_LINE = " קבצים\n";

        public static final String DAILY = "יומית", WEEKLY = "שבועית", SINCE = "מה השתנה מאז",
                TITLE = "תקופת ניסיון — מה נמצא מעבר לבדיקה השוטפת", BUTTON = "תקופת ניסיון — מה נמצא",
                EMPTY = "עוד לא נרשם דבר. הבדיקה היומית רצה פעם ביום, והבנייה השבועית — פעם בשבוע.",
                FILE = "תקופת ניסיון.txt",                                         // בכונן
                DAILY_ABOUT = "בדיקה מלאה של מה שהמאגר של אנדרואיד לא מכיר (מוסתרים, .nomedia)",
                WEEKLY_ABOUT = "בנייה מההתחלה, והשוואה לאינדקס שהיה",
                SINCE_ABOUT = "קבצים שנערכו במקומם, שבדיקת התאריכים לא תפסה",
                NOT_SUPPORTED = "באנדרואיד הזה אין \"מה השתנה מאז\" — במקומו בנייה מלאה פעם ביום";

        public static String found(int n) {
            return "נמצאו " + n;
        }

        public static String runs(int n) {
            return n == 1 ? "פעם אחת" : n + " פעמים";
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
        public static final String READERS_BUTTON = "בניית האינדקס — במקביל";
        public static final String DELETED_BUTTON = "קובץ שנמחק — מה קורה ברשימות";
        public static final String EXCLUDED_BUTTON = "תיקיות שלא נכנסות לאינדקס — באופן העבודה הזה";
        public static final String QUICK_REFRESH = "רענון מהיר";
        public static final String EDIT_INI = "עריכת קבצי ההגדרות";
        public static final String CHECK_PATH = "בדיקת מסלול באינדקס";
        public static final String CLEAN_ORPHANS = "ניקוי רשימות ללא כלי";
        public static final String ERROR_LOG = "יומן תקלות";
        public static final String DEEP_REFRESH = "רענון מעמיק — גודל ותאריך של כל הקבצים";
        public static final String REBUILD_WHAT = "סריקה מלאה של כל הכוננים. בכרטיס זה עשוי לקחת זמן";
        public static final String REBUILD_UNDO = "בינתיים החיפוש ממשיך לעבוד על האינדקס הקיים";
        public static final String BUILD = "בנייה";
        public static final String NOW = "כעת: ";
        public static final String LAST_BUILD_LINE = "\nבנייה אחרונה: ";
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
        public static final String READER = " קורא";
        public static final String READERS = " קוראים";
        public static final String PER_DRIVE = " לכל כונן";
        public static final String FASTEST = " (הכי מהיר)";
        public static final String BUILD_IS = "בניית האינדקס: ";
        public static final String LAST_SUFFIX = " · אחרונה: ";
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
        public static final String LAST_FULL_LINE = "\nבנייה מלאה אחרונה: ";
        public static final String LAST_DAILY_LINE = "\nבדיקה יומית אחרונה: ";
        public static final String NOT_YET = "עוד לא";
        public static final String LATEST = "\n\nהאחרונות:\n";
        public static final String FILE_LINE = "\n(הקובץ: ";
        public static final String CHECK_PATH_TITLE = "בדיקת מסלול";
        public static final String CHECK_RESULT = "תוצאת הבדיקה";
        public static final String ON_DRIVE = "בכונן: ";
        public static final String EXISTS = "✓ קיים";
        public static final String NOT_FOUND = "✗ לא נמצא";
        public static final String IN_INDEX_LINE = "\nבאינדקס: ";
        public static final String FOUND = "✓ נמצא";
        public static final String TRY_QUICK = "\n\nכדאי ללחוץ \"רענון מהיר\".";
        public static final String NO_ORPHANS = "אין רשימות ללא כלי";
        public static final String DELETE_ORPHANS = "מחיקת רשימות ללא כלי";
        public static final String LISTS_COLON = " רשימות: ";
        public static final String ITEMS_OLD_ROOT = " פריטים מהשורש הישן";
        public static final String DELETE = "מחיקה";
        public static final String BEFORE_CLEAN = "לפני ניקוי רשימות ללא כלי";
        public static final String DELETED = "נמחקו";
        public static final String LOG_EMPTY = "היומן ריק";
        public static final String LOG = "יומן";
        public static final String SHARE = "שיתוף";
        public static final String SHARE_LOG = "שיתוף היומן";
        public static final String CLEAR = "ניקוי";
        public static final String LOG_CLEARED = "היומן נוקה";
    }

    /** המסך הראשי (Home): אופני עבודה, כלים, הוספה */
    public static final class Main {
        private Main() { }

        public static final String APP_NAME = "מנהל קבצים וירטואלי";
        public static final String NEED_ACCESS = "כדי לנהל קבצים בכל המכשיר, התוכנה צריכה הרשאת גישה לכל הקבצים.";
        public static final String GRANT = "מתן הרשאה";
        public static final String NEW_MODE_MENU = "➕ אופן עבודה חדש…";
        public static final String MODE = "אופן עבודה";
        public static final String LONG_PRESS_MODE = " · נגיעה ארוכה: שינוי שם ומחיקה";
        public static final String RENAME = "שינוי שם";
        public static final String DELETE_MODE = "מחיקת האופן";
        public static final String ACTIVE = " (הפעיל)";
        public static final String RENAME_MODE = "שינוי שם האופן";
        public static final String NAME = "שם";
        public static final String RENAMED = "השם שונה";
        public static final String MODE_ACTIVE_CANT = "האופן פעיל. עוברים לאופן אחר וממחקים משם";
        public static final String DELETE_MODE_TITLE = "מחיקת אופן העבודה \"";
        public static final String NO_TOUCH_DRIVE = "לא נוגע בקבצים בכונן";
        public static final String LISTS_AND = " רשימות ו־";
        public static final String ITEMS_LEAVE = " פריטים, וגם הכלים שבבית, ההגדרות והאינדקס של האופן, יוצאו ממנו";
        public static final String MODE_DELETED = "האופן נמחק";
        public static final String NEW_MODE = "אופן עבודה חדש";
        public static final String NEW_MODE_HINT = "שם, למשל: בדיקות מקיפות";
        public static final String START_WITH = "\": במה להתחיל?";
        public static final String EMPTY_MODE = "ריק — בלי רשימות ובלי כלים";
        public static final String COPY_OF = "העתק של \"";
        public static final String STRUCTURE = "מבנה";
        public static final String SETTINGS = "הגדרות";
        public static final String THE_FILE = "הקובץ \"";
        public static final String MISSING = "\" חסר";
        public static final String ALL_LISTS_IN_IT = "כל הרשימות נמצאות בו";
        public static final String ALL_TILES_IN_IT = "כל הכלים שבמסך הבית נמצאים בו";
        public static final String BACKUP_FROM = "יש גיבוי מ-";
        public static final String RESTORE_FROM_IT = ". לשחזר ממנו?";
        public static final String NEW_FILE_STARTS_EMPTY = "\"קובץ חדש\": יתחיל קובץ ריק";
        public static final String RESTORE = "שחזור";
        public static final String RESTORED_FROM_BACKUP = "שוחזר מהגיבוי";
        public static final String NEW_FILE = "קובץ חדש";
        public static final String MODE_LINE = "אופן עבודה: ";
        public static final String FILES_IN_INDEX = " קבצים באינדקס · עודכן ";
        public static final String BUILT_IN = "\nנבנה ב־";   // 1.10.0: שורה משלו — בלי "·" בודדת בסוף שורה
        public static final String ADD = "הוספה";
        public static final String UNKNOWN_TOOL = "כלי עבודה לא מוכר: ";
        public static final String WHAT_TO_ADD = "מה להוסיף";
        /** 1.13.1: ➕ בבית — ארבע שורות (הוכרע 16.10). */
        public static final String ADD_MANUAL = "📋  רשימה ידנית", ADD_SMART = "✨  רשימה חכמה",   // 1.16.1: אותו תפריט
                ADD_CMD = "▶  פקודה";                                                                // גם בכפתור
        public static final String OPEN = "פתיחה";
        public static final String ICON = "צלמית";
        public static final String MOVE_RIGHT = "הזזה ימינה";
        public static final String MOVE_LEFT = "הזזה שמאלה";
        public static final String DELETE = "מחיקה";
        public static final String DESC = "תיאור";
    }

    /** הרשימה הידנית (VirtualScreen) */
    public static final class Manual {

        public static final String SETTINGS_HERE = "⚙ הגדרות הרשימה הזו";   // 1.16.0
        public static final String DESC = "תיאור";
        public static final String DESC_HINT = "למשל: מה יש כאן ולמה";
        private Manual() { }

        /** 1.13.0 */
        public static final String DESC_WORD = "יורד", ASC_WORD = "עולה";
        public static final String NEW_REF = "↪ קישור לרשימה קיימת — בלי להעתיק אותה";
        public static final String REF = "↪ ", REF_GONE = "↪ הרשימה לא נמצאה: ";
        public static final String REF_SELF = "אי אפשר לקשר לרשימה הזו עצמה או לרשימה שמעליה";
        public static final String ORDER = "הזזה", ORDER_DOTS = "↕ הזזה…", ORDER_SHORT = "הזזה";
        public static final String ORDER_UP = "▲ למעלה", ORDER_DOWN = "▼ למטה", ORDER_TOP = "⤒ לראש", ORDER_BOTTOM = "⤓ לסוף";
        public static final String ORDER_SORTED = "הסדר נשמר. התצוגה ממוינת אחרת — מיון \"הסדר שלי\" כדי לראות אותו";

        public static final String MANUAL_LIST = "רשימה ידנית";
        /** 1.10.0 (פריט 8): "רשימה חדשה" — ידנית או חכמה. */
        public static final String NEW_MANUAL = "📋 רשימה ידנית — אני בוחר מה נכנס",
                NEW_SMART = "✨ רשימה חכמה — לפי הגדרה, מתעדכנת לבד";
        public static final String BAD_LINE = "שורה לא מובנת";
        public static final String CANT_READ_TREE = "לא הצלחתי לקרוא את מבנה.txt. שינויים לא יישמרו עד שהקובץ ייקרא. כדאי לצאת ולהיכנס שוב.";
        public static final String LISTS_DOT = " רשימות · ";
        public static final String ITEMS_SUFFIX = " פריטים";
        public static final String LISTS_SEP = " רשימות   ·   ";
        public static final String NEW_ONE = "החדש: ";
        public static final String UP = " (למעלה)";
        public static final String NO_LISTS_YET = "אין עדיין רשימות.\nרשימה מצביעה על קבצים בלי להזיז אותם.";
        public static final String LIST_EMPTY = "הרשימה ריקה.";
        public static final String GO_TO_LEVEL = "מעבר לרמה";
        public static final String REMOVE_LINE = "הסרת השורה";
        public static final String EDIT_BY_HAND = "עריכה ידנית";
        public static final String LISTS_AND = " רשימות ו-";
        public static final String REMOVE_TITLE = "הסרה מהרשימה: ";
        public static final String REMOVES_ONLY = "מסיר מהרשימה בלבד";

        /** 94: כמה יוצא כשמסירים רשימות שלמות. */
        public static String removesLists(int lists, int subLists, int innerItems, int items) {
            return "יוסרו " + lists + (lists == 1 ? " רשימה" : " רשימות")
                    + (subLists > 0 ? ", ובתוכן " + subLists + " רשימות־משנה" : "")
                    + (innerItems > 0 ? (subLists > 0 ? " ו־" : ", ובתוכן ") + Files.count(innerItems) + " פריטים" : "")
                    + (items > 0 ? "; ועוד " + Files.count(items) + " פריטים מהרשימה הזו" : "")
                    + ". הקבצים עצמם נשארים בכונן.";
        }
        public static final String CAN_ADD_AGAIN = "אפשר להוסיף שוב";
        public static final String REMOVE = "הסרה";
        public static final String BEFORE_DELETING = "לפני מחיקת ";
        public static final String LISTS_SUFFIX = " רשימות";
        public static final String MEDIA_OUT = "המדיה הוצאה או פגומה — הקובץ יחזור כשהכונן יהיה זמין";
        public static final String OPEN_TRASH = "פתיחת סל המחזור";
        public static final String SEARCH_AGAIN = "חיפוש מחדש";
        public static final String LIST_NAME = "שם הרשימה";
        public static final String CURRENT_PLACE = " (המיקום הנוכחי)";
        public static final String SEARCHING_SIZE = "מחפש לפי גודל…";
        public static final String SEARCHING_FILE = "מחפש את הקובץ…";
        public static final String BY_NAME_SIZE = "לפי שם וגודל";
        /** 190 */
        public static final String BY_SIG = "לפי התוכן (חתימה)";
        public static final String BY_NAME = "לפי שם בלבד";
        public static final String BY_SIZE = "לפי גודל בלבד";
        public static final String NO_SUCH_NAME = "לא נמצא קובץ בשם הזה";
        public static final String SEARCH_SIZE_ASK = "לחפש קובץ באותו גודל בדיוק, גם אם שמו שונה?";
        public static final String SEARCH_BY_SIZE = "חיפוש לפי גודל";
        public static final String NOT_ON_DEVICE = "הקובץ לא נמצא במכשיר";
        public static final String UPDATE_PATH_TITLE = "עדכון המסלול בכל הרשימות";
        public static final String FOUND = "נמצא ";
        public static final String BACKUP_BEFORE_CHANGE = "נשמר גיבוי לפני השינוי, בתיקייה .גיבויים";
        public static final String UPDATE = "עדכון";
        public static final String MATCHES = " התאמות";
        public static final String PATH_UPDATED_IN = "המסלול עודכן ב-";
        public static final String NOTHING_TO_UPDATE = "לא נמצא פריט לעדכון";
        public static final String EDIT_TREE_TITLE = "עריכה ידנית של מבנה.txt";
        public static final String REFRESH = "רענון";
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
        public static final String TAP_ADDS = "נגיעה בתוצאה מוסיפה אותה לרשימה · ";
        public static final String REFRESHING = "מרענן את האינדקס…";
        public static final String INDEX_UP_TO_DATE = "האינדקס מעודכן";
        public static final String FOLDERS_REFRESHED = " תיקיות רועננו";
        public static final String IN_LIST = "✓ ברשימה";
        public static final String LISTS_DOT = " רשימות · ";
        public static final String ADDED_TO_LIST = "נוסף לרשימה";
        public static final String ALREADY_IN_LIST = "כבר נמצא ברשימה";
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
        public static final String COPY_NOTE = "\nעותק שנבנה מהקובץ הראשי. שינוי כאן יימחק; לעריכה יש לערוך את \"";
        public static final String ID_LINES_NOTE = "\nשורות \"מזהה\" מקשרות כלים ורשימות חכמות, ואינן ניתנות לעריכה.";
        public static final String UNKNOWN_KEPT = " שורות שאינן מוכרות יישמרו כמות שהן";
        public static final String CHANGED_OUTSIDE = "הקובץ השתנה מבחוץ";
        public static final String RELOAD = "טעינה מחדש";
        public static final String OVERWRITE = "דריסה";
        public static final String SAVE_FAILED_LOG = "השמירה נכשלה. הפרטים ביומן";
        public static final String READ_ONLY_COPY = "עותק לקריאה בלבד";
        public static final String ID_NOT_EDITABLE = "שורת מזהה אינה ניתנת לעריכה";
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

        public static final String INSIDE = " בתוך";
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
        public static final String ITEMS_SUFFIX = " פריטים";
        public static final String ADDED_N = "נוספו ";
        public static final String TO_LIST = " לרשימה";
        public static final String ALREADY_THERE = "כבר היו ברשימה";
        public static final String LIST_CREATED = "נוצרה הרשימה \"";
        public static final String AND_FILES_ADDED = "\" ונוספו הקבצים";
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

    /** 1.10.0: מכלים בסל המחזור (BinBox) */
    public static final class Box {
        private Box() { }

        public static final String LIST = "רשימה";
        public static final String LISTS = "רשימות";
        public static final String SMART = "רשימה חכמה";
        public static final String TOOL = "כלי";
        public static final String MODE = "אופן עבודה";
        public static final String FROM_MODE = " · מהאופן ";
        public static final String IN_BIN = "אפשר לשחזר מסל המחזור";
        public static final String REMOVED = "הוסר מהתוכנה";
        public static final String HOW = "הוסר מהתוכנה — נגיעה ארוכה: שחזור, או מחיקה לצמיתות";
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
        public static final String RENAME = "שינוי שם";
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
        public static final String ADD_TO_ICON = "➕ הוספה ל";
        public static final String ADD_TO = "הוספה ל";
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
        public static final String PICK_LIST_FIRST = "בוחרים קודם רשימה";
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

    /** תפריט הכלי (ToolMenu) */
    public static final class ToolM {

        private ToolM() { }

        public static final String DELETE = "הסרה";
        public static final String DESC = "תיאור";
        public static final String NAME = "שם";
        public static final String TOOLS = " כלים", PICKED = " נבחרו";   // 1.17.1
        public static final String DELETE_LIST_OF = "הסרת הרשימה \"";
        public static final String DELETE_OF = "הסרת \"";
        public static final String DELETES_TOOL_AND_LIST = "הרשימה מוסרת — עם כל מה שבתוכה; הקישורים אליה נשברים";
        public static final String REMOVES_FROM_SCREEN = "הכלי מוסר מהמסך בלבד";
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

    /** כלי הרשימות (FnVirtual) */
    public static final class ListT {
        private ListT() { }

        public static final String THE_LIST = "הרשימה \"";
        public static final String NOT_FOUND_CREATED = "\" לא נמצאה, ונוצרה ריקה בשם הזה. אם שינית לה שם מחוץ לתוכנה — אפשר לבחור אותה מחדש בהגדרות הכלי.";
        public static final String BEFORE_DELETING = "לפני מחיקת ";
        public static final String TOOL_NAME_OPT = "שם הכלי (לא חובה)";
        public static final String EXISTING_LIST = "רשימה קיימת";
        public static final String NEW_LIST_NAME = "שם הרשימה החדשה (ריק = כשם הכלי)";
        public static final String PICK_LIST = "📋 בחירת רשימה…";
        public static final String NO_LIST_PICKED = "לא נבחרה רשימה";
        public static final String CHOOSE_LIST = "בחירת רשימה";
        public static final String TOOL_NAME = "שם הכלי";
        public static final String WHICH_LIST = "על איזו רשימה הכלי עובד";
        public static final String NEED_LIST = "צריך לבחור רשימה";
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

        public static final String ALREADY_IN_LIST = "כבר נמצא ברשימה \"";
        public static final String ADDED_N = "נוספו ";
        public static final String TO_LIST = " לרשימה \"";
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
        public static final String WORDS_IN_NAME = "מילים בשם";
        public static final String NO_SOURCES_ALL = "אין מקורות = כל הכוננים";
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

        public static String shared(String name, int tools) {
            return "הגדרה נפוצה \"" + name + "\"" + (tools > 1 ? " — גם ב־" + (tools - 1) + (tools - 1 == 1 ? " מקום נוסף" : " מקומות נוספים") : "")
                    + "; שינוי כאן ישנה בכל מקום שהיא.";
        }
    }

    /** שורת הסיכום של כלי (Stats) */
    public static final class StatsW {
        private StatsW() { }

        public static final String ZERO_ITEMS = "0 פריטים";
        public static final String LISTS_DOT = " רשימות · ";
        public static final String FILES = " קבצים";
        public static final String EMPTY = "ריק";
        public static final String FILES_DOT = " קבצים · ";
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

    /** בורר הרשימות (ListPicker) */
    public static final class ListP {
        private ListP() { }

        public static final String LIST_NAME = "שם הרשימה";
    }

    /** העתקה מאופן אחר — הבורר (CopySource) */
    public static final class CopyS {
        private CopyS() { }

        public static final String LISTS_DOT = " רשימות · ";
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
        public static final String CANT_READ_LOG = "לא ניתן לקרוא את היומן: ";
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

    /** 1.9.0: תיקייה חיה ברשימה */
    public static final class Live {
        private Live() { }

        public static final String UPDATING = "🔄 מתעדכנת";
        public static final String GONE = "⚠ התיקייה לא נמצאה בכונן";
        public static final String REMOVE = "הסרת התיקייה המתעדכנת מהרשימה";
        public static final String NO_SETTINGS = "לתיקייה מתעדכנת אין הגדרות — היא התיקייה עצמה";   // 1.17.1
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
        public static final String DRIVE_ROOT = "זה שורש הכונן — אין תיקייה מעל";
    }

    /** 1.9.0: שורת הכפתורים העליונה */
    public static final class Top {
        private Top() { }

        public static final String HOME = "בית", BACK = "חזרה", SEARCH = "חיפוש", COMMANDS = "פקודות",
                FILES = "קבצים פיזיים", SETTINGS = "הגדרות", ADD = "כפתור";
        public static final String EDIT = "✏ עריכה", RIGHT = "→ הזזה ימינה", LEFT = "← הזזה שמאלה",
                DELETE = "הסרת הכפתור", RESET = "↺ חזרה לברירת מחדל";
        public static final String RESET_ASK = "חזרה לברירת מחדל";
        public static final String RESET_WHAT = "רק הכפתורים — לא רשימות ולא קבצים · רק באופן העבודה הזה";
        public static final String WHAT = "מה הכפתור יעשה?";
        public static final String T_LINE = "⌨ שורת פקודה בכתב", T_MENU = "☰ תפריט — רשימה מ\"שמור לתוכנה\"";
        public static final String NAME = "שם", ICON = "סמל (אות או סימן אחד)", ACTION = "מה הכפתור עושה: ",
                CHANGE = "שינוי…", FIXED = " (קבוע — זה כפתור עוגן)";
        public static final String NO_SMART = "אין רשימות חכמות שמורות";
        public static final String NO_BATCH = "אין קובצי פקודות";
        public static final String LINE_HINT = "למשל: פתח רשימה חכמה אחרונים · מצא סוג:ספרים תאריך:שבוע · MOVE …";
        public static final String GONE = "היעד של הכפתור לא נמצא: ";
        public static final String ANCHOR = "כפתור עוגן — אפשר לשנות שם, סמל ומקום, אבל לא להסיר";
        public static final String UNKNOWN = "פעולה לא מוכרת בכפתור: ";
        /** 1.13.0 */
        public static final String VIRTUAL = "קבצים וירטואליים", TRASH = "סל המחזור";
        public static final String STALE = "השורות שונו במסך אחר — רועננו. אפשר לנסות שוב";
        public static final String SAVED_AS = "נשמר: ";

        /** 1.10.0: סוגי המסכים — שמות המקומות של השורות שלהם (גם בקובץ הכפתורים). */
        public static final String S_LIST = "רשימה ידנית", S_HOME = "מסך הבית",
                S_SEARCH = "חיפוש ורשימה חכמה", S_DISK = "קבצים פיזיים", S_TRASH = "סל המחזור", S_DUP = "כפולים",
                S_SETTINGS = "הגדרות", S_BATCH = "עורך הפקודות", S_ASIF = "תוצאת הדמיה", S_EDITOR = "עורך ההגדרות";
        /** 1.10.0: מה שהיה כפתורים קבועים בתחתית. */
        public static final String NEW_LIST = "רשימה", FROM_DRIVE = "מהכונן", FROM_SEARCH = "מחיפוש";
        public static final String MOVE_ROW = "↕ העברה לשורה אחרת", NEW_BUTTON = "➕ כפתור חדש", NEW_ROW = "➕ שורה חדשה",
                DELETE_ROW = "הסרת השורה";
        public static final String P_TOP = "למעלה · בכל המסכים", P_BOTTOM = "למטה · בכל המסכים",
                P_TOP_HERE = "למעלה · רק ב", P_BOTTOM_HERE = "למטה · רק ב";
        public static final String WHERE_ROW = "איפה השורה החדשה?", MAX_ROWS = "יש כבר 5 שורות במקום הזה",
                ROW_ADDED = "נוספה שורה — ➕ שבסופה מוסיף כפתורים", ROW = "שורה ",
                NO_OTHER_ROW = "אין שורה אחרת במסך הזה — אפשר להוסיף שורה (נגיעה ארוכה על ➕)";
        public static final String RESET_PLACE = "רק ", RESET_ALL = "כל הכפתורים בכל המסכים";
        public static final String T_OWN = "🧩 פעולה של ";

        public static String notHere(String name) {
            return "\"" + name + "\" — פעולה של מסך אחר, לא של המסך הזה";
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
        public static final String COLOR = "צבע (בעיצוב הנוכחי)";
        public static final String COLOR_HINT = "ירוק, סגול, כתום, אפור, כחול, אדום, או #0F6E56";
        public static final String ICON_IN = "צלמית (בעיצוב הנוכחי)";
        public static final String CHOSEN = "העיצוב: ";
        public static final String COPY_FAILED = "העתקת העיצוב נכשלה";
    }

    /** 1.13.0: פקודות הפתיחה בעברית (Verbs) */
    public static final class Verb {
        private Verb() { }
        public static final String MENU = "תפריט: ";   // 1.17.0

        public static final String SCREEN = "מסך ", LIST = "רשימה: ", SMART = "רשימה חכמה: ", FOLDER = "תיקייה: ",
                FIND = "מצא: ", BACK = "חזרה למסך הקודם", RUN = "הרצה: ";
        public static final String NOT_FOUND = "לא נמצא: ";

        public static String atEnd(String what) {
            return "ייפתח בסוף: " + what;
        }

        public static String unknownScreen(String s) {
            return "מסך לא מוכר: \"" + s + "\" (הבית, חיפוש, פקודות, קבצים פיזיים, קבצים וירטואליים, הגדרות, סל)";
        }

        public static String usage(String verb) {
            if (verb.equals("מצא")) return "שימוש: מצא <תנאי>, למשל: מצא סוג:ספרים תאריך:שבוע";
            if (verb.equals("הרץ")) return "שימוש: הרץ <קובץ פקודות>";
            if (verb.equals("הוסף")) return "שימוש: הוסף מהכונן · הוסף מחיפוש";
            if (verb.equals("צור")) return "שימוש: צור רשימה <שם> · צור רשימה חכמה \"שם\" <תנאי> · צור פקודה";   // 1.22.5: גם צור פקודה
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
                H_TAKEN = "מה עושים כשביעד כבר יש קובץ באותו שם. /Y — כמו להחליף",
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

        public static final String FILTER_HINT = "חיפוש פקודה: העתק או COPY", IN_FILE = "בקובץ", MORE = "עוד פקודות",
                TEMPLATES = "★ נפוצות", COMMANDS = "☰ פקודות", TPL_SYS = "של התוכנה", TPL_MINE = "שלי",
                ADVANCED = "מתקדם", SIMULATE = "▶ הדמיה", SIMULATING = "מחשב…", REMOVE = "הסרה", UP = "▲", DOWN = "▼",
                SAVE_TPL = "★ שמור כנפוצה", TPL_NAME = "שם לפקודה הנפוצה", TPL_SAVED = "נשמרה בנפוצות",
                MINE_SUFFIX = " (שלי)", TPL_EDIT = "✎ ערוך", TPL_EDIT_COPY = "✎ ערוך — עותק אצל \"שלי\" (המקור נשאר)",
                TPL_RENAME = "שנה שם", TPL_DELETE = "מחק", TPL_DELETE_FAILED = "לא נמחקה", TPL_NAME_BAD = "השם לא תקין או תפוס",
                TPL_HINT = "נגיעה — שימוש · נגיעה ארוכה — עריכה", TPL_THIS_LINE = "השורה שנבחרה", TPL_WHOLE_FILE = "כל הקובץ",
                NEW = "חדשה — עוד לא בקובץ", THE_LINE = "השורה בקובץ (אפשר גם לערוך כאן)", THE_FILE = "הקובץ הפתוח",
                NEED_COMPARE = "⚠ %רק-במקור% / %רק-ביעד% צריכים שורת COMP לפניהם",
                NOT_UNDERSTOOD = "שורה שהממשק לא מכיר — נשמרת כמו שהיא. אפשר לערוך אותה בקובץ הפתוח",
                ONE_OPEN = "⚠ רק פקודת פתיחה אחת נפתחת בסוף — האחרונה", NO_STEP = "השורה לא מבוצעת (הערה או כותרת)",
                TAG_HEADER = "כותרת", TAG_UNKNOWN = "?", NONE = "—", ON = "פעיל", PICK = "בחירה…", FILL = "השלמת התבנית",
                VARS = "משתנים", EMPTY_FILE = "הקובץ ריק — בחר פקודה מהרשימה, והיא תיכנס לכאן",
                ALREADY_AS_IF = "יש כבר הדמיה בראש הקובץ", ALREADY_MEASURE = "יש כבר מדידה בקובץ",
                NO_SMART = "אין רשימות חכמות", PICK_FIRST = "בחר שורה בתפריט קודם";

        /** 1.25.2: מה מבקשים במקום הפתוח. */
        public static String holeKind(int kind) {
            switch (kind) {
                case 1: return " — רשימה ידנית (קיימת, או שם חדש — תיווצר)";
                case 2: return " — רשימה חכמה";
                case 3: return " — מספר";
                default: return " — תיקייה";
            }
        }

        public static String lineN(int n) {
            return "שורה " + n + " בקובץ";
        }

        public static String insertAfter(int n) {
            return n <= 0 ? "＋ הכנס לקובץ" : "＋ הכנס לקובץ אחרי שורה " + n;
        }

        public static String fillFirst(String what) {
            return "מלא קודם: " + what;
        }

        public static String advanced(int n) {
            return "▾ " + ADVANCED + " (" + n + ")";
        }

        public static String lines(int n) {
            return n == 1 ? "שורה אחת" : n + " שורות";
        }
    }

    /** 1.13.0: שורת "למה" (Why) */
    public static final class WhyW {
        private WhyW() { }

        public static final String PREFIX = "💡 למה: ";
        public static final String KEY_LIST = "רשימה:", KEY_TOOL = "כלי:", KEY_SCREEN = "מסך:";
        public static final String FIELD = "למה (לא חובה)";
        public static final String HINT = "מה המטרה — כדי לזכור גם בעוד חודש";
        public static final String VIRTUAL_FILES = "מהאינדקס — מהיר, עם חיפוש וסינון. פעולה על קובץ (העברה, מחיקה) נעשית גם בכונן, עם אזהרה";
        public static final String DISK_FILES = "ישר מהכונן, כמו כל מנהל קבצים. כל פעולה נעשית בכונן, ואחר כך נשמרת באינדקס";
    }

    /** 1.15.0: הסבת הסביבות (Envs) */
    public static final class EnvsW {
        private EnvsW() { }

        public static final String BACKUP = "לפני הסבת הסביבות לרשימות — ",
                UNIFY = "לפני האיחוד (תיקיות מתעדכנות, הגדרות נפוצות) — ";   // 1.17.3
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
                WHAT_MEASURE = "כל הקובץ נמדד: זמן לכל שורה, סך הכול, והשורה האיטית. /יומן:\"קובץ\" — נשמר גם לקובץ, בכל ריצה.",
                N_LOOP = "חזור — לולאה", N_END = "סוף לולאה", N_MEASURE = "מדידה";

        public static String round(int r) {
            return " (סבב " + r + ")";
        }

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
                INTO_ITSELF = "אי אפשר להעתיק רשימה לתוך עצמה (או לתוך רשימה שבתוכה)",
                MOVE_TO_LIST = "העברה לרשימה — אין לה מובן: לרשימה נכנסים רק המסלולים, והקבצים נשארים במקומם. להוספה — COPY",
                MOVE_LATER = "העברה של רשימות — עדיין לא בפקודות; בינתיים — העתקה ואחריה מחיקה",
                HOME_ONLY_CONTENT = "\"*\" הוא הבית — כמקור רק עם \"תוכן\": תוכן ידנית \"*\"",
                HOME_FILES = "לבית אין קבצים משלו — אפשר לבחור רשימה",
                HOME_NO_REMOVE = "את הבית אי אפשר למחוק בפקודה",
                MD_SMART = "רשימה חכמה — יוצרים בטופס שלה; בפקודה — העתקה של חכמה קיימת",
                RD_FILES = "למחיקת הקבצים שברשימה: DEL קבצי ידנית \"שם\"",
                NO_FILTER = "סינון (/תאריך, /גודל) — עדיין לא חל על רשימות",
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

        public static String homeContent(int n) {
            return "כל מה שבבית (" + n + " כלים)";
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

        public static String removes(String what) {
            return what + " — יוצא מהרשימות (לסל); הקבצים לא נוגעים";
        }

        public static String removed(String what) {
            return what + " — יצא (לסל)";
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

        public static String dupSkipped(int n) {
            return " (רשימות כפולים לא נספרו: " + n + ")";
        }

        public static String nowIn(long lists, long items) {
            return " · עכשיו בה: " + lists + " רשימות, " + items + " פריטים";
        }

        public static String copied(int done, int skipped, String why, String to) {
            return "נכנסו ל" + to + ": " + done + (skipped > 0 ? " · לא נכנסו: " + skipped + (why != null ? " (" + why + ")" : "") : "");
        }
    }

    /** 1.14.0: הלוח הווירטואלי (VClip) */
    public static final class VClipW {
        private VClipW() { }

        public static final String TO_COPY = " — לקישור", TO_MOVE = " — להעברה", TO_DUPLICATE = " — להעתקה",
                FROM_MODE = " · מהאופן ", NOT_HERE = "אי אפשר להדביק את זה כאן",
                TOOL_NOT_IN_LIST = "את הכלי הזה אי אפשר להכניס לרשימה",
                COPY = "🔗 קישור", MOVE = "✂ העברה", DUPLICATE = "📋 העתקה", DUPLICATE_SHORT = "העתקה", COPY_SHORT = "קישור",
                SETTINGS = "⚙ הגדרות", ORIGINAL = "⚙ עריכת הרשימה המקורית", REMOVE = "הסרה",
                HINT = " · נווט והדבק",
                PASTED = "הודבקו: ", SKIPPED = " · לא הודבקו: ", FAILED = "ההעתקה לא הצליחה";
    }

    /** 1.16.1: תפריט הפקודות האחד (CmdMenu) */
    public static final class CmdM {
        private CmdM() { }

        public static final String TITLE = "⭐ פקודות · נגיעה — הרצה · ארוכה — עריכה",
                FOLDER = "📂 פתיחת תיקייה במנהל הקבצים…", NEW = "➕ פקודה חדשה…",
                ARRANGE = "✎ סידור התפריט (הרשימה)…", SAVE_AS = "שמירה בשם — פקודות המערכת לא משתנות",
                MINE_SUFFIX = " — שלי",
                FROM_EXAMPLES = "התפריט בא מ\"שמור לתוכנה\" שבאופן \"דוגמאות\" — לסדר אותו שם, או להעתיק את \"שמור לתוכנה\" לאופן הזה";
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
        public static final String MISSING = "הקובץ לא נמצא";

        public static String added(String name) {
            return "נוסף לבית: " + name;
        }
    }
}
