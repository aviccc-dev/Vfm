package com.tikiyot.vfm;

// מקום לקבצים (ממצא 65). המסך עם שורות הכפתורים סביבו (TopRow.wrap), ובין השורות שלמעלה למסך — רצועה דקה.
// מבנה 275 (המשתמש, 18.11.2026, על מ0.50: "אולי עדיף פשוט אזור דק שנגיעה או גרירה בו מסלקת או מחזירה את השורה"; במקום הקיפול
// בגלילה של 272, שיצא קופצני): נגיעה ברצועה, או גרירה ממנה למעלה — הכותרת של המסך, שורות הכפתורים שלמעלה ושלמטה, ומה שהמסך
// סימן כ"מתקפל" (FOLD_TAG; בחיפוש — מה שבין שורת החיפוש לקבצים) — יורדים; הרצועה נשארת. נגיעה, או גרירה למטה — חוזרים.
// מבנה 278: הכותרת של אנדרואיד מוסתרת — שם המסך וה־⋮ ברצועה שלמעלה. מבנה 277: שתי רצועות, עם חץ — למעלה ולמטה, כל אחת לשורות שלה. הגלילה בקבצים — כמו תמיד, בלי שום קיפול. המצב — אחד לכל המסכים, כל עוד התוכנה פתוחה (מסך חדש נפתח מקופל, אם קיפלו);
// אחרי הפעלה מחדש — הכול מוצג. במקום אחד, לכל המסכים. הכלל — ב־FoldTrack (נבדק במחשב); כאן — הרצועה והקיפול.

import android.app.ActionBar;
import android.app.Activity;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

final class ScrollRoom extends LinearLayout {

    /** מה שמעל הקבצים ומתקפל יחד עם השורות — המסך מסמן (למשל החלק שמתחת לשורת החיפוש). */
    static final String FOLD_TAG = "fold-with-rows";  // תחביר

    /** מקופל או לא — למעלה ולמטה בנפרד; אחד לכל המסכים, כל עוד התוכנה פתוחה. */
    private static boolean foldedTop, foldedBottom;

    private final Activity a;
    private View top, content, bottom;
    private android.widget.TextView stripTop, stripBottom, name, more;
    private static final String MORE = "⋮";  // תחביר
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
        ActionBar bar = a.getActionBar();                                     // מבנה 278: הכותרת של אנדרואיד — מוסתרת; שם המסך וה־⋮ ברצועה
        if (bar != null) bar.hide();
    }

    /**
     * מבנה 277 (המשתמש, 19.11.2026: "חשבתי שהיא כמו המעלית בצד שמראה תנועה… כדאי לצמצם אותה יותר לכיוון הפס, לתת לה חיצים קטנים,
     * ולהוסיף גם למטה בפס כזו אחת"): רצועה צרה עם חץ קטן שמראה מה הנגיעה תעשה. למעלה (מתחת לשורות שלמעלה) — הכותרת, השורות
     * שלמעלה ומה שהמסך סימן; למטה (מעל השורות שלמטה) — השורות שלמטה. נגיעה מחליפה; גרירה לכיוון החץ — גם.
     */
    View strip(final boolean low) {
        final LinearLayout s = new LinearLayout(a);
        s.setGravity(Gravity.CENTER_VERTICAL);
        s.setBackgroundColor(Ui.BG_SOFT);
        android.widget.TextView arrow = small(9, Ui.SUB);
        arrow.setGravity(Gravity.CENTER);
        if (low) {
            s.setMinimumHeight(Ui.dp(a, 13));
            s.addView(arrow, new LayoutParams(0, -2, 1f));
            stripBottom = arrow;
        } else {
            // מבנה 278 (המשתמש, 19.11.2026, על הכותרת השחורה: "כל האזור המסומן מבוזבז"): הכותרת של אנדרואיד מוסתרת, ושם המסך וה־⋮
            // (התפריט של המסך) — כאן, ברצועה: שם המסך בצד אחד, החץ באמצע, ⋮ בצד השני. הרצועה נשארת גם כשהשורות מקופלות.
            s.setMinimumHeight(Ui.dp(a, 30));
            s.setPadding(Ui.dp(a, 12), 0, Ui.dp(a, 4), 0);
            name = small(15, Ui.TEXT);
            name.setSingleLine(true);
            name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            name.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            s.addView(name, new LayoutParams(0, -2, 1f));
            s.addView(arrow, new LayoutParams(Ui.dp(a, 40), -2));
            LinearLayout end = new LinearLayout(a);
            end.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
            more = small(20, Ui.TEXT);
            more.setText(MORE);
            more.setGravity(Gravity.CENTER);
            more.setContentDescription(Words.Top.SCREEN_MENU);
            more.setOnClickListener(new OnClickListener() {
                @Override public void onClick(View v) { menu(v); }
            });
            end.addView(more, new LayoutParams(Ui.dp(a, 44), Ui.dp(a, 30)));
            s.addView(end, new LayoutParams(0, -2, 1f));
            stripTop = arrow;
        }
        s.setClickable(true);
        s.setOnClickListener(new OnClickListener() {                         // נגיעה — וגם מקריא המסך
            @Override public void onClick(View v) { apply(low, (low ? foldedBottom : foldedTop) ? FoldTrack.OPEN : FoldTrack.FOLD); }
        });
        s.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent e) {
                int act = e.getActionMasked();
                if (act == MotionEvent.ACTION_DOWN) downY = e.getRawY();
                else if (act == MotionEvent.ACTION_UP) {
                    float dy = e.getRawY() - downY, step = Ui.dp(a, 12);
                    if (FoldTrack.tap(dy, step)) v.performClick();
                    else apply(low, FoldTrack.release(low ? -dy : dy, step, low ? foldedBottom : foldedTop));   // למטה — הכיוונים הפוכים
                }
                return true;
            }
        });
        say();
        return s;
    }

    private android.widget.TextView small(int size, int color) {
        android.widget.TextView t = new android.widget.TextView(a);
        t.setIncludeFontPadding(false);
        t.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, size);
        t.setTextColor(color);
        return t;
    }

    /** מבנה 278: התפריט של המסך (מה שהיה ב־⋮ של הכותרת) — אותו תפריט, אותן פעולות: המסך בונה אותו, והמסך מקבל את הבחירה. */
    private void menu(View anchor) {
        android.widget.PopupMenu pm = new android.widget.PopupMenu(a, anchor);
        a.onCreateOptionsMenu(pm.getMenu());
        a.onPrepareOptionsMenu(pm.getMenu());
        if (!pm.getMenu().hasVisibleItems()) return;
        pm.setOnMenuItemClickListener(new android.widget.PopupMenu.OnMenuItemClickListener() {
            @Override public boolean onMenuItemClick(android.view.MenuItem item) { return a.onOptionsItemSelected(item); }
        });
        pm.show();
    }

    private void apply(boolean low, int what) {
        if (what == FoldTrack.NONE) return;
        if (low) foldedBottom = what == FoldTrack.FOLD;
        else foldedTop = what == FoldTrack.FOLD;
        show();
    }

    /** מראים או מסתירים לפי המצב (האחד לכל המסכים). */
    private void show() {
        if (top == null) return;
        int v = foldedTop ? GONE : VISIBLE;
        top.setVisibility(v);
        View more = content.findViewWithTag(FOLD_TAG);
        if (more != null) more.setVisibility(v);
        bottom.setVisibility(foldedBottom ? GONE : VISIBLE);
        say();
    }

    /** החץ — לאן השורות ילכו בנגיעה; ומה הרצועה תעשה — למקריא המסך (במילון). */
    private void say() {
        if (stripTop != null) {
            stripTop.setText(FoldTrack.arrow(false, foldedTop));
            ((View) stripTop.getParent()).setContentDescription(foldedTop ? Words.Top.UNFOLD_ROWS : Words.Top.FOLD_ROWS);
        }
        if (stripBottom != null) {
            stripBottom.setText(FoldTrack.arrow(true, foldedBottom));
            ((View) stripBottom.getParent()).setContentDescription(foldedBottom ? Words.Top.UNFOLD_LOW : Words.Top.FOLD_LOW);
        }
        title();
    }

    /** שם המסך ברצועה — מה שהמסך קבע (setTitle); המסך משנה אותו בדרך, ולכן נבדק לפני כל ציור. */
    private void title() {
        if (name == null) return;
        CharSequence t = a.getTitle();
        String now = t == null ? "" : t.toString();
        if (!now.contentEquals(name.getText())) name.setText(now);
    }

    private final android.view.ViewTreeObserver.OnPreDrawListener fresh = new android.view.ViewTreeObserver.OnPreDrawListener() {
        @Override public boolean onPreDraw() { title(); return true; }
    };

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getViewTreeObserver().addOnPreDrawListener(fresh);
    }

    @Override
    protected void onDetachedFromWindow() {
        getViewTreeObserver().removeOnPreDrawListener(fresh);
        super.onDetachedFromWindow();
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
