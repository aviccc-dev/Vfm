package com.tikiyot.vfm;

// מ0.23: בורר רשימה מהמסד — במקום ListPicker (עץ הרשימות הישן) בכל מקום שבוחרים רשימה כשעובדים במסד: "לרשימה" במסכי
// הקבצים, שיתוף מאפליקציה אחרת, טופס המקומות הפתוחים בפקודות, כפתור בשורה העליונה. אותו חלון בדיוק (PickDialog) —
// רק המקור אחר (צילום של המסד: מהבית ומטה, רשימות ידניות בלבד), ויצירת רשימה חדשה דרך הליבה (DbListCmd.newList).
// מבנה 18: הצילום ושאר הקריאות מהמסד — ב־DbLists (החומר). אחראי: החלון, וההוספה לרשימה שנבחרה. אסור לו: לשנות רשימות חוץ מיצירת רשימה חדשה.

import android.app.Activity;

import java.util.ArrayList;
import java.util.List;

final class DbPicker {

    interface Picked {
        /** list — מספר הרשימה במסד; path — המסלול בשמות (כמו בפקודות). */
        void ok(String mode, long list, List<String> path);
    }

    private DbPicker() { }

    /** מקור הבורר — מהצילום. */
    private static final class Source extends PickSource {
        final DbLists.Snap snap;

        Source(DbLists.Snap snap) {
            this.snap = snap;
        }

        @Override
        List<String> children(List<String> path) {
            List<String> k = snap.kids.get(DbLists.key(path));
            return k == null ? null : new ArrayList<String>(k);
        }

        @Override
        boolean large() {
            return false;
        }

        @Override
        String fullLabel(List<String> path) {
            return Names.fullLabel(path);
        }

        @Override
        String whyNot(List<String> path) {
            return path.isEmpty() ? Words.Db.PICK_NOT_HOME : null;
        }
    }

    // ================= החלון =================

    static void choose(final Activity a, final String title, final Picked cb) {
        final String mode = DbLists.mode(a);
        final DbLists.Snap snap = DbLists.snap(a, mode);
        if (snap == null) {
            Ui.toast(a, Words.Db.NO_DB_HERE);
            return;
        }
        PickerModel m = new PickerModel(new Source(snap), null);
        PickDialog.Creator creator = new PickDialog.Creator() {
            @Override
            public void create(final Activity act, final ArrayList<String> parent, final PickDialog.Chosen done) {
                Ui.askText(act, Words.NEW_LIST, "", Words.ListP.LIST_NAME, new Ui.Text1() {
                    @Override
                    public void ok(final String name) {
                        if (name.trim().isEmpty()) return;
                        final long in = parent.isEmpty() ? snap.home : snap.id(parent);
                        final String[] err = {null};
                        final long id = DbVerbsUi.write(act, mode, new DbVerbsUi.Job() {
                            @Override
                            public long run(Store s, List<String> p) {
                                DbListCmd.Made r = DbListCmd.newListChecked(s, in, name);
                                err[0] = r.error;
                                return r.id;
                            }
                        }, null);
                        if (err[0] != null) {
                            Ui.toast(act, err[0]);
                            return;
                        }
                        if (id == 0) return;
                        ArrayList<String> p = new ArrayList<String>(parent);
                        p.add(Names.cleanName(name));
                        snap.ids.put(DbLists.key(p), id);
                        done.ok(p);
                    }
                });
            }
        };
        PickDialog.Chosen chosen = new PickDialog.Chosen() {
            @Override
            public void ok(ArrayList<String> path) {
                long id = snap.id(path);
                if (id == 0) {
                    Ui.toast(a, Words.LIST_NOT_FOUND);
                    return;
                }
                cb.ok(mode, id, path);
            }
        };
        PickDialog.show(a, title, m, PickDialog.LISTS, creator, chosen);
    }

    // ================= הוספת קבצים (המשותף לכל "לרשימה") =================

    /** הקבצים לרשימה שנבחרה, בעסקה אחת, עם חתימה — DbFiles.add, אותה ליבה של ➕ ושל COPY. ההודעה — כמה נכנסו. */
    static void addFiles(final Activity a, final List<String> paths, final Runnable after) {
        choose(a, Words.ADD_TO_LIST + " · " + Words.NO_TOUCH, new Picked() {
            @Override
            public void ok(String mode, long list, List<String> path) {
                addPicked(a, mode, list, path, paths, after);
            }
        });
    }

    /** אחרי שנבחרה רשימה: הקבצים אליה (DbFiles.add, עם חתימה), והודעה כמה נכנסו. */
    static void addPicked(Activity a, String mode, final long list, List<String> path, final List<String> paths, Runnable after) {
        final DbFiles.Added[] r = {null};
        DbVerbsUi.write(a, mode, new DbVerbsUi.Job() {
            @Override
            public long run(Store s, List<String> p) {
                r[0] = DbFiles.add(s, list, paths, null, true);
                return list;
            }
        }, null);
        if (r[0] != null) Ui.toast(a, Words.Db.addedTo(r[0].added, r[0].already, Names.fullLabel(path)));
        if (after != null) after.run();
    }

}
