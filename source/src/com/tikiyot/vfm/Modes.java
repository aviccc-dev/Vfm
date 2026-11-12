package com.tikiyot.vfm;

// אופני עבודה (105) — התיקיות שבכונן. אחראי: האופן של התיקיות (הקובץ "אופן עבודה.txt"), רשימת התיקיות, יצירת אופן ריק.
// האופן שעל המסך — של המסד (DbScreen.savedMode / DbListScreen.currentMode), לא מכאן.
// כל אופן הוא תיקייה משלו: הגדרות/, .מערכת/ (האינדקס), .גיבויים/ (הרשימות עצמן — במסד של האופן, באחסון הפרטי).
// משותף לכל האופנים: הקבצים בכונן עצמם וסל המחזור. אסור לו: לדעת מה יש בתוך הקבצים.


import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class Modes {

    public static final String DIR = "אופני עבודה";                       // בכונן
    public static final String FIRST = "עבודה אמיתית";                    // בכונן
    private static String current;

    private Modes() { }

    /** הקובץ שבו כתוב שם האופן הפעיל — שורה אחת. */
    static File pointer(File data) {
        return new File(data, "אופן עבודה.txt");                         // בכונן
    }

    /** שם האופן הפעיל. */
    public static synchronized String current() {
        if (current == null) {
            String m = "";
            try {
                m = Files.readUtf8(pointer(Paths.data())).trim();
            } catch (Throwable ignored) {
            }
            current = clean(m).isEmpty() ? FIRST : clean(m);
        }
        return current;
    }

    /** תיקיית האופן הפעיל. */
    public static synchronized File dir() {
        File w = new File(new File(Paths.data(), DIR), current());
        w.mkdirs();
        return w;
    }

    /** 162: התיקייה של אופן לפי שם. */
    public static File dirOf(String name) {
        return new File(new File(Paths.data(), DIR), name);
    }

    /** כל האופנים, לפי שם. */
    public static List<String> all() {
        List<String> out = new ArrayList<String>();
        File[] k = new File(Paths.data(), DIR).listFiles();
        if (k != null) for (File f : k) if (f.isDirectory() && !f.getName().startsWith(".")) out.add(f.getName());
        if (!out.contains(current())) out.add(current());
        String[] a = out.toArray(new String[0]);
        Arrays.sort(a);
        return new ArrayList<String>(Arrays.asList(a));
    }

    /** שם תקין לתיקייה: בלי "/" ובלי נקודה בהתחלה. */
    public static String clean(String name) {
        String n = name == null ? "" : name.replace('/', '∕').replace('\n', ' ').trim();
        while (n.startsWith(".")) n = n.substring(1).trim();
        return n;
    }

    /**
     * יוצר אופן חדש, ריק. מחזיר null אם הצליח, או הסבר למה לא.
     */
    public static String create(String name) {
        String n = clean(name);
        if (n.isEmpty()) return Words.NEED_NAME;
        String bad = Physical.badName(n);                                    // 129
        if (bad != null) return bad;
        File dir = new File(new File(Paths.data(), DIR), n);
        if (dir.exists()) return Words.WorkModes.EXISTS;
        return dir.mkdirs() ? null : Words.CANT_CREATE_FOLDER;
    }
}
