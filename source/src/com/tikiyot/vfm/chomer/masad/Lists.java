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
        if (n.isEmpty()) return Words.DbCmd.BAD_NAME;
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
    static String edit(Store s, final long id, final String name, final String icon, final String desc, final String why) {
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
                String n = it.type == Store.LIST ? Names.cleanName(name) : name.trim();
                if (!n.isEmpty() && !n.equals(it.name)) t.rename(id, n);
                if (it.type == Store.NOTE || it.type == Store.TITLE || it.type == Store.FILE) return;
                t.setProp(id, Store.P_ICON, icon.trim());
                t.setProp(id, Store.P_DESC, desc.trim().replace('\n', ' '));
                t.setProp(id, Store.P_WHY, why.trim().replace('\n', ' '));
            }
        }, true);
        return null;
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
    }

    /** מ0.23: רשימה חדשה מהמסך (➕, הבורר) — אחרי nameError, כמו MD: שם תפוס באותה רשימה — שגיאה, לא כפולה. */
    static Made newListChecked(Store s, long parent, String name) {
        Made r = new Made();
        long into = parent != 0 ? parent : DbPaths.home(s);
        List<Long> ps = new ArrayList<Long>();
        ps.add(into);
        r.error = nameError(s, ps, 0, Store.LIST, name);
        if (r.error == null) r.id = newList(s, into, name);
        return r;
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
