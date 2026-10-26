package com.tikiyot.vfm;

// 1.17.0: "שמור לתוכנה" — רשימות שהתוכנה משתמשת בהן (המילון, `claude/milon.md`, 23). המשתמש: "באופן עבודה דוגמאות
// תהיה תיקייה, שכל מה שבה יהיה שמור לתוכנה — בשלב הזה פקודות; ושם אפשר יהיה לערוך ולסדר את ההיררכיה כפי הרצוי";
// "אם יש רשימה כזו באופן עבודה אחר — ייקח ממנה; אם לא — ייקח מדוגמאות"; "וכאשר מחזירים לברירת מחדל — מתקנים רק את
// הרשימות של התוכנה; אם הוסף שם — נשאר"; "כל התפריטים אפשר יהיה לקבוע ברשימות, וכל עדכון יהיה שם — עם ברירות מחדל".
// ברירת המחדל: שמור לתוכנה / פקודות — פקודות המערכת (קובצי פקודות בתיקייה משותפת, שהתוכנה כותבת — הן לא משתנות),
// "פקודה חדשה" (`צור פקודה`), "הפקודות שלי" (תת־רשימה), ו"כל הפקודות במכשיר" (רשימה חכמה).
// אחראי: הרשימות של ברירת המחדל — לוודא, לתקן, ולמצוא באיזה אופן הן. בלי ממשק (התפריט — CmdMenu).

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
     * מוודא את רשימות ברירת המחדל באופן אחד (בזיכרון): מה שחסר — חוזר; מה שהמשתמש הוסיף — נשאר.
     * 1.16.1 → 1.17.0: רשימה "הפקודות שלי" בשורש — עוברת לתוך שמור לתוכנה / פקודות. מחזיר true אם השתנה משהו.
     */
    static boolean ensure(ModeCopy.Side side) {
        boolean changed = false;
        Tree t = side.tree;
        Tree.Node root = t.root.child(ROOT);
        if (root == null) {
            root = Tree.newChild(t.root, ROOT);
            root.icon = "⚙";
            changed = true;
        }
        Tree.Node cmds = root.child(CMDS);
        if (cmds == null) {
            cmds = Tree.newChild(root, CMDS);
            cmds.icon = "⭐";
            changed = true;
        }
        List<File> sys = writeSystem();
        for (int i = 0; i < sys.size(); i++) {                                // פקודות המערכת — ראשונות, ו"פקודה חדשה" אחריהן
            String p = sys.get(i).getAbsolutePath();
            if (Tree.contains(cmds.entries, p)) continue;
            cmds.entries.add(Math.min(i, cmds.entries.size()), new Tree.Entry(p, sys.get(i).length(), false));
            changed = true;
        }
        if (root.child(BACKUP) == null) {                                     // 1.22.3
            Tree.newChild(root, BACKUP).icon = "💾";
            changed = true;
        }
        Tree.Node mine = cmds.child(MINE);
        if (mine == null) {
            mine = Tree.newChild(cmds, MINE);
            mine.icon = "⭐";
            changed = true;
        }
        Tree.Node old = t.root.child(MINE);                                   // 1.16.1: "הפקודות שלי" בשורש
        if (old != null && old != mine) {
            for (Tree.Entry e : old.entries) if (!Tree.contains(mine.entries, e.path)) mine.entries.add(e);
            for (Tree.Node c : old.children) {
                c.name = mine.uniqueChildName(c.name, null);
                c.parent = mine;
                mine.children.add(c);
            }
            t.root.children.remove(old);
            changed = true;
        }
        if (allOf(side, cmds) == null) {
            cmds.smart.add(newAll(side.ini));
            changed = true;
        }
        if (!hasTool(side.ini)) {                                             // באג (1.17.0): בלי כלי בבית — אין דרך להגיע אליה
            String h = Structure.FUNC + ROOT;
            List<Ini.Line> b = new ArrayList<Ini.Line>();
            b.add(Ini.line("", h));
            b.add(Ini.line("[" + h + "]", h));
            b.add(Ini.line(Structure.ID + " = " + Structure.newId("f"), h));
            b.add(Ini.line("סוג = " + FnVirtual.KIND, h));  // תחביר
            b.add(Ini.line(FnVirtual.LIST + " = " + ROOT, h));
            side.ini.lines.addAll(Structure.insertAt(side.ini), b);
            changed = true;
        }
        return changed;
    }

    /** האם יש בבית כלי של "שמור לתוכנה". */
    static boolean hasTool(Ini ini) {
        for (Ini.Line l : ini.lines) if (l.key != null && l.key.equals(FnVirtual.LIST) && ROOT.equals(l.value)) return true;
        return false;
    }

    /** "כל הפקודות במכשיר" — המזהה שלה ברשימה, או null. */
    static String allOf(ModeCopy.Side side, Tree.Node cmds) {
        for (String r : cmds.smart) {
            Structure.Func f = Structure.byId(side.ini, r);
            if (f != null && f.name.endsWith(ALL)) return r;                   // גם אחרי העתקה (הכותרת בלי הבעלים)
        }
        return null;
    }

    private static String newAll(Ini ini) {
        String id = Structure.newId("p"), h = Nested.INNER + ROOT + " › " + CMDS + " › " + ALL;
        List<Ini.Line> b = new ArrayList<Ini.Line>();
        b.add(Ini.line("", h));
        b.add(Ini.line("[" + h + "]", h));
        b.add(Ini.line(Structure.ID + " = " + id, h));
        b.add(Ini.line("סוג = " + FnSmart.KIND, h));  // תחביר
        b.add(Ini.line(SmartLists.COND + " = " + Cond.TYPE + ":" + Batch.EXT, h));
        b.add(Ini.line(SmartLists.SORT + " = " + Sort.NAME, h));
        ini.lines.addAll(Structure.insertAt(ini), b);
        return id;
    }

    // ---------- בכונן ----------

    /** באיזה אופן הרשימות: הפעיל, אם יש בו "שמור לתוכנה"; אחרת — דוגמאות (אם קיים); אחרת — null. */
    static String modeInUse() {
        if (Tree.load().find(ROOT_PATH) != null) return Modes.current();
        File ex = ModeCopy.treeOf(Examples.MODE);
        return ex.isFile() && Tree.loadFrom(ex).find(ROOT_PATH) != null ? Examples.MODE : null;
    }

    /** הרשימות של האופן שבשימוש (לקריאה). null — אין. */
    static ModeCopy.Side inUse() {
        String m = modeInUse();
        if (m == null) return null;
        if (m.equals(Modes.current())) return new ModeCopy.Side(Ini.load(Structure.file()), Tree.load());
        return ModeCopy.read(m);
    }

    /** מוודא באופן mode, בקבצים שלו (בפתיחת התוכנה — לדוגמאות; ואחרי חזרה לברירת מחדל). */
    static void ensureMode(String mode) {
        try {
            synchronized (Structure.class) {
                synchronized (Tree.LOCK) {
                    File main = ModeCopy.mainOf(mode), tf = ModeCopy.treeOf(mode);
                    if (!main.isFile() || !tf.isFile()) return;
                    ModeCopy.Side s = new ModeCopy.Side(Ini.load(main), Tree.loadFrom(tf));
                    if (s.tree.unreadable) return;
                    if (ensure(s) && s.ini.saveQuiet()) {
                        s.tree.saveTo(tf);
                        if (mode.equals(Modes.current())) Mirror.rebuild();     // באג (1.17.4): העותק הקריא — גם הכלי החדש
                    }
                }
            }
        } catch (Throwable t) {
            Log.e("שמור לתוכנה — " + mode, t);
        }
    }

    /** 1.22.3: "גיבוי" גם בתוך "שמור לתוכנה" של האופן הזה, אם יש בו (בלי ליצור "שמור לתוכנה" חדש). */
    static void ensureBackup(String mode) {
        try {
            synchronized (Tree.LOCK) {
                File tf = mode.equals(Modes.current()) ? Tree.file() : ModeCopy.treeOf(mode);
                if (!tf.isFile()) return;
                Tree t = Tree.loadFrom(tf);
                Tree.Node root = t.root.child(ROOT);
                if (t.unreadable || root == null || root.child(BACKUP) != null) return;
                Tree.newChild(root, BACKUP).icon = "💾";
                t.saveTo(tf);
            }
        } catch (Throwable t) {
            Log.e("שמור לתוכנה — גיבוי", t);
        }
    }

    /** פקודה ל"הפקודות שלי" — באופן שבשימוש. */
    static void addToMine(final File f) {
        String m = modeInUse();
        if (m == null) return;
        synchronized (Tree.LOCK) {
            File tf = m.equals(Modes.current()) ? Tree.file() : ModeCopy.treeOf(m);
            Tree t = Tree.loadFrom(tf);
            Tree.Node n = t.find(MINE_PATH);
            if (t.unreadable || n == null || Tree.contains(n.entries, f.getAbsolutePath())) return;
            n.entries.add(new Tree.Entry(f.getAbsolutePath(), f.length(), false));
            t.saveTo(tf);
        }
    }
}
