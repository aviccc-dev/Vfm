package com.tikiyot.vfm;

// הוספת תיקייה מהכונן לרשימה. פעולה אחת לכל המסכים: מנהל קבצים, חיפוש ופס צף.

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

    /** listPath == null: בחירת רשימה בבורר היעד. */
    public static void folder(final Activity a, final File folder, final List<String> listPath, final Done done) {
        many(a, java.util.Collections.singletonList(folder), new ArrayList<String>(), listPath, done);
    }

    /**
     * הוספה לרשימה של כמה תיקיות וקבצים בבת אחת (76). שאלה אחת לכל התיקיות: מתעדכנת (1.9.0, ברירת המחדל), צילום עם המבנה, או הקבצים בלבד.
     * listPath == null: בחירת רשימה בבורר היעד.
     */
    public static void many(final Activity a, final List<File> folders, final List<String> files,
                            final List<String> listPath, final Done done) {
        if (listPath == null && DbPicker.on(a)) {                             // מ0.23: עובדים במסד — הרשימות של המסד
            manyDb(a, folders, files, done);
            return;
        }
        Ui.toast(a, Words.DbCmd.NO_DB);                                       // מ0.29: רשימות — רק במסד
    }

    /**
     * מ0.23: אותו דבר במסד — הבורר של המסד, ואותן שלוש אפשרויות לתיקיות: חיה (רשימה חכמה של התיקייה), עם המבנה (רשימות
     * לתת־התיקיות), או הקבצים בלבד. הקבצים — DbFiles.add, החכמה — DbMore.liveFolder, המבנה — DbMore.attach. ברקע.
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
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        final int[] c = {0, 0, 0};             // רשימות, פריטים, חכמות
                                        final List<Tree.Node> scanned = new ArrayList<Tree.Node>();
                                        if (which != 0) for (File f : folders) scanned.add(Tree.scanFolder(f, which == 1));
                                        final DbListCmd.Res res = DbListCmd.nowRes(mode, new DbListCmd.Work() {
                                            @Override
                                            public DbListCmd.Res run(Store t, DbListCmd.Finder fd) {
                                                if (which == 0) {
                                                    for (File f : folders) if (DbMore.liveFolder(t, list, f.getAbsolutePath()) != 0) c[2]++;
                                                } else {
                                                    for (Tree.Node n : scanned) {
                                                        int[] r = DbMore.attach(t, list, n, which == 1, true);
                                                        c[0] += r[0];
                                                        c[1] += r[1];
                                                    }
                                                }
                                                if (!files.isEmpty()) c[1] += DbFiles.add(t, list, files, null, true).added;
                                                return DbListCmd.Res.ok("");
                                            }
                                        }, false);
                                        a.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (res.error != null) Ui.toast(a, res.error);
                                                else if (which == 0) Ui.toast(a, Words.AddW.ADDED_N + c[2] + Words.AddW.LIVE_ADDED
                                                        + (c[1] > 0 ? c[1] + Words.AddW.ITEMS_DOT : "") + Words.NO_TOUCH);
                                                else Ui.toast(a, (c[0] > 0 ? Words.AddW.ADDED_N + c[0] + Words.AddW.LISTS_AND : Words.AddW.ADDED_N)
                                                        + c[1] + Words.AddW.ITEMS_DOT + Words.NO_TOUCH);
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
