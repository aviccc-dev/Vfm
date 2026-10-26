package com.tikiyot.vfm;

// אופני עבודה (105). אחראי: איזה אופן פעיל, רשימת האופנים, יצירה (ריק או העתק), מעבר.
// כל אופן הוא תיקייה משלו, ובה כל מה שהיה בתיקיית התוכנה: מבנה.txt, הגדרות/, .מערכת/ (האינדקס), .גיבויים/.
// משותף לכל האופנים: הקבצים בכונן עצמם וסל המחזור. אסור לו: לדעת מה יש בתוך הקבצים.

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class Modes {

    public static final String DIR = "אופני עבודה";                       // בכונן
    public static final String FIRST = "עבודה אמיתית";                    // בכונן
    /** אופן שנמחק לא נמחק באמת: התיקייה שלו עוברת לכאן, בתיקיית התוכנה (105). */
    public static final String DELETED = ".אופנים שנמחקו";                // בכונן
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

    /** 158: הקבצים שמגדירים אופן — הקובץ הראשי והרשימות (מה שקיים), לשיתוף. */
    public static List<String> settingsFiles(String name) {
        File dir = new File(new File(Paths.data(), DIR), name);
        List<String> out = new ArrayList<String>();
        for (File f : new File[]{new File(new File(dir, Paths.SETTINGS), Paths.MAIN), new File(dir, Paths.TREE),
                new File(dir, Sig.FILE)})                                              // 190: גם החתימות
            if (f.isFile()) out.add(f.getAbsolutePath());
        for (File f : Design.files(new File(new File(dir, Paths.SETTINGS), Design.DIR)))    // 1.12.0: גם העיצובים
            out.add(f.getAbsolutePath());
        return out;
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
     * יוצר אופן חדש. from = null — ריק; אחרת העתק של האופן הזה (בלי הגיבויים שלו).
     * מחזיר null אם הצליח, או הסבר למה לא.
     */
    public static String create(String name, String from) {
        String n = clean(name);
        if (n.isEmpty()) return Words.NEED_NAME;
        String bad = Physical.badName(n);                                    // 129
        if (bad != null) return bad;
        File dir = new File(new File(Paths.data(), DIR), n);
        if (dir.exists()) return Words.WorkModes.EXISTS;
        if (from == null) return dir.mkdirs() ? null : Words.CANT_CREATE_FOLDER;
        File src = new File(new File(Paths.data(), DIR), from);
        if (from.equals(current())) dir();                  // מוודא שההעברה הראשונה נעשתה
        try {
            copyTree(src, dir);
            return null;
        } catch (Throwable t) {
            Log.e("יצירת אופן עבודה", t);
            return Words.COPY_FAILED;
        }
    }

    private static void copyTree(File src, File dst) throws Exception {
        dst.mkdirs();
        File[] k = src.listFiles();
        if (k == null) return;
        for (File f : k) {
            if (f.getName().equals(Paths.BACKUPS)) continue;     // לאופן חדש — גיבויים משלו
            File t = new File(dst, f.getName());
            if (f.isDirectory()) copyTree(f, t);
            else Files.copy(f, t);
        }
    }

    /**
     * שינוי שם של אופן. null = הצליח; אחרת ההסבר. אם האופן פעיל — גם המצביע והשם בזיכרון מתעדכנים,
     * והקורא מפעיל את התוכנה מחדש (relaunch) אחרי שהאינדקס כבר נשמר (flush), כדי שלא יישאר
     * שום דבר שמצביע על התיקייה הישנה.
     */
    public static synchronized String rename(String old, String neu) {
        String n = clean(neu);
        if (n.isEmpty()) return Words.NEED_NAME;
        String bad = Physical.badName(n);                                    // 129
        if (bad != null) return bad;
        if (n.equals(old)) return null;
        File base = new File(Paths.data(), DIR);
        File src = new File(base, old);
        if (!src.isDirectory()) return Words.WorkModes.NOT_FOUND;
        for (String other : all())
            if (!other.equals(old) && other.equalsIgnoreCase(n)) return Words.WorkModes.EXISTS;
        boolean active = old.equals(current());
        File dst = new File(base, n);
        boolean ok;
        if (n.equalsIgnoreCase(old)) {                          // שינוי אותיות בלבד: בכונן שאינו מבדיל בין גדולות לקטנות
            File tmp = new File(base, ".שינוי-שם-" + System.nanoTime());   // בכונן
            ok = src.renameTo(tmp) && tmp.renameTo(dst);
        } else {
            ok = !dst.exists() && src.renameTo(dst);
        }
        if (!ok) return Words.RENAME_FAILED;
        if (active) {
            try {
                Files.writeAtomic(pointer(Paths.data()), n + "\n");
            } catch (Throwable t) {
                Log.e("שינוי שם האופן הפעיל", t);
                return Words.RENAME_FAILED;
            }
            current = n;
        }
        Log.i("אופני עבודה: \"" + old + "\" שונה ל-\"" + n + "\"");
        return null;
    }

    /**
     * מחיקת אופן (לא הפעיל). התיקייה לא נמחקת אלא עוברת ל-".אופנים שנמחקו" בתיקיית התוכנה,
     * כדי שאפשר יהיה להחזיר. null = הצליח; אחרת ההסבר.
     */
    public static synchronized String delete(String name) {
        if (name.equals(current())) return Words.WorkModes.ACTIVE;
        File src = new File(new File(Paths.data(), DIR), name);
        if (!src.isDirectory()) return Words.WorkModes.NOT_FOUND;
        if (BinBox.mode(name, src)) {                                         // 1.10.0: לסל המחזור
            Log.i("אופני עבודה: \"" + name + "\" הועבר לסל");
            return null;
        }
        File trash = new File(Paths.data(), DELETED);
        trash.mkdirs();
        File dst = new File(trash, name);
        if (dst.exists()) dst = new File(trash, name + " (" + System.currentTimeMillis() + ")");
        if (!src.renameTo(dst)) return Words.DELETE_FAILED;
        Log.i("אופני עבודה: \"" + name + "\" הועבר ל-" + DELETED);
        return null;
    }

    /** כמה רשימות וכמה פריטים יש באופן (לחלון האישור). {רשימות, פריטים}. */
    public static int[] counts(String name) {
        int lists = 0, items = 0;
        try {
            File f = new File(new File(new File(Paths.data(), DIR), name), Paths.TREE);
            if (f.isFile())
                for (String line : Files.readUtf8(f).split("\n")) {
                    if (line.trim().isEmpty() || line.startsWith(Tree.ICON_LINE.trim()) || line.startsWith(Tree.DESC_LINE.trim())
                            || line.startsWith(Tree.DESIGN_LINE.trim())) continue;                       // 1.12.0
                    if (line.startsWith("#") || line.startsWith(Tree.SMART_LINE)) lists++; else items++;   // 161
                }
        } catch (Throwable ignored) {
        }
        return new int[]{lists, items};
    }

    /**
     * מעבר לאופן אחר: כותב את שמו, מחכה שהאינדקס יישמר, ומפעיל את התוכנה מחדש —
     * כך כל מה שנטען מהאופן הקודם (רשימות, הגדרות, אינדקס) נטען מהחדש.
     */
    public static void switchTo(final Activity a, String name) {
        try {
            Library.get(a).flush(4000);
            Files.writeAtomic(pointer(Paths.data()), clean(name) + "\n");
        } catch (Throwable t) {
            Log.e("מעבר לאופן עבודה", t);
            Ui.toast(a, Words.WorkModes.SWITCH_FAILED);
            return;
        }
        Log.i("אופני עבודה: מעבר אל \"" + name + "\"");
        reenter(a);
    }

    /**
     * 163: אחרי שהאופן הפעיל השתנה (מעבר, שינוי שם) — בלי לסגור את התוכנה: שוכחים מה שנקרא מהקודם,
     * והמסך הראשי נכנס מחדש. בזמן בניית אינדקס — הדרך הבטוחה (סגירה ופתיחה), כדי שהבנייה לא תישמר לאופן החדש.
     */
    public static void reenter(Activity a) {
        Library lib = Library.get(a);
        if (lib.busy || !(a instanceof Home)) {
            relaunch(a);
            return;
        }
        forget();
        lib.forgetMode();
        ((Home) a).enter();
    }

    /** סוגר את התוכנה ופותח אותה מחדש (אחרי שהמצביע כבר נכתב והאינדקס נשמר). */
    public static void relaunch(Activity a) {
        Intent i = new Intent(a, Home.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pi = PendingIntent.getActivity(a, 1051, i, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager am = (AlarmManager) a.getSystemService(Context.ALARM_SERVICE);
        if (am != null) am.set(AlarmManager.RTC, System.currentTimeMillis() + 300, pi);
        a.finishAffinity();
        System.exit(0);
    }

    /** לבדיקות: שוכח את האופן הפעיל שנקרא, כדי שייקרא שוב מהקובץ. */
    static synchronized void forget() {
        current = null;
    }
}
