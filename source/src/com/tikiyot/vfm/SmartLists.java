package com.tikiyot.vfm;

// הגדרות נפוצות (147; השם מ־154) — הגדרות של רשימה חכמה שעומדות בפני עצמן, בקובץ הראשי: [הגדרה נפוצה: שם].
// כלי שמקשר אליהן ("הגדרה נפוצה = מזהה") — שינוי בהן מופיע בכל מקום. בלי — התנאי בתוך הכלי (רק לרשימה הזו).
// כאן: הקריאה, השמירה, הרשימות המוכנות, ומה כלי מציג בפועל. בלי ממשק — נבדק. הממשק: SmartForm.

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class SmartLists {

    static final String SECTION = "הגדרה נפוצה: ";  // תחביר
    /** המפתח בסעיף הכלי — המזהה של ההגדרה הנפוצה שהוא מקשר אליה. */
    static final String LINK = "הגדרה נפוצה";  // תחביר
    static final String COND = "תנאי", SORT = "מיון", DIR = "כיוון", VIEW = "תצוגה";  // תחביר
    /** חיפוש גם במסלול, והרחבת כתיב חסר — בסעיף הכלי (חיפוש, רשימה חכמה, הפס הצף). */
    static final String PATHS = "מסלול", LOOSE = "כתיב";  // תחביר
    /**
     * בהגדרות הכלליות: אילו רשימות מוכנות כבר נכתבו (שמות, מופרדים ב-|). כל אחת — פעם אחת;
     * מה שהמשתמש מחק — לא חוזר; מוכנה חדשה בגרסה חדשה — נכתבת.
     */
    static final String PRESETS_DONE = "רשימות חכמות מוכנות";  // תחביר

    /** מנהל קבצים (151) — "אפשרות פשוטה ולא חכמה": כל הקבצים בכל הכוננים, בתיקיות, כולל הכול. */
    /** 1.13.0: "קבצים וירטואליים" (המשתמש) — מול "קבצים פיזיים"; עד 1.12 — "מנהל קבצים" (מתחלף לבד). */
    static final String FILE_MANAGER = "קבצים וירטואליים";  // תחביר
    static final String OLD_FILE_MANAGER = "מנהל קבצים";  // תחביר

    /** הרשימות המוכנות: שם, תנאי, מיון, כיוון, תצוגה. */
    static final String[][] PRESETS = {
            {FILE_MANAGER, Sources.toCond(java.util.Arrays.asList(Sources.INTERNAL, Sources.CARD)), Sort.NAME, "", FolderView.ALL},
            {"אחרונים", DateSize.DATE + "שבוע", Sort.DATE, "", FolderView.FLAT},  // לפי תאריך — החדש קודם בלי "יורד" (שהופך) // תחביר
            {"נוספו השבוע", DateSize.ADDED + "שבוע", Sort.ADDED, "", FolderView.FLAT},          // 189 // תחביר
            {"כל קבצי המדיה", Cond.TYPE + ":תמונות|שמע|וידאו", Sort.DATE, "", FolderView.FLAT},  // תחביר
            {"תיקיות ריקות", Cond.EMPTY, Sort.NAME, "", FolderView.FLAT}};                     // 175 // תחביר

    private SmartLists() { }

    static final class Named {
        String id = "", name = "", cond = "", sort = "", dir = "", view = "";
    }

    /** כל הרשימות עם שם, לפי הסדר בקובץ. בפעם הראשונה — נכתבות הרשימות המוכנות. */
    static List<Named> all() {
        String done = Conf.get(Conf.GENERAL, PRESETS_DONE, "");
        java.util.Set<String> written = new java.util.HashSet<String>(java.util.Arrays.asList(done.split("\\|")));
        boolean changed = false;
        if (written.contains(OLD_FILE_MANAGER) && !written.contains(FILE_MANAGER)) {   // 1.13.0: השם החדש — אותה רשימה
            Structure.renameSection(SECTION, OLD_FILE_MANAGER, FILE_MANAGER);
            written.add(FILE_MANAGER);
            changed = true;
        }
        for (String[] p : PRESETS)
            if (!written.contains(p[0])) {
                if (find(Structure.sections(SECTION), p[0]) == null) save(p[0], p[1], p[2], p[3], p[4]);
                written.add(p[0]);
                changed = true;
            }
        if (changed) {
            StringBuilder sb = new StringBuilder();
            for (String[] p : PRESETS) if (written.contains(p[0])) sb.append(sb.length() > 0 ? "|" : "").append(p[0]);
            Conf.set(Conf.GENERAL, PRESETS_DONE, sb.toString());
        }
        List<Named> out = new ArrayList<Named>();
        for (Structure.Func f : Structure.sections(SECTION)) {
            Named n = new Named();
            n.id = f.id;
            n.name = f.name;
            n.cond = val(f, COND);
            n.sort = val(f, SORT);
            n.dir = val(f, DIR);
            n.view = val(f, VIEW);
            out.add(n);
        }
        return out;
    }

    private static String val(Structure.Func f, String key) {
        String v = f.params.get(key);
        return v == null ? "" : v;
    }

    private static Structure.Func find(List<Structure.Func> all, String name) {
        for (Structure.Func f : all) if (f.name.equals(name.trim())) return f;
        return null;
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

    /** יוצר או מעדכן לפי שם; מחזיר את המזהה. כיוון או תצוגה ריקים — לא נכתבים. */
    static String save(String name, String cond, String sort, String dir, String view) {
        Map<String, String> kv = new LinkedHashMap<String, String>();
        kv.put(COND, cond);
        kv.put(SORT, sort);
        if (dir != null && !dir.isEmpty()) kv.put(DIR, dir);
        if (view != null && !view.isEmpty()) kv.put(VIEW, view);
        return Structure.putSection(SECTION, name.trim(), "s", kv);
    }

    /**
     * 1.13.0: "צור רשימה חכמה" — לא משכפל לעולם. הזהות של רשימה חכמה = ההגדרות שלה (המשתמש: "רוצים הגדרות אחרות —
     * אז זו רשימה חדשה"): יש רשימה עם אותו תנאי — היא; אין — נוצרת; אותו שם עם תנאי אחר — שם עם מספר.
     */
    static Named ensure(String name, String cond, String sort) {
        String key = Text.norm(cond).trim();
        for (Named n : all()) if (Text.norm(n.cond).trim().equals(key)) return n;
        String base = name.trim(), use = base;
        for (int i = 2; byName(use) != null; i++) use = base + " (" + i + ")";
        save(use, cond.trim(), sort == null || sort.isEmpty() ? Sort.NAME : sort, "", FolderView.FLAT);
        return byName(use);
    }

    /** 1.13.0: "עדכן רשימה חכמה" — רק קיימת; שינוי מפורש של התנאי (והמיון, אם נכתב). */
    static Named update(String name, String cond, String sort) {
        Named n = byName(name);
        if (n == null) return null;
        save(n.name, cond.trim(), sort == null || sort.isEmpty() ? n.sort : sort, n.dir, n.view);
        return byName(n.name);
    }

    /** כמה כלים מקשרים אליה — "שינוי כאן יופיע ב־N מקומות". */
    static int users(String id) {
        int n = 0;
        for (Structure.Func f : Structure.load().all()) if (id.equals(linkOf(f.target()))) n++;
        return n;
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
        StringBuilder base = new StringBuilder(Sources.toCond(Sources.fromCond(cond)));
        StringBuilder words = new StringBuilder();
        DateSize ds = new DateSize();
        ds.allowAdded = true;                                                 // 189
        boolean kind = false;
        for (String token : Cond.tokens(Sources.rest(cond))) {
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
