package com.tikiyot.vfm;

// רשימות חכמות — המפתחות, התבניות המוכנות, ופירוק/חיבור של תנאי. מ0.29: ה"הגדרות הנפוצות" שנשמרו בקובץ הראשי של המנוע
// הישן נמחקו איתו; רשימה חכמה נשמרת במסד..
// כאן: הקריאה, השמירה, הרשימות המוכנות, ומה כלי מציג בפועל. בלי ממשק — נבדק. הממשק: SmartForm.

import java.util.ArrayList;
import java.util.List;

final class SmartLists {

    /** המפתח בסעיף הכלי — המזהה של ההגדרה הנפוצה שהוא מקשר אליה. */
    static final String LINK = "הגדרה נפוצה";  // תחביר
    static final String COND = "תנאי", SORT = "מיון", DIR = "כיוון", VIEW = "תצוגה";  // תחביר
    /** חיפוש גם במסלול, והרחבת כתיב חסר — בסעיף הכלי (חיפוש, רשימה חכמה, הפס הצף). */
    static final String PATHS = "מסלול", LOOSE = "כתיב";  // תחביר
    /**
     * בהגדרות הכלליות: אילו רשימות מוכנות כבר נכתבו (שמות, מופרדים ב-|). כל אחת — פעם אחת;
     * מה שהמשתמש מחק — לא חוזר; מוכנה חדשה בגרסה חדשה — נכתבת.
     */

    /** מנהל קבצים (151) — "אפשרות פשוטה ולא חכמה": כל הקבצים בכל הכוננים, בתיקיות, כולל הכול. */
    /** 1.13.0: "קבצים וירטואליים" (המשתמש) — מול "קבצים פיזיים"; עד 1.12 — "מנהל קבצים" (מתחלף לבד). */
    static final String FILE_MANAGER = "קבצים וירטואליים";  // תחביר

    /** הרשימות המוכנות: שם, תנאי, מיון, כיוון, תצוגה. */
    static final String[][] PRESETS = {
            {FILE_MANAGER, Cond.sourcesCond(java.util.Arrays.asList(Drive.INTERNAL, Drive.CARD)), Sort.NAME, "", FolderView.ALL},
            {"אחרונים", DateSize.DATE + "שבוע", Sort.DATE, "", FolderView.FLAT},  // לפי תאריך — החדש קודם בלי "יורד" (שהופך) // תחביר
            {"נוספו השבוע", DateSize.ADDED + "שבוע", Sort.ADDED, "", FolderView.FLAT},          // 189 // תחביר
            {"כל קבצי המדיה", Cond.TYPE + ":תמונות|שמע|וידאו", Sort.DATE, "", FolderView.FLAT},  // תחביר
            {"תיקיות ריקות", Cond.EMPTY, Sort.NAME, "", FolderView.FLAT}};                     // 175 // תחביר

    private SmartLists() { }

    static final class Named {
        String id = "", name = "", cond = "", sort = "", dir = "", view = "";
    }

    /**
     * מ0.29: התבניות המוכנות בלבד (PRESETS). עד כאן — "הגדרות נפוצות" שנשמרו בקובץ הראשי של המנוע הישן; הוא נמחק,
     * ורשימה חכמה נשמרת במסד (בלי קישור לתבנית — התנאי מועתק אליה, DbSmartForm).
     */
    static List<Named> all() {
        List<Named> out = new ArrayList<Named>();
        for (String[] p : PRESETS) {
            Named n = new Named();
            n.id = "preset:" + p[0];
            n.name = p[0];
            n.cond = p[1];
            n.sort = p[2];
            n.dir = p[3];
            n.view = p[4];
            out.add(n);
        }
        return out;
    }

    static Named byId(String id) {
        if (id == null || id.isEmpty()) return null;
        for (Named n : all()) if (n.id.equals(id)) return n;
        return null;
    }

    static Named byName(String name) {
        for (Named n : all()) if (n.name.equals(name.trim())) return n;
        return null;
    }

    /**
     * מה כלי מציג בפועל: { תנאי, מיון, תצוגה }. מקשר — מהרשימה עם השם (מיון שנקבע במסך הכלי — קודם);
     * מקומי, או שהרשימה שאליה קישר נמחקה מהקובץ — מהכלי עצמו. בלי תצוגה — ברצף.
     */
    static String[] resolve(Target t) {
        String link = linkOf(t);
        Named n = link.isEmpty() ? null : byId(link);
        if (n == null) return new String[]{t.get(COND, ""), t.get(SORT, ""), view(t.get(VIEW, ""))};
        return new String[]{n.cond, t.get(SORT, n.sort), view(n.view)};
    }

    /**
     * 152: מה הטופס מציג בפתיחה — { תנאי, מיון, תצוגה }. רשימה חדשה (t == null) — ריק, דירוג, ברצף.
     * (באג 1.7.41–1.7.43: לרשימה חדשה היו כאן שני ערכים בלבד, והטופס קרס — "הוספת חיפוש" סגרה את התוכנה.)
     */
    static String[] start(Target t) {
        return t == null ? new String[]{"", Sort.RANK, FolderView.FLAT} : resolve(t);
    }

    /**
     * 153 — החיפוש האחד: תנאי שמור ← { ההגדרות, הטקסט }. ההגדרות — מה שהטופס מבין (מקורות, סוג, תאריך, גודל),
     * והן נכנסות לחלונית ההגדרות במסך; כל השאר (מילים, ותנאים שאין להם שדה) — לשורת ההקלדה.
     */
    static String[] split(String cond) {
        if (cond == null) cond = "";
        StringBuilder base = new StringBuilder(Cond.sourcesCond(Cond.sourcesOf(cond)));
        StringBuilder words = new StringBuilder();
        DateSize ds = new DateSize();
        ds.allowAdded = true;                                                 // 189
        boolean kind = false;
        for (String token : Cond.tokens(Cond.withoutSources(cond))) {
            boolean setting = false;
            if (!kind && Cond.keyIs(token, Cond.TYPE)) kind = setting = true;
            else if (ds.take(token) || Cond.isEmptyDirs(token)) setting = true;   // 175: לחלונית ההגדרות
            StringBuilder to = setting ? base : words;
            to.append(to.length() > 0 ? " " : "").append(Cond.quoteToken(token));
        }
        return new String[]{base.toString(), words.toString()};
    }

    /** ההגדרות והטקסט — בחזרה לתנאי אחד. */
    /** 1.11.0: "%כאן%" בתנאי — התיקייה שממנה פתחו (ב"קבצים פיזיים" — הפתוחה; אחרת — תיקיית אופן העבודה). */
    static final String HERE_VAR = "%כאן%";  // תחביר

    /** מחליף את %כאן% בכל רכיב של התנאי, ושומר על «» כשיש רווח. */
    static String withHere(String cond, String dir) {
        if (cond == null || cond.indexOf(HERE_VAR) < 0 || dir == null) return cond;
        StringBuilder sb = new StringBuilder();
        for (String t : Cond.tokens(cond)) sb.append(sb.length() > 0 ? " " : "").append(Cond.quoteToken(t.replace(HERE_VAR, dir)));
        return sb.toString();
    }

    static String join(String settings, String words) {
        String a = settings == null ? "" : settings.trim(), b = words == null ? "" : words.trim();
        return a.isEmpty() ? b : b.isEmpty() ? a : a + " " + b;
    }

    private static String view(String v) {
        for (String m : FolderView.MODES) if (m.equals(v)) return m;
        return FolderView.FLAT;
    }

    /** מוסתרים מוצגים בפתיחה (נשמר בשמירה מהמסך, ונקרא ב־FnSmart). */
    static final String HIDDEN = "נסתרים";  // תחביר

    /** המזהה שהכלי מקשר אליו, או "". */
    static String linkOf(Target t) {
        return t == null ? "" : t.get(LINK, "");
    }

    /** הרשימה שאליה הכלי מקשר — ונמחקה מהקובץ. */
    static boolean broken(Target t) {
        String link = linkOf(t);
        return !link.isEmpty() && byId(link) == null;
    }
}
