package com.tikiyot.vfm;

// רשימה חכמה בתיקיות (148) — המודל, בלי ממשק (נבדק): מאיפה מתחילים, כמה התאמות בכל תיקייה,
// ומה מוצג בתיקייה ב"רק מה שעונה". ב"כולל הכול" — התיקייה מהאינדקס (Library.listDir), וההתאמות מסומנות.
// הממשק: SearchScreen.

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class FolderView {

    static final String FLAT = "ברצף", MATCH = "בתיקיות — רק מה שעונה", ALL = "בתיקיות — כולל הכול";  // תחביר
    /** 1.11.0: הקבצים שב־%כאן% (התיקייה שממנה פתחו) — ישר, למעלה; כל השאר — מקובצים לפי התיקייה שלהם. */
    static final String HERE = "קודם כאן, השאר בתיקיות";  // תחביר
    static final String[] MODES = {FLAT, HERE, MATCH, ALL};

    static boolean folders(String mode) {
        return MATCH.equals(mode) || ALL.equals(mode);
    }

    /** תיקייה בתצוגה: מסלול, שם, וכמה התאמות יש בה (כולל תת־תיקיות). */
    static final class Folder {
        final String path, name;
        final int matches;

        Folder(String path, String name, int matches) {
            this.path = path;
            this.name = name;
            this.matches = matches;
        }
    }

    final List<String> roots;
    private final Set<String> matched = new HashSet<String>();
    private final Map<String, Integer> counts = new HashMap<String, Integer>();
    private final Map<String, Set<String>> childDirs = new HashMap<String, Set<String>>();
    private final Map<String, List<String>> filesIn = new HashMap<String, List<String>>();

    /** hits — ההתאמות של הרשימה; roots — מסלולי המקורות (או הכוננים). */
    FolderView(List<Library.Hit> hits, List<String> roots) {
        this.roots = roots;
        for (Library.Hit h : hits) {
            String p = h.path();
            String root = rootOf(p);
            if (root == null) continue;
            matched.add(p);
            if (p.equals(root)) continue;                                     // המקור עצמו — אין מעליו בתצוגה
            String parent = parent(p);
            if (h.isDir()) addSet(childDirs, parent, p);
            else addList(filesIn, parent, p);
            String child = p;
            for (String d = parent; d != null; d = d.equals(root) ? null : parent(d)) {
                Integer c = counts.get(d);
                counts.put(d, c == null ? 1 : c + 1);
                if (!child.equals(p) || h.isDir()) addSet(childDirs, d, child);
                child = d;
            }
        }
    }

    private static void addSet(Map<String, Set<String>> m, String k, String v) {
        Set<String> s = m.get(k);
        if (s == null) m.put(k, s = new LinkedHashSet<String>());
        s.add(v);
    }

    private static void addList(Map<String, List<String>> m, String k, String v) {
        List<String> s = m.get(k);
        if (s == null) m.put(k, s = new ArrayList<String>());
        s.add(v);
    }

    static String parent(String p) {
        int i = p.lastIndexOf('/');
        return i <= 0 ? null : p.substring(0, i);
    }

    static String name(String p) {
        return p.substring(p.lastIndexOf('/') + 1);
    }

    /** המקור שהמסלול בתוכו (הארוך ביותר), או null. */
    String rootOf(String p) {
        String best = null;
        for (String r : roots)
            if ((p.equals(r) || p.startsWith(r + "/")) && (best == null || r.length() > best.length())) best = r;
        return best;
    }

    /** מאיפה מתחילים: המקור היחיד; כמה מקורות — null (רשימת המקורות). */
    String start() {
        return roots.size() == 1 ? roots.get(0) : null;
    }

    /** תיקייה אחת למעלה; ממקור — לרשימת המקורות (null), או נשארים כשיש רק מקור אחד. */
    String up(String dir) {
        if (dir == null) return null;
        if (roots.contains(dir)) return roots.size() == 1 ? dir : null;
        return parent(dir);
    }

    /** אפשר לעלות מכאן. */
    boolean canGoUp(String dir) {
        return dir != null && !(roots.size() == 1 && roots.contains(dir));
    }

    boolean isMatch(String path) {
        return matched.contains(path);
    }

    int matches(String dir) {
        Integer c = counts.get(dir);
        return c == null ? 0 : c;
    }

    int total() {
        int n = 0;
        for (String r : roots) n += matches(r);
        return n;
    }

    /** רשימת המקורות, כתיקיות (ב"רק מה שעונה" — רק מה שיש בו התאמות). */
    List<Folder> top(boolean onlyMatching) {
        List<Folder> out = new ArrayList<Folder>();
        for (String r : roots)
            if (!onlyMatching || matches(r) > 0) out.add(new Folder(r, name(r), matches(r)));   // השם לתצוגה — במסך
        return out;
    }

    /** "רק מה שעונה": תת־התיקיות שיש בהן התאמות. */
    List<Folder> matchDirs(String dir) {
        List<Folder> out = new ArrayList<Folder>();
        Set<String> s = childDirs.get(dir);
        if (s != null) for (String d : s) out.add(new Folder(d, name(d), matches(d)));
        return out;
    }

    /** "רק מה שעונה": הקבצים שעונים, בתיקייה עצמה. */
    List<String> matchFiles(String dir) {
        List<String> s = filesIn.get(dir);
        return s == null ? new ArrayList<String>() : s;
    }

    /**
     * מסלולי ההתחלה מהמקורות שבתנאי: "פנימי"/"כרטיס" — שורש הכונן; מסלול — כמו שהוא.
     * בלי מקורות — כל הכוננים. vols: {שורש, "פנימי"/"כרטיס"}.
     */
    static List<String> roots(List<String> sources, List<String[]> vols) {
        List<String> out = new ArrayList<String>();
        if (sources.isEmpty()) {
            for (String[] v : vols) out.add(v[0]);
            return out;
        }
        for (String s : sources) {
            if (Sources.INTERNAL.equals(s) || Sources.CARD.equals(s)) {
                for (String[] v : vols) if (v[1].equals(s) && !out.contains(v[0])) out.add(v[0]);
            } else if (!out.contains(s)) out.add(s);
        }
        return out;
    }
}
