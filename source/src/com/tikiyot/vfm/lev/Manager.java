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
        /** מבנה 91: אחרי say — המספר במסד של מה שנוצר (0 — לא נוצר), והאם הפעולה נכשלה. */
        long made;
        boolean failed;

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
            java.util.List<String> report = null;
            String said = null;
            try {
                report = Manager.run(plan, lib, null, null, false, null);
                for (Batch.Step s : plan.steps) if (s.said != null) {
                    said = s.said;
                    made = s.made;
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

    /**
     * מבנה 81: פעולה של מסך הסל על הפריטים שנבחרו בו — restore: "שחזר מהסל %נבחרים%"; אחרת "רוקן סל %נבחרים%" (מחיקה לצמיתות).
     * items — המסלולים של הפריטים בתוך הסל.
     */
    static Act onTrash(boolean restore, java.util.List<String> items, java.io.File here, java.util.Map<String, String> vars, Library lib) {
        String text = Commands.word(Commands.get(restore ? Batch.UNTRASH : Batch.PURGE)) + " %" + Batch.V_SELECTED + "%\n";
        return new Act(text, Batch.plan(text, here, vars, lib, null, new Batch.Selection(items, java.util.Collections.<String>emptyList())), null);
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
