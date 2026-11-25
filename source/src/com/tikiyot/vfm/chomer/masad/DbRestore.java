package com.tikiyot.vfm;

// מבנה 309 (המשתמש, 25.11.2026): "גיבוי יחזור רק לאופן שחזור, ומשם כל אחד יעשה מה שהוא רוצה. כיום אתה אומר לי: דיסק שלם יחזור
// למצב של לפני חודש — זה נשמע לך שימושי? רק במצב שכל הדיסק נמחק." · "תיפתח רשימה חדשה בשם האופן והתאריך השחזור, ולשם ישוחזר
// כל הגיבוי של האופן. וצריך אפשרות להעתיק מאופן לאופן כל חלק נצרך. וכך שתי ציפורים במכה אחת" (במקום סל לרשימות) · "ורק לשם.
// גם אם אופן שלם נמחק — משחזרים לאופן שחזור, ומשם יעתיק להיכן שרוצה."
// אחראי: להחזיר גיבוי אחד של אופן לרשימה חדשה באופן "שחזור" — עותק עצמאי של כל מה שהיה באופן (DbMore.copyAcross).
// אסור לו: לשנות את האופן שהגיבוי שלו (לא נפתח בכלל) ואת קובץ הגיבוי (נקרא מעותק זמני); מסכים.

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

final class DbRestore {

    private DbRestore() { }

    /** האופן שאליו חוזר כל גיבוי. אופן רגיל לכל דבר — נוצר בפעם הראשונה. */
    static final String MODE = "שחזור";  // בכונן

    static final class Res {
        final boolean ok;
        final String text, list;
        final int items;

        Res(boolean ok, String text, String list, int items) {
            this.ok = ok;
            this.text = text;
            this.list = list;
            this.items = items;
        }
    }

    /** השם של הרשימה החדשה: האופן, והיום והשעה של הגיבוי — כך רואים מאיזה מצב היא (בלי ":" — לא בשם של רשימה). */
    static String listName(String mode, long when) {
        return mode + " " + new SimpleDateFormat("dd.MM.yyyy HH.mm", Locale.US).format(new Date(when));
    }

    /** הגיבוי: name — הקובץ בשם הזה; null — העותק התקין האחרון. רק עותק של המסד (.db), לא טקסט ולא "פגום". */
    static Backups.Entry pick(Backups bk, String name, String[] why, String mode) {
        for (Backups.Entry e : bk.list()) {                                    // מהחדש לישן
            if (name != null && !e.name.equals(name)) continue;
            boolean copy = !e.text && !Backups.DAMAGED.equals(e.kind);
            int st = copy ? bk.verify(e) : Backups.BAD;
            if (copy && (st == Backups.OK || st == Backups.OK_UNRECORDED)) return e;
            if (name != null) {
                why[0] = e.text ? Words.DbCmd.RESTORE_BK_TEXT : Words.DbCmd.restoreBkBad(name);
                return null;
            }
        }
        why[0] = name == null ? Words.DbCmd.restoreBkNone(mode) : Words.DbCmd.restoreBkNoSuch(mode, name);
        return null;
    }

    /**
     * מבנה 310 (המשתמש: "תוסיף בפקודה מחיקה סופית"): הרשימה name שבבית של האופן "שחזור" — יוצאת לגמרי, עם מה שרק שלה (כמו כל
     * מחיקת רשימה; עותק של המסד לפני). אין אופן "שחזור" או אין רשימה כזו — בלי שינוי, ואינו כישלון (ניקוי שאפשר להריץ שוב).
     */
    static Res drop(android.content.Context c, final String name) {
        if (!Db.dbModes(c).contains(MODE)) return new Res(true, Words.DbCmd.restoreDropNone(name, MODE), null, 0);
        try {
            return Db.write(c, MODE, Backups.OP_DELETE, new Db.Use<Res>() {
                @Override
                public Res run(SqlStore t) {
                    long home = DbPaths.home(t);
                    for (Store.Member m : t.members(home)) {
                        if (m.item.type != Store.LIST || !m.item.name.equals(name)) continue;
                        Lists.removeFrom(t, home, m.item.id);
                        return new Res(true, Words.DbCmd.restoreDropped(name, MODE), name, 1);
                    }
                    return new Res(true, Words.DbCmd.restoreDropNone(name, MODE), null, 0);
                }
            });
        } catch (Throwable t) {
            Log.e("DbRestore.drop", t);
            return new Res(false, Words.Db.failed(String.valueOf(t.getMessage())), null, 0);
        }
    }

    /** מחזיר את הגיבוי (backup — שם הקובץ; null — האחרון התקין) של srcMode לרשימה חדשה באופן "שחזור". */
    static Res toList(android.content.Context c, String srcMode, String backup, Runnable systemFiles) {
        return toList(c, srcMode, backup, null, systemFiles);
    }

    /** מבנה 310: as — השם של הרשימה החדשה; null — האופן והתאריך של הגיבוי. תפוס — "(2)". */
    static Res toList(android.content.Context c, final String srcMode, String backup, final String as, Runnable systemFiles) {
        String[] why = {null};
        final Backups.Entry e = pick(Db.backupsOf(c, srcMode), backup, why, srcMode);
        if (e == null) return new Res(false, why[0], null, 0);
        File tmp = new File(c.getCacheDir(), "mrestore");
        tmp.mkdirs();
        File copy = new File(tmp, "from-" + System.nanoTime() + ".db");
        SqlStore src = null;
        try {
            SqlStore.copyFile(e.file, copy);                                   // הגיבוי עצמו לא נפתח — רק העותק
            src = Db.openRead(copy);
            if (Db.prepare(c, MODE, systemFiles) == 0) return new Res(false, Words.DbCmd.RESTORE_BK_NO_MODE, null, 0);
            final SqlStore from = src;
            return Db.write(c, MODE, null, new Db.Use<Res>() {
                @Override
                public Res run(SqlStore t) {
                    long home = DbPaths.home(t);
                    String name = DbMore.freeName(t, home, Store.LIST, as != null ? as : listName(srcMode, e.time()));
                    long into = Lists.newList(t, home, name);
                    int n = 0;
                    long was = DbPaths.home(from);
                    if (was != 0) {
                        for (Store.Member m : from.members(was)) n += DbMore.copyAcross(from, m.item.id, t, into);
                    } else {                                                   // גיבוי ישן, בלי בית — כל מה שאינו בתוך רשימה
                        for (Store.Item l : from.lists()) if (from.listsOf(l.id).isEmpty()) n += DbMore.copyAcross(from, l.id, t, into);
                        for (Store.Item l : from.smarts()) if (from.listsOf(l.id).isEmpty()) n += DbMore.copyAcross(from, l.id, t, into);
                    }
                    return new Res(true, Words.DbCmd.restoreBkDone(srcMode, Db.when(e), MODE, name, n), name, n);
                }
            });
        } catch (Throwable t) {
            Log.e("DbRestore.toList", t);
            return new Res(false, Words.Db.failed(String.valueOf(t.getMessage())), null, 0);
        } finally {
            if (src != null) src.close();
            copy.delete();
            new File(copy.getPath() + "-journal").delete();
            new File(copy.getPath() + "-wal").delete();
            new File(copy.getPath() + "-shm").delete();
        }
    }
}
