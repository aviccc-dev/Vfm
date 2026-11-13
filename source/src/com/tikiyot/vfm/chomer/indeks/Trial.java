package com.tikiyot.vfm;

// 182: תקופת הניסיון — היומית, השבועית ו"מה השתנה מאז" רושמות מה מצאו מעבר לבדיקה השוטפת, בקובץ טקסט
// גלוי ב־".מערכת" של אופן העבודה. הסיכום — בתוסף האבחון (מ0.43; עד כאן בהגדרות), מהקובץ עצמו — הנתונים במקום אחד.
// שורה: "2026-10-07 09:12 | יומית | 0.31 שנ' | נבדקו 4157 | נמצאו 2"

import java.io.File;

final class Trial {

    private Trial() { }

    static final String SEP = " | ";                                                         // תחביר

    static File file() {
        return new File(Paths.system(), Words.Trial.FILE);
    }

    /** שורה אחת ביומן. */
    static String line(long when, String kind, long ms, String detail, int found) {
        String date = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US).format(new java.util.Date(when));
        return date + SEP + kind + SEP + String.format(java.util.Locale.US, "%.2f שנ'", ms / 1000.0) + SEP + detail  // תחביר
                + SEP + Words.Trial.found(found);
    }

    static synchronized void add(String kind, long ms, String detail, int found) {
        try {
            java.io.FileOutputStream o = new java.io.FileOutputStream(file(), true);
            try {
                o.write((line(System.currentTimeMillis(), kind, ms, detail, found) + "\n").getBytes("UTF-8"));
            } finally {
                o.close();
            }
        } catch (Throwable t) {
            Log.e("תקופת ניסיון", t);
        }
    }
}
