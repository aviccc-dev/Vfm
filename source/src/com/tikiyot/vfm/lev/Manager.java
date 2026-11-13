package com.tikiyot.vfm;

/**
 * המנהל (הלב) — ההחלטות על הרצה של קובץ פקודות, במקום אחד ובלי מסך (מבנה 27).
 * עד כאן נכתבו פעמיים, בתוך קוד המסך: בהרצה המיידית (BatchUi) ובמסך ההרצה (RunScreen).
 * המסך שואל — ומציג ומבצע לפי התשובה; ההחלטה עצמה נבדקת במחשב.
 */
final class Manager {

    private Manager() { }

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

    /** פקודות הפתיחה שבסוף הקובץ רצות רק כשההרצה הלכה חלק (1.13.0; מ0.25). */
    static boolean opensAllowed(Batch.Plan plan) {
        return !plan.trouble;
    }
}
