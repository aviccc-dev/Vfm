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

    /** כמה קבצים וגודל: רשימה חכמה (לפי ההפניה שלה) ותיקייה מתעדכנת — מהאינדקס; null — לא ידוע (עדיין). */
    interface Index {
        long[] smart(String ref);

        long[] live(String folder);

        /** 1.23.4: הקבצים עצמם (מסלול → גודל), לספירת קבצים שונים; null — לא ידוע. */
        Map<String, Long> smartFiles(String ref);

        Map<String, Long> liveFiles(String folder);
    }

    long lists, items, size;
    /** 1.23.4: קבצים שונים — כל קובץ פעם אחת, ונפחם; -1 — לא ידוע (חלק מהחכמות או התיקיות לא נספרו). */
    long unique = -1, uniqueSize;
    private Map<String, Long> seenFiles = new HashMap<String, Long>();
    private boolean partial;

    private ListCount() { }

    /** הספירה של הרשימה n בעץ t. index — null: בלי האינדקס. */
    static ListCount of(Tree t, Tree.Node n, Index index) {
        ListCount c = new ListCount();
        if (n != null) c.add(t, n, index, new HashSet<Tree.Node>());
        if (!c.partial) {
            c.unique = c.seenFiles.size();
            for (Long v : c.seenFiles.values()) if (v != null && v > 0) c.uniqueSize += v;
        }
        c.seenFiles = null;
        return c;
    }

    private void add(Tree t, Tree.Node n, Index ix, Set<Tree.Node> seen) {
        if (!seen.add(n)) return;
        items += n.entries.size();
        for (Tree.Entry e : n.entries) {
            if (e.size > 0) size += e.size;
            if (!seenFiles.containsKey(e.path) || e.size > 0) seenFiles.put(e.path, e.size);
        }
        for (Tree.Node c : n.children) {
            lists++;
            add(t, c, ix, seen);
        }
        for (String r : n.smart) {
            lists++;
            plus(ix == null ? null : ix.smart(r));
            files(ix == null ? null : ix.smartFiles(r));
        }
        for (String f : n.live) {
            lists++;
            plus(ix == null ? null : ix.live(f));
            files(ix == null ? null : ix.liveFiles(f));
        }
        for (String r : n.refs) {
            lists++;
            Tree.Node target = t.find(Tree.parsePath(r));
            if (target != null) add(t, target, ix, seen);
        }
    }

    private void plus(long[] filesAndSize) {
        if (filesAndSize == null) return;
        items += filesAndSize[0];
        size += filesAndSize[1];
    }

    private void files(Map<String, Long> m) {
        if (m == null) {
            partial = true;
            return;
        }
        seenFiles.putAll(m);
    }

    /**
     * "3 רשימות · 12 פריטים · 4MB"; 1.23.4 — כשיש כפילות: "3 רשימות · 4,210 קבצים (109,138) · 18.2GB" —
     * הקבצים השונים ונפחם, ובסוגריים הסכום (המשתמש: "מדד לסיבוכיות של הרשימות החכמות").
     */
    String text() {
        String l = lists > 0 ? lists + Words.Manual.LISTS_DOT : "";
        if (unique >= 0 && unique < items)
            return l + Files.count(unique) + Words.StatsW.FILES + " (" + Files.count(items) + ")"
                    + (uniqueSize > 0 ? " · " + Files.size(uniqueSize) : "");
        return l + items + Words.Manual.ITEMS_SUFFIX + (size > 0 ? " · " + Files.size(size) : "");
    }

    /** מהאינדקס, עם זיכרון לכל ציור של המסך (חכמה שמופיעה בכמה רשימות — מחפשים פעם אחת). */
    static final class FromLibrary implements Index {
        private final Library lib;
        private final Map<String, long[]> cache = new HashMap<String, long[]>();
        private final Map<String, Map<String, Long>> files = new HashMap<String, Map<String, Long>>();

        FromLibrary(Library lib) {
            this.lib = lib;
        }

        @Override
        public long[] smart(String ref) {
            if (lib == null || !lib.ready) return null;
            long[] v = cache.get(ref);
            if (v != null) return v;
            Nested.Item it = Nested.resolve(ref);
            if (it.gone || it.dup != null) return null;                       // כפולים — נספרים רק בפתיחה
            Library.Result r = lib.search(it.cond, false, true);
            long s = 0;
            Map<String, Long> m = new HashMap<String, Long>();
            for (Library.Hit x : r.hits) {
                if (x.rec != null) s += x.rec.size;
                m.put(x.path(), x.rec != null ? x.rec.size : 0L);
            }
            files.put(ref, m);
            cache.put(ref, v = new long[]{r.hits.size(), s});
            return v;
        }

        @Override
        public Map<String, Long> smartFiles(String ref) {
            return smart(ref) == null ? null : files.get(ref);
        }

        @Override
        public Map<String, Long> liveFiles(String folder) {
            if (lib == null || !lib.ready) return null;
            Map<String, Long> m = files.get(folder);
            if (m == null) files.put(folder, m = lib.treeFiles(folder));
            return m;
        }

        @Override
        public long[] live(String folder) {
            if (lib == null || !lib.ready) return null;
            long[] v = cache.get(folder);
            if (v == null) cache.put(folder, v = lib.treeStats(folder));
            return v;
        }
    }
}
