package com.tikiyot.vfm;

// 181: כפתורי תצוגה מהירה — כל כפתור: שם + תנאי (כמה רכיבים, "חייב" או "בלי" — מינוס לפני רכיב).
// הרשימה — ב"הגדרות כלליות.ini", פרק [כפתורי תצוגה] (שם = תנאי), ניתנת לעריכה ביד.
// מה דולק — לכל כלי ולכל מופע שלו בנפרד (בסעיף הכלי לפי המזהה); חיפוש שלא מכלי — בפרק [חיפוש].
// אחראי: הרשימה, המצב והתנאי המשולב. אסור לו: לבנות מסכים.

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class Quick {

    private Quick() { }

    static final String SECTION = "כפתורי תצוגה", START_ON = "דולקים בהתחלה",                  // תחביר
            STATE = "כפתורים דולקים", SEP = "|";                                               // תחביר

    /**
     * 216: כפתור הרצה — במקום תנאי: "הרצה:<מסלול קובץ הפקודות>". נגיעה מריצה את הקובץ (לא מדליקה סינון).
     * המשתמש: "הכפתורים למעלה … כדאי שיהיה גם אפשרות לכפתור שיריץ פקודה".
     */
    static final String RUN = "הרצה:";   // תחביר

    static boolean isRun(Button b) {
        return b.cond.startsWith(RUN);
    }

    static final class Button {
        String name, cond;

        Button(String name, String cond) {
            this.name = name;
            this.cond = cond;
        }
    }

    /** ברירת המחדל בהתקנה: ארבעה כפתורים; "בלי תיקיית התוכנה" — דולק (180). */
    static String[][] defaults() {
        String app;
        try {
            app = Paths.data().getAbsolutePath();
        } catch (Throwable t) {
            app = "/storage/emulated/0/" + Paths.APP_DIR;                                        // בכונן
        }
        return new String[][]{
                {Words.Quick.APP, Cond.NOT + Cond.SOURCE + ":" + Cond.quote(app)},
                {Words.Quick.NO_INI, Cond.NOT + Cond.TYPE + ":ini"},                              // תחביר
                {Words.Quick.BOOKS, Cond.TYPE + ":" + Types.FIVE[0][0]},
                {Words.Quick.MONTH, Cond.DATE + ":" + Words.Quick.DATES[2]},
                {START_ON, Words.Quick.APP}};
    }

    static List<Button> all() {
        List<Button> out = new ArrayList<Button>();
        for (String[] kv : Conf.section(SECTION, defaults()))
            if (!kv[0].equals(START_ON) && !kv[0].trim().isEmpty()) out.add(new Button(kv[0].trim(), kv[1].trim()));
        return out;
    }

    static Set<String> parse(String state) {
        Set<String> out = new LinkedHashSet<String>();
        if (state != null) for (String s : state.split("\\" + SEP)) if (!s.trim().isEmpty()) out.add(s.trim());
        return out;
    }

    static String state(Set<String> on) {
        StringBuilder sb = new StringBuilder();
        for (String s : on) sb.append(sb.length() > 0 ? SEP : "").append(s);
        return sb.toString();
    }

    /** מה דולק בכלי שאין לו עוד מצב משלו. */
    static Set<String> startOn() {
        all();                                                                                  // ברירת המחדל נכתבת אם אין
        return parse(Conf.get(SECTION, START_ON, ""));
    }

    /** התנאי של הכפתורים הדולקים — מצטרף לחיפוש. */
    static String cond(List<Button> all, Set<String> on) {
        StringBuilder sb = new StringBuilder();
        for (Button b : all)
            if (on.contains(b.name) && !b.cond.isEmpty() && !isRun(b)) sb.append(sb.length() > 0 ? " " : "").append(b.cond);
        return sb.toString();
    }

    /** שמירת כל הרשימה לפי הסדר (שינוי, הזזה, מחיקה) — "דולקים בהתחלה" נשאר. */
    static void save(List<Button> all) {
        String start = Conf.get(SECTION, START_ON, "");
        List<String[]> rows = new ArrayList<String[]>();
        for (Button b : all) rows.add(new String[]{b.name, b.cond});
        rows.add(new String[]{START_ON, start});
        Conf.replaceSection(SECTION, rows);
    }

    /** הרכיבים של תנאי כפתור — לעורך: כל רכיב שורה ("-" — "בלי"). */
    static List<String> parts(String cond) {
        List<String> out = new ArrayList<String>();
        for (String t : Cond.tokens(cond)) {
            boolean not = t.length() > 1 && t.startsWith(Cond.NOT);
            out.add((not ? Cond.NOT : "") + Cond.quoteToken(not ? t.substring(1) : t));
        }
        return out;
    }

    static String join(List<String> parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) sb.append(sb.length() > 0 ? " " : "").append(p);
        return sb.toString();
    }

    static boolean isNot(String part) {
        return part.startsWith(Cond.NOT) && part.length() > 1;
    }

    static String flip(String part) {
        return isNot(part) ? part.substring(1) : Cond.NOT + part;
    }
}
