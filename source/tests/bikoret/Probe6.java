package com.tikiyot.vfm;

// בדיקות גישוש לביקורת השביעית (ביקורת על הביקורת) — לא חלק מהפרויקט.

import java.io.File;
import java.util.*;

public final class Probe6 {

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
        notInIndex();
        hiddenStar();
        formUnit();
        rootAndTrash();
        System.exit(0);
    }

    static Library lib(File root) {
        Index ix = new Index("ב", "ב", root.getAbsolutePath());
        ix.scan(null);
        return Library.forTests(new ArrayList<Index>(Collections.singletonList(ix)));
    }

    // 1: קבצים שנוספו אחרי הסריקה (כמו הורדה חדשה) — DEL ו-MOVE מקובץ פקודות
    static void notInIndex() throws Exception {
        File root = new File(home, "כונן-ג1");
        wipe(root);
        File dl = new File(root, "Download");
        mk(dl, "ישן.pdf", 10);
        new File(root, "ספרים").mkdirs();
        Library lib = lib(root);                                    // האינדקס נסרק עכשיו
        File fresh = mk(dl, "הורדה חדשה.pdf", 20);                  // ואחריו — הורדה חדשה
        File fresh2 = mk(dl, "עוד הורדה.epub", 30);
        Map<String, String> vars = Batch.vars(root, root.getAbsolutePath(), null);
        Batch.Plan p = Batch.plan("DEL %הורדות%\\*.pdf\n", root, vars);
        List<String> rep = Batch.run(p, lib, null, null);
        lib.flush(5000);
        say("1. DEL על קובץ שהורד אחרי הסריקה: בתצוגה המקדימה " + p.steps.get(0).files.size() + " קבצים · נמחק בפועל=" + !fresh.exists() + " · דוח: " + rep);
        Batch.Plan m = Batch.plan("MOVE %הורדות%\\*.epub %פנימי%\\ספרים\n", root, vars);
        List<String> rep2 = Batch.run(m, lib, null, null);
        lib.flush(5000);
        say("   MOVE של קובץ שהורד אחרי הסריקה: הועבר=" + new File(root, "ספרים/עוד הורדה.epub").exists() + " · דוח: " + rep2);
    }

    // 2: DEL *.* — קובץ מוסתר (ב-Windows: DEL *.* מדלג על מוסתרים)
    static void hiddenStar() throws Exception {
        File root = new File(home, "כונן-ג2");
        wipe(root);
        File d = new File(root, "תמונות");
        mk(d, ".nomedia", 0);
        mk(d, "תמונה.jpg", 10);
        Map<String, String> vars = Batch.vars(root, root.getAbsolutePath(), null);
        Batch.Plan p = Batch.plan("DEL %פנימי%\\תמונות\\*.*\n", root, vars);
        List<String> names = new ArrayList<String>();
        for (String f : p.steps.get(0).files) names.add(new File(f).getName());
        say("2. DEL *.* — נבחרו: " + names + (names.contains(".nomedia") ? "   ← כולל קובץ מוסתר" : ""));
    }

    // 3: הטופס — גודל בבתים (/גודל:>500) כשהטופס נפתח ונשמר בלי לגעת
    static void formUnit() {
        BatchLine b = BatchLine.parse("DEL %הורדות%\\*.* /גודל:>500");
        int ui = Arrays.asList(BatchLine.SIZE_UNITS).indexOf(b.sizeUnit);
        String before = b.toLine();
        b.sizeUnit = BatchLine.SIZE_UNITS[ui < 0 ? 1 : ui];          // מה שהטופס עושה ב-update()
        say("3. טופס: \"" + before + "\" ← אחרי פתיחה ושמירה בלי שינוי: \"" + b.toLine() + "\"");
        BatchLine c = BatchLine.parse("COPY a b /תאריך:>=2026-09-01");
        say("   תאריך: \"COPY a b /תאריך:>=2026-09-01\" ← \"" + c.toLine() + "\"");
    }

    // 4: קובץ פקודות מול שורש הכונן וסל המחזור (ההגנה בליבה)
    static void rootAndTrash() throws Exception {
        File root = new File(home, "כונן-ג4");
        wipe(root);
        mk(new File(root, "ספרים"), "א.pdf", 10);
        mk(new File(root, Index.TRASH), "ב.pdf", 10);
        new File(root, "יעד").mkdirs();
        Library lib = lib(root);
        Map<String, String> vars = Batch.vars(root, root.getAbsolutePath(), null);
        List<String> r1 = Batch.run(Batch.plan("RD \"%פנימי%\\" + Index.TRASH + "\" /S\n", root, vars), lib, null, null);
        List<String> r2 = Batch.run(Batch.plan("MOVE \"%פנימי%\\" + Index.TRASH + "\" %פנימי%\\יעד\n", root, vars), lib, null, null);
        lib.flush(5000);
        say("4. RD /S על סל המחזור: " + r1 + " · הסל קיים=" + new File(root, Index.TRASH + "/ב.pdf").exists());
        say("   MOVE של סל המחזור: " + r2 + " · הסל במקומו=" + new File(root, Index.TRASH).isDirectory());
    }
}
