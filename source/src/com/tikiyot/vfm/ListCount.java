package com.tikiyot.vfm;

// 1.14.0: ספירה אחת לרשימה ידנית — מה שמוצג מתחת לרשימה, בהפניה אליה, ובאריח שלה בבית ובסביבה.
// באג (המשתמש, 18.10: "ברשימות ידניות לא מופיע מה הן מכילות — כתוב 0 פריטים; אולי רק בחלק מהמצבים"): כל מקום ספר
// אחרת, ואף אחד לא ספר הפניות ↪ ותיקיות מתעדכנות. כאן — הכול: קבצים, תתי־רשימות, רשימות חכמות, תיקיות מתעדכנות
// והפניות (מה שברשימה שאליה מפנים), בכל העומק; הפניה שחוזרת על עצמה — פעם אחת (בלי לולאה).
// מה שרק האינדקס יודע (כמה קבצים מוצאת חכמה, כמה יש בתיקייה) — מ-Library כשהוא מוכן; עד אז — רק הרשימות נספרות.
// אחראי: הספירה בלבד (בלי ממשק — נבדק).

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

final class ListCount {

    private ListCount() { }

    /** הנוסח — מקום אחד (מ0.9.1: גם הספירה של המסד, DbCount). */
    static String text(long lists, long items, long size, long unique, long uniqueSize) {
        String l = lists > 0 ? lists + Words.Manual.LISTS_DOT : "";
        if (unique >= 0 && unique < items)
            return l + Files.count(unique) + Words.StatsW.FILES + " (" + Files.count(items) + ")"
                    + (uniqueSize > 0 ? " · " + Files.size(uniqueSize) : "");
        return l + items + Words.Manual.ITEMS_SUFFIX + (size > 0 ? " · " + Files.size(size) : "");
    }

    /**
     * מהאינדקס, עם זיכרון לכל ציור של המסך. 1.25.4 (באג, יומן המכשיר: OutOfMemoryError בהכנת בדיקת העומס): הזיכרון —
     * לפי התנאי, לא לפי ההפניה. עותקים של רשימה חכמה (אותו תנאי, הפניה אחרת) — חיפוש אחד ורשימת קבצים אחת; עד כאן —
     * חיפוש ורשימה לכל עותק, ובמאות עותקים הזיכרון נגמר.
     */
    static final class FromLibrary {
        private final Library lib;
        private final Map<String, String> condOf = new HashMap<String, String>();
        /** לבדיקות: כמה חיפושים נעשו. */
        int searches;
        private final Map<String, long[]> cache = new HashMap<String, long[]>();
        private final Map<String, Map<String, Long>> files = new HashMap<String, Map<String, Long>>();

        FromLibrary(Library lib) {
            this.lib = lib;
        }

        /** מ0.9.1: לפי התנאי עצמו (רשימה חכמה במסד); null — האינדקס לא מוכן, או אין תנאי. */
        long[] byCond(String c) {
            if (lib == null || !lib.ready || c == null) return null;
            long[] v = cache.get(c);
            if (v != null) return v;
            Library.Result r = lib.search(c, false, true);
            searches++;
            long s = 0;
            Map<String, Long> m = new HashMap<String, Long>();
            for (Library.Hit x : r.hits) {
                if (x.rec != null) s += x.rec.size;
                m.put(x.path(), x.rec != null ? x.rec.size : 0L);
            }
            files.put(c, m);
            cache.put(c, v = new long[]{r.hits.size(), s});
            return v;
        }

        Map<String, Long> filesByCond(String c) {
            if (byCond(c) == null) return null;
            return files.get(c);
        }
    }
}
