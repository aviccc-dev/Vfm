package com.tikiyot.vfm;

// 1.23.0: הפקודות הנפוצות (claude/mimshak-pkudot.md, החלטה 4): של התוכנה — מתוך ההתקנה (assets/commands/templates.txt),
// קריאה בלבד, מתעדכנות עם הגרסה; שלי — קובצי פקודות בתיקייה "פקודות נפוצות" שבתיקיית התוכנה, שעדכון גרסה לא נוגע בה.
// תבנית — שורה אחת או כמה, עם מקומות פתוחים (<מקור>, <יעד>…) שנשאלים לפני ההכנסה. בלי ממשק — נבדק.
// 1.23.1: קובץ הבדיקה של 1.23.0 — יצא; removeOldTest מוחק אותו מהמכשיר (בגרסה הבאה — למחוק גם אותה).

import android.content.Context;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class CmdTemplates {

    static final String ASSET = "commands/templates.txt";
    static final String MINE_DIR = Paths.USER_DIRS[0], TEST_DIR = Paths.USER_DIRS[1], TEST_FILE = "בדיקת הפקודות 1.23.0";  // בכונן
    private static final String HEAD = "=== ";   // תחביר
    /** מקום פתוח: <שם> — בלי רווח, בלי / \ : % (כך /תאריך:<שנה לא נתפס). */
    private static final Pattern HOLE = Pattern.compile("<([^<>\\s/\\\\:%]+)>");

    /** בבדיקות — תיקייה אחרת. */
    static File testRoot;

    static final class Tpl {
        final String name;
        final List<String> lines;
        /** של המשתמש — הקובץ; של התוכנה — null. */
        final File file;

        Tpl(String name, List<String> lines, File file) {
            this.name = name;
            this.lines = lines;
            this.file = file;
        }
    }

    private CmdTemplates() { }

    /** "=== שם" ואחריו השורות. */
    static List<Tpl> parse(String text) {
        List<Tpl> out = new ArrayList<Tpl>();
        String name = null;
        List<String> cur = null;
        for (String l : (text == null ? "" : text).replace("\r", "").split("\n")) {
            if (l.startsWith(HEAD)) {
                if (name != null && !cur.isEmpty()) out.add(new Tpl(name, cur, null));
                name = l.substring(HEAD.length()).trim();
                cur = new ArrayList<String>();
            } else if (cur != null && !l.trim().isEmpty()) {
                cur.add(l.trim());
            }
        }
        if (name != null && !cur.isEmpty()) out.add(new Tpl(name, cur, null));
        return out;
    }

    static List<Tpl> system(Context c) {
        try {
            return parse(Examples.asset(c, ASSET));
        } catch (Throwable t) {
            Log.e("הפקודות הנפוצות של התוכנה", t);
            return new ArrayList<Tpl>();
        }
    }

    private static File root() {
        return testRoot != null ? testRoot : Paths.data();
    }

    static File mineDir() {
        return new File(root(), MINE_DIR);
    }

    /** שלי — לפי שם. */
    static List<Tpl> mine() {
        List<Tpl> out = new ArrayList<Tpl>();
        File[] fs = mineDir().listFiles();
        if (fs == null) return out;
        Arrays.sort(fs, new java.util.Comparator<File>() {                     // לפי השם בלי הסיומת ("שלי" לפני "שלי 2")
            @Override public int compare(File a, File b) { return Text.stem(a.getName()).compareTo(Text.stem(b.getName())); }
        });
        for (File f : fs) {
            if (!f.isFile() || !f.getName().endsWith("." + Batch.EXT)) continue;
            try {
                List<String> lines = new ArrayList<String>();
                for (String l : Files.readUtf8(f).replace("\r", "").split("\n")) if (!l.trim().isEmpty()) lines.add(l.trim());
                if (!lines.isEmpty()) out.add(new Tpl(Text.stem(f.getName()), lines, f));
            } catch (Throwable t) {
                Log.e("פקודה נפוצה: " + f, t);
            }
        }
        return out;
    }

    /** שומר שורות כפקודה נפוצה שלי. שם תפוס — מספר בסוף. null — לא נשמר. */
    static File save(String name, List<String> lines) {
        String n = Physical.badName(name) == null ? name.trim() : name.trim().replaceAll("[\\\\/:*?\"<>|]", "_");
        if (n.isEmpty() || lines.isEmpty()) return null;
        File dir = mineDir();
        dir.mkdirs();
        File f = new File(dir, n + "." + Batch.EXT);
        for (int i = 2; f.exists(); i++) f = new File(dir, n + " " + i + "." + Batch.EXT);
        StringBuilder sb = new StringBuilder();
        for (String l : lines) sb.append(l).append('\n');
        try {
            Files.writeAtomic(f, sb.toString());
            return f;
        } catch (Throwable t) {
            Log.e("שמירת פקודה נפוצה", t);
            return null;
        }
    }

    /** 1.25.3: עריכת נפוצה של התוכנה — עותק אצל "שלי" (המקור נשאר ומתעדכן עם הגרסאות). */
    static File copyToMine(Tpl t) {
        return save(t.name + Words.Ed.MINE_SUFFIX, t.lines);
    }

    /** 1.25.3: שינוי שם לנפוצה שלי. null — לא הצליח (שם לא תקין או תפוס). */
    static File rename(Tpl t, String name) {
        if (t.file == null || name == null) return null;
        String n = name.trim();
        if (n.isEmpty() || Physical.badName(n) != null) return null;
        File to = new File(t.file.getParentFile(), n + "." + Batch.EXT);
        if (to.exists() && !to.getName().equalsIgnoreCase(t.file.getName())) return null;
        return t.file.renameTo(to) ? to : null;
    }

    /** המקומות הפתוחים, בסדר, כל אחד פעם אחת. */
    static List<String> holes(List<String> lines) {
        List<String> out = new ArrayList<String>();
        for (String l : lines) {
            Matcher m = HOLE.matcher(l);
            while (m.find()) if (!out.contains(m.group(1))) out.add(m.group(1));
        }
        return out;
    }

    /** ממלא את המקומות הפתוחים. מקום שלא מולא — נשאר כמו שהוא. */
    /** 1.25.2: סוג המקום הפתוח — לפי המילה שלפניו בשורה: מסלול (ברירת מחדל), רשימה ידנית, רשימה חכמה, מספר. */
    static final int HOLE_PATH = 0, HOLE_MANUAL = 1, HOLE_SMART = 2, HOLE_NUMBER = 3, HOLE_MANY = 4;
    /** 1.26.0: מקום ששמו מתחיל כך, אחרי "ידנית" — כמה רשימות. */
    static final String MANY_PREFIX = "רשימות";   // תחביר

    static int holeKind(List<String> lines, String hole) {
        String mark = "<" + hole + ">";
        for (String l : lines) {
            if (!l.contains(mark)) continue;
            List<ListCmd.Tok> tk = ListCmd.tokens(l);
            for (int i = 0; i < tk.size(); i++) {
                if (!tk.get(i).text.contains(mark)) continue;
                if (i == 1 && tk.get(0).text.equals(Batch.LOOP)) return HOLE_NUMBER;
                if (i > 0 && !tk.get(i - 1).quoted) {
                    String w = tk.get(i - 1).text;
                    if (w.equals(ListCmd.MANUAL)) return hole.startsWith(MANY_PREFIX) ? HOLE_MANY : HOLE_MANUAL;
                    if (w.equals(ListCmd.SMART)) return HOLE_SMART;
                }
            }
        }
        return HOLE_PATH;
    }

    static List<String> fill(List<String> lines, Map<String, String> values) {
        List<String> out = new ArrayList<String>();
        for (String l : lines) {
            String multi = null;                                               // 1.26.0: מקום עם כמה ערכים — השורה מוכפלת
            Matcher f = HOLE.matcher(l);
            while (f.find() && multi == null) {
                String v = values.get(f.group(1));
                if (v != null && many(v).size() > 1) multi = f.group(1);
            }
            List<String> copies = multi == null ? java.util.Collections.singletonList((String) null) : many(values.get(multi));
            for (String one : copies) {
                Matcher m = HOLE.matcher(l);
                StringBuffer sb = new StringBuffer();
                while (m.find()) {
                    String v = m.group(1).equals(multi) ? one : values.get(m.group(1));
                    if (v != null && multi == null && many(v).size() == 1) v = many(v).get(0);
                    m.appendReplacement(sb, Matcher.quoteReplacement(v == null || v.trim().isEmpty() ? m.group() : v.trim()));
                }
                m.appendTail(sb);
                out.add(sb.toString());
            }
        }
        return out;
    }

    /** 1.26.0: ערך של כמה — שורה לכל אחד (ריקות יורדות). */
    static List<String> many(String v) {
        List<String> out = new ArrayList<String>();
        if (v != null) for (String x : v.replace("\r", "").split("\n")) if (!x.trim().isEmpty()) out.add(x.trim());
        return out;
    }

    static Map<String, String> empty(List<String> holes) {
        Map<String, String> m = new LinkedHashMap<String, String>();
        for (String h : holes) m.put(h, "");
        return m;
    }
}
