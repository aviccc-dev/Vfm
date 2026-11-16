package com.tikiyot.vfm;

// מבנה 163 (סידור תיקיית הפקודות): התכנון של ההשוואה ושל מה שבא אחריה — "השווה" (תיקייה מול תיקייה, ורשימה מול תיקייה),
// "העתק / העבר %רק-במקור%" מתוצאת ההשוואה, ו"העתק … /מצב:" (שחזור המצב של זמן מסוים מגיבוי). עד כאן ישב בתוך Batch.
// (ההשוואה עצמה — chomer/indeks/Compare.) אחראי: לתכנן את השורות האלה. אסור לו: להריץ.

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class CompareCmds {

    private CompareCmds() { }

    /**
     * 1.20.0: COMP <מקור> <יעד> — השוואה לפי מסלול (Compare). מקור שאינו קיים — שגיאה (לא "ריק": אחרת הכול
     * ייראה כנמחק). יעד שאינו קיים — ריק רק אם שורה קודמת יוצרת אותו (MD); אחרת — שגיאה.
     */
    static void comp(Batch.Step s, List<String> args, List<String> paths, Sim sim) {
        sim.compare = null;
        if (s.listName != null) {                                             // 1.22.0
            compList(s, args, paths, sim);
            return;
        }
        if (paths.size() != 2) { s.error = Commands.usage("COMP"); return; }
        File a = new File(paths.get(0)), b = new File(paths.get(1));
        if (!a.isDirectory()) { s.error = Words.Cmd.compareNoSource(args.get(0)); return; }
        if (!b.isDirectory() && !sim.isDir(b)) { s.error = Words.Cmd.compareNoDest(args.get(1)); return; }
        if (DiskRules.inside(a, b) || DiskRules.inside(b, a)) { s.error = Words.Disk.NOT_INTO_ITSELF; return; }
        Compare.Result r = Compare.byPath(sim.lib, a, sim.underWiped(b.getAbsolutePath()) ? Batch.GHOST : b, s.hidden);   // מ0.27
        r.b = b.getAbsolutePath();
        sim.compare = r;
        s.dest = b;
        s.describe = Words.Cmd.compared(Bidi.path(r.a), Bidi.path(r.b),
                Compare.Result.files(r.onlyA, r.onlyADirs), Compare.Result.files(r.onlyB, r.onlyBDirs), r.same);
    }

    /** 1.22.0: COMP /רשימה:<שם> <יעד> — כל מה שברשימה, לפי <מפתח כונן>/<מסלול>, מול היעד. */
    static void compList(Batch.Step s, List<String> args, List<String> paths, Sim sim) {
        if (paths.size() != 1) { s.error = Commands.usage("COMP"); return; }
        File b = new File(paths.get(0));
        if (!b.isDirectory() && !sim.isDir(b)) { s.error = Words.Cmd.compareNoDest(args.get(0)); return; }
        if (sim.lib == null || !sim.lib.ready) { s.error = Words.Cmd.NEEDS_INDEX; return; }
        String[] err = {null};
        File skip = b.getName().equals(State.CURRENT) && b.getParentFile() != null ? b.getParentFile() : b;
        Compare.Source src;
        if (DbVerbs.active()) {                                             // מ0.25 (באג 2): עובדים במסד — הרשימה מהמסד
            src = DbListCmd.compSource(sim, s.listName, skip.getAbsolutePath(), err);
            if (src == null) { s.error = err[0]; return; }
        } else {
            s.error = Words.DbCmd.NO_DB;                                      // מ0.29: רשימות — רק במסד
            return;
        }
        if (src.files.isEmpty() && src.emptyDirs.isEmpty()) { s.error = Words.Cmd.listEmpty(s.listName); return; }
        Compare.Result r = Compare.byList(sim.lib, src, sim.underWiped(b.getAbsolutePath()) ? Batch.GHOST : b, s.hidden);   // מ0.27
        r.b = b.getAbsolutePath();
        sim.compare = r;
        s.dest = b;
        s.describe = Words.Cmd.compared(s.listName, Bidi.path(r.b),
                Compare.Result.files(r.onlyA, new ArrayList<String>()), Compare.Result.files(r.onlyB, r.onlyBDirs), r.same)
                + (src.outside > 0 ? Words.Cmd.outside(src.outside) : "")
                + (src.skippedSmart > 0 ? Words.Cmd.smartSkipped(src.skippedSmart) : "");
    }

    /**
     * 1.20.0: COPY/MOVE %רק-במקור% | %רק-ביעד% <יעד> — מה שההשוואה מצאה, עם שמירת מסלול (יחסית לשורש של אותו צד).
     * /עד:N% — אם זה יותר מ־N אחוזים מהקבצים של אותו צד — שגיאה, והשורות הבאות על ההשוואה לא ירוצו.
     */
    static void fromCompare(Batch.Step s, String cmd, List<String> paths, List<String> sw, Sim sim) {
        Compare.Result r = sim.compare;
        if (r == null) { s.error = Words.Cmd.NO_COMPARE; return; }
        if (r.broken) { s.error = Words.Cmd.COMPARE_BROKEN; return; }
        boolean a = paths.get(0).equals(Syntax.ONLY_A_MARK);
        File dest = new File(paths.get(1));
        if (sim.isFile(dest)) { s.error = Words.Cmd.NO_DEST; r.broken = true; return; }
        s.dest = dest;
        s.fromCompare = true;
        s.files.addAll(a ? r.onlyA : r.onlyB);
        s.dirs.addAll(a ? r.onlyADirs : r.onlyBDirs);
        boolean mapped = a && r.relA != null;                                 // 1.22.0: מקור שהוא רשימה
        if (mapped) {
            if (cmd.equals("MOVE")) { s.error = Words.Cmd.LIST_NO_MOVE; r.broken = true; return; }
            s.keepRoot = null;
        } else {
            s.keepRoot = a ? r.a : r.b;
            if (DiskRules.inside(dest, new File(s.keepRoot))) { s.error = Words.Disk.NOT_INTO_ITSELF; r.broken = true; return; }
        }
        int n = Compare.Result.files(s.files, s.dirs), total = a ? r.filesA : r.filesB;
        for (String x : sw)
            if (x.startsWith(Syntax.SW_LIMIT)) {
                int pct = Batch.limit(x);
                if (total > 0 && (long) n * 100 > (long) pct * total) {
                    s.error = Words.Cmd.overLimit(n, total, pct);
                    r.broken = true;
                    return;
                }
            }
        if (mapped) {                                                          // 1.22.0: כל פריט — לתיקייה שלו
            List<String> items = new ArrayList<String>(s.files);
            items.addAll(s.dirs);
            s.files.clear();
            s.dirs.clear();
            for (String f : items) {
                File to = new File(dest, r.relA.get(f));
                boolean dir = new File(f).isDirectory();
                if (dir ? s.skipTaken && sim.isFile(to) : s.drop(sim, new File(f), to)) { if (dir) s.skipped++; continue; }
                s.files.add(f);
                s.targets.add(to.getParentFile().getAbsolutePath());
                if (dir) sim.dirs.add(to.getAbsolutePath());
                else sim.move(f, s.arrive(sim, to), true);
            }
            s.describe = Words.Cmd.transfer(true, Words.files(s.files.size()), Bidi.path(dest.getAbsolutePath())) + Words.Cmd.KEEP_PATH
                    + Batch.notes(s);
            sim.dirs.add(dest.getAbsolutePath());
            return;
        }
        sim.dirs.add(dest.getAbsolutePath());
        for (java.util.Iterator<String> it = s.files.iterator(); it.hasNext(); ) {   // קובץ אחרי קובץ (מ0.40: לשנות-שם)
            String f = it.next();
            File to = new File(dest, DiskRules.rel(s.keepRoot, f));
            if (s.drop(sim, new File(f), to)) { it.remove(); continue; }
            sim.move(f, s.arrive(sim, to), cmd.equals("COPY"));
        }
        List<String> what = new ArrayList<String>();
        what.add(Words.files(s.files.size()));
        if (!s.dirs.isEmpty()) what.add(Words.folders(s.dirs.size()));
        s.describe = Words.Cmd.transfer(cmd.equals("COPY"), Batch.join(what, Words.Cmd.AND), Bidi.path(dest.getAbsolutePath()))
                + Words.Cmd.KEEP_PATH + Batch.notes(s);
        boolean move = cmd.equals("MOVE");
        for (String d : s.dirs) {                                               // מבנה 67 (ממצא 38): תיקייה שלמה — עם מה שבתוכה (עד כאן: רק התיקייה עצמה)
            File to = new File(dest, DiskRules.rel(s.keepRoot, d));
            if (move) sim.moveDir(new File(d), to, false);
            else sim.copyDir(new File(d), to);
        }
        if (move) {                                                             // מבנה 67 (ממצא 40ו): מה שהועבר כבר אינו "רק במקור" — שורה שנייה
            (a ? r.onlyA : r.onlyB).removeAll(new java.util.HashSet<String>(s.files));       // על אותה השוואה לא תנסה להעביר אותו שוב
            (a ? r.onlyADirs : r.onlyBDirs).removeAll(new java.util.HashSet<String>(s.dirs));
        }
    }

    /**
     * 1.21.0: שחזור — COPY <גיבוי> <יעד> /מצב:<זמן>. המקור: שורש גיבוי "מראה + היסטוריה", או מסלול לוגי בתוכו
     * ("<שורש>\\מסמכים\\א" = מה שהיה ב־מסמכים/א), ואפשר תבנית בסוף. כל קובץ שהיה במצב באותו זמן (State) — מ"נוכחי"
     * או מתיקיית היסטוריה — נכתב ביעד במסלול הלוגי שלו (יחסית לתת־התיקייה שנבחרה). הגיבוי עצמו — לא משתנה.
     */
    static void restore(Batch.Step s, List<String> args, List<String> paths, Filter flt, Sim sim) {
        if (paths.size() != 2) { s.error = Commands.usage("COPY"); return; }
        File src = new File(paths.get(0));
        String mask = null;
        if (src.getName().contains("*") || src.getName().contains("?")) {
            mask = src.getName();
            src = src.getParentFile();
        }
        File root = src;
        while (root != null && !State.isBackupRoot(root)) root = root.getParentFile();
        if (root == null) { s.error = Words.Cmd.notBackup(args.get(0)); return; }
        String prefix = DiskRules.rel(root.getAbsolutePath(), src.getAbsolutePath());
        if (prefix.equals(State.CURRENT) || prefix.startsWith(State.CURRENT + "/") || prefix.equals(State.HISTORY)
                || prefix.startsWith(State.HISTORY + "/")) { s.error = Words.Cmd.STATE_LOGICAL; return; }
        File dest = new File(paths.get(1));
        if (sim.isFile(dest)) { s.error = Words.Cmd.NO_DEST; return; }
        if (DiskRules.inside(dest, root)) { s.error = Words.Disk.NOT_INTO_ITSELF; return; }
        State st = new State(s.stateWhen);
        java.util.TreeMap<String, State.Where> best = new java.util.TreeMap<String, State.Where>();
        java.util.Map<String, String> phys = new java.util.HashMap<String, String>();
        List<File> places = new ArrayList<File>();
        places.add(new File(root, State.CURRENT));
        File[] hs = new File(root, State.HISTORY).listFiles();
        if (hs != null) for (File h : hs) if (h.isDirectory()) places.add(h);
        boolean known = prefix.isEmpty();                                       // מבנה 61: המסלול הלוגי קיים בגיבוי — עכשיו או בהיסטוריה
        for (File place : places) {
            File start = prefix.isEmpty() ? place : new File(place, prefix);
            List<String> found = new ArrayList<String>();
            if (start.exists()) known = true;
            // מבנה 61 (ממצא 40ב): קובץ אחד בשם מפורש ("<גיבוי>\מסמכים\א.txt") — עד כאן נמצאו 0 קבצים, בלי שגיאה (החיפוש היה רק בתיקייה)
            if (mask == null && start.isFile()) {
                if (Batch.visible(s, start) && !Physical.isPartial(start.getName())) found.add(start.getAbsolutePath());
            } else collect(s, start, mask, found);
            for (String f : found) {
                Library.Hit hit = sim.lib == null ? null : sim.lib.find(f);
                State.Where w = st.version(f, hit == null || hit.rec == null ? 0 : hit.rec.added);
                if (w == null || !(w.came <= st.when && st.when < w.left)) continue;
                s.total++;
                if (!flt.ok(new File(f))) continue;
                State.Where old = best.get(w.rel);
                if (old == null || w.left < old.left) {                     // שתי גרסאות — הקרובה ביותר אחרי הזמן
                    best.put(w.rel, w);
                    phys.put(w.rel, f);
                }
            }
        }
        if (!known) { s.error = Words.Cmd.notInBackup(args.get(0)); return; }   // מבנה 61: מסלול שלא היה בגיבוי אף פעם — שגיאה, לא "0 קבצים"
        s.dest = dest;
        for (Map.Entry<String, State.Where> e : best.entrySet()) {
            String rel = e.getKey();
            String sub = prefix.isEmpty() ? rel : rel.equals(prefix) ? new File(rel).getName() : rel.substring(prefix.length() + 1);   // קובץ אחד — ישר ליעד
            File to = new File(dest, sub).getParentFile();
            s.files.add(phys.get(rel));
            s.targets.add(to.getAbsolutePath());
            sim.added.put(new File(dest, sub).getAbsolutePath(), new File(phys.get(rel)));
            for (File up = to; up != null && !up.equals(dest.getParentFile()); up = up.getParentFile()) sim.dirs.add(up.getAbsolutePath());
        }
        if (s.skipTaken)
            for (int k = s.files.size() - 1; k >= 0; k--)
                if (new File(s.targets.get(k), new File(s.files.get(k)).getName()).exists()) {
                    s.files.remove(k);
                    s.targets.remove(k);
                    s.skipped++;
                }
        s.describe = Words.Cmd.restore(Words.files(s.files.size()), new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.ROOT)
                .format(new java.util.Date(s.stateWhen)), Bidi.path(dest.getAbsolutePath()));   // בכונן
    }

    /** כל הקבצים מתחת ל־d (מוסתרים — רק עם /A:H; זמניים — אף פעם), לפי תבנית אם יש. */
    static void collect(Batch.Step s, File d, String mask, List<String> out) {
        File[] k = d.listFiles();
        if (k == null) return;
        java.util.Arrays.sort(k);
        for (File x : k) {
            if (!Batch.visible(s, x) || Physical.isPartial(x.getName())) continue;
            if (x.isDirectory()) collect(s, x, mask, out);
            else if (mask == null || Parse.wild(mask, x.getName())) out.add(x.getAbsolutePath());
        }
    }
}
