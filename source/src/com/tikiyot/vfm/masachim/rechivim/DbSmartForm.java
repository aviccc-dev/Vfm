package com.tikiyot.vfm;

// מבנה 291: היצירה והעריכה של רשימה חכמה — כבר לא כאן (החלונית של מסך החיפוש — SearchSave; הפרטים — בחלון העריכה של הרשימות).
// נשאר: התוצאה (Result, props) שהמנהל מקבל, והטופס לעורך הפקודות (ask). מ0.10.1: רשימה חכמה במסד — בטופס המוכר (SmartForm: מאיפה, סוג, תאריך, גודל, מיון, תצוגה, צלמית, תיאור, "למה"),
// ליצירה ולעריכה (המשתמש: "רשימה חכמה היא בלי הטופס של התנאים"). הטופס עצמו לא משתנה; כאן רק התרגום בין המאפיינים
// במסד (cond/sort/view/icon/desc/why) לבין מה שהטופס מקבל ומחזיר.
// אחראי: התרגום, ושמירה בעסקה אחת. אסור לו: לפתוח מסד (מקבל Store פתוח לכתיבה).

import android.app.Activity;

final class DbSmartForm {

    private DbSmartForm() { }

    /** מה שהטופס החזיר, כבר במונחי המסד. */
    static final class Result {
        String name = "", cond = "", sort = "", view = "", icon = "", desc = "", why = "";
    }

    interface Done {
        void ok(Result r);
    }

    /**
     * מ0.41 (המשתמש, 12.11.2026, על העורך האינטראקטיבי: "בפקודה צור רשימה חכמה אין טופס לאפשרויות. הטופס הקיים היום
     * ליצירת רשימה חכמה מהתוכנה מתאים לשם"): הטופס המוכר לתנאי שכתוב בפקודה — בלי מסד, רק מחזיר מה שנבחר.
     */
    static void ask(Activity a, String name, String cond, String sort, final Done done) {
        Target t = Target.of(FnSmart.KIND, SmartLists.COND, cond == null ? "" : cond.trim(), SmartLists.SORT, sort == null ? "" : sort.trim());
        SmartForm.show(a, name == null || name.trim().isEmpty() ? null : name.trim(), t, new Func.Done() {
            @Override
            public void ok(String n, Target out) {
                done.ok(result(n, out));
            }
        });
    }

    private static Result result(String n, Target out) {
        Result r = new Result();
        r.name = n == null ? "" : n.trim();
        r.cond = out.get(SmartLists.COND, "");
        r.sort = out.get(SmartLists.SORT, "");
        r.view = out.get(SmartLists.VIEW, "");
        String link = out.get(SmartLists.LINK, "");
        if (r.cond.isEmpty() && !link.isEmpty()) {                     // נבחרה תבנית — התנאי שלה (במסד אין קישור)
            SmartLists.Named l = SmartLists.byId(link);
            if (l != null) {
                r.cond = l.cond;
                if (r.view.isEmpty()) r.view = l.view;
            }
        }
        r.icon = out.get(Program.ICON, "");
        r.desc = out.get(DESC, "");
        r.why = out.get(Program.WHY, "");
        return r;
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

    private static final String DESC = "תיאור";  // תחביר (המפתח בטופס, כמו SmartForm.detailKeys)

    private static String v(Store s, long id, String k) {
        String x = s.prop(id, k);
        return x == null ? "" : x;
    }
}
