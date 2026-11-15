package com.tikiyot.vfm;

/**
 * מבנה 135: עזר של הבדיקות — שורת פקודה באנגלית, כתובה בעברית (הפקודה והמתגים). עד כאן ישב בתוכנה (Commands.hebrew), מהעורך
 * הישן שנמחק; בתוכנה איש לא פנה אליו. הבדיקות צריכות אותו כדי לוודא ש"כל שורה בעברית מתוכננת בדיוק כמו המקבילה באנגלית".
 */
final class Ivrit {
    private Ivrit() { }

    /**
     * מ0.38: השורה כמו שהעורך מציג אותה — הפקודה והמתגים בעברית (המשתמש: "הכל בפלפון כבר מתחילים על צד שמאל").
     * שורה של רשימות (מילת סוג: ידנית, חכמה…) ושורה שכבר בעברית — כמו שהן.
     */
    static String hebrew(String line) {
        if (line == null) return "";
        String l = line.trim();
        if (l.isEmpty() || l.charAt(0) > 0x7F || l.startsWith("::")) return line;
        int sp = 0;
        while (sp < l.length() && !Character.isWhitespace(l.charAt(sp))) sp++;
        Commands.Def d = Commands.get(l.substring(0, sp));
        if (d == null || d.family != Commands.Family.FILE) return line;
        ListCmd.Parsed lp = d.name.equals("ECHO") || d.name.equals("REM") ? null : ListCmd.parse(l, new java.util.ArrayList<String>());
        if (lp != null) return line;
        String rest = l.substring(sp);
        if (d.name.equals("ECHO") || d.name.equals("REM")) return Commands.word(d) + rest;
        StringBuilder out = new StringBuilder(Commands.word(d));
        boolean q = false;
        int i = 0;
        while (i < rest.length()) {
            char c = rest.charAt(i);
            if (c == '"') q = !q;
            String hit = null, to = null;
            if (!q && c == '/' && i > 0 && Character.isWhitespace(rest.charAt(i - 1))) for (String[] m : Commands.HE_SWITCHES) {
                int e = i + m[1].length();
                if (rest.regionMatches(true, i, m[1], 0, m[1].length()) && (e == rest.length() || Character.isWhitespace(rest.charAt(e)))) { hit = m[1]; to = m[0]; }
            }
            if (hit != null) {
                out.append(to);
                i += hit.length();
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    /** קובץ שלם — שורה שורה. */
    static String hebrewText(String text) {
        if (text == null || text.isEmpty()) return "";
        String[] ls = text.split("\n", -1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ls.length; i++) sb.append(i > 0 ? "\n" : "").append(hebrew(ls[i]));
        return sb.toString();
    }
}
