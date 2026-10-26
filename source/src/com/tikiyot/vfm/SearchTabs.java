package com.tikiyot.vfm;

// "הוספת חיפוש" (150) — סוגי החיפוש בטופס אחד, עם לשוניות בראשו: רשימה חכמה · חיפוש חופשי · כפולים.
// כל לשונית — הטופס של היום של אותו כלי, בלי שינוי; כאן רק הלשוניות והמעבר ביניהן. גם בעריכה, עם החלפת סוג.
// מנהל הקבצים (151) — אינו לשונית: רשימה מוכנה בתוך הרשימה החכמה ("אפשרות פשוטה ולא חכמה").

import android.app.Activity;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

final class SearchTabs {

    /** הסוגים, לפי סדר הלשוניות. */
    static final String[] KINDS = {FnSmart.KIND, FnDup.KIND};

    private SearchTabs() { }

    static boolean isSearch(String kind) {
        for (String k : KINDS) if (k.equals(kind)) return true;
        return false;
    }

    /** הטופס, בלשונית kind. t — ההגדרות הקיימות; עוברות רק ללשונית מהסוג שלהן. done — עם הסוג שנבחר (target.kind). */
    static void configure(final Activity a, String asked, final String name, final Target t, final Func.Done done) {
        final String kind = asked;
        final Func fn = Func.byKind(kind);
        if (fn == null) return;
        Ui.formHeader(new Ui.Header() {
            @Override
            public View build(final Runnable close) {
                // 197: פס דק — "סוג: חכמה | כפולים", בשורה אחת (במקום לשוניות גבוהות עם סמל)
                LinearLayout seg = new LinearLayout(a);
                seg.setOrientation(LinearLayout.HORIZONTAL);
                android.graphics.drawable.GradientDrawable frame = new android.graphics.drawable.GradientDrawable();
                frame.setCornerRadius(Ui.dp(a, 6));
                frame.setStroke(Math.max(1, Ui.dp(a, 1)), Ui.HEADER_SOFT);
                seg.setBackground(frame);
                seg.setPadding(1, 1, 1, 1);
                for (int i = 0; i < KINDS.length; i++) {
                    final String k = KINDS[i];
                    TextView b = new TextView(a);
                    b.setText(Words.SearchTabs.SHORT[i]);
                    b.setGravity(Gravity.CENTER);
                    b.setTextSize(14);
                    b.setPadding(Ui.dp(a, 18), Ui.dp(a, 6), Ui.dp(a, 18), Ui.dp(a, 6));
                    boolean on = k.equals(kind);
                    b.setTextColor(on ? Ui.ACCENT_DARK : Ui.SUB);
                    if (on) b.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                    b.setBackgroundColor(on ? Ui.BAR : 0x00000000);
                    if (!on) b.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            close.run();                                      // הטופס הזה נסגר, והלשונית האחרת נפתחת
                            configure(a, k, name, t, done);
                        }
                    });
                    seg.addView(b);
                }
                LinearLayout hold = new LinearLayout(a);                       // הפס ברוחב שלו, לא עד הקצה
                hold.setOrientation(LinearLayout.HORIZONTAL);
                hold.addView(seg, new LinearLayout.LayoutParams(-2, -2));
                return SmartForm.line(a, Words.SearchTabs.KIND_L, hold, false);
            }
        });
        try {
            boolean same = t != null && kind.equals(t.kind);
            fn.configure(a, name, same ? t : null, done);
        } finally {
            Ui.formHeader(null);                                              // רק לטופס הזה
        }
    }
}
