package com.tikiyot.vfm;

// בדיקות מ0.2 על המחשב: ההעתקה מהמנוע הישן אל המסד — על MemStore ועל SqlStore (מול SQLite אמיתי), אותן בדיקות.
// המקור נכתב לקבצים אמיתיים (מבנה.txt, חתימות.txt) ונקרא בדיוק כמו במכשיר.

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class ImportTests {

    private final StoreTests.Out o;
    private final String label;

    ImportTests(StoreTests.Out o, String label) {
        this.o = o;
        this.label = label;
    }

    private void ck(String what, boolean ok) {
        if (ok) o.passed++;
        else o.failed.add(label + " — " + what);
    }

    private static void write(File f, String text) throws IOException {
        f.getParentFile().mkdirs();
        FileOutputStream out = new FileOutputStream(f);
        try {
            out.write(text.getBytes("UTF-8"));
        } finally {
            out.close();
        }
    }

    static final String A = "/storage/emulated/0/ספרים/א.pdf";
    static final String B = "/storage/emulated/0/ספרים/ב.pdf";
    static final String C = "/storage/ABCD-1234/ג.pdf";
    static final String D = "/storage/emulated/0/ספרים/ד.pdf";

    /** המקור הראשון: ארבע רשימות, שלושה קבצים, ומה שלא מועתק (שורה פגומה, הפניה, רשימה חכמה, תיקייה חיה). */
    static final String TREE1 = ""
            + "#1 ספרים\n"
            + "@צלמית 📘\n"
            + "@תיאור ספרי לימוד\n"
            + "@למה ללמוד\n"
            + "@סדר f" + B + "\n"
            + "@סדר cהלכה\n"
            + A + " | 100\n"
            + B + " | ?\n"
            + "שורה פגומה בלי נתיב\n"
            + "@רשימה מועדים / פסח\n"
            + "@חכמה s-abc\n"
            + "@תיקייה /storage/emulated/0/x\n"
            + "#2 ספרים / הלכה\n"
            + A + " | 100\n"
            + C + " | 5\n"
            + "#1 מועדים\n"
            + "#2 מועדים / פסח\n"
            + B + " | 7\n";

    static final String SIGS1 = ""
            + "2:00000000000000aa\t100\t1700\t" + A + "\n"
            + "1:ffffffffffffffff\t5\t9\t" + C + "\n"          // גרסה אחרת — מדלגים
            + "2:zz\tabc\t1\t" + B + "\n"                       // פגומה — מדלגים
            + "שורה פגומה\n";

    /** מקור מאוחר: קובץ חדש, חתימה לג׳, צלמית לרשימה שהייתה בלי, צלמית אחרת לרשימה שיש לה, רשימה חדשה, ו־א׳ יצא מהלכה. */
    static final String TREE2 = ""
            + "#1 ספרים\n"
            + "@צלמית 📕\n"
            + "@סדר f" + B + "\n"
            + "@סדר cהלכה\n"
            + A + " | 100\n"
            + B + " | ?\n"
            + "#2 ספרים / הלכה\n"
            + "@צלמית 🕯\n"
            + C + " | 5\n"
            + D + " | 40\n"
            + "#1 מועדים\n"
            + "#2 מועדים / פסח\n"
            + B + " | 7\n"
            + "#1 חדשה\n";

    static final String SIGS2 = SIGS1 + "2:00000000000000cc\t5\t9\t" + C + "\n";

    private static boolean same(Importer.Report a, Importer.Report b) {
        return a.listsNew == b.listsNew && a.listsKnown == b.listsKnown && a.filesNew == b.filesNew && a.filesKnown == b.filesKnown
                && a.filesUpdated == b.filesUpdated && a.filesWithSig == b.filesWithSig && a.linksAdded == b.linksAdded
                && a.linksKnown == b.linksKnown && a.rootFiles == b.rootFiles && a.propsSet == b.propsSet
                && a.badLines == b.badLines && a.refs == b.refs && a.smart == b.smart && a.live == b.live;
    }

    private static List<String> names(Store s, long list) {
        List<String> out = new ArrayList<String>();
        for (Store.Member m : s.members(list)) out.add(m.item.name);
        return out;
    }

    private static long listBy(Store s, String path) {
        return s.findByOrigin(Importer.originOf("עבודה", path));
    }

    private static long listIn(Store s, String mode, String path) {
        return s.findByOrigin(Importer.originOf(mode, path));
    }

    void run(Store s, File dir) throws Exception {
        File m1 = new File(dir, "v1/עבודה"), m2 = new File(dir, "v2/עבודה");   // אותו אופן בשתי גרסאות
        write(new File(m1, Paths.TREE), TREE1);
        write(new File(m1, Sig.FILE), SIGS1);
        write(new File(m2, Paths.TREE), TREE2);
        write(new File(m2, Sig.FILE), SIGS2);
        Importer.Source src1 = Importer.load(m1);

        // מקור: החתימות — רק תקינות ובגרסה הנוכחית
        ck("חתימות: נקראת רק שורה תקינה בגרסה הנוכחית", src1.sigs.size() == 1 && src1.sigs.get(A) != null
                && src1.sigs.get(A).size == 100 && src1.sigs.get(A).mtime == 1700 && src1.sigs.get(A).sig.equals("2:00000000000000aa"));

        // תצוגה מקדימה: הדוח מלא, והמסד נשאר ריק
        Importer.Report pre = Importer.run(s, src1, false);
        ck("תצוגה מקדימה: לא נשמר דבר", s.lists().isEmpty() && s.lastId() == 0 && s.findFile(A) == 0);
        ck("תצוגה מקדימה: ארבע רשימות חדשות, שלושה קבצים, חמישה קישורים", pre.listsNew == 4 && pre.listsKnown == 0
                && pre.filesNew == 3 && pre.linksAdded == 5 && pre.linksKnown == 0);
        ck("תצוגה מקדימה: מה שלא מועתק נספר", pre.badLines == 1 && pre.refs == 1 && pre.smart == 1 && pre.live == 1 && pre.skippedAny());
        ck("תצוגה מקדימה: קובץ עם חתימה נספר", pre.filesWithSig == 1);

        // העתקה
        Importer.Report r = Importer.run(s, src1, true);
        ck("העתקה: הדוח זהה לתצוגה המקדימה", same(pre, r));
        ck("העתקה: ארבע רשימות", s.lists().size() == 4);
        long books = listBy(s, "ספרים"), halacha = listBy(s, "ספרים / הלכה"), moadim = listBy(s, "מועדים"), pesach = listBy(s, "מועדים / פסח");
        ck("העתקה: כל רשימה נמצאה לפי מקורה", books != 0 && halacha != 0 && moadim != 0 && pesach != 0);
        ck("העתקה: השם הוא השם האחרון, לא המסלול", s.item(halacha).name.equals("הלכה") && s.item(pesach).name.equals("פסח"));
        ck("העתקה: תת־רשימה בתוך האב", s.listsOf(halacha).equals(Arrays.asList(books)) && s.listsOf(pesach).equals(Arrays.asList(moadim)));
        ck("העתקה: רשימה עליונה עומדת לעצמה", s.listsOf(books).isEmpty() && s.listsOf(moadim).isEmpty());
        ck("העתקה: הסדר החופשי — קובץ, רשימה, ואז מה שלא נזכר", names(s, books).equals(Arrays.asList("ב.pdf", "הלכה", "א.pdf")));
        ck("העתקה: צלמית, תיאור ו\"למה\"", "📘".equals(s.prop(books, Store.P_ICON)) && "ספרי לימוד".equals(s.prop(books, Store.P_DESC))
                && "ללמוד".equals(s.prop(books, Store.P_WHY)) && s.prop(books, Store.P_DESIGN) == null && s.prop(halacha, Store.P_ICON) == null);
        long fa = s.findFile(A), fb = s.findFile(B), fc = s.findFile(C);
        ck("העתקה: קובץ שבשתי רשימות הוא פריט אחד", fa != 0 && s.listsOf(fa).equals(Arrays.asList(books, halacha))
                && s.listsOf(fb).equals(Arrays.asList(books, pesach)));
        ck("העתקה: חתימה, גודל ותאריך מהקובץ שלהן", "2:00000000000000aa".equals(s.file(fa).sig) && s.file(fa).size == 100 && s.file(fa).mtime == 1700);
        ck("העתקה: בלי חתימה — הגודל מהרשימה, בלי תאריך", s.file(fc).sig == null && s.file(fc).size == 5 && s.file(fc).mtime == 0);
        ck("העתקה: הכונן", s.file(fa).drive.equals(Drive.INTERNAL) && s.file(fc).drive.equals("ABCD-1234"));
        ck("העתקה: המסד תקין", s.check() == null);

        // העתקה חוזרת של אותו מקור: אין מה לעשות, ושום דבר לא משתנה
        long last = s.lastId();
        Importer.Report again = Importer.run(s, src1, true);
        ck("חוזרת: אין שינוי", again.nothingToDo() && again.listsKnown == 4 && again.filesKnown == 3 && again.linksKnown == 5);
        ck("חוזרת: לא נוצרו מספרי זיהוי", s.lastId() == last && s.lists().size() == 4);

        // המשתמש שינה במסד; המקור התקדם
        s.rename(books, "ספרים שלי");
        s.setProp(pesach, Store.P_DESC, "שלי");
        Importer.Source src2 = Importer.load(m2);
        Importer.Report pre2 = Importer.run(s, src2, false);
        Importer.Report r2 = Importer.run(s, src2, true);
        ck("עדכון: התצוגה המקדימה זהה להעתקה", same(pre2, r2));
        ck("עדכון: רשימה חדשה וקובץ חדש", r2.listsNew == 1 && r2.filesNew == 1 && r2.listsKnown == 4);
        ck("עדכון: חתימה חדשה עדכנה קובץ קיים", r2.filesUpdated == 1 && "2:00000000000000cc".equals(s.file(fc).sig) && s.file(fc).mtime == 9);
        ck("עדכון: שם שהמשתמש שינה לא נדרס, והרשימה זוהתה", s.item(books).name.equals("ספרים שלי") && listBy(s, "ספרים") == books);
        ck("עדכון: מאפיין ריק התמלא, מאפיין שיש לא נדרס", "🕯".equals(s.prop(halacha, Store.P_ICON)) && "📘".equals(s.prop(books, Store.P_ICON))
                && r2.propsSet == 1);
        ck("עדכון: מה שהמשתמש כתב במסד נשמר", "שלי".equals(s.prop(pesach, Store.P_DESC)));
        ck("עדכון: מה שיצא מהמקור לא נמחק מהמסד", s.listsOf(fa).contains(halacha));
        long fd = s.findFile(D);
        ck("עדכון: הקובץ החדש נוסף לסוף הרשימה", fd != 0 && names(s, halacha).get(names(s, halacha).size() - 1).equals("ד.pdf"));
        ck("עדכון: חמישה קישורים ישנים נשארו, אחד חדש", r2.linksAdded == 1 && r2.linksKnown == 4);
        ck("עדכון: המסד תקין", s.check() == null);

        // שני אופנים עם אותה רשימה באותו מסלול: שתי רשימות, בלי ערבוב קבצים (באג שנתפס במכשיר)
        File other = new File(dir, "v4/ניסוי");
        write(new File(other, Paths.TREE), "#1 ספרים\n" + D + " | 40\n");
        int before = s.lists().size();
        long booksOther;
        Importer.Report ro = Importer.run(s, Importer.load(other), true);
        booksOther = listIn(s, "ניסוי", "ספרים");
        ck("שני אופנים: הרשימה של האופן השני נוצרה בנפרד", ro.listsNew == 1 && ro.listsKnown == 0 && s.lists().size() == before + 1
                && booksOther != 0 && booksOther != books);
        ck("שני אופנים: הקבצים לא התערבבו", names(s, booksOther).equals(Arrays.asList("ד.pdf")) && !s.listsOf(fb).contains(booksOther));
        ck("שני אופנים: הרשימה של האופן הראשון לא השתנתה", s.count(books) == 3 && s.listsOf(fa).contains(books));
        ck("שני אופנים: אותו קובץ הוא פריט אחד בשני האופנים", s.listsOf(s.findFile(D)).contains(booksOther) && s.findFile(D) == fd);

        // כישלון באמצע: הכול חוזר
        final Store real = s;
        final int[] calls = {0};
        Store flaky = (Store) Proxy.newProxyInstance(Store.class.getClassLoader(), new Class<?>[]{Store.class}, new InvocationHandler() {
            @Override
            public Object invoke(final Object proxy, Method m, Object[] args) throws Throwable {
                if (m.getName().equals("atomic")) {
                    final Store.Work w = (Store.Work) args[0];
                    real.atomic(new Store.Work() {
                        @Override public void run(Store x) { w.run((Store) proxy); }
                    }, ((Boolean) args[1]).booleanValue());
                    return null;
                }
                if (m.getName().equals("createFile") && ++calls[0] == 2) throw new Store.Failure("injected");
                try {
                    return m.invoke(real, args);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            }
        });
        write(new File(dir, "v3/עבודה/" + Paths.TREE), "#1 חדשה אחת\n/x/1 | 1\n/x/2 | 2\n/x/3 | 3\n");
        int listsBefore = s.lists().size();
        boolean threw = false;
        try {
            Importer.run(flaky, Importer.load(new File(dir, "v3/עבודה")), true);
        } catch (Store.Failure e) {
            threw = true;
        }
        ck("כישלון באמצע: ההודעה מגיעה למי שקרא", threw);
        ck("כישלון באמצע: שום דבר לא נשאר", s.lists().size() == listsBefore && s.findFile("/x/1") == 0 && listBy(s, "חדשה אחת") == 0);
    }

    // ---------- בלי Store ----------

    static void pure(StoreTests.Out o, File dir) throws Exception {
        ImportTests t = new ImportTests(o, "העתקה");
        t.drives();
        t.modes(dir);
    }

    private void drives() {
        ck("כונן: פנימי", Importer.driveOf("/storage/emulated/0/x/y.txt").equals(Drive.INTERNAL));
        ck("כונן: כרטיס לפי מזהה", Importer.driveOf("/storage/ABCD-1234/x.txt").equals("ABCD-1234"));
        ck("כונן: כרטיס בלי תת־תיקייה", Importer.driveOf("/storage/ABCD-1234").equals("ABCD-1234"));
        ck("כונן: לא מזוהה", Importer.driveOf("/mnt/x").equals(Importer.UNKNOWN_DRIVE) && Importer.driveOf("/storage/self/p").equals(Importer.UNKNOWN_DRIVE));
    }

    private void modes(File dir) throws Exception {
        File old = new File(dir, "olddata");
        ck("אופנים: אין תיקייה — אין אופנים, ואופן ברירת מחדל", Importer.modes(old).isEmpty() && Importer.activeMode(old).equals(Modes.FIRST));
        write(new File(old, Modes.DIR + "/עבודה אמיתית/" + Paths.TREE), "");
        write(new File(old, Modes.DIR + "/ניסוי/" + Paths.TREE), "");
        write(new File(old, Modes.DIR + "/.מוסתר/" + Paths.TREE), "");
        write(new File(old, Modes.DIR + "/בלי מבנה/אחר.txt"), "");
        ck("אופנים: רק אלה שיש בהם מבנה.txt, בלי מוסתרים, לפי שם", Importer.modes(old).equals(Arrays.asList("ניסוי", "עבודה אמיתית")));
        write(Modes.pointer(old), "ניסוי\n");
        ck("אופנים: האופן הפעיל נקרא מהמצביע", Importer.activeMode(old).equals("ניסוי"));
        boolean threw = false;
        try {
            Importer.load(new File(old, Modes.DIR + "/בלי מבנה"));
        } catch (Store.Failure e) {
            threw = true;
        }
        ck("מקור: אין מבנה.txt — עוצר", threw);
    }
}
