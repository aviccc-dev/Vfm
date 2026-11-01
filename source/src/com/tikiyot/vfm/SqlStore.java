package com.tikiyot.vfm;

// Store על SQLite. אחראי: טבלאות, עסקאות, יומן WAL, בדיקת תקינות בפתיחה. אסור לו: מסכים.
// גרסת הטבלאות — PRAGMA user_version (עכשיו 2; 1 → 2 במעבר אוטומטי בפתיחה, בעסקה אחת). מסד של גרסה חדשה יותר — לא נפתח (לא נוגעים במה שלא מכירים).
//
// הטבלאות:
//   items   — מספר זיהוי (מוחלט, לא חוזר), סוג, שם, שדה לתוכן לפי סוג (עוד לא בשימוש), נוצר/שונה, ושדות מקור
//             לנתונים שהגיעו ממערכת אחרת: קוד המערכת + המספר המקורי (עוד לא בשימוש), ומ0.2 גם origin_key —
//             מפתח טקסטואלי (למנוע הישן אין מספרי זיהוי), מאונדקס.
//   props   — (מ0.2) מאפיינים של פריט: מפתח וערך. לרשימה: צלמית, תיאור, עיצוב, "למה".
//   files   — (מ0.2) פרטי פריט קובץ: מסלול (ייחודי), כונן, גודל ותאריך כפי שהיו בחישוב החתימה, והחתימה.
//   members — רשימה, פריט, מקום (צפוף מ־0), מתי נוסף. פריט פעם אחת בכל רשימה.
//   meta    — המונה הבא, קוד המערכת.

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class SqlStore implements Store {

    public static final int VERSION = 2;

    private final SQLiteDatabase db;
    private final IdCounter counter;
    private final String code;

    private SqlStore(SQLiteDatabase db, IdCounter counter) {
        this.db = db;
        this.counter = counter;
        this.code = openMeta();
    }

    /** פותח (או יוצר) את המסד. @throws Failure אם הוא פגום, או מגרסה חדשה יותר. */
    public static SqlStore open(File file, IdCounter counter) {
        File dir = file.getParentFile();
        if (dir != null) dir.mkdirs();
        SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(file, null);
        try {
            db.enableWriteAheadLogging();
            db.setForeignKeyConstraintsEnabled(true);
            int v = db.getVersion();
            if (v > VERSION) throw new Failure("database is newer: " + v);
            if (v == 0) create(db);
            else if (v == 1) upgradeTo2(db);
            return new SqlStore(db, counter);
        } catch (RuntimeException e) {
            db.close();
            throw e;
        }
    }

    private static void create(SQLiteDatabase db) {
        db.beginTransaction();
        try {
            db.execSQL("CREATE TABLE meta(k TEXT PRIMARY KEY, v TEXT NOT NULL)");
            db.execSQL("CREATE TABLE items(id INTEGER PRIMARY KEY, type INTEGER NOT NULL, name TEXT NOT NULL, extra TEXT, "
                    + "created INTEGER NOT NULL, changed INTEGER NOT NULL, origin_sys TEXT, origin_id INTEGER, origin_key TEXT)");
            db.execSQL("CREATE TABLE members(list_id INTEGER NOT NULL REFERENCES items(id) ON DELETE CASCADE, "
                    + "item_id INTEGER NOT NULL REFERENCES items(id) ON DELETE CASCADE, "
                    + "position INTEGER NOT NULL, added INTEGER NOT NULL, PRIMARY KEY(list_id, item_id)) WITHOUT ROWID");
            db.execSQL("CREATE INDEX members_pos ON members(list_id, position)");
            db.execSQL("CREATE INDEX members_item ON members(item_id)");
            addV2Tables(db);
            db.execSQL("INSERT INTO meta(k, v) VALUES('next_id', '1')");
            db.execSQL("INSERT INTO meta(k, v) VALUES('system_code', ?)", new Object[]{Stores.newSystemCode()});
            db.setVersion(VERSION);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /** הטבלאות והאינדקסים שנוספו בגרסה 2 (גם ביצירה חדשה). */
    private static void addV2Tables(SQLiteDatabase db) {
        db.execSQL("CREATE INDEX items_origin ON items(origin_key)");
        db.execSQL("CREATE TABLE props(item_id INTEGER NOT NULL REFERENCES items(id) ON DELETE CASCADE, "
                + "k TEXT NOT NULL, v TEXT NOT NULL, PRIMARY KEY(item_id, k)) WITHOUT ROWID");
        db.execSQL("CREATE TABLE files(item_id INTEGER PRIMARY KEY REFERENCES items(id) ON DELETE CASCADE, "
                + "path TEXT NOT NULL, drive TEXT NOT NULL, size INTEGER NOT NULL, mtime INTEGER NOT NULL, sig TEXT)");
        db.execSQL("CREATE UNIQUE INDEX files_path ON files(path)");
        db.execSQL("CREATE INDEX files_sig ON files(sig)");
    }

    /** 1 → 2: עמודת מקור + props + files. הנתונים הקיימים לא משתנים. */
    private static void upgradeTo2(SQLiteDatabase db) {
        db.beginTransaction();
        try {
            db.execSQL("ALTER TABLE items ADD COLUMN origin_key TEXT");
            addV2Tables(db);
            db.setVersion(2);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /** המונה של המסד לא יורד מתחת לתקרה שבקובץ (שחזור מגיבוי ישן). מחזיר את קוד המערכת. */
    private String openMeta() {
        db.beginTransaction();
        try {
            long cur = Long.parseLong(text("SELECT v FROM meta WHERE k='next_id'"));
            long first = counter.first(cur);
            if (first != cur) db.execSQL("UPDATE meta SET v=? WHERE k='next_id'", new Object[]{String.valueOf(first)});
            String c = text("SELECT v FROM meta WHERE k='system_code'");
            db.setTransactionSuccessful();
            return c;
        } finally {
            db.endTransaction();
        }
    }

    // ---------- פנימי ----------

    private String text(String sql, String... args) {
        Cursor c = db.rawQuery(sql, args);
        try {
            return c.moveToFirst() ? c.getString(0) : null;
        } finally {
            c.close();
        }
    }

    private long num(String sql, String... args) {
        Cursor c = db.rawQuery(sql, args);
        try {
            return c.moveToFirst() && !c.isNull(0) ? c.getLong(0) : 0;
        } finally {
            c.close();
        }
    }

    private static String s(long v) {
        return String.valueOf(v);
    }

    private long issue() {
        long id = Long.parseLong(text("SELECT v FROM meta WHERE k='next_id'"));
        try {
            counter.reserve(id);
        } catch (IOException e) {
            throw new Failure("counter file", e);
        }
        db.execSQL("UPDATE meta SET v=? WHERE k='next_id'", new Object[]{s(id + 1)});
        return id;
    }

    private Item read(long id) {
        Cursor c = db.rawQuery("SELECT id, type, name, created, changed FROM items WHERE id=?", new String[]{s(id)});
        try {
            return c.moveToFirst() ? row(c) : null;
        } finally {
            c.close();
        }
    }

    private static Item row(Cursor c) {
        return new Item(c.getLong(0), c.getInt(1), c.getString(2), c.getLong(3), c.getLong(4));
    }

    private Item mustBeList(long id) {
        Item it = read(id);
        Stores.mustBeList(it, id);
        return it;
    }

    private long create(int type, String name) {
        long now = System.currentTimeMillis();
        long id = issue();
        db.execSQL("INSERT INTO items(id, type, name, created, changed) VALUES(?,?,?,?,?)",
                new Object[]{id, type, name, now, now});
        return id;
    }

    private void append(long list, long item, long now) {
        long pos = num("SELECT COUNT(*) FROM members WHERE list_id=?", s(list));
        db.execSQL("INSERT INTO members(list_id, item_id, position, added) VALUES(?,?,?,?)",
                new Object[]{list, item, pos, now});
    }

    /** סוגר את הפער שנשאר אחרי הסרת הפריט שהיה במקום pos. */
    private void closeGap(long list, long pos) {
        db.execSQL("UPDATE members SET position = position - 1 WHERE list_id=? AND position>?", new Object[]{list, pos});
    }

    // ---------- Store ----------

    @Override
    public long createList(String name, long parent) {
        String n = Stores.clean(name, "name");
        db.beginTransaction();
        try {
            if (parent != 0) mustBeList(parent);
            long id = create(LIST, n);
            if (parent != 0) append(parent, id, System.currentTimeMillis());
            db.setTransactionSuccessful();
            return id;
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public long addNote(long list, String text) {
        String t = Stores.clean(text, "text");
        db.beginTransaction();
        try {
            mustBeList(list);
            long id = create(NOTE, t);
            append(list, id, System.currentTimeMillis());
            db.setTransactionSuccessful();
            return id;
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public boolean addMember(long list, long item) {
        db.beginTransaction();
        try {
            mustBeList(list);
            Item it = read(item);
            if (it == null) throw new Failure("no such item " + item);
            Stores.noListInList(it);
            if (num("SELECT COUNT(*) FROM members WHERE list_id=? AND item_id=?", s(list), s(item)) > 0) {
                db.setTransactionSuccessful();   // "כבר שם" אינו כישלון: בלי הסימון, עסקה חיצונית (atomic) הייתה מתבטלת כולה
                return false;
            }
            append(list, item, System.currentTimeMillis());
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public boolean removeMember(long list, long item) {
        db.beginTransaction();
        try {
            mustBeList(list);
            Cursor c = db.rawQuery("SELECT position FROM members WHERE list_id=? AND item_id=?", new String[]{s(list), s(item)});
            long pos;
            try {
                if (!c.moveToFirst()) {
                    db.setTransactionSuccessful();   // "לא היה שם" אינו כישלון (ראו addMember)
                    return false;
                }
                pos = c.getLong(0);
            } finally {
                c.close();
            }
            db.execSQL("DELETE FROM members WHERE list_id=? AND item_id=?", new Object[]{list, item});
            closeGap(list, pos);
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void move(long list, long item, int position) {
        db.beginTransaction();
        try {
            mustBeList(list);
            Cursor c = db.rawQuery("SELECT position FROM members WHERE list_id=? AND item_id=?", new String[]{s(list), s(item)});
            long from;
            try {
                if (!c.moveToFirst()) throw new Failure("not a member");
                from = c.getLong(0);
            } finally {
                c.close();
            }
            long to = Math.max(0, Math.min(position, num("SELECT COUNT(*) FROM members WHERE list_id=?", s(list)) - 1));
            if (to != from) {
                if (to > from)
                    db.execSQL("UPDATE members SET position = position - 1 WHERE list_id=? AND position>? AND position<=?",
                            new Object[]{list, from, to});
                else
                    db.execSQL("UPDATE members SET position = position + 1 WHERE list_id=? AND position>=? AND position<?",
                            new Object[]{list, to, from});
                db.execSQL("UPDATE members SET position=? WHERE list_id=? AND item_id=?", new Object[]{to, list, item});
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void rename(long item, String name) {
        String n = Stores.clean(name, "name");
        if (read(item) == null) throw new Failure("no such item " + item);
        db.execSQL("UPDATE items SET name=?, changed=? WHERE id=?", new Object[]{n, System.currentTimeMillis(), item});
    }

    @Override
    public void deleteList(long list) {
        db.beginTransaction();
        try {
            mustBeList(list);
            Cursor c = db.rawQuery("SELECT list_id, position FROM members WHERE item_id=?", new String[]{s(list)});
            List<long[]> parents = new ArrayList<long[]>();
            try {
                while (c.moveToNext()) parents.add(new long[]{c.getLong(0), c.getLong(1)});
            } finally {
                c.close();
            }
            db.execSQL("DELETE FROM items WHERE id=?", new Object[]{list});     // החברויות — במחיקה מדורגת
            for (long[] p : parents) closeGap(p[0], p[1]);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public long createFile(FileInfo f) {
        Stores.checkFile(f);
        db.beginTransaction();
        try {
            if (findFile(f.path) != 0) throw new Failure("file already exists: " + f.path);
            long id = create(FILE, Stores.fileName(f.path));
            db.execSQL("INSERT INTO files(item_id, path, drive, size, mtime, sig) VALUES(?,?,?,?,?,?)",
                    new Object[]{id, f.path, f.drive, f.size, f.mtime, f.sig});
            db.setTransactionSuccessful();
            return id;
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public FileInfo file(long item) {
        Cursor c = db.rawQuery("SELECT path, drive, size, mtime, sig FROM files WHERE item_id=?", new String[]{s(item)});
        try {
            return c.moveToFirst() ? new FileInfo(c.getString(0), c.getString(1), c.getLong(2), c.getLong(3), c.getString(4)) : null;
        } finally {
            c.close();
        }
    }

    @Override
    public void updateFile(long item, long size, long mtime, String sig) {
        if (file(item) == null) throw new Failure("not a file: " + item);
        db.beginTransaction();
        try {
            db.execSQL("UPDATE files SET size=?, mtime=?, sig=? WHERE item_id=?", new Object[]{size, mtime, sig, item});
            db.execSQL("UPDATE items SET changed=? WHERE id=?", new Object[]{System.currentTimeMillis(), item});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public long findFile(String path) {
        return num("SELECT item_id FROM files WHERE path=?", path);
    }

    @Override
    public long findByOrigin(String key) {
        return num("SELECT id FROM items WHERE origin_key=? ORDER BY id LIMIT 1", key);
    }

    @Override
    public void setOrigin(long item, String key) {
        String k = Stores.clean(key, "origin");
        if (read(item) == null) throw new Failure("no such item " + item);
        db.execSQL("UPDATE items SET origin_key=? WHERE id=?", new Object[]{k, item});
    }

    @Override
    public String prop(long item, String key) {
        return text("SELECT v FROM props WHERE item_id=? AND k=?", s(item), key);
    }

    @Override
    public void setProp(long item, String key, String value) {
        Stores.clean(key, "key");
        if (read(item) == null) throw new Failure("no such item " + item);
        db.beginTransaction();
        try {
            if (value == null || value.isEmpty()) {
                db.execSQL("DELETE FROM props WHERE item_id=? AND k=?", new Object[]{item, key});
            } else {
                db.execSQL("INSERT OR REPLACE INTO props(item_id, k, v) VALUES(?,?,?)", new Object[]{item, key, value});
                db.execSQL("UPDATE items SET changed=? WHERE id=?", new Object[]{System.currentTimeMillis(), item});
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void atomic(Work w, boolean keep) {
        db.beginTransaction();
        try {
            w.run(this);
            if (keep) db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public Item item(long id) {
        return read(id);
    }

    @Override
    public List<Item> lists() {
        List<Item> out = new ArrayList<Item>();
        Cursor c = db.rawQuery("SELECT id, type, name, created, changed FROM items WHERE type=? ORDER BY id", new String[]{s(LIST)});
        try {
            while (c.moveToNext()) out.add(row(c));
        } finally {
            c.close();
        }
        return out;
    }

    @Override
    public List<Member> members(long list) {
        mustBeList(list);
        List<Member> out = new ArrayList<Member>();
        Cursor c = db.rawQuery("SELECT i.id, i.type, i.name, i.created, i.changed, m.position, m.added "
                + "FROM members m JOIN items i ON i.id = m.item_id WHERE m.list_id=? ORDER BY m.position", new String[]{s(list)});
        try {
            while (c.moveToNext()) out.add(new Member(row(c), c.getInt(5), c.getLong(6)));
        } finally {
            c.close();
        }
        return out;
    }

    @Override
    public int count(long list) {
        mustBeList(list);
        return (int) num("SELECT COUNT(*) FROM members WHERE list_id=?", s(list));
    }

    @Override
    public List<Long> listsOf(long item) {
        List<Long> out = new ArrayList<Long>();
        Cursor c = db.rawQuery("SELECT list_id FROM members WHERE item_id=? ORDER BY list_id", new String[]{s(item)});
        try {
            while (c.moveToNext()) out.add(c.getLong(0));
        } finally {
            c.close();
        }
        return out;
    }

    @Override
    public long lastId() {
        return Long.parseLong(text("SELECT v FROM meta WHERE k='next_id'")) - 1;
    }

    @Override
    public String systemCode() {
        return code;
    }

    @Override
    public String check() {
        String r = text("PRAGMA quick_check");
        if (!"ok".equals(r)) return String.valueOf(r);
        String fk = text("PRAGMA foreign_key_check");
        return fk == null ? null : "foreign key: " + fk;
    }

    /** מידע לבדיקה עצמית: גרסת SQLite, שיטת היומן וסנכרון. */
    public String info() {
        return text("SELECT sqlite_version()") + " · " + text("PRAGMA journal_mode") + " · sync=" + text("PRAGMA synchronous");
    }

    /**
     * בדיקת שמירה: כותב, סוגר, פותח מחדש וקורא; ומסד מגרסה עתידית לא נפתח. null = תקין, אחרת מה נכשל.
     * רצה במחשב (מול SQLite של המחשב) ובמכשיר ("בדיקה עצמית").
     */
    public static String persistenceCheck(File dir) {
        File db = new File(dir, "persist.db"), cf = new File(dir, "persist-counter.txt");
        SQLiteDatabase.deleteDatabase(db);
        cf.delete();
        try {
            SqlStore a = open(db, new IdCounter(cf));
            long l = a.createList("keep", 0);
            long n = a.addNote(l, "note");
            String code = a.systemCode();
            if (!a.info().toLowerCase().contains("wal")) return "journal mode is not WAL: " + a.info();
            a.close();
            SqlStore b = open(db, new IdCounter(cf));
            try {
                if (b.item(l) == null || b.members(l).size() != 1 || b.members(l).get(0).item.id != n) return "data lost after reopen";
                if (!b.systemCode().equals(code)) return "system code changed";
                if (b.createList("next", 0) <= n) return "id repeated after reopen";
                if (b.getVersionForTest() != VERSION) return "schema version " + b.getVersionForTest();
                b.db.setVersion(VERSION + 1);
            } finally {
                b.close();
            }
            try {
                open(db, new IdCounter(cf)).close();
                return "a newer database was opened";
            } catch (Failure expected) {
                return null;
            }
        } catch (Throwable t) {
            return String.valueOf(t);
        } finally {
            SQLiteDatabase.deleteDatabase(db);
            cf.delete();
        }
    }

    int getVersionForTest() {
        return db.getVersion();
    }

    @Override
    public void close() {
        db.close();
    }
}
