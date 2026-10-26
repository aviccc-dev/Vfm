package com.tikiyot.vfm;

// פונקציה: קבצים כפולים.

import android.app.Activity;
import android.content.Intent;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;

public final class FnDup extends Func {

    /** הסוג בקובץ הראשי (וסעיף ההגדרות של הכלי). */
    static final String KIND = "כפולים";  // תחביר


    public FnDup() {
        super(KIND, Words.DupT.DUPS, "⧉");
    }

    @Override
    public void open(Activity a, Target t) {
        Intent i = new Intent(a, DupScreen.class);
        i.putExtra(DupScreen.EXTRA_SCOPE, t.get("היקף", ""));  // תחביר
        i.putExtra(DupScreen.EXTRA_MODE, t.get("השוואה", "שם וגודל"));  // תחביר
        i.putExtra(DupScreen.EXTRA_AGAINST, between(t) ? t.get("מול", "") : "");  // תחביר
        i.putExtra(DupScreen.EXTRA_FUNC, t.id);
        a.startActivity(i);
    }

    static final String WITHIN = Words.DupT.WITHIN;
    static final String BETWEEN = Words.DupT.BETWEEN;

    /** האופן. */
    static boolean between(Target t) {
        return t.get("אופן", "").equals("בין");  // תחביר
    }

    @Override
    public void configure(final Activity a, String name, Target t, final Done done) {
        LinearLayout b = Ui.form(a);
        final EditText nm = Ui.field(a, name != null ? name : "", Words.NAME_OPTIONAL);
        final Spinner how = Ui.choice(a, new String[]{WITHIN, BETWEEN}, t != null && between(t) ? 1 : 0);
        final Sources srcA = new Sources(a, Words.DupT.NO_SOURCES_ALL);
        final Sources srcB = new Sources(a, Words.DupT.NEED_SOURCE);
        final String restA = t != null ? Sources.rest(t.get("היקף", "")) : "";  // תחביר
        final String restB = t != null ? Sources.rest(t.get("מול", "")) : "";  // תחביר
        if (t != null) {
            srcA.set(Sources.fromCond(t.get("היקף", "")));  // תחביר
            srcB.set(Sources.fromCond(t.get("מול", "")));  // תחביר
        }
        final DupMode was = DupMode.parse(t != null ? t.get("השוואה", "") : "");  // 195 // תחביר
        final android.widget.CheckBox nameSame = Ui.check(a, Words.DupT.NAME_SAME_BOX, was.name == DupMode.NAME_SAME);   // 196: סימון, כמו השאר
        final android.widget.CheckBox nameLike = Ui.check(a, Words.DupT.NAME_LIKE_BOX, was.name == DupMode.NAME_SIMILAR);
        final android.widget.CheckBox bySize = Ui.check(a, Words.DupT.SIZE_BOX, was.size && !was.content);
        final android.widget.CheckBox byContent = Ui.check(a, Words.DupT.CONTENT_BOX, was.content);
        final android.widget.TextView explain = Ui.label(a, "");
        final android.widget.TextView labelA = Ui.label(a, "");
        final android.widget.TextView labelB = Ui.label(a, Words.DupT.B_AGAINST);
        b.addView(Ui.label(a, Words.DupT.NAME));
        b.addView(nm);
        b.addView(Ui.label(a, Words.DupT.WHAT_WANT));
        b.addView(how);
        b.addView(labelA);
        b.addView(srcA.view);
        b.addView(labelB);
        b.addView(srcB.view);
        b.addView(Ui.label(a, Words.DupT.SAME_BY));
        b.addView(nameSame);
        b.addView(nameLike);
        b.addView(bySize);
        b.addView(byContent);
        b.addView(explain);
        final Runnable explainIt = new Runnable() {
            @Override
            public void run() {
                if (byContent.isChecked()) bySize.setChecked(true);
                bySize.setEnabled(!byContent.isChecked());
                explain.setText(Words.DupT.explain(nameOf(nameSame, nameLike), bySize.isChecked(), byContent.isChecked()));
            }
        };
        android.widget.CompoundButton.OnCheckedChangeListener re = new android.widget.CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(android.widget.CompoundButton c, boolean on) {
                if (on && c == nameSame) nameLike.setChecked(false);              // זהה או דומה — לא שניהם
                if (on && c == nameLike) nameSame.setChecked(false);
                explainIt.run();
            }
        };
        nameSame.setOnCheckedChangeListener(re);
        nameLike.setOnCheckedChangeListener(re);
        bySize.setOnCheckedChangeListener(re);
        byContent.setOnCheckedChangeListener(re);
        explainIt.run();
        final Runnable shape = new Runnable() {
            @Override
            public void run() {
                boolean bw = how.getSelectedItemPosition() == 1;
                labelA.setText(bw ? Words.DupT.A_WHAT : Words.DupT.WHERE_SEARCH);
                labelB.setVisibility(bw ? android.view.View.VISIBLE : android.view.View.GONE);
                srcB.view.setVisibility(bw ? android.view.View.VISIBLE : android.view.View.GONE);
            }
        };
        how.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> p, android.view.View v, int i, long l) { shape.run(); }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) { }
        });
        shape.run();
        Ui.askForm(a, Words.DupT.DUPS, b, new Runnable() {
            @Override
            public void run() {
                boolean bw = how.getSelectedItemPosition() == 1;
                final DupMode dm = new DupMode(nameOf(nameSame, nameLike), bySize.isChecked(), byContent.isChecked());
                if (!dm.valid()) {                                            // 195
                    Ui.toast(a, Words.DupT.explain(dm.name, dm.size, dm.content));
                    Ui.keepFormOpen();
                    return;
                }
                if (bw && srcB.isEmpty()) {
                    Ui.toast(a, Words.DupT.NEED_B);
                    Ui.keepFormOpen();                                        // 98
                    return;
                }
                done.ok(nm.getText().toString().trim(),
                        Target.of(KIND, "אופן", bw ? "בין" : "בתוך",  // תחביר
                                "היקף", join(Sources.toCond(srcA.get()), restA),  // תחביר
                                "מול", bw ? join(Sources.toCond(srcB.get()), restB) : "",  // תחביר
                                "השוואה", dm.text()));  // תחביר
            }
        });
    }

    private static int nameOf(android.widget.CheckBox same, android.widget.CheckBox like) {
        return same.isChecked() ? DupMode.NAME_SAME : like.isChecked() ? DupMode.NAME_SIMILAR : DupMode.NAME_ANY;
    }

    private static String join(String a, String b) {
        return (a + " " + b).trim();
    }
}
