package com.tikiyot.vfm;

// פעולות פיזיות — מחלקה אחת (80.1): מחיקה מהכונן, העברה לתיקייה, שחזור מהסל, מחיקה לצמיתות.
// כל פעולה כאן עוברת תמיד את אותה דרך: חלון האישור בתבנית הקבועה → הביצוע → האינדקס בשני
// הקצוות (דרך התור) → הרשימות → ההודעה בסוף, אחרי שהאינדקס כבר מראה את השינוי.
// אסור לו: לבנות מסכים או פסים (המרשם והמסכים עושים זאת).

import android.app.Activity;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Physical {

    private Physical() { }

    /** מה קרה בפעולה. */
    // ---------- 142, 149: הדבקה מהלוח — מקום אחד (מנהל הקבצים והרשימה החכמה) ----------

    /** מה שבלוח — אל dir, עם חלון האישור. after — אחרי הביצוע (גם אם נכשל). */
    static void paste(final Activity a, File dir, final Runnable after) {
        if (dir == null || Clip.isEmpty()) return;
        String why = Clip.cannotPasteInto(dir);
        if (why != null) {
            Ui.toast(a, why);
            return;
        }
        final boolean copy = Clip.isCopy();
        if (!copy) {
            String no = cannotMove(Library.get(a), Clip.files(), Clip.dirs());
            if (no != null) {
                Ui.toast(a, no);
                return;
            }
        }
        askTransferInto(a, new ArrayList<String>(Clip.files()), new ArrayList<String>(Clip.dirs()), dir, copy, Clip.warning(),
                new Done() {
                    @Override
                    public void done(Result r) {
                        if (r.failed == 0 || !r.done.isEmpty()) Clip.clear();
                        if (after != null) after.run();
                    }
                });
    }

    // ---------- 146: לאן עבר — כדי שמסך יציב ישאיר את השורה במקומה ----------

    /** מעברים אחרונים בתוכנה: מסלול קודם ← מסלול חדש (העברה, שינוי שם, מחיקה לסל, שחזור). לא העתקה. */
    private static final Map<String, String> MOVES = new LinkedHashMap<String, String>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> e) {
            return size() > 5000;
        }
    };

    static void moved(String from, String to) {
        synchronized (MOVES) {
            MOVES.put(from, to);
        }
    }

    /**
     * איפה הקובץ עכשיו, אחרי מה שנעשה בו בתוכנה — גם כמה פעולות ברצף, וגם כשהועברה התיקייה שהוא בתוכה.
     * לא זז — אותו מסלול.
     */
    static String whereNow(String path) {
        synchronized (MOVES) {
            String p = path;
            for (int step = 0; step < 50; step++) {
                String n = MOVES.get(p);
                if (n == null)
                    for (int i = p.lastIndexOf('/'); i > 0 && n == null; i = p.lastIndexOf('/', i - 1)) {
                        String dir = MOVES.get(p.substring(0, i));               // התיקייה שמעליו הועברה
                        if (dir != null) n = dir + p.substring(i);
                    }
                if (n == null || n.equals(p)) break;
                p = n;
            }
            return p;
        }
    }

    public static final class Result {
        /** המסלולים (המקוריים) שהפעולה הצליחה עליהם — קבצים ותיקיות שנבחרו. */
        public final List<String> done = new ArrayList<String>();
        /** מקור → יעד (במחיקה: המקום בסל; בהעברה ובשחזור: המקום החדש). */
        public final Map<String, String> to = new LinkedHashMap<String, String>();
        public int failed;
        /** כמה פריטים ברשימות הושפעו. */
        public int lists;
        /** הסיבה לכישלון הראשון, אם היה. */
        public String note = "";
        /** מתוך done: כמה תיקיות, וכמה קבצים היו בתוכן (82). */
        public int dirs, inner;
        /** המשתמש ביטל או עצר באמצע. */
        public boolean cancelled;

        void fail(String why) {
            failed++;
            if (note.isEmpty()) note = why;
        }
    }

    public interface Done {
        void done(Result r);
    }

    // ================= שם תפוס ביעד (82) =================

    public enum Choice { REPLACE, RENAME, MERGE, CANCEL }

    /** התשובה לשאלה "שם תפוס": מה לעשות, השם האחר, והאם אותו דבר לכל השאר. */
    public static final class Answer {
        public final Choice choice;
        public final String name;
        public final boolean all;

        public Answer(Choice choice, String name, boolean all) {
            this.choice = choice;
            this.name = name;
            this.all = all;
        }
    }

    /** שואל את המשתמש (נקרא בחוט הרקע וממתין לתשובה). suggested — השם החופשי הבא. */
    public interface Asker {
        Answer ask(File existing, File incoming, String suggested);
    }

    /** התקדמות בהעתקה ובהעברה, ו"עצור". */
    public interface Progress {
        void step(int files, int total, long bytes, long totalBytes);

        boolean stopped();
    }

    /** השם החופשי הבא בתיקייה: "ספר (2).pdf", "ספרים (2)". */
    public static String freeName(File dir, String name, boolean isDir) {
        String base = isDir ? name : Text.stem(name), ext = isDir ? "" : Text.tail(name);   // 124
        if (base.isEmpty()) { base = name; ext = ""; }
        String n = name;
        for (int i = 2; new File(dir, n).exists(); i++) n = base + " (" + i + ")" + ext;
        return n;
    }

    // ================= מה אסור (82) =================

    /** למה אי אפשר למחוק או להעביר את התיקייה הזו, או null אם מותר. */
    public static String forbidden(Library lib, File dir) {
        String p = dir.getAbsolutePath();
        String root = lib.rootOf(p);
        if (root != null && (p.equals(root) || (p + "/").equals(root))) return Words.Disk.DRIVE_ROOT;
        for (Index ix : lib.indexes) if (ix.rootPath.equals(p)) return Words.Disk.DRIVE_ROOT;
        if (root != null && (p + "/").startsWith(new File(root, Index.TRASH).getAbsolutePath() + "/"))
            return Words.Disk.IS_TRASH;
        String app = appDir();
        if (app != null && ((app + "/").startsWith(p + "/") || p.startsWith(app + "/"))) return Words.Disk.IS_APP_FOLDER;
        return null;
    }

    /**
     * 90: קובץ של התוכנה עצמה (מבנה.txt, קובצי ההגדרות, הגיבויים, האינדקס) — אסור למחוק או להעביר,
     * ואסור להעביר או להעתיק לתוך התיקייה שלה. מחיקה של מבנה.txt מחקה את כל הרשימות.
     */
    public static String programFile(String path) {
        String app = appDir();
        if (app == null || !(path.equals(app) || path.startsWith(app + "/"))) return null;
        for (String u : Paths.USER_DIRS)                                         // 1.25.6: מה שהמשתמש יצר — שלו
            if (path.startsWith(app + "/" + u + "/")) return null;
        return Words.Disk.IS_APP_FILE;
    }

    private static String appDir() {
        try {
            return Paths.data().getAbsolutePath();
        } catch (Throwable t) {
            return null;
        }
    }

    /** האם folder היא src עצמה או תיקייה שבתוכה. */
    static boolean inside(File folder, File src) {
        String f = folder.getAbsolutePath() + "/", s = src.getAbsolutePath() + "/";
        return f.startsWith(s);
    }

    // ================= הביצוע (בלי מסכים — נבדק בבדיקות) =================

    private static String stamp() {
        return new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", java.util.Locale.US).format(new java.util.Date());
    }

    /** מחיקה מהכונן של קבצים בלבד (כמו קודם). */
    public static Result delete(Library lib, List<String> paths, boolean grey) {
        return delete(lib, paths, java.util.Collections.<String>emptyList(), grey);
    }

    /**
     * מחיקה מהכונן: העברה ל".נמחקו" שבשורש אותו כונן, רישום ביומן המחיקות, האינדקס בשני הקצוות.
     * תיקייה עוברת כולה, ושחזור מחזיר את כולה (82). grey=false: הפריטים יורדים מהרשימות; true: נשארים באפור (58).
     */
    public static Result delete(Library lib, List<String> paths, List<String> dirs, boolean grey) {
        Result r = new Result();
        String stamp = stamp();
        List<String> gone = new ArrayList<String>();
        for (String p : paths) {
            String own = programFile(p);
            if (own != null) {
                r.fail(own);
                continue;
            }
            Library.Hit h = lib.find(p);
            File f = new File(p);
            if (h == null || h.rec == null || h.rec.gone) {
                r.fail(Words.Disk.NOT_IN_INDEX_REFRESH);
                continue;
            }
            if (!f.isFile()) {
                r.fail(Words.Disk.FILE_GONE_REFRESH);
                continue;
            }
            if (h.rec.size != f.length()) {
                r.fail(Words.Disk.FILE_CHANGED_REFRESH);
                continue;
            }
            String t = toTrash(lib, f, stamp, r);
            if (t == null) continue;
            gone.add(p);
            r.done.add(p);
            r.to.put(p, t);
            moved(p, t);
        }
        for (String p : dirs) {
            File f = new File(p);
            if (!f.isDirectory()) {
                r.fail(Words.Disk.FOLDER_GONE_REFRESH);
                continue;
            }
            String why = forbidden(lib, f);
            if (why != null) {
                r.fail(why);
                continue;
            }
            List<String> rel = Trash.relFiles(f);
            String t = toTrash(lib, f, stamp, r);
            if (t == null) continue;
            for (String x : rel) gone.add(p + "/" + x);
            r.done.add(p);
            r.to.put(p, t);
            moved(p, t);
            r.dirs++;
            r.inner += rel.size();
        }
        if (!grey && !gone.isEmpty()) r.lists = Tree.removeEverywhere(gone);
        return r;
    }

    /** קובץ או תיקייה אל ".נמחקו" של אותו כונן. מחזיר את המקום בסל, או null. */
    private static String toTrash(Library lib, File f, String stamp, Result r) {
        String root = lib.rootOf(f.getAbsolutePath());
        if (root == null) {
            r.fail(Words.Disk.DRIVE_NOT_IN_INDEX);
            return null;
        }
        boolean isDir = f.isDirectory();
        File dir = new File(root, Index.TRASH);
        dir.mkdirs();
        File target = new File(dir, stamp + " " + f.getName());
        if (target.exists())                                   // אותו שם באותה שנייה — תוספת לשם, לא לזמן
            target = new File(dir, freeName(dir, target.getName(), isDir));
        if (!f.renameTo(target)) {
            Log.i("מחיקה נכשלה: " + f);
            r.fail(Words.DELETE_FAILED);
            return null;
        }
        Trash.logDeleted(target, f.getAbsolutePath());
        if (isDir) {
            lib.dirChanged(f.getParent());
            lib.dirChanged(dir.getAbsolutePath());
        } else {
            lib.fileRemoved(f.getAbsolutePath());
            lib.fileAdded(target);
        }
        Log.i("נמחק: " + f);
        return target.getAbsolutePath();
    }

    /** העברה של קבצים (כמו קודם): שם תפוס ביעד — לא נדרס. */
    public static Result move(Library lib, List<String> paths, File folder) {
        return transfer(lib, paths, java.util.Collections.<String>emptyList(), folder, false, null, null, true);
    }

    /**
     * העברה (copy=false) או העתקה (copy=true) לתיקייה, של קבצים ותיקיות, גם בין כוננים (82).
     * שם תפוס — asker שואל (null: לא נוגעים, ונרשם ככישלון). בין כוננים — קובץ אחרי קובץ: מועתק, נבדק,
     * ורק אז נמחק מהמקור; כך "עצור" או הפסקת חשמל לא משאירים מצב שבור.
     * הרשימות: בהעברה — עוקבות; בהעתקה — העותק לא נכנס. קובץ שהוחלף — הפריטים נשארים ומצביעים על החדש.
     */
    public static Result transfer(Library lib, List<String> files, List<String> dirs, File folder, boolean copy,
                                  Asker asker, Progress progress, boolean grey) {
        return transfer(lib, files, dirs, folder, copy, asker, progress, grey, null);
    }

    /** 1.19.0: עם יומן — כל קובץ שהגיע ליעד נרשם בו (null — בלי). */
    public static Result transfer(Library lib, List<String> files, List<String> dirs, File folder, boolean copy,
                                  Asker asker, Progress progress, boolean grey, Journal journal) {
        Run run = new Run(lib, copy, asker, progress, grey);
        run.journal = journal;
        run.journalRoot = folder;
        List<File> all = topOnly(files, dirs);
        List<File> to = new ArrayList<File>();
        for (int k = 0; k < all.size(); k++) to.add(folder);
        go(run, all, to, folder);
        run.finish();
        return run.r;
    }

    /**
     * 1.19.0: העתקה או העברה ששומרת מסלול (כמו XCOPY /S): כל פריט נכתב ב־destRoot + המסלול שלו יחסית ל־root.
     * תיקיות היעד נוצרות; תיקייה שכבר קיימת ביעד — ממוזגת (בלי שאלה על התיקייה עצמה). פריט שאינו בתוך root —
     * כישלון. בהעברה — תיקיות המקור שהתרוקנו (מתחת ל־root) נמחקות.
     */
    public static Result transferKeep(Library lib, String root, List<String> files, List<String> dirs, File destRoot,
                                      boolean copy, Asker asker, Progress progress, boolean grey, Journal journal) {
        return transferKeep(lib, root, files, dirs, destRoot, copy, asker, progress, grey, journal, true);
    }

    /** 1.20.0: pruneEmpty=false — בהעברה לא מוחקים תיקיות מקור שהתרוקנו (מקור מתוך השוואה: הן קיימות בצד השני). */
    public static Result transferKeep(Library lib, String root, List<String> files, List<String> dirs, File destRoot,
                                      boolean copy, Asker asker, Progress progress, boolean grey, Journal journal,
                                      boolean pruneEmpty) {
        Run run = new Run(lib, copy, asker, progress, grey);
        run.keep = true;
        run.journal = journal;
        run.journalRoot = destRoot;
        String base = new File(root).getAbsolutePath();
        List<File> all = new ArrayList<File>(), to = new ArrayList<File>();
        for (File f : topOnly(files, dirs)) {
            String rel = rel(base, f.getAbsolutePath());
            if (rel == null || rel.isEmpty()) {
                run.r.fail(Words.Disk.NOT_UNDER_ROOT);
                continue;
            }
            File parent = new File(destRoot, rel).getParentFile();
            all.add(f);
            to.add(parent);
        }
        java.util.Set<String> emptied = new java.util.TreeSet<String>(java.util.Collections.reverseOrder());
        if (!copy && pruneEmpty)
            for (File f : all)
                for (File up = f.getParentFile(); up != null && rel(base, up.getAbsolutePath()) != null
                        && !up.getAbsolutePath().equals(base); up = up.getParentFile())
                    emptied.add(up.getAbsolutePath());
        go(run, all, to, destRoot);
        for (String d : emptied) run.removeIfEmpty(new File(d));     // מהעמוק אל הרדוד
        run.finish();
        return run.r;
    }

    /** 1.21.0: כל קובץ אל התיקייה שלו (folders[i]) — לשחזור, כשהקבצים באים מכמה שורשים. */
    public static Result transferTo(Library lib, List<String> files, List<String> folders, File checkFolder, boolean copy,
                                    Asker asker, Progress progress, boolean grey, Journal journal) {
        Run run = new Run(lib, copy, asker, progress, grey);
        run.keep = true;
        run.journal = journal;
        run.journalRoot = checkFolder;
        List<File> all = new ArrayList<File>(), to = new ArrayList<File>();
        for (int k = 0; k < files.size(); k++) {
            all.add(new File(files.get(k)));
            to.add(new File(folders.get(k)));
        }
        go(run, all, to, checkFolder);
        run.finish();
        return run.r;
    }

    /** המסלול של p יחסית ל־base ("" — הוא עצמו), או null אם אינו בתוכו. */
    static String rel(String base, String p) {
        if (p.equals(base)) return "";
        String b = base.endsWith("/") ? base : base + "/";
        return p.startsWith(b) ? p.substring(b.length()) : null;
    }

    /** הביצוע עצמו: all[i] אל תוך to[i]. הבדיקות (תיקיית התוכנה, סל, מקום, גודל) — לפי checkFolder. */
    private static void go(Run run, List<File> all, List<File> to, File checkFolder) {
        boolean copy = run.copy;
        Library lib = run.lib;
        for (File f : all) {
            if (f.isDirectory()) {
                long[] st = count(f);
                run.total += st[0];
                run.totalBytes += st[1];
            } else {
                run.total++;
                run.totalBytes += f.length();
            }
        }
        if (programFile(checkFolder.getAbsolutePath()) != null) {
            run.r.fail(Words.Disk.DEST_IS_APP);
            run.r.failed = all.size();
            return;
        }
        String droot = lib.rootOf(checkFolder.getAbsolutePath());
        if (droot != null && inside(checkFolder, new File(droot, Index.TRASH))) {
            run.r.fail(Words.Disk.DEST_IS_TRASH);
            run.r.failed = all.size();
            return;
        }
        long need = run.needed(all, checkFolder);
        if (need > 0 && free(checkFolder) < need) {
            run.r.fail(Words.Disk.NO_ROOM_NEED + Files.size(need) + Words.Disk.NO_ROOM_FREE + Files.size(free(checkFolder)) + ")");
            run.r.failed = all.size();
            return;
        }
        checkFolder.mkdirs();
        run.maxFile = maxFileSize(checkFolder);
        for (int k = 0; k < all.size(); k++) {
            if (run.r.cancelled) break;
            File f = all.get(k), folder = to.get(k);
            String p = f.getAbsolutePath();
            boolean isDir = f.isDirectory();
            if (!f.exists()) {
                run.r.fail(Words.Disk.NOT_FOUND_REFRESH);
                continue;
            }
            if (!copy && programFile(p) != null) {
                run.r.fail(programFile(p));
                continue;
            }
            if (isDir && inside(folder, f)) {
                run.r.fail(Words.Disk.NOT_INTO_ITSELF);
                continue;
            }
            if (isDir && !copy) {
                String why = forbidden(lib, f);
                if (why != null) {
                    run.r.fail(why);
                    continue;
                }
            }
            if (!copy && folder.getAbsolutePath().equals(f.getParent())) {
                run.r.fail(Words.Disk.ALREADY_HERE);
                continue;
            }
            if (!folder.isDirectory() && !folder.mkdirs() && !folder.isDirectory()) {
                run.r.fail(Words.Disk.NO_DEST_FOLDER);
                continue;
            }
            int before = run.filesDone;
            String dst = run.transfer(f, folder, true);
            if (dst == null) continue;
            run.r.done.add(p);
            run.r.to.put(p, dst);
            if (!copy) moved(p, dst);
            if (isDir) {
                run.r.dirs++;
                run.r.inner += run.filesDone - before;
            }
        }
    }

    private static long[] count(File dir) {
        long[] s = {0, 0};
        File[] all = dir.listFiles();
        if (all == null) return s;
        for (File f : all) {
            if (f.isDirectory()) {
                long[] c = count(f);
                s[0] += c[0];
                s[1] += c[1];
            } else {
                s[0]++;
                s[1] += f.length();
            }
        }
        return s;
    }

    /**
     * 1.18.0: רק הפריטים העליונים — פריט שתיקייה שמעליו כבר ברשימה יוצא (הוא עובר איתה), וכפילויות יוצאות.
     * בלי זה: תיקייה עוברת שלמה, ואחריה הקובץ שבתוכה "לא נמצא" (בהעברה) או מועתק שוב, שטוח, ליעד (בהעתקה).
     */
    static List<File> topOnly(List<String> files, List<String> dirs) {
        java.util.LinkedHashSet<String> in = new java.util.LinkedHashSet<String>();
        for (String p : files) in.add(new File(p).getAbsolutePath());
        for (String p : dirs) in.add(new File(p).getAbsolutePath());
        List<File> out = new ArrayList<File>();
        for (String p : in) {
            boolean under = false;
            for (File up = new File(p).getParentFile(); up != null && !under; up = up.getParentFile())
                under = in.contains(up.getAbsolutePath());
            if (!under) out.add(new File(p));
        }
        return out;
    }

    /** לבדיקות: גודל קובץ מרבי ביעד במקום מה שנקרא ממערכת הקבצים (0 — בלי). */
    static long maxFileForTests = 0;
    static final long FAT_MAX = 0xFFFFFFFFL;   // FAT32: עד 4GB פחות בית

    /** 1.18.0: הקובץ הגדול ביותר שאפשר לכתוב בתיקייה — FAT32: 4GB פחות בית; אחרת (או לא ידוע) — בלי הגבלה. */
    static long maxFileSize(File folder) {
        if (maxFileForTests > 0) return maxFileForTests;
        String t = fsType(folder.getAbsolutePath());
        return t != null && (t.equals("vfat") || t.equals("msdos") || t.equals("fat")) ? FAT_MAX : Long.MAX_VALUE;
    }

    /** סוג מערכת הקבצים לפי /proc/mounts: נקודת העגינה הארוכה ביותר שמכילה את המסלול. בכרטיס (fuse) — לפי /mnt/media_rw. */
    static String fsType(String path) {
        List<String[]> m = new ArrayList<String[]>();
        java.io.BufferedReader in = null;
        try {
            in = new java.io.BufferedReader(new java.io.FileReader("/proc/mounts"));
            String line;
            while ((line = in.readLine()) != null) {
                String[] f = line.split(" ");
                if (f.length >= 3) m.add(new String[]{f[1].replace("\\040", " "), f[2]});
            }
        } catch (Throwable t) {
            return null;
        } finally {
            try { if (in != null) in.close(); } catch (Throwable ignored) { }
        }
        return fsTypeIn(m, path);
    }

    /** מתוך רשימת עגינות {נקודה, סוג}. */
    static String fsTypeIn(List<String[]> m, String path) {
        String t = typeAt(m, path);
        if (t != null && (t.equals("fuse") || t.equals("sdcardfs")) && path.startsWith("/storage/")) {
            List<String[]> rw = new ArrayList<String[]>();
            for (String[] x : m) if (x[0].startsWith("/mnt/media_rw/")) rw.add(x);
            String under = typeAt(rw, "/mnt/media_rw/" + path.substring("/storage/".length()));
            if (under != null) return under;
        }
        return t;
    }

    static String typeAt(List<String[]> mounts, String path) {
        String best = null, type = null;
        for (String[] x : mounts) {
            String mp = x[0];
            boolean in = path.equals(mp) || path.startsWith(mp.endsWith("/") ? mp : mp + "/");
            if (in && (best == null || mp.length() > best.length())) {
                best = mp;
                type = x[1];
            }
        }
        return type;
    }

    private static long free(File folder) {
        File f = folder;
        while (f != null && !f.exists()) f = f.getParentFile();
        return f == null ? Long.MAX_VALUE : f.getUsableSpace();
    }

    /** ריצה אחת של העברה/העתקה: מה נזכר ("אותו דבר לכל השאר"), ההתקדמות, והשינויים לאינדקס ולרשימות. */
    private static final class Run {
        final Library lib;
        final boolean copy, grey;
        final Asker asker;
        final Progress progress;
        final Result r = new Result();
        final String stamp = stamp();
        Answer forFiles, forDirs;
        int total, filesDone;
        long totalBytes, bytesDone;
        /** 1.18.0: הקובץ הגדול ביותר שהיעד מקבל (FAT32). */
        long maxFile = Long.MAX_VALUE;
        /** 1.19.0: שמירת מסלול — תיקייה שכבר קיימת ביעד ממוזגת בלי שאלה. */
        boolean keep;
        /** 1.19.0: יומן — כל קובץ שהגיע ליעד, במסלולו יחסית ל־journalRoot. */
        Journal journal;
        File journalRoot;
        /** בהעברה: מקור → יעד לכל קובץ, לרשימות. */
        final Map<String, String> moved = new LinkedHashMap<String, String>();
        /** קבצים שהיו ביעד והוחלפו (עברו לסל). */
        final List<String> replaced = new ArrayList<String>();

        Run(Library lib, boolean copy, Asker asker, Progress progress, boolean grey) {
            this.lib = lib;
            this.copy = copy;
            this.asker = asker;
            this.progress = progress;
            this.grey = grey;
        }

        /** כמה מקום צריך ביעד: בהעתקה — הכול; בהעברה — רק מה שעובר בין כוננים. */
        long needed(List<File> all, File folder) {
            long n = 0;
            String to = lib.rootOf(folder.getAbsolutePath());
            for (File f : all) {
                if (!copy && to != null && to.equals(lib.rootOf(f.getAbsolutePath()))) continue;
                n += f.isDirectory() ? count(f)[1] : f.length();
            }
            return n;
        }

        private boolean stop() {
            if (!r.cancelled && progress != null && progress.stopped()) r.cancelled = true;
            return r.cancelled;
        }

        private void tick(long bytes) {
            filesDone++;
            bytesDone += bytes;
            if (progress != null) progress.step(filesDone, total, bytesDone, totalBytes);
        }

        /** src אל תוך dir. top — פריט שנבחר (לא תוכן של תיקייה). מחזיר את המקום החדש, או null. */
        String transfer(File src, File dir, boolean top) {
            if (stop()) return null;
            boolean isDir = src.isDirectory();
            File dst = new File(dir, src.getName());
            if (dst.equals(src)) {                             // העתקה לאותה תיקייה: עותק בשם חדש, בלי לשאול
                dst = new File(dir, freeName(dir, src.getName(), isDir));
            } else if (keep && isDir && dst.isDirectory()) {     // 1.19.0: התיקייה כבר קיימת ביעד — ממזגים
                merge(src, dst);
                return dst.getAbsolutePath();
            } else if (dst.exists()) {
                boolean both = isDir && dst.isDirectory();
                Answer a = both ? forDirs : forFiles;
                String sug = freeName(dir, src.getName(), isDir);
                if (a == null) {
                    if (asker == null) {
                        r.fail(Words.NAME_TAKEN_AT_DEST);
                        return null;
                    }
                    a = asker.ask(dst, src, sug);
                    if (a == null) a = new Answer(Choice.CANCEL, null, false);
                    if (a.all) {
                        if (both) forDirs = a; else forFiles = a;
                    }
                }
                switch (a.choice) {
                    case CANCEL:
                        r.cancelled = true;
                        return null;
                    case MERGE:
                        if (both) {
                            merge(src, dst);
                            return dst.getAbsolutePath();
                        }
                        // קובץ מול תיקייה — אין מיזוג; שם אחר
                    case RENAME: {
                        String n = a.all || a.name == null || a.name.trim().isEmpty() || a.name.contains("/")
                                ? sug : a.name.trim();
                        dst = new File(dir, n);
                        if (dst.exists()) dst = new File(dir, freeName(dir, n, isDir));
                        break;
                    }
                    case REPLACE: {
                        if (inside(src, dst)) {
                            r.fail(Words.Disk.CANT_REPLACE_PARENT);
                            return null;
                        }
                        if (dst.isDirectory()) for (String x : Trash.relFiles(dst)) replaced.add(dst + "/" + x);
                        else replaced.add(dst.getAbsolutePath());
                        if (toTrash(lib, dst, stamp, r) == null) return null;
                        break;
                    }
                }
            }
            return place(src, dst) ? dst.getAbsolutePath() : null;
        }

        /** מיזוג תיקייה אל תיקייה קיימת: כל פריט בנפרד, ושם תפוס — שאלה. */
        private void merge(File src, File dst) {
            File[] kids = src.listFiles();
            if (kids != null) for (File k : kids) {
                if (stop()) break;
                transfer(k, dst, false);
            }
            if (!copy) removeIfEmpty(src);
        }

        private void removeIfEmpty(File d) {
            String[] left = d.list();
            if (left != null && left.length == 0 && d.delete()) lib.dirChanged(d.getParent());
        }

        /** src למקום dst (שכבר פנוי). */
        private boolean place(File src, File dst) {
            if (src.isDirectory()) {
                if (!copy) {
                    List<String> rel = Trash.relFiles(src);
                    long bytes = count(src)[1];
                    List<Library.Hit> was = new ArrayList<Library.Hit>();
                    if (same(src, dst)) for (String x : rel) was.add(lib.find(src + "/" + x));
                    if (same(src, dst) && src.renameTo(dst)) {  // אותו כונן: רגע אחד
                        lib.dirChanged(src.getParent());
                        lib.dirChanged(dst.getParent());
                        for (int k = 0; k < rel.size(); k++) {
                            String x = rel.get(k);
                            moved.put(src + "/" + x, dst + "/" + x);
                            carry(was.get(k), dst + "/" + x);
                            if (journal != null) journal.file(journalRoot, new File(dst, x));
                        }
                        filesDone += rel.size() - 1;
                        tick(bytes);
                        return true;
                    }
                }
                if (!dst.mkdirs() && !dst.isDirectory()) {
                    r.fail(Words.Disk.NO_DEST_FOLDER);
                    return false;
                }
                File[] kids = src.listFiles();
                if (kids != null) for (File k : kids) {
                    if (stop()) break;
                    transfer(k, dst, false);
                }
                if (!copy) removeIfEmpty(src);
                lib.dirChanged(dst.getAbsolutePath());
                return true;
            }
            long len = src.length();
            if (len > maxFile && !(!copy && same(src, dst))) {
                r.fail(Words.Disk.TOO_BIG_FOR_DEST);
                return false;
            }
            Library.Hit was = copy ? null : lib.find(src.getAbsolutePath());
            boolean ok = copy ? copyFile(src, dst) : moveFile(src, dst, same(src, dst));
            if (!ok) {
                r.fail(copy ? Words.COPY_FAILED : Words.MOVE_FAILED);
                return false;
            }
            if (!copy) {
                lib.fileRemoved(src.getAbsolutePath());
                moved.put(src.getAbsolutePath(), dst.getAbsolutePath());
            }
            lib.fileAdded(dst);
            if (!copy) carry(was, dst.getAbsolutePath());
            if (journal != null) journal.file(journalRoot, dst);
            tick(len);
            return true;
        }

        /** 1.19.0: "נוסף" (והחתימה) עוברים בהעברה לפי המקור המדויק — לא לפי ניחוש גודל+תאריך. */
        private void carry(Library.Hit was, String to) {
            if (was != null && was.rec != null && was.rec.added > 0)
                lib.carry(to, was.rec.added, was.rec.sig, was.rec.size, was.rec.mtime);
        }

        /** אותו כונן (לפי האינדקס): אפשר שינוי שם. בין כוננים — תמיד קובץ אחרי קובץ. */
        private boolean same(File a, File b) {
            String x = lib.rootOf(a.getAbsolutePath()), y = lib.rootOf(b.getAbsolutePath());
            return x == null || y == null || x.equals(y);
        }

        /** בסוף: הרשימות. קובץ שהוחלף ונשאר במקומו — הפריטים נשארים; שלא נשאר — כמו מחיקה (58). */
        void finish() {
            if (!moved.isEmpty()) r.lists += Tree.replaceEverywhere(moved);
            if (!grey && !replaced.isEmpty()) {
                List<String> gone = new ArrayList<String>();
                for (String p : replaced) if (!new File(p).exists()) gone.add(p);
                if (!gone.isEmpty()) r.lists += Tree.removeEverywhere(gone);
            }
        }
    }

    /** העברת קובץ למקום פנוי: שינוי שם, ובין כוננים — העתקה, בדיקה, ורק אז מחיקה מהמקור. */
    static boolean moveFile(File src, File dst, boolean sameDrive) {
        if (dst.exists() || !src.isFile()) return false;
        if (sameDrive && src.renameTo(dst)) return true;
        if (!copyFile(src, dst)) return false;
        if (!src.delete()) {
            dst.delete();
            return false;
        }
        return true;
    }

    /** 1.18.0: סיומת הקובץ הזמני של העתקה — נכתב בשם זה, ורק בסוף מקבל את השם הסופי. */
    static final String PARTIAL = ".vfmtmp";  // בכונן

    /** השם הזמני של dst: מוסתר, ליד היעד. שם ארוך מדי (מעל 255 בתים) — שם קצר לפי גיבוב. */
    static File partial(File dst) {
        String n = "." + dst.getName() + PARTIAL;
        try {
            if (n.getBytes("UTF-8").length > 255) n = "." + Integer.toHexString(dst.getName().hashCode()) + PARTIAL;
        } catch (java.io.UnsupportedEncodingException ignored) {
        }
        return new File(dst.getParentFile(), n);
    }

    /** האם זה קובץ זמני של העתקה שלא הושלמה. */
    static boolean isPartial(String name) {
        return name.startsWith(".") && name.endsWith(PARTIAL);
    }

    /**
     * העתקת קובץ למקום פנוי, עם בדיקת גודל. 1.18.0: נכתב לשם זמני ורק בסוף מקבל את השם הסופי — הפסקה
     * באמצע (כרטיס שנשלף, מכשיר שכבה) לא משאירה קובץ שבור בשם האמיתי. שארית זמנית מניסיון קודם — נמחקת.
     */
    static boolean copyFile(File src, File dst) {
        if (dst.exists() || !src.isFile()) return false;
        File parent = dst.getParentFile();
        if (parent != null) parent.mkdirs();
        File tmp = partial(dst);
        if (tmp.exists() && !tmp.delete()) return false;
        try {
            java.io.InputStream in = new java.io.FileInputStream(src);
            java.io.FileOutputStream out = new java.io.FileOutputStream(tmp);
            try {
                byte[] buf = new byte[1 << 16];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                out.getFD().sync();
            } finally {
                in.close();
                out.close();
            }
            if (tmp.length() != src.length() || dst.exists() || !tmp.renameTo(dst)) {
                tmp.delete();
                return false;
            }
            dst.setLastModified(src.lastModified());
            return true;
        } catch (Throwable t) {
            tmp.delete();
            Log.e("העתקה", t);
            return false;
        }
    }

    /** שחזור מהסל למקום המקורי. שם תפוס → "(משוחזר)", והרשימות עוקבות (65). לא מחזיר לרשימות (החלטה). */
    public static Result restore(Library lib, List<Trash.Item> items) {
        Result r = new Result();
        for (Trash.Item it : items) {
            if (it.original == null) {
                r.fail(Words.Disk.NO_ORIGINAL_PLACE);
                continue;
            }
            if (BinBox.isBox(it.original)) {                         // 1.10.0: רשימה / כלי / אופן שהוסרו מהתוכנה
                String made = BinBox.restore(lib, it);
                if (made == null) r.fail(Words.RESTORE_FAILED);
                else r.done.add(it.path());
                continue;
            }
            String dst = Trash.restore(lib, it);
            if (dst == null) {
                r.fail(Words.RESTORE_FAILED);
                continue;
            }
            r.done.add(it.path());
            r.to.put(it.path(), dst);
            moved(it.path(), dst);
        }
        return r;
    }

    /** מחיקה לצמיתות מהסל. פריטים שנשארו באפור ברשימות יורדים מהן. */
    public static Result deleteForever(Library lib, List<Trash.Item> items) {
        Result r = new Result();
        List<String> origs = new ArrayList<String>();
        int n = Trash.empty(lib, items, origs);
        for (int i = 0; i < n; i++) r.done.add("");
        r.failed = items.size() - n;
        if (r.failed > 0) r.note = Words.DELETE_FAILED;
        if (!origs.isEmpty()) r.lists = Tree.removeEverywhere(origs);
        return r;
    }

    // ================= ניסוחים — במקום אחד =================

    static String what(List<String> names) {
        return names.size() == 1 ? "\"" + names.get(0) + "\"" : names.size() + Words.Disk.FILES_SUFFIX;
    }

    /**
     * ההודעה בסוף: "נמחק קובץ אחד" / "נמחקו 3 קבצים · הוסרו 2 פריטים ברשימות".
     * עם תיקיות (82): "נמחקה תיקייה אחת (214 קבצים)" / "הועברו: 2 קבצים · תיקייה אחת (30 קבצים)".
     */
    static String message(String verb, String listsVerb, Result r) {
        int dirs = r.dirs, n = r.done.size() - dirs;
        String one = verb.substring(0, verb.length() - 1);        // נמחקו → נמחק
        StringBuilder sb = new StringBuilder();
        String inner = r.inner > 0 ? " (" + Words.files(r.inner) + ")" : "";
        String d = Words.folders(dirs);
        if (dirs == 0) sb.append(n == 1 ? one + Words.Disk.ONE_FILE_SUFFIX : verb + " " + n + Words.Disk.FILES_SUFFIX);
        else if (n == 0) sb.append(dirs == 1 ? one + Words.Disk.FEM_SUFFIX : verb + " ").append(d).append(inner);
        else sb.append(verb).append(": ").append(Words.files(n)).append(" · ").append(d).append(inner);
        if (r.lists > 0) sb.append(" · ").append(listsVerb).append(" ").append(Words.items(r.lists)).append(Words.Disk.IN_LISTS_SUFFIX);
        if (r.failed > 0) sb.append(" · ").append(r.failed).append(Words.Disk.NOT_SUFFIX).append(r.note);
        if (r.cancelled) sb.append(Words.Disk.STOPPED_MIDWAY);
        return sb.toString();
    }

    /** "התיקייה ו־214 הקבצים שבה" / "2 קבצים ותיקייה אחת" — לחלונות האישור. */
    static String whatAll(List<String> files, List<String> dirs) {
        List<String> all = new ArrayList<String>(names(files));
        all.addAll(names(dirs));
        if (all.size() == 1) return "\"" + all.get(0) + "\"";
        String d = Words.folders(dirs.size());
        if (files.isEmpty()) return d;
        if (dirs.isEmpty()) return files.size() + Words.Disk.FILES_SUFFIX;
        return Words.files(files.size()) + Words.Disk.AND + d;
    }

    private static List<String> names(List<String> paths) {
        List<String> out = new ArrayList<String>();
        for (String p : paths) out.add(new File(p).getName());
        return out;
    }

    // ================= עם חלון אישור והודעה (למסכים) =================

    /** מחיקה מהכונן, בחלון האישור בתבנית הקבועה. warning — אזהרה נוספת (למשל "לא יישאר אף עותק"). */
    public static void askDelete(final Activity a, final List<String> paths, String warning, final Done done) {
        askDelete(a, paths, java.util.Collections.<String>emptyList(), warning, done);
    }

    /** מחיקה מהכונן של קבצים ותיקיות (82). */
    public static void askDelete(final Activity a, final List<String> paths, final List<String> dirs,
                                 String warning, final Done done) {
        final Library lib = Library.get(a);
        for (String p : paths)
            if (programFile(p) != null) {                             // 90
                Ui.toast(a, Words.Disk.CANT_DELETE + new File(p).getName() + "\": " + programFile(p));
                return;
            }
        long size = 0;
        int known = 0, inner = 0;
        List<String> all = new ArrayList<String>(paths);
        for (String p : paths) {
            Library.Hit h = lib.find(p);
            if (h != null && h.rec != null) {
                size += h.rec.size;
                known++;
            }
        }
        for (String d : dirs) {
            String why = forbidden(lib, new File(d));
            if (why != null) {
                Ui.toast(a, Words.Disk.CANT_DELETE + new File(d).getName() + "\": " + why);
                return;
            }
            long[] st = lib.treeStats(d);
            inner += st[0];
            size += st[1];
            known++;
            all.addAll(lib.filesUnder(d));
        }
        if (known == 0) {
            Ui.toast(a, Words.Disk.FILES_NOT_IN_INDEX);
            return;
        }
        final boolean grey = Acts.deletedStaysGrey();
        int inLists = Tree.countEverywhere(all);
        String lists = inLists == 0 ? "" : grey ? Words.Disk.LISTS_MARK_DELETED : Words.Disk.LISTS_REMOVE_FROM + Words.items(inLists) + Words.Disk.IN_LISTS_SUFFIX;
        String files;
        if (dirs.isEmpty()) files = paths.size() == 1 ? Words.Disk.FILE_TO_TRASH : Words.Disk.FILES_TO_TRASH;
        else if (paths.isEmpty() && dirs.size() == 1)
            files = Words.Disk.FOLDER_AND + (inner == 1 ? Words.Disk.ITS_FILE : inner + Words.Disk.ITS_FILES) + Words.Disk.GO_TO_TRASH;
        else files = Words.Disk.ALL_INCLUDING + Words.files(inner) + Words.Disk.IN_FOLDERS_TO_TRASH;
        String undo = dirs.isEmpty() ? Words.Disk.UNDO_FROM_TRASH : Words.Disk.UNDO_FOLDER_FROM_TRASH;
        Ui.ask(a, Words.Disk.DELETE_TITLE + whatAll(paths, dirs), files,
                Files.size(size) + Words.Disk.SPACE_ON_EMPTY + (warning == null ? "" : "\n" + warning) + namesOf(paths, dirs, 12),   // 141; 102: מתי המקום מתפנה
                undo + lists, Words.Disk.DELETE_ON_DRIVE, new Runnable() {
                    @Override
                    public void run() {
                        perform(a, Words.Disk.DELETED, Words.Disk.REMOVED, done, new Op() {
                            @Override public Result go() { return delete(lib, paths, dirs, grey); }
                        });
                    }
                });
    }


    /** העברה (copy=false) או העתקה (copy=true) של קבצים ותיקיות לתיקייה (82). */
    /**
     * 141: השמות שבחלון האישור — עד max, ואחריהם "ועוד N". תיקיות קודם (📁). כך רואים על מה מאשרים,
     * כמו בתצוגה המקדימה של קובצי הפקודות.
     */
    static String namesOf(List<String> paths, List<String> dirs, int max) {
        StringBuilder sb = new StringBuilder();
        int shown = 0, total = paths.size() + dirs.size();
        for (String d : dirs) {
            if (shown == max) break;
            sb.append("\n📁 ").append(new File(d).getName());
            shown++;
        }
        for (String p : paths) {
            if (shown == max) break;
            sb.append("\n· ").append(new File(p).getName());
            shown++;
        }
        if (total > shown) sb.append("\n· ").append(Words.andMore(total - shown));
        return sb.toString();
    }

    /** 142: האם אפשר להעביר את המקורות האלה (קובצי התוכנה, שורש, סל). null = אפשר; אחרת ההסבר. */
    public static String cannotMove(Library lib, List<String> paths, List<String> dirs) {
        for (String p : paths)
            if (programFile(p) != null) return Words.Disk.CANT_MOVE + new File(p).getName() + "\": " + programFile(p);
        for (String d : dirs) {
            String why = forbidden(lib, new File(d));
            if (why != null) return Words.Disk.CANT_MOVE + new File(d).getName() + "\": " + why;
        }
        return null;
    }

    /**
     * העתקה או העברה אל תיקייה ידועה — אחרי בחירה בבורר, או "הדבקה כאן" (142). כל הבדיקות של היעד,
     * המקום, ואז חלון האישור בתבנית (5).
     */
    public static void askTransferInto(final Activity a, final List<String> paths, final List<String> dirs,
                                       final File folder, final boolean copy, final String warning, final Done done) {
        final Library lib = Library.get(a);
        if (programFile(folder.getAbsolutePath()) != null) {      // 90
            Ui.toast(a, Words.Disk.DEST_APP_NOT);
            return;
        }
        String droot = lib.rootOf(folder.getAbsolutePath());
        if (droot != null && inside(folder, new File(droot, Index.TRASH))) {
            Ui.toast(a, Words.Disk.DEST_TRASH_NOT);
            return;
        }
        for (String d : dirs)
            if (inside(folder, new File(d))) {
                Ui.toast(a, Words.Disk.CANT + (copy ? Words.Disk.TO_COPY : Words.Disk.TO_MOVE) + Words.Disk.FOLDER_INTO_ITSELF);
                return;
            }
        long size = 0;
        int inner = 0;
        List<String> all = new ArrayList<String>(paths);
        for (String p : paths) size += new File(p).length();
        for (String d : dirs) {
            long[] st = lib.treeStats(d);
            inner += st[0];
            size += st[1];
            all.addAll(lib.filesUnder(d));
        }
        boolean across = false;
        String to = lib.rootOf(folder.getAbsolutePath());
        for (String p : all.isEmpty() ? dirs : all)
            if (to == null || !to.equals(lib.rootOf(p))) across = true;
        long need = copy || across ? size : 0;
        long free = free(folder);
        if (need > 0 && free < need) {
            Ui.ask(a, Words.Disk.NO_ROOM_IN + folder.getName() + "\"", Words.NO_TOUCH_FILES,
                    Words.Disk.NEED + Files.size(need) + Words.Disk.FREE + Files.size(free), null, Words.Disk.GOT_IT, new Runnable() {
                        @Override public void run() { }
                    });
            return;
        }
        String what = whatAll(paths, dirs);
        String files = copy ? Words.Disk.SOURCE_UNCHANGED
                : (paths.size() + dirs.size() == 1 ? (dirs.isEmpty() ? Words.Disk.FILE_MOVES : Words.Disk.FOLDER_MOVES)
                : Words.Disk.ALL_MOVES) + Words.Disk.ON_DRIVE_TO + folder.getName();
        String detail = folder.getAbsolutePath() + " · " + Files.size(size)
                + (inner > 0 ? " · " + inner + Words.Disk.FILES_IN_FOLDERS : "")
                + (across && !copy ? Words.Disk.ACROSS_DRIVES : "")
                + (warning == null ? "" : "\n" + warning)
                + namesOf(paths, dirs, 12);                                       // 141
        int inLists = copy ? 0 : Tree.countEverywhere(all);
        String undo = copy ? Words.Disk.COPIES_NOT_IN_LISTS
                : (inLists > 0 ? Words.Disk.LISTS_FOLLOW_OPEN + Words.items(inLists) + ")" : Words.Disk.LISTS_FOLLOW)
                + Words.Disk.TAKEN_ASK;
        final boolean grey = Acts.deletedStaysGrey();
        Ui.ask(a, (copy ? Words.Disk.COPY_TO : Words.Disk.MOVE_TO) + folder.getName() + "\": " + what, files, detail, undo,
                copy ? Words.COPY : Words.MOVE, new Runnable() {
                    @Override
                    public void run() {
                        final Busy busy = new Busy(a, copy ? Words.Disk.COPYING : Words.Disk.MOVING);
                        perform(a, copy ? Words.Disk.COPIED : Words.Disk.MOVED, copy ? Words.Disk.REMOVED : Words.Disk.UPDATED, done, new Op() {
                            @Override
                            public Result go() {
                                try {
                                    return transfer(lib, paths, dirs, folder, copy, new DialogAsker(a), busy, grey);
                                } finally {
                                    busy.close();
                                }
                            }
                        });
                    }
                });
    }

    // ---------- חלון "שם תפוס" (82) ----------

    /** שואל במסך וממתין לתשובה בחוט הרקע. */
    static final class DialogAsker implements Asker {
        final Activity a;

        DialogAsker(Activity a) {
            this.a = a;
        }

        @Override
        public Answer ask(final File existing, final File incoming, final String suggested) {
            final Answer[] out = {null};
            final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
            a.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        show(existing, incoming, suggested, out, latch);
                    } catch (Throwable t) {
                        Log.e("חלון שם תפוס", t);
                        latch.countDown();
                    }
                }
            });
            try {
                latch.await();
            } catch (InterruptedException e) {
                return new Answer(Choice.CANCEL, null, false);
            }
            return out[0] != null ? out[0] : new Answer(Choice.CANCEL, null, false);
        }

        private void show(File existing, File incoming, String suggested, final Answer[] out,
                          final java.util.concurrent.CountDownLatch latch) {
            final boolean dirs = existing.isDirectory() && incoming.isDirectory();
            android.widget.LinearLayout box = Ui.form(a);
            String where = existing.getParentFile() != null ? existing.getParentFile().getName() : "";
            android.widget.TextView t = Ui.label(a, "\"" + existing.getName() + Words.Disk.ALREADY
                    + (existing.isDirectory() ? Words.Disk.EXISTS_FEM : Words.Disk.EXISTS) + Words.Disk.IN_QUOTE + where + "\"");
            t.setTextSize(17);
            t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            box.addView(t);
            box.addView(Ui.label(a, Words.Disk.EXISTING + describe(existing) + Words.Disk.INCOMING + describe(incoming)));
            box.addView(Ui.label(a, Words.Disk.OTHER_NAME_LABEL));
            final android.widget.EditText name = Ui.field(a, suggested, "");
            box.addView(name);
            final android.widget.CheckBox all = Ui.check(a, Words.Disk.SAME_FOR_REST, false);
            box.addView(all);
            final android.app.AlertDialog[] dlg = {null};
            final String[] labels = dirs ? new String[]{Words.Disk.MERGE, Words.Disk.DELETE_PREVIOUS_FEM, Words.Disk.OTHER_NAME, Words.Disk.CANCEL_SHORT}
                    : new String[]{Words.Disk.DELETE_PREVIOUS, Words.Disk.OTHER_NAME, Words.Disk.CANCEL_SHORT};
            final Choice[] choices = dirs ? new Choice[]{Choice.MERGE, Choice.REPLACE, Choice.RENAME, Choice.CANCEL}
                    : new Choice[]{Choice.REPLACE, Choice.RENAME, Choice.CANCEL};
            for (int i = 0; i < labels.length; i++) {
                final Choice c = choices[i];
                android.widget.Button b = new android.widget.Button(a);
                b.setText(labels[i]);
                b.setAllCaps(false);
                if (c == Choice.REPLACE) b.setTextColor(Actions.WARNING);
                b.setOnClickListener(new android.view.View.OnClickListener() {
                    @Override
                    public void onClick(android.view.View v) {
                        out[0] = new Answer(c, name.getText().toString(), all.isChecked() && c != Choice.CANCEL);
                        if (dlg[0] != null) dlg[0].dismiss();
                        latch.countDown();
                    }
                });
                box.addView(b);
            }
            android.widget.ScrollView sv = new android.widget.ScrollView(a);
            sv.addView(box);
            dlg[0] = new android.app.AlertDialog.Builder(a).setView(sv).setCancelable(false).create();
            dlg[0].show();
        }

        private static String describe(File f) {
            if (f.isDirectory()) {
                long[] c = count(f);
                return Words.Disk.FOLDER_DOT + Files.count(c[0]) + Words.Disk.FILES_DOT + Files.size(c[1]);
            }
            return Files.size(f.length()) + " · " + Ui.date(f.lastModified());
        }
    }

    // ---------- התקדמות ו"עצור" (82) ----------

    /** חלון התקדמות עם "עצור" — להעברה, להעתקה, לפריסה ולאריזה לשליחה. */
    static final class Busy implements Progress {
        final Activity a;
        volatile boolean stop;
        volatile android.app.AlertDialog dlg;
        android.widget.TextView text;
        long last;

        Busy(final Activity a, final String title) {
            this.a = a;
            a.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        text = Ui.label(a, title);
                        int p = Ui.dp(a, 22);
                        text.setPadding(p, p, p, p);
                        dlg = new android.app.AlertDialog.Builder(a).setView(text).setCancelable(false)
                                .setNegativeButton(Words.STOP, new android.content.DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(android.content.DialogInterface d, int w) {
                                        stop = true;
                                        Ui.toast(a, Words.Disk.STOPPING);
                                    }
                                }).create();
                        dlg.show();
                    } catch (Throwable t) {
                        Log.e("חלון התקדמות", t);
                    }
                }
            });
        }

        @Override
        public void step(final int files, final int total, final long bytes, final long totalBytes) {
            long now = System.currentTimeMillis();
            if (now - last < 250 && files < total) return;
            last = now;
            a.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (text != null) text.setText(files + Words.Disk.OF + total + " · "
                            + Files.size(bytes) + Words.Disk.OF + Files.size(totalBytes));
                }
            });
        }

        @Override
        public boolean stopped() {
            return stop;
        }

        void close() {
            a.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (dlg != null && dlg.isShowing()) dlg.dismiss();
                    } catch (Throwable ignored) { }
                }
            });
        }
    }

    public static void askRestore(final Activity a, final List<Trash.Item> list, final Done done) {
        if (list.isEmpty()) return;
        int unknown = 0;
        List<String> shown = new ArrayList<String>();
        for (Trash.Item it : list) {
            if (it.original == null) unknown++;
            shown.add(TrashScreen.shownName(it.name()));
        }
        Ui.ask(a, Words.Disk.RESTORE_TITLE + what(shown), Words.Disk.RESTORE_FILES_BACK,
                list.size() == 1 && list.get(0).original != null ? list.get(0).original
                        : (unknown > 0 ? unknown + Words.Disk.UNKNOWN_STAY : Words.Disk.TAKEN_RESTORED),
                Words.Disk.RESTORE_NOT_LISTS, Words.Disk.RESTORE, new Runnable() {
                    @Override
                    public void run() {
                        perform(a, Words.Disk.RESTORED, Words.Disk.UPDATED, done, new Op() {
                            @Override public Result go() { return restore(Library.get(a), list); }
                        });
                    }
                });
    }

    public static void askDeleteForever(final Activity a, final List<Trash.Item> list, final Done done) {
        if (list.isEmpty()) return;
        long size = 0;
        List<String> shown = new ArrayList<String>();
        for (Trash.Item it : list) {
            size += it.size;
            shown.add(TrashScreen.shownName(it.name()));
        }
        Ui.ask(a, Words.Disk.PURGE_TITLE + what(shown), Words.Disk.PURGE_FILES,
                Files.size(size) + Words.Disk.PURGE_GREY,
                Words.Disk.NO_UNDO, Words.Disk.PURGE, new Runnable() {
                    @Override
                    public void run() {
                        perform(a, Words.Disk.DELETED, Words.Disk.REMOVED, done, new Op() {
                            @Override public Result go() { return deleteForever(Library.get(a), list); }
                        });
                    }
                });
    }

    // ================= שינוי שם בכונן (84) ומחיקה של תיקייה חדשה (106) =================

    /** בדיקת שם לקובץ או לתיקייה בכונן. null = תקין; אחרת ההסבר. */
    public static String badName(String name) {
        String n = name == null ? "" : name.trim();
        if (n.isEmpty()) return Words.NEED_NAME;
        if (n.equals(".") || n.equals("..")) return Words.Disk.BAD_NAME;
        for (char c : new char[]{'/', '\\', ':', '*', '?', '"', '<', '>', '|'})
            if (n.indexOf(c) >= 0) return Words.Disk.NO_CHAR + c;
        if (n.getBytes(java.nio.charset.Charset.forName("UTF-8")).length > 255) return Words.Disk.NAME_TOO_LONG;
        return null;
    }

    /**
     * שינוי שם של קובץ בכונן, באותה תיקייה (84). האינדקס מתעדכן בתור, והרשימות עוקבות אחרי השם החדש.
     * לא מחליף קובץ קיים — שם תפוס נדחה.
     */
    public static Result rename(Library lib, String path, String newName) {
        Result r = new Result();
        String own = programFile(path);
        if (own != null) {
            r.fail(own);
            return r;
        }
        File f = new File(path);
        String bad = badName(newName);
        if (bad != null) {
            r.fail(bad);
            return r;
        }
        String name = newName.trim();
        if (!f.isFile()) {
            r.fail(Words.FILE_NOT_FOUND);
            return r;
        }
        if (name.equals(f.getName())) return r;
        File dst = new File(f.getParentFile(), name);
        boolean onlyCase = name.equalsIgnoreCase(f.getName());
        if (!onlyCase && dst.exists()) {
            r.fail(Words.Disk.ALREADY_IN_FOLDER + name + "\"");
            return r;
        }
        boolean ok;
        if (onlyCase) {                                        // שינוי אותיות בלבד, בכונן שאינו מבדיל
            File tmp = new File(f.getParentFile(), ".שינוי-שם-" + System.nanoTime());  // בכונן
            ok = f.renameTo(tmp) && tmp.renameTo(dst);
        } else {
            ok = f.renameTo(dst);
        }
        if (!ok) {
            r.fail(Words.RENAME_FAILED);
            return r;
        }
        lib.fileRemoved(path);
        lib.fileAdded(dst);
        Map<String, String> moved = new LinkedHashMap<String, String>();
        moved.put(path, dst.getAbsolutePath());
        r.lists = Tree.replaceEverywhere(moved);
        r.done.add(path);
        r.to.put(path, dst.getAbsolutePath());
        moved(path, dst.getAbsolutePath());
        Log.i("שם שונה בכונן: " + path + " ← " + name);
        return r;
    }

    /** שינוי שם של קובץ בכונן: שם חדש, ואז חלון אישור בתבנית של הפעולות הפיזיות (5). */
    public static void askRename(final Activity a, final String path, final Done done) {
        final File f = new File(path);
        if (programFile(path) != null) {
            Ui.toast(a, Words.Disk.CANT_RENAME + f.getName() + "\": " + programFile(path));
            return;
        }
        if (!f.isFile()) {
            Ui.toast(a, Words.FILE_NOT_AT_PATH);
            return;
        }
        Ui.askText(a, Words.RENAME_ON_DRIVE, f.getName(), Words.Disk.FILE_NAME, new Ui.Text1() {
            @Override
            public void ok(String value) {
                final String name = value.trim();
                String bad = badName(name);
                if (bad != null) {
                    Ui.toast(a, bad);
                    return;
                }
                if (name.equals(f.getName())) return;
                if (!name.equalsIgnoreCase(f.getName()) && new File(f.getParentFile(), name).exists()) {
                    Ui.toast(a, Words.Disk.ALREADY_IN_FOLDER + name + "\"");
                    return;
                }
                String oldExt = Text.ext(f.getName()), newExt = Text.ext(name);
                int inLists = Tree.countEverywhere(java.util.Collections.singletonList(path));
                String detail = f.getName() + "\n← " + name + Words.Disk.IN_FOLDER_LINE + f.getParent()
                        + (!oldExt.equals(newExt) ? Words.Disk.EXT_CHANGES + (oldExt.isEmpty() ? Words.Disk.WITHOUT : oldExt)
                        + " ← " + (newExt.isEmpty() ? Words.Disk.WITHOUT : newExt) + Words.Disk.EXT_WARNING : "");
                String undo = (inLists > 0 ? Words.Disk.LISTS_FOLLOW_OPEN + Words.items(inLists) + ")" : Words.Disk.LISTS_FOLLOW)
                        + Words.Disk.CAN_RENAME_AGAIN;
                final Library lib = Library.get(a);
                Ui.ask(a, Words.Disk.RENAME_TITLE + f.getName(), Words.Disk.RENAME_FILE_ITSELF, detail, undo,
                        Words.Disk.RENAME, new Runnable() {
                            @Override
                            public void run() {
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Result x;
                                        try {
                                            x = rename(lib, path, name);
                                        } catch (Throwable t) {
                                            Log.e("שינוי שם בכונן", t);
                                            x = new Result();
                                            x.fail(Words.Disk.ERROR);
                                        }
                                        final Result r = x;
                                        Ui.afterIndex(a, new Runnable() {
                                            @Override
                                            public void run() {
                                                Ui.toast(a, r.failed > 0 ? Words.Disk.NOT_RENAMED + r.note
                                                        : Words.Disk.RENAMED_TO + name + "\""
                                                        + (r.lists > 0 ? Words.Disk.UPDATED_SUFFIX + Words.items(r.lists) + Words.Disk.IN_LISTS_SUFFIX : ""));
                                                if (done != null) done.done(r);
                                            }
                                        });
                                    }
                                }).start();
                            }
                        });
            }
        });
    }

    // ================= פריסת ZIP (116) =================

    /** פריסה של קובץ ZIP לתיקייה חדשה לידו — שינוי בכונן, עם חלון האישור בתבנית (5). */
    public static void askUnzip(final Activity a, final String path, final Done done) {
        final File zip = new File(path);
        if (!zip.isFile()) {
            Ui.toast(a, Words.FILE_NOT_AT_PATH);
            return;
        }
        final File dest = Zip.target(zip);
        if (programFile(dest.getAbsolutePath()) != null) {
            Ui.toast(a, Words.Disk.NO_UNPACK_IN_APP);
            return;
        }
        long[] c = Zip.contents(zip);
        if (c == null) {
            Ui.toast(a, Words.Disk.BAD_ZIP);
            return;
        }
        long free = free(zip.getParentFile());
        if (c[1] > 0 && free < c[1]) {
            Ui.ask(a, Words.Disk.NO_ROOM_UNPACK, Words.NO_TOUCH_FILES,
                    Words.Disk.NEED + Files.size(c[1]) + Words.Disk.FREE + Files.size(free), null, Words.Disk.GOT_IT, new Runnable() {
                        @Override public void run() { }
                    });
            return;
        }
        final Library lib = Library.get(a);
        Ui.ask(a, Words.Disk.UNPACK_TITLE + zip.getName(), Words.Disk.UNPACK_CREATES,
                Words.files(c[0]) + " · " + Files.size(c[1]) + Words.Disk.TO_LINE + dest.getAbsolutePath(),
                Words.Disk.ZIP_STAYS, Words.Disk.UNPACK, new Runnable() {
                    @Override
                    public void run() {
                        final Busy busy = new Busy(a, Words.Disk.UNPACKING);
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                final Zip.Result r = Zip.unpack(zip, dest, busy);
                                busy.close();
                                lib.dirChanged(zip.getParent());           // האינדקס לומד על התיקייה החדשה
                                Log.i("נפרס בכונן: " + zip + " ← " + dest + " · " + r.files + " קבצים");
                                Ui.afterIndex(a, new Runnable() {
                                    @Override
                                    public void run() {
                                        String msg = r.error != null ? r.error
                                                : Words.Disk.UNPACKED + Words.files(r.files) + Words.Disk.TO_QUOTE + dest.getName() + "\""
                                                + (r.skipped > 0 ? " · " + r.skipped + Words.Disk.SKIPPED : "")
                                                + (r.stopped ? Words.Disk.STOPPED_SUFFIX : "");
                                        Ui.toast(a, msg);
                                        if (done != null) done.done(new Result());
                                    }
                                });
                            }
                        }).start();
                    }
                });
    }

    /** 1.11.0: דחיסת תיקייה ל־ZIP לידה (שם פנוי), עם חלון אישור; התיקייה נשארת. */
    public static void askZip(final Activity a, final File dir, final Runnable after) {
        if (!dir.isDirectory()) return;
        if (programFile(dir.getAbsolutePath()) != null) {
            Ui.toast(a, Words.Disk.NO_FILE_IN_APP);
            return;
        }
        final File zip = new File(dir.getParentFile(), freeName(dir.getParentFile(), dir.getName() + ".zip", false));
        long[] m = Zip.measure(dir);
        Ui.ask(a, Words.DiskW.ZIP_TITLE + dir.getName(), Words.DiskW.ZIP_CREATES,
                Words.files(m[0]) + " · " + Files.size(m[1]) + Words.Disk.TO_LINE + zip.getAbsolutePath(),
                null, Words.Disk.CREATE, new Runnable() {
                    @Override
                    public void run() {
                        final Busy busy = new Busy(a, Words.DiskW.ZIPPING);
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                final Zip.Result r = Zip.pack(dir, zip, busy);
                                busy.close();
                                Library.get(a).dirChanged(dir.getParent());
                                Log.i("נדחס: " + dir + " ← " + zip);
                                a.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Ui.toast(a, r.error != null ? r.error : Words.DiskW.ZIPPED + zip.getName());
                                        if (after != null) after.run();
                                    }
                                });
                            }
                        }).start();
                    }
                });
    }

    /** יצירת תיקייה ריקה בכונן. null = הצליח; אחרת ההסבר. */
    public static String makeFolder(Library lib, File parent, String name) {
        String bad = badName(name);
        if (bad != null) return bad;
        if (programFile(new File(parent, name.trim()).getAbsolutePath()) != null) return Words.Disk.NO_FOLDER_IN_APP;
        File made = new File(parent, name.trim());
        if (made.exists()) return Words.Disk.ALREADY_HERE_NAME + name.trim() + "\"";
        if (!made.mkdirs()) return Words.CANT_CREATE_FOLDER;
        lib.dirChanged(parent.getAbsolutePath());               // האינדקס לומד עליה
        Log.i("נוצרה תיקייה בכונן: " + made.getAbsolutePath());
        return null;
    }

    /** קובץ טקסט חדש בכונן (קובץ פקודות, 117א) — עם חלון אישור בתבנית. לא מחליף קובץ קיים. */
    public static void askNewFile(final Activity a, final File f, final String content, final Runnable onMade) {
        String bad = badName(f.getName());
        if (bad != null) {
            Ui.toast(a, bad);
            return;
        }
        if (programFile(f.getAbsolutePath()) != null) {
            Ui.toast(a, Words.Disk.NO_FILE_IN_APP);
            return;
        }
        if (f.exists()) {
            Ui.toast(a, Words.Disk.ALREADY_NAME + f.getName() + "\"");
            return;
        }
        boolean newDir = !f.getParentFile().isDirectory();                     // 140
        Ui.ask(a, Words.Disk.NEW_FILE_TITLE + f.getName(),
                newDir ? Words.Disk.WILL_CREATE_FOLDER + f.getParentFile().getName() + Words.Disk.AND_FILE : Words.Disk.NEW_FILE_CREATED,
                Words.Disk.INSIDE + f.getParent(),
                Words.Disk.CAN_DELETE_IT, Words.Disk.CREATE, new Runnable() {
                    @Override
                    public void run() {
                        try {
                            f.getParentFile().mkdirs();
                            Files.writeAtomic(f, content);
                        } catch (Throwable t) {
                            Log.e("קובץ חדש", t);
                            Ui.toast(a, Words.Disk.CANT_CREATE_FILE);
                            return;
                        }
                        Library.get(a).dirChanged(f.getParent());
                        Log.i("נוצר קובץ בכונן: " + f);
                        onMade.run();
                    }
                });
    }

    /** יצירת תיקייה בכונן, עם חלון אישור בתבנית של הפעולות הפיזיות. onMade — אחרי שנוצרה. */
    public static void askMakeFolder(final Activity a, final File parent, final String name, final Runnable onMade) {
        String bad = badName(name);
        if (bad != null) {
            Ui.toast(a, bad);
            return;
        }
        final String n = name.trim();
        Ui.ask(a, Words.Disk.NEW_FOLDER_TITLE + n, Words.Disk.NEW_EMPTY_FOLDER,
                Words.Disk.INSIDE + parent.getAbsolutePath(), Words.Disk.CAN_DELETE_IT_FEM, Words.Disk.CREATE, new Runnable() {
                    @Override
                    public void run() {
                        String why = makeFolder(Library.get(a), parent, n);
                        if (why != null) {
                            Ui.toast(a, why);
                            return;
                        }
                        Ui.toast(a, Words.Disk.FOLDER_CREATED + n + "\"");
                        onMade.run();
                    }
                });
    }

    private interface Op {
        Result go();
    }

    /** הביצוע ברקע; ההודעה והעדכון במסך — אחרי שהאינדקס כבר מראה את השינוי. */
    private static void perform(final Activity a, final String verb, final String listsVerb, final Done done, final Op op) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final Result[] r = {null};
                try {
                    r[0] = op.go();
                } catch (Throwable t) {
                    Log.e("פעולה פיזית", t);
                    r[0] = new Result();
                    r[0].fail(Words.Disk.ERROR);
                }
                Ui.afterIndex(a, new Runnable() {
                    @Override
                    public void run() {
                        Ui.toast(a, message(verb, listsVerb, r[0]));
                        if (done != null) done.done(r[0]);
                    }
                });
            }
        }).start();
    }

    // ================= 1.19.0: יומן העתקה =================

    /**
     * יומן: קובץ טקסט שכל קובץ שהגיע ליעד נרשם בו מיד, שורה אחרי שורה — המסלול יחסית לשורש היעד, טאב, גודל.
     * הפסקה באמצע משאירה יומן נכון למה שבוצע. נוסף לסוף קובץ קיים. שורות שמתחילות ב־"#" — כותרת.
     */
    public static final class Journal {
        final File file;
        private java.io.OutputStream out;
        private boolean failed;
        int lines;

        public Journal(File file, String title) {
            this.file = file;
            try {
                File d = file.getParentFile();
                if (d != null) d.mkdirs();
                out = new java.io.FileOutputStream(file, true);
                write("# " + title + "\n");
            } catch (Throwable t) {
                failed = true;
                Log.e("יומן", t);
            }
        }

        void file(File root, File f) {
            String r = rel(root.getAbsolutePath(), f.getAbsolutePath());
            if (r == null) r = f.getAbsolutePath();
            write(r + "\t" + f.length() + "\n");
            lines++;
        }

        private void write(String line) {
            if (out == null) return;
            try {
                out.write(line.getBytes("UTF-8"));
                out.flush();
            } catch (Throwable t) {
                failed = true;
                Log.e("יומן", t);
            }
        }

        /** נכשל בפתיחה או בכתיבה. */
        public boolean failed() {
            return failed;
        }

        public void close() {
            if (out == null) return;
            try {
                if (out instanceof java.io.FileOutputStream) ((java.io.FileOutputStream) out).getFD().sync();
                out.close();
            } catch (Throwable t) {
                failed = true;
            }
            out = null;
        }

        /** קריאה: {מסלול יחסי, גודל} לכל שורה (בלי הכותרות). */
        public static List<String[]> read(File f) {
            List<String[]> outList = new ArrayList<String[]>();
            try {
                for (String line : Files.readUtf8(f).split("\n")) {
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    int t = line.lastIndexOf('\t');
                    outList.add(t < 0 ? new String[]{line, ""} : new String[]{line.substring(0, t), line.substring(t + 1)});
                }
            } catch (Throwable ignored) {
            }
            return outList;
        }
    }
}
