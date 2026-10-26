package com.tikiyot.vfm;

// פונקציה: תיקייה חכמה — תנאי שמוצג כתיקייה.

import android.app.Activity;
import android.content.Intent;

public final class FnSmart extends Func {

    /** הסוג בקובץ הראשי. */
    static final String KIND = "חכמה";  // תחביר

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
        i.putExtra(SearchScreen.EXTRA_FUNC, t.id);
        i.putExtra(SearchScreen.EXTRA_VIEW, cs[2]);                           // 148
        i.putExtra(SearchScreen.EXTRA_HIDDEN, t.is(SmartLists.HIDDEN));
        a.startActivity(i);
    }

    @Override
    public void configure(Activity a, String name, Target t, final Done done) {
        SmartForm.show(a, name, t, done);
    }
}
