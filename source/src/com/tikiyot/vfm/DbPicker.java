package com.tikiyot.vfm;

// מ0.23: בורר רשימה מהמסד — במקום ListPicker (עץ הרשימות הישן) בכל מקום שבוחרים רשימה כשעובדים במסד: "לרשימה" במסכי
// הקבצים, שיתוף מאפליקציה אחרת, טופס המקומות הפתוחים בפקודות, כפתור בשורה העליונה. אותו חלון בדיוק (PickDialog) —
// רק המקור אחר (צילום של המסד: מהבית ומטה, רשימות ידניות בלבד), ויצירת רשימה חדשה דרך הליבה (DbListCmd.newList).
// אחראי: הצילום (Snap — נבדק במחשב) והחלון. אסור לו: לשנות רשימות חוץ מיצירת רשימה חדשה.

import android.app.Activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class DbPicker {

    interface Picked {
        /** list — מספר הרשימה במסד; path — המסלול בשמות (כמו בפקודות). */
        void ok(String mode, long list, List<String> path);
    }

    private DbPicker() { }

    /** האם בוחרים מהמסד: בבניית מ, לא בצד הישן, ויש מסד לאופן. */
    static boolean on(android.content.Context c) {
        if (!Flavor.M) return false;
        String m = mode(c);
        return m != null && !m.isEmpty() && DbScreen.dbFile(c, m).isFile();
    }

    /** האופן: של מסך המסד הפתוח, ואם אין — מה שנבחר בפעם הקודמת. */
    static String mode(android.content.Context c) {
        return DbListScreen.currentMode != null ? DbListScreen.currentMode : DbScreen.savedMode(c);
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
                    smarts.add(path.isEmpty() ? m.item.name : ListCmd.pathText(path) + "\\" + m.item.name);
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
                out.add(ListCmd.pathText(q));
                collect(q, out);
            }
        }

        private static String unique(String name, Map<String, Integer> seen) {
            Integer k = seen.get(name);
            seen.put(name, k == null ? 1 : k + 1);
            return k == null ? name : name + " (" + (k + 1) + ")";
        }
    }

    private static List<String> child(List<String> p, String name) {
        List<String> c = new ArrayList<String>(p);
        c.add(name);
        return c;
    }

    static String keyOf(List<String> p) {
        return key(p);
    }

    private static String key(List<String> p) {
        StringBuilder sb = new StringBuilder();
        for (String s : p) sb.append(s).append('\u0001');
        return sb.toString();
    }

    /** הצילום של האופן, או null (אין מסד / שגיאה). */
    static Snap snap(android.content.Context c, String mode) {
        SqlStore s = null;
        try {
            s = SqlStore.openReadOnly(DbScreen.dbFile(c, mode));
            return Snap.of(s);
        } catch (Throwable t) {
            Log.e("DbPicker.snap", t);
            return null;
        } finally {
            if (s != null) s.close();
        }
    }

    /** מקור הבורר — מהצילום. */
    private static final class Source extends PickSource {
        final Snap snap;

        Source(Snap snap) {
            this.snap = snap;
        }

        @Override
        List<String> children(List<String> path) {
            List<String> k = snap.kids.get(key(path));
            return k == null ? null : new ArrayList<String>(k);
        }

        @Override
        boolean large() {
            return false;
        }

        @Override
        String fullLabel(List<String> path) {
            return Names.fullLabel(path);
        }

        @Override
        String whyNot(List<String> path) {
            return path.isEmpty() ? Words.Db.PICK_NOT_HOME : null;
        }
    }

    // ================= החלון =================

    static void choose(final Activity a, final String title, final Picked cb) {
        final String mode = mode(a);
        final Snap snap = snap(a, mode);
        if (snap == null) {
            Ui.toast(a, Words.Db.NO_DB_HERE);
            return;
        }
        PickerModel m = new PickerModel(new Source(snap), null, null, null);
        PickDialog.Creator creator = new PickDialog.Creator() {
            @Override
            public void create(final Activity act, final ArrayList<String> parent, final PickDialog.Chosen done) {
                Ui.askText(act, Words.NEW_LIST, "", Words.ListP.LIST_NAME, new Ui.Text1() {
                    @Override
                    public void ok(final String name) {
                        if (name.trim().isEmpty()) return;
                        final long in = parent.isEmpty() ? snap.home : snap.id(parent);
                        final String[] err = {null};
                        final long id = DbVerbs.write(act, mode, new DbVerbs.Job() {
                            @Override
                            public long run(Store s, List<String> p) {
                                DbListCmd.Made r = DbListCmd.newListChecked(s, in, name);
                                err[0] = r.error;
                                return r.id;
                            }
                        }, null);
                        if (err[0] != null) {
                            Ui.toast(act, err[0]);
                            return;
                        }
                        if (id == 0) return;
                        ArrayList<String> p = new ArrayList<String>(parent);
                        p.add(Names.cleanName(name));
                        snap.ids.put(key(p), id);
                        done.ok(p);
                    }
                });
            }
        };
        PickDialog.Chosen chosen = new PickDialog.Chosen() {
            @Override
            public void ok(ArrayList<String> path) {
                long id = snap.id(path);
                if (id == 0) {
                    Ui.toast(a, Words.LIST_NOT_FOUND);
                    return;
                }
                cb.ok(mode, id, path);
            }
        };
        PickDialog.show(a, title, m, PickDialog.LISTS, creator, chosen, null);
    }

    // ================= הוספת קבצים (המשותף לכל "לרשימה") =================

    /** הקבצים לרשימה שנבחרה, בעסקה אחת, עם חתימה — DbFiles.add, אותה ליבה של ➕ ושל COPY. ההודעה — כמה נכנסו. */
    static void addFiles(final Activity a, final List<String> paths, final Runnable after) {
        choose(a, Words.ADD_TO_LIST + " · " + Words.NO_TOUCH, new Picked() {
            @Override
            public void ok(String mode, long list, List<String> path) {
                addPicked(a, mode, list, path, paths, after);
            }
        });
    }

    /** אחרי שנבחרה רשימה: הקבצים אליה (DbFiles.add, עם חתימה), והודעה כמה נכנסו. */
    static void addPicked(Activity a, String mode, final long list, List<String> path, final List<String> paths, Runnable after) {
        final DbFiles.Added[] r = {null};
        DbVerbs.write(a, mode, new DbVerbs.Job() {
            @Override
            public long run(Store s, List<String> p) {
                r[0] = DbFiles.add(s, list, paths, null, true);
                return list;
            }
        }, null);
        if (r[0] != null) Ui.toast(a, Words.Db.addedTo(r[0].added, r[0].already, Names.fullLabel(path)));
        if (after != null) after.run();
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
            s = SqlStore.openReadOnly(DbScreen.dbFile(c, mode(c)));
            return whereListed(s);
        } catch (Throwable t) {
            Log.e("DbPicker.whereListed", t);
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
                if (f != null) n.entries.add(new Tree.Entry(f.path, f.size, false));
            }
        }
        return t;
    }

    /** הרשימות שהחתימות עובדות עליהן: במסד — של המסד (האופן הפתוח); אחרת — העץ הישן. */
    static Tree listsForSig() {
        android.content.Context c = DbListScreen.app;
        if (c == null || !on(c)) return Tree.load();
        SqlStore s = null;
        try {
            s = SqlStore.openReadOnly(DbScreen.dbFile(c, mode(c)));
            return asTree(s);
        } catch (Throwable t) {
            Log.e("DbPicker.listsForSig", t);
            Tree e = Tree.memory();
            e.unreadable = true;                                              // לא נקרא — לא מוחקים חתימות
            return e;
        } finally {
            if (s != null) s.close();
        }
    }
}
