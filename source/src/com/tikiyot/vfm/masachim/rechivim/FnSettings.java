package com.tikiyot.vfm;

// פונקציה: הגדרות התוכנה.

import android.app.Activity;
import android.content.Intent;
import android.widget.EditText;
import android.widget.LinearLayout;

public final class FnSettings extends Func {

    public FnSettings() {
        super("הגדרות", "הגדרות", "⚙️");  // תחביר
    }

    @Override
    public void open(Activity a, Target t) {
        a.startActivity(new Intent(a, SettingsScreen.class));
    }

    @Override
    public void configure(Activity a, String name, Target t, final Done done) {
        LinearLayout b = Ui.form(a);
        final EditText nm = Ui.field(a, name != null ? name : "", Words.NAME_OPTIONAL);
        b.addView(Ui.label(a, Words.SetT.NAME));
        b.addView(nm);
        Ui.askForm(a, Words.SetT.SETTINGS, b, new Runnable() {
            @Override
            public void run() { done.ok(nm.getText().toString().trim(), Target.of("הגדרות")); }  // תחביר
        });
    }
}
