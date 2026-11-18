package com.tikiyot.vfm;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * מפתחות המראה של פריט — הטבלה האחת. כל מפתח — שורה אחת ב־DEFS: השם בעיצוב (והמתג של "עצב" — עם "-" במקום רווח), השם הקבוע
 * ללוח, והערכים (הראשון שבהם — ברירת המחדל כשכתוב אחרת בשורה) או הטווח. מכאן נגזרים "עצב", "ודא עיצוב", הלוח והעורך, ואין מה
 * להוסיף בשום מקום אחר; הציור (Looks) קורא לפי השם הקבוע.
 * מבנה 265 (המשתמש, 18.11.2026: "יש דרך להוסיף בלי לעבור את ה1000 שורות?") — נפתח למפתחות החדשים; מבנה 267 (המשתמש: "אם תעשה
 * מחשבה כאילו אתה כותב את כל החלק הזה מחדש… אין חלקים שיכולים לכלול ביחד?" · "ותאחד את הכל") — גם עשרת הקודמים, שהיו כתובים
 * ב־Design כל אחד בשבעה מקומות.
 */
final class ItemKeys {
    static final String SHOW_ICON = "showIcon", AROUND = "around", TINT = "tint", SHOW_DATA = "showData", DATA_BELOW = "dataBelow",
            LINES = "lines", FIT = "fit", MIN = "min", SHOW_TAG = "showTag", FADE = "fade", SIDE = "side", SHOW_NAME = "showName";
    private static final String[] YES_NO = {Design.YES, Design.NO};
    static final int[] LINES_RANGE = {1, 6}, MIN_RANGE = {6, 40};

    /** מפתח אחד: בערכים — הראשון הוא ה"דלוק" (כן, מתחת); range — מספר בטווח (ואז אין ערכים). */
    static final class Def {
        final String key, id, def;
        final String[] values;
        final int[] range;

        Def(String key, String id, String def, String[] values, int[] range) {
            this.key = key;
            this.id = id;
            this.def = def;
            this.values = values;
            this.range = range;
        }
    }

    private static Def yesNo(String key, String id, String def) {
        return new Def(key, id, def, YES_NO, null);
    }

    static final Def[] DEFS = {
            yesNo(Design.K_SHOW_ICON, SHOW_ICON, Design.YES),
            new Def(Design.K_AROUND, AROUND, Design.FILL, new String[]{Design.NONE, Design.FRAME, Design.FILL, Design.CIRCLE}, null),
            yesNo(Design.K_TINT, TINT, Design.NO),
            yesNo(Design.K_SHOW_DATA, SHOW_DATA, Design.YES),
            new Def(Design.K_DATA_AT, DATA_BELOW, Design.BELOW, new String[]{Design.BELOW, Design.SIDE}, null),
            new Def(Design.K_LINES, LINES, "2", null, LINES_RANGE),
            yesNo(Design.K_FIT, FIT, Design.NO),
            new Def(Design.K_MIN, MIN, "11", null, MIN_RANGE),
            yesNo(Design.K_SHOW_TAG, SHOW_TAG, Design.YES),   // מבנה 256: "לא" — בלי תווית הסוג (ברשת — הפס; בשורות — הצלמית במקומה)
            yesNo(Design.K_FADE, FADE, Design.NO),            // מבנה 256: פריט מוסתר (שמו מתחיל בנקודה) — חיוור
            yesNo("צלמית לצד השם", SIDE, Design.NO),          // תחביר — מבנה 265: ברשת — אריח שוכב, הצלמית לצד השם
            yesNo("הצג שם", SHOW_NAME, Design.YES),           // תחביר — מבנה 265: "לא" — בלי השם (שימושי עם תמונות)
    };
    private final Map<String, String> set = new LinkedHashMap<String, String>();

    /** first, ואחריו כל המפתחות (keys) או כל השמות הקבועים — כך SHAPE_KEYS ו־SHAPE_IDS של Design. */
    static String[] with(String first, boolean keys) {
        String[] out = new String[1 + DEFS.length];
        out[0] = first;
        for (int i = 0; i < DEFS.length; i++) out[1 + i] = keys ? DEFS[i].key : DEFS[i].id;
        return out;
    }

    static Def def(String key) {
        for (Def d : DEFS) if (d.key.equals(key)) return d;
        return null;
    }

    private static Def byId(String id) {
        for (Def d : DEFS) if (d.id.equals(id)) return d;
        throw new IllegalArgumentException(id);
    }

    /** הערך של המפתח, כמו שנכתב בעיצוב; null — לא מפתח שכאן. */
    String get(String key) {
        Def d = def(key);
        if (d == null) return null;
        String v = set.get(key);
        return v != null ? v : d.def;
    }

    /** קובע. ערך שאינו מהערכים — ברירת המחדל; מספר — בתוך הטווח (לא מספר — נשאר מה שהיה). false — לא מפתח שכאן. */
    boolean put(String key, String v) {
        Def d = def(key);
        if (d == null) return false;
        if (d.range == null) set.put(key, java.util.Arrays.asList(d.values).contains(v) ? v : d.def);
        else {
            try {
                int n = Integer.parseInt(v.trim());
                set.put(key, String.valueOf(Math.max(d.range[0], Math.min(d.range[1], n))));
            } catch (NumberFormatException ignored) {
            }
        }
        return true;
    }

    /** לפי השם הקבוע: הערך. */
    String of(String id) {
        return get(byId(id).key);
    }

    /** לפי השם הקבוע: האם הערך הוא הראשון ("כן", "מתחת"). */
    boolean on(String id) {
        Def d = byId(id);
        return d.values[0].equals(get(d.key));
    }

    /** לפי השם הקבוע: המספר. */
    int num(String id) {
        return Integer.parseInt(of(id));
    }

    /** המקום לצלמית באריח של רשת: כל הרוחב פחות הריווח; כשהצלמית לצד השם — כשליש מהאריח (השאר לשם). */
    int room(int tileW, int pad) {
        return on(SIDE) ? tileW * 35 / 100 : tileW - 2 * pad;
    }

    void copyFrom(ItemKeys o) {
        set.clear();
        set.putAll(o.set);
    }

    String signature() {
        StringBuilder sb = new StringBuilder();
        for (Def d : DEFS) sb.append(get(d.key)).append('|');
        return sb.toString();
    }
}
