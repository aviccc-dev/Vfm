package com.tikiyot.vfm;

// 1.12.0: עיצוב עם שם — קובץ טקסט "<שם>.עיצוב.txt" בתיקיית "עיצובים" של האופן (ר' itzuv.md).
// אחראי: לקרוא, לכתוב ולמצוא עיצובים, הירושה (רשימה ← הרשימה שמעליה ← מסך הבית ← ברירת המחדל),
// והחלק הפרטי — צלמית וצבע לכל רשימה לפי המסלול שלה. בלי ממשק: את המראה מצייר Looks.
// מה שחסר בקובץ — נלקח מ"עיצוב ברירת מחדל"; שורה לא מובנת — מתעלמים ממנה (לא נשבר).

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class Design {

    static final String EXT = ".עיצוב.txt";  // בכונן
    static final String DIR = "עיצובים";  // בכונן
    /** העיצובים המובנים — תמיד זמינים; נכתבים לתיקיית האופן בפעם הראשונה שבוחרים בהם. */
    static final String DEFAULT = "עיצוב ברירת מחדל", FILES = "תוכנת ניהול קבצים", WORDS = "מילים וצבע";  // בכונן
    /** בהגדרות הכלליות של האופן: העיצוב של מסך הבית. */
    static final String HOME_KEY = "עיצוב מסך הבית";  // תחביר

    // ---------- התחביר של הקובץ ----------
    static final String S_TITLE = "כותרת המסך", S_MID = "אמצע", S_LISTS = "רשימות";  // תחביר
    static final String K_STYLE = "סגנון", GRID = "רשת", ROWS = "שורות", K_COLS = "טורים", K_ICON = "צלמית",  // תחביר
            K_GAP = "ריווח", K_SHOW_ICON = "הצג צלמית", K_AROUND = "סביב הצלמית", K_TINT = "צבע רקע לפריט",  // תחביר
            K_SHOW_DATA = "הצג נתונים", K_DATA_AT = "מקום הנתונים", BELOW = "מתחת", SIDE = "בצד",  // תחביר
            K_FIT = "התאם למקום", K_MIN = "גודל מזערי", K_LINES = "שורות לשם";  // תחביר
    static final String NONE = "בלי", FRAME = "מסגרת", FILL = "ריבוע צבעוני";  // תחביר
    static final String T_NAME = "שם", T_DATA = "נתונים", K_SIZE = "גודל", K_WEIGHT = "משקל", K_FONT = "גופן";  // תחביר
    static final String YES = "כן", NO = "לא", SYSTEM = "מערכת";  // תחביר
    static final String W_LIGHT = "דק", W_REG = "רגיל", W_MED = "בינוני", W_BOLD = "מודגש";  // תחביר
    static final String P_ICON = "צלמית", P_COLOR = "צבע", P_SEP = "·";  // תחביר

    /** סגנון טקסט: גודל (sp), משקל (100–900), גופן ("מערכת" או שם קובץ בתיקיית "גופנים"). */
    static final class Text {
        float size;
        int weight;
        String font;

        Text(float size, int weight, String font) {
            this.size = size;
            this.weight = weight;
            this.font = font;
        }

        Text copy() {
            return new Text(size, weight, font);
        }
    }

    String name = DEFAULT;
    File file;
    /** האזור האמצעי (הרשימות). */
    boolean grid;
    int cols = 1;
    /** רשת: אחוז מרוחב האריח. שורות: אחוז מגודל הטקסט של השם (החלטה 4). */
    int icon = 340;
    /** רשת: אחוז מרוחב האריח. שורות: אחוז מגודל הטקסט של השם — הריווח למעלה ולמטה. */
    int gap = 50;
    boolean showIcon = true;
    String around = FILL;
    boolean tint;
    boolean showData = true;
    boolean dataBelow = true;
    Text title = new Text(12, 400, SYSTEM), nameT = new Text(16, 400, SYSTEM), data = new Text(11, 400, SYSTEM);
    boolean fit;
    int min = 11;
    int lines = 2;
    /** החלק הפרטי: מסלול הרשימה באופן ("לימוד/הערות") ← {צלמית, צבע}. */
    final Map<String, String[]> lists = new LinkedHashMap<String, String[]>();

    /** סימן לזיהוי שינוי (המסך נבנה מחדש רק כשהמראה השתנה). */
    String signature() {
        return grid + "|" + cols + "|" + icon + "|" + gap + "|" + showIcon + "|" + around + "|" + tint + "|" + showData
                + "|" + dataBelow + "|" + nameT.size + nameT.weight + nameT.font + "|" + data.size + data.weight + data.font
                + "|" + fit + "|" + min + "|" + lines;
    }

    // ---------- קריאה ----------

    static Design parse(String name, String text) {
        Design d = new Design();
        d.name = name;
        String section = "";
        for (String raw : (text == null ? "" : text).replace("﻿", "").split("\r\n|\r|\n")) {
            String t = raw.trim();
            if (t.isEmpty() || t.startsWith(";") || t.startsWith("#")) continue;
            if (t.startsWith("[") && t.endsWith("]")) {
                section = t.substring(1, t.length() - 1).trim();
                continue;
            }
            int eq = t.indexOf('=');
            if (eq <= 0) continue;
            String key = t.substring(0, eq).trim(), value = t.substring(eq + 1).trim();
            if (S_LISTS.equals(section)) {
                d.lists.put(key, privateValue(value));
                continue;
            }
            int sc = value.indexOf(';');                                     // הערה בסוף השורה
            if (sc >= 0) value = value.substring(0, sc).trim();
            if (value.isEmpty()) continue;
            try {
                if (S_TITLE.equals(section)) d.text(d.title, key, value);
                else if (S_MID.equals(section)) d.mid(key, value);
            } catch (RuntimeException ignored) {                             // ערך לא מובן — נשאר מברירת המחדל
            }
        }
        return d;
    }

    private void mid(String key, String v) {
        if (key.equals(K_STYLE)) grid = v.equals(GRID);
        else if (key.equals(K_COLS)) cols = clamp(num(v), 1, 8);
        else if (key.equals(K_ICON)) icon = clamp(num(v), 0, 1000);
        else if (key.equals(K_GAP)) gap = clamp(num(v), 0, 200);
        else if (key.equals(K_SHOW_ICON)) showIcon = !v.equals(NO);
        else if (key.equals(K_AROUND)) around = v.equals(NONE) || v.equals(FRAME) ? v : FILL;
        else if (key.equals(K_TINT)) tint = v.equals(YES);
        else if (key.equals(K_SHOW_DATA)) showData = !v.equals(NO);
        else if (key.equals(K_DATA_AT)) dataBelow = !v.equals(SIDE);
        else if (key.equals(K_FIT)) fit = v.equals(YES);
        else if (key.equals(K_MIN)) min = clamp(num(v), 6, 40);
        else if (key.equals(K_LINES)) lines = clamp(num(v), 1, 6);
        else {
            int c = key.indexOf(':');
            if (c <= 0) return;
            String who = key.substring(0, c).trim(), what = key.substring(c + 1).trim();
            if (who.equals(T_NAME)) text(nameT, what, v);
            else if (who.equals(T_DATA)) text(data, what, v);
        }
    }

    private void text(Text t, String what, String v) {
        if (what.equals(K_SIZE)) t.size = clamp(num(v), 6, 60);
        else if (what.equals(K_WEIGHT)) t.weight = weight(v);
        else if (what.equals(K_FONT)) t.font = v;
    }

    static int weight(String v) {
        if (v.equals(W_LIGHT)) return 300;
        if (v.equals(W_REG)) return 400;
        if (v.equals(W_MED)) return 500;
        if (v.equals(W_BOLD)) return 700;
        return clamp(num(v), 100, 900);
    }

    static String weightName(int w) {
        return w <= 300 ? W_LIGHT : w < 500 ? W_REG : w < 600 ? W_MED : W_BOLD;
    }

    private static int num(String v) {
        return Integer.parseInt(v.replace("%", "").trim());
    }

    private static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : v > hi ? hi : v;
    }

    /** "צלמית 📚 · צבע ירוק" ← {"📚", "ירוק"}. */
    static String[] privateValue(String v) {
        String icon = "", color = "";
        for (String part : v.split(P_SEP)) {
            String p = part.trim();
            if (p.startsWith(P_ICON)) icon = p.substring(P_ICON.length()).trim();
            else if (p.startsWith(P_COLOR)) color = p.substring(P_COLOR.length()).trim();
        }
        return new String[]{icon, color};
    }

    static String privateText(String icon, String color) {
        StringBuilder sb = new StringBuilder();
        if (icon != null && !icon.isEmpty()) sb.append(P_ICON).append(' ').append(icon);
        if (color != null && !color.isEmpty()) sb.append(sb.length() > 0 ? " " + P_SEP + " " : "").append(P_COLOR).append(' ').append(color);
        return sb.toString();
    }

    // ---------- החלק הפרטי ----------

    static String key(List<String> path) {
        return Tree.key(path);
    }

    /** הצלמית של הרשימה: מהעיצוב (לפי המסלול); אם אין — מה שנשמר ברשימה ב־1.11.0; אחרת "". */
    String iconOf(List<String> path, String old) {
        String[] p = lists.get(key(path));
        if (p != null && !p[0].isEmpty()) return p[0];
        return old == null ? "" : old;
    }

    String colorOf(List<String> path) {
        String[] p = lists.get(key(path));
        return p == null ? "" : p[1];
    }

    /** כתיבת שורה בחלק הפרטי (צלמית וצבע ריקים — השורה יוצאת). שאר הקובץ — כמו שהוא. */
    static boolean setPrivate(File f, String path, String icon, String color) {
        try {
            String text = f.isFile() ? Files.readUtf8(f) : "";
            String line = privateText(icon, color);
            Files.writeAtomic(f, withPrivate(text, path, line.isEmpty() ? null : path + " = " + line));
            return true;
        } catch (Throwable t) {
            Log.e("שמירת צלמית בעיצוב", t);
            return false;
        }
    }

    /** הטקסט אחרי החלפת השורה של path בחלק הפרטי (newLine == null — מחיקה). */
    static String withPrivate(String text, String path, String newLine) {
        return withLine(text, S_LISTS, path, newLine);
    }

    private static String join(List<String> lines) {
        StringBuilder sb = new StringBuilder();
        for (String l : lines) sb.append(l).append('\n');
        return sb.toString();
    }

    /** 88: רשימה שזזה או שקיבלה שם חדש — השורות שלה (ושל מה שבתוכה) בכל עיצובי האופן עוברות איתה. */
    static int listMoved(List<String> from, List<String> to) {
        if (from == null || from.isEmpty() || from.equals(to)) return 0;
        String a = key(from), b = key(to);
        int n = 0;
        for (File f : files(dir())) {
            try {
                String text = Files.readUtf8(f);
                String out = renamed(text, a, b);
                if (!out.equals(text)) {
                    Files.writeAtomic(f, out);
                    n++;
                }
            } catch (Throwable t) {
                Log.e("עדכון עיצוב אחרי העברת רשימה", t);
            }
        }
        return n;
    }

    static String renamed(String text, String a, String b) {
        StringBuilder sb = new StringBuilder();
        String section = "";
        String[] lines = text.replace("\r", "").split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String l = lines[i], t = l.trim();
            if (t.startsWith("[") && t.endsWith("]")) section = t.substring(1, t.length() - 1).trim();
            else if (S_LISTS.equals(section)) {
                int eq = l.indexOf('=');
                if (eq > 0) {
                    String k = l.substring(0, eq).trim();
                    if (k.equals(a) || k.startsWith(a + "/")) l = b + k.substring(a.length()) + " =" + l.substring(eq + 1);
                }
            }
            sb.append(l);
            if (i < lines.length - 1) sb.append('\n');
        }
        return sb.toString();
    }

    // ---------- איפה ----------

    static File dir() {
        return new File(Paths.settings(), DIR);
    }

    static File fileOf(String name) {
        return new File(dir(), name + EXT);
    }

    static String nameOf(File f) {
        String n = f.getName();
        return n.endsWith(EXT) ? n.substring(0, n.length() - EXT.length()) : n;
    }

    static List<File> files(File dir) {
        List<File> out = new ArrayList<File>();
        File[] kids = dir.listFiles();
        if (kids != null) for (File k : kids) if (k.isFile() && k.getName().endsWith(EXT)) out.add(k);
        java.util.Collections.sort(out);
        return out;
    }

    static boolean builtIn(String name) {
        return DEFAULT.equals(name) || FILES.equals(name) || WORDS.equals(name);
    }

    /**
     * כל העיצובים שאפשר לבחור: {שם, מאיפה ("" — האופן הזה; שם אופן; או שם תיקייה), מסלול הקובץ ("" — מובנה שעוד לא נכתב)}.
     * found — קובצי עיצוב שהאינדקס מצא בכונן (לפי הסיומת), מחוץ לאופנים.
     */
    static List<String[]> all(List<String> found) {
        List<String[]> out = new ArrayList<String[]>();
        List<String> here = new ArrayList<String>();
        for (File f : files(dir())) {
            out.add(new String[]{nameOf(f), "", f.getAbsolutePath()});
            here.add(nameOf(f));
        }
        for (String b : new String[]{DEFAULT, FILES, WORDS})
            if (!here.contains(b)) out.add(new String[]{b, "", ""});
        String cur = Modes.current();
        for (String m : Modes.all()) {
            if (m.equals(cur)) continue;
            for (File f : files(new File(new File(Modes.dirOf(m), Paths.SETTINGS), DIR)))
                out.add(new String[]{nameOf(f), m, f.getAbsolutePath()});
        }
        String modes = new File(Paths.data(), Modes.DIR).getAbsolutePath() + "/";
        if (found != null)
            for (String p : found) {
                if (p.startsWith(modes)) continue;                           // כבר נמנו למעלה
                File f = new File(p);
                if (f.isFile()) out.add(new String[]{nameOf(f), f.getParentFile().getName(), p});
            }
        return out;
    }

    /**
     * עיצוב מאופן אחר — מועתק לאופן הזה (כל אופן מחזיק את העיצובים שלו). מחזיר את השם שלו כאן.
     * שם תפוס בתוכן אחר — מוסיפים את שם האופן.
     */
    static String adopt(String[] picked) {
        if (picked[1].isEmpty()) {
            if (picked[2].isEmpty()) ensure(picked[0]);
            return picked[0];
        }
        try {
            String text = Files.readUtf8(new File(picked[2]));
            String name = picked[0];
            File f = fileOf(name);
            if (f.isFile() && !Files.readUtf8(f).equals(text)) {
                name = picked[0] + " (" + picked[1] + ")";
                f = fileOf(name);
            }
            dir().mkdirs();
            if (!f.isFile()) Files.writeAtomic(f, text);
            return name;
        } catch (Throwable t) {
            Log.e("העתקת עיצוב מאופן אחר", t);
            return null;
        }
    }

    /** קובץ של עיצוב מובנה — נכתב אם חסר. */
    static File ensure(String name) {
        File f = fileOf(name);
        if (!f.isFile() && builtIn(name)) {
            try {
                dir().mkdirs();
                Files.writeAtomic(f, builtInText(name));
            } catch (Throwable t) {
                Log.e("כתיבת עיצוב מובנה", t);
            }
        }
        return f;
    }

    /** "חזרה לברירת מחדל": המראה מהמקור; הצלמיות והצבעים של הרשימות — נשארים. */
    static boolean reset(String name) {
        if (!builtIn(name)) return false;
        try {
            File f = fileOf(name);
            Design old = f.isFile() ? read(f, name) : null;
            String text = builtInText(name);
            if (old != null)
                for (Map.Entry<String, String[]> e : old.lists.entrySet())
                    text = withPrivate(text, e.getKey(), e.getKey() + " = " + privateText(e.getValue()[0], e.getValue()[1]));
            dir().mkdirs();
            Files.writeAtomic(f, text);
            return true;
        } catch (Throwable t) {
            Log.e("חזרה לברירת מחדל — עיצוב", t);
            return false;
        }
    }

    /** "שמירה בשם": עותק של הקובץ (כולל הצלמיות) בשם חדש. null — הצליח; אחרת הודעה. */
    static String saveAs(Design from, String newName) {
        String n = newName.trim().replace('/', '∕');
        if (n.isEmpty()) return Words.NEED_NAME;
        File f = fileOf(n);
        if (f.exists()) return Words.DesignW.EXISTS;
        try {
            File src = from.file != null && from.file.isFile() ? from.file : ensure(from.name);
            dir().mkdirs();
            Files.writeAtomic(f, src.isFile() ? Files.readUtf8(src) : builtInText(DEFAULT));
            return null;
        } catch (Throwable t) {
            Log.e("שמירת עיצוב בשם", t);
            return Words.SAVE_FAILED;
        }
    }

    // ---------- הטופס (1.12.1): כתיבת המראה הכללי ----------

    /** הטקסט אחרי קביעת key = value בקבוצה section (שורה קיימת — מוחלפת; חסרה — נוספת בסוף הקבוצה). */
    static String withKey(String text, String section, String key, String value) {
        return withLine(text, section, key, key + " = " + value);
    }

    /**
     * 1.14.0: השורה של key בקבוצה section — מוחלפת ב-line, נוספת בסוף הקבוצה, או (line == null) נמחקת. קבוצה חסרה —
     * נוספת לפני [רשימות] (והיא עצמה — בסוף). הערות (";") — לא נוגעים. (עד כאן — שתי העתקות כמעט זהות.)
     */
    private static String withLine(String text, String section, String key, String line) {
        List<String> lines = new ArrayList<String>();
        for (String l : text.replace("\r", "").split("\n", -1)) lines.add(l);
        while (!lines.isEmpty() && lines.get(lines.size() - 1).trim().isEmpty()) lines.remove(lines.size() - 1);
        int start = -1, end = lines.size();
        for (int i = 0; i < lines.size(); i++) {
            String t = lines.get(i).trim();
            if (t.startsWith("[") && t.endsWith("]")) {
                if (start >= 0) {
                    end = i;
                    break;
                }
                if (t.substring(1, t.length() - 1).trim().equals(section)) start = i;
            }
        }
        if (start < 0) {
            if (line == null) return join(lines);
            int at = 0;                                                       // לפני [רשימות], אם יש
            while (at < lines.size() && !lines.get(at).trim().equals("[" + S_LISTS + "]")) at++;
            List<String> add = new ArrayList<String>();
            if (at > 0) add.add("");
            add.add("[" + section + "]");
            add.add(line);
            if (at < lines.size()) add.add("");
            lines.addAll(at, add);
            return join(lines);
        }
        for (int i = start + 1; i < end; i++) {
            String t = lines.get(i);
            int eq = t.indexOf('=');
            if (eq > 0 && !t.trim().startsWith(";") && t.substring(0, eq).trim().equals(key)) {
                if (line == null) lines.remove(i);
                else lines.set(i, line);
                return join(lines);
            }
        }
        if (line != null) {
            int at = end;
            while (at > start + 1 && lines.get(at - 1).trim().isEmpty()) at--;
            lines.add(at, line);
        }
        return join(lines);
    }

    /** המראה הכללי של d — לקובץ שלו (ההערות והחלק הפרטי נשארים). */
    static boolean save(Design d) {
        try {
            File f = d.file != null ? d.file : fileOf(d.name);
            String text = f.isFile() ? Files.readUtf8(f) : "";
            text = withKey(text, S_TITLE, K_SIZE, num(d.title.size));
            text = withKey(text, S_TITLE, K_WEIGHT, weightName(d.title.weight));
            text = withKey(text, S_TITLE, K_FONT, d.title.font);
            String[][] mid = {{K_STYLE, d.grid ? GRID : ROWS}, {K_COLS, "" + d.cols}, {K_ICON, d.icon + "%"},
                    {K_GAP, d.gap + "%"}, {K_SHOW_ICON, d.showIcon ? YES : NO}, {K_AROUND, d.around},
                    {K_TINT, d.tint ? YES : NO}, {K_SHOW_DATA, d.showData ? YES : NO}, {K_DATA_AT, d.dataBelow ? BELOW : SIDE},
                    {T_NAME + ": " + K_SIZE, num(d.nameT.size)}, {T_NAME + ": " + K_WEIGHT, weightName(d.nameT.weight)},
                    {T_NAME + ": " + K_FONT, d.nameT.font}, {T_DATA + ": " + K_SIZE, num(d.data.size)},
                    {T_DATA + ": " + K_WEIGHT, weightName(d.data.weight)}, {T_DATA + ": " + K_FONT, d.data.font},
                    {K_FIT, d.fit ? YES : NO}, {K_MIN, "" + d.min}, {K_LINES, "" + d.lines}};
            for (String[] kv : mid) text = withKey(text, S_MID, kv[0], kv[1]);
            dir().mkdirs();
            Files.writeAtomic(f, text);
            return true;
        } catch (Throwable t) {
            Log.e("שמירת עיצוב מהטופס", t);
            return false;
        }
    }

    private static String num(float v) {
        return v == (int) v ? "" + (int) v : "" + v;
    }

    /** "שינוי שם": הקובץ, הרשימות שמפנות אליו ומסך הבית. null — הצליח; אחרת הודעה. */
    static String rename(final String from, String to) {
        final String n = to.trim().replace('/', '∕');
        if (n.isEmpty()) return Words.NEED_NAME;
        if (n.equals(from)) return null;
        File a = fileOf(from), b = fileOf(n);
        if (b.exists()) return Words.DesignW.EXISTS;
        if (!a.isFile() || !a.renameTo(b)) return Words.SAVE_FAILED;
        renameRefs(from, n);
        return null;
    }

    /** כל מה שמפנה לעיצוב from — מפנה עכשיו ל־to ("" — יורש). */
    static void renameRefs(final String from, final String to) {
        Tree.edit(new Tree.Edit() {
            @Override
            public boolean apply(Tree t) {
                return renameIn(t.root, from, to);
            }
        });
        if (from.equals(homeName())) setHome(to);
    }

    private static boolean renameIn(Tree.Node n, String from, String to) {
        boolean changed = false;
        if (n.design.equals(from)) {
            n.design = to;
            changed = true;
        }
        for (Tree.Node c : n.children) changed |= renameIn(c, from, to);
        return changed;
    }

    /** מחיקת עיצוב: הקובץ לגיבויים; מה שהפנה אליו — יורש (ומגיע בסוף לברירת המחדל). */
    static boolean delete(String name) {
        File f = fileOf(name);
        if (!f.isFile()) return false;
        Files.backup(f, DIR);
        if (!f.delete()) return false;
        if (!builtIn(name)) renameRefs(name, "");
        return true;
    }

    // ---------- טעינה, עם זיכרון ----------

    private static final Map<String, Design> cache = new java.util.HashMap<String, Design>();
    private static final Map<String, Long> stamp = new java.util.HashMap<String, Long>();

    static synchronized Design read(File f, String name) {
        String k = f.getAbsolutePath();
        Design d = cache.get(k);
        if (d != null && stamp.get(k) == f.lastModified()) return d;
        try {
            d = parse(name, Files.readUtf8(f));
        } catch (Throwable t) {
            Log.e("קריאת עיצוב " + f.getName(), t);
            d = parse(name, builtInText(DEFAULT));
        }
        d.file = f;
        cache.put(k, d);
        stamp.put(k, f.lastModified());
        return d;
    }

    /** העיצוב בשם הזה: מהאופן הזה; מובנה — נכתב; לא נמצא — "עיצוב ברירת מחדל" (לא נשבר). */
    static Design get(String name) {
        if (name == null || name.trim().isEmpty()) name = DEFAULT;
        File f = builtIn(name) ? ensure(name) : fileOf(name);
        if (f.isFile()) return read(f, name);
        if (!DEFAULT.equals(name)) return get(DEFAULT);
        Design d = parse(DEFAULT, builtInText(DEFAULT));
        d.file = f;
        return d;
    }

    // ---------- הירושה ----------

    /** העיצוב שנבחר למסך הבית; "" — לא נבחר (מסך הבית נשאר כמו שהיה, והרשימות — "עיצוב ברירת מחדל"). */
    static String homeName() {
        return Conf.get(Conf.GENERAL, HOME_KEY, "");
    }

    static void setHome(String name) {
        Conf.set(Conf.GENERAL, HOME_KEY, name);
    }

    /** שם העיצוב של רשימה: שלה; אם אין — של הרשימה שמעליה; …; ואז מסך הבית (החלטות 1 ו־5). */
    static String nameFor(Tree t, List<String> path) {
        for (int i = path.size(); i >= 0; i--) {
            Tree.Node n = t.find(path.subList(0, i));
            if (n != null && !n.design.isEmpty()) return n.design;
        }
        String home = homeName();
        return home.isEmpty() ? DEFAULT : home;
    }

    /** מאיפה הגיע העיצוב: null — נבחר לרשימה עצמה; אחרת שם הרשימה שמעליה, או "" — מסך הבית. */
    static String inheritedFrom(Tree t, List<String> path) {
        Tree.Node self = t.find(path);
        if (self != null && !self.design.isEmpty()) return null;
        for (int i = path.size() - 1; i >= 0; i--) {
            Tree.Node n = t.find(path.subList(0, i));
            if (n != null && !n.design.isEmpty()) return i == 0 ? Words.MANUAL_LISTS : path.get(i - 1);
        }
        return "";
    }

    static Design forList(Tree t, List<String> path) {
        return get(nameFor(t, path));
    }

    // ---------- העיצובים המובנים ----------

    static String builtInText(String name) {
        StringBuilder sb = new StringBuilder();
        boolean files = FILES.equals(name), words = WORDS.equals(name);
        sb.append("; ").append(name).append(" — ").append(files ? Words.DesignW.WHY_FILES : words ? Words.DesignW.WHY_WORDS
                : Words.DesignW.WHY_DEFAULT).append('\n');
        sb.append("; ").append(Words.DesignW.FILE_NOTE).append('\n');
        sb.append("; ").append(Words.DesignW.FILE_NOTE2).append('\n');
        sb.append('\n').append('[').append(S_TITLE).append("]\n");
        line(sb, K_SIZE, files ? "15" : "12");
        line(sb, K_WEIGHT, files ? W_BOLD : W_REG);
        line(sb, K_FONT, SYSTEM);
        sb.append('\n').append('[').append(S_MID).append("]\n");
        line(sb, K_STYLE, files ? GRID : ROWS);
        line(sb, K_COLS, files ? "3" : words ? "2" : "1");
        line(sb, K_ICON, files ? "55%" : "340%");
        line(sb, K_GAP, files ? "8%" : words ? "40%" : "50%");
        line(sb, K_SHOW_ICON, words ? NO : YES);
        line(sb, K_AROUND, files ? FRAME : FILL);
        line(sb, K_TINT, words ? YES : NO);
        line(sb, K_SHOW_DATA, YES);
        line(sb, K_DATA_AT, BELOW);
        line(sb, T_NAME + ": " + K_SIZE, files ? "14" : words ? "15" : "16");
        line(sb, T_NAME + ": " + K_WEIGHT, files || words ? W_BOLD : W_REG);
        line(sb, T_NAME + ": " + K_FONT, SYSTEM);
        line(sb, T_DATA + ": " + K_SIZE, "11");
        line(sb, T_DATA + ": " + K_WEIGHT, W_REG);
        line(sb, T_DATA + ": " + K_FONT, SYSTEM);
        line(sb, K_FIT, files || words ? YES : NO);
        line(sb, K_MIN, "11");
        line(sb, K_LINES, files ? "2" : words ? "2" : "2");
        sb.append('\n').append('[').append(S_LISTS).append("]\n");
        sb.append("; ").append(Words.DesignW.LISTS_NOTE).append('\n');
        return sb.toString();
    }

    private static void line(StringBuilder sb, String key, String value) {
        sb.append(key).append(" = ").append(value).append('\n');
    }
}
