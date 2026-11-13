package com.tikiyot.vfm;

// שליחה למכשיר (117): במקום חלון השיתוף המלא — חלון קטן עם תוכנות ההעברה בין מכשירים בלבד
// (Xiaomi Share, שיתוף מהיר, Bluetooth, ShareMe…), ובסופו "כל התוכנות…".
// אילו תוכנות נחשבות תוכנות העברה — כאן, במקום אחד (isTransfer). אסור לו: לשלוח בעצמו, או לשנות קבצים.

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class Transfer {

    private Transfer() { }

    /** תוכנות העברה בין מכשירים — לפי החבילה (ובשירותי Google: רק "שיתוף מהיר", לא שאר מה שבחבילה). */
    static boolean isTransfer(String pkg, String cls) {
        String p = pkg == null ? "" : pkg.toLowerCase(Locale.ROOT);
        String c = cls == null ? "" : cls.toLowerCase(Locale.ROOT);
        if (p.equals("com.google.android.gms")) return c.contains("nearby") || c.contains("sharing");
        return p.equals("com.miui.mishare.connectivity")            // Xiaomi Share
                || p.startsWith("com.xiaomi.mishare")
                || p.equals("com.xiaomi.midrop")                     // ShareMe
                || p.equals("com.android.bluetooth")                 // Bluetooth
                || p.equals("com.google.android.bluetooth")
                || p.equals("com.samsung.android.app.sharelive")     // Quick Share של סמסונג
                || p.startsWith("com.lenovo.anyshare");              // SHAREit
    }

    /** תוכנות ההעברה שמקבלות את הבקשה הזו. */
    static List<ResolveInfo> accepting(Activity a, Intent send) {
        List<ResolveInfo> all, out = new ArrayList<ResolveInfo>();
        try {
            all = a.getPackageManager().queryIntentActivities(send, 0);
        } catch (Throwable t) {
            all = new ArrayList<ResolveInfo>();
        }
        for (ResolveInfo r : all)
            if (r.activityInfo != null && isTransfer(r.activityInfo.packageName, r.activityInfo.name)) out.add(r);
        return out;
    }

    static String key(ResolveInfo r) {
        return r.activityInfo.packageName + "/" + r.activityInfo.name;
    }

    private static void go(Activity a, Intent send, String pkg, String cls) {
        Intent i = new Intent(send);
        i.setClassName(pkg, cls);
        try {
            a.startActivity(i);
        } catch (Throwable t) {
            Log.e("שליחה למכשיר", t);
            Ui.toast(a, Words.Send.SEND_FAILED);
        }
    }

    /**
     * מציג את תוכנות ההעברה שמותקנות, ושולח דרך מה שנבחר. אם אין — חלון השיתוף המלא.
     * 183: יעד קבוע (בהגדרות) — שולחים ישר אליו, בלי החלון. תוכנה שאינה מקבלת קבצים מתוכנות אחרות — לא מוצגת
     * (המשתמש, על ShareMe: "אם לא מקבלת את הרשימה, רק צריך לבחור מתוכה — אל תעשה איתה; היא עם פרסומות").
     */
    static void show(final Activity a, final Intent send) {
        final List<ResolveInfo> acc = accepting(a, send);
        String fixed = Conf.get(Conf.GENERAL, Conf.SEND_TO, "");
        if (!fixed.isEmpty()) {
            for (ResolveInfo r : acc) if (key(r).equals(fixed)) {
                go(a, send, r.activityInfo.packageName, r.activityInfo.name);
                return;
            }
        }
        show2(a, send, acc);
    }

    private static void show2(final Activity a, final Intent send, final List<ResolveInfo> acc) {
        PackageManager pm = a.getPackageManager();
        final List<ResolveInfo> found = acc;
        List<String> labels = new ArrayList<String>();
        for (ResolveInfo r : found) {
            CharSequence l = r.loadLabel(pm);
            labels.add(l == null ? r.activityInfo.packageName : l.toString());
        }
        if (found.isEmpty()) {
            Ui.toast(a, Words.Send.NO_TRANSFER_APP);
            chooser(a, send);
            return;
        }
        labels.add(Words.Send.ALL_APPS);
        Ui.list(a, Words.Send.TO_DEVICE, labels, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int which) {
                if (which < found.size()) go(a, send, found.get(which).activityInfo.packageName, found.get(which).activityInfo.name);
                else chooser(a, send);
            }
        });
    }

    static void chooser(Activity a, Intent send) {
        try {
            a.startActivity(Intent.createChooser(send, Words.Send.SHARE));
        } catch (Throwable t) {
            Ui.toast(a, Words.Send.SHARE_FAILED);
        }
    }

    /** 183: ההגדרה "יעד קבוע" — תוכנות ההעברה שבמכשיר שמקבלות קבצים, ובחירה. */
    static void chooseFixed(final Activity a, final Runnable done) {
        Intent probe = new Intent(Intent.ACTION_SEND_MULTIPLE);
        probe.setType("*/*");
        final List<ResolveInfo> acc = accepting(a, probe);
        final List<String> keys = new ArrayList<String>();
        List<String> labels = new ArrayList<String>();
        keys.add("");
        labels.add(Words.Send.NO_FIXED);
        PackageManager pm = a.getPackageManager();
        for (ResolveInfo r : acc) {
            keys.add(key(r));
            CharSequence l = r.loadLabel(pm);
            labels.add(l == null ? r.activityInfo.packageName : l.toString());
        }
        Ui.list(a, Words.Send.FIXED, labels, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int which) {
                Conf.set(Conf.GENERAL, Conf.SEND_TO, keys.get(which));
                if (done != null) done.run();
            }
        });
    }

    /** 183: התווית של היעד הקבוע (לכפתור בהגדרות). */
    static String fixedLabel(Activity a) {
        String f = Conf.get(Conf.GENERAL, Conf.SEND_TO, "");
        if (f.isEmpty()) return Words.Send.NO_FIXED;
        try {
            String pkg = f.substring(0, f.indexOf('/'));
            return a.getPackageManager().getApplicationLabel(a.getPackageManager().getApplicationInfo(pkg, 0)).toString();
        } catch (Throwable t) {
            return f;
        }
    }
}
