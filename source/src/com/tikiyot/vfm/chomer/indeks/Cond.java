package com.tikiyot.vfm;

// תחביר התנאים. אחראי: לפרק תנאי למילות חיפוש ולמסננים.
// אסור לו: לגעת בקבצים או במסכים.

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class Cond {

    public interface Test {
        boolean ok(Index ix, Index.Rec r);
    }

    public final List<String> words = new ArrayList<String>();
    public final List<Test> tests = new ArrayList<Test>();
    public final List<String> unknown = new ArrayList<String>();
    /** 181: "לא" — מינוס לפני רכיב ("-סוג:ini", "-מטמון"): מה שעונה עליו יוצא. */
    final List<Test> notTests = new ArrayList<Test>(), notKind = new ArrayList<Test>();
    final List<String> notWords = new ArrayList<String>();
    public static final String NOT = "-";                                                    // תחביר

    /** 181: יש בתנאי "לא". */
    public boolean hasNot() {
        return !notTests.isEmpty() || !notWords.isEmpty();
    }
    /** התנאי מבקש במפורש את תיקיית המחיקות (תיקייה:נמחקו). אחרת היא מוסתרת. */
    public boolean trash;
    /** 1.22.4: התנאי מבקש במפורש מקום או מצב (מצב: / תיקייה: / מקור:) — אז גם קבצי גיבוי. אחרת הם מוסתרים. */
    public boolean backup;

    private Cond() { }

    /**
     * מילות התנאי — מקום אחד. השאילתה מגיעה מנורמלת (Text.norm: אותיות סופיות, אותיות קטנות),
     * ולכן משווים אחרי נרמול: "תאריך" מגיע כ"תאריכ", "כונן" כ"כוננ" (באג: תאריך וכונן לא נמצאו כלל).
     */
    public static final String TYPE = "סוג", FOLDER = "תיקייה", DRIVE = "כונן", SOURCE = "מקור",  // תחביר
            SIZE = "גודל", DATE = "תאריך",  // תחביר
            ADDED = "נוסף",  // 189: מתי האינדקס ראה את הקובץ לראשונה // תחביר
            STATE = "מצב", STATE2 = "מצב-בתאריך";  // 1.21.0: מצב הגיבוי בזמן נבחר // תחביר
    /** 175: רק תיקיות ריקות — תיקיות במקום קבצים. */
    public static final String EMPTY = "תיקיות:ריקות";  // תחביר

    /** 175: האם הרכיב הוא "תיקיות:ריקות". */
    public static boolean isEmptyDirs(String token) {
        int c = token.indexOf(':');
        return c > 0 && is(token.substring(0, c), "תיקיות") && is(token.substring(c + 1), "ריקות");  // תחביר
    }

    /** 175: מבקשים תיקיות ריקות (ולא קבצים). */
    public boolean emptyDirs;
    /** 175: תנאי הסוג — לא חלים על תיקייה. */
    private final List<Test> kindTests = new ArrayList<Test>();

    private static boolean is(String key, String word) {
        return Text.norm(key).equals(Text.norm(word));
    }

    /**
     * פירוק לרכיבים. ערך עם רווחים נכתב בתוך «», למשל תיקייה:«/storage/…/ספרים חדשים».
     * הסימנים « » מוסרים מהרכיב.
     */
    public static List<String> tokens(String raw) {
        List<String> out = new ArrayList<String>();
        if (raw == null) return out;
        StringBuilder cur = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            if (ch == '«') {
                quoted = true;
            } else if (ch == '»') {
                quoted = false;
            } else if (!quoted && Character.isWhitespace(ch)) {
                if (cur.length() > 0) {
                    out.add(cur.toString());
                    cur.setLength(0);
                }
            } else {
                cur.append(ch);
            }
        }
        if (cur.length() > 0) out.add(cur.toString());
        return out;
    }

    /** האם הרכיב הוא "מילה:ערך" עם המילה הזו (אחרי נרמול — כמו בחיפוש, 144). */
    public static boolean keyIs(String token, String key) {
        int c = token.indexOf(':');
        return c > 0 && is(token.substring(0, c), key);
    }

    /** רכיב שפורק (בלי «») — בחזרה לכתיבה: "מילה:«ערך עם רווח»". */
    public static String quoteToken(String token) {
        if (token.indexOf(' ') < 0) return token;
        int c = token.indexOf(':');
        return c > 0 ? token.substring(0, c + 1) + quote(token.substring(c + 1)) : quote(token);
    }

    /** ערך לכתיבה בתנאי: בתוך «» אם יש בו רווח. */
    public static String quote(String value) {
        return value.indexOf(' ') >= 0 ? "«" + value + "»" : value;
    }

    public static Cond parse(String raw) {
        Cond c = new Cond();
        if (raw == null) return c;
        for (String token : tokens(raw)) {
            if (token.isEmpty()) continue;
            if (token.length() > 1 && token.startsWith(NOT) && !token.startsWith(NOT + NOT)) {   // 181
                Cond n = parse(quoteToken(token.substring(1)));
                c.notTests.addAll(n.tests);
                c.notKind.addAll(n.kindTests);
                for (String w : n.words) c.notWords.add(Text.norm(w));
                for (String u : n.unknown) c.unknown.add(NOT + u);
                continue;
            }
            int colon = token.indexOf(':');
            if (colon <= 0) {
                c.words.add(token);
                continue;
            }
            if (isEmptyDirs(token)) {                                       // 175
                c.emptyDirs = true;
                continue;
            }
            String key = token.substring(0, colon), value = token.substring(colon + 1);
            Test t = null;
            if (is(key, TYPE)) {
                t = ext(value);
                if (t != null) c.kindTests.add(t);
            }
            else if (is(key, FOLDER) || is(key, "תיקיה")) {  // תחביר
                t = folder(value);
                c.backup = true;
                if (value.contains("נמחקו")) c.trash = true;  // תחביר
            }
            else if (is(key, DRIVE)) t = volume(value);
            else if (is(key, SOURCE)) {
                t = source(value);
                c.backup = true;
                if (value.contains(Index.TRASH)) c.trash = true;
            }
            else if (is(key, SIZE)) t = size(value);
            else if (is(key, DATE)) t = date(value);
            else if (is(key, ADDED)) t = date(value, true);
            else if (is(key, STATE) || is(key, STATE2)) {
                t = state(value);
                c.backup = true;
            }
            else {
                c.words.add(token);
                continue;
            }
            if (t != null) c.tests.add(t); else c.unknown.add(token);
        }
        return c;
    }

    public boolean matches(Index ix, Index.Rec r) {
        for (Test t : tests) if (!t.ok(ix, r)) return false;
        for (Test t : notTests) if (t.ok(ix, r)) return false;                 // 181
        for (String w : notWords) if (r.norm != null && r.norm.contains(w)) return false;
        return true;
    }

    /** 175: תיקייה — בעיני המקורות, הכונן, התאריך (תנאי הסוג אינם חלים עליה; גודל — 0). */
    public boolean matchesDir(Index ix, int dirId) {
        Index.Rec r = new Index.Rec();
        r.dir = dirId;
        r.name = "";
        r.norm = r.loose = "";
        r.mtime = ix.dirs.get(dirId).mtime;
        for (Test t : tests) if (!kindTests.contains(t) && !t.ok(ix, r)) return false;
        for (Test t : notTests) if (!notKind.contains(t) && t.ok(ix, r)) return false;     // 181: "בלי סוג" — לא על תיקייה
        String dn = ix.dirs.get(dirId).norm;
        for (String w : notWords) if (dn != null && dn.contains(w)) return false;
        return true;
    }

    /**
     * סוג: סיומות, שמות של קבוצות (145 — "ספרים", "קובצי נתונים"; מההגדרות, Types), ו"שאר הקבצים"
     * (מה שאינו באף קבוצה, גם בלי סיומת). הקבוצות נקראות כאן, פעם אחת לתנאי — לא לכל קובץ.
     */
    private static Test ext(String value) {
        final Set<String> known = new HashSet<String>();
        final List<String> exts = new ArrayList<String>();
        boolean other = false;
        List<Types.Group> groups = Types.groups();
        for (String p : Text.norm(value).split("\\|")) {
            if (p.trim().isEmpty()) continue;
            Types.Group g = Types.find(groups, p);                        // שם של קבוצה קודם; אחרת — סיומת
            if (g != null) exts.addAll(g.exts);
            else if (Types.isOther(p)) other = true;
            else exts.add(p.trim());
        }
        if (other) for (Types.Group g : groups) known.addAll(g.exts);
        final String[] list = exts.toArray(new String[0]);
        final boolean others = other;
        return new Test() {
            @Override
            public boolean ok(Index ix, Index.Rec r) {
                String x = Text.ext(r.name);                                  // 104: גם "ספר.pdf (1)"
                if (others && !known.contains(x)) return true;
                for (String e : list)
                    if (!e.isEmpty() && (x.equals(e)
                            || (e.indexOf('.') > 0 && r.norm.endsWith("." + e))))  // 123: סוג מורכב — tar.gz
                        return true;
                return false;
            }
        };
    }

    private static Test folder(String value) {
        final String[] wants = Text.norm(value).split("\\|");
        final java.util.IdentityHashMap<Index, java.util.HashMap<Integer, Boolean>> cache =
                new java.util.IdentityHashMap<Index, java.util.HashMap<Integer, Boolean>>();
        return new Test() {
            @Override
            public boolean ok(Index ix, Index.Rec r) {
                java.util.HashMap<Integer, Boolean> m = cache.get(ix);
                if (m == null) {
                    m = new java.util.HashMap<Integer, Boolean>();
                    cache.put(ix, m);
                }
                Boolean b = m.get(r.dir);
                if (b == null) {                     // פעם אחת לכל תיקייה
                    String dp = ix.normPath(r.dir) + "/";
                    b = false;
                    for (String w : wants) if (!w.isEmpty() && dp.contains(w)) b = true;
                    m.put(r.dir, b);
                }
                return b;
            }
        };
    }

    /**
     * מקורות: "פנימי", "כרטיס" (כל כרטיס), או מסלול מלא של תיקייה. מופרדים ב"|", ו"או" ביניהם.
     * תיקייה כוללת את כל מה שבתוכה.
     */
    private static Test source(String value) {
        final List<String> drives = new ArrayList<String>(), folders = new ArrayList<String>();
        // ההשוואה מנורמלת בשני הצדדים: החיפוש מנרמל את כל השאילתה (אותיות קטנות),
        // ובלי זה "Download" לא היה נמצא (נמצא בבדיקות, 1.8).
        for (String v : Text.norm(value).split("\\|")) {
            v = v.trim();
            if (v.isEmpty()) continue;
            if (v.startsWith("/")) folders.add(v.endsWith("/") ? v.substring(0, v.length() - 1) : v);
            else drives.add(v);
        }
        final java.util.IdentityHashMap<Index, java.util.HashMap<Integer, Boolean>> cache =
                new java.util.IdentityHashMap<Index, java.util.HashMap<Integer, Boolean>>();
        final java.util.IdentityHashMap<Index, Boolean> byDrive = new java.util.IdentityHashMap<Index, Boolean>();
        return new Test() {
            /** 184: הכונן — פעם אחת לכל כונן, ולא נרמול שם הכונן לכל קובץ. */
            private boolean driveOk(Index ix) {
                Boolean b = byDrive.get(ix);
                if (b == null) {
                    b = false;
                    for (String d : drives) {
                        if (d.equals(Drive.INTERNAL) && ix.key.equals(Drive.INTERNAL)) b = true;
                        if (d.equals(Drive.CARD) && !ix.key.equals(Drive.INTERNAL)) b = true;
                        if (d.equals(Text.norm(ix.key))) b = true;
                    }
                    byDrive.put(ix, b);
                }
                return b;
            }

            @Override
            public boolean ok(Index ix, Index.Rec r) {
                if (!drives.isEmpty() && driveOk(ix)) return true;
                if (folders.isEmpty()) return false;
                java.util.HashMap<Integer, Boolean> m = cache.get(ix);
                if (m == null) cache.put(ix, m = new java.util.HashMap<Integer, Boolean>());
                Boolean b = m.get(r.dir);
                if (b == null) {
                    String dp = ix.normPath(r.dir);
                    b = false;
                    for (String f : folders) if (dp.equals(f) || dp.startsWith(f + "/")) b = true;
                    m.put(r.dir, b);
                }
                return b;
            }
        };
    }

    private static Test volume(String value) {
        final String want = Text.norm(value);
        final java.util.IdentityHashMap<Index, Boolean> cache = new java.util.IdentityHashMap<Index, Boolean>();
        return new Test() {
            @Override
            public boolean ok(Index ix, Index.Rec r) {
                Boolean b = cache.get(ix);                         // 184: פעם אחת לכל כונן
                if (b == null) cache.put(ix, b = Text.norm(ix.label).contains(want) || Text.norm(ix.key).contains(want));
                return b;
            }
        };
    }

    static Test size(String value) {
        if (value.length() < 2) return null;
        char op = value.charAt(0);
        if (op != '>' && op != '<') return null;
        String rest = value.substring(1).trim().toUpperCase(Locale.US);
        long mult = 1;
        if (rest.endsWith("KB")) { mult = 1024L; rest = cut(rest, 2); }
        else if (rest.endsWith("MB")) { mult = 1048576L; rest = cut(rest, 2); }
        else if (rest.endsWith("GB")) { mult = 1073741824L; rest = cut(rest, 2); }
        else if (rest.endsWith("B")) rest = cut(rest, 1);
        final long bytes;
        try {
            bytes = (long) (Double.parseDouble(rest.trim()) * mult);
        } catch (NumberFormatException e) {
            return null;
        }
        final boolean greater = op == '>';
        return new Test() {
            @Override
            public boolean ok(Index ix, Index.Rec r) {
                return greater ? r.size > bytes : r.size < bytes;
            }
        };
    }

    private static String cut(String s, int n) {
        return s.substring(0, s.length() - n);
    }

    /**
     * תאריך (השינוי האחרון של הקובץ). בלי סימן או עם ">" / ">=" — מאז; עם "<" — לפני (ישן מ־).
     * יחידות: יום/היום · שבוע · חודש · שנה · N ימים ("3ימים", "3 ימים", "3d") · תאריך מדויק (2026-09-01 / 1.9.2026) · שנה (2026).
     * (117א: "לפני" ותאריך־מדויק־לפני נוספו לקובצי הפקודות, ומשמשים גם בחיפוש — 102.)
     */
    static Test date(String value) {
        return date(value, false);
    }

    /**
     * 1.21.0: מצב:«26.10.2026 14:30» — רק קבצים בגיבוי "מראה + היסטוריה" שהיו בו באותו זמן: הגרסה שהגיעה עד אז
     * ועוד לא יצאה (State). כל המסלולים — לפי השורש של הגיבוי, כך שהתוצאה היא העץ של אותו זמן.
     */
    static Test state(String value) {
        long t = State.when(value);
        if (t == Long.MIN_VALUE) return null;
        final State st = new State(t);
        return new Test() {
            @Override
            public boolean ok(Index ix, Index.Rec r) {
                if (r.name == null || r.name.isEmpty()) return false;            // תיקייה — לא
                return st.in(ix.path(r), r.added);
            }
        };
    }

    /** 189: added — לפי "נוסף" (לא ידוע = 0: "ישן" — לא עונה ל"מאז", עונה ל"לפני"). */
    static Test date(String value, final boolean added) {
        String v = value.trim();
        boolean before = false;
        if (v.startsWith(">=")) v = v.substring(2).trim();
        else if (v.startsWith("<=")) { before = true; v = v.substring(2).trim(); }
        else if (v.startsWith(">")) v = v.substring(1).trim();
        else if (v.startsWith("<")) { before = true; v = v.substring(1).trim(); }
        final long edge = dateEdge(v);
        if (edge == Long.MIN_VALUE) return null;
        final boolean older = before;
        return new Test() {
            @Override
            public boolean ok(Index ix, Index.Rec r) {
                long t = added ? r.added : r.mtime;
                return older ? t < edge : t >= edge;
            }
        };
    }

    /** נקודת הזמן של ערך תאריך, או Long.MIN_VALUE אם לא הובן. */
    static long dateEdge(String raw) {
        String v = Text.norm(raw).trim();                     // גם כשהגיע מנורמל: "יום" ← "יומ", "ימים" ← "ימימ"
        if (is(v, "יום") || is(v, "היום")) return ago(Calendar.DAY_OF_YEAR, 1);  // תחביר
        if (is(v, "שבוע")) return ago(Calendar.DAY_OF_YEAR, 7);  // תחביר
        if (is(v, "חודש")) return ago(Calendar.MONTH, 1);  // תחביר
        if (is(v, "שנה")) return ago(Calendar.YEAR, 1);  // תחביר
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                "^(\\d+)\\s*(" + Text.norm("ימים") + "|" + Text.norm("יום") + "|d)$").matcher(v);  // תחביר
        if (m.find()) return ago(Calendar.DAY_OF_YEAR, Integer.parseInt(m.group(1)));
        try {
            String[] p = v.split("[-/.]");
            Calendar cal = Calendar.getInstance();
            cal.clear();
            if (p.length == 3) {
                int a = Integer.parseInt(p[0].trim());
                if (a > 1000) cal.set(a, Integer.parseInt(p[1].trim()) - 1, Integer.parseInt(p[2].trim()));
                else cal.set(year(Integer.parseInt(p[2].trim())), Integer.parseInt(p[1].trim()) - 1, a);
            } else if (p.length == 2) {                                   // יום וחודש — השנה הנוכחית ("1.9")
                cal.set(Calendar.getInstance().get(Calendar.YEAR), Integer.parseInt(p[1].trim()) - 1, Integer.parseInt(p[0].trim()));
            } else if (p.length == 1 && p[0].trim().length() == 4) {
                cal.set(Integer.parseInt(p[0].trim()), 0, 1);
            } else {
                return Long.MIN_VALUE;
            }
            return cal.getTimeInMillis();
        } catch (Throwable t) {
            return Long.MIN_VALUE;
        }
    }

    /** שנה בשתי ספרות ("1.9.26") — 2026. */
    private static int year(int y) {
        return y < 100 ? 2000 + y : y;
    }

    private static long ago(int field, int amount) {
        Calendar c = Calendar.getInstance();
        c.add(field, -amount);
        return c.getTimeInMillis();
    }
}
