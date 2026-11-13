package com.tikiyot.vfm;

// 181: שורת כפתורי התצוגה המהירה בראש מסך החיפוש, והעורך של כפתור — עם מה שיקרה לתוצאות לפני שמחליטים.
// אחראי: הממשק. הרשימה והמצב — Quick; החיפוש עצמו — המסך (Host).

import android.app.Activity;
import android.content.DialogInterface;
import android.view.View;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class QuickBar {

    /** המסך שהשורה עומדת בו. */
    interface Host {
        /** התנאי של המסך בלי הכפתורים (הכלי + שורת ההקלדה). */
        String base();

        /** כמה תוצאות לתנאי. */
        int count(String cond);

        /** תוצאות לתנאי — לספירה לפי סוג. */
        List<Library.Hit> hits(String cond);

        String loadState();

        void saveState(String state);

        /** הכפתורים השתנו — לחפש מחדש. */
        void changed();
    }

    private final Activity a;
    private final Host host;
    private final LinearLayout row;
    final HorizontalScrollView view;
    private List<Quick.Button> all;
    private Set<String> on;

    QuickBar(Activity a, Host host) {
        this.a = a;
        this.host = host;
        row = new LinearLayout(a);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutDirection(Ui.DIRECTION);
        row.setPadding(Ui.dp(a, 6), Ui.dp(a, 4), Ui.dp(a, 6), Ui.dp(a, 4));
        view = new HorizontalScrollView(a);
        view.setHorizontalScrollBarEnabled(false);
        view.addView(row);
        all = Quick.all();
        String st = host.loadState();
        on = st == null ? Quick.startOn() : Quick.parse(st);
        draw();
    }

    /** התנאי של הכפתורים הדולקים. */
    String cond() {
        return Quick.cond(all, on);
    }

    private void draw() {
        row.removeAllViews();
        for (final Quick.Button b : all) {
            if (Quick.isRun(b)) {                                             // 216: כפתור הרצה
                TextView t = chip("▶ " + b.name, false);
                t.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) { BatchUi.run(a, new File(b.cond.substring(Quick.RUN.length()))); }
                });
                t.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override public boolean onLongClick(View v) { menu(b); return true; }
                });
                row.addView(t);
                continue;
            }
            final TextView t = chip(b.name, on.contains(b.name));
            t.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!on.remove(b.name)) on.add(b.name);
                    host.saveState(Quick.state(on));
                    draw();
                    host.changed();
                }
            });
            t.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    menu(b);
                    return true;
                }
            });
            row.addView(t);
        }
        TextView add = chip(Words.Quick.ADD, false);
        add.setOnClickListener(new View.OnClickListener() {                  // 216: סינון, או הרצת קובץ פקודות
            @Override
            public void onClick(View v) {
                Ui.options(a, Words.Quick.TITLE_NEW, new String[]{Words.Quick.KIND_FILTER, Words.Quick.KIND_RUN},
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int w) {
                                if (w == 0) edit(null);
                                else pickRun();
                            }
                        });
            }
        });
        row.addView(add);
    }

    private TextView chip(String text, boolean lit) {
        TextView t = new TextView(a);
        t.setText(lit ? "✓ " + text : text);
        t.setTextSize(14);
        t.setTextColor(lit ? Ui.ON_COLOR : Ui.TEXT);
        t.setBackgroundColor(lit ? Ui.ICON : Ui.HEADER_SOFT);
        int p = Ui.dp(a, 10);
        t.setPadding(p, Ui.dp(a, 6), p, Ui.dp(a, 6));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.setMargins(Ui.dp(a, 3), 0, Ui.dp(a, 3), 0);
        t.setLayoutParams(lp);
        return t;
    }

    private void menu(final Quick.Button b) {
        Ui.options(a, b.name, new String[]{Words.Quick.EDIT, Words.Quick.RIGHT, Words.Quick.LEFT, Words.Quick.DELETE},
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        int i = all.indexOf(b);
                        if (w == 0) {
                            if (Quick.isRun(b)) BatchUi.edit(a, new File(b.cond.substring(Quick.RUN.length())));   // 216
                            else edit(b);
                            return;
                        }
                        if (w == 1 && i > 0) all.add(i - 1, all.remove(i));           // ימינה = קודם (מימין לשמאל)
                        else if (w == 2 && i < all.size() - 1) all.add(i + 1, all.remove(i));
                        else if (w == 3) {
                            all.remove(i);
                            if (on.remove(b.name)) host.saveState(Quick.state(on));
                        }
                        Quick.save(all);
                        draw();
                        if (w == 3) host.changed();
                    }
                });
    }

    /** 216: כפתור הרצה — בוחרים קובץ פקודות; השם — שם הקובץ. */
    private void pickRun() {
        final List<File> files = BatchUi.all(a);
        if (files.isEmpty()) {
            Ui.toast(a, Words.Quick.NO_BATCH);
            return;
        }
        List<String> labels = new ArrayList<String>();
        for (File f : files) labels.add("📜 " + Text.stem(f.getName()));
        Ui.list(a, Words.Quick.KIND_RUN, labels, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int w) {
                File f = files.get(w);
                String nm = Text.stem(f.getName()).replace("=", "").replace(Quick.SEP, "");
                for (Quick.Button x : all) if (x.name.equals(nm)) nm = nm + " (2)";
                all.add(new Quick.Button(nm, Quick.RUN + f.getAbsolutePath()));
                Quick.save(all);
                draw();
            }
        });
    }

    // ---------- העורך ----------

    /** התנאי שלפני הכפתור: המסך ושאר הכפתורים הדולקים (בלי זה שבעריכה); ריק — כל הכוננים. */
    private String before(Quick.Button editing) {
        StringBuilder sb = new StringBuilder();
        for (Quick.Button x : all)
            if (x != editing && on.contains(x.name) && !x.cond.isEmpty()) sb.append(sb.length() > 0 ? " " : "").append(x.cond);
        String base = SmartLists.join(host.base(), sb.toString());
        if (base.trim().isEmpty() || Cond.parse(base).words.isEmpty() && Cond.parse(base).tests.isEmpty())
            base = SmartLists.join(Sources.toCond(Arrays.asList(Sources.INTERNAL, Sources.CARD)), base);
        return base;
    }

    private String n(int v) {
        return Files.count(v);
    }

    private void edit(final Quick.Button editing) {
        final String base = before(editing);
        final List<String> parts = editing == null ? new ArrayList<String>() : Quick.parts(editing.cond);
        LinearLayout box = Ui.form(a);
        final EditText name = Ui.field(a, editing == null ? "" : editing.name, Words.Quick.NAME);
        box.addView(Ui.label(a, Words.Quick.NAME));
        box.addView(name);
        final LinearLayout rows = new LinearLayout(a);
        rows.setOrientation(LinearLayout.VERTICAL);
        box.addView(rows);
        final TextView preview = Ui.label(a, "");
        final Runnable[] redraw = new Runnable[1];
        redraw[0] = new Runnable() {
            @Override
            public void run() {
                rows.removeAllViews();
                if (parts.isEmpty()) rows.addView(Ui.label(a, Words.Quick.NO_CONDS));
                int total = host.count(base);
                for (int i = 0; i < parts.size(); i++) {
                    final int k = i;
                    List<String> without = new ArrayList<String>(parts);
                    without.remove(i);
                    int a1 = host.count(SmartLists.join(base, Quick.join(without)));
                    int a2 = host.count(SmartLists.join(base, Quick.join(parts)));
                    String p = parts.get(i);
                    TextView t = Ui.label(a, (Quick.isNot(p) ? Words.Quick.NOT : Words.Quick.MUST) + "  "
                            + (Quick.isNot(p) ? p.substring(1) : p) + "   " + (a2 - a1 >= 0 ? "+" : "−") + n(Math.abs(a2 - a1)));
                    t.setBackgroundColor(Ui.BG_SOFT);
                    t.setPadding(Ui.dp(a, 8), Ui.dp(a, 8), Ui.dp(a, 8), Ui.dp(a, 8));
                    t.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Ui.options(a, parts.get(k), new String[]{Words.Quick.FLIP, Words.Quick.REMOVE},
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface d, int w) {
                                            if (w == 0) parts.set(k, Quick.flip(parts.get(k)));
                                            else parts.remove(k);
                                            redraw[0].run();
                                        }
                                    });
                        }
                    });
                    rows.addView(t);
                }
                preview.setText(Words.Quick.preview(n(total), n(host.count(SmartLists.join(base, Quick.join(parts))))));
            }
        };
        android.widget.Button add = new android.widget.Button(a);
        add.setText(Words.Quick.ADD_COND);
        add.setAllCaps(false);
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addCond(SmartLists.join(base, Quick.join(parts)), new Ui.Text1() {
                    @Override
                    public void ok(String part) {
                        parts.add(part);
                        redraw[0].run();
                    }
                });
            }
        });
        box.addView(add);
        box.addView(preview);
        redraw[0].run();
        Ui.askForm(a, editing == null ? Words.Quick.TITLE_NEW : Words.Quick.TITLE_EDIT, box, new Runnable() {
            @Override
            public void run() {
                String nm = name.getText().toString().trim().replace("=", "").replace(Quick.SEP, "");
                if (nm.isEmpty() || nm.equals(Quick.START_ON)) {
                    Ui.toast(a, Words.NEED_NAME);
                    Ui.keepFormOpen();
                    return;
                }
                if (parts.isEmpty()) {
                    Ui.toast(a, Words.Quick.NEED_COND);
                    Ui.keepFormOpen();
                    return;
                }
                for (Quick.Button x : all) if (x != editing && x.name.equals(nm)) nm = nm + " (2)";
                if (editing == null) {
                    all.add(new Quick.Button(nm, Quick.join(parts)));
                } else {
                    if (on.remove(editing.name)) on.add(nm);
                    editing.name = nm;
                    editing.cond = Quick.join(parts);
                    host.saveState(Quick.state(on));
                }
                Quick.save(all);
                draw();
                host.changed();
            }
        });
    }

    /** "➕ תנאי": מה בודקים → איזה ערך (עם הכמות) → חייב או בלי (עם מה שיישאר). */
    private void addCond(final String current, final Ui.Text1 done) {
        final String[] kinds = {Words.Quick.KIND, Words.Quick.FOLDER, Words.Quick.WORD, Words.Quick.SIZE, Words.Quick.DATE};
        Ui.options(a, Words.Quick.WHAT, kinds, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int w) {
                if (w == 0) pickKind(current, done);
                else if (w == 1) FolderPicker.choose(a, Words.Quick.FOLDER, false, new FolderPicker.Picked() {
                    @Override
                    public void ok(File folder) {
                        mustOrNot(current, Cond.SOURCE + ":" + Cond.quote(folder.getAbsolutePath()), done);
                    }
                });
                else if (w == 2) Ui.askText(a, Words.Quick.WORD, "", Words.Quick.WORD_HINT, new Ui.Text1() {
                    @Override
                    public void ok(String value) {
                        if (!value.trim().isEmpty()) mustOrNot(current, Cond.quote(value.trim()), done);
                    }
                });
                else pickValue(current, w == 3 ? Cond.SIZE : Cond.DATE, w == 3 ? Words.Quick.SIZES : Words.Quick.DATES, done);
            }
        });
    }

    /** סוגים: הקבוצות ואז הסיומות — לפי כמה יש מהם בתוצאות של עכשיו. */
    private void pickKind(final String current, final Ui.Text1 done) {
        Map<String, Integer> ext = new LinkedHashMap<String, Integer>();
        for (Library.Hit h : host.hits(current)) {
            if (h.isDir()) continue;
            String e = Text.ext(h.name());
            if (e.isEmpty()) continue;
            Integer c = ext.get(e);
            ext.put(e, c == null ? 1 : c + 1);
        }
        final List<String> values = new ArrayList<String>();
        List<String> labels = new ArrayList<String>();
        for (Types.Group g : Types.groups()) {
            int c = 0;
            for (String e : g.exts) if (ext.containsKey(e)) c += ext.get(e);
            values.add(g.name);
            labels.add(Words.Quick.files(g.name, n(c)));
        }
        List<Map.Entry<String, Integer>> l = new ArrayList<Map.Entry<String, Integer>>(ext.entrySet());
        java.util.Collections.sort(l, new java.util.Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> x, Map.Entry<String, Integer> y) {
                return y.getValue().compareTo(x.getValue());
            }
        });
        for (int i = 0; i < l.size() && i < 40; i++) {
            values.add(l.get(i).getKey());
            labels.add(Words.Quick.files(l.get(i).getKey(), n(l.get(i).getValue())));
        }
        Ui.options(a, Words.Quick.KIND, labels.toArray(new String[0]), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int w) {
                mustOrNot(current, Cond.TYPE + ":" + values.get(w), done);
            }
        });
    }

    private void pickValue(final String current, final String key, final String[] values, final Ui.Text1 done) {
        String[] labels = new String[values.length];
        for (int i = 0; i < values.length; i++)
            labels[i] = Words.Quick.files(values[i], n(host.count(SmartLists.join(current, key + ":" + values[i]))));
        Ui.options(a, key, labels, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int w) {
                mustOrNot(current, key + ":" + values[w], done);
            }
        });
    }

    /** חייב או בלי — ולכל אחד: כמה יישארו. */
    private void mustOrNot(String current, final String part, final Ui.Text1 done) {
        String[] labels = {
                Words.Quick.MUST + " — " + Words.Quick.leaves(n(host.count(SmartLists.join(current, part)))),
                Words.Quick.NOT + " — " + Words.Quick.leaves(n(host.count(SmartLists.join(current, Cond.NOT + part))))};
        Ui.options(a, part, labels, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int w) {
                done.ok(w == 0 ? part : Cond.NOT + part);
            }
        });
    }
}
