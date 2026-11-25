package com.tikiyot.vfm;

// הפעולות עצמן על הרשימות (מבנה 19, 13.11.2026) — לפי מספר זיהוי, על Store: ליצור (newList, newListChecked) · למחוק
// (deleteWhole) · לשנות (edit — שם, צלמית, תיאור; moveTo — מקום) · לחבר ולנתק (linkInto, removeFrom) · להעתיק (copyInto, copyIds).
// המשתמש (12.11.2026): "מה עם הפעולות — העתקה, העברה, יצירה, מחיקה. אני לא מתכוון לפקודות שהם אופני ביצוע הפעולות, אלא
// על הפעולות עצמן" · "ובפרט שהם לא רבות. רק ברשימות הם מתרבות ומתעמקות." ← הפעולות — עם החומר; הפקודה רק מבקשת.
// עד כאן ישבו בתוך DbListCmd (קומת הפקודות), וגם החומר עצמו (DbMore, DbExamples), הלב והמסכים פנו לשם. אותן שורות.
// הכלל (12.11): יכולת חדשה ברשימות נבנית קודם מצירוף הפעולות האלה; פעולה חדשה כאן — רק כשאי אפשר להרכיב אותה.
// אחראי: לבצע פעולה אחת על המסד, כעסקה. אסור לו: לשאול, לפענח שורת פקודה, לפתוח מסד (מקבל Store).

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class Lists {

    private Lists() { }

    private static final String[] LIST_PROPS = {Store.P_ICON, Store.P_DESC, Store.P_DESIGN, Store.P_WHY, Store.P_GROUP};
    private static final String[] SMART_PROPS = {Store.P_COND, Store.P_SORT, Store.P_DIR, Store.P_VIEW, Store.P_ICON,
            Store.P_DESC, Store.P_WHY};

    // ---------- הליבה לפי מספר זיהוי — הפקודות והמסכים (מ0.13) עוברים כאן ----------

    /** כמה נכנסו, כמה לא, ולמה (הסיבה הראשונה). */
    static final class Copied {
        int done, skipped;
        String why;
    }

    /** העתקה לתוך dest — בעסקה אחת: רשימה — עותק עצמאי; חכמה — עותק ההגדרה; רעיון — עותק; קובץ — אותו פריט. */
    static Copied copyInto(Store s, final List<Store.Item> what, final long dest) {
        final Copied c = new Copied();
        s.atomic(new Store.Work() {
            @Override public void run(Store t) {
                for (Store.Item it : what) {
                    String w = copyItem(t, it, dest);
                    if (w == null) c.done++;
                    else {
                        c.skipped++;
                        if (c.why == null) c.why = w;
                    }
                }
            }
        }, true);
        return c;
    }

    /** מ0.16: העתקה לפי מספרי זיהוי (מהמסך) — רשימה לא לתוך עצמה (נספרת כ"לא נכנסה"). */
    static Copied copyIds(Store s, List<Long> ids, long dest) {
        List<Store.Item> what = new ArrayList<Store.Item>();
        int self = 0;
        for (Long id : ids) {
            Store.Item it = s.item(id);
            if (it == null) continue;
            if (it.type == Store.LIST && (id == dest || s.contains(id, dest))) { self++; continue; }
            what.add(it);
        }
        Copied c = copyInto(s, what, dest);
        if (self > 0) {
            c.skipped += self;
            if (c.why == null) c.why = Words.ListCmdW.INTO_ITSELF;
        }
        return c;
    }

    /** מ0.16: קישור — אותו פריט גם ב-dest (רשימה משותפת); מעגל, או כבר בה (גם בתת־רשימה) — לא נכנס. בעסקה אחת. */
    static Copied linkInto(Store s, final List<Long> ids, final long dest) {
        final Copied c = new Copied();
        s.atomic(new Store.Work() {
            @Override public void run(Store t) {
                for (Long id : ids) {
                    String w = t.item(id) == null ? Words.DbCmd.ALREADY : id == dest || t.makesCycle(dest, id) ? Words.ListCmdW.INTO_ITSELF
                            : t.contains(dest, id) || !t.addMember(dest, id) ? Words.DbCmd.ALREADY : null;
                    if (w == null) c.done++;
                    else {
                        c.skipped++;
                        if (c.why == null) c.why = w;
                    }
                }
            }
        }, true);
        return c;
    }

    /** מ0.16: העברה — קישור ל-dest, ויציאה מ-from רק של מה שנכנס עכשיו. בעסקה אחת. */
    static Copied moveTo(Store s, final List<Long> ids, final long from, final long dest) {
        final Copied c = new Copied();
        s.atomic(new Store.Work() {
            @Override public void run(Store t) {
                for (Long id : ids) {
                    Copied one = linkInto(t, java.util.Collections.singletonList(id), dest);
                    c.done += one.done;
                    c.skipped += one.skipped;
                    if (c.why == null) c.why = one.why;
                    if (one.done == 1 && from != 0) t.removeMember(from, id);
                }
            }
        }, true);
        return c;
    }

    /** מ0.30 (מ20): מחיקת רשימה מכל מקום (מסך המסד) — ואיתה מה שהיה רק שלה, כמו בהסרה; בעסקה אחת. */
    static void deleteWhole(Store s, final long id) {
        s.atomic(new Store.Work() {
            @Override public void run(Store t) {
                Store.Item it = t.item(id);
                if (it == null) return;
                for (Long p : new ArrayList<Long>(t.listsOf(id))) t.removeMember(p, id);
                dropIfAlone(t, id);
            }
        }, true);
    }

    /** הסרה מרשימה — בעסקה אחת: יוצא מ-parent; רשימה שלא נשארה באף מקום — נמחקת עם מה שרק שלה; רעיון שהיה רק כאן — לסל. */
    static void removeFrom(Store s, final long parent, final long item) {
        s.atomic(new Store.Work() {
            @Override public void run(Store t) {
                Store.Item it = t.item(item);
                if (it == null) return;
                boolean idea = it.type == Store.NOTE || it.type == Store.TITLE;
                if (idea && t.listsOf(item).size() <= 1) {
                    t.trash(item);
                    return;
                }
                if (parent != 0) t.removeMember(parent, item);
                if (it.type == Store.LIST || it.type == Store.SMART) dropIfAlone(t, item);
            }
        }, true);
    }

    /**
     * מ0.23: הבדיקה האחת של שם לרשימה (ידנית או חכמה) — של REN, של "הגדרות" במסך ושל רשימה חדשה מ־➕: "/" או "\\" —
     * לא (לפני הניקוי, שמוחק אותם בשקט); ריק — לא; באחת הרשימות שהיא בתוכן (parents) כבר יש פריט אחר מאותו סוג באותו
     * שם — לא. self — הפריט עצמו (0 — חדש). מחזיר את השגיאה, או null.
     */
    static String nameError(Store s, List<Long> parents, long self, int type, String raw) {
        String name = raw == null ? "" : raw;
        if (name.contains("/") || name.contains("\\")) return Words.DbCmd.BAD_NAME;
        String n = type == Store.SMART ? name.trim() : Names.cleanName(name);
        // מבנה 92: שם שכולו רווחים (או רק "#") — ריק; עד כאן הניקוי הפך אותו ל"ללא שם", והרשימה נוצרה או שונתה לשם הזה
        if (n.isEmpty() || name.replace('#', ' ').trim().isEmpty()) return Words.DbCmd.BAD_NAME;
        for (Long p : parents) {
            if (p == null || p == 0) continue;
            for (Store.Member m : s.members(p))
                if (m.item.id != self && m.item.type == type && m.item.name.equals(n)) return Words.DbCmd.nameTaken(n);
        }
        return null;
    }

    /**
     * מ0.13.1: עריכה — שם (ריק — בלי שינוי), צלמית, תיאור, "למה"; בעסקה אחת. רעיון/כותרת — השם הוא הטקסט.
     * מ0.23: לרשימה ולחכמה — שם חדש עובר את nameError (כמו REN); שגיאה — שום שינוי, ומוחזרת. אחרת — null.
     */
    static String edit(Store s, final long id, final String name0, final String icon, final String desc, final String why) {
        final String name = name0 == null ? "" : name0;                          // מבנה 113: null — בלי שינוי (גם בצלמית, בתיאור וב"למה")
        Store.Item cur = s.item(id);
        if (cur == null) return null;
        if ((cur.type == Store.LIST || cur.type == Store.SMART) && !name.trim().isEmpty()) {
            String n = cur.type == Store.LIST ? Names.cleanName(name) : name.trim();
            if (!n.equals(cur.name) || name.contains("/") || name.contains("\\")) {
                String e = nameError(s, s.listsOf(id), id, cur.type, name);
                if (e != null) return e;
            }
        }
        s.atomic(new Store.Work() {
            @Override public void run(Store t) {
                Store.Item it = t.item(id);
                if (it == null) return;
                // מבנה 113: שם ריק — בלי שינוי בשם (עד כאן, לרשימה, הניקוי הפך שם ריק ל"ללא שם" — והרשימה קיבלה את השם הזה)
                String n = name.trim().isEmpty() ? "" : it.type == Store.LIST ? Names.cleanName(name) : name.trim();
                if (!n.isEmpty() && !n.equals(it.name)) t.rename(id, n);
                if (it.type == Store.NOTE || it.type == Store.TITLE || it.type == Store.FILE) return;
                if (icon != null) t.setProp(id, Store.P_ICON, icon.trim());
                if (desc != null) t.setProp(id, Store.P_DESC, desc.trim().replace('\n', ' '));
                if (why != null) t.setProp(id, Store.P_WHY, why.trim().replace('\n', ' '));
            }
        }, true);
        return null;
    }

    /** מבנה 97: המאפיינים שטופס הרשימה החכמה שומר, מלבד השם והתנאי — בסדר קבוע. */
    static final String[] SMART_FORM = {Store.P_SORT, Store.P_VIEW, Store.P_ICON, Store.P_DESC, Store.P_WHY};

    /**
     * מבנה 97: שמירה של טופס הרשימה החכמה — בעסקה אחת. smart = 0 — נוצרת בתוך into (0 — הבית, אם יש; אין בית — עומדת לעצמה);
     * אחרת — מתעדכנת (שם ריק — בלי שינוי בשם). props — לפי SMART_FORM; מה שחסר — נמחק (כמו בטופס). מחזיר את מספר הזיהוי.
     * עד כאן ישב במסך (DbSmartForm.save) — אותן שורות.
     */
    static long saveSmart(Store s, final long smart, long into, final String name, final String cond, final java.util.Map<String, String> props) {
        final long in = into != 0 ? into : DbPaths.home(s);
        final long[] out = {smart};
        s.atomic(new Store.Work() {
            @Override
            public void run(Store t) {
                if (out[0] == 0) out[0] = t.createSmart(name.isEmpty() ? Words.SMART_LIST : name, in);
                else if (!name.isEmpty()) t.rename(out[0], name);
                t.setProp(out[0], Store.P_COND, cond);
                for (String k : SMART_FORM) t.setProp(out[0], k, props == null ? null : props.get(k));
            }
        }, true);
        return out[0];
    }

    /**
     * **רשימה חכמה חדשה — הכלל האחד** (מבנה 103–104). המשתמש (15.11.2026): "לא נאפשר רשימות חכמות באותו שם. התוכנה תודיע
     * כמו בקובץ עם אותו שם: האם לדרוס, האם להחליף שם, האם לעצור." בתוך into (0 — הבית). יש בה כבר רשימה חכמה בשם הזה —
     * לפי taken (בקובץ — אותן מילים כמו בהעתקת קבצים, `/תפוס:`): TAKEN_STOP — שגיאה ("שם קיים"); TAKEN_REPLACE ("להחליף") —
     * הקיימת מקבלת את ההגדרות החדשות (דריסה; הגיבוי לפני — אצל מי שמריץ); TAKEN_RENAME ("לשנות-שם") — חדשה בשם פנוי ("(2)");
     * TAKEN_SKIP ("לדלג") — בלי שינוי. מסד בלי בית — גם רשימה חכמה שעומדת לעצמה באותו שם. שם ריק — "רשימה חכמה".
     */
    /** מבנה 104: מה עושים כששם קיים (הפקודות מתרגמות את "/תפוס:<ערך>" לאחד מאלה). */
    static final int TAKEN_STOP = 0, TAKEN_REPLACE = 1, TAKEN_RENAME = 2, TAKEN_SKIP = 3;

    static Made newSmart(Store s, long into, String name, String cond, java.util.Map<String, String> props, int taken) {
        Made r = new Made();
        String n = name == null || name.trim().isEmpty() ? Words.SMART_LIST : name.trim();
        long in = into != 0 ? into : DbPaths.home(s);
        long had = smartIn(s, in, n);
        r.name = n;
        if (had == 0) {
            r.id = saveSmart(s, 0, in, n, cond, props);
            return r;
        }
        if (taken == TAKEN_REPLACE) {
            r.id = saveSmart(s, had, 0, n, cond, props);
            r.replaced = true;
        } else if (taken == TAKEN_RENAME) {
            String free = n;
            for (int k = 2; smartIn(s, in, free) != 0; k++) free = n + " (" + k + ")";
            r.name = free;
            r.id = saveSmart(s, 0, in, free, cond, props);
            r.renamed = true;
        } else if (taken == TAKEN_SKIP) {
            r.id = had;
            r.skipped = true;
        } else r.error = Words.ListCmdW.smartTaken(n);
        return r;
    }

    /**
     * מבנה 105: רשימות חכמות כפולות שכבר קיימות (משלפני צעד 104) — הזוג הראשון ברשימה list: {הראשונה, השנייה} לפי המקום, או
     * null. המשתמש: "ולא לאפשר גם רשימות שהיו — להקפיץ את אותה שאלה" · "כמו כאשר מנסים ליצור רשימה חדשה עם אותו שם": השנייה
     * היא "החדשה", והראשונה — "הקיימת".
     */
    static long[] smartTwin(Store s, long list) {
        java.util.Map<String, Long> seen = new java.util.HashMap<String, Long>();
        for (Store.Member m : s.members(list)) {
            if (m.item.type != Store.SMART) continue;
            Long had = seen.get(m.item.name);
            if (had != null) return new long[]{had, m.item.id};
            seen.put(m.item.name, m.item.id);
        }
        return null;
    }

    /**
     * מבנה 289 (אפיון החיפוש, 6 — התבניות; המשתמש: "1"): מה ש"📥 לטעון" שבחלונית מציע — המוכנות (SmartLists.PRESETS), ואחריהן
     * הרשימות החכמות של האופן, לפי השם. לכל אחת: {שם, תנאי, מיון, תצוגה, "1" — מוכנה}. s = null — רק המוכנות. רשימה חכמה בלי
     * תנאי — לא (אין מה לטעון).
     */
    static List<String[]> templates(Store s) {
        List<String[]> out = new ArrayList<String[]>();
        for (String[] p : SmartLists.PRESETS) out.add(new String[]{p[0], p[1], p[2], p[4], "1"});
        if (s == null) return out;
        List<String[]> own = new ArrayList<String[]>();
        for (Store.Item it : s.smarts()) {
            String c = s.prop(it.id, Store.P_COND);
            if (c == null || c.trim().isEmpty()) continue;
            String so = s.prop(it.id, Store.P_SORT), v = s.prop(it.id, Store.P_VIEW);
            own.add(new String[]{it.name, c, so == null ? "" : so, v == null ? "" : v, ""});
        }
        Collections.sort(own, new java.util.Comparator<String[]>() {
            @Override public int compare(String[] x, String[] y) { return x[0].compareTo(y[0]); }
        });
        out.addAll(own);
        return out;
    }

    /**
     * מבנה 307: אחרי שינוי שם של רשימה ידנית — הרשימות החכמות של האופן שמחפשות בה ("רשימה:", DbScope.renamed) מקבלות את השם
     * החדש, בעסקה אחת. מחזיר את השמות שלהן (להודעה). רק באופן של הרשימה; רשימה חכמה באופן אחר שפונה לכאן — לא.
     */
    static List<String> scopeRenamed(Store s, String mode, long id, String from, String to) {
        List<String> done = new ArrayList<String>();
        Store.Item it = s.item(id);
        if (it == null || it.type != Store.LIST) return done;
        final java.util.Map<Long, String> now = new java.util.LinkedHashMap<Long, String>();
        for (Store.Item sm : s.smarts()) {
            String n = DbScope.renamed(s.prop(sm.id, Store.P_COND), s, mode, id, from, to);
            if (n == null) continue;
            now.put(sm.id, n);
            done.add(sm.name);
        }
        if (now.isEmpty()) return done;
        s.atomic(new Store.Work() {
            @Override
            public void run(Store t) {
                for (java.util.Map.Entry<Long, String> e : now.entrySet()) t.setProp(e.getKey(), Store.P_COND, e.getValue());
            }
        }, true);
        return done;
    }

    /** מבנה 105: מה שבטופס של רשימה חכמה קיימת — לפי SMART_FORM (כדי לתת לאחרת את ההגדרות שלה). */
    static java.util.Map<String, String> smartProps(Store s, long id) {
        java.util.Map<String, String> m = new java.util.HashMap<String, String>();
        for (String k : SMART_FORM) m.put(k, s.prop(id, k));
        return m;
    }

    /** מבנה 104: הרשימה החכמה בשם name שב־in (in = 0 — מסד בלי בית: שעומדת לעצמה), או 0. */
    static long smartIn(Store s, long in, String name) {
        if (in != 0) {
            for (Store.Member m : s.members(in)) if (m.item.type == Store.SMART && m.item.name.equals(name)) return m.item.id;
            return 0;
        }
        for (Store.Item it : s.smarts()) if (it.name.equals(name) && s.listsOf(it.id).isEmpty()) return it.id;
        return 0;
    }

    /** מבנה 103: יש ב־in רשימה חכמה בשם name (in = 0 — מסד בלי בית: רשימה חכמה שעומדת לעצמה). */
    static boolean smartTaken(Store s, long in, String name) {
        return smartIn(s, in, name) != 0;
    }

    /** רשימה ידנית חדשה בתוך parent (0 — בבית, אם יש). בלי בדיקת שם — למי שכבר בדק (ייבוא, העתקה). */
    static long newList(Store s, final long parent, final String name) {
        final long into = parent != 0 ? parent : DbPaths.home(s);
        final long[] out = {0};
        s.atomic(new Store.Work() {
            @Override public void run(Store t) { out[0] = t.createList(Names.cleanName(name), into); }
        }, true);
        return out[0];
    }

    /** מה יצא מ־newListChecked: הרשימה, או השגיאה. */
    static final class Made {
        long id;
        String error;
        /** מבנה 104: רשימה חכמה — השם שנקבע בסוף; נדרסה / נוצרה בשם אחר / דילגו. */
        String name;
        boolean replaced, renamed, skipped;
    }

    /**
     * **רשימה חדשה — הכלל האחד** (מבנה 102; המשתמש, 15.11.2026: "לסרב בהודעה שם קיים" · "גם בפקודות לעצור… הוראה שלא
     * מתקיימת דינה לעצור את ההרצה, אלא אם כן יש הוראה מפורשת להמשיך"). בתוך parent (0 — הבית); הבדיקה האחת של שם
     * (nameError): שם תפוס באותה רשימה, ריק, או עם "/" — סירוב, ושום דבר לא נוצר. מסד בלי בית — גם רשימה שעומדת לעצמה באותו
     * שם היא "תפוס". ה־➕, הבורר, הכפתור "צור רשימה" ושתי השורות הכתובות ("צור רשימה", "צור תיקייה ידנית") — כולם כאן.
     */
    static Made newListChecked(Store s, long parent, String name) {
        return newListChecked(s, parent, name, TAKEN_STOP);
    }

    /** מבנה 109: taken — כשהשם קיים: TAKEN_STOP (סירוב), TAKEN_SKIP (הקיימת, בלי שינוי), TAKEN_RENAME (בשם פנוי "(2)"). */
    static Made newListChecked(Store s, long parent, String name, int taken) {
        Made r = new Made();
        long into0 = parent != 0 ? parent : DbPaths.home(s);
        String clean = Names.cleanName(name);
        long had = into0 != 0 ? DbPaths.child(s, into0, clean) : DbPaths.standalone(s, clean);
        if (had != 0 && taken == TAKEN_SKIP) {
            r.id = had;
            r.skipped = true;
            r.name = clean;
            return r;
        }
        if (had != 0 && taken == TAKEN_RENAME) {
            String free = clean;
            for (int k = 2; (into0 != 0 ? DbPaths.child(s, into0, free) : DbPaths.standalone(s, free)) != 0; k++) free = clean + " (" + k + ")";
            name = free;
            r.renamed = true;
        }
        long into = parent != 0 ? parent : DbPaths.home(s);
        List<Long> ps = new ArrayList<Long>();
        ps.add(into);
        r.error = nameError(s, ps, 0, Store.LIST, name);
        if (r.error == null && into == 0 && DbPaths.standalone(s, Names.cleanName(name)) != 0) r.error = Words.DbCmd.nameTaken(Names.cleanName(name));
        if (r.error == null) r.id = newList(s, into, name);
        r.name = Names.cleanName(name);
        return r;
    }

    /**
     * מבנה 102: רשימה חדשה לפי דרך של שמות ("א/ב/ג") — אותו כלל (newListChecked) על החלק האחרון; מה שבדרך ("א/ב") — כמו
     * שהפקודות מוצאות אותו (DbPaths.find), וחסר — נוצר. שם לא תקין בחלק האחרון — סירוב לפני שנוצר משהו בדרך.
     */
    static Made newListAt(Store s, String mode, List<String> path) {
        return newListAt(s, mode, path, TAKEN_STOP);
    }

    static Made newListAt(Store s, String mode, List<String> path, int taken) {
        Made r = new Made();
        if (path == null || path.isEmpty()) {
            r.error = Words.DbCmd.BAD_NAME;
            return r;
        }
        String last = path.get(path.size() - 1);
        r.error = nameError(s, new ArrayList<Long>(), 0, Store.LIST, last);
        if (r.error != null) return r;
        long parent = 0;
        if (path.size() > 1) {
            List<String> up = path.subList(0, path.size() - 1);
            DbPaths.Found at = DbPaths.listAt(s, mode, up);
            if (at.ambiguous) {                                                 // מבנה 107: יש ספק בדרך — לא יוצרים דרך חדשה
                r.error = at.error;
                return r;
            }
            parent = at.id;
            if (parent == 0) parent = DbPaths.ensure(s, mode, up);
            if (parent == 0) {
                r.error = Words.DbCmd.BAD_NAME;
                return r;
            }
        }
        return newListChecked(s, parent, last, taken);
    }


    /** פריט אחד לתוך into; null — נכנס, אחרת — למה לא. */
    private static String copyItem(Store t, Store.Item it, long into) {
        if (it.type == Store.LIST) {
            for (Store.Member m : t.members(into))
                if (m.item.type == Store.LIST && m.item.name.equals(it.name)) return Words.DbCmd.SAME_NAME;
            copyList(t, it.id, into);
            return null;
        }
        if (it.type == Store.SMART) {
            copySmart(t, it.id, into);
            return null;
        }
        if (it.type == Store.NOTE) {
            t.addNote(into, it.name);
            return null;
        }
        if (it.type == Store.TITLE) {
            t.addTitle(into, it.name);
            return null;
        }
        if (t.contains(into, it.id)) return Words.DbCmd.ALREADY;
        return t.addMember(into, it.id) ? null : Words.DbCmd.ALREADY;
    }

    /**
     * עותק עצמאי של רשימה, עד הסוף: כל תת־רשימה — עותק (גם רשימה שנמצאת גם במקום אחר); חכמה — עותק של ההגדרה.
     * מ0.24 (המשתמש: "העתק" — תמיד עותק; קישור — "צור קישור", בפקודות): קודם רשימה משותפת נשארה אותה רשימה.
     */
    private static long copyList(Store t, long src, long into) {
        return copyList(t, src, into, 0);
    }

    private static long copyList(Store t, long src, long into, int depth) {
        Store.Item it = t.item(src);
        long id = t.createList(it.name, into);
        copyProps(t, src, id, LIST_PROPS);
        for (Store.Member m : t.members(src)) {
            Store.Item x = m.item;
            if (x.type == Store.LIST) {
                if (depth < 60) copyList(t, x.id, id, depth + 1);
            } else if (x.type == Store.SMART) copySmart(t, x.id, id);
            else if (x.type == Store.NOTE) t.addNote(id, x.name);
            else if (x.type == Store.TITLE) t.addTitle(id, x.name);
            else t.addMember(id, x.id);
        }
        return id;
    }

    private static long copySmart(Store t, long src, long into) {
        long id = t.createSmart(t.item(src).name, into);
        copyProps(t, src, id, SMART_PROPS);
        return id;
    }

    private static void copyProps(Store t, long from, long to, String[] keys) {
        for (String k : keys) {
            String v = t.prop(from, k);
            if (v != null && !v.isEmpty()) t.setProp(to, k, v);
        }
    }

    /** רשימה (או חכמה) שלא נשארה באף רשימה — נמחקת, ואיתה מה שהיה רק שלה (רשימות; רעיונות וכותרות — לסל). */
    static void dropIfAlone(Store t, long id) {
        Store.Item it = t.item(id);
        if (it == null || !t.listsOf(id).isEmpty() || "1".equals(t.prop(id, Store.P_HOME))) return;
        List<Store.Member> was = it.type == Store.LIST ? t.members(id) : Collections.<Store.Member>emptyList();
        for (Store.Member m : was)
            if (m.item.type == Store.NOTE || m.item.type == Store.TITLE) {
                if (t.listsOf(m.item.id).size() == 1) t.trash(m.item.id);
            }
        t.deleteList(id);
        for (Store.Member m : was)
            if (m.item.type == Store.LIST || m.item.type == Store.SMART) dropIfAlone(t, m.item.id);
    }
}
