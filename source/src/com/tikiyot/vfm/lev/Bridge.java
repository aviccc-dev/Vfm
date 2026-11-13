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

        // --- מבנה 15: מה שהגשר מבקש מהמסך — ההודעות, הבוררים, הטופס, ההרצה, השליחה והצבעים. נקראות על חוט המסך (חוץ מ־colors ו־smartLists). ---

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

        /** שמות הרשימות החכמות של האופן. */
        List<String> smartLists();
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

    @JavascriptInterface
    public String info() {
        try {
            JSONObject o = new JSONObject();
            o.put("app", screen.getPackageName());
            o.put("version", screen.getPackageManager().getPackageInfo(screen.getPackageName(), 0).versionName);
            o.put("mode", String.valueOf(Db.currentMode));
            o.put("lab", exp);
            o.put("labVersion", page == null ? "" : page.ver);
            String h = page == null ? null : Lab.higher(Lab.root(), page);   // מ0.40: יש גרסה גבוהה יותר בתיקייה
            o.put("labNewer", h == null ? "" : h);
            // מ0.42 — לתוסף האבחון: המכשיר, ההרשאה, המקום, היומנים והאינדקס (מה שהיה בראש דוח האבחון ובהגדרות)
            o.put("versionCode", screen.getPackageManager().getPackageInfo(screen.getPackageName(), 0).versionCode);
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

    /** המקומות המפורסמים: [{name, path}] (הורדות, פנימי, כרטיס…). */
    @JavascriptInterface
    public String places() {
        JSONArray a = new JSONArray();
        try {
            String[] keys = {Batch.V_DOWNLOADS, Batch.V_INTERNAL, Batch.V_CARD};
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

    /** מסלול מלא ← הצורה שבקובצי הפקודות (%הורדות%\…). */
    @JavascriptInterface
    public String toVar(String abs) {
        return Batch.toVar(abs == null ? "" : abs, vars);
    }

    /** הצורה שבקובצי הפקודות ← מסלול מלא (null — משתנה לא מוכר). */
    @JavascriptInterface
    public String expand(String raw) {
        File f = resolve(raw);
        return f == null ? null : f.getAbsolutePath();
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

    /** קריאה בלבד: תוכן קובץ (עד 2MB), או null. */
    @JavascriptInterface
    public String readText(String path) {
        try {
            File f = resolve(path);
            if (f == null || !f.isFile() || f.length() > 2L * 1024 * 1024) return null;
            return Files.readUtf8(f);
        } catch (Throwable t) {
            return null;
        }
    }

    /** קריאה בלבד: מה בתיקייה — [{name, dir, size, time}], או null. */
    @JavascriptInterface
    public String listDir(String path) {
        try {
            File d = resolve(path);
            File[] fs = d == null ? null : d.listFiles();
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

    /**
     * מ0.40: קובצי הפקודות כמו ב"הפקודות שלי" שבתפריט (מהמסד — גם מאופן אחר, כמו התפריט), ועוד מה שבתיקיית
     * הקיצורים: [{name, path, time}]. (commandFiles — רק התיקייה; ככה עמוד המעבדה לא ראה את הפקודות של המשתמש.)
     */
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
            Files.writeAtomic(f, text == null ? "" : text);
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

    /** הרצה דרך ההרצה הרגילה — תצוגה מקדימה ואישור, כמו כל קובץ פקודות. */
    @JavascriptInterface
    public void runCommands(final String text) {
        screen.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    File dir = new File(screen.getCacheDir(), "lab");
                    dir.mkdirs();
                    File f = new File(dir, Words.Lab.RUN_FILE + "." + Batch.EXT);
                    // מ0.40 — המשתמש: "למה ההעברה בוצעה בפועל? הרי זה אמור לעבוד רק בכאילו." מהמעבדה — תמיד הדמיה:
                    // שורת "הדמיה" בראש, ומסך ההרצה מראה מה היה קורה, בלי כפתור הרצה. שום דבר בכונן או במסד לא משתנה.
                    Files.writeAtomic(f, Batch.AS_IF2 + "\n" + (text == null ? "" : text));
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
                    dir.mkdirs();
                    String w = where == null ? "" : where.trim();
                    String name = Lab.safe(new File(w).getName());
                    if (name.isEmpty()) name = Words.Lab.RUN_FILE + "." + Batch.EXT;
                    if (!Batch.isBatch(name)) name = name + "." + Batch.EXT;
                    File f = new File(dir, name);
                    Files.writeAtomic(f, (real ? "" : Batch.AS_IF2 + "\n") + (text == null ? "" : text));
                    File here = w.startsWith("/") ? new File(w).getParentFile() : Shortcuts.dir();
                    host.run(f, here);
                } catch (Throwable t) {
                    Log.e("Lab.runFile", t);
                    host.toast(Words.Db.failed(String.valueOf(t.getMessage())));
                }
            }
        });
    }

    /** מ0.40: שמות הרשימות החכמות של האופן (לבורר שבעורך): ["חדשים", "ספרים\\פדף"…]. */
    @JavascriptInterface
    public String smartLists() {
        JSONArray a = new JSONArray();
        try {
            List<String> sm = host.smartLists();
            if (sm != null) for (String x : sm) a.put(x);
        } catch (Throwable t) {
            Log.e("Lab.smartLists", t);
        }
        return a.toString();
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

    /** מ0.42: התיקייה של התוסף לקבצים שהוא כותב (קובצי עזר לבדיקות, תוצאות, דוחות). */
    @JavascriptInterface
    public String dataDir() {
        return Lab.dataDir(exp).getAbsolutePath();
    }

    /**
     * מ0.45: "כל הפקודות" — קובץ פקודות שנבנה מההגדרה האחת (CmdMatrix): כל פקודה, ובכל אחת כל מתג. לפני — הגיבוי הקטן ליד
     * קובץ ההרצה (ל־/מצב:). null — לא הצליח (ביומן).
     */
    /** מ0.45+: קובץ הפקודות לפתוח בעורך (מרשימה, מהפס הצף) — פעם אחת; null — אין. */
    @JavascriptInterface
    public String startFile() {
        return host.startFile();
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

    /** מ0.45+: לשמור כפקודה נפוצה שלי. */
    @JavascriptInterface
    public boolean saveTemplate(String name, String text) {
        if (name == null || name.trim().isEmpty() || text == null) return false;
        return CmdTemplates.save(name.trim(), java.util.Arrays.asList(text.replace("\r", "").split("\n"))) != null;
    }

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

    /** מ0.45 (ב9): כמה קוראים לכל כונן בבנייה — מה שהיה בהגדרות; התוסף מודד וקובע. 1 עד 16. */
    @JavascriptInterface
    public boolean setReaders(int n) {
        if (n < 1 || n > 16) return false;
        Conf.set(Conf.GENERAL, Conf.READERS, String.valueOf(n));
        Log.i("קוראים לכל כונן: " + n + " (מתוסף האבחון)");
        return true;
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
        File[] k = d.listFiles();
        int c = 0;
        Library lib = Library.get(screen);
        if (k != null)
            for (File f : k)
                if (f.isFile() && f.delete()) {
                    lib.fileRemoved(f.getAbsolutePath());
                    c++;
                }
        return c;
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
            if (!dir.isDirectory()) dir.mkdirs();
            File f = new File(dir, n);
            boolean was = f.isFile();
            Files.writeAtomic(f, text == null ? "" : text);
            Library lib = Library.get(screen);
            if (was) lib.fileRemoved(f.getAbsolutePath());
            lib.fileAdded(f);
            return f.getAbsolutePath();
        } catch (Throwable t) {
            Log.e("Lab.saveText", t);
            return null;
        }
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
                    s = SqlStore.openReadOnly(f);
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

    /** זיכרון קטן לכל ניסוי (נשאר בין פתיחות). */
    @JavascriptInterface
    public String load(String key) {
        return prefs().getString(exp + "\u0001" + key, null);
    }

    @JavascriptInterface
    public void save(String key, String value) {
        prefs().edit().putString(exp + "\u0001" + key, value).apply();
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
        String[] err = {null};
        String p = Batch.path(raw == null ? "" : raw, Shortcuts.dir(), vars, err);
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
