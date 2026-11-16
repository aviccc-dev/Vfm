package com.tikiyot.vfm;

// מבנה 184 (סידור קומת החומר): מנוע ההעתקה וההעברה — הלולאה על הפריטים (go), והריצה האחת (Run): שם תפוס ביעד, מיזוג תיקיות, ההתקדמות,
// ומה שהאינדקס והרשימות צריכים לדעת בסוף. עד כאן ב־Physical.
// אחראי: הביצוע. אסור לו: להיקרא מחוץ לתיקיית הכונן — הכניסות (transfer, transferKeep, transferTo) נשארו ב־Physical, ושם נשאל ההיתר (שומר 184).

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class Mover {

    private Mover() { }

    /** הביצוע עצמו: all[i] אל תוך to[i]. הבדיקות (תיקיית התוכנה, סל, מקום, גודל) — לפי checkFolder. */
    static void go(Run run, List<File> all, List<File> to, File checkFolder) {
        boolean copy = run.copy;
        Library lib = run.lib;
        for (File f : all) {
            if (f.isDirectory()) {
                long[] st = DiskInfo.count(f);
                run.total += st[0];
                run.totalBytes += st[1];
            } else {
                run.total++;
                run.totalBytes += f.length();
            }
        }
        if (DiskRules.programFile(checkFolder.getAbsolutePath()) != null) {
            run.r.fail(Words.Disk.DEST_IS_APP);
            run.r.failed = all.size();
            return;
        }
        String droot = lib.rootOf(checkFolder.getAbsolutePath());
        if (droot != null && DiskRules.inside(checkFolder, new File(droot, Index.TRASH))) {
            run.r.fail(Words.Disk.DEST_IS_TRASH);
            run.r.failed = all.size();
            return;
        }
        long need = run.needed(all, checkFolder);
        if (need > 0 && DiskInfo.free(checkFolder) < need) {
            run.r.fail(Words.Disk.NO_ROOM_NEED + Files.size(need) + Words.Disk.NO_ROOM_FREE + Files.size(DiskInfo.free(checkFolder)) + ")");
            run.r.failed = all.size();
            return;
        }
        checkFolder.mkdirs();
        run.maxFile = DiskInfo.maxFileSize(checkFolder);
        for (int k = 0; k < all.size(); k++) {
            if (run.r.cancelled) break;
            File f = all.get(k), folder = to.get(k);
            String p = f.getAbsolutePath();
            boolean isDir = f.isDirectory();
            if (!f.exists()) {
                run.r.fail(Words.Disk.NOT_FOUND_REFRESH);
                continue;
            }
            if (!copy && DiskRules.cantRemove(p) != null) {                     // מבנה 230
                run.r.fail(DiskRules.cantRemove(p));
                continue;
            }
            if (isDir && DiskRules.inside(folder, f)) {
                run.r.fail(Words.Disk.NOT_INTO_ITSELF);
                continue;
            }
            if (isDir && !copy) {
                String why = DiskRules.forbidden(lib, f);
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
            if (!copy) Physical.moved(p, dst);
            if (isDir) {
                run.r.dirs++;
                run.r.inner += run.filesDone - before;
            }
        }
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

    /** ריצה אחת של העברה/העתקה: מה נזכר ("אותו דבר לכל השאר"), ההתקדמות, והשינויים לאינדקס ולרשימות. */
    static final class Run {
        final Library lib;
        final boolean copy, grey;
        final Physical.Asker asker;
        final Physical.Progress progress;
        final Physical.Result r = new Physical.Result();
        final String stamp = Physical.stamp();
        Physical.Answer forFiles, forDirs;
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

        Run(Library lib, boolean copy, Physical.Asker asker, Physical.Progress progress, boolean grey) {
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
                n += f.isDirectory() ? DiskInfo.count(f)[1] : f.length();
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
            boolean self = dst.equals(src) || (dst.exists() && DiskRules.sameFile(src, dst));   // מבנה 42: אותו קובץ, גם בכתיב אחר
            if (self && !copy) {                               // העברה אל המקום שהוא כבר בו — לא "להחליף" אותו בעצמו
                r.fail(Words.Disk.ALREADY_HERE);
                return null;
            }
            if (self) {                                        // העתקה לאותה תיקייה: עותק בשם חדש, בלי לשאול
                dst = new File(dir, DiskRules.freeName(dir, src.getName(), isDir));
            } else if (keep && isDir && dst.isDirectory()) {     // 1.19.0: התיקייה כבר קיימת ביעד — ממזגים
                merge(src, dst);
                return dst.getAbsolutePath();
            } else if (dst.exists()) {
                boolean both = isDir && dst.isDirectory();
                Physical.Answer a = both ? forDirs : forFiles;
                String sug = DiskRules.freeName(dir, src.getName(), isDir);
                if (a == null) {
                    if (asker == null) {
                        r.fail(Words.NAME_TAKEN_AT_DEST);
                        return null;
                    }
                    a = asker.ask(dst, src, sug);
                    if (a == null) a = new Physical.Answer(Physical.Choice.CANCEL, null, false);
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
                        if (dst.exists()) dst = new File(dir, DiskRules.freeName(dir, n, isDir));
                        break;
                    }
                    case REPLACE: {
                        if (DiskRules.inside(src, dst)) {
                            r.fail(Words.Disk.CANT_REPLACE_PARENT);
                            return null;
                        }
                        if (dst.isDirectory()) for (String x : Trash.relFiles(dst)) replaced.add(dst + "/" + x);
                        else replaced.add(dst.getAbsolutePath());
                        if (Physical.toTrash(lib, dst, stamp, r) == null) return null;
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

        void removeIfEmpty(File d) {
            String[] left = d.list();
            if (left != null && left.length == 0 && d.delete()) lib.dirChanged(d.getParent());
        }

        /** src למקום dst (שכבר פנוי). */
        private boolean place(File src, File dst) {
            if (src.isDirectory()) {
                if (!copy) {
                    List<String> rel = Trash.relFiles(src);
                    long bytes = DiskInfo.count(src)[1];
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
            boolean ok = copy ? FileCopy.copyFile(src, dst) : FileCopy.moveFile(src, dst, same(src, dst));
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
            if (!moved.isEmpty()) r.lists += Physical.listsMoved(moved);
            if (!grey && !replaced.isEmpty()) {
                List<String> gone = new ArrayList<String>();
                for (String p : replaced) if (!new File(p).exists()) gone.add(p);
                if (!gone.isEmpty()) r.lists += Physical.listsGone(gone);
            }
        }
    }
}
