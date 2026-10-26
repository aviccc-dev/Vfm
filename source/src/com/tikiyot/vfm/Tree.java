package com.tikiyot.vfm;

// עץ התיקיות הווירטואליות, מתוך "מבנה.txt".
// אחראי: לקרוא, לשמור, ולשנות את העץ. אסור לו: לגעת בקבצים האמיתיים.

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public final class Tree {

    public static final String SEP = " / ";

    /** 165: דרך לרשימה מטקסט — "א/ב" (בכלי) וגם "א / ב" (ברשימה בתוך רשימה). שם רשימה אינו מכיל "/" (מוחלף ב-"∕"). */
    public static ArrayList<String> parsePath(String text) {
        ArrayList<String> out = new ArrayList<String>();
        if (text != null) for (String p : text.split("/")) if (!p.trim().isEmpty()) out.add(p.trim());
        return out;
    }
    /** 161: שורה של רשימה חכמה בתוך רשימה ידנית (קובץ — תמיד מתחיל ב-"/", ולכן אין התנגשות). */
    public static final String SMART_LINE = "@חכמה ";  // תחביר
    /** 1.9.0: תיקייה חיה בתוך רשימה — נקראת מהכונן בכל פתיחה ומתעדכנת לבד. בקובץ: "@תיקייה <מסלול>". */
    public static final String LIVE_LINE = "@תיקייה ";  // תחביר
    /** 1.11.0: צלמית ותיאור לרשימה ידנית (כך היא נראית כמו סביבה / פנקס). */
    public static final String ICON_LINE = "@צלמית ", DESC_LINE = "@תיאור ";  // תחביר
    /** 1.12.0: העיצוב שנבחר לרשימה (שם קובץ עיצוב). בלי השורה — יורשת מהרשימה שמעליה (Design). */
    public static final String DESIGN_LINE = "@עיצוב ";  // תחביר
    /** 1.13.0: "למה" — המטרה של הרשימה (מוצגת בראשה כשנכנסים, מתקפלת בנגיעה). */
    public static final String WHY_LINE = "@למה ";  // תחביר
    /**
     * 1.17.2: הסדר החופשי (המילון, 12: "ברור שכן") — שורה לכל פריט, לפי הסדר: סוג (c רשימה · s חכמה · r קישור ·
     * l תיקייה מתעדכנת · f קובץ) והערך שלו. אין — הסדר הרגיל (רשימות ואז קבצים).
     */
    public static final String ORDER_LINE = "@סדר ";  // תחביר
    /** 1.13.0: הפניה לרשימה ידנית אחרת — בלי להעתיק אותה (רשימה במקומה = קבוצה, הפניה אליה = תגית). "@רשימה א / ב". */
    public static final String REF_LINE = "@רשימה ";  // תחביר

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

        public String line() {
            // גודל לא ידוע נכתב כ"?", כדי ששם שמסתיים ב" | מספר" לא ייקרא כגודל
            return size >= 0 ? path + " | " + size : path + " | ?";
        }
    }

    public static final class Node {
        public String name = "";
        public Node parent;
        public final List<Node> children = new ArrayList<Node>();
        public final List<Entry> entries = new ArrayList<Entry>();
        /** 161: רשימות חכמות בתוך הרשימה — מזהים (s-… הגדרה נפוצה, p-… פנימית); בקובץ: "@חכמה <מזהה>". */
        public final List<String> smart = new ArrayList<String>();
        /** 1.9.0: תיקיות חיות — מסלולים מלאים בכונן. */
        public final List<String> live = new ArrayList<String>();
        /** 1.11.0: צלמית ותיאור ("" — אין). */
        public String icon = "", desc = "";
        /** 1.12.0: שם העיצוב שנבחר לרשימה ("" — יורשת). */
        public String design = "";
        /** 1.13.0: למה, והפניות לרשימות אחרות (מסלולים). */
        public String why = "";
        public final List<String> refs = new ArrayList<String>();
        /** 1.17.2: הסדר החופשי — מפתחות (סוג + ערך); ריק — הסדר הרגיל. */
        public final List<String> order = new ArrayList<String>();
        boolean declared;

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
            base = cleanName(base);
            if (!taken(base, except)) return base;
            int n = 2;
            while (taken(base + " (" + n + ")", except)) n++;
            return base + " (" + n + ")";
        }

        private boolean taken(String n, Node except) {
            for (Node c : children) if (c != except && c.name.equals(n)) return true;
            return false;
        }

        public boolean isAncestorOf(Node other) {
            for (Node x = other; x != null; x = x.parent) if (x == this) return true;
            return false;
        }

        /** [תתי-תיקיות, קבצים] בכל העומק */
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

    /** נעילה אחת לכל קריאה וכתיבה של מבנה.txt. */
    public static final Object LOCK = new Object();

    /** 92: קריאה, שינוי ושמירה בנעילה אחת. apply מחזיר true אם יש מה לשמור. */
    public interface Edit {
        boolean apply(Tree t);
    }

    public static boolean edit(Edit e) {
        synchronized (LOCK) {
            Tree t = loadLocked();
            return e.apply(t) && t.saveLocked();
        }
    }

    /** שם תקין לרשימה: "/" מפריד רמות בקובץ, ו-"#" בתחילת שם נבלע בכותרת. */
    public static String cleanName(String name) {
        String s = name == null ? "" : name.replace('/', '∕').replace('\\', '∖').trim();
        while (s.startsWith("#")) s = s.substring(1).trim();
        return s.isEmpty() ? "ללא שם" : s;  // תחביר
    }

    /** לבדיקות: הפניית מבנה.txt למקום אחר. */
    public static File testFile;

    static File file() {
        return testFile != null ? testFile : Paths.tree();
    }

    public final Node root = new Node();

    private Tree() {
        root.declared = true;
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
        c.name = cleanName(name);
        c.parent = parent;
        c.declared = true;
        parent.children.add(c);
        return c;
    }

    public static boolean contains(List<Entry> list, String path) {
        for (Entry e : list) if (e.path.equals(path)) return true;
        return false;
    }

    public static boolean remove(List<Entry> list, String path) {
        for (int i = 0; i < list.size(); i++)
            if (list.get(i).path.equals(path)) {
                list.remove(i);
                return true;
            }
        return false;
    }

    // ---------- קריאה ----------

    private static String clean(String raw) {
        return raw.replace("\uFEFF", "").replace("\u200E", "").replace("\u200F", "")
                .replace("\u202A", "").replace("\u202B", "").replace("\u202C", "").trim();
    }

    public static Entry parseEntry(String line) {
        if (!line.startsWith("/")) return new Entry(line, -1, true);
        int bar = line.lastIndexOf(" | ");
        if (bar > 0 && line.substring(bar + 3).trim().equals("?"))
            return new Entry(line.substring(0, bar).trim(), -1, false);
        if (bar > 0) {
            try {
                return new Entry(line.substring(0, bar).trim(),
                        Long.parseLong(line.substring(bar + 3).trim()), false);
            } catch (NumberFormatException ignored) {
            }
        }
        return new Entry(line, -1, false);
    }

    public static Tree load() {
        synchronized (LOCK) {
            return loadLocked();
        }
    }

    /**
     * 90: הקובץ קיים ולא נקרא. העץ ריק, ואסור לשמור אותו — שמירה הייתה דורסת את כל הרשימות.
     * כל שמירה נדחית, והמסכים מודיעים (VirtualScreen).
     */
    public boolean unreadable;

    private static Tree loadLocked() {
        return parse(file(), true);
    }

    /** 162: רשימות של אופן עבודה אחר (לקריאה בלבד — בלי תיקון ושמירה אוטומטיים). */
    static Tree loadFrom(File f) {
        return parse(f, false);
    }

    private static Tree parse(File f, boolean fix) {
        Tree t = new Tree();
        if (!f.exists()) return t;
        String content;
        try {
            content = Files.readUtf8(f);
        } catch (Throwable e) {
            Log.e("קריאת מבנה.txt", e);
            t.unreadable = true;
            return t;
        }
        boolean dirty = false;
        Node cur = t.root;
        for (String raw : content.split("\r\n|\r|\n")) {
            String line = clean(raw);
            if (line.isEmpty()) continue;
            if (line.startsWith(ICON_LINE.trim())) {                    // 1.11.0
                cur.icon = line.substring(ICON_LINE.trim().length()).trim();
                continue;
            }
            if (line.startsWith(DESIGN_LINE.trim())) {                  // 1.12.0
                cur.design = line.substring(DESIGN_LINE.trim().length()).trim();
                continue;
            }
            if (line.startsWith(DESC_LINE.trim())) {
                cur.desc = line.substring(DESC_LINE.trim().length()).trim();
                continue;
            }
            if (line.startsWith(WHY_LINE.trim() + " ") || line.equals(WHY_LINE.trim())) {   // 1.13.0
                cur.why = line.substring(WHY_LINE.trim().length()).trim();
                continue;
            }
            if (line.startsWith(ORDER_LINE)) {                          // 1.17.2: הסדר החופשי
                String k = line.substring(ORDER_LINE.length()).trim();
                if (k.length() > 1 && !cur.order.contains(k)) cur.order.add(k);
                continue;
            }
            if (line.startsWith(REF_LINE)) {                            // 1.13.0: הפניה לרשימה
                String r = line.substring(REF_LINE.length()).trim();
                if (!r.isEmpty() && !cur.refs.contains(r)) cur.refs.add(r);
                continue;
            }
            if (line.startsWith(LIVE_LINE)) {                           // 1.9.0: תיקייה חיה
                String p = line.substring(LIVE_LINE.length()).trim();
                if (!p.isEmpty() && !cur.live.contains(p)) cur.live.add(p);
                continue;
            }
            if (line.startsWith(SMART_LINE)) {                          // 161: רשימה חכמה בתוך הרשימה
                String ref = line.substring(SMART_LINE.length()).trim();
                if (!ref.isEmpty() && !cur.smart.contains(ref)) cur.smart.add(ref);
                continue;
            }
            if (!line.startsWith("#")) {
                cur.entries.add(parseEntry(line));
                continue;
            }
            int i = 1;
            while (i < line.length() && Character.isDigit(line.charAt(i))) i++;
            String digits = line.substring(1, i);
            String rest = line.substring(i).trim();
            while (rest.startsWith("#")) rest = rest.substring(1).trim();
            List<String> parts = new ArrayList<String>();
            for (String p : rest.split("/")) {
                p = p.trim();
                if (!p.isEmpty()) parts.add(p);
            }
            if (parts.isEmpty()) {
                parts.add("ללא שם");  // תחביר
                dirty = true;
            }
            int level = -1;
            try {
                if (!digits.isEmpty()) level = Integer.parseInt(digits);
            } catch (NumberFormatException ignored) {
            }
            if (level != parts.size()) dirty = true;
            Node parent = t.root;
            for (int k = 0; k < parts.size() - 1; k++) {
                Node c = parent.child(parts.get(k));
                if (c == null) {
                    c = newChild(parent, parts.get(k));
                    c.declared = false;
                    dirty = true;
                }
                parent = c;
            }
            String last = parts.get(parts.size() - 1);
            Node n = parent.child(last);
            if (n == null) {
                n = newChild(parent, last);
            } else if (!n.declared) {
                n.declared = true;
            } else {
                n = newChild(parent, parent.uniqueChildName(last, null));
                dirty = true;
                Log.i("מוספרה תיקייה כפולה: " + rest);
            }
            cur = n;
        }
        if (dirty && fix) t.save();
        return t;
    }

    /** 162: שמירה לקובץ של אופן אחר (אחרי העתקה אליו). */
    boolean saveTo(File f) {
        try {
            Files.writeAtomic(f, text());
            return true;
        } catch (IOException e) {
            Log.e("שמירת מבנה.txt של אופן אחר", e);
            return false;
        }
    }

    // ---------- שמירה ----------

    public boolean save() {
        synchronized (LOCK) {
            return saveLocked();
        }
    }

    private boolean saveLocked() {
        if (unreadable) {
            Log.i("מבנה.txt לא נקרא — השמירה נדחתה, כדי לא לדרוס את הרשימות");
            return false;
        }
        try {
            if (testFile == null) Files.backup(file(), "מבנה");  // בכונן
            Files.writeAtomic(file(), text());
            return true;
        } catch (IOException e) {
            Log.e("שמירת מבנה.txt", e);
            return false;
        }
    }

    /**
     * הקובץ כולו. 1.14.0: שמירה אחת לשני המקומות (עד כאן — כתובה פעמיים), ובשורש גם הרשימות החכמות וההפניות
     * (באג: מה שנוסף ל"רשימות ידניות" עצמה — נשמט בשמירה הבאה).
     */
    private String text() {
        StringBuilder sb = new StringBuilder();
        body(sb, root);
        return sb.toString();
    }

    private static void write(StringBuilder sb, Node n) {
        if (sb.length() > 0) sb.append('\n');
        List<String> p = n.path();
        sb.append('#').append(p.size()).append(' ').append(join(p, SEP)).append('\n');
        body(sb, n);
    }

    /** מה שבתוך רשימה (גם השורש — שם אין לה שורת כותרת). */
    private static void body(StringBuilder sb, Node n) {
        if (!n.icon.isEmpty()) sb.append(ICON_LINE).append(n.icon).append('\n');           // 1.11.0
        if (!n.design.isEmpty()) sb.append(DESIGN_LINE).append(n.design).append('\n');       // 1.12.0
        if (!n.desc.isEmpty()) sb.append(DESC_LINE).append(n.desc.replace('\n', ' ')).append('\n');
        if (!n.why.isEmpty()) sb.append(WHY_LINE).append(n.why.replace('\n', ' ')).append('\n');   // 1.13.0
        for (String k : n.order) sb.append(ORDER_LINE).append(k).append('\n');                // 1.17.2
        for (String r : n.smart) sb.append(SMART_LINE).append(r).append('\n');                // 161
        for (String r : n.refs) sb.append(REF_LINE).append(r).append('\n');                   // 1.13.0
        for (String l : n.live) sb.append(LIVE_LINE).append(l).append('\n');                  // 1.9.0
        for (Entry e : n.entries) sb.append(e.line()).append('\n');
        for (Node c : n.children) write(sb, c);
    }

    // ---------- באילו רשימות (80.4) ----------

    /** 169: רשימה חכמה שבתוך רשימה ידנית — השם שלה ומה שהיא מכילה עכשיו; name — null אם כבר אינה קיימת. */
    public interface Inner {
        String name(String ref);

        List<String> paths(String ref);
    }

    /** לכל קובץ: שמות הרשימות שבהן הוא נמצא, לפי סדר העץ. פונקציה אחת לכל הכלים. */
    public java.util.Map<String, List<String>> whereListed() {
        return whereListed(null);
    }

    /** 169: וגם הרשימות החכמות שבתוך רשימות ידניות ("מתכונים / צילומי מסך מהשבוע"). inner — null: בלעדיהן. */
    public java.util.Map<String, List<String>> whereListed(Inner inner) {
        java.util.Map<String, List<String>> out = new java.util.HashMap<String, List<String>>();
        whereListed(root, out, inner);
        return out;
    }

    private static void whereListed(Node n, java.util.Map<String, List<String>> out, Inner inner) {
        // 89: מסלול מלא ("מתכונים / פסח") — שתי רשימות באותו שם בענפים שונים הן שתיים
        String full = n.name.isEmpty() ? null : fullLabel(n.path());
        for (Entry e : n.entries) listed(out, e.path, full);
        if (inner != null && full != null)
            for (String ref : n.smart) {
                String name = inner.name(ref);
                if (name != null) for (String p : inner.paths(ref)) listed(out, p, full + " / " + name);
            }
        for (Node c : n.children) whereListed(c, out, inner);
    }

    private static void listed(java.util.Map<String, List<String>> out, String path, String label) {
        List<String> l = out.get(path);
        if (l == null) out.put(path, l = new ArrayList<String>());
        if (label != null && !l.contains(label)) l.add(label);
    }

    /** הרשימות שבהן קובץ אחד נמצא. */
    public static List<String> listsOf(String path) {
        Tree t = load();
        List<String> l = t.whereListed().get(path);
        if (l == null) l = new ArrayList<String>();
        liveListsOf(t.root, path, l);                                         // 1.9.0
        return l;
    }

    /** 1.9.0: הקובץ נמצא בתוך תיקייה חיה של הרשימה — לפי תחילת המסלול, בלי לקרוא את הכונן. */
    public static boolean inLive(String folder, String path) {
        return path.startsWith(folder.endsWith("/") ? folder : folder + "/");
    }

    private static void liveListsOf(Node n, String path, List<String> out) {
        if (!n.name.isEmpty())
            for (String f : n.live)
                if (inLive(f, path)) {
                    String full = fullLabel(n.path());
                    if (!out.contains(full)) out.add(full);
                    break;
                }
        for (Node c : n.children) liveListsOf(c, path, out);
    }

    // ---------- פעולות על פריטים ----------

    /** מוסיף קבצים לרשימה. מחזיר כמה נוספו (קבצים שכבר ברשימה לא נוספים שוב). */
    public static int addFiles(List<String> listPath, List<String> files) {
        synchronized (LOCK) {
            Tree t = load();
            Node n = t.find(listPath);
            if (n == null) return -1;
            int added = 0;
            for (String p : files) {
                if (contains(n.entries, p)) continue;
                n.entries.add(new Entry(p, new File(p).length(), false));
                added++;
            }
            if (added > 0) t.save();
            return added;
        }
    }

    /** מעביר או מוסיף פריטים מרשימה לרשימה. move=true מוציא אותם מהמקור. */
    /**
     * 1.13.1: מה נעשה בהעתקה עם הקישורים שברשימה — רשימה חכמה (מזהה) והפניה לרשימה ידנית (מסלול).
     * מחזיר את הקישור ביעד, או null — לא מועתק. באותו עץ — כמו שהם (SAME); לאופן אחר — ModeCopy מעתיק אותם.
     */
    interface Links {
        String smart(String ref);
        String list(String path);
    }

    static final Links SAME = new Links() {
        @Override public String smart(String ref) { return ref; }
        @Override public String list(String path) { return path; }
    };

    /** 1.14.0: האם path הוא top או בתוכו (הפניה או הדבקה לתוך עצמה — לולאה). */
    static boolean inside(List<String> path, List<String> top) {
        return path.size() >= top.size() && path.subList(0, top.size()).equals(top);
    }

    /** 1.14.0: הפניה ↪ ברשימה b (שבמסלול dest) אל target — לא לעצמה ולא למה שמעליה, ולא פעמיים. */
    static boolean addRef(Node b, List<String> dest, List<String> target) {
        if (target.isEmpty() || inside(dest, target)) return false;
        String rp = fullLabel(target);
        if (b.refs.contains(rp)) return false;
        b.refs.add(rp);
        return true;
    }

    /** 1.13.1: מסלול שמתחיל ב-from — עם to במקומו; לא מתחיל — null. */
    static List<String> moved(List<String> p, List<String> from, List<String> to) {
        if (from.isEmpty() || !inside(p, from)) return null;
        List<String> out = new ArrayList<String>(to);
        out.addAll(p.subList(from.size(), p.size()));
        return out;
    }

    /** 1.13.1 (באג): רשימה שינתה שם או מקום — ההפניות אליה ("@רשימה") בכל העץ עוברות איתה. מחזיר כמה. */
    static int refsMoved(final List<String> from, final List<String> to) {
        final int[] n = {0};
        edit(new Edit() {
            @Override
            public boolean apply(Tree t) {
                refsMoved(t.root, from, to, n);
                return n[0] > 0;
            }
        });
        return n[0];
    }

    private static void refsMoved(Node node, List<String> from, List<String> to, int[] n) {
        for (int i = 0; i < node.refs.size(); i++) {
            List<String> p = moved(parsePath(node.refs.get(i)), from, to);
            if (p == null) continue;
            node.refs.set(i, fullLabel(p));
            n[0]++;
        }
        for (Node c : node.children) refsMoved(c, from, to, n);
    }

    /** העתקה של רשימה עם כל מה שבתוכה — באותו עץ. */
    static void copyNode(Node from, Node to) {
        copyNode(from, to, SAME);
    }

    /**
     * העתקה של רשימה עם כל מה שבתוכה: קבצים, תיקיות חיות, רשימות חכמות, הפניות, צלמית, תיאור, עיצוב, למה,
     * ותתי־הרשימות. 1.13.1 (באג): עד כאן כל מעתיק שכח משהו — כאן לבדו העותק נבנה, לכל המעתיקים.
     */
    static void copyNode(Node from, Node to, Links links) {
        for (Entry e : from.entries) to.entries.add(new Entry(e.path, e.size, e.bad));
        for (String l : from.live) if (!to.live.contains(l)) to.live.add(l);             // 1.9.0
        for (String r : from.smart) {                                                     // 161
            String n = links.smart(r);
            if (n != null && !to.smart.contains(n)) to.smart.add(n);
        }
        for (String r : from.refs) {                                                      // 1.13.0
            String n = links.list(r);
            if (n != null && !to.refs.contains(n)) to.refs.add(n);
        }
        if (to.icon.isEmpty()) to.icon = from.icon;                                       // 1.11.0
        if (to.desc.isEmpty()) to.desc = from.desc;
        if (to.design.isEmpty()) to.design = from.design;                                 // 1.12.0
        if (to.why.isEmpty()) to.why = from.why;                                          // 1.13.0
        if (to.order.isEmpty())                                                           // 1.17.2: גם הסדר
            for (String k : from.order) {
                String v = k.substring(1);
                String n = k.charAt(0) == 's' ? links.smart(v) : k.charAt(0) == 'r' ? links.list(v) : v;
                if (n != null) to.order.add(k.charAt(0) + n);
            }
        for (Node c : from.children) copyNode(c, newChild(to, c.name), links);
    }

    public static int transfer(List<String> from, List<String> to, List<String> paths, boolean move) {
        synchronized (LOCK) {
            Tree t = load();
            Node a = t.find(from), b = t.find(to);
            if (a == null || b == null || a == b) return -1;
            int done = 0;
            for (String p : paths) {
                Entry src = null;
                for (Entry e : a.entries) if (e.path.equals(p)) src = e;
                if (src == null) continue;
                if (!contains(b.entries, p)) {
                    b.entries.add(new Entry(src.path, src.size, src.bad));
                    done++;
                }
                if (move) a.entries.remove(src);
            }
            t.save();
            return done;
        }
    }

    /** בכמה רשימות מופיע כל אחד מהמסלולים (סך המופעים). */
    public static int countEverywhere(java.util.Collection<String> paths) {
        synchronized (LOCK) {
            return countIn(load().root, new java.util.HashSet<String>(paths));
        }
    }

    private static int countIn(Node n, java.util.Set<String> paths) {
        int c = 0;
        for (Entry e : n.entries) if (paths.contains(e.path)) c++;
        for (Node ch : n.children) c += countIn(ch, paths);
        return c;
    }

    /** קבצים שזזו: כל פריט שהצביע על המסלול הישן מצביע על החדש. מחזיר כמה עודכנו. */
    public static int replaceEverywhere(java.util.Map<String, String> moved) {
        synchronized (LOCK) {
            Tree t = load();
            int n = replaceIn(t.root, moved);
            if (n > 0) t.save();
            return n;
        }
    }

    private static int replaceIn(Node node, java.util.Map<String, String> moved) {
        int c = 0;
        for (Entry e : node.entries) {
            String to = moved.get(e.path);
            if (to != null) {
                e.path = to;
                c++;
            }
        }
        c += retarget(node.live, moved);                 // 1.9.0: תיקייה חיה שזזה
        if (c > 0) {                                     // אותו קובץ פעמיים באותה רשימה — נשאר פריט אחד (82)
            java.util.Set<String> seen = new java.util.HashSet<String>();
            for (java.util.Iterator<Entry> it = node.entries.iterator(); it.hasNext(); )
                if (!seen.add(it.next().path)) it.remove();
        }
        for (Node ch : node.children) c += replaceIn(ch, moved);
        return c;
    }

    /**
     * 1.9.0: תיקייה חיה שזזה (העברה או שינוי שם — הקבצים שבה עברו, והיא עצמה כבר לא קיימת): המסלול שלה
     * מתעדכן לפי קובץ אחד שעבר. תיקייה שעדיין קיימת — לא נוגעים (רק חלק מהקבצים יצא ממנה).
     */
    static int retarget(List<String> live, java.util.Map<String, String> moved) {
        int c = 0;
        for (int i = 0; i < live.size(); i++) {
            String f = live.get(i);
            String to = moved.get(f);
            if (to == null && !new File(f).exists())
                for (java.util.Map.Entry<String, String> m : moved.entrySet())
                    if (inLive(f, m.getKey())) {
                        String rel = m.getKey().substring(f.length());
                        if (m.getValue().endsWith(rel)) to = m.getValue().substring(0, m.getValue().length() - rel.length());
                        break;
                    }
            if (to != null && !to.equals(f) && !live.contains(to)) {
                live.set(i, to);
                c++;
            }
        }
        return c;
    }

    /** מסיר את המסלולים מכל הרשימות. מחזיר כמה פריטים הוסרו. */
    public static int removeEverywhere(java.util.Collection<String> paths) {
        synchronized (LOCK) {
            Tree t = load();
            int removed = removeIn(t.root, new java.util.HashSet<String>(paths));
            if (removed > 0) t.save();
            return removed;
        }
    }

    private static int removeIn(Node n, java.util.Set<String> paths) {
        int c = 0;
        for (java.util.Iterator<Entry> it = n.entries.iterator(); it.hasNext(); )
            if (paths.contains(it.next().path)) {
                it.remove();
                c++;
            }
        for (Node ch : n.children) c += removeIn(ch, paths);
        return c;
    }

    public static int removeFiles(List<String> listPath, List<String> paths) {
        synchronized (LOCK) {
            Tree t = load();
            Node n = t.find(listPath);
            if (n == null) return -1;
            int removed = 0;
            for (String p : paths) if (remove(n.entries, p)) removed++;
            if (removed > 0) t.save();
            return removed;
        }
    }

    /** מוסיף תיקייה מהכונן: כרשימת-משנה עם המבנה, או הקבצים בלבד. מחזיר [רשימות, פריטים]. */
    /** סריקת תיקייה מהכונן לעץ מנותק. איטי — מתאים לרקע, ואינו נוגע ב"מבנה.txt". */
    public static Node scanFolder(File folder, boolean structure) {
        Node detached = new Node();
        detached.name = cleanName(folder.getName());
        if (structure) {
            fill(detached, folder, new int[2]);
        } else {
            List<String> all = new ArrayList<String>();
            collectFiles(folder, all);
            for (String p : all) detached.entries.add(new Entry(p, new File(p).length(), false));
        }
        return detached;
    }

    /** מחבר עץ שנסרק לרשימה. מהיר — נעשה בנעילה. מחזיר [רשימות, פריטים]. */
    public static int[] attach(List<String> listPath, Node scanned, boolean structure) {
        synchronized (LOCK) {
            Tree t = load();
            Node n = t.find(listPath);
            if (n == null) return null;
            int[] count = {0, 0};
            if (structure) {
                Node c = newChild(n, n.uniqueChildName(scanned.name, null));
                count[0]++;
                graft(scanned, c, count);
            } else {
                for (Entry e : scanned.entries)
                    if (!contains(n.entries, e.path)) {
                        n.entries.add(e);
                        count[1]++;
                    }
            }
            t.save();
            return count;
        }
    }

    private static void graft(Node from, Node to, int[] count) {
        for (Entry e : from.entries) {
            to.entries.add(e);
            count[1]++;
        }
        for (Node c : from.children) {
            Node x = newChild(to, to.uniqueChildName(c.name, null));
            count[0]++;
            graft(c, x, count);
        }
    }

    private static void fill(Node node, File dir, int[] count) {
        File[] all = dir.listFiles();
        if (all == null) return;
        java.util.Arrays.sort(all);
        for (File f : all) {
            if (Vols.skip(f, false)) continue;
            if (f.isDirectory()) {
                Node c = newChild(node, node.uniqueChildName(f.getName(), null));
                count[0]++;
                fill(c, f, count);
            } else {
                node.entries.add(new Entry(f.getAbsolutePath(), f.length(), false));
                count[1]++;
            }
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

    /** 89: המסלול המלא של רשימה, "מועדים / פסח" (כך גם בהפניה ↪). */
    public static String fullLabel(List<String> path) {
        if (path == null || path.isEmpty()) return Words.ROOT;
        return join(path, SEP);
    }

    /** 1.14.0: מסלול רשימה כמו שכלי ופקודה כותבים אותו — "מועדים/פסח" (עד כאן — אותה לולאה בעשרה מקומות). */
    public static String key(List<String> path) {
        return join(path, "/");
    }

    static String join(List<String> path, String sep) {
        StringBuilder sb = new StringBuilder();
        if (path != null) for (String p : path) sb.append(sb.length() > 0 ? sep : "").append(p);
        return sb.toString();
    }

    public static String label(List<String> path) {
        return path == null || path.isEmpty() ? Words.ROOT : path.get(path.size() - 1);
    }

    /** מחליף מסלול בכל התיקיות. מחזיר כמה מופעים עודכנו. */
    public int replacePath(String oldPath, String newPath, long size) {
        return replaceIn(root, oldPath, newPath, size);
    }

    private static int replaceIn(Node node, String oldPath, String newPath, long size) {
        int n = 0;
        for (Entry e : node.entries) {
            if (e.path.equals(oldPath)) {
                e.path = newPath;
                if (size >= 0) e.size = size;
                n++;
            }
        }
        for (Node c : node.children) n += replaceIn(c, oldPath, newPath, size);
        return n;
    }
}
