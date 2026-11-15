package com.tikiyot.vfm;

// הוספת תיקייה מהכונן לרשימה. פעולה אחת לכל המסכים: מנהל הקבצים והחיפוש.

import android.app.Activity;
import android.content.DialogInterface;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class Adder {

    public interface Done {
        void done();
    }

    private Adder() { }

    /**
     * הוספה לרשימה של כמה תיקיות וקבצים בבת אחת (76). שאלה אחת לכל התיקיות: מתעדכנת (1.9.0, ברירת המחדל), צילום עם המבנה, או הקבצים בלבד.
     * הרשימה — נבחרת בבורר היעד.
     */
    public static void many(final Activity a, final List<File> folders, final List<String> files, final Done done) {
        if (DbLists.on(a)) {                                                 // מ0.23: עובדים במסד — הרשימות של המסד
            manyDb(a, folders, files, done);
            return;
        }
        Ui.toast(a, Words.DbCmd.NO_DB);                                       // מ0.29: רשימות — רק במסד
    }

    /**
     * מ0.23: אותו דבר במסד — הבורר של המסד, ואותן שלוש אפשרויות לתיקיות: חיה (רשימה חכמה של התיקייה), עם המבנה (רשימות
     * לתת־התיקיות), או הקבצים בלבד. מבנה 115: הכול דרך המנהל, כפקודה (Manager.onListAdd) — המסך אינו כותב למסד. ברקע.
     */
    private static void manyDb(final Activity a, final List<File> folders, final List<String> files, final Done done) {
        DbPicker.choose(a, Words.ADD_TO_LIST + " · " + Words.NO_TOUCH, new DbPicker.Picked() {
            @Override
            public void ok(final String mode, final long list, final List<String> path) {
                if (folders.isEmpty()) {
                    DbPicker.addPicked(a, mode, list, path, files, done == null ? null : new Runnable() {
                        @Override public void run() { done.done(); }
                    });
                    return;
                }
                String what = folders.size() == 1 ? Words.AddW.THE_FOLDER + folders.get(0).getName() + "\"" : folders.size() + Words.AddW.FOLDERS_SUFFIX;
                if (!files.isEmpty()) what += Words.AddW.AND + Words.files(files.size());
                Ui.options(a, Words.AddW.ADD_N + what + Words.AddW.TO_LIST + Names.label(new ArrayList<String>(path)) + "\"",
                        new String[]{folders.size() == 1 ? Words.AddW.LIVE : Words.AddW.EACH_LIVE,
                                folders.size() == 1 ? Words.AddW.AS_SUBLIST : Words.AddW.EACH_AS_SUBLIST,
                                Words.AddW.FILES_ONLY},
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, final int which) {
                                Ui.toast(a, Words.AddW.ADDING);
                                // מבנה 115: דרך המנהל, כפקודה ("העתק %נבחרים% ידנית %נבחרים%" עם /חיה, /קבצים) — אותו כלל כמו בשורה כתובה. ברקע.
                                final List<String> dirs = new ArrayList<String>();
                                for (File f : folders) dirs.add(f.getAbsolutePath());
                                final Library lib = Library.get(a);
                                final Manager.Act act = Manager.onListAdd(mode, list, files, dirs,
                                        which == 0 ? Manager.FOLDER_LIVE : which == 1 ? Manager.FOLDER_TREE : Manager.FOLDER_FLAT,
                                        Paths.data(), BatchUi.vars(a, Paths.data()), lib);
                                if (act.error() != null) {
                                    Ui.toast(a, act.error());
                                    return;
                                }
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        final String said = act.say(lib);
                                        a.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                Ui.toast(a, said + (act.failed ? "" : Words.NO_TOUCH));
                                                if (done != null) done.done();
                                            }
                                        });
                                    }
                                }).start();
                            }
                        });
            }
        });
    }
}
