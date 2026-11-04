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
// אחראי: הלוגיקה (על Store — נבדק במחשב על שני המימושים) והחיבור לתכנון של Batch. אסור לו: לגעת במנוע הישן.

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
        if (x.home) return Importer.home(s);
        return DbPaths.find(s, mode, x.path);
    }

    /** רשימה חכמה: {הרשימה שבה (0 — עומדת לעצמה), החכמה}, או null. בלי דרך — בבית, ואם אין שם — לפי השם בכל המסד. */
    static long[] smart(Store s, String mode, ListCmd.Side x) {
        if (x.path == null || x.path.isEmpty()) return null;
        String name = x.path.get(x.path.size() - 1).trim();
        List<String> parent = new ArrayList<String>(x.path.subList(0, x.path.size() - 1));
        long in = parent.isEmpty() ? Importer.home(s) : DbPaths.find(s, mode, parent);
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
        List<String> up = new ArrayList<String>(a.path.subList(0, a.path.size() - 1));
        final long parent = up.isEmpty() ? Importer.home(s) : DbPaths.find(s, mode, up);
        s.atomic(new Store.Work() {
            @Override public void run(Store t) {
                if (parent != 0) t.removeMember(parent, id);
                dropIfAlone(t, id);
            }
        }, true);
        return Res.ok(Words.DbCmd.removed(Words.ListCmdW.list(a.label())));
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
        final long dest = b.home ? Importer.home(s) : b.mask != null ? 0 : DbPaths.find(s, mode, b.path);
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
            DbFiles.Added r = DbFiles.add(s, dest, ps, null);
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
                label = a.mask != null ? Words.files(what.size()) : a.home ? Words.ListCmdW.homeContent(what.size())
                        : Words.ListCmdW.contentOf(what.size(), a.label());
            } else {
                if (src == dest || s.contains(src, dest)) return Res.err(Words.ListCmdW.INTO_ITSELF);
                what.add(s.item(src));
                label = Words.ListCmdW.list(a.label());
            }
        }
        final int[] done = {0}, skipped = {0};
        final String[] why = {null};
        s.atomic(new Store.Work() {
            @Override public void run(Store t) {
                for (Store.Item it : what) {
                    String w = copyItem(t, it, dest);
                    if (w == null) done[0]++;
                    else {
                        skipped[0]++;
                        if (why[0] == null) why[0] = w;
                    }
                }
            }
        }, true);
        String selfText = self.isEmpty() ? "" : Words.ListCmdW.notIntoSelf(self);
        return Res.ok(Words.ListCmdW.copy(label, b.label()) + names(what) + selfText + " · "
                + Words.ListCmdW.copied(done[0], skipped[0], why[0], b.label()) + nowIn(s, dest));
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

    /** עותק עצמאי של רשימה: מה שרק שלה — מועתק; רשימה שגם במקום אחר — אותה רשימה; חכמה — עותק של ההגדרה. */
    private static long copyList(Store t, long src, long into) {
        Store.Item it = t.item(src);
        long id = t.createList(it.name, into);
        copyProps(t, src, id, LIST_PROPS);
        for (Store.Member m : t.members(src)) {
            Store.Item x = m.item;
            if (x.type == Store.LIST) {
                if (only(t, x.id, src)) copyList(t, x.id, id);
                else if (!t.makesCycle(id, x.id)) t.addMember(id, x.id);
            } else if (x.type == Store.SMART) copySmart(t, x.id, id);
            else if (x.type == Store.NOTE) t.addNote(id, x.name);
            else if (x.type == Store.TITLE) t.addTitle(id, x.name);
            else t.addMember(id, x.id);
        }
        return id;
    }

    /** הרשימה x נמצאת רק ב-parent (רשימה שגם בבית — משותפת). */
    private static boolean only(Store t, long x, long parent) {
        for (Long in : t.listsOf(x)) if (in != parent) return false;
        return true;
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
        for (Store.Item it : src.lists()) id.put(it.id, m.createList(it.name, 0));
        for (Store.Item it : src.smarts()) id.put(it.id, m.createSmart(it.name, 0));
        for (Map.Entry<Long, Long> e : id.entrySet()) {
            for (String k : ALL_PROPS) {
                String v = src.prop(e.getKey(), k);
                if (v != null) m.setProp(e.getValue(), k, v);
            }
            String o = src.origin(e.getKey());
            if (o != null && !o.isEmpty()) m.setOrigin(e.getValue(), o);
        }
        for (Store.Item it : src.lists())
            for (Store.Member mb : src.members(it.id)) {
                long into = id.get(it.id);
                Store.Item x = mb.item;
                if (x.type == Store.NOTE) m.addNote(into, x.name);
                else if (x.type == Store.TITLE) m.addTitle(into, x.name);
                else {
                    Long to = id.get(x.id);
                    if (to == null && x.type == Store.FILE) id.put(x.id, to = fileOf(src, m, x.id));
                    if (to != null) m.addMember(into, to);
                }
            }
        for (Store.Item it : src.unassigned()) if (it.type == Store.FILE && !id.containsKey(it.id)) fileOf(src, m, it.id);
        return m;
    }

    private static long fileOf(Store src, MemStore m, long item) {
        Store.FileInfo fi = src.file(item);
        long had = m.findFile(fi.path);
        return had != 0 ? had : m.createFile(fi);
    }

    // ---------- החיבור לתכנון של Batch ----------

    /** האופן שהפקודות עובדות עליו עכשיו (הבית של המסד שפתוח). */
    static String mode() {
        return DbListScreen.currentMode;
    }

    /** שורה עם מילות סוג, כשעובדים במסד: אותן בדיקות צורה כמו ListCmd.plan; הפעולה — על העותק בזיכרון, ובהרצה — על המסד. */
    static void plan(final Batch.Step s, final String cmd, ListCmd.Parsed p, List<String> sw, final Batch.Sim sim,
                     final List<String> fsFiles, final File fsDest) {
        for (String x : sw)
            if (x.startsWith("/" + Batch.SW_DATE) || x.startsWith("/" + Batch.SW_SIZE)) {
                s.error = Words.ListCmdW.NO_FILTER;
                return;
            }
        final String mode = mode();
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
            if (b.kind == ListCmd.FS) {                                      // מרשימה לתיקייה — הקבצים עצמם (הביצוע הרגיל)
                if (fsDest == null || !sim.isDir(fsDest)) { s.error = Words.Cmd.NO_DEST; return; }
                String[] err = {null};
                List<String> fs = files(db, mode, a, find, err);
                if (fs == null) { s.error = err[0]; return; }
                s.dest = fsDest;
                for (String x : fs) if (sim.isFile(new File(x))) s.files.add(new File(x).getAbsolutePath());
                s.describe = Words.Cmd.transfer(true, Words.files(s.files.size()), Bidi.path(fsDest.getAbsolutePath()));
                for (String x : s.files) sim.move(x, new File(fsDest, new File(x).getName()), true);
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
            else s.describe = r.text;
            return;
        } else if (cmd.equals("MOVE")) {
            boolean toList = sd.size() == 2 && sd.get(1).list();
            s.error = toList && !sd.get(0).list() ? Words.ListCmdW.MOVE_TO_LIST : Words.ListCmdW.MOVE_LATER;
            return;
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
        s.noop = r.noop;
        if (r.noop) return;
        final Work w = work;
        final boolean before = destructive;
        s.op = new ListCmd.Op() {
            @Override public String run(Library lib) { return real(mode, w, before, lib); }
        };
    }

    /** העותק בזיכרון — פעם אחת לכל קובץ פקודות (השורות הבאות רואות מה שהקודמות עשו). */
    private static Store sim(Batch.Sim sim, String mode) {
        if (sim.db != null) return sim.db;
        SqlStore real = SqlStore.openReadOnly(DbScreen.dbFile(DbListScreen.app, mode));
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

    /** "ניקוי" שבסוף ההרצה (RunScreen): הרשימות שההרצה יצרה — יוצאות מהמסד (גיבוי לפני). מחזיר את מה שיצא. */
    static List<String> removeLists(final List<List<String>> paths) {
        final String mode = mode();
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
    private static String real(String mode, Work w, boolean before, Library lib) {
        SqlStore s = null;
        try {
            s = SqlStore.open(DbScreen.dbFile(DbListScreen.app, mode), new IdCounter(DbScreen.counterFile()));
            if (before) {
                try {
                    DbScreen.backupsOf(DbListScreen.app, mode).before(s, Backups.OP_DELETE, System.currentTimeMillis());
                } catch (Throwable t) {
                    Log.e("DbListCmd.before", t);
                    return Words.DbCmd.beforeFailed(String.valueOf(t.getMessage()));
                }
            }
            Res r = w.run(s, over(lib, new HashMap<String, List<String>>()));
            return r.error != null ? r.error : r.text;
        } catch (Throwable t) {
            Log.e("DbListCmd.real", t);
            return Words.Db.failed(String.valueOf(t.getMessage()));
        } finally {
            if (s != null) s.close();
        }
    }
}
