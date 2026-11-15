package com.tikiyot.vfm;

// מבנה 155 (סידור תיקיית הפקודות): הסינון של שורת פקודה — תאריך, גודל, "/מכיל:" — אותם תנאים של החיפוש (Cond).
// עד כאן ישב בתוך Batch. אחראי: לפענח את מתגי הסינון, ולומר אם קובץ עובר. אסור לו: לדעת איזו פקודה שואלת.

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class Filter {

    private Filter() { }
    final List<Cond.Test> tests = new ArrayList<Cond.Test>();
    /** מ0.40: /מכיל:<טקסט> — רק קבצים שהטקסט כתוב בהם. המשפך: נבדק אחרון, ורק במי שעבר את השאר (גודל, תאריך). */
    final List<byte[]> contains = new ArrayList<byte[]>();
    String error;

    /** אין סינון בכלל. */
    boolean none() {
        return tests.isEmpty() && contains.isEmpty();
    }

    boolean ok(File f) {
        if (!tests.isEmpty()) {
            Index.Rec r = new Index.Rec();
            r.name = f.getName();
            r.size = f.length();
            r.mtime = f.lastModified();
            for (Cond.Test t : tests) if (!t.ok(null, r)) return false;
        }
        for (byte[] w : contains) if (f.exists() && !Files.contains(f, w)) return false;   // עוד אין (שורה קודמת יוצרת) — נבדק בהרצה
        return true;
    }

    /** הסינון שבמתגים של השורה. מתג שגוי — error. */
    static Filter of(List<String> switches) {
        Filter f = new Filter();
        for (String sw : switches) {
            String s = sw.substring(1);
            String up = s.toUpperCase(Locale.ROOT);
            Cond.Test t = null;
            boolean known = true;
            if (s.startsWith(Batch.SW_CONTAINS)) {                                  // מ0.40
                String v = s.substring(Batch.SW_CONTAINS.length()).trim();
                if (v.length() >= 2 && v.startsWith("\"") && v.endsWith("\"")) v = v.substring(1, v.length() - 1);
                if (v.isEmpty()) {
                    f.error = Words.Cmd.CONTAINS_USAGE;
                    return f;
                }
                try {
                    f.contains.add(v.getBytes("UTF-8"));
                } catch (java.io.UnsupportedEncodingException e) {
                    f.error = Words.Cmd.CONTAINS_USAGE;
                    return f;
                }
                continue;
            }
            if (s.startsWith(Batch.SW_DATE)) t = Cond.date(s.substring(Batch.SW_DATE.length()));
            else if (s.startsWith(Batch.SW_SIZE)) t = Cond.size(s.substring(Batch.SW_SIZE.length()));
            else if (up.startsWith("MAXAGE:")) t = Cond.date(s.substring(7).trim() + "d");
            else if (up.startsWith("MINAGE:")) t = Cond.date("<" + s.substring(7).trim() + "d");
            else if (up.startsWith("MAX:")) t = Cond.size("<" + (parseLong(s.substring(4)) + 1));
            else if (up.startsWith("MIN:")) t = Cond.size(">" + (parseLong(s.substring(4)) - 1));
            else if (up.startsWith("D:")) t = Cond.date(">=" + s.substring(2));
            else known = false;
            if (known && t == null) {
                f.error = Words.Cmd.switchUnknown(sw);
                return f;
            }
            if (t != null) f.tests.add(t);
        }
        return f;
    }

    private static long parseLong(String s) {
        try {
            return Long.parseLong(s.trim());
        } catch (Throwable t) {
            return Long.MIN_VALUE / 2;
        }
    }
}
