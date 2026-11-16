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

    // ================= הביצוע (בלי מסכים — נבדק בבדיקות) =================

    static String stamp() {
        return new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", java.util.Locale.US).format(new java.util.Date());
    }

    /** מבנה 76: פעולה שביקשו בלי ההיתר של שכבת הניהול — לא נעשתה; התוצאה אומרת למה. */
    private static Result refused(String why) {
        Result r = new Result();
        r.fail(why);
        return r;
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
        return delete(lib, paths, dirs, grey, false);
    }

    /**
     * מבנה 64: מחיקה שהתוכנה עצמה מבקשת, של תיקייה שלה (הגיבויים וההגדרות של אופן שבוטל) — לסל, כמו כל מחיקה. ההגנה על
     * קובצי התוכנה היא מפני המשתמש והפקודות; כאן התוכנה היא שמחליטה. רק תיקייה שבתוך תיקיית התוכנה או התיקייה של המסד —
     * כל תיקייה אחרת עוברת את הבדיקות הרגילות. שורש כונן והסל — אסורים גם כאן.
     */
    public static Result deleteOwn(Library lib, File dir) {
        List<String> one = java.util.Collections.singletonList(dir.getAbsolutePath());
        return delete(lib, new ArrayList<String>(), one, false, DiskRules.ours(Paths.clean(dir.getAbsolutePath())));
    }

    private static Result delete(Library lib, List<String> paths, List<String> dirs, boolean grey, boolean byApp) {
        Result r = new Result();
        String no = Permit.check(Permit.DELETE);                               // מבנה 76: רק דרך שכבת הניהול
        if (no != null) {
            r.fail(no);
            return r;
        }
        String stamp = stamp();
        List<String> gone = new ArrayList<String>();
        for (String p : paths) {
            String own = DiskRules.cantRemove(p);                              // מבנה 230
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
            String why = byApp ? DiskRules.rootOrTrash(lib, Paths.clean(f.getAbsolutePath())) : DiskRules.forbidden(lib, f);
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
    static String toTrash(Library lib, File f, String stamp, Result r) {
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
            target = new File(dir, DiskRules.freeName(dir, target.getName(), isDir));
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
        String no = Permit.check(Permit.TRANSFER);                             // מבנה 76
        if (no != null) return refused(no);
        Mover.Run run = new Mover.Run(lib, copy, asker, progress, grey);
        run.journal = journal;
        run.journalRoot = folder;
        List<File> all = Mover.topOnly(files, dirs);
        List<File> to = new ArrayList<File>();
        for (int k = 0; k < all.size(); k++) to.add(folder);
        Mover.go(run, all, to, folder);
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
        String no = Permit.check(Permit.TRANSFER);                             // מבנה 76
        if (no != null) return refused(no);
        Mover.Run run = new Mover.Run(lib, copy, asker, progress, grey);
        run.keep = true;
        run.journal = journal;
        run.journalRoot = destRoot;
        String base = new File(root).getAbsolutePath();
        List<File> all = new ArrayList<File>(), to = new ArrayList<File>();
        for (File f : Mover.topOnly(files, dirs)) {
            String rel = DiskRules.rel(base, f.getAbsolutePath());
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
                for (File up = f.getParentFile(); up != null && DiskRules.rel(base, up.getAbsolutePath()) != null
                        && !up.getAbsolutePath().equals(base); up = up.getParentFile())
                    emptied.add(up.getAbsolutePath());
        Mover.go(run, all, to, destRoot);
        for (String d : emptied) run.removeIfEmpty(new File(d));     // מהעמוק אל הרדוד
        run.finish();
        return run.r;
    }

    /** 1.21.0: כל קובץ אל התיקייה שלו (folders[i]) — לשחזור, כשהקבצים באים מכמה שורשים. */
    public static Result transferTo(Library lib, List<String> files, List<String> folders, File checkFolder, boolean copy,
                                    Asker asker, Progress progress, boolean grey, Journal journal) {
        String no = Permit.check(Permit.TRANSFER);                             // מבנה 76
        if (no != null) return refused(no);
        Mover.Run run = new Mover.Run(lib, copy, asker, progress, grey);
        run.keep = true;
        run.journal = journal;
        run.journalRoot = checkFolder;
        List<File> all = new ArrayList<File>(), to = new ArrayList<File>();
        for (int k = 0; k < files.size(); k++) {
            all.add(new File(files.get(k)));
            to.add(new File(folders.get(k)));
        }
        Mover.go(run, all, to, checkFolder);
        run.finish();
        return run.r;
    }

    /** שחזור מהסל למקום המקורי. שם תפוס → "(משוחזר)", והרשימות עוקבות (65). לא מחזיר לרשימות — החלטה;
     * המשתמש (11.11.2026): "כך הסברה הפשוטה. למה שנחזיר לרשימה אם הוא נמחק לגמרי". (במ0.32 נבנתה החזרה בטעות, ובמ0.34 הוסרה.) */
    public static Result restore(Library lib, List<Trash.Item> items) {
        String no = Permit.check(Permit.RESTORE);                              // מבנה 76
        if (no != null) return refused(no);
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
        String no = Permit.check(Permit.FOREVER);                              // מבנה 76
        if (no != null) return refused(no);
        Result r = new Result();
        List<String> origs = new ArrayList<String>();
        int n = Trash.empty(lib, items, origs);
        for (int i = 0; i < n; i++) r.done.add("");
        r.failed = items.size() - n;
        if (r.failed > 0) r.note = Words.DELETE_FAILED;
        if (!origs.isEmpty()) r.lists = listsGone(origs);
        return r;
    }

    // ================= שינוי שם בכונן (84) ומחיקה של תיקייה חדשה (106) =================

    /**
     * שינוי שם של קובץ בכונן, באותה תיקייה (84). האינדקס מתעדכן בתור, והרשימות עוקבות אחרי השם החדש.
     * לא מחליף קובץ קיים — שם תפוס נדחה.
     */
    public static Result rename(Library lib, String path, String newName) {
        String no = Permit.check(Permit.RENAME);                               // מבנה 76
        if (no != null) return refused(no);
        Result r = new Result();
        String own = DiskRules.programFile(path);
        if (own != null) {
            r.fail(own);
            return r;
        }
        File f = new File(path);
        String bad = DiskRules.badName(newName);
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

    // ================= תיקייה חדשה, קובץ חדש, וכתיבה באזור של התוכנה =================

    /** יצירת תיקייה ריקה בכונן. null = הצליח; אחרת ההסבר. */
    public static String makeFolder(Library lib, File parent, String name) {
        String no = Permit.check(Permit.MAKE_FOLDER);                          // מבנה 76
        if (no != null) return no;
        String bad = DiskRules.badName(name);
        if (bad != null) return bad;
        if (DiskRules.programFile(new File(parent, name.trim()).getAbsolutePath()) != null) return Words.Disk.NO_FOLDER_IN_APP;
        File made = new File(parent, name.trim());
        if (made.exists()) return Words.Disk.ALREADY_HERE_NAME + name.trim() + "\"";
        if (!made.mkdirs()) return Words.CANT_CREATE_FOLDER;
        lib.dirChanged(parent.getAbsolutePath());               // האינדקס לומד עליה
        Log.i("נוצרה תיקייה בכונן: " + made.getAbsolutePath());
        return null;
    }

    /**
     * מבנה 139: כתיבה של קובץ באזור שהתוכנה מנהלת בעצמה (התיקייה של תוסף, עותק פרטי של קובץ שרץ, קובץ פקודות בתוך תיקיית
     * התוכנה) — לא קובץ של המשתמש, ולכן בלי סל ובלי שאלה; אבל רק בהיתר של שכבת הניהול. null — נכתב; אחרת — למה לא.
     */
    public static String ownWrite(File f, String text) {
        String no = Permit.check(Permit.OWN);
        if (no != null) return no;
        try {
            Files.writeAtomic(f, text == null ? "" : text);
            return null;
        } catch (Throwable t) {
            Log.e("Physical.ownWrite", t);
            return Words.Db.failed(String.valueOf(t.getMessage()));
        }
    }

    /**
     * מבנה 154 (המשתמש, 15.11.2026: שמר בעורך קובץ פקודות קיים — "אין בסל"): קובץ פקודות של המשתמש שיושב בתוך תיקיית התוכנה
     * (הקיצורים, "הפקודות שלי") הוא שלו — כשהוא מוחלף, הגרסה הקודמת עוברת קודם לסל (הוראות, סעיף 4), ורק אז נכתב החדש.
     * null — נכתב; אחרת — למה לא (והקובץ הקודם לא נגע).
     */
    public static String ownReplace(Library lib, File f, String text) {
        String no = Permit.check(Permit.OWN);
        if (no != null) return no;
        if (f.isFile() && lib != null) {
            String why = oldToTrash(lib, f);
            if (why != null) return why;
        }
        String bad = ownWrite(f, text);
        if (bad == null && lib != null) lib.fileAdded(f);                      // מבנה 229: הקודם יצא מהאינדקס עם המעבר לסל — החדש נכנס
        return bad;
    }

    /** מבנה 139: מחיקה של קובץ באזור שהתוכנה מנהלת (תוצאות ישנות בתיקייה של תוסף) — רק בהיתר. true — נמחק. */
    public static boolean ownDelete(File f) {
        return Permit.check(Permit.OWN) == null && f.isFile() && f.delete();
    }

    /** מבנה 140: העברה או שינוי שם בתוך האזור שהתוכנה מנהלת — רק בהיתר. true — עבר. */
    public static boolean ownMove(File from, File to) {
        return Permit.check(Permit.OWN) == null && from.renameTo(to);
    }

    /** מבנה 140: תיקייה באזור שהתוכנה מנהלת — נוצרת (עם מה שמעליה), רק בהיתר. true — קיימת בסוף. */
    public static boolean ownDir(File d) {
        return d.isDirectory() || (Permit.check(Permit.OWN) == null && d.mkdirs());
    }

    /** מבנה 140: תיקייה ריקה באזור שהתוכנה מנהלת — יוצאת, רק בהיתר. תיקייה שיש בה משהו — נשארת. */
    public static boolean ownRemoveEmpty(File d) {
        String[] left = d.list();
        return left != null && left.length == 0 && Permit.check(Permit.OWN) == null && d.delete();
    }

    /**
     * מבנה 221 (ה10; הוראות, סעיף 2א): הכתיבה של "כתוב" — מקום אחד לשורה הכתובה, לקובץ חדש מהמסך ולשמירה של תוסף (שלושתם
     * עוברים בפקודה, דרך המנהל). עד כאן — שני קטעי קוד: אחד בפקודה ואחד כאן (newFile), עם בדיקות, נוסח ועדכון אינדקס שונים.
     * append — להוסיף בסוף; replace — קובץ קיים מוחלף, והקודם עובר לסל (הוראות, סעיף 4). מה שנבדק בתכנון נבדק כאן שוב, כי
     * בין התכנון להרצה הכונן יכול להשתנות (קובץ שנוצר בינתיים — לא מוחלף בלי הוראה). האינדקס מתעדכן ומחכים לו, כדי שהשורה
     * הבאה תראה את הקובץ. null — נכתב; אחרת — למה לא. had[0] — היה קובץ קודם; trashed[0] — הוא עבר לסל.
     */
    public static String write(Library lib, File f, String text, boolean append, boolean replace, boolean[] had, boolean[] trashed) {
        String no = Permit.check(Permit.WRITE);
        if (no != null) return no;
        // מבנה 65: מותר בכל מקום שמותר להעתיק אליו — גם בשורש הכונן ובתיקיות המשתמש שבתיקיית התוכנה; לא בסל, ולא קובץ של התוכנה
        String why = DiskRules.programFile(f.getAbsolutePath());
        if (why == null && lib != null) why = DiskRules.noWriteInto(lib, f.getParentFile());
        if (why != null) return why;
        if (f.isDirectory()) return Words.DbCmd.WRITE_IS_FOLDER;
        File dir = f.getParentFile();
        if (dir == null || !dir.isDirectory())                                  // מבנה 111: "כתוב" אינה יוצרת תיקייה
            return Words.DbCmd.writeNoFolder(Bidi.path(dir == null ? f.getAbsolutePath() : dir.getAbsolutePath()));
        boolean was = f.isFile();
        if (had != null) had[0] = was;
        if (was && !append && !replace) return Words.Cmd.fileTaken(f.getName());   // מבנה 110: נוצר בינתיים — לא מחליפים בלי הוראה
        boolean toTrash = was && !append && lib != null;
        try {
            if (toTrash) {                                                      // מבנה 46: הקובץ הקודם — לסל, לא נדרס
                String w = oldToTrash(lib, f);
                if (w != null) return w;
                if (trashed != null) trashed[0] = true;
            }
            Files.writeAtomic(f, append && was ? Files.readUtf8(f) + text : text);
        } catch (Throwable t) {
            Log.e("Physical.write", t);
            return Words.Db.failed(String.valueOf(t.getMessage()));
        }
        if (lib != null) {
            if (was && !toTrash) lib.fileRemoved(f.getAbsolutePath());
            lib.fileAdded(f);
            lib.flush(5000);   // מ0.45: שורה שאחריה (מחק, העבר) — מול הגודל החדש, לא "הקובץ השתנה מאז הסריקה"
        }
        Log.i((append && was ? "נוסף לקובץ בכונן: " : "נוצר קובץ בכונן: ") + f);   // כמו תיקייה חדשה ומחיקה — היומן יודע מה השתנה בכונן
        return null;
    }
}
