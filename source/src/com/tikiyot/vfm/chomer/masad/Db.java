package com.tikiyot.vfm;

// שער המסד — שלב ראשון (מבנה 6, 13.11.2026): איפה המסדים. לכל אופן עבודה מסד משלו; כאן המקום האחד שיודע איפה הקובץ
// של כל אופן, אילו אופנים יש, איפה קובץ המונה, ואיפה הגיבויים של אופן. עד כאן ישבו ארבעת אלה במסך הפיתוח (DbScreen),
// וכל מי שנזקק למסד — פקודות, הלב, המעקב אחרי הכונן — פנה למסך. אותן פונקציות ואותן שורות; רק המקום חדש.
// השלבים הבאים (claude/mivne-mutza.md, ב1): האופן הנוכחי, ההכנה של אופן, ו"קרא" / "כתוב" במקום אחד.
// אחראי: לדעת איפה. אסור לו: לפתוח מסכים, ולהחליט מה כותבים.

import android.os.Environment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

final class Db {

    /** מסד לכל אופן עבודה, באחסון הפרטי של התוכנה (לא בתיקייה הגלויה): vfm-m-<אופן>.db. */
    static final String DB_PREFIX = "vfm-m-", DB_SUFFIX = ".db";

    // --- ההקשר (מבנה 7): עד כאן — משתנים של המסך הראשי (Db.currentMode, Db.app) ---

    /** מ0.10: האופן של המסד שעובדים בו עכשיו (המסך האחרון שהוצג) — לפקודות (DbVerbs). */
    static String currentMode;

    /** מ0.12: ההקשר של התוכנה — לפקודות שרצות במסד (מיקום קובץ המסד). */
    static android.content.Context app;

    private Db() { }

    static File dbFile(android.content.Context c, String mode) {
        return c.getDatabasePath(DB_PREFIX + mode + DB_SUFFIX);
    }

    /** האופנים שיש להם מסד כבר. */
    static List<String> dbModes(android.content.Context c) {
        List<String> out = new ArrayList<String>();
        File[] k = dbFile(c, "x").getParentFile().listFiles();
        if (k != null)
            for (File f : k) {
                String n = f.getName();
                if (n.startsWith(DB_PREFIX) && n.endsWith(DB_SUFFIX) && n.length() > DB_PREFIX.length() + DB_SUFFIX.length())
                    out.add(n.substring(DB_PREFIX.length(), n.length() - DB_SUFFIX.length()));
            }
        java.util.Collections.sort(out);
        return out;
    }

    /** קובץ המונה — בתיקיית הנתונים הגלויה, כדי ששחזור המסד מגיבוי ישן לא יחזיר מספר זיהוי. */
    static File counterFile() {
        return new File(new File(Paths.dbData(), Paths.SYSTEM), "מונה-מזהים.txt");  // בכונן (מ0.11.2: dbData — לא זז)
    }

    /** מ0.12: גם לפקודות שרצות במסד (DbListCmd). */
    static Backups backupsOf(android.content.Context c, String m) {
        return new Backups(new File(new File(Paths.dbData(), Backups.DIR), m), new File(c.getCacheDir(), "mbackup"));
    }

    // --- האופן השמור, ההכנה של אופן, שחזור הדוגמאות, בדיקת המנוע (מבנה 8): עד כאן — במסך הפיתוח (DbScreen) ---

    /** אופן התפריטים ("שמור לתוכנה") — בא עם התוכנה; בבית שלו כל התפריטים (Menus). אינו אופן עבודה. */
    static final String MENUS_MODE = Names.RESERVED_ROOT;
    static final String PREFS = "m", PREF_MODE = "mode";

    /** מ0.29: האופן כשעוד לא נבחר אחד: הראשון שיש לו מסד, ואם אין אף מסד (התקנה חדשה) — הדוגמאות. */
    static String defaultMode(android.content.Context c) {
        for (String m : Db.dbModes(c)) if (!m.equals(MENUS_MODE)) return m;    // מ0.41: אופן התפריטים אינו אופן עבודה
        return DbExamples.MODE;
    }

    /** מ0.23: האופן של המסד בלי מסך פתוח (למשל שיתוף מאפליקציה אחרת): מה שנבחר בפעם הקודמת, ואם אין — הפעיל בישן. */
    static String savedMode(android.content.Context c) {
        String m = c.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE).getString(PREF_MODE, "");
        return m.isEmpty() ? defaultMode(c) : m;
    }

    /** מ0.9.1: האופן שנבחר — נזכר גם למסך המסד וגם למראה המוכר. */
    static void saveMode(android.content.Context c, String m) {
        c.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE).edit().putString(PREF_MODE, m).apply();
    }

    static void setup(android.content.Context c, Store store, String mode) {
        try {
            if (DbPaths.home(store) != 0) return;
            // שמירה על הנתונים: מסד ריק שיש לאופן שלו גיבויים — אולי המסד אבד; לא ממלאים אותו, כדי שההצעה לשחזר מגיבוי
            // (offerRestoreIfEmpty, במסך המסד) תופיע ולא תיחסם.
            if (!Db.backupsOf(c, mode).list().isEmpty()) return;
            if (mode.equals(DbExamples.MODE)) {
                DbExamples.files(DbExamples.asset(c, DbExamples.ASSET_DEMO));
                DbExamples.install(store, DbExamples.asset(c, DbExamples.ASSET), Environment.getExternalStorageDirectory().getAbsolutePath());
            } else DbPaths.ensureHome(store);
        } catch (Throwable t) {
            Log.e("DbScreen.setup", t);
        }
    }

    /** מ0.29: המסד של האופן קיים ויש בו בית (נוצר אם צריך — setup). מחזיר את הבית; 0 — לא הצליח (אין גישה לקבצים, מסד פגום). */
    static long prepare(android.content.Context c, String mode) {
        if (!Files.hasAccess(c)) return 0;
        SqlStore s = null;
        try {
            if (Db.dbFile(c, mode).isFile()) {                                    // קיים — רק קוראים (לא נוגעים במסד שיש בו בית)
                s = SqlStore.openReadOnly(Db.dbFile(c, mode));
                long h = DbPaths.home(s);
                if (h != 0) return h;
                s.close();
                s = null;
            }
            s = SqlStore.open(Db.dbFile(c, mode), new IdCounter(Db.counterFile()));
            setup(c, s, mode);
            return DbPaths.home(s);
        } catch (Throwable t) {
            Log.e("DbScreen.prepare", t);
            return 0;
        } finally {
            if (s != null) s.close();
        }
    }

    /** מ0.29 (באג 1): שחזור הדוגמאות באופן הדוגמאות — גיבוי של המסד לפני, ואז DbExamples.restore. מחזיר הודעה למסך. */
    static String restoreExamples(android.content.Context c) {
        SqlStore s = null;
        try {
            s = SqlStore.open(Db.dbFile(c, DbExamples.MODE), new IdCounter(Db.counterFile()));
            try {
                Db.backupsOf(c, DbExamples.MODE).before(s, Backups.OP_RESTORE, System.currentTimeMillis());
            } catch (Throwable t) {
                Log.e("DbScreen.restoreExamples.backup", t);
                return Words.Db.importBackupFailed(String.valueOf(t.getMessage()));
            }
            DbExamples.files(DbExamples.asset(c, DbExamples.ASSET_DEMO));
            int n = DbExamples.restore(s, DbExamples.asset(c, DbExamples.ASSET), Environment.getExternalStorageDirectory().getAbsolutePath());
            return Words.Db.examplesRestored(n);
        } catch (Throwable t) {
            Log.e("DbScreen.restoreExamples", t);
            return Words.Db.failed(String.valueOf(t.getMessage()));
        } finally {
            if (s != null) s.close();
        }
    }

    /** אותן בדיקות שרצות במחשב (StoreContract), על SQLite האמיתי של המכשיר, במסד זמני נפרד. ok[0] — האם עברו. */
    /**
     * בדיקת המנוע של המסד מול ה־SQLite של המכשיר — על מסדים זמניים בצד. מ0.36: רצה מדוח האבחון (המשתמש, 11.11.2026, על
     * הכפתור "בדיקה מלאה": "כך נפטרנו מעוד כפתור שלא משמש בשוטף").
     */
    static String selfTestText(android.content.Context c, boolean[] ok) {
        final File dir = new File(c.getCacheDir(), "mselftest");
        deleteAll(dir);
        dir.mkdirs();
        long t0 = System.currentTimeMillis();
        try {
            final int[] seq = {0};
            StoreContract.Result r = StoreContract.run(new StoreContract.Opener() {
                @Override
                public Store fresh(File counter) throws Exception {
                    return SqlStore.open(new File(dir, "t" + (seq[0]++) + ".db"), new IdCounter(counter));
                }
            }, dir);
            String persist = SqlStore.persistenceCheck(dir);
            if (persist != null) r.failed.add("persistence: " + persist);
            else r.passed++;
            StoreContract.Result br = BackupContract.run(new File(dir, "backup"));
            r.passed += br.passed;
            for (String f : br.failed) r.failed.add("backup: " + f);
            String info = "?";
            SqlStore s = SqlStore.open(new File(dir, "info.db"), new IdCounter(new File(dir, "info-counter.txt")));
            try {
                info = s.info();
            } finally {
                s.close();
            }
            long ms = System.currentTimeMillis() - t0;
            ok[0] = r.failed.isEmpty();
            return ok[0] ? Words.Db.selfTestOk(r.passed, ms, info) : Words.Db.selfTestBad(r.passed, r.failed, info);
        } catch (Throwable t) {
            Log.e("DbScreen.selfTest", t);
            ok[0] = false;
            return Words.Db.failed(String.valueOf(t));
        } finally {
            deleteAll(dir);
        }
    }

    private static void deleteAll(File f) {
        File[] kids = f.listFiles();
        if (kids != null) for (File k : kids) deleteAll(k);
        f.delete();
    }
}
