package com.tikiyot.vfm;

// מבנה 183 (סידור קומת החומר): קובץ אחד — העתקה לשם זמני ורק בסוף לשם הסופי, העברה (בין כוננים: העתקה, השוואת חתימות, ורק אז מחיקה מהמקור),
// והחתימה המלאה של תוכן. עד כאן ב־Physical.
// אחראי: הקובץ האחד. אסור לו: להחליט מה מעתיקים — הוא נקרא רק מתוך תיקיית הכונן, מאחורי כניסה ששאלה את ההיתר (שומר 183).

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class FileCopy {

    private FileCopy() { }

    /** העברת קובץ למקום פנוי: שינוי שם, ובין כוננים — העתקה, בדיקה, ורק אז מחיקה מהמקור. */
    static boolean moveFile(File src, File dst, boolean sameDrive) {
        if (dst.exists() || !src.isFile()) return false;
        if (sameDrive && src.renameTo(dst)) return true;
        String[] sig = new String[1];
        if (!copyFile(src, dst, sig)) return false;
        // מבנה 39 (ממצא 33; המשתמש, 14.11.2026: "להשוות חתימות"): בין כוננים המקור נמחק לצמיתות — ולכן קודם קוראים
        // את העותק מהכונן ומשווים חתימה מלאה של כל התוכן לזו של מה שנקרא מהמקור. שונה — העותק נמחק, והמקור נשאר.
        if (sig[0] == null || !sig[0].equals(digest(dst))) {
            Log.i(Words.Disk.COPY_DIFFERS + dst);
            dst.delete();
            return false;
        }
        if (!src.delete()) {
            dst.delete();
            return false;
        }
        return true;
    }

    /** 1.18.0: סיומת הקובץ הזמני של העתקה — נכתב בשם זה, ורק בסוף מקבל את השם הסופי. */
    static final String PARTIAL = ".vfmtmp";  // בכונן

    /** השם הזמני של dst: מוסתר, ליד היעד. שם ארוך מדי (מעל 255 בתים) — שם קצר לפי גיבוב. */
    static File partial(File dst) {
        String n = "." + dst.getName() + PARTIAL;
        try {
            if (n.getBytes("UTF-8").length > 255) n = "." + Integer.toHexString(dst.getName().hashCode()) + PARTIAL;
        } catch (java.io.UnsupportedEncodingException ignored) {
        }
        return new File(dst.getParentFile(), n);
    }

    /** האם זה קובץ זמני של העתקה שלא הושלמה. */
    static boolean isPartial(String name) {
        return name.startsWith(".") && name.endsWith(PARTIAL);
    }

    /**
     * העתקת קובץ למקום פנוי, עם בדיקת גודל. 1.18.0: נכתב לשם זמני ורק בסוף מקבל את השם הסופי — הפסקה
     * באמצע (כרטיס שנשלף, מכשיר שכבה) לא משאירה קובץ שבור בשם האמיתי. שארית זמנית מניסיון קודם — נמחקת.
     */
    static boolean copyFile(File src, File dst) {
        return copyFile(src, dst, null);
    }

    /** מבנה 39: חתימה מלאה של כל תוכן הקובץ (SHA-1, בהקסה) — להשוואת עותק למקור. null — לא נקרא. */
    static String digest(File f) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-1");
            java.io.InputStream in = new java.io.FileInputStream(f);
            try {
                byte[] buf = new byte[1 << 16];
                int n;
                while ((n = in.read(buf)) > 0) md.update(buf, 0, n);
            } finally {
                in.close();
            }
            return new java.math.BigInteger(1, md.digest()).toString(16);
        } catch (Throwable t) {
            Log.e("חתימת עותק", t);
            return null;
        }
    }

    /** sig — לא null: sig[0] מקבל את החתימה המלאה של מה שנקרא מהמקור בזמן ההעתקה עצמה (בלי קריאה נוספת של המקור). */
    static boolean copyFile(File src, File dst, String[] sig) {
        if (dst.exists() || !src.isFile()) return false;
        File parent = dst.getParentFile();
        if (parent != null) parent.mkdirs();
        File tmp = partial(dst);
        if (tmp.exists() && !tmp.delete()) return false;
        try {
            java.security.MessageDigest md = sig == null ? null : java.security.MessageDigest.getInstance("SHA-1");
            java.io.InputStream in = new java.io.FileInputStream(src);
            java.io.FileOutputStream out = new java.io.FileOutputStream(tmp);
            try {
                byte[] buf = new byte[1 << 16];
                int n;
                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                    if (md != null) md.update(buf, 0, n);
                }
                out.getFD().sync();
            } finally {
                in.close();
                out.close();
            }
            if (tmp.length() != src.length() || dst.exists() || !tmp.renameTo(dst)) {
                tmp.delete();
                return false;
            }
            dst.setLastModified(src.lastModified());
            if (md != null) sig[0] = new java.math.BigInteger(1, md.digest()).toString(16);
            return true;
        } catch (Throwable t) {
            tmp.delete();
            Log.e("העתקה", t);
            return false;
        }
    }
}
