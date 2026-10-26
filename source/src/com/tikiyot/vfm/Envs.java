package com.tikiyot.vfm;

// 1.15.0: הסבת סביבות העבודה לרשימות כסביבות עבודה (המשתמש, 18.10: "אין בהם צורך — להפוך אותם לרשימות כסביבות
// עבודה"; ועל השאלה "גם הסביבות שלך?" — "להכל"). זה שלב ב של האיחוד (`claude/filosofia.md`, עיקרון 7).
// כל סביבה ← רשימה ידנית בשורש, עם הצלמית, התיאור וה"למה" שלה, ועיצוב "תוכנת ניהול קבצים"; ובבית — כלי שלה במקום
// האריח, באותו סדר. הכלים שבה ← פריטים ברשימה (הכלל של הלוח):
//   רשימה ידנית ← הפניה ↪ · רשימה חכמה ← רשימה חכמה בתוכה (הסעיף נשאר עם המזהה, ההגדרות וה"למה" — רק נהיה
//   [רשימה פנימית]; כך גם הפניות אליו ממקומות אחרים לא נשברות; 1.15.1: כך גם כפולים — לשונית של רשימה חכמה) ·
//   פקודה ← קובץ הפקודות · סל, הגדרות ← קיצור (קובץ פקודות בן שורה) · פס, אפליקציה — אין להם צורה ברשימה: לבית.
// סביבה שכל כולה כלי של רשימה אחת — הכלי בבית פותח את הרשימה עצמה (בלי רשימה עוטפת).
// רץ על כל אופני העבודה בפתיחת התוכנה, ועל האופן הפעיל בכל כניסה (גיבוי לפני). בלי ממשק — נבדק.

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class Envs {

    static final String ICON = "🧰";

    private Envs() { }

    /** האם יש בקובץ הראשי סביבות עבודה (להסבה). */
    static boolean has(Ini ini) {
        for (Ini.Line l : ini.lines) if (l.isHeader() && l.headerName().startsWith(Structure.TILE)) return true;
        return false;
    }

    /**
     * מסב בזיכרון: ini ו-tree של אופן אחד. shortcuts — תיקיית הקיצורים של האופן (null — סל והגדרות עוברים לבית).
     * מחזיר: מזהה הסביבה ← מזהה הכלי שלה בבית.
     */
    static Map<String, String> migrate(Ini ini, Tree tree, File shortcuts) {
        Map<String, String> out = new LinkedHashMap<String, String>();
        Structure.ensureIds(ini);                                             // שורה שנכתבה ביד — גם היא בפנים
        List<Structure.Tile> tiles = Structure.parse(ini).tiles;
        if (tiles.isEmpty()) return out;
        List<Ini.Line> home = new ArrayList<Ini.Line>();
        for (Structure.Tile t : tiles) {
            List<String> path = single(t, tree);
            List<Ini.Line> moved = new ArrayList<Ini.Line>();
            if (path != null) {                                               // כולה רשימה אחת — היא עצמה
                Tree.Node node = tree.find(path);
                if (node.icon.isEmpty()) node.icon = t.icon;
                if (node.desc.isEmpty()) node.desc = t.description;
                if (node.why.isEmpty()) node.why = t.why;
                take(ini, t.funcs.get(0).id);
            } else {
                Tree.Node node = Tree.newChild(tree.root, tree.root.uniqueChildName(name(t.name), null));
                node.icon = t.icon.isEmpty() ? ICON : t.icon;
                node.desc = t.description;
                node.why = t.why;
                node.design = Design.FILES;
                path = node.path();
                for (Structure.Func f : t.funcs) moved.addAll(item(ini, tree, t, f, node, path, shortcuts));
            }
            String id = Structure.newId("f");
            out.put(t.id, id);
            Map<String, String> kv = new LinkedHashMap<String, String>();
            kv.put("סוג", FnVirtual.KIND);  // תחביר
            kv.put("תיאור", t.description);  // תחביר
            kv.put(FnVirtual.LIST, Tree.key(path));
            home.addAll(block(Structure.FUNC + t.name, id, kv));               // הכלי שלה — ואחריו מה שעבר ממנה לבית
            home.addAll(moved);
            take(ini, t.id);
        }
        ini.lines.addAll(Structure.insertAt(ini), home);
        for (int i = ini.lines.size() - 1; i > 0; i--)                        // מה שיצא השאיר שורות ריקות — אחת מספיקה
            if (ini.lines.get(i).raw.trim().isEmpty() && ini.lines.get(i - 1).raw.trim().isEmpty()) ini.lines.remove(i);
        return out;
    }

    /** כלי אחד מהסביבה ← פריט ברשימה node. מחזיר את השורות שעוברות לבית (אם אין לו צורה ברשימה). */
    private static List<Ini.Line> item(Ini ini, Tree tree, Structure.Tile t, Structure.Func f, Tree.Node node,
                                       List<String> path, File shortcuts) {
        List<Ini.Line> toHome = new ArrayList<Ini.Line>();
        Target target = f.target();
        if (FnVirtual.KIND.equals(f.kind)) {
            List<String> p = FnVirtual.base(target);
            if (p.isEmpty()) p = java.util.Collections.singletonList(tree.root.uniqueChildName(name(f.name), null));
            Tree.Node list = ensure(tree, p);
            if (list.desc.isEmpty()) list.desc = f.description;               // ה"למה" והתיאור של הכלי — לרשימה עצמה
            if (list.why.isEmpty()) list.why = target.get(Program.WHY, "");
            Tree.addRef(node, path, p);
            take(ini, f.id);
        } else if (FnSmart.KIND.equals(f.kind) || FnDup.KIND.equals(f.kind)) {     // 1.15.1: גם כפולים — לשונית של רשימה חכמה
            Structure.toInner(ini, f.id, t.name + " › " + f.name);
            if (!node.smart.contains(f.id)) node.smart.add(f.id);
        } else if (FnCommand.KIND.equals(f.kind) && FnCommand.file(f) != null) {
            entry(node, FnCommand.file(f));
            take(ini, f.id);
        } else if (shortcuts != null && ("סל".equals(f.kind) || "הגדרות".equals(f.kind))) {  // תחביר
            File s = Shortcuts.write(shortcuts, "סל".equals(f.kind) ? "🗑" : "⚙", f.name,  // תחביר
                    Verbs.openScreen("סל".equals(f.kind) ? Verbs.S_TRASH : Verbs.S_SETTINGS));  // תחביר
            if (s != null) {
                entry(node, s);
                take(ini, f.id);
            } else toHome.addAll(take(ini, f.id));
        } else {
            toHome.addAll(take(ini, f.id));                                  // פס, אפליקציה — לבית
        }
        return toHome;
    }

    /** סביבה שכל כולה כלי של רשימה קיימת — המסלול שלה; אחרת null. */
    private static List<String> single(Structure.Tile t, Tree tree) {
        if (t.funcs.size() != 1 || !FnVirtual.KIND.equals(t.funcs.get(0).kind)) return null;
        List<String> p = FnVirtual.base(t.funcs.get(0).target());
        return p.isEmpty() || tree.find(p) == null ? null : p;
    }

    private static Tree.Node ensure(Tree tree, List<String> p) {
        Tree.Node n = tree.root;
        for (String s : p) {
            Tree.Node c = n.child(s);
            n = c != null ? c : Tree.newChild(n, s);
        }
        return n;
    }

    private static void entry(Tree.Node node, File f) {
        String p = f.getAbsolutePath();
        if (!Tree.contains(node.entries, p)) node.entries.add(new Tree.Entry(p, f.length(), false));
    }

    private static String name(String s) {
        String n = Tree.cleanName(s);
        return n.isEmpty() ? Words.LIST : n;
    }

    /** הסעיף — מוצא מהקובץ, ומוחזר (כדי להכניס אותו במקום אחר). */
    private static List<Ini.Line> take(Ini ini, String id) {
        int at = Structure.indexOfId(ini, id);
        List<Ini.Line> out = new ArrayList<Ini.Line>();
        if (at < 0) return out;
        int end = ini.sectionEnd(at), last = end;
        while (last > at + 1 && ini.lines.get(last - 1).raw.trim().isEmpty()) last--;
        out.add(Ini.line("", null));
        out.addAll(ini.lines.subList(at, last));                              // בלי השורות הריקות שאחריו
        for (int i = end - 1; i >= at; i--) ini.lines.remove(i);              // והן יוצאות איתו
        return out;
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

    // ---------- בכונן ----------

    /** כל אופני העבודה (בפתיחת התוכנה). מחזיר כמה סביבות הוסבו. */
    static int all() {
        int n = 0;
        for (String m : Modes.all()) n += mode(m);
        return n;
    }

    /** אופן אחד, בקבצים שלו — עם גיבוי לפני. עץ שלא נקרא — לא נוגעים (כדי לא לדרוס את הרשימות). */
    static synchronized int mode(String mode) {
        try {
            synchronized (Structure.class) {
                synchronized (Tree.LOCK) {
                    File main = ModeCopy.mainOf(mode), treeFile = ModeCopy.treeOf(mode);
                    if (!main.isFile()) return 0;
                    Ini ini = Ini.load(main);
                    if (!has(ini)) return 0;
                    Tree tree = Tree.loadFrom(treeFile);
                    if (tree.unreadable) return 0;
                    Files.backupNamed(main, Words.EnvsW.BACKUP + mode);
                    Files.backupNamed(treeFile, Words.EnvsW.BACKUP + mode);
                    Map<String, String> done = migrate(ini, tree, new File(main.getParentFile(), Shortcuts.DIR));
                    if (!tree.saveTo(treeFile) || !ini.saveQuiet()) return 0;
                    if (mode.equals(Modes.current())) Mirror.rebuild();
                    Log.i("סביבות עבודה ← רשימות (" + mode + "): " + done.size());
                    return done.size();
                }
            }
        } catch (Throwable t) {
            Log.e("הסבת הסביבות — " + mode, t);
            return 0;
        }
    }
}
