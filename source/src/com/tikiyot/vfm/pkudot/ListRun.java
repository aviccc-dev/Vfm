package com.tikiyot.vfm;

// מבנה 171 (סידור תיקיית הפקודות): ההרצה של שורת רשימות על המסד — הגיבוי שלפני, הכתיבה (Db.write) בשם המנהל, ההודעה,
// והמעקב אחרי מה שהשתנה. עד כאן ישב בתוך DbListCmd. אחראי: לבצע את מה שתוכנן. אסור לו: לתכנן, ולהחליט אם מריצים.

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class ListRun {

    private ListRun() { }

    /** הקבצים לפי תנאי מהאינדקס — חיפוש אחד לכל תנאי (cache). */
    static DbListCmd.Finder over(final Library lib, final Map<String, List<String>> cache) {
        return new DbListCmd.Finder() {
            @Override
            public List<String> find(String cond) {
                if (cond == null || cond.trim().isEmpty()) return new ArrayList<String>();
                if (lib == null || !lib.ready) return null;
                List<String> had = cache.get(cond);
                if (had != null) return had;
                List<String> out = new ArrayList<String>();
                for (Library.Hit h : lib.list(cond).hits) if (h.rec != null) out.add(h.path());   // מבנה 94: "קבצי חכמה" — רק מה שעונה במלואו
                cache.put(cond, out);
                return out;
            }
        };
    }

    /** ההרצה: המסד נפתח לרגע; לפני פעולה שמוציאה — גיבוי "לפני פעולה" (נכשל — לא מבצעים). */
    /** מ0.37: "גבה" — גיבוי ידני של המסד של האופן (עותק וגם טקסט); גיבוי ידני לא נמחק לבד. */
    static String backupNow(final String mode) {
        try {
            return Db.write(Db.app, mode, null, new Db.Use<String>() {
                @Override
                public String run(SqlStore s) throws Exception {
                    List<Backups.Entry> made = Db.backupsOf(Db.app, mode).manual(s, System.currentTimeMillis());
                    StringBuilder names = new StringBuilder();
                    for (Backups.Entry e : made) names.append(names.length() > 0 ? ", " : "").append(e.name);
                    return Words.DbCmd.backedUp(mode, names.toString());
                }
            });
        } catch (Throwable t) {
            Log.e("DbListCmd.backupNow", t);
            DbListCmd.lastFailed = true;
            return Words.Db.failed(String.valueOf(t.getMessage()));
        }
    }

    static String real(String mode, DbListCmd.Work w, boolean before, Library lib) {
        DbListCmd.Res r = realRes(mode, w, before, lib);
        if (r.error != null) DbListCmd.lastFailed = true;
        return r.error != null ? r.error : Words.DbCmd.inDb(r.text);
    }

    static DbListCmd.Res realRes(String mode, DbListCmd.Work w, boolean before, Library lib) {
        return realRes(mode, w, before ? Backups.OP_DELETE : null, lib);
    }

    static DbListCmd.Res realRes(final String mode, final DbListCmd.Work w, String beforeOp, final Library lib) {
        try {
            return Db.write(Db.app, mode, beforeOp, new Db.Use<DbListCmd.Res>() {   // מבנה 33: דרך שער המסד
                @Override
                public DbListCmd.Res run(SqlStore s) {
                    DbListCmd.Res r = w.run(s, over(lib, new HashMap<String, List<String>>()));
                    if (r.error == null && r.renamed != 0) follow(s, mode, r);
                    return r;
                }
            });
        } catch (Db.BeforeFailed b) {
            Log.e("DbListCmd.before", b.getCause());
            return DbListCmd.Res.err(Words.DbCmd.beforeFailed(String.valueOf(b.getCause().getMessage())));
        } catch (Throwable t) {
            Log.e("DbListCmd.real", t);
            return DbListCmd.Res.err(Words.Db.failed(String.valueOf(t.getMessage())));
        }
    }

    /** מ0.42 (ב8): אחרי שינוי שם — קובצי הפקודות שפונים לרשימה מתעדכנים, וההודעה אומרת כמה ואילו. תקלה — נאמרת, לא נבלעת. */
    static void follow(Store s, String mode, DbListCmd.Res r) {
        try {
            List<File> fs = Shortcuts.listRenamed(s, mode, r.renamed, r.renamedFrom, r.renamedTo);
            List<String> names = new ArrayList<String>();
            for (File f : fs) names.add(Text.stem(f.getName()));
            if (!names.isEmpty()) r.text = (r.text == null ? "" : r.text) + Words.DbCmd.followed(names);
        } catch (Throwable t) {
            Log.e("DbListCmd.follow", t);
            r.text = (r.text == null ? "" : r.text) + Words.DbCmd.FOLLOW_FAILED;
        }
        try {                                                                    // מבנה 307: גם הרשימות החכמות שמחפשות בה
            List<String> smarts = Lists.scopeRenamed(s, mode, r.renamed, r.renamedFrom, r.renamedTo);
            if (!smarts.isEmpty()) r.text = (r.text == null ? "" : r.text) + Words.DbCmd.followedSmart(smarts);
        } catch (Throwable t) {
            Log.e("DbListCmd.follow — רשימות חכמות", t);
            r.text = (r.text == null ? "" : r.text) + Words.DbCmd.FOLLOW_SMART_FAILED;
        }
    }
}
