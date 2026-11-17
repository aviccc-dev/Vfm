package com.tikiyot.vfm;

// הגשר (vfm) — הדלת של התוספים אל התוכנה ("מחברת": כל תוסף עובד בצמוד ללב, דרך דלת אחת).
// מה שעמוד מעבדה יכול לעשות בתוכנה; כללי, לכל ניסוי (המשתמש: "המעבדה זה לא רק למצב הזה, אלא גם לפיתוחים
// אחרים"): לקרוא, לבחור בבורר האמיתי, לכתוב קובצי פקודות, ולהריץ פקודות דרך ההרצה הרגילה (תצוגה מקדימה ואישור).
// מבנה 2 (13.11.2026): יצא מ־LabScreen (שם היה מחלקה פנימית) לתיקיית הלב — אותן פונקציות ואותן שורות; מה שהיה "המסך"
// הוא עכשיו screen (המסך שמארח את העמוד), ומה שרק המסך יודע — דרך Host.
// אחראי: מה שתוסף יכול לבקש. אסור לו: לצייר את המסך של התוסף (LabScreen); לכתוב או למחוק קבצים אחרים ישירות — כל שינוי
// אחר עובר דרך הפקודות.

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.webkit.JavascriptInterface;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.List;
import java.util.Map;

/** מה שעמוד מעבדה יכול לעשות. כל קריאה — מחזירה טקסט (JSON כשיש מבנה); בחירה — חוזרת ב־window.vfmResult(id, ערך). */
final class Bridge {

    /** מה שהגשר צריך מהמסך שמארח את העמוד. המסך מכיר את הגשר; הגשר מכיר מהמסך רק את זה. */
    interface Host {
        /** תשובה לעמוד: window.vfmResult(id, ערך). */
        void result(String id, String value);

        /** קובץ הפקודות לפתוח בעורך (מרשימה, מהפס הצף) — פעם אחת; null — אין. */
        String startFile();

        /** מבנה 245: מה הלוח של העיצוב מעצב כשנפתח — "רשימה"/"אופן"/"תוכנה", טאב, הדרך או השם — פעם אחת; null — אין. */
        String startTarget();

        // --- מבנה 15: מה שהגשר מבקש מהמסך — ההודעות, הבוררים, הטופס, ההרצה, השליחה והצבעים. נקראות על חוט המסך (חוץ מ־colors). ---

        /** הודעה קצרה. */
        void toast(String text);

        /** להריץ קובץ פקודות במסך ההרצה: תצוגה מקדימה, אישור ודוח. here — התיקייה של %כאן% (null — של הקובץ). */
        void run(File f, File here);

        /** בורר התיקיות; התשובה — המסלול. */
        void pickFolder(String title, boolean canCreate, Picked done);

        /** קובץ: קודם התיקייה (הבורר הרגיל), ואז הקבצים שבה; התשובה — המסלול. */
        void pickFile(String title, Picked done);

        /** בורר הרשימות של המסד. */
        void pickList(String title, ListPicked done);

        /** הטופס המוכר של רשימה חכמה, עם מה שכבר כתוב. */
        void smartForm(String name, String cond, String sort, SmartPicked done);

        /** שליחת קובץ (חלון השיתוף). */
        void send(File f);

        /** הצבעים של התוכנה: שם ← צבע. */
        Map<String, Integer> colors();
    }

    interface Picked {
        void ok(String path);
    }

    interface ListPicked {
        void ok(String mode, List<String> path);
    }

    interface SmartPicked {
        void ok(String name, String cond, String sort, String view);
    }

    /** תשובת saveCommandFile כשיש כבר קובץ — העמוד שואל: להחליף או שם אחר. */
    /**
     * מבנה 141: מספר הגרסה של הדלת — של רשימת הפונקציות שתוסף יכול לפנות אליהן (info.door). עולה כשפונקציה יוצאת מהדלת או
     * משנה את מה שהיא עושה; פונקציה שנוספת — אינה מעלה אותו (תוסף ישן אינו נפגע), והתוסף שואל עליה ב־info.features.
     * שומר בבדיקות: הרשימה כתובה שם, שם אחרי שם; שינוי בה בלי לעדכן את הבדיקה — הבנייה נעצרת.
     */
    static final int DOOR = 1;

    static final String EXISTS = "exists";
    private static final String PREFS = "lab";

    private final Activity screen;
    private final Host host;
    /** התוסף, והעמוד שנפתח (גרסה אחת שלו). */
    private final String exp;
    private final Lab.Page page;
    private final Map<String, String> vars;

    Bridge(Activity screen, Host host, String exp, Lab.Page page, Map<String, String> vars) {
        this.screen = screen;
        this.host = host;
        this.exp = exp;
        this.page = page;
        this.vars = vars;
    }

    // ---------- מידע — מה התוכנה יודעת, ומה יש בה (אינן משנות דבר) ----------

    @JavascriptInterface
    public String info() {
        try {
            JSONObject o = new JSONObject();
            o.put("app", screen.getPackageName());
            android.content.pm.PackageInfo pi = screen.getPackageManager().getPackageInfo(screen.getPackageName(), 0);
            o.put("version", pi.versionName);
            o.put("mode", String.valueOf(Db.currentMode));
            o.put("lab", exp);
            o.put("labVersion", page == null ? "" : page.ver);
            String h = page == null ? null : Lab.higher(Lab.root(), page);   // מ0.40: יש גרסה גבוהה יותר בתיקייה
            o.put("labNewer", h == null ? "" : h);
            // מ0.42 — לתוסף האבחון: המכשיר, ההרשאה, המקום, היומנים והאינדקס (מה שהיה בראש דוח האבחון ובהגדרות)
            o.put("versionCode", pi.versionCode);
            o.put("registry", Manager.registryLine());                                  // מבנה 142: מה התוכנה מכירה, בשורה
            o.put("door", DOOR);                                                        // מבנה 141
            o.put("features", "steady dropclean zip set cmdfolders design");                         // מבנה 50: מה התוכנה יודעת (לתוסף — במקום לנחש לפי מספר הגרסה)
            // מבנה 59: איפה תיקיות הגיבויים של האופנים, בכתיב של קובץ פקודות — לבדיקה "אופן שבוטל וחוזר" (שם התיקייה שונה בין הבניות)
            o.put("backupsVar", "%" + Drive.INTERNAL + "%\\" + Paths.DB_DIR + "\\" + Backups.DIR);
            o.put("device", Log.device());
            o.put("access", Files.hasAccess(screen));
            o.put("freeMb", android.os.Environment.getExternalStorageDirectory().getUsableSpace() / (1024 * 1024));
            o.put("log", Log.file().getAbsolutePath());
            o.put("trial", Trial.file().getAbsolutePath());
            o.put("savedMode", Db.savedMode(screen));
            Library lib = Library.get(screen);
            o.put("indexReady", lib.ready);
            // מ0.43: מה שהיה בראש ההגדרות — האינדקס לכל כונן, ומה הוא עושה עכשיו
            JSONArray ix = new JSONArray();
            if (lib.ready)
                for (Index x : lib.indexes) {
                    JSONObject d = new JSONObject();
                    d.put("label", x.label);
                    d.put("files", x.fileCount());
                    ix.put(d);
                }
            o.put("indexes", ix);
            o.put("totalFiles", lib.ready ? lib.totalFiles() : -1);
            o.put("totalDirs", lib.ready ? lib.totalDirs() : -1);
            o.put("lastBuilt", lib.lastBuilt());
            o.put("indexStatus", lib.status);
            o.put("lastFullBuild", lib.lastFullBuild());
            o.put("lastDaily", lib.lastDaily());
            o.put("pointsSupported", lib.pointsSupported());
            o.put("readers", Library.readers());                            // מ0.45 (ב9): מה שהיה "בניית האינדקס — במקביל" בהגדרות
            o.put("lastBuild", lib.lastBuild());
            o.put("lastBuildDrives", lib.lastBuildDrives());
            return o.toString();
        } catch (Throwable t) {
            return "{}";
        }
    }

    /**
     * הצבעים של התוכנה (Ui) — שהעמוד ייראה כמו התוכנה, ולא כמו סקיצה. המשתמש: "האם העיצוב יהיה כמו של התוכנה, או כמו
     * שהיה פה? כי חלק מההערות היו קשורות לעיצוב".
     */
    @JavascriptInterface
    public String theme() {
        try {
            JSONObject o = new JSONObject();
            for (Map.Entry<String, Integer> e : host.colors().entrySet()) o.put(e.getKey(), hex(e.getValue()));
            return o.toString();
        } catch (Throwable t) {
            return "{}";
        }
    }

    /**
     * מבנה 243 (תוסף העיצוב — הלוח; המשתמש, 17.11.2026: "מקום מרוכז שבו משנים את כל הפרמטרים האלה — כמו לוח עם כפתורים
     * ובוררים"): מה שנקבע ומה שנראה — לכל התוכנה (what = "תוכנה"), או לפי הדרך (target), שמתחילה באופן (מבנה 246): "<אופן>"
     * — האופן (הבית); "<אופן>\<רשימה>…" — רשימה. what ו־target ריקים — מה שהמסך ביקש כשפתח את הלוח. הקריאה — במקום אחד (ShapeCmds.state, כמו "ודא עיצוב"); ועם זה — מה שהלוח
     * צריך כדי לכתוב את השורה של "עצב": המילים, המתגים, הטווחים, העיצובים, הגופנים והצבעים. אינה משנה דבר.
     */
    @JavascriptInterface
    public String design(String what, String target) {
        try {
            String w = what == null ? "" : what.trim(), t = target == null ? "" : target.trim();
            if (w.isEmpty()) {                                                   // מבנה 245: מה שהמסך ביקש כשפתח את הלוח
                String st = host.startTarget();
                int tab = st == null ? -1 : st.indexOf('\t');
                if (tab > 0) {
                    w = st.substring(0, tab);
                    t = st.substring(tab + 1);
                }
            }
            // מבנה 246 (המשתמש: "עצב אופן/רשימה/תת רשימה — אופן הוא הבית"): target — הדרך, מהאופן ("" — האופן שעל המסך)
            boolean program = w.equals(Verbs.PROGRAM_WORD) || (w.isEmpty() && t.isEmpty());
            java.util.List<String> way = Names.parsePath(t);
            if (!program && way.isEmpty()) way.add(String.valueOf(Db.currentMode));
            String kind = program ? Verbs.K_PROGRAM : way.size() > 1 ? Verbs.K_LIST : Verbs.K_MODE;
            String m = program ? String.valueOf(Db.currentMode) : way.get(0);
            String in = way.size() > 1 ? Names.pathText(way.subList(1, way.size())) : m;
            ShapeCmds.State st = ShapeCmds.state(kind, in, m);
            JSONObject o = new JSONObject();
            o.put("what", program ? Verbs.PROGRAM_WORD : kind.equals(Verbs.K_LIST) ? Verbs.LIST : Verbs.MODE_WORD);
            o.put("path", program ? "" : Names.pathText(way));                  // כמו שנכתב בפקודה: "<אופן>\<רשימה>…"
            o.put("mode", m);
            o.put("current", String.valueOf(Db.currentMode));
            o.put("error", st.error == null ? "" : st.error);
            if (st.error == null) {
                JSONObject own = new JSONObject();
                own.put("design", st.own);
                JSONObject look = new JSONObject();
                for (Map.Entry<String, String> e : Design.lookMap(st.look).entrySet()) look.put(e.getKey(), e.getValue());
                own.put("look", look);
                o.put("own", own);
                o.put("shown", designJson(st.shown, st.set, st.looks));
                o.put("above", designJson(st.above, st.setAbove, st.aboveLooks));
            }
            o.put("info", designInfo());
            return o.toString();
        } catch (Throwable e) {
            Log.e("Lab.design", e);
            return "{}";
        }
    }

    /** עיצוב אחד, כמו שהוא נראה: הצורה, ארבעת הכתבים, והצבעים (לפי המראות). */
    private static JSONObject designJson(Design d, boolean set, String[] looks) throws org.json.JSONException {
        JSONObject o = new JSONObject();
        o.put("set", set);
        o.put("design", d.name);
        o.put("grid", d.grid);
        o.put("cols", d.cols);
        o.put("gap", d.gap);
        o.put("icon", d.icon);
        o.put("showIcon", d.showIcon);
        o.put("around", d.around);
        o.put("showData", d.showData);
        o.put("dataBelow", d.dataBelow);
        JSONObject texts = new JSONObject();
        texts.put(Design.T_TITLE, textJson(d.title));
        texts.put(Design.T_NAME, textJson(d.nameT));
        texts.put(Design.T_DATA, textJson(d.data));
        texts.put(Design.T_GROUP, textJson(d.groupText()));
        texts.put(Design.T_TAG, textJson(d.tagText()));                         // מבנה 249
        o.put("tagOwn", d.tag != null);                                          // בלי — לפי גודל הצלמית
        o.put("texts", texts);
        o.put("groupOwn", d.group != null);                                     // בלי — כותרת הקבוצה הולכת אחרי השם
        if (looks == null) return o;                                            // עיצוב מוכן — בלי הצבעים (הם של התוכנה והאופן)
        o.put("dark", Palette.dark(looks));
        JSONObject colors = new JSONObject();
        int[] c = Palette.resolve(looks);
        for (int i = 0; i < Palette.ROLES.length; i++) colors.put(Palette.ROLES[i][0], Palette.hex(c[i]));
        o.put("colors", colors);
        return o;
    }

    private static JSONObject textJson(Design.Text t) throws org.json.JSONException {
        JSONObject o = new JSONObject();
        o.put("size", t.size == (int) t.size ? String.valueOf((int) t.size) : String.valueOf(t.size));
        o.put("weight", t.weight);
        o.put("weightName", Design.weightName(t.weight));
        o.put("font", t.font);
        return o;
    }

    /** מה שהלוח צריך כדי לכתוב את השורה — מהמקום שבו זה נקבע (Verbs, Design, Palette), ולא מועתק לעמוד. */
    private static JSONObject designInfo() throws org.json.JSONException {
        JSONObject o = new JSONObject();
        o.put("verb", Verbs.SHAPE);
        o.put("list", Verbs.LIST);
        o.put("modeWord", Verbs.MODE_WORD);
        o.put("program", Verbs.PROGRAM_WORD);
        JSONObject sw = new JSONObject();
        sw.put("design", Verbs.SW_DESIGN);
        sw.put("cols", Verbs.SW_COLS);
        sw.put("gap", Verbs.SW_GAP);
        sw.put("icon", Verbs.SW_ICON_SIZE);
        sw.put(Design.T_TITLE, Verbs.SW_T_TITLE);
        sw.put(Design.T_NAME, Verbs.SW_T_NAME);
        sw.put(Design.T_DATA, Verbs.SW_T_DATA);
        sw.put(Design.T_GROUP, Verbs.SW_T_GROUP);
        sw.put(Design.T_TAG, Verbs.SW_T_TAG);
        sw.put("look", Verbs.SW_MODE_LOOK);
        sw.put("color", Verbs.SW_COLOR);
        o.put("switches", sw);
        JSONObject keys = new JSONObject();
        keys.put("cols", Design.K_COLS);
        keys.put("gap", Design.K_GAP);
        keys.put("icon", Design.K_ICON);
        keys.put("size", Design.K_SIZE);
        keys.put("weight", Design.K_WEIGHT);
        keys.put("font", Design.K_FONT);
        keys.put("look", Palette.K_MODE);
        keys.put("color", Palette.K_COLOR);
        keys.put("light", Palette.LIGHT);
        keys.put("dark", Palette.DARK);
        keys.put("system", Design.SYSTEM);
        o.put("keys", keys);
        o.put("texts", new JSONArray(java.util.Arrays.asList(Design.T_TITLE, Design.T_NAME, Design.T_DATA, Design.T_GROUP, Design.T_TAG)));
        o.put("weights", new JSONArray(java.util.Arrays.asList(Design.W_LIGHT, Design.W_REG, Design.W_MED, Design.W_BOLD)));
        JSONObject ranges = new JSONObject();
        ranges.put("cols", new JSONArray(java.util.Arrays.asList(Design.COLS[0], Design.COLS[1])));
        ranges.put("gap", new JSONArray(java.util.Arrays.asList(Design.GAP[0], Design.GAP[1])));
        ranges.put("icon", new JSONArray(java.util.Arrays.asList(Design.ICON[0], Design.ICON[1])));
        ranges.put("size", new JSONArray(java.util.Arrays.asList(Design.SIZE[0], Design.SIZE[1])));
        ranges.put("weight", new JSONArray(java.util.Arrays.asList(Design.WEIGHT[0], Design.WEIGHT[1])));
        o.put("ranges", ranges);
        o.put("designs", new JSONArray(Design.names()));
        JSONObject presets = new JSONObject();                                  // לתצוגה המקדימה — כל עיצוב מוכן, כמו שהוא
        for (String n : Design.names()) presets.put(n, designJson(Design.get(n), true, null));
        o.put("presets", presets);
        JSONObject wv = new JSONObject();
        for (String w : new String[]{Design.W_LIGHT, Design.W_REG, Design.W_MED, Design.W_BOLD}) wv.put(w, Design.weight(w));
        o.put("weightValues", wv);
        o.put("fonts", new JSONArray(Design.fonts()));
        JSONArray roles = new JSONArray();
        for (String[] r : Palette.ROLES) roles.put(new JSONArray(java.util.Arrays.asList(r)));
        o.put("roles", roles);
        o.put("groups", new JSONArray(java.util.Arrays.asList(Palette.GROUPS)));
        return o;
    }

    /** המקומות המפורסמים: [{name, path}] (הורדות, פנימי, כרטיס…). */
    @JavascriptInterface
    public String places() {
        JSONArray a = new JSONArray();
        try {
            String[] keys = {Syntax.V_DOWNLOADS, Syntax.V_INTERNAL, Syntax.V_CARD};
            for (String k : keys) {
                String v = vars.get(k);
                if (v == null) continue;
                JSONObject o = new JSONObject();
                o.put("name", k);
                o.put("path", v);
                a.put(o);
            }
        } catch (Throwable t) {
            Log.e("Lab.places", t);
        }
        return a.toString();
    }

    /** מ0.40: אופני העבודה שיש להם מסד (לפקודה "אופן"). */
    @JavascriptInterface
    public String modes() {
        JSONArray a = new JSONArray();
        try {
            for (String m : Db.dbModes(screen)) a.put(m);
        } catch (Throwable t) {
            Log.e("Lab.modes", t);
        }
        return a.toString();
    }

    /** מ0.42: התיקייה של התוסף לקבצים שהוא כותב (קובצי עזר לבדיקות, תוצאות, דוחות). */
    @JavascriptInterface
    public String dataDir() {
        return Lab.dataDir(exp).getAbsolutePath();
    }

    /** מ0.45+: קובץ הפקודות לפתוח בעורך (מרשימה, מהפס הצף) — פעם אחת; null — אין. */
    @JavascriptInterface
    public String startFile() {
        return host.startFile();
    }

    /**
     * מ0.43: המסד של כל אופן — מה שהיה בחלק 2 של דוח האבחון: [{mode, kb, ok, error, lists, smarts, loose, home, backups, lastBackup}].
     * קריאה בלבד (בלי בדיקת תקינות חוזרת למסד שלא השתנה — SqlStore).
     */
    @JavascriptInterface
    public String dbStats() {
        JSONArray a = new JSONArray();
        for (String m : Db.dbModes(screen)) {
            JSONObject o = new JSONObject();
            SqlStore s = null;
            try {
                File f = Db.dbFile(screen, m);
                o.put("mode", m);
                o.put("kb", f.length() / 1024);
                try {
                    s = Db.openRead(screen, m);
                    long home = DbPaths.home(s);
                    o.put("ok", true);
                    o.put("lists", s.lists().size());
                    o.put("smarts", s.smarts().size());
                    o.put("loose", s.unassigned().size());
                    o.put("home", home == 0 ? -1 : s.members(home).size());
                } catch (Throwable t) {
                    o.put("ok", false);
                    o.put("error", String.valueOf(t.getMessage()));
                    Log.e("Lab.dbStats " + m, t);
                }
                List<Backups.Entry> bk = Db.backupsOf(screen, m).list();
                o.put("backups", bk.size());
                o.put("lastBackup", bk.isEmpty() ? "" : bk.get(0).name);
            } catch (Throwable t) {
                Log.e("Lab.dbStats", t);
            } finally {
                if (s != null) s.close();
            }
            a.put(o);
        }
        return a.toString();
    }

    /** מ0.42: מה שהיה "בדיקת מסלול באינדקס" בהגדרות: {disk, dir, index, size}. קריאה בלבד. */
    @JavascriptInterface
    public String pathInfo(String path) {
        try {
            File f = resolve(path);
            JSONObject o = new JSONObject();
            o.put("path", f == null ? "" : f.getAbsolutePath());
            o.put("disk", f != null && f.exists());
            o.put("dir", f != null && f.isDirectory());
            o.put("size", f == null ? -1 : f.length());
            o.put("index", f != null && Library.get(screen).find(f.getAbsolutePath()) != null);
            return o.toString();
        } catch (Throwable t) {
            Log.e("Lab.pathInfo", t);
            return "{}";
        }
    }

    /**
     * מ0.45: "כל הפקודות" — קובץ פקודות שנבנה מההגדרה האחת (CmdMatrix): כל פקודה, ובכל אחת כל מתג. לפני — הגיבוי הקטן ליד
     * קובץ ההרצה (ל־/מצב:). null — לא הצליח (ביומן).
     */
    @JavascriptInterface
    public String commandMatrix() {
        try {
            CmdMatrix.fixtures(Lab.dataDir(exp), Library.get(screen));
            return CmdMatrix.text(true);
        } catch (Throwable t) {
            Log.e("Lab.commandMatrix", t);
            return null;
        }
    }

    /** מ0.40: שמות הרשימות החכמות של האופן (לבורר שבעורך): ["חדשים", "ספרים\\פדף"…]. */
    @JavascriptInterface
    public String smartLists() {
        JSONArray a = new JSONArray();
        try {
            List<String> sm = DbLists.names(screen).get("smart");   // מבנה 18: ישר מהחומר (עד כאן — דרך המסך)
            if (sm != null) for (String x : sm) a.put(x);
        } catch (Throwable t) {
            Log.e("Lab.smartLists", t);
        }
        return a.toString();
    }

    /** קובצי הפקודות שלי: [{name, path, time}]. */
    @JavascriptInterface
    public String commandFiles() {
        JSONArray a = new JSONArray();
        try {
            for (File f : Shortcuts.mine()) {
                JSONObject o = new JSONObject();
                o.put("name", f.getName());
                o.put("path", f.getAbsolutePath());
                o.put("time", f.lastModified());
                a.put(o);
            }
        } catch (Throwable t) {
            Log.e("Lab.commandFiles", t);
        }
        return a.toString();
    }

    /**
     * מ0.40: קובצי הפקודות כמו ב"הפקודות שלי" שבתפריט (מהמסד — גם מאופן אחר, כמו התפריט), ועוד מה שבתיקיית
     * הקיצורים: [{name, path, time}]. (commandFiles — רק התיקייה; ככה עמוד המעבדה לא ראה את הפקודות של המשתמש.)
     */
    @JavascriptInterface
    public String commandFolders() {
        try {
            java.util.Map<String, Integer> m = Manager.commandFolders(Library.get(screen));   // מבנה 151: דרך המנהל, מהאינדקס
            if (m == null) return null;
            JSONArray a = new JSONArray();
            for (Map.Entry<String, Integer> e : m.entrySet()) {
                JSONObject o = new JSONObject();
                o.put("path", e.getKey());
                o.put("name", new File(e.getKey()).getName());
                o.put("count", e.getValue());
                a.put(o);
            }
            return a.toString();
        } catch (Throwable t) {
            Log.e("Lab.commandFolders", t);
            return null;
        }
    }

    /** מ0.41: "הפקודות שלי" — מהתפריטים (הכללי ושל האופן), ומתיקיית הקיצורים. */
    @JavascriptInterface
    public String myCommands() {
        JSONArray a = new JSONArray();
        java.util.Set<String> seen = new java.util.HashSet<String>();
        try {
            for (Menus.Item it : Menus.get(screen).at(Menus.MINE))            // מ0.41: מהתפריטים — הכללי ושל האופן
                if (it.e.kind == DbMore.E_FILE && new File(it.e.path).isFile() && seen.add(it.e.path)) a.put(entry(new File(it.e.path)));
            for (File f : Shortcuts.mine()) if (seen.add(f.getAbsolutePath())) a.put(entry(f));
        } catch (Throwable t) {
            Log.e("Lab.myCommands", t);
        }
        return a.toString();
    }

    /** מ0.45+: הפקודות הנפוצות — של התוכנה ושלי (מה שהיה בעורך הישן): [{name, mine, lines}]. */
    @JavascriptInterface
    public String templates() {
        try {
            org.json.JSONArray a = new org.json.JSONArray();
            for (CmdTemplates.Tpl t : CmdTemplates.system(screen)) a.put(tpl(t, false));
            for (CmdTemplates.Tpl t : CmdTemplates.mine()) a.put(tpl(t, true));
            return a.toString();
        } catch (Throwable t) {
            Log.e("Lab.templates", t);
            return "[]";
        }
    }

    private org.json.JSONObject tpl(CmdTemplates.Tpl t, boolean mine) throws org.json.JSONException {
        org.json.JSONObject o = new org.json.JSONObject();
        o.put("name", t.name);
        o.put("mine", mine);
        org.json.JSONArray l = new org.json.JSONArray();
        for (String x : t.lines) l.put(x);
        o.put("lines", l);
        return o;
    }

    // ---------- קריאה — קבצים ומסלולים ----------

    /** קריאה בלבד: תוכן קובץ (עד 2MB), או null. */
    @JavascriptInterface
    public String readText(String path) {
        try {
            return Manager.pluginRead(resolve(path));                           // מבנה 138: דרך המנהל
        } catch (Throwable t) {
            return null;
        }
    }

    /** קריאה בלבד: מה בתיקייה — [{name, dir, size, time}], או null. */
    @JavascriptInterface
    public String listDir(String path) {
        try {
            File[] fs = Manager.pluginList(resolve(path));                      // מבנה 138: דרך המנהל
            if (fs == null) return null;
            JSONArray a = new JSONArray();
            for (File f : fs) {
                JSONObject o = new JSONObject();
                o.put("name", f.getName());
                o.put("dir", f.isDirectory());
                o.put("size", f.length());
                o.put("time", f.lastModified());
                a.put(o);
            }
            return a.toString();
        } catch (Throwable t) {
            return null;
        }
    }

    /** מסלול מלא ← הצורה שבקובצי הפקודות (%הורדות%\…). */
    @JavascriptInterface
    public String toVar(String abs) {
        return Parse.toVar(abs == null ? "" : abs, vars);
    }

    /** הצורה שבקובצי הפקודות ← מסלול מלא (null — משתנה לא מוכר). */
    @JavascriptInterface
    public String expand(String raw) {
        File f = resolve(raw);
        return f == null ? null : f.getAbsolutePath();
    }

    // ---------- כתיבה — קובץ של המשתמש דרך המנהל ("כתוב"); האזור של התוכנה ושל התוסף — ישיר (מבנה 132) ----------

    /**
     * מ0.40 — שמירת קובץ פקודות. path — הקובץ שנפתח (גם מחוץ לתיקיית הקיצורים), או שם — קובץ חדש בתיקיית הקיצורים
     * (ונכנס ל"הפקודות שלי"). קובץ שכבר קיים — רק עם replace; בלי — חוזר "קיים", והעמוד שואל. המשתמש: "שתודיע שיש קובץ
     * בשם הזה, ואני אחליט אם למחוק או לשנות שם." "" — נשמר; אחרת השגיאה.
     */
    @JavascriptInterface
    public String saveCommandFile(String pathOrName, String text, boolean replace) {
        try {
            String raw = pathOrName == null ? "" : pathOrName.trim();
            File f;
            if (raw.startsWith("/")) f = new File(raw);
            else {
                String n = Lab.safe(new File(raw).getName());
                if (n.isEmpty()) return Words.Lab.ONLY_COMMANDS;
                if (!Batch.isBatch(n)) n = n + "." + Batch.EXT;
                f = new File(Shortcuts.dir(), n);
            }
            if (!Batch.isBatch(f.getName())) return Words.Lab.ONLY_COMMANDS;
            boolean was = f.isFile();
            if (was && !replace) return EXISTS;
            // מבנה 132: קובץ בתיקיות של המשתמש — דרך המנהל, כפקודה "כתוב": התיקייה של המסד וקובצי התוכנה מסורבים (ממצא 41),
            // והקובץ הקודם עובר לסל (הוראות, סעיף 4). בתוך תיקיית התוכנה (הקיצורים, פקודות המערכת — האזור שהתוכנה מנהלת,
            // ו"כתוב" אינה כותבת בו) — כמו שהיה: רק קובץ פקודות, נכתב ישר.
            String root = Paths.clean(Paths.data().getAbsolutePath()), at = Paths.clean(f.getAbsolutePath());
            if (at.startsWith(root + "/")) {                                    // מבנה 139: גם זה — דרך המנהל
                String no = Manager.ownSaveKeepOld(Paths.data(), f, text, Library.get(screen));   // מבנה 154: הקודם — לסל
                if (no != null) return no;
            } else {
                String no = Manager.saveFile(f, text, was, Library.get(screen));
                if (no != null) return no;
            }
            if (!was) {
                Library.get(screen).fileAdded(f);
                Menus.addToMine(f);
            }
            return "";
        } catch (Throwable t) {
            Log.e("Lab.saveCommandFile", t);
            return Words.Db.failed(String.valueOf(t.getMessage()));
        }
    }

    /** הישנה (עמודים עד "עורך 3"): בלי להחליף קובץ קיים — כמו saveCommandFile בלי replace. */
    @JavascriptInterface
    public String writeCommandFile(String name, String text) {
        String r = saveCommandFile(name, text, false);
        return EXISTS.equals(r) ? Words.Lab.EXISTS_OLD : r;
    }

    /**
     * מבנה 230 (המשתמש, 16.11.2026: "מחיקת קובץ פקודות מתוך העורך" · "גם אם הוא בתוך ספריית התוכנה"): מחיקה של קובץ פקודות —
     * דרך המנהל, כפקודה "מחק": לסל, ואפשר להחזיר. רק קובץ פקודות, במסלול מלא. "" — עבר לסל; אחרת — למה לא.
     */
    @JavascriptInterface
    public String deleteCommandFile(String path) {
        try {
            String raw = path == null ? "" : path.trim();
            File f = new File(raw);
            if (!raw.startsWith("/") || !Batch.isBatch(f.getName())) return Words.Lab.ONLY_COMMANDS_DEL;
            String no = Manager.trashFile(f, Library.get(screen));
            return no == null ? "" : no;
        } catch (Throwable t) {
            Log.e("Lab.deleteCommandFile", t);
            return Words.Db.failed(String.valueOf(t.getMessage()));
        }
    }

    /** מ0.45+: לשמור כפקודה נפוצה שלי. */
    @JavascriptInterface
    public boolean saveTemplate(String name, String text) {
        if (name == null || name.trim().isEmpty() || text == null) return false;
        return CmdTemplates.save(name.trim(), java.util.Arrays.asList(text.replace("\r", "").split("\n"))) != null;
    }

    /**
     * מ0.42: כתיבת קובץ טקסט — בתיקייה של התוסף, או (downloads) בתיקיית ההורדות. רק שם: לא כתיבה לכל מקום. מחזיר את המסלול
     * המלא, או null (והתקלה — ביומן).
     */
    @JavascriptInterface
    public String saveText(String name, String text, boolean downloads) {
        try {
            String n = Lab.safe(new File(name == null ? "" : name).getName());
            if (n.isEmpty()) return null;
            File dir = downloads ? android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                    : Lab.dataDir(exp);
            File f = new File(dir, n);
            boolean was = f.isFile();
            Library lib = Library.get(screen);
            if (downloads) {                                                    // מבנה 132: תיקייה של המשתמש — דרך המנהל, כפקודה "כתוב" (הקודם לסל)
                String no = Manager.saveFile(f, text, was, lib);
                if (no != null) {
                    Log.i("Lab.saveText: " + no);
                    return null;
                }
                return f.getAbsolutePath();
            }
            String no = Manager.ownSave(dir, f, text);                          // מבנה 139: התיקייה של התוסף עצמו — דרך המנהל
            if (no != null) {
                Log.i("Lab.saveText: " + no);
                return null;
            }
            if (was) lib.fileRemoved(f.getAbsolutePath());
            lib.fileAdded(f);
            return f.getAbsolutePath();
        } catch (Throwable t) {
            Log.e("Lab.saveText", t);
            return null;
        }
    }

    /**
     * מ0.45: מחיקת הקבצים שבתת־תיקייה של תיקיית התוסף (רק שם, ורק קבצים): התוצאות של הרצות ישנות. מחזיר כמה נמחקו; -1 — שם
     * לא תקין.
     */
    @JavascriptInterface
    public int cleanData(String sub) {
        String n = Lab.safe(sub == null ? "" : sub);
        if (n.isEmpty() || !n.equals(sub) || n.contains("/") || n.startsWith(".")) return -1;
        File d = new File(Lab.dataDir(exp), n);
        Library lib = Library.get(screen);
        java.util.List<File> gone = Manager.ownClean(Lab.dataDir(exp), d);     // מבנה 139: דרך המנהל
        for (File f : gone) lib.fileRemoved(f.getAbsolutePath());
        return gone.size();
    }

    /** מ0.45 (ב9): כמה קוראים לכל כונן בבנייה — מה שהיה בהגדרות; התוסף מודד וקובע. 1 עד 16. */
    @JavascriptInterface
    public boolean setReaders(int n) {
        if (n < 1 || n > 16) return false;
        String no = Manager.setting(Conf.READERS, String.valueOf(n));               // מבנה 127: דרך המנהל, כפקודה "הגדר"
        Log.i("קוראים לכל כונן: " + n + " (מתוסף האבחון)" + (no == null ? "" : " — " + no));
        return no == null;
    }

    /** זיכרון קטן לכל ניסוי (נשאר בין פתיחות). */
    @JavascriptInterface
    public String load(String key) {
        return prefs().getString(exp + "\u0001" + key, null);
    }

    @JavascriptInterface
    public void save(String key, String value) {
        prefs().edit().putString(exp + "\u0001" + key, value).apply();
    }

    // ---------- הרצה — תמיד דרך המסך המארח והמנהל ----------

    /** הרצה דרך ההרצה הרגילה — תצוגה מקדימה ואישור, כמו כל קובץ פקודות. */
    @JavascriptInterface
    public void runCommands(final String text) {
        screen.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    File dir = new File(screen.getCacheDir(), "lab");
                    File f = new File(dir, Words.Lab.RUN_FILE + "." + Batch.EXT);
                    // מ0.40 — המשתמש: "למה ההעברה בוצעה בפועל? הרי זה אמור לעבוד רק בכאילו." מהמעבדה — תמיד הדמיה:
                    // שורת "הדמיה" בראש, ומסך ההרצה מראה מה היה קורה, בלי כפתור הרצה. שום דבר בכונן או במסד לא משתנה.
                    String no = Manager.ownSave(dir, f, Syntax.AS_IF2 + "\n" + (text == null ? "" : text));   // מבנה 139
                    if (no != null) { host.toast(no); return; }
                    // מ0.39: תמיד במסך ההרצה — תצוגה מקדימה, אישור, ודוח (בהרצה הרגילה, בלי "בדיקה", זה רץ מיד ונראית רק
                    // הודעה קצרה; המשתמש: "איפה רואים את התוצאה של הפעולה?")
                    host.run(f, null);
                } catch (Throwable t) {
                    Log.e("Lab.runCommands", t);
                    host.toast(Words.Db.failed(String.valueOf(t.getMessage())));
                }
            }
        });
    }

    /**
     * מ0.40 — הרצה מהעורך. המשתמש: "ריצה אמיתית או בכאילו" (מתג בעמוד). בכאילו — שורת "הדמיה" בראש; אמיתית — כמו כל קובץ
     * פקודות: תצוגה מקדימה, ורק באישור (כלל ההקפדה: שינוי בכונן — תצוגה מקדימה ואישור). where — הקובץ שבעורך: שמו בכותרת
     * מסך ההרצה (המשתמש: "הכותרת 'הרצה מהמעבדה' — לשים את שם הקובץ"), והתיקייה שלו היא %כאן% (ושל כלול).
     */
    @JavascriptInterface
    public void runFile(final String text, final String where, final boolean real) {
        screen.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    File dir = new File(screen.getCacheDir(), "lab");
                    String w = where == null ? "" : where.trim();
                    String name = Lab.safe(new File(w).getName());
                    if (name.isEmpty()) name = Words.Lab.RUN_FILE + "." + Batch.EXT;
                    if (!Batch.isBatch(name)) name = name + "." + Batch.EXT;
                    File f = new File(dir, name);
                    String no = Manager.ownSave(dir, f, (real ? "" : Syntax.AS_IF2 + "\n") + (text == null ? "" : text));   // מבנה 139
                    if (no != null) { host.toast(no); return; }
                    File here = w.startsWith("/") ? new File(w).getParentFile() : Shortcuts.dir();
                    host.run(f, here);
                } catch (Throwable t) {
                    Log.e("Lab.runFile", t);
                    host.toast(Words.Db.failed(String.valueOf(t.getMessage())));
                }
            }
        });
    }

    /**
     * מבנה 243 (תוסף העיצוב): שורה אחת של "עצב" — דרך המנהל, ומיד (Manager.shape: אותו תכנון ואותן בדיקות כמו בקובץ פקודות;
     * כל שורה אחרת — מסורבת). "" — נעשה; אחרת — למה לא. המסך שמתחת נצבע מחדש כשחוזרים אליו.
     */
    @JavascriptInterface
    public String shape(String line) {
        try {
            String no = Manager.shape(line, Library.get(screen));
            return no == null ? "" : no;
        } catch (Throwable t) {
            Log.e("Lab.shape", t);
            return Words.Db.failed(String.valueOf(t.getMessage()));
        }
    }

    // ---------- חלונות ומסך — בוררים, טופס, הודעה, שליחה, סגירה ----------

    @JavascriptInterface
    public void pickFolder(final String id, final String title, final boolean canCreate) {
        screen.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                host.pickFolder(title == null ? Words.FolderP.PICK_FOLDER : title, canCreate, new Picked() {
                    @Override public void ok(String path) { host.result(id, path); }
                });
            }
        });
    }

    /** קובץ: קודם התיקייה (הבורר הרגיל), ואז הקבצים שבה. */
    @JavascriptInterface
    public void pickFile(final String id, final String title) {
        screen.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                host.pickFile(title == null ? Words.Lab.PICK_FILE : title, new Picked() {
                    @Override public void ok(String path) { host.result(id, path); }
                });
            }
        });
    }

    /** רשימה מהמסד: {mode, path} — path בשמות, מופרד ב־\. */
    @JavascriptInterface
    public void pickList(final String id, final String title) {
        screen.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                host.pickList(title == null ? Words.NEW_LIST : title, new ListPicked() {
                    @Override
                    public void ok(String mode, List<String> path) {
                        try {
                            JSONObject o = new JSONObject();
                            o.put("mode", mode);
                            StringBuilder sb = new StringBuilder();
                            for (String p : path) sb.append(sb.length() > 0 ? "\\" : "").append(p);
                            o.put("path", sb.toString());
                            host.result(id, o.toString());
                        } catch (Throwable t) {
                            Log.e("Lab.pickList", t);
                        }
                    }
                });
            }
        });
    }

    /**
     * מ0.41: הטופס המוכר של רשימה חכמה — עם מה שכתוב בשורה שבעורך; חוזר ב־vfmResult: {name, cond, sort, view}.
     * (העורך כותב לשורה שם, תנאי ומיון — מה שהפקודה נושאת.)
     */
    @JavascriptInterface
    public void smartForm(final String id, final String name, final String cond, final String sort) {
        screen.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    host.smartForm(name, cond, sort, new SmartPicked() {
                        @Override
                        public void ok(String rName, String rCond, String rSort, String rView) {
                            try {
                                JSONObject o = new JSONObject();
                                o.put("name", rName);
                                o.put("cond", rCond);
                                o.put("sort", rSort);
                                o.put("view", rView);
                                host.result(id, o.toString());
                            } catch (Throwable t) {
                                Log.e("Lab.smartForm.result", t);
                            }
                        }
                    });
                } catch (Throwable t) {
                    Log.e("Lab.smartForm", t);
                    host.toast(Words.Db.failed(String.valueOf(t.getMessage())));
                }
            }
        });
    }

    /** מ0.42: שליחת קובץ (דוח, יומן) — כמו "שליחה" של דוח האבחון. */
    @JavascriptInterface
    public void send(final String path) {
        screen.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                File f = resolve(path);
                if (f == null || !f.isFile()) {
                    host.toast(Words.Verb.NOT_FOUND + path);
                    return;
                }
                host.send(f);
            }
        });
    }

    @JavascriptInterface
    public void toast(final String s) {
        screen.runOnUiThread(new Runnable() {
            @Override public void run() { host.toast(s); }
        });
    }

    @JavascriptInterface
    public void log(String s) {
        Log.i("תוסף " + exp + ": " + s);   // בכונן
    }

    @JavascriptInterface
    public void close() {
        screen.runOnUiThread(new Runnable() {
            @Override public void run() { screen.finish(); }
        });
    }


    private File resolve(String raw) {
        if (raw == null || raw.trim().isEmpty()) return Shortcuts.dir();       // מבנה 41: לתוסף, ריק = תיקיית הפקודות (כמו שהיה); בקובץ פקודות — שגיאה
        String[] err = {null};
        String p = Parse.path(raw, Shortcuts.dir(), vars, err);
        return p == null ? null : new File(p);
    }

    private static JSONObject entry(File f) throws org.json.JSONException {
        JSONObject o = new JSONObject();
        o.put("name", f.getName());
        o.put("path", f.getAbsolutePath());
        o.put("time", f.lastModified());
        return o;
    }

    private static String hex(int c) {
        return String.format(java.util.Locale.ROOT, "#%06X", c & 0xFFFFFF);
    }

    /** הזיכרון הקטן של התוספים (וגם: אילו תוספים מההתקנה כבר נכנסו — LabScreen). */
    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private SharedPreferences prefs() {
        return prefs(screen);
    }
}
