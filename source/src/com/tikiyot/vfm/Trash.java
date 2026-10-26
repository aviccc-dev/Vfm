package com.tikiyot.vfm;

// סל המחזור. אחראי: יומן המחיקות, רשימת הסל מתוך האינדקס, שחזור וריקון.
// היומן הוא קובץ טקסט בתוך ".נמחקו" בכל כונן: שורה לכל קובץ — השם בסל, טאב, המסלול המקורי.

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Trash {

    /** מתחיל בנקודה, ולכן אינו נכנס לאינדקס ואינו מופיע בסל. */
    public static final String LOG = ".יומן מחיקות.txt";  // בכונן

    public static final class Item {
        public Library.Hit hit;
        public String original;       // null אם לא נרשם ביומן
        /** תיקייה שנמחקה כולה (82): שורה אחת בסל. */
        public boolean dir;
        public long size, mtime;
        /** בתיקייה: כמה קבצים בתוכה. */
        public int files = 1;

        public String path() { return hit.path(); }

        public String name() { return hit.name(); }
    }

    private Trash() { }

    private static final Object LOCK = new Object();
    private static volatile java.util.Set<String> originals;

    private static File logOf(File trashDir) {
        return new File(trashDir, LOG);
    }

    public static void logDeleted(File target, String original) {
        synchronized (LOCK) {
            try {
                File log = logOf(target.getParentFile());               // 97: שורה בסוף — לא כתיבה מחדש של כל היומן
                java.io.FileOutputStream o = new java.io.FileOutputStream(log, true);
                try {
                    o.write((target.getName() + "\t" + original + "\n").getBytes("UTF-8"));
                    o.getFD().sync();
                } finally {
                    o.close();
                }
            } catch (Throwable t) {
                Log.e("יומן מחיקות", t);
            }
            originals = null;
        }
    }

    /** השם בסל → המסלול המקורי, לכונן אחד. */
    private static Map<String, String> read(File trashDir) {
        Map<String, String> m = new HashMap<String, String>();
        File log = logOf(trashDir);
        if (!log.exists()) return m;
        try {
            for (String line : Files.readUtf8(log).split("\n")) {
                int tab = line.indexOf('\t');
                if (tab > 0) m.put(line.substring(0, tab), line.substring(tab + 1).trim());
            }
        } catch (Throwable t) {
            Log.e("קריאת יומן מחיקות", t);
        }
        return m;
    }

    static void forget(File trashDir, java.util.Collection<String> names) {
        synchronized (LOCK) {
            File log = logOf(trashDir);
            if (!log.exists()) return;
            try {
                StringBuilder sb = new StringBuilder();
                for (String line : Files.readUtf8(log).split("\n")) {
                    int tab = line.indexOf('\t');
                    if (line.trim().isEmpty() || (tab > 0 && names.contains(line.substring(0, tab)))) continue;
                    sb.append(line).append('\n');
                }
                Files.writeAtomic(log, sb.toString());
            } catch (Throwable t) {
                Log.e("עדכון יומן מחיקות", t);
            }
            originals = null;
        }
    }

    /** תוכן הסל מכל הכוננים, מתוך האינדקס: קבצים, ותיקיות שנמחקו כולן — שורה אחת לכל אחת (82). */
    public static List<Item> list(Library lib) {
        List<Item> out = new ArrayList<Item>();
        for (Index ix : lib.indexes) {
            int top = ix.dirId(new File(ix.rootPath, Index.TRASH).getAbsolutePath());
            if (top < 0) continue;
            Map<String, String> log = read(new File(ix.rootPath, Index.TRASH));
            Map<Integer, Item> dirs = new java.util.LinkedHashMap<Integer, Item>();
            int[] under = new int[ix.dirs.size()];              // לכל תיקייה בסל: התיקייה העליונה שלה
            for (int k = 0; k < ix.dirs.size(); k++) {
                Index.Dir d = ix.dirs.get(k);
                under[k] = -1;
                if (d.gone || d.parent < 0) continue;
                if (d.parent == top) {
                    under[k] = k;
                    Item it = new Item();
                    it.hit = new Library.Hit();
                    it.hit.ix = ix;
                    it.hit.dirId = k;
                    it.dir = true;
                    it.files = 0;
                    it.mtime = d.mtime;
                    it.original = log.get(d.name);
                    dirs.put(k, it);
                } else if (d.parent < k) {
                    under[k] = under[d.parent];
                }
            }
            for (Index.Rec r : ix.files) {
                if (r.gone || r.dir < 0) continue;
                if (r.dir == top) {
                    Item it = new Item();
                    it.hit = Library.hit(ix, r);
                    it.original = log.get(r.name);
                    it.size = r.size;
                    it.mtime = r.mtime;
                    out.add(it);
                } else if (under[r.dir] >= 0) {
                    Item it = dirs.get(under[r.dir]);
                    it.files++;
                    it.size += r.size;
                }
            }
            out.addAll(dirs.values());
        }
        return out;
    }

    /** האם מסלול זה נמחק ונמצא בסל (לסימון "נמחק · בסל המחזור" ברשימות) — גם בתוך תיקייה שנמחקה. */
    public static boolean isInTrash(Library lib, String originalPath) {
        java.util.Set<String> s = originals;
        List<String> pre = prefixes;
        if (s == null) {
            s = new java.util.HashSet<String>();
            pre = new ArrayList<String>();
            for (Index ix : lib.indexes) {
                File dir = new File(ix.rootPath, Index.TRASH);
                for (Map.Entry<String, String> e : read(dir).entrySet()) {
                    File f = new File(dir, e.getKey());
                    if (f.isFile()) s.add(e.getValue());
                    else if (f.isDirectory()) pre.add(e.getValue() + "/");
                }
            }
            prefixes = pre;
            originals = s;
        }
        if (s.contains(originalPath)) return true;
        for (String p : pre) if (originalPath.startsWith(p)) return true;
        return false;
    }

    private static volatile List<String> prefixes = new ArrayList<String>();

    /** כל הקבצים שבתוך תיקייה בכונן, כמסלולים יחסיים ("א/ב.pdf"). */
    static List<String> relFiles(File dir) {
        List<String> out = new ArrayList<String>();
        walk(dir, "", out);
        return out;
    }

    private static void walk(File dir, String pre, List<String> out) {
        File[] all = dir.listFiles();
        if (all == null) return;
        for (File f : all) {
            if (f.isDirectory()) walk(f, pre + f.getName() + "/", out);
            else out.add(pre + f.getName());
        }
    }

    /** מחיקה של ממש של תיקייה וכל מה שבתוכה. מחזיר אם נמחקה כולה. */
    static boolean wipe(File f) {
        File[] kids = f.listFiles();
        if (kids != null) for (File k : kids) wipe(k);
        return f.delete() || !f.exists();
    }

    /** שחזור למקום המקורי. שם תפוס מקבל תוספת. תיקייה חוזרת כולה (82). מחזיר את המסלול החדש, או null. */
    public static String restore(Library lib, Item it) {
        File src = new File(it.path());
        if (!(it.dir ? src.isDirectory() : src.isFile()) || it.original == null) return null;
        File dst = new File(it.original);
        if (dst.exists()) {
            String n = dst.getName();
            String base = it.dir ? n : Text.stem(n), ext = it.dir ? "" : Text.tail(n);          // 124
            if (base.isEmpty()) { base = n; ext = ""; }
            for (int i = 1; dst.exists(); i++)
                dst = new File(dst.getParentFile(), base + " (משוחזר" + (i > 1 ? " " + i : "") + ")" + ext);  // בכונן
        }
        File parent = dst.getParentFile();
        if (parent != null) parent.mkdirs();
        if (!src.renameTo(dst)) {
            Log.i("שחזור נכשל: " + src);
            return null;
        }
        boolean renamed = !dst.getAbsolutePath().equals(it.original);
        if (it.dir) {
            lib.dirChanged(src.getParent());
            lib.dirChanged(dst.getParent());
            if (renamed) {                                     // שם תפוס: הפריטים ברשימות עוברים לשם החדש (65)
                Map<String, String> m = new HashMap<String, String>();
                for (String rel : relFiles(dst)) m.put(it.original + "/" + rel, dst.getAbsolutePath() + "/" + rel);
                Tree.replaceEverywhere(m);
            }
        } else {
            lib.fileRemoved(src.getAbsolutePath());
            lib.fileAdded(dst);
            if (renamed) Tree.replaceEverywhere(java.util.Collections.singletonMap(it.original, dst.getAbsolutePath()));
        }
        forget(src.getParentFile(), java.util.Collections.singletonList(src.getName()));
        Log.i("שוחזר: " + dst);
        return dst.getAbsolutePath();
    }

    /** מחיקה של ממש. מחזיר כמה נמחקו, ומוסיף ל-origs את המסלולים המקוריים שלהם. */
    public static int empty(Library lib, List<Item> items, List<String> origs) {
        int n = 0;
        Map<File, List<String>> names = new HashMap<File, List<String>>();
        for (Item it : items) {
            File f = new File(it.path());
            if (it.dir) {
                List<String> rel = relFiles(f);
                if (!wipe(f)) {
                    lib.dirChanged(f.getParent());
                    continue;
                }
                lib.dirChanged(f.getParent());
                if (it.original != null && !BinBox.isBox(it.original)) for (String r : rel) origs.add(it.original + "/" + r);
            } else {
                if (f.isFile() && !f.delete()) continue;
                lib.fileRemoved(it.path());
                if (it.original != null) origs.add(it.original);
            }
            n++;
            List<String> l = names.get(f.getParentFile());
            if (l == null) names.put(f.getParentFile(), l = new ArrayList<String>());
            l.add(f.getName());
        }
        for (Map.Entry<File, List<String>> e : names.entrySet()) forget(e.getKey(), e.getValue());
        Log.i("סל המחזור: נמחקו " + n);
        return n;
    }
}
