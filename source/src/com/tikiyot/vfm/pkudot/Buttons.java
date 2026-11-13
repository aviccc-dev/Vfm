package com.tikiyot.vfm;

// שורות הכפתורים של עד מ0.40 — הקובץ "כפתורים.txt". מ0.41: השורות ברשימות של "שמור לתוכנה" (Menus); כאן נשארו רק קריאת
// הקובץ הישן (למעבר החד־פעמי) וברירת המחדל שבקוד (שורת החירום של TopRow). מה שהיה:
// 1.9.0: שורה עליונה אחת. 1.10.0: ארבעה מקומות, ובכל אחד עד 5 שורות (פריט 9, שלבים 2–3):
//   [למעלה] — בכל המסכים · [למעלה: <סוג מסך>] · [למטה: <סוג מסך>] · [למטה] — בכל המסכים.
// בקובץ "כפתורים.txt" בתיקיית ההגדרות של אופן העבודה (לכל אופן משלו; עובר בהעתקת אופן):
//   [מקום]  ואחריו שורה לכל כפתור:  סמל | שם | פעולה   ·   "---" — שורה חדשה באותו מקום   ·   # — הערה.
// קובץ של 1.9.0 (בלי [מקום]) — הכול בשורה העליונה.
// 1.13.0: הפעולה של כפתור = שורת פקודה (Verbs): פתח מסך הבית · חזור · פתח רשימה <מסלול> · פתח רשימה חכמה <שם>
//           · פתח תיקייה <מסלול> · הרץ <קובץ פקודות> · צור רשימה · הוסף מהכונן / מחיפוש / כלי · או כל שורה של קובץ פקודות.
//           קובץ של לפני 1.13.0 (מסך:בית, רשימה:…, קובץ:…, פקודה:…, פעולה:…) — מתורגם בקריאה (Verbs.fromOld).
// אחראי: מה בשורות. אסור לו: לבצע (הביצוע — TopRow).
// מועמד למחיקה (מ0.42): המעבר החד־פעמי מ"כפתורים.txt" נעשה (במכשיר — מ0.41). כשאין עוד מכשיר בלי מעבר — הקריאה של הקובץ (parseAll,
// Layout, Verbs.fromOld) יכולה לצאת, ושורת החירום (defaults) — לעבור ל־Menus.

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class Buttons {

    static final String FILE = "כפתורים.txt";  // בכונן
    /** 1.9.0 קרא לכפתור "מנהל קבצים"; מ־1.10.0 — "קבצים פיזיים" (להבדיל מהרשימה המובנית שדרך האינדקס). */
    private static final String OLD_FILES = "מנהל קבצים";  // תחביר

    static final String SCREEN = "מסך:", FOLDER = "תיקייה:", LIST = "רשימה:", SMART = "חכמה:",   // תחביר
            BATCH = "קובץ:", LINE = "פקודה:", ACT = "פעולה:";   // תחביר
    /** 1.13.0: פקודות. */
    static final String HOME = Verbs.HOME_LINE, BACK = Verbs.BACK_LINE, SEARCH = Verbs.openScreen(Verbs.S_SEARCH),
            COMMANDS = Verbs.openScreen(Verbs.S_COMMANDS), FILES = Verbs.openScreen(Verbs.S_DISK);
    /** 1.13.0: אותן פעולות — כשורת פקודה בכפתור. */
    static final String C_NEW_LIST = Verbs.CREATE + " " + Verbs.LIST, C_FROM_DRIVE = Verbs.ADD + " " + Verbs.A_DRIVE,
            C_FROM_SEARCH = Verbs.ADD + " " + Verbs.A_SEARCH;

    /** המקומות. */
    static final String TOP = "למעלה", BOTTOM = "למטה";   // תחביר
    static final String ROW_BREAK = "---";
    static final int MAX_ROWS = 5;

    /** מקום של סוג מסך: "למטה: רשימה ידנית". */
    static String local(String side, String screen) {
        return side + ": " + screen;
    }

    static boolean general(String place) {
        return TOP.equals(place) || BOTTOM.equals(place);
    }

    static final class Btn {
        String icon, name, act;

        Btn(String icon, String name, String act) {
            this.icon = icon;
            this.name = name;
            this.act = act;
        }
    }

    /** כל השורות: מקום ← שורות ← כפתורים. מקום בלי שורות — לא מוצג ולא תופס מקום. */
    static final class Layout {
        final Map<String, List<List<Btn>>> places = new LinkedHashMap<String, List<List<Btn>>>();

        List<List<Btn>> rows(String place) {
            List<List<Btn>> r = places.get(place);
            if (r == null) places.put(place, r = new ArrayList<List<Btn>>());
            return r;
        }
    }

    private Buttons() { }

    /** השורה העליונה של ברירת המחדל: 🏠 בית · ⭐ פקודות · 🔎 חיפוש · 📂 קבצים פיזיים · → חזרה. */
    static List<Btn> defaults() {
        List<Btn> l = new ArrayList<Btn>();
        l.add(new Btn("🏠", Words.Top.HOME, HOME));
        l.add(new Btn("⭐", Words.Top.COMMANDS, COMMANDS));
        l.add(new Btn("🔎", Words.Top.SEARCH, SEARCH));
        l.add(new Btn("📂", Words.Top.FILES, FILES));
        l.add(new Btn("→", Words.Top.BACK, BACK));
        return l;
    }

    /** ברירת המחדל של מקום אחד ("עשירה", פריט 9); מקום בלי ברירת מחדל — ריק. */
    static List<List<Btn>> defaults(String place) {
        List<List<Btn>> rows = new ArrayList<List<Btn>>();
        if (TOP.equals(place)) rows.add(defaults());
        else if (local(BOTTOM, Words.Top.S_LIST).equals(place)) {            // מה שהיה קבוע בתחתית הרשימה
            List<Btn> l = new ArrayList<Btn>();
            l.add(new Btn("➕", Words.Top.NEW_LIST, C_NEW_LIST));
            l.add(new Btn("💾", Words.Top.FROM_DRIVE, C_FROM_DRIVE));
            l.add(new Btn("🔎", Words.Top.FROM_SEARCH, C_FROM_SEARCH));
            rows.add(l);
        }
        return rows;
    }

    static Layout defaultsAll() {
        Layout l = new Layout();
        for (String p : new String[]{TOP, local(BOTTOM, Words.Top.S_LIST)})
            l.places.put(p, defaults(p));
        return l;
    }

    /** כל השורות מהטקסט. עוגן שחסר בשורות הכלליות (נמחק ביד בקובץ) — חוזר לשורה העליונה. */
    static Layout parseAll(String text) {
        Layout lay = new Layout();
        boolean sections = false;
        for (String raw : text.split("\r\n|\r|\n")) {
            String s = raw.trim();
            if (s.startsWith("[") && s.endsWith("]")) {
                sections = true;
                break;
            }
        }
        if (!sections) {                                                      // 1.9.0: רק השורה העליונה נכתבה
            lay.places.putAll(defaultsAll().places);
            lay.rows(TOP).clear();
        }
        String place = TOP;
        List<Btn> row = null;
        for (String raw : text.split("\r\n|\r|\n")) {
            String s = raw.trim();
            if (s.isEmpty() || s.startsWith("#")) continue;
            if (s.startsWith("[") && s.endsWith("]")) {
                place = s.substring(1, s.length() - 1).trim();
                lay.rows(place);
                row = null;
                continue;
            }
            if (s.equals(ROW_BREAK)) {                                         // 1.11.0: "---" פותח שורה — גם הראשונה (שורה ריקה נשמרת)
                List<List<Btn>> rows = lay.rows(place);
                if (row == null && rows.isEmpty()) rows.add(row = new ArrayList<Btn>());
                else if (rows.size() < MAX_ROWS) rows.add(row = new ArrayList<Btn>());
                continue;
            }
            String[] p = s.split("\\|", 3);
            if (p.length < 3) continue;
            String icon = p[0].trim(), name = p[1].trim(), act = Verbs.fromOld(p[2].trim());   // 1.13.0: כפתור = שורת פקודה
            if (act.isEmpty()) continue;
            if (name.isEmpty()) name = act;
            if (!sections && act.equals(FILES) && name.equals(OLD_FILES)) name = Words.Top.FILES;   // 1.10.0: השם החדש — רק בקובץ של 1.9.0 (1.11.0: אחרת שם שבחר המשתמש חוזר)
            if (row == null) {
                List<List<Btn>> rows = lay.rows(place);
                if (rows.isEmpty()) rows.add(new ArrayList<Btn>());
                row = rows.get(rows.size() - 1);
            }
            row.add(new Btn(icon, name, act));
        }
        anchors(lay);
        return lay;
    }

    /** בית וחזרה — תמיד באחת השורות הכלליות. */
    private static void anchors(Layout lay) {
        if (count(lay, HOME) == 0) firstTop(lay).add(0, new Btn("🏠", Words.Top.HOME, HOME));
        if (count(lay, BACK) == 0) firstTop(lay).add(new Btn("→", Words.Top.BACK, BACK));
    }

    private static List<Btn> firstTop(Layout lay) {
        List<List<Btn>> rows = lay.rows(TOP);
        if (rows.isEmpty()) rows.add(new ArrayList<Btn>());
        return rows.get(0);
    }

    /** כמה עותקים של הפעולה בשורות הכלליות. */
    static int count(Layout lay, String act) {
        int n = 0;
        for (String p : new String[]{TOP, BOTTOM})
            if (lay.places.containsKey(p))
                for (List<Btn> r : lay.places.get(p)) for (Btn b : r) if (act.equals(b.act)) n++;
        return n;
    }

    /** הקובץ הישן (עד מ0.40). מ0.41: נקרא פעם אחת — השורות שבו עוברות לרשימות (Menus.install); הקובץ נשאר במקומו. */
    static File file() {
        return new File(Paths.settings(), FILE);
    }
}
