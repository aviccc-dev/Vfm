package com.tikiyot.vfm;

// שומרי המבנה (מבנה 3, 13.11.2026). הקוד מתחלק לחמש תיקיות — חמש קומות, מלמטה למעלה:
// basis (הבסיס) ← chomer (החומר) ← pkudot (הפקודות) ← lev (הלב) ← masachim (המסכים). כל קומה פונה רק למה שמתחתיה.
// שני שומרים, שעוצרים את הבנייה כשהכלל מופר:
//   א. כל קובץ יושב באחת מחמש התיקיות (המשתמש: "צריך לבדוק שכל הנושאים נכללו בחמישה האלה").
//   ב. פנייה "כלפי מעלה" (קובץ שמזכיר קובץ מקומה שמעליו) — רק מה שרשום ב־tests/mivne/lemala.txt. הרשימה רק מתקצרת:
//      פנייה חדשה — נכשל; פנייה שנפתרה ועדיין רשומה — נכשל, עד שמורידים את השורה.
// הספירה — לפי הטקסט: שם של קובץ אחר, אחרי הסרת הערות ומחרוזות; שם שאחרי נקודה (Menus.Tree), שם של מחלקה פנימית
// של הקובץ עצמו ושם שיובא מבחוץ (import) — לא נספרים.
// לכתוב את הרשימה מחדש (אחרי שפניות נפתרו): להריץ את הבדיקות עם ‎-Dvfm.mivne.write=1.

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class MivneTests {

    static final class Out {
        int passed;
        final List<String> failed = new ArrayList<String>();
    }

    /** הקומות, מלמטה למעלה. */
    static final List<String> LEVELS = Arrays.asList("basis", "chomer", "pkudot", "lev", "masachim");
    static final String CODE = "src/com/tikiyot/vfm", LIST = "tests/mivne/lemala.txt";

    private MivneTests() { }

    static Out run(File src) {
        Out o = new Out();
        File root = new File(src, CODE);
        Map<String, Integer> level = new HashMap<String, Integer>();
        Map<String, File> files = new java.util.TreeMap<String, File>();
        List<String> homeless = new ArrayList<String>();
        collect(root, root, files, level, homeless);

        // א. כל קובץ באחת מחמש התיקיות
        if (homeless.isEmpty() && files.size() > 100) o.passed++;
        else o.failed.add("קובץ שאינו באחת מחמש התיקיות (" + LEVELS + "): " + homeless + " · קבצים: " + files.size());

        // ב. פניות כלפי מעלה
        Set<String> now = new TreeSet<String>();
        int refs = 0;
        Pattern word = Pattern.compile("(?<![\\w.])([A-Z]\\w*)\\b");
        for (Map.Entry<String, File> e : files.entrySet()) {
            String me = e.getKey();
            Integer mine = level.get(me);
            if (mine == null) continue;
            String raw;
            try {
                raw = Files.readUtf8(e.getValue());
            } catch (Exception x) {
                o.failed.add("לא נקרא: " + me + " — " + x);
                continue;
            }
            Set<String> skip = new HashSet<String>();
            Matcher im = Pattern.compile("(?m)^import\\s+[\\w.]+\\.(\\w+)\\s*;").matcher(raw);
            while (im.find()) skip.add(im.group(1));
            String code = strip(raw);
            Matcher in = Pattern.compile("\\b(?:class|interface|enum)\\s+(\\w+)").matcher(code);
            while (in.find()) skip.add(in.group(1));
            Matcher m = word.matcher(code);
            while (m.find()) {
                String t = m.group(1);
                Integer other = level.get(t);
                if (other == null || t.equals(me) || skip.contains(t) || other <= mine) continue;
                now.add(me + " > " + t);
                refs++;
            }
        }
        File list = new File(src, LIST);
        if (System.getProperty("vfm.mivne.write") != null) write(list, now, refs, level);
        Set<String> allowed = new TreeSet<String>();
        try {
            for (String l : Files.readUtf8(list).replace("\r", "").split("\n")) {
                String t = l.trim();
                if (!t.isEmpty() && !t.startsWith("#")) allowed.add(t);
            }
        } catch (Exception x) {
            o.failed.add("רשימת הפניות כלפי מעלה לא נקראה (" + LIST + "): " + x);
            return o;
        }
        int before = o.failed.size();
        for (String p : now) if (!allowed.contains(p)) o.failed.add("פנייה חדשה כלפי מעלה: " + p + " " + where(p, level));
        if (o.failed.size() == before) o.passed++;
        before = o.failed.size();
        for (String p : allowed) if (!now.contains(p)) o.failed.add("פנייה שנפתרה — להוריד את השורה מ־" + LIST + ": " + p);
        if (o.failed.size() == before) o.passed++;
        return o;
    }

    /** "(pkudot ← masachim)" — מאיזו קומה לאיזו. */
    private static String where(String pair, Map<String, Integer> level) {
        String[] p = pair.split(" > ");
        return "(" + LEVELS.get(level.get(p[0])) + " ← " + LEVELS.get(level.get(p[1])) + ")";
    }

    private static void collect(File root, File dir, Map<String, File> files, Map<String, Integer> level, List<String> homeless) {
        File[] k = dir.listFiles();
        if (k == null) return;
        for (File x : k) {
            if (x.isDirectory()) {
                collect(root, x, files, level, homeless);
                continue;
            }
            if (!x.getName().endsWith(".java")) continue;
            String name = x.getName().replace(".java", "");
            files.put(name, x);
            String rel = x.getPath().substring(root.getPath().length() + 1).replace(File.separatorChar, '/');
            int cut = rel.indexOf('/');
            int lv = cut < 0 ? -1 : LEVELS.indexOf(rel.substring(0, cut));
            if (lv < 0) homeless.add(rel);
            else level.put(name, lv);
        }
    }

    /** הקוד בלי הערות ובלי מחרוזות (רווחים במקומן; השורות נשמרות). */
    static String strip(String src) {
        StringBuilder sb = new StringBuilder(src.length());
        int i = 0, n = src.length();
        while (i < n) {
            char c = src.charAt(i);
            if (src.startsWith("//", i)) {
                int j = src.indexOf('\n', i);
                i = j < 0 ? n : j;
                continue;
            }
            if (src.startsWith("/*", i)) {
                int j = src.indexOf("*/", i + 2);
                j = j < 0 ? n : j + 2;
                for (int q = i; q < j; q++) if (src.charAt(q) == '\n') sb.append('\n');
                i = j;
                continue;
            }
            if (c == '\'' || c == '"') {
                int j = i + 1;
                while (j < n && src.charAt(j) != c) j += src.charAt(j) == '\\' ? 2 : 1;
                sb.append(' ');
                i = j + 1;
                continue;
            }
            sb.append(c);
            i++;
        }
        return sb.toString();
    }

    private static void write(File list, Set<String> now, int refs, Map<String, Integer> level) {
        StringBuilder sb = new StringBuilder();
        sb.append("# פניות \"כלפי מעלה\": קובץ > קובץ מקומה שמעליו. הקומות, מלמטה למעלה: ").append(LEVELS).append("\n");
        sb.append("# הרשימה רק מתקצרת (MivneTests): פנייה חדשה — הבדיקות נכשלות; פנייה שנפתרה — מורידים את השורה.\n");
        sb.append("# נכתב מהקוד: ").append(now.size()).append(" זוגות, ").append(refs).append(" הופעות.\n");
        final Map<String, Integer> lv0 = level;
        List<String> sorted = new ArrayList<String>(now);
        java.util.Collections.sort(sorted, new java.util.Comparator<String>() {
            @Override public int compare(String x, String y) {
                String[] p = x.split(" > "), q = y.split(" > ");
                int c = lv0.get(p[0]) - lv0.get(q[0]);
                if (c == 0) c = lv0.get(p[1]) - lv0.get(q[1]);
                return c != 0 ? c : x.compareTo(y);
            }
        });
        String last = "";
        for (String p : sorted) {
            String lv = where(p, level);
            if (!lv.equals(last)) sb.append("\n# ").append(lv).append("\n");
            last = lv;
            sb.append(p).append("\n");
        }
        try {
            list.getParentFile().mkdirs();
            Files.writeAtomic(list, sb.toString());
            System.out.println("מבנה: נכתבה הרשימה — " + now.size() + " זוגות, " + refs + " הופעות");
        } catch (Exception x) {
            System.out.println("מבנה: הרשימה לא נכתבה — " + x);
        }
    }
}
