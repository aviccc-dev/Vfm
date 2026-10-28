package com.tikiyot.vfm;

// 1.14.0: הלוח הווירטואלי — "העתקה" / "העברה" על כל דבר, ומנווטים ו"הדבקה כאן" בכל מקום (המשתמש, 18.10: "או לחזור
// לפשטות — העתקה לקליפ, ומשם לאיפה שרוצים בהדבקה"; "הכי פשוט — הכי טוב").
// מה נכנס ללוח: קובץ ברשימה · רשימה ידנית · רשימה חכמה בתוך רשימה · הפניה ↪ · תיקייה מתעדכנת · כלי.
// לאן מדביקים: לתוך רשימה ידנית, למסך הבית — גם באופן עבודה אחר (הלוח זוכר מאיזה אופן).
// הכלל (המשתמש: "העתקת רשימה יכולה להיעשות — או העתקת כל הרשימה, ואז היא פיזי שבווירטואלי; או רק קישור לרשימה,
// ואז זה וירטואלי"):
//   (1.16.0, המילון) קישור — רשימה נהיית הפניה ↪, כלי — עוד כלי לאותה רשימה; הקובץ בכונן לא זז.
//   העתקה (full) — עותק עצמאי עם כל מה שבו (גם הרשימות החכמות שבתוכו).
//   העברה — הדבר עצמו עובר (ויוצא מהמקום הקודם).
//   לאופן אחר — אין קישור בין אופנים, ולכן תמיד עותק מלא (ModeCopy), והעברה — עותק ויציאה מהמקור.
// אחראי: הלוח וההדבקה (בלי ממשק — נבדק). הממשק: VirtualScreen, Home, ToolMenu (PasteBar).

import java.io.File;
import java.util.ArrayList;
import java.util.List;

final class VClip {

    static final int FILE = 0, LIST = 1, SMART = 2, REF = 3, LIVE = 4, TOOL = 5;

    /** פריט בלוח: מה הוא, והערך שלו (מסלול קובץ / שם רשימה / מזהה / מסלול הפניה / תיקייה / מזהה כלי או סביבה). */
    static final class Item {
        final int kind;
        final String value, name;

        Item(int kind, String value, String name) {
            this.kind = kind;
            this.value = value;
            this.name = name;
        }
    }

    /** תוצאת הדבקה: כמה נכנסו, וכמה לא היה אפשר (הסבר אחד). */
    static final class Result {
        int done, skipped;
        String why;

        void skip(String w) {
            skipped++;
            if (why == null) why = w;
        }
    }

    private static final List<Item> items = new ArrayList<Item>();
    private static String mode;
    /** הרשימה שממנה (לקובץ, רשימה, חכמה, הפניה, תיקייה), או null. */
    private static List<String> from;
    private static boolean move, full;

    private VClip() { }

    static synchronized void set(String fromMode, List<String> fromList, List<Item> what, boolean isMove, boolean isFull) {
        items.clear();
        items.addAll(what);
        mode = fromMode;
        from = fromList == null ? null : new ArrayList<String>(fromList);
        move = isMove && !isFull;
        full = isFull;
    }

    static synchronized void clear() {
        items.clear();
        from = null;
        mode = null;
    }

    static synchronized boolean empty() {
        return items.isEmpty();
    }

    static synchronized List<Item> items() {
        return new ArrayList<Item>(items);
    }

    /** "📋 2 פריטים · רשימה אחת — להעתקה (קישור)". */
    static synchronized String label() {
        int lists = 0, other = 0;
        for (Item it : items) if (it.kind == LIST || it.kind == REF) lists++; else other++;
        StringBuilder sb = new StringBuilder();
        if (other > 0) sb.append(Words.items(other));
        if (lists > 0) sb.append(sb.length() > 0 ? Words.ClipW.AND : "").append(Words.lists(lists));
        if (items.size() == 1) sb = new StringBuilder("\"" + items.get(0).name + "\"");
        String how = full ? Words.VClipW.TO_DUPLICATE : move ? Words.VClipW.TO_MOVE : Words.VClipW.TO_COPY;
        String where = mode != null && !mode.equals(Modes.current()) ? Words.VClipW.FROM_MODE + mode : "";
        return sb + how + where;
    }

    private static boolean sameMode() {
        return mode == null || mode.equals(Modes.current());
    }

    // ---------- לתוך רשימה ידנית ----------

    /** האם אפשר להדביק לתוך הרשימה הזו. null — אפשר. */
    static synchronized String cannotPasteInto(List<String> dest) {
        if (items.isEmpty()) return Words.ClipW.PICK_LIST_FIRST;
        if (!sameMode()) return null;
        boolean any = false;
        for (Item it : items) {
            if (it.kind == LIST && from != null && Tree.inside(dest, child(from, it.value))) continue;
            if (move && from != null && from.equals(dest) && it.kind != TOOL) continue;
            any = true;
        }
        return any ? null : Words.VClipW.NOT_HERE;
    }

    /** מדביק לתוך הרשימה dest באופן הפעיל. */
    static synchronized Result pasteInto(final List<String> dest) {
        final Result r = new Result();
        if (items.isEmpty()) return r;
        Structure.batch(new Runnable() {                                       // 1.23.1: שמירה אחת לכל ההדבקה
            @Override
            public void run() {
                if (sameMode()) pasteIntoSame(dest, r);
                else pasteIntoOther(dest, r);
            }
        });
        if (r.done > 0) clear();
        return r;
    }

    /** הפריטים כ"קישורים" לעץ: כלים מתורגמים לרשימה / חכמה / קובץ שהם מחזיקים. */
    private static void pasteIntoSame(final List<String> dest, final Result r) {
        final List<Item> tree = new ArrayList<Item>();
        final java.util.Map<Item, Structure.Func> tools = new java.util.HashMap<Item, Structure.Func>();
        final List<Item> ok = new ArrayList<Item>();
        for (Item it : items) {
            if (it.kind != TOOL) {
                tree.add(it);
                continue;
            }
            Structure.Func f = Structure.func(it.value);
            Item as = f == null ? null : toolAsItem(f);
            if (as == null) {
                r.skip(Words.VClipW.TOOL_NOT_IN_LIST);
                continue;
            }
            tree.add(as);
            tools.put(as, f);
        }
        final List<String[]> moved = new ArrayList<String[]>();
        final List<String> src = from;
        final boolean mv = move, fl = full;
        final java.util.Map<String, String> copies = fl ? innerCopies(tree, src, dest) : null;   // 1.16.0
        Tree.edit(new Tree.Edit() {
            @Override
            public boolean apply(Tree t) {
                Tree.Node a = src == null ? null : t.find(src), b = t.find(dest);
                if (b == null) return false;
                boolean changed = false;
                for (Item it : tree) {
                    if (intoNode(t, it, a, b, dest, src, mv, fl, moved, copies)) {
                        r.done++;
                        ok.add(it);
                        changed = true;
                    } else r.skip(Words.VClipW.NOT_HERE);
                }
                return changed;
            }
        });
        // 1.23.3 (באג): עותק שלא נכנס — לא נשאר יתום. עד כאן — רק רשימה חכמה בודדת; רשימה שלא נכנסה (למשל הרשימה
        // שמדביקים לתוכה, כשגם הכלי שלה נבחר) השאירה את כל העותקים של החכמות שבה — יתומות, שנמחקו אחר כך אחת אחת
        // בחזרה לבית ("האפליקציה לא מגיבה"). עכשיו — כל עותק שאינו בשימוש, בשמירה האחת של ההדבקה.
        if (copies != null && !copies.isEmpty()) Nested.cleanup();
        for (String[] mvd : moved) FnVirtual.listMoved(child(src, mvd[0]), child(dest, mvd[1]));   // 88: הכלים עוברים איתה
        if (move) for (Item it : ok) {                                         // 1.15.1 (באג): רק מה שנכנס — יוצא מהבית
            Structure.Func f = tools.get(it);
            if (f == null) continue;
            if (it.kind == SMART && it.value.equals(f.id)) Structure.toInner(f.id, Tree.label(dest) + " › " + f.name);
            else Structure.removeFunc(f.id);
        }
    }

    /**
     * 1.16.0: לפני "העתקה" — העותקים העצמאיים של הרשימות החכמות: מה שהועתק עצמו, והפנימיות שבתוך רשימות שמועתקות
     * (כלי והגדרה נפוצה בתוך רשימה — הם קישור מטבעם, ונשארים). מחוץ לעריכת העץ — כך לא נועלים את שני הקבצים יחד.
     */
    private static java.util.Map<String, String> innerCopies(List<Item> items, List<String> src, List<String> dest) {
        java.util.Map<String, String> out = new java.util.HashMap<String, String>();
        Tree tr = Tree.load();
        String here = Tree.label(dest);
        for (Item it : items) {
            if (it.kind == SMART) {
                String c = Structure.copyInner(it.value, here + " › " + it.name);
                if (c != null) out.put(it.value, c);
                continue;
            }
            Tree.Node m = it.kind == LIST ? (src == null ? null : tr.find(child(src, it.value)))
                    : it.kind == REF ? tr.find(Tree.parsePath(it.value)) : null;
            if (m != null) innerOf(m, out);
        }
        return out;
    }

    private static void innerOf(Tree.Node n, java.util.Map<String, String> out) {
        for (String r : n.smart)
            if (!out.containsKey(r) && Nested.kindOf(r) == Nested.Kind.INNER) {
                Nested.Item it = Nested.resolve(r);
                String c = Structure.copyInner(r, n.name + " › " + it.name);
                if (c != null) out.put(r, c);
            }
        for (Tree.Node c : n.children) innerOf(c, out);
    }

    /** פריט אחד לתוך הרשימה b (באותו עץ). */
    private static boolean intoNode(Tree t, Item it, Tree.Node a, Tree.Node b, List<String> dest, List<String> src,
                                    boolean mv, boolean fl, List<String[]> moved, final java.util.Map<String, String> copies) {
        Tree.Links own = copies == null ? Tree.SAME : new Tree.Links() {     // 1.16.0: העתקה — גם הפנימיות שבתוכה עצמאיות
            @Override public String smart(String ref) { return copies.containsKey(ref) ? copies.get(ref) : ref; }
            @Override public String list(String path) { return path; }
        };
        boolean out = mv && a != null && a != b;                              // העברה — יוצא מהמקור
        switch (it.kind) {
            case FILE: {
                Tree.Entry e = null;
                if (a != null) for (Tree.Entry x : a.entries) if (x.path.equals(it.value)) e = x;
                if (!Tree.contains(b.entries, it.value))
                    b.entries.add(e != null ? new Tree.Entry(e.path, e.size, e.bad) : new Tree.Entry(it.value, new File(it.value).length(), false));
                else if (!out) return false;
                if (out && e != null) a.entries.remove(e);
                return true;
            }
            case LIST: {
                Tree.Node m = a == null ? null : a.child(it.value);
                if (m == null || m == b || m.isAncestorOf(b)) return false;
                if (fl) {
                    Tree.copyNode(m, Tree.newChild(b, b.uniqueChildName(m.name, null)), own);
                } else if (mv) {
                    if (a == b) return false;
                    a.children.remove(m);
                    String old = m.name;
                    m.name = b.uniqueChildName(m.name, null);
                    m.parent = b;
                    b.children.add(m);
                    moved.add(new String[]{old, m.name});
                } else {
                    return Tree.addRef(b, dest, child(src, it.value));
                }
                return true;
            }
            case REF: {
                List<String> target = Tree.parsePath(it.value);
                if (fl) {
                    Tree.Node m = t.find(target);
                    if (m == null || m == b || m.isAncestorOf(b)) return false;
                    Tree.copyNode(m, Tree.newChild(b, b.uniqueChildName(m.name, null)), own);
                } else if (!Tree.addRef(b, dest, target)) return false;
                if (out) a.refs.remove(it.value);
                return true;
            }
            case SMART:
                if (fl) {                                                     // 1.16.0: העתקה — רשימה חכמה עצמאית
                    String c = copies.get(it.value);
                    if (c == null || out) return false;
                    b.smart.add(c);
                    return true;
                }
                if (b.smart.contains(it.value)) {
                    if (!out) return false;
                } else b.smart.add(it.value);
                if (out) a.smart.remove(it.value);
                return true;
            case LIVE:
                if (b.live.contains(it.value)) {
                    if (!out) return false;
                } else b.live.add(it.value);
                if (out) a.live.remove(it.value);
                return true;
            default:
                return false;
        }
    }

    /** כלי — מה שהוא מחזיק, כפריט ברשימה: רשימה ← הפניה (או עותק); חכמה ← חכמה; קובץ פקודות ← קובץ. */
    private static Item toolAsItem(Structure.Func f) {
        Target t = f.target();
        if (FnVirtual.KIND.equals(f.kind)) {
            List<String> p = FnVirtual.base(t);
            return p.isEmpty() ? null : new Item(REF, Tree.fullLabel(p), f.name);
        }
        if (FnSmart.KIND.equals(f.kind) || FnDup.KIND.equals(f.kind)) {       // 1.15.1: כפולים — לשונית של רשימה חכמה
            String link = SmartLists.linkOf(t);
            if (!link.isEmpty() && !move) return new Item(SMART, link, f.name);
            return new Item(SMART, f.id, f.name);   // העתקה — קישור לכלי; העברה — הסעיף עצמו נהיה רשימה פנימית (אותו מזהה)
        }
        if (FnCommand.KIND.equals(f.kind)) {
            File x = FnCommand.file(f);
            return x == null ? null : new Item(FILE, x.getAbsolutePath(), f.name);
        }
        return null;
    }

    /** מאופן אחר: כל מה שצריך מועתק (ModeCopy); קבצים ותיקיות — כמו שהם. */
    private static void pasteIntoOther(List<String> dest, Result r) {
        final List<Item> plain = new ArrayList<Item>();
        for (Item it : items) {
            ModeCopy.Pick p = pick(it);
            if (it.kind == FILE || it.kind == LIVE) plain.add(it);
            else if (p == null) r.skip(Words.VClipW.NOT_HERE);
            else if (ModeCopy.intoList(mode, p, dest) != null) r.done++;
            else r.skip(Words.VClipW.FAILED);
        }
        if (!plain.isEmpty()) {
            final List<String> d = dest;
            final Result rr = r;
            Tree.edit(new Tree.Edit() {
                @Override
                public boolean apply(Tree t) {
                    Tree.Node b = t.find(d);
                    if (b == null) return false;
                    boolean ch = false;
                    for (Item it : plain) {
                        if (intoNode(t, it, null, b, d, null, false, false, null, null)) {
                            rr.done++;
                            ch = true;
                        }
                    }
                    return ch;
                }
            });
        }
        if (move && r.done > 0) removeFromSource();
    }

    // ---------- לבית ----------

    /** האם אפשר להדביק במסך הבית. null — אפשר. */
    static synchronized String cannotPasteHome() {
        if (items.isEmpty()) return Words.ClipW.PICK_LIST_FIRST;
        for (Item it : items) if (fitsHome(it)) return null;
        return Words.VClipW.NOT_HERE;
    }

    /** קובץ — רק קובץ פקודות; כלי שכבר בבית — לא מעבירים אליו. */
    private static boolean fitsHome(Item it) {
        if (it.kind == FILE) return Batch.isBatch(it.value);
        return it.kind != TOOL || !move || !sameMode();
    }

    /** מדביק במסך הבית של האופן הפעיל — בשמירה אחת (1.23.1). */
    static synchronized Result pasteHome() {
        final Result[] out = new Result[1];
        Structure.batch(new Runnable() {
            @Override public void run() { out[0] = pasteHomeNow(); }
        });
        return out[0];
    }

    private static Result pasteHomeNow() {
        Result r = new Result();
        if (items.isEmpty()) return r;
        boolean same = sameMode();
        final List<String> removeRefs = new ArrayList<String>(), removeSmart = new ArrayList<String>(),
                removeLive = new ArrayList<String>();
        for (Item it : items) {
            if (!fitsHome(it)) {
                r.skip(Words.VClipW.NOT_HERE);
                continue;
            }
            String made = same ? homeSame(it) : homeOther(it);
            if (made == null) {
                r.skip(Words.VClipW.FAILED);
                continue;
            }
            r.done++;
            if (same && move) {
                if (it.kind == REF) removeRefs.add(it.value);
                if (it.kind == SMART) removeSmart.add(it.value);
                if (it.kind == LIVE) removeLive.add(it.value);
            }
        }
        if (same && move && from != null && !(removeRefs.isEmpty() && removeSmart.isEmpty() && removeLive.isEmpty())) {
            final List<String> src = from;
            Tree.edit(new Tree.Edit() {
                @Override
                public boolean apply(Tree t) {
                    Tree.Node a = t.find(src);
                    if (a == null) return false;
                    a.refs.removeAll(removeRefs);
                    a.smart.removeAll(removeSmart);
                    a.live.removeAll(removeLive);
                    return true;
                }
            });
        }
        if (!same && move && r.done > 0) removeFromSource();
        if (r.done > 0) clear();
        return r;
    }

    private static Target listTool(List<String> p) {
        return Target.of(FnVirtual.KIND, FnVirtual.LIST, Tree.key(p));
    }

    /** באותו אופן: מה נוצר בבית (שם, או null — לא הצליח). */
    private static String homeSame(Item it) {
        switch (it.kind) {
            case TOOL: {
                Structure.Func f = Structure.func(it.value);
                if (f == null) return null;
                if (full) return ModeCopy.intoActive(Modes.current(), ModeCopy.Pick.tool(f.id, f.name));
                return Structure.addDirectFunc(f.name, f.kind, f.description, f.target()) != null ? f.name : null;
            }
            case LIST:
            case REF: {
                List<String> p = it.kind == LIST ? child(from, it.value) : Tree.parsePath(it.value);
                if (Tree.load().find(p) == null) return null;
                if (full) p = duplicateAtRoot(p);
                else if (move && it.kind == LIST && p.size() > 1) p = moveToRoot(p);
                if (p == null) return null;
                return Structure.addDirectFunc(p.get(p.size() - 1), FnVirtual.KIND, "", listTool(p)) != null ? it.name : null;
            }
            case SMART:
                return smartTool(it);
            case LIVE:
            case FILE: {
                File f = it.kind == FILE ? new File(it.value) : Shortcuts.write("📂", it.name, Verbs.openFolder(it.value));
                if (f == null) return null;
                return Structure.addDirectFunc(it.name, FnCommand.KIND, "", Target.of(FnCommand.KIND, FnCommand.FILE, f.getAbsolutePath())) != null
                        ? it.name : null;
            }
            default:
                return null;
        }
    }

    /** רשימה חכמה מתוך רשימה — כלי חכם: הגדרה נפוצה — מקושרת; כלי — עוד כלי כמוהו; פנימית — ההגדרות שלה. */
    private static String smartTool(Item it) {
        Nested.Kind k = Nested.kindOf(it.value);
        if (k == Nested.Kind.COMMON)
            return Structure.addDirectFunc(it.name, FnSmart.KIND, "", Target.of(FnSmart.KIND, SmartLists.LINK, it.value)) != null ? it.name : null;
        Structure.Func f = Structure.byId(it.value);
        if (f == null) return null;
        String kind = FnDup.KIND.equals(f.params.get("סוג")) ? FnDup.KIND : FnSmart.KIND;   // 1.15.1: גם כפולים // תחביר
        Target t = Target.of(kind, f.params);
        t.params.remove("סוג");  // תחביר
        t.params.remove("תיאור");  // תחביר
        return Structure.addDirectFunc(it.name, kind, "", t) != null ? it.name : null;
    }

    /** מאופן אחר — עותק מלא (ModeCopy). */
    private static String homeOther(Item it) {
        if (it.kind == FILE || it.kind == LIVE) return homeSame(it);
        ModeCopy.Pick p = pick(it);
        return p == null ? null : ModeCopy.intoActive(mode, p);
    }

    /** הפריט כבחירה של ModeCopy (מהאופן שממנו הועתק). */
    private static ModeCopy.Pick pick(Item it) {
        switch (it.kind) {
            case LIST: return ModeCopy.Pick.list(child(from, it.value));
            case REF: return ModeCopy.Pick.list(Tree.parsePath(it.value));
            case SMART: return ModeCopy.Pick.smart(it.value, it.name);
            case TOOL: return ModeCopy.Pick.tool(it.value, it.name);
            default: return null;
        }
    }

    // ---------- עזר ----------

    /** עותק של הרשימה בשורש (שם פנוי); מחזיר את המסלול שלו. */
    private static List<String> duplicateAtRoot(final List<String> p) {
        final String[] made = {null};
        Tree.edit(new Tree.Edit() {
            @Override
            public boolean apply(Tree t) {
                Tree.Node m = t.find(p);
                if (m == null) return false;
                Tree.Node c = Tree.newChild(t.root, t.root.uniqueChildName(m.name, null));
                Tree.copyNode(m, c);
                made[0] = c.name;
                return true;
            }
        });
        return made[0] == null ? null : child(new ArrayList<String>(), made[0]);
    }

    /** העברת רשימה לבית: היא עצמה עוברת לשורש (והכלים עוברים איתה); מחזיר את המסלול החדש. */
    private static List<String> moveToRoot(final List<String> p) {
        final String[] made = {null};
        Tree.edit(new Tree.Edit() {
            @Override
            public boolean apply(Tree t) {
                Tree.Node m = t.find(p);
                if (m == null || m.parent == null) return false;
                m.parent.children.remove(m);
                m.name = t.root.uniqueChildName(m.name, null);
                m.parent = t.root;
                t.root.children.add(m);
                made[0] = m.name;
                return true;
            }
        });
        if (made[0] == null) return null;
        List<String> to = child(new ArrayList<String>(), made[0]);
        FnVirtual.listMoved(p, to);
        return to;
    }

    /** העברה לאופן אחר — אחרי העותק: יוצא מהאופן שממנו (מהקבצים שלו, בכונן). */
    private static void removeFromSource() {
        try {
            ModeCopy.Side s = ModeCopy.read(mode);
            Tree.Node a = from == null ? null : s.tree.find(from);
            for (Item it : items) {
                if (it.kind == TOOL) {
                    Structure.cut(s.ini, it.value, false);
                    continue;
                }
                if (a == null) continue;
                if (it.kind == FILE) Tree.remove(a.entries, it.value);
                else if (it.kind == LIST) a.children.remove(a.child(it.value));
                else if (it.kind == REF) a.refs.remove(it.value);
                else if (it.kind == SMART) a.smart.remove(it.value);
                else if (it.kind == LIVE) a.live.remove(it.value);
            }
            s.tree.saveTo(ModeCopy.treeOf(mode));
            s.ini.saveQuiet();
        } catch (Throwable t) {
            Log.e("העברה מאופן אחר — יציאה מהמקור", t);
        }
    }

    private static List<String> child(List<String> parent, String name) {
        List<String> p = new ArrayList<String>(parent == null ? new ArrayList<String>() : parent);
        p.add(name);
        return p;
    }

}
