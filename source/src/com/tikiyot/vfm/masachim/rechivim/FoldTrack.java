package com.tikiyot.vfm;

// מבנה 272 (המשתמש, 18.11.2026 — ממצא 65: "כמעט אין מקום לדפדף ולגעת בקבצים… אולי לאפשר להסתיר ולגלות ולצמצם בין החלקים,
// באופן שיישאר יותר ריווח לקבצים" · "כאשר מדפדפים בקבצים השורה למעלה ולמטה מוסתרות, כאשר עוזבים הן חוזרות"; והוסכם — "כן":
// חוזרות כשגוללים מעט חזרה למעלה, ולא כשעוצרים, כדי שהרשימה לא תזוז מתחת לאצבע רגע לפני הנגיעה בקובץ).
// כאן — הכלל בלבד, בלי מסך (נבדק במחשב): מתי מקפלים ומתי פותחים, לפי תנועת האצבע. הקיפול עצמו — ב־ScrollRoom.

final class FoldTrack {

    static final int NONE = 0, FOLD = 1, OPEN = 2;

    /** כמה האצבע צריכה לזוז (בנקודות מסך) כדי שזה ייחשב גלילה, ולא רעד של נגיעה. */
    private final float step;
    private float anchor;
    private boolean folded;

    FoldTrack(float step) {
        this.step = step;
    }

    boolean folded() {
        return folded;
    }

    /** האצבע נגעה (y — הגובה על המסך). */
    void down(float y) {
        anchor = y;
    }

    /**
     * האצבע זזה. canDown — יש עוד קבצים מתחת (רשימה שכולה על המסך אינה מקפלת כלום). האצבע עלתה step מהמקום הנמוך ביותר שלה —
     * גוללים למטה: FOLD. ירדה step מהמקום הגבוה ביותר שלה כשמקופל — גוללים חזרה למעלה: OPEN. עצירה — אינה משנה דבר.
     */
    int move(float y, boolean canDown) {
        if (folded) {
            if (y < anchor) anchor = y;
            else if (y - anchor >= step) {
                anchor = y;
                folded = false;
                return OPEN;
            }
            return NONE;
        }
        if (y > anchor) anchor = y;
        else if (anchor - y >= step) {
            anchor = y;
            if (!canDown) return NONE;
            folded = true;
            return FOLD;
        }
        return NONE;
    }

    /** הרשימה הגיעה לראש בלי אצבע (תנופה), או שהמסך חזר לפנים — הכול חוזר. */
    int rest() {
        if (!folded) return NONE;
        folded = false;
        return OPEN;
    }
}
