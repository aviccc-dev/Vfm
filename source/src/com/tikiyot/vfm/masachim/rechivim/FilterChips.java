package com.tikiyot.vfm;

// מבנה 286 (אפיון החיפוש, 8 — המשתמש: "הכל"): המסננים שפעילים עכשיו (מחלונית ההגדרות) — שבבים מתחת לשורת החיפוש, כל אחד עם ✕.
// רואים מה מצמצם את התוצאות גם כשהחלונית סגורה, ונגיעה מסירה אותו. מה שמסנן ומה כתוב עליו — Parts.active; ההסרה — Parts.drop.
// אחראי: מה שרואים ונוגעים. אסור לו: לחפש או לשמור.

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

import java.util.List;

final class FilterChips {

    final Flow view;
    private final Activity a;
    private final SmartForm.Parts parts;
    /** מה שמוצג עכשיו — כדי לא לבנות מחדש בכל אות שמקלידים (המשפך). */
    private String shown = null;

    FilterChips(Activity a, SmartForm.Parts parts) {
        this.a = a;
        this.parts = parts;
        view = new Flow(a, 6, 4);
        view.setPadding(Ui.dp(a, 8), Ui.dp(a, 2), Ui.dp(a, 8), Ui.dp(a, 2));
        view.setVisibility(View.GONE);
    }

    void refresh() {
        List<String[]> on = parts.active();
        StringBuilder sig = new StringBuilder();
        for (String[] f : on) sig.append(f[1]).append('\n');
        if (sig.toString().equals(shown)) return;
        shown = sig.toString();
        view.removeAllViews();
        for (final String[] f : on) {
            TextView t = Ui.info(a);
            t.setText(Words.Find.chip(f[0]));
            t.setContentDescription(Words.Find.dropFilter(f[0]));
            t.setBackgroundColor(Ui.BG_SOFT);
            t.setPadding(Ui.dp(a, 10), Ui.dp(a, 6), Ui.dp(a, 10), Ui.dp(a, 6));
            t.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { parts.drop(a, f[1]); }   // הטעינה מחדש אומרת למסך — והוא מחפש שוב
            });
            view.addView(t);
        }
        view.setVisibility(on.isEmpty() ? View.GONE : View.VISIBLE);
    }
}
