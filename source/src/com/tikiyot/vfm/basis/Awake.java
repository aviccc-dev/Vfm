package com.tikiyot.vfm;

// להישאר ער בזמן עבודה ארוכה. אחראי: להחזיק את המעבד פועל גם כשהמסך כבוי, ולשחרר בסוף. אסור לו: להקריס, להציג מסכים.
// מבנה 38 (המשתמש, 14.11.2026: "שהפקודה תמשיך גם כאשר המסך כבוי"). עד כאן שום דבר לא החזיק את המכשיר ער,
// והרצה ארוכה נעצרה כשהמסך כבה (ממצא 31).
final class Awake {

    private Awake() { }

    /** גבול עליון — כדי שתקלה לא תשאיר את המכשיר ער לתמיד: אחרי שעתיים ההחזקה פוקעת מעצמה. */
    static final long MAX_MS = 2L * 60 * 60 * 1000;

    /** מתחיל להחזיק את המעבד ער. מחזיר את ההחזקה — למסור ל־release בסוף (ב־finally). null — לא הצליח (ממשיכים בלי). */
    static android.os.PowerManager.WakeLock hold(android.content.Context c) {
        try {
            android.os.PowerManager pm = (android.os.PowerManager) c.getApplicationContext().getSystemService(android.content.Context.POWER_SERVICE);
            android.os.PowerManager.WakeLock w = pm.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "vfm:run");
            w.setReferenceCounted(false);
            w.acquire(MAX_MS);
            return w;
        } catch (Throwable t) {
            Log.e("Awake.hold", t);
            return null;
        }
    }

    static void release(android.os.PowerManager.WakeLock w) {
        try {
            if (w != null && w.isHeld()) w.release();
        } catch (Throwable t) {
            Log.e("Awake.release", t);
        }
    }
}
