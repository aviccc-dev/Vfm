package com.tikiyot.vfm;

// רשימה בתוך רשימה (161) — המודל, בלי ממשק (נבדק). ההחלטות (בתוספות, "רשימה חכמה ככלי האחד"):
// מוצגות בראש הרשימה כשורות, עם התווית ("רשימה חכמה" / "רשימה ידנית"); הגדרה נפוצה — קישור (שינוי בה — בכל מקום);
// חדשה — מקומית, שייכת רק לרשימה שהיא בתוכה; ידנית בתוך חכמה — קישור לרשימה הקיימת; חכמה בתוך ידנית — אותו כלל.
// הספירה — כמו רשימה ידנית בתוך ידנית: מבחוץ כולל מה שבפנים.
//
// הפניה (ref): "s-…" — הגדרה נפוצה · "p-…" — רשימה פנימית מקומית · "m:<מסלול>" — רשימה ידנית.
// היכן נשמרות: ברשימה חכמה — המפתח "רשימות בתוך" בסעיף שלה (כלי, הגדרה נפוצה או פנימית), מופרדות ב-"|";
// ברשימה ידנית — שורות "@חכמה <מזהה>" במבנה.txt (Tree.Node.smart). רשימה פנימית — סעיף [רשימה פנימית: …].

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class Nested {

    static final String KEY = "רשימות בתוך";  // תחביר
    static final String INNER = "רשימה פנימית: ";  // תחביר
    static final String MANUAL = "m:";

    private Nested() { }

    /** מה ההפניה: הגדרה נפוצה, רשימה פנימית, כלי, רשימה ידנית — לפי הסעיף שלה (לא לפי צורת המזהה). */
    enum Kind { COMMON, INNER, TOOL, MANUAL, NONE }

    static Kind kindOf(String ref) {
        if (ref == null || ref.isEmpty()) return Kind.NONE;
        if (ref.startsWith(MANUAL)) return Kind.MANUAL;
        Structure.Func f = Structure.byId(ref);
        if (f == null) return Kind.NONE;
        if (f.name.startsWith(SmartLists.SECTION)) return Kind.COMMON;
        if (f.name.startsWith(INNER)) return Kind.INNER;
        if (f.name.startsWith(Structure.FUNC)) return Kind.TOOL;
        return Kind.NONE;
    }

    /** רשימה בתוך רשימה, כמו שמוצגת. */
    static final class Item {
        String ref = "", name = "", cond = "", sort = "", view = "", icon = "";
        /** השורה השנייה (מחושבת פעם אחת לכל הצגה — לא בכל ציור). */
        String sub = "";
        boolean smart;
        List<String> path;
        /** ההפניה כבר לא מובילה לשום דבר (נמחקה). */
        boolean gone;
        /** הגדרה נפוצה (קישור) / רשימה פנימית מקומית. */
        boolean common, local;
        /** 1.15.1: כפולים (הלשונית השנייה של רשימה חכמה) — נפתחים במסך הכפולים, עם ההגדרות שלהם. */
        Target dup;
    }

    // ---------- בתוך רשימה חכמה ----------

    /** ההפניות שבתוך רשימה חכמה (לפי מזהה הסעיף שלה). */
    static List<String> refs(String ownerId) {
        Structure.Func f = Structure.byId(ownerId);
        List<String> out = new ArrayList<String>();
        if (f == null) return out;
        String v = f.params.get(KEY);
        if (v != null) for (String r : v.split("\\|")) if (!r.trim().isEmpty() && !out.contains(r.trim())) out.add(r.trim());
        return out;
    }

    /** מוסיף; false — כבר שם, היא עצמה, או שהבעלים לא נמצא. */
    static boolean add(String ownerId, String ref) {
        if (ref == null || ref.isEmpty() || ref.equals(ownerId)) return false;   // לא בתוך עצמה
        List<String> r = refs(ownerId);
        if (r.contains(ref)) return false;
        r.add(ref);
        return Structure.setById(ownerId, KEY, join(r));
    }

    /** מוציא (רשימה פנימית — נמחקת; הגדרה נפוצה ורשימה ידנית — נשארות, רק הקישור יוצא). */
    static boolean remove(String ownerId, String ref) {
        List<String> r = refs(ownerId);
        if (!r.remove(ref)) return false;
        Structure.setById(ownerId, KEY, join(r));
        if (kindOf(ref) == Kind.INNER && uses(ref) == 0) Structure.removeById(ref);
        return true;
    }

    /**
     * 1.15.0: בכמה מקומות הרשימה הזו נמצאת — ברשימות ידניות ובתוך רשימות חכמות. רשימה פנימית שנמצאת עוד במקום אחר
     * לא נמחקת כשמוציאים אותה (מאז שהסביבות הוסבו, רשימה חכמה של הדוגמאות נמצאת גם ב"לפי מושג").
     */
    static int uses(String ref) {
        int[] n = {0};
        count(Tree.load().root, ref, n);
        for (Ini.Line l : Structure.ini().lines)
            if (KEY.equals(l.key) && l.value != null)
                for (String r : l.value.split("\\|")) if (r.trim().equals(ref)) n[0]++;
        return n[0];
    }

    private static void count(Tree.Node node, String ref, int[] n) {
        if (node.smart.contains(ref)) n[0]++;
        for (Tree.Node c : node.children) count(c, ref, n);
    }

    private static String join(List<String> l) {
        StringBuilder sb = new StringBuilder();
        for (String s : l) sb.append(sb.length() > 0 ? "|" : "").append(s);
        return sb.toString();
    }

    static String manual(List<String> path) {
        StringBuilder sb = new StringBuilder(MANUAL);
        for (int i = 0; i < path.size(); i++) sb.append(i > 0 ? Tree.SEP : "").append(path.get(i));
        return sb.toString();
    }

    // ---------- רשימה פנימית מקומית ----------

    /** רשימה חכמה חדשה, מקומית — שייכת רק ל-ownerLabel. מחזיר את המזהה (p-…). */
    static String newInner(String ownerLabel, String name, String cond, String sort, String view) {
        Map<String, String> kv = new LinkedHashMap<String, String>();
        kv.put(SmartLists.COND, cond);
        kv.put(SmartLists.SORT, sort);
        if (view != null && !view.isEmpty()) kv.put(SmartLists.VIEW, view);
        String section = ownerLabel + " › " + name;
        String n = section;
        for (int i = 2; exact(n); i++) n = section + " (" + i + ")";
        return Structure.putSection(INNER, n, "p", kv);
    }

    private static boolean exact(String n) {
        for (Structure.Func f : Structure.sections(INNER)) if (f.name.equals(n)) return true;
        return false;
    }

    /** עדכון רשימה פנימית ("שמירה" בתוכה). */
    static void saveInner(String id, String cond, String sort, String view) {
        Structure.setById(id, SmartLists.COND, cond);
        Structure.setById(id, SmartLists.SORT, sort);
        Structure.setById(id, SmartLists.VIEW, view);
    }

    // ---------- מה כל הפניה ----------

    static Item resolve(String ref) {
        Item it = new Item();
        it.ref = ref;
        if (ref.startsWith(MANUAL)) {
            it.path = Tree.parsePath(ref.substring(MANUAL.length()));         // 165
            it.name = it.path.get(it.path.size() - 1);
            it.gone = Tree.load().find(it.path) == null;
            return it;
        }
        it.smart = true;
        Kind k = kindOf(ref);
        it.common = k == Kind.COMMON;
        it.local = k == Kind.INNER;
        if (k == Kind.NONE) {
            it.gone = true;
            return it;
        }
        if (it.common) {
            SmartLists.Named n = SmartLists.byId(ref);
            if (n == null) it.gone = true;
            else {
                it.name = n.name;
                it.cond = n.cond;
                it.sort = n.sort;
                it.view = n.view;
            }
            return it;
        }
        Structure.Func f = Structure.byId(ref);
        if (f == null) {
            it.gone = true;
            return it;
        }
        it.icon = f.params.containsKey(Program.ICON) ? f.params.get(Program.ICON) : "";   // 1.17.0: הצלמית שנבחרה בהגדרות
        String h = f.name.startsWith(INNER) ? f.name.substring(INNER.length()) : f.name;
        int cut = h.lastIndexOf(" › ");
        it.name = cut >= 0 ? h.substring(cut + 3) : h;
        if (FnDup.KIND.equals(f.params.get("סוג"))) {                            // תחביר
            it.dup = Target.of(FnDup.KIND, f.params);
            it.dup.id = ref;
            return it;
        }
        String[] r = SmartLists.resolve(Target.of(FnSmart.KIND, f.params));   // 1.15.0 (באג): גם מקושרת — לפי הנפוצה
        it.cond = r[0];
        it.sort = r[1];
        it.view = r[2];
        return it;
    }

    /** 1.15.1: פתיחה — רשימה חכמה במסך החיפוש, כפולים במסך הכפולים (מכל מקום שיש בו רשימה בתוך רשימה). */
    static void open(android.app.Activity a, Item it) {
        if (it.dup != null) Func.byKind(FnDup.KIND).open(a, it.dup);
        else a.startActivity(SearchScreen.innerIntent(a, it));
    }

    /** 1.15.0: ההגדרה הנפוצה שרשימה (פנימית או כלי) מקושרת אליה, או "". */
    static String linkOf(String ref) {
        Structure.Func f = ref == null ? null : Structure.byId(ref);
        return f == null ? "" : SmartLists.linkOf(Target.of(FnSmart.KIND, f.params));
    }

    static List<Item> items(List<String> refs) {
        List<Item> out = new ArrayList<Item>();
        for (String r : refs) out.add(resolve(r));
        return out;
    }

    /**
     * 169: "באילו רשימות" — גם דרך רשימה חכמה שבתוך רשימה ידנית. כל אחת מחושבת פעם אחת, לפי האינדקס
     * (כמו הספירה שלה ברשימה: VirtualScreen.hitsOf). אינדקס שעוד לא מוכן — בלעדיהן.
     */
    static Tree.Inner inner(final Library lib) {
        return new Tree.Inner() {
            private final Map<String, Item> items = new java.util.HashMap<String, Item>();
            private final Map<String, List<String>> paths = new java.util.HashMap<String, List<String>>();

            private Item item(String ref) {
                Item it = items.get(ref);
                if (it == null) items.put(ref, it = resolve(ref));
                return it;
            }

            @Override
            public String name(String ref) {
                Item it = item(ref);
                return it.gone || !it.smart || it.dup != null ? null : it.name;   // כפולים — לא "רשימה" של קבצים
            }

            @Override
            public List<String> paths(String ref) {
                List<String> out = paths.get(ref);
                if (out != null) return out;
                out = new ArrayList<String>();
                Item it = item(ref);
                if (!it.gone && it.dup == null && lib != null && lib.ready)
                    for (Library.Hit h : lib.search(it.cond, false, true).hits) if (h.rec != null) out.add(h.path());
                paths.put(ref, out);
                return out;
            }
        };
    }

    /** 167: רשימה ידנית שזזה או קיבלה שם — הקישורים אליה ("m:…") בכל הרשימות החכמות עוברים איתה. */
    static int listMoved(List<String> from, List<String> to) {
        int n = 0;
        synchronized (Structure.class) {
            Ini ini = Structure.ini();
            for (Ini.Line l : new ArrayList<Ini.Line>(ini.lines)) {
                if (!KEY.equals(l.key) || l.value == null) continue;
                StringBuilder sb = new StringBuilder();
                boolean changed = false;
                for (String r : l.value.split("\\|")) {
                    String ref = r.trim();
                    if (ref.startsWith(MANUAL)) {
                        List<String> np = Tree.moved(Tree.parsePath(ref.substring(MANUAL.length())), from, to);
                        if (np != null) {
                            ref = manual(np);
                            changed = true;
                            n++;
                        }
                    }
                    if (!ref.isEmpty()) sb.append(sb.length() > 0 ? "|" : "").append(ref);
                }
                if (changed) ini.set(l, KEY, sb.toString());
            }
            if (n > 0) Structure.save(ini);
        }
        return n;
    }

    /**
     * 168: רשימות פנימיות מקומיות שאין אליהן אף הפניה (הכלי, הסביבה או הרשימה שהן היו בתוכן — נמחקו) — נמחקות.
     * הפניות: "רשימות בתוך" בכל סעיף, ו"@חכמה" בכל רשימה ידנית. מחזיר כמה נמחקו.
     */
    static int cleanup() {
        int removed = 0;
        synchronized (Structure.class) {
            synchronized (Tree.LOCK) {
                java.util.Set<String> used = new java.util.HashSet<String>();
                collect(Tree.load().root, used);
                for (boolean again = true; again; ) {
                    again = false;
                    Ini ini = Structure.ini();
                    java.util.Set<String> refs = new java.util.HashSet<String>(used);
                    String section = null, id = null;
                    List<String[]> inner = new ArrayList<String[]>();
                    for (Ini.Line l : ini.lines) {
                        if (l.isHeader()) {
                            section = l.headerName();
                            continue;
                        }
                        if (l.key == null) continue;
                        if (Structure.ID.equals(l.key) && section != null && section.startsWith(INNER)) inner.add(new String[]{l.value});
                        if (KEY.equals(l.key) && l.value != null)
                            for (String r : l.value.split("\\|")) if (!r.trim().isEmpty()) refs.add(r.trim());
                    }
                    for (String[] p : inner)
                        if (!refs.contains(p[0]) && Structure.removeById(p[0])) {
                            removed++;
                            again = true;                                     // פנימית שהייתה בתוך יתומה — בסיבוב הבא
                        }
                }
            }
        }
        if (removed > 0) Log.i("רשימות פנימיות יתומות נמחקו: " + removed);
        return removed;
    }

    private static void collect(Tree.Node n, java.util.Set<String> out) {
        out.addAll(n.smart);
        for (Tree.Node c : n.children) collect(c, out);
    }

    /** הבעלים של מסך רשימה חכמה: כלי שמקשר — ההגדרה הנפוצה (כך הפנימיות שלה בכל מקום); אחרת — הכלי עצמו. */
    static String ownerOf(Structure.Func tool) {
        if (tool == null) return null;
        String link = SmartLists.linkOf(tool.target());
        return !link.isEmpty() && SmartLists.byId(link) != null ? link : tool.id;
    }
}
