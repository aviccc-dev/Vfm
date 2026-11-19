package com.tikiyot.vfm;

// מבנה 289 (אפיון החיפוש, 6 — "מקום אחד לעריכה"; המשתמש: "א", ועל התבניות: "1"): השורה שבראש חלונית ההגדרות של החיפוש —
// 📥 לטעון (המוכנות והרשימות החכמות של האופן — Lists.templates), ↕ מיון, ▦ תצוגה, 💾 שמירה בשם, ו"💾 שמירה" כשעל המסך רשימה חכמה.
// מה שהיה רק בטופס הנפרד ובתפריט — כאן, ליד התנאים. אחראי: הכפתורים והבחירה. אסור לו: לשמור או לחפש — המסך עושה (Host).

import android.app.Activity;
import android.content.DialogInterface;
import android.view.View;
import android.widget.Button;

import java.util.List;

final class PanelTools {

    interface Host {
        /** נבחרה תבנית — התנאי שלה (הגדרות לחלונית, מילים לשורה), המיון והתצוגה. */
        void load(String cond, String sort, String view);

        void sort();

        void view();

        void saveAs();

        /** null — אין רשימה חכמה על המסך (ואין "💾 שמירה"). */
        Runnable saveHere();
    }

    private PanelTools() { }

    /** השורה — Flow (גולשת לשורה הבאה כשאין מקום). mode — האופן שממנו נטענות הרשימות החכמות (null — רק המוכנות). */
    static View row(final Activity a, final String mode, final Host host) {
        Flow f = new Flow(a, 6, 4);
        f.addView(button(a, Words.Find.LOAD, new Runnable() {
            @Override public void run() { pick(a, mode, host); }
        }));
        f.addView(button(a, Words.Find.SORT_B, new Runnable() {
            @Override public void run() { host.sort(); }
        }));
        f.addView(button(a, Words.Find.VIEW_B, new Runnable() {
            @Override public void run() { host.view(); }
        }));
        final Runnable here = host.saveHere();
        if (here != null) f.addView(button(a, Words.Find.SAVE_HERE_B, here));
        f.addView(button(a, Words.Find.SAVE_AS_B, new Runnable() {
            @Override public void run() { host.saveAs(); }
        }));
        return f;
    }

    private static Button button(Activity a, String text, final Runnable r) {
        Button b = new Button(a);
        b.setText(text);
        b.setAllCaps(false);
        b.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { r.run(); }
        });
        return b;
    }

    private static void pick(Activity a, String mode, final Host host) {
        List<String[]> t;
        try {
            t = mode == null ? Lists.templates(null) : Db.read(a, mode, new Db.Use<List<String[]>>() {
                @Override public List<String[]> run(SqlStore s) { return Lists.templates(s); }
            });
        } catch (Throwable e) {
            Ui.toast(a, Words.Db.failed(String.valueOf(e.getMessage())));
            t = Lists.templates(null);                                          // המסד לא נקרא — לפחות המוכנות (ונאמר)
        }
        final List<String[]> all = t;
        String[] labels = new String[all.size()];
        for (int i = 0; i < labels.length; i++) labels[i] = Words.Find.template(all.get(i)[0], "1".equals(all.get(i)[4]));
        Ui.options(a, Words.Find.LOAD_TITLE, labels, new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface d, int w) {
                String[] x = all.get(w);
                host.load(x[1], x[2], x[3]);
            }
        });
    }
}
