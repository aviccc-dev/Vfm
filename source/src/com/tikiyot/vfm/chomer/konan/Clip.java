package com.tikiyot.vfm;

// הלוח (142): "העברה" או "העתקה" — כמו שמקובל באנדרואיד. הקבצים נזכרים כאן, מנווטים במנהל הקבצים,
// ו"הדבקה כאן" מעבירה או מעתיקה אל התיקייה הנוכחית (דרך Physical, עם חלון האישור).
// לוח אחד לכל התוכנה, בזיכרון בלבד (נשכח בסגירת התוכנה). בלי ממשק, ולכן נבדק.

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class Clip {

    private static final List<String> files = new ArrayList<String>(), dirs = new ArrayList<String>();
    private static boolean copy;
    /** אזהרה שתופיע בחלון האישור של ההדבקה (למשל מהכפולים: "כל העותקים נבחרו"). */
    private static String warning;

    private Clip() { }

    /** 149: מסך שיודע להדביק בעצמו (הרשימה החכמה) — אחרי "העתקה/העברה" נשארים בו, והוא מציע איך להדביק. */
    interface Holder {
        void clipped();
    }

    static synchronized void set(List<String> f, List<String> d, boolean isCopy) {
        set(f, d, isCopy, null);
    }

    static synchronized void set(List<String> f, List<String> d, boolean isCopy, String warn) {
        warning = warn;
        files.clear();
        dirs.clear();
        files.addAll(f);
        dirs.addAll(d);
        copy = isCopy;
    }

    static synchronized void clear() {
        files.clear();
        dirs.clear();
    }

    static synchronized boolean isEmpty() {
        return files.isEmpty() && dirs.isEmpty();
    }

    static synchronized String warning() {
        return warning;
    }

    static synchronized boolean isCopy() {
        return copy;
    }

    static synchronized List<String> files() {
        return Collections.unmodifiableList(new ArrayList<String>(files));
    }

    static synchronized List<String> dirs() {
        return Collections.unmodifiableList(new ArrayList<String>(dirs));
    }

    /** "3 קבצים ותיקייה אחת להעברה". */
    static synchronized String label() {
        List<String> w = new ArrayList<String>();
        if (!files.isEmpty()) w.add(Words.files(files.size()));
        if (!dirs.isEmpty()) w.add(Words.folders(dirs.size()));
        StringBuilder sb = new StringBuilder();
        for (String x : w) sb.append(sb.length() > 0 ? Words.ClipW.AND : "").append(x);
        return sb + (copy ? Words.ClipW.TO_COPY : Words.ClipW.TO_MOVE);
    }

    /** האם אפשר להדביק כאן. null = אפשר; אחרת — למה לא (מוצג בפס, במקום הכפתור). */
    static synchronized String cannotPasteInto(File folder) {
        if (folder == null) return Words.ClipW.PICK_FOLDER_FIRST;
        if (Physical.programFile(folder.getAbsolutePath()) != null) return Words.ClipW.IS_APP_FOLDER;
        if (folder.getAbsolutePath().contains("/" + Index.TRASH)) return Words.ClipW.IS_TRASH;
        for (String d : dirs) {
            String p = folder.getAbsolutePath();
            if (p.equals(d) || p.startsWith(d + "/")) return Words.ClipW.NOT_INTO_MOVED;
        }
        if (!copy) {
            boolean allHere = !files.isEmpty() || !dirs.isEmpty();
            for (String f : files) if (!folder.getAbsolutePath().equals(new File(f).getParent())) allHere = false;
            for (String d : dirs) if (!folder.getAbsolutePath().equals(new File(d).getParent())) allHere = false;
            if (allHere) return Words.ClipW.ALREADY_HERE;
        }
        return null;
    }

    /** התיקייה שבה כדאי להתחיל לנווט — של הפריט הראשון. */
    static synchronized File startDir() {
        String first = !files.isEmpty() ? files.get(0) : !dirs.isEmpty() ? dirs.get(0) : null;
        return first == null ? null : new File(first).getParentFile();
    }
}
