package com.tikiyot.vfm;

// בחירת תיקייה בכונן, בדפדוף. בלי לכתוב מסלולים.
// אותו בורר כמו לרשימות (PickDialog, 106) — אותן תצוגות, חיפוש ותצוגה אחרונה — והתיקיות מהאינדקס (IndexSource):
// כל דבר במקום אחד, ומיידי כמו מנהל הקבצים והחיפוש.

import android.app.Activity;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public final class FolderPicker {

    public interface Picked {
        void ok(File folder);
    }

    private FolderPicker() { }

    /** בחירת תיקייה קיימת (בלי יצירה): למקורות של חיפוש, כפולים ורשימה חכמה. */
    public static void choose(Activity a, Picked cb) {
        choose(a, Words.FolderP.PICK_FOLDER, false, cb);
    }

    /** canCreate: אפשר גם ליצור תיקייה חדשה (יעד של העברה והעתקה). */
    public static void choose(final Activity a, String title, final boolean canCreate, final Picked cb) {
        choose(a, title, canCreate, cb, null, null);
    }

    /** 204: עם יעד נוסף ליד הכוננים — "📤 שיתוף" ביעד של COPY בקובץ פקודות. */
    public static void choose(final Activity a, String title, final boolean canCreate, final Picked cb,
                              String extraLabel, Runnable extra) {
        Library lib = Library.get(a);
        lib.start(null);
        List<IndexSource.Drv> drives = new ArrayList<IndexSource.Drv>();
        LinkedHashMap<ArrayList<String>, String> roots = new LinkedHashMap<ArrayList<String>, String>();
        for (Vols.Vol v : Vols.list(a)) {
            Index found = null;
            for (Index ix : lib.indexes) if (ix.key.equals(v.key)) found = ix;
            String label = Drive.label(v.internal, v.label);
            drives.add(new IndexSource.Drv(v.key, label, v.root.getAbsolutePath(), found));
            ArrayList<String> p = new ArrayList<String>();
            p.add(v.key);
            roots.put(p, label);
        }
        final IndexSource src = new IndexSource(drives);
        PickerModel m = new PickerModel(src, roots, null, null);
        m.direct = !canCreate;                               // בלי יצירה: נגיעה אחת בוחרת
        if (m.roots.size() == 1) m.enter(m.roots.get(0));    // כונן אחד: נכנסים אליו ישר
        PickDialog.Creator creator = !canCreate ? null : new PickDialog.Creator() {
            @Override
            public void create(final Activity act, final ArrayList<String> parent, final PickDialog.Chosen done) {
                Ui.askText(act, Words.NEW_FOLDER + Words.FolderP.ON_DRIVE_SUFFIX, "", Words.FolderP.FOLDER_NAME, new Ui.Text1() {
                    @Override
                    public void ok(String value) {
                        final String name = value.trim();
                        // יצירה בכונן היא פעולה פיזית: דרך Physical, עם חלון האישור והמילה "בכונן"
                        Physical.askMakeFolder(act, src.fileOf(parent), name, new Runnable() {
                            @Override
                            public void run() {
                                ArrayList<String> path = new ArrayList<String>(parent);
                                path.add(name);
                                done.ok(path);
                            }
                        });
                    }
                });
            }
        };
        PickDialog.Chosen chosen = new PickDialog.Chosen() {
            @Override public void ok(ArrayList<String> path) { cb.ok(src.fileOf(path)); }
        };
        if (extra != null) PickDialog.show(a, title, m, PickDialog.FOLDERS, creator, chosen, extraLabel, extra);
        else PickDialog.show(a, title, m, PickDialog.FOLDERS, creator, chosen, null);
    }
}
