package com.tikiyot.vfm;

// פונקציה: פס חיפוש צף.

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;

public final class FnBar extends Func {

    public FnBar() {
        super("פס", Words.BarT.BAR_NAME, "📌");  // תחביר
    }

    @Override
    public void open(Activity a, Target t) {
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(a)) {
            Ui.confirm(a, Words.BarT.NO_PERMISSION, Words.BarT.NEEDS_OVERLAY, Words.BarT.OPEN_SETTING,
                    new BarPermission(a));
            return;
        }
        Intent i = new Intent(a, BarService.class);
        i.putExtra(BarService.EXTRA_PATHS, t.is(SmartLists.PATHS));
        i.putExtra(BarService.EXTRA_LOOSE, !"לא".equals(t.get(SmartLists.LOOSE, "כן")));  // תחביר
        if (Build.VERSION.SDK_INT >= 26) a.startForegroundService(i); else a.startService(i);
        Ui.toast(a, Words.BarT.BAR_ON);
    }

    private static final class BarPermission implements Runnable {
        private final Activity a;

        BarPermission(Activity a) {
            this.a = a;
        }

        @Override
        public void run() {
            try {
                a.startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        android.net.Uri.parse("package:" + a.getPackageName())));
            } catch (Throwable t) {
                Ui.toast(a, Words.BarT.CANT_OPEN_PERMISSION);
            }
        }
    }

    @Override
    public void configure(Activity a, String name, Target t, final Done done) {
        LinearLayout b = Ui.form(a);
        final EditText nm = Ui.field(a, name != null ? name : "", Words.NAME_OPTIONAL);
        final CheckBox paths = Ui.check(a, Words.BarT.IN_PATH_TOO, t != null && t.is(SmartLists.PATHS));
        final CheckBox loose = Ui.check(a, Words.BarT.LOOSE,
                t == null || !"לא".equals(t.get(SmartLists.LOOSE, "כן")));  // תחביר
        b.addView(Ui.label(a, Words.BarT.NAME));
        b.addView(nm);
        b.addView(paths);
        b.addView(loose);
        Ui.askForm(a, Words.BarT.BAR_NAME, b, new Runnable() {
            @Override
            public void run() {
                done.ok(nm.getText().toString().trim(),
                        Target.of("פס", SmartLists.PATHS, paths.isChecked() ? "כן" : "",  // תחביר
                                SmartLists.LOOSE, loose.isChecked() ? "" : "לא"));  // תחביר
            }
        });
    }
}
