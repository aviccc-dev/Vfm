package com.tikiyot.vfm;

// 1.14.0: הספירה שמתחת לרשימה. מ0.29 (המנוע הישן נמחק): הספירה עצמה — DbCount, על המסד; כאן נשארו הנוסח (text) ומה
// שרק האינדקס יודע — כמה קבצים מוצאת רשימה חכמה (FromLibrary, עם זיכרון לפי התנאי).
// אחראי: הנוסח והספירה מהאינדקס (בלי ממשק — נבדק).

import java.util.HashMap;
import java.util.Map;

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
