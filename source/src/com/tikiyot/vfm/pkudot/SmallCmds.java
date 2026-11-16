package com.tikiyot.vfm;

// מבנה 161 (סידור תיקיית הפקודות): התכנון של הפקודות הקטנות — הגדר, כתוב, המתן, גבה, רענן, דוח, ארוז / פרוס, שחזר מהסל,
// רוקן סל, ודא, קטע, שחזר דוגמאות… (כל מה שאינו העתק / העבר / מחק / שנה / השוואה). עד כאן ישב בתוך Batch.
// אחראי: לתכנן שורה אחת של פקודה קטנה. אסור לו: להריץ בעצמו (את הפעולה — op, שהמנוע מריץ).

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class SmallCmds {

    private SmallCmds() { }

    /**
     * מבנה 127: "הגדר" — הכלל האחד לכל הדרכים (קובץ, מסך): ההגדרה חייבת להיות מוכרת (Conf.SETTINGS), והערך — מתאים לסוג שלה.
     * מהמסך — "הגדר "%שם-חדש%" "%תוכן%"": המפתח והערך מהבחירה, ואינם מפוענחים (מסלול של תיקייה יכול להכיל %).
     * נקבעת באופן העבודה הפתוח — שם קובץ ההגדרות.
     */
    static void planSet(final Batch.Step s, List<String> tk, Map<String, String> vars, Sim sim) {
        if (tk.size() != 3) { s.error = Words.Cmd.SET_USAGE; return; }
        Batch.Selection sel = sim.selected;
        boolean picked = tk.get(1).equals(Parse.var(Batch.V_NEW_NAME)) && tk.get(2).equals(Parse.var(Batch.V_CONTENT));
        if (picked && (sel == null || sel.name == null || sel.content == null)) { s.error = Words.Cmd.NOTHING_SELECTED; return; }
        final String key = picked ? sel.name : Verbs.withVars(tk.get(1), vars).trim();
        String raw = picked ? sel.content : Verbs.withVars(tk.get(2), vars);
        final Conf.Setting set = Conf.setting(key);
        if (set == null) {
            StringBuilder known = new StringBuilder();
            for (Conf.Setting x : Conf.SETTINGS) known.append(known.length() > 0 ? " · " : "").append(x.key);
            s.error = Words.Cmd.setUnknown(key, known.toString());
            return;
        }
        final String value = Conf.valid(set, raw);
        if (value == null) {
            StringBuilder allowed = new StringBuilder();
            if (set.values != null) for (String x : set.values) allowed.append(allowed.length() > 0 ? " · " : "").append(x);
            s.error = set.kind == Conf.CHOICE ? Words.Cmd.setBadChoice(key, raw.trim(), allowed.toString())
                    : set.kind == Conf.NUMBER ? Words.Cmd.setBadNumber(key, raw.trim())
                    : set.kind == Conf.DIRS ? Words.Cmd.setBadDirs(key, raw.trim()) : Words.Cmd.setBadText(key);
            return;
        }
        s.describe = Words.Cmd.setWill(key, value);
        s.op = new ListCmd.Op() {
            @Override public String run(Library lib) {
                if (value.equals(Conf.current(set))) return Words.Cmd.setDone(key, value) + " · " + Words.Cmd.SET_SAME;
                String no = Conf.apply(set, value);
                if (no != null) {
                    DbListCmd.lastFailed = true;
                    return no;
                }
                return Words.Cmd.setDone(key, value);
            }
        };
    }

    static boolean small(final Batch.Plan plan, final Batch.Step s, String cmd, List<String> tk, File here,
                                 Map<String, String> vars, final Sim sim) {
        if (cmd.equals(Syntax.DROP_MODE)) {                                            // מ0.45: מחק אופן
            if (tk.size() != 2 || tk.get(1).trim().isEmpty()) { s.error = Words.DbCmd.DROP_MODE_USAGE; return true; }
            DbListCmd.planDropMode(s, Verbs.withVars(tk.get(1).trim(), vars), sim);
            return true;
        }
        if (cmd.equals(Syntax.EXAMPLES)) return planExamples(plan, s, cmd, tk, here, vars, sim);   // מבנה 123: שחזר דוגמאות (מה שהמסך עשה בעצמו)
        if (cmd.equals(Syntax.SET)) {                                                  // מבנה 127: הגדר "<הגדרה>" "<ערך>"
            planSet(s, tk, vars, sim);
            return true;
        }
        if (cmd.equals(Syntax.UNTRASH)) {                                              // מ0.45: שחזר מהסל
            planUntrash(s, tk, here, vars, sim);
            return true;
        }
        if (cmd.equals(Syntax.PURGE)) {                                                // מ0.45: רוקן סל
            planPurge(s, tk, here, vars, sim);
            return true;
        }
        if (cmd.equals(Syntax.VERIFY) && tk.size() == 2 && tk.get(1).trim().equals(Words.DbCmd.W_OK_SINCE)) {   // מ0.45: ודא תקין
            s.cmd = Syntax.OK_SINCE;
            s.describe = Words.DbCmd.verifyWill(Words.DbCmd.OK_SINCE_WHAT);
            return true;
        }
        if (cmd.equals(Syntax.VERIFY) && tk.size() == 2 && tk.get(1).trim().equals(Words.DbCmd.W_STEADY)) return planVerifySteady(plan, s, cmd, tk, here, vars, sim);   // מבנה 50: ודא רציף
        if (cmd.equals(Syntax.VERIFY) && tk.size() == 4 && tk.get(1).trim().equals(Verbs.MODE_WORD)) return planVerifyMode(plan, s, cmd, tk, here, vars, sim);   // מ0.45: ודא אופן "<שם>" קיים / חסר
        if (cmd.equals(Syntax.SECTION)) {                                              // מ0.44: קטע
            if (tk.size() != 2 || tk.get(1).trim().isEmpty()) { s.error = Words.DbCmd.SECTION_USAGE; return true; }
            s.noop = true;
            s.describe = Words.DbCmd.section(Verbs.withVars(tk.get(1).trim(), vars));
            return true;
        }
        if (cmd.equals(Syntax.VERIFY) && tk.size() == 2 && tk.get(1).trim().equals(Syntax.W_DB)) {   // מ0.42: ודא מסד
            ListVerify.planVerifyDb(s);
            return true;
        }
        if (cmd.equals(Syntax.VERIFY) && tk.size() == 2 && tk.get(1).trim().equals(Syntax.W_ERROR)) {
            s.cmd = RunReport.EXPECT;
            s.noop = true;
            s.describe = Words.DbCmd.EXPECT_NEXT;
            return true;
        }
        if (cmd.equals(Syntax.WAIT)) return planWait(plan, s, cmd, tk, here, vars, sim);
        if (cmd.equals(Syntax.PACK) || cmd.equals(Syntax.UNPACK)) return planZip(plan, s, cmd, tk, here, vars, sim);   // מבנה 122: מה שהמסך עשה בעצמו (PhysicalUi.askZip / askUnzip)
        if (cmd.equals(Syntax.REPORT)) return planReport(plan, s, cmd, tk, here, vars, sim);
        if (cmd.equals(Syntax.REFRESH)) return planRefresh(plan, s, cmd, tk, here, vars, sim);
        if (cmd.equals(Syntax.BACKUP)) return planBackup(plan, s, cmd, tk, here, vars, sim);
        if (cmd.equals(Syntax.WRITE)) return planWrite(plan, s, cmd, tk, here, vars, sim);
        return false;
    }

    /** מבנה 123: שחזר דוגמאות (מה שהמסך עשה בעצמו) */
    private static boolean planExamples(final Batch.Plan plan, final Batch.Step s, String cmd, List<String> tk, File here, Map<String, String> vars, final Sim sim) {
        if (tk.size() != 1) { s.error = Words.Cmd.EXAMPLES_USAGE; return true; }
        if (!DbVerbs.active()) { s.error = Words.DbCmd.VERIFY_DB_ONLY; return true; }
        sim.dbs.remove(DbExamples.MODE);                                    // מה שההדמיה החזיקה מהאופן הזה — כבר לא תקף
        s.describe = Words.Cmd.EXAMPLES_WILL;
        s.op = new ListCmd.Op() {
            @Override public String run(Library lib) {
                boolean[] failed = {false};
                String msg = Db.restoreExamples(Db.app, Reserved.FILES, failed);
                if (failed[0]) DbListCmd.lastFailed = true;
                return msg;
            }
        };
        return true;
    }

    /** מבנה 50: ודא רציף */
    private static boolean planVerifySteady(final Batch.Plan plan, final Batch.Step s, String cmd, List<String> tk, File here, Map<String, String> vars, final Sim sim) {
        if (!plan.measure) { s.error = Words.DbCmd.STEADY_NEEDS_MEASURE; return true; }
        s.describe = Words.DbCmd.verifyWill(Words.DbCmd.STEADY_WHAT);
        s.op = new ListCmd.Op() {
            @Override public String run(Library lib) {
                Runner.Pulse p = plan.pulse;
                long off = p == null ? 0 : p.screenOff(), miss = p == null ? 0 : p.missed();
                int r = Batch.steady(off, miss);
                s.verified = r == Batch.STEADY_OK;
                return Words.DbCmd.steadyResult(r, off, miss);
            }
        };
        return true;
    }

    /** מ0.45: ודא אופן "<שם>" קיים / חסר */
    private static boolean planVerifyMode(final Batch.Plan plan, final Batch.Step s, String cmd, List<String> tk, File here, Map<String, String> vars, final Sim sim) {
        final String m = Verbs.withVars(tk.get(2).trim(), vars);
        String w = tk.get(3).trim();
        final boolean want = w.equals(Words.DbCmd.WORD_EXISTS_M) || w.equals(Words.DbCmd.WORD_EXISTS);
        if (!want && !w.equals(Words.DbCmd.WORD_MISSING_M) && !w.equals(Words.DbCmd.WORD_MISSING)) { s.error = Words.DbCmd.VERIFY_USAGE; return true; }
        if (!DbVerbs.active()) { s.error = Words.DbCmd.VERIFY_DB_ONLY; return true; }
        final String what = Words.DbCmd.verifyMode(m, want);
        s.target = m;
        s.describe = Words.DbCmd.verifyWill(what);
        s.op = new ListCmd.Op() {
            @Override public String run(Library lib) {
                boolean has = Db.dbModes(Db.app).contains(m);
                s.verified = has == want;
                return Words.DbCmd.verifyResult(has == want, what, has ? Words.DbCmd.WORD_EXISTS_M : Words.DbCmd.WORD_MISSING_M);
            }
        };
        return true;
    }

    /** planWait */
    private static boolean planWait(final Batch.Plan plan, final Batch.Step s, String cmd, List<String> tk, File here, Map<String, String> vars, final Sim sim) {
        int sec = -1;
        if (tk.size() == 2) try { sec = Integer.parseInt(tk.get(1).trim()); } catch (NumberFormatException e) { sec = -1; }
        if (sec < 0 || sec > 600) { s.error = Words.DbCmd.WAIT_USAGE; return true; }
        final int n = sec;
        s.describe = Words.DbCmd.waitFor(n);
        s.op = new ListCmd.Op() {
            @Override public String run(Library lib) {
                // מבנה 62 (ממצא 40ה): בחתיכות של רבע שנייה — "עצור" נענה מיד (עד כאן: רק אחרי שכל ההמתנה נגמרה, עד 600 שניות)
                for (int left = n * 4; left > 0; left--) {
                    if (plan.asked != null && plan.asked.stopped()) return Words.DbCmd.waitStopped(n, n - (left + 3) / 4);
                    try { Thread.sleep(250); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
                }
                return Words.DbCmd.waitFor(n);
            }
        };
        return true;
    }

    /** מבנה 122: מה שהמסך עשה בעצמו (PhysicalUi.askZip / askUnzip) */
    private static boolean planZip(final Batch.Plan plan, final Batch.Step s, String cmd, List<String> tk, File here, Map<String, String> vars, final Sim sim) {
        final boolean pack = cmd.equals(Syntax.PACK);
        String[] err = {null};
        String p = tk.size() == 2 ? Parse.path(tk.get(1), here, vars, err) : null;
        if (p == null) { s.error = err[0] != null ? err[0] : pack ? Words.Cmd.PACK_USAGE : Words.Cmd.UNPACK_USAGE; return true; }
        if (p.equals(Batch.SELECTED_MARK)) {                                      // מהמסך: התיקייה / הקובץ שנבחרו
            Batch.Selection sel = sim.selected;
            List<String> one = sel == null ? null : pack ? sel.dirs : sel.files;
            if (one == null || one.size() != 1) { s.error = Words.Cmd.NOTHING_SELECTED; return true; }
            p = one.get(0);
        }
        final File src = new File(p), parent = src.getParentFile();
        if (pack ? !sim.isDir(src) : !sim.isFile(src)) { s.error = (pack ? Words.FOLDER_NOT_FOUND : Words.FILE_NOT_FOUND) + ": " + tk.get(1); return true; }
        if (parent == null) { s.error = Words.Cmd.NO_DEST; return true; }
        String stem = src.getName();
        if (!pack && stem.toLowerCase(Locale.ROOT).endsWith(".zip")) stem = stem.substring(0, stem.length() - 4);
        if (!pack && stem.trim().isEmpty()) stem = Words.Send.UNPACK_DIR;
        File out0 = new File(parent, pack ? src.getName() + ".zip" : stem);
        for (int i = 2; sim.isFile(out0) || sim.isDir(out0); i++) out0 = new File(parent, pack ? src.getName() + " (" + i + ").zip" : stem + " (" + i + ")");
        final File out = out0;
        String own = Physical.programFile(pack ? src.getAbsolutePath() : out.getAbsolutePath());
        if (own != null) { s.error = pack ? Words.Disk.NO_FILE_IN_APP : Words.Disk.NO_UNPACK_IN_APP; return true; }
        if (sim.lib != null && Physical.noWriteInto(sim.lib, parent) != null) { s.error = Physical.noWriteInto(sim.lib, parent); return true; }
        if (!pack && src.isFile() && Zip.contents(src) == null) { s.error = Words.Disk.BAD_ZIP; return true; }
        if (pack) sim.added.put(out.getAbsolutePath(), Batch.GHOST);
        else sim.dirs.add(out.getAbsolutePath());                            // מה שבתוכה — נודע רק בהרצה
        sim.gone.remove(out.getAbsolutePath());
        s.dest = out;
        s.describe = pack ? Words.Cmd.packWill(src.getName(), out.getName()) : Words.Cmd.unpackWill(src.getName(), out.getName());
        s.op = new ListCmd.Op() {
            @Override public String run(Library lib) {
                try {
                    if (out.exists()) { DbListCmd.lastFailed = true; return Words.Disk.ALREADY_NAME + out.getName() + "\""; }
                    if (!pack) {
                        long[] c = Zip.contents(src);
                        if (c == null) { DbListCmd.lastFailed = true; return Words.Disk.BAD_ZIP; }
                        long free = Physical.free(parent);
                        if (c[1] > 0 && free < c[1]) { DbListCmd.lastFailed = true; return Words.Cmd.noRoom(Files.size(c[1]), Files.size(free)); }
                    }
                    Zip.Result r = pack ? Zip.pack(src, out, plan.asked) : Zip.unpack(src, out, plan.asked);
                    if (lib != null) {
                        lib.dirChanged(parent.getAbsolutePath());          // האינדקס לומד על מה שנוצר
                        lib.flush(5000);
                    }
                    if (r.error != null) { DbListCmd.lastFailed = true; return r.error; }
                    return (pack ? Words.Cmd.packed(out.getName(), r.files)
                            : Words.Disk.UNPACKED + Words.files(r.files) + Words.Disk.TO_QUOTE + out.getName() + "\""
                            + (r.skipped > 0 ? " · " + r.skipped + Words.Disk.SKIPPED : "")) + (r.stopped ? Words.Disk.STOPPED_SUFFIX : "");
                } catch (Throwable t) {
                    Log.e("Batch.zip", t);
                    DbListCmd.lastFailed = true;
                    return Words.Db.failed(String.valueOf(t.getMessage()));
                }
            }
        };
        return true;
    }

    /** planReport */
    private static boolean planReport(final Batch.Plan plan, final Batch.Step s, String cmd, List<String> tk, File here, Map<String, String> vars, final Sim sim) {
        String[] err = {null};
        String p = tk.size() == 2 ? Parse.path(tk.get(1), here, vars, err) : null;
        if (p == null) { s.error = err[0] != null ? err[0] : Words.DbCmd.REPORT_USAGE; return true; }
        if ((s.error = Batch.noWriteHere(p)) != null) return true;                 // מבנה 45
        plan.reportFile = p;
        s.noop = true;
        s.describe = Words.DbCmd.reportTo(Bidi.path(p));
        return true;
    }

    /** planRefresh */
    private static boolean planRefresh(final Batch.Plan plan, final Batch.Step s, String cmd, List<String> tk, File here, Map<String, String> vars, final Sim sim) {
        int readers = 0;                                                     // מ0.45 (ב9): "רענן מלא /קוראים:N" — כמה ראשים הכי מהיר
        List<String> a = new ArrayList<String>();
        for (int i = 1; i < tk.size(); i++) {
            String x = tk.get(i).trim();
            if (!x.startsWith(Words.DbCmd.SW_READERS)) { a.add(x); continue; }
            try { readers = Integer.parseInt(x.substring(Words.DbCmd.SW_READERS.length()).trim()); } catch (NumberFormatException e) { readers = -1; }
            if (readers < 1 || readers > 16) { s.error = Words.DbCmd.READERS_USAGE; return true; }
        }
        final boolean deep = a.size() == 1 && a.get(0).equals(Syntax.W_DEEP);
        final boolean full = a.size() == 1 && a.get(0).equals(Syntax.W_FULL);   // מ0.43: בנייה מחדש — מה שהיה הכפתור בהגדרות, כבדיקה
        if (a.size() > 1 || (a.size() == 1 && !deep && !full)) { s.error = Words.DbCmd.REFRESH_USAGE; return true; }
        if (readers > 0 && !full) { s.error = Words.DbCmd.READERS_USAGE; return true; }
        final int rd = readers;
        final String note = rd > 0 ? Words.DbCmd.readersNote(rd) : "";
        s.describe = full ? Words.DbCmd.REBUILD_WILL + note : Words.DbCmd.refreshWill(deep);
        // מ0.40 — המשתמש: "הוא יכול להריץ פקודה 'רענן אינדקס' לפני שהוא מתחיל". עד כאן "רענן" רץ רק בהרצה — אחרי שכל
        // השורות כבר תוכננו, ורשימה חכמה בשורה שאחריו לא ראתה קובץ שהורד הרגע. עכשיו — רענון מהיר כבר בהכנה (ברקע, לא
        // משנה קבצים), והשורות שאחריו מתוכננות מול האינדקס המעודכן. בהרצה — כמו קודם (גם מה ששורות קודמות שינו).
        final int[] inPlan = {0};                                            // מבנה 120: מה שנמצא כבר בהכנה — נספר בהודעה של ההרצה
        if (sim.lib != null && sim.lib.ready)
            try {
                int n = sim.lib.refreshAsked();
                inPlan[0] = n;
                sim.condFiles.clear();                                           // מה שחכמות מצאו לפני הרענון — לא תקף
                s.describe += Words.DbCmd.refreshedInPlan(n);
            } catch (Throwable t) {
                Log.e("Batch.plan: רענן", t);                                     // בכונן
            }
        s.op = new ListCmd.Op() {
            @Override public String run(Library lib) {
                if (lib == null) return full ? Words.DbCmd.REBUILD_WILL + note : Words.DbCmd.refreshWill(deep);
                if (full) {
                    final Library l = lib;                                   // מבנה 120: ההתקדמות — במצב של האינדקס (מסך ההגדרות מציג אותו)
                    Index.Progress pr = new Index.Progress() {
                        @Override public void onScan(String label, int items) { l.status = Words.Prefs.SCANNING + label + "… " + Files.count(items); }
                    };
                    if (rd > 0) lib.build(Words.Lib.WHY_REQUEST, pr, rd);
                    else lib.build(pr);
                    return Words.DbCmd.rebuilt(lib.totalFiles(), lib.totalDirs()) + note;
                }
                return Words.DbCmd.refreshed((deep ? lib.deepRefresh() : lib.refreshAsked()) + inPlan[0]);
            }
        };
        return true;
    }

    /** planBackup */
    private static boolean planBackup(final Batch.Plan plan, final Batch.Step s, String cmd, List<String> tk, File here, Map<String, String> vars, final Sim sim) {
        if (tk.size() != 1) { s.error = Words.DbCmd.BACKUP_USAGE; return true; }
        if (!DbVerbs.active()) { s.error = Words.DbCmd.VERIFY_DB_ONLY; return true; }
        if (sim.modeError != null) { s.error = sim.modeError; return true; }
        final String mode = sim.mode != null ? sim.mode : Db.currentMode;
        s.describe = Words.DbCmd.backupWill(mode);
        s.op = new ListCmd.Op() {
            @Override public String run(Library lib) { return DbListCmd.backupNow(mode); }
        };
        return true;
    }

    /** planWrite */
    private static boolean planWrite(final Batch.Plan plan, final Batch.Step s, String cmd, List<String> tk, File here, Map<String, String> vars, final Sim sim) {
        boolean append = false;
        String taken = null;                                                // מבנה 110 (ה6): /תפוס: — מה עושים כשהקובץ כבר קיים
        List<String> a = new ArrayList<String>();
        for (int i = 1; i < tk.size(); i++) {
            String x = tk.get(i).trim();
            if (x.equals(Syntax.SW_APPEND)) append = true;
            else if (x.startsWith(Syntax.SW_TAKEN)) {
                taken = x.substring(Syntax.SW_TAKEN.length());
                if (!taken.equals(Syntax.TAKEN_STOP) && !taken.equals(Syntax.TAKEN_REPLACE) && !taken.equals(Syntax.TAKEN_SKIP) && !taken.equals(Syntax.TAKEN_RENAME)) {
                    s.error = Words.Cmd.WRITE_TAKEN_ONLY;
                    return true;
                }
            } else a.add(tk.get(i));
        }
        if (a.isEmpty() || a.size() > 2) { s.error = Words.DbCmd.WRITE_USAGE; return true; }
        if (append && taken != null) { s.error = Words.Cmd.WRITE_TAKEN_ONLY; return true; }   // "להוסיף" — אין שם תפוס
        String[] err = {null};
        String p = Parse.path(a.get(0), here, vars, err);
        if (p == null) { s.error = err[0]; return true; }
        if (new File(p).getName().equals(Batch.NAME_TOKEN) || (a.size() == 2 && a.get(1).equals(Parse.var(Batch.V_CONTENT)) && sim.selected != null
                && sim.selected.content != null)) {                         // מבנה 80: קובץ חדש מהמסך
            writeSelected(s, new File(p), append || a.size() != 2 || !a.get(1).equals(Parse.var(Batch.V_CONTENT)), taken, sim);
            return true;
        }
        if (Physical.programFile(p) != null) { s.error = Physical.programFile(p); return true; }   // מבנה 45
        File f0 = new File(p);
        final String text = a.size() == 2 ? Verbs.withVars(a.get(1), vars).replace("\\n", "\n") : "";
        File dir = f0.getParentFile();
        if (dir == null || !sim.isDir(dir)) { s.error = Words.DbCmd.writeNoFolder(Bidi.path(dir == null ? p : dir.getAbsolutePath())); return true; }
        if (sim.isDir(f0)) { s.error = Words.DbCmd.WRITE_IS_FOLDER; return true; }
        int t = writeTaken(s, f0, append, taken, sim);                     // מבנה 110: הכלל האחד של "קובץ קיים"
        if (t == W_STOP) return true;
        if (t == W_FREE) f0 = sim.free(f0);
        final File f = f0;
        final boolean replace = t == W_REPLACE;
        if (sim.lib != null && Physical.noWriteInto(sim.lib, dir) != null) { s.error = Physical.noWriteInto(sim.lib, dir); return true; }   // מבנה 65: הסל — כבר בתכנון
        final boolean add = append;
        boolean had = sim.isFile(f) && sim.real(f) != Batch.GHOST;                // מבנה 46: קובץ שכבר קיים (לא כזה ששורה קודמת כותבת)
        sim.gone.remove(p);
        if (!sim.isFile(f)) sim.added.put(p, Batch.GHOST);
        s.dest = f;
        s.describe = Words.DbCmd.writeWill(f.getName(), text.length(), add) + (!add && had ? Words.DbCmd.OLD_TO_TRASH_WILL : "");
        s.op = new ListCmd.Op() {
            @Override public String run(Library lib) {
                try {
                    String no = Permit.check(Permit.WRITE);                     // מבנה 80: גם "כתוב" שואלת את ההיתר
                    if (no != null) { DbListCmd.lastFailed = true; return no; }
                    // מבנה 65: מותר בכל מקום שמותר להעתיק אליו — גם בשורש הכונן ובתיקיות המשתמש שבתיקיית התוכנה; לא בסל, ולא קובץ של התוכנה
                    String why0 = Physical.programFile(f.getAbsolutePath());
                    if (why0 == null && lib != null) why0 = Physical.noWriteInto(lib, f.getParentFile());
                    if (why0 != null) { DbListCmd.lastFailed = true; return why0; }
                    if (f.isDirectory()) { DbListCmd.lastFailed = true; return Words.DbCmd.WRITE_IS_FOLDER; }
                    boolean was = f.isFile(), toTrash = was && !add && lib != null;
                    if (was && !add && !replace) {                              // מבנה 110: נוצר בינתיים — לא מחליפים בלי הוראה
                        DbListCmd.lastFailed = true;
                        return Words.Cmd.fileTaken(f.getName());
                    }
                    if (toTrash) {                                              // מבנה 46: הקובץ הקודם — לסל, לא נדרס
                        String why = Physical.oldToTrash(lib, f);
                        if (why != null) { DbListCmd.lastFailed = true; return why; }
                    }
                    Files.writeAtomic(f, add && was ? Files.readUtf8(f) + text : text);
                    if (toTrash) {
                        lib.fileAdded(f);
                        lib.flush(5000);
                        return Words.DbCmd.written(f.getName(), text.length(), false) + Words.DbCmd.OLD_TO_TRASH;
                    }
                    if (lib != null) {
                        if (was) lib.fileRemoved(f.getAbsolutePath());
                        lib.fileAdded(f);
                        lib.flush(5000);   // מ0.45: שורה שאחריה (מחק, העבר) — מול הגודל החדש, לא "הקובץ השתנה מאז הסריקה"
                    }
                    return Words.DbCmd.written(f.getName(), text.length(), add && was);
                } catch (Throwable t) {
                    Log.e("Batch.write", t);
                    DbListCmd.lastFailed = true;
                    return Words.Db.failed(String.valueOf(t.getMessage()));
                }
            }
        };
        return true;
    }

    /**
     * מבנה 80: קובץ חדש מהמסך — "כתוב "%יעד%\%שם-חדש%" "%תוכן%"": בתיקייה שנבחרה, בשם ובתוכן שהמסך מסר (נלקחים מהבחירה ואינם
     * מפוענחים). שלא כמו "כתוב" כתובה (קובץ קיים — הקודם עובר לסל; התיקייה חייבת להיות קיימת) — כאן, כמו שהמסך עשה: קובץ קיים
     * הוא סירוב, ותיקייה שחסרה נוצרת. הסירובים — בתכנון ובנוסח של המסך.
     */
    /**
     * מבנה 110 (ה6, המשתמש: "כן" — כמו בתיקייה): הכלל האחד של "כתוב" על קובץ שכבר קיים, לשורה הכתובה ולקובץ החדש מהמסך.
     * בלי מתג (או /תפוס:לעצור) — שגיאה, והקובץ נעצר; להחליף — הקודם לסל; לשנות-שם — בשם פנוי "(2)"; לדלג — השורה אינה עושה דבר.
     * "/הוסף" — אין כאן שם תפוס. מחזיר מה לעשות; W_STOP — השורה כבר סגורה (שגיאה או דילוג).
     */
    static final int W_NEW = 0, W_REPLACE = 1, W_FREE = 2, W_STOP = 3;

    static int writeTaken(Batch.Step s, File f, boolean append, String taken, Sim sim) {
        if (append || !sim.isFile(f)) return W_NEW;
        if (Syntax.TAKEN_REPLACE.equals(taken)) return W_REPLACE;
        if (Syntax.TAKEN_RENAME.equals(taken)) return W_FREE;
        if (Syntax.TAKEN_SKIP.equals(taken)) {
            s.noop = true;
            s.dest = f;
            s.describe = Words.Cmd.fileSkipped(f.getName());
            return W_STOP;
        }
        s.error = Words.Cmd.fileTaken(f.getName());
        return W_STOP;
    }

    static void writeSelected(final Batch.Step s, File marked, boolean misuse, String taken, Sim sim) {
        final Batch.Selection sel = sim.selected;
        File parent = marked.getParentFile();
        if (misuse || sel == null || sel.name == null || sel.content == null || sel.dest == null || !marked.getName().equals(Batch.NAME_TOKEN)
                || parent == null || !parent.getAbsolutePath().equals(Paths.clean(sel.dest))) {
            s.error = Words.DbCmd.WRITE_USAGE;
            return;
        }
        String bad = Physical.badName(sel.name);
        if (bad != null) { s.error = bad; return; }
        File f0 = new File(parent, sel.name.trim());
        if (Physical.programFile(f0.getAbsolutePath()) != null) { s.error = Words.Disk.NO_FILE_IN_APP; return; }
        if (sim.isDir(f0)) { s.error = Words.Disk.ALREADY_NAME + f0.getName() + "\""; return; }
        int t = writeTaken(s, f0, false, taken, sim);                          // מבנה 110: אותו כלל כמו בשורה הכתובה
        if (t == W_STOP) return;
        if (t == W_FREE) f0 = sim.free(f0);
        final File f = f0;
        final boolean replace = t == W_REPLACE;
        // מבנה 111 (ה6ב): כמו בשורה הכתובה — התיקייה חייבת להיות קיימת (מי שצריך — "צור תיקייה" לפני; המנהל עושה זאת למסך)
        if (!sim.isDir(parent)) { s.error = Words.DbCmd.writeNoFolder(Bidi.path(parent.getAbsolutePath())); return; }
        if (sim.lib != null && Physical.noWriteInto(sim.lib, parent) != null) { s.error = Physical.noWriteInto(sim.lib, parent); return; }
        sim.gone.remove(f.getAbsolutePath());
        sim.added.put(f.getAbsolutePath(), Batch.GHOST);
        s.dest = f;
        s.describe = Words.DbCmd.writeWill(f.getName(), sel.content.length(), false) + (replace ? Words.DbCmd.OLD_TO_TRASH_WILL : "");
        s.op = new ListCmd.Op() {
            @Override public String run(Library lib) {
                boolean was = f.isFile();
                String why = Physical.newFile(lib, f, sel.content, replace);
                if (why != null) { DbListCmd.lastFailed = true; return why; }
                return Words.DbCmd.written(f.getName(), sel.content.length(), false) + (was ? Words.DbCmd.OLD_TO_TRASH : "");
            }
        };
    }

    /**
     * מ0.45: "שחזר מהסל "<מסלול מקורי>"" — מה שנמחק מהמסלול הזה (האחרון, אם נמחק כמה פעמים) חוזר למקומו, כמו "שחזור" במסך הסל
     * (Physical.restore: שם תפוס — "(משוחזר)"; לא חוזר לרשימות). מה שנמחק בשורות שלפני עוד אינו בסל בהכנה — ולכן לא שגיאה
     * בהכנה אלא "ייבדק בהרצה", ושם: אין — כישלון.
     */
    static void planUntrash(final Batch.Step s, List<String> tk, File here, Map<String, String> vars, Sim sim) {
        if (tk.size() != 2) { s.error = Words.DbCmd.UNTRASH_USAGE; return; }
        String[] err = {null};
        final String p = Parse.path(tk.get(1), here, vars, err);
        if (p == null) { s.error = err[0] != null ? err[0] : Words.DbCmd.UNTRASH_USAGE; return; }
        if (p.startsWith(Batch.SELECTED_MARK)) {                                      // מבנה 81: מה שנבחר במסך הסל
            if (p.equals(Batch.SELECTED_MARK)) trashSelected(s, sim, true);
            else s.error = Words.Cmd.SELECTED_ONLY_SOURCE;
            return;
        }
        final File f = new File(p);
        boolean now = false;
        if (sim.lib != null && sim.lib.ready)
            try {
                now = trashed(sim.lib, p, false).size() > 0;
            } catch (Throwable t) {
                Log.e("Batch.plan: שחזר מהסל", t);
            }
        s.target = f.getName();
        s.dest = f;
        s.describe = Words.DbCmd.untrashWill(f.getName(), now);
        sim.gone.remove(p);
        if (!sim.isFile(f) && !sim.isDir(f)) sim.added.put(p, Batch.GHOST);
        s.op = new ListCmd.Op() {
            @Override public String run(Library lib) {
                if (lib == null) return s.describe;
                lib.flush(5000);                                                   // מה שנמחק הרגע — נכנס לאינדקס של הסל
                List<Trash.Item> its = trashed(lib, p, false);
                if (its.isEmpty()) { DbListCmd.lastFailed = true; return Words.DbCmd.notInTrash(f.getName()); }
                Trash.Item last = its.get(0);
                for (Trash.Item it : its) if (newer(it.name(), last.name())) last = it;
                Physical.Result r = Physical.restore(lib, java.util.Collections.singletonList(last));
                if (r.failed > 0 || r.done.isEmpty()) {
                    DbListCmd.lastFailed = true;
                    return Words.Db.failed(r.note);
                }
                String to = r.to.get(last.path());
                return Words.DbCmd.untrashed(f.getName(), to != null && !to.equals(p) ? new File(to).getName() : null);
            }
        };
    }

    /** מ0.45: "רוקן סל" — כל הסל; "רוקן סל "<מסלול מקורי>"" — מה שנמחק מהמסלול הזה (או מתוכו). Physical.deleteForever. */
    static void planPurge(final Batch.Step s, List<String> tk, File here, Map<String, String> vars, Sim sim) {
        if (tk.size() > 2) { s.error = Words.DbCmd.PURGE_USAGE; return; }
        final String p;
        if (tk.size() == 2) {
            String[] err = {null};
            p = Parse.path(tk.get(1), here, vars, err);
            if (p == null) { s.error = err[0] != null ? err[0] : Words.DbCmd.PURGE_USAGE; return; }
            if (p.startsWith(Batch.SELECTED_MARK)) {                                  // מבנה 81: מה שנבחר במסך הסל
                if (p.equals(Batch.SELECTED_MARK)) trashSelected(s, sim, false);
                else s.error = Words.Cmd.SELECTED_ONLY_SOURCE;
                return;
            }
            s.target = new File(p).getName();
            s.describe = Words.DbCmd.purgeWill(new File(p).getName());
        } else {
            p = null;
            s.describe = Words.DbCmd.PURGE_ALL_WILL;
        }
        s.op = new ListCmd.Op() {
            @Override public String run(Library lib) {
                if (lib == null) return s.describe;
                lib.flush(5000);
                List<Trash.Item> its = p == null ? Trash.list(lib) : trashed(lib, p, true);
                if (its.isEmpty()) return p == null ? Words.DbCmd.purged(0, 0) : Words.DbCmd.notInTrash(new File(p).getName());
                Physical.Result r = Physical.deleteForever(lib, its);
                if (r.failed > 0) DbListCmd.lastFailed = true;
                return Words.DbCmd.purged(r.done.size(), r.failed);
            }
        };
    }

    /**
     * מבנה 81: שחזור או מחיקה לצמיתות של פריטים שנבחרו במסך הסל. הבחירה היא המסלולים של הפריטים **בתוך הסל** (הפריט עצמו — ולא
     * "מה שנמחק מהמסלול הזה", כמו בשורה כתובה: במסך בוחרים פריט מסוים, גם כזה שהמקום המקורי שלו אינו ידוע). בהרצה הפריטים
     * נמצאים מחדש בסל לפי המסלול — מסלול שאינו פריט בסל פשוט אינו נמצא, כך שמחיקה לצמיתות אינה יכולה לגעת במה שמחוץ לסל.
     * פריט שכבר אינו בסל — נספר ככישלון. ההדמיה אינה יודעת לאן כל פריט חוזר (שורה אחת, מהמסך).
     */
    static void trashSelected(final Batch.Step s, Sim sim, final boolean restore) {
        Batch.Selection sel = sim.selected;
        if (sel == null || (sel.files.isEmpty() && sel.dirs.isEmpty())) { s.error = Words.Cmd.NOTHING_SELECTED; return; }
        final List<String> picked = new ArrayList<String>(sel.files);
        picked.addAll(sel.dirs);
        s.describe = restore ? Words.DbCmd.untrashPickedWill(picked.size()) : Words.DbCmd.purgePickedWill(picked.size());
        s.op = new ListCmd.Op() {
            @Override public String run(Library lib) {
                if (lib == null) return s.describe;
                java.util.Set<String> want = new java.util.HashSet<String>(picked);
                List<Trash.Item> its = new ArrayList<Trash.Item>();
                for (Trash.Item it : Trash.list(lib)) if (want.contains(it.path())) its.add(it);
                Physical.Result r = restore ? Physical.restore(lib, its) : Physical.deleteForever(lib, its);
                for (int k = its.size(); k < picked.size(); k++) r.fail(Words.DbCmd.GONE_FROM_TRASH);
                s.result = r;
                if (r.failed > 0) DbListCmd.lastFailed = true;
                return (restore ? Words.DbCmd.untrashedPicked(r.done.size(), r.failed) : Words.DbCmd.purged(r.done.size(), r.failed))
                        + (r.failed > 0 && !r.note.isEmpty() ? " (" + r.note + ")" : "");
            }
        };
    }

    /**
     * מה שנמחק אחרי: השם בסל מתחיל בזמן המחיקה (עד השנייה), ובאותה שנייה נוסף לשם "(2)", "(3)"… (Physical.freeName) — הקצר
     * קודם, ובאורך שווה — לפי הסדר.
     */
    static boolean newer(String a, String b) {
        int k = Math.min(19, Math.min(a.length(), b.length()));
        int c = a.substring(0, k).compareTo(b.substring(0, k));
        if (c != 0) return c > 0;
        if (a.length() != b.length()) return a.length() > b.length();
        return a.compareTo(b) > 0;
    }

    /** מה שבסל ונמחק מהמסלול p (under — גם מתוך התיקייה p). */
    static List<Trash.Item> trashed(Library lib, String p, boolean under) {
        List<Trash.Item> out = new ArrayList<Trash.Item>();
        for (Trash.Item it : Trash.list(lib))
            if (it.original != null && (it.original.equals(p) || (under && it.original.startsWith(p + "/")))) out.add(it);
        return out;
    }
}
