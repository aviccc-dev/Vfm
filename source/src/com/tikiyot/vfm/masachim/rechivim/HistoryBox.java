package com.tikiyot.vfm;

// מבנה 284 (אפיון החיפוש, 4 — המשתמש, 19.11.2026: "לשמור היסטוריית חיפוש עד כמות מסויימת שתופיע בהגדרות"; "א" — ההיסטוריה
// מחזירה רק את המילים, ומחפשים כאן): מתחת לשורה ריקה — החיפושים האחרונים; נגיעה — המילים חוזרות לשורה. הכלל (האחרון ראשון,
// בלי כפילות, עד הכמות) — Search.remember. אחראי: מה שרואים ונוגעים. אסור לו: לשמור בעצמו — המסך שומר (Host).

import android.app.Activity;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

final class HistoryBox {

    interface Host {
        /** נגעו בחיפוש מההיסטוריה — המילים לשורה. */
        void use(String words);

        /** ההיסטוריה השתנתה — לשמור (כל הרשימה). */
        void saved(List<String> list);
    }

    /** מבנה 302: מה שהמסך מציג — נגלל; השורות עצמן — בתוכו. */
    final android.widget.ScrollView view;
    private final LinearLayout rows;
    /** מבנה 302: עד כמה שורות נראות בלי גלילה, והגובה המשוער של שורה (dp). */
    static final int SHOWN = 5, ROW_DP = 44;

    /**
     * מבנה 302 (הביקורת אחרי מ0.53, 2): הגובה של ההיסטוריה — עד SHOWN שורות לפי התוכן (‎-2); יותר — גובה קבוע, והשאר בגלילה.
     * עד כאן — עד 20 שורות בחלק שאינו נגלל: עם מקלדת פתוחה התחתונות ו"ניקוי" יצאו מהמסך. n — השורות, כולל "ניקוי".
     */
    static int heightDp(int n) {
        return n <= SHOWN ? -2 : SHOWN * ROW_DP + ROW_DP / 2;                 // חצי שורה נוספת — רואים שיש עוד
    }
    private final Activity a;
    private final Host host;
    private List<String> list;
    private int max;
    private boolean empty = true;

    HistoryBox(Activity a, List<String> list, int max, Host host) {
        this.a = a;
        this.host = host;
        this.max = max;
        this.list = Search.remember(list, null, max);
        view = new android.widget.ScrollView(a);
        rows = new LinearLayout(a);
        rows.setOrientation(LinearLayout.VERTICAL);
        view.addView(rows);
        fill();
    }

    /** חיפוש שנעשה (פתחו תוצאה, "חפש" במקלדת, יצאו מהמסך) — נכנס להיסטוריה. */
    void remember(String words) {
        List<String> n = Search.remember(list, words, max);
        if (n.equals(list)) return;
        list = n;
        host.saved(n);
        fill();
    }

    /** השורה ריקה — רואים את ההיסטוריה; יש בה משהו — לא. */
    void show(boolean emptyField) {
        empty = emptyField;
        view.setVisibility(empty && !list.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void fill() {
        rows.removeAllViews();
        for (final String w : list) {
            TextView t = Ui.info(a);
            t.setText(Words.Find.historyItem(w));
            t.setTextSize(15);
            t.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { host.use(w); }
            });
            rows.addView(t);
        }
        if (!list.isEmpty()) {
            TextView clear = Ui.info(a);
            clear.setText(Words.Find.HISTORY_CLEAR);
            clear.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    Ui.confirm(a, Words.Find.HISTORY_CLEAR, Words.Find.HISTORY_CLEAR_ASK, Words.Find.HISTORY_CLEAR_YES, new Runnable() {
                        @Override public void run() {
                            list = new ArrayList<String>();
                            host.saved(list);
                            fill();
                        }
                    });
                }
            });
            rows.addView(clear);
        }
        int h = heightDp(list.isEmpty() ? 0 : list.size() + 1);
        view.setLayoutParams(new LinearLayout.LayoutParams(-1, h < 0 ? h : Ui.dp(a, h)));
        show(empty);
    }

    /** מבנה 284: "כמות בהיסטוריה" — שורה בחלונית ההגדרות של החיפוש; 0 — בלי היסטוריה. */
    View countRow(final Runnable saveMax) {
        LinearLayout row = new LinearLayout(a);
        row.setOrientation(LinearLayout.HORIZONTAL);
        TextView label = Ui.info(a);
        label.setText(Words.Find.HISTORY_MAX);
        final EditText n = Ui.field(a, String.valueOf(max), null);
        n.setInputType(InputType.TYPE_CLASS_NUMBER);
        n.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean has) {
                if (has) return;
                int m = max;
                try {
                    m = Math.max(0, Math.min(500, Integer.parseInt(n.getText().toString().trim())));
                } catch (NumberFormatException e) {
                    // לא מספר — נשאר מה שהיה, ונראה בשדה
                }
                n.setText(String.valueOf(m));
                if (m == max) return;
                max = m;
                saveMax.run();
                remember(null);                                                 // פחות מקום — הישנים יוצאים
            }
        });
        row.addView(label, new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(n, new LinearLayout.LayoutParams(Ui.dp(a, 90), -2));
        return row;
    }

    int max() {
        return max;
    }
}
