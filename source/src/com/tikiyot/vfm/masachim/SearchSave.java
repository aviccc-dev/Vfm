package com.tikiyot.vfm;

// מבנה 290 (המסך התקרב ל־1,000 שורות): השמירה ממסך החיפוש — "💾 שמירה" אל הרשימה החכמה שעל המסך (287), "💾 שמירה בשם" (רשימה
// חכמה חדשה), ו"📥 לטעון" (289). עד כאן — בתוך SearchScreen, אותן שורות. אחראי: לאסוף מהמסך ולשלוח למנהל. אסור לו: לכתוב בעצמו.

final class SearchSave {

    private final SearchScreen s;
    /** מבנה 287: הרשימה החכמה שעל המסך (0 — אין): "💾 שמירה" כותב אליה את מה שבחלונית. */
    private String smartMode;
    private long smartId;
    /** מבנה 291: רשימה חכמה חדשה — האופן והרשימה שבתוכה (newMode = null — לא חדשה). */
    private String newMode;
    private long newIn;
    /** מבנה 292: העורך מבקש תנאי — "✓ לבחור" מחזיר אותו, ולא שומר. */
    private boolean pick;

    SearchSave(SearchScreen s) {
        this.s = s;
    }

    void read(android.content.Intent i) {
        smartMode = i.getStringExtra(SearchScreen.EXTRA_SMART_MODE);
        smartId = smartMode == null ? 0 : i.getLongExtra(SearchScreen.EXTRA_SMART_ID, 0);
        newMode = i.getStringExtra(SearchScreen.EXTRA_NEW_MODE);
        newIn = i.getLongExtra(SearchScreen.EXTRA_NEW_IN, 0);
        pick = i.getBooleanExtra(SearchScreen.EXTRA_PICK, false);
    }

    boolean picking() {
        return pick;
    }

    /** על המסך רשימה חכמה של המסד, או חדשה — יש "💾 שמירה". */
    boolean onSmart() {
        return smartId != 0 || newMode != null || pick;
    }

    /** מבנה 291: רשימה חכמה חדשה, שעוד לא נשמרה. */
    boolean isNew() {
        return (newMode != null && smartId == 0) || pick;                  // מבנה 292: גם "לבחור" — החלונית פתוחה
    }

    /** האופן שממנו נטענות התבניות: של הרשימה החכמה שעל המסך (או החדשה), אחרת — האופן הפתוח. */
    String mode() {
        return smartMode != null ? smartMode : newMode != null ? newMode : Db.currentMode;
    }

    /**
     * מבנה 287 (אפיון החיפוש, 6 — "מקום אחד לעריכה"): התנאי שבחלונית ובשורה, המיון והתצוגה — אל הרשימה החכמה שעל המסך, דרך המנהל
     * (אותה פקודה כמו בטופס: "עדכן רשימה חכמה"). הצלמית, התיאור וה"למה" — נשארים כמו שהם (נקראים מהמסד; הטופס מוחק מה שחסר).
     */
    /** מבנה 293: הכלל של הטופס הנפרד — עכשיו כאן, במקום האחד: "רק הסוגים שאבחר" בלי אף סוג — לא נשמר (אחרת — כל הסוגים, בשקט). */
    private boolean noKind() {
        if (!s.parts.noKind()) return false;
        Ui.toast(s, Words.Kinds.NONE_PICKED);
        return true;
    }

    void saveHere() {
        if (noKind()) return;
        if (pick) {
            pickDone();
            return;
        }
        final String q = SmartLists.orAll(s.query());                         // מבנה 294: ריק — כל הקבצים בכל הכוננים
        if (isNew()) {
            saveNew(q);
            return;
        }
        java.util.Map<String, String> props;
        try {
            props = Db.read(s, smartMode, new Db.Use<java.util.Map<String, String>>() {
                @Override public java.util.Map<String, String> run(SqlStore st) {
                    return st.item(smartId) == null ? null : Lists.smartProps(st, smartId);
                }
            });
        } catch (Throwable t) {
            Ui.toast(s, Words.Db.failed(String.valueOf(t.getMessage())));
            return;
        }
        if (props == null) {
            Ui.toast(s, Words.Db.ITEM_GONE);
            return;
        }
        props.put(Store.P_SORT, s.srt.by);
        props.put(Store.P_VIEW, s.view);
        Manager.Act act = Manager.onSmartEdit(smartMode, smartId, s.getTitle().toString(), q, props, Paths.data(), BatchUi.vars(s, Paths.data()), s.lib);
        Ui.toast(s, act.error() != null ? act.error() : act.say(s.lib));
    }

    /** מבנה 289: "📥 לטעון" — התנאי של תבנית (ההגדרות לחלונית, המילים לשורה), המיון והתצוגה שלה. */
    void load(String cond, String sort, String v) {
        String[] sp = SmartLists.split(cond);
        s.parts.reload(s, sp[0]);
        s.input.setText(sp[1]);
        if (sort != null && !sort.isEmpty()) s.srt.prefer(sort);
        if (v != null && !v.isEmpty()) {
            s.view = v;
            s.placed = false;
            s.here = null;
        }
        s.search();
    }

    /**
     * "שמירה בשם" — רשימה חכמה חדשה במסד, בבית של האופן הפתוח, עם התנאי, המיון והתצוגה שעל המסך.
     * מ0.29: עד כאן נשמרה "הגדרה נפוצה" בקובץ הראשי של המנוע הישן (ולכן לא הופיעה במסד).
     */
    /**
     * מבנה 291: רשימה חכמה חדשה — השם (נשאל), ונוצרת בתוך הרשימה שממנה באו, דרך המנהל (אותו "צור רשימה חכמה" של ➕ עד כאן;
     * שם קיים — שואלים, מבנה 103). מכאן — המסך הוא של הרשימה החדשה: "💾 שמירה" מעדכן אותה.
     */
    private void saveNew(final String q) {
        Ui.askText(s, Words.Find.NEW_SMART_TITLE, s.input.getText().toString().trim(), Words.Short.NAME_HINT, new Ui.Text1() {
            @Override
            public void ok(String value) {
                final String n = value.trim();
                if (n.isEmpty()) {
                    Ui.toast(s, Words.NEED_NAME);
                    return;
                }
                final DbSmartForm.Result r = new DbSmartForm.Result();
                r.name = n;
                r.cond = q;
                r.sort = s.srt.by;
                r.view = s.view;
                DbVerbsUi.makeSmart(s, newMode, newIn, r, new DbVerbsUi.Made() {
                    @Override public void ok(long id) {
                        smartMode = newMode;
                        smartId = id;
                        s.setTitle(r.name);
                        s.invalidateOptionsMenu();
                        Ui.toast(s, Words.Find.saved(r.name));
                    }
                });
            }
        });
    }

    /**
     * מבנה 292: "✓ לבחור" — השם (נשאל, עם מה שבא מהעורך), והתנאי כמו שהוא בחלונית ובשורה: בלי המקום ובלי להחליף את %כאן% (כמו
     * שהטופס הנפרד החזיר — בשורה של קובץ פקודות %כאן% הוא משתנה). המסך נסגר, והעורך מקבל.
     */
    private void pickDone() {
        final String q = SmartLists.orAll(SmartLists.join(s.parts.build(), s.input.getText().toString()));   // מבנה 294
        Ui.askText(s, Words.Find.PICK, s.getTitle().toString(), Words.Short.NAME_HINT, new Ui.Text1() {
            @Override
            public void ok(String value) {
                android.content.Intent r = new android.content.Intent();
                r.putExtra(SearchScreen.PICKED_NAME, value.trim());
                r.putExtra(SearchScreen.PICKED_COND, q);
                r.putExtra(SearchScreen.PICKED_SORT, s.srt.by);
                r.putExtra(SearchScreen.PICKED_VIEW, s.view);
                s.setResult(android.app.Activity.RESULT_OK, r);
                s.finish();
            }
        });
    }

    void saveAs() {
        if (noKind()) return;
        final String q = SmartLists.orAll(s.query()), so = s.srt.by, w = s.view;   // מבנה 294
        Ui.askText(s, Words.Find.SAVE_AS_TITLE, s.input.getText().toString().trim(), Words.Short.NAME_HINT, new Ui.Text1() {   // מ0.30: שם משלו — לא "תבנית"
            @Override
            public void ok(String value) {
                final String n = value.trim();
                if (n.isEmpty()) {
                    Ui.toast(s, Words.NEED_NAME);
                    return;
                }
                final DbSmartForm.Result r = new DbSmartForm.Result();
                r.name = n;
                r.cond = q;
                r.sort = so;
                r.view = w;
                String mode = Db.currentMode;
                if (mode == null) {
                    Ui.toast(s, Words.DbCmd.NO_DB);
                    return;
                }
                DbVerbsUi.makeSmart(s, mode, 0, r, new DbVerbsUi.Made() {   // מבנה 97: דרך המנהל; בבית. מבנה 103: שם קיים — שואלים
                    @Override public void ok(long id) { Ui.toast(s, Words.Find.saved(r.name)); }
                });
            }
        });
    }
}
