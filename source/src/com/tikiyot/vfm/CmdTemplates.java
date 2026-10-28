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
    static final String MINE_DIR = "פקודות נפוצות", TEST_DIR = "בדיקות", TEST_FILE = "בדיקת הפקודות 1.23.0";  // בכונן
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
    static List<String> fill(List<String> lines, Map<String, String> values) {
        List<String> out = new ArrayList<String>();
        for (String l : lines) {
            Matcher m = HOLE.matcher(l);
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                String v = values.get(m.group(1));
                m.appendReplacement(sb, Matcher.quoteReplacement(v == null || v.trim().isEmpty() ? m.group() : v.trim()));
            }
            m.appendTail(sb);
            out.add(sb.toString());
        }
        return out;
    }

    static Map<String, String> empty(List<String> holes) {
        Map<String, String> m = new LinkedHashMap<String, String>();
        for (String h : holes) m.put(h, "");
        return m;
    }

    /**
     * 1.23.1 (המשתמש: "תזכור למחוק בדיקות קודמות"): קובץ הבדיקה של 1.23.0 נמחק מהמכשיר — רק אם לא שונה (אותו
     * גודל ואותו CRC32 כמו שנכתב); ששונה — נשאר. התיקייה "בדיקות" — רק אם התרוקנה. בגרסה הבאה — למחוק את הפונקציה.
     */
    static boolean removeOldTest() {
        File dir = new File(root(), TEST_DIR), f = new File(dir, TEST_FILE + "." + Batch.EXT), mark = new File(dir, ".1.23.0");
        boolean gone = false;
        try {
            if (f.isFile() && f.length() == OLD_TEST_LEN) {
                java.util.zip.CRC32 crc = new java.util.zip.CRC32();                   // בלי java.nio.file — לא קיים לפני אנדרואיד 8
                java.io.InputStream in = new java.io.FileInputStream(f);
                try {
                    byte[] b = new byte[4096];
                    for (int n; (n = in.read(b)) > 0; ) crc.update(b, 0, n);
                } finally {
                    in.close();
                }
                if (crc.getValue() == OLD_TEST_CRC) gone = f.delete();
            }
            if (!f.exists()) {
                mark.delete();
                String[] left = dir.list();
                if (left != null && left.length == 0) dir.delete();
                Library lib = Library.peek();
                if (lib != null && gone) lib.fileRemoved(f.getAbsolutePath());
            }
        } catch (Throwable t) {
            Log.e("מחיקת קובץ הבדיקה 1.23.0", t);
        }
        return gone;
    }

    private static final long OLD_TEST_LEN = 1573, OLD_TEST_CRC = 3112421943L;
}
