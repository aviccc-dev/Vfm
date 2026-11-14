package com.tikiyot.vfm;

// פעולות פיזיות — מחלקה אחת (80.1): מחיקה מהכונן, העברה לתיקייה, שחזור מהסל, מחיקה לצמיתות.
// כל פעולה כאן עוברת תמיד את אותה דרך: חלון האישור בתבנית הקבועה → הביצוע → האינדקס בשני
// הקצוות (דרך התור) → הרשימות → ההודעה בסוף, אחרי שהאינדקס כבר מראה את השינוי.
// אסור לו: לבנות מסכים או פסים (המרשם והמסכים עושים זאת).
// מבנה 10 (13.11.2026): חלון האישור, ההתקדמות וההודעה בסוף — בקובץ משלהם במסכים (PhysicalUi); כאן רק הביצוע.

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Physical {

    private Physical() { }

    /** מה קרה בפעולה. */

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
        String p = Paths.clean(dir.getAbsolutePath());                          // מבנה 42: גם "תיקייה/.." הוא השורש
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
     * 90: קובץ של התוכנה עצמה (קובצי ההגדרות, הגיבויים, האינדקס) — אסור למחוק או להעביר,
     * ואסור להעביר או להעתיק לתוך התיקייה שלה.
     */
    public static String programFile(String path) {
        path = Paths.clean(path);                                               // מבנה 42
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
        String f = Paths.clean(folder.getAbsolutePath()) + "/", s = Paths.clean(src.getAbsolutePath()) + "/";   // מבנה 42
        return f.startsWith(s);
    }

    /**
     * מבנה 42: אותו קובץ בכונן, גם כשהמסלולים כתובים אחרת — קישור (‎/sdcard), או אותיות גדולות וקטנות בכונן שאינו מבדיל
     * ביניהן. נשאל רק כשהשם ביעד תפוס (המשפך), לפני שמחליפים: "להחליף" קובץ בעצמו שלח אותו לסל, וההעברה נכשלה.
     */
    static boolean sameFile(File a, File b) {
        if (a.equals(b)) return true;
        try {
            if (a.getCanonicalFile().equals(b.getCanonicalFile())) return true;
        } catch (java.io.IOException e) {
            Log.e("Physical.sameFile", e);
        }
        try {
            return java.nio.file.Files.isSameFile(a.toPath(), b.toPath());      // אנדרואיד 8 ומעלה; לפניו — לפי המסלול בלבד
        } catch (Throwable t) {
            return false;
        }
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
        if (!grey && !gone.isEmpty()) r.lists = listsGone(gone);
        return r;
    }

    // ================= הרשימות אחרי הכונן — במקום אחד (מ0.23) =================
    // בבניית מ — המסד (בכל האופנים) הוא מה שהמשתמש רואה, והמספר בהודעה הוא שלו; עץ הרשימות הישן ממשיך להתעדכן כמו קודם
    // (התיקייה שלו משותפת לתוכנה הישנה, עד המעבר לתיקייה נפרדת — claude/yetzia-mipituach.md).

    static int listsMoved(java.util.Map<String, String> moved) {
        return DbFollow.movedEverywhere(moved);
    }

    static int listsGone(java.util.Collection<String> gone) {
        return DbFollow.goneEverywhere(gone);
    }

    static int listsCount(java.util.Collection<String> paths) {
        return DbFollow.countEverywhere(paths);
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

    /**
     * מבנה 46: הגרסה הקודמת של קובץ שעומדים לכתוב מחדש ("כתוב" בלי /הוסף) — אל הסל של אותו כונן, כמו ב"החלף".
     * המשתמש (14.11.2026), על השאלה אם לשלוח את הישן לסל כמו בשאר התוכנה: "כן". הפריטים שברשימות נשארים — המסלול
     * אותו מסלול, והקובץ החדש נכתב מיד. מחזיר null כשהצליח, או את הסיבה.
     */
    static String oldToTrash(Library lib, File f) {
        Result r = new Result();
        return toTrash(lib, f, stamp(), r) != null ? null : r.note;
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
            String dst = run.transfer(f, folder);
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

    static long[] count(File dir) {
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

    static long free(File folder) {
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

        /** src אל תוך dir. מחזיר את המקום החדש, או null. */
        String transfer(File src, File dir) {
            if (stop()) return null;
            boolean isDir = src.isDirectory();
            File dst = new File(dir, src.getName());
            boolean self = dst.equals(src) || (dst.exists() && sameFile(src, dst));   // מבנה 42: אותו קובץ, גם בכתיב אחר
            if (self && !copy) {                               // העברה אל המקום שהוא כבר בו — לא "להחליף" אותו בעצמו
                r.fail(Words.Disk.ALREADY_HERE);
                return null;
            }
            if (self) {                                        // העתקה לאותה תיקייה: עותק בשם חדש, בלי לשאול
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
                transfer(k, dst);
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
                    transfer(k, dst);
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
            if (!moved.isEmpty()) r.lists += listsMoved(moved);
            if (!grey && !replaced.isEmpty()) {
                List<String> gone = new ArrayList<String>();
                for (String p : replaced) if (!new File(p).exists()) gone.add(p);
                if (!gone.isEmpty()) r.lists += listsGone(gone);
            }
        }
    }

    /** העברת קובץ למקום פנוי: שינוי שם, ובין כוננים — העתקה, בדיקה, ורק אז מחיקה מהמקור. */
    static boolean moveFile(File src, File dst, boolean sameDrive) {
        if (dst.exists() || !src.isFile()) return false;
        if (sameDrive && src.renameTo(dst)) return true;
        String[] sig = new String[1];
        if (!copyFile(src, dst, sig)) return false;
        // מבנה 39 (ממצא 33; המשתמש, 14.11.2026: "להשוות חתימות"): בין כוננים המקור נמחק לצמיתות — ולכן קודם קוראים
        // את העותק מהכונן ומשווים חתימה מלאה של כל התוכן לזו של מה שנקרא מהמקור. שונה — העותק נמחק, והמקור נשאר.
        if (sig[0] == null || !sig[0].equals(digest(dst))) {
            Log.i(Words.Disk.COPY_DIFFERS + dst);
            dst.delete();
            return false;
        }
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
        return copyFile(src, dst, null);
    }

    /** מבנה 39: חתימה מלאה של כל תוכן הקובץ (SHA-1, בהקסה) — להשוואת עותק למקור. null — לא נקרא. */
    static String digest(File f) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-1");
            java.io.InputStream in = new java.io.FileInputStream(f);
            try {
                byte[] buf = new byte[1 << 16];
                int n;
                while ((n = in.read(buf)) > 0) md.update(buf, 0, n);
            } finally {
                in.close();
            }
            return new java.math.BigInteger(1, md.digest()).toString(16);
        } catch (Throwable t) {
            Log.e("חתימת עותק", t);
            return null;
        }
    }

    /** sig — לא null: sig[0] מקבל את החתימה המלאה של מה שנקרא מהמקור בזמן ההעתקה עצמה (בלי קריאה נוספת של המקור). */
    static boolean copyFile(File src, File dst, String[] sig) {
        if (dst.exists() || !src.isFile()) return false;
        File parent = dst.getParentFile();
        if (parent != null) parent.mkdirs();
        File tmp = partial(dst);
        if (tmp.exists() && !tmp.delete()) return false;
        try {
            java.security.MessageDigest md = sig == null ? null : java.security.MessageDigest.getInstance("SHA-1");
            java.io.InputStream in = new java.io.FileInputStream(src);
            java.io.FileOutputStream out = new java.io.FileOutputStream(tmp);
            try {
                byte[] buf = new byte[1 << 16];
                int n;
                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                    if (md != null) md.update(buf, 0, n);
                }
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
            if (md != null) sig[0] = new java.math.BigInteger(1, md.digest()).toString(16);
            return true;
        } catch (Throwable t) {
            tmp.delete();
            Log.e("העתקה", t);
            return false;
        }
    }

    /** שחזור מהסל למקום המקורי. שם תפוס → "(משוחזר)", והרשימות עוקבות (65). לא מחזיר לרשימות — החלטה;
     * המשתמש (11.11.2026): "כך הסברה הפשוטה. למה שנחזיר לרשימה אם הוא נמחק לגמרי". (במ0.32 נבנתה החזרה בטעות, ובמ0.34 הוסרה.) */
    public static Result restore(Library lib, List<Trash.Item> items) {
        Result r = new Result();
        for (Trash.Item it : items) {
            if (it.original == null) {
                r.fail(Words.Disk.NO_ORIGINAL_PLACE);
                continue;
            }
            if (it.original.startsWith(Trash.OLD_BOX)) {                    // מ0.29: "📦" — רשימה / כלי / אופן שהוסרו במנוע הישן
                r.fail(Words.Box.OLD_ENGINE);
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
        if (!origs.isEmpty()) r.lists = listsGone(origs);
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

    // ================= שינוי שם בכונן (84) ומחיקה של תיקייה חדשה (106) =================

    /** בדיקת שם לקובץ או לתיקייה בכונן. null = תקין; אחרת ההסבר. מבנה 9: הבדיקה עצמה — ב־Files (גם הבסיס שואל). */
    public static String badName(String name) {
        return Files.badName(name);
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
        r.lists = listsMoved(moved);                                         // מ0.23: גם המסד
        r.done.add(path);
        r.to.put(path, dst.getAbsolutePath());
        moved(path, dst.getAbsolutePath());
        Log.i("שם שונה בכונן: " + path + " ← " + name);
        return r;
    }

    // ================= פריסת ZIP (116) =================

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
                Log.e("Physical.1684", ignored);   // מ0.32: לא בולעים בשקט
            }
            return outList;
        }
    }
}
