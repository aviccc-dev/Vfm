package com.tikiyot.vfm;

// מונה המזהים המוחלט — הקובץ הקטן שלצד המסד. אחראי: שמספר זיהוי לא יחזור לעולם, גם אם המסד שוחזר מגיבוי ישן.
// הקובץ שומר "התקרה": המספר הגבוה ביותר שייתכן שכבר הונפק, בבלוקים של 100 — כתיבה לקובץ פעם ב־100 מזהים.
// אסור לו: לדעת איזה מסד משתמש בו.

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public final class IdCounter {

    public static final int BLOCK = 100;

    private final File file;
    private long ceiling;

    /** @throws IOException אם הקובץ קיים ופגום — עדיף לעצור מאשר להסתכן במספר חוזר. */
    public IdCounter(File file) throws IOException {
        this.file = file;
        this.ceiling = read(file);
    }

    public long ceiling() {
        return ceiling;
    }

    /** המספר הראשון שמותר להנפיק: אחרי התקרה, ולא פחות ממה שהמסד עצמו חושב שהבא. */
    public long first(long dbNext) {
        return Math.max(dbNext, ceiling + 1);
    }

    /**
     * לפני שמשתמשים במספר: מבטיח שהקובץ כבר מכסה אותו (כותב בלוק חדש רק כשצריך). מ0.5.1: קודם קוראים את הקובץ שוב —
     * מונה אחר על אותו קובץ (מסד של אופן אחר, הבדיקה המלאה, השחזור) אולי כבר העלה אותו; התקרה בקובץ לעולם לא יורדת.
     */
    public void reserve(long id) throws IOException {
        if (id <= ceiling) return;
        long now = read(file);
        if (id <= now) {
            ceiling = now;
            return;
        }
        long next = Math.max(id + BLOCK - 1, now);
        write(file, next);
        ceiling = next;
    }

    private static long read(File f) throws IOException {
        if (!f.exists()) return 0;
        byte[] b = new byte[64];
        FileInputStream in = new FileInputStream(f);
        int n;
        try {
            n = in.read(b);
        } finally {
            in.close();
        }
        String s = n <= 0 ? "" : new String(b, 0, n, "UTF-8").trim();
        try {
            long v = Long.parseLong(s);
            if (v < 0) throw new NumberFormatException();
            return v;
        } catch (NumberFormatException e) {
            throw new IOException("counter file is damaged: " + f.getPath());
        }
    }

    /** כתיבה אטומית: קובץ זמני, סנכרון לדיסק, ושינוי שם. */
    private static void write(File f, long v) throws IOException {
        File dir = f.getParentFile();
        if (dir != null && !dir.isDirectory() && !dir.mkdirs()) throw new IOException("cannot create " + dir.getPath());
        File tmp = new File(f.getPath() + ".tmp");
        FileOutputStream out = new FileOutputStream(tmp);
        try {
            out.write((v + "\n").getBytes("UTF-8"));
            out.getFD().sync();
        } finally {
            out.close();
        }
        if (!tmp.renameTo(f)) {
            f.delete();
            if (!tmp.renameTo(f)) throw new IOException("cannot replace " + f.getPath());
        }
    }
}
