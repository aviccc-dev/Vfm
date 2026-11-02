package com.tikiyot.vfm;

// בדיקות הגיבוי (מ0.5): עותק, גיבוי טקסט, שחזור, מסד פגום, המונה אחרי שחזור, תיקיית הגיבויים והכללים למחיקת ישנים.
// רץ במחשב (מול SQLite של המחשב) ובמכשיר, כחלק מהבדיקה העצמית — כך גם ההתנהגות של אנדרואיד מול מסד פגום נבדקת במכשיר עצמו.
// שמות הבדיקות באנגלית — רק כשבדיקה נכשלת, לאנשי הפיתוח. נתוני בדיקה בעברית — כרצפי escape (בדיקת המילון).

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

public final class BackupContract {

    private final File dir;
    private final StoreContract.Result r = new StoreContract.Result();
    private int seq;

    private BackupContract(File dir) {
        this.dir = dir;
    }

    public static StoreContract.Result run(File dir) {
        BackupContract c = new BackupContract(dir);
        dir.mkdirs();
        try {
            c.all();
        } catch (Throwable t) {
            c.r.failed.add("backup contract crashed: " + t);
        } finally {
            SqlStore.forceFileCopy = false;
        }
        return c.r;
    }

    private void ck(String name, boolean ok) {
        if (ok) r.passed++;
        else r.failed.add(name);
    }

    private interface Act {
        void run() throws Exception;
    }

    /** מצפה לחריגה מהסוג הנתון. */
    private void fails(String name, Class<?> type, Act a) {
        try {
            a.run();
            r.failed.add(name + " (no failure)");
        } catch (Throwable t) {
            if (type.isInstance(t)) r.passed++;
            else r.failed.add(name + " (wrong exception " + t + ")");
        }
    }

    private File f(String name) {
        return new File(dir, name);
    }

    private IdCounter counter(String name) throws IOException {
        return new IdCounter(f(name + "-counter.txt"));
    }

    private File fresh(String base) {
        return f(base + "-" + (seq++) + ".db");
    }

    private static final long DAY = 86400000L;
    /** 2.11.2026, 12:00 שעון מקומי בערך — מספיק לבדיקה (רק הפרשים בין ימים חשובים). */
    private static final long NOW = 1793620800000L;

    // ---------- הבדיקות ----------

    private void all() throws Exception {
        File aFile = fresh("live");
        IdCounter aCounter = counter("live");
        SqlStore a = SqlStore.open(aFile, aCounter);
        try {
            fill(a);
            copies(a);
            text(a);
            damage(a);
            counterAfterRestore(a, aFile);
            folder(a);
        } finally {
            a.close();
        }
        retention();
        names();
        swap();
    }

    /** מסד עם כל הסוגים, ותווים שצריך לשמור עליהם: טאב, שורה חדשה, קו נטוי, "\N" כטקסט, אמוג'י. */
    private void fill(SqlStore a) {
        long l1 = a.createList("\u05e1\u05e4\u05e8\u05d9\u05dd", 0);
        long l2 = a.createList("\u05d1\u05ea\u05d5\u05da", l1);
        a.setProp(l1, Store.P_ICON, "\ud83d\udcda");
        a.setProp(l1, Store.P_DESC, "tab\there\nnew line \\ back \\N end\r");
        a.setProp(l2, Store.P_GROUP, "1");
        long sm = a.createSmart("\u05d7\u05db\u05de\u05d4", l1);
        a.setProp(sm, Store.P_COND, "\u05e1\u05d5\u05d2:pdf");
        a.addNote(l1, "\u05e8\u05e2\u05d9\u05d5\u05df\t\u05e2\u05dd\n\u05e9\u05d5\u05e8\u05d4");
        a.addTitle(l2, "\u05db\u05d5\u05ea\u05e8\u05ea");
        long gone = a.addNote(l2, "bin");
        a.trash(gone);
        long fa = a.createFile(new Store.FileInfo("/x/\u05d0.pdf", "internal", 10, 20, null));
        long fb = a.createFile(new Store.FileInfo("/x/b c.txt", "card", 30, 40, "abc"));
        a.addMember(l1, fa);
        a.addMember(l2, fb);
        a.addMember(l2, sm);
        a.setOrigin(l1, "tree:m:L:\u05e1\u05e4\u05e8\u05d9\u05dd");
        a.createList("\\N", 0);
    }

    private void copies(SqlStore a) throws Exception {
        String live = DbDump.hash(a);
        File c1 = f("copy1.db");
        a.copyTo(c1);
        ck("copy: file made", c1.isFile() && c1.length() > 0);
        ck("copy: no journal next to the copy", !new File(c1.getPath() + "-wal").exists());
        SqlStore ro = SqlStore.openReadOnly(c1);
        try {
            ck("copy: same content signature", live.equals(DbDump.hash(ro)));
            ck("copy: same system code", a.systemCode().equals(ro.systemCode()));
            ck("copy: quick check", ro.check() == null);
            fails("copy: read-only copy refuses writing", RuntimeException.class, new Act() {
                @Override public void run() { ro2(ro); }
            });
        } finally {
            ro.close();
        }
        String sha = Backups.sha(c1);
        SqlStore.openReadOnly(c1).close();
        ck("copy: opening for reading changes not one byte", sha.equals(Backups.sha(c1)));
        final File c1f = c1;
        this.liveForCopy = a;
        fails("copy: target that exists is refused", Store.Failure.class, new Act() {
            @Override public void run() { aCopy(c1f); }
        });
        // אנדרואיד ישן: העתקת הקובץ והיומן בזמן נעילה
        a.addNote(a.lists().get(0).id, "pending in the journal");
        String live2 = DbDump.hash(a);
        SqlStore.forceFileCopy = true;
        try {
            File c2 = f("copy2.db");
            a.copyTo(c2);
            SqlStore r2 = SqlStore.openReadOnly(c2);
            try {
                ck("file copy (old Android): same content signature", live2.equals(DbDump.hash(r2)));
            } finally {
                r2.close();
            }
            ck("file copy (old Android): journal folded, none left", !new File(c2.getPath() + "-wal").exists());
        } finally {
            SqlStore.forceFileCopy = false;
        }
        fails("read-only open of a missing file", Store.Failure.class, new Act() {
            @Override public void run() { SqlStore.openReadOnly(f("none.db")); }
        });
    }

    private SqlStore liveForCopy;

    private void aCopy(File to) {
        liveForCopy.copyTo(to);
    }

    private static void ro2(SqlStore ro) {
        ro.createList("x", 0);
    }

    private void text(SqlStore a) throws Exception {
        String live = DbDump.hash(a);
        File t1 = f("t1.txt");
        String h = DbDump.write(a, t1, NOW);
        ck("text: signature of the file = signature of the database", live.equals(h));
        ck("text: no partial file left", !new File(t1.getPath() + ".part").exists());
        DbDump.Data d = DbDump.read(t1);
        ck("text: read back, same signature", h.equals(d.hash));
        ck("text: all items", d.count("I") == a.itemCount());
        ck("text: format and schema", d.format == DbDump.FORMAT && d.schema == SqlStore.VERSION && d.made == NOW);
        File bFile = fresh("fromtext");
        SqlStore b = SqlStore.open(bFile, counter("fromtext"));
        try {
            DbDump.load(d, b);
            ck("text: restored into an empty database, same signature", h.equals(DbDump.hash(b)));
            ck("text: restored keeps the ids", b.lastId() == a.lastId() && b.lists().size() == a.lists().size());
            long l1 = a.lists().get(0).id;
            ck("text: tab, new line, back slash and \\N kept", a.prop(l1, Store.P_DESC).equals(b.prop(l1, Store.P_DESC)));
            final DbDump.Data dd = d;
            final SqlStore bb = b;
            fails("text: restore into a database that is not empty is refused", Store.Failure.class, new Act() {
                @Override public void run() { DbDump.load(dd, bb); }
            });
            ck("text: the refused restore changed nothing", h.equals(DbDump.hash(b)));
        } finally {
            b.close();
        }
        SqlStore b2 = SqlStore.open(bFile, counter("fromtext"));
        try {
            ck("text: restored keeps the system code (same system)", a.systemCode().equals(b2.systemCode()));
        } finally {
            b2.close();
        }
        // קובץ קטוע, שונה, או מגרסה חדשה — לא נקרא
        List<String> lines = readLines(t1);
        writeLines(f("cut.txt"), lines.subList(0, lines.size() - 3));
        fails("text: cut short is refused", Store.Failure.class, new Act() {
            @Override public void run() throws Exception { DbDump.read(f("cut.txt")); }
        });
        List<String> edited = new ArrayList<String>(lines);
        int mid = edited.size() / 2;
        edited.set(mid, edited.get(mid).replace('1', '2').replace('a', 'b') + "x");
        writeLines(f("edited.txt"), edited);
        fails("text: an edited line is refused", Store.Failure.class, new Act() {
            @Override public void run() throws Exception { DbDump.read(f("edited.txt")); }
        });
        List<String> newer = new ArrayList<String>(lines);
        newer.set(1, "format\t" + (DbDump.FORMAT + 1));
        writeLines(f("newer.txt"), newer);
        fails("text: a newer format is refused", Store.Failure.class, new Act() {
            @Override public void run() throws Exception { DbDump.read(f("newer.txt")); }
        });
        ck("text: escapes round trip", "a\tb\nc\\d\r\\N".equals(DbDump.unesc(DbDump.esc("a\tb\nc\\d\r\\N"))) && DbDump.unesc(DbDump.esc(null)) == null);
    }

    private void damage(SqlStore a) throws Exception {
        File good = f("dmg-src.db");
        a.copyTo(good);
        // כותרת הרוסה: "הקובץ אינו מסד"
        final File d1 = f("dmg-head.db");
        SqlStore.copyFile(good, d1);
        scribble(d1, 0, 32);
        String s1 = Backups.sha(d1);
        fails("damaged header: open fails as Damaged", SqlStore.Damaged.class, new Act() {
            @Override public void run() throws Exception { SqlStore.open(d1, counter("dmg1")).close(); }
        });
        ck("damaged header: the file is kept, not one byte changed", d1.isFile() && s1.equals(Backups.sha(d1)));
        // עמוד פנימי הרוס
        final File d2 = f("dmg-page.db");
        SqlStore.copyFile(good, d2);
        int page = pageSize(d2);
        scribble(d2, page, page);
        String s2 = Backups.sha(d2);
        fails("damaged page: open fails as Damaged", SqlStore.Damaged.class, new Act() {
            @Override public void run() throws Exception { SqlStore.open(d2, counter("dmg2")).close(); }
        });
        ck("damaged page: the file is kept, not one byte changed", d2.isFile() && s2.equals(Backups.sha(d2)));
        fails("damaged page: read-only open fails as Damaged", SqlStore.Damaged.class, new Act() {
            @Override public void run() { SqlStore.openReadOnly(d2).close(); }
        });
        final File target = f("dmg-built.db");
        fails("restore from a damaged copy fails", RuntimeException.class, new Act() {
            @Override public void run() throws Exception { Backups.build(d2, target, counter("dmg3")); }
        });
        ck("restore from a damaged copy leaves nothing", !target.exists());
    }

    private void counterAfterRestore(SqlStore a, File aFile) throws Exception {
        File old = f("old.db");
        a.copyTo(old);
        File oldText = f("old.txt");
        DbDump.write(a, oldText, NOW);
        long before = a.lastId();
        for (int i = 0; i < 5; i++) a.createList("later " + i, 0);
        long after = a.lastId();
        IdCounter c = new IdCounter(new File(aFile.getPath().replace(".db", "") + "-shared-counter.txt"));
        // המונה המשותף ראה את כל המספרים (כמו קובץ המונה האמיתי, שלא מתחלף בשחזור)
        c.reserve(after);
        File r1 = fresh("restored");
        Backups.Built b = Backups.build(old, r1, c);
        ck("restore from copy: what the copy had", b.items > 0 && b.lists > 0);
        SqlStore s = SqlStore.open(r1, c);
        try {
            long id = s.createList("new", 0);
            ck("restore from an old copy: no id is issued twice", id > after && before < after);
        } finally {
            s.close();
        }
        File r2 = fresh("restored-text");
        Backups.build(oldText, r2, c);
        SqlStore s2 = SqlStore.open(r2, c);
        try {
            ck("restore from an old text backup: no id is issued twice", s2.createList("new", 0) > after);
        } finally {
            s2.close();
        }
    }

    private void folder(SqlStore a) throws Exception {
        File bdir = f("backups"), tmp = f("tmp");
        Backups bk = new Backups(bdir, tmp);
        Backups.Daily d = bk.daily(a, NOW);
        List<Backups.Entry> all = bk.list();
        ck("daily: first time — copy and text", d.copied && d.texted && all.size() == 2);
        ck("daily: the temporary copy is gone", tmp.listFiles() == null || tmp.listFiles().length == 0);
        ck("daily: same day — not again", bk.daily(a, NOW + 3600000L).checkedAlready && bk.list().size() == 2);
        Backups.Daily d2 = bk.daily(a, NOW + DAY);
        ck("daily: next day, no change — no new copy", !d2.copied && !d2.texted && !d2.checkedAlready && bk.list().size() == 2);
        a.addNote(a.lists().get(0).id, "change");
        Backups.Daily d3 = bk.daily(a, NOW + 2 * DAY);
        ck("daily: a change — new copy; text waits for a week", d3.copied && !d3.texted && bk.list().size() == 3);
        ck("before: nothing changed since the last copy — no copy", bk.before(a, Backups.OP_DELETE, NOW + 2 * DAY + 60000L) == null);
        a.addNote(a.lists().get(0).id, "change 2");
        Backups.Entry be = bk.before(a, Backups.OP_DELETE, NOW + 2 * DAY + 120000L);
        ck("before: a change — copy named after the action", be != null && be.kind.equals(Backups.BEFORE) && Backups.OP_DELETE.equals(be.op));
        Backups.Daily d4 = bk.daily(a, NOW + 9 * DAY);
        ck("daily: after a week with a change — text again", d4.texted);
        List<Backups.Entry> m = bk.manual(a, NOW + 9 * DAY + 1000L);
        ck("manual: copy and text", m.size() == 2 && !m.get(0).text && m.get(1).text && m.get(0).kind.equals(Backups.MANUAL));
        all = bk.list();
        int[] v = bk.verifyAll(all);
        ck("verify: all recorded and whole", v[0] == all.size() && v[1] == 0 && v[2] == 0);
        // עותק שהשתנה אחרי שנרשם
        Backups.Entry victim = null;
        for (Backups.Entry e : all) if (e.kind.equals(Backups.DAILY)) victim = e;
        scribble(victim.file, (int) (victim.file.length() / 2), 8);
        ck("verify: a byte changed after recording — found", bk.verify(victim) == Backups.CHANGED);
        File stray = new File(bdir, Backups.DAILY + " 2020-01-01.db");
        writeLines(stray, java.util.Arrays.asList("not a database"));
        File mine = new File(bdir, "mine.db");
        writeLines(mine, java.util.Arrays.asList("x"));
        Backups.Entry se = Backups.parse(stray);
        ck("verify: unrecorded file that is not a database — bad", bk.verify(se) == Backups.BAD);
        ck("folder: a file that is not ours is not listed", !names(bk.list()).contains("mine.db"));
        Backups.Entry de = bk.keepDamaged(victim.file, NOW + 10 * DAY);
        ck("damaged: kept as is, not counted as a problem", de != null && de.kind.equals(Backups.DAMAGED) && bk.verify(de) == Backups.KEPT_DAMAGED);
        // הרבה ימים: המחיקה לפי הכללים, וקובץ שאינו שלנו נשאר
        for (int i = 11; i < 60; i++) {
            a.addNote(a.lists().get(0).id, "day " + i);
            bk.daily(a, NOW + i * DAY);
        }
        int daily = 0, text = 0;
        for (Backups.Entry e : bk.list()) {
            if (e.kind.equals(Backups.DAILY)) daily++;
            if (e.kind.equals(Backups.TEXT)) text++;
        }
        ck("prune: 7 daily + one for each of 4 weeks", daily == Backups.KEEP_DAILY + Backups.KEEP_WEEKS);
        ck("prune: 5 text backups", text == Backups.KEEP_TEXT);
        ck("prune: manual and damaged are kept", count(bk.list(), Backups.MANUAL) == 2 && count(bk.list(), Backups.DAMAGED) == 1);
        ck("prune: a file that is not ours is kept", mine.isFile());
        int[] v2 = bk.verifyAll(bk.list());
        ck("prune: the record follows (no bad, only the changed one)", v2[2] == 0 && v2[1] <= 1);
    }

    private void retention() {
        List<Backups.Entry> all = new ArrayList<Backups.Entry>();
        for (int i = 0; i < 60; i++) all.add(entry(Backups.DAILY, null, NOW - i * DAY, false));
        for (int i = 0; i < 15; i++) all.add(entry(Backups.BEFORE, Backups.OP_DELETE, NOW - i * 3600000L, false));
        for (int i = 0; i < 8; i++) all.add(entry(Backups.TEXT, null, NOW - i * 7 * DAY, true));
        for (int i = 0; i < 20; i++) all.add(entry(Backups.MANUAL, null, NOW - i * DAY, false));
        List<Backups.Entry> del = Backups.toDelete(all);
        ck("retention: daily — 60 made, 11 kept", count(del, Backups.DAILY) == 49);
        ck("retention: before — 15 made, 10 kept", count(del, Backups.BEFORE) == 5);
        ck("retention: text — 8 made, 5 kept", count(del, Backups.TEXT) == 3);
        ck("retention: manual — never", count(del, Backups.MANUAL) == 0);
        boolean newestKept = true;
        for (Backups.Entry e : del) if (e.when > NOW - 7 * DAY && e.kind.equals(Backups.DAILY)) newestKept = false;
        ck("retention: the newest 7 days are kept", newestKept);
        List<Backups.Entry> few = new ArrayList<Backups.Entry>();
        for (int i = 0; i < 3; i++) few.add(entry(Backups.DAILY, null, NOW - i * DAY, false));
        ck("retention: few — nothing deleted", Backups.toDelete(few).isEmpty());
    }

    private void names() {
        Backups.Entry d = Backups.parse(new File(Backups.DAILY + " 2026-11-02.db"));
        ck("name: daily", d != null && d.kind.equals(Backups.DAILY) && !d.text && Backups.day(d.when).equals("2026-11-02"));
        Backups.Entry b = Backups.parse(new File(Backups.BEFORE + " " + Backups.OP_IMPORT + " 2026-11-02 14-03-05.db"));
        ck("name: before, with the action and time", b != null && b.kind.equals(Backups.BEFORE) && Backups.OP_IMPORT.equals(b.op)
                && Backups.stamp(b.when).equals("2026-11-02 14-03-05"));
        ck("name: manual text", Backups.parse(new File(Backups.MANUAL + " 2026-11-02 14-03-05.txt")) != null);
        ck("name: second of the same second", Backups.parse(new File(Backups.MANUAL + " 2026-11-02 14-03-05 (2).db")) != null);
        ck("name: daily .txt is not ours", Backups.parse(new File(Backups.DAILY + " 2026-11-02.txt")) == null);
        ck("name: text .db is not ours", Backups.parse(new File(Backups.TEXT + " 2026-11-02.db")) == null);
        ck("name: no date — not ours", Backups.parse(new File(Backups.DAILY + ".db")) == null);
        ck("name: impossible date — not ours", Backups.parse(new File(Backups.DAILY + " 2026-13-40.db")) == null);
        ck("name: the record is not a backup", Backups.parse(new File(Backups.RECORD)) == null);
    }

    private void swap() throws Exception {
        File db = fresh("swap");
        IdCounter c = counter("swap");
        SqlStore s = SqlStore.open(db, c);
        s.createList("old", 0);
        s.close();
        File src = fresh("swap-src");
        SqlStore n = SqlStore.open(src, counter("swap-src"));
        n.createList("one", 0);
        n.createList("two", 0);
        File t = f("swap-src.txt");
        DbDump.write(n, t, NOW);
        n.close();
        File built = new File(db.getPath() + ".restore");
        Backups.Built b = Backups.build(t, built, c);
        ck("build: from text — two lists", b.lists == 2);
        Backups.swap(built, db);
        ck("swap: nothing left of the built file", !built.exists() && !new File(built.getPath() + "-wal").exists());
        SqlStore after = SqlStore.open(db, c);
        try {
            ck("swap: the database is the restored one", after.lists().size() == 2);
        } finally {
            after.close();
        }
        final File none = f("nothing.restore");
        fails("swap: nothing to swap in — refused, database untouched", IOException.class, new Act() {
            @Override public void run() throws Exception { Backups.swap(none, f("never.db")); }
        });
    }

    // ---------- עזרים ----------

    private static Backups.Entry entry(String kind, String op, long when, boolean text) {
        String name = kind + (op == null ? "" : " " + op) + " " + (kind.equals(Backups.DAILY) || kind.equals(Backups.TEXT) ? Backups.day(when) : Backups.stamp(when))
                + (text ? ".txt" : ".db");
        return Backups.parse(new File(name));
    }

    private static int count(List<Backups.Entry> l, String kind) {
        int n = 0;
        for (Backups.Entry e : l) if (e.kind.equals(kind)) n++;
        return n;
    }

    private static List<String> names(List<Backups.Entry> l) {
        List<String> out = new ArrayList<String>();
        for (Backups.Entry e : l) out.add(e.name);
        return out;
    }

    /** כותב בתים אקראיים לתוך קובץ, במקום הנתון. */
    private static void scribble(File file, int at, int n) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        try {
            byte[] b = new byte[n];
            java.util.Random rnd = new java.util.Random(at + n);
            rnd.nextBytes(b);
            raf.seek(at);
            raf.write(b);
        } finally {
            raf.close();
        }
    }

    /** גודל העמוד מכותרת המסד (בתים 16–17). */
    private static int pageSize(File file) throws IOException {
        FileInputStream in = new FileInputStream(file);
        try {
            byte[] h = new byte[18];
            if (in.read(h) < 18) return 4096;
            int p = ((h[16] & 255) << 8) | (h[17] & 255);
            return p == 1 ? 65536 : p;
        } finally {
            in.close();
        }
    }

    private static List<String> readLines(File file) throws IOException {
        List<String> out = new ArrayList<String>();
        java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(new FileInputStream(file), "UTF-8"));
        try {
            String l;
            while ((l = in.readLine()) != null) out.add(l);
        } finally {
            in.close();
        }
        return out;
    }

    private static void writeLines(File file, List<String> lines) throws IOException {
        FileOutputStream out = new FileOutputStream(file);
        try {
            for (String l : lines) out.write((l + "\n").getBytes("UTF-8"));
        } finally {
            out.close();
        }
    }
}
