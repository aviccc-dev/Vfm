package com.tikiyot.vfm;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * מבנה 265 (המשתמש, 18.11.2026: "תגמור את שניהם" — לבנות בתוכנה את מה שהיה רק בניסוי של הלוח; ועל הגודל: "יש דרך להוסיף בלי
 * לעבור את ה1000 שורות?"): מפתחות נוספים של מראה הפריט — בקובץ משלהם, כדי ש־Design לא יגדל. כולם כן / לא. מפתח חדש — שורה אחת
 * ב־DEFS; המתג של "עצב", "ודא עיצוב", הלוח והעורך נגזרים ממנה (דרך Design.SHAPE_KEYS), ואין מה להוסיף בשום מקום אחר.
 */
final class Extras {
    static final String SIDE = "side", SHOW_NAME = "showName";
    /** [המפתח בקובץ העיצוב (והמתג — עם "-" במקום רווח), השם הקבוע ללוח, ברירת המחדל]. */
    static final String[][] DEFS = {
            {"צלמית לצד השם", SIDE, Design.NO},      // תחביר — ברשת: אריח שוכב, הצלמית לצד השם
            {"הצג שם", SHOW_NAME, Design.YES},        // תחביר — "לא": בלי השם (שימושי עם תמונות)
    };
    private final Map<String, String> set = new LinkedHashMap<String, String>();

    /** base ועוד העמודה col של כל המפתחות שכאן — כך SHAPE_KEYS ו־SHAPE_IDS של Design כוללים אותם. */
    static String[] with(String[] base, int col) {
        String[] out = new String[base.length + DEFS.length];
        System.arraycopy(base, 0, out, 0, base.length);
        for (int i = 0; i < DEFS.length; i++) out[base.length + i] = DEFS[i][col];
        return out;
    }

    private static String[] def(String key) {
        for (String[] d : DEFS) if (d[0].equals(key)) return d;
        return null;
    }

    static boolean has(String key) {
        return def(key) != null;
    }

    /** הערך של המפתח (כן / לא); null — לא מפתח שכאן. */
    String get(String key) {
        String[] d = def(key);
        if (d == null) return null;
        String v = set.get(key);
        return v != null ? v : d[2];
    }

    /** קובע; מה שאינו "כן" או "לא" — ברירת המחדל. false — לא מפתח שכאן. */
    boolean put(String key, String v) {
        String[] d = def(key);
        if (d == null) return false;
        set.put(key, Design.YES.equals(v) || Design.NO.equals(v) ? v : d[2]);
        return true;
    }

    /** לפי השם הקבוע: האם "כן". */
    boolean on(String id) {
        for (String[] d : DEFS) if (d[1].equals(id)) return Design.YES.equals(get(d[0]));
        return false;
    }

    /** המקום לצלמית באריח של רשת: כל הרוחב פחות הריווח; כשהצלמית לצד השם — כשליש מהאריח (השאר לשם). */
    int room(int tileW, int pad) {
        return on(SIDE) ? tileW * 35 / 100 : tileW - 2 * pad;
    }

    void copyFrom(Extras o) {
        set.clear();
        set.putAll(o.set);
    }

    String signature() {
        StringBuilder sb = new StringBuilder();
        for (String[] d : DEFS) sb.append(get(d[0])).append('|');
        return sb.toString();
    }

    /** השורות לקובץ עיצוב מובנה — ברירות המחדל. */
    static void defaults(StringBuilder sb) {
        for (String[] d : DEFS) sb.append(d[0]).append(" = ").append(d[2]).append('\n');
    }
}
