package com.tikiyot.vfm;

// מ0.10: הפקודות על רשימות, כשעובדים במסד — איתור רשימה לפי המסלול שבפקודה ("א/ב"), יצירה (צור רשימה), ורשימה חכמה לפי
// שם (פתח / צור / עדכן), באותם כללים כמו במנוע הישן (Verbs.ensureList, SmartLists.ensure / update).
// אחראי: הלוגיקה בלבד (בלי מסך — נבדק). אסור לו: לפתוח מסכים או מסדים.

import java.util.ArrayList;
import java.util.List;

final class DbPaths {

    private DbPaths() { }

    /**
     * הרשימה במסלול, או 0. קודם — לפי המקור (רשימה שהועתקה מהישן באותו מסלול); אחרת — הליכה לפי השמות: החלק הראשון —
     * רשימה בשם הזה שאינה בתוך רשימה אחרת (חוץ מהבית), ואם אין — כל רשימה בשם הזה; כל חלק הבא — רשימה בשם הזה בתוך הקודמת.
     */
    static long find(Store s, String mode, List<String> path) {
        if (path == null || path.isEmpty()) return 0;
        long o = s.findByOrigin(DbPaths.originOf(mode, Names.fullLabel(path)));
        Store.Item oi = o == 0 ? null : s.item(o);
        if (oi != null && oi.type == Store.LIST && oi.name.equals(path.get(path.size() - 1).trim())) return o;   // מ0.13.1: אחרי שינוי שם — לפי השמות
        long cur = top(s, path.get(0));
        for (int i = 1; i < path.size() && cur != 0; i++) cur = child(s, cur, path.get(i));
        return cur;
    }

    /** "צור רשימה": קיימת — כמו שהיא; חסרה — נוצרת בכל הדרך (חלק ראשון — עומד לעצמו). בעסקה אחת. מחזיר את הרשימה. */
    static long ensure(Store s, final String mode, final List<String> path) {
        if (path == null || path.isEmpty()) return 0;
        long had = find(s, mode, path);
        if (had != 0) return had;
        final long[] out = {0};
        s.atomic(new Store.Work() {
            @Override
            public void run(Store t) {
                String first = Names.cleanName(path.get(0));
                long cur = top(t, first);
                if (cur == 0) cur = t.createList(first, DbPaths.home(t));    // מ0.10.1: חדשה בראש — בבית (אם יש)
                for (int i = 1; i < path.size(); i++) {
                    String name = Names.cleanName(path.get(i));
                    long c = child(t, cur, name);
                    cur = c != 0 ? c : t.createList(name, cur);
                }
                out[0] = cur;
            }
        }, true);
        return out[0];
    }

    /** רשימה חכמה לפי השם (הראשונה במספר זיהוי), או 0. */
    static long smart(Store s, String name) {
        String n = name == null ? "" : name.trim();
        for (Store.Item it : s.smarts()) if (it.name.equals(n)) return it.id;
        return 0;
    }

    /**
     * "צור רשימה חכמה": אותו שם ואותו תנאי — הקיימת; אותו שם ותנאי אחר — "שם (2)" (ואם תפוס — 3…), כמו במנוע הישן.
     * עומדת לעצמה. בעסקה אחת. מחזיר את הרשימה החכמה.
     */
    static long ensureSmart(Store s, String name, final String cond, final String sort) {
        final String base = name == null ? "" : name.trim();
        long had = smart(s, base);
        if (had != 0 && same(s.prop(had, Store.P_COND), cond)) return had;
        String n = base;
        for (int k = 2; smart(s, n) != 0; k++) n = base + " (" + k + ")";
        final String free = n;
        final long[] out = {0};
        s.atomic(new Store.Work() {
            @Override
            public void run(Store t) {
                long id = t.createSmart(free, 0);
                t.setProp(id, Store.P_COND, cond);
                t.setProp(id, Store.P_SORT, sort);
                out[0] = id;
            }
        }, true);
        return out[0];
    }

    /** "עדכן רשימה חכמה": רק קיימת — התנאי (והמיון, אם נכתב). 0 — אין כזו. */
    static long updateSmart(Store s, String name, final String cond, final String sort) {
        final long id = smart(s, name);
        if (id == 0) return 0;
        s.atomic(new Store.Work() {
            @Override
            public void run(Store t) {
                t.setProp(id, Store.P_COND, cond);
                if (sort != null && !sort.isEmpty()) t.setProp(id, Store.P_SORT, sort);
            }
        }, true);
        return id;
    }

    private static boolean same(String a, String b) {
        return (a == null ? "" : a.trim()).equals(b == null ? "" : b.trim());
    }

    /** רשימה בשם הזה בראש: שאינה בתוך רשימה (חוץ מהבית); אם אין — כל רשימה בשם הזה. */
    private static long top(Store s, String name) {
        String n = name.trim();
        long home = DbPaths.home(s), any = 0;
        for (Store.Item it : s.lists()) {
            if (!it.name.equals(n) || it.id == home) continue;
            List<Long> in = new ArrayList<Long>(s.listsOf(it.id));
            in.remove(home);
            if (in.isEmpty()) return it.id;
            if (any == 0) any = it.id;
        }
        return any;
    }

    static long child(Store s, long list, String name) {
        String n = name.trim();
        for (Store.Member m : s.members(list)) if (m.item.type == Store.LIST && m.item.name.equals(n)) return m.item.id;
        return 0;
    }

    // ---------- מ0.29: מה שהיה ב־Importer ונשאר אחרי מחיקת המנוע הישן ----------

    /** כונן שלא זוהה ממסלולו. */
    static final String UNKNOWN_DRIVE = "?";

    /** רשימת הבית במסד (לפי הסימון), או 0. */
    static long home(Store s) {
        for (Store.Item it : s.lists()) if ("1".equals(s.prop(it.id, Store.P_HOME))) return it.id;
        return 0;
    }

    /** רשימת הבית; אין — נוצרת (מסד חדש). */
    static long ensureHome(Store s) {
        long h = home(s);
        if (h != 0) return h;
        final long[] out = {0};
        s.atomic(new Store.Work() {
            @Override
            public void run(Store t) {
                out[0] = t.createList(Words.Db.HOME_NAME, 0);
                t.setProp(out[0], Store.P_HOME, "1");
            }
        }, true);
        return out[0];
    }

    /** הכונן של מסלול: "פנימי", או שם הכרטיס. */
    static String driveOf(String path) {
        if (Drive.internalPath(path)) return Drive.INTERNAL;
        if (path.startsWith("/storage/")) {
            int e = path.indexOf('/', 9);
            String v = e < 0 ? path.substring(9) : path.substring(9, e);
            if (!v.isEmpty() && !v.equals("emulated") && !v.equals("self")) return v;
        }
        return UNKNOWN_DRIVE;
    }

    /**
     * מפתח המקור של רשימה שהגיעה מהמנוע הישן (בהעתקה שנעשתה עד מ0.28): שם האופן והמסלול המלא. רשימות ישנות במסד עדיין
     * נושאות אותו, ו־find מוצא לפיו קודם (אחרי שינוי שם — לפי השמות).
     */
    static String originOf(String mode, String listPath) {
        return "tree:" + mode + ":L:" + listPath;  // תחביר
    }
}
