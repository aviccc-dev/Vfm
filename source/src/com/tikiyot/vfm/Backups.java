package com.tikiyot.vfm;

// הגיבויים של מסד אחד (מ0.5). אחראי: לעשות עותקים (יומי, לפני פעולה, ידני) וגיבוי טקסט שבועי; לרשום לכל קובץ חתימה;
// למחוק ישנים לפי הכללים; לבדוק את הגיבויים מול הרישום; לבנות מסד משוחזר ולהחליף בו את המסד.
// אסור לו: מסכים; לגעת בקובץ בתיקייה שאינו שלו (שם לא מוכר — לא נמחק ולא נבדק).
//
// התיקייה: <תיקיית התוכנה>/גיבוי מסד/<אופן>/ — גלויה, מחוץ לאחסון הפרטי (כך הגיבויים נשארים גם אם התוכנה נמחקת).
//   יומי <יום>.db                — כשפותחים את המסך, פעם ביום, ורק אם היה שינוי מאז הגיבוי האחרון.
//                                  נשמרים 7 האחרונים, ועוד אחד (האחרון) מכל אחד מ־4 השבועות שלפניהם.
//   לפני <פעולה> <יום שעה>.db     — לפני העתקה מהישן, מחיקת רשימה, מחיקה לצמיתות ושחזור. נשמרים 10 האחרונים.
//   טקסט <יום>.txt               — גיבוי טקסט (DbDump), פעם בשבוע כשהיה שינוי. נשמרים 5 האחרונים.
//   ידני <יום שעה>.db וגם .txt    — "גיבוי עכשיו". לא נמחקים לבד.
//   פגום <יום שעה>.db             — המסד הפגום כמו שהיה (עם היומן שלו, אם היה), לפני שחזור. לא נמחק לבד.
//   רישום.txt                    — לכל קובץ: גודל, SHA-256 של הקובץ, חתימת התוכן ומתי נעשה; ואיזה יום כבר נבדק.
// כל עותק נעשה קודם בתיקייה זמנית פרטית (SqlStore.copyTo), נפתח שם לקריאה ונבדק (תקינות וחתימת תוכן), ורק אז מועתק
// לתיקייה ונרשם. עותק שהתוכן שלו זהה לגיבוי האחרון — לא נשמר שוב. בדיקת גיבוי רשום: הקובץ באותו גודל ובאותה חתימה
// כמו ברישום (בלי לפתוח אותו); גיבוי לא רשום — נפתח ונבדק.

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class Backups {

    static final String DIR = "גיבוי מסד";  // בכונן
    static final String DAILY = "יומי", BEFORE = "לפני", MANUAL = "ידני", TEXT = "טקסט", DAMAGED = "פגום";  // בכונן
    static final String RECORD = "רישום.txt";  // בכונן
    /** הפעולות שלפניהן נעשה עותק — חלק משם הקובץ. */
    static final String OP_IMPORT = "העתקה מהישן", OP_DELETE = "מחיקת רשימה", OP_PURGE = "מחיקה לצמיתות", OP_RESTORE = "שחזור";  // בכונן

    static final int KEEP_DAILY = 7, KEEP_WEEKS = 4, KEEP_BEFORE = 10, KEEP_TEXT = 5, TEXT_EVERY_DAYS = 7;

    /** תוצאת בדיקה של גיבוי. */
    static final int OK = 0, OK_UNRECORDED = 1, CHANGED = 2, BAD = 3, KEPT_DAMAGED = 4;

    private static final Pattern NAME = Pattern.compile(
            "^(" + DAILY + "|" + BEFORE + " (.+)|" + MANUAL + "|" + TEXT + "|" + DAMAGED + ") "  // בכונן
                    + "(\\d{4}-\\d{2}-\\d{2})(?: (\\d{2})-(\\d{2})-(\\d{2}))?(?: \\((\\d+)\\))?\\.(db|txt)$");

    /** כל הכתיבות לתיקיות הגיבוי — אחת בכל פעם (הגיבוי היומי רץ ברקע). */
    private static final Object LOCK = new Object();

    final File dir, tmp;

    /** dir — תיקיית הגיבויים של המסד; tmp — תיקייה פרטית זמנית (עותקים לפני בדיקה). */
    Backups(File dir, File tmp) {
        this.dir = dir;
        this.tmp = tmp;
    }

    // ---------- גיבוי ----------

    static final class Entry {
        final File file;
        final String name, kind, op;
        final boolean text;
        final long when;
        /** מהרישום; ‎-1 / null — לא רשום. */
        long bytes = -1, made;
        String sha, content;

        Entry(File file, String kind, String op, boolean text, long when) {
            this.file = file;
            this.name = file.getName();
            this.kind = kind;
            this.op = op;
            this.text = text;
            this.when = when;
        }

        boolean recorded() {
            return sha != null;
        }

        /** מתי נעשה באמת: מהרישום; אם אינו רשום — מהשם (ביומי ובטקסט — רק היום, בלי שעה). */
        long time() {
            return made > 0 ? made : when;
        }
    }

    /** כל הגיבויים המוכרים בתיקייה, החדש קודם. */
    List<Entry> list() {
        Map<String, String[]> rec = readRecord().files;
        List<Entry> out = new ArrayList<Entry>();
        File[] k = dir.listFiles();
        if (k != null)
            for (File f : k) {
                Entry e = parse(f);
                if (e == null) continue;
                String[] r = rec.get(e.name);
                if (r != null) {
                    try {
                        e.bytes = Long.parseLong(r[1]);
                        e.sha = r[2];
                        e.content = r[3].isEmpty() ? null : r[3];
                        e.made = Long.parseLong(r[4]);
                    } catch (RuntimeException ignored) {
                        e.sha = null;
                    }
                }
                out.add(e);
            }
        Collections.sort(out, NEWEST_FIRST);
        return out;
    }

    private static final Comparator<Entry> NEWEST_FIRST = new Comparator<Entry>() {
        @Override
        public int compare(Entry a, Entry b) {
            long x = a.time(), y = b.time();
            return x != y ? (x > y ? -1 : 1) : b.name.compareTo(a.name);
        }
    };

    /** שם קובץ ← גיבוי, או null אם השם לא שלנו. */
    static Entry parse(File f) {
        Matcher m = NAME.matcher(f.getName());
        if (!m.matches()) return null;
        String kind = m.group(1).startsWith(BEFORE + " ") ? BEFORE : m.group(1);
        boolean text = m.group(8).equals("txt");
        if (text && !kind.equals(TEXT) && !kind.equals(MANUAL)) return null;     // טקסט — רק "טקסט" ו"ידני"
        if (!text && kind.equals(TEXT)) return null;
        long when;
        try {
            SimpleDateFormat d = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss", Locale.US);
            d.setLenient(false);
            when = d.parse(m.group(3) + " " + (m.group(4) == null ? "00-00-00" : m.group(4) + "-" + m.group(5) + "-" + m.group(6))).getTime();
        } catch (Exception e) {
            return null;
        }
        return new Entry(f, kind, kind.equals(BEFORE) ? m.group(2) : null, text, when);
    }

    /** עותק עקבי של המסד, שנבדק בתיקייה הזמנית. */
    static final class Snapshot {
        final File file;
        final String content;
        final int lists;
        final long items;

        Snapshot(File file, String content, int lists, long items) {
            this.file = file;
            this.content = content;
            this.lists = lists;
            this.items = items;
        }
    }

    /** עושה עותק בתיקייה הזמנית, פותח אותו לקריאה, בודק ומחשב חתימת תוכן. */
    Snapshot snapshot(SqlStore live) {
        tmp.mkdirs();
        File f = new File(tmp, "snap-" + System.nanoTime() + ".db");
        live.copyTo(f);
        try {
            SqlStore c = SqlStore.openReadOnly(f);
            try {
                return new Snapshot(f, DbDump.hash(c), c.lists().size(), c.itemCount());
            } finally {
                c.close();
            }
        } catch (RuntimeException e) {
            f.delete();
            throw e;
        }
    }

    /** עותקים זמניים שנשארו מריצה שנקטעה (התוכנה נסגרה באמצע) — נמחקים אחרי שעה. */
    private void sweep(long now) {
        File[] k = tmp.listFiles();
        if (k == null) return;
        for (File f : k)
            if ((f.getName().startsWith("snap-") || f.getName().startsWith("verify-")) && now - f.lastModified() > 3600000L) f.delete();
    }

    /** מה נעשה בגיבוי היומי — לדוח וליומן. */
    static final class Daily {
        boolean checkedAlready, copied, texted;
        final List<String> deleted = new ArrayList<String>();
    }

    /**
     * הגיבוי היומי: אם היום עוד לא נבדק — עותק (אם אין של היום ויש שינוי מאז הגיבוי האחרון), גיבוי טקסט (אם עבר שבוע
     * ויש שינוי), ומחיקת ישנים. עותק שזהה לאחרון לא נשמר.
     */
    Daily daily(SqlStore live, long now) throws IOException {
        synchronized (LOCK) {
            Daily r = new Daily();
            String today = day(now);
            sweep(now);
            Record rec = readRecord();
            if (today.equals(rec.checked)) {
                r.checkedAlready = true;
                return r;
            }
            Snapshot s = snapshot(live);
            try {
                List<Entry> all = list();
                Entry last = newestCopy(all);
                boolean hasToday = false;
                for (Entry e : all) if (e.kind.equals(DAILY) && day(e.when).equals(today)) hasToday = true;
                if (!hasToday && (last == null || !s.content.equals(last.content))) {
                    keepCopy(s, DAILY, null, now, false);
                    r.copied = true;
                }
                Entry lastText = newest(all, TEXT);
                if (lastText == null || (dayIndex(now) - dayIndex(lastText.when) >= TEXT_EVERY_DAYS && !s.content.equals(lastText.content))) {
                    keepText(s, TEXT, now, false);
                    r.texted = true;
                }
                r.deleted.addAll(prune());
                Record again = readRecord();
                again.checked = today;
                writeRecord(again);
                return r;
            } finally {
                s.file.delete();
            }
        }
    }

    /** עותק לפני פעולה הרסנית. null — התוכן זהה לגיבוי האחרון, כבר מוגן. */
    Entry before(SqlStore live, String op, long now) throws IOException {
        synchronized (LOCK) {
            Snapshot s = snapshot(live);
            try {
                Entry last = newestCopy(list());
                if (last != null && s.content.equals(last.content)) return null;
                Entry e = keepCopy(s, BEFORE, op, now, true);
                prune();
                return e;
            } finally {
                s.file.delete();
            }
        }
    }

    /** "גיבוי עכשיו": עותק וגם גיבוי טקסט, באותו שם זמן. מחזיר את שניהם. */
    List<Entry> manual(SqlStore live, long now) throws IOException {
        synchronized (LOCK) {
            Snapshot s = snapshot(live);
            try {
                List<Entry> out = new ArrayList<Entry>();
                out.add(keepCopy(s, MANUAL, null, now, true));
                out.add(keepText(s, MANUAL, now, true));
                return out;
            } finally {
                s.file.delete();
            }
        }
    }

    /** שומר את המסד הפגום כמו שהוא (קובץ ויומן), בלי SQLite — לפני שחזור. */
    Entry keepDamaged(File db, long now) throws IOException {
        synchronized (LOCK) {
            dir.mkdirs();
            File dest = unique(DAMAGED, null, now, true, "db");
            String sha = copyWithSha(db, dest);
            File wal = new File(db.getPath() + "-wal");
            if (wal.isFile() && wal.length() > 0) SqlStore.copyFile(wal, new File(dest.getPath() + "-wal"));
            put(dest, sha, null, now);
            return parse(dest);
        }
    }

    private Entry keepCopy(Snapshot s, String kind, String op, long now, boolean withTime) throws IOException {
        dir.mkdirs();
        File dest = unique(kind, op, now, withTime, "db");
        File part = new File(dir, dest.getName() + ".part");
        String sha;
        try {
            sha = copyWithSha(s.file, part);
        } catch (IOException e) {
            part.delete();
            throw e;
        }
        if (!part.renameTo(dest)) {
            part.delete();
            throw new IOException("cannot rename " + part.getPath());
        }
        put(dest, sha, s.content, now);
        return parse(dest);
    }

    private Entry keepText(Snapshot s, String kind, long now, boolean withTime) throws IOException {
        dir.mkdirs();
        File dest = unique(kind, null, now, withTime, "txt");
        SqlStore c = SqlStore.openReadOnly(s.file);
        String content;
        try {
            content = DbDump.write(c, dest, now);
        } finally {
            c.close();
        }
        if (!content.equals(s.content)) throw new IOException("text backup differs from the copy");
        put(dest, sha(dest), content, now);
        return parse(dest);
    }

    /** שם פנוי: "<סוג>[ <פעולה>] <יום>[ <שעה>][ (n)].<סיומת>". */
    private File unique(String kind, String op, long now, boolean withTime, String ext) {
        String base = kind + (op == null ? "" : " " + op) + " " + (withTime ? stamp(now) : day(now));
        File f = new File(dir, base + "." + ext);
        for (int n = 2; f.exists(); n++) f = new File(dir, base + " (" + n + ")." + ext);
        return f;
    }

    // ---------- מחיקת ישנים ----------

    /** מוחק לפי הכללים. מחזיר את שמות הקבצים שנמחקו. */
    List<String> prune() {
        synchronized (LOCK) {
            List<String> gone = new ArrayList<String>();
            List<Entry> del = toDelete(list());
            if (del.isEmpty()) return gone;
            Record rec = readRecord();
            for (Entry e : del) {
                if (e.file.delete()) {
                    gone.add(e.name);
                    rec.files.remove(e.name);
                }
            }
            try {
                writeRecord(rec);
            } catch (IOException ex) {
                Log.e("Backups.prune", ex);
            }
            return gone;
        }
    }

    /**
     * מה למחוק (פונקציה טהורה): יומיים — מעבר ל־7 האחרונים, חוץ מהאחרון בכל אחד מ־4 השבועות שלפניהם; "לפני" — מעבר ל־10;
     * טקסט — מעבר ל־5. ידניים ופגומים — לעולם לא.
     */
    static List<Entry> toDelete(List<Entry> all) {
        List<Entry> sorted = new ArrayList<Entry>(all);
        Collections.sort(sorted, NEWEST_FIRST);
        List<Entry> out = new ArrayList<Entry>();
        int daily = 0, before = 0, text = 0;
        Set<Long> weeks = new HashSet<Long>();
        for (Entry e : sorted) {
            if (e.kind.equals(DAILY)) {
                daily++;
                if (daily <= KEEP_DAILY) continue;
                long w = weekIndex(e.when);
                if (!weeks.contains(w) && weeks.size() < KEEP_WEEKS) {
                    weeks.add(w);
                    continue;
                }
                out.add(e);
            } else if (e.kind.equals(BEFORE)) {
                if (++before > KEEP_BEFORE) out.add(e);
            } else if (e.kind.equals(TEXT)) {
                if (++text > KEEP_TEXT) out.add(e);
            }
        }
        return out;
    }

    // ---------- בדיקה ----------

    /** בודק גיבוי אחד: רשום — גודל וחתימת קובץ מול הרישום; לא רשום — פתיחה ובדיקה. */
    int verify(Entry e) {
        if (e.kind.equals(DAMAGED)) return KEPT_DAMAGED;
        try {
            if (e.recorded()) return e.file.length() == e.bytes && sha(e.file).equals(e.sha) ? OK : CHANGED;
            if (e.text) {
                DbDump.read(e.file);
                return OK_UNRECORDED;
            }
            tmp.mkdirs();
            File c = new File(tmp, "verify-" + System.nanoTime() + ".db");
            try {
                SqlStore.copyFile(e.file, c);
                SqlStore.openReadOnly(c).close();
            } finally {
                c.delete();
            }
            return OK_UNRECORDED;
        } catch (Throwable t) {
            return BAD;
        }
    }

    /** סיכום הבדיקה של כל הגיבויים: [תקינים, שונו, פגומים, פגום שנשמר]. */
    int[] verifyAll(List<Entry> all) {
        int[] r = new int[4];
        for (Entry e : all) {
            int v = verify(e);
            if (v == OK || v == OK_UNRECORDED) r[0]++;
            else if (v == CHANGED) r[1]++;
            else if (v == BAD) r[2]++;
            else r[3]++;
        }
        return r;
    }

    // ---------- שחזור ----------

    /** מה יש במסד שנבנה. */
    static final class Built {
        final int lists;
        final long items, members;
        final String content;

        Built(int lists, long items, long members, String content) {
            this.lists = lists;
            this.items = items;
            this.members = members;
            this.content = content;
        }
    }

    /**
     * בונה מסד משוחזר בקובץ target (מה שהיה בו נמחק) מגיבוי — עותק (.db) או טקסט (.txt). המונה — של המסד האמיתי, כדי
     * שמספר זיהוי שכבר ניתן לא יחזור. גיבוי פגום, קטוע או שונה — חריגה, ושום דבר לא נשאר.
     */
    static Built build(File source, File target, IdCounter counter) throws IOException {
        return build(source, target, counter, null);
    }

    /**
     * כמו build, ו־expected — חתימת התוכן מהרישום (null: לא רשום). עותק שהתוכן שלו שונה ממה שנרשם — לא משוחזר, גם אם הוא
     * תקין כמסד (שינוי בערך אחד לא נתפס בבדיקת התקינות). הבדיקה לפני הפתיחה עם המונה, שמעלה את "המספר הבא".
     */
    static Built build(File source, File target, IdCounter counter, String expected) throws IOException {
        android.database.sqlite.SQLiteDatabase.deleteDatabase(target);
        try {
            SqlStore s;
            if (source.getName().endsWith(".txt")) {
                DbDump.Data d = DbDump.read(source);
                s = SqlStore.open(target, counter);
                try {
                    if (expected != null && !expected.equals(d.hash)) throw new Store.Failure("content differs from the record");
                    DbDump.load(d, s);
                    if (!DbDump.hash(s).equals(d.hash)) throw new Store.Failure("restored content differs from the text backup");
                } catch (RuntimeException e) {
                    s.close();
                    throw e;
                }
                s.close();
            } else {
                SqlStore.copyFile(source, target);
                if (expected != null) {
                    SqlStore ro = SqlStore.openReadOnly(target);
                    try {
                        if (!expected.equals(DbDump.hash(ro))) throw new Store.Failure("content differs from the record");
                    } finally {
                        ro.close();
                    }
                }
            }
            s = SqlStore.open(target, counter);                                // בדיקת תקינות, שדרוג טבלאות, ומונה לא יורד
            try {
                String bad = s.check();
                if (bad != null) throw new SqlStore.Damaged(bad);
                return new Built(s.lists().size(), s.itemCount(), s.memberCount(), DbDump.hash(s));
            } finally {
                s.close();
            }
        } catch (IOException e) {
            android.database.sqlite.SQLiteDatabase.deleteDatabase(target);
            throw e;
        } catch (RuntimeException e) {
            android.database.sqlite.SQLiteDatabase.deleteDatabase(target);
            throw e;
        }
    }

    /**
     * מחליף את המסד במסד שנבנה. שניהם סגורים. שינוי השם מחליף את הקובץ בפעולה אחת — בכל רגע יש בנתיב מסד שלם, הישן או
     * החדש. היומן הישן נמחק קודם (מסד סגור — היומן שלו כבר אוחד).
     */
    static void swap(File built, File db) throws IOException {
        if (!built.isFile()) throw new IOException("nothing to swap in: " + built.getPath());
        for (String suf : new String[]{"-wal", "-shm", "-journal"}) new File(db.getPath() + suf).delete();
        if (!built.renameTo(db)) throw new IOException("cannot replace " + db.getPath());
        File wal = new File(built.getPath() + "-wal");
        if (wal.isFile() && !wal.renameTo(new File(db.getPath() + "-wal"))) throw new IOException("cannot move the journal");
        new File(built.getPath() + "-shm").delete();
    }

    // ---------- הרישום ----------

    private static final class Record {
        String checked = "";
        final Map<String, String[]> files = new LinkedHashMap<String, String[]>();
    }

    private Record readRecord() {
        Record r = new Record();
        File f = new File(dir, RECORD);
        if (!f.isFile()) return r;
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.startsWith("#")) continue;
                    String[] p = line.split("\t", -1);
                    if (p.length == 2 && p[0].equals("checked")) r.checked = p[1];
                    else if (p.length == 5) r.files.put(p[0], p);
                }
            } finally {
                in.close();
            }
        } catch (IOException e) {
            Log.e("Backups.readRecord", e);
        }
        return r;
    }

    private void writeRecord(Record r) throws IOException {
        dir.mkdirs();
        File f = new File(dir, RECORD), part = new File(dir, RECORD + ".part");
        FileOutputStream fo = new FileOutputStream(part);
        try {
            Writer w = new OutputStreamWriter(fo, "UTF-8");
            w.write(Words.Db.RECORD_HEAD + "\n");
            if (!r.checked.isEmpty()) w.write("checked\t" + r.checked + "\n");
            for (String[] p : r.files.values()) w.write(p[0] + "\t" + p[1] + "\t" + p[2] + "\t" + p[3] + "\t" + p[4] + "\n");
            w.flush();
            fo.getFD().sync();
        } finally {
            fo.close();
        }
        if (!part.renameTo(f)) throw new IOException("cannot rename " + part.getPath());
    }

    private void put(File f, String sha, String content, long now) throws IOException {
        Record r = readRecord();
        r.files.put(f.getName(), new String[]{f.getName(), String.valueOf(f.length()), sha, content == null ? "" : content, String.valueOf(now)});
        writeRecord(r);
    }

    // ---------- עזרים ----------

    private static Entry newestCopy(List<Entry> all) {
        for (Entry e : all) if (!e.text && !e.kind.equals(DAMAGED) && e.content != null) return e;
        return null;
    }

    private static Entry newest(List<Entry> all, String kind) {
        for (Entry e : all) if (e.kind.equals(kind)) return e;
        return null;
    }

    static String day(long t) {
        SimpleDateFormat d = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return d.format(new Date(t));
    }

    static String stamp(long t) {
        SimpleDateFormat d = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss", Locale.US);
        return d.format(new Date(t));
    }

    /** מספר היום המקומי מאז 1970. */
    static long dayIndex(long t) {
        return floorDiv(t + TimeZone.getDefault().getOffset(t), 86400000L);
    }

    /** מספר השבוע, שבוע שמתחיל ביום ראשון (1.1.1970 היה יום חמישי). */
    static long weekIndex(long t) {
        return floorDiv(dayIndex(t) + 4, 7L);
    }

    /** חלוקה מעוגלת כלפי מטה (Math.floorDiv — רק מאנדרואיד 7). */
    private static long floorDiv(long a, long b) {
        long q = a / b;
        return (a % b != 0 && (a < 0) != (b < 0)) ? q - 1 : q;
    }

    static String hex(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (byte x : b) sb.append(Character.forDigit((x >> 4) & 15, 16)).append(Character.forDigit(x & 15, 16));
        return sb.toString();
    }

    static String sha(File f) throws IOException {
        InputStream in = new FileInputStream(f);
        try {
            MessageDigest md = digest();
            byte[] b = new byte[64 * 1024];
            int n;
            while ((n = in.read(b)) > 0) md.update(b, 0, n);
            return hex(md.digest());
        } finally {
            in.close();
        }
    }

    /** מעתיק ומחשב חתימה על מה שנכתב. */
    private static String copyWithSha(File from, File to) throws IOException {
        InputStream in = new FileInputStream(from);
        try {
            FileOutputStream out = new FileOutputStream(to);
            try {
                MessageDigest md = digest();
                byte[] b = new byte[64 * 1024];
                int n;
                while ((n = in.read(b)) > 0) {
                    out.write(b, 0, n);
                    md.update(b, 0, n);
                }
                out.getFD().sync();
                return hex(md.digest());
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }

    private static MessageDigest digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (Exception e) {
            throw new Store.Failure("no SHA-256", e);
        }
    }
}
