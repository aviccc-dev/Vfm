package com.tikiyot.vfm;

// מבנה 272 (ממצא 65; המשתמש, 18.11.2026): מקום לקבצים. המסך עם שורות הכפתורים סביבו (TopRow.wrap) — וכשגוללים בקבצים למטה,
// הכותרת של המסך, שורות הכפתורים שלמעלה ושלמטה, ומה שהמסך סימן כ"מתקפל" (FOLD_TAG) — יורדים מהמסך, והקבצים מקבלים את המקום.
// גוללים מעט חזרה למעלה, הרשימה מגיעה לראש, או שהמסך חוזר לפנים — הכול חוזר. עצירה ונגיעה בקובץ — שום דבר לא זז.
// במקום אחד, לכל המסכים שיש בהם רשימה; מסך בלי רשימה (הגדרות, תוסף) — כמו שהיה. הכלל — ב־FoldTrack (נבדק במחשב); כאן — רק
// הקשב לאצבע (בלי לקחת את הנגיעה מהרשימה) והקיפול.

import android.app.ActionBar;
import android.app.Activity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.LinearLayout;

final class ScrollRoom extends LinearLayout {

    /** מה שמעל הקבצים ומתקפל יחד עם השורות — המסך מסמן (למשל החלק שמתחת לשורת החיפוש). */
    static final String FOLD_TAG = "fold-with-rows";  // תחביר

    private final Activity a;
    private final FoldTrack track;
    private View top, content, bottom;
    private boolean inList, touching;
    private final ViewTreeObserver.OnScrollChangedListener atTop = new ViewTreeObserver.OnScrollChangedListener() {
        @Override
        public void onScrollChanged() {                                       // תנופה שהגיעה לראש הרשימה, בלי אצבע
            if (touching || !track.folded()) return;
            AbsListView l = list(content);
            if (l != null && !l.canScrollVertically(-1)) apply(track.rest());
        }
    };

    ScrollRoom(Activity a) {
        super(a);
        this.a = a;
        track = new FoldTrack(Ui.dp(a, 24));
    }

    /** החלקים: השורות שלמעלה, המסך עצמו, והשורות שלמטה (בתוך מחזיק משלהן — פס הבחירה מסתיר את השורות עצמן). */
    void parts(View top, View content, View bottom) {
        this.top = top;
        this.content = content;
        this.bottom = bottom;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent e) {
        try {
            watch(e);
        } catch (RuntimeException x) {
            Log.e("ScrollRoom", x);
        }
        return super.dispatchTouchEvent(e);
    }

    /** רק מקשיבים: הנגיעה ממשיכה לרשימה כרגיל. הגובה — על המסך (לא בתוך המסגרת, שזזה כשהכותרת מתקפלת). */
    private void watch(MotionEvent e) {
        int act = e.getActionMasked();
        if (act == MotionEvent.ACTION_DOWN) {
            touching = true;
            AbsListView l = list(content);
            inList = l != null && inside(l, e.getRawX(), e.getRawY());
            track.down(e.getRawY());
        } else if (act == MotionEvent.ACTION_UP || act == MotionEvent.ACTION_CANCEL) {
            touching = false;
        } else if (act == MotionEvent.ACTION_MOVE && inList) {
            AbsListView l = list(content);
            apply(track.move(e.getRawY(), l != null && l.canScrollVertically(1)));
        }
    }

    private void apply(int what) {
        if (what == FoldTrack.NONE || top == null) return;
        int v = what == FoldTrack.FOLD ? GONE : VISIBLE;
        top.setVisibility(v);
        bottom.setVisibility(v);
        View more = content.findViewWithTag(FOLD_TAG);
        if (more != null) more.setVisibility(v);
        ActionBar bar = a.getActionBar();
        if (bar != null) {
            if (what == FoldTrack.FOLD) bar.hide();
            else bar.show();
        }
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

    /** המסך חזר לפנים (ממסך אחר) — הכול מוצג. */
    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == VISIBLE) apply(track.rest());
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getViewTreeObserver().addOnScrollChangedListener(atTop);
    }

    @Override
    protected void onDetachedFromWindow() {
        getViewTreeObserver().removeOnScrollChangedListener(atTop);
        super.onDetachedFromWindow();
    }

    /** הרשימה שעל המסך (שורות או רשת); המסך יכול להחליף אותה, ולכן מחפשים בכל נגיעה. אין — null. */
    private static AbsListView list(View v) {
        if (v == null || v.getVisibility() != VISIBLE) return null;
        if (v instanceof AbsListView) return (AbsListView) v;
        if (!(v instanceof ViewGroup)) return null;
        ViewGroup g = (ViewGroup) v;
        for (int i = 0; i < g.getChildCount(); i++) {
            AbsListView l = list(g.getChildAt(i));
            if (l != null) return l;
        }
        return null;
    }

    private static boolean inside(View v, float x, float y) {
        int[] at = new int[2];
        v.getLocationOnScreen(at);
        return x >= at[0] && x < at[0] + v.getWidth() && y >= at[1] && y < at[1] + v.getHeight();
    }
}
