package com.tikiyot.vfm;

// בדיקה מלאה אחת (מ0.2.2): מריצה הכול ומחזירה דוח טקסט אחד, שאפשר להעתיק ולשלוח.
// אחראי: לבדוק את שכבת האחסון (בדיקה עצמית), ולכל אופן עבודה במנוע הישן — העתקה מלאה למסד זמני ובדיקה מול המקור
// (רשימות, חברות, קבצים, חתימות, רשימות חכמות ותנאיהן, "לא משויכים", העתקה חוזרת, רשימות ריקות), הערכת התנאים מול האינדקס
// (מ0.3), ואת המסד האמיתי של אותו אופן מול המקור (בלי לשנות אותו). מ0.5: עותק, גיבוי טקסט ושחזור של המסד האמיתי —
// הכול על עותקים זמניים — ובדיקת הגיבויים שבתיקייה מול הרישום. מ0.6: מסך הבית — אותם כלים ובאותו סדר.
// אסור לו: לשנות את המנוע הישן, את המסדים האמיתיים (רק תצוגה מקדימה עליהם) או את הגיבויים (רק קריאה).

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

        /** מ0.5: תיקייה זמנית חדשה וריקה לבדיקת הגיבוי (נמחקת אחרי הבדיקה). */
        File scratch() throws Exception;

        /** מ0.5: הגיבויים של האופן, או null. */
        Backups backups(String mode);
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
            int tLists = t.lists().size() - (Importer.home(t) != 0 ? 1 : 0);   // מ0.6: רשימת הבית אינה רשימה מהעץ
            out.append(Words.Db.modeTemp(tLists, r.filesNew, f.lists, f.membersWrong, f.filesMissing, f.sigWrong,
                    again.nothingToDo(), t.check() == null, ms)).append("\n");
            add(problems, mode, f, nodes.size() - 1, tLists, again.nothingToDo(), t.check());
            Home hm = home(t, src);
            out.append(Words.Db.modeHome(hm.tools, hm.expected.size(), hm.got.size(), hm.skipped, hm.missing, hm.exact())).append("\n");
            if (!hm.exact() || r.homeAdded != hm.expected.size()) problems.add(Words.Db.pHome(mode, hm.expected.size(), hm.got.size()));
            int[] un = unassigned(t, nodes, hm.commandFiles);
            out.append(Words.Db.modeTempSmart(defs.size(), f.smartFound, f.smartWrong, un[0], un[1])).append("\n");
            int[] rf = refs(t, src, nodes, f);
            int cyc = cycles(t);
            out.append(Words.Db.modeRefs(refs, rf[0], rf[1], rf[2], rf[3], cyc)).append("\n");
            if (rf[3] > 0 || r.refsAdded + r.refsKnown != rf[0] || r.refCycles != rf[1] || r.refMissing != rf[2])
                problems.add(Words.Db.pRefs(mode, Math.max(rf[3], 1)));
            if (cyc > 0) problems.add(Words.Db.pCycles(mode, cyc));
            if (un[0] != un[1] || un[2] != 0) problems.add(Words.Db.pUnassigned(mode, un[0], un[1] + un[2]));
            if (f.smartMissing > 0) problems.add(Words.Db.pSmartMissing(mode, f.smartMissing));
            if (f.smartWrong > 0) problems.add(Words.Db.pSmartWrong(mode, f.smartWrong));
            List<Importer.Smart> all = new ArrayList<Importer.Smart>(defs);   // מ0.6: גם התנאים של הכלים החכמים שבבית
            Set<String> keys = new HashSet<String>();
            for (Importer.Smart d : defs) keys.add(d.key);
            for (Structure.Func tf : src.tools())
                if (FnSmart.KIND.equals(tf.kind)) {
                    Importer.Smart d = src.toolSmart(tf);
                    if (keys.add(d.key)) all.add(d);
                }
            evaluate(out, problems, mode, all, env.finder());
            empties(out, src, nodes, t);
        } finally {
            t.close();
        }

        // 2. המסד האמיתי של האופן, מול המקור — בלי לכתוב אליו
        Store real;
        try {
            real = env.openReal(mode);
        } catch (SqlStore.Damaged d) {
            out.append(Words.Db.realDamaged(String.valueOf(d.getMessage()))).append("\n");
            problems.add(Words.Db.pRealDamaged(mode));
            folder(env, mode, out, problems);
            return;
        }
        if (real == null) {
            out.append(Words.Db.REAL_NONE).append("\n");
            folder(env, mode, out, problems);
            return;
        }
        try {
            Found f = verify(real, src, nodes, paths, defs, false);
            Importer.Report pre = Importer.run(real, src, false);
            out.append(Words.Db.modeReal(real.lists().size(), nodes.size() - 1, f.lists, f.membersWrong, f.filesMissing,
                    pre.listsNew, pre.filesNew, pre.filesUpdated, pre.linksAdded, pre.smartNew, real.check() == null)).append("\n");
            out.append(Words.Db.modeRealUnassigned(real.unassigned().size())).append("\n");
            int[] rf = refs(real, src, nodes, f);
            int cyc = cycles(real);
            out.append(Words.Db.modeRealRefs(rf[0], cyc)).append("\n");
            Home hm = home(real, src);
            out.append(Words.Db.modeRealHome(hm.expected.size(), hm.got.size(), pre.homeAdded)).append("\n");
            if (pre.homeAdded > 0 || !hm.present()) problems.add(Words.Db.pHomeBehind(mode, Math.max(pre.homeAdded, 1)));
            if (rf[3] > 0 && pre.refsAdded == 0) problems.add(Words.Db.pRefs(mode, rf[3]));   // חסרות שהעתקה חוזרת תשלים — כבר ב"לא תואם"
            if (cyc > 0) problems.add(Words.Db.pCycles(mode, cyc));
            if (pre.listsNew + pre.filesNew + pre.filesUpdated + pre.linksAdded + pre.smartNew + pre.refsAdded > 0 || f.listsMissing > 0
                    || f.filesMissing > 0 || f.smartMissing > 0)
                problems.add(Words.Db.pRealBehind(mode, pre.listsNew, pre.filesNew, pre.filesUpdated, pre.linksAdded, pre.smartNew, pre.refsAdded));
            if (real.check() != null) problems.add(Words.Db.pCheck(mode, real.check()));
            for (String e : f.examples) out.append("   ").append(e).append("\n");
            if (real instanceof SqlStore) backup((SqlStore) real, mode, env, out, problems);
        } finally {
            real.close();
        }
        folder(env, mode, out, problems);
    }

    // ---------- מ0.5: גיבוי ----------

    /**
     * עותק דרך SQLite, גיבוי טקסט, שחזור מהטקסט למסד ריק ושחזור מהעותק — כולם בתיקייה זמנית; כל אחד חייב לתת אותה חתימת
     * תוכן כמו המסד האמיתי. המסד האמיתי רק נקרא.
     */
    private static void backup(SqlStore real, String mode, Env env, StringBuilder out, List<String> problems) {
        long t0 = System.currentTimeMillis();
        try {
            File w = env.scratch();
            String live = DbDump.hash(real);
            File copy = new File(w, "copy.db"), txt = new File(w, "copy.txt");
            real.copyTo(copy);
            String hc, ht;
            SqlStore ro = SqlStore.openReadOnly(copy);
            try {
                hc = DbDump.hash(ro);
                ht = DbDump.write(ro, txt, System.currentTimeMillis());
            } finally {
                ro.close();
            }
            DbDump.Data d = DbDump.read(txt);
            Backups.Built fromText = Backups.build(txt, new File(w, "from-text.db"), new IdCounter(new File(w, "c1.txt")));
            Backups.Built fromCopy = Backups.build(copy, new File(w, "from-copy.db"), new IdCounter(new File(w, "c2.txt")));
            boolean copyOk = live.equals(hc), textOk = live.equals(ht) && live.equals(d.hash) && live.equals(fromText.content),
                    restoreOk = live.equals(fromCopy.content);
            out.append(Words.Db.backupLine(copyOk, copy.length(), textOk, d.records.size(), txt.length(), restoreOk,
                    System.currentTimeMillis() - t0)).append("\n");
            if (!copyOk) problems.add(Words.Db.pBackupCopy(mode));
            if (!textOk) problems.add(Words.Db.pBackupText(mode));
            if (!restoreOk) problems.add(Words.Db.pBackupRestore(mode));
        } catch (Throwable t) {
            Log.e("FullCheck.backup", t);
            out.append(Words.Db.pBackupFailed(mode, String.valueOf(t))).append("\n");
            problems.add(Words.Db.pBackupFailed(mode, String.valueOf(t)));
        }
    }

    /** הגיבויים שבתיקייה: כמה מכל סוג, האחרון, ובדיקה מול הרישום. */
    private static void folder(Env env, String mode, StringBuilder out, List<String> problems) {
        Backups b = env.backups(mode);
        if (b == null) return;
        try {
            List<Backups.Entry> all = b.list();
            if (all.isEmpty()) {
                out.append(Words.Db.BACKUPS_NONE).append("\n");
                return;
            }
            int daily = 0, before = 0, text = 0, manual = 0;
            for (Backups.Entry e : all) {
                if (e.kind.equals(Backups.DAILY)) daily++;
                else if (e.kind.equals(Backups.BEFORE)) before++;
                else if (e.kind.equals(Backups.TEXT)) text++;
                else if (e.kind.equals(Backups.MANUAL)) manual++;
            }
            int[] v = b.verifyAll(all);
            out.append(Words.Db.backupsLine(all.size(), daily, before, text, manual, when(all.get(0)), v[0], v[1], v[2], v[3])).append("\n");
            if (v[1] > 0) problems.add(Words.Db.pBackupsChanged(mode, v[1]));
            if (v[2] > 0) problems.add(Words.Db.pBackupsBad(mode, v[2]));
        } catch (Throwable t) {
            Log.e("FullCheck.folder", t);
            problems.add(Words.Db.pBackupFailed(mode, String.valueOf(t)));
        }
    }

    /** מתי נעשה הגיבוי: מהרישום (מדויק), ואם אינו רשום — מהשם. */
    static String when(Backups.Entry e) {
        return new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.US).format(new java.util.Date(e.made > 0 ? e.made : e.when));
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
            for (String ref : n.refs) {
                long tg = s.findByOrigin(Importer.originOf(src.mode, Tree.fullLabel(Tree.parsePath(ref))));
                if (tg != 0 && !s.makesCycle(id, tg)) want.add(tg);
            }
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

    // ---------- מ0.6: מסך הבית ----------

    /** מה שבבית מול מה שצפוי מהכלים במקור. expected — לפי סדר הכלים, בלי כפילות; 0 = כלי שהפריט שלו לא נמצא במסד. */
    private static final class Home {
        int tools, skipped, missing;
        final List<Long> expected = new ArrayList<Long>(), got = new ArrayList<Long>();
        final Set<String> commandFiles = new HashSet<String>();

        /** בדיוק אותם כלים ובאותו סדר (במסד שנוצר עכשיו מההעתקה). */
        boolean exact() {
            return !expected.contains(0L) && got.equals(expected);
        }

        /** כל הצפויים נמצאים (במסד האמיתי — המשתמש רשאי לסדר ולהוסיף). */
        boolean present() {
            return !expected.contains(0L) && got.containsAll(expected);
        }
    }

    /** מחפש כל כלי במסד בדרך משלו (לפי מקור או מסלול) — לא דרך ההעתקה — ומשווה לחברי רשימת הבית. */
    private static Home home(Store s, Importer.Source src) {
        Home h = new Home();
        java.util.LinkedHashSet<Long> want = new java.util.LinkedHashSet<Long>();
        for (Structure.Func f : src.tools()) {
            h.tools++;
            if (FnVirtual.KIND.equals(f.kind)) {
                long id = s.findByOrigin(Importer.originOf(src.mode, Tree.fullLabel(Importer.toolList(f))));
                if (id == 0) h.missing++;
                else want.add(id);
            } else if (FnSmart.KIND.equals(f.kind)) {
                want.add(s.findByOrigin(Importer.smartOrigin(src.mode, src.toolSmart(f).key)));
            } else if (FnCommand.KIND.equals(f.kind)) {
                String p = Importer.toolFile(f);
                if (p.isEmpty()) h.missing++;
                else {
                    h.commandFiles.add(p);
                    want.add(s.findFile(p));
                }
            } else {
                h.skipped++;
            }
        }
        h.expected.addAll(want);
        long id = Importer.home(s);
        if (id != 0) for (Store.Member m : s.members(id)) h.got.add(m.item.id);
        return h;
    }

    private static boolean eq(String a, String b) {
        return (a == null ? "" : a).equals(b == null ? "" : b);
    }

    /**
     * "לא משויכים" במסד הזמני מול המצופה: קבצים שבשורש המקור ואינם באף רשימה. מחזיר [צפוי, נמצא, פריטים שאינם קבצים].
     * (רעיון בלי רשימה אינו אמור להיווצר מהעתקה.)
     */
    private static int[] unassigned(Store t, List<Tree.Node> nodes, Set<String> inHome) {
        Set<String> inLists = new HashSet<String>(inHome), root = new HashSet<String>();   // מ0.6: קובץ פקודות בבית — משויך
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
            if (skippedRefs(t, src, n, id) + skippedIn(src, n) + n.live.size() + badIn(n) > 0) {
                pending++;
            } else {
                truly++;
                if (names.size() < EXAMPLES) names.add(Tree.fullLabel(n.path()));
            }
        }
        out.append(Words.Db.modeEmpty(total, truly, pending)).append("\n");
        for (String nm : names) out.append("   ").append(nm).append("\n");
    }

    /** הפניות של הרשימה שלא הועתקו: לרשימה שלא קיימת, או שהיו סוגרות מעגל. */
    private static int skippedRefs(Store t, Importer.Source src, Tree.Node n, long id) {
        int k = 0;
        for (String ref : n.refs) {
            long tg = t.findByOrigin(Importer.originOf(src.mode, Tree.fullLabel(Tree.parsePath(ref))));
            if (tg == 0 || t.makesCycle(id, tg)) k++;
        }
        return k;
    }

    /**
     * ההפניות במסד מול המקור: [הועתקו, נחסמו כמעגל, לרשימה שלא קיימת, שגויות]. הועתקה = הרשימה שאליה מפנים חברה ברשימה המפנה;
     * מעגל = היא כבר מכילה את המפנה; שגויה — אף אחד מאלה. הפניות בשורש — "לא קיימת" (אין רשימה מפנה).
     */
    private static int[] refs(Store s, Importer.Source src, List<Tree.Node> nodes, Found f) {
        int copied = 0, cyc = 0, missing = nodes.get(0).refs.size(), wrong = 0;
        for (int i = 1; i < nodes.size(); i++) {
            Tree.Node n = nodes.get(i);
            if (n.refs.isEmpty()) continue;
            long id = s.findByOrigin(Importer.originOf(src.mode, Tree.fullLabel(n.path())));
            if (id == 0) continue;
            Set<Long> got = new HashSet<Long>();
            for (Store.Member m : s.members(id)) got.add(m.item.id);
            for (String ref : n.refs) {
                long tg = s.findByOrigin(Importer.originOf(src.mode, Tree.fullLabel(Tree.parsePath(ref))));
                if (tg == 0) missing++;
                else if (got.contains(tg)) copied++;
                else if (s.makesCycle(id, tg)) {
                    cyc++;
                    example(f, Words.Db.exCycle(Tree.fullLabel(n.path()), ref));
                } else wrong++;
            }
        }
        return new int[]{copied, cyc, missing, wrong};
    }

    /** כמה רשימות במסד נמצאות בתוך עצמן. אמור להיות 0 תמיד. */
    private static int cycles(Store s) {
        int n = 0;
        for (Store.Item l : s.lists()) if (s.contains(l.id, l.id)) n++;
        return n;
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
