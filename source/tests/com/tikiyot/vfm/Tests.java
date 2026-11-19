package com.tikiyot.vfm;

// בדיקות במקום אחד (80): רצות בכל בנייה על המחשב, ובנייה שנכשלת בבדיקה לא יוצאת.
// כל בדיקה נושאת את מספר הסעיף שהיא שומרת עליו, כשיש כזה.

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class Tests {

    private static int passed, failed;
    private static final List<String> failures = new ArrayList<String>();
    private static File home;

    private static void check(String what, boolean ok) {
        if (ok) passed++;
        else {
            failed++;
            failures.add(what);
        }
    }

    private static void eq(String what, Object expected, Object actual) {
        boolean ok = expected == null ? actual == null : expected.equals(actual);
        check(what + (ok ? "" : "  — צפוי: " + expected + " · התקבל: " + actual), ok);
    }

    public static void main(String[] args) throws Exception {
        // מבנה 76: הבדיקות קוראות לפעולות הכונן ולמנוע ישירות, אלפי פעמים — כאן הן עצמן "שכבת הניהול", וההיתר פתוח לכל אורכן.
        // הסירוב עצמו (בלי היתר) נבדק בבדיקה משלו, שסוגרת את ההיתר לרגע.
        Permit.open();
        home = new File(System.getProperty("vfm.home"));
        run("טקסט", new Runnable() { public void run() { text(); } });
        run("תנאים", new Runnable() { public void run() { cond(); } });
        run("אינדקס", new Runnable() { public void run() { index(); } });
        run("חיפוש", new Runnable() { public void run() { search(); } });
        run("מודל הפריט", new Runnable() { public void run() { item(); } });
        run("תור האינדקס", new Runnable() { public void run() { queue(); } });
        run("מרשם הפעולות", new Runnable() { public void run() { actions(); } });
        run("פעולות פיזיות", new Runnable() { public void run() { physical(); } });
        run("מיון אחד", new Runnable() { public void run() { sorting(); } });
        run("תיקיות והעתקה (82)", new Runnable() { public void run() { folders(); } });
        run("בין כוננים (82)", new Runnable() { public void run() { acrossDrives(); } });
        run("שמירה על הנתונים (90, 92, 93)", new Runnable() { public void run() { dataSafety(); } });
        run("קטנים (102, 104)", new Runnable() { public void run() { smalls(); } });
        run("בורר התיקיות מהאינדקס (106)", new Runnable() { public void run() { folderPicker(); } });
        run("שינוי שם בכונן ותיקייה חדשה (84)", new Runnable() { public void run() { renameOnDrive(); } });
        run("שליחת תיקיות — אריזה ופריסה של ZIP (116)", new Runnable() { public void run() { zipRoundTrip(); } });
        run("שליחה למכשיר — רק תוכנות העברה (117)", new Runnable() { public void run() { transferApps(); } });
        run("קובצי פקודות בסגנון bat — המנוע (117א)", new Runnable() { public void run() { batch(); } });
        run("מ0.27: RD /S ואחריו MD ו־COMP — ההדמיה שוכחת מה שהיה בתיקייה", new Runnable() { public void run() { m027(); } });
        run("מ0.30: RD ואחריו MD לאותה תיקייה; ECHO עם משתנה; רשימה שאינה בבית", new Runnable() { public void run() { m030(); } });
        run("מ0.32: RD לתת־תיקייה ואז לתיקייה", new Runnable() { public void run() { m032(); } });
        run("מ0.38: הפקודות והמתגים בעברית", new Runnable() { public void run() { m038(); } });
        run("מ0.38: המעבדה", new Runnable() { public void run() { m038lab(); } });
        run("מ0.40: התוספים, והשורות שהעורך האינטראקטיבי כותב", new Runnable() { public void run() { m040(); } });
        run("מ0.40: העבר — כבר במקום, /תפוס:לשנות-שם, /מכיל:", new Runnable() { public void run() { m040move(); } });
        run("מ0.40: החלק החדש של הבדיקה הכללית — בהרצה אמיתית", new Runnable() { public void run() { m040general(); } });
        run("מ0.42: פקודות לבדיקות — ודא מסד, צור אופן, ודא … באינדקס", new Runnable() { public void run() { m042cmds(); } });
        run("מ0.42: תוסף האבחון בא עם התוכנה, עם קובצי הבדיקה של הגרסה", new Runnable() { public void run() { m042plugin(); } });
        run("מ0.42: רשימה ששינתה שם — קובצי הפקודות שפונים אליה מתעדכנים (ב8)", new Runnable() { public void run() { m042renamed(); } });
        run("מ0.44: קטע — שורה בדוח במקום שלה", new Runnable() { public void run() { m044section(); } });
        run("מ0.45: הסל — שחזר מהסל, רוקן סל, ודא בסל", new Runnable() { public void run() { m045trash(); } });
        run("מ0.45: ודא תקין · מדידה /עד: · השמות בשתי מילים", new Runnable() { public void run() { m045checks(); } });
        run("מבנה 27: המנהל מאשר — ההחלטה על תכנית, בלי מסך", new Runnable() { public void run() { mivne27manager(); } });
        run("מ0.45: כל הפקודות עם כל המתגים — נבנה מההגדרה", new Runnable() { public void run() { m045matrix(); } });
        run("מ0.41: בטל לא-משויכים, קובץ של פקודת פתיחה אחת, שמות המסכים", new Runnable() { public void run() { m041(); } });
        run("מ0.41: העורך האינטראקטיבי בא עם התוכנה", new Runnable() { public void run() { m041bundled(); } });
        run("מבנה 244: תוסף העיצוב — הלוח", new Runnable() { public void run() { m244panel(); } });
        run("מבנה 248: תוויות הסוג", new Runnable() { public void run() {
            // מבנה 248 (המשתמש, 17.11.2026: "תיקייה מתעדכנת בוטל"): כל תווית במילון — של סוג שיש בתוכנה (אין תווית בלי סוג)
            List<String> dead = new ArrayList<String>();
            String[] icons = {Ui.FOLDER, Ui.LINK, Ui.CMD, Ui.SMART, Ui.MANUAL, Drive.icon(true), Drive.icon(false)};
            try {
                for (java.lang.reflect.Field f : Words.Tags.class.getFields()) {
                    String v = (String) f.get(null);
                    boolean used = false;
                    for (String i : icons) if (v.equals(Ui.tag(i))) used = true;
                    if (!used) dead.add(f.getName());
                }
            } catch (Exception e) { dead.add(String.valueOf(e)); }
            check("מבנה 248: כל תווית של סוג — של סוג שיש בתוכנה (\"תיקייה מתעדכנת\" — ירדה) " + dead, dead.isEmpty() && Words.Tags.class.getFields().length == 7);
        } });
        run("מ0.41: קובץ הדוגמה של רשימת \"בחירה\"", new Runnable() { public void run() { m041example(); } });
        run("מ0.41: רשימת \"בחירה\" — %נבחרים%, כמה קבצים הפקודה מקבלת, והתכנון שקובע", new Runnable() { public void run() { m041select(); } });
        run("מ0.37: הפקודות הקטנות — כתוב, ודא (תיקייה, מכיל, קבצים, שגיאה), דוח, המתן, משתנים, רעיון וכותרת", new Runnable() { public void run() { m037(); } });
        run("קובצי פקודות — השורה כשדות לטופס (117א, שלב 2)", new Runnable() { public void run() { batchForm(); } });
        run("תיקוני הביקורות השישית והשביעית (118–122, 134, 135, 139)", new Runnable() { public void run() { review7(); } });
        run("תיקוני באגים 123, 124, 137", new Runnable() { public void run() { review7b(); } });
        run("העברה והעתקה כמו באנדרואיד — הלוח (142), ושם אופן (129)", new Runnable() { public void run() { clip(); } });
        run("עדכון האינדקס (107–110, 113) ומוסתרות בבורר (130)", new Runnable() { public void run() { indexUpdate(); } });
        run("סינון תאריך וגודל במקום אחד (128), ושמות בחלון האישור (141)", new Runnable() { public void run() { dateSize(); } });
        run("הודעות במילון (131)", new Runnable() { public void run() { dictionary(); } });
        run("רשימה חכמה — תאריך וכונן דרך החיפוש (144)", new Runnable() { public void run() { smartDate(); } });
        run("סוגי קבצים — קבוצות, קובצי נתונים, שאר הקבצים (145)", new Runnable() { public void run() { kinds(); } });
        run("מסך יציב — לאן עבר הקובץ, ולמה אינו עונה (146)", new Runnable() { public void run() { stable(); } });
        run("רשימה חכמה בתיקיות (148)", new Runnable() { public void run() { folderView(); } });
        run("החיפוש האחד — הגדרות ומילים, שמירה (153)", new Runnable() { public void run() { oneSearch(); } });
        run("הדבקה ברשימה החכמה, שיתוף ההגדרות (149, 158)", new Runnable() { public void run() { pasteShare(); } });
        run("169, ביצועים, צמצום שלב 1 (170)", new Runnable() { public void run() { step170(); } });
        run("תיקיות ריקות (175)", new Runnable() { public void run() { empty175(); } });
        run("סריקה במקביל (177)", new Runnable() { public void run() { parallel177(); } });
        run("בחינת האינדקס (184)", new Runnable() { public void run() { index184(); } });
        run("סדר הפעולות של האינדקס (182)", new Runnable() { public void run() { order182(); } });
        run("\"לא\" בתנאים וכפתורי תצוגה מהירה (181)", new Runnable() { public void run() { quick181(); } });
        run("\"נוסף\" — מתי הקובץ הגיע (189)", new Runnable() { public void run() { added189(); } });
        run("חתימות תוכן ואיתור החסרים (190)", new Runnable() { public void run() { sig190(); } });
        run("תיקונים קטנים (192)", new Runnable() { public void run() { small192(); } });
        run("גרסה 1.9.0 — תיקייה חיה, מנהל ישיר, כפתורים, START %בית%", new Runnable() { public void run() { v190(); } });
        run("גרסה 1.10.0 — בכאילו, שליחת תיקייה, העברת כלי, סל למכלים, דוגמאות", new Runnable() { public void run() { v1100(); } });
        run("גרסה 1.12.0 — עיצובים עם שם: קובץ, ירושה, צלמית לרשימה", new Runnable() { public void run() { v1120(); } });
        run("גרסה 1.18.0 — הגנות בהעתקה: שם זמני, רק העליון, גודל ל-FAT32", new Runnable() { public void run() { v1180(); } });
        run("גרסה 1.19.0 — שמירת מסלול, יומן, \"נוסף\" מדויק בהעברה, %תאריך%", new Runnable() { public void run() { v1190(); } });
        run("גרסה 1.20.0 — השוואה לפי מסלול (COMP), %רק-במקור% / %רק-ביעד%, /עד:, גיבוי מלא", new Runnable() { public void run() { v1200(); } });
        run("גרסה 1.21.0 — מצב בתאריך: תנאי חיפוש ושחזור (COPY /מצב:)", new Runnable() { public void run() { v1210(); } });
        run("גרסה 1.22.0 — COMP /רשימה: — רשימה (עם חכמות, תיקיות מתעדכנות ותתי־רשימות) כמקור, מכמה כוננים", new Runnable() { public void run() { v1220(); } });
        run("גרסה 1.22.5 — ההגדרה האחת של שפת הפקודות (Commands)", new Runnable() { public void run() { v1225(); } });
        run("גרסה 1.23.0 — הפקודות הנפוצות", new Runnable() { public void run() { v1230(); } });
        run("מ0.1 — מונה המזהים, שתי הבניות, שכבת האחסון (MemStore, SqlStore)", new Runnable() { public void run() { storeM01(); } });
        run("מבנה 3: שומרי המבנה — כל קובץ באחת מחמש התיקיות; פניות כלפי מעלה רק מהרשימה", new Runnable() { public void run() { mivne(); } });
        run("מ0.3 — הערכת רשימה חכמה מעל האינדקס (Smarts)", new Runnable() { public void run() { smartsM03(); } });

        run("מבנה 71 — פקודות הרשימות במסד אמיתי, במחשב (מה שעד כאן נבדק רק במכשיר)", new Runnable() { public void run() { dbCmd(); } });

        System.out.println("בדיקות: " + passed + " עברו · " + failed + " נכשלו");
        for (String f : failures) System.out.println("  ✗ " + f);
        System.exit(failed == 0 ? 0 : 1);
    }

    private static void collect76(File dir, List<File> out) {
        File[] k = dir.listFiles();
        if (k == null) return;
        for (File f : k) {
            if (f.isDirectory()) collect76(f, out);
            else if (f.getName().endsWith(".java")) out.add(f);
        }
    }

    /** מבנה 71: פקודות הרשימות במסד אמיתי — בקובץ נפרד (DbCmdTests); כאן רק מצרפים את התוצאה. */
    private static void dbCmd() {
        String src = System.getProperty("vfm.src");
        if (src == null) return;
        DbCmdTests.Out o = DbCmdTests.run(new File(src));
        passed += o.passed;
        for (String f : o.failed) check(f, false);
    }

    /** מבנה 3: שומרי המבנה — בקובץ נפרד (MivneTests); כאן רק מצרפים את התוצאה. */
    private static void mivne() {
        String src = System.getProperty("vfm.src");
        if (src == null) return;
        MivneTests.Out o = MivneTests.run(new File(src));
        passed += o.passed;
        for (String f : o.failed) check("מבנה: " + f, false);
    }

    /** מ0.1: הבדיקות בקובץ נפרד (StoreTests); כאן רק מצרפים את התוצאה. */
    private static void storeM01() {
        String src = System.getProperty("vfm.src");
        StoreTests.Out o = StoreTests.run(home, src == null ? null : new File(src));
        passed += o.passed;
        for (String f : o.failed) check("מ0.1: " + f, false);
    }

    /** מ0.3: Smarts.over — ספירה מלאה, שורות מוגבלות וממוינות, הערה לתנאי שלא הובן, אינדקס שלא מוכן. */
    private static void smartsM03() {
        File root = drive("פנימי-חכמות");
        Index ix = built("פנימי-חכמות", root);
        Library lib = Library.forTests(Collections.singletonList(ix));
        Smarts.Finder f = Smarts.over(lib);
        Smarts.Result all = f.find("pdf", "", 100);
        check("חכמה: אינדקס מוכן, נמצאו קבצים", all.ready && all.count >= 4 && all.rows.size() == all.count);
        Smarts.Result few = f.find("pdf", Sort.NAME, 2);
        check("חכמה: הספירה מלאה גם כשהשורות מוגבלות", few.count == all.count && few.rows.size() == 2);
        check("חכמה: מיון לפי שם", few.rows.get(0).name.compareToIgnoreCase(few.rows.get(1).name) <= 0);
        Smarts.Result bySize = f.find("pdf", Sort.SIZE, 100);
        boolean desc = true;
        for (int i = 1; i < bySize.rows.size(); i++) desc &= bySize.rows.get(i - 1).size >= bySize.rows.get(i).size;
        check("חכמה: מיון לפי גודל — הגדול קודם", desc && bySize.rows.get(0).size == 2L * 1024 * 1024);
        check("חכמה: לשורה יש תיקייה ומסלול", all.rows.get(0).path.endsWith(all.rows.get(0).name) && !all.rows.get(0).folder.isEmpty());
        check("חכמה: סכום הגדלים", all.size >= 2L * 1024 * 1024);
        Smarts.Result none = f.find("אין-כזה-בכלל", "", 10);
        check("חכמה: בלי תוצאות — מוכן, אפס", none.ready && none.count == 0 && none.rows.isEmpty() && none.note.isEmpty());
        Smarts.Result bad = f.find("ספר גודל:2MB", "", 10);
        check("חכמה: תנאי שלא הובן — הערה", bad.ready && !bad.note.isEmpty());
        check("חכמה: תנאי ריק — לא מוכן", !f.find("  ", "", 10).ready);
        check("חכמה: בלי אינדקס — לא מוכן", !Smarts.over(null).find("pdf", "", 10).ready);
        Library cold = Library.forTests(Collections.singletonList(ix));
        cold.ready = false;
        check("חכמה: אינדקס שעוד נבנה — לא מוכן", !Smarts.over(cold).find("pdf", "", 10).ready);
    }

    private static void run(String name, Runnable r) {
        try {
            r.run();
        } catch (Throwable t) {
            failed++;
            failures.add(name + ": חריגה — " + t);
            t.printStackTrace();
        }
    }

    // ---------- עזר ----------

    private static File mk(File dir, String name, int bytes) {
        try {
            dir.mkdirs();
            File f = new File(dir, name);
            java.io.FileOutputStream o = new java.io.FileOutputStream(f);
            o.write(new byte[bytes]);
            o.close();
            return f;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void wipe(File f) {
        File[] kids = f.listFiles();
        if (kids != null) for (File k : kids) wipe(k);
        f.delete();
    }

    /** כונן לדוגמה: קבצים רגילים, מוסתרים, סל, ותיקייה שהמערכת חוסמת. */
    private static File drive(String name) {
        File root = new File(home, name);
        wipe(root);
        mk(new File(root, "Download"), "יוגה למתחילים.pdf", 100);
        mk(new File(root, "Download"), "מוגה.pdf", 200);
        mk(new File(root, "Download"), "ספר מוגה.epub", 300);
        mk(new File(root, "Download/פאולה"), "הספר.pdf", 2 * 1024 * 1024);
        mk(new File(root, "Download2"), "אחר.pdf", 10);
        mk(new File(root, "Documents"), "היגיון.txt", 10);
        mk(new File(root, "Documents"), ".סוד.txt", 10);
        mk(new File(root, ".מטמון/פנים"), "תמונה.jpg", 10);
        mk(new File(root, Index.TRASH), "נמחק.pdf", 10);
        mk(new File(root, "Android/data/app"), "חסום.txt", 10);
        return root;
    }

    private static Index built(String key, File root) {
        Index ix = new Index(key, key, root.getAbsolutePath());
        ix.scan(null);
        return ix;
    }

    private static Index.Rec rec(Index ix, String name) {
        for (Index.Rec r : ix.files) if (r.name.equals(name)) return r;
        return null;
    }

    private static int count(Index ix, String cond) {
        Cond c = Cond.parse(cond);
        int n = 0;
        for (Index.Rec r : ix.files) if (!r.gone && !Library.hide(ix, r, c) && c.matches(ix, r)) n++;
        return n;
    }

    private static List<String> names(Library.Result res) {
        List<String> out = new ArrayList<String>();
        for (Library.Hit h : res.hits) out.add(h.name());
        return out;
    }

    // ---------- טקסט ----------

    private static void text() {
        eq("נרמול: אותיות סופיות", "שלומ", Text.norm("שלום"));
        eq("נרמול: ניקוד מוסר", "שלומ", Text.norm("שָׁלוֹם"));
        eq("נרמול: אותיות קטנות", "abc", Text.norm("AbC"));
        eq("כתיב חסר: היגיון = הגיון", Text.loose(Text.norm("הגיון")), Text.loose(Text.norm("היגיון")));
        eq("אות שימוש מוסרת", "וגה", Text.withoutPrefixLetter("מוגה"));
        eq("מילה קצרה לא מורחבת", null, Text.withoutPrefixLetter("מו"));
        check("77: 'וגה' בתחילת מילה לא נמצא בתוך 'יוגה'", !Text.wordStart("וגה").matches("יוגה למתחילים.pdf"));
        check("77: 'וגה' נמצא בתחילת מילה", Text.wordStart("וגה").matches("ספר וגה.pdf"));
        check("תו כללי *", Text.compile(Text.widen("מה*אומר")).matches("מה הוא אומר"));
        check("תו כללי ?", Text.compile(Text.widen("ס?ר")).matches("ספר"));
        check("הכלה רגילה", Text.compile("ספר").matches("הספר.pdf"));
        // מבנה 279: הכוכבית בחיפוש — כמו בפקודות (מעוגנת); בלי כוכבית — הכלה; ומימוש אחד לשניהם
        check("מבנה 279: בחיפוש — \"דוח*\" מתחיל ב, \"*.pdf\" נגמר ב, \"מה*אומר\" מתחיל ונגמר; \"פרק?\" ו\"ספר\" — הכלה",
                Text.compile(Text.widen("דוח*")).matches("דוח מאי.pdf") && !Text.compile(Text.widen("דוח*")).matches("סיכום דוח.pdf")
                        && Text.compile(Text.widen("*.pdf")).matches("ספר.pdf") && !Text.compile(Text.widen("*.pdf")).matches("ספר.pdf.txt")
                        && Text.compile(Text.widen("מה*אומר")).matches("מה הוא אומר") && !Text.compile(Text.widen("מה*אומר")).matches("מה הוא אומר.txt")
                        && Text.compile(Text.widen("פרק?")).matches("ספר פרק1.pdf") && Text.compile(Text.widen("ספר")).matches("הספר.pdf"));
        check("מבנה 279: בפקודות — אותה התאמה (Text.wild): השם כולו, בלי הבדל בין גדולות לקטנות, \"*.*\" — הכול, בלי תו כללי — השם בדיוק",
                Text.wild("*.pdf", "Book.PDF") && !Text.wild("*.pdf", "book.pdf.txt") && Text.wild("*.*", "README") && Text.wild("*", "a")
                        && Text.wild("פרק?", "פרק1") && !Text.wild("פרק?", "פרק12") && Text.wild("דוח*", "דוח 1.txt") && !Text.wild("דוח*", "סיכום דוח")
                        && Text.wild("a.txt", "A.TXT") && !Text.wild("a.txt", "aa.txt") && Text.wild("a*b*c", "aXbYc") && !Text.wild("a*b*c", "aXbYcd"));
        // מבנה 281: כל המילים מסומנות בשם; מסננים ו"בלי" — לא; חלופות ותווים כלליים — החלקים
        check("מבנה 281: סימון — כל המילים (\"דוח 2026\"), חלופות (\"ספר|דוח\"), החלקים שבין התווים הכלליים (\"דוח*2026\"); "
                        + "מסנן ו\"בלי\" — לא; בלי התאמה — כלום",
                java.util.Arrays.equals(Text.marks("דוח מאי 2026.pdf", "דוח 2026"), new int[]{0, 3, 8, 12})
                        && java.util.Arrays.equals(Text.marks("דוח מאי 2026.pdf", "ספר|דוח"), new int[]{0, 3})
                        && java.util.Arrays.equals(Text.marks("דוח מאי 2026.pdf", "דוח*2026"), new int[]{0, 3, 8, 12})
                        && java.util.Arrays.equals(Text.marks("דוח מאי 2026.pdf", "סוג:pdf -מאי דוח"), new int[]{0, 3})
                        && java.util.Arrays.equals(Text.marks("ספר שלם.pdf", "שלם ספר"), new int[]{0, 3, 4, 7})
                        && Text.marks("דוח.pdf", "תמונה").length == 0 && Text.marks("דוח.pdf", null).length == 0);
        // מבנה 283: מקום החיפוש — התנאי נבנה במקום אחד (Cond), המסכים אומרים מה המקום שלהם, והפתיחה מעבירה אותו
        String src283 = System.getProperty("vfm.src") + "/src/com/tikiyot/vfm/";
        String ss283 = read(new File(src283 + "masachim/SearchScreen.java")), vu283 = read(new File(src283 + "masachim/rechivim/VerbsUi.java")),
                dl283 = read(new File(src283 + "masachim/DbListScreen.java")), dk283 = read(new File(src283 + "masachim/DiskScreen.java"));
        check("מבנה 283: מקום החיפוש — \"רשימה:\" (האופן קודם; בבית — רק האופן) ו\"מקור:\" (תיקייה, ערך עם רווח בתוך «»); "
                        + "תיקיות ריקות — בלי לפרק את כל התנאי; הסימון מדלג על הערך שבתוך «» (" + Cond.inList("עבודה", java.util.Arrays.asList("א ב", "ג")) + ")",
                Cond.inList("עבודה", java.util.Arrays.asList("א ב", "ג")).equals("רשימה:«עבודה\\א ב\\ג»")
                        && Cond.inList("עבודה", new java.util.ArrayList<String>()).equals("רשימה:עבודה")
                        && Cond.inFolder("/storage/emulated/0/ספרים חדשים").equals("מקור:«/storage/emulated/0/ספרים חדשים»")
                        && Cond.emptyDirsIn("סוג:pdf תיקיות:ריקות") && !Cond.emptyDirsIn("-תיקיות:ריקות") && !Cond.emptyDirsIn("תיקיות")
                        && java.util.Arrays.equals(Text.marks("חשבון 282 ג.txt", "חשבון רשימה:«בדיקת 282\\ג»"), new int[]{0, 5})
                        && java.util.Arrays.equals(Text.marks("חשבון 282.txt", "רשימה:«א ב» 282"), new int[]{6, 9}));
        check("מבנה 283: \"פתח חיפוש\" — מהמסך שעליו עומדים, עם המקום שלו (רשימה, בית, תיקייה); רשימה חכמה — בלי מקום; "
                        + "המקום נכנס לתנאי רק כשמחפשים משהו, ולא כש\"בכל המכשיר\"; השבב — מתחת לשורה",
                vu283.contains("SearchScreen.open(a);") && !vu283.contains("new Intent(a, SearchScreen.class)); }")
                        && dl283.contains("PlaceChip.Place {") && dl283.contains("Cond.inList(mode, path)")
                        && dk283.contains("PlaceChip.Place {") && dk283.contains("Cond.inFolder(d.getAbsolutePath())")
                        && ss283.contains("place = fixed ? null : getIntent().getStringExtra(EXTRA_PLACE);")
                        && ss283.contains("q.trim().isEmpty() || place == null || wide ? q : SmartLists.join(place, q)")
                        && ss283.contains("new PlaceChip(this,") && ss283.contains("saveSetting(SmartLists.WHOLE,")
                        && !ss283.contains("Cond.parse(") && Words.Find.placeChip("x").contains("x"));
        // מבנה 284: היסטוריית החיפוש — הכלל (Search.remember), השמירה בשורה אחת, והמסך
        java.util.List<String> h284 = Search.remember(java.util.Arrays.asList("דוח מאי", "ספר", "חשבון"), "  ספר  ", 20);
        java.util.List<String> c284 = Search.remember(java.util.Arrays.asList("א", "ב", "ג", "ד"), "ה", 3);
        java.util.List<String> f284 = Search.remember(java.util.Arrays.asList("ספרים"), "ספרימ", 20);
        String hs284 = read(new File(System.getProperty("vfm.src"), "src/com/tikiyot/vfm/masachim/SearchScreen.java"));
        check("מבנה 284: היסטוריה — החדש ראשון; מה שכבר היה עולה למעלה (גם ברווחים ובאותיות סופיות), לא פעמיים; עד הכמות; 0 — בלי; "
                        + "ריק — רק חיתוך; \"‖\" אינו נשמר בתוך חיפוש; נשמרת בשורה אחת ונקראת חזרה " + h284 + c284 + f284,
                h284.equals(java.util.Arrays.asList("ספר", "דוח מאי", "חשבון")) && c284.equals(java.util.Arrays.asList("ה", "א", "ב"))
                        && f284.equals(java.util.Arrays.asList("ספרימ")) && Search.remember(java.util.Arrays.asList("א"), "ב", 0).isEmpty()
                        && Search.remember(java.util.Arrays.asList("א", "ב", "ג"), null, 2).equals(java.util.Arrays.asList("א", "ב"))
                        && Search.remember(java.util.Arrays.asList("א"), "  ", 5).equals(java.util.Arrays.asList("א"))
                        && Search.remember(new java.util.ArrayList<String>(), "x‖y", 5).equals(java.util.Arrays.asList("x y"))
                        && Search.historyOf(Search.historyText(java.util.Arrays.asList("דוח *.pdf", "ספר|דוח"))).equals(java.util.Arrays.asList("דוח *.pdf", "ספר|דוח"))
                        && Search.historyOf("").isEmpty() && Search.historyOf(null).isEmpty());
        check("מבנה 284: המסך — היסטוריה רק בחיפוש (לא ברשימה חכמה), מתחת לשורה ריקה; נכנס אליה מה שבשורה כשפותחים תוצאה, ב\"חפש\" "
                        + "שבמקלדת וביציאה; הכמות — בחלונית ההגדרות",
                hs284.contains("if (!fixed) {") && hs284.contains("new HistoryBox(this, Search.historyOf(setting(SmartLists.HISTORY, \"\"))")
                        && hs284.contains("history.show(s.toString().trim().isEmpty());") && hs284.contains("history.remember(input.getText().toString());")
                        && hs284.indexOf("remember();", hs284.indexOf("private void tap(")) > 0 && hs284.indexOf("remember();", hs284.indexOf("protected void onPause()")) > 0
                        && hs284.contains("IME_ACTION_SEARCH") && hs284.contains("history.countRow(") && SmartLists.HISTORY_DEFAULT == 20);
        String sf281 = read(new File(System.getProperty("vfm.src"), "src/com/tikiyot/vfm/masachim/rechivim/SmartForm.java"));
        // מבנה 286: שבבי המסננים — ההסרה של מסנן אחד משאירה את השאר; המסך בונה אותם מהחלונית ומרענן בכל חיפוש
        String fc286 = read(new File(System.getProperty("vfm.src"), "src/com/tikiyot/vfm/masachim/rechivim/FilterChips.java")),
                ss286 = read(new File(System.getProperty("vfm.src"), "src/com/tikiyot/vfm/masachim/SearchScreen.java"));
        check("מבנה 286: שבב ✕ מסיר מסנן אחד — השאר נשאר (גם ערך עם רווח, גם מסנן של כמה רכיבים, גם רכיב שמופיע פעמיים — פעם אחת)",
                SmartForm.without("מקור:«/s/ספרים חדשים» סוג:pdf גודל:>10MB", "סוג:pdf").equals("מקור:«/s/ספרים חדשים» גודל:>10MB")
                        && SmartForm.without("מקור:«/s/ספרים חדשים» סוג:pdf", "מקור:«/s/ספרים חדשים»").equals("סוג:pdf")
                        && SmartForm.without("א ב ג", "א ג").equals("ב") && SmartForm.without("א א", "א").equals("א")
                        && SmartForm.without("סוג:pdf", "סוג:pdf").isEmpty() && SmartForm.without("", "x").isEmpty());
        check("מבנה 286: השבבים — מהחלונית (Parts.active), מתחת לשורה, נבנים מחדש רק כשהמסננים השתנו, ונגיעה מסירה (Parts.drop); "
                        + "\"רק תיקיות ריקות\" — בלי שבב של סוג",
                fc286.contains("parts.active()") && fc286.contains("parts.drop(a, f[1])") && fc286.contains("if (sig.toString().equals(shown)) return;")
                        && ss286.contains("chips = new FilterChips(this, parts);") && ss286.contains("if (chips != null) chips.refresh();")
                        && sf281.contains("if (!k.isEmpty() && !empty.isChecked()) {") && Words.Find.chip("x").contains("✕"));
        String ss287 = read(new File(System.getProperty("vfm.src"), "src/com/tikiyot/vfm/masachim/SearchScreen.java")),
                dl287 = read(new File(System.getProperty("vfm.src"), "src/com/tikiyot/vfm/masachim/DbListScreen.java"));
        check("מבנה 287: רשימה חכמה של המסד נפתחת עם האופן והמספר שלה, ובתפריט — \"💾 שמירה\" דרך המנהל (אותה פקודה של הטופס)",
                dl287.contains("i.putExtra(SearchScreen.EXTRA_SMART_ID, id);") && dl287.contains("openSmart(this, mode, it.id, it.name, cond, sort, view);")
                        && ss287.contains("if (smartId != 0) menu.add(0, 9, 5, Words.Find.saveHere(")
                        && ss287.contains("Manager.onSmartEdit(smartMode, smartId,") && ss287.contains("Lists.smartProps(s, smartId)"));
        int ed288 = dl287.indexOf("private void edit(final Store.Item it) {"), ee288 = dl287.indexOf("private void done(String msg) {", ed288);
        String edit288 = ed288 > 0 && ee288 > ed288 ? dl287.substring(ed288, ee288) : "";
        check("מבנה 288: עריכה של רשימה חכמה — אותו חלון של רשימה ידנית (שם, צלמית, תיאור, \"למה\"); התנאי, המיון והתצוגה נשמרים כמו שהם; "
                        + "הטופס הנפרד כבר לא נפתח מכאן",
                !edit288.isEmpty() && !edit288.contains("DbSmartForm.show(") && edit288.contains("list = it.type == Store.LIST || smart;")
                        && edit288.contains("r.cond = prop(it.id, Store.P_COND);") && edit288.contains("saveSmart(it, r);"));
        // מבנה 289: "📥 לטעון" — המוכנות, ואחריהן הרשימות החכמות של האופן (לפי השם; בלי תנאי — לא)
        MemStore ms289 = new MemStore(IdCounter.memory());
        long b289 = ms289.createSmart("ב חכמה", 0), a289 = ms289.createSmart("א חכמה", 0), e289 = ms289.createSmart("ריקה", 0);
        ms289.setProp(b289, Store.P_COND, "סוג:pdf");
        ms289.setProp(b289, Store.P_SORT, Sort.DATE);
        ms289.setProp(a289, Store.P_COND, "דוח");
        java.util.List<String[]> t289 = Lists.templates(ms289), p289 = Lists.templates(null);
        int np289 = SmartLists.PRESETS.length;
        String pt289 = read(new File(System.getProperty("vfm.src"), "src/com/tikiyot/vfm/masachim/rechivim/PanelTools.java"));
        check("מבנה 289: התבניות — " + np289 + " המוכנות קודם (מסומנות), ואחריהן הרשימות החכמות של האופן לפי השם, עם התנאי והמיון; בלי תנאי — לא; "
                        + "בלי מסד — רק המוכנות; בחלונית — לטעון, מיון, תצוגה, שמירה בשם (ושמירה ברשימה חכמה)",
                p289.size() == np289 && t289.size() == np289 + 2 && "1".equals(t289.get(0)[4]) && t289.get(0)[0].equals(SmartLists.PRESETS[0][0])
                        && t289.get(np289)[0].equals("א חכמה") && t289.get(np289)[1].equals("דוח") && t289.get(np289)[4].isEmpty()
                        && t289.get(np289 + 1)[0].equals("ב חכמה") && t289.get(np289 + 1)[2].equals(Sort.DATE)
                        && pt289.contains("Words.Find.LOAD") && pt289.contains("Words.Find.SORT_B") && pt289.contains("Words.Find.VIEW_B")
                        && pt289.contains("Words.Find.SAVE_AS_B") && pt289.contains("Lists.templates(s)")
                        && ss287.contains("PanelTools.row(this,") && ss287.contains("private void pickView() {") && ss287.contains("private void loadTemplate("));
        int rl285 = sf281.indexOf("public void reload(Activity a, String cond) {");
        check("מבנה 285: טעינה של הגדרה אחרת לטופס מנקה גם את \"רק תיקיות ריקות\" (עד כאן נשארה מסומנת מהקודמת)",
                rl285 > 0 && sf281.indexOf("empty.setChecked(false);", rl285) > rl285 && sf281.indexOf("empty.setChecked(false);", rl285) < sf281.indexOf("load(cond);", rl285));
        check("מבנה 281: שורת החיפוש — נגיעה בוחרת את הכתוב, ✕ מנקה (רק כשיש מה), והמסך משתמש בה; הנוסח למקריא — במילון",
                sf281.contains("input.setSelectAllOnFocus(true);") && sf281.contains("input.setText(\"\");") && sf281.contains("Words.Find.CLEAR")
                        && read(new File(System.getProperty("vfm.src"), "src/com/tikiyot/vfm/masachim/SearchScreen.java")).contains("SmartForm.fieldRow(this, input)")
                        && !Words.Find.CLEAR.isEmpty());
        String ss280 = read(new File(System.getProperty("vfm.src"), "src/com/tikiyot/vfm/masachim/SearchScreen.java")),
                fb280 = read(new File(System.getProperty("vfm.src"), "src/com/tikiyot/vfm/masachim/FolderBrowse.java"));
        check("מבנה 280: התצוגה בתיקיות ופס הלוח — בקובץ משלהם (FolderBrowse), ולא גם במסך החיפוש",
                fb280.contains("void showFolder() {") && fb280.contains("void stayInFolder() {") && fb280.contains("void buildClipBar(LinearLayout root) {")
                        && fb280.contains("void showClip() {") && !ss280.contains("void showFolder() {") && !ss280.contains("void buildClipBar(")
                        && !ss280.contains("void showClip() {") && ss280.contains("private final FolderBrowse fb = new FolderBrowse(this);"));
        check("מבנה 279: מימוש אחד לתווים כלליים — אין עוד Parse.wild, והפקודות קוראות ל־Text.wild",
                !read(new File(System.getProperty("vfm.src"), "src/com/tikiyot/vfm/pkudot/Parse.java")).contains("static boolean wild(")
                        && read(new File(System.getProperty("vfm.src"), "src/com/tikiyot/vfm/pkudot/FileCmds.java")).contains("Text.wild("));
        eq("דירוג: מתחיל בשאילתה", 0, Text.rank("ספר טוב", "ספר"));
        eq("דירוג: מילה מתחילה בה", 1, Text.rank("הנה ספר", "ספר"));
        eq("דירוג: בתוך מילה", 2, Text.rank("הספר", "ספר"));
    }

    // ---------- תנאים ----------

    private static void cond() {
        eq("«» שומר רווחים", Arrays.asList("תיקייה:/a/ספרים חדשים", "מילה"),
                Cond.tokens("תיקייה:«/a/ספרים חדשים» מילה"));
        eq("quote עוטף רק כשיש רווח", "«א ב»", Cond.quote("א ב"));
        eq("quote בלי רווח", "אב", Cond.quote("אב"));

        File root = drive("פנימי");
        Index ix = built("פנימי", root);
        String dl = new File(root, "Download").getAbsolutePath();

        eq("סוג:pdf", 4, count(ix, "סוג:pdf"));
        eq("סוג:pdf|epub", 5, count(ix, "סוג:pdf|epub"));
        eq("גודל:>1MB", 1, count(ix, "גודל:>1MB"));
        eq("גודל:<50", 2, count(ix, "גודל:<50"));
        eq("מקור:פנימי", 6, count(ix, "מקור:פנימי"));
        eq("מקור:כרטיס בכונן פנימי", 0, count(ix, "מקור:כרטיס"));
        eq("71: מקור תיקייה כולל תת-תיקיות", 4, count(ix, "מקור:" + Cond.quote(dl)));
        eq("71: מקור תיקייה לא כולל תיקייה אחות בשם דומה (Download2)", 4, count(ix, "מקור:" + dl));
        eq("71: \"או\" בין שתי תיקיות", 5,
                count(ix, "מקור:" + dl + "|" + new File(root, "Documents").getAbsolutePath()));
        eq("תנאי שגוי נאסף כלא מובן", 1, Cond.parse("גודל:abc").unknown.size());
        check("תיקייה:נמחקו מבקש את הסל", Cond.parse("תיקייה:נמחקו").trash);
        check("מקור:.נמחקו מבקש את הסל", Cond.parse("מקור:/x/" + Index.TRASH).trash);
    }

    // ---------- אינדקס ----------

    private static void index() {
        File root = drive("כרטיס1");
        Index ix = built("כרטיס1", root);
        check("קובץ רגיל באינדקס", rec(ix, "מוגה.pdf") != null);
        check("63: קובץ מוסתר באינדקס", rec(ix, ".סוד.txt") != null);
        check("63: קובץ מוסתר מסומן", ix.hidden(rec(ix, ".סוד.txt")));
        check("63: קובץ בתוך תיקייה מוסתרת מסומן", ix.hidden(rec(ix, "תמונה.jpg")));
        check("54: הסל באינדקס ומסומן", rec(ix, "נמחק.pdf") != null && ix.trash(rec(ix, "נמחק.pdf")));
        check("Android/data לא באינדקס", rec(ix, "חסום.txt") == null);
        eq("ספירה בלי מוסתרים ובלי הסל", 6, ix.fileCount());

        String p = new File(root, "Download/פאולה/הספר.pdf").getAbsolutePath();
        eq("מסלול מלא מהאינדקס", p, ix.path(rec(ix, "הספר.pdf")));
        eq("מזהה תיקייה לפי מסלול", "פאולה", ix.dirs.get(ix.dirId(new File(root, "Download/פאולה").getAbsolutePath())).name);
        eq("מסלול מחוץ לכונן", -2, ix.dirId("/אחר/לגמרי"));

        Index c = ix.copy();
        c.files.get(0).name = "שונה";
        check("תמונת מצב: העותק עמוק", !ix.files.get(0).name.equals("שונה"));

        File added = mk(new File(root, "חדשה/פנימית"), "נוסף.pdf", 5);
        check("הוספה נקודתית יוצרת תיקיות חסרות", ix.addFile(added) && rec(ix, "נוסף.pdf") != null);
        eq("הוספה נקודתית: מסלול נכון", added.getAbsolutePath(), ix.path(rec(ix, "נוסף.pdf")));

        try {
            ix.save();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Index back = Index.load("כרטיס1");
        check("שמירה וטעינה", back != null);
        if (back != null) {
            eq("שמירה וטעינה: מספר קבצים", ix.files.size(), back.files.size());
            eq("שמירה וטעינה: מספר תיקיות", ix.dirs.size(), back.dirs.size());
            check("שמירה וטעינה: סימון מוסתר נשמר", back.hidden(rec(back, "תמונה.jpg")));
            check("שמירה וטעינה: סימון סל נשמר", back.trash(rec(back, "נמחק.pdf")));
            eq("שמירה וטעינה: גודל", 2L * 1024 * 1024, rec(back, "הספר.pdf").size);
        }
    }

    // ---------- חיפוש ----------

    private static void search() {
        File root = drive("פנימי");
        Index ix = built("פנימי", root);
        Library lib = Library.forTests(Collections.singletonList(ix));

        List<String> n = names(lib.search("מוגה", false, true));
        check("77: 'מוגה' מוצא את 'מוגה.pdf'", n.contains("מוגה.pdf"));
        check("77: 'מוגה' לא מוצא את 'יוגה למתחילים.pdf'", !n.contains("יוגה למתחילים.pdf"));
        check("חיפוש מוצא בתוך שם", names(lib.search("ספר", false, true)).contains("ספר מוגה.epub"));
        // 80.5: כונן — צלמית ושם במקום אחד
        eq("80.5: פנימי", "📱 זיכרון פנימי", Drive.label(true, Drive.name(true, 0)));
        eq("80.5: כרטיס שני", "💾 כרטיס זיכרון 2", Drive.label(false, Drive.name(false, 2)));
        eq("80.5: לפי מסלול", Drive.INTERNAL_NAME, Drive.nameOf("/storage/emulated/0/Download"));
        eq("80.5: מקור קריא", "💾 כרטיס זיכרון (כולו)", Sources.label(Drive.CARD));
        eq("80: הבידוד לא נכנס לחיפוש", Text.norm("Download"), Text.norm(Text.ISO + "Download" + Text.PDI).replace("\u2068", "").replace("\u2069", ""));
        Sort ds = Sort.of(Sort.SPACE, false, null);
        eq("כפולים: ברירת המחדל — מקום לפינוי", Sort.SPACE, ds.by);
        check("כפולים: בתפריט — הבסיס המשותף ו'מקום לפינוי'", java.util.Arrays.asList(ds.options()).containsAll(
                java.util.Arrays.asList(Sort.NAME, Sort.DATE, Sort.SIZE, Sort.TYPE, Sort.FOLDER, Sort.SPACE)));
        java.util.Set<String> icons = new java.util.HashSet<String>(java.util.Arrays.asList(Drive.INTERNAL_ICON, Drive.CARD_ICON));
        for (Func f : Func.all()) check("צלמית אחת לדבר אחד — " + f.kind, icons.add(f.icon));
        // מבנה 14: שמות הכלים שהפקודות מכירות (Target.KINDS) — בדיוק הכלים שברשימת הכלים (Func), באותו סדר
        List<String> kinds = new ArrayList<String>();
        for (Func f : Func.all()) kinds.add(f.kind);
        eq("מבנה 14: שמות הכלים — אותה רשימה בפקודות ובכלים", Target.KINDS, kinds);
        // 85: שם דומה
        check("85: סוגריים", Text.similarNames("ספר.pdf", "ספר (1).pdf"));
        check("85: z-library", Text.similarNames("להיות מורה.epub", "להיות מורה (z-library.sk, 1lib.sk, z-lib.sk).epub"));
        check("85: עותק", Text.similarNames("ספר.pdf", "ספר - עותק.pdf"));
        check("85: סימנים", Text.similarNames("איך_להיות-מדען.pdf", "איך להיות  מדען.pdf"));
        check("85: סדר מילים", Text.similarNames("מדען איך להיות.pdf", "איך להיות מדען.pdf"));
        check("85: כתיב מלא וחסר", Text.similarNames("היגיון.txt", "הגיון.txt"));
        check("85: אנגלית גדולות וקטנות", Text.similarNames("Raw Food.epub", "raw food.epub"));
        check("85: מספור בסוף", Text.similarNames("ספר.pdf", "ספר 2.pdf"));
        check("85: סיומת שונה — לא", !Text.similarNames("ספר.pdf", "ספר.epub"));
        check("85: שמות שונים — לא", !Text.similarNames("עשה לך רב.pdf", "בענין לבחור לו רב.pdf"));
        // 78: בלי אות השימוש — קבוצה נפרדת, לא "מלאה"
        Library.Result pr = lib.search("מספר", false, true);
        Library.Hit ph = null;
        for (Library.Hit x : pr.hits) if (x.name().equals("ספר מוגה.epub")) ph = x;
        check("78: 'מספר' מוצא את 'ספר מוגה' — בקבוצה נפרדת", ph != null && ph.section == Library.NO_PREFIX);
        eq("78: נספר בנפרד", 0, pr.counts[Library.FULL] > 0 && ph != null && pr.counts[Library.NO_PREFIX] == 0 ? 1 : 0);
        check("78: נספר כ'בלי אות השימוש'", pr.counts[Library.NO_PREFIX] >= 1);
        eq("78: כותרת עם האות", "בלי אות השימוש (מ')", Library.header(Library.NO_PREFIX, pr));
        Library.Result fr = lib.search("ספר", false, true);
        check("78: 'ספר' — מלאה, לא בקבוצת האות", fr.counts[Library.NO_PREFIX] == 0 && fr.counts[Library.FULL] >= 1);
        List<Library.Hit> order = pr.hits;
        boolean afterFull = true;
        for (int i = 1; i < order.size(); i++) if (order.get(i).section < order.get(i - 1).section) afterFull = false;
        check("78: הקבוצות בסדר — מלאות קודם", afterFull);
        check("כתיב חסר: 'הגיון' מוצא 'היגיון.txt'", names(lib.search("הגיון", false, true)).contains("היגיון.txt"));
        check("63: מוסתר לא בתוצאות", !names(lib.search("סוד", false, true)).contains(".סוד.txt"));
        check("54: הסל לא בתוצאות", !names(lib.search("נמחק", false, true)).contains("נמחק.pdf"));
        check("55: הסל מוצג כשמבקשים", names(lib.search("תיקייה:נמחקו", false, true)).contains("נמחק.pdf"));
        check("סינון בלבד: סוג:epub", names(lib.search("סוג:epub", false, true)).contains("ספר מוגה.epub"));
        // מבנה 94: רשימה חכמה — רק מה שעונה על התנאי במלואו (המשתמש: "היא לא מסננת מה שאמורה לסנן")
        Library.Result two94 = lib.search("ספר מוגה", false, true), list94 = lib.list("ספר מוגה");
        List<String> l94 = names(list94);
        check("מבנה 94: החיפוש מוסיף גם \"רק חלק מהמילים\" — 'מוגה.pdf' אין בו 'ספר' (כך התגלה)",
                names(two94).contains("מוגה.pdf") && two94.counts[Library.PARTIAL] >= 1);
        check("מבנה 94: הרשימה — רק מה שיש בו את שתי המילים " + l94, l94.contains("ספר מוגה.epub") && !l94.contains("מוגה.pdf")
                && !l94.contains("הספר.pdf") && list94.total == list94.hits.size());
        check("מבנה 94: הרשימה — בלי \"רק בלי אות השימוש\" ('מספר' אינו 'ספר מוגה')", !names(lib.list("מספר")).contains("ספר מוגה.epub"));
        check("מבנה 94: הרשימה — בלי כתיב חסר ('הגיון' אינו 'היגיון'), ובלי הסל והמוסתרים",
                !names(lib.list("הגיון")).contains("היגיון.txt") && !names(lib.list("נמחק")).contains("נמחק.pdf") && !names(lib.list("סוד")).contains(".סוד.txt"));
        boolean same94 = true;
        for (String q94 : new String[]{"ספר מוגה", "מספר", "הגיון", "ספר", "מוגה", "סוג:epub", "סוג:pdf ספר", "ספר -מוגה", "תיקייה:נמחקו"}) {
            List<String> full = new ArrayList<String>();
            for (Library.Hit x : lib.search(q94, false, true).hits) if (x.section == Library.FULL) full.add(x.path());
            List<String> strict = new ArrayList<String>();
            for (Library.Hit x : lib.list(q94).hits) strict.add(x.path());
            if (!full.equals(strict)) same94 = false;
        }
        check("מבנה 94: בכל תנאי — הרשימה היא בדיוק ההתאמה המלאה של החיפוש (תשעה תנאים)", same94);
        // מבנה 96 — המקרה של המשתמש: "ביקשתי קובץ pdf עם תווים מסויימים בשם, והוא הביא קבצים עם סיומת אחרת"
        {
            File r96 = new File(home, "מבנה96");
            wipe(r96);
            for (String n96 : new String[]{"שיעור 12 כללים.pdf", "שיעור 12 כללים.docx", "שיעור 12 כללים.mp3", "כללים.txt", "אחר.pdf"})
                mk(new File(r96, "D"), n96, 10);
            Library lib96 = Library.forTests(Collections.singletonList(built("מבנה96", r96)));
            List<String> s96 = names(lib96.search("כללים pdf", false, true));
            check("מבנה 96: כך היה — \"כללים pdf\" בחיפוש מביא גם txt, docx ו־mp3 (מתאימים רק ל\"כללים\") " + s96,
                    s96.contains("כללים.txt") && s96.contains("שיעור 12 כללים.docx") && s96.contains("שיעור 12 כללים.mp3"));
            boolean only96 = true;
            for (String q96 : new String[]{"כללים pdf", "pdf כללים", "סוג:pdf כללים", "כללים.pdf", "*כללים*.pdf", "סוג:.pdf כללים", "סוג:*.pdf כללים", "סוג:PDF כללים"})
                if (!names(lib96.list(q96)).equals(java.util.Arrays.asList("שיעור 12 כללים.pdf"))) only96 = false;
            check("מבנה 96: ברשימה החכמה — בכל שמונה הדרכים לכתוב את זה, רק \"שיעור 12 כללים.pdf\"", only96);
            wipe(r96);
        }
        // מבנה 190: מי כפול של מי (Dups) — עד כאן בתוך מסך הכפולים, בלי אף בדיקת מחשב; לפיו המשתמש מוחק קבצים
        {
            File r190 = new File(home, "מבנה190");
            wipe(r190);
            File a190 = new File(r190, "A"), b190 = new File(r190, "B");
            mk(a190, "ספר.pdf", 10);
            mk(a190, "ספר (1).pdf", 10);
            mk(a190, "אחר.pdf", 10);
            mk(a190, "יחיד.txt", 7);
            mk(a190, "ריק.txt", 0);
            mk(b190, "ספר.pdf", 10);
            mk(b190, "רק-בב.doc", 33);
            mk(b190, "אחר.pdf", 12);
            Index ix190 = built("מבנה190", r190);
            Library lib190 = Library.forTests(Collections.singletonList(ix190));
            List<Library.Hit> all190 = Dups.group(lib190, ""), in190a = new ArrayList<Library.Hit>(), in190b = new ArrayList<Library.Hit>();
            for (Library.Hit x : all190) (x.path().contains("/A/") ? in190a : in190b).add(x);
            check("מבנה 190: נבדקים כל הקבצים חוץ מהריקים (7 מ־8), וגם \"כל הכונן\" — הכול (" + all190.size() + ")",
                    all190.size() == 7 && Dups.group(lib190, Words.Dup.WHOLE_DRIVE).size() == 7 && Dups.group(lib190, null).size() == 7);
            DupMode ns190 = DupMode.parse(null), sim190 = DupMode.parse(Words.Dup.SIZE_SIMILAR), nm190 = DupMode.parse(Words.Dup.NAME),
                    ct190 = DupMode.parse(Words.Dup.CONTENT);
            List<List<Library.Hit>> w190 = Dups.within(ns190, all190);
            check("מבנה 190: שם וגודל — קבוצה אחת: \"ספר.pdf\" בשני המקומות; \"אחר.pdf\" בגודל אחר ו\"ספר (1).pdf\" — לא",
                    w190.size() == 1 && w190.get(0).size() == 2 && w190.get(0).get(0).name().equals("ספר.pdf") && w190.get(0).get(1).name().equals("ספר.pdf")
                            && !Dups.doubt(ns190, w190.get(0)));
            List<List<Library.Hit>> s190 = Dups.within(sim190, all190);
            check("מבנה 190: גודל ושם דומה — \"ספר (1).pdf\" מצטרף (שלושה), הקבוצה \"בספק\"; \"אחר.pdf\" באותו גודל — לא נכנס",
                    s190.size() == 1 && s190.get(0).size() == 3 && Dups.doubt(sim190, s190.get(0)));
            check("מבנה 190: שם בלבד — שתי קבוצות (גם \"אחר.pdf\", אף שהגודל שונה)", Dups.within(nm190, all190).size() == 2);
            {   // מבנה 207: "כפולים:" בתנאי של רשימה חכמה — אותו חישוב, דרך החיפוש (ולכן גם הספירה ו"קבצי חכמה")
                java.util.List<String> d1 = names(lib190.list("כפולים:")), d2 = names(lib190.list("כפולים:גודל-ושם-דומה")),
                        d3 = names(lib190.list("כפולים:שם")), d4 = names(lib190.list("סוג:pdf כפולים:")), d5 = names(lib190.list("ספר כפולים:"));
                check("מבנה 207: \"כפולים:\" (שם וגודל) — רק שני העותקים של \"ספר.pdf\" " + d1, d1.size() == 2 && d1.get(0).equals("ספר.pdf") && d1.get(1).equals("ספר.pdf"));
                check("מבנה 207: \"כפולים:גודל-ושם-דומה\" — גם \"ספר (1).pdf\" " + d2, d2.size() == 3 && d2.contains("ספר (1).pdf"));
                check("מבנה 207: \"כפולים:שם\" — גם \"אחר.pdf\" בשני הגדלים " + d3, d3.size() == 4 && d3.contains("אחר.pdf"));
                check("מבנה 207: עם שאר התנאי — הכפולים מתוך מה שנמצא " + d4 + " " + d5, d4.size() == 2 && d5.size() == 2);
                Library.Result bad207 = lib190.search("כפולים:בלה", false, false);
                check("מבנה 207: \"לפי מה\" שאינו מוכר — נאמר (\"לא הובן\"), ולא מתבצע בשקט", bad207.note != null && bad207.note.contains("בלה") && bad207.hits.isEmpty());
                check("מבנה 207: הספירה של רשימה חכמה — אותו מספר", lib190.list("כפולים:").total == 2);
            }
            Dups.Versus v190 = Dups.versus(ns190, in190a, in190b);
            check("מבנה 190: בין שני מקומות — בשניהם: \"ספר.pdf\" (הקובץ של א׳ ואחריו העותק שבב׳); רק בא׳ — 3; רק בב׳ — 2",
                    v190.both.size() == 1 && v190.both.get(0).size() == 2 && v190.both.get(0).get(0).path().contains("/A/")
                            && v190.both.get(0).get(1).path().contains("/B/") && v190.onlyA.size() == 3 && v190.onlyB.size() == 2);
            {   // מבנה 207 (נמצא בבדיקה של תוסף האבחון במחשב): אותו קובץ פעמיים ברשימת הקבצים (אותו מסלול) — אינו כפיל של עצמו
                List<Library.Hit> twice = new ArrayList<Library.Hit>(all190);
                for (Library.Hit x : all190) if (x.name().equals("יחיד.txt")) twice.add(x);
                check("מבנה 207: קובץ שמופיע פעמיים באותו מסלול — אינו כפיל של עצמו", Dups.within(ns190, twice).size() == 1);
            }
            Dups.Versus self190 = Dups.versus(ns190, in190a, in190a);
            check("מבנה 190: מקום מול עצמו — הקובץ עצמו אינו עותק של עצמו: אין \"בשניהם\", ואין \"רק בב׳\"",
                    self190.both.isEmpty() && self190.onlyA.size() == in190a.size() && self190.onlyB.isEmpty());
            check("מבנה 190: לפי התוכן, בלי חתימות — אין כפולים (קובץ בלי חתימה עומד לבד); 4 קבצים באותו גודל מחכים לחתימה",
                    Dups.within(ct190, all190).isEmpty() && Dups.unsigned(all190) == 4 && Dups.needSig(all190).size() == 4);
            for (Library.Hit x : all190) if (x.name().equals("ספר.pdf")) x.rec.sig = 5; else if (x.name().equals("ספר (1).pdf")) x.rec.sig = 6;
            List<List<Library.Hit>> c190 = Dups.within(ct190, all190);
            check("מבנה 190: לפי התוכן, עם חתימות — רק שני הקבצים שהחתימה שלהם זהה; אותו גודל בחתימה אחרת — לא; נשאר אחד בלי חתימה",
                    c190.size() == 1 && c190.get(0).size() == 2 && Dups.unsigned(all190) == 1 && Dups.needSig(all190).size() == 1);
            String ds190 = read(new File(System.getProperty("vfm.src"), "src/com/tikiyot/vfm/masachim/DupScreen.java"));
            check("מבנה 190: מסך הכפולים אינו מחשב בעצמו מי כפול — הוא שואל את Dups (אין בו מפתח, פיצול או השוואת שמות משלו)",
                    ds190.contains("Dups.within(") && ds190.contains("Dups.versus(") && ds190.contains("Dups.group(lib, ")
                            && !ds190.contains("similarNames") && !ds190.contains("rec.norm") && !ds190.contains("rec.sig"));
            wipe(r190);
        }
        Smarts.Result sm94 = Smarts.over(lib).find("ספר מוגה", "", 100);
        check("מבנה 94: הספירה של הרשימה החכמה ושורותיה — לפי ההתאמה המלאה (" + sm94.count + ")", sm94.count == list94.hits.size());
        String src94 = read(new File(System.getProperty("vfm.src"), "src/com/tikiyot/vfm/chomer/indeks/Smarts.java"))
                + read(new File(System.getProperty("vfm.src"), "src/com/tikiyot/vfm/chomer/masad/ListCount.java"))
                + readAll(new File(System.getProperty("vfm.src"), "src/com/tikiyot/vfm/pkudot"));   // מבנה 169: כל תיקיית הפקודות (הקוד מתפצל)
        String scr94 = read(new File(System.getProperty("vfm.src"), "src/com/tikiyot/vfm/masachim/SearchScreen.java"));
        check("מבנה 94: שומר — מי שמחשב רשימה חכמה (הספירה, הקבצים לפקודות, המסך כשהיא פתוחה) — דרך lib.list, לא דרך החיפוש",
                !src94.contains("lib.search(") && scr94.contains("fixed ? lib.list(q)"));

        String dl = new File(root, "Download").getAbsolutePath();
        eq("73: חיפוש עם מקור (תיקייה באותיות גדולות)", 4, lib.search("מקור:" + dl, false, true).total);
        eq("73: מילה ומקור יחד", 2, lib.search("מוגה מקור:" + dl, false, true).total);

        Index card = built("1A2B-3C4D", drive("1A2B-3C4D"));
        Library two = Library.forTests(Arrays.asList(ix, card));
        eq("73: מקור לפי כרטיס מסוים (אותיות גדולות במזהה)", 6, two.search("מקור:1A2B-3C4D", false, true).total);
        eq("73: מקור:כרטיס", 6, two.search("מקור:כרטיס", false, true).total);
        eq("73: מקור:פנימי|כרטיס", 12, two.search("מקור:פנימי|כרטיס", false, true).total);
    }

    // ---------- 1.9.0 ----------

    private static void v190() {
        File root = new File(home, "v190");
        root.mkdirs();
        // תיקייה חיה ברשימה
        File f = new File(home, "מבנה190.txt");
        f.delete();
        try {
            Files.writeAtomic(f, "#1 לימוד\n@תיקייה /storage/x/הורדות\n/storage/x/a.pdf | 1\n");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // 1.17.3: הוספת תיקייה — רשימה חכמה של התיקייה (Unify.addFolders; התיקייה המתעדכנת הישנה עוד נקראת)
        // תיקייה חיה שזזה — המסלול מתעדכן; תיקייה שעדיין קיימת — לא
        File gone = new File(root, "ישנה"), moved = new File(root, "חדשה");
        moved.mkdirs();
        java.util.List<String> live = new java.util.ArrayList<String>(Arrays.asList(gone.getAbsolutePath()));
        Map<String, String> mv = Collections.singletonMap(gone.getAbsolutePath() + "/משנה/x.pdf", moved.getAbsolutePath() + "/משנה/x.pdf");
        java.util.List<String> stay = new java.util.ArrayList<String>(Arrays.asList(moved.getAbsolutePath()));

        // קריאה ישירה מהכונן
        File d = new File(root, "כונן");
        mk(d, "ב.pdf", 3);
        mk(d, "א.pdf", 3);
        mk(d, ".מוסתר", 1);
        new File(d, "תיקייה").mkdirs();
        DiskList.Listing ls = DiskList.read(d, false);
        check("1.9.0: תיקיות וקבצים", ls.dirs.size() == 1 && ls.files.size() == 2 && ls.hidden == 1);
        eq("1.9.0: לפי השם", "א.pdf", ls.files.get(0).getName());
        check("1.9.0: עם מוסתרים", DiskList.read(d, true).files.size() == 3);
        check("1.9.0: תיקייה שאינה קיימת", DiskList.read(new File(root, "אין"), false).missing);
        check("1.9.0: Android/data ו־obb — חסומות", DiskList.blocked(new File("/s/Android/data"))
                && DiskList.blocked(new File("/s/Android/obb/com.a")) && !DiskList.blocked(new File("/s/Android/database")));
        File blockedDir = new File(root, "Android/data");
        blockedDir.mkdirs();
        check("1.9.0: חסומה ריקה — \"חסומה\", לא \"ריקה\"", DiskList.read(blockedDir, false).blocked);

        // כפתורים — ארבעה מקומות ושורות (1.10.0)
        String listBottom = Buttons.local(Buttons.BOTTOM, Words.Top.S_LIST);   // מ0.41: הקובץ הישן נקרא רק למעבר לרשימות (Menus)
        Buttons.Layout old = Buttons.parseAll("⭐ | פקודות | מסך:פקודות\n📂 | מנהל קבצים | מסך:מנהל קבצים\nשורה שבורה\n");
        java.util.List<Buttons.Btn> top = old.rows(Buttons.TOP).get(0);
        check("1.10.0: קובץ של 1.9.0 — הכול בשורה העליונה, עוגנים חוזרים", top.size() == 4 && top.get(0).act.equals(Buttons.HOME)
                && top.get(3).act.equals(Buttons.BACK));
        eq("1.10.0: השם הישן \"מנהל קבצים\" — \"קבצים פיזיים\"", Words.Top.FILES, top.get(2).name);
        check("1.10.0: קובץ של 1.9.0 — ברירת המחדל של התחתונות נשמרת", old.places.get(listBottom) != null && !old.places.get(listBottom).isEmpty());
        Buttons.Layout rows = Buttons.parseAll("[למעלה]\n🏠 | בית | מסך:בית\n---\n→ | חזרה | מסך:חזרה\n[" + listBottom + "]\n");
        check("1.10.0: שתי שורות במקום אחד", rows.rows(Buttons.TOP).size() == 2);
        check("1.10.0: מקום שרוקן — נשאר ריק (בלי ברירת מחדל)", rows.places.get(listBottom) == null || rows.places.get(listBottom).isEmpty());
        StringBuilder many = new StringBuilder("[למעלה]\n");
        for (int i = 0; i < 8; i++) many.append("⭐ | א | מסך:פקודות\n---\n");
        check("1.10.0: עד 5 שורות במקום", Buttons.parseAll(many.toString()).rows(Buttons.TOP).size() == Buttons.MAX_ROWS);

        // START %בית%
        Map<String, String> vars = Parse.vars(root, root.getAbsolutePath(), null);
        Batch.Plan hp = Batch.plan("START %בית%", root, vars);
        check("1.9.0: START %בית%", hp.errors == 0 && Parse.isHome(hp.start));
        check("1.9.0: %בית% רק אחרי START", Batch.plan("COPY %כאן%\\א.pdf %בית%", root, vars).errors == 1);
        check("1.9.0: אחרי %בית% — בלי מסלול", Batch.plan("START %בית%\\x", root, vars).errors == 1);
    }

    private static void v1100() {
        // בכאילו: שורת כותרת; התוכנית מחושבת, והתוצאה — רשימה זמנית
        File root = new File(home, "ש1100");
        new File(root, "הורדות").mkdirs();
        try {
            Files.writeAtomic(new File(root, "הורדות/א.pdf"), "x");
            Files.writeAtomic(new File(root, "הורדות/ב.txt"), "x");
        } catch (Exception e) { throw new RuntimeException(e); }
        Map<String, String> vars = Parse.vars(root, root.getAbsolutePath(), null);
        Batch.Plan p = Batch.plan("@ECHO OFF\nבכאילו\nMD לשלוח\nMOVE הורדות\\*.pdf לשלוח\nDEL הורדות\\*.txt\nECHO סוף\n", root, vars);
        check("1.10.0: בכאילו — מזוהה", p.asIf && p.errors == 0);
        check("1.10.0: בלי בכאילו — רגיל", !Batch.plan("MD לשלוח", root, vars).asIf);
        Batch.Plan md = Batch.plan("MD א24\\ב\\ג\nRD א24\\ב\\ג\nRD א24\\ב\nRD א24\n", root, vars);
        check("מ0.24: MD של מסלול עמוק — גם התיקיות שמעל קיימות בהדמיה (RD שלהן לא נכשל)", md.errors == 0);
        java.util.List<Batch.AsIfRow> rows = Batch.asIfRows(p);
        String all = "";
        for (Batch.AsIfRow r : rows) all += r.icon + r.title + "|";
        check("1.10.0: ההדמיה — תיקייה חדשה, הקובץ שעבר, מה לסל, הודעה", all.contains("📁לשלוח") && all.contains("📄א.pdf")
                && all.contains("🗑ב.txt") && all.contains("💬סוף") && !all.contains("🗑א.pdf"));
        check("1.10.0: ההדמיה לא נגעה בכונן", new File(root, "הורדות/א.pdf").exists() && !new File(root, "לשלוח").exists());
        eq("1.10.0: הדמיה ריקה", Words.AsIf.NOTHING, Batch.asIfRows(Batch.plan("בכאילו", root, vars)).get(0).title);

        // שליחת תיקייה: כל הקבצים, גם בתיקיות המשנה, בלי מוסתרים
        new File(root, "הורדות/פנימה/.נסתר").mkdirs();
        try {
            Files.writeAtomic(new File(root, "הורדות/פנימה/ג.pdf"), "x");
            Files.writeAtomic(new File(root, "הורדות/פנימה/.נסתר/ד.pdf"), "x");
        } catch (Exception e) { throw new RuntimeException(e); }
        java.util.List<String> names = new java.util.ArrayList<String>();
        for (File f : Files.filesIn(new File(root, "הורדות"))) names.add(f.getName());
        eq("1.10.0: שליחת תיקייה — כל הקבצים, גם בתיקיות משנה", "[א.pdf, ב.txt, ג.pdf]", names.toString());

        // דוגמאות הפקודות: תיקיית התרגול נבנית, וכל קובץ — בכאילו, בלי שגיאות
        String src = System.getProperty("vfm.src");
        if (src == null) return;
        File demo = new File(home, "תרגול1100");
        try {
            DbExamples.demo(demo, new String(java.nio.file.Files.readAllBytes(new File(src, "assets/examples/demo.txt").toPath()), "UTF-8"));
        } catch (Exception e) { throw new RuntimeException(e); }
        File[] bats = demo.listFiles();
        int n = 0;
        if (bats != null) for (File b : bats) {
            if (!Batch.isBatch(b.getName()) || !Character.isDigit(b.getName().charAt(0))) continue;   // 1.13.0: התרגול — המספרים (התוכנות — בבדיקה של 1.13.0)
            n++;
            String text = "";
            try { text = Files.readUtf8(b); } catch (Exception e) { }
            Batch.Plan bp = Batch.plan(text, demo, Parse.vars(demo, home.getAbsolutePath(), null));
            String errs = "";
            for (Batch.Step st : bp.steps) if (st.error != null) errs += st.line + ": " + st.error + " ";
            check("1.10.0: דוגמת פקודות בכאילו ובלי שגיאות — " + b.getName() + " " + errs, bp.asIf && bp.errors == 0 && bp.changes() > 0 || bp.start != null && bp.asIf && bp.errors == 0);
        }
        eq("1.10.0: ארבע דוגמאות פקודות", 4, n);
        check("1.10.0: קובץ \"ישן\" — לפני שנתיים", System.currentTimeMillis() - new File(demo, "הורדות/קבלה ישנה.txt").lastModified() > 700L * 24 * 3600 * 1000);
        Batch.Plan del = null;
        for (File b : bats) if (b.getName().startsWith("3 ")) try { del = Batch.plan(Files.readUtf8(b), demo, Parse.vars(demo, home.getAbsolutePath(), null)); } catch (Exception e) { }
        check("1.10.0: דוגמה 3 — מוחקת רק את הישנים", del != null && del.steps.get(0).files.size() == 2);
    }



    private static void v1120() {
        try {
            v1120run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void v1120run() throws Exception {
        // ברירת המחדל — המראה של היום: שורות, צלמית 54dp (340% מ־16), שם 16
        Design def = Design.parse(Design.DEFAULT, Design.builtInText(Design.DEFAULT));
        check("1.12.0: ברירת מחדל — שורות", !def.grid && def.cols == 1 && def.icon == 340 && def.nameT.size == 16
                && Design.FILL.equals(def.items.of(ItemKeys.AROUND)) && !def.items.on(ItemKeys.FIT) && def.items.num(ItemKeys.LINES) == 2);
        Design files = Design.parse(Design.FILES, Design.builtInText(Design.FILES));
        check("1.12.0: תוכנת ניהול קבצים — רשת 3, מסגרת, התאם למקום (מבנה 258: הצלמית והריווח — מגודל השם: 500%, 35%)", files.grid && files.cols == 3
                && files.icon == 500 && files.gap == 35 && Design.FRAME.equals(files.items.of(ItemKeys.AROUND)) && files.items.on(ItemKeys.FIT) && files.nameT.weight == 700);
        // מבנה 258 (המשתמש, 18.11.2026: "האם יש דרך לקבוע יחס אחיד לשניהם?" — "כן"): צלמית וריווח — אחוז מגודל השם, בשורות וברשת
        Design g258 = Design.parse("רשת", Design.builtInText(Design.DEFAULT).replace("סגנון = שורות", "סגנון = רשת"));
        check("מבנה 258: אותו מספר — אותה צלמית ואותו ריווח בשורות וברשת (בלי מה שיתקן אחרי החלפת סגנון)", g258.grid && !def.grid
                && g258.iconPx(48f, 1080, g258.padPx(48f, 6)) == def.iconPx(48f, 1080, def.padPx(48f, 0)) && def.iconPx(48f, 1080, 24) == 163
                && g258.padPx(48f, 6) == def.padPx(48f, 0) && def.padPx(48f, 0) == 24);
        check("מבנה 258: ברשת — הצלמית לא גדולה ממה שנכנס באריח; בשורות — לא מוגבלת; ריווח 0 ברשת — המעט שנשאר", g258.iconPx(48f, 120, 10) == 100
                && def.iconPx(48f, 120, 10) == 163 && Design.parse("x", "[אמצע]\nמידות = לפי השם\nסגנון = רשת\nריווח = 0%\n").padPx(48f, 6) == 6);
        // מבנה 265, 267: מפתחות הפריט — טבלה אחת (ItemKeys); המתג, הלוח והעורך נגזרים ממנה
        Design x265 = Design.parse("בדיקה", Design.builtInText(Design.FILES));
        boolean in265 = true;
        for (ItemKeys.Def x : ItemKeys.DEFS)
            in265 &= java.util.Arrays.asList(Design.SHAPE_KEYS).contains(x.key) && java.util.Arrays.asList(Design.SHAPE_IDS).contains(x.id)
                    && (x.range == null) == (x.values != null) && (x.values == null || java.util.Arrays.asList(x.values).contains(x.def))
                    && x.def.equals(new Design().shapeValue(x.key)) && Words.Verb.usage(Verbs.SHAPE).contains(Verbs.shapeSwitch(x.key))
                    && Design.lookError(x.key, x.def) == null && Design.lookError(x.key, "אולי") != null;
        Design y265 = Design.parse("בדיקה", "[אמצע]\nסגנון = רשת\nצלמית לצד השם = כן\nהצג שם = לא\nשורות לשם = 40\nגודל מזערי = x\nסביב הצלמית = משושה\n");
        check("מבנה 265, 267: כל מפתח בטבלה — בין מפתחות הפריט, עם שם קבוע, ערכים או טווח, ברירת מחדל, מתג, ובעזרה; ערך שאינו מהערכים — שגיאה ב\"עצב\"",
                in265 && Design.SHAPE_KEYS.length == Design.SHAPE_IDS.length && Design.SHAPE_KEYS.length == 13 && Design.K_STYLE.equals(Design.SHAPE_KEYS[0]));
        check("מבנה 267: נקרא מטקסט של עיצוב — מה שנכתב; מספר — בתוך הטווח; לא מספר — נשאר; ערך לא מוכר — ברירת המחדל; עותק — עם הערכים",
                y265.items.on(ItemKeys.SIDE) && !y265.items.on(ItemKeys.SHOW_NAME) && y265.items.num(ItemKeys.LINES) == 6 && y265.items.num(ItemKeys.MIN) == 11
                        && Design.FILL.equals(y265.items.of(ItemKeys.AROUND)) && !x265.items.on(ItemKeys.SIDE) && x265.items.on(ItemKeys.SHOW_NAME)
                        && y265.copy().items.on(ItemKeys.SIDE) && !y265.signature().equals(x265.signature())
                        && Design.FRAME.equals(x265.items.of(ItemKeys.AROUND)) && x265.items.on(ItemKeys.FIT) && x265.items.on(ItemKeys.SHOW_DATA));
        y265 = Design.parse("בדיקה", Design.builtInText(Design.FILES) + "צלמית לצד השם = כן\n");
        check("מבנה 265: צלמית לצד השם — ברשת הצלמית עד כשליש מהאריח (השאר לשם); בלי — כמו עד כאן",
                y265.grid && y265.iconPx(48f, 300, 10) == 105 && x265.iconPx(48f, 300, 10) == 240 && y265.iconPx(10f, 300, 10) == 50);
        check("מבנה 258: התוכנה אומרת לתוספים שהיא מודדת מגודל השם (namesize)",
                read(new File(System.getProperty("vfm.src"), "src/com/tikiyot/vfm/lev/Bridge.java")).contains(" namesize "));
        check("מבנה 269: התוכנה אומרת לתוספים שצבעים נקבעים גם לרשימה (listcolors)",
                read(new File(System.getProperty("vfm.src"), "src/com/tikiyot/vfm/lev/Bridge.java")).contains(" listcolors\""));
        // מבנה 275 (ממצא 65; המשתמש, על מ0.50: הקיפול בגלילה (272) "קופצני" — "אולי עדיף פשוט אזור דק שנגיעה או גרירה בו מסלקת או
        // מחזירה את השורה"): הכלל של הרצועה — נגיעה מחליפה; גרירה למעלה מקפלת, למטה מחזירה; גרירה לכיוון של מה שכבר נעשה — כלום
        boolean tap275 = FoldTrack.tap(5, 12) && FoldTrack.tap(-11, 12) && !FoldTrack.tap(12, 12) && !FoldTrack.tap(-30, 12)
                && FoldTrack.release(3, 12, false) == FoldTrack.FOLD && FoldTrack.release(-3, 12, true) == FoldTrack.OPEN;
        boolean drag275 = FoldTrack.release(-40, 12, false) == FoldTrack.FOLD && FoldTrack.release(40, 12, true) == FoldTrack.OPEN
                && FoldTrack.release(-40, 12, true) == FoldTrack.NONE && FoldTrack.release(40, 12, false) == FoldTrack.NONE;
        check("מבנה 275: הרצועה הדקה — נגיעה מקפלת או מחזירה; גרירה למעלה מקפלת, למטה מחזירה; לכיוון של מה שכבר נעשה — כלום", tap275 && drag275);
        String top272 = read(new File(System.getProperty("vfm.src"), "src/com/tikiyot/vfm/masachim/rechivim/TopRow.java")),
                room272 = read(new File(System.getProperty("vfm.src"), "src/com/tikiyot/vfm/masachim/rechivim/ScrollRoom.java"));
        check("מבנה 275: הקיפול — במקום אחד לכל המסכים (המסגרת של TopRow.wrap): הרצועה בין השורות שלמעלה למסך; היא מקפלת את השורות שלמעלה, "
                        + "שלמטה (במחזיק — פס הבחירה נשאר כמו שהוא), הכותרת, ומה שהמסך סימן; הגלילה בקבצים — בלי קיפול (272 ירד); מצב אחד לכל המסכים; "
                        + "מה שהרצועה עושה — במילון",
                top272.contains("ScrollRoom box = new ScrollRoom(a);") && top272.contains("box.parts(top, content, hold);") && top272.contains("hold.addView(bottom")
                        && top272.contains("box.addView(box.strip(false), ") && top272.contains("box.addView(box.strip(true), ")
                        && top272.indexOf("box.addView(top, ") < top272.indexOf("box.addView(box.strip(false), ") && top272.indexOf("box.addView(box.strip(false), ") < top272.indexOf("box.addView(content, ")
                        && top272.indexOf("box.addView(content, ") < top272.indexOf("box.addView(box.strip(true), ") && top272.indexOf("box.addView(box.strip(true), ") < top272.indexOf("box.addView(hold, ")
                        && !room272.contains("dispatchTouchEvent") && !room272.contains("canScrollVertically") && !room272.contains("OnScrollChangedListener")
                        && room272.contains("private static boolean foldedTop, foldedBottom;") && room272.contains("FoldTrack.release(low ? -dy : dy, step, low ? foldedBottom : foldedTop)")
                        && room272.contains("bottom.setVisibility(foldedBottom ? GONE : VISIBLE);")
                        && room272.contains("top.setVisibility(v);") && room272.contains("findViewWithTag(FOLD_TAG)")
                        && room272.contains("if (bar != null) bar.hide();") && !room272.contains("bar.show();")
                        && room272.contains("Words.Top.UNFOLD_ROWS : Words.Top.FOLD_ROWS") && room272.contains("Words.Top.UNFOLD_LOW : Words.Top.FOLD_LOW")
                        && !Words.Top.FOLD_ROWS.isEmpty() && !Words.Top.UNFOLD_ROWS.isEmpty() && !Words.Top.FOLD_LOW.isEmpty() && !Words.Top.UNFOLD_LOW.isEmpty());
        check("מבנה 278: הכותרת של אנדרואיד מוסתרת, ושם המסך וה־⋮ ברצועה שלמעלה — אותו תפריט של המסך (המסך בונה, המסך מקבל את הבחירה), "
                        + "והשם מתעדכן כשהמסך משנה אותו",
                room272.contains("a.onCreateOptionsMenu(pm.getMenu());") && room272.contains("a.onPrepareOptionsMenu(pm.getMenu());")
                        && room272.contains("return a.onOptionsItemSelected(item);") && room272.contains("CharSequence t = a.getTitle();")
                        && room272.contains("addOnPreDrawListener(fresh)") && !Words.Top.SCREEN_MENU.isEmpty());
        check("מבנה 277: החץ שעל הרצועה מראה לאן השורות ילכו — למעלה: מוצגות ▲, מקופלות ▼; למטה: מוצגות ▼, מקופלות ▲",
                FoldTrack.arrow(false, false).equals("▲") && FoldTrack.arrow(false, true).equals("▼")
                        && FoldTrack.arrow(true, false).equals("▼") && FoldTrack.arrow(true, true).equals("▲"));
        String s273 = read(new File(System.getProperty("vfm.src"), "src/com/tikiyot/vfm/masachim/SearchScreen.java"));
        check("מבנה 273: בחיפוש — מה שבין שורת החיפוש לקבצים (מהירים, הגדרות, מידע, מקרא, איפה עומדים) בחלק אחד שמתקפל עם השורות; שורת החיפוש "
                        + "והקבצים — לא בתוכו; שורת המידע ושם של כפתור — בשורה אחת",
                s273.contains("head = ScrollRoom.head(this);") && room272.contains("h.setTag(FOLD_TAG);") && s273.contains("root.addView(SmartForm.fieldRow(this, input));") && s273.contains("root.addView(list, ")
                        && s273.contains("head.addView(quick.view);") && s273.contains("head.addView(settings);") && s273.contains("head.addView(panel, ")
                        && s273.contains("head.addView(info);") && s273.contains("head.addView(legend);") && s273.contains("head.addView(where);")
                        && s273.contains("ScrollRoom.oneLine(info);") && s273.contains("ScrollRoom.wide(info); return;") && room272.contains("t.setSingleLine(t.getMaxLines() != 1);")
                        && top272.contains("nm.setMaxLines(1);") && !top272.contains("nm.setMaxLines(2);"));
        String b271 = read(new File(System.getProperty("vfm.src"), "assets/plugins/itzuv.html")), e271 = read(new File(System.getProperty("vfm.src"), "assets/plugins/orech-interaktivi.html"));
        check("מבנה 271: הלוח והעורך מציעים צבעים לרשימה רק לתוכנה שאומרת שהיא מכירה (listcolors) — שואלים, לא מנחשים",
                b271.contains("split(' ').includes('listcolors')") && b271.contains("const noColors=()=>isList()&&!LISTCOL();") && !b271.contains("if(isList()){c4.")
                        && e271.contains("indexOf(' listcolors ')>=0") && e271.contains("DETS[x].prog&&!compat.listcolors&&v[0]&&v[0].is==='list'"));
        Design words = Design.parse(Design.WORDS, Design.builtInText(Design.WORDS));
        check("1.12.0: מילים וצבע — בלי צלמית, עם צבע רקע", !words.grid && words.cols == 2 && !words.items.on(ItemKeys.SHOW_ICON) && words.items.on(ItemKeys.TINT));

        // מה שחסר — מברירת המחדל; שורה לא מובנת — לא נלקחת; הערה בסוף שורה
        Design part = Design.parse("חלקי", "[אמצע]\nסגנון = רשת ; הערה\nטורים = ארבעה\nשם: משקל = מודגש\nמשהו מוזר\n[לא קיים]\nגודל = 99\n");
        check("1.12.0: חלקי — רשת, משקל מודגש", part.grid && part.nameT.weight == 700);
        eq("1.12.0: ערך לא מובן — נשאר מברירת המחדל", 1, part.cols);
        eq("1.12.0: מה שחסר — מברירת המחדל", 16f, part.nameT.size);
        eq("1.12.0: משקל — שם ↔ מספר", 300, Design.weight("דק"));
        eq("1.12.0: מספר טורים — בגבולות", 8, Design.parse("x", "[אמצע]\nטורים = 40\n").cols);

        // @עיצוב ברשימה — נקרא ונשמר; הירושה
        File tf = new File(home, "מבנה-1120.txt");
        Files.writeAtomic(tf, "@עיצוב פנקס שורש\n#1 לימוד\n@עיצוב תוכנת ניהול קבצים\n#2 לימוד / הערות\n#3 לימוד / הערות / ישנות\n@עיצוב מילים וצבע\n#1 ספרים\n");
        File tf2 = new File(home, "מבנה-1120ב.txt");
        // מבנה 266 (המשתמש, 18.11.2026: "אולי את העיצובים הקבועים לשים במקום אחד ואת המשתנים בשני" · "תתחיל נקי"): הקבועים — בקוד בלבד
        Design g = Design.get(Design.FILES);
        File was266 = new File(new File(Paths.settings(), "עיצובים"), Design.FILES + ".עיצוב.txt");   // קובץ עיצוב של פעם — כבר לא נקרא
        was266.getParentFile().mkdirs();
        Files.writeAtomic(was266, "[אמצע]\nטורים = 5\n");
        check("מבנה 266: עיצוב קבוע — מהקוד (רשת, 3 טורים); קובץ עיצוב של פעם באותו שם אינו משנה אותו, ואינו נמחק",
                g.grid && g.cols == 3 && Design.get(Design.FILES).cols == 3 && Design.get(Design.WORDS).cols == 2 && was266.isFile());
        was266.delete();
        eq("1.12.0: עיצוב שלא קיים — ברירת המחדל (לא נשבר)", Design.DEFAULT, Design.get("אין כזה").name);
        check("מבנה 266: העיצובים שאפשר לבחור — שלושת הקבועים; שם קודם של קבוע — עדיין מובן; שם אחר — אין",
                Design.names().equals(Arrays.asList(Design.DEFAULT, Design.FILES, Design.WORDS)) && Design.known("תוכנת ניהול קבצים")
                        && Design.FILES.equals(Design.get("תוכנת ניהול קבצים").name) && !Design.known("מדף ספרים") && !Design.known(null));
        Design c266 = Design.get(Design.FILES).copy();
        c266.cols = 7;
        check("מבנה 266: עיצוב קבוע אינו משתנה — משנים עותק", Design.get(Design.FILES).cols == 3 && Design.get(Design.FILES) == g);
        String src266 = read(new File(System.getProperty("vfm.src"), "src/com/tikiyot/vfm/chomer/itzuv/Design.java"));
        check("מבנה 266: העיצוב אינו כותב ואינו מוחק קבצים; החלון הישן ירד",
                !src266.contains("writeAtomic") && !src266.contains(".delete()") && !src266.contains("Permit.") && !src266.contains("java.io.FileOutputStream")
                        && !new File(System.getProperty("vfm.src"), "src/com/tikiyot/vfm/masachim/itzuv/DesignUi.java").exists());
    }


    private static String namesOf(java.util.List<File> fs) {
        java.util.List<String> n = new java.util.ArrayList<String>();
        for (File f : fs) n.add(f.getName());
        return n.toString();
    }

    private static java.util.List<Buttons.Btn> newRow(Buttons.Layout lay, String place) {
        java.util.List<Buttons.Btn> r = new java.util.ArrayList<Buttons.Btn>();
        lay.rows(place).add(r);
        return r;
    }

    // ---------- מבנה.txt ----------


    // ---------- קובץ ההגדרות ----------


    // ---------- מודל הפריט ----------

    private static void item() {
        File root = drive("פנימי");
        Index ix = built("פנימי", root);
        Library lib = Library.forTests(Collections.singletonList(ix));
        String ok = new File(root, "Download/מוגה.pdf").getAbsolutePath();

        FileItem a = FileItem.of(lib, ok, -1, true);
        eq("80: קובץ במקומו", FileItem.State.OK, a.state);
        eq("80: גודל מהאינדקס", 200L, a.size);
        eq("80: שם", "מוגה.pdf", a.name);
        eq("80: קובץ במקומו גם בלי אינדקס", FileItem.State.OK, FileItem.of(lib, ok, -1, false).state);
        eq("80: מוסתר", FileItem.State.HIDDEN,
                FileItem.of(lib, new File(root, "Documents/.סוד.txt").getAbsolutePath(), -1, true).state);
        eq("80: בתוך הסל", FileItem.State.IN_TRASH, FileItem.of(lib, ix.path(rec(ix, "נמחק.pdf")), -1, true).state);
        eq("80: חסר", FileItem.State.MISSING,
                FileItem.of(lib, new File(root, "אין/כזה.pdf").getAbsolutePath(), 5, true).state);
        eq("66: כונן שאינו זמין — לא 'חסר'", FileItem.State.OFFLINE,
                FileItem.of(lib, "/storage/0000-DEAD/Download/x.pdf", 5, true).state);

        // 58: קובץ שנמחק ונרשם ביומן המחיקות — "נמחק · בסל המחזור"
        String orig = new File(root, "Download/נמחק עכשיו.pdf").getAbsolutePath();
        File inTrash = mk(new File(root, Index.TRASH), "נמחק עכשיו.pdf", 5);
        Trash.logDeleted(inTrash, orig);
        eq("58: נמחק ונמצא בסל", FileItem.State.DELETED, FileItem.of(lib, orig, 5, true).state);

        eq("סיכום: חסרים", "⚠ 3 חסרים", FileItem.State.MISSING.count(3));
        eq("סיכום: מדיה", "2 במדיה שהוצאה או פגומה", FileItem.State.OFFLINE.count(2));
        eq("סיכום: נמחקו", "🗑 1 נמחקו · בסל המחזור", FileItem.State.DELETED.count(1));
    }

    // ---------- תור אחד לשינויי האינדקס (80) ----------

    private static boolean has(Library lib, String path) {
        Library.Hit h = lib.find(path);
        return h != null && !h.rec.gone;
    }

    private static void queue() {
        File root = drive("תור");
        Index ix = built("תור", root);
        final Library lib = Library.forTests(new ArrayList<Index>(Collections.singletonList(ix)));
        final List<Library.Change> heard = Collections.synchronizedList(new ArrayList<Library.Change>());
        lib.listen(new Library.Listener() {
            @Override public void onChange(Library.Change c) { heard.add(c); }
        });

        // מחיקה מהכונן: האינדקס בשני הקצוות, דרך התור
        String p = new File(root, "Download/מוגה.pdf").getAbsolutePath();
        List<Index> before = lib.indexes;
        Physical.Result o = Physical.delete(lib, Collections.singletonList(p), true);
        check("מחיקה הצליחה", o.done.contains(p));
        final String trashPath = o.to.get(p);
        final boolean[] sawIt = {true};
        lib.whenApplied(new Runnable() {
            @Override public void run() { sawIt[0] = !has(lib, p); }
        });
        check("התור התרוקן", lib.flush(5000));
        check("62: אחרי העדכון — המחיקה כבר מוצגת כשהמסך מתרענן", sawIt[0]);
        check("המקום הישן ירד מהאינדקס", !has(lib, p));
        check("הקובץ בסל נוסף לאינדקס", has(lib, trashPath));
        check("תמונת מצב: התמונה הקודמת לא השתנתה במקומה", before.get(0).path(rec(before.get(0), "מוגה.pdf")) != null
                && !rec(before.get(0), "מוגה.pdf").gone);
        check("\"מה קרה\": הודעה נשלחה", !heard.isEmpty());
        boolean rem = false, add = false;
        for (Library.Change c : heard) {
            rem |= c.removed.contains(p);
            add |= c.added.contains(trashPath);
        }
        check("\"מה קרה\": מה ירד", rem);
        check("\"מה קרה\": מה נוסף", add);
        eq("58: במצב אפור — \"נמחק · בסל המחזור\"", FileItem.State.DELETED, FileItem.of(lib, p, 200, true).state);
        check("החיפוש לא מוצא את הנמחק", !names(lib.search("מוגה", false, true)).contains("מוגה.pdf"));

        Index saved = Index.load("תור");
        check("נשמר לקובץ אחרי ההצגה", saved != null && rec(saved, "מוגה.pdf") == null);

        // שחזור: חוזר למקומו, דרך התור
        Trash.Item it = null;
        for (Trash.Item t : Trash.list(lib)) if (p.equals(t.original)) it = t;
        check("הקובץ ברשימת הסל", it != null);
        if (it != null) {
            eq("שחזור למקום המקורי", p, Trash.restore(lib, it));
            lib.flush(5000);
            check("אחרי שחזור — באינדקס במקומו", has(lib, p));
            check("אחרי שחזור — לא בסל", !has(lib, trashPath));
            eq("אחרי שחזור — המצב \"במקומו\"", FileItem.State.OK, FileItem.of(lib, p, 200, true).state);
        }

        // שינוי בזמן בנייה: לא הולך לאיבוד כשהתמונה החדשה מוחלפת
        String q = new File(root, "Download/ספר מוגה.epub").getAbsolutePath();
        lib.beginBusy();
        Index fresh = built("תור", root);            // "נסרק" לפני השינוי
        lib.fileRemoved(q);
        lib.flush(5000);
        check("בזמן בנייה — השינוי מוצג מיד", !has(lib, q));
        lib.swapBuilt(fresh);
        lib.endBusy();
        check("בנייה שהסתיימה לא מחזירה את מה שירד בזמן שרצה", !has(lib, q));

        // הרבה שינויים רצופים — כולם מוחלים, בסדר
        List<String> many = new ArrayList<String>();
        for (int k = 0; k < 50; k++) many.add(mk(new File(root, "רבים"), "ק" + k + ".pdf", 1).getAbsolutePath());
        for (String m : many) lib.fileAdded(new File(m));
        lib.fileRemoved(many.get(7));
        lib.flush(5000);
        int in = 0;
        for (String m : many) if (has(lib, m)) in++;
        eq("50 הוספות ואז הסרה אחת — בסדר הנכון", 49, in);

        // 63: קובץ שנוסף מבחוץ (מנהל קבצים אחר) — מופיע בכניסה לתיקייה
        File dl = new File(root, "Download");
        dl.setLastModified(System.currentTimeMillis() - 60000);
        lib.fileAdded(mk(dl, "סתם.pdf", 1));          // האינדקס מכיר את התאריך הישן
        lib.flush(5000);
        Index cur = lib.indexes.get(0);
        cur.dirs.get(cur.dirId(dl.getAbsolutePath())).mtime = dl.lastModified();
        heard.clear();
        mk(dl, "הועתק מבחוץ.pdf", 7);
        dl.setLastModified(System.currentTimeMillis());
        List<File> first = lib.listDir(dl, false, null);
        boolean inFirst = false;
        for (File x : first) inFirst |= x.getName().equals("הועתק מבחוץ.pdf");
        lib.flush(5000);
        boolean inSecond = false;
        for (File x : lib.listDir(dl, false, null)) inSecond |= x.getName().equals("הועתק מבחוץ.pdf");
        check("63: קובץ מבחוץ — מופיע אחרי הסנכרון", inSecond);
        boolean syncedHeard = false;
        for (Library.Change c : heard) syncedHeard |= c.synced.contains(dl.getAbsolutePath());
        check("63: הסנכרון מודיע \"מה קרה\" (כדי שהמסך יתרענן)", syncedHeard);
    }

    // ---------- מרשם הפעולות (80, רמה 2) ----------

    private static List<String> labels(List<Actions.Action> l) {
        List<String> out = new ArrayList<String>();
        for (Actions.Action a : l) out.add(a.label);
        return out;
    }

    private static void actions() {
        for (Actions.Tool t : Actions.Tool.values()) {
            List<Actions.Action> l = Actions.forTool(t);
            boolean seen = false, ok = true;
            for (Actions.Action a : l) {
                if (a.kind == Actions.Kind.DESTRUCTIVE) seen = true;
                else if (seen) ok = false;
            }
            check("64: " + t + " — פעולה הורסת אחרונה בפס", ok);
        }
        for (Actions.Action a : Actions.all())
            if (a.kind == Actions.Kind.DESTRUCTIVE)
                check("64: \"" + a.label + "\" אומרת מאיפה (מהכונן/לצמיתות)",
                        a.label.contains("מהכונן") || a.label.contains("לצמיתות"));
        check("64: המילה \"מחיקה\" לבד לא משמשת", !labels(Actions.all()).contains("מחיקה"));

        List<String> one = labels(Actions.fitting(Actions.Tool.SEARCH, 1, 0, 0));
        List<String> two = labels(Actions.fitting(Actions.Tool.SEARCH, 2, 0, 0));
        check("חיפוש, קובץ אחד: פתיחה", one.contains("פתיחה"));
        check("חיפוש, שני קבצים: בלי פתיחה", !two.contains("פתיחה"));
        check("חיפוש, שני קבצים: מחיקה מהכונן", two.contains("מחיקה מהכונן"));
        eq("חיפוש, כלום לא נבחר: אין פעולות", 0, Actions.fitting(Actions.Tool.SEARCH, 0, 0, 0).size());
        // 174: תיקיות בכונן בבחירה — רק מה שעובד על תיקיות (כמו הפס)
        List<Actions.Action> withDir = Actions.fitting(Actions.Tool.SEARCH, 1, 0, 1);
        check("174: תיקייה + קובץ — מחיקה מהכונן, העברה, הוספה לרשימה", withDir.contains(Actions.DELETE_FROM_DRIVE)
                && withDir.contains(Actions.MOVE_TO_FOLDER) && withDir.contains(Actions.ADD_TO_LIST));
        boolean onlyFolders = true;
        for (Actions.Action x : withDir) if (!x.folders) onlyFolders = false;
        check("174: עם תיקייה — אין פעולה שאינה עובדת על תיקיות", onlyFolders);
        check("174: תיקייה לבד — יש פעולות", !Actions.fitting(Actions.Tool.SEARCH, 0, 0, 1).isEmpty());

        List<String> list = labels(Actions.fitting(Actions.Tool.LIST, 1, 0, 0));
        check("רשימה ידנית: הסרה מהרשימה", list.contains(Words.REMOVE_FROM_LIST));
        check("רשימה ידנית: אין \"הוספה לרשימה\" (יש העברה/הוספה גם)", !list.contains(Actions.ADD_TO_LIST.label));
        check("רשימה ידנית: פתיחה קובץ אחד (חדש מהמרשם)", list.contains("פתיחה"));
        List<String> withList = labels(Actions.fitting(Actions.Tool.LIST, 1, 1, 0));
        check("רשימה ידנית, קובץ ורשימה: בלי מחיקה מהכונן", !withList.contains("מחיקה מהכונן"));
        check("רשימה ידנית, קובץ ורשימה: העברה (ללוח)", withList.contains(Words.VClipW.MOVE));
        // 1.14.0: בכלי וירטואלי — שום פעולה פיזית בין העיקריות או תחת "עוד"; הכול בתפריט "פיזי"
        for (Actions.Tool vt : new Actions.Tool[]{Actions.Tool.SEARCH, Actions.Tool.LIST})
            for (Actions.Action a : Actions.forTool(vt))
                check("1.14.0: " + vt + " — " + a.id + (a.kind == Actions.Kind.VIRTUAL ? " רגילה" : " בתפריט פיזי"),
                        Actions.inPhysicalMenu(a, vt) == (a.kind != Actions.Kind.VIRTUAL) && !(Actions.inPhysicalMenu(a, vt) && a.mainIn(vt)));
        check("1.14.0: שיתוף — פיזי", Actions.SHARE.kind == Actions.Kind.PHYSICAL && Actions.inPhysicalMenu(Actions.SHARE, Actions.Tool.LIST));
        check("1.14.0: במנהל הקבצים — הפיזיות בפס כמו קודם", !Actions.inPhysicalMenu(Actions.DELETE_FROM_DRIVE, Actions.Tool.DRIVE)
                && Actions.DELETE_FROM_DRIVE.mainIn(Actions.Tool.DRIVE));
        check("1.16.0: העתקה — גם רשימות וגם קבצים, וירטואלית (לא בתפריט פיזי)", Actions.DUPLICATE_LIST.fits(Actions.Tool.LIST, 0, 2, 0)
                && Actions.DUPLICATE_LIST.fits(Actions.Tool.LIST, 1, 1, 0) && !Actions.inPhysicalMenu(Actions.DUPLICATE_LIST, Actions.Tool.LIST));
        check("1.16.0: קישור — רק לרשימות", Actions.ALSO_TO_LIST.fits(Actions.Tool.LIST, 0, 2, 0)
                && !Actions.ALSO_TO_LIST.fits(Actions.Tool.LIST, 1, 1, 0));
        check("1.16.0: הגדרות — לרשימה אחת בלבד", Actions.fitting(Actions.Tool.LIST, 0, 1, 0).contains(Actions.SETTINGS_LIST)
                && !Actions.fitting(Actions.Tool.LIST, 1, 1, 0).contains(Actions.SETTINGS_LIST)
                && !Actions.fitting(Actions.Tool.LIST, 0, 2, 0).contains(Actions.SETTINGS_LIST));

        List<String> trash = labels(Actions.fitting(Actions.Tool.TRASH, 3, 0, 0));
        eq("סל: שחזור ומחיקה לצמיתות", Arrays.asList("↩ שחזור", "מחיקה לצמיתות"), trash);
        check("סל: אין מחיקה מהכונן", !labels(Actions.forTool(Actions.Tool.TRASH)).contains("מחיקה מהכונן"));
        check("76: תיקייה נבחרה — הוספה לרשימה", Actions.ADD_TO_LIST.fits(Actions.Tool.DRIVE, 0, 0, 2));
        check("76: תיקייה וקובץ — הוספה לרשימה", Actions.ADD_TO_LIST.fits(Actions.Tool.DRIVE, 1, 0, 1));
        check("82: תיקייה נבחרה — יש מחיקה מהכונן", Actions.DELETE_FROM_DRIVE.fits(Actions.Tool.DRIVE, 1, 0, 1));
        check("82: תיקייה נבחרה — יש העברה", Actions.MOVE_TO_FOLDER.fits(Actions.Tool.SEARCH, 0, 0, 1));
        check("82: תיקייה נבחרה — יש העתקה", Actions.COPY_TO_FOLDER.fits(Actions.Tool.DRIVE, 0, 0, 2));
        check("82: העתקה — פיזית, לפני ההורסות", Actions.forTool(Actions.Tool.DRIVE).indexOf(Actions.COPY_TO_FOLDER)
                < Actions.forTool(Actions.Tool.DRIVE).indexOf(Actions.DELETE_FROM_DRIVE));
        check("82: אין העתקה בסל", !Actions.COPY_TO_FOLDER.in(Actions.Tool.TRASH));
        // 83: הפס — ארבע עיקריות לכל כלי, מחיקה אחרונה, לכל פעולה צלמית ומילה קצרה
        for (Actions.Tool tl : Actions.Tool.values()) {
            List<Actions.Action> mains = new ArrayList<Actions.Action>();
            for (Actions.Action a : Actions.forTool(tl)) {
                check("83: צלמית ומילה — " + a.id, a.icon != null && !a.icon.equals("•") && a.shortLabel != null);
                String src = System.getProperty("vfm.src");
                if (src != null)
                    check("83: יש קובץ צלמית — " + a.icon, new File(src, "res/drawable/ic_" + a.icon + ".xml").isFile());
                if (a.kind == Actions.Kind.DESTRUCTIVE)                              // "מחיקה" לבד לא משמשת (מושגים)
                    check("83: \"" + a.shortLabel + "\" אומרת מאיפה", a.shortLabel.contains("מהכונן") || a.shortLabel.contains("לצמיתות"));
                if (a.mainIn(tl)) mains.add(a);
            }
            check("83: " + tl + " — לכל היותר 4 עיקריות", mains.size() <= 4 && mains.size() >= 3);
            for (int i = 0; i < mains.size() - 1; i++)
                check("83: " + tl + " — ההורסת אחרונה", mains.get(i).kind != Actions.Kind.DESTRUCTIVE);
        }
        check("83: כפולים — העיקריות", Actions.ADD_TO_LIST.mainIn(Actions.Tool.DUP) && Actions.MOVE_TO_FOLDER.mainIn(Actions.Tool.DUP)
                && Actions.COPY_TO_FOLDER.mainIn(Actions.Tool.DUP) && Actions.DELETE_FROM_DRIVE.mainIn(Actions.Tool.DUP));
        check("83: כפולים — פתיחה ושיתוף תחת עוד", Actions.OPEN.in(Actions.Tool.DUP) && !Actions.OPEN.mainIn(Actions.Tool.DUP)
                && Actions.SHARE.in(Actions.Tool.DUP));
        check("83: רשימה ידנית — העברה לתיקייה תחת עוד", Actions.MOVE_TO_FOLDER.in(Actions.Tool.LIST)
                && !Actions.MOVE_TO_FOLDER.mainIn(Actions.Tool.LIST) && Actions.REMOVE_FROM_LIST.mainIn(Actions.Tool.LIST));
        check("83: סל — פתיחה, שחזור, לצמיתות", Actions.OPEN.mainIn(Actions.Tool.TRASH) && Actions.RESTORE.mainIn(Actions.Tool.TRASH)
                && Actions.DELETE_FOREVER.mainIn(Actions.Tool.TRASH));
        check("תיקייה נבחרה — בלי פתיחה", !Actions.OPEN.fits(Actions.Tool.DRIVE, 0, 0, 1));
        check("מנהל הקבצים: אין הסרה מהרשימה", !labels(Actions.forTool(Actions.Tool.DRIVE)).contains(Words.REMOVE_FROM_LIST));
    }

    // ---------- פעולות פיזיות — מחלקה אחת (80.1) ----------

    private static void physical() {
        File root = drive("פיזי");
        Index ix = built("פיזי", root);
        Library lib = Library.forTests(new ArrayList<Index>(Collections.singletonList(ix)));
        File tf = new File(home, "מבנה-פיזי.txt");
        tf.delete();
        String a = new File(root, "Download/מוגה.pdf").getAbsolutePath();
        String b = new File(root, "Download/יוגה למתחילים.pdf").getAbsolutePath();
        String c = new File(root, "Documents/היגיון.txt").getAbsolutePath();

        // מחיקה — ברירת המחדל: יורד מהרשימות (58)
        Physical.Result r = Physical.delete(lib, Arrays.asList(a), false);
        lib.flush(5000);
        eq("מחיקה: בוצעה", 1, r.done.size());
        check("מחיקה: הקובץ בסל בכונן", new File(r.to.get(a)).isFile() && !new File(a).exists());
        check("מחיקה: האינדקס בשני הקצוות", !has(lib, a) && has(lib, r.to.get(a)));

        // מחיקה במצב אפור: נשאר ברשימה
        r = Physical.delete(lib, Arrays.asList(b), true);
        lib.flush(5000);
        eq("58: במצב אפור — לא יורד מהרשימות", 0, r.lists);

        // קובץ שאינו באינדקס — לא נמחק, וההודעה אומרת למה
        r = Physical.delete(lib, Arrays.asList(new File(root, "אין.pdf").getAbsolutePath()), false);
        eq("מחיקה של קובץ לא ידוע נכשלת", 1, r.failed);
        check("הודעה: הסיבה", DiskSay.message("נמחקו", "הוסרו", r).contains("לא נמצא"));

        // העברה לתיקייה: הקובץ זז, האינדקס בשני הקצוות, הרשימות עוקבות
        File dest = new File(root, "יעד");
        r = Physical.transfer(lib, Arrays.asList(c), java.util.Collections.<String>emptyList(), dest, false, null, null, true);
        lib.flush(5000);
        String moved = new File(dest, "היגיון.txt").getAbsolutePath();
        check("העברה: הקובץ ביעד", new File(moved).isFile() && !new File(c).exists());
        check("העברה: האינדקס בשני הקצוות", !has(lib, c) && has(lib, moved));
        mk(new File(root, "Documents"), "היגיון.txt", 3);
        r = Physical.transfer(lib, Arrays.asList(new File(root, "Documents/היגיון.txt").getAbsolutePath()),
                java.util.Collections.<String>emptyList(), dest, false, null, null, true);
        eq("העברה: שם תפוס ביעד — לא נדרס", 1, r.failed);
        eq("העברה: הקובץ שביעד לא השתנה", 10L, new File(moved).length());

        // שחזור, ואז מחיקה לצמיתות של מה שנשאר באפור
        List<Trash.Item> inTrash = Trash.list(lib);
        Trash.Item ofB = null, ofA = null;
        for (Trash.Item x : inTrash) {
            if (b.equals(x.original)) ofB = x;
            if (a.equals(x.original)) ofA = x;
        }
        r = Physical.restore(lib, Arrays.asList(ofA));
        lib.flush(5000);
        check("שחזור: חזר למקומו", new File(a).isFile() && has(lib, a));
        r = Physical.deleteForever(lib, Arrays.asList(ofB));
        lib.flush(5000);
        eq("מחיקה לצמיתות: בוצעה", 1, r.done.size());
        check("מחיקה לצמיתות: לא בכונן", !new File(ofB.path()).exists());
    }

    // ---------- פעולות פיזיות על תיקיות, והעתקה (82) ----------

    /** עונה תשובות קבועות, וסופר כמה פעמים נשאל. */
    private static final class Script implements Physical.Asker {
        final List<Physical.Answer> answers = new ArrayList<Physical.Answer>();
        int asked;

        Script(Physical.Answer... a) {
            answers.addAll(Arrays.asList(a));
        }

        public Physical.Answer ask(File existing, File incoming, String suggested) {
            asked++;
            return answers.isEmpty() ? null : answers.remove(0);
        }
    }

    private static Physical.Answer ans(Physical.Choice c, String name, boolean all) {
        return new Physical.Answer(c, name, all);
    }

    private static Trash.Item trashItem(Library lib, String original) {
        for (Trash.Item x : Trash.list(lib)) if (original.equals(x.original)) return x;
        return null;
    }

    private static List<String> P(String... p) {
        return Arrays.asList(p);
    }

    private static final List<String> NONE = Collections.<String>emptyList();

    private static void folders() {
        File root = drive("תיקיות");
        mk(new File(root, "ספרים/משנה"), "א.pdf", 50);
        mk(new File(root, "ספרים"), "ב.pdf", 60);
        Index ix = built("תיקיות", root);
        Library lib = Library.forTests(new ArrayList<Index>(Collections.singletonList(ix)));
        File tf = new File(home, "מבנה-תיקיות.txt");
        tf.delete();
        String books = new File(root, "ספרים").getAbsolutePath();
        String fa = books + "/משנה/א.pdf", fb = books + "/ב.pdf";
        eq("82: מהאינדקס — קבצים בתיקייה", 2, lib.filesUnder(books).size());
        eq("82: מהאינדקס — גודל התיקייה", 110L, lib.treeStats(books)[1]);

        // אסור
        check("82: שורש הכונן אסור", DiskRules.forbidden(lib, root) != null);
        check("82: סל המחזור אסור", DiskRules.forbidden(lib, new File(root, Index.TRASH)) != null);
        check("82: תיקיית התוכנה אסורה", DiskRules.forbidden(lib, Paths.data()) != null);
        check("82: תיקייה רגילה מותרת", DiskRules.forbidden(lib, new File(books)) == null);
        Physical.Result r = Physical.delete(lib, NONE, P(root.getAbsolutePath()), false);
        check("82: מחיקת שורש נכשלת, והכונן שלם", r.failed == 1 && new File(fb).isFile());

        // מחיקת תיקייה — כולה לסל, הרשימות לפי 58
        r = Physical.delete(lib, NONE, P(books), true);
        lib.flush(5000);
        check("82: מחיקת תיקייה — בסל", !new File(books).exists() && new File(r.to.get(books), "משנה/א.pdf").isFile());
        check("82: מחיקת תיקייה — האינדקס בשני הקצוות", !has(lib, fa) && has(lib, r.to.get(books) + "/משנה/א.pdf"));
        check("82: קובץ מתוך תיקייה שנמחקה — \"בסל\"", Trash.isInTrash(lib, fa));
        eq("82: הודעה", "נמחקה תיקייה אחת (2 קבצים)", DiskSay.message("נמחקו", "הוסרו", r));
        Trash.Item it = trashItem(lib, books);
        check("82: בסל — שורה אחת לתיקייה", it != null && it.dir && it.files == 2 && it.size == 110);
        int trashRows = 0;
        for (Trash.Item x : Trash.list(lib)) if (x.path().contains("ספרים")) trashRows++;
        eq("82: הקבצים שבתוכה לא בשורות נפרדות", 1, trashRows);

        // שחזור לשם תפוס — "(משוחזר)", והרשימות עוקבות
        mk(new File(books), "חדש.pdf", 5);
        r = Physical.restore(lib, P1(it));
        lib.flush(5000);
        String back = books + " (משוחזר)";
        check("82: שחזור תיקייה לשם תפוס", new File(back + "/משנה/א.pdf").isFile() && has(lib, back + "/ב.pdf"));
        check("82: אחרי שחזור — לא בסל", trashItem(lib, books) == null && !Trash.isInTrash(lib, back + "/ב.pdf"));

        // העברה באותו כונן: רגע אחד, הרשימות עוקבות
        File dest = new File(root, "יעד");
        dest.mkdirs();
        r = Physical.transfer(lib, NONE, P(back), dest, false, null, null, false);
        lib.flush(5000);
        String moved = dest + "/ספרים (משוחזר)";
        check("82: העברת תיקייה", new File(moved + "/ב.pdf").isFile() && !new File(back).exists());
        check("82: העברה — האינדקס", has(lib, moved + "/משנה/א.pdf") && !has(lib, back + "/ב.pdf"));

        // לתוך עצמה — אסור
        r = Physical.transfer(lib, NONE, P(moved), new File(moved, "משנה"), true, null, null, false);
        check("82: העתקה לתוך עצמה נכשלת", r.failed == 1 && !new File(moved, "משנה/ספרים (משוחזר)").exists());

        // העתקה לאותה תיקייה — עותק "(2)", בלי לשאול
        String fm = moved + "/ב.pdf";
        Script s0 = new Script();
        r = Physical.transfer(lib, P(fm), NONE, new File(moved), true, s0, null, false);
        check("82: העתקה לאותה תיקייה — (2)", new File(moved, "ב (2).pdf").isFile() && s0.asked == 0);

        // שם תפוס: "מחק את הקודם" — הקודם לסל, הפריטים נשארים ומצביעים על החדש
        String other = new File(root, "Documents/היגיון.txt").getAbsolutePath();
        mk(new File(moved), "היגיון.txt", 99);
        r = Physical.transfer(lib, P(other), NONE, new File(moved), false, new Script(ans(Physical.Choice.REPLACE, null, false)),
                null, false);
        lib.flush(5000);
        eq("82: החלפה — התוכן החדש במקום", 10L, new File(moved, "היגיון.txt").length());
        check("82: החלפה — הקודם בסל", trashItem(lib, moved + "/היגיון.txt") != null);
        int same = 0;

        // שם אחר — שם שהמשתמש כתב
        mk(new File(root, "Documents"), "היגיון.txt", 7);
        r = Physical.transfer(lib, P(other), NONE, new File(moved), true, new Script(ans(Physical.Choice.RENAME, "חדש.txt", false)),
                null, false);
        check("82: שם אחר", new File(moved, "חדש.txt").isFile() && new File(other).isFile());

        // ביטול — עוצר, מה שכבר נעשה נשאר
        File src2 = new File(root, "מקור2");
        mk(src2, "1.pdf", 1);
        mk(src2, "2.pdf", 2);
        mk(src2, "3.pdf", 3);
        File dst2 = new File(root, "יעד2");
        mk(dst2, "1.pdf", 1);
        mk(dst2, "2.pdf", 2);
        mk(dst2, "3.pdf", 3);
        List<String> three = P(src2 + "/1.pdf", src2 + "/2.pdf", src2 + "/3.pdf");
        Script sc = new Script(ans(Physical.Choice.RENAME, null, false), ans(Physical.Choice.CANCEL, null, false));
        r = Physical.transfer(lib, three, NONE, dst2, true, sc, null, false);
        check("82: ביטול — עצר", r.cancelled && r.done.size() == 1 && !new File(dst2, "3 (2).pdf").exists());
        check("82: ביטול — הודעה", DiskSay.message("הועתקו", "הוסרו", r).contains("בוטל"));

        // "אותו דבר לכל השאר" — שואל פעם אחת
        sc = new Script(ans(Physical.Choice.RENAME, null, true));
        r = Physical.transfer(lib, three, NONE, dst2, true, sc, null, false);
        check("82: לכל השאר — נשאל פעם אחת", sc.asked == 1 && r.done.size() == 3);

        // מיזוג תיקיות: שואל רק על קובץ תפוס; המקור שהתרוקן נמחק
        File m1 = new File(root, "מיזוג/כאן");
        mk(m1, "משותף.pdf", 4);
        mk(m1, "רק כאן.pdf", 4);
        File m2 = new File(root, "שם/כאן");
        mk(m2, "משותף.pdf", 8);
        sc = new Script(ans(Physical.Choice.MERGE, null, false), ans(Physical.Choice.RENAME, null, false));
        r = Physical.transfer(lib, NONE, P(m1.getAbsolutePath()), m2.getParentFile(), false, sc, null, false);
        lib.flush(5000);
        check("82: מיזוג", sc.asked == 2 && new File(m2, "רק כאן.pdf").isFile() && new File(m2, "משותף (2).pdf").isFile()
                && new File(m2, "משותף.pdf").length() == 8);
        check("82: מיזוג — המקור שהתרוקן נמחק", !m1.exists());
        check("82: מיזוג — האינדקס", has(lib, m2 + "/רק כאן.pdf") && !has(lib, m1 + "/רק כאן.pdf"));

        // מחיקה לצמיתות של תיקייה: הפריטים האפורים יורדים
        String gone = moved + "/משנה";
        Physical.delete(lib, NONE, P(gone), true);
        lib.flush(5000);
        r = Physical.deleteForever(lib, P1(trashItem(lib, gone)));
        lib.flush(5000);
        eq("82: מחיקה לצמיתות של תיקייה", 1, r.done.size());
        check("82: מחיקה לצמיתות — לא בסל", trashItem(lib, gone) == null);
    }

    private static List<Trash.Item> P1(Trash.Item it) {
        return Collections.singletonList(it);
    }

    /** "עוצר" אחרי n קבצים. */
    private static final class StopAfter implements Physical.Progress {
        final int n;
        int seen;

        StopAfter(int n) {
            this.n = n;
        }

        public void step(int files, int total, long bytes, long totalBytes) {
            seen = files;
        }

        public boolean stopped() {
            return seen >= n;
        }
    }

    private static void acrossDrives() {
        File r1 = drive("כונן א"), r2 = drive("כונן ב");
        File box = new File(r1, "ארגז");
        mk(new File(box, "פנים"), "1.pdf", 11);
        mk(box, "2.pdf", 22);
        mk(box, "3.pdf", 33);
        Index i1 = built("א", r1), i2 = built("ב", r2);
        Library lib = Library.forTests(new ArrayList<Index>(Arrays.asList(i1, i2)));
        File tf = new File(home, "מבנה-כוננים.txt");
        tf.delete();

        Physical.Result r = Physical.transfer(lib, NONE, P(box.getAbsolutePath()), r2, false, null, null, false);
        lib.flush(5000);
        File nb = new File(r2, "ארגז");
        check("82: בין כוננים — הכול עבר", new File(nb, "פנים/1.pdf").length() == 11 && new File(nb, "3.pdf").isFile());
        check("82: בין כוננים — המקור נמחק", !box.exists());
        check("82: בין כוננים — האינדקס בשני הכוננים", has(lib, nb + "/פנים/1.pdf") && !has(lib, box + "/2.pdf"));
        eq("82: בין כוננים — ספירה", 3, r.inner);

        // עצירה באמצע: מה שעבר — עבר, השאר במקור; אין קובץ בשני המקומות
        File back = new File(r1, "חזרה");
        back.mkdirs();
        r = Physical.transfer(lib, NONE, P(nb.getAbsolutePath()), back, false, null, new StopAfter(1), false);
        lib.flush(5000);
        int there = 0, here = 0;
        for (String x : P("פנים/1.pdf", "2.pdf", "3.pdf")) {
            boolean a = new File(back, "ארגז/" + x).isFile(), b = new File(nb, x).isFile();
            if (a && b) here = -99;
            if (a) there++;
            if (b) here++;
        }
        check("82: עצירה — מצב תקין (כל קובץ במקום אחד)", r.cancelled && there == 1 && here == 2);
    }

    // ---------- מיון אחד לכל התוכנה (79) ----------

    private static final class R {
        final String name, folder;
        final long size, mtime, special;
        final boolean dir;
        final int section;

        R(String name, String folder, long size, long mtime, boolean dir, long special, int section) {
            this.name = name; this.folder = folder; this.size = size; this.mtime = mtime;
            this.dir = dir; this.special = special; this.section = section;
        }
    }

    private static final Sort.Key<R> RK = new Sort.Key<R>() {
        public String name(R r) { return r.name; }
        public String folder(R r) { return r.folder; }
        public long size(R r) { return r.size; }
        public long mtime(R r) { return r.mtime; }
        public long added(R r) { return r.special; }                     // 189: בבדיקה — אותו שדה
        public boolean dir(R r) { return r.dir; }
        public long special(R r) { return r.special; }
        public int section(R r) { return r.section; }
    };

    private static final class Mem implements Sort.Store {
        // מבנה 133: השמירה של המיון נעשית ברקע, בתהליכון לכל שמירה — שתי שמירות רצופות כותבות יחד. מפה רגילה אינה בטוחה
        // לכך, ורשומה אחת יכלה ללכת לאיבוד (כך נכשלה "79: הבחירה נשמרת" פעם בכמה הרצות). בתוכנה המקום הוא Conf, והוא מסונכרן.
        final Map<String, String> m = new java.util.concurrent.ConcurrentHashMap<String, String>();
        public String get(String k, String d) { String v = m.get(k); return v == null ? d : v; }
        public void set(String k, String v) { m.put(k, v); }
    }

    private static List<String> order(List<R> l) {
        List<String> out = new ArrayList<String>();
        for (R r : l) out.add(r.name);
        return out;
    }

    private static void sorting() {
        eq("79: בסיס משותף בכל כלי", Arrays.asList("שם", "תאריך", "גודל", "סוג", "תיקייה בכונן"),
                Arrays.asList(Sort.of(null, false, null).options()));
        eq("79: חיפוש — דירוג בראש", "דירוג", Sort.of(Sort.RANK, true, null).options()[0]);
        eq("79: רשימה ידנית — הסדר שלי בראש", "הסדר שלי", Sort.of(Sort.MINE, false, null).options()[0]);
        eq("79: סל — תאריך מחיקה בראש", "תאריך מחיקה", Sort.of(Sort.DELETED_AT, false, null).options()[0]);
        eq("79: ברירת מחדל — האפשרות המיוחדת", Sort.MINE, Sort.of(Sort.MINE, false, new Mem()).by);
        eq("79: בלי מיוחדת — שם", Sort.NAME, Sort.of(null, false, new Mem()).by);
        check("79: \"קובץ חסר קודם\" אינו מיון", !Arrays.asList(Sort.of(Sort.MINE, false, null).options()).contains("קובץ חסר קודם"));

        Mem old = new Mem();
        old.set("מיון", "סדר שמור");
        eq("קובץ הגדרות ישן: \"סדר שמור\" → הסדר שלי", Sort.MINE, Sort.of(Sort.MINE, false, old).by);
        old.set("מיון", "קובץ חסר קודם");
        eq("קובץ הגדרות ישן: \"קובץ חסר קודם\" → הסדר שלי", Sort.MINE, Sort.of(Sort.MINE, false, old).by);

        List<R> rows = new ArrayList<R>(Arrays.asList(
                new R("ב.pdf", "/x", 10, 300, false, 2, 0),
                new R("א.epub", "/y", 30, 100, false, 0, 1),
                new R("ג.pdf", "/x", 20, 200, false, 1, 0),
                new R("תיקייה", "/x", 0, 50, true, 3, 0)));
        Sort s = Sort.of(null, false, null);
        s.apply(rows, RK);
        eq("שם, ותיקיות למעלה", Arrays.asList("תיקייה", "א.epub", "ב.pdf", "ג.pdf"), order(rows));
        s.by = Sort.DATE;
        s.apply(rows, RK);
        eq("תאריך: החדש קודם", Arrays.asList("תיקייה", "ב.pdf", "ג.pdf", "א.epub"), order(rows));
        s.desc = true;
        s.apply(rows, RK);
        eq("כיוון הפוך", Arrays.asList("תיקייה", "א.epub", "ג.pdf", "ב.pdf"), order(rows));
        s.desc = false;
        s.by = Sort.SIZE;
        s.apply(rows, RK);
        eq("גודל: הגדול קודם", Arrays.asList("תיקייה", "א.epub", "ג.pdf", "ב.pdf"), order(rows));
        s.by = Sort.TYPE;
        s.apply(rows, RK);
        eq("סוג, ובתוכו שם", Arrays.asList("תיקייה", "א.epub", "ב.pdf", "ג.pdf"), order(rows));
        s.by = Sort.FOLDER;
        s.apply(rows, RK);
        eq("תיקייה בכונן, ובתוכה שם", Arrays.asList("תיקייה", "ב.pdf", "ג.pdf", "א.epub"), order(rows));

        Sort r = Sort.of(Sort.RANK, true, null);
        r.apply(rows, RK);
        eq("דירוג: לפי הדירוג, גם תיקייה", Arrays.asList("א.epub", "ג.pdf", "ב.pdf", "תיקייה"), order(rows));
        r.group = true;
        r.by = Sort.NAME;
        r.apply(rows, RK);
        eq("קיבוץ לפי סוג התאמה, ובתוכו שם", Arrays.asList("תיקייה", "ב.pdf", "ג.pdf", "א.epub"), order(rows));

        Mem m = new Mem();
        Sort w = Sort.of(Sort.MINE, false, m);
        w.choose(1, w.options().length);             // שם
        w.choose(w.options().length, w.options().length);   // כיוון
        // מבנה 89: השמירה נעשית ברקע. עד כאן — המתנה קבועה של 200 אלפיות, ובמחשב עמוס זה לא הספיק (נכשלה בהרצה אחת משלוש,
        // 15.11.2026). עכשיו: מחכים עד שנשמר, עד 5 שניות.
        for (int k = 0; k < 100 && !("שם".equals(m.get("מיון", "")) && "יורד".equals(m.get("כיוון", ""))); k++)
            try { Thread.sleep(50); } catch (InterruptedException ignored) { }
        eq("79: הבחירה נשמרת", "שם", m.get("מיון", ""));
        eq("79: הכיוון נשמר", "יורד", m.get("כיוון", ""));
        Sort again = Sort.of(Sort.MINE, false, m);
        check("79: נטען כפי שנשמר", again.by.equals("שם") && again.desc);
        eq("תיאור לשורת הסיכום", "ממוין לפי שם ↑", again.describe());
    }

    // ---------- ביקורת רביעית: שמירה על הנתונים (90, 92, 93) ----------

    private static void dataSafety() {
        try {
            dataSafetyRun();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void dataSafetyRun() throws Exception {
        File drive = new File(home, "כונן-90");
        wipe(drive);
        File book = mk(drive, "ספר.pdf", 30);
        Index ix = new Index("בית90", "בית90", home.getAbsolutePath());
        ix.scan(null);
        Library lib = Library.forTests(new ArrayList<Index>(java.util.Collections.singletonList(ix)));
        check("90: קובץ רגיל אינו קובץ התוכנה", DiskRules.programFile(book.getAbsolutePath()) == null);
        lib.flush(5000);

        // 90ב: קובץ קיים שלא נקרא — לא שומרים
        File bad = new File(home, "מבנה-לא-נקרא.txt");
        wipe(bad);
        bad.mkdirs();                                   // קיים, והקריאה נכשלת
        Ini ini = Ini.load(bad);
        check("90: קובץ הגדרות שלא נקרא מסומן", ini.unreadable);
        check("90: שמירה של קובץ הגדרות שלא נקרא נדחית", !ini.saveQuiet() && bad.isDirectory());
        wipe(bad);
    }

    // ---------- ביקורת רביעית: נכונות (95, 88, 89) ----------


    // ---------- ביקורת רביעית: קטנים (102, 104) ----------

    private static void smalls() {
        // 104: זיהוי סיומת
        eq("104: סיומת רגילה", "pdf", Text.ext("ספר.PDF"));
        eq("104: סיומת שכולה ספרות אינה סיומת", "", Text.ext("פרק 1.17"));
        eq("104: \"pdf (1)\" הוא pdf", "pdf", Text.ext("ספר.pdf (1)"));
        eq("104: \"mp3-_=1\" הוא mp3", "mp3", Text.ext("שיר.mp3-_=1"));
        eq("104: \"js-ver=r7\" הוא js", "js", Text.ext("קוד.js-ver=r7"));
        eq("104: \"docx (2)\" הוא docx ולא doc", "docx", Text.ext("מכתב.docx (2)"));
        eq("104: סיומת לא מוכרת נשארת", "pdfx", Text.ext("משהו.pdfx"));
        eq("104: בלי נקודה — בלי סיומת", "", Text.ext("README"));
        File root = new File(home, "כונן-104");
        wipe(root);
        mk(root, "ספר.pdf (1)", 10);
        mk(root, "ספר רגיל.pdf", 10);
        mk(root, "פרק 1.17", 10);
        Index ix = new Index("104", "104", root.getAbsolutePath());
        ix.scan(null);
        Library lib = Library.forTests(new ArrayList<Index>(java.util.Collections.singletonList(ix)));
        eq("104: סוג:pdf מוצא גם \"ספר.pdf (1)\"", 2, lib.search("סוג:pdf", false, false).total);
        eq("104: \"פרק 1.17\" אינו מסוג 17", 0, lib.search("סוג:17", false, false).total);

        // 102: תנאי שלא הובן — נאמר, ולא נבלע
        Library.Result r = lib.search("ספר גודל:2MB", false, false);
        check("102: \"לא הובן\" מופיע בתוצאה", r.note.contains("לא הובן") && r.note.contains("גודל:2mb"));
        eq("102: תנאי תקין — בלי הערה", "", lib.search("ספר גודל:>2MB", false, false).note);
    }

    // ---------- אופני עבודה (105) ----------



    // ---------- אופני עבודה: שינוי שם ומחיקה (105) ----------



    // ---------- בורר הרשימות (106): המודל, בלי ממשק ----------

    private static List<String> pickLabels(List<PickerModel.Row> rows) {
        List<String> l = new ArrayList<String>();
        for (PickerModel.Row r : rows) l.add(r.label);
        return l;
    }

    private static PickerModel.Row rowOf(List<PickerModel.Row> rows, String label) {
        for (PickerModel.Row r : rows) if (r.label.equals(label)) return r;
        return null;
    }



    // ---------- בורר התיקיות: אותו בורר, התיקיות מהאינדקס (106) ----------

    private static Index.Dir pdir(int parent, String name, boolean hidden, boolean trash, boolean gone) {
        Index.Dir d = new Index.Dir();
        d.parent = parent;
        d.name = name;
        d.norm = Text.norm(name);
        d.hidden = hidden;
        d.trash = trash;
        d.gone = gone;
        return d;
    }

    private static void folderPicker() {
        Index ix = new Index("internal", "זיכרון", "/storage/emulated/0");
        ix.dirs.add(pdir(-1, "ספרים", false, false, false));     // 0
        ix.dirs.add(pdir(0, "רמחל", false, false, false));       // 1
        ix.dirs.add(pdir(0, "הגיון", false, false, false));      // 2
        ix.dirs.add(pdir(-1, ".נסתר", true, false, false));      // 3
        ix.dirs.add(pdir(-1, ".נמחקו", true, true, false));      // 4
        ix.dirs.add(pdir(3, "בפנים", true, false, false));       // 5
        ix.dirs.add(pdir(-1, "אבג", false, false, false));       // 6
        ix.dirs.add(pdir(-1, "נמחקה", false, false, true));      // 7
        File cardRoot = new File(home, "כרטיס-בדוי");
        wipe(cardRoot);
        new File(cardRoot, "ב").mkdirs();
        new File(cardRoot, "א").mkdirs();
        new File(cardRoot, ".ג").mkdirs();
        List<IndexSource.Drv> drives = new ArrayList<IndexSource.Drv>();
        drives.add(new IndexSource.Drv("internal", "📱 זיכרון פנימי", "/storage/emulated/0", ix));
        drives.add(new IndexSource.Drv("card", "💾 כרטיס", cardRoot.getAbsolutePath(), null));
        IndexSource src = new IndexSource(drives);
        java.util.List<String> in = java.util.Arrays.asList("internal");

        eq("106: תיקיות מהאינדקס — ממוינות, בלי מוסתרות, סל ומה שנמחק", "[אבג, ספרים]", src.children(in).toString());
        eq("106: תיקיות בתוך תיקייה", "[הגיון, רמחל]", src.children(java.util.Arrays.asList("internal", "ספרים")).toString());
        check("106: תיקייה מוסתרת אינה קיימת לבורר", src.children(java.util.Arrays.asList("internal", ".נסתר")) == null);
        check("106: תיקייה שלא קיימת", src.children(java.util.Arrays.asList("internal", "אין")) == null);
        check("106: כונן שלא קיים", src.children(java.util.Arrays.asList("אין")) == null);
        eq("106: כונן בלי אינדקס — נקרא מהכונן, בלי מוסתרות", "[א, ב]", src.children(java.util.Arrays.asList("card")).toString());
        eq("106: מסלול לתיקייה", "/storage/emulated/0/ספרים/רמחל", src.fileOf(java.util.Arrays.asList("internal", "ספרים", "רמחל")).getPath());
        eq("106: מסלול לשורש הכונן", "/storage/emulated/0", src.fileOf(in).getPath());
        eq("106: שם מלא עם שם הכונן", "📱 זיכרון פנימי / ספרים / רמחל", src.fullLabel(java.util.Arrays.asList("internal", "ספרים", "רמחל")));
        check("106: עץ גדול", src.large());

        java.util.LinkedHashMap<ArrayList<String>, String> roots = new java.util.LinkedHashMap<ArrayList<String>, String>();
        roots.put(new ArrayList<String>(in), "📱 זיכרון פנימי");
        roots.put(new ArrayList<String>(java.util.Arrays.asList("card")), "💾 כרטיס");
        PickerModel m = new PickerModel(src, roots);
        eq("106: הכוננים הם נקודות הפתיחה", PickerModel.U, m.mode);
        eq("מ0.42: הכותרת ברמת הכוננים — \"כוננים\" (היה: \"הרשימות של הכלים\")", Words.DiskW.DRIVES, m.crumb());
        eq("106: תיקיות — הכוננים", "[📱 זיכרון פנימי, 💾 כרטיס]", pickLabels(m.folderRows()).toString());
        m.direct = false;
        m.enter(java.util.Arrays.asList("internal", "ספרים"));
        m.up();
        eq("106: עולים בתוך הכונן", in, m.current);
        m.up();
        check("106: ומהכונן — חזרה לרשימת הכוננים", m.current == null);
        check("106: בשורש הכוננים אין \"לכאן\"", !m.canChooseHere());
        m.enter(in);
        check("106: בתוך כונן אפשר \"לכאן\" (שורש הכונן)", m.canChooseHere());

        // עץ גדול: ענפים סגורים, פותחים בנגיעה
        eq("106: עץ גדול — נקודות הפתיחה פתוחות והשאר סגור", "[📱 זיכרון פנימי, אבג, ספרים, 💾 כרטיס, א, ב]", pickLabels(m.treeRows()).toString());
        PickerModel.Row books = rowOf(m.treeRows(), "ספרים");
        check("106: ענף סגור מסומן — אפשר לפתוח", books.expandable && !books.open && books.kids);
        check("106: תיקייה בלי תיקיות בפנים — לא לפתיחה", !rowOf(m.treeRows(), "אבג").expandable);
        m.toggle(books.path);
        eq("106: אחרי פתיחה", "[📱 זיכרון פנימי, אבג, ספרים, הגיון, רמחל, 💾 כרטיס, א, ב]", pickLabels(m.treeRows()).toString());
        check("106: הענף הפתוח מסומן, והילדים מתחתיו (כונן 0, ספרים 1, רמחל 2)", rowOf(m.treeRows(), "ספרים").open && rowOf(m.treeRows(), "ספרים").depth == 1 && rowOf(m.treeRows(), "רמחל").depth == 2 && rowOf(m.treeRows(), "רמחל").last && !rowOf(m.treeRows(), "רמחל").cont[0]);
        m.toggle(books.path);
        check("106: סגירה", !rowOf(m.treeRows(), "ספרים").open && rowOf(m.treeRows(), "רמחל") == null);

        // חיפוש מוצא גם בענפים סגורים, עם המסלול המלא
        List<PickerModel.Row> found = m.search("רמח");
        eq("106: חיפוש בענף סגור", 1, found.size());
        eq("106: עם המסלול המלא וכונן", "📱 זיכרון פנימי / ספרים / רמחל", found.get(0).sub);
        check("106: לא חתך", !m.truncated);

        // תקרת תוצאות בחיפוש
        Index big = new Index("internal", "זיכרון", "/storage/emulated/0");
        for (int i = 0; i < PickerModel.SEARCH_MAX + 50; i++) big.dirs.add(pdir(-1, "תיקייה " + i, false, false, false));
        List<IndexSource.Drv> bd = new ArrayList<IndexSource.Drv>();
        bd.add(new IndexSource.Drv("internal", "זיכרון", "/storage/emulated/0", big));
        java.util.LinkedHashMap<ArrayList<String>, String> br = new java.util.LinkedHashMap<ArrayList<String>, String>();
        br.put(new ArrayList<String>(in), "זיכרון");
        PickerModel bm = new PickerModel(new IndexSource(bd), br);
        eq("106: חיפוש עם הרבה תוצאות — חתוך", PickerModel.SEARCH_MAX, bm.search("תיקייה").size());
        check("106: ומסומן שיש עוד", bm.truncated);
        check("106: חיפוש צר — לא חתוך", bm.search("תיקייה 7").size() < PickerModel.SEARCH_MAX && !bm.truncated);
        wipe(cardRoot);
    }

    // ---------- שינוי שם בכונן (84), ותיקייה חדשה דרך Physical ----------

    private static void renameOnDrive() {
        try {
            renameOnDriveRun();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void renameOnDriveRun() throws Exception {
        File root = new File(home, "כונן-84");
        wipe(root);
        File dir = new File(root, "ספרים");
        File a = mk(dir, "דרך ה'.pdf", 40);
        mk(dir, "תפוס.pdf", 7);
        Index ix = new Index("84", "84", root.getAbsolutePath());
        ix.scan(null);
        Library lib = Library.forTests(new ArrayList<Index>(java.util.Collections.singletonList(ix)));
        File tf = new File(home, "מבנה-84.txt");
        tf.delete();

        // בדיקת שם
        eq("84: שם ריק", "צריך שם", DiskRules.badName("  "));
        check("84: \"/\" בשם — נדחה", DiskRules.badName("א/ב.pdf") != null);
        check("84: נקודה בלבד — נדחה", DiskRules.badName("..") != null);
        eq("84: שם תקין בעברית", null, DiskRules.badName("מסילת ישרים.pdf"));

        // שינוי שם
        Physical.Result r = Physical.rename(lib, a.getAbsolutePath(), "מסילת ישרים.pdf");
        lib.flush(5000);
        File b = new File(dir, "מסילת ישרים.pdf");
        check("84: הקובץ קיבל שם חדש בכונן", b.isFile() && !a.exists() && b.length() == 40);
        eq("84: הצליח", 1, r.done.size());
        check("84: האינדקס מכיר את השם החדש ולא את הישן", lib.find(b.getAbsolutePath()) != null && lib.find(a.getAbsolutePath()) == null);

        // שם תפוס, שם זהה, שינוי אותיות, קובץ שחסר, קובץ של התוכנה
        r = Physical.rename(lib, b.getAbsolutePath(), "תפוס.pdf");
        check("84: שם תפוס — נדחה, ושני הקבצים במקומם", r.failed == 1 && b.isFile() && new File(dir, "תפוס.pdf").length() == 7);
        r = Physical.rename(lib, b.getAbsolutePath(), "מסילת ישרים.pdf");
        check("84: אותו שם — כלום", r.failed == 0 && r.done.isEmpty() && b.isFile());
        File en = mk(dir, "book.pdf", 3);
        lib.fileAdded(en);
        lib.flush(5000);
        r = Physical.rename(lib, en.getAbsolutePath(), "Book.pdf");
        check("84: שינוי אותיות בלבד", r.failed == 0 && new File(dir, "Book.pdf").isFile());
        r = Physical.rename(lib, new File(dir, "אין.pdf").getAbsolutePath(), "משהו.pdf");
        check("84: קובץ שחסר — נדחה", r.failed == 1);
        r = Physical.rename(lib, b.getAbsolutePath(), "א/ב.pdf");
        check("84: שם לא תקין — נדחה, הקובץ לא זז", r.failed == 1 && b.isFile());

        // תיקייה חדשה — דרך Physical
        eq("106: תיקייה חדשה", null, Physical.makeFolder(lib, dir, "חדשה"));
        check("106: נוצרה בכונן", new File(dir, "חדשה").isDirectory());
        eq("106: שם תפוס", "כבר יש כאן \"חדשה\"", Physical.makeFolder(lib, dir, "חדשה"));
        check("106: שם לא תקין", Physical.makeFolder(lib, dir, "א/ב") != null);
        lib.flush(5000);
        wipe(root);
    }

    // ---------- אריזה ופריסה של ZIP (116) ----------

    private static void zipRoundTrip() {
        try {
            zipRun();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private static void zipRun() throws Exception {
        File root = new File(home, "כונן-107");
        wipe(root);
        File books = new File(root, "ספרים");
        mk(new File(books, "רמח\"ל"), "דרך ה'.pdf", 3000);
        mk(new File(books, "רמח\"ל/מסילה"), "פרק א.epub", 1234);
        mk(books, "הקדמה.txt", 10);
        mk(books, ".nomedia", 0);
        new File(books, "ריקה").mkdirs();
        mk(new File(books, Index.TRASH), "נמחק.pdf", 50);

        long[] m = Zip.measure(books);
        eq("116: מדידה — בלי סל המחזור", 4L, m[0]);
        eq("116: מדידה — בתים", 4244L, m[1]);

        File out = new File(root, "שליחה/ספרים.zip");
        Zip.Result p = Zip.pack(books, out, null);
        check("116: נארז", p.error == null && !p.stopped && out.isFile());
        eq("116: ארבעה קבצים, בלי הסל", 4, p.files);
        check("116: אין קובץ זמני שנשאר", !new File(out.getPath() + ".tmp").exists());
        long[] c = Zip.contents(out);
        check("116: תוכן הקובץ לחלון האישור", c != null && c[0] == 4 && c[1] == 4244);
        check("116: קובץ שאינו ZIP", Zip.contents(new File(books, "הקדמה.txt")) == null);

        // פריסה במקום אחר — כמו בטאבלט
        File other = new File(root, "טאבלט/Download");
        other.mkdirs();
        File arrived = new File(other, "ספרים.zip");
        Files.copy(out, arrived);
        File dest = new File(other, "ספרים");               // מבנה 135: היעד נקבע בפקודה "פרוס" (נבדק במבנה 122); Zip.target נמחקה
        Zip.Result u = Zip.unpack(arrived, dest, null);
        check("116: נפרס", u.error == null && u.files == 4 && u.skipped == 0);
        check("116: קובץ ה-ZIP נשאר", arrived.isFile());

        // פעם שנייה — שם תפוס, ובלי הכפלה של התיקייה העליונה
        File dest2 = new File(other, "ספרים (2)");
        Zip.unpack(arrived, dest2, null);

        // האינדקס לומד על מה שנפרס
        Index ix = new Index("107", "107", root.getAbsolutePath());
        ix.scan(null);
        Library lib = Library.forTests(new ArrayList<Index>(java.util.Collections.singletonList(ix)));
        File dest3 = new File(other, "ספרים (3)");
        Zip.unpack(arrived, dest3, null);
        lib.dirChanged(other.getAbsolutePath());
        lib.flush(5000);
        check("116: האינדקס מכיר קובץ עמוק שנפרס", lib.find(new File(dest3, "רמח\"ל/מסילה/פרק א.epub").getAbsolutePath()) != null);

        // קובץ ZIP עם כמה פריטים בשורש — לא מושמטת קידומת
        File loose = new File(root, "שונים.zip");
        java.util.zip.ZipOutputStream zo = new java.util.zip.ZipOutputStream(new java.io.FileOutputStream(loose));
        zo.putNextEntry(new java.util.zip.ZipEntry("א.txt"));
        zo.write(new byte[5]);
        zo.closeEntry();
        zo.putNextEntry(new java.util.zip.ZipEntry("ב/ג.txt"));
        zo.write(new byte[6]);
        zo.closeEntry();
        zo.putNextEntry(new java.util.zip.ZipEntry("../../מחוץ.txt"));      // ניסיון לצאת מהיעד
        zo.write(new byte[7]);
        zo.closeEntry();
        zo.putNextEntry(new java.util.zip.ZipEntry("/etc/מוחלט.txt"));
        zo.write(new byte[8]);
        zo.closeEntry();
        zo.close();
        File ld = new File(loose.getParentFile(), "שונים");
        Zip.Result lr = Zip.unpack(loose, ld, null);
        eq("116: שמות שמנסים לצאת מהיעד — מדולגים", 2, lr.skipped);
        check("116: לא נכתב שום דבר מחוץ ליעד", !new File(root, "מחוץ.txt").exists() && !new File(home, "מחוץ.txt").exists());

        // לא מחליף קובץ קיים
        File keep = new File(root, "שמירה");
        mk(keep, "א.txt", 99);
        Zip.Result kr = Zip.unpack(loose, keep, null);
        check("116: קובץ קיים לא מוחלף", new File(keep, "א.txt").length() == 99 && kr.skipped >= 1);

        // מתי הפעולה מוצעת
        check("116: \"פריסת ZIP\" רק לקובץ ZIP אחד", Actions.isZip(P("/x/א.zip")) && Actions.isZip(P("/x/א.ZIP"))
                && !Actions.isZip(P("/x/א.pdf")) && !Actions.isZip(P("/x/א.zip", "/x/ב.zip")));
        check("116: שליחה — גם כמה קבצים וגם תיקיות", !Actions.SHARE.one && Actions.SHARE.folders);
        check("116: תיקיית האריזות מוגנת (בתוך תיקיית התוכנה)", DiskRules.programFile(new File(Files.sendDir(), "x.zip").getAbsolutePath()) != null);
        wipe(root);
    }

    // ---------- שליחה למכשיר: אילו תוכנות מוצגות (117) ----------

    private static void transferApps() {
        check("117: Xiaomi Share", Transfer.isTransfer("com.miui.mishare.connectivity", "com.miui.mishare.connectivity.ShareActivity"));
        check("117: ShareMe", Transfer.isTransfer("com.xiaomi.midrop", "x.y.Z"));
        check("117: Bluetooth", Transfer.isTransfer("com.android.bluetooth", "com.android.bluetooth.opp.BluetoothOppLauncherActivity")
                && Transfer.isTransfer("com.google.android.bluetooth", "a.b.C"));
        check("117: שיתוף מהיר של Google", Transfer.isTransfer("com.google.android.gms", "com.google.android.gms.nearby.sharing.ShareSheetActivity"));
        check("117: שאר שירותי Google — לא", !Transfer.isTransfer("com.google.android.gms", "com.google.android.gms.other.SomeActivity"));
        check("117: וואטסאפ — לא", !Transfer.isTransfer("com.whatsapp", "com.whatsapp.contact.picker.ContactPicker"));
        check("117: ג'ימייל ודרייב — לא", !Transfer.isTransfer("com.google.android.gm", "x") && !Transfer.isTransfer("com.google.android.apps.docs", "x"));
        check("117: התוכנה שלנו — לא", !Transfer.isTransfer("com.tikiyot.vfm", "com.tikiyot.vfm.ShareScreen"));
        check("117: שתי פעולות נפרדות, שתיהן לכמה קבצים ולתיקיות",
                Actions.SEND_DEVICE != Actions.SHARE && !Actions.SEND_DEVICE.one && Actions.SEND_DEVICE.folders && Actions.SHARE.folders);
        check("117: \"שליחה למכשיר\" לפני \"שיתוף\" בפס", Actions.forTool(Actions.Tool.DRIVE).indexOf(Actions.SEND_DEVICE)
                < Actions.forTool(Actions.Tool.DRIVE).indexOf(Actions.SHARE));
    }

    // ---------- קובצי פקודות (117א) ----------

    private static void batch() {
        try {
            batchRun();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** מ0.27 (נמצא בהרצה במכשיר, 10.11): אחרי RD /S, MD לתת־תיקייה נחשב "כבר קיימת" ולא רץ, ו־COMP השווה מול מה שנמחק. */
    private static void m027() {
        File root = new File(home, "כונן-027");
        wipe(root);
        File here = new File(root, "כאן");
        here.mkdirs();
        File src = new File(root, "מקור"), work = new File(root, "עבודה");
        mk(src, "א.txt", 10);
        mk(new File(work, "נוכחי"), "א.txt", 10);
        java.util.Map<String, String> vars = Parse.vars(here, root.getAbsolutePath(), null);
        Batch.Plan p = Batch.plan("RD \"" + work + "\" /S\nMD \"" + work + "/נוכחי\"\nCOMP \"" + src + "\" \"" + work + "/נוכחי\"\n"
                + "COPY %רק-במקור% \"" + work + "/נוכחי\"", here, vars);
        eq("מ0.27: בלי שגיאות", 0, p.errors);
        check("מ0.27: MD אחרי RD /S — רץ (לא \"כבר קיימת\")", !p.steps.get(1).noop);
        eq("מ0.27: COMP אחרי RD /S — מול תיקייה ריקה: הקובץ רק במקור", 1, p.steps.get(3).files.size());
    }

    private static void m038lab() {
        String[] a = Lab.nameOf("<title>x</title><meta name=\"vfm-lab\" content=\"עורך\"><meta content=\"3\" name=\"vfm-lab-version\">", "בלה.html");
        check("מ0.38: שם וגרסה — מתוך העמוד (meta)", a[0].equals("עורך") && a[1].equals("3"));
        a = Lab.nameOf("<p>", "מעבדה עורך 2 (1).html");
        check("מ0.38: בלי meta — משם הקובץ, בלי \"(1)\" של הדפדפן", a[0].equals("עורך") && a[1].equals("2"));
        a = Lab.nameOf("<p>", "מעבדה.html");
        check("מ0.38: בלי שם בכלל — \"תוסף\", וגרסה \"?\" (מ0.40: קבועה, לא לפי הזמן)", a[0].equals(Words.Lab.UNNAMED) && a[1].equals("?"));
        eq("מ0.38: שם לא תקין — מתוקן לדומה", "א-ב (ג) חו״ל", Lab.safe("א/ב <ג> חו\"ל*"));
        {   // מבנה 204 (המשתמש, 16.11.2026: "לא כל קובץ html הוא תוסף"): תוסף — רק עמוד שכתוב בו שהוא תוסף
            File d204 = new File(home, "מבנה204");
            wipe(d204);
            d204.mkdirs();
            File plug = new File(d204, "משהו.html"), old = new File(d204, "ישן.htm"), plain = new File(d204, "דף.html"), txt = new File(d204, "תוסף.txt");
            write(d204, "משהו.html", "<html><head><meta name=\"vfm-plugin\" content=\"שלי\"><meta name=\"vfm-plugin-version\" content=\"2\"></head></html>");
            write(d204, "ישן.htm", "<meta name=\"vfm-lab\" content=\"ישן\">");
            write(d204, "דף.html", "<html><head><title>דף רגיל</title></head><body>שלום</body></html>");
            write(d204, "תוסף.txt", "<meta name=\"vfm-plugin\" content=\"שלי\">");
            check("מבנה 204: עמוד שכתוב בו שהוא תוסף (גם בסימון הישן) — תוסף; דף html רגיל וקובץ שאינו html — לא",
                    Lab.isPlugin(plug) && Lab.isPlugin(old) && !Lab.isPlugin(plain) && !Lab.isPlugin(txt) && !Lab.isPlugin(new File(d204, "אין.html")));
            {   // מבנה 211: תוסף שבא עם התוכנה — רק הגרסה שבאה איתה נשארת; הקודמות ותוספי הבדיקה שירדו — לסל, דרך המנהל
                File r211 = new File(d204, "תוספים");
                r211.mkdirs();
                String[][] pages211 = {{"תוסף עורך 15.html", "עורך", "15"}, {"תוסף עורך 16.html", "עורך", "16"}, {"תוסף עורך 17.html", "עורך", "17"},
                        {"תוסף בדיקה 4.html", "עורך — בדיקת תפריטים", "4"}, {"תוסף אחר 2.html", "אחר", "2"}};
                for (String[] pg : pages211)
                    write(r211, pg[0], "<meta name=\"vfm-plugin\" content=\"" + pg[1] + "\"><meta name=\"vfm-plugin-version\" content=\"" + pg[2] + "\">");
                Library lib211 = Library.forTests(Collections.singletonList(built("מבנה211", d204)));
                int kept211 = Lab.keepOnly(r211, "עורך", "17", lib211), gone211 = Lab.retire(r211, lib211);
                File trash211 = new File(d204, Index.TRASH);
                String[] inTrash211 = trash211.list();
                check("מבנה 211: הגרסאות הקודמות של העורך (2) ותוסף הבדיקה (1) — לסל; העורך 17 ותוסף אחר — נשארו (" + kept211 + ", " + gone211 + ")",
                        kept211 == 2 && gone211 == 1 && new File(r211, "תוסף עורך 17.html").isFile() && new File(r211, "תוסף אחר 2.html").isFile()
                                && !new File(r211, "תוסף עורך 16.html").exists() && !new File(r211, "תוסף בדיקה 4.html").exists()
                                && inTrash211 != null && inTrash211.length >= 3);
                check("מבנה 211: פעם שנייה — אין מה להעביר", Lab.keepOnly(r211, "עורך", "17", lib211) == 0 && Lab.retire(r211, lib211) == 0);
            }
            check("מבנה 204: השם והגרסה של תוסף מחוץ לתיקיית התוספים — מתוך העמוד", Lab.page(plug, null).name.equals("שלי") && Lab.page(plug, null).ver.equals("2"));
            String ui204 = read(new File(System.getProperty("vfm.src"), "src/com/tikiyot/vfm/masachim/rechivim/Ui.java"));
            String vu204 = read(new File(System.getProperty("vfm.src"), "src/com/tikiyot/vfm/masachim/rechivim/VerbsUi.java"));
            check("מבנה 204: נגיעה בקובץ ו\"הרץ\" — אותו כלל (Ui.openPlugin), ותוסף נפתח בשניהם",
                    ui204.contains("if (c instanceof Activity && openPlugin((Activity) c, f)) return;") && vu204.contains("else if (!Ui.openPlugin(a, f)) BatchUi.run(a, f);"));
            wipe(d204);
        }
        File dl = new File(home, "הורדות-038");
        wipe(dl);
        dl.mkdirs();
        try {
            java.util.Map<String, String> vars = Parse.vars(dl, home.getAbsolutePath(), null);
            vars.put(Syntax.V_DOWNLOADS, dl.getAbsolutePath());
            Batch.Plan pl = Batch.plan("הוסף מעבדה\nהחלף מעבדה \"עורך\" \"2\"\n", dl, vars);
            check("מ0.38→מ0.40: \"הוסף/החלף מעבדה\" ירדו — שגיאה עם הסבר, ולא נבלעות בפועל \"הוסף\"", pl.errors == 2
                    && pl.steps.get(0).cmd.equals(Syntax.LAB) && Words.Lab.GONE.equals(pl.steps.get(0).error));
            eq("מ0.38: הפועל \"פתח מסך מעבדה\" מוכר", null, Verbs.parse("פתח מסך מעבדה \"עורך\"").error);
            File w = new File(home, "עורך-מעבדה-038");                       // מה שעמוד המעבדה כותב — המנוע קורא בלי שגיאות
            wipe(w);
            mk(new File(w, "מקור"), "א.pdf", 5);
            mk(new File(w, "מקור"), "ב.tmp", 5);
            mk(new File(w, "יעד"), "x.txt", 5);
            java.util.Map<String, String> wv = Parse.vars(w, w.getAbsolutePath(), null);
            Batch.Plan lp = Batch.plan("הערה ניסוי\n"
                    + "העתק \"%כאן%\\מקור\" \"%כאן%\\יעד\" /עמוק /תפוס:לדלג /תאריך:חודש /נסתרים\n"
                    + "העבר \"%כאן%\\מקור\\*.pdf\" \"%כאן%\\יעד\" /תפוס:להחליף\n"
                    + "מחק \"%כאן%\\מקור\\*.tmp\" /נסתרים /תאריך:שבוע\n"
                    + "צור תיקייה \"%כאן%\\יעד\\חדשה\"\n"
                    + "שנה \"%כאן%\\יעד\\x.txt\" \"חו״ל- 1.txt\"\n"
                    + "השווה \"%כאן%\\מקור\" \"%כאן%\\יעד\" /נסתרים\n"
                    + "ודא \"%כאן%\\יעד\" קיים\n"
                    + "המתן 1\nרענן עמוק\nהודעה סוף\n"
                    + "מחק תיקייה \"%כאן%\\יעד\\חדשה\" /עמוק\n", w, wv);
            eq("מ0.38: שורות שעמוד המעבדה כותב — בלי שגיאות במנוע", 0, lp.errors);
            File yd = new File(w, "כרטיס-י"), dd = new File(w, "הורדות-י");      // קובץ ההחזרה שנשלח למשתמש (12.11.2026) — מתוכנן בלי שגיאה
            mk(new File(yd, "YMusic"), "מעבדה עורך 1.html", 5);
            dd.mkdirs();
            java.util.Map<String, String> yv = Parse.vars(w, w.getAbsolutePath(), yd.getAbsolutePath());
            yv.put(Syntax.V_DOWNLOADS, dd.getAbsolutePath());
            Batch.Plan yp = Batch.plan("בדיקה\nהעבר \"%כרטיס%\\YMusic\" \"%הורדות%\" /עמוק /תאריך:יום /תפוס:לשאול\n", w, yv);
            check("מ0.39: קובץ ההחזרה להורדות — בלי שגיאה, וקובץ אחד", yp.errors == 0 && yp.steps.get(yp.steps.size() - 1).files.size() == 1);
            Batch.Plan yp2 = Batch.plan("בדיקה\nהעבר \"%כרטיס%\\YMusic\" \"%הורדות%\" /עמוק /תאריך:\"3 ימים\" /תפוס:לשאול\n", w, yv);
            check("מ0.39: קובץ ההחזרה — חלון של 3 ימים", yp2.errors == 0 && yp2.steps.get(yp2.steps.size() - 1).files.size() == 1);
            Batch.Plan kp = Batch.plan("צור תיקייה \"%כאן%\\ק\\תת\"\nכתוב \"%כאן%\\ק\\תת\\ב.txt\" \"x\"\nכתוב \"%כאן%\\ק\\א.txt\" \"y\"\n"
                    + "העתק \"%כאן%\\ק\" \"%כאן%\\ע\" /עמוק\n", w, wv);
            Batch.Step ks = kp.steps.get(kp.steps.size() - 1);
            check("מ0.38 (הבדיקה הכללית במכשיר): /עמוק רואה גם תת־תיקייה ששורה קודמת יצרה", kp.errors == 0 && ks.files.size() == 2);
        } catch (Exception e) {
            check("מ0.38: המעבדה — " + e, false);
        }
    }

    private static void m038() {
        File root = new File(home, "כונן-038");
        wipe(root);
        File here = new File(root, "כאן"), src = new File(root, "מקור");
        here.mkdirs();
        mk(src, "א.txt", 10);
        mk(new File(src, "תת"), "ב.txt", 10);
        mk(src, ".נסתר", 5);
        java.util.Map<String, String> vars = Parse.vars(here, root.getAbsolutePath(), null);
        String r = root.getAbsolutePath();
        String he = "הערה זו שורת הערה\n"
                + "הודעה שלום מהבדיקה\n"
                + "צור תיקייה \"" + r + "/יעד\"\n"
                + "העתק \"" + r + "/מקור\" \"" + r + "/יעד\" /עמוק /נסתרים\n"
                + "שנה \"" + r + "/מקור/א.txt\" \"חדש.txt\"\n"
                + "העבר \"" + r + "/מקור/חדש.txt\" \"" + r + "/יעד\" /תפוס:להחליף\n"
                + "השווה \"" + r + "/מקור\" \"" + r + "/יעד\"\n"
                + "מחק \"" + r + "/יעד/חדש.txt\"\n"
                + "מחק תיקייה \"" + r + "/יעד\" /עמוק\n";
        String en = "REM x\nECHO שלום מהבדיקה\nMD \"" + r + "/יעד\"\nCOPY \"" + r + "/מקור\" \"" + r + "/יעד\" /S /A:H\n"
                + "REN \"" + r + "/מקור/א.txt\" \"חדש.txt\"\nMOVE \"" + r + "/מקור/חדש.txt\" \"" + r + "/יעד\" /תפוס:להחליף\n"
                + "COMP \"" + r + "/מקור\" \"" + r + "/יעד\"\nDEL \"" + r + "/יעד/חדש.txt\"\nRD \"" + r + "/יעד\" /S\n";
        Batch.Plan ph = Batch.plan(he, here, vars), pe = Batch.plan(en, here, vars);
        eq("מ0.38: קובץ שכולו בעברית — בלי שגיאות", 0, ph.errors);
        eq("מ0.38: אותו מספר שורות כמו באנגלית", pe.steps.size(), ph.steps.size());
        boolean same = ph.steps.size() == pe.steps.size();
        for (int i = 0; same && i < ph.steps.size(); i++) {
            Batch.Step a = ph.steps.get(i), b2 = pe.steps.get(i);
            same = a.cmd.equals(b2.cmd) && a.files.equals(b2.files) && a.dirs.equals(b2.dirs) && a.recursive == b2.recursive && a.hidden == b2.hidden
                    && String.valueOf(a.describe).equals(String.valueOf(b2.describe));
        }
        check("מ0.38: כל שורה בעברית מתוכננת בדיוק כמו המקבילה באנגלית (פקודה, קבצים, /עמוק, /נסתרים)", same);
        check("מ0.38: ההודעה — בלי המילה \"הודעה\"", ph.echoes.size() == 1 && ph.echoes.get(0).equals("שלום מהבדיקה"));
        check("מ0.38: בדוח נשארת השורה כמו שנכתבה, בעברית", ph.steps.get(1).text.startsWith("צור תיקייה"));
        eq("מ0.38: מתג עברי בתוך מרכאות — לא נוגעים", "MD \"x /עמוק\"", Commands.normal("צור תיקייה \"x /עמוק\""));
        eq("מ0.38: \"צור רשימה\" נשארת פועל", "צור רשימה א", Commands.normal("צור רשימה א"));
        check("מ0.38: \"צור תיקייה\" — MD; שם עברי לכל פקודה", Commands.normal("צור תיקייה \"x\"").startsWith("MD ")
                && Commands.word(Commands.get("COPY")).equals("העתק") && Commands.word(Commands.get("RD")).equals("מחק תיקייה") && Commands.get("העבר").name.equals("MOVE"));
        eq("מ0.38: העורך מציג בעברית", "העתק \"a /S\" b /עמוק /נסתרים /Y", Ivrit.hebrew("COPY \"a /S\" b /s /A:H /Y"));
        eq("מ0.38: RD ← מחק תיקייה", "מחק תיקייה x /עמוק", Ivrit.hebrew("RD x /S"));
        eq("מ0.38: ECHO — הטקסט לא נגוע", "הודעה a /S b", Ivrit.hebrew("ECHO a /S b"));
        eq("מ0.38: שורת רשימות — כמו שהיא", "COPY ידנית \"א\" קובץ \"x.txt\"", Ivrit.hebrew("COPY ידנית \"א\" קובץ \"x.txt\""));
        eq("מ0.38: פועל, הערה, שורה ריקה — כמו שהן", "ודא \"x\" קיים\n:: REM\n\nהעבר a b", Ivrit.hebrewText("ודא \"x\" קיים\n:: REM\n\nMOVE a b"));
        Batch.Plan pt = Batch.plan(Ivrit.hebrewText(en), here, vars);
        boolean same2 = pt.errors == pe.errors && pt.steps.size() == pe.steps.size();
        for (int i = 0; same2 && i < pt.steps.size(); i++)
            same2 = pt.steps.get(i).cmd.equals(pe.steps.get(i).cmd) && pt.steps.get(i).files.equals(pe.steps.get(i).files) && pt.steps.get(i).recursive == pe.steps.get(i).recursive;
        check("מ0.38: קובץ שהעורך תרגם — מתוכנן בדיוק כמו המקור", same2);
        check("מ0.38: \"בטל\" ו\"הכפל\" — לרשימות", Commands.canonical("בטל").equals("RD") && Commands.canonical("הכפל").equals("COPY"));
    }

    /** מ0.40: המעבדה ← התוספים (התיקייה עוברת, השמות הישנים עובדים); וכל שורה שהעורך האינטראקטיבי (גרסה 4) כותב — במנוע. */
    private static void m040() {
        String[] a = Lab.nameOf("<meta name=\"vfm-plugin\" content=\"עורך אינטראקטיבי\"><meta name=\"vfm-plugin-version\" content=\"4\">", "x.html");
        check("מ0.40: שם וגרסה של תוסף — מתוך העמוד (vfm-plugin)", a[0].equals("עורך אינטראקטיבי") && a[1].equals("4"));
        a = Lab.nameOf("<p>", "תוסף עורך 2.html");
        check("מ0.40: בלי meta — משם הקובץ שמתחיל ב\"תוסף\"", a[0].equals("עורך") && a[1].equals("2"));
        check("מ0.40: הפקודות שירדו — מזוהות (כדי לומר מה במקומן)", Batch.isLabLine("הוסף תוסף") && Batch.isLabLine("החלף תוסף \"א\" \"1\"") && Batch.isLabLine("הוסף מעבדה")
                && !Batch.isLabLine("הוסף מהכונן"));
        Verbs.Cmd pc = Verbs.parse("פתח תוסף \"עורך אינטראקטיבי\""), pe = Verbs.parse("פתח תוסף"), ps = Verbs.parse("פתח מסך תוספים");
        check("מ0.40: פתח תוסף \"<שם>\" · פתח תוסף · פתח מסך תוספים", pc.error == null && Verbs.K_SCREEN.equals(pc.kind) && Verbs.S_PLUGINS.equals(pc.arg)
                && pc.cond.equals("עורך אינטראקטיבי") && pc.ver.isEmpty() && pe.error == null && pe.cond.isEmpty() && ps.error == null && Verbs.S_PLUGINS.equals(ps.arg));
        Verbs.Cmd no = Verbs.parse("צור רשימה חכמה \"עמודי תוספים\" תוסף סוג:html /בלי-לפתוח מיון:שם");
        check("מ0.40: /בלי-לפתוח — יורד מהתנאי ומהמיון", no.error == null && no.noOpen && no.cond.equals("תוסף סוג:html") && no.sort.equals("שם"));
        check("מ0.40: /בלי-לפתוח — רק בפועל שכותב רשימה", Verbs.parse("פתח רשימה \"א\" /בלי-לפתוח").error != null
                && Verbs.parse("צור רשימה \"א/ב\" /בלי-לפתוח").error == null);
        Verbs.Cmd pv = Verbs.parse("פתח תוסף \"עורך אינטראקטיבי\" \"4\""), pu = Verbs.parse("פתח תוסף עורך אינטראקטיבי");
        check("מ0.40: פתח תוסף \"<שם>\" \"<גרסה>\"; ובלי מרכאות — כל השאר הוא השם", pv.error == null && pv.cond.equals("עורך אינטראקטיבי")
                && pv.ver.equals("4") && pu.cond.equals("עורך אינטראקטיבי") && pu.ver.isEmpty());
        File root = Lab.root(), old = new File(Paths.dbData(), Lab.OLD_DIR);
        wipe(root);
        wipe(old);
        try {
            File oe = new File(old, "עורך");
            oe.mkdirs();
            Files.writeAtomic(new File(oe, "3.html"), "<p>");
            Lab.recheck();                                                  // מבנה 145: המעבר נבדק פעם אחת בהפעלה — כאן, כמו הפעלה חדשה
            File r = Lab.root();
            check("מ0.40: התיקייה \"מעבדה\" עוברת ל\"תוספים\" (פעם אחת), והעמודים — ישר בה", r.getName().equals(Lab.DIR)
                    && new File(r, "תוסף עורך 3.html").isFile() && !new File(r, "עורך").exists() && !old.exists());
            check("מ0.40: אחרי המעבר — אותו ניסוי, עכשיו תוסף (התיקייה = השם, הקובץ = הגרסה)", "[עורך]".equals(Lab.names(r).toString())
                    && "3".equals(Lab.newest(r, "עורך").ver));
            File oe2 = new File(old, "ישן");                                        // "תוספים" כבר קיימת (המריץ יצר אותה) — עובר פריט פריט
            oe2.mkdirs();
            Files.writeAtomic(new File(oe2, "1.html"), "<p>");
            Lab.recheck();                                                  // מבנה 145: המעבר נבדק פעם אחת בהפעלה — כאן, כמו הפעלה חדשה
            r = Lab.root();
            check("מ0.40: \"מעבדה\" שנשארה, כש\"תוספים\" כבר קיימת — מה שבה עובר, והיא נמחקת", new File(r, "תוסף ישן 1.html").isFile() && !old.exists());
            // המריץ העביר מההורדות את אותו עמוד לפני שהתיקייה הישנה עברה — לא נוצר "(2)"; תוכן אחר באותו שם — כן
            File o3 = new File(old, "כפול");
            o3.mkdirs();
            Files.writeAtomic(new File(o3, "5.html"), "<p>חמש</p>");
            Files.writeAtomic(new File(o3, "6.html"), "<p>שש ישן</p>");
            Files.writeAtomic(new File(r, "תוסף כפול 5.html"), "<p>חמש</p>");
            Files.writeAtomic(new File(r, "תוסף כפול 6.html"), "<p>שש חדש</p>");
            Lab.recheck();                                                  // מבנה 145: המעבר נבדק פעם אחת בהפעלה — כאן, כמו הפעלה חדשה
            r = Lab.root();
            check("מ0.40: עמוד ישן שזהה לו כבר בתיקייה — לא מוכפל; שונה באותו שם — \"(2)\"", !new File(r, "תוסף כפול 5 (2).html").exists()
                    && new File(r, "תוסף כפול 6 (2).html").isFile() && Lab.versions(r, "כפול").size() == 3 && !old.exists());
        } catch (Exception e) {
            check("מ0.40: מעבר התיקייה — " + e, false);
        }
        File w = new File(home, "עורך-040");
        wipe(w);
        mk(new File(w, "מקור"), "a.pdf", 5);
        mk(new File(w, "מקור"), "b.tmp", 5);
        mk(new File(w, "יעד"), "x.txt", 5);
        try {
            Files.writeAtomic(new File(w, "ק.vbat"), "הודעה מתוך כלול\n");
            Files.writeAtomic(new File(w, "תוסף עורך אינטראקטיבי 4.html"), "<meta name=\"vfm-lab\" content=\"עורך אינטראקטיבי\"><meta name=\"vfm-plugin\" content=\"עורך אינטראקטיבי\"><meta name=\"vfm-lab-version\" content=\"4\">");
        } catch (Exception e) {
            check("מ0.40: הכנה — " + e, false);
        }
        java.util.Map<String, String> wv = Parse.vars(w, w.getAbsolutePath(), null);
        // השורות כמו שהעורך כותב אותן (מודל העמוד, lab-pages/מעבדה עורך 4.html — אותו מבנה בעברית ובאנגלית)
        String he = "הערה ניסוי\n"
                + "העתק \"%כאן%\\מקור\" \"%כאן%\\יעד\" /עמוק /תפוס:לדלג /תאריך:חודש /נסתרים\n"
                + "העתק \"%כאן%\\מקור\\a.pdf\" \"%כאן%\\יעד\" /תפוס:להחליף /יומן:%כאן%\\j.txt\n"
                + "העבר \"%כאן%\\מקור\\*.pdf\" \"%כאן%\\יעד\" /תפוס:להחליף\n"
                + "מחק \"%כאן%\\מקור\\*.tmp\" /נסתרים /תאריך:\"3 ימים\" /גודל:>1\n"
                + "צור תיקייה \"%כאן%\\יעד\\חדשה\"\n"
                + "שנה \"%כאן%\\יעד\\x.txt\" \"חו״ל- 1.txt\"\n"
                + "השווה \"%כאן%\\מקור\" \"%כאן%\\יעד\" /נסתרים\n"
                + "ודא \"%כאן%\\יעד\" קיים\n"
                + "ודא \"%כאן%\\אין\" חסר\n"
                + "כתוב \"%כאן%\\ב.txt\" \"שלום\"\n"
                + "כתוב \"%כאן%\\ב.txt\" \" עולם\" /הוסף\n"
                + "כתוב \"%כאן%\\ריק.txt\"\n"
                + "ודא \"%כאן%\\ב.txt\" מכיל \"שלום\"\n"
                + "ודא \"%כאן%\\מקור\" /קבצים:1\n"
                + "דוח \"%כאן%\\יומן.txt\"\n"
                + "המתן 0\nרענן עמוק\nרענן\nהודעה סוף\n"
                + "המשך (5)\n"
                + "כלול \"%כאן%\\ק.vbat\"\n"
                + "חזור 2 /עד:30שניות\nהודעה סבב %סבב%\nסוף\n"
                + "ודא שגיאה\nהעתק \"%כאן%\\אין-כזה\" \"%כאן%\\יעד\"\n"
                + "הפעל \"%כאן%\\יעד\"\n"
                + "מחק תיקייה \"%כאן%\\יעד\\חדשה\" /עמוק\n"
                + "פתח מסך הבית\nפתח מסך קבצים פיזיים \"%כאן%\"\nפתח תוסף \"עורך אינטראקטיבי\"\n"
                + "פתח רשימה \"א/ב\" מיון:שם\nפתח רשימה חכמה \"חדשים\"\nפתח תיקייה \"%כאן%\"\nפתח תפריט \"שמור לתוכנה\"\n"
                + "מצא סוג:ספרים תאריך:שבוע\nחזור\nהרץ \"%כאן%\\ק.vbat\"\nהוסף מהכונן\nצור פקודה\n"
                + "צור רשימה \"א/ב\"\nצור רשימה\nצור רשימה חכמה \"חדשים\" סוג:ספרים מיון:שם\nעדכן רשימה חכמה \"חדשים\" סוג:ספרים\n"
                + "צור רעיון \"לקנות חלב\" \"פנקס/קניות\"\nצור כותרת \"פרק א\"\n"
                + "צלמית 🧪\nשם בדיקה\nלמה כי\nמדידה /יומן:%כאן%\\m.txt\nבדיקה\n";
        String en = "REM ניסוי\n"
                + "COPY \"%כאן%\\מקור\" \"%כאן%\\יעד\" /S /תפוס:לדלג /תאריך:חודש /A:H\n"
                + "COPY \"%כאן%\\מקור\\a.pdf\" \"%כאן%\\יעד\" /Y /יומן:%כאן%\\j.txt\n"
                + "MOVE \"%כאן%\\מקור\\*.pdf\" \"%כאן%\\יעד\" /Y\n"
                + "DEL \"%כאן%\\מקור\\*.tmp\" /A:H /תאריך:\"3 ימים\" /גודל:>1\n"
                + "MD \"%כאן%\\יעד\\חדשה\"\n"
                + "REN \"%כאן%\\יעד\\x.txt\" \"חו״ל- 1.txt\"\n"
                + "COMP \"%כאן%\\מקור\" \"%כאן%\\יעד\" /A:H\n"
                + "VERIFY \"%כאן%\\יעד\" קיים\n"
                + "WRITE \"%כאן%\\ב.txt\" \"שלום\"\nWRITE \"%כאן%\\ב.txt\" \" עולם\" /הוסף\n"
                + "VERIFY \"%כאן%\\ב.txt\" מכיל \"שלום\"\n"
                + "REPORT \"%כאן%\\יומן.txt\"\nWAIT 0\nREFRESH עמוק\nECHO סוף\nCONTINUE (5)\nCALL \"%כאן%\\ק.vbat\"\n"
                + "VERIFY שגיאה\nCOPY \"%כאן%\\אין-כזה\" \"%כאן%\\יעד\"\n"
                + "START \"%כאן%\\יעד\"\n"
                + "RD \"%כאן%\\יעד\\חדשה\" /S\n";
        Batch.Plan ph = Batch.plan(he, w, wv), pn = Batch.plan(en, w, wv);
        StringBuilder bad = new StringBuilder();
        for (Batch.Step x : ph.steps) if (x.error != null) bad.append(" | ").append(x.text).append(" ← ").append(x.error);
        for (Batch.Step x : pn.steps) if (x.error != null) bad.append(" | ").append(x.text).append(" ← ").append(x.error);
        eq("מ0.40: השורות שהעורך כותב בעברית — בלי שגיאות במנוע" + bad, 0, ph.errors);
        eq("מ0.40: ובאנגלית — בלי שגיאות במנוע", 0, pn.errors);
        check("מ0.40: בעברית ובאנגלית — אותו מספר צעדים לחלק המשותף", pn.steps.size() > 15);
        // שורות עם רשימות: במחשב אין מסד — התחביר (מילות הסוג) והפקודה
        String[] lists = {"העתק קבצי ידנית \"בדיקה\" \"%כאן%\\יעד\" /עמוק /תפוס:לשאול", "הכפל ידנית \"א\" ידנית \"ב\"", "COPY ידנית \"א\" ידנית \"ב\"",
                "העתק ידנית \"א\" קובץ \"%כאן%\\r.txt\"", "העתק קובץ \"%כאן%\\r.txt\" ידנית \"ב\"", "העתק \"%כאן%\\מקור\" ידנית \"ב\"",
                "העתק קבצי חכמה \"חדשים\" ידנית \"ב\"", "העתק תוכן ידנית \"א\" ידנית \"ב\"", "העבר ידנית \"א\" ידנית \"ב\"", "MOVE ידנית \"א\" ידנית \"ב\"",
                "מחק קבצי ידנית \"א\"", "DEL קבצי חכמה \"חדשים\"", "בטל ידנית \"א/ב\"", "RD ידנית \"א/ב\"", "בטל חכמה \"חדשים\"", "בטל תוכן ידנית \"א\"",
                "קשר ידנית \"א\" ידנית \"ב\"", "LINK חכמה \"ח\" ידנית \"ב\"", "שנה ידנית \"א\" \"ב\"", "REN חכמה \"א\" \"ב\"",
                "השווה ידנית \"א\" ידנית \"ב\"", "השווה חכמה \"א\" חכמה \"ב\" /תנאים", "ודא ידנית \"א\" קיימת /פריטים:1", "VERIFY חכמה \"א\" חסרה /רשימות:0"};
        for (String l : lists) {
            String n = Commands.normal(l);
            java.util.List<String> sw = new java.util.ArrayList<String>();
            ListCmd.Parsed lp = ListCmd.parse(n, sw);
            String c = Commands.canonical(Parse.tokens(n).get(0));
            boolean known = java.util.Arrays.asList("COPY", "MOVE", "DEL", "RD", "REN", "COMP", Syntax.LINK, Syntax.VERIFY).contains(c);
            check("מ0.40: שורת רשימות שהעורך כותב — " + l, lp != null && lp.error == null && lp.anyList() && known);
        }
        m040runner();
        Batch.Plan cm = Batch.plan("השווה /רשימה:\"א/ב\" \"%כאן%\\יעד\"\nאופן \"בדיקות מערכת\"\nMODE \"בדיקות מערכת\"\n", w, wv);
        check("מ0.40: השווה רשימה מול תיקייה (/רשימה:), ואופן — מזוהים", cm.steps.size() == 3 && Syntax.MODE.equals(cm.steps.get(1).cmd) && Syntax.MODE.equals(cm.steps.get(2).cmd));
    }

    /** מ0.40: המריץ "עורך אינטראקטיבי.vbat" — מעביר מההורדות לתוספים (זהה — נשאר; שם תפוס — בשם אחר) ופותח את החדש. */
    private static void m040runner() {
        File ext = Paths.dbData().getParentFile(), dl = new File(home, "הורדות-040"), root = Lab.root();
        wipe(dl);
        wipe(root);
        dl.mkdirs();
        try {
            String v4 = "<meta name=\"vfm-plugin\" content=\"עורך אינטראקטיבי\"><meta name=\"vfm-plugin-version\" content=\"4\">ארבע";
            String v6 = "<meta name=\"vfm-plugin\" content=\"עורך אינטראקטיבי\"><meta name=\"vfm-plugin-version\" content=\"6\">שש";
            Files.writeAtomic(new File(dl, "תוסף עורך אינטראקטיבי 6.html"), v6);
            Files.writeAtomic(new File(dl, "שמור.html"), "<p>תוסף תזונה</p>");                 // html אחר — לא זז
            java.util.Map<String, String> iv = Parse.vars(dl, ext.getAbsolutePath(), null);
            iv.put(Syntax.V_DOWNLOADS, dl.getAbsolutePath());
            String real = Files.readUtf8(new File(System.getProperty("vfm.src"), "../lab-pages/עורך אינטראקטיבי.vbat"));
            Batch.Plan rp = Batch.plan(real, dl, iv);                           // במחשב אין מסד: רק שורת הרשימה — "אין מסד"
            int noDb = 0;
            for (Batch.Step x : rp.steps) if (Words.DbCmd.NO_DB.equals(x.error)) noDb++;
            check("מ0.40: המריץ (רענן, חכמה /בלי-לפתוח, העבר קבצי חכמה) — מובן; פותח רק את העורך", rp.errors == noDb && noDb == 1
                    && rp.opens.size() == 1 && rp.opens.get(0)[0].contains("עורך אינטראקטיבי"));
            // אותה העברה — מההורדות (במחשב אין רשימה חכמה): מה שהמריץ עושה בכל עמוד
            String inst = real.replace("העבר קבצי חכמה \"עמודי תוספים\"", "העבר \"%הורדות%\\*.html\"")
                    .replaceAll("(?m)^צור רשימה חכמה.*\n", "");
            Index ix = new Index("040", "040", ext.getAbsolutePath());
            Library lib = Library.forTests(new ArrayList<Index>(java.util.Collections.singletonList(ix)));
            Batch.Plan p1 = Batch.plan(inst, dl, iv, lib);
            Batch.Step mv = p1.steps.get(p1.steps.size() - 2);
            StringBuilder e1 = new StringBuilder();
            for (Batch.Step x : p1.steps) e1.append(" | ").append(x.text).append(" ← ").append(x.error != null ? x.error : x.describe);
            check("מ0.40: המריץ — בלי שגיאות; מעביר רק את עמוד התוסף (/מכיל:), ופותח את העורך בסוף" + e1, p1.errors == 0 && mv.files.size() == 1
                    && p1.opens.size() == 1 && p1.opens.get(0)[0].contains("עורך אינטראקטיבי"));
            Batch.run(p1, lib, null, null);
            File in6 = new File(root, "תוסף עורך אינטראקטיבי 6.html");
            check("מ0.40: הועבר לתיקיית התוספים; ה־html האחר נשאר", in6.isFile() && !new File(dl, in6.getName()).exists()
                    && new File(dl, "שמור.html").isFile());
            eq("מ0.40: \"פתח תוסף\" — החדש", "6", Lab.newest(root, "עורך אינטראקטיבי").ver);
            Files.writeAtomic(new File(dl, "תוסף עורך אינטראקטיבי 6.html"), v6);              // אותו עמוד שוב — זהה בחתימה
            Batch.Plan p2 = Batch.plan(inst, dl, iv, lib);
            Batch.Step m2 = p2.steps.get(p2.steps.size() - 2);
            check("מ0.40: זהה כבר בתוספים — לא זז, ולא שגיאה (" + m2.describe + ")", p2.errors == 0 && m2.noop && m2.same == 1);
            Batch.run(p2, lib, null, null);
            check("מ0.40: ...והוא נשאר בהורדות, ובתוספים — אחד", new File(dl, in6.getName()).isFile() && Lab.versions(root, "עורך אינטראקטיבי").size() == 1);
            Files.writeAtomic(new File(dl, "תוסף עורך אינטראקטיבי 6.html"), v6 + " תיקון");    // אותו שם, תוכן אחר — בשם אחר
            File v4f = new File(dl, "תוסף עורך אינטראקטיבי 4.html");
            Files.writeAtomic(v4f, v4);
            v4f.setLastModified(System.currentTimeMillis() - 3600000);
            Batch.Plan p3 = Batch.plan(inst, dl, iv, lib);
            check("מ0.40: שם תפוס ותוכן אחר — עובר; וגם הגרסה הישנה", p3.errors == 0 && p3.steps.get(p3.steps.size() - 2).files.size() == 2);
            Batch.run(p3, lib, null, null);
            File in62 = new File(root, "תוסף עורך אינטראקטיבי 6 (2).html");
            check("מ0.40: ...בשם \"(2)\", בלי לשאול; המקור הקודם לא נגע", in62.isFile() && Files.readUtf8(in62).endsWith("תיקון")
                    && Files.readUtf8(in6).endsWith("שש"));
            Lab.Page nw = Lab.newest(root, "עורך אינטראקטיבי");
            check("מ0.40: שלוש גרסאות; החדש לפי התאריך — 6 המתוקן; ומעליו אין גבוהה יותר", Lab.versions(root, "עורך אינטראקטיבי").size() == 3
                    && nw.file.equals(in62) && Lab.higher(root, nw) == null);
            Lab.Page old4 = Lab.version(root, "עורך אינטראקטיבי", "4");
            check("מ0.40: גרסה 4 בפקודה; ובה — \"יש גרסה 6\"", old4 != null && "6".equals(Lab.higher(root, old4)));
            check("מ0.40: סדר גרסאות — מספרים כמספרים", Lab.compareVer("10", "9") > 0 && Lab.compareVer("5.10", "5.2") > 0
                    && Lab.compareVer("4", "4") == 0 && Lab.compareVer("6", "6b") < 0);
            eq("מ0.40: בהורדות — רק מה שלא זז (ה־html האחר)", "[שמור.html]", java.util.Arrays.toString(dl.list()));
        } catch (Exception e) {
            check("מ0.40: המריץ — " + e, false);
        }
    }

    /** מ0.40: "העבר" — אל התיקייה שהקובץ כבר בה (כלום); /תפוס:לשנות-שם (זהה — נשאר, שם תפוס — שם אחר); /מכיל:. */
    private static void m040move() {
        File r = new File(home, "כונן-040ב");
        wipe(r);
        File a = new File(r, "א"), b = new File(r, "ב");
        mk(a, "x.txt", 5);
        mk(b, "y.txt", 5);
        try { Files.writeAtomic(new File(a, "x.txt"), "איקס"); } catch (Exception e) { check("מ0.40: הכנה — " + e, false); }
        java.util.Map<String, String> v = Parse.vars(r, r.getAbsolutePath(), null);
        Index ix = new Index("040ב", "040ב", r.getAbsolutePath());
        ix.scan(null);
        Library lib = Library.forTests(new ArrayList<Index>(java.util.Collections.singletonList(ix)));
        try {
            Batch.Plan p = Batch.plan("העבר \"%כאן%\\א\\x.txt\" \"%כאן%\\א\"\n", r, v);
            Batch.Step s = p.steps.get(0);
            check("מ0.40 (באג): העברה אל התיקייה שהקובץ כבר בה — \"כבר במקום\", לא \"קובץ אחד\"", p.errors == 0 && s.noop && s.inPlace == 1 && s.files.isEmpty());
            String rep = Batch.run(p, lib, null, null).toString();
            check("מ0.40 (באג): ...ובהרצה — לא כישלון ולא שם חדש", !p.trouble && new File(a, "x.txt").isFile() && a.list().length == 1 && rep.contains("כבר במקום"));
            Batch.Plan cp = Batch.plan("העתק \"%כאן%\\א\\x.txt\" \"%כאן%\\א\"\n", r, v);
            check("מ0.40: העתקה לאותה תיקייה — כמו קודם, עותק בשם חדש", cp.errors == 0 && cp.steps.get(0).files.size() == 1);
            Files.writeAtomic(new File(a, "z.txt"), "שונה");
            Files.writeAtomic(new File(b, "z.txt"), "אחר!");
            Files.writeAtomic(new File(a, "w.txt"), "זהה");
            Files.writeAtomic(new File(b, "w-עותק.txt"), "זהה");
            Batch.Plan rn = Batch.plan("העבר \"%כאן%\\א\\*.txt\" \"%כאן%\\ב\" /תפוס:לשנות-שם\n", r, v);
            Batch.Step t = rn.steps.get(0);
            check("מ0.40: לשנות-שם — זהה (גם בשם אחר) נשאר; השאר עוברים (" + t.describe + ")", rn.errors == 0 && t.same == 1 && t.files.size() == 2);
            Batch.run(rn, lib, null, null);
            check("מ0.40: לשנות-שם — שם תפוס ותוכן אחר: \"(2)\"; הזהה נשאר במקור", new File(b, "z (2).txt").isFile() && Files.readUtf8(new File(b, "z.txt")).equals("אחר!")
                    && new File(a, "w.txt").isFile() && new File(b, "x.txt").isFile() && !new File(a, "x.txt").exists());
            Files.writeAtomic(new File(a, "q1.txt"), "כפול");
            Files.writeAtomic(new File(a, "q2.txt"), "כפול");
            Batch.Step two = Batch.plan("העתק \"%כאן%\\א\\q*.txt\" \"%כאן%\\ב\" /תפוס:לשנות-שם\n", r, v).steps.get(0);
            check("מ0.40: לשנות-שם — שני קבצים זהים זה לזה: רק הראשון עובר", two.files.size() == 1 && two.same == 1);
            Batch.Step h = Batch.plan("העתק \"%כאן%\\א\\*.txt\" \"%כאן%\\ב\" /מכיל:כפול\n", r, v).steps.get(0);
            check("מ0.40: /מכיל: — רק הקבצים שהטקסט כתוב בהם (" + h.describe + h.error + ")", h.files.size() == 2 && h.describe.contains("עברו את הסינון"));
            // מבנה 112: שם של תיקייה הוא התיקייה עצמה, וסינון חל על קבצים — לכן עם סינון כותבים "א\*"
            Batch.Plan ed = Batch.plan("העתק \"%כאן%\\א\\*\" \"%כאן%\\ב\" /תפוס:לשנות-שם /מכיל:\"שתי מילים\"\nמחק \"%כאן%\\א\\*.txt\" /מכיל:זבל\n"
                    + "COPY \"%כאן%\\א\\*\" \"%כאן%\\ב\" /תפוס:לשנות-שם /מכיל:x\n", r, v);
            File a112 = new File(r, "מקור112"), b112 = new File(r, "יעד112");
            new File(a112, "תת").mkdirs();
            b112.mkdirs();
            Files.writeAtomic(new File(a112, "א.txt"), "1");
            Files.writeAtomic(new File(a112, "תת/ב.txt"), "2");
            Batch.Plan w112 = Batch.plan("העתק \"%כאן%\\מקור112\" \"%כאן%\\יעד112\"\n", r, v);
            if (w112.errors == 0) Batch.run(w112, lib, null, null);
            check("מבנה 112 (ה7): העתקה של שם תיקייה — התיקייה עצמה, עם כל מה שבתוכה, כמו במסך וכמו בהעברה (" + w112.errorText() + ")",
                    w112.errors == 0 && w112.steps.get(0).dirs.size() == 1 && "1".equals(read(new File(b112, "מקור112/א.txt")))
                    && "2".equals(read(new File(b112, "מקור112/תת/ב.txt"))) && !new File(b112, "א.txt").exists() && new File(a112, "א.txt").isFile());
            Batch.Plan in112 = Batch.plan("העתק \"%כאן%\\מקור112\\*\" \"%כאן%\\יעד112\"\n", r, v);
            check("מבנה 112: \"תיקייה\\*\" — רק הקבצים שבתוכה; ושם תיקייה עם סינון — שגיאה שאומרת איך",
                    in112.errors == 0 && in112.steps.get(0).dirs.isEmpty() && in112.steps.get(0).files.size() == 1
                    && Batch.plan("העתק \"%כאן%\\מקור112\" \"%כאן%\\יעד112\" /מכיל:x\n", r, v).errors == 1);
            eq("מ0.40: השורות שהעורך (6) כותב עם האפשרויות החדשות — בלי שגיאות", 0, ed.errors);
            eq("מ0.40: /מכיל: — הבחירה של העורך (בלי מרכאות כפולות בטקסט)", 0, ed.steps.get(0).files.size());
            eq("מ0.40: /מכיל: ריק — שגיאה", 1, Batch.plan("העתק \"%כאן%\\א\\*.txt\" \"%כאן%\\ב\" /מכיל:\n", r, v).errors);
            eq("מ0.40: /תפוס: לא מוכר — שגיאה שמונה גם את לשנות-שם", true,
                    String.valueOf(Batch.plan("העתק \"%כאן%\\א\\*.txt\" \"%כאן%\\ב\" /תפוס:בלה\n", r, v).steps.get(0).error).contains(Syntax.TAKEN_RENAME));
            File fresh = new File(a, "חדש-מהורדה.txt");                       // "רענן" כבר בהכנה — השורות שאחריו רואות קובץ חדש
            Files.writeAtomic(fresh, "חדש");
            new File(a, ".").setLastModified(System.currentTimeMillis() + 5000);
            a.setLastModified(System.currentTimeMillis() + 5000);
            boolean before = lib.find(fresh.getAbsolutePath()) != null;
            Batch.Plan rf = Batch.plan("רענן\n", r, v, lib);
            check("מ0.40: \"רענן\" — כבר בהכנה: הקובץ החדש באינדקס לפני השורה הבאה (" + rf.steps.get(0).describe + ")", !before
                    && lib.find(fresh.getAbsolutePath()) != null && rf.errors == 0);
            // ממצא 41: מתג שלא שייך לפקודה — שגיאה, ולא בליעה בשקט
            String[][] sw = {
                    {"צור תיקייה \"%כאן%\\חדשה\" /עמוק", "1"}, {"שנה \"%כאן%\\ב\\x.txt\" \"y.txt\" /נסתרים", "1"},
                    {"מחק \"%כאן%\\ב\\*.txt\" /תפוס:לדלג", "1"}, {"השווה \"%כאן%\\א\" \"%כאן%\\ב\" /עמוק", "1"},
                    {"העתק \"%כאן%\\א\" \"%כאן%\\ב\" /עד:20%", "1"}, {"מחק תיקייה \"%כאן%\\א\" /S /Q", "0"},
                    {"העתק \"%כאן%\\א\" \"%כאן%\\ב\" /עמוק /נסתרים /תאריך:שנה /Q", "0"}, {"הפעל \"%כאן%\\א\\*.txt\" /נסתרים", "0"},
                    {"השווה \"%כאן%\\א\" \"%כאן%\\ב\"\nהעתק %רק-במקור% \"%כאן%\\ב\" /עד:100%", "0"},
                    {"השווה \"%כאן%\\א\" \"%כאן%\\ב\"\nהעתק %רק-במקור% \"%כאן%\\ב\" /תאריך:שבוע", "1"}};
            for (String[] x : sw) {
                Batch.Plan sp = Batch.plan(x[0] + "\n", r, v);
                String why = "";
                for (Batch.Step st : sp.steps) if (st.error != null) why = " ← " + st.error;
                eq("מ0.40 (41): " + x[0].replace("\n", " · ") + why, Integer.parseInt(x[1]), sp.errors);
            }
            String[][] lsw = {{"העתק ידנית \"א\" ידנית \"ב\" /עמוק", "לא שייך"}, {"בטל ידנית \"א\" /תפוס:לדלג", "לא שייך"},
                    {"העתק קבצי ידנית \"א\" \"%כאן%\\ב\" /יומן:\"%כאן%\\j.txt\"", "לא שייך"}, {"קשר ידנית \"א\" ידנית \"ב\" /מכיל:x", "סינון"},
                    {"העתק קבצי ידנית \"א\" \"%כאן%\\ב\" /עמוק /תפוס:לשנות-שם /מכיל:\"שתי מילים\"", Words.DbCmd.NO_DB},
                    {"העבר קבצי חכמה \"ח\" \"%כאן%\\ב\" /תפוס:לדלג", Words.DbCmd.NO_DB}, {"העתק \"%כאן%\\א\\*.txt\" ידנית \"ב\" /מכיל:x", Words.DbCmd.NO_DB},
                    {"השווה חכמה \"א\" חכמה \"ב\" /תנאים", Words.DbCmd.NO_DB}};
            for (String[] x : lsw) {
                String er = String.valueOf(Batch.plan(x[0] + "\n", r, v).steps.get(0).error);
                check("מ0.40 (41), שורת רשימה: " + x[0] + " ← " + er, er.contains(x[1]));
            }
            eq("מ0.40: דרך של רשימה — גם עם \\", "[א, ב, ג]", Names.parsePath("א\\ב / ג").toString());
            eq("מ0.40: פתח רשימה \"א\\ב\" — אותה רשימה כמו \"א/ב\"", Names.parsePath(Verbs.parse("פתח רשימה \"א/ב\"").arg).toString(),
                    Names.parsePath(Verbs.parse("פתח רשימה \"א\\ב\"").arg).toString());
            File big = new File(r, "גדול.bin");
            byte[] data = new byte[200000];
            byte[] word = "סוף-הקובץ".getBytes("UTF-8");
            System.arraycopy(word, 0, data, data.length - word.length, word.length);
            java.io.FileOutputStream o = new java.io.FileOutputStream(big);
            o.write(data);
            o.close();
            check("מ0.40: חיפוש בקובץ — גם מעבר לגבול של קטע קריאה, ולא מוצא מה שאין", Files.contains(big, word) && !Files.contains(big, "אין".getBytes("UTF-8")));
        } catch (Exception e) {
            check("מ0.40: העבר — " + e, false);
        }
    }

    /** מ0.40: החלק החדש של הבדיקה הכללית (assets/tests/klali.vbat) — השורות בכונן, בתכנון ובהרצה אמיתית. */
    private static void m040general() {
        File r = new File(home, "כונן-040ג");
        wipe(r);
        r.mkdirs();
        try {
            File tdir = new File(new File(System.getProperty("vfm.src")).getParentFile(), "lab-pages/avchon-tests");   // מ0.43: רק בתוסף האבחון
            String all = Files.readUtf8(new File(tdir, "klali.vbat"));
            int a = all.indexOf(":: מ0.40"), b = all.indexOf(":: מ0.40 — הקבצים שברשימה");
            String part = all.substring(a, b) + "מחק תיקייה \"%פנימי%\\בדיקת מ0.40 (אפשר למחוק)\" /עמוק\n";
            java.util.Map<String, String> v = Parse.vars(r, r.getAbsolutePath(), null);
            Index ix = new Index("040ג", "040ג", r.getAbsolutePath());
            ix.scan(null);
            Library lib = Library.forTests(new ArrayList<Index>(java.util.Collections.singletonList(ix)));
            Batch.Plan p = Batch.plan(part, r, v, lib);
            eq("מ0.40: החלק החדש של הבדיקה הכללית — בלי שגיאות בתכנון", 0, p.errors);
            String rep = Batch.run(p, lib, null, null).toString();
            check("מ0.40: ...ובהרצה — כל ה\"ודא\" עוברים (" + rep + ")", rep.contains("ודא: 7 עברו · 0 נכשלו") && !p.trouble);
            check("מ0.40: ...והתיקייה הוסרה", !new File(r, "בדיקת מ0.40 (אפשר למחוק)").exists());
            // מ0.43: כמו "הכול" של תוסף האבחון — הקבצים בשמות שהתוסף נותן להם (FILES בעמוד), וכל אחד נכלל
            File td = new File(r, "אבחון");
            td.mkdirs();
            String[][] parts = {{"klali", "בדיקה כללית"}, {"lachatz", "בדיקת לחץ"}, {"shney-ofanim", "בדיקת שני אופנים"}, {"omes-masad", "בדיקת עומס"},
                    {"pkudot-masad", "בדיקת פקודות"}, {"pkudot-kalul", "בדיקת כלול"}, {"gersa-023", "בדיקת מ0.23"}, {"gersa-024", "בדיקת מ0.24"},
                    {"itzuv", "בדיקת עיצוב"}};   // מבנה 242
            for (String[] x : parts) Files.writeAtomic(new File(td, x[1] + "." + Batch.EXT), Files.readUtf8(new File(tdir, x[0] + ".vbat")));
            Batch.Plan sp = Batch.plan("בדיקה\nהמשך ()\nכלול \"בדיקה כללית.vbat\"\nכלול \"בדיקת לחץ.vbat\"\nכלול \"בדיקת שני אופנים.vbat\"\nכלול \"בדיקת עומס.vbat\"\n",
                    td, Parse.vars(td, r.getAbsolutePath(), null));
            int calls = 0, verifies = 0;
            for (Batch.Step x : sp.steps) {
                if (Words.DbCmd.CALL_FAILED.equals(x.error)) calls++;
                if (Syntax.VERIFY.equals(x.cmd) || "EXPECT".equals(x.cmd)) verifies++;   // גם "ודא שגיאה" נספרת בסיכום
            }
            check("מ0.43: \"הכול\" של התוסף — כל החלקים נכללים (" + sp.steps.size() + " שורות)", calls == 0 && sp.steps.size() > 200);
            eq("מ0.43: \"הכול\" — כמה \"ודא\" יש בחלקים (הכללית 88, לחץ 19, שני אופנים 4, עומס 4; מ0.45: +14 — אופן חדש והסל; מבנה 48: +6 — ממשיך מגיבוי, כתיב אחר; מבנה 51: +4 — ארבע השורות שצפויות להידחות מסומנות \"ודא שגיאה\"; מבנה 59: -2 — \"ממשיך מגיבוי\" עבר לקבוצה משלו בתוסף; מבנה 102: +1 — רשימה שכבר קיימת: \"ודא שגיאה\"; מבנה 207: +2 — כפולים ברשימה חכמה; מבנה 242: +21 — העיצוב; מבנה 249: +3 — תווית הסוג; מבנה 250: +3 — הנוסח של התוויות; מבנה 254: +4 — קבוצה א: סגנון, הצג צלמית, סביב הצלמית…; מבנה 269: +1 — צבעים לרשימה)", 158, verifies);
            // מ0.41: הקטע החדש של הכללית — השורות שאינן של המסד מתוכננות בלי שגיאה; "ודא שגיאה" תופס את %נבחרים%
            String m042 = all.substring(all.indexOf(":: מ0.42"), all.indexOf(":: מ0.45 — אופן חדש"));   // נבדק לבד (m042cmds)
            String m045 = all.substring(all.indexOf(":: מ0.45 — אופן חדש"), all.indexOf(":: מ0.41 — הבדיקה מנקה"));   // נבדק לבד (למטה)
            String sec = "צור תיקייה \"%פנימי%\\בדיקת יצוא (אפשר למחוק)\"\n" + all.substring(all.indexOf(":: מ0.41 — בטל לא-משויכים"))
                    .replace(m042, "").replace(m045, "")
                    .replace(":: מבנה 242 — העיצוב: \"עצב\" ו\"ודא עיצוב\", כל מתג, ברשימה ובאופן\nכלול \"בדיקת עיצוב.vbat\"\n", "");   // נבדק לבד (מבנה 71, וההרצה של "הכול")
            Batch.Plan np = Batch.plan(sec, r, v, lib);
            int fsBad = 0, dbLines = 0;
            boolean caught = false;
            for (Batch.Step x : np.steps) {
                boolean db = x.text.contains("ידנית") || x.text.contains(ListCmd.LOOSE_WORD) || x.text.startsWith("אופן") || x.text.startsWith("צור רשימה")
                        || x.text.startsWith("בטל חכמה");                                     // מבנה 207
                if (db) dbLines++;
                else if (x.error != null) fsBad++;
                if (x.text.contains("%נבחרים%")) caught = x.error == null && Boolean.TRUE.equals(x.verified);
            }
            check("מ0.41: הקטע החדש של הכללית — שורות הכונן בלי שגיאה (" + fsBad + "), ושורות המסד מזוהות (" + dbLines + ")", fsBad == 0 && dbLines == 22);   // מבנה 207: +7 — בדיקת הכפולים
            check("מ0.41: ...ו\"ודא שגיאה\" תופס את %נבחרים% שמחוץ לרשימת \"בחירה\"", caught);
            // מ0.42: הקטע של הגרסה — שורות הכונן והאינדקס, בלי שורות המסד (צור אופן "בדיקות מערכת", ודא מסד): כל ה"ודא" עוברים
            StringBuilder fs42 = new StringBuilder();
            for (String l : m042.split("\n"))
                if (!l.equals("צור אופן \"בדיקות מערכת\"") && !l.equals("ודא מסד") && !l.contains("בדיקת שם 042")) fs42.append(l).append('\n');
            Batch.Plan p42 = Batch.plan(fs42.toString(), r, v, lib);
            String rep42 = Batch.run(p42, lib, null, null).toString();
            check("מ0.42: הקטע החדש של הכללית — שם אופן לא תקין נתפס, הקובץ שנכתב באינדקס, והתיקייה הוסרה (" + rep42 + ")",
                    p42.errors == 0 && rep42.contains("ודא: 4 עברו · 0 נכשלו") && !new File(r, "בדיקת מ0.42 (אפשר למחוק)").exists());
            // מ0.45: הסל (בלי המסד — "אופן חדש" רק במכשיר): מחק ← שחזר מהסל ← רוקן סל
            String trash = m045.substring(m045.indexOf(":: מ0.45 — הסל"));
            Batch.Plan p45 = Batch.plan(trash, r, v, lib);
            String rep45 = Batch.run(p45, lib, null, null).toString();
            check("מ0.45: קטע הסל של הכללית — כל ה\"ודא\" עוברים, ומהתיקייה לא נשאר דבר בכונן ובסל (" + rep45 + ")",
                    p45.errors == 0 && rep45.contains("ודא: 7 עברו · 0 נכשלו") && !new File(r, "בדיקת מ0.45 (אפשר למחוק)").exists());
        } catch (Exception e) {
            check("מ0.40: הבדיקה הכללית — " + e, false);
        }
    }

    /** מ0.42: העמוד שבהתקנה — "אבחון", זהה לעמוד שב־lab-pages, ובתוכו קובצי הבדיקה כמו שהם בהתקנה (avchon-build.py). */
    private static void m042plugin() {
        try {
            File src = new File(System.getProperty("vfm.src")), pages = new File(src.getParentFile(), "lab-pages");
            File asset = new File(src, "assets/" + Lab.BUNDLED + "/avchon.html");
            String html = Files.readUtf8(asset);
            String[] nv = Lab.nameOf(html, asset.getName());
            eq("מ0.42: תוסף האבחון — השם שהתפריט פותח", Words.DbCmd.TOOL_DIAG, nv[0]);
            File same = new File(pages, "תוסף " + nv[0] + " " + nv[1] + ".html");
            check("מ0.42: ...זהה לעמוד שב־lab-pages (" + nv[1] + ")", same.isFile() && Files.readUtf8(same).equals(html));
            File[] t = new File(pages, "avchon-tests").listFiles();
            int n = 0;
            for (File f : t) {
                if (!f.getName().endsWith(".vbat")) continue;
                String body = Files.readUtf8(f).replace("\r", "");
                StringBuilder j = new StringBuilder();
                for (char c : body.toCharArray())
                    j.append(c == '"' ? "\\\"" : c == '\\' ? "\\\\" : c == '\n' ? "\\n" : c == '\t' ? "\\t" : String.valueOf(c));
                String key = "\"" + f.getName().replace(".vbat", "") + "\": \"" + j.toString().replace("</", "<\\/") + "\"";
                if (html.contains(key)) n++;
                else check("מ0.42: קובץ הבדיקה " + f.getName() + " — בעמוד כמו בהתקנה (להריץ avchon-build.py)", false);
            }
            check("מ0.42: כל קובצי הבדיקה בעמוד (" + n + ")", n >= 8);
            check("מ0.43: קובצי הבדיקה — כבר לא בקובץ ההתקנה (רק בתוסף)", !new File(src, "assets/tests").exists());
            check("מ0.43: התוסף מנקה בסוף — בטל לא-משויכים, ובודק את האינדקס ובנייתו", html.contains("'בטל לא-משויכים'") && html.contains("'רענן מלא'")
                    && html.contains("'רענן עמוק'") && html.contains("dbStats"));
            check("מ0.42: העמוד יוצר את אופן הבדיקות ומבקש את התוצאה בקובץ", html.contains("'צור אופן \"'+TEST_MODE") && html.contains("'דוח \"'+path"));
            check("מ0.45: כל הפקודות (מהגשר), עצירה באמצע (הרצה משלה), כמה קוראים, מגבלת זמן לשורה, והתיקיות הזמניות — גם מהסל",
                    html.contains("V.commandMatrix()") && html.contains("function runOwn") && html.contains("'המשך (1)'") && html.contains("'רענן מלא /קוראים:16'")
                            && html.contains("V.setReaders(") && html.contains("'מדידה /עד:'+LIMIT") && html.contains("'רוקן סל \"'+d"));
        } catch (Exception e) {
            check("מ0.42: תוסף האבחון — " + e, false);
        }
    }

    /** מ0.44: "קטע" — שורה בדוח במקום שלה (לא בראש, כמו ECHO), ובלי שינוי. */
    private static void m044section() {
        File r = new File(home, "כונן-044");
        wipe(r);
        r.mkdirs();
        java.util.Map<String, String> v = Parse.vars(r, r.getAbsolutePath(), null);
        Batch.Plan p = Batch.plan("קטע \"א\"\nכתוב \"%כאן%\\א.txt\" \"x\"\nודא \"%כאן%\\א.txt\" קיים\nקטע \"ב\"\nודא \"%כאן%\\ב.txt\" חסר\n", r, v);
        eq("מ0.44: קטע — בלי שגיאות, ולא נחשב שינוי", 0, p.errors);
        List<String> rep = Batch.run(p, null, null, null);
        String all = rep.toString();
        int a = -1, b = -1, okA = -1, okB = -1;
        for (int i = 0; i < rep.size(); i++) {
            if (rep.get(i).endsWith(": ▸ א")) a = i;
            if (rep.get(i).endsWith(": ▸ ב")) b = i;
            if (rep.get(i).contains("✅ \"א.txt\" קיים")) okA = i;
            if (rep.get(i).contains("✅ \"ב.txt\" חסר")) okB = i;
        }
        check("מ0.44: הקטעים בדוח — לפי הסדר, כל אחד לפני השורות שלו (" + all + ")", a >= 0 && a < okA && okA < b && b < okB);
        eq("מ0.44: קטע בלי שם — שגיאה", 1, Batch.plan("קטע\n", r, v).errors);
        eq("מ0.44: \"קטע\" — השם בעברית של SECTION", Syntax.SECTION, Commands.canonical("קטע"));
    }

    private static void m045trash() {
        File r = new File(home, "כונן-045");
        wipe(r);
        r.mkdirs();
        Index ix = new Index("045", "045", r.getAbsolutePath());
        ix.scan(null);
        Library lib = Library.forTests(new ArrayList<Index>(java.util.Collections.singletonList(ix)));
        java.util.Map<String, String> v = Parse.vars(r, r.getAbsolutePath(), null);
        String f = "\"%כאן%\\סל\\א.txt\"";
        String text = "צור תיקייה \"%כאן%\\סל\"\nכתוב " + f + " \"ראשון\"\nמחק " + f + "\nודא " + f + " חסר\nודא " + f + " /בסל:1\n"
                + "שחזר מהסל " + f + "\nודא " + f + " מכיל \"ראשון\"\nודא " + f + " /בסל:0\n"
                // מבנה 46: "כתוב שני" נכתב על "ראשון" ששוחזר (מבנה 110: עם /תפוס:להחליף) — והוא עובר לסל; לכן 3 בסל (עד כאן 2), ואחרי השחזור 2 (עד כאן 1)
                + "כתוב " + f + " \"שני\" /תפוס:להחליף\nמחק " + f + "\nכתוב " + f + " \"שלישי\"\nמחק " + f + "\nודא " + f + " /בסל:3\n"
                + "שחזר מהסל " + f + "\nודא " + f + " מכיל \"שלישי\"\nודא " + f + " /בסל:2\n"
                + "מחק " + f + "\nרוקן סל " + f + "\nודא " + f + " /בסל:0\nודא " + f + " חסר\n";
        Batch.Plan p = Batch.plan(text, r, v, lib);
        eq("מ0.45: הסל — בלי שגיאות בתכנון (" + p.errorText() + ")", 0, p.errors);
        String rep = Batch.run(p, lib, null, null).toString();
        check("מ0.45: מחק ← בסל ← שחזר מהסל (האחרון שנמחק) ← רוקן סל — כל ה\"ודא\" עוברים (" + rep + ")",
                rep.contains("ודא: 9 עברו · 0 נכשלו") && !p.trouble);
        // לא בסל — לא שגיאה בהכנה (אולי שורה שלפני מוחקת), אבל כישלון בהרצה
        Batch.Plan q = Batch.plan("שחזר מהסל \"%כאן%\\סל\\אין.txt\"\nכתוב \"%כאן%\\סל\\אחרי.txt\" \"x\"\n", r, v, lib);
        check("מ0.45: שחזר מהסל — מה שאינו בסל: \"ייבדק בהרצה\", לא שגיאה", q.errors == 0 && q.steps.get(0).describe.contains("ייבדק בהרצה"));
        String qr = Batch.run(q, lib, null, null).toString();
        check("מ0.45: ...ובהרצה — כישלון, והשורות שאחריו לא רצות (" + qr + ")", qr.contains("אין בסל") && qr.contains("⛔")
                && !new File(r, "סל/אחרי.txt").exists());
        // רוקן סל בלי מסלול — כל הסל
        Batch.Plan all = Batch.plan("כתוב \"%כאן%\\סל\\ב.txt\" \"x\"\nמחק \"%כאן%\\סל\\ב.txt\"\nרוקן סל\nודא \"%כאן%\\סל\\ב.txt\" /בסל:0\n", r, v, lib);
        check("מ0.45: רוקן סל — בתצוגה: כל הסל, ומסומן כמחיקה", all.errors == 0 && all.steps.get(2).describe.equals(Words.DbCmd.PURGE_ALL_WILL)
                && Runner.kind(all.steps.get(2)) == Runner.K_DELETE);
        String ar = Batch.run(all, lib, null, null).toString();
        check("מ0.45: ...והסל ריק (" + ar + ")", ar.contains("ודא: 1 עברו · 0 נכשלו") && Trash.list(lib).isEmpty());
        eq("מ0.45: שחזר מהסל בלי מסלול — שגיאה", 1, Batch.plan("שחזר מהסל\n", r, v, lib).errors);
        eq("מ0.45: רוקן סל עם שני מסלולים — שגיאה", 1, Batch.plan("רוקן סל \"א\" \"ב\"\n", r, v, lib).errors);
        wipe(r);
    }

    private static void m045matrix() {
        java.util.Set<String> got = new java.util.HashSet<String>();
        String all = CmdMatrix.text(true, got);
        java.util.Set<String> miss = CmdMatrix.needed();
        miss.removeAll(got);
        check("מ0.45: לכל פקודה ולכל מתג (ולכל אפשרות) — שורה בקובץ; פקודה חדשה — או דוגמה, או ב־SKIP עם הסבר (" + miss + ")", miss.isEmpty());
        for (String[] x : CmdMatrix.SKIP) check("מ0.45: SKIP — פקודה שקיימת, עם הסבר: " + x[0], Commands.get(x[0]) != null && x[1].length() > 10);
        File r = new File(home, "כונן-045מ");
        wipe(r);
        r.mkdirs();
        Index ix = new Index("045מ", "045מ", r.getAbsolutePath());
        ix.scan(null);
        Library lib = Library.forTests(new ArrayList<Index>(java.util.Collections.singletonList(ix)));
        File here = new File(r, "נתוני התוסף");                               // כמו בתוסף: קובץ ההרצה — בתיקייה משלו
        here.mkdirs();
        try {
            Files.writeAtomic(new File(here, "הרצה.vbat"), "ECHO x");
            CmdMatrix.fixtures(here, null);
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
        ix.scan(null);
        java.util.Map<String, String> v = Parse.vars(here, r.getAbsolutePath(), null);
        String text = "המשך ()\nמדידה /עד:30שניות\n" + CmdMatrix.text(false);
        Batch.Plan p = Batch.plan(text, here, v, lib);
        eq("מ0.45: ...בלי מסד — בלי שגיאות בתכנון (" + p.errorText() + ")", 0, p.errors);
        List<String> rep = Batch.run(p, lib, null, null);
        StringBuilder bad = new StringBuilder();
        for (String x : rep) if (x.contains("❌") || x.contains("⚠") || x.contains("⛔")) bad.append(x).append(" | ");
        check("מ0.45: ...ובהרצה — \"ודא תקין\" עובר, ושום ❌ (" + bad + ")", bad.length() == 0
                && rep.toString().contains("✅ " + Words.DbCmd.OK_SINCE_WHAT));
        // מבנה 70 (מהדוח של נ0.6): "העבר %רק-במקור%" של הבדיקה לא מרוקן את התיקייה של קובץ ההרצה (הגיבוי הקטן, הדוחות של התוסף)
        check("מבנה 70: אחרי \"כל הפקודות\" — קובץ ההרצה והגיבוי הקטן עדיין במקומם", new File(here, "הרצה.vbat").isFile()
                && new File(here, CmdMatrix.FIXTURE + "/" + State.CURRENT + "/א.txt").isFile());
        check("מבנה 70: ההשוואה שלפני \"/עד:\" — מול תיקייה משלה", text.contains(CmdMatrix.COMPARE) && new File(here, CmdMatrix.COMPARE).isDirectory());
        StringBuilder left = new StringBuilder();
        for (Trash.Item it : Trash.list(lib)) left.append(it.name()).append(" ← ").append(it.original).append(" | ");
        check("מ0.45: ...והתיקייה הזמנית — לא בכונן ולא בסל (" + left + ")", !new File(r, "בדיקת כל הפקודות (אפשר למחוק)").exists() && left.length() == 0);
        check("מ0.45: ...עם המסד — עוד שורות (קשר, ודא ידנית, השווה /רשימה:)", CmdMatrix.text(true).split("\n").length > CmdMatrix.text(false).split("\n").length + 3);
        // מבנה 5: הקובץ הזמני של יומן המחיקות ("." + השם + "." + מספר + ".tmp" — Files.writeAtomic) שנתפס באינדקס באמצע כתיבה —
        // לא פריט בסל. (הסינון בדק רק שם שמתחיל ב־LOG, והזמני מתחיל בנקודה נוספת; הבדיקה שלמעלה נכשלה לפעמים, לפי התזמון.)
        File tr = new File(r, Index.TRASH);
        tr.mkdirs();
        try {
            new java.io.FileOutputStream(new File(tr, "." + Trash.LOG + ".123.tmp")).close();
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
        lib.fileAdded(new File(tr, "." + Trash.LOG + ".123.tmp"));                // כמו שהאינדקס שומע על קובץ חדש — בלי להסתמך על סריקה
        StringBuilder tmp = new StringBuilder();
        for (Trash.Item it : Trash.list(lib)) tmp.append(it.name()).append(" | ");
        check("מבנה 5: הקובץ הזמני של יומן המחיקות — לא פריט בסל (" + tmp + ")", tmp.length() == 0);
        wipe(r);
    }

    /** מבנה 27: ההחלטות של הרצת קובץ פקודות — אצל המנהל (lev/Manager); שני הרצפים (BatchUi, RunScreen) שואלים אותו. */
    private static boolean link(File link, File target) {
        try {
            java.nio.file.Files.createSymbolicLink(link.toPath(), target.toPath());
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean sameViaLink(File f) {
        File l = new File(f.getParentFile().getParentFile(), "קישור-לבדיקה");
        if (!link(l, f.getParentFile())) return true;                          // אין קישורים במערכת הזו — אין מה לבדוק
        boolean same = DiskRules.sameFile(f, new File(l, f.getName()));
        l.delete();
        return same;
    }

    private static void mivne27manager() {
        File r = new File(home, "כונן-מבנה27");
        wipe(r);
        r.mkdirs();
        java.util.Map<String, String> v = Parse.vars(r, r.getAbsolutePath(), null);
        eq("מבנה 27: יש שינוי, בלי שגיאה — מותר להריץ", Manager.GO, Manager.approve(Batch.plan("MD \"א\"\n", r, v)));
        eq("מבנה 27: בכאילו — רק התוצאה", Manager.AS_IF, Manager.approve(Batch.plan(Syntax.AS_IF + "\nMD \"א\"\n", r, v)));
        eq("מבנה 27: רק ECHO — אין מה לעשות", Manager.NOTHING, Manager.approve(Batch.plan("ECHO שלום\n", r, v)));
        eq("מבנה 27: קובץ ריק — אין מה לעשות", Manager.NOTHING, Manager.approve(Batch.plan("\n", r, v)));
        Batch.Plan bad = Batch.plan("MD \"א\"\nCOPY\n", r, v);
        check("מבנה 27: שורה שגויה לצד שינוי — לא מריצים דבר", bad.errors == 1 && Manager.approve(bad) == Manager.BLOCKED);
        Batch.Plan onlyBad = Batch.plan("COPY\n", r, v);
        eq("מבנה 27: רק שורה שגויה — קודם \"אין מה לעשות\" (הסדר שהיה בשני המסכים)", Manager.NOTHING, Manager.approve(onlyBad));
        Batch.Plan asIfBad = Batch.plan(Syntax.AS_IF + "\nMD \"א\"\nCOPY\n", r, v);
        eq("מבנה 27: בכאילו קודם לכול — גם עם שגיאה", Manager.AS_IF, Manager.approve(asIfBad));
        Batch.Plan ok = Batch.plan("MD \"א\"\n", r, v);
        check("מבנה 27: פתיחות הסוף — רק כשההרצה הלכה חלק", Manager.opensAllowed(ok));
        ok.trouble = true;
        check("מבנה 27: הייתה תקלה — פתיחות הסוף לא רצות", !Manager.opensAllowed(ok));
        File code = new File(System.getProperty("vfm.src"), "src/com/tikiyot/vfm");
        // מבנה 29: מה קורה בסוף הרצה מיידית
        Batch.Plan quiet = Batch.plan("MD \"א\"\n", r, v);
        check("מבנה 29: הלך חלק — הודעה קצרה, לא הדוח", !Manager.fullReport(quiet) && !Manager.openStartOnTrouble(quiet));
        check("מבנה 29: יש ECHO — הדוח המלא", Manager.fullReport(Batch.plan("ECHO שלום\nMD \"א\"\n", r, v)));
        check("מבנה 29: מדידה — תמיד הדוח", Manager.fullReport(Batch.plan("מדידה\nMD \"א\"\n", r, v)));
        quiet.trouble = true;
        check("מבנה 29: תקלה — הדוח המלא; בלי START אין מה לפתוח", Manager.fullReport(quiet) && !Manager.openStartOnTrouble(quiet));
        File st = new File(r, "פתיחה.txt");
        try { new java.io.FileOutputStream(st).close(); } catch (java.io.IOException e) { throw new RuntimeException(e); }
        quiet.start = st;
        check("מבנה 29 (186): תקלה ו־START הוא קובץ — נפתח בכל זאת", Manager.openStartOnTrouble(quiet));
        quiet.start = r;
        check("מבנה 29: ...תיקייה — לא", !Manager.openStartOnTrouble(quiet));
        quiet.start = st;
        quiet.trouble = false;
        check("מבנה 29: ...בלי תקלה — לא (START נפתח בדרך הרגילה)", !Manager.openStartOnTrouble(quiet));
        // מבנה 28: מכוונת — לאן הולך טקסט של פקודות
        eq("מבנה 28: פקודת פתיחה אחת — מיד", Manager.TO_VERB, Manager.route("צלמית 🏠\nשם בית\nפתח מסך הבית\n", false));
        eq("מבנה 28: ...נוסתה ולא בוצעה — ממשיכים כקובץ רגיל", Manager.TO_RUN, Manager.route("פתח מסך הבית\n", true));
        eq("מבנה 28: \"בדיקה\" בראש — למסך ההרצה", Manager.TO_CHECK, Manager.route(Syntax.CHECK + "\nפתח מסך הבית\n", false));
        String holes = "COPY <מקור> <יעד>\n";
        check("מבנה 28: מקומות פתוחים — נשאלים לפני ההרצה", !CmdTemplates.holes(Manager.lines(holes)).isEmpty() && Manager.route(holes, false) == Manager.TO_HOLES);
        eq("מבנה 28: \"בדיקה\" קודמת למקומות הפתוחים (מסך ההרצה שואל עליהם בעצמו)", Manager.TO_CHECK, Manager.route(Syntax.CHECK + "\n" + holes, false));
        eq("מבנה 28: קובץ רגיל — הכנה והרצה", Manager.TO_RUN, Manager.route("MD \"א\"\n", false));
        eq("מבנה 28: השורות — בלי \\r", Arrays.asList("א", "ב"), Manager.lines("א\r\nב\r\n"));
        // מבנה 41: מסלול ריק — שגיאה, לא "התיקייה הזו"
        String[] pe = {null};
        check("מבנה 41: מסלול ריק — שגיאה", Parse.path("", r, v, pe) == null && Words.Cmd.EMPTY_PATH.equals(pe[0]));
        check("מבנה 41: רק רווחים — שגיאה", Parse.path("   ", r, v, pe) == null);
        java.util.Map<String, String> v2 = new java.util.HashMap<String, String>(v);
        v2.put("ריק", "");
        check("מבנה 41: משתנה שהערך שלו ריק — שגיאה", Parse.path("%ריק%", r, v2, pe) == null && Words.Cmd.EMPTY_PATH.equals(pe[0]));
        eq("מבנה 41: נקודה — התיקייה של הקובץ, כמו קודם (מבנה 42: בלי הנקודה בסוף)", r.getAbsolutePath(), Parse.path(".", r, v, pe));
        // מבנה 42: מסלול בצורה אחת
        eq("מבנה 42: נקודות ולוכסנים כפולים יורדים", "/a/c", Paths.clean("/a/./b/..//c/"));
        eq("מבנה 42: מעל השורש — נשארים בשורש", "/", Paths.clean("/../.."));
        eq("מבנה 42: מסלול נקי — כמו שהוא", "/a/.נסתר/b", Paths.clean("/a/.נסתר/b"));
        eq("מבנה 42: סימן פנימי של המנוע — לא נוגעים", Syntax.SHARE_MARK, Paths.clean(Syntax.SHARE_MARK));
        eq("מבנה 42: מסלול יחסי", "../x", Paths.clean("a/../../x"));
        eq("מבנה 42: בקובץ פקודות — \"תיקייה\\..\" היא התיקייה שמעל", r.getAbsolutePath(), Parse.path("א\\..\\.", r, v, pe));
        File k42 = new File(r, "ק42");
        new File(k42, "פנים").mkdirs();
        File f42 = new File(k42, "קובץ.txt");
        try { java.io.FileOutputStream o = new java.io.FileOutputStream(f42); o.write("תוכן".getBytes("UTF-8")); o.close(); } catch (java.io.IOException e) { throw new RuntimeException(e); }
        Index ix42 = new Index("מבנה42", "מבנה42", r.getAbsolutePath());
        ix42.scan(null);
        Library lib42 = Library.forTests(new ArrayList<Index>(java.util.Collections.singletonList(ix42)));
        Batch.Plan here42 = Batch.plan("MOVE \"ק42\\קובץ.txt\" \"ק42\\פנים\\..\" /Y\n", r, v, lib42);
        check("מבנה 42: העברה אל התיקייה שהקובץ כבר בה, בכתיב עם נקודות — \"כבר במקום\", לא העברה", here42.errors == 0 && here42.steps.get(0).noop);
        check("מבנה 42: אותו קובץ בשני מסלולים (קישור) — מזוהה", sameViaLink(f42));
        check("מבנה 42: שני קבצים שונים — לא אותו קובץ", !DiskRules.sameFile(f42, new File(r, "פתיחה.txt")));
        File link42 = new File(r, "קישור42");
        boolean linked = link(link42, k42);
        if (linked) {
            Batch.Plan viaLink = Batch.plan("MOVE \"קישור42\\קובץ.txt\" \"ק42\" /Y\n", r, v, lib42);
            check("מבנה 48: העברה של קובץ אל התיקייה שלו דרך קישור — כבר בתכנון \"כבר במקום\", והשורה לא רצה",
                    viaLink.errors == 0 && viaLink.steps.get(0).noop && viaLink.steps.get(0).inPlace == 1);
            Physical.Result self42 = Physical.transfer(lib42, java.util.Collections.singletonList(new File(link42, "קובץ.txt").getAbsolutePath()),
                    java.util.Collections.<String>emptyList(), k42, false, new Physical.Asker() {
                        @Override public Physical.Answer ask(File existing, File incoming, String suggested) {
                            return new Physical.Answer(Physical.Choice.REPLACE, null, true);
                        }
                    }, null, false);
            check("מבנה 42: ...וגם ההעברה עצמה, עם \"להחליף\" — הקובץ נשאר במקומו ולא נשלח לסל (" + self42.note + ")",
                    f42.isFile() && "תוכן".equals(read(f42)) && self42.failed == 1 && self42.note.contains(Words.Disk.ALREADY_HERE));
            Batch.Plan copyLink = Batch.plan("COPY \"קישור42\\קובץ.txt\" \"ק42\" /Y\n", r, v, lib42);
            Batch.run(copyLink, lib42, null, null);
            check("מבנה 42: העתקה של קובץ אל עצמו דרך קישור — עותק בשם חדש, והמקור שלם", "תוכן".equals(read(f42)) && k42.list().length == 3);
        }
        check("מבנה 42: ההגנה על שורש הכונן — גם בכתיב \"תיקייה/..\"", Words.Disk.DRIVE_ROOT.equals(DiskRules.forbidden(lib42, new File(k42, ".."))));
        check("מבנה 42: ההגנה על הסל — גם בכתיב עם נקודות", Words.Disk.IS_TRASH.equals(DiskRules.forbidden(lib42, new File(k42, "../" + Index.TRASH + "/x"))));
        String app42 = Paths.data().getAbsolutePath();
        check("מבנה 42: קובץ של התוכנה — גם דרך תיקייה של המשתמש ו\"..\"", DiskRules.programFile(app42 + "/" + Paths.USER_DIRS[0] + "/../" + Paths.SETTINGS + "/x.txt") != null
                && DiskRules.programFile(app42 + "/" + Paths.USER_DIRS[0] + "/x.txt") == null);
        check("מבנה 42: \"בתוך\" — גם בכתיב עם נקודות", DiskRules.inside(new File(k42, "פנים/../פנים"), k42) && !DiskRules.inside(new File(k42, ".."), k42));
        // מבנה 43: פערים קטנים בין התכנון להרצה
        Batch.Plan p43 = Batch.plan("MD \"ת43\\א\"\nMD \"ת43-יעד\"\nMOVE \"ת43\\א\" \"ת43-יעד\"\nRD \"ת43\"\n", r, v);
        eq("מבנה 43: MD, MOVE של התיקייה החוצה, RD — התיקייה ריקה, בלי שגיאה", 0, p43.errors);
        new File(r, "ע43/פנים").mkdirs();
        Batch.Plan self43 = Batch.plan("MOVE \"ע43\" \"ע43\\פנים\"\n", r, v);
        check("מבנה 43: תיקייה לתוך עצמה — שגיאה כבר בתכנון", self43.errors == 1 && Words.Disk.NOT_INTO_ITSELF.equals(self43.steps.get(0).error));
        Batch.Plan ren43 = Batch.plan("REN \"ק42\\קובץ.txt\" \"פנים\"\n", r, v);
        check("מבנה 43: REN לשם של תיקייה קיימת — שגיאה כבר בתכנון", ren43.errors == 1);
        java.util.Map<String, String> v43 = new java.util.HashMap<String, String>(v);
        v43.put("שם43", "חדש");
        Batch.Plan renVar = Batch.plan("REN \"ק42\\קובץ.txt\" \"%שם43%.txt\"\n", r, v43);
        check("מבנה 43: משתנה בשם החדש של REN — מוחלף", renVar.errors == 0 && "חדש.txt".equals(renVar.steps.get(0).newName));
        v43.put("תיקייה43", "א/ב");
        eq("מבנה 43: ...ומשתנה שהוא מסלול — שגיאת שימוש", 1, Batch.plan("REN \"ק42\\קובץ.txt\" \"%תיקייה43%\"\n", r, v43).errors);
        Batch.Plan mj = Batch.plan(Syntax.MEASURE + " " + Syntax.SW_JOURNAL + "%אין-כזה%\nMD \"ת43-ב\"\n", r, v);
        check("מבנה 43: מדידה עם יומן במסלול שגוי — שגיאה, לא \"בלי יומן\" בשקט", mj.errors == 1 && mj.blocked());
        // מבנה 58: גיבוי לפני עריכה — מכסה משלו (5), ולא דוחק את העותקים שלפני מחיקה (10)
        List<Backups.Entry> all58 = new ArrayList<Backups.Entry>();
        long now58 = 1800000000000L;
        for (int i = 0; i < 12; i++) all58.add(new Backups.Entry(new File("/x/e" + i + ".db"), Backups.BEFORE, Backups.OP_EDIT, false, now58 - i * 60000L));
        for (int i = 0; i < 10; i++) all58.add(new Backups.Entry(new File("/x/d" + i + ".db"), Backups.BEFORE, Backups.OP_DELETE, false, now58 - 86400000L - i * 60000L));
        List<Backups.Entry> del58 = Backups.toDelete(all58);
        int edits58 = 0, others58 = 0;
        for (Backups.Entry e : del58) { if (Backups.OP_EDIT.equals(e.op)) edits58++; else others58++; }
        check("מבנה 58: 12 עותקים לפני עריכה — 5 נשארים, והישנים נמחקים", edits58 == 7 && del58.get(0).when <= now58 - 5 * 60000L);
        check("מבנה 58: 10 העותקים שלפני מחיקה — נשארים כולם, גם אחרי 12 עריכות", others58 == 0);
        Backups.Entry p58 = Backups.parse(new File("/x/לפני עריכה 2026-11-14 22-10-05.db"));
        check("מבנה 58: שם של עותק לפני עריכה — נקרא חזרה", p58 != null && Backups.BEFORE.equals(p58.kind) && Backups.OP_EDIT.equals(p58.op));
        // מבנה 59: אופן שבוטל — הגיבויים שלו לסל. הנוסח אומר זאת; וכל מה שקבוצה בתוסף האבחון "צריכה" — התוכנה מצהירה שהיא יודעת
        check("מבנה 59: ההודעה אחרי ביטול — הגיבויים בסל, ואיך חוזרים", Words.DbCmd.dropModeDone("א", true, true).contains("הגיבויים שלו — בסל")
                && Words.DbCmd.dropModeDone("א", true, true).contains("צור אופן") && !Words.DbCmd.dropModeDone("א", false, false).contains("גיבוי"));
        check("מבנה 59: אופן שכבר אינו — נאמר מה קרה לגיבויים שנשארו", Words.DbCmd.dropModeLeft("א", true).contains("עברו לסל")
                && Words.DbCmd.dropModeLeft("א", false).contains("לא עברו"));
        try {
            File root59 = new File(System.getProperty("vfm.src")).getParentFile();
            String tpl59 = Files.readUtf8(new File(root59, "lab-pages/avchon.template"));
            String br59 = Files.readUtf8(new File(System.getProperty("vfm.src"), "src/com/tikiyot/vfm/lev/Bridge.java"));
            java.util.regex.Matcher f59 = java.util.regex.Pattern.compile("o\\.put\\(\"features\", \"([^\"]*)\"\\)").matcher(br59);
            String feats59 = f59.find() ? " " + f59.group(1) + " " : "";
            java.util.regex.Matcher n59 = java.util.regex.Pattern.compile("need:'([^']+)'").matcher(tpl59);
            String missing59 = "";
            int needs59 = 0;
            while (n59.find()) {
                needs59++;
                if (!n59.group(1).equals("card") && !feats59.contains(" " + n59.group(1) + " ")) missing59 += n59.group(1) + " ";
            }
            check("מבנה 59: כל מה שקבוצה בתוסף האבחון צריכה — התוכנה מצהירה עליו (חסר: " + missing59 + ")", needs59 >= 3 && missing59.isEmpty());
            check("מבנה 59: הקבוצה \"אופן שבוטל וחוזר\" — בתוסף, והמקום של הגיבויים בא מהתוכנה", tpl59.contains("k:'resume'") && tpl59.contains("info.backupsVar")
                    && br59.contains("o.put(\"backupsVar\""));
            String kl59 = Files.readUtf8(new File(root59, "lab-pages/avchon-tests/klali.vbat"));
            check("מבנה 59: בבדיקה הכללית — אחרי \"בטל אופן\", \"צור אופן\" בלי מתג (אופן חדש וריק)", !kl59.contains("/מגיבוי"));
        } catch (Throwable t) {
            check("מבנה 59: קריאת התוסף (" + t + ")", false);
        }
        // מבנה 62 (ממצא 40ה): "המתן" נענה ל"עצור" — עד כאן רק אחרי שכל ההמתנה נגמרה
        final long from62 = System.currentTimeMillis();
        Physical.Progress stop62 = new Physical.Progress() {
            @Override public void step(int files, int total, long bytes, long totalBytes) { }
            @Override public boolean stopped() { return System.currentTimeMillis() - from62 > 600; }
        };
        Batch.Plan p62 = Batch.plan("המתן 20\nMD \"אחרי-המתנה\"\n", r, v);
        String rep62 = String.valueOf(Batch.run(p62, null, null, stop62));
        long took62 = System.currentTimeMillis() - from62;
        check("מבנה 62: \"עצור\" באמצע \"המתן 20\" — ההמתנה נפסקת מיד (" + took62 + " אלפיות), ונאמר", took62 < 5000 && rep62.contains("נעצרה אחרי"));
        check("מבנה 62: ...והשורה שאחריה לא רצה", !new File(r, "אחרי-המתנה").exists() && p62.trouble);
        long from62b = System.currentTimeMillis();
        String rep62b = String.valueOf(Batch.run(Batch.plan("המתן 1\n", r, v), null, null, null));
        check("מבנה 62: בלי עצירה — ממתין את כל הזמן", System.currentTimeMillis() - from62b >= 1000 && rep62b.contains("המתנה של 1 שניות") && !rep62b.contains("נעצרה"));
        // מבנה 63 (פתוח 12): אופן שמסך המסד מחזיק את המסד שלו פתוח — "בשימוש", גם כשאינו האופן שבמסך הראשי
        String was63 = Db.currentMode;
        Db.currentMode = "ראשי-63";
        check("מבנה 63: האופן שבמסך הראשי — בשימוש; אופן אחר — לא", Db.inUse("ראשי-63") && !Db.inUse("מאחור-63") && !Db.inUse(null));
        Db.hold("מאחור-63", 1);
        Db.hold("מאחור-63", 1);
        check("מבנה 63: אופן שמסך מחזיק — בשימוש", Db.inUse("מאחור-63"));
        Db.hold("מאחור-63", -1);
        check("מבנה 63: שני מסכים החזיקו, אחד סגר — עדיין בשימוש", Db.inUse("מאחור-63"));
        Db.hold("מאחור-63", -1);
        Db.hold("מאחור-63", -1);
        check("מבנה 63: כולם סגרו — לא בשימוש (וסגירה מיותרת לא מקלקלת)", !Db.inUse("מאחור-63"));
        Db.currentMode = was63;
        String dlc63 = read(new File(code, "pkudot/ModeCmds.java")),   // מבנה 168: "מחק אופן" עבר מ־DbListCmd ל־ModeCmds — אותה בדיקה
                dbs63 = read(new File(code, "masachim/DbScreen.java"));
        check("מבנה 63: \"בטל אופן\" שואל את השער גם בתכנון וגם בהרצה, ומסך המסד נרשם וסוגר דרכו",
                dlc63.split("Db\\.inUse\\(name\\)", -1).length == 3 && !dlc63.contains("name.equals(Db.currentMode)")
                        && dbs63.contains("Db.openHeld(this, mode)") && dbs63.contains("Db.closeHeld(store, heldMode)") && !dbs63.contains("store.close()"));
        // מבנה 64 (ממצא 44; המשתמש: "כן" — להגן): התיקייה של המסד בכונן מוגנת כמו תיקיית התוכנה; התוספים ומה שהם כותבים — של המשתמש
        {
            File ext64 = Paths.data().getParentFile(), db64 = Paths.dbData();
            File bk64 = new File(new File(db64, Backups.DIR), "אופן-64"), plug64 = new File(db64, Paths.PLUGINS_DIR),
                    data64 = new File(new File(db64, Paths.PLUGIN_DATA_DIR), "אבחון-64");
            bk64.mkdirs();
            plug64.mkdirs();
            data64.mkdirs();
            write(bk64, "לפני מחיקת רשימה 2026-11-14 10-00-00.db", "גיבוי");
            write(plug64, "תוסף-64.html", "<html>");
            write(data64, "דוח.txt", "דוח");
            File md64 = Modes.dirOf("אופן-64");
            md64.mkdirs();
            write(md64, "x.txt", "x");
            Index ix64 = new Index("64", "64", ext64.getAbsolutePath());
            ix64.scan(null);
            Library lib64 = Library.forTests(new ArrayList<Index>(java.util.Collections.singletonList(ix64)));
            String counter64 = new File(new File(db64, Paths.SYSTEM), "מונה-מזהים.txt").getAbsolutePath();
            check("מבנה 64: התיקייה של המסד, \"גיבוי מסד\" ותיקיית גיבויים של אופן — אסורות למחיקה ולהעברה",
                    DiskRules.forbidden(lib64, db64) != null && DiskRules.forbidden(lib64, bk64.getParentFile()) != null && DiskRules.forbidden(lib64, bk64) != null
                            && DiskRules.forbidden(lib64, new File(db64, Paths.SYSTEM)) != null);
            check("מבנה 64: התוספים ומה שהם כותבים — של המשתמש", DiskRules.forbidden(lib64, plug64) == null && DiskRules.forbidden(lib64, data64) == null
                    && DiskRules.programFile(new File(plug64, "תוסף-64.html").getAbsolutePath()) == null
                    && DiskRules.programFile(new File(data64, "דוח.txt").getAbsolutePath()) == null);
            check("מבנה 64: קובץ המונה וקובץ גיבוי — קבצים של התוכנה; גם דרך \"..\" מתיקיית התוספים",
                    DiskRules.programFile(counter64) != null && DiskRules.programFile(new File(bk64, "א.db").getAbsolutePath()) != null
                            && DiskRules.programFile(db64.getAbsolutePath()) != null
                            && DiskRules.programFile(plug64.getAbsolutePath() + "/../" + Backups.DIR + "/אופן-64/א.db") != null);
            java.util.Map<String, String> v64 = Parse.vars(ext64, ext64.getAbsolutePath(), null);
            String bkCmd = "%" + Syntax.V_INTERNAL + "%\\" + Paths.DB_DIR + "\\" + Backups.DIR + "\\אופן-64";
            Batch.Plan del64 = Batch.plan("המשך ()\nמחק תיקייה \"" + bkCmd + "\" /עמוק\nDEL \"" + bkCmd + "\\*.db\"\nMOVE \"" + bkCmd + "\" \"%" + Syntax.V_INTERNAL + "%\\יעד-64\"\n", ext64, v64, lib64);
            Batch.run(del64, lib64, null, null);
            lib64.flush(5000);
            check("מבנה 64: בקובץ פקודות — מחיקה, מחיקת קבצים והעברה של תיקיית הגיבויים: לא קורה כלום", bk64.isDirectory() && bk64.list().length == 1
                    && !new File(ext64, "יעד-64/אופן-64").exists());
            Physical.Result no64 = Physical.delete(lib64, new ArrayList<String>(), java.util.Collections.singletonList(bk64.getAbsolutePath()), false);
            check("מבנה 64: המחיקה הרגילה מסרבת לתיקיית הגיבויים", no64.failed == 1 && bk64.isDirectory());
            Physical.Result own64 = Physical.deleteOwn(lib64, bk64);
            lib64.flush(5000);
            check("מבנה 64: התוכנה עצמה (\"בטל אופן\") — כן: התיקייה עוברת לסל", own64.failed == 0 && own64.done.size() == 1 && !bk64.exists()
                    && SmallCmds.trashed(lib64, bk64.getAbsolutePath(), false).size() == 1);
            Batch.run(Batch.plan("שחזר מהסל \"" + bkCmd + "\"\n", ext64, v64, lib64), lib64, null, null);
            lib64.flush(5000);
            check("מבנה 64: ...ושחזור מהסל מחזיר אותה למקומה (מי שמתחרט על ביטול אופן)", bk64.isDirectory() && bk64.list().length == 1);
            Physical.Result set64 = Physical.deleteOwn(lib64, md64);
            lib64.flush(5000);
            check("מבנה 64 (ממצא 45): תיקיית ההגדרות של אופן שבוטל — עוברת לסל (עד כאן המחיקה סירבה: \"זו תיקיית התוכנה\")",
                    set64.failed == 0 && set64.done.size() == 1 && !md64.exists());
            check("מבנה 64: מחיקה של התוכנה אינה עוקפת את שורש הכונן", Physical.deleteOwn(lib64, ext64).failed == 1 && ext64.isDirectory());
            File plain64 = new File(ext64, "רגילה-64");
            plain64.mkdirs();
            ix64.scan(null);
            check("מבנה 64: ותיקייה רגילה — לפי הכללים הרגילים", Physical.deleteOwn(lib64, plain64).failed == 0 && !plain64.exists());
            Batch.Plan rep64 = Batch.plan("דוח \"%" + Syntax.V_INTERNAL + "%\\" + Paths.DB_DIR + "\\" + Paths.PLUGIN_DATA_DIR + "\\אבחון-64\\תוצאה.txt\"\nECHO א\n", ext64, v64, lib64);
            check("מבנה 64: \"דוח\" לתיקיית הנתונים של תוסף — מותר (כך תוסף האבחון מקבל את התוצאות)", rep64.errors == 0);
            check("מבנה 64: \"דוח\" ו\"כתוב\" לתוך \"גיבוי מסד\" — שגיאה",
                    Batch.plan("דוח \"" + bkCmd + "\\דוח.txt\"\nECHO א\n", ext64, v64, lib64).errors == 1
                            && Batch.plan("כתוב \"" + bkCmd + "\\א.txt\" \"א\"\n", ext64, v64, lib64).errors == 1);
            // מבנה 65 (ממצא 40ח): "כתוב" — בכל מקום שמותר להעתיק אליו
            File user65 = new File(Paths.data(), Paths.USER_DIRS[0]);
            user65.mkdirs();
            new File(ext64, Index.TRASH).mkdirs();
            ix64.scan(null);
            String in65 = "%" + Syntax.V_INTERNAL + "%";
            Batch.Plan w65 = Batch.plan("המשך ()\nכתוב \"" + in65 + "\\שורש-65.txt\" \"א\"\nכתוב \"" + in65 + "\\" + Paths.APP_DIR + "\\" + Paths.USER_DIRS[0] + "\\שלי-65.txt\" \"ב\"\n", ext64, v64, lib64);
            Batch.run(w65, lib64, null, null);
            lib64.flush(5000);
            check("מבנה 65: \"כתוב\" בשורש הכונן — נכתב (עד כאן סורב בהרצה)", w65.errors == 0 && "א".equals(read(new File(ext64, "שורש-65.txt"))));
            check("מבנה 65: \"כתוב\" בתיקיית משתמש שבתוך תיקיית התוכנה — נכתב", "ב".equals(read(new File(user65, "שלי-65.txt"))));
            check("מבנה 65: \"כתוב\" לתוך הסל — שגיאה כבר בתכנון",
                    Batch.plan("כתוב \"" + in65 + "\\" + Index.TRASH + "\\x.txt\" \"א\"\n", ext64, v64, lib64).errors == 1);
            check("מבנה 65: \"כתוב\" על קובץ הגדרות של התוכנה — שגיאה (כמו קודם)",
                    Batch.plan("כתוב \"" + in65 + "\\" + Paths.APP_DIR + "\\x.txt\" \"א\"\n", ext64, v64, lib64).errors == 1);
            // מבנה 230 (המשתמש, 16.11.2026: "מחיקת קובץ פקודות מתוך העורך" · "גם אם הוא בתוך ספריית התוכנה"): קובץ פקודות
            // ב"הפקודות שלי" — של המשתמש; כל השאר בתיקיית התוכנה — מוגן, כמו קודם
            File sc230 = Shortcuts.dir();
            boolean had230 = sc230.isDirectory();
            sc230.mkdirs();
            String sp230 = Paths.clean(sc230.getAbsolutePath());
            check("מבנה 230: קובץ פקודות ב\"הפקודות שלי\" — מותר להוציא (למחוק, להעביר); קובץ אחר שם, בתת־תיקייה שלה, בהגדרות ובקובצי המערכת — לא; לכתוב שם — כמו קודם (קובץ של התוכנה)",
                    DiskRules.cantRemove(sp230 + "/שלי.vbat") == null && DiskRules.programFile(sp230 + "/שלי.vbat") != null
                            && DiskRules.cantRemove(sp230 + "/שלי.txt") != null
                            && DiskRules.cantRemove(sp230 + "/תת/שלי.vbat") != null
                            && DiskRules.cantRemove(Paths.clean(Paths.settings().getAbsolutePath()) + "/שלי.vbat") != null
                            && DiskRules.cantRemove(Paths.clean(Paths.system().getAbsolutePath()) + "/שלי.vbat") != null
                            && DiskRules.cantRemove(sp230 + "/../שלי.vbat") != null
                            && DiskRules.cantRemove(sp230) != null);
            File f230 = new File(sc230, "למחיקה-230.vbat"), keep230 = new File(Paths.settings(), "הגדרה-230.vbat");
            try {
                Files.writeAtomic(f230, "הודעה א\n");
                Files.writeAtomic(keep230, "x\n");
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
            ix64.scan(null);
            check("מבנה 230: \"מחק\" בקובץ פקודות על קובץ ב\"הפקודות שלי\" — בלי שגיאה; על קובץ של התוכנה — שגיאה, כמו קודם",
                    Batch.plan("מחק \"" + f230.getAbsolutePath() + "\"\n", ext64, v64, lib64).errors == 0
                            && Batch.plan("מחק \"" + keep230.getAbsolutePath() + "\"\n", ext64, v64, lib64).errors == 1);
            String del230 = Manager.trashFile(f230, lib64);
            lib64.flush(5000);
            boolean tr230 = false;
            File[] t230 = new File(ext64, Index.TRASH).listFiles();
            if (t230 != null) for (File t : t230) if (t.getName().endsWith("למחיקה-230.vbat") && "הודעה א\n".equals(read(t))) tr230 = true;
            String no230 = Manager.trashFile(keep230, lib64);
            check("מבנה 230: מחיקת קובץ פקודות מ\"הפקודות שלי\" (כמו מהעורך) — הפקודה \"מחק\" דרך המנהל: לסל, עם התוכן (" + del230
                            + "); קובץ של התוכנה — מסורב (" + no230 + ") ונשאר",
                    del230 == null && !f230.exists() && tr230 && no230 != null && keep230.isFile());
            keep230.delete();
            if (t230 != null) for (File t : t230) if (t.getName().endsWith("למחיקה-230.vbat")) t.delete();   // הסל — כמו שהיה, לבדיקות שאחרי
            if (!had230) sc230.delete();                                        // וגם תיקיית הקיצורים (מבנה 132 בודק אותה כשאין)
            new File(ext64, "שורש-65.txt").delete();
            new File(user65, "שלי-65.txt").delete();
            wipe(bk64);
            wipe(data64);
            new File(plug64, "תוסף-64.html").delete();
            lib64.flush(5000);
            List<Trash.Item> mine64 = new ArrayList<Trash.Item>();                // מה שהבדיקה הזו שלחה לסל — לא נשאר לבדיקות הבאות
            for (Trash.Item it : Trash.list(lib64)) if (it.original != null && it.original.contains("-64")) mine64.add(it);
            Physical.deleteForever(lib64, mine64);
        }
        // מבנה 66 (ממצא 29): ההודעה "אין מסד לאופן … — יש: …" מונה את האופנים כמו שהקובץ רואה אותם
        java.util.Set<String> made66 = new java.util.HashSet<String>(java.util.Arrays.asList("חדש", "ב")), gone66 = new java.util.HashSet<String>(java.util.Arrays.asList("ב", "ג"));
        eq("מבנה 66: מה שבמכשיר, בלי מה שהקובץ ביטל, ועם מה שהוא יצר (גם אחרי ביטול)", "[א, ב, חדש]",
                Sim.modes(java.util.Arrays.asList("א", "ב", "ג"), made66, gone66).toString());
        String b66 = read(new File(code, "pkudot/Batch.java")), d66 = read(new File(code, "pkudot/DbListCmd.java"));
        String all66 = readAll(new File(code, "pkudot"));                   // מבנה 168: מה שאסור — בכל תיקיית הפקודות, לא רק בשני הקבצים (הקוד מתפצל)
        check("מבנה 66: שתי ההודעות שואלות את ההדמיה, לא את המכשיר", !all66.contains("noMode(m, Db.dbModes") && !all66.contains("noMode(fromMode, Db.dbModes")
                && b66.contains("noMode(m, sim.modes())") && d66.contains("noMode(fromMode, sim.modes())"));
        // מבנה 67 (ממצאים 38, 40ו): העברה והעתקה מתוך השוואה — תיקייה שלמה עם מה שבתוכה; ו־MOVE שני על אותה השוואה אינו מנסה שוב
        {
            File d67 = new File(r, "ניסוי-67");
            wipe(d67);
            new File(d67, "א/תת").mkdirs();
            new File(d67, "ב").mkdirs();
            write(new File(d67, "א"), "לבד.txt", "1");
            write(new File(d67, "א/תת"), "x.txt", "x");
            java.util.Map<String, String> v67 = Parse.vars(d67, d67.getAbsolutePath(), null);
            Batch.Plan c67 = Batch.plan("COMP א ב\nCOPY %רק-במקור% ב\nDEL \"ב\\תת\\x.txt\"\nDEL \"א\\תת\\x.txt\"\n", d67, v67);
            check("מבנה 67: אחרי העתקה מהשוואה — קובץ שבתוך תיקייה שהועתקה נראה ביעד, והמקור נשאר (" + c67.errorText() + ")", c67.errors == 0);
            Batch.Plan m67 = Batch.plan("COMP א ב\nMOVE %רק-במקור% ב\nDEL \"ב\\תת\\x.txt\"\n", d67, v67);
            check("מבנה 67: אחרי העברה מהשוואה — הקובץ נראה במקום החדש (" + m67.errorText() + ")", m67.errors == 0);
            check("מבנה 67: ...ובמקום הישן כבר לא", Batch.plan("COMP א ב\nMOVE %רק-במקור% ב\nDEL \"א\\תת\\x.txt\"\n", d67, v67).errors == 1);
            Index ix67 = new Index("67", "67", d67.getAbsolutePath());
            ix67.scan(null);
            Library lib67 = Library.forTests(new ArrayList<Index>(java.util.Collections.singletonList(ix67)));
            Batch.Plan t67 = Batch.plan("COMP א ב\nMOVE %רק-במקור% ב\nMOVE %רק-במקור% ב\n", d67, v67, lib67);
            Batch.Step second67 = t67.steps.get(t67.steps.size() - 1);
            check("מבנה 67: MOVE שני על אותה השוואה — אין מה להעביר (עד כאן: אותם קבצים שוב, וכישלון בהרצה)",
                    t67.errors == 0 && second67.files.isEmpty() && second67.dirs.isEmpty());
            String rep67 = String.valueOf(Batch.run(t67, lib67, null, null));
            lib67.flush(5000);
            check("מבנה 67: ...וההרצה עוברת בלי כישלון, והכול ביעד (" + rep67 + ")", !t67.trouble && "1".equals(read(new File(d67, "ב/לבד.txt")))
                    && "x".equals(read(new File(d67, "ב/תת/x.txt"))) && !new File(d67, "א/לבד.txt").exists());
            wipe(d67);
        }
        // מבנה 73 (שלב ב — כל פעולה של מסך דרך פקודה): מה שנבחר במסך נמסר כיחידה, ו"מחק %נבחרים%" הוא שורה אחת
        {
            File d73 = new File(r, "ניסוי-73");
            wipe(d73);
            new File(d73, "תת/עמוק").mkdirs();
            write(d73, "א.txt", "1");
            write(d73, "ב.txt", "2");
            write(d73, "נשאר.txt", "3");
            write(new File(d73, "תת"), "ג.txt", "4");
            write(new File(d73, "תת/עמוק"), "ד.txt", "5");
            Index ix73 = new Index("73", "73", d73.getAbsolutePath());
            ix73.scan(null);
            Library lib73 = Library.forTests(new ArrayList<Index>(java.util.Collections.singletonList(ix73)));
            java.util.Map<String, String> v73 = Parse.vars(d73, d73.getAbsolutePath(), null);
            Batch.Selection sel73 = new Batch.Selection(java.util.Arrays.asList(new File(d73, "א.txt").getAbsolutePath(), new File(d73, "ב.txt").getAbsolutePath()),
                    java.util.Collections.singletonList(new File(d73, "תת").getAbsolutePath()));
            Batch.Plan p73 = Batch.plan("מחק %נבחרים%\n", d73, v73, lib73, null, sel73);
            Batch.Step s73 = p73.steps.isEmpty() ? null : p73.steps.get(0);
            check("מבנה 73: \"מחק %נבחרים%\" — שורה אחת, עם שני הקבצים והתיקייה (" + p73.errorText() + ")", p73.errors == 0 && p73.steps.size() == 1
                    && s73.files.size() == 2 && s73.dirs.size() == 1 && s73.describe.contains("2") && s73.describe.contains(Words.folders(1)));
            check("מבנה 73: בהדמיה — מה שבתוך התיקייה שנבחרה כבר איננו; מה שלא נבחר נשאר",
                    Batch.plan("מחק %נבחרים%\nDEL \"תת\\ג.txt\"\n", d73, v73, lib73, null, sel73).errors == 1
                            && Batch.plan("מחק %נבחרים%\nDEL \"נשאר.txt\"\n", d73, v73, lib73, null, sel73).errors == 0);
            check("מבנה 73: בלי בחירה — %נבחרים% אינו מוכר (כמו שהיה)", Batch.plan("מחק %נבחרים%\n", d73, v73, lib73).errors == 1);
            check("מבנה 73: %נבחרים% — רק לבדו, ורק כמקור של \"מחק\", \"העתק\", \"העבר\", \"שנה\" (מבנה 77, 78)",
                    String.valueOf(Batch.plan("RD %נבחרים%\n", d73, v73, lib73, null, sel73).errorText()).contains("לבדו")
                            && String.valueOf(Batch.plan("העתק \"א.txt\" %נבחרים%\n", d73, v73, lib73, null, sel73).errorText()).contains("לבדו")
                            && Batch.plan("מחק \"%נבחרים%\\x.txt\"\n", d73, v73, lib73, null, sel73).errors == 1);
            check("מבנה 73: סינון עם תיקייה שנבחרה — שגיאה; עם קבצים בלבד — עובד",
                    Batch.plan("מחק %נבחרים% /MINAGE:1\n", d73, v73, lib73, null, sel73).errors == 1
                            && Batch.plan("מחק %נבחרים% /MINAGE:1\n", d73, v73, lib73, null,
                            new Batch.Selection(sel73.files, new ArrayList<String>())).steps.get(0).files.isEmpty());
            check("מבנה 73: קובץ שנבחר וכבר איננו — שגיאה, ולא מחיקה של השאר", Batch.plan("מחק %נבחרים%\n", d73, v73, lib73, null,
                    new Batch.Selection(java.util.Arrays.asList(new File(d73, "א.txt").getAbsolutePath(), new File(d73, "אין.txt").getAbsolutePath()), new ArrayList<String>())).errors == 1);
            check("מבנה 73: בחירה ריקה — שגיאה", Batch.plan("מחק %נבחרים%\n", d73, v73, lib73, null,
                    new Batch.Selection(new ArrayList<String>(), new ArrayList<String>())).errors == 1);
            List<String> rep73 = Batch.run(p73, lib73, null, null);
            lib73.flush(5000);
            check("מבנה 73: בהרצה — הקבצים והתיקייה בסל, מה שלא נבחר במקומו (" + rep73 + ")", !p73.trouble && !new File(d73, "א.txt").exists()
                    && !new File(d73, "ב.txt").exists() && !new File(d73, "תת").exists() && new File(d73, "נשאר.txt").isFile());
            check("מבנה 73: ...ובסל שלושה פריטים — שני קבצים ותיקייה אחת שלמה (שחזור מחזיר אותה כולה)", Trash.list(lib73).size() == 3);
            Batch.run(Batch.plan("שחזר מהסל \"תת\"\n", d73, v73, lib73), lib73, null, null);
            lib73.flush(5000);
            check("מבנה 73: ...והתיקייה חוזרת עם מה שבתוכה", "5".equals(read(new File(d73, "תת/עמוק/ד.txt"))));
            wipe(d73);
        }
        // מבנה 74: המחיקה מהמסך — דרך המנהל, כפקודה "מחק %נבחרים%"
        {
            File ext74 = Paths.data().getParentFile(), d74 = new File(ext74, "ניסוי-74");
            wipe(d74);
            new File(d74, "תיקייה").mkdirs();
            write(d74, "א.txt", "1");
            write(new File(d74, "תיקייה"), "ב.txt", "2");
            Paths.data().mkdirs();
            write(Paths.data(), "הגדרה-74.ini", "x");
            Index ix74 = new Index("74", "74", ext74.getAbsolutePath());
            ix74.scan(null);
            Library lib74 = Library.forTests(new ArrayList<Index>(java.util.Collections.singletonList(ix74)));
            java.util.Map<String, String> v74 = Parse.vars(Paths.data(), ext74.getAbsolutePath(), null);
            List<String> f74 = java.util.Collections.singletonList(new File(d74, "א.txt").getAbsolutePath()),
                    dirs74 = java.util.Collections.singletonList(new File(d74, "תיקייה").getAbsolutePath()), none74 = new ArrayList<String>();
            Manager.Act own74 = Manager.onSelected("DEL", java.util.Collections.singletonList(new File(Paths.data(), "הגדרה-74.ini").getAbsolutePath()), none74, Paths.data(), v74, lib74);
            check("מבנה 74: קובץ של התוכנה — הסירוב בא מהתכנון, בנוסח של המסך (" + own74.error() + ")", own74.error() != null
                    && own74.error().startsWith(Words.Disk.CANT_DELETE + "הגדרה-74.ini") && new File(Paths.data(), "הגדרה-74.ini").isFile());
            Manager.Act app74 = Manager.onSelected("DEL", none74, java.util.Collections.singletonList(Paths.data().getAbsolutePath()), Paths.data(), v74, lib74);
            check("מבנה 74: תיקיית התוכנה — סירוב מהתכנון", app74.error() != null && app74.error().contains(Paths.data().getName()));
            Manager.Act act74 = Manager.onSelected("DEL", f74, dirs74, Paths.data(), v74, lib74);
            check("מבנה 74: הפקודה שהמסך מריץ — \"מחק %נבחרים%\", בלי סירוב", act74.error() == null && act74.text.trim().equals("מחק %נבחרים%"));
            Physical.Result r74 = act74.run(lib74, null, null, false);
            lib74.flush(5000);
            check("מבנה 74: ההרצה מחזירה למסך את מה שיצא — שני פריטים, בלי כישלון", r74.failed == 0 && r74.done.size() == 2
                    && !new File(d74, "א.txt").exists() && !new File(d74, "תיקייה").exists());
            check("מבנה 74: הודעת המסך נבנית מאותה תוצאה", DiskSay.message(Words.Disk.DELETED, Words.Disk.REMOVED, r74).contains(Words.folders(1)) && r74.dirs == 1);
            Physical.Result again74 = act74.run(lib74, null, null, false);
            check("מבנה 74: הרצה שנייה של אותה פעולה (הקבצים כבר אינם) — כישלון שמדווח, לא חריגה", again74.failed > 0 || again74.done.isEmpty());
            String pui74 = read(new File(code, "masachim/rechivim/PhysicalUi.java"));
            check("מבנה 74: במסכים — אין עוד מחיקה ישירה מהכונן; רק דרך המנהל", !pui74.contains("Physical.delete(") && pui74.contains("Manager.onSelected(\"DEL\""));
            List<Trash.Item> mine74 = new ArrayList<Trash.Item>();
            for (Trash.Item it : Trash.list(lib74)) if (it.original != null && it.original.contains("-74")) mine74.add(it);
            Physical.deleteForever(lib74, mine74);
            new File(Paths.data(), "הגדרה-74.ini").delete();
            wipe(d74);
        }
        // מבנה 75 (המשתמש, 15.11.2026: "צריך לרשום בתור הוראות תכנות וסדר. לכל מי שמשנה גרסה. ושיופיע במקום מרכזי ובולט"):
        // ההוראות בשורש המאגר — קיימות, שלמות, ודף הפתיחה מפנה אליהן. מי שמוחק או מרוקן אותן — הבנייה נעצרת.
        {
            File root75 = new File(System.getProperty("vfm.src")).getParentFile();
            File rules75 = new File(root75, "CLAUDE.md");
            String t75 = rules75.isFile() ? read(rules75) : "";
            boolean floors75 = true;
            for (String f : MivneTests.LEVELS) floors75 &= t75.contains("`" + f + "`");
            check("מבנה 75: הוראות התכנות והסדר — בשורש המאגר (CLAUDE.md), עם חמש הקומות", rules75.isFile() && floors75);
            check("מבנה 75: ...ועם הכללים: כל צעד דרך השכבה הדקה, השערים, השומרים, סדר העבודה",
                    t75.contains("מסך אינו משנה דבר בעצמו") && t75.contains("Db.write") && t75.contains("לא מוחקים, לא מחלישים ולא עוקפים שומר")
                            && t75.contains("בניית גרסה — רק בהוראה מפורשת של המשתמש") && t75.contains("רק באישור המשתמש"));
            check("מבנה 76: ההוראות כוללות את ההיתר, בנוסח של המשתמש — ואותו נוסח בתוכנה", t75.contains(Words.PermitW.BYPASS + ". " + Words.PermitW.REFUSED)
                    && t75.contains("basis/Permit"));
            check("מבנה 75: דף הפתיחה של המאגר מפנה אליהן בראשו", read(new File(root75, "README.md")).indexOf("CLAUDE.md") >= 0
                    && read(new File(root75, "README.md")).indexOf("CLAUDE.md") < 300);
            check("מבנה 75: הרצת הבדיקות מזכירה אותן", read(new File(root75, "source/build/test.sh")).contains("CLAUDE.md"));
        }
        // מבנה 76 (המשתמש, 15.11.2026: "פעולה מנסה לעקוף את שכבת הניהול. פעולה זו לא חוקית ואסורה. כל פעולה חייבת לעבור דרך
        // שכבת הניהול"): ההיתר — בלי היתר, מחיקה מסורבת; דרך המנהל — נעשית
        {
            File d76 = new File(r, "ניסוי-76");
            wipe(d76);
            d76.mkdirs();
            write(d76, "א.txt", "1");
            write(d76, "ב.txt", "2");
            Index ix76 = new Index("76", "76", d76.getAbsolutePath());
            ix76.scan(null);
            Library lib76 = Library.forTests(new ArrayList<Index>(java.util.Collections.singletonList(ix76)));
            java.util.Map<String, String> v76 = Parse.vars(d76, d76.getAbsolutePath(), null);
            List<String> a76 = java.util.Collections.singletonList(new File(d76, "א.txt").getAbsolutePath());
            check("מבנה 76: בבדיקות ההיתר פתוח (הן קוראות לפעולות ישירות)", Permit.held());
            Permit.close();                                                     // מכאן — כמו בתוכנה: אין היתר, אלא אם המנהל פותח
            try {
                check("מבנה 76: עכשיו אין היתר", !Permit.held());
                Physical.Result no76 = Physical.delete(lib76, a76, false);
                check("מבנה 76: מחיקה בלי היתר — מסורבת, והקובץ במקומו", no76.failed == 1 && no76.done.isEmpty() && new File(d76, "א.txt").isFile());
                check("מבנה 76: ...בנוסח של המשתמש (" + no76.note + ")", no76.note.contains("פעולה מנסה לעקוף את שכבת הניהול")
                        && no76.note.contains("פעולה זו לא חוקית ואסורה. כל פעולה חייבת לעבור דרך שכבת הניהול."));
                Batch.Plan p76 = Batch.plan("DEL \"א.txt\"\n", d76, v76, lib76);
                Batch.run(p76, lib76, null, null);
                check("מבנה 76: גם המנוע, כשמריצים אותו שלא דרך המנהל — לא מוחק, והשורה נכשלת", new File(d76, "א.txt").isFile() && p76.trouble);
                check("מבנה 76: פעולה שעוד במעבר — אינה מסורבת, רק נרשמת ביומן (מצעד 81 אין כזו בכונן: כל פעולות הכונן מסורבות בלי היתר)",
                        Permit.check("פעולת בדיקה שעוד במעבר") == null && !Permit.strict("פעולת בדיקה שעוד במעבר")
                                && Permit.strict(Permit.DELETE) && Permit.strict(Permit.TRANSFER) && Permit.strict(Permit.RENAME)
                                && Permit.strict(Permit.MAKE_FOLDER) && Permit.strict(Permit.WRITE) && Permit.strict(Permit.RESTORE)
                                && Permit.strict(Permit.FOREVER));
                Manager.Act act76 = Manager.onSelected("DEL", a76, new ArrayList<String>(), d76, v76, lib76);
                Physical.Result yes76 = act76.run(lib76, null, null, false);
                lib76.flush(5000);
                check("מבנה 76: דרך המנהל — המחיקה נעשית", yes76.failed == 0 && yes76.done.size() == 1 && !new File(d76, "א.txt").exists());
                check("מבנה 76: ...וההיתר נסגר אחריה", !Permit.held());
                Batch.Plan q76 = Batch.plan("DEL \"ב.txt\"\n", d76, v76, lib76);
                Manager.run(q76, lib76, null, null, false, null);
                lib76.flush(5000);
                check("מבנה 76: קובץ פקודות שהמנהל מריץ — מוחק", !new File(d76, "ב.txt").exists() && !q76.trouble);
            } finally {
                Permit.open();
            }
            String man76 = read(new File(code, "lev/Manager.java"));
            int opens76 = 0, runs76 = 0;
            List<File> all76 = new ArrayList<File>();
            collect76(code, all76);
            for (File f : all76) {
                String t = read(f), n = f.getName();
                if (t.contains("Permit.open(") && !n.equals("Manager.java") && !n.equals("Permit.java")) opens76++;
                if (t.contains("Batch.run(") && !n.equals("Manager.java") && !n.equals("Batch.java")) runs76++;
            }
            check("מבנה 76: רק המנהל פותח היתר (" + opens76 + " מקומות אחרים)", opens76 == 0 && man76.contains("Permit.open();") && man76.contains("Permit.close();"));
            check("מבנה 76: רק המנהל מריץ את המנוע (" + runs76 + " מקומות אחרים)", runs76 == 0);
            wipe(d76);
        }
        // מבנה 77: ההעתקה וההעברה מהמסך — דרך המנהל, כפקודה "העתק / העבר %נבחרים% "%יעד%""
        {
            File ext77 = Paths.data().getParentFile(), d77 = new File(ext77, "ניסוי-77");
            wipe(d77);
            new File(d77, "תיקייה/פנים").mkdirs();
            File to77 = new File(d77, "יעד"), pc77 = new File(d77, "יעד 100% מלא"), trash77 = new File(ext77, Index.TRASH);
            to77.mkdirs();
            pc77.mkdirs();
            trash77.mkdirs();
            write(d77, "א.txt", "1");
            write(d77, "ב.txt", "2");
            write(new File(d77, "תיקייה"), "ג.txt", "3");
            Paths.data().mkdirs();
            write(Paths.data(), "הגדרה-77.ini", "x");
            Index ix77 = new Index("77", "77", ext77.getAbsolutePath());
            ix77.scan(null);
            Library lib77 = Library.forTests(new ArrayList<Index>(java.util.Collections.singletonList(ix77)));
            java.util.Map<String, String> v77 = Parse.vars(Paths.data(), ext77.getAbsolutePath(), null);
            String a77 = new File(d77, "א.txt").getAbsolutePath(), b77 = new File(d77, "ב.txt").getAbsolutePath(),
                    dir77 = new File(d77, "תיקייה").getAbsolutePath(), own77 = new File(Paths.data(), "הגדרה-77.ini").getAbsolutePath();
            List<String> f77 = java.util.Arrays.asList(a77, b77), dirs77 = java.util.Collections.singletonList(dir77), none77 = new ArrayList<String>();
            // המנוע
            Batch.Selection sel77 = new Batch.Selection(f77, dirs77, to77);
            Batch.Plan p77 = Batch.plan("העתק %נבחרים% \"%יעד%\"\n", d77, v77, lib77, null, sel77);
            Batch.Step s77 = p77.steps.isEmpty() ? null : p77.steps.get(0);
            check("מבנה 77: \"העתק %נבחרים% %יעד%\" — שורה אחת: שני קבצים, והתיקייה שלמה גם בהעתקה (" + p77.errorText() + ")",
                    p77.errors == 0 && p77.steps.size() == 1 && s77.files.size() == 2 && s77.dirs.size() == 1 && to77.equals(s77.dest));
            check("מבנה 77: בהדמיה — התיקייה ביעד עם מה שבתוכה, והמקור במקומו", p77.sim.isFile(new File(to77, "תיקייה/ג.txt"))
                    && p77.sim.isDir(new File(to77, "תיקייה/פנים")) && p77.sim.isFile(new File(dir77, "ג.txt")) && p77.sim.isFile(new File(to77, "א.txt")));
            Batch.Plan m77 = Batch.plan("העבר %נבחרים% \"%יעד%\"\n", d77, v77, lib77, null, sel77);
            check("מבנה 77: בהעברה, בהדמיה — המקור כבר איננו", m77.errors == 0 && !m77.sim.isFile(new File(a77)) && !m77.sim.isDir(new File(dir77))
                    && m77.sim.isFile(new File(to77, "תיקייה/ג.txt")));
            check("מבנה 77: %נבחרים% — לא עם /S ולא לשיתוף",
                    String.valueOf(Batch.plan("העתק %נבחרים% \"%יעד%\" /S\n", d77, v77, lib77, null, sel77).errorText()).contains(Words.Cmd.SELECTED_PLAIN)
                            && String.valueOf(Batch.plan("העתק %נבחרים% %שיתוף%\n", d77, v77, lib77, null, sel77).errorText()).contains(Words.Cmd.SELECTED_PLAIN));
            check("מבנה 77: בלי יעד בבחירה — %יעד% אינו מוכר", Batch.plan("העתק %נבחרים% \"%יעד%\"\n", d77, v77, lib77, null, new Batch.Selection(f77, dirs77)).errors == 1);
            // הסירובים — מהתכנון, בנוסח של המסך
            Manager.Act app77 = Manager.onSelected("COPY", f77, none77, Paths.data(), Paths.data(), v77, lib77);
            check("מבנה 77: יעד בתיקיית התוכנה — סירוב מהתכנון (" + app77.error() + ")", Words.Disk.DEST_APP_NOT.equals(app77.error()));
            Manager.Act tr77 = Manager.onSelected("MOVE", f77, none77, trash77, Paths.data(), v77, lib77);
            check("מבנה 77: יעד בסל — סירוב מהתכנון (" + tr77.error() + ")", Words.Disk.DEST_TRASH_NOT.equals(tr77.error()));
            Manager.Act in77 = Manager.onSelected("MOVE", none77, dirs77, new File(dir77, "פנים"), Paths.data(), v77, lib77);
            check("מבנה 77: תיקייה לתוך עצמה — סירוב מהתכנון (" + in77.error() + ")",
                    Words.Disk.NOT_INTO_ITSELF.equals(in77.error()));                // מבנה 201 (ה11): הנוסח האחד — של קובץ הפקודות
            {   // מבנה 201 (ה11, המשתמש 16.11.2026): השורה הכתובה — אותם סירובים כמו מהמסך, כבר בתכנון, ובאותו נוסח
                String mv201 = Commands.word(Commands.get("MOVE")), del201 = Commands.word(Commands.get("DEL")), cp201 = Commands.word(Commands.get("COPY"));
                Batch.Plan pTrash = Batch.plan(mv201 + " \"" + f77.get(0) + "\" \"" + trash77.getAbsolutePath() + "\"\n", Paths.data(), v77, lib77, null);
                Batch.Plan pOwn = Batch.plan(mv201 + " \"" + own77 + "\" \"" + to77.getAbsolutePath() + "\"\n", Paths.data(), v77, lib77, null);
                Batch.Plan pDel = Batch.plan(del201 + " \"" + own77 + "\"\n", Paths.data(), v77, lib77, null);
                Batch.Plan pCopy = Batch.plan(cp201 + " \"" + own77 + "\" \"" + to77.getAbsolutePath() + "\"\n", Paths.data(), v77, lib77, null);
                check("מבנה 201: שורה כתובה — יעד בסל נתפס בתכנון (עד כאן: רק בהרצה), באותו נוסח כמו מהמסך (" + pTrash.steps.get(0).error + ")",
                        tr77.error() != null && tr77.error().equals(pTrash.steps.get(0).error) && pTrash.blocked());
                check("מבנה 201: שורה כתובה — להעביר קובץ של התוכנה: נתפס בתכנון, באותו נוסח כמו מהמסך (" + pOwn.steps.get(0).error + ")",
                        pOwn.steps.get(0).error != null && pOwn.steps.get(0).error.equals(Manager.onSelected("MOVE", java.util.Collections.singletonList(own77),
                                none77, to77, Paths.data(), v77, lib77).error()) && pOwn.blocked());
                check("מבנה 201: שורה כתובה — למחוק קובץ של התוכנה: נתפס בתכנון (" + pDel.steps.get(0).error + ")",
                        pDel.steps.get(0).error != null && pDel.steps.get(0).error.startsWith(Words.Disk.CANT_DELETE + "הגדרה-77.ini") && pDel.blocked());
                check("מבנה 201: להעתיק קובץ של התוכנה — מותר, גם בשורה כתובה", pCopy.steps.get(0).error == null);
            }
            Manager.Act mo77 = Manager.onSelected("MOVE", java.util.Collections.singletonList(own77), none77, to77, Paths.data(), v77, lib77);
            check("מבנה 77: להעביר קובץ של התוכנה — סירוב מהתכנון (" + mo77.error() + ")", mo77.error() != null
                    && mo77.error().startsWith(Words.Disk.CANT_MOVE + "הגדרה-77.ini"));
            check("מבנה 77: ...אותו נוסח שהמסך אמר עד כאן", mo77.error().equals(DiskRules.cannotMove(lib77, java.util.Collections.singletonList(own77), none77)));
            check("מבנה 77: להעתיק קובץ של התוכנה — מותר", Manager.onSelected("COPY", java.util.Collections.singletonList(own77), none77, to77, Paths.data(), v77, lib77).error() == null);
            check("מבנה 77: פריט שנבחר וכבר איננו — סירוב מהתכנון", Manager.onSelected("COPY", java.util.Collections.singletonList(new File(d77, "אין.txt").getAbsolutePath()),
                    none77, to77, Paths.data(), v77, lib77).error() != null);
            // ההרצה, בלי היתר פתוח מבחוץ — כמו בתוכנה
            Permit.close();
            try {
                Physical.Result no77 = Physical.transfer(lib77, f77, none77, to77, true, null, null, false);
                check("מבנה 77: העתקה בלי היתר — מסורבת, ודבר לא הגיע ליעד (" + no77.note + ")", no77.failed > 0 && no77.done.isEmpty()
                        && !new File(to77, "א.txt").exists() && no77.note.contains(Words.PermitW.BYPASS) && Permit.strict(Permit.TRANSFER));
                Manager.Act c77 = Manager.onSelected("COPY", f77, dirs77, to77, Paths.data(), v77, lib77);
                check("מבנה 77: הפקודה שהמסך מריץ — \"העתק %נבחרים% \"%יעד%\"\", בלי סירוב (" + c77.error() + ")",
                        c77.error() == null && c77.text.trim().equals("העתק %נבחרים% \"%יעד%\""));
                Physical.Result rc77 = c77.run(lib77, null, null, false);
                lib77.flush(5000);
                check("מבנה 77: ההעתקה נעשית — שלושה פריטים, התיקייה שלמה, והמקור במקומו (" + rc77.note + ")", rc77.failed == 0 && rc77.done.size() == 3
                        && "1".equals(read(new File(to77, "א.txt"))) && "3".equals(read(new File(to77, "תיקייה/ג.txt")))
                        && new File(to77, "תיקייה/פנים").isDirectory() && new File(a77).isFile() && new File(dir77, "ג.txt").isFile());
                check("מבנה 77: הודעת המסך נבנית מאותה תוצאה", rc77.dirs == 1 && DiskSay.message(Words.Disk.COPIED, Words.Disk.REMOVED, rc77).contains(Words.folders(1)));
                Physical.Result again77 = Manager.onSelected("COPY", java.util.Collections.singletonList(a77), none77, to77, Paths.data(), v77, lib77).run(lib77, null, null, false);
                check("מבנה 77: שם תפוס, ואין את מי לשאול — כישלון שמדווח, והקובץ שביעד לא נדרס", again77.failed == 1 && again77.done.isEmpty()
                        && "1".equals(read(new File(to77, "א.txt"))));
                Manager.Act mv77 = Manager.onSelected("MOVE", f77, dirs77, pc77, Paths.data(), v77, lib77);
                Physical.Result rm77 = mv77.run(lib77, null, null, false);
                lib77.flush(5000);
                check("מבנה 77: העברה אל תיקייה ששמה מכיל % — נעשית (" + mv77.error() + " · " + rm77.note + ")", mv77.error() == null && rm77.failed == 0
                        && rm77.done.size() == 3 && "2".equals(read(new File(pc77, "ב.txt"))) && "3".equals(read(new File(pc77, "תיקייה/ג.txt")))
                        && !new File(a77).exists() && !new File(dir77).exists());
                check("מבנה 77: ...והתוצאה אומרת לאן כל פריט הגיע", new File(pc77, "א.txt").getAbsolutePath().equals(rm77.to.get(a77)));
                write(d77, "חדש.txt", "9");
                ix77.scan(null);
                Physical.Result here77 = Manager.onSelected("MOVE", java.util.Arrays.asList(new File(pc77, "א.txt").getAbsolutePath(), new File(d77, "חדש.txt").getAbsolutePath()),
                        none77, pc77, Paths.data(), v77, lib77).run(lib77, null, null, false);
                lib77.flush(5000);
                check("מבנה 77: פריט שכבר בתיקיית היעד — \"כבר בתיקייה הזו\", כמו שהמסך אמר; השאר עובר (" + here77.note + ")", here77.failed == 1
                        && here77.done.size() == 1 && Words.Disk.ALREADY_HERE.equals(here77.note) && new File(pc77, "חדש.txt").isFile() && new File(pc77, "א.txt").isFile());
                check("מבנה 77: ההיתר נסגר אחרי כל הרצה", !Permit.held());
            } finally {
                Permit.open();
            }
            String pui77 = read(new File(code, "masachim/rechivim/PhysicalUi.java"));
            int direct77 = 0;
            List<File> all77 = new ArrayList<File>();
            collect76(new File(code, "masachim"), all77);
            for (File f : all77) if (read(f).contains("Physical.transfer")) direct77++;
            check("מבנה 77: במסכים — אין עוד העתקה או העברה ישירה בכונן (" + direct77 + "); רק דרך המנהל", direct77 == 0
                    && pui77.contains("Manager.onSelected(copy ? \"COPY\" : \"MOVE\"") && pui77.contains("act.run(lib, new DialogAsker(a), busy, grey)"));
            new File(Paths.data(), "הגדרה-77.ini").delete();
            wipe(d77);
        }
        // מבנה 78: שינוי השם מהמסך — דרך המנהל, כפקודה "שנה %נבחרים% "%שם-חדש%""
        {
            File ext78 = Paths.data().getParentFile(), d78 = new File(ext78, "ניסוי-78");
            wipe(d78);
            new File(d78, "תיקייה").mkdirs();
            write(d78, "א.txt", "1");
            write(d78, "תפוס.txt", "2");
            Paths.data().mkdirs();
            write(Paths.data(), "הגדרה-78.ini", "x");
            Index ix78 = new Index("78", "78", ext78.getAbsolutePath());
            ix78.scan(null);
            Library lib78 = Library.forTests(new ArrayList<Index>(java.util.Collections.singletonList(ix78)));
            java.util.Map<String, String> v78 = Parse.vars(Paths.data(), ext78.getAbsolutePath(), null);
            String a78 = new File(d78, "א.txt").getAbsolutePath(), own78 = new File(Paths.data(), "הגדרה-78.ini").getAbsolutePath();
            Manager.Act can78 = Manager.onRename(a78, "א.txt", Paths.data(), v78, lib78);
            check("מבנה 78: לפני שמבקשים שם — \"מותר לשנות לקובץ הזה שם?\": כן, ואין מה לעשות (" + can78.error() + ")",
                    can78.error() == null && can78.plan.steps.size() == 1 && can78.plan.steps.get(0).noop
                            && can78.text.trim().equals("שנה %נבחרים% \"%שם-חדש%\""));
            Manager.Act own78a = Manager.onRename(own78, "הגדרה-78.ini", Paths.data(), v78, lib78);
            check("מבנה 78: קובץ של התוכנה — סירוב מהתכנון, בנוסח של המסך (" + own78a.error() + ")", own78a.error() != null
                    && own78a.error().equals(Words.Disk.CANT_RENAME + "הגדרה-78.ini\": " + DiskRules.programFile(own78)));
            // מבנה 196 (ה8, המשתמש 16.11.2026): הנוסח האחד — של קובץ הפקודות, גם מהמסך
            check("מבנה 78 / 196: קובץ שכבר איננו — סירוב מהתכנון, בנוסח האחד", (Words.FILE_NOT_FOUND + ": אין.txt").equals(Manager.onRename(new File(d78, "אין.txt").getAbsolutePath(), "אין.txt",
                    Paths.data(), v78, lib78).error()));
            check("מבנה 78 / 196: שם תפוס — סירוב מהתכנון, בנוסח האחד", Words.Cmd.alreadyHas("תפוס.txt").equals(
                    Manager.onRename(a78, "תפוס.txt", Paths.data(), v78, lib78).error())
                    && Words.Cmd.alreadyHas("תיקייה").equals(Manager.onRename(a78, "תיקייה", Paths.data(), v78, lib78).error()));
            {   // מבנה 196 (ה8): השורה הכתובה והמסך — אותו כלל, אותו נוסח, ובאותו רגע: בתכנון, לפני שמשהו רץ
                String ren196 = Commands.word(Commands.get("REN"));
                Batch.Plan pOwn = Batch.plan(ren196 + " \"" + own78 + "\" \"אחר-196.ini\"\n", Paths.data(), v78, lib78, null);
                Batch.Plan pBad = Batch.plan(ren196 + " \"" + a78 + "\" \"א:ב.txt\"\n", Paths.data(), v78, lib78, null);
                Batch.Plan pTaken = Batch.plan(ren196 + " \"" + a78 + "\" \"תפוס.txt\"\n", Paths.data(), v78, lib78, null);
                Batch.Plan pSame = Batch.plan(ren196 + " \"" + a78 + "\" \"א.txt\"\n", Paths.data(), v78, lib78, null);
                String e196 = pOwn.steps.get(0).error;
                check("מבנה 196: שורה כתובה — קובץ של התוכנה נתפס כבר בתכנון (עד כאן: רק בהרצה), והקובץ אינו רץ (" + e196 + ")",
                        e196 != null && e196.equals(Manager.onRename(own78, "אחר-196.ini", Paths.data(), v78, lib78).error()) && pOwn.blocked());
                check("מבנה 196: שורה כתובה — שם לא תקין נתפס בתכנון (" + pBad.steps.get(0).error + ")",
                        pBad.steps.get(0).error != null && pBad.steps.get(0).error.equals(DiskRules.badName("א:ב.txt")) && pBad.blocked());
                check("מבנה 196: שם תפוס — אותו נוסח בשורה הכתובה ומהמסך",
                        Words.Cmd.alreadyHas("תפוס.txt").equals(pTaken.steps.get(0).error));
                check("מבנה 196: אותו שם — אין מה לעשות, בשתי הדרכים (בלי שגיאה)", pSame.steps.get(0).error == null && pSame.steps.get(0).noop
                        && Manager.onRename(a78, "א.txt", Paths.data(), v78, lib78).error() == null);
            }
            check("מבנה 78: שם לא תקין — אותו סירוב של הכונן", DiskRules.badName("א/ב.txt") != null
                    && DiskRules.badName("א/ב.txt").equals(Manager.onRename(a78, "א/ב.txt", Paths.data(), v78, lib78).error())
                    && Manager.onRename(a78, "  ", Paths.data(), v78, lib78).error() != null);
            check("מבנה 78: במנוע — %נבחרים% של \"שנה\" הוא קובץ אחד בלבד; ותיקייה — לא",
                    Batch.plan("שנה %נבחרים% \"%שם-חדש%\"\n", d78, v78, lib78, null, new Batch.Selection(java.util.Arrays.asList(a78,
                            new File(d78, "תפוס.txt").getAbsolutePath()), new ArrayList<String>(), null, "x.txt")).errors == 1
                            && Batch.plan("שנה %נבחרים% \"%שם-חדש%\"\n", d78, v78, lib78, null, new Batch.Selection(new ArrayList<String>(),
                            java.util.Collections.singletonList(new File(d78, "תיקייה").getAbsolutePath()), null, "x")).errors == 1
                            && Batch.plan("שנה %נבחרים% \"x.txt\"\n", d78, v78, lib78, null, new Batch.Selection(java.util.Collections.singletonList(a78),
                            new ArrayList<String>(), null, "y.txt")).errors == 1);
            Permit.close();
            try {
                Physical.Result no78 = Physical.rename(lib78, a78, "ב.txt");
                check("מבנה 78: שינוי שם בלי היתר — מסורב, והקובץ בשמו (" + no78.note + ")", no78.failed == 1 && new File(a78).isFile()
                        && !new File(d78, "ב.txt").exists() && no78.note.contains(Words.PermitW.BYPASS) && Permit.strict(Permit.RENAME));
                String odd78 = "100% %תאריך% חדש.txt";
                Manager.Act act78 = Manager.onRename(a78, "  " + odd78 + " ", Paths.data(), v78, lib78);
                Physical.Result r78 = act78.run(lib78, null, null, false);
                lib78.flush(5000);
                check("מבנה 78: דרך המנהל — נעשה; שם עם % ועם טקסט שנראה כמו משתנה נשמר כלשונו (" + act78.error() + " · " + r78.note + ")",
                        act78.error() == null && r78.failed == 0 && r78.done.size() == 1 && "1".equals(read(new File(d78, odd78))) && !new File(a78).exists());
                check("מבנה 78: ...והתוצאה אומרת לאן", new File(d78, odd78).getAbsolutePath().equals(r78.to.get(a78)));
                Physical.Result again78 = act78.run(lib78, null, null, false);
                check("מבנה 78: הרצה שנייה (הקובץ כבר בשם החדש) — כישלון שמדווח, לא חריגה", again78.failed == 1 && !again78.note.isEmpty());
                Physical.Result case78 = Manager.onRename(new File(d78, "תפוס.txt").getAbsolutePath(), "תפוס.TXT", Paths.data(), v78, lib78).run(lib78, null, null, false);
                check("מבנה 78: שינוי של אותיות גדולות וקטנות בלבד — נעשה", case78.failed == 0 && new File(d78, "תפוס.TXT").isFile());
                check("מבנה 78: ההיתר נסגר אחרי כל הרצה", !Permit.held());
            } finally {
                Permit.open();
            }
            int direct78 = 0;
            List<File> all78 = new ArrayList<File>();
            collect76(new File(code, "masachim"), all78);
            for (File f : all78) if (read(f).contains("Physical.rename")) direct78++;
            check("מבנה 78: במסכים — אין עוד שינוי שם ישיר בכונן (" + direct78 + "); רק דרך המנהל", direct78 == 0
                    && read(new File(code, "masachim/rechivim/PhysicalUi.java")).contains("Manager.onRename(path, name"));
            new File(Paths.data(), "הגדרה-78.ini").delete();
            wipe(d78);
        }
        // מבנה 79: תיקייה חדשה מהמסך — דרך המנהל, כפקודה "צור תיקייה "%יעד%\%שם-חדש%""
        {
            File ext79 = Paths.data().getParentFile(), d79 = new File(ext79, "ניסוי-79");
            wipe(d79);
            new File(d79, "קיימת").mkdirs();
            write(d79, "קובץ", "1");
            Paths.data().mkdirs();
            Index ix79 = new Index("79", "79", ext79.getAbsolutePath());
            ix79.scan(null);
            Library lib79 = Library.forTests(new ArrayList<Index>(java.util.Collections.singletonList(ix79)));
            java.util.Map<String, String> v79 = Parse.vars(Paths.data(), ext79.getAbsolutePath(), null);
            Manager.Act act79 = Manager.onMakeFolder(d79, "  חדשה 100% %תאריך% ", Paths.data(), v79, lib79);
            check("מבנה 79: הפקודה שהמסך מריץ — \"צור תיקייה \"%יעד%\\%שם-חדש%\"\", בלי סירוב (" + act79.error() + ")", act79.error() == null
                    && act79.text.trim().equals("צור תיקייה \"%יעד%\\%שם-חדש%\"") && act79.plan.steps.size() == 1
                    && new File(d79, "חדשה 100% %תאריך%").equals(act79.plan.steps.get(0).dest));
            // מבנה 197 (ה9, המשתמש 16.11.2026): הנוסח האחד — של קובץ הפקודות, גם מהמסך
            check("מבנה 79 / 197: שם שכבר קיים (תיקייה או קובץ) — סירוב מהתכנון, בנוסח האחד",
                    Words.Cmd.folderTaken("קיימת").equals(Manager.onMakeFolder(d79, "קיימת", Paths.data(), v79, lib79).error())
                            && Words.Cmd.exists("קובץ").equals(Manager.onMakeFolder(d79, "קובץ", Paths.data(), v79, lib79).error()));
            {   // מבנה 197 (ה9): השורה הכתובה — שם לא תקין ותיקיית התוכנה נתפסים בתכנון, באותו נוסח כמו מהמסך
                String md197 = Commands.word(Commands.get("MD"));
                Batch.Plan pBad = Batch.plan(md197 + " \"" + d79 + "\\א:ב\"\n", Paths.data(), v79, lib79, null);
                Batch.Plan pApp = Batch.plan(md197 + " \"" + new File(Paths.data(), "בפנים-197").getAbsolutePath() + "\"\n", Paths.data(), v79, lib79, null);
                Batch.Plan pTaken = Batch.plan(md197 + " \"" + d79 + "\\קיימת\"\n", Paths.data(), v79, lib79, null);
                check("מבנה 197: שורה כתובה — שם לא תקין נתפס בתכנון (עד כאן: רק בהרצה), והקובץ אינו רץ (" + pBad.steps.get(0).error + ")",
                        pBad.steps.get(0).error != null && pBad.steps.get(0).error.equals(DiskRules.badName("א:ב")) && pBad.blocked());
                check("מבנה 197: שורה כתובה — תיקייה בתוך תיקיית התוכנה נתפסת בתכנון, כמו מהמסך (" + pApp.steps.get(0).error + ")",
                        Words.Disk.NO_FOLDER_IN_APP.equals(pApp.steps.get(0).error) && pApp.blocked());
                check("מבנה 197: תיקייה קיימת — אותו נוסח בשורה הכתובה ומהמסך",
                        pTaken.steps.get(0).error != null && pTaken.steps.get(0).error.equals(Manager.onMakeFolder(d79, "קיימת", Paths.data(), v79, lib79).error()));
            }
            check("מבנה 79: שם לא תקין — אותו סירוב של הכונן; גם שם ריק", DiskRules.badName("א/ב") != null
                    && DiskRules.badName("א/ב").equals(Manager.onMakeFolder(d79, "א/ב", Paths.data(), v79, lib79).error())
                    && Manager.onMakeFolder(d79, " ", Paths.data(), v79, lib79).error() != null);
            check("מבנה 79: בתוך תיקיית התוכנה — סירוב מהתכנון", Words.Disk.NO_FOLDER_IN_APP.equals(
                    Manager.onMakeFolder(Paths.data(), "חדשה-79", Paths.data(), v79, lib79).error()));
            check("מבנה 79: במנוע — %שם-חדש% בלי בחירה אינו מוכר; מבנה 109: \"צור תיקייה\" כתובה, קיימת — סירוב; עם /תפוס:לדלג — בלי שינוי",
                    Batch.plan("צור תיקייה \"%יעד%\\%שם-חדש%\"\n", d79, v79, lib79).errors == 1
                            && Batch.plan("צור תיקייה \"קיימת\"\n", d79, v79, lib79).errors == 1
                            && Batch.plan("צור תיקייה \"קיימת\" /תפוס:לדלג\n", d79, v79, lib79).steps.get(0).noop);
            Permit.close();
            try {
                String no79 = Physical.makeFolder(lib79, d79, "עוקפת");
                check("מבנה 79: תיקייה חדשה בלי היתר — מסורבת, ולא נוצרה (" + no79 + ")", no79 != null && no79.contains(Words.PermitW.BYPASS)
                        && !new File(d79, "עוקפת").exists() && Permit.strict(Permit.MAKE_FOLDER));
                Physical.Result r79 = act79.run(lib79, null, null, false);
                check("מבנה 79: דרך המנהל — נוצרה, בשם שהוקלד כלשונו (עם % ועם טקסט שנראה כמו משתנה)", r79.failed == 0 && r79.done.size() == 1
                        && new File(d79, "חדשה 100% %תאריך%").isDirectory());
                Physical.Result again79 = act79.run(lib79, null, null, false);
                check("מבנה 79: הרצה שנייה (כבר קיימת) — כישלון שמדווח, בנוסח של הכונן (" + again79.note + ")", again79.failed == 1
                        && again79.note.startsWith(Words.Disk.ALREADY_HERE_NAME));
                check("מבנה 79: ההיתר נסגר אחרי כל הרצה", !Permit.held());
            } finally {
                Permit.open();
            }
            int direct79 = 0;
            List<File> all79 = new ArrayList<File>();
            collect76(new File(code, "masachim"), all79);
            for (File f : all79) if (read(f).contains("Physical.makeFolder")) direct79++;
            check("מבנה 79: במסכים — אין עוד יצירת תיקייה ישירה בכונן (" + direct79 + "); רק דרך המנהל", direct79 == 0
                    && read(new File(code, "masachim/rechivim/PhysicalUi.java")).contains("Manager.onMakeFolder(parent, name"));
            wipe(d79);
        }
        // מבנה 80: קובץ חדש מהמסך — דרך המנהל, כפקודה "כתוב "%יעד%\%שם-חדש%" "%תוכן%""
        {
            File ext80 = Paths.data().getParentFile(), d80 = new File(ext80, "ניסוי-80");
            wipe(d80);
            d80.mkdirs();
            write(d80, "קיים.vbat", "ישן");
            Paths.data().mkdirs();
            Index ix80 = new Index("80", "80", ext80.getAbsolutePath());
            ix80.scan(null);
            Library lib80 = Library.forTests(new ArrayList<Index>(java.util.Collections.singletonList(ix80)));
            java.util.Map<String, String> v80 = Parse.vars(Paths.data(), ext80.getAbsolutePath(), null);
            String body80 = "REM %פנימי% \"מירכאות\"\nשורה שנייה \\n לא שורה חדשה\n" + Batch.template();
            File new80 = new File(d80, "חדש 100%.vbat"), deep80 = new File(d80, "תיקייה חדשה/עמוק.vbat");
            Manager.Act act80 = Manager.onNewFile(new80, body80, Paths.data(), v80, lib80);
            check("מבנה 80: הפקודה שהמסך מריץ — \"כתוב \"%יעד%\\%שם-חדש%\" \"%תוכן%\"\", בלי סירוב (" + act80.error() + ")", act80.error() == null
                    && act80.text.trim().equals("כתוב \"%יעד%\\%שם-חדש%\" \"%תוכן%\"") && new80.equals(act80.plan.steps.get(0).dest));
            check("מבנה 110: קובץ שכבר קיים — מהמסך אותה שגיאה כמו בשורה (\"שם קיים\", והמסך שואל)", Words.Cmd.fileTaken("קיים.vbat").equals(
                    Manager.onNewFile(new File(d80, "קיים.vbat"), "x", Paths.data(), v80, lib80).error()));
            Manager.Act rep110 = Manager.onNewFile(new File(d80, "קיים.vbat"), "חדש-110", Syntax.TAKEN_REPLACE, Paths.data(), v80, lib80);
            String said110 = rep110.error() == null ? String.valueOf(rep110.run(lib80, null, null, false).note) : rep110.error();
            lib80.flush(5000);
            List<Trash.Item> tr110 = SmallCmds.trashed(lib80, new File(d80, "קיים.vbat").getAbsolutePath(), false);
            check("מבנה 110: מהמסך \"להחליף\" — אותה פקודה עם /תפוס:להחליף: נכתב החדש, והקודם בסל עם התוכן שלו (" + said110 + ")",
                    rep110.text.contains("/תפוס:להחליף") && "חדש-110".equals(read(new File(d80, "קיים.vbat"))) && !tr110.isEmpty()
                    && "ישן".equals(read(new File(tr110.get(tr110.size() - 1).path()))));
            Batch.run(Batch.plan("רוקן סל \"" + new File(d80, "קיים.vbat").getAbsolutePath() + "\"\n", d80, v80, lib80), lib80, null, null);   // הסל — נקי לבדיקות שאחרי
            lib80.flush(5000);
            // מבנה 200 (ה10, המשתמש 16.11.2026): הנוסח האחד — של קובץ הפקודות, גם מהמסך
            String app200 = new File(Paths.data(), "חדש-80.vbat").getAbsolutePath(), wr200 = Commands.word(Commands.get(Syntax.WRITE));
            check("מבנה 80 / 200: בתוך תיקיית התוכנה — סירוב מהתכנון, בנוסח האחד", DiskRules.programFile(app200) != null
                    && DiskRules.programFile(app200).equals(Manager.onNewFile(new File(app200), "x", Paths.data(), v80, lib80).error()));
            {   // מבנה 200 (ה10): השורה הכתובה והמסך — אותו כלל (SmallCmds.writeRule), אותו נוסח, ובתכנון
                Batch.Plan pBad = Batch.plan(wr200 + " \"" + d80 + "\\א:ב?.vbat\" \"x\"\n", d80, v80, lib80, null);
                Batch.Plan pApp = Batch.plan(wr200 + " \"" + app200 + "\" \"x\"\n", d80, v80, lib80, null);
                Batch.Plan pTaken = Batch.plan(wr200 + " \"" + new File(d80, "קיים.vbat").getAbsolutePath() + "\" \"x\"\n", d80, v80, lib80, null);
                check("מבנה 200: שורה כתובה — שם לא תקין נתפס בתכנון (עד כאן: לא נבדק), והקובץ אינו רץ (" + pBad.steps.get(0).error + ")",
                        pBad.steps.get(0).error != null && pBad.steps.get(0).error.equals(DiskRules.badName("א:ב?.vbat")) && pBad.blocked());
                check("מבנה 200: קובץ של התוכנה, וקובץ קיים — אותו נוסח בשורה הכתובה ומהמסך",
                        pApp.steps.get(0).error != null && pApp.steps.get(0).error.equals(Manager.onNewFile(new File(app200), "x", Paths.data(), v80, lib80).error())
                                && pTaken.steps.get(0).error != null
                                && pTaken.steps.get(0).error.equals(Manager.onNewFile(new File(d80, "קיים.vbat"), "x", Paths.data(), v80, lib80).error()));
                String sc200 = read(new File(System.getProperty("vfm.src"), "src/com/tikiyot/vfm/pkudot/SmallCmds.java"));
                check("מבנה 200: הבדיקות שלפני \"כתוב\" — במקום אחד (writeRule), לשתי הדרכים",
                        sc200.split("writeRule\\(s, ", -1).length == 3 && sc200.split("DiskRules\\.programFile\\(f0", -1).length == 2);
            }
            check("מבנה 80: שם לא תקין — אותו סירוב של הכונן", DiskRules.badName("א:ב?.vbat") != null
                    && DiskRules.badName("א:ב?.vbat").equals(Manager.onNewFile(new File(d80, "א:ב?.vbat"), "x", Paths.data(), v80, lib80).error()));
            check("מבנה 80: במנוע — %תוכן% עם /הוסף, או בלי השם שמהמסך — שגיאה; \"כתוב\" כתובה על קובץ קיים — שם קיים, ועם /תפוס:להחליף — עוברת (מבנה 110)",
                    Batch.plan("כתוב \"%יעד%\\%שם-חדש%\" \"%תוכן%\" /הוסף\n", d80, v80, lib80, null, new Batch.Selection(new ArrayList<String>(),
                            new ArrayList<String>(), d80, "x.txt", "y")).errors == 1
                            && Batch.plan("כתוב \"x.txt\" \"%תוכן%\"\n", d80, v80, lib80, null, new Batch.Selection(new ArrayList<String>(),
                            new ArrayList<String>(), d80, "x.txt", "y")).errors == 1
                            && Batch.plan("כתוב \"קיים.vbat\" \"חדש\"\n", d80, v80, lib80).errors == 1
                            && Batch.plan("כתוב \"קיים.vbat\" \"חדש\" /תפוס:להחליף\n", d80, v80, lib80).errors == 0);
            Permit.close();
            try {
                String no80 = Physical.write(lib80, new File(d80, "עוקף.txt"), "x", false, false, null, null);
                check("מבנה 80: קובץ חדש בלי היתר — מסורב, ולא נכתב (" + no80 + ")", no80 != null && no80.contains(Words.PermitW.BYPASS)
                        && !new File(d80, "עוקף.txt").exists() && Permit.strict(Permit.WRITE));
                Batch.Plan w80 = Batch.plan("כתוב \"בפקודה.txt\" \"א\"\n", d80, v80, lib80);
                Batch.run(w80, lib80, null, null);
                check("מבנה 80: גם \"כתוב\" בקובץ פקודות, כשהמנוע רץ שלא דרך המנהל — לא נכתב, והשורה נכשלת", !new File(d80, "בפקודה.txt").exists() && w80.trouble);
                Batch.Plan w80b = Batch.plan("כתוב \"בפקודה.txt\" \"א\"\n", d80, v80, lib80);
                Manager.run(w80b, lib80, null, null, false, null);
                check("מבנה 80: ...ודרך המנהל — נכתב", "א".equals(read(new File(d80, "בפקודה.txt"))) && !w80b.trouble);
                Physical.Result r80 = act80.run(lib80, null, null, false);
                check("מבנה 80: דרך המנהל — הקובץ נוצר, והתוכן כלשונו: משתנים, מירכאות ו־\\n לא פוענחו (" + r80.note + ")", r80.failed == 0
                        && r80.done.size() == 1 && body80.equals(read(new80)));
                check("מבנה 221 (ה10): הקובץ החדש מהמסך — באינדקס מיד, כמו בשורה כתובה (אותה כתיבה: Physical.write)", lib80.find(new80.getAbsolutePath()) != null);
                Physical.Result again80 = act80.run(lib80, null, null, false);
                check("מבנה 80: הרצה שנייה (כבר קיים) — כישלון שמדווח, והקובץ לא הוחלף (" + again80.note + ")", again80.failed == 1
                        && again80.note.contains(Words.Cmd.fileTaken(new80.getName())) && body80.equals(read(new80)));
                Physical.Result deep80r = Manager.onNewFile(deep80, "ע", Paths.data(), v80, lib80).run(lib80, null, null, false);
                check("מבנה 80: תיקייה שחסרה מעל הקובץ — נוצרת, כמו שהמסך עשה", deep80r.failed == 0 && "ע".equals(read(deep80)));
                File deep111 = new File(d80, "תיקייה 111/ק.vbat");
                Manager.Act a111 = Manager.onNewFile(deep111, "x", Paths.data(), v80, lib80);
                check("מבנה 111: מהמסך, תיקייה חסרה — שתי פקודות: \"צור תיקייה … /תפוס:לדלג\" ואז \"כתוב\" (" + a111.text.trim() + ")",
                        a111.error() == null && a111.plan.steps.size() == 2 && a111.text.startsWith("צור תיקייה \"%יעד%\" /תפוס:לדלג\n"));
                check("מבנה 111: \"כתוב\" לבדה — לא יוצרת תיקייה: בשורה ומהבחירה — שגיאה; ובחומר — סירוב",
                        Batch.plan("כתוב \"אין-111\\א.txt\" \"x\"\n", d80, v80, lib80).errors == 1
                        && Batch.plan("כתוב \"%יעד%\\%שם-חדש%\" \"%תוכן%\"\n", d80, v80, lib80, null, new Batch.Selection(new ArrayList<String>(),
                                new ArrayList<String>(), new File(d80, "אין-111"), "א.txt", "y")).errors == 1
                        && Physical.write(lib80, new File(d80, "אין-111/א.txt"), "x", false, false, null, null) != null && !new File(d80, "אין-111").exists());
                // מבנה 122: אריזה ופריסה של zip — כפקודות ("ארוז", "פרוס"), והמסך דרך המנהל
                File z122 = new File(d80, "לארוז 122");
                new File(z122, "תת").mkdirs();
                write(z122, "א.txt", "אחת");
                write(new File(z122, "תת"), "ב.txt", "שתיים");
                lib80.dirChanged(d80.getAbsolutePath());
                lib80.flush(5000);
                Zip.Result by122 = Zip.pack(z122, new File(d80, "עוקף.zip"), null);
                check("מבנה 122: אריזה בלי היתר — מסורבת, ולא נוצר קובץ (" + by122.error + ")", by122.error != null && by122.error.contains(Words.PermitW.BYPASS)
                        && !new File(d80, "עוקף.zip").exists() && Permit.strict(Permit.ZIP));
                Manager.Act pk122 = Manager.onZip(true, z122.getAbsolutePath(), Paths.data(), v80, lib80);
                String sp122 = pk122.error() != null ? pk122.error() : pk122.say(lib80);
                File zip122 = new File(d80, "לארוז 122.zip");
                Manager.Act un122 = Manager.onZip(false, zip122.getAbsolutePath(), Paths.data(), v80, lib80);
                String su122 = un122.error() != null ? un122.error() : un122.say(lib80);
                check("מבנה 122: מהמסך, דרך המנהל — \"ארוז %נבחרים%\" (" + sp122 + ") ו\"פרוס %נבחרים%\" (" + su122 + "): הקובץ נוצר ליד התיקייה, ונפרס לתיקייה בשם פנוי, עם התוכן",
                        !pk122.failed && zip122.isFile() && zip122.equals(pk122.target()) && !un122.failed && new File(d80, "לארוז 122 (2)").equals(un122.target())
                        && "שתיים".equals(read(new File(d80, "לארוז 122 (2)/תת/ב.txt"))) && pk122.text.trim().equals("ארוז %נבחרים%"));
                Batch.Plan w122 = Batch.plan("ארוז \"לארוז 122\"\nודא \"לארוז 122 (2).zip\" קיים\nפרוס \"לארוז 122 (2).zip\"\nודא \"לארוז 122 (2) (2)\\לארוז 122\\א.txt\" קיים\n", d80, v80, lib80);
                String r122 = w122.errors == 0 ? String.valueOf(Manager.run(w122, lib80, null, null, false, null)) : w122.errorText();
                check("מבנה 122: בקובץ פקודות — שם תפוס: \"(2)\"; מה שנוצר נראה לשורות שאחריו " + r122,
                        w122.errors == 0 && !w122.trouble && new File(d80, "לארוז 122 (2).zip").isFile() && "אחת".equals(read(new File(d80, "לארוז 122 (2) (2)/לארוז 122/א.txt"))));
                check("מבנה 122: תיקייה שאין, קובץ שאינו zip, קובץ של התוכנה — שגיאה בתכנון",
                        Batch.plan("ארוז \"אין 122\"\n", d80, v80, lib80).errors == 1 && Batch.plan("פרוס \"לארוז 122\\א.txt\"\n", d80, v80, lib80).errors == 1
                                && Batch.plan("ארוז\n", d80, v80, lib80).errors == 1);
                String pui122 = read(new File(code, "masachim/rechivim/PhysicalUi.java"));
                check("מבנה 122: המסך אינו אורז ואינו פורס בעצמו — רק דרך המנהל", !pui122.contains("Zip.pack(") && !pui122.contains("Zip.unpack(")
                        && pui122.contains("Manager.onZip(true") && pui122.contains("Manager.onZip(false"));
                // מבנה 124: פעולות של המערכת — רק מה שהמנהל רשם בשמו מקבל היתר
                Manager.boot();
                boolean in124 = Permit.systemOpen(Permit.SYS_FOLLOW);
                boolean held124 = Permit.held();
                Permit.systemClose(in124);
                boolean no124 = Permit.systemOpen("פעולה שאיש לא רשם 124");
                boolean heldNo124 = Permit.held();
                Permit.systemClose(no124);
                check("מבנה 124: פעולה של המערכת שהמנהל רשם — מקבלת היתר לזמן שהיא רצה, והוא נסגר; שאינה רשומה — בלי היתר",
                        in124 && held124 && !no124 && !heldNo124 && !Permit.held() && Permit.isSystem(Permit.SYS_MENUS) && Permit.isSystem(Permit.SYS_DEV));
                int allow124 = 0, sys124 = 0;
                StringBuilder who124 = new StringBuilder();
                for (File f : javaFiles(code)) {
                    String t = read(f), n = f.getName();
                    if (t.contains("Permit.allowSystem(") && !n.equals("Manager.java") && !n.equals("Permit.java")) allow124++;
                    if (t.contains("Permit.systemOpen(") && !n.equals("Permit.java")) { sys124++; who124.append(n).append(' '); }
                }
                check("מבנה 124: רק המנהל רושם פעולות של המערכת (" + allow124 + " מקומות אחרים); ומי שמשתמש בהן — רק המקומות הידועים (" + who124.toString().trim() + ")",
                        allow124 == 0 && sys124 == 6 && who124.toString().contains("Menus.java") && who124.toString().contains("DbFollow.java")
                                && who124.toString().contains("DbListScreen.java") && who124.toString().contains("DbCmdMenu.java")
                                && who124.toString().contains("TopRow.java") && who124.toString().contains("DbScreen.java"));
                check("מבנה 80: ההיתר נסגר אחרי כל הרצה", !Permit.held());
            } finally {
                Permit.open();
            }
            // מבנה 132: הדלת לתוספים — קובץ של המשתמש שתוסף שומר נכתב דרך המנהל, כפקודה "כתוב"
            {
                File f132 = new File(d80, "מתוסף 132.vbat");
                String body132 = "REM %פנימי% \"מירכאות\"\nשורה שנייה";
                Permit.close();                                                 // כמו בתוכנה: אין היתר, אלא אם המנהל פותח
                String new132, again132, repl132, prog132;
                int trash132;
                try {
                    new132 = Manager.saveFile(f132, body132, false, lib80);
                    again132 = Manager.saveFile(f132, "אחר", false, lib80);
                    String first132 = read(f132);
                    lib80.flush(5000);
                    repl132 = Manager.saveFile(f132, "גרסה שנייה", true, lib80);
                    lib80.flush(5000);                                              // הסל נקרא מהאינדקס — מחכים שיתעדכן (כמו בבדיקה של מבנה 110)
                    List<Trash.Item> tr132 = SmallCmds.trashed(lib80, f132.getAbsolutePath(), false);
                    trash132 = tr132.size() == 1 && body132.equals(read(new File(tr132.get(0).path()))) ? 1 : -tr132.size();
                    prog132 = Manager.saveFile(Conf.file(), "x", true, lib80);
                    File sc132 = new File(Shortcuts.dir(), "קיצור 132.vbat");
                    check("מבנה 132: \"כתוב\" אינה כותבת בתוך תיקיית התוכנה (הקיצורים) — ולכן שם הגשר כותב כמו שהיה, ורק קובץ פקודות",
                            in132(Manager.saveFile(sc132, "ECHO", false, lib80)) && !sc132.exists());   // מבנה 197: התיקייה שעוד אין שם — מסורבת קודם
                    check("מבנה 132: תוסף שומר קובץ חדש — נכתב כלשונו (" + new132 + "); שוב בלי \"להחליף\" — מסורב, והקובץ לא נגע (" + again132 + ")",
                            new132 == null && body132.equals(first132) && again132 != null);
                } finally {
                    Permit.open();
                }
                check("מבנה 132: עם \"להחליף\" — התוכן החדש, והקודם עבר לסל עם התוכן שלו (" + repl132 + " · " + trash132 + ")", repl132 == null && "גרסה שנייה".equals(read(f132)) && trash132 == 1);
                lib80.flush(5000);
                Batch.run(Batch.plan("רוקן סל \"" + f132.getAbsolutePath() + "\"\n", d80, v80, lib80), lib80, null, null);   // הסל — נקי לבדיקות שאחרי
                lib80.flush(5000);
                check("מבנה 132: קובץ של התוכנה — מסורב גם לתוסף (" + prog132 + ")", prog132 != null && !"x".equals(read(Conf.file())));
                String br132 = read(new File(code, "lev/Bridge.java")), ls132 = read(new File(code, "masachim/LabScreen.java"));
                int doors132 = 0;
                for (File f : javaFiles(code)) if (read(f).contains("addJavascriptInterface(")) doors132++;
                check("מבנה 132: דלת אחת — תוסף מתחבר לתוכנה רק במקום אחד, ורק אל הגשר; והגשר שומר קובץ של המשתמש רק דרך המנהל",
                        doors132 == 1 && ls132.contains("addJavascriptInterface(new Bridge(") && br132.contains("Manager.saveFile(f, text, was, Library.get(screen))")
                                && br132.contains("Manager.saveFile(f, text, was, lib)") && br132.contains("Manager.ownSaveKeepOld(Paths.data(), f, text, Library.get(screen))")
                                && br132.contains("if (!Batch.isBatch(f.getName())) return Words.Lab.ONLY_COMMANDS;"));
            }
            {   // מבנה 138 (המשתמש: "הכל יוכל, אבל דרך שכבת הניהול"): תוסף קורא קובץ ושואל מה בתיקייה — דרך המנהל
                File d138 = new File(home, "תוסף138");
                wipe(d138);
                File f138 = mk(d138, "לקריאה.txt", 12);
                File[] in138 = Manager.pluginList(d138);
                check("מבנה 138: קריאה דרך המנהל — הקובץ כלשונו; גם קובץ של התוכנה נקרא; אין קובץ — null",
                        read(f138).equals(Manager.pluginRead(f138)) && Manager.pluginRead(new File(d138, "אין.txt")) == null && Manager.pluginRead(null) == null
                                && (!Conf.file().isFile() || Manager.pluginRead(Conf.file()) != null));
                check("מבנה 138: מה שבתיקייה — דרך המנהל; אין תיקייה — null", in138 != null && in138.length == 1 && in138[0].getName().equals("לקריאה.txt")
                        && Manager.pluginList(new File(d138, "אין")) == null && Manager.pluginList(null) == null);
                String br138 = read(new File(code, "lev/Bridge.java"));
                check("מבנה 138: השומר — הגשר אינו קורא קובץ בעצמו, ואינו סורק תיקייה מלבד התיקייה של התוסף (הניקוי)",
                        !br138.contains("Files.readUtf8(") && br138.contains("Manager.pluginRead(resolve(path))") && br138.contains("Manager.pluginList(resolve(path))")
                                && !br138.contains("listFiles("));
                // מבנה 139: האזור שהתוכנה מנהלת — כתיבה ומחיקה דרך המנהל, בהיתר; מחוץ לאזור — לא
                File z139 = new File(d138, "אזור"), f139 = new File(z139, "תת/קובץ.txt"), out139 = new File(d138, "בחוץ.txt");
                String ok139 = Manager.ownSave(z139, f139, "תוכן"), no139 = Manager.ownSave(z139, out139, "x");
                String by139;
                boolean del139;
                Permit.close();                                                 // הבדיקות כאן רצות בתוך היתר פתוח; "בלי המנהל" — בלעדיו
                try {
                    by139 = Physical.ownWrite(new File(z139, "עוקף.txt"), "x");
                    del139 = Physical.ownDelete(f139);
                } finally {
                    Permit.open();
                }
                check("מבנה 139: קובץ באזור של התוכנה — נכתב דרך המנהל (" + ok139 + "); מחוץ לאזור — מסורב (" + no139 + "); בלי המנהל — מסורב, ולא נכתב",
                        ok139 == null && "תוכן".equals(read(f139)) && Words.PermitW.NOT_IN_ZONE.equals(no139) && !out139.exists()
                                && by139 != null && by139.contains(Words.PermitW.BYPASS) && !new File(z139, "עוקף.txt").exists() && !del139 && f139.isFile());
                new File(z139, "תת/תיקייה").mkdirs();
                java.util.List<File> gone139 = Manager.ownClean(z139, new File(z139, "תת"));
                check("מבנה 139: ניקוי — רק הקבצים שבתיקייה, דרך המנהל; תיקייה מחוץ לאזור — דבר", gone139.size() == 1 && !f139.exists()
                        && new File(z139, "תת/תיקייה").isDirectory() && Manager.ownClean(z139, d138).isEmpty() && f138.isFile());
                check("מבנה 139: השומר — הגשר אינו כותב ואינו מוחק קובץ בעצמו; ו\"כתיבה באזור של התוכנה\" מסורבת בלי היתר",
                        !br138.contains("Files.writeAtomic(") && !br138.contains(".delete()") && !br138.contains(".mkdirs()") && Permit.strict(Permit.OWN));
                // מבנה 140: תיקיית התוספים — מה שהתוכנה עושה שם לבד רשום אצל המנהל, ונעשה בחומר בהיתר
                File p140 = new File(d138, "תוספים140");
                File b140 = Lab.addBundled(p140, "<meta name=\"vfm-plugin\" content=\"בדיקה140\"><meta name=\"vfm-plugin-version\" content=\"1\">", "בדיקה140.html");
                boolean mv140, dir140;
                Permit.close();
                try {
                    mv140 = Physical.ownMove(f138, new File(d138, "זז.txt"));
                    dir140 = Physical.ownDir(new File(d138, "חדשה140"));
                } finally {
                    Permit.open();
                }
                String lab140 = read(new File(code, "lev/Lab.java"));
                check("מבנה 140: תוסף שבא עם התוכנה נכנס לתיקיית התוספים (" + b140 + "), ופעם שנייה — כבר יש; העברה מחוץ לאזור — לא",
                        b140 != null && b140.isFile() && Lab.addBundled(p140, read(b140), "בדיקה140.html") == null && !Manager.ownMove(p140, f138, new File(p140, "x.txt")) && f138.isFile()
                                && Manager.ownFolder(p140, new File(p140, "תת140")) && Manager.ownDrop(p140, new File(p140, "תת140")) && !new File(p140, "תת140").exists());
                check("מבנה 140: העברה ותיקייה באזור של התוכנה — בלי היתר מסורבות; והשומר — Lab אינו כותב, מוחק או מעביר קובץ בעצמו",
                        !mv140 && f138.isFile() && !dir140 && !new File(d138, "חדשה140").exists()
                                && !lab140.contains("Permit.") && !lab140.contains("Physical.own") && !lab140.contains("Files.writeAtomic(") && !lab140.contains(".delete()") && !lab140.contains(".renameTo(") && !lab140.contains(".mkdirs()"));
                // מבנה 141: מספר הגרסה של הדלת — רשימת הפונקציות קבועה; פונקציה שיוצאת מחייבת להעלות את המספר (ולעדכן כאן)
                String br141 = read(new File(code, "lev/Bridge.java"));
                StringBuilder door141 = new StringBuilder();
                java.util.regex.Matcher m141 = java.util.regex.Pattern.compile("@JavascriptInterface\\s+public \\S+ (\\w+)\\(").matcher(br141);
                while (m141.find()) door141.append(m141.group(1)).append(' ');
                check("מבנה 141: הדלת, גרסה 1 — 39 הפונקציות (35, ועוד commandFolders — מבנה 151, deleteCommandFile — מבנה 230, design ו־shape — מבנה 243; תוספת אינה מעלה את המספר), לפי הקבוצות (מידע · קריאה · כתיבה · הרצה · חלונות); התוכנה מוסרת את המספר (info.door) [" + door141.toString().trim() + "]",
                        Bridge.DOOR == 1 && br141.contains("o.put(\"door\", DOOR);")
                                && door141.toString().trim().equals("info theme design places modes dataDir startFile dbStats pathInfo commandMatrix smartLists commandFiles commandFolders myCommands templates readText listDir toVar expand saveCommandFile writeCommandFile deleteCommandFile saveTemplate saveText cleanData setReaders load save runCommands runFile shape pickFolder pickFile pickList smartForm send toast log close"));
                // מבנה 145: המעבר מהמבנה הישן נבדק פעם אחת בהפעלה, ולא בכל קריאה
                Lab.recheck();
                File r145 = Lab.root(), old145 = new File(Paths.dbData(), Lab.OLD_DIR);
                mk(old145, "נשאר145.html", 5);
                Lab.root();
                boolean once145 = new File(old145, "נשאר145.html").isFile();
                Lab.recheck();
                Lab.root();
                check("מבנה 145: תיקיית התוספים — המעבר מהישן נבדק פעם אחת בהפעלה (קריאה חוזרת אינה סורקת); בהפעלה הבאה — נעשה",
                        once145 && !old145.exists() && new File(r145, "נשאר145.html").isFile());
                new File(r145, "נשאר145.html").delete();
                // מבנה 146: ערך שנגזר מקובץ — פעם אחת, עד שהקובץ משתנה (Once); שלושת המטמונים של התפריטים עליו
                File f146 = mk(d138, "פעם146.vbat", 7);
                Once<String> o146 = new Once<String>(2);
                boolean none146 = o146.had(f146, "") == null;
                o146.put(f146, "", "א");
                o146.put(f146, "ב", "אחר");
                boolean kept146 = "א".equals(o146.had(f146, "")) && "אחר".equals(o146.had(f146, "ב"));
                mk(d138, "פעם146.vbat", 9);                                     // הקובץ השתנה (הגודל)
                boolean changed146 = o146.had(f146, "") == null;
                o146.put(f146, "", "1");
                o146.put(f146, "ב", "2");
                o146.put(f146, "ג", "3");                                       // התמלא (2) — מתרוקן, והחדש נכנס
                String m146 = read(new File(code, "lev/Menus.java"));
                check("מבנה 146: Once — אין לפני; נזכר (גם כמה ערכים לקובץ); קובץ שהשתנה — נשכח; מלא — מתרוקן; והתפריטים אינם מחזיקים מטמון משלהם",
                        none146 && kept146 && changed146 && o146.had(f146, "") == null && "3".equals(o146.had(f146, "ג"))
                                && !m146.contains("new HashMap<String, String[]>()") && m146.split("new Once<", -1).length - 1 == 1
                                && read(new File(code, "pkudot/Choice.java")).split("new Once<", -1).length - 1 == 2 && !m146.contains("static String expand(")
                                && "".equals(Choice.textOf(mk(d138, "ריק146.vbat", 0))) && Menus.look(f146).length == 2);
                // מבנה 151: התיקיות שיש בהן קובצי פקודות — מהאינדקס, דרך המנהל
                File r151 = new File(home, "כונן151");
                wipe(r151);
                mk(new File(r151, "א"), "אחד.vbat", 3);
                mk(new File(r151, "א"), "שניים.vbat", 3);
                mk(new File(r151, "א"), "לא פקודה.txt", 3);
                mk(new File(r151, "ב/עמוק"), "שלוש.vbat", 3);
                mk(new File(r151, "ג"), "רק טקסט.txt", 3);
                Index ix151 = new Index("151", "151", r151.getAbsolutePath());
                ix151.scan(null);
                Library lib151 = Library.forTests(new ArrayList<Index>(java.util.Collections.singletonList(ix151)));
                Map<String, Integer> cf151 = Manager.commandFolders(lib151);
                check("מבנה 151: רק תיקיות שיש בהן קובצי פקודות, וכמה בכל אחת (" + cf151 + "); בלי אינדקס — null",
                        cf151 != null && cf151.size() == 2 && Integer.valueOf(2).equals(cf151.get(new File(r151, "א").getAbsolutePath()))
                                && Integer.valueOf(1).equals(cf151.get(new File(r151, "ב/עמוק").getAbsolutePath())) && Manager.commandFolders(null) == null);
                // מבנה 154: קובץ פקודות של המשתמש בתוך תיקיית התוכנה — כשהוא מוחלף, הקודם עובר לסל
                File z154 = new File(r151, "אזור154"), f154 = mk(z154, "פקודה.vbat", 4);
                lib151.dirChanged(z154.getAbsolutePath());
                lib151.flush(5000);
                String first154 = read(f154);
                String ok154 = Manager.ownSaveKeepOld(z154, f154, "גרסה חדשה", lib151);
                lib151.flush(5000);
                File[] tr154 = new File(r151, Index.TRASH).listFiles();
                boolean in154 = false;
                if (tr154 != null) for (File t : tr154) if (t.getName().endsWith("פקודה.vbat") && first154.equals(read(t))) in154 = true;
                String new154 = Manager.ownSaveKeepOld(z154, new File(z154, "חדש.vbat"), "x", lib151);
                check("מבנה 154: שמירה על קובץ קיים באזור — התוכן החדש נכתב (" + ok154 + "), והקודם בסל עם התוכן שלו; קובץ חדש — נכתב, בלי סל; מחוץ לאזור — לא",
                        ok154 == null && "גרסה חדשה".equals(read(f154)) && in154 && new154 == null && "x".equals(read(new File(z154, "חדש.vbat")))
                                && Words.PermitW.NOT_IN_ZONE.equals(Manager.ownSaveKeepOld(z154, new File(r151, "בחוץ.vbat"), "x", lib151)));
                // מבנה 229 (באג שנמצא בדרך למחיקה מהעורך): אחרי שמירה על קובץ קיים באזור — הקובץ החדש באינדקס, בגודל שלו (עד כאן הקודם
                // יצא מהאינדקס עם המעבר לסל, והחדש לא נכנס: "מחק" עליו נכשל — "לא באינדקס")
                lib151.flush(5000);
                Library.Hit h229 = lib151.find(f154.getAbsolutePath()), n229 = lib151.find(new File(z154, "חדש.vbat").getAbsolutePath());
                check("מבנה 229: קובץ פקודות שנשמר מחדש בתוך תיקיית התוכנה — נשאר באינדקס, בגודל החדש; קובץ חדש שם — נכנס",
                        h229 != null && h229.rec != null && !h229.rec.gone && h229.rec.size == f154.length() && n229 != null && n229.rec != null && !n229.rec.gone);
                // מבנה 164: ההרצה יצאה ל־Runner — הכניסה נשארה Batch.run (רק המנהל קורא לה); ל־Runner.run0 — רק Batch
                StringBuilder run164 = new StringBuilder();
                for (File f : javaFiles(code)) if (!f.getName().equals("Batch.java") && read(f).contains("Runner.run0(")) run164.append(f.getName()).append(' ');
                check("מבנה 164: ההרצה עצמה (Runner.run0) — רק מהכניסה Batch.run (" + run164.toString().trim() + ")",
                        run164.length() == 0 && read(new File(code, "pkudot/Batch.java")).contains("Runner.run0("));
                // מבנה 183: העתקת קובץ יצאה מ־Physical אל FileCopy. היא כותבת לכונן ואינה שואלת היתר בעצמה — ולכן מותר לקרוא לה
                // רק מתוך תיקיית הכונן, מאחורי כניסה של Physical ששאלה (הוראות, סעיף 2).
                StringBuilder copy183 = new StringBuilder();
                for (File f : javaFiles(code))
                    if (!f.getParentFile().getName().equals("konan") && (read(f).contains("FileCopy.copyFile(") || read(f).contains("FileCopy.moveFile(")))
                        copy183.append(f.getName()).append(' ');
                check("מבנה 183: העתקה / העברה של קובץ (FileCopy) — רק מתוך תיקיית הכונן (" + copy183.toString().trim() + ")",
                        copy183.length() == 0 && readAll(new File(code, "chomer/konan")).contains("FileCopy.copyFile("));
                // מבנה 184: מנוע ההעתקה וההעברה יצא מ־Physical אל Mover. הכניסות (transfer, transferKeep, transferTo) נשארו ב־Physical,
                // ושם נשאל ההיתר; למנוע עצמו — רק מתוך תיקיית הכונן.
                StringBuilder mover184 = new StringBuilder();
                java.util.regex.Pattern m184 = java.util.regex.Pattern.compile("\\bMover\\.|\\bPhysical\\.(toTrash|stamp)\\(");   // גם מה שנפתח בשביל המנוע
                for (File f : javaFiles(code))
                    if (!f.getParentFile().getName().equals("konan") && m184.matcher(read(f)).find()) mover184.append(f.getName()).append(' ');
                String ph184 = read(new File(code, "chomer/konan/Physical.java"));
                check("מבנה 184: מנוע ההעברה (Mover) — רק מתוך תיקיית הכונן (" + mover184.toString().trim() + "); הכניסות ב־Physical, והן שואלות את ההיתר",
                        mover184.length() == 0 && ph184.contains("Mover.go(") && ph184.contains("Permit.check(Permit.TRANSFER)")
                                && !read(new File(code, "chomer/konan/Mover.java")).contains("Permit."));
                // מבנה 186: החיפוש יצא מ־Library אל Search — בלי מצב משלו: עובד על הצילום שהוא מקבל. הכניסות (search, list) — ב־Library.
                StringBuilder search186 = new StringBuilder();
                for (File f : javaFiles(code)) if (!f.getName().equals("Library.java") && read(f).contains("Search.run(")) search186.append(f.getName()).append(' ');
                String se186 = read(new File(code, "chomer/indeks/Search.java"));
                check("מבנה 186: החיפוש (Search.run) — רק מהכניסות שב־Library (" + search186.toString().trim() + "); בלי מצב משלו, ובלי לגעת ברשימת האינדקסים",
                        search186.length() == 0 && read(new File(code, "chomer/indeks/Library.java")).contains("Search.run(indexes, ")
                                && !se186.contains("synchronized") && !se186.contains(".indexes") && !se186.contains("volatile"));
                // מבנה 192: שומר על גודל. המשתמש (16.11.2026): "אם הוא עובר את האלף שורות — צריך להביא אותו לדיון ולקבל החלטה מפורשת."
                // ההחלטה — לפצל, או לאשר: קובץ שאושר נרשם כאן, עם הסיבה ועם תקרה (0 — בלי תקרה). להוסיף שורה — רק באישור המשתמש.
                {
                    String[][] big192 = {
                            {"basis/Words.java", "0", "המילון — כל טקסט שהמשתמש רואה במקום אחד (הוראות, סעיף 8); גדל עם כל טקסט"},
                            {"chomer/indeks/Library.java", "1300", "ההפעלה, הבנייה, הרענון והתור — נושא אחד תחת מנעול אחד (צעד 188)"},
                            {"chomer/indeks/Index.java", "1100", "הטבלאות והסריקה של אינדקס אחד (צעד 188)"},
                    };
                    StringBuilder over192 = new StringBuilder();
                    String root192 = code.getPath().replace('\\', '/');
                    for (File f : javaFiles(code)) {
                        int n = read(f).split("\n", -1).length;
                        if (n <= 1000) continue;
                        String rel = f.getPath().replace('\\', '/').substring(root192.length() + 1);
                        int cap = -1;
                        for (String[] b : big192) if (b[0].equals(rel)) cap = Integer.parseInt(b[1]);
                        if (cap == 0 || (cap > 0 && n <= cap)) continue;
                        over192.append(rel).append(" (").append(n).append(cap > 0 ? " > " + cap : "").append(") ");
                    }
                    check("מבנה 192: קובץ קוד שעבר 1,000 שורות — להביא למשתמש לדיון ולהחלטה מפורשת: לפצל, או לאשר ולרשום (" + over192.toString().trim() + ")",
                            over192.length() == 0);
                }
                wipe(r151);
                wipe(d138);
            }
            String pui80 = read(new File(code, "masachim/rechivim/PhysicalUi.java"));
            check("מבנה 80: המסך של הפעולות בכונן אינו כותב עוד קובץ בעצמו; רק דרך המנהל", !pui80.contains("writeAtomic")
                    && pui80.contains("Manager.onNewFile(f, content"));
            wipe(d80);
        }
        // מבנה 81: מסך הסל — שחזור ומחיקה לצמיתות של הפריטים שנבחרו, דרך המנהל
        {
            File d81 = new File(r, "ניסוי-81");
            wipe(d81);
            new File(d81, "תיקייה").mkdirs();
            for (String n : new String[]{"א.txt", "ב.txt", "ג.txt", "ד.txt"}) write(d81, n, n);
            write(new File(d81, "תיקייה"), "פנים.txt", "5");
            Index ix81 = new Index("81", "81", d81.getAbsolutePath());
            ix81.scan(null);
            Library lib81 = Library.forTests(new ArrayList<Index>(java.util.Collections.singletonList(ix81)));
            java.util.Map<String, String> v81 = Parse.vars(d81, d81.getAbsolutePath(), null);
            Physical.delete(lib81, java.util.Arrays.asList(new File(d81, "א.txt").getAbsolutePath(), new File(d81, "ב.txt").getAbsolutePath(),
                    new File(d81, "ג.txt").getAbsolutePath()), java.util.Collections.singletonList(new File(d81, "תיקייה").getAbsolutePath()), false);
            lib81.flush(5000);
            java.util.Map<String, String> in81 = new java.util.HashMap<String, String>();     // שם מקורי ← המסלול בסל
            for (Trash.Item it : Trash.list(lib81)) if (it.original != null) in81.put(new File(it.original).getName(), it.path());
            check("מבנה 81: ההכנה — ארבעה פריטים בסל (שלושה קבצים ותיקייה)", in81.size() == 4 && Trash.list(lib81).size() == 4);
            List<String> back81 = java.util.Arrays.asList(in81.get("א.txt"), in81.get("תיקייה")), kill81 = java.util.Collections.singletonList(in81.get("ב.txt"));
            check("מבנה 81: בחירה ריקה — שגיאה; %נבחרים% שלא לבדו — שגיאה", Manager.onTrash(true, new ArrayList<String>(), d81, v81, lib81).error() != null
                    && Batch.plan("רוקן סל \"%נבחרים%\\x\"\n", d81, v81, lib81, null, new Batch.Selection(kill81, new ArrayList<String>())).errors == 1);
            Permit.close();
            try {
                List<Trash.Item> one81 = new ArrayList<Trash.Item>();
                for (Trash.Item it : Trash.list(lib81)) if (it.path().equals(in81.get("ג.txt"))) one81.add(it);
                Physical.Result no81 = Physical.restore(lib81, one81), no81b = Physical.deleteForever(lib81, one81);
                check("מבנה 81: שחזור ומחיקה לצמיתות בלי היתר — מסורבים, והפריט בסל (" + no81.note + ")", no81.failed > 0 && no81b.failed > 0
                        && no81.note.contains(Words.PermitW.BYPASS) && no81b.note.contains(Words.PermitW.BYPASS) && Trash.list(lib81).size() == 4
                        && !new File(d81, "ג.txt").exists() && Permit.strict(Permit.RESTORE) && Permit.strict(Permit.FOREVER));
                Manager.Act r81 = Manager.onTrash(true, back81, d81, v81, lib81);
                check("מבנה 81: הפקודה שהמסך מריץ לשחזור — \"שחזר מהסל %נבחרים%\" (" + r81.error() + ")", r81.error() == null
                        && r81.text.trim().equals("שחזר מהסל %נבחרים%"));
                Physical.Result rr81 = r81.run(lib81, null, null, false);
                lib81.flush(5000);
                check("מבנה 81: דרך המנהל — הקובץ והתיקייה (עם מה שבתוכה) חזרו למקומם; השאר בסל (" + rr81.note + ")", rr81.failed == 0 && rr81.done.size() == 2
                        && "א.txt".equals(read(new File(d81, "א.txt"))) && "5".equals(read(new File(d81, "תיקייה/פנים.txt"))) && Trash.list(lib81).size() == 2);
                Physical.Result again81 = r81.run(lib81, null, null, false);
                check("מבנה 81: הרצה שנייה (הפריטים כבר אינם בסל) — כישלון שמדווח לכל פריט (" + again81.note + ")", again81.failed == 2
                        && again81.done.isEmpty() && Words.DbCmd.GONE_FROM_TRASH.equals(again81.note));
                Manager.Act k81 = Manager.onTrash(false, kill81, d81, v81, lib81);
                check("מבנה 81: הפקודה שהמסך מריץ למחיקה לצמיתות — \"רוקן סל %נבחרים%\"", k81.error() == null && k81.text.trim().equals("רוקן סל %נבחרים%"));
                Physical.Result kr81 = k81.run(lib81, null, null, false);
                lib81.flush(5000);
                check("מבנה 81: ...נמחק רק מה שנבחר — \"ג\" עדיין בסל", kr81.failed == 0 && kr81.done.size() == 1 && Trash.list(lib81).size() == 1
                        && Trash.list(lib81).get(0).path().equals(in81.get("ג.txt")));
                Physical.Result out81 = Manager.onTrash(false, java.util.Collections.singletonList(new File(d81, "ד.txt").getAbsolutePath()), d81, v81, lib81)
                        .run(lib81, null, null, false);
                check("מבנה 81: מסלול שאינו פריט בסל — מחיקה לצמיתות אינה נוגעת בו", out81.failed == 1 && out81.done.isEmpty() && new File(d81, "ד.txt").isFile());
                check("מבנה 81: ההיתר נסגר אחרי כל הרצה", !Permit.held());
            } finally {
                Permit.open();
            }
            String pui81 = read(new File(code, "masachim/rechivim/PhysicalUi.java"));
            int direct81 = 0;
            List<File> all81 = new ArrayList<File>();
            collect76(new File(code, "masachim"), all81);
            for (File f : all81) {
                String t = read(f);
                if (t.contains("Physical.restore(") || t.contains("Physical.deleteForever(") || t.contains("Trash.empty(") || t.contains("Trash.restore(")) direct81++;
            }
            check("מבנה 81: במסכים — אין עוד שחזור או מחיקה לצמיתות ישירים (" + direct81 + "); רק דרך המנהל", direct81 == 0 && pui81.contains("Manager.onTrash("));
            check("מבנה 81: המסך של הפעולות בכונן אינו קורא עוד לאף פעולה שמשנה בכונן — רק שאלות (שמות, מקום פנוי, ספירה)",
                    !java.util.regex.Pattern.compile("Physical\\.(delete|deleteOwn|transfer|transferKeep|transferTo|rename|makeFolder|newFile|write|restore|deleteForever|oldToTrash)\\(").matcher(pui81).find());
            Physical.deleteForever(lib81, Trash.list(lib81));
            wipe(d81);
        }
        // מבנה 83 (מהיומן של המכשיר): רענון אחרי רענון, כל אחד על עותק של האינדקס ועם תיקייה חדשה — מערך המסלולים אינו מתנפח
        {
            File d83 = new File(r, "ניסוי-83");
            wipe(d83);
            new File(d83, "בסיס").mkdirs();
            write(new File(d83, "בסיס"), "א.txt", "1");
            Index ix83 = new Index("83", "83", d83.getAbsolutePath());
            ix83.scan(null);
            ix83.dirPath(0);
            boolean found83 = true;
            for (int k = 0; k < 30; k++) {                                        // כמו Library.refresh: התאריכים, עותק, עדכון העותק
                new File(d83, "בסיס/חדשה " + k).mkdirs();
                new File(d83, "בסיס").setLastModified(1700000000000L + k * 60000L);   // השעון של הכונן גס — מוודאים שהתאריך זז
                long[] t83 = ix83.times(1);
                Index c83 = ix83.copy();
                c83.apply(t83);
                ix83 = c83;
            }
            int dirs83 = 0;
            for (Index.Dir d : ix83.dirs) if (!d.gone) dirs83++;
            for (int k = 0; k < 30; k++) if (ix83.dirId(new File(d83, "בסיס/חדשה " + k).getAbsolutePath()) < 0) found83 = false;
            check("מבנה 83: אחרי 30 רענונים שכל אחד מצא תיקייה חדשה — כל התיקיות באינדקס (" + dirs83 + ")", found83 && dirs83 >= 31);
            check("מבנה 83: ...ומערך המסלולים בגודל סביר: " + ix83.pathsCapacity() + " תאים ל־" + dirs83 + " תיקיות (לפני התיקון — פי 1.5 בכל רענון)",
                    ix83.pathsCapacity() <= dirs83 * 2 + 64);
            check("מבנה 83: ...והמסלול של התיקייה האחרונה נכון", new File(d83, "בסיס/חדשה 29").getAbsolutePath()
                    .equals(ix83.dirPath(ix83.dirId(new File(d83, "בסיס/חדשה 29").getAbsolutePath()))));
            wipe(d83);
        }
        // מבנה 84: גם הכתיבה למסד שואלת את ההיתר. מבנה 126: המעבר הסתיים — בלי היתר היא מסורבת
        {
            String db84 = read(new File(code, "chomer/masad/Db.java"));
            check("מבנה 84: שער הכתיבה למסד (Db.write) ומסד שמוחזק פתוח (openHeld) שואלים את ההיתר",
                    db84.contains("Permit.check(Permit.DB, PERMIT_WRAPPERS)") && db84.contains("Permit.check(Permit.DB);"));
            Permit.close();
            try {
                String no84 = Permit.check(Permit.DB);
                check("מבנה 126: כתיבה למסד בלי היתר — מסורבת, בנוסח של המשתמש (המעבר של המסד הסתיים)", no84 != null && Permit.strict(Permit.DB)
                        && no84.contains(Words.PermitW.BYPASS) && no84.contains(Words.PermitW.REFUSED) && no84.contains(Permit.DB));
            } finally {
                Permit.open();
            }
            // מבנה 126: מי שפונה לשער הכתיבה של המסד מהלב ומהמסכים — רק המקומות הידועים (פעולות של המערכת, רשומות אצל המנהל); הרשימה רק מתקצרת
            StringBuilder who126 = new StringBuilder(), wrap126 = new StringBuilder();
            for (File f : javaFiles(code)) {
                String t = read(f), n = f.getName(), path126 = f.getPath().replace('\\', '/');
                boolean upper = path126.contains("/lev/") || path126.contains("/masachim/");
                if (upper && (t.contains("Db.write(") || t.contains("Db.openHeld("))) who126.append(n).append(' ');
                if (!n.equals("DbVerbsUi.java") && t.contains("DbVerbsUi.write(")) wrap126.append(n).append(' ');
            }
            check("מבנה 126: בלב ובמסכים, לשער הכתיבה של המסד פונים רק המקומות הידועים (" + who126.toString().trim() + ")",
                    who126.toString().trim().split(" ").length == 5 && who126.toString().contains("Menus.java") && who126.toString().contains("DbListScreen.java")
                            && who126.toString().contains("DbVerbsUi.java") && who126.toString().contains("DbCmdMenu.java") && who126.toString().contains("DbScreen.java"));
            check("מבנה 126: ...ולעטיפה של המסכים (DbVerbsUi.write) — רק השורה העליונה (" + wrap126.toString().trim() + ")", wrap126.toString().trim().equals("TopRow.java"));
            String run126 = read(new File(code, "masachim/RunScreen.java"));
            check("מבנה 126: מסך ההרצה אינו מנקה רשימות בעצמו; רק דרך המנהל", run126.contains("Manager.onRunClean(") && !run126.contains("DbListCmd.")
                    && !readAll(new File(code, "pkudot")).contains("removeLists("));   // מבנה 168: בכל תיקיית הפקודות
            // מבנה 127: "הגדר" — הגדרה של התוכנה בפקודה; המסכים קובעים דרך המנהל; בלי היתר — מסורב
            {
                Library lib127 = Library.forTests(new ArrayList<Index>());
                String was127 = Conf.get(Conf.GENERAL, Conf.DELETED, ""), ex127 = Conf.get(Conf.GENERAL, Conf.EXCLUDED, "");
                Batch.Plan w127 = Batch.plan("הגדר \"קובץ שנמחק\" \"השאר באפור\"\nהגדר \"רענון תקופתי בדקות\" \"05\"\n"
                        + "הגדר \"לא לאנדקס\" \"%פנימי%\\מטמון\\..\\מטמון | %פנימי%/ב\"\n", r, v);
                String rep127 = String.valueOf(Manager.run(w127, lib127, null, null, false, null));
                check("מבנה 127: בקובץ פקודות — שלוש הגדרות נקבעות (" + w127.errorText() + "): בחירה, מספר (\"05\" ← 5), ותיקיות (מסלולים נקיים, מופרדים ב|)",
                        w127.errors == 0 && !w127.trouble && Conf.DELETED_GREY.equals(Conf.get(Conf.GENERAL, Conf.DELETED, ""))
                                && "5".equals(Conf.get(Conf.GENERAL, Conf.PERIODIC, "")) && lib127.periodicMinutes() == 5
                                && (r.getAbsolutePath() + "/מטמון|" + r.getAbsolutePath() + "/ב").equals(Conf.get(Conf.GENERAL, Conf.EXCLUDED, ""))
                                && rep127.contains(Words.Cmd.setDone(Conf.DELETED, Conf.DELETED_GREY)));
                check("מבנה 127: הגדרה שאין, ערך שאינו מהבחירה, מספר שאינו מספר, תיקייה במסלול יחסי, ושורה חסרה — שגיאה בתכנון, והקובץ אינו רץ",
                        Batch.plan("הגדר \"אין הגדרה כזו\" \"1\"\n", r, v).errors == 1 && Batch.plan("הגדר \"קובץ שנמחק\" \"אולי\"\n", r, v).errors == 1
                                && Batch.plan("הגדר \"רענון תקופתי בדקות\" \"חמש\"\n", r, v).errors == 1 && Batch.plan("הגדר \"לא לאנדקס\" \"מטמון\"\n", r, v).errors == 1
                                && Batch.plan("הגדר \"קובץ שנמחק\"\n", r, v).errors == 1 && Batch.plan("הגדר \"%שם-חדש%\" \"%תוכן%\"\n", r, v).errors == 1
                                && Batch.plan("הגדר \"אין הגדרה כזו\" \"1\"\n", r, v).errorText().contains(Conf.SEND_TO));
                Manager.Act a127 = Manager.onSetting(Conf.EXCLUDED, r.getAbsolutePath() + "/100% %פנימי% \"מ\"", r, v, lib127);
                Permit.close();                                                 // כמו בתוכנה: אין היתר, אלא אם המנהל פותח
                String said127, direct127;
                try {
                    said127 = a127.error() != null ? a127.error() : a127.say(lib127);
                    direct127 = Conf.apply(Conf.setting(Conf.DELETED), Conf.DELETED_REMOVE);
                    check("מבנה 127: ההיתר נסגר אחרי ההרצה", !Permit.held());
                } finally {
                    Permit.open();
                }
                check("מבנה 127: מהמסך, דרך המנהל — \"הגדר \"%שם-חדש%\" \"%תוכן%\"\"; הערך כלשונו, גם עם % ומירכאות (" + said127 + ")",
                        a127.text.trim().equals("הגדר \"%שם-חדש%\" \"%תוכן%\"") && !a127.failed
                                && (r.getAbsolutePath() + "/100% %פנימי% \"מ\"").equals(Conf.get(Conf.GENERAL, Conf.EXCLUDED, ""))
                                && Manager.onSetting("אין הגדרה כזו", "1", r, v, lib127).error() != null
                                && Manager.onSetting(Conf.LEVEL, "לפעמים", r, v, lib127).error() != null);
                check("מבנה 127: קביעת הגדרה בלי היתר — מסורבת בנוסח של המשתמש, וההגדרה לא השתנתה (" + direct127 + ")", direct127 != null
                        && direct127.contains(Words.PermitW.BYPASS) && Permit.strict(Permit.SETTING) && Conf.DELETED_GREY.equals(Conf.get(Conf.GENERAL, Conf.DELETED, "")));
                Manager.Act same127 = Manager.onSetting(Conf.DELETED, Conf.DELETED_GREY, r, v, lib127);
                check("מבנה 127: אותו ערך שוב — \"כבר כך\", בלי כישלון", same127.say(lib127).contains(Words.Cmd.SET_SAME) && !same127.failed);
                String scr127 = read(new File(code, "masachim/SettingsScreen.java")), tr127 = read(new File(code, "masachim/rechivim/Transfer.java")),
                        bar127 = read(new File(code, "masachim/rechivim/DbPasteBar.java")), lib127src = readAll(new File(code, "chomer/indeks"));   // מבנה 186: כל תיקיית האינדקס — Library מתפצל
                check("מבנה 127: מסך ההגדרות, יעד השליחה והצירוף ללוח אינם כותבים הגדרה בעצמם — רק דרך המנהל (Acts.setting)",
                        !scr127.contains("Conf.set(") && !tr127.contains("Conf.set(") && !bar127.contains("Conf.set") && scr127.contains("Acts.setting(")
                                && tr127.contains("Acts.setting(") && bar127.contains("Acts.setting(") && !lib127src.contains("setUpdateLevel") && !lib127src.contains("setPeriodicMinutes")
                                && read(new File(code, "masachim/rechivim/Acts.java")).contains("Manager.setting("));
                StringBuilder gen127 = new StringBuilder();
                boolean bool127 = false;
                for (File f : javaFiles(code)) {
                    String t = read(f), n = f.getName();
                    if (n.equals("Conf.java")) continue;
                    if (t.contains("Conf.set(Conf.GENERAL")) gen127.append(n).append(' ');
                    if (t.contains("Conf.setBool(Conf.GENERAL")) bool127 = true;
                }
                check("מבנה 127: לסעיף [כללי] של ההגדרות אין כותבים ישירים (מבנה 266: גם \"עיצוב הבית\" ירד — הרשימה ריקה) (" + gen127.toString().trim() + ")",
                        gen127.toString().trim().isEmpty() && !bool127);
                check("מבנה 127: קביעה במקום אחד (Manager.setting) — נקבעת, ושגויה — נאמר למה", Manager.setting(Conf.READERS, "4") == null
                        && "4".equals(Conf.get(Conf.GENERAL, Conf.READERS, "")) && Manager.setting(Conf.READERS, "הרבה") != null && Manager.setting("אין הגדרה כזו", "1") != null);
                Conf.set(Conf.GENERAL, Conf.READERS, "8");
                Conf.set(Conf.GENERAL, Conf.DELETED, was127.isEmpty() ? Conf.DELETED_REMOVE : was127);
                Conf.set(Conf.GENERAL, Conf.EXCLUDED, ex127);
                Conf.set(Conf.GENERAL, Conf.PERIODIC, "0");
            }
            // מבנה 131: מכירה ומכוונת — מי שפותח נרשם אצל המנהל; פקודת פתיחה שהיעד שלה אינו רשום — אינה נפתחת
            {
                String vu131 = read(new File(code, "masachim/rechivim/VerbsUi.java"));
                check("מבנה 131: המסכים אינם מפענחים ואינם מחליטים איזה מסך נפתח — רק רושמים את הפותחים ושואלים את המנהל",
                        !vu131.contains("Verbs.parse(") && !vu131.contains("Verbs.isVerb(") && vu131.contains("Manager.open(") && vu131.contains("Manager.register("));
                boolean loaded131 = true;
                try {
                    Class.forName("com.tikiyot.vfm.VerbsUi");
                } catch (Throwable t) {
                    loaded131 = false;
                }
                StringBuilder miss131 = new StringBuilder();
                for (String sc : new String[]{Verbs.S_HOME, Verbs.S_SEARCH, Verbs.S_COMMANDS, Verbs.S_DISK, Verbs.S_VIRTUAL, Verbs.S_SETTINGS, Verbs.S_TRASH,
                        Verbs.S_EDITOR, Verbs.S_BACKUPS, Verbs.S_DIAG, Verbs.S_LAB, Verbs.S_PLUGINS})
                    if (!Manager.registered(Manager.screenKey(sc))) miss131.append(sc).append(' ');
                for (String k : new String[]{Verbs.K_LIST, Verbs.K_SMART, Verbs.K_FOLDER, Verbs.K_FIND, Verbs.K_BACK, Verbs.K_RUN, Verbs.K_NEW_LIST, Verbs.K_ADD,
                        Verbs.K_MENU, Verbs.K_NEW_CMD, Verbs.K_TOOL, Verbs.K_NOTE, Verbs.K_TITLE, Verbs.K_MODE, Manager.OPEN_DB})
                    if (!Manager.registered(k)) miss131.append(k).append(' ');
                check("מבנה 131: כל מסך וכל סוג של פקודת פתיחה רשומים אצל המנהל (" + miss131.toString().trim() + ")", loaded131 && miss131.length() == 0);
                // מבנה 142: המרשם האחד — כל הרשימות רשומות אצל המנהל, וכל אחת מוסרת בדיוק את מה שיש בה
                Map<String, List<String>> reg142 = Manager.registry();
                String line142 = Manager.registryLine();
                check("מבנה 142: המרשם — שישה סוגים, לפי הסדר (" + reg142.keySet() + ")", new ArrayList<String>(reg142.keySet()).equals(Arrays.asList(
                        Words.PermitW.R_COMMANDS, Words.PermitW.R_OPENERS, Words.PermitW.R_SYSTEM, Words.PermitW.R_PLUGINS, Words.PermitW.R_TOOLS, Words.PermitW.R_ACTIONS)));
                check("מבנה 142: כל רשימה — בדיוק מה שיש במקור שלה (" + line142 + ")",
                        reg142.get(Words.PermitW.R_COMMANDS).size() == Commands.ALL.size() && reg142.get(Words.PermitW.R_COMMANDS).contains("COPY")
                                && reg142.get(Words.PermitW.R_TOOLS).size() == Func.all().size() && reg142.get(Words.PermitW.R_ACTIONS).size() == Actions.all().size()
                                && reg142.get(Words.PermitW.R_OPENERS).contains(Manager.screenKey(Verbs.S_HOME)) && reg142.get(Words.PermitW.R_SYSTEM).contains(Permit.SYS_MENUS)
                                && line142.startsWith(Words.PermitW.R_COMMANDS + " " + Commands.ALL.size() + " · "));
                Manager.list("בדיקה142", new Manager.Lister() {
                    @Override public List<String> names() { throw new IllegalStateException("נכשלת בכוונה"); }
                });
                boolean bad142 = Manager.registry().get("בדיקה142").isEmpty() && Manager.registry().get(Words.PermitW.R_COMMANDS).size() == Commands.ALL.size();
                Manager.list("בדיקה142", null);
                check("מבנה 142: רשימה שנכשלת — ריקה (ונרשמת ביומן), והשאר נמסרות; התוכנה מוסרת את המרשם לתוסף (info.registry)",
                        bad142 && !Manager.registry().containsKey("בדיקה142") && read(new File(code, "lev/Bridge.java")).contains("o.put(\"registry\", Manager.registryLine());"));
                String line131 = "פתח מסך " + Verbs.S_SETTINGS, key131 = Manager.screenKey(Verbs.S_SETTINGS);
                final int[] called131 = {0};
                Manager.Opener db131 = Manager.register(Manager.OPEN_DB, null);   // בלי פקודות הרשימות: בבדיקה אין מסך
                Manager.Opener was131 = Manager.register(key131, null);
                String none131, done131;
                try {
                    none131 = Manager.open(null, line131, r, v);
                    Manager.register(key131, new Manager.Opener() {
                        @Override public boolean open(android.app.Activity a, Verbs.Cmd c, String full, File here) {
                            called131[0]++;
                            return true;
                        }
                    });
                    done131 = Manager.open(null, line131, r, v);
                } finally {
                    Manager.register(key131, was131);
                    Manager.register(Manager.OPEN_DB, db131);
                }
                check("מבנה 131: יעד שאינו רשום — אינו נפתח, ונאמר (" + none131 + "); רשום — המנהל מכוון אליו, פעם אחת",
                        none131 != null && none131.contains(Words.PermitW.notKnown("")) && "".equals(done131) && called131[0] == 1
                                && Manager.registered(key131) == (was131 != null));
                check("מבנה 131: שורה שאינה פקודת פתיחה — לא של המנהל הזה (null); שורה שגויה — השגיאה שלה",
                        Manager.open(null, "העתק \"א\" \"ב\"", r, v) == null && Manager.open(null, "פתח מסך אין-מסך-כזה-131", r, v) != null
                                && !Manager.open(null, "פתח מסך אין-מסך-כזה-131", r, v).isEmpty());
            }
            // מבנה 128: השלד נסגר — מי שעוד כותב או מוחק קובץ ישירות, בלב ובמסכים, הוא רשימה סגורה שרק מתקצרת
            {
                String[][] known128 = {
                        {"masachim/IniEditScreen.java", "1", "עורך קובצי ההגדרות"},
                        {"masachim/LabScreen.java", "2", "עותק פרטי של עמוד התוסף שרץ"},
                        {"masachim/rechivim/Acts.java", "1", "ניקוי עותקים ישנים בתיקיית השליחה של התוכנה"},
                        {"masachim/rechivim/BatchUi.java", "2", "עותק פרטי להצגה"},
                        // מבנה 193 (המשתמש, 16.11.2026: "מאשר"): גם קומת הפקודות בשומר. מקום חדש שכותב — להביא למשתמש להחלטה.
                        {"pkudot/CmdMatrix.java", "5", "קובצי הניסיון של הבדיקה \"כל הפקודות\" (בתיקיית הבדיקה שלה)"},
                        {"pkudot/CmdTemplates.java", "2", "קובץ התבניות של המשתמש"},
                        {"pkudot/ListOps.java", "2", "יצוא של רשימה לקובץ טקסט"},
                        {"pkudot/Reserved.java", "4", "קובצי הפקודות השמורות של התוכנה (באזור שלה)"},
                        {"pkudot/RunReport.java", "2", "הדוח של הרצה (\"דוח\")"},
                        {"pkudot/Runner.java", "6", "יומן המדידה, ההכנה לשיתוף (עותקים בתיקיית השליחה של התוכנה) והניקוי שלה"},
                        {"pkudot/Shortcuts.java", "3", "קובצי הקיצורים (\"הפקודות שלי\")"},
                };
                java.util.regex.Pattern w128 = java.util.regex.Pattern.compile(
                        "Files\\.writeAtomic\\(|\\.delete\\(\\)|\\.renameTo\\(|\\.mkdirs\\(\\)|new (?:java\\.io\\.)?FileOutputStream|new (?:java\\.io\\.)?FileWriter|saveQuiet\\(\\)|Files\\.(copy|write|append)[A-Za-z]*\\(");
                StringBuilder bad128 = new StringBuilder(), now128 = new StringBuilder();
                String root128 = code.getPath().replace('\\', '/');
                for (File f : javaFiles(code)) {
                    String rel = f.getPath().replace('\\', '/').substring(root128.length() + 1);
                    if (!rel.startsWith("lev/") && !rel.startsWith("masachim/") && !rel.startsWith("pkudot/")) continue;
                    int n = 0;
                    java.util.regex.Matcher m128 = w128.matcher(read(f));
                    while (m128.find()) n++;
                    if (n == 0) continue;
                    now128.append(rel).append('=').append(n).append(' ');
                    int allowed = 0;
                    for (String[] k : known128) if (k[0].equals(rel)) allowed = Integer.parseInt(k[1]);
                    if (n > allowed) bad128.append(rel).append(" (").append(n).append(" > ").append(allowed).append(") ");
                }
                check("מבנה 128 (193: גם בפקודות): בלב, במסכים ובפקודות — כתיבה או מחיקה ישירה של קובץ רק במקומות הידועים, ולא יותר ממה שנרשם; מקום חדש — להביא למשתמש להחלטה (" + bad128.toString().trim() + ") [" + now128.toString().trim() + "]",
                        bad128.length() == 0);
            }
            check("מבנה 84: ...ופעולות הכונן — מסורבות כולן (לא חזרו אחורה)", Permit.strict(Permit.DELETE) && Permit.strict(Permit.WRITE));
            {   // מבנה 232 ← 266: החלון הישן ירד, והשער שלו (Manager.oldDesign) איתו — אין עוד מי שכותב עיצוב שלא דרך "עצב"
                StringBuilder od = new StringBuilder();
                String root232 = code.getPath().replace('\\', '/');
                for (File f : javaFiles(code)) {
                    String t232 = read(f);
                    if (t232.contains("oldDesign(") || t232.contains("DesignUi.")) od.append(f.getPath().replace('\\', '/').substring(root232.length() + 1)).append(' ');
                }
                check("מבנה 266: Manager.oldDesign והחלון הישן — אינם בקוד (" + od.toString().trim() + ")", od.length() == 0);
            }
            {   // מבנה 238: הצבעים מהעיצוב — כל מסך מתחיל ב־Ui.begin, לפני super.onCreate; והסדר של הצבעים כמו בעיצוב (Palette)
                StringBuilder nb = new StringBuilder();
                for (File f : javaFiles(new File(code, "masachim"))) {
                    String t = read(f);
                    if (!t.contains("extends Activity")) continue;
                    int b = t.indexOf("Ui.begin(this"), s = t.indexOf("super.onCreate(");
                    if (b < 0 || s < 0 || b > s) nb.append(f.getName()).append(' ');
                }
                check("מבנה 238: כל מסך — Ui.begin לפני super.onCreate (" + nb.toString().trim() + ")", nb.length() == 0);
                int[] was = {Ui.TEXT, Ui.SUB, Ui.MUTED, Ui.ON_COLOR, Ui.BG, Ui.BG_SOFT, Ui.HEADER, Ui.HEADER_SOFT, Ui.BAR, Ui.SELECTED,
                        Ui.ACCENT, Ui.ACCENT_DARK, Ui.MARK, Ui.DOUBT, Ui.DOUBT_STRONG, Ui.RED, Ui.ICON, Ui.RULE};
                int[] light = {0xFF202124, 0xFF5F6368, 0xFF9AA0A6, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFF4F5F6, 0xFFF1F3F4, 0xFFE8EAED, 0xFFE8F0FE,
                        0xFFD2E3FC, 0xFF1A73E8, 0xFF174EA6, 0xFFFFF2A8, 0xFFFFF1C2, 0xFFF2B52B, 0xFFC5221F, 0xFF5F6368, 0xFFE0E0E0};
                Ui.use(Palette.resolve(), false);
                int[] got = {Ui.TEXT, Ui.SUB, Ui.MUTED, Ui.ON_COLOR, Ui.BG, Ui.BG_SOFT, Ui.HEADER, Ui.HEADER_SOFT, Ui.BAR, Ui.SELECTED,
                        Ui.ACCENT, Ui.ACCENT_DARK, Ui.MARK, Ui.DOUBT, Ui.DOUBT_STRONG, Ui.RED, Ui.ICON, Ui.RULE};
                check("מבנה 238: הערכה הבהירה — בדיוק הצבעים שהיו קבועים בקוד, באותו סדר (בלי שינוי במה שרואים)",
                        Arrays.equals(light, got) && Arrays.equals(light, was));
                Ui.use(Palette.resolve(Palette.K_MODE + " = " + Palette.DARK, Palette.K_COLOR + ": הדגשה = #00FF00"), true);
                check("מבנה 238: כהה — הרקע כהה והטקסט בהיר; צבע שנקבע — במקומו", Ui.BG == 0xFF202124 && Ui.TEXT == 0xFFE8EAED
                        && Ui.ACCENT == 0xFF00FF00 && Ui.RULE == 0xFF3C4043);
                Ui.use(Palette.resolve(), false);
            }
        }
        // מבנה 55: המכשיר עצר את התוכנה בזמן הרצה — הדוח אומר זאת ואיזו הגדרה פותרת
        List<String> rep55 = new ArrayList<String>();
        Runner.stoppedHint(rep55, 4000);
        check("מבנה 55: פחות מ־5 שניות — בלי שורה", rep55.isEmpty());
        Runner.stoppedHint(rep55, 58400);
        check("מבנה 55: 58 שניות — שורה אחת, עם הזמן ועם ההגדרה", rep55.size() == 1 && rep55.get(0).contains("58") && rep55.get(0).contains("ללא הגבלות"));
        Batch.Plan p55 = Batch.plan("ECHO שלום\n", r, v);
        check("מבנה 55: הרצה רגילה — בלי השורה, והדופק פועל גם בלי \"מדידה\"", !String.valueOf(Batch.run(p55, null, null, null)).contains("עצר את התוכנה") && p55.pulse != null);
        // מבנה 54: מסד בלי בית (גיבוי של אופן מהתקלה הישנה) — בית נוצר, והרשימות שבחוץ נכנסות אליו
        MemStore m54 = new MemStore(IdCounter.memory());
        long out54 = m54.createList("בדיקת מ0.45", 0), in54 = m54.createList("פנימית", out54), res54 = m54.createList(Names.RESERVED_ROOT, 0);
        check("מבנה 54: לפני — אין בית", DbPaths.home(m54) == 0);
        long h54 = DbPaths.homeWithOrphans(m54);
        check("מבנה 54: נוצר בית, והרשימה שהייתה מחוץ לו — בתוכו; רשימה פנימית ו\"שמור לתוכנה\" — במקומן",
                h54 != 0 && DbPaths.home(m54) == h54 && m54.listsOf(out54).contains(h54) && m54.listsOf(in54).size() == 1 && !m54.listsOf(res54).contains(h54));
        int members54 = m54.members(h54).size();
        check("מבנה 54: מסד שיש בו בית — לא נוגעים בו (קריאה שנייה לא משנה דבר)", DbPaths.homeWithOrphans(m54) == h54 && m54.members(h54).size() == members54);
        // מבנה 50: "ודא רציף" — בדיקת מסך כבוי שמדווחת בעצמה
        eq("מבנה 50: המסך היה כבוי והתוכנה רצה — עבר", Batch.STEADY_OK, Batch.steady(60000, 600));
        eq("מבנה 50: המסך לא כבה — לא נבדק (כישלון שאומר מה לעשות)", Batch.STEADY_SCREEN_ON, Batch.steady(4000, 0));
        eq("מבנה 50: התוכנה לא רצה — כישלון, גם אם המסך כמעט לא נספר כבוי", Batch.STEADY_STOPPED, Batch.steady(1000, 45000));
        check("מבנה 50: ודא רציף בלי \"מדידה\" — שגיאה", Words.DbCmd.STEADY_NEEDS_MEASURE.equals(Batch.plan("ודא רציף\n", r, v).steps.get(0).error));
        Batch.Plan st50 = Batch.plan(Syntax.MEASURE + "\nודא רציף\n", r, v);
        String rep50 = String.valueOf(Batch.run(st50, null, null, null));
        check("מבנה 50: במחשב אין מסך — הבדיקה אומרת שהמסך לא כבה, ונספרת כ\"ודא\" שנכשל (" + rep50 + ")",
                st50.errors == 0 && Boolean.FALSE.equals(st50.steps.get(st50.steps.size() - 1).verified) && rep50.contains("המסך לא כבה"));
        check("מבנה 50: נוסח התוצאות", Words.DbCmd.steadyResult(0, 61000, 0).startsWith("✅") && Words.DbCmd.steadyResult(0, 61000, 0).contains("61 שניות")
                && Words.DbCmd.steadyResult(2, 61000, 45000).contains("לא רצה 45 שניות"));
        // מבנה 48: צור אופן "<שם>" /מגיבוי
        Verbs.Cmd m48 = Verbs.parse("צור אופן \"ישן\" /מגיבוי");
        check("מבנה 48: /מגיבוי ב\"צור אופן\" — נקרא, והשם בלי המתג", m48.error == null && m48.fromBackup && Verbs.K_MODE.equals(m48.kind) && "ישן".equals(m48.arg));
        check("מבנה 48: בלי המתג — כמו קודם", !Verbs.parse("צור אופן \"ישן\"").fromBackup && Verbs.parse("צור אופן \"ישן\"").error == null);
        eq("מבנה 48: /מגיבוי בפועל אחר — שגיאה", Words.Verb.FROM_BACKUP_ONLY, Verbs.parse("צור רשימה \"א\" /מגיבוי").error);
        check("מבנה 48: ההודעה בקובץ פקודות אומרת את שתי הדרכים", Words.DbCmd.modeHasBackup("ישן").contains(Verbs.FROM_BACKUP) && Words.DbCmd.modeHasBackup("ישן").contains("בשם אחר"));
        // מבנה 47: התיקייה של תוסף — תמיד בתוך "נתוני תוספים", גם כששם התוסף הוא ".." או ריק
        File base47 = new File(Paths.dbData(), Lab.DATA_DIR);
        check("מבנה 47: תוסף ששמו \"..\" — התיקייה שלו בתוך \"נתוני תוספים\", לא מעליה", base47.equals(Lab.dataDir("..").getParentFile())
                && base47.equals(Lab.dataDir(".").getParentFile()) && base47.equals(Lab.dataDir("").getParentFile()) && base47.equals(Lab.dataDir(null).getParentFile()));
        eq("מבנה 47: שם רגיל — כמו קודם", new File(base47, "אבחון"), Lab.dataDir("אבחון"));
        // מבנה 46: "כתוב" על קובץ קיים — הקודם לסל
        new File(r, "ת46").mkdirs();
        File w46 = new File(r, "ת46/כתוב46.txt");
        try { java.io.FileOutputStream o = new java.io.FileOutputStream(w46); o.write("ישן".getBytes("UTF-8")); o.close(); } catch (java.io.IOException e) { throw new RuntimeException(e); }
        Index ix46 = new Index("מבנה46", "מבנה46", r.getAbsolutePath());
        ix46.scan(null);
        Library lib46 = Library.forTests(new ArrayList<Index>(java.util.Collections.singletonList(ix46)));
        int trash46 = SmallCmds.trashed(lib46, w46.getAbsolutePath(), false).size();
        Batch.Plan pw46 = Batch.plan(Syntax.WRITE + " \"ת46\\כתוב46.txt\" \"חדש\" /תפוס:להחליף\n" + Syntax.WRITE + " \"ת46\\כתוב46-ב.txt\" \"א\"\n" + Syntax.WRITE + " \"ת46\\כתוב46-ב.txt\" \"ב\" " + Syntax.SW_APPEND + "\n", r, v, lib46);
        check("מבנה 46: התכנון אומר שהקובץ הקיים יעבור לסל — ורק עליו", pw46.errors == 0 && pw46.steps.get(0).describe.contains(Words.DbCmd.OLD_TO_TRASH_WILL)
                && !pw46.steps.get(1).describe.contains(Words.DbCmd.OLD_TO_TRASH_WILL) && !pw46.steps.get(2).describe.contains(Words.DbCmd.OLD_TO_TRASH_WILL));
        String rep46 = String.valueOf(Batch.run(pw46, lib46, null, null));
        lib46.flush(5000);
        List<Trash.Item> in46 = SmallCmds.trashed(lib46, w46.getAbsolutePath(), false);
        check("מבנה 46: \"כתוב\" על קובץ קיים — התוכן החדש נכתב, והקודם בסל עם התוכן שלו (" + rep46 + ")", !pw46.trouble && "חדש".equals(read(w46))
                && in46.size() == trash46 + 1 && "ישן".equals(read(new File(in46.get(in46.size() - 1).path()))) && rep46.contains(Words.DbCmd.OLD_TO_TRASH));
        check("מבנה 46: /הוסף — מוסיף, ושום דבר לא עובר לסל", "אב".equals(read(new File(r, "ת46/כתוב46-ב.txt")))
                && SmallCmds.trashed(lib46, new File(r, "ת46/כתוב46-ב.txt").getAbsolutePath(), false).isEmpty());
        // מבנה 110 (ה6): "כתוב" על קובץ קיים — בלי הוראה עוצר; /תפוס:לדלג — ממשיכים; /תפוס:לשנות-שם — "(2)"; לשאול / עם /הוסף — שגיאה
        Batch.Plan stop110 = Batch.plan(Syntax.WRITE + " \"ת46\\כתוב46.txt\" \"עוד\"\n", r, v, lib46);
        Batch.Plan go110 = Batch.plan(Syntax.WRITE + " \"ת46\\כתוב46.txt\" \"עוד\" /תפוס:לדלג\n" + Syntax.WRITE + " \"ת46\\כתוב46.txt\" \"שני\" /תפוס:לשנות-שם\n", r, v, lib46);
        String rep110 = go110.errors == 0 ? String.valueOf(Batch.run(go110, lib46, null, null)) : go110.errorText();
        lib46.flush(5000);
        check("מבנה 110: \"כתוב\" על קובץ קיים — בלי מתג: שם קיים, הקובץ נעצר; לדלג: בלי שינוי (ונאמר); לשנות-שם: \"(2)\" (" + rep110 + ")",
                stop110.blocked() && stop110.errorText().contains(Words.Cmd.fileTaken("כתוב46.txt"))
                && "חדש".equals(read(w46)) && rep110.contains(Words.Cmd.fileSkipped("כתוב46.txt"))
                && "שני".equals(read(new File(r, "ת46/כתוב46 (2).txt"))));
        check("מבנה 110: /תפוס:לשאול ב\"כתוב\", ו־/תפוס עם /הוסף — שגיאה (לא נבלע בשקט)",
                Batch.plan(Syntax.WRITE + " \"ת46\\כתוב46.txt\" \"x\" /תפוס:לשאול\n", r, v, lib46).errorText().contains(Words.Cmd.WRITE_TAKEN_ONLY)
                && Batch.plan(Syntax.WRITE + " \"ת46\\כתוב46.txt\" \"x\" /הוסף /תפוס:להחליף\n", r, v, lib46).errorText().contains(Words.Cmd.WRITE_TAKEN_ONLY));
        // מבנה 45: קובץ שהמנוע כותב בעצמו — לא על קובץ של התוכנה
        String set45 = app42 + "/" + Paths.SETTINGS + "/x.txt";
        check("מבנה 45: דוח על קובץ של התוכנה — שגיאה", Batch.plan("דוח \"" + set45 + "\"\nMD \"ת45\"\n", r, v).errors == 1);
        check("מבנה 45: דוח על תיקייה — שגיאה", Batch.plan("דוח \"ק42\"\nMD \"ת45\"\n", r, v).errors == 1);
        check("מבנה 45: דוח בתיקייה של המשתמש שבתוך תיקיית התוכנה — מותר", Batch.plan("דוח \"" + app42 + "/" + Paths.USER_DIRS[1] + "/דוח.txt\"\nMD \"ת45\"\n", r, v).errors == 0);
        check("מבנה 45: /יומן: על קובץ של התוכנה — שגיאה", Batch.plan("COPY \"פתיחה.txt\" \"ק42\" " + Syntax.SW_JOURNAL + "\"" + set45 + "\"\n", r, v).errors == 1);
        check("מבנה 45: מדידה עם יומן על קובץ של התוכנה — שגיאה", Batch.plan(Syntax.MEASURE + " " + Syntax.SW_JOURNAL + "\"" + set45 + "\"\nMD \"ת45\"\n", r, v).errors == 1);
        check("מבנה 45: כתוב על קובץ של התוכנה — שגיאה כבר בתכנון", Batch.plan(Syntax.WRITE + " \"" + set45 + "\" \"א\"\n", r, v).errors == 1);
        // מבנה 44: העברה של תיקייה שלמה — ההדמיה מעבירה גם את מה שבתוכה
        File d44 = new File(r, "ת44/עמוק");
        d44.mkdirs();
        new File(r, "יעד44").mkdirs();
        new File(r, "אחר44").mkdirs();
        try {
            java.io.FileOutputStream o = new java.io.FileOutputStream(new File(r, "ת44/א.txt")); o.write(1); o.close();
            o = new java.io.FileOutputStream(new File(d44, "ב.txt")); o.write(2); o.close();
        } catch (java.io.IOException e) { throw new RuntimeException(e); }
        Batch.Plan old44 = Batch.plan("MOVE \"ת44\" \"יעד44\"\nDEL \"ת44\\א.txt\"\n", r, v);
        check("מבנה 44: אחרי העברת התיקייה — קובץ במקום הישן כבר לא נמצא (עד כאן תוכנן למחיקה)", old44.errors == 1 && old44.steps.get(1).error != null);
        Batch.Plan new44 = Batch.plan("MOVE \"ת44\" \"יעד44\"\nCOPY \"יעד44\\ת44\\עמוק\\ב.txt\" \"אחר44\"\nRD \"יעד44\\ת44\\עמוק\"\n", r, v);
        check("מבנה 44: ...ובמקום החדש הוא נמצא, גם בתת־תיקייה (עד כאן \"לא נמצא\", והקובץ כולו לא רץ)",
                new44.errors == 1 && new44.steps.get(1).error == null && new44.steps.get(1).files.size() == 1 && Words.Cmd.FOLDER_NOT_EMPTY.equals(new44.steps.get(2).error));
        Batch.Plan run44 = Batch.plan("MD \"ת44\\חדשה\"\nCOPY \"פתיחה.txt\" \"ת44\\חדשה\"\nMOVE \"ת44\" \"יעד44\"\nCOPY \"יעד44\\ת44\\חדשה\\פתיחה.txt\" \"אחר44\"\nMD \"ת44\"\nRD \"ת44\"\n", r, v, lib42);
        eq("מבנה 44: גם מה ששורות קודמות יצרו בתיקייה עובר איתה; ותיקייה חדשה בשם הישן — ריקה", 0, run44.errors);
        String rep44 = String.valueOf(Batch.run(run44, lib42, null, null));
        check("מבנה 44: ...וההרצה עושה בדיוק את מה שתוכנן (" + rep44 + ")", !run44.trouble && new File(r, "אחר44/פתיחה.txt").isFile()
                && new File(r, "יעד44/ת44/עמוק/ב.txt").isFile() && !new File(r, "ת44").exists());
        Batch.Plan delEmpty = Batch.plan("DEL \"\"\n", r, v);
        check("מבנה 41: DEL \"\" — לא מוחק את כל התיקייה: שגיאה, ושום שורה לא רצה", delEmpty.errors == 1 && delEmpty.blocked());
        // מבנה 40: הדמיה — המנוע עצמו אינו מריץ; וכישלון שאינו "ודא" נאמר בסיכום
        File asIfDir = new File(r, "בהדמיה-לא-נוצרת");
        List<String> asIfRep = Batch.run(Batch.plan(Syntax.AS_IF + "\nMD \"בהדמיה-לא-נוצרת\"\n", r, v), null, null, null);
        check("מבנה 40: תכנית של הדמיה שנמסרה למנוע — לא בוצעה, והדוח אומר זאת", !asIfDir.exists() && String.valueOf(asIfRep).contains(Words.Run.AS_IF));
        eq("מבנה 40: נוסח שורת הכישלונות", "⚠ כישלונות בהרצה: 2 — הפרטים בשורות שלמעלה", Words.DbCmd.failsSummary(2));
        Batch.Plan goOn = Batch.plan("המשך ()\nCOPY\nECHO סוף\n", r, v);
        List<String> goOnRep = Batch.run(goOn, null, null, null);
        check("מבנה 40: שורה שנכשלה וההרצה המשיכה — הכישלון כתוב גם בסיכום", !goOn.blocked() && String.valueOf(goOnRep).contains(Words.DbCmd.failsSummary(1)));
        List<String> cleanRep = Batch.run(Batch.plan("ECHO שלום\n", r, v), null, null, null);
        check("מבנה 40: הרצה בלי כישלון — בלי שורת כישלונות", !String.valueOf(cleanRep).contains("כישלונות בהרצה"));
        // מבנה 39: העברה בין כוננים — המקור נמחק רק אחרי השוואת חתימה מלאה של העותק
        File ma = new File(r, "מקור39.bin"), mb = new File(r, "יעד39.bin"), mc = new File(r, "אחר39.bin");
        try {
            java.io.FileOutputStream o = new java.io.FileOutputStream(ma);
            byte[] big = new byte[300000];
            for (int i = 0; i < big.length; i++) big[i] = (byte) (i * 31 + i / 7);
            o.write(big);
            o.close();
            big[150000] ^= 1;                                                   // אותו גודל, בית אחד שונה
            o = new java.io.FileOutputStream(mc);
            o.write(big);
            o.close();
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
        String da = FileCopy.digest(ma);
        check("מבנה 39: חתימה מלאה — בית אחד שונה באותו גודל נתפס", da != null && ma.length() == mc.length() && !da.equals(FileCopy.digest(mc)));
        String[] got = new String[1];
        check("מבנה 39: ההעתקה מוסרת את חתימת המקור בלי קריאה נוספת, והעותק זהה", FileCopy.copyFile(ma, mb, got) && da.equals(got[0]) && da.equals(FileCopy.digest(mb)));
        mb.delete();
        check("מבנה 39: העברה בין כוננים — העותק נבדק, ורק אז המקור נמחק", FileCopy.moveFile(ma, mb, false) && !ma.exists() && da.equals(FileCopy.digest(mb)));
        check("מבנה 39: קובץ שאינו נקרא — אין חתימה (ולכן אין מחיקה של מקור)", FileCopy.digest(new File(r, "אין-קובץ-כזה")) == null);
        // מבנה 38: הדופק — כמה זמן התוכנה לא רצה
        Runner.Pulse pulse = new Runner.Pulse();
        try { Thread.sleep(1300); } catch (InterruptedException e) { throw new RuntimeException(e); }
        check("מבנה 38: התוכנה רצה כרגיל — הדופק לא מדווח על עצירה", pulse.missed() == 0);
        pulse.stop();
        eq("מבנה 38: פחות משנייה — אין תוספת לסיכום", "", Words.Loop.notRunning(900));
        check("מבנה 38: עצירה של דקה — כתובה בסיכום של המדידה", Words.Loop.notRunning(67000).contains("67.0 שניות"));
        Batch.Plan measuredPlan = Batch.plan("מדידה\nECHO דופק\n", r, v);
        List<String> pulseReport = Batch.run(measuredPlan, null, null, null);
        check("מבנה 38: הרצה עם מדידה — יש דופק, והוא נעצר בסוף; בלי עצירה אין תוספת",
                measuredPlan.pulse != null && String.valueOf(pulseReport).contains("סך הכול") && !String.valueOf(pulseReport).contains("לא רצה"));
        String bat = read(new File(code, "masachim/rechivim/BatchUi.java")), run2 = read(new File(code, "masachim/RunScreen.java"));
        check("מבנה 38: שני רצפי ההרצה מחזיקים את המכשיר ער, ומשחררים בסוף",
                bat.contains("Awake.hold(a)") && bat.contains("Awake.release(awake)") && run2.contains("Awake.hold(RunScreen.this)") && run2.contains("Awake.release(awake)")
                        && read(new File(System.getProperty("vfm.src"), "AndroidManifest.xml")).contains("android.permission.WAKE_LOCK"));
        String dls = read(new File(code, "masachim/DbListScreen.java"));
        check("מבנה 37: שורה שנעלמה מהמסד מתחת למסך אינה מקריסה אותו (הקריסה ב\"שחזור הדוגמאות\", נ0.2) — הציור עטוף, והמסך נטען מחדש",
                dls.contains("boolean missing = gone instanceof Store.Failure") && dls.contains("private void bindRow(View v, Store.Member row)") && dls.contains("Words.Db.ROW_GONE"));
        // מבנה 60: גם הטעינה עצמה עטופה (רשימה שנמחקה בין קריאה לקריאה), שגיאה שאינה של המסד אינה מוסתרת, ועריכה של פריט שנעלם — הודעה
        check("מבנה 60: הטעינה של מסך הרשימות עטופה — הודעה ורישום ביומן, לא קריסה",
                dls.contains("private void load()") && dls.contains("Log.e(\"DbListScreen.refresh\", gone)") && dls.indexOf("load();") < dls.indexOf("private void load()"));
        check("מבנה 60: בציור שורה — רק שגיאות של המסד נתפסות; תקלה אחרת בקוד נזרקת הלאה",
                dls.contains("if (!(gone instanceof android.database.SQLException) && !(gone instanceof IllegalStateException)) throw gone;")
                        && dls.contains("if (missing && !stale)"));
        // מבנה 99: הטופס של הרשימה החכמה עבר לפקודה — ההגנה עברה איתו (planPickedSmartEdit), ונבדקת שם גם בהרצה (DbCmdTests, מבנה 99)
        String dlc60 = read(new File(code, "pkudot/ListVerbs.java"));   // מבנה 169: הטפסים עברו מ־DbListCmd ל־ListVerbs — אותה בדיקה
        int at60 = dlc60.indexOf("static String planPickedSmartEdit(");
        // מבנה 113: גם הטופס של הרשימה עבר לפקודה — ההגנה עברה איתו (edited, שגם השורה הכתובה עוברת בו), ונבדקת בהרצה (DbCmdTests, מבנה 113)
        int ed60 = dlc60.indexOf("static DbListCmd.Res edited(");
        check("מבנה 60: עריכה של פריט שנמחק בזמן שהטופס פתוח — הודעה ברורה (בשני הטפסים, שניהם בפקודה: הרשימה — מבנה 113; החכמה — מבנה 99)",
                at60 > 0 && ed60 > 0
                        && dlc60.indexOf("Words.Db.ITEM_GONE", at60) > at60 && dlc60.indexOf("Words.Db.ITEM_GONE", at60) < dlc60.indexOf("\n    }\n", at60)
                        && dlc60.indexOf("Words.Db.ITEM_GONE", ed60) > ed60 && dlc60.indexOf("Words.Db.ITEM_GONE", ed60) < dlc60.indexOf("\n    }\n", ed60)
                        && dls.contains("Manager.onListEdit(") && dls.contains("Manager.onSmartEdit(")
                        && Words.Db.ITEM_GONE.contains("לא נשמר"));
        String bu = read(new File(code, "masachim/rechivim/BatchUi.java")), rs = read(new File(code, "masachim/RunScreen.java"));
        check("מבנה 27: שני הרצפים שואלים את המנהל, ואינם מחליטים בעצמם",
                bu.contains("Manager.approve(plan)") && rs.contains("Manager.approve(plan)") && !bu.contains("plan.blocked()") && !rs.contains("plan.blocked()")
                        && bu.contains("Manager.route(text, false)") && !bu.contains("Batch.wantsCheck(")
                        && bu.contains("Manager.fullReport(plan)") && !bu.contains("plan.measure"));
    }

    private static void m045checks() {
        File r = new File(home, "כונן-045ב");
        wipe(r);
        r.mkdirs();
        java.util.Map<String, String> v = Parse.vars(r, r.getAbsolutePath(), null);
        // ודא תקין — מתחילת הקטע
        Batch.Plan p = Batch.plan("המשך ()\nקטע \"א\"\nכתוב \"%כאן%\\א.txt\" \"x\"\nודא תקין\nקטע \"ב\"\nמחק \"%כאן%\\אין\\*.txt\"\nREN \"%כאן%\\אין.txt\" \"ב.txt\"\nודא תקין\nקטע \"ג\"\nודא תקין\n", r, v);
        List<String> rep = Batch.run(p, null, null, null);
        String all = rep.toString();
        int ok = 0, bad = 0;
        for (String x : rep) {
            if (x.contains("✅ " + Words.DbCmd.OK_SINCE_WHAT)) ok++;
            if (x.contains("❌ " + Words.DbCmd.OK_SINCE_WHAT)) bad++;
        }
        check("מ0.45: ודא תקין — ✅ בקטע בלי כישלון, ❌ בקטע שנכשל בו, ושוב ✅ בקטע הבא (" + all + ")", ok == 2 && bad == 1);
        // מדידה /עד:
        Batch.Plan m = Batch.plan("מדידה /עד:300אלפיות\nהמתן 1\nכתוב \"%כאן%\\ג.txt\" \"x\"\nודא \"%כאן%\\ג.txt\" קיים\n", r, v);
        eq("מ0.45: מדידה /עד: — בלי שגיאות", 0, m.errors);
        List<String> mr = Batch.run(m, null, null, null);
        String ms = mr.toString();
        boolean slow = false;
        for (String x : mr) if (x.startsWith(Words.Cmd.line("2", "")) && x.contains("❌ חרגה מהזמן")) slow = true;
        check("מ0.45: ...השורה שחרגה — ❌ אחרי \"שורה N:\", ונספרת עם הנכשלות (" + ms + ")", slow && ms.contains("ודא: 1 עברו · 1 נכשלו") && m.trouble);
        eq("מ0.45: מדידה /עד: עם שתי מילים (\"/עד:2 דקות\")", 120000L, Batch.plan("מדידה /עד:2 דקות\n", r, v).lineLimit);
        eq("מ0.45: מדידה /עד: לא מובן — שגיאה", 1, Batch.plan("מדידה /עד:מחר\n", r, v).errors);
        // השמות בשתי מילים
        eq("מ0.45: \"בטל אופן\" — DROPMODE", "DROPMODE \"זמני\"", Commands.normal("בטל אופן \"זמני\""));
        eq("מ0.45: ...\"מחק אופן\" (השם הראשון) — עדיין נקרא", "DROPMODE \"זמני\"", Commands.normal("מחק אופן \"זמני\""));
        eq("מ0.45: העורך כותב \"בטל אופן\"", "בטל אופן \"זמני\"", Ivrit.hebrew("DROPMODE \"זמני\""));
        eq("מ0.45: \"בטל לא-משויכים\" ו\"בטל ידנית\" — כמו קודם", "בטל ידנית \"א\"", Commands.normal("בטל ידנית \"א\""));
        eq("מ0.45: \"רוקן סל\" לבד — PURGE", "PURGE", Commands.normal("רוקן סל"));
        eq("מ0.45: \"שחזר מהסל\" — UNTRASH", "UNTRASH \"א\"", Commands.normal("שחזר מהסל \"א\""));
        eq("מ0.45: \"מחק תיקייה\" — עדיין RD", "RD \"א\"", Commands.normal("מחק תיקייה \"א\""));
        eq("מ0.45: העורך כותב בעברית", "שחזר מהסל \"א\"", Ivrit.hebrew("UNTRASH \"א\""));
        check("מ0.45: מחק אופן / ודא אופן — רק במסד (במחשב: שגיאה מוסברת)",
                Words.DbCmd.VERIFY_DB_ONLY.equals(Batch.plan("בטל אופן \"זמני\"\n", r, v).steps.get(0).error)
                        && Words.DbCmd.VERIFY_DB_ONLY.equals(Batch.plan("ודא אופן \"זמני\" חסר\n", r, v).steps.get(0).error));
        eq("מ0.45: מחק אופן בלי שם — שגיאה", Words.DbCmd.DROP_MODE_USAGE, Batch.plan("בטל אופן\n", r, v).steps.get(0).error);
        // רענן מלא /קוראים:
        eq("מ0.45: רענן מלא /קוראים:4 — בלי שגיאות", 0, Batch.plan("רענן מלא /קוראים:4\n", r, v).errors);
        check("מ0.45: ...ובתצוגה — כמה קוראים", Batch.plan("רענן מלא /קוראים:4\n", r, v).steps.get(0).describe.contains("4 קוראים"));
        eq("מ0.45: /קוראים: בלי מלא — שגיאה", 1, Batch.plan("רענן /קוראים:4\n", r, v).errors);
        eq("מ0.45: /קוראים:17 — שגיאה", 1, Batch.plan("רענן מלא /קוראים:17\n", r, v).errors);
        wipe(r);
    }

    private static void m042renamed() {
        MemStore s = new MemStore(IdCounter.memory());
        long h = DbPaths.ensureHome(s);
        long a = s.createList("לקוחות", h), b = s.createList("פעילים", a), other = s.createList("פעילים", h);
        long sm = s.createSmart("חדשים", a);
        s.rename(b, "פעילים 2026");
        String m = "עבודה";
        eq("מ0.42: פתח רשימה — הדרך המלאה", "פתח רשימה \"לקוחות/פעילים 2026\" מיון:שם",
                Verbs.renamed("פתח רשימה \"לקוחות/פעילים\" מיון:שם", s, m, b, false, "פעילים", "פעילים 2026"));
        eq("מ0.42: ...עם \\ ובלי מרכאות — המפריד נשאר", "פתח רשימה לקוחות\\פעילים 2026",
                Verbs.renamed("פתח רשימה לקוחות\\פעילים", s, m, b, false, "פעילים", "פעילים 2026"));
        check("מ0.42: רשימה אחרת באותו שם (בבית) — לא נוגעים", Verbs.renamed("פתח רשימה \"פעילים\"", s, m, b, false, "פעילים", "פעילים 2026") == null);
        eq("מ0.42: שורת רשימות — שני הצדדים, רק זה שפונה אליה", "העתק ידנית \"לקוחות/פעילים 2026\" ידנית \"פעילים\"",
                Verbs.renamed("העתק ידנית \"לקוחות/פעילים\" ידנית \"פעילים\"", s, m, b, false, "פעילים", "פעילים 2026"));
        eq("מ0.42: /בלי-לפתוח נשאר", "צור רשימה \"לקוחות/פעילים 2026/ינואר\" /בלי-לפתוח",
                Verbs.renamed("צור רשימה \"לקוחות/פעילים/ינואר\" /בלי-לפתוח", s, m, b, false, "פעילים", "פעילים 2026"));
        check("מ0.42: עם משתנה — לא נוגעים", Verbs.renamed("פתח רשימה \"%שם%/פעילים\"", s, m, b, false, "פעילים", "פעילים 2026") == null);
        // רשימה שבאמצע הדרך של חכמה; וחכמה עצמה
        s.rename(a, "לקוחות ותיקים");
        eq("מ0.42: חכמה שבתוך רשימה ששינתה שם", "פתח רשימה חכמה \"לקוחות ותיקים/חדשים\"",
                Verbs.renamed("פתח רשימה חכמה \"לקוחות/חדשים\"", s, m, a, false, "לקוחות", "לקוחות ותיקים"));
        s.rename(sm, "חדשים השבוע");
        eq("מ0.42: חכמה ששינתה שם — שורת רשימות", "העתק חכמה \"חדשים השבוע\" ידנית \"ארכיון\"",
                Verbs.renamed("העתק חכמה \"חדשים\" ידנית \"ארכיון\"", s, m, sm, true, "חדשים", "חדשים השבוע"));
        check("מ0.42: ...וחכמה — לא נוגעת בשורה של ידנית באותו שם", Verbs.renamed("פתח רשימה \"חדשים\"", s, m, sm, true, "חדשים", "חדשים השבוע") == null);
        eq("מ0.42: החלק בדרך — רק הוא", "א/ ב חדש /ג", Verbs.segment("א/ ב /ג", 1, "ב חדש"));
        // הקבצים: קובץ פקודות שבתוך רשימה של המסד — מתעדכן; אחרי "אופן" אחר — לא; ההודעה
        File r = new File(home, "כונן-042ב");
        wipe(r);
        r.mkdirs();
        File cmd = new File(r, "פותח.vbat");
        try {
            Files.writeAtomic(cmd, "פתח רשימה \"לקוחות ותיקים/פעילים 2026\"\nאופן \"אחר\"\nפתח רשימה \"לקוחות ותיקים/חדשים השבוע\"\n");
            DbFiles.add(s, other, java.util.Collections.singletonList(cmd.getAbsolutePath()), null, false);
            s.rename(b, "פעילים 2027");
            List<File> done = Shortcuts.listRenamed(s, m, b, "פעילים 2026", "פעילים 2027");
            String now = Files.readUtf8(cmd);
            check("מ0.42: קובץ פקודות ברשימה — עודכן, ומה שאחרי \"אופן\" אחר — לא (" + now + ")", done.contains(cmd)
                    && now.startsWith("פתח רשימה \"לקוחות ותיקים/פעילים 2027\"\n") && now.contains("אופן \"אחר\"\nפתח רשימה \"לקוחות ותיקים/חדשים השבוע\""));
            check("מ0.42: אותו שם — כלום", Shortcuts.listRenamed(s, m, b, "פעילים 2027", "פעילים 2027").isEmpty());
            eq("מ0.42: ההודעה — קובץ אחד", " · עודכן קובץ פקודות אחד שפונה לרשימה: פותח", Words.DbCmd.followed(java.util.Arrays.asList("פותח")));
            eq("מ0.42: ההודעה — כמה", " · עודכנו 2 קובצי פקודות שפונים לרשימה: א, ב", Words.DbCmd.followed(java.util.Arrays.asList("א", "ב")));
            List<String> rep = new ArrayList<String>();
            for (int i = 1; i <= 20; i++) rep.add("שורה " + i + ": " + (i == 7 ? "❌ \"א\" — בפועל: חסרה" : "✅ \"ב\" קיימת"));
            rep.add(Words.DbCmd.verifySummary(19, 1));
            String ll = RunReport.logLine(rep);
            check("מ0.43: דוח ארוך ביומן — בקיצור: כמה שורות, ה־❌, והסיכום (" + ll + ")", ll.startsWith("21 שורות בדוח") && ll.contains("שורה 7: ❌")
                    && !ll.contains("שורה 8:") && ll.contains("ודא: 19 עברו · 1 נכשלו"));
            eq("מ0.43: דוח קצר — כמו שהוא", "[שורה 1: נוצרה א]", RunReport.logLine(java.util.Collections.singletonList("שורה 1: נוצרה א")));
            DbListCmd.Res res = DbListCmd.Res.ok("x").renamed(b, "א", "א");
            check("מ0.42: Res — שם שלא השתנה אינו שינוי שם", res.renamed == 0);
        } catch (Exception e) {
            check("מ0.42: קובצי פקודות — " + e, false);
        }
    }

    private static void m042cmds() {
        File r = new File(home, "כונן-042");
        wipe(r);
        r.mkdirs();
        java.util.Map<String, String> v = Parse.vars(r, r.getAbsolutePath(), null);
        Verbs.Cmd m = Verbs.parse("צור אופן \"בדיקות מערכת\"");
        check("מ0.42: צור אופן — השם", m.error == null && m.kind.equals(Verbs.K_MODE) && m.arg.equals("בדיקות מערכת"));
        check("מ0.42: צור אופן בלי שם — שגיאה", Verbs.parse("צור אופן").error != null);
        check("מ0.42: צור אופן — אינו נפתח בסוף, ובמחשב (בלי מסד) שגיאה", Batch.plan("צור אופן \"א\"\n", r, v).open == null
                && Words.DbCmd.VERIFY_DB_ONLY.equals(Batch.plan("צור אופן \"א\"\n", r, v).steps.get(0).error));
        check("מ0.42: צור אופן — שם לא תקין", Batch.plan("צור אופן \"א:ב\"\n", r, v).steps.get(0).error.startsWith("שם אופן לא תקין"));
        Batch.Plan d = Batch.plan("ודא מסד\n", r, v);
        check("מ0.42: ודא מסד — במחשב אין מסד: שגיאה, ולא נבלע", d.errors == 1 && Words.DbCmd.VERIFY_DB_ONLY.equals(d.steps.get(0).error));
        File f = new File(r, "א.txt");
        Batch.Plan x = Batch.plan("כתוב \"%כאן%\\א.txt\" \"x\"\nודא \"%כאן%\\א.txt\" באינדקס\n", r, v);
        check("מ0.42: ודא … באינדקס — מתוכנן", x.errors == 0 && x.steps.get(1).describe.contains("באינדקס"));
        Batch.run(x, null, null, null);
        check("מ0.42: ודא … באינדקס — בלי אינדקס: ❌ (ולא ✅ בטעות)", f.isFile() && Boolean.FALSE.equals(x.steps.get(1).verified));
        check("מ0.42: ודא … באינדקס עם עוד מילה — שגיאה", Batch.plan("ודא \"%כאן%\\א.txt\" באינדקס קיים\n", r, v).errors == 1);
        check("מ0.42: הטקסט של התוצאה — באינדקס / לא, ובכונן", Words.DbCmd.indexGot(false, true).contains("לא באינדקס")
                && Words.DbCmd.indexGot(false, true).contains("קיים") && Words.DbCmd.indexGot(true, true).startsWith("באינדקס"));
    }

    private static void m041() {
        File r = new File(home, "כונן-041");
        wipe(r);
        r.mkdirs();
        java.util.Map<String, String> v = Parse.vars(r, r.getAbsolutePath(), null);
        Batch.Plan p = Batch.plan("בטל לא-משויכים\nודא לא-משויכים /פריטים:0\nהעתק לא-משויכים ידנית \"א\"\n", r, v);
        check("מ0.41: בטל לא-משויכים — שורה של המסד (במחשב אין מסד)", p.steps.size() == 3 && Words.DbCmd.NO_DB.equals(p.steps.get(0).error)
                && Words.DbCmd.VERIFY_DB_ONLY.equals(p.steps.get(1).error) && p.steps.get(2).error != null);
        new File(r, "לא-משויכים").mkdirs();
        Batch.Plan q = Batch.plan("בטל \"לא-משויכים\"\n", r, v);
        check("מ0.41: \"לא-משויכים\" במרכאות — תיקייה רגילה", q.errors == 0 && q.steps.get(0).cmd.equals("RD"));
        // קובץ שכולו פקודת פתיחה אחת — רץ מיד (הכפתורים בשורות הם קובצי פקודות)
        eq("מ0.41: צלמית, שם ופקודת פתיחה — השורה", "פתח מסך הבית", Parse.singleVerb("צלמית 🏠\nשם בית\nפתח מסך הבית\n"));
        eq("מ0.41: ...גם עם הערה, שורה ריקה ו\"למה\"", "חזור", Parse.singleVerb("צלמית →\nלמה חוזרים\n\nהערה סתם\n:: עוד\nחזור\n"));
        check("מ0.41: שתי פקודות — לא", Parse.singleVerb("פתח מסך הבית\nפתח מסך חיפוש\n") == null);
        check("מ0.41: פקודת קבצים — לא", Parse.singleVerb("צלמית ▶\nהעתק \"א\" \"ב\"\n") == null && Parse.singleVerb("מחק \"א\"\n") == null);
        check("מ0.41: \"צור תיקייה\" אינה פקודת פתיחה", Parse.singleVerb("צור תיקייה \"א\"\n") == null);
        check("מ0.41: \"בדיקה\" או \"הדמיה\" בראש — לא (מסך ההרצה)", Parse.singleVerb("בדיקה\nפתח מסך הבית\n") == null
                && Parse.singleVerb("הדמיה\nפתח מסך הבית\n") == null);
        check("מ0.41: קובץ ריק — לא", Parse.singleVerb("") == null && Parse.singleVerb("צלמית ▶\n") == null);
        for (String[] x : Reserved.system())
            check("מ0.41: פקודת המערכת \"" + x[1] + "\" — פקודת פתיחה אחת", x[2].equals(Parse.singleVerb(Program.ICON + " " + x[0] + "\n" + Program.NAME + " " + x[1] + "\n" + x[2] + "\n")));
        // שמות המסכים של השורות — של "פתח מסך", ועוד שניים
        check("מ0.41: שמות המסכים — \"רשימה\" ו\"רשימה חכמה\" אינם שמות של \"פתח מסך\"", !java.util.Arrays.asList(Verbs.SCREENS).contains(Verbs.S_LIST_SCREEN)
                && !java.util.Arrays.asList(Verbs.SCREENS).contains(Verbs.S_SMART_SCREEN) && java.util.Arrays.asList(Verbs.SCREENS).contains(Verbs.S_HOME));
        eq("מ0.41: דרך של שורה כללית", java.util.Arrays.asList("שורה למעלה 2"), Menus.rowPath(null, Menus.ROW_TOP, 2));
        eq("מ0.41: דרך של שורה של מסך", java.util.Arrays.asList("הבית", "שורה למטה 1"), Menus.rowPath(Verbs.S_HOME, Menus.ROW_BOTTOM, 1));
        eq("מ0.41: \"שמור לתוכנה/פקודות\" — בתוך התפריטים", Menus.CMDS, Menus.relative(Names.parsePath("שמור לתוכנה/פקודות")));
        eq("מ0.41: \"בחירה\" — כמו שהיא", Menus.SELECT_PATH, Menus.relative(Names.parsePath("בחירה")));
        check("מ0.41: הפקודה של העורך האינטראקטיבי — בין הכלים, ופותחת את התוסף", toolLine(Words.DbCmd.TOOL_EDITOR).equals("פתח תוסף \"עורך אינטראקטיבי\"")
                && Verbs.parse(toolLine(Words.DbCmd.TOOL_EDITOR)).error == null && Verbs.S_PLUGINS.equals(Verbs.parse(toolLine(Words.DbCmd.TOOL_EDITOR)).arg));
        // מבנה 203 (המשתמש, 16.11.2026: "הרצת העורך אמורה להיות מובנת בתוכנה"): כניסה קבועה לעורך בתפריט, ישר — בלי קובץ פקודות באמצע
        String dls203 = read(new File(System.getProperty("vfm.src"), "src/com/tikiyot/vfm/masachim/DbListScreen.java"));
        // מבנה 206 (המשתמש, 16.11.2026): שורות הכפתורים — עריכה במקום (התפריט של עד מ0.40), אבל כל שינוי הוא פקודה על הרשימה של השורה
        String tr206 = read(new File(System.getProperty("vfm.src"), "src/com/tikiyot/vfm/masachim/rechivim/TopRow.java"));
        check("מבנה 206: נגיעה ארוכה על כפתור — עריכה, ימינה / שמאלה, הסרה, הרשימה; וכל שינוי דרך המנהל (בלי לכתוב בעצמו)",
                tr206.contains("buttonMenu(row, it);") && tr206.contains("Manager.onListOrder(") && tr206.contains("Manager.onListRemove(")
                        && tr206.contains("DbVerbsUi.addToList(") && !tr206.contains("Lists.") && !tr206.contains("Db.write("));
        String dbs205 = read(new File(System.getProperty("vfm.src"), "src/com/tikiyot/vfm/masachim/DbScreen.java"));
        check("מבנה 205 (ממצא 55): פותחי המסכים נרשמים אצל המנהל כבר בהפעלה — מיד אחרי פעולות המערכת",
                dbs205.contains("Manager.boot();") && dbs205.indexOf("VerbsUi.boot();") > dbs205.indexOf("Manager.boot();"));
        check("מבנה 203: \"עורך פקודות\" — כניסה קבועה בתפריט של התוכנה, ישר לעורך",
                dls203.contains("menu.add(0, MENU_EDITOR, 0, Words.DbCmd.MENU_EDITOR)") && dls203.contains("if (id == MENU_EDITOR) LabScreen.edit(this, null);"));
    }

    private static void m041select() {
        File r = new File(home, "כונן-041ב");
        wipe(r);
        File d = new File(r, "ת");
        d.mkdirs();
        try {
            Files.writeAtomic(new File(d, "א.txt"), "אחת");
            Files.writeAtomic(new File(d, "ב.txt"), "שתיים");
        } catch (Exception e) {
            check("מ0.41: הכנה — " + e, false);
        }
        String a = new File(d, "א.txt").getAbsolutePath(), b = new File(d, "ב.txt").getAbsolutePath();
        java.util.List<String> one = java.util.Arrays.asList(a), two = java.util.Arrays.asList(a, b);
        eq("מ0.41: %נבחרים% — שורה לכל קובץ, במרכאות; שאר השורות כמו שהן", "צלמית 🗑\nמחק \"" + a + "\"\nמחק \"" + b + "\"\nהודעה סוף\n",
                Choice.expand("צלמית 🗑\nמחק %נבחרים%\nהודעה סוף\n", two));
        eq("מ0.41: %נבחרים% שכבר במרכאות — בלי מרכאות כפולות", "שנה \"" + a + "\" \"<שם>\"\n", Choice.expand("שנה \"%נבחרים%\" \"<שם>\"", one));
        eq("מ0.41: כמה קבצים — מקור של העתק, העבר, מחק", 2, Choice.arity("העתק %נבחרים% \"%הורדות%\"\n"));
        eq("מ0.41: ...גם לרשימה", 2, Choice.arity("העתק %נבחרים% ידנית \"מועדפים\"\n"));
        eq("מ0.41: ...וגם באנגלית, עם מתג", 2, Choice.arity("DEL \"%נבחרים%\" /תאריך:שבוע\n"));
        eq("מ0.41: קובץ אחד — שנה", 1, Choice.arity("צלמית ✏\nשנה %נבחרים% \"<שם>\"\n"));
        eq("מ0.41: קובץ אחד — %נבחרים% כיעד", 1, Choice.arity("העתק \"%הורדות%\\א.txt\" %נבחרים%\n"));
        eq("מ0.41: קובץ אחד — פקודת פתיחה", 1, Choice.arity("פתח תיקייה %נבחרים%\n"));
        eq("מ0.41: שורה אחת מקבלת כמה ואחרת אחד — אחד", 1, Choice.arity("מחק %נבחרים%\nהפעל %נבחרים%\n"));
        eq("מ0.41: בלי %נבחרים% (גם בהערה) — 0", 0, Choice.arity("הערה %נבחרים%\nפתח מסך הבית\n"));
        java.util.Map<String, String> v = Parse.vars(d, r.getAbsolutePath(), null);
        Index ix = new Index("041ב", "041ב", r.getAbsolutePath());
        ix.scan(null);
        Library lib = Library.forTests(new ArrayList<Index>(java.util.Collections.singletonList(ix)));
        eq("מ0.41: התכנון קובע — שם קבוע לקובץ אחד: מתאימה", Boolean.TRUE, Choice.planned(Choice.expand("שנה %נבחרים% \"חדש.txt\"", one), d, v, lib));
        eq("מ0.41: ...לשני קבצים — לא (השני נתקל בראשון)", Boolean.FALSE, Choice.planned(Choice.expand("שנה %נבחרים% \"חדש.txt\"", two), d, v, lib));
        eq("מ0.41: מחק — מתאימה לשניים", Boolean.TRUE, Choice.planned(Choice.expand("מחק %נבחרים%", two), d, v, lib));
        eq("מ0.41: מתג שלא שייך — לא מתאימה", Boolean.FALSE, Choice.planned(Choice.expand("מחק %נבחרים% /מסלול", one), d, v, lib));
        check("מ0.41: מקום פתוח — לא מתכננים כאן (נשאל בהרצה)", Choice.planned(Choice.expand("שנה %נבחרים% \"<שם>\"", one), d, v, lib) == null);
        check("מ0.41: שורה של רשימה, או פקודת פתיחה — לא מתכננים כאן", Choice.planned(Choice.expand("העתק %נבחרים% ידנית \"א\"", one), d, v, lib) == null
                && Choice.planned(Choice.expand("פתח תיקייה %נבחרים%", one), d, v, lib) == null);
        check("מ0.41: שום דבר לא השתנה בכונן מהתכנון", new File(a).isFile() && new File(b).isFile() && !new File(d, "חדש.txt").exists());
        Batch.Plan direct = Batch.plan("מחק %נבחרים%\n", d, v, lib);
        check("מ0.41: %נבחרים% בהרצה רגילה (לא מבחירה) — שגיאה, לא ריצה בשקט", direct.errors == 1);
        // ההרצה עצמה: הטקסט שהתקבל — קובץ פקודות רגיל
        Batch.Plan p = Batch.plan(Choice.expand("העתק %נבחרים% \"" + new File(r, "יעד").getAbsolutePath() + "\"\n", two), d, v, lib);
        new File(r, "יעד").mkdirs();
        p = Batch.plan(Choice.expand("העתק %נבחרים% \"" + new File(r, "יעד").getAbsolutePath() + "\"\n", two), d, v, lib);
        Batch.run(p, lib, null, null);
        check("מ0.41: פקודת בחירה שרצה — שני הקבצים הועתקו", p.errors == 0 && new File(r, "יעד/א.txt").isFile() && new File(r, "יעד/ב.txt").isFile());
    }

    /**
     * מבנה 244 (המשתמש, 17.11.2026: "מקום מרוכז… כמו לוח עם כפתורים ובוררים" · "הכל עובר דרך שכבת הניהול"): הלוח בא עם התוכנה,
     * זהה לעמוד של אותה גרסה שב־lab-pages; כל שינוי — רק בשורה של "עצב" דרך המנהל (shape), ומה שנקבע — מהתוכנה (design); לא
     * מריץ ולא כותב בדרך אחרת. והגשר לדוגמה שבעמוד (לבדיקת העמוד בדפדפן) — באותן מילים כמו בתוכנה.
     */
    private static void m244panel() {
        try {
            File src = new File(System.getProperty("vfm.src")), pages = new File(src.getParentFile(), "lab-pages");
            File asset = new File(src, "assets/" + Lab.BUNDLED + "/itzuv.html");
            String html = Files.readUtf8(asset);
            String[] nv = Lab.nameOf(html, asset.getName());
            File same = null;
            File[] k = pages.listFiles();
            if (k != null) for (File f : k) {
                if (!Lab.isHtml(f.getName())) continue;
                String[] x = Lab.nameOf(Files.readUtf8(f), f.getName());
                if (x[0].equals(nv[0]) && x[1].equals(nv[1])) same = f;
            }
            check("מבנה 244: הלוח בא עם התוכנה (" + nv[0] + " " + nv[1] + "), וזהה לעמוד של אותה גרסה שב־lab-pages",
                    nv[0].equals("עיצוב") && same != null && Files.readUtf8(same).equals(html));
            check("מבנה 244: הלוח משנה רק דרך \"עצב\" (V.shape) וקורא דרך V.design — לא מריץ קובץ, לא כותב ולא שומר בדרך אחרת",
                    html.contains("V.shape(") && html.contains("V.design(") && !html.contains("V.runFile(") && !html.contains("V.runCommands(")
                            && !html.contains("V.writeCommandFile(") && !html.contains("V.saveCommandFile(") && !html.contains("V.saveText(")
                            && !html.contains("V.deleteCommandFile("));
            List<String> miss = new ArrayList<String>();
            for (String sw : new String[]{Verbs.SW_DESIGN, Verbs.SW_COLS, Verbs.SW_GAP, Verbs.SW_ICON_SIZE, Verbs.SW_T_TITLE, Verbs.SW_T_NAME,
                    Verbs.SW_T_DATA, Verbs.SW_T_GROUP, Verbs.SW_T_TAG, Verbs.SW_MODE_LOOK, Verbs.SW_COLOR, Verbs.SW_WORDING})
                if (!html.contains("'" + sw + "'")) miss.add(sw);
            for (String[] r : Palette.ROLES) if (!html.contains("[\"" + r[0] + "\",\"" + r[1] + "\",\"" + r[2] + "\"]")) miss.add(r[0]);
            for (String d : new String[]{Design.DEFAULT, Design.FILES, Design.WORDS}) if (!html.contains("'" + d + "':{design:'" + d + "'")) miss.add(d);
            if (!html.contains("styleCols:{'" + Design.GRID + "':" + Design.usualCols(true) + ",'" + Design.ROWS + "':" + Design.usualCols(false) + "}"))
                miss.add("styleCols");                                          // מבנה 260: הטורים הרגילים של סגנון — כמו בתוכנה
            for (int n = 0; n < Design.SHAPE_KEYS.length; n++) {                    // מבנה 255: הפריט — אותם מפתחות, מתגים, ערכים ושמות
                String sk = Design.SHAPE_KEYS[n];
                String[] one = Design.choices(sk);
                int[] r = Design.shapeRange(sk);
                String vals = one != null ? "'" + String.join("','", one) + "'" : r[0] + "," + r[1];
                if (!html.contains("['" + sk + "','" + Verbs.shapeSwitch(sk) + "',[" + vals + "],'" + Design.SHAPE_IDS[n] + "']")) miss.add(sk);
            }
            check("מבנה 244: הגשר לדוגמה שבעמוד — באותן מילים, צבעים ועיצובים כמו בתוכנה (" + miss + ")", miss.isEmpty());
            // מבנה 245: הכניסה — "🎨 עיצוב" פותח את הלוח על הרשימה שעל המסך (בבית — על האופן); מההגדרות — על כל התוכנה; החלון הישן —
            // בפריט משלו עד שהלוח יעבוד במכשיר; מסך שנבנה בצבעים אחרים — נבנה מחדש כשחוזרים אליו
            File code = new File(src, "src/com/tikiyot/vfm");
            String dbl = read(new File(code, "masachim/DbListScreen.java")), set = read(new File(code, "masachim/SettingsScreen.java")),
                    ls = read(new File(code, "masachim/ListScreen.java")), lab = read(new File(code, "masachim/LabScreen.java")),
                    br = read(new File(code, "lev/Bridge.java"));
            check("מבנה 245: \"🎨 עיצוב\" — הלוח (על הרשימה, בדרך שהפקודה מוצאת; בבית — על האופן); מההגדרות — על כל התוכנה; החלון הישן — ירד (מבנה 266); "
                            + "הלוח מתחיל במה שהמסך ביקש; מסך שהצבעים שלו השתנו — נבנה מחדש",
                    Words.DbCmd.TOOL_DESIGN.equals(nv[0]) && dbl.contains("MENU_DESIGN) panel();") && !dbl.contains("MENU_DESIGN_OLD") && !set.contains("DesignW.MANAGE")
                            && dbl.contains("LabScreen.design(this, Verbs.LIST, Names.pathText(path))") && dbl.contains("DbPaths.find(store, mode, path) != listId")
                            && dbl.contains("LabScreen.design(this, Verbs.MODE_WORD, mode)")
                            && set.contains("LabScreen.design(SettingsScreen.this, Verbs.PROGRAM_WORD, \"\")")
                            && lab.contains("putExtra(EXTRA, Words.DbCmd.TOOL_DESIGN)") && br.contains("host.startTarget()") && html.contains("load('','')")
                            && ls.contains("paint = Ui.begin(this, false);") && ls.contains("if (!paint.equals(Ui.colors(this)))") && ls.contains("recreate();"));
            // מבנה 270: הצבעים של מסך — של הרשימה שעליו; ומסך שחוזר לפנים — הצבעים שלו חוזרים לשימוש, במקום אחד
            String ui270 = read(new File(code, "masachim/rechivim/Ui.java"));
            check("מבנה 270: מסך של רשימה אומר מה המראות שלה (מהכלל שב־Palette), הצבעים נכנסים לשימוש לפי המסך, וכל מסך שחוזר לפנים מחזיר את שלו",
                    dbl.contains("implements Ui.Colors") && dbl.contains("Palette.looksOf(getIntent().getStringExtra(EXTRA_MODE), getIntent().getLongExtra(EXTRA_LIST, 0))")
                            && ui270.contains("a instanceof Colors ? ((Colors) a).looks() : Palette.looks()")
                            && ui270.contains("registerActivityLifecycleCallbacks") && ui270.contains("public void onActivityResumed(Activity x) { colors(x); }")
                            && !ui270.contains("Palette.version"));
        } catch (Exception e) {
            check("מבנה 244: " + e, false);
        }
    }

    /** מ0.41: העורך האינטראקטיבי בא עם התוכנה — העמוד שבהתקנה הוא הגרסה האחרונה שב־lab-pages, והכפתור פותח אותו. */
    private static void m041bundled() {
        try {
            File src = new File(System.getProperty("vfm.src")), pages = new File(src.getParentFile(), "lab-pages");
            File asset = new File(src, "assets/" + Lab.BUNDLED + "/orech-interaktivi.html");
            String html = Files.readUtf8(asset);
            String[] nv = Lab.nameOf(html, asset.getName());
            eq("מ0.41: העמוד שבהתקנה — התוסף שהכפתור פותח", Words.DbCmd.TOOL_EDITOR, nv[0]);
            File same = null;                                                   // עותק העבודה יכול להיות כבר בגרסה הבאה — משווים לאותה גרסה
            File[] k = pages.listFiles();
            if (k != null) for (File f : k) {
                if (!Lab.isHtml(f.getName())) continue;
                String[] x = Lab.nameOf(Files.readUtf8(f), f.getName());
                if (x[0].equals(nv[0]) && x[1].equals(nv[1])) same = f;
            }
            check("מ0.41: ...והוא זהה לעמוד של אותה גרסה שב־lab-pages (" + nv[1] + ")", same != null && Files.readUtf8(same).equals(html));
            File root = new File(home, "תוספים-041");
            wipe(root);
            File first = Lab.addBundled(root, html, asset.getName());
            check("מ0.41: תוסף מההתקנה נכנס לתיקיית התוספים, בשם של תוסף וגרסה", first != null && first.isFile()
                    && first.getName().equals("תוסף " + nv[0] + " " + nv[1] + ".html") && Lab.newest(root, nv[0]) != null && Lab.newest(root, nv[0]).ver.equals(nv[1]));
            check("מ0.41: ...פעם שנייה — כבר יש אותה גרסה, לא נכתב שוב", Lab.addBundled(root, html, asset.getName()) == null && root.listFiles().length == 1);
            check("מ0.41: העמוד מכיר את הטופס של רשימה חכמה (smartForm), ורק כשהתוכנה מכירה", html.contains("can('smartForm')") && html.contains("ask('smartForm'"));
            // מבנה 68 (פתוח 6): העורך 14 מכיר "/מגיבוי" ו"ודא רציף" — ורק כשהתוכנה מצהירה שהיא מכירה (info.features)
            check("מבנה 68: העורך מכיר את המתג /מגיבוי ואת \"ודא רציף\", ומציע אותם רק לתוכנה שמכירה", html.contains("flag('/מגיבוי'") && html.contains("'ודא רציף':{w:'ודא רציף'")
                    && html.contains("NEW_N4=['מגיבוי'],NEW_N4_CMDS=['ודא רציף']") && html.contains("indexOf(' steady ')")
                    && read(new File(src, "src/com/tikiyot/vfm/lev/Bridge.java")).contains("\"steady "));
            check("מבנה 68: העזרה של \"בטל אופן\" בעורך — כמו בתוכנה (הגיבויים לסל)", html.contains("הגיבויים שלו ותיקיית ההגדרות שלו — לסל")
                    && Words.DbCmd.W_DROP_MODE.contains("הגיבויים שלו ותיקיית ההגדרות שלו — לסל"));
            // מ0.45: העורך מכיר כל פקודת קבצים שבהגדרה (בשם העברי) — פקודה חדשה בלי העורך: הבדיקה נכשלת
            List<String> unknown = new ArrayList<String>();
            for (Commands.Def d : Commands.ALL)
                if (d.family == Commands.Family.FILE && !html.contains("'" + Commands.word(d) + "':{")) unknown.add(Commands.word(d));
            check("מ0.45: העורך מכיר את כל פקודות הקבצים (" + unknown + ")", unknown.isEmpty());
            check("מ0.45: ...ואת הפרטים החדשים: רענן מלא /קוראים:, ודא … בסל, מדידה /עד:", html.contains("valsw('/קוראים:'") && html.contains("valsw('/בסל:'")
                    && html.contains("'עד זמן לשורה'") && html.contains("can('commandMatrix')"));
            check("מ0.45+: העורך הישן נמחק — החדש נפתח עם הקובץ (startFile), ויש בו הפקודות הנפוצות ושמירה כנפוצה",
                    html.contains("V.startFile()") && html.contains("V.templates()") && html.contains("V.saveTemplate(")
                            && !new File(System.getProperty("vfm.src"), "src/com/tikiyot/vfm/BatchEditScreen.java").exists());
            // מבנה 261 (המשתמש, 18.11.2026: "תזכור להכניס הכל לגרסה ולתוסף העורך"): העורך מכיר "עצב" ו"ודא עיצוב". המתגים, הערכים
            // והטווחים — מהתוכנה (design().info), ולא כתובים בקוד של העמוד ("כל דבר נקבע במקום אחד"); ורק לתוכנה שאומרת שהיא מכירה
            // (info.features: design). הגשר לדוגמה שבעמוד (לבדיקת העמוד) — באותן מילים כמו בתוכנה.
            int demoAt = html.indexOf("function demoBridge(");
            String page = html.substring(0, demoAt), demo = html.substring(demoAt);
            String verify = Words.DbCmd.N_VERIFY + " " + Syntax.W_SHAPE;
            check("מבנה 261: העורך מכיר \"" + Verbs.SHAPE + "\" ו\"" + verify + "\", באותו הסבר כמו בתוכנה; הפרטים — ממה שהתוכנה מוסרת, ורק כשהיא אומרת שהיא מכירה",
                    page.contains("'" + Verbs.SHAPE + "':{w:'" + Verbs.SHAPE + "',form:'shape',what:'" + Words.Def.W_SHAPE + "'")
                            && page.contains("'" + verify + "':{w:'" + verify + "',en:'" + Syntax.VERIFY + " " + Syntax.W_SHAPE + "',form:'shape'")
                            && page.contains("NEW_SHAPE_CMDS=['" + Verbs.SHAPE + "','" + verify + "']") && page.contains("V.design('','')")
                            && page.contains("indexOf(' design ')") && page.contains("Mo.setDesign(")
                            && read(new File(src, "src/com/tikiyot/vfm/lev/Bridge.java")).contains(" design "));
            List<String> written = new ArrayList<String>(), miss = new ArrayList<String>();
            List<String> sws = new ArrayList<String>(java.util.Arrays.asList(Verbs.SW_DESIGN, Verbs.SW_COLS, Verbs.SW_GAP, Verbs.SW_ICON_SIZE, Verbs.SW_T_TITLE,
                    Verbs.SW_T_NAME, Verbs.SW_T_DATA, Verbs.SW_T_GROUP, Verbs.SW_T_TAG, Verbs.SW_MODE_LOOK, Verbs.SW_COLOR, Verbs.SW_WORDING));
            for (String sk : Design.SHAPE_KEYS) sws.add(Verbs.shapeSwitch(sk));
            for (String sw : sws) {
                if (page.contains(sw)) written.add(sw);
                if (!demo.contains("'" + sw + "'")) miss.add(sw);
            }
            check("מבנה 261: אף מתג של \"עצב\" אינו כתוב בקוד של העורך (" + written + ") — רק בגשר לדוגמה", written.isEmpty());
            for (String[] r : Palette.ROLES) if (!demo.contains("[\"" + r[0] + "\",\"" + r[1] + "\",\"" + r[2] + "\"]")) miss.add(r[0]);
            for (String d : new String[]{Design.DEFAULT, Design.FILES, Design.WORDS}) if (!demo.contains("'" + d + "'")) miss.add(d);
            for (String kind : Design.tagKinds()) if (!demo.contains("'" + kind + "'")) miss.add(kind);
            for (int n = 0; n < Design.SHAPE_KEYS.length; n++) {
                String sk = Design.SHAPE_KEYS[n];
                String[] one = Design.choices(sk);
                int[] r = Design.shapeRange(sk);
                String vals = one != null ? "'" + String.join("','", one) + "'" : r[0] + "," + r[1];
                if (!demo.contains("['" + sk + "','" + Verbs.shapeSwitch(sk) + "',[" + vals + "],'" + Design.SHAPE_IDS[n] + "']")) miss.add(sk);
            }
            for (String x : new String[]{"verb:'" + Verbs.SHAPE + "',program:'" + Verbs.PROGRAM_WORD + "'",
                    "ranges:{cols:[" + Design.COLS[0] + "," + Design.COLS[1] + "],gap:[" + Design.GAP[0] + "," + Design.GAP[1] + "],icon:[" + Design.ICON[0] + ","
                            + Design.ICON[1] + "]}",
                    "texts:['" + Design.T_TITLE + "','" + Design.T_NAME + "','" + Design.T_DATA + "','" + Design.T_GROUP + "','" + Design.T_TAG + "']",
                    "weights:['" + Design.W_LIGHT + "','" + Design.W_REG + "','" + Design.W_MED + "','" + Design.W_BOLD + "']",
                    "keys:{light:'" + Palette.LIGHT + "',dark:'" + Palette.DARK + "',system:'" + Design.SYSTEM + "'}"})
                if (!demo.contains(x)) miss.add(x);
            check("מבנה 261: הגשר לדוגמה שבעורך — באותן מילים, ערכים וטווחים כמו בתוכנה (" + miss + ")", miss.isEmpty());
            // מה שהעורך כותב (השורות שבבדיקות העמוד) — המנוע קורא, ומבין אותו דבר
            String js = read(new File(pages, "orech-bdika.js"));
            String w1 = "עצב תוכנה /טורים:\"\" /סגנון:רשת /כתב-שם:16,מודגש,\"דוד.ttf\" /תווית:\"פקודה\"=\"סקריפט\" /צבע:\"טקסט\"=#112233",
                    w2 = "עצב \"עבודה\\לקוחות\\פעילים\" /עיצוב:\"" + Design.FILES + "\" /סגנון:רשת /סביב-הצלמית:\"ריבוע צבעוני\" /שורות-לשם:2";
            Verbs.Cmd c1 = Verbs.parse(w1), c2 = Verbs.parse(w2);
            String L = Verbs.E_LOOK;
            check("מבנה 261: שורה שהעורך כותב — המנוע קורא אותה: כל התוכנה, \"\" לטורים, סגנון, כתב (גודל, משקל, גופן), נוסח וצבע",
                    js.contains("'" + w1 + "'") && c1.error == null && Verbs.K_PROGRAM.equals(c1.kind) && c1.edit != null && c1.edit.size() == 7
                            && "".equals(c1.edit.get(L + Design.K_COLS)) && Design.GRID.equals(c1.edit.get(L + Design.K_STYLE))
                            && "16".equals(c1.edit.get(L + Design.T_NAME + ": " + Design.K_SIZE)) && Design.W_BOLD.equals(c1.edit.get(L + Design.T_NAME + ": " + Design.K_WEIGHT))
                            && "דוד.ttf".equals(c1.edit.get(L + Design.T_NAME + ": " + Design.K_FONT))
                            && "סקריפט".equals(c1.edit.get(L + Design.K_WORDING + ": פקודה")) && "#112233".equals(c1.edit.get(L + Palette.K_COLOR + ": טקסט")));
            check("מבנה 261: ...ורשימה: הדרך מתחילה באופן; עיצוב מוכן, ערך בשתי מילים, ומספר",
                    js.contains("'" + w2.replace("\\", "\\\\") + "'") && c2.error == null && Verbs.K_LIST.equals(c2.kind) && "עבודה".equals(c2.cond)
                            && "לקוחות\\פעילים".equals(c2.arg.replace('/', '\\')) && Design.FILES.equals(c2.edit.get(Store.P_DESIGN))
                            && Design.FILL.equals(c2.edit.get(L + Design.K_AROUND)) && "2".equals(c2.edit.get(L + Design.K_LINES)));
        } catch (Exception e) {
            check("מ0.41: התוסף שבהתקנה — " + e, false);
        }
    }

    /** מ0.41: קובץ הדוגמה לבדיקת המכשיר — כותב שתי פקודות לרשימת "בחירה"; מה שנכתב נקרא נכון ורץ על קבצים שנבחרו. */
    private static void m041example() {
        File r = new File(home, "כונן-041ג");
        wipe(r);
        new File(r, Paths.DB_DIR).mkdirs();
        new File(r, "Download").mkdirs();
        try {
            String text = Files.readUtf8(new File(System.getProperty("vfm.src"), "../lab-pages/דוגמה לבחירה.vbat"));
            java.util.Map<String, String> v = Parse.vars(r, r.getAbsolutePath(), null);
            Index ix = new Index("041ג", "041ג", r.getAbsolutePath());
            ix.scan(null);
            Library lib = Library.forTests(new ArrayList<Index>(java.util.Collections.singletonList(ix)));
            Batch.Plan p = Batch.plan(text, r, v, lib);
            int bad = 0;
            for (Batch.Step x : p.steps) if (x.error != null && !x.text.contains("ידנית") && !x.text.startsWith("אופן")) bad++;
            eq("מ0.41: קובץ הדוגמה — שורות הכונן בלי שגיאה", 0, bad);
            Batch.Plan fs = Batch.plan(text.substring(0, text.indexOf("אופן \"")), r, v, lib);   // במחשב אין מסד — רק שורות הכונן רצות
            Batch.run(fs, lib, null, null);
            File dir = new File(r, Paths.DB_DIR + "/פקודות לדוגמה"), copy = new File(dir, "העתק להורדות.vbat"), start = new File(dir, "הפעל.vbat");
            check("מ0.41: ...שני קובצי פקודות נכתבו, עם צלמית ושם", copy.isFile() && start.isFile() && "📥".equals(Program.read(copy).icon)
                    && "הפעל".equals(Program.read(start).name));
            String ct = Files.readUtf8(copy), st = Files.readUtf8(start);
            check("מ0.41: ...%נבחרים% נשאר כמו שהוא, ו־%הורדות% הוחלף במסלול", ct.contains("%נבחרים%") && !ct.contains("%הורדות%") && ct.contains("/Download"));
            eq("מ0.41: \"העתק להורדות\" — לכמה קבצים", 2, Choice.arity(ct));
            eq("מ0.41: \"הפעל\" — לקובץ אחד", 1, Choice.arity(st));
            File a = new File(r, "א.txt"), b = new File(r, "ב.txt");
            Files.writeAtomic(a, "אחת");
            Files.writeAtomic(b, "שתיים");
            java.util.List<String> two = java.util.Arrays.asList(a.getAbsolutePath(), b.getAbsolutePath());
            eq("מ0.41: \"העתק להורדות\" על שני קבצים — התכנון עובר", Boolean.TRUE, Choice.planned(Choice.expand(ct, two), dir, v, lib));
            Batch.Plan run = Batch.plan(Choice.expand(ct, two), dir, v, lib);
            Batch.run(run, lib, null, null);
            check("מ0.41: ...והקבצים הועתקו להורדות", run.errors == 0 && new File(r, "Download/א.txt").isFile() && new File(r, "Download/ב.txt").isFile() && a.isFile());
            eq("מ0.41: \"הפעל\" על קובץ אחד — התכנון עובר", Boolean.TRUE, Choice.planned(Choice.expand(st, two.subList(0, 1)), dir, v, lib));
        } catch (Exception e) {
            check("מ0.41: קובץ הדוגמה — " + e, false);
        }
    }

    private static String toolLine(String name) {
        for (String[] t : Reserved.dbTools()) if (t[1].equals(name)) return t[2];
        return "";
    }

    private static void m037() {
        File root = new File(home, "כונן-037");
        wipe(root);
        File here = new File(root, "כאן"), work = new File(root, "עבודה");
        here.mkdirs();
        work.mkdirs();
        java.util.Map<String, String> vars = Parse.vars(here, root.getAbsolutePath(), null);
        check("מ0.37: המשתנים %שעה% ו־%ייחודי% קיימים", vars.get(Syntax.V_TIME) != null && vars.get(Syntax.V_UNIQUE) != null);
        String w = work.getAbsolutePath();
        String text = "המשך ()\n"
                + "דוח \"" + w + "/יומן.txt\"\n"
                + "כתוב \"" + w + "/א.txt\" \"שלום עולם\"\n"
                + "כתוב \"" + w + "/א.txt\" \" ועוד\" /הוסף\n"
                + "כתוב \"" + w + "/ריק.txt\"\n"
                + "ודא \"" + w + "/א.txt\" מכיל \"עולם ועוד\"\n"
                + "ודא \"" + w + "\" קיים\n"
                + "ודא \"" + w + "\" /קבצים:2\n"
                + "ודא \"" + w + "/אין\" חסר\n"
                + "ודא שגיאה\n"
                + "COPY \"" + w + "/אין-כזה.txt\" \"" + w + "\"\n"
                + "ודא שגיאה\n"
                + "כתוב \"" + w + "\" \"x\"\n"
                + "המתן 0\n";
        eq("מ0.37: קובץ ש\"כתוב\" ייצור נראה לשורות הבאות (COPY ממנו — בלי שגיאה)", 0,
                Batch.plan(text + "COPY \"" + w + "/א.txt\" \"" + here.getAbsolutePath() + "\"\n", here, vars).errors);
        wipe(work);
        work.mkdirs();
        Batch.Plan p = Batch.plan(text, here, vars);
        eq("מ0.37: \"ודא שגיאה\" — שורה שנכשלת בתכנון אינה שגיאה", 0, p.errors);
        String rep = Batch.run(p, null, null, null).toString();
        int ok = 0, bad = 0;
        for (Batch.Step s : p.steps) if (s.verified != null) { if (s.verified) ok++; else bad++; }
        eq("מ0.37: כל ה\"ודא\" עברו (4 רגילים + 2 \"שגיאה\")", 6, ok);
        eq("מ0.37: ואף אחד לא נכשל", 0, bad);
        String body = "";
        try { body = Files.readUtf8(new File(work, "א.txt")); } catch (Exception e) { body = String.valueOf(e); }
        eq("מ0.37: כתוב ואחריו /הוסף", "שלום עולם ועוד", body);
        check("מ0.37: קובץ ריק נוצר", new File(work, "ריק.txt").isFile() && new File(work, "ריק.txt").length() == 0);
        String log = "";
        try { log = Files.readUtf8(new File(work, "יומן.txt")); } catch (Exception e) { log = ""; }
        check("מ0.37: \"דוח\" — היומן נכתב לקובץ, עם סיכום ה\"ודא\"", log.contains("6 עברו") && rep.contains("יומן.txt"));
        Batch.Plan q = Batch.plan("ודא שגיאה\nהמתן 0\n", here, vars);
        Batch.run(q, null, null, null);
        check("מ0.37: \"ודא שגיאה\" על שורה שהצליחה — נכשל", q.steps.get(1).verified != null && !q.steps.get(1).verified);
        eq("מ0.37: \"ודא שגיאה\" בלי שורה אחריה — שגיאה", 1, Batch.plan("ודא שגיאה\n", here, vars).errors);
        eq("מ0.37: המתן בלי מספר — שגיאה", 1, Batch.plan("המתן הרבה\n", here, vars).errors);
        Verbs.Cmd n = Verbs.parse("צור רעיון \"לקנות חלב\" \"פנקס/קניות\""), ti = Verbs.parse("צור כותרת \"פרק א\"");
        check("מ0.37: צור רעיון — הטקסט והרשימה", n.error == null && n.kind.equals(Verbs.K_NOTE) && n.arg.equals("לקנות חלב") && n.cond.equals("פנקס/קניות"));
        check("מ0.37: צור כותרת — בלי רשימה (הרשימה שעל המסך)", ti.error == null && ti.kind.equals(Verbs.K_TITLE) && ti.arg.equals("פרק א") && ti.cond.isEmpty());
        Verbs.Cmd sc = Verbs.parse("פתח מסך אבחון"), sb = Verbs.parse("פתח מסך גיבויים");
        check("מ0.37: פתח מסך אבחון / גיבויים", sc.error == null && sc.arg.equals(Verbs.S_DIAG) && sb.error == null && sb.arg.equals(Verbs.S_BACKUPS));
    }

    private static void m032() {
        File root = new File(home, "כונן-032");
        wipe(root);
        File here = new File(root, "כאן"), x = new File(root, "X"), sub = new File(x, "sub");
        here.mkdirs();
        sub.mkdirs();
        java.util.Map<String, String> vars = Parse.vars(here, root.getAbsolutePath(), null);
        Batch.Plan p = Batch.plan("RD \"" + sub + "\"\nRD \"" + x + "\"", here, vars);
        eq("מ0.32 (44): RD לתת־תיקייה ואחריו RD לתיקייה — בלי \"לא ריקה\"", 0, p.errors);
        Batch.Plan q = Batch.plan("MD \"" + x + "/חדשה\"\nRD \"" + x + "/sub\"\nRD \"" + x + "\"", here, vars);
        eq("מ0.32 (44): תת־תיקייה ששורה קודמת יוצרת — התיקייה לא ריקה", 1, q.errors);
    }

    private static void m030() {
        File root = new File(home, "כונן-030");
        wipe(root);
        File here = new File(root, "כאן");
        here.mkdirs();
        File src = new File(root, "מקור"), work = new File(root, "עבודה");
        mk(src, "א.txt", 10);
        mk(work, "ישן.txt", 10);
        java.util.Map<String, String> vars = Parse.vars(here, root.getAbsolutePath(), null);
        Batch.Plan p = Batch.plan("RD \"" + work + "\" /S\nMD \"" + work + "\"\nCOPY \"" + src + "/א.txt\" \"" + work + "\"\nECHO כאן: %כאן%", here, vars);
        eq("מ0.30 (35): RD /S ואחריו MD לאותה תיקייה ו־COPY אליה — בלי שגיאות", 0, p.errors);
        Batch.Plan bad = Batch.plan("MD \"" + work + "/חדשה\"\nCOPY \"" + root + "/אין-כזה.txt\" \"" + work + "\"", here, vars);
        check("מ0.30 (47): שגיאה בקובץ — חסום, ושום שורה לא רצה", bad.errors == 1 && bad.blocked()
                && Batch.run(bad, null, null, null).size() == 2 && !new File(work, "חדשה").exists());
        String miss = "COPY \"" + root + "/אין-כזה.txt\" \"" + work + "\"\n";
        Batch.Plan cont = Batch.plan("המשך\n" + miss, here, vars), cont2 = Batch.plan("המשך\n" + miss + miss, here, vars);
        Batch.Plan cont3 = Batch.plan("המשך (3)\n" + miss + miss + miss, here, vars), contAll = Batch.plan("המשך ()\n" + miss + miss + miss + miss, here, vars);
        check("מ0.31: \"המשך\" — כישלון אחד מותר, שניים — חסום", !cont.blocked() && cont2.blocked());
        check("מ0.31: \"המשך (3)\" — שלושה; \"המשך ()\" — בלי גבול", !cont3.blocked() && cont3.allowErrors == 3 && !contAll.blocked());
        eq("מ0.31: צורה לא מובנת", -1, Parse.allowOf("המשך הרבה"));
        Batch.Plan stop = Batch.plan("המשך\n" + miss + "ECHO סוף\n", here, vars);
        check("מ0.31: כישלון אחד עם \"המשך\" — ההרצה לא נעצרת", !Batch.run(stop, null, null, null).toString().contains("⛔"));
        check("מ0.30 (46): ECHO מחליף משתנים", p.echoes.size() == 1 && p.echoes.get(0).contains(here.getAbsolutePath()));
        MemStore s = new MemStore(IdCounter.memory());
        long h = DbPaths.ensureHome(s), a = s.createList("א", h), b = s.createList("ב", h), in = s.createList("פנימית", a);
        ListCmd.Side side = new ListCmd.Side();
        side.path = java.util.Collections.singletonList("פנימית");
        eq("מ0.30 (40): רשימה שאינה בבית — יוצאת מהרשימה שהיא בה", a, ListOps.parentOf(s, "x", side, in));
        s.addMember(b, in);
        eq("מ0.30 (40): בכמה רשימות ובלי דרך — צריך דרך מלאה", -1L, ListOps.parentOf(s, "x", side, in));
        s.addMember(h, in);
        eq("מ0.30 (40): גם בבית — מהבית", h, ListOps.parentOf(s, "x", side, in));
        Lists.deleteWhole(s, a);
        check("מ0.30 (מ20): מחיקת רשימה מכל מקום — נמחקה, ומה שנשאר במקום אחר נשאר", s.item(a) == null && s.item(in) != null);
    }

    private static void batchRun() throws Exception {
        // קריאת שורה, כמו cmd
        eq("117א: מרכאות שומרות רווחים", "[MOVE, %הורדות%\\ספר חדש.pdf, יעד, /Y]",
                Parse.tokens("MOVE \"%הורדות%\\ספר חדש.pdf\" יעד /Y").toString());
        check("117א: מתג מוכר", Parse.isSwitch("/Y") && Parse.isSwitch("/-y") && Parse.isSwitch("/תאריך:יום") && Parse.isSwitch("/MAXAGE:3"));
        check("117א: מסלול אינו מתג", !Parse.isSwitch("/storage/emulated/0") && !Parse.isSwitch("/ספרים"));
        check("117א: תבנית", Text.wild("*.pdf", "ספר.PDF") && !Text.wild("*.pdf", "ספר.epub") && Text.wild("פרק ?.txt", "פרק 3.txt")
                && Text.wild("*.*", "README"));

        File root = new File(home, "כונן-117");
        wipe(root);
        File dl = new File(root, "Download");
        File here = new File(root, "קיצורי דרך");
        here.mkdirs();
        File a = mk(dl, "ספר א.pdf", 3000);
        File b = mk(dl, "ספר ב.pdf", 20);
        File c = mk(dl, "שיר.mp3", 50);
        File old = mk(dl, "ישן.pdf", 40);
        old.setLastModified(System.currentTimeMillis() - 400L * 24 * 3600 * 1000);   // לפני יותר משנה
        java.util.Map<String, String> vars = Parse.vars(here, root.getAbsolutePath(), null);

        // משתנים ומסלולים
        String[] err = {null};
        eq("117א: משתנה ו-\\", dl.getAbsolutePath() + "/x.pdf", Parse.path("%הורדות%\\x.pdf", here, vars, err));
        eq("117א: מסלול יחסי — ביחס לתיקייה של הקובץ", new File(here, "משנה").getAbsolutePath(), Parse.path("משנה", here, vars, err));
        check("117א: משתנה שאינו מוכר", Parse.path("%כרטיס%\\x", here, vars, err) == null && err[0].contains("כרטיס"));

        // תכנון: MOVE עם סינון, בלי לשנות דבר
        new File(root, "ספרים").mkdirs();
        Batch.Plan p = Batch.plan("@ECHO OFF\nREM הערה\n:: גם הערה\nECHO שלום\n"
                + "MOVE \"%הורדות%\\*.pdf\" \"%פנימי%\\ספרים\" /תאריך:שנה\n"
                + "COPY %הורדות%\\*.* %פנימי%\\ספרים /גודל:>1KB\n"
                + "DEL %הורדות%\\*.pdf /תאריך:<שנה\n"
                + "DEL %הורדות%\\*.mp3 /MINAGE:365\n"
                + "MD %פנימי%\\חדשה\n"
                + "REN %הורדות%\\שיר.mp3 זמר.mp3\n"
                + "START %פנימי%\\ספרים\n"
                + "FORMAT C:\n"
                + "COPY %הורדות%\\*.pdf %פנימי%\\אין-כזו\n"
                + "RD %פנימי%\\ספרים\n", here, vars);
        eq("117א: ההערות לא נכנסות, ECHO כן", "[שלום]", p.echoes.toString());
        Batch.Step mv = p.steps.get(1);
        eq("117א: MOVE — שני PDF מהשנה האחרונה (בלי הישן)", 2, mv.files.size());
        check("117א: MOVE — הסינון נאמר", mv.describe.contains("2 מתוך 3"));
        eq("117א/118: COPY *.* /גודל:>1KB — הגדול כבר הועבר בשורה הקודמת, ולכן אין", 0, p.steps.get(2).files.size());
        eq("117א: DEL /תאריך:<שנה — רק הישן", "[" + old.getAbsolutePath() + "]", p.steps.get(3).files.toString());
        eq("117א: DEL /MINAGE:365 (ROBOCOPY) — mp3 חדש לא נבחר", 0, p.steps.get(4).files.size());
        check("117א: MD מתוכנן", p.steps.get(5).error == null && p.steps.get(5).describe.contains("חדשה"));
        check("117א: REN מתוכנן", p.steps.get(6).error == null && "זמר.mp3".equals(p.steps.get(6).newName));
        eq("117א: START — התיקייה לפתיחה בסוף", new File(root, "ספרים").getAbsolutePath(), p.start.getAbsolutePath());
        check("117א: פקודה לא מוכרת — שגיאה", p.steps.get(8).error != null && p.steps.get(8).error.contains("FORMAT"));
        check("117א: יעד שלא קיים — שגיאה, עם עצה", p.steps.get(9).error != null && p.steps.get(9).error.contains("MD"));
        eq("117א/118: שלוש שגיאות — FORMAT, יעד שלא קיים, ו-RD על \"ספרים\" שהשורות הקודמות מילאו (בלי /S)", 3, p.errors);
        check("117א: התכנון לא שינה דבר", a.isFile() && b.isFile() && c.isFile() && old.isFile() && !new File(root, "חדשה").exists());
        // 186: START על קובץ ועל תבנית — החדש ביותר (קובץ apk — התקנה, במכשיר)
        File dl186 = new File(vars.get(Syntax.V_DOWNLOADS));
        File k1 = mk(dl186, "vfm-1.7.78.apk", 3), k2 = mk(dl186, "vfm-1.7.80.apk", 3), k3 = mk(dl186, "vfm-1.7.79.apk", 3);
        k1.setLastModified(System.currentTimeMillis() - 30000);
        k3.setLastModified(System.currentTimeMillis() - 20000);
        k2.setLastModified(System.currentTimeMillis() - 10000);
        Batch.Plan st = Batch.plan("START \"%הורדות%\\vfm-*.apk\"\n", here, vars);
        eq("186: START תבנית — החדש ביותר", k2.getAbsolutePath(), st.start == null ? null : st.start.getAbsolutePath());
        check("186: START תבנית — נאמר מבין כמה", st.steps.get(0).describe.contains("מבין 3"));
        check("186: START בלבד — אין שינויים בכונן", st.changes() == 0 && st.errors == 0);
        eq("186: START קובץ", k1.getAbsolutePath(), Batch.plan("START %הורדות%\\vfm-1.7.78.apk\n", here, vars).start.getAbsolutePath());
        Batch.Plan none = Batch.plan("START %הורדות%\\*.xyz\n", here, vars);
        check("186: START תבנית בלי התאמה — שגיאה", none.errors == 1 && none.start == null && none.steps.get(0).error.contains("*.xyz"));
        Batch.Plan made = Batch.plan("COPY %הורדות%\\vfm-1.7.78.apk %פנימי%\\ספרים\nSTART %פנימי%\\ספרים\\*.apk\n", here, vars);
        check("186: START על קובץ שהשורות הקודמות יעתיקו", made.start != null && made.start.getName().equals("vfm-1.7.78.apk"));
        k1.delete(); k2.delete(); k3.delete();
        Batch.Plan bad = Batch.plan("COPY a b /תאריך:מחר", here, vars);
        check("117א: מתג שלא הובן — שגיאה", bad.errors == 1 && bad.steps.get(0).error.contains("/תאריך:מחר"));

        // 203: COPY … %שיתוף% — נאסף לשליחה, בלי שינוי בכונן
        Batch.Plan sh = Batch.plan("COPY %הורדות%\\*.pdf %שיתוף%\nCOPY %פנימי%\\ספרים %שיתוף%\n", here, vars);
        check("203: לשיתוף — קבצים לפי תבנית", sh.errors == 0 && sh.steps.get(0).share && sh.steps.get(0).files.size() >= 2);
        check("203: לשיתוף — תיקייה כמו שהיא (תיארז)", sh.steps.get(1).share && sh.steps.get(1).dirs.size() == 1);
        check("203: לשיתוף — נספר כשינוי (יש מה לאשר)", sh.changes() == 2);
        check("203: MOVE לשיתוף — נדחה", Batch.plan("MOVE %הורדות%\\*.pdf %שיתוף%", here, vars).steps.get(0).error.contains("MOVE"));
        check("203: %שיתוף% במקום אחר — נדחה", Batch.plan("DEL %שיתוף%", here, vars).errors == 1
                && Batch.plan("COPY %שיתוף% %פנימי%\\ספרים", here, vars).errors == 1);
        check("203: מסלול אחרי %שיתוף% — נדחה (בינתיים)", Batch.plan("COPY %הורדות%\\*.pdf %שיתוף%\\ספרים", here, vars).errors == 1);
        check("203: DEL אחרי השיתוף — מתוכנן כרגיל", Batch.plan("COPY %הורדות%\\*.pdf %שיתוף%\nDEL %הורדות%\\*.pdf", here, vars).errors == 0);

        // RD: לא ריקה בלי /S — כמו ב-bat
        mk(new File(root, "מלאה"), "x.txt", 1);
        check("117א: RD על תיקייה לא ריקה בלי /S — נדחה", Batch.plan("RD %פנימי%\\מלאה", here, vars).errors == 1);
        check("117א: RD /S — מתוכנן", Batch.plan("RD %פנימי%\\מלאה /S /Q", here, vars).errors == 0);

        // ביצוע — דרך Physical
        Index ix = new Index("117", "117", root.getAbsolutePath());
        ix.scan(null);
        Library lib = Library.forTests(new ArrayList<Index>(java.util.Collections.singletonList(ix)));
        File tf = new File(home, "מבנה-117.txt");
        tf.delete();
        Batch.Plan go = Batch.plan("MOVE %הורדות%\\*.pdf %פנימי%\\ספרים /תאריך:שנה\n"
                + "DEL %הורדות%\\*.pdf\n"
                + "MD %פנימי%\\חדשה\n"
                + "REN %הורדות%\\שיר.mp3 זמר.mp3\n", here, vars);
        List<String> report = Batch.run(go, lib, null, null);
        lib.flush(5000);
        check("117א: MOVE ביצע — בספרים, ולא בהורדות", new File(root, "ספרים/ספר א.pdf").isFile() && !a.exists() && !b.exists());
        check("117א: DEL — לסל המחזור, לא לצמיתות", !old.exists() && new File(root, Index.TRASH).isDirectory()
                && new File(root, Index.TRASH).list().length >= 1);
        check("117א: MD", new File(root, "חדשה").isDirectory());
        check("117א: REN", new File(dl, "זמר.mp3").isFile() && !c.exists());
        eq("117א: דוח לכל שורה", 4, report.size());

        // /Y — שם תפוס: להחליף, הקודם לסל
        File x1 = mk(dl, "כפול.pdf", 11);
        File x2 = mk(new File(root, "ספרים"), "כפול.pdf", 22);
        lib.fileAdded(x1);
        lib.fileAdded(x2);
        lib.flush(5000);
        Batch.run(Batch.plan("COPY %הורדות%\\כפול.pdf %פנימי%\\ספרים /Y", here, vars), lib, null, null);
        lib.flush(5000);
        eq("117א: /Y — הוחלף", 11L, x2.length());
        // /תפוס:לדלג
        File y1 = mk(dl, "עוד.pdf", 5);
        mk(new File(root, "ספרים"), "עוד.pdf", 6);
        lib.fileAdded(y1);
        lib.flush(5000);
        eq("117א: /תפוס:לדלג — הקובץ התפוס לא נבחר", 0,
                Batch.plan("COPY %הורדות%\\עוד.pdf %פנימי%\\ספרים /תפוס:לדלג", here, vars).steps.get(0).files.size());
        check("117א: התבנית מסבירה ומתחילה ב-@ECHO OFF", Batch.template().startsWith("@ECHO OFF") && Batch.template().contains("REM"));
        wipe(root);

        // התנאי בחיפוש — גם "לפני" ומספר ימים (102)
        check("117א: תאריך \"לפני\" בחיפוש", Cond.parse("תאריך:<שנה").tests.size() == 1 && Cond.parse("תאריך:3ימים").tests.size() == 1
                && Cond.parse("תאריך:<2026-01-01").unknown.isEmpty());
    }

    // ---------- קובצי פקודות: מסלול מהבורר ← משתנה (117א, שלב 2) ----------

    private static void batchForm() {
        java.util.Map<String, String> vars = Parse.vars(new File("/storage/emulated/0/קיצורי דרך"), "/storage/emulated/0", "/storage/AAAA-BBBB");
        eq("שלב 2: מסלול מהבורר — המשתנה הארוך ביותר", "%הורדות%\\ספרים", Parse.toVar("/storage/emulated/0/Download/ספרים", vars));
        eq("שלב 2: פנימי", "%פנימי%\\ספרים\\חדשים", Parse.toVar("/storage/emulated/0/ספרים/חדשים", vars));
        eq("שלב 2: כרטיס", "%כרטיס%", Parse.toVar("/storage/AAAA-BBBB", vars));
        eq("שלב 2: מחוץ למשתנים — כמו שהוא, עם \\", "\\mnt\\x", Parse.toVar("/mnt/x", vars));
    }

    // ---------- תיקוני הביקורות השישית והשביעית ----------

    private static void review7() {
        try {
            review7Run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void review7Run() throws Exception {
        File root = new File(home, "כונן-ב7");
        wipe(root);
        File dl = new File(root, "Download");
        mk(dl, "ישן.pdf", 10);
        mk(dl, "שיר.mp3", 10);
        mk(dl, ".nomedia", 0);
        java.util.Map<String, String> vars = Parse.vars(root, root.getAbsolutePath(), null);

        // 118: שורה שתלויה בשורה שלפניה
        Batch.Plan p = Batch.plan("MD %פנימי%\\חדשה\nMOVE %הורדות%\\*.pdf %פנימי%\\חדשה\nREN %פנימי%\\חדשה\\ישן.pdf חדש.pdf\n"
                + "COPY %פנימי%\\חדשה\\חדש.pdf %הורדות%\nDEL %הורדות%\\חדש.pdf\nSTART %פנימי%\\חדשה\n", root, vars);
        eq("118: MD ← MOVE ← REN ← COPY ← DEL — בלי שגיאות", 0, p.errors);
        eq("118: MOVE לתיקייה שתיווצר — קובץ אחד", 1, p.steps.get(1).files.size());
        eq("118: COPY של השם החדש", new File(root, "חדשה/חדש.pdf").getAbsolutePath(), p.steps.get(3).files.get(0));
        eq("118: DEL של מה שהועתק בשורה הקודמת", 1, p.steps.get(4).files.size());
        check("118: התכנון לא שינה דבר בכונן", !new File(root, "חדשה").exists() && new File(dl, "ישן.pdf").isFile());
        Index ix = new Index("ב7", "ב7", root.getAbsolutePath());
        ix.scan(null);
        Library lib = Library.forTests(new ArrayList<Index>(java.util.Collections.singletonList(ix)));
        File tf = new File(home, "מבנה-ב7.txt");
        tf.delete();
        List<String> rep = Batch.run(p, lib, null, null);
        lib.flush(5000);
        check("118: הביצוע — הכול במקום", new File(root, "חדשה/חדש.pdf").isFile() && !new File(dl, "ישן.pdf").exists()
                && !new File(dl, "חדש.pdf").exists());
        check("118: בלי כשלונות בדוח", !rep.toString().contains("נכשלו") && !rep.toString().contains("דולגה"));

        // 134: קובץ שנוסף אחרי הסריקה — DEL עובד
        File fresh = mk(dl, "הורדה חדשה.pdf", 20);
        List<String> r2 = Batch.run(Batch.plan("DEL %הורדות%\\*.pdf", root, vars), lib, null, null);
        lib.flush(5000);
        check("134: DEL על קובץ שלא היה באינדקס — הועבר לסל", !fresh.exists() && r2.toString().contains("הועברו לסל 1"));

        // 135: *.* בלי מוסתרים; /A:H — עם
        Batch.Plan h = Batch.plan("DEL %הורדות%\\*.*", root, vars);
        check("135: DEL *.* — בלי .nomedia", h.steps.get(0).files.size() == 1 && !h.steps.get(0).files.get(0).endsWith(".nomedia"));
        check("135: DEL *.* /A:H — גם המוסתר", Batch.plan("DEL %הורדות%\\*.* /A:H", root, vars).steps.get(0).files.size() == 2);
        check("135: קובץ מוסתר שנכתב במפורש — נבחר", Batch.plan("DEL %הורדות%\\.nomedia", root, vars).steps.get(0).files.size() == 1);

        // 121: MOVE של תיקייה עם סינון — שגיאה ברורה; והניסוח
        mk(new File(root, "ספרים"), "א.pdf", 5);
        new File(root, "יעד").mkdirs();
        Batch.Plan f = Batch.plan("MOVE %פנימי%\\ספרים %פנימי%\\יעד /תאריך:שבוע\nMOVE %פנימי%\\ספרים %פנימי%\\יעד", root, vars);
        check("121: סינון על תיקייה שלמה — שגיאה", f.steps.get(0).error != null && f.steps.get(0).error.contains("תיקייה שלמה"));
        eq("121: הניסוח של העברת תיקייה", "העברה: תיקייה אחת ← " + new File(root, "יעד").getAbsolutePath(), Bidi.strip(f.steps.get(1).describe));
        // 204: סימני כיוון — רק בתצוגה
        String bl = "COPY %הורדות%\\לשלוח\\*.* %שיתוף%\nREM הערה בעברית עם רווחים\nECHO שלום עולם";
        String bm = Bidi.mark(bl);
        check("204: סימן אחרי המפרידים בשורת פקודה", bm.split("\n")[0].contains("%\u200E\\\u200E") && bm.split("\n")[0].contains("\u200E%\u200Eשיתוף%\u200E"));
        check("204: REM ו־ECHO — בלי סימנים", bm.split("\n")[1].indexOf(Bidi.LRM) < 0 && bm.split("\n")[2].indexOf(Bidi.LRM) < 0);
        eq("204: הסרה מחזירה בדיוק את המקור", bl, Bidi.strip(bm));
        eq("204: סימון פעמיים — כמו פעם אחת", bm, Bidi.mark(bm));
        check("204: התכנון מתעלם מהסימנים", Batch.plan(Bidi.mark("MD %פנימי%\\חדשה204"), root, vars).errors == 0);

        // ניקוי השיתוף (1.9.0): MD לתיקייה שכבר קיימת — בלי שגיאה; קובץ פקודות — רק בסיומת vbat
        File land2 = new File(root, "נחתו190");
        land2.mkdirs();
        Batch.Plan again = Batch.plan("MD \"%כאן%\\לשלוח\"\nMD \"%כאן%\\לשלוח\" /תפוס:לדלג", land2, Parse.vars(land2, root.getAbsolutePath(), null));
        check("MD לתיקייה שתיווצר/קיימת — עם /תפוס:לדלג: בלי שגיאה (מבנה 109)", again.errors == 0 && again.changes() == 1);
        // מבנה 109 (המשתמש): "סירוב, אלא אם כן יש הוראה להמשיך… אם קיימת ספריה זאת המשך לשורה הבאה, אחרת עוצר"
        Batch.Plan stop109 = Batch.plan("MD \"%כאן%\\לשלוח\"\nMD \"%כאן%\\לשלוח\"", land2, Parse.vars(land2, root.getAbsolutePath(), null));
        Batch.Plan ren109 = Batch.plan("MD \"%כאן%\\לשלוח\"\nMD \"%כאן%\\לשלוח\" /תפוס:לשנות-שם", land2, Parse.vars(land2, root.getAbsolutePath(), null));
        Batch.Plan bad109 = Batch.plan("MD \"%כאן%\\לשלוח\" /תפוס:להחליף", land2, Parse.vars(land2, root.getAbsolutePath(), null));
        check("מבנה 109: תיקייה קיימת בלי מתג — שגיאה (\"שם קיים\"), והקובץ אינו רץ; /תפוס:לשנות-שם — \"(2)\"; /תפוס:להחליף — לא קיים לתיקייה",
                stop109.blocked() && stop109.errorText().contains(Words.Cmd.folderTaken("לשלוח")) && ren109.errors == 0 && ren109.changes() == 2
                        && ren109.steps.get(1).dest.getName().equals("לשלוח (2)") && bad109.errors == 1);
        check("1.9.0: קובץ פקודות — רק vbat", Batch.isBatch("א.VBAT") && !Batch.isBatch("סידור — לשלוח.vbat.txt")
                && !Batch.isBatch("הערות.txt"));
        check("1.9.0: /חתימה: — מתג לא מוכר", Batch.plan("COPY %כאן%\\א.pdf %כאן% /חתימה:2:abc", land2, Parse.vars(land2, root.getAbsolutePath(), null)).errors == 1);

        // 139: %~dp0 ו-ECHO.
        String[] err = {null};
        eq("139: %~dp0 — התיקייה של הקובץ", new File(root, "x.pdf").getAbsolutePath(), Parse.path("%~dp0x.pdf", root, vars, err));
        eq("139: %~dp0\\ — גם עם לוכסן", new File(root, "x.pdf").getAbsolutePath(), Parse.path("%~dp0\\x.pdf", root, vars, err));
        Batch.Plan e = Batch.plan("ECHO.\nECHO שלום", root, vars);
        check("139: ECHO. — מוכר", e.errors == 0 && e.echoes.size() == 2);

        // 119: אריזה כשהפלט בתוך התיקייה
        File prog = new File(root, "תוכנה");
        mk(prog, "מבנה.txt", 50);
        File out = new File(prog, ".מערכת/שליחה/תוכנה.zip");
        mk(out.getParentFile(), "ישנה.zip", 999);
        Zip.Result z = Zip.pack(prog, out, null);
        java.util.zip.ZipFile zf = new java.util.zip.ZipFile(out);
        boolean self = false;
        for (java.util.Enumeration<? extends java.util.zip.ZipEntry> en = zf.entries(); en.hasMoreElements(); )
            if (en.nextElement().getName().contains("שליחה")) self = true;
        zf.close();
        check("119: תיקיית הפלט לא נארזת — לא הקובץ הזמני ולא אריזות קודמות", !self && z.files == 1);
        wipe(root);
    }

    // ---------- תיקוני 123, 124, 137 ----------

    private static void review7b() {
        try {
            review7bRun();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void review7bRun() throws Exception {
        // 124: סיומת במקום אחד
        eq("124: stem — שם עם סיומת", "ספר", Text.stem("ספר.pdf"));
        eq("124: stem — \"פרק 1.17\" שלם", "פרק 1.17", Text.stem("פרק 1.17"));
        eq("124: stem — \"ספר.pdf (1)\" שלם", "ספר.pdf (1)", Text.stem("ספר.pdf (1)"));
        eq("124: tail", ".PDF", Text.tail("ספר.PDF"));
        eq("124: השם המוצג — \"פרק 1.17\" לא נחתך", "פרק 1.17", Ui.baseName("פרק 1.17", false));
        eq("124: השם המוצג — \"ספר.pdf\"", "ספר", Ui.baseName("ספר.pdf", false));
        File d = new File(home, "ב124");
        wipe(d);
        mk(d, "פרק 1.17", 1);
        mk(d, "ספר.pdf", 1);
        mk(d, ".nomedia", 1);
        eq("124: שם תפוס — \"פרק 1.17 (2)\"", "פרק 1.17 (2)", DiskRules.freeName(d, "פרק 1.17", false));
        eq("124: שם תפוס — \"ספר (2).pdf\"", "ספר (2).pdf", DiskRules.freeName(d, "ספר.pdf", false));
        eq("124: שם תפוס — קובץ מוסתר", ".nomedia (2)", DiskRules.freeName(d, ".nomedia", false));
        wipe(d);

        // 123: סוג מורכב, וסוגים נוספים בשם משובש
        File root = new File(home, "כונן-123");
        wipe(root);
        mk(root, "גיבוי.tar.gz", 10);
        mk(root, "photo.heic (1)", 10);
        mk(root, "ספר.pdf", 10);
        Index ix = new Index("123", "123", root.getAbsolutePath());
        ix.scan(null);
        Library lib = Library.forTests(new ArrayList<Index>(java.util.Collections.singletonList(ix)));
        eq("123: סוג:tar.gz", 1, lib.search("סוג:tar.gz", false, false).total);
        eq("123: סוג:gz", 1, lib.search("סוג:gz", false, false).total);
        eq("123: סוג:heic בשם משובש", 1, lib.search("סוג:heic", false, false).total);
        eq("123: סוג:pdf לא השתנה", 1, lib.search("סוג:pdf", false, false).total);
        wipe(root);

        // 137: הבורר — תיקייה שאינה באינדקס (חדשה או מוצאת)
        File drive = new File(home, "כונן-137");
        wipe(drive);
        new File(drive, "ספרים").mkdirs();
        Index i2 = new Index("internal", "זיכרון", drive.getAbsolutePath());
        i2.scan(null);
        new File(drive, "WhatsApp/Media").mkdirs();               // אחרי הסריקה — כמו תיקייה שהוצאה או נוצרה מבחוץ
        new File(drive, ".מוסתרת").mkdirs();
        List<IndexSource.Drv> dv = new ArrayList<IndexSource.Drv>();
        dv.add(new IndexSource.Drv("internal", "זיכרון", drive.getAbsolutePath(), i2));
        IndexSource src = new IndexSource(dv);
        java.util.List<String> in = java.util.Arrays.asList("internal");
        eq("137: לפני כניסה — רק מה שבאינדקס", "[ספרים]", src.children(in).toString());
        src.visit(in);
        eq("137: אחרי כניסה — גם מה שבכונן (בלי מוסתרות), ממוין", "[WhatsApp, ספרים]", src.children(in).toString());
        eq("137: ובתוך תיקייה שאינה באינדקס — מהכונן", "[Media]", src.children(java.util.Arrays.asList("internal", "WhatsApp")).toString());
        java.util.LinkedHashMap<ArrayList<String>, String> rts = new java.util.LinkedHashMap<ArrayList<String>, String>();
        rts.put(new ArrayList<String>(in), "זיכרון");
        PickerModel pm = new PickerModel(new IndexSource(dv), rts);
        pm.enter(in);
        eq("137: בבורר — נכנסים לכונן ורואים גם את החדשה", "[WhatsApp, ספרים]", pickLabels(pm.folderRows()).toString());
        check("137: אפשר לבחור אותה", rowOf(pm.folderRows(), "WhatsApp").pickable);
        wipe(drive);
    }

    // ---------- הלוח (142) ושם אופן (129) ----------

    private static void clip() {
        File d = new File(home, "לוח");
        wipe(d);
        File a = new File(d, "ספרים/א.pdf"), b = new File(d, "ספרים/ב.pdf");
        String dir = new File(d, "ספרים/רמחל").getAbsolutePath();
        Clip.set(P(a.getAbsolutePath(), b.getAbsolutePath()), P(dir), false);
        check("142: הלוח לא ריק", !Clip.isEmpty() && !Clip.isCopy());
        eq("142: מה בלוח", "2 קבצים ותיקייה אחת להעברה", Clip.label());
        eq("142: מתחילים לנווט בתיקייה של הראשון", new File(d, "ספרים").getAbsolutePath(), Clip.startDir().getAbsolutePath());
        check("142: אי אפשר להדביק בלי תיקייה", Clip.cannotPasteInto(null) != null);
        check("142: אי אפשר להדביק לתוך התיקייה שמעבירים", Clip.cannotPasteInto(new File(dir)) != null
                && Clip.cannotPasteInto(new File(dir, "משנה")) != null);
        check("142: אי אפשר להדביק בסל", Clip.cannotPasteInto(new File(d, Index.TRASH)) != null);
        check("142: אי אפשר להדביק בתיקיית התוכנה", Clip.cannotPasteInto(Paths.data()) != null);
        Clip.set(P(a.getAbsolutePath()), new ArrayList<String>(), false);
        check("142: העברה לאותה תיקייה — אין טעם", Clip.cannotPasteInto(new File(d, "ספרים")) != null);
        eq("142: תיקייה אחרת — מותר", null, Clip.cannotPasteInto(new File(d, "אחר")));
        Clip.set(P(a.getAbsolutePath()), new ArrayList<String>(), true, "אזהרה");
        eq("142: העתקה לאותה תיקייה — מותר (שם תפוס — שאלה)", null, Clip.cannotPasteInto(new File(d, "ספרים")));
        eq("142: האזהרה (מהכפולים) נשמרת להדבקה", "אזהרה", Clip.warning());
        Clip.clear();
        check("142: ביטול מנקה", Clip.isEmpty());
        check("142: הפעולות — \"ואז הדבקה\"", Actions.MOVE_TO_FOLDER.label.contains("הדבקה") && Actions.COPY_TO_FOLDER.label.contains("הדבקה"));

        // 129: שם אופן — אותו כלל של שם בכונן. מבנה 135: נבדק בדרך שהתוכנה יוצרת בה אופן — "צור אופן" (Modes.create נמחקה)
        File r129 = drive("אופן129");
        Map<String, String> v129 = new java.util.HashMap<String, String>();
        check("129: שם אופן עם \":\" נדחה", String.valueOf(Batch.plan("צור אופן \"בדיקות: מקיפות\"\n", r129, v129).steps.get(0).error).startsWith("שם אופן לא תקין"));
        check("129: שם אופן עם \"?\" נדחה", String.valueOf(Batch.plan("צור אופן \"מה?\"\n", r129, v129).steps.get(0).error).startsWith("שם אופן לא תקין"));
    }

    // ---------- הלוח ברשימות (142), ומהירות הבורר (133) ----------


    // ---------- עדכון האינדקס (107–110, 113) ומוסתרות בבורר (130) ----------

    private static void indexUpdate() {
        try {
            indexUpdateRun();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void indexUpdateRun() throws Exception {
        // 107: דיווח התקדמות — כל 1,024 קבצים
        File root = new File(home, "כונן-107");
        wipe(root);
        java.util.Random rnd = new java.util.Random(5);
        for (int i = 0; i < 300; i++) {
            File d = new File(root, "ת" + i);
            int n = 1 + rnd.nextInt(30);
            for (int k = 0; k < n; k++) mk(d, "ק" + k + ".pdf", 1);
        }
        final int[] calls = {0};
        Index ix = new Index("107", "107", root.getAbsolutePath());
        ix.scan(new Index.Progress() {
            public void onScan(String label, int items) { calls[0]++; }
        });
        check("107: דיווח בערך כל 1,024 קבצים (" + ix.fileCount() + " קבצים, " + calls[0] + " דיווחים)",
                calls[0] >= ix.fileCount() / 1024 - 1 && calls[0] <= ix.fileCount() / 1024 + 1);

        // 108: קובץ שנכתב במקום — בתיקייה שהשתנתה (סנכרון), ובכל מקום (רענון מעמיק)
        File a = new File(root, "ת1/ק0.pdf"), b = new File(root, "ת2/ק0.pdf");
        Thread.sleep(1100);
        java.io.FileOutputStream oa = new java.io.FileOutputStream(a, true); oa.write(new byte[5000]); oa.close();
        java.io.FileOutputStream ob = new java.io.FileOutputStream(b, true); ob.write(new byte[7000]); ob.close();
        mk(new File(root, "ת2"), "חדש.pdf", 3);                            // ת2 השתנתה, ת1 לא
        Index r = ix.copy();
        r.refresh();
        long sa = -1, sb = -1;
        for (Index.Rec x : r.files) {
            if (x.gone) continue;
            if (r.path(x).equals(a.getAbsolutePath())) sa = x.size;
            if (r.path(x).equals(b.getAbsolutePath())) sb = x.size;
        }
        eq("108: קובץ שנכתב במקום בתיקייה שהשתנתה — מתעדכן ברענון", b.length(), sb);
        check("108: בתיקייה שלא השתנתה — הרענון הרגיל לא רואה (" + sa + ")", sa != a.length());
        int deep = r.deepRefresh();
        for (Index.Rec x : r.files) if (!x.gone && r.path(x).equals(a.getAbsolutePath())) sa = x.size;
        check("108: הרענון המעמיק — מתעדכן (" + deep + " שינויים)", sa == a.length() && deep >= 1);

        // 113: רענון בלי שינוי — בלי החלפה
        Library lib = Library.forTests(new ArrayList<Index>(java.util.Collections.singletonList(r)));
        Index before = lib.indexes.get(0);
        int ch = r.copy().refresh();
        eq("113: אין שינוי — 0", 0, ch);
        check("113: התמונה לא הוחלפה", lib.indexes.get(0) == before);

        // 110: רמות — מקובץ ההגדרות, ברירת מחדל: בכל חזרה
        check("110: שלוש רמות, השלישית — בכל חזרה", Library.LEVELS.length == 3 && Library.LEVELS[2].contains("חזרה"));
        wipe(root);

        // 130: מוסתרות בבורר
        Index hx = new Index("internal", "זיכרון", "/storage/emulated/0");
        hx.dirs.add(pdir(-1, "ספרים", false, false, false));
        hx.dirs.add(pdir(-1, ".פרטי", true, false, false));
        hx.dirs.add(pdir(-1, ".נמחקו", true, true, false));
        List<IndexSource.Drv> dv = new ArrayList<IndexSource.Drv>();
        dv.add(new IndexSource.Drv("internal", "זיכרון", "/storage/emulated/0", hx));
        IndexSource src = new IndexSource(dv);
        java.util.List<String> in = java.util.Arrays.asList("internal");
        eq("130: בלי מוסתרות", "[ספרים]", src.children(in).toString());
        src.showHidden(true);
        eq("130: עם מוסתרות — אבל לא הסל", "[.פרטי, ספרים]", src.children(in).toString());
    }

    // ---------- סינון במקום אחד (128), ושמות בחלון האישור (141) ----------

    private static void dateSize() {
        DateSize d = new DateSize();
        check("128: תאריך — לפני שנה", d.take("תאריך:<שנה") && d.dateMode == 2 && d.dateValue.equals("שנה"));
        check("128: גודל — גדול מ־10MB", d.take("גודל:>10MB") && d.sizeMode == 1 && d.sizeNumber.equals("10") && d.sizeUnit.equals("MB"));
        eq("128: חזרה לתחביר החיפוש", "תאריך:<שנה גודל:>10MB", d.dateToken() + " " + d.sizeToken());
        check("128: תאריך שני — לא נקלט (כבר יש)", !d.take("תאריך:יום"));
        DateSize e = new DateSize();
        check("128: גודל בלי סימן — לא נקלט, לא מנחשים", !e.take("גודל:10MB") && e.sizeMode == 0);
        check("128: בתים", e.take("גודל:<500") && e.sizeUnit.equals("B") && e.sizeToken().equals("גודל:<500"));
        DateSize f = new DateSize();
        check("128: \"מאז\" עם >=", f.take("תאריך:>=2026-09-01") && f.dateMode == 1 && f.dateValue.equals("2026-09-01"));
        eq("128: ריק — כלום", "", new DateSize().dateToken() + new DateSize().sizeToken());
        // התנאים של הרשימות החכמות הישנות (מהרשימות הקבועות) — נקלטים
        for (String old : new String[]{"תאריך:יום", "תאריך:שבוע", "תאריך:חודש", "תאריך:שנה", "גודל:>1MB", "גודל:>10MB", "גודל:>100MB", "גודל:<1MB"})
            check("128: תנאי ישן נקלט — " + old, new DateSize().take(old));
        Cond c = Cond.parse("סוג:pdf " + d.dateToken() + " " + d.sizeToken());
        check("128: התנאי מובן לחיפוש", c.unknown.isEmpty() && c.tests.size() == 3);

        // 141: שמות בחלון האישור
        String n = DiskSay.namesOf(P("/x/א.pdf", "/x/ב.pdf"), P("/x/ספרים"), 12);
        check("141: תיקייה קודם, ואחריה הקבצים", n.indexOf("📁 ספרים") < n.indexOf("א.pdf") && n.contains("ב.pdf"));
        List<String> many = new ArrayList<String>();
        for (int i = 0; i < 30; i++) many.add("/x/ק" + i + ".pdf");
        String m = DiskSay.namesOf(many, new ArrayList<String>(), 12);
        check("141: עד 12, ואחר כך \"ועוד 18\"", m.contains("ק11.pdf") && !m.contains("ק12.pdf") && m.contains("ועוד 18"));
    }

    /** מחרוזות בקוד (בלי הערות), כל אחת עם השורה שלה. מנתח קטן — מספיק לקוד שלנו. */
    private static List<String[]> literals(String src) {
        List<String[]> out = new ArrayList<String[]>();
        String[] lines = src.split("\n", -1);
        int i = 0, n = src.length(), line = 0;
        while (i < n) {
            char c = src.charAt(i);
            if (c == '\n') { line++; i++; continue; }
            if (src.startsWith("//", i)) { int j = src.indexOf('\n', i); i = j < 0 ? n : j; continue; }
            if (src.startsWith("/*", i)) {
                int j = src.indexOf("*/", i + 2);
                for (int k = i; k < j; k++) if (src.charAt(k) == '\n') line++;
                i = j + 2;
                continue;
            }
            if (c == '\'' || c == '"') {
                int j = i + 1;
                while (src.charAt(j) != c) j += src.charAt(j) == '\\' ? 2 : 1;
                if (c == '"') out.add(new String[]{src.substring(i + 1, j), lines[line]});
                i = j + 1;
                continue;
            }
            i++;
        }
        return out;
    }

    private static boolean hebrew(String s) {
        for (int i = 0; i < s.length(); i++) if (s.charAt(i) >= 0x0590 && s.charAt(i) <= 0x05FF) return true;
        return false;
    }

    private static void dictionary() {
        // ספירות — מקום אחד
        check("131: קובץ אחד / 3 קבצים", Words.files(1).equals("קובץ אחד") && Words.files(3).equals("3 קבצים"));
        check("131: תיקייה אחת / 2 תיקיות", Words.folders(1).equals("תיקייה אחת") && Words.folders(2).equals("2 תיקיות"));
        check("131: פריט אחד / רשימה אחת", Words.items(1).equals("פריט אחד") && Words.lists(4).equals("4 רשימות"));
        check("131: ועוד", Words.andMore(18).equals("ועוד 18"));
        check("187: שינוי אחד / 3 שינויים (ביומן)", Words.Lib.changes(1).equals("שינוי אחד") && Words.Lib.changes(3).equals("3 שינויים"));
        Batch.Plan p = Batch.plan("RD\n", new File("/x"), Parse.vars(new File("/x"), "/p", null));
        check("131: שגיאת שימוש — מהמילון", p.steps.size() == 1 && Commands.usage("RD").equals(p.steps.get(0).error));
        // התחביר במקום אחד: התבנית והטופס מבינים זה את זה
        String t = Batch.template();
        check("131: התבנית — המשתנים מהתחביר", t.contains(Parse.var(Syntax.V_HERE)) && t.contains(Parse.var(Syntax.V_DOWNLOADS)));
        check("131: התבנית — שם תפוס מהתחביר", t.contains(Syntax.SW_TAKEN + Syntax.TAKEN_SKIP));
        check("131: תאריך וגודל — מ-DateSize", Syntax.SW_DATE.equals(DateSize.DATE) && Syntax.SW_SIZE.equals(DateSize.SIZE));
        // שמות בכונן — מ-Paths

        String src = System.getProperty("vfm.src");
        if (src == null) return;
        File dir = new File(src, "src/com/tikiyot/vfm");
        // הקבצים שעברו (131; 1.7.78 — הפעולות בכונן; 1.7.79 — המסכים): כל הודעה מהמילון. מותר רק תחביר ושמות בכונן (מסומנים), ויומן.
        // 185 (1.7.82): כל הקבצים, חוץ מהמילון עצמו. תחביר ושמות בכונן — מסומנים בשורה ("// תחביר", "// בכונן")
        // מ0.45: CmdMatrix — קובץ פקודות של בדיקה, כולו תחביר (Diag, שהיה כאן, נמחק במ0.43)
        java.util.Set<String> notYet = new java.util.HashSet<String>(Arrays.asList("Words", "CmdMatrix"));
        // מבנה 1 (13.11.2026): הקוד מתחלק לתיקיות לפי נושא (החבילה אחת) — הבדיקה עוברת על כל התיקיות, בכל העומק
        List<File> all = javaFiles(dir);
        java.util.Map<String, File> byName = new java.util.TreeMap<String, File>();
        for (File x : all) byName.put(x.getName().replace(".java", ""), x);
        List<String> names = new ArrayList<String>();
        for (String n : byName.keySet()) if (!notYet.contains(n)) names.add(n);
        check("185: הבדיקה עוברת על כל הקבצים (" + names.size() + ")", names.size() > 60);
        for (String name : names) {
            List<String> loose = new ArrayList<String>();
            try {
                for (String[] l : literals(Files.readUtf8(byName.get(name))))
                    if (hebrew(l[0]) && !l[1].contains("// תחביר") && !l[1].contains("// בכונן") && !l[1].contains("Log."))
                        loose.add(l[0]);
            } catch (Exception e) {
                loose.add(String.valueOf(e));
            }
            check("131: " + name + " — אין הודעה מחוץ למילון" + (loose.isEmpty() ? "" : ": " + loose), loose.isEmpty());
        }
        // ההודעות החוזרות — רק במילון, לא שוב בקוד (אז הניסוח לא יסטה)
        String[] once = {"ביטול", "סגירה", "לא נבחר דבר", "צריך שם", "הרשימה לא נמצאה", "הקובץ לא נמצא במסלול שלו",
                "לא נוגע בקבצים", "נשמר גיבוי לפני המחיקה, בתיקייה .גיבויים", "טוען את האינדקס…", "המחיקה נכשלה",
                "שינוי השם נכשל", "ההעתקה נכשלה", "השחזור נכשל", "הדבקה כאן", "קובץ אחד", "תיקייה אחת", "פריט אחד", "רשימה אחת"};
        for (String w : once) {
            List<String> where = new ArrayList<String>();
                for (File f : all) {
                    if (!f.getName().endsWith(".java") || f.getName().equals("Words.java")) continue;
                    try {
                        for (String[] l : literals(Files.readUtf8(f))) if (l[0].equals(w)) where.add(f.getName());
                    } catch (Exception e) {
                        where.add(String.valueOf(e));
                    }
                }
            check("131: \"" + w + "\" — רק במילון" + (where.isEmpty() ? "" : ": " + where), where.isEmpty());
        }
    }

    /** כל קובצי הקוד שמתחת לתיקייה, בכל העומק. */
    private static List<File> javaFiles(File dir) {
        List<File> out = new ArrayList<File>();
        File[] k = dir.listFiles();
        if (k != null) for (File x : k) {
            if (x.isDirectory()) out.addAll(javaFiles(x));
            else if (x.getName().endsWith(".java")) out.add(x);
        }
        return out;
    }

    /** 144: תנאי תאריך וכונן — דרך החיפוש עצמו (השאילתה מנורמלת), לא רק דרך Cond. */
    private static void smartDate() {
        File root = new File(home, "ת144");
        root.mkdirs();
        Calendar c = Calendar.getInstance();
        int y = c.get(Calendar.YEAR);
        String[][] fs = {{"old.pdf", "2020-03-10"}, {"aug.pdf", y + "-08-20"}, {"sep.pdf", y + "-09-05"}, {"now.pdf", null}};
        for (String[] f : fs) {
            File x = new File(root, f[0]);
            try { new java.io.FileOutputStream(x).close(); } catch (Exception e) { throw new RuntimeException(e); }
            if (f[1] == null) x.setLastModified(System.currentTimeMillis() - 3600000L);
            else {
                String[] p = f[1].split("-");
                c.clear();
                c.set(Integer.parseInt(p[0]), Integer.parseInt(p[1]) - 1, Integer.parseInt(p[2]), 12, 0);
                x.setLastModified(c.getTimeInMillis());
            }
        }
        Index ix = built("ת144", root);
        Library lib = Library.forTests(Collections.singletonList(ix));
        List<String> since = names(lib.search("תאריך:" + y + "-09-01", false, false));
        check("144: מתאריך מסוים — נמצאו (היה: כלום)", since.contains("now.pdf") && !since.contains("old.pdf") && !since.contains("aug.pdf")
                && since.contains("sep.pdf"));
        check("144: אותו תאריך בכתיב אחר — 1.9." + y, names(lib.search("תאריך:1.9." + y, false, false)).equals(since));
        check("144: שנה בשתי ספרות — 1.9." + (y % 100), names(lib.search("תאריך:1.9." + (y % 100), false, false)).equals(since));
        check("144: יום וחודש — השנה הנוכחית", names(lib.search("תאריך:1/9", false, false)).equals(since));
        List<String> before = names(lib.search("תאריך:<" + y + "-09-01", false, false));
        check("144: לפני תאריך", before.contains("old.pdf") && before.contains("aug.pdf") && !before.contains("now.pdf"));
        check("144: יום (היה: לא הובן)", names(lib.search("תאריך:יום", false, false)).equals(Arrays.asList("now.pdf")));
        check("144: 3ימים", names(lib.search("תאריך:3ימים", false, false)).contains("now.pdf")
                && !names(lib.search("תאריך:3ימים", false, false)).contains("old.pdf"));
        check("144: שנה (בשנה האחרונה) — בלי הישן", !names(lib.search("תאריך:שנה", false, false)).contains("old.pdf"));
        check("144: עם סוג — יחד", names(lib.search("סוג:pdf תאריך:" + y + "-09-01", false, false)).equals(since));
        check("144: כונן (היה: לא נמצא כלום)", names(lib.search("כונן:ת144", false, false)).size() == 4);
        // הטופס: "3 ימים" עם רווח — נכתב כך שהחיפוש מבין
        DateSize d = new DateSize();
        d.dateMode = 1;
        d.dateValue = "3 ימים";
        eq("144: רווח בערך — נכתב בלי רווח", "תאריך:3ימים", d.dateToken());
        check("144: והחיפוש מבין", names(lib.search(d.dateToken(), false, false)).contains("now.pdf"));
        check("144: התחביר — אותה מילה ב-Cond וב-DateSize", DateSize.DATE.equals(Cond.DATE + ":"));
    }

    private static java.util.Set<String> nameSet(Library.Result r) {
        return new java.util.TreeSet<String>(names(r));
    }

    private static java.util.Set<String> set(String... x) {
        return new java.util.TreeSet<String>(Arrays.asList(x));
    }

    /** 145: קבוצות הסוגים מההגדרות, "קובצי נתונים", "שאר הקבצים", בחירה ידנית — דרך החיפוש עצמו. */
    private static void kinds() {
        String data = Types.dataDefault();
        check("145: קובצי נתונים — התקנה ומכווצים", ("|" + data + "|").contains("|zip|") && ("|" + data + "|").contains("|apk|")
                && ("|" + data + "|").contains("|7z|") && ("|" + data + "|").contains("|iso|"));
        check("145: קובצי נתונים — בלי מה שבחמש הקבוצות", !("|" + data + "|").contains("|pdf|") && !("|" + data + "|").contains("|jpg|"));
        List<Types.Group> gs = Types.groups();
        check("145: שש קבוצות, לפי הסדר", gs.size() == 6 && gs.get(0).name.equals("ספרים") && gs.get(5).name.equals(Types.DATA));
        boolean written = false;
        try { written = Files.readUtf8(Conf.file()).contains("[" + Types.SECTION + "]"); } catch (Exception e) { }
        check("145: הקבוצות נכתבו לקובץ ההגדרות — אפשר לערוך", written);

        File root = new File(home, "ס145");
        root.mkdirs();
        for (String n : new String[]{"a.pdf", "b.pdf", "c.pdf", "d.zip", "e.apk", "f.weird", "noext", "g.jpg", ".h.zip"})
            try { new java.io.FileOutputStream(new File(root, n)).close(); } catch (Exception e) { throw new RuntimeException(e); }
        Index ix = built("ס145", root);
        Library lib = Library.forTests(Collections.singletonList(ix));
        eq("145: ספרים", set("a.pdf", "b.pdf", "c.pdf"), nameSet(lib.search("סוג:ספרים", false, false)));
        eq("145: קובצי נתונים", set("d.zip", "e.apk"), nameSet(lib.search("סוג:«קובצי נתונים»", false, false)));
        eq("145: שאר הקבצים — גם בלי סיומת", set("f.weird", "noext"), nameSet(lib.search("סוג:«שאר הקבצים»", false, false)));
        eq("145: שתיהן יחד", set("d.zip", "e.apk", "f.weird", "noext"),
                nameSet(lib.search("סוג:«קובצי נתונים|שאר הקבצים»", false, false)));
        eq("145: קבוצה וסוג ידני", set("a.pdf", "b.pdf", "c.pdf", "g.jpg"), nameSet(lib.search("סוג:ספרים|jpg", false, false)));
        eq("145: תנאי ישן (סיומות) — כמו קודם", set("a.pdf", "b.pdf", "c.pdf"),
                nameSet(lib.search("סוג:pdf|epub|mobi|azw3|djvu", false, false)));
        Conf.set(Types.SECTION, "ספרים", "pdf|zip");
        eq("145: שינוי הקבוצה בקובץ — הרשימות עוקבות", set("a.pdf", "b.pdf", "c.pdf", "d.zip"),
                nameSet(lib.search("סוג:ספרים", false, false)));
        Conf.set(Types.SECTION, "ספרים", Types.FIVE[0][1]);
        // ספירת הסוגים שבאינדקס — הנפוץ קודם, בלי מוסתרים ובלי "בלי סיומת"
        List<Map.Entry<String, Integer>> tc = lib.typeCounts();
        check("145: הנפוץ קודם", tc.size() > 0 && tc.get(0).getKey().equals("pdf") && tc.get(0).getValue() == 3);
        int zip = 0;
        for (Map.Entry<String, Integer> e : tc) if (e.getKey().equals("zip")) zip = e.getValue();
        check("145: מוסתר לא נספר", zip == 1);
        boolean noExt = false;
        for (Map.Entry<String, Integer> e : tc) if (e.getKey().isEmpty()) noExt = true;
        check("145: בלי סיומת — לא ברשימה", !noExt);

        // הבחירה בטופס — המודל: תנאי ⇄ בחירה
        Types.Choice c = new Types.Choice();
        eq("145: כל הסוגים — בלי תנאי", "", c.token());
        c.take("pdf|epub|mobi|azw3|djvu|jpg|jpeg|png|webp|gif|zip", gs);
        check("145: תנאי ישן של שתי קבוצות — לקבוצות (היה: נפל ל\"מילים\")",
                c.groups.equals(new java.util.LinkedHashSet<String>(Arrays.asList("ספרים", "תמונות"))) && c.manual.contains("zip"));
        eq("145: ובחזרה — בשמות", "סוג:ספרים|תמונות|zip", c.token());
        Types.Choice d = new Types.Choice();
        d.take("קובצי נתונים|שאר הקבצים", gs);
        check("145: קובצי נתונים ושאר", d.groups.contains(Types.DATA) && d.other && !d.all);
        eq("145: שם עם רווח — בתוך «»", "סוג:«קובצי נתונים|שאר הקבצים»", d.token());
        Cond parsed = Cond.parse(Text.norm(d.token()));
        check("145: התנאי מובן גם אחרי נרמול", parsed.unknown.isEmpty() && parsed.tests.size() == 1 && parsed.words.isEmpty());
        Types.Choice e = new Types.Choice();
        e.all = false;
        check("145: \"רק שאבחר\" בלי בחירה — ריק (הטופס עוצר)", e.empty() && e.token().isEmpty());
        check("145: מילת התנאי מזוהה גם מנורמלת", Cond.keyIs("סוג:pdf", Cond.TYPE) && Cond.keyIs(Text.norm("תאריך:שבוע"), Cond.DATE));
        eq("145: רכיב עם רווח — חוזר עם «»", "סוג:«שאר הקבצים»", Cond.quoteToken("סוג:שאר הקבצים"));
    }

    /** 146: אחרי פעולה — איפה הקובץ עכשיו (Physical.whereNow), ולמה השורה אפורה (FileItem.dropped). */
    private static void stable() {
        File root = new File(home, "י146");
        new File(root, "תיקייה").mkdirs();
        for (String n : new String[]{"a.pdf", "b.pdf", "c.pdf", "d.pdf", "תיקייה/e.pdf", "f.pdf"})
            try { new java.io.FileOutputStream(new File(root, n)).close(); } catch (Exception e) { throw new RuntimeException(e); }
        Index ix = built("י146", root);
        Library lib = Library.forTests(new ArrayList<Index>(Collections.singletonList(ix)));
        File tf = new File(home, "מבנה-146.txt");
        tf.delete();
        String a = new File(root, "a.pdf").getAbsolutePath(), b = new File(root, "b.pdf").getAbsolutePath(),
                c = new File(root, "c.pdf").getAbsolutePath(), d = new File(root, "d.pdf").getAbsolutePath(),
                dir = new File(root, "תיקייה").getAbsolutePath(), e = new File(root, "תיקייה/e.pdf").getAbsolutePath(),
                f = new File(root, "f.pdf").getAbsolutePath();

        check("146: לא זז — אותו מסלול", Physical.whereNow(a).equals(a));
        Physical.rename(lib, a, "a.txt");
        lib.flush(5000);
        String a2 = new File(root, "a.txt").getAbsolutePath();
        eq("146: שינוי שם — המקום החדש", a2, Physical.whereNow(a));
        eq("146: שם שאינו עונה (pdf ← txt) — \"לא עונה עוד\"", FileItem.State.OUT, FileItem.dropped(lib, Physical.whereNow(a), true));
        check("146: והחיפוש באמת לא מוצא אותו", !names(lib.search("סוג:pdf", false, false)).contains("a.txt"));

        Physical.rename(lib, b, "b2.pdf");
        lib.flush(5000);
        check("146: שם חדש שעדיין עונה — נמצא במקום החדש",
                names(lib.search("סוג:pdf", false, false)).contains("b2.pdf") && Physical.whereNow(b).endsWith("b2.pdf"));
        Physical.rename(lib, Physical.whereNow(b), "b3.pdf");
        lib.flush(5000);
        check("146: שתי פעולות ברצף — עד הסוף", Physical.whereNow(b).endsWith("b3.pdf"));

        Physical.delete(lib, Arrays.asList(c), false);
        lib.flush(5000);
        eq("146: נמחק — \"נמחק · בסל המחזור\"", FileItem.State.DELETED, FileItem.dropped(lib, Physical.whereNow(c), true));

        new File(d).delete();                                                 // נעלם מבחוץ
        eq("146: נעלם — חסר", FileItem.State.MISSING, FileItem.dropped(lib, Physical.whereNow(d), false));

        File dest = new File(root, "יעד");
        dest.mkdirs();
        Physical.transfer(lib, new ArrayList<String>(), new ArrayList<String>(Arrays.asList(dir)), dest, false, null, null, false);
        lib.flush(5000);
        eq("146: התיקייה שמעליו הועברה — גם הקובץ", new File(dest, "תיקייה/e.pdf").getAbsolutePath(), Physical.whereNow(e));

        Physical.transfer(lib, new ArrayList<String>(Arrays.asList(f)), new ArrayList<String>(), dest, true, null, null, false);
        lib.flush(5000);
        eq("146: העתקה — המקור לא זז", f, Physical.whereNow(f));
    }


    private static List<String> folderNames(List<FolderView.Folder> l) {
        List<String> out = new ArrayList<String>();
        for (FolderView.Folder f : l) out.add(f.name);
        java.util.Collections.sort(out);
        return out;
    }

    /** 148: המודל של התצוגה בתיקיות — מאיפה מתחילים, ספירה לכל תיקייה, מה מוצג, ולמעלה. */
    private static void folderView() {
        File root = new File(home, "ת148");
        for (String n : new String[]{"ספרים/רמחל/a.pdf", "ספרים/רמחל/b.pdf", "ספרים/רמחל/c.docx", "ספרים/מבוא.pdf",
                "ספרים/רשימה.txt", "תמונות/1.jpg", "הורדות/חדש.pdf", "ריקה/x.txt"}) {
            File f = new File(root, n);
            f.getParentFile().mkdirs();
            try { new java.io.FileOutputStream(f).close(); } catch (Exception e) { throw new RuntimeException(e); }
        }
        Index ix = built("ת148", root);
        Library lib = Library.forTests(Collections.singletonList(ix));
        List<Library.Hit> hits = lib.search("סוג:pdf", false, false).hits;
        String r = root.getAbsolutePath(), books = r + "/ספרים", ramchal = books + "/רמחל";
        FolderView v = new FolderView(hits, Arrays.asList(r));
        eq("148: מקור אחד — מתחילים בו", r, v.start());
        check("148: מהמקור היחיד — אין למעלה", !v.canGoUp(r) && v.up(r).equals(r));
        eq("148: כל ההתאמות", 4, v.total());
        eq("148: ספירה בתיקייה — כולל תת־תיקיות", 3, v.matches(books));
        eq("148: ספירה בתת־תיקייה", 2, v.matches(ramchal));
        eq("148: תיקייה בלי התאמות — 0", 0, v.matches(r + "/תמונות"));
        eq("148: \"רק מה שעונה\" — רק תיקיות עם התאמות", Arrays.asList("הורדות", "ספרים"), folderNames(v.matchDirs(r)));
        eq("148: \"רק מה שעונה\" — הקבצים שעונים בתיקייה", Arrays.asList(books + "/מבוא.pdf"), v.matchFiles(books));
        check("148: סימון התאמה", v.isMatch(ramchal + "/a.pdf") && !v.isMatch(ramchal + "/c.docx") && !v.isMatch(books + "/רשימה.txt"));
        eq("148: למעלה מתת־תיקייה", books, v.up(ramchal));

        FolderView two = new FolderView(hits, Arrays.asList(books, r + "/הורדות", r + "/תמונות"));
        check("148: כמה מקורות — מתחילים ברשימת המקורות", two.start() == null && !two.canGoUp(null));
        eq("148: ממקור — לרשימת המקורות", null, two.up(books));
        eq("148: רשימת המקורות — \"רק מה שעונה\" בלי מקור ריק", Arrays.asList("הורדות", "ספרים"), folderNames(two.top(true)));
        eq("148: רשימת המקורות — \"כולל הכול\" כולם", 3, two.top(false).size());
        eq("148: ספירה במקור", 3, two.matches(books));
        check("148: התאמה מחוץ למקורות — לא נספרת", new FolderView(hits, Arrays.asList(r + "/תמונות")).total() == 0);

        List<String[]> vols = Arrays.asList(new String[]{"/storage/emulated/0", Sources.INTERNAL}, new String[]{"/storage/AB-12", Sources.CARD});
        eq("148: בלי מקורות — כל הכוננים", Arrays.asList("/storage/emulated/0", "/storage/AB-12"),
                FolderView.roots(new ArrayList<String>(), vols));
        eq("148: \"כרטיס\" ותיקייה", Arrays.asList("/storage/AB-12", "/storage/emulated/0/Download"),
                FolderView.roots(Arrays.asList(Sources.CARD, "/storage/emulated/0/Download"), vols));

        // התצוגה — הגדרה של הרשימה: מקומית ועם שם, וברירת מחדל
        eq("148: בלי תצוגה — ברצף", FolderView.FLAT, SmartLists.resolve(Target.of("חכמה", "תנאי", "סוג:pdf"))[2]);
        eq("148: מקומית — מהכלי", FolderView.ALL,
                SmartLists.resolve(Target.of("חכמה", "תנאי", "סוג:pdf", SmartLists.VIEW, FolderView.ALL))[2]);
        eq("148: ערך לא מוכר — ברצף", FolderView.FLAT,
                SmartLists.resolve(Target.of("חכמה", "תנאי", "סוג:pdf", SmartLists.VIEW, "משהו"))[2]);
    }


    /** 153: תנאי ← הגדרות (לחלונית) + מילים (לשורה), ובחזרה; כלי חדש מחזיר מזהה; איפה הכלי יושב. */
    private static void oneSearch() {
        String[] a = SmartLists.split("מקור:כרטיס סוג:ספרים רמח\"ל תאריך:שבוע מסילת");
        eq("153: הגדרות — מקור, סוג, תאריך", "מקור:כרטיס סוג:ספרים תאריך:שבוע", a[0]);
        eq("153: מילים — לשורת ההקלדה", "רמח\"ל מסילת", a[1]);
        String[] b = SmartLists.split("סוג:pdf");
        check("153: בלי מילים — השורה ריקה", b[0].equals("סוג:pdf") && b[1].isEmpty());
        String[] c = SmartLists.split("רשימה:«לימוד א» ספר");
        eq("153: תנאי בלי שדה — בשורה, כמו שהוא", "רשימה:«לימוד א» ספר", c[1]);
        eq("153: תיקייה (תחביר ישן) — למקורות", "מקור:«/x y»", SmartLists.split("תיקייה:«/x y»")[0]);
        String[] d = SmartLists.split("סוג:«קובצי נתונים|שאר הקבצים»");
        eq("153: ערך עם רווח — נשמר בתוך «»", "סוג:«קובצי נתונים|שאר הקבצים»", d[0]);
        String[] e = SmartLists.split("תאריך:שבוע תאריך:<שנה");
        check("153: תאריך שני — לשורה (לחלונית אחד)", e[0].equals("תאריך:שבוע") && e[1].equals("תאריך:<שנה"));
        check("153: ריק", SmartLists.split(null)[0].isEmpty() && SmartLists.split("")[1].isEmpty());
        eq("153: חיבור", "סוג:pdf רמח\"ל", SmartLists.join("סוג:pdf", " רמח\"ל "));
        check("153: חיבור עם צד ריק", SmartLists.join("", "x").equals("x") && SmartLists.join("סוג:pdf", "").equals("סוג:pdf"));
        String q = "מקור:כרטיס סוג:ספרים רמח\"ל תאריך:שבוע";
        String[] r = SmartLists.split(q);
        File root = new File(home, "ח153");
        root.mkdirs();
        Library lib = Library.forTests(Collections.singletonList(built("ח153", root)));
        eq("153: פיצול וחיבור — אותו חיפוש", names(lib.search(q, false, false)), names(lib.search(SmartLists.join(r[0], r[1]), false, false)));

        File f = new File(home, "מבנה-153.ini");
        f.delete();
        try {
        } finally {
        }
    }



    private static void deleteTree(File f) {
        File[] k = f.listFiles();
        if (k != null) for (File x : k) deleteTree(x);
        f.delete();
    }

    /** 149: הרשימה החכמה מדביקה בעצמה (לא זורקת למנהל הקבצים). 158: הקבצים של אופן — לשיתוף. */
    private static void pasteShare() {
        check("149: הרשימה החכמה — מדביקה בעצמה", Clip.Holder.class.isAssignableFrom(SearchScreen.class));
        File dir = new File(new File(Paths.data(), Modes.DIR), "ש158");
        try {
        } catch (Exception e) { throw new RuntimeException(e); }
    }



    private static String text(Ini ini) {
        StringBuilder sb = new StringBuilder();
        for (Ini.Line l : ini.lines) sb.append(l.raw).append('\n');
        return sb.toString();
    }




    /** 1.13.1 — תיקוני באגים. */

    /** קובץ הבדיקה של 1.23.0, אות באות — לבדיקת המחיקה שלו. */
    private static final String OLD_TEST = "הדמיה\nREM בדיקת ממשק הפקודות 1.23.0. לעבור על כל שורה בתפריט שלמעלה: לבחור, לקרוא את ההסבר והפרמטרים, ולנסות ▶ הדמיה.\nREM השורה הראשונה (הדמיה) — כל הקובץ רץ בכאילו, ושום דבר לא משתנה בכונן. הקובץ נמחק בגרסה הבאה.\nECHO מתחילים את הבדיקה\nMD \"%פנימי%\\בדיקת-פקודות\\א\"\nCOPY \"%הורדות%\\*.pdf\" \"%פנימי%\\בדיקת-פקודות\\א\" /תפוס:לדלג /תאריך:שנה\nCOPY \"%פנימי%\\Documents\" \"%פנימי%\\בדיקת-פקודות\\ב\" /S /יומן:\"%פנימי%\\בדיקת-פקודות\\יומן.txt\"\nMOVE \"%פנימי%\\בדיקת-פקודות\\א\\*.pdf\" \"%פנימי%\\בדיקת-פקודות\\ג\" /גודל:>1MB\nREN \"%פנימי%\\בדיקת-פקודות\\קובץ.txt\" חדש.txt\nCOMP \"%פנימי%\\בדיקת-פקודות\\א\" \"%פנימי%\\בדיקת-פקודות\\ב\"\nCOPY %רק-במקור% \"%פנימי%\\בדיקת-פקודות\\ב\" /תפוס:לעצור\nMOVE %רק-ביעד% \"%פנימי%\\בדיקת-פקודות\\היסטוריה\\%תאריך%\" /עד:20%\nCOMP /רשימה:גיבוי \"%פנימי%\\בדיקת-פקודות\\גיבוי\"\nDEL \"%פנימי%\\בדיקת-פקודות\\*.tmp\"\nRD \"%פנימי%\\בדיקת-פקודות\\ריקה\"\nRD \"%פנימי%\\בדיקת-פקודות\\ג\" /S\nCOPY \"%פנימי%\\גיבוי\" \"%פנימי%\\בדיקת-פקודות\\שחזור\" /מצב:\"28.10.2026\"\nSTART \"%פנימי%\\בדיקת-פקודות\"\nפתח מסך חיפוש\nמצא סוג:ספרים\n";





    /** 1.23.0: CmdLine — שינוי פרמטר אחד בלי לאבד דבר; התבניות; קובץ הבדיקה. */
    /** 1.23.0: הפקודות הנפוצות (CmdLine — של העורך הישן — נמחק במ0.45+, עם העורך). */
    private static void v1230() {
        // התבניות
        String src0 = System.getProperty("vfm.src");
        String tplText = "";
        try {
            tplText = Files.readUtf8(new File(src0, "assets/commands/templates.txt"));
        } catch (Throwable t) {
            check("1.23.0: קבצי ההתקנה נקראו", false);
        }
        List<CmdTemplates.Tpl> ts = CmdTemplates.parse(tplText);
        check("1.23.0: תבניות התוכנה", ts.size() >= 8);
        check("1.23.0: גיבוי — מקומות פתוחים", CmdTemplates.holes(ts.get(0).lines).equals(Arrays.asList("יעד", "מקור")));
        check("1.23.0: /תאריך:<שנה — לא מקום פתוח", CmdTemplates.holes(Arrays.asList("COPY a b /תאריך:<שנה /גודל:>10MB")).isEmpty());
        for (CmdTemplates.Tpl t : ts) {
            java.util.Map<String, String> v = CmdTemplates.empty(CmdTemplates.holes(t.lines));
            for (String h : v.keySet()) v.put(h, "%פנימי%\\x");
            List<String> filled = CmdTemplates.fill(t.lines, v);
            boolean ok = CmdTemplates.holes(filled).isEmpty();
            for (String fl : filled) ok &= Commands.get(Commands.normal(fl).split(" ")[0]) != null || Loops.isLoopStart(fl) || Loops.isLoopEnd(fl)
                    || fl.startsWith(Syntax.MEASURE) || fl.startsWith("::") || fl.equals(Syntax.CHECK);             // 1.25.0: שורות מבנה; הערה
            check("1.23.0: תבנית — ממולאת ומוכרת: " + t.name, ok);
        }
        File tr = drive("נפוצות");
        CmdTemplates.testRoot = tr;
        try {
            File a = CmdTemplates.save("שלי", Arrays.asList("COPY a b")), b = CmdTemplates.save("שלי", Arrays.asList("MD x"));
            List<CmdTemplates.Tpl> mine = CmdTemplates.mine();
            check("1.23.0: שלי — נשמרה, ושם תפוס מקבל מספר", a != null && b != null && !a.equals(b) && mine.size() == 2
                    && mine.get(0).lines.get(0).equals("COPY a b"));
            check("1.23.0: שם עם תווים אסורים", CmdTemplates.save("א/ב", Arrays.asList("MD x")) != null);
        } finally {
            CmdTemplates.testRoot = null;
        }
    }

    /** 1.22.5: ההגדרה האחת — שלמה, תואמת למנוע, וכל פרמטר עם שם והסבר. */
    private static void v1225() {
        check("1.22.5: פקודות הקבצים — אותן ובאותו סדר (מ0.14: ודא)",
                java.util.Arrays.equals(Commands.fileNames(), new String[]{"COPY", "MOVE", "DEL", "REN", "MD", "RD", "COMP", "LINK", "VERIFY", "MODE", "CONTINUE", "WRITE", "WAIT", "BACKUP", "REFRESH", "REPORT", "CALL", "START", "ECHO", "REM", "SECTION", "DROPMODE", "UNTRASH", "PURGE", "EXAMPLES", "SET", "PACK", "UNPACK"}));   // מ0.44: קטע; מ0.45: מחק אופן, שחזר מהסל, רוקן סל; מבנה 122: ארוז, פרוס
        check("1.22.5: שמות נרדפים", Commands.canonical("MKDIR").equals("MD") && Commands.canonical("rmdir").equals("RD")
                && Commands.canonical("ERASE").equals("DEL") && Commands.canonical("RENAME").equals("REN")
                && Commands.canonical("copy").equals("COPY") && Commands.canonical("FORMAT").equals("FORMAT")
                && Commands.canonical("ECHO.").equals("ECHO."));
        for (Commands.Def d : Commands.ALL) {
            check("1.22.5: מה עושה — " + d.name, !d.what.isEmpty());
            check("1.22.5: שימוש — " + d.name, !d.usage().isEmpty() && (d.family != Commands.Family.FILE || d.usage().startsWith(Commands.word(d))));   // מ0.38: בעברית
            for (Commands.Param p : d.params) {
                boolean ok = !p.label.isEmpty() && !p.help.isEmpty() && (!p.isSwitch() || Parse.isSwitch(p.key))
                        && (p.type != Commands.Type.CHOICE || !p.options.isEmpty());
                check("1.22.5: פרמטר — " + d.name + " " + (p.isSwitch() ? p.key : p.label), ok);
            }
        }
        String[] verbs = {Verbs.OPEN, Verbs.CREATE, Verbs.UPDATE, Verbs.FIND, Verbs.BACK, Verbs.RUN, Verbs.ADD};
        for (String v : verbs) {
            Commands.Def d = Commands.get(v);
            check("1.22.5: פועל בהגדרה — " + v, d != null && d.family == Commands.Family.VERB && Verbs.isVerb(v + " x"));
        }
        // כל מתג שהמנוע מכיר — שייך לפקודה בהגדרה
        String[][] seen = {{"COPY", "/S"}, {"COPY", "/מסלול"}, {"COPY", "/Y"}, {"COPY", "/-Y"}, {"COPY", "/תפוס:לדלג"},
                {"COPY", "/יומן:x.txt"}, {"COPY", "/עד:20%"}, {"COPY", "/מצב:26.10.2026"}, {"COPY", "/תאריך:שבוע"},
                {"COPY", "/גודל:>1MB"}, {"COPY", "/A:H"}, {"COPY", "/A"}, {"COPY", "/D:1-1-2026"}, {"COPY", "/MAXAGE:3"},
                {"COPY", "/MINAGE:3"}, {"COPY", "/MAX:5"}, {"COPY", "/MIN:5"}, {"COPY", "/Q"}, {"COPY", "/F"},
                {"MOVE", "/עד:20%"}, {"DEL", "/תאריך:שבוע"}, {"RD", "/S"}, {"COMP", "/רשימה:גיבוי"}, {"COMP", "/A:H"}};
        for (String[] x : seen) {
            Commands.Def d = Commands.get(x[0]);
            check("1.22.5: מתג בהגדרה — " + x[0] + " " + x[1], Parse.isSwitch(x[1]) && d.switchFor(x[1]) != null);
        }
        check("1.22.5: COPY בלי /רשימה:, COMP בלי /מצב:", Commands.get("COPY").switchFor("/רשימה:x") == null
                && Commands.get("COMP").switchFor("/מצב:1.1.2026") == null);
        String u = Commands.usage("COPY");
        check("1.22.5: שימוש עם המתגים עצמם", u.contains(Syntax.SW_TAKEN) && u.contains(Syntax.SW_JOURNAL) && !u.contains("/MAXAGE"));
        check("1.22.5: COMP — שתי הצורות", Commands.usage("COMP").contains(Syntax.SW_LIST));
        check("1.22.5: חזור — שימוש משלו", Words.Verb.usage("חזור").contains("חזור") && !Words.Verb.usage("חזור").contains("פתח"));
        check("1.22.5: צור פקודה ופתח תפריט בשימוש", Words.Verb.usage("צור").contains("פקודה") && Words.Verb.usage("פתח").contains("תפריט"));
        check("1.22.5: הדמיה — כותרת", Commands.get("בכאילו") != null && Commands.get("בכאילו").family == Commands.Family.HEADER);
        File rr = drive("שינוי-שם-תיקייה");
        new File(rr, "תיקייה").mkdirs();
        Batch.Plan pr = Batch.plan("REN \"" + new File(rr, "תיקייה").getAbsolutePath() + "\" חדש", rr, new java.util.HashMap<String, String>());
        check("1.22.5: REN על תיקייה — הודעה ברורה", pr.errors == 1 && Words.Cmd.REN_FOLDER.equals(pr.steps.get(0).error));
        check("1.22.5: שימוש — ערך המתג לפי סוגו", Commands.usage("COPY").contains(Syntax.SW_JOURNAL + Words.Def.V_FILE)
                && Commands.usage("COPY").contains(Syntax.SW_STATE + Words.Def.V_TIME));
    }

    private static void v1220() {
        File r1 = drive("רשימה-פנימי"), r2 = drive("רשימה-כרטיס");
        write(new File(r1, "Docs"), "a.txt", "a1").setLastModified(1700000000000L);
        write(new File(r1, "Docs/sub"), "b.txt", "b").setLastModified(1700000000000L);
        new File(r1, "Docs/empty").mkdirs();
        write(new File(r2, "מסמכים"), "חשבון-1.pdf", "1").setLastModified(1700000000000L);
        write(new File(r2, "מסמכים"), "חשבון-2.pdf", "2").setLastModified(1700000000000L);
        write(new File(r2, "x"), "y.txt", "y").setLastModified(1700000000000L);
        Index i1 = built(Drive.INTERNAL, r1), i2 = built("ABCD-1234", r2);
        Library lib = Library.forTests(new ArrayList<Index>(Arrays.asList(i1, i2)));
        File sf = new File(home, "מבנה-1220.ini");
        sf.delete();
        File tf = new File(home, "מבנה-1220.txt");
        tf.delete();
        try {

            String text = "MD \"גיבוי\\נוכחי\" /תפוס:לדלג\n"   // מבנה 109: כמו בתבנית — גיבוי שרץ שוב ושוב
            + ""
                    + "COMP /רשימה:\"המסמכים שלי\" \"גיבוי\\נוכחי\"\n"
                    + "MOVE %רק-ביעד% \"גיבוי\\היסטוריה\\%תאריך%\" /עד:50%\n"
                    + "COPY %רק-במקור% \"גיבוי\\נוכחי\" /תפוס:לעצור /יומן:\"גיבוי\\יומן\\%תאריך%.txt\"\n";
            Map<String, String> vars = Parse.vars(r1, r1.getAbsolutePath(), null);
            vars.put(Syntax.V_NOW, "01.10.2026 10-00-00");
            Batch.Plan p = Batch.plan(text, r1, vars, lib);
            Batch.run(p, lib, null, null);
            lib.flush(5000);
            File cur = new File(r1, "גיבוי/נוכחי");
            check("1.22.0: גיבוי רשימה — רק מה שברשימה", !new File(cur, Drive.INTERNAL + "/Download/יוגה למתחילים.pdf").exists());

            // שינויים: עריכה, קובץ שיצא מהרשימה החכמה (נמחק), קובץ חדש בתיקייה המתעדכנת
            write(new File(r1, "Docs"), "a.txt", "a2").setLastModified(1700000005000L);
            File gone2 = new File(r2, "מסמכים/חשבון-2.pdf");
            gone2.delete();
            lib.fileRemoved(gone2.getAbsolutePath());
            write(new File(r1, "Docs/sub"), "c.txt", "c");
            lib.flush(5000);
            vars.put(Syntax.V_NOW, "05.10.2026 10-00-00");
            p = Batch.plan(text, r1, vars, lib);
            Batch.run(p, lib, null, null);
            lib.flush(5000);
            File h = new File(r1, "גיבוי/היסטוריה/05.10.2026 10-00-00");
            // מצב בתאריך עובד גם כאן
            java.util.TreeMap<String, String> st = stateOf(new File(r1, "גיבוי"), "מצב:03.10.2026");

            // שגיאות
            check("1.22.0: רשימה שלא קיימת — שגיאה", Batch.plan("COMP /רשימה:אין \"גיבוי\\נוכחי\"", r1, vars, lib).errors == 1);
            check("1.22.0: רשימה ריקה — שגיאה", Batch.plan("COMP /רשימה:ריקה \"גיבוי\\נוכחי\"", r1, vars, lib).errors == 1);
            check("1.22.0: בלי אינדקס — שגיאה", Batch.plan("COMP /רשימה:\"המסמכים שלי\" \"גיבוי\\נוכחי\"", r1, vars).errors == 1);
            check("1.22.0: /רשימה: ב־COPY — שגיאה", Batch.plan("COPY /רשימה:x Docs ש", r1, vars, lib).errors >= 1);
            // 1.22.4: קבצי הגיבוי לא בחיפוש הרגיל (ממצא 6); כן — עם מצב:
            boolean inBk = false, anyTxt = false;
            for (Library.Hit hh : lib.search("סוג:txt", false, true).hits) {
                anyTxt = true;
                if (hh.ix.path(hh.rec).contains("/גיבוי/")) inBk = true;
            }
            check("1.22.4: חיפוש רגיל — בלי קבצי הגיבוי", anyTxt && !inBk);
            int st4 = 0;
            for (Library.Hit hh : lib.search("סוג:txt מצב:20.10.2026", false, true).hits)
                if (hh.ix.path(hh.rec).contains("/גיבוי/")) st4++;
            File rd = Reserved.testDir;
            Reserved.testDir = new File(home, "פקודות-1220");
            try {
                Batch.Plan gb = Batch.plan("COMP /רשימה:גיבוי \"גיבוי\\נוכחי\"", r1, vars, lib);
            } finally {
                Reserved.testDir = rd;
            }
            String nf = Batch.plan("COMP /רשימה:\"אין כזו\" \"גיבוי\\נוכחי\"", r1, vars, lib).steps.get(0).error;
            Batch.Plan amb = Batch.plan("COMP /רשימה:כפול \"גיבוי\\נוכחי\"", r1, vars, lib);
        } finally {
        }
    }

    /** הקבצים בגיבוי שעונים לתנאי: מסלול לוגי ← תוכן. */
    private static java.util.TreeMap<String, String> stateOf(File root, String cond) {
        Index ix = built("מצב-" + System.nanoTime(), root);
        Cond c = Cond.parse(cond);
        java.util.TreeMap<String, String> out = new java.util.TreeMap<String, String>();
        State st = new State(0);
        for (Index.Rec r : ix.files) {
            if (r.gone || !c.matches(ix, r)) continue;
            String p = ix.path(r);
            State.Where w = st.where(p);
            out.put(w == null ? p : w.rel, read(new File(p)));
        }
        return out;
    }

    private static void v1210() {
        // תאריכים ושמות
        long s1 = State.stamp("01.10.2026 10-00-00");
        check("1.21.0: שם גיבוי — נקרא", s1 != Long.MIN_VALUE && State.stamp("0007 01.10.2026 10-00-00") == s1);
        check("1.21.0: שם שאינו גיבוי", State.stamp("ספרים") == Long.MIN_VALUE && State.stamp("0002") == Long.MIN_VALUE);
        check("1.21.0: תאריך בלבד — סוף היום", State.when("01.10.2026") > s1 && State.when("01.10.2026") < s1 + 14L * 3600 * 1000);
        check("1.21.0: תאריך ושעה", State.when("01.10.2026 10:00") >= s1 && State.when("01.10.2026 09:59") < s1);
        check("1.21.0: לא תאריך", State.when("אתמול בערב") == Long.MIN_VALUE);

        File root = drive("מצב");
        File docs = new File(root, "מסמכים");
        Index ix = built("מצב", root);
        Library lib = Library.forTests(new ArrayList<Index>(Collections.singletonList(ix)));
        File tf = new File(home, "מבנה-מצב.txt");
        tf.delete();
        String text = "MD \"גיבוי\\נוכחי\" /תפוס:לדלג\n"   // מבנה 109: כמו בתבנית — גיבוי שרץ שוב ושוב
            + ""
                + "COMP מסמכים \"גיבוי\\נוכחי\"\n"
                + "MOVE %רק-ביעד% \"גיבוי\\היסטוריה\\%תאריך%\"\n"
                + "COPY %רק-במקור% \"גיבוי\\נוכחי\" /תפוס:לעצור /יומן:\"גיבוי\\יומן\\%תאריך%.txt\"\n";
        long base = 1700000000000L;
        // גיבוי 1
        write(docs, "a.txt", "v1").setLastModified(base);
        write(docs, "b.txt", "b").setLastModified(base);
        write(new File(docs, "dir"), "c.txt", "c").setLastModified(base);
        Map<String, String> vars = Parse.vars(root, root.getAbsolutePath(), null);
        vars.put(Syntax.V_NOW, "01.10.2026 10-00-00");
        Batch.Plan p = Batch.plan(text, root, vars, lib);
        Batch.run(p, lib, null, null);
        lib.flush(5000);
        // גיבוי 2: a נערך, b נמחק, d חדש
        write(docs, "a.txt", "v2").setLastModified(base + 1000);
        new File(docs, "b.txt").delete();
        write(docs, "d.txt", "d").setLastModified(base);
        vars.put(Syntax.V_NOW, "05.10.2026 10-00-00");
        Batch.run(Batch.plan(text, root, vars, lib), lib, null, null);
        lib.flush(5000);
        // גיבוי 3: a שוב, c שינה שם
        write(docs, "a.txt", "v3").setLastModified(base + 2000);
        new File(docs, "dir/c.txt").renameTo(new File(docs, "dir/c2.txt"));
        vars.put(Syntax.V_NOW, "10.10.2026 10-00-00");
        p = Batch.plan(text, root, vars, lib);
        check("1.21.0: (הכנה) שלושה גיבויים בלי שגיאות", p.errors == 0);
        Batch.run(p, lib, null, null);
        lib.flush(5000);
        File back = new File(root, "גיבוי");

        // תנאי החיפוש
        java.util.TreeMap<String, String> e1 = new java.util.TreeMap<String, String>();
        e1.put("a.txt", "v1");
        e1.put("b.txt", "b");
        e1.put("dir/c.txt", "c");
        eq("1.21.0: מצב בין גיבוי 1 ל־2", e1.toString(), stateOf(back, "מצב:03.10.2026").toString());
        java.util.TreeMap<String, String> e2 = new java.util.TreeMap<String, String>();
        e2.put("a.txt", "v2");
        e2.put("d.txt", "d");
        e2.put("dir/c.txt", "c");
        eq("1.21.0: מצב ביום גיבוי 2", e2.toString(), stateOf(back, "מצב:05.10.2026").toString());
        eq("1.21.0: לפני השעה של גיבוי 2 — עדיין 1", e1.toString(), stateOf(back, "מצב:«05.10.2026 09:59»").toString());
        java.util.TreeMap<String, String> e3 = new java.util.TreeMap<String, String>();
        e3.put("a.txt", "v3");
        e3.put("d.txt", "d");
        e3.put("dir/c2.txt", "c");
        eq("1.21.0: מצב אחרי גיבוי 3", e3.toString(), stateOf(back, "מצב:20.10.2026").toString());
        eq("1.21.0: מצב-בתאריך — שם נרדף", e3.toString(), stateOf(back, "מצב-בתאריך:20.10.2026").toString());
        check("1.21.0: לפני הגיבוי הראשון — ריק", stateOf(back, "מצב:30.09.2026").isEmpty());
        eq("1.21.0: עם תנאי נוסף (גודל)", "{a.txt=v1}", stateOf(back, "מצב:03.10.2026 גודל:>1").toString());
        check("1.21.0: מצב: לא מובן — לא נקלט כתנאי", !Cond.parse("מצב:אתמול").unknown.isEmpty());
        check("1.21.0: קובץ מחוץ לגיבוי — אינו במצב", stateOf(docs, "מצב:20.10.2026").isEmpty());

        // שחזור
        p = Batch.plan("COPY גיבוי שחזור1 /מצב:03.10.2026", root, vars, lib);
        check("1.21.0: שחזור — תכנון", p.errors == 0 && p.steps.get(0).files.size() == 3);
        Batch.run(p, lib, null, null);
        lib.flush(5000);
        File r1 = new File(root, "שחזור1");
        check("1.21.0: שחזור — המצב של אז, במסלולים הלוגיים", "v1".equals(read(new File(r1, "a.txt")))
                && "b".equals(read(new File(r1, "b.txt"))) && "c".equals(read(new File(r1, "dir/c.txt"))) && !new File(r1, "d.txt").exists());
        check("1.21.0: שחזור — הגיבוי לא השתנה", "v3".equals(read(new File(back, "נוכחי/a.txt"))));
        p = Batch.plan("COPY \"גיבוי\\dir\" שחזור2 /מצב:\"05.10.2026 12:00\"", root, vars, lib);
        Batch.run(p, lib, null, null);
        lib.flush(5000);
        check("1.21.0: שחזור תת־תיקייה", new File(root, "שחזור2/c.txt").isFile() && new File(root, "שחזור2").list().length == 1);
        p = Batch.plan("COPY \"גיבוי\\*.txt\" שחזור3 /מצב:20.10.2026", root, vars, lib);
        eq("1.21.0: שחזור עם תבנית — בכל העומק", 3, p.steps.get(0).files.size());
        // מבנה 61 (ממצא 40ב): קובץ אחד בשם מפורש — עד כאן נמצאו 0 קבצים, בלי שגיאה
        p = Batch.plan("COPY \"גיבוי\\a.txt\" שחזור4 /מצב:03.10.2026", root, vars, lib);
        check("מבנה 61: שחזור של קובץ אחד בשם מפורש — תכנון: קובץ אחד", p.errors == 0 && p.steps.get(0).files.size() == 1);
        Batch.run(p, lib, null, null);
        lib.flush(5000);
        check("מבנה 61: ...הגרסה של אז (מההיסטוריה), ישר בתיקיית היעד", "v1".equals(read(new File(root, "שחזור4/a.txt"))) && new File(root, "שחזור4").list().length == 1);
        p = Batch.plan("COPY \"גיבוי\\a.txt\" שחזור5 /מצב:20.10.2026", root, vars, lib);
        Batch.run(p, lib, null, null);
        lib.flush(5000);
        check("מבנה 61: ...ובזמן מאוחר — הגרסה הנוכחית", "v3".equals(read(new File(root, "שחזור5/a.txt"))));
        p = Batch.plan("COPY \"גיבוי\\dir\\c.txt\" שחזור6 /מצב:03.10.2026", root, vars, lib);
        Batch.run(p, lib, null, null);
        lib.flush(5000);
        check("מבנה 61: קובץ אחד מתת־תיקייה — ישר ביעד, בלי התיקייה", "c".equals(read(new File(root, "שחזור6/c.txt"))) && new File(root, "שחזור6").list().length == 1);
        p = Batch.plan("COPY \"גיבוי\\b.txt\" שחזור7 /מצב:20.10.2026", root, vars, lib);
        check("מבנה 61: קובץ שהיה בגיבוי ונמחק לפני הזמן שנבחר — 0 קבצים, בלי שגיאה", p.errors == 0 && p.steps.get(0).files.isEmpty());
        p = Batch.plan("COPY \"גיבוי\\אין-כזה.txt\" שחזור8 /מצב:20.10.2026", root, vars, lib);
        check("מבנה 61: מסלול שלא היה בגיבוי אף פעם — שגיאה שאומרת זאת (עד כאן: \"0 קבצים\")", p.errors == 1 && String.valueOf(p.steps.get(0).error).contains("לא היה בגיבוי"));
        check("מבנה 61: וגם תיקייה שלא הייתה, עם תבנית", Batch.plan("COPY \"גיבוי\\אין\\*.txt\" שחזור9 /מצב:20.10.2026", root, vars, lib).errors == 1);
        check("1.21.0: /מצב: ב־MOVE — שגיאה", Batch.plan("MOVE גיבוי ש /מצב:03.10.2026", root, vars).errors == 1);
        check("1.21.0: מקור שאינו גיבוי — שגיאה", Batch.plan("COPY מסמכים ש /מצב:03.10.2026", root, vars).errors == 1);
        check("1.21.0: נוכחי במסלול — שגיאה (מסלול לוגי)", Batch.plan("COPY \"גיבוי\\נוכחי\" ש /מצב:03.10.2026", root, vars).errors == 1);
        check("1.21.0: זמן לא מובן — שגיאה", Batch.plan("COPY גיבוי ש /מצב:מחר", root, vars).errors == 1);
        check("1.21.0: לתוך הגיבוי — שגיאה", Batch.plan("COPY גיבוי \"גיבוי\\ש\" /מצב:03.10.2026", root, vars).errors == 1);
    }

    /** תוכן כל הקבצים בעץ: מסלול יחסי ← גודל:בתים (בלי מוסתרים). */
    private static java.util.TreeMap<String, String> treeOf(File d) {
        java.util.TreeMap<String, String> out = new java.util.TreeMap<String, String>();
        treeOf(d, "", out);
        return out;
    }

    private static void treeOf(File d, String rel, java.util.TreeMap<String, String> out) {
        File[] k = d.listFiles();
        if (k == null) return;
        for (File x : k) {
            if (x.getName().startsWith(".")) continue;
            String r = rel.isEmpty() ? x.getName() : rel + "/" + x.getName();
            if (x.isDirectory()) {
                out.put(r + "/", "dir");
                treeOf(x, r, out);
            } else {
                try {
                    out.put(r, x.length() + ":" + java.util.Arrays.toString(java.nio.file.Files.readAllBytes(x.toPath())));
                } catch (Exception e) {
                    out.put(r, "?");
                }
            }
        }
    }

    private static File write(File dir, String name, String content) {
        try {
            dir.mkdirs();
            File f = new File(dir, name);
            java.io.FileOutputStream o = new java.io.FileOutputStream(f);
            o.write(content.getBytes("UTF-8"));
            o.close();
            return f;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void v1200() {
        File root = drive("השוואה");
        File docs = new File(root, "מסמכים");
        write(new File(docs, "א"), "1.txt", "אחת");
        write(new File(docs, "ב/ג"), "2.txt", "שתיים");
        write(docs, "3.txt", "שלוש");
        new File(docs, "ריקה").mkdirs();
        for (int k = 1; k <= 7; k++) write(new File(docs, "קבוע"), k + ".txt", "קבוע " + k);
        Index ix = built("השוואה", root);
        Library lib = Library.forTests(new ArrayList<Index>(Collections.singletonList(ix)));
        File tf = new File(home, "מבנה-השוואה.txt");
        tf.delete();
        File back = new File(root, "גיבוי");
        File cur = new File(back, "נוכחי");

        // 1. המשפך
        File x = write(new File(root, "פ1"), "a.bin", "AAAA"), y = write(new File(root, "פ2"), "a.bin", "AAAA");
        y.setLastModified(x.lastModified() - 50000);
        Compare.Result cr = Compare.byPath(lib, new File(root, "פ1"), new File(root, "פ2"), false);
        check("1.20.0: אותו תוכן, תאריך שונה — זהה (בקריאה)", cr.same == 1 && cr.read == 1 && cr.onlyA.isEmpty());
        write(new File(root, "פ2"), "a.bin", "AAAB");
        new File(root, "פ2/a.bin").setLastModified(x.lastModified() - 50000);
        cr = Compare.byPath(lib, new File(root, "פ1"), new File(root, "פ2"), false);
        check("1.20.0: אותו גודל, תוכן שונה — שונה", cr.same == 0 && cr.onlyA.size() == 1 && cr.onlyB.size() == 1);
        new File(root, "פ2/a.bin").setLastModified(x.lastModified());
        cr = Compare.byPath(lib, new File(root, "פ1"), new File(root, "פ2"), false);
        check("1.20.0: אותו גודל ותאריך — זהה בלי קריאה", cr.same == 1 && cr.read == 0);
        write(new File(root, "פ1"), "b", "x");
        write(new File(root, "פ2/b"), "c.txt", "x");
        cr = Compare.byPath(lib, new File(root, "פ1"), new File(root, "פ2"), false);
        check("1.20.0: קובץ מול תיקייה — שניהם \"רק\"", cr.onlyA.contains(new File(root, "פ1/b").getAbsolutePath())
                && cr.onlyBDirs.contains(new File(root, "פ2/b").getAbsolutePath()));

        // 2. גיבוי ראשון — יעד ריק (נוצר ב־MD)
        String text = "MD \"גיבוי\\נוכחי\" /תפוס:לדלג\n"   // מבנה 109: כמו בתבנית — גיבוי שרץ שוב ושוב
            + ""
                + "COMP מסמכים \"גיבוי\\נוכחי\"\n"
                + "MOVE %רק-ביעד% \"גיבוי\\היסטוריה\\%תאריך%\" /עד:50%\n"
                + "COPY %רק-במקור% \"גיבוי\\נוכחי\" /תפוס:לעצור /יומן:\"גיבוי\\יומן\\%תאריך%.txt\"\n";
        Map<String, String> vars = Parse.vars(root, root.getAbsolutePath(), null);
        Batch.Plan p = Batch.plan(text, root, vars, lib);
        check("1.20.0: גיבוי ראשון — בלי שגיאות", p.errors == 0);
        check("1.20.0: COMP — תיאור", p.steps.get(1).describe.contains("רק במקור 10"));
        check("1.20.0: COMP אינו \"שינוי\"", p.changes() == 3);
        List<String> rep = Batch.run(p, lib, null, null);
        lib.flush(5000);
        check("1.20.0: גיבוי ראשון — המראה זהה למקור", treeOf(docs).equals(treeOf(cur)));
        String t1 = vars.get(Syntax.V_NOW);
        eq("1.20.0: גיבוי ראשון — יומן", 10, Journal.read(new File(back, "יומן/" + t1 + ".txt")).size());
        check("1.20.0: גיבוי ראשון — הדוח כולל את ההשוואה", rep.size() == 4 && rep.get(1).contains("השוואה"));

        // 3. שינויים: עריכה, מחיקה, שינוי שם, תיקייה ריקה חדשה, תיקייה שנמחקה
        write(new File(docs, "א"), "1.txt", "אחת — נערך");
        new File(docs, "3.txt").delete();
        new File(docs, "ב/ג/2.txt").renameTo(new File(docs, "ב/ג/שתיים.txt"));
        new File(docs, "חדשה/ריקה").mkdirs();
        new File(docs, "ריקה").delete();
        write(docs, "4.txt", "ארבע");
        vars = Parse.vars(root, root.getAbsolutePath(), null);
        vars.put(Syntax.V_NOW, "0002");
        p = Batch.plan(text, root, vars, lib);
        check("1.20.0: גיבוי שני — בלי שגיאות", p.errors == 0);
        Batch.run(p, lib, null, null);
        lib.flush(5000);
        check("1.20.0: גיבוי שני — המראה זהה למקור", treeOf(docs).equals(treeOf(cur)));
        File h2 = new File(back, "היסטוריה/0002");
        check("1.20.0: היסטוריה — הגרסה הישנה של מה שנערך", "אחת".equals(read(new File(h2, "א/1.txt"))));
        check("1.20.0: היסטוריה — מה שנמחק ומה ששמו שונה", new File(h2, "3.txt").isFile() && new File(h2, "ב/ג/2.txt").isFile());
        check("1.20.0: היסטוריה — תיקייה ריקה שנמחקה", new File(h2, "ריקה").isDirectory());
        check("1.20.0: המראה — תיקיות שקיימות במקור לא נמחקו", new File(cur, "ב/ג").isDirectory() && new File(cur, "א").isDirectory());
        eq("1.20.0: יומן שני — רק מה שהגיע", 3, Journal.read(new File(back, "יומן/0002.txt")).size());

        // 4. בלי שינוי — כלום לא זז
        vars.put(Syntax.V_NOW, "0003");
        p = Batch.plan(text, root, vars, lib);
        check("1.20.0: בלי שינוי — אין מה להעביר ולהעתיק", p.errors == 0 && p.steps.get(2).files.isEmpty() && p.steps.get(3).files.isEmpty()
                && p.steps.get(2).dirs.isEmpty() && p.steps.get(3).dirs.isEmpty());

        // 5. מקור שנעלם — עצירה, ולא "הכול נמחק"
        File gone = new File(root, "מסמכים-x");
        docs.renameTo(gone);
        p = Batch.plan(text, root, vars, lib);
        check("1.20.0: מקור חסר — COMP שגיאה, והשורות שאחריו לא רצות", p.steps.get(1).error != null
                && p.steps.get(2).error != null && p.steps.get(3).error != null);
        gone.renameTo(docs);
        // 6. רוב הקבצים "נמחקו" — /עד: עוצר, וההעתקה שאחריה לא רצה
        for (File f : new File(docs, "א").listFiles()) f.delete();
        new File(docs, "4.txt").delete();
        new File(docs, "ב/ג/שתיים.txt").delete();
        for (File f : new File(docs, "קבוע").listFiles()) f.delete();
        p = Batch.plan(text, root, vars, lib);
        check("1.20.0: /עד:50% — עוצר", p.steps.get(2).error != null && p.steps.get(2).error.contains("50%"));
        eq("1.20.0: /עד: — ההעתקה על אותה השוואה לא רצה", Words.Cmd.COMPARE_BROKEN, p.steps.get(3).error);
        // 7. שימוש שגוי
        check("1.20.0: %רק-במקור% בלי COMP — שגיאה", Batch.plan("COPY %רק-במקור% גיבוי", root, vars).errors == 1);
        check("1.20.0: %רק-במקור% כיעד — שגיאה", Batch.plan("COMP מסמכים גיבוי\nCOPY מסמכים %רק-במקור%", root, vars).errors == 1);
        check("1.20.0: יעד חסר בלי MD — שגיאה", Batch.plan("COMP מסמכים אין-כזה", root, vars).errors == 1);
        check("1.20.0: /עד: לא מספר — שגיאה", Batch.plan("COMP מסמכים גיבוי\nMOVE %רק-ביעד% ה /עד:הרבה", root, vars).errors == 1);
        check("1.20.0: /תפוס:לעצור — מוכר", Batch.plan("COPY מסמכים\\*.* גיבוי /תפוס:לעצור", root, vars).errors == 0);
    }

    /** מבנה 132 / 197: סירוב של "בתוך תיקיית התוכנה" — על הקובץ, או (כשהתיקייה עוד לא קיימת) על התיקייה שהייתה נוצרת בשבילו. */
    private static boolean in132(String no) {
        return Words.Disk.NO_FILE_IN_APP.equals(no) || Words.Disk.NO_FOLDER_IN_APP.equals(no);
    }

    /** מבנה 168: כל קובצי הקוד שבתיקייה, ברצף — לשומרים שבודקים "אסור" (הקוד מתפצל לקבצים). */
    static String readAll(File dir) {
        StringBuilder sb = new StringBuilder();
        File[] k = dir.listFiles();
        if (k != null) for (File f : k) if (f.getName().endsWith(".java")) sb.append(read(f)).append('\n');
        return sb.toString();
    }

    private static String read(File f) {
        try {
            return Files.readUtf8(f);
        } catch (Exception e) {
            return null;
        }
    }

    private static void v1190() {
        File root = drive("מסלול");
        File src = new File(root, "מקור");
        mk(new File(src, "א"), "1.pdf", 10);
        mk(new File(src, "ב/ג"), "2.pdf", 20);
        mk(src, "3.txt", 30);
        new File(src, "ריקה/פנימית").mkdirs();
        Index ix = built("מסלול", root);
        Library lib = Library.forTests(new ArrayList<Index>(Collections.singletonList(ix)));
        File tf = new File(home, "מבנה-מסלול.txt");
        tf.delete();

        // 1. העתקה עם שמירת מסלול, ויומן
        File dest = new File(root, "גיבוי/נוכחי");
        File jf = new File(root, "גיבוי/יומן/1.txt");
        {   // מבנה 194: היומן שואל את ההיתר — בלי היתר של שכבת הניהול הוא אינו נפתח ואינו כותב, והוא "נכשל"
            File no194 = new File(jf.getParentFile(), "בלי-היתר-194.txt");
            no194.delete();
            Permit.close();
            Journal j194;
            try {
                j194 = new Journal(no194, "בלי היתר");
                j194.close();
            } finally {
                Permit.open();
            }
            check("מבנה 194: יומן של העברה בלי היתר של שכבת הניהול — לא נפתח, לא נכתב, ו\"נכשל\"", j194.failed() && !no194.exists());
        }
        Journal j = new Journal(jf, "בדיקה");
        Physical.Result r = Physical.transferKeep(lib, src.getAbsolutePath(),
                P(src + "/א/1.pdf", src + "/ב/ג/2.pdf", src + "/3.txt"), P(src + "/ריקה/פנימית"), dest, true, null, null, false, j);
        j.close();
        lib.flush(5000);
        check("1.19.0: שמירת מסלול — הכול במקומו", r.failed == 0 && new File(dest, "א/1.pdf").length() == 10
                && new File(dest, "ב/ג/2.pdf").length() == 20 && new File(dest, "3.txt").isFile());
        check("1.19.0: שמירת מסלול — תיקייה ריקה", new File(dest, "ריקה/פנימית").isDirectory());
        check("1.19.0: שמירת מסלול — המקור נשאר", new File(src, "א/1.pdf").isFile());
        List<String[]> lines = Journal.read(jf);
        eq("1.19.0: יומן — שורה לכל קובץ", 3, lines.size());
        check("1.19.0: יומן — מסלול יחסי וגודל", lines.size() == 3 && lines.get(1)[0].equals("ב/ג/2.pdf") && lines.get(1)[1].equals("20"));
        String head = "";
        try {
            head = Files.readUtf8(jf);
        } catch (Exception e) {
            head = "";
        }
        check("1.19.0: יומן — כותרת", head.startsWith("# בדיקה"));

        // 2. שוב לאותו יעד: תיקיות קיימות ממוזגות בלי שאלה; קובץ תפוס בלי שואל — כישלון, לא נדרס
        mk(new File(src, "א"), "4.pdf", 4);
        r = Physical.transferKeep(lib, src.getAbsolutePath(), P(src + "/א/1.pdf", src + "/א/4.pdf"), P(src + "/ריקה/פנימית"),
                dest, true, null, null, false, null);
        check("1.19.0: שוב — רק הקובץ התפוס נכשל", r.failed == 1 && new File(dest, "א/4.pdf").isFile());
        // פריט מחוץ לשורש — כישלון
        r = Physical.transferKeep(lib, src + "/א", P(src + "/3.txt"), NONE, dest, true, null, null, false, null);
        check("1.19.0: מחוץ לשורש — כישלון", r.failed == 1 && r.note.equals(Words.Disk.NOT_UNDER_ROOT));

        // 3. העברה עם שמירת מסלול: "נוסף" עובר לפי המקור המדויק, גם לשני קבצים באותו גודל ותאריך
        File m = new File(root, "להעביר");
        File x1 = mk(new File(m, "ש1"), "x.bin", 50), x2 = mk(new File(m, "ש2"), "y.bin", 50);
        x1.setLastModified(1600000000000L);
        x2.setLastModified(1600000000000L);
        lib.fileAdded(x1);
        lib.fileAdded(x2);
        lib.flush(5000);
        lib.find(x1.getAbsolutePath()).rec.added = 1111L;
        lib.find(x2.getAbsolutePath()).rec.added = 2222L;
        File hist = new File(root, "גיבוי/היסטוריה/0001");
        r = Physical.transferKeep(lib, m.getAbsolutePath(), P(x2.getAbsolutePath(), x1.getAbsolutePath()), NONE, hist, false,
                null, null, false, null);
        lib.flush(5000);
        Library.Hit h1 = lib.find(hist + "/ש1/x.bin"), h2 = lib.find(hist + "/ש2/y.bin");
        check("1.19.0: העברה — במקום", r.failed == 0 && h1 != null && h2 != null);
        check("1.19.0: \"נוסף\" עובר לפי המקור המדויק", h1 != null && h2 != null && h1.rec.added == 1111L && h2.rec.added == 2222L);
        check("1.19.0: העברה — תיקיות המקור שהתרוקנו נמחקו, השורש נשאר", !new File(m, "ש1").exists() && !new File(m, "ש2").exists() && m.isDirectory());
        // העברת תיקייה שלמה באותו כונן (שינוי שם) — גם כן
        File box = new File(root, "ארגז");
        File b1 = mk(box, "ב1.bin", 70);
        b1.setLastModified(1600000000000L);
        lib.fileAdded(b1);
        lib.flush(5000);
        lib.find(b1.getAbsolutePath()).rec.added = 3333L;
        r = Physical.transfer(lib, NONE, P(box.getAbsolutePath()), hist, false, null, null, false);
        lib.flush(5000);
        Library.Hit hb = lib.find(hist + "/ארגז/ב1.bin");
        check("1.19.0: העברת תיקייה — \"נוסף\" עובר", hb != null && hb.rec.added == 3333L);

        // 4. קובץ פקודות: COPY /S ו־/מסלול, /יומן:, %תאריך%, %מזהה-כרטיס%
        Map<String, String> vars = Parse.vars(root, root.getAbsolutePath(), "/storage/ABCD-1234");
        check("1.19.0: %תאריך% — יום.חודש.שנה שעה-דקה-שנייה", vars.get(Syntax.V_NOW).matches("\\d\\d\\.\\d\\d\\.\\d{4} \\d\\d-\\d\\d-\\d\\d"));
        eq("1.19.0: %מזהה-כרטיס%", "ABCD-1234", vars.get(Syntax.V_CARD_ID));
        check("1.19.0: בלי כרטיס — בלי מזהה", Parse.vars(root, root.getAbsolutePath(), null).get(Syntax.V_CARD_ID) == null);
        vars = Parse.vars(root, root.getAbsolutePath(), null);
        String now = vars.get(Syntax.V_NOW);
        Batch.Plan p = Batch.plan("COPY מקור \"גיבוי2\\%תאריך%\" /S /יומן:\"גיבוי2\\יומן\\%תאריך%.txt\"", root, vars);
        check("1.19.0: COPY /S — בלי שגיאה", p.errors == 0 && p.steps.size() == 1);
        Batch.Step st = p.steps.get(0);
        check("1.19.0: COPY /S — שומר מסלול בתיאור", st.describe != null && st.describe.contains(Words.Cmd.KEEP_PATH.trim()));
        eq("1.19.0: COPY /S — כל הקבצים", 4, st.files.size());
        Batch.run(p, lib, null, null);
        lib.flush(5000);
        File g2 = new File(root, "גיבוי2/" + now);
        check("1.19.0: COPY /S — בוצע", new File(g2, "ב/ג/2.pdf").isFile() && new File(g2, "ריקה/פנימית").isDirectory());
        eq("1.19.0: /יומן: — נכתב בשם עם %תאריך%", 4, Journal.read(new File(root, "גיבוי2/יומן/" + now + ".txt")).size());
        // תבנית עם /מסלול — רק pdf, בכל העומק, בלי תיקיות ריקות
        p = Batch.plan("COPY מקור\\*.pdf גיבוי3 /מסלול", root, vars);
        check("1.19.0: /מסלול עם תבנית", p.errors == 0 && p.steps.get(0).files.size() == 3 && p.steps.get(0).dirs.isEmpty());
        // קובץ זמני של העתקה — לא נכלל
        mk(new File(src, "א"), FileCopy.partial(new File(src, "א/5.pdf")).getName(), 3);
        p = Batch.plan("COPY מקור גיבוי4 /S /A:H", root, vars);
        boolean partial = false;
        for (String f : p.steps.get(0).files) if (FileCopy.isPartial(new File(f).getName())) partial = true;
        check("1.19.0: קובץ זמני — לא נכלל גם עם /A:H", !partial);
        // לתוך עצמו — שגיאה; /יומן: ב־DEL — שגיאה; /יומן: בלי שם — שגיאה
        check("1.19.0: COPY /S לתוך עצמו — שגיאה", Batch.plan("COPY מקור מקור\\בפנים /S", root, vars).errors == 1);
        check("1.19.0: /יומן: ב־DEL — שגיאה", Batch.plan("DEL מקור\\*.txt /יומן:x.txt", root, vars).errors == 1);
        check("1.19.0: /יומן: בלי שם — שגיאה", Batch.plan("COPY מקור\\3.txt גיבוי2 /יומן:", root, vars).errors == 1);
        // /תפוס:לדלג עם שמירת מסלול — מה שכבר ביעד יוצא מהתכנון
        mk(src, "חדש.pdf", 1);
        p = Batch.plan("COPY מקור \"גיבוי2\\%תאריך%\" /S /תפוס:לדלג", root, vars);
        eq("1.19.0: /S /תפוס:לדלג — רק החדש", 1, p.steps.get(0).files.size());
        // MOVE /S — מעביר, ומשאיר את שורש המקור
        File mv = new File(root, "מעבר");
        mk(new File(mv, "פ"), "ק.pdf", 9);
        p = Batch.plan("MOVE מעבר יעד-מעבר /S", root, vars);
        Batch.run(p, lib, null, null);
        lib.flush(5000);
        check("1.19.0: MOVE /S", new File(root, "יעד-מעבר/פ/ק.pdf").isFile() && !new File(mv, "פ").exists() && mv.isDirectory());
    }

    private static void v1180() {
        File root = drive("הגנות");
        File src = new File(root, "מקור");
        File a = mk(src, "א.pdf", 30);
        a.setLastModified(1500000000000L);
        File dest = new File(root, "יעד");
        dest.mkdirs();

        // 1. שם זמני: אחרי הצלחה — אין קובץ זמני, התאריך נשמר
        File d1 = new File(dest, "א.pdf");
        check("1.18.0: העתקה — הצליחה", FileCopy.copyFile(a, d1) && d1.length() == 30);
        check("1.18.0: העתקה — אין קובץ זמני", !FileCopy.partial(d1).exists());
        eq("1.18.0: העתקה — התאריך נשמר", 1500000000000L, d1.lastModified());
        check("1.18.0: השם הזמני — מוסתר ומזוהה", FileCopy.partial(d1).getName().startsWith(".")
                && FileCopy.isPartial(FileCopy.partial(d1).getName()) && !FileCopy.isPartial("א.pdf"));
        // שארית מהפסקה קודמת — נמחקת, ולא נשאר קובץ בשם האמיתי לפני הסוף
        File d2 = new File(dest, "ב.pdf");
        File left = mk(dest, FileCopy.partial(d2).getName(), 7);
        check("1.18.0: שארית — קיימת לפני", left.isFile() && !d2.exists());
        check("1.18.0: שארית — ההעתקה מצליחה ומנקה", FileCopy.copyFile(a, d2) && d2.length() == 30 && !left.exists());
        // יעד תפוס — לא נוגעים
        check("1.18.0: יעד תפוס — לא נדרס", !FileCopy.copyFile(mk(src, "ג.pdf", 5), d1) && d1.length() == 30);
        // מקור שאינו קובץ — אין שארית זמנית
        File d3 = new File(dest, "ד.pdf");
        check("1.18.0: מקור חסר — נכשל בלי שארית", !FileCopy.copyFile(new File(src, "אין.pdf"), d3)
                && !d3.exists() && !FileCopy.partial(d3).exists());
        // שם ארוך מאוד — השם הזמני מתקצר
        StringBuilder lng = new StringBuilder();
        for (int i = 0; i < 120; i++) lng.append('ש');
        File longDst = new File(dest, lng + ".pdf");
        try {
            check("1.18.0: שם ארוך — השם הזמני עד 255 בתים", FileCopy.partial(longDst).getName().getBytes("UTF-8").length <= 255);
        } catch (Exception e) {
            check("1.18.0: שם ארוך", false);
        }

        // 2. רק הפריטים העליונים
        File box = new File(src, "ארגז");
        mk(new File(box, "פנים"), "1.pdf", 11);
        mk(box, "2.pdf", 22);
        List<File> top = Mover.topOnly(P(box + "/2.pdf", box + "/פנים/1.pdf"), P(box.getAbsolutePath(), box.getAbsolutePath(), box + "/פנים"));
        check("1.18.0: רק העליון — תיקייה אחת", top.size() == 1 && top.get(0).getAbsolutePath().equals(box.getAbsolutePath()));
        List<File> sib = Mover.topOnly(P(src + "/ארגז2/x.pdf"), P(box.getAbsolutePath()));
        eq("1.18.0: שם דומה (ארגז / ארגז2) — אינו צאצא", 2, sib.size());

        Index ix = built("הגנות", root);
        Library lib = Library.forTests(new ArrayList<Index>(Collections.singletonList(ix)));
        File tf = new File(home, "מבנה-הגנות.txt");
        tf.delete();
        File cp = new File(root, "העתק");
        cp.mkdirs();
        Physical.Result r = Physical.transfer(lib, P(box + "/2.pdf", box + "/פנים/1.pdf"), P(box.getAbsolutePath()), cp, true, null, null, false);
        lib.flush(5000);
        check("1.18.0: העתקת תיקייה וילדיה — בלי כישלון", r.failed == 0 && new File(cp, "ארגז/פנים/1.pdf").isFile());
        check("1.18.0: העתקת תיקייה וילדיה — בלי עותק שטוח", !new File(cp, "2.pdf").exists() && !new File(cp, "1.pdf").exists());
        File mv = new File(root, "העבר");
        mv.mkdirs();
        r = Physical.transfer(lib, P(box + "/2.pdf"), P(box.getAbsolutePath()), mv, false, null, null, false);
        lib.flush(5000);
        check("1.18.0: העברת תיקייה וקובץ שבה — בלי \"לא נמצא\"", r.failed == 0 && new File(mv, "ארגז/2.pdf").isFile() && !box.exists());

        // 3. גודל מרבי ביעד (FAT32)
        File big = mk(src, "גדול.bin", 100);
        File small = mk(src, "קטן.bin", 10);
        File fat = new File(root, "כרטיס");
        fat.mkdirs();
        DiskInfo.maxFileForTests = 50;
        try {
            r = Physical.transfer(lib, P(big.getAbsolutePath(), small.getAbsolutePath()), NONE, fat, true, null, null, false);
        } finally {
            DiskInfo.maxFileForTests = 0;
        }
        check("1.18.0: קובץ גדול מהמותר — נכשל, השאר הועתק", r.failed == 1 && !new File(fat, "גדול.bin").exists()
                && new File(fat, "קטן.bin").isFile() && !FileCopy.partial(new File(fat, "גדול.bin")).exists());
        check("1.18.0: הודעה — 4GB", r.note.contains("4GB"));
        // סוג מערכת הקבצים מ-/proc/mounts
        List<String[]> m = new ArrayList<String[]>();
        m.add(new String[]{"/", "ext4"});
        m.add(new String[]{"/storage/emulated", "fuse"});
        m.add(new String[]{"/storage/ABCD-1234", "fuse"});
        m.add(new String[]{"/mnt/media_rw/ABCD-1234", "vfat"});
        m.add(new String[]{"/storage/EF01-2345", "fuse"});
        m.add(new String[]{"/mnt/media_rw/EF01-2345", "exfat"});
        eq("1.18.0: כרטיס FAT32", "vfat", DiskInfo.fsTypeIn(m, "/storage/ABCD-1234/גיבוי/נוכחי"));
        eq("1.18.0: כרטיס exFAT", "exfat", DiskInfo.fsTypeIn(m, "/storage/EF01-2345"));
        eq("1.18.0: פנימי", "fuse", DiskInfo.fsTypeIn(m, "/storage/emulated/0/Download"));
        eq("1.18.0: נקודה דומה אינה תואמת", "ext4", DiskInfo.typeAt(m, "/storage/emulatedX"));
    }



    /** 169: באילו רשימות — גם דרך רשימה חכמה פנימית. 170: קריאה מהזיכרון; המרת הכלים הישנים. */
    private static void step170() {
        // --- 170: קריאה פעם אחת ---
        File c = new File(home, "מטמון-170.txt");
        try {
            Files.writeAtomic(c, "א");
            eq("170: מה שנכתב — נקרא", "א", Files.readUtf8(c));
            Files.writeAtomic(c, "ב");
            eq("170: כתיבה שנייה — הערך החדש", "ב", Files.readUtf8(c));
            java.io.FileOutputStream o = new java.io.FileOutputStream(c);      // שינוי מבחוץ, בלי writeAtomic
            o.write("גגג".getBytes("UTF-8"));
            o.close();
            eq("170: שינוי מבחוץ — נקרא מהכונן", "גגג", Files.readUtf8(c));
            c.setLastModified(System.currentTimeMillis() - 60000);
            Files.readUtf8(c);                                                // יציב — נזכר
            o = new java.io.FileOutputStream(c);
            o.write("דדד".getBytes("UTF-8"));                                 // אותו גודל, תאריך חדש
            o.close();
            eq("170: אותו גודל ותאריך אחר — נקרא שוב", "דדד", Files.readUtf8(c));
            File d = new File(home, "מטמון-170-מקור.txt");
            Files.writeAtomic(d, "ההה");
            c.setLastModified(System.currentTimeMillis() - 60000);
            Files.readUtf8(c);
            long m = c.lastModified();
            Files.copy(d, c);
            c.setLastModified(m);                                             // גם אם התאריך נשאר (שחזור)
            eq("170: העתקה על הקובץ — נקרא שוב", "ההה", Files.readUtf8(c));
            d.delete();
        } catch (Exception e) {
            check("170: מטמון — " + e, false);
        } finally {
            c.delete();
        }
        boolean threw = false;
        try {
            Files.readUtf8(c);
        } catch (java.io.IOException e) {
            threw = true;
        }
        check("170: קובץ שנמחק — לא מהזיכרון", threw);

        // --- 169 ---
        File root = drive("פנימי-169");
        Index ix = built("פנימי-169", root);
        Library lib = Library.forTests(new ArrayList<Index>(Collections.singletonList(ix)));
        File f = new File(home, "מבנה-169.ini"), tf = new File(home, "מבנה-169.txt");
        f.delete();
        tf.delete();
        try {
            String pdf = new File(root, "Download/מוגה.pdf").getAbsolutePath();
            String yoga = new File(root, "Download/יוגה למתחילים.pdf").getAbsolutePath();
            Library notReady = Library.forTests(new ArrayList<Index>(Collections.singletonList(ix)));
            notReady.ready = false;
        } catch (Exception e) {
            check("169: " + e, false);
        } finally {
        }

    }

    /** 175: תיקיות ריקות — רק החיצונית, קובץ מוסתר לבד לא נחשב, מקורות ומילים. */
    private static void empty175() {
        File root = new File(home, "ריקות-175");
        wipe(root);
        new File(root, "ריקה").mkdirs();
        new File(root, "ריקה עם ריקות/פנימית/עוד").mkdirs();
        new File(root, "ריקה עם ריקות/שנייה").mkdirs();
        mk(new File(root, "רק מוסתר"), ".nomedia", 0);
        mk(new File(root, "הגדרות של אפליקציה"), ".config", 3);
        new File(root, "Android/media/com.xiaomi.bluetooth").mkdirs();         // 1.7.69: data ו־obb אינם באינדקס
        mk(new File(root, "מלאה/ריקה בתוך מלאה/x"), "קובץ.pdf", 10);
        new File(root, "מלאה/ריקה בתוך מלאה/y").mkdirs();
        new File(root, "Download/ישנה").mkdirs();
        mk(new File(root, "Download"), "הורדה.pdf", 5);
        new File(root, ".מוסתרת").mkdirs();
        new File(root, Index.TRASH + "/נמחקה").mkdirs();
        Index ix = built("ריקות-175", root);
        Library lib = Library.forTests(new ArrayList<Index>(Collections.singletonList(ix)));
        Library.Result r = lib.search(Cond.EMPTY, false, true);
        java.util.Map<String, Integer> got = new java.util.HashMap<String, Integer>();
        for (Library.Hit h : r.hits) got.put(h.name(), h.innerEmpty);
        check("175: ריקה — נמצאה", got.containsKey("ריקה") && got.get("ריקה") == 0);
        eq("175: ריקה עם ריקות — רק החיצונית, ו־3 בתוכה", Integer.valueOf(3), got.get("ריקה עם ריקות"));
        check("175: הפנימיות — לא בנפרד", !got.containsKey("פנימית") && !got.containsKey("עוד") && !got.containsKey("שנייה"));
        check("175: רק .nomedia — ריקה", got.containsKey("רק מוסתר"));
        check("175: קובץ מוסתר אחר — לא ריקה (1.7.65)", !got.containsKey("הגדרות של אפליקציה"));
        check("1.7.69: Android בשורש — לעולם לא ריקה; מה שבתוכה — כן", !got.containsKey("Android") && got.containsKey("media"));
        check("175: מלאה — לא; ריקה בתוך מלאה — לא (יש בה קובץ)", !got.containsKey("מלאה") && !got.containsKey("ריקה בתוך מלאה"));
        check("175: ריקה שבתוך תיקייה שאינה ריקה — כן", got.containsKey("y") && got.containsKey("ישנה"));
        check("175: מוסתרת וסל — לא", !got.containsKey(".מוסתרת") && !got.containsKey("נמחקה") && !got.containsKey(Index.TRASH));
        check("175: לא קבצים", !got.containsKey(".nomedia") && !got.containsKey("קובץ.pdf"));
        new File(root, "ריקה2/בתוכה").mkdirs();
        Index ix2 = built("ריקות-175", root);
        java.util.Set<String> n2 = new java.util.HashSet<String>(names(Library.forTests(new ArrayList<Index>(Collections.singletonList(ix2))).search(Cond.EMPTY, false, true)));
        check("175: כל התיקייה ריקה — רק היא, לא מה שבתוכה", n2.contains("ריקה2") && !n2.contains("בתוכה"));
        String dl = Sources.toCond(Arrays.asList(new File(root, "Download").getAbsolutePath()));
        List<String> inDl = names(lib.search(dl + " " + Cond.EMPTY, false, true));
        eq("175: עם מקור — רק מה שבתוכו", Arrays.asList("ישנה"), inDl);
        eq("175: עם מילה — בשם התיקייה", Arrays.asList("ישנה"), names(lib.search(Cond.EMPTY + " ישנה", false, true)));
        check("175: סוג אינו חל על תיקייה", lib.search("סוג:pdf " + Cond.EMPTY, false, true).hits.size() == r.hits.size());
        check("175: התנאי מזוהה, ולחלונית ההגדרות", Cond.isEmptyDirs(Cond.EMPTY) && SmartLists.split("יוגה " + Cond.EMPTY)[0].contains(Cond.EMPTY)
                && SmartLists.split("יוגה " + Cond.EMPTY)[1].equals("יוגה"));
        check("175: לא \"לא הובן\"", lib.search(Cond.EMPTY, false, true).note.isEmpty());
        check("175: רשימה מוכנה \"תיקיות ריקות\"", SmartLists.PRESETS[SmartLists.PRESETS.length - 1][1].equals(Cond.EMPTY));
        wipe(root);
    }

    /** 177: סריקה ב־N קוראים — אותם קבצים ותיקיות כמו בקורא אחד, וההורה תמיד לפני הילד. */
    private static void parallel177() {
        File root = new File(home, "מקביל-177");
        wipe(root);
        for (int a = 0; a < 12; a++)
            for (int b = 0; b < 6; b++) {
                File d = new File(root, "א" + a + "/ב" + b + (b % 2 == 0 ? "/ג" : ""));
                for (int c = 0; c < 5; c++) mk(d, "קובץ " + c + ".pdf", c + 1);
            }
        new File(root, "ריקה/בתוכה").mkdirs();
        mk(new File(root, ".מוסתרת"), "x.txt", 1);
        Index one = new Index("מקביל-177", "ניסוי", root.getAbsolutePath());
        long t1 = System.nanoTime();
        one.scan(null);
        t1 = System.nanoTime() - t1;
        java.util.Set<String> want = all177(one);
        for (int n : new int[]{2, 4, 16}) {
            Index many = new Index("מקביל-177", "ניסוי", root.getAbsolutePath());
            many.scan(null, n);
            check("177: " + n + " קוראים — אותם קבצים ותיקיות (" + want.size() + ")", all177(many).equals(want));
            boolean order = true;
            for (int k = 0; k < many.dirs.size(); k++) if (many.dirs.get(k).parent >= k) order = false;
            check("177: " + n + " קוראים — ההורה לפני הילד", order);
            boolean hid = false;
            for (Index.Dir d : many.dirs) if (d.name.equals(".מוסתרת") && d.hidden) hid = true;
            check("177: " + n + " קוראים — סימון מוסתרים", hid);
        }
        // 1.7.73: רענון — התאריכים ב־8 קוראים; התוצאה כמו סריקה חדשה
        Index ref = new Index("מקביל-177", "ניסוי", root.getAbsolutePath());
        ref.scan(null);
        try {
            mk(new File(root, "א3/ב1"), "חדש.pdf", 9);                               // קובץ חדש
            File gone = new File(root, "א5/ב0/ג");
            for (File f : gone.listFiles()) f.delete();
            gone.delete();                                                       // תיקייה שנמחקה
            mk(new File(root, "א7/חדשה/פנימית"), "x.pdf", 2);                        // תיקייה חדשה, עם תת־תיקייה
            for (String d : new String[]{"א3/ב1", "א5/ב0", "א7"})
                new File(root, d).setLastModified(System.currentTimeMillis() + 5000);   // שעון גס — שהשינוי ייראה
        } catch (Exception e) {
            check("1.7.73: הכנה — " + e, false);
        }
        int ch = ref.refresh(8);
        Index fresh = new Index("מקביל-177", "ניסוי", root.getAbsolutePath());
        fresh.scan(null);
        java.util.Set<String> got = new java.util.HashSet<String>(), exp = new java.util.HashSet<String>();
        for (int k = 0; k < ref.dirs.size(); k++) if (!ref.dirs.get(k).gone) got.add(ref.dirPath(k));
        for (Index.Rec r : ref.files) if (!r.gone) got.add(ref.path(r));
        for (int k = 0; k < fresh.dirs.size(); k++) exp.add(fresh.dirPath(k));
        for (Index.Rec r : fresh.files) exp.add(fresh.path(r));
        check("1.7.73: רענון ב־8 קוראים — שינויים נמצאו (" + ch + ")", ch >= 3);
        check("1.7.73: רענון ב־8 קוראים — כמו סריקה חדשה", got.equals(exp));
        wipe(root);
    }

    private static void quick181() {
        File root = drive("ח181");
        Index ix = built("ח181", root);
        Library lib = Library.forTests(new ArrayList<Index>(Collections.singletonList(ix)));
        // --- "לא" בתנאים ---
        eq("181: בלי \"לא\" — 4 קובצי PDF", 4, count(ix, "סוג:pdf"));
        eq("181: \"-מוגה\" — מוציא קבצים שבשמם \"מוגה\"", 3, count(ix, "סוג:pdf -מוגה"));
        eq("181: \"-סוג:epub\" — מוציא את ה־epub", count(ix, "תיקייה:download") - 1, count(ix, "תיקייה:download -סוג:epub"));
        String sub = new File(root, "Download/פאולה").getAbsolutePath();
        eq("181: \"-מקור:«…»\" — מוציא תיקייה עם כל מה שבתוכה", 3, count(ix, "סוג:pdf -מקור:" + Cond.quote(sub)));
        check("181: \"-מקור\" עם רווח במסלול — נקרא", Cond.parse("-מקור:«/a b/c»").hasNot() && Cond.parse("-מקור:«/a b/c»").unknown.isEmpty());
        check("181: \"לא\" בלבד אינו חיפוש", lib.search("-סוג:ini", false, true).hits.isEmpty());
        List<String> got = names(lib.search("מוגה -epub", false, true));
        check("181: חיפוש + \"לא\" — " + got, got.contains("מוגה.pdf") && !got.contains("ספר מוגה.epub"));
        check("181: \"לא\" — גם על תיקיות בתוצאות", !names(lib.search("פאולה -מקור:" + Cond.quote(sub), false, true)).contains("פאולה")
                && names(lib.search("פאולה", false, true)).contains("פאולה"));
        check("181: \"בלי סוג\" לא מוציא תיקייה", Cond.parse("-סוג:pdf").matchesDir(ix, ix.dirId(sub)));
        check("181: מינוס בודד, ושני מינוסים — מילה", !Cond.parse("-").hasNot() && !Cond.parse("--x").hasNot());
        String[] sp = SmartLists.split("סוג:pdf -סוג:ini -מטמון");
        check("181: \"לא\" נשמר בשורת ההקלדה ולא הולך לאיבוד (" + sp[1] + ")", sp[1].contains("-סוג:ini") && sp[1].contains("-מטמון"));
        // --- הכפתורים ---
        List<String> parts = Quick.parts("סוג:pdf -מקור:«/a b» -מטמון");
        eq("181: רכיבי הכפתור — שורה לכל אחד", 3, parts.size());
        eq("181: רכיב עם רווח — במרכאות", "-מקור:«/a b»", parts.get(1));
        eq("181: הלוך וחזור", "סוג:pdf -מקור:«/a b» -מטמון", Quick.join(parts));
        check("181: הפיכה — חייב ↔ בלי", Quick.flip("סוג:pdf").equals("-סוג:pdf") && Quick.flip("-סוג:pdf").equals("סוג:pdf"));
        List<Quick.Button> bs = Arrays.asList(new Quick.Button("א", "-סוג:ini"), new Quick.Button("ב", "סוג:ספרים"), new Quick.Button("ג", "תאריך:חודש"));
        eq("181: התנאי — רק הדולקים, לפי הסדר", "-סוג:ini תאריך:חודש",
                Quick.cond(bs, new java.util.LinkedHashSet<String>(Arrays.asList("ג", "א"))));
        eq("181: המצב — הלוך וחזור", "[א, ג]", Quick.parse(Quick.state(new java.util.LinkedHashSet<String>(Arrays.asList("א", "ג")))).toString());
        check("181: \"הכול כבוי\" נשמר כמשהו (לא חוזר לברירת המחדל)", Quick.parse(Quick.SEP).isEmpty());
        List<Quick.Button> rb = Arrays.asList(new Quick.Button("א", "-סוג:ini"), new Quick.Button("מעבר", Quick.RUN + "/x/מעבר.vbat"));
        check("216: כפתור הרצה — מזוהה", Quick.isRun(rb.get(1)) && !Quick.isRun(rb.get(0)));
        eq("216: כפתור הרצה לא נכנס לתנאי הסינון", "-סוג:ini",
                Quick.cond(rb, new java.util.LinkedHashSet<String>(Arrays.asList("א", "מעבר"))));
        List<Quick.Button> all = Quick.all();
        check("181: ברירת המחדל — ארבעה כפתורים, בלי שורת \"דולקים בהתחלה\"", all.size() == 4 && !all.get(0).cond.isEmpty());
        check("181: \"בלי תיקיית התוכנה\" — דולק בהתחלה", Quick.startOn().contains(Words.Quick.APP));
        check("181: \"בלי תיקיית התוכנה\" — תנאי שהובן", Cond.parse(all.get(0).cond).hasNot() && Cond.parse(all.get(0).cond).unknown.isEmpty());
        check("181: \"רק ספרים\" — הקבוצה מהסוגים", count(ix, all.get(2).cond) == count(ix, "סוג:pdf|epub|mobi|azw3|djvu"));
        java.util.Collections.swap(all, 0, 1);
        all.get(0).name = "בלי ini שלי";
        Quick.save(all);
        List<Quick.Button> again = Quick.all();
        check("181: שמירה — שם, סדר, ו\"דולקים בהתחלה\" נשאר", again.size() == 4 && again.get(0).name.equals("בלי ini שלי")
                && again.get(1).name.equals(Words.Quick.APP) && Quick.startOn().contains(Words.Quick.APP));
        wipe(root);
    }

    /** 192: יומן המחיקות — הוספה בסוף; יומן התקלות — נשאר הסוף; הניסוח של הסרת רשימות. */
    private static void small192() {
        File root = new File(home, "קטנים-192");
        wipe(root);
        File tr = new File(root, Index.TRASH);
        tr.mkdirs();
        Trash.logDeleted(new File(tr, "א.pdf"), "/x/א.pdf");
        Trash.logDeleted(new File(tr, "ב.pdf"), "/x/ב.pdf");
        String log = "";
        try { log = Files.readUtf8(new File(tr, Trash.LOG)); } catch (Exception e) { }
        check("192: יומן המחיקות — שתי השורות, בסדר", log.equals("א.pdf\t/x/א.pdf\nב.pdf\t/x/ב.pdf\n"));
        File big = new File(root, "יומן.txt");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 20000; i++) sb.append("שורה ").append(i).append('\n');
        try { Files.writeAtomic(big, sb.toString()); } catch (Exception e) { throw new RuntimeException(e); }
        Log.keepTail(big);
        String t = "";
        try { t = Files.readUtf8(big); } catch (Exception e) { }
        check("192: יומן התקלות — נשאר הסוף, מתחילת שורה", big.length() <= Log.KEEP + 200 && t.endsWith("שורה 19999\n")
                && t.startsWith(Words.LogW.TRIMMED + "\nשורה "));
        wipe(root);
    }

    private static File mkBytes(File dir, String name, byte[] b) {
        try {
            dir.mkdirs();
            File f = new File(dir, name);
            java.io.FileOutputStream o = new java.io.FileOutputStream(f);
            o.write(b);
            o.close();
            return f;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] pattern(int n, int seed) {
        byte[] b = new byte[n];
        for (int i = 0; i < n; i++) b[i] = (byte) (i * 31 + seed);
        return b;
    }

    /** 190: חתימה, הקובץ שלה, העבודה ברקע, והאיתור — גם בשם אחר, גם במקום אחר לגמרי. */
    private static void sig190() {
        File root = new File(home, "חתימות-190");
        wipe(root);
        // --- החישוב ---
        byte[] big = pattern(200 * 1024, 1);
        File a = mkBytes(new File(root, "א"), "גדול.pdf", big);
        File a2 = mkBytes(new File(root, "ב"), "עותק.pdf", big);
        String sa = Sig.of(a);
        check("190: חתימה — בצורה הקבועה", sa != null && sa.startsWith(Sig.VER) && sa.length() == Sig.VER.length() + 16);
        eq("190: אותו תוכן — אותה חתימה (גם בשם ובמקום אחר)", sa, Sig.of(a2));
        byte[] mid = big.clone();
        mid[100 * 1024] ^= 1;
        check("190: שינוי בית אחד באמצע — חתימה אחרת", !sa.equals(Sig.of(mkBytes(new File(root, "ג"), "אמצע.pdf", mid))));
        byte[] tail = big.clone();
        tail[tail.length - 1] ^= 1;
        check("190: שינוי בסוף — חתימה אחרת", !sa.equals(Sig.of(mkBytes(new File(root, "ג"), "סוף.pdf", tail))));
        byte[] zeros = new byte[big.length];
        check("190: העתקה שנקטעה (אותו גודל, אפסים) — חתימה אחרת", !sa.equals(Sig.of(mkBytes(new File(root, "ג"), "קטוע.pdf", zeros))));
        File small = mkBytes(new File(root, "א"), "קטן.txt", pattern(1000, 2));
        byte[] sm = pattern(1000, 2);
        sm[500] ^= 1;
        check("190: קובץ קטן — כולו נקרא", !Sig.of(small).equals(Sig.of(mkBytes(new File(root, "ג"), "קטן2.txt", sm))));
        check("190: קובץ שאינו — null", Sig.of(new File(root, "אין.pdf")) == null);

        // --- הקובץ ---
        File sf = new File(home, "חתימות-190.txt");
        Sig.testFile = sf;
        Sig.forget();
        File tf = new File(home, "מבנה-190.txt");
        try {
            Sig.put("/x/קובץ.pdf", 5, 6, "2:abc");
            check("190: נרשמה — עם הגודל והתאריך", Sig.stored("/x/קובץ.pdf") != null && Sig.stored("/x/קובץ.pdf").sig.equals("2:abc")
                    && Sig.stored("/x/קובץ.pdf").size == 5 && Sig.stored("/x/קובץ.pdf").mtime == 6);
            Sig.save();
            Sig.forget();
            check("190: נשמר ונקרא", Sig.stored("/x/קובץ.pdf") != null && Sig.stored("/x/קובץ.pdf").sig.equals("2:abc"));

            // --- המצב והעבודה ברקע ---
            File gone = mkBytes(new File(root, "ישן"), "נעלם.pdf", pattern(70 * 1024, 3));
            File gone2 = mkBytes(new File(root, "ישן"), "שם ישן.pdf", pattern(80 * 1024, 4));
            File gone3 = mkBytes(new File(root, "ישן"), "שונה.pdf", pattern(90 * 1024, 5));
            Files.writeAtomic(tf, "#1 ספרים\n" + a.getAbsolutePath() + " | " + a.length() + "\n"
                    + gone.getAbsolutePath() + " | " + gone.length() + "\n"
                    + gone2.getAbsolutePath() + " | " + gone2.length() + "\n"
                    + gone3.getAbsolutePath() + " | " + gone3.length() + "\n"
                    + "/storage/אחר/בלי חתימה.pdf | 1234\n"
                    + "/storage/אחר/אין בכלל.pdf | 999\n");

            // --- האיתור: הקבצים עברו למקום אחר לגמרי ---
            File moved = new File(new File(root, "חדש/מקום אחר"), "נעלם.pdf");
            moved.getParentFile().mkdirs();
            check("190: (הכנה) העברה", gone.renameTo(moved));
            File renamed = new File(new File(root, "חדש"), "שם חדש לגמרי.pdf");
            check("190: (הכנה) העברה ושינוי שם", gone2.renameTo(renamed));
            byte[] changed = pattern(90 * 1024, 5);
            changed[45 * 1024] ^= 1;
            mkBytes(new File(root, "חדש"), "שונה.pdf", changed);
            check("190: (הכנה) המקור של \"שונה\" נעלם", gone3.delete());
            mkBytes(new File(root, "חדש/אחר"), "בלי חתימה.pdf", pattern(1234, 6));
            mkBytes(new File(root, "חדש/עוד"), "בלי חתימה.pdf", pattern(1234, 7));
            Index ix = built("חתימות-190", root);
            Library lib = Library.forTests(new ArrayList<Index>(Collections.singletonList(ix)));
            // --- כל הקבצים באינדקס ---
            int[] before = Sig.indexStatus(lib);
            check("190: באינדקס — עוד לא חתום", before[0] == 0 && before[1] > 5);
            int did = Sig.indexRound(lib);
            int[] after = Sig.indexStatus(lib);
            check("190: סבב על כל הקבצים — כולם חתומים", did > 0 && after[0] == after[1]);
            Index.Rec ra = ix.find(a.getAbsolutePath());
            eq("190: החתימה ברשומה — כמו של הקובץ", Sig.str(ra.sig), Sig.of(a));
            ix.save();
            Index back = Index.load(ix.key);
            eq("190: נשמרה באינדקס ונטענה", ra.sig, back.find(a.getAbsolutePath()).sig);
            eq("190: סבב שני — אין מה לחשב", 0, Sig.indexRound(lib));
            Index cp = ix.copy();
            File grow = mkBytes(new File(root, "א"), "גדול.pdf", pattern(201 * 1024, 1));
            cp.addFile(grow);
            eq("190: קובץ שהשתנה — החתימה מתאפסת", 0L, cp.find(grow.getAbsolutePath()).sig);
            File mv = new File(new File(root, "ב/עבר"), "עותק.pdf");
            mv.getParentFile().mkdirs();
            long sigA2 = cp.find(a2.getAbsolutePath()).sig;
            check("190: (הכנה) העברה", a2.renameTo(mv));
            cp.removeFile(a2.getAbsolutePath());
            cp.addFile(mv);
            eq("190: הועבר — החתימה עוברת (אותו גודל ותאריך)", sigA2, cp.find(mv.getAbsolutePath()).sig);
            Index fresh = built("חתימות-190", root);
            lib.carryAdded(cp, fresh);
            eq("190: בנייה מההתחלה — החתימה נשמרת", sigA2, fresh.find(mv.getAbsolutePath()).sig);
            // 193: חתימה שנכתבה בתמונה המוצגת אחרי שנוצר עותק — עוברת לעותק בהחלפה
            Index shown = built("חתימות-193", root);
            Library lib193 = Library.forTests(new ArrayList<Index>(Collections.singletonList(shown)));
            Index copy193 = shown.copy();
            Index.Rec r193 = shown.find(small.getAbsolutePath());
            r193.sig = 777L;
            lib193.swapBuilt(copy193);
            eq("193: חתימה מאחרי העותק — לא אבדה בהחלפה", 777L, copy193.find(small.getAbsolutePath()).sig);
            check("193: והתמונה המוצגת — העותק", lib193.published("חתימות-193") == copy193);
            // 195: שם / גודל / תוכן — כל השילובים, ובשמות הקודמים
            for (String dmode : new String[]{Words.Dup.NAME_SIZE, Words.Dup.SIZE_SIMILAR, Words.Dup.SIZE, Words.Dup.NAME,
                    Words.Dup.CONTENT, Words.Dup.NAME_CONTENT, Words.Dup.CONTENT_SIMILAR})
                eq("195: הלוך ושוב — " + dmode, dmode, DupMode.parse(dmode).text());
            check("195: ריק — שם וגודל (כמו קודם)", DupMode.parse("").text().equals(Words.Dup.NAME_SIZE));
            check("195: תוכן — כולל גודל", new DupMode(DupMode.NAME_ANY, false, true).size);
            check("195: שם דומה בלי גודל — לא", !new DupMode(DupMode.NAME_SIMILAR, false, false).valid());
            check("195: כלום — לא", !new DupMode(DupMode.NAME_ANY, false, false).valid());
            check("195: שם זהה לבד — כן", new DupMode(DupMode.NAME_SAME, false, false).valid());
            check("195: ההסבר — שם זהה ותוכן", Words.DupT.explain(1, true, true).contains("אותו שם בדיוק") && Words.DupT.explain(1, true, true).contains("אותו תוכן"));
            eq("190: הדומה במסלול", "/b/ספרים/x.pdf", Sig.closest("/a/ספרים/x.pdf", Arrays.asList("/c/x.pdf", "/b/ספרים/x.pdf")));
        } catch (Exception e) {
            check("190: " + e, false);
        } finally {
            Sig.testFile = null;
            Sig.forget();
        }
        wipe(root);
    }

    /** 189: "נוסף" — באינדקס, בהעברה, בבנייה מההתחלה, מהמאגר, בתנאי, בטופס ובמיון. */
    private static void added189() {
        File root = new File(home, "נוסף-189");
        wipe(root);
        File a = mk(new File(root, "ספרים"), "ישן.pdf", 10);
        mk(new File(root, ".מוסתר"), "סוד.txt", 11);
        Index ix = built("נוסף-189", root);
        check("189: בבנייה הראשונה — \"נוסף\" לא ידוע", rec(ix, "ישן.pdf").added == 0 && rec(ix, "סוד.txt").added == 0);
        long t0 = System.currentTimeMillis();
        File b = mk(new File(root, "ספרים"), "חדש.pdf", 20);
        Index cp = ix.copy();
        cp.addFile(b);
        check("189: קובץ שנוסף אחר כך — עכשיו", rec(cp, "חדש.pdf").added >= t0);
        check("189: הקיים — בלי שינוי", rec(cp, "ישן.pdf").added == 0);
        // העברה ושינוי שם — שומרים את "נוסף"
        rec(cp, "חדש.pdf").added = 12345L;
        File moved = new File(new File(root, "ארכיון"), "חדש.pdf");
        moved.getParentFile().mkdirs();
        check("189: (הכנה) העברה בכונן", b.renameTo(moved));
        cp.removeFile(b.getAbsolutePath());
        cp.addFile(moved);
        eq("189: הועבר — \"נוסף\" נשמר", 12345L, cp.find(moved.getAbsolutePath()).added);
        File renamed = new File(moved.getParentFile(), "שם אחר.pdf");
        check("189: (הכנה) שינוי שם", moved.renameTo(renamed));
        cp.syncDir(cp.dirId(moved.getParent()), moved.getParentFile());
        eq("189: שם שונה (גם בסנכרון התיקייה) — נשמר", 12345L, cp.find(renamed.getAbsolutePath()).added);
        File other = mk(new File(root, "ספרים"), "אחר.pdf", 30);
        cp.addFile(other);
        check("189: קובץ חדש אחר — עכשיו (לא \"ירש\")", cp.find(other.getAbsolutePath()).added >= t0);
        // שמירה וטעינה
        try {
            cp.save();
        } catch (Exception e) {
            check("189: שמירה — " + e, false);
        }
        Index back = Index.load(cp.key);
        check("189: נשמר ונטען", back != null && !back.noAdded && back.find(renamed.getAbsolutePath()).added == 12345L);
        // קובץ מגרסה 1 — נקרא, בלי "נוסף"
        try {
            java.io.DataOutputStream d = new java.io.DataOutputStream(new java.io.FileOutputStream(Paths.index("ישן-189")));
            d.writeInt(0x56464D31);
            d.writeInt(1);
            d.writeUTF(root.getAbsolutePath());
            d.writeUTF("ישן");
            d.writeLong(1L);
            d.writeInt(0);
            d.writeInt(1);
            d.writeInt(-1);
            d.writeLong(10);
            d.writeLong(a.lastModified());
            d.writeUTF("x.pdf");
            d.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Index v1 = Index.load("ישן-189");
        check("189: אינדקס מגרסה 1 — נקרא (בלי בנייה מחדש)", v1 != null && v1.files.size() == 1 && v1.noAdded
                && v1.files.get(0).added == 0 && v1.files.get(0).name.equals("x.pdf"));

        // בנייה מההתחלה: מהאינדקס הקודם, מהמאגר, ו"חדש"
        FakeMedia fm = new FakeMedia();
        Library lib = Library.forTests(new ArrayList<Index>(Collections.singletonList(cp)));
        lib.useMedia(fm);
        File c = mk(new File(root, "ספרים"), "מהמחשב.pdf", 40);
        File d2 = mk(new File(root, "ספרים"), "בלי מאגר.pdf", 50);
        fm.added.put(c.getAbsolutePath(), 777000L);
        fm.added.put(a.getAbsolutePath(), 555000L);
        Index fresh = built("נוסף-189", root);
        lib.carryAdded(cp, fresh);
        eq("189: בנייה מההתחלה — מהאינדקס הקודם", 12345L, fresh.find(renamed.getAbsolutePath()).added);
        eq("189: לא היה ידוע — מהמאגר (מתי הגיע למכשיר)", 555000L, fresh.find(a.getAbsolutePath()).added);
        eq("189: חדש, והמאגר מכיר — מהמאגר", 777000L, fresh.find(c.getAbsolutePath()).added);
        check("189: חדש, והמאגר אינו מכיר — עכשיו", fresh.find(d2.getAbsolutePath()).added >= t0);
        eq("189: מוסתר — נשאר לא ידוע", 0L, rec(fresh, "סוד.txt").added);
        Index first = built("נוסף-189", root);
        lib.carryAdded(null, first);
        check("189: בלי אינדקס קודם — מהמאגר, ומה שאינו מכיר: לא ידוע (לא \"חדש\")",
                first.find(c.getAbsolutePath()).added == 777000L && first.find(d2.getAbsolutePath()).added == 0);
        fm.fail = true;
        Index noMedia = built("נוסף-189", root);
        lib.carryAdded(null, noMedia);
        check("189: המאגר לא עונה — לא ידוע", noMedia.find(c.getAbsolutePath()).added == 0);
        fm.fail = false;
        // אינדקס מגרסה 1 — ממלאים מהמאגר פעם אחת
        Index old = built("נוסף-189", root);
        old.noAdded = true;
        Library lib2 = Library.forTests(new ArrayList<Index>(Collections.singletonList(old)));
        lib2.useMedia(fm);
        lib2.seedAdded();
        Library.Hit h = lib2.find(a.getAbsolutePath());
        check("189: מגרסה 1 — מולא מהמאגר", h != null && h.rec.added == 555000L);
        int asked = fm.askedAdded;
        lib2.seedAdded();
        eq("189: פעם אחת בלבד", asked, fm.askedAdded);

        // התנאי
        long now = System.currentTimeMillis();
        Index q = new Index("q", "q", root.getAbsolutePath());
        Index.Rec r = new Index.Rec();
        r.dir = -1;
        r.name = r.norm = r.loose = "x.pdf";
        r.mtime = now - 400L * 24 * 3600 * 1000;
        r.added = now - 3600 * 1000;
        check("189: נוסף:שבוע — נוסף לפני שעה, אף שהשתנה לפני שנה", Cond.parse("נוסף:שבוע").matches(q, r)
                && !Cond.parse("תאריך:שבוע").matches(q, r));
        check("189: נוסף:<שבוע — לא", !Cond.parse("נוסף:<שבוע").matches(q, r));
        // מבנה 95: תאריך מדויק הוא יום שלם; תאריך שאינו קיים — לא מובן; גודל עם "="
        {
            Index.Rec d95 = new Index.Rec();
            d95.dir = -1;
            d95.name = d95.norm = d95.loose = "y.pdf";
            java.util.Calendar c95 = java.util.Calendar.getInstance();
            c95.clear();
            c95.set(2026, 8, 1, 15, 30);                                     // 1.9.2026 בשלוש וחצי
            d95.mtime = c95.getTimeInMillis();
            d95.size = 10L * 1048576;
            String[][] w95 = {                                               // {תנאי, האם הקובץ מ־1.9.2026 בצהריים עונה}
                    {"תאריך:<=1.9.2026", "1"}, {"תאריך:<1.9.2026", "0"}, {"תאריך:>=1.9.2026", "1"}, {"תאריך:>1.9.2026", "0"},
                    {"תאריך:1.9.2026", "1"}, {"תאריך:<=31.8.2026", "0"}, {"תאריך:>31.8.2026", "1"}, {"תאריך:<2.9.2026", "1"},
                    {"תאריך:<=2026", "1"}, {"תאריך:>2026", "0"}, {"תאריך:>2025", "1"}, {"תאריך:2026-09-01", "1"}, {"תאריך:<=2026-08-31", "0"},
                    {"גודל:>=10MB", "1"}, {"גודל:>10MB", "0"}, {"גודל:<=10MB", "1"}, {"גודל:<10MB", "0"}};
            StringBuilder bad95 = new StringBuilder();
            for (String[] w : w95) {
                Cond k95 = Cond.parse(w[0]);
                boolean want = w[1].equals("1"), got = k95.unknown.isEmpty() && k95.matches(q, d95);
                if (!k95.unknown.isEmpty() || got != want) bad95.append(" [").append(w[0]).append(" — ").append(k95.unknown.isEmpty() ? String.valueOf(got) : "לא הובן").append("]");
                Cond n = Cond.parse(Text.norm(w[0]));                        // גם אחרי נרמול, כמו בחיפוש
                if (!n.unknown.isEmpty() || n.matches(q, d95) != want) bad95.append(" [מנורמל: ").append(w[0]).append("]");
            }
            check("מבנה 95: \"<=\" ו\">\" על תאריך מדויק — לפי היום כולו; \"=\" בגודל (17 תנאים)" + bad95, bad95.length() == 0);
            check("מבנה 95: תאריך שאינו קיים (31.2, 30.13) — \"לא הובן\", ולא תאריך אחר בשקט",
                    Cond.parse("תאריך:31.2.2026").unknown.size() == 1 && Cond.parse("תאריך:1.13.2026").unknown.size() == 1);
            check("מבנה 95: זמן יחסי — בלי שינוי (\">שבוע\" = \"שבוע\"; \"<=3ימים\" = \"<3ימים\")",
                    Cond.parse("תאריך:>שבוע").matches(q, r) == Cond.parse("תאריך:שבוע").matches(q, r)
                            && Cond.parse("תאריך:<=3ימים").matches(q, d95) == Cond.parse("תאריך:<3ימים").matches(q, d95));
        }
        check("189: גם אחרי נרמול (\"נוספ\")", Cond.parse(Text.norm("נוסף:שבוע")).matches(q, r));
        r.added = 0;
        check("189: לא ידוע — לא \"חדש\", כן \"ישן\"", !Cond.parse("נוסף:שבוע").matches(q, r)
                && Cond.parse("נוסף:<שבוע").matches(q, r));

        // הטופס
        DateSize ds = new DateSize();
        ds.allowAdded = true;
        check("189: הטופס קולט נוסף:", ds.take("נוסף:<חודש") && ds.added && ds.dateMode == 2 && ds.dateValue.equals("חודש"));
        eq("189: ובחזרה", "נוסף:<חודש", ds.dateToken());
        DateSize batch = new DateSize();
        check("189: בקובץ פקודות — לא (נשאר כמו שהוא)", !batch.take("נוסף:שבוע"));
        ds.added = false;
        eq("189: בחירה \"של השינוי\" — תאריך:", "תאריך:<חודש", ds.dateToken());
        eq("189: הרשימה החכמה — נוסף: בחלונית ההגדרות", "נוסף:שבוע", SmartLists.split("נוסף:שבוע יוגה")[0]);

        // המיון
        Sort plain = Sort.of(null, false, null), withA = Sort.withAdded(Sort.RANK, true, null);
        check("189: תאריך ההוספה — רק בכלים שיש להם", !Arrays.asList(plain.options()).contains(Sort.ADDED)
                && Arrays.asList(withA.options()).indexOf(Sort.ADDED) == Arrays.asList(withA.options()).indexOf(Sort.DATE) + 1);
        List<R> rows = new ArrayList<R>(Arrays.asList(
                new R("א", "/x", 1, 1, false, 0, 0), new R("ב", "/x", 1, 1, false, 500, 0), new R("ג", "/x", 1, 1, false, 900, 0)));
        withA.by = Sort.ADDED;
        withA.apply(rows, RK);
        eq("189: החדש קודם, לא ידוע בסוף", Arrays.asList("ג", "ב", "א"), order(rows));
        Mem m = new Mem();
        m.set(SmartLists.SORT, Sort.ADDED);
        check("189: נשמר ונקרא", Sort.withAdded(Sort.RANK, true, m).by.equals(Sort.ADDED)
                && !Sort.of(Sort.RANK, true, m).by.equals(Sort.ADDED));
        wipe(root);
    }

    /** 182: תחליף למאגר של אנדרואיד — מונה, גרסה, ומה השתנה מאז. */
    private static final class FakeMedia implements Media.Source {
        long gen = 100;
        String version = "v1";
        List<String> changed = new ArrayList<String>();
        int asked;
        long askedSince = -1;
        boolean fail;

        public long generation(String volume) { return gen; }
        public String version(String volume) { return version; }
        public List<String> changedSince(String volume, long since) {
            asked++;
            askedSince = since;
            return fail ? null : changed;
        }
        /** 189: מתי נוסף — לפי מסלול (null — המאגר לא עונה). */
        java.util.Map<String, Long> added = new java.util.HashMap<String, Long>();
        int askedAdded;
        public java.util.Map<String, Long> dateAdded(String volume) {
            askedAdded++;
            return fail ? null : added;
        }
    }

    private static java.util.Set<String> live(Index ix) {
        java.util.Set<String> out = new java.util.HashSet<String>();
        for (int k = 0; k < ix.dirs.size(); k++) if (!ix.dirs.get(k).gone) out.add("D " + ix.dirPath(k));
        for (Index.Rec r : ix.files)
            if (!r.gone && (r.dir < 0 || !ix.dirs.get(r.dir).gone)) out.add(ix.path(r) + " " + r.size + " " + r.mtime);
        return out;
    }

    private static void order182() {
        // --- מסלולים מהמאגר ---
        Index card = new Index("ABCD-1234", "כרטיס", "/storage/ABCD-1234");
        eq("182: מסלול מהמאגר — השורש בלי הבדל אותיות", "/storage/ABCD-1234/ספרים/א.pdf",
                Media.onDrive(card, "/storage/abcd-1234/ספרים/א.pdf"));
        check("182: מסלול מכונן אחר — לא", Media.onDrive(card, "/storage/emulated/0/א.pdf") == null);
        check("182: שורש הכונן עצמו — לא קובץ", Media.onDrive(card, "/storage/ABCD-1234") == null
                && Media.onDrive(card, "/storage/ABCD-1234/") == null);
        check("182: שם דומה לשורש — לא", Media.onDrive(card, "/storage/ABCD-12345/א.pdf") == null);
        eq("182: שם הכרטיס במאגר — באותיות קטנות", "abcd-1234", Media.volume(card));
        eq("182: הפנימי במאגר", "external_primary", Media.volume(new Index(Drive.INTERNAL, "פנימי", "/storage/emulated/0")));

        File root = new File(home, "סדר-182");
        wipe(root);
        mk(new File(root, "ספרים"), "א.pdf", 10);
        mk(new File(root, "ספרים"), "ב.pdf", 20);
        mk(new File(root, "WhatsApp/.Statuses"), "s.jpg", 5);
        mk(new File(root, "תמונות/מטמון"), ".nomedia", 0);
        mk(new File(root, "תמונות/מטמון/פנימית"), "t.jpg", 7);
        mk(new File(root, "תמונות"), "רגילה.jpg", 8);
        mk(new File(root, "ספרים"), ".הגדרות", 3);
        mk(new File(root, "Android/data/x"), "d.bin", 4);
        mk(new File(root, "הוצאה"), "e.pdf", 4);
        Index ix = built("סדר-182", root);
        String rp = root.getAbsolutePath();

        // --- מה צריך להיכנס ---
        check("182: קובץ רגיל — אמור להיות באינדקס", ix.indexable(rp + "/ספרים/א.pdf"));
        check("182: Android/data — לא", !ix.indexable(rp + "/Android/data/x/d.bin"));
        Index.EXCLUDED = new java.util.HashSet<String>(Collections.singletonList(rp + "/הוצאה"));
        check("182: תיקייה שהוצאה — לא", !ix.indexable(rp + "/הוצאה/e.pdf"));
        Index.EXCLUDED = new java.util.HashSet<String>();
        check("182: מחוץ לכונן — לא", !ix.indexable("/אחר/א.pdf"));

        // --- (ב) קובץ שנערך במקומו: בדיקת התאריכים לא רואה, המאגר כן ---
        File a = new File(root, "ספרים/א.pdf");
        long dirTime = new File(root, "ספרים").lastModified();
        mk(new File(root, "ספרים"), "א.pdf", 15);
        a.setLastModified(a.lastModified() + 7000);
        new File(root, "ספרים").setLastModified(dirTime);                 // עריכה במקום לא משנה את התיקייה
        long[] times = ix.times(4);
        check("182: עריכה במקום — בדיקת התאריכים לא רואה", !ix.differs(times));
        List<String> rep = new ArrayList<String>(Arrays.asList(a.getAbsolutePath(), a.getAbsolutePath(),
                rp + "/ספרים/ב.pdf", rp + "/ספרים/נמחק-מזמן.pdf", rp + "/Android/data/x/d.bin"));
        List<String> st = ix.stale(rep);
        eq("182: מה שהמאגר דיווח — רק מה שבאמת השתנה (פעם אחת; לא מה שאינו בכונן, לא Android/data)",
                Collections.singletonList(a.getAbsolutePath()), st);
        Index cp = ix.copy();
        cp.apply(times);
        eq("182: העדכון — קובץ אחד", 1, cp.update(cp.stale(rep)));
        eq("182: אחרי העדכון — כמו סריקה חדשה", live(built("סדר-182", root)), live(cp));
        check("182: התמונה המוצגת לא השתנתה (עותק)", rec(ix, "א.pdf").size == 10);
        check("182: אחרי העדכון — אין עוד מה לעדכן", cp.stale(rep).isEmpty());
        // קובץ חדש שהמאגר מדווח — נכנס, גם בלי שינוי בתאריך התיקייה
        File n = mk(new File(root, "ספרים"), "חדש.pdf", 30);
        new File(root, "ספרים").setLastModified(dirTime);
        Index cp2 = cp.copy();
        eq("182: קובץ חדש מהמאגר — נכנס", 1, cp2.update(cp2.stale(Collections.singletonList(n.getAbsolutePath()))));
        check("182: קובץ חדש מהמאגר — בתיקייה הנכונה", cp2.find(n.getAbsolutePath()) != null);
        eq("182: קובץ חדש — פעם אחת בלבד", 1, cp2.update(cp2.stale(Collections.singletonList(n.getAbsolutePath()))) + 1);

        // --- הנקודה ---
        FakeMedia fm = new FakeMedia();
        Index pv = new Index("נקודה-182", "בדיקה", rp);
        Paths.point(pv.key).delete();
        Media.Point[] next = new Media.Point[1];
        check("182: בלי נקודה — בלי שאלה למאגר", Media.since(fm, pv, next).isEmpty() && fm.asked == 0);
        eq("182: בלי נקודה — מתחילים מהמונה של עכשיו", 100L, next[0].gen);
        next[0].save(pv.key);
        eq("182: הנקודה נשמרה ונקראת", 100L, Media.Point.read(pv.key).gen);
        check("182: אותו מונה — בלי שאלה למאגר", Media.since(fm, pv, next).isEmpty() && fm.asked == 0);
        fm.gen = 105;
        fm.changed = Collections.singletonList(a.getAbsolutePath());
        List<String> got = Media.since(fm, pv, next);
        check("182: מונה גדל — שואלים מה השתנה מאז הנקודה", fm.asked == 1 && fm.askedSince == 100 && got.size() == 1);
        eq("182: הנקודה הבאה — המונה של עכשיו", 105L, next[0].gen);
        fm.fail = true;
        Media.since(fm, pv, next);
        next[0].save(pv.key);
        eq("182: השאלה נכשלה — הנקודה לא זזה", 100L, Media.Point.read(pv.key).gen);
        fm.fail = false;
        fm.version = "v2";
        int before = fm.asked;
        check("182: המאגר נבנה מחדש (גרסה אחרת) — בלי רשימה", Media.since(fm, pv, next).isEmpty() && fm.asked == before);
        next[0].save(pv.key);
        fm.gen = 3;
        check("182: מונה קטן מהשמור — מתחילים מחדש", Media.since(fm, pv, next).isEmpty() && fm.asked == before && next[0].gen == 3);
        fm.gen = -1;
        check("182: אין מאגר (אנדרואיד ישן) — בלי רשימה", Media.since(fm, pv, next).isEmpty() && next[0].gen < 0);
        next[0].save(pv.key);
        eq("182: אין מאגר — הנקודה הקודמת נשארת", 105L, Media.Point.read(pv.key).gen);
        check("182: בלי מקור — בלי רשימה", Media.since(null, pv, next).isEmpty());
        Paths.point(pv.key).delete();

        // --- מה שהמאגר לא מכיר (היומית) ---
        Index u = built("סדר-182", root);
        boolean[] unk = u.unknownToMedia();
        java.util.Set<String> unknown = new java.util.HashSet<String>();
        for (int i = 0; i < unk.length; i++) if (unk[i]) unknown.add(u.files.get(i).name);
        eq("182: לא מוכרים למאגר — מוסתרים, .nomedia וכל מה שבתוכה",
                new java.util.HashSet<String>(Arrays.asList("s.jpg", ".nomedia", "t.jpg", ".הגדרות")), unknown);
        File s1 = new File(root, "WhatsApp/.Statuses/s.jpg");
        mk(s1.getParentFile(), "s.jpg", 50);
        mk(new File(root, "תמונות"), "רגילה.jpg", 80);
        File inner = new File(root, "תמונות/מטמון/פנימית");
        long it = inner.lastModified();
        mk(inner, "t.jpg", 70);
        inner.setLastModified(it);
        Index ud = u.copy();
        eq("182: היומית — רק מה שהמאגר לא מכיר (שניים השתנו)", 2, ud.deepRefresh(4, ud.unknownToMedia()));
        check("182: היומית — קובץ רגיל לא נבדק (בשבילו המאגר)", rec(ud, "רגילה.jpg").size == 8);
        check("182: היומית — במוסתרת ובתוך .nomedia — עודכן", rec(ud, "s.jpg").size == 50 && rec(ud, "t.jpg").size == 70);
        Index all = u.copy();
        check("182: בלי סימון — כמו הרענון המעמיק (כל הקבצים)", all.deepRefresh(4, null) >= 3);
        File rootNm = new File(home, "סדר-182-שורש");
        wipe(rootNm);
        mk(rootNm, ".nomedia", 0);
        mk(new File(rootNm, "א"), "x.pdf", 1);
        boolean[] allUnk = built("ש182", rootNm).unknownToMedia();
        check("182: .nomedia בשורש הכונן — כל הכונן לא מוכר", allUnk.length == 2 && allUnk[0] && allUnk[1]);
        wipe(rootNm);

        // --- השבועית: ההבדלים מול האינדקס שהיה ---
        Index old = built("סדר-182", root);
        new File(root, "ספרים/ב.pdf").delete();
        mk(new File(root, "ספרים"), "ג.pdf", 1);
        mk(new File(root, "ספרים"), "א.pdf", 99);
        Index nw = built("סדר-182", root);
        int[] d = Index.diff(old, nw);
        check("182: השבועית — נוסף 1, נמחק 1, השתנה 1 (" + d[0] + "," + d[1] + "," + d[2] + ")", d[0] == 1 && d[1] == 1 && d[2] == 1);
        int[] z = Index.diff(nw, built("סדר-182", root));
        check("182: בלי שינוי — אפס בכולם", z[0] == 0 && z[1] == 0 && z[2] == 0);

        // --- מה מגיע עכשיו ---
        long now = 100L * Library.WEEK, h = 3600_000L;
        eq("182: יום לא עבר — כלום", Library.NONE, Library.due(now, now - 2 * h, 0, true));
        eq("182: עבר יום — היומית (רק מה שהמאגר לא מכיר)", Library.DAILY, Library.due(now, now - 25 * h, 0, true));
        eq("182: היומית רצה לפני שעה — כלום", Library.NONE, Library.due(now, now - 3 * Library.DAY, now - h, true));
        eq("182: עבר שבוע — בנייה מההתחלה", Library.WEEKLY, Library.due(now, now - Library.WEEK - h, now - h, true));
        eq("182: אנדרואיד ישן — בנייה מלאה פעם ביום", Library.FULL_DAILY, Library.due(now, now - 25 * h, now - h, false));
        eq("182: אנדרואיד ישן, יום לא עבר — כלום", Library.NONE, Library.due(now, now - h, 0, false));

        // --- תקופת הניסיון ---
        String l1 = Trial.line(0, Words.Trial.DAILY, 310, "נבדקו 4157", 2);
        check("182: שורה ביומן — " + l1, l1.endsWith(" | יומית | 0.31 שנ' | נבדקו 4157 | נמצאו 2"));
        List<String> lines = Arrays.asList(l1, Trial.line(0, Words.Trial.DAILY, 300, "נבדקו 4157", 0),
                Trial.line(0, Words.Trial.WEEKLY, 1900, "נוספו 3 · נמחקו 0 · השתנו 1", 4),
                Trial.line(0, Words.Trial.SINCE, 10, "פנימי", 1), "שורה לא מובנת", "");
        check("182: השורות — כל אחת בשורה משלה (הסיכום — בתוסף האבחון, מ0.43)", lines.get(2).contains(" | שבועית | ") && lines.get(3).contains(" | " + Words.Trial.SINCE + " | "));
        Trial.file().delete();
        Trial.add(Words.Trial.DAILY, 5, "נבדקו 1", 0);
        Trial.add(Words.Trial.WEEKLY, 5, "נוספו 0 · נמחקו 0 · השתנו 0", 0);
        try {
            eq("182: היומן בקובץ — שתי שורות", 2, Files.readUtf8(Trial.file()).trim().split("\n").length);
        } catch (Exception e) {
            check("182: היומן בקובץ — " + e, false);
        }
        check("182: היומן — ב־.מערכת של האופן", Trial.file().getParentFile().getName().equals(Paths.SYSTEM));
        Trial.file().delete();
        wipe(root);
    }

    private static void index184() {
        // --- נרמול מהיר = הנרמול הישן, תו בתו ---
        String pool = "aZbQ09 _-.()[]`'\"\t  ABCxyzאבגדהוזחטיכךלמםנןסעפףצץקרשתְִָּׁ֑"
                + "״׳”“’‘‎‏﻿ΣΑΒİÄÖÜÉéДЖ😀ﬃ";
        java.util.Random rnd = new java.util.Random(184);
        boolean same = true;
        String bad = "";
        for (int k = 0; k < 20000 && same; k++) {
            StringBuilder b = new StringBuilder();
            int len = rnd.nextInt(14);
            for (int i = 0; i < len; i++) b.append(pool.charAt(rnd.nextInt(pool.length())));
            String x = b.toString();
            if (!Text.norm(x).equals(Text.normSlow(x))) { same = false; bad = x; }
        }
        check("184: נרמול מהיר — זהה לישן ב־20,000 שמות אקראיים" + (same ? "" : " (נכשל: " + bad + ")"), same);
        String plain = "שיעור 12.pdf";
        check("184: שם שלא משתנה — אותה מחרוזת (בלי העתק בזיכרון)", Text.norm(plain) == plain);
        String nl = Text.norm("מגדל");
        check("184: כתיב חסר בלי שינוי — אותה מחרוזת", Text.loose(nl) == nl);
        eq("184: נרמול — סופיות ואותיות קטנות", "שלומ abc", Text.norm("שלום ABC"));

        // --- מסלולים, מי בתוך מי, מזהה לפי מסלול ---
        File root = new File(home, "בחינה-184");
        wipe(root);
        for (int a = 0; a < 6; a++)
            for (int b = 0; b < 4; b++) {
                File d = new File(root, "א" + a + "/ב" + b + (b % 2 == 0 ? "/ג/ד" : ""));
                for (int c = 0; c < 3; c++) mk(d, "קובץ " + c + ".pdf", c + 1);
            }
        mk(root, "בשורש.txt", 3);
        Index ix = new Index("בחינה-184", "ניסוי", root.getAbsolutePath());
        ix.scan(null, 4);
        boolean paths = true, norms = true, kids = true, ids = true, files = true;
        for (int k = 0; k < ix.dirs.size(); k++) {
            StringBuilder sb = new StringBuilder();
            for (int d = k; d >= 0; d = ix.dirs.get(d).parent) sb.insert(0, "/" + ix.dirs.get(d).name);
            String slow = ix.rootPath + sb;
            if (!slow.equals(ix.dirPath(k))) paths = false;
            if (!Text.norm(slow).equals(ix.normPath(k))) norms = false;
            if (ix.dirId(slow) != k) ids = false;
            java.util.List<Integer> lin = new java.util.ArrayList<Integer>();
            for (int i = 0; i < ix.dirs.size(); i++) if (ix.dirs.get(i).parent == k && !ix.dirs.get(i).gone) lin.add(i);
            if (!lin.equals(ix.childDirs(k))) kids = false;
            java.util.List<Index.Rec> lf = new java.util.ArrayList<Index.Rec>();
            for (Index.Rec r : ix.files) if (r.dir == k && !r.gone) lf.add(r);
            if (!lf.equals(ix.files(k))) files = false;
        }
        check("184: מסלול תיקייה מהמטמון — כמו החישוב הישן", paths);
        check("184: מסלול מנורמל — כמו נרמול המסלול", norms);
        check("184: מזהה לפי מסלול — חוזר לאותה תיקייה", ids);
        check("184: תיקיות בנות — כמו מעבר על הכול", kids);
        check("184: קבצי תיקייה — כמו מעבר על הכול", files);
        boolean normOk = true;
        for (Index.Rec r : ix.files) if (r.norm == null || !r.norm.equals(Text.norm(r.name)) || !r.loose.equals(Text.loose(r.norm))) normOk = false;
        for (Index.Dir d : ix.dirs) if (d.norm == null || !d.norm.equals(Text.norm(d.name))) normOk = false;
        check("184: סריקה מקבילית — השמות המנורמלים מולאו בסוף", normOk);

        // --- בדיקה בלי העתקה: אין שינוי ← אין הבדל, והאינדקס לא נגע ---
        long[] t = ix.times(4);
        check("184: מיד אחרי סריקה — אין הבדל (לא מעתיקים)", !ix.differs(t));
        int before = ix.files.size();
        try {
            mk(root, "חדש בשורש.txt", 5);
            root.setLastModified(System.currentTimeMillis() + 7000);
        } catch (Exception e) { check("184: הכנה — " + e, false); }
        t = ix.times(4);
        check("184: קובץ חדש בשורש — נראה כהבדל", ix.differs(t));
        eq("184: הבדיקה עצמה לא שינתה את האינדקס", before, ix.files.size());
        Index c = ix.copy();
        int ch = c.apply(t);
        check("184: קובץ חדש בשורש — נספר כשינוי (קודם לא הוצג) (" + ch + ")", ch >= 1);
        check("184: קובץ חדש בשורש — נמצא", c.dirId(root.getAbsolutePath()) == -1 && hasFile(c, "חדש בשורש.txt"));
        check("184: אחרי העדכון — אין הבדל", !c.differs(c.times(4)));

        // --- מטמון המסלולים משותף לעותקים — ושני עותקים לא דורסים זה את זה ---
        int n0 = ix.dirs.size();
        Index c1 = ix.copy(), c2 = ix.copy();
        c1.addFile(mk(new File(root, "א1/חדשה-אחת"), "x.pdf", 1));
        c2.addFile(mk(new File(root, "א2/חדשה-שתיים"), "y.pdf", 1));
        check("184: עותק 1 — מסלול התיקייה החדשה", c1.dirPath(n0).endsWith("/א1/חדשה-אחת"));
        check("184: עותק 2 — מסלול התיקייה החדשה (לא דרוס)", c2.dirPath(n0).endsWith("/א2/חדשה-שתיים"));
        check("184: עותק 1 — עדיין שלו", c1.dirPath(n0).endsWith("/א1/חדשה-אחת"));
        eq("184: המקור — בלי תיקייה חדשה", n0, ix.dirs.size());
        check("184: מזהה לפי מסלול בעותק — אחרי הוספה", c1.dirId(new File(root, "א1/חדשה-אחת").getAbsolutePath()) == n0);
        check("184: הסרת קובץ בעותק — לפי מי בתוך מי", c1.removeFile(new File(root, "א1/חדשה-אחת/x.pdf").getAbsolutePath()));

        // --- מחיקת עץ: כל מה שבתוכו יוצא ---
        Index r = ix.copy();
        File big = new File(root, "א4");
        wipe(big);
        root.setLastModified(System.currentTimeMillis() + 9000);
        r.refresh(4);
        boolean out = true;
        for (Index.Rec x : r.files) if (!x.gone && r.path(x).contains("/א4/")) out = false;
        for (int k = 0; k < r.dirs.size(); k++) if (!r.dirs.get(k).gone && r.dirPath(k).contains("/א4")) out = false;
        check("184: תיקייה שנמחקה עם כל העץ — הכול יצא", out);

        // --- רענון מעמיק בכמה קוראים: קובץ שנכתב במקומו, וקובץ שנמחק ---
        Index deep = new Index("בחינה-184", "ניסוי", root.getAbsolutePath());
        deep.scan(null);
        File f1 = new File(root, "א0/ב1/קובץ 1.pdf"), f2 = new File(root, "א0/ב1/קובץ 2.pdf");
        long dm = new File(root, "א0/ב1").lastModified();
        try {
            java.io.FileOutputStream o = new java.io.FileOutputStream(f1);
            o.write(new byte[500]);
            o.close();
        } catch (Exception e) { check("184: הכנה — " + e, false); }
        f2.delete();
        new File(root, "א0/ב1").setLastModified(dm);
        int dc = deep.deepRefresh(8);
        check("184: רענון מעמיק ב־8 — שני שינויים (" + dc + ")", dc == 2);
        boolean grew = false, gone = true;
        for (Index.Rec x : deep.files) {
            if (x.gone) continue;
            if (deep.path(x).equals(f1.getAbsolutePath()) && x.size == 500) grew = true;
            if (deep.path(x).equals(f2.getAbsolutePath())) gone = false;
        }
        check("184: רענון מעמיק — הגודל החדש", grew);
        check("184: רענון מעמיק — הנמחק יצא", gone);

        // --- שמירה בלי לשנות: מה שנעלם לא נכתב, והאינדקס עצמו לא זז ---
        try {
            int dn = deep.dirs.size(), fn = deep.files.size();
            java.util.Set<String> live = new java.util.HashSet<String>();
            for (Index.Rec x : deep.files) if (!x.gone) live.add(deep.path(x));
            for (int k = 0; k < deep.dirs.size(); k++) if (!deep.dirs.get(k).gone) live.add(deep.dirPath(k) + "/");
            deep.save();
            eq("184: שמירה — האינדקס לא השתנה (תיקיות)", dn, deep.dirs.size());
            eq("184: שמירה — האינדקס לא השתנה (קבצים, כולל שנעלמו)", fn, deep.files.size());
            Index back = Index.load("בחינה-184");
            java.util.Set<String> got = new java.util.HashSet<String>();
            for (Index.Rec x : back.files) got.add(back.path(x));
            for (int k = 0; k < back.dirs.size(); k++) got.add(back.dirPath(k) + "/");
            check("184: נטען — בדיוק מה שקיים (" + got.size() + ")", got.equals(live));
            Paths.index("בחינה-184").delete();
        } catch (Exception e) {
            check("184: שמירה — " + e, false);
        }

        // --- חיפוש במסלולים — דרך המסלול המנורמל ---
        Library lib = Library.forTests(new java.util.ArrayList<Index>(java.util.Collections.singletonList(deep)));
        Library.Result res = lib.search("ד", true, false);
        boolean viaPath = false;
        for (Library.Hit h : res.hits) if (!h.isDir() && h.path().contains("/ג/ד/")) viaPath = true;
        check("184: חיפוש במסלולים — נמצא לפי שם תיקייה במסלול", viaPath);
        check("184: קובץ לפי מסלול — נמצא", lib.find(f1.getAbsolutePath()) != null);
        check("184: קובץ שנמחק — לא נמצא", lib.find(f2.getAbsolutePath()) == null);
        check("184: תיקייה — אינה קובץ", lib.find(new File(root, "א0/ב1").getAbsolutePath()) == null);
        check("184: מחוץ לכונן — לא נמצא", lib.find("/אחר/x.pdf") == null);
        File many = new File(root, "הרבה");
        for (int i = 0; i < 50; i++) mk(many, "ק" + i + ".txt", 1);
        Index mx = new Index("בחינה-184", "ניסוי", root.getAbsolutePath());
        mx.scan(null);
        boolean all50 = true;
        for (int i = 0; i < 50; i++) if (mx.find(new File(many, "ק" + i + ".txt").getAbsolutePath()) == null) all50 = false;
        check("184: תיקייה גדולה (מפת שמות) — כל 50 נמצאים", all50);
        check("184: תיקייה גדולה — שם שאינו קיים", mx.find(new File(many, "אין.txt").getAbsolutePath()) == null);
        Index mc = mx.copy();
        mc.removeFile(new File(many, "ק7.txt").getAbsolutePath());
        check("184: אחרי הסרה בעותק — לא נמצא בעותק", mc.find(new File(many, "ק7.txt").getAbsolutePath()) == null);
        check("184: …ועדיין נמצא במקור", mx.find(new File(many, "ק7.txt").getAbsolutePath()) != null);
        wipe(root);

        // --- שמירה וטעינה גדולה: הנרמול בכל הליבות ---
        Index g = new Index("גדול-184", "ניסוי", "/storage/x");
        try {
            java.lang.reflect.Method nd = Index.class.getDeclaredMethod("newDir", int.class, String.class, long.class);
            java.lang.reflect.Method nr = Index.class.getDeclaredMethod("newRec", int.class, String.class, long.class, long.class);
            nd.setAccessible(true);
            nr.setAccessible(true);
            for (int i = 0; i < 300; i++) nd.invoke(g, i < 5 ? -1 : rnd.nextInt(i), "תיקייה " + i, 1L);
            for (int i = 0; i < 25000; i++) nr.invoke(g, rnd.nextInt(300), (i % 3 == 0 ? "שיעור" : i % 3 == 1 ? "IMG_" : "מגדלים ") + i + ".JPG", 1L, 2L);
            g.save();
            Index l = Index.load("גדול-184");
            boolean ok = l != null && l.files.size() == 25000;
            for (int i = 0; ok && i < l.files.size(); i++) {
                Index.Rec x = l.files.get(i);
                if (!Text.norm(x.name).equals(x.norm) || !Text.loose(x.norm).equals(x.loose)) ok = false;
            }
            check("184: טעינה של 25,000 — כל השמות המנורמלים נכונים", ok);
            check("184: טעינה — מסלול תיקייה", l != null && l.dirPath(7).equals(g.dirPath(7)));
            Paths.index("גדול-184").delete();
        } catch (Exception e) {
            check("184: שמירה וטעינה — " + e, false);
        }
    }

    private static boolean hasFile(Index ix, String name) {
        for (Index.Rec r : ix.files) if (!r.gone && r.name.equals(name)) return true;
        return false;
    }

    private static java.util.Set<String> all177(Index ix) {
        java.util.Set<String> out = new java.util.HashSet<String>();
        for (int k = 0; k < ix.dirs.size(); k++) out.add("D " + ix.dirPath(k) + " " + ix.dirs.get(k).mtime);
        for (Index.Rec r : ix.files) out.add("F " + ix.path(r) + " " + r.size + " " + r.mtime);
        return out;
    }
}
