package com.tikiyot.vfm;

// פעולות על קובץ מתוך רשימה. אחראי: תפריט הפעולות ומסך האזהרה.

import android.app.Activity;
import android.content.Intent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class Acts {

    public interface Changed {
        void changed();
    }

    private Acts() { }

    /** הוספת קבצים לרשימה, עם בורר יעד. */
    public static void addToList(final Activity a, final List<String> paths) {
        ListPicker.choose(a, Words.ADD_TO_LIST + " · " + Words.NO_TOUCH, null, new ListPicker.Picked() {
            @Override
            public void ok(java.util.ArrayList<String> path) {
                int n = Tree.addFiles(path, paths);
                Library.get(a).learn(paths);
                Ui.toast(a, n < 0 ? Words.LIST_NOT_FOUND
                        : n == 0 ? Words.ActsW.ALREADY_IN_LIST + Tree.label(path) + "\""
                        : Words.ActsW.ADDED_N + n + Words.ActsW.TO_LIST + Tree.label(path) + "\"");
            }
        });
    }

    /** בקשת השיתוף עצמה — במקום אחד (80.7): גם השיתוף מהפס הצף משתמש בה. */
    public static Intent shareIntent(File f) {
        Intent i = new Intent(Intent.ACTION_SEND);
        String m = Ui.mime(f.getName());
        i.setType(m != null ? m : "*/*");
        i.putExtra(Intent.EXTRA_STREAM, Provider.uriFor(f));
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return i;
    }


    /** לשליחה: התיקייה שבה נארזות תיקיות לפני שליחה (בתוך תיקיות המערכת של אופן העבודה). */
    static File sendDir() {
        return new File(Paths.system(), Words.ActsW.SEND);
    }

    /**
     * שליחה של כמה קבצים ותיקיות (116) — דרך חלון השיתוף של המערכת (Quick Share, Bluetooth, שיאומי…).
     * קבצים נשלחים כמו שהם. תיקייה (1.10.0): בבחירה — הקבצים שבה כמו שהם (גם מתיקיות המשנה), או ZIP בשמה.
     */
    public static void shareMany(final Activity a, List<String> files, final List<String> dirs) {
        shareMany(a, files, dirs, false);
    }

    /** toDevice: "שליחה למכשיר" (117) — רק תוכנות העברה; אחרת "שיתוף" — כל התוכנות. */
    public static void shareMany(final Activity a, List<String> files, final List<String> dirs, final boolean toDevice) {
        final List<File> ready = new ArrayList<File>();
        int missing = 0;
        for (String p : files) {
            File f = new File(p);
            if (f.isFile()) ready.add(f); else missing++;
        }
        final int miss = missing;
        if (dirs.isEmpty()) {
            if (ready.isEmpty()) {
                Ui.toast(a, Words.ActsW.NOT_AT_PATH);
                return;
            }
            send(a, ready, miss, toDevice);
            return;
        }
        // 1.10.0: תיקייה — הקבצים כמו שהם (גם מתיקיות המשנה), או ארוזה. המשתמש: "שישלח קבצים כפי שהם".
        final List<File> inside = new ArrayList<File>(ready);
        for (String d : dirs) inside.addAll(filesIn(new File(d)));
        if (inside.size() > MAX_SEND) {                                       // חלון השיתוף לא עומד בכמויות גדולות
            Ui.toast(a, Words.ActsW.tooMany(inside.size(), MAX_SEND));
            pack(a, ready, dirs, miss, toDevice);
            return;
        }
        Ui.options(a, Words.ActsW.FOLDER_HOW, new String[]{Words.ActsW.asFiles(inside.size()), Words.ActsW.AS_ZIP},
                new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int w) {
                        if (w == 0) {
                            if (inside.isEmpty()) Ui.toast(a, Words.ActsW.NOT_AT_PATH);
                            else send(a, inside, miss, toDevice);
                        } else pack(a, ready, dirs, miss, toDevice);
                    }
                });
    }

    /** 1.10.0: כמה קבצים לכל היותר בשליחה אחת כקבצים; מעבר לזה — ארוזה. */
    static final int MAX_SEND = 300;

    /** 1.10.0: כל הקבצים שבתיקייה, גם בתיקיות המשנה — בלי מוסתרים (וסל המחזור) ובלי Android/data. */
    static List<File> filesIn(File dir) {
        List<File> out = new ArrayList<File>();
        java.util.ArrayDeque<File> q = new java.util.ArrayDeque<File>();
        q.add(dir);
        while (!q.isEmpty()) {
            File[] all = q.poll().listFiles();
            if (all == null) continue;
            java.util.Arrays.sort(all);
            for (File f : all) {
                if (Vols.skip(f, false)) continue;
                if (f.isDirectory()) q.add(f); else out.add(f);
            }
        }
        return out;
    }

    /** 203: מקובץ פקודות — מה שהוכן (עותקים ואריזות): שליחה אחת למכשיר. */
    static void sendReady(Activity a, List<File> ready) {
        if (!ready.isEmpty()) send(a, ready, 0, true);
    }

    /** אריזה ל־ZIP (116), ואז שליחה. */
    private static void pack(final Activity a, final List<File> ready, final List<String> dirs, final int miss, final boolean toDevice) {
        long need = 0;
        for (String d : dirs) need += Zip.measure(new File(d))[1];
        long free = new File(Paths.system().getAbsolutePath()).getUsableSpace();
        if (free < need) {
            Ui.toast(a, Words.ActsW.NO_ROOM_PACK + Files.size(need) + Words.ActsW.FREE + Files.size(free));
            return;
        }
        final Physical.Busy busy = new Physical.Busy(a, Words.ActsW.PACKING);
        new Thread(new Runnable() {
            @Override
            public void run() {
                clean();
                String error = null;
                boolean stopped = false;
                for (String d : dirs) {
                    File dir = new File(d);
                    File zip = new File(sendDir(), dir.getName() + ".zip");
                    Zip.Result r = Zip.pack(dir, zip, busy);
                    if (r.stopped) {
                        stopped = true;
                        break;
                    }
                    if (r.error != null) {
                        error = r.error + ": " + dir.getName();
                        break;
                    }
                    ready.add(zip);
                }
                busy.close();
                final String err = error;
                final boolean stop = stopped;
                a.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (err != null) Ui.toast(a, err);
                        else if (stop) Ui.toast(a, Words.ActsW.SEND_CANCELLED);
                        else send(a, ready, miss, toDevice);
                    }
                });
            }
        }).start();
    }

    /** מוחק אריזות ישנות (מעל יום) — שהתיקייה לא תגדל. אריזה חדשה נשארת עד שהשליחה נגמרת. */
    private static void clean() {
        File[] old = sendDir().listFiles();
        if (old != null)
            for (File f : old)
                if (System.currentTimeMillis() - f.lastModified() > 24L * 3600 * 1000) f.delete();
    }

    /** בונה את בקשת השליחה (קובץ אחד או כמה), ושולח: למכשיר (117) — דרך חלון תוכנות ההעברה; אחרת — חלון השיתוף המלא. */
    private static void send(Activity a, List<File> ready, int missing, boolean toDevice) {
        Intent i;
        if (ready.size() == 1) {
            i = shareIntent(ready.get(0));
        } else {
            ArrayList<android.net.Uri> uris = new ArrayList<android.net.Uri>();
            String type = null;
            for (File f : ready) {
                uris.add(Provider.uriFor(f));
                String m = Ui.mime(f.getName());
                if (m == null) m = "*/*";
                if (type == null) type = m;
                else if (!type.equals(m)) type = type.split("/")[0].equals(m.split("/")[0]) ? type.split("/")[0] + "/*" : "*/*";
            }
            i = new Intent(Intent.ACTION_SEND_MULTIPLE);
            i.setType(type);
            i.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        if (toDevice) Transfer.show(a, i); else Transfer.chooser(a, i);
        if (missing > 0) Ui.toast(a, missing + Words.ActsW.NOT_SENT);
    }

    /** ההגדרה \"קובץ שנמחק\": להשאיר ברשימות באפור, או להסיר (ברירת המחדל). */
    public static boolean deletedStaysGrey() {
        return Conf.DELETED_GREY.equals(Conf.get(Conf.GENERAL, Conf.DELETED, Conf.DELETED_REMOVE));
    }

}
