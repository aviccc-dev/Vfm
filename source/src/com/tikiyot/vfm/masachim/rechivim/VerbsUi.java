package com.tikiyot.vfm;

// הביצוע של פקודות הפתיחה (מבנה 13, 13.11.2026): פתיחת מסך לפי שמו, תיקייה, חיפוש, הרצת קובץ, תפריט, כלי, "חזור".
// עד כאן ישב בתוך Verbs (קומת הפקודות), ולכן הפקודות "הכירו" עשרה מסכים. השפה — הפענוח, התיאור, התחביר — נשארה ב־Verbs;
// כאן רק מה שנוגע במסך. מבנה 131: כל פותח נרשם אצל המנהל בשם של מה שהוא פותח; ההחלטה (פענוח, "היעד רשום?", כיוון) — במנהל.
// אחראי: לרשום את הפותחים ולפתוח. אסור לו: לפענח או להחליט בעצמו (Manager.open), לשנות קבצים בכונן (Batch, דרך Physical).

import android.app.Activity;
import android.content.Intent;
import java.io.File;
import java.util.Map;

final class VerbsUi {

    private VerbsUi() { }

    // ---------- מבנה 131: מי שפותח נרשם אצל המנהל; ההחלטה — אצלו ----------
    // עד כאן המקום הזה פענח את השורה, החליט מה היא ופתח. עכשיו: הפענוח, הבדיקה שהיעד רשום והכיוון — במנהל (Manager.open);
    // כאן רק הרישום של כל פותח בשם של מה שהוא פותח, והפתיחה עצמה. מסך שלא נרשם — אינו נפתח.

    private abstract static class Do implements Manager.Opener {
        abstract void go(Activity a, Verbs.Cmd c, File here);

        @Override public boolean open(Activity a, Verbs.Cmd c, String full, File here) {
            go(a, c, here);
            return true;
        }
    }

    static {                                                                    // מבנה 142: הרשימות של הקומה הזו — רשומות אצל המנהל
        Manager.list(Words.PermitW.R_TOOLS, new Manager.Lister() {
            @Override public java.util.List<String> names() {
                java.util.List<String> l = new java.util.ArrayList<String>();
                for (Func f : Func.all()) l.add(f.kind);
                return l;
            }
        });
        Manager.list(Words.PermitW.R_ACTIONS, new Manager.Lister() {
            @Override public java.util.List<String> names() {
                java.util.List<String> l = new java.util.ArrayList<String>();
                for (Actions.Action x : Actions.all()) l.add(x.id);
                return l;
            }
        });
    }

    private static void screen(String name, Do d) {
        Manager.register(Manager.screenKey(name), d);
    }

    static {
        Manager.register(Manager.OPEN_DB, new Manager.Opener() {                 // מ0.10: במסד — פקודות הרשימות על המסד
            @Override public boolean open(Activity a, Verbs.Cmd c, String full, File here) { return DbVerbsUi.run(a, c, full, here); }
        });
        Manager.register(Verbs.K_BACK, new Do() {                                // באג (1.17.6): בבית — יציאה מהתוכנה;
            @Override void go(Activity a, Verbs.Cmd c, File here) { a.onBackPressed(); }   // בבית של המסד — בחירת אופן (DbListScreen.onBackPressed)
        });
        Manager.register(Verbs.K_MENU, new Do() {                                // 1.17.0
            @Override void go(Activity a, Verbs.Cmd c, File here) { CmdMenu.showList(a, Names.parsePath(c.arg), null); }
        });
        Manager.register(Verbs.K_TOOL, new Do() {                                // מ0.19: הכלי עם הפרמטרים שבשורה
            @Override void go(Activity a, Verbs.Cmd c, File here) {
                Target t = Verbs.toolTarget(c.arg);
                Func f = t == null ? null : Func.byKind(t.kind);
                if (f == null) Ui.toast(a, Words.Verb.NOT_FOUND + c.arg);
                else f.open(a, t);
            }
        });
        Manager.register(Verbs.K_NEW_CMD, new Do() {
            @Override void go(Activity a, Verbs.Cmd c, File here) { CmdMenu.create(a, here, null); }
        });
        Manager.register(Verbs.K_FOLDER, new Do() {
            @Override void go(Activity a, Verbs.Cmd c, File here) {
                if (new File(c.arg).isDirectory()) DiskScreen.open(a, c.arg);
                else Ui.toast(a, Words.Verb.NOT_FOUND + c.arg);
            }
        });
        Do noDb = new Do() {                                                     // מ0.26: רשימות — רק במסד (DbVerbsUi.run)
            @Override void go(Activity a, Verbs.Cmd c, File here) { Ui.toast(a, Words.DbCmd.NO_DB); }
        };
        for (String k : new String[]{Verbs.K_LIST, Verbs.K_SMART, Verbs.K_NEW_LIST, Verbs.K_ADD, Verbs.K_NOTE, Verbs.K_TITLE, Verbs.K_MODE})
            Manager.register(k, noDb);
        Manager.register(Verbs.K_FIND, new Do() {
            @Override void go(Activity a, Verbs.Cmd c, File here) {
                Intent i = new Intent(a, SearchScreen.class);
                i.putExtra(SearchScreen.EXTRA_QUERY, c.cond);
                i.putExtra(SearchScreen.EXTRA_FIXED, true);
                i.putExtra(SearchScreen.EXTRA_TITLE, Words.Verb.FIND + c.cond);
                i.putExtra(SearchScreen.EXTRA_SORT, c.sort.isEmpty() ? Sort.NAME : c.sort);
                i.putExtra(SearchScreen.EXTRA_VIEW, FolderView.FLAT);
                a.startActivity(i);
            }
        });
        Manager.register(Verbs.K_RUN, new Do() {
            @Override void go(Activity a, Verbs.Cmd c, File here) {
                File f = new File(c.arg);
                if (!f.isAbsolute() && here != null) f = new File(here, c.arg);
                if (!f.isFile()) Ui.toast(a, Words.Verb.NOT_FOUND + f.getName());
                else if (!Ui.openPlugin(a, f)) BatchUi.run(a, f);              // מבנה 204: "הרץ" על תוסף — פותח אותו
            }
        });
        // המסכים — כל אחד בשמו. זה המקום האחד שבו כתוב איזה מסך נפתח לכל שם.
        screen(Verbs.S_HOME, new Do() {
            @Override void go(Activity a, Verbs.Cmd c, File here) { Home.go(a); }
        });
        screen(Verbs.S_SEARCH, new Do() {
            @Override void go(Activity a, Verbs.Cmd c, File here) { a.startActivity(new Intent(a, SearchScreen.class)); }
        });
        screen(Verbs.S_COMMANDS, new Do() {                                      // 1.16.1: אותו תפריט בכל מקום
            @Override void go(Activity a, Verbs.Cmd c, File here) { CmdMenu.show(a, null); }
        });
        screen(Verbs.S_DISK, new Do() {
            @Override void go(Activity a, Verbs.Cmd c, File here) { DiskScreen.open(a, c.cond.isEmpty() ? null : c.cond); }
        });
        screen(Verbs.S_VIRTUAL, new Do() {
            @Override void go(Activity a, Verbs.Cmd c, File here) { a.startActivity(SearchScreen.fileManager(a, c.cond.isEmpty() ? null : c.cond)); }
        });
        screen(Verbs.S_SETTINGS, new Do() {
            @Override void go(Activity a, Verbs.Cmd c, File here) { a.startActivity(new Intent(a, SettingsScreen.class)); }
        });
        screen(Verbs.S_TRASH, new Do() {
            @Override void go(Activity a, Verbs.Cmd c, File here) { a.startActivity(new Intent(a, TrashScreen.class)); }
        });
        screen(Verbs.S_EDITOR, new Do() {                                        // מ0.45+: העורך האינטראקטיבי (הישן נמחק)
            @Override void go(Activity a, Verbs.Cmd c, File here) { LabScreen.edit(a, null); }
        });
        screen(Verbs.S_BACKUPS, new Do() {                                       // מ0.37
            @Override void go(Activity a, Verbs.Cmd c, File here) { DbScreen.openBackups(a); }
        });
        screen(Verbs.S_DIAG, new Do() {                                          // מ0.37: דוח האבחון כפקודה; מ0.43: תוסף האבחון
            @Override void go(Activity a, Verbs.Cmd c, File here) { LabScreen.open(a, Words.DbCmd.TOOL_DIAG); }
        });
        Do plugins = new Do() {                                                  // מ0.38: המעבדה; מ0.40: התוספים — רק בפקודה
            @Override void go(Activity a, Verbs.Cmd c, File here) { LabScreen.open(a, c.cond, c.ver, null); }
        };
        screen(Verbs.S_LAB, plugins);
        screen(Verbs.S_PLUGINS, plugins);
    }

    /** מבצע שורה; false — אינה פקודת פתיחה (אז היא שורה למנוע הרגיל). here — התיקייה של הקובץ (או null). */
    static boolean run(Activity a, String line, File here) {
        String said = Manager.open(a, line, here, BatchUi.vars(a, here != null ? here : Paths.work()));
        if (said == null) return false;
        if (!said.isEmpty()) Ui.toast(a, said);
        return true;
    }
}
