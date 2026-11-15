package com.tikiyot.vfm;

// מבנה 147 (ממצא 4): רשימת "בחירה" — קובץ פקודות שרץ על מה שנבחר במסך: איך הטקסט נפרס לכל קובץ שנבחר, על כמה קבצים
// הוא יודע לעבוד, והאם הוא מתאים לבחירה הזו. עד כאן ישב בתוך התפריטים (Menus, בלב), אבל זו לוגיקה של שפת הפקודות.
// אחראי: לפרוס ולשאול את התכנון. אסור לו: להציג מסך, ולהריץ.

import android.content.Context;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class Choice {

    private Choice() { }

    // ---------- רשימת "בחירה": פקודות על מה שנבחר ----------

    /** בקובץ פקודות שברשימת "בחירה": %נבחרים% — במקום הקובץ; הפקודה רצה על כל קובץ שנבחר. */
    static final String V_SELECTED = Batch.V_SELECTED;   // מבנה 73: ההגדרה — במנוע
    private static final String SEL = "%" + V_SELECTED + "%";

    /** הטקסט להרצה: כל שורה שיש בה %נבחרים% — פעם אחת לכל קובץ שנבחר, עם המסלול שלו (במרכאות); שאר השורות — כמו שהן. */
    static String expand(String text, List<String> paths) {
        StringBuilder sb = new StringBuilder();
        String[] lines = (text == null ? "" : text).replace("\r", "").split("\n");
        for (String raw : lines) {
            if (!raw.contains(SEL)) {
                sb.append(raw).append('\n');
                continue;
            }
            for (String p : paths) {
                boolean q = false;
                int i = 0;
                while (i < raw.length()) {
                    if (raw.startsWith(SEL, i)) {
                        sb.append(q ? p : "\"" + p + "\"");                    // השורה כבר שמה מרכאות — רק המסלול
                        i += SEL.length();
                        continue;
                    }
                    char c = raw.charAt(i++);
                    if (c == '"') q = !q;
                    sb.append(c);
                }
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    /**
     * כמה קבצים הפקודה מקבלת במקום של %נבחרים% — מההגדרה האחת של הפקודות (Commands): 0 — אין %נבחרים% בקובץ; 1 — קובץ
     * אחד בלבד (שנה, הפעל, פתח…; או %נבחרים% כיעד); 2 — כמה שנבחרו (המקור של העתק / העבר, מחק, הודעה).
     */
    static int arity(String text) {
        int out = 0;
        for (String raw : Bidi.strip(text == null ? "" : text).replace("\r", "").split("\n")) {
            String l = Commands.normal(raw);
            if (!l.contains(SEL) || l.startsWith("::") || Program.isHeader(l)) continue;
            List<String> tk = Batch.tokens(l);
            Commands.Def d = tk.isEmpty() ? null : Commands.get(tk.get(0));
            if (d != null && d.name.equals("REM")) continue;
            int pos = -1, at = 0;
            for (int i = 1; i < tk.size() && pos < 0; i++) {
                String t = tk.get(i);
                if (Batch.isSwitch(t) || TYPE_WORDS.contains(t)) continue;
                if (t.contains(SEL)) pos = at;
                at++;
            }
            boolean many = d != null && d.family == Commands.Family.FILE && (d.name.equals("ECHO")
                    || (pos >= 0 && pos < d.positional().size() && d.positional().get(pos).type == Commands.Type.FILES));
            out = out == 1 || !many ? 1 : 2;
        }
        return out;
    }

    private static final List<String> TYPE_WORDS = Arrays.asList(ListCmd.MANUAL, ListCmd.SMART, ListCmd.CONTENT, ListCmd.FILES_OF,
            ListCmd.FOLDER, ListCmd.FILE_WORD);
    /** פקודות הכונן הפשוטות — רק אותן מתכננים כדי להחליט אם הכפתור מופיע (התכנון שלהן זול). */
    private static final List<String> PLAIN = Arrays.asList("COPY", "MOVE", "DEL", "REN", "MD", "RD", "START", "ECHO");

    /**
     * "הפקודה קובעת בעצמה מתי היא מופיעה": התכנון הרגיל של הטקסט (אותה בדיקה שלפני כל הרצה) — בלי שגיאות: מתאימה. null —
     * לא נבדק כאן: יש בקובץ מקום פתוח (נשאל רק בהרצה), או שורה שהתכנון שלה יקר (רשימות, אופן, פקודות פתיחה).
     */
    static Boolean planned(String expanded, File here, Map<String, String> vars, Library lib) {
        List<String> lines = Arrays.asList(Bidi.strip(expanded).replace("\r", "").split("\n"));
        if (!CmdTemplates.holes(lines).isEmpty()) return null;
        boolean any = false;
        for (String raw : lines) {
            String l = Commands.normal(raw);
            if (l.isEmpty() || l.startsWith("::") || Program.isHeader(l)) continue;
            List<String> tk = Batch.tokens(l);
            Commands.Def d = tk.isEmpty() ? null : Commands.get(tk.get(0));
            if (d != null && d.name.equals("REM")) continue;
            if (d == null || !PLAIN.contains(d.name) || ListCmd.parse(l, new ArrayList<String>()) != null) return null;
            any = true;
        }
        if (!any) return null;
        Batch.Plan p = Batch.plan(expanded, here, vars, lib);
        return p.errors == 0;
    }

    private static final Once<Boolean> fitCache = new Once<Boolean>(300);

    /**
     * האם פקודה מרשימת "בחירה" מתאימה למה שנבחר עכשיו. המשפך: קודם כמה נבחרו מול מה שהפקודה מקבלת (זול, תמיד); ורק אז
     * התכנון — על דוגמה (קובץ אחד או שניים), ופעם אחת לכל צורת בחירה (אחד / כמה): התוצאה נשמרת עד שהקובץ משתנה.
     */
    static boolean fits(Context c, File f, String text, List<String> files, List<String> dirs, int lists) {
        int n = files.size() + dirs.size();
        if (lists > 0 || n == 0) return false;                                // רשימות שנבחרו — לא קבצים
        int ar = arity(text);
        if (ar == 0) return true;
        if (ar == 1 && n != 1) return false;
        if (!dirs.isEmpty()) return true;                                     // תיקייה — התכנון שלה סורק את כולה; לא כאן
        String kind = n == 1 ? "1" : "2";
        Boolean had = fitCache.had(f, kind);                                  // מבנה 146
        if (had != null) return had;
        boolean ok = true;
        try {
            Boolean p = planned(expand(text, files.subList(0, Math.min(2, files.size()))), f.getParentFile(),
                    Batch.vars(f.getParentFile(), Vols.internalRoot(c), Vols.cardRoot(c)), Library.get(c));
            ok = p == null || p;
        } catch (Throwable t) {
            Log.e("Choice.fits", t);
        }
        return fitCache.put(f, kind, ok);
    }

    private static final Once<String> texts = new Once<String>(300);

    /** הטקסט של קובץ פקודות — נקרא פעם אחת, עד שהקובץ משתנה (פס הבחירה שואל בכל שינוי של הבחירה). null — לא נקרא. */
    static String textOf(File f) {
        String had = texts.had(f, "");
        if (had != null) return had;
        try {
            return texts.put(f, "", Files.readUtf8(f));
        } catch (Throwable e) {
            return null;
        }
    }
}
