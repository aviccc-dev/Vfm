package com.tikiyot.vfm;

// 1.13.0: קובץ פקודות כתוכנה (המשתמש, 15.10: "שני מצבים — אחד כמו שהיה, רשימת פקודות; ומצב שני — שיש בתוך הקובץ
// ממשק. הממשק המינימלי הוא צלמית, וכך הוא הופך להיות תוכנה"). בראש הקובץ, כמו "הדמיה" — שורות כותרת:
//     צלמית 📚
//     שם ספרים חדשים
//     למה אוסף את הספרים מההורדות לתיקיית הספרים
// בלי "צלמית" — קובץ פקודות רגיל. עם — תוכנה: בכל מקום שהיא מופיעה — הצלמית, השם ("שם", ובלעדיו שם הקובץ),
// ו"למה" מתחת; נגיעה — הרצה. ונתון חי: כמה קבצים התוכנה מסננת ומוצאת עכשיו (צד המקור של כל פקודה; קובץ שנמצא
// בשתי פקודות — פעם אחת; פקודה בלי סינון קבצים — לא נספרת; תוכנה בלי שום סינון — אין מספר).
// אחראי: לקרוא את הכותרת ולספור. אסור לו: לשנות משהו בכונן.

import android.content.Context;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class Program {

    static final String ICON = "צלמית", NAME = "שם", WHY = "למה";   // תחביר

    String icon = "", name = "", why = "";
    /** תוכנה — קובץ קטן; גדול מזה לא נקרא (לא בכל ציור של המסך). */
    private static final long MAX = 64 * 1024;

    private Program() { }

    /** שורת כותרת: "צלמית …" / "שם …" / "למה …" (וגם "REM למה: …"). */
    static String[] header(String line) {
        String s = line.trim();
        if (s.toUpperCase(java.util.Locale.ROOT).startsWith("REM ")) {
            String r = s.substring(4).trim();
            if (r.startsWith(WHY + ":")) return new String[]{WHY, r.substring(WHY.length() + 1).trim()};
            return null;
        }
        for (String k : new String[]{ICON, NAME, WHY})
            if (s.equals(k) || s.startsWith(k + " ") || s.startsWith(k + ":"))
                return new String[]{k, s.substring(k.length()).replaceFirst("^[:\\s]+", "").trim()};
        return null;
    }

    /** האם השורה היא כותרת שהמנוע מדלג עליה ("REM למה:" — ממילא הערה). */
    static boolean isHeader(String line) {
        String[] h = header(line);
        return h != null && !line.trim().toUpperCase(java.util.Locale.ROOT).startsWith("REM ");
    }

    static Program parse(String text) {
        Program p = new Program();
        if (text == null) return p;
        for (String raw : Bidi.strip(text).replace("\r", "").split("\n")) {
            String[] h = header(raw);
            if (h == null) continue;
            if (h[0].equals(ICON) && p.icon.isEmpty()) p.icon = h[1];
            else if (h[0].equals(NAME) && p.name.isEmpty()) p.name = h[1];
            else if (h[0].equals(WHY) && p.why.isEmpty()) p.why = h[1];
        }
        return p;
    }

    /** קורא את הכותרת; קובץ שלא נקרא — בלי כותרת. קבצים גדולים לא נקראים (תוכנה — קובץ קטן). */
    static Program read(File f) {
        try {
            if (f.isFile() && f.length() < MAX) return parse(Files.readUtf8(f));
        } catch (Throwable ignored) {
        }
        return new Program();
    }

    /** תוכנה = יש לה צלמית. */
    boolean isProgram() {
        return !icon.isEmpty();
    }

    String title(File f) {
        return !name.isEmpty() ? name : Text.stem(f.getName());
    }

    // ---------- הנתון החי ----------

    /**
     * כמה קבצים התוכנה מסננת ומוצאת עכשיו; -1 — אין בה שום סינון (אז מוצג רק "למה").
     * לפקודות הפתיחה (פתח / מצא) — מהאינדקס ומהרשימות; ל־MOVE / COPY / DEL / REN — מהתכנון (בלי לשנות דבר).
     */
    static int count(Context c, File f) {
        String text;
        try {
            if (f.length() >= MAX) return -1;                                 // 1.13.1: כמו read — קובץ ענק אינו תוכנה
            text = Files.readUtf8(f);
        } catch (Throwable t) {
            return -1;
        }
        Map<String, String> vars = Batch.vars(f.getParentFile(), Vols.internalRoot(c), Vols.cardRoot(c));
        Batch.Plan plan = Batch.plan(text, f.getParentFile(), vars);
        Set<String> all = new HashSet<String>();
        boolean any = false;
        for (Batch.Step s : plan.steps) {
            if (s.error != null) continue;
            if (s.cmd.equals("MOVE") || s.cmd.equals("COPY") || s.cmd.equals("DEL") || s.cmd.equals("REN")) {
                any = true;
                all.addAll(s.files);
            } else if (s.cmd.equals(Batch.OPEN)) {
                List<String> found = Verbs.files(c, Verbs.parse(s.open));
                if (found != null) {
                    any = true;
                    all.addAll(found);
                }
            }
        }
        return any ? all.size() : -1;
    }

    /** הנתון כטקסט: "12 קבצים", או null — אין מספר. */
    static String stat(Context c, File f) {
        int n = count(c, f);
        return n < 0 ? null : Words.files(n);
    }
}
