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

        Act(String text, Batch.Plan plan, java.io.File dest) {
            this.text = text;
            this.plan = plan;
            this.dest = dest;
        }

        /** למה אי אפשר — מהתכנון (קובץ של התוכנה, תיקייה אסורה, פריט שכבר איננו); null — אפשר. */
        String error() {
            for (Batch.Step s : plan.steps) if (s.error != null) return s.error;
            return plan.errors > 0 || plan.steps.isEmpty() ? plan.errorText() : null;
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
        String text = Commands.word(Commands.get(Batch.WRITE)) + " \"%" + Batch.V_DEST + "%\\%" + Batch.V_NEW_NAME + "%\" \"%" + Batch.V_CONTENT + "%\"\n";
        return new Act(text, Batch.plan(text, here, vars, lib, null, new Batch.Selection(java.util.Collections.<String>emptyList(),
                java.util.Collections.<String>emptyList(), f.getParentFile(), f.getName(), content)), f.getParentFile());
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
