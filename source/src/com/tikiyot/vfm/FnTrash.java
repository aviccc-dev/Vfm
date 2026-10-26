package com.tikiyot.vfm;

// כלי עבודה: סל המחזור — תוכן ".נמחקו" מכל הכוננים, שחזור וריקון.

import android.app.Activity;
import android.content.Intent;
import android.widget.EditText;
import android.widget.LinearLayout;

public final class FnTrash extends Func {

    public FnTrash() {
        super("סל", Words.BinT.TRASH, "🗑️");  // תחביר
    }

    @Override
    public void open(Activity a, Target t) {
        Intent i = new Intent(a, TrashScreen.class);
        i.putExtra(TrashScreen.EXTRA_FUNC, t.id);
        a.startActivity(i);
    }

    @Override
    public void configure(Activity a, String name, Target t, final Done done) {
        LinearLayout b = Ui.form(a);
        final EditText nm = Ui.field(a, name != null ? name : "", Words.NAME_OPTIONAL);
        b.addView(Ui.label(a, Words.BinT.NAME));
        b.addView(nm);
        Ui.askForm(a, Words.BinT.TRASH, b, new Runnable() {
            @Override
            public void run() { done.ok(nm.getText().toString().trim(), Target.of("סל")); }  // תחביר
        });
    }
}
