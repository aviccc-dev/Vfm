package com.tikiyot.lab;

// 1.5 — מדידת כתיבה ואמינות של מסד SQLite במקום נתון, בשיטת יומן נתונה, בצורת כתיבה נתונה.
// ריצה אחת = מסד חדש, כתיבה, קריאה, בדיקות שלמות, סגירה, פתיחה מחדש ובדיקה שוב, ניקוי.

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.io.File;

final class DbBench {

    private DbBench() { }

    static final class Result {
        boolean ok;
        String err = "";
        long insertNs, readNs;
        String journal = "?", sync = "?", side = "";
    }

    static Result run(File dir, boolean wal, int shape) {
        Result r = new Result();
        File f = new File(dir, "bench.db");
        SQLiteDatabase db = null;
        try {
            dir.mkdirs();
            if (!dir.isDirectory()) throw new IllegalStateException("התיקייה לא נוצרה: " + dir);
            SQLiteDatabase.deleteDatabase(f);
            Object[][] rows = DbPlan.rowsFor(DbPlan.rows(shape));
            int n = rows.length;

            db = SQLiteDatabase.openOrCreateDatabase(f, null);
            if (wal) db.enableWriteAheadLogging();
            else {
                db.disableWriteAheadLogging();
                pragma(db, "journal_mode=DELETE");
            }
            r.journal = pragma(db, "journal_mode");
            r.sync = pragma(db, "synchronous");
            db.execSQL(DbPlan.CREATE);
            db.execSQL(DbPlan.CREATE_INDEX);

            long t0 = System.nanoTime();
            insert(db, shape, rows);
            r.insertNs = System.nanoTime() - t0;
            r.side = side(f);

            t0 = System.nanoTime();
            String why = verify(db, n);
            for (int k = 0; k < DbPlan.LOOKUPS && why == null; k++) {
                int id = 1 + (int) (((long) k * 7919) % n);
                String got = text(db, "SELECT sig FROM items WHERE id=" + id);
                if (!DbPlan.row(id)[4].equals(got)) why = "שורה " + id + " חזרה אחרת";
            }
            r.readNs = System.nanoTime() - t0;
            if (why != null) throw new IllegalStateException(why);

            db.close();
            db = SQLiteDatabase.openDatabase(f.getPath(), null, SQLiteDatabase.OPEN_READWRITE);
            why = verify(db, n);
            if (why != null) throw new IllegalStateException("אחרי פתיחה מחדש: " + why);
            r.ok = true;
        } catch (Throwable t) {
            r.err = String.valueOf(t);
        } finally {
            try {
                if (db != null) db.close();
            } catch (Throwable ignored) { }
            try {
                SQLiteDatabase.deleteDatabase(f);
            } catch (Throwable ignored) { }
        }
        return r;
    }

    private static void insert(SQLiteDatabase db, int shape, Object[][] rows) {
        if (shape == 0) {
            for (Object[] row : rows) db.execSQL(DbPlan.INSERT, row);
        } else if (shape == 1) {
            for (int b = 0; b < DbPlan.BATCHES; b++) {
                db.beginTransaction();
                try {
                    for (int j = 0; j < DbPlan.BATCH_ROWS; j++) db.execSQL(DbPlan.INSERT, rows[b * DbPlan.BATCH_ROWS + j]);
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }
        } else {
            db.beginTransaction();
            try {
                for (Object[] row : rows) db.execSQL(DbPlan.INSERT, row);
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
    }

    // ספירה, שני סכומים, ובדיקת השלמות של SQLite עצמו. null = הכול תקין.
    private static String verify(SQLiteDatabase db, int n) {
        long c = num(db, "SELECT COUNT(*) FROM items");
        if (c != n) return "ספירה " + c + " במקום " + n;
        if (num(db, "SELECT SUM(id) FROM items") != DbPlan.sumIds(n)) return "סכום המספרים שונה";
        if (num(db, "SELECT SUM(LENGTH(path)) FROM items") != DbPlan.sumPathLen(n)) return "תוכן הנתיבים שונה";
        String ic = text(db, "PRAGMA integrity_check");
        if (!"ok".equals(ic)) return "integrity_check: " + ic;
        return null;
    }

    private static String side(File f) {
        StringBuilder b = new StringBuilder();
        for (String s : new String[]{"-journal", "-wal", "-shm"}) {
            File x = new File(f.getPath() + s);
            if (x.exists()) b.append(s).append(' ').append(x.length()).append("B ");
        }
        return b.toString().trim();
    }

    private static String pragma(SQLiteDatabase db, String p) {
        return text(db, "PRAGMA " + p);
    }

    private static String text(SQLiteDatabase db, String sql) {
        Cursor c = db.rawQuery(sql, null);
        try {
            return c.moveToFirst() ? c.getString(0) : null;
        } finally {
            c.close();
        }
    }

    private static long num(SQLiteDatabase db, String sql) {
        Cursor c = db.rawQuery(sql, null);
        try {
            return c.moveToFirst() ? c.getLong(0) : -1;
        } finally {
            c.close();
        }
    }
}
