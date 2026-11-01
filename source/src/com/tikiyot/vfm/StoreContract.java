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
            fails("list into list is not allowed yet", new Act() { public void run() { s.addMember(l1, l2); } });
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
