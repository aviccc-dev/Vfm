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
        if (listPath == null) {
            ListPicker.choose(a, Words.ADD_TO_LIST + " · " + Words.NO_TOUCH, null, new ListPicker.Picked() {
                @Override
                public void ok(ArrayList<String> path) { many(a, folders, files, path, done); }
            });
            return;
        }
        if (folders.isEmpty()) {
            int n = Tree.addFiles(listPath, files);
            Ui.toast(a, n < 0 ? Words.LIST_NOT_FOUND : Words.AddW.ADDED_N + n + Words.AddW.ITEMS_DOT + Words.NO_TOUCH);
            if (done != null) done.done();
            return;
        }
        String what = folders.size() == 1 ? Words.AddW.THE_FOLDER + folders.get(0).getName() + "\"" : folders.size() + Words.AddW.FOLDERS_SUFFIX;
        if (!files.isEmpty()) what += Words.AddW.AND + Words.files(files.size());
        Ui.options(a, Words.AddW.ADD_N + what + Words.AddW.TO_LIST + Tree.label(listPath) + "\"",
                new String[]{folders.size() == 1 ? Words.AddW.LIVE : Words.AddW.EACH_LIVE,      // 1.9.0: ברירת המחדל
                        folders.size() == 1 ? Words.AddW.AS_SUBLIST : Words.AddW.EACH_AS_SUBLIST,
                        Words.AddW.FILES_ONLY},
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, final int which) {
                        if (which == 0) {                                      // 1.9.0: תיקייה חיה — מיד, בלי סריקה
                            List<String> paths = new ArrayList<String>();
                            for (File f : folders) paths.add(f.getAbsolutePath());
                            int lives = Unify.addFolders(listPath, paths);    // 1.17.3: רשימה חכמה של התיקייה
                            int items = lives < 0 || files.isEmpty() ? 0 : Math.max(0, Tree.addFiles(listPath, files));
                            Ui.toast(a, lives < 0 ? Words.LIST_NOT_FOUND
                                    : Words.AddW.ADDED_N + lives + Words.AddW.LIVE_ADDED
                                    + (items > 0 ? items + Words.AddW.ITEMS_DOT : "") + Words.NO_TOUCH);
                            if (done != null) done.done();
                            return;
                        }
                        final boolean structure = which == 1;
                        Ui.toast(a, Words.AddW.ADDING);
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                final List<Tree.Node> scanned = new ArrayList<Tree.Node>();
                                for (File f : folders) scanned.add(Tree.scanFolder(f, structure));   // ברקע
                                a.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        int lists = 0, items = 0;
                                        boolean missing = false;
                                        for (Tree.Node n : scanned) {
                                            int[] c = Tree.attach(listPath, n, structure);   // במסך, בנעילה
                                            if (c == null) { missing = true; break; }
                                            lists += c[0];
                                            items += c[1];
                                        }
                                        if (!missing && !files.isEmpty()) items += Math.max(0, Tree.addFiles(listPath, files));
                                        if (missing) Ui.toast(a, Words.LIST_NOT_FOUND);
                                        else Ui.toast(a, (lists > 0 ? Words.AddW.ADDED_N + lists + Words.AddW.LISTS_AND : Words.AddW.ADDED_N)
                                                + items + Words.AddW.ITEMS_DOT + Words.NO_TOUCH);
                                        if (done != null) done.done();
                                    }
                                });
                            }
                        }).start();
                    }
                });
    }
}
