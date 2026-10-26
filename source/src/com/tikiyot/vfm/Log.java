package com.tikiyot.vfm;

// יומן התקלות. אחראי: רישום שגיאות והודעות לקובץ. אסור לו: להקריס, להציג מסכים.

import android.os.Build;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class Log {

    private static boolean installed;
    public static String version = "";

    public static void initVersion(android.content.Context c) {
        try {
            version = c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName;
        } catch (Throwable ignored) {
        }
    }

    private Log() { }

    public static String device() {
        return Words.LogW.VERSION + version + " · " + Build.MANUFACTURER + " " + Build.MODEL + Words.LogW.ANDROID + Build.VERSION.RELEASE
                + " (API " + Build.VERSION.SDK_INT + ")";
    }

    public static synchronized void i(String msg) {
        write(msg);
    }

    public static synchronized void e(String where, Throwable t) {
        StringWriter sw = new StringWriter();
        if (t != null) t.printStackTrace(new PrintWriter(sw));
        write(Words.LogW.ERROR_IN + where + ": " + t + "\n" + sw);
    }

    static final long MAX = 300000, KEEP = 150000;

    /** היומן גדול מדי: נשאר החלק האחרון (מתחילת שורה), והשאר יוצא. */
    static void keepTail(File f) {
        try {
            java.io.RandomAccessFile raf = new java.io.RandomAccessFile(f, "r");
            byte[] b;
            try {
                long from = Math.max(0, raf.length() - KEEP);
                raf.seek(from);
                b = new byte[(int) (raf.length() - from)];
                raf.readFully(b);
            } finally {
                raf.close();
            }
            int start = 0;
            while (start < b.length && b[start] != '\n') start++;
            String tail = new String(b, Math.min(b.length, start + 1), Math.max(0, b.length - start - 1), "UTF-8");
            Files.writeAtomic(f, Words.LogW.TRIMMED + "\n" + tail);
        } catch (Throwable t) {
            f.delete();
        }
    }

    private static void write(String msg) {
        try {
            File f = new File(Paths.system(), "יומן.txt");  // בכונן
            if (f.length() > MAX) keepTail(f);                        // 102: לא למחוק — לשמור את הסוף
            OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream(f, true), "UTF-8");
            w.write(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date())
                    + "  " + msg + "\n");
            w.close();
        } catch (Throwable ignored) {
        }
    }

    public static synchronized String read() {
        try {
            File f = new File(Paths.system(), "יומן.txt");  // בכונן
            if (!f.exists()) return "";
            FileInputStream in = new FileInputStream(f);
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
            in.close();
            return bo.toString("UTF-8");
        } catch (Throwable t) {
            return Words.LogW.CANT_READ_LOG + t;
        }
    }

    public static synchronized void clear() {
        new File(Paths.system(), "יומן.txt").delete();  // בכונן
    }

    public static void install() {
        if (installed) return;
        installed = true;
        final Thread.UncaughtExceptionHandler prev = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable ex) {
                e(Words.LogW.CRASH, ex);
                if (prev != null) prev.uncaughtException(t, ex);
            }
        });
    }
}
