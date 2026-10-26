package com.tikiyot.vfm;

// 1.16.0: "הגדרות" של רשימה ידנית — טופס אחד, מכל מקום (המילון, `claude/milon.md`, 10–11: "אפשר לכלול את הכל
// בהגדרות"): שם · צלמית · צבע · תיאור · "למה" · עיצוב. עד כאן — ארבעה מקומות שונים: "שינוי שם" בבחירה, "צלמית
// ותיאור" בתפריט ⋮ ובבחירה, "🎨 עיצוב" בתפריט ⋮, בבחירה ובתפריט הכלי, ו"שינוי שם ותיאור" לכלי בבית.
// אחראי: הטופס והשמירה ברשימה עצמה (שם חדש — כל מה שעובד עליה עובר איתה: FnVirtual.listMoved).

import android.app.Activity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

final class ListSettings {

    /** אחרי שמירה: המסלול החדש של הרשימה (אם השם השתנה), השם והתיאור. */
    interface Done {
        void saved(List<String> path, String name, String desc);
    }

    private ListSettings() { }

    /** הטופס לרשימה path (לא השורש). */
    static void show(Activity a, List<String> path, Done done) {
        show(a, path, Words.VClipW.SETTINGS, done);
    }

    /** title — "⚙ הגדרות", או (מקישור) "⚙ עריכת הרשימה המקורית" (המשתמש: "צריך להדגיש שהגדרות עורך את הרשימה עצמה"). */
    static void show(final Activity a, final List<String> path, String title, final Done done) {
        Tree tr = Tree.load();
        final Tree.Node n = tr.find(path);
        if (n == null || path.isEmpty()) return;
        final List<String> parent = new ArrayList<String>(path.subList(0, path.size() - 1));
        final Design where = Design.forList(tr, parent);                      // הצלמית — בעיצוב של המקום שבו היא מוצגת
        LinearLayout form = Ui.form(a);
        form.addView(Ui.label(a, Words.ToolM.NAME));
        final EditText name = Ui.field(a, n.name, null);
        form.addView(name);
        form.addView(Ui.label(a, Words.DesignW.ICON_IN + " · " + where.name));
        final EditText icon = Ui.field(a, where.iconOf(path, n.icon), "📁");
        form.addView(icon);
        form.addView(Ui.label(a, Words.DesignW.COLOR));
        final EditText color = Ui.field(a, where.colorOf(path), Words.DesignW.COLOR_HINT);
        form.addView(color);
        form.addView(Ui.label(a, Words.Manual.DESC));
        final EditText desc = Ui.field(a, n.desc, Words.Manual.DESC_HINT);
        desc.setSingleLine(false);
        form.addView(desc);
        form.addView(Ui.label(a, Words.WhyW.FIELD));
        final EditText why = Ui.field(a, n.why, Words.WhyW.HINT);
        why.setSingleLine(false);
        form.addView(why);
        Button design = new Button(a);                                        // העיצוב — חלון משלו (הוא רשימת עיצובים)
        design.setAllCaps(false);
        design.setText(Words.DesignW.MENU + " · " + (n.design.isEmpty() ? Words.DesignW.INHERIT : n.design));
        design.setOnClickListener(new android.view.View.OnClickListener() {
            @Override public void onClick(android.view.View v) { design(a, path, null); }
        });
        form.addView(design);
        Ui.askForm(a, title + ": " + n.name, form, new Runnable() {
            @Override
            public void run() {
                final String i = icon.getText().toString().trim(), d = desc.getText().toString().trim().replace('\n', ' ');
                final String w = why.getText().toString().trim().replace('\n', ' ');
                File f = where.file != null ? where.file : Design.ensure(where.name);
                if (!Design.setPrivate(f, Design.key(path), i, color.getText().toString().trim())) {
                    Ui.toast(a, Words.SAVE_FAILED);
                    return;
                }
                final String wanted = Tree.cleanName(name.getText().toString());
                final String[] now = {null};
                Tree.edit(new Tree.Edit() {
                    @Override
                    public boolean apply(Tree t) {
                        Tree.Node node = t.find(path);
                        if (node == null) return false;
                        node.icon = "";                                       // הצלמית — בקובץ העיצוב (1.12.0)
                        node.desc = d;
                        node.why = w;
                        if (!wanted.isEmpty() && !wanted.equals(node.name)) {
                            String was = node.name;
                            node.name = node.parent.uniqueChildName(wanted, node);
                            int at = node.parent.order.indexOf("c" + was);    // 1.17.2: מקומה בסדר החופשי נשאר
                            if (at >= 0) node.parent.order.set(at, "c" + node.name);
                        }
                        now[0] = node.name;
                        return true;
                    }
                });
                List<String> to = new ArrayList<String>(parent);
                to.add(now[0] != null ? now[0] : n.name);
                if (!to.equals(path)) FnVirtual.listMoved(path, to);         // הכלים, הקישורים, הכפתורים — איתה
                if (done != null) done.saved(to, to.get(to.size() - 1), d);
            }
        });
    }

    /** "🎨 עיצוב" לרשימה path — בשם, או כמו הרשימה שמעליה. after — אחרי שינוי (או null). */
    static void design(final Activity a, final List<String> path, final Runnable after) {
        Tree t = Tree.load();
        DesignUi.choose(a, Words.DesignW.TITLE_LIST + (path.isEmpty() ? "" : ": " + path.get(path.size() - 1)),
                Design.nameFor(t, path), Design.inheritedFrom(t, path), true,
                new DesignUi.Done() {
                    @Override
                    public void chosen(final String name) {
                        Tree.edit(new Tree.Edit() {
                            @Override
                            public boolean apply(Tree t) {
                                Tree.Node n = t.find(path);
                                if (n == null) return false;
                                n.design = name;
                                return true;
                            }
                        });
                        if (after != null) after.run();
                    }

                    @Override public void changed() { if (after != null) after.run(); }
                });
    }
}
