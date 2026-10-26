package com.tikiyot.vfm;

// מקור הבורר לתיקיות בכוננים (106): מהאינדקס — מיידי, כמו מנהל הקבצים והחיפוש (86: הבסיס הוא האינדקס).
// מסלול הצומת: [מפתח הכונן, שם תיקייה, שם תיקייה…]. תיקיות מוסתרות, הסל וכל מה שבתוכם אינם מוצגים.
// כונן שעוד אין לו אינדקס (בנייה ראשונה) — נקרא מהכונן עצמו, כדי שהבורר לא יהיה ריק.

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class IndexSource extends PickSource {

    static final class Drv {
        final String key, label, root;
        final Index ix;

        /** ix: האינדקס של הכונן, או null אם עוד אין. */
        Drv(String key, String label, String root, Index ix) {
            this.key = key;
            this.label = label;
            this.root = root;
            this.ix = ix;
        }
    }

    private final List<Drv> drives;
    private final Map<String, Map<Integer, List<Integer>>> cache = new HashMap<String, Map<Integer, List<Integer>>>();
    /** 137: תיקיות שנכנסו אליהן — מה שבתוכן נקרא גם מהכונן (פעם אחת), ומצטרף למה שבאינדקס. */
    private final Map<String, List<String>> disk = new HashMap<String, List<String>>();

    private static String key(List<String> p) {
        StringBuilder sb = new StringBuilder();
        for (String s : p) sb.append(s).append('\u0001');
        return sb.toString();
    }

    /** 130: גם תיקיות מוסתרות (שם שמתחיל בנקודה) — חוץ מסל המחזור, שלא מוצג אף פעם. */
    private boolean hidden;

    @Override
    public boolean canShowHidden() {
        return true;
    }

    @Override
    public void showHidden(boolean show) {
        if (show == hidden) return;
        hidden = show;
        cache.clear();
        disk.clear();
    }

    @Override
    public void visit(List<String> path) {
        if (path.isEmpty() || disk.containsKey(key(path))) return;
        Drv d = drive(path.get(0));
        if (d == null) return;
        List<String> l = fromDisk(d, path.subList(1, path.size()));
        if (l != null) disk.put(key(path), l);
    }

    IndexSource(List<Drv> drives) {
        this.drives = drives;
    }

    private Drv drive(String key) {
        for (Drv d : drives) if (d.key.equals(key)) return d;
        return null;
    }

    private static boolean empty(Index ix) {
        return ix == null || (ix.dirs.isEmpty() && ix.files.isEmpty());
    }

    /** תיקיות לפי הורה, ממוינות לפי שם; נבנה פעם אחת לכונן. */
    private Map<Integer, List<Integer>> kids(final Drv d) {
        Map<Integer, List<Integer>> m = cache.get(d.key);
        if (m != null) return m;
        final Index ix = d.ix;
        m = new HashMap<Integer, List<Integer>>();
        for (int i = 0; i < ix.dirs.size(); i++) {
            Index.Dir x = ix.dirs.get(i);
            if (x.gone || x.trash || (x.hidden && !hidden)) continue;
            List<Integer> l = m.get(x.parent);
            if (l == null) m.put(x.parent, l = new ArrayList<Integer>());
            l.add(i);
        }
        for (List<Integer> l : m.values())
            Collections.sort(l, new Comparator<Integer>() {
                @Override
                public int compare(Integer a, Integer b) {
                    return ix.dirs.get(a).norm.compareTo(ix.dirs.get(b).norm);
                }
            });
        cache.put(d.key, m);
        return m;
    }

    @Override
    public List<String> children(List<String> path) {
        List<String> out = new ArrayList<String>();
        if (path.isEmpty()) {
            for (Drv d : drives) out.add(d.key);
            return out;
        }
        Drv d = drive(path.get(0));
        if (d == null) return null;
        List<String> rest = path.subList(1, path.size());
        if (empty(d.ix)) return fromDisk(d, rest);
        Map<Integer, List<Integer>> kids = kids(d);
        int id = -1;
        for (String n : rest) {
            int found = -2;
            List<Integer> l = kids.get(id);
            if (l != null)
                for (int c : l)
                    if (d.ix.dirs.get(c).name.equals(n)) {
                        found = c;
                        break;
                    }
            if (found == -2) return fromDisk(d, rest);           // 137: תיקייה שאינה באינדקס — מהכונן
            id = found;
        }
        List<Integer> l = kids.get(id);
        if (l != null) for (int c : l) out.add(d.ix.dirs.get(c).name);
        List<String> more = disk.get(key(path));                 // 137: מה שבכונן ולא באינדקס (הוצא, חדש)
        if (more != null) {
            for (String m : more) if (!out.contains(m)) out.add(m);
            java.util.Collections.sort(out, new Comparator<String>() {
                @Override
                public int compare(String a, String b) { return Text.norm(a).compareTo(Text.norm(b)); }
            });
        }
        return out;
    }

    private List<String> fromDisk(Drv d, List<String> rest) {
        File dir = fileOf(d, rest);
        File[] all = dir.listFiles();
        if (all == null) return dir.isDirectory() ? new ArrayList<String>() : null;
        Arrays.sort(all);
        List<String> out = new ArrayList<String>();
        for (File f : all)
            if (f.isDirectory() && !Vols.skip(f, hidden) && !f.getName().equals(Index.TRASH)) out.add(f.getName());
        return out;
    }

    private static File fileOf(Drv d, List<String> rest) {
        StringBuilder sb = new StringBuilder(d.root);
        for (String n : rest) sb.append('/').append(n);
        return new File(sb.toString());
    }

    /** התיקייה שבמסלול. */
    File fileOf(List<String> path) {
        Drv d = drive(path.get(0));
        return fileOf(d, path.subList(1, path.size()));
    }

    @Override
    public boolean large() {
        return true;
    }

    @Override
    public String fullLabel(List<String> path) {
        if (path.isEmpty()) return "";
        Drv d = drive(path.get(0));
        StringBuilder sb = new StringBuilder(d == null ? path.get(0) : d.label);
        for (int i = 1; i < path.size(); i++) sb.append(" / ").append(path.get(i));
        return sb.toString();
    }
}
