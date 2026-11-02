package com.tikiyot.vfm;

// מ0.9.1: הספירה של רשימה במסד — מה שמוצג מתחת לאריח ולשורה, כמו ListCount במנוע הישן ובאותו נוסח (ListCount.text):
// תתי־רשימות ורשימות חכמות בכל העומק (רשימה שמופיעה בכמה מקומות — נספרת פעם אחת, בלי לולאה), קבצים (סכום), קבצים
// שונים ונפחם. מה שרק האינדקס יודע (כמה קבצים מוצאת רשימה חכמה) — מ־Index; לא ידוע — רק הסכום, בלי "שונים".
// אחראי: הספירה בלבד (בלי ממשק — נבדק). אסור לו: לכתוב למסד.

import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

final class DbCount {

    /** מה שהאינדקס יודע על תנאי: [קבצים, נפח] והקבצים עצמם (מסלול → גודל); null — לא ידוע (עדיין). */
    interface Index {
        long[] cond(String c);

        Map<String, Long> files(String c);
    }

    long lists, items, size, unique = -1, uniqueSize;
    private final Map<String, Long> seen = new HashMap<String, Long>();
    private final IdentityHashMap<Map<String, Long>, Boolean> merged = new IdentityHashMap<Map<String, Long>, Boolean>();
    private boolean partial;

    private DbCount() { }

    static DbCount of(Store s, long list, Index ix) {
        DbCount c = new DbCount();
        c.add(s, list, ix, new HashSet<Long>());
        if (!c.partial) {
            c.unique = c.seen.size();
            for (Long v : c.seen.values()) if (v != null && v > 0) c.uniqueSize += v;
        }
        return c;
    }

    /** רשימה חכמה לבדה: [קבצים, נפח] מהאינדקס, או null. */
    static long[] smart(Store s, long smart, Index ix) {
        String cond = s.prop(smart, Store.P_COND);
        return ix == null || cond == null || cond.isEmpty() ? null : ix.cond(cond);
    }

    String text() {
        return ListCount.text(lists, items, size, unique, uniqueSize);
    }

    private void add(Store s, long list, Index ix, Set<Long> done) {
        if (!done.add(list)) return;
        for (Store.Member m : s.members(list)) {
            Store.Item it = m.item;
            if (it.type == Store.LIST) {
                lists++;
                add(s, it.id, ix, done);
            } else if (it.type == Store.SMART) {
                lists++;
                String cond = s.prop(it.id, Store.P_COND);
                if (cond == null || cond.isEmpty()) continue;
                long[] v = ix == null ? null : ix.cond(cond);
                if (v != null) {
                    items += v[0];
                    size += v[1];
                }
                Map<String, Long> f = ix == null ? null : ix.files(cond);
                if (f == null) partial = true;
                else if (merged.put(f, Boolean.TRUE) == null) seen.putAll(f);
            } else if (it.type == Store.FILE) {
                Store.FileInfo fi = s.file(it.id);
                if (fi == null) continue;
                items++;
                if (fi.size > 0) size += fi.size;
                if (!seen.containsKey(fi.path) || fi.size > 0) seen.put(fi.path, fi.size);
            }
        }
    }
}
