package com.tikiyot.vfm;

// מבנה 275 (ממצא 65; המשתמש, 18.11.2026, על מ0.50 — הקיפול בגלילה של 272: "הגלילה והפסים לא מוצלח, קופצני. כשאני מדפדף למעלה
// הוא עושה חלון מלא, למטה סותם הכל בקפיצות. אולי עדיף פשוט אזור דק שנגיעה או גרירה בו מסלקת או מחזירה את השורה").
// הקיפול בגלילה ירד. כאן — הכלל של הרצועה הדקה בלבד, בלי מסך (נבדק במחשב): מה עושה נגיעה, ומה עושה גרירה. הקיפול עצמו — ב־ScrollRoom.

final class FoldTrack {

    static final int NONE = 0, FOLD = 1, OPEN = 2;

    private FoldTrack() { }

    /** האצבע זזה פחות מ־step (בנקודות מסך) — נגיעה, לא גרירה. */
    static boolean tap(float dy, float step) {
        return Math.abs(dy) < step;
    }

    /**
     * האצבע עזבה את הרצועה, אחרי שזזה dy (שלילי — למעלה). נגיעה — מקפלת או מחזירה; גרירה למעלה — מקפלת; גרירה למטה — מחזירה;
     * גרירה לכיוון של מה שכבר נעשה — כלום.
     */
    static int release(float dy, float step, boolean folded) {
        if (tap(dy, step)) return folded ? OPEN : FOLD;
        if (dy < 0) return folded ? NONE : FOLD;
        return folded ? OPEN : NONE;
    }
}
