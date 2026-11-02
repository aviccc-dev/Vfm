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
        return a.listsNew == b.listsNew && a.listsKnown == b.listsKnown && a.smartNew == b.smartNew && a.smartKnown == b.smartKnown
                && a.nested == b.nested && a.filesNew == b.filesNew && a.filesKnown == b.filesKnown
                && a.filesUpdated == b.filesUpdated && a.filesWithSig == b.filesWithSig && a.linksAdded == b.linksAdded
                && a.linksKnown == b.linksKnown && a.rootFiles == b.rootFiles && a.propsSet == b.propsSet
                && a.badLines == b.badLines && a.refsAdded == b.refsAdded && a.refsKnown == b.refsKnown && a.refCycles == b.refCycles
                && a.refMissing == b.refMissing && a.smart == b.smart && a.live == b.live
                && a.homeTools == b.homeTools && a.homeAdded == b.homeAdded && a.homeKnown == b.homeKnown
                && a.homeSkipped == b.homeSkipped && a.homeMissing == b.homeMissing && a.homeNew == b.homeNew;
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
        ck("תצוגה מקדימה: ארבע רשימות חדשות, שלושה קבצים, שישה קישורים (מ0.7: גם התיקייה החיה)", pre.listsNew == 4 && pre.listsKnown == 0
                && pre.filesNew == 3 && pre.linksAdded == 6 && pre.linksKnown == 0 && pre.smartNew == 1);
        ck("תצוגה מקדימה: מה שלא מועתק נספר (ההפניה — מ0.4 — כבר מועתקת)", pre.badLines == 1 && pre.refsAdded == 1 && pre.refCycles == 0 && pre.refMissing == 0 && pre.smart == 1 && pre.live == 1 && pre.skippedAny());
        ck("תצוגה מקדימה: קובץ עם חתימה נספר", pre.filesWithSig == 1);

        // העתקה
        Importer.Report r = Importer.run(s, src1, true);
        ck("העתקה: הדוח זהה לתצוגה המקדימה", same(pre, r));
        ck("העתקה: ארבע רשימות", s.lists().size() == 4);
        long books = listBy(s, "ספרים"), halacha = listBy(s, "ספרים / הלכה"), moadim = listBy(s, "מועדים"), pesach = listBy(s, "מועדים / פסח");
        ck("העתקה: כל רשימה נמצאה לפי מקורה", books != 0 && halacha != 0 && moadim != 0 && pesach != 0);
        ck("העתקה: השם הוא השם האחרון, לא המסלול", s.item(halacha).name.equals("הלכה") && s.item(pesach).name.equals("פסח"));
        ck("העתקה: תת־רשימה בתוך האב", s.listsOf(halacha).equals(Arrays.asList(books)) && s.listsOf(pesach).equals(Arrays.asList(books, moadim)));
        ck("העתקה: רשימה עליונה עומדת לעצמה", s.listsOf(books).isEmpty() && s.listsOf(moadim).isEmpty());
        ck("העתקה: הסדר החופשי — קובץ, רשימה, ואז מה שלא נזכר (תיקייה חיה, הפניה, ואז קבצים)", names(s, books).equals(Arrays.asList("ב.pdf", "הלכה", "x", "פסח", "א.pdf")));
        long live = s.findByOrigin(Importer.liveOrigin("עבודה", "ספרים", "/storage/emulated/0/x"));
        ck("מ0.7: תיקייה חיה — רשימה חכמה של התיקייה, כמו ההסבה של המנוע הישן", live != 0 && s.item(live).type == Store.SMART
                && s.item(live).name.equals("x") && s.listsOf(live).equals(Arrays.asList(books))
                && (Cond.SOURCE + ":" + Cond.quote("/storage/emulated/0/x")).equals(s.prop(live, Store.P_COND))
                && Sort.NAME.equals(s.prop(live, Store.P_SORT)) && FolderView.ALL.equals(s.prop(live, Store.P_VIEW)));
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
        ck("חוזרת: אין שינוי", again.nothingToDo() && again.listsKnown == 4 && again.filesKnown == 3 && again.linksKnown == 6 && again.smartKnown == 1);
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
        ck("שני אופנים: הרשימה של האופן הראשון לא השתנתה", s.count(books) == 5 && s.listsOf(fa).contains(books));
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


    // ---------- רשימות חכמות (מ0.3) ----------

    static final String E = "/storage/emulated/0/ש.pdf";

    /** עץ עם חכמות: מקושרת לנפוצה (p-link), פנימית (p-shot), נפוצה (s-img, גם בתת־רשימה), כלי (f-1), ונמחקה (s-gone). */
    static final String SMART_TREE = ""
            + E + " | 3\n"
            + "#1 ספרים\n"
            + "@חכמה p-link\n"
            + "@חכמה p-shot\n"
            + "@חכמה s-img\n"
            + "@חכמה f-1\n"
            + "@חכמה s-gone\n"
            + "@סדר ss-img\n"
            + "@סדר cהלכה\n"
            + A + " | 100\n"
            + "#2 ספרים / הלכה\n"
            + "@חכמה s-img\n"
            + B + " | 7\n";

    static final String SMART_INI = ""
            + "[הגדרה נפוצה: אחרונים]\nמזהה = s-rec\nתנאי = תאריך:שבוע\nמיון = תאריך\nתצוגה = ברצף\n\n"
            + "[הגדרה נפוצה: תמונות]\nמזהה = s-img\nתנאי = סוג:תמונות\nמיון = שם\n\n"
            + "[הגדרה נפוצה: ריקה]\nמזהה = s-empty\nתנאי =\n\n"
            + "[רשימה פנימית: ספרים › אחרונים שלי]\nמזהה = p-link\nהגדרה נפוצה = s-rec\n\n"
            + "[רשימה פנימית: ספרים › צילומי מסך]\nמזהה = p-shot\nתנאי = סוג:תמונות צילום\nמיון = גודל\nרשימות בתוך = m:ספרים|s-img\n\n"
            + "[פונקציה: כלי]\nמזהה = f-1\nסוג = חיפוש\nתנאי = x\n";

    static void writeSmartMode(File mode) throws IOException {
        write(new File(mode, Paths.TREE), SMART_TREE);
        write(new File(new File(mode, Paths.SETTINGS), Paths.MAIN), SMART_INI);
    }

    /** מסד חדש לחלוטין (מופע Store משלו): העתקת חכמות, חברות וסדר, "לא משויכים", עדכון בלי דריסה, ומה שנמחק חוזר. */
    void smart(Store s, File dir) throws Exception {
        File m = new File(dir, "חכם");
        writeSmartMode(m);
        Importer.Source src = Importer.load(m);
        ck("חכמות: נקראו 4 הגדרות — הנפוצות ואחריהן הפנימית שאינה מקושרת", src.wanted().size() == 4
                && src.wanted().get(0).key.equals("s-rec") && src.wanted().get(3).key.equals("p-shot"));
        ck("חכמות: פנימית מקושרת היא אותה הגדרה כמו הנפוצה", src.smart("p-link") != null && src.smart("p-link").key.equals("s-rec"));
        ck("חכמות: כלי ונמחקה אינן הגדרה", src.smart("f-1") == null && src.smart("s-gone") == null && src.skippedSmart() == 2);

        Importer.Report pre = Importer.run(s, src, false);
        ck("חכמות: תצוגה מקדימה לא שומרת", s.smarts().isEmpty() && s.lists().isEmpty() && s.lastId() == 0);
        Importer.Report r = Importer.run(s, src, true);
        ck("חכמות: הדוח זהה לתצוגה המקדימה", same(pre, r));
        ck("חכמות: ארבע חדשות, שתי רשימות, 6 קישורים (קובץ וחכמה), 3 קבצים", r.smartNew == 4 && r.smartKnown == 0 && r.listsNew == 2
                && r.linksAdded == 6 && r.filesNew == 3 && r.rootFiles == 1);
        ck("חכמות: מה שלא מועתק — שתי הפניות, ושתי \"רשימות בתוך\"", r.smart == 2 && r.nested == 2 && r.skippedAny());
        ck("חכמות: ארבעה פריטים חכמים במסד", s.smarts().size() == 4 && s.lists().size() == 2);
        long books = s.findByOrigin(Importer.originOf("חכם", "ספרים")), halacha = s.findByOrigin(Importer.originOf("חכם", "ספרים / הלכה"));
        long rec = s.findByOrigin(Importer.smartOrigin("חכם", "s-rec")), img = s.findByOrigin(Importer.smartOrigin("חכם", "s-img"));
        long shot = s.findByOrigin(Importer.smartOrigin("חכם", "p-shot")), empty = s.findByOrigin(Importer.smartOrigin("חכם", "s-empty"));
        ck("חכמות: כל אחת נמצאת לפי מקורה; המקושרת לא קיבלה פריט משלה", rec != 0 && img != 0 && shot != 0 && empty != 0
                && s.findByOrigin(Importer.smartOrigin("חכם", "p-link")) == 0);
        ck("חכמות: סוג, שם ותנאי", s.item(rec).type == Store.SMART && s.item(rec).name.equals("אחרונים") && "תאריך:שבוע".equals(s.prop(rec, Store.P_COND))
                && "תאריך".equals(s.prop(rec, Store.P_SORT)) && "ברצף".equals(s.prop(rec, Store.P_VIEW)) && s.prop(rec, Store.P_DIR) == null);
        ck("חכמות: פנימית — שם אחרי החץ, תנאי ומיון", s.item(shot).name.equals("צילומי מסך") && "סוג:תמונות צילום".equals(s.prop(shot, Store.P_COND))
                && "גודל".equals(s.prop(shot, Store.P_SORT)));
        ck("חכמות: נפוצה בלי תנאי נוצרת, בלי תנאי", s.item(empty) != null && s.prop(empty, Store.P_COND) == null);
        ck("חכמות: הנפוצות עומדות לעצמן", s.listsOf(rec).equals(Arrays.asList(books)) && s.listsOf(empty).isEmpty());
        ck("חכמות: הסדר — מה ש־@סדר קבע, אחריו תת־רשימות, חכמות ואז קבצים",
                names(s, books).equals(Arrays.asList("תמונות", "הלכה", "אחרונים", "צילומי מסך", "א.pdf")));
        ck("חכמות: אותה חכמה בשתי רשימות — פריט אחד", s.listsOf(img).equals(Arrays.asList(books, halacha))
                && names(s, halacha).equals(Arrays.asList("תמונות", "ב.pdf")));
        ck("חכמות: לא משויכים — רק הקובץ מהשורש", names(s, books).size() == 5 && s.unassigned().size() == 1
                && s.unassigned().get(0).name.equals("ש.pdf") && s.findFile(E) == s.unassigned().get(0).id);
        ck("חכמות: המסד תקין", s.check() == null);

        long last = s.lastId();
        Importer.Report again = Importer.run(s, src, true);
        ck("חכמות חוזרת: אין שינוי ואין מספרים חדשים", again.nothingToDo() && again.smartKnown == 4 && again.linksKnown == 6 && s.lastId() == last);

        // המשתמש עורך: התנאי שלו לא נדרס; מאפיין שנמחק חוזר; חכמה שנמחקה חוזרת
        s.setProp(rec, Store.P_COND, "משלי");
        s.rename(rec, "אחרונים שלי");
        s.setProp(img, Store.P_SORT, "");
        Importer.Report upd = Importer.run(s, src, true);
        ck("חכמות עדכון: התנאי והשם של המשתמש נשארים", "משלי".equals(s.prop(rec, Store.P_COND)) && s.item(rec).name.equals("אחרונים שלי"));
        ck("חכמות עדכון: מאפיין ריק התמלא פעם אחת", "שם".equals(s.prop(img, Store.P_SORT)) && upd.propsSet == 1 && upd.smartNew == 0);
        s.deleteList(img);
        Importer.Report back = Importer.run(s, src, true);
        long img2 = s.findByOrigin(Importer.smartOrigin("חכם", "s-img"));
        ck("חכמות: חכמה שנמחקה במסד חוזרת בהעתקה חוזרת, ומתחברת מחדש לשתי הרשימות", back.smartNew == 1 && img2 != 0 && img2 != img
                && s.listsOf(img2).size() == 2 && back.linksAdded == 2);
        ck("חכמות: המסד תקין בסוף", s.check() == null);

        // בלי קובץ ראשי: ההפניות נספרות כלא־נמצאו, ושום דבר לא קורס
        File bare = new File(dir, "בלי-ראשי/חכם2");
        write(new File(bare, Paths.TREE), SMART_TREE);
        Importer.Source noIni = Importer.load(bare);
        ck("חכמות: בלי קובץ ראשי — אין הגדרות, וכל ההפניות לא נמצאו", noIni.ini != null && noIni.wanted().isEmpty() && noIni.skippedSmart() == 6);
        // קובץ ראשי עם הפניה מעגלית של פנימית שמקשרת לפנימית אחרת (לא נפוצה): לא נתקע
        File loop = new File(dir, "מעגל/חכם3");
        write(new File(loop, Paths.TREE), "#1 א\n@חכמה p-a\n");
        write(new File(new File(loop, Paths.SETTINGS), Paths.MAIN),
                "[רשימה פנימית: א › אחת]\nמזהה = p-a\nהגדרה נפוצה = p-b\nתנאי = x\n\n[רשימה פנימית: א › שתיים]\nמזהה = p-b\nהגדרה נפוצה = p-a\nתנאי = y\n");
        Importer.Source lp = Importer.load(loop);
        ck("חכמות: קישור בין שתי פנימיות לא נתקע — כל אחת היא הגדרה בפני עצמה", lp.wanted().size() == 1 && "x".equals(lp.wanted().get(0).cond));
    }

    // ---------- פקודות רשימות במסד (מ0.10) ----------

    void paths(Store s, File dir) throws Exception {
        File m = new File(dir, "פק");
        write(new File(m, Paths.TREE), "#1 ספרים\n#2 ספרים / הלכה\n#1 מועדים\n#2 מועדים / הלכה\n");
        Importer.run(s, Importer.load(m), true);
        long books = listIn(s, "פק", "ספרים"), hal = listIn(s, "פק", "ספרים / הלכה"), moHal = listIn(s, "פק", "מועדים / הלכה");
        ck("פקודות: פתח רשימה — לפי המקור", DbPaths.find(s, "פק", Tree.parsePath("ספרים/הלכה")) == hal
                && DbPaths.find(s, "פק", Tree.parsePath("מועדים / הלכה")) == moHal);
        ck("פקודות: לא קיימת — 0", DbPaths.find(s, "פק", Tree.parsePath("אין/כזו")) == 0 && DbPaths.find(s, "פק", Tree.parsePath("")) == 0);
        long n1 = s.createList("חדש", 0), n2 = s.createList("פנים", n1);
        ck("פקודות: רשימה שנוצרה במסד (בלי מקור) — לפי השמות", DbPaths.find(s, "פק", Tree.parsePath("חדש/פנים")) == n2);
        long last = s.lastId();
        ck("פקודות: צור רשימה קיימת — אותה רשימה, בלי מספר חדש", DbPaths.ensure(s, "פק", Tree.parsePath("ספרים/הלכה")) == hal && s.lastId() == last);
        long made = DbPaths.ensure(s, "פק", Tree.parsePath("ספרים/הלכה/שבת"));
        ck("פקודות: צור רשימה — החלק החסר נוצר בתוך הקיים", made != 0 && s.listsOf(made).equals(Arrays.asList(hal)) && s.item(made).name.equals("שבת"));
        long deep = DbPaths.ensure(s, "פק", Tree.parsePath("א/ב/ג"));
        long a1 = DbPaths.find(s, "פק", Tree.parsePath("א"));
        ck("פקודות: צור רשימה — כל הדרך, הראשונה עומדת לעצמה", deep != 0 && a1 != 0 && s.listsOf(a1).isEmpty()
                && DbPaths.find(s, "פק", Tree.parsePath("א/ב/ג")) == deep);
        long home = s.createList("בית", 0);
        s.setProp(home, Store.P_HOME, "1");
        s.addMember(home, books);
        ck("פקודות: רשימה שבבית — עדיין בראש", DbPaths.find(s, "פק", Tree.parsePath("ספרים")) == books);
        long sm = DbPaths.ensureSmart(s, "חדשים", "תאריך:שבוע", "תאריך");
        ck("פקודות: צור רשימה חכמה — נוצרת עם התנאי והמיון", sm != 0 && s.item(sm).type == Store.SMART && "תאריך:שבוע".equals(s.prop(sm, Store.P_COND))
                && "תאריך".equals(s.prop(sm, Store.P_SORT)) && DbPaths.smart(s, "חדשים") == sm);
        ck("פקודות: אותו שם ואותו תנאי — הקיימת", DbPaths.ensureSmart(s, "חדשים", "תאריך:שבוע", "") == sm);
        long sm2 = DbPaths.ensureSmart(s, "חדשים", "תאריך:חודש", "");
        long sm3 = DbPaths.ensureSmart(s, "חדשים", "סוג:ספרים", "");
        ck("פקודות: אותו שם ותנאי אחר — \"שם (2)\", ואחר כך (3)", s.item(sm2).name.equals("חדשים (2)") && s.item(sm3).name.equals("חדשים (3)"));
        ck("פקודות: עדכן רשימה חכמה — התנאי; המיון רק אם נכתב", DbPaths.updateSmart(s, "חדשים", "תאריך:יום", "") == sm
                && "תאריך:יום".equals(s.prop(sm, Store.P_COND)) && "תאריך".equals(s.prop(sm, Store.P_SORT)));
        ck("פקודות: עדכן רשימה חכמה שאינה — 0", DbPaths.updateSmart(s, "אין", "x", "") == 0);
        ck("פקודות: המסד תקין", s.check() == null);
    }

    // ---------- הספירה במראה המוכר (מ0.9.1) ----------

    void count(Store s) throws Exception {
        long a = s.createList("א", 0), b = s.createList("ב", 0), c = s.createList("ג", 0);
        long f1 = s.createFile(new Store.FileInfo("/x/1", "פנימי", 10, 0, null));
        long f2 = s.createFile(new Store.FileInfo("/x/2", "פנימי", 5, 0, null));
        long f3 = s.createFile(new Store.FileInfo("/x/3", "פנימי", 7, 0, null));
        long sm = s.createSmart("ח", 0);
        s.setProp(sm, Store.P_COND, "X");
        s.addMember(a, f1);
        s.addMember(a, b);
        s.addMember(a, sm);
        s.addMember(a, c);
        s.addMember(b, f1);
        s.addMember(b, f2);
        s.addMember(b, c);
        s.addMember(c, f3);
        final java.util.Map<String, Long> hits = new java.util.HashMap<String, Long>();
        hits.put("/x/1", 10L);
        hits.put("/x/9", 20L);
        hits.put("/x/8", 0L);
        DbCount.Index ix = new DbCount.Index() {
            @Override public long[] cond(String q) { return "X".equals(q) ? new long[]{3, 30} : null; }
            @Override public java.util.Map<String, Long> files(String q) { return "X".equals(q) ? hits : null; }
        };
        DbCount k = DbCount.of(s, a, ix);
        ck("ספירה: רשימות בכל העומק (כל הופעה), קבצים כסכום, ורשימה משותפת — תוכנה פעם אחת",
                k.lists == 4 && k.items == 7 && k.size == 62);
        ck("ספירה: קבצים שונים ונפחם — כולל מה שהחכמה מוצאת", k.unique == 5 && k.uniqueSize == 42);
        ck("ספירה: אותו נוסח כמו במנוע הישן", k.text().equals(ListCount.text(4, 7, 62, 5, 42)));
        DbCount none = DbCount.of(s, a, null);
        ck("ספירה בלי האינדקס: רק הקבצים שברשימות, בלי \"שונים\"", none.items == 4 && none.size == 32 && none.unique == -1);
        ck("ספירה: רשימה חכמה לבדה", java.util.Arrays.equals(DbCount.smart(s, sm, ix), new long[]{3, 30}) && DbCount.smart(s, sm, null) == null);
        ck("ספירה: המסד תקין", s.check() == null);
    }

    // ---------- תיקיות חיות (מ0.7) ----------

    /** בשורש, ברשימה עם סדר חופשי (מפתח "l" — כמו שהמנוע הישן כותב), עם קו נטוי בסוף, ואותה תיקייה בשתי רשימות. */
    static final String LIVE_TREE = ""
            + "@תיקייה /s/r\n"
            + "#1 א\n"
            + "@סדר l/s/x/\n"
            + "@סדר f/a.pdf\n"
            + "/a.pdf | 1\n"
            + "@תיקייה /s/y\n"
            + "@תיקייה /s/x/\n"
            + "#1 ב\n"
            + "@תיקייה /s/y\n";

    void live(Store s, File dir) throws Exception {
        File m = new File(dir, "חי");
        write(new File(m, Paths.TREE), LIVE_TREE);
        Importer.Source src = Importer.load(m);
        ck("חיות: נקראו — אחת בשורש, שתיים ב־א, אחת ב־ב", src.tree.root.live.size() == 1 && src.tree.root.children.get(0).live.size() == 2
                && src.tree.root.children.get(1).live.size() == 1);
        Importer.Report pre = Importer.run(s, src, false);
        Importer.Report r = Importer.run(s, src, true);
        ck("חיות: הדוח זהה לתצוגה המקדימה", same(pre, r));
        ck("חיות: ארבע נספרו וארבע רשימות חכמות נוצרו — ולא נחשבות \"לא מועתק\"", r.live == 4 && r.smartNew == 4 && !r.skippedAny());
        long a = listIn(s, "חי", "א"), b = listIn(s, "חי", "ב");
        ck("חיות: הסדר — מה ש־@סדר קבע (גם תיקייה חיה), ואחריו השאר", names(s, a).equals(Arrays.asList("x", "a.pdf", "y")));
        long ya = s.findByOrigin(Importer.liveOrigin("חי", "א", "/s/y")), yb = s.findByOrigin(Importer.liveOrigin("חי", "ב", "/s/y"));
        ck("חיות: אותה תיקייה בשתי רשימות — שתי רשימות חכמות (כמו בהסבה של הישן)", ya != 0 && yb != 0 && ya != yb
                && s.listsOf(ya).equals(Arrays.asList(a)) && s.listsOf(yb).equals(Arrays.asList(b)));
        long root = s.findByOrigin(Importer.liveOrigin("חי", "", "/s/r"));
        ck("חיות: בשורש — עומדת לעצמה", root != 0 && s.listsOf(root).isEmpty() && s.item(root).name.equals("r"));
        long x = s.findByOrigin(Importer.liveOrigin("חי", "א", "/s/x/"));
        ck("חיות: קו נטוי בסוף — השם בלעדיו, והתנאי כמו בהסבה של הישן", x != 0 && s.item(x).name.equals("x")
                && Unify.folderKeys("/s/x/").get(SmartLists.COND).equals(s.prop(x, Store.P_COND)));
        long last = s.lastId();
        Importer.Report again = Importer.run(s, src, true);
        ck("חיות חוזרת: אין שינוי ואין מספרים חדשים", again.nothingToDo() && again.smartKnown == 4 && s.lastId() == last);
        s.setProp(ya, Store.P_COND, "משלי");
        s.setProp(ya, Store.P_VIEW, "");
        Importer.Report upd = Importer.run(s, src, true);
        ck("חיות: התנאי של המשתמש נשאר, ומאפיין שנמחק חוזר", "משלי".equals(s.prop(ya, Store.P_COND))
                && FolderView.ALL.equals(s.prop(ya, Store.P_VIEW)) && upd.propsSet == 1);
        s.deleteList(yb);
        Importer.Report back = Importer.run(s, src, true);
        long yb2 = s.findByOrigin(Importer.liveOrigin("חי", "ב", "/s/y"));
        ck("חיות: רשימה חכמה שנמחקה במסד חוזרת, למקומה", back.smartNew == 1 && yb2 != 0 && yb2 != yb && s.listsOf(yb2).equals(Arrays.asList(b)));
        ck("חיות: המסד תקין", s.check() == null);
    }

    // ---------- מסך הבית (מ0.6) ----------

    static final String CMD = "/x/גיבוי.vbat";

    /** קובץ הפקודות של הבית נמצא גם בשורש הרשימות — כדי לבדוק שהוא לא נספר ב"לא משויכים". */
    static final String HOME_TREE = "#1 ספרים\n/x/a.pdf | 1\n#1 מועדים\n#2 מועדים / פסח\n" + CMD + " | 3\n";

    /** עשרה כלים: שתי רשימות, חכמה מקושרת וחכמה עצמאית, כפולים, פקודה, רשימה שאינה, פקודה בלי קובץ, אותה רשימה שוב, ישן בלי רשימה. */
    static final String HOME_INI = ""
            + "[הגדרה נפוצה: אחרונים]\nמזהה = s-rec\nתנאי = תאריך:שבוע\n\n"
            + "[פונקציה: פסח]\nמזהה = f-1\nסוג = וירטואלי\nתיאור = לחג\nרשימה = מועדים/פסח\n\n"
            + "[פונקציה: ספרים]\nמזהה = f-2\nסוג = וירטואלי\nרשימה = ספרים\n\n"
            + "[פונקציה: חדשים]\nמזהה = f-3\nסוג = חכמה\nהגדרה נפוצה = s-rec\n\n"
            + "[פונקציה: תמונות]\nמזהה = f-4\nסוג = חכמה\nתנאי = סוג:תמונות\nמיון = שם\n\n"
            + "[פונקציה: כפולים]\nמזהה = f-5\nסוג = כפולים\n\n"
            + "[פונקציה: גיבוי]\nמזהה = f-6\nסוג = פקודה\nקובץ = " + CMD + "\n\n"
            + "[פונקציה: חסרה]\nמזהה = f-7\nסוג = וירטואלי\nרשימה = אין כזו\n\n"
            + "[פונקציה: פקודה ריקה]\nמזהה = f-8\nסוג = פקודה\n\n"
            + "[פונקציה: שוב ספרים]\nמזהה = f-9\nסוג = וירטואלי\nרשימה = ספרים\n\n"
            + "[פונקציה: ישן]\nמזהה = f-10\nסוג = וירטואלי\n";

    static void writeHomeMode(File mode) throws IOException {
        write(new File(mode, Paths.TREE), HOME_TREE);
        write(new File(new File(mode, Paths.SETTINGS), Paths.MAIN), HOME_INI);
    }

    /** מסד חדש: הבית נוצר עם הכלים לפי הסדר; מה שלא מועתק נספר; העתקה חוזרת; מה שהמשתמש שינה; בית שנמחק; מעגל. */
    void home(Store s, File dir) throws Exception {
        File m = new File(dir, "בית");
        writeHomeMode(m);
        Importer.Source src = Importer.load(m);
        ck("בית: נקראו עשרה כלים לפי הסדר", src.tools().size() == 10 && src.tools().get(0).name.equals("פסח") && src.tools().get(9).name.equals("ישן"));
        ck("בית: כלי חכם מקושר — אותה הגדרה כמו הנפוצה; עצמאי — ההגדרה שבו", src.toolSmart(src.tools().get(2)).key.equals("s-rec")
                && src.toolSmart(src.tools().get(3)).key.equals("f-4") && "סוג:תמונות".equals(src.toolSmart(src.tools().get(3)).cond));

        Importer.Report pre = Importer.run(s, src, false);
        ck("בית: תצוגה מקדימה לא שומרת", s.lists().isEmpty() && Importer.home(s) == 0 && pre.homeNew);
        Importer.Report r = Importer.run(s, src, true);
        ck("בית: הדוח זהה לתצוגה המקדימה", same(pre, r));
        ck("בית: 10 כלים — 5 נכנסו, אחד כבר היה (אותה רשימה פעמיים), סוג אחד לא מועתק, 3 בלי רשימה או קובץ",
                r.homeTools == 10 && r.homeAdded == 5 && r.homeKnown == 1 && r.homeSkipped == 1 && r.homeMissing == 3 && r.homeNew && r.skippedAny());
        long h = Importer.home(s);
        ck("בית: רשימה אחת מסומנת, בשם בית, ומקורה האופן", h != 0 && s.item(h).name.equals(Words.Db.HOME_NAME)
                && s.findByOrigin(Importer.homeOrigin("בית")) == h && "1".equals(s.prop(h, Store.P_HOME)));
        ck("בית: הכלים לפי הסדר", names(s, h).equals(Arrays.asList("פסח", "ספרים", "אחרונים", "תמונות", "גיבוי.vbat")));
        long pesach = listIn(s, "בית", "מועדים / פסח"), books = listIn(s, "בית", "ספרים");
        long rec = s.findByOrigin(Importer.smartOrigin("בית", "s-rec")), img = s.findByOrigin(Importer.smartOrigin("בית", "f-4"));
        ck("בית: רשימה — אותה רשימה (גם במקומה), לא עותק", s.members(h).get(0).item.id == pesach && s.listsOf(pesach).size() == 2
                && s.members(h).get(1).item.id == books);
        ck("בית: חכמה מקושרת — הפריט של הנפוצה, בלי פריט משלה", s.members(h).get(2).item.id == rec
                && s.findByOrigin(Importer.smartOrigin("בית", "f-3")) == 0);
        ck("בית: חכמה עצמאית — פריט משלה עם התנאי והמיון", img != 0 && s.item(img).type == Store.SMART && s.item(img).name.equals("תמונות")
                && "סוג:תמונות".equals(s.prop(img, Store.P_COND)) && "שם".equals(s.prop(img, Store.P_SORT)));
        ck("בית: פקודה — פריט הקובץ (אותו פריט כמו בשורש הרשימות)", s.members(h).get(4).item.id == s.findFile(CMD) && s.item(s.findFile(CMD)).type == Store.FILE);
        ck("בית: קובץ הפקודות בבית אינו \"לא משויך\"", s.unassigned().isEmpty());
        ck("בית: התיאור של הכלי עבר לרשימה שלא היה לה", "לחג".equals(s.prop(pesach, Store.P_DESC)));
        ck("בית: המסד תקין", s.check() == null);

        long last = s.lastId();
        Importer.Report again = Importer.run(s, src, true);
        ck("בית חוזרת: אין שינוי ואין מספרים חדשים", again.nothingToDo() && again.homeKnown == 6 && again.homeAdded == 0 && !again.homeNew
                && s.lastId() == last);

        // המשתמש מסדר ומוציא: הסדר שלו נשמר, ומה שהוציא חוזר לסוף
        s.move(h, img, 0);
        s.removeMember(h, s.findFile(CMD));
        Importer.Report upd = Importer.run(s, src, true);
        ck("בית: הסדר של המשתמש נשמר, והפקודה שהוצאה חוזרת לסוף", upd.homeAdded == 1
                && names(s, h).equals(Arrays.asList("תמונות", "פסח", "ספרים", "אחרונים", "גיבוי.vbat")));

        // הבית נמחק: חוזר בהעתקה חוזרת, והרשימות עצמן לא נפגעו
        s.deleteList(h);
        ck("בית: מחיקת הבית לא מוחקת את הרשימות שבו", s.item(pesach) != null && s.item(books) != null && Importer.home(s) == 0);
        Importer.Report back = Importer.run(s, src, true);
        long h2 = Importer.home(s);
        ck("בית: בית שנמחק נוצר שוב, עם כל הכלים", back.homeNew && h2 != 0 && h2 != h && names(s, h2).size() == 5);

        // בית מסומן בלי מקור (לא מהעתקה) — משמש, לא נוצר שני; ומעגל נחסם
        s.deleteList(h2);
        long mine = s.createList("הבית שלי", 0);
        s.setProp(mine, Store.P_HOME, "1");
        s.addMember(pesach, mine);
        Importer.Report cyc = Importer.run(s, src, true);
        ck("בית: בית מסומן קיים — משמש, ולא נוצר בית שני", !cyc.homeNew && Importer.home(s) == mine && s.findByOrigin(Importer.homeOrigin("בית")) == 0);
        ck("בית: רשימה שהבית בתוכה — לא נכנסת לבית (מעגל), נספרת", cyc.homeMissing == 4 && !names(s, mine).contains("פסח")
                && names(s, mine).equals(Arrays.asList("ספרים", "אחרונים", "תמונות", "גיבוי.vbat")));
        ck("בית: המסד תקין בסוף", s.check() == null);

        // רק כלים שלא מועתקים — אין בית
        File only = new File(dir, "רק-כפולים/אחר");
        write(new File(only, Paths.TREE), "#1 א\n");
        write(new File(new File(only, Paths.SETTINGS), Paths.MAIN), "[פונקציה: כפולים]\nמזהה = f-1\nסוג = כפולים\n");
        Importer.Report none = Importer.run(s, Importer.load(only), false);
        ck("בית: רק כלים שלא מועתקים — לא נוצר בית", !none.homeNew && none.homeTools == 1 && none.homeSkipped == 1 && none.homeAdded == 0);
    }

    // ---------- הפניות בין רשימות (מ0.4) ----------

    /** הפניות: לרשימה מאוחרת בקובץ (ג), לעצמה, לרשימה שלא קיימת, לאב, ושרשרת שנסגרת (ג ← ב ← א ← ג). */
    static final String REF_TREE = ""
            + "#1 א\n"
            + "@רשימה ג\n"
            + "@רשימה א\n"
            + "@סדר rג\n"
            + "/x/a1 | 1\n"
            + "#1 ב\n"
            + "@רשימה א\n"
            + "@רשימה לא קיימת\n"
            + "#2 ב / פנימית\n"
            + "@רשימה ב\n"
            + "#1 ג\n"
            + "@רשימה ב\n";

    void refs(Store s, File dir) throws Exception {
        File m = new File(dir, "הפניות");
        write(new File(m, Paths.TREE), REF_TREE);
        Importer.Source src = Importer.load(m);
        Importer.Report pre = Importer.run(s, src, false);
        ck("הפניות: תצוגה מקדימה לא שומרת", s.lists().isEmpty());
        Importer.Report r = Importer.run(s, src, true);
        ck("הפניות: הדוח זהה לתצוגה המקדימה", same(pre, r));
        ck("הפניות: שתיים הועתקו, שלוש נחסמו כמעגל, אחת לרשימה שלא קיימת", r.refsAdded == 2 && r.refCycles == 3 && r.refMissing == 1
                && r.refsKnown == 0 && r.skippedAny());
        long a = listIn(s, "הפניות", "א"), b = listIn(s, "הפניות", "ב"), c = listIn(s, "הפניות", "ג"), in = listIn(s, "הפניות", "ב / פנימית");
        ck("הפניות: הפניה לרשימה שמופיעה מאוחר בקובץ — הועתקה, ולפי @סדר", names(s, a).equals(Arrays.asList("ג", "a1")));
        ck("הפניות: אותה רשימה בשני מקומות", s.listsOf(a).equals(Arrays.asList(b)) && s.listsOf(c).equals(Arrays.asList(a)));
        ck("הפניות: תת־רשימה בתוך האב, וההפניה שלה לאב נחסמה", s.listsOf(in).equals(Arrays.asList(b)) && s.count(in) == 0);
        ck("הפניות: השרשרת שנסגרת נחסמה", s.count(c) == 0 && s.contains(b, c) && !s.contains(c, b));
        boolean noCycle = true;
        for (Store.Item l : s.lists()) noCycle &= !s.contains(l.id, l.id);
        ck("הפניות: אין אף רשימה בתוך עצמה", noCycle && s.check() == null);
        long last = s.lastId();
        Importer.Report again = Importer.run(s, src, true);
        ck("הפניות חוזרת: אין שינוי", again.nothingToDo() && again.refsKnown == 2 && again.refCycles == 3 && again.refMissing == 1
                && s.lastId() == last);
        s.removeMember(a, c);
        Importer.Report back = Importer.run(s, src, true);
        ck("הפניות: הפניה שהמשתמש הסיר חוזרת בהעתקה חוזרת (כמו קובץ)", back.refsAdded == 1 && s.listsOf(c).equals(Arrays.asList(a)));
    }

    // ---------- הבדיקה המלאה (FullCheck) ----------

    /** opener — יוצר Store חדש. sabotage — האם המסד ה"אמיתי" חסר קישור אחד. */
    private String fullReport(final StoreContract.Opener opener, File dir, final boolean sabotage, final boolean selfOk) throws Exception {
        return fullReport(opener, dir, sabotage, selfOk, false);
    }

    /** מנוע הערכה מדומה: "סוג:תמונות" — 3, "סוג:תמונות צילום" — 2 (או הערה "לא הובן" כש־badCond), כל השאר — 0. */
    static Smarts.Finder fake(final boolean badCond) {
        return new Smarts.Finder() {
            @Override
            public Smarts.Result find(String cond, String sort, int limit) {
                int n = cond.equals("סוג:תמונות") ? 3 : cond.equals("סוג:תמונות צילום") ? 2 : 0;
                return new Smarts.Result(true, n, n * 10L, badCond && cond.contains("צילום") ? "לא הובן" : "", new ArrayList<Smarts.Row>());
            }
        };
    }

    private String fullReport(final StoreContract.Opener opener, File dir, final boolean sabotage, final boolean selfOk,
                              final boolean badCond) throws Exception {
        return fullReport(opener, dir, sabotage, selfOk, badCond, 0);
    }

    /** homeReal: 0 — לאופן "בית" אין מסד אמיתי; 1 — יש, מועתק; 2 — יש, וחסר בו כלי בבית. */
    private String fullReport(final StoreContract.Opener opener, File dir, final boolean sabotage, final boolean selfOk,
                              final boolean badCond, final int homeReal) throws Exception {
        final File old = new File(dir, "olddata-" + sabotage + selfOk + badCond + homeReal);
        writeSmartMode(new File(old, Modes.DIR + "/חכם"));
        writeHomeMode(new File(old, Modes.DIR + "/בית"));
        write(new File(old, Modes.DIR + "/חי/" + Paths.TREE), LIVE_TREE);
        write(new File(old, Modes.DIR + "/הפניות/" + Paths.TREE), REF_TREE);
        write(new File(old, Modes.DIR + "/ראשון/" + Paths.TREE), TREE1);
        write(new File(old, Modes.DIR + "/ראשון/" + Sig.FILE), SIGS1);
        write(new File(old, Modes.DIR + "/שני/" + Paths.TREE), TREE2);
        write(new File(old, Modes.DIR + "/שני/" + Sig.FILE), SIGS2);
        final int[] n = {0};
        final File ctr = dir;
        return FullCheck.run(old, new FullCheck.Env() {
            @Override public String header() { return "HEADER"; }
            @Override public String selfTest() { return "SELFTEST"; }
            @Override public boolean selfTestOk() { return selfOk; }
            @Override public Store openTemp() throws Exception { return opener.fresh(new File(ctr, "fc-" + (n[0]++) + "-" + badCond + ".txt")); }
            @Override public Smarts.Finder finder() { return fake(badCond); }
            @Override public File scratch() { File w = new File(ctr, "scratch-" + System.nanoTime()); w.mkdirs(); return w; }
            @Override public Backups backups(String mode) { return mode.equals("ראשון") ? new Backups(new File(ctr, "backups-" + mode), new File(ctr, "bk-tmp")) : null; }
            @Override
            public Store openReal(String mode) throws Exception {
                if (mode.equals("בית") && homeReal > 0) {
                    Store s = opener.fresh(new File(ctr, "fc-" + (n[0]++) + "-" + badCond + "h.txt"));
                    Importer.run(s, Importer.load(new File(old, Modes.DIR + "/בית")), true);
                    if (homeReal == 2) s.removeMember(Importer.home(s), s.findFile(CMD));
                    return s;
                }
                if (!mode.equals("ראשון")) return null;
                Store s = opener.fresh(new File(ctr, "fc-" + (n[0]++) + "-" + badCond + "r.txt"));
                Importer.run(s, Importer.load(new File(old, Modes.DIR + "/ראשון")), true);
                if (sabotage) {
                    long books = s.findByOrigin(Importer.originOf("ראשון", "ספרים"));
                    s.removeMember(books, s.findFile(A));
                    s.removeMember(books, s.findByOrigin(Importer.originOf("ראשון", "מועדים / פסח")));   // כמו מסד ממ0.3: בלי הפניות
                }
                return s;
            }
        });
    }

    void fullCheck(StoreContract.Opener opener, File dir) throws Exception {
        String ok = fullReport(opener, dir, false, true);
        ck("בדיקה מלאה: הכול תקין — פסק דין ירוק", ok.startsWith(Words.Db.VERDICT_OK));
        ck("בדיקה מלאה: הדוח כולל כותרת, בדיקה עצמית ושני אופנים", ok.contains("HEADER") && ok.contains("SELFTEST")
                && ok.contains(Words.Db.modeHead("ראשון")) && ok.contains(Words.Db.modeHead("שני")));
        ck("בדיקה מלאה: אופן בלי מסד אמיתי מדווח כך", ok.contains(Words.Db.REAL_NONE));
        ck("בדיקה מלאה: מה שלא מועתק מופיע", ok.contains("1 הפניות") && ok.contains("1 הפניות חכמות שלא נמצאו"));
        ck("בדיקה מלאה: רשימה ריקה בלי סיבה מוצגת בשמה", ok.contains(Words.Db.modeEmpty(1, 1, 0)) && ok.contains("   חדשה") && ok.contains(Words.Db.modeEmpty(0, 0, 0)));
        String bad = fullReport(opener, dir, true, true);
        ck("בדיקה מלאה: מסד אמיתי שחסרים בו קישור והפניה — פסק דין אדום, בעיה אחת (העתקה חוזרת תשלים)", bad.startsWith(Words.Db.verdictBad(1)) && bad.contains(Words.Db.pRealBehind("ראשון", 0, 0, 0, 1, 0, 1)));
        ck("בדיקה מלאה: רשימות חכמות — במקור 4, נמצאו 4, אפס שגויות; לא משויכים צפוי 1 ונמצא 1",
                ok.contains(Words.Db.modeTempSmart(4, 4, 0, 1, 1)));
        ck("בדיקה מלאה: הערכה מול האינדקס — 3 תנאים, 2 עם תוצאות, אחד בלי, אחד בלי תנאי",
                ok.contains("נבדקו 3 תנאים · עם תוצאות: 2 · בלי תוצאות: 1 · לא הובנו: 0 · בלי תנאי: 1 · סה״כ 5 קבצים")
                        && ok.contains(Words.Db.evalNoHits("אחרונים")));
        ck("בדיקה מלאה: הפניות — 6 במקור, 2 הועתקו, 3 נחסמו כמעגל, 1 לא קיימת, אפס שגויות, אפס מעגלים במסד",
                ok.contains(Words.Db.modeRefs(6, 2, 3, 1, 0, 0)));
        ck("בדיקה מלאה: הפניה מהמנוע הישן — מועתקת", ok.contains(Words.Db.modeRefs(1, 1, 0, 0, 0, 0)));
        ck("בדיקה מלאה: מסך הבית — 10 כלים, 5 צפויים ונמצאו, אחד לא מועתק, 3 בלי רשימה או קובץ, אותו סדר",
                ok.contains(Words.Db.modeHome(10, 5, 5, 1, 3, true)));
        ck("בדיקה מלאה: תיקיות חיות — נספרות כמועתקות, והחברות נבדקת (פסק הדין ירוק)",
                ok.contains(Words.Db.modeSource(2, 1, 0, 0, 0, 0, 4, 0)));
        ck("בדיקה מלאה: אופן בלי כלים — בית ריק בלי בעיה", ok.contains(Words.Db.modeHome(0, 0, 0, 0, 0, true)));
        String hok = fullReport(opener, dir, false, true, false, 1);
        ck("בדיקה מלאה: בית במסד האמיתי — הכול נמצא", hok.startsWith(Words.Db.VERDICT_OK) && hok.contains(Words.Db.modeRealHome(5, 5, 0)));
        String hbad = fullReport(opener, dir, false, true, false, 2);
        ck("בדיקה מלאה: חסר כלי בבית במסד האמיתי — פסק דין אדום, בעיה אחת", hbad.startsWith(Words.Db.verdictBad(1))
                && hbad.contains(Words.Db.pHomeBehind("בית", 1)) && hbad.contains(Words.Db.modeRealHome(5, 4, 1)));
        String badc = fullReport(opener, dir, false, true, true);
        ck("בדיקה מלאה: תנאי שהמנוע לא הבין — פסק דין אדום", badc.startsWith(Words.Db.verdictBad(1)) && badc.contains(Words.Db.pSmartCond("חכם", 1))
                && badc.contains(Words.Db.evalBad("צילומי מסך", "לא הובן")));
        String self = fullReport(opener, dir, false, false);
        ck("בדיקה מלאה: בדיקה עצמית שנכשלה — פסק דין אדום", self.startsWith(Words.Db.verdictBad(1)) && self.contains(Words.Db.pSelfTest()));
        // מ0.5: גיבוי
        ck("בדיקה מלאה: תיקיית גיבויים ריקה — מדווח, לא בעיה", ok.contains(Words.Db.BACKUPS_NONE));
        boolean sql = label.contains("SqlStore");
        ck("בדיקה מלאה: עותק, גיבוי טקסט ושחזור — זהים למסד (רק על SQLite)",
                sql == (ok.contains("עותק דרך SQLite — זהה") && ok.contains("שוחזר למסד ריק וזהה") && ok.contains("שחזור מעותק — זהה")));
        if (!sql) return;
        File bdir = new File(dir, "backups-ראשון");
        Backups bk = new Backups(bdir, new File(dir, "bk-tmp"));
        SqlStore s = (SqlStore) opener.fresh(new File(dir, "bk-counter.txt"));
        try {
            s.createList("x", 0);
            bk.daily(s, System.currentTimeMillis());
            bk.manual(s, System.currentTimeMillis());
        } finally {
            s.close();
        }
        String withBk = fullReport(opener, dir, false, true);
        ck("בדיקה מלאה: הגיבויים בתיקייה נבדקים — 4 תקינים (יומי, טקסט, ידני כעותק וכטקסט)", withBk.startsWith(Words.Db.VERDICT_OK)
                && withBk.contains("גיבויים בתיקייה: 4 (יומיים 1, לפני פעולה 0, טקסט 1, ידניים 2)") && withBk.contains("תקינים: 4 · שונו: 0 · פגומים: 0"));
        Backups.Entry victim = bk.list().get(bk.list().size() - 1);
        java.io.RandomAccessFile raf = new java.io.RandomAccessFile(victim.file, "rw");
        try {
            raf.seek(victim.file.length() / 2);
            raf.write(new byte[]{1, 2, 3, 4, 5, 6, 7, 8});
        } finally {
            raf.close();
        }
        String changed = fullReport(opener, dir, false, true);
        ck("בדיקה מלאה: גיבוי שהשתנה אחרי שנרשם — פסק דין אדום", changed.startsWith(Words.Db.verdictBad(1)) && changed.contains(Words.Db.pBackupsChanged("ראשון", 1)));
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
