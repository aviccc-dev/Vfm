package com.tikiyot.vfm;

// מבנה 164 (סידור תיקיית הפקודות): ההרצה של תכנית מוכנה — שורה אחרי שורה, הדופק (מסך כבוי), מה שהשתנה לאינדקס,
// ההכנה לשליחה, המדידה, והסיווג של שורה (יוצרת / משנה / מוחקת / קוראת). עד כאן ישב בתוך Batch. הכניסה נשארה
// Batch.run — רק המנהל קורא לה (שומר). אחראי: לבצע את מה שתוכנן. אסור לו: לתכנן, ולהחליט אם מריצים.

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class Runner {

    private Runner() { }

    /** קובץ שעוד לא באינדקס (למשל הורדה חדשה) — נכנס אליו לפני שמוחקים אותו (134). */
    static void known(Library lib, List<String> files) {
        boolean any = false;
        for (String p : files)
            if (lib.find(p) == null && new File(p).isFile()) {
                lib.fileAdded(new File(p));
                any = true;
            }
        if (any) lib.flush(5000);
    }

    /**
     * מבנה 55: המערכת של המכשיר עצרה את התוכנה באמצע הרצה — הדוח אומר זאת, ואיזו הגדרה במכשיר פותרת. נבדק במכשיר של המשתמש
     * (14.11.2026): עם החזקת המעבד והשירות הקדמי התוכנה עוד נעצרה 58 שניות מתוך 205 כשהמסך כבוי; אחרי "ללא הגבלות" בהגדרות
     * הסוללה — רצה ברציפות. המשפך: שורה רק כשזה קרה (5 שניות ומעלה).
     */
    static final long HINT_MS = 5000;

    static void stoppedHint(List<String> report, long missedMs) {
        if (missedMs >= HINT_MS) report.add(Words.Loop.stoppedBySystem(missedMs));
    }

    /**
     * מבנה 38: דופק ברקע בזמן הרצה עם "מדידה" — פעימה כל חצי שנייה, לפי השעון הרגיל. פער גדול בין שתי פעימות אומר
     * שכל התוכנה לא רצה: המכשיר ישן, או שהמערכת עצרה אותה (מסך כבוי). כך שורה איטית מסבירה את עצמה —
     * אם הדופק המשיך לפעום, השורה עצמה חיכתה למשהו; אם נעצר — לא השורה אשמה. (נ0.2: שורה אחת — 67.8 שניות, ואי אפשר היה לדעת למה.)
     */
    static final class Pulse implements Runnable {
        static final long EVERY = 500, GAP = 2000;
        private volatile boolean on = true;
        private volatile long missed, screenOff;
        private final Thread thread = new Thread(this, "vfm-pulse");

        Pulse() {
            thread.setDaemon(true);
            thread.start();
        }

        @Override
        public void run() {
            long last = System.currentTimeMillis();
            while (on) {
                try {
                    Thread.sleep(EVERY);
                } catch (InterruptedException e) {
                    return;
                }
                long now = System.currentTimeMillis();
                if (now - last >= GAP) missed += now - last - EVERY;
                if (!Awake.screenOn(Db.app)) screenOff += Math.min(now - last, GAP);   // מבנה 50: כמה זמן המסך היה כבוי (פעימה שהתעכבה — לא נספרת כולה)
                last = now;
            }
        }

        /** מבנה 50: כמה זמן (באלפיות) המסך היה כבוי מאז שהדופק התחיל. */
        long screenOff() {
            return screenOff;
        }

        /** כמה זמן (באלפיות) התוכנה לא רצה מאז שהדופק התחיל. */
        long missed() {
            return missed;
        }

        void stop() {
            on = false;
            thread.interrupt();
        }
    }

    static List<String> run0(Batch.Plan plan, Library lib, Physical.Asker asker, Physical.Progress progress, boolean grey, Batch.Listener listen) {
        List<String> report = new ArrayList<String>(plan.inputs);              // 1.27.0: הקלט — בראש
        plan.asked = progress;                                                   // מבנה 62
        int fails = 0;                                                           // מ0.31: כישלונות בהרצה — מול המותר ("המשך")
        Batch.Step pending = null;                                                     // מ0.37: "ודא שגיאה"
        int pendingFails = 0;
        int sectionFails = 0;                                                    // מ0.45: "ודא תקין" — הכישלונות מתחילת הקטע
        boolean stopped = false;                                                 // מבנה 40: ההרצה נעצרה בגלל כישלונות (ואז ההודעה על כך כבר בדוח)
        if (plan.blocked()) {                                                    // מ0.30: שגיאה בקובץ — שום שורה לא רצה
            plan.trouble = true;
            report.add(Words.DbCmd.notRun(plan.errors));
            report.add(plan.errorText());
            RunReport.write(plan, report);
            return report;
        }
        Loops.Clock clock = new Loops.Clock();                                               // 1.25.0: לולאה — עצירה לפי זמן
        long all0 = System.nanoTime();
        Batch.Step prev = null;
        int mark = 0;
        long t0 = System.nanoTime();
        for (int si = 0; si <= plan.steps.size(); si++) {
            if (prev != null && plan.measure) measureLine(plan, prev, report, mark, t0);   // 1.25.0: מדידה — זמן השורה הקודמת
            if (prev != null && listen != null) listen.after(prev, report.size() > mark ? report.get(report.size() - 1) : null);
            mark = report.size();
            t0 = System.nanoTime();
            prev = null;
            if (pending != null) {                                              // מ0.37: "ודא שגיאה" — השורה הקודמת הייתה צריכה להיכשל
                boolean failed = fails > pendingFails;
                pending.verified = failed;
                if (failed) fails = pendingFails;                                // כישלון צפוי אינו נספר
                report.add(Words.Cmd.line(pending.label(), failed ? Words.DbCmd.EXPECT_OK_RUN : Words.DbCmd.EXPECT_BAD));
                pending = null;
            }
            if (si == plan.steps.size()) break;
            if (fails > plan.allowErrors) {                                      // מ0.31: יותר כישלונות מהמותר — עוצרים כאן
                stopped = true;
                plan.trouble = true;
                report.add(Words.DbCmd.stoppedAt(plan.steps.get(si).label(), fails, plan.allowErrors));
                break;
            }
            Batch.Step s = plan.steps.get(si);
            if (progress != null && progress.stopped()) {
                report.add(Words.Cmd.stoppedBefore(s.line));
                plan.trouble = true;
                break;
            }
            if (clock.skip(plan, s.loops, s.rounds)) {
                if (!clock.notes.isEmpty()) {
                    report.addAll(clock.notes);
                    clock.notes.clear();
                }
                continue;
            }
            prev = s;
            if (s.expectFail) {
                pending = s;
                pendingFails = fails;
            }
            s.ran = s.error == null;
            if (listen != null) listen.before(si, plan.steps.size(), s);
            if (s.error != null) {
                report.add(Words.Cmd.line(s.label(), Words.Cmd.skipped(s.error)));
                plan.trouble = true;
                fails++;
                continue;
            }
            if (s.contains != null) containsAtRun(s);                          // מ0.40: /מכיל: — מול התוכן של עכשיו
            if (s.noop) {                                                       // 1.27.0: גם מה שלא השתנה — נאמר
                if (s.cmd.equals(Syntax.SECTION)) sectionFails = fails;
                report.add(Words.Cmd.line(s.label(), s.describe));
                continue;
            }
            if (s.cmd.equals(Syntax.OK_SINCE)) {                                       // מ0.45: ודא תקין
                boolean ok = fails == sectionFails;
                s.verified = ok;
                report.add(Words.Cmd.line(s.label(), Words.DbCmd.verifyResult(ok, Words.DbCmd.OK_SINCE_WHAT,
                        Words.DbCmd.okSinceGot(fails - sectionFails))));
                continue;
            }
            if (s.cmd.equals("ECHO") || s.cmd.equals("START") || (s.cmd.equals(Syntax.OPEN) && s.op == null)) continue;
            if (s.op != null) {                                                // 1.24.0: רשימות
                fails += runOp(plan, s, lib, report);
                continue;
            }
            if (s.cmd.equals("COMP")) {                                       // 1.20.0: חושב בתכנון — רק בדוח
                report.add(Words.Cmd.line(s.label(), s.describe));
                continue;
            }
            Done d = disk(plan, s, lib, asker, progress, grey);
            fails += d.fails;
            report.add(Words.Cmd.line(s.label(), d.out));
        }
        if (plan.measure) measured(plan, report, (System.nanoTime() - all0) / 1000000);
        // מבנה 40 (ממצא 25): כישלונות שאינם "ודא" — שורה שנכשלה וההרצה המשיכה ("המשך") — נאמרים גם בסיכום, לא רק בשורה שלהם.
        // הכלל של המשתמש: "אין מצב שיש שגיאה והקובץ רץ ואף אחד לא יודע". (נ0.1: "צור אופן … לא נוצר" — התגלה רק בהשוואה בין שני דוחות.)
        if (fails > 0 && !stopped) report.add(Words.DbCmd.failsSummary(fails));
        int ok = 0, bad = 0;                                                     // מ0.14: ודא — כמה עברו וכמה נכשלו
        for (Batch.Step s : plan.steps) if (s.verified != null) { if (s.verified) ok++; else bad++; }
        if (ok + bad > 0) report.add(Words.DbCmd.verifySummary(ok, bad));
        RunReport.write(plan, report);
        return report;
    }

    /** מבנה 177: מה שיצא משורה של הכונן — ההודעה, וכמה כישלונות להוסיף. */
    private static final class Done {
        String out;
        int fails;
    }

    /** מבנה 177 (עד כאן — בתוך הלולאה של run0): פעולה אחת על הכונן — צור תיקייה, מחק, שיתוף, העתק / העבר, שנה שם. */
    private static Done disk(Batch.Plan plan, Batch.Step s, Library lib, Physical.Asker asker, Physical.Progress progress, boolean grey) {
        String out;
        int fails = 0;
        if (s.cmd.equals("MD")) {
            String why = Physical.makeFolder(lib, s.dest.getParentFile(), s.dest.getName());
            out = why == null ? Words.Cmd.created(s.dest.getName()) : why;
            s.result = new Physical.Result();                               // מבנה 79
            if (why == null) s.result.done.add(s.dest.getAbsolutePath());
            else s.result.fail(why);
            if (why != null) { plan.trouble = true; fails++; }
        } else if (s.cmd.equals("RD") || s.cmd.equals("DEL")) {
            if (s.files.isEmpty() && s.dirs.isEmpty()) out = Words.Cmd.NO_FILES;
            else {
                known(lib, s.files);
                Physical.Result r = Physical.delete(lib, s.files, s.dirs, grey);
                s.result = r;
                out = result(r, Words.Cmd.TRASHED);
                if (r.failed > 0) { plan.trouble = true; fails += r.failed; }
            }
        } else if (s.share) {                                          // 203
            if (s.files.isEmpty() && s.dirs.isEmpty()) out = Words.Cmd.NO_FILES;
            else out = stage(plan, s, progress);
        } else if (s.cmd.equals("COPY") || s.cmd.equals("MOVE")) {
            if (s.renameTaken) sameAtRun(s);                                // מ0.40: "יש כבר זהה" — שוב, מול הכונן של עכשיו
            if (s.files.isEmpty() && s.dirs.isEmpty()) {
                out = Batch.notes(s).isEmpty() ? Words.Cmd.NO_FILES : Batch.notes(s).substring(3);
                s.result = new Physical.Result();                           // מבנה 77: לא נעשה דבר — וזו התוצאה, לא תקלה
            } else {
                boolean copy = s.cmd.equals("COPY");
                Journal j = s.journal == null ? null : new Journal(new File(s.journal), s.text);   // 1.19.0
                Physical.Asker ask = s.replace ? Batch.REPLACE_ALL : s.renameTaken ? Batch.RENAME_ALL   // מ0.40: /תפוס:לשנות-שם
                        : s.failTaken ? null : asker;                                    // 1.20.0: /תפוס:לעצור
                Physical.Result r = !s.targets.isEmpty()                           // 1.21.0 שחזור, 1.22.0 רשימה (מ0.40: גם העברה)
                        ? Physical.transferTo(lib, s.files, s.targets, s.dest, copy, ask, progress, grey, j)
                        : s.keepRoot != null
                        ? Physical.transferKeep(lib, s.keepRoot, s.files, s.dirs, s.dest, copy, ask, progress, grey, j,
                                !s.fromCompare)
                        : Physical.transfer(lib, s.files, s.dirs, s.dest, copy, ask, progress, grey, j);
                s.result = r;                                               // מבנה 77
                out = result(r, copy ? Words.Cmd.COPIED : Words.Cmd.MOVED);
                out += Batch.notes(s);
                if (j != null) {
                    j.close();
                    if (j.failed()) {
                        out += Words.Cmd.JOURNAL_FAILED;
                        plan.trouble = true;
                        fails++;
                    }
                }
                if (r.failed > 0) { plan.trouble = true; fails += r.failed; }
            }
        } else if (s.cmd.equals("REN")) {
            Physical.Result r = Physical.rename(lib, s.files.get(0), s.newName);
            s.result = r;                                                   // מבנה 78
            out = result(r, Words.Cmd.RENAMED);
            if (r.failed > 0) { plan.trouble = true; fails += r.failed; }
        } else {
            out = Words.Cmd.NOT_DONE;
            plan.trouble = true;
            fails++;
        }
        Done d = new Done();
        d.out = out;
        d.fails = fails;
        return d;
    }

    /** מבנה 177 (עד כאן — בתוך הלולאה של run0): שורה של רשימות (במסד). מחזירה 1 כשנכשלה. */
    private static int runOp(Batch.Plan plan, Batch.Step s, Library lib, List<String> report) {
        DbListCmd.lastFailed = false;
        String got = s.op.run(lib);
        s.said = got;                                                   // מבנה 85
        s.saidFailed = DbListCmd.lastFailed;
        report.add(Words.Cmd.line(s.label(), got));
        if (s.cmd.equals(Syntax.WRITE) && s.dest != null) {                    // מבנה 80: מה שיצא — גם למסך שהריץ את השורה
            s.result = new Physical.Result();
            if (DbListCmd.lastFailed) s.result.fail(got);
            else s.result.done.add(s.dest.getAbsolutePath());
        }
        if (DbListCmd.lastFailed) {
            plan.trouble = true;
            return 1;
        }
        return 0;
    }

    /** מבנה 177 (עד כאן — בתוך הלולאה של run0): הזמן של השורה הקודמת, ו"חרגה מהזמן". */
    private static void measureLine(Batch.Plan plan, Batch.Step prev, List<String> report, int mark, long t0) {
        prev.ms = (System.nanoTime() - t0) / 1000000;
        if (report.size() > mark) {
            String last = report.get(report.size() - 1) + Words.Loop.took(prev.ms);
            if (plan.lineLimit > 0 && prev.ms > plan.lineLimit) {           // מ0.45: חרגה מהזמן — ❌, ונספרת עם "ודא" שנכשלו
                String head = Words.Cmd.line(prev.label(), "");
                last = last.startsWith(head) ? head + Words.Loop.tooSlow(plan.lineLimit) + last.substring(head.length())
                        : Words.Loop.tooSlow(plan.lineLimit) + last;
                prev.verified = Boolean.FALSE;
                plan.trouble = true;
            }
            report.set(report.size() - 1, last);
        }
    }

    /** 1.26.0: מה שורה עושה — לתצוגה המקדימה ולסיכום. */
    static final int K_CREATE = 0, K_CHANGE = 1, K_DELETE = 2, K_READ = 3;

    static int kind(Batch.Step s) {
        if (s.cmd.equals("MD")) return K_CREATE;
        if (s.cmd.equals("DEL") || s.cmd.equals("RD") || s.cmd.equals(Syntax.PURGE) || s.cmd.equals(Syntax.DROP_MODE)) return K_DELETE;   // מ0.45
        if (s.cmd.equals("COPY") || s.cmd.equals("MOVE") || s.cmd.equals("REN")) return s.share ? K_READ : K_CHANGE;
        return K_READ;
    }

    /** נוגעת בקבצים בכונן (לא ברשימות). */
    static boolean disk(Batch.Step s) {
        return s.op == null && !s.noop && (s.cmd.equals("COPY") || s.cmd.equals("MOVE") || s.cmd.equals("DEL") || s.cmd.equals("RD")
                || s.cmd.equals("MD") || s.cmd.equals("REN"));
    }

    /** 1.25.0: סיכום המדידה, ויומן (שורה לכל שורה בדוח, נוסף בכל ריצה). */
    static void measured(Batch.Plan plan, List<String> report, long total) {
        Batch.Step slow = null;
        for (Batch.Step x : plan.steps) if (x.ms >= 0 && (slow == null || x.ms > slow.ms)) slow = x;
        report.add(Words.Loop.total(total, slow == null ? null : slow.label(), slow == null ? 0 : slow.ms)
                + Words.Loop.notRunning(plan.pulse == null ? 0 : plan.pulse.missed()));
        if (plan.measureJournal == null) return;
        try {
            File j = new File(plan.measureJournal);
            if (j.getParentFile() != null) j.getParentFile().mkdirs();
            java.io.Writer w = new java.io.OutputStreamWriter(new java.io.FileOutputStream(j, true), "UTF-8");
            try {
                w.write("=== " + new java.text.SimpleDateFormat(Syntax.NOW_FORMAT, Locale.ROOT).format(new java.util.Date()) + " · "
                        + Log.version + "\n");
                for (String r : report) w.write(r + "\n");
                w.write("\n");
            } finally {
                w.close();
            }
        } catch (Throwable t) {
            report.add(Words.Loop.JOURNAL_FAILED);
            Log.e("מדידה — יומן", t);
        }
    }

    /**
     * 203: מכין לשליחה — עותק של כל קובץ, ואריזה של כל תיקייה, בתיקיית השליחה. כך שורה מאוחרת יותר (DEL, MOVE)
     * לא פוגעת במה שעוד לא נשלח. אין די מקום — לא מכין, ואומר כמה צריך.
     */
    static String stage(Batch.Plan plan, Batch.Step s, Physical.Progress progress) {
        File dir = stageDir(plan);
        long need = 0;
        for (String f : s.files) need += new File(f).length();
        for (String d : s.dirs) need += Zip.measure(new File(d))[1];
        long free = dir.getUsableSpace();
        if (free < need) return Words.ActsW.NO_ROOM_PACK + Files.size(need) + Words.ActsW.FREE + Files.size(free);
        int done = 0, failed = 0;
        for (String f : s.files) {
            File src = new File(f);
            File to = new File(dir, Physical.freeName(dir, src.getName(), false));
            try {
                Files.copy(src, to);
                plan.shared.add(to);
                done++;
            } catch (Throwable t) {
                Log.e("203: שיתוף בקובץ פקודות", t);
                failed++;
            }
        }
        for (String d : s.dirs) {                                          // 1.10.0: תיקייה — הקבצים שבה כמו שהם
            File src = new File(d);
            List<File> inside = Files.filesIn(src);
            if (inside.size() > Files.MAX_SEND) {                              // יותר מדי לחלון השיתוף — ארוזה בשמה
                File zip = new File(dir, Physical.freeName(dir, src.getName() + ".zip", false));
                Zip.Result r = Zip.pack(src, zip, progress);
                if (r.error == null && !r.stopped) {
                    plan.shared.add(zip);
                    done++;
                } else failed++;
                continue;
            }
            for (File x : inside) {
                File to = new File(dir, Physical.freeName(dir, x.getName(), false));
                try {
                    Files.copy(x, to);
                    plan.shared.add(to);
                    done++;
                } catch (Throwable t) {
                    Log.e("1.10.0: שיתוף תיקייה כקבצים", t);
                    failed++;
                }
            }
        }
        return Words.Cmd.STAGED + " " + done + (failed > 0 ? Words.Cmd.failed(failed) : "");
    }

    /** תיקייה חדשה לכל הרצה, בתוך תיקיית השליחה; ישנות (מעל יום) נמחקות. */
    static File stageDir(Batch.Plan plan) {
        if (plan.stage != null) return plan.stage;
        File root = Files.sendDir();
        File[] old = root.listFiles();
        if (old != null)
            for (File f : old)
                if (f.isDirectory() && f.getName().startsWith(STAGE) && System.currentTimeMillis() - f.lastModified() > 24L * 3600 * 1000)
                    wipe(f);
        plan.stage = new File(root, STAGE + System.currentTimeMillis());
        plan.stage.mkdirs();
        return plan.stage;
    }

    static final String STAGE = "batch-";

    /** מוחק הכנה ישנה — רק בתוך תיקיית השליחה של התוכנה (לא קובצי המשתמש). */
    static void wipe(File f) {
        File[] k = f.listFiles();
        if (k != null) for (File x : k) wipe(x);
        f.delete();
    }

    static String result(Physical.Result r, String verb) {
        String s = verb + " " + r.done.size();
        if (r.failed > 0) s += Words.Cmd.failed(r.failed) + (r.note.isEmpty() ? "" : " (" + r.note + ")");
        return s;
    }

    /**
     * מ0.40: /תפוס:לשנות-שם בהרצה — קובץ שיש בתיקיית היעד שלו קובץ זהה (בכונן עכשיו, או שהגיע בשורה הזו לפניו) יוצא מהשורה.
     * בתכנון זה נבדק מול ההדמיה; אבל לקובץ ששורה קודמת באותו קובץ יוצרת עוד אין תוכן בתכנון, וגם בין התצוגה המקדימה
     * להרצה הכונן יכול להשתנות. המשפך — כמו בתכנון: הגודל קודם, חתימה רק לשווי גודל.
     */
    static void sameAtRun(Batch.Step s) {
        Map<String, List<File>> arriving = new java.util.HashMap<String, List<File>>();
        for (int k = 0; k < s.files.size(); k++) {
            File f = new File(s.files.get(k));
            File dir = !s.targets.isEmpty() ? new File(s.targets.get(k))
                    : s.keepRoot != null ? new File(s.dest, Physical.rel(s.keepRoot, f.getAbsolutePath())).getParentFile() : s.dest;
            List<File> came = arriving.get(dir.getAbsolutePath());
            if (came == null) arriving.put(dir.getAbsolutePath(), came = new ArrayList<File>());
            if (sameOnDisk(f, dir, came)) {
                s.files.remove(k);
                if (!s.targets.isEmpty()) s.targets.remove(k);
                k--;
                s.same++;
            } else came.add(f);
        }
    }

    /** מ0.40: /מכיל: בהרצה — קובץ שלא כתוב בו הטקסט יוצא מהשורה (בתכנון עבר כ"עוד לא ידוע", או שהשתנה מאז). */
    static void containsAtRun(Batch.Step s) {
        for (int k = 0; k < s.files.size(); k++) {
            File f = new File(s.files.get(k));
            boolean ok = true;
            for (byte[] w : s.contains) if (!Files.contains(f, w)) ok = false;
            if (ok) continue;
            s.files.remove(k);
            if (!s.targets.isEmpty()) s.targets.remove(k);
            k--;
        }
    }

    static boolean sameOnDisk(File src, File dir, List<File> came) {
        long n = src.length();
        String sig = null;
        List<File> all = new ArrayList<File>(came);
        File[] in = dir.listFiles();
        if (in != null) all.addAll(Arrays.asList(in));
        for (File g : all) {
            if (!g.isFile() || g.length() != n) continue;
            if (sig == null && (sig = Sig.of(src)) == null) return false;
            if (sig.equals(Sig.of(g))) return true;
        }
        return false;
    }
}
