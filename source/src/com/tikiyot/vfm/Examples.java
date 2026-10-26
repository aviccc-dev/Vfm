package com.tikiyot.vfm;

// אופן העבודה "דוגמאות" (156; קבצים מ־157) — נכתב בהתקנה, כדי שיהיה קל לבדוק דברים ולקחת רעיונות.
// הדוגמאות עצמן — קובצי טקסט רגילים בתוך קובץ ההתקנה: assets/examples/main.ini (הקובץ הראשי)
// ו-assets/examples/tree.txt (הרשימות הידניות). כדי לשנות דוגמאות — מחליפים את הקבצים, בלי לגעת בקוד.
// "%פנימי%" בקבצים — מוחלף בשורש הזיכרון הפנימי. ניתן לעריכה; "החזרה למצב ההתחלתי" — כותב מחדש;
// נמחק — לא חוזר לבד (סימון בתיקיית האופנים). בלי ממשק — נבדק. הממשק: Home (התקנה בפתיחה, תפריט האופנים).

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.List;

final class Examples {

    static final String MODE = "דוגמאות";  // בכונן
    /** בתיקיית האופנים: הדוגמאות כבר הותקנו פעם אחת (מחיקה — לא מחזירה אותן לבד). */
    static final String MARK = ".דוגמאות הותקנו";                          // בכונן
    /** הקבצים בתוך קובץ ההתקנה (שמות לטיניים — כך בטוח בכל מכשיר). */
    static final String ASSET_MAIN = "examples/main.ini", ASSET_TREE = "examples/tree.txt", ASSET_DEMO = "examples/demo.txt";
    /** 1.10.0: תיקיית ההדמיה של דוגמאות הפקודות — בתוך תיקיית התוכנה; נבנית מחדש בכל "חזרה לברירת מחדל". */
    static final String DEMO = "דוגמאות לפקודות";  // בכונן
    private static final String FILE_LINE = "=== ", OLD = " @ישן";  // תחביר
    /** 1.11.0: המקום של המשתמש בתוך אופן הדוגמאות — עובר כמו שהוא ב"חזרה לברירת מחדל" (1.15.0: רשימה, לא סביבה). */
    static final String MINE = "הדוגמאות שלי";  // בכונן
    static final List<String> MINE_PATH = java.util.Collections.singletonList(MINE);
    /** בקבצים: מקום הכונן הפנימי; מוחלף בשורש האמיתי בהתקנה. */
    static final String IN = "%פנימי%";  // תחביר

    private Examples() { }

    /** קובץ מתוך קובץ ההתקנה, כטקסט. */
    static String asset(Context c, String name) throws Exception {
        InputStream in = c.getAssets().open(name);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] b = new byte[8192];
            for (int n; (n = in.read(b)) > 0; ) out.write(b, 0, n);
            return new String(out.toByteArray(), "UTF-8");
        } finally {
            in.close();
        }
    }

    /** מהמכשיר: הקבצים מתוך ההתקנה. */
    static boolean install(Context c, File data, String internal, boolean reset) {
        try {
            return install(data, internal, reset, asset(c, ASSET_MAIN), asset(c, ASSET_TREE), asset(c, ASSET_DEMO));
        } catch (Throwable t) {
            Log.e("קריאת הדוגמאות מההתקנה", t);
            return false;
        }
    }

    /**
     * מתקין לתיקיית האופנים (data/אופני עבודה/דוגמאות). reset — כותב מחדש גם אם קיים (הקודם — לגיבוי).
     * בלי reset — רק בפעם הראשונה; אחרי שנמחק — לא. מחזיר true אם נכתב.
     */
    static boolean install(File data, String internal, boolean reset, String main, String tree, String demo) {
        File modes = new File(data, Modes.DIR);
        File dir = new File(modes, MODE), mark = new File(modes, MARK);
        if (!reset && (mark.exists() || dir.exists())) return false;
        try {
            File settings = new File(dir, Paths.SETTINGS), mainFile = new File(settings, Paths.MAIN), treeFile = new File(dir, Paths.TREE);
            ModeCopy.Side mine = null;                                        // 1.11.0: "הדוגמאות שלי" — לפני שהאופן הישן יוצא
            if (reset && mainFile.isFile()) {
                ModeCopy.Side old = new ModeCopy.Side(Ini.load(mainFile), Tree.loadFrom(treeFile));
                Envs.migrate(old.ini, old.tree, null);                        // 1.15.0: מלפני ההסבה — סביבה ← רשימה
                if (!old.tree.unreadable && (old.tree.find(MINE_PATH) != null || old.tree.find(Reserved.ROOT_PATH) != null)) mine = old;
            }
            if (reset && dir.exists() && !BinBox.mode(MODE, dir)) {          // 1.10.0: הכול מההתחלה — הקודם לסל
                Files.backup(mainFile, "דוגמאות");  // בכונן
                Files.backup(treeFile, "דוגמאות");  // בכונן
                Trash.wipe(dir);
            }
            settings.mkdirs();
            Files.writeAtomic(mainFile, main.replace(IN, internal));
            Files.writeAtomic(treeFile, tree.replace(IN, internal));
            if (mine != null) keepMine(mine, mainFile, treeFile);              // 1.13.2 (באג): מיד — לפני כל דבר שעלול להיכשל
            if (mine != null && mine.tree.find(Reserved.ROOT_PATH) != null) keepReserved(mine, mainFile, treeFile);
            if (demo != null) {
                try {
                    demo(new File(data, DEMO), demo);
                } catch (Throwable t) {                                       // תיקיית התרגול — לא מבטלת את כל השאר
                    Log.e("תיקיית התרגול של הדוגמאות", t);
                }
            }
            modes.mkdirs();
            if (!mark.exists()) Files.writeAtomic(mark, "1\n");
            ModeCopy.Side un = new ModeCopy.Side(Ini.load(mainFile), Tree.loadFrom(treeFile));
            if (Unify.run(un) && un.ini.saveQuiet()) un.tree.saveTo(treeFile);  // 1.17.3: גם הדוגמאות — מיד, לא רק בפתיחה הבאה
            if (mine == null || mine.tree.find(Reserved.ROOT_PATH) == null) {  // 1.17.0: "שמור לתוכנה" — ברירות המחדל
                ModeCopy.Side now = new ModeCopy.Side(Ini.load(mainFile), Tree.loadFrom(treeFile));
                if (Reserved.ensure(now) && now.ini.saveQuiet()) now.tree.saveTo(treeFile);
            }
            return true;
        } catch (Throwable t) {
            Log.e("התקנת הדוגמאות", t);
            return false;
        }
    }

    /**
     * הרשימה "הדוגמאות שלי" — מהאופן הישן, עם כל מה שבה, במקום הריקה החדשה (1.15.0: רשימה כסביבת עבודה; עד 1.14 —
     * סביבה). מה שקיים גם בדוגמאות החדשות — נשאר קישור; מה שרק אצלך — מועתק.
     * 1.13.2 (באג — המשתמש: "הכל נעלם, גם מה שהעתקתי מלפי מושג וגם מה שהוספתי בעצמי; הסביבה הייתה ריקה"):
     * ההעתקה לא הצליחה — עד כאן נשארה הריקה בשקט. עכשיו — גיבוי: הרשימה כמו שהיא (הקישורים כמו שהם).
     */
    static void keepMine(ModeCopy.Side old, File mainFile, File treeFile) {
        try {
            ModeCopy.Side now = new ModeCopy.Side(Ini.load(mainFile), Tree.loadFrom(treeFile));
            if (ModeCopy.keep(old, MINE_PATH, now) && now.tree.saveTo(treeFile) && now.ini.saveQuiet()) return;
            Log.i("הדוגמאות שלי — לא הועתקה; עוברת כמו שהיא");  // בכונן
        } catch (Throwable t) {
            Log.e("הדוגמאות שלי — העתקה", t);
        }
        try {
            ModeCopy.Side now = new ModeCopy.Side(Ini.load(mainFile), Tree.loadFrom(treeFile));   // מההתחלה — בלי חצאי העתקה
            Tree.Node from = old.tree.find(MINE_PATH), to = now.tree.find(MINE_PATH);
            if (from == null) return;
            if (to == null) to = Tree.newChild(now.tree.root, MINE);
            Tree.copyNode(from, to);
            now.tree.saveTo(treeFile);
        } catch (Throwable t) {
            Log.e("הדוגמאות שלי — גיבוי", t);
        }
    }

    /**
     * 1.17.0: "שמור לתוכנה" — מה שהמשתמש הוסיף שם נשאר (המשתמש: "כאשר מחזירים לברירת מחדל — מתקנים רק את הרשימות של
     * התוכנה; אם הוסף שם — נשאר"); ואחר כך ברירות המחדל — מה שחסר בהן חוזר.
     */
    static void keepReserved(ModeCopy.Side old, File mainFile, File treeFile) {
        try {
            ModeCopy.Side now = new ModeCopy.Side(Ini.load(mainFile), Tree.loadFrom(treeFile));
            ModeCopy.keep(old, Reserved.ROOT_PATH, now);
            Reserved.ensure(now);
            if (now.ini.saveQuiet()) now.tree.saveTo(treeFile);
        } catch (Throwable t) {
            Log.e("שמור לתוכנה — חזרה לברירת מחדל", t);
        }
    }

    /** 1.10.0: בונה מחדש את תיקיית ההדמיה (מה שהיה בה — נמחק; היא שלנו). */
    static void demo(File dir, String spec) throws java.io.IOException {
        if (dir.exists()) Trash.wipe(dir);
        File cur = null;
        boolean old = false;
        StringBuilder body = new StringBuilder();
        for (String line : (spec + "\n" + FILE_LINE).replace("\r", "").split("\n")) {
            if (!line.startsWith(FILE_LINE)) {
                if (cur != null) body.append(line).append('\n');
                continue;
            }
            if (cur != null) {
                cur.getParentFile().mkdirs();
                Files.writeAtomic(cur, body.toString());
                if (old) cur.setLastModified(System.currentTimeMillis() - 730L * 24 * 3600 * 1000);
            }
            String p = line.substring(FILE_LINE.length()).trim();
            old = p.endsWith(OLD.trim());
            if (old) p = p.substring(0, p.length() - OLD.trim().length()).trim();
            cur = p.isEmpty() ? null : new File(dir, p);
            body.setLength(0);
        }
        Library lib = Library.peek();
        if (lib != null) lib.dirChanged(dir.getParent());
    }
}
