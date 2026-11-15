package com.tikiyot.vfm;

// מבנה 158 (סידור תיקיית הפקודות): ההדמיה — מצב הכונן והמסד כפי שיהיו אחרי השורות הקודמות, כדי שהתכנון של כל שורה
// יכיר את מה שקודמותיה ייצרו, יזיזו או ימחקו (118). עד כאן ישב בתוך Batch. אחראי: לזכור מה ישתנה. אסור לו: לשנות דבר.

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * מה שהשורות הקודמות ייצרו או יזיזו — כדי שהתכנון יכיר אותן לפני שבוצעו (118): תיקייה מ-MD,
 * קובץ שהועבר או הועתק ליעד, שם חדש מ-REN, ומה שנמחק או הועבר ממקומו.
 */
final class Sim {
    final java.util.Set<String> dirs = new java.util.HashSet<String>();
    final java.util.Set<String> gone = new java.util.HashSet<String>();
    /**
     * מ0.27: תיקיות ש־RD מחק בשורה קודמת — כל מה שהיה בהן בכונן כבר לא נחשב (גם אם התיקייה נוצרת שוב ב־MD):
     * רק מה שהשורות שאחרי יוצרות. בלי זה MD אחרי RD /S נחשב "כבר קיימת" ולא רץ, ו־COMP השווה מול מה שנמחק.
     */
    final java.util.Set<String> wiped = new java.util.HashSet<String>();

    boolean underWiped(String p) {
        for (String w : wiped) if (p.equals(w) || p.startsWith(w + "/")) return true;
        return false;
    }

    /** מ0.32 (ממצא 44): יש בתיקייה תת־תיקייה — לפי ההדמיה (מה ששורות קודמות מחקו או יצרו), לא רק לפי הכונן. */
    /** מ0.38: התיקיות שבתוך d — בכונן, ומה ששורות קודמות יצרו; בלי מה שנמחק. לפי השם. */
    List<File> subdirs(File d) {
        java.util.TreeMap<String, File> out = new java.util.TreeMap<String, File>();
        String dp = d.getAbsolutePath();
        File[] k = underWiped(dp) ? null : d.listFiles();
        if (k != null) for (File x : k) if (x.isDirectory() && !gone.contains(x.getAbsolutePath())) out.put(x.getName(), x);
        String in = dp + "/";
        for (String x : dirs) if (x.startsWith(in) && x.indexOf('/', in.length()) < 0 && !gone.contains(x)) out.put(x.substring(in.length()), new File(x));
        return new ArrayList<File>(out.values());
    }

    boolean hasSubdirs(File d) {
        return !subdirs(d).isEmpty();                                       // מבנה 43: אותה רשימה (עד כאן: תיקייה ש־MD יצר ו־MOVE הוציא עוד נספרה)
    }

    /** RD: התיקייה וכל מה שבתוכה — יוצאים מההדמיה. */
    void wipe(String d) {
        wiped.add(d);
        gone.add(d);
        String in = d + "/";
        for (java.util.Iterator<String> it = dirs.iterator(); it.hasNext(); ) {
            String x = it.next();
            if (x.equals(d) || x.startsWith(in)) it.remove();
        }
        for (java.util.Iterator<String> it = added.keySet().iterator(); it.hasNext(); ) if (it.next().startsWith(in)) it.remove();
    }

    /**
     * מבנה 44: העברה של תיקייה שלמה — מה שבתוכה (כמו שההדמיה רואה אותו עכשיו) עובר איתה: במקום הישן אין עוד דבר,
     * ובחדש — הכול. עד כאן נרשמה רק התיקייה עצמה: שורה שאחריה עוד ראתה את הקבצים במקום הישן (ותכננה למחוק או
     * להעתיק קובץ שכבר לא יהיה שם), ולא מצאה אותם במקום החדש ("לא נמצא" — והקובץ כולו לא רץ). replace — "להחליף":
     * מה שהיה ביעד בשם הזה הולך לסל. המחיר: מעבר אחד על התיקייה בזמן התכנון, ורק בשורה שמעבירה תיקייה שלמה.
     */
    void moveDir(File from, File to, boolean replace) {
        List<File> files = new ArrayList<File>(), subs = new ArrayList<File>();
        tree(from, files, subs);
        String a = from.getAbsolutePath(), b = to.getAbsolutePath();
        Map<String, File> src = new LinkedHashMap<String, File>();
        for (File f : files) src.put(f.getAbsolutePath(), real(f));       // לפני שהמקום הישן נמחק מההדמיה
        if (replace && isDir(to)) wipe(b);
        wipe(a);
        gone.remove(b);
        dirs.add(b);
        for (File d : subs) {
            String p = b + d.getAbsolutePath().substring(a.length());
            gone.remove(p);
            dirs.add(p);
        }
        Map<String, String> moved = new LinkedHashMap<String, String>();
        for (Map.Entry<String, File> e : src.entrySet()) {
            String p = b + e.getKey().substring(a.length());
            gone.remove(p);
            added.put(p, e.getValue());
            moved.put(e.getKey(), p);
        }
        if (db != null && !moved.isEmpty()) DbFollow.moved(db, moved);     // כמו בהעברת קובץ: הרשימות במסד עוקבות גם בהדמיה
    }

    /** מבנה 67: העתקה של תיקייה שלמה — מה שבתוכה מופיע גם במקום החדש; המקור נשאר. */
    void copyDir(File from, File to) {
        List<File> files = new ArrayList<File>(), subs = new ArrayList<File>();
        tree(from, files, subs);
        String a = from.getAbsolutePath(), b = to.getAbsolutePath();
        gone.remove(b);
        dirs.add(b);
        for (File d : subs) {
            String p = b + d.getAbsolutePath().substring(a.length());
            gone.remove(p);
            dirs.add(p);
        }
        for (File f : files) {
            String p = b + f.getAbsolutePath().substring(a.length());
            gone.remove(p);
            added.put(p, real(f));
        }
    }

    private void tree(File d, List<File> files, List<File> subs) {
        files.addAll(files(d));
        for (File k : subdirs(d)) {
            subs.add(k);
            tree(k, files, subs);
        }
    }

    /** קובץ שיהיה ← הקובץ שממנו הוא בא (לגודל ולתאריך). */
    final Map<String, File> added = new LinkedHashMap<String, File>();
    /** 1.20.0: ההשוואה האחרונה (COMP), והאינדקס לחתימות שכבר חושבו (יכול להיות null). */
    Compare.Result compare;
    /** מבנה 73: מה שנבחר במסך (null — הקובץ לא הורץ על בחירה). */
    Batch.Selection selected;
    Library lib;
    /** מ0.12: כשעובדים במסד — עותק של המסד בזיכרון (DbListCmd), והקבצים לכל תנאי; null — עוד לא נטען. לא נשמר. */
    Store db;
    /** מ0.32 (ממצא 38): עותק המסד של כל אופן שכבר טופל בקובץ — חזרה לאופן לא שוכחת מה שהשורות הקודמות עשו בו. */
    final java.util.Map<String, Store> dbs = new java.util.HashMap<String, Store>();
    /** מ0.42: האופנים ש"צור אופן" יוצר בקובץ (בהדמיה — מסד ריק עם בית ב־dbs). */
    final java.util.Set<String> created = new java.util.HashSet<String>();
    /** מ0.45: אופנים ש"מחק אופן" שלפני מחק — "אופן" אחריו נכשל בתכנון. */
    final java.util.Set<String> dropped = new java.util.HashSet<String>();

    boolean hasMode(String m) {
        return created.contains(m) || (!dropped.contains(m) && Db.dbModes(Db.app).contains(m));
    }

    /** מבנה 66 (ממצא 29): האופנים כמו שהקובץ רואה אותם בשורה הזו — להודעה "אין מסד לאופן … — יש: …". */
    List<String> modes() {
        return modes(Db.dbModes(Db.app), created, dropped);
    }

    /** מה שיש במכשיר, בלי מה ששורות קודמות ביטלו, ועם מה שהן יצרו (בסוף, לפי השם). */
    static List<String> modes(List<String> real, java.util.Set<String> created, java.util.Set<String> dropped) {
        List<String> out = new ArrayList<String>();
        for (String m : real) if (!dropped.contains(m) || created.contains(m)) out.add(m);
        for (String m : new java.util.TreeSet<String>(created)) if (!out.contains(m)) out.add(m);
        return out;
    }
    /** מ0.15: האופן מ"אופן" בקובץ; null — האופן שפתוח. */
    String mode;
    /** מ0.17.1: "אופן" שנכשל — כל שורות המסד שאחריו נכשלות (לא לרוץ בטעות על אופן אחר). */
    String modeError;
    final Map<String, List<String>> condFiles = new java.util.HashMap<String, List<String>>();

    boolean isDir(File d) {
        String p = d.getAbsolutePath();
        if (gone.contains(p)) return false;
        return dirs.contains(p) || (d.isDirectory() && !underWiped(p));
    }

    boolean isFile(File f) {
        String p = f.getAbsolutePath();
        return added.containsKey(p) || (!gone.contains(p) && f.isFile() && !underWiped(p));
    }

    /** המקור האמיתי (לגודל ולתאריך). */
    File real(File f) {
        File r = added.get(f.getAbsolutePath());
        return r != null ? r : f;
    }

    /** הקבצים שיהיו בתיקייה, ממוינים. */
    List<File> files(File dir) {
        java.util.TreeMap<String, File> out = new java.util.TreeMap<String, File>();
        File[] all = underWiped(dir.getAbsolutePath()) ? null : dir.listFiles();
        if (all != null)
            for (File x : all)
                if (x.isFile() && !gone.contains(x.getAbsolutePath())) out.put(x.getName(), x);
        String d = dir.getAbsolutePath() + "/";
        for (String p : added.keySet())
            if (p.startsWith(d) && p.indexOf('/', d.length()) < 0) out.put(p.substring(d.length()), new File(p));
        return new ArrayList<File>(out.values());
    }

    /**
     * מ0.40: האם יש בתיקייה (כמו שתהיה) קובץ זהה ל־src. המשפך: הגודל (חינם) מוציא את רובם; חתימה — רק לשווי גודל,
     * וחתימת המקור — פעם אחת. חתימה שלא נקראה — ספק, לא "זהה".
     */
    boolean sameIn(File src, File dir) {
        File s = real(src);
        long n = s.length();
        String sig = null;
        for (File g : files(dir)) {
            File r = real(g);
            if (r.length() != n) continue;
            if (sig == null && (sig = Sig.of(s)) == null) return false;
            if (sig.equals(Sig.of(r))) return true;
        }
        return false;
    }

    /** השם הפנוי הבא לקובץ to בתיקייה שלו, כמו Physical.freeName — אבל לפי ההדמיה. */
    File free(File to) {
        File dir = to.getParentFile();
        String name = to.getName(), base = Text.stem(name), ext = Text.tail(name);
        if (base.isEmpty()) { base = name; ext = ""; }
        File f = to;
        for (int i = 2; isFile(f) || isDir(f); i++) f = new File(dir, base + " (" + i + ")" + ext);
        return f;
    }

    void move(String from, File to, boolean copy) {
        File src = real(new File(from));
        if (!copy) {
            gone.add(from);
            added.remove(from);
        }
        gone.remove(to.getAbsolutePath());
        added.put(to.getAbsolutePath(), src);
        if (!copy && db != null)                                           // מ0.23: גם בהדמיה — הרשימות במסד עוקבות
            DbFollow.moved(db, java.util.Collections.singletonMap(from, to.getAbsolutePath()));
    }
}
