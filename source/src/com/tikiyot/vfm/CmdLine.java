package com.tikiyot.vfm;

// 1.23.0: שורת פקודה אחת לממשק הפקודות — קריאה ושינוי של פרמטר אחד לפי ההגדרה (Commands), בלי לאבד דבר
// (claude/mimshak-pkudot.md, בדיקת באגים 5–8): מתג שלא מוכר, משתנה, מרכאות — נשארים; שורה שלא שונתה — נשארת אות באות;
// פרמטר חדש — במקום שלו לפי הסדר שבהגדרה; שורה קיימת — לא מסודרת מחדש. בלי ממשק — נבדק.
// אחראי: השורה כטקסט ↔ הערכים. אסור לו: לבדוק תקינות או להריץ (זה Batch).

import java.util.ArrayList;
import java.util.List;

final class CmdLine {

    /** המילה הראשונה כמו שנכתבה. */
    final String word;
    /** ההגדרה (משפחת הקבצים), או null — שורה שהממשק לא מכיר. */
    final Commands.Def def;
    /** השורה כמו שנכתבה — כל עוד לא שונתה. */
    private final String raw;
    private boolean changed;
    /** מה שאחרי הפקודה, בלי מרכאות (כמו Batch.tokens). */
    private final List<String> tokens = new ArrayList<String>();
    /** ECHO / REM — הטקסט כמו שהוא. */
    private String rest = "";

    private CmdLine(String word, Commands.Def def, String raw) {
        this.word = word;
        this.def = def;
        this.raw = raw;
    }

    static CmdLine parse(String line) {
        String l = Bidi.strip(line == null ? "" : line).trim();
        List<String> tk = Batch.tokens(l);
        String w = tk.isEmpty() ? "" : tk.get(0);
        Commands.Def d = Commands.get(w);
        if (d != null && d.family != Commands.Family.FILE) d = null;
        CmdLine c = new CmdLine(w, d, l);
        if (isText(d)) c.rest = l.length() > w.length() ? l.substring(w.length()).trim() : "";
        else for (int i = 1; i < tk.size(); i++) c.tokens.add(tk.get(i));
        return c;
    }

    /** פקודה חדשה, בלי ערכים. */
    static CmdLine of(Commands.Def d) {
        CmdLine c = new CmdLine(d.name, d, d.name);
        c.changed = true;
        return c;
    }

    private static boolean isText(Commands.Def d) {
        return d != null && (d.name.equals("ECHO") || d.name.equals("REM"));
    }

    /** השורה. לא שונתה — כמו שנכתבה. */
    String text() {
        if (!changed) return raw;
        if (isText(def)) return rest.isEmpty() ? word : word + " " + rest;
        StringBuilder sb = new StringBuilder(word);
        for (String t : tokens) sb.append(' ').append(quote(t));
        return sb.toString();
    }

    private static String quote(String t) {
        if (t.isEmpty()) return "\"\"";
        if (!t.contains(" ") && !t.contains("\t")) return t;
        int c = t.indexOf(':');
        if (Batch.isSwitch(t) && c > 0) return t.substring(0, c + 1) + "\"" + t.substring(c + 1) + "\"";
        return "\"" + t + "\"";
    }

    // ---------- פרמטר לפי מקום ----------

    /** COMP /רשימה: — אז המקור הוא הרשימה, והיעד הוא הפרמטר הראשון. */
    private boolean listInsteadOfSource() {
        if (def == null || !def.name.equals("COMP")) return false;
        for (String t : tokens) if (t.startsWith(Batch.SW_LIST)) return true;
        return false;
    }

    /** המקום של p בין הפרמטרים לפי מקום, בשורה הזו; -1 — לא רלוונטי (המקור כשיש /רשימה:). */
    int slot(Commands.Param p) {
        if (def == null || p.isSwitch()) return -1;
        int i = def.positional().indexOf(p);
        if (listInsteadOfSource()) return i == 0 ? -1 : i - 1;
        return i;
    }

    private List<Integer> positionalAt() {
        List<Integer> out = new ArrayList<Integer>();
        for (int i = 0; i < tokens.size(); i++) if (!Batch.isSwitch(tokens.get(i))) out.add(i);
        return out;
    }

    String value(Commands.Param p) {
        if (isText(def)) return rest;
        if (!p.isSwitch()) {
            int s = slot(p);
            List<Integer> at = positionalAt();
            return s < 0 || s >= at.size() ? "" : tokens.get(at.get(s));
        }
        for (String t : tokens) {
            String k = keyOf(p, t);
            if (k == null) continue;
            if (p.type == Commands.Type.FLAG) return "1";
            if (p.type == Commands.Type.CHOICE && !k.endsWith(":"))           // /Y · /-Y
                return k.equalsIgnoreCase("/Y") ? Batch.TAKEN_REPLACE : Batch.TAKEN_ASK;
            String v = t.substring(k.length());
            if (p.type == Commands.Type.PERCENT && v.endsWith("%")) v = v.substring(0, v.length() - 1);
            return v;
        }
        return "";
    }

    /** המתג (או השם הנרדף) שהטוקן כתוב בו, או null. */
    private static String keyOf(Commands.Param p, String t) {
        for (String k : p.all())
            if (k.endsWith(":") ? t.regionMatches(true, 0, k, 0, k.length()) : t.equalsIgnoreCase(k)) return k;
        return null;
    }

    /** קובע ערך. ריק (או מתג כבוי) — הפרמטר יוצא מהשורה. */
    void set(Commands.Param p, String v) {
        String val = v == null ? "" : v.trim();
        changed = true;
        if (isText(def)) {
            rest = val;
            return;
        }
        if (!p.isSwitch()) {
            setPositional(slot(p), val);
            return;
        }
        int idx = -1;
        for (int i = 0; i < tokens.size(); i++) if (keyOf(p, tokens.get(i)) != null) idx = i;
        String tok;
        if (p.type == Commands.Type.FLAG) tok = val.isEmpty() ? null : idx >= 0 ? tokens.get(idx) : p.key;
        else if (val.isEmpty()) tok = null;
        else tok = p.key + val + (p.type == Commands.Type.PERCENT && !val.endsWith("%") ? "%" : "");
        if (idx >= 0) {
            if (tok == null) tokens.remove(idx);
            else tokens.set(idx, tok);
            return;
        }
        if (tok == null) return;
        int order = def.params.indexOf(p), at = tokens.size();                  // לפי הסדר שבהגדרה (החלטה 8)
        for (int i = 0; i < tokens.size(); i++) {
            Commands.Param q = def.switchFor(tokens.get(i));
            if (q != null && def.params.indexOf(q) > order) {
                at = i;
                break;
            }
        }
        tokens.add(at, tok);
    }

    private void setPositional(int slot, String v) {
        if (slot < 0) return;
        List<Integer> at = positionalAt();
        if (slot < at.size()) {
            if (v.isEmpty() && slot == at.size() - 1) tokens.remove((int) at.get(slot));
            else tokens.set(at.get(slot), v);
            return;
        }
        if (v.isEmpty()) return;
        int put = at.isEmpty() ? firstSwitchOrEnd() : at.get(at.size() - 1) + 1;
        for (int i = at.size(); i < slot; i++) tokens.add(put++, "");          // פרמטר שחסר לפניו — ריק במקומו
        tokens.add(put, v);
    }

    private int firstSwitchOrEnd() {
        for (int i = 0; i < tokens.size(); i++) if (Batch.isSwitch(tokens.get(i))) return i;
        return tokens.size();
    }

    /** הפרמטרים לפי מקום שחסרים (לפני הכנסה לקובץ). */
    List<Commands.Param> missing() {
        List<Commands.Param> out = new ArrayList<Commands.Param>();
        if (def == null || isText(def)) return out;
        for (Commands.Param p : def.positional()) if (slot(p) >= 0 && value(p).isEmpty()) out.add(p);
        return out;
    }

    /** מקור מתוך השוואה (%רק-במקור% / %רק-ביעד%) — צריך COMP לפניו. */
    boolean usesCompare() {
        for (String t : tokens)
            if (t.equals(Batch.var(Batch.V_ONLY_A)) || t.equals(Batch.var(Batch.V_ONLY_B))) return true;
        return false;
    }

    /** פקודה שמוציאה קבצים ממקומם — מסומנת (בדיקת באגים 12). */
    boolean destructive() {
        if (def == null) return false;
        String n = def.name;
        return n.equals("DEL") || n.equals("MOVE") || (n.equals("RD") && value(def.switches().get(0)).length() > 0);
    }
}
