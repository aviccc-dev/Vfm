package com.tikiyot.vfm;

// מ0.12: אופן העבודה "בדיקות מערכת" (המשתמש: "ואולי תבנה אופן של בדיקות ושם תריץ את הפקודות"; "כל הבדיקות — שיופיעו
// במסכים"; "הבדיקות ייעשו בפקודות בלבד"). הבדיקה עצמה — קובץ פקודות רגיל מתוך קובץ ההתקנה (assets/tests), לא קוד:
// כאן רק מתקינים — האופן (אם חסר), קובץ הפקודות בתיקייה שלו (נכתב מחדש בכל פעם, כדי שתמיד יהיה העדכני), ובבית שלו
// במסד — הקובץ. מריצים בנגיעה, ורואים הכול במסך ההרצה. האופנים האחרים לא נוגעים.
// אחראי: ההתקנה בלבד. אסור לו: להריץ בדיקות בעצמו.

import android.app.Activity;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Collections;
import java.util.List;

final class DbTestMode {

    static final String ASSET = "tests/pkudot-masad.vbat", ASSET_CALL = "tests/pkudot-kalul.vbat", ASSET_LOAD = "tests/omes-masad.vbat",
            ASSET_ALL = "tests/klali.vbat", ASSET_STRESS = "tests/lachatz.vbat", ASSET_V23 = "tests/gersa-023.vbat",
            ASSET_V24 = "tests/gersa-024.vbat";

    /** בקובץ בדיקת העומס: מוחלף בשם האופן עם המסד הגדול ביותר (הנתונים האמיתיים). */
    static final String REAL = "%האופן-האמיתי%";  // תחביר

    private DbTestMode() { }

    /** מ0.17.1: האופן שהמסד שלו הגדול ביותר — חוץ מאופן הבדיקות. אין — אופן הבדיקות עצמו. */
    static String realMode(Activity a, String test) {
        String best = test;
        long size = -1;
        for (String m : DbScreen.dbModes(a)) {
            if (m.equals(test)) continue;
            long n = DbScreen.dbFile(a, m).length();
            if (n > size) {
                size = n;
                best = m;
            }
        }
        return best;
    }

    static void open(final Activity a) {
        final String mode = Words.DbCmd.TEST_MODE;
        try {
            if (!Modes.dirOf(mode).isDirectory()) {
                String err = Modes.create(mode, null);
                if (err != null) {
                    Ui.toast(a, err);
                    return;
                }
            }
            final File f = new File(Modes.dirOf(mode), Words.DbCmd.TEST_FILE + "." + Batch.EXT);
            FileOutputStream out = new FileOutputStream(f);
            try {
                out.write(Examples.asset(a, ASSET).getBytes("UTF-8"));
            } finally {
                out.close();
            }
            FileOutputStream out2 = new FileOutputStream(new File(Modes.dirOf(mode), Words.DbCmd.TEST_CALL_FILE + "." + Batch.EXT));
            try {
                out2.write(Examples.asset(a, ASSET_CALL).getBytes("UTF-8"));   // מ0.15: הקובץ שהבדיקה כוללת
            } finally {
                out2.close();
            }
            final File load = new File(Modes.dirOf(mode), Words.DbCmd.TEST_LOAD_FILE + "." + Batch.EXT);
            FileOutputStream out3 = new FileOutputStream(load);
            try {
                out3.write(Examples.asset(a, ASSET_LOAD).replace(REAL, realMode(a, mode)).getBytes("UTF-8"));   // מ0.17: על הנתונים האמיתיים
            } finally {
                out3.close();
            }
            final File all = new File(Modes.dirOf(mode), Words.DbCmd.TEST_ALL_FILE + "." + Batch.EXT);
            FileOutputStream out4 = new FileOutputStream(all);
            try {
                out4.write(Examples.asset(a, ASSET_ALL).getBytes("UTF-8"));   // מ0.18: הבדיקה הכללית — מתעדכנת בכל גרסה
            } finally {
                out4.close();
            }
            final File stress = new File(Modes.dirOf(mode), Words.DbCmd.TEST_STRESS_FILE + "." + Batch.EXT);
            FileOutputStream out5 = new FileOutputStream(stress);
            try {
                out5.write(Examples.asset(a, ASSET_STRESS).getBytes("UTF-8"));   // מ0.21: בדיקת הלחץ — מנסה לסבך
            } finally {
                out5.close();
            }
            final File v23 = new File(Modes.dirOf(mode), Words.DbCmd.TEST_V23_FILE + "." + Batch.EXT);
            FileOutputStream out6 = new FileOutputStream(v23);
            try {
                out6.write(Examples.asset(a, ASSET_V23).getBytes("UTF-8"));   // מ0.23: בדיקת הגרסה (נכללת גם בכללית)
            } finally {
                out6.close();
            }
            final File v24 = new File(Modes.dirOf(mode), Words.DbCmd.TEST_V24_FILE + "." + Batch.EXT);
            FileOutputStream out7 = new FileOutputStream(v24);
            try {
                out7.write(Examples.asset(a, ASSET_V24).getBytes("UTF-8"));   // מ0.24: בדיקת הגרסה (נכללת גם בכללית)
            } finally {
                out7.close();
            }
            long home = DbVerbs.write(a, mode, new DbVerbs.Job() {
                @Override
                public long run(Store s, List<String> p) {
                    final long[] h = {Importer.home(s)};
                    s.atomic(new Store.Work() {
                        @Override public void run(Store t) {
                            if (h[0] == 0) {
                                h[0] = t.createList(Words.Db.HOME_NAME, 0);
                                t.setProp(h[0], Store.P_HOME, "1");
                            }
                        }
                    }, true);
                    DbFiles.add(s, h[0], java.util.Arrays.asList(all.getAbsolutePath(), v24.getAbsolutePath(), v23.getAbsolutePath(), f.getAbsolutePath(), stress.getAbsolutePath(), load.getAbsolutePath()), null, true);
                    return h[0];
                }
            }, null);
            if (home == 0) return;
            DbScreen.saveMode(a, mode);
            Ui.toast(a, Words.DbCmd.TEST_READY);
            DbListScreen.open(a, mode, home);
        } catch (Throwable t) {
            Log.e("DbTestMode", t);
            Ui.toast(a, Words.Db.failed(String.valueOf(t.getMessage())));
        }
    }
}
