package com.tikiyot.vfm;

// הרשימות של אופן — כצילום לקריאה (מבנה 18, 13.11.2026): עץ הרשימות הידניות מהבית ומטה (Snap), השמות לטפסים,
// "באילו רשימות כל קובץ", והעץ שהחתימות עובדות עליו. עד כאן ישב בתוך DbPicker — יחד עם החלון של בורר הרשימות —
// ולכן החתימות (Sig) והגשר נזקקו למסך כדי לשאול על הרשימות. אותן שורות. החלון נשאר ב־DbPicker.
// אחראי: לקרוא את המסד ולתאר את הרשימות. אסור לו: לכתוב למסד, לפתוח חלונות.

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class DbLists {

    private DbLists() { }

    /** האם בוחרים מהמסד: בבניית מ, לא בצד הישן, ויש מסד לאופן. */
    static boolean on(android.content.Context c) {
        String m = mode(c);
        return m != null && !m.isEmpty() && Db.dbFile(c, m).isFile();
    }

    /** האופן: של מסך המסד הפתוח, ואם אין — מה שנבחר בפעם הקודמת. */
    static String mode(android.content.Context c) {
        return Db.currentMode != null ? Db.currentMode : Db.savedMode(c);
    }

    // ================= הצילום =================

    /** עץ הרשימות הידניות מהבית ומטה, לפי שמות. שני אחים באותו שם — השני "שם (2)" (כדי שלכל אחד יהיה מסלול). */
    static final class Snap {
        final Map<String, Long> ids = new HashMap<String, Long>();
        final Map<String, List<String>> kids = new HashMap<String, List<String>>();
        /** הרשימות החכמות, בשם שהפקודות מכירות: בבית — בשמה; בתוך רשימה — "מסלול\\שם". */
        final List<String> smarts = new ArrayList<String>();
        long home;

        /**
         * השורש — כמו החלק הראשון של מסלול בפקודה (DbPaths.find): הרשימות שבבית, ואחריהן הרשימות שאינן בתוך שום רשימה
         * (חוץ מהבית עצמו).
         */
        static Snap of(Store s) {
            Snap n = new Snap();
            n.home = DbPaths.home(s);
            List<String> root = new ArrayList<String>();
            List<String> names = new ArrayList<String>();
            n.kids.put(key(root), names);
            Map<String, Integer> seen = new HashMap<String, Integer>();
            List<Long> tops = new ArrayList<Long>();
            if (n.home != 0)
                for (Store.Member m : s.members(n.home)) {
                    if (m.item.type == Store.SMART) n.smarts.add(m.item.name);
                    else if (m.item.type == Store.LIST) tops.add(m.item.id);
                }
            for (Store.Item it : s.lists())
                if (it.id != n.home && s.listsOf(it.id).isEmpty() && !tops.contains(it.id)) tops.add(it.id);
            for (Store.Item it : s.smarts())                                  // חכמה שעומדת לעצמה — בשמה
                if (s.listsOf(it.id).isEmpty() && !n.smarts.contains(it.name)) n.smarts.add(it.name);
            for (Long id : tops) {
                String nm = unique(s.item(id).name, seen);
                names.add(nm);
                List<String> p = child(root, nm);
                n.ids.put(key(p), id);
                n.walk(s, id, p, 1);
            }
            return n;
        }

        private void walk(Store s, long list, List<String> path, int depth) {
            List<String> names = new ArrayList<String>();
            kids.put(key(path), names);
            if (depth > 40) return;                                           // המסד מונע מעגלים; זו רק רשת ביטחון
            Map<String, Integer> seen = new HashMap<String, Integer>();
            for (Store.Member m : s.members(list)) {
                if (m.item.type == Store.SMART) {
                    smarts.add(path.isEmpty() ? m.item.name : Names.pathText(path) + "\\" + m.item.name);
                    continue;
                }
                if (m.item.type != Store.LIST) continue;
                String nm = unique(m.item.name, seen);
                names.add(nm);
                List<String> p = child(path, nm);
                ids.put(key(p), m.item.id);
                walk(s, m.item.id, p, depth + 1);
            }
        }

        /** הרשימה במסלול, או 0. */
        long id(List<String> path) {
            Long v = ids.get(key(path));
            return v == null ? 0 : v;
        }

        /** כל המסלולים, לפי הסדר (לטופס המקומות הפתוחים): "א", "א\\ב"… */
        List<String> paths() {
            List<String> out = new ArrayList<String>();
            collect(new ArrayList<String>(), out);
            return out;
        }

        private void collect(List<String> p, List<String> out) {
            List<String> k = kids.get(key(p));
            if (k == null) return;
            for (String c : k) {
                List<String> q = child(p, c);
                out.add(Names.pathText(q));
                collect(q, out);
            }
        }

        private static String unique(String name, Map<String, Integer> seen) {
            Integer k = seen.get(name);
            seen.put(name, k == null ? 1 : k + 1);
            return k == null ? name : name + " (" + (k + 1) + ")";
        }
    }

    static List<String> child(List<String> p, String name) {
        List<String> c = new ArrayList<String>(p);
        c.add(name);
        return c;
    }

    static String key(List<String> p) {
        StringBuilder sb = new StringBuilder();
        for (String s : p) sb.append(s).append('\u0001');
        return sb.toString();
    }

    /** הצילום של האופן, או null (אין מסד / שגיאה). */
    static Snap snap(android.content.Context c, String mode) {
        SqlStore s = null;
        try {
            s = Db.openRead(c, mode);
            return Snap.of(s);
        } catch (Throwable t) {
            Log.e("DbLists.snap", t);
            return null;
        } finally {
            if (s != null) s.close();
        }
    }

    /** לטופס המקומות הפתוחים: כל המסלולים של הרשימות הידניות במסד, ואז החכמות. */
    static Map<String, List<String>> names(android.content.Context c) {
        Map<String, List<String>> out = new LinkedHashMap<String, List<String>>();
        Snap s = snap(c, mode(c));
        out.put("manual", s == null ? new ArrayList<String>() : s.paths());
        out.put("smart", s == null ? new ArrayList<String>() : s.smarts);
        return out;
    }

    // ================= איפה כל קובץ (מסך הכפולים) =================

    /** לכל מסלול של קובץ ברשימה — שמות הרשימות שהוא בהן (מסלול מלא, "א / ב"), כמו Tree.whereListed בישן. */
    static Map<String, List<String>> whereListed(Store s) {
        Snap n = Snap.of(s);
        Map<Long, String> label = new HashMap<Long, String>();
        for (Map.Entry<String, Long> e : n.ids.entrySet()) {
            String k = e.getKey();
            List<String> p = new ArrayList<String>(java.util.Arrays.asList(k.substring(0, Math.max(0, k.length() - 1)).split("\u0001")));
            String l = Names.fullLabel(p);
            String had = label.get(e.getValue());
            if (had == null || l.length() < had.length()) label.put(e.getValue(), l);   // רשימה משותפת — המסלול הקצר
        }
        Map<String, List<String>> out = new HashMap<String, List<String>>();
        for (Store.Item it : s.lists()) {
            if (it.id == n.home) continue;
            String l = label.containsKey(it.id) ? label.get(it.id) : it.name;
            for (Store.Member m : s.members(it.id)) {
                if (m.item.type != Store.FILE) continue;
                Store.FileInfo f = s.file(m.item.id);
                if (f == null) continue;
                List<String> ls = out.get(f.path);
                if (ls == null) out.put(f.path, ls = new ArrayList<String>());
                if (!ls.contains(l)) ls.add(l);
            }
        }
        return out;
    }

    /** במכשיר: של האופן הפתוח. ריק — אם אין מסד. */
    static Map<String, List<String>> whereListed(android.content.Context c) {
        SqlStore s = null;
        try {
            s = Db.openRead(c, mode(c));
            return whereListed(s);
        } catch (Throwable t) {
            Log.e("DbLists.whereListed", t);
            return new HashMap<String, List<String>>();
        } finally {
            if (s != null) s.close();
        }
    }

    // ================= עץ בזיכרון מהמסד (לחתימות) =================

    /** הרשימות של המסד כעץ בזיכרון (לא נשמר) — רשימה אחת לכל רשימה במסד, עם הקבצים שבה. למי שקורא עץ (Sig). */
    static Tree asTree(Store s) {
        Tree t = Tree.memory();
        for (Store.Item it : s.lists()) {
            Tree.Node n = Tree.newChild(t.root, it.name + " #" + it.id);
            for (Store.Member m : s.members(it.id)) {
                if (m.item.type != Store.FILE) continue;
                Store.FileInfo f = s.file(m.item.id);
                if (f != null) n.entries.add(new Tree.Entry(f.path, f.size));
            }
        }
        return t;
    }

    /** הרשימות שהחתימות עובדות עליהן: במסד — של המסד (האופן הפתוח); אחרת — העץ הישן. */
    static Tree listsForSig() {
        android.content.Context c = Db.app;
        if (c == null || !on(c)) {
            Tree none = Tree.memory();
            none.unreadable = true;                                           // אין מסד — לא מסיקים שום דבר
            return none;
        }
        SqlStore s = null;
        try {
            s = Db.openRead(c, mode(c));
            return asTree(s);
        } catch (Throwable t) {
            Log.e("DbLists.listsForSig", t);
            Tree e = Tree.memory();
            e.unreadable = true;                                              // לא נקרא — לא מוחקים חתימות
            return e;
        } finally {
            if (s != null) s.close();
        }
    }
}
