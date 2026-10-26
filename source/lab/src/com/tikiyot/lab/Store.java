package com.tikiyot.lab;

// 1.4 — המאגר של אנדרואיד (MediaStore): כל הקבצים שהמערכת מכירה, בשאילתה אחת לכל כונן; ו"מה השתנה מאז" —
// לפי מונה השינויים (אנדרואיד 11 ומעלה). רק קריאה.

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class Store {

    private Store() { }

    /** שם הכונן במאגר ← השורש שלו בכונן ("external_primary" ← /storage/emulated/0). */
    static Map<String, String> volumes(Context c) {
        Map<String, String> out = new HashMap<String, String>();
        if (Build.VERSION.SDK_INT < 29) {
            out.put("external", Environment.getExternalStorageDirectory().getAbsolutePath());
            return out;
        }
        Set<String> names = MediaStore.getExternalVolumeNames(c);
        for (String n : names) {
            if (n.equals(MediaStore.VOLUME_EXTERNAL_PRIMARY))
                out.put(n, Environment.getExternalStorageDirectory().getAbsolutePath());
            else out.put(n, "/storage/" + n.toUpperCase(java.util.Locale.ROOT));
        }
        return out;
    }

    /** תוצאה: מסלולי הקבצים (בלי תיקיות), והזמן. */
    static final class Rows {
        final List<String> paths = new ArrayList<String>();
        long ms;
        int dirs;
        String error;
    }

    @SuppressWarnings("deprecation")
    static Rows all(Context c, String volume, String root) {
        Rows r = new Rows();
        long t0 = System.nanoTime();
        Uri u = Build.VERSION.SDK_INT >= 29 ? MediaStore.Files.getContentUri(volume) : MediaStore.Files.getContentUri("external");
        String[] proj = Build.VERSION.SDK_INT >= 29
                ? new String[]{MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.RELATIVE_PATH, MediaStore.MediaColumns.DISPLAY_NAME, "format"}
                : new String[]{MediaStore.MediaColumns.DATA, "format"};
        Cursor cur = null;
        try {
            cur = c.getContentResolver().query(u, proj, null, null, null);
            if (cur == null) {
                r.error = "המאגר לא ענה";
                return r;
            }
            int iFormat = cur.getColumnIndex("format");
            while (cur.moveToNext()) {
                if (iFormat >= 0 && cur.getInt(iFormat) == 0x3001) {           // שורה של תיקייה
                    r.dirs++;
                    continue;
                }
                String p = cur.getString(0);
                if (p == null && Build.VERSION.SDK_INT >= 29) {
                    String rel = cur.getString(1), name = cur.getString(2);
                    if (rel != null && name != null) p = root + "/" + rel + name;
                }
                if (p != null) r.paths.add(p);
            }
        } catch (Throwable t) {
            r.error = t.toString();
        } finally {
            if (cur != null) cur.close();
            r.ms = (System.nanoTime() - t0) / 1000000;
        }
        return r;
    }

    /** מונה השינויים של הכונן (אנדרואיד 11+); -1 — לא נתמך. */
    static long generation(Context c, String volume) {
        if (Build.VERSION.SDK_INT < 30) return -1;
        try {
            return MediaStore.getGeneration(c, volume);
        } catch (Throwable t) {
            return -1;
        }
    }

    /** הקבצים שהשתנו (נוספו או נערכו) מאז המונה since — שמות; וכמה זמן. */
    @SuppressWarnings("deprecation")
    static Rows changedSince(Context c, String volume, long since) {
        Rows r = new Rows();
        long t0 = System.nanoTime();
        Cursor cur = null;
        try {
            cur = c.getContentResolver().query(MediaStore.Files.getContentUri(volume),
                    new String[]{MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.DISPLAY_NAME},
                    MediaStore.MediaColumns.GENERATION_MODIFIED + ">?", new String[]{"" + since}, null);
            if (cur != null) while (cur.moveToNext()) {
                String p = cur.getString(0);
                r.paths.add(p != null ? p : cur.getString(1));
            }
        } catch (Throwable t) {
            r.error = t.toString();
        } finally {
            if (cur != null) cur.close();
            r.ms = (System.nanoTime() - t0) / 1000000;
        }
        return r;
    }
}
