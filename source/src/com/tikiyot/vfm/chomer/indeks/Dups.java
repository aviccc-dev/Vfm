package com.tikiyot.vfm;

// מבנה 190 (סידור קומת המסכים): מי כפול של מי — החישוב של מסך הכפולים. עד כאן בתוך המסך (DupScreen), בלי אף בדיקת מחשב;
// לפי החישוב הזה המשתמש מוחק קבצים, ולכן מקומו כאן — בחומר, נבדק בכל בנייה.
// אחראי: אילו קבצים נבדקים, מתי שניים הם "אותו קובץ" (שם / גודל / תוכן — DupMode), שמות דומים, ו"בשניהם · רק בא׳ · רק בב׳".
// אסור לו: מסכים, ולשנות דבר — הוא רק קורא את האינדקס ומחזיר קבוצות. מה מציגים ואיך — במסך.

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class Dups {

    private Dups() { }

    /** הקבצים שנבדקים במקום אחד (תנאי של מקורות; ריק או "כל הכונן" — הכול). בלי ריקים, בלי הסל והמוסתרים. */
    static List<Library.Hit> group(Library lib, String spec) {
        List<Library.Hit> out = new ArrayList<Library.Hit>();
        String sp = spec == null ? "" : spec.trim();
        Cond cond = sp.isEmpty() || sp.equals(Words.Dup.WHOLE_DRIVE) ? null : Cond.parse(sp);
        for (Index ix : lib.indexes)
            for (Index.Rec r : ix.files) {
                if (r.gone) continue;
                if (r.size == 0) continue;                        // 96: קבצים ריקים אינם עותקים זה של זה
                if (Library.hide(ix, r, cond)) continue;          // הסל והמוסתרים לא נחשבים עותקים
                if (cond != null && !cond.matches(ix, r)) continue;
                out.add(Library.hit(ix, r));
            }
        return out;
    }

    static boolean alike(DupMode d, Library.Hit x, Library.Hit y) {
        return d.name != DupMode.NAME_SIMILAR || Text.similarNames(x.name(), y.name());
    }

    /** קבוצה של אותו גודל — מפוצלת לקבוצות של שמות דומים (כל מי שדומה לאחד מהקבוצה נכנס אליה). */
    static List<List<Library.Hit>> split(DupMode d, List<Library.Hit> g) {
        List<List<Library.Hit>> out = new ArrayList<List<Library.Hit>>();
        if (d.name != DupMode.NAME_SIMILAR) {
            out.add(g);
            return out;
        }
        for (Library.Hit x : g) {
            List<Library.Hit> home = null;
            for (java.util.Iterator<List<Library.Hit>> it = out.iterator(); it.hasNext(); ) {
                List<Library.Hit> c = it.next();
                boolean near = false;
                for (Library.Hit y : c) if (alike(d, x, y)) { near = true; break; }
                if (!near) continue;
                if (home == null) home = c;
                else {                                    // x מחבר שתי קבוצות — מאחדים
                    home.addAll(c);
                    it.remove();
                }
            }
            if (home == null) out.add(home = new ArrayList<Library.Hit>());
            home.add(x);
        }
        return out;
    }

    static String key(DupMode d, Library.Hit hit) {                              // 195: כל שילוב
        StringBuilder k = new StringBuilder();
        if (d.name == DupMode.NAME_SAME) k.append(hit.rec.norm);
        k.append('|');
        if (d.size) k.append(hit.rec.size);
        k.append('|');
        if (d.content) k.append(hit.rec.sig != 0 ? Long.toHexString(hit.rec.sig) : "?" + hit.path());   // 190: בלי חתימה — לבד
        return k.toString();
    }

    /** בספק (85): שמות דומים — ובקבוצה יש שם שאינו זהה לראשון. */
    static boolean doubt(DupMode d, List<Library.Hit> g) {
        if (d.name == DupMode.NAME_SIMILAR)
            for (Library.Hit x : g) if (!x.rec.norm.equals(g.get(0).rec.norm)) return true;
        return false;
    }

    /** 194: כמה קבצים שיש להם עוד קובץ באותו גודל נשארו בלי חתימה — לא נחשבים כפולים "בתוכן". */
    static int unsigned(List<Library.Hit> a) {
        int unsigned = 0;
        Map<Long, Integer> sz = new HashMap<Long, Integer>();
        for (Library.Hit x : a) sz.put(x.rec.size, sz.containsKey(x.rec.size) ? sz.get(x.rec.size) + 1 : 1);
        for (Library.Hit x : a) if (x.rec.sig == 0 && sz.get(x.rec.size) > 1) unsigned++;
        return unsigned;
    }

    /** 190: הקבצים שצריך לחשב להם חתימה עכשיו — יש עוד קובץ באותו גודל, ואין להם חתימה. כל רשומה פעם אחת. */
    static List<Library.Hit> needSig(List<Library.Hit> all) {
        Map<Long, Integer> sizes = new HashMap<Long, Integer>();
        for (Library.Hit x : all) {
            Integer n = sizes.get(x.rec.size);
            sizes.put(x.rec.size, n == null ? 1 : n + 1);
        }
        List<Library.Hit> todo = new ArrayList<Library.Hit>();
        Set<Index.Rec> seen = new HashSet<Index.Rec>();
        for (Library.Hit x : all)
            if (x.rec.sig == 0 && sizes.get(x.rec.size) > 1 && seen.add(x.rec)) todo.add(x);
        return todo;
    }

    /** כפולים בתוך מקום אחד: הקבוצות (שני קבצים ומעלה), בסדר שבו נמצאו. */
    static List<List<Library.Hit>> within(DupMode d, List<Library.Hit> all) {
        Map<String, List<Library.Hit>> m = new LinkedHashMap<String, List<Library.Hit>>();
        Set<String> seen = new HashSet<String>();                              // מבנה 207: אותו קובץ פעמיים (אותו מסלול) — אינו כפיל של עצמו
        for (Library.Hit x : all) if (seen.add(x.path())) put(d, m, x);
        List<List<Library.Hit>> out = new ArrayList<List<Library.Hit>>();
        for (List<Library.Hit> big : m.values()) {
            if (big.size() < 2) continue;
            for (List<Library.Hit> g : split(d, big)) {
                if (g.size() < 2) continue;
                out.add(g);
            }
        }
        return out;
    }

    /** השוואה בין שני מקומות. both — לכל קובץ של א׳ שיש לו עותק בב׳: הוא, ואחריו העותקים שלו בב׳. */
    static final class Versus {
        final List<List<Library.Hit>> both = new ArrayList<List<Library.Hit>>();
        final List<Library.Hit> onlyA = new ArrayList<Library.Hit>(), onlyB = new ArrayList<Library.Hit>();
    }

    static Versus versus(DupMode d, List<Library.Hit> a, List<Library.Hit> b) {
        Versus v = new Versus();
        Map<String, List<Library.Hit>> inA = new HashMap<String, List<Library.Hit>>(), inB = new HashMap<String, List<Library.Hit>>();
        for (Library.Hit x : a) put(d, inA, x);
        for (Library.Hit y : b) put(d, inB, y);
        for (Library.Hit x : a) {
            List<Library.Hit> copies = new ArrayList<Library.Hit>();
            List<Library.Hit> g = inB.get(key(d, x));
            if (g != null) for (Library.Hit y : g) if (y.rec != x.rec && alike(d, x, y)) copies.add(y);   // הקובץ עצמו אינו עותק
            if (copies.isEmpty()) {
                v.onlyA.add(x);
            } else {
                List<Library.Hit> all = new ArrayList<Library.Hit>();
                all.add(x);
                all.addAll(copies);
                v.both.add(all);
            }
        }
        Set<Index.Rec> aRecs = new HashSet<Index.Rec>();
        for (Library.Hit x : a) aRecs.add(x.rec);
        for (Library.Hit y : b) {
            if (aRecs.contains(y.rec)) continue;
            List<Library.Hit> g = inA.get(key(d, y));
            boolean has = false;
            if (g != null) for (Library.Hit x : g) if (x.rec != y.rec && alike(d, x, y)) has = true;
            if (!has) v.onlyB.add(y);
        }
        return v;
    }

    private static void put(DupMode d, Map<String, List<Library.Hit>> m, Library.Hit x) {
        List<Library.Hit> g = m.get(key(d, x));
        if (g == null) m.put(key(d, x), g = new ArrayList<Library.Hit>());
        g.add(x);
    }
}
