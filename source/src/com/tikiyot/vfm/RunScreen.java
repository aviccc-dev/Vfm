package com.tikiyot.vfm;

// 1.26.0: מסך ההרצה האחד (claude/hartzat-pkudot.md). המשתמש: "לא הבנתי — הוא הריץ, ואז מה, חזר לעצמו? ואני צריך
// להיות נביא מה הוא עשה, ומה נשאר, ומה מחק, ומה הדוח … הוא מתנהל באופן כאוטי." — אחרי סקיצה: "בסדר".
// ארבעה שלבים זה מתחת לזה, על מסך אחד: מילוי ← מה יקרה ← מריץ ← סיכום. שום חלון לא קופץ ונעלם; הסיכום נשאר.
// ההכנה וההרצה — ברקע, עם התקדמות ועצירה. אחראי: ההצגה בלבד; התכנון וההרצה — Batch, המילוי — HoleForm.

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RunScreen extends Activity {

    static final String EXTRA_FILE = "file";

    private File file;
    private String text;
    private Map<String, String> vars;
    private Library lib;
    private List<String> lines;
    private HoleForm.Fields fields;
    /** 1.27.0: מה שמולא — לראש הדוח והיומן. */
    private Map<String, String> filled;
    private Batch.Plan plan;
    private List<String> report = new ArrayList<String>();
    private final Handler h = new Handler();
    private volatile boolean stop, running;
    private long started, took;

    private TextView stageLabel;
    private LinearLayout[] cards = new LinearLayout[4];
    private LinearLayout[] bodies = new LinearLayout[4];
    private TextView[] nums = new TextView[4];
    private LinearLayout bottom;

    static void start(Activity a, File f) {
        a.startActivity(new Intent(a, RunScreen.class).putExtra(EXTRA_FILE, f.getAbsolutePath()));
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        String p = getIntent() != null ? getIntent().getStringExtra(EXTRA_FILE) : null;
        if (p == null) {
            finish();
            return;
        }
        file = new File(p);
        try {
            text = Files.readUtf8(file);
        } catch (Throwable t) {
            Ui.toast(this, Words.CANT_READ_FILE);
            finish();
            return;
        }
        vars = BatchUi.vars(this, file.getParentFile());
        lib = Library.get(this);
        lines = Arrays.asList(text.replace("\r", "").split("\n"));
        setTitle(Words.Run.TITLE + Text.stem(file.getName()));

        LinearLayout root = Ui.screen(this);
        LinearLayout head = new LinearLayout(this);
        head.setPadding(Ui.dp(this, 14), Ui.dp(this, 10), Ui.dp(this, 14), Ui.dp(this, 4));
        TextView name = new TextView(this);
        name.setText(Words.Run.TITLE + Text.stem(file.getName()));
        name.setTextSize(16);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        name.setTextColor(Ui.TEXT);
        head.addView(name, new LinearLayout.LayoutParams(0, -2, 1f));
        stageLabel = Ui.info(this);
        head.addView(stageLabel);
        root.addView(head);

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(Ui.dp(this, 10), Ui.dp(this, 4), Ui.dp(this, 10), Ui.dp(this, 10));
        String[] titles = {Words.Run.S_FILL, Words.Run.S_WHAT, Words.Run.S_RUN, Words.Run.S_SUM};
        for (int i = 0; i < 4; i++) list.addView(card(i, titles[i]));
        ScrollView sv = new ScrollView(this);
        sv.addView(list);
        root.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1f));

        bottom = new LinearLayout(this);
        bottom.setPadding(Ui.dp(this, 10), Ui.dp(this, 4), Ui.dp(this, 10), Ui.dp(this, 8));
        root.addView(bottom);
        setContentView(TopRow.wrap(this, root));

        if (CmdTemplates.holes(lines).isEmpty()) {
            bodies[0].addView(info(Words.Run.NOTHING_TO_FILL));
            prepare(text);
        } else {
            stage(0);
            fields = HoleForm.fields(this, lines, vars);
            bodies[0].addView(fields.view);
            buttons(btn(Words.Run.NEXT, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Map<String, String> val = fields.values(RunScreen.this);
                    if (val == null) return;
                    filled = val;
                    StringBuilder sb = new StringBuilder();
                    for (String l : CmdTemplates.fill(lines, val)) sb.append(l).append('\n');
                    fields.view.setEnabled(false);
                    bodies[0].removeAllViews();
                    for (String k : val.keySet()) bodies[0].addView(info(k + ": " + val.get(k).replace("\n", " · ")));
                    prepare(sb.toString());
                }
            }), btn(Words.CANCEL, close()));
        }
    }

    // ---------- שלב 2: מה יקרה ----------

    private void prepare(final String body) {
        stage(1);
        final TextView pr = info(Words.Run.CHECKING);
        bodies[1].addView(pr);
        buttons(btn(Words.STOP, new View.OnClickListener() {
            @Override public void onClick(View v) { stop = true; }
        }));
        new Thread(new Runnable() {
            @Override
            public void run() {
                Batch.Plan p = null;
                Throwable err = null;
                try {
                    p = Batch.plan(body, file.getParentFile(), vars, lib, progress(pr, true));
                } catch (Throwable t) {
                    err = t;
                    Log.e("הכנת קובץ פקודות: " + file, t);
                }
                final Batch.Plan done = p;
                final Throwable e = err;
                h.post(new Runnable() {
                    @Override
                    public void run() {
                        if (isFinishing()) return;
                        bodies[1].removeAllViews();
                        if (e != null) {
                            bodies[1].addView(info(Words.Cmd.PLAN_FAILED + ": " + e));
                            buttons(btn(Words.CLOSE, close()));
                        } else if (done.stopped) {
                            bodies[1].addView(info(Words.Cmd.STOPPED_PREPARING));
                            buttons(btn(Words.CLOSE, close()));
                        } else {
                            plan = done;
                            preview();
                        }
                    }
                });
            }
        }).start();
    }

    private void preview() {
        List<Batch.Step> ok = new ArrayList<Batch.Step>();
        for (Batch.Step s : plan.steps) if (s.error == null) ok.add(s);
        for (String e : plan.echoes) bodies[1].addView(info("💬 " + e));
        for (Batch.Step s : ok)                                                // 1.27.0: היעד "החדש" כבר קיים — אזהרה
            if (s.noop && s.listPath != null && s.cmd.equals("MD")) bodies[1].addView(warn(Words.Run.exists(s.target)));
        row(bodies[1], Words.Run.WILL_CREATE, names(ok, Batch.K_CREATE, false));
        row(bodies[1], Words.Run.WILL_CHANGE, names(ok, Batch.K_CHANGE, false));
        row(bodies[1], Words.Run.WILL_DELETE, names(ok, Batch.K_DELETE, false));
        row(bodies[1], Words.Run.DISK, diskText(ok));
        if (plan.errors > 0) row(bodies[1], Words.Run.ERRORS, String.valueOf(plan.errors));
        for (String n : plan.notes) bodies[1].addView(info(n));
        bodies[1].addView(details(Words.Run.DETAILS + plan.steps.size() + Words.Run.LINES, planLines()));
        if (plan.asIf) {
            bodies[1].addView(info(Words.Run.AS_IF));
            buttons(btn(Words.Run.SHOW_AS_IF, new View.OnClickListener() {
                @Override public void onClick(View v) { AsIfScreen.show(RunScreen.this, file, Batch.asIfRows(plan)); }
            }), btn(Words.CLOSE, close()));
            return;
        }
        if (plan.changes() == 0 && plan.start == null && plan.open == null) {
            bodies[1].addView(info(Words.Cmd.NO_CHANGE));
            buttons(btn(Words.CLOSE, close()));
            return;
        }
        buttons(btn(Words.Run.RUN, new View.OnClickListener() {
            @Override public void onClick(View v) { run(); }
        }), btn(Words.CANCEL, close()));
    }

    // ---------- שלב 3: מריץ ----------

    private void run() {
        stage(2);
        plan.inputs.clear();                                                   // 1.27.0: הקלט — בראש הדוח והיומן
        plan.inputs.add(Words.Run.FILE + file.getName());
        if (filled != null)
            for (Map.Entry<String, String> e : filled.entrySet())
                plan.inputs.add(Words.Run.INPUT + " · " + e.getKey() + ": " + e.getValue().replace("\n", ", "));
        final TextView where = info(""), last = info(""), clock = info("");
        final ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(Math.max(1, plan.steps.size()));
        bodies[2].addView(where);
        bodies[2].addView(bar, new LinearLayout.LayoutParams(-1, -2));
        bodies[2].addView(clock);
        bodies[2].addView(last);
        buttons(btn(Words.STOP, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stop = true;
                Ui.toast(RunScreen.this, Words.Disk.STOPPING);
            }
        }));
        stop = false;
        running = true;
        started = System.currentTimeMillis();
        h.post(new Runnable() {
            @Override
            public void run() {
                if (!running) return;
                clock.setText(Words.Loop.time(System.currentTimeMillis() - started));
                h.postDelayed(this, 500);
            }
        });
        final Batch.Listener listen = new Batch.Listener() {
            @Override
            public void before(final int index, final int total, final Batch.Step s) {
                h.post(new Runnable() {
                    @Override
                    public void run() {
                        where.setText(Words.Run.at(index + 1, total, s.rounds == null ? 0 : s.rounds[s.rounds.length - 1]) + "\n" + s.text);
                        bar.setProgress(index + 1);
                    }
                });
            }

            @Override
            public void after(Batch.Step s, final String line) {
                if (line == null) return;
                h.post(new Runnable() {
                    @Override public void run() { last.setText(line); }
                });
            }
        };
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<String> rep;
                try {
                    rep = Batch.run(plan, lib, new Physical.DialogAsker(RunScreen.this), progress(null, false), Acts.deletedStaysGrey(), listen);
                } catch (Throwable t) {
                    Log.e("הרצת קובץ פקודות: " + file, t);
                    rep = new ArrayList<String>();
                    rep.add(Words.Cmd.RUN_FAILED + t);
                    plan.trouble = true;
                }
                report = rep;
                took = System.currentTimeMillis() - started;
                running = false;
                Log.i("קובץ פקודות: " + file + " · " + (report.isEmpty() ? Words.Cmd.NO_CHANGE : report.toString()));
                Ui.afterIndex(RunScreen.this, new Runnable() {
                    @Override public void run() { summary(); }
                });
            }
        }).start();
    }

    // ---------- שלב 4: סיכום ----------

    private void summary() {
        bodies[2].removeAllViews();
        bodies[2].addView(info(Words.Run.ranIn(Words.Loop.time(took)) + (stop ? Words.Run.STOPPED : "")));
        stage(3);
        List<Batch.Step> ran = new ArrayList<Batch.Step>();
        int skipped = 0;
        for (Batch.Step s : plan.steps) {
            if (s.ran && !s.noop) ran.add(s);
            else if (!s.ran) skipped++;
        }
        for (String e : plan.echoes) bodies[3].addView(info("💬 " + e));
        for (String in : plan.inputs) bodies[3].addView(info(in));
        row(bodies[3], Words.Run.CREATED, names(ran, Batch.K_CREATE, true));
        row(bodies[3], Words.Run.CHANGED, names(ran, Batch.K_CHANGE, true));
        row(bodies[3], Words.Run.DELETED, names(ran, Batch.K_DELETE, true));
        row(bodies[3], Words.Run.DISK, diskText(ran));
        row(bodies[3], Words.Run.SKIPPED, skipped == 0 ? Words.Ed.NONE : String.valueOf(skipped));
        row(bodies[3], Words.Run.TIME, Words.Loop.time(took));
        bodies[3].addView(details(Words.Run.FULL_REPORT, report));

        List<Button> bs = new ArrayList<Button>();
        final List<String> open = firstList();
        if (open != null)
            bs.add(btn(Words.Run.open(ListCmd.pathText(open)), new View.OnClickListener() {
                @Override public void onClick(View v) { Verbs.run(RunScreen.this, Verbs.openList(open), file.getParentFile()); }
            }));
        if (plan.open != null && !plan.trouble)
            bs.add(btn("▶ " + Verbs.label(Verbs.parse(plan.open)), new View.OnClickListener() {
                @Override public void onClick(View v) { Verbs.run(RunScreen.this, plan.open, file.getParentFile()); }
            }));
        if (plan.start != null)
            bs.add(btn("▶ " + (Batch.isHome(plan.start) ? Words.Cmd.HOME_AT_END : plan.start.getName()), new View.OnClickListener() {
                @Override public void onClick(View v) { BatchUi.openStart(RunScreen.this, plan.start); }
            }));
        if (plan.measureJournal != null && new File(plan.measureJournal).isFile())
            bs.add(btn(Words.Run.JOURNAL, new View.OnClickListener() {
                @Override public void onClick(View v) { Ui.open(RunScreen.this, new File(plan.measureJournal)); }
            }));
        final List<List<String>> made = created();
        if (!made.isEmpty()) {                                                 // 1.27.0: הניקוי — אחרי שבדקת, בשמות
            final Button[] clean = new Button[1];
            clean[0] = btn(Words.Run.CLEAN, new View.OnClickListener() {
                @Override public void onClick(View v) { clean(made, clean[0]); }
            });
            bs.add(clean[0]);
        }
        bs.add(btn(Words.CLOSE, close()));
        buttons(bs.toArray(new Button[0]));
        if (!plan.shared.isEmpty()) {                                          // 203: כל מה שלשיתוף — שליחה אחת
            Ui.toast(this, Words.Cmd.sending(plan.shared.size()));
            Acts.sendReady(this, plan.shared);
        }
    }

    /** 1.27.0: הרשימות שההרצה יצרה (MD שלא היה קיים) — רק העליונות (מה שבתוכן נמחק איתן). */
    private List<List<String>> created() {
        List<List<String>> all = new ArrayList<List<String>>();
        for (Batch.Step s : plan.steps)
            if (s.ran && !s.noop && s.listPath != null && s.cmd.equals("MD")) all.add(s.listPath);
        List<List<String>> top = new ArrayList<List<String>>();
        for (List<String> p : all) {
            boolean inner = false;
            for (List<String> q : all) if (q != p && q.size() < p.size() && Tree.inside(p, q)) inner = true;
            if (!inner && !top.contains(p)) top.add(p);
        }
        return top;
    }

    private void clean(final List<List<String>> made, final Button b) {
        StringBuilder names = new StringBuilder();
        for (List<String> p : made) names.append("• ").append(ListCmd.pathText(p)).append('\n');
        Ui.ask(this, Words.Run.CLEAN_TITLE, names.toString().trim(), Words.Run.CLEAN_WHAT, Words.Box.IN_BIN, Words.Run.CLEAN, new Runnable() {
            @Override
            public void run() {
                List<String> gone = new ArrayList<String>();
                for (List<String> p : made) {
                    if (Tree.load().find(p) == null) continue;
                    List<String> parent = new ArrayList<String>(p.subList(0, p.size() - 1));
                    String leaf = p.get(p.size() - 1);
                    VClip.remove(parent, java.util.Collections.singletonList(new VClip.Item(VClip.LIST, leaf, leaf)));
                    gone.add(ListCmd.pathText(p));
                }
                Log.i(Words.Run.CLEANED + ": " + gone);
                bodies[3].addView(warn(Words.Run.CLEANED + ": " + join(gone)));
                b.setEnabled(false);
            }
        });
    }

    private static String join(List<String> xs) {
        StringBuilder sb = new StringBuilder();
        for (String x : xs) sb.append(sb.length() > 0 ? ", " : "").append('"').append(x).append('"');
        return sb.toString();
    }

    private TextView warn(String s) {
        TextView t = info(s);
        t.setTextColor(Ui.TEXT);
        t.setBackgroundColor(Ui.DOUBT);
        t.setPadding(Ui.dp(this, 8), Ui.dp(this, 4), Ui.dp(this, 8), Ui.dp(this, 4));
        return t;
    }

    /** הרשימה הראשונה שנוצרה או שהשתנתה (לכפתור "פתח"). */
    private List<String> firstList() {
        for (Batch.Step s : plan.steps)
            if (s.ran && s.op != null && s.target != null && Batch.kind(s) != Batch.K_DELETE) {
                String t = s.target.replace("\"", "").trim();
                if (t.equals(Words.ListCmdW.HOME)) continue;
                List<String> p = new ArrayList<String>();
                for (String x : t.split("\\\\")) if (!x.trim().isEmpty()) p.add(x.trim());
                if (Tree.load().find(p) != null) return p;
            }
        return null;
    }

    // ---------- עזר ----------

    private String names(List<Batch.Step> steps, int kind, boolean after) {
        Set<String> out = new LinkedHashSet<String>();
        int n = 0;
        for (Batch.Step s : steps) {
            if (Batch.kind(s) != kind || s.noop) continue;
            n++;
            out.add(s.target != null ? s.target : s.dest != null && s.op == null && kind == Batch.K_CREATE ? s.dest.getName() : s.describe);
        }
        if (n == 0) return Words.Ed.NONE;
        StringBuilder sb = new StringBuilder();
        int k = 0;
        for (String x : out) {
            if (k++ == 3) {
                sb.append(" · ").append(Words.andMore(out.size() - 3));
                break;
            }
            sb.append(sb.length() > 0 ? " · " : "").append(x);
        }
        return (n > 1 ? n + Words.Run.LINES_IN : "") + sb;
    }

    private String diskText(List<Batch.Step> steps) {
        int n = 0;
        for (Batch.Step s : steps) if (Batch.disk(s)) n++;
        return n == 0 ? Words.Run.NO_DISK : Words.Run.diskLines(n);
    }

    private List<String> planLines() {
        List<String> out = new ArrayList<String>();
        for (Batch.Step s : plan.steps) out.add(s.label() + ". " + (s.error != null ? "⚠ " + s.error + " — " + s.text : s.describe));
        return out;
    }

    private Physical.Progress progress(final TextView t, final boolean planning) {
        return new Physical.Progress() {
            long last;

            @Override
            public void step(final int files, final int total, long bytes, long totalBytes) {
                if (t == null) return;
                long now = System.currentTimeMillis();
                if (now - last < 200 && files < total) return;
                last = now;
                h.post(new Runnable() {
                    @Override public void run() { t.setText(Words.Run.checking(files, total)); }
                });
            }

            @Override
            public boolean stopped() {
                return stop;
            }
        };
    }

    private LinearLayout card(int i, String title) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(Ui.dp(this, 12), Ui.dp(this, 8), Ui.dp(this, 12), Ui.dp(this, 10));
        LinearLayout hd = new LinearLayout(this);
        hd.setGravity(android.view.Gravity.CENTER_VERTICAL);
        TextView n = new TextView(this);
        n.setText(String.valueOf(i + 1));
        n.setGravity(android.view.Gravity.CENTER);
        n.setTextSize(12);
        int d = Ui.dp(this, 22);
        hd.addView(n, new LinearLayout.LayoutParams(d, d));
        TextView t = new TextView(this);
        t.setText(title);
        t.setTextSize(15);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextColor(Ui.TEXT);
        t.setPadding(Ui.dp(this, 8), 0, 0, 0);
        hd.addView(t);
        c.addView(hd);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(0, Ui.dp(this, 6), 0, 0);
        c.addView(body);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, Ui.dp(this, 10));
        c.setLayoutParams(lp);
        cards[i] = c;
        bodies[i] = body;
        nums[i] = n;
        paint(i, 2);
        return c;
    }

    /** state: 0 — פעיל, 1 — נעשה, 2 — ממתין. */
    private void paint(int i, int state) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(Ui.BG);
        g.setCornerRadius(Ui.dp(this, 12));
        g.setStroke(Ui.dp(this, state == 0 ? 2 : 1), state == 0 ? Ui.ACCENT : Ui.RULE);
        cards[i].setBackground(g);
        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        dot.setColor(state == 0 ? Ui.SELECTED : state == 1 ? 0xFFCEEAD6 : Ui.BG_SOFT);
        nums[i].setBackground(dot);
        nums[i].setText(state == 1 ? "✓" : String.valueOf(i + 1));
        nums[i].setTextColor(state == 2 ? Ui.MUTED : Ui.TEXT);
    }

    private void stage(int i) {
        for (int k = 0; k < 4; k++) paint(k, k < i ? 1 : k == i ? 0 : 2);
        if (i == 3) paint(3, 1);
        stageLabel.setText(Words.Run.stage(i + 1));
    }

    private TextView info(String s) {
        TextView t = Ui.info(this);
        t.setTextSize(13);
        t.setText(s);
        t.setPadding(0, Ui.dp(this, 2), 0, Ui.dp(this, 2));
        return t;
    }

    private void row(LinearLayout into, String label, String value) {
        LinearLayout r = new LinearLayout(this);
        r.setPadding(0, Ui.dp(this, 3), 0, Ui.dp(this, 3));
        TextView a = new TextView(this);
        a.setText(label);
        a.setTextSize(13);
        a.setTextColor(Ui.SUB);
        r.addView(a, new LinearLayout.LayoutParams(Ui.dp(this, 110), -2));
        TextView b = new TextView(this);
        b.setText(value);
        b.setTextSize(13);
        b.setTextColor(Ui.TEXT);
        b.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        r.addView(b, new LinearLayout.LayoutParams(0, -2, 1f));
        into.addView(r);
    }

    /** כפתור שפותח ומקפל רשימת שורות (פרטים / הדוח המלא). */
    private View details(final String label, final List<String> ls) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        final TextView body = new TextView(this);
        StringBuilder sb = new StringBuilder();
        for (String l : ls) sb.append(l).append('\n');
        body.setText(sb.toString().trim());
        body.setTextSize(12);
        body.setTextColor(Ui.TEXT);
        body.setTextIsSelectable(true);
        body.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        body.setVisibility(View.GONE);
        final Button b = btn("▸ " + label, null);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean show = body.getVisibility() != View.VISIBLE;
                body.setVisibility(show ? View.VISIBLE : View.GONE);
                b.setText((show ? "▾ " : "▸ ") + label);
            }
        });
        box.addView(b);
        box.addView(body);
        return box;
    }

    private Button btn(String label, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        if (l != null) b.setOnClickListener(l);
        return b;
    }

    private void buttons(Button... bs) {
        bottom.removeAllViews();
        android.widget.HorizontalScrollView hs = new android.widget.HorizontalScrollView(this);
        LinearLayout row = new LinearLayout(this);
        for (Button b : bs) row.addView(b);
        hs.addView(row);
        bottom.addView(hs);
    }

    private View.OnClickListener close() {
        return new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        };
    }

    @Override
    public void onBackPressed() {
        if (running) {
            stop = true;
            Ui.toast(this, Words.Disk.STOPPING);
            return;
        }
        super.onBackPressed();
    }
}
