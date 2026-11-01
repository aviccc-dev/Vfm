package com.tikiyot.vfm;

// בדיקות מ0.1 על המחשב: מונה המזהים, שתי הבניות, ושכבת האחסון — MemStore, ו־SqlStore מול SQLite אמיתי (גשר python).

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

final class StoreTests {

    static final class Out {
        int passed;
        final List<String> failed = new ArrayList<String>();
    }

    private final Out o = new Out();

    private void ck(String what, boolean ok) {
        if (ok) o.passed++;
        else o.failed.add(what);
    }

    static Out run(File home, File src) {
        StoreTests t = new StoreTests();
        File dir = new File(home, "מ01-" + System.nanoTime());
        dir.mkdirs();
        try {
            t.counter(dir);
            t.flavor(src);
            t.contract("MemStore", new StoreContract.Opener() {
                @Override
                public Store fresh(File counter) throws IOException {
                    return new MemStore(new IdCounter(counter));
                }
            }, dir);
            final int[] n = {0};
            final File dbDir = dir;
            t.contract("SqlStore מול SQLite של המחשב", new StoreContract.Opener() {
                @Override
                public Store fresh(File counter) throws IOException {
                    return SqlStore.open(new File(dbDir, "c" + (n[0]++) + ".db"), new IdCounter(counter));
                }
            }, dir);
            String p = SqlStore.persistenceCheck(dir);
            t.ck("שמירה בין פתיחות, WAL, גרסה עתידית לא נפתחת" + (p == null ? "" : ": " + p), p == null);
        } catch (Throwable x) {
            t.o.failed.add("חריגה: " + x);
        }
        return t.o;
    }

    private void contract(String name, StoreContract.Opener opener, File dir) {
        File sub = new File(dir, name.replace(' ', '_') + "-contract");
        sub.mkdirs();
        StoreContract.Result r = StoreContract.run(opener, sub);
        o.passed += r.passed;
        for (String f : r.failed) o.failed.add(name + " — " + f);
        ck(name + ": החוזה רץ (לפחות 40 בדיקות)", r.passed + r.failed.size() >= 40);
    }

    private static String read(File f) throws IOException {
        return Files.readUtf8(f).trim();
    }

    private void counter(File dir) throws IOException {
        File f = new File(dir, "ids/counter.txt");
        IdCounter c = new IdCounter(f);
        ck("מונה: בלי קובץ — תקרה 0, המספר הראשון 1", c.ceiling() == 0 && c.first(1) == 1);
        c.reserve(1);
        ck("מונה: המספר הראשון שומר בלוק של 100", c.ceiling() == 100 && read(f).equals("100"));
        long before = f.lastModified();
        for (int i = 2; i <= 100; i++) c.reserve(i);
        ck("מונה: עד 100 — אין כתיבה נוספת", read(f).equals("100") && f.lastModified() == before);
        c.reserve(101);
        ck("מונה: 101 — בלוק חדש, 200", c.ceiling() == 200 && read(f).equals("200"));
        IdCounter again = new IdCounter(f);
        ck("מונה: נקרא בחזרה מהקובץ", again.ceiling() == 200 && again.first(5) == 201);
        ck("מונה: מסד קדימה מהתקרה — נשאר", again.first(1000) == 1000);
        ck("מונה: אין קובץ זמני שנשאר", !new File(f.getPath() + ".tmp").exists());
        for (String bad : new String[]{"abc", "", "-5", "12x"}) {
            File b = new File(dir, "ids/bad.txt");
            FileOutputStream out = new FileOutputStream(b);
            out.write(bad.getBytes("UTF-8"));
            out.close();
            boolean threw = false;
            try {
                new IdCounter(b);
            } catch (IOException e) {
                threw = true;
            }
            ck("מונה: קובץ פגום (\"" + bad + "\") — עוצר ולא ממשיך", threw);
        }
    }

    private void flavor(File src) throws IOException {
        ck("בניות: שמות שונים", !Flavor.pkg(true).equals(Flavor.pkg(false)) && !Flavor.authority(true).equals(Flavor.authority(false))
                && !Flavor.appDir(true).equals(Flavor.appDir(false)));
        ck("בניות: בניית מ — חבילה, רשות ותיקייה", Flavor.pkg(true).equals("com.tikiyot.vfm.m") && Flavor.authority(true).equals("com.tikiyot.vfm.m.files")
                && Flavor.appDir(true).equals(Flavor.oldAppDir() + " מ"));
        ck("בניות: הישנה לא השתנתה — שם החבילה, הרשות והתיקייה", !Flavor.M && Paths.APP_DIR.equals("מנהל קבצים וירטואלי")
                && Provider.AUTHORITY.equals("com.tikiyot.vfm.files") && Flavor.oldAppDir().equals(Paths.APP_DIR));
        if (src == null) return;
        String mf = read(new File(src, "AndroidManifest.xml"));
        ck("בניות: המניפסט מתאים ל־Flavor (חבילה ורשות)", mf.contains("package=\"" + Flavor.PKG_OLD + "\"") && mf.contains("android:authorities=\"" + Flavor.authority(false) + "\""));
        ck("בניות: מסך המסד רשום במניפסט", mf.contains("android:name=\".DbScreen\""));
    }
}
