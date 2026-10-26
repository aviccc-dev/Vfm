package com.tikiyot.vfm;

// 1.21.0: "מצב בתאריך" (שלב 4 של שילוב הגיבוי). בגיבוי "מראה + היסטוריה":
//   <שורש>/נוכחי/<מסלול>                  — הגרסה של היום
//   <שורש>/היסטוריה/<זמן>/<מסלול>          — גרסה שיצאה מ"נוכחי" בגיבוי של <זמן>
//   <שורש>/יומן/<זמן>.txt                  — מה שהגיע ל"נוכחי" בגיבוי של <זמן> (שורה לכל קובץ: מסלול <טאב> גודל)
// גרסה הייתה במצב בזמן T אם הגיעה עד T ולא יצאה עד T. "הגיעה" — מהיומנים (מקור האמת, שמור בגיבוי עצמו);
// אם אין יומן שמזכיר אותה — "נוסף" מהאינדקס. "יצאה" — שם תיקיית ההיסטוריה. גרסה ב"נוכחי" — לא יצאה.
// T שהוא בין שני גיבויים — המצב שאחרי הגיבוי שלפניו (מתעגל מעצמו).
// אחראי: לקרוא יומנים ושמות תיקיות, ולענות לכל קובץ. אסור לו: לשנות דבר.

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class State {

    static final String CURRENT = "נוכחי", HISTORY = "היסטוריה", JOURNAL = "יומן";   // בכונן

    /** הזמן שנבחר. */
    final long when;
    /** שורש גיבוי ← ציר הזמן שלו (נקרא פעם אחת לכל חיפוש). */
    private final Map<String, Line> lines = new HashMap<String, Line>();

    State(long when) {
        this.when = when;
    }

    /** ציר הזמן של שורש אחד: מסלול ← זמני ההגעה (ממוינים). null — אינו שורש גיבוי. */
    static final class Line {
        final Map<String, long[]> arrivals = new HashMap<String, long[]>();
    }

    /** האם הקובץ (מסלול מלא) היה במצב ב־when. added — "נוסף" מהאינדקס, לגיבוי בלי יומן. */
    boolean in(String path, long added) {
        Where w = version(path, added);
        return w != null && w.came <= when && when < w.left;
    }

    /** הגרסה: איפה בגיבוי, מתי הגיעה ומתי יצאה. null — אינה בגיבוי. */
    Where version(String path, long added) {
        Where w = where(path);
        if (w == null) return null;
        Line line = line(w.root);
        long[] arr = line.arrivals.get(w.rel);
        long came = Long.MIN_VALUE;
        if (arr != null)
            for (long t : arr) if (t < w.left) came = Math.max(came, t);      // ההגעה האחרונה לפני היציאה
        if (came == Long.MIN_VALUE) came = added > 0 ? added : Long.MIN_VALUE + 1;
        w.came = came;
        return w;
    }

    /** איפה בגיבוי: השורש, המסלול שאחרי נוכחי/ או היסטוריה/<זמן>/, ומתי יצא. */
    static final class Where {
        String root, rel;
        /** מתי יצאה (Long.MAX_VALUE — עדיין בנוכחי), ומתי הגיעה. */
        long left = Long.MAX_VALUE, came;
    }

    Where where(String path) {
        String cur = "/" + CURRENT + "/", hist = "/" + HISTORY + "/";
        int from = 0;
        while (true) {
            int c = path.indexOf(cur, from), h = path.indexOf(hist, from);
            if (c < 0 && h < 0) return null;
            boolean isCur = c >= 0 && (h < 0 || c < h);
            int at = isCur ? c : h;
            String root = path.substring(0, at);
            if (isBackup(root)) {
                Where w = new Where();
                w.root = root;
                if (isCur) {
                    w.rel = path.substring(at + cur.length());
                    return w;
                }
                String rest = path.substring(at + hist.length());
                int slash = rest.indexOf('/');
                if (slash > 0) {
                    long t = stamp(rest.substring(0, slash));
                    if (t != Long.MIN_VALUE) {
                        w.left = t;
                        w.rel = rest.substring(slash + 1);
                        return w;
                    }
                }
            }
            from = at + 1;
        }
    }

    private final Map<String, Boolean> backups = new HashMap<String, Boolean>();

    /** שורש גיבוי: יש בו יומן או היסטוריה, וגם נוכחי. נבדק פעם אחת לכל שורש. */
    private synchronized boolean isBackup(String root) {
        Boolean b = backups.get(root);
        if (b == null) {
            File r = new File(root);
            b = isBackupRoot(r);
            backups.put(root, b);
        }
        return b;
    }

    static boolean isBackupRoot(File r) {
        return new File(r, CURRENT).isDirectory() && (new File(r, JOURNAL).isDirectory() || new File(r, HISTORY).isDirectory());
    }

    private synchronized Line line(String root) {
        if (lines.containsKey(root)) return lines.get(root);
        Line l = read(new File(root));
        lines.put(root, l);
        return l;
    }

    /** קורא את כל היומנים של שורש. */
    static Line read(File root) {
        Line l = new Line();
        File[] js = new File(root, JOURNAL).listFiles();
        if (js == null) return l;
        Map<String, List<Long>> m = new HashMap<String, List<Long>>();
        for (File j : js) {
            String n = j.getName();
            if (!n.endsWith(".txt")) continue;
            long t = stamp(n.substring(0, n.length() - 4));
            if (t == Long.MIN_VALUE) continue;
            for (String[] row : Physical.Journal.read(j)) {
                List<Long> x = m.get(row[0]);
                if (x == null) m.put(row[0], x = new ArrayList<Long>());
                x.add(t);
            }
        }
        for (Map.Entry<String, List<Long>> e : m.entrySet()) {
            long[] a = new long[e.getValue().size()];
            for (int i = 0; i < a.length; i++) a[i] = e.getValue().get(i);
            Arrays.sort(a);
            l.arrivals.put(e.getKey(), a);
        }
        return l;
    }

    /** שם של גיבוי: "dd.MM.yyyy HH-mm-ss", אפשר עם מספר לפניו ("0007 …"). Long.MIN_VALUE — לא הובן. */
    static long stamp(String name) {
        String n = name.trim();
        int sp = n.indexOf(' ');
        if (sp > 0 && n.substring(0, sp).matches("\\d+") && n.indexOf('.') > sp) n = n.substring(sp + 1).trim();
        try {
            java.text.SimpleDateFormat f = new java.text.SimpleDateFormat(Batch.NOW_FORMAT, Locale.ROOT);
            f.setLenient(false);
            java.text.ParsePosition pos = new java.text.ParsePosition(0);
            java.util.Date d = f.parse(n, pos);
            return d == null || pos.getIndex() != n.length() ? Long.MIN_VALUE : d.getTime();
        } catch (Throwable t) {
            return Long.MIN_VALUE;
        }
    }

    /**
     * הזמן שבתנאי: "26.10.2026" (סוף היום), "26.10.2026 14:30", "26.10.2026 14:30:05", או שם של גיבוי
     * ("26.10.2026 14-30-05", גם עם מספר לפניו). Long.MIN_VALUE — לא הובן.
     */
    static long when(String raw) {
        String v = raw.trim();
        long s = stamp(v);
        if (s != Long.MIN_VALUE) return s + 999;                        // השנייה כולה
        String[] fmts = {"dd.MM.yyyy HH:mm:ss", "dd.MM.yyyy HH:mm", "d.M.yyyy HH:mm:ss", "d.M.yyyy HH:mm"};   // בכונן
        for (String fm : fmts) {
            try {
                java.text.SimpleDateFormat f = new java.text.SimpleDateFormat(fm, Locale.ROOT);
                f.setLenient(false);
                java.text.ParsePosition pos = new java.text.ParsePosition(0);
                java.util.Date d = f.parse(v, pos);
                if (d != null && pos.getIndex() == v.length()) return d.getTime() + (fm.endsWith("ss") ? 999 : 59999);
            } catch (Throwable ignored) {
            }
        }
        long day = Cond.dateEdge(v);
        if (day == Long.MIN_VALUE || !v.matches("[\\d./-]+")) return Long.MIN_VALUE;
        return day + 24L * 3600 * 1000 - 1;                              // תאריך בלבד — סוף היום
    }
}
