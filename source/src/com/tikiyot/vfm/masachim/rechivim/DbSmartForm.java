package com.tikiyot.vfm;

// רשימה חכמה במסד — מה שהמסכים מוסרים למנהל: השם, התנאי, וההגדרות (מיון, תצוגה, צלמית, תיאור, "למה").
// מבנה 293 (אפיון החיפוש, 6 — "מקום אחד לעריכה"): הטופס הנפרד יצא. היצירה, העריכה של התנאים, והתנאי שהעורך מבקש — בחלונית של מסך
// החיפוש (SearchSave); הפרטים — בחלון העריכה של הרשימות (DbListScreen.edit). כאן נשאר רק המבנה של מה שנשמר.
// אחראי: המבנה והתרגום למפתחות של המסד. אסור לו: לפתוח מסד או מסך.

final class DbSmartForm {

    private DbSmartForm() { }

    /** מה שנשמר, במונחי המסד. */
    static final class Result {
        String name = "", cond = "", sort = "", view = "", icon = "", desc = "", why = "";
    }

    /** מבנה 97: מה שבטופס, מלבד השם והתנאי — לפי Lists.SMART_FORM. */
    static java.util.Map<String, String> props(Result r) {
        java.util.Map<String, String> m = new java.util.HashMap<String, String>();
        m.put(Store.P_SORT, r.sort);
        m.put(Store.P_VIEW, r.view);
        m.put(Store.P_ICON, r.icon);
        m.put(Store.P_DESC, r.desc);
        m.put(Store.P_WHY, r.why);
        return m;
    }
}
