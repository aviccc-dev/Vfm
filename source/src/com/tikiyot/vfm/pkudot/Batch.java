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
                planList(s, cmd, lp, lsw, here, vars, sim);
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
            if (err[0] == null) err[0] = setsMisuse(cmd, paths);            // 1.20.0
            if (err[0] == null) err[0] = shareMisuse(cmd, paths);           // 203
            if (err[0] == null) err[0] = homeMisuse(cmd, paths);            // 1.9.0
            if (err[0] == null && cmd.equals("REN") && args.size() == 2 && args.get(1).indexOf('%') >= 0)
                args.set(1, Verbs.withVars(args.get(1), vars));                 // מבנה 43: גם בשם החדש המשתנים מוחלפים (עד כאן: נכתב "%שם%" כלשונו)
            if (err[0] != null) s.error = err[0];
            else plan(s, cmd, args, paths, sw, sim);
            if (s.error != null) plan.errors++;
            if (cmd.equals("START") && s.error == null) plan.start = s.dest;
            plan.steps.add(s);
        }
        RunReport.expectErrors(plan);                                                    // מ0.37: "ודא שגיאה"
        return plan;
    }

    /** 1.24.0: שורה עם רשימה — הצד שבכונן מפוענח כאן (מסלול, תבנית, סינון), והשאר — ListCmd. */
    private static void planList(Step s, String cmd, ListCmd.Parsed lp, List<String> sw, File here, Map<String, String> vars, Sim sim) {
        if (cmd.equals(Syntax.VERIFY)) {                                               // מ0.14: ודא — הצד הראשון רשימה, השאר תנאים
            if (!DbVerbs.active()) { s.error = Words.DbCmd.VERIFY_DB_ONLY; return; }
            if (lp.sides.size() >= 2 && !lp.sides.get(1).list()
                    && (DbListCmd.W_SAME.equals(lp.sides.get(1).raw) || DbListCmd.W_EQUAL.equals(lp.sides.get(1).raw))) {   // מבנה 87–88: … זהה … · … שווה …
                if (lp.sides.size() != 3 || !sw.isEmpty() || !lp.sides.get(0).list() || !lp.sides.get(2).list()) {
                    s.error = Words.DbCmd.VERIFY_SAME_USAGE;
                    return;
                }
                ListCmd.resolve(lp.sides.get(0), vars);
                ListCmd.resolve(lp.sides.get(2), vars);
                DbListCmd.planVerifySame(s, lp.sides.get(0), lp.sides.get(2), DbListCmd.W_SAME.equals(lp.sides.get(1).raw), sim);
                return;
            }
            List<String> conds = new ArrayList<String>(sw);
            for (int i = 1; i < lp.sides.size(); i++) conds.add(lp.sides.get(i).raw);
            ListCmd.Side a = lp.sides.get(0);
            if (a.list()) ListCmd.resolve(a, vars);
            else {                                                              // מ0.18: ודא "קובץ" קיים / חסר — בכונן
                String[] err = {null};
                String p = Parse.path(a.raw, here, vars, err);
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
        List<String> fsDirs = new ArrayList<String>();                         // מבנה 115: תיקייה שלמה מהכונן אל רשימה
        boolean dirToList = cmd.equals("COPY") && lp.sides.size() == 2 && lp.sides.get(1).list();
        File fsDest = null;
        for (int i = 0; i < lp.sides.size(); i++) {
            ListCmd.Side x = lp.sides.get(i);
            if (x.list()) {
                if (Parse.var(V_SELECTED).equals(x.raw)) x.picked = true;             // מבנה 85: מה שנבחר במסך הרשימות — מספרים, לא דרך של שמות
                else ListCmd.resolve(x, vars);
                continue;
            }
            String[] err = {null};
            String p = Parse.path(x.raw, here, vars, err);
            if (p == null) { s.error = err[0]; return; }
            if (p.equals(SELECTED_MARK)) {                                      // מבנה 114: הקבצים שנבחרו במסך — אל רשימה
                Selection sel = sim.selected;
                if (i != 0 || sel == null || (sel.files.isEmpty() && sel.dirs.isEmpty())) { s.error = Words.Cmd.NOTHING_SELECTED; return; }
                if (!sel.dirs.isEmpty() && !dirToList) { s.error = Words.ListCmdW.FOLDER_TO_LIST; return; }
                for (String f : sel.files) {
                    if (!sim.isFile(new File(f))) { s.error = Words.Cmd.notFound(f); return; }
                    fsFiles.add(new File(f).getAbsolutePath());
                }
                for (String d : sel.dirs) {                                     // מבנה 115
                    if (!sim.isDir(new File(d))) { s.error = Words.FOLDER_NOT_FOUND + ": " + d; return; }
                    fsDirs.add(new File(d).getAbsolutePath());
                }
                continue;
            }
            // מבנה 115 (הכלל של מבנה 112): שם של תיקייה הוא התיקייה עצמה — אל רשימה היא נכנסת כרשימה בשמה, עם המבנה שלה
            // (עד כאן: הקבצים שבה). רק הקבצים שבה: "תיקייה\*".
            if (dirToList && i == 0 && !new File(p).getName().contains("*") && !new File(p).getName().contains("?") && sim.isDir(new File(p))) {
                fsDirs.add(new File(p).getAbsolutePath());
                continue;
            }
            if (i == lp.sides.size() - 1 && i > 0) fsDest = new File(p);
            else {
                Filter flt = Filter.of(sw);
                if (flt.error != null) { s.error = flt.error; return; }
                match(s, p, flt, false, sim);
                if (s.error != null) return;
                if (!s.dirs.isEmpty()) { s.error = Words.ListCmdW.FOLDER_TO_LIST; return; }
                fsFiles.addAll(s.files);
                s.files.clear();
            }
        }
        if (DbVerbs.active()) DbListCmd.plan(s, cmd, lp, sw, sim, fsFiles, fsDest, fsDirs);   // מ0.12: עובדים במסד
        else s.error = Words.DbCmd.NO_DB;                                    // מ0.26: המנוע הישן נמחק — רשימות רק במסד
    }

    /**
     * מבנה 115: תיקייה כמו שההדמיה רואה אותה (כולל מה ששורות קודמות יצרו) — לעץ מנותק, כמו Tree.scanFolder: עם המבנה
     * (תת־תיקיות — צמתים), או הקבצים בלבד (כולם, גם מתת־תיקיות, בצומת אחד).
     */
    static Tree.Node scan(Sim sim, Step s, File dir, boolean structure) {
        Tree.Node n = new Tree.Node();
        n.name = Names.cleanName(dir.getName());
        scanInto(n, n, dir, structure, sim, s, 0);
        return n;
    }

    private static void scanInto(Tree.Node node, Tree.Node flat, File dir, boolean structure, Sim sim, Step s, int depth) {
        for (File x : sim.files(dir))
            if (visible(s, x)) (structure ? node : flat).entries.add(new Tree.Entry(x.getAbsolutePath(), sim.real(x).length()));
        if (depth > 60) return;
        for (File d : sim.subdirs(dir)) {
            if (Vols.skip(d, false)) continue;
            scanInto(structure ? Tree.newChild(node, node.uniqueChildName(d.getName())) : node, flat, d, structure, sim, s, depth + 1);
        }
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
            if (x.startsWith("/" + Syntax.SW_DATE) || x.startsWith("/" + Syntax.SW_SIZE)) return Words.ListCmdW.NO_FILTER;
            if (x.startsWith("/" + Syntax.SW_CONTAINS)) { if (toFolder || fromDisk) continue; return Words.ListCmdW.NO_FILTER; }
            if (x.equals(ListCmd.SW_CONDS) || x.equals(ListCmd.SW_DIFF)) { if (cmd.equals("COMP")) continue; return Words.Cmd.switchNotHere(x, null); }
            boolean take = u.equals("/S") || x.equals(Syntax.SW_KEEP) || u.equals("/Y") || u.equals("/-Y") || x.startsWith(Syntax.SW_TAKEN);
            if (take && toFolder) continue;
            if (fromDisk && cmd.equals("COPY") && sd.get(1).list() && (x.equals(ListCmd.SW_LIVE) || x.equals(ListCmd.SW_FLAT)
                    || x.equals(Syntax.SW_TAKEN + Syntax.TAKEN_SKIP) || x.equals(Syntax.SW_TAKEN + Syntax.TAKEN_RENAME) || x.equals(Syntax.SW_TAKEN + Syntax.TAKEN_STOP)))
                continue;                                                       // מבנה 115: תיקייה מהכונן אל רשימה
            if (cmd.equals("MD") && (x.equals(Syntax.SW_TAKEN + Syntax.TAKEN_SKIP) || x.equals(Syntax.SW_TAKEN + Syntax.TAKEN_RENAME) || x.equals(Syntax.SW_TAKEN + Syntax.TAKEN_STOP)))
                continue;                                                       // מבנה 109: "צור תיקייה ידנית … /תפוס:לדלג"
            return Words.Cmd.switchNotHere(Commands.switchWord(x), null);
        }
        return null;
    }

    private static void plan(Step s, String cmd, List<String> args, List<String> paths, List<String> sw, Sim sim) {
        Filter flt = Filter.of(sw);
        if (flt.error != null) {
            s.error = flt.error;
            return;
        }
        if (!flt.contains.isEmpty()) s.contains = flt.contains;               // מ0.40
        // מ0.40 (ממצא 41; הכלל של המשתמש: "אין מצב שיש שגיאה והקובץ רץ ואף אחד לא יודע"): מתג שלא שייך לפקודה — שגיאה,
        // ולא בליעה בשקט. מה מותר — מההגדרה האחת (Commands). /Q ו־/F — תאימות ל־bat, מתעלמים בכוונה.
        Commands.Def def = Commands.get(cmd);
        boolean fromCmp = (cmd.equals("COPY") || cmd.equals("MOVE")) && paths.size() == 2
                && (paths.get(0).equals(Syntax.ONLY_A_MARK) || paths.get(0).equals(Syntax.ONLY_B_MARK));
        if (def != null) for (String x : sw) {
            String u = x.toUpperCase(Locale.ROOT);
            if (u.equals("/Q") || u.equals("/F")) continue;
            boolean ok = false;
            for (Commands.Param p : def.params) if (p.matches(x)) ok = true;
            if (!ok) { s.error = Words.Cmd.switchNotHere(Commands.switchWord(x), Commands.word(def)); return; }
            if (x.startsWith(Syntax.SW_LIMIT) && !fromCmp) { s.error = Words.Cmd.LIMIT_ONLY_COMPARE; return; }
        }
        if (fromCmp && !flt.none()) { s.error = Words.Cmd.NO_FILTER_ON_COMPARE; return; }
        for (String x : sw) {
            String u = x.toUpperCase(Locale.ROOT);
            if (u.equals("/Y") || x.equals(Syntax.SW_TAKEN + Syntax.TAKEN_REPLACE)) s.replace = true;
            if (x.equals(Syntax.SW_TAKEN + Syntax.TAKEN_SKIP)) s.skipTaken = true;
            if (x.equals(Syntax.SW_TAKEN + Syntax.TAKEN_STOP)) s.failTaken = true;          // 1.20.0
            if (x.equals(Syntax.SW_TAKEN + Syntax.TAKEN_RENAME)) s.renameTaken = true;      // מ0.40
            if (u.equals("/S") || x.equals(Syntax.SW_KEEP)) s.recursive = true;
            if (u.equals("/A") || u.startsWith("/A:H") || u.equals("/A:")) s.hidden = true;
            if (x.startsWith(Syntax.SW_TAKEN) && !Parse.takenValue(x)) s.error = Parse.takenUnknown(x);
            if (x.startsWith(Syntax.SW_LIMIT) && limit(x) < 0) s.error = Words.Cmd.switchUnknown(x);
            if (x.startsWith(Syntax.SW_LIST)) {                                      // 1.22.0
                s.listName = x.substring(Syntax.SW_LIST.length()).trim();
                if (s.listName.isEmpty() || !cmd.equals("COMP")) s.error = Words.Cmd.LIST_ONLY_COMP;
            }
            if (x.startsWith(Syntax.SW_STATE)) {                                     // 1.21.0
                s.stateWhen = State.when(x.substring(Syntax.SW_STATE.length()));
                if (s.stateWhen == Long.MIN_VALUE) s.error = Words.Cmd.switchUnknown(x);
                else if (!cmd.equals("COPY")) s.error = Words.Cmd.STATE_ONLY_COPY;
            }
        }
        if (s.error != null) return;
        if (cmd.equals("MD")) {
            if (paths.size() != 1) { s.error = Commands.usage(cmd); return; }
            File d = new File(paths.get(0));
            if (d.getName().equals(NAME_TOKEN)) {                               // מבנה 79: "%יעד%\%שם-חדש%" — תיקייה חדשה מהמסך
                makeSelected(s, cmd, d, sim);
                return;
            }
            for (String x : sw)                                                 // מבנה 109: "להחליף" / "לשאול" — לא לתיקייה; לא בולעים בשקט
                if (x.toUpperCase(Locale.ROOT).equals("/Y") || x.equals(Syntax.SW_TAKEN + Syntax.TAKEN_REPLACE) || x.equals(Syntax.SW_TAKEN + Syntax.TAKEN_ASK)) {
                    s.error = Words.Cmd.MD_TAKEN_ONLY;
                    return;
                }
            if (sim.isDir(d) && s.renameTaken) {                               // מבנה 109: /תפוס:לשנות-שם — בשם פנוי "(2)"
                File free = d;
                for (int k = 2; sim.isDir(free) || sim.isFile(free); k++) free = new File(d.getParentFile(), d.getName() + " (" + k + ")");
                d = free;
            } else if (sim.isDir(d)) {
                if (s.skipTaken) {                                              // מבנה 109: /תפוס:לדלג — "המשך לשורה הבאה"
                    s.noop = true;
                    s.dest = d;
                    s.describe = Words.Cmd.folderSkipped(d.getName());
                    return;
                }
                s.error = Words.Cmd.folderTaken(d.getName());                  // מבנה 109 (המשתמש): "סירוב, אלא אם כן יש הוראה להמשיך"
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
            if (paths.get(0).equals(SELECTED_MARK)) {                           // מבנה 73: מה שנבחר במסך — קבצים, וגם תיקיות שלמות
                selected(s, flt, sim, cmd);
                if (s.error != null) return;
                List<String> what = new ArrayList<String>();
                if (!s.files.isEmpty() || s.dirs.isEmpty()) what.add(Words.files(s.files.size()));
                if (!s.dirs.isEmpty()) what.add(Words.folders(s.dirs.size()));
                s.describe = Words.Cmd.toTrash(join(what, Words.Cmd.AND)) + filtered(s);
                for (String d : s.dirs) sim.wipe(d);
            } else {
                match(s, paths.get(0), flt, false, sim);
                if (s.error == null) s.describe = Words.Cmd.toTrash(Words.files(s.files.size())) + filtered(s);
            }
            for (String f : s.files) {
                sim.gone.add(f);
                sim.added.remove(f);
            }
        } else if ((cmd.equals("COPY") || cmd.equals("MOVE")) && !paths.isEmpty() && paths.get(0).equals(SELECTED_MARK)) {
            fromSelected(s, cmd, paths, flt, sim);                               // מבנה 77: מה שנבחר במסך — אל תיקייה
        } else if (cmd.equals("COPY") && paths.size() == 2 && paths.get(1).equals(Syntax.SHARE_MARK)) {   // 203: לשיתוף
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
            CompareCmds.restore(s, args, paths, flt, sim);
        } else if (cmd.equals("COMP")) {                                      // 1.20.0
            CompareCmds.comp(s, args, paths, sim);
        } else if ((cmd.equals("COPY") || cmd.equals("MOVE")) && paths.size() == 2
                && (paths.get(0).equals(Syntax.ONLY_A_MARK) || paths.get(0).equals(Syntax.ONLY_B_MARK))) {
            CompareCmds.fromCompare(s, cmd, paths, sw, sim);
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
            // מבנה 112 (ה7, המשתמש: "כמו במסך"): שם של תיקייה הוא התיקייה עצמה — גם בהעתקה (עד כאן: בהעתקה — הקבצים שבתוכה;
            // בהעברה ומהמסך — התיקייה). מה שבתוכה בלבד: "תיקייה\*".
            match(s, paths.get(0), flt, true, sim);
            if (s.error != null) return;
            if (!s.dirs.isEmpty() && !flt.none()) {            // 121: סינון לא חל על תיקייה שלמה
                s.error = Words.Cmd.filterOnFolder(cmd + " \"" + args.get(0) + "\\*.*\" …");
                return;
            }
            for (String d : s.dirs)                                             // מבנה 43: תיקייה לתוך עצמה — כבר בתכנון (עד כאן: רק בהרצה)
                if (Physical.inside(dest, new File(d))) { s.error = Words.Disk.NOT_INTO_ITSELF; return; }
            arrange(s, cmd.equals("COPY"), dest, sim);
        } else if (cmd.equals("REN")) {
            if (paths.size() != 2 || args.get(1).contains("/") || args.get(1).contains("\\")) {
                s.error = Commands.usage(cmd);
                return;
            }
            if (paths.get(0).equals(SELECTED_MARK) || paths.get(1).equals(NAME_MARK)) {   // מבנה 78: מה שנבחר במסך, בשם שהוקלד בו
                renameSelected(s, cmd, paths, sim);
                return;
            }
            File f = new File(paths.get(0));
            if (args.get(0).contains("*") || args.get(0).contains("?")) { s.error = Words.Cmd.REN_WILD; return; }
            if (sim.isDir(f)) { s.error = Words.Cmd.REN_FOLDER; return; }                  // 1.22.5
            if (!sim.isFile(f)) { s.error = Words.FILE_NOT_FOUND + ": " + args.get(0); return; }
            String bad = Physical.badName(args.get(1));
            if (bad != null) { s.error = bad; return; }
            File to = new File(f.getParentFile(), args.get(1));
            if (!to.getName().equalsIgnoreCase(f.getName()) && (sim.isFile(to) || sim.isDir(to))) {   // מבנה 43: גם תיקייה בשם הזה
                s.error = Words.Cmd.alreadyHas(args.get(1));
                return;
            }
            s.files.add(f.getAbsolutePath());
            s.newName = args.get(1);
            s.describe = Words.Cmd.rename(f.getName(), s.newName);
            sim.move(f.getAbsolutePath(), to, false);
        } else if (cmd.equals("START")) {
            if (paths.isEmpty()) { s.error = Commands.usage(cmd); return; }
            if (paths.get(paths.size() - 1).equals(Syntax.HOME_MARK)) {      // 1.9.0: START %בית%
                s.dest = new File(Syntax.HOME_MARK);
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
                    if (Parse.wild(name, x.getName()) && visible(s, x)) {
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

    /** מבנה 48: היעד הוא התיקייה שהפריט כבר בה — לפי המסלול, ואם לא — לפי הכונן (Physical.sameFile), פעם אחת לכל תיקייה. */
    private static boolean here(File dest, File parent, Map<String, Boolean> asked) {
        if (parent == null) return false;
        if (dest.equals(parent)) return true;
        Boolean b = asked.get(parent.getAbsolutePath());
        if (b == null) asked.put(parent.getAbsolutePath(), b = dest.isDirectory() && parent.isDirectory() && Physical.sameFile(dest, parent));
        return b;
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
            if (mask != null && !Parse.wild(mask, x.getName())) continue;
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

    /** 1.20.0: %רק-במקור% / %רק-ביעד% — רק לבדם, כמקור של COPY או MOVE. */
    private static String setsMisuse(String cmd, List<String> paths) {
        for (int i = 0; i < paths.size(); i++) {
            String p = paths.get(i);
            if (p.startsWith(SELECTED_MARK)) {                                  // מבנה 73, 77: %נבחרים% כיחידה — רק לבדו, כמקור של "מחק", "העתק", "העבר"
                if (!(cmd.equals("DEL") || cmd.equals("COPY") || cmd.equals("MOVE") || cmd.equals("REN")) || i != 0 || !p.equals(SELECTED_MARK))
                    return Words.Cmd.SELECTED_ONLY_SOURCE;
                continue;
            }
            if (!p.startsWith(Syntax.ONLY_A_MARK) && !p.startsWith(Syntax.ONLY_B_MARK)) continue;
            if (!(cmd.equals("COPY") || cmd.equals("MOVE")) || i != 0 || !(p.equals(Syntax.ONLY_A_MARK) || p.equals(Syntax.ONLY_B_MARK)))
                return Words.Cmd.SETS_ONLY_SOURCE;
        }
        return null;
    }

    /** 1.9.0: "%בית%" — רק לבדו, אחרי START. */
    private static String homeMisuse(String cmd, List<String> paths) {
        for (String p : paths) {
            if (!p.startsWith(Syntax.HOME_MARK)) continue;
            if (!cmd.equals("START") || !p.equals(Syntax.HOME_MARK)) return Words.Cmd.HOME_ONLY_START;
        }
        return null;
    }

    /** 203: "%שיתוף%" — רק כיעד של COPY, ובלי מסלול אחריו. */
    private static String shareMisuse(String cmd, List<String> paths) {
        for (int i = 0; i < paths.size(); i++) {
            String p = paths.get(i);
            if (!p.startsWith(Syntax.SHARE_MARK)) continue;
            if (!p.equals(Syntax.SHARE_MARK)) return Words.Cmd.SHARE_NO_PATH;
            if (cmd.equals("MOVE") && i == 1) return Words.Cmd.SHARE_NO_MOVE;
            if (!cmd.equals("COPY") || i != 1) return Words.Cmd.SHARE_ONLY_COPY;
        }
        return null;
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

    /** מקור: קובץ, תבנית (*.pdf), או תיקייה (COPY — הקבצים שבתוכה; MOVE — התיקייה עצמה, כמו ב-bat). */
    private static void match(Step s, String src, Filter flt, boolean moveDir, Sim sim) {
        File f = new File(src);
        String name = f.getName();
        if (name.contains("*") || name.contains("?")) {
            File dir = f.getParentFile();
            if (dir == null || !sim.isDir(dir)) { s.error = Words.FOLDER_NOT_FOUND + ": " + (dir == null ? src : dir.getAbsolutePath()); return; }
            for (File x : sim.files(dir))
                if (Parse.wild(name, x.getName()) && visible(s, x)) {
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

    /**
     * מבנה 77: סוף התכנון של העתקה או העברה אל תיקייה — מה יוצא מהשורה (כבר במקום, /תפוס:), ההדמיה, והתיאור. מקום אחד לשורה
     * כתובה ול"נבחרים" של המסך.
     */
    private static void arrange(Step s, boolean copy, File dest, Sim sim) {
        // מ0.40: העברה אל התיקייה שהפריט כבר בה — כלום (עד כאן: "קובץ אחד" בתכנון, ו"כבר בתיקייה הזו" ככישלון בהרצה).
        // ובקובץ אחרי קובץ — כדי שקובץ שני זהה לראשון יראה אותו כבר ביעד (לשנות-שם).
        // מבנה 48: "כבר בתיקייה הזו" — גם כשהיעד הוא אותה תיקייה בכתיב אחר (קישור; אותיות גדולות וקטנות בכונן שאינו מבדיל).
        // המשפך: השאלה לכונן — פעם אחת לכל תיקיית מקור, ורק בהעברה.
        Map<String, Boolean> sameDir = new java.util.HashMap<String, Boolean>();
        for (java.util.Iterator<String> it = s.files.iterator(); it.hasNext(); ) {
            File f = new File(it.next());
            if (!copy && here(dest, f.getParentFile(), sameDir)) { it.remove(); s.inPlace++; continue; }
            File to = new File(dest, f.getName());
            if (s.drop(sim, f, to)) { it.remove(); continue; }
            sim.move(f.getAbsolutePath(), s.arrive(sim, to), copy);
        }
        for (java.util.Iterator<String> it = s.dirs.iterator(); it.hasNext(); ) {
            File d = new File(it.next());
            if (!copy && here(dest, d.getParentFile(), sameDir)) { it.remove(); s.inPlace++; continue; }
            File to = new File(dest, d.getName());
            if (s.skipTaken && sim.isDir(to)) { it.remove(); s.skipped++; continue; }
            File at = s.renameTaken && (sim.isDir(to) || sim.isFile(to)) ? sim.free(to) : to;
            if (copy) sim.copyDir(d, at);                                        // מבנה 77: תיקייה שלמה בהעתקה — רק מ"נבחרים"
            else sim.moveDir(d, at, s.replace);                                  // מבנה 44
        }
        List<String> what = new ArrayList<String>();
        if (!s.files.isEmpty() || s.dirs.isEmpty()) what.add(Words.files(s.files.size()));
        if (!s.dirs.isEmpty()) what.add(Words.folders(s.dirs.size()));
        s.describe = Words.Cmd.transfer(copy, join(what, Words.Cmd.AND), Bidi.path(dest.getAbsolutePath())) + filtered(s);
        s.noop = s.files.isEmpty() && s.dirs.isEmpty() && s.same + s.inPlace > 0;   // הכול כבר במקום — לא שגיאה, ואין מה לעשות
    }

    /**
     * מבנה 79: תיקייה חדשה מהמסך — בתיקייה שנבחרה (%יעד%), בשם שהוקלד (%שם-חדש%, נלקח מהבחירה ואינו מפוענח). שלא כמו
     * "צור תיקייה" כתובה (שבה תיקייה קיימת היא "בלי שינוי", ומסלול יוצר גם את מה שמעליו) — כאן, כמו שהמסך עשה: שם אחד,
     * ושם שכבר קיים הוא סירוב. הסירובים (שם לא תקין, תיקיית התוכנה, כבר קיים) — בתכנון ובנוסח של המסך.
     */
    private static void makeSelected(Step s, String cmd, File d, Sim sim) {
        Selection sel = sim.selected;
        File parent = d.getParentFile();
        if (sel == null || sel.name == null || sel.dest == null || parent == null || !parent.getAbsolutePath().equals(Paths.clean(sel.dest))) {
            s.error = Commands.usage(cmd);
            return;
        }
        String bad = Physical.badName(sel.name);
        if (bad != null) { s.error = bad; return; }
        File made = new File(parent, sel.name.trim());
        if (Physical.programFile(made.getAbsolutePath()) != null) { s.error = Words.Disk.NO_FOLDER_IN_APP; return; }
        if (sim.isDir(made) || sim.isFile(made)) { s.error = Words.Disk.ALREADY_HERE_NAME + made.getName() + "\""; return; }
        s.dest = made;
        s.describe = Words.Cmd.newFolder(Bidi.path(made.getAbsolutePath()));
        sim.dirs.add(made.getAbsolutePath());
        sim.gone.remove(made.getAbsolutePath());
    }

    /**
     * מבנה 78: שינוי שם של הקובץ שנבחר במסך, לשם שהוקלד בו. אותה הרצה כמו בשורה כתובה; הסירובים של המסך (קובץ של התוכנה, הקובץ
     * כבר איננו, שם לא תקין, שם תפוס) נאמרים בתכנון ובנוסח של המסך. שם זהה לקיים — תקין, ואין מה לעשות: כך המסך שואל
     * "מותר לשנות לקובץ הזה שם?" עוד לפני שהמשתמש מקליד.
     */
    private static void renameSelected(Step s, String cmd, List<String> paths, Sim sim) {
        Selection sel = sim.selected;
        if (sel == null || !paths.get(0).equals(SELECTED_MARK) || !paths.get(1).equals(NAME_MARK) || sel.name == null) {
            s.error = Commands.usage(cmd);
            return;
        }
        if (sel.files.isEmpty() && sel.dirs.isEmpty()) { s.error = Words.Cmd.NOTHING_SELECTED; return; }
        if (!sel.dirs.isEmpty()) { s.error = Words.Cmd.REN_FOLDER; return; }
        if (sel.files.size() != 1) { s.error = Words.Cmd.SELECTED_ONE; return; }
        File f = new File(sel.files.get(0));
        String own = Physical.programFile(f.getAbsolutePath());
        if (own != null) { s.error = Words.Disk.CANT_RENAME + f.getName() + "\": " + own; return; }
        if (!sim.isFile(f)) { s.error = Words.FILE_NOT_AT_PATH; return; }
        String name = sel.name.trim(), bad = Physical.badName(name);
        if (bad != null) { s.error = bad; return; }
        File to = new File(f.getParentFile(), name);
        if (!name.equalsIgnoreCase(f.getName()) && (sim.isFile(to) || sim.isDir(to))) {
            s.error = Words.Disk.ALREADY_IN_FOLDER + name + "\"";
            return;
        }
        s.files.add(f.getAbsolutePath());
        s.newName = name;
        s.describe = Words.Cmd.rename(f.getName(), name);
        if (name.equals(f.getName())) s.noop = true;
        else sim.move(f.getAbsolutePath(), to, false);
    }

    /**
     * מבנה 77: העתקה או העברה של מה שנבחר במסך אל תיקייה. אותה הדמיה ואותה הרצה כמו בשורה כתובה, חוץ משני דברים: תיקייה שנבחרה
     * עוברת שלמה גם בהעתקה (בשורה כתובה, "העתק <תיקייה>" פירושו הקבצים שבה), והסירובים של המסך — יעד בתיקיית התוכנה או בסל,
     * תיקייה לתוך עצמה, קובץ של התוכנה בהעברה — נאמרים כבר בתכנון ובנוסח של המסך, כלומר לפני חלון האישור.
     */
    private static void fromSelected(Step s, String cmd, List<String> paths, Filter flt, Sim sim) {
        boolean copy = cmd.equals("COPY");
        if (paths.size() != 2) { s.error = Commands.usage(cmd); return; }
        if (s.recursive || s.stateWhen != Long.MIN_VALUE || paths.get(1).equals(Syntax.SHARE_MARK)) { s.error = Words.Cmd.SELECTED_PLAIN; return; }
        File dest = new File(paths.get(1));
        if (!sim.isDir(dest)) { s.error = Words.Cmd.NO_DEST; return; }
        String no = sim.lib == null ? null : Physical.noDest(sim.lib, dest);
        if (no != null) { s.error = no; return; }
        s.dest = dest;
        selected(s, flt, sim, cmd);
        if (s.error != null) return;
        for (String d : s.dirs)
            if (Physical.inside(dest, new File(d))) {
                s.error = Words.Disk.CANT + (copy ? Words.Disk.TO_COPY : Words.Disk.TO_MOVE) + Words.Disk.FOLDER_INTO_ITSELF;
                return;
            }
        arrange(s, copy, dest, sim);
    }

    /**
     * מבנה 73: המקור הוא מה שנבחר במסך. קובץ או תיקייה שכבר אינם — שגיאה (המסך הראה משהו שאיננו); סינון (גיל, גודל…) חל
     * על הקבצים; תיקייה — שלמה, ולכן סינון עם תיקייה שנבחרה הוא שגיאה, כמו בתיקייה שנכתבת במפורש (121).
     */
    private static void selected(Step s, Filter flt, Sim sim, String cmd) {
        // מבנה 77: מה שאסור למחוק אסור גם להעביר; להעתיק — מותר (המקור אינו משתנה).
        String cant = cmd.equals("DEL") ? Words.Disk.CANT_DELETE : cmd.equals("MOVE") ? Words.Disk.CANT_MOVE : null;
        Selection sel = sim.selected;
        if (sel == null || (sel.files.isEmpty() && sel.dirs.isEmpty())) { s.error = Words.Cmd.NOTHING_SELECTED; return; }
        if (!sel.dirs.isEmpty() && !flt.none()) { s.error = Words.Cmd.SELECTED_FILTER_FOLDER; return; }
        for (String p : sel.files) {
            File x = new File(p);
            if (!sim.isFile(x)) { s.error = Words.Cmd.notFound(p); return; }
            String own = cant == null ? null : Physical.programFile(p);         // מבנה 74: כבר בתכנון — המסך אומר זאת לפני חלון האישור
            if (own != null) { s.error = cant + x.getName() + "\": " + own; return; }
            s.total++;
            if (flt.ok(sim.real(x))) s.files.add(x.getAbsolutePath());
        }
        for (String d : sel.dirs) {
            File x = new File(d);
            if (!sim.isDir(x)) { s.error = Words.FOLDER_NOT_FOUND + ": " + d; return; }
            String why = cant == null || sim.lib == null ? null : Physical.forbidden(sim.lib, x);
            if (why != null) { s.error = cant + x.getName() + "\": " + why; return; }
            s.dirs.add(x.getAbsolutePath());
        }
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
        plan.pulse = new Pulse();                                               // מבנה 38: כמה זמן התוכנה לא רצה. מבנה 55: תמיד, לא רק עם "מדידה"
        try {
            List<String> rep = run0(plan, lib, asker, progress, grey, listen);
            stoppedHint(rep, plan.pulse.missed());
            return rep;
        } finally {
            plan.pulse.stop();
        }
    }

    /**
     * מבנה 55: המערכת של המכשיר עצרה את התוכנה באמצע הרצה — הדוח אומר זאת, ואיזו הגדרה במכשיר פותרת. נבדק במכשיר של המשתמש
     * (14.11.2026): עם החזקת המעבד והשירות הקדמי התוכנה עוד נעצרה 58 שניות מתוך 205 כשהמסך כבוי; אחרי "ללא הגבלות" בהגדרות
     * הסוללה — רצה ברציפות. המשפך: שורה רק כשזה קרה (5 שניות ומעלה).
     */
    static final long HINT_MS = 5000;

    static void stoppedHint(List<String> report, long missedMs) {
        if (missedMs >= HINT_MS) report.add(Words.Loop.stoppedBySystem(missedMs));
    }

    /**
     * מבנה 38: דופק ברקע בזמן הרצה עם "מדידה" — פעימה כל חצי שנייה, לפי השעון הרגיל. פער גדול בין שתי פעימות אומר
     * שכל התוכנה לא רצה: המכשיר ישן, או שהמערכת עצרה אותה (מסך כבוי). כך שורה איטית מסבירה את עצמה —
     * אם הדופק המשיך לפעום, השורה עצמה חיכתה למשהו; אם נעצר — לא השורה אשמה. (נ0.2: שורה אחת — 67.8 שניות, ואי אפשר היה לדעת למה.)
     */
    static final class Pulse implements Runnable {
        static final long EVERY = 500, GAP = 2000;
        private volatile boolean on = true;
        private volatile long missed, screenOff;
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
                if (!Awake.screenOn(Db.app)) screenOff += Math.min(now - last, GAP);   // מבנה 50: כמה זמן המסך היה כבוי (פעימה שהתעכבה — לא נספרת כולה)
                last = now;
            }
        }

        /** מבנה 50: כמה זמן (באלפיות) המסך היה כבוי מאז שהדופק התחיל. */
        long screenOff() {
            return screenOff;
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
        plan.asked = progress;                                                   // מבנה 62
        int fails = 0;                                                           // מ0.31: כישלונות בהרצה — מול המותר ("המשך")
        Step pending = null;                                                     // מ0.37: "ודא שגיאה"
        int pendingFails = 0;
        int sectionFails = 0;                                                    // מ0.45: "ודא תקין" — הכישלונות מתחילת הקטע
        boolean stopped = false;                                                 // מבנה 40: ההרצה נעצרה בגלל כישלונות (ואז ההודעה על כך כבר בדוח)
        if (plan.blocked()) {                                                    // מ0.30: שגיאה בקובץ — שום שורה לא רצה
            plan.trouble = true;
            report.add(Words.DbCmd.notRun(plan.errors));
            report.add(plan.errorText());
            RunReport.write(plan, report);
            return report;
        }
        Loops.Clock clock = new Loops.Clock();                                               // 1.25.0: לולאה — עצירה לפי זמן
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
                if (s.cmd.equals(Syntax.SECTION)) sectionFails = fails;
                report.add(Words.Cmd.line(s.label(), s.describe));
                continue;
            }
            if (s.cmd.equals(Syntax.OK_SINCE)) {                                       // מ0.45: ודא תקין
                boolean ok = fails == sectionFails;
                s.verified = ok;
                report.add(Words.Cmd.line(s.label(), Words.DbCmd.verifyResult(ok, Words.DbCmd.OK_SINCE_WHAT,
                        Words.DbCmd.okSinceGot(fails - sectionFails))));
                continue;
            }
            if (s.cmd.equals("ECHO") || s.cmd.equals("START") || (s.cmd.equals(Syntax.OPEN) && s.op == null)) continue;
            if (s.op != null) {                                                // 1.24.0: רשימות
                DbListCmd.lastFailed = false;
                String got = s.op.run(lib);
                s.said = got;                                                   // מבנה 85
                s.saidFailed = DbListCmd.lastFailed;
                report.add(Words.Cmd.line(s.label(), got));
                if (s.cmd.equals(Syntax.WRITE) && s.dest != null) {                    // מבנה 80: מה שיצא — גם למסך שהריץ את השורה
                    s.result = new Physical.Result();
                    if (DbListCmd.lastFailed) s.result.fail(got);
                    else s.result.done.add(s.dest.getAbsolutePath());
                }
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
                s.result = new Physical.Result();                               // מבנה 79
                if (why == null) s.result.done.add(s.dest.getAbsolutePath());
                else s.result.fail(why);
                if (why != null) { plan.trouble = true; fails++; }
            } else if (s.cmd.equals("RD") || s.cmd.equals("DEL")) {
                if (s.files.isEmpty() && s.dirs.isEmpty()) out = Words.Cmd.NO_FILES;
                else {
                    known(lib, s.files);
                    Physical.Result r = Physical.delete(lib, s.files, s.dirs, grey);
                    s.result = r;
                    out = result(r, Words.Cmd.TRASHED);
                    if (r.failed > 0) { plan.trouble = true; fails += r.failed; }
                }
            } else if (s.share) {                                          // 203
                if (s.files.isEmpty() && s.dirs.isEmpty()) out = Words.Cmd.NO_FILES;
                else out = stage(plan, s, progress);
            } else if (s.cmd.equals("COPY") || s.cmd.equals("MOVE")) {
                if (s.renameTaken) sameAtRun(s);                                // מ0.40: "יש כבר זהה" — שוב, מול הכונן של עכשיו
                if (s.files.isEmpty() && s.dirs.isEmpty()) {
                    out = notes(s).isEmpty() ? Words.Cmd.NO_FILES : notes(s).substring(3);
                    s.result = new Physical.Result();                           // מבנה 77: לא נעשה דבר — וזו התוצאה, לא תקלה
                } else {
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
                    s.result = r;                                               // מבנה 77
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
                s.result = r;                                                   // מבנה 78
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
        RunReport.write(plan, report);
        return report;
    }

    /** 1.26.0: מה שורה עושה — לתצוגה המקדימה ולסיכום. */
    static final int K_CREATE = 0, K_CHANGE = 1, K_DELETE = 2, K_READ = 3;

    static int kind(Step s) {
        if (s.cmd.equals("MD")) return K_CREATE;
        if (s.cmd.equals("DEL") || s.cmd.equals("RD") || s.cmd.equals(Syntax.PURGE) || s.cmd.equals(Syntax.DROP_MODE)) return K_DELETE;   // מ0.45
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
        for (String raw : Bidi.strip(text).replace("\r", "").split("\n")) if (Parse.clean(raw).equals(Syntax.CHECK)) return true;
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
                w.write("=== " + new java.text.SimpleDateFormat(Syntax.NOW_FORMAT, Locale.ROOT).format(new java.util.Date()) + " · "
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
