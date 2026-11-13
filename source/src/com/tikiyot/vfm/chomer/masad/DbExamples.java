package com.tikiyot.vfm;

// מ0.29: הדוגמאות — קובץ יצוא שמצורף לתוכנה ומיובא למסד (המשתמש: "אולי תוכל לבנות להם קובץ יצוא ומשם לעשות יבוא, וכך
// לא תצטרך להוסיף דוגמאות בקוד"). עד כאן הן היו קובצי הטקסט של המנוע הישן, שהותקנו בכונן והועתקו למסד; המנוע הישן נמחק.
// הקובץ: assets/examples/dugmaot.txt — פורמט היצוא (DbExport), עם "%פנימי%" במקום שורש הזיכרון הפנימי.
// גם: שחזור הדוגמאות (באג 1) — מחליפים רק את רשימות הדוגמאות; מה שהמשתמש הוסיף לאופן — נשאר.
// אחראי: הקובץ מההתקנה, קובצי התרגול בכונן, הייבוא והשחזור. אסור לו: מסכים.

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

final class DbExamples {

    /** אופן הדוגמאות. */
    static final String MODE = "דוגמאות";  // בכונן
    static final String ASSET = "examples/dugmaot.txt", ASSET_DEMO = "examples/demo.txt";
    /** תיקיית התרגול של דוגמאות הפקודות — בתוך תיקיית התוכנה; נבנית מחדש בכל שחזור. */
    static final String DEMO = "דוגמאות לפקודות";  // בכונן
    private static final String FILE_LINE = "=== ", OLD = " @ישן";  // תחביר
    /** בקובץ: מקום הזיכרון הפנימי; מוחלף בשורש האמיתי בייבוא. */
    static final String IN = "%פנימי%";  // תחביר
    /** רשימות של המשתמש בתוך אופן הדוגמאות — שחזור לא מוחק אותן (רק משלים מה שחסר). */
    static final String MINE = "הדוגמאות שלי";  // בכונן

    private DbExamples() { }

    /** קובץ מתוך קובץ ההתקנה, כטקסט. */
    static String asset(Context c, String name) throws Exception {
        InputStream in = c.getAssets().open(name);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] b = new byte[8192];
            for (int n; (n = in.read(b)) > 0; ) out.write(b, 0, n);
            return new String(out.toByteArray(), "UTF-8");
        } finally {
            in.close();
        }
    }

    /** מה שהדוגמאות מצביעות עליו בכונן: פקודות המערכת, הכלים, ותיקיית התרגול (נבנית מחדש). תקלה — ביומן, וממשיכים. */
    static void files(String demo) {
        try {
            Reserved.writeSystem();
            Reserved.writeTools();
            if (demo != null) demo(new File(Paths.data(), DEMO), demo);
        } catch (Throwable t) {
            Log.e("DbExamples.files", t);
        }
    }

    /** הייבוא עצמו: לבית של המסד (נוצר אם אין). text — קובץ הדוגמאות; internal — שורש הזיכרון הפנימי. */
    static DbMore.Imported install(Store s, String text, String internal) {
        long home = DbPaths.ensureHome(s);
        return DbMore.importText(s, home, text.replace(IN, internal));
    }

    /** הרשימות שבראש קובץ הדוגמאות (בבית), לפי הסדר. */
    static List<String> tops(String text) {
        List<String> out = new ArrayList<String>();
        for (String raw : text.split("\r\n|\r|\n")) {
            String line = raw.trim();
            if (line.startsWith("#")) break;
            if (line.startsWith(DbExport.SUB.trim() + " ")) out.add(Names.cleanName(line.substring(DbExport.SUB.trim().length()).trim()));
        }
        return out;
    }

    /**
     * שחזור הדוגמאות (באג 1; ההחלטה מ־5.11.2026): מחליפים רק את הרשימות ששייכות לדוגמאות — כל אחת יוצאת מהבית (עם מה שרק
     * שלה) ונכנסת מחדש מהקובץ; שאר מה שבאופן נשאר; קובץ שנוסף לרשימת דוגמאות יוצא ממנה, בלי לגעת בו בכונן.
     * "הדוגמאות שלי" ו"שמור לתוכנה" — של המשתמש: לא נמחקות, רק מה שחסר בהן חוזר. הגיבוי שלפני — אצל הקורא.
     * מחזיר כמה רשימות הוחלפו.
     */
    static int restore(Store s, String text, String internal) {
        final long home = DbPaths.ensureHome(s);
        int replaced = 0;
        for (String name : tops(text)) {
            if (name.equals(MINE) || name.equals(Reserved.ROOT)) continue;
            for (Store.Member m : s.members(home))
                if (m.item.type == Store.LIST && m.item.name.equals(name)) {
                    Lists.removeFrom(s, home, m.item.id);
                    replaced++;
                    break;
                }
        }
        install(s, text, internal);
        return replaced;
    }

    /** תיקיית התרגול: כל קובץ מתחיל ב"=== <מסלול>" (יחסי לתיקייה); "@ישן" בסוף — תאריך השינוי לפני שנתיים. */
    static void demo(File dir, String spec) throws java.io.IOException {
        if (dir.exists()) Trash.wipe(dir);
        File cur = null;
        boolean old = false;
        StringBuilder body = new StringBuilder();
        for (String line : (spec + "\n" + FILE_LINE).replace("\r", "").split("\n")) {
            if (!line.startsWith(FILE_LINE)) {
                if (cur != null) body.append(line).append('\n');
                continue;
            }
            if (cur != null) {
                cur.getParentFile().mkdirs();
                Files.writeAtomic(cur, body.toString());
                if (old) cur.setLastModified(System.currentTimeMillis() - 730L * 24 * 3600 * 1000);
            }
            String p = line.substring(FILE_LINE.length()).trim();
            old = p.endsWith(OLD.trim());
            if (old) p = p.substring(0, p.length() - OLD.trim().length()).trim();
            cur = p.isEmpty() ? null : new File(dir, p);
            body.setLength(0);
        }
        Library lib = Library.peek();
        if (lib != null) lib.dirChanged(dir.getParent());
    }
}
