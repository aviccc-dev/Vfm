package com.tikiyot.vfm;

// מ0.18: יצוא מהמסד לטקסט (claude/masad-yetzu.md): כותרת "#N א / ב" לכל רשימה, @צלמית/@תיאור/@למה/@עיצוב, ושורת קובץ
// "/מסלול | גודל". השורה הראשונה — @תיאור: גרסת הפורמט, התאריך ומה יצא.
// מ0.29 — פורמט 2 (הכול, ובסדר של הרשימה; המשתמש: "תבנה להם קובץ יצוא ומשם לעשות יבוא" — כך גם הדוגמאות):
//   @תת-רשימה <שם>  — רשימה שבתוך הרשימה, במקומה בסדר (הבלוק שלה — בהמשך הקובץ, בכותרת "#N …" משלה)
//   @קישור <א / ב>  — רשימה שכבר הופיעה במקום אחר (אותה רשימה בשני מקומות)
//   @חכמה <שם>      — רשימה חכמה; אחריה שורות "+תנאי", "+מיון", "+כיוון", "+תצוגה", "+צלמית", "+תיאור", "+למה"
//   @כותרת <טקסט> · @רעיון <טקסט>  ·  @חבורה (רשימה מוטמעת)
// קובץ בפורמט 1 נקרא כמו תמיד (פורמט 2 רק מוסיף שורות).
// אחראי: הטקסט ושם הקובץ (בלי ממשק — נבדק). אסור לו: לכתוב למסד.

import java.io.File;
import java.util.List;

final class DbExport {

    static final int FORMAT = 2;
    static final String SUB = "@תת-רשימה ", LINK = "@קישור ", SMART = "@חכמה ", TITLE = "@כותרת ", NOTE = "@רעיון ", GROUP = "@חבורה";  // תחביר
    static final String ICON = "@צלמית ", DESC = "@תיאור ", DESIGN = "@עיצוב ", WHY = "@למה ";  // תחביר
    /** מאפייני רשימה חכמה: השורה בקובץ ← המפתח במסד. */
    static final String[][] SMART_PROPS = {{"+תנאי ", Store.P_COND}, {"+מיון ", Store.P_SORT}, {"+כיוון ", Store.P_DIR},  // תחביר
            {"+תצוגה ", Store.P_VIEW}, {"+צלמית ", Store.P_ICON}, {"+תיאור ", Store.P_DESC}, {"+למה ", Store.P_WHY}};  // תחביר

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
        java.util.Map<Long, List<String>> where = new java.util.HashMap<Long, List<String>>();
        if (home) {
            where.put(list, new java.util.ArrayList<String>());
            members(s, list, new java.util.ArrayList<String>(), body, o, where);
        } else {
            List<String> p = java.util.Collections.singletonList(s.item(list).name);
            where.put(list, p);
            node(s, list, p, body, o, where);
        }
        o.text = DESC + Words.DbCmd.exportHeader(FORMAT, date(when), title, o.lists, o.files, o.skipped) + "\n" + body;
        return o;
    }

    /** רק המסלולים של הקבצים (קבצי ידנית). */
    static Out paths(List<String> paths, String title, long when) {
        Out o = new Out();
        StringBuilder sb = new StringBuilder(DESC).append(Words.DbCmd.exportHeader(FORMAT, date(when), title, 0, paths.size(), 0)).append('\n');
        for (String p : paths) sb.append(p).append('\n');
        o.files = paths.size();
        o.text = sb.toString();
        return o;
    }

    private static void node(Store s, long id, List<String> path, StringBuilder sb, Out o, java.util.Map<Long, List<String>> where) {
        o.lists++;
        if (sb.length() > 0) sb.append('\n');
        sb.append('#').append(path.size()).append(' ').append(Names.join(path, Names.SEP)).append('\n');
        prop(sb, s, id, Store.P_ICON, ICON);
        prop(sb, s, id, Store.P_DESIGN, DESIGN);
        prop(sb, s, id, Store.P_DESC, DESC);
        prop(sb, s, id, Store.P_WHY, WHY);
        if ("1".equals(s.prop(id, Store.P_GROUP))) sb.append(GROUP).append('\n');
        members(s, id, path, sb, o, where);
    }

    /** החברים של רשימה, בסדר שלה; תתי־הרשימות — שורה במקומן, והבלוק של כל אחת אחרי הרשימה. */
    private static void members(Store s, long id, List<String> path, StringBuilder sb, Out o, java.util.Map<Long, List<String>> where) {
        List<long[]> later = new java.util.ArrayList<long[]>();
        List<List<String>> laterPath = new java.util.ArrayList<List<String>>();
        for (Store.Member m : s.members(id)) {
            Store.Item it = m.item;
            if (it.type == Store.FILE) entry(s, it.id, sb, o);
            else if (it.type == Store.SMART) {
                sb.append(SMART).append(one(it.name)).append('\n');
                for (String[] k : SMART_PROPS) prop(sb, s, it.id, k[1], k[0]);
            } else if (it.type == Store.TITLE) sb.append(TITLE).append(one(it.name)).append('\n');
            else if (it.type == Store.NOTE) sb.append(NOTE).append(one(it.name)).append('\n');
            else if (it.type == Store.LIST) {
                List<String> had = where.get(it.id);
                if (had != null) {
                    if (!had.isEmpty()) sb.append(LINK).append(Names.join(had, Names.SEP)).append('\n');
                    continue;
                }
                List<String> p = new java.util.ArrayList<String>(path);
                p.add(it.name);
                where.put(it.id, p);
                sb.append(SUB).append(it.name).append('\n');
                later.add(new long[]{it.id});
                laterPath.add(p);
            } else o.skipped++;
        }
        for (int i = 0; i < later.size(); i++) node(s, later.get(i)[0], laterPath.get(i), sb, o, where);
    }

    /** טקסט בשורה אחת (שורה חדשה — "⏎"). */
    static String one(String t) {
        return (t == null ? "" : t).replace("\r", "").replace("\n", " ⏎ ").trim();
    }

    private static void entry(Store s, long id, StringBuilder sb, Out o) {
        Store.FileInfo fi = s.file(id);
        if (fi == null) return;
        sb.append(fi.size >= 0 ? fi.path + " | " + fi.size : fi.path + " | ?").append('\n');   // גודל לא ידוע — "?"
        o.files++;
    }

    private static void prop(StringBuilder sb, Store s, long id, String key, String line) {
        String v = s.prop(id, key);
        if (v != null && !v.trim().isEmpty()) sb.append(line).append(one(v)).append('\n');
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

    /**
     * לאן: "*" — שם לפי מה שיוצא ("אופן X", "רשימה X", "קבצי רשימה X"), בתיקייה "יצוא" של התוכנה, ותפוס — "(2)";
     * שם בלי מסלול — בתיקייה "יצוא"; מסלול מלא — כמו שנכתב (הכותב בחר — נכתב מעליו).
     */
    static File target(String raw, String defaultName, File exportDir) {
        String r = raw == null ? "" : raw.trim().replace('\\', '/');   // מ0.18.1: "\\" כמו בשאר הפקודות (באנדרואיד — שם קובץ אסור)
        if (r.isEmpty() || r.equals(Names.HOME)) {
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
