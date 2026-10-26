package com.tikiyot.vfm;

// אינדקס של כונן אחד. אחראי: טבלת תיקיות וטבלת קבצים, סריקה, רענון, שמירה וטעינה.
// אסור לו: לשנות קבצים בכונן.

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class Index {

    private static final int MAGIC = 0x56464D31;   // VFM1
    private static final int VERSION = 3;          // 189: "נוסף"; 190: חתימת תוכן (גרסאות 1–2 — נקראות, בלי מה שחסר בהן)

    /** תיקיית המחיקות בשורש כל כונן. נכללת באינדקס אף שמתחילה בנקודה, ומסומנת כסל. */
    public static final String TRASH = ".נמחקו";  // בכונן

    public static final class Dir {
        public int parent;
        public String name, norm;
        public long mtime;
        public boolean gone;
        /** התיקייה היא ".נמחקו" שבשורש, או בתוכה. */
        public boolean trash;
        /** מוסתרת: שמה מתחיל בנקודה, או שהיא בתוך מוסתרת (63). */
        public boolean hidden;
    }

    public static final class Rec {
        public int dir;
        public String name, norm, loose;
        public long size, mtime;
        /** 189: מתי האינדקס ראה את הקובץ לראשונה ("נוסף"); 0 — לא ידוע (היה כבר בבנייה הראשונה). */
        public long added;
        /**
         * 190: חתימת התוכן (Sig, 64 סיביות); 0 — עוד לא חושבה. תקפה לגודל ולתאריך שברשומה — כשהם משתנים, מתאפסת.
         * נכתבת ברקע גם על התמונה המוצגת (volatile — קריאה וכתיבה שלמות).
         */
        public volatile long sig;
        public boolean gone;
    }

    public interface Progress {
        void onScan(String label, int items);
    }

    public final String key;
    public String label, rootPath;
    public final ArrayList<Dir> dirs = new ArrayList<Dir>();
    public final ArrayList<Rec> files = new ArrayList<Rec>();
    public long built;

    public Index(String key, String label, String rootPath) {
        this.key = key;
        this.label = label;
        this.rootPath = rootPath;
    }

    /** עותק עמוק: הבנייה והרענון עובדים עליו, והחיפוש ממשיך על המקור עד ההחלפה. */
    public Index copy() {
        Index c = new Index(key, label, rootPath);
        c.built = built;
        c.rootMtime = rootMtime;
        c.paths = paths;                                    // 184: אותם מזהים — אותם מסלולים (רק נוספים בסוף)
        c.dirs.ensureCapacity(dirs.size());
        for (Dir d : dirs) {
            Dir x = new Dir();
            x.parent = d.parent;
            x.name = d.name;
            x.norm = d.norm;
            x.mtime = d.mtime;
            x.gone = d.gone;
            x.trash = d.trash;
            x.hidden = d.hidden;
            c.dirs.add(x);
        }
        c.files.ensureCapacity(files.size());
        for (Rec r : files) {
            Rec x = new Rec();
            x.dir = r.dir;
            x.name = r.name;
            x.norm = r.norm;
            x.loose = r.loose;
            x.size = r.size;
            x.mtime = r.mtime;
            x.added = r.added;
            x.sig = r.sig;
            x.gone = r.gone;
            c.files.add(x);
        }
        return c;
    }

    // ---------- מסלולים ----------

    // 184: מסלולי התיקיות — נבנים פעם אחת (ההורה תמיד לפני הילד: מסלול ההורה + "/" + שם), ולא בכל קריאה.
    // מיליון קבצים: path() לכולם — מ־1.3 שניות ל־0.1 במחשב. מתבטל רק כשהמזהים זזים (compact, סריקה).
    private static final class Paths2 {
        final String root;
        final String[] raw;
        final int count;
        /** מי בנה את המערך — רק הוא ממשיך לכתוב בו (עותק — מעתיק קודם, כי המקור משותף). */
        final Index owner;
        volatile String[] norm;

        Paths2(String root, String[] raw, int count, Index owner) {
            this.root = root;
            this.raw = raw;
            this.count = count;
            this.owner = owner;
        }
    }

    private volatile Paths2 paths;

    private Paths2 paths() {
        Paths2 p = paths;
        int n = dirs.size();
        if (p != null && p.root == rootPath && p.count >= n) return p;
        String[] raw;
        int from = 0;
        if (p != null && p.root == rootPath) {                  // נוספו תיקיות בסוף: ממשיכים מאיפה שנעצרנו
            from = p.count;
            raw = n <= p.raw.length && p.owner == this ? p.raw : java.util.Arrays.copyOf(p.raw, Math.max(n, p.raw.length + p.raw.length / 2));
        } else {
            raw = new String[n];
        }
        for (int i = from; i < n; i++) {
            Dir d = dirs.get(i);
            raw[i] = (d.parent >= 0 && d.parent < i ? raw[d.parent] : d.parent < 0 ? rootPath : slowPath(d.parent)) + "/" + d.name;
        }
        p = new Paths2(rootPath, raw, n, this);
        paths = p;
        return p;
    }

    private String slowPath(int id) {
        if (id < 0) return rootPath;
        StringBuilder sb = new StringBuilder();
        for (int d = id; d >= 0; d = dirs.get(d).parent) sb.insert(0, "/" + dirs.get(d).name);
        return rootPath + sb;
    }

    public String dirPath(int id) {
        if (id < 0) return rootPath;
        return paths().raw[id];
    }

    /** 184: המסלול המנורמל של תיקייה (לחיפוש במסלולים ולתנאי תיקייה) — פעם אחת לכל תיקייה. */
    public String normPath(int id) {
        if (id < 0) return Text.norm(rootPath);
        Paths2 p = paths();
        String[] nm = p.norm;
        if (nm == null) {
            nm = new String[p.count];
            for (int i = 0; i < nm.length; i++) nm[i] = Text.norm(p.raw[i]);
            p.norm = nm;
        }
        return nm[id];
    }

    public String path(Rec r) {
        return dirPath(r.dir) + "/" + r.name;
    }

    /** קובץ מוסתר: שמו מתחיל בנקודה, או שהוא בתיקייה מוסתרת. מוצג רק כשמבקשים. */
    public boolean hidden(Rec r) {
        return r.name.startsWith(".") || (r.dir >= 0 && r.dir < dirs.size() && dirs.get(r.dir).hidden);
    }

    /** מזהה התיקייה לפי מסלול מלא: -1 לשורש, -2 אם אינה באינדקס. */
    public int dirId(String path) {
        String root = rootPath.endsWith("/") ? rootPath.substring(0, rootPath.length() - 1) : rootPath;
        if (path.equals(root)) return -1;
        if (!path.startsWith(root + "/")) return -2;
        int cur = -1;
        for (String seg : path.substring(root.length() + 1).split("/")) {
            if (seg.isEmpty()) continue;
            int found = child(cur, seg);
            if (found < 0) return -2;
            cur = found;
        }
        return cur;
    }

    /** 184: תיקיית בת לפי שם, או -1. */
    private int child(int parent, String name) {
        List<Integer> l = kids().get(parent);
        if (l != null)
            for (int k : l) {
                Dir d = dirs.get(k);
                if (!d.gone && d.name.equals(name)) return k;
            }
        return -1;
    }

    /** תאריך התיקייה כפי שנרשם באינדקס (לשורש: 0 — תמיד נבדק). */
    public long dirMtime(int id) {
        return id >= 0 && id < dirs.size() ? dirs.get(id).mtime : 0;
    }

    /** סנכרון תיקייה אחת מול הכונן (כניסה לתיקייה במנהל הקבצים). על עותק בלבד. */
    public void syncDir(int id, File dir) {
        syncChildren(id, dir);
        if (id >= 0 && id < dirs.size()) dirs.get(id).mtime = dir.lastModified();
    }

    /** התיקיות שמתחת לתיקייה. */
    public List<Integer> childDirs(int id) {
        List<Integer> out = new ArrayList<Integer>();
        List<Integer> l = kids().get(id);
        if (l != null) for (int k : l) if (!dirs.get(k).gone) out.add(k);
        return out;
    }

    /** הקבצים שבתוך תיקייה. */
    public List<Rec> files(int id) {
        List<Rec> out = new ArrayList<Rec>();
        for (Rec r : filesIn(id)) if (!r.gone) out.add(r);
        return out;
    }

    /** האם הקובץ נמצא בסל (".נמחקו"). */
    /**
     * 1.22.4: הקובץ בתוך גיבוי "מראה + היסטוריה" (בתיקיות נוכחי / היסטוריה / יומן של שורש גיבוי). חיפושים
     * ורשימות רגילים לא מציגים אותם (ממצא 6) — אחרת כל קובץ מופיע פעמיים ויותר, וכל ספירה מוכפלת.
     */
    public boolean inBackup(Rec r) {
        if (r.dir < 0) return false;
        boolean[] m = backupDirs;
        if (m == null || m.length != dirs.size()) backupDirs = m = backupMarks();
        return r.dir < m.length && m[r.dir];
    }

    private volatile boolean[] backupDirs;

    private boolean[] backupMarks() {
        int n = dirs.size();
        boolean[] m = new boolean[n];
        java.util.HashMap<Integer, java.util.HashMap<String, Integer>> kidsOf = new java.util.HashMap<Integer, java.util.HashMap<String, Integer>>();
        java.util.HashMap<Integer, List<Integer>> all = new java.util.HashMap<Integer, List<Integer>>();
        for (int i = 0; i < n; i++) {
            Dir d = dirs.get(i);
            if (d.gone) continue;
            java.util.HashMap<String, Integer> k = kidsOf.get(d.parent);
            if (k == null) kidsOf.put(d.parent, k = new java.util.HashMap<String, Integer>());
            k.put(d.name, i);
            List<Integer> l = all.get(d.parent);
            if (l == null) all.put(d.parent, l = new ArrayList<Integer>());
            l.add(i);
        }
        for (java.util.Map.Entry<Integer, java.util.HashMap<String, Integer>> e : kidsOf.entrySet()) {
            java.util.HashMap<String, Integer> k = e.getValue();
            if (!k.containsKey(State.CURRENT) || !(k.containsKey(State.JOURNAL) || k.containsKey(State.HISTORY))) continue;
            for (String part : new String[]{State.CURRENT, State.HISTORY, State.JOURNAL}) {
                Integer top = k.get(part);
                if (top == null) continue;
                java.util.ArrayDeque<Integer> q = new java.util.ArrayDeque<Integer>();
                q.add(top);
                while (!q.isEmpty()) {
                    int d = q.poll();
                    if (m[d]) continue;
                    m[d] = true;
                    List<Integer> c = all.get(d);
                    if (c != null) q.addAll(c);
                }
            }
        }
        return m;
    }

    public boolean trash(Rec r) {
        return r.dir >= 0 && r.dir < dirs.size() && dirs.get(r.dir).trash;
    }

    public int fileCount() {
        int n = 0;
        for (Rec r : files) if (!r.gone && !trash(r) && !hidden(r)) n++;
        return n;
    }

    public int dirCount() {
        int n = 0;
        for (Dir d : dirs) if (!d.gone) n++;
        return n;
    }

    // ---------- עדכון נקודתי ----------

    /** מוסיף לאינדקס קובץ שנוצר או הועבר, לפי המסלול שלו. יוצר את התיקיות החסרות. */
    public boolean addFile(File f) {
        String root = rootPath.endsWith("/") ? rootPath : rootPath + "/";
        String p = f.getAbsolutePath();
        if (!p.startsWith(root) || !f.isFile()) return false;
        String[] parts = p.substring(root.length()).split("/");
        int parent = -1;
        File cur = new File(rootPath);
        for (int i = 0; i < parts.length - 1; i++) {
            cur = new File(cur, parts[i]);
            int found = child(parent, parts[i]);
            if (found < 0) {
                newDir(parent, cur);
                found = dirs.size() - 1;
            }
            parent = found;
        }
        String name = parts[parts.length - 1];
        for (Rec r : filesIn(parent))
            if (!r.gone && r.name.equals(name)) {
                long len = f.length(), mt = f.lastModified();
                if (len != r.size || mt != r.mtime) r.sig = 0;               // 190
                r.size = len;
                r.mtime = mt;
                return true;
            }
        newRec(parent, f);
        return true;
    }

    /**
     * 184: קובץ לפי מסלול מלא, או null — דרך "מי בתוך מי", במקום מפה של כל המסלולים בזיכרון
     * (במיליון קבצים — כ־170 מגה). בתיקייה גדולה — מפת שמות לתיקייה ההיא בלבד, פעם אחת.
     */
    public Rec find(String path) {
        int slash = path.lastIndexOf('/');
        if (slash <= 0) return null;
        int dir = dirId(path.substring(0, slash));
        if (dir == -2) return null;
        String name = path.substring(slash + 1);
        List<Rec> l = filesIn(dir);
        if (l.size() > 32) {
            java.util.concurrent.ConcurrentHashMap<Integer, java.util.HashMap<String, Rec>> all = names;
            if (all == null) names = all = new java.util.concurrent.ConcurrentHashMap<Integer, java.util.HashMap<String, Rec>>();
            java.util.HashMap<String, Rec> m = all.get(dir);
            if (m == null) {
                m = new java.util.HashMap<String, Rec>(l.size() * 2);
                for (Rec r : l) if (!r.gone) m.put(r.name, r);
                all.put(dir, m);
            }
            Rec r = m.get(name);
            if (r == null || !r.gone) return r;
        }
        for (Rec r : l) if (!r.gone && r.name.equals(name)) return r;
        return null;
    }

    private volatile java.util.concurrent.ConcurrentHashMap<Integer, java.util.HashMap<String, Rec>> names;

    /** מסמן קובץ כנעלם, לפי המסלול שלו. על עותק בלבד. מחזיר אם נמצא. */
    public boolean removeFile(String path) {
        int slash = path.lastIndexOf('/');
        if (slash <= 0) return false;
        int dir = dirId(path.substring(0, slash));
        if (dir == -2) return false;
        String name = path.substring(slash + 1);
        for (Rec r : filesIn(dir))
            if (!r.gone && r.name.equals(name)) {
                gone(r);
                return true;
            }
        return false;
    }

    // ---------- סריקה ----------

    private Dir newDir(int parent, File f) {
        return newDir(parent, f.getName(), f.lastModified());
    }

    private Dir newDir(int parent, String name, long mtime) {
        Dir d = new Dir();
        d.parent = parent;
        d.name = name;
        if (!deferNorm) d.norm = Text.norm(d.name);
        d.mtime = mtime;
        d.trash = parent < 0 ? d.name.equals(TRASH) : dirs.get(parent).trash;
        d.hidden = d.name.startsWith(".") || (parent >= 0 && dirs.get(parent).hidden);
        dirs.add(d);
        java.util.HashMap<Integer, List<Integer>> k = kids;
        if (k != null) add(k, parent, dirs.size() - 1);
        return d;
    }

    /** תיקיות שהמשתמש הוציא מהאינדקס (76). נקבע מההגדרות לפני כל בנייה ורענון. */
    public static volatile java.util.Set<String> EXCLUDED = new HashSet<String>();

    /** דילוג בסריקה: מוסתרים נכנסים (עם סימון, 63); לא נכנסים Android/data ו-obb, ומה שהוצא. */
    private static boolean skip(int parent, File f) {
        if (Vols.skip(f, true)) return true;
        java.util.Set<String> ex = EXCLUDED;
        return !ex.isEmpty() && ex.contains(f.getAbsolutePath());
    }

    // 184: מי בתוך מי — נבנה פעם אחת כשצריך (תוכן תיקייה, מסלול ← מזהה, רענון), ומתעדכן עם כל תוספת.
    // קודם: כל שאלה עברה על כל הקבצים (תיקייה אחת במיליון קבצים — 10 מ"ש במחשב, פי כמה בטלפון),
    // ומחיקת עץ גדול עברה על כל התיקיות לכל תיקייה (ריבועי). מתבטל כשהמזהים זזים (compact, סריקה).
    private volatile java.util.HashMap<Integer, List<Rec>> byDir;
    private volatile java.util.HashMap<Integer, List<Integer>> kids;

    private java.util.HashMap<Integer, List<Rec>> byDir() {
        java.util.HashMap<Integer, List<Rec>> m = byDir;
        if (m != null) return m;
        synchronized (this) {
            if (byDir != null) return byDir;
            m = new java.util.HashMap<Integer, List<Rec>>();
            for (Rec r : files) add(m, r.dir, r);
            byDir = m;
            return m;
        }
    }

    private java.util.HashMap<Integer, List<Integer>> kids() {
        java.util.HashMap<Integer, List<Integer>> m = kids;
        if (m != null) return m;
        synchronized (this) {
            if (kids != null) return kids;
            m = new java.util.HashMap<Integer, List<Integer>>();
            for (int i = 0; i < dirs.size(); i++) add(m, dirs.get(i).parent, i);
            kids = m;
            return m;
        }
    }

    private static <T> void add(java.util.HashMap<Integer, List<T>> m, int key, T v) {
        List<T> l = m.get(key);
        if (l == null) m.put(key, l = new ArrayList<T>(4));
        l.add(v);
    }

    /** 184: בסריקה המקבילית ובטעינה — הנרמול נדחה לסוף, ונעשה בכל הליבות (fillNorms). */
    private boolean deferNorm;

    /**
     * 184: השמות המנורמלים (לחיפוש) — מחולקים בין כל ליבות המעבד. קודם — בחוט אחד, אחד אחד:
     * בטעינת מיליון קבצים — כמחצית מהזמן.
     */
    void fillNorms() {
        deferNorm = false;
        int nd = dirs.size(), nf = files.size();
        int cores = Math.max(1, Math.min(8, Runtime.getRuntime().availableProcessors()));
        if (cores > 1 && nd + nf >= 20000) parallelNorms(nd, nf, cores);
        for (Dir d : dirs) if (d.norm == null) d.norm = Text.norm(d.name);             // קטן — הכול כאן; גדול — מה שנשאר
        for (Rec r : files) if (r.norm == null) {
            r.norm = Text.norm(r.name);
            r.loose = Text.loose(r.norm);
        }
    }

    private void parallelNorms(final int nd, final int nf, int cores) {
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(cores);
        final int step = Math.max(1, (nd + nf) / (cores * 4) + 1);
        for (int s = 0; s < nd + nf; s += step) {
            final int a = s, b = Math.min(nd + nf, s + step);
            pool.execute(new Runnable() {
                @Override
                public void run() {
                    for (int i = a; i < b; i++) {
                        if (i < nd) {
                            Dir d = dirs.get(i);
                            if (d.norm == null) d.norm = Text.norm(d.name);
                        } else {
                            Rec r = files.get(i - nd);
                            if (r.norm == null) {
                                String n = Text.norm(r.name);
                                r.loose = Text.loose(n);
                                r.norm = n;
                            }
                        }
                    }
                }
            });
        }
        pool.shutdown();
        try {
            pool.awaitTermination(10, java.util.concurrent.TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** המזהים זזו (או התחלה מחדש): מה שנבנה לפיהם — מתבטל. */
    private void moved() {
        backupDirs = null;
        names = null;
        goneByKey = null;
        byDir = null;
        kids = null;
        paths = null;
    }

    private void newRec(int parent, File f) {
        newRec(parent, f.getName(), f.length(), f.lastModified());
        if (!fullScan) {
            Rec r = files.get(files.size() - 1);
            r.added = carried(r);
        }
    }

    // ---------- 189: "נוסף" ----------

    /** 189: נטען מקובץ בגרסה 1 (בלי "נוסף") — Library ממלא מהמאגר פעם אחת (seedAdded). לא עובר בהעתקה. */
    boolean noAdded;

    /** בסריקה מההתחלה "נוסף" לא ידוע (0) — הוא נלקח אחר כך מהאינדקס הקודם או מהמאגר (Library.carryAdded). */
    private boolean fullScan;

    /**
     * קובץ שנעלם (העברה, שינוי שם, סל, שחזור — בתוכנה או מחוצה לה) ומופיע במקום אחר באותו כונן: אותו
     * גודל ואותו תאריך שינוי. מכאן "נוסף" של קובץ חדש — של זה שנעלם, ואם אין כזה — עכשיו.
     */
    private java.util.HashMap<String, Rec> goneByKey;

    private static String goneKey(Rec r) {
        return r.size + "|" + r.mtime;
    }

    private void gone(Rec r) {
        r.gone = true;
        if (goneByKey != null && r.size > 0) goneByKey.put(goneKey(r), r);
    }

    /** "נוסף" — של הקובץ שנעלם באותו גודל ותאריך, ואם אין — עכשיו. 190: גם החתימה עוברת (אותו תוכן). */
    private long carried(Rec r) {
        if (r.size <= 0) return System.currentTimeMillis();
        if (goneByKey == null) {
            goneByKey = new java.util.HashMap<String, Rec>();
            for (Rec x : files) if (x.gone && x.size > 0) goneByKey.put(goneKey(x), x);
        }
        Rec was = goneByKey.remove(goneKey(r));
        if (was == null) return System.currentTimeMillis();
        r.sig = was.sig;
        return was.added;
    }

    private void newRec(int parent, String name, long size, long mtime) {
        Rec r = new Rec();
        r.dir = parent;
        r.name = name;
        if (!deferNorm) {
            r.norm = Text.norm(r.name);
            r.loose = Text.loose(r.norm);
        }
        r.size = size;
        r.mtime = mtime;
        files.add(r);
        java.util.HashMap<Integer, List<Rec>> m = byDir;
        if (m != null) add(m, parent, r);
        names = null;
    }

    private List<Rec> filesIn(int dir) {
        List<Rec> l = byDir().get(dir);
        return l != null ? l : java.util.Collections.<Rec>emptyList();
    }

    public void scan(Progress progress) {
        fullScan = true;
        try {
            scanAll(progress);
        } finally {
            fullScan = false;
        }
    }

    private void scanAll(Progress progress) {
        dirs.clear();
        files.clear();
        moved();
        rootMtime = new File(rootPath).lastModified();
        ArrayDeque<Integer> parents = new ArrayDeque<Integer>();
        ArrayDeque<File> paths = new ArrayDeque<File>();
        parents.add(-1);
        paths.add(new File(rootPath));
        while (!paths.isEmpty()) {
            int parent = parents.poll();
            File[] all = paths.poll().listFiles();
            if (all == null) continue;
            for (File f : all) {
                if (skip(parent, f)) continue;
                if (f.isDirectory()) {
                    newDir(parent, f);
                    parents.add(dirs.size() - 1);
                    paths.add(f);
                } else {
                    newRec(parent, f);
                }
            }
            if (progress != null && files.size() - reported >= 1024) {           // 107: כל 1,024 קבצים, ולא רק בכפולה מדויקת
                reported = files.size();
                progress.onScan(label, files.size());
            }
        }
        built = System.currentTimeMillis();
    }

    // ---------- 177: סריקה בכמה קוראים במקביל ----------

    /** מה שקורא אחד קרא מתיקייה אחת: שמות, תיקייה או קובץ, גודל ותאריך (null בשם — דילוג). */
    private static final class Listing {
        int parent;
        File dir;
        String[] names;
        boolean[] isDir;
        long[] size, mtime;
    }

    private static Listing read(int parent, File dir) {
        Listing l = new Listing();
        l.parent = parent;
        l.dir = dir;
        File[] all = dir.listFiles();
        if (all == null) return l;
        int n = all.length;
        l.names = new String[n];
        l.isDir = new boolean[n];
        l.size = new long[n];
        l.mtime = new long[n];
        for (int i = 0; i < n; i++) {
            File f = all[i];
            if (skip(parent, f)) continue;
            l.names[i] = f.getName();
            l.isDir[i] = f.isDirectory();
            if (!l.isDir[i]) l.size[i] = f.length();
            l.mtime[i] = f.lastModified();
        }
        return l;
    }

    /**
     * סריקה ב־readers קוראים במקביל (1 — הסריקה הרגילה). הקוראים רק קוראים מהכונן (תוכן תיקייה, גודל, תאריך);
     * המספור (מזהי התיקיות) — בחוט הזה, לפי סדר ההגעה: ההורה תמיד לפני הילד. התוצאה — אותם קבצים ותיקיות.
     */
    public void scan(Progress progress, int readers) {
        if (readers <= 1) {
            scan(progress);
            return;
        }
        fullScan = true;
        dirs.clear();
        files.clear();
        moved();
        rootMtime = new File(rootPath).lastModified();
        deferNorm = true;
        final java.util.concurrent.LinkedBlockingQueue<Listing> done = new java.util.concurrent.LinkedBlockingQueue<Listing>();
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(readers);
        int pending = 0;
        try {
            pending += submit(pool, done, -1, new File(rootPath));
            while (pending > 0) {
                Listing l = done.take();
                pending--;
                if (l.names == null) continue;
                for (int i = 0; i < l.names.length; i++) {
                    if (l.names[i] == null) continue;
                    if (l.isDir[i]) {
                        newDir(l.parent, l.names[i], l.mtime[i]);
                        pending += submit(pool, done, dirs.size() - 1, new File(l.dir, l.names[i]));
                    } else newRec(l.parent, l.names[i], l.size[i], l.mtime[i]);
                }
                if (progress != null && files.size() - reported >= 1024) {
                    reported = files.size();
                    progress.onScan(label, files.size());
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            pool.shutdownNow();
            fillNorms();
            fullScan = false;
        }
        built = System.currentTimeMillis();
    }

    private static int submit(java.util.concurrent.ExecutorService pool, final java.util.concurrent.BlockingQueue<Listing> done,
                              final int parent, final File dir) {
        pool.execute(new Runnable() {
            @Override
            public void run() {
                Listing l;
                try {
                    l = read(parent, dir);
                } catch (Throwable t) {
                    l = new Listing();                                  // תיקייה שלא נקראה — כמו listFiles == null
                    l.parent = parent;
                    l.dir = dir;
                }
                done.add(l);
            }
        });
        return 1;
    }

    /** תאריך שורש הכונן בבדיקה האחרונה (בזיכרון בלבד; 0 — לא נבדק, ואז השורש מסונכרן בבדיקה הראשונה). */
    long rootMtime;

    /** בודק תאריכי תיקיות וסורק מחדש רק את מה שהשתנה. מחזיר כמה תיקיות רועננו. */
    public int refresh() {
        return refresh(1);
    }

    /** 1.7.73: התאריכים של כל התיקיות — נקראים ב־readers קוראים; העדכון עצמו (רק מה שהשתנה) — כאן, לפי הסדר. */
    public int refresh(int readers) {
        long[] t = times(readers);
        return t == null ? 0 : apply(t);
    }

    /**
     * 184: קריאה בלבד — התאריכים של כל התיקיות (במקום האחרון — של השורש), או null אם הכונן הוצא.
     * לא משנה כלום, ולכן אפשר על התמונה המוצגת עצמה, בלי להעתיק את כל האינדקס בכל בדיקה.
     */
    public long[] times(int readers) {
        final File root = new File(rootPath);
        if (!root.isDirectory()) return null;               // כונן שהוצא: לא מוחקים את האינדקס שלו
        final int n = dirs.size();
        final long[] times = new long[n + 1];
        final String[] raw = n > 0 ? paths().raw : new String[0];
        final boolean[] gone = new boolean[n];
        for (int i = 0; i < n; i++) gone[i] = dirs.get(i).gone;
        if (readers <= 1 || n < 64) {
            for (int i = 0; i < n; i++) if (!gone[i]) times[i] = new File(raw[i]).lastModified();
        } else {
            java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(readers);
            final int step = Math.max(1, n / (readers * 8));
            for (int s = 0; s < n; s += step) {
                final int a = s, b = Math.min(n, s + step);
                pool.execute(new Runnable() {
                    @Override
                    public void run() {
                        for (int i = a; i < b; i++) if (!gone[i]) times[i] = new File(raw[i]).lastModified();
                    }
                });
            }
            pool.shutdown();
            try {
                pool.awaitTermination(5, java.util.concurrent.TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        times[n] = root.lastModified();
        return times;
    }

    /** 184: האם התאריכים שנקראו שונים ממה שבאינדקס (תיקייה, או השורש). */
    public boolean differs(long[] times) {
        if (times == null) return false;
        int n = Math.min(dirs.size(), times.length - 1);
        for (int i = 0; i < n; i++) {
            Dir d = dirs.get(i);
            if (!d.gone && times[i] != d.mtime) return true;
        }
        return times[times.length - 1] != rootMtime;
    }

    /** 184: העדכון עצמו — רק התיקיות שתאריכן השתנה (times — מ־times(), על אינדקס עם אותם מזהים). */
    public int apply(long[] times) {
        int n = Math.min(dirs.size(), times.length - 1);
        int changed = 0;
        for (int i = 0; i < n; i++) {
            Dir d = dirs.get(i);
            if (d.gone) continue;
            long t = times[i];
            if (t == 0) {
                removeSubtree(i);
                changed++;
            } else if (t != d.mtime) {
                d.mtime = t;
                syncChildren(i, new File(dirPath(i)));
                changed++;
            }
        }
        long rt = times[times.length - 1];
        if (rt != rootMtime) {                               // השורש: רק כשהשתנה (קודם — בכל בדיקה)
            rootMtime = rt;
            File root = new File(rootPath);
            if (root.isDirectory() && syncChildren(-1, root)) changed++;    // 184: קובץ חדש בשורש — נספר (קודם לא, ולא הוצג עד שינוי אחר)
        }
        built = System.currentTimeMillis();
        return changed;
    }

    /** 107: מתי דווחה ההתקדמות לאחרונה (מספר קבצים). */
    private int reported;

    /**
     * רענון מעמיק (108, 114): גודל ותאריך של כל הקבצים, גם בתיקיות שלא השתנו (קובץ שנכתב במקום),
     * וקובץ שנעלם — יוצא. קריאת מערכת אחת לקובץ. מחזיר כמה השתנו.
     */
    public int deepRefresh() {
        return deepRefresh(1);
    }

    /** 184: הקריאה מהכונן — ב־readers קוראים (כמו התאריכים ברענון); העדכון — כאן, לפי הסדר. */
    public int deepRefresh(int readers) {
        return deepRefresh(readers, null);
    }

    /** 182: only — רק הקבצים המסומנים (null — כולם). מחזיר כמה השתנו. */
    int deepRefresh(int readers, boolean[] only) {
        final int n = files.size();
        final long[] len = new long[n], mt = new long[n];
        final String[] p = new String[n];
        for (int i = 0; i < n; i++) {
            Rec r = files.get(i);
            if (!r.gone && (only == null || (i < only.length && only[i]))) p[i] = path(r);
        }
        int threads = Math.max(1, Math.min(readers, 16));
        final int step = Math.max(1, (n + threads * 8 - 1) / (threads * 8));
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(threads);
        for (int s = 0; s < n; s += step) {
            final int a = s, b = Math.min(n, s + step);
            pool.execute(new Runnable() {
                @Override
                public void run() {
                    for (int i = a; i < b; i++) {
                        if (p[i] == null) continue;
                        File f = new File(p[i]);
                        mt[i] = f.lastModified();
                        len[i] = mt[i] == 0 && !f.exists() ? -1 : f.length();
                    }
                }
            });
        }
        pool.shutdown();
        try {
            pool.awaitTermination(30, java.util.concurrent.TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return 0;
        }
        int changed = 0;
        for (int i = 0; i < n; i++) {
            if (p[i] == null) continue;
            Rec r = files.get(i);
            if (len[i] == -1) {
                gone(r);
                changed++;
            } else if (len[i] != r.size || mt[i] != r.mtime) {
                r.size = len[i];
                r.mtime = mt[i];
                r.sig = 0;                                                  // 190
                changed++;
            }
        }
        return changed;
    }

    // ---------- 182: מה שהמאגר של אנדרואיד מדווח, ומה שאינו מכיר ----------

    /**
     * 182: האם קובץ במסלול הזה אמור להיות באינדקס — בתוך הכונן, לא ב־Android/data או obb, ולא בתיקייה
     * שהוצאה מהאינדקס (76). (מוסתרים — כן, עם סימון, כמו בסריקה.)
     */
    boolean indexable(String path) {
        String root = rootPath.endsWith("/") ? rootPath : rootPath + "/";
        if (path == null || !path.startsWith(root) || path.length() == root.length()) return false;
        java.util.Set<String> ex = EXCLUDED;
        for (int i = path.indexOf('/', root.length()); i > 0; i = path.indexOf('/', i + 1)) {
            String p = path.substring(0, i);
            if (p.endsWith("/Android/data") || p.endsWith("/Android/obb")) return false;
            if (!ex.isEmpty() && ex.contains(p)) return false;
        }
        return true;
    }

    /**
     * 182: מתוך המסלולים שהמאגר דיווח — אלה שבאמת צריכים עדכון: קובץ שקיים בכונן ואינו באינדקס, או שגודלו
     * או תאריכו שונים. קריאה בלבד (אפשר על התמונה המוצגת). קובץ שהמאגר מדווח ואינו בכונן — לא נכנס.
     */
    List<String> stale(List<String> reported) {
        List<String> out = new ArrayList<String>();
        if (reported == null) return out;
        Set<String> seen = new HashSet<String>();
        for (String raw : reported) {
            String p = Media.onDrive(this, raw);
            if (p == null || !seen.add(p) || !indexable(p)) continue;
            File f = new File(p);
            if (!f.isFile()) continue;
            Rec r = find(p);
            if (r == null || r.size != f.length() || r.mtime != f.lastModified()) out.add(p);
        }
        return out;
    }

    /** 182: מעדכן את הקבצים האלה (מ־stale) — גודל ותאריך, או מוסיף. על עותק בלבד. מחזיר כמה עודכנו. */
    int update(List<String> paths) {
        int n = 0;
        for (String p : paths) {
            if (!indexable(p)) continue;
            File f = new File(p);
            if (!f.isFile()) continue;
            Rec r = find(p);
            if (r != null && r.size == f.length() && r.mtime == f.lastModified()) continue;
            if (addFile(f)) n++;
        }
        return n;
    }

    /**
     * 182: הקבצים שהמאגר של אנדרואיד אינו מכיר — מוסתרים (שם בנקודה, או בתיקייה מוסתרת), וכל מה שבתיקייה
     * שיש בה ".nomedia" או בתוך תיקייה כזו. רק להם הבדיקה המלאה היומית; את השאר — "מה השתנה מאז".
     */
    boolean[] unknownToMedia() {
        int nd = dirs.size();
        boolean[] mark = new boolean[nd];
        boolean rootNomedia = false;
        for (Rec r : files)
            if (!r.gone && r.name.equalsIgnoreCase(".nomedia")) {
                if (r.dir < 0) rootNomedia = true;
                else if (r.dir < nd) mark[r.dir] = true;
            }
        for (int i = 0; i < nd; i++) {                                  // ההורה תמיד לפני הילד
            Dir d = dirs.get(i);
            if (d.hidden || rootNomedia || (d.parent >= 0 && d.parent < i && mark[d.parent])) mark[i] = true;
        }
        boolean[] out = new boolean[files.size()];
        for (int i = 0; i < out.length; i++) {
            Rec r = files.get(i);
            out[i] = !r.gone && (rootNomedia || r.name.startsWith(".") || (r.dir >= 0 && r.dir < nd && mark[r.dir]));
        }
        return out;
    }

    /**
     * 182: ההבדלים בין אינדקס חדש (בנייה מההתחלה) לבין הקודם — {נוספו, נמחקו, השתנו}. לתקופת הניסיון:
     * מה הבנייה השבועית מצאה מעבר לבדיקה השוטפת.
     */
    static int[] diff(Index old, Index fresh) {
        int added = 0, changed = 0, matched = 0, oldLive = 0;
        for (Rec r : old.files)
            if (!r.gone && (r.dir < 0 || (r.dir < old.dirs.size() && !old.dirs.get(r.dir).gone))) oldLive++;
        for (Rec r : fresh.files) {
            if (r.gone || (r.dir >= 0 && (r.dir >= fresh.dirs.size() || fresh.dirs.get(r.dir).gone))) continue;
            Rec o = old.find(fresh.path(r));
            if (o == null) {
                added++;
                continue;
            }
            matched++;
            if (o.size != r.size || o.mtime != r.mtime) changed++;
        }
        return new int[]{added, Math.max(0, oldLive - matched), changed};
    }

    /** מחזיר אם משהו השתנה (נוסף, יצא, גודל או תאריך). */
    private boolean syncChildren(int id, File dir) {
        File[] all = dir.listFiles();
        if (all == null) return false;
        boolean changed = false;
        Set<String> diskFiles = new HashSet<String>(), diskDirs = new HashSet<String>();
        for (File f : all) {
            if (skip(id, f)) continue;
            if (f.isDirectory()) diskDirs.add(f.getName()); else diskFiles.add(f.getName());
        }
        Set<String> knownFiles = new HashSet<String>(), knownDirs = new HashSet<String>();
        for (Rec r : filesIn(id)) {
            if (r.gone) continue;
            if (diskFiles.contains(r.name)) {
                knownFiles.add(r.name);
                File f = new File(dir, r.name);                 // 108: קובץ ידוע — גם גודל ותאריך
                long len = f.length(), mt = f.lastModified();
                if (len != r.size || mt != r.mtime) {
                    r.size = len;
                    r.mtime = mt;
                    r.sig = 0;                                              // 190
                    changed = true;
                }
            } else {
                gone(r);
                changed = true;
            }
        }
        List<Integer> mine = kids().get(id);
        if (mine != null)
            for (int i : new ArrayList<Integer>(mine)) {
                Dir d = dirs.get(i);
                if (d.gone) continue;
                if (diskDirs.contains(d.name)) knownDirs.add(d.name);
                else {
                    removeSubtree(i);
                    changed = true;
                }
            }
        for (File f : all) {
            if (skip(id, f)) continue;
            if (f.isDirectory()) {
                if (knownDirs.contains(f.getName())) continue;
                newDir(id, f);
                scanInto(dirs.size() - 1, f);
                changed = true;
            } else if (!knownFiles.contains(f.getName())) {
                newRec(id, f);
                changed = true;
            }
        }
        return changed;
    }

    private void scanInto(int parent, File dir) {
        File[] all = dir.listFiles();
        if (all == null) return;
        for (File f : all) {
            if (skip(parent, f)) continue;
            if (f.isDirectory()) {
                newDir(parent, f);
                scanInto(dirs.size() - 1, f);
            } else {
                newRec(parent, f);
            }
        }
    }

    private void removeSubtree(int id) {
        ArrayDeque<Integer> todo = new ArrayDeque<Integer>();       // 184: לפי "מי בתוך מי", בלי רקורסיה עמוקה
        todo.add(id);
        while (!todo.isEmpty()) {
            int x = todo.poll();
            dirs.get(x).gone = true;
            for (Rec r : filesIn(x)) gone(r);
            List<Integer> l = kids().get(x);
            if (l != null) for (int k : l) if (!dirs.get(k).gone) todo.add(k);
        }
    }

    // ---------- קובץ ----------

    /**
     * 184: שמירה בלי לשנות את האינדקס — מה שנעלם פשוט לא נכתב, והמזהים מכווצים בדרך. כך אפשר לשמור
     * את התמונה המוצגת עצמה, בלי להעתיק קודם את כל האינדקס (במיליון קבצים — כפל זיכרון בכל שמירה).
     */
    public void save() throws IOException {
        int nDirs = dirs.size();
        int[] map = new int[nDirs];
        int keptDirs = 0;
        for (int i = 0; i < nDirs; i++) {
            Dir x = dirs.get(i);
            boolean lost = x.gone || (x.parent >= 0 && (x.parent >= i || map[x.parent] < 0));
            map[i] = lost ? -2 : keptDirs++;
        }
        int keptFiles = 0;
        for (Rec r : files) if (kept(r, map)) keptFiles++;
        File target = Paths.index(key);
        File tmp = new File(target.getParentFile(), "." + target.getName() + "." + System.nanoTime() + ".tmp");
        FileOutputStream fos = new FileOutputStream(tmp);
        DataOutputStream d = new DataOutputStream(new BufferedOutputStream(fos, 1 << 16));
        try {
            d.writeInt(MAGIC);
            d.writeInt(VERSION);
            d.writeUTF(rootPath);
            d.writeUTF(label);
            d.writeLong(built);
            d.writeInt(keptDirs);
            for (int i = 0; i < nDirs; i++) {
                if (map[i] < 0) continue;
                Dir x = dirs.get(i);
                d.writeInt(x.parent < 0 ? x.parent : map[x.parent]);
                d.writeLong(x.mtime);
                d.writeUTF(x.name);
            }
            d.writeInt(keptFiles);
            for (Rec r : files) {
                if (!kept(r, map)) continue;
                d.writeInt(r.dir < 0 ? r.dir : map[r.dir]);
                d.writeLong(r.size);
                d.writeLong(r.mtime);
                d.writeLong(r.added);
                d.writeLong(r.sig);
                d.writeUTF(r.name);
            }
            d.flush();
            fos.getFD().sync();
        } finally {
            d.close();
        }
        if (!tmp.renameTo(target)) {
            target.delete();
            if (!tmp.renameTo(target)) throw new IOException(Words.Idx.CANT_SAVE_INDEX);
        }
    }

    private static boolean kept(Rec r, int[] map) {
        return !r.gone && (r.dir < 0 || (r.dir < map.length && map[r.dir] >= 0));
    }

    /** null אם אין קובץ, אם הגרסה שונה או אם הוא פגום. */
    public static Index load(String key) {
        File f = Paths.index(key);
        if (!f.exists()) return null;
        DataInputStream d = null;
        try {
            d = new DataInputStream(new BufferedInputStream(new FileInputStream(f), 1 << 16));
            if (d.readInt() != MAGIC) return null;
            int version = d.readInt();
            if (version < 1 || version > VERSION) return null;
            Index ix = new Index(key, "", d.readUTF());
            ix.noAdded = version < 2;
            ix.label = d.readUTF();
            ix.built = d.readLong();
            int nd = d.readInt();
            ix.dirs.ensureCapacity(nd);
            for (int i = 0; i < nd; i++) {
                Dir x = new Dir();
                x.parent = d.readInt();
                x.mtime = d.readLong();
                x.name = d.readUTF();
                x.trash = x.parent < 0 ? x.name.equals(TRASH)
                        : x.parent < ix.dirs.size() && ix.dirs.get(x.parent).trash;
                x.hidden = x.name.startsWith(".")
                        || (x.parent >= 0 && x.parent < ix.dirs.size() && ix.dirs.get(x.parent).hidden);
                ix.dirs.add(x);
            }
            int nf = d.readInt();
            ix.files.ensureCapacity(nf);
            for (int i = 0; i < nf; i++) {
                Rec r = new Rec();
                r.dir = d.readInt();
                r.size = d.readLong();
                r.mtime = d.readLong();
                if (version >= 2) r.added = d.readLong();
                if (version >= 3) r.sig = d.readLong();
                r.name = d.readUTF();
                ix.files.add(r);
            }
            ix.fillNorms();                                         // 184: בכל הליבות
            return ix;
        } catch (Throwable t) {
            Log.e("טעינת אינדקס " + key, t);
            return null;
        } finally {
            if (d != null) try { d.close(); } catch (IOException ignored) { }
        }
    }
}
