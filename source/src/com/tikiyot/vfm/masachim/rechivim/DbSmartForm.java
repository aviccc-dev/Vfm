package com.tikiyot.vfm;

// מ0.10.1: רשימה חכמה במסד — בטופס המוכר (SmartForm: מאיפה, סוג, תאריך, גודל, מיון, תצוגה, צלמית, תיאור, "למה"),
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

    /** הטופס; smart — לעריכה (0 — חדשה). s — לקריאת הערכים הקיימים בלבד. */
    static void show(Activity a, Store s, long smart, final Done done) {
        Target t = null;
        String name = null;
        if (smart != 0) {
            name = s.item(smart).name;
            t = Target.of(FnSmart.KIND, SmartLists.COND, v(s, smart, Store.P_COND), SmartLists.SORT, v(s, smart, Store.P_SORT),
                    SmartLists.VIEW, v(s, smart, Store.P_VIEW), Program.ICON, v(s, smart, Store.P_ICON),
                    DESC, v(s, smart, Store.P_DESC), Program.WHY, v(s, smart, Store.P_WHY));
        }
        SmartForm.show(a, name, t, new Func.Done() {
            @Override
            public void ok(String n, Target out) {
                done.ok(result(n, out));
            }
        });
    }

    /**
     * מ0.28 (המשתמש: "כפולים אמור להיות באותו תפריט של רשימה חכמה בתור עוד אופציה"): רשימה חכמה חדשה — הטופס עם הפס
     * "סוג: חכמה | כפולים" (SearchTabs, כמו בבית הישן). חכמה — done; כפולים — dups (השם וההגדרות של הכפולים).
     */
    static void showNew(Activity a, final Done done, final Func.Done dups) {
        SearchTabs.configure(a, FnSmart.KIND, null, null, new Func.Done() {
            @Override
            public void ok(String n, Target out) {
                if (FnDup.KIND.equals(out.kind)) dups.ok(n, out);
                else done.ok(result(n, out));
            }
        });
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

    /** שומר בעסקה אחת: smart = 0 — נוצרת (בתוך into, אם לא 0). מחזיר את מספר הזיהוי. */
    static long save(Store s, final long smart, final long into, final Result r) {
        final long[] out = {smart};
        s.atomic(new Store.Work() {
            @Override
            public void run(Store t) {
                if (out[0] == 0) out[0] = t.createSmart(r.name.isEmpty() ? Words.SMART_LIST : r.name, into);
                else if (!r.name.isEmpty()) t.rename(out[0], r.name);
                t.setProp(out[0], Store.P_COND, r.cond);
                t.setProp(out[0], Store.P_SORT, r.sort);
                t.setProp(out[0], Store.P_VIEW, r.view);
                t.setProp(out[0], Store.P_ICON, r.icon);
                t.setProp(out[0], Store.P_DESC, r.desc);
                t.setProp(out[0], Store.P_WHY, r.why);
            }
        }, true);
        return out[0];
    }

    private static final String DESC = "תיאור";  // תחביר (המפתח בטופס, כמו SmartForm.detailKeys)

    private static String v(Store s, long id, String k) {
        String x = s.prop(id, k);
        return x == null ? "" : x;
    }
}
