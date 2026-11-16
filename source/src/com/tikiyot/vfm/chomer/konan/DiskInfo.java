package com.tikiyot.vfm;

// מבנה 180 (סידור קומת החומר): מה הכונן אומר — סוג מערכת הקבצים, הקובץ הגדול ביותר שאפשר לכתוב, המקום הפנוי, וכמה יש בתיקייה. עד כאן ב־Physical.
// אחראי: לשאול את הכונן. אסור לו: לשנות בו דבר.

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class DiskInfo {

    private DiskInfo() { }

    static long[] count(File dir) {
        long[] s = {0, 0};
        File[] all = dir.listFiles();
        if (all == null) return s;
        for (File f : all) {
            if (f.isDirectory()) {
                long[] c = count(f);
                s[0] += c[0];
                s[1] += c[1];
            } else {
                s[0]++;
                s[1] += f.length();
            }
        }
        return s;
    }

    /** לבדיקות: גודל קובץ מרבי ביעד במקום מה שנקרא ממערכת הקבצים (0 — בלי). */
    static long maxFileForTests = 0;
    static final long FAT_MAX = 0xFFFFFFFFL;   // FAT32: עד 4GB פחות בית

    /** 1.18.0: הקובץ הגדול ביותר שאפשר לכתוב בתיקייה — FAT32: 4GB פחות בית; אחרת (או לא ידוע) — בלי הגבלה. */
    static long maxFileSize(File folder) {
        if (maxFileForTests > 0) return maxFileForTests;
        String t = fsType(folder.getAbsolutePath());
        return t != null && (t.equals("vfat") || t.equals("msdos") || t.equals("fat")) ? FAT_MAX : Long.MAX_VALUE;
    }

    /** סוג מערכת הקבצים לפי /proc/mounts: נקודת העגינה הארוכה ביותר שמכילה את המסלול. בכרטיס (fuse) — לפי /mnt/media_rw. */
    static String fsType(String path) {
        List<String[]> m = new ArrayList<String[]>();
        java.io.BufferedReader in = null;
        try {
            in = new java.io.BufferedReader(new java.io.FileReader("/proc/mounts"));
            String line;
            while ((line = in.readLine()) != null) {
                String[] f = line.split(" ");
                if (f.length >= 3) m.add(new String[]{f[1].replace("\\040", " "), f[2]});
            }
        } catch (Throwable t) {
            return null;
        } finally {
            try { if (in != null) in.close(); } catch (Throwable ignored) { }
        }
        return fsTypeIn(m, path);
    }

    /** מתוך רשימת עגינות {נקודה, סוג}. */
    static String fsTypeIn(List<String[]> m, String path) {
        String t = typeAt(m, path);
        if (t != null && (t.equals("fuse") || t.equals("sdcardfs")) && path.startsWith("/storage/")) {
            List<String[]> rw = new ArrayList<String[]>();
            for (String[] x : m) if (x[0].startsWith("/mnt/media_rw/")) rw.add(x);
            String under = typeAt(rw, "/mnt/media_rw/" + path.substring("/storage/".length()));
            if (under != null) return under;
        }
        return t;
    }

    static String typeAt(List<String[]> mounts, String path) {
        String best = null, type = null;
        for (String[] x : mounts) {
            String mp = x[0];
            boolean in = path.equals(mp) || path.startsWith(mp.endsWith("/") ? mp : mp + "/");
            if (in && (best == null || mp.length() > best.length())) {
                best = mp;
                type = x[1];
            }
        }
        return type;
    }

    static long free(File folder) {
        File f = folder;
        while (f != null && !f.exists()) f = f.getParentFile();
        return f == null ? Long.MAX_VALUE : f.getUsableSpace();
    }
}
