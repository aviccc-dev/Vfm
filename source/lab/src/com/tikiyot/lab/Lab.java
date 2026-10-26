package com.tikiyot.lab;

// מעבדת זירוזים 1.1 — מה מזרז את בניית האינדקס. הסדר: ריצת חימום (לא נספרת; ממנה גם "המפה הקודמת"),
// הבסיס (התוכנה היום), כל שיפור לבד על הבסיס, הכול יחד ב־4, 8 ו־16 קוראים, ורענון.
// כל ניסוי — שלוש ריצות; "הטוב" ו"האמצעי"; וההבדל מהבסיס באחוזים. לא נוגעת בקבצים.

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Lab extends Activity {

    private static final int RUNS = 3;

    private TextView log, summaryView;
    private final StringBuilder all = new StringBuilder(), summary = new StringBuilder();
    private final List<Button> buttons = new ArrayList<Button>();
    private final Handler ui = new Handler(Looper.getMainLooper());
    private volatile boolean busy;
    /** "המפה הקודמת" לכל כונן — מהריצה האחרונה (לשיפור 2). */
    private final Map<String, Scan.Result> prev = new HashMap<String, Scan.Result>();
    private List<Scan.Result> lastResults;
    private long baseMedian;

    /** ניסוי: שם ואפשרויות. */
    private static final class Exp {
        final String name;
        final Scan.Opts o;

        Exp(String name, int readers, boolean fast, boolean seed, boolean split, boolean push, boolean names) {
            this.name = name;
            o = new Scan.Opts();
            o.readers = readers;
            o.fast = fast;
            o.seed = seed;
            o.split = split;
            o.push = push;
            o.names = names;
        }
    }

    private static final Exp BASE = new Exp("בסיס — התוכנה היום (4 קוראים)", 4, false, false, false, false, false);
    /** 1.2: ההשוואה המכריעה — אותם 8 קוראים, בלי השיפורים ועם כולם. */
    private static final Exp BASE8 = new Exp("בסיס · 8 קוראים", 8, false, false, false, false, false);
    private static final Exp ALL8 = new Exp("הכול יחד · 8 קוראים", 8, true, true, true, true, true);
    /** 1.3: מי נושא את הרווח — הכול ב־8 קוראים: הבסיס, כל שיפור לבד, הפשוטים יחד (1+3+5), והכול. */
    private static final Exp[] WHO = {
            new Exp("בסיס", 8, false, false, false, false, false),
            new Exp("1. שאלה אחת", 8, true, false, false, false, false),
            new Exp("2. מפה קודמת", 8, false, true, false, false, false),
            new Exp("3. חלוקת תיקייה גדולה", 8, false, false, true, false, false),
            new Exp("4. קוראים מכניסים לתור", 8, false, false, false, true, false),
            new Exp("5. שמות אצל הקוראים", 8, false, false, false, false, true),
            new Exp("הפשוטים יחד (1+3+5)", 8, true, false, true, false, true),
            new Exp("הכול יחד", 8, true, true, true, true, true)};
    private static final Exp[] ALONE = {
            new Exp("1. שאלה אחת לכל פריט", 4, true, false, false, false, false),
            new Exp("2. מפה מהאינדקס הקודם", 4, false, true, false, false, false),
            new Exp("3. חלוקת תיקייה גדולה", 4, false, false, true, false, false),
            new Exp("4. הקוראים מכניסים לתור בעצמם", 4, false, false, false, true, false),
            new Exp("5. השמות אצל הקוראים", 4, false, false, false, false, true)};
    private static final Exp[] TOGETHER = {
            new Exp("הכול יחד · 4 קוראים", 4, true, true, true, true, true),
            new Exp("הכול יחד · 8 קוראים", 8, true, true, true, true, true),
            new Exp("הכול יחד · 16 קוראים", 16, true, true, true, true, true)};

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Scan.FAST = Fast.stat();
        ScrollView sv = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        int p = dp(12);
        root.setPadding(p, p, p, p);
        sv.addView(root);
        setContentView(sv);

        root.addView(text("מה מזרז את בניית האינדקס. כל ניסוי רץ שלוש פעמים; \"הטוב\" ו\"האמצעי\" בשניות, "
                + "וההבדל מהבסיס באחוזים (מינוס = מהיר יותר). לא נוגעת בקבצים. בזמן הריצה — לא להשתמש בטלפון.", 14, 0xFF555555));
        if (!allowed()) root.addView(button("🔓 לאשר גישה לקבצים", new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:" + getPackageName())));
            }
        }, false));

        section(root, "1.4 — המאגר של אנדרואיד");
        root.addView(text("לאנדרואיד יש מאגר משלו של הקבצים. האם הוא שלם, וכמה מהר — מול הסריקה שלנו?", 13, 0xFF777777));
        Button st = button("▶ המאגר של אנדרואיד מול הסריקה (כחצי דקה)", new Runnable() {
            @Override public void run() { compareStore(); }
        }, true);
        st.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(st);
        root.addView(text("\"מה השתנה מאז\": לסמן נקודה ← לצלם מסך, להוריד קובץ, לשנות שם לקובץ ← ללחוץ \"מה השתנה\".", 13, 0xFF777777));
        root.addView(button("📍 לסמן נקודה עכשיו", new Runnable() {
            @Override public void run() { mark(); }
        }, true));
        root.addView(button("🔎 מה השתנה מאז הנקודה?", new Runnable() {
            @Override public void run() { sinceMark(); }
        }, true));
        section(root, "1.3 — האלגוריתם");
        Button who = button("▶ מי נושא את הרווח? · 8 קוראים · 5 סבבים (כדקה וחצי)", new Runnable() {
            @Override public void run() { whoCarries(); }
        }, true);
        who.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(who);
        root.addView(button("ההשוואה של 1.2: בסיס מול הכול · 8 קוראים", new Runnable() {
            @Override public void run() { compare(); }
        }, true));
        root.addView(button("להריץ את כל הניסויים (1.1 · כמה דקות)", new Runnable() {
            @Override public void run() { everything(); }
        }, true));
        root.addView(button("📋 להעתיק את התוצאות", new Runnable() {
            @Override public void run() { copy(); }
        }, false));

        section(root, "או ניסוי אחד");
        root.addView(expButton(BASE));
        for (Exp e : ALONE) root.addView(expButton(e));
        for (Exp e : TOGETHER) root.addView(expButton(e));
        root.addView(button("רענון: תאריכי כל התיקיות", new Runnable() {
            @Override public void run() { refresh(); }
        }, true));

        section(root, "סיכום");
        summaryView = text("", 15, Color.BLACK);
        summaryView.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(summaryView);
        section(root, "פירוט");
        log = text("", 13, 0xFF333333);
        root.addView(log);
    }

    // ---------- הריצות ----------

    private void everything() {
        line("━━ חימום (לא נספר; ממנו גם המפה הקודמת)");
        once(BASE.o, null);
        run(BASE);
        for (Exp e : ALONE) run(e);
        for (Exp e : TOGETHER) run(e);
        refresh();
        line("✔ סיום");
    }

    // ---------- 1.4: המאגר של אנדרואיד ----------

    private static final Scan.Opts BEST = ALL_OPTS();

    private static Scan.Opts ALL_OPTS() {
        Scan.Opts o = new Scan.Opts();
        o.readers = 8;
        o.fast = o.seed = o.split = o.push = o.names = true;
        o.collect = true;
        return o;
    }

    private void compareStore() {
        line("━━ המאגר של אנדרואיד מול הסריקה");
        Map<String, String> vols = Store.volumes(this);
        for (Map.Entry<String, String> v : vols.entrySet()) {
            String root = v.getValue();
            if (!new File(root).isDirectory()) {
                line("   " + v.getKey() + ": לא נמצא בכונן (" + root + ")");
                continue;
            }
            Store.Rows m = Store.all(this, v.getKey(), root);
            Scan.Result r = Scan.scan(root, BEST, prev.get(root));
            prev.put(root, r);
            java.util.Set<String> inStore = new java.util.HashSet<String>(m.paths), inScan = new java.util.HashSet<String>(r.paths);
            List<String> onlyScan = new ArrayList<String>(), onlyStore = new ArrayList<String>();
            for (String p : r.paths) if (!inStore.contains(p)) onlyScan.add(p);
            for (String p : m.paths) if (!inScan.contains(p) && p.startsWith(root + "/")) onlyStore.add(p);
            int hidden = 0;
            for (String p : onlyScan) if (p.substring(root.length()).contains("/.")) hidden++;
            StringBuilder sb = new StringBuilder();
            sb.append("   ").append(label(root)).append(" (").append(v.getKey()).append(")\n")
                    .append("   המאגר: ").append(fmt(m.paths.size())).append(" קבצים · ").append(Scan.secs(m.ms)).append(" שניות")
                    .append(m.error != null ? " · ❗ " + m.error : "").append("\n")
                    .append("   הסריקה: ").append(fmt(r.files)).append(" קבצים · ").append(Scan.secs(r.ms)).append(" שניות\n")
                    .append("   חסרים במאגר: ").append(fmt(onlyScan.size())).append(" (מהם מוסתרים: ").append(fmt(hidden)).append(")\n")
                    .append(top(onlyScan, root, 8))
                    .append("   במאגר ולא בכונן: ").append(fmt(onlyStore.size())).append("\n")
                    .append(top(onlyStore, root, 4));
            line(sb.toString());
            summary.append(label(root)).append(": המאגר ").append(Scan.secs(m.ms)).append(" · הסריקה ").append(Scan.secs(r.ms))
                    .append(" · חסרים במאגר ").append(fmt(onlyScan.size())).append(" מתוך ").append(fmt(r.files))
                    .append(" (מוסתרים ").append(fmt(hidden)).append(")\n");
        }
        showSummary();
        line("✔ סיום");
    }

    /** התיקיות שבהן הכי הרבה מהרשימה (עד שלוש רמות מתחת לשורש). */
    private static String top(List<String> paths, String root, int k) {
        final Map<String, Integer> c = new HashMap<String, Integer>();
        for (String p : paths) {
            String rel = p.length() > root.length() ? p.substring(root.length() + 1) : p;
            String[] parts = rel.split("/");
            StringBuilder d = new StringBuilder();
            for (int i = 0; i < Math.min(3, parts.length - 1); i++) d.append(i > 0 ? "/" : "").append(parts[i]);
            String key = d.length() == 0 ? "(שורש)" : d.toString();
            Integer n = c.get(key);
            c.put(key, n == null ? 1 : n + 1);
        }
        List<String> keys = new ArrayList<String>(c.keySet());
        java.util.Collections.sort(keys, new java.util.Comparator<String>() {
            @Override public int compare(String a, String b) { return c.get(b) - c.get(a); }
        });
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(k, keys.size()); i++)
            sb.append("      ").append(fmt(c.get(keys.get(i)))).append(" · ").append(keys.get(i)).append("\n");
        return sb.toString();
    }

    private static String fmt(int n) {
        return String.format(Locale.US, "%,d", n);
    }

    private final Map<String, Long> marks = new HashMap<String, Long>();

    private void mark() {
        marks.clear();
        StringBuilder sb = new StringBuilder("📍 נקודה סומנה:");
        for (Map.Entry<String, String> v : Store.volumes(this).entrySet()) {
            long t0 = System.nanoTime();
            long g = Store.generation(this, v.getKey());
            marks.put(v.getKey(), g);
            sb.append("\n   ").append(label(v.getValue())).append(": מונה ").append(g < 0 ? "לא נתמך (צריך אנדרואיד 11+)" : g)
                    .append(" · ").append(Scan.secs((System.nanoTime() - t0) / 1000000)).append(" שניות");
        }
        line(sb.toString());
    }

    private void sinceMark() {
        if (marks.isEmpty()) {
            line("קודם לסמן נקודה");
            return;
        }
        StringBuilder sb = new StringBuilder("🔎 מה השתנה מאז הנקודה:");
        for (Map.Entry<String, String> v : Store.volumes(this).entrySet()) {
            Long g = marks.get(v.getKey());
            if (g == null || g < 0) continue;
            Store.Rows r = Store.changedSince(this, v.getKey(), g);
            sb.append("\n   ").append(label(v.getValue())).append(": ").append(r.paths.size()).append(" קבצים · ")
                    .append(Scan.secs(r.ms)).append(" שניות").append(r.error != null ? " · ❗ " + r.error : "");
            for (int i = 0; i < Math.min(10, r.paths.size()); i++) sb.append("\n      ").append(r.paths.get(i));
        }
        sb.append("\n   (קובץ שנמחק לא מופיע כאן — המאגר מוחק אותו)");
        line(sb.toString());
        summary.append("מה השתנה מאז הנקודה — ר' בפירוט\n");
        showSummary();
    }

    /**
     * 1.3: כל הצירופים של WHO, בחמישה סבבים. בכל סבב הסדר מסתובב (מי שהיה ראשון — עובר לסוף), כדי שמצב הטלפון
     * (התחממות, אנדרואיד שזוכר תיקיות) ישפיע על כולם שווה. בסיכום: האמצעי, וההבדל מהבסיס — מהמהיר לאיטי.
     */
    private void whoCarries() {
        final int rounds = 5, n = WHO.length;
        line("━━ חימום (לא נספר; ממנו גם המפה הקודמת)");
        once(WHO[0].o, null);
        long[][] t = new long[n][rounds];
        for (int r = 0; r < rounds; r++) {
            StringBuilder sb = new StringBuilder("   סבב " + (r + 1) + ":");
            for (int k = 0; k < n; k++) {
                int i = (k + r) % n;
                t[i][r] = once(WHO[i].o, null);
            }
            for (int i = 0; i < n; i++) sb.append(i == 0 ? " " : " · ").append(Scan.secs(t[i][r]));
            line(sb.toString());
        }
        final long[] med = new long[n];
        for (int i = 0; i < n; i++) med[i] = median(t[i]);
        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) order[i] = i;
        Arrays.sort(order, new java.util.Comparator<Integer>() {
            @Override public int compare(Integer a, Integer b) { return Long.compare(med[a], med[b]); }
        });
        summary.append("מי נושא את הרווח · 8 קוראים · 5 סבבים (האמצעי, מהמהיר לאיטי)\n");
        for (int i : order)
            summary.append(WHO[i].name).append(": ").append(Scan.secs(med[i]))
                    .append(i == 0 ? "" : String.format(Locale.US, "  (%+d%%)", Math.round(100.0 * (med[i] - med[0]) / med[0])))
                    .append("\n");
        showSummary();
        line("✔ סיום (סדר העמודות בכל סבב: " + names(WHO) + ")");
    }

    private static String names(Exp[] e) {
        StringBuilder sb = new StringBuilder();
        for (Exp x : e) sb.append(sb.length() > 0 ? " · " : "").append(x.name);
        return sb.toString();
    }

    /** 1.2: בסיס 8 מול הכול 8 — לסירוגין (פעם זה ראשון, פעם זה), כדי שמצב הטלפון ישפיע על שניהם שווה. */
    private void compare() {
        final int n = 5;
        line("━━ חימום (לא נספר; ממנו גם המפה הקודמת)");
        once(BASE8.o, null);
        long[] a = new long[n], b = new long[n];
        for (int i = 0; i < n; i++) {
            if (i % 2 == 0) {
                a[i] = once(BASE8.o, null);
                b[i] = once(ALL8.o, null);
            } else {
                b[i] = once(ALL8.o, null);
                a[i] = once(BASE8.o, null);
            }
            line("   ריצה " + (i + 1) + ": בסיס " + Scan.secs(a[i]) + " · הכול " + Scan.secs(b[i]));
        }
        long ma = median(a), mb = median(b);
        String verdict = String.format(Locale.US, "ההבדל באמצעי: %+d%%", Math.round(100.0 * (mb - ma) / ma));
        summary.append("השוואה · 8 קוראים (5 ריצות)\n")
                .append(BASE8.name).append(": הטוב ").append(Scan.secs(min(a))).append(" · האמצעי ").append(Scan.secs(ma)).append("\n")
                .append(ALL8.name).append(": הטוב ").append(Scan.secs(min(b))).append(" · האמצעי ").append(Scan.secs(mb)).append("\n")
                .append(verdict).append(" (מינוס = השיפורים מזרזים)\n");
        showSummary();
        line("✔ סיום · " + verdict);
    }

    private static long median(long[] t) {
        long[] s = t.clone();
        Arrays.sort(s);
        return s[s.length / 2];
    }

    private static long min(long[] t) {
        long m = Long.MAX_VALUE;
        for (long x : t) m = Math.min(m, x);
        return m;
    }

    /** כל הכוננים יחד; מחזיר את הזמן הכולל, ושומר את התוצאות (גם כמפה הבאה). */
    private long once(final Scan.Opts o, StringBuilder per) {
        final List<String> roots = roots();
        final Scan.Result[] res = new Scan.Result[roots.size()];
        long t0 = System.nanoTime();
        List<Thread> ts = new ArrayList<Thread>();
        for (int i = 0; i < roots.size(); i++) {
            final int k = i;
            final Scan.Result pv = prev.get(roots.get(k));
            Thread t = new Thread(new Runnable() {
                @Override public void run() { res[k] = Scan.scan(roots.get(k), o, pv); }
            });
            ts.add(t);
            t.start();
        }
        for (Thread t : ts) try {
            t.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        long ms = (System.nanoTime() - t0) / 1000000;
        lastResults = new ArrayList<Scan.Result>(Arrays.asList(res));
        for (Scan.Result r : res) {
            prev.put(r.root, r);
            if (per != null) per.append("   ").append(label(r.root)).append(": ").append(Scan.secs(r.ms)).append(" · ")
                    .append(String.format(Locale.US, "%,d", r.files)).append(" קבצים · הגדולה: ")
                    .append(new File(r.biggest).getName()).append(String.format(Locale.US, " (%,d)", r.biggestFiles))
                    .append(" הסתיימה אחרי ").append(Scan.secs(r.biggestDoneMs)).append("\n");
        }
        return ms;
    }

    private void run(Exp e) {
        if (e.o.seed && prev.isEmpty()) once(BASE.o, null);                    // צריך מפה
        long[] t = new long[RUNS];
        StringBuilder best = null;
        long bestMs = Long.MAX_VALUE;
        for (int i = 0; i < RUNS; i++) {
            StringBuilder per = new StringBuilder();
            t[i] = once(e.o, per);
            if (t[i] < bestMs) {
                bestMs = t[i];
                best = per;
            }
        }
        long[] s = t.clone();
        Arrays.sort(s);
        long median = s[RUNS / 2];
        if (e == BASE) baseMedian = median;
        String diff = e == BASE || baseMedian == 0 ? "" : String.format(Locale.US, "  (%+d%%)", Math.round(100.0 * (median - baseMedian) / baseMedian));
        line("━━ " + e.name + "\n   ריצות: " + Scan.secs(t[0]) + " · " + Scan.secs(t[1]) + " · " + Scan.secs(t[2])
                + " שניות\n   הטובה:\n" + best);
        summary.append(e.name).append(": הטוב ").append(Scan.secs(s[0])).append(" · האמצעי ").append(Scan.secs(median)).append(diff).append("\n");
        showSummary();
    }

    private Button expButton(final Exp e) {
        return button(e.name, new Runnable() {
            @Override public void run() { Lab.this.run(e); }
        }, true);
    }

    private void refresh() {
        if (lastResults == null) once(BASE.o, null);
        long[] t = new long[RUNS];
        StringBuilder per = new StringBuilder();
        for (int i = 0; i < RUNS; i++) {
            long t0 = System.nanoTime();
            for (Scan.Result r : lastResults) {
                Scan.Check c = Scan.refresh(r, 4);
                if (i == RUNS - 1) per.append("   ").append(label(r.root)).append(": ")
                        .append(String.format(Locale.US, "%,d", c.checked)).append(" תיקיות · ").append(c.changed).append(" השתנו\n");
            }
            t[i] = (System.nanoTime() - t0) / 1000000;
        }
        long[] s = t.clone();
        Arrays.sort(s);
        line("━━ רענון: תאריכי כל התיקיות (4 קוראים)\n   ריצות: " + Scan.secs(t[0]) + " · " + Scan.secs(t[1]) + " · "
                + Scan.secs(t[2]) + " שניות\n" + per);
        summary.append("רענון: הטוב ").append(Scan.secs(s[0])).append(" · האמצעי ").append(Scan.secs(s[RUNS / 2])).append("\n");
        showSummary();
    }

    // ---------- עזרים ----------

    private List<String> roots() {
        List<String> out = new ArrayList<String>();
        out.add(Environment.getExternalStorageDirectory().getAbsolutePath());
        File[] dirs = getExternalFilesDirs(null);
        if (dirs != null) for (File f : dirs) {
            if (f == null) continue;
            String p = f.getAbsolutePath();
            int k = p.indexOf("/Android/");
            if (k > 0 && !out.contains(p.substring(0, k))) out.add(p.substring(0, k));
        }
        return out;
    }

    private static String label(String path) {
        return path.startsWith("/storage/emulated/") ? "פנימי" : "כרטיס";
    }

    private boolean allowed() {
        return Build.VERSION.SDK_INT < 30 || Environment.isExternalStorageManager();
    }

    private void showSummary() {
        final String s = summary.toString();
        ui.post(new Runnable() {
            @Override public void run() { summaryView.setText(s); }
        });
    }

    private void line(String s) {
        all.append(s).append("\n");
        final String t = all.toString();
        ui.post(new Runnable() {
            @Override public void run() { log.setText(t); }
        });
    }

    private void copy() {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("מעבדת זירוזים", "מעבדת זירוזים 1.4 · אנדרואיד " + Build.VERSION.RELEASE
                + "\nסיכום (שניות):\n" + summary + "\n" + all));
        Toast.makeText(this, "הועתק", Toast.LENGTH_SHORT).show();
    }

    private Button button(String text, final Runnable r, final boolean background) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!background) {
                    r.run();
                    return;
                }
                if (busy) return;
                if (!allowed()) {
                    Toast.makeText(Lab.this, "קודם לאשר גישה לקבצים", Toast.LENGTH_SHORT).show();
                    return;
                }
                busy = true;
                enable(false);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            r.run();
                        } catch (Throwable t) {
                            line("❗ " + t);
                        } finally {
                            busy = false;
                            enable(true);
                        }
                    }
                }).start();
            }
        });
        if (background) buttons.add(b);
        return b;
    }

    private void enable(final boolean on) {
        ui.post(new Runnable() {
            @Override
            public void run() {
                for (Button b : buttons) b.setEnabled(on);
                setTitle(on ? "מעבדת זירוזים 1.4" : "מעבדת זירוזים — רץ…");
            }
        });
    }

    private void section(LinearLayout root, String s) {
        TextView t = text(s, 16, 0xFF1A4D8F);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(0, dp(16), 0, dp(4));
        root.addView(t);
    }

    private TextView text(String s, int size, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setTextIsSelectable(true);
        return t;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
