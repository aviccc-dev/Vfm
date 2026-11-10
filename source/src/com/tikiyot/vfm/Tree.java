package com.tikiyot.vfm;

// עץ רשימות בזיכרון בלבד. מ0.29: המנוע הישן נמחק — אין עוד "מבנה.txt", אין קריאה ואין שמירה; נשאר רק המבנה עצמו
// (רשימה, תתי־רשימות, קבצים), למי שצריך עץ זמני: סריקת תיקייה מהכונן לפני שהיא נכנסת למסד (DbMore.attach), ותצוגה של
// המסד כעץ לחתימות (DbPicker.listsForSig). אחראי: המבנה והסריקה. אסור לו: לשמור, ולגעת בקבצים האמיתיים.

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public final class Tree {

    public final Node root = new Node();
    /** המקור לא נקרא (תקלה) — מי שמסיק "הקובץ לא ברשימות" לא יסמוך על העץ הזה. */
    boolean unreadable;

    public static final class Entry {
        public String path;
        public long size = -1;
        public final boolean bad;

        public Entry(String path, long size, boolean bad) {
            this.path = path;
            this.size = size;
            this.bad = bad;
        }

        public String name() {
            int i = path.lastIndexOf('/');
            return i >= 0 && i < path.length() - 1 ? path.substring(i + 1) : path;
        }
    }

    public static final class Node {
        public String name = "";
        public Node parent;
        public final List<Node> children = new ArrayList<Node>();
        public final List<Entry> entries = new ArrayList<Entry>();
        /** צלמית, תיאור, עיצוב, "למה" ("" — אין). */
        public String icon = "", desc = "", design = "", why = "";

        public Node child(String n) {
            for (Node c : children) if (c.name.equals(n)) return c;
            return null;
        }

        public ArrayList<String> path() {
            LinkedList<String> p = new LinkedList<String>();
            for (Node x = this; x.parent != null; x = x.parent) p.addFirst(x.name);
            return new ArrayList<String>(p);
        }

        public String uniqueChildName(String base, Node except) {
            base = Names.cleanName(base);
            if (!taken(base, except)) return base;
            int n = 2;
            while (taken(base + " (" + n + ")", except)) n++;
            return base + " (" + n + ")";
        }

        private boolean taken(String n, Node except) {
            for (Node c : children) if (c != except && c.name.equals(n)) return true;
            return false;
        }

        /** [תתי-רשימות, קבצים] בכל העומק */
        public int[] totals() {
            int[] r = {0, entries.size()};
            for (Node c : children) {
                int[] t = c.totals();
                r[0] += 1 + t[0];
                r[1] += t[1];
            }
            return r;
        }
    }

    private Tree() { }

    /** עץ ריק בזיכרון. */
    static Tree memory() {
        return new Tree();
    }

    public Node find(List<String> path) {
        Node cur = root;
        for (String p : path) {
            cur = cur.child(p);
            if (cur == null) return null;
        }
        return cur;
    }

    public static Node newChild(Node parent, String name) {
        Node c = new Node();
        c.name = Names.cleanName(name);
        c.parent = parent;
        parent.children.add(c);
        return c;
    }

    public static boolean contains(List<Entry> list, String path) {
        for (Entry e : list) if (e.path.equals(path)) return true;
        return false;
    }

    /** האם path הוא top או בתוכו. */
    static boolean inside(List<String> path, List<String> top) {
        return path.size() >= top.size() && path.subList(0, top.size()).equals(top);
    }

    /** לכל קובץ — באילו רשימות הוא (המסלול המלא של כל רשימה). */
    public java.util.Map<String, List<String>> whereListed() {
        java.util.Map<String, List<String>> out = new java.util.HashMap<String, List<String>>();
        whereListed(root, out);
        return out;
    }

    private static void whereListed(Node n, java.util.Map<String, List<String>> out) {
        String label = n.name.isEmpty() ? null : Names.fullLabel(n.path());
        for (Entry e : n.entries) {
            List<String> l = out.get(e.path);
            if (l == null) out.put(e.path, l = new ArrayList<String>());
            if (label != null && !l.contains(label)) l.add(label);
        }
        for (Node c : n.children) whereListed(c, out);
    }

    /** סריקת תיקייה מהכונן לעץ מנותק: עם המבנה (תתי־רשימות), או הקבצים בלבד. איטי — מתאים לרקע. */
    public static Node scanFolder(File folder, boolean structure) {
        Node detached = new Node();
        detached.name = Names.cleanName(folder.getName());
        if (structure) {
            fill(detached, folder);
        } else {
            List<String> all = new ArrayList<String>();
            collectFiles(folder, all);
            for (String p : all) detached.entries.add(new Entry(p, new File(p).length(), false));
        }
        return detached;
    }

    private static void fill(Node node, File dir) {
        File[] all = dir.listFiles();
        if (all == null) return;
        java.util.Arrays.sort(all);
        for (File f : all) {
            if (Vols.skip(f, false)) continue;
            if (f.isDirectory()) fill(newChild(node, node.uniqueChildName(f.getName(), null)), f);
            else node.entries.add(new Entry(f.getAbsolutePath(), f.length(), false));
        }
    }

    private static void collectFiles(File dir, List<String> out) {
        File[] all = dir.listFiles();
        if (all == null) return;
        for (File f : all) {
            if (Vols.skip(f, false)) continue;
            if (f.isDirectory()) collectFiles(f, out); else out.add(f.getAbsolutePath());
        }
    }
}
