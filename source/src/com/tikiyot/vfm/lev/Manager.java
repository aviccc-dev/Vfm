package com.tikiyot.vfm;

/**
 * המנהל (הלב) — ההחלטות על הרצה של קובץ פקודות, במקום אחד ובלי מסך (מבנה 27).
 * עד כאן נכתבו פעמיים, בתוך קוד המסך: בהרצה המיידית (BatchUi) ובמסך ההרצה (RunScreen).
 * המסך שואל — ומציג ומבצע לפי התשובה; ההחלטה עצמה נבדקת במחשב.
 */
final class Manager {

    private Manager() { }

    // ---------- מכוונת: לאן הולך טקסט של פקודות (מבנה 28) ----------

    /** פקודת פתיחה אחת — מבוצעת מיד, כמו כפתור (מ0.41). */
    static final int TO_VERB = 1;
    /** יש "בדיקה" — למסך ההרצה: תצוגה מקדימה ואישור (1.28.0). */
    static final int TO_CHECK = 2;
    /** יש מקומות פתוחים — נשאלים לפני ההרצה (1.25.6). */
    static final int TO_HOLES = 3;
    /** הכנה והרצה מיד. */
    static final int TO_RUN = 4;

    /** לאן הולך הטקסט, לפי הסדר. verbTried — פקודת הפתיחה כבר נוסתה ולא בוצעה: ממשיכים כקובץ רגיל. */
    static int route(String text, boolean verbTried) {
        if (!verbTried && Batch.singleVerb(text) != null) return TO_VERB;
        if (Batch.wantsCheck(text)) return TO_CHECK;
        if (!CmdTemplates.holes(lines(text)).isEmpty()) return TO_HOLES;
        return TO_RUN;
    }

    /** פקודת הפתיחה שהטקסט כולו — למי שקיבל TO_VERB. */
    static String verb(String text) {
        return Batch.singleVerb(text);
    }

    /** שורות הטקסט — כמו שההרצה מפרקת אותן. */
    static java.util.List<String> lines(String text) {
        return java.util.Arrays.asList(text.replace("\r", "").split("\n"));
    }

    // ---------- מכירה ומכוונת: פקודת פתיחה (מבנה 131) ----------
    // ההגדרה של המשתמש: "כל שכבה צריכה להיות מוכרת על ידה אחרת לא תעבוד." מי שיודע לפתוח משהו — מסך, כלי, תפריט, תיקייה —
    // נרשם אצל המנהל בשם של מה שהוא פותח (register). פקודת פתיחה מגיעה למנהל (open): הוא מפענח, בודק שהיעד רשום אצלו,
    // ומכוון אליו. יעד שאינו רשום — אינו נפתח, ונאמר. המסכים אינם מחליטים בעצמם איזה מסך נפתח (שומר בבדיקות המבנה).

    /** מי שפותח: מקבל את הפקודה המפוענחת. true — טיפל בה. */
    interface Opener {
        boolean open(android.app.Activity a, Verbs.Cmd c, String full, java.io.File here);
    }

    /** מי שמטפל קודם בפקודות הרשימות כשהמסד פעיל (פתיחת רשימה, ושורות שכותבות — דרך onVerbLine). */
    static final String OPEN_DB = "db";

    private static final java.util.Map<String, Opener> OPENERS =
            java.util.Collections.synchronizedMap(new java.util.LinkedHashMap<String, Opener>());

    /** רישום: what — סוג הפקודה (Verbs.K_*), ולמסך — screenKey של המסך. */
    static Opener register(String what, Opener o) {
        return o == null ? OPENERS.remove(what) : OPENERS.put(what, o);          // null — מוציא מהמרשם; מחזיר את מי שהיה רשום
    }

    static boolean registered(String what) {
        return OPENERS.containsKey(what);
    }

    /** השם שבו היעד של הפקודה רשום: מסך — בשמו; כל השאר — לפי סוג הפקודה. */
    static String openKey(Verbs.Cmd c) {
        return Verbs.K_SCREEN.equals(c.kind) ? screenKey(c.arg) : c.kind;
    }

    static String screenKey(String screen) {
        return Verbs.K_SCREEN + ":" + screen;
    }

    /**
     * פקודת פתיחה. null — אינה פקודת פתיחה (שורה למנוע הרגיל). "" — כוונה ליעד הרשום. אחרת — מה לומר למשתמש: שגיאה
     * בשורה, או יעד שאינו רשום אצל שכבת הניהול.
     */
    static String open(android.app.Activity a, String line, java.io.File here, java.util.Map<String, String> vars) {
        if (!Verbs.isVerb(line)) return null;
        String full = Verbs.withVars(line, vars);
        Verbs.Cmd c = Verbs.parse(full);
        if (c.error != null) return c.error;
        if (DbVerbs.active()) {                                                 // מ0.10: במסד — פקודות הרשימות על המסד
            Opener db = OPENERS.get(OPEN_DB);
            if (db != null && db.open(a, c, full, here)) return "";
        }
        if (Verbs.hostCode(c) != null) return Words.Top.notHere(Verbs.label(c));   // מ0.42: אין עוד מסך שעושה אותן בעצמו
        Opener o = OPENERS.get(openKey(c));
        if (o == null) {
            Log.i("⚠ " + Words.PermitW.notKnown(openKey(c)));
            return Words.PermitW.notKnown(Verbs.label(c));
        }
        o.open(a, c, full, here);
        return "";
    }

    // ---------- מאשרת: מה עושים עם תכנית מוכנה ----------

    /** בכאילו — רק התוצאה, בלי ביצוע (1.10.0). */
    static final int AS_IF = 1;
    /** אין מה לעשות — רק מציגים: אין שינוי, ואין מה לפתוח בסוף. */
    static final int NOTHING = 2;
    /** שגיאה בקובץ — לא מריצים דבר (מ0.30). */
    static final int BLOCKED = 3;
    /** מותר להריץ. */
    static final int GO = 4;

    /** ההחלטה על תכנית, לפי הסדר: בכאילו ← אין מה לעשות ← שגיאה ← מותר. */
    static int approve(Batch.Plan plan) {
        if (plan.asIf) return AS_IF;
        if (plan.changes() == 0 && plan.start == null && plan.open == null) return NOTHING;
        if (plan.blocked()) return BLOCKED;
        return GO;
    }

    // ---------- ההרצה — רק מכאן (מבנה 76) ----------

    // ---------- מבנה 124: הפעולות שהתוכנה עושה לבד — מוכרות כאן, בשמן; מה שאינו ברשימה אינו מקבל היתר ----------
    static {
        for (String name : new String[]{Permit.SYS_MENUS, Permit.SYS_MINE, Permit.SYS_FOLLOW, Permit.SYS_TOOLS, Permit.SYS_EXIT, Permit.SYS_START,
                Permit.SYS_TOP_ROW, Permit.SYS_DEV})
            Permit.allowSystem(name);
    }

    /** נקרא בכניסה לתוכנה (מסך, שירות, ספק) — כדי שהרישום שלמעלה ייעשה לפני כל פעולה של המערכת. */
    static void boot() { }

    /**
     * מריץ תכנית. זה המקום היחיד שפותח את ההיתר של שכבת הניהול (Permit): מה שהתכנית עושה בכונן נעשה רק בתוכו.
     * המסכים ושאר הקוד אינם קוראים ל־Batch.run בעצמם (שומר בבדיקות המבנה).
     */
    static java.util.List<String> run(Batch.Plan plan, Library lib, Physical.Asker asker, Physical.Progress progress, boolean grey,
                                      Batch.Listener listen) {
        Permit.open();
        try {
            return Batch.run(plan, lib, asker, progress, grey, listen);
        } finally {
            Permit.close();
        }
    }

    // ---------- פעולה של מסך — כפקודה (מבנה 74; שלב ב: "כל פעולה של מסך עוברת דרך פקודה") ----------
    // המסך אינו עושה את הפעולה בעצמו: הוא מוסר מה נבחר ואיזו פקודה, ומקבל תכנית (ובה גם הסירוב, אם יש) ותוצאה.
    // אותה פקודה, אותן בדיקות ואותו מסלול כמו בקובץ פקודות — ולכן מה שנבדק בפקודות נבדק גם למסך.

    /** פעולה של מסך על מה שנבחר בו: הפקודה, התכנית שלה, והרצה שמחזירה את מה שיצא. */
    static final class Act {
        final String text;
        final Batch.Plan plan;
        /** מבנה 77: היעד (ליומן — בפקודה הוא משתנה); null — אין. */
        final java.io.File dest;
        /** מבנה 91: אחרי say — המספר במסד של מה שנוצר (0 — לא נוצר), והאם הפעולה נכשלה. */
        long made;
        boolean failed;
        /** מבנה 116: אחרי say — הפריטים שנכנסו ליעד (הדבקה מהלוח); null — לא רלוונטי. */
        java.util.List<Long> entered;

        Act(String text, Batch.Plan plan, java.io.File dest) {
            this.text = text;
            this.plan = plan;
            this.dest = dest;
        }

        /** מבנה 100: יש בתכנית משהו לעשות (ולא רק "כבר קיים — בלי שינוי"). */
        boolean writes() {
            for (Batch.Step s : plan.steps) if (s.op != null) return true;
            return false;
        }

        /** מבנה 122: מה שהפעולה תיצור (קובץ ה־zip, התיקייה שנפרסת) — לחלון האישור; null — אין. */
        java.io.File target() {
            for (Batch.Step s : plan.steps) if (s.dest != null) return s.dest;
            return null;
        }

        /** למה אי אפשר — מהתכנון (קובץ של התוכנה, תיקייה אסורה, פריט שכבר איננו); null — אפשר. */
        String error() {
            for (Batch.Step s : plan.steps) if (s.error != null) return s.error;
            return plan.errors > 0 || plan.steps.isEmpty() ? plan.errorText() : null;
        }

        /**
         * מבנה 85: הרצה של פעולה במסד (רשימות). מחזירה את מה שהפעולה אמרה — ההודעה שהמסך מציג, כמו שהעטיפה הישנה החזירה;
         * תקלה — הודעת כישלון, לא חריגה.
         */
        String say(Library lib) {
            return say(lib, null);
        }

        /** מבנה 122: ...עם התקדמות ו"עצור" (אריזה ופריסה של zip). */
        String say(Library lib, Physical.Progress progress) {
            java.util.List<String> report = null;
            String said = null;
            try {
                report = Manager.run(plan, lib, null, progress, false, null);
                for (Batch.Step s : plan.steps) if (s.said != null) {
                    said = s.said;
                    made = s.made;
                    entered = s.entered;
                    failed = s.saidFailed;
                }
            } catch (Throwable t) {
                Log.e("פעולת מסך: " + text.trim(), t);
            }
            Log.i("פעולת מסך: " + text.trim() + " · " + (report == null ? Words.Disk.ERROR : Batch.logLine(report)));
            if (said == null) failed = true;
            return said != null ? said : Words.Db.failed(Words.Disk.ERROR);
        }

        /** ההרצה. מחזירה את התוצאה של הפעולה בכונן; תקלה — תוצאה עם כישלון, לא חריגה. */
        Physical.Result run(Library lib, Physical.Asker asker, Physical.Progress progress, boolean grey) {
            java.util.List<String> report = null;
            Physical.Result r = null;
            try {
                report = Manager.run(plan, lib, asker, progress, grey, null);
                for (Batch.Step s : plan.steps) if (s.result != null) r = s.result;
            } catch (Throwable t) {
                Log.e("פעולת מסך: " + text.trim(), t);
            }
            if (r == null) {
                r = new Physical.Result();
                r.fail(Words.Disk.ERROR);
            }
            // מבנה 77: מה שכבר היה בתיקיית היעד — בפקודה "לא נעשה דבר"; למסך זה נמסר כמו שהיה אצלו: "כבר בתיקייה הזו", לכל פריט.
            for (Batch.Step s : plan.steps) for (int k = 0; k < s.inPlace; k++) r.fail(Words.Disk.ALREADY_HERE);
            Log.i("פעולת מסך: " + text.trim() + (dest == null ? "" : " [" + dest.getAbsolutePath() + "]") + " · "
                    + (report == null ? Words.Disk.ERROR : Batch.logLine(report)));
            return r;
        }
    }

    /** הפקודה cmd (השם שבהגדרת הפקודות: "DEL"…) על מה שנבחר במסך. here — רק למשתנים; הבחירה במסלולים מלאים. */
    static Act onSelected(String cmd, java.util.List<String> files, java.util.List<String> dirs, java.io.File here,
                          java.util.Map<String, String> vars, Library lib) {
        return onSelected(cmd, files, dirs, null, here, vars, lib);
    }

    /** מבנה 77: ...אל התיקייה dest ("COPY", "MOVE"): "העתק %נבחרים% "%יעד%"". dest == null — פקודה בלי יעד. */
    static Act onSelected(String cmd, java.util.List<String> files, java.util.List<String> dirs, java.io.File dest, java.io.File here,
                          java.util.Map<String, String> vars, Library lib) {
        String text = Commands.word(Commands.get(cmd)) + " %" + Batch.V_SELECTED + "%"
                + (dest == null ? "" : " \"%" + Batch.V_DEST + "%\"") + "\n";
        return new Act(text, Batch.plan(text, here, vars, lib, null, new Batch.Selection(files, dirs, dest)), dest);
    }

    /**
     * מבנה 78: שינוי שם של קובץ, מהמסך: "שנה %נבחרים% "%שם-חדש%"". name — מה שהוקלד, כמו שהוא. עם השם של היום — התכנית עונה רק על
     * "מותר לשנות לקובץ הזה שם?" (קובץ של התוכנה, קובץ שכבר איננו), לפני שהמסך מבקש שם.
     */
    static Act onRename(String path, String name, java.io.File here, java.util.Map<String, String> vars, Library lib) {
        String text = Commands.word(Commands.get("REN")) + " %" + Batch.V_SELECTED + "% \"%" + Batch.V_NEW_NAME + "%\"\n";
        return new Act(text, Batch.plan(text, here, vars, lib, null, new Batch.Selection(java.util.Collections.singletonList(path),
                java.util.Collections.<String>emptyList(), null, name)), null);
    }

    /** מבנה 79: תיקייה חדשה, מהמסך — בתיקייה parent, בשם שהוקלד: "צור תיקייה "%יעד%\%שם-חדש%"". */
    static Act onMakeFolder(java.io.File parent, String name, java.io.File here, java.util.Map<String, String> vars, Library lib) {
        String text = Commands.word(Commands.get("MD")) + " \"%" + Batch.V_DEST + "%\\%" + Batch.V_NEW_NAME + "%\"\n";
        return new Act(text, Batch.plan(text, here, vars, lib, null, new Batch.Selection(java.util.Collections.<String>emptyList(),
                java.util.Collections.<String>emptyList(), parent, name)), parent);
    }

    /** מבנה 80: קובץ טקסט חדש, מהמסך — הקובץ f עם התוכן הזה: "כתוב "%יעד%\%שם-חדש%" "%תוכן%"". אינו מחליף קובץ קיים. */
    static Act onNewFile(java.io.File f, String content, java.io.File here, java.util.Map<String, String> vars, Library lib) {
        return onNewFile(f, content, "", here, vars, lib);
    }

    /** מבנה 110 (ה6): taken — התשובה של המשתמש לשאלה "שם קיים" (להחליף), כמו /תפוס: בקובץ; "" — בלי. */
    static Act onNewFile(java.io.File f, String content, String taken, java.io.File here, java.util.Map<String, String> vars, Library lib) {
        // מבנה 111 (ה6ב, המשתמש: "כן"): "כתוב" אינו יוצר תיקייה. כשהמסך יוצר קובץ בתיקייה שעוד אין ("פקודה חדשה" בתיקייה
        // שעוד לא נוצרה) — קודם "צור תיקייה … /תפוס:לדלג", ואז "כתוב": שתי פקודות, כל אחת בכלל שלה.
        String md = f.getParentFile() != null && !f.getParentFile().isDirectory()
                ? Commands.word(Commands.get("MD")) + " \"%" + Batch.V_DEST + "%\" " + Batch.SW_TAKEN + Batch.TAKEN_SKIP + "\n" : "";
        String text = md + Commands.word(Commands.get(Batch.WRITE)) + " \"%" + Batch.V_DEST + "%\\%" + Batch.V_NEW_NAME + "%\" \"%" + Batch.V_CONTENT + "%\""
                + (taken == null || taken.isEmpty() ? "" : " " + Batch.SW_TAKEN + taken) + "\n";
        return new Act(text, Batch.plan(text, here, vars, lib, null, new Batch.Selection(java.util.Collections.<String>emptyList(),
                java.util.Collections.<String>emptyList(), f.getParentFile(), f.getName(), content)), f.getParentFile());
    }

    /** מבנה 138: הגודל הגדול ביותר של קובץ שתוסף קורא כטקסט. */
    static final long PLUGIN_READ_MAX = 2L * 1024 * 1024;

    /**
     * מבנה 138 (המשתמש, 15.11.2026, על מה שמותר לתוסף: "הכל יוכל, אבל דרך שכבת הניהול"): תוסף קורא קובץ — כאן. מותר כל
     * קובץ, גם של התוכנה; קריאה אינה משנה דבר, ולכן אין לה היתר — אבל היא עוברת במקום אחד. null — אין קובץ כזה, גדול מדי,
     * או שהקריאה נכשלה (נרשם ביומן).
     */
    static String pluginRead(java.io.File f) {
        if (f == null || !f.isFile() || f.length() > PLUGIN_READ_MAX) return null;
        try {
            return Files.readUtf8(f);
        } catch (Throwable t) {
            Log.e("תוסף: קריאת קובץ " + f.getAbsolutePath(), t);
            return null;
        }
    }

    /** מבנה 138: תוסף שואל מה יש בתיקייה — כאן. null — אין תיקייה כזו, או שאי אפשר לקרוא אותה. */
    static java.io.File[] pluginList(java.io.File d) {
        return d == null ? null : d.listFiles();
    }

    /**
     * מבנה 132 (הדלת לתוספים — "מחברת"): קובץ של המשתמש שתוסף שומר — קובץ פקודות, או דוח לתיקיית ההורדות — נכתב כפקודה
     * "כתוב", כמו מכל מקום אחר: קובץ של התוכנה — מסורב; קובץ קיים — רק עם replace, והקודם עובר לסל (הוראות, סעיף 4).
     * null — נשמר; אחרת — למה לא.
     */
    static String saveFile(java.io.File f, String text, boolean replace, Library lib) {
        java.io.File here = f.getParentFile() != null ? f.getParentFile() : Paths.work();
        Act act = onNewFile(f, text == null ? "" : text, replace ? Batch.TAKEN_REPLACE : "", here, new java.util.HashMap<String, String>(), lib);
        String no = act.error();
        if (no != null) return no;
        Physical.Result r = act.run(lib, null, null, false);
        return r.failed > 0 ? (r.note == null || r.note.isEmpty() ? Words.SAVE_FAILED : r.note) : null;
    }

    /**
     * מבנה 81: פעולה של מסך הסל על הפריטים שנבחרו בו — restore: "שחזר מהסל %נבחרים%"; אחרת "רוקן סל %נבחרים%" (מחיקה לצמיתות).
     * items — המסלולים של הפריטים בתוך הסל.
     */
    static Act onTrash(boolean restore, java.util.List<String> items, java.io.File here, java.util.Map<String, String> vars, Library lib) {
        String text = Commands.word(Commands.get(restore ? Batch.UNTRASH : Batch.PURGE)) + " %" + Batch.V_SELECTED + "%\n";
        return new Act(text, Batch.plan(text, here, vars, lib, null, new Batch.Selection(items, java.util.Collections.<String>emptyList())), null);
    }

    /** מבנה 122: אריזה של תיקייה ("ארוז %נבחרים%") או פריסה של קובץ zip ("פרוס %נבחרים%"), מהמסך. path — מה שנבחר. */
    static Act onZip(boolean pack, String path, java.io.File here, java.util.Map<String, String> vars, Library lib) {
        String text = Commands.word(Commands.get(pack ? Batch.PACK : Batch.UNPACK)) + " %" + Batch.V_SELECTED + "%\n";
        java.util.List<String> one = java.util.Collections.singletonList(path), none = java.util.Collections.<String>emptyList();
        return new Act(text, Batch.plan(text, here, vars, lib, null, new Batch.Selection(pack ? none : one, pack ? one : none)), null);
    }

    /**
     * מבנה 85: "הסרה מכאן" במסך הרשימות — "בטל ידנית %נבחרים%". mode — האופן של המסך; in — הרשימה שמוצגת; ids — הפריטים שנבחרו
     * (מספרים במסד).
     */
    static Act onListRemove(String mode, long in, java.util.List<Long> ids, java.io.File here, java.util.Map<String, String> vars, Library lib) {
        String text = Commands.HE_UNLIST + " " + ListCmd.MANUAL + " %" + Batch.V_SELECTED + "%\n";
        return new Act(text, Batch.plan(text, here, vars, lib, null, new Batch.Selection(mode, in, ids)), null);
    }

    /**
     * מבנה 91: רשימה חדשה במסך הרשימות (➕) ובבורר הרשימות — "צור תיקייה ידנית %נבחרים%". mode — האופן; in — הרשימה שבתוכה
     * יוצרים (מספר במסד; 0 — הבית); name — השם שהוקלד, כלשונו.
     */
    static Act onListNew(String mode, long in, String name, java.io.File here, java.util.Map<String, String> vars, Library lib) {
        String text = Commands.HE_MD + " " + ListCmd.MANUAL + " %" + Batch.V_SELECTED + "%\n";
        return new Act(text, Batch.plan(text, here, vars, lib, null, new Batch.Selection(mode, in, name)), null);
    }

    /**
     * מבנה 93: רעיון או כותרת במסך הרשימות — "צור רעיון %נבחרים%" (ברשימה שמוצגת: in) או "צור רעיון %נבחרים% "דרך"" (ברשימה
     * שנכתבה: listPath, לא ריק). text — מה שהוקלד, כלשונו.
     */
    static Act onListNote(String mode, long in, boolean title, String text, String listPath, java.io.File here,
                          java.util.Map<String, String> vars, Library lib) {
        boolean named = listPath != null && !listPath.trim().isEmpty();
        String line = Verbs.CREATE + " " + (title ? Verbs.TITLE_WORD : Verbs.NOTE_WORD) + " %" + Batch.V_SELECTED + "%"
                + (named ? " \"" + listPath.trim() + "\"" : "") + "\n";
        return new Act(line, Batch.plan(line, here, vars, lib, null, new Batch.Selection(mode, named ? 0 : in, text)), null);
    }

    /**
     * מבנה 97: רשימה חכמה חדשה מהטופס (➕ במסך הרשימות, "שמירה כרשימה חכמה" במסך החיפוש) — "צור רשימה חכמה %שם-חדש% %תוכן%".
     * in — הרשימה שבתוכה יוצרים (0 — הבית); name, cond, props — מה שבטופס, כלשונו.
     */
    static Act onSmartNew(String mode, long in, String name, String cond, java.util.Map<String, String> props, java.io.File here,
                          java.util.Map<String, String> vars, Library lib) {
        return onSmartNew(mode, in, name, cond, props, "", here, vars, lib);
    }

    /**
     * מבנה 104: taken — מה שהמשתמש ענה כשהשם קיים ("להחליף" = לדרוס; "" — עוד לא נשאל). אותה מילה כמו בקובץ פקודות
     * (`/תפוס:להחליף`) — אותה הוראה, אותו מסלול.
     */
    static Act onSmartNew(String mode, long in, String name, String cond, java.util.Map<String, String> props, String taken,
                          java.io.File here, java.util.Map<String, String> vars, Library lib) {
        String text = Verbs.CREATE + " " + Verbs.LIST + " " + Verbs.SMART_WORD + " %" + Batch.V_NEW_NAME + "% %" + Batch.V_CONTENT + "%"
                + (taken == null || taken.isEmpty() ? "" : " " + Batch.SW_TAKEN + taken) + "\n";
        return new Act(text, Batch.plan(text, here, vars, lib, null, new Batch.Selection(mode, in, name, cond, props)), null);
    }

    /**
     * מבנה 99: עריכה של רשימה חכמה בטופס שלה — "עדכן רשימה חכמה %נבחרים% %תוכן%". id — הרשימה (מספר במסד); name — השם בטופס
     * (ריק — בלי שינוי); cond, props — מה שבטופס, כלשונו.
     */
    static Act onSmartEdit(String mode, long id, String name, String cond, java.util.Map<String, String> props, java.io.File here,
                           java.util.Map<String, String> vars, Library lib) {
        String text = Verbs.UPDATE + " " + Verbs.LIST + " " + Verbs.SMART_WORD + " %" + Batch.V_SELECTED + "% %" + Batch.V_CONTENT + "%\n";
        Batch.Selection sel = new Batch.Selection(mode, 0, name, cond, props);
        sel.ids.add(id);
        return new Act(text, Batch.plan(text, here, vars, lib, null, sel), null);
    }

    /**
     * מבנה 114: קבצים מהכונן אל רשימה (הבורר "לרשימה", ➕, קובץ פקודות שנכנס לרשימה) — "העתק %נבחרים% ידנית %נבחרים%".
     * list — הרשימה (מספר במסד; 0 — הבית); paths — הקבצים, במסלולים מלאים.
     */
    static Act onListAdd(String mode, long list, java.util.List<String> paths, java.io.File here, java.util.Map<String, String> vars, Library lib) {
        return onListAdd(mode, list, paths, new java.util.ArrayList<String>(), FOLDER_TREE, here, vars, lib);
    }

    /** מבנה 115: איך תיקייה נכנסת לרשימה — כמו שלוש האפשרויות שבמסך. */
    static final int FOLDER_TREE = 0, FOLDER_LIVE = 1, FOLDER_FLAT = 2;

    /**
     * מבנה 115: ...וגם תיקיות (dirs): FOLDER_TREE — התיקייה עצמה, רשימה בשמה עם המבנה; FOLDER_LIVE — רשימה חכמה של התיקייה
     * ("/חיה"); FOLDER_FLAT — רק הקבצים ("/קבצים"). שם קיים — המסך עונה מראש "שם אחר" ("/תפוס:לשנות-שם"), כמו שעשה עד כאן.
     */
    static Act onListAdd(String mode, long list, java.util.List<String> paths, java.util.List<String> dirs, int how, java.io.File here,
                         java.util.Map<String, String> vars, Library lib) {
        String sw = dirs.isEmpty() ? "" : how == FOLDER_FLAT ? " " + ListCmd.SW_FLAT
                : (how == FOLDER_LIVE ? " " + ListCmd.SW_LIVE : "") + " " + Batch.SW_TAKEN + Batch.TAKEN_RENAME;
        String text = Commands.word(Commands.get("COPY")) + " %" + Batch.V_SELECTED + "% " + ListCmd.MANUAL + " %" + Batch.V_SELECTED + "%" + sw + "\n";
        Batch.Selection sel = new Batch.Selection(mode, list, new java.util.ArrayList<Long>());
        sel.files.addAll(paths);
        sel.dirs.addAll(dirs);
        return new Act(text, Batch.plan(text, here, vars, lib, null, sel), null);
    }

    /** מבנה 121: סדר ידני של מה שנבחר ברשימה in — "עדכן רשימה %נבחרים% /מקום:<איך>". how — DbMore.UP … BOTTOM. */
    static Act onListOrder(String mode, long in, java.util.List<Long> ids, int how, java.io.File here, java.util.Map<String, String> vars, Library lib) {
        String text = Verbs.UPDATE + " " + Verbs.LIST + " %" + Batch.V_SELECTED + "% " + Verbs.SW_PLACE + Verbs.PLACES[how] + "\n";
        return new Act(text, Batch.plan(text, here, vars, lib, null, new Batch.Selection(mode, in, ids)), null);
    }

    /** מבנה 121: העיצוב של רשימה — "עדכן רשימה %נבחרים% /עיצוב:%תוכן%". name — שם העיצוב, כלשונו ("" — לרשת). */
    static Act onListDesign(String mode, long list, String name, java.io.File here, java.util.Map<String, String> vars, Library lib) {
        String text = Verbs.UPDATE + " " + Verbs.LIST + " %" + Batch.V_SELECTED + "% " + Verbs.SW_DESIGN + "%" + Batch.V_CONTENT + "%\n";
        Batch.Selection sel = new Batch.Selection(mode, 0, "", name, null);
        sel.ids.add(list);
        return new Act(text, Batch.plan(text, here, vars, lib, null, sel), null);
    }

    /** מבנה 120: רענון האינדקס מהמסך (הגדרות, חיפוש) — "רענן" · "רענן עמוק" · "רענן מלא", כמו בקובץ פקודות. */
    static final int INDEX_QUICK = 0, INDEX_DEEP = 1, INDEX_FULL = 2;

    static Act onIndex(int kind, java.io.File here, java.util.Map<String, String> vars, Library lib) {
        String text = Commands.word(Commands.get(Batch.REFRESH)) + (kind == INDEX_DEEP ? " " + Batch.W_DEEP : kind == INDEX_FULL ? " " + Batch.W_FULL : "") + "\n";
        return new Act(text, Batch.plan(text, here, vars, lib), null);
    }

    /** מבנה 119: "גיבוי עכשיו" של אופן, מהמסך — "אופן "<שם>"" ואחריה "גבה", כמו בקובץ פקודות. */
    static Act onBackup(String mode, java.io.File here, java.util.Map<String, String> vars, Library lib) {
        String text = Commands.word(Commands.get(Batch.MODE)) + " \"" + mode + "\"\n" + Commands.word(Commands.get(Batch.BACKUP)) + "\n";
        return new Act(text, Batch.plan(text, here, vars, lib), null);
    }

    /**
     * מבנה 127: הגדרה של התוכנה, מהמסך — "הגדר "%שם-חדש%" "%תוכן%"": המפתח (כמו שהוא בקובץ ההגדרות) והערך — מהבחירה, ואינם
     * מפוענחים. אותו כלל כמו בשורה כתובה (Conf.SETTINGS): הגדרה שאינה מוכרת, או ערך שאינו מתאים — סירוב מהתכנון.
     */
    static Act onSetting(String key, String value, java.io.File here, java.util.Map<String, String> vars, Library lib) {
        String text = Commands.word(Commands.get(Batch.SET)) + " \"%" + Batch.V_NEW_NAME + "%\" \"%" + Batch.V_CONTENT + "%\"\n";
        return new Act(text, Batch.plan(text, here, vars, lib, null, new Batch.Selection(java.util.Collections.<String>emptyList(),
                java.util.Collections.<String>emptyList(), null, key, value)), null);
    }

    /** מבנה 127: קביעת הגדרה — מתכנן ומריץ במקום אחד (המסכים, הגשר). null — נקבעה; אחרת — למה לא (הסירוב, או הכישלון). */
    static String setting(String key, String value) {
        Act act = onSetting(key, value, Paths.work(), new java.util.HashMap<String, String>(), null);
        String no = act.error();
        if (no != null) return no;
        String said = act.say(null);
        return act.failed ? said : null;
    }

    /**
     * מבנה 126: "ניקוי" שבסוף הרצה של קובץ פקודות (מסך ההרצה) — רשימה שההרצה יצרה יוצאת מהמסד: "אופן "<שם>"" (אם הקובץ קבע
     * אופן) ואחריה "בטל ידנית "<הדרך>"", כמו בקובץ פקודות. פעולה אחת לכל רשימה: אופן שבוטל בינתיים — סירוב מהתכנון;
     * רשימה שכבר איננה — אין מה לעשות (writes); והשאר ממשיכות.
     */
    static Act onRunClean(String mode, java.util.List<String> path, java.io.File here, java.util.Map<String, String> vars, Library lib) {
        String text = (mode == null ? "" : Commands.word(Commands.get(Batch.MODE)) + " \"" + mode + "\"\n")
                + Commands.HE_UNLIST + " " + ListCmd.MANUAL + " \"" + ListCmd.pathText(path) + "\"\n";
        return new Act(text, Batch.plan(text, here, vars, lib), null);
    }

    /**
     * מבנה 116: הדבקה מהלוח של הרשימות — "העתק / העבר ידנית %נבחרים% ידנית %נבחרים%". ids — הפריטים (מספרים במסד); from —
     * הרשימה שסומנו בה (להעברה: משם הם יוצאים); dest — הרשימה שאליה מדביקים.
     */
    static Act onListPaste(String mode, boolean move, long from, java.util.List<Long> ids, long dest, java.io.File here,
                           java.util.Map<String, String> vars, Library lib) {
        String text = Commands.word(Commands.get(move ? "MOVE" : "COPY")) + " " + ListCmd.MANUAL + " %" + Batch.V_SELECTED + "% " + ListCmd.MANUAL
                + " %" + Batch.V_SELECTED + "%\n";
        Batch.Selection sel = new Batch.Selection(mode, from, ids);
        sel.to = dest;
        return new Act(text, Batch.plan(text, here, vars, lib, null, sel), null);
    }

    /**
     * מבנה 113: עריכה של רשימה בטופס שלה (שם, צלמית, תיאור, "למה"; לרעיון ולכותרת — הטקסט) — "עדכן רשימה %נבחרים%".
     * id — הפריט (מספר במסד); name — השם בטופס (ריק — בלי שינוי); props — צלמית, תיאור ו"למה", כלשונם.
     */
    static Act onListEdit(String mode, long id, String name, java.util.Map<String, String> props, java.io.File here,
                          java.util.Map<String, String> vars, Library lib) {
        String text = Verbs.UPDATE + " " + Verbs.LIST + " %" + Batch.V_SELECTED + "%\n";
        Batch.Selection sel = new Batch.Selection(mode, 0, name, "", props);
        sel.ids.add(id);
        return new Act(text, Batch.plan(text, here, vars, lib, null, sel), null);
    }

    /**
     * מבנה 100: כפתור (בתפריט, בשורה העליונה) שהפעולה שלו היא שורה של פקודה שכותבת — "צור רשימה", "צור רשימה חכמה", "עדכן
     * רשימה חכמה". עד כאן המסך ביצע אותה בעצמו (DbVerbsUi); עכשיו — אותה שורה בדיוק, כמו בקובץ פקודות. line — כמו שהכפתור כתוב.
     */
    static Act onVerbLine(String line, java.io.File here, java.util.Map<String, String> vars, Library lib) {
        String text = line.trim() + "\n";
        return new Act(text, Batch.plan(text, here, vars, lib), null);
    }

    // ---------- התוצאה חוזרת: מה קורה בסוף ההרצה ----------

    /** פקודות הפתיחה שבסוף הקובץ רצות רק כשההרצה הלכה חלק (1.13.0; מ0.25). */
    static boolean opensAllowed(Batch.Plan plan) {
        return !plan.trouble;
    }

    /** בהרצה מיידית — הדוח המלא, ולא הודעה קצרה: הייתה תקלה, יש ECHO, או מדידה (216; 1.25.0). מבנה 29. */
    static boolean fullReport(Batch.Plan plan) {
        return plan.trouble || !plan.echoes.isEmpty() || plan.measure;
    }

    /** 186: הייתה תקלה, ו־START הוא קובץ (לא הבית) — נפתח בכל זאת, לצד הדוח. מבנה 29. */
    static boolean openStartOnTrouble(Batch.Plan plan) {
        return plan.trouble && plan.start != null && !Batch.isHome(plan.start) && plan.start.isFile();
    }
}
