package com.tikiyot.vfm;

// אריזה ופריסה של ZIP (116): אריזת תיקייה לשליחה, כך שהמבנה נשמר; ופריסה של ZIP לתיקייה חדשה.
// בלי ממשק, ולכן נבדק במחשב. הממשק והאישורים — Acts (שליחה) ו-Physical (פריסה, שהיא שינוי בכונן).
// אסור לו: לכתוב מחוץ לתיקיית היעד שקיבל (הגנה מפני "../" בשמות שבתוך הקובץ), ולהחליף קובץ קיים.

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

final class Zip {

    private Zip() { }

    /** מה שקרה באריזה או בפריסה. */
    static final class Result {
        int files, dirs, skipped;
        long bytes;
        boolean stopped;
        String error;
    }

    /** כמה קבצים ובתים יש בתיקייה (לאריזה), בלי סל המחזור. */
    static long[] measure(File dir) {
        long[] r = {0, 0};
        File[] k = dir.listFiles();
        if (k == null) return r;
        for (File f : k) {
            if (f.getName().equals(Index.TRASH)) continue;
            if (f.isDirectory()) {
                long[] s = measure(f);
                r[0] += s[0];
                r[1] += s[1];
            } else {
                r[0]++;
                r[1] += f.length();
            }
        }
        return r;
    }

    /**
     * אורז תיקייה לקובץ ZIP. בתוך הקובץ — התיקייה עצמה בשמה, ובתוכה הכול כמו שהוא (גם תיקיות ריקות
     * וקבצים מוסתרים), חוץ מסל המחזור. שמות בעברית נשמרים (UTF-8). דחיסה מהירה: ספרים ממילא דחוסים.
     */
    static Result pack(File dir, File zip, Physical.Progress p) {
        Result r = new Result();
        long[] total = measure(dir);
        File tmp = new File(zip.getParentFile(), zip.getName() + ".tmp");
        ZipOutputStream out = null;
        try {
            zip.getParentFile().mkdirs();
            out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(tmp), 1 << 16));
            out.setLevel(Deflater.BEST_SPEED);
            add(out, dir, dir.getName(), r, (int) total[0], total[1], p, zip.getParentFile().getAbsolutePath());
            out.close();
            out = null;
            if (r.stopped) {
                tmp.delete();
                return r;
            }
            zip.delete();
            if (!tmp.renameTo(zip)) throw new java.io.IOException(Words.Send.CANT_SAVE);
        } catch (Throwable t) {
            Log.e("אריזת ZIP", t);
            r.error = Words.Send.PACK_FAILED;
            tmp.delete();
        } finally {
            if (out != null) try { out.close(); } catch (Throwable ignored) { }
        }
        return r;
    }

    /** skip: התיקייה שבה נכתב קובץ הפלט — לא נארזת, גם אם היא בתוך מה שנארז (119). */
    private static void add(ZipOutputStream out, File f, String name, Result r, int total, long totalBytes,
                            Physical.Progress p, String skip) throws Exception {
        if (p != null && p.stopped()) {
            r.stopped = true;
            return;
        }
        if (f.getAbsolutePath().equals(skip)) return;
        if (f.isDirectory()) {
            File[] k = f.listFiles();
            if (k == null) k = new File[0];
            Arrays.sort(k);
            out.putNextEntry(new ZipEntry(name + "/"));        // גם תיקייה ריקה נשמרת
            out.closeEntry();
            r.dirs++;
            for (File c : k) {
                if (c.getName().equals(Index.TRASH)) continue;
                add(out, c, name + "/" + c.getName(), r, total, totalBytes, p, skip);
                if (r.stopped) return;
            }
            return;
        }
        ZipEntry e = new ZipEntry(name);
        e.setTime(f.lastModified());
        out.putNextEntry(e);
        InputStream in = new BufferedInputStream(new FileInputStream(f), 1 << 16);
        try {
            byte[] buf = new byte[1 << 16];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        } finally {
            in.close();
        }
        out.closeEntry();
        r.files++;
        r.bytes += f.length();
        if (p != null) p.step(r.files, total, r.bytes, totalBytes);
    }

    /** מה יש בקובץ ZIP, לחלון האישור: {קבצים, בתים לאחר פריסה}. null — הקובץ פגום. */
    static long[] contents(File zip) {
        ZipFile z = null;
        try {
            z = new ZipFile(zip);
            long files = 0, bytes = 0;
            for (Enumeration<? extends ZipEntry> en = z.entries(); en.hasMoreElements(); ) {
                ZipEntry e = en.nextElement();
                if (e.isDirectory()) continue;
                files++;
                if (e.getSize() > 0) bytes += e.getSize();
            }
            return new long[]{files, bytes};
        } catch (Throwable t) {
            return null;
        } finally {
            if (z != null) try { z.close(); } catch (Throwable ignored) { }
        }
    }

    /** לאן לפרוס: תיקייה חדשה ליד הקובץ, בשם הקובץ בלי ".zip"; שם תפוס — "(2)". */
    static File target(File zip) {
        String n = zip.getName();
        if (n.toLowerCase(java.util.Locale.ROOT).endsWith(".zip")) n = n.substring(0, n.length() - 4);
        if (n.trim().isEmpty()) n = Words.Send.UNPACK_DIR;
        return new File(zip.getParentFile(), Physical.freeName(zip.getParentFile(), n, true));
    }

    /**
     * פורס את הקובץ אל dest (שנוצרת). אם כל התוכן בתוך תיקייה אחת בשם היעד — היא לא מוכפלת
     * (קובץ שארזנו מכיל "ספרים/…", והיעד כבר "ספרים"). שם שמנסה לצאת מהיעד ("../") או מסלול מוחלט —
     * מדולג. לא מחליף קובץ קיים.
     */
    static Result unpack(File zip, File dest, Physical.Progress p) {
        Result r = new Result();
        ZipFile z = null;
        try {
            z = new ZipFile(zip);
            List<ZipEntry> all = new ArrayList<ZipEntry>();
            long totalBytes = 0;
            int total = 0;
            for (Enumeration<? extends ZipEntry> en = z.entries(); en.hasMoreElements(); ) {
                ZipEntry e = en.nextElement();
                all.add(e);
                if (!e.isDirectory()) {
                    total++;
                    if (e.getSize() > 0) totalBytes += e.getSize();
                }
            }
            String strip = commonTop(all, dest.getName());
            if (!dest.mkdirs() && !dest.isDirectory()) throw new java.io.IOException(Words.CANT_CREATE_FOLDER);
            String base = dest.getCanonicalPath() + File.separator;
            byte[] buf = new byte[1 << 16];
            for (ZipEntry e : all) {
                if (p != null && p.stopped()) {
                    r.stopped = true;
                    break;
                }
                String name = e.getName().replace('\\', '/');
                if (strip != null) name = name.length() > strip.length() ? name.substring(strip.length()) : "";
                if (name.isEmpty()) continue;
                if (name.startsWith("/") || name.contains("../") || name.equals("..") || name.endsWith("/..")) {
                    r.skipped++;
                    continue;
                }
                File out = new File(dest, name);
                if (!(out.getCanonicalPath() + (e.isDirectory() ? File.separator : "")).startsWith(base)
                        && !out.getCanonicalPath().equals(dest.getCanonicalPath())) {
                    r.skipped++;
                    continue;
                }
                if (e.isDirectory()) {
                    if (out.mkdirs()) r.dirs++;
                    continue;
                }
                if (out.exists()) {
                    r.skipped++;
                    continue;
                }
                out.getParentFile().mkdirs();
                InputStream in = z.getInputStream(e);
                OutputStream o = new BufferedOutputStream(new FileOutputStream(out), 1 << 16);
                try {
                    int n;
                    while ((n = in.read(buf)) > 0) o.write(buf, 0, n);
                } finally {
                    o.close();
                    in.close();
                }
                if (e.getTime() > 0) out.setLastModified(e.getTime());
                r.files++;
                r.bytes += out.length();
                if (p != null) p.step(r.files, total, r.bytes, totalBytes);
            }
        } catch (Throwable t) {
            Log.e("פריסת ZIP", t);
            r.error = Words.Send.UNPACK_FAILED;
        } finally {
            if (z != null) try { z.close(); } catch (Throwable ignored) { }
        }
        return r;
    }

    /** אם כל הרשומות בתוך תיקייה עליונה אחת ששמה כשם היעד — הקידומת שלה, להשמטה; אחרת null. */
    private static String commonTop(List<ZipEntry> all, String destName) {
        String top = null;
        for (ZipEntry e : all) {
            String n = e.getName().replace('\\', '/');
            int i = n.indexOf('/');
            if (i <= 0) return null;                              // קובץ בשורש הקובץ
            String t = n.substring(0, i);
            if (top == null) top = t;
            else if (!top.equals(t)) return null;
        }
        if (top == null) return null;
        String base = destName.replaceAll(" \\(\\d+\\)$", "");     // "ספרים (2)" — אותה תיקייה
        return top.equals(base) || top.equals(destName) ? top + "/" : null;
    }
}
