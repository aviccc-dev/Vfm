package com.tikiyot.vfm;

// רשימת הסוגים שבאינדקס (145): הנפוץ קודם, עם המספר; חיפוש קטן; תיבות סימון לכמה סוגים.
// המודל — Library.typeCounts ו-Types.Choice; כאן רק החלון.

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class TypePicker {

    interface Picked {
        void ok(Set<String> chosen);
    }

    private TypePicker() { }

    static void choose(final Activity a, Set<String> current, final Picked done) {
        Library lib = Library.get(a);
        final List<String> exts = new ArrayList<String>();
        final List<String> labels = new ArrayList<String>();
        for (Map.Entry<String, Integer> e : lib.typeCounts()) {
            exts.add(e.getKey());
            labels.add(Words.Kinds.counted(e.getKey(), e.getValue()));
        }
        for (String m : current)                                   // נבחר קודם ואין באינדקס — שאפשר יהיה לבטל
            if (!exts.contains(m)) {
                exts.add(0, m);
                labels.add(0, m + " (" + Words.Kinds.NOT_IN_INDEX + ")");
            }
        if (exts.isEmpty() && (!lib.ready || lib.busy)) Ui.toast(a, Words.LOADING_INDEX);
        final Set<String> sel = new LinkedHashSet<String>(current);

        LinearLayout box = Ui.form(a);
        final EditText q = Ui.field(a, "", Words.Kinds.SEARCH);
        q.setTextDirection(View.TEXT_DIRECTION_LTR);
        box.addView(q);
        final ListView lv = new ListView(a);
        lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        box.addView(lv, new LinearLayout.LayoutParams(-1, Ui.dp(a, 360)));
        final List<String> shown = new ArrayList<String>();
        final Runnable fill = new Runnable() {
            @Override
            public void run() {
                String f = q.getText().toString().trim().toLowerCase(java.util.Locale.ROOT);
                shown.clear();
                List<String> lab = new ArrayList<String>();
                for (int i = 0; i < exts.size(); i++)
                    if (f.isEmpty() || exts.get(i).contains(f)) {
                        shown.add(exts.get(i));
                        lab.add(labels.get(i));
                    }
                lv.setAdapter(new ArrayAdapter<String>(a, android.R.layout.simple_list_item_multiple_choice, lab));
                for (int i = 0; i < shown.size(); i++) lv.setItemChecked(i, sel.contains(shown.get(i)));
            }
        };
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> p, View v, int pos, long id) {
                String e = shown.get(pos);
                if (lv.isItemChecked(pos)) sel.add(e); else sel.remove(e);
            }
        });
        q.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int j, int k) { }
            @Override public void onTextChanged(CharSequence s, int i, int j, int k) { }
            @Override public void afterTextChanged(Editable s) { fill.run(); }
        });
        fill.run();
        new AlertDialog.Builder(a)
                .setTitle(Words.Kinds.MORE)
                .setView(box)
                .setPositiveButton(Words.OK, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        done.ok(sel);
                    }
                })
                .setNegativeButton(Words.CANCEL, null)
                .show();
    }
}
