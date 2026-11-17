package com.tikiyot.vfm;

// מ0.10: הפקודות על רשימות, כשעובדים במסד — איתור רשימה לפי המסלול שבפקודה ("א/ב"), יצירה (צור רשימה), ורשימה חכמה לפי
// שם (פתח / צור / עדכן), באותם כללים כמו במנוע הישן (Verbs.ensureList, SmartLists.ensure / update).
// אחראי: הלוגיקה בלבד (בלי מסך — נבדק). אסור לו: לפתוח מסכים או מסדים.

import java.util.ArrayList;
import java.util.List;

final class DbPaths {

    private DbPaths() { }

    /** הרשימה במסלול, או 0 — אין, או שיש ספק (listAt אומר מה). */
    static long find(Store s, String mode, List<String> path) {
        Found f = listAt(s, mode, path);
        return f.error != null ? 0 : f.id;
    }

    /**
     * **מבנה 107 — פנייה לרשימה ידנית: הכלל האחד** (אותו עיקרון של מבנה 106; המשתמש: "כאשר רוצים לפנות לרשימה צריך לכתוב את
     * כל המסלול… אם אין ספק בכוונתו — התוכנה מקצרת, בודקת לבד ופועלת בהתאם; אם יש ספק — הוא צריך להחליט"). קודם — לפי המקור
     * (רשימה שהועתקה מהישן באותו מסלול). אחרת — לפי השמות: החלק הראשון — רשימה בשם הזה בראש (בבית, או שאינה בתוך שום רשימה);
     * אין בראש — הקיצור: אחת בשם הזה בכל המסד — היא; כמה — שגיאה ("יש כמה… לכתוב את המקום"). כל חלק הבא — בתוך הקודם (כמה
     * באותה רשימה — מנתונים ישנים — גם זה ספק). עד כאן: בראש — הראשונה; אין בראש — הראשונה בכל המסד, בשקט.
     */
    static Found listAt(Store s, String mode, List<String> path) {
        Found f = new Found();
        if (path == null || path.isEmpty()) {
            f.error = Words.ListCmdW.noList("");
            return f;
        }
        long o = s.findByOrigin(DbPaths.originOf(mode, Names.fullLabel(path)));
        Store.Item oi = o == 0 ? null : s.item(o);
        if (oi != null && oi.type == Store.LIST && oi.name.equals(path.get(path.size() - 1).trim())) {   // מ0.13.1: אחרי שינוי שם — לפי השמות
            f.id = o;
            return f;
        }
        String first = path.get(0).trim();
        long home = home(s);
        List<Long> at = new ArrayList<Long>();
        for (Store.Item it : s.lists()) {
            if (!it.name.equals(first) || it.id == home) continue;
            List<Long> in = new ArrayList<Long>(s.listsOf(it.id));
            in.remove(home);
            if (in.isEmpty()) at.add(it.id);
        }
        if (at.isEmpty())                                                       // הקיצור: לא בראש — בכל המסד
            for (Store.Item it : s.lists()) if (it.name.equals(first) && it.id != home) at.add(it.id);
        if (!many(s, f, at, first, null)) return f;
        long cur = at.get(0);
        for (int i = 1; i < path.size(); i++) {
            String n = path.get(i).trim();
            List<Long> kids = new ArrayList<Long>();
            for (Store.Member m : s.members(cur)) if (m.item.type == Store.LIST && m.item.name.equals(n)) kids.add(m.item.id);
            if (!many(s, f, kids, n, Names.key(path.subList(0, i)))) {
                if (f.error == null) f.error = Words.ListCmdW.noList(Names.key(path));
                return f;
            }
            cur = kids.get(0);
        }
        f.id = cur;
        return f;
    }

    /** true — בדיוק אחת. אף אחת — שגיאה "אין"; כמה — "יש כמה… לכתוב את המקום" (in — הדרך עד כאן, או null בחלק הראשון). */
    private static boolean many(Store s, Found f, List<Long> ids, String name, String in) {
        if (ids.size() == 1) return true;
        if (ids.isEmpty()) {
            f.error = Words.ListCmdW.noList(in == null ? name : Names.key(java.util.Arrays.asList(in, name)));
            return false;
        }
        List<String> places = new ArrayList<String>();
        String example = null;
        for (Long id : ids) {
            List<Long> of = s.listsOf(id);
            String where = of.isEmpty() ? Words.Db.STANDALONE : s.item(of.get(0)).name;
            places.add(where);
            if (example == null && !of.isEmpty() && in == null) example = Names.key(java.util.Arrays.asList(where, name));
        }
        f.error = Words.ListCmdW.listAmbiguous(name, places, example == null ? (in == null ? name : in + "/" + name) : example);
        f.ambiguous = true;
        return false;
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
                long cur = topOnly(t, first);                                   // מבנה 107: יוצרים בראש — רק מה שבראש, בלי קיצור
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
    /** מה נמצא: הרשימה שבה (in; 0 — עומדת לעצמה), הרשימה החכמה (id), או השגיאה. */
    static final class Found {
        long in, id;
        String error;
        /** יש כמה בשם הזה — המשתמש צריך להחליט (כותב את המקום). */
        boolean ambiguous;
    }

    /**
     * **מבנה 106 — פנייה לרשימה חכמה: הכלל האחד.** המשתמש (15.11.2026): "כאשר רוצים לפנות לרשימה צריך לכתוב את כל המסלול —
     * אופן, רשימה, רשימות משנה, הרשימה הנצרכת. רק בשביל לחסוך לכתוב: אם אין ספק בכוונתו, אז התוכנה מקצרת, בודקת לבד ופועלת
     * בהתאם. אם יש ספק — אז הוא צריך להחליט." · דרך עם רשימה ("ספרים/חדשים") — בדיוק שם. שם לבד — זו הדרך מהבית: יש בבית
     * רשימה חכמה בשם הזה — היא; אין — הקיצור: אחת בכל המסד — היא; כמה — שגיאה ("יש כמה… לכתוב את המקום"); אף אחת — "אין".
     * עד כאן שם לבד הביא בשקט את הראשונה במספר.
     */
    static Found smartAt(Store s, String mode, List<String> path) {
        Found f = new Found();
        String raw = Names.key(path == null ? new ArrayList<String>() : path);
        if (path == null || path.isEmpty()) {
            f.error = Words.ListCmdW.noSmart(raw);
            return f;
        }
        String name = path.get(path.size() - 1).trim();
        if (path.size() > 1) {
            List<String> up = path.subList(0, path.size() - 1);
            f.in = find(s, mode, up);
            if (f.in == 0) {
                f.error = Words.ListCmdW.noList(Names.key(up));
                return f;
            }
            f.id = Lists.smartIn(s, f.in, name);
            if (f.id == 0) f.error = Words.ListCmdW.noSmart(raw);
            return f;
        }
        long h = home(s);
        if (h != 0 && (f.id = Lists.smartIn(s, h, name)) != 0) {
            f.in = h;
            return f;
        }
        List<Store.Item> all = new ArrayList<Store.Item>();
        for (Store.Item it : s.smarts()) if (it.name.equals(name)) all.add(it);
        if (all.isEmpty()) {
            f.error = Words.ListCmdW.noSmart(raw);
            return f;
        }
        if (all.size() > 1) {
            List<String> places = new ArrayList<String>();
            String example = null;
            for (Store.Item it : all) {
                List<Long> of = s.listsOf(it.id);
                String where = of.isEmpty() ? Words.Db.STANDALONE : s.item(of.get(0)).name;
                places.add(where);
                if (example == null && !of.isEmpty()) example = Names.key(java.util.Arrays.asList(where, name));
            }
            f.error = Words.ListCmdW.smartAmbiguous(name, places, example == null ? name : example);
            f.ambiguous = true;
            return f;
        }
        f.id = all.get(0).id;
        List<Long> of = s.listsOf(f.id);
        f.in = of.isEmpty() ? 0 : of.get(0);
        return f;
    }

    // מבנה 103: "צור רשימה חכמה" (ensureSmart — אותו שם ואותו תנאי: הקיימת; תנאי אחר: "(2)") נמחק: הכלל האחד — Lists.newSmart.

    /** "עדכן רשימה חכמה": רק קיימת — התנאי (והמיון, אם נכתב). מבנה 106: הרשימה — לפי smartAt (דרך, או שם בלי ספק). */
    static Found updateSmart(Store s, String mode, List<String> path, final String cond, final String sort) {
        final Found f = smartAt(s, mode, path);
        if (f.error != null) return f;
        final long id = f.id;
        s.atomic(new Store.Work() {
            @Override
            public void run(Store t) {
                t.setProp(id, Store.P_COND, cond);
                if (sort != null && !sort.isEmpty()) t.setProp(id, Store.P_SORT, sort);
            }
        }, true);
        return f;
    }

    /** רשימה בשם הזה בראש: שאינה בתוך רשימה (חוץ מהבית), או 0. מבנה 107: בלי "ואם אין — כל רשימה בשם הזה". */
    private static long topOnly(Store s, String name) {
        String n = name.trim();
        long home = DbPaths.home(s);
        for (Store.Item it : s.lists()) {
            if (!it.name.equals(n) || it.id == home) continue;
            List<Long> in = new ArrayList<Long>(s.listsOf(it.id));
            in.remove(home);
            if (in.isEmpty()) return it.id;
        }
        return 0;
    }

    /** מבנה 102: רשימה בשם הזה שעומדת לעצמה (אינה בתוך שום רשימה, והיא אינה הבית) — או 0. למסד בלי בית. */
    static long standalone(Store s, String name) {
        String n = name.trim();
        for (Store.Item it : s.lists())
            if (it.name.equals(n) && s.listsOf(it.id).isEmpty() && !"1".equals(s.prop(it.id, Store.P_HOME))) return it.id;
        return 0;
    }

    /**
     * מבנה 243 (תוסף העיצוב — מהרשימה שעל המסך אל הפקודה): הדרך של רשימה, מהראש עד אליה — דרך הרשימה הראשונה שהיא בה (כמו
     * הירושה של העיצוב, Design.forList). הבית, או רשימה שאינה — ריקה.
     */
    static List<String> pathOf(Store s, long id) {
        List<String> out = new ArrayList<String>();
        java.util.Set<Long> seen = new java.util.HashSet<Long>();
        for (long at = id; at != 0 && seen.add(at); ) {
            if ("1".equals(s.prop(at, Store.P_HOME))) break;
            Store.Item it = s.item(at);
            if (it == null) return new ArrayList<String>();
            out.add(0, it.name);
            List<Long> up = s.listsOf(at);
            at = up.isEmpty() ? 0 : up.get(0);
        }
        return out;
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

    /**
     * מבנה 54: מסד שיש בו רשימות ואין בו בית — נוצר בית, ורשימות (וחכמות) שאינן בתוך שום רשימה נכנסות אליו. מסד שיש בו
     * בית — לא נוגעים בו. מאיפה מסד כזה: "צור אופן" של פעם, בשם שהיו לו גיבויים, השאיר מסד ריק בלי בית (ממצא 27), ופקודת
     * רשימות יצרה בו רשימה "בראש" — מחוץ לבית. גיבוי שלו ממשיך את המצב הזה, והמסך אמר "אין בית במסד הזה" (בדיקת נ0.4).
     */
    static long homeWithOrphans(Store s) {
        long had = home(s);
        if (had != 0) return had;
        final long h = ensureHome(s);
        List<Store.Item> roots = new ArrayList<Store.Item>(s.lists());
        roots.addAll(s.smarts());
        for (Store.Item it : roots)
            if (it.id != h && !Names.RESERVED_ROOT.equals(it.name) && s.listsOf(it.id).isEmpty()) s.addMember(h, it.id);
        return h;
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
