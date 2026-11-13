package com.tikiyot.vfm;

// מ0.12: פקודות הרשימות על המסד (MD, RD/DEL, COPY, COMP — אותן מילות סוג כמו ListCmd: ידנית · חכמה · תוכן ידנית ·
// קבצי ידנית · קבצי חכמה · תיקייה; "*" — הבית), כשעובדים במסד (DbVerbs.active). אותו נוסח בקובץ הפקודות — רק
// האחסון אחר (המשתמש: "הפקודות פונות לרשימה בשמה ובמקומה במבנה — לא לאחסון שלה").
// ההדמיה: אותה פעולה בדיוק, על עותק של המסד בזיכרון (copyOf) — לא נוגע במסד האמיתי ולא משאיר כלום; הטקסט של ההדמיה
// הוא התוצאה שתהיה. בהרצה: המסד נפתח לרגע, ולפני פעולה שמוציאה — גיבוי "לפני פעולה".
// כללים שאין להם מקבילה בישן (במסד אין "הורה"): רשימה שבתוך רשימה אחת בלבד — "שלה": בהעתקה מועתקת,
// במחיקה נמחקת איתה; רשימה שגם במקום אחר — בהעתקה נכנסת כמו שהיא (אותה רשימה), במחיקה רק יוצאת מכאן.
// רשימה חכמה בהעתקה — עותק עצמאי של ההגדרה (כמו בישן). מחיקה של רשימה שאינה קיימת — בלי שינוי (לא שגיאה), כדי
// שקובץ בדיקה יוכל להתחיל בניקוי.
// אחראי: הלוגיקה (על Store — נבדק במחשב על שני המימושים) והחיבור לתכנון של Batch.

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class DbListCmd {

    private DbListCmd() { }

    /** הקבצים שתנאי מוצא (מהאינדקס); null — האינדקס עוד לא מוכן. */
    interface Finder {
        List<String> find(String cond);
    }

    /** תוצאה של פעולה: טקסט, או שגיאה; noop — בלי שינוי. */
    static final class Res {
        String text, error;
        boolean noop;
        /** מ0.42 (ב8): רשימה או חכמה ששינתה שם — רק בהרצה אמיתית (realRes) קובצי הפקודות שפונים אליה מתעדכנים. */
        long renamed;
        String renamedFrom, renamedTo;

        Res renamed(long id, String from, String to) {
            if (id != 0 && from != null && to != null && !from.equals(to)) {
                renamed = id;
                renamedFrom = from;
                renamedTo = to;
            }
            return this;
        }

        static Res err(String e) {
            Res r = new Res();
            r.error = e;
            return r;
        }

        static Res ok(String t) {
            Res r = new Res();
            r.text = t;
            return r;
        }

        static Res same(String t) {
            Res r = ok(t);
            r.noop = true;
            return r;
        }
    }

    /** פעולה אחת על מסד (הדמיה או אמיתי). */
    interface Work {
        Res run(Store s, Finder f);
    }

    private static final String[] LIST_PROPS = {Store.P_ICON, Store.P_DESC, Store.P_DESIGN, Store.P_WHY, Store.P_GROUP};
    private static final String[] SMART_PROPS = {Store.P_COND, Store.P_SORT, Store.P_DIR, Store.P_VIEW, Store.P_ICON,
            Store.P_DESC, Store.P_WHY};
    private static final String[] ALL_PROPS = {Store.P_ICON, Store.P_DESC, Store.P_DESIGN, Store.P_WHY, Store.P_COND,
            Store.P_SORT, Store.P_DIR, Store.P_VIEW, Store.P_GROUP, Store.P_HOME};

    // ---------- מציאה ----------

    /** הרשימה הידנית של הצד: "*" — הבית; אחרת לפי המסלול (DbPaths). 0 — אין. */
    static long list(Store s, String mode, ListCmd.Side x) {
        if (x.home) return DbPaths.home(s);
        return DbPaths.find(s, mode, x.path);
    }

    /** רשימה חכמה: {הרשימה שבה (0 — עומדת לעצמה), החכמה}, או null. בלי דרך — בבית, ואם אין שם — לפי השם בכל המסד. */
    static long[] smart(Store s, String mode, ListCmd.Side x) {
        if (x.path == null || x.path.isEmpty()) return null;
        String name = x.path.get(x.path.size() - 1).trim();
        List<String> parent = new ArrayList<String>(x.path.subList(0, x.path.size() - 1));
        long in = parent.isEmpty() ? DbPaths.home(s) : DbPaths.find(s, mode, parent);
        if (in != 0)
            for (Store.Member m : s.members(in))
                if (m.item.type == Store.SMART && m.item.name.equals(name)) return new long[]{in, m.item.id};
        if (!parent.isEmpty()) return null;
        long id = DbPaths.smart(s, name);
        if (id == 0) return null;
        List<Long> of = s.listsOf(id);
        return new long[]{of.isEmpty() ? 0 : of.get(0), id};
    }

    /** הקבצים שהצד מצביע עליהם: ידנית — בכל העומק (כל רשימה פעם אחת); חכמה — מה שהיא מוצאת עכשיו. ממוינים, בלי כפולים. */
    static List<String> files(Store s, String mode, ListCmd.Side x, Finder f, String[] err) {
        Set<String> out = new LinkedHashSet<String>();
        if (x.kind == ListCmd.SMART_LIST) {
            long[] r = smart(s, mode, x);
            if (r == null) {
                err[0] = Words.ListCmdW.noSmart(x.raw);
                return null;
            }
            List<String> found = f == null ? null : f.find(s.prop(r[1], Store.P_COND));
            if (found == null) {
                err[0] = Words.ListCmdW.INDEX_NOT_READY;
                return null;
            }
            out.addAll(found);
        } else {
            if (x.home) {
                err[0] = Words.ListCmdW.HOME_FILES;
                return null;
            }
            long id = list(s, mode, x);
            if (id == 0) {
                err[0] = Words.ListCmdW.noList(x.raw);
                return null;
            }
            if (x.mask != null) {
                for (Store.Member m : s.members(id)) {
                    String p = pathOf(s, m.item);
                    if (p != null && Batch.wild(x.mask, new File(p).getName())) out.add(p);
                }
            } else if (!deep(s, id, f, out, new HashSet<Long>())) {
                err[0] = Words.ListCmdW.INDEX_NOT_READY;
                return null;
            }
        }
        List<String> l = new ArrayList<String>(out);
        Collections.sort(l);
        return l;
    }

    private static boolean deep(Store s, long list, Finder f, Set<String> out, Set<Long> done) {
        if (!done.add(list)) return true;
        for (Store.Member m : s.members(list)) {
            Store.Item it = m.item;
            if (it.type == Store.LIST) {
                if (!deep(s, it.id, f, out, done)) return false;
            } else if (it.type == Store.SMART) {
                String c = s.prop(it.id, Store.P_COND);
                if (c == null || c.trim().isEmpty()) continue;
                List<String> found = f == null ? null : f.find(c);
                if (found == null) return false;
                out.addAll(found);
            } else {
                String p = pathOf(s, it);
                if (p != null) out.add(p);
            }
        }
        return true;
    }

    private static String pathOf(Store s, Store.Item it) {
        if (it.type != Store.FILE) return null;
        Store.FileInfo fi = s.file(it.id);
        return fi == null ? null : fi.path;
    }

    // ---------- הפעולות ----------

    /** MD ידנית "א\ב": קיימת — בלי שינוי; חסרה — נוצרת בכל הדרך (חלק ראשון — בבית). */
    static Res md(Store s, String mode, ListCmd.Side a) {
        if (DbPaths.find(s, mode, a.path) != 0) return Res.same(Words.ListCmdW.exists(a.label()));
        DbPaths.ensure(s, mode, a.path);
        return Res.ok(Words.ListCmdW.created(a.label()));
    }

    /** RD / DEL על רשימה: רשימה, חכמה, תוכן, או קבצים לפי תבנית — יוצאים מהרשימות (הקבצים בכונן לא נוגעים). */
    static Res remove(Store s, final String mode, final ListCmd.Side a) {
        if (a.kind == ListCmd.LOOSE) {                                        // מ0.41: רישומי קבצים שאינם באף רשימה — מהמסד בלבד
            final int[] n = {0};
            s.atomic(new Store.Work() {
                @Override public void run(Store t) {
                    for (Store.Item it : t.unassigned()) if (it.type == Store.FILE) {
                        t.purge(it.id);
                        n[0]++;
                    }
                }
            }, true);
            return n[0] == 0 ? Res.same(Words.DbCmd.LOOSE_NONE) : Res.ok(Words.DbCmd.looseRemoved(n[0]));
        }
        if (a.kind == ListCmd.SMART_LIST) {
            final long[] r = smart(s, mode, a);
            if (r == null) return Res.err(Words.ListCmdW.noSmart(a.raw));
            s.atomic(new Store.Work() {
                @Override public void run(Store t) {
                    if (r[0] != 0) t.removeMember(r[0], r[1]);
                    dropIfAlone(t, r[1]);
                }
            }, true);
            return Res.ok(Words.DbCmd.removed(Words.ListCmdW.smartList(a.label())));
        }
        if (a.home && a.mask == null && !a.content) return Res.err(Words.ListCmdW.HOME_NO_REMOVE);
        final long id = list(s, mode, a);
        if (id == 0) return Res.same(Words.DbCmd.notThere(a.label()));
        final int[] n = {0};
        if (a.mask != null || a.content) {
            s.atomic(new Store.Work() {
                @Override public void run(Store t) {
                    for (Store.Member m : t.members(id)) {
                        if (a.mask != null) {
                            String p = pathOf(t, m.item);
                            if (p == null || !Batch.wild(a.mask, new File(p).getName())) continue;
                        }
                        n[0]++;
                        boolean idea = m.item.type == Store.NOTE || m.item.type == Store.TITLE;
                        if (idea && t.listsOf(m.item.id).size() == 1) {
                            t.trash(m.item.id);                              // רעיון שהיה רק כאן — לסל (ויוצא מהרשימה)
                            continue;
                        }
                        t.removeMember(id, m.item.id);
                        if (m.item.type == Store.LIST || m.item.type == Store.SMART) dropIfAlone(t, m.item.id);
                    }
                }
            }, true);
            String what = a.content ? Words.ListCmdW.contentOf(n[0], a.label()) : Words.files(n[0]);
            return Res.ok(Words.DbCmd.removed(what));
        }
        long parent = parentOf(s, mode, a, id);
        if (parent == -1) return Res.err(Words.DbCmd.needFullWay(a.label()));
        removeFrom(s, parent, id);
        return Res.ok(Words.DbCmd.removed(Words.ListCmdW.list(a.label())));
    }

    /**
     * מ0.30 (ממצא 40): הרשימה שממנה מוציאים את id. נכתבה הדרך — לפיה. שם לבד: הבית, אם היא בו; אחרת הרשימה היחידה
     * שהיא בתוכה; עומדת לעצמה — 0; בכמה רשימות — ‎-1 (צריך לכתוב את הדרך המלאה).
     */
    static long parentOf(Store s, String mode, ListCmd.Side a, long id) {
        List<String> up = new ArrayList<String>(a.path.subList(0, a.path.size() - 1));
        if (!up.isEmpty()) return DbPaths.find(s, mode, up);
        long home = DbPaths.home(s);
        List<Long> in = s.listsOf(id);
        if (in.contains(home)) return home;
        return in.isEmpty() ? 0 : in.size() == 1 ? in.get(0) : -1;
    }

    /** רשימה (או חכמה) שלא נשארה באף רשימה — נמחקת, ואיתה מה שהיה רק שלה (רשימות; רעיונות וכותרות — לסל). */
    private static void dropIfAlone(Store t, long id) {
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

    /** COPY: לרשימה ידנית (או לבית). fsFiles — קבצים מהכונן (כשהמקור אינו רשימה); הקבצים עצמם לתיקייה — ב-plan. */
    static Res copy(Store s, String mode, ListCmd.Side a, ListCmd.Side b, final List<String> fsFiles, Finder f) {
        final long dest = b.home ? DbPaths.home(s) : b.mask != null ? 0 : DbPaths.find(s, mode, b.path);
        if (dest == 0) return Res.err(b.home ? Words.DbCmd.NO_HOME : Words.ListCmdW.noList(b.raw));
        if (!a.list() || a.filesOf) {                                        // מסלולים ← לרשימה
            List<String> paths = fsFiles;
            String what = Words.files(fsFiles == null ? 0 : fsFiles.size());
            if (a.filesOf) {
                String[] err = {null};
                paths = files(s, mode, a, f, err);
                if (paths == null) return Res.err(err[0]);
                what = Words.ListCmdW.filesOf(Words.files(paths.size()), a.label());
            }
            final List<String> ps = paths;
            DbFiles.Added r = DbFiles.add(s, dest, ps, null, !(s instanceof MemStore));   // מ0.15: חתימה — בהרצה, לא בהדמיה
            return Res.ok(what + " — " + Words.ListCmdW.copied(r.added, ps.size() - r.added, ps.size() > r.added ? Words.DbCmd.ALREADY : null, b.label()) + nowIn(s, dest));
        }
        final List<Store.Item> what = new ArrayList<Store.Item>();
        final List<String> self = new ArrayList<String>();
        String label;
        if (a.kind == ListCmd.SMART_LIST) {
            long[] r = smart(s, mode, a);
            if (r == null) return Res.err(Words.ListCmdW.noSmart(a.raw));
            what.add(s.item(r[1]));
            label = Words.ListCmdW.smartList(a.label());
        } else {
            long src = list(s, mode, a);
            if (src == 0) return Res.err(a.home ? Words.DbCmd.NO_HOME : Words.ListCmdW.noList(a.raw));
            if (a.home || a.content || a.mask != null) {
                for (Store.Member m : s.members(src)) {
                    if (a.mask != null) {
                        String p = pathOf(s, m.item);
                        if (p == null || !Batch.wild(a.mask, new File(p).getName())) continue;
                    }
                    if (m.item.type == Store.LIST && (m.item.id == dest || s.contains(m.item.id, dest))) {
                        self.add(m.item.name);                               // היעד עצמו (או מה שהוא בתוכו) — לא לתוך עצמו
                        continue;
                    }
                    what.add(m.item);
                }
                label = a.mask != null ? Words.files(what.size()) : a.home ? Words.DbCmd.homeContent(what.size())
                        : Words.ListCmdW.contentOf(what.size(), a.label());
            } else {
                if (src == dest || s.contains(src, dest)) return Res.err(Words.ListCmdW.INTO_ITSELF);
                what.add(s.item(src));
                label = Words.ListCmdW.list(a.label());
            }
        }
        Copied c = copyInto(s, what, dest);
        String selfText = self.isEmpty() ? "" : Words.ListCmdW.notIntoSelf(self);
        return Res.ok(Words.ListCmdW.copy(label, b.label()) + names(what) + selfText + " · "
                + Words.ListCmdW.copied(c.done, c.skipped, c.why, b.label()) + nowIn(s, dest));
    }

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

    private static String nowIn(Store s, long list) {
        DbCount c = DbCount.of(s, list, null);
        return Words.ListCmdW.nowIn(c.lists, c.items);
    }

    private static String names(List<Store.Item> items) {
        if (items.isEmpty()) return "";
        StringBuilder sb = new StringBuilder(" — ");
        for (int i = 0; i < Math.min(5, items.size()); i++) sb.append(i > 0 ? ", " : "").append('"').append(items.get(i).name).append('"');
        if (items.size() > 5) sb.append(" ").append(Words.andMore(items.size() - 5));
        return sb.toString();
    }

    /** מ0.21: REN ידנית/חכמה "א\ב" "שם חדש" — שם תפוס באותה רשימה (אותו סוג) — לא; "/" או "\" בשם — לא. */
    static Res rename(Store s, String mode, final ListCmd.Side a, String name) {
        if (name.contains("/") || name.contains("\\")) return Res.err(Words.DbCmd.BAD_NAME);   // לפני הניקוי (שמוחק אותם)
        final String n = a.kind == ListCmd.SMART_LIST ? name.trim() : Names.cleanName(name);
        if (n.isEmpty()) return Res.err(Words.DbCmd.BAD_NAME);
        long id, parent;
        if (a.kind == ListCmd.SMART_LIST) {
            long[] r = smart(s, mode, a);
            if (r == null) return Res.err(Words.ListCmdW.noSmart(a.raw));
            id = r[1];
            parent = r[0];
        } else {
            id = DbPaths.find(s, mode, a.path);
            if (id == 0) return Res.err(Words.ListCmdW.noList(a.raw));
            List<String> up = new ArrayList<String>(a.path.subList(0, a.path.size() - 1));
            parent = up.isEmpty() ? DbPaths.home(s) : DbPaths.find(s, mode, up);
        }
        Store.Item it = s.item(id);
        if (n.equals(it.name)) return Res.same(Words.DbCmd.renamed(a.label(), n));
        String e = nameError(s, java.util.Collections.singletonList(parent), id, it.type, name);   // מ0.23: הבדיקה האחת
        if (e != null) return Res.err(e);
        final long fid = id;
        s.atomic(new Store.Work() {
            @Override public void run(Store t) { t.rename(fid, n); }
        }, true);
        return Res.ok(Words.DbCmd.renamed(a.label(), n)).renamed(fid, it.name, n);
    }

    /**
     * מ0.17: LINK (קשר) — אותו פריט גם ביעד (רשימה משותפת); MOVE (העבר) — נכנס ליעד ויוצא מהמקום שלו. המקור: רשימה
     * (מהרשימה שמעליה, או מהבית), חכמה, תוכן של רשימה, או קבצים לפי תבנית — כמו ב-COPY.
     */
    static Res link(Store s, String mode, ListCmd.Side a, ListCmd.Side b, boolean move) {
        long dest = b.home ? DbPaths.home(s) : b.mask != null ? 0 : DbPaths.find(s, mode, b.path);
        if (dest == 0) return Res.err(b.home ? Words.DbCmd.NO_HOME : Words.ListCmdW.noList(b.raw));
        List<Long> ids = new ArrayList<Long>();
        long from;
        String label;
        if (a.kind == ListCmd.SMART_LIST) {
            long[] r = smart(s, mode, a);
            if (r == null) return Res.err(Words.ListCmdW.noSmart(a.raw));
            ids.add(r[1]);
            from = r[0];
            label = Words.ListCmdW.smartList(a.label());
        } else {
            long src = list(s, mode, a);
            if (src == 0) return Res.err(a.home ? Words.DbCmd.NO_HOME : Words.ListCmdW.noList(a.raw));
            if (a.home || a.content || a.mask != null) {
                for (Store.Member m : s.members(src)) {
                    if (a.mask != null) {
                        String p = pathOf(s, m.item);
                        if (p == null || !Batch.wild(a.mask, new File(p).getName())) continue;
                    }
                    ids.add(m.item.id);
                }
                from = src;
                label = a.mask != null ? Words.files(ids.size()) : a.home ? Words.DbCmd.homeContent(ids.size())
                        : Words.ListCmdW.contentOf(ids.size(), a.label());
            } else {
                if (src == dest || s.contains(src, dest)) return Res.err(Words.ListCmdW.INTO_ITSELF);
                from = parentOf(s, mode, a, src);
                if (from == -1) return Res.err(Words.DbCmd.needFullWay(a.label()));
                ids.add(src);
                label = Words.ListCmdW.list(a.label());
            }
        }
        Copied c = move ? moveTo(s, ids, from, dest) : linkInto(s, ids, dest);
        return Res.ok((move ? Words.DbCmd.moved(label, b.label()) : Words.DbCmd.linked(label, b.label())) + " · "
                + Words.ListCmdW.copied(c.done, c.skipped, c.why, b.label()) + nowIn(s, dest));
    }

    /** מ0.18: היצוא — write=false (ההדמיה): רק השם והמספרים; write=true: נכתב. */
    static Res export(Store s, String mode, ListCmd.Side a, ListCmd.Side b, Finder f, boolean write) {
        long when = System.currentTimeMillis();
        DbExport.Out o;
        String name;
        if (a.filesOf) {
            String[] err = {null};
            List<String> ps = files(s, mode, a, f, err);
            if (ps == null) return Res.err(err[0]);
            name = Words.DbCmd.exportFilesName(a.raw);
            o = DbExport.paths(ps, name, when);
        } else {
            long id = list(s, mode, a);
            if (id == 0) return Res.err(a.home ? Words.DbCmd.NO_HOME : Words.ListCmdW.noList(a.raw));
            name = a.home ? Words.DbCmd.exportModeName(mode) : Words.DbCmd.exportListName(s.item(id).name);
            o = DbExport.list(s, id, a.home, name, when);
        }
        File to = DbExport.target(b.raw, name, exportDir());
        if (write) {
            try {
                File dir = to.getParentFile();
                if (dir != null && !dir.isDirectory() && !dir.mkdirs()) return Res.err(Words.CANT_CREATE_FOLDER);
                Files.writeAtomic(to, o.text);
            } catch (Throwable t) {
                Log.e("DbListCmd.export", t);
                return Res.err(Words.Db.failed(String.valueOf(t.getMessage())));
            }
        }
        return Res.ok(Words.DbCmd.exported(name, Bidi.path(to.getAbsolutePath()), o.lists, o.files, o.skipped));
    }

    /** מ0.22: קובץ הייבוא — שם בלי מסלול: בתיקיית היצוא; "\\" כמו "/". */
    static File importFile(String raw) {
        String r = raw == null ? "" : raw.trim().replace('\\', '/');
        File f = new File(r);
        return f.isAbsolute() ? f : new File(exportDir(), r);
    }

    /** מ0.22: ייבוא — הטקסט (בפורמט היצוא) לתוך הרשימה. */
    static Res importFrom(Store s, String mode, File in, ListCmd.Side b) {
        long dest = b.home ? DbPaths.home(s) : DbPaths.find(s, mode, b.path);
        if (dest == 0) return Res.err(b.home ? Words.DbCmd.NO_HOME : Words.ListCmdW.noList(b.raw));
        String text;
        try {
            text = Files.readUtf8(in);
        } catch (Throwable t) {
            return Res.err(Words.DbCmd.cantRead(in.getName()));
        }
        DbMore.Imported r = DbMore.importText(s, dest, text);
        return Res.ok(Words.DbCmd.imported(in.getName(), b.label(), r.lists, r.files, r.bad) + nowIn(s, dest));
    }

    /** מ0.22: העתקה מאופן אחר — המקור נפתח לקריאה בלבד; עותק עצמאי (קבצים — לפי המסלול). */
    static Res copyFromMode(Store t, String mode, String srcMode, ListCmd.Side a, ListCmd.Side b) {
        long dest = b.home ? DbPaths.home(t) : DbPaths.find(t, mode, b.path);
        if (dest == 0) return Res.err(b.home ? Words.DbCmd.NO_HOME : Words.ListCmdW.noList(b.raw));
        SqlStore src = null;
        try {
            src = SqlStore.openReadOnly(Db.dbFile(DbListScreen.app, srcMode));
            return copyAcross(src, srcMode, a, t, dest, b.label());
        } finally {
            if (src != null) src.close();
        }
    }

    /** מ0.22: הליבה של ההעתקה בין מסדים (נבדקת במחשב על שני מסדים). */
    static Res copyAcross(Store src, String srcMode, ListCmd.Side a, Store t, long dest, String destLabel) {
        long id = DbPaths.find(src, srcMode, a.path);
        if (id == 0) return Res.err(Words.ListCmdW.noList(Words.DbCmd.MODE_PREFIX + srcMode + "/" + ListCmd.pathText(a.path)));
        int n = 0;
        if (a.content) for (Store.Member m : src.members(id)) n += DbMore.copyAcross(src, m.item.id, t, dest);
        else {
            for (Store.Member m : t.members(dest))
                if (m.item.type == Store.LIST && m.item.name.equals(src.item(id).name)) return Res.err(Words.DbCmd.SAME_NAME);
            n = DbMore.copyAcross(src, id, t, dest);
        }
        return Res.ok(Words.DbCmd.fromMode(srcMode, ListCmd.pathText(a.path), destLabel, n) + nowIn(t, dest));
    }

    /** התיקייה "יצוא" בתיקיית התוכנה. */
    static File exportDir() {
        return new File(Paths.data(), Words.DbCmd.EXPORT_DIR);
    }

    /** COMP: לפי המסלולים; /תנאים — גם התנאים של שתי חכמות. */
    static Res comp(Store s, String mode, ListCmd.Side a, ListCmd.Side b, boolean conds, Finder f) {
        String[] err = {null};
        List<String> fa = files(s, mode, a, f, err), fb = fa == null ? null : files(s, mode, b, f, err);
        if (fa == null || fb == null) return Res.err(err[0]);
        Set<String> sa = new LinkedHashSet<String>(fa), sb = new LinkedHashSet<String>(fb);
        List<String> onlyA = new ArrayList<String>(), onlyB = new ArrayList<String>();
        int both = 0;
        for (String x : sa) if (sb.contains(x)) both++; else onlyA.add(x);
        for (String x : sb) if (!sa.contains(x)) onlyB.add(x);
        String text = Words.ListCmdW.compared(a.label(), onlyA.size(), b.label(), onlyB.size(), both) + examples(onlyA) + examples(onlyB);
        if (conds) {
            long[] ra = smart(s, mode, a), rb = smart(s, mode, b);
            text += "\n" + ListCmd.condsDiff(ra == null ? "" : s.prop(ra[1], Store.P_COND), rb == null ? "" : s.prop(rb[1], Store.P_COND),
                    a.label(), b.label());
        }
        return Res.same(text);
    }

    private static String examples(List<String> paths) {
        if (paths.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("\n   ");
        for (int i = 0; i < Math.min(3, paths.size()); i++) sb.append(i > 0 ? " · " : "").append(new File(paths.get(i)).getName());
        if (paths.size() > 3) sb.append(" · ").append(Words.andMore(paths.size() - 3));
        return sb.toString();
    }

    // ---------- עותק בזיכרון להדמיה ----------

    /** עותק של המסד בזיכרון (רשימות, חכמות, קבצים, רעיונות וכותרות, הסדר, המאפיינים והמקורות). לא נשמר. */
    static MemStore copyOf(Store src) {
        final MemStore m = new MemStore(IdCounter.memory());
        final Map<Long, Long> id = new HashMap<Long, Long>();
        List<Store.Item> lists = src.lists();                                // מ0.38: פעם אחת — המסד יכול להשתנות בזמן ההעתקה
        for (Store.Item it : lists) id.put(it.id, m.createList(it.name, 0));
        for (Store.Item it : src.smarts()) id.put(it.id, m.createSmart(it.name, 0));
        for (Map.Entry<Long, Long> e : id.entrySet()) {
            for (String k : ALL_PROPS) {
                String v = src.prop(e.getKey(), k);
                if (v != null) m.setProp(e.getValue(), k, v);
            }
            String o = src.origin(e.getKey());
            if (o != null && !o.isEmpty()) m.setOrigin(e.getValue(), o);
        }
        for (Store.Item it : lists) {
            List<Store.Member> ms;
            try {
                ms = src.members(it.id);
            } catch (Store.Failure f) {                                        // מ0.38 (דוח האבחון): נמחקה תוך כדי — מדלגים
                continue;
            }
            for (Store.Member mb : ms) {
                long into = id.get(it.id);
                Store.Item x = mb.item;
                if (x.type == Store.NOTE) m.addNote(into, x.name);
                else if (x.type == Store.TITLE) m.addTitle(into, x.name);
                else {
                    Long to = id.get(x.id);
                    if (to == null && x.type == Store.FILE) {
                        long fid = fileOf(src, m, x.id);
                        if (fid != 0) id.put(x.id, to = fid);
                    }
                    if (to != null) m.addMember(into, to);
                }
            }
        }
        for (Store.Item it : src.unassigned()) if (it.type == Store.FILE && !id.containsKey(it.id)) fileOf(src, m, it.id);
        return m;
    }

    private static long fileOf(Store src, MemStore m, long item) {
        Store.FileInfo fi;
        try {
            fi = src.file(item);
        } catch (Store.Failure f) {
            return 0;                                                         // מ0.38: נמחק תוך כדי ההעתקה
        }
        if (fi == null) return 0;
        long had = m.findFile(fi.path);
        return had != 0 ? had : m.createFile(fi);
    }

    // ---------- החיבור לתכנון של Batch ----------

    /** האופן שהפקודות עובדות עליו עכשיו (הבית של המסד שפתוח). */
    static String mode() {
        return DbListScreen.currentMode;
    }

    /** מ0.15: האופן של הקובץ — מ"אופן" שבו, אחרת זה שפתוח. */
    static String mode(Batch.Sim sim) {
        return sim != null && sim.mode != null ? sim.mode : mode();
    }

    /** שורה עם מילות סוג, כשעובדים במסד: אותן בדיקות צורה כמו ListCmd.plan; הפעולה — על העותק בזיכרון, ובהרצה — על המסד. */
    static void plan(final Batch.Step s, final String cmd, ListCmd.Parsed p, List<String> sw, final Batch.Sim sim,
                     final List<String> fsFiles, final File fsDest) {
        boolean disk = false;                                                  // מ0.40: יש צד בכונן — שם /מכיל: חל
        for (ListCmd.Side x : p.sides) if (!x.list()) disk = true;
        for (String x : sw)
            if (x.startsWith("/" + Batch.SW_DATE) || x.startsWith("/" + Batch.SW_SIZE) || (!disk && x.startsWith("/" + Batch.SW_CONTAINS))) {
                s.error = Words.ListCmdW.NO_FILTER;
                return;
            }
        for (ListCmd.Side x : p.sides)                                        // מ0.41: "לא-משויכים" — רק "בטל לא-משויכים" (ו"ודא")
            if (x.kind == ListCmd.LOOSE && !((cmd.equals("RD") || cmd.equals("DEL")) && p.sides.size() == 1)) {
                s.error = Words.DbCmd.LOOSE_USAGE;
                return;
            }
        if (sim.modeError != null) { s.error = sim.modeError; return; }      // מ0.17.1: "אופן" שנכשל — לא לרוץ על אופן אחר
        final String mode = mode(sim);
        final Store db;
        try {
            db = sim(sim, mode);
        } catch (Throwable t) {
            Log.e("DbListCmd.sim", t);
            s.error = Words.Db.failed(String.valueOf(t.getMessage()));
            return;
        }
        final Finder find = finder(sim);
        final List<ListCmd.Side> sd = p.sides;
        Work work = null;
        boolean destructive = false;
        if (cmd.equals("MD")) {
            if (sd.size() != 1) { s.error = Words.Cmd.usage(cmd); return; }
            final ListCmd.Side a = sd.get(0);
            if (a.kind == ListCmd.SMART_LIST) { s.error = Words.ListCmdW.MD_SMART; return; }
            if (a.kind != ListCmd.LIST || a.content || a.filesOf || a.home || a.mask != null) { s.error = Words.ListCmdW.USAGE_MD; return; }
            s.listPath = a.path;
            work = new Work() {
                @Override public Res run(Store t, Finder f) { return md(t, mode, a); }
            };
        } else if (cmd.equals("RD") || cmd.equals("DEL")) {
            if (sd.size() != 1) { s.error = Words.Cmd.usage(cmd); return; }
            final ListCmd.Side a = sd.get(0);
            if (a.filesOf) {                                                 // הקבצים עצמם — לסל (הביצוע הרגיל)
                if (cmd.equals("RD")) { s.error = Words.ListCmdW.RD_FILES; return; }
                String[] err = {null};
                List<String> fs = files(db, mode, a, find, err);
                if (fs == null) { s.error = err[0]; return; }
                for (String x : fs) if (sim.isFile(new File(x))) s.files.add(new File(x).getAbsolutePath());
                s.describe = Words.Cmd.toTrash(Words.files(s.files.size())) + Words.ListCmdW.FROM_DISK;
                for (String x : s.files) {
                    sim.gone.add(x);
                    sim.added.remove(x);
                }
                s.target = a.label();
                return;
            }
            destructive = true;
            work = new Work() {
                @Override public Res run(Store t, Finder f) { return remove(t, mode, a); }
            };
        } else if (cmd.equals("COPY")) {
            if (sd.size() != 2) { s.error = Words.ListCmdW.USAGE; return; }
            final ListCmd.Side a = sd.get(0), b = sd.get(1);
            if (b.content || b.filesOf) { s.error = Words.ListCmdW.USAGE; return; }
            if (b.kind == ListCmd.SMART_LIST) { s.error = Words.ListCmdW.INTO_SMART; return; }
            String fromMode = a.path == null || a.path.isEmpty() || !a.path.get(0).startsWith(Words.DbCmd.MODE_PREFIX) ? null
                    : a.path.get(0).substring(Words.DbCmd.MODE_PREFIX.length()).trim();
            if (fromMode != null) {                                          // מ0.22: מאופן אחר — "אופן:X/א\ב"
                a.path = new ArrayList<String>(a.path.subList(1, a.path.size()));
                if (a.path.isEmpty()) { s.error = Words.DbCmd.USAGE_FROM_MODE; return; }
                if (!fromMode.equals(mode)) {
                    if (!Db.dbModes(DbListScreen.app).contains(fromMode)) { s.error = Words.DbCmd.noMode(fromMode, Db.dbModes(DbListScreen.app)); return; }
                    if (a.kind != ListCmd.LIST || a.filesOf || a.mask != null || b.kind != ListCmd.LIST) { s.error = Words.DbCmd.USAGE_FROM_MODE; return; }
                    final String src = fromMode;
                    work = new Work() {
                        @Override public Res run(Store t, Finder f) { return copyFromMode(t, mode, src, a, b); }
                    };
                    s.target = b.label();
                    Res pr;
                    try { pr = work.run(db, find); } catch (Store.Failure e) { pr = Res.err(Words.Db.failed(e.getMessage())); }
                    if (pr.error != null) { s.error = pr.error; return; }
                    s.describe = Words.DbCmd.inDb(pr.text);
                    final Work w = work;
                    s.op = new ListCmd.Op() {
                        @Override public String run(Library lib) { return real(mode, w, false, lib); }
                    };
                    return;
                }
            }
            if (a.kind == ListCmd.EXPORT) {                                  // מ0.22: ייבוא מקובץ טקסט לרשימה
                final File in = importFile(a.raw);
                work = new Work() {
                    @Override public Res run(Store t, Finder f) { return importFrom(t, mode, in, b); }
                };
                s.target = b.label();
                if (in.isFile()) {
                    Res pr = work.run(db, find);
                    if (pr.error != null) { s.error = pr.error; return; }
                    s.describe = Words.DbCmd.inDb(pr.text);
                } else s.describe = Words.DbCmd.importLater(Bidi.path(in.getAbsolutePath()));   // ייווצר בשורות שלפני
                final Work w = work;
                s.op = new ListCmd.Op() {
                    @Override public String run(Library lib) { return real(mode, w, false, lib); }
                };
                return;
            }
            if (b.kind == ListCmd.EXPORT) {                                  // מ0.18: יצוא לקובץ טקסט
                if (a.kind == ListCmd.SMART_LIST || a.content || a.mask != null || !a.list()) { s.error = Words.DbCmd.USAGE_EXPORT; return; }
                work = new Work() {
                    @Override public Res run(Store t, Finder f) { return export(t, mode, a, b, f, !(t instanceof MemStore)); }
                };
                s.target = b.label();
                Res pr = work.run(db, find);
                if (pr.error != null) { s.error = pr.error; return; }
                s.describe = Words.DbCmd.inDb(pr.text);
                final Work w = work;
                s.op = new ListCmd.Op() {
                    @Override public String run(Library lib) { return real(mode, w, false, lib); }
                };
                return;
            }
            if (b.kind == ListCmd.FS) {                                      // מרשימה לתיקייה — הקבצים עצמם (הביצוע הרגיל)
                toFolder(s, cmd, a, db, mode, find, sw, sim, fsDest);
                return;
            }
            work = new Work() {
                @Override public Res run(Store t, Finder f) { return copy(t, mode, a, b, fsFiles, f); }
            };
        } else if (cmd.equals("COMP")) {
            if (sd.size() != 2) { s.error = Words.ListCmdW.USAGE_COMP; return; }
            final ListCmd.Side a = sd.get(0), b = sd.get(1);
            if (!a.list() || !b.list()) { s.error = Words.ListCmdW.COMP_FS; return; }
            if (a.content || b.content) { s.error = Words.ListCmdW.USAGE_COMP; return; }
            final boolean conds = sw.contains(ListCmd.SW_CONDS);
            if (conds && (a.kind != ListCmd.SMART_LIST || b.kind != ListCmd.SMART_LIST || a.filesOf || b.filesOf)) {
                s.error = Words.ListCmdW.CONDS_SMART;
                return;
            }
            Res r = comp(db, mode, a, b, conds, find);
            if (r.error != null) s.error = r.error;
            else s.describe = Words.DbCmd.inDb(r.text);
            return;
        } else if (cmd.equals("REN")) {                                       // מ0.21: שינוי שם לרשימה — במסד
            if (sd.size() != 2) { s.error = Words.DbCmd.USAGE_REN; return; }
            final ListCmd.Side a = sd.get(0);
            final String name = sd.get(1).raw == null ? "" : sd.get(1).raw.trim();
            if (!a.list() || a.content || a.filesOf || a.home || a.mask != null || sd.get(1).list()) { s.error = Words.DbCmd.USAGE_REN; return; }
            work = new Work() {
                @Override public Res run(Store t, Finder f) { return rename(t, mode, a, name); }
            };
        } else if (cmd.equals("MOVE") || cmd.equals(Batch.LINK)) {           // מ0.17: העברה וקישור — במסד
            if (sd.size() != 2) { s.error = Words.DbCmd.USAGE_LINK; return; }
            final ListCmd.Side a = sd.get(0), b = sd.get(1);
            if (cmd.equals("MOVE") && a.list() && b.kind == ListCmd.FS && !b.content && !b.filesOf) {   // מ0.40: הקבצים עצמם — לתיקייה
                toFolder(s, cmd, a, db, mode, find, sw, sim, fsDest);
                return;
            }
            if (!a.list() && cmd.equals("MOVE")) { s.error = Words.ListCmdW.MOVE_TO_LIST; return; }
            if (!a.list() || a.filesOf || b.content || b.filesOf || b.kind == ListCmd.FS) { s.error = Words.DbCmd.USAGE_LINK; return; }
            if (b.kind == ListCmd.SMART_LIST) { s.error = Words.ListCmdW.INTO_SMART; return; }
            final boolean move = cmd.equals("MOVE");
            destructive = move;
            work = new Work() {
                @Override public Res run(Store t, Finder f) { return link(t, mode, a, b, move); }
            };
        } else {
            s.error = Words.ListCmdW.notYet(cmd);
            return;
        }
        s.target = sd.get(sd.size() - 1).label();
        Res r;
        try {
            r = work.run(db, find);                                          // ההדמיה — על העותק
        } catch (Store.Failure e) {
            r = Res.err(Words.Db.failed(e.getMessage()));
        }
        if (r.error != null) { s.error = r.error; return; }
        s.describe = Words.DbCmd.inDb(r.text);
        // מ0.42 (בדיקת המכשיר דרך תוסף האבחון: "ודא לא-משויכים /פריטים:0 — בפועל: 2"): "בטל לא-משויכים" נבדק תמיד בהרצה. קובץ
        // שנמחק בכונן בשורות שלפני יוצא מהרשימות שלו במסד (DbFollow) — וההדמיה לא יודעת (ד, 37). עד כאן ההדמיה אמרה "אין", והשורה
        // לא רצה בכלל; זה הוסתר כל עוד נשארו "לא משויכים" מהרצות קודמות.
        boolean atRun = sd.size() == 1 && sd.get(0).kind == ListCmd.LOOSE;
        if (atRun && r.noop) s.describe = Words.DbCmd.LOOSE_AT_RUN;
        s.noop = r.noop && !atRun;
        if (s.noop) return;
        final Work w = work;
        final boolean before = destructive;
        s.op = new ListCmd.Op() {
            @Override public String run(Library lib) { return real(mode, w, before, lib); }
        };
    }

    /**
     * מ0.25 (באג 7; המשתמש: "כל הפעלים בפקודות רצים לפי הסדר בו הם כתובים"): החלק הכותב של פועל בעברית — "צור רשימה",
     * "צור רשימה חכמה", "עדכן רשימה חכמה" — הוא שלב רגיל: על העותק בתכנון (שורות מאוחרות רואות אותו), ובהרצה על המסד, במקומו.
     * מחזיר את האופן אם זה פועל כותב (ואז בסוף רק פותחים את המסך), null — אינו כותב (נשאר כולו לסוף).
     */
    static String planVerb(final Batch.Step s, final Verbs.Cmd c, final Batch.Sim sim) {
        final boolean list = c.kind.equals(Verbs.K_LIST) && c.verb.equals(Verbs.CREATE);
        final boolean smartNew = c.kind.equals(Verbs.K_SMART) && c.verb.equals(Verbs.CREATE);
        final boolean smartUpd = c.kind.equals(Verbs.K_SMART) && c.verb.equals(Verbs.UPDATE);
        final boolean note = c.kind.equals(Verbs.K_NOTE), title = c.kind.equals(Verbs.K_TITLE);   // מ0.37
        if (!list && !smartNew && !smartUpd && !note && !title) return null;
        if (sim.modeError != null) { s.error = sim.modeError; return null; }
        final String mode = mode(sim);
        final Store db;
        try {
            db = sim(sim, mode);
        } catch (Throwable t) {
            Log.e("DbListCmd.sim", t);
            s.error = Words.Db.failed(String.valueOf(t.getMessage()));
            return null;
        }
        final List<String> path = list ? Names.parsePath(c.arg) : note || title ? Names.parsePath(c.cond) : null;
        if ((note || title) && path.isEmpty()) { s.error = Words.DbCmd.NOTE_NEEDS_LIST; return null; }
        final Work work = new Work() {
            @Override
            public Res run(Store t, Finder f) {
                if (note || title) {
                    long in = DbPaths.find(t, mode, path);
                    if (in == 0) return Res.err(Words.ListCmdW.noList(Names.label(path)));
                    if (title) t.addTitle(in, c.arg);
                    else t.addNote(in, c.arg);
                    return Res.ok(Words.DbCmd.noteAdded(title, c.arg, Names.label(path)));
                }
                if (list) {
                    if (path == null || path.isEmpty()) return Res.err(Words.Verb.NOT_FOUND + c.arg);
                    if (DbPaths.find(t, mode, path) != 0) return Res.same(Words.ListCmdW.exists(Names.label(path)));
                    return DbPaths.ensure(t, mode, path) == 0 ? Res.err(Words.Verb.NOT_FOUND + c.arg)
                            : Res.ok(Words.ListCmdW.created(Names.label(path)));
                }
                if (smartNew) {
                    long had = DbPaths.smart(t, c.arg);
                    long id = DbPaths.ensureSmart(t, c.arg, c.cond, c.sort);
                    if (id == 0) return Res.err(Words.Verb.NOT_FOUND + c.arg);
                    return id == had ? Res.same(Words.ListCmdW.exists(c.arg)) : Res.ok(Words.ListCmdW.created(c.arg));
                }
                return DbPaths.updateSmart(t, c.arg, c.cond, c.sort) == 0 ? Res.err(Words.Verb.NOT_FOUND + c.arg)
                        : Res.ok(Words.DbCmd.updated(c.arg));
            }
        };
        Res r;
        try {
            r = work.run(db, finder(sim));                                   // ההדמיה — על העותק
        } catch (Throwable e) {
            r = Res.err(Words.Db.failed(String.valueOf(e.getMessage())));
        }
        if (r.error != null) { s.error = r.error; return null; }
        if (!r.noop) {
            s.describe = Words.DbCmd.inDb(r.text);
            s.op = new ListCmd.Op() {
                @Override public String run(Library lib) { return real(mode, work, false, lib); }
            };
        }
        return mode;
    }

    /**
     * מ0.25 (באג 2): המקור של COMP /רשימה:<שם> — מהמסד (עד עכשיו נקרא עץ הרשימות של המנוע הישן, והרשימה שבמסד לא נמצאה).
     * הרשימה: הדרך כמו שנכתבה ("א/ב"); ואם אין — בתוך "שמור לתוכנה". כל הקבצים שבה, בכל העומק, כולל מה שהרשימות החכמות
     * שבתוכה מוצאות עכשיו. null — שגיאה (ב־err).
     */
    static Compare.Source compSource(Batch.Sim sim, String name, String skip, String[] err) {
        if (sim.modeError != null) { err[0] = sim.modeError; return null; }
        final String mode = mode(sim);
        Store db;
        try {
            db = sim(sim, mode);
        } catch (Throwable t) {
            Log.e("DbListCmd.compSource", t);
            err[0] = Words.Db.failed(String.valueOf(t.getMessage()));
            return null;
        }
        List<String> path = Names.parsePath(name == null ? "" : name.replace('\\', '/'));
        long id = DbPaths.find(db, mode, path);
        if (id == 0 && !path.isEmpty()) {
            List<String> in = new ArrayList<String>();
            in.add(Reserved.ROOT);
            in.addAll(path);
            id = DbPaths.find(db, mode, in);
        }
        if (id == 0) { err[0] = Words.ListCmdW.noList(name); return null; }
        Set<String> out = new LinkedHashSet<String>();
        if (!deep(db, id, finder(sim), out, new HashSet<Long>())) { err[0] = Words.ListCmdW.INDEX_NOT_READY; return null; }
        Compare.Source src = new Compare.Source();
        for (String p : out) Compare.addFile(sim.lib, src, new File(p), skip);
        return src;
    }

    // ---------- מ0.14: ודא ----------

    /** מה לוודא: קיימת/חסרה, ומספרי רשימות ופריטים (-1 — לא נבדק). */
    static final class Expect {
        Boolean exists;
        long lists = -1, items = -1;
        String error;

        String text(ListCmd.Side a) {
            StringBuilder sb = new StringBuilder(a.label());
            if (exists != null) sb.append(' ').append(exists ? Words.DbCmd.WORD_EXISTS : Words.DbCmd.WORD_MISSING);
            if (lists >= 0) sb.append(' ').append(Words.DbCmd.SW_LISTS).append(lists);
            if (items >= 0) sb.append(' ').append(Words.DbCmd.SW_ITEMS).append(items);
            return sb.toString();
        }
    }

    static Expect expect(List<String> conds) {
        Expect e = new Expect();
        for (String c : conds) {
            String t = c.trim();
            try {
                if (t.equals(Words.DbCmd.WORD_EXISTS)) e.exists = Boolean.TRUE;
                else if (t.equals(Words.DbCmd.WORD_MISSING)) e.exists = Boolean.FALSE;
                else if (t.startsWith(Words.DbCmd.SW_LISTS)) e.lists = Long.parseLong(t.substring(Words.DbCmd.SW_LISTS.length()).trim());
                else if (t.startsWith(Words.DbCmd.SW_ITEMS)) e.items = Long.parseLong(t.substring(Words.DbCmd.SW_ITEMS.length()).trim());
                else e.error = Words.DbCmd.VERIFY_USAGE;
            } catch (NumberFormatException x) {
                e.error = Words.DbCmd.VERIFY_USAGE;
            }
        }
        if (e.exists == null && e.lists < 0 && e.items < 0) e.exists = Boolean.TRUE;   // בלי תנאי — קיימת
        if (Boolean.FALSE.equals(e.exists) && (e.lists >= 0 || e.items >= 0)) e.error = Words.DbCmd.VERIFY_USAGE;
        return e;
    }

    /** הבדיקה עצמה על המסד: Res.ok — עבר; Res.err — נכשל (הטקסט: מה נמצא בפועל). */
    static boolean verify(Store s, String mode, ListCmd.Side a, Expect e, String[] found) {
        if (a.kind == ListCmd.LOOSE) {                                        // מ0.41: כמה קבצים אינם באף רשימה
            long n = 0;
            for (Store.Item it : s.unassigned()) if (it.type == Store.FILE) n++;
            found[0] = Words.DbCmd.SW_ITEMS + n;
            return !Boolean.FALSE.equals(e.exists) && e.lists <= 0 && (e.items < 0 || e.items == n);
        }
        long id = a.kind == ListCmd.SMART_LIST ? (smart(s, mode, a) == null ? 0 : smart(s, mode, a)[1]) : list(s, mode, a);
        if (id == 0) {
            found[0] = Words.DbCmd.WORD_MISSING;
            return Boolean.FALSE.equals(e.exists);
        }
        if (Boolean.FALSE.equals(e.exists)) {
            found[0] = Words.DbCmd.WORD_EXISTS;
            return false;
        }
        DbCount c = a.kind == ListCmd.SMART_LIST ? null : DbCount.of(s, id, null);
        long lists = c == null ? 0 : c.lists, items = c == null ? 0 : c.items;
        found[0] = Words.DbCmd.SW_LISTS + lists + " " + Words.DbCmd.SW_ITEMS + items;
        return (e.lists < 0 || e.lists == lists) && (e.items < 0 || e.items == items);
    }

    /** שורת ודא: בתכנון — על העותק (מה צפוי), בהרצה — על המסד; ❌ לא עוצרת. */
    static void planVerify(final Batch.Step s, final ListCmd.Side a, List<String> conds, Batch.Sim sim) {
        if (!a.list() || a.content || a.filesOf || a.mask != null) { s.error = Words.DbCmd.VERIFY_USAGE; return; }
        final Expect e = expect(conds);
        if (e.error != null) { s.error = e.error; return; }
        if (sim.modeError != null) { s.error = sim.modeError; return; }
        final String mode = mode(sim);
        Store db;
        try {
            db = sim(sim, mode);
        } catch (Throwable t) {
            Log.e("DbListCmd.verify", t);
            s.error = Words.Db.failed(String.valueOf(t.getMessage()));
            return;
        }
        final String what = e.text(a);
        String[] found = {""};
        boolean now = verify(db, mode, a, e, found);
        s.target = a.label();
        s.describe = Words.DbCmd.verifyWill(what) + " · " + Words.DbCmd.inDb(Words.DbCmd.verifyResult(now, what, found[0]));
        s.op = new ListCmd.Op() {
            @Override
            public String run(Library lib) {
                SqlStore st = null;
                try {
                    st = SqlStore.openReadOnly(Db.dbFile(DbListScreen.app, mode));
                    String[] f = {""};
                    boolean ok = verify(st, mode, a, e, f);
                    s.verified = ok;
                    return Words.DbCmd.verifyResult(ok, what, f[0]);
                } catch (Throwable t) {
                    s.verified = Boolean.FALSE;
                    return Words.Db.failed(String.valueOf(t.getMessage()));
                } finally {
                    if (st != null) st.close();
                }
            }
        };
    }

    /** מ0.18: ודא "קובץ" קיים / חסר — בכונן (גם מה שהשורות שלפני ייצרו, לפי ההדמיה של Batch). */
    static void planVerifyFile(final Batch.Step s, final File f, List<String> conds) {
        Boolean want = null;
        int count = -1;
        String has = null;
        boolean index = false;
        int trash = -2;
        for (int i = 0; i < conds.size(); i++) {
            String t = conds.get(i).trim();
            if (t.equals(W_IN_INDEX) && conds.size() == 1) index = true;          // מ0.42: ודא "<קובץ>" באינדקס
            else if (t.equals(Words.DbCmd.W_IN_TRASH) && conds.size() == 1) trash = -1;   // מ0.45: ודא "<קובץ>" בסל · /בסל:N
            else if (t.startsWith(Words.DbCmd.SW_IN_TRASH) && conds.size() == 1) {
                try { trash = Integer.parseInt(t.substring(Words.DbCmd.SW_IN_TRASH.length()).trim()); } catch (NumberFormatException e) { trash = -3; }
                if (trash < 0) { s.error = Words.DbCmd.VERIFY_USAGE; return; }
            }
            else if (t.equals(Words.DbCmd.WORD_EXISTS) || t.equals(Words.DbCmd.WORD_EXISTS_M)) want = Boolean.TRUE;
            else if (t.equals(Words.DbCmd.WORD_MISSING) || t.equals(Words.DbCmd.WORD_MISSING_M)) want = Boolean.FALSE;
            else if (t.startsWith(SW_FILES_IN)) {                            // מ0.37: /קבצים:N — כמה קבצים בתיקייה (בלי תתי־תיקיות)
                try { count = Integer.parseInt(t.substring(SW_FILES_IN.length()).trim()); } catch (NumberFormatException e) { count = -1; }
                if (count < 0) { s.error = Words.DbCmd.VERIFY_USAGE; return; }
            } else if (t.equals(W_CONTAINS) && i + 1 < conds.size()) has = conds.get(++i);   // מ0.37: מכיל "טקסט"
            else { s.error = Words.DbCmd.VERIFY_USAGE; return; }
        }
        if (index) {
            planVerifyIndex(s, f);
            return;
        }
        if (trash > -2) {
            planVerifyTrash(s, f, trash);
            return;
        }
        final boolean exists = want == null || want;
        final int n = count;
        final String text = has;
        final String what = "\"" + f.getName() + "\" " + (n >= 0 ? Words.DbCmd.verifyFilesIn(n) : text != null ? Words.DbCmd.verifyContains(text)
                : exists ? Words.DbCmd.WORD_EXISTS_M : Words.DbCmd.WORD_MISSING_M);
        s.target = f.getName();
        s.describe = Words.DbCmd.verifyWill(what);
        s.op = new ListCmd.Op() {
            @Override
            public String run(Library lib) {
                boolean there = f.isFile() || f.isDirectory();                   // מ0.37: גם תיקייה
                boolean ok;
                String got;
                if (n >= 0) {
                    int c = 0;
                    File[] k = f.listFiles();
                    if (k != null) for (File x : k) if (x.isFile()) c++;
                    ok = f.isDirectory() && c == n;
                    got = f.isDirectory() ? Words.DbCmd.verifyFilesIn(c) : Words.DbCmd.WORD_MISSING_M;
                } else if (text != null) {
                    String body = null;
                    try {
                        if (f.isFile()) body = Files.readUtf8(f);
                    } catch (Throwable t) {
                        Log.e("DbListCmd.verifyFile", t);
                    }
                    ok = body != null && body.contains(text);
                    got = body == null ? Words.DbCmd.WORD_MISSING_M : ok ? Words.DbCmd.verifyContains(text) : Words.DbCmd.VERIFY_NOT_CONTAINS;
                } else {
                    ok = there == exists;
                    got = there ? Words.DbCmd.WORD_EXISTS_M : Words.DbCmd.WORD_MISSING_M;
                }
                s.verified = ok;
                return Words.DbCmd.verifyResult(ok, what, got);
            }
        };
    }

    /** מ0.37: "ודא "<תיקייה>" /קבצים:N" · "ודא "<קובץ>" מכיל "<טקסט>"". */
    static final String SW_FILES_IN = "/קבצים:", W_CONTAINS = "מכיל";   // תחביר
    /** מ0.42: "ודא "<קובץ>" באינדקס" — מה שהיה "בדיקת מסלול באינדקס" בהגדרות. */
    static final String W_IN_INDEX = "באינדקס";   // תחביר

    /** מ0.42: האם האינדקס מכיר את הקובץ (ובכונן — קיים?). בהרצה — מול האינדקס של אותו רגע, אחרי שהתור שלו התרוקן. */
    private static void planVerifyIndex(final Batch.Step s, final File f) {
        final String what = Words.DbCmd.verifyInIndex(f.getName());
        s.target = f.getName();
        s.describe = Words.DbCmd.verifyWill(what);
        s.op = new ListCmd.Op() {
            @Override
            public String run(Library lib) {
                if (lib != null) lib.flush(5000);                                  // השינויים שבתור (כתוב, העתק…) — נכנסים קודם
                boolean in = lib != null && lib.find(f.getAbsolutePath()) != null;
                s.verified = in;
                return Words.DbCmd.verifyResult(in, what, Words.DbCmd.indexGot(in, f.isFile()));
            }
        };
    }

    /** מ0.45: "ודא "<קובץ>" בסל" — יש בסל משהו שנמחק מהמסלול הזה; "/בסל:N" — בדיוק N. */
    private static void planVerifyTrash(final Batch.Step s, final File f, final int n) {
        final String what = Words.DbCmd.verifyInTrash(f.getName(), n);
        s.target = f.getName();
        s.describe = Words.DbCmd.verifyWill(what);
        s.op = new ListCmd.Op() {
            @Override
            public String run(Library lib) {
                if (lib != null) lib.flush(5000);
                int c = lib == null ? 0 : Batch.trashed(lib, f.getAbsolutePath(), false).size();
                boolean ok = n < 0 ? c > 0 : c == n;
                s.verified = ok;
                return Words.DbCmd.verifyResult(ok, what, Words.DbCmd.trashGot(c));
            }
        };
    }

    /**
     * מ0.42: "ודא מסד" — מה שהיה חלק 11 של דוח האבחון: המנוע של המסד מול ה־SQLite של המכשיר (StoreContract ו־BackupContract,
     * על מסדים זמניים בצד), ותקינות המסד של כל אופן (לקריאה בלבד). לא נוגע במסדים — ולכן גם לא בהדמיה.
     */
    static void planVerifyDb(final Batch.Step s) {
        if (DbListScreen.app == null) { s.error = Words.DbCmd.VERIFY_DB_ONLY; return; }
        final String what = Words.DbCmd.VERIFY_DB_WHAT;
        s.describe = Words.DbCmd.verifyWill(what);
        s.op = new ListCmd.Op() {
            @Override
            public String run(Library lib) {
                android.content.Context c = DbListScreen.app;
                boolean[] ok = {false};
                String engine = DbScreen.selfTestText(c, ok).trim().replace("\n", " · ");
                List<String> bad = new ArrayList<String>();
                int good = 0;
                for (String m : Db.dbModes(c)) {
                    SqlStore st = null;
                    try {
                        st = SqlStore.openReadOnly(Db.dbFile(c, m));
                        String chk = st.check();
                        if (chk == null) good++;
                        else bad.add(m + ": " + chk);
                    } catch (Throwable t) {
                        Log.e("DbListCmd.verifyDb", t);
                        bad.add(m + ": " + t.getMessage());
                    } finally {
                        if (st != null) st.close();
                    }
                }
                boolean all = ok[0] && bad.isEmpty();
                s.verified = all;
                return Words.DbCmd.verifyDbResult(all, engine, good, bad);
            }
        };
    }

    /**
     * מ0.42: "צור אופן "<שם>"" — אופן חדש: מסד עם בית (כמו "אופן חדש" בתפריט האופנים). קיים — בלי שינוי. ההדמיה מכירה אותו
     * מהשורה הזו: שורת "אופן" שאחריה עובדת, על מסד ריק בזיכרון. המשתמש (על הבדיקות): "סתם מצטבר שם שאריות".
     */
    static void planMode(final Batch.Step s, String raw, Batch.Sim sim) {
        final String name = raw == null ? "" : raw.trim();
        if (name.isEmpty() || !name.equals(Names.cleanName(name).replaceAll("[:*?\"<>|\n\t]", " ").trim())) {
            s.error = Words.DbCmd.modeBadName(name);
            return;
        }
        if (!DbVerbs.active()) { s.error = Words.DbCmd.VERIFY_DB_ONLY; return; }
        s.target = name;
        if (Db.dbModes(DbListScreen.app).contains(name) || sim.created.contains(name)) {
            s.noop = true;
            s.describe = Words.DbCmd.modeExists(name);
            return;
        }
        sim.created.add(name);
        MemStore m = new MemStore(IdCounter.memory());
        DbPaths.ensureHome(m);
        sim.dbs.put(name, m);
        s.describe = Words.DbCmd.modeWill(name);
        s.op = new ListCmd.Op() {
            @Override
            public String run(Library lib) {
                if (Db.dbModes(DbListScreen.app).contains(name)) return Words.DbCmd.modeExists(name);
                if (DbScreen.prepare(DbListScreen.app, name) != 0) return Words.DbCmd.modeCreated(name);
                lastFailed = true;
                return Words.DbCmd.modeFailed(name);
            }
        };
    }

    /**
     * מ0.45: "בטל אופן "<שם>"" (נבנה בשם "מחק אופן", שעדיין נקרא) — המסד של האופן נמחק (עם קובצי היומן שלו). יש בו משהו — גיבוי לפני (לא נשמר — לא נמחק); הגיבויים
     * נשארים, ו"צור אופן" באותו שם מחזיר אליהם. לא: האופן הפתוח במסך, "שמור לתוכנה", והאופן שהקובץ עובד בו עכשיו. אין אופן כזה —
     * בלי שינוי (ניקוי שאפשר להריץ שוב). ההדמיה: "אופן" אחריו — שגיאה.
     */
    static void planDropMode(final Batch.Step s, final String name, Batch.Sim sim) {
        if (!DbVerbs.active()) { s.error = Words.DbCmd.VERIFY_DB_ONLY; return; }
        s.target = name;
        if (!sim.hasMode(name)) {
            s.noop = true;
            s.describe = Words.DbCmd.dropModeNone(name);
            return;
        }
        if (name.equals(Menus.MODE)) { s.error = Words.DbCmd.dropModeSystem(name); return; }
        if (name.equals(DbListScreen.currentMode) || name.equals(sim.mode)) { s.error = Words.DbCmd.dropModeOpen(name); return; }
        sim.created.remove(name);
        sim.dropped.add(name);
        sim.dbs.remove(name);
        s.describe = Words.DbCmd.dropModeWill(name);
        s.op = new ListCmd.Op() {
            @Override
            public String run(Library lib) {
                android.content.Context c = DbListScreen.app;
                File f = Db.dbFile(c, name);
                if (!f.isFile()) return Words.DbCmd.dropModeNone(name);
                if (name.equals(DbListScreen.currentMode)) { lastFailed = true; return Words.DbCmd.dropModeOpen(name); }
                boolean kept = false;
                SqlStore st = null;
                try {
                    st = SqlStore.open(f, new IdCounter(Db.counterFile()));
                    long home = DbPaths.home(st);
                    boolean empty = st.lists().isEmpty() && st.smarts().isEmpty() && st.unassigned().isEmpty()
                            && (home == 0 || st.members(home).isEmpty());
                    if (!empty) {
                        Db.backupsOf(c, name).before(st, Backups.OP_DROP_MODE, System.currentTimeMillis());
                        kept = true;
                    }
                } catch (Throwable t) {
                    Log.e("DbListCmd.dropMode", t);
                    lastFailed = true;
                    return Words.DbCmd.dropModeFailed(name, Words.DbCmd.DROP_MODE_NO_BACKUP);
                } finally {
                    if (st != null) st.close();
                }
                if (!android.database.sqlite.SQLiteDatabase.deleteDatabase(f) && f.exists()) {
                    lastFailed = true;
                    return Words.DbCmd.dropModeFailed(name, Words.Db.failed(f.getName()));
                }
                if (name.equals(DbScreen.savedMode(c))) DbScreen.saveMode(c, "");   // הפתיחה הבאה — באופן אחר
                // מ0.45+ (claude/bitul-ofen.md, המשתמש: "למה להשאיר? בלי אופן היא סתם בלבול"): תיקיית ההגדרות שלו — לסל (לא לצמיתות)
                boolean folder = false;
                String note = "";
                File dir = Modes.dirOf(name);
                if (dir.isDirectory() && !name.equals(Modes.current()) && lib != null) {
                    Physical.Result r = Physical.delete(lib, new ArrayList<String>(), java.util.Collections.singletonList(dir.getAbsolutePath()), false);
                    folder = r.failed == 0 && !r.done.isEmpty();
                    if (!folder) {
                        note = Words.DbCmd.DROP_MODE_FOLDER_KEPT;
                        Log.i("ביטול אופן: תיקיית ההגדרות לא עברה לסל — " + dir + " · " + r.note);
                    }
                }
                Log.i("בוטל אופן: " + name + (kept ? " (גיבוי לפני)" : " (ריק)") + (folder ? " · התיקייה לסל" : ""));
                return Words.DbCmd.dropModeDone(name, kept, folder) + note;
            }
        };
    }

    /** העותק בזיכרון — פעם אחת לכל קובץ פקודות (השורות הבאות רואות מה שהקודמות עשו). */
    private static Store sim(Batch.Sim sim, String mode) {
        if (sim.db != null) return sim.db;
        SqlStore real = SqlStore.openReadOnly(Db.dbFile(DbListScreen.app, mode));
        try {
            sim.db = copyOf(real);
        } finally {
            real.close();
        }
        return sim.db;
    }

    private static Finder finder(final Batch.Sim sim) {
        return over(sim.lib, sim.condFiles);
    }

    /** הקבצים לפי תנאי מהאינדקס — חיפוש אחד לכל תנאי (cache). */
    static Finder over(final Library lib, final Map<String, List<String>> cache) {
        return new Finder() {
            @Override
            public List<String> find(String cond) {
                if (cond == null || cond.trim().isEmpty()) return new ArrayList<String>();
                if (lib == null || !lib.ready) return null;
                List<String> had = cache.get(cond);
                if (had != null) return had;
                List<String> out = new ArrayList<String>();
                for (Library.Hit h : lib.search(cond, false, true).hits) if (h.rec != null) out.add(h.path());
                cache.put(cond, out);
                return out;
            }
        };
    }

    /** מ0.13: פעולה מהמסך — דרך אותה הליבה; לפני הסרה — גיבוי. מחזיר את ההודעה. */
    static String now(String mode, Work w, boolean before) {
        return real(mode, w, before, null);
    }

    /** "ניקוי" שבסוף ההרצה (RunScreen): הרשימות שההרצה יצרה — יוצאות מהמסד (גיבוי לפני). מחזיר את מה שיצא. */
    static List<String> removeLists(final List<List<String>> paths, String fileMode) {
        final String mode = fileMode != null ? fileMode : mode();   // מ0.17: האופן של הקובץ ("אופן"), אם נכתב
        final List<String> gone = new ArrayList<String>();
        String r = real(mode, new Work() {
            @Override
            public Res run(Store t, Finder f) {
                for (List<String> p : paths) {
                    ListCmd.Side x = new ListCmd.Side();
                    x.kind = ListCmd.LIST;
                    x.raw = ListCmd.pathText(p);
                    x.path = new ArrayList<String>(p);
                    Res one = remove(t, mode, x);
                    if (one.error == null && !one.noop) gone.add(x.raw);
                }
                return Res.ok("");
            }
        }, true, null);
        if (r != null && !r.isEmpty()) Log.i("DbListCmd.removeLists: " + r);
        return gone;
    }

    /** ההרצה: המסד נפתח לרגע; לפני פעולה שמוציאה — גיבוי "לפני פעולה" (נכשל — לא מבצעים). */
    /** מ0.37: "גבה" — גיבוי ידני של המסד של האופן (עותק וגם טקסט); גיבוי ידני לא נמחק לבד. */
    static String backupNow(String mode) {
        SqlStore s = null;
        try {
            s = SqlStore.open(Db.dbFile(DbListScreen.app, mode), new IdCounter(Db.counterFile()));
            List<Backups.Entry> made = Db.backupsOf(DbListScreen.app, mode).manual(s, System.currentTimeMillis());
            StringBuilder names = new StringBuilder();
            for (Backups.Entry e : made) names.append(names.length() > 0 ? ", " : "").append(e.name);
            return Words.DbCmd.backedUp(mode, names.toString());
        } catch (Throwable t) {
            Log.e("DbListCmd.backupNow", t);
            lastFailed = true;
            return Words.Db.failed(String.valueOf(t.getMessage()));
        } finally {
            if (s != null) s.close();
        }
    }

    /** מ0.31: הפעולה האחרונה במסד נכשלה — ההרצה סופרת כישלונות ("המשך"). */
    static volatile boolean lastFailed;

    private static String real(String mode, Work w, boolean before, Library lib) {
        Res r = realRes(mode, w, before, lib);
        if (r.error != null) lastFailed = true;
        return r.error != null ? r.error : Words.DbCmd.inDb(r.text);
    }

    /** מ0.23: כמו now, אבל התוצאה עצמה — כדי לדעת אם נכשל (מסכים שמראים הודעה משלהם). */
    static Res nowRes(String mode, Work w, boolean before) {
        return realRes(mode, w, before, null);
    }

    private static Res realRes(String mode, Work w, boolean before, Library lib) {
        SqlStore s = null;
        try {
            s = SqlStore.open(Db.dbFile(DbListScreen.app, mode), new IdCounter(Db.counterFile()));
            if (before) {
                try {
                    Db.backupsOf(DbListScreen.app, mode).before(s, Backups.OP_DELETE, System.currentTimeMillis());
                } catch (Throwable t) {
                    Log.e("DbListCmd.before", t);
                    return Res.err(Words.DbCmd.beforeFailed(String.valueOf(t.getMessage())));
                }
            }
            Res r = w.run(s, over(lib, new HashMap<String, List<String>>()));
            if (r.error == null && r.renamed != 0) follow(s, mode, r);
            return r;
        } catch (Throwable t) {
            Log.e("DbListCmd.real", t);
            return Res.err(Words.Db.failed(String.valueOf(t.getMessage())));
        } finally {
            if (s != null) s.close();
        }
    }

    /** מ0.42 (ב8): אחרי שינוי שם — קובצי הפקודות שפונים לרשימה מתעדכנים, וההודעה אומרת כמה ואילו. תקלה — נאמרת, לא נבלעת. */
    private static void follow(Store s, String mode, Res r) {
        try {
            List<File> fs = Shortcuts.listRenamed(s, mode, r.renamed, r.renamedFrom, r.renamedTo);
            if (fs.isEmpty()) return;
            List<String> names = new ArrayList<String>();
            for (File f : fs) names.add(Text.stem(f.getName()));
            r.text = (r.text == null ? "" : r.text) + Words.DbCmd.followed(names);
        } catch (Throwable t) {
            Log.e("DbListCmd.follow", t);
            r.text = (r.text == null ? "" : r.text) + Words.DbCmd.FOLLOW_FAILED;
        }
    }

    /**
     * מ0.25: תיקיית היעד של קובץ כשמבקשים /מסלול מרשימה — היעד + המסלול המלא של התיקייה שלו בכונן (בלי שורש הכונן):
     * /storage/emulated/0/Download/א/ב.pdf ← <יעד>/Download/א. כרטיס: /storage/XXXX-XXXX/… — אותו דבר. מסלול אחר — כולו.
     */
    /**
     * הקבצים שברשימה (ידנית, חכמה, "קבצי …") — אל תיקייה בכונן: העתקה, ומ0.40 גם העברה (המשתמש, על התוספים: "המריץ מעביר את
     * כולם, ואם צריך משנה שם"). הביצוע — הרגיל של הכונן (Batch.run); כאן רק התכנון, קובץ אחרי קובץ על ההדמיה.
     */
    private static void toFolder(Batch.Step s, String cmd, ListCmd.Side a, Store db, String mode, Finder find, List<String> sw,
                                 Batch.Sim sim, File fsDest) {
        if (fsDest == null || !sim.isDir(fsDest)) { s.error = Words.Cmd.NO_DEST; return; }
        final boolean copy = cmd.equals("COPY");
        String[] err = {null};
        List<String> fs = files(db, mode, a, find, err);
        if (fs == null) { s.error = err[0]; return; }
        s.dest = fsDest;
        // מ0.26: המתגים של שורת רשימה לא נקראים ב־Batch.plan (שם רק שורות כונן) — נקראים כאן. במ0.25 זה הוחמץ,
        // ולכן התיקון לא פעל במכשיר ("הועתקו 1" במקום "דולגו", והקובץ ישר בתיקייה).
        for (String x : sw) {
            String u = x.toUpperCase(java.util.Locale.ROOT);
            if (u.equals("/Y") || x.equals(Batch.SW_TAKEN + Batch.TAKEN_REPLACE)) s.replace = true;
            if (x.equals(Batch.SW_TAKEN + Batch.TAKEN_SKIP)) s.skipTaken = true;
            if (x.equals(Batch.SW_TAKEN + Batch.TAKEN_STOP)) s.failTaken = true;
            if (x.equals(Batch.SW_TAKEN + Batch.TAKEN_RENAME)) s.renameTaken = true;
            if (x.startsWith(Batch.SW_TAKEN) && !Batch.takenValue(x)) { s.error = Batch.takenUnknown(x); return; }
            if (u.equals("/S") || x.equals(Batch.SW_KEEP)) s.recursive = true;
        }
        Batch.Filter flt = Batch.filter(sw);                                   // מ0.40: /מכיל:
        if (flt.error != null) { s.error = flt.error; return; }
        if (!flt.contains.isEmpty()) s.contains = flt.contains;
        // מ0.25 (באג 4): /תפוס:לדלג ו־/מסלול גם מרשימה לתיקייה. /מסלול — המסלול המלא של הקובץ מהכונן, בתוך היעד
        // (המשתמש: "אם הבקשה עם מסלול אז עם כל המסלול"); לכל קובץ תיקיית יעד משלו (targets, כמו בשחזור).
        for (String x : fs) {
            File src = new File(x);
            if (!sim.isFile(src)) continue;
            s.total++;
            if (!flt.ok(sim.real(src))) continue;
            File dir = s.recursive ? fullPathUnder(fsDest, src) : fsDest;
            if (!copy && dir.equals(src.getParentFile())) { s.inPlace++; continue; }   // מ0.40: כבר שם — כלום
            File to = new File(dir, src.getName());
            if (s.drop(sim, src, to)) continue;
            s.files.add(src.getAbsolutePath());
            if (s.recursive) {
                s.targets.add(dir.getAbsolutePath());
                for (File up = dir; up != null && !up.equals(fsDest); up = up.getParentFile()) sim.dirs.add(up.getAbsolutePath());
            }
            sim.move(src.getAbsolutePath(), s.arrive(sim, to), copy);
        }
        int passed = s.files.size() + s.skipped + s.same + s.inPlace;
        s.describe = Words.Cmd.transfer(copy, Words.files(s.files.size()), Bidi.path(fsDest.getAbsolutePath()))
                + (s.recursive ? Words.Cmd.KEEP_PATH : "") + (s.total > passed ? Words.Cmd.filtered(passed, s.total) : "") + Batch.notes(s);
        s.noop = s.files.isEmpty() && s.same + s.inPlace > 0;
    }

    static File fullPathUnder(File dest, File src) {
        String p = src.getParentFile() == null ? "" : src.getParentFile().getAbsolutePath();
        String rel = p;
        if (p.startsWith("/storage/emulated/")) {
            int e = p.indexOf('/', "/storage/emulated/".length());
            rel = e < 0 ? "" : p.substring(e + 1);
        } else if (p.startsWith("/storage/")) {
            int e = p.indexOf('/', "/storage/".length());
            rel = e < 0 ? "" : p.substring(e + 1);
        }
        while (rel.startsWith("/")) rel = rel.substring(1);
        return rel.isEmpty() ? dest : new File(dest, rel);
    }
}
