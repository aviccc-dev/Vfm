package com.tikiyot.vfm;

// בדיקות גישוש לביקורת — לאמת ממצאים בקוד, לא חלק מהפרויקט.
// הרצה: כמו build/test.sh, עם הקבצים האלה במקום tests/com; פרמטר = מספר הבדיקה (6 — כמשתמש שאינו root). הממצאים: shipurim.md, 86–102.

import java.io.File;
import java.util.*;

public final class Probe {

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
        String only = a.length > 0 ? a[0] : "";
        if (only.isEmpty() || only.equals("1")) sameLeafNames();
        if (only.isEmpty() || only.equals("2")) structureRace();
        if (only.isEmpty() || only.equals("3")) updateFuncDrops();
        if (only.isEmpty() || only.equals("4")) renamedBase();
        if (only.isEmpty() || only.equals("5")) deleteProgramFile();
        if (only.equals("6")) readFailure();
        if (only.isEmpty() || only.equals("7")) backupRotation();
        if (only.isEmpty() || only.equals("8")) badCondition();
        if (only.isEmpty() || only.equals("9")) searchPaths();
        if (only.isEmpty() || only.equals("10")) trashLog();
        if (only.isEmpty() || only.equals("11")) similarCost();
        if (only.isEmpty() || only.equals("12")) treeRace();
        if (only.isEmpty() || only.equals("13")) zeroSize();
        System.exit(0);
    }

    // 1: "באילו רשימות" — שתי רשימות באותו שם בענפים שונים
    static void sameLeafNames() {
        File tf = new File(home, "p1.txt");
        tf.delete();
        Tree.testFile = tf;
        Tree t = Tree.load();
        Tree.Node m = Tree.newChild(t.root, "מועדים");
        Tree.newChild(m, "פסח");
        Tree.Node r = Tree.newChild(t.root, "מתכונים");
        Tree.newChild(r, "פסח");
        t.save();
        String f = "/storage/x/עוגה.pdf";
        Tree.addFiles(Arrays.asList("מועדים", "פסח"), Arrays.asList(f));
        Tree.addFiles(Arrays.asList("מתכונים", "פסח"), Arrays.asList(f));
        say("1. באילו רשימות (הקובץ בשתי רשימות): " + Tree.listsOf(f)
                + " · countEverywhere=" + Tree.countEverywhere(Arrays.asList(f)));
        Tree.testFile = null;
    }

    // 2: שני חוטים משנים שני מפתחות באותו כלי, במקביל (כמו \"הגדרות החיפוש\")
    static void structureRace() throws Exception {
        File sf = new File(home, "p2.ini");
        int lost = 0, runs = 150;
        for (int i = 0; i < runs; i++) {
            sf.delete();
            Structure.testFile = sf;
            Structure.addDirectFunc("חיפוש", "חיפוש", "", Target.of("חיפוש"));
            final String id = Structure.load().direct.get(0).id;
            Thread x = new Thread(new Runnable() { public void run() { Structure.setKey(id, "מסלול", "כן"); } });
            Thread y = new Thread(new Runnable() { public void run() { Structure.setKey(id, "כתיב", "לא"); } });
            x.start(); y.start(); x.join(); y.join();
            Structure.Func f = Structure.func(id);
            if (!"כן".equals(f.params.get("מסלול")) || !"לא".equals(f.params.get("כתיב"))) lost++;
        }
        say("2. שינוי שתי הגדרות במקביל: אחת אבדה ב-" + lost + " מתוך " + runs + " ניסיונות");
        Structure.testFile = null;
    }

    // 3: עריכת כלי בטופס — שורה שהתוכנה לא מכירה
    static void updateFuncDrops() throws Exception {
        File sf = new File(home, "p3.ini");
        sf.delete();
        Structure.testFile = sf;
        Structure.addDirectFunc("ספרים", "חכמה", "", Target.of("חכמה", "תנאי", "סוג:pdf"));
        String id = Structure.load().direct.get(0).id;
        Ini ini = Structure.ini();
        int at = -1;
        for (int i = 0; i < ini.lines.size(); i++) if (ini.lines.get(i).isHeader() && ini.lines.get(i).headerName().startsWith("פונקציה")) at = i;
        ini.lines.add(at + 1, Ini.line("; הערה שלי על הכלי", "פונקציה: ספרים"));
        ini.lines.add(at + 2, Ini.line("הערה = לקריאה בשבת", "פונקציה: ספרים"));
        ini.save();
        String before = Files.readUtf8(sf);
        Structure.updateFunc(id, "ספרים", "", Target.of("חכמה", "תנאי", "סוג:pdf|epub", "מיון", "שם"));
        String after = Files.readUtf8(sf);
        say("3. לפני העריכה: הערה=" + before.contains("הערה = לקריאה") + ", שורת ; =" + before.contains("; הערה שלי")
                + " · אחרי: הערה=" + after.contains("הערה = לקריאה") + ", שורת ; =" + after.contains("; הערה שלי"));
        Structure.testFile = null;
    }

    // 4: כלי שעובד על רשימה קיימת בתוך רשימה אחרת — ושינוי השם שלה
    static void renamedBase() throws Exception {
        File tf = new File(home, "p4.txt"), sf = new File(home, "p4.ini");
        tf.delete();
        sf.delete();
        Tree.testFile = tf;
        Structure.testFile = sf;
        Tree t = Tree.load();
        Tree.Node l = Tree.newChild(t.root, "לימוד");
        Tree.Node r = Tree.newChild(l, "רמחל");
        r.entries.add(new Tree.Entry("/storage/x/דרך ה'.pdf", 10, false));
        t.save();
        Structure.addDirectFunc("רמח\"ל", "וירטואלי", "", Target.of("וירטואלי", "רשימה", "לימוד/רמחל"));
        Structure.Func f = Structure.load().direct.get(0);
        // שינוי שם הרשימה \"רמחל\" מתוך הכלי של \"לימוד\" (כמו renameFolder)
        t = Tree.load();
        Tree.Node c = t.find(Arrays.asList("לימוד", "רמחל"));
        c.name = "רמח\"ל";
        t.save();
        FnVirtual.ensureOwn(f);                          // פתיחת הכלי
        t = Tree.load();
        Tree.Node old = t.find(Arrays.asList("לימוד", "רמחל")), renamed = t.find(Arrays.asList("לימוד", "רמח\"ל"));
        say("4. אחרי שינוי שם: הכלי פותח רשימה עם " + (old == null ? "—" : old.entries.size() + " פריטים")
                + " · הרשימה האמיתית (בשם החדש) עם " + (renamed == null ? "—" : renamed.entries.size() + " פריטים"));
        Tree.testFile = null;
        Structure.testFile = null;
    }

    // 5: מחיקת מבנה.txt עצמו מתוך התוכנה, ופתיחת כלי רשימות אחר כך
    static void deleteProgramFile() throws Exception {
        File drive = new File(home, "כונן5");
        wipe(drive);
        drive.mkdirs();
        String saved = System.getProperty("vfm.home");
        // Paths משתמש ב-Environment (תחליף): התיקייה של התוכנה בתוך vfm.home
        Tree.testFile = null;
        Structure.testFile = new File(home, "p5.ini");
        Structure.testFile.delete();
        File tree = Paths.tree();
        tree.getParentFile().mkdirs();
        Files.writeAtomic(tree, "#1 לימוד\n/storage/x/א.pdf | 10\n/storage/x/ב.pdf | 20\n");
        Index ix = new Index("בית", "בית", home.getAbsolutePath());
        ix.scan(null);
        Library lib = Library.forTests(new ArrayList<Index>(Collections.singletonList(ix)));
        Physical.Result res = Physical.delete(lib, Arrays.asList(tree.getAbsolutePath()), false);
        lib.flush(5000);
        say("5. מחיקת מבנה.txt מתוך התוכנה: " + (res.done.size() == 1 ? "הצליחה" : "נחסמה (" + res.note + ")")
                + " · רשימות שנשארו: " + Tree.load().root.children.size());
        Structure.addDirectFunc("לימוד", "וירטואלי", "", Target.of("וירטואלי", "רשימה", "לימוד"));
        FnVirtual.ensureOwn(Structure.load().direct.get(0));
        Tree t = Tree.load();
        Tree.Node n = t.find(Arrays.asList("לימוד"));
        say("   אחרי פתיחת כלי הרשימות: מבנה.txt קיים=" + tree.exists() + " · ב\"לימוד\" " + (n == null ? "—" : n.entries.size()) + " פריטים (היו 2)");
        Structure.testFile = null;
    }

    // 6: קריאה שנכשלה (הקובץ קיים ואי אפשר לקרוא אותו) — ואז פתיחת כלי רשימות
    static void readFailure() throws Exception {
        File tree = Paths.tree();
        Tree.testFile = null;
        Structure.testFile = new File(home, "p6.ini");
        Structure.testFile.delete();
        Files.writeAtomic(tree, "#1 לימוד\n/storage/x/א.pdf | 10\n/storage/x/ב.pdf | 20\n#1 מתכונים\n/storage/x/ג.pdf | 30\n");
        tree.setReadable(false, false);
        Structure.addDirectFunc("לימוד", "וירטואלי", "", Target.of("וירטואלי", "רשימה", "לימוד"));
        FnVirtual.ensureOwn(Structure.load().direct.get(0));          // פתיחת כלי הרשימות
        tree.setReadable(true, false);
        String now = Files.readUtf8(tree);
        File dir = new File(Paths.backups(), "מבנה");
        boolean inBackup = false;
        File[] all = dir.listFiles();
        if (all != null) for (File f : all) if (Files.readUtf8(f).contains("מתכונים")) inBackup = true;
        say("6. אחרי קריאה שנכשלה ופתיחת כלי: \"מתכונים\" בקובץ=" + now.contains("מתכונים")
                + " · \"א.pdf\" בקובץ=" + now.contains("א.pdf") + " · נשמר בגיבוי=" + inBackup + "\n   תוכן הקובץ עכשיו: " + now.replace("\n", " ⏎ "));
        Structure.testFile = null;
    }

    // 7: הגיבויים — לפי מספר
    static void backupRotation() throws Exception {
        File tree = Paths.tree();
        Tree.testFile = null;
        File dir = new File(Paths.backups(), "מבנה");
        wipe(dir);
        Files.writeAtomic(tree, "#1 הגרסה הטובה\n/storage/x/א.pdf | 10\n");
        for (int i = 0; i < 45; i++) {
            Tree t = Tree.load();
            t.root.children.get(0).entries.add(new Tree.Entry("/storage/x/" + i + ".pdf", 1, false));
            if (i == 0) t.root.children.get(0).name = "גרסה פגומה";
            t.save();
            try { Thread.sleep(3); } catch (InterruptedException e) { }
        }
        File[] all = dir.listFiles();
        boolean good = false;
        for (File f : all) if (Files.readUtf8(f).contains("הגרסה הטובה")) good = true;
        say("7. אחרי 45 שמירות: " + all.length + " גיבויים · הגרסה שלפני הטעות עוד שם=" + good);
    }

    // 8: תנאי שגוי
    static void badCondition() throws Exception {
        File root = new File(home, "כונן8");
        wipe(root);
        mk(root, "ספר קטן.pdf", 100);
        mk(root, "ספר גדול.pdf", 3 * 1024 * 1024);
        Index ix = new Index("8", "8", root.getAbsolutePath());
        ix.scan(null);
        Library lib = Library.forTests(new ArrayList<Index>(Collections.singletonList(ix)));
        Cond c = Cond.parse("ספר גודל:2MB");
        say("8. \"ספר גודל:2MB\" (בלי >): תוצאות=" + lib.search("ספר גודל:2MB", false, false).total
                + " · עם >: " + lib.search("ספר גודל:>2MB", false, false).total
                + " · לא הובן: " + c.unknown + " (לא מוצג בשום מקום)");
    }

    // 9: חיפוש \"גם במסלול\" — זמן
    static Library synthetic(int nDirs, int nFiles) {
        Index ix = new Index("ס", "ס", "/storage/9999-0000");
        Random rnd = new Random(1);
        String[] w = {"ספרים", "תורה", "מחשבה", "הלכה", "רמחל", "Download", "לימוד", "חומש", "גמרא", "קבלה", "מוסר", "שונות"};
        int[] depth = new int[nDirs];
        for (int i = 0; i < nDirs; i++) {
            Index.Dir d = new Index.Dir();
            int par = -1;
            if (i >= 12) { do { par = rnd.nextInt(i); } while (depth[par] >= 4); }
            d.parent = par;
            depth[i] = par < 0 ? 0 : depth[par] + 1;
            d.name = w[rnd.nextInt(w.length)] + " " + i;
            d.norm = Text.norm(d.name);
            ix.dirs.add(d);
        }
        for (int i = 0; i < nFiles; i++) {
            Index.Rec r = new Index.Rec();
            r.dir = rnd.nextInt(nDirs);
            r.name = "קובץ " + w[rnd.nextInt(w.length)] + " " + i + ".pdf";
            r.norm = Text.norm(r.name);
            r.loose = Text.loose(r.norm);
            r.size = 1000 + rnd.nextInt(1000000);
            ix.files.add(r);
        }
        return Library.forTests(new ArrayList<Index>(Collections.singletonList(ix)));
    }

    static void searchPaths() {
        Library lib = synthetic(3000, 25000);
        for (int i = 0; i < 5; i++) { lib.search("זזז", false, true); lib.search("זזז", true, true); }
        long t0 = System.nanoTime();
        for (int i = 0; i < 20; i++) lib.search("זזז", false, true);
        long t1 = System.nanoTime();
        for (int i = 0; i < 20; i++) lib.search("זזז", true, true);
        long t2 = System.nanoTime();
        say(String.format(Locale.US, "9. חיפוש ב-25,000 קבצים (כאן, במחשב): בשם %.1f מ\"ש · גם במסלול %.1f מ\"ש",
                (t1 - t0) / 20e6, (t2 - t1) / 20e6));
    }

    // 10: יומן המחיקות נכתב מחדש בכל מחיקה
    static void trashLog() throws Exception {
        for (int n : new int[]{500, 1000, 2000}) {
            File root = new File(home, "כונן10");
            wipe(root);
            List<String> paths = new ArrayList<String>();
            for (int i = 0; i < n; i++) paths.add(mk(new File(root, "ספרים"), "ספר מספר " + i + ".pdf", 10).getAbsolutePath());
            Index ix = new Index("10", "10", root.getAbsolutePath());
            ix.scan(null);
            Library lib = Library.forTests(new ArrayList<Index>(Collections.singletonList(ix)));
            File tf = new File(home, "p10.txt");
            tf.delete();
            Tree.testFile = tf;
            long t0 = System.currentTimeMillis();
            Physical.delete(lib, paths, false);
            long t1 = System.currentTimeMillis();
            lib.flush(60000);
            File log = new File(new File(root, Index.TRASH), Trash.LOG);
            say("10. מחיקת " + n + " קבצים: " + (t1 - t0) + " מ\"ש (כאן) · יומן " + log.length() / 1024 + "KB");
            Tree.testFile = null;
        }
    }

    // 11: כפולים בשם דומה — הרבה קבצים באותו גודל
    static void similarCost() {
        for (int n : new int[]{300, 1000}) {
            List<String> names = new ArrayList<String>();
            for (int i = 0; i < n; i++) names.add("תמונה ממצלמה " + i + " (" + (i * 7) + ").jpg");
            long t0 = System.nanoTime();
            List<List<String>> out = new ArrayList<List<String>>();
            for (String x : names) {                        // כמו split() במסך הכפולים
                List<String> home = null;
                for (Iterator<List<String>> it = out.iterator(); it.hasNext(); ) {
                    List<String> c = it.next();
                    boolean near = false;
                    for (String y : c) if (Text.similarNames(x, y)) { near = true; break; }
                    if (!near) continue;
                    if (home == null) home = c; else { home.addAll(c); it.remove(); }
                }
                if (home == null) out.add(home = new ArrayList<String>());
                home.add(x);
            }
            say(String.format(Locale.US, "11. %d קבצים באותו גודל, בשם דומה: %.0f מ\"ש (כאן) · קבוצות: %d", n,
                    (System.nanoTime() - t0) / 1e6, out.size()));
        }
    }

    // 12: שני כותבים ל\"מבנה.txt\": מסך בלי נעילה, ופעולה ברקע עם נעילה
    static void treeRace() throws Exception {
        File tf = new File(home, "p12.txt");
        tf.delete();
        Tree.testFile = tf;
        Tree t = Tree.load();
        Tree.Node l = Tree.newChild(t.root, "לימוד");
        l.entries.add(new Tree.Entry("/storage/x/נמחק.pdf", 10, false));
        t.save();
        final Tree screen = Tree.load();                              // המסך קורא (כמו moveFolder / transferSelected)
        Thread bg = new Thread(new Runnable() {                        // ברקע: מחיקה מהכונן מסירה מהרשימות
            public void run() { Tree.removeEverywhere(Arrays.asList("/storage/x/נמחק.pdf")); }
        });
        bg.start();
        bg.join();
        Tree.newChild(screen.root, "רשימה חדשה");                     // המסך משנה ושומר
        screen.save();
        boolean back = Tree.listsOf("/storage/x/נמחק.pdf").size() > 0;
        say("12. אחרי שני הכותבים: הפריט שהוסר ברקע חזר לרשימה=" + back);
        Tree.testFile = null;
    }

    // 13: כפולים לפי גודל — קבצים ריקים
    static void zeroSize() throws Exception {
        File root = new File(home, "כונן13");
        wipe(root);
        for (int i = 0; i < 5; i++) mk(new File(root, "תיקייה" + i), "ריק " + i + ".txt", 0);
        Index ix = new Index("13", "13", root.getAbsolutePath());
        ix.scan(null);
        Map<Long, Integer> bySize = new HashMap<Long, Integer>();
        for (Index.Rec r : ix.files) bySize.put(r.size, bySize.containsKey(r.size) ? bySize.get(r.size) + 1 : 1);
        say("13. כפולים לפי גודל: 5 קבצים ריקים בשמות שונים נכנסים לקבוצה אחת (" + bySize.get(0L) + " עותקים של 0B)");
    }
}
