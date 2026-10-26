package com.tikiyot.vfm;

// 194: חתימות התוכן גם כשהתוכנה סגורה. אנדרואיד מקפיא תוכנה שברקע, ולכן העבודה שבתוך התוכנה נעצרת כשיוצאים
// ממנה. כשהמסך האחרון יוצא ונשארה עבודה — מתוזמנת "עבודה" של המערכת (JobScheduler): רצה ברצף עד כ־9 דקות,
// ואם נשאר — מתוזמנת שוב. אחראי: רק להפעיל את Sig ולחכות לו. אסור לו: לגעת בקבצים.

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;

public final class SigJob extends JobService {

    static final int ID = 190;
    static final long MAX_MS = 9 * 60_000L;

    static void schedule(Context c) {
        try {
            JobScheduler js = (JobScheduler) c.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            if (js == null) return;
            js.schedule(new JobInfo.Builder(ID, new ComponentName(c, SigJob.class))
                    .setMinimumLatency(20_000L)
                    .setOverrideDeadline(15 * 60_000L)
                    .build());
        } catch (Throwable t) {
            Log.e("תזמון החתימות", t);
        }
    }

    @Override
    public boolean onStartJob(final JobParameters params) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean more = false;
                try {
                    Library lib = Library.get(SigJob.this);
                    lib.start(null);
                    long end = System.currentTimeMillis() + MAX_MS;
                    for (int i = 0; i < 300 && !lib.ready; i++) Thread.sleep(200);
                    Sig.fast = true;                                         // אין מי שהעבודה תפריע לו
                    Sig.kick(lib);
                    Thread.sleep(500);
                    while (Sig.running() && System.currentTimeMillis() < end) Thread.sleep(1000);
                    more = Sig.needed(lib);
                } catch (Throwable t) {
                    Log.e("חתימות — עבודה ברקע", t);
                } finally {
                    jobFinished(params, false);
                    if (more) schedule(SigJob.this);
                }
            }
        }, "חתימות — עבודת מערכת").start();  // תחביר
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        schedule(this);                                                      // המערכת עצרה — ננסה שוב אחר כך
        return false;
    }
}
