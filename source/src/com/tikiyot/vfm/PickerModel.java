package com.tikiyot.vfm;

// המודל של הבורר (106): אילו צמתים מוצגים ובאיזה סדר, איך עולים ונכנסים, מה נפתח, ומה נמצא בחיפוש.
// בלי ממשק, ולכן נבדק במחשב. הממשק — PickDialog. המקור (רשימות או תיקיות) — PickSource.
// שני מצבים: U — נקודות פתיחה קבועות (הכוננים), בשם שלהן; R — הכול, מהשורש (הרשימות).
// (עד מ0.42 היה גם S — בתוך רשימה אחת של כלי, עם מעבר ל"רשימה בכלי אחר"; של המנוע הישן, אף אחד לא פתח אותו.)

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class PickerModel {

    static final int U = 0, R = 1;

    /** כמה תוצאות חיפוש מוצגות לכל היותר. */
    static final int SEARCH_MAX = 150;

    static final class Row {
        ArrayList<String> path;
        String label, sub;
        int depth;
        /** בתצוגת עץ: לכל אב (מעומק 1 ואילך) — האם יש אחריו אחים, כלומר האם הקו שלו נמשך. */
        boolean[] cont = new boolean[0];
        boolean last, pickable, kids;
        /** עץ גדול: אפשר לפתוח ולסגור את הענף, והאם הוא פתוח. */
        boolean expandable, open;
    }

    final PickSource src;
    final int mode;
    /** U: נקודות הפתיחה, ושמותיהן. */
    final List<ArrayList<String>> roots = new ArrayList<ArrayList<String>>();
    final List<String> rootNames = new ArrayList<String>();
    /** תצוגת תיקיות: איפה עומדים. null = נקודות הפתיחה (U); ריק = שורש העץ (R). */
    ArrayList<String> current;
    /** בחירה בנגיעה אחת בתצוגת עץ (בלי שאלה): ברירת מחדל ב-U. */
    boolean direct;
    /** החיפוש האחרון הציג רק חלק מהתוצאות. */
    boolean truncated;
    private final Set<String> expanded = new HashSet<String>();
    /** 133: כל הצמתים והשמות המנורמלים — נבנים פעם אחת לחיפוש, ולא בכל הקשה. */
    private List<Row> all;
    private List<String> allNorm;

    /** bases: נקודות פתיחה בשמן, או null — הכול, מהשורש. */
    PickerModel(PickSource src, Map<ArrayList<String>, String> bases) {
        this.src = src;
        if (bases != null)
            for (Map.Entry<ArrayList<String>, String> e : bases.entrySet())
                if (exists(e.getKey())) {
                    roots.add(e.getKey());
                    rootNames.add(e.getValue());
                }
        if (!roots.isEmpty()) {
            mode = U;
            current = null;
        } else {
            mode = R;
            current = new ArrayList<String>();
        }
        direct = mode == U;
        if (src.large())                         // עץ גדול: נקודות הפתיחה פתוחות, והשאר סגור
            for (ArrayList<String> r : roots) {
                expanded.add(key(r));
                src.visit(r);                                     // 137
            }
    }

    boolean exists(List<String> p) {
        return p != null && src.children(p) != null;
    }

    boolean pickable(List<String> p) {
        return src.whyNot(p) == null;
    }

    private static ArrayList<String> child(List<String> p, String name) {
        ArrayList<String> c = new ArrayList<String>(p);
        c.add(name);
        return c;
    }

    private static String key(List<String> p) {
        StringBuilder sb = new StringBuilder();
        for (String s : p) sb.append(s).append('\u0001');
        return sb.toString();
    }

    private Row row(ArrayList<String> path, String label, int depth, boolean[] cont, boolean last) {
        Row r = new Row();
        r.path = path;
        r.label = label != null ? label : src.label(path);       // 171: המקור יודע את השם
        r.depth = depth;
        r.cont = cont;
        r.last = last;
        r.pickable = pickable(path);
        r.sub = src.sub(path);                                    // 171
        List<String> kids = src.children(path);
        r.kids = kids != null && !kids.isEmpty();
        return r;
    }

    // ---------- תצוגת תיקיות ----------

    List<Row> folderRows() {
        List<Row> out = new ArrayList<Row>();
        if (current == null) {
            for (int i = 0; i < roots.size(); i++)
                out.add(row(roots.get(i), rootNames.get(i), 0, new boolean[0], i == roots.size() - 1));
        } else {
            List<String> names = src.children(current);
            if (names != null)
                for (int i = 0; i < names.size(); i++)
                    out.add(row(child(current, names.get(i)), null, 0, new boolean[0], i == names.size() - 1));
        }
        return out;
    }

    boolean canUp() {
        if (mode == U) return current != null;
        return !current.isEmpty();
    }

    /** עולה רמה. מ-U: אם ההורה מחוץ לנקודות הפתיחה — חוזרים לרשימת נקודות הפתיחה. */
    void up() {
        if (!canUp()) return;
        ArrayList<String> parent = new ArrayList<String>(current.subList(0, current.size() - 1));
        if (mode == U) current = !parent.isEmpty() && inScope(parent) ? parent : null;
        else current = parent;
    }

    void enter(List<String> p) {
        current = new ArrayList<String>(p);
        src.visit(current);                                       // 137
        all = null;
    }

    private boolean inScope(List<String> p) {
        for (ArrayList<String> r : roots)
            if (p.size() >= r.size() && p.subList(0, r.size()).equals(r)) return true;
        return false;
    }

    /** האם "לכאן" על הצומת הנוכחי אפשרי: צומת אמיתי (לא השורש ולא רשימת נקודות הפתיחה), ולא זה שאסור כיעד. */
    boolean canChooseHere() {
        return current != null && !current.isEmpty() && pickable(current);
    }

    /** הכותרת בתצוגת תיקיות: איפה עומדים. */
    String crumb() {
        if (current == null || current.isEmpty()) return src.top() != null ? src.top() : Words.ALL_LISTS;
        return src.fullLabel(current);
    }

    /** איפה נוצר צומת חדש. null = צריך קודם לבחור באיזו נקודת פתיחה (כונן). */
    ArrayList<String> createIn(boolean folderView) {
        if (folderView) return current;
        return mode == R ? new ArrayList<String>() : null;
    }

    // ---------- תצוגת עץ ----------

    List<Row> treeRows() {
        return tree(false);
    }

    private List<Row> tree(boolean all) {
        List<Row> out = new ArrayList<Row>();
        if (mode == U) {
            for (int i = 0; i < roots.size(); i++)
                addTree(roots.get(i), rootNames.get(i), 0, new boolean[0], i == roots.size() - 1, all, out);
        } else {
            List<String> top = src.children(new ArrayList<String>());
            if (top != null)
                for (int i = 0; i < top.size(); i++)
                    addTree(child(new ArrayList<String>(), top.get(i)), null, 0, new boolean[0], i == top.size() - 1, all, out);
        }
        return out;
    }

    private void addTree(ArrayList<String> path, String label, int depth, boolean[] cont, boolean last,
                         boolean all, List<Row> out) {
        Row r = row(path, label, depth, cont, last);
        r.expandable = src.large() && r.kids;
        r.open = expanded.contains(key(path));
        out.add(r);
        if (r.expandable && !all && !r.open) return;               // עץ גדול: ענף סגור
        boolean[] cc;
        if (depth == 0) {
            cc = new boolean[0];
        } else {
            cc = java.util.Arrays.copyOf(cont, cont.length + 1);
            cc[cont.length] = !last;
        }
        List<String> names = src.children(path);
        if (names == null) return;
        for (int i = 0; i < names.size(); i++)
            addTree(child(path, names.get(i)), null, depth + 1, cc, i == names.size() - 1, all, out);
    }

    /** 130: מציג או מסתיר תיקיות מוסתרות — הכול נבנה מחדש. */
    void showHidden(boolean show) {
        src.showHidden(show);
        all = null;
        if (current != null) src.visit(current);
    }

    /** פותח או סוגר ענף (בעץ גדול). */
    void toggle(List<String> p) {
        String k = key(p);
        if (!expanded.remove(k)) {
            expanded.add(k);
            src.visit(p);                                         // 137
            all = null;
        }
    }

    // ---------- חיפוש ----------

    /**
     * כל הצמתים שבתחום ששמם מכיל את הטקסט, שטוח, עם המסלול המלא — גם בענפים סגורים.
     * צומת שאינו יעד — לא מופיע. עד SEARCH_MAX תוצאות; truncated אומר שיש עוד.
     */
    List<Row> search(String query) {
        String q = Text.norm(query == null ? "" : query).trim();
        List<Row> out = new ArrayList<Row>();
        truncated = false;
        if (q.isEmpty()) return out;
        if (all == null) {                                       // 133: פעם אחת
            all = tree(true);
            allNorm = new ArrayList<String>(all.size());
            for (Row r : all) allNorm.add(Text.norm(r.label) + "\u0001" + Text.norm(r.path.get(r.path.size() - 1)));
        }
        Set<String> seen = new HashSet<String>();
        for (int i = 0; i < all.size(); i++) {
            Row r = all.get(i);
            if (!r.pickable) continue;
            if (!allNorm.get(i).contains(q)) continue;
            if (!seen.add(key(r.path))) continue;
            if (out.size() >= SEARCH_MAX) {
                truncated = true;
                break;
            }
            Row x = new Row();
            x.path = r.path;
            x.label = r.label;
            x.pickable = true;
            x.kids = r.kids;
            x.sub = r.path.size() > 1 ? src.fullLabel(r.path) : null;
            out.add(x);
        }
        return out;
    }
}
