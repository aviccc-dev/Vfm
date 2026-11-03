package com.tikiyot.vfm;

// מ0.10: פקודות הרשימות (פתח / צור / עדכן רשימה, רשימה חכמה; "רשימה חדשה"; הוספה מהכונן ומחיפוש) — כשעובדים במסד,
// פועלות על המסד; כשעובדים בישן — כמו תמיד (Verbs). "עובדים במסד" = מסך המסד המוכר פתוח (DbListScreen.alive).
// המשתמש: "ושאר הפקודות עובדות באותו מסד, וכאשר אני בישן — עובדות בישן."
// אחראי: לפתוח את המסד, להפעיל את DbPaths ולפתוח את המסך. אסור לו: לגעת במנוע הישן.

import android.app.Activity;
import android.content.Intent;

import java.util.List;

final class DbVerbs {

    private DbVerbs() { }

    /** האם הפקודות עכשיו של המסד. */
    static boolean active() {
        return DbListScreen.alive > 0 && DbListScreen.currentMode != null && !Home.inOld;
    }

    /** מבצע את הפקודה במסד; false — אינה פקודת רשימות (ממשיכה כרגיל: מסכים, תיקייה, חיפוש, הרצה). */
    static boolean run(final Activity a, Verbs.Cmd c) {
        final String mode = DbListScreen.currentMode;
        if (c.kind.equals(Verbs.K_LIST)) {
            List<String> path = Tree.parsePath(c.arg);
            long id = c.verb.equals(Verbs.CREATE) ? write(a, mode, new Job() {
                @Override public long run(Store s, List<String> p) { return DbPaths.ensure(s, mode, p); }
            }, path) : read(a, mode, path);
            if (id == 0) Ui.toast(a, Words.Verb.NOT_FOUND + c.arg);
            else DbListScreen.open(a, mode, id);
            return true;
        }
        if (c.kind.equals(Verbs.K_SMART)) {
            final String name = c.arg, cond = c.cond, sort = c.sort;
            long id;
            if (c.verb.equals(Verbs.CREATE)) id = write(a, mode, new Job() {
                @Override public long run(Store s, List<String> p) { return DbPaths.ensureSmart(s, name, cond, sort); }
            }, null);
            else if (c.verb.equals(Verbs.UPDATE)) id = write(a, mode, new Job() {
                @Override public long run(Store s, List<String> p) { return DbPaths.updateSmart(s, name, cond, sort); }
            }, null);
            else id = readSmart(a, mode, name);
            if (id == 0) Ui.toast(a, Words.Verb.NOT_FOUND + c.arg);
            else openSmart(a, mode, id, sort);
            return true;
        }
        if (c.kind.equals(Verbs.K_NEW_LIST)) {
            newList(a, mode, a instanceof DbListScreen ? ((DbListScreen) a).listId() : 0);
            return true;
        }
        if (c.kind.equals(Verbs.K_ADD) && (c.arg.equals(Verbs.A_DRIVE) || c.arg.equals(Verbs.A_SEARCH))) {
            if (!(a instanceof DbListScreen)) {
                Ui.toast(a, Words.Db.ADD_IN_LIST);
                return true;
            }
            addFiles(a, mode, ((DbListScreen) a).listId(), c.arg.equals(Verbs.A_DRIVE) ? DbScreen.DO_FOLDER : DbScreen.DO_SEARCH);
            return true;
        }
        return false;
    }

    // ---------- מ0.10.1: מה שה־➕ במראה המוכר עושה (וגם הפקודות) ----------

    /** רשימה ידנית חדשה בתוך in (0 — בתוך הבית, אם יש), ונפתחת. */
    static void newList(final Activity a, final String mode, final long in) {
        Ui.askText(a, Words.Db.NEW_LIST_TITLE, "", Words.Db.LIST_NAME, new Ui.Text1() {
            @Override
            public void ok(final String name) {
                if (name.trim().isEmpty()) return;
                long id = write(a, mode, new Job() {
                    @Override
                    public long run(Store s, List<String> p) {
                        final long into = in != 0 ? in : Importer.home(s);
                        final long[] out = {0};
                        s.atomic(new Store.Work() {
                            @Override public void run(Store t) { out[0] = t.createList(Tree.cleanName(name), into); }
                        }, true);
                        return out[0];
                    }
                }, null);
                if (id != 0) DbListScreen.open(a, mode, id);
            }
        });
    }

    /** רשימה חכמה חדשה — בטופס המוכר — בתוך in (0 — בתוך הבית, אם יש). */
    static void newSmart(final Activity a, final String mode, final long in) {
        DbSmartForm.show(a, null, 0, new DbSmartForm.Done() {
            @Override
            public void ok(final DbSmartForm.Result r) {
                write(a, mode, new Job() {
                    @Override
                    public long run(Store s, List<String> p) {
                        return DbSmartForm.save(s, 0, in != 0 ? in : Importer.home(s), r);
                    }
                }, null);
                if (a instanceof DbListScreen) ((DbListScreen) a).reload();
            }
        });
    }

    /** פקודה (מתפריט הפקודות האחד) — קובץ הפקודות נכנס לרשימה. */
    static void addCommand(final Activity a, final String mode, final long in) {
        CmdMenu.show(a, null, new CmdMenu.Picked() {
            @Override
            public void ok(String icon, String name, String line, java.io.File file) {
                final java.io.File f = file != null ? file : Shortcuts.write(icon, name, line);
                write(a, mode, new Job() {
                    @Override
                    public long run(Store s, List<String> p) {
                        DbFiles.add(s, in, java.util.Collections.singletonList(f.getAbsolutePath()), null);
                        return in;
                    }
                }, null);
                if (a instanceof DbListScreen) ((DbListScreen) a).reload();
            }
        }, null);
    }

    /** קבצים מתיקייה / מחיפוש — מסך המסד נפתח ברשימה, וישר חלון הבחירה. */
    static void addFiles(Activity a, String mode, long in, String what) {
        a.startActivity(new Intent(a, DbScreen.class).putExtra(DbScreen.EXTRA_MODE, mode)
                .putExtra(DbScreen.EXTRA_AT, in).putExtra(DbScreen.EXTRA_DO, what));
    }

    interface Job {
        long run(Store s, List<String> path);
    }

    /** פעולה שכותבת: המסד נפתח לכתיבה לרגע (עם קובץ המונה) ונסגר. 0 — נכשל (והודעה). */
    static long write(Activity a, String mode, Job job, List<String> path) {
        SqlStore s = null;
        try {
            s = SqlStore.open(DbScreen.dbFile(a, mode), new IdCounter(DbScreen.counterFile()));
            return job.run(s, path);
        } catch (Throwable t) {
            Log.e("DbVerbs.write", t);
            Ui.toast(a, Words.Db.failed(String.valueOf(t.getMessage())));
            return 0;
        } finally {
            if (s != null) s.close();
        }
    }

    private static long read(Activity a, String mode, List<String> path) {
        SqlStore s = null;
        try {
            s = SqlStore.openReadOnly(DbScreen.dbFile(a, mode));
            return DbPaths.find(s, mode, path);
        } catch (Throwable t) {
            Log.e("DbVerbs.read", t);
            return 0;
        } finally {
            if (s != null) s.close();
        }
    }

    private static long readSmart(Activity a, String mode, String name) {
        SqlStore s = null;
        try {
            s = SqlStore.openReadOnly(DbScreen.dbFile(a, mode));
            return DbPaths.smart(s, name);
        } catch (Throwable t) {
            Log.e("DbVerbs.smart", t);
            return 0;
        } finally {
            if (s != null) s.close();
        }
    }

    /** פותח רשימה חכמה של המסד במסך החיפוש המוכר; sort — של הפקודה (אם נכתב), אחרת של הרשימה. */
    private static void openSmart(Activity a, String mode, long id, String sort) {
        SqlStore s = null;
        try {
            s = SqlStore.openReadOnly(DbScreen.dbFile(a, mode));
            Store.Item it = s.item(id);
            String cond = s.prop(id, Store.P_COND), own = s.prop(id, Store.P_SORT), view = s.prop(id, Store.P_VIEW);
            DbListScreen.openSmart(a, it == null ? "" : it.name, cond, sort != null && !sort.isEmpty() ? sort : own, view);
        } catch (Throwable t) {
            Log.e("DbVerbs.openSmart", t);
        } finally {
            if (s != null) s.close();
        }
    }
}
