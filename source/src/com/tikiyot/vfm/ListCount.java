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
    }

    long lists, items, size;

    private ListCount() { }

    /** הספירה של הרשימה n בעץ t. index — null: בלי האינדקס. */
    static ListCount of(Tree t, Tree.Node n, Index index) {
        ListCount c = new ListCount();
        if (n != null) c.add(t, n, index, new HashSet<Tree.Node>());
        return c;
    }

    private void add(Tree t, Tree.Node n, Index ix, Set<Tree.Node> seen) {
        if (!seen.add(n)) return;
        items += n.entries.size();
        for (Tree.Entry e : n.entries) if (e.size > 0) size += e.size;
        for (Tree.Node c : n.children) {
            lists++;
            add(t, c, ix, seen);
        }
        for (String r : n.smart) {
            lists++;
            plus(ix == null ? null : ix.smart(r));
        }
        for (String f : n.live) {
            lists++;
            plus(ix == null ? null : ix.live(f));
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

    /** "3 רשימות · 12 פריטים · 4MB". */
    String text() {
        return (lists > 0 ? lists + Words.Manual.LISTS_DOT : "") + items + Words.Manual.ITEMS_SUFFIX
                + (size > 0 ? " · " + Files.size(size) : "");
    }

    /** מהאינדקס, עם זיכרון לכל ציור של המסך (חכמה שמופיעה בכמה רשימות — מחפשים פעם אחת). */
    static final class FromLibrary implements Index {
        private final Library lib;
        private final Map<String, long[]> cache = new HashMap<String, long[]>();

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
            for (Library.Hit x : r.hits) if (x.rec != null) s += x.rec.size;
            cache.put(ref, v = new long[]{r.hits.size(), s});
            return v;
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
