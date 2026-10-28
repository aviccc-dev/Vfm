package com.tikiyot.vfm;

// 1.14.0: פס "הדבקה כאן" של הלוח הווירטואלי (VClip) — אותו פס ברשימה ידנית, במסך הבית ובסביבה.
// מה בלוח, אם אפשר להדביק כאן (ואם לא — למה), "הדבקה כאן", "ביטול". אחראי: הפס בלבד; מה קורה בהדבקה — VClip.

import android.app.Activity;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

final class PasteBar {

    /** המקום: אם אפשר להדביק בו (null — אפשר), וההדבקה עצמה. */
    interface Place {
        String cannot();

        VClip.Result paste();
    }

    final LinearLayout view;
    private final TextView text;
    private final Button paste;
    private final Activity a;
    private final Place place;
    private final Runnable after;

    PasteBar(Activity a, Place place, Runnable after) {
        this.a = a;
        this.place = place;
        this.after = after;
        view = new LinearLayout(a);
        view.setOrientation(LinearLayout.HORIZONTAL);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setBackgroundColor(Ui.HEADER);
        view.setPadding(Ui.dp(a, 8), Ui.dp(a, 4), Ui.dp(a, 8), Ui.dp(a, 4));
        text = new TextView(a);
        text.setTextColor(Ui.TEXT);
        text.setTextSize(14);
        view.addView(text, new LinearLayout.LayoutParams(0, -2, 1f));
        paste = button(Words.PASTE_HERE, new Runnable() {
            @Override public void run() { paste(); }
        });
        button(Words.CANCEL, new Runnable() {
            @Override
            public void run() {
                VClip.clear();
                refresh();
            }
        });
        view.setVisibility(View.GONE);
    }

    private Button button(String label, final Runnable r) {
        Button b = new Button(a);
        b.setText(label);
        b.setAllCaps(false);
        b.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { r.run(); }
        });
        view.addView(b);
        return b;
    }

    /** מה בלוח עכשיו (בכל חזרה למסך — אולי הועתק משהו במסך אחר). */
    void refresh() {
        if (VClip.empty()) {
            view.setVisibility(View.GONE);
            return;
        }
        view.setVisibility(View.VISIBLE);
        String why = place.cannot();
        text.setText("📋 " + VClip.label() + (why != null ? "\n" + why : ""));
        paste.setEnabled(why == null);
    }

    private void paste() {
        long t0 = System.currentTimeMillis();
        VClip.Result r = place.paste();
        refresh();
        if (after != null) after.run();
        long ms = System.currentTimeMillis() - t0;                            // 1.23.1: זמני — למדידה במכשיר (בגרסה הבאה — יוצא)
        Ui.toast(a, Words.VClipW.PASTED + r.done + (r.skipped > 0 ? Words.VClipW.SKIPPED + r.skipped + " (" + r.why + ")" : "")
                + Words.VClipW.took(ms));
    }
}
