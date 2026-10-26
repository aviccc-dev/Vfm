package com.tikiyot.vfm;

// פעולות קובץ בסיסיות. אחראי: קריאה, כתיבה בטוחה, העתקה, גיבוי.
// אסור לו: לדעת על מבנה התוכנה.

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

public final class Files {

    public static final int KEEP_BACKUPS = 10;

    private Files() { }

    // ---------- 170: קריאה פעם אחת (ביצועים) ----------
    // קובצי התוכנה (הקובץ הראשי, מבנה.txt, ההגדרות) נקראים הרבה פעמים בכל רענון. קובץ שלא השתנה
    // (אותו תאריך ואותו גודל) — מהזיכרון. מה שהתוכנה כותבת — נשמר מיד כמו שנכתב. קובץ שהשתנה מבחוץ
    // לפני רגע — נקרא מהכונן, ונזכר רק כשהוא יציב (שעון גס בכרטיס: שני שינויים באותה שנייה ייראו זהים).

    private static final class Cached {
        final long mtime, length;
        final String text;

        Cached(long mtime, long length, String text) {
            this.mtime = mtime;
            this.length = length;
            this.text = text;
        }
    }

    private static final int CACHE_FILES = 8, CACHE_MAX = 2 * 1024 * 1024;
    private static final long STABLE_MS = 2500;
    private static final java.util.LinkedHashMap<String, Cached> CACHE = new java.util.LinkedHashMap<String, Cached>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(java.util.Map.Entry<String, Cached> e) {
            return size() > CACHE_FILES;
        }
    };

    private static void remember(File f, String text, boolean ours) {
        long m = f.lastModified(), n = f.length();
        synchronized (CACHE) {
            String key = f.getAbsolutePath();
            if (m > 0 && n <= CACHE_MAX && (ours || System.currentTimeMillis() - m > STABLE_MS))
                CACHE.put(key, new Cached(m, n, text));
            else CACHE.remove(key);
        }
    }

    /** הקובץ השתנה בדרך אחרת (העתקה, שחזור) — נקרא שוב מהכונן. */
    static void forget(File f) {
        synchronized (CACHE) {
            CACHE.remove(f.getAbsolutePath());
        }
    }

    public static String readUtf8(File f) throws IOException {
        long m = f.lastModified(), n = f.length();
        synchronized (CACHE) {
            Cached c = CACHE.get(f.getAbsolutePath());
            if (c != null && m > 0 && c.mtime == m && c.length == n) return c.text;
        }
        String text = readRaw(f);
        if (f.lastModified() == m && f.length() == n) remember(f, text, false);
        return text;
    }

    private static String readRaw(File f) throws IOException {
        InputStream in = new FileInputStream(f);
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
            return bo.toString("UTF-8");
        } finally {
            in.close();
        }
    }

    /** כתיבה לקובץ זמני ושינוי שם, כדי שקריסה לא תשאיר קובץ פגום. */
    public static void writeAtomic(File f, String content) throws IOException {
        File dir = f.getParentFile();
        if (dir != null) dir.mkdirs();
        // שם זמני ייחודי: שני כותבים במקביל לא דורסים זה את הקובץ הזמני של זה
        File tmp = new File(dir, "." + f.getName() + "." + System.nanoTime() + ".tmp");
        FileOutputStream out = new FileOutputStream(tmp);
        try {
            out.write(content.getBytes("UTF-8"));
            out.flush();
            out.getFD().sync();          // נכתב פיזית לפני ההחלפה, גם אם החשמל ייפסק
        } finally {
            out.close();
        }
        forget(f);
        if (!tmp.renameTo(f)) {
            f.delete();
            if (!tmp.renameTo(f)) throw new IOException(Words.FilesW.CANT_SAVE + f.getName());
        }
        remember(f, content, true);                                          // 170: מה שנכתב — כמו שנכתב
    }

    public static void copy(File src, File dst) throws IOException {
        forget(dst);
        InputStream in = new FileInputStream(src);
        try {
            OutputStream out = new FileOutputStream(dst);
            try {
                byte[] buf = new byte[16384];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }

    /** עותק לפני שמירה, בתיקיית הגיבויים, עם שמירה על העשרה האחרונים. */
    public static void backup(File f, String group) {
        try {
            if (!f.exists()) return;
            File dir = new File(Paths.backups(), group);
            dir.mkdirs();
            String stamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS", Locale.US).format(new Date());
            copy(f, new File(dir, stamp + "-" + f.getName()));
            File[] all = dir.listFiles();
            if (all != null && all.length > KEEP_BACKUPS * 4) {
                Arrays.sort(all, new Comparator<File>() {
                    @Override
                    public int compare(File a, File b) { return b.getName().compareTo(a.getName()); }
                });
                for (int i = KEEP_BACKUPS * 4; i < all.length; i++) all[i].delete();
            }
        } catch (Throwable t) {
            Log.e("גיבוי " + f.getName(), t);
        }
    }

    /** 90: הגיבוי האוטומטי האחרון של קובץ (לפי השם), או null. */
    public static File latestBackup(File f, String group) {
        try {
            File[] all = new File(Paths.backups(), group).listFiles();
            File best = null;
            if (all != null) for (File b : all)
                if (b.isFile() && b.length() > 0 && b.getName().endsWith("-" + f.getName())
                        && (best == null || b.getName().compareTo(best.getName()) > 0)) best = b;
            return best;
        } catch (Throwable t) {
            return null;
        }
    }

    /** גיבוי בשם, לפני פעולה הרסנית. */
    public static void backupNamed(File f, String label) {
        try {
            if (!f.exists()) return;
            File dir = new File(Paths.backups(), "בשם");  // בכונן
            dir.mkdirs();
            String stamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US).format(new Date());
            copy(f, new File(dir, stamp + " " + label.replace('/', '-') + " - " + f.getName()));
        } catch (Throwable t) {
            Log.e("גיבוי בשם", t);
        }
    }

    public static String size(long b) {
        if (b < 1024) return b + "B";
        if (b < 1024L * 1024) return String.format(Locale.US, "%.0fKB", b / 1024.0);
        if (b < 1024L * 1024 * 1024) return String.format(Locale.US, "%.1fMB", b / 1048576.0);
        return String.format(Locale.US, "%.2fGB", b / 1073741824.0);
    }

    public static String count(long n) {
        return String.format(Locale.US, "%,d", n);
    }

    public static String when(long t) {
        if (t <= 0) return Words.FilesW.UNKNOWN;
        long diff = System.currentTimeMillis() - t;
        if (diff < 60000) return Words.FilesW.NOW;
        if (diff < 3600000) return Words.FilesW.AGO + (diff / 60000) + Words.FilesW.MINUTES;
        if (diff < 86400000) return Words.FilesW.AGO + (diff / 3600000) + Words.FilesW.HOURS;
        return new SimpleDateFormat("d.M.yy HH:mm", Locale.US).format(new Date(t));
    }

    /** כונן שאינו זמין (כרטיס שהוצא או מדיה פגומה) — לפי תחילת המסלול. */
    public static boolean driveMissing(String path) {
        if (path == null || !path.startsWith("/storage/") || path.startsWith("/storage/emulated/")) return false;
        int i = path.indexOf('/', 9);
        File root = new File(i > 0 ? path.substring(0, i) : path);
        return !root.exists() || !root.canRead();
    }
}
