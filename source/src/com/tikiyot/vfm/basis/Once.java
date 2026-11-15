package com.tikiyot.vfm;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * מבנה 146 (ממצא 5): ערך שנגזר מקובץ — מחושב פעם אחת, עד שהקובץ משתנה (זמן השינוי או הגודל). עד כאן נכתב שלוש פעמים
 * בתפריטים (הטקסט של קובץ פקודות, איך כפתור נראה, "מתאים לבחירה"), כל פעם קצת אחרת — ואחת מהן בלי גבול לגודל.
 * אחראי: לזכור ולשכוח. אסור לו: לדעת מה הערך.
 */
public final class Once<V> {

    private final Map<String, Object[]> map = new HashMap<String, Object[]>();
    private final int max;

    public Once(int max) {
        this.max = max;
    }

    private static String stamp(File f) {
        return f.lastModified() + "\n" + f.length();
    }

    /** מה שנזכר לקובץ הזה (ול־extra — כשיש כמה ערכים לקובץ), אם הקובץ לא השתנה מאז; אחרת null. */
    @SuppressWarnings("unchecked")
    public synchronized V had(File f, String extra) {
        Object[] e = map.get(f.getPath() + "\n" + extra);
        return e != null && e[0].equals(stamp(f)) ? (V) e[1] : null;
    }

    /** זוכר. כשהתמלא — מתרוקן (פשוט, ומספיק: מה שצריך יחושב שוב). */
    public synchronized V put(File f, String extra, V v) {
        if (map.size() >= max) map.clear();
        map.put(f.getPath() + "\n" + extra, new Object[]{stamp(f), v});
        return v;
    }
}
