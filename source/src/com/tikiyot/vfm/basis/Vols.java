package com.tikiyot.vfm;

// כוננים. אחראי: לאתר את הזיכרון הפנימי וכרטיסי הזיכרון.

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class Vols {

    public static final class Vol {
        public final File root;
        public final String label, key;
        public final boolean internal;

        Vol(File root, String label, String key, boolean internal) {
            this.root = root;
            this.label = label;
            this.key = key;
            this.internal = internal;
        }
    }

    private Vols() { }

    private static String canon(File f) {
        try {
            return f.getCanonicalPath();
        } catch (Exception e) {
            return f.getAbsolutePath();
        }
    }

    public static List<Vol> list(Context ctx) {
        List<Vol> out = new ArrayList<Vol>();
        Set<String> seen = new HashSet<String>();
        File in = Environment.getExternalStorageDirectory();
        out.add(new Vol(in, Drive.INTERNAL_NAME, Drive.INTERNAL, true));
        seen.add(canon(in));

        List<File> cards = new ArrayList<File>();
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                StorageManager sm = (StorageManager) ctx.getSystemService(Context.STORAGE_SERVICE);
                if (sm != null) for (StorageVolume v : sm.getStorageVolumes()) {
                    if (v.isPrimary() || v.getDirectory() == null) continue;
                    String st = v.getState();
                    if (Environment.MEDIA_MOUNTED.equals(st) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(st))
                        cards.add(v.getDirectory());
                }
            } catch (Throwable t) {
                Log.e("איתור כרטיסים", t);
            }
        }
        try {
            File[] dirs = ctx.getExternalFilesDirs(null);
            if (dirs != null) for (File d : dirs) {
                if (d == null) continue;
                String p = d.getAbsolutePath();
                int i = p.indexOf("/Android/data/");
                if (i > 0) cards.add(new File(p.substring(0, i)));
            }
        } catch (Throwable ignored) {
        }
        File[] st = new File("/storage").listFiles();
        if (st != null) for (File f : st) {
            String n = f.getName();
            if (n.equals("emulated") || n.equals("self") || n.startsWith(".")) continue;
            if (f.isDirectory() && f.canRead()) cards.add(f);
        }

        int n = 0;
        for (File c : cards) {
            if (!c.isDirectory() || !seen.add(canon(c))) continue;
            n++;
            out.add(new Vol(c, Drive.name(false, n), c.getName(), false));
        }
        return out;
    }

    /** האם לדלג על הקובץ או התיקייה בסריקה. */
    /** 1.13.0: שורש הזיכרון הפנימי (למשתני הפקודות). */
    static String internalRoot(Context c) {
        for (Vol v : list(c)) if (v.internal) return v.root.getAbsolutePath();
        return "/storage/emulated/0";
    }

    /** 1.13.0: שורש הכרטיס, או null. */
    static String cardRoot(Context c) {
        for (Vol v : list(c)) if (!v.internal) return v.root.getAbsolutePath();
        return null;
    }

    public static boolean skip(File f, boolean hidden) {
        String n = f.getName();
        if (!hidden && n.startsWith(".")) return true;
        String p = f.getAbsolutePath();
        return p.endsWith("/Android/data") || p.endsWith("/Android/obb");
    }
}
