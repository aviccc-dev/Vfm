package com.tikiyot.vfm;

// 1.17.0: "שמור לתוכנה" — רשימות שהתוכנה משתמשת בהן (המילון, `claude/milon.md`, 23). המשתמש: "באופן עבודה דוגמאות
// תהיה תיקייה, שכל מה שבה יהיה שמור לתוכנה — בשלב הזה פקודות; ושם אפשר יהיה לערוך ולסדר את ההיררכיה כפי הרצוי";
// "אם יש רשימה כזו באופן עבודה אחר — ייקח ממנה; אם לא — ייקח מדוגמאות"; "וכאשר מחזירים לברירת מחדל — מתקנים רק את
// הרשימות של התוכנה; אם הוסף שם — נשאר"; "כל התפריטים אפשר יהיה לקבוע ברשימות, וכל עדכון יהיה שם — עם ברירות מחדל".
// ברירת המחדל: שמור לתוכנה / פקודות — פקודות המערכת (קובצי פקודות בתיקייה משותפת, שהתוכנה כותבת — הן לא משתנות),
// "פקודה חדשה" (`צור פקודה`), "הפקודות שלי" (תת־רשימה), ו"כל הפקודות במכשיר" (רשימה חכמה).
// מ0.29: הרשימות עצמן — במסד, ומגיעות מקובץ הדוגמאות (DbExamples); כאן נשארו השמות, קובצי פקודות המערכת בכונן, ו"הפקודות שלי".
// אחראי: השמות, קובצי פקודות המערכת, והוספה ל"הפקודות שלי". בלי ממשק (התפריט — CmdMenu, DbCmdMenu).

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

final class Reserved {

    static final String ROOT = "שמור לתוכנה", CMDS = "פקודות", MINE = "הפקודות שלי",  // בכונן
            ALL = "כל הפקודות במכשיר", NEW = "פקודה חדשה", SYS_DIR = "פקודות המערכת";  // בכונן
    /** 1.22.3: "שמור לתוכנה / גיבוי" — כל מה שבה נכנס לגיבוי (המשתמש: "הכי פשוט — בתוך שמור לתוכנה תיקייה גיבוי, וכל מה ששם נכנס לגיבוי"). */
    static final String BACKUP = "גיבוי";  // בכונן
    static final List<String> ROOT_PATH = Collections.singletonList(ROOT);
    static final List<String> CMDS_PATH = Arrays.asList(ROOT, CMDS);
    static final List<String> MINE_PATH = Arrays.asList(ROOT, CMDS, MINE);

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
     * פקודה חדשה נכנסת ל"הפקודות שלי" — במסד של האופן הפתוח, ואם אין שם רשימה כזו — באופן שיש בו (קודם הדוגמאות).
     * מ0.29: במסד (עד כאן — בעץ של המנוע הישן, ולכן פקודה חדשה לא הופיעה ב"הפקודות שלי" של המסד).
     */
    static void addToMine(final File f) {
        android.content.Context c = DbListScreen.app;
        String cur = DbListScreen.currentMode;
        if (c == null || cur == null || f == null) return;
        List<String> modes = new ArrayList<String>();
        modes.add(cur);
        List<String> all = DbScreen.dbModes(c);
        if (all.remove(DbExamples.MODE)) all.add(0, DbExamples.MODE);
        for (String m : all) if (!modes.contains(m)) modes.add(m);
        for (String m : modes) {
            if (!DbScreen.dbFile(c, m).isFile()) continue;
            SqlStore s = null;
            try {
                s = SqlStore.open(DbScreen.dbFile(c, m), new IdCounter(DbScreen.counterFile()));
                long mine = DbPaths.find(s, m, MINE_PATH);
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
