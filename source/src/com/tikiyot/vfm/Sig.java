package com.tikiyot.vfm;

// 190: חתימת תוכן — לכל הקבצים באינדקס (Index.Rec.sig), ולפריטים שברשימות גם בקובץ — זיהוי בטוח של הקובץ
// (איתור החסרים, מעבר בין מכשירים, ובהמשך כפולים ושיבוש).
// חלקית: הגודל, ותחילת הקובץ, אמצעו וסופו (16KB מכל אחד; קובץ קטן — כולו). SHA-1, 8 הבתים הראשונים.
// אחראי: החישוב, הקובץ "חתימות.txt" באופן העבודה (הפריטים שברשימות — נשלח עם "שיתוף ההגדרות"), והעבודה ברקע.
// אסור לו: לשנות קבצים או רשימות.
// שורה בקובץ: חתימה <טאב> גודל <טאב> תאריך שינוי <טאב> מסלול (המסלול אחרון — יכול להכיל הכול).

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class Sig {

    private Sig() { }

    static final String FILE = "חתימות.txt";                                                // בכונן
    static final int PART = 16 * 1024;
    /** גרסת החישוב — אם ישתנה, חתימה בגרסה אחרת אינה מושווית. */
    static final String VER = "2:";

    static final class Entry {
        final long size, mtime;
        final String sig;

        Entry(long size, long mtime, String sig) {
            this.size = size;
            this.mtime = mtime;
            this.sig = sig;
        }
    }

    /** לבדיקות. */
    static File testFile;

    static File file() {
        return testFile != null ? testFile : new File(Modes.dir(), FILE);
    }

    // ---------- החישוב ----------

    /** החתימה של הקובץ, או null אם לא נקרא. */
    static String of(File f) {
        long v = ofLong(f);
        return v == 0 ? null : str(v);
    }

    /** בצורת הטקסט: "2:" ו־16 ספרות הקסה. */
    static String str(long v) {
        return VER + String.format(java.util.Locale.US, "%016x", v);
    }

    /** החתימה כמספר (0 — לא נקרא). */
    static long ofLong(File f) {
        RandomAccessFile raf = null;
        try {
            long n = f.length();
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-1");
            for (int i = 7; i >= 0; i--) md.update((byte) (n >>> (8 * i)));
            raf = new RandomAccessFile(f, "r");
            byte[] buf = new byte[PART];
            if (n <= 3L * PART) {
                int r;
                while ((r = raf.read(buf)) > 0) md.update(buf, 0, r);
            } else {
                for (long at : new long[]{0, n / 2 - PART / 2, n - PART}) {
                    raf.seek(at);
                    raf.readFully(buf);
                    md.update(buf);
                }
            }
            byte[] d = md.digest();
            long v = 0;
            for (int i = 0; i < 8; i++) v = (v << 8) | (d[i] & 0xff);
            return v == 0 ? 1 : v;
        } catch (Throwable t) {
            return 0;
        } finally {
            if (raf != null) try { raf.close(); } catch (Throwable ignored) { }
        }
    }

    // ---------- הקובץ ----------

    private static Map<String, Entry> map;
    private static File loadedFrom;
    private static boolean dirty;

    private static Map<String, Entry> map() {
        File f = file();
        if (map == null || !f.equals(loadedFrom)) {
            map = new LinkedHashMap<String, Entry>();
            loadedFrom = f;
            dirty = false;
            try {
                if (f.exists())
                    for (String l : Files.readUtf8(f).split("\n")) {
                        String[] p = l.split("\t", 4);
                        if (p.length < 4 || !p[0].startsWith(VER)) continue;
                        try {
                            map.put(p[3], new Entry(Long.parseLong(p[1]), Long.parseLong(p[2]), p[0]));
                        } catch (NumberFormatException ignored) {
                        }
                    }
            } catch (Throwable t) {
                Log.e("קריאת החתימות", t);
            }
        }
        return map;
    }

    static synchronized void save() {
        if (map == null || !dirty) return;
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Entry> e : map.entrySet())
            sb.append(e.getValue().sig).append('\t').append(e.getValue().size).append('\t')
                    .append(e.getValue().mtime).append('\t').append(e.getKey()).append('\n');
        try {
            Files.writeAtomic(loadedFrom, sb.toString());
            dirty = false;
        } catch (Throwable t) {
            Log.e("שמירת החתימות", t);
        }
    }

    /** מה שנשמר למסלול (גם כשהקובץ כבר אינו שם — לאיתור), או null. */
    static synchronized Entry stored(String path) {
        return map().get(path);
    }

    /** חתימה תקפה: נשמרה לאותו גודל ותאריך שינוי. אחרת — null. */
    static synchronized String valid(String path, long size, long mtime) {
        Entry e = map().get(path);
        return e != null && e.size == size && e.mtime == mtime ? e.sig : null;
    }

    static synchronized void put(String path, long size, long mtime, String sig) {
        if (sig == null) return;
        map().put(path, new Entry(size, mtime, sig));
        dirty = true;
    }

    /** הקבצים עברו (איתור): החתימה עוברת עם המסלול. */
    static synchronized void moved(Map<String, String> moved) {
        Map<String, Entry> m = map();
        for (Map.Entry<String, String> e : moved.entrySet()) {
            Entry x = m.remove(e.getKey());
            if (x != null) m.put(e.getValue(), x);
        }
        dirty = true;
        save();
    }

    /** לבדיקות ולמעבר אופן: לשכוח את מה שבזיכרון. */
    static synchronized void forget() {
        map = null;
        loadedFrom = null;
        dirty = false;
    }

    // ---------- מה צריך חתימה ----------

    /** כל המסלולים שבכל הרשימות. */
    static Set<String> listed(Tree t) {
        Set<String> out = new java.util.LinkedHashSet<String>();
        collect(t.root, out);
        return out;
    }

    private static void collect(Tree.Node n, Set<String> out) {
        for (Tree.Entry e : n.entries) if (!e.bad) out.add(e.path);
        for (Tree.Node c : n.children) collect(c, out);
    }

    /** {כמה יש להם חתימה תקפה, כמה קבצים קיימים ברשימות}. */
    static final class Status {
        int done, total;
        /** מה שעוד צריך חתימה — מסלול, גודל, תאריך. */
        final List<String> todo = new ArrayList<String>();
        final List<long[]> todoMeta = new ArrayList<long[]>();

        int percent() {
            return total == 0 ? 100 : (int) (100L * done / total);
        }
    }

    /**
     * מה המצב, ומה עוד צריך: כל פריט ברשימות שהקובץ שלו קיים (לפי האינדקס; כשאינו מוכן — לפי הכונן). חתימה
     * של מסלול שאינו עוד באף רשימה — נמחקת; של פריט חסר — נשארת (היא שתאתר אותו).
     */
    static synchronized Status status(Library lib, Tree t) {
        Status s = new Status();
        Set<String> paths = listed(t);
        Map<String, Entry> m = map();
        if (!t.unreadable)                                     // הרשימות לא נקראו — לא מוחקים חתימות
        for (java.util.Iterator<String> it = m.keySet().iterator(); it.hasNext(); )
            if (!paths.contains(it.next())) {
                it.remove();
                dirty = true;
            }
        for (String p : paths) {
            long size, mtime;
            Library.Hit h = lib != null && lib.ready ? lib.find(p) : null;
            if (h != null && h.rec != null && !h.rec.gone) {
                size = h.rec.size;
                mtime = h.rec.mtime;
                long v = h.rec.sig;                                  // כבר חושבה באינדקס — בלי לקרוא שוב
                Entry e0 = m.get(p);
                if (v != 0 && (e0 == null || e0.size != size || e0.mtime != mtime || !e0.sig.equals(str(v)))) {
                    m.put(p, new Entry(size, mtime, str(v)));
                    dirty = true;
                }
            } else {
                File f = new File(p);
                if (!f.isFile()) continue;
                size = f.length();
                mtime = f.lastModified();
            }
            s.total++;
            Entry e = m.get(p);
            if (e != null && e.size == size && e.mtime == mtime) s.done++;
            else if (!failed.contains(p)) {
                s.todo.add(p);
                s.todoMeta.add(new long[]{size, mtime});
            }
        }
        return s;
    }

    /** קבצים שלא נקראו בהפעלה הזו — לא מנסים שוב בכל סבב. */
    private static final java.util.Set<String> failed = java.util.Collections.synchronizedSet(new java.util.HashSet<String>());

    /** מחשב עד n חתימות מתוך הרשימה. מחזיר כמה חושבו. (מחוץ לנעילה — הקריאה מהכונן.) */
    static int step(Status s, int n) {
        int done = 0;
        for (int i = 0; i < s.todo.size() && done < n; i++) {
            String p = s.todo.get(i);
            File f = new File(p);
            if (!f.isFile()) continue;
            String sig = of(f);
            long[] meta = s.todoMeta.get(i);
            if (sig != null && f.length() == meta[0] && f.lastModified() == meta[1]) put(p, meta[0], meta[1], sig);
            else if (sig == null) failed.add(p);
            done++;
        }
        save();
        return done;
    }

    // ---------- העבודה ברקע ----------

    private static volatile Thread worker;
    /** "זרז": בלי הפסקות בין המנות. */
    static volatile boolean fast;
    /** המצב האחרון שחושב — למסך ההגדרות. */
    static volatile int lastDone = -1, lastTotal = -1;

    static final int BATCH = 40;
    static final long PAUSE_MS = 1500;

    /** מתחיל את העבודה ברקע, אם אינה רצה. נגמרת כשאין עוד מה לחתום; הקריאה הבאה — מתחילה מחדש. */
    static void kick(final Library lib) {
        if (worker != null) return;
        synchronized (Sig.class) {
            if (worker != null) return;
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        for (int i = 0; i < 300 && !lib.ready; i++) Thread.sleep(200);
                        for (int round = 0; round < 20; round++) {           // קובץ שמשתנה כל הזמן — לא לנצח
                            Status s = status(lib, Tree.load());              // פעם אחת לסבב, לא לכל מנה
                            lastDone = s.done;
                            lastTotal = s.total;
                            if (s.todo.isEmpty()) {
                                save();
                                if (indexRound(lib) == 0) break;              // קודם הרשימות, ואחר כך כל הקבצים
                                continue;
                            }
                            int did = 0;
                            while (did < s.todo.size()) {
                                Status part = new Status();
                                int end = Math.min(s.todo.size(), did + (fast ? 4 * BATCH : BATCH));
                                part.todo.addAll(s.todo.subList(did, end));
                                part.todoMeta.addAll(s.todoMeta.subList(did, end));
                                step(part, part.todo.size());
                                did = end;
                                lastDone = s.done + did;
                                if (!fast) Thread.sleep(PAUSE_MS);
                            }
                        }
                    } catch (InterruptedException ignored) {
                    } catch (Throwable t) {
                        Log.e("חתימות ברקע", t);
                    } finally {
                        fast = false;
                        worker = null;
                    }
                }
            }, "חתימות");  // תחביר
            t.setPriority(Thread.MIN_PRIORITY);
            t.setDaemon(true);
            worker = t;
            t.start();
        }
    }

    static boolean running() {
        return worker != null;
    }

    /** 194: יש עוד עבודה (רצה עכשיו, או שלא כל הקבצים חתומים). */
    static boolean needed(Library lib) {
        if (running()) return true;
        int[] st = indexStatus(lib);
        return st[1] > 0 && st[0] < st[1];
    }

    /** 193: החתימה ברשומה — וגם בתמונה העדכנית, אם הוחלפה בינתיים. */
    static void write(Library lib, Index ix, Index.Rec r, String path, long v) {
        r.sig = v;
        Index now = lib == null ? null : lib.published(ix.key);
        if (now != null && now != ix) {
            Index.Rec cr = now.find(path);
            if (cr != null && cr.size == r.size && cr.mtime == r.mtime) cr.sig = v;
        }
    }

    /** המצב של כל הקבצים באינדקס (בלי הסל ובלי המוסתרים — מטמונים, קובצי מערכת): {חתומים, כולם}. */
    static int[] indexStatus(Library lib) {
        int done = 0, all = 0;
        if (lib == null || !lib.ready) return new int[]{-1, -1};
        for (Index ix : lib.indexes)
            for (Index.Rec r : ix.files)
                if (!r.gone && !ix.trash(r) && !ix.hidden(r)) {
                    all++;
                    if (r.sig != 0) done++;
                }
        return new int[]{done, all};
    }

    /**
     * סבב אחד על כל הקבצים שבאינדקס שאין להם חתימה — במנות, עם הפסקות. הכתיבה — ברשומה עצמה (גם בתמונה
     * המוצגת; תמונה שהוחלפה בינתיים — תושלם בסבב הבא). מחזיר כמה חושבו.
     */
    static int indexRound(Library lib) throws InterruptedException {
        int did = 0;
        for (Index ix : new ArrayList<Index>(lib.indexes)) {
            List<Index.Rec> todo = new ArrayList<Index.Rec>();
            for (Index.Rec r : ix.files)
                if (!r.gone && !ix.trash(r) && !ix.hidden(r) && r.sig == 0 && !failed.contains(ix.key + "#" + r.name + "#" + r.size)) todo.add(r);
            int sinceSave = 0;
            for (int i = 0; i < todo.size(); i++) {
                Index.Rec r = todo.get(i);
                File f = new File(ix.path(r));
                if (f.length() == r.size && f.lastModified() == r.mtime) {
                    long v = ofLong(f);
                    if (v != 0 && f.length() == r.size && f.lastModified() == r.mtime) {
                        write(lib, ix, r, f.getAbsolutePath(), v);
                        did++;
                        sinceSave++;
                    } else if (v == 0) failed.add(ix.key + "#" + r.name + "#" + r.size);
                }
                if ((i + 1) % (fast ? 4 * BATCH : BATCH) == 0) {
                    if (sinceSave >= 500) {
                        lib.sigsChanged(ix.key);
                        sinceSave = 0;
                    }
                    if (!fast) Thread.sleep(PAUSE_MS);
                }
            }
            if (sinceSave > 0) lib.sigsChanged(ix.key);
        }
        return did;
    }

    // ---------- האיתור ----------

    /** תוצאת האיתור: מה יתעדכן, ומה לא. */
    static final class Plan {
        /** אומת בחתימה (גם אם השם שונה). */
        final Map<String, String> verified = new LinkedHashMap<String, String>();
        /** לפי שם וגודל בלבד (לפריט אין חתימה). */
        final Map<String, String> byNameSize = new LinkedHashMap<String, String>();
        /** חסרים שנבדקו. */
        int missing;
        /** לפי שם וגודל — ביותר ממקום אחד (נבחר הדומה במסלול). */
        int several;
        /** נמצא קובץ באותו שם וגודל, אבל התוכן שונה — לא מתעדכן. */
        int differ;
        int notFound;
    }

    /**
     * לכל פריט חסר בכל הרשימות: מועמדים באינדקס לפי הגודל; עם חתימה שמורה — רק מי שהחתימה שלו זהה (גם בשם
     * אחר); בלי חתימה — אותו שם ואותו גודל. כמה מועמדים — הדומה ביותר בסוף המסלול. קריאה בלבד.
     */
    static Plan plan(Library lib, Tree t) {
        Plan pl = new Plan();
        Map<String, Long> missing = new LinkedHashMap<String, Long>();
        missingIn(lib, t.root, missing);
        pl.missing = missing.size();
        if (missing.isEmpty() || lib == null || !lib.ready) {
            pl.notFound = missing.size();
            return pl;
        }
        Map<Long, List<String>> bySize = new HashMap<Long, List<String>>();
        Map<String, Long> recSig = new HashMap<String, Long>();             // חתימות שכבר באינדקס
        for (Map.Entry<String, Long> e : missing.entrySet()) {
            long size = sizeOf(e.getKey(), e.getValue());
            if (size > 0) bySize.put(size, new ArrayList<String>());
        }
        for (Index ix : lib.indexes)
            for (Index.Rec r : ix.files)
                if (!r.gone && !ix.trash(r)) {
                    List<String> l = bySize.get(r.size);
                    if (l != null) {
                        String p = ix.path(r);
                        l.add(p);
                        if (r.sig != 0) recSig.put(p, r.sig);
                    }
                }
        Map<String, String> sigCache = new HashMap<String, String>();
        for (Map.Entry<String, Long> e : missing.entrySet()) {
            String old = e.getKey();
            long size = sizeOf(old, e.getValue());
            List<String> cands = size > 0 ? bySize.get(size) : null;
            String name = name(old);
            Entry st = stored(old);
            List<String> named = new ArrayList<String>();
            if (cands != null) for (String c : cands) if (name(c).equals(name)) named.add(c);
            if (st != null && st.size == size && cands != null) {
                List<String> same = new ArrayList<String>();
                for (String c : named.isEmpty() ? cands : concat(named, cands)) {
                    if (same.contains(c)) continue;
                    String sig = sigCache.get(c);
                    if (sig == null && recSig.containsKey(c)) sig = str(recSig.get(c));
                    if (sig == null) {
                        sig = of(new File(c));
                        if (sig != null) sigCache.put(c, sig);
                    }
                    if (st.sig.equals(sig)) same.add(c);
                    if (sigCache.size() > 5000) break;
                }
                if (!same.isEmpty()) pl.verified.put(old, closest(old, same));
                else if (!named.isEmpty()) pl.differ++;
                else pl.notFound++;
            } else if (!named.isEmpty()) {
                if (named.size() > 1) pl.several++;
                pl.byNameSize.put(old, closest(old, named));
            } else pl.notFound++;
        }
        return pl;
    }

    private static List<String> concat(List<String> a, List<String> b) {
        List<String> out = new ArrayList<String>(a);
        out.addAll(b);
        return out;
    }

    /** הגודל של פריט חסר: מהרשימה, ואם אינו בה — מהחתימה השמורה. */
    private static long sizeOf(String path, Long listed) {
        if (listed != null && listed > 0) return listed;
        Entry st = stored(path);
        return st != null ? st.size : -1;
    }

    /** הפריטים שהקובץ שלהם אינו במקומו (לא באינדקס ולא בכונן), עם הגודל שברשימה. */
    private static void missingIn(Library lib, Tree.Node n, Map<String, Long> out) {
        for (Tree.Entry e : n.entries) {
            if (e.bad || out.containsKey(e.path)) continue;
            Library.Hit h = lib != null && lib.ready ? lib.find(e.path) : null;
            if (h != null && h.rec != null && !h.rec.gone) continue;
            if (new File(e.path).isFile()) continue;
            out.put(e.path, e.size);
        }
        for (Tree.Node c : n.children) missingIn(lib, c, out);
    }

    private static String name(String path) {
        int i = path.lastIndexOf('/');
        return i >= 0 ? path.substring(i + 1) : path;
    }

    /** מבין המועמדים — זה שסוף המסלול שלו (התיקיות שמעל הקובץ) הכי דומה למסלול הישן. */
    static String closest(String old, List<String> cands) {
        String[] o = old.split("/");
        String best = cands.get(0);
        int bestScore = -1;
        for (String c : cands) {
            String[] p = c.split("/");
            int s = 0;
            for (int i = o.length - 1, j = p.length - 1; i >= 0 && j >= 0 && o[i].equals(p[j]); i--, j--) s++;
            if (s > bestScore) {
                bestScore = s;
                best = c;
            }
        }
        return best;
    }

    /** מעדכן את הרשימות (והחתימות) לפי מה שנמצא. מחזיר כמה פריטים עודכנו. */
    static int apply(Map<String, String> moved) {
        if (moved.isEmpty()) return 0;
        int n;
        synchronized (Tree.LOCK) {
            Tree t = Tree.load();
            n = 0;
            for (Map.Entry<String, String> e : moved.entrySet())
                n += t.replacePath(e.getKey(), e.getValue(), new File(e.getValue()).length());
            if (n > 0) t.save();
        }
        moved(moved);
        return n;
    }
}
