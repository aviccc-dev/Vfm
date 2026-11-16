package com.tikiyot.vfm;

// מבנה 181 (סידור קומת החומר): היומן של העתקה / העברה (1.19.0) — עד כאן מחלקה בתוך Physical.

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * יומן: קובץ טקסט שכל קובץ שהגיע ליעד נרשם בו מיד, שורה אחרי שורה — המסלול יחסית לשורש היעד, טאב, גודל.
 * הפסקה באמצע משאירה יומן נכון למה שבוצע. נוסף לסוף קובץ קיים. שורות שמתחילות ב־"#" — כותרת.
 */
public final class Journal {
    final File file;
    private java.io.OutputStream out;
    private boolean failed;
    int lines;

    public Journal(File file, String title) {
        this.file = file;
        try {
            File d = file.getParentFile();
            if (d != null) d.mkdirs();
            out = new java.io.FileOutputStream(file, true);
            write("# " + title + "\n");
        } catch (Throwable t) {
            failed = true;
            Log.e("יומן", t);
        }
    }

    void file(File root, File f) {
        String r = Physical.rel(root.getAbsolutePath(), f.getAbsolutePath());
        if (r == null) r = f.getAbsolutePath();
        write(r + "\t" + f.length() + "\n");
        lines++;
    }

    private void write(String line) {
        if (out == null) return;
        try {
            out.write(line.getBytes("UTF-8"));
            out.flush();
        } catch (Throwable t) {
            failed = true;
            Log.e("יומן", t);
        }
    }

    /** נכשל בפתיחה או בכתיבה. */
    public boolean failed() {
        return failed;
    }

    public void close() {
        if (out == null) return;
        try {
            if (out instanceof java.io.FileOutputStream) ((java.io.FileOutputStream) out).getFD().sync();
            out.close();
        } catch (Throwable t) {
            failed = true;
        }
        out = null;
    }

    /** קריאה: {מסלול יחסי, גודל} לכל שורה (בלי הכותרות). */
    public static List<String[]> read(File f) {
        List<String[]> outList = new ArrayList<String[]>();
        try {
            for (String line : Files.readUtf8(f).split("\n")) {
                if (line.isEmpty() || line.startsWith("#")) continue;
                int t = line.lastIndexOf('\t');
                outList.add(t < 0 ? new String[]{line, ""} : new String[]{line.substring(0, t), line.substring(t + 1)});
            }
        } catch (Throwable ignored) {
            Log.e("Physical.1684", ignored);   // מ0.32: לא בולעים בשקט
        }
        return outList;
    }
}
