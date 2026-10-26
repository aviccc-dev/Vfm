package com.tikiyot.lab;

// מעבדת זירוזים 1.1 — המנוע. הבסיס הוא הבנייה של התוכנה היום: הכוננים במקביל, N קוראים, תור תיקיות,
// רושם אחד (נותן מקום לכל תיקייה, מכין את השמות לחיפוש ומכניס תתי־תיקיות לתור). חמשת השיפורים — כל אחד דגל:
//   1 fast   — שאלה אחת אמיתית לכל פריט (סוג, גודל ותאריך יחד) במקום שלוש;
//   2 seed   — מפה מהאינדקס הקודם: כל התיקיות הידועות לתור מההתחלה, הגדולות ראשונות;
//   3 split  — תיקייה גדולה מתחלקת לחבילות בין כמה קוראים;
//   4 push   — הקורא מכניס תתי־תיקיות לתור בעצמו, בלי לחכות לרושם;
//   5 names  — הכנת השמות לחיפוש אצל הקוראים, ולא אצל הרושם.
// בלי אנדרואיד: נבדק במחשב (ScanTest).

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class Scan {

    private Scan() { }

    public static final class Opts {
        public int readers = 4;
        public boolean fast, seed, split, push, names;
        /** 1.4: לאסוף את המסלולים של כל הקבצים (להשוואה עם המאגר של אנדרואיד). */
        public boolean collect;

        public Opts copy() {
            Opts o = new Opts();
            o.readers = readers;
            o.fast = fast;
            o.seed = seed;
            o.split = split;
            o.push = push;
            o.names = names;
            o.collect = collect;
            return o;
        }
    }

    /** קריאת סוג, גודל ותאריך של פריט: {1 — תיקייה / 0 — קובץ, גודל, תאריך}; null — לא נקרא. */
    public interface Stat {
        long[] read(File dir, String name);
    }

    /** שלוש שאלות (כמו היום). */
    public static final Stat THREE = new Stat() {
        @Override
        public long[] read(File dir, String name) {
            File f = new File(dir, name);
            boolean d = f.isDirectory();
            return new long[]{d ? 1 : 0, d ? 0 : f.length(), f.lastModified()};
        }
    };

    /** שאלה אחת — נותנת המעבדה באנדרואיד (Os.lstat); null — לא זמין (במחשב). */
    public static volatile Stat FAST;

    /** גודל חבילה בחלוקת תיקייה גדולה (3). */
    public static int CHUNK = 400;

    public static final class Result {
        public final String root;
        public final List<String> dirs = new ArrayList<String>();
        public final List<Long> mtimes = new ArrayList<Long>();
        /** כמה קבצים ישירות בכל תיקייה (גם השורש). */
        public final Map<String, Integer> counts = new HashMap<String, Integer>();
        public int files;
        public long ms;
        /** התיקייה הגדולה: שם, קבצים, ומתי הסתיימה (מתחילת הסריקה). */
        public String biggest = "";
        public int biggestFiles;
        public long biggestDoneMs;
        boolean fastUsed;
        /** 1.4: המסלולים (רק כש־collect). */
        public List<String> paths;
        /** כדי שהמחשב לא ידלג על הכנת השמות. */
        public long sink;

        Result(String root) {
            this.root = root;
        }
    }

    private static final class Listing {
        File dir;
        String[] names;
        long[][] stat;
        String[] norms;
    }

    /** "Android/data" ו־"obb" בשורש — כמו בתוכנה. */
    static boolean skipped(File dir, String name, String root) {
        return (name.equals("data") || name.equals("obb")) && dir.getName().equals("Android")
                && dir.getParentFile() != null && dir.getParentFile().getAbsolutePath().equals(root);
    }

    public static Result scan(final String root, final Opts o, Result prev) {
        final Stat st = o.fast && FAST != null ? FAST : THREE;
        final Result r = new Result(root);
        r.fastUsed = st != THREE;
        if (o.collect) r.paths = new ArrayList<String>();
        final long t0 = System.nanoTime();
        final LinkedBlockingQueue<Listing> done = new LinkedBlockingQueue<Listing>();
        final AtomicInteger pending = new AtomicInteger();
        final Set<String> queued = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
        final ExecutorService pool = Executors.newFixedThreadPool(Math.max(1, o.readers));
        final Map<String, Long> lastArrive = new HashMap<String, Long>();

        class Ctl {
            void dir(final File d) {
                if (!queued.add(d.getAbsolutePath())) return;
                pending.incrementAndGet();
                pool.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            readDir(d);
                        } catch (Throwable t) {
                            empty(d);
                        }
                    }
                });
            }

            void empty(File d) {
                Listing l = new Listing();
                l.dir = d;
                done.add(l);
            }

            void readDir(final File d) {
                final String[] all = d.list();
                if (all == null || all.length == 0) {
                    empty(d);
                    return;
                }
                if (o.split && all.length > CHUNK) {
                    for (int from = 0; from < all.length; from += CHUNK) {
                        final int a = from, b = Math.min(all.length, from + CHUNK);
                        pending.incrementAndGet();
                        pool.execute(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    done.add(stat(d, all, a, b));
                                } catch (Throwable t) {
                                    empty(d);
                                }
                            }
                        });
                    }
                    empty(d);                                                  // מה שהקורא עצמו מסר: כלום
                    return;
                }
                done.add(stat(d, all, 0, all.length));
            }

            Listing stat(File d, String[] all, int a, int b) {
                Listing l = new Listing();
                l.dir = d;
                l.names = new String[b - a];
                l.stat = new long[b - a][];
                if (o.names) l.norms = new String[b - a];
                for (int i = a; i < b; i++) {
                    String n = all[i];
                    if (skipped(d, n, root)) continue;
                    l.names[i - a] = n;
                    l.stat[i - a] = st.read(d, n);
                    if (l.stat[i - a] == null) continue;
                    if (o.names) {
                        String nn = Text.norm(n);
                        l.norms[i - a] = nn + Text.loose(nn);
                    }
                    if (o.push && l.stat[i - a][0] == 1) dir(new File(d, n));   // 4: בלי לחכות לרושם
                }
                return l;
            }
        }
        final Ctl c = new Ctl();
        if (o.seed && prev != null) {                                          // 2: הגדולות ראשונות
            List<String> known = new ArrayList<String>(prev.counts.keySet());
            final Map<String, Integer> cnt = prev.counts;
            Collections.sort(known, new Comparator<String>() {
                @Override
                public int compare(String a, String b) {
                    return cnt.get(b) - cnt.get(a);
                }
            });
            for (String p : known) if (!p.equals(root)) c.dir(new File(p));
        }
        c.dir(new File(root));
        try {
            while (pending.get() > 0) {
                Listing l = done.take();
                String dp = l.dir.getAbsolutePath();
                lastArrive.put(dp, System.nanoTime());
                if (l.names != null) for (int i = 0; i < l.names.length; i++) {
                    if (l.names[i] == null || l.stat[i] == null) continue;
                    if (l.stat[i][0] == 1) {
                        File d = new File(l.dir, l.names[i]);
                        r.dirs.add(d.getAbsolutePath());
                        r.mtimes.add(l.stat[i][2]);
                        if (!o.push) c.dir(d);
                    } else {
                        r.files++;
                        if (r.paths != null) r.paths.add(dp + "/" + l.names[i]);
                        Integer k = r.counts.get(dp);
                        r.counts.put(dp, k == null ? 1 : k + 1);
                        if (!o.names) {
                            String nn = Text.norm(l.names[i]);
                            r.sink += (nn + Text.loose(nn)).length();
                        } else r.sink += l.norms[i].length();
                    }
                }
                pending.decrementAndGet();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            pool.shutdownNow();
        }
        r.ms = (System.nanoTime() - t0) / 1000000;
        for (Map.Entry<String, Integer> e : r.counts.entrySet())
            if (e.getValue() > r.biggestFiles) {
                r.biggestFiles = e.getValue();
                r.biggest = e.getKey();
            }
        Long end = lastArrive.get(r.biggest);
        r.biggestDoneMs = end == null ? 0 : (end - t0) / 1000000;
        return r;
    }

    // ---------- רענון: תאריכי כל התיקיות ----------

    public static final class Check {
        public int checked, changed;
        public long ms;
    }

    public static Check refresh(final Result r, int readers) {
        final Stat st = r.fastUsed ? FAST : THREE;
        final Check c = new Check();
        final AtomicInteger changed = new AtomicInteger();
        long t0 = System.nanoTime();
        ExecutorService pool = Executors.newFixedThreadPool(Math.max(1, readers));
        final int n = r.dirs.size(), step = Math.max(1, n / (Math.max(1, readers) * 8));
        for (int s = 0; s < n; s += step) {
            final int a = s, b = Math.min(n, s + step);
            pool.execute(new Runnable() {
                @Override
                public void run() {
                    for (int i = a; i < b; i++) {
                        File f = new File(r.dirs.get(i));
                        long[] x = st.read(f.getParentFile(), f.getName());
                        if (x == null || x[2] != r.mtimes.get(i)) changed.incrementAndGet();
                    }
                }
            });
        }
        pool.shutdown();
        try {
            pool.awaitTermination(10, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        c.checked = n;
        c.changed = changed.get();
        c.ms = (System.nanoTime() - t0) / 1000000;
        return c;
    }

    public static String secs(long ms) {
        return String.format(Locale.US, "%.2f", ms / 1000.0);
    }
}
