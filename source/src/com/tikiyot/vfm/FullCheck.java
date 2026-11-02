package com.tikiyot.vfm;

// בדיקה מלאה אחת (מ0.2.2): מריצה הכול ומחזירה דוח טקסט אחד, שאפשר להעתיק ולשלוח.
// אחראי: לבדוק את שכבת האחסון (בדיקה עצמית), ולכל אופן עבודה במנוע הישן — העתקה מלאה למסד זמני ובדיקה מול המקור
// (רשימות, חברות, קבצים, חתימות, רשימות חכמות ותנאיהן, "לא משויכים", העתקה חוזרת, רשימות ריקות), הערכת התנאים מול האינדקס
// (מ0.3), ואת המסד האמיתי של אותו אופן מול המקור (בלי לשנות אותו).
// אסור לו: לשנות את המנוע הישן, או את המסדים האמיתיים (רק תצוגה מקדימה עליהם).

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class FullCheck {

    private FullCheck() { }

    /** מה שהסביבה נותנת: מידע על המכשיר, בדיקה עצמית, ומסדים. */
    interface Env {
        String header();

        /** תוצאת הבדיקה העצמית: [0] — הטקסט; השורה הראשונה מתחילה ב־"✅" או "❌". */
        String selfTest();

        boolean selfTestOk();

        /** מסד זמני חדש וריק (נמחק אחרי הבדיקה). */
        Store openTemp() throws Exception;

        /** המסד האמיתי של האופן, או null אם עוד אין. לא יוצרים אותו. */
        Store openReal(String mode) throws Exception;

        /** מנוע ההערכה של רשימות חכמות (האינדקס); null — אין (למשל בבדיקות במחשב בלי אינדקס). */
        Smarts.Finder finder();
    }

    private static final int EXAMPLES = 6;

    /** הדוח כולו. לא זורק — כל כישלון נכנס לדוח כבעיה. */
    static String run(File oldData, Env env) {
        List<String> problems = new ArrayList<String>();
        StringBuilder body = new StringBuilder();
        long t0 = System.currentTimeMillis();
        body.append(env.header()).append("\n\n");
        body.append(env.selfTest()).append("\n");
        if (!env.selfTestOk()) problems.add(Words.Db.pSelfTest());
        List<String> modes = Importer.modes(oldData);
        if (modes.isEmpty()) {
            body.append("\n").append(Words.Db.IMPORT_NO_OLD).append("\n");
        }
        for (String mode : modes) {
            body.append("\n");
            try {
                mode(oldData, mode, env, body, problems);
            } catch (Throwable t) {
                Log.e("FullCheck.mode", t);
                problems.add(Words.Db.pCrash(mode, String.valueOf(t)));
                body.append(Words.Db.pCrash(mode, String.valueOf(t))).append("\n");
            }
        }
        StringBuilder out = new StringBuilder();
        out.append(problems.isEmpty() ? Words.Db.VERDICT_OK : Words.Db.verdictBad(problems.size())).append("\n");
        for (String p : problems) out.append("• ").append(p).append("\n");
        out.append("\n").append(body);
        out.append("\n").append(Words.Db.checkTotal(System.currentTimeMillis() - t0));
        return out.toString();
    }

    // ---------- אופן אחד ----------

    private static void mode(File oldData, String mode, Env env, StringBuilder out, List<String> problems) throws Exception {
        long t0 = System.currentTimeMillis();
        Importer.Source src = Importer.load(new File(new File(oldData, Modes.DIR), mode));
        List<Tree.Node> nodes = new ArrayList<Tree.Node>();
        collect(src.tree.root, nodes);
        Set<String> paths = new HashSet<String>();
        int bad = 0, refs = 0, live = 0;
        for (Tree.Node n : nodes) {
            bad += count(n, paths);
            refs += n.refs.size();
            live += n.live.size();
        }
        List<Importer.Smart> defs = src.wanted();
        int nested = 0;
        for (Importer.Smart d : defs) nested += d.nested;
        out.append(Words.Db.modeHead(mode)).append("\n");
        out.append(Words.Db.modeSource(nodes.size() - 1, paths.size(), defs.size(), refs, src.skippedSmart(), nested, live, bad)).append("\n");

        // 1. העתקה מלאה למסד זמני ובדיקה מול המקור
        Store t = env.openTemp();
        try {
            Importer.Report r = Importer.run(t, src, true);
            Found f = verify(t, src, nodes, paths, defs, true);
            Importer.Report again = Importer.run(t, src, false);
            long ms = System.currentTimeMillis() - t0;
            out.append(Words.Db.modeTemp(t.lists().size(), r.filesNew, f.lists, f.membersWrong, f.filesMissing, f.sigWrong,
                    again.nothingToDo(), t.check() == null, ms)).append("\n");
            add(problems, mode, f, nodes.size() - 1, t.lists().size(), again.nothingToDo(), t.check());
            int[] un = unassigned(t, nodes);
            out.append(Words.Db.modeTempSmart(defs.size(), f.smartFound, f.smartWrong, un[0], un[1])).append("\n");
            if (un[0] != un[1] || un[2] != 0) problems.add(Words.Db.pUnassigned(mode, un[0], un[1] + un[2]));
            if (f.smartMissing > 0) problems.add(Words.Db.pSmartMissing(mode, f.smartMissing));
            if (f.smartWrong > 0) problems.add(Words.Db.pSmartWrong(mode, f.smartWrong));
            evaluate(out, problems, mode, defs, env.finder());
            empties(out, src, nodes, t);
        } finally {
            t.close();
        }

        // 2. המסד האמיתי של האופן, מול המקור — בלי לכתוב אליו
        Store real = env.openReal(mode);
        if (real == null) {
            out.append(Words.Db.REAL_NONE).append("\n");
            return;
        }
        try {
            Found f = verify(real, src, nodes, paths, defs, false);
            Importer.Report pre = Importer.run(real, src, false);
            out.append(Words.Db.modeReal(real.lists().size(), nodes.size() - 1, f.lists, f.membersWrong, f.filesMissing,
                    pre.listsNew, pre.filesNew, pre.filesUpdated, pre.linksAdded, pre.smartNew, real.check() == null)).append("\n");
            out.append(Words.Db.modeRealUnassigned(real.unassigned().size())).append("\n");
            if (pre.listsNew + pre.filesNew + pre.filesUpdated + pre.linksAdded + pre.smartNew > 0 || f.listsMissing > 0
                    || f.filesMissing > 0 || f.smartMissing > 0)
                problems.add(Words.Db.pRealBehind(mode, pre.listsNew, pre.filesNew, pre.filesUpdated, pre.linksAdded, pre.smartNew));
            if (real.check() != null) problems.add(Words.Db.pCheck(mode, real.check()));
            for (String e : f.examples) out.append("   ").append(e).append("\n");
        } finally {
            real.close();
        }
    }

    private static void add(List<String> problems, String mode, Found f, int expectedLists, int gotLists, boolean again, String check) {
        if (gotLists != expectedLists) problems.add(Words.Db.pListCount(mode, expectedLists, gotLists));
        if (f.membersWrong > 0) problems.add(Words.Db.pMembers(mode, f.membersWrong));
        if (f.filesMissing > 0) problems.add(Words.Db.pFiles(mode, f.filesMissing));
        if (f.sigWrong > 0) problems.add(Words.Db.pSig(mode, f.sigWrong));
        if (!again) problems.add(Words.Db.pAgain(mode));
        if (check != null) problems.add(Words.Db.pCheck(mode, check));
    }

    // ---------- המקור ----------

    /** כל הרשימות בעץ, כולל השורש (הראשון). */
    private static void collect(Tree.Node n, List<Tree.Node> out) {
        out.add(n);
        for (Tree.Node c : n.children) collect(c, out);
    }

    /** מוסיף את מסלולי הקבצים התקינים של הרשימה; מחזיר כמה שורות פגומות. */
    private static int count(Tree.Node n, Set<String> paths) {
        int bad = 0;
        for (Tree.Entry e : n.entries) {
            if (e.bad) bad++;
            else paths.add(e.path);
        }
        return bad;
    }

    // ---------- בדיקה מול המקור ----------

    private static final class Found {
        int lists, listsMissing, membersWrong, filesMissing, sigWrong;
        /** רשימות חכמות: נמצאו / חסרות / תנאי או מיון שונה (רק בבדיקה מדויקת). */
        int smartFound, smartMissing, smartWrong;
        final List<String> examples = new ArrayList<String>();
    }

    /**
     * לכל רשימה במקור: נמצאת במסד, וחבריה (תת־רשימות וקבצים) הם בדיוק מה שבמקור (exact) או לפחות מה שבמקור.
     * לכל קובץ: נמצא, ואם יש לו חתימה במקור — היא זהה.
     */
    private static Found verify(Store s, Importer.Source src, List<Tree.Node> nodes, Set<String> paths, List<Importer.Smart> defs,
                                boolean exact) {
        Found f = new Found();
        for (Importer.Smart d : defs) {
            long sid = s.findByOrigin(Importer.smartOrigin(src.mode, d.key));
            if (sid == 0) {
                f.smartMissing++;
                example(f, Words.Db.exSmartMissing(d.name));
                continue;
            }
            f.smartFound++;
            if (exact && !(eq(s.prop(sid, Store.P_COND), d.cond) && eq(s.prop(sid, Store.P_SORT), d.sort)
                    && eq(s.prop(sid, Store.P_DIR), d.dir) && eq(s.prop(sid, Store.P_VIEW), d.view))) f.smartWrong++;
        }
        for (int i = 1; i < nodes.size(); i++) {
            Tree.Node n = nodes.get(i);
            long id = s.findByOrigin(Importer.originOf(src.mode, Tree.fullLabel(n.path())));
            if (id == 0) {
                f.listsMissing++;
                example(f, Words.Db.exListMissing(Tree.fullLabel(n.path())));
                continue;
            }
            f.lists++;
            Set<Long> want = new HashSet<Long>();
            for (Tree.Node c : n.children) want.add(s.findByOrigin(Importer.originOf(src.mode, Tree.fullLabel(c.path()))));
            for (String ref : n.smart) {
                Importer.Smart d = src.smart(ref);
                if (d != null) want.add(s.findByOrigin(Importer.smartOrigin(src.mode, d.key)));
            }
            for (Tree.Entry e : n.entries) if (!e.bad) want.add(s.findFile(e.path));
            Set<Long> got = new HashSet<Long>();
            for (Store.Member m : s.members(id)) got.add(m.item.id);
            boolean ok = got.containsAll(want) && !want.contains(0L) && (!exact || want.containsAll(got));
            if (!ok) {
                f.membersWrong++;
                example(f, Words.Db.exMembers(Tree.fullLabel(n.path()), want.size(), got.size()));
            }
        }
        for (String p : paths) {
            long fid = s.findFile(p);
            if (fid == 0) {
                f.filesMissing++;
                example(f, Words.Db.exFileMissing(p));
                continue;
            }
            Sig.Entry g = src.sigs.get(p);
            Store.FileInfo fi = s.file(fid);
            if (g != null && (fi == null || !g.sig.equals(fi.sig))) f.sigWrong++;
        }
        return f;
    }

    private static boolean eq(String a, String b) {
        return (a == null ? "" : a).equals(b == null ? "" : b);
    }

    /**
     * "לא משויכים" במסד הזמני מול המצופה: קבצים שבשורש המקור ואינם באף רשימה. מחזיר [צפוי, נמצא, פריטים שאינם קבצים].
     * (רעיון בלי רשימה אינו אמור להיווצר מהעתקה.)
     */
    private static int[] unassigned(Store t, List<Tree.Node> nodes) {
        Set<String> inLists = new HashSet<String>(), root = new HashSet<String>();
        for (Tree.Entry e : nodes.get(0).entries) if (!e.bad) root.add(e.path);
        for (int i = 1; i < nodes.size(); i++) for (Tree.Entry e : nodes.get(i).entries) if (!e.bad) inLists.add(e.path);
        root.removeAll(inLists);
        int got = 0, other = 0, wrong = 0;
        for (Store.Item it : t.unassigned()) {
            Store.FileInfo fi = it.type == Store.FILE ? t.file(it.id) : null;
            if (fi == null) other++;
            else {
                got++;
                if (!root.contains(fi.path)) wrong++;
            }
        }
        return new int[]{root.size(), got, other + wrong};
    }

    /** הערכת התנאים שהועתקו מול האינדקס: כל תנאי פעם אחת. תנאי שלא הובן הוא בעיה; בלי תוצאות — לידיעה בלבד. */
    private static void evaluate(StringBuilder out, List<String> problems, String mode, List<Importer.Smart> defs, Smarts.Finder fd) {
        if (fd == null || defs.isEmpty()) return;
        long t0 = System.currentTimeMillis();
        Set<String> seen = new HashSet<String>();
        int checked = 0, withHits = 0, noHits = 0, bad = 0, noCond = 0;
        long files = 0;
        List<String> none = new ArrayList<String>(), badLines = new ArrayList<String>();
        for (Importer.Smart d : defs) {
            if (d.cond.isEmpty()) {
                noCond++;
                continue;
            }
            if (!seen.add(d.cond)) continue;
            Smarts.Result r;
            try {
                r = fd.find(d.cond, "", 0);
            } catch (Throwable t) {                                            // תנאי אחד שהפיל את המנוע — בעיה, לא קריסת האופן
                Log.e("FullCheck.evaluate", t);
                bad++;
                if (badLines.size() < EXAMPLES) badLines.add(Words.Db.evalBad(d.name, String.valueOf(t)));
                continue;
            }
            if (!r.ready) {
                out.append(Words.Db.evalNone()).append("\n");
                return;
            }
            checked++;
            files += r.count;
            if (!r.note.isEmpty()) {
                bad++;
                if (badLines.size() < EXAMPLES) badLines.add(Words.Db.evalBad(d.name, r.note));
            } else if (r.count > 0) {
                withHits++;
            } else {
                noHits++;
                if (none.size() < EXAMPLES) none.add(d.name);
            }
        }
        out.append(Words.Db.evalLine(checked, withHits, noHits, bad, noCond, files, System.currentTimeMillis() - t0)).append("\n");
        if (!none.isEmpty()) out.append(Words.Db.evalNoHits(join(none))).append("\n");
        for (String l : badLines) out.append(l).append("\n");
        if (bad > 0) problems.add(Words.Db.pSmartCond(mode, bad));
    }

    private static String join(List<String> l) {
        StringBuilder sb = new StringBuilder();
        for (String x : l) sb.append(sb.length() > 0 ? ", " : "").append(x);
        return sb.toString();
    }

    private static void example(Found f, String text) {
        if (f.examples.size() < EXAMPLES) f.examples.add(text);
    }

    // ---------- רשימות ריקות ----------

    /** רשימות שאין בהן כלום במסד: האם גם במקור הן ריקות, או שיש בהן משהו שעוד לא מועתק. */
    private static void empties(StringBuilder out, Importer.Source src, List<Tree.Node> nodes, Store t) {
        int total = 0, truly = 0, pending = 0;
        List<String> names = new ArrayList<String>();
        for (int i = 1; i < nodes.size(); i++) {
            Tree.Node n = nodes.get(i);
            long id = t.findByOrigin(Importer.originOf(src.mode, Tree.fullLabel(n.path())));
            if (id == 0 || t.count(id) > 0) continue;
            total++;
            if (n.refs.size() + skippedIn(src, n) + n.live.size() + badIn(n) > 0) {
                pending++;
            } else {
                truly++;
                if (names.size() < EXAMPLES) names.add(Tree.fullLabel(n.path()));
            }
        }
        out.append(Words.Db.modeEmpty(total, truly, pending)).append("\n");
        for (String nm : names) out.append("   ").append(nm).append("\n");
    }

    private static int skippedIn(Importer.Source src, Tree.Node n) {
        int k = 0;
        for (String ref : n.smart) if (src.smart(ref) == null) k++;
        return k;
    }

    private static int badIn(Tree.Node n) {
        int b = 0;
        for (Tree.Entry e : n.entries) if (e.bad) b++;
        return b;
    }
}
