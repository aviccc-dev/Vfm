package com.tikiyot.vfm;

// מבנה 160 (סידור תיקיית הפקודות): קריאת שורה (Parse — לא Line: יש כבר State.Line ו־Ini.Line) — פירוק למילים, המשתנים (%פנימי%, %כאן%…), המתגים, המסלולים, תבניות
// השם (* ?), ו"המשך (N)". עד כאן ישב בתוך Batch. המילים עצמן — Syntax. אחראי: להבין שורה. אסור לו: לתכנן ולהריץ.

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class Parse {

    private Parse() { }

    /**
     * מ0.31 (המשתמש, 11.11.2026: "המשך סתם רק פעם אחת, אם יש מספר בתוך סוגריים אז כפי המספר, אם סוגריים ריקות אז ממשיך
     * בלי עצירה בכלל"): כמה כישלונות מותרים לפי שורת "המשך". ‎-1 — הצורה לא מובנת.
     */
    static int allowOf(String line) {
        String rest = line.trim();
        int sp = 0;
        while (sp < rest.length() && !Character.isWhitespace(rest.charAt(sp)) && rest.charAt(sp) != '(') sp++;
        rest = rest.substring(sp).trim();
        if (rest.isEmpty()) return 1;
        if (rest.startsWith("(") && rest.endsWith(")")) rest = rest.substring(1, rest.length() - 1).trim();
        if (rest.isEmpty()) return Integer.MAX_VALUE;
        try {
            int n = Integer.parseInt(rest);
            return n < 0 ? -1 : n;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** מפצל שורה לחלקים כמו cmd: רווחים מפרידים, ומרכאות שומרות רווחים (המרכאות עצמן יורדות). */
    static List<String> tokens(String line) {
        List<String> out = new ArrayList<String>();
        StringBuilder cur = new StringBuilder();
        boolean q = false, any = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                q = !q;
                any = true;
            } else if (!q && Character.isWhitespace(c)) {
                if (any || cur.length() > 0) out.add(cur.toString());
                cur.setLength(0);
                any = false;
            } else {
                cur.append(c);
            }
        }
        if (any || cur.length() > 0) out.add(cur.toString());
        return out;
    }

    /** START %בית% — היעד שבסוף הוא מסך הבית. */
    static boolean isHome(File f) {
        return f != null && Syntax.HOME_MARK.equals(f.getPath());
    }

    /** "/תפוס:<ערך>" — ערך מוכר. */
    static boolean takenValue(String sw) {
        for (String v : new String[]{Syntax.TAKEN_ASK, Syntax.TAKEN_SKIP, Syntax.TAKEN_REPLACE, Syntax.TAKEN_STOP, Syntax.TAKEN_RENAME}) if (sw.equals(Syntax.SW_TAKEN + v)) return true;
        return false;
    }

    static String takenUnknown(String sw) {
        return Words.Cmd.switchUnknown(sw) + " (" + Syntax.TAKEN_ASK + " · " + Syntax.TAKEN_SKIP + " · " + Syntax.TAKEN_REPLACE + " · " + Syntax.TAKEN_STOP
                + " · " + Syntax.TAKEN_RENAME + ")";
    }

    static String var(String name) {
        return "%" + name + "%";
    }

    /** מתג: מתחיל ב-"/" ושמו מוכר (כך "/storage/..." אינו מתג). */
    private static final Pattern SWITCH = Pattern.compile(
            "^/(-?Y|S|Q|F|A(:.*)?|D(:.*)?|" + Syntax.SW_DATE + ".*|" + Syntax.SW_SIZE + ".*|" + Syntax.SW_TAKEN.substring(1)
                    + ".*|" + Syntax.SW_KEEP.substring(1) + "|" + Syntax.SW_JOURNAL.substring(1) + ".*|" + Syntax.SW_LIMIT.substring(1)
                    + ".*|" + Syntax.SW_STATE.substring(1) + ".*|" + Syntax.SW_LIST.substring(1) + ".*|" + Syntax.SW_APPEND.substring(1) + "|" + Syntax.SW_CONTAINS + ".*|MAXAGE:.*|MINAGE:.*|MAX:.*|MIN:.*)$",
            Pattern.CASE_INSENSITIVE);

    static boolean isSwitch(String t) {
        return SWITCH.matcher(t).matches();
    }

    /** מסלול מלא שנבחר בבורר — בסגנון bat: המשתנה הארוך ביותר שמתאים בהתחלה, ו-"\\". */
    static String toVar(String abs, Map<String, String> vars) {
        String best = null, bestVal = "";
        for (Map.Entry<String, String> e : vars.entrySet()) {
            if (e.getKey().equals(Syntax.V_HERE)) continue;                      // תלוי בקובץ — לא מציעים
            String v = e.getValue();
            if ((abs.equals(v) || abs.startsWith(v + "/")) && v.length() > bestVal.length()) {
                best = e.getKey();
                bestVal = v;
            }
        }
        String rest = best == null ? abs : abs.substring(bestVal.length());
        String out = (best == null ? "" : "%" + best + "%") + rest.replace('/', '\\');
        return out.isEmpty() ? abs : out;
    }

    /** משתנים קבועים. */
    /**
     * מ0.41: קובץ שכולו פקודת פתיחה אחת (חוץ מהכותרת — צלמית, שם, למה — ומהערות): השורה; אחרת null. קובץ כזה רץ מיד, בלי
     * הכנה ותצוגה מקדימה — כמו כפתור (הכפתורים בשורות הם קובצי פקודות).
     */
    static String singleVerb(String text) {
        String one = null;
        for (String raw : Bidi.strip(text == null ? "" : text).replace("\r", "").split("\n")) {
            String l = Commands.normal(raw);
            if (l.isEmpty() || l.startsWith("::") || Program.isHeader(l)) continue;
            int sp = l.indexOf(' ');
            Commands.Def d = Commands.get(sp < 0 ? l : l.substring(0, sp));
            if (d != null && d.name.equals("REM")) continue;
            if (one != null || !Verbs.isVerb(l)) return null;
            one = l;
        }
        return one;
    }

    static Map<String, String> vars(File here, String internal, String card) {
        Map<String, String> v = new LinkedHashMap<String, String>();
        v.put(Syntax.V_INTERNAL, internal);
        if (card != null) v.put(Syntax.V_CARD, card);
        v.put(Syntax.V_DOWNLOADS, internal + "/Download");
        v.put(Syntax.V_HERE, here.getAbsolutePath());
        if (card != null) v.put(Syntax.V_CARD_ID, new File(card).getName());       // 1.19.0
        v.put(Syntax.V_NOW, new java.text.SimpleDateFormat(Syntax.NOW_FORMAT, Locale.ROOT).format(new java.util.Date()));   // 1.19.0
        v.put(Syntax.V_TIME, new java.text.SimpleDateFormat("HH-mm-ss", Locale.ROOT).format(new java.util.Date()));   // מ0.37
        v.put(Syntax.V_UNIQUE, Long.toString(System.currentTimeMillis(), 36));        // מ0.37: שם שלא חוזר בין הרצות
        v.put(Syntax.V_ONLY_A, Syntax.ONLY_A_MARK);                                         // 1.20.0
        v.put(Syntax.V_ONLY_B, Syntax.ONLY_B_MARK);
        v.put(Syntax.V_SHARE, Syntax.SHARE_MARK);                                           // 203
        v.put(Syntax.V_HOME, Syntax.HOME_MARK);                                             // 1.9.0
        return v;
    }

    /** מחליף משתנים, "\\" ב-"/", ומסלול יחסי — ביחס לתיקייה של הקובץ. null = משתנה לא מוכר. */
    static String path(String raw, File here, Map<String, String> vars, String[] error) {
        // מבנה 41 (ממצא 35): מסלול ריק — שגיאה. עד כאן "" פוענח כ"התיקייה של הקובץ", ו־DEL "" מחק את כל הקבצים שבה
        // (משתנה ריק או מקום פתוח שלא מולא הופכים בדיוק לזה). מי שמתכוון לתיקייה הזו — כותב "." .
        if (raw == null || raw.trim().isEmpty()) {
            error[0] = Words.Cmd.EMPTY_PATH;
            return null;
        }
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < raw.length()) {
            char c = raw.charAt(i);
            if (raw.startsWith("%~dp0", i)) {                          // 139: התיקייה של הקובץ, כמו ב-bat
                String h = vars.get(Syntax.V_HERE);
                sb.append(h).append('/');
                i += 5;
                if (i < raw.length() && (raw.charAt(i) == '\\' || raw.charAt(i) == '/')) i++;
                continue;
            }
            if (c == '%') {
                int j = raw.indexOf('%', i + 1);
                if (j < 0) {
                    error[0] = Words.Cmd.MISSING_PERCENT;
                    return null;
                }
                String name = raw.substring(i + 1, j);
                String val = vars.get(name);
                if (val == null) {
                    error[0] = Words.Cmd.unknownVar(name);
                    return null;
                }
                sb.append(val);
                i = j + 1;
            } else {
                sb.append(c == '\\' ? '/' : c);
                i++;
            }
        }
        String p = sb.toString();
        if (p.trim().isEmpty()) {                                               // משתנה שהערך שלו ריק
            error[0] = Words.Cmd.EMPTY_PATH;
            return null;
        }
        while (p.length() > 1 && p.endsWith("/")) p = p.substring(0, p.length() - 1);
        if (!p.startsWith("/")) p = new File(here, p).getAbsolutePath();
        return Paths.clean(p);                                                  // מבנה 42: צורה אחת — בלי "." ו".."
    }


    static String clean(String raw) {
        String line = raw.trim();
        if (line.startsWith("@")) line = line.substring(1).trim();
        return line;
    }
}
