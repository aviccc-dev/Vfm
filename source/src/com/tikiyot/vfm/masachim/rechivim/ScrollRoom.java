package com.tikiyot.vfm;

// מקום לקבצים (ממצא 65). המסך עם שורות הכפתורים סביבו (TopRow.wrap), ובין השורות שלמעלה למסך — רצועה דקה.
// מבנה 275 (המשתמש, 18.11.2026, על מ0.50: "אולי עדיף פשוט אזור דק שנגיעה או גרירה בו מסלקת או מחזירה את השורה"; במקום הקיפול
// בגלילה של 272, שיצא קופצני): נגיעה ברצועה, או גרירה ממנה למעלה — הכותרת של המסך, שורות הכפתורים שלמעלה ושלמטה, ומה שהמסך
// סימן כ"מתקפל" (FOLD_TAG; בחיפוש — מה שבין שורת החיפוש לקבצים) — יורדים; הרצועה נשארת. נגיעה, או גרירה למטה — חוזרים.
// הגלילה בקבצים — כמו תמיד, בלי שום קיפול. המצב — אחד לכל המסכים, כל עוד התוכנה פתוחה (מסך חדש נפתח מקופל, אם קיפלו);
// אחרי הפעלה מחדש — הכול מוצג. במקום אחד, לכל המסכים. הכלל — ב־FoldTrack (נבדק במחשב); כאן — הרצועה והקיפול.

import android.app.ActionBar;
import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

final class ScrollRoom extends LinearLayout {

    /** מה שמעל הקבצים ומתקפל יחד עם השורות — המסך מסמן (למשל החלק שמתחת לשורת החיפוש). */
    static final String FOLD_TAG = "fold-with-rows";  // תחביר

    /** מקופל או לא — אחד לכל המסכים, כל עוד התוכנה פתוחה. */
    private static boolean folded;

    private final Activity a;
    private View top, content, bottom, strip;
    private float downY;

    ScrollRoom(Activity a) {
        super(a);
        this.a = a;
    }

    /** החלקים: השורות שלמעלה, המסך עצמו, והשורות שלמטה (בתוך מחזיק משלהן — פס הבחירה מסתיר את השורות עצמן). */
    void parts(View top, View content, View bottom) {
        this.top = top;
        this.content = content;
        this.bottom = bottom;
    }

    /** הרצועה הדקה, עם ידית באמצע: נגיעה — מקפלת או מחזירה; גרירה ממנה למעלה — מקפלת; למטה — מחזירה. */
    View strip() {
        LinearLayout s = new LinearLayout(a);
        s.setGravity(Gravity.CENTER);
        s.setBackgroundColor(Ui.BG_SOFT);
        s.setMinimumHeight(Ui.dp(a, 18));
        View pill = new View(a);
        GradientDrawable g = new GradientDrawable();
        g.setColor(Ui.MUTED);
        g.setCornerRadius(Ui.dp(a, 2));
        pill.setBackground(g);
        s.addView(pill, new LayoutParams(Ui.dp(a, 40), Ui.dp(a, 4)));
        s.setClickable(true);
        s.setOnClickListener(new OnClickListener() {                         // נגיעה — וגם מקריא המסך
            @Override public void onClick(View v) { apply(folded ? FoldTrack.OPEN : FoldTrack.FOLD); }
        });
        s.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent e) {
                int act = e.getActionMasked();
                if (act == MotionEvent.ACTION_DOWN) downY = e.getRawY();
                else if (act == MotionEvent.ACTION_UP) {
                    float dy = e.getRawY() - downY, step = Ui.dp(a, 12);
                    if (FoldTrack.tap(dy, step)) v.performClick();
                    else apply(FoldTrack.release(dy, step, folded));
                }
                return true;
            }
        });
        strip = s;
        say();
        return s;
    }

    private void apply(int what) {
        if (what == FoldTrack.NONE) return;
        folded = what == FoldTrack.FOLD;
        show();
    }

    /** מראים או מסתירים לפי המצב (האחד לכל המסכים). */
    private void show() {
        if (top == null) return;
        int v = folded ? GONE : VISIBLE;
        top.setVisibility(v);
        bottom.setVisibility(v);
        View more = content.findViewWithTag(FOLD_TAG);
        if (more != null) more.setVisibility(v);
        ActionBar bar = a.getActionBar();
        if (bar != null) {
            if (folded) bar.hide();
            else bar.show();
        }
        say();
    }

    /** מה הרצועה תעשה — למקריא המסך (במילון). */
    private void say() {
        if (strip != null) strip.setContentDescription(folded ? Words.Top.UNFOLD_ROWS : Words.Top.FOLD_ROWS);
    }

    /** מסך שנפתח, או שחזר לפנים — במצב שבו השאירו אותו במסך אחר. */
    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == VISIBLE) show();
    }

    /**
     * מבנה 273: החלק שמתקפל עם השורות — המסך מוסיף לתוכו את מה שמעל הקבצים (בחיפוש: הכפתורים המהירים, "הגדרות" והחלונית, "למה",
     * שורת המידע, המקרא ו"איפה עומדים"; שורת החיפוש — מחוץ לו, ונשארת).
     */
    static LinearLayout head(Activity a) {
        LinearLayout h = new LinearLayout(a);
        h.setOrientation(VERTICAL);
        h.setTag(FOLD_TAG);
        return h;
    }

    /** מבנה 273: שורת מידע בשורה אחת, עם "…" — כדי שלא תיקח שתי שורות מהקבצים. */
    static void oneLine(android.widget.TextView t) {
        t.setSingleLine(true);
        t.setEllipsize(android.text.TextUtils.TruncateAt.END);
    }

    /** מבנה 273: נגיעה בשורה כזו — פותחת את כולה, ונגיעה נוספת — חזרה לשורה אחת (מה שנחתך אינו נעלם). */
    static void wide(android.widget.TextView t) {
        t.setSingleLine(t.getMaxLines() != 1);
    }
}
