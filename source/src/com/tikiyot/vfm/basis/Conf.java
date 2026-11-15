package com.tikiyot.vfm;

// הגדרות כלליות בקובץ טקסט. אחראי: קריאה וכתיבה של "הגדרות כלליות.ini".
// כל הגדרה שאינה של כלי מסוים נשמרת כאן, ולא בזיכרון הפנימי של אנדרואיד.


import java.io.File;

public final class Conf {

    public static final String GENERAL = "כללי";  // תחביר
    public static final String SEARCH = "חיפוש";  // תחביר

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
    /** מ0.24: "העתקה"/"העברה" מוסיפות ללוח של המסד במקום להחליף אותו (DbClip). */
    public static final String CLIP_ADD = "צירוף ללוח";  // תחביר
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
        String v = get(section, key, def ? YES : NO);
        return YES.equals(v);
    }

    public static synchronized void set(String section, String key, String value) {
        put(section, key, value);
    }

    /** כמו set, ומחזיר אם נשמר (מבנה 127: מי שקובע בפקודה צריך לדעת). */
    private static synchronized boolean put(String section, String key, String value) {
        Ini ini = ini();
        for (Ini.Line l : ini.entries(section))
            if (l.key.equals(key)) {
                if (value.equals(l.value)) return true;
                ini.set(l, key, value);
                return ini.saveQuiet();
            }
        ini.add(section, key, value);
        return ini.saveQuiet();
    }

    // ---------- מבנה 127: ההגדרות שנקבעות בפקודה "הגדר" — מפתח, סוג וערכים מותרים, במקום אחד ----------
    // המשתמש (15.11.2026): "כל מה שאפשר לעבוד בפקודה עדיף שיעבוד בפקודה." המסכים והקובץ לוקחים מכאן; הגדרה שאינה כאן —
    // "הגדר" אינה מכירה. הכול בסעיף [כללי] של קובץ ההגדרות של אופן העבודה.

    /** 110: רמות העדכון של האינדקס, והרענון התקופתי. */
    public static final String LEVEL = "עדכון האינדקס", PERIODIC = "רענון תקופתי בדקות";  // תחביר
    public static final String[] LEVELS = {"ידני בלבד", "בכל הפעלה של התוכנה", "בכל חזרה אל התוכנה"};  // תחביר
    public static final String YES = "כן", NO = "לא";  // תחביר

    /** סוג הערך: אחד מכמה · מספר (0 ומעלה) · תיקיות (מסלולים מלאים, מופרדים ב"|"; ריק — אין) · טקסט. */
    public static final int CHOICE = 0, NUMBER = 1, DIRS = 2, TEXT = 3;

    public static final class Setting {
        public final String key;
        public final int kind;
        public final String[] values;

        Setting(String key, int kind, String... values) {
            this.key = key;
            this.kind = kind;
            this.values = values;
        }
    }

    public static final Setting[] SETTINGS = {
            new Setting(DELETED, CHOICE, DELETED_REMOVE, DELETED_GREY),
            new Setting(EXCLUDED, DIRS),
            new Setting(SEND_TO, TEXT),
            new Setting(LEVEL, CHOICE, LEVELS),
            new Setting(PERIODIC, NUMBER),
            new Setting(FULL, CHOICE, YES, NO),
            new Setting(CLIP_ADD, CHOICE, YES, NO),
            new Setting(READERS, NUMBER),
    };

    /** ההגדרה לפי המפתח שלה; null — "הגדר" אינה מכירה. */
    public static Setting setting(String key) {
        for (Setting s : SETTINGS) if (s.key.equals(key)) return s;
        return null;
    }

    /** הערך כמו שיישמר (תיקיות — מסלולים נקיים); null — הערך אינו מתאים להגדרה. */
    public static String valid(Setting s, String value) {
        String v = value == null ? "" : value.trim();
        if (s.kind == CHOICE) {
            for (String x : s.values) if (x.equals(v)) return v;
            return null;
        }
        if (s.kind == NUMBER) {
            if (!v.matches("\\d{1,6}")) return null;
            return String.valueOf(Integer.parseInt(v));
        }
        if (s.kind == DIRS) {
            StringBuilder sb = new StringBuilder();
            for (String part : v.split("\\|")) {
                String d = part.trim().replace('\\', '/');
                if (d.isEmpty()) continue;
                if (!d.startsWith("/")) return null;
                sb.append(sb.length() > 0 ? "|" : "").append(Paths.clean(d));
            }
            return sb.toString();
        }
        return v.indexOf('\n') >= 0 || v.indexOf('\r') >= 0 ? null : v;
    }

    /** הערך של היום ("" — לא נקבע). */
    public static String current(Setting s) {
        return get(GENERAL, s.key, "");
    }

    /**
     * הקביעה עצמה. שואלת את ההיתר של שכבת הניהול (הוראות, סעיף 2). null — נקבע ונשמר; אחרת — למה לא (הסירוב, או שהשמירה נכשלה).
     */
    public static String apply(Setting s, String value) {
        String no = Permit.check(Permit.SETTING);
        if (no != null) return no;
        return put(GENERAL, s.key, value) ? null : Words.SAVE_FAILED;
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
        set(section, key, v ? YES : NO);
    }
}
