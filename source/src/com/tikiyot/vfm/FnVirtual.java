package com.tikiyot.vfm;

// פונקציה: תיקיות וירטואליות (ידניות).

import android.app.Activity;
import android.widget.EditText;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

public final class FnVirtual extends Func {

    /** הסוג בקובץ הראשי, והמפתח של הרשימה של הכלי. */
    static final String KIND = "וירטואלי", LIST = "רשימה";  // תחביר

    public FnVirtual() {
        super(KIND, Words.MANUAL_LISTS, "📋");
    }

    /** מסלול הרשימה של המופע, מהפרמטר "רשימה". */
    static ArrayList<String> base(Target t) {
        return Tree.parsePath(t.get(LIST, ""));                          // 165: פירוק אחד
    }

    /**
     * 88: רשימה קיבלה שם חדש או הועברה — כל כלי שעובד עליה, או על רשימה בתוכה, עובר איתה.
     * מחזיר כמה כלים עודכנו.
     */
    public static int listMoved(List<String> from, List<String> to) {
        if (from == null || from.isEmpty() || from.equals(to)) return 0;
        int n = 0;
        for (Structure.Func f : Structure.load().all()) {
            if (!f.kind.equals(KIND)) continue;
            List<String> nb = Tree.moved(base(f.target()), from, to);
            if (nb == null) continue;
            Structure.setKey(f.id, LIST, Tree.key(nb));
            n++;
        }
        n += Nested.listMoved(from, to);                                    // 167: גם קישורים מרשימות חכמות
        Design.listMoved(from, to);                                         // 1.12.0: הצלמית והצבע בעיצובים עוברים איתה
        Tree.refsMoved(from, to);                                           // 1.13.1 (באג): ההפניות ↪, הכפתורים
        n += Buttons.listMoved(from, to) + Shortcuts.listMoved(from, to);   //   וקיצורי הבית — עוברים איתה
        return n;
    }

    /** כמו ensureOwn, ומודיע כשהרשימה לא נמצאה ונוצרה ריקה במקומה (88). */
    public static Target ensureOwn(Activity a, Structure.Func f) {
        recreated = null;
        Target t = ensureOwn(f);
        if (recreated != null)
            Ui.toast(a, Words.ListT.THE_LIST + recreated + Words.ListT.NOT_FOUND_CREATED);
        recreated = null;
        return t;
    }

    /** 88: שם הרשימה שנוצרה מחדש בקריאה האחרונה ל-ensureOwn, או null. */
    static String recreated;

    /** מוודא שלמופע יש רשימה משלו. מופע ישן בלי רשימה מקבל אחת בשמו, והמבנה מתעדכן. */
    public static Target ensureOwn(Structure.Func f) {
        Target t = f.target();
        ArrayList<String> path = base(t);
        String name;
        synchronized (Tree.LOCK) {                                    // 92
            Tree tree = Tree.load();
            if (tree.unreadable) return t;                            // 90: לא יוצרים על עץ שלא נקרא
            if (!path.isEmpty()) {
                if (tree.find(path) == null) {                     // הרשימה נמחקה: יוצרים מחדש, ומודיעים (88)
                    StringBuilder lb = new StringBuilder();
                    for (String p : path) lb.append(lb.length() > 0 ? " / " : "").append(p);
                    recreated = lb.toString();
                    Tree.Node n = tree.root;
                    for (String p : path) {
                        Tree.Node c = n.child(p);
                        n = c != null ? c : Tree.newChild(n, p);
                    }
                    tree.save();
                }
                return t;
            }
            name = tree.root.uniqueChildName(f.name.isEmpty() ? Words.LIST : f.name, null);
            Tree.newChild(tree.root, name);
            if (!tree.save()) return t;
        }
        Target fixed = Target.of(KIND, f.params);
        fixed.params.put(LIST, name);
        Structure.updateFunc(f.id, f.name, f.description, fixed);
        return fixed;
    }

    /** מוחק את הרשימה של המופע, אחרי גיבוי בשם. הקבצים עצמם לא נפגעים. */
    public static void deleteOwnList(Structure.Func f) {
        ArrayList<String> path = base(f.target());
        if (path.isEmpty() || usedByOther(f, path)) return;
        synchronized (Tree.LOCK) {
            Tree t = Tree.load();
            Tree.Node n = t.find(path);
            if (n == null || n.parent == null) return;
            Files.backupNamed(Paths.tree(), Words.ListT.BEFORE_DELETING + n.name);
            n.parent.children.remove(n);
            t.save();
        }
    }

    /** האם כלי אחר עובד על אותה רשימה — אז מחיקת הכלי לא מוחקת אותה. */
    public static boolean usedByOther(Structure.Func f, ArrayList<String> path) {
        Structure.Model m = Structure.load();
        List<Structure.Func> all = m.all();
        for (Structure.Func o : all)
            if (!o.id.equals(f.id) && o.kind.equals(KIND) && base(o.target()).equals(path)) return true;
        return false;
    }

    /** מסלולי הרשימות של כל מופעי הרשימות הקיימים, עם שם המופע. */
    public static java.util.LinkedHashMap<ArrayList<String>, String> allBases() {
        java.util.LinkedHashMap<ArrayList<String>, String> out = new java.util.LinkedHashMap<ArrayList<String>, String>();
        Structure.Model m = Structure.load();
        List<Structure.Func> all = m.all();
        for (Structure.Func f : all) {
            if (!f.kind.equals(KIND)) continue;
            ArrayList<String> b = base(f.target());
            if (!b.isEmpty() && !out.containsKey(b)) out.put(b, f.name);
        }
        return out;
    }

    @Override
    public void open(Activity a, Target t) {
        VirtualScreen.open(a, base(t), null, t.get(Program.WHY, ""));        // 1.13.0: "למה" של הכלי
    }

    @Override
    public void configure(final Activity a, String name, Target t, final Done done) {
        LinearLayout b = Ui.form(a);
        final EditText nm = Ui.field(a, name != null ? name : "", Words.ListT.TOOL_NAME_OPT);
        final ArrayList<String> current = t != null ? base(t) : new ArrayList<String>();
        final android.widget.Spinner which = Ui.choice(a, new String[]{Words.NEW_LIST, Words.ListT.EXISTING_LIST},
                current.isEmpty() ? 0 : 1);
        final EditText newName = Ui.field(a, "", Words.ListT.NEW_LIST_NAME);
        final ArrayList<String> chosen = new ArrayList<String>(current);
        final android.widget.TextView chosenLabel = Ui.label(a, "");
        final android.widget.Button pick = new android.widget.Button(a);
        pick.setText(Words.ListT.PICK_LIST);
        pick.setAllCaps(false);
        final Runnable showChosen = new Runnable() {
            @Override
            public void run() { chosenLabel.setText(chosen.isEmpty() ? Words.ListT.NO_LIST_PICKED : "📋 " + Tree.label(chosen)); }
        };
        pick.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                ListPicker.choose(a, Words.ListT.CHOOSE_LIST, null, new ListPicker.Picked() {
                    @Override
                    public void ok(ArrayList<String> path) {
                        chosen.clear();
                        chosen.addAll(path);
                        showChosen.run();
                    }
                });
            }
        });
        final Runnable shape = new Runnable() {
            @Override
            public void run() {
                boolean existing = which.getSelectedItemPosition() == 1;
                newName.setVisibility(existing ? android.view.View.GONE : android.view.View.VISIBLE);
                pick.setVisibility(existing ? android.view.View.VISIBLE : android.view.View.GONE);
                chosenLabel.setVisibility(existing ? android.view.View.VISIBLE : android.view.View.GONE);
            }
        };
        which.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> p, android.view.View v, int i, long l) { shape.run(); }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) { }
        });
        b.addView(Ui.label(a, Words.ListT.TOOL_NAME));
        b.addView(nm);
        b.addView(Ui.label(a, Words.ListT.WHICH_LIST));
        b.addView(which);
        b.addView(newName);
        b.addView(pick);
        b.addView(chosenLabel);
        showChosen.run();
        shape.run();
        Ui.askForm(a, Words.MANUAL_LISTS, b, new Runnable() {
            @Override
            public void run() {
                String n = nm.getText().toString().trim();
                String list;
                if (which.getSelectedItemPosition() == 1) {
                    if (chosen.isEmpty()) {
                        Ui.toast(a, Words.ListT.NEED_LIST);
                        Ui.keepFormOpen();                                    // 98
                        return;
                    }
                    list = Tree.key(chosen);
                    if (n.isEmpty()) n = chosen.get(chosen.size() - 1);
                } else {
                    if (n.isEmpty()) n = Words.LIST;
                    String ln = newName.getText().toString().trim();
                    synchronized (Tree.LOCK) {
                        Tree tree = Tree.load();
                        list = tree.root.uniqueChildName(ln.isEmpty() ? n : ln, null);
                        Tree.newChild(tree.root, list);
                        tree.save();
                    }
                }
                // שינוי שם הכלי משנה רק את הכלי, לא את הרשימה (74)
                done.ok(n, Target.of(KIND, LIST, list));
            }
        });
    }
}
