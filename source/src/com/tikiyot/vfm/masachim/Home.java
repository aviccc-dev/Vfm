package com.tikiyot.vfm;

// נקודת הכניסה של התוכנה (המסך שהמערכת מפעילה). מ0.26 — מחיקת המנוע הישן, שלב 2: הבית הישן (אריחים וכלים מהקובץ
// הראשי) נמחק; הבית הוא הרשימה "בית" של המסד (DbListScreen). אחראי: לפתוח את הבית של המסד, ו"בית" מכל מסך.

import android.app.Activity;
import android.os.Bundle;

public class Home extends Activity {

    /**
     * "בית" מכל מסך (באג 30; המשתמש: "כפתור בית לא תמיד עובד… מבצע 'חזור' לעיתים"): הבית נפתח מחדש, וכל המסכים
     * שמעליו נסגרים יחד. עריכה שלא נשמרה — לא מנקים (כדי לא לאבד אותה).
     */
    static void go(Activity a) {
        if (a instanceof DbListScreen && ((DbListScreen) a).isHomeScreen()) return;
        DbScreen.startFresh(a);
    }

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        Log.install();
        Log.initVersion(this);
        DbScreen.start(this);
        finish();
    }
}
