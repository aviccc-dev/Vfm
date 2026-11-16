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
        /** מבנה 91: המספר של מה שנוצר (רשימה חדשה מהמסך). */
        long made;
        /** מבנה 104: נדרס מה שהיה (רשימה חכמה בשם קיים) — בהרצה: גיבוי לפני. */
        boolean replaced;
        /** מבנה 116: הפריטים (מספרים) שהפעולה הכניסה ליעד — כדי שהלוח של המסך ידע מה עבר. */
        List<Long> entered;

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

    private static final String[] ALL_PROPS = {Store.P_ICON, Store.P_DESC, Store.P_DESIGN, Store.P_WHY, Store.P_COND,
            Store.P_SORT, Store.P_DIR, Store.P_VIEW, Store.P_GROUP, Store.P_HOME};

    // ---------- מציאה ----------

    /** הרשימה הידנית של הצד: "*" — הבית; אחרת לפי המסלול (DbPaths). 0 — אין. */
    static long list(Store s, String mode, ListCmd.Side x) {
        if (x.home) return DbPaths.home(s);
        return DbPaths.find(s, mode, x.path);
    }

    /** מבנה 107: למה הצד אינו מוביל לרשימה — "אין", או "יש כמה… לכתוב את המקום". */
    static String listWhy(Store s, String mode, ListCmd.Side x) {
        if (x.home) return Words.DbCmd.NO_HOME;
        DbPaths.Found f = DbPaths.listAt(s, mode, x.path);
        return f.ambiguous ? f.error : Words.ListCmdW.noList(x.raw);
    }

    /** רשימה חכמה: {הרשימה שבה (0 — עומדת לעצמה), החכמה}, או null. בלי דרך — בבית, ואם אין שם — לפי השם בכל המסד. */
    static long[] smart(Store s, String mode, ListCmd.Side x) {
        DbPaths.Found f = DbPaths.smartAt(s, mode, x.path);                       // מבנה 106: הכלל האחד — דרך, או שם בלי ספק
        return f.error != null ? null : new long[]{f.in, f.id};
    }

    /** מבנה 106: למה הצד אינו מוביל לרשימה חכמה — "אין", או "יש כמה… לכתוב את המקום". */
    static String smartWhy(Store s, String mode, ListCmd.Side x) {
        String e = DbPaths.smartAt(s, mode, x.path).error;
        return e != null ? e : Words.ListCmdW.noSmart(x.raw);
    }

    /** הקבצים שהצד מצביע עליהם: ידנית — בכל העומק (כל רשימה פעם אחת); חכמה — מה שהיא מוצאת עכשיו. ממוינים, בלי כפולים. */
    static List<String> files(Store s, String mode, ListCmd.Side x, Finder f, String[] err) {
        Set<String> out = new LinkedHashSet<String>();
        if (x.kind == ListCmd.SMART_LIST) {
            long[] r = smart(s, mode, x);
            if (r == null) {
                err[0] = smartWhy(s, mode, x);
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
                err[0] = listWhy(s, mode, x);
                return null;
            }
            if (x.mask != null) {
                for (Store.Member m : s.members(id)) {
                    String p = pathOf(s, m.item);
                    if (p != null && Parse.wild(x.mask, new File(p).getName())) out.add(p);
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

    static String pathOf(Store s, Store.Item it) {
        if (it.type != Store.FILE) return null;
        Store.FileInfo fi = s.file(it.id);
        return fi == null ? null : fi.path;
    }

    // ---------- הפעולות ----------

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
        return Db.currentMode;
    }

    /** מ0.15: האופן של הקובץ — מ"אופן" שבו, אחרת זה שפתוח. */
    static String mode(Sim sim) {
        return sim != null && sim.mode != null ? sim.mode : mode();
    }

    /** שורה עם מילות סוג, כשעובדים במסד: אותן בדיקות צורה כמו ListCmd.plan; הפעולה — על העותק בזיכרון, ובהרצה — על המסד. */
    static void plan(final Batch.Step s, final String cmd, ListCmd.Parsed p, List<String> sw, final Sim sim,
                     final List<String> fsFiles, final File fsDest, final List<String> fsDirs) {
        boolean disk = false;                                                  // מ0.40: יש צד בכונן — שם /מכיל: חל
        for (ListCmd.Side x : p.sides) if (!x.list()) disk = true;
        for (String x : sw)
            if (x.startsWith("/" + Syntax.SW_DATE) || x.startsWith("/" + Syntax.SW_SIZE) || (!disk && x.startsWith("/" + Syntax.SW_CONTAINS))) {
                s.error = Words.ListCmdW.NO_FILTER;
                return;
            }
        for (ListCmd.Side x : p.sides)                                        // מ0.41: "לא-משויכים" — רק "בטל לא-משויכים" (ו"ודא")
            if (x.kind == ListCmd.LOOSE && !((cmd.equals("RD") || cmd.equals("DEL")) && p.sides.size() == 1)) {
                s.error = Words.DbCmd.LOOSE_USAGE;
                return;
            }
        if (sim.modeError != null) { s.error = sim.modeError; return; }      // מ0.17.1: "אופן" שנכשל — לא לרוץ על אופן אחר
        for (ListCmd.Side x : p.sides)
            if (x.picked) {                                                    // מבנה 85: מה שנבחר במסך הרשימות
                planPicked(s, cmd, p, sw, sim, fsFiles, fsDirs);
                return;
            }
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
        Work work;
        if (cmd.equals("MD")) work = planMd(s, cmd, sd, sw, sim, db, mode, find, fsDest, fsFiles, fsDirs);
        else if (cmd.equals("RD") || cmd.equals("DEL")) work = planRemove(s, cmd, sd, sw, sim, db, mode, find, fsDest, fsFiles, fsDirs);
        else if (cmd.equals("COPY")) work = planCopy(s, cmd, sd, sw, sim, db, mode, find, fsDest, fsFiles, fsDirs);
        else if (cmd.equals("COMP")) work = planComp(s, cmd, sd, sw, sim, db, mode, find, fsDest, fsFiles, fsDirs);
        else if (cmd.equals("REN")) work = planRen(s, cmd, sd, sw, sim, db, mode, find, fsDest, fsFiles, fsDirs);
        else if (cmd.equals("MOVE") || cmd.equals(Syntax.LINK)) work = planLink(s, cmd, sd, sw, sim, db, mode, find, fsDest, fsFiles, fsDirs);
        else {
            s.error = Words.ListCmdW.notYet(cmd);
            return;
        }
        if (work == null) return;
        boolean destructive = cmd.equals("RD") || cmd.equals("DEL") || cmd.equals("MOVE");   // מבנה 173: מה שהיה בענפים — רק שם נוצרת work
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
        // מבנה 108 (ה4; הוראות סעיף 4 — דריסה רק אחרי גיבוי): שינוי שם של רשימה — גיבוי "לפני עריכה", כמו בטופס שבמסך (עד כאן — בלי)
        final String before = destructive ? Backups.OP_DELETE : cmd.equals("REN") ? Backups.OP_EDIT : null;
        s.op = new ListCmd.Op() {
            @Override public String run(Library lib) {
                Res r = ListRun.realRes(mode, w, before, lib);
                if (r.error != null) lastFailed = true;
                return r.error != null ? r.error : Words.DbCmd.inDb(r.text);
            }
        };
    }

    /** מבנה 173: הענף של "cmd.equals(MD)" מתוך plan. */
    private static Work planMd(final Batch.Step s, final String cmd, final List<ListCmd.Side> sd, final List<String> sw, final Sim sim, final Store db,
                                final String mode, final Finder find, final File fsDest, final List<String> fsFiles, final List<String> fsDirs) {
        Work work = null;
        if (sd.size() != 1) { s.error = Commands.usage(cmd); return null; }
        final ListCmd.Side a = sd.get(0);
        if (a.kind == ListCmd.SMART_LIST) { s.error = Words.ListCmdW.MD_SMART; return null; }
        if (a.kind != ListCmd.LIST || a.content || a.filesOf || a.home || a.mask != null) { s.error = Words.ListCmdW.USAGE_MD; return null; }
        s.listPath = a.path;
        final int taken = ListOps.takenOf(sw);                                       // מבנה 109
        work = new Work() {
            @Override public Res run(Store t, Finder f) { return ListOps.md(t, mode, a, taken); }
        };
        return work;
    }

    /** מבנה 173: הענף של "cmd.equals(RD) || cmd.equals(DEL)" מתוך plan. */
    private static Work planRemove(final Batch.Step s, final String cmd, final List<ListCmd.Side> sd, final List<String> sw, final Sim sim, final Store db,
                                final String mode, final Finder find, final File fsDest, final List<String> fsFiles, final List<String> fsDirs) {
        Work work = null;
        if (sd.size() != 1) { s.error = Commands.usage(cmd); return null; }
        final ListCmd.Side a = sd.get(0);
        if (a.filesOf) {                                                 // הקבצים עצמם — לסל (הביצוע הרגיל)
            if (cmd.equals("RD")) { s.error = Words.ListCmdW.RD_FILES; return null; }
            String[] err = {null};
            List<String> fs = files(db, mode, a, find, err);
            if (fs == null) { s.error = err[0]; return null; }
            for (String x : fs) if (sim.isFile(new File(x))) s.files.add(new File(x).getAbsolutePath());
            s.describe = Words.Cmd.toTrash(Words.files(s.files.size())) + Words.ListCmdW.FROM_DISK;
            for (String x : s.files) {
                sim.gone.add(x);
                sim.added.remove(x);
            }
            s.target = a.label();
            return null;
        }
        work = new Work() {
            @Override public Res run(Store t, Finder f) { return ListOps.remove(t, mode, a); }
        };
        return work;
    }

    /** מבנה 173: הענף של "cmd.equals(COPY)" מתוך plan. */
    private static Work planCopy(final Batch.Step s, final String cmd, final List<ListCmd.Side> sd, final List<String> sw, final Sim sim, final Store db,
                                final String mode, final Finder find, final File fsDest, final List<String> fsFiles, final List<String> fsDirs) {
        Work work = null;
        if (sd.size() != 2) { s.error = Words.ListCmdW.USAGE; return null; }
        final ListCmd.Side a = sd.get(0), b = sd.get(1);
        if (b.content || b.filesOf) { s.error = Words.ListCmdW.USAGE; return null; }
        if (b.kind == ListCmd.SMART_LIST) { s.error = Words.ListCmdW.INTO_SMART; return null; }
        String fromMode = a.path == null || a.path.isEmpty() || !a.path.get(0).startsWith(Words.DbCmd.MODE_PREFIX) ? null
                : a.path.get(0).substring(Words.DbCmd.MODE_PREFIX.length()).trim();
        if (fromMode != null) {                                          // מ0.22: מאופן אחר — "אופן:X/א\ב"
            a.path = new ArrayList<String>(a.path.subList(1, a.path.size()));
            if (a.path.isEmpty()) { s.error = Words.DbCmd.USAGE_FROM_MODE; return null; }
            if (!fromMode.equals(mode)) {
                if (!Db.dbModes(Db.app).contains(fromMode)) { s.error = Words.DbCmd.noMode(fromMode, sim.modes()); return null; }
                if (a.kind != ListCmd.LIST || a.filesOf || a.mask != null || b.kind != ListCmd.LIST) { s.error = Words.DbCmd.USAGE_FROM_MODE; return null; }
                final String src = fromMode;
                work = new Work() {
                    @Override public Res run(Store t, Finder f) { return ListOps.copyFromMode(t, mode, src, a, b); }
                };
                s.target = b.label();
                Res pr;
                try { pr = work.run(db, find); } catch (Store.Failure e) { pr = Res.err(Words.Db.failed(e.getMessage())); }
                if (pr.error != null) { s.error = pr.error; return null; }
                s.describe = Words.DbCmd.inDb(pr.text);
                final Work w = work;
                s.op = new ListCmd.Op() {
                    @Override public String run(Library lib) { return ListRun.real(mode, w, false, lib); }
                };
                return null;
            }
        }
        if (a.kind == ListCmd.EXPORT) {                                  // מ0.22: ייבוא מקובץ טקסט לרשימה
            final File in = ListOps.importFile(a.raw);
            work = new Work() {
                @Override public Res run(Store t, Finder f) { return ListOps.importFrom(t, mode, in, b); }
            };
            s.target = b.label();
            if (in.isFile()) {
                Res pr = work.run(db, find);
                if (pr.error != null) { s.error = pr.error; return null; }
                s.describe = Words.DbCmd.inDb(pr.text);
            } else s.describe = Words.DbCmd.importLater(Bidi.path(in.getAbsolutePath()));   // ייווצר בשורות שלפני
            final Work w = work;
            s.op = new ListCmd.Op() {
                @Override public String run(Library lib) { return ListRun.real(mode, w, false, lib); }
            };
            return null;
        }
        if (b.kind == ListCmd.EXPORT) {                                  // מ0.18: יצוא לקובץ טקסט
            if (a.kind == ListCmd.SMART_LIST || a.content || a.mask != null || !a.list()) { s.error = Words.DbCmd.USAGE_EXPORT; return null; }
            work = new Work() {
                @Override public Res run(Store t, Finder f) { return ListOps.export(t, mode, a, b, f, !(t instanceof MemStore)); }
            };
            s.target = b.label();
            Res pr = work.run(db, find);
            if (pr.error != null) { s.error = pr.error; return null; }
            s.describe = Words.DbCmd.inDb(pr.text);
            final Work w = work;
            s.op = new ListCmd.Op() {
                @Override public String run(Library lib) { return ListRun.real(mode, w, false, lib); }
            };
            return null;
        }
        if (b.kind == ListCmd.FS) {                                      // מרשימה לתיקייה — הקבצים עצמם (הביצוע הרגיל)
            toFolder(s, cmd, a, db, mode, find, sw, sim, fsDest);
            return null;
        }
        final boolean live = sw.contains(ListCmd.SW_LIVE), flat = sw.contains(ListCmd.SW_FLAT);
        if ((live || flat) && (a.list() || fsDirs.isEmpty() || (live && flat))) { s.error = Words.ListCmdW.FOLDER_SW_ONLY; return null; }
        if (!a.list() && !fsDirs.isEmpty()) {                               // מבנה 115: תיקייה שלמה מהכונן אל רשימה
            final List<Tree.Node> nodes = new ArrayList<Tree.Node>();
            if (!live) for (String d : fsDirs) nodes.add(ListLines.scan(sim, s, new File(d), !flat));
            final int taken = ListOps.takenOf(sw);
            work = new Work() {
                @Override public Res run(Store t, Finder f) {
                    long dest = b.home ? DbPaths.home(t) : b.mask != null ? 0 : DbPaths.find(t, mode, b.path);
                    if (dest == 0) return Res.err(listWhy(t, mode, b));
                    return ListOps.addFolders(t, dest, b.label(), fsDirs, nodes, fsFiles, live, flat, taken);
                }
            };
        } else work = new Work() {
            @Override public Res run(Store t, Finder f) { return ListOps.copy(t, mode, a, b, fsFiles, f); }
        };
        return work;
    }

    /** מבנה 173: הענף של "cmd.equals(COMP)" מתוך plan. */
    private static Work planComp(final Batch.Step s, final String cmd, final List<ListCmd.Side> sd, final List<String> sw, final Sim sim, final Store db,
                                final String mode, final Finder find, final File fsDest, final List<String> fsFiles, final List<String> fsDirs) {
        if (sd.size() != 2) { s.error = Words.ListCmdW.USAGE_COMP; return null; }
        final ListCmd.Side a = sd.get(0), b = sd.get(1);
        if (!a.list() || !b.list()) { s.error = Words.ListCmdW.COMP_FS; return null; }
        if (sw.contains(ListCmd.SW_DIFF)) {                                  // מבנה 90: כל ההבדלים בין שתי רשימות ידניות
            ListVerify.planDiff(s, a, b, sw, mode);
            return null;
        }
        if (a.content || b.content) { s.error = Words.ListCmdW.USAGE_COMP; return null; }
        final boolean conds = sw.contains(ListCmd.SW_CONDS);
        if (conds && (a.kind != ListCmd.SMART_LIST || b.kind != ListCmd.SMART_LIST || a.filesOf || b.filesOf)) {
            s.error = Words.ListCmdW.CONDS_SMART;
            return null;
        }
        Res r = ListOps.comp(db, mode, a, b, conds, find);
        if (r.error != null) s.error = r.error;
        else s.describe = Words.DbCmd.inDb(r.text);
        return null;
    }

    /** מ0.21: שינוי שם לרשימה — במסד — מבנה 173: הענף של "cmd.equals(REN)" מתוך plan. */
    private static Work planRen(final Batch.Step s, final String cmd, final List<ListCmd.Side> sd, final List<String> sw, final Sim sim, final Store db,
                                final String mode, final Finder find, final File fsDest, final List<String> fsFiles, final List<String> fsDirs) {
        Work work = null;
        if (sd.size() != 2) { s.error = Words.DbCmd.USAGE_REN; return null; }
        final ListCmd.Side a = sd.get(0);
        final String name = sd.get(1).raw == null ? "" : sd.get(1).raw.trim();
        if (!a.list() || a.content || a.filesOf || a.home || a.mask != null || sd.get(1).list()) { s.error = Words.DbCmd.USAGE_REN; return null; }
        work = new Work() {
            @Override public Res run(Store t, Finder f) { return ListOps.rename(t, mode, a, name); }
        };
        return work;
    }

    /** מ0.17: העברה וקישור — במסד — מבנה 173: הענף של "cmd.equals(MOVE) || cmd.equals(Syntax.LINK)" מתוך plan. */
    private static Work planLink(final Batch.Step s, final String cmd, final List<ListCmd.Side> sd, final List<String> sw, final Sim sim, final Store db,
                                final String mode, final Finder find, final File fsDest, final List<String> fsFiles, final List<String> fsDirs) {
        Work work = null;
        if (sd.size() != 2) { s.error = Words.DbCmd.USAGE_LINK; return null; }
        final ListCmd.Side a = sd.get(0), b = sd.get(1);
        if (cmd.equals("MOVE") && a.list() && b.kind == ListCmd.FS && !b.content && !b.filesOf) {   // מ0.40: הקבצים עצמם — לתיקייה
            toFolder(s, cmd, a, db, mode, find, sw, sim, fsDest);
            return null;
        }
        if (!a.list() && cmd.equals("MOVE")) { s.error = Words.ListCmdW.MOVE_TO_LIST; return null; }
        if (!a.list() || a.filesOf || b.content || b.filesOf || b.kind == ListCmd.FS) { s.error = Words.DbCmd.USAGE_LINK; return null; }
        if (b.kind == ListCmd.SMART_LIST) { s.error = Words.ListCmdW.INTO_SMART; return null; }
        final boolean move = cmd.equals("MOVE");
        work = new Work() {
            @Override public Res run(Store t, Finder f) { return ListOps.link(t, mode, a, b, move); }
        };
        return work;
    }

    /**
     * מבנה 85: פעולה של מסך הרשימות על הפריטים שנבחרו בו — "בטל ידנית %נבחרים%" ("הסרה מכאן"). הבחירה היא מספרי הפריטים במסד
     * והרשימה שהם מוצגים בה (Batch.Selection), לא דרך של שמות: כך בוחרים במסך, ושני פריטים באותו שם אינם מתבלבלים.
     * הפעולה עצמה — אותה פעולה של החומר (Lists) שהמסך קרא לה עד כאן, דרך אותה עטיפה של הפקודות (real: שער המסד, גיבוי לפני).
     * **בלי הדמיה:** התכנון של פקודת רשימה כתובה מעתיק את כל המסד לזיכרון כדי שהשורות הבאות יראו את התוצאה; כאן שורה אחת,
     * והמסך כבר הציג את חלון האישור שלו — העתקה של כל המסד בכל לחיצה הייתה עבודה יקרה לחינם (המשפך).
     */
    private static void planPicked(final Batch.Step s, String cmd, ListCmd.Parsed p, List<String> sw, Sim sim, List<String> fsFiles,
                                   List<String> fsDirs) {
        final Batch.Selection sel = sim.selected;
        ListCmd.Side a = p.sides.get(0);
        boolean make = cmd.equals("MD") && p.sides.size() == 1 && sw.isEmpty() && a.kind == ListCmd.LIST && !a.content && !a.filesOf;
        final ListCmd.Side to = p.sides.size() == 2 ? p.sides.get(1) : null;
        // מבנה 114: "העתק %נבחרים% ידנית %נבחרים%" — הקבצים שנבחרו (מסלולים), אל הרשימה שנבחרה (המספר שלה; 0 — הבית)
        // מבנה 115: גם תיקיות שנבחרו — עם /חיה, /קבצים, /תפוס: (כמו בשורה הכתובה)
        final boolean live = sw.contains(ListCmd.SW_LIVE), flat = sw.contains(ListCmd.SW_FLAT);
        final boolean hasDirs = fsDirs != null && !fsDirs.isEmpty();
        boolean add = cmd.equals("COPY") && to != null && !a.list() && to.picked && to.kind == ListCmd.LIST && !to.content && !to.filesOf;
        if (add && (live || flat) && (!hasDirs || (live && flat))) { s.error = Words.ListCmdW.FOLDER_SW_ONLY; return; }
        if (sel == null || sel.ids == null || sel.mode == null
                || (make ? sel.name == null : add ? (fsFiles == null || fsFiles.isEmpty()) && !hasDirs : sel.ids.isEmpty())) {
            s.error = Words.Cmd.NOTHING_SELECTED;
            return;
        }
        final String mode = sel.mode;
        if (!Db.dbFile(Db.app, mode).isFile()) { s.error = Words.DbCmd.noMode(mode, sim.modes()); return; }
        if (make) {
            // מבנה 91: רשימה חדשה מהמסך (➕, בורר הרשימות) — בתוך הרשימה שמוצגת (לפי המספר שלה; 0 — הבית), בשם שהוקלד כלשונו.
            // שלא כמו "צור תיקייה ידנית" כתובה (דרך של שמות; רשימה קיימת — "בלי שינוי"; יוצרת גם את מה שבדרך) — כאן כמו שהמסך עשה:
            // שם אחד, והבדיקה האחת של שם (Lists.nameError): תפוס, ריק, או עם "/" — סירוב, ושום דבר לא נוצר. בלי גיבוי לפני (רק מוסיפה).
            final long into = sel.in;
            final String name = sel.name;
            s.target = a.label();
            s.describe = Words.DbCmd.inDb(Words.ListCmdW.created(Names.cleanName(name)));
            s.op = new ListCmd.Op() {
                @Override public String run(Library lib) {
                    Res r = ListRun.realRes(mode, new Work() {
                        @Override public Res run(Store t, Finder f) {
                            if (into != 0 && (t.item(into) == null || t.item(into).type != Store.LIST)) return Res.err(Words.Db.LIST_GONE);
                            Lists.Made m = Lists.newListChecked(t, into, name);
                            if (m.error != null) return Res.err(m.error);
                            Res ok = Res.ok(Words.ListCmdW.created(Names.cleanName(name)));
                            ok.made = m.id;
                            return ok;
                        }
                    }, false, lib);
                    if (r.error != null) lastFailed = true;
                    s.made = r.made;
                    return r.error != null ? r.error : Words.DbCmd.inDb(r.text);
                }
            };
            return;
        }
        // מבנה 116: ההדבקה מהלוח של הרשימות — "העתק / העבר ידנית %נבחרים% ידנית %נבחרים%": הפריטים שנבחרו (מספרים), מהרשימה
        // שסומנו בה (sel.in — להעברה), אל הרשימה sel.to. אותה ליבה כמו בשורה הכתובה (Lists.copyInto / moveTo), ואותה הודעה;
        // מה שנובע מהמקום: לפי מספרים, ולכן כל פריט (גם קובץ, רעיון, רשימה חכמה) — לא רק מה שיש לו דרך של שמות.
        boolean xfer = (cmd.equals("COPY") || cmd.equals("MOVE")) && to != null && sw.isEmpty() && a.picked && a.kind == ListCmd.LIST
                && !a.content && !a.filesOf && to.picked && to.kind == ListCmd.LIST && !to.content && !to.filesOf;
        if (xfer) {
            final boolean move = cmd.equals("MOVE");
            final long from = sel.in, into = sel.to;
            final List<Long> ids = new ArrayList<Long>(sel.ids);
            s.describe = Words.DbCmd.inDb(Words.ListCmdW.copied(ids.size(), 0, null, Words.DbCmd.HERE_LIST));
            s.op = new ListCmd.Op() {
                @Override public String run(Library lib) {
                    Res r = ListRun.realRes(mode, new Work() {
                        @Override public Res run(Store t, Finder f) {
                            Store.Item li = into == 0 ? null : t.item(into);
                            if (li == null || li.type != Store.LIST) return Res.err(Words.Db.LIST_GONE);
                            Lists.Copied all = new Lists.Copied();
                            List<Long> entered = new ArrayList<Long>();
                            for (Long id : ids) {                               // אחד אחד — כדי לדעת מי נכנס (הלוח)
                                Lists.Copied one;
                                if (t.item(id) == null) {
                                    one = new Lists.Copied();
                                    one.skipped = 1;
                                    one.why = Words.DbClipW.GONE;
                                } else one = move ? Lists.moveTo(t, java.util.Collections.singletonList(id), from, into)
                                        : Lists.copyIds(t, java.util.Collections.singletonList(id), into);
                                if (one.done == 1) entered.add(id);
                                all.done += one.done;
                                all.skipped += one.skipped;
                                if (all.why == null) all.why = one.why;
                            }
                            Res ok = Res.ok(Words.ListCmdW.copied(all.done, all.skipped, all.why, li.name) + ListOps.nowIn(t, into));
                            ok.made = into;
                            ok.entered = entered;
                            return ok;
                        }
                    }, move, lib);                                              // העברה מוציאה מהמקור — גיבוי לפני
                    if (r.error != null) lastFailed = true;
                    s.made = r.made;
                    s.entered = r.entered;
                    return r.error != null ? r.error : Words.DbCmd.inDb(r.text);
                }
            };
            return;
        }
        if (add) {
            // אותו כלל כמו "העתק "<קובץ>" ידנית "<רשימה>"" כתובה (addFiles); מה שנובע מהמקום: הרשימה — לפי המספר. רק מוסיפה — בלי גיבוי לפני.
            final long into = sel.in;
            final List<String> ps = new ArrayList<String>(fsFiles);
            final List<String> ds = hasDirs ? new ArrayList<String>(fsDirs) : new ArrayList<String>();
            final int taken = ListOps.takenOf(sw);
            s.describe = Words.DbCmd.inDb((ds.isEmpty() ? "" : Words.folders(ds.size()) + " · ") + Words.files(ps.size()) + " — "
                    + Words.ListCmdW.copied(ps.size() + ds.size(), 0, null, Words.DbCmd.HERE_LIST));
            s.op = new ListCmd.Op() {
                @Override public String run(Library lib) {
                    Res r = ListRun.realRes(mode, new Work() {
                        @Override public Res run(Store t, Finder f) {
                            long dest = into != 0 ? into : DbPaths.home(t);
                            Store.Item li = dest == 0 ? null : t.item(dest);
                            if (li == null || li.type != Store.LIST) return Res.err(Words.Db.LIST_GONE);
                            if (!ds.isEmpty()) {                                // מבנה 115: התיקיות נסרקות כאן, בהרצה (המסך מריץ ברקע)
                                List<Tree.Node> nodes = new ArrayList<Tree.Node>();
                                if (!live) for (String d : ds) nodes.add(Tree.scanFolder(new File(d), !flat));
                                return ListOps.addFolders(t, dest, li.name, ds, nodes, ps, live, flat, taken);
                            }
                            Res ok = ListOps.addFiles(t, dest, ps, Words.files(ps.size()), li.name);
                            ok.made = dest;
                            return ok;
                        }
                    }, false, lib);
                    if (r.error != null) lastFailed = true;
                    s.made = r.made;
                    return r.error != null ? r.error : Words.DbCmd.inDb(r.text);
                }
            };
            return;
        }
        boolean remove = (cmd.equals("RD") || cmd.equals("DEL")) && p.sides.size() == 1 && sw.isEmpty()
                && a.kind == ListCmd.LIST && !a.content && !a.filesOf;
        if (!remove) { s.error = Words.Cmd.SELECTED_IN_LIST; return; }
        final long in = sel.in;
        final List<Long> ids = new ArrayList<Long>(sel.ids);
        final Work w = new Work() {
            @Override public Res run(Store t, Finder f) {
                for (Long id : ids) Lists.removeFrom(t, in, id);
                return Res.ok(Words.DbCmd.removed(Words.DbCmd.items(ids.size())));
            }
        };
        s.target = a.label();
        s.describe = Words.DbCmd.inDb(Words.DbCmd.removed(Words.DbCmd.items(ids.size())));
        s.op = new ListCmd.Op() {
            @Override public String run(Library lib) { return ListRun.real(mode, w, true, lib); }
        };
    }

    /** מבנה 104: "/תפוס:<ערך>" (Verbs.Cmd.taken) ← מה שהחומר מבין. "" ו"לשאול" — בקובץ אין את מי לשאול: לעצור. */
    static int taken(String v) {
        return Syntax.TAKEN_REPLACE.equals(v) ? Lists.TAKEN_REPLACE : Syntax.TAKEN_RENAME.equals(v) ? Lists.TAKEN_RENAME
                : Syntax.TAKEN_SKIP.equals(v) ? Lists.TAKEN_SKIP : Lists.TAKEN_STOP;
    }

    /**
     * מ0.25 (באג 2): המקור של COMP /רשימה:<שם> — מהמסד (עד עכשיו נקרא עץ הרשימות של המנוע הישן, והרשימה שבמסד לא נמצאה).
     * הרשימה: הדרך כמו שנכתבה ("א/ב"); ואם אין — בתוך "שמור לתוכנה". כל הקבצים שבה, בכל העומק, כולל מה שהרשימות החכמות
     * שבתוכה מוצאות עכשיו. null — שגיאה (ב־err).
     */
    static Compare.Source compSource(Sim sim, String name, String skip, String[] err) {
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
        DbPaths.Found at = DbPaths.listAt(db, mode, path);
        long id = at.error != null ? 0 : at.id;
        if (id == 0 && !path.isEmpty() && !at.ambiguous) {
            List<String> in = new ArrayList<String>();
            in.add(Reserved.ROOT);
            in.addAll(path);
            id = DbPaths.find(db, mode, in);
        }
        if (id == 0) { err[0] = at.ambiguous ? at.error : Words.ListCmdW.noList(name); return null; }
        Set<String> out = new LinkedHashSet<String>();
        if (!deep(db, id, finder(sim), out, new HashSet<Long>())) { err[0] = Words.ListCmdW.INDEX_NOT_READY; return null; }
        Compare.Source src = new Compare.Source();
        for (String p : out) Compare.addFile(sim.lib, src, new File(p), skip);
        return src;
    }

    // ---------- מ0.14: ודא ----------

    /** מ0.37: "ודא "<תיקייה>" /קבצים:N" · "ודא "<קובץ>" מכיל "<טקסט>"". */
    static final String SW_FILES_IN = "/קבצים:", W_CONTAINS = "מכיל";   // תחביר
    /** מ0.42: "ודא "<קובץ>" באינדקס" — מה שהיה "בדיקת מסלול באינדקס" בהגדרות. */
    static final String W_IN_INDEX = "באינדקס";   // תחביר
    /** מבנה 87–88: ודא ידנית "א" זהה ידנית "ב" (אותה רשימה) · ודא ידנית "א" שווה ידנית "ב" (רשימה אחרת, שווה). */
    static final String W_SAME = "זהה", W_EQUAL = "שווה";   // תחביר

    /** העותק בזיכרון — פעם אחת לכל קובץ פקודות (השורות הבאות רואות מה שהקודמות עשו). */
    static Store sim(Sim sim, String mode) {
        if (sim.db != null) return sim.db;
        SqlStore real = Db.openRead(Db.app, mode);
        try {
            sim.db = copyOf(real);
        } finally {
            real.close();
        }
        return sim.db;
    }

    static Finder finder(final Sim sim) {
        return ListRun.over(sim.lib, sim.condFiles);
    }

    /** מ0.31: הפעולה האחרונה במסד נכשלה — ההרצה סופרת כישלונות ("המשך"). */
    static volatile boolean lastFailed;

    /**
     * מ0.25: תיקיית היעד של קובץ כשמבקשים /מסלול מרשימה — היעד + המסלול המלא של התיקייה שלו בכונן (בלי שורש הכונן):
     * /storage/emulated/0/Download/א/ב.pdf ← <יעד>/Download/א. כרטיס: /storage/XXXX-XXXX/… — אותו דבר. מסלול אחר — כולו.
     */
    /**
     * הקבצים שברשימה (ידנית, חכמה, "קבצי …") — אל תיקייה בכונן: העתקה, ומ0.40 גם העברה (המשתמש, על התוספים: "המריץ מעביר את
     * כולם, ואם צריך משנה שם"). הביצוע — הרגיל של הכונן (Batch.run); כאן רק התכנון, קובץ אחרי קובץ על ההדמיה.
     */
    private static void toFolder(Batch.Step s, String cmd, ListCmd.Side a, Store db, String mode, Finder find, List<String> sw,
                                 Sim sim, File fsDest) {
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
            if (u.equals("/Y") || x.equals(Syntax.SW_TAKEN + Syntax.TAKEN_REPLACE)) s.replace = true;
            if (x.equals(Syntax.SW_TAKEN + Syntax.TAKEN_SKIP)) s.skipTaken = true;
            if (x.equals(Syntax.SW_TAKEN + Syntax.TAKEN_STOP)) s.failTaken = true;
            if (x.equals(Syntax.SW_TAKEN + Syntax.TAKEN_RENAME)) s.renameTaken = true;
            if (x.startsWith(Syntax.SW_TAKEN) && !Parse.takenValue(x)) { s.error = Parse.takenUnknown(x); return; }
            if (u.equals("/S") || x.equals(Syntax.SW_KEEP)) s.recursive = true;
        }
        Filter flt = Filter.of(sw);                                   // מ0.40: /מכיל:
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
