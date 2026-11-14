package com.tikiyot.vfm;

// מבנה 51 (14.11.2026): הרצה של קובץ פקודות נמשכת גם כשהמסך כבוי — שירות קדמי עם הודעה קבועה, רק בזמן ההרצה.
// למה: בדיקת המכשיר של נ0.3 — המשתמש כיבה את המסך, והדוח אמר בעצמו "התוכנה לא רצה 561.2 שניות": החזקת המעבד (Awake,
// מבנה 38) לא הספיקה, המערכת עצרה את התוכנה. שירות קדמי הוא הדרך של אנדרואיד לומר "התוכנה עובדת עכשיו, לא לעצור".
// אחראי: להודיע למערכת שהרצה פועלת. אסור לו: להריץ בעצמו, ולהחליט מה רץ.

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

public class RunService extends Service {

    private static final String STOP = "stop";
    /** כמה הרצות פועלות עכשיו (הרצה בתוך הרצה — השירות נעצר רק כשהאחרונה נגמרה). */
    private static int runs;

    /** לפני ההרצה. תקלה — ביומן, וההרצה ממשיכה בלי השירות. */
    static synchronized void begin(Context c) {
        try {
            if (runs++ > 0) return;
            Intent i = new Intent(c.getApplicationContext(), RunService.class);
            if (Build.VERSION.SDK_INT >= 26) c.getApplicationContext().startForegroundService(i);
            else c.getApplicationContext().startService(i);
        } catch (Throwable t) {
            Log.e("RunService.begin", t);
        }
    }

    /**
     * אחרי ההרצה (ב־finally). העצירה נשלחת לשירות עצמו ולא ב־stopService: הרצה של אלפית שנייה נגמרת לפני שהשירות הספיק
     * לעלות, ואנדרואיד מפיל תוכנה שביקשה שירות קדמי ועצרה אותו לפני שהציג את ההודעה.
     */
    static synchronized void end(Context c) {
        try {
            if (--runs > 0) return;
            runs = 0;
            c.getApplicationContext().startService(new Intent(c.getApplicationContext(), RunService.class).setAction(STOP));
        } catch (Throwable t) {
            Log.e("RunService.end", t);
            try {
                c.getApplicationContext().stopService(new Intent(c.getApplicationContext(), RunService.class));
            } catch (Throwable ignored) {
                Log.e("RunService.end: stop", ignored);
            }
        }
    }

    @Override public IBinder onBind(Intent i) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel("run", Words.Run.SERVICE_CHANNEL, NotificationManager.IMPORTANCE_LOW);
            if (nm != null) nm.createNotificationChannel(ch);
            b = new Notification.Builder(this, "run");
        } else {
            b = new Notification.Builder(this);
        }
        b.setSmallIcon(android.R.drawable.ic_popup_sync).setContentTitle(Words.Main.APP_NAME).setContentText(Words.Run.SERVICE_ACTIVE).setOngoing(true);
        startForeground(2, b.build());
    }

    @Override
    public int onStartCommand(Intent i, int f, int id) {
        if (i == null || STOP.equals(i.getAction())) {          // נגמרה; או שהמערכת הפעילה מחדש בלי הרצה — אין מה להחזיק
            stopForeground(true);
            stopSelf();
        }
        return START_NOT_STICKY;
    }
}
