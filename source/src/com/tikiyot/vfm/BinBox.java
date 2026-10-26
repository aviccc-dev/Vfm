package com.tikiyot.vfm;

// 1.10.0: סל המחזור למכלים — רשימה, רשימה חכמה, כלי, סביבת עבודה ואופן שהוסרו נשמרים בסל וניתנים לשחזור.
// פריט בודד ברשימה — לא (מוסיפים אותו שוב). כל הסרה = תיקייה אחת בסל ("<זמן> רשימה - קניות"), ובה צילום של
// קובץ המבנה ושל הרשימות כפי שהיו רגע לפני ההסרה. השחזור מעתיק מהצילום חזרה (ModeCopy) — כמו העתקה בין אופנים.
// אופן — התיקייה שלו עצמה עוברת לסל, ובשחזור חוזרת. אחראי: ההכנסה לסל והשחזור. אסור לו: לגעת בקבצים פיזיים.

import android.os.Environment;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

final class BinBox {

    private BinBox() { }

    /** תחילית "המקום המקורי" ביומן הסל — כך יודעים שזו הסרה מהתוכנה ולא קובץ. מסלול אמיתי מתחיל ב־"/". */
    static final String MARK = "📦 ";
    private static final String META = "פרטים.txt", MAIN = "מבנה התוכנה.ini", TREE = "מבנה.txt", MODE_DIR = "אופן";  // בכונן
    private static final String K_MODE = "אופן", K_LIST = "רשימה", K_SMART = "חכמה", K_TOOL = "כלי", K_TILE = "סביבה";  // בכונן

    static boolean isBox(String original) {
        return original != null && original.startsWith(MARK);
    }

    // ---------- הכנסה לסל ----------

    /** רשימות (ידניות) שהוסרו מתוך הרשימה parent. */
    static void lists(List<String> parent, List<String> names) {
        if (names.isEmpty()) return;
        List<String> picks = new ArrayList<String>();
        for (String n : names) {
            List<String> p = new ArrayList<String>(parent);
            p.add(n);
            picks.add(K_LIST + "\t" + join(p));
        }
        put(names.size() == 1 ? Words.Box.LIST : Words.Box.LISTS, names.size() == 1 ? names.get(0) : names.size() + "", picks);
    }

    /** רשימה חכמה פנימית שהוסרה מתוך הרשימה parent. */
    static void smart(List<String> parent, String ref, String label) {
        put(Words.Box.SMART, label, one(K_SMART + "\t" + ref + "\t" + label + "\t" + join(parent)));
    }

    static void tool(Structure.Func f) {
        put(Words.Box.TOOL, f.name, one(K_TOOL + "\t" + f.id + "\t" + f.name));
    }

    /** אופן שלם: התיקייה שלו עוברת לסל. false — לא הצליח (ואז נשאר הדרך הישנה). */
    static boolean mode(String name, File modeDir) {
        File box = open(Words.Box.MODE, name);
        if (box == null) return false;
        if (!modeDir.renameTo(new File(box, MODE_DIR))) {
            Trash.wipe(box);
            return false;
        }
        return close(box, Words.Box.MODE, name, K_MODE + "=" + name + "\n");
    }

    private static List<String> one(String s) {
        List<String> l = new ArrayList<String>();
        l.add(s);
        return l;
    }

    private static String join(List<String> p) {
        return Tree.join(p, Tree.SEP);
    }

    /** תיקייה חדשה בסל של הזיכרון הפנימי, או null (הכונן לא באינדקס). */
    private static File open(String kind, String label) {
        Library lib = Library.peek();
        File root = Environment.getExternalStorageDirectory();
        if (lib == null || lib.rootOf(root.getAbsolutePath()) == null) return null;
        File trash = new File(root, Index.TRASH);
        String stamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(new Date());
        String base = stamp + " " + kind + " - " + label.replaceAll("[/\\\\:*?\"<>|\n\t]", " ").trim();
        File box = new File(trash, base);
        for (int i = 2; box.exists(); i++) box = new File(trash, base + " (" + i + ")");
        return box.mkdirs() ? box : null;
    }

    /** צילום של האופן הפעיל ורשימת מה שהוסר. */
    private static void put(String kind, String label, List<String> picks) {
        File box = open(kind, label);
        if (box == null) return;
        try {
            if (Structure.file().isFile()) Files.copy(Structure.file(), new File(box, MAIN));
            if (Paths.tree().isFile()) Files.copy(Paths.tree(), new File(box, TREE));
        } catch (Throwable t) {
            Log.e("סל: צילום", t);
            Trash.wipe(box);
            return;
        }
        StringBuilder meta = new StringBuilder(K_MODE + "=" + Modes.current() + "\n");
        for (String p : picks) meta.append(p).append('\n');
        close(box, kind, label, meta.toString());
    }

    private static boolean close(File box, String kind, String label, String meta) {
        try {
            Files.writeAtomic(new File(box, META), meta);
        } catch (Throwable t) {
            Log.e("סל: פרטים", t);
        }
        String mode = meta.startsWith(K_MODE + "=") ? meta.substring(K_MODE.length() + 1, meta.indexOf('\n')) : "";
        Trash.logDeleted(box, MARK + kind + ": " + label + (kind.equals(Words.Box.MODE) ? "" : Words.Box.FROM_MODE + mode));
        Library lib = Library.peek();
        if (lib != null) lib.dirChanged(box.getParent());
        Log.i("סל: " + kind + " \"" + label + "\"");
        return true;
    }

    // ---------- שחזור ----------

    /** מחזיר את השם ששוחזר, או null. */
    static String restore(Library lib, Trash.Item it) {
        File box = new File(it.path());
        String meta;
        try {
            meta = Files.readUtf8(new File(box, META));
        } catch (Throwable t) {
            return null;
        }
        String mode = "";
        List<String[]> picks = new ArrayList<String[]>();
        for (String line : meta.split("\n")) {
            if (line.startsWith(K_MODE + "=")) mode = line.substring(K_MODE.length() + 1).trim();
            else if (!line.trim().isEmpty()) picks.add(line.split("\t", -1));
        }
        String made = null;
        File modeDir = new File(box, MODE_DIR);
        if (modeDir.isDirectory()) {
            File dst = Modes.dirOf(mode);
            for (int i = 2; dst.exists(); i++) dst = Modes.dirOf(mode + " (" + i + ")");
            if (!modeDir.renameTo(dst)) return null;
            made = dst.getName();
        } else {
            String into = Modes.dirOf(mode).isDirectory() ? mode : Modes.current();
            ModeCopy.Side src = new ModeCopy.Side(Ini.load(new File(box, MAIN)), Tree.loadFrom(new File(box, TREE)));
            for (String[] p : picks) {
                String m = restoreOne(src, p, into);
                if (m != null) made = made == null ? m : made + ", " + m;
            }
            if (made == null) return null;
        }
        Trash.wipe(box);
        Trash.forget(box.getParentFile(), java.util.Collections.singletonList(box.getName()));
        lib.dirChanged(box.getParent());
        Log.i("סל: שוחזר " + made);
        return made;
    }

    private static String restoreOne(ModeCopy.Side src, String[] p, String mode) {
        if (p[0].equals(K_LIST) && p.length >= 2) {
            ArrayList<String> path = Tree.parsePath(p[1]);
            if (path.isEmpty()) return null;
            List<String> parent = path.subList(0, path.size() - 1);
            return ModeCopy.restore(src, ModeCopy.Pick.list(path), mode, parent.isEmpty() ? null : parent);
        }
        if (p[0].equals(K_SMART) && p.length >= 4)
            return ModeCopy.restore(src, ModeCopy.Pick.smart(p[1], p[2]), mode, Tree.parsePath(p[3]));
        if (p[0].equals(K_TOOL) && p.length >= 3)                          // (עד 1.14 — גם הסביבה שבה היה; לבית)
            return ModeCopy.restore(src, ModeCopy.Pick.tool(p[1], p[2]), mode, null);
        if (p[0].equals(K_TILE) && p.length >= 3) {                         // סביבה מלפני 1.15 — כרשימה כסביבת עבודה
            String tool = Envs.migrate(src.ini, src.tree, null).get(p[1]);
            return tool == null ? null : ModeCopy.restore(src, ModeCopy.Pick.tool(tool, p[2]), mode, null);
        }
        return null;
    }
}
