package com.tikiyot.vfm;

// 1.12.0: חלון "🎨 עיצוב" — בחירת עיצוב בשם (מהאופן הזה ומאופנים אחרים), "כמו הרשימה שמעליה",
// עריכה, "שמירה בשם" ו"חזרה לברירת מחדל". משותף למסך הבית ולרשימה ידנית.

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

final class DesignUi {

    interface Done {
        /** נבחר עיצוב: השם שלו באופן הזה; "" — לרשת מהרשימה שמעליה. */
        void chosen(String name);

        /** העיצוב הנוכחי השתנה (עריכה / חזרה לברירת מחדל) — לצייר מחדש. */
        void changed();
    }

    private DesignUi() { }

    /**
     * current — העיצוב שבתוקף עכשיו. from — מאיפה הוא בא: null — נבחר כאן; "" — ממסך הבית; אחרת — שם הרשימה שמעליה.
     * canInherit — האם להציע "כמו הרשימה שמעליה".
     */
    static void choose(final Activity a, String title, final String current, String from, final boolean canInherit,
                       final Done done) {
        Library lib = Library.get(a);
        final List<String[]> all = Design.all(lib.ready ? lib.byEnding(Design.EXT) : null);   // גם מה שהגיע בשיתוף
        final List<String> items = new ArrayList<String>();
        for (String[] d : all)
            items.add((d[1].isEmpty() && d[0].equals(current) ? "✓ " : "") + d[0]
                    + (d[1].isEmpty() ? "" : Words.DesignW.FROM_MODE + d[1]));
        final int inherit = canInherit ? items.size() : -1;
        if (canInherit) items.add((from != null ? "✓ " : "") + Words.DesignW.INHERIT);
        final int form = items.size();
        items.add(Words.DesignW.EDIT_FORM);                                   // 1.12.1: הטופס
        final int edit = items.size();
        items.add(Words.DesignW.EDIT);
        items.add(Words.DesignW.SAVE_AS);
        items.add(Words.DesignW.RESET);
        String now = Words.DesignW.NOW + current + (from == null ? "" : from.isEmpty() ? Words.DesignW.FROM_HOME
                : Words.DesignW.FROM_ABOVE + from + "\")");
        Ui.list(a, title + "\n" + now, items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dlg, int which) {
                if (which < all.size()) {
                    String name = Design.adopt(all.get(which));
                    if (name == null) Ui.toast(a, Words.DesignW.COPY_FAILED);
                    else done.chosen(name);
                } else if (which == inherit) {
                    done.chosen("");
                } else if (which == form) {
                    form(a, current, new Done() {
                        @Override public void chosen(String name) { done.chosen(name); }

                        @Override public void changed() { done.changed(); }
                    });
                } else if (which == edit) {
                    Design d = Design.get(current);
                    if (d.file != null && d.file.isFile()) Ui.editText(a, d.file);
                } else if (which == edit + 1) {
                    Ui.askText(a, Words.DesignW.SAVE_AS, "", Words.DesignW.SAVE_AS_HINT, new Ui.Text1() {
                        @Override
                        public void ok(String name) {
                            String err = Design.saveAs(Design.get(current), name);
                            if (err != null) Ui.toast(a, err);
                            else done.chosen(name.trim().replace('/', '∕'));
                        }
                    });
                } else {
                    if (!Design.builtIn(current)) {
                        Ui.toast(a, Words.DesignW.NOT_BUILT_IN);
                        return;
                    }
                    Ui.toast(a, Design.reset(current) ? Words.DesignW.RESET_DONE : Words.SAVE_FAILED);
                    done.changed();
                }
            }
        });
    }
    // ---------- 1.12.1: הטופס ----------

    /** שדה מספר קצר. */
    private static EditText number(Activity a, String value) {
        EditText e = Ui.field(a, value, null);
        e.setInputType(InputType.TYPE_CLASS_NUMBER);
        e.setMinWidth(Ui.dp(a, 52));
        e.setGravity(Gravity.CENTER);
        return e;
    }

    private static LinearLayout line(Activity a, View... views) {
        LinearLayout l = new LinearLayout(a);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        l.setLayoutDirection(Ui.DIRECTION);
        for (View v : views) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.setMarginEnd(Ui.dp(a, 8));
            l.addView(v, lp);
        }
        return l;
    }

    private static TextView word(Activity a, String s) {
        TextView t = new TextView(a);
        t.setText(s);
        t.setTextColor(Ui.TEXT);
        t.setTextSize(14);
        return t;
    }

    private static int num(EditText e, int def) {
        try {
            return Integer.parseInt(e.getText().toString().trim());
        } catch (NumberFormatException x) {
            return def;
        }
    }

    private static final String[] WEIGHTS = {Design.W_LIGHT, Design.W_REG, Design.W_MED, Design.W_BOLD};

    private static int weightIndex(int w) {
        String n = Design.weightName(w);
        for (int i = 0; i < WEIGHTS.length; i++) if (WEIGHTS[i].equals(n)) return i;
        return 1;
    }

    /** שורת טקסט בטופס: שם · גודל · משקל · גופן. */
    private static final class TextRow {
        final EditText size, font;
        final Spinner weight;
        final LinearLayout view;

        TextRow(Activity a, String label, Design.Text t) {
            size = number(a, "" + (int) t.size);
            weight = Ui.choice(a, WEIGHTS, weightIndex(t.weight));
            font = Ui.field(a, t.font, Design.SYSTEM);
            font.setMinWidth(Ui.dp(a, 80));
            TextView l = word(a, label);
            l.setMinWidth(Ui.dp(a, 52));
            view = line(a, l, size, weight, font);
        }

        void into(Design.Text t) {
            t.size = Math.max(6, Math.min(60, num(size, (int) t.size)));
            t.weight = Design.weight(WEIGHTS[weight.getSelectedItemPosition()]);
            String f = font.getText().toString().trim();
            t.font = f.isEmpty() ? Design.SYSTEM : f;
        }
    }

    /**
     * טופס לשינוי עיצוב (החלטות 15.10): תיבות סימון ושדות מספר, קומפקטי; משקל — רשימת בחירה;
     * המסך משתנה אחרי סגירת הטופס. "שמירה בשם…" — עותק חדש, והוא נבחר במקום.
     */
    static void form(final Activity a, final String name, final Done done) {
        final Design d = Design.get(name);
        LinearLayout f = Ui.form(a);
        final CheckBox grid = Ui.check(a, Words.DesignW.GRID, d.grid);
        final EditText cols = number(a, "" + d.cols);
        f.addView(line(a, grid, word(a, Words.DesignW.COLS), cols));
        final EditText icon = number(a, "" + d.icon), gap = number(a, "" + d.gap);
        f.addView(line(a, word(a, Words.DesignW.ICON_PCT), icon, word(a, Words.DesignW.GAP_PCT), gap));
        final CheckBox showIcon = Ui.check(a, Words.DesignW.SHOW_ICON, d.showIcon);
        final CheckBox frame = Ui.check(a, Words.DesignW.FRAME, Design.FRAME.equals(d.around));
        final CheckBox fill = Ui.check(a, Words.DesignW.FILL, Design.FILL.equals(d.around));
        frame.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {    // אחד מבטל את השני
            @Override public void onCheckedChanged(CompoundButton b, boolean on) { if (on) fill.setChecked(false); }
        });
        fill.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton b, boolean on) { if (on) frame.setChecked(false); }
        });
        f.addView(line(a, showIcon, frame, fill));
        final CheckBox tint = Ui.check(a, Words.DesignW.TINT, d.tint);
        f.addView(line(a, tint));
        final CheckBox showData = Ui.check(a, Words.DesignW.SHOW_DATA, d.showData);
        final CheckBox side = Ui.check(a, Words.DesignW.DATA_SIDE, !d.dataBelow);
        f.addView(line(a, showData, side));
        f.addView(Ui.label(a, Words.DesignW.TEXT_HEAD));
        final TextRow nameRow = new TextRow(a, Words.DesignW.T_NAME, d.nameT);
        final TextRow dataRow = new TextRow(a, Words.DesignW.T_DATA, d.data);
        final TextRow titleRow = new TextRow(a, Words.DesignW.T_TITLE, d.title);
        f.addView(nameRow.view);
        f.addView(dataRow.view);
        f.addView(titleRow.view);
        final CheckBox fit = Ui.check(a, Words.DesignW.FIT, d.fit);
        final EditText min = number(a, "" + d.min), lines = number(a, "" + d.lines);
        f.addView(line(a, fit, word(a, Words.DesignW.MIN), min, word(a, Words.DesignW.LINES), lines));

        final Runnable collect = new Runnable() {
            @Override
            public void run() {
                d.grid = grid.isChecked();
                d.cols = Math.max(1, Math.min(8, num(cols, d.cols)));
                d.icon = Math.max(0, Math.min(1000, num(icon, d.icon)));
                d.gap = Math.max(0, Math.min(200, num(gap, d.gap)));
                d.showIcon = showIcon.isChecked();
                d.around = frame.isChecked() ? Design.FRAME : fill.isChecked() ? Design.FILL : Design.NONE;
                d.tint = tint.isChecked();
                d.showData = showData.isChecked();
                d.dataBelow = !side.isChecked();
                nameRow.into(d.nameT);
                dataRow.into(d.data);
                titleRow.into(d.title);
                d.fit = fit.isChecked();
                d.min = Math.max(6, Math.min(40, num(min, d.min)));
                d.lines = Math.max(1, Math.min(6, num(lines, d.lines)));
            }
        };
        ScrollView sv = new ScrollView(a);
        sv.addView(f);
        final AlertDialog dlg = new AlertDialog.Builder(a)
                .setTitle(Words.DesignW.FORM_TITLE + name)
                .setView(sv)
                .setPositiveButton(Words.SAVE, null)
                .setNeutralButton(Words.DesignW.SAVE_AS_BTN, null)
                .setNegativeButton(Words.CANCEL, null)
                .create();
        dlg.show();
        dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                collect.run();
                if (!Design.save(d)) {
                    Ui.toast(a, Words.SAVE_FAILED);
                    return;
                }
                dlg.dismiss();
                done.changed();                                               // המסך משתנה אחרי הסגירה
            }
        });
        dlg.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Ui.askText(a, Words.DesignW.SAVE_AS, "", Words.DesignW.SAVE_AS_HINT, new Ui.Text1() {
                    @Override
                    public void ok(String newName) {
                        String err = Design.saveAs(d, newName);
                        if (err != null) {
                            Ui.toast(a, err);
                            return;
                        }
                        String n = newName.trim().replace('/', '∕');
                        collect.run();
                        Design copy = Design.get(n);
                        copyLook(d, copy);
                        if (!Design.save(copy)) {
                            Ui.toast(a, Words.SAVE_FAILED);
                            return;
                        }
                        dlg.dismiss();
                        done.chosen(n);
                    }
                });
            }
        });
    }

    /** המראה הכללי מעיצוב לעיצוב (לא השם, הקובץ והחלק הפרטי). */
    static void copyLook(Design from, Design to) {
        to.grid = from.grid;
        to.cols = from.cols;
        to.icon = from.icon;
        to.gap = from.gap;
        to.showIcon = from.showIcon;
        to.around = from.around;
        to.tint = from.tint;
        to.showData = from.showData;
        to.dataBelow = from.dataBelow;
        to.title = from.title.copy();
        to.nameT = from.nameT.copy();
        to.data = from.data.copy();
        to.fit = from.fit;
        to.min = from.min;
        to.lines = from.lines;
    }

    // ---------- 1.12.1: במסך ההגדרות — כל העיצובים של האופן ----------

    static void manage(final Activity a) {
        final List<String> names = new ArrayList<String>();
        for (java.io.File f : Design.files(Design.dir())) names.add(Design.nameOf(f));
        for (String b : new String[]{Design.DEFAULT, Design.FILES, Design.WORDS}) if (!names.contains(b)) names.add(b);
        final List<String> items = new ArrayList<String>();
        String home = Design.homeName();
        items.add(Words.DesignW.HOME_LINE + (home.isEmpty() ? Words.DesignW.HOME_NONE : home));
        for (String n : names) items.add("🎨 " + n);
        Ui.list(a, Words.DesignW.MANAGE, items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dlg, int which) {
                if (which == 0) {
                    String cur = Design.homeName();
                    choose(a, Words.DesignW.TITLE_HOME, cur.isEmpty() ? Design.DEFAULT : cur, null, false, new Done() {
                        @Override public void chosen(String name) { Design.setHome(name); manage(a); }

                        @Override public void changed() { manage(a); }
                    });
                } else {
                    one(a, names.get(which - 1));
                }
            }
        });
    }

    /** עיצוב אחד: שינוי בטופס, עריכה בקובץ, שמירה בשם, שינוי שם, מחיקה, חזרה לברירת מחדל. */
    private static void one(final Activity a, final String name) {
        final boolean built = Design.builtIn(name);
        List<String> items = new ArrayList<String>();
        items.add(Words.DesignW.EDIT_FORM);
        items.add(Words.DesignW.EDIT);
        items.add(Words.DesignW.SAVE_AS);
        items.add(Words.DesignW.RENAME);
        items.add(Words.DesignW.DELETE);
        if (built) items.add(Words.DesignW.RESET);
        final Done back = new Done() {
            @Override public void chosen(String n) { manage(a); }

            @Override public void changed() { manage(a); }
        };
        Ui.list(a, "🎨 " + name, items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dlg, int which) {
                if (which == 0) form(a, name, back);
                else if (which == 1) {
                    Design d = Design.get(name);
                    if (d.file != null && d.file.isFile()) Ui.editText(a, d.file);
                } else if (which == 2) {
                    Ui.askText(a, Words.DesignW.SAVE_AS, "", Words.DesignW.SAVE_AS_HINT, new Ui.Text1() {
                        @Override
                        public void ok(String n) {
                            String err = Design.saveAs(Design.get(name), n);
                            Ui.toast(a, err != null ? err : Words.DesignW.CHOSEN + n.trim());
                            manage(a);
                        }
                    });
                } else if (which == 3) {
                    Ui.askText(a, Words.DesignW.RENAME, name, null, new Ui.Text1() {
                        @Override
                        public void ok(String n) {
                            Design.ensure(name);
                            String err = Design.rename(name, n);
                            if (err != null) Ui.toast(a, err);
                            manage(a);
                        }
                    });
                } else if (which == 4) {
                    Ui.ask(a, Words.DesignW.DELETE + ": " + name, Words.DesignW.DELETE_ASK, null, null, Words.DesignW.DELETE,
                            new Runnable() {
                                @Override
                                public void run() {
                                    Ui.toast(a, Design.delete(name) ? Words.DesignW.DELETED : Words.DELETE_FAILED);
                                    manage(a);
                                }
                            });
                } else {
                    Ui.toast(a, Design.reset(name) ? Words.DesignW.RESET_DONE : Words.SAVE_FAILED);
                    manage(a);
                }
            }
        });
    }
}
