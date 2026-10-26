package com.tikiyot.vfm;

// 197: שורה שגולשת — כל רכיב ברוחב הטבעי שלו, וכשאין מקום — לשורה הבאה (מימין לשמאל).
// כך תיבות הסוגים והכפתורים הקטנים לא נדחסים ולא נשברים לשתי שורות, גם בגופן גדול במכשיר.
// אחראי: מדידה והנחה בלבד.

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

final class Flow extends ViewGroup {

    private final int hGap, vGap;

    Flow(Context c, int hGapDp, int vGapDp) {
        super(c);
        hGap = Ui.dp(c, hGapDp);
        vGap = Ui.dp(c, vGapDp);
        setLayoutDirection(Ui.DIRECTION);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        int mode = MeasureSpec.getMode(wSpec);
        int max = mode == MeasureSpec.UNSPECIFIED ? Integer.MAX_VALUE / 2
                : MeasureSpec.getSize(wSpec) - getPaddingLeft() - getPaddingRight();
        int x = 0, y = 0, lineH = 0, widest = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View c = getChildAt(i);
            if (c.getVisibility() == GONE) continue;
            c.measure(MeasureSpec.makeMeasureSpec(Math.max(0, max), MeasureSpec.AT_MOST),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            int cw = c.getMeasuredWidth(), ch = c.getMeasuredHeight();
            if (x > 0 && x + cw > max) {
                y += lineH + vGap;
                x = 0;
                lineH = 0;
            }
            x += cw;
            widest = Math.max(widest, x);
            x += hGap;
            lineH = Math.max(lineH, ch);
        }
        int w = mode == MeasureSpec.EXACTLY ? MeasureSpec.getSize(wSpec)
                : Math.min(widest + getPaddingLeft() + getPaddingRight(),
                mode == MeasureSpec.AT_MOST ? MeasureSpec.getSize(wSpec) : Integer.MAX_VALUE);
        setMeasuredDimension(w, resolveSize(y + lineH + getPaddingTop() + getPaddingBottom(), hSpec));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        boolean rtl = getLayoutDirection() == LAYOUT_DIRECTION_RTL;
        int width = r - l, max = width - getPaddingLeft() - getPaddingRight();
        int x = 0, y = getPaddingTop(), lineH = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View c = getChildAt(i);
            if (c.getVisibility() == GONE) continue;
            int cw = c.getMeasuredWidth(), ch = c.getMeasuredHeight();
            if (x > 0 && x + cw > max) {
                y += lineH + vGap;
                x = 0;
                lineH = 0;
            }
            int left = rtl ? width - getPaddingRight() - x - cw : getPaddingLeft() + x;
            c.layout(left, y, left + cw, y + ch);
            x += cw + hGap;
            lineH = Math.max(lineH, ch);
        }
    }
}
