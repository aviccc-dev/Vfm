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
            // מבנה 85: "הסרה מכאן" של מסך הרשימות — דרך המנהל, כפקודה "בטל ידנית %נבחרים%", על מסד אמיתי
            {
                final String m85 = "בדיקת-85";
                List<String> r85 = go("המשך ()\nצור אופן \"" + m85 + "\"\nאופן \"" + m85 + "\"\nMD ידנית \"ראש\\א\"\nMD ידנית \"ראש\\ב\"\n"
                        + "MD ידנית \"ראש\\ג\"\nודא ידנית \"ראש\" /רשימות:3\n", here, vars, lib);
                final long[] id85 = new long[4];
                Db.read(Db.app, m85, new Db.Use<Void>() {
                    @Override public Void run(SqlStore s) {
                        id85[0] = DbPaths.find(s, m85, java.util.Arrays.asList("ראש"));
                        id85[1] = DbPaths.find(s, m85, java.util.Arrays.asList("ראש", "א"));
                        id85[2] = DbPaths.find(s, m85, java.util.Arrays.asList("ראש", "ב"));
                        id85[3] = DbPaths.find(s, m85, java.util.Arrays.asList("ראש", "ג"));
                        return null;
                    }
                });
                o.check("מבנה 85: ההכנה — רשימה ובה שלוש (" + bad(r85) + ")", bad(r85).isEmpty() && id85[0] != 0 && id85[1] != 0 && id85[2] != 0 && id85[3] != 0);
                List<Long> two85 = java.util.Arrays.asList(id85[1], id85[2]);
                Manager.Act act85 = Manager.onListRemove(m85, id85[0], two85, here, vars, lib);
                o.check("מבנה 85: הפקודה שהמסך מריץ — \"בטל ידנית %נבחרים%\", בלי סירוב (" + act85.error() + ")", act85.error() == null
                        && act85.text.trim().equals("בטל ידנית %נבחרים%") && act85.plan.steps.size() == 1);
                o.check("מבנה 85: בחירה ריקה, אופן שאין לו מסד, ופקודת רשימות אחרת עם %נבחרים% — סירוב מהתכנון",
                        Manager.onListRemove(m85, id85[0], new ArrayList<Long>(), here, vars, lib).error() != null
                                && Manager.onListRemove("אין-אופן-כזה-85", id85[0], two85, here, vars, lib).error() != null
                                && !Db.dbFile(Db.app, "אין-אופן-כזה-85").exists()
                                && Batch.plan("העתק ידנית %נבחרים% ידנית \"ראש\"\n", here, vars, lib, null, new Batch.Selection(m85, id85[0], two85)).errors == 1
                                && Batch.plan("בטל ידנית %נבחרים%\n", here, vars, lib).errors == 1);
                String said85;
                Permit.close();                                                 // כמו בתוכנה: אין היתר, אלא אם המנהל פותח
                try {
                    said85 = act85.say(lib);
                    o.check("מבנה 85: ההיתר נסגר אחרי ההרצה", !Permit.held());
                } finally {
                    Permit.open();
                }
                final long[] after85 = new long[3];
                final int[] backups85 = {0};
                Db.read(Db.app, m85, new Db.Use<Void>() {
                    @Override public Void run(SqlStore s) {
                        after85[0] = DbPaths.find(s, m85, java.util.Arrays.asList("ראש", "א"));
                        after85[1] = DbPaths.find(s, m85, java.util.Arrays.asList("ראש", "ב"));
                        after85[2] = DbPaths.find(s, m85, java.util.Arrays.asList("ראש", "ג"));
                        return null;
                    }
                });
                File[] bk85 = Db.backupsDir(m85).listFiles();
                if (bk85 != null) for (File f : bk85) if (f.getName().contains(Backups.OP_DELETE)) backups85[0]++;
                o.check("מבנה 85: דרך המנהל — שתי הרשימות שנבחרו יצאו, השלישית נשארה; ההודעה — כמו שהמסך אמר (" + said85 + ")",
                        after85[0] == 0 && after85[1] == 0 && after85[2] == id85[3]
                                && said85.equals(Words.DbCmd.inDb(Words.DbCmd.removed(Words.DbCmd.items(2)))));
                o.check("מבנה 85: ...ולפני ההסרה נעשה גיבוי של המסד (" + backups85[0] + ")", backups85[0] >= 1);
                String scr85 = Files.readUtf8(new File(src, "src/com/tikiyot/vfm/masachim/DbListScreen.java"));
                o.check("מבנה 85: מסך הרשימות אינו מסיר עוד בעצמו; רק דרך המנהל", !scr85.contains("Lists.removeFrom(") && scr85.contains("Manager.onListRemove("));
                go("המשך ()\nבטל אופן \"" + m85 + "\"\n", here, vars, lib);
            }
            // מבנה 86: שינוי שם של רשימה שנמצאת בשתי רשימות — השם נבדק בשתיהן (כמו בעריכה שבמסך)
            {
                String m86 = "בדיקת-86";
                List<String> r86 = go("המשך ()\nצור אופן \"" + m86 + "\"\nאופן \"" + m86 + "\"\nMD ידנית \"ראש/א/משותפת\"\nMD ידנית \"ראש/ב/תפוסה\"\n"
                        + "קשר ידנית \"ראש/א/משותפת\" ידנית \"ראש/ב\"\nודא ידנית \"ראש/ב\" /רשימות:2\n"
                        + "ודא שגיאה\nREN ידנית \"ראש/א/משותפת\" \"תפוסה\"\n"
                        + "ודא ידנית \"ראש/א/משותפת\" קיימת\nודא ידנית \"ראש/ב/משותפת\" קיימת\n"
                        + "REN ידנית \"ראש/א/משותפת\" \"פנויה\"\nודא ידנית \"ראש/ב/פנויה\" קיימת\n", here, vars, lib);
                o.check("מבנה 86: רשימה שמשותפת לשתי רשימות — שם שתפוס באחת מהן נדחה, גם כשהדרך שנכתבה היא דרך השנייה; שם פנוי — משתנה בשתיהן ("
                        + bad(r86) + ")" + (count(r86, "✅") == 6 ? "" : " " + r86), bad(r86).isEmpty() && count(r86, "✅") == 6);   // חמש שורות ושורת הסיכום
                go("המשך ()\nבטל אופן \"" + m86 + "\"\n", here, vars, lib);
            }
            // מבנה 87: "ודא ידנית א זהה ידנית ב" — תמונת מצב של רשימה, והשוואה עד הסוף
            {
                final String m87 = "בדיקת-87";
                String dir87 = "%" + Batch.V_INTERNAL + "%\\בדיקת 87 (אפשר למחוק)";
                StringBuilder t87 = new StringBuilder("המשך ()\nצור אופן \"" + m87 + "\"\nאופן \"" + m87 + "\"\nMD \"" + dir87 + "\"\n"
                        + "כתוב \"" + dir87 + "\\ק1.txt\" \"אחת\"\nכתוב \"" + dir87 + "\\ק2.txt\" \"שתיים\"\nכתוב \"" + dir87 + "\\ק3.txt\" \"שלוש\"\n");
                // אותו מבנה, פעמיים (א, ב); ג — אותו תוכן בסדר אחר; ד — עותק במקום קישור; ה — קובץ נוסף בעומק
                for (String r87 : new String[]{"א", "ב", "ג", "ד", "ה"}) {
                    t87.append("MD ידנית \"").append(r87).append("/ספרים/עמוק/עמוק יותר\"\n");
                    t87.append("MD ידנית \"").append(r87).append("/ריקה\"\n");
                    if (r87.equals("ג")) t87.append("צור כותרת \"כותרת\" \"").append(r87).append("\"\n");   // לפני הקובץ, ולא אחריו
                    t87.append("העתק \"").append(dir87).append("\\ק1.txt\" ידנית \"").append(r87).append("/ספרים\"\n");
                    t87.append("העתק \"").append(dir87).append("\\ק2.txt\" ידנית \"").append(r87).append("/ספרים/עמוק\"\n");
                    t87.append("העתק \"").append(dir87).append("\\ק1.txt\" ידנית \"").append(r87).append("\"\n");          // אותו קובץ בשתי רשימות
                    t87.append("צור רעיון \"רעיון ראשון\" \"").append(r87).append("/ספרים\"\n");
                    if (!r87.equals("ג")) t87.append("צור כותרת \"כותרת\" \"").append(r87).append("\"\n");
                    if (r87.equals("ה")) t87.append("העתק \"").append(dir87).append("\\ק3.txt\" ידנית \"").append(r87).append("/ספרים/עמוק/עמוק יותר\"\n");
                    t87.append(r87.equals("ד") ? "העתק" : "קשר").append(" ידנית \"").append(r87).append("/ספרים/עמוק\" ידנית \"").append(r87).append("/ריקה\"\n");
                }
                t87.append("ודא תוכן ידנית \"א\" שווה תוכן ידנית \"ב\"\nודא ידנית \"א\" זהה ידנית \"א\"\nודא ידנית * זהה ידנית *\n"
                        + "ודא ידנית \"א/ספרים\" שווה ידנית \"ב/ספרים\"\n"
                        + "ודא ידנית \"א/ספרים/עמוק\" זהה ידנית \"א/ריקה/עמוק\"\n"       // קישור: אותה רשימה, בשתי דרכים
                        + "ודא ידנית \"ד/ספרים/עמוק\" שווה ידנית \"ד/ריקה/עמוק\"\n");     // עותק: רשימה אחרת, שווה
                List<String> r87 = go(t87.toString(), here, vars, lib);
                o.check("מבנה 87–88: שתי רשימות שנבנו באותן פקודות — התוכן שווה; רשימה זהה לעצמה, והבית לעצמו; תת־רשימות באותו שם ובאותו תוכן — שוות;"
                        + " רשימה מקושרת — זהה משתי הדרכים; עותק — שווה (" + bad(r87) + ")"
                        + (count(r87, "✅") == 7 ? "" : " " + r87), bad(r87).isEmpty() && count(r87, "✅") == 7);
                // ההגדרה של המשתמש: שם שווה ותוכן שווה במספרים שונים — שוות ולא זהות; פרט אחד שונה — דומות ולא שוות
                List<String> q87 = go("המשך ()\nאופן \"" + m87 + "\"\n"
                        + "ודא ידנית \"א/ספרים/עמוק\" שווה ידנית \"א/ריקה/עמוק\"\n"       // זהות — ולכן לא "שוות"
                        + "ודא ידנית \"ד/ספרים/עמוק\" זהה ידנית \"ד/ריקה/עמוק\"\n"        // שוות — ולכן לא "זהות"
                        + "ודא ידנית \"א\" שווה ידנית \"ב\"\n"                              // אותו תוכן, שם אחר — דומות
                        + "ודא ידנית \"א/ספרים\" שווה ידנית \"ה/ספרים\"\n", here, vars, lib);   // אותו שם, קובץ נוסף בעומק — דומות
                String q87s = String.valueOf(q87);
                o.check("מבנה 88: \"שווה\" על אותה רשימה — ❌ \"זהות\"; \"זהה\" על עותק — ❌ \"שוות\"; שם אחר או פרט אחד שונה — ❌ \"דומות, לא שוות\" ("
                        + count(q87, "❌") + ") " + (count(q87, "❌") == 5 ? "" : q87s), count(q87, "❌") == 5 && q87s.contains(Words.DbCmd.REL_SAME)
                        && q87s.contains(Words.DbCmd.REL_EQUAL) && q87s.contains(Words.DbCmd.relNames("א", "ב") + " · " + Words.DbCmd.REL_CONTENT_EQUAL
                        + Words.DbCmd.REL_SIMILAR) && count(q87, Words.DbCmd.REL_SIMILAR) == 2);
                List<String> d87 = go("המשך ()\nאופן \"" + m87 + "\"\nודא תוכן ידנית \"א\" שווה תוכן ידנית \"ג\"\nודא תוכן ידנית \"א\" שווה תוכן ידנית \"ד\"\n"
                        + "ודא תוכן ידנית \"א\" שווה תוכן ידנית \"ה\"\nודא ידנית \"א\" שווה ידנית \"אין כזו\"\n"
                        + "ודא תוכן ידנית \"א\" שווה תוכן ידנית \"א/ספרים\"\n", here, vars, lib);
                String all87 = String.valueOf(d87);
                o.check("מבנה 87: סדר אחר, עותק במקום קישור, קובץ נוסף בעומק, רשימה שאיננה, ורשימה מול תת־רשימה שלה — חמש ❌, כל אחת אומרת איפה ("
                        + count(d87, "❌") + ")" + (count(d87, "❌") == 6 ? "" : " " + d87), count(d87, "❌") == 6 && count(d87, "שונות מהשורה ה־") == 4
                        && all87.contains(Words.ListCmdW.noList("אין כזו")));
                o.check("מבנה 87: ...עותק במקום קישור נתפס בדיוק במקום: אצל א — \"= L\" (אותה רשימה שוב), אצל ד — רשימה חדשה",
                        java.util.regex.Pattern.compile("«= L\\d+» מול «L\\d+ \"עמוק\"»").matcher(all87).find());
                // התמונה עצמה: בלי מספרי המסד, עם אותו קובץ בשני מקומות, ועם מאפיין שמשנה אותה
                final List<List<String>> snap87 = new ArrayList<List<String>>();
                final long[] root87 = {0};
                Db.read(Db.app, m87, new Db.Use<Void>() {
                    @Override public Void run(SqlStore s) {
                        root87[0] = DbPaths.find(s, m87, java.util.Arrays.asList("א"));
                        snap87.add(DbSnap.of(s, root87[0]));
                        snap87.add(DbSnap.of(s, DbPaths.find(s, m87, java.util.Arrays.asList("ב"))));
                        return null;
                    }
                });
                String s87 = String.valueOf(snap87.get(0));
                List<String> a87 = snap87.get(0);
                o.check("מבנה 87: התמונה — שורה לכל פריט, לפי הסדר והעומק; הקובץ שבשתי רשימות מופיע פעם אחת במסלול שלו ופעם כ\"= F2\", והרשימה"
                        + " המשותפת — כ\"= L2\"; בלי מספרי המסד " + a87,
                        a87.size() == 11 && a87.get(0).equals("@") && a87.get(1).equals("  L1 \"ספרים\"") && a87.get(2).equals("    L2 \"עמוק\"")
                                && a87.get(3).equals("      L3 \"עמוק יותר\"") && a87.get(4).startsWith("      F1 /") && a87.get(4).endsWith("ק2.txt | 10")
                                && a87.get(5).startsWith("    F2 /") && a87.get(5).endsWith("ק1.txt | 6") && a87.get(6).equals("    N1 \"רעיון ראשון\"")
                                && a87.get(7).equals("  L4 \"ריקה\"") && a87.get(8).equals("    = L2") && a87.get(9).equals("  = F2")
                                && a87.get(10).equals("  T1 \"כותרת\"") && DbSnap.diff(a87, snap87.get(1)) == null && !s87.contains(String.valueOf(root87[0]) + " "));
                Db.write(Db.app, m87, null, new Db.Use<Void>() {
                    @Override public Void run(SqlStore s) {
                        s.setProp(DbPaths.find(s, m87, java.util.Arrays.asList("ב", "ספרים", "עמוק")), Store.P_ICON, "📚");
                        return null;
                    }
                });
                List<String> p87 = go("המשך ()\nאופן \"" + m87 + "\"\nודא תוכן ידנית \"א\" שווה תוכן ידנית \"ב\"\n", here, vars, lib);
                o.check("מבנה 87: צלמית שונה לרשימה אחת בעומק — כבר לא זהות, והשורה אומרת מה (" + bad(p87) + ")", count(p87, "❌") == 2
                        && String.valueOf(p87).contains("{icon=📚}"));
                o.check("מבנה 87: צורה שגויה — שגיאה כבר בתכנון", Batch.plan("אופן \"" + m87 + "\"\nודא ידנית \"א\" זהה \"ב\"\n", here, vars, lib).errors == 1
                        && Batch.plan("אופן \"" + m87 + "\"\nודא ידנית \"א\" זהה חכמה \"ב\"\n", here, vars, lib).errors == 1
                        && Batch.plan("אופן \"" + m87 + "\"\nודא ידנית \"א\" זהה ידנית \"ב\" /רשימות:1\n", here, vars, lib).errors == 1
                        && Batch.plan("אופן \"" + m87 + "\"\nודא תוכן ידנית \"א\" שווה ידנית \"ב\"\n", here, vars, lib).errors == 1
                        && Batch.plan("אופן \"" + m87 + "\"\nודא תוכן ידנית \"א\" זהה תוכן ידנית \"ב\"\n", here, vars, lib).errors == 1
                        && Batch.plan("אופן \"" + m87 + "\"\nודא ידנית \"א\" שווה \"ב\"\n", here, vars, lib).errors == 1);
                // יצוא לקובץ טקסט וייבוא ממנו — הרשימה שחוזרת זהה למקור, כולל הרשימה המשותפת והקובץ שבשתי רשימות.
                // (נבדק מול "א" עצמה; ל"ב" שונתה צלמית למעלה.)
                List<String> x87 = go("המשך ()\nאופן \"" + m87 + "\"\nCOPY ידנית \"א\" קובץ \"" + dir87 + "\\יצוא-87.txt\"\nMD ידנית \"מיובא\"\n"
                        + "COPY קובץ \"" + dir87 + "\\יצוא-87.txt\" ידנית \"מיובא\"\nודא ידנית \"א\" שווה ידנית \"מיובא/א\"\n", here, vars, lib);
                o.check("מבנה 87–88: יצוא לקובץ וייבוא ממנו — הרשימה שחוזרת שווה למקור עד הסוף: רשימה אחרת, אותו שם ואותו תוכן (" + bad(x87) + ")" + (count(x87, "✅") == 2 ? "" : " " + x87),
                        bad(x87).isEmpty() && count(x87, "✅") == 2);
                go("המשך ()\nמחק תיקייה \"" + dir87 + "\" /S\nבטל אופן \"" + m87 + "\"\n", here, vars, lib);
            }
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
