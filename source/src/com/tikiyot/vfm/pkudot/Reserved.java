package com.tikiyot.vfm;

// 1.17.0: "שמור לתוכנה" — רשימות שהתוכנה משתמשת בהן (המילון, `claude/milon.md`, 23). המשתמש: "באופן עבודה דוגמאות
// תהיה תיקייה, שכל מה שבה יהיה שמור לתוכנה — בשלב הזה פקודות; ושם אפשר יהיה לערוך ולסדר את ההיררכיה כפי הרצוי";
// "אם יש רשימה כזו באופן עבודה אחר — ייקח ממנה; אם לא — ייקח מדוגמאות"; "וכאשר מחזירים לברירת מחדל — מתקנים רק את
// הרשימות של התוכנה; אם הוסף שם — נשאר"; "כל התפריטים אפשר יהיה לקבוע ברשימות, וכל עדכון יהיה שם — עם ברירות מחדל".
// ברירת המחדל: שמור לתוכנה / פקודות — פקודות המערכת (קובצי פקודות בתיקייה משותפת, שהתוכנה כותבת — הן לא משתנות),
// "פקודה חדשה" (`צור פקודה`), "הפקודות שלי" (תת־רשימה), ו"כל הפקודות במכשיר" (רשימה חכמה).
// מ0.29: הרשימות עצמן — במסד. מ0.41: באופן "שמור לתוכנה", מקובץ ברירת המחדל (Menus, assets/examples/shamur.txt);
// כאן נשארו השמות, קובצי פקודות המערכת בכונן, וההוספה ל"הפקודות שלי".
// אחראי: השמות, קובצי פקודות המערכת, והוספה ל"הפקודות שלי". בלי ממשק (מה בתפריטים — Menus; הציור — DbCmdMenu).

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

final class Reserved {

    static final String ROOT = Names.RESERVED_ROOT, CMDS = "פקודות", MINE = "הפקודות שלי",  // בכונן
            NEW = "פקודה חדשה", SYS_DIR = "פקודות המערכת";  // בכונן
    static final List<String> ROOT_PATH = Collections.singletonList(ROOT);
    static final List<String> CMDS_PATH = Arrays.asList(ROOT, CMDS);

    /** בבדיקות — תיקייה אחרת לפקודות המערכת. */
    static File testDir;

    private Reserved() { }

    /** התיקייה המשותפת של פקודות המערכת (לכל האופנים — הן אותן פקודות). */
    static File sysDir() {
        return testDir != null ? testDir : new File(Paths.data(), SYS_DIR);
    }

    /** פקודות המערכת והקובץ "פקודה חדשה" — נכתבים כמו שהם (מה שהשתנה — חוזר). מחזיר את הקבצים, לפי הסדר. */
    static List<File> writeSystem() {
        List<File> out = new ArrayList<File>();
        List<String[]> all = new ArrayList<String[]>(system());
        all.add(new String[]{"➕", NEW, Verbs.CREATE + " " + Verbs.CMD_WORD});
        File dir = sysDir();
        for (String[] s : all) {
            File f = new File(dir, s[1] + "." + Batch.EXT);
            String text = Program.ICON + " " + s[0] + "\n" + Program.NAME + " " + s[1] + "\n" + s[2] + "\n";
            try {
                if (!f.isFile() || !text.equals(Files.readUtf8(f))) {
                    if (!dir.isDirectory()) dir.mkdirs();
                    Files.writeAtomic(f, text);
                }
            } catch (Throwable e) {
                Log.e("פקודות המערכת", e);
            }
            out.add(f);
        }
        return out;
    }

    static boolean isSystem(File f) {
        return f != null && f.getParentFile() != null && f.getParentFile().equals(sysDir());
    }

    static boolean isNew(File f) {
        return isSystem(f) && f.getName().equals(NEW + "." + Batch.EXT);
    }

    // ---------- מבנה 12: רשימות פקודות המערכת — עד כאן בתוך התפריטים (CmdMenu.system, DbCmdMenu.dbTools / writeTools) ----------

    /** פקודות המערכת — קבוצה 1: פתיחת מסכים (המשתמש: "יוצגו גם בתור טקסט לעריכה"). {צלמית, שם, שורה}. */
    static List<String[]> system() {
        return Arrays.asList(
                new String[]{"🏠", Words.Top.HOME, Verbs.openScreen(Verbs.S_HOME)},
                new String[]{"→", Words.Top.BACK, Verbs.BACK},
                new String[]{"🔎", Words.Top.SEARCH, Verbs.openScreen(Verbs.S_SEARCH)},
                new String[]{"⭐", Words.Top.COMMANDS, Verbs.openScreen(Verbs.S_COMMANDS)},
                new String[]{"📂", Words.Top.FILES, Verbs.openScreen(Verbs.S_DISK)},
                new String[]{"🗂", Words.Top.VIRTUAL, Verbs.openScreen(Verbs.S_VIRTUAL)},
                new String[]{"✎", Words.Top.EDITOR, Verbs.OPEN + " " + Verbs.S_EDITOR},       // 1.28.0
                new String[]{"⚙", Words.Top.SETTINGS, Verbs.openScreen(Verbs.S_SETTINGS)},
                new String[]{"🗑", Words.Top.TRASH, Verbs.openScreen(Verbs.S_TRASH)});
    }

    /** [צלמית, שם, שורת הפקודה] — המקומות הפתוחים נשאלים בהרצה (הטופס — עם בורר הרשימות של המסד). */
    static List<String[]> dbTools() {
        List<String[]> out = new ArrayList<String[]>();
        out.add(new String[]{"📤", Words.DbCmd.TOOL_EXPORT, "COPY " + ListCmd.MANUAL + " \"<" + Words.DbCmd.HOLE_EXPORT + ">\" "
                + ListCmd.FILE_WORD + " \"*\""});   // תחביר
        out.add(new String[]{"📥", Words.DbCmd.TOOL_IMPORT, "COPY " + ListCmd.FILE_WORD + " \"<" + Words.DbCmd.HOLE_FILE + ">\" "
                + ListCmd.MANUAL + " \"<" + Words.DbCmd.HOLE_INTO + ">\""});   // תחביר
        // מ0.25 (באג 28; המשתמש: "יכולים להישאר בפקודות. ותרשום לך להכניס אותם לשמורים לתוכנה"): הכלים שלא עברו לבית
        // (סל והגדרות — כבר בפקודות המערכת). כפולים — גם כאן, עם ברירות המחדל.
        out.add(new String[]{"⧉", Words.DbCmd.TOOL_DUPS, Verbs.toolLine(Target.KIND_DUP, new java.util.LinkedHashMap<String, String>())});
        out.add(new String[]{"📌", Words.DbCmd.TOOL_BAR, Verbs.toolLine(Target.KIND_BAR, new java.util.LinkedHashMap<String, String>())});   // תחביר
        out.add(new String[]{"🔗", Words.DbCmd.TOOL_APP, Verbs.toolLine(Target.KIND_APP, new java.util.LinkedHashMap<String, String>())});   // תחביר; בלי חבילה — נשאל
        // מ0.41 (המשתמש, 12.11.2026: "להוסיף כפתור שמריץ אותו. וכך הוא הופך להיות חלק אינטגרלי מהתוכנה"): העורך האינטראקטיבי
        out.add(new String[]{"📝", Words.DbCmd.TOOL_EDITOR, Verbs.OPEN + " " + Verbs.PLUGIN + " \"" + Words.DbCmd.TOOL_EDITOR + "\""});
        return out;
    }

    /** הקבצים בתיקיית פקודות המערכת (כמו Reserved.writeSystem: מה שהשתנה — נכתב מחדש). */
    static List<File> writeTools() {
        List<File> out = new ArrayList<File>();
        File dir = sysDir();
        for (String[] t : dbTools()) {
            File f = new File(dir, t[1] + "." + Batch.EXT);
            String text = Program.ICON + " " + t[0] + "\n" + Program.NAME + " " + t[1] + "\n" + t[2] + "\n";
            try {
                if (!f.isFile() || !text.equals(Files.readUtf8(f))) {
                    if (!dir.isDirectory()) dir.mkdirs();
                    Files.writeAtomic(f, text);
                }
                out.add(f);
            } catch (Throwable e) {
                Log.e("Reserved.writeTools", e);
            }
        }
        return out;
    }
}
