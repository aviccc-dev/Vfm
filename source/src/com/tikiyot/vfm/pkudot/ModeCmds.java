package com.tikiyot.vfm;

// מבנה 168 (סידור תיקיית הפקודות): שורות האופנים — "אופן" (מעבר, ויצירה מגיבוי), "צור אופן", "מחק אופן" (והגיבויים
// שלו לסל). עד כאן ישב בתוך DbListCmd. אחראי: לתכנן שורת אופן. אסור לו: להריץ.

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class ModeCmds {

    private ModeCmds() { }

    /**
     * מ0.42: "צור אופן "<שם>"" — אופן חדש: מסד עם בית (כמו "אופן חדש" בתפריט האופנים). קיים — בלי שינוי. ההדמיה מכירה אותו
     * מהשורה הזו: שורת "אופן" שאחריה עובדת, על מסד ריק בזיכרון. המשתמש (על הבדיקות): "סתם מצטבר שם שאריות".
     */
    /** מבנה 123: במסד של האופן יש רשימות (או רשימות חכמות) ואין בית. */
    static boolean noHome(String mode) {
        try {
            return Db.read(Db.app, mode, new Db.Use<Boolean>() {
                @Override public Boolean run(SqlStore s) { return DbPaths.home(s) == 0 && !(s.lists().isEmpty() && s.smarts().isEmpty()); }
            });
        } catch (Throwable t) {
            Log.e("DbListCmd.noHome", t);
            return false;
        }
    }

    static void planMode(final Batch.Step s, String raw, final boolean fromBackup, Sim sim) {
        final String name = raw == null ? "" : raw.trim();
        if (name.isEmpty() || !name.equals(Names.cleanName(name).replaceAll("[:*?\"<>|\n\t]", " ").trim())) {
            s.error = Words.DbCmd.modeBadName(name);
            return;
        }
        if (!DbVerbs.active()) { s.error = Words.DbCmd.VERIFY_DB_ONLY; return; }
        s.target = name;
        // מבנה 47: אופן ש"בטל אופן" שלפני ביטל — אינו "קיים" (עד כאן: "צור אופן" אחריו לא עשה דבר, ו"אופן" אחריהם נכשל בתכנון)
        if ((Db.dbModes(Db.app).contains(name) && !sim.dropped.contains(name)) || sim.created.contains(name)) {
            // מבנה 123: אופן שיש במסד שלו רשימות ואין בו בית (מבנה 56) — "צור אופן" מתקן: הבית נוצר, והרשימות נכנסות אליו. עד כאן
            // תיקן רק תפריט האופנים שבמסך, בעצמו. מסד ריק — לא נוגעים.
            if (!sim.created.contains(name) && noHome(name)) {
                s.describe = Words.Cmd.modeRepairWill(name);
                s.op = new ListCmd.Op() {
                    @Override public String run(Library lib) {
                        if (Db.repairHome(Db.app, name) != 0) return Words.Cmd.modeRepaired(name);
                        DbListCmd.lastFailed = true;
                        return Words.DbCmd.modeFailed(name);
                    }
                };
                return;
            }
            s.noop = true;
            s.describe = Words.DbCmd.modeExists(name);
            return;
        }
        // מבנה 48 (המשתמש: "להודיע שקיים גיבוי של אופן באותו שם. האם להמשיך את האופן הישן מהגיבוי, או לפתוח בשם אחר"): בקובץ פקודות
        // אין את מי לשאול — בלי /מגיבוי השורה היא שגיאה שאומרת את שתי הדרכים. אופן ששורה קודמת מבטלת — הגיבוי שלו ייווצר רק
        // בהרצה, ולכן שם זה נבדק בהרצה.
        final boolean has = !sim.dropped.contains(name) && Db.modeState(Db.app, name) == Db.MODE_HAS_BACKUP;
        if (has && !fromBackup) { s.error = Words.DbCmd.modeHasBackup(name); return; }
        sim.dropped.remove(name);
        sim.created.add(name);
        MemStore m = new MemStore(IdCounter.memory());
        DbPaths.ensureHome(m);
        sim.dbs.put(name, m);
        s.describe = has ? Words.DbCmd.modeResumeWill(name) : Words.DbCmd.modeWill(name);
        s.op = new ListCmd.Op() {
            @Override
            public String run(Library lib) {
                int st = Db.modeState(Db.app, name);
                if (st == Db.MODE_EXISTS) return Words.DbCmd.modeExists(name);
                if (st == Db.MODE_HAS_BACKUP) {
                    if (!fromBackup) { DbListCmd.lastFailed = true; return Words.DbCmd.modeHasBackup(name); }
                    try {
                        Backups.Entry e = Db.resume(Db.app, name);
                        if (e != null) return Words.WorkModes.resumed(name, Db.when(e));
                        DbListCmd.lastFailed = true;
                        return Words.DbCmd.modeNoGoodBackup(name);
                    } catch (Throwable t) {
                        Log.e("DbListCmd.planMode: resume", t);
                        DbListCmd.lastFailed = true;
                        return Words.Db.failed(String.valueOf(t.getMessage()));
                    }
                }
                if (Db.prepare(Db.app, name, Reserved.FILES) != 0) return Words.DbCmd.modeCreated(name);
                DbListCmd.lastFailed = true;
                return Words.DbCmd.modeFailed(name);
            }
        };
    }

    /**
     * מ0.45: "בטל אופן "<שם>"" (נבנה בשם "מחק אופן", שעדיין נקרא) — המסד של האופן נמחק (עם קובצי היומן שלו). יש בו משהו — גיבוי לפני (לא נשמר — לא נמחק).
     * לא: האופן הפתוח במסך, "שמור לתוכנה", והאופן שהקובץ עובד בו עכשיו. אין אופן כזה — בלי שינוי (ניקוי שאפשר להריץ שוב).
     * ההדמיה: "אופן" אחריו — שגיאה.
     * מבנה 59 (המשתמש, 14.11.2026: "כל עוד האופן קיים יש צורך בגיבוי שלו. כאשר הוא נמחק אין צורך לגיבוי. אם ישתנה בהמשך נראה"):
     * תיקיית הגיבויים של האופן עוברת לסל (לא לצמיתות — כמו תיקיית ההגדרות שלו), ולכן "צור אופן" באותו שם פותח אופן חדש וריק,
     * בלי שאלה. מי שמתחרט — משחזר את התיקייה מהסל, ואז "צור אופן" באותו שם מציע להמשיך מהגיבוי (מבנה 48). אופן שכבר אינו
     * ונשארו ממנו גיבויים (מגרסה קודמת) — "בטל אופן" שולח גם אותם לסל.
     */
    static void planDropMode(final Batch.Step s, final String name, Sim sim) {
        if (!DbVerbs.active()) { s.error = Words.DbCmd.VERIFY_DB_ONLY; return; }
        s.target = name;
        if (!sim.hasMode(name)) {
            if (!sim.dropped.contains(name) && !name.equals(Db.MENUS_MODE) && Db.modeState(Db.app, name) == Db.MODE_HAS_BACKUP) {
                sim.dropped.add(name);                                          // מבנה 59: "צור אופן" שאחריו — נבדק בהרצה (הגיבויים כבר לא יהיו)
                s.describe = Words.DbCmd.dropModeLeftWill(name);
                s.op = new ListCmd.Op() {
                    @Override
                    public String run(Library lib) {
                        if (Db.dbFile(Db.app, name).isFile()) return Words.DbCmd.dropModeNone(name);   // נוצר בינתיים — לא נוגעים
                        int bk = backupsToTrash(lib, name);
                        if (bk < 0) DbListCmd.lastFailed = true;
                        return bk == 0 ? Words.DbCmd.dropModeNone(name) : Words.DbCmd.dropModeLeft(name, bk > 0);
                    }
                };
                return;
            }
            s.noop = true;
            s.describe = Words.DbCmd.dropModeNone(name);
            return;
        }
        if (name.equals(Db.MENUS_MODE)) { s.error = Words.DbCmd.dropModeSystem(name); return; }
        if (Db.inUse(name) || name.equals(sim.mode)) { s.error = Words.DbCmd.dropModeOpen(name); return; }   // מבנה 63: גם אופן שמסך המסד מחזיק
        sim.created.remove(name);
        sim.dropped.add(name);
        sim.dbs.remove(name);
        s.describe = Words.DbCmd.dropModeWill(name);
        s.op = new ListCmd.Op() {
            @Override
            public String run(Library lib) {
                android.content.Context c = Db.app;
                File f = Db.dbFile(c, name);
                if (!f.isFile()) {                                               // מבנה 59: אין מסד — ואם נשארו גיבויים, לסל
                    int left = backupsToTrash(lib, name);
                    if (left < 0) DbListCmd.lastFailed = true;
                    return left == 0 ? Words.DbCmd.dropModeNone(name) : Words.DbCmd.dropModeLeft(name, left > 0);
                }
                if (Db.inUse(name)) { DbListCmd.lastFailed = true; return Words.DbCmd.dropModeOpen(name); }
                boolean kept = false;
                try {
                    kept = Db.write(c, name, null, new Db.Use<Boolean>() {
                        @Override
                        public Boolean run(SqlStore st) throws Exception {
                            long home = DbPaths.home(st);
                            boolean empty = st.lists().isEmpty() && st.smarts().isEmpty() && st.unassigned().isEmpty()
                                    && (home == 0 || st.members(home).isEmpty());
                            if (empty) return false;
                            Db.backupsOf(c, name).before(st, Backups.OP_DROP_MODE, System.currentTimeMillis());   // רק למסד שיש בו משהו
                            return true;
                        }
                    });
                } catch (Throwable t) {
                    Log.e("DbListCmd.dropMode", t);
                    DbListCmd.lastFailed = true;
                    return Words.DbCmd.dropModeFailed(name, Words.DbCmd.DROP_MODE_NO_BACKUP);
                }
                if (!android.database.sqlite.SQLiteDatabase.deleteDatabase(f) && f.exists()) {
                    DbListCmd.lastFailed = true;
                    return Words.DbCmd.dropModeFailed(name, Words.Db.failed(f.getName()));
                }
                if (name.equals(Db.savedMode(c))) Db.saveMode(c, "");   // הפתיחה הבאה — באופן אחר
                // מ0.45+ (claude/bitul-ofen.md, המשתמש: "למה להשאיר? בלי אופן היא סתם בלבול"): תיקיית ההגדרות שלו — לסל (לא לצמיתות)
                boolean folder = false;
                String note = "";
                File dir = Modes.dirOf(name);
                if (dir.isDirectory() && !name.equals(Modes.current()) && lib != null) {
                    Physical.Result r = Physical.deleteOwn(lib, dir);            // מבנה 64 (ממצא 45): עד כאן המחיקה הרגילה סירבה — "זו תיקיית התוכנה"
                    folder = r.failed == 0 && !r.done.isEmpty();
                    if (!folder) {
                        note = Words.DbCmd.DROP_MODE_FOLDER_KEPT;
                        Log.i("ביטול אופן: תיקיית ההגדרות לא עברה לסל — " + dir + " · " + r.note);
                    }
                }
                int bk = backupsToTrash(lib, name);                              // מבנה 59: אופן שבוטל — אין צורך בגיבויים שלו
                if (bk < 0) note += Words.DbCmd.DROP_MODE_BACKUPS_KEPT;
                Log.i("בוטל אופן: " + name + (kept ? " (גיבוי לפני)" : " (ריק)") + (bk > 0 ? " · הגיבויים לסל" : "") + (folder ? " · התיקייה לסל" : ""));
                return Words.DbCmd.dropModeDone(name, bk > 0, folder) + note;
            }
        };
    }

    /**
     * מבנה 59: תיקיית הגיבויים של אופן שבוטל — לסל של הכונן (שחזור מהסל מחזיר את כולה). 0 — אין תיקייה כזו; 1 — עברה;
     * ‎-1 — לא עברה (נשארת במקומה, ו"צור אופן" באותו שם יגיד שיש גיבוי).
     */
    static int backupsToTrash(Library lib, String name) {
        File dir = Db.backupsDir(name);
        if (!dir.isDirectory()) return 0;
        if (lib == null) {
            Log.i("ביטול אופן: הגיבויים לא עברו לסל (אין אינדקס) — " + dir);
            return -1;
        }
        Physical.Result r = Physical.deleteOwn(lib, dir);                         // מבנה 64: התיקייה של המסד מוגנת — זו מחיקה של התוכנה עצמה
        if (r.failed == 0 && !r.done.isEmpty()) return 1;
        Log.i("ביטול אופן: הגיבויים לא עברו לסל — " + dir + " · " + r.note);
        return -1;
    }
}
