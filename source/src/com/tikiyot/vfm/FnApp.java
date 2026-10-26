package com.tikiyot.vfm;

// פונקציה: הפניה לאפליקציה אחרת.

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.ArrayList;
import java.util.List;

public final class FnApp extends Func {

    public FnApp() {
        super("אפליקציה", Words.AppT.APP_REF, "🔗");     // 📱 שמורה לזיכרון הפנימי // תחביר
    }

    @Override
    public void open(Activity a, Target t) {
        String pkg = t.get("חבילה", "");  // תחביר
        Intent i = pkg.isEmpty() ? null : a.getPackageManager().getLaunchIntentForPackage(pkg);
        if (i == null) Ui.toast(a, Words.AppT.APP_NOT_FOUND); else a.startActivity(i);
    }

    @Override
    public void configure(final Activity a, String name, Target t, final Done done) {
        final List<String> labels = new ArrayList<String>();
        final List<String> packages = new ArrayList<String>();
        try {
            PackageManager pm = a.getPackageManager();
            Intent main = new Intent(Intent.ACTION_MAIN, null);
            main.addCategory(Intent.CATEGORY_LAUNCHER);
            for (ResolveInfo ri : pm.queryIntentActivities(main, 0)) {
                if (ri.activityInfo == null) continue;
                labels.add(String.valueOf(ri.loadLabel(pm)));
                packages.add(ri.activityInfo.packageName);
            }
        } catch (Throwable th) {
            Log.e("רשימת אפליקציות", th);
        }
        if (labels.isEmpty()) {
            Ui.toast(a, Words.AppT.NO_APPS);
            return;
        }
        Ui.list(a, Words.AppT.PICK_APP, labels, new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(android.content.DialogInterface d, int which) {
                done.ok(labels.get(which), Target.of("אפליקציה", "חבילה", packages.get(which)));  // תחביר
            }
        });
    }
}
