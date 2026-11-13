package com.tikiyot.vfm;

// שדות הסינון לפי תאריך וגודל (128) — רכיב אחד לטופס הרשימה החכמה ולטופס של קובצי הפקודות.
// המודל — DateSize. כל שינוי מעדכן את המודל וקורא ל-changed.

import android.app.Activity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

final class FilterForm {

    private FilterForm() { }

    static LinearLayout build(final Activity a, final DateSize ds, final Runnable changed) {
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);

        // 197: בשורה — "תאריך [בלי ▾]"; הפרטים (של מה, כמה, יחידות) — רק כשנבחר משהו
        final Spinner dmode = Ui.choice(a, DateSize.DATE_MODES, ds.dateMode);
        box.addView(SmartForm.line(a, Words.Filter.DATE_LABEL_ANY, dmode, false));
        final LinearLayout dmore = new LinearLayout(a);
        dmore.setOrientation(LinearLayout.VERTICAL);
        dmore.setPaddingRelative(SmartForm.labelW(a), 0, 0, 0);
        final Spinner dby = ds.allowAdded ? Ui.choice(a, Words.Filter.DATE_BY, ds.added ? 1 : 0) : null;   // 189
        if (dby != null) dmore.addView(dby);
        else {                                                                // בלי "של ההוספה" — במילים, של מה
            TextView of = Ui.info(a);
            of.setPadding(0, Ui.dp(a, 4), 0, 0);
            of.setText(Words.Filter.DATE_BY[0]);
            dmore.addView(of);
        }
        final EditText dval = Ui.field(a, ds.dateValue, Words.Filter.DATE_HINT);
        dmore.addView(dval);
        Flow units = new Flow(a, 6, 0);
        for (final String u : DateSize.DATE_UNITS) {
            Button bt = new Button(a);
            bt.setText(u);
            bt.setAllCaps(false);
            bt.setTextSize(13);
            bt.setMinWidth(0);
            bt.setMinimumWidth(0);
            bt.setPadding(Ui.dp(a, 12), 0, Ui.dp(a, 12), 0);
            bt.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dval.setText(u);
                    if (dmode.getSelectedItemPosition() == 0) dmode.setSelection(1);
                }
            });
            units.addView(bt);
        }
        dmore.addView(units);
        box.addView(dmore);

        final Spinner smode = Ui.choice(a, DateSize.SIZE_MODES, ds.sizeMode);
        box.addView(SmartForm.line(a, Words.Filter.SIZE_LABEL, smode, false));
        final LinearLayout sz = new LinearLayout(a);
        sz.setOrientation(LinearLayout.HORIZONTAL);
        sz.setGravity(android.view.Gravity.CENTER_VERTICAL);
        sz.setPaddingRelative(SmartForm.labelW(a), 0, 0, 0);
        final EditText snum = Ui.field(a, ds.sizeNumber, "10");
        snum.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        sz.addView(snum, new LinearLayout.LayoutParams(0, -2, 1f));
        int ui = java.util.Arrays.asList(DateSize.SIZE_UNITS).indexOf(ds.sizeUnit);
        final Spinner sunit = Ui.choice(a, DateSize.SIZE_UNITS, ui < 0 ? 2 : ui);
        sz.addView(sunit, new LinearLayout.LayoutParams(-2, -2));
        box.addView(sz);
        final Runnable shown = new Runnable() {                              // 197: "בלי" — בלי הפרטים
            @Override
            public void run() {
                dmore.setVisibility(dmode.getSelectedItemPosition() == 0 && dval.getText().length() == 0 ? View.GONE : View.VISIBLE);
                sz.setVisibility(smode.getSelectedItemPosition() == 0 ? View.GONE : View.VISIBLE);
            }
        };
        shown.run();

        final Runnable read = new Runnable() {
            @Override
            public void run() {
                ds.dateMode = dmode.getSelectedItemPosition();
                if (dby != null) ds.added = dby.getSelectedItemPosition() == 1;
                ds.dateValue = dval.getText().toString().trim();
                ds.sizeMode = smode.getSelectedItemPosition();
                ds.sizeNumber = snum.getText().toString().trim();
                ds.sizeUnit = DateSize.SIZE_UNITS[sunit.getSelectedItemPosition()];
                shown.run();
                if (changed != null) changed.run();
            }
        };
        AdapterView.OnItemSelectedListener sel = new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { read.run(); }
            @Override public void onNothingSelected(AdapterView<?> p) { }
        };
        dmode.setOnItemSelectedListener(sel);
        if (dby != null) dby.setOnItemSelectedListener(sel);
        smode.setOnItemSelectedListener(sel);
        sunit.setOnItemSelectedListener(sel);
        TextWatcher w = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int j, int k) { }
            @Override public void onTextChanged(CharSequence s, int i, int j, int k) { }
            @Override public void afterTextChanged(Editable s) { read.run(); }
        };
        dval.addTextChangedListener(w);
        snum.addTextChangedListener(w);
        return box;
    }
}
