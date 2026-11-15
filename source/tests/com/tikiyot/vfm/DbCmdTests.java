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
            // מבנה 90: "השווה ידנית א ידנית ב /הבדלים" — כל ההבדלים בין שתי רשימות
            {
                final String m90 = "בדיקת-90";
                String dir90 = "%" + Batch.V_INTERNAL + "%\\בדיקת 90 (אפשר למחוק)";
                StringBuilder t90 = new StringBuilder("המשך ()\nצור אופן \"" + m90 + "\"\nאופן \"" + m90 + "\"\nMD \"" + dir90 + "\"\n"
                        + "כתוב \"" + dir90 + "\\ק1.txt\" \"אחת\"\nכתוב \"" + dir90 + "\\ק2.txt\" \"שתיים\"\nכתוב \"" + dir90 + "\\ק3.txt\" \"שלוש\"\n");
                // א, ב — אותו מבנה. כל אחת מהאחרות שונה מ"א" בדבר אחד בלבד:
                // ג — הכותרת לפני הקובץ · ד — עותק במקום קישור · ה — קובץ נוסף בעומק · ו — "ספרים" בשם אחר · ז — בלי "ריקה"
                final String[] roots90 = {"א", "ב", "ג", "ד", "ה", "ו", "ז"};
                for (String r : roots90) {
                    String bk = r.equals("ו") ? "ספרים חדשים" : "ספרים";
                    t90.append("MD ידנית \"").append(r).append("/").append(bk).append("/עמוק/עמוק יותר\"\n");
                    if (!r.equals("ז")) t90.append("MD ידנית \"").append(r).append("/ריקה\"\n");
                    if (r.equals("ג")) t90.append("צור כותרת \"כותרת\" \"").append(r).append("\"\n");
                    t90.append("העתק \"").append(dir90).append("\\ק1.txt\" ידנית \"").append(r).append("/").append(bk).append("\"\n");
                    t90.append("העתק \"").append(dir90).append("\\ק2.txt\" ידנית \"").append(r).append("/").append(bk).append("/עמוק\"\n");
                    t90.append("העתק \"").append(dir90).append("\\ק1.txt\" ידנית \"").append(r).append("\"\n");
                    t90.append("צור רעיון \"רעיון ראשון\" \"").append(r).append("/").append(bk).append("\"\n");
                    if (!r.equals("ג")) t90.append("צור כותרת \"כותרת\" \"").append(r).append("\"\n");
                    if (r.equals("ה")) t90.append("העתק \"").append(dir90).append("\\ק3.txt\" ידנית \"").append(r).append("/").append(bk).append("/עמוק/עמוק יותר\"\n");
                    if (!r.equals("ז")) t90.append(r.equals("ד") ? "העתק" : "קשר").append(" ידנית \"").append(r).append("/").append(bk)
                            .append("/עמוק\" ידנית \"").append(r).append("/ריקה\"\n");
                }
                for (String r : roots90) t90.append("השווה תוכן ידנית \"א\" תוכן ידנית \"").append(r).append("\" /הבדלים\n");
                t90.append("השווה ידנית \"א\" ידנית \"ב\" /הבדלים\nהשווה ידנית \"א/ספרים/עמוק\" ידנית \"א/ריקה/עמוק\" /הבדלים\n");
                List<String> r90 = go(t90.toString(), here, vars, lib);
                String a90 = "";
                for (String l : r90) a90 += l + "\n";
                o.check("מבנה 90: ההכנה והדוחות רצו בלי כישלון (" + bad(r90) + ")", bad(r90).isEmpty());
                o.check("מבנה 90: רשימה מול עצמה — \"אין הבדלים\"; מול רשימה שנבנתה באותן פקודות — \"אין הבדלים … התוכן שווה\"",
                        a90.contains("אין הבדלים בין \"א\" ל־\"א\" — " + Words.DbCmd.REL_SAME)
                                && a90.contains("אין הבדלים בין \"א\" ל־\"ב\" — " + Words.DbCmd.REL_CONTENT_EQUAL));
                o.check("מבנה 90: הכותרת לפני הקובץ — הבדל אחד: \"מקום אחר\" " + pick(r90, "\"ג\""),
                        a90.contains("הבדלים בין \"א\" ל־\"ג\": 1 — מקום אחר: 1") && a90.contains("   · מקום אחר: "));
                o.check("מבנה 90: עותק במקום קישור — הבדל אחד: \"קישור שונה\", במקום המדויק, ובלי לחזור עליו לכל רשימה שבפנים " + pick(r90, "\"ד\""),
                        a90.contains("הבדלים בין \"א\" ל־\"ד\": 1 — קישורים: 1")
                                && a90.contains("   · " + Words.DbCmd.diffLink("ריקה" + Names.SEP + "עמוק", "\"א\"", "ספרים" + Names.SEP + "עמוק", "\"ד\"")));
                o.check("מבנה 90: קובץ נוסף בעומק — הבדל אחד: \"רק ב־ה\", עם הדרך המלאה; ופעם אחת, אף שהרשימה שלו נמצאת בשני מקומות " + pick(r90, "\"ה\""),
                        a90.contains("הבדלים בין \"א\" ל־\"ה\": 1 — רק ב־\"ה\": 1") && a90.contains("   · רק ב־\"ה\": ספרים" + Names.SEP + "עמוק" + Names.SEP + "עמוק יותר" + Names.SEP + "ק3.txt (קובץ · /")
                                && a90.contains("/ק3.txt)"));
                o.check("מבנה 90: רשימה באותו תוכן ובשם אחר — הבדל אחד: \"שם אחר\" (ולא \"נעלמה אחת ונוספה אחרת\") " + pick(r90, "\"ו\""),
                        a90.contains("הבדלים בין \"א\" ל־\"ו\": 1 — שם אחר: 1")
                                && a90.contains("   · " + Words.DbCmd.diffRenamed("\"א\"", "ספרים", "\"ו\"", "ספרים חדשים")));
                o.check("מבנה 90: רשימה שחסרה בצד אחד — הבדל אחד: \"רק ב־א\", עם מה שבתוכה " + pick(r90, "\"ז\""),
                        a90.contains("הבדלים בין \"א\" ל־\"ז\": 1 — רק ב־\"א\": 1")
                                && a90.contains("   · " + Words.DbCmd.diffOnly("\"א\"", "ריקה", "רשימה" + Words.DbCmd.diffInside(2, 1))));
                o.check("מבנה 90: בלי \"תוכן\" — גם השם של הרשימה עצמה נבדק; ושתי דרכים לאותה רשימה — \"זהות\"",
                        a90.contains("   · " + Words.DbCmd.diffRenamed("\"א\"", "א", "\"ב\"", "ב"))
                                && a90.contains("אין הבדלים בין \"א/ספרים/עמוק\" ל־\"א/ריקה/עמוק\" — " + Words.DbCmd.REL_SAME));
                // הכלל ששומר על הכלי: הדוח ריק אם ורק אם התוכן שווה לפי תמונת המצב — בכל זוג מהשבע, לשני הכיוונים; ואין "הבדל שלא סווג"
                final int[] agree90 = {0, 0, 0};
                Db.write(Db.app, m90, null, new Db.Use<Void>() {
                    @Override public Void run(SqlStore st) {
                        st.setProp(DbPaths.find(st, m90, java.util.Arrays.asList("ב", "ספרים", "עמוק")), Store.P_ICON, "📚");
                        return null;
                    }
                });
                Db.read(Db.app, m90, new Db.Use<Void>() {
                    @Override public Void run(SqlStore st) {
                        for (String x : roots90) for (String y : roots90) {
                            long ix = DbPaths.find(st, m90, java.util.Arrays.asList(x)), iy = DbPaths.find(st, m90, java.util.Arrays.asList(y));
                            List<String> sx = DbSnap.of(st, ix), sy = DbSnap.of(st, iy);
                            boolean equal = DbSnap.diff(sx.subList(1, sx.size()), sy.subList(1, sy.size())) == null;
                            DbDiff.Out d = DbDiff.of(st, ix, iy, x, y, false);
                            agree90[0]++;
                            if (equal == (d.total() == 0)) agree90[1]++;
                            agree90[2] += d.other;
                        }
                        return null;
                    }
                });
                o.check("מבנה 90: בכל 49 הזוגות — הדוח ריק בדיוק כשהתוכן שווה (" + agree90[1] + " מ־" + agree90[0] + "), ואין הבדל שלא סווג (" + agree90[2] + ")",
                        agree90[0] == 49 && agree90[1] == 49 && agree90[2] == 0);
                List<String> p90 = go("המשך ()\nאופן \"" + m90 + "\"\nהשווה תוכן ידנית \"א\" תוכן ידנית \"ב\" /הבדלים\n"
                        + "השווה ידנית \"א\" ידנית \"אין כזו\" /הבדלים\n", here, vars, lib);
                String p90s = String.valueOf(p90);
                o.check("מבנה 90: צלמית שונה לרשימה בעומק — \"מאפיין שונה\" עם שני הערכים; רשימה שאיננה — כישלון שנאמר " + p90s,
                        p90s.contains("מאפיינים: 1") && p90s.contains(Words.DbCmd.diffProp("ספרים" + Names.SEP + "עמוק", "צלמית", "", "📚"))
                                && p90s.contains(Words.ListCmdW.noList("אין כזו")) && p90s.contains("כישלונות בהרצה: 1"));
                o.check("מבנה 90: צורה שגויה — שגיאה כבר בתכנון",
                        Batch.plan("אופן \"" + m90 + "\"\nהשווה תוכן ידנית \"א\" ידנית \"ב\" /הבדלים\n", here, vars, lib).errors == 1
                                && Batch.plan("אופן \"" + m90 + "\"\nהשווה חכמה \"א\" חכמה \"ב\" /הבדלים\n", here, vars, lib).errors == 1
                                && Batch.plan("אופן \"" + m90 + "\"\nהעתק ידנית \"א\" ידנית \"ב\" /הבדלים\n", here, vars, lib).errors == 1);
                boolean[] k90 = DbDiff.keptOrder(new int[]{2, 0, 1}), k90b = DbDiff.keptOrder(new int[]{0, -1, 1, 2});
                o.check("מבנה 90: \"מקום אחר\" — רק מי שיצא מהסדר: מתוך ג־א־ב זז אחד; ומי שאין לו בן זוג אינו נספר",
                        !k90[0] && k90[1] && k90[2] && k90b[0] && k90b[1] && k90b[2] && k90b[3]);
                go("המשך ()\nמחק תיקייה \"" + dir90 + "\" /S\nבטל אופן \"" + m90 + "\"\n", here, vars, lib);
            }
            // מבנה 91: "רשימה חדשה" של המסך (➕, הבורר) — דרך המנהל, כפקודה; מול הדרך הישנה, על המצבים של המפה (ב1, ב2, ב4, ג2–ג4, א5)
            {
                final String m91 = "בדיקת-91";
                // "ישן" ו"חדש" — אותו מבנה: רשימה "בפנים", רשימה חכמה "תפוס־חכמה", ו"משותפת" שנמצאת גם ב"מקום שני"
                StringBuilder t91 = new StringBuilder("המשך ()\nצור אופן \"" + m91 + "\"\nאופן \"" + m91 + "\"\n");
                for (String r : new String[]{"ישן", "חדש"})
                    t91.append("MD ידנית \"").append(r).append("/בפנים/עמוק\"\nMD ידנית \"").append(r).append("/משותפת\"\nMD ידנית \"").append(r)
                            .append("/מקום שני\"\nקשר ידנית \"").append(r).append("/משותפת\" ידנית \"").append(r).append("/מקום שני\"\n");
                List<String> r91 = go(t91.toString(), here, vars, lib);
                o.check("מבנה 91: ההכנה (" + bad(r91) + ")", bad(r91).isEmpty());
                final String[][] cases91 = {                                    // {בתוך מה (דרך), השם שהוקלד}
                        {"", "רגילה"}, {"בפנים", "בעומק שתיים"}, {"בפנים/עמוק", "בעומק שלוש"}, {"משותפת", "בתוך משותפת"},
                        {"", "בפנים"}, {"", "  רווחים בקצוות  "}, {"", "100% %תאריך% \"מירכאות\" *"}, {"", "א/ב"}, {"", "א\\ב"}, {"", "   "},
                        {"בפנים", "עמוק"}, {"", "רגילה"}};
                final String[] old91 = new String[cases91.length], new91 = new String[cases91.length];
                final long[] newId91 = new long[cases91.length];
                final long[] home91 = {0};
                for (int i = 0; i < cases91.length; i++) {
                    final int k = i;
                    final List<String> po = new ArrayList<String>(), pn = new ArrayList<String>();
                    po.add("ישן");
                    pn.add("חדש");
                    for (String x : cases91[i][0].split("/")) if (!x.isEmpty()) { po.add(x); pn.add(x); }
                    final long[] in = {0, 0};
                    Db.write(Db.app, m91, null, new Db.Use<Void>() {            // הדרך הישנה: מה שהמסך עשה בעצמו
                        @Override public Void run(SqlStore st) {
                            in[0] = DbPaths.find(st, m91, po);
                            in[1] = DbPaths.find(st, m91, pn);
                            home91[0] = DbPaths.home(st);
                            Lists.Made m = Lists.newListChecked(st, in[0], cases91[k][1]);
                            old91[k] = m.error == null ? "נוצרה" : m.error;
                            return null;
                        }
                    });
                    Manager.Act act = Manager.onListNew(m91, in[1], cases91[i][1], here, vars, lib);
                    if (act.error() != null) new91[i] = "תכנון: " + act.error();
                    else {
                        Permit.close();
                        try {
                            String said = act.say(lib);
                            new91[i] = act.failed ? said : "נוצרה";
                            newId91[i] = act.made;
                            if (Permit.held()) new91[i] += " (ההיתר נשאר פתוח)";
                        } finally {
                            Permit.open();
                        }
                    }
                }
                boolean same91 = true;
                StringBuilder d91 = new StringBuilder();
                for (int i = 0; i < cases91.length; i++)
                    if (!old91[i].equals(new91[i])) {
                        same91 = false;
                        d91.append(" [").append(cases91[i][1]).append(": ישן «").append(old91[i]).append("» חדש «").append(new91[i]).append("»]");
                    }
                o.check("מבנה 91: בכל 12 המצבים — הפקודה עונה כמו שהמסך ענה (נוצרה, או אותו סירוב באותו נוסח)" + d91, same91);
                o.check("מבנה 91: מה נוצר ומה סורב — רגילה, בעומק, בתוך רשימה משותפת, רווחים, תווים מיוחדים: נוצרו; שם תפוס, \"/\", \"\\\": סורבו "
                                + java.util.Arrays.toString(new91),
                        new91[0].equals("נוצרה") && new91[1].equals("נוצרה") && new91[2].equals("נוצרה") && new91[3].equals("נוצרה")
                                && new91[4].equals(Words.DbCmd.nameTaken("בפנים")) && new91[5].equals("נוצרה") && new91[6].equals("נוצרה")
                                && new91[7].equals(Words.DbCmd.BAD_NAME) && new91[8].equals(Words.DbCmd.BAD_NAME) && new91[9].equals(Words.DbCmd.BAD_NAME)
                                && new91[10].equals(Words.DbCmd.nameTaken("עמוק")) && new91[11].equals(Words.DbCmd.nameTaken("רגילה"))
                                && newId91[0] != 0 && newId91[4] == 0 && newId91[7] == 0);
                final String[] got91 = {"", ""};
                final boolean[] ok91 = {false, false, false};
                Db.read(Db.app, m91, new Db.Use<Void>() {
                    @Override public Void run(SqlStore st) {
                        got91[0] = st.item(newId91[5]) == null ? "?" : st.item(newId91[5]).name;
                        got91[1] = st.item(newId91[6]) == null ? "?" : st.item(newId91[6]).name;
                        long shared = DbPaths.find(st, m91, java.util.Arrays.asList("חדש", "משותפת"));
                        ok91[0] = st.listsOf(newId91[0]).size() == 1 && st.listsOf(newId91[0]).get(0) == DbPaths.find(st, m91, java.util.Arrays.asList("חדש"));
                        ok91[1] = DbPaths.find(st, m91, java.util.Arrays.asList("חדש", "מקום שני", "משותפת", "בתוך משותפת")) == newId91[3]
                                && st.listsOf(newId91[3]).size() == 1 && st.listsOf(newId91[3]).get(0) == shared;
                        ok91[2] = st.check() == null;
                        return null;
                    }
                });
                o.check("מבנה 91: השם נשמר כמו שהוקלד — הרווחים שבקצוות יורדים, ו־% ומירכאות אינם מפוענחים («" + got91[0] + "» · «" + got91[1] + "»)",
                        got91[0].equals("רווחים בקצוות") && got91[1].equals(Names.cleanName("100% %תאריך% \"מירכאות\" *")));
                o.check("מבנה 91: הרשימה החדשה — במקום אחד בלבד, בתוך הרשימה שנבחרה; ברשימה משותפת — נראית משני המקומות; המסד תקין",
                        ok91[0] && ok91[1] && ok91[2]);
                List<String> v91 = go("המשך ()\nאופן \"" + m91 + "\"\nודא תוכן ידנית \"ישן\" שווה תוכן ידנית \"חדש\"\n"
                        + "השווה תוכן ידנית \"ישן\" תוכן ידנית \"חדש\" /הבדלים\n", here, vars, lib);
                o.check("מבנה 91: אחרי 12 הפעולות — מה שהדרך הישנה בנתה ומה שהפקודה בנתה שווים עד הסוף " + v91,
                        bad(v91).isEmpty() && String.valueOf(v91).contains("אין הבדלים"));
                // בבית (in = 0), רשימה שכבר איננה, ופריט שאינו רשימה
                Manager.Act home91a = Manager.onListNew(m91, 0, "ישר בבית", here, vars, lib);
                String saidHome91 = home91a.say(lib);
                final long[] par91 = {0, 0};
                final long made91 = home91a.made;
                Db.write(Db.app, m91, null, new Db.Use<Void>() {
                    @Override public Void run(SqlStore st) {
                        par91[0] = st.listsOf(made91).isEmpty() ? -1 : st.listsOf(made91).get(0);
                        par91[1] = st.addNote(home91[0], "רעיון");
                        return null;
                    }
                });
                Manager.Act gone91 = Manager.onListNew(m91, 987654321L, "לשום מקום", here, vars, lib);
                String saidGone91 = gone91.say(lib);
                Manager.Act note91 = Manager.onListNew(m91, par91[1], "בתוך רעיון", here, vars, lib);
                String saidNote91 = note91.say(lib);
                o.check("מבנה 91: בלי רשימה (0) — נוצרת בבית; רשימה שכבר איננה, או פריט שאינו רשימה — סירוב שנאמר, ושום דבר לא נוצר ("
                                + saidHome91 + " · " + saidGone91 + " · " + saidNote91 + ")",
                        !home91a.failed && made91 != 0 && par91[0] == home91[0] && gone91.failed && gone91.made == 0 && saidGone91.equals(Words.Db.LIST_GONE)
                                && note91.failed && note91.made == 0);
                File[] bk91 = Db.backupsDir(m91).listFiles();
                o.check("מבנה 91: יצירה רק מוסיפה — בלי גיבוי לפני (כמו במסך); אופן שאין לו מסד, ובלי שם — סירוב מהתכנון",
                        (bk91 == null || bk91.length == 0)
                                && Manager.onListNew("אין-אופן-כזה-91", 0, "א", here, vars, lib).error() != null && !Db.dbFile(Db.app, "אין-אופן-כזה-91").exists()
                                && Batch.plan("צור תיקייה ידנית %נבחרים%\n", here, vars, lib, null, new Batch.Selection(m91, 0, new ArrayList<Long>())).errors == 1
                                && Batch.plan("צור תיקייה ידנית %נבחרים%\n", here, vars, lib).errors == 1);
                // מבנה 102 (המשתמש): רשימה קיימת — סירוב גם בשורה כתובה, ובלי "ודא שגיאה" / "המשך" — הקובץ כולו אינו רץ
                Batch.Plan md102 = Batch.plan("אופן \"" + m91 + "\"\nMD ידנית \"חדש/בפנים\"\n", here, vars, lib);
                Batch.Plan vb102 = Batch.plan("אופן \"" + m91 + "\"\nצור רשימה \"חדש/בפנים\" /בלי-לפתוח\n", here, vars, lib);
                o.check("מבנה 102: \"צור תיקייה ידנית\" ו\"צור רשימה\" על רשימה קיימת — שגיאה בתכנון, \"שם תפוס\", והקובץ אינו רץ",
                        md102.blocked() && vb102.blocked() && md102.errorText().contains(Words.DbCmd.nameTaken("בפנים"))
                                && vb102.errorText().contains(Words.DbCmd.nameTaken("בפנים")));
                List<String> md91 = go("אופן \"" + m91 + "\"\nודא שגיאה\nMD ידנית \"חדש/בפנים\"\nודא ידנית \"חדש/בפנים\" קיימת\n", here, vars, lib);
                o.check("מבנה 102: עם \"ודא שגיאה\" (הוראה מפורשת) — ממשיך, והרשימה הקיימת לא נגעה " + md91, bad(md91).isEmpty());
                String dlc102 = Files.readUtf8(new File(src, "src/com/tikiyot/vfm/pkudot/DbListCmd.java"));
                int md102at = dlc102.indexOf("static Res md(Store s");
                o.check("מבנה 102: שומר — הכלל האחד: \"צור תיקייה ידנית\" ו\"צור רשימה\" — דרך Lists.newListAt; ה־➕ — דרך Lists.newListChecked; ואין עוד \"כבר קיימת — בלי שינוי\" לרשימה",
                        md102at > 0 && dlc102.indexOf("Lists.newListAt(", md102at) > md102at && dlc102.indexOf("Lists.newListAt(", md102at) < dlc102.indexOf("\n    }\n", md102at)
                                && dlc102.split("Lists\\.newListAt\\(", -1).length == 3 && dlc102.contains("Lists.newListChecked(")
                                && !dlc102.contains("Res.same(Words.ListCmdW.exists(a.label()))") && !dlc102.contains("Res.same(Words.ListCmdW.exists(Names.label(path)))"));
                String ui91 = Files.readUtf8(new File(src, "src/com/tikiyot/vfm/masachim/rechivim/DbVerbsUi.java"))
                        + Files.readUtf8(new File(src, "src/com/tikiyot/vfm/masachim/rechivim/DbPicker.java"));
                boolean none91 = true;
                java.util.ArrayDeque<File> todo91 = new java.util.ArrayDeque<File>();
                todo91.add(new File(src, "src/com/tikiyot/vfm/masachim"));
                while (!todo91.isEmpty()) {
                    File[] fs91 = todo91.poll().listFiles();
                    if (fs91 == null) continue;
                    for (File f : fs91) {
                        if (f.isDirectory()) todo91.add(f);
                        else if (f.getName().endsWith(".java")) {
                            String c91 = Files.readUtf8(f);
                            if (c91.contains("Lists.newList(") || c91.contains("Lists.newListChecked(")) none91 = false;
                        }
                    }
                }
                o.check("מבנה 91: אף מסך אינו יוצר רשימה בעצמו; ה־➕ והבורר — דרך המנהל", none91 && ui91.contains("Manager.onListNew(") && ui91.contains("DbVerbsUi.makeList("));
                // מבנה 93: רעיון וכותרת מהמסך — דרך המנהל; מול הדרך הישנה (המסך כתב בעצמו: addNote / addTitle), על אותן שתי רשימות
                {
                    final String[][] c93 = {                                    // {כותרת?, בתוך מה (דרך; ריק — "ישן"/"חדש" עצמן), הטקסט}
                            {"", "", "רעיון רגיל"}, {"1", "", "כותרת רגילה"}, {"", "בפנים/עמוק", "רעיון בעומק"}, {"", "משותפת", "רעיון ברשימה משותפת"},
                            {"", "", "רעיון רגיל"}, {"1", "", "כותרת רגילה"}, {"", "", "  רווחים  "}, {"", "", "100% %תאריך% \"מירכאות\" /לוכסן\\ #"},
                            {"1", "בפנים", "שתי\nשורות"}};
                    final String[] old93 = new String[c93.length], new93 = new String[c93.length];
                    for (int i = 0; i < c93.length; i++) {
                        final int k = i;
                        final boolean title = !c93[i][0].isEmpty();
                        final List<String> po = new ArrayList<String>(), pn = new ArrayList<String>();
                        po.add("ישן");
                        pn.add("חדש");
                        for (String x : c93[i][1].split("/")) if (!x.isEmpty()) { po.add(x); pn.add(x); }
                        final long[] in = {0};
                        try {
                            Db.write(Db.app, m91, null, new Db.Use<Void>() {
                                @Override public Void run(SqlStore st) {
                                    in[0] = DbPaths.find(st, m91, pn);
                                    long at = DbPaths.find(st, m91, po);
                                    long id = title ? st.addTitle(at, c93[k][2]) : st.addNote(at, c93[k][2]);
                                    old93[k] = id != 0 ? "נוסף" : "לא";
                                    return null;
                                }
                            });
                        } catch (Throwable t) {
                            old93[k] = "נכשל";
                        }
                        // לסירוגין: פעם לפי המספר של הרשימה שבמסך, פעם לפי הדרך שנכתבה
                        Manager.Act act = i % 2 == 0 ? Manager.onListNote(m91, in[0], title, c93[i][2], "", here, vars, lib)
                                : Manager.onListNote(m91, 0, title, c93[i][2], Names.key(pn), here, vars, lib);
                        if (act.error() != null) new93[i] = "תכנון: " + act.error();
                        else {
                            Permit.close();
                            try {
                                act.say(lib);
                                new93[i] = act.failed ? "נכשל" : act.made != 0 ? "נוסף" : "לא";
                                if (Permit.held()) new93[i] += " (ההיתר נשאר פתוח)";
                            } finally {
                                Permit.open();
                            }
                        }
                    }
                    o.check("מבנה 93: רעיון וכותרת בתשעה מצבים (רגיל, בעומק, ברשימה משותפת, אותו טקסט פעמיים, רווחים, תווים מיוחדים, שתי שורות) — "
                                    + "הפקודה עונה כמו המסך " + java.util.Arrays.toString(old93) + " " + java.util.Arrays.toString(new93),
                            java.util.Arrays.equals(old93, new93) && new93[0].equals("נוסף") && new93[7].equals("נוסף"));
                    List<String> v93 = go("המשך ()\nאופן \"" + m91 + "\"\nודא תוכן ידנית \"ישן\" שווה תוכן ידנית \"חדש\"\n"
                            + "השווה תוכן ידנית \"ישן\" תוכן ידנית \"חדש\" /הבדלים\n", here, vars, lib);
                    o.check("מבנה 93: אחרי תשע הפעולות — שתי הרשימות שוות עד הסוף (אותם רעיונות וכותרות, באותו סדר ובאותו טקסט) " + v93,
                            bad(v93).isEmpty() && String.valueOf(v93).contains("אין הבדלים"));
                    Manager.Act none93 = Manager.onListNote(m91, 0, false, "בלי רשימה", "", here, vars, lib);
                    Manager.Act empty93 = Manager.onListNote(m91, home91[0], false, "   ", "", here, vars, lib);
                    Manager.Act lost93 = Manager.onListNote(m91, 0, false, "לרשימה שאין", "אין/כזו", here, vars, lib);
                    String saidLost93 = lost93.error() != null ? "תכנון: " + lost93.error() : lost93.say(lib);
                    Manager.Act gone93 = Manager.onListNote(m91, 987654321L, true, "לרשימה שנמחקה", "", here, vars, lib);
                    String saidGone93 = gone93.error() != null ? "תכנון: " + gone93.error() : gone93.say(lib);
                    o.check("מבנה 93: בלי רשימה — \"" + Words.DbCmd.NOTE_NEEDS_LIST + "\"; טקסט ריק — סירוב; דרך שאין — \"לא נמצא\" כמו במסך; רשימה שנמחקה — נאמר ("
                                    + saidLost93 + " · " + saidGone93 + ")",
                            Words.DbCmd.NOTE_NEEDS_LIST.equals(none93.error()) && empty93.error() != null
                                    && saidLost93.equals(Words.Verb.NOT_FOUND + "אין/כזו") && lost93.failed && lost93.made == 0
                                    && saidGone93.equals(Words.Db.LIST_GONE) && gone93.failed);
                    List<String> w93 = go("המשך ()\nאופן \"" + m91 + "\"\nצור רעיון \"מקובץ\" \"חדש/בפנים\"\nצור רעיון \"מקובץ\" \"ישן/בפנים\"\nודא שגיאה\nצור רעיון %נבחרים% \"חדש\"\n", here, vars, lib);
                    o.check("מבנה 93: \"צור רעיון\" כתובה לא השתנתה; ו־%נבחרים% בלי בחירה של מסך — שגיאה " + w93, bad(w93).isEmpty());
                    String ui93 = Files.readUtf8(new File(src, "src/com/tikiyot/vfm/masachim/rechivim/DbVerbsUi.java"));
                    o.check("מבנה 93: המסך אינו כותב עוד רעיון או כותרת בעצמו", !ui93.contains(".addNote(") && !ui93.contains(".addTitle(") && ui93.contains("Manager.onListNote("));
                }
                // מבנה 97: רשימה חכמה חדשה מהטופס — דרך המנהל; מול הדרך הישנה (המסך שמר בעצמו), על אותן שתי רשימות
                {
                    final String[][] c97 = {   // {בתוך מה, שם, תנאי, מיון, תצוגה, צלמית, תיאור, למה}
                            {"", "חכמה רגילה", "סוג:pdf", "", "", "", "", ""},
                            {"בפנים/עמוק", "בעומק", "סוג:pdf כללים|הלכות", "שם", "תיקיות", "📚", "תיאור", "למה כך"},
                            {"משותפת", "ברשימה משותפת", "תאריך:שבוע", "תאריך", "", "", "", ""},
                            {"", "חכמה רגילה", "סוג:epub", "", "", "", "", ""},                         // אותו שם — שוב (כמו במסך: נוצרת)
                            {"", "", "גודל:>10MB", "", "", "", "", ""},                                 // בלי שם — "רשימה חכמה"
                            {"", "  רווחים  ", "100% %תאריך% \"מירכאות\" מקור:«/storage/emulated/0/ספרים חדשים»", "", "", "", "", ""},
                            {"בפנים", "בפנים", "סוג:txt", "", "", "", "שורה\nשנייה", ""}};             // אותו שם כמו רשימה ידנית
                    final String[] old97 = new String[c97.length], new97 = new String[c97.length];
                    for (int i = 0; i < c97.length; i++) {
                        final int k = i;
                        final DbSmartForm.Result f97 = new DbSmartForm.Result();
                        f97.name = c97[i][1];
                        f97.cond = c97[i][2];
                        f97.sort = c97[i][3];
                        f97.view = c97[i][4];
                        f97.icon = c97[i][5];
                        f97.desc = c97[i][6];
                        f97.why = c97[i][7];
                        final List<String> po = new ArrayList<String>(), pn = new ArrayList<String>();
                        po.add("ישן");
                        pn.add("חדש");
                        for (String x : c97[i][0].split("/")) if (!x.isEmpty()) { po.add(x); pn.add(x); }
                        final long[] in = {0};
                        Db.write(Db.app, m91, null, new Db.Use<Void>() {
                            @Override public Void run(SqlStore st) {
                                in[0] = DbPaths.find(st, m91, pn);
                                long at = DbPaths.find(st, m91, po);
                                // מבנה 103 (המשתמש): שם קיים — עוצרים; הדרך "הישנה" כאן היא הכלל שהמשתמש קבע, כדי ששתי הרשימות יישארו שוות
                                if (Lists.smartTaken(st, at != 0 ? at : DbPaths.home(st), f97.name.trim().isEmpty() ? Words.SMART_LIST : f97.name.trim()))
                                    old97[k] = "תכנון: " + Words.ListCmdW.smartTaken(f97.name.trim());
                                else old97[k] = DbSmartForm.save(st, 0, at, f97) != 0 ? "נוצרה" : "לא";
                                return null;
                            }
                        });
                        Manager.Act act = Manager.onSmartNew(m91, in[0], f97.name, f97.cond, DbSmartForm.props(f97), here, vars, lib);
                        if (act.error() != null) new97[i] = "תכנון: " + act.error();
                        else {
                            Permit.close();
                            try {
                                act.say(lib);
                                new97[i] = act.failed ? "נכשל" : act.made != 0 ? "נוצרה" : "לא";
                                if (Permit.held()) new97[i] += " (ההיתר נשאר פתוח)";
                                if (!act.plan.opens.isEmpty()) new97[i] += " (נפתחת בסוף)";
                            } finally {
                                Permit.open();
                            }
                        }
                    }
                    o.check("מבנה 97: רשימה חכמה חדשה בשבעה מצבים (בבית, בעומק, ברשימה משותפת, אותו שם שוב, בלי שם, תווים מיוחדים ו«», שם של רשימה ידנית) — "
                                    + "הפקודה עונה כמו המסך " + java.util.Arrays.toString(old97) + " " + java.util.Arrays.toString(new97),
                            java.util.Arrays.equals(old97, new97) && new97[0].equals("נוצרה")
                                    && new97[3].equals("תכנון: " + Words.ListCmdW.smartTaken("חכמה רגילה")));
                    // מבנה 104: "לדרוס" (המשתמש ענה במסך) — אותה פקודה עם /תפוס:להחליף; הקיימת מקבלת את ההגדרות, עם גיבוי לפני
                    {
                        final DbSmartForm.Result f103 = new DbSmartForm.Result();
                        f103.name = "חכמה רגילה";
                        f103.cond = "סוג:epub";
                        final long[] in103 = {0, 0};
                        Db.write(Db.app, m91, null, new Db.Use<Void>() {
                            @Override public Void run(SqlStore st) {
                                in103[0] = DbPaths.find(st, m91, java.util.Arrays.asList("חדש"));
                                long oldIn = DbPaths.find(st, m91, java.util.Arrays.asList("ישן"));
                                DbSmartForm.save(st, Lists.smartIn(st, oldIn, f103.name), 0, f103);   // הצד השני — אותה דריסה
                                in103[1] = Lists.smartIn(st, in103[0], f103.name);
                                return null;
                            }
                        });
                        java.util.Set<String> edits104 = editNames(m91);
                        Manager.Act rep104 = Manager.onSmartNew(m91, in103[0], f103.name, f103.cond, DbSmartForm.props(f103), Batch.TAKEN_REPLACE, here, vars, lib);
                        String said104 = rep104.error() != null ? "תכנון: " + rep104.error() : rep104.say(lib);
                        o.check("מבנה 104: \"לדרוס\" — הפקודה עם /תפוס:להחליף; אותה רשימה (לא עוד אחת), עם גיבוי לפני (" + said104 + ")",
                                rep104.error() == null && !rep104.failed && rep104.made == in103[1] && rep104.text.contains(Batch.SW_TAKEN + Batch.TAKEN_REPLACE)
                                        && said104.contains(Words.ListCmdW.smartReplaced("חכמה רגילה")) && !edits104.containsAll(editNames(m91)));
                    }
                    List<String> v97 = go("המשך ()\nאופן \"" + m91 + "\"\nודא תוכן ידנית \"ישן\" שווה תוכן ידנית \"חדש\"\n"
                            + "השווה תוכן ידנית \"ישן\" תוכן ידנית \"חדש\" /הבדלים\n", here, vars, lib);
                    o.check("מבנה 97: אחרי שבע הפעולות — שתי הרשימות שוות עד הסוף, כולל התנאי, המיון, התצוגה, הצלמית, התיאור וה\"למה\" " + v97,
                            bad(v97).isEmpty() && String.valueOf(v97).contains("אין הבדלים"));
                    final String[] cond97 = {""};
                    Db.read(Db.app, m91, new Db.Use<Void>() {
                        @Override public Void run(SqlStore st) {
                            for (Store.Member m : st.members(DbPaths.find(st, m91, java.util.Arrays.asList("חדש"))))
                                if (m.item.type == Store.SMART && m.item.name.equals("רווחים")) cond97[0] = st.prop(m.item.id, Store.P_COND);
                            return null;
                        }
                    });
                    o.check("מבנה 97: התנאי נשמר כלשונו — % ומירכאות ו«» אינם מפוענחים («" + cond97[0] + "»)", cond97[0].equals(c97[5][2]));
                    Manager.Act gone97 = Manager.onSmartNew(m91, 987654321L, "לשום מקום", "סוג:pdf", new java.util.HashMap<String, String>(), here, vars, lib);
                    String said97 = gone97.say(lib);
                    List<String> w97 = go("המשך ()\nאופן \"" + m91 + "\"\nצור רשימה חכמה \"מקובץ 97\" סוג:pdf\nצור רשימה חכמה \"מקובץ 97\" סוג:pdf\n"
                            + "ודא שגיאה\nצור רשימה חכמה %שם-חדש% %תוכן%\n", here, vars, lib);
                    Batch.Plan stop103 = Batch.plan("אופן \"" + m91 + "\"\nצור רשימה חכמה \"מקובץ 97\" סוג:txt\n", here, vars, lib);
                    o.check("מבנה 104: בקובץ בלי מתג — שם קיים: שגיאה בתכנון, והקובץ אינו רץ; עם \"המשך\" — השורה אינה מבוצעת, והקובץ ממשיך",
                            stop103.blocked() && stop103.errorText().contains(Words.ListCmdW.smartTaken("מקובץ 97"))
                                    && String.valueOf(w97).contains(Words.ListCmdW.smartTaken("מקובץ 97")));
                    List<String> t104 = go("אופן \"" + m91 + "\"\nצור רשימה חכמה \"מקובץ 97\" סוג:txt /תפוס:להחליף\nצור רשימה חכמה \"מקובץ 97\" סוג:mp3 /תפוס:לשנות-שם\n"
                            + "צור רשימה חכמה \"מקובץ 97\" סוג:zip /תפוס:לדלג\nודא שגיאה\nצור רשימה חכמה \"מקובץ 97\" סוג:zip /תפוס:בלבול\n", here, vars, lib);
                    final String[] c104 = {"", ""};
                    Db.read(Db.app, m91, new Db.Use<Void>() {
                        @Override public Void run(SqlStore st) {
                            for (Store.Item it : st.smarts()) {
                                if (it.name.equals("מקובץ 97")) c104[0] += st.prop(it.id, Store.P_COND) + ";";
                                if (it.name.equals("מקובץ 97 (2)")) c104[1] += st.prop(it.id, Store.P_COND) + ";";
                            }
                            return null;
                        }
                    });
                    o.check("מבנה 104: בקובץ — /תפוס:להחליף דורס, /תפוס:לשנות-שם — \"(2)\", /תפוס:לדלג — בלי שינוי, ערך לא מוכר — שגיאה ("
                                    + c104[0] + " · " + c104[1] + ") " + t104,
                            bad(t104).isEmpty() && c104[0].equals("סוג:txt;") && c104[1].equals("סוג:mp3;") && String.valueOf(t104).contains(Words.ListCmdW.smartSkipped("מקובץ 97")));
                    o.check("מבנה 97: רשימה שנמחקה — סירוב שנאמר; \"צור רשימה חכמה\" פעמיים עם \"המשך\" — אחת (מבנה 104); %שם-חדש% בלי טופס — שגיאה "
                                    + said97 + " " + w97,
                            gone97.failed && said97.equals(Words.Db.LIST_GONE) && bad(w97).contains("כישלונות בהרצה: 1") && smarts97(m91, "מקובץ 97") == 1);
                    o.check("מבנה 98: רשימה חכמה חדשה נאמרת \"רשימה חכמה חדשה\" — לא \"רשימה ידנית חדשה\" " + w97,
                            String.valueOf(w97).contains(Words.ListCmdW.createdSmart("מקובץ 97")) && !String.valueOf(w97).contains(Words.ListCmdW.created("מקובץ 97")));
                    String ui97 = Files.readUtf8(new File(src, "src/com/tikiyot/vfm/masachim/rechivim/DbVerbsUi.java"))
                            + Files.readUtf8(new File(src, "src/com/tikiyot/vfm/masachim/SearchScreen.java"));
                    o.check("מבנה 97: ה־➕ ומסך החיפוש אינם יוצרים עוד רשימה חכמה בעצמם — דרך המנהל",
                            !ui97.contains("DbSmartForm.save(") && ui97.contains("Manager.onSmartNew(") && ui97.contains("DbVerbsUi.makeSmart("));
                }
                // מבנה 99: עריכה של רשימה חכמה בטופס — דרך המנהל; מול הדרך הישנה (המסך שמר בעצמו, עם גיבוי לפני ועדכון קובצי פקודות)
                {
                    final String[][] e99 = {   // {הרשימה (שם בתוך "ישן"/"חדש"), שם חדש, תנאי, מיון, תצוגה, צלמית, תיאור, למה}
                            {"חכמה רגילה", "", "סוג:pdf|epub", "גודל", "תיקיות", "📗", "חדש", "כי"},          // בלי שינוי בשם
                            {"רשימה חכמה", "אחרי שינוי", "גודל:>1MB", "", "", "", "", ""},                // שם חדש
                            {"בפנים", "בפנים", "100% «שתי מילים» \"מירכאות\"", "", "", "", "", ""}};          // אותו שם, תנאי עם תווים
                    final String[] old99 = new String[e99.length], new99 = new String[e99.length];
                    java.util.Set<String> before99 = editNames(m91);
                    for (int i = 0; i < e99.length; i++) {
                        final int k = i;
                        final DbSmartForm.Result f = new DbSmartForm.Result();
                        f.name = e99[i][1];
                        f.cond = e99[i][2];
                        f.sort = e99[i][3];
                        f.view = e99[i][4];
                        f.icon = e99[i][5];
                        f.desc = e99[i][6];
                        f.why = e99[i][7];
                        final long[] ids = {0, 0};
                        Db.write(Db.app, m91, null, new Db.Use<Void>() {
                            @Override public Void run(SqlStore st) {
                                for (int side = 0; side < 2; side++)
                                    for (Store.Member m : st.members(DbPaths.find(st, m91, java.util.Arrays.asList(side == 0 ? "ישן" : "חדש"))))
                                        if (m.item.type == Store.SMART && m.item.name.equals(e99[k][0]) && ids[side] == 0) ids[side] = m.item.id;
                                return null;
                            }
                        });
                        old99[i] = DbListCmd.nowEdit(m91, new DbListCmd.Work() {           // הדרך הישנה — בדיוק מה שהמסך עשה
                            @Override public DbListCmd.Res run(Store t, DbListCmd.Finder fd) {
                                if (t.item(ids[0]) == null) return DbListCmd.Res.err(Words.Db.ITEM_GONE);
                                String was = t.item(ids[0]).name;
                                DbSmartForm.save(t, ids[0], 0, f);
                                return DbListCmd.Res.ok(Words.DbCmd.saved(f.name.isEmpty() ? was : f.name)).renamed(ids[0], was, t.item(ids[0]).name);
                            }
                        });
                        Manager.Act act = Manager.onSmartEdit(m91, ids[1], f.name, f.cond, DbSmartForm.props(f), here, vars, lib);
                        Permit.close();
                        try {
                            new99[i] = act.error() != null ? "תכנון: " + act.error() : act.say(lib);
                            if (Permit.held()) new99[i] += " (ההיתר נשאר פתוח)";
                            if (!act.plan.opens.isEmpty()) new99[i] += " (נפתחת בסוף)";
                        } finally {
                            Permit.open();
                        }
                    }
                    o.check("מבנה 99: עריכה של רשימה חכמה בשלושה מצבים (בלי שינוי בשם, שם חדש, תנאי עם תווים) — ההודעה של הפקודה כמו של המסך "
                                    + java.util.Arrays.toString(old99) + " " + java.util.Arrays.toString(new99),
                            java.util.Arrays.equals(old99, new99));
                    List<String> v99 = go("המשך ()\nאופן \"" + m91 + "\"\nודא תוכן ידנית \"ישן\" שווה תוכן ידנית \"חדש\"\n"
                            + "השווה תוכן ידנית \"ישן\" תוכן ידנית \"חדש\" /הבדלים\n", here, vars, lib);
                    o.check("מבנה 99: אחרי שלוש העריכות — שתי הרשימות שוות עד הסוף " + v99, bad(v99).isEmpty() && String.valueOf(v99).contains("אין הבדלים"));
                    java.util.Set<String> after99 = editNames(m91);
                    o.check("מבנה 99: לפני כל עריכה — גיבוי \"לפני עריכה\", גם בפקודה (" + before99 + " ← " + after99 + ")", !before99.containsAll(after99));
                    final long[] two104 = {0, 0};
                    Db.write(Db.app, m91, null, new Db.Use<Void>() {
                        @Override public Void run(SqlStore st) {
                            long in = DbPaths.find(st, m91, java.util.Arrays.asList("חדש"));
                            two104[0] = Lists.smartIn(st, in, "חכמה רגילה");
                            two104[1] = Lists.saveSmart(st, 0, in, "שנייה 104", "סוג:pdf", null);
                            return null;
                        }
                    });
                    Manager.Act ren104 = Manager.onSmartEdit(m91, two104[1], "חכמה רגילה", "סוג:pdf", new java.util.HashMap<String, String>(), here, vars, lib);
                    o.check("מבנה 104: שינוי שם בטופס לשם שכבר קיים באותה רשימה — \"שם קיים\" כבר בתכנון, ושום דבר לא נשמר (" + ren104.error() + ")",
                            Words.ListCmdW.smartTaken("חכמה רגילה").equals(ren104.error()) && two104[0] != 0);
                    Db.write(Db.app, m91, null, new Db.Use<Void>() {
                        @Override public Void run(SqlStore st) { Lists.removeFrom(st, DbPaths.find(st, m91, java.util.Arrays.asList("חדש")), two104[1]); return null; }
                    });
                    // מבנה 105: כפולות שכבר קיימות — אותה שאלה כמו ביצירה; התשובות — בפקודות שהמסך מריץ (עריכה, הסרה מכאן)
                    {
                        final long[] t105 = {0, 0, 0, 0, 0};                       // רשימה, ראשונה, שנייה, (לשם אחר:) ראשונה, שנייה
                        java.util.Map<String, String> p105 = new java.util.HashMap<String, String>();
                        p105.put(Store.P_SORT, "גודל");
                        p105.put(Store.P_ICON, "🔁");
                        final java.util.Map<String, String> pp105 = p105;
                        Db.write(Db.app, m91, null, new Db.Use<Void>() {           // כמו שהיה לפני צעד 104: שתיים באותו שם באותה רשימה
                            @Override public Void run(SqlStore st) {
                                t105[0] = Lists.newListChecked(st, 0, "כפולות 105").id;
                                t105[1] = Lists.saveSmart(st, 0, t105[0], "תאומה", "סוג:pdf", null);
                                t105[2] = Lists.saveSmart(st, 0, t105[0], "תאומה", "סוג:epub", pp105);
                                t105[3] = Lists.saveSmart(st, 0, t105[0], "שנייה", "סוג:txt", null);
                                t105[4] = Lists.saveSmart(st, 0, t105[0], "שנייה", "סוג:doc", null);
                                return null;
                            }
                        });
                        final long[][] found105 = new long[1][];
                        final java.util.Map<String, String>[] got105 = new java.util.Map[1];
                        Db.read(Db.app, m91, new Db.Use<Void>() {
                            @Override public Void run(SqlStore st) {
                                found105[0] = Lists.smartTwin(st, t105[0]);
                                got105[0] = Lists.smartProps(st, t105[2]);
                                return null;
                            }
                        });
                        o.check("מבנה 105: הזוג הראשון של כפולות נמצא — הראשונה והשנייה לפי המקום",
                                found105[0] != null && found105[0][0] == t105[1] && found105[0][1] == t105[2]);
                        // "לדרוס": הראשונה מקבלת את ההגדרות של השנייה, והשנייה יוצאת
                        Manager.Act e105 = Manager.onSmartEdit(m91, t105[1], "תאומה", "סוג:epub", got105[0], here, vars, lib);
                        String se105 = e105.error() != null ? e105.error() : e105.say(lib);
                        Manager.Act r105 = Manager.onListRemove(m91, t105[0], java.util.Collections.singletonList(t105[2]), here, vars, lib);
                        String sr105 = r105.error() != null ? r105.error() : r105.say(lib);
                        // "שם אחר": לשנייה — שם חדש; ושם קיים — שוב השאלה
                        Manager.Act taken105 = Manager.onSmartEdit(m91, t105[4], "תאומה", "סוג:doc", new java.util.HashMap<String, String>(), here, vars, lib);
                        Manager.Act n105 = Manager.onSmartEdit(m91, t105[4], "שנייה אחרת", "סוג:doc", new java.util.HashMap<String, String>(), here, vars, lib);
                        String sn105 = n105.error() != null ? n105.error() : n105.say(lib);
                        final String[] after105 = {""};
                        final boolean[] none105 = {false};
                        Db.read(Db.app, m91, new Db.Use<Void>() {
                            @Override public Void run(SqlStore st) {
                                for (Store.Member m : st.members(t105[0]))
                                    after105[0] += m.item.name + "=" + st.prop(m.item.id, Store.P_COND) + "/" + st.prop(m.item.id, Store.P_SORT) + "/" + st.prop(m.item.id, Store.P_ICON) + ";";
                                none105[0] = Lists.smartTwin(st, t105[0]) == null && st.item(t105[2]) == null;
                                return null;
                            }
                        });
                        o.check("מבנה 105: \"לדרוס\" — הראשונה קיבלה את ההגדרות של השנייה (תנאי, מיון, צלמית), והשנייה יצאה; \"שם אחר\" — שם קיים: שוב \"שם קיים\"; "
                                        + "שם פנוי — נשמר; ואין עוד כפולות (" + after105[0] + ") " + se105 + " · " + sr105 + " · " + sn105,
                                after105[0].equals("תאומה=סוג:epub/גודל/🔁;שנייה=סוג:txt/null/null;שנייה אחרת=סוג:doc/null/null;") && none105[0]
                                        && Words.ListCmdW.smartTaken("תאומה").equals(taken105.error()));
                        String scr105 = Files.readUtf8(new File(src, "src/com/tikiyot/vfm/masachim/DbListScreen.java"));
                        o.check("מבנה 105: מסך הרשימות שואל על כפולות כשנפתח, ועונה דרך המנהל בלבד",
                                scr105.contains("Lists.smartTwin(store, listId)") && scr105.contains("askTwin();") && scr105.contains("Manager.onListRemove(mode, listId, java.util.Collections.singletonList(twin[1])"));
                    }
                    // מבנה 106: "עדכן רשימה חכמה" בקובץ — שם עם ספק: עוצרים; עם המקום: מעדכנים, עם גיבוי לפני
                    {
                        go("המשך ()\nאופן \"" + m91 + "\"\nMD ידנית \"ספרים 106\"\nMD ידנית \"שיעורים 106\"\n", here, vars, lib);
                        Db.write(Db.app, m91, null, new Db.Use<Void>() {
                            @Override public Void run(SqlStore st) {
                                Lists.saveSmart(st, 0, DbPaths.find(st, m91, java.util.Arrays.asList("ספרים 106")), "חדשים 106", "a", null);
                                Lists.saveSmart(st, 0, DbPaths.find(st, m91, java.util.Arrays.asList("שיעורים 106")), "חדשים 106", "b", null);
                                return null;
                            }
                        });
                        Batch.Plan amb106 = Batch.plan("אופן \"" + m91 + "\"\nעדכן רשימה חכמה \"חדשים 106\" סוג:pdf\n", here, vars, lib);
                        java.util.Set<String> e106 = editNames(m91);
                        List<String> ok106 = go("אופן \"" + m91 + "\"\nעדכן רשימה חכמה \"שיעורים 106/חדשים 106\" סוג:pdf\n", here, vars, lib);
                        final String[] c106 = {"", ""};
                        Db.read(Db.app, m91, new Db.Use<Void>() {
                            @Override public Void run(SqlStore st) {
                                c106[0] = st.prop(Lists.smartIn(st, DbPaths.find(st, m91, java.util.Arrays.asList("ספרים 106")), "חדשים 106"), Store.P_COND);
                                c106[1] = st.prop(Lists.smartIn(st, DbPaths.find(st, m91, java.util.Arrays.asList("שיעורים 106")), "חדשים 106"), Store.P_COND);
                                return null;
                            }
                        });
                        o.check("מבנה 106: בקובץ — שם של שתי רשימות חכמות: הקובץ אינו רץ, \"יש כמה… לכתוב את המקום\"; עם המקום — רק היא מתעדכנת, וגיבוי לפני ("
                                        + c106[0] + " · " + c106[1] + ") " + ok106,
                                amb106.blocked() && amb106.errorText().contains("יש כמה רשימות חכמות בשם \"חדשים 106\"") && bad(ok106).isEmpty()
                                        && c106[0].equals("a") && c106[1].equals("סוג:pdf") && !e106.containsAll(editNames(m91)));
                    }
                    // מבנה 107: בקובץ — רשימה ידנית בשם של שתיים: הקובץ אינו רץ; עם המקום — רץ
                    {
                        go("המשך ()\nאופן \"" + m91 + "\"\nMD ידנית \"ספרים 106/פרק 107\"\nMD ידנית \"שיעורים 106/פרק 107\"\n", here, vars, lib);
                        Batch.Plan amb107 = Batch.plan("אופן \"" + m91 + "\"\nצור רעיון \"x\" \"פרק 107\"\n", here, vars, lib);
                        Batch.Plan amb107b = Batch.plan("אופן \"" + m91 + "\"\nהעתק ידנית \"פרק 107\" ידנית \"חדש\"\n", here, vars, lib);
                        List<String> ok107 = go("אופן \"" + m91 + "\"\nצור רעיון \"רעיון 107\" \"שיעורים 106/פרק 107\"\n", here, vars, lib);
                        final int[] n107 = {-1, -1};
                        Db.read(Db.app, m91, new Db.Use<Void>() {
                            @Override public Void run(SqlStore st) {
                                n107[0] = st.members(DbPaths.find(st, m91, java.util.Arrays.asList("שיעורים 106", "פרק 107"))).size();
                                n107[1] = st.members(DbPaths.find(st, m91, java.util.Arrays.asList("ספרים 106", "פרק 107"))).size();
                                return null;
                            }
                        });
                        ok107.add("בשיעורים: " + n107[0] + ", בספרים: " + n107[1]);
                        o.check("מבנה 107: בקובץ — שם של שתי רשימות ידניות: הקובץ אינו רץ, \"יש כמה רשימות… לכתוב את המקום\" (רעיון, העתקה); עם המקום — רק היא " + ok107,
                                amb107.blocked() && amb107.errorText().contains("יש כמה רשימות בשם \"פרק 107\"")
                                        && amb107b.blocked() && amb107b.errorText().contains("יש כמה רשימות בשם \"פרק 107\"") && bad(ok107).isEmpty()
                                        && n107[0] == 1 && n107[1] == 0);
                    }
                    // מבנה 108 (ה4): שינוי שם של רשימה בקובץ — עם גיבוי "לפני עריכה", כמו בטופס
                    {
                        java.util.Set<String> b108 = editNames(m91);
                        List<String> r108 = go("אופן \"" + m91 + "\"\nשנה ידנית \"שיעורים 106/פרק 107\" \"פרק 108\"\n", here, vars, lib);
                        o.check("מבנה 108: \"שנה ידנית\" בקובץ — השם השתנה, ולפני כן נעשה גיבוי \"לפני עריכה\" " + r108,
                                bad(r108).isEmpty() && !b108.containsAll(editNames(m91)));
                    }
                    Manager.Act gone99 = Manager.onSmartEdit(m91, 987654321L, "x", "סוג:pdf", new java.util.HashMap<String, String>(), here, vars, lib);
                    String saidGone99 = gone99.say(lib);
                    o.check("מבנה 99: רשימה שנמחקה בזמן שהטופס היה פתוח — \"" + Words.Db.ITEM_GONE + "\" (" + saidGone99 + ")",
                            gone99.failed && saidGone99.equals(Words.Db.ITEM_GONE));
                    List<String> w99 = go("המשך ()\nאופן \"" + m91 + "\"\nעדכן רשימה חכמה \"מקובץ 97\" סוג:epub\nודא שגיאה\nעדכן רשימה חכמה %נבחרים% %תוכן%\n", here, vars, lib);
                    o.check("מבנה 99: \"עדכן רשימה חכמה\" כתובה לא השתנתה; %נבחרים% בלי טופס — שגיאה " + w99, bad(w99).isEmpty());
                    String scr99 = Files.readUtf8(new File(src, "src/com/tikiyot/vfm/masachim/DbListScreen.java"));
                    o.check("מבנה 99: מסך הרשימות אינו שומר עוד רשימה חכמה בעצמו — דרך המנהל",
                            !scr99.contains("DbSmartForm.save(") && scr99.contains("Manager.onSmartEdit("));
                }
                // מבנה 100: כפתורים שהפעולה שלהם שורה שכותבת — "צור רשימה", "צור רשימה חכמה", "עדכן רשימה חכמה"; מול הדרך הישנה
                {
                    final String[] steps100 = {"list", "list", "smart:סוג:pdf", "smart:סוג:pdf", "smart:סוג:epub", "update:סוג:txt"};
                    final long[] oldIds = new long[steps100.length];
                    final long[] newIds = new long[steps100.length];
                    final boolean[] newWrites = new boolean[steps100.length];
                    String wasMode100 = Db.currentMode;
                    Db.currentMode = m91;                                       // הכפתור פועל באופן שפתוח במסך — כמו בתוכנה
                    try {
                    for (int i = 0; i < steps100.length; i++) {
                        final String st100 = steps100[i];
                        Db.write(Db.app, m91, null, new Db.Use<Void>() {                // הדרך הישנה — מה שהכפתור עשה בעצמו
                            @Override public Void run(SqlStore t) {
                                int k = java.util.Arrays.asList(steps100).indexOf(st100);
                                long id = st100.equals("list") ? DbPaths.ensure(t, m91, java.util.Arrays.asList("ישן-כפתור", "א", "ב"))
                                        : st100.startsWith("smart:") ? Lists.newSmart(t, 0, "ישן-כפתור", st100.substring(6), null, Lists.TAKEN_STOP).id
                                        : DbPaths.updateSmart(t, m91, java.util.Arrays.asList("ישן-כפתור"), st100.substring(7), "").id;
                                for (int j = 0; j < steps100.length; j++) if (steps100[j].equals(st100) && oldIds[j] == 0) { oldIds[j] = id; break; }
                                return null;
                            }
                        });
                        String line = st100.equals("list") ? "צור רשימה \"חדש-כפתור/א/ב\""
                                : st100.startsWith("smart:") ? "צור רשימה חכמה \"חדש-כפתור\" " + st100.substring(6)
                                : "עדכן רשימה חכמה \"חדש-כפתור\" " + st100.substring(7);
                        Manager.Act act = Manager.onVerbLine(line, here, vars, lib);
                        Permit.close();
                        try {
                            newWrites[i] = act.error() == null && act.writes();
                            if (newWrites[i]) act.say(lib);
                            newIds[i] = act.made;
                        } finally {
                            Permit.open();
                        }
                    }
                    } finally {
                        Db.currentMode = wasMode100;
                    }
                    final List<String> old100 = new ArrayList<String>(), new100 = new ArrayList<String>();
                    Db.read(Db.app, m91, new Db.Use<Void>() {
                        @Override public Void run(SqlStore t) {
                            for (Store.Item it : t.smarts()) {
                                String row = it.name.replace("ישן-", "X-").replace("חדש-", "X-") + " · " + t.prop(it.id, Store.P_COND);
                                if (it.name.startsWith("ישן-כפתור")) old100.add(row);
                                if (it.name.startsWith("חדש-כפתור")) new100.add(row);
                            }
                            return null;
                        }
                    });
                    // מבנה 102–103 (המשתמש): שם קיים — סירוב, לרשימה ולרשימה חכמה; בכפתור אין "המשך", ולכן גם בתנאי אחר — סירוב
                    o.check("מבנה 103: רשימות חכמות מכפתור — רק אחת בשם הזה (אותו שם שוב, גם בתנאי אחר — סירוב), והעדכון חל עליה " + new100,
                            new100.size() == 1 && new100.get(0).equals("X-כפתור · סוג:txt"));
                    o.check("מבנה 100: שם קיים — בלי כתיבה; מה שנוצר או עודכן — המספר שלו חוזר " + java.util.Arrays.toString(newIds),
                            newWrites[0] && !newWrites[1] && newWrites[2] && !newWrites[3] && !newWrites[4] && newWrites[5]
                                    && newIds[0] != 0 && newIds[2] != 0 && newIds[5] == newIds[2] && oldIds[0] != 0);
                    List<String> v100 = go("המשך ()\nאופן \"" + m91 + "\"\nודא תוכן ידנית \"ישן-כפתור\" שווה תוכן ידנית \"חדש-כפתור\"\n", here, vars, lib);
                    o.check("מבנה 100: \"צור רשימה\" מכפתור — שתי הדרכים בנו אותו מבנה " + v100, bad(v100).isEmpty());
                    Manager.Act none100 = Manager.onVerbLine("אופן \"" + m91 + "\"\nעדכן רשימה חכמה \"אין כזו 100\" סוג:pdf", here, vars, lib);
                    o.check("מבנה 100: עדכון של רשימה חכמה שאין — סירוב מהתכנון (מבנה 106: בנוסח של הכלל האחד) (" + none100.error() + ")",
                            Words.ListCmdW.noSmart("אין כזו 100").equals(none100.error()));
                    String ui100 = Files.readUtf8(new File(src, "src/com/tikiyot/vfm/masachim/rechivim/DbVerbsUi.java"));
                    o.check("מבנה 100: הכפתורים אינם כותבים עוד רשימה או רשימה חכמה בעצמם — דרך המנהל",
                            !ui100.contains("DbPaths.ensure(") && !ui100.contains("DbPaths.ensureSmart(") && !ui100.contains("DbPaths.updateSmart(")
                                    && ui100.contains("Manager.onVerbLine("));
                }
                List<String> e92 = go("המשך ()\nאופן \"" + m91 + "\"\nודא שגיאה\nשנה ידנית \"חדש/בפנים\" \"  \"\nודא ידנית \"חדש/בפנים\" קיימת\n"
                        + "ודא ידנית \"חדש/ללא שם\" חסרה\n", here, vars, lib);
                o.check("מבנה 92: שם שכולו רווחים — סירוב, גם בשינוי שם בפקודה (עד כאן הרשימה קיבלה את השם \"ללא שם\") " + e92, bad(e92).isEmpty());
                go("המשך ()\nבטל אופן \"" + m91 + "\"\n", here, vars, lib);
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

    /** מבנה 106: השמות של הגיבויים "לפני עריכה" (נשמרים עד חמישה — לכן משווים שמות, לא ספירה). */
    private static java.util.Set<String> editNames(String mode) {
        java.util.Set<String> out = new java.util.HashSet<String>();
        File[] bk = Db.backupsDir(mode).listFiles();
        if (bk != null) for (File f : bk) if (f.getName().contains(Backups.OP_EDIT)) out.add(f.getName());
        return out;
    }

    /** מבנה 97: כמה רשימות חכמות בשם הזה יש במסד של האופן. */
    private static int smarts97(final String mode, final String name) throws Exception {
        final int[] n = {0};
        Db.read(Db.app, mode, new Db.Use<Void>() {
            @Override public Void run(SqlStore st) {
                for (Store.Item it : st.smarts()) if (it.name.equals(name)) n[0]++;
                return null;
            }
        });
        return n[0];
    }

    /** השורות של הדוח שמזכירות את what, ומה שמתחתן (לשם הבדיקה, כשהיא נכשלת). */
    private static String pick(List<String> rep, String what) {
        StringBuilder sb = new StringBuilder();
        for (String l : rep) if (l.contains("ל־" + what)) sb.append("[").append(l).append("] ");
        return sb.toString();
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
