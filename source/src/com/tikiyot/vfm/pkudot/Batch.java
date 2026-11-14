package com.tikiyot.vfm;

// קובצי פקודות בסגנון bat (117א, שלב 1 — המנוע). קורא את הקובץ, מתכנן (תצוגה מקדימה: מה ייעשה, בלי לשנות
// דבר), ומבצע — כל שינוי בכונן דרך Physical (אישור, סל המחזור, רשימות עוקבות, אינדקס).
// פקודות: REM ECHO MD/MKDIR RD/RMDIR DEL/ERASE COPY MOVE REN/RENAME START (ו-"@ECHO OFF", "::" כהערה).
// מתגים: /תאריך: /גודל: /תפוס: · ROBOCOPY: /MAXAGE: /MINAGE: /MAX: /MIN: · XCOPY: /D: · COPY: /Y /-Y · RD: /S /Q · DEL: /Q /F
// · /A:H — גם קבצים מוסתרים (בלי — מדלגים עליהם, כמו ב-bat). %~dp0 — כמו %כאן%.
// משתנים קבועים (לקריאה בלבד): %פנימי% %כרטיס% %הורדות% %כאן%. מסלול יחסי — ביחס לתיקייה של הקובץ.
// אסור לו: לשנות משהו בכונן בעצמו (רק דרך Physical), או לפעול בלי תצוגה מקדימה ואישור.

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

final class Batch {

    static final String EXT = Files.CMD_EXT;

    static boolean isBatch(String name) {
        return Files.isCmdFile(name);
    }

    /** שורה מתוכננת: מה תעשה, על אילו קבצים, ומה נכשל כבר בתכנון. */
    static final class Step {
        int line;
        String text, cmd, error;
        final List<String> files = new ArrayList<String>();
        final List<String> dirs = new ArrayList<String>();
        File dest;
        String newName;
        /** 1.13.0: פקודת פתיחה — השורה, אחרי המשתנים. */
        String open;
        boolean replace, skipTaken, recursive, hidden;
        /** מ0.37: "ודא שגיאה" — השורה הזו צריכה להיכשל בהרצה (בתכנון היא תקינה). */
        boolean expectFail;
        /** 1.19.0: COPY/MOVE /S — שמירת מסלול: השורש שממנו נמדד המסלול; files — כל הקבצים, dirs — תיקיות ריקות. */
        String keepRoot;
        /** 1.19.0: /יומן: — קובץ היומן (מסלול מלא), או null. */
        String journal;
        /** 1.20.0: מקור מתוך השוואה (%רק-במקור% / %רק-ביעד%) — בהעברה לא מוחקים תיקיות שהתרוקנו (הן קיימות בצד השני). */
        boolean fromCompare;
        /** 1.20.0: /תפוס:לעצור — שם תפוס נרשם ככישלון, בלי לשאול. */
        boolean failTaken;
        /** 1.21.0: COPY … /מצב:<זמן> — שחזור: לכל קובץ — תיקיית היעד שלו (המסלול הלוגי). */
        final List<String> targets = new ArrayList<String>();
        /** מ0.25: כמה קבצים דולגו בגלל /תפוס:לדלג — לתיאור ולדוח ("דולגו", לא "אין קבצים"). */
        int skipped;
        /** מ0.40: /תפוס:לשנות-שם; same — כמה נשארו במקומם כי יש ביעד קובץ זהה להם; inPlace — העברה אל התיקייה שהם כבר בה. */
        boolean renameTaken;
        int same, inPlace;
        /** מ0.40: /מכיל: — נבדק שוב בהרצה (קובץ ששורה קודמת יוצרת — בתכנון עוד אין לו תוכן; ומחיקה — רק במה שבאמת מכיל). */
        List<byte[]> contains;

        /**
         * מ0.40: לפני ההרצה — האם הקובץ src (שיגיע ל־to) יוצא מהשורה לפי /תפוס: (ונספר). מקום אחד לכל סוגי השורות.
         * לשנות-שם: אם יש בתיקיית היעד קובץ זהה לו — לא זז; אחרת נשאר, ושם תפוס יקבל בהרצה שם אחר.
         */
        boolean drop(Sim sim, File src, File to) {
            if (skipTaken && sim.isFile(to)) { skipped++; return true; }
            if (renameTaken && sim.sameIn(src, to.getParentFile())) { same++; return true; }
            return false;
        }

        /** לאן הקובץ יגיע בהדמיה: בשם תפוס עם לשנות-שם — בשם הפנוי הבא. */
        File arrive(Sim sim, File to) {
            return renameTaken && sim.isFile(to) ? sim.free(to) : to;
        }
        long stateWhen = Long.MIN_VALUE;
        /** 1.22.0: COMP /רשימה:<שם> — המקור הוא רשימה ידנית (עם כל מה שבתוכה). */
        String listName;
        /** 203: COPY … %שיתוף% — נאסף לשליחה אחת בסוף. */
        boolean share;
        /** MD לתיקייה שכבר קיימת — בלי שינוי (ולא שגיאה). */
        boolean noop;
        /** 1.24.0: שורה על רשימות (ListCmd) — מה שיתבצע בהרצה. */
        ListCmd.Op op;
        /** 1.25.0: בתוך לולאה — הלולאות שסביב השורה (מזהים) והסבב בכל אחת; null — מחוץ ללולאה. */
        int[] loops, rounds;
        /** 1.25.0: מדידה — כמה זמן לקחה (אלפיות שנייה). */
        long ms = -1;
        /** 1.26.0: בוצעה בהרצה (לא דולגה, לא נעצרה לפניה) — לסיכום. */
        boolean ran;
        /** 1.26.0: על מה השורה פועלת (רשימה / תיקייה) — לסיכום; null — מתוך describe. */
        String target;
        /** 1.27.0: MD ידנית — הרשימה (לניקוי שבסוף, ולאזהרה כשכבר קיימת). */
        List<String> listPath;
        /** מ0.14: ודא — עבר (TRUE) / נכשל (FALSE) בהרצה; null — אינה ודא, או לא רצה. */
        Boolean verified;

        /** "12" · בלולאה — "12 (סבב 3)". */
        String label() {
            return rounds == null ? String.valueOf(line) : line + Words.Loop.round(rounds[rounds.length - 1]);
        }
        int total;              // לפני הסינון
        String describe;
    }

    /** התוכנית כולה. */
    static final class Plan {
        final List<Step> steps = new ArrayList<Step>();
        final List<String> echoes = new ArrayList<String>();
        File start;
        int errors;
        /**
         * מ0.30 (ממצא 47; המשתמש: "צריך לעצור לפני שהוא מריץ משהו, אלא אם כן תהיה פקודה מיוחדת" · "המשך עם מספר. אם אין
         * מספר אז המשך תמיד"): כמה שורות שגיאה מותרות — 0 בלי השורה "המשך"; "המשך N" — עד N; "המשך" לבד — בלי גבול.
         */
        int allowErrors;

        /** מ0.37: "דוח "<קובץ>"" — היומן של ההרצה נכתב גם לקובץ הזה (מסלול מלא), או null. */
        String reportFile;

        /** מ0.32 (ממצא 38): מאיזו שורה (מקום ברשימת השורות) כל "אופן" בתוקף — כדי לדעת באיזה אופן רצה כל שורה. */
        final List<Integer> modeFrom = new ArrayList<Integer>();
        final List<String> modeName = new ArrayList<String>();

        /** האופן שבו רצה השורה: ה"אופן" האחרון שלפניה בקובץ; null — לא נכתב (האופן שעל המסך). */
        String modeOf(Step s) {
            int at = steps.indexOf(s);
            String m = null;
            for (int i = 0; i < modeFrom.size(); i++) if (modeFrom.get(i) <= at) m = modeName.get(i);
            return m;
        }

        /** יש יותר שורות שגיאה מהמותר — שום שורה לא רצה. */
        boolean blocked() {
            return errors > allowErrors;
        }

        /** שורות השגיאה, לחלון שמסביר למה הקובץ לא רץ. */
        String errorText() {
            StringBuilder sb = new StringBuilder();
            for (Step s : steps) if (s.error != null) sb.append(s.label()).append(". ⚠ ").append(s.error).append("\n     ").append(s.text).append("\n");
            return sb.toString();
        }
        /** 203: מה שהוכן לשליחה (עותקים ואריזות בתיקיית השליחה) — נשלח פעם אחת, בסוף. */
        final List<File> shared = new ArrayList<File>();
        File stage;
        /** 1.10.0: "בכאילו" בקובץ — שום דבר לא מתבצע; רק ההדמיה (sim) מוצגת כרשימה זמנית. */
        boolean asIf;
        /** 1.10.0: המצב הווירטואלי בסוף התכנון — מה יהיה איפה (לבכאילו). */
        Sim sim;
        /** 1.13.0: פקודת פתיחה בעברית (פתח / מצא / חזור…) — נפתחת בסוף ההרצה, כמו START; null — אין. */
        String open;
        /**
         * מ0.25 (באג 7): כל פקודות הפתיחה בסדר הכתיבה (לא רק האחרונה). לכל אחת: [השורה, האופן] — האופן אינו null כשהחלק
         * הכותב כבר בוצע כשלב רגיל במסד (צור / עדכן), ואז בסוף רק פותחים, באופן של הקובץ.
         */
        final List<String[]> opens = new ArrayList<String[]>();
        /** 216: משהו בהרצה לא הלך חלק (שורה שדולגה, כשל, עצירה) — אז הדוח מוצג; אחרת — הודעה קצרה בלבד. */
        boolean trouble;
        /** 1.25.0: שורת "מדידה" — זמן לכל שורה, סיכום, ויומן (אם נכתב /יומן:). */
        boolean measure;
        /** מבנה 38: הדופק של ההרצה (רק עם "מדידה"). */
        Pulse pulse;
        String measureJournal;
        /** מ0.45: "מדידה /עד:<זמן>" — שורה שלקחה יותר מסומנת ❌ (ונספרת עם "ודא" שנכשלו); 0 — בלי. */
        long lineLimit;
        /** 1.25.0: הלולאות — לכל מזהה: הגבול לסבב (אלפיות שנייה; 0 — בלי) והשורה של "חזור". */
        final List<Long> loopLimit = new ArrayList<Long>();
        final List<Integer> loopLine = new ArrayList<Integer>();
        /** 1.27.0: הקלט — בראש הדוח והיומן (קובץ, ערכים שמולאו). */
        final List<String> inputs = new ArrayList<String>();
        /** 1.25.4: המשתמש עצר בזמן ההכנה — לא מריצים. */
        boolean stopped;
        /** 1.25.0: לולאה שנעצרה בתכנון (הסבב עבר את הגבול) — מוצג בתצוגה המקדימה. */
        final List<String> notes = new ArrayList<String>();

        int changes() {
            int n = 0;
            for (Step s : steps) if (s.error == null && !s.noop && !s.cmd.equals("ECHO") && !s.cmd.equals("START")
                    && !(s.cmd.equals(OPEN) && s.op == null) && !s.cmd.equals("COMP")) n++;
            return n;
        }
    }

    private Batch() { }

    /**
     * מ0.37 (המשתמש, 11.11.2026: "אם כבר אז תאסוף כמה קטנות, לא רק את אלה של הבדיקות"): כתוב · המתן · גבה · רענן · דוח ·
     * "ודא שגיאה". מחזיר true אם השורה שלהן (ואז s מוכן: שגיאה, או תיאור ופעולה).
     */
    private static boolean small(final Plan plan, final Step s, String cmd, List<String> tk, File here,
                                 Map<String, String> vars, final Sim sim) {
        if (cmd.equals(DROP_MODE)) {                                            // מ0.45: מחק אופן
            if (tk.size() != 2 || tk.get(1).trim().isEmpty()) { s.error = Words.DbCmd.DROP_MODE_USAGE; return true; }
            DbListCmd.planDropMode(s, Verbs.withVars(tk.get(1).trim(), vars), sim);
            return true;
        }
        if (cmd.equals(UNTRASH)) {                                              // מ0.45: שחזר מהסל
            planUntrash(s, tk, here, vars, sim);
            return true;
        }
        if (cmd.equals(PURGE)) {                                                // מ0.45: רוקן סל
            planPurge(s, tk, here, vars);
            return true;
        }
        if (cmd.equals(VERIFY) && tk.size() == 2 && tk.get(1).trim().equals(Words.DbCmd.W_OK_SINCE)) {   // מ0.45: ודא תקין
            s.cmd = OK_SINCE;
            s.describe = Words.DbCmd.verifyWill(Words.DbCmd.OK_SINCE_WHAT);
            return true;
        }
        if (cmd.equals(VERIFY) && tk.size() == 4 && tk.get(1).trim().equals(Verbs.MODE_WORD)) {   // מ0.45: ודא אופן "<שם>" קיים / חסר
            final String m = Verbs.withVars(tk.get(2).trim(), vars);
            String w = tk.get(3).trim();
            final boolean want = w.equals(Words.DbCmd.WORD_EXISTS_M) || w.equals(Words.DbCmd.WORD_EXISTS);
            if (!want && !w.equals(Words.DbCmd.WORD_MISSING_M) && !w.equals(Words.DbCmd.WORD_MISSING)) { s.error = Words.DbCmd.VERIFY_USAGE; return true; }
            if (!DbVerbs.active()) { s.error = Words.DbCmd.VERIFY_DB_ONLY; return true; }
            final String what = Words.DbCmd.verifyMode(m, want);
            s.target = m;
            s.describe = Words.DbCmd.verifyWill(what);
            s.op = new ListCmd.Op() {
                @Override public String run(Library lib) {
                    boolean has = Db.dbModes(Db.app).contains(m);
                    s.verified = has == want;
                    return Words.DbCmd.verifyResult(has == want, what, has ? Words.DbCmd.WORD_EXISTS_M : Words.DbCmd.WORD_MISSING_M);
                }
            };
            return true;
        }
        if (cmd.equals(SECTION)) {                                              // מ0.44: קטע
            if (tk.size() != 2 || tk.get(1).trim().isEmpty()) { s.error = Words.DbCmd.SECTION_USAGE; return true; }
            s.noop = true;
            s.describe = Words.DbCmd.section(Verbs.withVars(tk.get(1).trim(), vars));
            return true;
        }
        if (cmd.equals(VERIFY) && tk.size() == 2 && tk.get(1).trim().equals(W_DB)) {   // מ0.42: ודא מסד
            DbListCmd.planVerifyDb(s);
            return true;
        }
        if (cmd.equals(VERIFY) && tk.size() == 2 && tk.get(1).trim().equals(W_ERROR)) {
            s.cmd = EXPECT;
            s.noop = true;
            s.describe = Words.DbCmd.EXPECT_NEXT;
            return true;
        }
        if (cmd.equals(WAIT)) {
            int sec = -1;
            if (tk.size() == 2) try { sec = Integer.parseInt(tk.get(1).trim()); } catch (NumberFormatException e) { sec = -1; }
            if (sec < 0 || sec > 600) { s.error = Words.DbCmd.WAIT_USAGE; return true; }
            final int n = sec;
            s.describe = Words.DbCmd.waitFor(n);
            s.op = new ListCmd.Op() {
                @Override public String run(Library lib) {
                    try { Thread.sleep(n * 1000L); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    return Words.DbCmd.waitFor(n);
                }
            };
            return true;
        }
        if (cmd.equals(REPORT)) {
            String[] err = {null};
            String p = tk.size() == 2 ? path(tk.get(1), here, vars, err) : null;
            if (p == null) { s.error = err[0] != null ? err[0] : Words.DbCmd.REPORT_USAGE; return true; }
            plan.reportFile = p;
            s.noop = true;
            s.describe = Words.DbCmd.reportTo(Bidi.path(p));
            return true;
        }
        if (cmd.equals(REFRESH)) {
            int readers = 0;                                                     // מ0.45 (ב9): "רענן מלא /קוראים:N" — כמה ראשים הכי מהיר
            List<String> a = new ArrayList<String>();
            for (int i = 1; i < tk.size(); i++) {
                String x = tk.get(i).trim();
                if (!x.startsWith(Words.DbCmd.SW_READERS)) { a.add(x); continue; }
                try { readers = Integer.parseInt(x.substring(Words.DbCmd.SW_READERS.length()).trim()); } catch (NumberFormatException e) { readers = -1; }
                if (readers < 1 || readers > 16) { s.error = Words.DbCmd.READERS_USAGE; return true; }
            }
            final boolean deep = a.size() == 1 && a.get(0).equals(W_DEEP);
            final boolean full = a.size() == 1 && a.get(0).equals(W_FULL);   // מ0.43: בנייה מחדש — מה שהיה הכפתור בהגדרות, כבדיקה
            if (a.size() > 1 || (a.size() == 1 && !deep && !full)) { s.error = Words.DbCmd.REFRESH_USAGE; return true; }
            if (readers > 0 && !full) { s.error = Words.DbCmd.READERS_USAGE; return true; }
            final int rd = readers;
            final String note = rd > 0 ? Words.DbCmd.readersNote(rd) : "";
            s.describe = full ? Words.DbCmd.REBUILD_WILL + note : Words.DbCmd.refreshWill(deep);
            // מ0.40 — המשתמש: "הוא יכול להריץ פקודה 'רענן אינדקס' לפני שהוא מתחיל". עד כאן "רענן" רץ רק בהרצה — אחרי שכל
            // השורות כבר תוכננו, ורשימה חכמה בשורה שאחריו לא ראתה קובץ שהורד הרגע. עכשיו — רענון מהיר כבר בהכנה (ברקע, לא
            // משנה קבצים), והשורות שאחריו מתוכננות מול האינדקס המעודכן. בהרצה — כמו קודם (גם מה ששורות קודמות שינו).
            if (sim.lib != null && sim.lib.ready)
                try {
                    int n = sim.lib.refreshAsked();
                    sim.condFiles.clear();                                           // מה שחכמות מצאו לפני הרענון — לא תקף
                    s.describe += Words.DbCmd.refreshedInPlan(n);
                } catch (Throwable t) {
                    Log.e("Batch.plan: רענן", t);                                     // בכונן
                }
            s.op = new ListCmd.Op() {
                @Override public String run(Library lib) {
                    if (lib == null) return full ? Words.DbCmd.REBUILD_WILL + note : Words.DbCmd.refreshWill(deep);
                    if (full) {
                        if (rd > 0) lib.build(Words.Lib.WHY_REQUEST, null, rd);
                        else lib.build(null);
                        return Words.DbCmd.rebuilt(lib.totalFiles(), lib.totalDirs()) + note;
                    }
                    return Words.DbCmd.refreshed(deep ? lib.deepRefresh() : lib.refreshAsked());
                }
            };
            return true;
        }
        if (cmd.equals(BACKUP)) {
            if (tk.size() != 1) { s.error = Words.DbCmd.BACKUP_USAGE; return true; }
            if (!DbVerbs.active()) { s.error = Words.DbCmd.VERIFY_DB_ONLY; return true; }
            if (sim.modeError != null) { s.error = sim.modeError; return true; }
            final String mode = sim.mode != null ? sim.mode : Db.currentMode;
            s.describe = Words.DbCmd.backupWill(mode);
            s.op = new ListCmd.Op() {
                @Override public String run(Library lib) { return DbListCmd.backupNow(mode); }
            };
            return true;
        }
        if (cmd.equals(WRITE)) {
            boolean append = false;
            List<String> a = new ArrayList<String>();
            for (int i = 1; i < tk.size(); i++) {
                if (tk.get(i).trim().equals(SW_APPEND)) append = true;
                else a.add(tk.get(i));
            }
            if (a.isEmpty() || a.size() > 2) { s.error = Words.DbCmd.WRITE_USAGE; return true; }
            String[] err = {null};
            String p = path(a.get(0), here, vars, err);
            if (p == null) { s.error = err[0]; return true; }
            final File f = new File(p);
            final String text = a.size() == 2 ? Verbs.withVars(a.get(1), vars).replace("\\n", "\n") : "";
            File dir = f.getParentFile();
            if (dir == null || !sim.isDir(dir)) { s.error = Words.DbCmd.writeNoFolder(Bidi.path(dir == null ? p : dir.getAbsolutePath())); return true; }
            if (sim.isDir(f)) { s.error = Words.DbCmd.WRITE_IS_FOLDER; return true; }
            final boolean add = append;
            sim.gone.remove(p);
            if (!sim.isFile(f)) sim.added.put(p, GHOST);
            s.dest = f;
            s.describe = Words.DbCmd.writeWill(f.getName(), text.length(), add);
            s.op = new ListCmd.Op() {
                @Override public String run(Library lib) {
                    try {
                        if (lib != null) {
                            String why = Physical.forbidden(lib, f.getParentFile());
                            if (why != null) { DbListCmd.lastFailed = true; return why; }
                        }
                        if (f.isDirectory()) { DbListCmd.lastFailed = true; return Words.DbCmd.WRITE_IS_FOLDER; }
                        boolean was = f.isFile();
                        Files.writeAtomic(f, add && was ? Files.readUtf8(f) + text : text);
                        if (lib != null) {
                            if (was) lib.fileRemoved(f.getAbsolutePath());
                            lib.fileAdded(f);
                            lib.flush(5000);   // מ0.45: שורה שאחריה (מחק, העבר) — מול הגודל החדש, לא "הקובץ השתנה מאז הסריקה"
                        }
                        return Words.DbCmd.written(f.getName(), text.length(), add && was);
                    } catch (Throwable t) {
                        Log.e("Batch.write", t);
                        DbListCmd.lastFailed = true;
                        return Words.Db.failed(String.valueOf(t.getMessage()));
                    }
                }
            };
            return true;
        }
        return false;
    }

    /**
     * מ0.45: "שחזר מהסל "<מסלול מקורי>"" — מה שנמחק מהמסלול הזה (האחרון, אם נמחק כמה פעמים) חוזר למקומו, כמו "שחזור" במסך הסל
     * (Physical.restore: שם תפוס — "(משוחזר)"; לא חוזר לרשימות). מה שנמחק בשורות שלפני עוד אינו בסל בהכנה — ולכן לא שגיאה
     * בהכנה אלא "ייבדק בהרצה", ושם: אין — כישלון.
     */
    private static void planUntrash(final Step s, List<String> tk, File here, Map<String, String> vars, Sim sim) {
        if (tk.size() != 2) { s.error = Words.DbCmd.UNTRASH_USAGE; return; }
        String[] err = {null};
        final String p = path(tk.get(1), here, vars, err);
        if (p == null) { s.error = err[0] != null ? err[0] : Words.DbCmd.UNTRASH_USAGE; return; }
        final File f = new File(p);
        boolean now = false;
        if (sim.lib != null && sim.lib.ready)
            try {
                now = trashed(sim.lib, p, false).size() > 0;
            } catch (Throwable t) {
                Log.e("Batch.plan: שחזר מהסל", t);
            }
        s.target = f.getName();
        s.dest = f;
        s.describe = Words.DbCmd.untrashWill(f.getName(), now);
        sim.gone.remove(p);
        if (!sim.isFile(f) && !sim.isDir(f)) sim.added.put(p, GHOST);
        s.op = new ListCmd.Op() {
            @Override public String run(Library lib) {
                if (lib == null) return s.describe;
                lib.flush(5000);                                                   // מה שנמחק הרגע — נכנס לאינדקס של הסל
                List<Trash.Item> its = trashed(lib, p, false);
                if (its.isEmpty()) { DbListCmd.lastFailed = true; return Words.DbCmd.notInTrash(f.getName()); }
                Trash.Item last = its.get(0);
                for (Trash.Item it : its) if (newer(it.name(), last.name())) last = it;
                Physical.Result r = Physical.restore(lib, java.util.Collections.singletonList(last));
                if (r.failed > 0 || r.done.isEmpty()) {
                    DbListCmd.lastFailed = true;
                    return Words.Db.failed(r.note);
                }
                String to = r.to.get(last.path());
                return Words.DbCmd.untrashed(f.getName(), to != null && !to.equals(p) ? new File(to).getName() : null);
            }
        };
    }

    /** מ0.45: "רוקן סל" — כל הסל; "רוקן סל "<מסלול מקורי>"" — מה שנמחק מהמסלול הזה (או מתוכו). Physical.deleteForever. */
    private static void planPurge(final Step s, List<String> tk, File here, Map<String, String> vars) {
        if (tk.size() > 2) { s.error = Words.DbCmd.PURGE_USAGE; return; }
        final String p;
        if (tk.size() == 2) {
            String[] err = {null};
            p = path(tk.get(1), here, vars, err);
            if (p == null) { s.error = err[0] != null ? err[0] : Words.DbCmd.PURGE_USAGE; return; }
            s.target = new File(p).getName();
            s.describe = Words.DbCmd.purgeWill(new File(p).getName());
        } else {
            p = null;
            s.describe = Words.DbCmd.PURGE_ALL_WILL;
        }
        s.op = new ListCmd.Op() {
            @Override public String run(Library lib) {
                if (lib == null) return s.describe;
                lib.flush(5000);
                List<Trash.Item> its = p == null ? Trash.list(lib) : trashed(lib, p, true);
                if (its.isEmpty()) return p == null ? Words.DbCmd.purged(0, 0) : Words.DbCmd.notInTrash(new File(p).getName());
                Physical.Result r = Physical.deleteForever(lib, its);
                if (r.failed > 0) DbListCmd.lastFailed = true;
                return Words.DbCmd.purged(r.done.size(), r.failed);
            }
        };
    }

    /**
     * מה שנמחק אחרי: השם בסל מתחיל בזמן המחיקה (עד השנייה), ובאותה שנייה נוסף לשם "(2)", "(3)"… (Physical.freeName) — הקצר
     * קודם, ובאורך שווה — לפי הסדר.
     */
    static boolean newer(String a, String b) {
        int k = Math.min(19, Math.min(a.length(), b.length()));
        int c = a.substring(0, k).compareTo(b.substring(0, k));
        if (c != 0) return c > 0;
        if (a.length() != b.length()) return a.length() > b.length();
        return a.compareTo(b) > 0;
    }

    /** מה שבסל ונמחק מהמסלול p (under — גם מתוך התיקייה p). */
    static List<Trash.Item> trashed(Library lib, String p, boolean under) {
        List<Trash.Item> out = new ArrayList<Trash.Item>();
        for (Trash.Item it : Trash.list(lib))
            if (it.original != null && (it.original.equals(p) || (under && it.original.startsWith(p + "/")))) out.add(it);
        return out;
    }

    /**
     * מ0.43: דוח של הרצה — ליומן התקלות בקיצור: כמה שורות, הסיכום של "ודא", וכל שורה עם ❌ או ⚠ (עד 30). עד כאן — כל הדוח, ובדיקה
     * אחת (300 שורות) דחקה את התקלות מהיומן. הדוח המלא — בשורה "דוח" ובמסך ההרצה.
     */
    static String logLine(List<String> report) {
        if (report.isEmpty()) return Words.Cmd.NO_CHANGE;
        if (report.size() <= 12) return report.toString();
        StringBuilder sb = new StringBuilder(Words.DbCmd.logLines(report.size()));
        int shown = 0;
        for (String r : report) {
            boolean bad = r.contains("❌") || r.contains("⚠");
            boolean sum = r.contains(Words.DbCmd.VERIFY_SUM_MARK);
            if ((bad && shown < 30) || sum) {
                sb.append(" · ").append(r);
                if (bad) shown++;
            }
        }
        return sb.toString();
    }

    /** מ0.37: "דוח "<קובץ>"" — ההודעות (ECHO) והיומן נכתבים לקובץ; כישלון בכתיבה — שורה ביומן, לא עוצר. */
    private static void writeReport(Plan plan, List<String> report) {
        if (plan.reportFile == null) return;
        try {
            StringBuilder sb = new StringBuilder();
            for (String e : plan.echoes) sb.append(e).append('\n');
            if (!plan.echoes.isEmpty()) sb.append('\n');
            for (String r : report) sb.append(r).append('\n');
            File f = new File(plan.reportFile);
            File dir = f.getParentFile();
            if (dir != null && !dir.isDirectory()) dir.mkdirs();
            Files.writeAtomic(f, sb.toString());
            report.add(Words.DbCmd.reportWritten(Bidi.path(plan.reportFile)));
        } catch (Throwable t) {
            Log.e("Batch.report", t);
            report.add(Words.DbCmd.reportFailed(String.valueOf(t.getMessage())));
        }
    }

    /** מ0.37: שורת "ודא שגיאה" — סימון בלבד; מה שנבדק היא השורה שאחריה. */
    static final String EXPECT = "EXPECT";

    /** מ0.37: אחרי התכנון — כל "ודא שגיאה" נצמד לשורה שאחריו: נכשלה כבר בתכנון — עברה (ואינה שגיאה); אחרת — תיבדק בהרצה. */
    static void expectErrors(Plan plan) {
        for (int i = 0; i < plan.steps.size(); i++) {
            Step m = plan.steps.get(i);
            if (!EXPECT.equals(m.cmd) || m.error != null) continue;
            Step next = i + 1 < plan.steps.size() ? plan.steps.get(i + 1) : null;
            if (next == null || EXPECT.equals(next.cmd)) {
                m.noop = false;
                m.error = Words.DbCmd.EXPECT_NOTHING;
                plan.errors++;
            } else if (next.error != null) {
                next.describe = Words.DbCmd.expectOk(next.error);
                next.error = null;
                next.noop = true;
                next.op = null;
                next.verified = Boolean.TRUE;
                plan.errors--;
            } else next.expectFail = true;
        }
    }

    /**
     * מ0.31 (המשתמש, 11.11.2026: "המשך סתם רק פעם אחת, אם יש מספר בתוך סוגריים אז כפי המספר, אם סוגריים ריקות אז ממשיך
     * בלי עצירה בכלל"): כמה כישלונות מותרים לפי שורת "המשך". ‎-1 — הצורה לא מובנת.
     */
    static int allowOf(String line) {
        String rest = line.trim();
        int sp = 0;
        while (sp < rest.length() && !Character.isWhitespace(rest.charAt(sp)) && rest.charAt(sp) != '(') sp++;
        rest = rest.substring(sp).trim();
        if (rest.isEmpty()) return 1;
        if (rest.startsWith("(") && rest.endsWith(")")) rest = rest.substring(1, rest.length() - 1).trim();
        if (rest.isEmpty()) return Integer.MAX_VALUE;
        try {
            int n = Integer.parseInt(rest);
            return n < 0 ? -1 : n;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // ---------- קריאה ----------

    /** מפצל שורה לחלקים כמו cmd: רווחים מפרידים, ומרכאות שומרות רווחים (המרכאות עצמן יורדות). */
    static List<String> tokens(String line) {
        List<String> out = new ArrayList<String>();
        StringBuilder cur = new StringBuilder();
        boolean q = false, any = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                q = !q;
                any = true;
            } else if (!q && Character.isWhitespace(c)) {
                if (any || cur.length() > 0) out.add(cur.toString());
                cur.setLength(0);
                any = false;
            } else {
                cur.append(c);
            }
        }
        if (any || cur.length() > 0) out.add(cur.toString());
        return out;
    }

    // ---------- התחביר בעברית — במקום אחד (131). גם העורך לוקח מכאן. ----------
    /** משתנים: %פנימי% %כרטיס% %הורדות% %כאן%. */
    static final String V_INTERNAL = Drive.INTERNAL, V_CARD = Drive.CARD, V_DOWNLOADS = "הורדות", V_HERE = "כאן";   // תחביר
    /**
     * 203: "%שיתוף%" — יעד שאינו תיקייה: מה שמועתק אליו נשלח למכשיר אחר בסוף (חלון תוכנות ההעברה, או היעד הקבוע).
     * המסלול שהוא מקבל — סימן שלא ייתכן בכונן.
     */
    static final String V_SHARE = "שיתוף", SHARE_MARK = "/\u0000share";   // תחביר
    /** 1.10.0: שורה "בכאילו" — הקובץ רץ בהדמיה בלבד (המשתמש: "פקודה כללית בכל קובץ פקודות שתאמר שהיא רצה רק בכאילו"). */
    static final String AS_IF = "בכאילו";   // תחביר
    /** 1.13.0: "הדמיה" — שם נרדף לבכאילו, רשמי יותר (המשתמש: "זו מילה נרדפת לבכאילו, וגם יותר רשמית, פחות סלנג"). */
    static final String AS_IF2 = "הדמיה";   // תחביר
    /** 1.25.0: "מדידה" — שורת כותרת: זמן לכל שורה (claude/pkudot-reshimot.md). לולאה: "חזור N [/עד:<זמן>]" … "סוף". */
    static final String MEASURE = "מדידה", LOOP = "חזור", LOOP_END = "סוף", V_ROUND = "סבב";   // תחביר
    /** 1.28.0: "בדיקה" — שורת כותרת: הקובץ רץ במסך ההרצה, בשלבים ובאישור. בלעדיה — רץ מיד. */
    static final String CHECK = "בדיקה";   // תחביר
    /** מ0.14: ודא — בדיקה בתוך קובץ פקודות (✅ / ❌), לא עוצרת. */
    static final String VERIFY = "VERIFY";
    /** מ0.15: אופן — מכאן והלאה הפקודות במסד של אופן אחר; CALL — שורות של קובץ אחר במקום השורה. */
    static final String MODE = "MODE", CALL = "CALL", CONT = "CONTINUE";
    /** מ0.37 — הפקודות הקטנות: כתוב לקובץ · המתן · גבה (המסד) · רענן (האינדקס) · דוח (היומן לקובץ). */
    static final String WRITE = "WRITE", WAIT = "WAIT", BACKUP = "BACKUP", REFRESH = "REFRESH", REPORT = "REPORT", LAB = "LAB";
    /**
     * מ0.44: "קטע "<שם>"" — שורה בדוח, במקום שלה בהרצה ("▸ <שם>"). ההודעות (ECHO) עולות לראש הדוח; הקטע מחלק את הדוח לפי הסדר —
     * תוסף האבחון יודע כך איזו בדיקה עברה ואיזו נכשלה, כשמריצים כמה ביחד. לא משנה דבר.
     */
    static final String SECTION = "SECTION";
    /**
     * מ0.45 (המשתמש: "צריך קודם פקודה חדשה… יצירה של אופן חדש באמת… הסל: שחזור וריקון"): מחק אופן · שחזר מהסל · רוקן סל.
     * ו"ודא תקין" — כל השורות מתחילת הקטע (או הקובץ) רצו בלי כישלון (לבדיקה שנבנית מהגדרת הפקודות, CmdMatrix).
     */
    static final String DROP_MODE = "DROPMODE", UNTRASH = "UNTRASH", PURGE = "PURGE", OK_SINCE = "OKSINCE";
    static final String SW_APPEND = "/הוסף", W_DEEP = "עמוק", W_ERROR = "שגיאה", W_FULL = "מלא";   // תחביר
    /** מ0.42: "ודא מסד" — המנוע של המסד מול ה־SQLite של המכשיר, ותקינות המסד של כל אופן. */
    static final String W_DB = "מסד";   // תחביר
    static final String V_TIME = "שעה", V_UNIQUE = "ייחודי";   // תחביר
    /** מ0.17: קישור — אותה רשימה / אותו פריט גם במקום נוסף (במסד). */
    static final String LINK = "LINK";
    /** מ0.15: עומק מרבי של CALL בתוך CALL. */
    static final int CALL_DEPTH = 5;
    /** הגבול לסבבים בלולאה, ולשורות אחרי הפריסה. */
    static final int MAX_ROUNDS = 1000, MAX_LINES = 50000;
    /** 1.13.0: שלב של פקודת פתיחה בעברית (Verbs). */
    static final String OPEN = "OPEN";

    /** 1.10.0: שורה אחת בתוצאת ההדמיה. */
    static final class AsIfRow {
        final String icon, title, sub;

        AsIfRow(String icon, String title, String sub) {
            this.icon = icon;
            this.title = title;
            this.sub = sub;
        }
    }

    /**
     * 1.10.0: תוצאת ההדמיה — המצב בסוף, אחרי כל השורות: מה יופיע ואיפה (ומאיפה בא), תיקיות חדשות, מה ייצא לסל,
     * מה יישלח, מה ייפתח בסוף, ומה לא יתבצע. שום דבר לא נוגע בכונן.
     */
    static List<AsIfRow> asIfRows(Plan plan) {
        List<AsIfRow> out = new ArrayList<AsIfRow>();
        for (String e : plan.echoes) out.add(new AsIfRow("💬", e, Words.AsIf.ECHO));
        for (Step s : plan.steps)
            if (s.error != null) out.add(new AsIfRow("⚠", s.error, s.line + ". " + s.text));
        java.util.Set<String> sources = new java.util.HashSet<String>();
        for (File f : plan.sim.added.values()) sources.add(f.getAbsolutePath());
        java.util.Set<String> movedDirs = new java.util.HashSet<String>();
        for (Step s : plan.steps)
            if (s.error == null && s.cmd.equals("MOVE")) movedDirs.addAll(s.dirs);
        for (String d : new java.util.TreeSet<String>(plan.sim.dirs))
            out.add(new AsIfRow("📁", new File(d).getName(), Words.AsIf.NEW_FOLDER + Bidi.path(d)));
        for (Map.Entry<String, File> e : plan.sim.added.entrySet()) {
            String from = e.getValue().getAbsolutePath();
            out.add(new AsIfRow("📄", new File(e.getKey()).getName(),
                    Words.AsIf.WILL_BE + Bidi.path(new File(e.getKey()).getParent())
                            + (from.equals(e.getKey()) ? "" : Words.AsIf.FROM + Bidi.path(from))));
        }
        for (String g : new java.util.TreeSet<String>(plan.sim.gone))
            if (!sources.contains(g) && !movedDirs.contains(g))
                out.add(new AsIfRow("🗑", new File(g).getName(), Words.AsIf.TO_TRASH + Bidi.path(g)));
        for (Step s : plan.steps)                                               // 1.24.0: רשימות
            if (s.error == null && s.op != null) out.add(new AsIfRow("🗂", s.describe, s.line + ". " + s.text));
        for (Step s : plan.steps)
            if (s.error == null && s.share)
                out.add(new AsIfRow("📤", Words.AsIf.SHARE, s.describe));
        for (String[] o : plan.opens) out.add(new AsIfRow("▶", Verbs.label(Verbs.parse(o[0])), Words.AsIf.AT_END));   // מ0.25: כולן, בסדר
        if (plan.start != null)
            out.add(new AsIfRow("▶", isHome(plan.start) ? Words.Cmd.HOME_AT_END : plan.start.getName(), Words.AsIf.AT_END));
        if (out.isEmpty()) out.add(new AsIfRow("·", Words.AsIf.NOTHING, ""));
        return out;
    }

    /** 1.9.0: "%בית%" — רק אחרי START: בסוף ההרצה חוזרים למסך הבית. */
    static final String V_HOME = "בית", HOME_MARK = "/\u0000home";   // תחביר

    /** START %בית% — היעד שבסוף הוא מסך הבית. */
    static boolean isHome(File f) {
        return f != null && HOME_MARK.equals(f.getPath());
    }
    /** מתגי סינון: /תאריך:… /גודל:… · מ0.40: /מכיל:<טקסט> (בלי הקו הנטוי — כמו SW_DATE). */
    static final String SW_DATE = DateSize.DATE, SW_SIZE = DateSize.SIZE, SW_CONTAINS = "מכיל:";   // תחביר
    /** שם תפוס: /תפוס:לשאול · /תפוס:לדלג · /תפוס:להחליף */
    static final String SW_TAKEN = "/תפוס:", TAKEN_ASK = "לשאול", TAKEN_SKIP = "לדלג", TAKEN_REPLACE = "להחליף";   // תחביר
    /**
     * מ0.40: /תפוס:לשנות-שם — שם תפוס: בשם אחר (כמו "(2)"), בלי לשאול; וקובץ שכבר יש ביעד קובץ זהה לו (גודל, ואז חתימה) — לא
     * זז ממקומו. המשתמש: "אם יש קובץ זהה כבר לפי החתימה הוא לא מעביר את הקובץ שנמצא במקום אחר. רק כאשר הקובץ שונה בחתימה וזהה
     * בשם — אז הוא מעביר אותו בשם אחר".
     */
    static final String TAKEN_RENAME = "לשנות-שם";   // תחביר

    /** "/תפוס:<ערך>" — ערך מוכר. */
    static boolean takenValue(String sw) {
        for (String v : new String[]{TAKEN_ASK, TAKEN_SKIP, TAKEN_REPLACE, TAKEN_STOP, TAKEN_RENAME}) if (sw.equals(SW_TAKEN + v)) return true;
        return false;
    }

    static String takenUnknown(String sw) {
        return Words.Cmd.switchUnknown(sw) + " (" + TAKEN_ASK + " · " + TAKEN_SKIP + " · " + TAKEN_REPLACE + " · " + TAKEN_STOP
                + " · " + TAKEN_RENAME + ")";
    }
    /** 1.19.0: שמירת מסלול — /S (כמו XCOPY), ובעברית /מסלול. יומן — /יומן:<קובץ>. */
    static final String SW_KEEP = "/מסלול", SW_JOURNAL = "/יומן:";   // תחביר
    /** 1.19.0: %תאריך% — זמן ההרצה (קבוע לכל הקובץ); %מזהה-כרטיס% — שם הכרטיס הקבוע (למשל ABCD-1234). */
    static final String V_NOW = "תאריך", V_CARD_ID = "מזהה-כרטיס";   // תחביר
    /** 1.20.0: תוצאת COMP האחרונה — מקור לשורות COPY/MOVE שאחריה (תמיד עם שמירת מסלול). /עד:N% — סף. */
    static final String V_ONLY_A = "רק-במקור", V_ONLY_B = "רק-ביעד", ONLY_A_MARK = "/\u0000onlyA", ONLY_B_MARK = "/\u0000onlyB";   // תחביר
    static final String SW_LIMIT = "/עד:", TAKEN_STOP = "לעצור";   // תחביר
    /** 1.21.0: COPY <גיבוי[\\תת־תיקייה][\\תבנית]> <יעד> /מצב:<זמן> — שחזור המצב של אותו זמן. */
    static final String SW_STATE = "/מצב:";   // תחביר
    /** 1.22.0: COMP /רשימה:"<רשימה>" <יעד> — המקור הוא רשימה ידנית (קבצים, תתי־רשימות, חכמות, תיקיות מתעדכנות). */
    static final String SW_LIST = "/רשימה:";   // תחביר
    static final String NOW_FORMAT = Files.NOW_FORMAT;

    static String var(String name) {
        return "%" + name + "%";
    }

    /** מתג: מתחיל ב-"/" ושמו מוכר (כך "/storage/..." אינו מתג). */
    private static final Pattern SWITCH = Pattern.compile(
            "^/(-?Y|S|Q|F|A(:.*)?|D(:.*)?|" + SW_DATE + ".*|" + SW_SIZE + ".*|" + SW_TAKEN.substring(1)
                    + ".*|" + SW_KEEP.substring(1) + "|" + SW_JOURNAL.substring(1) + ".*|" + SW_LIMIT.substring(1)
                    + ".*|" + SW_STATE.substring(1) + ".*|" + SW_LIST.substring(1) + ".*|" + SW_APPEND.substring(1) + "|" + SW_CONTAINS + ".*|MAXAGE:.*|MINAGE:.*|MAX:.*|MIN:.*)$",
            Pattern.CASE_INSENSITIVE);

    static boolean isSwitch(String t) {
        return SWITCH.matcher(t).matches();
    }

    /** מסלול מלא שנבחר בבורר — בסגנון bat: המשתנה הארוך ביותר שמתאים בהתחלה, ו-"\\". */
    static String toVar(String abs, Map<String, String> vars) {
        String best = null, bestVal = "";
        for (Map.Entry<String, String> e : vars.entrySet()) {
            if (e.getKey().equals(V_HERE)) continue;                      // תלוי בקובץ — לא מציעים
            String v = e.getValue();
            if ((abs.equals(v) || abs.startsWith(v + "/")) && v.length() > bestVal.length()) {
                best = e.getKey();
                bestVal = v;
            }
        }
        String rest = best == null ? abs : abs.substring(bestVal.length());
        String out = (best == null ? "" : "%" + best + "%") + rest.replace('/', '\\');
        return out.isEmpty() ? abs : out;
    }

    /** משתנים קבועים. */
    /**
     * מ0.41: קובץ שכולו פקודת פתיחה אחת (חוץ מהכותרת — צלמית, שם, למה — ומהערות): השורה; אחרת null. קובץ כזה רץ מיד, בלי
     * הכנה ותצוגה מקדימה — כמו כפתור (הכפתורים בשורות הם קובצי פקודות).
     */
    static String singleVerb(String text) {
        String one = null;
        for (String raw : Bidi.strip(text == null ? "" : text).replace("\r", "").split("\n")) {
            String l = Commands.normal(raw);
            if (l.isEmpty() || l.startsWith("::") || Program.isHeader(l)) continue;
            int sp = l.indexOf(' ');
            Commands.Def d = Commands.get(sp < 0 ? l : l.substring(0, sp));
            if (d != null && d.name.equals("REM")) continue;
            if (one != null || !Verbs.isVerb(l)) return null;
            one = l;
        }
        return one;
    }

    static Map<String, String> vars(File here, String internal, String card) {
        Map<String, String> v = new LinkedHashMap<String, String>();
        v.put(V_INTERNAL, internal);
        if (card != null) v.put(V_CARD, card);
        v.put(V_DOWNLOADS, internal + "/Download");
        v.put(V_HERE, here.getAbsolutePath());
        if (card != null) v.put(V_CARD_ID, new File(card).getName());       // 1.19.0
        v.put(V_NOW, new java.text.SimpleDateFormat(NOW_FORMAT, Locale.ROOT).format(new java.util.Date()));   // 1.19.0
        v.put(V_TIME, new java.text.SimpleDateFormat("HH-mm-ss", Locale.ROOT).format(new java.util.Date()));   // מ0.37
        v.put(V_UNIQUE, Long.toString(System.currentTimeMillis(), 36));        // מ0.37: שם שלא חוזר בין הרצות
        v.put(V_ONLY_A, ONLY_A_MARK);                                         // 1.20.0
        v.put(V_ONLY_B, ONLY_B_MARK);
        v.put(V_SHARE, SHARE_MARK);                                           // 203
        v.put(V_HOME, HOME_MARK);                                             // 1.9.0
        return v;
    }

    /** מחליף משתנים, "\\" ב-"/", ומסלול יחסי — ביחס לתיקייה של הקובץ. null = משתנה לא מוכר. */
    static String path(String raw, File here, Map<String, String> vars, String[] error) {
        // מבנה 41 (ממצא 35): מסלול ריק — שגיאה. עד כאן "" פוענח כ"התיקייה של הקובץ", ו־DEL "" מחק את כל הקבצים שבה
        // (משתנה ריק או מקום פתוח שלא מולא הופכים בדיוק לזה). מי שמתכוון לתיקייה הזו — כותב "." .
        if (raw == null || raw.trim().isEmpty()) {
            error[0] = Words.Cmd.EMPTY_PATH;
            return null;
        }
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < raw.length()) {
            char c = raw.charAt(i);
            if (raw.startsWith("%~dp0", i)) {                          // 139: התיקייה של הקובץ, כמו ב-bat
                String h = vars.get(V_HERE);
                sb.append(h).append('/');
                i += 5;
                if (i < raw.length() && (raw.charAt(i) == '\\' || raw.charAt(i) == '/')) i++;
                continue;
            }
            if (c == '%') {
                int j = raw.indexOf('%', i + 1);
                if (j < 0) {
                    error[0] = Words.Cmd.MISSING_PERCENT;
                    return null;
                }
                String name = raw.substring(i + 1, j);
                String val = vars.get(name);
                if (val == null) {
                    error[0] = Words.Cmd.unknownVar(name);
                    return null;
                }
                sb.append(val);
                i = j + 1;
            } else {
                sb.append(c == '\\' ? '/' : c);
                i++;
            }
        }
        String p = sb.toString();
        if (p.trim().isEmpty()) {                                               // משתנה שהערך שלו ריק
            error[0] = Words.Cmd.EMPTY_PATH;
            return null;
        }
        while (p.length() > 1 && p.endsWith("/")) p = p.substring(0, p.length() - 1);
        if (!p.startsWith("/")) p = new File(here, p).getAbsolutePath();
        return Paths.clean(p);                                                  // מבנה 42: צורה אחת — בלי "." ו".."
    }

    /** תבנית שם בסגנון bat: * ו-? (בלי הבדל בין גדולות לקטנות). */
    static boolean wild(String pattern, String name) {
        StringBuilder re = new StringBuilder();
        for (char c : pattern.toCharArray()) {
            if (c == '*') re.append(".*");
            else if (c == '?') re.append('.');
            else re.append(Pattern.quote(String.valueOf(c)));
        }
        String p = re.toString();
        if (pattern.equals("*.*")) p = ".*";                   // כמו ב-bat: *.* — כל הקבצים, גם בלי סיומת
        return Pattern.compile(p, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(name).matches();
    }

    // ---------- סינון (תאריך וגודל) — אותם תנאים של החיפוש ----------

    static final class Filter {
        final List<Cond.Test> tests = new ArrayList<Cond.Test>();
        /** מ0.40: /מכיל:<טקסט> — רק קבצים שהטקסט כתוב בהם. המשפך: נבדק אחרון, ורק במי שעבר את השאר (גודל, תאריך). */
        final List<byte[]> contains = new ArrayList<byte[]>();
        String error;

        /** אין סינון בכלל. */
        boolean none() {
            return tests.isEmpty() && contains.isEmpty();
        }

        boolean ok(File f) {
            if (!tests.isEmpty()) {
                Index.Rec r = new Index.Rec();
                r.name = f.getName();
                r.size = f.length();
                r.mtime = f.lastModified();
                for (Cond.Test t : tests) if (!t.ok(null, r)) return false;
            }
            for (byte[] w : contains) if (f.exists() && !Files.contains(f, w)) return false;   // עוד אין (שורה קודמת יוצרת) — נבדק בהרצה
            return true;
        }
    }

    static Filter filter(List<String> switches) {
        Filter f = new Filter();
        for (String sw : switches) {
            String s = sw.substring(1);
            String up = s.toUpperCase(Locale.ROOT);
            Cond.Test t = null;
            boolean known = true;
            if (s.startsWith(SW_CONTAINS)) {                                  // מ0.40
                String v = s.substring(SW_CONTAINS.length()).trim();
                if (v.length() >= 2 && v.startsWith("\"") && v.endsWith("\"")) v = v.substring(1, v.length() - 1);
                if (v.isEmpty()) {
                    f.error = Words.Cmd.CONTAINS_USAGE;
                    return f;
                }
                try {
                    f.contains.add(v.getBytes("UTF-8"));
                } catch (java.io.UnsupportedEncodingException e) {
                    f.error = Words.Cmd.CONTAINS_USAGE;
                    return f;
                }
                continue;
            }
            if (s.startsWith(SW_DATE)) t = Cond.date(s.substring(SW_DATE.length()));
            else if (s.startsWith(SW_SIZE)) t = Cond.size(s.substring(SW_SIZE.length()));
            else if (up.startsWith("MAXAGE:")) t = Cond.date(s.substring(7).trim() + "d");
            else if (up.startsWith("MINAGE:")) t = Cond.date("<" + s.substring(7).trim() + "d");
            else if (up.startsWith("MAX:")) t = Cond.size("<" + (parseLong(s.substring(4)) + 1));
            else if (up.startsWith("MIN:")) t = Cond.size(">" + (parseLong(s.substring(4)) - 1));
            else if (up.startsWith("D:")) t = Cond.date(">=" + s.substring(2));
            else known = false;
            if (known && t == null) {
                f.error = Words.Cmd.switchUnknown(sw);
                return f;
            }
            if (t != null) f.tests.add(t);
        }
        return f;
    }

    private static long parseLong(String s) {
        try {
            return Long.parseLong(s.trim());
        } catch (Throwable t) {
            return Long.MIN_VALUE / 2;
        }
    }

    // ---------- תכנון ----------

    /** מ0.27: יעד שנמחק בשורה קודמת (RD) — ההשוואה מולו היא מול תיקייה ריקה, לא מול מה שעוד בכונן בזמן התכנון. */
    private static final File GHOST = new File("/\u0000none");

    /** מתכנן את כל הקובץ, בלי לשנות דבר. */
    /**
     * מה שהשורות הקודמות ייצרו או יזיזו — כדי שהתכנון יכיר אותן לפני שבוצעו (118): תיקייה מ-MD,
     * קובץ שהועבר או הועתק ליעד, שם חדש מ-REN, ומה שנמחק או הועבר ממקומו.
     */
    static final class Sim {
        final java.util.Set<String> dirs = new java.util.HashSet<String>();
        final java.util.Set<String> gone = new java.util.HashSet<String>();
        /**
         * מ0.27: תיקיות ש־RD מחק בשורה קודמת — כל מה שהיה בהן בכונן כבר לא נחשב (גם אם התיקייה נוצרת שוב ב־MD):
         * רק מה שהשורות שאחרי יוצרות. בלי זה MD אחרי RD /S נחשב "כבר קיימת" ולא רץ, ו־COMP השווה מול מה שנמחק.
         */
        final java.util.Set<String> wiped = new java.util.HashSet<String>();

        boolean underWiped(String p) {
            for (String w : wiped) if (p.equals(w) || p.startsWith(w + "/")) return true;
            return false;
        }

        /** מ0.32 (ממצא 44): יש בתיקייה תת־תיקייה — לפי ההדמיה (מה ששורות קודמות מחקו או יצרו), לא רק לפי הכונן. */
        /** מ0.38: התיקיות שבתוך d — בכונן, ומה ששורות קודמות יצרו; בלי מה שנמחק. לפי השם. */
        List<File> subdirs(File d) {
            java.util.TreeMap<String, File> out = new java.util.TreeMap<String, File>();
            String dp = d.getAbsolutePath();
            File[] k = underWiped(dp) ? null : d.listFiles();
            if (k != null) for (File x : k) if (x.isDirectory() && !gone.contains(x.getAbsolutePath())) out.put(x.getName(), x);
            String in = dp + "/";
            for (String x : dirs) if (x.startsWith(in) && x.indexOf('/', in.length()) < 0 && !gone.contains(x)) out.put(x.substring(in.length()), new File(x));
            return new ArrayList<File>(out.values());
        }

        boolean hasSubdirs(File d) {
            String in = d.getAbsolutePath() + "/";
            for (String x : dirs) if (x.startsWith(in) && x.indexOf('/', in.length()) < 0) return true;
            File[] k = d.listFiles();
            if (k != null) for (File x : k) {
                String p = x.getAbsolutePath();
                if (x.isDirectory() && !gone.contains(p) && !underWiped(p)) return true;
            }
            return false;
        }

        /** RD: התיקייה וכל מה שבתוכה — יוצאים מההדמיה. */
        void wipe(String d) {
            wiped.add(d);
            gone.add(d);
            String in = d + "/";
            for (java.util.Iterator<String> it = dirs.iterator(); it.hasNext(); ) {
                String x = it.next();
                if (x.equals(d) || x.startsWith(in)) it.remove();
            }
            for (java.util.Iterator<String> it = added.keySet().iterator(); it.hasNext(); ) if (it.next().startsWith(in)) it.remove();
        }

        /** קובץ שיהיה ← הקובץ שממנו הוא בא (לגודל ולתאריך). */
        final Map<String, File> added = new LinkedHashMap<String, File>();
        /** 1.20.0: ההשוואה האחרונה (COMP), והאינדקס לחתימות שכבר חושבו (יכול להיות null). */
        Compare.Result compare;
        Library lib;
        /** מ0.12: כשעובדים במסד — עותק של המסד בזיכרון (DbListCmd), והקבצים לכל תנאי; null — עוד לא נטען. לא נשמר. */
        Store db;
        /** מ0.32 (ממצא 38): עותק המסד של כל אופן שכבר טופל בקובץ — חזרה לאופן לא שוכחת מה שהשורות הקודמות עשו בו. */
        final java.util.Map<String, Store> dbs = new java.util.HashMap<String, Store>();
        /** מ0.42: האופנים ש"צור אופן" יוצר בקובץ (בהדמיה — מסד ריק עם בית ב־dbs). */
        final java.util.Set<String> created = new java.util.HashSet<String>();
        /** מ0.45: אופנים ש"מחק אופן" שלפני מחק — "אופן" אחריו נכשל בתכנון. */
        final java.util.Set<String> dropped = new java.util.HashSet<String>();

        boolean hasMode(String m) {
            return created.contains(m) || (!dropped.contains(m) && Db.dbModes(Db.app).contains(m));
        }
        /** מ0.15: האופן מ"אופן" בקובץ; null — האופן שפתוח. */
        String mode;
        /** מ0.17.1: "אופן" שנכשל — כל שורות המסד שאחריו נכשלות (לא לרוץ בטעות על אופן אחר). */
        String modeError;
        final Map<String, List<String>> condFiles = new java.util.HashMap<String, List<String>>();

        boolean isDir(File d) {
            String p = d.getAbsolutePath();
            if (gone.contains(p)) return false;
            return dirs.contains(p) || (d.isDirectory() && !underWiped(p));
        }

        boolean isFile(File f) {
            String p = f.getAbsolutePath();
            return added.containsKey(p) || (!gone.contains(p) && f.isFile() && !underWiped(p));
        }

        /** המקור האמיתי (לגודל ולתאריך). */
        File real(File f) {
            File r = added.get(f.getAbsolutePath());
            return r != null ? r : f;
        }

        /** הקבצים שיהיו בתיקייה, ממוינים. */
        List<File> files(File dir) {
            java.util.TreeMap<String, File> out = new java.util.TreeMap<String, File>();
            File[] all = underWiped(dir.getAbsolutePath()) ? null : dir.listFiles();
            if (all != null)
                for (File x : all)
                    if (x.isFile() && !gone.contains(x.getAbsolutePath())) out.put(x.getName(), x);
            String d = dir.getAbsolutePath() + "/";
            for (String p : added.keySet())
                if (p.startsWith(d) && p.indexOf('/', d.length()) < 0) out.put(p.substring(d.length()), new File(p));
            return new ArrayList<File>(out.values());
        }

        /**
         * מ0.40: האם יש בתיקייה (כמו שתהיה) קובץ זהה ל־src. המשפך: הגודל (חינם) מוציא את רובם; חתימה — רק לשווי גודל,
         * וחתימת המקור — פעם אחת. חתימה שלא נקראה — ספק, לא "זהה".
         */
        boolean sameIn(File src, File dir) {
            File s = real(src);
            long n = s.length();
            String sig = null;
            for (File g : files(dir)) {
                File r = real(g);
                if (r.length() != n) continue;
                if (sig == null && (sig = Sig.of(s)) == null) return false;
                if (sig.equals(Sig.of(r))) return true;
            }
            return false;
        }

        /** השם הפנוי הבא לקובץ to בתיקייה שלו, כמו Physical.freeName — אבל לפי ההדמיה. */
        File free(File to) {
            File dir = to.getParentFile();
            String name = to.getName(), base = Text.stem(name), ext = Text.tail(name);
            if (base.isEmpty()) { base = name; ext = ""; }
            File f = to;
            for (int i = 2; isFile(f) || isDir(f); i++) f = new File(dir, base + " (" + i + ")" + ext);
            return f;
        }

        void move(String from, File to, boolean copy) {
            File src = real(new File(from));
            if (!copy) {
                gone.add(from);
                added.remove(from);
            }
            gone.remove(to.getAbsolutePath());
            added.put(to.getAbsolutePath(), src);
            if (!copy && db != null)                                           // מ0.23: גם בהדמיה — הרשימות במסד עוקבות
                DbFollow.moved(db, java.util.Collections.singletonMap(from, to.getAbsolutePath()));
        }
    }

    /** מתכנן את כל הקובץ, בלי לשנות דבר. כל שורה מכירה את מה שהשורות שלפניה יעשו (118). */
    static Plan plan(String text, File here, Map<String, String> vars) {
        return plan(text, here, vars, null);
    }

    // ---------- 1.25.0: לולאה ----------

    /** שורה אחרי פריסת הלולאות: הטקסט (עם %סבב%), המספר בקובץ, והלולאות שסביבה. */
    static final class Ln {
        String text, error;
        int num;
        int[] loops, rounds;
    }

    /** "חזור 6" · "חזור <סבבים>" — לולאה (בלי מספר — הפועל "חזור", חזרה למסך הקודם). */
    static boolean isLoopStart(String line) {
        List<String> tk = tokens(line);
        if (tk.size() < 2 || !tk.get(0).equals(LOOP)) return false;
        char c = tk.get(1).isEmpty() ? ' ' : tk.get(1).charAt(0);
        return Character.isDigit(c) || c == '<' || c == '%';
    }

    static boolean isLoopEnd(String line) {
        return line.equals(LOOP_END);
    }

    private static String clean(String raw) {
        String line = raw.trim();
        if (line.startsWith("@")) line = line.substring(1).trim();
        return line;
    }

    /** הגבול לסבב: "30שניות", "30 שניות", "2דקות", "500ms"; -1 — לא מובן. */
    static long limitMs(String v) {
        String t = v.replace(" ", "").toLowerCase(Locale.ROOT);
        int i = 0;
        while (i < t.length() && (Character.isDigit(t.charAt(i)) || t.charAt(i) == '.')) i++;
        if (i == 0) return -1;
        double n;
        try { n = Double.parseDouble(t.substring(0, i)); } catch (NumberFormatException e) { return -1; }
        String u = t.substring(i);
        if (u.isEmpty() || u.startsWith("שנ") || u.equals("ש") || u.equals("s") || u.equals("sec")) return (long) (n * 1000);   // תחביר
        if (u.startsWith("דק") || u.equals("m") || u.equals("min")) return (long) (n * 60000);                                  // תחביר
        if (u.equals("ms") || u.startsWith("אלפ")) return (long) n;                                                               // תחביר
        return -1;
    }

    /** פורס את הלולאות: כל סבב — השורות שבתוכה, עם %סבב% = מספר הסבב (בלולאה פנימית — שלה). */
    /**
     * מ0.15: CALL "קובץ" (בעברית: כלול) — במקום השורה: הודעה "▸ כלול: …" ואחריה שורות הקובץ (גם CALL שבתוכו, עד
     * CALL_DEPTH). קובץ שלא נמצא, מעגל, או עומק — השורה נשארת כמו שהיא ונכשלת בתכנון (עם הסבר).
     */
    static String[] calls(String[] lines, File here, Map<String, String> vars, int depth, java.util.Set<String> stack) {
        List<String> out = new ArrayList<String>();
        for (String raw : lines) {
            List<String> tk = tokens(clean(raw));
            if (tk.size() != 2 || !CALL.equals(Commands.canonical(tk.get(0)))) {
                out.add(raw);
                continue;
            }
            String[] err = {null};
            String p = path(tk.get(1), here, vars, err);
            File f = p == null ? null : new File(p);
            String key = f == null ? "" : f.getAbsolutePath();
            if (f == null || !f.isFile() || depth >= CALL_DEPTH || stack.contains(key)) {
                out.add(raw);
                continue;
            }
            try {
                String[] inner = Bidi.strip(Files.readUtf8(f)).replace("\r", "").split("\n");
                stack.add(key);
                String[] sub = calls(inner, f.getParentFile(), vars, depth + 1, stack);
                stack.remove(key);
                List<String> keep = new ArrayList<String>();
                for (String x : sub) {
                    String c = clean(x);
                    if (c.equals(CHECK) || c.equals(AS_IF) || c.equals(AS_IF2)) continue;   // שורות כותרת — רק של הקובץ הראשי
                    keep.add(x);
                }
                out.add("ECHO " + Words.DbCmd.called(f.getName(), keep.size()));
                out.addAll(keep);
            } catch (Throwable t) {
                Log.e("Batch.calls", t);
                out.add(raw);
            }
        }
        return out.toArray(new String[0]);
    }

    static List<Ln> expand(String[] lines, Plan plan) {
        List<Ln> out = new ArrayList<Ln>();
        expand(lines, 0, lines.length, new int[0], new int[0], null, plan, out);
        return out;
    }

    private static void expand(String[] lines, int from, int to, int[] loops, int[] rounds, String round, Plan plan, List<Ln> out) {
        for (int i = from; i < to; i++) {
            if (out.size() > MAX_LINES) return;
            String line = clean(lines[i]);
            if (isLoopStart(line)) {
                int depth = 0, end = -1;
                for (int j = i + 1; j < to && end < 0; j++) {
                    String x = clean(lines[j]);
                    if (isLoopStart(x)) depth++;
                    else if (isLoopEnd(x)) {
                        if (depth == 0) end = j;
                        else depth--;
                    }
                }
                List<String> tk = tokens(round == null ? line : line.replace(var(V_ROUND), round));
                int n = -1;
                long limit = 0;
                String err = null;
                try { n = Integer.parseInt(tk.get(1)); } catch (NumberFormatException e) { err = Words.Loop.NEED_NUMBER; }
                for (int k = 2; k < tk.size() && err == null; k++) {
                    String x = tk.get(k);
                    if (x.equals(Words.Loop.TIMES_WORD)) continue;
                    if (x.startsWith(SW_LIMIT)) {
                        String v = x.substring(SW_LIMIT.length());
                        if (k + 1 < tk.size() && !tk.get(k + 1).isEmpty() && !Character.isDigit(tk.get(k + 1).charAt(0)) && !tk.get(k + 1).startsWith("/")
                                && limitMs(v + tk.get(k + 1)) > 0) v += tk.get(++k);                 // "/עד:30 שניות"
                        limit = limitMs(v);
                        if (limit <= 0) err = Words.Loop.BAD_LIMIT;
                    } else err = Words.Loop.USAGE;
                }
                if (err == null && (n < 1 || n > MAX_ROUNDS)) err = Words.Loop.tooMany(MAX_ROUNDS);
                if (err == null && end < 0) err = Words.Loop.NO_END;
                if (err != null) {
                    Ln e = new Ln();
                    e.num = i + 1;
                    e.text = line;
                    e.error = err;
                    out.add(e);
                    if (end < 0) continue;
                    i = end;
                    continue;
                }
                int id = plan.loopLimit.size();
                plan.loopLimit.add(limit);
                plan.loopLine.add(i + 1);
                int[] l2 = java.util.Arrays.copyOf(loops, loops.length + 1), r2 = java.util.Arrays.copyOf(rounds, rounds.length + 1);
                l2[loops.length] = id;
                for (int r = 1; r <= n && out.size() <= MAX_LINES; r++) {
                    r2 = r2.clone();
                    r2[rounds.length] = r;
                    expand(lines, i + 1, end, l2, r2, String.valueOf(r), plan, out);
                }
                i = end;
                continue;
            }
            Ln x = new Ln();
            x.num = i + 1;
            x.text = round == null ? lines[i] : lines[i].replace(var(V_ROUND), round);
            if (isLoopEnd(line)) x.error = Words.Loop.END_WITHOUT_START;
            if (loops.length > 0) {
                x.loops = loops;
                x.rounds = rounds;
            }
            out.add(x);
        }
        if (out.size() > MAX_LINES && (out.isEmpty() || out.get(out.size() - 1).error == null)) {
            Ln e = new Ln();
            e.num = from + 1;
            e.text = "";
            e.error = Words.Loop.tooLong(MAX_LINES);
            out.add(e);
        }
    }

    /**
     * עצירה לפי זמן: מעבר לסבב חדש בלולאה עם /עד: — אם הסבב הקודם עבר את הגבול, הלולאה נעצרת. מחזיר את מזהה
     * הלולאה שנעצרה כעת (כדי לכתוב פעם אחת), או -1.
     */
    static final class Clock {
        final Map<Integer, Integer> round = new java.util.HashMap<Integer, Integer>();
        final Map<Integer, Long> start = new java.util.HashMap<Integer, Long>();
        final java.util.Set<Integer> stopped = new java.util.HashSet<Integer>();
        final List<String> notes = new ArrayList<String>();

        /** true — השורה מדולגת (לולאה שסביבה נעצרה). */
        boolean skip(Plan plan, int[] loops, int[] rounds) {
            if (loops == null) return false;
            long now = System.nanoTime();
            for (int k = 0; k < loops.length; k++) {
                int id = loops[k];
                if (stopped.contains(id)) return true;
                Integer cur = round.get(id);
                if (cur == null || cur != rounds[k]) {
                    if (cur != null) {
                        long took = (now - start.get(id)) / 1000000;
                        long lim = plan.loopLimit.get(id);
                        if (lim > 0 && took > lim) {
                            stopped.add(id);
                            notes.add(Words.Loop.stopped(plan.loopLine.get(id), cur, took));
                            return true;
                        }
                    }
                    round.put(id, rounds[k]);
                    start.put(id, now);
                    for (int m = k + 1; m < loops.length; m++) {          // לולאות פנימיות — מתחילות מחדש
                        round.remove(loops[m]);
                        stopped.remove(loops[m]);
                    }
                }
            }
            return false;
        }
    }

    /** 1.20.0: עם האינדקס — ההשוואה לוקחת ממנו חתימות שכבר חושבו. */
    static Plan plan(String text, File here, Map<String, String> vars, Library lib) {
        return plan(text, here, vars, lib, null);
    }

    /** 1.25.4: עם עצירה — ההכנה עצמה יכולה לקחת זמן (לולאות, השוואות של רשימות גדולות). */
    static Plan plan(String text, File here, Map<String, String> vars, Library lib, Physical.Progress progress) {
        Plan plan = new Plan();
        Sim sim = new Sim();
        sim.lib = lib;
        plan.sim = sim;
        String[] lines = Bidi.strip(text).replace("\r", "").split("\n");          // 204
        lines = calls(lines, here, vars, 0, new java.util.HashSet<String>());  // מ0.15: CALL — השורות של הקובץ במקומה
        List<Ln> all = expand(lines, plan);                                    // 1.25.0: לולאות
        Clock clock = new Clock();
        int mark = 0;
        Ln prev = null;
        long t0 = System.nanoTime();
        for (int li = 0; li <= all.size(); li++) {
            for (int k = mark; k < plan.steps.size(); k++) {                   // 1.25.0: השורות שנוספו — מהשורה הקודמת
                Step x = plan.steps.get(k);
                x.loops = prev.loops;
                x.rounds = prev.rounds;
                if (plan.measure) x.ms = (System.nanoTime() - t0) / 1000000;
            }
            mark = plan.steps.size();
            t0 = System.nanoTime();
            if (li == all.size()) break;
            if (progress != null && progress.stopped()) {                      // 1.25.4: עצירה בהכנה
                plan.stopped = true;
                break;
            }
            if (progress != null) progress.step(li + 1, all.size(), 0, 0);    // 1.26.0: "בודק שורה X מתוך Y"
            Ln ln = all.get(li);
            prev = ln;
            if (clock.skip(plan, ln.loops, ln.rounds)) continue;               // לולאה שעברה את הגבול — גם בתכנון
            int n = ln.num - 1;
            final String shown = clean(ln.text);                                 // מ0.38: מה שנכתב (לדוח) — גם כשהוא בעברית
            String line = Commands.normal(shown);
            if (ln.error != null) {
                Step s = new Step();
                s.line = ln.num;
                s.text = shown;
                s.cmd = LOOP;
                s.error = ln.error;
                plan.errors++;
                plan.steps.add(s);
                continue;
            }
            if (line.isEmpty() || line.startsWith("::")) continue;
            if (line.equals(AS_IF) || line.equals(AS_IF2)) {                  // 1.10.0: שורת כותרת — כל הקובץ בהדמיה
                plan.asIf = true;
                continue;
            }
            if (line.equals(CHECK)) continue;                                  // 1.28.0: שורת כותרת — מסך ההרצה (BatchUi)
            if (line.equals(MEASURE) || line.startsWith(MEASURE + " ")) {     // 1.25.0: שורת כותרת — מדידה
                plan.measure = true;
                List<String> mt = tokens(line);
                for (int k = 0; k < mt.size(); k++) {
                    String x = mt.get(k);
                    if (x.startsWith(SW_JOURNAL)) {
                        String[] err = {null};
                        plan.measureJournal = path(x.substring(SW_JOURNAL.length()).trim(), here, vars, err);
                    } else if (x.startsWith(SW_LIMIT)) {                         // מ0.45: מגבלת זמן לכל שורה
                        String v = x.substring(SW_LIMIT.length());
                        if (k + 1 < mt.size() && !mt.get(k + 1).isEmpty() && !Character.isDigit(mt.get(k + 1).charAt(0))
                                && !mt.get(k + 1).startsWith("/") && limitMs(v + mt.get(k + 1)) > 0) v += mt.get(++k);   // "/עד:2 דקות"
                        plan.lineLimit = limitMs(v);
                        if (plan.lineLimit <= 0) {
                            plan.lineLimit = 0;
                            Step s = new Step();
                            s.line = n + 1;
                            s.text = shown;
                            s.cmd = MEASURE;
                            s.error = Words.Loop.LIMIT_USAGE;
                            plan.errors++;
                            plan.steps.add(s);
                        }
                    }
                }
                continue;
            }
            if (Program.isHeader(line)) continue;                             // 1.13.0: צלמית / שם / למה — כותרת התוכנה
            if (isLabLine(line)) {                                           // מ0.38: הוסף/החלף מעבדה (לפני הפועל "הוסף"); מ0.40: ירדו — שגיאה עם הסבר
                Step s = new Step();
                s.line = n + 1;
                s.text = shown;
                s.cmd = LAB;
                planLabLine(s);
                if (s.error != null) plan.errors++;
                plan.steps.add(s);
                continue;
            }
            if (Verbs.isVerb(line)) {                                         // 1.13.0: פקודות הפתיחה בעברית — נפתחות בסוף
                Step s = new Step();
                s.line = n + 1;
                s.text = shown;
                s.cmd = OPEN;
                s.open = Verbs.withVars(line, vars);
                Verbs.Cmd vc = Verbs.parse(s.open);
                s.error = vc.error;
                s.describe = Verbs.describe(vc);
                String doneIn = null;
                if (s.error == null && Verbs.K_MODE.equals(vc.kind)) DbListCmd.planMode(s, vc.arg, sim);   // מ0.42: צור אופן
                else if (s.error == null && DbVerbs.active()) doneIn = DbListCmd.planVerb(s, vc, sim);   // מ0.25: החלק הכותב — בסדר השורות
                // מ0.30 (ממצא 39): "פתח רשימה / רשימה חכמה" אחרי "אופן X" — נפתחת באופן של הקובץ, לא באופן שפתוח במסך
                if (s.error == null && doneIn == null && sim.mode != null && Verbs.OPEN.equals(vc.verb) && vc.arg != null && !vc.arg.trim().isEmpty()
                        && (Verbs.K_LIST.equals(vc.kind) || Verbs.K_SMART.equals(vc.kind))) doneIn = sim.mode;
                if (s.error != null) plan.errors++;
                else if (!Verbs.K_NOTE.equals(vc.kind) && !Verbs.K_TITLE.equals(vc.kind) && !Verbs.K_MODE.equals(vc.kind) && !vc.noOpen) {   // מ0.37: רעיון / כותרת; מ0.40: /בלי-לפתוח; מ0.42: אופן
                    plan.open = s.open;
                    plan.opens.add(new String[]{s.open, doneIn});
                }
                plan.steps.add(s);
                continue;
            }
            List<String> tk = tokens(line);
            if (tk.isEmpty()) continue;
            String cmd = Commands.canonical(tk.get(0));                       // 1.22.5: שמות נרדפים — מההגדרה האחת
            if (cmd.equals("REM")) continue;
            Step s = new Step();
            s.line = n + 1;
            s.text = shown;
            if (cmd.equals("ECHO.")) cmd = "ECHO";                   // 139: שורה ריקה, כמו ב-bat
            s.cmd = cmd;
            if (cmd.equals("ECHO")) {
                int w0 = 0;                                                      // מ0.38: אחרי המילה הראשונה (ECHO או "הודעה")
                while (w0 < line.length() && !Character.isWhitespace(line.charAt(w0))) w0++;
                String rest = cmd.equals("ECHO") && tk.get(0).toUpperCase(Locale.ROOT).startsWith("ECHO.") ? line.substring(4).trim()
                        : line.substring(w0).trim();
                if (rest.equalsIgnoreCase("OFF") || rest.equalsIgnoreCase("ON")) continue;
                rest = Verbs.withVars(rest, vars);                               // מ0.30 (ממצא 46): גם ב־ECHO המשתנים מוחלפים
                plan.echoes.add(rest.startsWith(".") ? rest.substring(1) : rest);
                s.describe = Words.Cmd.echo(rest);
                plan.steps.add(s);
                continue;
            }
            if (cmd.equals(CALL)) {                                             // מ0.15: לא נכלל (לא נמצא / מעגל / עומק)
                s.error = Words.DbCmd.CALL_FAILED;
                plan.errors++;
                plan.steps.add(s);
                continue;
            }
            if (cmd.equals(CONT)) {                                             // מ0.30: המשך [N] — הקובץ רץ גם כשיש שורות שגיאה
                int allow = allowOf(line);
                if (allow < 0) {
                    s.error = Words.DbCmd.CONT_USAGE;
                    plan.errors++;
                } else {
                    plan.allowErrors = allow;
                    s.noop = true;
                    s.describe = allow == Integer.MAX_VALUE ? Words.DbCmd.CONT_ALWAYS : Words.DbCmd.contUpTo(allow);

                }
                plan.steps.add(s);
                continue;
            }
            if (small(plan, s, cmd, tk, here, vars, sim)) {                // מ0.37: הפקודות הקטנות
                if (s.error != null) plan.errors++;
                plan.steps.add(s);
                continue;
            }
            if (cmd.equals(MODE)) {                                             // מ0.15: אופן — מכאן והלאה, במסד
                String m = tk.size() == 2 ? tk.get(1).trim() : "";
                if (m.isEmpty()) s.error = Words.DbCmd.MODE_USAGE;
                else if (!DbVerbs.active()) s.error = Words.DbCmd.VERIFY_DB_ONLY;
                else if (!sim.hasMode(m))   // מ0.42: גם מה ש"צור אופן" שלפניו יצר; מ0.45: בלי מה ש"מחק אופן" מחק
                    s.error = sim.modeError = Words.DbCmd.noMode(m, Db.dbModes(Db.app));
                else {
                    sim.dbs.put(sim.mode == null ? "" : sim.mode, sim.db);          // מ0.32: עותק לכל אופן
                    sim.mode = m;
                    sim.modeError = null;
                    sim.db = sim.dbs.get(m);
                    plan.modeFrom.add(plan.steps.size());
                    plan.modeName.add(m);
                    s.noop = true;
                    s.describe = Words.DbCmd.modeNow(m);
                }
                if (s.error != null) plan.errors++;
                plan.steps.add(s);
                continue;
            }
            List<String> args = new ArrayList<String>(), sw = new ArrayList<String>();
            List<String> lsw = new ArrayList<String>();
            ListCmd.Parsed lp = ListCmd.parse(line, lsw);                      // 1.24.0: מילות סוג — רשימות
            if (lp != null && lp.error == null && lp.anyList()) {
                planList(s, cmd, lp, lsw, here, vars, sim);
                if (s.error != null) plan.errors++;
                plan.steps.add(s);
                continue;
            }
            if (lp == null && cmd.equals(VERIFY) && tk.size() >= 2) {          // מ0.18: ודא "קובץ" קיים — בלי מילת סוג
                List<String> conds = new ArrayList<String>(tk.subList(2, tk.size()));
                String[] err = {null};
                String p = path(tk.get(1), here, vars, err);
                if (p == null) s.error = err[0];
                else DbListCmd.planVerifyFile(s, new File(p), conds);
                if (s.error != null) plan.errors++;
                plan.steps.add(s);
                continue;
            }
            if (lp != null && lp.error != null) {
                s.error = lp.error;
                plan.errors++;
                plan.steps.add(s);
                continue;
            }
            if (lp != null) {                                                  // רק "תיקייה" — שורה רגילה בלי המילים
                sw.addAll(lsw);
                for (ListCmd.Side x : lp.sides) args.add(x.raw);
            } else
            for (int i = 1; i < tk.size(); i++) (isSwitch(tk.get(i)) ? sw : args).add(tk.get(i));
            String[] err = {null};
            List<String> paths = new ArrayList<String>();
            for (String a : args) {
                String p = path(a, here, vars, err);
                if (p == null) break;
                paths.add(p);
            }
            if (err[0] == null)                                                // 1.19.0: /יומן:<קובץ>
                for (String x : sw) {
                    if (!x.startsWith(SW_JOURNAL)) continue;
                    String v = x.substring(SW_JOURNAL.length()).trim();
                    if (v.isEmpty() || !(cmd.equals("COPY") || cmd.equals("MOVE"))) {
                        err[0] = Words.Cmd.JOURNAL_USAGE;
                        break;
                    }
                    s.journal = path(v, here, vars, err);
                    if (s.journal == null) break;
                }
            if (err[0] == null) err[0] = setsMisuse(cmd, paths);            // 1.20.0
            if (err[0] == null) err[0] = shareMisuse(cmd, paths);           // 203
            if (err[0] == null) err[0] = homeMisuse(cmd, paths);            // 1.9.0
            if (err[0] != null) s.error = err[0];
            else plan(s, cmd, args, paths, sw, sim);
            if (s.error != null) plan.errors++;
            if (cmd.equals("START") && s.error == null) plan.start = s.dest;
            plan.steps.add(s);
        }
        expectErrors(plan);                                                    // מ0.37: "ודא שגיאה"
        return plan;
    }

    /** 1.24.0: שורה עם רשימה — הצד שבכונן מפוענח כאן (מסלול, תבנית, סינון), והשאר — ListCmd. */
    private static void planList(Step s, String cmd, ListCmd.Parsed lp, List<String> sw, File here, Map<String, String> vars, Sim sim) {
        if (cmd.equals(VERIFY)) {                                               // מ0.14: ודא — הצד הראשון רשימה, השאר תנאים
            if (!DbVerbs.active()) { s.error = Words.DbCmd.VERIFY_DB_ONLY; return; }
            List<String> conds = new ArrayList<String>(sw);
            for (int i = 1; i < lp.sides.size(); i++) conds.add(lp.sides.get(i).raw);
            ListCmd.Side a = lp.sides.get(0);
            if (a.list()) ListCmd.resolve(a, vars);
            else {                                                              // מ0.18: ודא "קובץ" קיים / חסר — בכונן
                String[] err = {null};
                String p = path(a.raw, here, vars, err);
                if (p == null) { s.error = err[0]; return; }
                DbListCmd.planVerifyFile(s, new File(p), conds);
                return;
            }
            DbListCmd.planVerify(s, a, conds, sim);
            return;
        }
        String bad = listSwitch(cmd, lp, sw);                                 // מ0.40 (ממצא 41): מתג שלא שייך לשורה — שגיאה
        if (bad != null) { s.error = bad; return; }
        List<String> fsFiles = new ArrayList<String>();
        File fsDest = null;
        for (int i = 0; i < lp.sides.size(); i++) {
            ListCmd.Side x = lp.sides.get(i);
            if (x.list()) {
                ListCmd.resolve(x, vars);
                continue;
            }
            String[] err = {null};
            String p = path(x.raw, here, vars, err);
            if (p == null) { s.error = err[0]; return; }
            if (i == lp.sides.size() - 1 && i > 0) fsDest = new File(p);
            else {
                Filter flt = filter(sw);
                if (flt.error != null) { s.error = flt.error; return; }
                match(s, p, flt, false, sim);
                if (s.error != null) return;
                if (!s.dirs.isEmpty()) { s.error = Words.ListCmdW.FOLDER_TO_LIST; return; }
                fsFiles.addAll(s.files);
                s.files.clear();
            }
        }
        if (DbVerbs.active()) DbListCmd.plan(s, cmd, lp, sw, sim, fsFiles, fsDest);   // מ0.12: עובדים במסד
        else s.error = Words.DbCmd.NO_DB;                                    // מ0.26: המנוע הישן נמחק — רשימות רק במסד
    }

    /**
     * מ0.40 (ממצא 41): המתגים שמותרים בשורה עם רשימה — לפי צורת השורה. הקבצים של רשימה אל תיקייה (העתק / העבר): /עמוק,
     * /תפוס:…, /מכיל:; מהכונן אל רשימה: /מכיל:; השוואה: /תנאים. בכל שורה אחרת — אין מתגים. null — תקין.
     */
    static String listSwitch(String cmd, ListCmd.Parsed lp, List<String> sw) {
        List<ListCmd.Side> sd = lp.sides;
        boolean two = sd.size() == 2, transfer = cmd.equals("COPY") || cmd.equals("MOVE");
        boolean toFolder = transfer && two && sd.get(0).list() && !sd.get(1).list() && sd.get(1).kind == ListCmd.FS;
        boolean fromDisk = transfer && two && !sd.get(0).list();
        for (String x : sw) {
            String u = x.toUpperCase(Locale.ROOT);
            if (u.equals("/Q") || u.equals("/F")) continue;
            if (x.startsWith("/" + SW_DATE) || x.startsWith("/" + SW_SIZE)) return Words.ListCmdW.NO_FILTER;
            if (x.startsWith("/" + SW_CONTAINS)) { if (toFolder || fromDisk) continue; return Words.ListCmdW.NO_FILTER; }
            if (x.equals(ListCmd.SW_CONDS)) { if (cmd.equals("COMP")) continue; return Words.Cmd.switchNotHere(x, null); }
            boolean take = u.equals("/S") || x.equals(SW_KEEP) || u.equals("/Y") || u.equals("/-Y") || x.startsWith(SW_TAKEN);
            if (take && toFolder) continue;
            return Words.Cmd.switchNotHere(Commands.switchWord(x), null);
        }
        return null;
    }

    private static void plan(Step s, String cmd, List<String> args, List<String> paths, List<String> sw, Sim sim) {
        Filter flt = filter(sw);
        if (flt.error != null) {
            s.error = flt.error;
            return;
        }
        if (!flt.contains.isEmpty()) s.contains = flt.contains;               // מ0.40
        // מ0.40 (ממצא 41; הכלל של המשתמש: "אין מצב שיש שגיאה והקובץ רץ ואף אחד לא יודע"): מתג שלא שייך לפקודה — שגיאה,
        // ולא בליעה בשקט. מה מותר — מההגדרה האחת (Commands). /Q ו־/F — תאימות ל־bat, מתעלמים בכוונה.
        Commands.Def def = Commands.get(cmd);
        boolean fromCmp = (cmd.equals("COPY") || cmd.equals("MOVE")) && paths.size() == 2
                && (paths.get(0).equals(ONLY_A_MARK) || paths.get(0).equals(ONLY_B_MARK));
        if (def != null) for (String x : sw) {
            String u = x.toUpperCase(Locale.ROOT);
            if (u.equals("/Q") || u.equals("/F")) continue;
            boolean ok = false;
            for (Commands.Param p : def.params) if (p.matches(x)) ok = true;
            if (!ok) { s.error = Words.Cmd.switchNotHere(Commands.switchWord(x), Commands.word(def)); return; }
            if (x.startsWith(SW_LIMIT) && !fromCmp) { s.error = Words.Cmd.LIMIT_ONLY_COMPARE; return; }
        }
        if (fromCmp && !flt.none()) { s.error = Words.Cmd.NO_FILTER_ON_COMPARE; return; }
        for (String x : sw) {
            String u = x.toUpperCase(Locale.ROOT);
            if (u.equals("/Y") || x.equals(SW_TAKEN + TAKEN_REPLACE)) s.replace = true;
            if (x.equals(SW_TAKEN + TAKEN_SKIP)) s.skipTaken = true;
            if (x.equals(SW_TAKEN + TAKEN_STOP)) s.failTaken = true;          // 1.20.0
            if (x.equals(SW_TAKEN + TAKEN_RENAME)) s.renameTaken = true;      // מ0.40
            if (u.equals("/S") || x.equals(SW_KEEP)) s.recursive = true;
            if (u.equals("/A") || u.startsWith("/A:H") || u.equals("/A:")) s.hidden = true;
            if (x.startsWith(SW_TAKEN) && !takenValue(x)) s.error = takenUnknown(x);
            if (x.startsWith(SW_LIMIT) && limit(x) < 0) s.error = Words.Cmd.switchUnknown(x);
            if (x.startsWith(SW_LIST)) {                                      // 1.22.0
                s.listName = x.substring(SW_LIST.length()).trim();
                if (s.listName.isEmpty() || !cmd.equals("COMP")) s.error = Words.Cmd.LIST_ONLY_COMP;
            }
            if (x.startsWith(SW_STATE)) {                                     // 1.21.0
                s.stateWhen = State.when(x.substring(SW_STATE.length()));
                if (s.stateWhen == Long.MIN_VALUE) s.error = Words.Cmd.switchUnknown(x);
                else if (!cmd.equals("COPY")) s.error = Words.Cmd.STATE_ONLY_COPY;
            }
        }
        if (s.error != null) return;
        if (cmd.equals("MD")) {
            if (paths.size() != 1) { s.error = Commands.usage(cmd); return; }
            File d = new File(paths.get(0));
            if (sim.isDir(d)) {                                                // קיימת — בלי שינוי
                s.noop = true;
                s.dest = d;
                s.describe = Words.Cmd.folderExists(d.getName());
                return;
            }
            if (sim.isFile(d)) { s.error = Words.Cmd.exists(d.getName()); return; }
            s.dest = d;
            s.describe = Words.Cmd.newFolder(Bidi.path(d.getAbsolutePath()));
            sim.dirs.add(d.getAbsolutePath());
            sim.gone.remove(d.getAbsolutePath());                                // מ0.30 (ממצא 35): RD ואחריו MD לאותה תיקייה — היא קיימת
            for (File up = d.getParentFile(); up != null && !sim.isDir(up); up = up.getParentFile()) {   // מ0.24: כמו mkdirs —
                sim.dirs.add(up.getAbsolutePath());                                                    // גם התיקיות שמעל
                sim.gone.remove(up.getAbsolutePath());
            }
        } else if (cmd.equals("RD")) {
            if (paths.size() != 1) { s.error = Commands.usage(cmd); return; }
            File d = new File(paths.get(0));
            if (!sim.isDir(d)) { s.error = Words.FOLDER_NOT_FOUND + ": " + args.get(0); return; }
            if (!s.recursive && (!sim.files(d).isEmpty() || sim.hasSubdirs(d))) {
                s.error = Words.Cmd.FOLDER_NOT_EMPTY;
                return;
            }
            s.dirs.add(d.getAbsolutePath());
            s.describe = Words.Cmd.folderToTrash(Bidi.path(d.getAbsolutePath()));
            sim.wipe(d.getAbsolutePath());
        } else if (cmd.equals("DEL")) {
            if (paths.size() != 1) { s.error = Commands.usage(cmd); return; }
            if (s.recursive) { s.error = Words.Cmd.DEL_S; return; }
            match(s, paths.get(0), flt, false, sim);
            if (s.error == null) s.describe = Words.Cmd.toTrash(Words.files(s.files.size())) + filtered(s);
            for (String f : s.files) {
                sim.gone.add(f);
                sim.added.remove(f);
            }
        } else if (cmd.equals("COPY") && paths.size() == 2 && paths.get(1).equals(SHARE_MARK)) {   // 203: לשיתוף
            s.share = true;
            match(s, paths.get(0), flt, true, sim);                          // תיקייה — כמו שהיא (תיארז בשמה)
            if (s.error != null) return;
            if (!s.dirs.isEmpty() && !flt.none()) {
                s.error = Words.Cmd.filterOnFolder(cmd + " \"" + args.get(0) + "\\*.*\" …");
                return;
            }
            List<String> what = new ArrayList<String>();
            if (!s.files.isEmpty() || s.dirs.isEmpty()) what.add(Words.files(s.files.size()));
            if (!s.dirs.isEmpty()) what.add(Words.folders(s.dirs.size()));
            s.describe = Words.Cmd.toShare(join(what, Words.Cmd.AND)) + filtered(s);
        } else if (cmd.equals("COPY") && s.stateWhen != Long.MIN_VALUE) {         // 1.21.0: שחזור
            restore(s, args, paths, flt, sim);
        } else if (cmd.equals("COMP")) {                                      // 1.20.0
            comp(s, args, paths, sim);
        } else if ((cmd.equals("COPY") || cmd.equals("MOVE")) && paths.size() == 2
                && (paths.get(0).equals(ONLY_A_MARK) || paths.get(0).equals(ONLY_B_MARK))) {
            fromCompare(s, cmd, paths, sw, sim);
        } else if ((cmd.equals("COPY") || cmd.equals("MOVE")) && s.recursive) {     // 1.19.0: שמירת מסלול
            keep(s, cmd, args, paths, flt, sim);
        } else if (cmd.equals("COPY") || cmd.equals("MOVE")) {
            if (paths.size() != 2) { s.error = Commands.usage(cmd); return; }
            File dest = new File(paths.get(1));
            if (!sim.isDir(dest)) {
                s.error = Words.Cmd.NO_DEST;
                return;
            }
            s.dest = dest;
            match(s, paths.get(0), flt, cmd.equals("MOVE"), sim);
            if (s.error != null) return;
            if (!s.dirs.isEmpty() && !flt.none()) {            // 121: סינון לא חל על תיקייה שלמה
                s.error = Words.Cmd.filterOnFolder(cmd + " \"" + args.get(0) + "\\*.*\" …");
                return;
            }
            boolean copy = cmd.equals("COPY");
            // מ0.40: העברה אל התיקייה שהפריט כבר בה — כלום (עד כאן: "קובץ אחד" בתכנון, ו"כבר בתיקייה הזו" ככישלון בהרצה).
            // ובקובץ אחרי קובץ — כדי שקובץ שני זהה לראשון יראה אותו כבר ביעד (לשנות-שם).
            for (java.util.Iterator<String> it = s.files.iterator(); it.hasNext(); ) {
                File f = new File(it.next());
                if (!copy && dest.equals(f.getParentFile())) { it.remove(); s.inPlace++; continue; }
                File to = new File(dest, f.getName());
                if (s.drop(sim, f, to)) { it.remove(); continue; }
                sim.move(f.getAbsolutePath(), s.arrive(sim, to), copy);
            }
            for (java.util.Iterator<String> it = s.dirs.iterator(); it.hasNext(); ) {
                File d = new File(it.next());
                if (!copy && dest.equals(d.getParentFile())) { it.remove(); s.inPlace++; continue; }
                File to = new File(dest, d.getName());
                if (s.skipTaken && sim.isDir(to)) { it.remove(); s.skipped++; continue; }
                sim.gone.add(d.getAbsolutePath());
                sim.dirs.add(s.renameTaken && (sim.isDir(to) || sim.isFile(to)) ? sim.free(to).getAbsolutePath() : to.getAbsolutePath());
            }
            List<String> what = new ArrayList<String>();
            if (!s.files.isEmpty() || s.dirs.isEmpty()) what.add(Words.files(s.files.size()));
            if (!s.dirs.isEmpty()) what.add(Words.folders(s.dirs.size()));
            s.describe = Words.Cmd.transfer(copy, join(what, Words.Cmd.AND), Bidi.path(dest.getAbsolutePath())) + filtered(s);
            s.noop = s.files.isEmpty() && s.dirs.isEmpty() && s.same + s.inPlace > 0;   // הכול כבר במקום — לא שגיאה, ואין מה לעשות
        } else if (cmd.equals("REN")) {
            if (paths.size() != 2 || args.get(1).contains("/") || args.get(1).contains("\\")) {
                s.error = Commands.usage(cmd);
                return;
            }
            File f = new File(paths.get(0));
            if (args.get(0).contains("*") || args.get(0).contains("?")) { s.error = Words.Cmd.REN_WILD; return; }
            if (sim.isDir(f)) { s.error = Words.Cmd.REN_FOLDER; return; }                  // 1.22.5
            if (!sim.isFile(f)) { s.error = Words.FILE_NOT_FOUND + ": " + args.get(0); return; }
            String bad = Physical.badName(args.get(1));
            if (bad != null) { s.error = bad; return; }
            File to = new File(f.getParentFile(), args.get(1));
            if (!to.getName().equalsIgnoreCase(f.getName()) && sim.isFile(to)) { s.error = Words.Cmd.alreadyHas(args.get(1)); return; }
            s.files.add(f.getAbsolutePath());
            s.newName = args.get(1);
            s.describe = Words.Cmd.rename(f.getName(), s.newName);
            sim.move(f.getAbsolutePath(), to, false);
        } else if (cmd.equals("START")) {
            if (paths.isEmpty()) { s.error = Commands.usage(cmd); return; }
            if (paths.get(paths.size() - 1).equals(HOME_MARK)) {      // 1.9.0: START %בית%
                s.dest = new File(HOME_MARK);
                s.describe = Words.Cmd.HOME_AT_END;
                return;
            }
            File d = new File(paths.get(paths.size() - 1));       // "START "" תיקייה" — הכותרת הריקה יורדת
            String name = d.getName();
            if (name.contains("*") || name.contains("?")) {       // 186: תבנית — החדש ביותר מבין המתאימים
                File dir = d.getParentFile();
                if (dir == null || !sim.isDir(dir)) { s.error = Words.FOLDER_NOT_FOUND + ": " + (dir == null ? name : dir.getAbsolutePath()); return; }
                File best = null;
                int n = 0;
                for (File x : sim.files(dir))
                    if (wild(name, x.getName()) && visible(s, x)) {
                        n++;
                        if (best == null || sim.real(x).lastModified() > sim.real(best).lastModified()) best = x;
                    }
                if (best == null) { s.error = Words.Cmd.noMatch(name); return; }
                s.dest = best;
                s.describe = Words.Cmd.openAtEnd(Bidi.path(best.getAbsolutePath())) + (n > 1 ? Words.Cmd.newestOf(n) : "");
                return;
            }
            if (sim.isFile(d)) {                                   // 186: קובץ — נפתח בסוף (apk — התקנה)
                s.dest = d;
                s.describe = Words.Cmd.openAtEnd(Bidi.path(d.getAbsolutePath()));
                return;
            }
            if (!sim.isDir(d)) { s.error = Words.FOLDER_NOT_FOUND; return; }
            s.dest = d;
            s.describe = Words.Cmd.openAtEnd(Bidi.path(d.getAbsolutePath()));
        } else {
            s.error = Words.Cmd.unknownCommand(cmd) + " (" + join(java.util.Arrays.asList(Commands.fileNames()), " ") + ")";
        }
    }

    /**
     * 1.19.0: COPY/MOVE <תיקייה או תיקייה\תבנית> <יעד> /S — כל הקבצים שבתוכה, גם בתיקיות שבתוכה, נכתבים ביעד
     * באותו מסלול (כמו XCOPY /S). היעד נוצר אם אינו קיים. בלי תבנית ובלי סינון — גם תיקיות ריקות (כמו /E).
     */
    private static void keep(Step s, String cmd, List<String> args, List<String> paths, Filter flt, Sim sim) {
        if (paths.size() != 2) { s.error = Commands.usage(cmd); return; }
        File dest = new File(paths.get(1));
        if (sim.isFile(dest)) { s.error = Words.Cmd.NO_DEST; return; }
        File src = new File(paths.get(0));
        String mask = null;
        if (src.getName().contains("*") || src.getName().contains("?")) {
            mask = src.getName();
            src = src.getParentFile();
        }
        if (src == null || !sim.isDir(src)) { s.error = Words.FOLDER_NOT_FOUND + ": " + args.get(0); return; }
        if (Physical.inside(dest, src)) { s.error = Words.Disk.NOT_INTO_ITSELF; return; }
        s.dest = dest;
        s.keepRoot = src.getAbsolutePath();
        walk(s, src, mask, flt, sim, mask == null && flt.none(), true);
        sim.dirs.add(dest.getAbsolutePath());
        for (java.util.Iterator<String> it = s.files.iterator(); it.hasNext(); ) {   // קובץ אחרי קובץ (מ0.40: לשנות-שם)
            String f = it.next();
            File to = new File(dest, Physical.rel(s.keepRoot, f));
            if (s.drop(sim, new File(f), to)) { it.remove(); continue; }
            for (File up = to.getParentFile(); up != null && !up.equals(dest); up = up.getParentFile()) sim.dirs.add(up.getAbsolutePath());
            sim.move(f, s.arrive(sim, to), cmd.equals("COPY"));
        }
        List<String> what = new ArrayList<String>();
        what.add(Words.files(s.files.size()));
        if (!s.dirs.isEmpty()) what.add(Words.Cmd.emptyFolders(s.dirs.size()));
        s.describe = Words.Cmd.transfer(cmd.equals("COPY"), join(what, Words.Cmd.AND), Bidi.path(dest.getAbsolutePath()))
                + Words.Cmd.KEEP_PATH + filtered(s);
        for (String d : s.dirs) sim.dirs.add(new File(dest, Physical.rel(s.keepRoot, d)).getAbsolutePath());
    }

    /** הקבצים בתיקייה ובתיקיות שבתוכה (מוסתרים — רק עם /A:H; קבצים זמניים של העתקה — אף פעם). */
    private static boolean walk(Step s, File dir, String mask, Filter flt, Sim sim, boolean empties, boolean top) {
        boolean any = false;
        for (File x : sim.files(dir)) {
            if (!visible(s, x) || Physical.isPartial(x.getName())) continue;
            any = true;
            if (mask != null && !wild(mask, x.getName())) continue;
            s.total++;
            if (flt.ok(sim.real(x))) s.files.add(x.getAbsolutePath());
        }
        for (File k : sim.subdirs(dir)) {                                   // מ0.38 (הבדיקה הכללית): גם תיקייה ששורה קודמת יצרה
            if (!visible(s, k)) continue;
            any = true;
            walk(s, k, mask, flt, sim, empties, false);
        }
        if (!any && !top && empties) s.dirs.add(dir.getAbsolutePath());
        return any;
    }

    /**
     * 1.20.0: COMP <מקור> <יעד> — השוואה לפי מסלול (Compare). מקור שאינו קיים — שגיאה (לא "ריק": אחרת הכול
     * ייראה כנמחק). יעד שאינו קיים — ריק רק אם שורה קודמת יוצרת אותו (MD); אחרת — שגיאה.
     */
    private static void comp(Step s, List<String> args, List<String> paths, Sim sim) {
        sim.compare = null;
        if (s.listName != null) {                                             // 1.22.0
            compList(s, args, paths, sim);
            return;
        }
        if (paths.size() != 2) { s.error = Commands.usage("COMP"); return; }
        File a = new File(paths.get(0)), b = new File(paths.get(1));
        if (!a.isDirectory()) { s.error = Words.Cmd.compareNoSource(args.get(0)); return; }
        if (!b.isDirectory() && !sim.isDir(b)) { s.error = Words.Cmd.compareNoDest(args.get(1)); return; }
        if (Physical.inside(a, b) || Physical.inside(b, a)) { s.error = Words.Disk.NOT_INTO_ITSELF; return; }
        Compare.Result r = Compare.byPath(sim.lib, a, sim.underWiped(b.getAbsolutePath()) ? GHOST : b, s.hidden);   // מ0.27
        r.b = b.getAbsolutePath();
        sim.compare = r;
        s.dest = b;
        s.describe = Words.Cmd.compared(Bidi.path(r.a), Bidi.path(r.b),
                Compare.Result.files(r.onlyA, r.onlyADirs), Compare.Result.files(r.onlyB, r.onlyBDirs), r.same);
    }

    /** 1.22.0: COMP /רשימה:<שם> <יעד> — כל מה שברשימה, לפי <מפתח כונן>/<מסלול>, מול היעד. */
    private static void compList(Step s, List<String> args, List<String> paths, Sim sim) {
        if (paths.size() != 1) { s.error = Commands.usage("COMP"); return; }
        File b = new File(paths.get(0));
        if (!b.isDirectory() && !sim.isDir(b)) { s.error = Words.Cmd.compareNoDest(args.get(0)); return; }
        if (sim.lib == null || !sim.lib.ready) { s.error = Words.Cmd.NEEDS_INDEX; return; }
        String[] err = {null};
        File skip = b.getName().equals(State.CURRENT) && b.getParentFile() != null ? b.getParentFile() : b;
        Compare.Source src;
        if (DbVerbs.active()) {                                             // מ0.25 (באג 2): עובדים במסד — הרשימה מהמסד
            src = DbListCmd.compSource(sim, s.listName, skip.getAbsolutePath(), err);
            if (src == null) { s.error = err[0]; return; }
        } else {
            s.error = Words.DbCmd.NO_DB;                                      // מ0.29: רשימות — רק במסד
            return;
        }
        if (src.files.isEmpty() && src.emptyDirs.isEmpty()) { s.error = Words.Cmd.listEmpty(s.listName); return; }
        Compare.Result r = Compare.byList(sim.lib, src, sim.underWiped(b.getAbsolutePath()) ? GHOST : b, s.hidden);   // מ0.27
        r.b = b.getAbsolutePath();
        sim.compare = r;
        s.dest = b;
        s.describe = Words.Cmd.compared(s.listName, Bidi.path(r.b),
                Compare.Result.files(r.onlyA, new ArrayList<String>()), Compare.Result.files(r.onlyB, r.onlyBDirs), r.same)
                + (src.outside > 0 ? Words.Cmd.outside(src.outside) : "")
                + (src.skippedSmart > 0 ? Words.Cmd.smartSkipped(src.skippedSmart) : "");
    }

    /**
     * 1.20.0: COPY/MOVE %רק-במקור% | %רק-ביעד% <יעד> — מה שההשוואה מצאה, עם שמירת מסלול (יחסית לשורש של אותו צד).
     * /עד:N% — אם זה יותר מ־N אחוזים מהקבצים של אותו צד — שגיאה, והשורות הבאות על ההשוואה לא ירוצו.
     */
    private static void fromCompare(Step s, String cmd, List<String> paths, List<String> sw, Sim sim) {
        Compare.Result r = sim.compare;
        if (r == null) { s.error = Words.Cmd.NO_COMPARE; return; }
        if (r.broken) { s.error = Words.Cmd.COMPARE_BROKEN; return; }
        boolean a = paths.get(0).equals(ONLY_A_MARK);
        File dest = new File(paths.get(1));
        if (sim.isFile(dest)) { s.error = Words.Cmd.NO_DEST; r.broken = true; return; }
        s.dest = dest;
        s.fromCompare = true;
        s.files.addAll(a ? r.onlyA : r.onlyB);
        s.dirs.addAll(a ? r.onlyADirs : r.onlyBDirs);
        boolean mapped = a && r.relA != null;                                 // 1.22.0: מקור שהוא רשימה
        if (mapped) {
            if (cmd.equals("MOVE")) { s.error = Words.Cmd.LIST_NO_MOVE; r.broken = true; return; }
            s.keepRoot = null;
        } else {
            s.keepRoot = a ? r.a : r.b;
            if (Physical.inside(dest, new File(s.keepRoot))) { s.error = Words.Disk.NOT_INTO_ITSELF; r.broken = true; return; }
        }
        int n = Compare.Result.files(s.files, s.dirs), total = a ? r.filesA : r.filesB;
        for (String x : sw)
            if (x.startsWith(SW_LIMIT)) {
                int pct = limit(x);
                if (total > 0 && (long) n * 100 > (long) pct * total) {
                    s.error = Words.Cmd.overLimit(n, total, pct);
                    r.broken = true;
                    return;
                }
            }
        if (mapped) {                                                          // 1.22.0: כל פריט — לתיקייה שלו
            List<String> items = new ArrayList<String>(s.files);
            items.addAll(s.dirs);
            s.files.clear();
            s.dirs.clear();
            for (String f : items) {
                File to = new File(dest, r.relA.get(f));
                boolean dir = new File(f).isDirectory();
                if (dir ? s.skipTaken && sim.isFile(to) : s.drop(sim, new File(f), to)) { if (dir) s.skipped++; continue; }
                s.files.add(f);
                s.targets.add(to.getParentFile().getAbsolutePath());
                if (dir) sim.dirs.add(to.getAbsolutePath());
                else sim.move(f, s.arrive(sim, to), true);
            }
            s.describe = Words.Cmd.transfer(true, Words.files(s.files.size()), Bidi.path(dest.getAbsolutePath())) + Words.Cmd.KEEP_PATH
                    + notes(s);
            sim.dirs.add(dest.getAbsolutePath());
            return;
        }
        sim.dirs.add(dest.getAbsolutePath());
        for (java.util.Iterator<String> it = s.files.iterator(); it.hasNext(); ) {   // קובץ אחרי קובץ (מ0.40: לשנות-שם)
            String f = it.next();
            File to = new File(dest, Physical.rel(s.keepRoot, f));
            if (s.drop(sim, new File(f), to)) { it.remove(); continue; }
            sim.move(f, s.arrive(sim, to), cmd.equals("COPY"));
        }
        List<String> what = new ArrayList<String>();
        what.add(Words.files(s.files.size()));
        if (!s.dirs.isEmpty()) what.add(Words.folders(s.dirs.size()));
        s.describe = Words.Cmd.transfer(cmd.equals("COPY"), join(what, Words.Cmd.AND), Bidi.path(dest.getAbsolutePath()))
                + Words.Cmd.KEEP_PATH + notes(s);
        for (String d : s.dirs) {
            if (cmd.equals("MOVE")) sim.gone.add(d);
            sim.dirs.add(new File(dest, Physical.rel(s.keepRoot, d)).getAbsolutePath());
        }
    }

    /**
     * 1.21.0: שחזור — COPY <גיבוי> <יעד> /מצב:<זמן>. המקור: שורש גיבוי "מראה + היסטוריה", או מסלול לוגי בתוכו
     * ("<שורש>\\מסמכים\\א" = מה שהיה ב־מסמכים/א), ואפשר תבנית בסוף. כל קובץ שהיה במצב באותו זמן (State) — מ"נוכחי"
     * או מתיקיית היסטוריה — נכתב ביעד במסלול הלוגי שלו (יחסית לתת־התיקייה שנבחרה). הגיבוי עצמו — לא משתנה.
     */
    private static void restore(Step s, List<String> args, List<String> paths, Filter flt, Sim sim) {
        if (paths.size() != 2) { s.error = Commands.usage("COPY"); return; }
        File src = new File(paths.get(0));
        String mask = null;
        if (src.getName().contains("*") || src.getName().contains("?")) {
            mask = src.getName();
            src = src.getParentFile();
        }
        File root = src;
        while (root != null && !State.isBackupRoot(root)) root = root.getParentFile();
        if (root == null) { s.error = Words.Cmd.notBackup(args.get(0)); return; }
        String prefix = Physical.rel(root.getAbsolutePath(), src.getAbsolutePath());
        if (prefix.equals(State.CURRENT) || prefix.startsWith(State.CURRENT + "/") || prefix.equals(State.HISTORY)
                || prefix.startsWith(State.HISTORY + "/")) { s.error = Words.Cmd.STATE_LOGICAL; return; }
        File dest = new File(paths.get(1));
        if (sim.isFile(dest)) { s.error = Words.Cmd.NO_DEST; return; }
        if (Physical.inside(dest, root)) { s.error = Words.Disk.NOT_INTO_ITSELF; return; }
        State st = new State(s.stateWhen);
        java.util.TreeMap<String, State.Where> best = new java.util.TreeMap<String, State.Where>();
        java.util.Map<String, String> phys = new java.util.HashMap<String, String>();
        List<File> places = new ArrayList<File>();
        places.add(new File(root, State.CURRENT));
        File[] hs = new File(root, State.HISTORY).listFiles();
        if (hs != null) for (File h : hs) if (h.isDirectory()) places.add(h);
        for (File place : places) {
            File start = prefix.isEmpty() ? place : new File(place, prefix);
            List<String> found = new ArrayList<String>();
            collect(s, start, mask, found);
            for (String f : found) {
                Library.Hit hit = sim.lib == null ? null : sim.lib.find(f);
                State.Where w = st.version(f, hit == null || hit.rec == null ? 0 : hit.rec.added);
                if (w == null || !(w.came <= st.when && st.when < w.left)) continue;
                s.total++;
                if (!flt.ok(new File(f))) continue;
                State.Where old = best.get(w.rel);
                if (old == null || w.left < old.left) {                     // שתי גרסאות — הקרובה ביותר אחרי הזמן
                    best.put(w.rel, w);
                    phys.put(w.rel, f);
                }
            }
        }
        s.dest = dest;
        for (Map.Entry<String, State.Where> e : best.entrySet()) {
            String rel = e.getKey();
            String sub = prefix.isEmpty() ? rel : rel.substring(prefix.length() + 1);
            File to = new File(dest, sub).getParentFile();
            s.files.add(phys.get(rel));
            s.targets.add(to.getAbsolutePath());
            sim.added.put(new File(dest, sub).getAbsolutePath(), new File(phys.get(rel)));
            for (File up = to; up != null && !up.equals(dest.getParentFile()); up = up.getParentFile()) sim.dirs.add(up.getAbsolutePath());
        }
        if (s.skipTaken)
            for (int k = s.files.size() - 1; k >= 0; k--)
                if (new File(s.targets.get(k), new File(s.files.get(k)).getName()).exists()) {
                    s.files.remove(k);
                    s.targets.remove(k);
                    s.skipped++;
                }
        s.describe = Words.Cmd.restore(Words.files(s.files.size()), new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.ROOT)
                .format(new java.util.Date(s.stateWhen)), Bidi.path(dest.getAbsolutePath()));   // בכונן
    }

    /** כל הקבצים מתחת ל־d (מוסתרים — רק עם /A:H; זמניים — אף פעם), לפי תבנית אם יש. */
    private static void collect(Step s, File d, String mask, List<String> out) {
        File[] k = d.listFiles();
        if (k == null) return;
        java.util.Arrays.sort(k);
        for (File x : k) {
            if (!visible(s, x) || Physical.isPartial(x.getName())) continue;
            if (x.isDirectory()) collect(s, x, mask, out);
            else if (mask == null || wild(mask, x.getName())) out.add(x.getAbsolutePath());
        }
    }

    /** /עד:20% או /עד:20 — המספר; -1 אם אינו מספר בין 0 ל־100. */
    static int limit(String sw) {
        String v = sw.substring(SW_LIMIT.length()).trim();
        if (v.endsWith("%")) v = v.substring(0, v.length() - 1).trim();
        try {
            int n = Integer.parseInt(v);
            return n >= 0 && n <= 100 ? n : -1;
        } catch (Throwable t) {
            return -1;
        }
    }

    /** 1.20.0: %רק-במקור% / %רק-ביעד% — רק לבדם, כמקור של COPY או MOVE. */
    private static String setsMisuse(String cmd, List<String> paths) {
        for (int i = 0; i < paths.size(); i++) {
            String p = paths.get(i);
            if (!p.startsWith(ONLY_A_MARK) && !p.startsWith(ONLY_B_MARK)) continue;
            if (!(cmd.equals("COPY") || cmd.equals("MOVE")) || i != 0 || !(p.equals(ONLY_A_MARK) || p.equals(ONLY_B_MARK)))
                return Words.Cmd.SETS_ONLY_SOURCE;
        }
        return null;
    }

    /** 1.9.0: "%בית%" — רק לבדו, אחרי START. */
    private static String homeMisuse(String cmd, List<String> paths) {
        for (String p : paths) {
            if (!p.startsWith(HOME_MARK)) continue;
            if (!cmd.equals("START") || !p.equals(HOME_MARK)) return Words.Cmd.HOME_ONLY_START;
        }
        return null;
    }

    /** 203: "%שיתוף%" — רק כיעד של COPY, ובלי מסלול אחריו. */
    private static String shareMisuse(String cmd, List<String> paths) {
        for (int i = 0; i < paths.size(); i++) {
            String p = paths.get(i);
            if (!p.startsWith(SHARE_MARK)) continue;
            if (!p.equals(SHARE_MARK)) return Words.Cmd.SHARE_NO_PATH;
            if (cmd.equals("MOVE") && i == 1) return Words.Cmd.SHARE_NO_MOVE;
            if (!cmd.equals("COPY") || i != 1) return Words.Cmd.SHARE_ONLY_COPY;
        }
        return null;
    }

    private static String join(List<String> parts, String sep) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) sb.append(sb.length() > 0 ? sep : "").append(p);
        return sb.toString();
    }

    /** קובץ מוסתר (שם שמתחיל בנקודה) — כמו ב-bat, רק עם /A:H (135). */
    private static boolean visible(Step s, File x) {
        return s.hidden || !x.getName().startsWith(".");
    }

    /** מקור: קובץ, תבנית (*.pdf), או תיקייה (COPY — הקבצים שבתוכה; MOVE — התיקייה עצמה, כמו ב-bat). */
    private static void match(Step s, String src, Filter flt, boolean moveDir, Sim sim) {
        File f = new File(src);
        String name = f.getName();
        if (name.contains("*") || name.contains("?")) {
            File dir = f.getParentFile();
            if (dir == null || !sim.isDir(dir)) { s.error = Words.FOLDER_NOT_FOUND + ": " + (dir == null ? src : dir.getAbsolutePath()); return; }
            for (File x : sim.files(dir))
                if (wild(name, x.getName()) && visible(s, x)) {
                    s.total++;
                    if (flt.ok(sim.real(x))) s.files.add(x.getAbsolutePath());
                }
            return;
        }
        if (sim.isDir(f)) {
            if (moveDir) {
                s.dirs.add(f.getAbsolutePath());
                return;
            }
            for (File x : sim.files(f))
                if (visible(s, x)) {
                    s.total++;
                    if (flt.ok(sim.real(x))) s.files.add(x.getAbsolutePath());
                }
            return;
        }
        if (sim.isFile(f)) {                                      // קובץ שנכתב במפורש — גם אם מוסתר
            s.total++;
            if (flt.ok(sim.real(f))) s.files.add(f.getAbsolutePath());
            return;
        }
        s.error = Words.Cmd.notFound(src);
    }

    private static String filtered(Step s) {
        int passed = s.files.size() + s.skipped + s.same + s.inPlace;                 // מ0.40: גם מה שיצא אחרי הסינון — עבר אותו
        String f = s.total > passed && s.dirs.isEmpty() ? Words.Cmd.filtered(passed, s.total) : "";
        return f + notes(s);
    }

    /**
     * מ0.40: /תפוס:לשנות-שם בהרצה — קובץ שיש בתיקיית היעד שלו קובץ זהה (בכונן עכשיו, או שהגיע בשורה הזו לפניו) יוצא מהשורה.
     * בתכנון זה נבדק מול ההדמיה; אבל לקובץ ששורה קודמת באותו קובץ יוצרת עוד אין תוכן בתכנון, וגם בין התצוגה המקדימה
     * להרצה הכונן יכול להשתנות. המשפך — כמו בתכנון: הגודל קודם, חתימה רק לשווי גודל.
     */
    private static void sameAtRun(Step s) {
        Map<String, List<File>> arriving = new java.util.HashMap<String, List<File>>();
        for (int k = 0; k < s.files.size(); k++) {
            File f = new File(s.files.get(k));
            File dir = !s.targets.isEmpty() ? new File(s.targets.get(k))
                    : s.keepRoot != null ? new File(s.dest, Physical.rel(s.keepRoot, f.getAbsolutePath())).getParentFile() : s.dest;
            List<File> came = arriving.get(dir.getAbsolutePath());
            if (came == null) arriving.put(dir.getAbsolutePath(), came = new ArrayList<File>());
            if (sameOnDisk(f, dir, came)) {
                s.files.remove(k);
                if (!s.targets.isEmpty()) s.targets.remove(k);
                k--;
                s.same++;
            } else came.add(f);
        }
    }

    /** מ0.40: /מכיל: בהרצה — קובץ שלא כתוב בו הטקסט יוצא מהשורה (בתכנון עבר כ"עוד לא ידוע", או שהשתנה מאז). */
    private static void containsAtRun(Step s) {
        for (int k = 0; k < s.files.size(); k++) {
            File f = new File(s.files.get(k));
            boolean ok = true;
            for (byte[] w : s.contains) if (!Files.contains(f, w)) ok = false;
            if (ok) continue;
            s.files.remove(k);
            if (!s.targets.isEmpty()) s.targets.remove(k);
            k--;
        }
    }

    private static boolean sameOnDisk(File src, File dir, List<File> came) {
        long n = src.length();
        String sig = null;
        List<File> all = new ArrayList<File>(came);
        File[] in = dir.listFiles();
        if (in != null) all.addAll(Arrays.asList(in));
        for (File g : all) {
            if (!g.isFile() || g.length() != n) continue;
            if (sig == null && (sig = Sig.of(src)) == null) return false;
            if (sig.equals(Sig.of(g))) return true;
        }
        return false;
    }

    /** מה יצא מהשורה אחרי הסינון, ולמה: שם תפוס (לדלג) · זהה כבר ביעד (לשנות-שם) · כבר במקום (העברה). מתחיל ב־" · ". */
    static String notes(Step s) {
        return (s.skipped > 0 ? Words.Cmd.skippedTaken(s.skipped) : "") + (s.same > 0 ? Words.Cmd.sameAtDest(s.same) : "")
                + (s.inPlace > 0 ? Words.Cmd.inPlace(s.inPlace) : "");
    }

    // ---------- ביצוע ----------

    /** מבצע את התוכנית (ברקע), דרך Physical. מחזיר דוח. */
    static List<String> run(Plan plan, Library lib, Physical.Asker asker, Physical.Progress progress) {
        return run(plan, lib, asker, progress, false);
    }

    /** קובץ שעוד לא באינדקס (למשל הורדה חדשה) — נכנס אליו לפני שמוחקים אותו (134). */
    private static void known(Library lib, List<String> files) {
        boolean any = false;
        for (String p : files)
            if (lib.find(p) == null && new File(p).isFile()) {
                lib.fileAdded(new File(p));
                any = true;
            }
        if (any) lib.flush(5000);
    }

    /** grey: ההגדרה "קובץ שנמחק — השאר באפור" (58), כמו בשאר המסכים (120). */
    /** 1.26.0: מסך ההרצה — לפני כל שורה (מקום, כמה בסך הכול), ואחרי כל שורה (מה יצא). */
    interface Listener {
        void before(int index, int total, Step s);

        void after(Step s, String line);
    }

    static List<String> run(Plan plan, Library lib, Physical.Asker asker, Physical.Progress progress, boolean grey) {
        return run(plan, lib, asker, progress, grey, null);
    }

    static List<String> run(Plan plan, Library lib, Physical.Asker asker, Physical.Progress progress, boolean grey, Listener listen) {
        if (plan.asIf) {                                                        // מבנה 40 (ממצא 34): הדמיה — גם המנוע עצמו אינו מריץ.
            List<String> no = new ArrayList<String>(plan.inputs);               // עד כאן רק המסכים שמרו על זה; מי שהיה קורא ל־run — היה מבצע באמת
            no.add(Words.Run.AS_IF);
            return no;
        }
        plan.pulse = plan.measure ? new Pulse() : null;                         // מבנה 38: עם "מדידה" — גם כמה זמן התוכנה לא רצה
        try {
            return run0(plan, lib, asker, progress, grey, listen);
        } finally {
            if (plan.pulse != null) plan.pulse.stop();
        }
    }

    /**
     * מבנה 38: דופק ברקע בזמן הרצה עם "מדידה" — פעימה כל חצי שנייה, לפי השעון הרגיל. פער גדול בין שתי פעימות אומר
     * שכל התוכנה לא רצה: המכשיר ישן, או שהמערכת עצרה אותה (מסך כבוי). כך שורה איטית מסבירה את עצמה —
     * אם הדופק המשיך לפעום, השורה עצמה חיכתה למשהו; אם נעצר — לא השורה אשמה. (נ0.2: שורה אחת — 67.8 שניות, ואי אפשר היה לדעת למה.)
     */
    static final class Pulse implements Runnable {
        static final long EVERY = 500, GAP = 2000;
        private volatile boolean on = true;
        private volatile long missed;
        private final Thread thread = new Thread(this, "vfm-pulse");

        Pulse() {
            thread.setDaemon(true);
            thread.start();
        }

        @Override
        public void run() {
            long last = System.currentTimeMillis();
            while (on) {
                try {
                    Thread.sleep(EVERY);
                } catch (InterruptedException e) {
                    return;
                }
                long now = System.currentTimeMillis();
                if (now - last >= GAP) missed += now - last - EVERY;
                last = now;
            }
        }

        /** כמה זמן (באלפיות) התוכנה לא רצה מאז שהדופק התחיל. */
        long missed() {
            return missed;
        }

        void stop() {
            on = false;
            thread.interrupt();
        }
    }

    private static List<String> run0(Plan plan, Library lib, Physical.Asker asker, Physical.Progress progress, boolean grey, Listener listen) {
        List<String> report = new ArrayList<String>(plan.inputs);              // 1.27.0: הקלט — בראש
        int fails = 0;                                                           // מ0.31: כישלונות בהרצה — מול המותר ("המשך")
        Step pending = null;                                                     // מ0.37: "ודא שגיאה"
        int pendingFails = 0;
        int sectionFails = 0;                                                    // מ0.45: "ודא תקין" — הכישלונות מתחילת הקטע
        boolean stopped = false;                                                 // מבנה 40: ההרצה נעצרה בגלל כישלונות (ואז ההודעה על כך כבר בדוח)
        if (plan.blocked()) {                                                    // מ0.30: שגיאה בקובץ — שום שורה לא רצה
            plan.trouble = true;
            report.add(Words.DbCmd.notRun(plan.errors));
            report.add(plan.errorText());
            writeReport(plan, report);
            return report;
        }
        Clock clock = new Clock();                                               // 1.25.0: לולאה — עצירה לפי זמן
        long all0 = System.nanoTime();
        Step prev = null;
        int mark = 0;
        long t0 = System.nanoTime();
        for (int si = 0; si <= plan.steps.size(); si++) {
            if (prev != null && plan.measure) {                                  // 1.25.0: מדידה — זמן השורה הקודמת
                prev.ms = (System.nanoTime() - t0) / 1000000;
                if (report.size() > mark) {
                    String last = report.get(report.size() - 1) + Words.Loop.took(prev.ms);
                    if (plan.lineLimit > 0 && prev.ms > plan.lineLimit) {           // מ0.45: חרגה מהזמן — ❌, ונספרת עם "ודא" שנכשלו
                        String head = Words.Cmd.line(prev.label(), "");
                        last = last.startsWith(head) ? head + Words.Loop.tooSlow(plan.lineLimit) + last.substring(head.length())
                                : Words.Loop.tooSlow(plan.lineLimit) + last;
                        prev.verified = Boolean.FALSE;
                        plan.trouble = true;
                    }
                    report.set(report.size() - 1, last);
                }
            }
            if (prev != null && listen != null) listen.after(prev, report.size() > mark ? report.get(report.size() - 1) : null);
            mark = report.size();
            t0 = System.nanoTime();
            prev = null;
            if (pending != null) {                                              // מ0.37: "ודא שגיאה" — השורה הקודמת הייתה צריכה להיכשל
                boolean failed = fails > pendingFails;
                pending.verified = failed;
                if (failed) fails = pendingFails;                                // כישלון צפוי אינו נספר
                report.add(Words.Cmd.line(pending.label(), failed ? Words.DbCmd.EXPECT_OK_RUN : Words.DbCmd.EXPECT_BAD));
                pending = null;
            }
            if (si == plan.steps.size()) break;
            if (fails > plan.allowErrors) {                                      // מ0.31: יותר כישלונות מהמותר — עוצרים כאן
                stopped = true;
                plan.trouble = true;
                report.add(Words.DbCmd.stoppedAt(plan.steps.get(si).label(), fails, plan.allowErrors));
                break;
            }
            Step s = plan.steps.get(si);
            if (progress != null && progress.stopped()) {
                report.add(Words.Cmd.stoppedBefore(s.line));
                plan.trouble = true;
                break;
            }
            if (clock.skip(plan, s.loops, s.rounds)) {
                if (!clock.notes.isEmpty()) {
                    report.addAll(clock.notes);
                    clock.notes.clear();
                }
                continue;
            }
            prev = s;
            if (s.expectFail) {
                pending = s;
                pendingFails = fails;
            }
            s.ran = s.error == null;
            if (listen != null) listen.before(si, plan.steps.size(), s);
            if (s.error != null) {
                report.add(Words.Cmd.line(s.label(), Words.Cmd.skipped(s.error)));
                plan.trouble = true;
                fails++;
                continue;
            }
            if (s.contains != null) containsAtRun(s);                          // מ0.40: /מכיל: — מול התוכן של עכשיו
            String out;
            if (s.noop) {                                                       // 1.27.0: גם מה שלא השתנה — נאמר
                if (s.cmd.equals(SECTION)) sectionFails = fails;
                report.add(Words.Cmd.line(s.label(), s.describe));
                continue;
            }
            if (s.cmd.equals(OK_SINCE)) {                                       // מ0.45: ודא תקין
                boolean ok = fails == sectionFails;
                s.verified = ok;
                report.add(Words.Cmd.line(s.label(), Words.DbCmd.verifyResult(ok, Words.DbCmd.OK_SINCE_WHAT,
                        Words.DbCmd.okSinceGot(fails - sectionFails))));
                continue;
            }
            if (s.cmd.equals("ECHO") || s.cmd.equals("START") || (s.cmd.equals(OPEN) && s.op == null)) continue;
            if (s.op != null) {                                                // 1.24.0: רשימות
                DbListCmd.lastFailed = false;
                report.add(Words.Cmd.line(s.label(), s.op.run(lib)));
                if (DbListCmd.lastFailed) {
                    plan.trouble = true;
                    fails++;
                }
                continue;
            }
            if (s.cmd.equals("COMP")) {                                       // 1.20.0: חושב בתכנון — רק בדוח
                report.add(Words.Cmd.line(s.label(), s.describe));
                continue;
            }
            if (s.cmd.equals("MD")) {
                String why = Physical.makeFolder(lib, s.dest.getParentFile(), s.dest.getName());
                out = why == null ? Words.Cmd.created(s.dest.getName()) : why;
                if (why != null) { plan.trouble = true; fails++; }
            } else if (s.cmd.equals("RD") || s.cmd.equals("DEL")) {
                if (s.files.isEmpty() && s.dirs.isEmpty()) out = Words.Cmd.NO_FILES;
                else {
                    known(lib, s.files);
                    Physical.Result r = Physical.delete(lib, s.files, s.dirs, grey);
                    out = result(r, Words.Cmd.TRASHED);
                    if (r.failed > 0) { plan.trouble = true; fails += r.failed; }
                }
            } else if (s.share) {                                          // 203
                if (s.files.isEmpty() && s.dirs.isEmpty()) out = Words.Cmd.NO_FILES;
                else out = stage(plan, s, progress);
            } else if (s.cmd.equals("COPY") || s.cmd.equals("MOVE")) {
                if (s.renameTaken) sameAtRun(s);                                // מ0.40: "יש כבר זהה" — שוב, מול הכונן של עכשיו
                if (s.files.isEmpty() && s.dirs.isEmpty()) out = notes(s).isEmpty() ? Words.Cmd.NO_FILES : notes(s).substring(3);
                else {
                    boolean copy = s.cmd.equals("COPY");
                    Physical.Journal j = s.journal == null ? null : new Physical.Journal(new File(s.journal), s.text);   // 1.19.0
                    Physical.Asker ask = s.replace ? REPLACE_ALL : s.renameTaken ? RENAME_ALL   // מ0.40: /תפוס:לשנות-שם
                            : s.failTaken ? null : asker;                                    // 1.20.0: /תפוס:לעצור
                    Physical.Result r = !s.targets.isEmpty()                           // 1.21.0 שחזור, 1.22.0 רשימה (מ0.40: גם העברה)
                            ? Physical.transferTo(lib, s.files, s.targets, s.dest, copy, ask, progress, grey, j)
                            : s.keepRoot != null
                            ? Physical.transferKeep(lib, s.keepRoot, s.files, s.dirs, s.dest, copy, ask, progress, grey, j,
                                    !s.fromCompare)
                            : Physical.transfer(lib, s.files, s.dirs, s.dest, copy, ask, progress, grey, j);
                    out = result(r, copy ? Words.Cmd.COPIED : Words.Cmd.MOVED);
                    out += notes(s);
                    if (j != null) {
                        j.close();
                        if (j.failed()) {
                            out += Words.Cmd.JOURNAL_FAILED;
                            plan.trouble = true;
                            fails++;
                        }
                    }
                    if (r.failed > 0) { plan.trouble = true; fails += r.failed; }
                }
            } else if (s.cmd.equals("REN")) {
                Physical.Result r = Physical.rename(lib, s.files.get(0), s.newName);
                out = result(r, Words.Cmd.RENAMED);
                if (r.failed > 0) { plan.trouble = true; fails += r.failed; }
            } else {
                out = Words.Cmd.NOT_DONE;
                plan.trouble = true;
                fails++;
            }
            report.add(Words.Cmd.line(s.label(), out));
        }
        if (plan.measure) measured(plan, report, (System.nanoTime() - all0) / 1000000);
        // מבנה 40 (ממצא 25): כישלונות שאינם "ודא" — שורה שנכשלה וההרצה המשיכה ("המשך") — נאמרים גם בסיכום, לא רק בשורה שלהם.
        // הכלל של המשתמש: "אין מצב שיש שגיאה והקובץ רץ ואף אחד לא יודע". (נ0.1: "צור אופן … לא נוצר" — התגלה רק בהשוואה בין שני דוחות.)
        if (fails > 0 && !stopped) report.add(Words.DbCmd.failsSummary(fails));
        int ok = 0, bad = 0;                                                     // מ0.14: ודא — כמה עברו וכמה נכשלו
        for (Step s : plan.steps) if (s.verified != null) { if (s.verified) ok++; else bad++; }
        if (ok + bad > 0) report.add(Words.DbCmd.verifySummary(ok, bad));
        writeReport(plan, report);
        return report;
    }

    /** 1.26.0: מה שורה עושה — לתצוגה המקדימה ולסיכום. */
    static final int K_CREATE = 0, K_CHANGE = 1, K_DELETE = 2, K_READ = 3;

    static int kind(Step s) {
        if (s.cmd.equals("MD")) return K_CREATE;
        if (s.cmd.equals("DEL") || s.cmd.equals("RD") || s.cmd.equals(PURGE) || s.cmd.equals(DROP_MODE)) return K_DELETE;   // מ0.45
        if (s.cmd.equals("COPY") || s.cmd.equals("MOVE") || s.cmd.equals("REN")) return s.share ? K_READ : K_CHANGE;
        return K_READ;
    }

    /** נוגעת בקבצים בכונן (לא ברשימות). */
    static boolean disk(Step s) {
        return s.op == null && !s.noop && (s.cmd.equals("COPY") || s.cmd.equals("MOVE") || s.cmd.equals("DEL") || s.cmd.equals("RD")
                || s.cmd.equals("MD") || s.cmd.equals("REN"));
    }

    /**
     * 1.28.0 (המשתמש: "את הבדיקות שהוספת לאחרונה תשים באופציה בפקודה — קובץ שירצה יריץ את הבדיקות האלה; אין צורך שכל
     * פקודה תעבור את המבחן הזה תמיד"): מסך ההרצה — רק לקובץ שיש בו שורת "בדיקה".
     */
    static boolean wantsCheck(String text) {
        for (String raw : Bidi.strip(text).replace("\r", "").split("\n")) if (clean(raw).equals(CHECK)) return true;
        return false;
    }

    /** 1.25.0: סיכום המדידה, ויומן (שורה לכל שורה בדוח, נוסף בכל ריצה). */
    private static void measured(Plan plan, List<String> report, long total) {
        Step slow = null;
        for (Step x : plan.steps) if (x.ms >= 0 && (slow == null || x.ms > slow.ms)) slow = x;
        report.add(Words.Loop.total(total, slow == null ? null : slow.label(), slow == null ? 0 : slow.ms)
                + Words.Loop.notRunning(plan.pulse == null ? 0 : plan.pulse.missed()));
        if (plan.measureJournal == null) return;
        try {
            File j = new File(plan.measureJournal);
            if (j.getParentFile() != null) j.getParentFile().mkdirs();
            java.io.Writer w = new java.io.OutputStreamWriter(new java.io.FileOutputStream(j, true), "UTF-8");
            try {
                w.write("=== " + new java.text.SimpleDateFormat(NOW_FORMAT, Locale.ROOT).format(new java.util.Date()) + " · "
                        + Log.version + "\n");
                for (String r : report) w.write(r + "\n");
                w.write("\n");
            } finally {
                w.close();
            }
        } catch (Throwable t) {
            report.add(Words.Loop.JOURNAL_FAILED);
            Log.e("מדידה — יומן", t);
        }
    }

    /**
     * 203: מכין לשליחה — עותק של כל קובץ, ואריזה של כל תיקייה, בתיקיית השליחה. כך שורה מאוחרת יותר (DEL, MOVE)
     * לא פוגעת במה שעוד לא נשלח. אין די מקום — לא מכין, ואומר כמה צריך.
     */
    private static String stage(Plan plan, Step s, Physical.Progress progress) {
        File dir = stageDir(plan);
        long need = 0;
        for (String f : s.files) need += new File(f).length();
        for (String d : s.dirs) need += Zip.measure(new File(d))[1];
        long free = dir.getUsableSpace();
        if (free < need) return Words.ActsW.NO_ROOM_PACK + Files.size(need) + Words.ActsW.FREE + Files.size(free);
        int done = 0, failed = 0;
        for (String f : s.files) {
            File src = new File(f);
            File to = new File(dir, Physical.freeName(dir, src.getName(), false));
            try {
                Files.copy(src, to);
                plan.shared.add(to);
                done++;
            } catch (Throwable t) {
                Log.e("203: שיתוף בקובץ פקודות", t);
                failed++;
            }
        }
        for (String d : s.dirs) {                                          // 1.10.0: תיקייה — הקבצים שבה כמו שהם
            File src = new File(d);
            List<File> inside = Files.filesIn(src);
            if (inside.size() > Files.MAX_SEND) {                              // יותר מדי לחלון השיתוף — ארוזה בשמה
                File zip = new File(dir, Physical.freeName(dir, src.getName() + ".zip", false));
                Zip.Result r = Zip.pack(src, zip, progress);
                if (r.error == null && !r.stopped) {
                    plan.shared.add(zip);
                    done++;
                } else failed++;
                continue;
            }
            for (File x : inside) {
                File to = new File(dir, Physical.freeName(dir, x.getName(), false));
                try {
                    Files.copy(x, to);
                    plan.shared.add(to);
                    done++;
                } catch (Throwable t) {
                    Log.e("1.10.0: שיתוף תיקייה כקבצים", t);
                    failed++;
                }
            }
        }
        return Words.Cmd.STAGED + " " + done + (failed > 0 ? Words.Cmd.failed(failed) : "");
    }

    /** תיקייה חדשה לכל הרצה, בתוך תיקיית השליחה; ישנות (מעל יום) נמחקות. */
    private static File stageDir(Plan plan) {
        if (plan.stage != null) return plan.stage;
        File root = Files.sendDir();
        File[] old = root.listFiles();
        if (old != null)
            for (File f : old)
                if (f.isDirectory() && f.getName().startsWith(STAGE) && System.currentTimeMillis() - f.lastModified() > 24L * 3600 * 1000)
                    wipe(f);
        plan.stage = new File(root, STAGE + System.currentTimeMillis());
        plan.stage.mkdirs();
        return plan.stage;
    }

    private static final String STAGE = "batch-";

    /** מוחק הכנה ישנה — רק בתוך תיקיית השליחה של התוכנה (לא קובצי המשתמש). */
    private static void wipe(File f) {
        File[] k = f.listFiles();
        if (k != null) for (File x : k) wipe(x);
        f.delete();
    }

    private static String result(Physical.Result r, String verb) {
        String s = verb + " " + r.done.size();
        if (r.failed > 0) s += Words.Cmd.failed(r.failed) + (r.note.isEmpty() ? "" : " (" + r.note + ")");
        return s;
    }

    /** /Y: שם תפוס — להחליף (הקודם עובר לסל), בלי לשאול. */
    static final Physical.Asker REPLACE_ALL = new Physical.Asker() {
        @Override
        public Physical.Answer ask(File existing, File incoming, String suggested) {
            return new Physical.Answer(Physical.Choice.REPLACE, null, true);
        }
    };

    /** מ0.40: /תפוס:לשנות-שם — בהרצה: שם תפוס ← השם הפנוי הבא ("(2)"), גם לתיקייה (בלי מיזוג). */
    static final Physical.Asker RENAME_ALL = new Physical.Asker() {
        @Override
        public Physical.Answer ask(File existing, File incoming, String suggested) {
            return new Physical.Answer(Physical.Choice.RENAME, null, true);
        }
    };

    /** תבנית לקובץ חדש — ההסבר מהמילון, התחביר מכאן. */
    static String template() {
        return Words.Cmd.template(join(Arrays.asList(Commands.fileNames()), "  "),
                var(V_INTERNAL) + "  " + var(V_CARD) + "  " + var(V_DOWNLOADS) + "  " + var(V_HERE) + "  " + var(V_SHARE) + "  " + var(V_HOME)
                        + "  " + var(V_NOW) + "  " + var(V_CARD_ID),
                "/" + SW_DATE + "יום  /" + SW_DATE + "<שנה  /" + SW_SIZE + ">10MB",   // תחביר
                SW_TAKEN + TAKEN_ASK, SW_TAKEN + TAKEN_SKIP,
                "MOVE \"" + var(V_DOWNLOADS) + "\\*.pdf\" \"" + var(V_INTERNAL) + "\\ספרים\" /" + SW_DATE + "שבוע");   // תחביר
    }

    // ---------- הפקודות שירדו: "הוסף תוסף" / "החלף תוסף" (מבנה 11: עד כאן ב־Lab; אלה שורות של פקודות, לא ניהול התוספים) ----------

    /** ירדו במ0.40 — שורה כזו היא שגיאה עם הסבר (ולא "הוסף" של הפעלים). */
    static final String LAB_ADD = "הוסף תוסף", LAB_SWITCH = "החלף תוסף", LAB_OLD_ADD = "הוסף מעבדה", LAB_OLD_SWITCH = "החלף מעבדה";   // תחביר

    static boolean isLabLine(String line) {
        for (String w : new String[]{LAB_ADD, LAB_SWITCH, LAB_OLD_ADD, LAB_OLD_SWITCH}) if (line.equals(w) || line.startsWith(w + " ")) return true;
        return false;
    }

    /** "הוסף תוסף" / "החלף תוסף" (וגם "… מעבדה"): ירדו במ0.40 — שגיאה שאומרת מה במקומן (ולא "הוסף" של הפעלים, שמבלבל). */
    static void planLabLine(Step s) {
        s.error = Words.Lab.GONE;
    }
}
