package com.tikiyot.vfm;

// כלי: רשימה חכמה — תנאי שנפתח במסך החיפוש (מבנה 293: והוא גם המקום לערוך אותו — אין טופס נפרד).

import android.app.Activity;
import android.content.Intent;

public final class FnSmart extends Func {

    /** הסוג בקובץ הראשי. */
    static final String KIND = Target.KIND_SMART;

    public FnSmart() {
        super(KIND, Words.SMART_LIST, "✨");
    }

    @Override
    public void open(Activity a, Target t) {
        if (SmartLists.broken(t)) Ui.toast(a, Words.Named.GONE);              // 147
        String[] cs = SmartLists.resolve(t);                                   // 147: מקושר — מהרשימה עם השם
        Intent i = new Intent(a, SearchScreen.class);
        i.putExtra(SearchScreen.EXTRA_QUERY, cs[0]);
        i.putExtra(SearchScreen.EXTRA_FIXED, true);
        i.putExtra(SearchScreen.EXTRA_TITLE, t.get("שם", Words.SMART_LIST));   // קבצים ישנים, בלי מזהה // תחביר
        i.putExtra(SearchScreen.EXTRA_SORT, cs[1].isEmpty() ? "שם" : cs[1]);  // תחביר
        i.putExtra(SearchScreen.EXTRA_VIEW, cs[2]);                           // 148
        i.putExtra(SearchScreen.EXTRA_HIDDEN, t.is(SmartLists.HIDDEN));
        a.startActivity(i);
    }

    /**
     * מבנה 293 (אפיון החיפוש, 6 — "מקום אחד לעריכה"): אין עוד טופס נפרד — התנאים נערכים בחלונית של מסך החיפוש. מי שמבקש להגדיר
     * כלי חכם — המסך נפתח עם התנאי שלו (כמו open), ושם עורכים ושומרים.
     */
    @Override
    public void configure(Activity a, String name, Target t, final Done done) {
        if (t != null) open(a, t);
        else a.startActivity(new Intent(a, SearchScreen.class));
    }
}
