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

    static final String ASSET = "tests/pkudot-masad.vbat", ASSET_CALL = "tests/pkudot-kalul.vbat";

    private DbTestMode() { }

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
                    DbFiles.add(s, h[0], Collections.singletonList(f.getAbsolutePath()), null, true);
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
