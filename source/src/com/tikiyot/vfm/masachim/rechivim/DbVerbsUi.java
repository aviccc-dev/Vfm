package com.tikiyot.vfm;

// הביצוע של פקודות הרשימות במסד (מבנה 13, 13.11.2026): פתח / צור / עדכן רשימה ורשימה חכמה, "רשימה חדשה", רעיון וכותרת,
// אופן חדש, הוספה מהכונן ומחיפוש, ופתיחות הסוף של קובץ פקודות. עד כאן ישב ב־DbVerbs (קומת הפקודות) — עם חלונות, הודעות
// ופתיחת מסכים. אותן שורות. ב־DbVerbs נשארה רק השאלה active().
// אחראי: לפתוח את המסד לרגע, להפעיל את DbPaths ולפתוח את המסך. אסור לו: להחזיק מסד פתוח.

import android.app.Activity;
import android.content.Intent;
import java.util.List;

final class DbVerbsUi {

    private DbVerbsUi() { }

    /** מבצע את הפקודה במסד; false — אינה פקודת רשימות (ממשיכה כרגיל: מסכים, תיקייה, חיפוש, הרצה). */
    static boolean run(final Activity a, Verbs.Cmd c) {
        final String mode = Db.currentMode;
        if (c.kind.equals(Verbs.K_LIST)) {
            List<String> path = Names.parsePath(c.arg);
            long id = c.verb.equals(Verbs.CREATE) ? write(a, mode, new Job() {
                @Override public long run(Store s, List<String> p) { return DbPaths.ensure(s, mode, p); }
            }, path) : read(a, mode, path);
            if (id == 0) Ui.toast(a, Words.Verb.NOT_FOUND + c.arg);
            else if (c.noOpen) Ui.toast(a, Words.ListCmdW.created(c.arg));     // מ0.40: /בלי-לפתוח
            else DbListScreen.open(a, mode, id, c.sort);                    // מ0.22: עם המיון שבפקודה
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
            else if (c.noOpen) Ui.toast(a, Words.ListCmdW.created(c.arg));     // מ0.40: /בלי-לפתוח
            else openSmart(a, mode, id, sort);
            return true;
        }
        if (c.kind.equals(Verbs.K_MODE)) {                                       // מ0.42: מכפתור — נוצר (אם אין) ונפתח, כמו "אופן חדש"
            createMode(a, c.arg.trim(), new ModeReady() {                       // מבנה 48: יש גיבוי בשם הזה — שואלים
                @Override
                public void open(String name, long home) {
                    Db.saveMode(a, name);
                    DbListScreen.open(a, name, home);
                }
            });
            return true;
        }
        if (c.kind.equals(Verbs.K_NOTE) || c.kind.equals(Verbs.K_TITLE)) {      // מ0.37: מכפתור — בלי רשימה = הרשימה שעל המסך
            final boolean title = c.kind.equals(Verbs.K_TITLE);
            final String text = c.arg;
            final List<String> path = Names.parsePath(c.cond);
            final long here = a instanceof DbListScreen ? ((DbListScreen) a).listId() : 0;
            if (path.isEmpty() && here == 0) {
                Ui.toast(a, Words.DbCmd.NOTE_NEEDS_LIST);
                return true;
            }
            long id = write(a, mode, new Job() {
                @Override public long run(Store s, List<String> p) {
                    long in = path.isEmpty() ? here : DbPaths.find(s, mode, path);
                    if (in == 0) return 0;
                    return title ? s.addTitle(in, text) : s.addNote(in, text);
                }
            }, null);
            if (id == 0) Ui.toast(a, Words.Verb.NOT_FOUND + c.cond);
            else if (a instanceof DbListScreen) ((DbListScreen) a).reload();
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
                final String[] err = {null};
                long id = write(a, mode, new Job() {
                    @Override
                    public long run(Store s, List<String> p) {                // מ0.23: עם בדיקת השם (כמו MD) — לא כפולה
                        Lists.Made r = Lists.newListChecked(s, in, name);
                        err[0] = r.error;
                        return r.id;
                    }
                }, null);
                if (err[0] != null) Ui.toast(a, err[0]);
                else if (id != 0) DbListScreen.open(a, mode, id);
            }
        });
    }

    /** רשימה חכמה חדשה — בטופס המוכר — בתוך in (0 — בתוך הבית, אם יש). */
    static void newSmart(final Activity a, final String mode, final long in) {
        DbSmartForm.showNew(a, new DbSmartForm.Done() {
            @Override
            public void ok(final DbSmartForm.Result r) {
                write(a, mode, new Job() {
                    @Override
                    public long run(Store s, List<String> p) {
                        return DbSmartForm.save(s, 0, in != 0 ? in : DbPaths.home(s), r);
                    }
                }, null);
                if (a instanceof DbListScreen) ((DbListScreen) a).reload();
            }
        }, new Func.Done() {
            @Override public void ok(String name, Target t) { saveDups(a, mode, in, name, t); }   // מ0.28: נבחר "כפולים" בפס הסוג
        });
    }

    /**
     * מ0.25 (באג 27): כפולים — אותו טופס כמו בבית הישן (FnDup); נשמר כקובץ פקודות קטן ("פתח כלי "כפולים" …") שנכנס לרשימה,
     * כמו שהייבוא מהישן עושה לכלי כפולים קיים (מ0.19). נגיעה — פותחת את מסך הכפולים עם ההגדרות האלה.
     */
    static void saveDups(final Activity a, final String mode, final long in, String name, final Target t) {
        final Func f = Func.byKind(Target.KIND_DUP);
        if (f == null) return;
        final java.io.File file = Shortcuts.write(f.icon, name == null || name.isEmpty() ? f.title : name,
                Verbs.toolLine(t.kind, t.params));
        if (file == null) {
            Ui.toast(a, Words.SAVE_FAILED);
            return;
        }
        write(a, mode, new Job() {
            @Override
            public long run(Store s, List<String> p) {
                DbFiles.add(s, in != 0 ? in : DbPaths.home(s), java.util.Collections.singletonList(file.getAbsolutePath()), null, true);
                return in;
            }
        }, null);
        if (a instanceof DbListScreen) ((DbListScreen) a).reload();
        f.open(a, t);
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
                        DbFiles.add(s, in, java.util.Collections.singletonList(f.getAbsolutePath()), null, true);
                        return in;
                    }
                }, null);
                if (a instanceof DbListScreen) ((DbListScreen) a).reload();
            }
        });
    }

    /** קבצים מתיקייה / מחיפוש — מסך המסד נפתח ברשימה, וישר חלון הבחירה. */
    static void addFiles(Activity a, String mode, long in, String what) {
        a.startActivity(new Intent(a, DbScreen.class).putExtra(DbScreen.EXTRA_MODE, mode)
                .putExtra(DbScreen.EXTRA_AT, in).putExtra(DbScreen.EXTRA_DO, what));
    }

    /** מ0.18.1: פותח רשימה לפי המסלול — באופן נתון (גם אם אינו האופן שפתוח). */
    static void openPath(Activity a, String mode, List<String> path) {
        long id = read(a, mode, path);
        if (id == 0) Ui.toast(a, Words.Verb.NOT_FOUND + Names.label(path));
        else {
            Db.saveMode(a, mode);
            DbListScreen.open(a, mode, id);
        }
    }

    /**
     * מ0.25 (באג 7): פקודות הפתיחה של קובץ — כולן, בסדר הכתיבה. o[1] != null — החלק הכותב כבר בוצע באופן הזה (צור / עדכן),
     * ולכן כאן רק פותחים (בלי ליצור שוב, ובאופן של הקובץ — לא באופן שפתוח).
     */
    static void runOpens(Activity a, List<String[]> opens, java.io.File here) {
        for (String[] o : opens) {
            if (o[1] == null) { VerbsUi.run(a, o[0], here); continue; }
            Verbs.Cmd c = Verbs.parse(o[0]);
            if (c.kind.equals(Verbs.K_LIST)) openPath(a, o[1], Names.parsePath(c.arg));
            else {
                long id = readSmart(a, o[1], c.arg);
                if (id == 0) Ui.toast(a, Words.Verb.NOT_FOUND + c.arg);
                else {
                    Db.saveMode(a, o[1]);
                    openSmart(a, o[1], id, c.sort);
                }
            }
        }
    }

    interface Job {
        long run(Store s, List<String> path);
    }

    /** פעולה שכותבת: המסד נפתח לכתיבה לרגע (עם קובץ המונה) ונסגר. 0 — נכשל (והודעה). */
    static long write(Activity a, String mode, final Job job, final List<String> path) {
        try {
            return Db.write(a, mode, null, new Db.Use<Long>() {                   // מבנה 33: דרך שער המסד
                @Override
                public Long run(SqlStore s) {
                    return job.run(s, path);
                }
            });
        } catch (Throwable t) {
            Log.e("DbVerbsUi.write", t);
            Ui.toast(a, Words.Db.failed(String.valueOf(t.getMessage())));
            return 0;
        }
    }

    /** מ0.23: האם יש במסד של האופן רשימה במסלול הזה. */
    static boolean exists(Activity a, String mode, List<String> path) {
        return mode != null && Db.dbFile(a, mode).isFile() && read(a, mode, path) != 0;
    }

    private static long read(Activity a, String mode, List<String> path) {
        SqlStore s = null;
        try {
            s = Db.openRead(a, mode);
            return DbPaths.find(s, mode, path);
        } catch (Throwable t) {
            Log.e("DbVerbsUi.read", t);
            return 0;
        } finally {
            if (s != null) s.close();
        }
    }

    private static long readSmart(Activity a, String mode, String name) {
        SqlStore s = null;
        try {
            s = Db.openRead(a, mode);
            return DbPaths.smart(s, name);
        } catch (Throwable t) {
            Log.e("DbVerbsUi.smart", t);
            return 0;
        } finally {
            if (s != null) s.close();
        }
    }

    /** פותח רשימה חכמה של המסד במסך החיפוש המוכר; sort — של הפקודה (אם נכתב), אחרת של הרשימה. */
    private static void openSmart(Activity a, String mode, long id, String sort) {
        SqlStore s = null;
        try {
            s = Db.openRead(a, mode);
            Store.Item it = s.item(id);
            String cond = s.prop(id, Store.P_COND), own = s.prop(id, Store.P_SORT), view = s.prop(id, Store.P_VIEW);
            DbListScreen.openSmart(a, it == null ? "" : it.name, cond, sort != null && !sort.isEmpty() ? sort : own, view);
        } catch (Throwable t) {
            Log.e("DbVerbsUi.openSmart", t);
        } finally {
            if (s != null) s.close();
        }
    }

    // ---------- מבנה 48: יצירת אופן מהמסך ----------

    /** האופן מוכן (נוצר, או ממשיך מהגיבוי) — המסך פותח אותו בדרכו. */
    interface ModeReady {
        void open(String name, long home);
    }

    /**
     * יצירת אופן מהמסך. המשתמש (14.11.2026): "להודיע שקיים גיבוי של אופן באותו שם. האם להמשיך את האופן הישן מהגיבוי, או לפתוח
     * בשם אחר." אין גיבוי — נוצר כרגיל. (עד כאן: נוצר מסד ריק, והמשתמש קיבל "אין בית במסד הזה".)
     */
    static void createMode(final Activity a, final String name, final ModeReady ready) {
        if (Db.modeState(a, name) != Db.MODE_HAS_BACKUP) {
            long h = Db.prepare(a, name, Reserved.FILES);
            if (h == 0) Ui.toast(a, Words.DbCmd.modeFailed(name));
            else ready.open(name, h);
            return;
        }
        Backups.Entry last = Db.lastBackup(a, name);
        new android.app.AlertDialog.Builder(a).setTitle(Words.WorkModes.HAS_BACKUP_TITLE)
                .setMessage(Words.WorkModes.hasBackup(name, last == null ? "" : Db.when(last)))
                .setPositiveButton(Words.WorkModes.RESUME, new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface d, int w) { resumeMode(a, name, ready); }
                })
                .setNegativeButton(Words.WorkModes.OTHER_NAME, new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface d, int w) { askModeName(a, ready); }
                })
                .setNeutralButton(Words.CANCEL, null).show();
    }

    /** "אופן חדש" / "שם אחר": שואלים שם, ויוצרים. שם של אופן קיים — הודעה. */
    static void askModeName(final Activity a, final ModeReady ready) {
        Ui.askText(a, Words.Db.MODE_NEW, "", Words.Db.MODE_NAME, new Ui.Text1() {
            @Override
            public void ok(String raw) {
                String name = Names.cleanName(raw).replaceAll("[:*?\"<>|\n\t]", " ").trim();
                if (raw.trim().isEmpty() || name.isEmpty()) return;
                if (Db.dbModes(a).contains(name)) {
                    Ui.toast(a, Words.WorkModes.EXISTS);
                    return;
                }
                createMode(a, name, ready);
            }
        });
    }

    /** ממשיכים מהגיבוי — ברקע (קריאה ובדיקה של הגיבוי), ואז האופן נפתח. */
    private static void resumeMode(final Activity a, final String name, final ModeReady ready) {
        final PhysicalUi.Busy busy = new PhysicalUi.Busy(a, Words.WorkModes.RESUMING);
        final android.content.Context app = a.getApplicationContext();
        new Thread(new Runnable() {
            @Override
            public void run() {
                Backups.Entry got = null;
                String err = null;
                long h = 0;
                try {
                    got = Db.resume(app, name);
                    if (got != null) h = Db.read(app, name, new Db.Use<Long>() {
                        @Override public Long run(SqlStore s) { return DbPaths.home(s); }
                    });
                } catch (Throwable t) {
                    Log.e("DbVerbsUi.resumeMode", t);
                    err = String.valueOf(t.getMessage());
                }
                final Backups.Entry e = got;
                final String error = err;
                final long home = h;
                a.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        busy.close();
                        if (a.isFinishing()) return;
                        if (error != null) Ui.toast(a, Words.Db.failed(error));
                        else if (e == null) Ui.toast(a, Words.WorkModes.NO_GOOD_BACKUP);
                        else if (home == 0) Ui.toast(a, Words.Db.NO_HOME);
                        else {
                            Ui.toast(a, Words.WorkModes.resumed(name, Db.when(e)));
                            ready.open(name, home);
                        }
                    }
                });
            }
        }).start();
    }
}
