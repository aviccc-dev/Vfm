package com.tikiyot.vfm;

// 204: כיוון הכתב בקובצי פקודות. בשורה משמאל לימין, מסלול עם שתי מילים בעברית ("%הורדות%\לשלוח") מתהפך:
// הסימנים שבין שתי המילים (% \ /) נצמדים לעברית, והקטע כולו מוצג מימין לשמאל — "לשלוח\%הורדות%".
// התיקון: סימן כיוון בלתי נראה (LRM) אחרי כל מפריד, רק בתצוגה; בשמירה ובקריאה — יורד.
// שורות REM ו־ECHO — טקסט חופשי בעברית: בלי סימנים. אחראי: התצוגה בלבד; התוכן שנשמר — בלי שינוי.

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import java.util.Locale;

final class Bidi {

    static final char LRM = '‎';

    private Bidi() { }

    /** בלי סימני הכיוון — מה שנשמר ומה שמתוכנן. */
    static String strip(String s) {
        if (s == null) return null;
        if (s.indexOf(LRM) < 0 && s.indexOf('\u202A') < 0) return s;
        return s.replace(String.valueOf(LRM), "").replace("\u202A", "").replace("\u202C", "");
    }

    /** מוסיף סימן אחרי כל מפריד במסלול (\ / %) ואחרי רווח שמחוץ למרכאות; שורות REM / ECHO / :: — כמו שהן. */
    static String mark(String text) {
        String t = strip(text);
        StringBuilder out = new StringBuilder(t.length() + 16);
        int from = 0;
        while (from <= t.length()) {
            int nl = t.indexOf('\n', from);
            String line = nl < 0 ? t.substring(from) : t.substring(from, nl);
            if (plain(line)) out.append(line);
            else {
                boolean q = false;
                for (int i = 0; i < line.length(); i++) {
                    char c = line.charAt(i);
                    out.append(c);
                    if (c == '"') q = !q;
                    if (c == '\\' || c == '/' || c == '%' || (c == ' ' && !q)) out.append(LRM);
                }
            }
            if (nl < 0) break;
            out.append('\n');
            from = nl + 1;
        }
        return out.toString();
    }

    private static boolean plain(String line) {
        String s = line.trim();
        if (s.startsWith("@")) s = s.substring(1).trim();
        String u = s.toUpperCase(Locale.ROOT);
        return s.startsWith("::") || u.startsWith("REM") || u.startsWith("ECHO") || hebrew(s);
    }

    /**
     * 1.17.6: שורה שמתחילה בעברית (פקודה בעברית, כותרת) — נכתבת מימין לשמאל, בלי סימנים (המשתמש: "בעברית כותבים
     * מימין לשמאל, באנגלית משמאל לימין"); העורך קובע את הכיוון לכל שורה לפי האות הראשונה שלה.
     */
    static boolean hebrew(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '\u0590' && c <= '\u05FF') return true;
            if (Character.isLetter(c)) return false;
        }
        return false;
    }

    /** מסלול בתוך משפט בעברית (בתצוגה המקדימה): משמאל לימין, בלי היפוך. */
    static String path(String p) {
        return "‪" + mark(p) + "‬";
    }

    /** שומר על הסימנים תוך כדי הקלדה; מחיקה של סימן בלתי נראה — מוחקת את התו שלפניו (שהמחיקה לא "תיתקע"). */
    static void watch(final EditText e) {
        e.addTextChangedListener(new TextWatcher() {
            boolean busy;
            int delAt = -1;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (busy) return;
                delAt = count == 1 && after == 0 && start > 0 && s.charAt(start) == LRM ? start : -1;
            }

            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable ed) {
                if (busy) return;
                busy = true;
                try {
                    String cur = ed.toString();
                    int sel = e.getSelectionEnd();
                    if (sel < 0 || sel > cur.length()) sel = cur.length();
                    if (delAt > 0 && delAt <= cur.length()) {
                        cur = cur.substring(0, delAt - 1) + cur.substring(delAt);
                        sel = delAt - 1;
                    }
                    delAt = -1;
                    int logical = strip(cur.substring(0, Math.min(sel, cur.length()))).length();
                    String want = mark(cur);
                    if (want.equals(ed.toString())) return;
                    ed.replace(0, ed.length(), want);
                    int pos = 0, n = 0;
                    while (pos < want.length() && n < logical) {
                        if (want.charAt(pos) != LRM) n++;
                        pos++;
                    }
                    while (pos < want.length() && want.charAt(pos) == LRM) pos++;
                    e.setSelection(Math.min(pos, ed.length()));
                } finally {
                    busy = false;
                }
            }
        });
    }
}
