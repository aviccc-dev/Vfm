package com.tikiyot.vfm;

// העתקה בין אופני עבודה (162) — בלי ממשק (נבדק). המשתמש: "כאשר אני בעבודה אמיתית ורוצה להביא לכאן משהו
// מהדוגמאות — אני בוחר 'הוסף', ומנווט… עד שמגיע לרשימה או תת־רשימה — לחיצה ארוכה. אבל אפשר משני הכיוונים."
// מעתיקים: כלי · רשימה ידנית או תת־רשימה (ככלי חדש) · רשימה חכמה — ואיתם כל מה שהם צריכים:
// הרשימות הידניות שלהם, ההגדרות הנפוצות והרשימות הפנימיות שהם מקשרים אליהן (גם לעומק). הכול — עם מזהים חדשים;
// שם שכבר תפוס ביעד — מקבל " (2)". המקור לא משתנה.

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ModeCopy {

    /** מה בוחרים להעתיק. */
    static final class Pick {
        final int kind;
        final String id;
        final List<String> path;
        final String name;

        private Pick(int kind, String id, List<String> path, String name) {
            this.kind = kind;
            this.id = id;
            this.path = path;
            this.name = name;
        }

        static final int TOOL = 1, LIST = 2, SMART = 3;

        /** 1.10.0: רשימה חכמה פנימית (לפי המזהה שלה) — לשחזור מהסל אל תוך רשימה. */
        static Pick smart(String ref, String name) {
            return new Pick(SMART, ref, null, name);
        }

        static Pick tool(String id, String name) {
            return new Pick(TOOL, id, null, name);
        }

        static Pick list(List<String> path) {
            return new Pick(LIST, null, path, path.get(path.size() - 1));
        }
    }

    /** הקבצים של אופן אחד: הקובץ הראשי והרשימות. */
    static final class Side {
        final Ini ini;
        final Tree tree;

        Side(Ini ini, Tree tree) {
            this.ini = ini;
            this.tree = tree;
        }
    }

    static File mainOf(String mode) {
        return new File(new File(Modes.dirOf(mode), Paths.SETTINGS), Paths.MAIN);
    }

    static File treeOf(String mode) {
        return new File(Modes.dirOf(mode), Paths.TREE);
    }

    static Side read(String mode) {
        File m = mainOf(mode);
        Ini.createIfMissing(m, "");
        Ini ini = Ini.load(m);
        return new Side(ini, Tree.loadFrom(treeOf(mode)));
    }

    private final Side src, dst;
    /**
     * 1.13.1: המקור והיעד — אותו אופן עבודה. אז הקישורים נשארים קישורים: הפניה לרשימה ידנית, הגדרה נפוצה וכלי —
     * מצביעים על אותו דבר (כמו "גם לרשימה"), ורק מה ששייך לרשימה עצמה (רשימה חכמה פנימית) — משוכפל.
     */
    private final boolean same;
    /** 1.15.0: מה שכבר קיים ביעד (אותו מזהה, אותו מסלול) — מקושר, ורק השאר מועתק ("הדוגמאות שלי" בחזרה לברירת מחדל). */
    private boolean prefer;
    /** מזהה במקור ← מזהה ביעד (כך מה שמקושר פעמיים — מועתק פעם אחת). */
    private final Map<String, String> ids = new HashMap<String, String>();

    private ModeCopy(Side src, Side dst, boolean same) {
        this.src = src;
        this.dst = dst;
        this.same = same;
    }

    // ---------- הכניסה ----------

    /** מעתיק מ-src אל מסך הבית של dst. מחזיר את השם שנוצר ביעד, או null — לא נמצא במקור. */
    static String copy(Side src, Pick pick, Side dst) {
        return copy(src, pick, dst, false);
    }

    static String copy(Side src, Pick pick, Side dst, boolean same) {
        return new ModeCopy(src, dst, same).run(pick);
    }

    /** אל האופן הפעיל — בנעילות של הקובץ הראשי ושל מבנה.txt, ושמירה. */
    static String intoActive(String srcMode, final Pick pick) {
        return active(srcMode, new Op() {
            @Override public String run(ModeCopy c, Side dst) { return c.run(pick); }
        });
    }

    /** מה עושים עם המעתיק כשהיעד הוא האופן הפעיל. */
    private interface Op {
        String run(ModeCopy c, Side dst);
    }

    /** 1.14.0: המסגרת האחת של "אל האופן הפעיל" (עד כאן — כתובה פעמיים): נעילות, קריאה, ושמירה אם נעשה משהו. */
    private static String active(String srcMode, Op op) {
        boolean same = srcMode.equals(Modes.current());
        synchronized (Structure.class) {
            synchronized (Tree.LOCK) {
                Side dst = new Side(Structure.ini(), Tree.load());
                Side src = same ? new Side(Ini.load(Structure.file()), Tree.load()) : read(srcMode);
                String made = op.run(new ModeCopy(src, dst, same), dst);
                if (made != null) {
                    dst.tree.save();
                    Structure.save(dst.ini);
                }
                return made;
            }
        }
    }

    /**
     * 1.10.0: שחזור מסל המחזור — מהצילום שנשמר ברגע ההסרה (src) אל האופן dstMode. into — הרשימה שבתוכה הייתה
     * (אם עדיין קיימת; אחרת — כלי חדש במסך הבית). מחזיר שם, או null.
     */
    static String restore(Side src, Pick pick, String dstMode, List<String> into) {
        boolean active = dstMode.equals(Modes.current());
        synchronized (Structure.class) {
            synchronized (Tree.LOCK) {
                Side dst = active ? new Side(Structure.ini(), Tree.load()) : read(dstMode);
                ModeCopy c = new ModeCopy(src, dst, false);                 // צילום מהסל — הכול מועתק
                Tree.Node node = into == null ? null : dst.tree.find(into);
                String made;
                if (pick.kind == Pick.SMART) {
                    String id = node == null ? null : c.copyRef(pick.id);
                    if (id != null && !node.smart.contains(id)) node.smart.add(id);
                    made = id == null ? null : pick.name;
                } else {
                    made = node != null ? c.intoNode(pick, node) : c.run(pick);
                }
                if (made != null) {
                    if (active) {
                        dst.tree.save();
                        Structure.save(dst.ini);
                    } else {
                        dst.tree.saveTo(treeOf(dstMode));
                        dst.ini.saveQuiet();
                    }
                }
                return made;
            }
        }
    }

    /**
     * 1.15.0: הרשימה path מ-src אל אותו מקום ב-dst — מחליפה את התוכן שלה שם (הצלמית, התיאור וה"למה" של היעד נשארים).
     * הפניות למה שכבר קיים ביעד — נשארות קישור; השאר מועתק. false — אין רשימה כזו במקור.
     */
    static boolean keep(Side src, List<String> path, Side dst) {
        Tree.Node from = src.tree.find(path);
        if (from == null) return false;
        Tree.Node to = dst.tree.root;
        for (String p : path) {
            Tree.Node c = to.child(p);
            to = c != null ? c : Tree.newChild(to, p);
        }
        to.entries.clear();
        to.children.clear();
        to.smart.clear();
        to.refs.clear();
        to.live.clear();
        ModeCopy c = new ModeCopy(src, dst, false);
        c.prefer = true;
        c.fill(from, to);
        return true;
    }

    // ---------- 164: אל תוך רשימה ----------

    /**
     * 164: אל תוך רשימה ידנית באופן הפעיל (destPath). רשימה ידנית (או כלי שלה) — נכנסת כתת־רשימה, עם כל מה שבתוכה;
     * רשימה חכמה (כלי) — נכנסת כרשימה חכמה בתוך הרשימה. מחזיר את השם, או null.
     */
    static String intoList(String srcMode, final Pick pick, final List<String> destPath) {
        return active(srcMode, new Op() {
            @Override
            public String run(ModeCopy c, Side dst) {
                Tree.Node into = dst.tree.find(destPath);
                return into == null ? null : c.intoNode(pick, into);
            }
        });
    }

    /** ידנית — תת־רשימה של into; חכמה — רשימה חכמה בתוך into. */
    private String intoNode(Pick pick, Tree.Node into) {
        if (pick.kind == Pick.SMART) {                                        // 1.14.0: רשימה חכמה מתוך רשימה (הלוח)
            String id = copyRef(pick.id);
            if (id == null) return null;
            if (!into.smart.contains(id)) into.smart.add(id);
            return pick.name;
        }
        List<String> path = manualPath(pick);
        if (path != null) {
            Tree.Node from = src.tree.find(path);
            if (from == null) return null;
            Tree.Node to = Tree.newChild(into, into.uniqueChildName(from.name, null));
            fill(from, to);
            return to.name;
        }
        String[] ref = asRef(pick);
        if (ref == null) return null;
        if (!into.smart.contains(ref[0])) into.smart.add(ref[0]);
        return ref[1];
    }

    /** הרשימה הידנית שבבחירה (רשימה, או כלי של רשימה ידנית), או null. */
    private List<String> manualPath(Pick pick) {
        if (pick.kind == Pick.LIST) return pick.path;
        Map<String, String> kv = keys(src.ini, pick.id);
        if (FnVirtual.KIND.equals(kv.get("סוג")) && kv.get(FnVirtual.LIST) != null) return Tree.parsePath(kv.get(FnVirtual.LIST));   // 165 // תחביר
        return null;
    }

    /**
     * הבחירה כהפניה ביעד: { הפניה, שם }. ידנית — "m:" לרשימה שהועתקה לשורש; רשימה חכמה מקושרת — ההגדרה הנפוצה
     * (מועתקת); רשימה חכמה מקומית — רשימה פנימית חדשה עם ההגדרות שלה.
     */
    private String[] asRef(Pick pick) {
        List<String> path = manualPath(pick);
        if (path != null) {
            String list = copyList(path);
            return list == null ? null : new String[]{Nested.MANUAL + list, list};
        }
        Map<String, String> kv = keys(src.ini, pick.id);
        boolean dup = FnDup.KIND.equals(kv.get("סוג"));                               // תחביר
        if (!dup && !FnSmart.KIND.equals(kv.get("סוג"))) return null;                // תחביר
        String link = kv.get(SmartLists.LINK);
        if (!dup && link != null && !link.isEmpty()) {
            String id = copyRef(link);
            return id == null ? null : new String[]{id, pick.name};
        }
        return new String[]{newInner(kv, pick.name, null), pick.name};
    }

    /**
     * 1.14.0: רשימה פנימית חדשה ביעד, עם ההגדרות מ-kv (עד כאן — אותו קוד בשלושה מקומות); ref — לזכור.
     * 1.15.1 (באג): עם כל המפתחות — גם "למה", הכפתורים הדולקים והקישור להגדרה נפוצה (עד כאן — רק תנאי, מיון ותצוגה),
     * וגם כפולים.
     */
    private String newInner(Map<String, String> kv, String name, String ref) {
        Map<String, String> inner = new LinkedHashMap<String, String>(kv);
        String id = Structure.newId("p");
        if (ref != null) ids.put(ref, id);                                    // לפני הקישורים — כך סיבוב לא חוזר
        remapLinks(inner);
        dst.ini.lines.addAll(firstHeader(Nested.INNER), block(Nested.INNER + freeName(Nested.INNER, name), id, inner));
        return id;
    }

    private String run(Pick pick) {
        if (pick.kind == Pick.TOOL) return copyTool(pick.id);
        if (pick.kind == Pick.SMART) return smartTool(pick);
        String list = copyList(pick.path);
        if (list == null) return null;
        Map<String, String> kv = new LinkedHashMap<String, String>();
        kv.put("סוג", FnVirtual.KIND);  // תחביר
        kv.put(FnVirtual.LIST, list);
        return addTool(list, kv);
    }

    /**
     * 1.14.0: רשימה חכמה מתוך רשימה ידנית — לבית באופן אחר (הלוח): כלי חכם. הגדרה נפוצה — מועתקת
     * ומקושרת; כלי או רשימה פנימית — ההגדרות שלהם בכלי.
     */
    private String smartTool(Pick pick) {
        String[] h = header(src.ini, pick.id);
        if (h == null) return null;
        Map<String, String> kv = new LinkedHashMap<String, String>();
        if (h[0].startsWith(SmartLists.SECTION)) {
            String id = copyRef(pick.id);
            if (id == null) return null;
            kv.put("סוג", FnSmart.KIND);  // תחביר
            kv.put(SmartLists.LINK, id);
        } else {
            kv.putAll(keys(src.ini, pick.id));                                // 1.15.1: כל המפתחות (גם כפולים)
            if (!FnDup.KIND.equals(kv.get("סוג"))) kv.put("סוג", FnSmart.KIND);  // תחביר
            remapLinks(kv);
        }
        return addTool(pick.name, kv);
    }

    // ---------- סעיפים בקובץ הראשי ----------

    /** הכותרת והמפתחות של סעיף (לפי מזהה) — בלי המזהה. */
    private static String[] header(Ini ini, String id) {
        Structure.Func f = Structure.byId(ini, id);
        return f == null ? null : new String[]{f.name};
    }

    private static Map<String, String> keys(Ini ini, String id) {
        Structure.Func f = Structure.byId(ini, id);
        return f == null ? new LinkedHashMap<String, String>() : new LinkedHashMap<String, String>(f.params);
    }

    private static List<Ini.Line> block(String section, String id, Map<String, String> kv) {
        List<Ini.Line> b = new ArrayList<Ini.Line>();
        b.add(Ini.line("", section));
        b.add(Ini.line("[" + section + "]", section));
        b.add(Ini.line(Structure.ID + " = " + id, section));
        for (Map.Entry<String, String> e : kv.entrySet())
            if (e.getValue() != null && !e.getValue().isEmpty()) b.add(Ini.line(e.getKey() + " = " + e.getValue(), section));
        return b;
    }

    /** שם פנוי ביעד לסעיף מהסוג הזה ("שם", "שם (2)", …). */
    private String freeName(String prefix, String name) {
        String n = name;
        for (int i = 2; hasHeader(prefix + n); i++) n = name + " (" + i + ")";
        return n;
    }

    private boolean hasHeader(String h) {
        for (Ini.Line l : dst.ini.lines) if (l.isHeader() && l.headerName().trim().equals(h.trim())) return true;
        return false;
    }

    private int firstHeader(String exceptPrefix) {
        for (int i = 0; i < dst.ini.lines.size(); i++) {
            Ini.Line l = dst.ini.lines.get(i);
            if (l.isHeader() && (exceptPrefix == null || !l.headerName().startsWith(exceptPrefix))) return i;
        }
        return dst.ini.lines.size();
    }

    // ---------- מה שמועתק ----------

    private String copyTool(String toolId) {
        String[] h = header(src.ini, toolId);
        if (h == null || !h[0].startsWith(Structure.FUNC)) return null;
        Map<String, String> kv = keys(src.ini, toolId);
        if (FnVirtual.KIND.equals(kv.get("סוג")) && kv.get(FnVirtual.LIST) != null) {  // תחביר
            String list = copyList(Tree.parsePath(kv.get(FnVirtual.LIST)));         // 165: גם תת־רשימה ("א/ב")
            if (list != null) kv.put(FnVirtual.LIST, list);
            else kv.remove(FnVirtual.LIST);                                          // 166: לא נמצאה — לא להיצמד לרשימה זרה
        }
        remapLinks(kv);
        return addTool(h[0].substring(Structure.FUNC.length()).trim(), kv);
    }

    private String addTool(String name, Map<String, String> kv) {
        dst.ini.lines.addAll(Structure.insertAt(dst.ini), block(Structure.FUNC + name, Structure.newId("f"), kv));
        return name;
    }

    /** הקישורים שבסעיף: הגדרה נפוצה, ורשימות בתוך — מועתקים, והערך מקבל את המזהים החדשים. */
    private void remapLinks(Map<String, String> kv) {
        String v = kv.get(SmartLists.LINK);
        if (v != null && !v.isEmpty()) {
            String n = copyRef(v);
            kv.remove(SmartLists.LINK);
            if (n != null) kv.put(SmartLists.LINK, n);
        }
        String in = kv.get(Nested.KEY);
        if (in != null && !in.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String r : in.split("\\|")) {
                String n = copyRef(r.trim());
                if (n != null) sb.append(sb.length() > 0 ? "|" : "").append(n);
            }
            kv.put(Nested.KEY, sb.toString());
        }
    }

    /** הפניה (הגדרה נפוצה, רשימה פנימית, רשימה ידנית) — מועתקת פעם אחת; מחזיר את ההפניה ביעד. */
    private String copyRef(String ref) {
        if (ref == null || ref.isEmpty()) return null;
        if (ids.containsKey(ref)) return ids.get(ref);
        if (prefer && (ref.startsWith(Nested.MANUAL) ? dst.tree.find(Tree.parsePath(ref.substring(Nested.MANUAL.length()))) != null
                : header(dst.ini, ref) != null)) return ref;
        String[] h0 = header(src.ini, ref);
        boolean tool = h0 != null && h0[0].startsWith(Structure.FUNC);
        if (same && (ref.startsWith(Nested.MANUAL) || tool || h0 != null && h0[0].startsWith(SmartLists.SECTION)))
            return ref;                                                       // 1.13.1: באותו אופן — קישור נשאר קישור
        if (tool) return toolAsInner(ref, h0[0].substring(Structure.FUNC.length()).trim());
        if (ref.startsWith(Nested.MANUAL)) {
            String list = copyList(Tree.parsePath(ref.substring(Nested.MANUAL.length())));   // 165
            String out = list == null ? null : Nested.MANUAL + list;
            ids.put(ref, out);
            return out;
        }
        String[] h = header(src.ini, ref);
        if (h == null) return null;
        String prefix = h[0].startsWith(SmartLists.SECTION) ? SmartLists.SECTION
                : h[0].startsWith(Nested.INNER) ? Nested.INNER : null;
        if (prefix == null) return null;
        String now = prefix.equals(Nested.INNER) ? Nested.INNER : SmartLists.SECTION;
        String id = Structure.newId(now.equals(Nested.INNER) ? "p" : "s");
        ids.put(ref, id);                                                     // לפני הרקורסיה — כך סיבוב לא חוזר על עצמו
        Map<String, String> kv = keys(src.ini, ref);
        remapLinks(kv);
        String name = freeName(now, h[0].substring(prefix.length()).trim());
        int at = firstHeader(now);
        List<Ini.Line> b = block(now + name, id, kv);
        dst.ini.lines.addAll(at, b);
        return id;
    }

    /**
     * 1.13.1 (באג): רשימה חכמה שהיא כלי ("@חכמה f-…") — לאופן אחר אי אפשר לקשר לכלי שאין שם; מועתקת כרשימה
     * פנימית עם ההגדרות שלו (עד כאן — נשמטה).
     */
    private String toolAsInner(String ref, String name) {
        return newInner(keys(src.ini, ref), name, ref);
    }

    /** רשימה ידנית (עם כל מה שבתוכה) — לשורש היעד, בשם פנוי; מחזיר את השם. */
    private String copyList(List<String> path) {
        Tree.Node from = src.tree.find(path);
        if (from == null) return null;
        String name = dst.tree.root.uniqueChildName(from.name, null);
        Tree.Node to = Tree.newChild(dst.tree.root, name);
        fill(from, to);
        return name;
    }

    private void fill(Tree.Node from, Tree.Node to) {
        Tree.copyNode(from, to, new Tree.Links() {                           // 1.13.1 (באג): גם הפניות, צלמית, למה…
            @Override public String smart(String ref) { return copyRef(ref); }
            @Override public String list(String path) {
                if (same) return path;
                String r = copyRef(Nested.MANUAL + path);                     // לאופן אחר — גם הרשימה שאליה מפנים
                return r == null ? null : r.substring(Nested.MANUAL.length());
            }
        });
    }

    /** המודל של קובץ ראשי כלשהו. */
    static Structure.Model modelOf(Ini ini) {
        return Structure.parse(ini);
    }
}
