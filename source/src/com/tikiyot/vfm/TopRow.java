package com.tikiyot.vfm;

// שורות הכפתורים בכל מסכי התוכנה (פריטים 2 ו־9). 1.9.0: שורה עליונה. 1.10.0: ארבעה מקומות —
// למעלה (בכל המסכים) · למעלה של סוג המסך · [התוכן] · למטה של סוג המסך · למטה (בכל המסכים); בכל מקום עד 5 שורות.
// נגיעה — הפעולה; נגיעה ארוכה על כפתור — עריכה / הזזה / העברה לשורה אחרת / מחיקה / חזרה לברירת מחדל.
// ➕ "כפתור" בסוף כל שורה מוסיף כפתור; נגיעה ארוכה עליו — כפתור חדש / שורה חדשה / מחיקת השורה / חזרה לברירת מחדל.
// עוגנים: ➕ בסוף כל שורה; 🏠 בית ו־→ חזרה — תמיד באחת השורות הכלליות, והפעולה שלהם קבועה.
// פס הבחירה (פעולות על הנבחרים) — נשאר כמו שהוא; בזמן בחירה השורות התחתונות מוסתרות.
// אחראי: הממשק והביצוע. הנתונים — Buttons. פעולות של סוג מסך — המסך עצמו (Host).

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

final class TopRow {

    /** מסך שיש לו פעולות משלו (מה שהיה כפתורים קבועים בתחתית). */
    interface Host {
        /** הפעולות של המסך: {פעולה, סמל, שם}. */
        List<String[]> actions();

        /** ביצוע פעולה של המסך; false — לא שלו. */
        boolean act(String code);
    }

    static final String BOTTOM_TAG = "rows-bottom";  // תחביר

    private final Activity a;
    private final String screen;
    private final LinearLayout top, bottom;
    private long built = -1;

    private TopRow(Activity a, String screen, LinearLayout top, LinearLayout bottom) {
        this.a = a;
        this.screen = screen;
        this.top = top;
        this.bottom = bottom;
    }

    /** סוג המסך — שם המקום של השורות שלו. */
    static String screenOf(Activity a) {
        if (a instanceof VirtualScreen) return Words.Top.S_LIST;
        if (a instanceof Home) return Words.Top.S_HOME;
        if (a instanceof SearchScreen)                                        // 1.17.3: רשימה חכמה — אותה שורה כמו ידנית
            return ((SearchScreen) a).isList() ? Words.Top.S_LIST : Words.Top.S_SEARCH;
        if (a instanceof DiskScreen) return Words.Top.S_DISK;
        if (a instanceof TrashScreen) return Words.Top.S_TRASH;
        if (a instanceof DupScreen) return Words.Top.S_DUP;
        if (a instanceof SettingsScreen) return Words.Top.S_SETTINGS;
        if (a instanceof BatchEditScreen) return Words.Top.S_BATCH;
        if (a instanceof AsIfScreen) return Words.Top.S_ASIF;
        return Words.Top.S_EDITOR;
    }

    /** המסך עם השורות סביבו. כל המסכים קוראים לזה במקום setContentView(content). */
    static View wrap(final Activity a, View content) {
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setLayoutDirection(Ui.DIRECTION);
        box.setBackgroundColor(Ui.BG);
        LinearLayout top = new LinearLayout(a);
        top.setOrientation(LinearLayout.VERTICAL);
        LinearLayout bottom = new LinearLayout(a);
        bottom.setOrientation(LinearLayout.VERTICAL);
        bottom.setTag(BOTTOM_TAG);
        box.addView(top, new LinearLayout.LayoutParams(-1, -2));
        if (content.getParent() instanceof ViewGroup) ((ViewGroup) content.getParent()).removeView(content);
        box.addView(content, new LinearLayout.LayoutParams(-1, 0, 1f));
        box.addView(bottom, new LinearLayout.LayoutParams(-1, -2));
        final TopRow t = new TopRow(a, screenOf(a), top, bottom);
        t.build();
        // מסך שחזר לקדמה — אם השורות שונו במסך אחר, נבנות מחדש
        box.getViewTreeObserver().addOnWindowFocusChangeListener(new ViewTreeObserver.OnWindowFocusChangeListener() {
            @Override
            public void onWindowFocusChanged(boolean focus) {
                if (focus && t.built != Buttons.stamp()) t.build();
            }
        });
        return box;
    }

    /** השורות התחתונות של המסך — כדי שפס הבחירה יסתיר אותן בזמן בחירה. */
    static View bottomOf(View box) {
        return box.findViewWithTag(BOTTOM_TAG);
    }

    private String[] places() {
        return new String[]{Buttons.TOP, Buttons.local(Buttons.TOP, screen),
                Buttons.local(Buttons.BOTTOM, screen), Buttons.BOTTOM};
    }

    private void build() {
        built = Buttons.stamp();
        top.removeAllViews();
        bottom.removeAllViews();
        final Buttons.Layout lay = Buttons.loadAll();
        for (String place : places()) {
            List<List<Buttons.Btn>> rows = lay.places.get(place);
            if (rows == null) continue;
            boolean up = place.startsWith(Buttons.TOP);
            for (List<Buttons.Btn> row : rows) (up ? top : bottom).addView(rowView(lay, place, row, up));
        }
    }

    /**
     * 1.17.3: שורה אחת לכל הרשימות — ידניות וחכמות (המשתמש: "שורה למסך הבית, ושורה נפרדת לירידה ברמה לתוך רשימה —
     * לא משנה חכמה או ידנית"). כפתור של פעולה שרק סוג אחד מכיר (למשל "מהכונן" ברשימה ידנית) — לא מוצג בשני.
     */
    private boolean fits(Buttons.Btn b) {
        String code = Verbs.isVerb(b.act) ? Verbs.hostCode(Verbs.parse(b.act)) : b.act.startsWith(Buttons.ACT) ? b.act : null;
        if (code == null) return true;
        if (!(a instanceof Host)) return false;
        for (String[] x : ((Host) a).actions()) if (x[0].equals(code)) return true;
        return false;
    }

    private View rowView(final Buttons.Layout lay, final String place, final List<Buttons.Btn> row, boolean up) {
        LinearLayout wrap = new LinearLayout(a);
        wrap.setOrientation(LinearLayout.VERTICAL);
        HorizontalScrollView sv = new HorizontalScrollView(a);
        sv.setHorizontalScrollBarEnabled(false);
        sv.setBackgroundColor(Buttons.general(place) ? Ui.BAR : Ui.BG_SOFT);
        LinearLayout r = new LinearLayout(a);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setLayoutDirection(Ui.DIRECTION);
        int p = Ui.dp(a, 4);
        r.setPadding(p, p, p, p);
        sv.addView(r);
        for (int i = 0; i < row.size(); i++) {
            final int at = i;
            final Buttons.Btn b = row.get(i);
            if (!fits(b)) continue;                                           // 1.17.3: פעולה שהמסך הזה לא מכיר — לא מוצגת
            View v = button(b.icon, b.name, Ui.ACCENT);
            v.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View x) { run(b); }
            });
            v.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View x) {
                    if (!stale()) menu(lay, place, row, at);
                    return true;
                }
            });
            r.addView(v);
        }
        View add = button("➕", Words.Top.ADD, Ui.SUB);                       // עוגן: תמיד בסוף השורה
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View x) {                                     // 1.11.0: נגיעה רגילה — תפריט גלוי (כפתור / שורה)
                if (stale()) return;
                Ui.options(a, describe(place), new String[]{Words.Top.NEW_BUTTON, Words.Top.NEW_ROW},
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int w) {
                                if (w == 0) addNew(lay, row); else newRow(lay);
                            }
                        });
            }
        });
        add.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View x) {
                if (!stale()) rowMenu(lay, place, row);
                return true;
            }
        });
        r.addView(add);
        View rule = new View(a);
        rule.setBackgroundColor(Ui.RULE);
        if (!up) wrap.addView(rule, new LinearLayout.LayoutParams(-1, 1));
        wrap.addView(sv, new LinearLayout.LayoutParams(-1, -2));
        if (up) wrap.addView(rule, new LinearLayout.LayoutParams(-1, 1));
        return wrap;
    }

    private View button(String icon, String name, int color) {
        LinearLayout b = new LinearLayout(a);
        b.setOrientation(LinearLayout.VERTICAL);
        b.setGravity(Gravity.CENTER);
        int p = Ui.dp(a, 6);
        b.setPadding(p, Ui.dp(a, 2), p, Ui.dp(a, 2));
        b.setMinimumWidth(Ui.dp(a, 52));
        b.setClickable(true);
        b.setFocusable(true);
        android.util.TypedValue tv = new android.util.TypedValue();
        if (a.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true))
            b.setBackgroundResource(tv.resourceId);
        TextView ic = new TextView(a);
        ic.setText(icon);
        ic.setTextSize(18);
        ic.setTextColor(color);
        ic.setGravity(Gravity.CENTER);
        TextView nm = new TextView(a);
        nm.setText(name);
        nm.setTextSize(11);
        nm.setTextColor(color);
        nm.setMaxLines(2);                                                   // 1.10.0: שם ארוך — בשתי שורות, לא דוחק את השורה
        nm.setEllipsize(android.text.TextUtils.TruncateAt.END);
        nm.setMaxWidth(Ui.dp(a, 96));
        nm.setGravity(Gravity.CENTER);
        b.addView(ic);
        b.addView(nm);
        return b;
    }

    // ---------- הביצוע ----------

    /** 1.13.0: כפתור = שורת פקודה. פקודת פתיחה (פתח / חזור / מצא / הרץ / הוסף…) — מיד; אחרת — שורה של קובץ פקודות. */
    private void run(Buttons.Btn b) {
        String act = b.act;
        if (Verbs.run(a, act, null)) return;
        File f = new File(Paths.system(), "כפתור." + Batch.EXT);   // בכונן
        try {
            Files.writeAtomic(f, act + "\n");
            BatchUi.run(a, f);
        } catch (Throwable e) {
            Log.e("כפתור — שורת פקודה", e);
        }
    }

    /** 1.11.0: המסך יודע מה "כאן" שלו (ב"קבצים פיזיים" — התיקייה הפתוחה). */
    interface Here {
        File hereDir();
    }

    // ---------- עריכה ----------

    /** 1.13.0 (באג): השורות שונו במסך אחר מאז שהמסך הזה נבנה — לבנות מחדש לפני כל שינוי, כדי לא לדרוס. */
    private boolean stale() {
        if (built == Buttons.stamp()) return false;
        build();
        Ui.toast(a, Words.Top.STALE);
        return true;
    }

    private void saved(Buttons.Layout lay, boolean changed) {
        if (changed) Buttons.save(lay);
        build();
    }

    /** נגיעה ארוכה על כפתור. */
    private void menu(final Buttons.Layout lay, final String place, final List<Buttons.Btn> row, final int at) {
        final Buttons.Btn b = row.get(at);
        final List<String> items = new ArrayList<String>();
        items.add(Words.Top.EDIT);
        if (at > 0) items.add(Words.Top.RIGHT);
        if (at < row.size() - 1) items.add(Words.Top.LEFT);
        items.add(Words.Top.MOVE_ROW);
        if (Buttons.removable(lay, place, row, at)) items.add(Words.Top.DELETE);
        items.add(Words.Top.RESET);
        Ui.list(a, b.icon + " " + b.name + (b.anchor() ? " · " + Words.Top.ANCHOR : ""), items,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        String it = items.get(w);
                        if (it.equals(Words.Top.EDIT)) edit(lay, b);
                        else if (it.equals(Words.Top.RIGHT)) saved(lay, Buttons.move(row, at, -1));
                        else if (it.equals(Words.Top.LEFT)) saved(lay, Buttons.move(row, at, 1));
                        else if (it.equals(Words.Top.MOVE_ROW)) moveToRow(lay, place, row, at);
                        else if (it.equals(Words.Top.DELETE)) {
                            row.remove(at);
                            saved(lay, true);
                        } else askReset(lay, place);
                    }
                });
    }

    /** נגיעה ארוכה על ➕ בסוף שורה. */
    private void rowMenu(final Buttons.Layout lay, final String place, final List<Buttons.Btn> row) {
        final List<String> items = new ArrayList<String>();
        items.add(Words.Top.NEW_BUTTON);
        items.add(Words.Top.NEW_ROW);
        if (Buttons.rowRemovable(lay, place, row)) items.add(Words.Top.DELETE_ROW);
        items.add(Words.Top.RESET);
        Ui.list(a, describe(place), items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int w) {
                String it = items.get(w);
                if (it.equals(Words.Top.NEW_BUTTON)) addNew(lay, row);
                else if (it.equals(Words.Top.NEW_ROW)) newRow(lay);
                else if (it.equals(Words.Top.DELETE_ROW)) {
                    lay.rows(place).remove(row);
                    saved(lay, true);
                } else askReset(lay, place);
            }
        });
    }

    /** שם המקום כמו שהמשתמש מבין אותו. */
    private String describe(String place) {
        if (place.equals(Buttons.TOP)) return Words.Top.P_TOP;
        if (place.equals(Buttons.BOTTOM)) return Words.Top.P_BOTTOM;
        return (place.startsWith(Buttons.TOP) ? Words.Top.P_TOP_HERE : Words.Top.P_BOTTOM_HERE) + screen;
    }

    private void newRow(final Buttons.Layout lay) {
        final String[] ps = places();
        String[] labels = new String[ps.length];
        for (int i = 0; i < ps.length; i++) labels[i] = describe(ps[i]);
        Ui.options(a, Words.Top.WHERE_ROW, labels, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int w) {
                List<List<Buttons.Btn>> rows = lay.rows(ps[w]);
                if (rows.size() >= Buttons.MAX_ROWS) {
                    Ui.toast(a, Words.Top.MAX_ROWS);
                    return;
                }
                List<Buttons.Btn> row = new ArrayList<Buttons.Btn>();
                rows.add(row);
                Buttons.save(lay);
                build();
                Ui.toast(a, Words.Top.ROW_ADDED);
            }
        });
    }

    private void moveToRow(final Buttons.Layout lay, final String place, final List<Buttons.Btn> row, final int at) {
        final List<String> targetsPlace = new ArrayList<String>();
        final List<List<Buttons.Btn>> targets = new ArrayList<List<Buttons.Btn>>();
        List<String> labels = new ArrayList<String>();
        for (String p : places()) {
            List<List<Buttons.Btn>> rows = lay.places.get(p);
            if (rows == null) continue;
            for (int i = 0; i < rows.size(); i++) {
                if (rows.get(i) == row) continue;
                targetsPlace.add(p);
                targets.add(rows.get(i));
                labels.add(describe(p) + (rows.size() > 1 ? " · " + Words.Top.ROW + (i + 1) : ""));
            }
        }
        if (targets.isEmpty()) {
            Ui.toast(a, Words.Top.NO_OTHER_ROW);
            return;
        }
        Ui.list(a, Words.Top.MOVE_ROW, labels, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int w) {
                if (Buttons.moveTo(lay, place, row, at, targetsPlace.get(w), targets.get(w))) saved(lay, true);
                else Ui.toast(a, Words.Top.ANCHOR);
            }
        });
    }

    private void askReset(final Buttons.Layout lay, final String place) {
        Ui.options(a, Words.Top.RESET_ASK, new String[]{Words.Top.RESET_PLACE + describe(place), Words.Top.RESET_ALL},
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        if (w == 0) {
                            Buttons.reset(lay, place);
                            saved(lay, true);
                        } else {
                            Ui.ask(a, Words.Top.RESET_ALL, null, Words.Top.RESET_WHAT, null, Words.Top.RESET, new Runnable() {
                                @Override
                                public void run() {
                                    Buttons.reset();
                                    build();
                                }
                            });
                        }
                    }
                });
    }

    /** עריכה: שם, סמל, ומה הכפתור עושה (לעוגן — קבוע). */
    private void edit(final Buttons.Layout lay, final Buttons.Btn b) {
        final String[] act = {b.act};
        LinearLayout form = Ui.form(a);
        form.addView(Ui.label(a, Words.Top.NAME));
        final EditText name = Ui.field(a, b.name, Words.Top.NAME);
        form.addView(name);
        form.addView(Ui.label(a, Words.Top.ICON));
        final EditText icon = Ui.field(a, b.icon, "⭐");
        form.addView(icon);
        final TextView what = Ui.label(a, Words.Top.ACTION + act[0] + (b.anchor() ? Words.Top.FIXED : ""));
        form.addView(what);
        if (!b.anchor()) {
            Button change = new Button(a);
            change.setText(Words.Top.CHANGE);
            change.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    chooseTarget(new Chosen() {
                        @Override
                        public void ok(String i, String n, String ac) {
                            act[0] = ac;
                            what.setText(Words.Top.ACTION + ac);
                        }
                    });
                }
            });
            form.addView(change);
        }
        Ui.askForm(a, Words.Top.EDIT, form, new Runnable() {
            @Override
            public void run() {
                String n = name.getText().toString().trim(), i = icon.getText().toString().trim();
                if (!n.isEmpty()) b.name = n.replace("|", " ");
                if (!i.isEmpty()) b.icon = i.replace("|", " ");
                b.act = act[0];
                saved(lay, true);
                Ui.toast(a, Words.Top.SAVED_AS + b.icon + " " + b.name);   // 1.13.0: שם ארוך מקוצר בשורה — כאן רואים אותו כולו
            }
        });
    }

    private void addNew(final Buttons.Layout lay, final List<Buttons.Btn> row) {
        chooseTarget(new Chosen() {
            @Override
            public void ok(String icon, String name, String act) {
                Buttons.Btn b = new Buttons.Btn(icon, name.replace("|", " "), act);
                row.add(b);
                saved(lay, true);
                edit(lay, b);                                                 // לשנות שם או סמל מיד, אם רוצים
            }
        });
    }

    interface Chosen {
        void ok(String icon, String name, String act);
    }

    private void chooseTarget(final Chosen cb) {
        pick(a, screen, cb);
    }

    /** 1.17.0: "☰ תפריט" — רשימה מ"שמור לתוכנה" (המשתמש: "בכפתורים תהיה אפשרות לקחת רשימה כתפריט — רק משמור לתוכנה"). */
    private static void menus(Activity a, final Chosen cb) {
        ModeCopy.Side side = Reserved.inUse();
        Tree.Node root = side == null ? null : side.tree.find(Reserved.ROOT_PATH);
        if (root == null) {
            Ui.toast(a, Words.Verb.NOT_FOUND + Reserved.ROOT);
            return;
        }
        final List<List<String>> paths = new ArrayList<List<String>>();
        List<String> labels = new ArrayList<String>();
        collect(root, 0, paths, labels);
        Ui.list(a, Words.Top.T_MENU, labels, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int k) {
                List<String> p = paths.get(k);
                cb.ok("☰", p.get(p.size() - 1), Verbs.openMenu(p));
            }
        });
    }

    private static void collect(Tree.Node n, int depth, List<List<String>> paths, List<String> labels) {
        for (Tree.Node c : n.children) {
            StringBuilder in = new StringBuilder();
            for (int i = 0; i < depth; i++) in.append("    ");
            paths.add(c.path());
            labels.add(in + "☰ " + c.name);
            collect(c, depth + 1, paths, labels);
        }
    }

    /**
     * "מה הכפתור יעשה?" — 1.16.1: אותו תפריט כמו ➕ בבית (המשתמש: "שני המקומות אמורים להיות זהים; ההבדל היחיד —
     * הפלוס מוסיף במסך הבית, והשני מוסיף כפתור במקום שמחליטים"): רשימה ידנית · רשימה חכמה · פקודה (תפריט הפקודות
     * האחד — ובו גם שורה בכתב, תיקייה במנהל הקבצים ופעולה של המסך). התוצאה — שורת פקודה.
     */
    static void pick(final Activity a, final String screen, final Chosen cb) {
        Ui.options(a, Words.Top.WHAT, new String[]{Words.Main.ADD_MANUAL, Words.Main.ADD_SMART, Words.Main.ADD_CMD, Words.Top.T_MENU},
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int w) {
                if (w == 0) ListPicker.choose(a, Words.Main.ADD_MANUAL.trim(), null, new ListPicker.Picked() {
                    @Override
                    public void ok(ArrayList<String> path) {
                        if (path.isEmpty()) return;
                        cb.ok("📋", path.get(path.size() - 1), Verbs.openList(path));
                    }
                });
                else if (w == 1) {
                    final List<SmartLists.Named> all = SmartLists.all();
                    if (all.isEmpty()) {
                        Ui.toast(a, Words.Top.NO_SMART);
                        return;
                    }
                    List<String> names = new ArrayList<String>();
                    for (SmartLists.Named n : all) names.add("✨ " + n.name);
                    Ui.list(a, Words.Main.ADD_SMART.trim(), names, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface d2, int k) {
                            cb.ok("✨", all.get(k).name, Verbs.openSmart(all.get(k).name));
                        }
                    });
                } else if (w == 3) menus(a, cb);                                  // 1.17.0: רשימה מ"שמור לתוכנה" כתפריט
                else CmdMenu.show(a, null, new CmdMenu.Picked() {
                    @Override
                    public void ok(String icon, String name, String line, File file) { cb.ok(icon, name, line); }
                }, screen);
            }
        });
    }
}
