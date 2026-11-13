package com.tikiyot.vfm;

// שער המסד — שלב ראשון (מבנה 6, 13.11.2026): איפה המסדים. לכל אופן עבודה מסד משלו; כאן המקום האחד שיודע איפה הקובץ
// של כל אופן, אילו אופנים יש, איפה קובץ המונה, ואיפה הגיבויים של אופן. עד כאן ישבו ארבעת אלה במסך הפיתוח (DbScreen),
// וכל מי שנזקק למסד — פקודות, הלב, המעקב אחרי הכונן — פנה למסך. אותן פונקציות ואותן שורות; רק המקום חדש.
// השלבים הבאים (claude/mivne-mutza.md, ב1): האופן הנוכחי, ההכנה של אופן, ו"קרא" / "כתוב" במקום אחד.
// אחראי: לדעת איפה. אסור לו: לפתוח מסכים, ולהחליט מה כותבים.

import java.io.File;
import java.util.ArrayList;
import java.util.List;

final class Db {

    /** מסד לכל אופן עבודה, באחסון הפרטי של התוכנה (לא בתיקייה הגלויה): vfm-m-<אופן>.db. */
    static final String DB_PREFIX = "vfm-m-", DB_SUFFIX = ".db";

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
}
