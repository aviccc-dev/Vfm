package com.tikiyot.vfm;

// עץ רשימות בזיכרון בלבד. מ0.29: המנוע הישן נמחק — אין עוד "מבנה.txt", אין קריאה ואין שמירה; נשאר רק המבנה עצמו
// (רשימה, תתי־רשימות, קבצים), למי שצריך עץ זמני: סריקת תיקייה מהכונן לפני שהיא נכנסת למסד (DbMore.attach), ותצוגה של
// המסד כעץ לחתימות (DbLists.listsForSig). אחראי: המבנה והסריקה. אסור לו: לשמור, ולגעת בקבצים האמיתיים.

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class Tree {

    public final Node root = new Node();
    /** המקור לא נקרא (תקלה) — מי שמסיק "הקובץ לא ברשימות" לא יסמוך על העץ הזה. */
    boolean unreadable;

    public static final class Entry {
        public String path;
        public long size = -1;

        public Entry(String path, long size) {
            this.path = path;
            this.size = size;
        }
    }

    public static final class Node {
        public String name = "";
        public final List<Node> children = new ArrayList<Node>();
        public final List<Entry> entries = new ArrayList<Entry>();

        public String uniqueChildName(String base) {
            base = Names.cleanName(base);
            if (!taken(base)) return base;
            int n = 2;
            while (taken(base + " (" + n + ")")) n++;
            return base + " (" + n + ")";
        }

        private boolean taken(String n) {
            for (Node c : children) if (c.name.equals(n)) return true;
            return false;
        }
    }

    private Tree() { }

    /** עץ ריק בזיכרון. */
    static Tree memory() {
        return new Tree();
    }

    public static Node newChild(Node parent, String name) {
        Node c = new Node();
        c.name = Names.cleanName(name);
        parent.children.add(c);
        return c;
    }

    /** האם path הוא top או בתוכו. */
    static boolean inside(List<String> path, List<String> top) {
        return path.size() >= top.size() && path.subList(0, top.size()).equals(top);
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
            for (String p : all) detached.entries.add(new Entry(p, new File(p).length()));
        }
        return detached;
    }

    private static void fill(Node node, File dir) {
        File[] all = dir.listFiles();
        if (all == null) return;
        java.util.Arrays.sort(all);
        for (File f : all) {
            if (Vols.skip(f, false)) continue;
            if (f.isDirectory()) fill(newChild(node, node.uniqueChildName(f.getName())), f);
            else node.entries.add(new Entry(f.getAbsolutePath(), f.length()));
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
