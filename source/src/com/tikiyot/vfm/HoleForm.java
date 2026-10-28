package com.tikiyot.vfm;

// 1.25.6: חלון המילוי של המקומות הפתוחים (<רשימה-חדשה>, <סבבים>, <יעד>) — אחד לעורך ולהרצה.
// המשתמש: "כאשר אני מריץ ישר ומכניס רשימה ומספר — אין דוח": קובץ נפוצה שנפתח והורץ ישר רץ עם המקומות ריקים.
// עכשיו גם ההרצה שואלת אותם (הקובץ עצמו לא משתנה). הבוחר — לפי סוג המקום (1.25.2).
// אחראי: החלון בלבד; המילוי עצמו — CmdTemplates.

import android.app.Activity;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class HoleForm {

    interface Filled {
        void ok(List<String> lines);
    }

    private HoleForm() { }

    /** שואל את כל המקומות הפתוחים, ומחזיר את השורות הממולאות. ביטול — לא כלום. */
    static void ask(final Activity a, final String title, final List<String> lines, final Map<String, String> vars, final Filled cb) {
        final List<String> holes = CmdTemplates.holes(lines);
        if (holes.isEmpty()) {
            cb.ok(lines);
            return;
        }
        LinearLayout box = Ui.form(a);
        final List<EditText> fields = new ArrayList<EditText>();
        for (final String h : holes) {
            final int kind = CmdTemplates.holeKind(lines, h);
            box.addView(Ui.label(a, h + Words.Ed.holeKind(kind)));
            final EditText e = Ui.field(a, "", "");
            e.setTextDirection(kind == CmdTemplates.HOLE_PATH ? View.TEXT_DIRECTION_LTR : View.TEXT_DIRECTION_FIRST_STRONG);
            if (kind == CmdTemplates.HOLE_NUMBER) e.setInputType(InputType.TYPE_CLASS_NUMBER);
            Bidi.watch(e);
            box.addView(e);
            fields.add(e);
            if (kind == CmdTemplates.HOLE_NUMBER) continue;
            Button b = new Button(a);
            b.setText(Words.Cmd.PICK);
            b.setAllCaps(false);
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (kind == CmdTemplates.HOLE_MANUAL) {
                        ListPicker.choose(a, h, null, new ArrayList<String>(), new ListPicker.Picked() {   // כל הרשימות
                            @Override public void ok(ArrayList<String> path) { e.setText(ListCmd.pathText(path)); }
                        });
                    } else if (kind == CmdTemplates.HOLE_SMART) {
                        final List<String> names = ListCmd.smartNames();
                        if (names.isEmpty()) {
                            Ui.toast(a, Words.Ed.NO_SMART);
                            return;
                        }
                        Ui.options(a, h, names.toArray(new String[0]), new android.content.DialogInterface.OnClickListener() {
                            @Override public void onClick(android.content.DialogInterface d, int which) { e.setText(names.get(which)); }
                        });
                    } else {
                        FolderPicker.choose(a, h, true, new FolderPicker.Picked() {
                            @Override public void ok(File folder) { e.setText(BatchLine.toVar(folder.getAbsolutePath(), vars)); }
                        });
                    }
                }
            });
            box.addView(b);
        }
        Ui.askForm(a, Words.Ed.FILL + ": " + title, box, new Runnable() {
            @Override
            public void run() {
                Map<String, String> v = CmdTemplates.empty(holes);
                for (int i = 0; i < holes.size(); i++) v.put(holes.get(i), Bidi.strip(fields.get(i).getText().toString()).trim());
                for (String h : holes)
                    if (v.get(h).isEmpty()) {                                  // ריק — שואלים שוב, לא מריצים עם "<…>"
                        Ui.toast(a, Words.Ed.fillFirst(h));
                        ask(a, title, lines, vars, cb);
                        return;
                    }
                cb.ok(CmdTemplates.fill(lines, v));
            }
        });
    }
}
