package com.tikiyot.vfm;

// בורר רשימה: בחירת רשימה להעברה, להוספה או לכלי, עם אפשרות ליצור רשימה חדשה.
// הבורר עצמו — PickDialog, אחד לרשימות ולתיקיות (106). כאן רק המקור (עץ הרשימות) ויצירת רשימה.
// אסור לו: לשנות רשימות חוץ מיצירת רשימה חדשה.

import android.app.Activity;

import java.util.ArrayList;
import java.util.List;

public final class ListPicker {

    public interface Picked {
        void ok(ArrayList<String> path);
    }

    private ListPicker() { }

    /** except: רשימה שלא תופיע כיעד (למשל הרשימה הנוכחית), או null. */
    public static void choose(Activity a, String title, List<String> except, Picked cb) {
        choose(a, title, except, null, cb);
    }

    /** base: הרשימה של המופע. אם null — הרשימות של כל הכלים. */
    public static void choose(final Activity a, final String title, final List<String> except,
                              final List<String> base, final Picked cb) {
        PickerModel m = new PickerModel(new TreeSource(Tree.load()), base == null ? FnVirtual.allBases() : null, except, base);
        PickDialog.Creator creator = new PickDialog.Creator() {
            @Override
            public void create(Activity act, ArrayList<String> parent, PickDialog.Chosen done) {
                ListPicker.create(act, parent, done);
            }
        };
        PickDialog.Chosen chosen = new PickDialog.Chosen() {
            @Override public void ok(ArrayList<String> path) { cb.ok(path); }
        };
        Runnable cross = new Runnable() {
            @Override public void run() { choose(a, title, except, null, cb); }
        };
        PickDialog.show(a, title, m, PickDialog.LISTS, creator, chosen, cross);
    }

    /** רשימה חדשה בתוך parent: שם, יצירה, ו־done עם המסלול שלה (גם למסך השיתוף). */
    static void create(final Activity a, final ArrayList<String> parent, final PickDialog.Chosen done) {
        Ui.askText(a, Words.NEW_LIST, "", Words.ListP.LIST_NAME, new Ui.Text1() {
            @Override
            public void ok(String name) {
                Tree.Node c;
                synchronized (Tree.LOCK) {                           // 92
                    Tree t = Tree.load();
                    Tree.Node p = t.find(parent);
                    if (p == null) p = t.root;
                    c = Tree.newChild(p, p.uniqueChildName(name, null));
                    t.save();
                }
                done.ok(c.path());
            }
        });
    }
}
