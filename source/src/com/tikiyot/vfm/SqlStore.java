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
//
// מ0.5 — שמירה על הקובץ:
//   • מסד פגום לא נמחק. ברירת המחדל של אנדרואיד (DefaultDatabaseErrorHandler) מוחקת את קובץ המסד כשהיא מזהה פגם,
//     ופותחת במקומו מסד ריק — בלי לשאול. כאן המטפל לא עושה כלום: הקובץ נשאר, והפתיחה נכשלת ב־Damaged.
//   • בדיקת תקינות קצרה (quick_check) בכל פתיחה, לפני שכותבים דבר. מסד פגום — Damaged, ולא נוגעים בו.
//   • עותק עקבי של המסד (copyTo) דרך SQLite: VACUUM INTO, ובאנדרואיד ישן (SQLite לפני 3.27) — העתקת הקובץ ויומן ה־WAL
//     בזמן שהכתיבה נעולה, ואיחוד שלהם בעותק.
//   • פתיחה לקריאה בלבד (openReadOnly) — לבדיקת עותקים בלי לשנות בהם בית אחד.

import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseCorruptException;
import android.database.sqlite.SQLiteException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public final class SqlStore implements Store {

    public static final int VERSION = 2;

    /** המסד פגום (או שהקובץ אינו מסד). הקובץ לא נמחק ולא נכתב; ההודעה — לאנשי הפיתוח. */
    public static final class Damaged extends RuntimeException {
        public Damaged(String m) {
            super(m);
        }
    }

    /** מטפל פגמים שלא מוחק כלום (ראו בראש הקובץ). */
    private static final DatabaseErrorHandler KEEP = new DatabaseErrorHandler() {
        @Override
        public void onCorruption(SQLiteDatabase d) {
            // מ0.44: עם הקובץ — כדי שיהיה ברור כשזה מסד זמני של "ודא מסד" (שם הורסים קובץ בכוונה), ולא מסד של אופן
            String f = "?";
            try { f = d.getPath(); } catch (Throwable ignored) { }
            Log.i("SqlStore: corruption reported, the file is kept: " + f + (f.contains("/mselftest/") ? " (ודא מסד — מסד זמני שנהרס בכוונה)" : ""));   // בכונן
        }
    };

    /** לבדיקות במחשב: לבדוק גם את דרך ההעתקה של אנדרואיד ישן. */
    static boolean forceFileCopy;

    private final SQLiteDatabase db;
    private final IdCounter counter;
    private final String code;
    private final boolean readOnly;

    private SqlStore(SQLiteDatabase db, IdCounter counter) {
        this.db = db;
        this.counter = counter;
        this.readOnly = false;
        this.code = openMeta();
    }

    private SqlStore(SQLiteDatabase db) {
        this.db = db;
        this.counter = null;
        this.readOnly = true;
        this.code = text("SELECT v FROM meta WHERE k='system_code'");
    }

    /** פותח (או יוצר) את המסד. @throws Damaged אם הוא פגום; Failure אם הוא מגרסה חדשה יותר. */
    public static SqlStore open(File file, IdCounter counter) {
        File dir = file.getParentFile();
        if (dir != null) dir.mkdirs();
        if (file.isFile() && file.length() > 0) probe(file);                 // לפני כל פתיחה לכתיבה
        SQLiteDatabase db = connect(file, false);
        try {
            db.enableWriteAheadLogging();
            db.setForeignKeyConstraintsEnabled(true);
            int v = db.getVersion();
            if (v > VERSION) throw new Failure("database is newer: " + v);
            if (v == 0) create(db);
            else if (v == 1) upgradeTo2(db);
            return new SqlStore(db, counter);
        } catch (SQLiteException e) {
            db.close();
            if (corrupt(e)) throw new Damaged(String.valueOf(e.getMessage()));
            throw e;
        } catch (RuntimeException e) {
            db.close();
            throw e;
        }
    }

    /**
     * מ0.5: פותח עותק לקריאה בלבד — לבדיקה, לספירה ולגיבוי טקסט. לא כותב בו דבר (גם לא את המונה). כתיבה דרכו — Failure.
     * @throws Damaged אם הוא פגום; Failure אם אינו קיים, או מגרסה חדשה יותר, או שאינו מסד של התוכנה.
     */
    public static SqlStore openReadOnly(File file) {
        if (!file.isFile()) throw new Failure("no such file: " + file.getPath());
        String sig = sig(file);
        SQLiteDatabase db = connect(file, true);
        try {
            int v = db.getVersion();
            if (v > VERSION) throw new Failure("database is newer: " + v);
            if (v < 1) throw new Failure("not a database of this app");
            mustBeWhole(db, file, sig);
            return new SqlStore(db);
        } catch (SQLiteException e) {
            db.close();
            if (corrupt(e)) throw new Damaged(String.valueOf(e.getMessage()));
            throw e;
        } catch (RuntimeException e) {
            db.close();
            throw e;
        }
    }

    /**
     * בדיקה בפתיחה, לקריאה בלבד: גרסה ותקינות. מסד פגום לא נפתח לכתיבה בכלל — גם לא לשינוי שיטת היומן (שכותב בכותרת).
     */
    private static void probe(File file) {
        String sig = sig(file);
        SQLiteDatabase db;
        try {
            db = connect(file, true);
        } catch (SQLiteException e) {                                         // לא פגם (פגם — Damaged מ־connect): לא חוסמים את הפתיחה
            Log.e("SqlStore.probe", e);
            return;
        }
        try {
            int v = db.getVersion();
            if (v > VERSION) throw new Failure("database is newer: " + v);
            if (v > 0) mustBeWhole(db, file, sig);
        } catch (SQLiteException e) {
            if (corrupt(e)) throw new Damaged(String.valueOf(e.getMessage()));
            Log.e("SqlStore.probe", e);                                        // כמו למעלה: הבדיקה היא הגנה נוספת, לא תנאי לפתיחה
        } finally {
            db.close();
        }
    }

    private static SQLiteDatabase connect(File file, boolean readOnly) {
        try {
            return readOnly
                    ? SQLiteDatabase.openDatabase(file.getPath(), null, SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS, KEEP)
                    : SQLiteDatabase.openOrCreateDatabase(file.getPath(), null, KEEP);
        } catch (SQLiteException e) {
            if (corrupt(e)) throw new Damaged(String.valueOf(e.getMessage()));
            throw e;
        }
    }

    /** האם השגיאה אומרת שהקובץ פגום (ולא, למשל, שאין הרשאה). */
    static boolean corrupt(Throwable t) {
        if (t instanceof SQLiteDatabaseCorruptException) return true;
        String m = String.valueOf(t.getMessage()).toLowerCase();
        return m.contains("malformed") || m.contains("not a database") || m.contains("corrupt") || m.contains("notadb");
    }

    /**
     * מ0.42 (ממצא 1 של מ0.41: העברת קובץ — 1.5 שניות; כל אחד משבעת המסדים נפתח, וכל פתיחה — quick_check): קובץ שנמצא תקין,
     * ומאז לא השתנה (גודל ותאריך, שלו ושל קובץ היומן שלו — לפני הפתיחה) — לא נבדק שוב. השתנה, הוחלף (שחזור) — נבדק.
     */
    private static final java.util.Map<String, String> whole = new java.util.concurrent.ConcurrentHashMap<String, String>();

    private static String sig(File f) {
        File w = new File(f.getPath() + "-wal");
        return f.length() + ":" + f.lastModified() + ":" + (w.isFile() ? w.length() + ":" + w.lastModified() : "-");
    }

    private static void mustBeWhole(SQLiteDatabase db, File file, String sig) {
        String k = file.getAbsolutePath();
        if (sig.equals(whole.get(k))) return;
        whole.remove(k);
        mustBeWhole(db);
        whole.put(k, sig);
    }

    /** בדיקת התקינות הקצרה. Damaged אם נמצא פגם. */
    private static void mustBeWhole(SQLiteDatabase db) {
        Cursor c = db.rawQuery("PRAGMA quick_check", null);
        try {
            if (!c.moveToFirst()) throw new Damaged("quick_check returned nothing");
            String r = c.getString(0);
            if (!"ok".equals(r)) throw new Damaged("quick_check: " + r);
        } finally {
            c.close();
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
        if (readOnly) throw new Failure("read-only copy");
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
    public long createSmart(String name, long parent) {
        String n = Stores.clean(name, "name");
        db.beginTransaction();
        try {
            if (parent != 0) mustBeList(parent);
            long id = create(SMART, n);
            if (parent != 0) append(parent, id, System.currentTimeMillis());
            db.setTransactionSuccessful();
            return id;
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public long addTitle(long list, String text) {
        String t = Stores.clean(text, "text");
        db.beginTransaction();
        try {
            mustBeList(list);
            long id = create(TITLE, t);
            append(list, id, System.currentTimeMillis());
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
        // הבדיקות לפני העסקה: כישלון בתוך עסקה פנימית היה מבטל גם את העסקה החיצונית (atomic)
        mustBeList(list);
        Item it = read(item);
        if (it == null) throw new Failure("no such item " + item);
        if (prop(item, P_TRASHED) != null) throw new Failure("item is in the bin: " + item);
        boolean there = num("SELECT COUNT(*) FROM members WHERE list_id=? AND item_id=?", s(list), s(item)) > 0;
        if (!there && it.type == LIST && makesCycle(list, item)) throw new Failure("cycle: " + item + " already holds " + list);
        db.beginTransaction();
        try {
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

    /** הרשימות שבתוך רשימה (צעד אחד). */
    private List<Long> subLists(long list) {
        List<Long> out = new ArrayList<Long>();
        Cursor c = db.rawQuery("SELECT m.item_id FROM members m JOIN items i ON i.id = m.item_id WHERE m.list_id=? AND i.type=?",
                new String[]{s(list), s(LIST)});
        try {
            while (c.moveToNext()) out.add(c.getLong(0));
        } finally {
            c.close();
        }
        return out;
    }

    @Override
    public boolean contains(long outer, long inner) {
        return containsVia(outer, inner);
    }

    /** חיפוש לרוחב דרך הרשימות שבתוך הרשימות; מבקר בכל רשימה פעם אחת (מעגל ישן לא יתקע). */
    private boolean containsVia(long outer, long inner) {
        java.util.Set<Long> seen = new java.util.HashSet<Long>();
        java.util.ArrayDeque<Long> q = new java.util.ArrayDeque<Long>();
        q.add(outer);
        seen.add(outer);
        while (!q.isEmpty()) {
            long l = q.poll();
            if (num("SELECT COUNT(*) FROM members WHERE list_id=? AND item_id=?", s(l), s(inner)) > 0) return true;
            for (Long sub : subLists(l)) if (seen.add(sub)) q.add(sub);
        }
        return false;
    }

    @Override
    public boolean makesCycle(long list, long item) {
        return list == item || contains(item, list);
    }

    @Override
    public void trash(long item) {
        Stores.mayTrash(read(item), item);
        if (prop(item, P_TRASHED) != null) return;
        db.beginTransaction();
        try {
            List<long[]> from = new ArrayList<long[]>();
            Cursor c = db.rawQuery("SELECT list_id, position FROM members WHERE item_id=? ORDER BY list_id", new String[]{s(item)});
            try {
                while (c.moveToNext()) from.add(new long[]{c.getLong(0), c.getLong(1)});
            } finally {
                c.close();
            }
            db.execSQL("DELETE FROM members WHERE item_id=?", new Object[]{item});
            for (long[] f : from) closeGap(f[0], f[1]);
            setProp(item, P_TRASH_FROM, Stores.joinFrom(from));
            setProp(item, P_TRASHED, String.valueOf(System.currentTimeMillis()));
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void restore(long item) {
        if (read(item) == null) throw new Failure("no such item " + item);
        if (prop(item, P_TRASHED) == null) throw new Failure("not in the bin: " + item);
        db.beginTransaction();
        try {
            List<long[]> from = Stores.parseFrom(prop(item, P_TRASH_FROM));
            setProp(item, P_TRASHED, null);
            setProp(item, P_TRASH_FROM, null);
            for (long[] f : from) {
                Item l = read(f[0]);
                if (l == null || l.type != LIST) continue;
                if (addMember(f[0], item)) move(f[0], item, (int) f[1]);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void purge(long item) {
        Item it = read(item);
        if (it == null) throw new Failure("no such item " + item);
        boolean loose = it.type == FILE && num("SELECT COUNT(*) FROM members WHERE item_id=?", s(item)) == 0;   // מ0.41: קובץ שאינו באף רשימה
        if (prop(item, P_TRASHED) == null && !loose) throw new Failure("not in the bin: " + item);
        db.execSQL("DELETE FROM items WHERE id=?", new Object[]{item});        // מאפיינים ופרטי הקובץ — במחיקה מדורגת; חברויות אין
    }

    @Override
    public List<Item> trashed() {
        return itemsWhere("id IN (SELECT item_id FROM props WHERE k=?)", P_TRASHED);
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
            Stores.mustBeListOrSmart(read(list), list);
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
    public void moveFile(long item, String path, String drive) {
        if (file(item) == null) throw new Failure("not a file: " + item);
        Stores.checkFile(new FileInfo(path, drive, 0, 0, null));
        db.beginTransaction();
        try {
            long had = findFile(path);
            if (had != 0 && had != item) throw new Failure("file already exists: " + path);
            db.execSQL("UPDATE files SET path=?, drive=? WHERE item_id=?", new Object[]{path, drive, item});
            db.execSQL("UPDATE items SET name=?, changed=? WHERE id=?", new Object[]{Stores.fileName(path), System.currentTimeMillis(), item});
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
    public String origin(long item) {
        return text("SELECT origin_key FROM items WHERE id=?", s(item));
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

    private List<Item> itemsWhere(String where, String... args) {
        List<Item> out = new ArrayList<Item>();
        Cursor c = db.rawQuery("SELECT id, type, name, created, changed FROM items WHERE " + where + " ORDER BY id", args);
        try {
            while (c.moveToNext()) out.add(row(c));
        } finally {
            c.close();
        }
        return out;
    }

    @Override
    public List<Item> smarts() {
        return itemsWhere("type=?", s(SMART));
    }

    @Override
    public List<Item> unassigned() {
        return itemsWhere("type IN (?,?,?) AND id NOT IN (SELECT item_id FROM members) AND id NOT IN (SELECT item_id FROM props WHERE k=?)",
                s(FILE), s(NOTE), s(TITLE), P_TRASHED);
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

    // ---------- מ0.5: עותק, וגישה ישירה לגיבוי הטקסט (DbDump) ----------

    /** שורה מתוצאת שאילתה: הערכים כטקסט, null לריק. */
    interface Row {
        void row(String[] v);
    }

    /** מריץ שאילתה ומעביר כל שורה (cols עמודות) — לגיבוי הטקסט. */
    void each(String sql, int cols, Row r) {
        Cursor c = db.rawQuery(sql, null);
        try {
            while (c.moveToNext()) {
                String[] v = new String[cols];
                for (int i = 0; i < cols; i++) v[i] = c.isNull(i) ? null : c.getString(i);
                r.row(v);
            }
        } finally {
            c.close();
        }
    }

    /** כתיבה ישירה — רק לשחזור מגיבוי טקסט, בתוך rawWork. */
    void rawExec(String sql, Object[] args) {
        if (readOnly) throw new Failure("read-only copy");
        db.execSQL(sql, args);
    }

    /** עסקה אחת לשחזור: הכול או כלום. */
    void rawWork(Runnable r) {
        if (readOnly) throw new Failure("read-only copy");
        db.beginTransaction();
        try {
            r.run();
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /** כמה פריטים יש במסד (שחזור נכנס רק למסד ריק). */
    long itemCount() {
        return num("SELECT COUNT(*) FROM items");
    }

    /** כמה חברויות יש במסד. */
    long memberCount() {
        return num("SELECT COUNT(*) FROM members");
    }

    /**
     * מ0.5: עותק עקבי של כל המסד לקובץ חדש (שלא קיים). העותק — קובץ אחד שלם, בלי יומן. לא בתוך עסקה.
     * SQLite 3.27 ומעלה (אנדרואיד 11 ומעלה): VACUUM INTO. לפני כן: נועלים כתיבה, מעתיקים את הקובץ ואת יומן ה־WAL,
     * ופותחים את העותק כדי שיאחד אותם.
     */
    public void copyTo(File out) {
        if (out.exists()) throw new Failure("target exists: " + out.getPath());
        File dir = out.getParentFile();
        if (dir != null) dir.mkdirs();
        if (!forceFileCopy && atLeast(text("SELECT sqlite_version()"), 3, 27)) {
            db.execSQL("VACUUM INTO '" + out.getPath().replace("'", "''") + "'");
            return;
        }
        File main = new File(path()), wal = new File(path() + "-wal"), outWal = new File(out.getPath() + "-wal");
        db.beginTransaction();                                                 // באנדרואיד: נעילת כתיבה; היומן לא משתנה עד הסוף
        try {
            copyFile(main, out);
            if (wal.isFile()) copyFile(wal, outWal);
        } catch (IOException e) {
            throw new Failure("copy failed", e);
        } finally {
            db.endTransaction();
        }
        SQLiteDatabase c = SQLiteDatabase.openOrCreateDatabase(out.getPath(), null, KEEP);
        try {
            Cursor r = c.rawQuery("PRAGMA journal_mode=DELETE", null);       // מאחד את היומן לתוך הקובץ ומוחק אותו
            try {
                r.moveToFirst();
            } finally {
                r.close();
            }
        } finally {
            c.close();
        }
        new File(out.getPath() + "-shm").delete();
        if (outWal.exists()) throw new Failure("journal was not folded into the copy");
    }

    /** הקובץ של המסד הראשי. */
    private String path() {
        Cursor c = db.rawQuery("PRAGMA database_list", null);
        try {
            while (c.moveToNext()) if ("main".equals(c.getString(1))) return c.getString(2);
            return "";
        } finally {
            c.close();
        }
    }

    static boolean atLeast(String version, int major, int minor) {
        try {
            String[] p = version.split("\\.");
            int a = Integer.parseInt(p[0]), b = p.length > 1 ? Integer.parseInt(p[1]) : 0;
            return a > major || (a == major && b >= minor);
        } catch (RuntimeException e) {
            return false;
        }
    }

    static void copyFile(File from, File to) throws IOException {
        InputStream in = new FileInputStream(from);
        try {
            OutputStream out = new FileOutputStream(to);
            try {
                byte[] b = new byte[64 * 1024];
                int n;
                while ((n = in.read(b)) > 0) out.write(b, 0, n);
                ((FileOutputStream) out).getFD().sync();
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
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
