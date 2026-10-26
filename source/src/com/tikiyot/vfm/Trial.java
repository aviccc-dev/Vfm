package com.tikiyot.vfm;

// 182: תקופת הניסיון — היומית, השבועית ו"מה השתנה מאז" רושמות מה מצאו מעבר לבדיקה השוטפת, בקובץ טקסט
// גלוי ב־".מערכת" של אופן העבודה. הסיכום (בהגדרות) מחושב מהקובץ עצמו — הנתונים במקום אחד.
// שורה: "2026-10-07 09:12 | יומית | 0.31 שנ' | נבדקו 4157 | נמצאו 2"

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    static List<String> lines() {
        List<String> out = new ArrayList<String>();
        File f = file();
        if (!f.exists()) return out;
        try {
            for (String l : Files.readUtf8(f).split("\n")) if (!l.trim().isEmpty()) out.add(l.trim());
        } catch (Throwable t) {
            Log.e("תקופת ניסיון — קריאה", t);
        }
        return out;
    }

    /** סיכום לפי סוג: {כמה פעמים, כמה נמצא בסך הכול, כמה פעמים נמצא משהו}. שורה שלא הובנה — מדלגים. */
    static Map<String, int[]> summary(List<String> lines) {
        Map<String, int[]> out = new LinkedHashMap<String, int[]>();
        for (String k : new String[]{Words.Trial.SINCE, Words.Trial.DAILY, Words.Trial.WEEKLY}) out.put(k, new int[3]);
        String prefix = Words.Trial.found(0).replace("0", "");
        for (String l : lines) {
            String[] p = l.split(" \\| ");
            if (p.length < 5 || !p[p.length - 1].startsWith(prefix)) continue;
            int[] s = out.get(p[1].trim());
            if (s == null) continue;
            try {
                int n = Integer.parseInt(p[p.length - 1].substring(prefix.length()).trim());
                s[0]++;
                s[1] += n;
                if (n > 0) s[2]++;
            } catch (NumberFormatException ignored) {
            }
        }
        return out;
    }

    /** הסיכום במילים — שורה לכל סוג. */
    static String text(List<String> lines) {
        if (lines.isEmpty()) return Words.Trial.EMPTY;
        StringBuilder sb = new StringBuilder(Words.Trial.FROM_DATE + lines.get(0).split(" ")[0] + ":\n");
        for (Map.Entry<String, int[]> e : summary(lines).entrySet()) {
            int[] s = e.getValue();
            String about = e.getKey().equals(Words.Trial.DAILY) ? Words.Trial.DAILY_ABOUT
                    : e.getKey().equals(Words.Trial.WEEKLY) ? Words.Trial.WEEKLY_ABOUT : Words.Trial.SINCE_ABOUT;
            sb.append("\n• ").append(e.getKey()).append(" — ").append(about).append(":\n   ")
                    .append(e.getKey().equals(Words.Trial.SINCE) ? Words.Trial.FOUND_ANY + Words.Trial.runs(s[0])
                            : Words.Trial.RAN + Words.Trial.runs(s[0]) + Words.Trial.FOUND_ANY_FEM + Words.Trial.runs(s[2]))
                    .append(" · ").append(Words.Trial.found(s[1])).append(Words.Trial.FILES_LINE);
        }
        return sb.toString();
    }
}
