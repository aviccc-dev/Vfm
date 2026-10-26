package com.tikiyot.vfm;

// סרגל בחירה: מספר הנבחרים, פעולות, וסיום. משותף לכל המסכים.
// הכלל האחיד: נגיעה ארוכה מתחילה בחירה, וכל הפעולות כאן — לפריט אחד ולהרבה באותה צורה.
// הצורה (83): למעלה ✕ ומספר הנבחרים; למטה ארבע פעולות עיקריות — צלמית ומילה — ו"⋯ עוד" לכל השאר.
// 1.14.0: ובמסכים הווירטואליים — "פיזי": כל מה שנוגע בכונן, בתפריט משלו (לא בין הפעולות הרגילות).

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public final class SelectBar {

    private final Activity a;
    public final LinearLayout view;
    private final LinearLayout top, row;
    private final TextView count;
    private final View more, physical;

    /** פעולה בפס: עיקרית (אריח) או תחת "עוד". */
    private static final class Item {
        String icon, shortLabel, label;
        Runnable action;
        When when;
        boolean main, danger, physical;
        View tile;
    }

    private final List<Item> items = new ArrayList<Item>();

    public SelectBar(Activity a, final Runnable onDone) {
        this.a = a;
        view = new LinearLayout(a);
        view.setOrientation(LinearLayout.VERTICAL);
        view.setBackgroundColor(Ui.BAR);
        view.setVisibility(View.GONE);

        top = new LinearLayout(a);
        top.setLayoutDirection(Ui.DIRECTION);
        top.setGravity(Gravity.CENTER_VERTICAL);
        int p = Ui.dp(a, 4);
        top.setPadding(p, 0, p, 0);
        View x = iconButton("close", Words.DONE);
        x.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { onDone.run(); }
        });
        top.addView(x);
        count = new TextView(a);
        count.setTextSize(14);
        count.setTextColor(Ui.TEXT);
        count.setPadding(Ui.dp(a, 8), 0, Ui.dp(a, 8), 0);
        top.addView(count, new LinearLayout.LayoutParams(0, -2, 1f));
        view.addView(top);

        row = new LinearLayout(a);
        row.setLayoutDirection(Ui.DIRECTION);
        row.setPadding(0, 0, 0, Ui.dp(a, 4));
        view.addView(row);
        physical = tile("folder", Words.SelBar.PHYSICAL, false);              // 1.14.0
        physical.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showGroup(true); }
        });
        physical.setVisibility(View.GONE);
        row.addView(physical, weight());
        more = tile("more", Words.SelBar.MORE, false);
        more.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showGroup(false); }
        });
        more.setVisibility(View.GONE);
        row.addView(more, weight());
    }

    private LinearLayout.LayoutParams weight() {
        return new LinearLayout.LayoutParams(0, -2, 1f);
    }

    private View iconButton(String icon, String what) {
        android.widget.ImageView i = icon(icon, false);
        int p = Ui.dp(a, 10);
        i.setPadding(p, p, p, p);
        i.setContentDescription(what);
        ripple(i);
        return i;
    }

    /** צלמית מהסט האחיד (res/drawable/ic_<שם>.xml), בצבע הטקסט או באדום. */
    private android.widget.ImageView icon(String name, boolean danger) {
        android.widget.ImageView i = new android.widget.ImageView(a);
        int id = a.getResources().getIdentifier("ic_" + name, "drawable", a.getPackageName());
        if (id != 0) i.setImageResource(id);
        i.setColorFilter(danger ? Actions.WARNING : Ui.ICON);
        int s = Ui.dp(a, 24);
        i.setLayoutParams(new LinearLayout.LayoutParams(s + 2 * Ui.dp(a, 10), s + 2 * Ui.dp(a, 10)));
        return i;
    }

    private void ripple(View v) {
        TypedValue tv = new TypedValue();
        if (a.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true))
            v.setBackgroundResource(tv.resourceId);
        v.setClickable(true);
    }

    /** אריח: צלמית, ומילה מתחתיה. */
    private View tile(String icon, String label, boolean danger) {
        LinearLayout t = new LinearLayout(a);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setGravity(Gravity.CENTER_HORIZONTAL);
        int p = Ui.dp(a, 4);
        t.setPadding(p, p, p, p);
        ripple(t);
        android.widget.ImageView i = icon(icon, danger);
        i.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(a, 26), Ui.dp(a, 26)));
        TextView l = new TextView(a);
        l.setText(label);
        l.setTextSize(12);
        l.setGravity(Gravity.CENTER);
        l.setMaxLines(2);
        if (android.os.Build.VERSION.SDK_INT >= 23) {            // שבירה רק בין מילים, לא באמצע מילה
            l.setHyphenationFrequency(android.text.Layout.HYPHENATION_FREQUENCY_NONE);
            l.setBreakStrategy(android.text.Layout.BREAK_STRATEGY_SIMPLE);
        }
        l.setEllipsize(android.text.TextUtils.TruncateAt.END);
        l.setTextColor(danger ? Actions.WARNING : Ui.TEXT);
        t.addView(i);
        t.addView(l);
        t.setContentDescription(label);
        return t;
    }

    /** כפתור בשורה העליונה, ליד ✕ (למשל "בחירת הכול"). */
    public Button add(String label, final Runnable action) {
        Button b = new Button(a, null, android.R.attr.borderlessButtonStyle);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(13);
        b.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { action.run(); }
        });
        top.addView(b);
        return b;
    }

    /** תנאי להצגת פעולה, למשל "רק כשנבחר פריט אחד". */
    public interface When {
        boolean ok();
    }

    /**
     * פעולה בפס. main — בין הארבע העיקריות (אריח עם צלמית); אחרת — תחת "עוד".
     * danger — פעולה הורסת: באדום, והמרשם שם אותה אחרונה.
     */
    public void addWhen(String icon, String shortLabel, String label, final Runnable action, When when,
                        boolean main, boolean danger) {
        addWhen(icon, shortLabel, label, action, when, main, danger, false);
    }

    /** physical — בתפריט "פיזי" (1.14.0), לא בין העיקריות ולא תחת "עוד". */
    public void addWhen(String icon, String shortLabel, String label, final Runnable action, When when,
                        boolean main, boolean danger, boolean physical) {
        final Item it = new Item();
        it.physical = physical;
        if (physical) main = false;
        it.icon = icon;
        it.shortLabel = shortLabel == null ? label : shortLabel;
        it.label = label;
        it.action = action;
        it.when = when;
        it.main = main;
        it.danger = danger;
        if (main) {
            it.tile = tile(icon, it.shortLabel, danger);
            it.tile.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { action.run(); }
            });
            it.tile.setVisibility(View.GONE);
            row.addView(it.tile, row.getChildCount() - 2, weight());     // "פיזי" ו"עוד" — אחרונים
        }
        items.add(it);
    }

    /** הפעולות שתחת "עוד" (או "פיזי") ומתאימות לבחירה הנוכחית. */
    private List<Item> group(boolean phys) {
        List<Item> out = new ArrayList<Item>();
        for (Item it : items) if (!it.main && it.physical == phys && it.when.ok()) out.add(it);
        return out;
    }

    private void showGroup(boolean phys) {
        final List<Item> list = group(phys);
        if (list.isEmpty()) return;
        String[] labels = new String[list.size()];
        for (int i = 0; i < labels.length; i++) labels[i] = list.get(i).label;
        AlertDialog.Builder b = new AlertDialog.Builder(a);
        if (phys) b.setTitle(Words.SelBar.PHYSICAL_TITLE);
        b.setItems(labels, new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface d, int w) { list.get(w).action.run(); }
        }).show();
    }

    private final List<View> hidden = new ArrayList<View>();

    /** מה שמוסתר בזמן בחירה (למשל כפתורי ההוספה ברשימה ידנית) — שלא יהיו שתי שורות כפתורים. */
    public void hideWhileOn(View v) {
        hidden.add(v);
        v.setVisibility(view.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
    }

    public void show(boolean on) {
        view.setVisibility(on ? View.VISIBLE : View.GONE);
        for (View v : hidden) v.setVisibility(on ? View.GONE : View.VISIBLE);
    }

    public void setCount(int n) {
        setCount(Words.selected(n));
    }

    /** טקסט חופשי במקום הספירה (למשל בכפולים: "נבחרו 3 · 12MB"). */
    public void setCount(String text) {
        count.setText(text);
        boolean any = false, phys = false;
        for (Item it : items) {
            boolean ok = it.when.ok();
            if (it.main) it.tile.setVisibility(ok ? View.VISIBLE : View.GONE);
            else if (ok && it.physical) phys = true;
            else if (ok) any = true;
        }
        more.setVisibility(any ? View.VISIBLE : View.GONE);
        physical.setVisibility(phys ? View.VISIBLE : View.GONE);
        int shown = 0;                                            // חמש משבצות — אות קטנה במעט, שהמילה תיכנס
        for (int i = 0; i < row.getChildCount(); i++) if (row.getChildAt(i).getVisibility() == View.VISIBLE) shown++;
        for (int i = 0; i < row.getChildCount(); i++) {
            View t = row.getChildAt(i);
            if (t instanceof LinearLayout && ((LinearLayout) t).getChildCount() > 1)
                ((TextView) ((LinearLayout) t).getChildAt(1)).setTextSize(shown >= 5 ? 10.5f : 12);
        }
    }
}
