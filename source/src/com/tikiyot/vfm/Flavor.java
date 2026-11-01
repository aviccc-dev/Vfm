package com.tikiyot.vfm;

// הבנייה: ישנה או מ (מנוע המסד). אחראי: לדעת באיזו בנייה אנחנו, ולתת את השמות שמבדילים ביניהן.
// אסור לו: לדעת דבר על הנתונים. קוד המקור אחד; סקריפט הבנייה מחליף את שורת ה־BUILD-FLAVOR בעותק (build/build.sh).

public final class Flavor {

    /** false = הבנייה הישנה; true = בניית מ. אל תשנה ידנית — הסקריפט מחליף את השורה. */
    public static final boolean M = false;  // BUILD-FLAVOR

    public static final String PKG_OLD = "com.tikiyot.vfm", PKG_M = "com.tikiyot.vfm.m";
    private static final String DIR_OLD = "מנהל קבצים וירטואלי", DIR_M = "מנהל קבצים וירטואלי מ";  // בכונן

    private Flavor() { }

    /** שם החבילה (והרשאת הקבצים המשותפים) — שתי התוכנות מותקנות זו לצד זו. */
    public static String pkg(boolean m) {
        return m ? PKG_M : PKG_OLD;
    }

    public static String authority(boolean m) {
        return pkg(m) + ".files";
    }

    /** תיקיית הנתונים בכונן — נפרדת, כדי ששתי התוכנות לא יכתבו לאותם קבצים. */
    public static String appDir(boolean m) {
        return m ? DIR_M : DIR_OLD;
    }

    /** תיקיית הנתונים של התוכנה הישנה — ממנה תעתיק בניית מ (מ0.2). */
    public static String oldAppDir() {
        return DIR_OLD;
    }
}
