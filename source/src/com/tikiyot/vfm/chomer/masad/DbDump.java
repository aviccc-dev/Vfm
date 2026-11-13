package com.tikiyot.vfm;

// גיבוי טקסט של מסד (מ0.5). אחראי: לכתוב את כל המסד לקובץ טקסט בפורמט מתועד, לקרוא אותו בחזרה — ולסרב לקובץ חסר,
// קטוע או שונה — ולשחזר ממנו למסד ריק. אסור לו: למזג עם נתונים קיימים; לדעת איפה הגיבויים נשמרים.
//
// הפורמט, גרסה 1 — UTF-8, שורה לכל רשומה, שדות מופרדים בטאב:
//   שורה 1: כותרת לקורא (לא נבדקת)
//   format <גרסת הפורמט>
//   made <מתי נעשה, אלפיות שנייה>
//   ― התוכן, שנכנס לחתימה: ―
//   schema <גרסת הטבלאות>
//   X <מפתח> <ערך>                                                      — meta: המונה הבא, קוד המערכת
//   I <מספר> <סוג> <נוצר> <שונה> <שם> <extra> <origin_sys> <origin_id> <origin_key>   — פריט
//   P <פריט> <מפתח> <ערך>                                              — מאפיין
//   F <פריט> <מסלול> <כונן> <גודל> <תאריך> <חתימה>                      — פרטי קובץ
//   M <רשימה> <פריט> <מקום> <נוסף>                                      — חברות
//   ― סוף התוכן ―
//   end <מספר שורות התוכן> <SHA-256 של שורות התוכן, כל אחת עם סוף שורה>
// בתוך שדה: "\\" לקו נטוי, "\t" לטאב, "\n" ו־"\r" לסוף שורה; "\N" לבד — ריק (NULL).
// אותו מסד נותן תמיד אותו תוכן ואותה חתימה — גם עותק שלו. כך משווים מסד, עותק וגיבוי טקסט.

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

final class DbDump {

    static final int FORMAT = 1;
    private static final String NULL = "\\N";

    private DbDump() { }

    /** מה שנקרא מקובץ — כבר נבדק (שלם, חתימה תואמת). */
    static final class Data {
        final int format, schema;
        final long made;
        /** הרשומות, כל אחת שדות אחרי פענוח (null לריק); הראשון — סוג הרשומה. */
        final List<String[]> records;
        final String hash;

        Data(int format, int schema, long made, List<String[]> records, String hash) {
            this.format = format;
            this.schema = schema;
            this.made = made;
            this.records = records;
            this.hash = hash;
        }

        int count(String tag) {
            int n = 0;
            for (String[] r : records) if (r[0].equals(tag)) n++;
            return n;
        }
    }

    /** חתימת התוכן של המסד — זהה לזו שבגיבוי הטקסט שלו ושל כל עותק שלו. */
    static String hash(SqlStore s) {
        Sink k = new Sink(null);
        lines(s, k);
        return k.hex();
    }

    /** כותב גיבוי טקסט (דרך קובץ זמני ושינוי שם). מחזיר את חתימת התוכן. */
    static String write(SqlStore s, File f, long made) throws IOException {
        File part = new File(f.getPath() + ".part");
        FileOutputStream fo = new FileOutputStream(part);
        String hex;
        int n;
        boolean ok = false;
        try {
            Writer w = new OutputStreamWriter(fo, "UTF-8");
            w.write(Words.Db.DUMP_TITLE + "\n");
            w.write("format\t" + FORMAT + "\n");
            w.write("made\t" + made + "\n");
            Sink k = new Sink(w);
            lines(s, k);
            hex = k.hex();
            n = k.count;
            w.write("end\t" + n + "\t" + hex + "\n");
            w.flush();
            fo.getFD().sync();
            ok = true;
        } finally {
            fo.close();
            if (!ok) part.delete();
        }
        if (!part.renameTo(f)) {
            part.delete();
            throw new IOException("cannot rename " + part.getPath());
        }
        return hex;
    }

    /** קורא ובודק. @throws Store.Failure אם הקובץ קטוע, שונה, מגרסה חדשה יותר או לא בפורמט. */
    static Data read(File f) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
        try {
            String title = in.readLine();
            if (title == null) throw new Store.Failure("empty file");
            int format = (int) field(in.readLine(), "format");
            if (format > FORMAT) throw new Store.Failure("newer format: " + format);
            if (format < 1) throw new Store.Failure("bad format: " + format);
            long made = field(in.readLine(), "made");
            Sink k = new Sink(null);
            List<String[]> records = new ArrayList<String[]>();
            int schema = -1;
            String line;
            boolean ended = false;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("end\t")) {
                    String[] e = line.split("\t", -1);
                    if (e.length != 3) throw new Store.Failure("bad end line");
                    if (Integer.parseInt(e[1]) != k.count) throw new Store.Failure("line count differs: " + e[1] + " / " + k.count);
                    if (!e[2].equals(k.hex())) throw new Store.Failure("content signature differs");
                    ended = true;
                    if (in.readLine() != null) throw new Store.Failure("text after the end line");
                    break;
                }
                k.line(line);
                String[] p = line.split("\t", -1);
                if (k.count == 1) {
                    if (p.length != 2 || !p[0].equals("schema")) throw new Store.Failure("missing schema line");
                    schema = Integer.parseInt(p[1]);
                    if (schema > SqlStore.VERSION) throw new Store.Failure("newer schema: " + schema);
                    continue;
                }
                int want = width(p[0]);
                if (want < 0 || p.length != want) throw new Store.Failure("bad record at line " + (k.count + 3));
                for (int i = 1; i < p.length; i++) p[i] = unesc(p[i]);
                records.add(p);
            }
            if (!ended) throw new Store.Failure("cut short: no end line");
            if (schema < 0) throw new Store.Failure("missing schema line");
            return new Data(format, schema, made, records, k.hex());
        } catch (NumberFormatException e) {
            throw new Store.Failure("bad number: " + e.getMessage());
        } finally {
            in.close();
        }
    }

    /** משחזר למסד ריק, בעסקה אחת: הכול או כלום. מסד שיש בו פריטים — Failure (בלי מיזוג). */
    static void load(final Data d, final SqlStore target) {
        if (target.itemCount() > 0) throw new Store.Failure("restore goes only into an empty database");
        target.rawWork(new Runnable() {
            @Override
            public void run() {
                target.rawExec("DELETE FROM meta", new Object[0]);
                for (String[] r : d.records) {
                    String t = r[0];
                    if (t.equals("X")) {
                        target.rawExec("INSERT INTO meta(k, v) VALUES(?,?)", new Object[]{r[1], r[2]});
                    } else if (t.equals("I")) {
                        target.rawExec("INSERT INTO items(id, type, created, changed, name, extra, origin_sys, origin_id, origin_key) "
                                + "VALUES(?,?,?,?,?,?,?,?,?)", new Object[]{num(r[1]), num(r[2]), num(r[3]), num(r[4]), r[5], r[6], r[7],
                                r[8] == null ? null : num(r[8]), r[9]});
                    } else if (t.equals("P")) {
                        target.rawExec("INSERT INTO props(item_id, k, v) VALUES(?,?,?)", new Object[]{num(r[1]), r[2], r[3]});
                    } else if (t.equals("F")) {
                        target.rawExec("INSERT INTO files(item_id, path, drive, size, mtime, sig) VALUES(?,?,?,?,?,?)",
                                new Object[]{num(r[1]), r[2], r[3], num(r[4]), num(r[5]), r[6]});
                    } else if (t.equals("M")) {
                        target.rawExec("INSERT INTO members(list_id, item_id, position, added) VALUES(?,?,?,?)",
                                new Object[]{num(r[1]), num(r[2]), num(r[3]), num(r[4])});
                    }
                }
            }
        });
    }

    // ---------- פנימי ----------

    /** שורות התוכן, בסדר קבוע. */
    private static void lines(SqlStore s, final Sink k) {
        k.line("schema\t" + s.getVersionForTest());
        s.each("SELECT k, v FROM meta ORDER BY k", 2, rec(k, "X"));
        s.each("SELECT id, type, created, changed, name, extra, origin_sys, origin_id, origin_key FROM items ORDER BY id", 9, rec(k, "I"));
        s.each("SELECT item_id, k, v FROM props ORDER BY item_id, k", 3, rec(k, "P"));
        s.each("SELECT item_id, path, drive, size, mtime, sig FROM files ORDER BY item_id", 6, rec(k, "F"));
        s.each("SELECT list_id, item_id, position, added FROM members ORDER BY list_id, position, item_id", 4, rec(k, "M"));
    }

    private static SqlStore.Row rec(final Sink k, final String tag) {
        return new SqlStore.Row() {
            @Override
            public void row(String[] v) {
                StringBuilder sb = new StringBuilder(tag);
                for (String x : v) sb.append('\t').append(esc(x));
                k.line(sb.toString());
            }
        };
    }

    /** כמה שדות (כולל הסוג) בכל סוג רשומה; ‎-1 — סוג לא מוכר. */
    private static int width(String tag) {
        if (tag.equals("X")) return 3;
        if (tag.equals("I")) return 10;
        if (tag.equals("P")) return 4;
        if (tag.equals("F")) return 7;
        if (tag.equals("M")) return 5;
        return -1;
    }

    private static long field(String line, String name) {
        if (line == null) throw new Store.Failure("missing " + name + " line");
        String[] p = line.split("\t", -1);
        if (p.length != 2 || !p[0].equals(name)) throw new Store.Failure("missing " + name + " line");
        return Long.parseLong(p[1]);
    }

    private static Long num(String v) {
        if (v == null) throw new Store.Failure("missing number");
        return Long.valueOf(Long.parseLong(v));
    }

    static String esc(String s) {
        if (s == null) return NULL;
        StringBuilder b = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\') b.append("\\\\");
            else if (c == '\t') b.append("\\t");
            else if (c == '\n') b.append("\\n");
            else if (c == '\r') b.append("\\r");
            else b.append(c);
        }
        return b.toString();
    }

    static String unesc(String s) {
        if (s.equals(NULL)) return null;
        if (s.indexOf('\\') < 0) return s;
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '\\') {
                b.append(c);
                continue;
            }
            if (i + 1 >= s.length()) throw new Store.Failure("bad escape");
            char n = s.charAt(++i);
            if (n == '\\') b.append('\\');
            else if (n == 't') b.append('\t');
            else if (n == 'n') b.append('\n');
            else if (n == 'r') b.append('\r');
            else throw new Store.Failure("bad escape");
        }
        return b.toString();
    }

    /** מקבל שורות: סופר, מחשב חתימה, וכותב אם יש לאן. */
    private static final class Sink {
        private final Writer w;
        private final MessageDigest md;
        int count;

        Sink(Writer w) {
            this.w = w;
            try {
                md = MessageDigest.getInstance("SHA-256");
            } catch (Exception e) {
                throw new Store.Failure("no SHA-256", e);
            }
        }

        void line(String l) {
            try {
                byte[] b = (l + "\n").getBytes("UTF-8");
                md.update(b);
                if (w != null) w.write(l + "\n");
            } catch (IOException e) {
                throw new Store.Failure("write failed", e);
            }
            count++;
        }

        private String done;

        /** החתימה. אחריה לא מוסיפים שורות. */
        String hex() {
            if (done == null) done = Backups.hex(md.digest());
            return done;
        }
    }
}
