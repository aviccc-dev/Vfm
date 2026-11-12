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
import java.util.Map;

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


    private static List<String> names(Store s, long list) {
        List<String> out = new ArrayList<String>();
        for (Store.Member m : s.members(list)) out.add(m.item.name);
        return out;
    }

    private static long listBy(Store s, String path) {
        return s.findByOrigin(DbPaths.originOf("עבודה", path));
    }

    private static long listIn(Store s, String mode, String path) {
        return DbPaths.find(s, mode, Names.parsePath(path));                   // מ0.29: לפי השמות (אין עוד מפתח מקור מהישן)
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



    // ---------- פקודות רשימות במסד (מ0.10) ----------

    void paths(Store s, File dir) throws Exception {
        DbMore.importText(s, 0, "#1 ספרים\n#2 ספרים / הלכה\n#1 מועדים\n#2 מועדים / הלכה\n");   // מ0.29: הייבוא של המסד (עד כאן — ההעתקה מהישן)
        long books = listIn(s, "פק", "ספרים"), hal = listIn(s, "פק", "ספרים / הלכה"), moHal = listIn(s, "פק", "מועדים / הלכה");
        ck("פקודות: פתח רשימה — לפי המקור", DbPaths.find(s, "פק", Names.parsePath("ספרים/הלכה")) == hal
                && DbPaths.find(s, "פק", Names.parsePath("מועדים / הלכה")) == moHal);
        ck("פקודות: לא קיימת — 0", DbPaths.find(s, "פק", Names.parsePath("אין/כזו")) == 0 && DbPaths.find(s, "פק", Names.parsePath("")) == 0);
        long n1 = s.createList("חדש", 0), n2 = s.createList("פנים", n1);
        ck("פקודות: רשימה שנוצרה במסד (בלי מקור) — לפי השמות", DbPaths.find(s, "פק", Names.parsePath("חדש/פנים")) == n2);
        long last = s.lastId();
        ck("פקודות: צור רשימה קיימת — אותה רשימה, בלי מספר חדש", DbPaths.ensure(s, "פק", Names.parsePath("ספרים/הלכה")) == hal && s.lastId() == last);
        long made = DbPaths.ensure(s, "פק", Names.parsePath("ספרים/הלכה/שבת"));
        ck("פקודות: צור רשימה — החלק החסר נוצר בתוך הקיים", made != 0 && s.listsOf(made).equals(Arrays.asList(hal)) && s.item(made).name.equals("שבת"));
        long deep = DbPaths.ensure(s, "פק", Names.parsePath("א/ב/ג"));
        long a1 = DbPaths.find(s, "פק", Names.parsePath("א"));
        ck("פקודות: צור רשימה — כל הדרך, הראשונה עומדת לעצמה", deep != 0 && a1 != 0 && s.listsOf(a1).isEmpty()
                && DbPaths.find(s, "פק", Names.parsePath("א/ב/ג")) == deep);
        long home = s.createList("בית", 0);
        s.setProp(home, Store.P_HOME, "1");
        s.addMember(home, books);
        ck("פקודות: רשימה שבבית — עדיין בראש", DbPaths.find(s, "פק", Names.parsePath("ספרים")) == books);
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
        long made2 = DbPaths.ensure(s, "פק", Names.parsePath("חדשה בראש"));
        ck("פקודות: צור רשימה חדשה בראש — נכנסת לבית", made2 != 0 && s.listsOf(made2).equals(Arrays.asList(home))
                && DbPaths.find(s, "פק", Names.parsePath("חדשה בראש")) == made2);
        DbSmartForm.Result r = new DbSmartForm.Result();
        r.name = "מהטופס";
        r.cond = "סוג:ספרים";
        r.sort = "שם";
        r.view = "ברצף";
        r.why = "למה כך";
        long fs = DbSmartForm.save(s, 0, home, r);
        ck("טופס חכמה: נוצרת בבית עם כל השדות", s.item(fs).type == Store.SMART && s.listsOf(fs).equals(Arrays.asList(home))
                && "סוג:ספרים".equals(s.prop(fs, Store.P_COND)) && "ברצף".equals(s.prop(fs, Store.P_VIEW)) && "למה כך".equals(s.prop(fs, Store.P_WHY)));
        r.name = "";
        r.cond = "סוג:תמונות";
        r.why = "";
        ck("טופס חכמה: עריכה — אותו פריט, שם נשאר כשריק, שדה שנמחק נמחק", DbSmartForm.save(s, fs, 0, r) == fs
                && s.item(fs).name.equals("מהטופס") && "סוג:תמונות".equals(s.prop(fs, Store.P_COND)) && s.prop(fs, Store.P_WHY) == null);
        ck("פקודות: המסד תקין", s.check() == null);
    }

    // ---------- הספירה במראה המוכר (מ0.9.1) ----------

    // ---------- מ0.12: פקודות הרשימות על המסד (DbListCmd) ----------

    private static ListCmd.Side[] sides(String line) {
        ListCmd.Parsed p = ListCmd.parse(line, new ArrayList<String>());
        ListCmd.Side[] out = new ListCmd.Side[p.sides.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = p.sides.get(i);
            if (out[i].list()) ListCmd.resolve(out[i], null);
        }
        return out;
    }

    private static long at(Store s, String path) {
        return DbPaths.find(s, "פק", Names.parsePath(path));
    }

    private static int filesIn(Store s, long list) {
        int n = 0;
        for (Store.Member m : s.members(list)) if (m.item.type == Store.FILE) n++;
        return n;
    }

    /** מ0.24: הלוח של המסד (DbClip) — כל פריט עם הפעולה שלו, צירוף, מקור שנמחק, אופן אחר; "העתק" — עותק עד הסוף. */
    void fixes24(Store s) throws Exception {
        long home = s.createList("בית", 0);
        s.setProp(home, Store.P_HOME, "1");
        long a = s.createList("א", home), b = s.createList("ב", home), c = s.createList("ג", home);
        long sub = s.createList("משנה", a);
        s.addMember(home, sub);                                              // רשימה שנמצאת בשני מקומות
        DbFiles.add(s, a, Arrays.asList("/s/1.pdf", "/s/2.pdf"), null);
        long f1 = s.findFile("/s/1.pdf"), f2 = s.findFile("/s/2.pdf");
        DbClip.clear();
        ck("לוח: ריק בהתחלה", DbClip.empty() && DbClip.cannot("x") == null);

        DbClip.put("x", a, Arrays.asList(s.item(f1)), DbClip.COPY, false);
        DbClip.put("x", a, Arrays.asList(s.item(f2)), DbClip.MOVE, false);
        ck("לוח: בלי צירוף — מתחלף", DbClip.items().size() == 1 && DbClip.items().get(0).id == f2);
        DbClip.put("x", a, Arrays.asList(s.item(f1), s.item(sub)), DbClip.COPY, true);
        ck("לוח: צירוף — מוסיף; התווית לפי הפעולות", DbClip.items().size() == 3
                && DbClip.label().equals("2" + Words.ClipW.TO_COPY + " · 1" + Words.ClipW.TO_MOVE) && DbClip.anyMove());
        DbClip.put("x", a, Arrays.asList(s.item(f2)), DbClip.COPY, true);
        ck("לוח: אותו פריט מאותה רשימה — הפעולה מתעדכנת, בלי כפילות", DbClip.items().size() == 3 && !DbClip.anyMove());
        DbClip.put("x", a, Arrays.asList(s.item(f2)), DbClip.MOVE, true);
        ck("לוח: אופן אחר — אי אפשר להדביק", DbClip.cannot("y") != null && DbClip.cannot("x") == null);

        DbListCmd.Copied r = DbClip.paste(s, b);
        ck("לוח: הדבקה — כולם נכנסים ליעד", r.done == 3 && s.contains(b, f1) && s.contains(b, f2));
        ck("לוח: \"העבר\" — יצא מהמקור; \"העתק\" — נשאר במקור", !s.contains(a, f2) && s.contains(a, f1));
        long subCopy = 0;
        for (Store.Member m : s.members(b)) if (m.item.type == Store.LIST && m.item.name.equals("משנה")) subCopy = m.item.id;
        ck("לוח: רשימה בהעתקה — עותק עצמאי (גם כשהיא בשני מקומות)", subCopy != 0 && subCopy != sub && s.listsOf(sub).size() == 2);
        ck("לוח: אחרי הדבקה — \"העבר\" יצאו מהלוח, \"העתק\" נשארו", DbClip.items().size() == 2 && !DbClip.anyMove());

        r = DbClip.paste(s, c);
        ck("לוח: אפשר להדביק שוב במקום נוסף", r.done == 2 && s.contains(c, f1));
        DbClip.clear();
        long gone = s.createList("זמנית", home);
        DbClip.put("x", home, Arrays.asList(s.item(gone)), DbClip.COPY, false);
        DbListCmd.removeFrom(s, home, gone);
        r = DbClip.paste(s, c);
        ck("לוח: המקור נמחק לפני ההדבקה — לא נכנס, \"המקור כבר לא קיים\"", r.done == 0 && r.skipped == 1
                && Words.DbClipW.GONE.equals(r.why));
        DbClip.clear();
        DbClip.put("x", home, Arrays.asList(s.item(a)), DbClip.MOVE, false);
        r = DbClip.paste(s, sub);
        ck("לוח: העברת רשימה לתוך עצמה — לא נכנסת, ונשארת בלוח", r.done == 0 && r.skipped == 1 && DbClip.items().size() == 1
                && s.contains(home, a));
        DbClip.clear();
    }

    /** מ0.23: סט התיקונים — המסד עוקב אחרי הכונן, בורר הרשימות, בדיקת השם האחת, תיקייה לרשימה, הכלים ב־⭐. */
    void fixes23(Store s, File dir) throws Exception {
        final String M = "x";
        long home = s.createList("בית", 0);
        s.setProp(home, Store.P_HOME, "1");
        long a = s.createList("א", home), b = s.createList("ב", a), c = s.createList("ג", home);
        String p1 = "/storage/emulated/0/ת/1.pdf", p2 = "/storage/emulated/0/ת/2.pdf", p3 = "/storage/emulated/0/ת/3.pdf";
        DbFiles.add(s, a, Arrays.asList(p1, p2), null);
        DbFiles.add(s, b, Arrays.asList(p1), null);
        DbFiles.add(s, c, Arrays.asList(p3), null);
        long f1 = s.findFile(p1), f3 = s.findFile(p3);

        // --- המסד עוקב אחרי הכונן (DbFollow) ---
        ck("עוקב: ספירה — בכמה רשימות (כל הופעה)", DbFollow.count(s, Arrays.asList(p1, p2, "/nope")) == 3);
        java.util.Map<String, String> mv = new java.util.LinkedHashMap<String, String>();
        mv.put(p1, "/storage/ABCD-1234/חדש/1.pdf");
        mv.put("/nope", "/x");
        ck("עוקב: העברה — אותו פריט, המסלול והשם החדשים, ברשימות כמו קודם", DbFollow.moved(s, mv) == 1
                && s.findFile("/storage/ABCD-1234/חדש/1.pdf") == f1 && s.findFile(p1) == 0
                && s.file(f1).drive.equals("ABCD-1234") && s.listsOf(f1).equals(Arrays.asList(a, b)));
        java.util.Map<String, String> onto = new java.util.HashMap<String, String>();
        onto.put("/storage/ABCD-1234/חדש/1.pdf", p3);                      // החליף את 3 — החברות עוברת אליו
        ck("עוקב: העברה על קובץ שיש לו פריט — החברות עוברת אליו, הישן יוצא מהרשימות", DbFollow.moved(s, onto) == 1
                && s.listsOf(f3).containsAll(Arrays.asList(a, b, c)) && s.listsOf(f1).isEmpty());
        ck("עוקב: מחיקה — יוצא מכל הרשימות (הפריט נשאר)", DbFollow.gone(s, Arrays.asList(p3)) == 3
                && s.listsOf(f3).isEmpty() && s.item(f3) != null && DbFollow.count(s, Arrays.asList(p3)) == 0);
        ck("עוקב: בלי כלום — 0", DbFollow.moved(s, new java.util.HashMap<String, String>()) == 0
                && DbFollow.gone(s, new ArrayList<String>()) == 0);
        s.setProp(b, Store.P_DESIGN, "כחול");
        ck("עוקב: שינוי שם של עיצוב — הרשימות עוברות לשם החדש; מחיקה — יורשות",
                DbFollow.designRenamed(s, "כחול", "ים") == 1 && "ים".equals(s.prop(b, Store.P_DESIGN))
                        && DbFollow.designRenamed(s, "ים", "") == 1 && s.prop(b, Store.P_DESIGN) == null);

        // --- בורר הרשימות של המסד (DbPicker.Snap) ---
        long a2 = s.createList("א", home);                                   // שם כפול באותה רשימה (נוצר לפני התיקון)
        long loose = s.createList("עצמאית", 0);
        long sm = s.createSmart("חכמה-בבית", home), sm2 = s.createSmart("חכמה-בא", a), sm3 = s.createSmart("לבד", 0);
        DbPicker.Snap snap = DbPicker.Snap.of(s);
        ck("בורר: השורש — הרשימות שבבית, ואחריהן מה שאינו בשום רשימה", snap.kids.get("").equals(Arrays.asList("א", "ג", "א (2)", "עצמאית")));
        ck("בורר: לכל מסלול — הרשימה הנכונה (גם הכפולה)", snap.id(Names.parsePath("א/ב")) == b && snap.id(Names.parsePath("א (2)")) == a2
                && snap.id(Names.parsePath("עצמאית")) == loose && snap.id(Names.parsePath("אין")) == 0 && snap.home == home);
        ck("בורר: כל המסלולים בצורת הפקודות", snap.paths().equals(Arrays.asList("א", "א\\ב", "ג", "א (2)", "עצמאית")));
        ck("בורר: החכמות בשם שהפקודות מכירות", snap.smarts.equals(Arrays.asList("חכמה-בבית", "לבד", "א\\חכמה-בא")));
        DbFiles.add(s, a2, Arrays.asList(p2), null);
        java.util.Map<String, List<String>> where = DbPicker.whereListed(s);
        ck("בורר: איפה כל קובץ — מסלול מלא", where.get(p2) != null && where.get(p2).containsAll(Arrays.asList("א", "א (2)"))
                && where.get(p2).size() == 2 && where.get(p3) == null);
        Tree mt = DbPicker.asTree(s);

        // --- בדיקת השם האחת (REN, הגדרות, ➕) ---
        ck("שם: ➕ רשימה חדשה בשם תפוס — שגיאה, לא כפולה", DbListCmd.newListChecked(s, home, "ג").error != null
                && DbListCmd.newListChecked(s, home, "ג").id == 0);
        ck("שם: ➕ עם / — שגיאה (לא נמחק בשקט)", Words.DbCmd.BAD_NAME.equals(DbListCmd.newListChecked(s, home, "ד/ה").error));
        DbListCmd.Made ok = DbListCmd.newListChecked(s, 0, "ד");
        ck("שם: ➕ שם פנוי — נוצרת (0 — בבית)", ok.error == null && ok.id != 0 && s.listsOf(ok.id).equals(Arrays.asList(home)));
        ck("שם: הגדרות — שם תפוס באחת הרשימות שהיא בהן — שגיאה, בלי שינוי",
                DbListCmd.edit(s, c, "ד", "", "", "") != null && s.item(c).name.equals("ג"));
        ck("שם: הגדרות — עם / — שגיאה", Words.DbCmd.BAD_NAME.equals(DbListCmd.edit(s, c, "ג/ז", "", "", "")));
        ck("שם: הגדרות — שם פנוי — משתנה, וגם הצלמית", DbListCmd.edit(s, c, "ז", "🌟", "", "") == null && s.item(c).name.equals("ז")
                && "🌟".equals(s.prop(c, Store.P_ICON)));
        ck("שם: הגדרות — אותו שם — בלי שגיאה (רק הפרטים)", DbListCmd.edit(s, c, "ז", "📘", "", "") == null && "📘".equals(s.prop(c, Store.P_ICON)));
        ck("שם: חכמה — אותה בדיקה (תפוס)", DbListCmd.edit(s, sm, "חכמה-בבית", "", "", "") == null
                && DbListCmd.nameError(s, Arrays.asList(home), 0, Store.SMART, "חכמה-בבית") != null);

        // --- תיקייה מהכונן לרשימה (DbMore.liveFolder / attach) ---
        ck("תיקייה: שם פנוי — (2) כשתפוס", DbMore.freeName(s, home, Store.LIST, "ד").equals("ד (2)") && DbMore.freeName(s, home, Store.LIST, "ח").equals("ח"));
        long live = DbMore.liveFolder(s, ok.id, "/storage/emulated/0/תמונות");
        ck("תיקייה חיה: חכמה בתוך הרשימה, בשם התיקייה, עם התנאי של התיקייה", live != 0 && s.item(live).type == Store.SMART
                && s.item(live).name.equals("תמונות") && s.listsOf(live).equals(Arrays.asList(ok.id))
                && (Cond.SOURCE + ":" + Cond.quote("/storage/emulated/0/תמונות")).equals(s.prop(live, Store.P_COND)));
        ck("תיקייה חיה: אותה תיקייה שוב — בלי כפולה", DbMore.liveFolder(s, ok.id, "/storage/emulated/0/תמונות") == 0);
        Tree.Node scan = new Tree.Node();
        scan.name = "תיקייה";
        scan.entries.add(new Tree.Entry("/storage/emulated/0/תיקייה/a.txt", 3, false));
        Tree.Node subn = new Tree.Node();
        subn.name = "פנימית";
        subn.entries.add(new Tree.Entry("/storage/emulated/0/תיקייה/פנימית/b.txt", 4, false));
        scan.children.add(subn);
        int[] st = DbMore.attach(s, ok.id, scan, true, false);
        long tl = DbPaths.find(s, M, Names.parsePath("ד/תיקייה")), pl = DbPaths.find(s, M, Names.parsePath("ד/תיקייה/פנימית"));
        ck("תיקייה עם המבנה: רשימה לכל תיקייה, הקבצים בתוכן", st[0] == 2 && st[1] == 2 && tl != 0 && pl != 0
                && s.count(tl) == 2 && s.count(pl) == 1);
        Tree.Node flat = new Tree.Node();
        flat.name = "שטוחה";
        flat.entries.add(new Tree.Entry("/storage/emulated/0/תיקייה/a.txt", 3, false));
        flat.entries.add(new Tree.Entry("/storage/emulated/0/תיקייה/c.txt", 3, false));
        int[] sf = DbMore.attach(s, ok.id, flat, false, false);
        ck("תיקייה — הקבצים בלבד: ישר לתוך הרשימה, בלי רשימה חדשה", sf[0] == 0 && sf[1] == 2);
        int[] sa = DbMore.attach(s, ok.id, scan, true, false);
        ck("תיקייה עם המבנה פעמיים — שם פנוי, לא מתערבב", sa[0] == 2 && DbPaths.find(s, M, Names.parsePath("ד/תיקייה (2)")) != 0);

        // --- יצוא וייבוא ב־⭐ (DbCmdMenu.ensureTools) ---
        long root = s.createList(Reserved.ROOT, home);
        long cmds = s.createList(Reserved.CMDS, root);
        dir.mkdirs();
        File t1 = new File(dir, Words.DbCmd.TOOL_EXPORT + ".vbat"), t2 = new File(dir, Words.DbCmd.TOOL_IMPORT + ".vbat");
        write(t1, "x");
        write(t2, "y");
        List<String> added = DbCmdMenu.ensureTools(s, M, Arrays.asList(t1, t2));
        ck("כלים: נכנסים לרשימת הפקודות של המסד", added.equals(Arrays.asList(Words.DbCmd.TOOL_EXPORT, Words.DbCmd.TOOL_IMPORT))
                && s.count(cmds) == 2);
        DbListCmd.removeFrom(s, cmds, s.findFile(t1.getAbsolutePath()));
        ck("כלים: מה שהמשתמש הוציא — לא חוזר", DbCmdMenu.ensureTools(s, M, Arrays.asList(t1, t2)).isEmpty() && s.count(cmds) == 1);
        ck("כלים: השורות — יצוא ⟵ ידנית וקובץ \"*\"; ייבוא ⟵ קובץ וידנית; המקומות הפתוחים — רשימה ידנית",
                DbCmdMenu.dbTools().get(0)[2].startsWith("COPY ידנית \"<") && DbCmdMenu.dbTools().get(0)[2].endsWith("קובץ \"*\"")
                        && CmdTemplates.holeKind(Arrays.asList(DbCmdMenu.dbTools().get(0)[2]), Words.DbCmd.HOLE_EXPORT) == CmdTemplates.HOLE_MANUAL
                        && CmdTemplates.holeKind(Arrays.asList(DbCmdMenu.dbTools().get(1)[2]), Words.DbCmd.HOLE_INTO) == CmdTemplates.HOLE_MANUAL
                        && CmdTemplates.holeKind(Arrays.asList(DbCmdMenu.dbTools().get(1)[2]), Words.DbCmd.HOLE_FILE) == CmdTemplates.HOLE_FILE);   // מ0.31: בורר קבצים
        ck("כלים: אין רשימת פקודות — כלום", DbCmdMenu.ensureTools(new MemStore(IdCounter.memory()), M, Arrays.asList(t1)).isEmpty());
    }

    /** מ0.41: "בטל לא-משויכים" — רישומי קבצים שאינם באף רשימה נמחקים מהמסד; "ודא לא-משויכים /פריטים:N". */
    void loose(Store s) throws Exception {
        final String M = "פק";
        long home = s.createList("בית", 0);
        s.setProp(home, Store.P_HOME, "1");
        long a = DbPaths.ensure(s, M, Names.parsePath("א")), b = DbPaths.ensure(s, M, Names.parsePath("ב"));
        DbFiles.add(s, a, Arrays.asList("/s/1.txt", "/s/2.txt"), null);
        DbFiles.add(s, b, Arrays.asList("/s/2.txt", "/s/3.txt"), null);
        ListCmd.Side[] x = sides("RD לא-משויכים");
        ck("בטל לא-משויכים: צד אחד, בלי שם אחריו", x.length == 1 && x[0].kind == ListCmd.LOOSE);
        ck("בטל לא-משויכים: כשאין כאלה — בלי שינוי", DbListCmd.remove(s, M, x[0]).noop);
        String[] found = {""};
        ck("ודא לא-משויכים /פריטים:0", DbListCmd.verify(s, M, x[0], DbListCmd.expect(Arrays.asList("/פריטים:0")), found));
        DbListCmd.remove(s, M, sides("RD ידנית \"א\"")[0]);                  // 1.txt נשאר בלי רשימה; 2.txt עוד ברשימה ב
        ck("ודא לא-משויכים /פריטים:1 — אחרי ביטול רשימה", DbListCmd.verify(s, M, x[0], DbListCmd.expect(Arrays.asList("/פריטים:1")), found)
                && !DbListCmd.verify(s, M, x[0], DbListCmd.expect(Arrays.asList("/פריטים:0")), found) && found[0].endsWith("1"));
        ck("ודא לא-משויכים חסרה — לא עובר (אין לה קיימת/חסרה)", !DbListCmd.verify(s, M, x[0], DbListCmd.expect(Arrays.asList("חסרה")), found));
        DbListCmd.Res r = DbListCmd.remove(s, M, x[0]);
        ck("בטל לא-משויכים: הרישום נמחק מהמסד, ורק שלו", r.error == null && !r.noop && s.findFile("/s/1.txt") == 0
                && s.findFile("/s/2.txt") != 0 && s.findFile("/s/3.txt") != 0 && s.unassigned().isEmpty() && filesIn(s, b) == 2);
        ck("בטל לא-משויכים: פעם שנייה — בלי שינוי", DbListCmd.remove(s, M, x[0]).noop);
        ck("\"לא-משויכים\" במרכאות — שם רגיל, לא מילת סוג", ListCmd.parse("RD \"לא-משויכים\"", new ArrayList<String>()) == null);
        ListCmd.Parsed p = ListCmd.parse("COPY לא-משויכים ידנית \"ב\"", new ArrayList<String>());
        ck("לא-משויכים עם צד נוסף — נקרא כשני צדדים (השגיאה — בתכנון)", p != null && p.sides.size() == 2 && p.sides.get(0).kind == ListCmd.LOOSE);
        ck("תוכן לא-משויכים — שגיאת צורה", ListCmd.parse("RD תוכן לא-משויכים", new ArrayList<String>()).error != null);
    }

    void listCmd(Store s) throws Exception {
        final String M = "פק";
        DbListCmd.Finder f = new DbListCmd.Finder() {
            @Override public List<String> find(String cond) {
                return "x".equals(cond) ? Arrays.asList("/s/c.pdf") : new ArrayList<String>();
            }
        };
        long home = s.createList("בית", 0);
        s.setProp(home, Store.P_HOME, "1");
        long hal = DbPaths.ensure(s, M, Names.parsePath("ספרים/הלכה")), books = at(s, "ספרים");
        DbFiles.add(s, hal, Arrays.asList("/s/a.pdf", "/s/b.txt"), null);
        s.addNote(books, "רעיון");
        long sm = s.createSmart("חדשים", books);
        s.setProp(sm, Store.P_COND, "x");
        long shared = s.createList("משותף", home);
        s.addMember(hal, shared);
        ck("פקודות במסד: הכנה", books != 0 && s.listsOf(books).equals(Arrays.asList(home)));

        ListCmd.Side[] x = sides("MD ידנית \"תורה/גמרא\"");
        DbListCmd.Res r = DbListCmd.md(s, M, x[0]);
        long torah = at(s, "תורה"), gem = at(s, "תורה/גמרא");
        ck("MD במסד: נוצרת בכל הדרך, הראשונה בבית", !r.noop && r.error == null && gem != 0 && s.listsOf(torah).equals(Arrays.asList(home)));
        ck("MD במסד: קיימת — בלי שינוי", DbListCmd.md(s, M, x[0]).noop);

        x = sides("COPY ידנית \"ספרים\" ידנית \"תורה\"");
        r = DbListCmd.copy(s, M, x[0], x[1], null, f);
        long cBooks = at(s, "תורה/ספרים"), cHal = at(s, "תורה/ספרים/הלכה");
        boolean smartCopied = false, noteCopied = false;
        for (Store.Member m : s.members(cBooks)) {
            if (m.item.type == Store.SMART && m.item.id != sm && "x".equals(s.prop(m.item.id, Store.P_COND))) smartCopied = true;
            if (m.item.type == Store.NOTE && m.item.name.equals("רעיון")) noteCopied = true;
        }
        ck("COPY רשימה במסד: עותק עצמאי (מספר אחר), עם תת־הרשימה שלה", r.error == null && cBooks != 0 && cBooks != books && cHal != 0 && cHal != hal);
        ck("COPY רשימה במסד: אותם פריטי קבצים (לא קבצים חדשים)", filesIn(s, cHal) == 2 && s.findFile("/s/a.pdf") != 0
                && s.listsOf(s.findFile("/s/a.pdf")).size() == 2);
        ck("COPY רשימה במסד: חכמה — עותק של ההגדרה; רעיון — הועתק", smartCopied && noteCopied);
        long cShared = at(s, "תורה/ספרים/הלכה/משותף");
        ck("מ0.24: COPY רשימה במסד: גם רשימה שנמצאת גם בבית — עותק עצמאי (\"העתק\" — תמיד עותק)",
                cShared != 0 && cShared != shared && !s.contains(cHal, shared) && s.listsOf(shared).size() == 2);
        ck("COPY רשימה במסד: המקור לא השתנה", filesIn(s, hal) == 2 && s.members(books).size() == 3);
        r = DbListCmd.copy(s, M, x[0], x[1], null, f);
        ck("COPY במסד: שוב — לא נכנסה (כבר יש בשם הזה)", r.error == null && r.text.contains(Words.DbCmd.SAME_NAME));
        x = sides("COPY ידנית \"ספרים\" ידנית \"ספרים/הלכה\"");
        ck("COPY במסד: לא לתוך עצמה", Words.ListCmdW.INTO_ITSELF.equals(DbListCmd.copy(s, M, x[0], x[1], null, f).error));
        x = sides("COPY ידנית \"אין\" ידנית \"תורה\"");
        ck("COPY במסד: מקור שאינו קיים — שגיאה", Words.ListCmdW.noList("אין").equals(DbListCmd.copy(s, M, x[0], x[1], null, f).error));

        x = sides("COPY קבצי ידנית \"ספרים\" ידנית \"תורה/גמרא\"");
        r = DbListCmd.copy(s, M, x[0], x[1], null, f);
        ck("COPY קבצי ידנית במסד: כל העומק, וגם מה שהחכמה מוצאת", r.error == null && filesIn(s, gem) == 3);
        x = sides("COPY \"/s/d.txt\" ידנית \"תורה/גמרא\"");
        r = DbListCmd.copy(s, M, x[0], x[1], Arrays.asList("/s/d.txt", "/s/a.pdf"), f);
        ck("COPY קבצים מהכונן לרשימה במסד: רק מה שחסר", r.error == null && filesIn(s, gem) == 4);
        x = sides("COPY קבצי חכמה \"ספרים/חדשים\" ידנית \"תורה/גמרא\"");
        DbListCmd.Finder notReady = new DbListCmd.Finder() {
            @Override public List<String> find(String cond) { return null; }
        };
        ck("COPY קבצי חכמה במסד: האינדקס לא מוכן — שגיאה, בלי שינוי", Words.ListCmdW.INDEX_NOT_READY.equals(DbListCmd.copy(s, M, x[0], x[1], null, notReady).error)
                && filesIn(s, gem) == 4);

        x = sides("COMP ידנית \"ספרים\" ידנית \"תורה/גמרא\"");
        r = DbListCmd.comp(s, M, x[0], x[1], false, f);
        ck("COMP במסד: לפי המסלולים", r.error == null && r.text.equals(Words.ListCmdW.compared(x[0].label(), 0, x[1].label(), 1, 3) + "\n   d.txt"));

        x = sides("DEL ידנית \"תורה/גמרא/*.pdf\"");
        r = DbListCmd.remove(s, M, x[0]);
        ck("DEL קבצים לפי תבנית במסד: רק אלה יוצאים", r.error == null && filesIn(s, gem) == 2 && s.findFile("/s/a.pdf") != 0);
        x = sides("RD תוכן ידנית \"תורה/ספרים\"");
        long trashed0 = s.trashed().size();
        r = DbListCmd.remove(s, M, x[0]);
        ck("RD תוכן במסד: מתרוקנת; תת־רשימה שהייתה רק שלה — נמחקת; רעיון — לסל", r.error == null && s.members(cBooks).isEmpty()
                && s.item(cHal) == null && s.trashed().size() == trashed0 + 1 && s.item(shared) != null);
        x = sides("RD ידנית \"תורה\"");
        r = DbListCmd.remove(s, M, x[0]);
        ck("RD רשימה במסד: נמחקת עם מה שרק שלה; המקור לא נוגע", r.error == null && at(s, "תורה") == 0 && s.item(gem) == null
                && s.item(cBooks) == null && filesIn(s, hal) == 2 && s.findFile("/s/d.txt") != 0);
        ck("RD במסד: רשימה שאינה קיימת — בלי שינוי", DbListCmd.remove(s, M, x[0]).noop);
        x = sides("RD חכמה \"ספרים/חדשים\"");
        r = DbListCmd.remove(s, M, x[0]);
        ck("RD חכמה במסד: יוצאת ונמחקת", r.error == null && s.item(sm) == null);
        x = sides("RD ידנית \"*\"");
        ck("RD במסד: את הבית — לא", Words.ListCmdW.HOME_NO_REMOVE.equals(DbListCmd.remove(s, M, x[0]).error));
        x = sides("RD ידנית \"ספרים/הלכה\"");
        r = DbListCmd.remove(s, M, x[0]);
        ck("RD במסד: רשימה שגם במקום אחר — רק יוצאת מכאן", r.error == null && s.item(hal) == null && s.item(shared) != null
                && s.listsOf(shared).equals(Arrays.asList(home)));

        // בחירה מרובה (מ0.16): העתקה / קישור / העברה לפי מספרי זיהוי
        long ta = s.createList("יעד-א", home), tb = s.createList("יעד-ב", home);
        long f1 = s.findFile("/s/a.pdf"), lst = s.createList("פנימית", ta);
        DbListCmd.Copied lk = DbListCmd.linkInto(s, Arrays.asList(lst, f1), tb);
        ck("קישור: אותה רשימה ואותו קובץ גם ביעד", lk.done == 2 && s.contains(tb, lst) && s.listsOf(lst).size() == 2);
        ck("קישור: שוב — לא נכנס (כבר בה); לתוך עצמו — לא", DbListCmd.linkInto(s, Arrays.asList(lst), tb).skipped == 1
                && DbListCmd.linkInto(s, Arrays.asList(tb), tb).skipped == 1);
        long f2 = s.createFile(new Store.FileInfo("/s/m.txt", "פנימי", 1, 0, null));
        s.addMember(ta, f2);
        DbListCmd.Copied mv = DbListCmd.moveTo(s, Arrays.asList(f2), ta, tb);
        ck("העברה: נכנס ליעד ויצא מהמקור", mv.done == 1 && s.listsOf(f2).equals(Arrays.asList(tb)));
        DbListCmd.Copied cpi = DbListCmd.copyIds(s, Arrays.asList(ta, tb), ta);
        ck("העתקה לפי מספרים: רשימה לא לתוך עצמה (נספרת)", cpi.skipped >= 1 && Words.ListCmdW.INTO_ITSELF.equals(cpi.why));

        // קשר / העבר בפקודה (מ0.17)
        DbListCmd.Res lr = DbListCmd.link(s, M, sides("LINK ידנית \"יעד-א/פנימית\" ידנית \"ספרים\"")[0],
                sides("LINK ידנית \"יעד-א/פנימית\" ידנית \"ספרים\"")[1], false);
        ck("קשר: אותה רשימה גם ביעד", lr.error == null && s.contains(books, lst) && s.contains(ta, lst));
        DbListCmd.Res mr = DbListCmd.link(s, M, sides("MOVE ידנית \"יעד-א/פנימית\" ידנית \"יעד-ב\"")[0],
                sides("MOVE ידנית \"יעד-א/פנימית\" ידנית \"יעד-ב\"")[1], true);
        ck("העבר: יוצאת מהרשימה שמעליה (כבר ביעד — לא נכנסה, ולכן גם לא יצאה)", mr.error == null && s.contains(ta, lst));
        long lone = s.createList("לבד", ta);
        DbListCmd.link(s, M, sides("MOVE ידנית \"יעד-א/לבד\" ידנית \"יעד-ב\"")[0], sides("MOVE ידנית \"יעד-א/לבד\" ידנית \"יעד-ב\"")[1], true);
        ck("העבר: נכנסת ליעד ויוצאת מהמקור", s.listsOf(lone).equals(Arrays.asList(tb)));
        ck("העבר/קשר: לא לתוך עצמה", Words.ListCmdW.INTO_ITSELF.equals(DbListCmd.link(s, M, sides("LINK ידנית \"יעד-ב\" ידנית \"יעד-ב/לבד\"")[0],
                sides("LINK ידנית \"יעד-ב\" ידנית \"יעד-ב/לבד\"")[1], false).error));

        // יצוא (מ0.18): הטקסט נקרא בחזרה בהעתקה מהישן — אותן רשימות ואותם קבצים
        DbExport.Out eo = DbExport.list(s, books, false, "רשימה ספרים", 0);
        File em = new File(System.getProperty("java.io.tmpdir"), "export" + System.nanoTime());
        MemStore back = new MemStore(IdCounter.memory());
        DbMore.importText(back, 0, eo.text);                                  // מ0.29: נקרא בחזרה בייבוא של המסד
        long bb = DbPaths.find(back, "פק", Names.parsePath("ספרים"));
        ck("יצוא: נקרא בחזרה — אותן רשימות ואותם קבצים (" + eo.lists + "/" + eo.files + ")", bb != 0
                && back.lists().size() == eo.lists && DbCount.of(back, bb, null).items == eo.files && eo.text.startsWith(DbExport.DESC));
        File ed = new File(em, "יצוא");
        ed.mkdirs();
        File e1 = DbExport.target("*", "רשימה ספרים", ed);
        write(e1, "x");
        ck("יצוא: שם לפי מה שיוצא, ותפוס — (2); שם בלי מסלול — בתיקיית היצוא; \\ כמו /",
                e1.getName().equals("רשימה ספרים.txt") && DbExport.target("*", "רשימה ספרים", ed).getName().equals("רשימה ספרים (2).txt")
                && DbExport.target("שלי", "x", ed).equals(new File(ed, "שלי.txt"))
                && DbExport.target(ed.getAbsolutePath() + "\\שלי.txt", "x", ed).equals(new File(ed, "שלי.txt")));

        // שינוי שם בפקודה (מ0.21)
        long rn = s.createList("לשינוי", ta);
        DbListCmd.Res rr = DbListCmd.rename(s, M, sides("REN ידנית \"יעד-א/לשינוי\" \"שונה\"")[0], "שונה");
        ck("REN: השם השתנה, ומוצאים לפי השם החדש", rr.error == null && s.item(rn).name.equals("שונה") && at(s, "יעד-א/שונה") == rn && at(s, "יעד-א/לשינוי") == 0);
        s.createList("תפוס", ta);
        ck("REN: שם תפוס באותה רשימה — לא; / בשם — לא", DbListCmd.rename(s, M, sides("REN ידנית \"יעד-א/שונה\" \"x\"")[0], "תפוס").error != null
                && DbListCmd.rename(s, M, sides("REN ידנית \"יעד-א/שונה\" \"x\"")[0], "א/ב").error != null && s.item(rn).name.equals("שונה"));

        // מ0.22: מיון, סידור ידני, ייבוא, העתקה בין מסדים, תפריט
        long so = s.createList("מיון", home);
        long fz = s.createFile(new Store.FileInfo("/s/z.txt", "פנימי", 10, 300, null)), fa2 = s.createFile(new Store.FileInfo("/s/aa.txt", "פנימי", 99, 100, null));
        s.addMember(so, fz);
        s.addMember(so, fa2);
        long sub = s.createList("תת", so);
        List<Store.Member> rows = new ArrayList<Store.Member>(s.members(so));
        DbMore.sort(s, rows, Sort.NAME);
        ck("מיון: רשימות קודם, ואז קבצים לפי שם", rows.get(0).item.id == sub && rows.get(1).item.id == fa2 && rows.get(2).item.id == fz);
        DbMore.sort(s, rows, Sort.DATE);
        ck("מיון לפי תאריך: החדש ראשון", rows.get(1).item.id == fz);
        DbMore.sort(s, rows, Sort.SIZE);
        ck("מיון לפי גודל: הגדול ראשון", rows.get(1).item.id == fa2);
        DbMore.order(s, so, Arrays.asList(sub), DbMore.BOTTOM);
        ck("סידור: לסוף", s.members(so).get(2).item.id == sub);
        DbMore.order(s, so, Arrays.asList(sub), DbMore.UP);
        ck("סידור: מקום אחד למעלה", s.members(so).get(1).item.id == sub);
        DbMore.order(s, so, Arrays.asList(sub, fz), DbMore.TOP);
        ck("סידור: כמה לראש — הסדר ביניהם נשמר", s.members(so).get(0).item.id == fz && s.members(so).get(1).item.id == sub);
        DbMore.order(s, so, Arrays.asList(fa2), DbMore.DOWN);
        ck("סידור: למטה בסוף — נשאר", s.members(so).get(2).item.id == fa2);

        DbExport.Out ex2 = DbExport.list(s, books, false, "x", 0);
        long imp = s.createList("מיובא", home);
        DbMore.Imported im = DbMore.importText(s, imp, ex2.text);
        ck("ייבוא: מה שיוצא — נכנס (רשימות וקבצים), בתוך היעד", im.lists == ex2.lists && im.files == ex2.files && im.bad == 0
                && DbCount.of(s, imp, null).lists == ex2.lists && DbPaths.find(s, M, Names.parsePath("מיובא/ספרים")) != 0);
        DbMore.Imported im2 = DbMore.importText(s, imp, ex2.text);
        ck("ייבוא שוב: אותן רשימות (לא כפולות), קבצים שכבר שם — לא שוב", im2.files == 0 && DbCount.of(s, imp, null).lists == ex2.lists);
        DbMore.Imported im3 = DbMore.importText(s, imp, DbExport.paths(Arrays.asList("/s/a.pdf", "/s/q.txt"), "x", 0).text);
        ck("ייבוא קבצי רשימה: המסלולים נכנסים ליעד עצמו", im3.files == 2 && s.contains(imp, s.findFile("/s/q.txt")));

        MemStore other = new MemStore(IdCounter.memory());
        long oh = other.createList("בית", 0);
        other.setProp(oh, Store.P_HOME, "1");
        long ob = other.createList("אחר", oh), obs = other.createList("פנים", ob);
        long ofile = other.createFile(new Store.FileInfo("/o/x.pdf", "פנימי", 5, 7, "2:abc"));
        other.addMember(obs, ofile);
        other.addNote(ob, "רעיון שם");
        DbListCmd.Res xr = DbListCmd.copyAcross(other, "שני", sides("COPY ידנית \"אחר\" ידנית \"מיובא\"")[0], s, imp, "\"מיובא\"");
        long cx = DbPaths.find(s, M, Names.parsePath("מיובא/אחר"));
        ck("מאופן לאופן: עותק עצמאי — רשימות, רעיון, וקובץ לפי המסלול עם החתימה", xr.error == null && cx != 0 && DbPaths.find(s, M, Names.parsePath("מיובא/אחר/פנים")) != 0
                && s.findFile("/o/x.pdf") != 0 && "2:abc".equals(s.file(s.findFile("/o/x.pdf")).sig) && other.lists().size() == 3);
        ck("מאופן לאופן: שוב — כבר יש בשם הזה", DbListCmd.copyAcross(other, "שני", sides("COPY ידנית \"אחר\" ידנית \"מיובא\"")[0], s, imp, "x").error != null);

        long mn = s.createList("תפריט", home);
        long bat = s.createFile(new Store.FileInfo("/s/run.vbat", "פנימי", 1, 0, null));
        s.addMember(mn, bat);
        s.addMember(mn, fz);
        long msub = s.createList("משנה", mn);
        long msm = s.createSmart("מוצאת", mn);
        List<DbMore.Entry> me = DbMore.menu(s, mn);
        ck("תפריט מהמסד: קובץ פקודות, תת־תפריט, חכמה — לפי הסדר; קובץ שאינו פקודות — לא", me.size() == 3 && me.get(0).kind == DbMore.E_FILE
                && me.get(1).id == msub && me.get(2).id == msm);

        // ודא (מ0.14)
        ListCmd.Side vb = sides("VERIFY ידנית \"ספרים\" קיימת")[0];
        String[] fd = {""};
        ck("ודא: קיימת — עוברת", DbListCmd.verify(s, M, vb, DbListCmd.expect(Arrays.asList("קיימת")), fd));
        ck("ודא: חסרה על קיימת — נכשלת, ואומרת מה בפועל", !DbListCmd.verify(s, M, vb, DbListCmd.expect(Arrays.asList("חסרה")), fd)
                && fd[0].equals(Words.DbCmd.WORD_EXISTS));
        DbCount bc = DbCount.of(s, books, null);
        ck("ודא: מספרים נכונים — עוברת; שגויים — נכשלת", DbListCmd.verify(s, M, vb, DbListCmd.expect(Arrays.asList("/רשימות:" + bc.lists, "/פריטים:" + bc.items)), fd)
                && !DbListCmd.verify(s, M, vb, DbListCmd.expect(Arrays.asList("/פריטים:" + (bc.items + 1))), fd));
        ListCmd.Side vn = sides("VERIFY ידנית \"אין כזו\" חסרה")[0];
        ck("ודא: חסרה על חסרה — עוברת; בלי תנאי — קיימת", DbListCmd.verify(s, M, vn, DbListCmd.expect(Arrays.asList("חסרה")), fd)
                && !DbListCmd.verify(s, M, vn, DbListCmd.expect(new ArrayList<String>()), fd));
        ck("ודא: תנאי לא מוכר / מספר שגוי — צורה", DbListCmd.expect(Arrays.asList("משהו")).error != null
                && DbListCmd.expect(Arrays.asList("/פריטים:x")).error != null && DbListCmd.expect(Arrays.asList("חסרה", "/רשימות:1")).error != null);
        ck("ודא: השם בעברית מוכר כפקודה", Batch.VERIFY.equals(Commands.canonical("ודא")));

        // כלול (מ0.15)
        File cd = new File(System.getProperty("java.io.tmpdir"), "call" + System.nanoTime());
        cd.mkdirs();
        write(new File(cd, "פנים.vbat"), "בדיקה\nMD ידנית \"מכלול\"\nכלול \"עמוק.vbat\"\n");
        write(new File(cd, "עמוק.vbat"), "ECHO עמוק\n");
        write(new File(cd, "מעגל.vbat"), "כלול \"מעגל.vbat\"\n");
        String[] ex = Batch.calls(new String[]{"ECHO א", "כלול \"פנים.vbat\"", "ECHO ב"}, cd, null, 0, new java.util.HashSet<String>());
        ck("כלול: השורות של הקובץ במקום השורה (גם כלול בתוך כלול), בלי שורת \"בדיקה\" שלו", Arrays.asList(ex).equals(Arrays.asList(
                "ECHO א", "ECHO " + Words.DbCmd.called("פנים.vbat", 3), "MD ידנית \"מכלול\"", "ECHO " + Words.DbCmd.called("עמוק.vbat", 1), "ECHO עמוק", "ECHO ב")));
        String[] cyc = Batch.calls(new String[]{"כלול \"מעגל.vbat\""}, cd, null, 0, new java.util.HashSet<String>());
        ck("כלול: מעגל — נעצר (השורה נשארת ונכשלת)", cyc.length == 2 && cyc[1].equals("כלול \"מעגל.vbat\""));
        String[] none = Batch.calls(new String[]{"כלול \"אין.vbat\""}, cd, null, 0, new java.util.HashSet<String>());
        ck("כלול: קובץ שלא קיים — השורה נשארת", none.length == 1);
        Batch.Plan bp = Batch.plan("כלול \"אין.vbat\"\nאופן \"x\"\n", cd, null);
        ck("כלול שנכשל ואופן מחוץ למסד — שגיאה בתכנון, עם הסבר", bp.steps.size() == 2 && Words.DbCmd.CALL_FAILED.equals(bp.steps.get(0).error)
                && Words.DbCmd.VERIFY_DB_ONLY.equals(bp.steps.get(1).error));

        long lists = s.lists().size();
        MemStore c = DbListCmd.copyOf(s);
        ck("העותק להדמיה: אותן רשימות, אותם קבצים, אותו בית, אותם מקורות", c.lists().size() == lists && c.findFile("/s/d.txt") != 0
                && DbPaths.home(c) != 0 && DbPaths.find(c, M, Names.parsePath("ספרים")) != 0);
        x = sides("RD ידנית \"ספרים\"");
        DbListCmd.remove(c, M, x[0]);
        ck("העותק להדמיה: שינוי בו לא נוגע במסד", at(s, "ספרים") == books && c.lists().size() < lists && s.lists().size() == lists);
        long nl = DbListCmd.newList(s, 0, "מהמסך");
        ck("הליבה (מסכים): רשימה חדשה — בבית", nl != 0 && s.listsOf(nl).equals(Arrays.asList(home)));
        long note = s.addNote(nl, "הערה");
        DbListCmd.removeFrom(s, nl, note);
        ck("הליבה (מסכים): רעיון שהוסר מהרשימה היחידה — לסל", s.members(nl).isEmpty() && "הערה".equals(s.trashed().get(s.trashed().size() - 1).name));
        DbListCmd.Copied cp = DbListCmd.copyInto(s, Arrays.asList(s.item(books)), nl);
        ck("הליבה (מסכים): העתקה לתוך רשימה — עותק עצמאי", cp.done == 1 && s.members(nl).size() == 1 && s.members(nl).get(0).item.id != books);
        DbListCmd.edit(s, nl, "מהמסך חדש", "⭐", "תיאור", "");
        ck("הליבה (מסכים): עריכה — שם, צלמית, תיאור", s.item(nl).name.equals("מהמסך חדש") && "⭐".equals(s.prop(nl, Store.P_ICON))
                && "תיאור".equals(s.prop(nl, Store.P_DESC)) && at(s, "מהמסך חדש") == nl);
        long hal2 = at(s, "ספרים"), copyInBooks = s.createList("זמני", hal2);
        s.setOrigin(copyInBooks, DbPaths.originOf(M, "ספרים / זמני"));
        DbListCmd.edit(s, copyInBooks, "קבוע", "", "", "");
        ck("שינוי שם: הפקודה מוצאת לפי השם החדש, לא לפי המקור הישן", at(s, "ספרים/קבוע") == copyInBooks && at(s, "ספרים/זמני") == 0);
        DbListCmd.removeFrom(s, home, nl);
        ck("הליבה (מסכים): הסרה מהבית — נמחקת עם מה שרק שלה", s.item(nl) == null && at(s, "ספרים") == books);
        ck("פקודות במסד: המסד תקין", s.check() == null);
    }

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


    // ---------- מ0.29: פורמט היצוא 2, הייבוא, והדוגמאות כקובץ יצוא ----------

    /** יצוא ← ייבוא למסד ריק ← יצוא: אותו טקסט; חכמות, קישור, כותרת, רעיון, חבורה והסדר — נשמרים. */
    void text2(Store s) throws Exception {
        long home = DbPaths.ensureHome(s);
        ck("בית: נוצר ומסומן, ופעם שנייה — אותו", home != 0 && DbPaths.home(s) == home && DbPaths.ensureHome(s) == home);
        long a = s.createList("א", home), sm = s.createSmart("חכמה ראשונה", home), b = s.createList("ב", home);
        s.setProp(sm, Store.P_COND, "סוג:ספרים");
        s.setProp(sm, Store.P_SORT, "תאריך");
        s.setProp(sm, Store.P_VIEW, "ברצף");
        s.setProp(sm, Store.P_WHY, "למה כך");
        long f1 = s.createFile(new Store.FileInfo("/storage/emulated/0/x/1.pdf", "פנימי", 10, 0, null));
        long f2 = s.createFile(new Store.FileInfo("/storage/emulated/0/x/2.pdf", "פנימי", -1, 0, null));
        s.addMember(a, f1);
        long in = s.createList("פנימית", a);
        s.addMember(a, f2);                                                   // הסדר: קובץ, תת־רשימה, קובץ
        s.setProp(in, Store.P_ICON, "📚");
        s.setProp(in, Store.P_GROUP, "1");
        long sm2 = s.createSmart("בתוך", in);
        s.setProp(sm2, Store.P_COND, "מקור:«/storage/emulated/0/x»");
        s.addTitle(in, "כותרת");
        s.addNote(in, "רעיון\nבשתי שורות");
        s.addMember(b, in);                                                   // אותה רשימה בשני מקומות — קישור
        DbExport.Out o = DbExport.list(s, home, true, "בית", 0);
        ck("פורמט 2: הכול בפורמט (אין דילוגים), 3 רשימות ו־2 קבצים", o.skipped == 0 && o.lists == 3 && o.files == 2);
        ck("פורמט 2: חכמה עם המאפיינים שלה", o.text.contains(DbExport.SMART + "חכמה ראשונה\n+תנאי סוג:ספרים\n+מיון תאריך\n+תצוגה ברצף\n+למה למה כך\n"));
        ck("פורמט 2: רשימה שכבר הופיעה — קישור", o.text.contains(DbExport.LINK + "א / פנימית\n"));
        ck("פורמט 2: הסדר — קובץ, תת־רשימה, קובץ", o.text.contains("/storage/emulated/0/x/1.pdf | 10\n" + DbExport.SUB + "פנימית\n/storage/emulated/0/x/2.pdf | ?\n"));
        MemStore back = new MemStore(IdCounter.memory());
        long h2 = DbPaths.ensureHome(back);
        DbMore.Imported im = DbMore.importText(back, h2, o.text);
        DbExport.Out o2 = DbExport.list(back, h2, true, "בית", 0);
        ck("פורמט 2: ייבוא למסד ריק ויצוא שוב — אותו טקסט", o.text.equals(o2.text) && im.bad == 0 && im.lists == 3 && im.files == 2 && im.smarts == 2);
        long in2 = DbPaths.find(back, "x", Names.parsePath("א/פנימית"));
        ck("פורמט 2: הקישור — אותה רשימה בשני מקומות", in2 != 0 && back.listsOf(in2).size() == 2);
        ck("פורמט 2: חבורה, צלמית, כותרת ורעיון (עם שורה חדשה)", "1".equals(back.prop(in2, Store.P_GROUP)) && "📚".equals(back.prop(in2, Store.P_ICON))
                && names(back, in2).contains("כותרת") && names(back, in2).contains("רעיון\nבשתי שורות"));
        DbMore.Imported again = DbMore.importText(back, h2, o.text);
        ck("פורמט 2: ייבוא שני — בלי רשימות, קבצים או חכמות כפולים", again.files == 0 && back.lists().size() == 4 && back.smarts().size() == 2);
        ck("פורמט 2: המסד תקין", back.check() == null && s.check() == null);
    }

    private static List<String> filesOf(List<Menus.Item> items) {
        List<String> out = new ArrayList<String>();
        for (Menus.Item it : items) out.add(it.e.kind == DbMore.E_FILE ? new File(it.e.path).getName() : (it.e.kind == DbMore.E_LIST ? "☰" : "📜") + it.e.label);
        return out;
    }

    /** מ0.41: התפריטים — ברירת המחדל מהקובץ שבהתקנה, המיזוג (הכללי ואחריו של האופן), השורות של מסך, והמעבר מהישן. */
    void menus(Store s, File asset, File dir) throws Exception {
        final String IN = "/storage/emulated/0", SYS = IN + "/" + Paths.APP_DIR + "/" + Reserved.SYS_DIR + "/";
        String text = new String(java.nio.file.Files.readAllBytes(asset.toPath()), "UTF-8");
        java.util.Set<String> known = new java.util.HashSet<String>();
        for (File f : Menus.systemFiles().values()) known.add(f.getName());
        boolean all = true;
        int refs = 0;
        for (String line : text.split("\n")) if (line.startsWith(DbExamples.IN)) {
            refs++;
            String name = line.substring(line.lastIndexOf('/') + 1, line.lastIndexOf(" | "));
            if (!line.startsWith(DbExamples.IN + "/" + Paths.APP_DIR + "/" + Reserved.SYS_DIR + "/") || !known.contains(name)) all = false;
        }
        ck("תפריטים: כל קובץ שבברירת המחדל — פקודת מערכת או כלי שהתוכנה כותבת (" + refs + ")", all && refs > 20);
        DbMore.Imported im = Menus.defaults(s, text, IN);
        long home = DbPaths.home(s);
        ck("תפריטים: ברירת המחדל נכנסת לבית בלי שורות שלא הובנו", im.bad == 0 && home != 0 && Menus.root(s, Menus.MODE) == home);
        Menus.Tree g = Menus.tree(s, Menus.MODE);
        Menus.Snap one = new Menus.Snap(g, null);
        List<Menus.Row> top = one.rows(null, Menus.ROW_TOP);
        ck("תפריטים: שורה למעלה 1 — בית, פקודות, חיפוש, קבצים פיזיים, חזרה", top.size() == 1 && top.get(0).n == 1
                && filesOf(top.get(0).items).equals(Arrays.asList("בית.vbat", "פקודות.vbat", "חיפוש.vbat", "קבצים פיזיים.vbat", "חזרה.vbat")));
        ck("תפריטים: אין שורה תחתונה כללית; בבית — העורך האינטראקטיבי", one.rows(null, Menus.ROW_BOTTOM).isEmpty() && one.rows(Verbs.S_LIST_SCREEN, Menus.ROW_BOTTOM).isEmpty()
                && one.rows(Verbs.S_HOME, Menus.ROW_BOTTOM).size() == 1
                && filesOf(one.rows(Verbs.S_HOME, Menus.ROW_BOTTOM).get(0).items).equals(Arrays.asList(Words.DbCmd.TOOL_EDITOR + ".vbat")));
        ck("תפריטים: \"פקודות\" — הפקודות, \"הפקודות שלי\" והחכמה; \"בחירה\" — ריקה", one.at(Menus.CMDS).size() == 18 && one.has(Menus.MINE) && one.at(Menus.MINE).isEmpty()
                && one.has(Menus.SELECT_PATH) && one.at(Menus.SELECT_PATH).isEmpty() && !one.has(Arrays.asList("אין")));
        ck("תפריטים: הדרכים לבורר — בלי השורש, אב לפני בן", one.paths().size() == 6 && one.paths().indexOf(Menus.CMDS) < one.paths().indexOf(Menus.MINE));
        ck("תפריטים: ייבוא שני של ברירת המחדל — בלי כפולים", Menus.defaults(s, text, IN).files == 0 && Menus.tree(s, Menus.MODE).lists.get(Menus.key(Menus.CMDS)).size() == 18);
        // של האופן: מוסיף, לא מחליף; מה שכבר יש — לא פעמיים
        MemStore w = new MemStore(IdCounter.memory());
        long wh = DbPaths.ensureHome(w), wr = w.createList(Reserved.ROOT, wh), wrow = w.createList("שורה למעלה 1", wr), wc = w.createList(Reserved.CMDS, wr),
                wm = w.createList(Reserved.MINE, wc), wscr = w.createList(Verbs.S_HOME, wr), wsrow = w.createList("שורה למעלה 1", wscr);
        DbFiles.add(w, wrow, Arrays.asList(SYS + "חזרה.vbat", "/x/שלי.vbat"), null);
        DbFiles.add(w, wm, Arrays.asList("/x/מאופן.vbat"), null);
        DbFiles.add(w, wc, Arrays.asList(SYS + "בית.vbat", "/x/עוד.vbat"), null);
        DbFiles.add(w, wsrow, Arrays.asList("/x/בבית.vbat", "/x/שלי.vbat"), null);
        Menus.Snap two = new Menus.Snap(g, Menus.tree(w, "עבודה"));
        ck("תפריטים: של האופן — אחרי הכללי, בלי מה שכבר יש", filesOf(two.at(Menus.rowPath(null, Menus.ROW_TOP, 1)))
                .equals(Arrays.asList("בית.vbat", "פקודות.vbat", "חיפוש.vbat", "קבצים פיזיים.vbat", "חזרה.vbat", "שלי.vbat")));
        ck("תפריטים: \"הפקודות שלי\" — תפריט משנה אחד, ממוזג", two.at(Menus.CMDS).size() == 19 && filesOf(two.at(Menus.MINE)).equals(Arrays.asList("מאופן.vbat")));
        List<Menus.Row> homeTop = two.rows(Verbs.S_HOME, Menus.ROW_TOP);
        ck("תפריטים: שורה של מסך — אחרי הכללית, באותה שורה, בלי כפולים", homeTop.size() == 1 && filesOf(homeTop.get(0).items)
                .equals(Arrays.asList("בית.vbat", "פקודות.vbat", "חיפוש.vbat", "קבצים פיזיים.vbat", "חזרה.vbat", "שלי.vbat", "בבית.vbat")));
        ck("תפריטים: במסך אחר — רק הכללית", filesOf(two.rows(Verbs.S_SEARCH, Menus.ROW_TOP).get(0).items).size() == 6);
        Object[] where = two.where(Menus.MINE);
        ck("תפריטים: הרשימה לסידור — הכללית; ושל האופן — כשרק לו יש", where != null && Menus.MODE.equals(where[0]) && "עבודה".equals(two.where(Arrays.asList(Verbs.S_HOME, "שורה למעלה 1"))[0]));
        Menus.Item mine = two.at(Menus.rowPath(null, Menus.ROW_TOP, 1)).get(5);
        ck("תפריטים: כל שורה יודעת מאיזו רשימה היא", "עבודה".equals(mine.mode) && mine.list == wrow && mine.path.equals(Arrays.asList("שורה למעלה 1")));
        ck("תפריטים: אופן בלי \"שמור לתוכנה\" — רק הכללי", Menus.tree(new MemStore(IdCounter.memory()), "ריק").lists.isEmpty());
        // קישור של רשימה לתוך עצמה — לא נתקעים
        w.addMember(wm, wc == 0 ? wm : w.createList("פנימית", wm));
        ck("תפריטים: עומק — נקרא", Menus.tree(w, "עבודה").ids.containsKey(Menus.key(Arrays.asList(Reserved.CMDS, Reserved.MINE, "פנימית"))));
        // הכלים — גם באופן התפריטים (השורש הוא הבית)
        File t1 = new File(dir, "כלי-חדש.vbat");
        t1.getParentFile().mkdirs();
        write(t1, "הערה\n");
        ck("תפריטים: כלי שחסר נכנס ל\"פקודות\" של אופן התפריטים", DbCmdMenu.ensureTools(s, Menus.MODE, Arrays.asList(t1)).size() == 1
                && s.findFile(t1.getAbsolutePath()) != 0 && DbCmdMenu.ensureTools(s, Menus.MODE, Arrays.asList(t1)).isEmpty());
        // המעבר מ"דוגמאות": מה שהמשתמש הוסיף עובר, והרשימה יוצאת משם עם רישומי הקבצים שלה
        MemStore ex = new MemStore(IdCounter.memory());
        long eh = DbPaths.ensureHome(ex), er = ex.createList(Reserved.ROOT, eh), ec = ex.createList(Reserved.CMDS, er), em = ex.createList(Reserved.MINE, ec),
                eb = ex.createList("גיבוי", er), other = ex.createList("רשימה אחרת", eh);
        DbFiles.add(ex, ec, Arrays.asList(SYS + "בית.vbat", "/x/הוספתי.vbat"), null);
        DbFiles.add(ex, em, Arrays.asList("/x/פקודה שלי.vbat"), null);
        DbFiles.add(ex, eb, Arrays.asList("/x/לגיבוי.txt"), null);
        DbFiles.add(ex, other, Arrays.asList("/x/הוספתי.vbat", "/x/נשאר.pdf"), null);
        long sm = ex.createSmart("חכמה שלי", ec);
        ex.setProp(sm, Store.P_COND, "סוג:vbat שם:א");
        int moved = Menus.moveFrom(ex, DbExamples.MODE, s);
        long cmds = Menus.at(s, home, Menus.CMDS), myList = Menus.at(s, home, Menus.MINE);
        ck("מעבר מהדוגמאות: הרשימות עברו לבית של אופן התפריטים", moved > 0 && filesIn(s, myList) == 1 && s.listsOf(s.findFile("/x/פקודה שלי.vbat")).contains(myList)
                && s.listsOf(s.findFile("/x/הוספתי.vbat")).contains(cmds) && Menus.at(s, home, Arrays.asList("גיבוי")) != 0);
        int same = 0;
        for (Store.Member m : s.members(cmds)) if (m.item.name.equals("בית.vbat")) same++;
        boolean smart = false;
        for (Store.Member m : s.members(cmds)) if (m.item.type == Store.SMART && m.item.name.equals("חכמה שלי")) smart = "סוג:vbat שם:א".equals(s.prop(m.item.id, Store.P_COND));
        ck("מעבר מהדוגמאות: מה שכבר היה — לא פעמיים; רשימה חכמה שנוספה — עם התנאי", same == 1 && smart);
        ck("מעבר מהדוגמאות: שם היא כבר לא קיימת, ורישומי הקבצים שהיו רק בה נמחקו", DbPaths.find(ex, DbExamples.MODE, Reserved.ROOT_PATH) == 0
                && ex.findFile("/x/פקודה שלי.vbat") == 0 && ex.findFile(SYS + "בית.vbat") == 0 && ex.unassigned().isEmpty());
        ck("מעבר מהדוגמאות: מה שהיה גם ברשימה אחרת — נשאר בה", ex.findFile("/x/הוספתי.vbat") != 0 && filesIn(ex, other) == 2 && ex.check() == null);
        ck("מעבר מהדוגמאות: פעם שנייה — אין מה להעביר", Menus.moveFrom(ex, DbExamples.MODE, s) == 0 && s.check() == null);
        // המעבר מקובץ הכפתורים הישן
        final Map<String, File> sys = Menus.systemFiles();
        final File made = new File(dir, "קיצורים");
        Menus.Maker maker = new Menus.Maker() {
            @Override public File file(String icon, String name, String line) {
                File f = sys.get(line.trim());
                return f != null ? new File(SYS + f.getName()) : new File(made, name + ".vbat");
            }
        };
        Buttons.Layout lay = Buttons.parseAll("[למעלה]\n---\n🏠 | בית | פתח מסך הבית\n⭐ | אחרונים | פתח רשימה חכמה אחרונים\n→ | חזרה | חזור\n---\n📂 | הורדות | פתח תיקייה /x\n"
                + "[למעלה: " + Words.Top.S_HOME + "]\n---\n📋 | לימוד | פתח רשימה לימוד\n[למטה: " + Words.Top.S_LIST + "]\n---\n➕ | רשימה | צור רשימה\n"
                + "[למטה: " + Words.Top.S_DISK + "]\n---\n➕ | רשימה | צור רשימה\n⭐ | אחרונים | פתח רשימה חכמה אחרונים\n");
        int nb = Menus.fromButtons(s, lay, maker);
        Menus.Snap after = new Menus.Snap(Menus.tree(s, Menus.MODE), null);
        ck("מעבר מהכפתורים: השורה העליונה — בדיוק מה שהיה בקובץ, לפי הסדר (מה שבא מברירת המחדל יצא)", filesOf(after.at(Menus.rowPath(null, Menus.ROW_TOP, 1)))
                .equals(Arrays.asList("בית.vbat", "אחרונים.vbat", "חזרה.vbat")) && filesOf(after.at(Menus.rowPath(null, Menus.ROW_TOP, 2))).equals(Arrays.asList("הורדות.vbat")));
        ck("מעבר מהכפתורים: שורה של \"מסך הבית\" הישן — בבית וברשימה", filesOf(after.rows(Verbs.S_HOME, Menus.ROW_TOP).get(0).items).contains("לימוד.vbat")
                && filesOf(after.rows(Verbs.S_LIST_SCREEN, Menus.ROW_TOP).get(0).items).contains("לימוד.vbat") && !filesOf(after.rows(Verbs.S_SEARCH, Menus.ROW_TOP).get(0).items).contains("לימוד.vbat"));
        ck("מעבר מהכפתורים: \"פעולה של המסך\" לא עוברת; מסך בלי שם — לא עובר", filesOf(after.rows(Verbs.S_DISK, Menus.ROW_BOTTOM).get(0).items).equals(Arrays.asList("אחרונים.vbat"))
                && after.rows(Verbs.S_SMART_SCREEN, Menus.ROW_BOTTOM).isEmpty() && nb == 7);
        ck("מעבר מהכפתורים: מה שיצא מהשורה נשאר ב\"פקודות\"; המסד תקין", s.findFile(SYS + "חיפוש.vbat") != 0 && s.unassigned().isEmpty() && s.check() == null);
    }

    /** הדוגמאות — הקובץ שבהתקנה: נקרא כולו, מיובא לבית, ושחזור מחליף רק את רשימות הדוגמאות. */
    void examples(Store s, File asset) throws Exception {
        String text = new String(java.nio.file.Files.readAllBytes(asset.toPath()), "UTF-8");
        List<String> tops = DbExamples.tops(text);
        ck("דוגמאות: בקובץ יש רשימות בבית, ובהן \"הדוגמאות שלי\"; \"שמור לתוכנה\" — כבר לא (מ0.41: אופן משלה)", tops.size() >= 10 && tops.contains(DbExamples.MINE) && !tops.contains(Reserved.ROOT)
                && !text.contains(Reserved.ROOT + Names.SEP));
        DbMore.Imported im = DbExamples.install(s, text, "/storage/emulated/0");
        long home = DbPaths.home(s);
        ck("דוגמאות: מיובאות בלי שורות שלא הובנו (" + im.lists + " רשימות, " + im.smarts + " חכמות, " + im.files + " קבצים)",
                im.bad == 0 && im.lists > 50 && im.smarts > 30 && im.files > 5 && s.count(home) == tops.size());   // מ0.41: הפקודות — באופן התפריטים
        boolean raw = false;
        for (Store.Item l : s.lists()) for (Store.Member m : s.members(l.id))
            if (m.item.type == Store.FILE && s.file(m.item.id).path.startsWith("%")) raw = true;
        ck("דוגמאות: \"%פנימי%\" הוחלף בשורש — אין פריט שמתחיל ב-%", !raw);
        ck("דוגמאות: המסד תקין אחרי הייבוא", s.check() == null);
        int lists = s.lists().size(), smarts = s.smarts().size();
        long mine = DbPaths.find(s, "x", Names.parsePath(DbExamples.MINE));
        long userList = s.createList("של המשתמש", home), inMine = s.createList("שמרתי כאן", mine);
        long first = 0;
        for (Store.Member m : s.members(home)) if (m.item.type == Store.LIST && m.item.name.equals(tops.get(0))) first = m.item.id;
        long added = s.createList("הוספתי לדוגמה", first);
        int n = DbExamples.restore(s, text, "/storage/emulated/0");
        ck("שחזור: הוחלפו כל רשימות הדוגמאות חוץ מ\"הדוגמאות שלי\"", n == tops.size() - 1);
        ck("שחזור: רשימה שהמשתמש הוסיף לבית — נשארה", s.item(userList) != null && s.listsOf(userList).contains(home));
        ck("שחזור: \"הדוגמאות שלי\" — אותה רשימה, עם מה שבה", DbPaths.find(s, "x", Names.parsePath(DbExamples.MINE)) == mine && s.listsOf(inMine).contains(mine));
        ck("שחזור: מה שנוסף לרשימת דוגמאות — יצא איתה", s.item(added) == null || s.listsOf(added).isEmpty());
        ck("שחזור: אותו מספר רשימות וחכמות כמו אחרי התקנה (ועוד שתי הרשימות של המשתמש)", s.lists().size() == lists + 2 && s.smarts().size() == smarts);
        ck("שחזור: המסד תקין", s.check() == null);
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

    // ---------- בלי Store ----------

    static void pure(StoreTests.Out o, File dir) throws Exception {
        ImportTests t = new ImportTests(o, "העתקה");
        t.drives();
    }

    private void drives() {
        ck("כונן: פנימי", DbPaths.driveOf("/storage/emulated/0/x/y.txt").equals(Drive.INTERNAL));
        ck("כונן: כרטיס לפי מזהה", DbPaths.driveOf("/storage/ABCD-1234/x.txt").equals("ABCD-1234"));
        ck("כונן: כרטיס בלי תת־תיקייה", DbPaths.driveOf("/storage/ABCD-1234").equals("ABCD-1234"));
        ck("כונן: לא מזוהה", DbPaths.driveOf("/mnt/x").equals(DbPaths.UNKNOWN_DRIVE) && DbPaths.driveOf("/storage/self/p").equals(DbPaths.UNKNOWN_DRIVE));
    }

}
