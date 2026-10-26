package com.tikiyot.vfm;

// שורת פקודה כשדות (117א, שלב 2): הטופס ממלא את השדות מהשורה, והשורה נבנית מחדש מהשדות — כך הטקסט
// והטופס תמיד מתאימים. בלי ממשק, ולכן נבדק. הממשק — BatchEditScreen. מתגים שהטופס לא מכיר נשמרים כמו שהם.

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class BatchLine {

    static final String[] COMMANDS = {"COPY", "MOVE", "DEL", "REN", "MD", "RD", "COMP", "START", "ECHO", "REM"};

    /** הסבר קצר לכל פקודה — לשורה שתחת העורך ולכותרת הטופס (מהמילון). */
    static String help(String cmd) {
        return Words.Cmd.help(cmd);
    }

    /** 128: השדות והתחביר של תאריך וגודל — מ-DateSize (מקום אחד). */
    static final String[] DATE_MODES = DateSize.DATE_MODES, DATE_UNITS = DateSize.DATE_UNITS,
            SIZE_MODES = DateSize.SIZE_MODES, SIZE_UNITS = DateSize.SIZE_UNITS;
    static final String[] TAKEN = {Batch.TAKEN_ASK, Batch.TAKEN_SKIP, Batch.TAKEN_REPLACE + Words.Cmd.REPLACE_NOTE};

    String cmd = "COPY";
    /** תיקיית המקור (כמו שנכתבה, עם משתנים), והתבנית (*.pdf) — או ריק. */
    String srcDir = "", mask = "";
    String dest = "", newName = "", text = "";
    /** 0 בלי · 1 מאז · 2 לפני. הערך: יום/שבוע/… או מספר ימים או תאריך. */
    int dateMode;
    String dateValue = new DateSize().dateValue;
    /** 0 בלי · 1 גדול מ־ · 2 קטן מ־. */
    int sizeMode;
    String sizeNumber = "10", sizeUnit = "MB";
    /** 0 לשאול · 1 לדלג · 2 להחליף. */
    int taken;
    boolean recursive;
    /** מתגים שהטופס לא מכיר (ROBOCOPY, /Q …) — נשמרים כמו שהם. */
    final List<String> other = new ArrayList<String>();

    /** מפרק שורה. שורה ריקה או לא מוכרת — ברירת מחדל של הפקודה. */
    static BatchLine parse(String line) {
        BatchLine b = new BatchLine();
        String l = Bidi.strip(line).trim();                                  // 204
        if (l.startsWith("@")) l = l.substring(1).trim();
        List<String> tk = Batch.tokens(l);
        if (tk.isEmpty()) return b;
        String c = tk.get(0).toUpperCase(Locale.ROOT);
        if (c.equals("MKDIR")) c = "MD";
        if (c.equals("RMDIR")) c = "RD";
        if (c.equals("ERASE")) c = "DEL";
        if (c.equals("RENAME")) c = "REN";
        b.cmd = c;
        if (c.equals("ECHO") || c.equals("REM")) {
            b.text = l.length() > c.length() ? l.substring(c.length()).trim() : "";
            return b;
        }
        List<String> args = new ArrayList<String>();
        for (int i = 1; i < tk.size(); i++) {
            String t = tk.get(i);
            if (!Batch.isSwitch(t)) {
                args.add(t);
                continue;
            }
            String u = t.toUpperCase(Locale.ROOT);
            DateSize ds = b.filter();
            if ((t.startsWith("/" + Batch.SW_DATE) || t.startsWith("/" + Batch.SW_SIZE)) && ds.take(t.substring(1))) b.filter(ds);
            else if (t.equals(Batch.SW_TAKEN + Batch.TAKEN_SKIP)) b.taken = 1;
            else if (t.equals(Batch.SW_TAKEN + Batch.TAKEN_REPLACE) || u.equals("/Y")) b.taken = 2;
            else if (t.equals(Batch.SW_TAKEN + Batch.TAKEN_ASK) || u.equals("/-Y")) b.taken = 0;
            else if (u.equals("/S") && c.equals("RD")) b.recursive = true;    // /S — רק ב-RD; בשאר נשמר כמו שהוא (122)
            else b.other.add(t);                                                // כולל /גודל שלא הובן — לא מנחשים
        }
        String a0 = args.size() > 0 ? args.get(0) : "", a1 = args.size() > 1 ? args.get(1) : "";
        if (c.equals("COPY") || c.equals("MOVE") || c.equals("DEL")) {
            String n = a0.replace('/', '\\');
            int cut = n.lastIndexOf('\\');
            String last = cut >= 0 ? n.substring(cut + 1) : n;
            if (last.contains("*") || last.contains("?")) {
                b.srcDir = cut >= 0 ? a0.substring(0, cut) : "";
                b.mask = last;
            } else {
                b.srcDir = a0;
            }
            b.dest = a1;
        } else if (c.equals("REN") || c.equals("COMP")) {                 // 1.20.0: COMP <מקור> <יעד>
            b.srcDir = a0;
            b.newName = a1;
        } else {
            b.dest = a0;                                   // MD, RD, START
        }
        return b;
    }

    /** 128: התאריך והגודל של השורה כ-DateSize (עותק). */
    DateSize filter() {
        DateSize d = new DateSize();
        d.dateMode = dateMode;
        d.dateValue = dateValue;
        d.sizeMode = sizeMode;
        d.sizeNumber = sizeNumber;
        d.sizeUnit = sizeUnit;
        return d;
    }

    void filter(DateSize d) {
        dateMode = d.dateMode;
        dateValue = d.dateValue;
        sizeMode = d.sizeMode;
        sizeNumber = d.sizeNumber;
        sizeUnit = d.sizeUnit;
    }

    private static String q(String s) {
        return s.isEmpty() || s.contains(" ") || s.contains("\t") ? "\"" + s + "\"" : s;
    }

    /** המקור כנתיב אחד: תיקייה + תבנית. */
    String source() {
        if (mask.isEmpty()) return srcDir;
        if (srcDir.isEmpty()) return mask;
        return srcDir + (srcDir.endsWith("\\") || srcDir.endsWith("/") ? "" : "\\") + mask;
    }

    /** בונה את השורה מהשדות. */
    String toLine() {
        StringBuilder sb = new StringBuilder(cmd);
        if (cmd.equals("ECHO") || cmd.equals("REM")) return text.isEmpty() ? cmd : cmd + " " + text;
        if (cmd.equals("COPY") || cmd.equals("MOVE")) sb.append(' ').append(q(source())).append(' ').append(q(dest));
        else if (cmd.equals("DEL")) sb.append(' ').append(q(source()));
        else if (cmd.equals("COMP") && newName.isEmpty()) sb.append(' ').append(q(srcDir));   // 1.22.0: COMP /רשימה: <יעד>
        else if (cmd.equals("REN") || cmd.equals("COMP")) sb.append(' ').append(q(srcDir)).append(' ').append(q(newName));
        else sb.append(' ').append(q(dest));
        boolean filters = cmd.equals("COPY") || cmd.equals("MOVE") || cmd.equals("DEL");
        DateSize ds = filter();
        if (filters && !ds.dateToken().isEmpty()) sb.append(" /").append(ds.dateToken());
        if (filters && !ds.sizeToken().isEmpty()) sb.append(" /").append(ds.sizeToken());
        if ((cmd.equals("COPY") || cmd.equals("MOVE")) && taken > 0) sb.append(taken == 1 ? " " + Batch.SW_TAKEN + Batch.TAKEN_SKIP : " /Y");
        if (cmd.equals("RD") && recursive) sb.append(" /S");
        for (String o : other) {                                            // 1.22.0: ערך עם רווח — במרכאות
            int c = o.indexOf(':');
            if (o.contains(" ") && c > 0) sb.append(' ').append(o, 0, c + 1).append('"').append(o.substring(c + 1)).append('"');
            else sb.append(' ').append(o);
        }
        return sb.toString();
    }

    /** מסלול מלא שנבחר בבורר — בסגנון bat: המשתנה הארוך ביותר שמתאים בהתחלה, ו-"\\". */
    static String toVar(String abs, Map<String, String> vars) {
        String best = null, bestVal = "";
        for (Map.Entry<String, String> e : vars.entrySet()) {
            if (e.getKey().equals(Batch.V_HERE)) continue;                // תלוי בקובץ — לא מציעים
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
}
