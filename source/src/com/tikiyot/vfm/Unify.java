package com.tikiyot.vfm;

// 1.17.3: שני איחודים לפי המילון (`claude/milon.md`), בכל אופני העבודה (בפתיחת התוכנה, עם גיבוי):
//   (4) "תיקייה מתעדכנת" יוצאת כסוג נפרד — המשתמש: "זו גם סוג של רשימה חכמה"; "בחלק הוירטואלי אין כזה דבר ישר מהכונן —
//       זה רק בחלק הפיזי". כל תיקייה מתעדכנת ← רשימה חכמה בתוך הרשימה: התנאי — התיקייה (מקור:"…"), בתצוגת תיקיות.
//   (20) "הגדרה נפוצה" ← "תבנית רשימה חכמה" — המשתמש: "רק התבנית משתנה". רשימה חכמה שמקושרת להגדרה נפוצה — מקבלת
//       את ההגדרות לעצמה (בלי הקישור); הגדרה נפוצה שנמצאת ישר בתוך רשימה — עותק משלה. התבניות נשארות — לטעינה.
// אחראי: ההסבה (בלי ממשק — נבדק), וההוספה החדשה של תיקייה כרשימה חכמה.

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class Unify {

    private Unify() { }

    /** ההסבה באופן אחד, בזיכרון. מחזיר true אם השתנה משהו. */
    static boolean run(ModeCopy.Side s) {
        boolean ch = unlink(s.ini);
        ch |= walk(s, s.tree.root);
        return ch;
    }

    private static boolean walk(ModeCopy.Side s, Tree.Node n) {
        boolean ch = false;
        String owner = n.parent == null ? Words.MANUAL_LISTS : Tree.label(n.path());
        for (String lp : new ArrayList<String>(n.live)) {                     // תיקייה מתעדכנת ← רשימה חכמה
            String id = inner(s.ini, owner + " › " + new File(lp).getName(), folderKeys(lp));
            if (!n.smart.contains(id)) n.smart.add(id);
            rekey(n, "l" + lp, "s" + id);
            ch = true;
        }
        n.live.clear();
        for (int i = 0; i < n.smart.size(); i++) {                            // הגדרה נפוצה ישר ברשימה ← עותק משלה
            String r = n.smart.get(i);
            Structure.Func f = Structure.byId(s.ini, r);
            if (f == null || !f.name.startsWith(SmartLists.SECTION)) continue;
            Map<String, String> kv = new LinkedHashMap<String, String>();
            kv.put("סוג", FnSmart.KIND);  // תחביר
            for (Map.Entry<String, String> e : f.params.entrySet())
                if (!e.getKey().equals("שם")) kv.put(e.getKey(), e.getValue());  // תחביר
            String id = inner(s.ini, owner + " › " + f.name.substring(SmartLists.SECTION.length()), kv);
            n.smart.set(i, id);
            rekey(n, "s" + r, "s" + id);
            ch = true;
        }
        for (Tree.Node c : n.children) ch |= walk(s, c);
        return ch;
    }

    private static void rekey(Tree.Node n, String from, String to) {
        int at = n.order.indexOf(from);
        if (at >= 0) n.order.set(at, to);
    }

    /** רשימה חכמה של תיקייה: מה שבתיקייה (ובתוכה), בתצוגת תיקיות, לפי שם. */
    static Map<String, String> folderKeys(String folder) {
        Map<String, String> kv = new LinkedHashMap<String, String>();
        kv.put("סוג", FnSmart.KIND);  // תחביר
        kv.put(SmartLists.COND, Cond.SOURCE + ":" + Cond.quote(folder));
        kv.put(SmartLists.SORT, Sort.NAME);
        kv.put(SmartLists.VIEW, FolderView.ALL);
        return kv;
    }

    /** כל סעיף שמקושר להגדרה נפוצה — מקבל את ההגדרות שלה לעצמו, והקישור יוצא. */
    private static boolean unlink(Ini ini) {
        boolean ch = false;
        for (int i = 0; i < ini.lines.size(); i++) {
            Ini.Line l = ini.lines.get(i);
            if (!l.isHeader() || l.headerName().startsWith(SmartLists.SECTION)) continue;
            int end = ini.sectionEnd(i), link = -1;
            java.util.Set<String> has = new java.util.HashSet<String>();
            for (int k = i + 1; k < end; k++) {
                Ini.Line x = ini.lines.get(k);
                if (x.key == null) continue;
                has.add(x.key);
                if (x.key.equals(SmartLists.LINK)) link = k;
            }
            if (link < 0) continue;
            Structure.Func common = Structure.byId(ini, ini.lines.get(link).value);
            String header = l.headerName();
            ini.lines.remove(link);
            if (common != null) {
                int at = link;
                for (String key : new String[]{SmartLists.COND, SmartLists.SORT, SmartLists.VIEW, SmartLists.DIR}) {
                    String v = common.params.get(key);
                    if (v == null || v.isEmpty() || has.contains(key)) continue;
                    ini.lines.add(at++, Ini.line(key + " = " + v, header));
                }
            }
            ch = true;
        }
        return ch;
    }

    /** רשימה פנימית חדשה בקובץ הזה; מחזיר את המזהה. */
    private static String inner(Ini ini, String label, Map<String, String> kv) {
        String h = Nested.INNER + label, base = h;
        for (int n = 2; taken(ini, h); n++) h = base + " (" + n + ")";
        String id = Structure.newId("p");
        List<Ini.Line> b = new ArrayList<Ini.Line>();
        b.add(Ini.line("", h));
        b.add(Ini.line("[" + h + "]", h));
        b.add(Ini.line(Structure.ID + " = " + id, h));
        for (Map.Entry<String, String> e : kv.entrySet()) b.add(Ini.line(e.getKey() + " = " + e.getValue(), h));
        ini.lines.addAll(Structure.insertAt(ini), b);
        return id;
    }

    private static boolean taken(Ini ini, String h) {
        for (Ini.Line l : ini.lines) if (l.isHeader() && l.headerName().trim().equals(h.trim())) return true;
        return false;
    }

    // ---------- בכונן ----------

    /** כל האופנים (בפתיחת התוכנה). */
    static void all() {
        for (String m : Modes.all()) mode(m);
    }

    static void mode(String mode) {
        try {
            synchronized (Structure.class) {
                synchronized (Tree.LOCK) {
                    File main = ModeCopy.mainOf(mode), tf = ModeCopy.treeOf(mode);
                    if (!main.isFile() || !tf.isFile()) return;
                    ModeCopy.Side s = new ModeCopy.Side(Ini.load(main), Tree.loadFrom(tf));
                    if (s.tree.unreadable || !run(s)) return;
                    Files.backupNamed(main, Words.EnvsW.UNIFY + mode);
                    Files.backupNamed(tf, Words.EnvsW.UNIFY + mode);
                    if (s.ini.saveQuiet()) {
                        s.tree.saveTo(tf);
                        if (mode.equals(Modes.current())) Mirror.rebuild();
                    }
                }
            }
        } catch (Throwable t) {
            Log.e("איחוד — " + mode, t);
        }
    }

    /** 1.17.3: "➕ תיקייה" לרשימה — רשימה חכמה של כל תיקייה (במקום "תיקייה מתעדכנת"). מחזיר כמה נוספו, או ‎-1. */
    static int addFolders(List<String> listPath, List<String> folders) {
        if (Tree.load().find(listPath) == null) return -1;
        final List<String> ids = new ArrayList<String>();
        for (String f : folders) {
            Map<String, String> kv = folderKeys(f);
            ids.add(Nested.newInner(Tree.label(listPath), new File(f).getName(), kv.get(SmartLists.COND),
                    kv.get(SmartLists.SORT), kv.get(SmartLists.VIEW)));
        }
        final List<String> p = listPath;
        final int[] n = {0};
        Tree.edit(new Tree.Edit() {
            @Override
            public boolean apply(Tree t) {
                Tree.Node node = t.find(p);
                if (node == null) return false;
                for (String id : ids) if (!node.smart.contains(id)) {
                    node.smart.add(id);
                    n[0]++;
                }
                return n[0] > 0;
            }
        });
        return n[0];
    }
}
