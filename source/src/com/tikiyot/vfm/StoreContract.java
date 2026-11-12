package com.tikiyot.vfm;

// חוזה האחסון (מ0.1): אותן בדיקות על כל מימוש של Store. במחשב — על MemStore ועל SqlStore מול SQLite של המחשב;
// במכשיר — על SqlStore מול SQLite של אנדרואיד ("בדיקה עצמית"). אחראי: להוכיח ששני המימושים מתנהגים אותו דבר.
// שמות הבדיקות באנגלית — הם מופיעים רק כשבדיקה נכשלת, לאנשי הפיתוח. נתוני הבדיקה בעברית כתובים כרצפי escape — כדי שבדיקת המילון
// (כל הודעה למשתמש ב־Words) לא תתבלבל מנתוני בדיקה.

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public final class StoreContract {

    /** יוצר Store ריק חדש שמשתמש בקובץ המונה הנתון. */
    public interface Opener {
        Store fresh(File counterFile) throws Exception;
    }

    public static final class Result {
        public int passed;
        public final List<String> failed = new ArrayList<String>();
    }

    private interface Act {
        void run() throws Exception;
    }

    private final Opener opener;
    private final File dir;
    private final Result r = new Result();
    private int counterSeq;

    private StoreContract(Opener opener, File dir) {
        this.opener = opener;
        this.dir = dir;
    }

    public static Result run(Opener opener, File dir) {
        StoreContract c = new StoreContract(opener, dir);
        c.all();
        return c.r;
    }

    // ---------- עזרים ----------

    private void ck(String name, boolean ok) {
        if (ok) r.passed++;
        else r.failed.add(name);
    }

    private void fails(String name, Act a) {
        try {
            a.run();
            r.failed.add(name + " (no failure)");
        } catch (Store.Failure e) {
            r.passed++;
        } catch (Throwable t) {
            r.failed.add(name + " (wrong exception " + t + ")");
        }
    }

    private File counterFile() {
        return new File(dir, "counter-" + (counterSeq++) + ".txt");
    }

    private static List<Long> ids(List<Store.Member> ms) {
        List<Long> out = new ArrayList<Long>();
        for (Store.Member m : ms) out.add(m.item.id);
        return out;
    }

    private boolean dense(Store s, long list) {
        List<Store.Member> ms = s.members(list);
        for (int i = 0; i < ms.size(); i++) if (ms.get(i).position != i) return false;
        return true;
    }

    private void all() {
        try {
            ids();
            membership();
            moving();
            lists();
            unicode();
            restore();
            files();
            adding();
            smarts();
            nesting();
            bin();
            atomicity();
            randomOps();
        } catch (Throwable t) {
            r.failed.add("contract crashed: " + t);
        }
    }

    // ---------- הבדיקות ----------

    private void ids() throws Exception {
        final Store s = opener.fresh(counterFile());
        try {
            ck("fresh lastId is 0", s.lastId() == 0);
            long a = s.createList("  \u05d0  ", 0);
            long b = s.createList("\u05d1", 0);
            ck("first id is 1, then 2", a == 1 && b == 2);
            ck("name is trimmed", s.item(a).name.equals("\u05d0") && s.item(a).type == Store.LIST);
            ck("lastId follows", s.lastId() == 2);
            fails("empty list name", new Act() { public void run() { s.createList("   ", 0); } });
            fails("list under missing parent", new Act() { public void run() { s.createList("\u05d2", 99); } });
            long c = s.createList("\u05d2", 0);
            ck("failed creates use no id", c == 3);
            s.deleteList(c);
            long d = s.createList("\u05d3", 0);
            ck("deleted id is never reused", d == 4 && s.item(c) == null);
            ck("system code is 8 hex chars", s.systemCode().matches("[0-9a-f]{8}") && s.systemCode().equals(s.systemCode()));
            ck("check is clean", s.check() == null);
        } finally {
            s.close();
        }
    }

    private void membership() throws Exception {
        final Store s = opener.fresh(counterFile());
        try {
            final long l1 = s.createList("l1", 0), l2 = s.createList("l2", 0);
            long n1 = s.addNote(l1, "n1"), n2 = s.addNote(l1, " n2 "), n3 = s.addNote(l1, "n3");
            ck("notes join at the end in order", ids(s.members(l1)).equals(java.util.Arrays.asList(n1, n2, n3)));
            ck("positions are dense", dense(s, l1));
            ck("note text is trimmed and typed", s.item(n2).name.equals("n2") && s.item(n2).type == Store.NOTE);
            ck("count", s.count(l1) == 3 && s.count(l2) == 0);
            ck("add existing to another list", s.addMember(l2, n1));
            ck("add twice is refused", !s.addMember(l2, n1) && s.count(l2) == 1);
            ck("item sits in two lists", s.listsOf(n1).equals(java.util.Arrays.asList(l1, l2)));
            final long n1f = n1;
            ck("a list can join another list (m0.4)", s.addMember(l1, l2) && s.removeMember(l1, l2));
            fails("note as a list", new Act() { public void run() { s.addNote(n1f, "x"); } });
            fails("add missing item", new Act() { public void run() { s.addMember(l1, 999); } });
            fails("add to missing list", new Act() { public void run() { s.addMember(999, n1f); } });
            fails("empty note", new Act() { public void run() { s.addNote(l1, ""); } });
            ck("remove middle", s.removeMember(l1, n2));
            ck("gap is closed", ids(s.members(l1)).equals(java.util.Arrays.asList(n1, n3)) && dense(s, l1));
            ck("removed item still exists", s.item(n2) != null && s.listsOf(n2).isEmpty());
            ck("second remove is false", !s.removeMember(l1, n2));
            ck("other list untouched", s.count(l2) == 1);
            ck("added time is set", s.members(l1).get(0).added > 0);
            s.removeMember(l1, n1);
            s.removeMember(l1, n3);
            ck("empty list", s.members(l1).isEmpty() && s.count(l1) == 0);
            // רשימה גדולה
            long big = s.createList("big", 0);
            for (int i = 0; i < 300; i++) s.addNote(big, "x" + i);
            ck("300 members, dense", s.count(big) == 300 && dense(s, big) && s.members(big).get(299).item.name.equals("x299"));
        } finally {
            s.close();
        }
    }

    private void moving() throws Exception {
        final Store s = opener.fresh(counterFile());
        try {
            final long l = s.createList("l", 0);
            final long a = s.addNote(l, "a"), b = s.addNote(l, "b"), c = s.addNote(l, "c"), d = s.addNote(l, "d");
            s.move(l, a, 2);
            ck("move forward", ids(s.members(l)).equals(java.util.Arrays.asList(b, c, a, d)) && dense(s, l));
            s.move(l, d, 0);
            ck("move to front", ids(s.members(l)).equals(java.util.Arrays.asList(d, b, c, a)) && dense(s, l));
            s.move(l, d, 99);
            ck("move past the end goes to the end", ids(s.members(l)).equals(java.util.Arrays.asList(b, c, a, d)));
            s.move(l, d, -5);
            ck("move before the start goes to the start", ids(s.members(l)).equals(java.util.Arrays.asList(d, b, c, a)));
            s.move(l, b, 1);
            ck("move in place", ids(s.members(l)).equals(java.util.Arrays.asList(d, b, c, a)) && dense(s, l));
            final long other = s.createList("o", 0);
            fails("move a non-member", new Act() { public void run() { s.move(other, a, 0); } });
        } finally {
            s.close();
        }
    }

    private void lists() throws Exception {
        final Store s = opener.fresh(counterFile());
        try {
            long top = s.createList("top", 0);
            long sub = s.createList("sub", top);
            long n1 = s.addNote(top, "n1");
            long subNote = s.addNote(sub, "inner");
            ck("sub-list is a member of its parent", ids(s.members(top)).equals(java.util.Arrays.asList(sub, n1)));
            ck("lists() has both, by id", s.lists().size() == 2 && s.lists().get(0).id == top && s.lists().get(1).id == sub);
            final long subF = sub;
            s.rename(sub, "  \u05d7\u05d3\u05e9  ");
            ck("rename trims and stamps", s.item(sub).name.equals("\u05d7\u05d3\u05e9") && s.item(sub).changed >= s.item(sub).created);
            fails("rename to empty", new Act() { public void run() { s.rename(subF, " "); } });
            fails("rename missing", new Act() { public void run() { s.rename(777, "x"); } });
            long other = s.createList("other", 0);
            s.addMember(other, n1);
            s.deleteList(sub);
            ck("deleted sub-list leaves its parent, gap closed",
                    ids(s.members(top)).equals(java.util.Arrays.asList(n1)) && dense(s, top));
            ck("deleted list's item remains, without a list", s.item(subNote) != null && s.listsOf(subNote).isEmpty());
            ck("deleted list is gone", s.item(sub) == null && s.lists().size() == 2);
            fails("members of a deleted list", new Act() { public void run() { s.members(subF); } });
            fails("delete a note as a list", new Act() { public void run() { s.deleteList(n1Of(s)); } });
            ck("shared note survives list delete", s.listsOf(n1).equals(java.util.Arrays.asList(top, other)));
            s.deleteList(top);
            ck("shared note stays in the other list", s.listsOf(n1).equals(java.util.Arrays.asList(other)) && dense(s, other));
            ck("check after deletes", s.check() == null);
        } finally {
            s.close();
        }
    }

    /** מספר הפתק היחיד שנשאר ברשימה "other" — לבדיקת מחיקה של פריט שאינו רשימה. */
    private static long n1Of(Store s) {
        for (Store.Item it : s.lists()) if (it.name.equals("other")) return s.members(it.id).get(0).item.id;
        return -1;
    }

    private void unicode() throws Exception {
        Store s = opener.fresh(counterFile());
        try {
            long l = s.createList("\u05e8\u05e9\u05d9\u05de\u05d4 \"\u05d1\u05d3\u05d9\u05e7\u05d4\" '\u05d2' ; -- DROP TABLE items", 0);
            String[] texts = {"\u05e9\u05d5\u05e8\u05d4 \u05e8\u05d0\u05e9\u05d5\u05e0\u05d4\n\u05e9\u05d5\u05e8\u05d4 \u05e9\u05e0\u05d9\u05d9\u05d4", "O'Brien \"x\" %s ? \\ ", "\ud83d\ude00 \u05d0\u05d9\u05de\u05d5\u05d2'\u05d9", "\u062a\u062c\u0631\u0628\u0629", "\u05ea\u05d5\t\u05d8\u05d0\u05d1"};
            List<Long> ids = new ArrayList<Long>();
            for (String t : texts) ids.add(s.addNote(l, t));
            boolean same = true;
            for (int i = 0; i < texts.length; i++) same &= s.item(ids.get(i)).name.equals(texts[i].trim());
            ck("text round-trips exactly", same);
            ck("tricky list name round-trips", s.item(l).name.equals("\u05e8\u05e9\u05d9\u05de\u05d4 \"\u05d1\u05d3\u05d9\u05e7\u05d4\" '\u05d2' ; -- DROP TABLE items"));
            ck("tables survive", s.check() == null && s.lists().size() == 1);
        } finally {
            s.close();
        }
    }

    // ---------- מ0.2: קבצים, מקור, מאפיינים, עסקה אחת ----------

    private static Store.FileInfo info(String path, long size, long mtime, String sig) {
        return new Store.FileInfo(path, "\u05e4\u05e0\u05d9\u05de\u05d9", size, mtime, sig);
    }

    private void files() throws Exception {
        final Store s = opener.fresh(counterFile());
        try {
            final long l1 = s.createList("l1", 0), l2 = s.createList("l2", 0);
            String hp = "/storage/emulated/0/\u05de\u05e1\u05de\u05db\u05d9\u05dd/\u05e7\u05d5\u05d1\u05e5 1.pdf";
            long f1 = s.createFile(info(hp, 1234, 1700000000000L, "2:00112233445566ff"));
            ck("file item is typed and named by its file name", s.item(f1).type == Store.FILE && s.item(f1).name.equals("\u05e7\u05d5\u05d1\u05e5 1.pdf"));
            Store.FileInfo fi = s.file(f1);
            ck("file info round trip", fi != null && fi.path.equals(hp) && fi.drive.equals("\u05e4\u05e0\u05d9\u05de\u05d9") && fi.size == 1234
                    && fi.mtime == 1700000000000L && "2:00112233445566ff".equals(fi.sig));
            ck("findFile by path", s.findFile(hp) == f1 && s.findFile("/nope") == 0);
            final String hpf = hp;
            fails("same path twice", new Act() { public void run() { s.createFile(info(hpf, 1, 1, null)); } });
            fails("empty path", new Act() { public void run() { s.createFile(info("  ", 1, 1, null)); } });
            fails("empty drive", new Act() { public void run() { s.createFile(new Store.FileInfo("/a", "", 1, 1, null)); } });
            long f2 = s.createFile(info("/storage/ABCD-1234/a.txt", -1, 0, null));
            ck("unknown size and no signature are kept", s.file(f2).size == -1 && s.file(f2).sig == null && s.file(f2).mtime == 0);
            ck("a file joins a list", s.addMember(l1, f1) && s.addMember(l1, f2) && s.addMember(l2, f1));
            ck("a file sits in two lists", s.listsOf(f1).equals(java.util.Arrays.asList(l1, l2)) && dense(s, l1));
            ck("members show the file type", s.members(l1).get(0).item.type == Store.FILE);
            ck("file() of a list is null", s.file(l1) == null && s.file(999) == null);
            final long f1f = f1, l1f = l1;
            s.updateFile(f2, 55, 66, "2:aa");
            ck("updateFile", s.file(f2).size == 55 && s.file(f2).mtime == 66 && "2:aa".equals(s.file(f2).sig));
            s.updateFile(f2, 55, 66, null);
            ck("updateFile can clear the signature", s.file(f2).sig == null);
            fails("updateFile of a list", new Act() { public void run() { s.updateFile(l1f, 1, 1, null); } });
            fails("updateFile of a missing item", new Act() { public void run() { s.updateFile(999, 1, 1, null); } });
            // מ0.23: הקובץ זז בכונן — אותו פריט, מסלול ושם חדשים, החברות נשארת
            String moved = "/storage/ABCD-1234/חדש/b.txt";  // בכונן
            s.moveFile(f2, moved, "ABCD-1234");
            ck("moveFile: new path, drive and name, same item",
                    s.findFile(moved) == f2 && s.findFile("/storage/ABCD-1234/a.txt") == 0 && s.file(f2).path.equals(moved)
                            && s.file(f2).drive.equals("ABCD-1234") && s.item(f2).name.equals("b.txt") && s.file(f2).size == 55);
            ck("moveFile keeps the lists", s.listsOf(f2).equals(java.util.Collections.singletonList(l1)));
            s.moveFile(f2, moved, "ABCD-1234");
            ck("moveFile to its own path — no change", s.findFile(moved) == f2);
            final long f2f = f2;
            final String hpm = hp;
            fails("moveFile onto another file's path", new Act() { public void run() { s.moveFile(f2f, hpm, "x"); } });
            ck("a refused moveFile changes nothing", s.findFile(moved) == f2 && s.findFile(hp) == f1);
            fails("moveFile of a list", new Act() { public void run() { s.moveFile(l1f, "/storage/emulated/0/z", "x"); } });
            fails("moveFile to an empty path", new Act() { public void run() { s.moveFile(f2f, " ", "x"); } });
            // מקור
            ck("no origin yet", s.findByOrigin("tree:L:x") == 0);
            s.setOrigin(l1, "tree:L:x");
            ck("origin found", s.findByOrigin("tree:L:x") == l1 && s.findByOrigin("tree:L:y") == 0);
            fails("origin of a missing item", new Act() { public void run() { s.setOrigin(999, "k"); } });
            fails("empty origin", new Act() { public void run() { s.setOrigin(l1f, " "); } });
            // מאפיינים
            ck("no prop yet", s.prop(l1, Store.P_ICON) == null);
            s.setProp(l1, Store.P_ICON, "\ud83d\udcd8");
            s.setProp(l1, Store.P_DESC, "a\nb \u05d0");
            ck("props round trip (emoji, newline, Hebrew)", "\ud83d\udcd8".equals(s.prop(l1, Store.P_ICON)) && "a\nb \u05d0".equals(s.prop(l1, Store.P_DESC)));
            s.setProp(l1, Store.P_ICON, "x");
            ck("prop is overwritten", "x".equals(s.prop(l1, Store.P_ICON)));
            s.setProp(l1, Store.P_ICON, "");
            ck("empty value removes the prop", s.prop(l1, Store.P_ICON) == null && s.prop(l1, Store.P_DESC) != null);
            s.setProp(l1, Store.P_DESC, null);
            ck("null value removes the prop", s.prop(l1, Store.P_DESC) == null);
            ck("props are per item", s.prop(l2, Store.P_ICON) == null);
            fails("prop of a missing item", new Act() { public void run() { s.setProp(999, "k", "v"); } });
            // מחיקת רשימה: המאפיינים והמקור הולכים איתה, הקבצים נשארים
            s.setProp(l1, Store.P_WHY, "w");
            s.deleteList(l1);
            ck("deleting a list drops its origin", s.findByOrigin("tree:L:x") == 0);
            ck("files stay after their list is deleted", s.file(f1f) != null && s.listsOf(f1f).equals(java.util.Arrays.asList(l2)));
            ck("check is clean after files", s.check() == null);
        } finally {
            s.close();
        }
    }

    private static List<Long> idsOf(List<Store.Item> items) {
        List<Long> out = new ArrayList<Long>();
        for (Store.Item it : items) out.add(it.id);
        return out;
    }

    /** מ0.8: הוספת קבצים לרשימה (DbFiles) — פריט אחד לכל מסלול, סדר, כפולים, רשימה חכמה, מסלול פסול, עסקה אחת. */
    private void adding() throws Exception {
        final Store s = opener.fresh(counterFile());
        try {
            final long l = s.createList("l", 0), other = s.createList("o", 0);
            final long sm = s.createSmart("sm", 0);
            long loose = s.createFile(info("/storage/emulated/0/x/old.pdf", 5, 0, null));
            DbFiles.Added a = DbFiles.add(s, l, java.util.Arrays.asList("/storage/emulated/0/x/a.pdf", "/storage/ABCD-1234/b.txt",
                    "/storage/emulated/0/x/a.pdf", "relative.txt", "/storage/emulated/0/x/old.pdf"), java.util.Arrays.asList(10L, 20L, 10L, 1L, 5L));
            ck("add files: new, duplicate in input once, bad path skipped, loose file reused", a.added == 3 && a.already == 0 && a.created == 2 && a.bad == 1);
            java.util.List<Store.Member> ms = s.members(l);
            ck("add files: order kept, the loose item is the same item", ms.size() == 3 && ms.get(0).item.name.equals("a.pdf")
                    && ms.get(1).item.name.equals("b.txt") && ms.get(2).item.id == loose && s.unassigned().isEmpty());
            Store.FileInfo b = s.file(ms.get(1).item.id);
            ck("add files: drive by path, size given, no signature", b.drive.equals("ABCD-1234") && b.size == 20 && b.sig == null && b.mtime == 0);
            DbFiles.Added again = DbFiles.add(s, l, java.util.Arrays.asList("/storage/ABCD-1234/b.txt", "/storage/emulated/0/x/c.pdf"), null);
            ck("add files again: one already there, one new at the end (size unknown)", again.added == 1 && again.already == 1 && again.created == 1
                    && s.count(l) == 4 && s.members(l).get(3).item.name.equals("c.pdf") && s.file(s.members(l).get(3).item.id).size == -1);
            DbFiles.add(s, other, java.util.Arrays.asList("/storage/emulated/0/x/c.pdf"), null);
            ck("add files: the same file in a second list is one item", s.listsOf(s.findFile("/storage/emulated/0/x/c.pdf")).equals(java.util.Arrays.asList(l, other)));
            final long last = s.lastId();
            fails("add files to a smart list", new Act() { public void run() { DbFiles.add(s, sm, java.util.Arrays.asList("/storage/emulated/0/y.pdf"), null); } });
            ck("add files to a smart list changed nothing", s.lastId() == last && s.findFile("/storage/emulated/0/y.pdf") == 0);
            DbFiles.Added none = DbFiles.add(s, l, java.util.Arrays.asList("", "no-slash"), null);
            ck("add files: only bad paths — nothing done", none.added == 0 && none.created == 0 && none.bad == 2 && s.count(l) == 4);
            ck("add files: store still valid", s.check() == null);
        } finally {
            s.close();
        }
    }

    private void smarts() throws Exception {
        final Store s = opener.fresh(counterFile());
        try {
            final long l = s.createList("l", 0);
            final long s1 = s.createSmart("recent", 0);
            final long s2 = s.createSmart("inside", l);
            ck("smart items are typed", s.item(s1).type == Store.SMART && s.item(s2).type == Store.SMART);
            ck("smarts() lists the smarts only", idsOf(s.smarts()).equals(java.util.Arrays.asList(s1, s2)));
            ck("lists() leaves the smarts out", idsOf(s.lists()).equals(java.util.Arrays.asList(l)));
            ck("a smart created inside a list is a member", s.count(l) == 1 && s.members(l).get(0).item.id == s2
                    && s.members(l).get(0).item.type == Store.SMART);
            ck("an existing smart joins a list", s.addMember(l, s1) && !s.addMember(l, s1) && s.count(l) == 2 && dense(s, l));
            ck("a smart sits in two lists", s.addMember(s.createList("l2", 0), s1) && s.listsOf(s1).size() == 2);
            s.setProp(s1, Store.P_COND, "type:x \u05e9\u05d1\u05d5\u05e2");
            s.setProp(s1, Store.P_SORT, "date");
            ck("a smart keeps its condition and sort", "type:x \u05e9\u05d1\u05d5\u05e2".equals(s.prop(s1, Store.P_COND)) && "date".equals(s.prop(s1, Store.P_SORT)));
            s.setOrigin(s1, "ini:m:S:s-1");
            ck("a smart is found by origin", s.findByOrigin("ini:m:S:s-1") == s1);
            fails("a smart has no members", new Act() { public void run() { s.members(s1); } });
            fails("a smart has no count", new Act() { public void run() { s.count(s1); } });
            fails("a note cannot go in a smart", new Act() { public void run() { s.addNote(s1, "x"); } });
            fails("a list cannot be created in a smart", new Act() { public void run() { s.createList("x", s1); } });
            fails("a smart cannot be created in a smart", new Act() { public void run() { s.createSmart("x", s1); } });
            fails("empty smart name", new Act() { public void run() { s.createSmart("  ", 0); } });
            fails("a file cannot be deleted as a list", new Act() {
                public void run() { s.deleteList(s.createFile(info("/storage/emulated/0/f.txt", 1, 1, null))); }
            });
            // unassigned: files and notes that are in no list
            long f2 = s.createFile(info("/storage/emulated/0/g.txt", 2, 2, null));
            long l3 = s.createList("l3", 0);
            long n3 = s.addNote(l3, "orphan soon");
            long nIn = s.addNote(l, "kept");
            long f3 = s.findFile("/storage/emulated/0/f.txt");
            ck("a file or note in no list is unassigned", idsOf(s.unassigned()).equals(java.util.Arrays.asList(f3, f2)));
            s.deleteList(l3);
            ck("a note stays, unassigned, when its list is deleted", idsOf(s.unassigned()).equals(java.util.Arrays.asList(f3, f2, n3)));
            ck("lists and smarts are never unassigned", !idsOf(s.unassigned()).contains(l) && !idsOf(s.unassigned()).contains(s1));
            s.addMember(l, f2);
            ck("joining a list takes an item out", idsOf(s.unassigned()).equals(java.util.Arrays.asList(f3, n3)));
            s.removeMember(l, f2);
            ck("leaving the last list brings it back", idsOf(s.unassigned()).equals(java.util.Arrays.asList(f3, f2, n3)));
            ck("a note in a list is not unassigned", !idsOf(s.unassigned()).contains(nIn));
            s.deleteList(s2);
            ck("deleting a smart removes it from its lists", s.smarts().size() == 1 && dense(s, l) && s.item(s2) == null);
            s.deleteList(s1);
            ck("deleting a smart drops its props and origin", s.item(s1) == null && s.findByOrigin("ini:m:S:s-1") == 0 && s.smarts().isEmpty());
            ck("check is clean after smarts", s.check() == null);
        } finally {
            s.close();
        }
    }

    /** m0.4: a list inside lists (no parent), cycles by id are refused, titles, groups. */
    private void nesting() throws Exception {
        final Store s = opener.fresh(counterFile());
        try {
            final long a = s.createList("a", 0), b = s.createList("b", a), c = s.createList("c", b);
            final long d = s.createList("d", 0);
            ck("a sub list is a member of its parent", s.listsOf(b).equals(java.util.Arrays.asList(a)) && s.contains(a, c) && !s.contains(c, a));
            ck("the same list can sit in two lists", s.addMember(d, c) && s.listsOf(c).equals(java.util.Arrays.asList(b, d)));
            ck("contains goes through every level", s.contains(a, c) && s.contains(d, c) && !s.contains(a, d) && !s.contains(a, a));
            ck("makesCycle: the same item", s.makesCycle(a, a));
            ck("makesCycle: the list is already inside the item", s.makesCycle(c, a) && s.makesCycle(b, a) && !s.makesCycle(a, d));
            fails("a list cannot go into itself", new Act() { public void run() { s.addMember(a, a); } });
            fails("a cycle is refused (c already inside a)", new Act() { public void run() { s.addMember(c, a); } });
            fails("a longer cycle is refused", new Act() { public void run() { s.addMember(c, b); } });
            ck("a refused cycle changed nothing", s.count(c) == 0 && s.listsOf(a).isEmpty() && s.check() == null);
            ck("adding an existing member again is just false, even if it would look like a cycle", !s.addMember(b, c));
            long n = s.addNote(c, "deep");
            ck("contains finds a note at any depth", s.contains(a, n) && s.contains(d, n) && !s.contains(b, d));
            // a cycle refused inside atomic does not cancel the work around it
            final long[] made = {0};
            s.atomic(new Store.Work() {
                @Override
                public void run(Store x) {
                    made[0] = x.createList("inside", 0);
                    if (!x.makesCycle(c, a)) x.addMember(c, a);
                }
            }, true);
            ck("checking makesCycle first keeps the atomic work", s.item(made[0]) != null);
            // titles
            long t = s.addTitle(a, "  head  ");
            ck("title is typed, trimmed, and joins the end", s.item(t).type == Store.TITLE && s.item(t).name.equals("head")
                    && s.members(a).get(s.count(a) - 1).item.id == t);
            fails("empty title", new Act() { public void run() { s.addTitle(a, " "); } });
            fails("title in a missing list", new Act() { public void run() { s.addTitle(999, "x"); } });
            // groups are lists with a mark
            long g = s.createList("group", a);
            s.setProp(g, Store.P_GROUP, "1");
            ck("a group is a list with a mark, and can hold several items", "1".equals(s.prop(g, Store.P_GROUP)) && s.addMember(g, n)
                    && s.addTitle(g, "g-head") > 0 && s.count(g) == 2);
            s.setProp(g, Store.P_GROUP, null);
            ck("a group turns back into a plain list", s.prop(g, Store.P_GROUP) == null && s.item(g).type == Store.LIST);
            // deleting a list that sits in two places
            s.deleteList(c);
            ck("deleting a shared list removes it from every list, dense", s.count(b) == 0 && s.count(d) == 0 && dense(s, a));
            ck("its note stays", s.item(n) != null && s.listsOf(n).equals(java.util.Arrays.asList(g)));
            ck("check is clean after nesting", s.check() == null);
        } finally {
            s.close();
        }
    }

    /** m0.4: the recycle bin, for notes and titles. */
    private void bin() throws Exception {
        final Store s = opener.fresh(counterFile());
        try {
            final long l1 = s.createList("l1", 0), l2 = s.createList("l2", 0);
            final long x = s.addNote(l1, "x");
            long y = s.addNote(l1, "y");
            s.addNote(l1, "z");
            s.addMember(l2, y);
            final long t = s.addTitle(l2, "t");
            s.trash(y);
            ck("trash takes an item out of every list", s.listsOf(y).isEmpty() && s.count(l1) == 2 && s.count(l2) == 1 && dense(s, l1) && dense(s, l2));
            ck("the bin lists it", idsOf(s.trashed()).equals(java.util.Arrays.asList(y)));
            ck("a trashed item is not unassigned", !idsOf(s.unassigned()).contains(y));
            final long yf = y;
            fails("a trashed item cannot join a list", new Act() { public void run() { s.addMember(l1, yf); } });
            s.trash(y);
            ck("trash twice does nothing more", s.trashed().size() == 1);
            s.restore(y);
            ck("restore brings it back to the same places", s.listsOf(y).equals(java.util.Arrays.asList(l1, l2))
                    && ids(s.members(l1)).indexOf(y) == 1 && ids(s.members(l2)).indexOf(y) == 0 && s.trashed().isEmpty());
            ck("restore clears the bin marks", s.prop(y, Store.P_TRASHED) == null && s.prop(y, Store.P_TRASH_FROM) == null);
            fails("restore of an item that is not in the bin", new Act() { public void run() { s.restore(yf); } });
            fails("purge of an item that is not in the bin", new Act() { public void run() { s.purge(yf); } });
            s.trash(t);
            s.deleteList(l2);
            s.restore(t);
            ck("restore when its list is gone: unassigned", s.listsOf(t).isEmpty() && idsOf(s.unassigned()).contains(t));
            ck("a title in no list is unassigned", s.item(t).type == Store.TITLE);
            s.trash(x);
            long last = s.lastId();
            s.purge(x);
            ck("purge deletes for good, and the id does not come back", s.item(x) == null && s.trashed().isEmpty()
                    && s.createList("after", 0) > last);
            final long loose = s.createFile(info("/storage/emulated/0/loose.txt", 1, 1, null));   // מ0.41: "בטל לא-משויכים"
            final long kept = s.createFile(info("/storage/emulated/0/kept.txt", 1, 1, null));
            s.addMember(l1, kept);
            fails("purge of a file that is in a list", new Act() { public void run() { s.purge(kept); } });
            s.purge(loose);
            ck("purge deletes a file that is in no list, and only it", s.item(loose) == null && s.findFile("/storage/emulated/0/loose.txt") == 0
                    && s.item(kept) != null && !idsOf(s.unassigned()).contains(loose));
            s.removeMember(l1, kept);
            s.purge(kept);
            ck("a file that left its last list can be purged", s.item(kept) == null);
            fails("a file cannot go to the bin", new Act() {
                public void run() { s.trash(s.createFile(info("/storage/emulated/0/bin.txt", 1, 1, null))); }
            });
            fails("a list cannot go to the bin", new Act() { public void run() { s.trash(l1); } });
            fails("a missing item cannot go to the bin", new Act() { public void run() { s.trash(999); } });
            ck("check is clean after the bin", s.check() == null);
        } finally {
            s.close();
        }
    }

    private void atomicity() throws Exception {
        final Store s = opener.fresh(counterFile());
        try {
            final long keepList = s.createList("keep", 0);
            final long keepFile = s.createFile(info("/keep.txt", 1, 1, null));
            s.addMember(keepList, keepFile);
            s.setOrigin(keepList, "o:keep");
            s.setProp(keepList, Store.P_WHY, "w");
            final long last = s.lastId();
            final long[] made = new long[2];
            s.atomic(new Store.Work() {
                public void run(Store x) {
                    made[0] = x.createList("inside", 0);
                    made[1] = x.createFile(info("/inside.txt", 2, 2, "2:bb"));
                    x.addMember(made[0], made[1]);
                    x.setOrigin(made[0], "o:inside");
                    x.setProp(made[0], Store.P_ICON, "i");
                }
            }, true);
            ck("atomic keep=true commits everything", s.item(made[0]) != null && s.findFile("/inside.txt") == made[1]
                    && s.count(made[0]) == 1 && s.findByOrigin("o:inside") == made[0] && "i".equals(s.prop(made[0], Store.P_ICON)));
            // "כבר שם" ו"לא היה שם" אינם כישלון: עסקה חיצונית נשמרת למרות שפעולה פנימית החזירה false
            final long[] again = new long[1];
            s.atomic(new Store.Work() {
                public void run(Store x) {
                    boolean a = x.addMember(keepList, keepFile);
                    boolean b = x.removeMember(keepList, made[0]);
                    if (!a && !b) again[0] = x.createList("after-false", 0);
                }
            }, true);
            ck("false results inside atomic do not cancel it", again[0] != 0 && s.item(again[0]) != null);
            s.deleteList(again[0]);
            final long last2 = s.lastId();
            ck("atomic ids moved on", last2 > last);
            // תצוגה מקדימה: אותה עבודה, בלי שמירה
            s.atomic(new Store.Work() {
                public void run(Store x) {
                    long l = x.createList("dry", 0);
                    long f = x.createFile(info("/dry.txt", 3, 3, null));
                    x.addMember(l, f);
                    x.addMember(keepList, f);
                    x.setOrigin(l, "o:dry");
                    x.setProp(keepList, Store.P_WHY, "changed");
                    x.updateFile(keepFile, 9, 9, "2:cc");
                    x.rename(keepList, "renamed");
                    x.removeMember(keepList, keepFile);
                }
            }, false);
            ck("atomic keep=false leaves nothing", s.lists().size() == 2 && s.findFile("/dry.txt") == 0 && s.findByOrigin("o:dry") == 0
                    && s.lastId() == last2 && s.count(keepList) == 1 && "w".equals(s.prop(keepList, Store.P_WHY))
                    && s.file(keepFile).size == 1 && s.item(keepList).name.equals("keep"));
            // חריגה באמצע: הכול חוזר, והחריגה מגיעה למי שקרא
            boolean threw = false;
            try {
                s.atomic(new Store.Work() {
                    public void run(Store x) {
                        x.createList("half", 0);
                        x.createFile(info("/half.txt", 1, 1, null));
                        x.createFile(info("/half.txt", 1, 1, null));
                    }
                }, true);
            } catch (Store.Failure e) {
                threw = true;
            }
            ck("a failure inside atomic propagates", threw);
            ck("a failure inside atomic undoes all of it", s.lists().size() == 2 && s.findFile("/half.txt") == 0 && s.lastId() == last2);
            long after = s.createList("after", 0);
            ck("work continues after a rollback", s.item(after) != null && s.check() == null);
        } finally {
            s.close();
        }
    }

    private void restore() throws Exception {
        File cf = counterFile();
        Store a = opener.fresh(cf);
        long last;
        try {
            long l = a.createList("l", 0);
            for (int i = 0; i < 4; i++) a.addNote(l, "n" + i);
            last = a.lastId();
        } finally {
            a.close();
        }
        Store b = opener.fresh(cf);                                  // מסד ריק (כמו אחרי שחזור ישן) עם אותו קובץ מונה
        try {
            ck("restored store starts above every id ever issued", b.lastId() >= last);
            long id = b.createList("after", 0);
            ck("no id repeats after restore", id > last);
        } finally {
            b.close();
        }
    }

    /** מודל בזיכרון מול הממשק, בפעולות אקראיות (זרע קבוע). */
    private void randomOps() throws Exception {
        Store s = opener.fresh(counterFile());
        try {
            Random rnd = new Random(7);
            List<Long> lists = new ArrayList<Long>();
            List<Long> notes = new ArrayList<Long>();
            java.util.Map<Long, List<Long>> model = new java.util.HashMap<Long, List<Long>>();
            for (int i = 0; i < 4; i++) {
                long id = s.createList("L" + i, 0);
                lists.add(id);
                model.put(id, new ArrayList<Long>());
            }
            boolean ok = true;
            String bad = "";
            for (int step = 0; step < 300 && ok; step++) {
                int op = rnd.nextInt(6);
                long l = lists.get(rnd.nextInt(lists.size()));
                List<Long> m = model.get(l);
                if (op <= 1) {
                    long n = s.addNote(l, "n" + step);
                    notes.add(n);
                    m.add(n);
                } else if (op == 2 && !notes.isEmpty()) {
                    long n = notes.get(rnd.nextInt(notes.size()));
                    boolean added = s.addMember(l, n);
                    if (added != !m.contains(n)) { ok = false; bad = "addMember result at step " + step; }
                    if (added) m.add(n);
                } else if (op == 3 && !m.isEmpty()) {
                    long n = m.get(rnd.nextInt(m.size()));
                    s.removeMember(l, n);
                    m.remove(n);
                } else if (op == 4 && !m.isEmpty()) {
                    long n = m.get(rnd.nextInt(m.size()));
                    int to = rnd.nextInt(m.size() + 2) - 1;
                    s.move(l, n, to);
                    m.remove(n);
                    m.add(Math.max(0, Math.min(to, m.size())), n);
                } else if (op == 5 && lists.size() > 2 && rnd.nextInt(8) == 0) {
                    s.deleteList(l);
                    lists.remove(l);
                    model.remove(l);
                }
                for (Long x : lists)
                    if (!ids(s.members(x)).equals(model.get(x)) || !dense(s, x)) { ok = false; bad = "list " + x + " at step " + step; }
            }
            ck("300 random operations match the model" + (ok ? "" : ": " + bad), ok);
            Set<Long> seen = new HashSet<Long>();
            boolean unique = true;
            for (Store.Item it : s.lists()) unique &= seen.add(it.id);
            ck("random: unique ids and clean check", unique && s.check() == null);
        } finally {
            s.close();
        }
    }
}
