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

    static final String ROOT = "שמור לתוכנה", CMDS = "פקודות", MINE = "הפקודות שלי",  // בכונן
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
        List<String[]> all = new ArrayList<String[]>(CmdMenu.system());
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

    /**
     * פקודה חדשה נכנסת ל"הפקודות שלי": של האופן הפתוח, אם יש לו "שמור לתוכנה" משלו; אחרת — של אופן התפריטים (מ0.41;
     * עד כאן — של אופן אחר שיש בו, קודם הדוגמאות).
     */
    static void addToMine(final File f) {
        android.content.Context c = DbListScreen.app;
        if (c == null || f == null) return;
        Menus.ensure(c);
        for (String m : new String[]{DbListScreen.currentMode, Menus.MODE}) {
            if (m == null || !DbScreen.dbFile(c, m).isFile()) continue;
            SqlStore s = null;
            try {
                s = SqlStore.open(DbScreen.dbFile(c, m), new IdCounter(DbScreen.counterFile()));
                long mine = Menus.at(s, Menus.root(s, m), Menus.MINE);
                if (mine == 0) continue;
                DbFiles.add(s, mine, Collections.singletonList(f.getAbsolutePath()), null, false);
                return;
            } catch (Throwable t) {
                Log.e("Reserved.addToMine", t);
                return;
            } finally {
                if (s != null) s.close();
            }
        }
    }
}
