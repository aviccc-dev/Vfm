package com.tikiyot.vfm;

// מבנה 237 (המשתמש, 17.11.2026, על לוח העיצוב: "רשימת צבעים עם מחוונים למקום לקבוע בהירות" · "ולפי החלוקה שנמצאת בתכנות"):
// הצבעים של התוכנה — 18 התפקידים שבקוד (עד כאן קבועים ב־Ui), בשמות של המשתמש, בשתי ערכות (בהיר, כהה). מה שנקבע — שורות
// במראה של כל התוכנה ושל האופן (Store.P_ALL_LOOK): "מראה = כהה", "צבע: טקסט משני = #5F6368". הסדר: הערכה, ואז מה שנקבע לכל
// התוכנה, ואז מה שנקבע לאופן. הכלל (מה מותר) והקריאה — כאן; הציור — ב־Ui (המסכים); הקביעה — "עצב תוכנה" / "עצב "<אופן>"" (מבנה 246).

final class Palette {

    private Palette() { }

    /** במראה: בהיר או כהה. */
    static final String K_MODE = "מראה", LIGHT = "בהיר", DARK = "כהה";  // תחביר
    /** במראה: צבע של תפקיד ("צבע: טקסט = #202124"). */
    static final String K_COLOR = "צבע";  // תחביר

    /** התפקידים — לפי הסדר שבקוד (Ui), בחמש קבוצות: השם, בהיר, כהה. */
    static final String[][] ROLES = {
            {"טקסט", "#202124", "#E8EAED"}, {"טקסט משני", "#5F6368", "#BDC1C6"}, {"טקסט חלש", "#9AA0A6", "#80868B"},   // תחביר
            {"טקסט על צבע", "#FFFFFF", "#FFFFFF"},   // תחביר
            {"רקע", "#FFFFFF", "#202124"}, {"רקע רך", "#F4F5F6", "#2A2B2E"}, {"רקע הכותרת", "#F1F3F4", "#303134"},   // תחביר
            {"כותרת קבוצה", "#E8EAED", "#3C4043"},   // תחביר
            {"פס הבחירה", "#E8F0FE", "#1F3A5F"}, {"שורה שנבחרה", "#D2E3FC", "#28456E"}, {"הדגשה", "#1A73E8", "#8AB4F8"},   // תחביר
            {"הדגשה כהה", "#174EA6", "#AECBFA"},   // תחביר
            {"סימון התאמה", "#FFF2A8", "#5C4A00"}, {"בספק", "#FFF1C2", "#4A3D10"}, {"בספק חזק", "#F2B52B", "#F2B52B"},   // תחביר
            {"אזהרה ומחיקה", "#C5221F", "#F28B82"},   // תחביר
            {"צלמיות", "#5F6368", "#BDC1C6"}, {"קו בין חלקים", "#E0E0E0", "#3C4043"}};  // תחביר
    /** הקבוצות — לתוסף העיצוב (כמה תפקידים בכל אחת, לפי הסדר). */
    static final String[] GROUPS = {"טקסט:4", "רקעים:4", "בחירה והדגשה:4", "סימון ואזהרה:4", "צלמיות וקווים:2"};  // תחביר

    static int role(String name) {
        for (int i = 0; i < ROLES.length; i++) if (ROLES[i][0].equals(name == null ? "" : name.trim())) return i;
        return -1;
    }

    /** "#RRGGBB" ← צבע (אטום); לא מובן — null. */
    static Integer color(String v) {
        String s = v == null ? "" : v.trim();
        if (s.startsWith("#")) s = s.substring(1);
        if (!s.matches("[0-9A-Fa-f]{6}")) return null;
        return 0xFF000000 | Integer.parseInt(s, 16);
    }

    static String hex(int c) {
        return String.format(java.util.Locale.ROOT, "#%06X", c & 0xFFFFFF);
    }

    /** null — תקין; אחרת — למה לא (במילים של המשתמש). key — "מראה", או "צבע: <תפקיד>". */
    static String error(String key, String value) {
        if (K_MODE.equals(key)) return LIGHT.equals(value) || DARK.equals(value) ? null : Words.DesignW.badMode(value);
        int c = key.indexOf(':');
        if (c > 0 && key.substring(0, c).trim().equals(K_COLOR)) {
            if (role(key.substring(c + 1)) < 0) return Words.DesignW.noRole(key.substring(c + 1).trim());
            return color(value) != null ? null : Words.DesignW.badColor(value);
        }
        return Words.DesignW.noRole(key);
    }

    /** הצבעים, לפי הסדר של ROLES: הערכה (בהיר / כהה — מי שנקבע אחרון), ואז הצבעים שנקבעו, לפי הסדר של looks. */
    static int[] resolve(String... looks) {
        boolean dark = false;
        for (String l : looks) {
            String m = value(l, K_MODE);
            if (m != null) dark = DARK.equals(m);
        }
        int[] out = new int[ROLES.length];
        for (int i = 0; i < ROLES.length; i++) out[i] = color(ROLES[i][dark ? 2 : 1]);
        for (String l : looks)
            for (int i = 0; i < ROLES.length; i++) {
                Integer c = color(value(l, K_COLOR + ": " + ROLES[i][0]));
                if (c != null) out[i] = c;
            }
        return out;
    }

    /** האם כהה (לפי מה שנקבע אחרון). */
    static boolean dark(String... looks) {
        boolean dark = false;
        for (String l : looks) {
            String m = value(l, K_MODE);
            if (m != null) dark = DARK.equals(m);
        }
        return dark;
    }

    /** הערך של key בשורת המראה; אין — null. */
    static String value(String look, String key) {
        if (look == null) return null;
        for (String part : look.split(java.util.regex.Pattern.quote(Design.P_SEP))) {
            int eq = part.lastIndexOf('=');
            if (eq > 0 && part.substring(0, eq).trim().equals(key)) return part.substring(eq + 1).trim();
        }
        return null;
    }

    // ---------- מה שנקבע עכשיו: כל התוכנה, ואז האופן שעל המסך ----------

    private static String[] now = null;
    private static String nowMode = null;
    private static int version = 0;

    /** אחרי "עצב תוכנה" / "עצב "<אופן>"", ובמעבר אופן — לקרוא מחדש. */
    static synchronized void forget() {
        now = null;
        version++;
    }

    /** מספר שמשתנה בכל פעם שמה שנקבע עשוי להשתנות — המסכים צובעים מחדש רק כשהוא זז. */
    static synchronized int version() {
        return version;
    }

    /** המראה של כל התוכנה ושל האופן שעל המסך — {של התוכנה, של האופן}. */
    static synchronized String[] looks() {
        if (now != null && String.valueOf(Db.currentMode).equals(nowMode)) return now;   // אופן אחר על המסך — קוראים מחדש
        if (now != null) version++;
        nowMode = String.valueOf(Db.currentMode);
        String all = Design.programProps()[1], mode = null;
        try {
            if (Db.app != null && Db.currentMode != null && Db.dbFile(Db.app, Db.currentMode).isFile())
                mode = Db.read(Db.app, Db.currentMode, new Db.Use<String>() {
                    @Override public String run(SqlStore s) {
                        long h = DbPaths.home(s);
                        return h == 0 ? null : s.prop(h, Store.P_ALL_LOOK);
                    }
                });
        } catch (Throwable e) {
            Log.e("Palette.looks", e);
        }
        now = new String[]{all, mode};
        return now;
    }
}
