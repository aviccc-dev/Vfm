package com.tikiyot.vfm;

// 182: המאגר של אנדרואיד (MediaStore) — רק לשאלה "מה השתנה מאז הנקודה" (מונה השינויים, אנדרואיד 11 ומעלה).
// אחראי: לשאול את המאגר, ולתרגם את המסלולים שלו לכונן של האינדקס. אסור לו: לשנות את האינדקס או קבצים.
// המאגר אינו מחליף את הסריקה (חסרים בו מוסתרים ו־.nomedia — מעבדה 1.4); הוא תופס קבצים שנערכו במקומם.

import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.provider.MediaStore;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class Media {

    private Media() { }

    /** מקור התשובות — באנדרואיד המאגר; בבדיקות במחשב — תחליף. */
    interface Source {
        /** מונה השינויים של הכונן במאגר; -1 — לא נתמך (אנדרואיד שלפני 11, או כונן שהמאגר אינו מכיר). */
        long generation(String volume);

        /** גרסת המאגר של הכונן (משתנה כשהמאגר נבנה מחדש — ואז המונה מתחיל מחדש); "" — לא ידוע. */
        String version(String volume);

        /** המסלולים שנוספו או נערכו מאז המונה since (קבצים בלבד); null — השאלה נכשלה. */
        List<String> changedSince(String volume, long since);

        /** 189: מתי כל קובץ הגיע למכשיר (DATE_ADDED, באלפיות שנייה), לפי המסלול במאגר; null — לא נתמך או נכשל. */
        java.util.Map<String, Long> dateAdded(String volume);
    }

    /** 189: "נוסף" מהמאגר — לפי המסלול בכונן של האינדקס (onDrive). ריק — אין מאגר או אין תשובה. */
    static java.util.Map<String, Long> dates(Source m, Index ix) {
        java.util.Map<String, Long> out = new java.util.HashMap<String, Long>();
        java.util.Map<String, Long> raw = m == null ? null : m.dateAdded(volume(ix));
        if (raw == null) return out;
        for (java.util.Map.Entry<String, Long> e : raw.entrySet()) {
            String p = onDrive(ix, e.getKey());
            if (p != null && e.getValue() != null && e.getValue() > 0) out.put(p, e.getValue());
        }
        return out;
    }

    /** שם הכונן במאגר: הפנימי — "external_primary"; כרטיס — שם התיקייה שלו באותיות קטנות ("1a2b-3c4d"). */
    static String volume(Index ix) {
        return Drive.INTERNAL.equals(ix.key) ? "external_primary" : ix.key.toLowerCase(Locale.ROOT);
    }

    /**
     * מסלול מהמאגר ← מסלול בכונן של האינדקס, או null אם אינו בו. השורש — בלי הבדל בין אותיות גדולות
     * לקטנות (במאגר הכרטיס לפעמים באותיות קטנות); ההמשך — כמו שהוא.
     */
    static String onDrive(Index ix, String path) {
        if (path == null) return null;
        String root = ix.rootPath.endsWith("/") ? ix.rootPath.substring(0, ix.rootPath.length() - 1) : ix.rootPath;
        if (path.length() <= root.length() + 1 || path.charAt(root.length()) != '/') return null;
        if (!path.regionMatches(true, 0, root, 0, root.length())) return null;
        return root + path.substring(root.length());
    }

    // ---------- הנקודה: עד איפה האינדקס יודע את מה שבמאגר ----------

    /**
     * הנקודה של כונן — המונה והגרסה של המאגר, בקובץ טקסט קטן ליד האינדקס (".מערכת/נקודה-<כונן>.txt"),
     * כך שלכל אופן עבודה נקודה משלו, והיא נמחקת ונבנית יחד עם האינדקס.
     */
    static final class Point {
        final long gen;
        final String version;

        Point(long gen, String version) {
            this.gen = gen;
            this.version = version == null ? "" : version;
        }

        static Point read(String key) {
            try {
                File f = Paths.point(key);
                if (!f.exists()) return null;
                String[] p = Files.readUtf8(f).trim().split("\\|", -1);
                return new Point(Long.parseLong(p[0].trim()), p.length > 1 ? p[1].trim() : "");
            } catch (Throwable t) {
                return null;
            }
        }

        /** -1 — לא שומרים (המאגר לא ענה). */
        void save(String key) {
            if (gen < 0) return;
            try {
                Files.writeAtomic(Paths.point(key), gen + "|" + version + "\n");        // תחביר
            } catch (Throwable t) {
                Log.e("שמירת הנקודה " + key, t);
            }
        }
    }

    /** הנקודה של עכשיו (לפני בנייה או בדיקה — מה שישתנה בזמן הסריקה ייתפס בבדיקה הבאה). */
    static Point now(Source m, Index ix) {
        if (m == null) return new Point(-1, "");
        String vol = volume(ix);
        long g = m.generation(vol);
        return new Point(g, g < 0 ? "" : m.version(vol));
    }

    /**
     * 182 (ב): המסלולים שהשתנו במאגר מאז הנקודה השמורה. next — הנקודה לשמור אחרי העדכון. בלי נקודה שמורה,
     * או כשהמאגר נבנה מחדש (גרסה אחרת, או מונה קטן מהשמור) — מתחילים מכאן, בלי רשימה.
     */
    static List<String> since(Source m, Index ix, Point[] next) {
        List<String> none = new ArrayList<String>();
        Point now = now(m, ix);
        next[0] = now;
        if (now.gen < 0) return none;
        Point saved = Point.read(ix.key);
        if (saved == null || !saved.version.equals(now.version) || now.gen < saved.gen || now.gen == saved.gen) return none;
        List<String> l = m.changedSince(volume(ix), saved.gen);
        if (l == null) {                                   // השאלה נכשלה — הנקודה נשארת, ננסה בפעם הבאה
            next[0] = new Point(-1, "");
            return none;
        }
        return l;
    }

    /** המאגר של אנדרואיד. */
    static Source android(final Context c) {
        return new Source() {
            @Override
            public long generation(String volume) {
                if (Build.VERSION.SDK_INT < 30 || c == null) return -1;
                try {
                    if (!MediaStore.getExternalVolumeNames(c).contains(volume)) return -1;
                    return MediaStore.getGeneration(c, volume);
                } catch (Throwable t) {
                    return -1;
                }
            }

            @Override
            public String version(String volume) {
                if (Build.VERSION.SDK_INT < 30 || c == null) return "";
                try {
                    return String.valueOf(MediaStore.getVersion(c, volume));
                } catch (Throwable t) {
                    return "";
                }
            }

            @Override
            @SuppressWarnings("deprecation")
            public List<String> changedSince(String volume, long since) {
                if (Build.VERSION.SDK_INT < 30 || c == null) return null;
                List<String> out = new ArrayList<String>();
                Cursor cur = null;
                try {
                    cur = c.getContentResolver().query(MediaStore.Files.getContentUri(volume),
                            new String[]{MediaStore.MediaColumns.DATA, "format"},
                            MediaStore.MediaColumns.GENERATION_MODIFIED + ">?", new String[]{String.valueOf(since)}, null);
                    if (cur == null) return null;
                    int iFormat = cur.getColumnIndex("format");
                    while (cur.moveToNext()) {
                        if (iFormat >= 0 && cur.getInt(iFormat) == 0x3001) continue;      // שורה של תיקייה
                        String p = cur.getString(0);
                        if (p != null) out.add(p);
                    }
                    return out;
                } catch (Throwable t) {
                    Log.e("המאגר של אנדרואיד — מה השתנה מאז", t);
                    return null;
                } finally {
                    if (cur != null) cur.close();
                }
            }

            @Override
            @SuppressWarnings("deprecation")
            public java.util.Map<String, Long> dateAdded(String volume) {
                if (Build.VERSION.SDK_INT < 30 || c == null) return null;
                java.util.Map<String, Long> out = new java.util.HashMap<String, Long>();
                Cursor cur = null;
                try {
                    if (!MediaStore.getExternalVolumeNames(c).contains(volume)) return null;
                    cur = c.getContentResolver().query(MediaStore.Files.getContentUri(volume),
                            new String[]{MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.DATE_ADDED, "format"},
                            null, null, null);
                    if (cur == null) return null;
                    int iFormat = cur.getColumnIndex("format");
                    while (cur.moveToNext()) {
                        if (iFormat >= 0 && cur.getInt(iFormat) == 0x3001) continue;      // תיקייה
                        String p = cur.getString(0);
                        long sec = cur.getLong(1);
                        if (p != null && sec > 0) out.put(p, sec * 1000L);
                    }
                    return out;
                } catch (Throwable t) {
                    Log.e("המאגר של אנדרואיד — מתי נוסף", t);
                    return null;
                } finally {
                    if (cur != null) cur.close();
                }
            }
        };
    }
}
