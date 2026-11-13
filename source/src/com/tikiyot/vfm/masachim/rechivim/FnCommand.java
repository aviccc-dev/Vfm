package com.tikiyot.vfm;

// 1.13.0: כלי "פקודה" — מריץ קובץ פקודות; הכלי מפנה לקובץ (קובץ = <מסלול>). עד מ0.29 — אריח במסך הבית ובסביבה (המשתמש:
// "במסך הבית בנוסף אפשר להוסיף פקודה שתעשה כל מה שפקודה יכולה לעשות"); היום — רק דרך "פתח כלי "פקודה" /קובץ:…" (כמו "הרץ"),
// והטופס שלו (בחירת קובץ פקודות קיים). קובצי הקיצורים — Shortcuts.

import android.app.Activity;
import android.content.DialogInterface;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class FnCommand extends Func {

    public static final String KIND = "פקודה", FILE = "קובץ";  // תחביר

    public FnCommand() {
        super(KIND, Words.CmdTool.TITLE, "▶");
    }

    static File file(Target t) {
        String p = t.get(FILE, "");
        return p.isEmpty() ? null : new File(p);
    }

    @Override
    public void open(Activity a, Target t) {
        File f = file(t);
        if (f != null && f.isFile()) BatchUi.run(a, f);
        else Ui.toast(a, Words.Verb.NOT_FOUND + (f == null ? "" : f.getName()));
    }

    /** הגדרה: בחירת קובץ פקודות קיים. */
    @Override
    public void configure(final Activity a, String name, Target t, final Done done) {
        final List<File> found = BatchUi.all(a);
        if (found.isEmpty()) {
            Ui.toast(a, Words.Top.NO_BATCH);
            return;
        }
        List<String> labels = new ArrayList<String>();
        for (File f : found) {
            Program p = Program.read(f);
            labels.add((p.isProgram() ? p.icon : "▶") + " " + p.title(f));
        }
        Ui.list(a, Words.CmdTool.WHICH, labels, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int w) {
                File f = found.get(w);
                done.ok(Program.read(f).title(f), Target.of(KIND, FILE, f.getAbsolutePath()));
            }
        });
    }
}
