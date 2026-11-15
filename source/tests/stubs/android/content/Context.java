package android.content;

import java.io.File;

// תחליף לבדיקות על המחשב (מבנה 71): ה"הקשר" של אנדרואיד — רק מה שהקוד שמתחת למסכים מבקש ממנו. המסדים — בתיקייה
// זמנית ליד ה"כונן" (vfm.home), ההעדפות — בזיכרון, והנכסים — מתיקיית assets של המקור (vfm.src).
// עם זה שער המסד (Db) עובד במחשב, ופקודות הרשימות נבדקות בכל בנייה — לא רק במכשיר.
public class Context {
    public static final int MODE_PRIVATE = 0;
    public static final String POWER_SERVICE = "power";

    private final File base = new File(System.getProperty("vfm.home") + "-app");
    private final java.util.Map<String, SharedPreferences> prefs = new java.util.HashMap<String, SharedPreferences>();

    public Context getApplicationContext() { return this; }
    public File getDatabasePath(String name) { File d = new File(base, "databases"); d.mkdirs(); return new File(d, name); }
    public File getCacheDir() { File d = new File(base, "cache"); d.mkdirs(); return d; }
    public File getFilesDir() { File d = new File(base, "files"); d.mkdirs(); return d; }
    public File[] getExternalFilesDirs(String type) { return new File[0]; }
    public String getPackageName() { return "com.tikiyot.vfm.tests"; }
    public Object getSystemService(String name) { return null; }
    public android.content.pm.PackageManager getPackageManager() { return null; }
    public ContentResolver getContentResolver() { return null; }
    public int checkSelfPermission(String p) { return 0; }
    public android.content.res.AssetManager getAssets() { return new android.content.res.AssetManager(); }
    public boolean deleteDatabase(String name) { return android.database.sqlite.SQLiteDatabase.deleteDatabase(getDatabasePath(name)); }

    public synchronized SharedPreferences getSharedPreferences(String name, int mode) {
        SharedPreferences p = prefs.get(name);
        if (p == null) prefs.put(name, p = new MemPrefs());
        return p;
    }
}
