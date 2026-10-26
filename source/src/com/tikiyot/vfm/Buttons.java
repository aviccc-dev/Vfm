package com.tikiyot.vfm;

// שורות הכפתורים — הנתונים בלבד (בלי ממשק): קריאה, כתיבה, ברירת מחדל, עוגנים.
// 1.9.0: שורה עליונה אחת. 1.10.0: ארבעה מקומות, ובכל אחד עד 5 שורות (פריט 9, שלבים 2–3):
//   [למעלה] — בכל המסכים · [למעלה: <סוג מסך>] · [למטה: <סוג מסך>] · [למטה] — בכל המסכים.
// בקובץ "כפתורים.txt" בתיקיית ההגדרות של אופן העבודה (לכל אופן משלו; עובר בהעתקת אופן):
//   [מקום]  ואחריו שורה לכל כפתור:  סמל | שם | פעולה   ·   "---" — שורה חדשה באותו מקום   ·   # — הערה.
// קובץ של 1.9.0 (בלי [מקום]) — הכול בשורה העליונה.
// 1.13.0: הפעולה של כפתור = שורת פקודה (Verbs): פתח מסך הבית · חזור · פתח רשימה <מסלול> · פתח רשימה חכמה <שם>
//           · פתח תיקייה <מסלול> · הרץ <קובץ פקודות> · צור רשימה · הוסף מהכונן / מחיפוש / כלי · או כל שורה של קובץ פקודות.
//           קובץ של לפני 1.13.0 (מסך:בית, רשימה:…, קובץ:…, פקודה:…, פעולה:…) — מתורגם בקריאה (Verbs.fromOld).
// אחראי: מה בשורות. אסור לו: לבצע (הביצוע — TopRow).

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
            COMMANDS = Verbs.openScreen(Verbs.S_COMMANDS), FILES = Verbs.openScreen(Verbs.S_DISK),
            SETTINGS = Verbs.openScreen(Verbs.S_SETTINGS);
    /** 1.10.0: פעולות של סוג מסך — הקוד שהמסך מכיר (TopRow.Host). */
    static final String NEW_LIST = ACT + "רשימה חדשה", FROM_DRIVE = ACT + "מהכונן",   // תחביר
            FROM_SEARCH = ACT + "מחיפוש";   // תחביר
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

        /** עוגן: בית וחזרה — הפעולה קבועה, ותמיד יש עותק באחת השורות הכלליות (פריט 9: "תמיד נגישים"). */
        boolean anchor() {
            return HOME.equals(act) || BACK.equals(act);
        }

        String line() {
            return icon + " | " + name + " | " + act;
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

        boolean has(String place) {
            List<List<Btn>> r = places.get(place);
            return r != null && !r.isEmpty();
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

    static File file() {
        return new File(Paths.settings(), FILE);
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

    static String textAll(Layout lay) {
        StringBuilder sb = new StringBuilder();
        sb.append("# שורות הכפתורים — [מקום], ואחריו שורה לכל כפתור:  סמל | שם | פעולה   ·   --- שורה חדשה\n");   // בכונן
        sb.append("# מקומות: [למעלה] [למעלה: <סוג מסך>] [למטה: <סוג מסך>] [למטה]\n");   // בכונן
        sb.append("# הפעולה — שורת פקודה: פתח מסך הבית · חזור · פתח מסך חיפוש / פקודות / קבצים פיזיים / קבצים וירטואליים / הגדרות / סל\n");   // בכונן
        sb.append("#   פתח רשימה <מסלול> · פתח רשימה חכמה <שם> · פתח תיקייה <מסלול> · מצא <תנאי> · הרץ <קובץ פקודות>\n");   // בכונן
        sb.append("#   צור רשימה · הוסף מהכונן · הוסף מחיפוש · הוסף כלי · או כל שורה של קובץ פקודות\n");   // בכונן
        for (Map.Entry<String, List<List<Btn>>> e : lay.places.entrySet()) {
            sb.append('\n').append('[').append(e.getKey()).append("]\n");      // גם מקום ריק נכתב — כדי שלא תחזור אליו ברירת המחדל
            for (int i = 0; i < e.getValue().size(); i++) {
                sb.append(ROW_BREAK).append('\n');                              // 1.11.0: לפני כל שורה — כך גם שורה ריקה נשמרת
                for (Btn b : e.getValue().get(i)) sb.append(b.line()).append('\n');
            }
        }
        return sb.toString();
    }

    /** הכפתורים של אופן העבודה הפעיל; אין קובץ — ברירת המחדל. */
    static Layout loadAll() {
        File f = file();
        if (!f.isFile()) return defaultsAll();
        try {
            return parseAll(Files.readUtf8(f));
        } catch (Throwable e) {
            Log.e("קריאת " + FILE, e);
            return defaultsAll();
        }
    }

    static boolean save(Layout lay) {
        try {
            Files.writeAtomic(file(), textAll(lay));
            return true;
        } catch (Throwable e) {
            Log.e("שמירת " + FILE, e);
            return false;
        }
    }

    /** 1.13.1 (באג): רשימה שינתה שם או מקום — כפתורים שפותחים אותה עוברים איתה. מחזיר כמה. */
    static int listMoved(List<String> from, List<String> to) {
        if (!file().isFile()) return 0;                                       // ברירת המחדל — בלי רשימות בשם
        Layout lay = loadAll();
        int n = 0;
        for (List<List<Btn>> rows : lay.places.values())
            for (List<Btn> row : rows)
                for (Btn b : row) {
                    String now = Verbs.listMoved(b.act, from, to);
                    if (now != null) {
                        b.act = now;
                        n++;
                    }
                }
        if (n > 0) save(lay);
        return n;
    }

    /** חזרה לברירת מחדל של כל הכפתורים — רק הכפתורים, רק באופן הזה. */
    static boolean reset() {
        File f = file();
        return !f.exists() || f.delete();
    }

    /** חזרה לברירת מחדל של מקום אחד. */
    static void reset(Layout lay, String place) {
        lay.places.put(place, defaults(place));
        anchors(lay);
    }

    /** מתי השתנה הקובץ (0 — אין): כדי שמסך שחזר לקדמה יראה שינוי שנעשה במסך אחר. */
    static long stamp() {
        File f = file();
        return f.isFile() ? f.lastModified() : 0;
    }

    /** הזזה בתוך השורה: dir = -1 — מקום אחד קודם (ימינה, בשורה מימין לשמאל), +1 — אחד אחרי. */
    static boolean move(List<Btn> l, int i, int dir) {
        int j = i + dir;
        if (i < 0 || i >= l.size() || j < 0 || j >= l.size()) return false;
        Btn b = l.remove(i);
        l.add(j, b);
        return true;
    }

    /** אפשר למחוק: לא עוגן — או עוגן שיש לו עוד עותק בשורות הכלליות. */
    static boolean removable(Layout lay, String place, List<Btn> row, int i) {
        if (i < 0 || i >= row.size()) return false;
        Btn b = row.get(i);
        if (!b.anchor()) return true;
        return !general(place) || count(lay, b.act) > 1;
    }

    /** אפשר למחוק שורה: אין בה עוגן שאין לו עותק אחר בשורות הכלליות. */
    static boolean rowRemovable(Layout lay, String place, List<Btn> row) {
        if (!general(place)) return true;
        for (Btn b : row)
            if (b.anchor()) {
                int inRow = 0;
                for (Btn x : row) if (x.act.equals(b.act)) inRow++;
                if (count(lay, b.act) - inRow < 1) return false;
            }
        return true;
    }

    /** העברת כפתור לשורה אחרת (לסופה). עוגן — רק אם נשאר עותק בשורות הכלליות. */
    static boolean moveTo(Layout lay, String fromPlace, List<Btn> from, int i, String toPlace, List<Btn> to) {
        if (i < 0 || i >= from.size() || from == to) return false;
        Btn b = from.get(i);
        if (b.anchor() && general(fromPlace) && !general(toPlace) && count(lay, b.act) < 2) return false;
        from.remove(i);
        to.add(b);
        return true;
    }
}
