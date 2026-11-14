package com.tikiyot.vfm;

// מבנה 57 (14.11.2026): פעולת כונן ארוכה שמתחילה מהמסך (העתקה, העברה, מחיקה, אריזה, פריסה) נמשכת גם כשהמסך כבוי —
// אותו פתרון שנבדק במכשיר להרצת קובץ פקודות (מבנה 38, 51): החזקת המעבד, והודעה קבועה שאומרת למערכת שהתוכנה עובדת.
// המשפך: ההודעה עולה רק אם הפעולה נמשכת יותר משנייה וחצי — מחיקה של קובץ אחד לא מהבהבת בשורת ההודעות.
// אחראי: להחזיק את התוכנה ערה מתחילת הפעולה עד סופה. אסור לו: לדעת מה הפעולה.

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;

final class LongOp {

    private static final long LATE_MS = 1500;

    private final Activity a;
    private final String text;
    private final Handler h = new Handler(Looper.getMainLooper());
    private android.os.PowerManager.WakeLock awake;
    private boolean shown, ended;

    private final Runnable late = new Runnable() {
        @Override
        public void run() {
            synchronized (LongOp.this) {
                if (ended) return;
                shown = true;
                RunService.begin(a, text);
            }
        }
    };

    private LongOp(Activity a, String text) {
        this.a = a;
        this.text = text;
    }

    /** לפני הפעולה (מכל תהליכון). */
    static LongOp begin(Activity a, String text) {
        LongOp o = new LongOp(a, text);
        o.awake = Awake.hold(a);
        o.h.postDelayed(o.late, LATE_MS);
        return o;
    }

    /** אחרי הפעולה — ב־finally. */
    void end() {
        synchronized (this) {
            ended = true;
            h.removeCallbacks(late);
            if (shown) RunService.end(a);
        }
        Awake.release(awake);
    }
}
