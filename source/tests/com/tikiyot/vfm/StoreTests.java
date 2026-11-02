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
            t.migration(dir);
            ImportTests.pure(t.o, dir);
            StoreContract.Result br = BackupContract.run(new File(dir, "backup"));
            t.o.passed += br.passed;
            for (String f : br.failed) t.o.failed.add("גיבוי — " + f);
            t.ck("גיבוי: החוזה רץ (לפחות 60 בדיקות)", br.passed + br.failed.size() >= 60);
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
        ck(name + ": החוזה רץ (לפחות 80 בדיקות)", r.passed + r.failed.size() >= 80);
        try {
            Store s = opener.fresh(new File(sub, "import-counter.txt"));
            try {
                new ImportTests(o, name).run(s, new File(sub, "import"));
            } finally {
                s.close();
            }
            Store sm = opener.fresh(new File(sub, "smart-counter.txt"));
            try {
                new ImportTests(o, name).smart(sm, new File(sub, "smart"));
            } finally {
                sm.close();
            }
            Store rs = opener.fresh(new File(sub, "refs-counter.txt"));
            try {
                new ImportTests(o, name).refs(rs, new File(sub, "refs"));
            } finally {
                rs.close();
            }
            new ImportTests(o, name).fullCheck(opener, new File(sub, "full"));
        } catch (Throwable x) {
            o.failed.add(name + " — העתקה: חריגה " + x);
        }
    }

    /** מסד מגרסה 1 (כמו שנוצר ב־מ0.1) נפתח, עובר לגרסה 2 ולא מאבד דבר. */
    private void migration(File dir) {
        File f = new File(dir, "v1.db");
        File cf = new File(dir, "v1-counter.txt");
        android.database.sqlite.SQLiteDatabase.deleteDatabase(f);
        try {
            android.database.sqlite.SQLiteDatabase db = android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(f, null);
            db.enableWriteAheadLogging();
            db.setForeignKeyConstraintsEnabled(true);
            db.execSQL("CREATE TABLE meta(k TEXT PRIMARY KEY, v TEXT NOT NULL)");
            db.execSQL("CREATE TABLE items(id INTEGER PRIMARY KEY, type INTEGER NOT NULL, name TEXT NOT NULL, extra TEXT, "
                    + "created INTEGER NOT NULL, changed INTEGER NOT NULL, origin_sys TEXT, origin_id INTEGER)");
            db.execSQL("CREATE TABLE members(list_id INTEGER NOT NULL REFERENCES items(id) ON DELETE CASCADE, "
                    + "item_id INTEGER NOT NULL REFERENCES items(id) ON DELETE CASCADE, "
                    + "position INTEGER NOT NULL, added INTEGER NOT NULL, PRIMARY KEY(list_id, item_id)) WITHOUT ROWID");
            db.execSQL("CREATE INDEX members_pos ON members(list_id, position)");
            db.execSQL("CREATE INDEX members_item ON members(item_id)");
            db.execSQL("INSERT INTO meta(k, v) VALUES('next_id', '3')");
            db.execSQL("INSERT INTO meta(k, v) VALUES('system_code', 'abcd1234')");
            db.execSQL("INSERT INTO items(id,type,name,created,changed) VALUES(1,1,'old list',5,5)");
            db.execSQL("INSERT INTO items(id,type,name,created,changed) VALUES(2,2,'old note',6,6)");
            db.execSQL("INSERT INTO members(list_id,item_id,position,added) VALUES(1,2,0,7)");
            db.setVersion(1);
            db.close();
            SqlStore s = SqlStore.open(f, new IdCounter(cf));
            try {
                ck("מעבר 1→2: הגרסה עלתה", s.getVersionForTest() == 2);
                ck("מעבר 1→2: הנתונים הישנים שלמים", s.item(1) != null && s.item(1).name.equals("old list") && s.members(1).size() == 1
                        && s.members(1).get(0).item.name.equals("old note") && s.systemCode().equals("abcd1234") && s.lastId() == 2);
                long fi = s.createFile(new Store.FileInfo("/a/b.txt", "x", 1, 2, "2:aa"));
                s.setOrigin(1, "k");
                s.setProp(1, Store.P_ICON, "i");
                ck("מעבר 1→2: הטבלאות החדשות עובדות", fi == 3 && s.file(fi).sig.equals("2:aa") && s.findByOrigin("k") == 1 && "i".equals(s.prop(1, Store.P_ICON)));
                ck("מעבר 1→2: המסד תקין", s.check() == null);
            } finally {
                s.close();
            }
            SqlStore again = SqlStore.open(f, new IdCounter(cf));
            try {
                ck("מעבר 1→2: פתיחה שנייה לא מעבירה שוב", again.getVersionForTest() == 2 && again.findFile("/a/b.txt") == 3);
            } finally {
                again.close();
            }
        } catch (Throwable x) {
            o.failed.add("מעבר 1→2: חריגה " + x);
        } finally {
            android.database.sqlite.SQLiteDatabase.deleteDatabase(f);
            cf.delete();
        }
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
