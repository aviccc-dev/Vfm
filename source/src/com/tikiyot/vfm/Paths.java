package com.tikiyot.vfm;

// מיקומי הנתונים. אחראי: לדעת איפה כל קובץ. אסור לו: לקרוא או לכתוב תוכן.

import android.os.Environment;

import java.io.File;

public final class Paths {

    public static final String APP_DIR = "מנהל קבצים וירטואלי";  // בכונן
    /** השמות בכונן, בתוך תיקיית אופן העבודה — מקום אחד (131). */
    public static final String TREE = "מבנה.txt", SETTINGS = "הגדרות", SYSTEM = ".מערכת", BACKUPS = ".גיבויים",  // בכונן
            MAIN = "מבנה התוכנה.ini";  // בכונן

    /**
     * 1.25.6: תיקיות בתוך תיקיית התוכנה שהתוכן שלהן — של המשתמש (הנפוצות שלו, יומני הבדיקות): אפשר למחוק ולשנות
     * מהכונן. המשתמש: "ומהכונן הוא לא מוכן למחוק בטענה שזו תוכנת מערכת".
     */
    public static final String[] USER_DIRS = {"פקודות נפוצות", "בדיקות"};  // בכונן

    private Paths() { }

    private static File mk(File f) {
        f.mkdirs();
        return f;
    }

    public static File data() {
        return mk(new File(Environment.getExternalStorageDirectory(), APP_DIR));
    }

    /** 105: תיקיית אופן העבודה הפעיל — בה כל מה שלהלן. data() היא תיקיית התוכנה, המשותפת לכל האופנים. */
    public static File work() {
        return Modes.dir();
    }

    /** עץ התיקיות הווירטואליות */
    public static File tree() {
        return new File(work(), TREE);
    }

    public static File settings() {
        return mk(new File(work(), SETTINGS));
    }

    /** הקובץ הראשי: כל מבנה התוכנה */
    public static File structure() {
        return new File(settings(), MAIN);
    }

    public static File mirror() {
        return mk(new File(settings(), "כלים"));  // בכונן — 1.17.5: היה "אריחים" (סביבות העבודה יצאו ב־1.15.0)
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

    public static String relative(File f) {
        String base = settings().getAbsolutePath() + "/";
        String p = f.getAbsolutePath();
        return p.startsWith(base) ? p.substring(base.length()) : p;
    }
}
