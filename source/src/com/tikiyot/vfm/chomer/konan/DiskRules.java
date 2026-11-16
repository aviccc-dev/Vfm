package com.tikiyot.vfm;

// מבנה 182 (סידור קומת החומר): הכללים של הכונן — מה מוגן ומה אסור, לפני שנוגעים: שורש הכונן, הסל, תיקיית התוכנה והתיקייה של המסד,
// תיקייה לתוך עצמה, אותו קובץ, שם לא חוקי, שם פנוי, ומסלול יחסי. עד כאן ב־Physical.
// אחראי: לענות "מותר?" ו"למה לא". אסור לו: לשנות דבר בכונן.

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class DiskRules {

    private DiskRules() { }

    /** השם החופשי הבא בתיקייה: "ספר (2).pdf", "ספרים (2)". */
    public static String freeName(File dir, String name, boolean isDir) {
        String base = isDir ? name : Text.stem(name), ext = isDir ? "" : Text.tail(name);   // 124
        if (base.isEmpty()) { base = name; ext = ""; }
        String n = name;
        for (int i = 2; new File(dir, n).exists(); i++) n = base + " (" + i + ")" + ext;
        return n;
    }

    /** למה אי אפשר למחוק או להעביר את התיקייה הזו, או null אם מותר. */
    public static String forbidden(Library lib, File dir) {
        String p = Paths.clean(dir.getAbsolutePath());                          // מבנה 42: גם "תיקייה/.." הוא השורש
        String why = rootOrTrash(lib, p);
        if (why != null) return why;
        String app = appDir();
        if (app != null && ((app + "/").startsWith(p + "/") || p.startsWith(app + "/"))) return Words.Disk.IS_APP_FOLDER;
        // מבנה 64 (ממצא 44; המשתמש, 14.11.2026: "כן" — להגן): גם התיקייה של המסד (קובץ המונה, הגיבויים של כל האופנים) — היא עצמה,
        // תיקייה שמכילה אותה, ומה שבתוכה; חוץ מהתוספים ומה שהם כותבים (Paths.DB_USER_DIRS) — אלה של המשתמש.
        String db = dbDir();
        if (db != null && ((db + "/").startsWith(p + "/") || (p.startsWith(db + "/") && !dbUser(db, p)))) return Words.Disk.IS_APP_FOLDER;
        return null;
    }

    /**
     * מבנה 65 (ממצא 40ח): האם אסור לכתוב קובץ חדש בתיקייה הזו — כמו יעד של העתקה: לא בסל. (קובץ של התוכנה — programFile, על
     * המסלול של הקובץ עצמו.) שורש הכונן ותיקיות המשתמש שבתיקיית התוכנה — מותרים: עד כאן "כתוב" שאל את forbidden, שהיא השאלה
     * "מותר למחוק או להעביר את התיקייה הזו", וסירב גם להם. null — מותר.
     */
    public static String noWriteInto(Library lib, File dir) {
        String p = Paths.clean(dir.getAbsolutePath()), root = lib.rootOf(p);
        return root != null && (p + "/").startsWith(new File(root, Index.TRASH).getAbsolutePath() + "/") ? Words.Disk.IS_TRASH : null;
    }

    /** שורש כונן, או הסל ומה שבתוכו — אסורים תמיד, גם לתוכנה עצמה (deleteOwn). */
    static String rootOrTrash(Library lib, String p) {
        String root = lib.rootOf(p);
        if (root != null && (p.equals(root) || (p + "/").equals(root))) return Words.Disk.DRIVE_ROOT;
        for (Index ix : lib.indexes) if (ix.rootPath.equals(p)) return Words.Disk.DRIVE_ROOT;
        if (root != null && (p + "/").startsWith(new File(root, Index.TRASH).getAbsolutePath() + "/"))
            return Words.Disk.IS_TRASH;
        return null;
    }

    /**
     * 90: קובץ של התוכנה עצמה (קובצי ההגדרות, הגיבויים, האינדקס) — אסור למחוק או להעביר,
     * ואסור להעביר או להעתיק לתוך התיקייה שלה. מבנה 64: גם מה שבתיקייה של המסד (קובץ המונה, "גיבוי מסד").
     */
    public static String programFile(String path) {
        path = Paths.clean(path);                                               // מבנה 42
        String app = appDir();
        if (app != null && (path.equals(app) || path.startsWith(app + "/"))) {
            for (String u : Paths.USER_DIRS)                                     // 1.25.6: מה שהמשתמש יצר — שלו
                if (path.startsWith(app + "/" + u + "/")) return null;
            return Words.Disk.IS_APP_FILE;
        }
        String db = dbDir();
        if (db != null && (path.equals(db) || path.startsWith(db + "/"))) return dbUser(db, path) ? null : Words.Disk.IS_APP_FILE;
        return null;
    }

    /**
     * מבנה 230 (המשתמש, 16.11.2026, על מחיקת קובץ פקודות מהעורך: "גם אם הוא בתוך ספריית התוכנה"): למה אסור להוציא את הקובץ
     * הזה ממקומו — למחוק (לסל) או להעביר (מבנה 77: מה שאסור למחוק — אסור גם להעביר); null — מותר. כמו programFile, חוץ מקובץ
     * פקודות ב"הפקודות שלי" של אופן עבודה: אותו מותר, מכל מקום (מסך, עורך, קובץ פקודות). לכתוב שם, לשנות שם ולהכניס לשם —
     * כמו קודם (programFile).
     */
    public static String cantRemove(String path) {
        String why = programFile(path);
        if (why == null) return null;
        String app = appDir(), p = Paths.clean(path);
        return app != null && myCommand(app, p) ? null : why;
    }

    /** מבנה 230: p (נקי) — קובץ פקודות בתיקיית "הפקודות שלי" של אופן (אופני עבודה / האופן / הגדרות / קיצורים), לא בתת־תיקייה. */
    static boolean myCommand(String app, String p) {
        if (!p.startsWith(app + "/")) return false;
        String[] k = p.substring(app.length() + 1).split("/");
        return k.length == 5 && k[0].equals(Modes.DIR) && !k[1].startsWith(".") && k[2].equals(Paths.SETTINGS)
                && k[3].equals(Paths.SHORTCUTS) && Files.isCmdFile(k[4]);
    }

    static String appDir() {
        try {
            return Paths.data().getAbsolutePath();
        } catch (Throwable t) {
            return null;
        }
    }

    static String dbDir() {
        try {
            return Paths.dbData().getAbsolutePath();
        } catch (Throwable t) {
            return null;
        }
    }

    /** מבנה 64: p (נקי) הוא אחת מתיקיות המשתמש שבתיקייה של המסד (תוספים, נתוני תוספים), או בתוכה. */
    static boolean dbUser(String db, String p) {
        for (String u : Paths.DB_USER_DIRS) if (p.equals(db + "/" + u) || p.startsWith(db + "/" + u + "/")) return true;
        return false;
    }

    /** מבנה 64: p (נקי) בתוך תיקיית התוכנה או בתוך התיקייה של המסד. */
    static boolean ours(String p) {
        String app = appDir(), db = dbDir();
        return (app != null && p.startsWith(app + "/")) || (db != null && p.startsWith(db + "/"));
    }

    /** האם folder היא src עצמה או תיקייה שבתוכה. */
    static boolean inside(File folder, File src) {
        String f = Paths.clean(folder.getAbsolutePath()) + "/", s = Paths.clean(src.getAbsolutePath()) + "/";   // מבנה 42
        return f.startsWith(s);
    }

    /**
     * מבנה 42: אותו קובץ בכונן, גם כשהמסלולים כתובים אחרת — קישור (‎/sdcard), או אותיות גדולות וקטנות בכונן שאינו מבדיל
     * ביניהן. נשאל רק כשהשם ביעד תפוס (המשפך), לפני שמחליפים: "להחליף" קובץ בעצמו שלח אותו לסל, וההעברה נכשלה.
     */
    static boolean sameFile(File a, File b) {
        if (a.equals(b)) return true;
        try {
            if (a.getCanonicalFile().equals(b.getCanonicalFile())) return true;
        } catch (java.io.IOException e) {
            Log.e("Physical.sameFile", e);
        }
        try {
            return java.nio.file.Files.isSameFile(a.toPath(), b.toPath());      // אנדרואיד 8 ומעלה; לפניו — לפי המסלול בלבד
        } catch (Throwable t) {
            return false;
        }
    }

    /** המסלול של p יחסית ל־base ("" — הוא עצמו), או null אם אינו בתוכו. */
    static String rel(String base, String p) {
        if (p.equals(base)) return "";
        String b = base.endsWith("/") ? base : base + "/";
        return p.startsWith(b) ? p.substring(b.length()) : null;
    }

    /** מבנה 77: למה אי אפשר לבחור את התיקייה הזו כיעד של העתקה או העברה (תיקיית התוכנה, הסל) — בנוסח של המסך; null — אפשר. */
    public static String noDest(Library lib, File folder) {
        if (programFile(folder.getAbsolutePath()) != null) return Words.Disk.DEST_APP_NOT;
        String droot = lib.rootOf(folder.getAbsolutePath());
        return droot != null && inside(folder, new File(droot, Index.TRASH)) ? Words.Disk.DEST_TRASH_NOT : null;
    }

    /** 142: האם אפשר להעביר את המקורות האלה (קובצי התוכנה, שורש, סל). null = אפשר; אחרת ההסבר. */
    public static String cannotMove(Library lib, List<String> paths, List<String> dirs) {
        for (String p : paths)
            if (cantRemove(p) != null) return Words.Disk.CANT_MOVE + new File(p).getName() + "\": " + cantRemove(p);   // מבנה 230
        for (String d : dirs) {
            String why = forbidden(lib, new File(d));
            if (why != null) return Words.Disk.CANT_MOVE + new File(d).getName() + "\": " + why;
        }
        return null;
    }

    /** בדיקת שם לקובץ או לתיקייה בכונן. null = תקין; אחרת ההסבר. מבנה 9: הבדיקה עצמה — ב־Files (גם הבסיס שואל). */
    public static String badName(String name) {
        return Files.badName(name);
    }
}
