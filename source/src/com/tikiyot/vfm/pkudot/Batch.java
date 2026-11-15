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
        /** מבנה 74: מה שיצא מהפעולה בכונן (מחיקה) — למי שהריץ את השורה מהמסך וצריך את הפרטים, לא רק את שורת הדוח. */
        Physical.Result result;
        /** מבנה 85: מה שפעולה של רשימות או של המסד אמרה בהרצה (ההודעה), והאם נכשלה — למסך שהריץ את השורה. */
        String said;
        boolean saidFailed;
        /** מבנה 91: המספר במסד של מה שהפעולה יצרה (רשימה חדשה מהמסך) — כדי שהמסך יפתח אותה; 0 — לא נוצר דבר. */
        long made;
        /** מבנה 116: הפריטים שהפעולה הכניסה ליעד (הדבקה מהלוח של הרשימות) — null: לא רלוונטי. */
        List<Long> entered;
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
        Runner.Pulse pulse;
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
        /** מבנה 62: מי ששואלים אם המשתמש ביקש לעצור — בזמן ההרצה (שורה ארוכה, כמו "המתן", שואלת באמצע). */
        Physical.Progress asked;
        /** 1.25.0: לולאה שנעצרה בתכנון (הסבב עבר את הגבול) — מוצג בתצוגה המקדימה. */
        final List<String> notes = new ArrayList<String>();

        int changes() {
            int n = 0;
            for (Step s : steps) if (s.error == null && !s.noop && !s.cmd.equals("ECHO") && !s.cmd.equals("START")
                    && !(s.cmd.equals(Syntax.OPEN) && s.op == null) && !s.cmd.equals("COMP")) n++;
            return n;
        }
    }

    private Batch() { }

    /**
     * מ0.37 (המשתמש, 11.11.2026: "אם כבר אז תאסוף כמה קטנות, לא רק את אלה של הבדיקות"): כתוב · המתן · גבה · רענן · דוח ·
     * "ודא שגיאה". מחזיר true אם השורה שלהן (ואז s מוכן: שגיאה, או תיאור ופעולה).
     */
    /**
     * מבנה 50 — "ודא רציף": בדיקה של "ההרצה ממשיכה כשהמסך כבוי" שמדווחת בעצמה (המשתמש, 14.11.2026: "למה אתה לא מכין פקודות
     * שבודקות את כל זה"). המסך לא כבה לפחות 10 שניות — הבדיקה לא בדקה דבר, וזה כישלון שאומר מה לעשות; המסך כבה והתוכנה לא רצה
     * 3 שניות ויותר — כישלון; אחרת — עבר.
     */
    static final int STEADY_OK = 0, STEADY_SCREEN_ON = 1, STEADY_STOPPED = 2;
    static final long STEADY_MIN_OFF = 10000, STEADY_MAX_MISS = 3000;

    static int steady(long screenOffMs, long missedMs) {
        if (missedMs >= STEADY_MAX_MISS) return STEADY_STOPPED;
        return screenOffMs < STEADY_MIN_OFF ? STEADY_SCREEN_ON : STEADY_OK;
    }

    /**
     * מבנה 45: קובץ שהמנוע כותב בעצמו — דוח, יומן, "כתוב" — לא על קובץ של התוכנה (הגדרות, גיבויים, אינדקס), ולא על תיקייה.
     * עד כאן לא נבדק: "דוח" ו"/יומן:" כתבו לכל מסלול שנכתב בשורה. null — מותר.
     */
    static String noWriteHere(String p) {
        String why = Physical.programFile(p);
        if (why != null) return why;
        return new File(p).isDirectory() ? Words.DbCmd.WRITE_IS_FOLDER : null;
    }

    // ---------- קריאה ----------

    // ---------- קריאת שורה: משתנים, מתגים, מסלולים (המילים עצמן — Syntax, מבנה 159) ----------

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
            out.add(new AsIfRow("▶", Parse.isHome(plan.start) ? Words.Cmd.HOME_AT_END : plan.start.getName(), Words.AsIf.AT_END));
        if (out.isEmpty()) out.add(new AsIfRow("·", Words.AsIf.NOTHING, ""));
        return out;
    }

    // ---------- תכנון ----------

    /** מ0.27: יעד שנמחק בשורה קודמת (RD) — ההשוואה מולו היא מול תיקייה ריקה, לא מול מה שעוד בכונן בזמן התכנון. */
    static final File GHOST = new File("/\u0000none");

    /** מתכנן את כל הקובץ, בלי לשנות דבר. */
    /** מתכנן את כל הקובץ, בלי לשנות דבר. כל שורה מכירה את מה שהשורות שלפניה יעשו (118). */
    static Plan plan(String text, File here, Map<String, String> vars) {
        return plan(text, here, vars, null);
    }

    /** 1.20.0: עם האינדקס — ההשוואה לוקחת ממנו חתימות שכבר חושבו. */
    static Plan plan(String text, File here, Map<String, String> vars, Library lib) {
        return plan(text, here, vars, lib, null);
    }

    /** 1.25.4: עם עצירה — ההכנה עצמה יכולה לקחת זמן (לולאות, השוואות של רשימות גדולות). */
    static Plan plan(String text, File here, Map<String, String> vars, Library lib, Physical.Progress progress) {
        return plan(text, here, vars, lib, progress, null);
    }

    /**
     * מבנה 73 (שלב ב: "כל פעולה של מסך עוברת דרך פקודה"): מה שנבחר במסך — קבצים ותיקיות — נמסר לקובץ כיחידה אחת, ו־%נבחרים%
     * הוא כולו: "מחק %נבחרים%" הוא שורה אחת שמוחקת את כל מה שנבחר (פעולה אחת בכונן, מעבר אחד על הרשימות), כמו שהמסך עושה.
     * (ברשימת "בחירה" של המשתמש %נבחרים% עדיין נפרש לשורה לכל קובץ — Choice.expand; שם אין selected, והשורות מגיעות לכאן כבר פרושות.)
     */
    static final class Selection {
        final List<String> files, dirs;
        /** מבנה 77: התיקייה שנבחרה כיעד (העתקה, העברה) — %יעד%; null — אין. */
        final String dest;
        /** מבנה 78: השם שהוקלד במסך (שינוי שם) — %שם-חדש%; null — אין. */
        final String name;
        /** מבנה 80: התוכן של קובץ חדש — %תוכן%; null — אין. */
        final String content;
        /**
         * מבנה 85: בחירה במסך הרשימות — במסד פריט מזוהה במספר שלו, לא במסלול: האופן, הרשימה שהפריטים מוצגים בה (0 — אין), והמספרים.
         * ids == null — הבחירה היא של הכונן (files, dirs).
         */
        String mode;
        long in;
        List<Long> ids;
        /** מבנה 116: הרשימה שאליה מדביקים (הדבקה מהלוח של הרשימות); 0 — אין. */
        long to;

        Selection(String mode, long in, List<Long> ids) {
            this(new ArrayList<String>(), new ArrayList<String>(), null, null, null);
            this.mode = mode;
            this.in = in;
            this.ids = new ArrayList<Long>(ids);
        }

        /** מבנה 97: מה שבטופס (רשימה חכמה): מיון, תצוגה, צלמית, תיאור, "למה" — לפי Lists.SMART_FORM; null — אין טופס. */
        java.util.Map<String, String> props;

        /** מבנה 97: רשימה חכמה חדשה מהטופס — השם (%שם-חדש%), התנאי (%תוכן%) ושאר מה שבטופס; כלום מהם אינו מפוענח. */
        Selection(String mode, long in, String name, String cond, java.util.Map<String, String> props) {
            this(new ArrayList<String>(), new ArrayList<String>(), null, name, cond);
            this.mode = mode;
            this.in = in;
            this.ids = new ArrayList<Long>();
            this.props = props == null ? new java.util.HashMap<String, String>() : new java.util.HashMap<String, String>(props);
        }

        /** מבנה 91: יצירה במסך הרשימות — האופן, הרשימה שבתוכה יוצרים (0 — הבית), והשם שהוקלד (אינו מפוענח). */
        Selection(String mode, long in, String name) {
            this(new ArrayList<String>(), new ArrayList<String>(), null, name, null);
            this.mode = mode;
            this.in = in;
            this.ids = new ArrayList<Long>();
        }

        Selection(List<String> files, List<String> dirs) {
            this(files, dirs, null, null);
        }

        Selection(List<String> files, List<String> dirs, File dest) {
            this(files, dirs, dest, null);
        }

        Selection(List<String> files, List<String> dirs, File dest, String name) {
            this(files, dirs, dest, name, null);
        }

        Selection(List<String> files, List<String> dirs, File dest, String name, String content) {
            this.files = new ArrayList<String>(files);
            this.dirs = new ArrayList<String>(dirs);
            this.dest = dest == null ? null : dest.getAbsolutePath();
            this.name = name;
            this.content = content;
        }
    }

    static final String V_SELECTED = "נבחרים", SELECTED_MARK = "/\u0000selected";   // תחביר
    /** מבנה 77: היעד שנבחר במסך. נמסר כמשתנה ולא ככתוב — שם של תיקייה יכול להכיל % , והערך של משתנה אינו נפתח שוב. */
    static final String V_DEST = "יעד";   // תחביר
    /**
     * מבנה 78: השם שהוקלד במסך. נמסר כסימן, והשם עצמו נלקח מהבחירה — כך הוא אינו מפוענח בכלל: שם של קובץ יכול להכיל % ,
     * ואפילו טקסט שנראה כמו משתנה ("%תאריך%"), ובשורה כתובה המשתנים שבשם החדש מוחלפים (מבנה 43).
     */
    static final String V_NEW_NAME = "שם-חדש", NAME_TOKEN = "\u0000name", NAME_MARK = "/" + NAME_TOKEN;   // תחביר
    /** מבנה 80: התוכן של קובץ חדש מהמסך — גם הוא סימן, ונלקח מהבחירה כלשונו (בשורה כתובה, ב"כתוב", המשתנים מוחלפים ו־\n הוא שורה חדשה). */
    static final String V_CONTENT = "תוכן", CONTENT_MARK = "/\u0000content";   // תחביר

    static Plan plan(String text, File here, Map<String, String> vars, Library lib, Physical.Progress progress, Selection selected) {
        Plan plan = new Plan();
        Sim sim = new Sim();
        sim.lib = lib;
        sim.selected = selected;
        if (selected != null) {
            vars = new LinkedHashMap<String, String>(vars);
            vars.put(V_SELECTED, SELECTED_MARK);
            if (selected.dest != null) vars.put(V_DEST, selected.dest);
            if (selected.name != null) vars.put(V_NEW_NAME, NAME_MARK);
            if (selected.content != null) vars.put(V_CONTENT, CONTENT_MARK);
        }
        plan.sim = sim;
        String[] lines = Bidi.strip(text).replace("\r", "").split("\n");          // 204
        lines = Loops.calls(lines, here, vars, 0, new java.util.HashSet<String>());  // מ0.15: Syntax.CALL — השורות של הקובץ במקומה
        List<Loops.Ln> all = Loops.expand(lines, plan);                                    // 1.25.0: לולאות
        Loops.Clock clock = new Loops.Clock();
        int mark = 0;
        Loops.Ln prev = null;
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
            Loops.Ln ln = all.get(li);
            prev = ln;
            if (clock.skip(plan, ln.loops, ln.rounds)) continue;               // לולאה שעברה את הגבול — גם בתכנון
            int n = ln.num - 1;
            final String shown = Parse.clean(ln.text);                                 // מ0.38: מה שנכתב (לדוח) — גם כשהוא בעברית
            String line = Commands.normal(shown);
            if (ln.error != null) {
                Step s = new Step();
                s.line = ln.num;
                s.text = shown;
                s.cmd = Syntax.LOOP;
                s.error = ln.error;
                plan.errors++;
                plan.steps.add(s);
                continue;
            }
            if (line.isEmpty() || line.startsWith("::")) continue;
            if (line.equals(Syntax.AS_IF) || line.equals(Syntax.AS_IF2)) {                  // 1.10.0: שורת כותרת — כל הקובץ בהדמיה
                plan.asIf = true;
                continue;
            }
            if (line.equals(Syntax.CHECK)) continue;                                  // 1.28.0: שורת כותרת — מסך ההרצה (BatchUi)
            if (line.equals(Syntax.MEASURE) || line.startsWith(Syntax.MEASURE + " ")) {     // 1.25.0: שורת כותרת — מדידה
                plan.measure = true;
                List<String> mt = Parse.tokens(line);
                for (int k = 0; k < mt.size(); k++) {
                    String x = mt.get(k);
                    if (x.startsWith(Syntax.SW_JOURNAL)) {
                        String[] err = {null};
                        plan.measureJournal = Parse.path(x.substring(Syntax.SW_JOURNAL.length()).trim(), here, vars, err);
                        if (plan.measureJournal != null && (err[0] = noWriteHere(plan.measureJournal)) != null) plan.measureJournal = null;   // מבנה 45
                        if (plan.measureJournal == null) {                           // מבנה 43: מסלול שגוי — שגיאה, לא "בלי יומן" בשקט
                            Step s = new Step();
                            s.line = n + 1;
                            s.text = shown;
                            s.cmd = Syntax.MEASURE;
                            s.error = err[0];
                            plan.errors++;
                            plan.steps.add(s);
                        }
                    } else if (x.startsWith(Syntax.SW_LIMIT)) {                         // מ0.45: מגבלת זמן לכל שורה
                        String v = x.substring(Syntax.SW_LIMIT.length());
                        if (k + 1 < mt.size() && !mt.get(k + 1).isEmpty() && !Character.isDigit(mt.get(k + 1).charAt(0))
                                && !mt.get(k + 1).startsWith("/") && Loops.limitMs(v + mt.get(k + 1)) > 0) v += mt.get(++k);   // "/עד:2 דקות"
                        plan.lineLimit = Loops.limitMs(v);
                        if (plan.lineLimit <= 0) {
                            plan.lineLimit = 0;
                            Step s = new Step();
                            s.line = n + 1;
                            s.text = shown;
                            s.cmd = Syntax.MEASURE;
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
                s.cmd = Syntax.LAB;
                planLabLine(s);
                if (s.error != null) plan.errors++;
                plan.steps.add(s);
                continue;
            }
            if (Verbs.isVerb(line)) {                                         // 1.13.0: פקודות הפתיחה בעברית — נפתחות בסוף
                Step s = new Step();
                s.line = n + 1;
                s.text = shown;
                s.cmd = Syntax.OPEN;
                s.open = Verbs.withVars(line, vars);
                Verbs.Cmd vc = Verbs.parse(s.open);
                s.error = vc.error;
                s.describe = Verbs.describe(vc);
                String doneIn = null;
                if (s.error == null && Verbs.K_MODE.equals(vc.kind)) DbListCmd.planMode(s, vc.arg, vc.fromBackup, sim);   // מ0.42: צור אופן
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
            List<String> tk = Parse.tokens(line);
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
            if (cmd.equals(Syntax.CALL)) {                                             // מ0.15: לא נכלל (לא נמצא / מעגל / עומק)
                s.error = Words.DbCmd.CALL_FAILED;
                plan.errors++;
                plan.steps.add(s);
                continue;
            }
            if (cmd.equals(Syntax.CONT)) {                                             // מ0.30: המשך [N] — הקובץ רץ גם כשיש שורות שגיאה
                int allow = Parse.allowOf(line);
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
            if (SmallCmds.small(plan, s, cmd, tk, here, vars, sim)) {                // מ0.37: הפקודות הקטנות
                if (s.error != null) plan.errors++;
                plan.steps.add(s);
                continue;
            }
            if (cmd.equals(Syntax.MODE)) {                                             // מ0.15: אופן — מכאן והלאה, במסד
                String m = tk.size() == 2 ? tk.get(1).trim() : "";
                if (m.isEmpty()) s.error = Words.DbCmd.MODE_USAGE;
                else if (!DbVerbs.active()) s.error = Words.DbCmd.VERIFY_DB_ONLY;
                else if (!sim.hasMode(m))   // מ0.42: גם מה ש"צור אופן" שלפניו יצר; מ0.45: בלי מה ש"מחק אופן" מחק
                    s.error = sim.modeError = Words.DbCmd.noMode(m, sim.modes());   // מבנה 66: עם מה שהקובץ עצמו יצר, בלי מה שביטל
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
                ListLines.planList(s, cmd, lp, lsw, here, vars, sim);
                if (s.error != null) plan.errors++;
                plan.steps.add(s);
                continue;
            }
            if (lp == null && cmd.equals(Syntax.VERIFY) && tk.size() >= 2) {          // מ0.18: ודא "קובץ" קיים — בלי מילת סוג
                List<String> conds = new ArrayList<String>(tk.subList(2, tk.size()));
                String[] err = {null};
                String p = Parse.path(tk.get(1), here, vars, err);
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
            for (int i = 1; i < tk.size(); i++) (Parse.isSwitch(tk.get(i)) ? sw : args).add(tk.get(i));
            String[] err = {null};
            List<String> paths = new ArrayList<String>();
            for (String a : args) {
                String p = Parse.path(a, here, vars, err);
                if (p == null) break;
                paths.add(p);
            }
            if (err[0] == null)                                                // 1.19.0: /יומן:<קובץ>
                for (String x : sw) {
                    if (!x.startsWith(Syntax.SW_JOURNAL)) continue;
                    String v = x.substring(Syntax.SW_JOURNAL.length()).trim();
                    if (v.isEmpty() || !(cmd.equals("COPY") || cmd.equals("MOVE"))) {
                        err[0] = Words.Cmd.JOURNAL_USAGE;
                        break;
                    }
                    s.journal = Parse.path(v, here, vars, err);
                    if (s.journal == null) break;
                    if ((err[0] = noWriteHere(s.journal)) != null) break;       // מבנה 45
                }
            if (err[0] == null) err[0] = FileCmds.setsMisuse(cmd, paths);            // 1.20.0
            if (err[0] == null) err[0] = FileCmds.shareMisuse(cmd, paths);           // 203
            if (err[0] == null) err[0] = FileCmds.homeMisuse(cmd, paths);            // 1.9.0
            if (err[0] == null && cmd.equals("REN") && args.size() == 2 && args.get(1).indexOf('%') >= 0)
                args.set(1, Verbs.withVars(args.get(1), vars));                 // מבנה 43: גם בשם החדש המשתנים מוחלפים (עד כאן: נכתב "%שם%" כלשונו)
            if (err[0] != null) s.error = err[0];
            else FileCmds.planFile(s, cmd, args, paths, sw, sim);
            if (s.error != null) plan.errors++;
            if (cmd.equals("START") && s.error == null) plan.start = s.dest;
            plan.steps.add(s);
        }
        RunReport.expectErrors(plan);                                                    // מ0.37: "ודא שגיאה"
        return plan;
    }

    /** /עד:20% או /עד:20 — המספר; -1 אם אינו מספר בין 0 ל־100. */
    static int limit(String sw) {
        String v = sw.substring(Syntax.SW_LIMIT.length()).trim();
        if (v.endsWith("%")) v = v.substring(0, v.length() - 1).trim();
        try {
            int n = Integer.parseInt(v);
            return n >= 0 && n <= 100 ? n : -1;
        } catch (Throwable t) {
            return -1;
        }
    }

    static String join(List<String> parts, String sep) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) sb.append(sb.length() > 0 ? sep : "").append(p);
        return sb.toString();
    }

    /** קובץ מוסתר (שם שמתחיל בנקודה) — כמו ב-bat, רק עם /A:H (135). */
    static boolean visible(Step s, File x) {
        return s.hidden || !x.getName().startsWith(".");
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
        plan.pulse = new Runner.Pulse();                                               // מבנה 38: כמה זמן התוכנה לא רצה. מבנה 55: תמיד, לא רק עם "מדידה"
        try {
            List<String> rep = Runner.run0(plan, lib, asker, progress, grey, listen);
            Runner.stoppedHint(rep, plan.pulse.missed());
            return rep;
        } finally {
            plan.pulse.stop();
        }
    }

    /**
     * 1.28.0 (המשתמש: "את הבדיקות שהוספת לאחרונה תשים באופציה בפקודה — קובץ שירצה יריץ את הבדיקות האלה; אין צורך שכל
     * פקודה תעבור את המבחן הזה תמיד"): מסך ההרצה — רק לקובץ שיש בו שורת "בדיקה".
     */
    static boolean wantsCheck(String text) {
        for (String raw : Bidi.strip(text).replace("\r", "").split("\n")) if (Parse.clean(raw).equals(Syntax.CHECK)) return true;
        return false;
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
                Parse.var(Syntax.V_INTERNAL) + "  " + Parse.var(Syntax.V_CARD) + "  " + Parse.var(Syntax.V_DOWNLOADS) + "  " + Parse.var(Syntax.V_HERE) + "  " + Parse.var(Syntax.V_SHARE) + "  " + Parse.var(Syntax.V_HOME)
                        + "  " + Parse.var(Syntax.V_NOW) + "  " + Parse.var(Syntax.V_CARD_ID),
                "/" + Syntax.SW_DATE + "יום  /" + Syntax.SW_DATE + "<שנה  /" + Syntax.SW_SIZE + ">10MB",   // תחביר
                Syntax.SW_TAKEN + Syntax.TAKEN_ASK, Syntax.SW_TAKEN + Syntax.TAKEN_SKIP,
                "MOVE \"" + Parse.var(Syntax.V_DOWNLOADS) + "\\*.pdf\" \"" + Parse.var(Syntax.V_INTERNAL) + "\\ספרים\" /" + Syntax.SW_DATE + "שבוע");   // תחביר
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
