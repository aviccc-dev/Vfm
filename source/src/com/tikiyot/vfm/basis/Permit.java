package com.tikiyot.vfm;

/**
 * ההיתר של שכבת הניהול (מבנה 76).
 * המשתמש (15.11.2026): "פעולה מנסה לעקוף את שכבת הניהול. פעולה זו לא חוקית ואסורה. כל פעולה חייבת לעבור דרך שכבת הניהול."
 * וההגדרה שלו לשכבה הדקה: "כל צעד עובר דרכה ומחייב את אישורה והכוונתה, כל שכבה צריכה להיות מוכרת על ידה אחרת לא תעבוד."
 *
 * איך זה עובד: שכבת הניהול (lev/Manager) — ורק היא — פותחת היתר לזמן ההרצה שהיא מאשרת. כל פעולה שמשנה משהו שואלת
 * כאן לפני שהיא מתחילה (check). אין היתר —
 *   פעולה שכבר עברה כולה לשכבת הניהול (STRICT) — מסורבת, בנוסח של המשתמש, ונרשמת ביומן;
 *   פעולה שעוד במעבר — נעשית, ונרשמת ביומן פעם אחת לכל מקום שעוקף ("מי עוד עוקף" — הרשימה של מה שנשאר להעביר).
 * כשפעולה עוברת לשכבת הניהול, מוסיפים אותה ל־STRICT; בסוף המעבר — כולן שם.
 *
 * הקובץ יושב בבסיס כדי שכל קומה תוכל לשאול; לפתוח היתר מותר רק למנהל (שומר בבדיקות המבנה).
 */
public final class Permit {

    private Permit() { }

    /** שמות הפעולות — כמו שהם נאמרים בסירוב וביומן. */
    public static final String DELETE = Words.PermitW.DELETE, TRANSFER = Words.PermitW.TRANSFER, RENAME = Words.PermitW.RENAME,
            MAKE_FOLDER = Words.PermitW.MAKE_FOLDER, RESTORE = Words.PermitW.RESTORE, FOREVER = Words.PermitW.FOREVER,
            WRITE = Words.PermitW.WRITE, ZIP = Words.PermitW.ZIP,
            /** מבנה 84: כתיבה למסד (רשימות, אופנים). מבנה 126: המעבר הסתיים — בלי היתר היא מסורבת, כמו פעולות הכונן. */
            DB = Words.PermitW.DB;

    /** הפעולות שכבר עוברות כולן דרך שכבת הניהול: בלי היתר — מסורבות. */
    private static final java.util.Set<String> STRICT = new java.util.HashSet<String>(java.util.Arrays.asList(DELETE, TRANSFER, RENAME, MAKE_FOLDER, WRITE, RESTORE, FOREVER, ZIP, DB));   // מבנה 77: גם העתקה והעברה · 78: שינוי שם · 79: תיקייה חדשה · 80: כתיבת קובץ · 81: הסל — כל פעולות הכונן

    private static final ThreadLocal<int[]> DEPTH = new ThreadLocal<int[]>() {
        @Override protected int[] initialValue() { return new int[1]; }
    };

    /** מה שכבר נרשם ביומן — פעם אחת לכל מקום שעוקף, עד שהתוכנה נסגרת. */
    private static final java.util.Set<String> warned = java.util.Collections.synchronizedSet(new java.util.HashSet<String>());

    /** רק שכבת הניהול. ההיתר — של התהליכון הזה, עד close. */
    public static void open() {
        DEPTH.get()[0]++;
    }

    public static void close() {
        int[] d = DEPTH.get();
        if (d[0] > 0) d[0]--;
    }

    // ---------- מבנה 124: פעולות שהתוכנה עושה לבד (לא פעולה של המשתמש ולא קובץ פקודות) ----------
    // המשתמש: "כל שכבה צריכה להיות מוכרת על ידה אחרת לא תעבוד". שכבת הניהול רושמת, בשמן, את הפעולות האוטומטיות המותרות
    // (Manager — המקום היחיד שרושם; שומר בבדיקות המבנה). פעולה רשומה מקבלת היתר לזמן שהיא רצה; שאינה רשומה — בלי היתר:
    // נרשמת ביומן, ומה שהיא מנסה לשנות נבדק כמו כל פעולה שעוקפת.

    public static final String SYS_MENUS = Words.PermitW.SYS_MENUS, SYS_MINE = Words.PermitW.SYS_MINE, SYS_FOLLOW = Words.PermitW.SYS_FOLLOW,
            SYS_TOOLS = Words.PermitW.SYS_TOOLS, SYS_EXIT = Words.PermitW.SYS_EXIT, SYS_START = Words.PermitW.SYS_START,
            SYS_TOP_ROW = Words.PermitW.SYS_TOP_ROW, SYS_DEV = Words.PermitW.SYS_DEV;

    private static final java.util.Set<String> SYSTEM = java.util.Collections.synchronizedSet(new java.util.HashSet<String>());

    /** רק שכבת הניהול: רושמת פעולה של המערכת, בשמה. */
    public static void allowSystem(String name) {
        SYSTEM.add(name);
    }

    public static boolean isSystem(String name) {
        return SYSTEM.contains(name);
    }

    /**
     * תחילת פעולה של המערכת. רשומה — ההיתר נפתח בתהליכון הזה (true), ומי שקרא סוגר ב־systemClose, ב־finally. לא רשומה —
     * false: בלי היתר, ונרשם ביומן (פעם אחת לכל שם).
     */
    public static boolean systemOpen(String name) {
        if (SYSTEM.contains(name)) {
            open();
            return true;
        }
        if (warned.add("system|" + name)) Log.i("⚠ " + Words.PermitW.notRegistered(name));
        return false;
    }

    public static void systemClose(boolean opened) {
        if (opened) close();
    }

    public static boolean held() {
        return DEPTH.get()[0] > 0;
    }

    public static boolean strict(String action) {
        return STRICT.contains(action);
    }

    /**
     * נשאל בתחילת כל פעולה שמשנה. null — מותר להמשיך. אחרת — הסירוב (הפעולה לא נעשית, וזה מה שנאמר למי שביקש).
     */
    public static String check(String action) {
        return check(action, NO_WRAPPERS);
    }

    private static final String[] NO_WRAPPERS = {};

    /**
     * מבנה 84: כמו check, ו־wrappers — מחלקות של עטיפות שדרכן מגיעים לפעולה ("DbListCmd", "DbVerbsUi"): ביומן נרשם מי שקרא
     * להן, לא הן עצמן — כך כל מקום שעוקף מופיע בשמו, והיומן הוא הרשימה של מה שנשאר להעביר.
     */
    public static String check(String action, String... wrappers) {
        if (held()) return null;
        String from = caller(wrappers);
        if (STRICT.contains(action)) {
            Log.i("⚠ " + Words.PermitW.bypass(action, from) + " — " + Words.PermitW.REFUSED);
            return Words.PermitW.refused(action);
        }
        if (warned.add(action + "|" + from)) Log.i("⚠ " + Words.PermitW.bypass(action, from) + " — " + Words.PermitW.STILL_MOVING);
        return null;
    }

    /** מי ביקש: המקום הראשון בשרשרת הקריאות שאינו הפעולה עצמה ואינו כאן ("PhysicalUi.askRename"). */
    private static String caller(String[] wrappers) {
        StackTraceElement[] st = new Throwable().getStackTrace();
        String first = null;
        for (StackTraceElement e : st) {
            String c = e.getClassName();
            c = c.substring(c.lastIndexOf('.') + 1);
            int inner = c.indexOf('$');
            boolean nested = inner > 0;
            if (nested) c = c.substring(0, inner);
            if (c.equals("Permit")) continue;
            if (first == null) { first = c; continue; }           // הפעולה עצמה (Physical…)
            if (c.equals(first) || java.util.Arrays.asList(wrappers).contains(c)) continue;
            // בתוך מחלקה פנימית השם של הפונקציה הוא "run" או "onClick" — מספר השורה אומר איזו
            return c + "." + e.getMethodName() + (nested && e.getLineNumber() > 0 ? ":" + e.getLineNumber() : "");
        }
        return first == null ? "?" : first;
    }
}
