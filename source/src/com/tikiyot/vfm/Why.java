package com.tikiyot.vfm;

// 1.13.0: שורת "למה" בראש המסך (המשתמש, 15.10: "מרוב ריבוי האפשרויות קל לשכוח מה מטרת כל כלי וסביבה ושאר
// האפשרויות"). מוצגת כשנכנסים, באותיות קטנות; נגיעה מקפלת אותה — ונשארת מקופלת בפעם הבאה (לכל דבר בנפרד).
// אחראי: השורה בלבד. מה כתוב בה — מי שקורא לו (כלי, סביבה, רשימה, תוכנה, מסך).

import android.content.Context;
import android.view.View;
import android.widget.TextView;

final class Why {

    private static final String SECTION = "למה מקופל";   // תחביר

    private Why() { }

    static TextView bar(Context c) {
        TextView t = new TextView(c);
        t.setTextSize(12);
        t.setTextColor(Ui.SUB);
        t.setBackgroundColor(Ui.BG_SOFT);
        int p = Ui.dp(c, 8);
        t.setPadding(p + p, Ui.dp(c, 4), p + p, Ui.dp(c, 4));
        t.setVisibility(View.GONE);
        return t;
    }

    /** key — לזכור את הקיפול (מזהה כלי, מסלול רשימה…); why — ריק: השורה לא מוצגת. */
    static void show(final TextView t, final String key, final String why) {
        if (t == null) return;
        if (why == null || why.trim().isEmpty()) {
            t.setVisibility(View.GONE);
            return;
        }
        t.setVisibility(View.VISIBLE);
        fill(t, why.trim(), folded(key));
        t.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean now = !folded(key);
                try {
                    Conf.setBool(SECTION, clean(key), now);
                } catch (Throwable e) {
                    Log.e("למה — קיפול", e);
                }
                fill(t, why.trim(), now);
            }
        });
    }

    private static void fill(TextView t, String why, boolean folded) {
        t.setSingleLine(folded);
        t.setEllipsize(folded ? android.text.TextUtils.TruncateAt.END : null);
        t.setText(Words.WhyW.PREFIX + why);
    }

    private static boolean folded(String key) {
        try {
            return Conf.is(SECTION, clean(key), false);
        } catch (Throwable e) {
            return false;
        }
    }

    private static String clean(String key) {
        return key == null ? "" : key.replace('=', '-').replace('\n', ' ').replace('[', '(').replace(']', ')');
    }
}
