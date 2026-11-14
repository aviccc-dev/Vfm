package com.tikiyot.vfm;

// מיקומי הנתונים. אחראי: לדעת איפה כל קובץ. אסור לו: לקרוא או לכתוב תוכן.

import android.os.Environment;

import java.io.File;

public final class Paths {

    /**
     * מ0.11.2: התיקייה של התוכנה הרגילה — אותם אופנים ואותם קבצים. מה ששייך למסד (קובץ המונה, הגיבויים) — בתיקייה של
     * בניית מ: dbData().
     */
    public static final String APP_DIR = "מנהל קבצים וירטואלי";  // בכונן
    /** השמות בכונן, בתוך תיקיית אופן העבודה — מקום אחד (131). */
    public static final String SETTINGS = "הגדרות", SYSTEM = ".מערכת", BACKUPS = ".גיבויים";  // בכונן

    /**
     * 1.25.6: תיקיות בתוך תיקיית התוכנה שהתוכן שלהן — של המשתמש (הנפוצות שלו, יומני הבדיקות): אפשר למחוק ולשנות
     * מהכונן. המשתמש: "ומהכונן הוא לא מוכן למחוק בטענה שזו תוכנת מערכת".
     */
    public static final String[] USER_DIRS = {"פקודות נפוצות", "בדיקות"};  // בכונן

    /** התיקייה של המסד בכונן (קובץ המונה, הגיבויים) — כמו שהייתה מ־מ0.1. */
    public static final String DB_DIR = "מנהל קבצים וירטואלי מ";  // בכונן

    private Paths() { }

    private static File mk(File f) {
        f.mkdirs();
        return f;
    }

    public static File data() {
        return mk(new File(Environment.getExternalStorageDirectory(), APP_DIR));
    }

    /** מ0.11.2: התיקייה של המסד בכונן (קובץ המונה, "גיבוי מסד") — של בניית מ, כמו שהייתה מ־מ0.1 (לא זזה). */
    public static File dbData() {
        return mk(new File(Environment.getExternalStorageDirectory(), DB_DIR));
    }

    /** 105: תיקיית אופן העבודה הפעיל — בה כל מה שלהלן. data() היא תיקיית התוכנה, המשותפת לכל האופנים. */
    public static File work() {
        return Modes.dir();
    }


    public static File settings() {
        return mk(new File(work(), SETTINGS));
    }



    public static File system() {
        return mk(new File(work(), SYSTEM));
    }

    public static File backups() {
        return mk(new File(work(), BACKUPS));
    }

    public static File index(String volumeKey) {
        return new File(system(), "אינדקס-" + volumeKey + ".bin");  // בכונן
    }

    /** 182: הנקודה במאגר של אנדרואיד — לכל כונן, ליד האינדקס שלו. */
    public static File point(String volumeKey) {
        return new File(system(), "נקודה-" + volumeKey + ".txt");  // בכונן
    }

    /**
     * מבנה 42: מסלול בצורה אחת — בלי "." ו"..", בלי לוכסן כפול ובלי לוכסן בסוף. לפי הכתוב בלבד, בלי לגשת לכונן.
     * בלי זה אותה תיקייה נכתבת בכמה צורות, וכל מי שמשווה מסלולים — ההגנות על שורש הכונן ועל תיקיית התוכנה,
     * "כבר במקום", ההדמיה של קובץ הפקודות — רואה תיקיות שונות. המשפך: מסלול נקי (כמעט כולם) חוזר כמו שהוא.
     */
    public static String clean(String path) {
        if (path == null || path.startsWith("/\u0000")) return path;          // סימנים פנימיים של מנוע הפקודות
        if (path.indexOf("/.") < 0 && path.indexOf("//") < 0 && !path.startsWith(".") && !(path.length() > 1 && path.endsWith("/"))) return path;
        boolean abs = path.startsWith("/");
        java.util.ArrayList<String> out = new java.util.ArrayList<String>();
        for (String seg : path.split("/")) {
            if (seg.isEmpty() || seg.equals(".")) continue;
            if (seg.equals("..")) {
                if (!out.isEmpty() && !out.get(out.size() - 1).equals("..")) out.remove(out.size() - 1);
                else if (!abs) out.add("..");
                continue;
            }
            out.add(seg);
        }
        StringBuilder sb = new StringBuilder(abs ? "/" : "");
        for (int i = 0; i < out.size(); i++) {
            if (i > 0) sb.append('/');
            sb.append(out.get(i));
        }
        return sb.length() == 0 ? "." : sb.toString();
    }

    public static String relative(File f) {
        String base = settings().getAbsolutePath() + "/";
        String p = f.getAbsolutePath();
        return p.startsWith(base) ? p.substring(base.length()) : p;
    }
}
