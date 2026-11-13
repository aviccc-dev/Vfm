package com.tikiyot.vfm;

// הביצוע של פקודות הפתיחה (מבנה 13, 13.11.2026): פתיחת מסך לפי שמו, תיקייה, חיפוש, הרצת קובץ, תפריט, כלי, "חזור".
// עד כאן ישב בתוך Verbs (קומת הפקודות), ולכן הפקודות "הכירו" עשרה מסכים. השפה — הפענוח, התיאור, התחביר — נשארה ב־Verbs;
// כאן רק מה שנוגע במסך. screen() הוא המקום האחד שבו כתוב איזה מסך נפתח לכל שם. אותן שורות.
// אחראי: לבצע פקודת פתיחה על המסך. אסור לו: לפענח בעצמו (Verbs), לשנות קבצים בכונן (Batch, דרך Physical).

import android.app.Activity;
import android.content.Intent;
import java.io.File;
import java.util.Map;

final class VerbsUi {

    private VerbsUi() { }

    // ---------- ביצוע ----------

    /** מבצע שורה; false — אינה פקודת פתיחה (אז היא שורה למנוע הרגיל). here — התיקייה של הקובץ (או null). */
    static boolean run(Activity a, String line, File here) {
        if (!Verbs.isVerb(line)) return false;
        Map<String, String> vars = BatchUi.vars(a, here != null ? here : Paths.work());
        Verbs.Cmd c = Verbs.parse(Verbs.withVars(line, vars));
        if (c.error != null) {
            Ui.toast(a, c.error);
            return true;
        }
        if (DbVerbs.active() && DbVerbsUi.run(a, c)) return true;               // מ0.10: במסד — פקודות הרשימות על המסד
        String host = Verbs.hostCode(c);
        if (host != null) {
            Ui.toast(a, Words.Top.notHere(Verbs.label(c)));                         // מ0.42: אין עוד מסך שעושה אותן בעצמו (TopRow.Host — נמחק)
            return true;
        }
        if (c.kind.equals(Verbs.K_BACK)) {                                          // באג (1.17.6): בבית — יציאה מהתוכנה;
            a.onBackPressed();                                                // בבית של המסד — בחירת אופן (DbListScreen.onBackPressed)
        }
        else if (c.kind.equals(Verbs.K_MENU)) CmdMenu.showList(a, Names.parsePath(c.arg), null);   // 1.17.0
        else if (c.kind.equals(Verbs.K_TOOL)) {                                       // מ0.19: הכלי עם הפרמטרים שבשורה
            Target t = Verbs.toolTarget(c.arg);
            Func f = t == null ? null : Func.byKind(t.kind);
            if (f == null) Ui.toast(a, Words.Verb.NOT_FOUND + c.arg);
            else f.open(a, t);
        }
        else if (c.kind.equals(Verbs.K_NEW_CMD)) CmdMenu.create(a, here, null);
        else if (c.kind.equals(Verbs.K_SCREEN)) screen(a, c);
        else if (c.kind.equals(Verbs.K_FOLDER)) {
            if (new File(c.arg).isDirectory()) DiskScreen.open(a, c.arg);
            else Ui.toast(a, Words.Verb.NOT_FOUND + c.arg);
        } else if (c.kind.equals(Verbs.K_LIST)) {
            Ui.toast(a, Words.DbCmd.NO_DB);                                   // מ0.26: רשימות — רק במסד (DbVerbsUi.run, למעלה)
        } else if (c.kind.equals(Verbs.K_SMART)) {
            Ui.toast(a, Words.DbCmd.NO_DB);
        } else if (c.kind.equals(Verbs.K_FIND)) {
            Intent i = new Intent(a, SearchScreen.class);
            i.putExtra(SearchScreen.EXTRA_QUERY, c.cond);
            i.putExtra(SearchScreen.EXTRA_FIXED, true);
            i.putExtra(SearchScreen.EXTRA_TITLE, Words.Verb.FIND + c.cond);
            i.putExtra(SearchScreen.EXTRA_SORT, c.sort.isEmpty() ? Sort.NAME : c.sort);
            i.putExtra(SearchScreen.EXTRA_VIEW, FolderView.FLAT);
            a.startActivity(i);
        } else if (c.kind.equals(Verbs.K_RUN)) {
            File f = new File(c.arg);
            if (!f.isAbsolute() && here != null) f = new File(here, c.arg);
            if (f.isFile()) BatchUi.run(a, f); else Ui.toast(a, Words.Verb.NOT_FOUND + f.getName());
        }
        return true;
    }

    private static void screen(Activity a, Verbs.Cmd c) {
        String s = c.arg;
        if (s.equals(Verbs.S_HOME)) Home.go(a);
        else if (s.equals(Verbs.S_SEARCH)) a.startActivity(new Intent(a, SearchScreen.class));
        else if (s.equals(Verbs.S_COMMANDS)) CmdMenu.show(a, null);             // 1.16.1: אותו תפריט בכל מקום
        else if (s.equals(Verbs.S_DISK)) DiskScreen.open(a, c.cond.isEmpty() ? null : c.cond);
        else if (s.equals(Verbs.S_VIRTUAL)) a.startActivity(SearchScreen.fileManager(a, c.cond.isEmpty() ? null : c.cond));
        else if (s.equals(Verbs.S_SETTINGS)) a.startActivity(new Intent(a, SettingsScreen.class));
        else if (s.equals(Verbs.S_TRASH)) a.startActivity(new Intent(a, TrashScreen.class));
        else if (s.equals(Verbs.S_EDITOR)) LabScreen.edit(a, null);                // מ0.45+: העורך האינטראקטיבי (הישן נמחק)
        else if (s.equals(Verbs.S_BACKUPS)) DbScreen.openBackups(a);                // מ0.37
        else if (s.equals(Verbs.S_DIAG)) LabScreen.open(a, Words.DbCmd.TOOL_DIAG);  // מ0.37: דוח האבחון כפקודה; מ0.43: תוסף האבחון
        else if (s.equals(Verbs.S_LAB) || s.equals(Verbs.S_PLUGINS)) LabScreen.open(a, c.cond, c.ver, null);   // מ0.38: המעבדה; מ0.40: התוספים — רק בפקודה
    }
}
