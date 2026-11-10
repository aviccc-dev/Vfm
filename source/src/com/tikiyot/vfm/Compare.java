package com.tikiyot.vfm;

// 1.20.0: השוואה בין שתי תיקיות לפי מסלול (שלב 3 של שילוב הגיבוי). כל קובץ ותיקייה מזוהים לפי המסלול
// שלהם יחסית לשורש של הצד שלהם. התוצאה: מה רק במקור (חדש, או שתוכנו שונה), מה רק ביעד (נמחק, או הגרסה
// הישנה של מה שהשתנה), וכמה זהים. תיקייה שקיימת רק בצד אחד — שורה אחת (התוכן שלה עובר איתה).
// המשפך: גודל (חינם) ← תאריך שינוי (חינם) ← חתימה חלקית (מהאינדקס כשהיא תקפה, אחרת קריאה). מה שלא השתנה — לא נקרא.
// אסור לו: לשנות דבר בכונן. קורא בלבד.

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

final class Compare {

    private Compare() { }

    static final class Result {
        String a, b;
        /** רק במקור: קבצים (חדשים או שתוכנם שונה) ותיקיות שאין ביעד. */
        final List<String> onlyA = new ArrayList<String>(), onlyADirs = new ArrayList<String>();
        /** רק ביעד: קבצים (שנמחקו, או הגרסה הישנה) ותיקיות שאין במקור. */
        final List<String> onlyB = new ArrayList<String>(), onlyBDirs = new ArrayList<String>();
        /** זהים; מתוכם — כמה דרשו קריאה (תאריך שונה, אותו גודל); קבצים בכל צד. */
        int same, read, filesA, filesB;
        /** שורה שמשתמשת בתוצאה נכשלה — השורות הבאות לא ימשיכו עליה. */
        boolean broken;
        /** 1.22.0: מקור שהוא רשימה — לכל קובץ ותיקייה במקור, המסלול שלו (<מפתח כונן>/<מסלול מתחילת הכונן>). */
        Map<String, String> relA;

        /** כמה קבצים ברשימה, כולל מה שבתוך התיקיות. */
        static int files(List<String> files, List<String> dirs) {
            int n = files.size();
            for (String d : dirs) n += countFiles(new File(d));
            return n;
        }
    }

    /**
     * משווה את a (המקור) מול b (היעד). b שאינו קיים — ריק (למשל תיקייה ש־MD בשורה קודמת ייצור).
     * hidden — גם מוסתרים. קבצים זמניים של העתקה — אף פעם.
     */
    static Result byPath(Library lib, File a, File b, boolean hidden) {
        Result r = new Result();
        r.a = a.getAbsolutePath();
        r.b = b.getAbsolutePath();
        walk(lib, r, a, b, hidden);
        return r;
    }

    private static void walk(Library lib, Result r, File da, File db, boolean hidden) {
        File[] ka = list(da, hidden), kb = list(db, hidden);
        int i = 0, j = 0;
        while (i < ka.length || j < kb.length) {
            int c = i >= ka.length ? 1 : j >= kb.length ? -1 : ka[i].getName().compareTo(kb[j].getName());
            if (c < 0) {
                onlyA(r, ka[i++]);
            } else if (c > 0) {
                onlyB(r, kb[j++]);
            } else {
                File x = ka[i++], y = kb[j++];
                boolean dx = x.isDirectory(), dy = y.isDirectory();
                if (dx && dy) {
                    walk(lib, r, x, y, hidden);
                } else if (dx || dy) {                                         // קובץ מול תיקייה — שניהם "רק"
                    onlyA(r, x);
                    onlyB(r, y);
                } else {
                    r.filesA++;
                    r.filesB++;
                    if (same(lib, r, x, y)) r.same++;
                    else {
                        r.onlyA.add(x.getAbsolutePath());
                        r.onlyB.add(y.getAbsolutePath());
                    }
                }
            }
        }
    }

    private static void onlyA(Result r, File f) {
        if (f.isDirectory()) {
            r.onlyADirs.add(f.getAbsolutePath());
            r.filesA += countFiles(f);
        } else {
            r.onlyA.add(f.getAbsolutePath());
            r.filesA++;
        }
    }

    private static void onlyB(Result r, File f) {
        if (f.isDirectory()) {
            r.onlyBDirs.add(f.getAbsolutePath());
            r.filesB += countFiles(f);
        } else {
            r.onlyB.add(f.getAbsolutePath());
            r.filesB++;
        }
    }

    /** המשפך: גודל ← תאריך ← חתימה חלקית. */
    static boolean same(Library lib, Result r, File x, File y) {
        long n = x.length();
        if (n != y.length()) return false;
        if (x.lastModified() == y.lastModified()) return true;
        r.read++;
        long sx = sig(lib, x), sy = sig(lib, y);
        return sx != 0 && sx == sy;
    }

    /** חתימה חלקית: מהאינדקס כשהיא תקפה לגודל ולתאריך של הקובץ עכשיו, אחרת — מחושבת. */
    private static long sig(Library lib, File f) {
        if (lib != null) {
            Library.Hit h = lib.find(f.getAbsolutePath());
            if (h != null && h.rec != null && h.rec.sig != 0 && h.rec.size == f.length() && h.rec.mtime == f.lastModified())
                return h.rec.sig;
        }
        return Sig.ofLong(f);
    }

    /** הילדים, ממוינים לפי שם. מוסתרים — רק כשביקשו; קבצים זמניים של העתקה — אף פעם. */
    private static File[] list(File d, boolean hidden) {
        File[] k = d.isDirectory() ? d.listFiles() : null;
        if (k == null) return new File[0];
        List<File> out = new ArrayList<File>();
        for (File x : k) {
            String n = x.getName();
            if (Physical.isPartial(n)) continue;
            if (!hidden && n.startsWith(".")) continue;
            out.add(x);
        }
        File[] arr = out.toArray(new File[0]);
        Arrays.sort(arr, new java.util.Comparator<File>() {
            @Override public int compare(File p, File q) { return p.getName().compareTo(q.getName()); }
        });
        return arr;
    }

    static int countFiles(File d) {
        File[] k = d.listFiles();
        if (k == null) return 0;
        int n = 0;
        for (File x : k) n += x.isDirectory() ? countFiles(x) : (Physical.isPartial(x.getName()) ? 0 : 1);
        return n;
    }

    // ================= 1.22.0: רשימה כמקור =================

    /** מה שברשימה: כל קובץ ← המסלול שלו; תיקיות ריקות (מתיקיות מתעדכנות) ← המסלול שלהן. */
    static final class Source {
        final Map<String, String> files = new LinkedHashMap<String, String>(), emptyDirs = new LinkedHashMap<String, String>();
        /** קבצים מחוץ לכוננים המוכרים (לא נכללים). */
        int outside;
        /** רשימות חכמות שלא נמצאו (נמחקו). */
        int skippedSmart;
    }

    /** מ0.25: קובץ אחד למקור (לרשימה מהמסד — DbListCmd.compSource). */
    static void addFile(Library lib, Source src, File f, String skip) {
        if (f.isFile() && !Physical.isPartial(f.getName())) add(lib, src, f, skip);
    }

    private static void add(Library lib, Source src, File f, String skip) {
        String p = f.getAbsolutePath();
        if (skip != null && p.startsWith(skip + "/")) return;
        if (src.files.containsKey(p)) return;
        String rel = relOf(lib, p);
        if (rel == null) src.outside++;
        else src.files.put(p, rel);
    }

    /** "<מפתח כונן>/<מסלול מתחילת הכונן>", או null — מחוץ לכוננים. */
    static String relOf(Library lib, String p) {
        String root = lib.rootOf(p), key = lib.keyOf(p);
        if (root == null || key == null) return null;
        String rel = Physical.rel(root, p);
        if (rel == null) return null;
        return rel.isEmpty() ? key : key + "/" + rel;
    }

    /** השוואה של רשימה (Source) מול תיקייה b — אותו משפך, לפי המסלולים של הרשימה. */
    static Result byList(Library lib, Source src, File b, boolean hidden) {
        Result r = new Result();
        r.b = b.getAbsolutePath();
        r.relA = new HashMap<String, String>();
        TreeMap<String, String> files = new TreeMap<String, String>();        // מסלול ← קובץ
        Set<String> dirs = new HashSet<String>();
        for (Map.Entry<String, String> e : src.files.entrySet()) {
            files.put(e.getValue(), e.getKey());
            r.relA.put(e.getKey(), e.getValue());
            parents(e.getValue(), dirs);
        }
        for (Map.Entry<String, String> e : src.emptyDirs.entrySet()) {
            dirs.add(e.getValue());
            parents(e.getValue(), dirs);
            r.relA.put(e.getKey(), e.getValue());
        }
        r.filesA = files.size();
        Set<String> matched = new HashSet<String>(), seenDirs = new HashSet<String>();
        walkB(lib, r, b, "", files, dirs, matched, seenDirs, hidden);
        for (Map.Entry<String, String> e : files.entrySet())
            if (!matched.contains(e.getKey())) r.onlyA.add(e.getValue());
        for (Map.Entry<String, String> e : src.emptyDirs.entrySet())
            if (!seenDirs.contains(e.getValue())) r.onlyADirs.add(e.getKey());
        return r;
    }

    private static void parents(String rel, Set<String> dirs) {
        for (int i = rel.indexOf('/'); i > 0; i = rel.indexOf('/', i + 1)) dirs.add(rel.substring(0, i));
    }

    private static void walkB(Library lib, Result r, File d, String rel, TreeMap<String, String> files, Set<String> dirs,
                              Set<String> matched, Set<String> seenDirs, boolean hidden) {
        for (File y : list(d, hidden)) {
            String ry = rel.isEmpty() ? y.getName() : rel + "/" + y.getName();
            if (y.isDirectory()) {
                if (dirs.contains(ry)) {
                    seenDirs.add(ry);
                    walkB(lib, r, y, ry, files, dirs, matched, seenDirs, hidden);
                } else onlyB(r, y);
                continue;
            }
            String x = files.get(ry);
            if (x == null) {
                onlyB(r, y);
                continue;
            }
            matched.add(ry);
            r.filesB++;
            if (same(lib, r, new File(x), y)) r.same++;
            else {
                r.onlyA.add(x);
                r.onlyB.add(y.getAbsolutePath());
            }
        }
    }
}
