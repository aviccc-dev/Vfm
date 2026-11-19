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
    static boolean run(final Activity a, Verbs.Cmd c, String line, java.io.File dir) {
        final String mode = Db.currentMode;
        if (c.kind.equals(Verbs.K_LIST)) {
            List<String> path = Names.parsePath(c.arg);
            long id;
            if (c.verb.equals(Verbs.UPDATE)) {                                   // מבנה 113: כפתור "עדכן רשימה …" — השורה עצמה, דרך המנהל
                if (verbLine(a, line, dir) != null) Ui.toast(a, Words.DbCmd.saved(c.arg));
                return true;
            }
            if (c.verb.equals(Verbs.CREATE)) {                                   // מבנה 100: דרך המנהל — השורה עצמה, כמו בקובץ פקודות
                long[] made = verbLine(a, line, dir);
                if (made == null) return true;
                id = made[0] != 0 ? made[0] : read(a, mode, path);
            } else id = read(a, mode, path);
            if (id < 0) return true;                                           // מבנה 107: יש ספק — ההודעה כבר הוצגה
            if (id == 0) Ui.toast(a, Words.Verb.NOT_FOUND + c.arg);
            else if (c.noOpen) Ui.toast(a, Words.ListCmdW.created(c.arg));     // מ0.40: /בלי-לפתוח
            else DbListScreen.open(a, mode, id, c.sort);                    // מ0.22: עם המיון שבפקודה
            return true;
        }
        if (c.kind.equals(Verbs.K_SMART)) {
            final String name = c.arg, sort = c.sort;
            long id;
            if (c.verb.equals(Verbs.CREATE) || c.verb.equals(Verbs.UPDATE)) {   // מבנה 100: דרך המנהל — השורה עצמה
                long[] made = verbLine(a, line, dir);
                if (made == null) return true;
                id = made[0] != 0 ? made[0] : readSmart(a, mode, name);
            } else id = readSmart(a, mode, name);
            if (id < 0) return true;                                           // מבנה 106: יש ספק — ההודעה כבר הוצגה
            if (id == 0) Ui.toast(a, Words.Verb.NOT_FOUND + c.arg);
            else if (c.noOpen) Ui.toast(a, Words.ListCmdW.createdSmart(c.arg));   // מ0.40: /בלי-לפתוח; מבנה 98: "חכמה", לא "ידנית"
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
            // מבנה 93: דרך המנהל, כפקודה ("צור רעיון %נבחרים%"); כאן נשארות ההודעה והטעינה מחדש
            Library lib = Library.get(a);
            Manager.Act act = Manager.onListNote(mode, here, title, text, c.cond, Paths.data(), BatchUi.vars(a, Paths.data()), lib);
            String said = act.error() != null ? act.error() : act.say(lib);
            if (act.error() != null || act.failed || act.made == 0) Ui.toast(a, said);
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
                long id = makeList(a, mode, in, name);
                if (id != 0) DbListScreen.open(a, mode, id);
            }
        });
    }

    /**
     * מבנה 91: רשימה חדשה בשם שהוקלד, בתוך in (0 — הבית) — דרך המנהל, כפקודה ("צור תיקייה ידנית %נבחרים%"). משמש את ה־➕
     * ואת בורר הרשימות. מחזיר את המספר של הרשימה; 0 — לא נוצרה, וההודעה כבר הוצגה (שם תפוס, שם לא תקין, תקלה).
     */
    static long makeList(Activity a, String mode, long in, String name) {
        Library lib = Library.get(a);
        Manager.Act act = Manager.onListNew(mode, in, name, Paths.data(), BatchUi.vars(a, Paths.data()), lib);
        if (act.error() != null) {
            Ui.toast(a, act.error());
            return 0;
        }
        String said = act.say(lib);
        if (act.failed || act.made == 0) {
            Ui.toast(a, said);
            return 0;
        }
        return act.made;
    }

    /**
     * מבנה 100: שורה של פקודה שכותבת, מכפתור — דרך המנהל. מחזיר {המספר של מה שנוצר או עודכן; 0 — כבר היה, בלי שינוי}, או null —
     * לא נעשה, וההודעה כבר הוצגה (שגיאה בשורה, אין כזו, תקלה).
     */
    private static long[] verbLine(Activity a, String line, java.io.File here) {
        Library lib = Library.get(a);
        java.io.File dir = here != null ? here : Paths.work();
        Manager.Act act = Manager.onVerbLine(line, dir, BatchUi.vars(a, dir), lib);
        if (act.error() != null) {
            Ui.toast(a, act.error());
            return null;
        }
        if (!act.writes()) return new long[]{0};
        String said = act.say(lib);
        if (act.failed) {
            Ui.toast(a, said);
            return null;
        }
        return new long[]{act.made};
    }

    /**
     * מבנה 97: רשימה חכמה חדשה ממה שבטופס, בתוך in (0 — הבית) — דרך המנהל, כפקודה ("צור רשימה חכמה %שם-חדש% %תוכן%"). מחזיר את
     * המספר שלה; 0 — לא נוצרה, וההודעה כבר הוצגה.
     */
    interface Made {
        void ok(long id);
    }

    /**
     * מבנה 104: שם קיים באותה רשימה — המסך עוצר ושואל, כמו בקובץ עם שם תפוס (המשתמש: "האם לדרוס, האם להחליף שם, האם לעצור"):
     * "לדרוס" — אותה פקודה עם `/תפוס:להחליף` (כמו בקובץ; גיבוי לפני); "שם אחר" — שואלים שם, ומנסים שוב; "לעצור" — כלום.
     * done — רק כשנוצרה או נדרסה.
     */
    static void makeSmart(final Activity a, final String mode, final long in, final DbSmartForm.Result r, final Made done) {
        makeSmart(a, mode, in, r, "", done);
    }

    private static void makeSmart(final Activity a, final String mode, final long in, final DbSmartForm.Result r, String taken, final Made done) {
        final Library lib = Library.get(a);
        Manager.Act act = Manager.onSmartNew(mode, in, r.name, r.cond, DbSmartForm.props(r), taken, Paths.data(), BatchUi.vars(a, Paths.data()), lib);
        String why = act.error();
        if (why != null && why.equals(Words.ListCmdW.smartTaken(r.name.trim().isEmpty() ? Words.SMART_LIST : r.name.trim()))) {
            new android.app.AlertDialog.Builder(a).setTitle(Words.ListCmdW.SMART_TAKEN_TITLE).setMessage(why)
                    .setPositiveButton(Words.ListCmdW.SMART_REPLACE, new android.content.DialogInterface.OnClickListener() {
                        @Override public void onClick(android.content.DialogInterface d, int w) { makeSmart(a, mode, in, r, Syntax.TAKEN_REPLACE, done); }
                    })
                    .setNegativeButton(Words.ListCmdW.OTHER_NAME, new android.content.DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(android.content.DialogInterface d, int w) {
                            Ui.askText(a, Words.ListCmdW.OTHER_NAME, r.name, Words.Short.NAME_HINT, new Ui.Text1() {
                                @Override
                                public void ok(String value) {
                                    if (value.trim().isEmpty()) return;
                                    r.name = value.trim();
                                    makeSmart(a, mode, in, r, "", done);
                                }
                            });
                        }
                    })
                    .setNeutralButton(Words.ListCmdW.SMART_STOP, null).show();
            return;
        }
        if (why != null) {
            Ui.toast(a, why);
            return;
        }
        String said = act.say(lib);
        if (act.failed || act.made == 0) {
            Ui.toast(a, said);
            return;
        }
        done.ok(act.made);
    }

    /**
     * רשימה חכמה חדשה בתוך in (0 — בתוך הבית, אם יש). מבנה 291 (אפיון החיפוש, 6 — מקום אחד לעריכה): בחלונית של מסך החיפוש —
     * מגדירים, רואים מה נמצא, ו"💾 שמירה" יוצרת אותה כאן (SearchSave). כפולים — אפשרות נוספת באותה בחירה (מ0.28, המשתמש: "כפולים אמור
     * להיות באותו תפריט של רשימה חכמה בתור עוד אופציה"), בטופס שלהם.
     */
    static void newSmart(final Activity a, final String mode, final long in) {
        Ui.options(a, Words.Main.ADD_SMART, new String[]{Words.Find.NEW_SMART, Words.Find.NEW_DUPS}, new android.content.DialogInterface.OnClickListener() {
            @Override public void onClick(android.content.DialogInterface d, int w) {
                if (w == 0) {
                    SearchScreen.newSmart(a, mode, in);
                    return;
                }
                Func f = Func.byKind(Target.KIND_DUP);
                if (f != null) f.configure(a, null, null, new Func.Done() {
                    @Override public void ok(String name, Target t) { saveDups(a, mode, in, name, t); }
                });
            }
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
        addToList(a, mode, in, java.util.Collections.singletonList(file.getAbsolutePath()), false);   // מבנה 114
        if (a instanceof DbListScreen) ((DbListScreen) a).reload();
        f.open(a, t);
    }

    /**
     * מבנה 114: קבצים מהכונן אל רשימה (list — המספר שלה; 0 — הבית) — דרך המנהל, כפקודה ("העתק %נבחרים% ידנית %נבחרים%"): אותו
     * כלל כמו בשורה כתובה. tell — להציג גם את ההודעה כשהצליח (כישלון מוצג תמיד). false — לא נוסף.
     */
    static boolean addToList(Activity a, String mode, long list, List<String> paths, boolean tell) {
        Library lib = Library.get(a);
        Manager.Act act = Manager.onListAdd(mode, list, paths, Paths.data(), BatchUi.vars(a, Paths.data()), lib);
        String said = act.error() != null ? act.error() : act.say(lib);
        boolean ok = act.error() == null && !act.failed;
        if (tell || !ok) Ui.toast(a, said);
        return ok;
    }

    /** פקודה (מתפריט הפקודות האחד) — קובץ הפקודות נכנס לרשימה. */
    static void addCommand(final Activity a, final String mode, final long in) {
        CmdMenu.show(a, null, new CmdMenu.Picked() {
            @Override
            public void ok(String icon, String name, String line, java.io.File file) {
                final java.io.File f = file != null ? file : Shortcuts.write(icon, name, line);
                addToList(a, mode, in, java.util.Collections.singletonList(f.getAbsolutePath()), false);   // מבנה 114
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
        if (id < 0) return;                                                    // מבנה 107: יש ספק — ההודעה כבר הוצגה
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
                if (id < 0) continue;                                          // מבנה 106: יש ספק — ההודעה כבר הוצגה
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
        return mode != null && Db.dbFile(a, mode).isFile() && read(a, mode, path) > 0;
    }

    /** הרשימה במסלול; מבנה 107: יש ספק ("יש כמה… לכתוב את המקום") — ההודעה מוצגת, ו־‎-1. */
    private static long read(Activity a, String mode, List<String> path) {
        SqlStore s = null;
        try {
            s = Db.openRead(a, mode);
            DbPaths.Found f = DbPaths.listAt(s, mode, path);
            if (f.ambiguous) {
                Ui.toast(a, f.error);
                return -1;
            }
            return f.error != null ? 0 : f.id;
        } catch (Throwable t) {
            Log.e("DbVerbsUi.read", t);
            return 0;
        } finally {
            if (s != null) s.close();
        }
    }

    /** מבנה 106: הרשימה החכמה לפי הכלל האחד (דרך, או שם בלי ספק); ספק — ההודעה מוצגת, ו־0. */
    private static long readSmart(Activity a, String mode, String name) {
        SqlStore s = null;
        try {
            s = Db.openRead(a, mode);
            DbPaths.Found f = DbPaths.smartAt(s, mode, Names.parsePath(name));
            if (f.ambiguous) {
                Ui.toast(a, f.error);
                return -1;
            }
            return f.error != null ? 0 : f.id;
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
            DbListScreen.openSmart(a, mode, it == null ? 0 : id, it == null ? "" : it.name, cond, sort != null && !sort.isEmpty() ? sort : own, view);
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
            long h = modeHome(a, name);
            if (h != 0) ready.open(name, h);
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

    /** מבנה 118: "צור אופן "<שם>"" (ועם /מגיבוי) — השורה, דרך המנהל. */
    private static Manager.Act modeAct(Activity a, String name, boolean fromBackup, Library lib) {
        String line = Verbs.CREATE + " " + Verbs.MODE_WORD + " \"" + name + "\"" + (fromBackup ? " " + Verbs.FROM_BACKUP : "");
        return Manager.onVerbLine(line, Paths.work(), BatchUi.vars(a, Paths.work()), lib);
    }

    /**
     * מבנה 118, 123: האופן קיים ויש בו בית — דרך המנהל, בשורה "צור אופן "<שם>"" (כמו בקובץ פקודות): אין — נוצר; קיים בלי בית
     * ויש בו רשימות — הבית נוצר; קיים — בלי כתיבה. מחזיר את הבית; 0 — לא הצליח (וההודעה הוצגה).
     */
    static long modeHome(Activity a, String name) {
        Library lib = Library.get(a);
        Manager.Act act = modeAct(a, name, false, lib);
        if (act.error() != null) {
            Ui.toast(a, act.error());
            return 0;
        }
        if (act.writes()) {
            String said = act.say(lib);
            if (act.failed) {
                Ui.toast(a, said);
                return 0;
            }
        }
        long h = homeOf(a, name);
        if (h == 0) Ui.toast(a, Words.Db.NO_HOME);
        return h;
    }

    private static long homeOf(android.content.Context c, String mode) {
        try {
            if (!Db.dbFile(c, mode).isFile()) return 0;
            return Db.read(c, mode, new Db.Use<Long>() {
                @Override public Long run(SqlStore s) { return DbPaths.home(s); }
            });
        } catch (Throwable t) {
            Log.e("DbVerbsUi.homeOf", t);
            return 0;
        }
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
        // מבנה 118: דרך המנהל — "צור אופן "<שם>" /מגיבוי", כמו בקובץ פקודות. כאן נשארים חלון ההמתנה וההודעה.
        final Library lib = Library.get(a);
        final Manager.Act act = modeAct(a, name, true, lib);
        if (act.error() != null) {
            Ui.toast(a, act.error());
            return;
        }
        final PhysicalUi.Busy busy = new PhysicalUi.Busy(a, Words.WorkModes.RESUMING);
        final android.content.Context app = a.getApplicationContext();
        new Thread(new Runnable() {
            @Override
            public void run() {
                final String said = act.writes() ? act.say(lib) : null;
                final long home = act.failed ? 0 : homeOf(app, name);
                a.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        busy.close();
                        if (a.isFinishing()) return;
                        if (act.failed) Ui.toast(a, said);
                        else if (home == 0) Ui.toast(a, Words.Db.NO_HOME);
                        else {
                            if (said != null) Ui.toast(a, said);
                            ready.open(name, home);
                        }
                    }
                });
            }
        }).start();
    }
}
