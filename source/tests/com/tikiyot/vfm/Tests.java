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
        run("גרסה 1.23.0 — ממשק הפקודות: שורה לפי ההגדרה (CmdLine), פקודות נפוצות, קובץ הבדיקה", new Runnable() { public void run() { v1230(); } });
        run("מ0.1 — מונה המזהים, שתי הבניות, שכבת האחסון (MemStore, SqlStore)", new Runnable() { public void run() { storeM01(); } });
        run("מ0.3 — הערכת רשימה חכמה מעל האינדקס (Smarts)", new Runnable() { public void run() { smartsM03(); } });

        System.out.println("בדיקות: " + passed + " עברו · " + failed + " נכשלו");
        for (String f : failures) System.out.println("  ✗ " + f);
        System.exit(failed == 0 ? 0 : 1);
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
        eq("80: בידוד כיוון", "\u2068Download\u2069", Text.iso("Download"));
        eq("80: הבידוד לא נכנס לחיפוש", Text.norm("Download"), Text.norm(Text.iso("Download")).replace("\u2068", "").replace("\u2069", ""));
        Sort ds = Sort.of(Sort.SPACE, false, null);
        eq("כפולים: ברירת המחדל — מקום לפינוי", Sort.SPACE, ds.by);
        check("כפולים: בתפריט — הבסיס המשותף ו'מקום לפינוי'", java.util.Arrays.asList(ds.options()).containsAll(
                java.util.Arrays.asList(Sort.NAME, Sort.DATE, Sort.SIZE, Sort.TYPE, Sort.FOLDER, Sort.SPACE)));
        java.util.Set<String> icons = new java.util.HashSet<String>(java.util.Arrays.asList(Drive.INTERNAL_ICON, Drive.CARD_ICON));
        for (Func f : Func.all()) check("צלמית אחת לדבר אחד — " + f.kind, icons.add(f.icon));
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
        Buttons.Layout def = Buttons.defaultsAll();
        Buttons.Layout back = Buttons.parseAll(Buttons.textAll(def));
        check("1.10.0: כפתורים — נכתבים ונקראים", back.rows(Buttons.TOP).size() == 1
                && back.rows(Buttons.TOP).get(0).size() == Buttons.defaults().size()
                && back.rows(Buttons.TOP).get(0).get(0).act.equals(Buttons.HOME));
        String listBottom = Buttons.local(Buttons.BOTTOM, Words.Top.S_LIST);
        check("1.10.0: הכפתורים הקבועים של הרשימה — בשורה התחתונה שלה", back.rows(listBottom).size() == 1
                && back.rows(listBottom).get(0).get(0).act.equals(Buttons.C_NEW_LIST));   // 1.13.0: כשורת פקודה
        Buttons.Layout old = Buttons.parseAll("⭐ | פקודות | מסך:פקודות\n📂 | מנהל קבצים | מסך:מנהל קבצים\nשורה שבורה\n");
        java.util.List<Buttons.Btn> top = old.rows(Buttons.TOP).get(0);
        check("1.10.0: קובץ של 1.9.0 — הכול בשורה העליונה, עוגנים חוזרים", top.size() == 4 && top.get(0).act.equals(Buttons.HOME)
                && top.get(3).act.equals(Buttons.BACK));
        eq("1.10.0: השם הישן \"מנהל קבצים\" — \"קבצים פיזיים\"", Words.Top.FILES, top.get(2).name);
        check("1.10.0: קובץ של 1.9.0 — ברירת המחדל של התחתונות נשמרת", old.has(listBottom));
        Buttons.Layout rows = Buttons.parseAll("[למעלה]\n🏠 | בית | מסך:בית\n---\n→ | חזרה | מסך:חזרה\n[" + listBottom + "]\n");
        check("1.10.0: שתי שורות במקום אחד", rows.rows(Buttons.TOP).size() == 2);
        check("1.10.0: מקום שרוקן — נשאר ריק (בלי ברירת מחדל)", !rows.has(listBottom));
        java.util.List<Buttons.Btn> r0 = rows.rows(Buttons.TOP).get(0), r1 = rows.rows(Buttons.TOP).get(1);
        check("1.10.0: עוגן יחיד — לא נמחק, והשורה שלו לא נמחקת", !Buttons.removable(rows, Buttons.TOP, r0, 0)
                && !Buttons.rowRemovable(rows, Buttons.TOP, r0));
        r1.add(new Buttons.Btn("🏠", "שוב", Buttons.HOME));
        check("1.10.0: עוגן כפול — אפשר למחוק אחד", Buttons.removable(rows, Buttons.TOP, r0, 0)
                && Buttons.rowRemovable(rows, Buttons.TOP, r0));
        check("1.10.0: עוגן אחרון — לא עובר לשורה של סוג מסך", !Buttons.moveTo(rows, Buttons.TOP, r1, 0, listBottom, rows.rows(listBottom).isEmpty()
                ? newRow(rows, listBottom) : rows.rows(listBottom).get(0)));
        java.util.List<Buttons.Btn> d0 = Buttons.defaults();
        check("1.9.0: הזזה", Buttons.move(d0, 1, 1) && d0.get(2).act.equals(Buttons.COMMANDS) && !Buttons.move(d0, 0, -1));
        StringBuilder many = new StringBuilder("[למעלה]\n");
        for (int i = 0; i < 8; i++) many.append("⭐ | א | מסך:פקודות\n---\n");
        check("1.10.0: עד 5 שורות במקום", Buttons.parseAll(many.toString()).rows(Buttons.TOP).size() == Buttons.MAX_ROWS);

        // START %בית%
        Map<String, String> vars = Batch.vars(root, root.getAbsolutePath(), null);
        Batch.Plan hp = Batch.plan("START %בית%", root, vars);
        check("1.9.0: START %בית%", hp.errors == 0 && Batch.isHome(hp.start));
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
        Map<String, String> vars = Batch.vars(root, root.getAbsolutePath(), null);
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
        for (File f : Acts.filesIn(new File(root, "הורדות"))) names.add(f.getName());
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
            Batch.Plan bp = Batch.plan(text, demo, Batch.vars(demo, home.getAbsolutePath(), null));
            String errs = "";
            for (Batch.Step st : bp.steps) if (st.error != null) errs += st.line + ": " + st.error + " ";
            check("1.10.0: דוגמת פקודות בכאילו ובלי שגיאות — " + b.getName() + " " + errs, bp.asIf && bp.errors == 0 && bp.changes() > 0 || bp.start != null && bp.asIf && bp.errors == 0);
        }
        eq("1.10.0: ארבע דוגמאות פקודות", 4, n);
        check("1.10.0: קובץ \"ישן\" — לפני שנתיים", System.currentTimeMillis() - new File(demo, "הורדות/קבלה ישנה.txt").lastModified() > 700L * 24 * 3600 * 1000);
        Batch.Plan del = null;
        for (File b : bats) if (b.getName().startsWith("3 ")) try { del = Batch.plan(Files.readUtf8(b), demo, Batch.vars(demo, home.getAbsolutePath(), null)); } catch (Exception e) { }
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
                && Design.FILL.equals(def.around) && !def.fit && def.lines == 2);
        Design files = Design.parse(Design.FILES, Design.builtInText(Design.FILES));
        check("1.12.0: תוכנת ניהול קבצים — רשת 3, מסגרת, התאם למקום", files.grid && files.cols == 3 && files.icon == 55
                && Design.FRAME.equals(files.around) && files.fit && files.nameT.weight == 700);
        Design words = Design.parse(Design.WORDS, Design.builtInText(Design.WORDS));
        check("1.12.0: מילים וצבע — בלי צלמית, עם צבע רקע", !words.grid && words.cols == 2 && !words.showIcon && words.tint);

        // מה שחסר — מברירת המחדל; שורה לא מובנת — לא נלקחת; הערה בסוף שורה
        Design part = Design.parse("חלקי", "[אמצע]\nסגנון = רשת ; הערה\nטורים = ארבעה\nשם: משקל = מודגש\nמשהו מוזר\n[לא קיים]\nגודל = 99\n");
        check("1.12.0: חלקי — רשת, משקל מודגש", part.grid && part.nameT.weight == 700);
        eq("1.12.0: ערך לא מובן — נשאר מברירת המחדל", 1, part.cols);
        eq("1.12.0: מה שחסר — מברירת המחדל", 16f, part.nameT.size);
        eq("1.12.0: משקל — שם ↔ מספר", 300, Design.weight("דק"));
        eq("1.12.0: מספר טורים — בגבולות", 8, Design.parse("x", "[אמצע]\nטורים = 40\n").cols);

        // החלק הפרטי — צלמית וצבע לפי המסלול
        Design withLists = Design.parse("x", "[רשימות]\nלימוד = צלמית 📚 · צבע ירוק\nלימוד/הערות = צבע #112233\n");
        eq("1.12.0: צלמית לפי המסלול", "📚", withLists.iconOf(Arrays.asList("לימוד"), "📋"));
        eq("1.12.0: בלי צלמית בעיצוב — מה שהיה ברשימה (1.11.0)", "📒", withLists.iconOf(Arrays.asList("לימוד", "הערות"), "📒"));
        eq("1.12.0: צבע", "#112233", withLists.colorOf(Arrays.asList("לימוד", "הערות")));
        eq("1.12.0: רשימה בשם אחר — בלי", "", withLists.colorOf(Arrays.asList("ספרים")));
        String t1 = Design.withPrivate("[אמצע]\nטורים = 3\n", "לימוד", "לימוד = צלמית 📚");
        check("1.12.0: שורה פרטית ראשונה — נוסף [רשימות]", t1.contains("[רשימות]\nלימוד = צלמית 📚") && t1.startsWith("[אמצע]"));
        String t2 = Design.withPrivate(t1, "לימוד", "לימוד = צלמית 📖");
        check("1.12.0: שורה פרטית — מוחלפת, לא כפולה", t2.contains("📖") && !t2.contains("📚"));
        String t3 = Design.withPrivate(t2 + "[אחר]\nא = ב\n", "ספרים", "ספרים = צבע כחול");
        check("1.12.0: שורה פרטית חדשה — בתוך [רשימות], לפני הקבוצה הבאה",
                t3.indexOf("ספרים = צבע כחול") < t3.indexOf("[אחר]") && t3.indexOf("ספרים = צבע כחול") > t3.indexOf("[רשימות]"));
        check("1.12.0: מחיקת שורה פרטית", !Design.withPrivate(t3, "לימוד", null).contains("לימוד ="));
        String moved = Design.renamed("[רשימות]\nלימוד = צלמית 📚\nלימוד/הערות = צבע ירוק\nלימודים = צבע כחול\n[אמצע]\nלימוד = 3\n",
                "לימוד", "תורה");
        check("1.12.0: רשימה שזזה — השורות שלה ושל מה שבתוכה עוברות", moved.contains("תורה = צלמית 📚")
                && moved.contains("תורה/הערות = צבע ירוק") && moved.contains("לימודים = צבע כחול")
                && moved.contains("[אמצע]\nלימוד = 3"));
        check("1.12.0: פירוק השורה הפרטית", Arrays.equals(new String[]{"📚", "ירוק"}, Design.privateValue("צלמית 📚 · צבע ירוק")));

        // @עיצוב ברשימה — נקרא ונשמר; הירושה
        File tf = new File(home, "מבנה-1120.txt");
        Files.writeAtomic(tf, "@עיצוב פנקס שורש\n#1 לימוד\n@עיצוב תוכנת ניהול קבצים\n#2 לימוד / הערות\n#3 לימוד / הערות / ישנות\n@עיצוב מילים וצבע\n#1 ספרים\n");
        File tf2 = new File(home, "מבנה-1120ב.txt");
        String homeBefore = Design.homeName();
        Design.setHome("");
        Design.setHome(Design.FILES);
        Design.setHome(homeBefore);

        // קבצים: מובנה נכתב כשצריך; שם שלא קיים — ברירת המחדל; שמירה בשם; חזרה לברירת מחדל שומרת את הצלמיות
        File dir = Design.dir();
        wipe(dir);
        Design g = Design.get(Design.FILES);
        check("1.12.0: מובנה — נכתב לתיקיית האופן", Design.fileOf(Design.FILES).isFile() && g.grid);
        eq("1.12.0: עיצוב שלא קיים — ברירת המחדל (לא נשבר)", Design.DEFAULT, Design.get("אין כזה").name);
        eq("1.12.0: שמירה בשם", null, Design.saveAs(g, "מדף ספרים"));
        check("1.12.0: שמירה בשם — העותק זהה", Design.get("מדף ספרים").grid && Design.get("מדף ספרים").cols == 3);
        eq("1.12.0: שמירה בשם תפוס — נדחה", Words.DesignW.EXISTS, Design.saveAs(g, "מדף ספרים"));
        check("1.12.0: צלמית לרשימה — נכתבת לקובץ", Design.setPrivate(Design.fileOf(Design.FILES), "לימוד", "📚", "ירוק"));
        Files.writeAtomic(Design.fileOf(Design.FILES),
                Files.readUtf8(Design.fileOf(Design.FILES)).replace("טורים = 3", "טורים = 5"));
        Design.fileOf(Design.FILES).setLastModified(System.currentTimeMillis() + 2000);
        eq("1.12.0: עריכה ידנית — נקראת", 5, Design.get(Design.FILES).cols);
        check("1.12.0: חזרה לברירת מחדל", Design.reset(Design.FILES));
        Design.fileOf(Design.FILES).setLastModified(System.currentTimeMillis() + 4000);
        Design r = Design.get(Design.FILES);
        check("1.12.0: חזרה לברירת מחדל — המראה מהמקור, הצלמיות נשארו", r.cols == 3
                && "📚".equals(r.iconOf(Arrays.asList("לימוד"), "")) && "ירוק".equals(r.colorOf(Arrays.asList("לימוד"))));
        check("1.12.0: חזרה לברירת מחדל — רק למובנה", !Design.reset("מדף ספרים"));

        // מאופן אחר — מועתק לאופן הזה; שם תפוס בתוכן אחר — עם שם האופן
        File other = new File(new File(new File(new File(Paths.data(), Modes.DIR), "בדיקת עיצובים"), Paths.SETTINGS), Design.DIR);
        wipe(other.getParentFile().getParentFile());
        other.mkdirs();
        Files.writeAtomic(new File(other, "מדף ספרים" + Design.EXT), "[אמצע]\nטורים = 4\n");
        Files.writeAtomic(new File(other, "פנקס" + Design.EXT), "[אמצע]\nסגנון = שורות\n");
        List<String[]> all = Design.all(Arrays.asList("/x/הורדות/מקבלים" + Design.EXT));
        boolean fromOther = false, builtIns = false, fromDrive = false;
        for (String[] a : all) {
            if (a[0].equals("פנקס") && a[1].equals("בדיקת עיצובים")) fromOther = true;
            if (a[0].equals(Design.WORDS) && a[1].isEmpty()) builtIns = true;
            if (a[0].equals("מקבלים")) fromDrive = true;
        }
        check("1.12.0: כל העיצובים — גם מאופן אחר, וגם המובנים", fromOther && builtIns);
        check("1.12.0: קובץ שלא קיים בכונן — לא ברשימה", !fromDrive);
        eq("1.12.0: מאופן אחר — מועתק בשמו", "פנקס", Design.adopt(new String[]{"פנקס", "בדיקת עיצובים",
                new File(other, "פנקס" + Design.EXT).getAbsolutePath()}));
        check("1.12.0: מאופן אחר — עכשיו כאן", Design.fileOf("פנקס").isFile());
        eq("1.12.0: שם תפוס בתוכן אחר — עם שם האופן", "מדף ספרים (בדיקת עיצובים)", Design.adopt(new String[]{"מדף ספרים",
                "בדיקת עיצובים", new File(other, "מדף ספרים" + Design.EXT).getAbsolutePath()}));
        wipe(other.getParentFile().getParentFile());

        // רשימה שזזה — בכל עיצובי האופן; ספירת הרשימות באופן — @עיצוב אינו פריט
        Design.listMoved(Arrays.asList("לימוד"), Arrays.asList("תורה"));
        Design.fileOf(Design.FILES).setLastModified(System.currentTimeMillis() + 6000);
        eq("1.12.0: רשימה שזזה — הצלמית איתה", "📚", Design.get(Design.FILES).iconOf(Arrays.asList("תורה"), ""));
        String m = "בדיקת ספירה 1120";
        File md = new File(new File(Paths.data(), Modes.DIR), m);
        md.mkdirs();
        wipe(md);
        Files.writeAtomic(new File(Design.dir(), "x.txt"), "לא עיצוב");
        boolean onlyDesigns = true;
        for (File f : Design.files(Design.dir())) if (!f.getName().endsWith(Design.EXT)) onlyDesigns = false;
        check("1.12.0: מוצאים רק לפי הסיומת", onlyDesigns);

        // 1.12.1: הטופס — כתיבת המראה הכללי; ההערות והחלק הפרטי נשארים
        String k1 = Design.withKey("; הערה\n[אמצע]\nטורים = 3 ; למה\n\n[רשימות]\nלימוד = צלמית 📚\n", Design.S_MID, Design.K_COLS, "4");
        check("1.12.1: מפתח קיים — מוחלף במקומו", k1.contains("[אמצע]\nטורים = 4\n") && k1.startsWith("; הערה"));
        String k2 = Design.withKey(k1, Design.S_MID, Design.K_TINT, "כן");
        check("1.12.1: מפתח חסר — נוסף בסוף הקבוצה, לפני [רשימות]",
                k2.indexOf("צבע רקע לפריט = כן") < k2.indexOf("[רשימות]") && k2.contains("לימוד = צלמית 📚"));
        String k3 = Design.withKey("[רשימות]\nא = צבע ירוק\n", Design.S_TITLE, Design.K_SIZE, "15");
        check("1.12.1: קבוצה חסרה — נוספת לפני [רשימות]", k3.indexOf("[כותרת המסך]") < k3.indexOf("[רשימות]"));
        Design form = Design.get(Design.FILES);
        form.cols = 4;
        form.tint = true;
        form.nameT.weight = 500;
        form.around = Design.NONE;
        check("1.12.1: שמירה מהטופס", Design.save(form));
        Design.fileOf(Design.FILES).setLastModified(System.currentTimeMillis() + 8000);
        Design back = Design.get(Design.FILES);
        check("1.12.1: שמירה מהטופס — נקראת בחזרה", back.cols == 4 && back.tint && back.nameT.weight == 500
                && Design.NONE.equals(back.around) && back.grid);
        check("1.12.1: שמירה מהטופס — הצלמיות נשארו", "📚".equals(back.iconOf(Arrays.asList("תורה"), "")));

        // 1.12.1: שינוי שם ומחיקה — הרשימות ומסך הבית מתעדכנים
        String homeWas = Design.homeName();
        Design.setHome("מדף ספרים");
        eq("1.12.1: שינוי שם", null, Design.rename("מדף ספרים", "מדף"));
        eq("1.12.1: שינוי שם — גם מסך הבית", "מדף", Design.homeName());
        eq("1.12.1: שינוי שם לשם תפוס — נדחה", Words.DesignW.EXISTS, Design.rename("מדף", Design.FILES));
        check("1.12.1: מחיקה", Design.delete("מדף") && !Design.fileOf("מדף").exists());
        Design.setHome(homeWas);
        wipe(dir);
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
        eq("80: בתוך הסל", FileItem.State.IN_TRASH, FileItem.of(ix, rec(ix, "נמחק.pdf")).state);
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
        check("מוסתר ובסל נחשבים קיימים", FileItem.State.HIDDEN.present() && FileItem.State.IN_TRASH.present());
        check("חסר, נמחק ומדיה אינם קיימים", !FileItem.State.MISSING.present()
                && !FileItem.State.DELETED.present() && !FileItem.State.OFFLINE.present());
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

        List<String> one = labels(Actions.fitting(Actions.Tool.SEARCH, 1, 0, 0, false));
        List<String> two = labels(Actions.fitting(Actions.Tool.SEARCH, 2, 0, 0, false));
        check("חיפוש, קובץ אחד: פתיחה", one.contains("פתיחה"));
        check("חיפוש, שני קבצים: בלי פתיחה", !two.contains("פתיחה"));
        check("חיפוש, שני קבצים: מחיקה מהכונן", two.contains("מחיקה מהכונן"));
        eq("חיפוש, כלום לא נבחר: אין פעולות", 0, Actions.fitting(Actions.Tool.SEARCH, 0, 0, 0, false).size());
        // 174: תיקיות בכונן בבחירה — רק מה שעובד על תיקיות (כמו הפס)
        List<Actions.Action> withDir = Actions.fitting(Actions.Tool.SEARCH, 1, 0, 1, false);
        check("174: תיקייה + קובץ — מחיקה מהכונן, העברה, הוספה לרשימה", withDir.contains(Actions.DELETE_FROM_DRIVE)
                && withDir.contains(Actions.MOVE_TO_FOLDER) && withDir.contains(Actions.ADD_TO_LIST));
        boolean onlyFolders = true;
        for (Actions.Action x : withDir) if (!x.folders) onlyFolders = false;
        check("174: עם תיקייה — אין פעולה שאינה עובדת על תיקיות", onlyFolders);
        check("174: תיקייה לבד — יש פעולות", !Actions.fitting(Actions.Tool.SEARCH, 0, 0, 1, false).isEmpty());
        check("בחירה לרשימה (➕ מחיפוש): בלי מחיקה", !labels(Actions.fitting(Actions.Tool.SEARCH, 2, 0, 0, true))
                .contains("מחיקה מהכונן"));
        check("בחירה לרשימה: הוספה נשארת", labels(Actions.fitting(Actions.Tool.SEARCH, 2, 0, 0, true))
                .contains(Actions.ADD_TO_LIST.label));

        List<String> list = labels(Actions.fitting(Actions.Tool.LIST, 1, 0, 0, false));
        check("רשימה ידנית: הסרה מהרשימה", list.contains(Words.REMOVE_FROM_LIST));
        check("רשימה ידנית: אין \"הוספה לרשימה\" (יש העברה/הוספה גם)", !list.contains(Actions.ADD_TO_LIST.label));
        check("רשימה ידנית: פתיחה קובץ אחד (חדש מהמרשם)", list.contains("פתיחה"));
        List<String> withList = labels(Actions.fitting(Actions.Tool.LIST, 1, 1, 0, false));
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
        check("1.16.0: העתקה — גם רשימות וגם קבצים, וירטואלית (לא בתפריט פיזי)", Actions.DUPLICATE_LIST.fits(Actions.Tool.LIST, 0, 2, 0, false)
                && Actions.DUPLICATE_LIST.fits(Actions.Tool.LIST, 1, 1, 0, false) && !Actions.inPhysicalMenu(Actions.DUPLICATE_LIST, Actions.Tool.LIST));
        check("1.16.0: קישור — רק לרשימות", Actions.ALSO_TO_LIST.fits(Actions.Tool.LIST, 0, 2, 0, false)
                && !Actions.ALSO_TO_LIST.fits(Actions.Tool.LIST, 1, 1, 0, false));
        check("1.16.0: הגדרות — לרשימה אחת בלבד", Actions.fitting(Actions.Tool.LIST, 0, 1, 0, false).contains(Actions.SETTINGS_LIST)
                && !Actions.fitting(Actions.Tool.LIST, 1, 1, 0, false).contains(Actions.SETTINGS_LIST)
                && !Actions.fitting(Actions.Tool.LIST, 0, 2, 0, false).contains(Actions.SETTINGS_LIST));

        List<String> trash = labels(Actions.fitting(Actions.Tool.TRASH, 3, 0, 0, false));
        eq("סל: שחזור ומחיקה לצמיתות", Arrays.asList("↩ שחזור", "מחיקה לצמיתות"), trash);
        check("סל: אין מחיקה מהכונן", !labels(Actions.forTool(Actions.Tool.TRASH)).contains("מחיקה מהכונן"));
        check("76: תיקייה נבחרה — הוספה לרשימה", Actions.ADD_TO_LIST.fits(Actions.Tool.DRIVE, 0, 0, 2, false));
        check("76: תיקייה וקובץ — הוספה לרשימה", Actions.ADD_TO_LIST.fits(Actions.Tool.DRIVE, 1, 0, 1, false));
        check("82: תיקייה נבחרה — יש מחיקה מהכונן", Actions.DELETE_FROM_DRIVE.fits(Actions.Tool.DRIVE, 1, 0, 1, false));
        check("82: תיקייה נבחרה — יש העברה", Actions.MOVE_TO_FOLDER.fits(Actions.Tool.SEARCH, 0, 0, 1, false));
        check("82: תיקייה נבחרה — יש העתקה", Actions.COPY_TO_FOLDER.fits(Actions.Tool.DRIVE, 0, 0, 2, false));
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
        check("תיקייה נבחרה — בלי פתיחה", !Actions.OPEN.fits(Actions.Tool.DRIVE, 0, 0, 1, false));
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
        check("הודעה: הסיבה", Physical.message("נמחקו", "הוסרו", r).contains("לא נמצא"));

        // העברה לתיקייה: הקובץ זז, האינדקס בשני הקצוות, הרשימות עוקבות
        File dest = new File(root, "יעד");
        r = Physical.move(lib, Arrays.asList(c), dest);
        lib.flush(5000);
        String moved = new File(dest, "היגיון.txt").getAbsolutePath();
        check("העברה: הקובץ ביעד", new File(moved).isFile() && !new File(c).exists());
        check("העברה: האינדקס בשני הקצוות", !has(lib, c) && has(lib, moved));
        mk(new File(root, "Documents"), "היגיון.txt", 3);
        r = Physical.move(lib, Arrays.asList(new File(root, "Documents/היגיון.txt").getAbsolutePath()), dest);
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
        check("82: שורש הכונן אסור", Physical.forbidden(lib, root) != null);
        check("82: סל המחזור אסור", Physical.forbidden(lib, new File(root, Index.TRASH)) != null);
        check("82: תיקיית התוכנה אסורה", Physical.forbidden(lib, Paths.data()) != null);
        check("82: תיקייה רגילה מותרת", Physical.forbidden(lib, new File(books)) == null);
        Physical.Result r = Physical.delete(lib, NONE, P(root.getAbsolutePath()), false);
        check("82: מחיקת שורש נכשלת, והכונן שלם", r.failed == 1 && new File(fb).isFile());

        // מחיקת תיקייה — כולה לסל, הרשימות לפי 58
        r = Physical.delete(lib, NONE, P(books), true);
        lib.flush(5000);
        check("82: מחיקת תיקייה — בסל", !new File(books).exists() && new File(r.to.get(books), "משנה/א.pdf").isFile());
        check("82: מחיקת תיקייה — האינדקס בשני הקצוות", !has(lib, fa) && has(lib, r.to.get(books) + "/משנה/א.pdf"));
        check("82: קובץ מתוך תיקייה שנמחקה — \"בסל\"", Trash.isInTrash(lib, fa));
        eq("82: הודעה", "נמחקה תיקייה אחת (2 קבצים)", Physical.message("נמחקו", "הוסרו", r));
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
        check("82: ביטול — הודעה", Physical.message("הועתקו", "הוסרו", r).contains("בוטל"));

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
        final Map<String, String> m = new java.util.HashMap<String, String>();
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
        try { Thread.sleep(200); } catch (InterruptedException ignored) { }
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
        check("90: קובץ רגיל אינו קובץ התוכנה", Physical.programFile(book.getAbsolutePath()) == null);
        lib.flush(5000);

        // 90ב: קובץ קיים שלא נקרא — לא שומרים
        File bad = new File(home, "מבנה-לא-נקרא.txt");
        wipe(bad);
        bad.mkdirs();                                   // קיים, והקריאה נכשלת
        Ini ini = Ini.load(bad);
        check("90: קובץ הגדרות שלא נקרא מסומן", ini.unreadable);
        check("90: שמירה של קובץ הגדרות שלא נקרא נדחית", !ini.saveQuiet() && bad.isDirectory());
        wipe(bad);

        // 90ג: הגיבוי האחרון
        File bdir = new File(Paths.backups(), "מבנה-בדיקה");
        wipe(bdir);
        bdir.mkdirs();
        Files.writeAtomic(new File(bdir, "2026-01-01_10-00-00-000-מבנה.txt"), "ישן");
        Files.writeAtomic(new File(bdir, "2026-03-01_10-00-00-000-מבנה.txt"), "חדש");
        Files.writeAtomic(new File(bdir, "2026-04-01_10-00-00-000-אחר.txt"), "לא שלו");
        File last = Files.latestBackup(new File("מבנה.txt"), "מבנה-בדיקה");
        eq("90: הגיבוי האחרון של הקובץ", "חדש", last == null ? null : Files.readUtf8(last));
        wipe(bdir);
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
        PickerModel m = new PickerModel(src, roots, null, null);
        eq("106: הכוננים הם נקודות הפתיחה", PickerModel.U, m.mode);
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
        PickerModel bm = new PickerModel(new IndexSource(bd), br, null, null);
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
        eq("84: שם ריק", "צריך שם", Physical.badName("  "));
        check("84: \"/\" בשם — נדחה", Physical.badName("א/ב.pdf") != null);
        check("84: נקודה בלבד — נדחה", Physical.badName("..") != null);
        eq("84: שם תקין בעברית", null, Physical.badName("מסילת ישרים.pdf"));

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
        File dest = Zip.target(arrived);
        eq("116: היעד — תיקייה בשם הקובץ", "ספרים", dest.getName());
        Zip.Result u = Zip.unpack(arrived, dest, null);
        check("116: נפרס", u.error == null && u.files == 4 && u.skipped == 0);
        check("116: קובץ ה-ZIP נשאר", arrived.isFile());

        // פעם שנייה — שם תפוס, ובלי הכפלה של התיקייה העליונה
        File dest2 = Zip.target(arrived);
        eq("116: יעד תפוס — \"(2)\"", "ספרים (2)", dest2.getName());
        Zip.unpack(arrived, dest2, null);

        // האינדקס לומד על מה שנפרס
        Index ix = new Index("107", "107", root.getAbsolutePath());
        ix.scan(null);
        Library lib = Library.forTests(new ArrayList<Index>(java.util.Collections.singletonList(ix)));
        File dest3 = Zip.target(arrived);
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
        File ld = Zip.target(loose);
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
        check("116: תיקיית האריזות מוגנת (בתוך תיקיית התוכנה)", Physical.programFile(new File(Acts.sendDir(), "x.zip").getAbsolutePath()) != null);
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
        java.util.Map<String, String> vars = Batch.vars(here, root.getAbsolutePath(), null);
        Batch.Plan p = Batch.plan("RD \"" + work + "\" /S\nMD \"" + work + "/נוכחי\"\nCOMP \"" + src + "\" \"" + work + "/נוכחי\"\n"
                + "COPY %רק-במקור% \"" + work + "/נוכחי\"", here, vars);
        eq("מ0.27: בלי שגיאות", 0, p.errors);
        check("מ0.27: MD אחרי RD /S — רץ (לא \"כבר קיימת\")", !p.steps.get(1).noop);
        eq("מ0.27: COMP אחרי RD /S — מול תיקייה ריקה: הקובץ רק במקור", 1, p.steps.get(3).files.size());
    }

    private static void batchRun() throws Exception {
        // קריאת שורה, כמו cmd
        eq("117א: מרכאות שומרות רווחים", "[MOVE, %הורדות%\\ספר חדש.pdf, יעד, /Y]",
                Batch.tokens("MOVE \"%הורדות%\\ספר חדש.pdf\" יעד /Y").toString());
        check("117א: מתג מוכר", Batch.isSwitch("/Y") && Batch.isSwitch("/-y") && Batch.isSwitch("/תאריך:יום") && Batch.isSwitch("/MAXAGE:3"));
        check("117א: מסלול אינו מתג", !Batch.isSwitch("/storage/emulated/0") && !Batch.isSwitch("/ספרים"));
        check("117א: תבנית", Batch.wild("*.pdf", "ספר.PDF") && !Batch.wild("*.pdf", "ספר.epub") && Batch.wild("פרק ?.txt", "פרק 3.txt")
                && Batch.wild("*.*", "README"));

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
        java.util.Map<String, String> vars = Batch.vars(here, root.getAbsolutePath(), null);

        // משתנים ומסלולים
        String[] err = {null};
        eq("117א: משתנה ו-\\", dl.getAbsolutePath() + "/x.pdf", Batch.path("%הורדות%\\x.pdf", here, vars, err));
        eq("117א: מסלול יחסי — ביחס לתיקייה של הקובץ", new File(here, "משנה").getAbsolutePath(), Batch.path("משנה", here, vars, err));
        check("117א: משתנה שאינו מוכר", Batch.path("%כרטיס%\\x", here, vars, err) == null && err[0].contains("כרטיס"));

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
        File dl186 = new File(vars.get(Batch.V_DOWNLOADS));
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

    // ---------- קובצי פקודות: השורה כשדות לטופס (117א, שלב 2) ----------

    private static void batchForm() {
        BatchLine b = BatchLine.parse("MOVE \"%הורדות%\\*.pdf\" \"%פנימי%\\ספרים חדשים\" /תאריך:שבוע /גודל:>10MB /Y");
        eq("שלב 2: פקודה", "MOVE", b.cmd);
        eq("שלב 2: תיקיית מקור", "%הורדות%", b.srcDir);
        eq("שלב 2: תבנית", "*.pdf", b.mask);
        eq("שלב 2: יעד", "%פנימי%\\ספרים חדשים", b.dest);
        check("שלב 2: תאריך — מאז שבוע", b.dateMode == 1 && b.dateValue.equals("שבוע"));
        check("שלב 2: גודל — גדול מ־10MB", b.sizeMode == 1 && b.sizeNumber.equals("10") && b.sizeUnit.equals("MB"));
        eq("שלב 2: /Y — להחליף", 2, b.taken);
        eq("שלב 2: חזרה לשורה — יעד עם רווח במרכאות", "MOVE %הורדות%\\*.pdf \"%פנימי%\\ספרים חדשים\" /תאריך:שבוע /גודל:>10MB /Y", b.toLine());
        eq("שלב 2: פירוק של השורה שנבנתה נותן אותה שורה", b.toLine(), BatchLine.parse(b.toLine()).toLine());

        BatchLine d = BatchLine.parse("del %הורדות%\\*.* /תאריך:<שנה /MAXAGE:3 /Q");
        eq("שלב 2: אותיות קטנות — DEL", "DEL", d.cmd);
        check("שלב 2: לפני שנה", d.dateMode == 2 && d.dateValue.equals("שנה"));
        eq("שלב 2: מתגים שהטופס לא מכיר — נשמרים", "[/MAXAGE:3, /Q]", d.other.toString());
        eq("שלב 2: DEL חוזר", "DEL %הורדות%\\*.* /תאריך:<שנה /MAXAGE:3 /Q", d.toLine());
        d.dateMode = 0;
        d.sizeMode = 2;
        d.sizeNumber = "500";
        d.sizeUnit = "KB";
        eq("שלב 2: שינוי בטופס — השורה מתעדכנת", "DEL %הורדות%\\*.* /גודל:<500KB /MAXAGE:3 /Q", d.toLine());

        BatchLine c = BatchLine.parse("COPY \"%כאן%\\ספר.pdf\" גיבוי");
        check("שלב 2: קובץ אחד בלי תבנית", c.mask.isEmpty() && c.srcDir.equals("%כאן%\\ספר.pdf"));
        eq("שלב 2: /תפוס:לדלג", "COPY a b /תפוס:לדלג", BatchLine.parse("COPY a b /תפוס:לדלג").toLine());
        eq("שלב 2: RD /S", "RD \"%פנימי%\\תיקייה ישנה\" /S", BatchLine.parse("RMDIR \"%פנימי%\\תיקייה ישנה\" /S").toLine());
        eq("שלב 2: REN", "REN \"ספר ישן.pdf\" חדש.pdf", BatchLine.parse("RENAME \"ספר ישן.pdf\" חדש.pdf").toLine());
        eq("שלב 2: ECHO שומר את הטקסט", "ECHO העברה הסתיימה!", BatchLine.parse("@echo העברה הסתיימה!").toLine());
        eq("שלב 2: START", "START %פנימי%\\ספרים", BatchLine.parse("START %פנימי%\\ספרים").toLine());
        BatchLine n = new BatchLine();
        n.cmd = "COPY";
        n.srcDir = "%הורדות%";
        n.mask = "*.epub";
        n.dest = "%כרטיס%\\ספרים";
        n.dateMode = 1;
        n.dateValue = "3ימים";
        eq("שלב 2: שורה חדשה מהטופס", "COPY %הורדות%\\*.epub %כרטיס%\\ספרים /תאריך:3ימים", n.toLine());
        Batch.Plan p = Batch.plan(n.toLine(), home, Batch.vars(home, "/storage/emulated/0", "/storage/AAAA-BBBB"));
        check("שלב 2: השורה מהטופס מובנת למנוע (בלי שגיאת תחביר)", p.steps.size() == 1
                && (p.steps.get(0).error == null || !p.steps.get(0).error.contains("לא הובן")));

        java.util.Map<String, String> vars = Batch.vars(new File("/storage/emulated/0/קיצורי דרך"), "/storage/emulated/0", "/storage/AAAA-BBBB");
        eq("שלב 2: מסלול מהבורר — המשתנה הארוך ביותר", "%הורדות%\\ספרים", BatchLine.toVar("/storage/emulated/0/Download/ספרים", vars));
        eq("שלב 2: פנימי", "%פנימי%\\ספרים\\חדשים", BatchLine.toVar("/storage/emulated/0/ספרים/חדשים", vars));
        eq("שלב 2: כרטיס", "%כרטיס%", BatchLine.toVar("/storage/AAAA-BBBB", vars));
        eq("שלב 2: מחוץ למשתנים — כמו שהוא, עם \\", "\\mnt\\x", BatchLine.toVar("/mnt/x", vars));
        check("שלב 2: הסבר לכל פקודה", !BatchLine.help("MOVE").isEmpty() && BatchLine.help("FORMAT").isEmpty());
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
        java.util.Map<String, String> vars = Batch.vars(root, root.getAbsolutePath(), null);

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
        Batch.Plan again = Batch.plan("MD \"%כאן%\\לשלוח\"\nMD \"%כאן%\\לשלוח\"", land2, Batch.vars(land2, root.getAbsolutePath(), null));
        check("MD לתיקייה שתיווצר/קיימת — בלי שגיאה", again.errors == 0 && again.changes() == 1);
        check("1.9.0: קובץ פקודות — רק vbat", Batch.isBatch("א.VBAT") && !Batch.isBatch("סידור — לשלוח.vbat.txt")
                && !Batch.isBatch("הערות.txt"));
        check("1.9.0: /חתימה: — מתג לא מוכר", Batch.plan("COPY %כאן%\\א.pdf %כאן% /חתימה:2:abc", land2, Batch.vars(land2, root.getAbsolutePath(), null)).errors == 1);

        // 139: %~dp0 ו-ECHO.
        String[] err = {null};
        eq("139: %~dp0 — התיקייה של הקובץ", new File(root, "x.pdf").getAbsolutePath(), Batch.path("%~dp0x.pdf", root, vars, err));
        eq("139: %~dp0\\ — גם עם לוכסן", new File(root, "x.pdf").getAbsolutePath(), Batch.path("%~dp0\\x.pdf", root, vars, err));
        Batch.Plan e = Batch.plan("ECHO.\nECHO שלום", root, vars);
        check("139: ECHO. — מוכר", e.errors == 0 && e.echoes.size() == 2);

        // 122: הטופס לא משנה שורה בשקט
        eq("122: /S ב-DEL נשמר", "DEL %הורדות%\\*.pdf /S", BatchLine.parse("DEL %הורדות%\\*.pdf /S").toLine());
        eq("122: /S ב-COPY נשמר", "COPY a b /S", BatchLine.parse("COPY a b /S").toLine());
        eq("122: גודל בלי סימן — נשמר כמו שהוא", "DEL a /גודל:10MB", BatchLine.parse("DEL a /גודל:10MB").toLine());
        BatchLine bytes = BatchLine.parse("DEL a\\*.* /גודל:<500");
        int ui = java.util.Arrays.asList(BatchLine.SIZE_UNITS).indexOf(bytes.sizeUnit);
        bytes.sizeUnit = BatchLine.SIZE_UNITS[ui < 0 ? 2 : ui];      // מה שהטופס עושה בשמירה בלי שינוי
        eq("122(ג): בתים נשארים בתים אחרי הטופס", "DEL a\\*.* /גודל:<500", bytes.toLine());

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
        eq("124: שם תפוס — \"פרק 1.17 (2)\"", "פרק 1.17 (2)", Physical.freeName(d, "פרק 1.17", false));
        eq("124: שם תפוס — \"ספר (2).pdf\"", "ספר (2).pdf", Physical.freeName(d, "ספר.pdf", false));
        eq("124: שם תפוס — קובץ מוסתר", ".nomedia (2)", Physical.freeName(d, ".nomedia", false));
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
        PickerModel pm = new PickerModel(new IndexSource(dv), rts, null, null);
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

        // 129: שם אופן — אותו כלל של שם בכונן
        check("129: שם אופן עם \":\" נדחה", Modes.create("בדיקות: מקיפות", null) != null);
        check("129: שם אופן עם \"?\" נדחה", Modes.create("מה?", null) != null);
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
        eq("128: חזרה לתחביר החיפוש", "תאריך:<שנה גודל:>10MB", d.toCond());
        check("128: תאריך שני — לא נקלט (כבר יש)", !d.take("תאריך:יום"));
        DateSize e = new DateSize();
        check("128: גודל בלי סימן — לא נקלט, לא מנחשים", !e.take("גודל:10MB") && e.sizeMode == 0);
        check("128: בתים", e.take("גודל:<500") && e.sizeUnit.equals("B") && e.sizeToken().equals("גודל:<500"));
        DateSize f = new DateSize();
        check("128: \"מאז\" עם >=", f.take("תאריך:>=2026-09-01") && f.dateMode == 1 && f.dateValue.equals("2026-09-01"));
        eq("128: ריק — כלום", "", new DateSize().toCond());
        // התנאים של הרשימות החכמות הישנות (מהרשימות הקבועות) — נקלטים
        for (String old : new String[]{"תאריך:יום", "תאריך:שבוע", "תאריך:חודש", "תאריך:שנה", "גודל:>1MB", "גודל:>10MB", "גודל:>100MB", "גודל:<1MB"})
            check("128: תנאי ישן נקלט — " + old, new DateSize().take(old));
        // אותו תחביר בקובצי הפקודות
        eq("128: שורת פקודה דרך DateSize", "DEL a\\*.* /תאריך:<שנה /גודל:<500", BatchLine.parse("DEL a\\*.* /תאריך:<שנה /גודל:<500").toLine());
        Cond c = Cond.parse("סוג:pdf " + d.toCond());
        check("128: התנאי מובן לחיפוש", c.unknown.isEmpty() && c.tests.size() == 3);

        // 141: שמות בחלון האישור
        String n = Physical.namesOf(P("/x/א.pdf", "/x/ב.pdf"), P("/x/ספרים"), 12);
        check("141: תיקייה קודם, ואחריה הקבצים", n.indexOf("📁 ספרים") < n.indexOf("א.pdf") && n.contains("ב.pdf"));
        List<String> many = new ArrayList<String>();
        for (int i = 0; i < 30; i++) many.add("/x/ק" + i + ".pdf");
        String m = Physical.namesOf(many, new ArrayList<String>(), 12);
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
        // קובצי פקודות — הסבר אחד לכל פקודה, גם בעורך וגם בשגיאות
        for (String c : BatchLine.COMMANDS)
            check("131: הסבר ושימוש — " + c, BatchLine.help(c).startsWith(c + " — ") && BatchLine.help(c).endsWith(Words.Cmd.usage(c)));
        check("131: פקודה לא מוכרת — בלי הסבר", BatchLine.help("XCOPY").isEmpty());
        Batch.Plan p = Batch.plan("RD\n", new File("/x"), Batch.vars(new File("/x"), "/p", null));
        check("131: שגיאת שימוש — מהמילון", p.steps.size() == 1 && Words.Cmd.usage("RD").equals(p.steps.get(0).error));
        // התחביר במקום אחד: התבנית והטופס מבינים זה את זה
        String t = Batch.template();
        check("131: התבנית — המשתנים מהתחביר", t.contains(Batch.var(Batch.V_HERE)) && t.contains(Batch.var(Batch.V_DOWNLOADS)));
        check("131: התבנית — שם תפוס מהתחביר", t.contains(Batch.SW_TAKEN + Batch.TAKEN_SKIP));
        check("131: תאריך וגודל — מ-DateSize", Batch.SW_DATE.equals(DateSize.DATE) && Batch.SW_SIZE.equals(DateSize.SIZE));
        BatchLine b = BatchLine.parse("COPY a b " + Batch.SW_TAKEN + Batch.TAKEN_SKIP);
        check("131: /תפוס:לדלג — נקלט ונכתב בחזרה", b.taken == 1 && b.toLine().endsWith(Batch.SW_TAKEN + Batch.TAKEN_SKIP));
        // שמות בכונן — מ-Paths

        String src = System.getProperty("vfm.src");
        if (src == null) return;
        File dir = new File(src, "src/com/tikiyot/vfm");
        // הקבצים שעברו (131; 1.7.78 — הפעולות בכונן; 1.7.79 — המסכים): כל הודעה מהמילון. מותר רק תחביר ושמות בכונן (מסומנים), ויומן.
        // 185 (1.7.82): כל הקבצים, חוץ מהמילון עצמו. תחביר ושמות בכונן — מסומנים בשורה ("// תחביר", "// בכונן")
        java.util.Set<String> notYet = new java.util.HashSet<String>(Arrays.asList("Words"));
        File[] srcs = dir.listFiles();
        List<String> names = new ArrayList<String>();
        if (srcs != null) for (File x : srcs)
            if (x.getName().endsWith(".java") && !notYet.contains(x.getName().replace(".java", ""))) names.add(x.getName().replace(".java", ""));
        Collections.sort(names);
        check("185: הבדיקה עוברת על כל הקבצים (" + names.size() + ")", names.size() > 60);
        for (String name : names) {
            List<String> loose = new ArrayList<String>();
            try {
                for (String[] l : literals(Files.readUtf8(new File(dir, name + ".java"))))
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
        File[] all = dir.listFiles();
        for (String w : once) {
            List<String> where = new ArrayList<String>();
            if (all != null)
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
    private static void v1230() {
        Commands.Def cp = Commands.get("COPY");
        Commands.Param src = cp.positional().get(0), dst = cp.positional().get(1), keep = cp.switchFor("/S"),
                taken = cp.switchFor(Batch.SW_TAKEN), journal = cp.switchFor(Batch.SW_JOURNAL), limit = cp.switchFor(Batch.SW_LIMIT),
                date = cp.switchFor("/" + Batch.SW_DATE);
        String raw = "COPY   \"%פנימי%\\א ב\"  x /Q";
        check("1.23.0: שורה שלא שונתה — אות באות", CmdLine.parse(raw).text().equals(raw));
        CmdLine c = CmdLine.parse(raw);
        check("1.23.0: ערכים לפי מקום", c.value(src).equals("%פנימי%\\א ב") && c.value(dst).equals("x"));
        c.set(dst, "%כרטיס%\\ג ד");
        check("1.23.0: שינוי יעד — מרכאות, ו/Q נשאר", c.text().equals("COPY \"%פנימי%\\א ב\" \"%כרטיס%\\ג ד\" /Q"));
        CmdLine d = CmdLine.parse("COPY a b /תאריך:שבוע");
        d.set(keep, "1");
        check("1.23.0: מתג חדש — במקומו לפי ההגדרה", d.text().equals("COPY a b /S /תאריך:שבוע"));
        d.set(journal, "x y.txt");
        check("1.23.0: ערך עם רווח — במרכאות", d.text().equals("COPY a b /S /יומן:\"x y.txt\" /תאריך:שבוע"));
        check("1.23.0: הערך חוזר כמו שנקבע", CmdLine.parse(d.text()).value(journal).equals("x y.txt"));
        d.set(keep, "");
        check("1.23.0: מתג כבוי — יוצא", d.text().equals("COPY a b /יומן:\"x y.txt\" /תאריך:שבוע"));
        CmdLine y = CmdLine.parse("COPY a b /Y");
        check("1.23.0: /Y = להחליף", y.value(taken).equals(Batch.TAKEN_REPLACE));
        y.set(taken, Batch.TAKEN_SKIP);
        check("1.23.0: /Y ← /תפוס:לדלג (באותו מקום)", y.text().equals("COPY a b " + Batch.SW_TAKEN + Batch.TAKEN_SKIP));
        check("1.23.0: /מסלול = /S", CmdLine.parse("COPY a b " + Batch.SW_KEEP).value(keep).equals("1"));
        CmdLine l = CmdLine.parse("MOVE %רק-ביעד% h /עד:20%");
        Commands.Param mlimit = Commands.get("MOVE").switchFor(Batch.SW_LIMIT);
        check("1.23.0: אחוז", l.value(mlimit).equals("20"));
        l.set(mlimit, "30");
        check("1.23.0: אחוז — עם %", l.text().equals("MOVE %רק-ביעד% h /עד:30%") && l.usesCompare() && l.destructive());
        Commands.Def comp = Commands.get("COMP");
        CmdLine k = CmdLine.parse("COMP /רשימה:גיבוי \"%כרטיס%\\גיבוי\\נוכחי\"");
        check("1.23.0: COMP /רשימה: — המקור לא רלוונטי, היעד ראשון",
                k.slot(comp.positional().get(0)) < 0 && k.value(comp.positional().get(1)).equals("%כרטיס%\\גיבוי\\נוכחי") && k.missing().isEmpty());
        CmdLine n = CmdLine.of(cp);
        check("1.23.0: פקודה חדשה — חסרים מקור ויעד", n.missing().size() == 2 && n.text().equals("COPY"));
        n.set(dst, "b");
        check("1.23.0: יעד לפני מקור — מקום ריק למקור", n.text().equals("COPY \"\" b"));
        n.set(src, "a");
        check("1.23.0: ואז המקור במקומו", n.text().equals("COPY a b") && n.missing().isEmpty());
        n.set(limit, "20");
        n.set(date, "שבוע");
        n.set(taken, Batch.TAKEN_STOP);
        check("1.23.0: כמה מתגים — בסדר ההגדרה", n.text().equals("COPY a b " + Batch.SW_TAKEN + Batch.TAKEN_STOP + " /עד:20% /תאריך:שבוע"));
        Commands.Param txt = Commands.get("ECHO").positional().get(0);
        CmdLine e = CmdLine.parse("ECHO שלום  עולם");
        check("1.23.0: ECHO — הטקסט כמו שהוא", e.value(txt).equals("שלום  עולם"));
        e.set(txt, "חדש");
        check("1.23.0: ECHO — שינוי", e.text().equals("ECHO חדש"));
        check("1.23.0: שורה לא מוכרת — בלי הגדרה, נשמרת", CmdLine.parse("FORMAT C:").def == null && CmdLine.parse("FORMAT C:").text().equals("FORMAT C:"));
        check("1.23.0: פועל — לא פקודת קבצים", CmdLine.parse("פתח מסך חיפוש").def == null);
        check("1.23.0: חיפוש בתפריט — העתק / CO / שינוי", Commands.matches(cp, "העתק") && Commands.matches(cp, "co")
                && Commands.matches(Commands.get("REN"), "שנה") && !Commands.matches(cp, "מחק"));

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
            for (String fl : filled) ok &= CmdLine.parse(fl).def != null || Batch.isLoopStart(fl) || Batch.isLoopEnd(fl)
                    || fl.startsWith(Batch.MEASURE) || fl.startsWith("::") || fl.equals(Batch.CHECK);             // 1.25.0: שורות מבנה; הערה
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
            File cpy = CmdTemplates.copyToMine(ts.get(0));                      // 1.25.3
            check("1.25.3: עריכת נפוצה של התוכנה — עותק אצל שלי", cpy != null && cpy.getName().contains("(שלי)")
                    && cpy.length() > 0);
            CmdTemplates.Tpl first = CmdTemplates.mine().get(0);
            File rn = CmdTemplates.rename(first, "שם חדש");
            check("1.25.3: שינוי שם לשלי", rn != null && rn.isFile() && !first.file.exists());
            check("1.25.3: שם תפוס — לא", CmdTemplates.rename(CmdTemplates.mine().get(0), Text.stem(CmdTemplates.mine().get(1).file.getName())) == null);
        } finally {
            CmdTemplates.testRoot = null;
        }
    }

    /** 1.22.5: ההגדרה האחת — שלמה, תואמת למנוע, וכל פרמטר עם שם והסבר. */
    private static void v1225() {
        check("1.22.5: פקודות הקבצים — אותן ובאותו סדר (מ0.14: ודא)",
                java.util.Arrays.equals(BatchLine.COMMANDS, new String[]{"COPY", "MOVE", "DEL", "REN", "MD", "RD", "COMP", "LINK", "VERIFY", "MODE", "CALL", "START", "ECHO", "REM"}));
        check("1.22.5: שמות נרדפים", Commands.canonical("MKDIR").equals("MD") && Commands.canonical("rmdir").equals("RD")
                && Commands.canonical("ERASE").equals("DEL") && Commands.canonical("RENAME").equals("REN")
                && Commands.canonical("copy").equals("COPY") && Commands.canonical("FORMAT").equals("FORMAT")
                && Commands.canonical("ECHO.").equals("ECHO."));
        check("1.22.5: שם נרדף בטופס", BatchLine.parse("MKDIR \"%פנימי%\\x\"").cmd.equals("MD"));
        for (Commands.Def d : Commands.ALL) {
            check("1.22.5: מה עושה — " + d.name, !d.what.isEmpty());
            check("1.22.5: שימוש — " + d.name, !d.usage().isEmpty() && (d.family != Commands.Family.FILE || d.usage().startsWith(d.name)));
            for (Commands.Param p : d.params) {
                boolean ok = !p.label.isEmpty() && !p.help.isEmpty() && (!p.isSwitch() || Batch.isSwitch(p.key))
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
            check("1.22.5: מתג בהגדרה — " + x[0] + " " + x[1], Batch.isSwitch(x[1]) && d.switchFor(x[1]) != null);
        }
        check("1.22.5: COPY בלי /רשימה:, COMP בלי /מצב:", Commands.get("COPY").switchFor("/רשימה:x") == null
                && Commands.get("COMP").switchFor("/מצב:1.1.2026") == null);
        String u = Words.Cmd.usage("COPY");
        check("1.22.5: שימוש עם המתגים עצמם", u.contains(Batch.SW_TAKEN) && u.contains(Batch.SW_JOURNAL) && !u.contains("/MAXAGE"));
        check("1.22.5: COMP — שתי הצורות", Words.Cmd.usage("COMP").contains(Batch.SW_LIST));
        check("1.22.5: חזור — שימוש משלו", Words.Verb.usage("חזור").contains("חזור") && !Words.Verb.usage("חזור").contains("פתח"));
        check("1.22.5: צור פקודה ופתח תפריט בשימוש", Words.Verb.usage("צור").contains("פקודה") && Words.Verb.usage("פתח").contains("תפריט"));
        check("1.22.5: הדמיה — כותרת", Commands.get("בכאילו") != null && Commands.get("בכאילו").family == Commands.Family.HEADER);
        File rr = drive("שינוי-שם-תיקייה");
        new File(rr, "תיקייה").mkdirs();
        Batch.Plan pr = Batch.plan("REN \"" + new File(rr, "תיקייה").getAbsolutePath() + "\" חדש", rr, new java.util.HashMap<String, String>());
        check("1.22.5: REN על תיקייה — הודעה ברורה", pr.errors == 1 && Words.Cmd.REN_FOLDER.equals(pr.steps.get(0).error));
        check("1.22.5: שימוש — ערך המתג לפי סוגו", Words.Cmd.usage("COPY").contains(Batch.SW_JOURNAL + Words.Def.V_FILE)
                && Words.Cmd.usage("COPY").contains(Batch.SW_STATE + Words.Def.V_TIME));
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
        Conf.set(Conf.GENERAL, SmartLists.PRESETS_DONE, "");
        File tf = new File(home, "מבנה-1220.txt");
        tf.delete();
        try {

            String text = "MD \"גיבוי\\נוכחי\"\n"
                    + "COMP /רשימה:\"המסמכים שלי\" \"גיבוי\\נוכחי\"\n"
                    + "MOVE %רק-ביעד% \"גיבוי\\היסטוריה\\%תאריך%\" /עד:50%\n"
                    + "COPY %רק-במקור% \"גיבוי\\נוכחי\" /תפוס:לעצור /יומן:\"גיבוי\\יומן\\%תאריך%.txt\"\n";
            Map<String, String> vars = Batch.vars(r1, r1.getAbsolutePath(), null);
            vars.put(Batch.V_NOW, "01.10.2026 10-00-00");
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
            vars.put(Batch.V_NOW, "05.10.2026 10-00-00");
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
            eq("1.22.0: הטופס — COMP עם רשימה נשמר", "COMP גיבוי\\נוכחי /רשימה:x", BatchLine.parse("COMP /רשימה:x \"גיבוי\\נוכחי\"").toLine());
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
            String line = BatchLine.parse("COMP /רשימה:\"המסמכים שלי\" יעד /יומן:\"גיבוי\\יומן חדש.txt\"").toLine();
            check("1.22.0: הטופס — ערך עם רווח נשאר במרכאות", line.contains("/רשימה:\"המסמכים שלי\"") && line.contains("/יומן:\"גיבוי\\יומן חדש.txt\"")
                    && BatchLine.parse(line).toLine().equals(line));
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
        String text = "MD \"גיבוי\\נוכחי\"\n"
                + "COMP מסמכים \"גיבוי\\נוכחי\"\n"
                + "MOVE %רק-ביעד% \"גיבוי\\היסטוריה\\%תאריך%\"\n"
                + "COPY %רק-במקור% \"גיבוי\\נוכחי\" /תפוס:לעצור /יומן:\"גיבוי\\יומן\\%תאריך%.txt\"\n";
        long base = 1700000000000L;
        // גיבוי 1
        write(docs, "a.txt", "v1").setLastModified(base);
        write(docs, "b.txt", "b").setLastModified(base);
        write(new File(docs, "dir"), "c.txt", "c").setLastModified(base);
        Map<String, String> vars = Batch.vars(root, root.getAbsolutePath(), null);
        vars.put(Batch.V_NOW, "01.10.2026 10-00-00");
        Batch.Plan p = Batch.plan(text, root, vars, lib);
        Batch.run(p, lib, null, null);
        lib.flush(5000);
        // גיבוי 2: a נערך, b נמחק, d חדש
        write(docs, "a.txt", "v2").setLastModified(base + 1000);
        new File(docs, "b.txt").delete();
        write(docs, "d.txt", "d").setLastModified(base);
        vars.put(Batch.V_NOW, "05.10.2026 10-00-00");
        Batch.run(Batch.plan(text, root, vars, lib), lib, null, null);
        lib.flush(5000);
        // גיבוי 3: a שוב, c שינה שם
        write(docs, "a.txt", "v3").setLastModified(base + 2000);
        new File(docs, "dir/c.txt").renameTo(new File(docs, "dir/c2.txt"));
        vars.put(Batch.V_NOW, "10.10.2026 10-00-00");
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
        String text = "MD \"גיבוי\\נוכחי\"\n"
                + "COMP מסמכים \"גיבוי\\נוכחי\"\n"
                + "MOVE %רק-ביעד% \"גיבוי\\היסטוריה\\%תאריך%\" /עד:50%\n"
                + "COPY %רק-במקור% \"גיבוי\\נוכחי\" /תפוס:לעצור /יומן:\"גיבוי\\יומן\\%תאריך%.txt\"\n";
        Map<String, String> vars = Batch.vars(root, root.getAbsolutePath(), null);
        Batch.Plan p = Batch.plan(text, root, vars, lib);
        check("1.20.0: גיבוי ראשון — בלי שגיאות", p.errors == 0);
        check("1.20.0: COMP — תיאור", p.steps.get(1).describe.contains("רק במקור 10"));
        check("1.20.0: COMP אינו \"שינוי\"", p.changes() == 3);
        List<String> rep = Batch.run(p, lib, null, null);
        lib.flush(5000);
        check("1.20.0: גיבוי ראשון — המראה זהה למקור", treeOf(docs).equals(treeOf(cur)));
        String t1 = vars.get(Batch.V_NOW);
        eq("1.20.0: גיבוי ראשון — יומן", 10, Physical.Journal.read(new File(back, "יומן/" + t1 + ".txt")).size());
        check("1.20.0: גיבוי ראשון — הדוח כולל את ההשוואה", rep.size() == 4 && rep.get(1).contains("השוואה"));

        // 3. שינויים: עריכה, מחיקה, שינוי שם, תיקייה ריקה חדשה, תיקייה שנמחקה
        write(new File(docs, "א"), "1.txt", "אחת — נערך");
        new File(docs, "3.txt").delete();
        new File(docs, "ב/ג/2.txt").renameTo(new File(docs, "ב/ג/שתיים.txt"));
        new File(docs, "חדשה/ריקה").mkdirs();
        new File(docs, "ריקה").delete();
        write(docs, "4.txt", "ארבע");
        vars = Batch.vars(root, root.getAbsolutePath(), null);
        vars.put(Batch.V_NOW, "0002");
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
        eq("1.20.0: יומן שני — רק מה שהגיע", 3, Physical.Journal.read(new File(back, "יומן/0002.txt")).size());

        // 4. בלי שינוי — כלום לא זז
        vars.put(Batch.V_NOW, "0003");
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
        eq("1.20.0: הטופס — COMP נשמר", "COMP מסמכים גיבוי", BatchLine.parse("COMP מסמכים גיבוי").toLine());
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
        Physical.Journal j = new Physical.Journal(jf, "בדיקה");
        Physical.Result r = Physical.transferKeep(lib, src.getAbsolutePath(),
                P(src + "/א/1.pdf", src + "/ב/ג/2.pdf", src + "/3.txt"), P(src + "/ריקה/פנימית"), dest, true, null, null, false, j);
        j.close();
        lib.flush(5000);
        check("1.19.0: שמירת מסלול — הכול במקומו", r.failed == 0 && new File(dest, "א/1.pdf").length() == 10
                && new File(dest, "ב/ג/2.pdf").length() == 20 && new File(dest, "3.txt").isFile());
        check("1.19.0: שמירת מסלול — תיקייה ריקה", new File(dest, "ריקה/פנימית").isDirectory());
        check("1.19.0: שמירת מסלול — המקור נשאר", new File(src, "א/1.pdf").isFile());
        List<String[]> lines = Physical.Journal.read(jf);
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
        Map<String, String> vars = Batch.vars(root, root.getAbsolutePath(), "/storage/ABCD-1234");
        check("1.19.0: %תאריך% — יום.חודש.שנה שעה-דקה-שנייה", vars.get(Batch.V_NOW).matches("\\d\\d\\.\\d\\d\\.\\d{4} \\d\\d-\\d\\d-\\d\\d"));
        eq("1.19.0: %מזהה-כרטיס%", "ABCD-1234", vars.get(Batch.V_CARD_ID));
        check("1.19.0: בלי כרטיס — בלי מזהה", Batch.vars(root, root.getAbsolutePath(), null).get(Batch.V_CARD_ID) == null);
        vars = Batch.vars(root, root.getAbsolutePath(), null);
        String now = vars.get(Batch.V_NOW);
        Batch.Plan p = Batch.plan("COPY מקור \"גיבוי2\\%תאריך%\" /S /יומן:\"גיבוי2\\יומן\\%תאריך%.txt\"", root, vars);
        check("1.19.0: COPY /S — בלי שגיאה", p.errors == 0 && p.steps.size() == 1);
        Batch.Step st = p.steps.get(0);
        check("1.19.0: COPY /S — שומר מסלול בתיאור", st.describe != null && st.describe.contains(Words.Cmd.KEEP_PATH.trim()));
        eq("1.19.0: COPY /S — כל הקבצים", 4, st.files.size());
        Batch.run(p, lib, null, null);
        lib.flush(5000);
        File g2 = new File(root, "גיבוי2/" + now);
        check("1.19.0: COPY /S — בוצע", new File(g2, "ב/ג/2.pdf").isFile() && new File(g2, "ריקה/פנימית").isDirectory());
        eq("1.19.0: /יומן: — נכתב בשם עם %תאריך%", 4, Physical.Journal.read(new File(root, "גיבוי2/יומן/" + now + ".txt")).size());
        // תבנית עם /מסלול — רק pdf, בכל העומק, בלי תיקיות ריקות
        p = Batch.plan("COPY מקור\\*.pdf גיבוי3 /מסלול", root, vars);
        check("1.19.0: /מסלול עם תבנית", p.errors == 0 && p.steps.get(0).files.size() == 3 && p.steps.get(0).dirs.isEmpty());
        // קובץ זמני של העתקה — לא נכלל
        mk(new File(src, "א"), Physical.partial(new File(src, "א/5.pdf")).getName(), 3);
        p = Batch.plan("COPY מקור גיבוי4 /S /A:H", root, vars);
        boolean partial = false;
        for (String f : p.steps.get(0).files) if (Physical.isPartial(new File(f).getName())) partial = true;
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
        // השורה בטופס — המתגים נשמרים
        BatchLine bl = BatchLine.parse("COPY מקור יעד /S /יומן:\"י\\1.txt\"");
        check("1.19.0: הטופס — /S ו־/יומן: נשמרים", bl.toLine().contains("/S") && bl.toLine().contains("/יומן:"));
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
        check("1.18.0: העתקה — הצליחה", Physical.copyFile(a, d1) && d1.length() == 30);
        check("1.18.0: העתקה — אין קובץ זמני", !Physical.partial(d1).exists());
        eq("1.18.0: העתקה — התאריך נשמר", 1500000000000L, d1.lastModified());
        check("1.18.0: השם הזמני — מוסתר ומזוהה", Physical.partial(d1).getName().startsWith(".")
                && Physical.isPartial(Physical.partial(d1).getName()) && !Physical.isPartial("א.pdf"));
        // שארית מהפסקה קודמת — נמחקת, ולא נשאר קובץ בשם האמיתי לפני הסוף
        File d2 = new File(dest, "ב.pdf");
        File left = mk(dest, Physical.partial(d2).getName(), 7);
        check("1.18.0: שארית — קיימת לפני", left.isFile() && !d2.exists());
        check("1.18.0: שארית — ההעתקה מצליחה ומנקה", Physical.copyFile(a, d2) && d2.length() == 30 && !left.exists());
        // יעד תפוס — לא נוגעים
        check("1.18.0: יעד תפוס — לא נדרס", !Physical.copyFile(mk(src, "ג.pdf", 5), d1) && d1.length() == 30);
        // מקור שאינו קובץ — אין שארית זמנית
        File d3 = new File(dest, "ד.pdf");
        check("1.18.0: מקור חסר — נכשל בלי שארית", !Physical.copyFile(new File(src, "אין.pdf"), d3)
                && !d3.exists() && !Physical.partial(d3).exists());
        // שם ארוך מאוד — השם הזמני מתקצר
        StringBuilder lng = new StringBuilder();
        for (int i = 0; i < 120; i++) lng.append('ש');
        File longDst = new File(dest, lng + ".pdf");
        try {
            check("1.18.0: שם ארוך — השם הזמני עד 255 בתים", Physical.partial(longDst).getName().getBytes("UTF-8").length <= 255);
        } catch (Exception e) {
            check("1.18.0: שם ארוך", false);
        }

        // 2. רק הפריטים העליונים
        File box = new File(src, "ארגז");
        mk(new File(box, "פנים"), "1.pdf", 11);
        mk(box, "2.pdf", 22);
        List<File> top = Physical.topOnly(P(box + "/2.pdf", box + "/פנים/1.pdf"), P(box.getAbsolutePath(), box.getAbsolutePath(), box + "/פנים"));
        check("1.18.0: רק העליון — תיקייה אחת", top.size() == 1 && top.get(0).getAbsolutePath().equals(box.getAbsolutePath()));
        List<File> sib = Physical.topOnly(P(src + "/ארגז2/x.pdf"), P(box.getAbsolutePath()));
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
        Physical.maxFileForTests = 50;
        try {
            r = Physical.transfer(lib, P(big.getAbsolutePath(), small.getAbsolutePath()), NONE, fat, true, null, null, false);
        } finally {
            Physical.maxFileForTests = 0;
        }
        check("1.18.0: קובץ גדול מהמותר — נכשל, השאר הועתק", r.failed == 1 && !new File(fat, "גדול.bin").exists()
                && new File(fat, "קטן.bin").isFile() && !Physical.partial(new File(fat, "גדול.bin")).exists());
        check("1.18.0: הודעה — 4GB", r.note.contains("4GB"));
        // סוג מערכת הקבצים מ-/proc/mounts
        List<String[]> m = new ArrayList<String[]>();
        m.add(new String[]{"/", "ext4"});
        m.add(new String[]{"/storage/emulated", "fuse"});
        m.add(new String[]{"/storage/ABCD-1234", "fuse"});
        m.add(new String[]{"/mnt/media_rw/ABCD-1234", "vfat"});
        m.add(new String[]{"/storage/EF01-2345", "fuse"});
        m.add(new String[]{"/mnt/media_rw/EF01-2345", "exfat"});
        eq("1.18.0: כרטיס FAT32", "vfat", Physical.fsTypeIn(m, "/storage/ABCD-1234/גיבוי/נוכחי"));
        eq("1.18.0: כרטיס exFAT", "exfat", Physical.fsTypeIn(m, "/storage/EF01-2345"));
        eq("1.18.0: פנימי", "fuse", Physical.fsTypeIn(m, "/storage/emulated/0/Download"));
        eq("1.18.0: נקודה דומה אינה תואמת", "ext4", Physical.typeAt(m, "/storage/emulatedX"));
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
        check("192: הסרת רשימות — כמה יוצא", Words.Manual.removesLists(2, 5, 340, 0).contains("2 רשימות")
                && Words.Manual.removesLists(2, 5, 340, 0).contains("5 רשימות־משנה") && Words.Manual.removesLists(2, 5, 340, 0).contains("340 פריטים"));
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
            eq("190: חתימה תקפה — אותו גודל ותאריך", "2:abc", Sig.valid("/x/קובץ.pdf", 5, 6));
            check("190: קובץ שהשתנה — החתימה אינה תקפה", Sig.valid("/x/קובץ.pdf", 5, 7) == null);
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
        check("189: גם אחרי נרמול (\"נוספ\")", Cond.parse(Text.norm("נוסף:שבוע")).matches(q, r));
        r.added = 0;
        check("189: לא ידוע — לא \"חדש\", כן \"ישן\"", !Cond.parse("נוסף:שבוע").matches(q, r)
                && Cond.parse("נוסף:<שבוע").matches(q, r));

        // הטופס
        DateSize ds = new DateSize();
        ds.allowAdded = true;
        check("189: הטופס קולט נוסף:", ds.take("נוסף:<חודש") && ds.added && ds.dateMode == 2 && ds.dateValue.equals("חודש"));
        eq("189: ובחזרה", "נוסף:<חודש", ds.toCond());
        DateSize batch = new DateSize();
        check("189: בקובץ פקודות — לא (נשאר כמו שהוא)", !batch.take("נוסף:שבוע"));
        ds.added = false;
        eq("189: בחירה \"של השינוי\" — תאריך:", "תאריך:<חודש", ds.toCond());
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
        Map<String, int[]> sum = Trial.summary(lines);
        check("182: סיכום — יומית: 2 פעמים, נמצאו 2, פעם אחת נמצא", Arrays.equals(sum.get(Words.Trial.DAILY), new int[]{2, 2, 1}));
        check("182: סיכום — שבועית: פעם אחת, נמצאו 4", Arrays.equals(sum.get(Words.Trial.WEEKLY), new int[]{1, 4, 1}));
        check("182: סיכום — מה השתנה מאז: נמצא 1", Arrays.equals(sum.get(Words.Trial.SINCE), new int[]{1, 1, 1}));
        check("182: סיכום במילים", Trial.text(lines).contains("רצה 2 פעמים, מצאה משהו פעם אחת"));
        eq("182: יומן ריק — הסבר", Words.Trial.EMPTY, Trial.text(new ArrayList<String>()));
        Trial.file().delete();
        Trial.add(Words.Trial.DAILY, 5, "נבדקו 1", 0);
        Trial.add(Words.Trial.WEEKLY, 5, "נוספו 0 · נמחקו 0 · השתנו 0", 0);
        eq("182: היומן בקובץ — שתי שורות", 2, Trial.lines().size());
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
        check("184: קובץ לפי מסלול — נמצא", lib.find(f1.getAbsolutePath()) != null && lib.exists(f1.getAbsolutePath()));
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
