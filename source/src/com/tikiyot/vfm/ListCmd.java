package com.tikiyot.vfm;

// 1.24.0: פקודות על רשימות (claude/pkudot-reshimot.md). המשתמש: "אם פרמטר — מספיק העתק רגיל; אם בסתם — זה קובץ; אם
// העתק רשימה — אז זו רשימה"; "העתק רשימה א לתיקייה ב — כך שיעתיק מהרשימה את הקבצים לתיקייה"; "לפצל מההתחלה
// לרשימות ידניות וחכמות"; "קבצים לא יכולים להיכנס לרשימה — רק השמות והמסלולים שלהם".
// מילת סוג לפני כל צד: ידנית · חכמה · תוכן ידנית · קבצי ידנית · קבצי חכמה · תיקייה (בלי מילה — קבצים ותיקיות).
// "*" — הבית של האופן. הפקודות פונות לרשימה בשמה ובמקומה במבנה — לא לאחסון שלה (כך גם במסד נתונים בעתיד).
// שלב 1: MD, COPY, RD, DEL. ההדבקה וההסרה — אותו מנוע של הלוח (VClip); ההדמיה — על עותק של העץ בזיכרון.
// אחראי: הקריאה, התכנון וההרצה של שורה עם מילות סוג (בלי ממשק — נבדק).

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class ListCmd {

    static final String MANUAL = "ידנית", SMART = "חכמה", CONTENT = "תוכן", FILES_OF = "קבצי", FOLDER = "תיקייה";   // תחביר
    /** מ0.18: יעד שהוא קובץ טקסט — יצוא מהמסד. */
    static final String FILE_WORD = "קובץ";   // תחביר
    static final String HOME = "*";
    /** 1.25.0: COMP של שתי רשימות חכמות — גם התנאים (המשתמש: "אולי פרמטר שיוסיף את השוואת התנאים"). */
    static final String SW_CONDS = "/תנאים";   // תחביר

    static final int FS = 0, LIST = 1, SMART_LIST = 2, EXPORT = 3;

    /** צד אחד בשורה: מה הוא, והערך שלו. */
    static final class Side {
        int kind = FS;
        boolean content, filesOf;
        String raw;
        /** ברשימה: הדרך (ריקה — הבית); mask — תבנית קבצים בסוף הדרך ("א\*.pdf"), או null. */
        List<String> path;
        boolean home;
        String mask;

        boolean list() {
            return kind != FS || content || filesOf;
        }

        String label() {
            return home ? Words.ListCmdW.HOME : "\"" + raw + "\"";
        }
    }

    /** חלק בשורה — עם סימן אם היה במרכאות (מילה במרכאות היא שם, לא מילת סוג). */
    static final class Tok {
        final String text;
        final boolean quoted;
        /** מ0.40: המרכאות — רק בערך של מתג (/מכיל:"שתי מילים"): החלק מתחיל ב־"/" ולא במרכאות, ולכן הוא מתג ולא שם. */
        final boolean valueQuoted;

        Tok(String text, boolean quoted) {
            this(text, quoted, false);
        }

        Tok(String text, boolean quoted, boolean valueQuoted) {
            this.text = text;
            this.quoted = quoted;
            this.valueQuoted = valueQuoted;
        }
    }

    /** הצדדים של שורה, או null — אין בה מילת סוג (שורה רגילה). */
    static final class Parsed {
        final List<Side> sides = new ArrayList<Side>();
        String error;

        boolean anyList() {
            for (Side s : sides) if (s.list()) return true;
            return false;
        }
    }

    /** הפעולה שתתבצע בהרצה (רשימות בלבד; קבצים בכונן — Batch כרגיל). */
    interface Op {
        String run(Library lib);
    }

    private ListCmd() { }

    // ---------- קריאה ----------

    static List<Tok> tokens(String line) {
        List<Tok> out = new ArrayList<Tok>();
        StringBuilder cur = new StringBuilder();
        boolean q = false, any = false, wasQ = false, lead = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (!any && cur.length() == 0) lead = true;                    // החלק מתחיל במרכאות — שם
                q = !q;
                any = true;
                wasQ = true;
            } else if (!q && Character.isWhitespace(c)) {
                if (any || cur.length() > 0) out.add(new Tok(cur.toString(), wasQ, wasQ && !lead));
                cur.setLength(0);
                any = false;
                wasQ = false;
                lead = false;
            } else cur.append(c);
        }
        if (any || cur.length() > 0) out.add(new Tok(cur.toString(), wasQ, wasQ && !lead));
        return out;
    }

    private static boolean word(Tok t) {
        return !t.quoted && (t.text.equals(MANUAL) || t.text.equals(SMART) || t.text.equals(CONTENT)
                || t.text.equals(FILES_OF) || t.text.equals(FOLDER) || t.text.equals(FILE_WORD));
    }

    /** null — אין בשורה מילת סוג. switches — נאספים בנפרד (כמו ב-Batch). */
    static Parsed parse(String line, List<String> switches) {
        List<Tok> tk = tokens(line);
        boolean typed = false;
        for (int i = 1; i < tk.size(); i++) typed |= word(tk.get(i));
        if (!typed) return null;
        Parsed p = new Parsed();
        Side cur = null;
        for (int i = 1; i < tk.size(); i++) {
            Tok t = tk.get(i);
            if ((!t.quoted && (Batch.isSwitch(t.text) || t.text.equals(SW_CONDS))) || (t.valueQuoted && Batch.isSwitch(t.text))) {
                switches.add(t.text);
                continue;
            }
            if (cur == null) cur = new Side();
            if (word(t)) {
                if (t.text.equals(CONTENT)) cur.content = true;
                else if (t.text.equals(FILES_OF)) cur.filesOf = true;
                else if (t.text.equals(MANUAL)) cur.kind = LIST;
                else if (t.text.equals(SMART)) cur.kind = SMART_LIST;
                else if (t.text.equals(FILE_WORD)) cur.kind = EXPORT;
                else cur.kind = FS;
                if (t.text.equals(FOLDER) && (cur.content || cur.filesOf)) p.error = Words.ListCmdW.USAGE;
                continue;
            }
            cur.raw = t.text;
            if ((cur.content || cur.filesOf) && cur.kind == FS) p.error = Words.ListCmdW.USAGE;   // "תוכן" בלי "ידנית"
            if (cur.content && cur.kind == SMART_LIST) p.error = Words.ListCmdW.CONTENT_SMART;
            p.sides.add(cur);
            cur = null;
        }
        if (cur != null) p.error = Words.ListCmdW.USAGE;                     // מילת סוג בלי שם אחריה
        for (int i = 0; i < p.sides.size(); i++) {                            // מ0.18: "קובץ" — יעד (יצוא); מ0.22: או מקור לרשימה (ייבוא)
            Side x = p.sides.get(i);
            if (x.kind != EXPORT) continue;
            boolean export = i == p.sides.size() - 1 && i > 0;
            boolean imp = i == 0 && p.sides.size() == 2 && p.sides.get(1).kind == LIST;
            if ((!export && !imp) || x.content || x.filesOf) p.error = Words.DbCmd.USAGE_EXPORT;
        }
        return p;
    }

    /** הדרך לרשימה: "א\ב" או "א/ב"; "*" — הבית; משתנים (%…%) מוחלפים. */
    static void resolve(Side s, Map<String, String> vars) {
        String raw = s.raw;
        if (vars != null) for (Map.Entry<String, String> e : vars.entrySet()) raw = raw.replace(Batch.var(e.getKey()), e.getValue());
        s.raw = raw;
        s.path = new ArrayList<String>();
        if (raw.trim().equals(HOME)) {
            s.home = true;
            return;
        }
        for (String part : raw.replace('\\', '/').split("/")) if (!part.trim().isEmpty()) s.path.add(part.trim());
        if (!s.path.isEmpty()) {
            String last = s.path.get(s.path.size() - 1);
            if (last.contains("*") || last.contains("?")) {
                s.mask = last;
                s.path.remove(s.path.size() - 1);
            }
        }
        if (s.path.isEmpty() && s.mask == null) s.home = true;
    }

    /** "א\ב" — כמו שהפקודות כותבות דרך לרשימה. */
    static String pathText(List<String> path) {
        StringBuilder sb = new StringBuilder();
        for (String p : path) sb.append(sb.length() > 0 ? "\\" : "").append(p);
        return sb.toString();
    }

    /** התנאים — מילה מול מילה: מה רק באחת, מה רק בשנייה. */
    static String condsDiff(String ca, String cb, String la, String lb) {
        List<String> a = Batch.tokens(ca == null ? "" : ca), b = Batch.tokens(cb == null ? "" : cb);
        List<String> onlyA = new ArrayList<String>(), onlyB = new ArrayList<String>();
        for (String x : a) if (!b.contains(x)) onlyA.add(x);
        for (String x : b) if (!a.contains(x)) onlyB.add(x);
        if (onlyA.isEmpty() && onlyB.isEmpty()) return Words.ListCmdW.SAME_CONDS;
        return Words.ListCmdW.conds(la, join(onlyA), lb, join(onlyB));
    }

    private static String join(List<String> xs) {
        StringBuilder sb = new StringBuilder();
        for (String x : xs) sb.append(sb.length() > 0 ? " " : "").append(x);
        return sb.length() == 0 ? "—" : sb.toString();
    }

}
