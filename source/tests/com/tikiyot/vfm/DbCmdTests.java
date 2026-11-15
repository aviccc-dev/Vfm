package com.tikiyot.vfm;

// מבנה 71 (שלב ב, הראשון; המשתמש, 14.11.2026: "לנסות לאפשר בדיקות לכל מצב ושינוי. שיוכלו לעבור דרך הפקודות ולא
// יהיה צורך להתערבות המשתמש"; 15.11: בדיקות בלי התערבות — לפני כל השאר).
// עד כאן פקודות הרשימות (כל מה שכותב למסד: רשימה, אופן, קישור, יצוא, ייבוא…) נבדקו רק במכשיר, דרך תוסף האבחון — במחשב
// לא היה "הקשר" של אנדרואיד, ושער המסד (Db) לא נפתח. עכשיו יש הקשר מדומה (tests/stubs/android/content), המסד — SQLite
// אמיתי של המחשב (הגשר שכבר שימש את בדיקות המנוע), והקבצים שתוסף האבחון מריץ במכשיר — רצים כאן בכל בנייה, כמו שהם.
// מה שעדיין רק במכשיר: המסכים עצמם, הכרטיס, מסך כבוי, וה־SQLite של המכשיר.

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class DbCmdTests {

    static final class Out {
        int passed;
        final List<String> failed = new ArrayList<String>();

        void check(String what, boolean ok) {
            if (ok) passed++;
            else failed.add(what);
        }
    }

    static final String TEST_MODE = "בדיקות מערכת";

    private DbCmdTests() { }

    static Out run(File src) {
        Out o = new Out();
        android.content.Context was = Db.app;
        String wasMode = Db.currentMode;
        try {
            Db.app = (android.content.Context) Class.forName("android.content.Context").newInstance();
            Db.currentMode = null;
            File root = android.os.Environment.getExternalStorageDirectory();
            File here = new File(new File(new File(root, Paths.DB_DIR), Paths.PLUGIN_DATA_DIR), "אבחון");   // כמו במכשיר: תיקיית הנתונים של התוסף
            here.mkdirs();
            // כמו במכשיר: תיקיית ההורדות; ובמכשיר האותיות הגדולות והקטנות בשם תיקייה הן אותו דבר — כאן, קישור
            File down = new File(root, "Download");
            down.mkdirs();
            try {
                java.nio.file.Files.createSymbolicLink(new File(root, "download").toPath(), down.toPath());
            } catch (Throwable t) {
                // כבר קיים, או שאין קישורים במערכת הזו
            }
            // כמו בהפעלה הראשונה אחרי התקנה: אופן הדוגמאות (עם קובצי פקודות המערכת) ואופן התפריטים
            long exHome = Db.prepare(Db.app, DbExamples.MODE, Reserved.FILES);
            Menus.ensure(Db.app);
            o.check("מבנה 71: התקנה טרייה במחשב — אופן הדוגמאות ואופן התפריטים נוצרים, עם בית", exHome != 0
                    && Db.dbModes(Db.app).contains(DbExamples.MODE) && Db.dbModes(Db.app).contains(Db.MENUS_MODE));
            Index ix = new Index("71", "71", root.getAbsolutePath());
            ix.scan(null);
            Library lib = Library.forTests(new ArrayList<Index>(java.util.Collections.singletonList(ix)));
            Map<String, String> vars = Batch.vars(here, root.getAbsolutePath(), null);
            o.check("מבנה 71: עם ההקשר המדומה — הפקודות הן של המסד", DbVerbs.active());

            // א. הקטן ביותר: אופן, רשימה, בדיקה, ביטול
            List<String> rep = go("המשך ()\nצור אופן \"" + TEST_MODE + "\"\nאופן \"" + TEST_MODE + "\"\nבטל ידנית \"בדיקת 71\"\n"
                    + "צור רשימה \"בדיקת 71/א\" /בלי-לפתוח\nודא ידנית \"בדיקת 71\" /רשימות:1 /פריטים:0\nבטל ידנית \"בדיקת 71\"\nודא ידנית \"בדיקת 71\" חסרה\n", here, vars, lib);
            o.check("מבנה 71: אופן חדש, רשימה, \"ודא\", ביטול — במסד אמיתי, במחשב (" + bad(rep) + ")" + (count(rep, "✅") == 3 ? "" : " " + rep), bad(rep).isEmpty() && count(rep, "✅") == 3);   // שתי בדיקות ושורת הסיכום
            o.check("מבנה 71: ...והמסד של האופן — קובץ SQLite בתיקיית המסדים", Db.dbFile(Db.app, TEST_MODE).isFile() && Db.dbModes(Db.app).contains(TEST_MODE));

            // ב. הקבצים שתוסף האבחון מריץ במכשיר — כמו שהם, מאותה תיקייה ובאותם שמות
            String tpl = Files.readUtf8(new File(src.getParentFile(), "lab-pages/avchon.template"));
            Matcher fm = Pattern.compile("'([^']+\\.vbat)':'([a-z0-9-]+)'").matcher(tpl);
            int files = 0;
            while (fm.find()) {
                Files.writeAtomic(new File(here, fm.group(1)), Files.readUtf8(new File(src.getParentFile(), "lab-pages/avchon-tests/" + fm.group(2) + ".vbat")));
                files++;
            }
            ix.scan(null);
            o.check("מבנה 71: קובצי הבדיקה של תוסף האבחון — נכתבו לתיקיית התוסף (" + files + ")", files >= 7);
            // הקבוצות — מההגדרה שבתוסף עצמו (שם, קבצים כלולים, שורות): מה שייתוסף שם — נבדק כאן לבד
            Map<String, String> names = new java.util.HashMap<String, String>();
            names.put("TEST_MODE", TEST_MODE);
            names.put("BK_MODE", "בדיקת המשך מגיבוי");
            names.put("BK_DIR", "%" + Batch.V_INTERNAL + "%\\" + Paths.DB_DIR + "\\" + Backups.DIR + "\\בדיקת המשך מגיבוי");
            int all = 0;
            // מה שלא רץ כאן, ולמה. קבוצה חדשה בתוסף שאינה כאן — רצה; ואם אינה יכולה לרוץ במחשב, הבנייה נעצרת עד שרושמים אותה כאן.
            Map<String, String> onlyDevice = new java.util.LinkedHashMap<String, String>();
            onlyDevice.put("stop", "הרצה נפרדת עם אישור במסך; העצירה עצמה נבדקת בבדיקות המנוע");
            onlyDevice.put("card", "כרטיס זיכרון");
            onlyDevice.put("screen", "מסך כבוי");
            onlyDevice.put("rebuild", "בניית האינדקס של המכשיר, עם זמנים");
            onlyDevice.put("readers", "מהירות הקריאה של המכשיר");
            onlyDevice.put("load", "עומס על הנתונים האמיתיים של המשתמש");
            List<String> keys = new ArrayList<String>();
            Matcher km = Pattern.compile("\\n  \\{k:'([a-z]+)'").matcher(tpl);
            while (km.find()) keys.add(km.group(1));
            o.check("מבנה 71: הקבוצות של תוסף האבחון נקראו מהעמוד שלו (" + keys + ")", keys.size() >= 13 && keys.containsAll(onlyDevice.keySet()));
            int ran = 0;
            for (String k : keys) {
                if (onlyDevice.containsKey(k)) continue;
                Group g = group(tpl, k, names);
                ran++;
                StringBuilder text = new StringBuilder("בדיקה\nהמשך ()\nמדידה /עד:1דקות\nצור אופן \"" + TEST_MODE + "\"\nקטע \"" + g.name + "\"\n");
                for (String f : g.inc) text.append("כלול \"").append(f).append("\"\n");
                for (String l : g.lines) text.append(l).append("\n");
                if (g.matrix) {
                    CmdMatrix.fixtures(here, lib);
                    text.append(CmdMatrix.text(true)).append("\n");
                }
                List<String> r = go(text.toString(), here, vars, lib);
                int ok = verified(r);
                all += Math.max(ok, 0);
                o.check("מבנה 71: \"" + g.name + "\" של תוסף האבחון — במחשב: " + ok + " \"ודא\" עברו, בלי כישלון (" + bad(r) + ")", bad(r).isEmpty() && ok > 0);
            }
            o.check("מבנה 71: בסך הכול — " + ran + " קבוצות ולפחות 135 \"ודא\" של תוסף האבחון עוברים במחשב (" + all + ")", ran >= 7 && all >= 135);
        } catch (Throwable t) {
            t.printStackTrace();
            o.check("מבנה 71: חריגה — " + t, false);
        } finally {
            Db.app = was;
            Db.currentMode = wasMode;
        }
        return o;
    }

    private static List<String> go(String text, File here, Map<String, String> vars, Library lib) {
        Batch.Plan p = Batch.plan(text, here, vars, lib);
        List<String> rep = Batch.run(p, lib, null, null);
        lib.flush(5000);
        return rep;
    }

    /** קבוצה של תוסף האבחון, כמו שהיא מוגדרת בעמוד שלו. */
    static final class Group {
        String name;
        final List<String> inc = new ArrayList<String>(), lines = new ArrayList<String>();
        boolean matrix;
    }

    /** קוראת קבוצה מתוך ההגדרה שבתוסף ({k:'…',n:'…',inc:['…'],lines:['…'+שם+'…', …]}). names — הקבועים שהשורות מרכיבות מהם. */
    static Group group(String tpl, String k, Map<String, String> names) {
        int at = tpl.indexOf("{k:'" + k + "'");
        if (at < 0) return null;
        int end = tpl.indexOf("\n  {k:'", at + 1);
        String body = end < 0 ? tpl.substring(at) : tpl.substring(at, end);
        Group g = new Group();
        Matcher n = Pattern.compile("[,{]n:'([^']*)'").matcher(body);
        g.name = n.find() ? n.group(1) : k;
        g.matrix = body.contains("gen:'matrix'");
        for (String x : array(body, "inc:[", names)) g.inc.add(x);
        for (String x : array(body, "lines:[", names)) g.lines.add(x);
        return g;
    }

    /** מערך של מחרוזות בכתיב של העמוד: 'א'+שם+'ב','ג' — עד הסוגר שסוגר אותו. */
    private static List<String> array(String body, String open, Map<String, String> names) {
        List<String> out = new ArrayList<String>();
        int i = body.indexOf(open);
        if (i < 0) return out;
        i += open.length();
        StringBuilder cur = new StringBuilder();
        boolean any = false;
        while (i < body.length()) {
            char c = body.charAt(i);
            if (c == '\'') {
                int j = i + 1;
                StringBuilder str = new StringBuilder();
                while (j < body.length() && body.charAt(j) != '\'') {
                    if (body.charAt(j) == '\\' && j + 1 < body.length()) j++;
                    str.append(body.charAt(j++));
                }
                cur.append(str);
                any = true;
                i = j + 1;
            } else if (Character.isLetter(c) || c == '_') {
                int j = i;
                while (j < body.length() && (Character.isLetterOrDigit(body.charAt(j)) || body.charAt(j) == '_')) j++;
                String id = body.substring(i, j);
                if (!names.containsKey(id)) throw new IllegalStateException("קבוע לא מוכר בשורות של התוסף: " + id);
                cur.append(names.get(id));
                any = true;
                i = j;
            } else if (c == ',' || c == ']') {
                if (any) out.add(cur.toString());
                cur.setLength(0);
                any = false;
                i++;
                if (c == ']') break;
            } else i++;
        }
        return out;
    }

    /** כמה "ודא" עברו לפי שורת הסיכום; ‎-1 — אין שורת סיכום, או שיש כישלון. */
    static int verified(List<String> rep) {
        Matcher m = Pattern.compile("✅ ודא: (\\d+) עברו · 0 נכשלו").matcher(rep.toString());
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }

    /** השורות הרעות שבדוח. */
    static String bad(List<String> rep) {
        StringBuilder b = new StringBuilder();
        for (String x : rep) if (x.contains("❌") || x.contains("⛔") || x.contains("⚠")) b.append(x).append(" | ");
        return b.toString();
    }

    static int count(List<String> rep, String mark) {
        int n = 0;
        for (String x : rep) if (x.contains(mark)) n++;
        return n;
    }
}
