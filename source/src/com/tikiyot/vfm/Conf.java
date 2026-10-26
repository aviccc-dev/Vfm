package com.tikiyot.vfm;

// הגדרות כלליות בקובץ טקסט. אחראי: קריאה וכתיבה של "הגדרות כלליות.ini".
// כל הגדרה שאינה של כלי מסוים נשמרת כאן, ולא בזיכרון הפנימי של אנדרואיד.


import java.io.File;

public final class Conf {

    public static final String GENERAL = "כללי";  // תחביר
    public static final String SEARCH = "חיפוש";  // תחביר
    public static final String LIST_SORT = "מיון רשימות";  // תחביר

    public static final String FULL = "נתונים מלאים";  // תחביר
    public static final String DELETED = "קובץ שנמחק";  // תחביר
    public static final String DELETED_REMOVE = "הסר מהרשימות";  // תחביר
    public static final String DELETED_GREY = "השאר באפור";  // תחביר
    /** תיקיות שלא נכנסות לאינדקס, מופרדות ב"|" (76). */
    public static final String EXCLUDED = "לא לאנדקס";  // תחביר
    /**
     * 177: קוראים לכל כונן — בבנייה, בבדיקת התאריכים ובקריאה מחדש ("0" — כונן אחרי כונן, קורא אחד).
     * 1.7.73: שם חדש למפתח — הערכים שנשמרו בניסויים (16, 32, 64) לא נגררים; ברירת המחדל — 8.
     */
    public static final String READERS = "קוראים לכל כונן";  // תחביר
    /** 183: יעד קבוע ל"שליחה למכשיר" — חבילה/מחלקה; "" — לבחור בכל פעם. */
    public static final String SEND_TO = "שליחה למכשיר — יעד";  // תחביר
    /** מנהל הקבצים מוגבל לתיקיות אלה (76). ריק = כל הכוננים. */

    private static final String HEADER =
            "; הגדרות כלליות של התוכנה. אפשר לערוך ידנית.\n"  // תחביר
            + ";\n"
            + "; [כללי]\n"  // תחביר
            + ";   נתונים מלאים = כן / לא\n"  // תחביר
            + ";   קובץ שנמחק = הסר מהרשימות / השאר באפור\n"  // תחביר
            + ";   לא לאנדקס = מסלולי תיקיות, מופרדים ב|\n"  // תחביר
            + "; [חיפוש]          הגדרות חיפוש שנפתח שלא מתוך כלי (למשל מתוך רשימה)\n"  // תחביר
            + "; [מיון רשימות]    מיון לכל רשימה ידנית, לפי המסלול שלה\n"  // תחביר
            + "; [סוגי קבצים]     קבוצות הסוגים לרשימה חכמה ולחיפוש: שם = pdf|epub|… (אפשר לשנות, להוסיף ולמחוק)\n";  // תחביר

    private static Ini cache;

    private Conf() { }

    public static File file() {
        return new File(Paths.settings(), "הגדרות כלליות.ini");  // בכונן
    }

    private static synchronized Ini ini() {
        if (cache == null || cache.changedOutside() || !cache.file.exists() || !cache.file.equals(file())) {   // 163: גם אופן אחר
            Ini.createIfMissing(file(), HEADER);
            cache = Ini.load(file());
        }
        return cache;
    }

    public static synchronized String get(String section, String key, String def) {
        return ini().get(section, key, def);
    }

    public static boolean is(String section, String key, boolean def) {
        String v = get(section, key, def ? "כן" : "לא");  // תחביר
        return "כן".equals(v);  // תחביר
    }

    public static synchronized void set(String section, String key, String value) {
        Ini ini = ini();
        for (Ini.Line l : ini.entries(section))
            if (l.key.equals(key)) {
                if (value.equals(l.value)) return;
                ini.set(l, key, value);
                ini.saveQuiet();
                return;
            }
        ini.add(section, key, value);
        ini.saveQuiet();
    }

    /**
     * כל השורות של סעיף, לפי הסדר (מפתח, ערך). סעיף שאין בו שורה אחת — נכתב קודם עם ברירת המחדל (145).
     * מה שהמשתמש שינה או מחק בסעיף קיים — נשאר כמו שהוא.
     */
    public static synchronized java.util.List<String[]> section(String section, String[][] defaults) {
        Ini ini = ini();
        if (ini.entries(section).isEmpty() && defaults != null && defaults.length > 0) {
            for (String[] kv : defaults) ini.add(section, kv[0], kv[1]);
            ini.saveQuiet();
        }
        java.util.List<String[]> out = new java.util.ArrayList<String[]>();
        for (Ini.Line l : ini.entries(section)) out.add(new String[]{l.key, l.value == null ? "" : l.value});
        return out;
    }

    /** 181: כל הסעיף מחדש — לפי הסדר (שינוי, הזזה, מחיקה). שורות שאינן מפתח=ערך (הערות) — נשארות. */
    public static synchronized void replaceSection(String section, java.util.List<String[]> rows) {
        Ini ini = ini();
        for (Ini.Line l : new java.util.ArrayList<Ini.Line>(ini.entries(section))) ini.remove(l);
        for (String[] kv : rows) ini.add(section, kv[0], kv[1]);
        ini.saveQuiet();
    }

    public static void setBool(String section, String key, boolean v) {
        set(section, key, v ? "כן" : "לא");  // תחביר
    }
}
