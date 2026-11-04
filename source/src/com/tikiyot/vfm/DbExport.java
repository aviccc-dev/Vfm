package com.tikiyot.vfm;

// מ0.18: יצוא מהמסד לטקסט (claude/masad-yetzu.md). הפורמט — של "מבנה.txt" הישן (מוכר, נערך ביד, וההעתקה מהישן כבר
// קוראת אותו): כותרת "#N א / ב" לכל רשימה, @צלמית/@תיאור/@למה/@עיצוב, ושורת קובץ "/מסלול | גודל". השורה הראשונה —
// @תיאור של השורש: גרסת הפורמט, התאריך ומה יצא. רשימה עם תתי־רשימות — בקובץ אחד; רשימה שמופיעה פעמיים — פעם אחת.
// עוד לא בפורמט (נספר בשורה הראשונה): רשימות חכמות, רעיונות וכותרות.
// אחראי: הטקסט ושם הקובץ (בלי ממשק — נבדק). אסור לו: לכתוב למסד.

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class DbExport {

    static final int FORMAT = 1;

    private DbExport() { }

    /** מה יצא. */
    static final class Out {
        String text;
        int lists, files, skipped;
    }

    /** רשימה (או הבית) עם כל מה שבתוכה. title — מה יוצא (לשורה הראשונה). */
    static Out list(Store s, long list, boolean home, String title, long when) {
        Out o = new Out();
        StringBuilder body = new StringBuilder();
        Set<Long> seen = new HashSet<Long>();
        seen.add(list);
        if (home) {
            for (Store.Member m : s.members(list)) {
                Store.Item it = m.item;
                if (it.type == Store.LIST) node(s, it.id, java.util.Collections.singletonList(it.name), body, o, seen);
                else if (it.type == Store.FILE) entry(s, it.id, body, o);
                else o.skipped++;
            }
        } else {
            node(s, list, java.util.Collections.singletonList(s.item(list).name), body, o, seen);
        }
        o.text = Tree.DESC_LINE + Words.DbCmd.exportHeader(FORMAT, date(when), title, o.lists, o.files, o.skipped) + "\n" + body;
        return o;
    }

    /** רק המסלולים של הקבצים (קבצי ידנית). */
    static Out paths(List<String> paths, String title, long when) {
        Out o = new Out();
        StringBuilder sb = new StringBuilder(Tree.DESC_LINE).append(Words.DbCmd.exportHeader(FORMAT, date(when), title, 0, paths.size(), 0)).append('\n');
        for (String p : paths) sb.append(p).append('\n');
        o.files = paths.size();
        o.text = sb.toString();
        return o;
    }

    private static void node(Store s, long id, List<String> path, StringBuilder sb, Out o, Set<Long> seen) {
        o.lists++;
        if (sb.length() > 0) sb.append('\n');
        sb.append('#').append(path.size()).append(' ').append(join(path)).append('\n');
        prop(sb, s, id, Store.P_ICON, Tree.ICON_LINE);
        prop(sb, s, id, Store.P_DESIGN, Tree.DESIGN_LINE);
        prop(sb, s, id, Store.P_DESC, Tree.DESC_LINE);
        prop(sb, s, id, Store.P_WHY, Tree.WHY_LINE);
        List<Store.Member> ms = s.members(id);
        for (Store.Member m : ms) {
            if (m.item.type == Store.FILE) entry(s, m.item.id, sb, o);
            else if (m.item.type != Store.LIST) o.skipped++;
        }
        for (Store.Member m : ms) {
            if (m.item.type != Store.LIST || !seen.add(m.item.id)) continue;   // פעם אחת, ובלי לולאה
            List<String> p = new java.util.ArrayList<String>(path);
            p.add(m.item.name);
            node(s, m.item.id, p, sb, o, seen);
        }
    }

    private static void entry(Store s, long id, StringBuilder sb, Out o) {
        Store.FileInfo fi = s.file(id);
        if (fi == null) return;
        sb.append(new Tree.Entry(fi.path, fi.size, false).line()).append('\n');
        o.files++;
    }

    private static void prop(StringBuilder sb, Store s, long id, String key, String line) {
        String v = s.prop(id, key);
        if (v != null && !v.trim().isEmpty()) sb.append(line).append(v.replace('\n', ' ').trim()).append('\n');
    }

    static String date(long when) {
        return new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.US).format(new java.util.Date(when));
    }

    /** שם קובץ בטוח: תווים שאסורים בשם — רווח; עד 80 תווים. */
    static String safe(String name) {
        StringBuilder sb = new StringBuilder();
        for (char c : (name == null ? "" : name).toCharArray()) sb.append("/\\:*?\"<>|".indexOf(c) >= 0 ? ' ' : c);
        String r = sb.toString().trim();
        return r.length() > 80 ? r.substring(0, 80).trim() : r.isEmpty() ? "export" : r;
    }

    private static String join(List<String> p) {
        StringBuilder sb = new StringBuilder();
        for (String x : p) sb.append(sb.length() > 0 ? Tree.SEP : "").append(x);
        return sb.toString();
    }

    /**
     * לאן: "*" — שם לפי מה שיוצא ("אופן X", "רשימה X", "קבצי רשימה X"), בתיקייה "יצוא" של התוכנה, ותפוס — "(2)";
     * שם בלי מסלול — בתיקייה "יצוא"; מסלול מלא — כמו שנכתב (הכותב בחר — נכתב מעליו).
     */
    static File target(String raw, String defaultName, File exportDir) {
        String r = raw == null ? "" : raw.trim();
        if (r.isEmpty() || r.equals(ListCmd.HOME)) {
            String base = safe(defaultName);
            File f = new File(exportDir, base + ".txt");
            for (int k = 2; f.exists(); k++) f = new File(exportDir, base + " (" + k + ").txt");
            return f;
        }
        File f = new File(r);
        if (!f.isAbsolute()) f = new File(exportDir, r);
        if (!f.getName().contains(".")) f = new File(f.getParentFile(), f.getName() + ".txt");
        return f;
    }
}
