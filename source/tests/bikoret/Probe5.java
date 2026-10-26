package com.tikiyot.vfm;

// בדיקות גישוש לביקורת השישית (קוד חדש וחצאי באגים) — לאמת ממצאים, לא חלק מהפרויקט.

import java.io.File;
import java.util.*;

public final class Probe5 {

    static File home;

    static File mk(File dir, String name, int bytes) throws Exception {
        dir.mkdirs();
        File f = new File(dir, name);
        java.io.FileOutputStream o = new java.io.FileOutputStream(f);
        o.write(new byte[bytes]);
        o.close();
        return f;
    }

    static void wipe(File f) {
        File[] k = f.listFiles();
        if (k != null) for (File x : k) wipe(x);
        f.delete();
    }

    static void say(String s) { System.out.println(s); }

    public static void main(String[] a) throws Exception {
        home = new File(System.getProperty("vfm.home"));
        mdThenMove();
        renThenCopy();
        filterOnDir();
        zipSelf();
        tarGz();
        formSlash();
        baseName();
        indexSearchPerf();
        System.exit(0);
    }

    // 1: MD ואז העברה אליה — האם התכנון מכיר את מה שיווצר בשורה הקודמת
    static void mdThenMove() throws Exception {
        File root = new File(home, "כונן-ב1");
        wipe(root);
        File dl = new File(root, "Download");
        mk(dl, "א.pdf", 10);
        Map<String, String> vars = Batch.vars(root, root.getAbsolutePath(), null);
        Batch.Plan p = Batch.plan("MD %פנימי%\\חדשה\n" + "MOVE %הורדות%\\*.pdf %פנימי%\\חדשה\n", root, vars);
        Batch.Step mv = p.steps.get(1);
        say("1. MD ואז MOVE אליה — שגיאת תכנון: " + (mv.error == null ? "אין" : "\"" + mv.error + "\"") + " · שגיאות בתוכנית: " + p.errors);
    }

    // 2: שינוי שם ואז העתקה של השם החדש
    static void renThenCopy() throws Exception {
        File root = new File(home, "כונן-ב2");
        wipe(root);
        File dl = new File(root, "Download");
        mk(dl, "ישן.pdf", 10);
        new File(root, "יעד").mkdirs();
        Map<String, String> vars = Batch.vars(root, root.getAbsolutePath(), null);
        Batch.Plan p = Batch.plan("REN %הורדות%\\ישן.pdf חדש.pdf\n" + "COPY %הורדות%\\חדש.pdf %פנימי%\\יעד\n", root, vars);
        Batch.Step cp = p.steps.get(1);
        say("2. REN ואז COPY של השם החדש — שגיאת תכנון: " + (cp.error == null ? "אין" : "\"" + cp.error + "\""));
    }

    // 3: /תאריך על תיקייה שלמה (MOVE של תיקייה) — האם הסינון מתעלמים ממנו בשקט
    static void filterOnDir() throws Exception {
        File root = new File(home, "כונן-ב3");
        wipe(root);
        File dir = new File(root, "ספרים");
        File old = mk(dir, "ישן.pdf", 10);
        old.setLastModified(System.currentTimeMillis() - 900L * 24 * 3600 * 1000);
        new File(root, "יעד").mkdirs();
        Map<String, String> vars = Batch.vars(root, root.getAbsolutePath(), null);
        Batch.Plan p = Batch.plan("MOVE %פנימי%\\ספרים %פנימי%\\יעד /תאריך:שבוע\n", root, vars);
        Batch.Step s = p.steps.get(0);
        say("3. MOVE של תיקייה עם /תאריך:שבוע (הקובץ בה בן שנתיים): שגיאה=" + s.error + " · תיקיות=" + s.dirs.size() + " · \"" + s.describe + "\" · הסינון נאמר? " + s.describe.contains("סינון"));
    }

    // 4: אריזת תיקייה כשקובץ ה-ZIP נכתב בתוכה (כמו שליחת תיקיית התוכנה עצמה)
    static void zipSelf() throws Exception {
        File root = new File(home, "כונן-ב4");
        wipe(root);
        File prog = new File(root, "מנהל קבצים וירטואלי");
        mk(prog, "מבנה.txt", 5000);
        mk(new File(prog, ".מערכת"), "אינדקס.bin", 20000);
        File out = new File(prog, ".מערכת/שליחה/מנהל קבצים וירטואלי.zip");
        Zip.Result r = Zip.pack(prog, out, null);
        java.util.zip.ZipFile z = new java.util.zip.ZipFile(out);
        int n = 0;
        boolean tmpInside = false, zipInside = false;
        for (Enumeration<? extends java.util.zip.ZipEntry> en = z.entries(); en.hasMoreElements(); ) {
            java.util.zip.ZipEntry e = en.nextElement();
            n++;
            if (e.getName().endsWith(".tmp")) tmpInside = true;
            if (e.getName().endsWith(".zip")) zipInside = true;
        }
        z.close();
        say("4. אריזת תיקייה כשה-ZIP נכתב בתוכה: " + n + " רשומות · הקובץ הזמני נארז לתוך עצמו=" + tmpInside + " · zip בתוך zip=" + zipInside + " · קבצים=" + r.files + " · בתים=" + r.bytes + " · גודל ה-ZIP=" + out.length());
    }

    // 5: סוג מורכב — tar.gz (הרגרסיה של 104)
    static void tarGz() throws Exception {
        File root = new File(home, "כונן-ב5");
        wipe(root);
        mk(root, "גיבוי.tar.gz", 10);
        mk(root, "ספר.pdf", 10);
        mk(root, "photo.heic (1)", 10);
        Index ix = new Index("ב5", "ב5", root.getAbsolutePath());
        ix.scan(null);
        Library lib = Library.forTests(new ArrayList<Index>(Collections.singletonList(ix)));
        say("5. סוג:tar.gz מוצא " + lib.search("סוג:tar.gz", false, false).total + " (לפני 104 היה 1) · סוג:gz מוצא " + lib.search("סוג:gz", false, false).total
                + " · Text.ext(\"photo.heic (1)\")=\"" + Text.ext("photo.heic (1)") + "\" · סוג:heic מוצא " + lib.search("סוג:heic", false, false).total);
    }

    // 6: הטופס מאבד /S משורת DEL
    static void formSlash() {
        String[] lines = {"DEL %הורדות%\\*.pdf /S", "COPY a b /S", "RD %פנימי%\\x /S", "DEL a /גודל:10MB"};
        for (String l : lines) {
            String back = BatchLine.parse(l).toLine();
            say("6. טופס: \"" + l + "\" ← \"" + back + "\"" + (back.contains("/S") == l.contains("/S") ? "" : "   ← /S אבד") + (l.contains("גודל:10MB") && !back.contains(">") ? "   ← גודל בלי סימן הפך ל־" + back.replaceAll(".*(/גודל:\\S+).*", "$1") : ""));
        }
    }

    // 7: שם מוצג בשורה
    static void baseName() {
        try {
            say("7. Ui.baseName(\"פרק 1.17\")=\"" + Ui.baseName("פרק 1.17", false) + "\" · (\"ספר.pdf (1)\")=\"" + Ui.baseName("ספר.pdf (1)", false) + "\" · (\"ספר.pdf\")=\"" + Ui.baseName("ספר.pdf", false) + "\"");
        } catch (Throwable t) {
            say("7. Ui.baseName — לא ניתן להריץ כאן (" + t.getClass().getSimpleName() + ")");
        }
        say("   Physical.freeName(\"פרק 1.17\" תפוס)=" + freeNameOf("פרק 1.17"));
    }

    static String freeNameOf(String n) {
        try {
            File d = new File(home, "ב7");
            wipe(d);
            mk(d, n, 1);
            return "\"" + Physical.freeName(d, n, false) + "\"";
        } catch (Throwable t) {
            return "?";
        }
    }

    // 8: זמן חיפוש בבורר התיקיות על הרבה תיקיות
    static void indexSearchPerf() {
        for (int n : new int[]{3000, 30000}) {
            Index ix = new Index("internal", "זיכרון", "/storage/emulated/0");
            Random rnd = new Random(3);
            String[] w = {"ספרים", "תורה", "מחשבה", "הלכה", "רמחל", "לימוד", "חומש", "גמרא"};
            int[] depth = new int[n];
            for (int i = 0; i < n; i++) {
                Index.Dir d = new Index.Dir();
                int par = -1;
                if (i >= 12) { do { par = rnd.nextInt(i); } while (depth[par] >= 5); }
                d.parent = par;
                depth[i] = par < 0 ? 0 : depth[par] + 1;
                d.name = w[rnd.nextInt(w.length)] + " " + i;
                d.norm = Text.norm(d.name);
                ix.dirs.add(d);
            }
            List<IndexSource.Drv> drv = new ArrayList<IndexSource.Drv>();
            drv.add(new IndexSource.Drv("internal", "זיכרון", "/storage/emulated/0", ix));
            LinkedHashMap<ArrayList<String>, String> roots = new LinkedHashMap<ArrayList<String>, String>();
            roots.put(new ArrayList<String>(Arrays.asList("internal")), "זיכרון");
            PickerModel m = new PickerModel(new IndexSource(drv), roots, null, null);
            m.search("זזזזז");                                        // חימום: בניית המפה
            long t0 = System.nanoTime();
            for (int i = 0; i < 5; i++) m.search("זזזזז");             // לא נמצא כלום: עובר על הכול
            long t1 = System.nanoTime();
            m.search("רמחל");
            long t2 = System.nanoTime();
            long t3 = System.nanoTime();
            m.folderRows();
            long t4 = System.nanoTime();
            say(String.format(Locale.US, "8. בורר תיקיות, %d תיקיות: חיפוש שלא מוצא %.0f מ\"ש להקשה · חיפוש שמוצא %.0f מ\"ש · תצוגת תיקיות %.1f מ\"ש (במחשב)",
                    n, (t1 - t0) / 5e6, (t2 - t1) / 1e6, (t4 - t3) / 1e6));
        }
    }
}
