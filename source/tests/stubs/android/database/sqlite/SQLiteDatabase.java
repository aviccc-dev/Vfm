package android.database.sqlite;

import android.database.Cursor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

// תחליף לבדיקות על המחשב: SQLiteDatabase שמדבר עם SQLite אמיתי של המחשב דרך tests/bridge/sqlite_bridge.py.
// כך קוד SqlStore האמיתי רץ בכל בנייה מול מנוע SQL אמיתי, בלי אנדרואיד.
public class SQLiteDatabase {

    public interface CursorFactory { }

    private final Process proc;
    private final BufferedReader in;
    private final BufferedWriter out;
    private boolean inTx, txOk;

    private SQLiteDatabase(Process p) throws IOException {
        proc = p;
        in = new BufferedReader(new InputStreamReader(p.getInputStream(), "UTF-8"));
        out = new BufferedWriter(new OutputStreamWriter(p.getOutputStream(), "UTF-8"));
    }

    public static SQLiteDatabase openOrCreateDatabase(File file, CursorFactory factory) {
        try {
            File script = new File(System.getProperty("vfm.src"), "tests/bridge/sqlite_bridge.py");
            SQLiteDatabase db = new SQLiteDatabase(new ProcessBuilder("python3", "-I", script.getPath()).redirectErrorStream(false).start());
            db.call("open", esc(file.getPath()));
            return db;
        } catch (IOException e) {
            throw new RuntimeException("python3 bridge: " + e, e);
        }
    }

    public static boolean deleteDatabase(File file) {
        boolean any = file.delete();
        for (String suffix : new String[]{"-journal", "-wal", "-shm"}) any |= new File(file.getPath() + suffix).delete();
        return any;
    }

    // ---------- פרוטוקול ----------

    private static String esc(String s) {
        return s.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n").replace("\r", "\\r");
    }

    private static String unesc(String s) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(++i);
                b.append(n == 't' ? '\t' : n == 'n' ? '\n' : n == 'r' ? '\r' : n);
            } else b.append(c);
        }
        return b.toString();
    }

    private static String arg(Object o) {
        if (o == null) return "N";
        if (o instanceof Number) return "I" + ((Number) o).longValue();
        return "S" + esc(o.toString());
    }

    private synchronized String[] call(String op, String... fields) {
        try {
            StringBuilder sb = new StringBuilder(op);
            for (String f : fields) sb.append('\t').append(f);
            out.write(sb.append('\n').toString());
            out.flush();
            String line = in.readLine();
            if (line == null) throw new SQLiteException("bridge closed");
            String[] p = line.split("\t", -1);
            if (p[0].equals("ERR")) throw new SQLiteException(unesc(p.length > 1 ? p[1] : ""));
            return p;
        } catch (IOException e) {
            throw new SQLiteException(e.toString());
        }
    }

    // ---------- מה ש־SqlStore משתמש בו ----------

    public boolean enableWriteAheadLogging() {
        call("exec", esc("PRAGMA journal_mode=WAL"));
        return true;
    }

    public void setForeignKeyConstraintsEnabled(boolean on) {
        call("exec", esc("PRAGMA foreign_keys=" + (on ? "ON" : "OFF")));
    }

    public int getVersion() {
        Cursor c = rawQuery("PRAGMA user_version", null);
        try {
            c.moveToFirst();
            return c.getInt(0);
        } finally {
            c.close();
        }
    }

    public void setVersion(int v) {
        call("exec", esc("PRAGMA user_version=" + v));
    }

    public void beginTransaction() {
        call("begin");
        inTx = true;
        txOk = false;
    }

    public void setTransactionSuccessful() {
        txOk = true;
    }

    public void endTransaction() {
        if (!inTx) throw new IllegalStateException("no transaction");
        inTx = false;
        call(txOk ? "commit" : "rollback");
    }

    public void execSQL(String sql) {
        execSQL(sql, new Object[0]);
    }

    public void execSQL(String sql, Object[] args) {
        String[] f = new String[1 + args.length];
        f[0] = esc(sql);
        for (int i = 0; i < args.length; i++) f[i + 1] = arg(args[i]);
        call("exec", f);
    }

    public Cursor rawQuery(String sql, String[] args) {
        String[] f = new String[1 + (args == null ? 0 : args.length)];
        f[0] = esc(sql);
        if (args != null) for (int i = 0; i < args.length; i++) f[i + 1] = "S" + esc(args[i]);
        String[] head = call("query", f);
        int cols = Integer.parseInt(head[1]), n = Integer.parseInt(head[2]);
        List<String[]> rows = new ArrayList<String[]>();
        try {
            for (int i = 0; i < n; i++) rows.add(in.readLine().split("\t", -1));
        } catch (IOException e) {
            throw new SQLiteException(e.toString());
        }
        return new Rows(rows, cols);
    }

    public void close() {
        try {
            call("close");
        } catch (RuntimeException ignored) {
        }
        proc.destroy();
    }

    private static final class Rows implements Cursor {
        private final List<String[]> rows;
        private int at = -1;

        Rows(List<String[]> rows, int cols) {
            this.rows = rows;
        }

        @Override public boolean moveToFirst() { at = 0; return !rows.isEmpty(); }
        @Override public boolean moveToNext() { at++; return at < rows.size(); }
        @Override public boolean isNull(int c) { return rows.get(at)[c].equals("N"); }
        @Override public String getString(int c) { String v = rows.get(at)[c]; return v.equals("N") ? null : unesc(v.substring(1)); }
        @Override public long getLong(int c) { String v = rows.get(at)[c]; return v.equals("N") ? 0 : Long.parseLong(unesc(v.substring(1))); }
        @Override public int getInt(int c) { return (int) getLong(c); }
        @Override public void close() { }
    }
}
