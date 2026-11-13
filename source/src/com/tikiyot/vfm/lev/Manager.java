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
