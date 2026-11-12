package com.tikiyot.vfm;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// מ0.45 (המשתמש: "בדיקה שנבנית לבד מהגדרת הפקודות: כל פקודה עם כל מתג" — "כן / וגם זה תעשה"): קובץ פקודות שנבנה מההגדרה
// האחת (Commands.ALL): לכל פקודת קבצים — שורה בסיסית, ושורה לכל מתג (ולכל אפשרות של מתג בחירה), בתיקייה זמנית. בסוף — "ודא
// תקין": כל השורות רצו בלי כישלון. פקודה חדשה או מתג חדש בלי דוגמה כאן — בדיקת המחשב נכשלת (ולכן הבדיקה "נבנית לבד").
// תוסף האבחון מקבל את הקובץ מהגשר (commandMatrix) ומריץ אותו כבדיקה "כל הפקודות".
// אחראי: לכתוב את השורות. אסור לו: להריץ (Batch), לשנות את ההגדרה (Commands).
final class CmdMatrix {

    private CmdMatrix() { }

    /** התיקייה הזמנית (בכונן הפנימי). */
    static final String DIR = "%פנימי%\\בדיקת כל הפקודות (אפשר למחוק)";   // בכונן
    /** הרשימה של השורות שעובדות במסד (באופן הבדיקות). */
    static final String LIST = "בדיקת כל הפקודות";   // תחביר

    /**
     * פקודות שאינן בקובץ, ולמה — כל השאר חייבות להיות בו. (אם נוספת פקודה — או שורה כאן, או דוגמה למטה.)
     */
    static final String[][] SKIP = {
            {"MODE", "מחליפה את האופן לכל ההמשך — התוסף כבר עובד בה (\"אופן \\\"בדיקות מערכת\\\"\")"},
            {"CONTINUE", "משנה כמה כישלונות מותר לכל הקובץ — בראש ההרצה של התוסף"},
            {"REPORT", "מעבירה את הדוח לקובץ אחר — והתוסף קורא את שלו"},
            {"CALL", "נכללת בהכנה, לפני שהקבצים נכתבים — נבדקת ב\"בדיקת כלול\""},
            {"START", "פותחת אפליקציה אחרת באמצע הבדיקה"},
            {"SECTION", "מחלקת את הדוח — התוסף שם אותה לפני כל בדיקה"},
            {"BACKUP", "גיבוי ידני שלא נמחק לבד — מצטבר בכל הרצה"},
            {"DROPMODE", "נבדקת בבדיקה הכללית (אופן חדש באמת — נוצר ונמחק)"},
    };

    static boolean skipped(String name) {
        for (String[] x : SKIP) if (x[0].equals(name)) return true;
        return false;
    }

    /** הקובץ. db — גם השורות של המסד (במחשב — בלי). */
    static String text(boolean db) {
        return text(db, new HashSet<String>());
    }

    /** got — לבדיקת המחשב: "COPY", "COPY /S", "COPY /תפוס:לדלג" … — מה שנכתבה לו שורה. */
    static String text(boolean db, Set<String> got) {
        List<String> out = new ArrayList<String>();
        out.add(":: נבנה מהגדרת הפקודות (CmdMatrix): כל פקודה, ובכל אחת — כל מתג. בסוף \"ודא תקין\" — כל השורות רצו בלי כישלון");
        out.add("צור תיקייה \"" + DIR + "\"");
        out.add("מחק תיקייה \"" + DIR + "\" /עמוק");                         // שאריות של הרצה שנעצרה באמצע
        out.add("צור תיקייה \"" + DIR + "\"");
        out.add("צור תיקייה \"" + d("מקור") + "\"");
        out.add("כתוב \"" + d("מקור\\א.txt") + "\" \"אחת\"");
        out.add("כתוב \"" + d("מקור\\ב.txt") + "\" \"שתיים שתיים\"");
        out.add("צור תיקייה \"" + d("מקור\\פנימית") + "\"");
        out.add("כתוב \"" + d("מקור\\פנימית\\ג.txt") + "\" \"שלוש\"");
        out.add("כתוב \"" + d("מקור\\.נסתר.txt") + "\" \"נסתר\"");
        if (db) out.add("צור תיקייה ידנית \"" + LIST + "/א\"");
        if (db) out.add("צור תיקייה ידנית \"" + LIST + "/ב\"");
        int[] n = {0};
        for (Commands.Def def : Commands.ALL) {
            if (def.family != Commands.Family.FILE || skipped(def.name)) continue;
            out.add(":: " + Commands.word(def));
            cases(def, out, n, db, got);
        }
        out.add(":: סוף — התיקייה הזמנית לסל, ומהסל לצמיתות");
        out.add("מחק תיקייה \"" + DIR + "\" /עמוק");
        out.add("רוקן סל \"" + DIR + "\"");
        out.add("ודא \"" + DIR + "\" חסר");
        out.add("ודא \"" + DIR + "\" /בסל:0");
        out.add("ודא תקין");
        StringBuilder sb = new StringBuilder();
        for (String l : out) sb.append(l).append('\n');
        return sb.toString();
    }

    /** גיבוי קטן ("נוכחי" ו"יומן") ליד קובץ ההרצה — ל־/מצב:, שבודק בהכנה שהמקור גיבוי. fixtures — יוצר אותו. */
    static final String FIXTURE = "גיבוי לבדיקה";   // בכונן

    static void fixtures(File here, Library lib) throws java.io.IOException {
        File cur = new File(new File(here, FIXTURE), State.CURRENT);
        if (!cur.isDirectory() && !cur.mkdirs()) throw new java.io.IOException(cur.getPath());
        File j = new File(new File(here, FIXTURE), State.JOURNAL);
        if (!j.isDirectory() && !j.mkdirs()) throw new java.io.IOException(j.getPath());
        File a = new File(cur, "א.txt");
        if (!a.isFile()) {
            Files.writeAtomic(a, "גיבוי");
            if (lib != null) lib.fileAdded(a);
        }
        if (lib != null) lib.flush(5000);
    }

    /** תיקייה שכבר קיימת בהכנה (ההשוואה מחושבת בהכנה): התיקייה של קובץ ההרצה — בתוסף, תיקיית הנתונים שלו. */
    static final String HERE = "%" + Batch.V_HERE + "%";

    private static String d(String rel) {
        return DIR + "\\" + rel;
    }

    /** השורות של פקודה אחת: בלי מתגים, ואז כל מתג (בחירה — כל אפשרות). */
    private static void cases(Commands.Def def, List<String> out, int[] n, boolean db, Set<String> got) {
        if (add(def, null, null, out, n, db)) got.add(def.name);
        for (Commands.Param p : def.switches()) {
            if (p.type == Commands.Type.CHOICE) {
                for (String o : p.options) if (add(def, p, o, out, n, db)) got.add(def.name + " " + p.key + o);
            } else if (add(def, p, null, out, n, db)) got.add(def.name + " " + p.key);
        }
    }

    /** שורה אחת (עם שורות ההכנה שלה). false — לא נכתבה (של המסד, כשבלי מסד; או בלי דוגמה). */
    private static boolean add(Commands.Def def, Commands.Param p, String option, List<String> out, int[] n, boolean db) {
        int k = ++n[0];
        String key = p == null ? null : p.key;
        String sw = p == null ? "" : " " + sample(p, option, k);
        String name = def.name;
        if (key != null && key.equals(Batch.SW_LIST) && !db) return false;    // COMP /רשימה: — במסד
        if (sw.contains("⚠")) return false;
        if (name.equals("COPY") || name.equals("MOVE")) {
            boolean copy = name.equals("COPY");
            String word = Commands.word(def);
            String dst = d((copy ? "העתק\\" : "הועבר\\") + k);
            String src = copy ? d("מקור\\*.txt") : d("להעברה\\" + k + "\\*.txt");
            if (!copy) {
                out.add("צור תיקייה \"" + d("להעברה\\" + k) + "\"");
                out.add("כתוב \"" + d("להעברה\\" + k + "\\א.txt") + "\" \"להעברה\"");
                out.add("כתוב \"" + d("להעברה\\" + k + "\\ב.txt") + "\" \"להעברה שתיים\"");
                out.add("כתוב \"" + d("להעברה\\" + k + "\\.נסתר.txt") + "\" \"נסתר\"");
            }
            out.add("צור תיקייה \"" + dst + "\"");
            if (Batch.SW_TAKEN.equals(key) && !Batch.TAKEN_ASK.equals(option) && !Batch.TAKEN_STOP.equals(option))   // שם תפוס — באמת
                out.add("כתוב \"" + dst + "\\א.txt\" \"היה ביעד\"");
            if (Batch.SW_STATE.equals(key)) src = HERE + "\\" + FIXTURE;          // גיבוי נבדק בהכנה — מול הכונן
            if ("/S".equals(key)) src = copy ? d("מקור") : d("להעברה\\" + k);
            if (Batch.SW_LIMIT.equals(key)) {                                    // אחרי השוואה בלבד
                out.add("השווה \"" + HERE + "\" \"" + dst + "\"");
                out.add(word + " %" + Batch.V_ONLY_A + "% \"" + dst + "\"" + sw);
                return true;
            }
            out.add(word + " \"" + src + "\" \"" + dst + "\"" + sw);
        } else if (name.equals("DEL")) {
            out.add("צור תיקייה \"" + d("למחיקה\\" + k) + "\"");
            out.add("כתוב \"" + d("למחיקה\\" + k + "\\א.txt") + "\" \"למחיקה\"");
            out.add("כתוב \"" + d("למחיקה\\" + k + "\\.נסתר.txt") + "\" \"נסתר\"");
            out.add(Commands.word(def) + " \"" + d("למחיקה\\" + k + "\\*.txt") + "\"" + sw);
        } else if (name.equals("REN")) {
            out.add("כתוב \"" + d("לשינוי " + k + ".txt") + "\" \"x\"");
            out.add(Commands.word(def) + " \"" + d("לשינוי " + k + ".txt") + "\" \"שונה " + k + ".txt\"" + sw);
        } else if (name.equals("MD")) {
            out.add(Commands.word(def) + " \"" + d("תיקייה " + k) + "\"" + sw);
        } else if (name.equals("RD")) {
            out.add("צור תיקייה \"" + d("להסרה " + k) + "\"");
            if (key != null) out.add("כתוב \"" + d("להסרה " + k + "\\א.txt") + "\" \"x\"");
            out.add(Commands.word(def) + " \"" + d("להסרה " + k) + "\"" + sw);
        } else if (name.equals("COMP")) {                                        // ההשוואה מחושבת בהכנה — מול מה שכבר בכונן
            if (Batch.SW_LIST.equals(key)) out.add(Commands.word(def) + " " + Batch.SW_LIST + "\"" + LIST + "/א\" \"" + HERE + "\"");
            else out.add(Commands.word(def) + " \"" + HERE + "\" \"" + d("העתק\\1") + "\"" + sw);
        } else if (name.equals(Batch.LINK)) {
            if (!db) return false;
            out.add(Commands.word(def) + " ידנית \"" + LIST + "/א\" ידנית \"" + LIST + "/ב\"" + sw);
        } else if (name.equals(Batch.VERIFY)) {
            out.add(Commands.word(def) + " \"" + d("מקור\\א.txt") + "\" קיים" + sw);
            if (db) out.add(Commands.word(def) + " ידנית \"" + LIST + "/א\" קיימת");
        } else if (name.equals(Batch.WRITE)) {
            out.add(Commands.word(def) + " \"" + d("כתוב " + k + ".txt") + "\" \"שורה\"" + sw);
        } else if (name.equals(Batch.WAIT)) {
            out.add(Commands.word(def) + " 0" + sw);
        } else if (name.equals(Batch.REFRESH)) {
            out.add(Commands.word(def) + sw);
        } else if (name.equals("ECHO") || name.equals("REM")) {
            out.add(Commands.word(def) + " בדיקת כל הפקודות" + sw);
        } else if (name.equals(Batch.UNTRASH)) {
            out.add("כתוב \"" + d("לסל " + k + ".txt") + "\" \"x\"");
            out.add("מחק \"" + d("לסל " + k + ".txt") + "\"");
            out.add(Commands.word(def) + " \"" + d("לסל " + k + ".txt") + "\"" + sw);
            out.add("ודא \"" + d("לסל " + k + ".txt") + "\" קיים");
        } else if (name.equals(Batch.PURGE)) {
            out.add("כתוב \"" + d("לסל " + k + ".txt") + "\" \"x\"");
            out.add("מחק \"" + d("לסל " + k + ".txt") + "\"");
            out.add(Commands.word(def) + " \"" + d("לסל " + k + ".txt") + "\"" + sw);
            out.add("ודא \"" + d("לסל " + k + ".txt") + "\" /בסל:0");
        } else {
            return false;                                                        // אין דוגמה — בדיקת המחשב נכשלת על זה
        }
        return true;
    }

    /** הערך של מתג — לפי המתג (הדוגמאות שעובדות על הקבצים של התיקייה הזמנית). */
    static String sample(Commands.Param p, String option, int k) {
        String key = Commands.switchWord(p.key);
        switch (p.type) {
            case FLAG: return key;
            case CHOICE: return key + option;
            default: break;
        }
        String v = SAMPLES.get(p.key);
        return v == null ? key + "⚠" : key + v.replace("#", String.valueOf(k));
    }

    /** דוגמה לכל מתג עם ערך (המפתח — כמו בהגדרה). */
    static final java.util.Map<String, String> SAMPLES = new java.util.LinkedHashMap<String, String>();

    static {
        SAMPLES.put(Batch.SW_JOURNAL, "\"" + DIR + "\\יומן #.txt\"");
        SAMPLES.put(Batch.SW_LIMIT, "100%");
        SAMPLES.put(Batch.SW_STATE, "31.12.2099");
        SAMPLES.put("/" + Batch.SW_DATE, "שבוע");
        SAMPLES.put("/" + Batch.SW_SIZE, "<1MB");
        SAMPLES.put("/" + Batch.SW_CONTAINS, "\"אחת\"");
        SAMPLES.put("/D:", "1-1-2000");
        SAMPLES.put("/MAXAGE:", "7");
        SAMPLES.put("/MINAGE:", "0");
        SAMPLES.put("/MAX:", "1000000");
        SAMPLES.put("/MIN:", "1");
        SAMPLES.put(Batch.SW_LIST, "\"" + LIST + "/א\"");
    }

    /** המתגים שכל פקודה בקובץ צריכה (לבדיקת המחשב): "COPY /S" … */
    static Set<String> needed() {
        Set<String> out = new HashSet<String>();
        for (Commands.Def def : Commands.ALL) {
            if (def.family != Commands.Family.FILE || skipped(def.name)) continue;
            out.add(def.name);
            for (Commands.Param p : def.switches())
                if (p.type == Commands.Type.CHOICE) for (String o : p.options) out.add(def.name + " " + p.key + o);
                else out.add(def.name + " " + p.key);
        }
        return out;
    }
}
