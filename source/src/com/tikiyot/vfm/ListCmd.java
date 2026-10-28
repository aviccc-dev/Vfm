package com.tikiyot.vfm;

// 1.24.0: פקודות על רשימות (claude/pkudot-reshimot.md). המשתמש: "אם פרמטר — מספיק העתק רגיל; אם בסתם — זה קובץ; אם
// העתק רשימה — אז זו רשימה"; "העתק רשימה א לתיקייה ב — כך שיעתיק מהרשימה את הקבצים לתיקייה"; "לפצל מההתחלה
// לרשימות ידניות וחכמות"; "קבצים לא יכולים להיכנס לרשימה — רק השמות והמסלולים שלהם".
// מילת סוג לפני כל צד: ידנית · חכמה · תוכן ידנית · קבצי ידנית · קבצי חכמה · תיקייה (בלי מילה — קבצים ותיקיות).
// "*" — הבית של האופן. הפקודות פונות לרשימה בשמה ובמקומה במבנה — לא לאחסון שלה (כך גם במסד נתונים בעתיד).
// שלב 1: MD, COPY, RD, DEL. ההדבקה וההסרה — אותו מנוע של הלוח (VClip); ההדמיה — על עותק של העץ בזיכרון.
// אחראי: הקריאה, התכנון וההרצה של שורה עם מילות סוג (בלי ממשק — נבדק).

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class ListCmd {

    static final String MANUAL = "ידנית", SMART = "חכמה", CONTENT = "תוכן", FILES_OF = "קבצי", FOLDER = "תיקייה";   // תחביר
    static final String HOME = "*";
    /** 1.25.0: COMP של שתי רשימות חכמות — גם התנאים (המשתמש: "אולי פרמטר שיוסיף את השוואת התנאים"). */
    static final String SW_CONDS = "/תנאים";   // תחביר

    static final int FS = 0, LIST = 1, SMART_LIST = 2;

    /** צד אחד בשורה: מה הוא, והערך שלו. */
    static final class Side {
        int kind = FS;
        boolean content, filesOf;
        String raw;
        /** ברשימה: הדרך (ריקה — הבית); mask — תבנית קבצים בסוף הדרך ("א\*.pdf"), או null. */
        List<String> path;
        boolean home;
        String mask;

        boolean list() {
            return kind != FS || content || filesOf;
        }

        String label() {
            return home ? Words.ListCmdW.HOME : "\"" + raw + "\"";
        }
    }

    /** חלק בשורה — עם סימן אם היה במרכאות (מילה במרכאות היא שם, לא מילת סוג). */
    static final class Tok {
        final String text;
        final boolean quoted;

        Tok(String text, boolean quoted) {
            this.text = text;
            this.quoted = quoted;
        }
    }

    /** הצדדים של שורה, או null — אין בה מילת סוג (שורה רגילה). */
    static final class Parsed {
        final List<Side> sides = new ArrayList<Side>();
        String error;

        boolean anyList() {
            for (Side s : sides) if (s.list()) return true;
            return false;
        }
    }

    /** הפעולה שתתבצע בהרצה (רשימות בלבד; קבצים בכונן — Batch כרגיל). */
    interface Op {
        String run(Library lib);
    }

    private ListCmd() { }

    // ---------- קריאה ----------

    static List<Tok> tokens(String line) {
        List<Tok> out = new ArrayList<Tok>();
        StringBuilder cur = new StringBuilder();
        boolean q = false, any = false, wasQ = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                q = !q;
                any = true;
                wasQ = true;
            } else if (!q && Character.isWhitespace(c)) {
                if (any || cur.length() > 0) out.add(new Tok(cur.toString(), wasQ));
                cur.setLength(0);
                any = false;
                wasQ = false;
            } else cur.append(c);
        }
        if (any || cur.length() > 0) out.add(new Tok(cur.toString(), wasQ));
        return out;
    }

    private static boolean word(Tok t) {
        return !t.quoted && (t.text.equals(MANUAL) || t.text.equals(SMART) || t.text.equals(CONTENT)
                || t.text.equals(FILES_OF) || t.text.equals(FOLDER));
    }

    /** null — אין בשורה מילת סוג. switches — נאספים בנפרד (כמו ב-Batch). */
    static Parsed parse(String line, List<String> switches) {
        List<Tok> tk = tokens(line);
        boolean typed = false;
        for (int i = 1; i < tk.size(); i++) typed |= word(tk.get(i));
        if (!typed) return null;
        Parsed p = new Parsed();
        Side cur = null;
        for (int i = 1; i < tk.size(); i++) {
            Tok t = tk.get(i);
            if (!t.quoted && (Batch.isSwitch(t.text) || t.text.equals(SW_CONDS))) {
                switches.add(t.text);
                continue;
            }
            if (cur == null) cur = new Side();
            if (word(t)) {
                if (t.text.equals(CONTENT)) cur.content = true;
                else if (t.text.equals(FILES_OF)) cur.filesOf = true;
                else if (t.text.equals(MANUAL)) cur.kind = LIST;
                else if (t.text.equals(SMART)) cur.kind = SMART_LIST;
                else cur.kind = FS;
                if (t.text.equals(FOLDER) && (cur.content || cur.filesOf)) p.error = Words.ListCmdW.USAGE;
                continue;
            }
            cur.raw = t.text;
            if ((cur.content || cur.filesOf) && cur.kind == FS) p.error = Words.ListCmdW.USAGE;   // "תוכן" בלי "ידנית"
            if (cur.content && cur.kind == SMART_LIST) p.error = Words.ListCmdW.CONTENT_SMART;
            p.sides.add(cur);
            cur = null;
        }
        if (cur != null) p.error = Words.ListCmdW.USAGE;                     // מילת סוג בלי שם אחריה
        return p;
    }

    /** הדרך לרשימה: "א\ב" או "א/ב"; "*" — הבית; משתנים (%…%) מוחלפים. */
    static void resolve(Side s, Map<String, String> vars) {
        String raw = s.raw;
        if (vars != null) for (Map.Entry<String, String> e : vars.entrySet()) raw = raw.replace(Batch.var(e.getKey()), e.getValue());
        s.raw = raw;
        s.path = new ArrayList<String>();
        if (raw.trim().equals(HOME)) {
            s.home = true;
            return;
        }
        for (String part : raw.replace('\\', '/').split("/")) if (!part.trim().isEmpty()) s.path.add(part.trim());
        if (!s.path.isEmpty()) {
            String last = s.path.get(s.path.size() - 1);
            if (last.contains("*") || last.contains("?")) {
                s.mask = last;
                s.path.remove(s.path.size() - 1);
            }
        }
        if (s.path.isEmpty() && s.mask == null) s.home = true;
    }

    /** 1.25.2: כל הרשימות החכמות — בשם שהפקודות מכירות ("רשימה\\חכמה"; כלי בבית — בשמו), לבוחר בעורך. */
    static List<String> smartNames() {
        List<String> out = new ArrayList<String>();
        for (Structure.Func f : Structure.load().direct)
            if (FnSmart.KIND.equals(f.kind) || FnDup.KIND.equals(f.kind)) out.add(f.name);
        smartNames(Tree.load().root, "", out);
        return out;
    }

    private static void smartNames(Tree.Node n, String prefix, List<String> out) {
        for (String r : n.smart) {
            Nested.Item it = Nested.resolve(r);
            if (!it.gone && it.name != null && !it.name.isEmpty()) out.add(prefix + it.name);
        }
        for (Tree.Node c : n.children) smartNames(c, prefix + c.name + "\\", out);
    }

    /** "א\ב" — כמו שהפקודות כותבות דרך לרשימה. */
    static String pathText(List<String> path) {
        StringBuilder sb = new StringBuilder();
        for (String p : path) sb.append(sb.length() > 0 ? "\\" : "").append(p);
        return sb.toString();
    }

    // ---------- מציאה בעץ ----------

    /** רשימה חכמה: ההפניה שלה ברשימה (from = הרשימה שבה), או כלי בבית (tool). */
    static final class SmartRef {
        List<String> from;
        String ref, name;
        Structure.Func tool;
    }

    static SmartRef smart(Tree t, Side s) {
        if (s.path.isEmpty()) return null;
        List<String> parent = new ArrayList<String>(s.path.subList(0, s.path.size() - 1));
        String name = s.path.get(s.path.size() - 1);
        Tree.Node n = t.find(parent);
        if (n != null)
            for (String r : n.smart) {
                Nested.Item it = Nested.resolve(r);
                if (!it.gone && name.equals(it.name)) {
                    SmartRef x = new SmartRef();
                    x.from = parent;
                    x.ref = r;
                    x.name = it.name;
                    return x;
                }
            }
        if (parent.isEmpty())
            for (Structure.Func f : Structure.load().direct)
                if ((FnSmart.KIND.equals(f.kind) || FnDup.KIND.equals(f.kind)) && f.name.equals(name)) {
                    SmartRef x = new SmartRef();
                    x.tool = f;
                    x.name = f.name;
                    return x;
                }
        return null;
    }

    /** מקור להדבקה: מאיזו רשימה (null — מהבית), ומה. */
    static final class Src {
        List<String> from;
        final List<VClip.Item> items = new ArrayList<VClip.Item>();
        String error;
        String what;
        /** הסרה של רשימה חכמה שהיא כלי בבית. */
        Structure.Func tool;
    }

    /** כל מה שברשימה, כפריטי לוח. */
    private static void contentOf(Tree.Node n, Src out) {
        for (Tree.Node c : n.children) out.items.add(new VClip.Item(VClip.LIST, c.name, c.name));
        for (String r : n.smart) out.items.add(new VClip.Item(VClip.SMART, r, Nested.resolve(r).name));
        for (String r : n.refs) out.items.add(new VClip.Item(VClip.REF, r, r));
        for (String l : n.live) out.items.add(new VClip.Item(VClip.LIVE, l, new File(l).getName()));
        for (Tree.Entry e : n.entries) out.items.add(new VClip.Item(VClip.FILE, e.path, new File(e.path).getName()));
    }

    /** הקבצים שהצד מצביע עליהם (ידנית — בכל העומק; חכמה — מה שהיא מוצאת עכשיו). */
    static List<String> files(Tree t, Side s, Library lib, String[] err) {
        return files(t, s, lib, err, new Lenient(lib));
    }

    /** בתכנון — עם הזיכרון של הקובץ כולו (sim.files). */
    static List<String> files(Tree t, Side s, Batch.Sim sim, String[] err) {
        if (sim.files == null) sim.files = new Lenient(sim.lib);
        return files(t, s, sim.lib, err, sim.files);
    }

    static List<String> files(Tree t, Side s, Library lib, String[] err, Lenient len) {
        List<String> out = new ArrayList<String>();
        Map<String, Long> m;
        if (s.kind == SMART_LIST) {
            SmartRef r = smart(t, s);
            if (r == null) {
                err[0] = Words.ListCmdW.noSmart(s.raw);
                return null;
            }
            if (lib == null || !lib.ready) {
                err[0] = Words.ListCmdW.INDEX_NOT_READY;
                return null;
            }
            String cond = r.tool != null ? SmartLists.resolve(r.tool.target())[0] : Nested.resolve(r.ref).cond;
            for (Library.Hit h : lib.search(cond, false, true).hits) if (h.rec != null) out.add(h.path());
            return out;
        }
        if (s.home) {
            err[0] = Words.ListCmdW.HOME_FILES;
            return null;
        }
        Tree.Node n = t.find(s.path);
        if (n == null) {
            err[0] = Words.ListCmdW.noList(s.raw);
            return null;
        }
        if (s.mask != null) {
            for (Tree.Entry e : n.entries) if (!e.bad && Batch.wild(s.mask, new File(e.path).getName())) out.add(e.path);
            return out;
        }
        len.dups = 0;
        m = ListCount.filesOf(t, n, len);
        if (m == null) {
            err[0] = Words.ListCmdW.INDEX_NOT_READY;
            return null;
        }
        if (len.dups > 0 && err.length > 1) err[1] = Words.ListCmdW.dupSkipped(len.dups);
        out.addAll(m.keySet());
        java.util.Collections.sort(out);
        return out;
    }

    /**
     * 1.25.0: הקבצים של רשימה לפקודות — חכמה שנמחקה: אין קבצים (לא "לא ידוע"); רשימת כפולים: לא נספרת (הקבצים שלה הם
     * כפילויות של קבצים שבחכמות אחרות; לדעת אותם — רק בפתיחה), ונאמר כמה כאלה דולגו. האינדקס לא מוכן — לא ידוע.
     */
    static final class Lenient implements ListCount.Index {
        private final ListCount.FromLibrary in;
        private final Library lib;
        int dups;

        Lenient(Library lib) {
            this.lib = lib;
            in = new ListCount.FromLibrary(lib);
        }

        @Override public long[] smart(String ref) { return in.smart(ref); }

        @Override public long[] live(String folder) { return in.live(folder); }

        @Override
        public Map<String, Long> smartFiles(String ref) {
            if (lib == null || !lib.ready) return null;
            Nested.Item it = Nested.resolve(ref);
            if (it.gone) return new java.util.HashMap<String, Long>();
            if (it.dup != null) {
                dups++;
                return new java.util.HashMap<String, Long>();
            }
            return in.smartFiles(ref);                                         // לפי התנאי — FromLibrary (1.25.4)
        }

        @Override public Map<String, Long> liveFiles(String folder) { return in.liveFiles(folder); }
    }

    /** מקור להעתקה לרשימה, מתוך העץ t (בתכנון — העותק בזיכרון; בהרצה — העץ האמיתי). */
    static Src source(Tree t, Side s, List<String> fsFiles, Library lib) {
        Src out = new Src();
        if (!s.list()) {                                                     // קבצים מהכונן ← מסלולים
            for (String f : fsFiles) out.items.add(new VClip.Item(VClip.FILE, f, new File(f).getName()));
            out.what = Words.files(out.items.size());
            return out;
        }
        if (s.filesOf) {
            String[] err = {null};
            List<String> fs = files(t, s, lib, err);
            if (fs == null) {
                out.error = err[0];
                return out;
            }
            for (String f : fs) out.items.add(new VClip.Item(VClip.FILE, f, new File(f).getName()));
            out.what = Words.ListCmdW.filesOf(Words.files(fs.size()), s.label());
            return out;
        }
        if (s.kind == SMART_LIST) {
            SmartRef r = smart(t, s);
            if (r == null) {
                out.error = Words.ListCmdW.noSmart(s.raw);
                return out;
            }
            if (r.tool != null) out.items.add(new VClip.Item(VClip.TOOL, r.tool.id, r.tool.name));
            else {
                out.from = r.from;
                out.items.add(new VClip.Item(VClip.SMART, r.ref, r.name));
            }
            out.what = Words.ListCmdW.smartList(s.label());
            return out;
        }
        if (s.home) {                                                        // "*" — הבית: רק התוכן
            if (!s.content) {
                out.error = Words.ListCmdW.HOME_ONLY_CONTENT;
                return out;
            }
            for (Structure.Func f : Structure.load().direct) out.items.add(new VClip.Item(VClip.TOOL, f.id, f.name));
            out.what = Words.ListCmdW.homeContent(out.items.size());
            return out;
        }
        Tree.Node n = t.find(s.path);
        if (n == null) {
            out.error = Words.ListCmdW.noList(s.raw);
            return out;
        }
        if (s.mask != null) {                                                // "א\*.pdf" — הקבצים ברשימה שמתאימים
            out.from = s.path;
            for (Tree.Entry e : n.entries)
                if (Batch.wild(s.mask, new File(e.path).getName())) out.items.add(new VClip.Item(VClip.FILE, e.path, new File(e.path).getName()));
            out.what = Words.files(out.items.size());
            return out;
        }
        if (s.content) {
            out.from = s.path;
            contentOf(n, out);
            out.what = Words.ListCmdW.contentOf(out.items.size(), s.label());
            return out;
        }
        out.from = new ArrayList<String>(s.path.subList(0, s.path.size() - 1));
        out.items.add(new VClip.Item(VClip.LIST, n.name, n.name));
        out.what = Words.ListCmdW.list(s.label());
        return out;
    }

    // ---------- תכנון ----------

    /** העץ בזיכרון לתכנון — נטען פעם אחת לכל קובץ פקודות, ולא נשמר. */
    static Tree simTree(Batch.Sim sim) {
        if (sim.tree == null) sim.tree = Tree.load();
        return sim.tree;
    }

    /**
     * שורה עם מילות סוג. fsFiles — הקבצים של צד המקור שבכונן; fsDest — יעד בכונן (Batch מוצא אותם). התוצאה: s.op
     * (רשימות), או קבצים ב-s.files ו-s.dest לביצוע הרגיל של Batch.
     */
    static void plan(final Batch.Step s, final String cmd, final Parsed p, List<String> sw, final Batch.Sim sim,
                     final List<String> fsFiles, final File fsDest) {
        for (String x : sw)
            if (x.startsWith("/" + Batch.SW_DATE) || x.startsWith("/" + Batch.SW_SIZE)) {
                s.error = Words.ListCmdW.NO_FILTER;
                return;
            }
        final Tree t = simTree(sim);
        final List<Side> sd = p.sides;
        if (cmd.equals("MD")) {
            if (sd.size() != 1) { s.error = Words.Cmd.usage(cmd); return; }
            final Side a = sd.get(0);
            if (a.kind == SMART_LIST) { s.error = Words.ListCmdW.MD_SMART; return; }
            if (a.kind != LIST || a.content || a.filesOf || a.home || a.mask != null) { s.error = Words.ListCmdW.USAGE_MD; return; }
            if (t.find(a.path) != null) {
                s.noop = true;
                s.describe = Words.ListCmdW.exists(a.label());
                return;
            }
            Tree.Node n = t.root;
            for (String part : a.path) {
                Tree.Node c = n.child(Tree.cleanName(part));
                n = c != null ? c : Tree.newChild(n, part);
            }
            s.describe = Words.ListCmdW.created(a.label());
            s.op = new Op() {
                @Override public String run(Library lib) {
                    Verbs.ensureList(a.path);
                    return Words.ListCmdW.created(a.label());
                }
            };
            return;
        }
        if (cmd.equals("RD") || cmd.equals("DEL")) {
            if (sd.size() != 1) { s.error = Words.Cmd.usage(cmd); return; }
            final Side a = sd.get(0);
            if (a.filesOf) {                                                 // הקבצים עצמם — לסל (הביצוע הרגיל)
                if (cmd.equals("RD")) { s.error = Words.ListCmdW.RD_FILES; return; }
                String[] err = {null, null};
                List<String> fs = files(t, a, sim, err);
                if (fs == null) { s.error = err[0]; return; }
                for (String f : fs) if (sim.isFile(new File(f))) s.files.add(new File(f).getAbsolutePath());
                s.describe = Words.Cmd.toTrash(Words.files(s.files.size())) + Words.ListCmdW.FROM_DISK + (err[1] != null ? err[1] : "");
                for (String f : s.files) {
                    sim.gone.add(f);
                    sim.added.remove(f);
                }
                return;
            }
            if (a.home) { s.error = Words.ListCmdW.HOME_NO_REMOVE; return; }
            final Src src = removal(t, a);
            if (src.error != null) { s.error = src.error; return; }
            s.describe = Words.ListCmdW.removes(src.what);
            if (src.tool == null) VClip.simulateRemove(t, src.from, src.items);
            s.op = new Op() {
                @Override public String run(Library lib) {
                    Src now = removal(Tree.load(), a);
                    if (now.error != null) return now.error;
                    if (now.tool != null) ToolMenu.removeNow(java.util.Collections.singletonList(now.tool));
                    else VClip.remove(now.from, now.items);
                    return Words.ListCmdW.removed(now.what);
                }
            };
            return;
        }
        if (cmd.equals("COPY")) {
            if (sd.size() != 2) { s.error = Words.ListCmdW.USAGE; return; }
            final Side a = sd.get(0), b = sd.get(1);
            if (b.content || b.filesOf) { s.error = Words.ListCmdW.USAGE; return; }
            if (b.kind == SMART_LIST) { s.error = Words.ListCmdW.INTO_SMART; return; }
            if (b.kind == FS) {                                             // מרשימה לתיקייה — הקבצים עצמם (הביצוע הרגיל)
                if (fsDest == null || !sim.isDir(fsDest)) { s.error = Words.Cmd.NO_DEST; return; }
                String[] err = {null, null};
                List<String> fs = files(t, a, sim, err);
                if (fs == null) { s.error = err[0]; return; }
                s.dest = fsDest;
                for (String f : fs) if (sim.isFile(new File(f))) s.files.add(new File(f).getAbsolutePath());
                s.describe = Words.Cmd.transfer(true, Words.files(s.files.size()), Bidi.path(fsDest.getAbsolutePath())) + (err[1] != null ? err[1] : "");
                for (String f : s.files) sim.move(f, new File(fsDest, new File(f).getName()), true);
                return;
            }
            if (!b.home && (b.mask != null || t.find(b.path) == null)) { s.error = Words.ListCmdW.noList(b.raw); return; }
            final boolean full = a.list() && !a.filesOf;                     // רשימות — עותק עצמאי; קבצים — מסלולים
            Src src = source(t, a, fsFiles, sim.lib);
            if (src.error != null) { s.error = src.error; return; }
            if (!b.home && a.kind == LIST && !a.content && !a.filesOf && a.mask == null) {
                Tree.Node m = t.find(a.path), dn = t.find(b.path);
                if (m != null && (m == dn || m.isAncestorOf(dn))) { s.error = Words.ListCmdW.INTO_ITSELF; return; }
            }
            s.describe = Words.ListCmdW.copy(src.what, b.label());
            VClip.simulate(t, src.from, src.items, b.home ? null : b.path, full);
            s.op = new Op() {
                @Override public String run(Library lib) {
                    Src now = source(Tree.load(), a, fsFiles, lib);
                    if (now.error != null) return now.error;
                    VClip.Result r = VClip.apply(now.from, now.items, full, b.home ? null : b.path);
                    String size = "";
                    if (!b.home) {                                           // 1.25.0: כמה יש ביעד עכשיו (לגידול בסבבים)
                        Tree t2 = Tree.load();
                        ListCount c = ListCount.of(t2, t2.find(b.path), null);
                        size = Words.ListCmdW.nowIn(c.lists, c.items);
                    }
                    return Words.ListCmdW.copied(r.done, r.skipped, r.why, b.label()) + size;
                }
            };
            return;
        }
        if (cmd.equals("COMP")) {                                            // 1.25.0: השוואת רשימות — לפי המסלולים
            if (sd.size() != 2) { s.error = Words.ListCmdW.USAGE_COMP; return; }
            Side a = sd.get(0), b = sd.get(1);
            if (!a.list() || !b.list()) { s.error = Words.ListCmdW.COMP_FS; return; }
            if (a.content || b.content) { s.error = Words.ListCmdW.USAGE_COMP; return; }
            boolean conds = sw.contains(SW_CONDS);
            if (conds && (a.kind != SMART_LIST || b.kind != SMART_LIST || a.filesOf || b.filesOf)) { s.error = Words.ListCmdW.CONDS_SMART; return; }
            String[] err = {null, null};
            List<String> fa = files(t, a, sim, err), fb = fa == null ? null : files(t, b, sim, err);
            if (fa == null || fb == null) { s.error = err[0]; return; }
            String note = err[1] != null ? err[1] : "";
            java.util.Set<String> sa = new java.util.LinkedHashSet<String>(fa), sb = new java.util.LinkedHashSet<String>(fb);
            List<String> onlyA = new ArrayList<String>(), onlyB = new ArrayList<String>();
            int both = 0;
            for (String x : sa) if (sb.contains(x)) both++; else onlyA.add(x);
            for (String x : sb) if (!sa.contains(x)) onlyB.add(x);
            String text = Words.ListCmdW.compared(a.label(), onlyA.size(), b.label(), onlyB.size(), both) + note + examples(onlyA) + examples(onlyB);
            if (conds) text += "\n" + condsDiff(smartCond(t, a), smartCond(t, b), a.label(), b.label());
            s.describe = text;
            return;
        }
        if (cmd.equals("MOVE")) {
            boolean toList = sd.size() == 2 && sd.get(1).list();
            s.error = toList && !sd.get(0).list() ? Words.ListCmdW.MOVE_TO_LIST : Words.ListCmdW.MOVE_LATER;
            return;
        }
        s.error = Words.ListCmdW.notYet(cmd);
    }

    private static String examples(List<String> paths) {
        if (paths.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("\n   ");
        for (int i = 0; i < Math.min(3, paths.size()); i++) sb.append(i > 0 ? " · " : "").append(new File(paths.get(i)).getName());
        if (paths.size() > 3) sb.append(" · ").append(Words.andMore(paths.size() - 3));
        return sb.toString();
    }

    private static String smartCond(Tree t, Side s) {
        SmartRef r = smart(t, s);
        if (r == null) return "";
        return r.tool != null ? SmartLists.resolve(r.tool.target())[0] : Nested.resolve(r.ref).cond;
    }

    /** התנאים — מילה מול מילה: מה רק באחת, מה רק בשנייה. */
    static String condsDiff(String ca, String cb, String la, String lb) {
        List<String> a = Batch.tokens(ca == null ? "" : ca), b = Batch.tokens(cb == null ? "" : cb);
        List<String> onlyA = new ArrayList<String>(), onlyB = new ArrayList<String>();
        for (String x : a) if (!b.contains(x)) onlyA.add(x);
        for (String x : b) if (!a.contains(x)) onlyB.add(x);
        if (onlyA.isEmpty() && onlyB.isEmpty()) return Words.ListCmdW.SAME_CONDS;
        return Words.ListCmdW.conds(la, join(onlyA), lb, join(onlyB));
    }

    private static String join(List<String> xs) {
        StringBuilder sb = new StringBuilder();
        for (String x : xs) sb.append(sb.length() > 0 ? " " : "").append(x);
        return sb.length() == 0 ? "—" : sb.toString();
    }

    /** מה יוצא בהסרה: רשימה, חכמה (או כלי בבית), תוכן, או קבצים לפי תבנית. */
    private static Src removal(Tree t, Side a) {
        Src out = new Src();
        if (a.kind == SMART_LIST) {
            SmartRef r = smart(t, a);
            if (r == null) {
                out.error = Words.ListCmdW.noSmart(a.raw);
                return out;
            }
            if (r.tool != null) out.tool = r.tool;
            else {
                out.from = r.from;
                out.items.add(new VClip.Item(VClip.SMART, r.ref, r.name));
            }
            out.what = Words.ListCmdW.smartList(a.label());
            return out;
        }
        Tree.Node n = t.find(a.path);
        if (n == null) {
            out.error = Words.ListCmdW.noList(a.raw);
            return out;
        }
        if (a.mask != null || a.content) {
            out.from = a.path;
            if (a.content) contentOf(n, out);
            else for (Tree.Entry e : n.entries)
                if (Batch.wild(a.mask, new File(e.path).getName())) out.items.add(new VClip.Item(VClip.FILE, e.path, new File(e.path).getName()));
            out.what = a.content ? Words.ListCmdW.contentOf(out.items.size(), a.label()) : Words.files(out.items.size());
            return out;
        }
        out.from = new ArrayList<String>(a.path.subList(0, a.path.size() - 1));
        out.items.add(new VClip.Item(VClip.LIST, n.name, n.name));
        out.what = Words.ListCmdW.list(a.label());
        return out;
    }
}
