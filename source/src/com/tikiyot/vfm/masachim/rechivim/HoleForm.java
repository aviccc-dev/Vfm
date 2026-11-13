package com.tikiyot.vfm;

// 1.25.6: המילוי של המקומות הפתוחים (<רשימה-חדשה>, <סבבים>, <יעד>) — אחד לעורך ולהרצה. הבוחר — לפי סוג המקום (1.25.2).
// 1.26.0: גם בתוך מסך ההרצה (fields), ובחירה מרובה של רשימות — המשתמש: "אני רוצה שתביא לי כמה רשימות ולבחור איזה
// מהם; הוא נותן לי לבחור אחת וזהו". מקום ששמו מתחיל ב"רשימות" אחרי "ידנית" — כמה רשימות, שורה לכל אחת; השורה
// בקובץ מוכפלת לכל רשימה שנבחרה (CmdTemplates.fill).
// אחראי: השדות והחלון בלבד; המילוי עצמו — CmdTemplates.

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
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

    /** השדות של המקומות הפתוחים — לחלון או למסך ההרצה. */
    static final class Fields {
        final LinearLayout view;
        final List<String> holes;
        final List<EditText> edits = new ArrayList<EditText>();

        Fields(LinearLayout view, List<String> holes) {
            this.view = view;
            this.holes = holes;
        }

        /** הערכים; null — יש מקום ריק (ונאמר איזה). */
        Map<String, String> values(Activity a) {
            Map<String, String> v = CmdTemplates.empty(holes);
            for (int i = 0; i < holes.size(); i++) {
                String x = Bidi.strip(edits.get(i).getText().toString()).trim();
                if (x.isEmpty()) {
                    Ui.toast(a, Words.Ed.fillFirst(holes.get(i)));
                    return null;
                }
                v.put(holes.get(i), x);
            }
            return v;
        }
    }

    private HoleForm() { }

    static Fields fields(final Activity a, final List<String> lines, final Map<String, String> vars) {
        final List<String> holes = CmdTemplates.holes(lines);
        LinearLayout box = Ui.form(a);
        Fields out = new Fields(box, holes);
        for (final String h : holes) {
            final int kind = CmdTemplates.holeKind(lines, h);
            box.addView(Ui.label(a, h + Words.Ed.holeKind(kind)));
            final EditText e = Ui.field(a, "", "");
            e.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);   // מ0.38
            if (kind == CmdTemplates.HOLE_NUMBER) e.setInputType(InputType.TYPE_CLASS_NUMBER);
            if (kind == CmdTemplates.HOLE_MANY) {
                e.setSingleLine(false);
                e.setMinLines(2);
            }
            Bidi.watch(e);
            box.addView(e);
            out.edits.add(e);
            if (kind == CmdTemplates.HOLE_NUMBER) continue;
            Button b = new Button(a);
            b.setText(Words.Cmd.PICK);
            b.setAllCaps(false);
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (kind == CmdTemplates.HOLE_MANY) {
                        pickMany(a, h, e);
                    } else if (kind == CmdTemplates.HOLE_MANUAL && DbPicker.on(a)) {   // מ0.23: עובדים במסד — הרשימות של המסד
                        DbPicker.choose(a, h, new DbPicker.Picked() {
                            @Override public void ok(String mode, long id, List<String> path) { e.setText(ListCmd.pathText(path)); }
                        });
                    } else if (kind == CmdTemplates.HOLE_MANUAL) {
                        Ui.toast(a, Words.DbCmd.NO_DB);                         // מ0.26: רשימות — רק במסד
                    } else if (kind == CmdTemplates.HOLE_SMART) {
                        final List<String> names = DbPicker.on(a) ? DbPicker.names(a).get("smart") : new ArrayList<String>();   // מ0.23
                        if (names.isEmpty()) {
                            Ui.toast(a, Words.Ed.NO_SMART);
                            return;
                        }
                        Ui.options(a, h, names.toArray(new String[0]), new DialogInterface.OnClickListener() {
                            @Override public void onClick(DialogInterface d, int which) { e.setText(names.get(which)); }
                        });
                    } else if (kind == CmdTemplates.HOLE_FILE) {               // מ0.31: הקבצים שבתיקיית היצוא, החדש קודם
                        File[] k = DbListCmd.exportDir().listFiles();
                        final List<File> fs = new ArrayList<File>();
                        if (k != null) for (File x : k) if (x.isFile()) fs.add(x);
                        java.util.Collections.sort(fs, new java.util.Comparator<File>() {
                            @Override public int compare(File x, File y) { return Long.compare(y.lastModified(), x.lastModified()); }
                        });
                        if (fs.isEmpty()) {
                            Ui.toast(a, Words.DbCmd.NO_EXPORT_FILES);
                            return;
                        }
                        String[] names = new String[fs.size()];
                        for (int i = 0; i < names.length; i++) names[i] = fs.get(i).getName();
                        Ui.options(a, h, names, new DialogInterface.OnClickListener() {
                            @Override public void onClick(DialogInterface d, int which) { e.setText(fs.get(which).getName()); }
                        });
                    } else {
                        FolderPicker.choose(a, h, true, new FolderPicker.Picked() {
                            @Override public void ok(File folder) { e.setText(Batch.toVar(folder.getAbsolutePath(), vars)); }
                        });
                    }
                }
            });
            box.addView(b);
        }
        return out;
    }

    /** 1.26.0: כמה רשימות — כל הרשימות הידניות (בכל העומק), ובראש "כל הבית"; מה שכבר בשדה — מסומן. */
    private static void pickMany(Activity a, String title, final EditText e) {
        final List<String> all = new ArrayList<String>();
        all.add(ListCmd.HOME);
        if (DbPicker.on(a)) all.addAll(DbPicker.names(a).get("manual"));     // מ0.23: עובדים במסד
        String[] shown = new String[all.size()];
        final boolean[] on = new boolean[all.size()];
        List<String> now = CmdTemplates.many(Bidi.strip(e.getText().toString()));
        for (int i = 0; i < all.size(); i++) {
            String p = all.get(i);
            shown[i] = p.equals(ListCmd.HOME) ? Words.Ed.WHOLE_HOME : indent(p) + leaf(p);
            on[i] = now.contains(p);
        }
        new AlertDialog.Builder(a).setTitle(title)
                .setMultiChoiceItems(shown, on, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override public void onClick(DialogInterface d, int which, boolean checked) { on[which] = checked; }
                })
                .setPositiveButton(Words.OK, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < all.size(); i++) if (on[i]) sb.append(sb.length() > 0 ? "\n" : "").append(all.get(i));
                        e.setText(sb.toString());
                    }
                })
                .setNegativeButton(Words.CANCEL, null).show();
    }

    private static String indent(String path) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < path.length(); i++) if (path.charAt(i) == '\\') sb.append("    ");
        return sb.toString();
    }

    private static String leaf(String path) {
        int i = path.lastIndexOf('\\');
        return i < 0 ? path : path.substring(i + 1);
    }

    /** שואל את כל המקומות הפתוחים בחלון, ומחזיר את השורות הממולאות. ביטול — לא כלום. */
    static void ask(final Activity a, final String title, final List<String> lines, final Map<String, String> vars, final Filled cb) {
        if (CmdTemplates.holes(lines).isEmpty()) {
            cb.ok(lines);
            return;
        }
        final Fields f = fields(a, lines, vars);
        Ui.askForm(a, Words.Ed.FILL + ": " + title, f.view, new Runnable() {
            @Override
            public void run() {
                Map<String, String> v = f.values(a);
                if (v == null) {                                               // ריק — שואלים שוב, לא מריצים עם "<…>"
                    ask(a, title, lines, vars, cb);
                    return;
                }
                cb.ok(CmdTemplates.fill(lines, v));
            }
        });
    }
}
