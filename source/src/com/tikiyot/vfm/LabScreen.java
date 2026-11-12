package com.tikiyot.vfm;

// מ0.38 — מסך המעבדה (Lab); מ0.40 — מסך התוספים (המשתמש: "הוא כבר לא אמור להיקרא מעבדה, אלא תוסף עורך אינטראקטיבי").
// בלי תוסף — רשימת התוספים (נגיעה: החדש; "גרסאות": בחירה). עם תוסף — העמוד שלו, ובראש: השם, הגרסה, "יש גרסה N" אם יש
// גבוהה יותר בתיקייה, ו"גרסאות". נפתח בפקודה: פתח תוסף ["<תוסף>" ["<גרסה>"]] (וגם: פתח מסך תוספים / מעבדה).
// הגשר (vfm) — מה שעמוד מעבדה יכול לעשות בתוכנה; כללי, לכל ניסוי (המשתמש: "המעבדה זה לא רק למצב הזה, אלא גם לפיתוחים
// אחרים"): לקרוא, לבחור בבורר האמיתי, לכתוב קובצי פקודות, ולהריץ פקודות דרך ההרצה הרגילה (תצוגה מקדימה ואישור).
// אחראי: התצוגה והגשר. אסור לו: לכתוב או למחוק קבצים אחרים ישירות — כל שינוי אחר עובר דרך הפקודות.

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class LabScreen extends Activity {

    static final String EXTRA = "exp", EXTRA_VER = "ver", EXTRA_FILE = "file";
    /** תשובת saveCommandFile כשיש כבר קובץ — העמוד שואל: להחליף או שם אחר. */
    static final String EXISTS = "exists";
    private static final String PREFS = "lab";

    private String exp;
    /** מ0.40: העמוד שנפתח (גרסה אחת של התוסף); null — עוד לא נבחר. */
    private Lab.Page page;
    private WebView web;
    private Map<String, String> vars;

    static void open(Activity a, String exp) {
        open(a, exp, null, null);
    }

    /** מ0.40: ver — גרסה מסוימת ("פתח תוסף "<שם>" "<גרסה>""); file — עמוד מסוים (מבחירת הגרסה). שניהם null — החדש. */
    static void open(Activity a, String exp, String ver, File file) {
        Intent i = new Intent(a, LabScreen.class);
        if (exp != null && !exp.trim().isEmpty()) i.putExtra(EXTRA, exp.trim());
        if (ver != null && !ver.trim().isEmpty()) i.putExtra(EXTRA_VER, ver.trim());
        if (file != null) i.putExtra(EXTRA_FILE, file.getAbsolutePath());
        a.startActivity(i);
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        exp = getIntent().getStringExtra(EXTRA);
        vars = BatchUi.vars(this, Shortcuts.dir());
        bundled(false);
        if (exp == null) { showList(); return; }
        File root = Lab.root();
        if (Lab.newest(root, exp) == null) bundled(true);                     // מ0.42: תוסף שבא עם התוכנה ונמחק — חוזר כשפותחים אותו
        String ver = getIntent().getStringExtra(EXTRA_VER), file = getIntent().getStringExtra(EXTRA_FILE);
        if (file != null) for (Lab.Page p : Lab.versions(root, exp)) if (p.file.getAbsolutePath().equals(file)) page = p;
        if (page == null && ver != null) {
            page = Lab.version(root, exp, ver);
            if (page == null) Ui.toast(this, Words.Lab.noVersion(exp, ver));
        }
        if (page == null && ver == null) page = Lab.newest(root, exp);
        showPage();
    }

    /** מ0.41: התוספים שבאים עם התוכנה — לתיקיית התוספים, פעם אחת לכל גרסה של התוכנה (מי שמחק — לא חוזר עד הגרסה הבאה). */
    private void bundled(boolean force) {
        try {
            SharedPreferences p = prefs();
            int vc = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
            if ((p.getInt("bundled", 0) == vc && !force) || !Ui.hasFiles(this)) return;
            String[] names = getAssets().list(Lab.BUNDLED);
            if (names != null) for (String n : names)
                if (Lab.isHtml(n)) {
                    File f = Lab.addBundled(Lab.root(), DbExamples.asset(this, Lab.BUNDLED + "/" + n), n);
                    if (f != null) Log.i("תוסף מההתקנה: " + f.getName());   // בכונן
                }
            p.edit().putInt("bundled", vc).apply();
        } catch (Throwable t) {
            Log.e("LabScreen.bundled", t);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (exp == null && web == null) showList();                          // אחרי שעמוד עבר לתיקייה — הרשימה מתעדכנת
        // מ0.42: חזרה לעמוד (למשל ממסך ההרצה) — העמוד קורא את התוצאה (תוסף האבחון: קובץ ה"דוח" של ההרצה)
        if (web != null) web.evaluateJavascript("window.vfmResume&&window.vfmResume()", null);
    }

    // ---------- רשימת הניסויים ----------

    private void showList() {
        final File root = Lab.root();
        LinearLayout box = Ui.screen(this);
        box.setPadding(Ui.dp(this, 14), Ui.dp(this, 10), Ui.dp(this, 14), Ui.dp(this, 10));
        TextView title = Ui.label(this, Words.Lab.TITLE);
        title.setTextSize(18);
        box.addView(title);
        List<String> exps = Lab.names(root);
        if (exps.isEmpty()) {
            TextView t = Ui.info(this);
            t.setText(Words.Lab.EMPTY);
            t.setTextSize(14);
            box.addView(t);
        }
        for (final String e : exps) {
            LinearLayout row = new LinearLayout(this);
            row.setLayoutDirection(Ui.DIRECTION);
            row.setGravity(Gravity.CENTER_VERTICAL);
            Button open = new Button(this);
            open.setAllCaps(false);
            List<Lab.Page> all = Lab.versions(root, e);
            String h = Lab.higher(root, all.get(0));
            open.setText(Words.Lab.row(e, all.get(0).ver, all.size()) + (h == null ? "" : " · " + Words.Lab.higher(h)));
            open.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { open(LabScreen.this, e); }
            });
            row.addView(open, new LinearLayout.LayoutParams(0, -2, 1f));
            Button vs = new Button(this);
            vs.setAllCaps(false);
            vs.setText(Words.Lab.VERSIONS);
            vs.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { versions(e, false); }
            });
            row.addView(vs);
            box.addView(row);
        }
        setContentView(TopRow.wrap(this, Ui.scroll(this, box)));
    }

    /**
     * מ0.40: בחירת גרסה — כל העמודים של התוסף, החדש (לפי התאריך) קודם; הבחירה פותחת אותו עכשיו. "פתח תוסף" פותח תמיד את
     * החדש; גרסה קבועה — בפקודה: פתח תוסף "<שם>" "<גרסה>".
     */
    private void versions(final String e, final boolean here) {
        final List<Lab.Page> vs = Lab.versions(Lab.root(), e);
        final List<String> shown = new ArrayList<String>();
        for (Lab.Page p : vs)
            shown.add(Words.Lab.versionRow(p.ver, Files.when(p.file.lastModified()), page != null && p.file.equals(page.file)));
        Ui.list(this, e, shown, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int which) {
                Lab.Page p = vs.get(which);
                if (!here) {
                    open(LabScreen.this, e, null, p.file);
                    return;
                }
                if (web != null) web.destroy();
                web = null;
                page = p;
                showPage();
            }
        });
    }

    // ---------- עמוד הניסוי ----------

    private void showPage() {
        final File root = Lab.root();
        if (page == null || !page.file.isFile()) {
            if (Lab.newest(root, exp) == null) Ui.toast(this, Words.Lab.NOT_FOUND);
            exp = null;
            page = null;
            showList();
            return;
        }
        File run;                                                              // עותק בתיקייה הפרטית — נטען תמיד, בכל אנדרואיד
        try {
            File dir = new File(getFilesDir(), "lab-run");
            dir.mkdirs();
            run = new File(dir, "page.html");
            Files.writeAtomic(run, Files.readUtf8(page.file));
        } catch (Throwable t) {
            Log.e("LabScreen.copy", t);
            Ui.toast(this, Words.Db.failed(String.valueOf(t.getMessage())));
            finish();
            return;
        }
        LinearLayout root2 = Ui.screen(this);
        LinearLayout bar = new LinearLayout(this);
        bar.setLayoutDirection(Ui.DIRECTION);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(Ui.dp(this, 10), 0, Ui.dp(this, 6), 0);
        String h = Lab.higher(root, page);                                    // "ואם יש יותר חדש — הוא מזוהה בתוסף"
        TextView t = Ui.label(this, Words.Lab.TITLE + " · " + exp + " · " + Words.Lab.version(page.ver) + (h == null ? "" : " · " + Words.Lab.higher(h)));
        bar.addView(t, new LinearLayout.LayoutParams(0, -2, 1f));
        Button vs = new Button(this);
        vs.setAllCaps(false);
        vs.setText(Words.Lab.VERSIONS);
        vs.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { versions(exp, true); }
        });
        bar.addView(vs);
        root2.addView(bar);

        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setAllowFileAccess(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccessFromFileURLs(false);
        s.setAllowUniversalAccessFromFileURLs(false);
        s.setAllowContentAccess(false);
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, String url) {
                return url == null || !url.startsWith("file:");                     // רק העמוד עצמו; שום אתר
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                return !"file".equals(r.getUrl().getScheme());
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage m) {
                if (m.messageLevel() == ConsoleMessage.MessageLevel.ERROR)   // שגיאה בעמוד — ליומן (נראית בדוח האבחון)
                    Log.i("תוסף " + exp + ": " + m.message() + " @" + m.lineNumber());   // בכונן
                return true;
            }
        });
        web.addJavascriptInterface(new Bridge(), "vfm");
        root2.addView(web, new LinearLayout.LayoutParams(-1, 0, 1f));
        setContentView(root2);
        web.loadUrl(Uri.fromFile(run).toString());
    }

    @Override
    public void onBackPressed() {
        if (web == null) {
            super.onBackPressed();
            return;
        }
        web.evaluateJavascript("(window.vfmBack&&window.vfmBack())?'1':''", new ValueCallback<String>() {   // העמוד סוגר חלון משלו קודם
            @Override public void onReceiveValue(String v) { if (!"\"1\"".equals(v)) finish(); }
        });
    }

    @Override
    protected void onDestroy() {
        if (web != null) web.destroy();
        super.onDestroy();
    }

    private void result(final String id, final String value) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (web == null) return;
                web.evaluateJavascript("window.vfmResult&&window.vfmResult(" + JSONObject.quote(id) + ","
                        + (value == null ? "null" : JSONObject.quote(value)) + ")", null);
            }
        });
    }

    private File resolve(String raw) {
        String[] err = {null};
        String p = Batch.path(raw == null ? "" : raw, Shortcuts.dir(), vars, err);
        return p == null ? null : new File(p);
    }

    // ---------- הגשר ----------

    /** מה שעמוד מעבדה יכול לעשות. כל קריאה — מחזירה טקסט (JSON כשיש מבנה); בחירה — חוזרת ב־window.vfmResult(id, ערך). */
    final class Bridge {

        @JavascriptInterface
        public String info() {
            try {
                JSONObject o = new JSONObject();
                o.put("app", getPackageName());
                o.put("version", getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
                o.put("mode", String.valueOf(DbListScreen.currentMode));
                o.put("lab", exp);
                o.put("labVersion", page == null ? "" : page.ver);
                String h = page == null ? null : Lab.higher(Lab.root(), page);   // מ0.40: יש גרסה גבוהה יותר בתיקייה
                o.put("labNewer", h == null ? "" : h);
                // מ0.42 — לתוסף האבחון: המכשיר, ההרשאה, המקום, היומנים והאינדקס (מה שהיה בראש דוח האבחון ובהגדרות)
                o.put("versionCode", getPackageManager().getPackageInfo(getPackageName(), 0).versionCode);
                o.put("device", Log.device());
                o.put("access", Ui.hasFiles(LabScreen.this));
                o.put("freeMb", android.os.Environment.getExternalStorageDirectory().getUsableSpace() / (1024 * 1024));
                o.put("log", Log.file().getAbsolutePath());
                o.put("trial", Trial.file().getAbsolutePath());
                o.put("savedMode", DbScreen.savedMode(LabScreen.this));
                Library lib = Library.get(LabScreen.this);
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
                o.put("text", hex(Ui.TEXT)); o.put("sub", hex(Ui.SUB)); o.put("muted", hex(Ui.MUTED)); o.put("red", hex(Ui.RED));
                o.put("bg", hex(Ui.BG)); o.put("bgSoft", hex(Ui.BG_SOFT)); o.put("header", hex(Ui.HEADER)); o.put("headerSoft", hex(Ui.HEADER_SOFT));
                o.put("bar", hex(Ui.BAR)); o.put("selected", hex(Ui.SELECTED)); o.put("accent", hex(Ui.ACCENT)); o.put("accentDark", hex(Ui.ACCENT_DARK));
                o.put("rule", hex(Ui.RULE)); o.put("onColor", hex(Ui.ON_COLOR));
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
                for (Menus.Item it : Menus.get(LabScreen.this).at(Menus.MINE))            // מ0.41: מהתפריטים — הכללי ושל האופן
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
                    Library.get(LabScreen.this).fileAdded(f);
                    Reserved.addToMine(f);
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
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        File dir = new File(getCacheDir(), "lab");
                        dir.mkdirs();
                        File f = new File(dir, Words.Lab.RUN_FILE + "." + Batch.EXT);
                        // מ0.40 — המשתמש: "למה ההעברה בוצעה בפועל? הרי זה אמור לעבוד רק בכאילו." מהמעבדה — תמיד הדמיה:
                        // שורת "הדמיה" בראש, ומסך ההרצה מראה מה היה קורה, בלי כפתור הרצה. שום דבר בכונן או במסד לא משתנה.
                        Files.writeAtomic(f, Batch.AS_IF2 + "\n" + (text == null ? "" : text));
                        // מ0.39: תמיד במסך ההרצה — תצוגה מקדימה, אישור, ודוח (בהרצה הרגילה, בלי "בדיקה", זה רץ מיד ונראית רק
                        // הודעה קצרה; המשתמש: "איפה רואים את התוצאה של הפעולה?")
                        RunScreen.start(LabScreen.this, f);
                    } catch (Throwable t) {
                        Log.e("Lab.runCommands", t);
                        Ui.toast(LabScreen.this, Words.Db.failed(String.valueOf(t.getMessage())));
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
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        File dir = new File(getCacheDir(), "lab");
                        dir.mkdirs();
                        String w = where == null ? "" : where.trim();
                        String name = Lab.safe(new File(w).getName());
                        if (name.isEmpty()) name = Words.Lab.RUN_FILE + "." + Batch.EXT;
                        if (!Batch.isBatch(name)) name = name + "." + Batch.EXT;
                        File f = new File(dir, name);
                        Files.writeAtomic(f, (real ? "" : Batch.AS_IF2 + "\n") + (text == null ? "" : text));
                        File here = w.startsWith("/") ? new File(w).getParentFile() : Shortcuts.dir();
                        RunScreen.start(LabScreen.this, f, here);
                    } catch (Throwable t) {
                        Log.e("Lab.runFile", t);
                        Ui.toast(LabScreen.this, Words.Db.failed(String.valueOf(t.getMessage())));
                    }
                }
            });
        }

        /** מ0.40: שמות הרשימות החכמות של האופן (לבורר שבעורך): ["חדשים", "ספרים\\פדף"…]. */
        @JavascriptInterface
        public String smartLists() {
            JSONArray a = new JSONArray();
            try {
                Map<String, List<String>> n = DbPicker.names(LabScreen.this);
                List<String> sm = n.get("smart");
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
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        DbSmartForm.ask(LabScreen.this, name, cond, sort, new DbSmartForm.Done() {
                            @Override
                            public void ok(DbSmartForm.Result r) {
                                try {
                                    JSONObject o = new JSONObject();
                                    o.put("name", r.name);
                                    o.put("cond", r.cond);
                                    o.put("sort", r.sort);
                                    o.put("view", r.view);
                                    result(id, o.toString());
                                } catch (Throwable t) {
                                    Log.e("Lab.smartForm.result", t);
                                }
                            }
                        });
                    } catch (Throwable t) {
                        Log.e("Lab.smartForm", t);
                        Ui.toast(LabScreen.this, Words.Db.failed(String.valueOf(t.getMessage())));
                    }
                }
            });
        }

        /** מ0.40: אופני העבודה שיש להם מסד (לפקודה "אופן"). */
        @JavascriptInterface
        public String modes() {
            JSONArray a = new JSONArray();
            try {
                for (String m : DbScreen.dbModes(LabScreen.this)) a.put(m);
            } catch (Throwable t) {
                Log.e("Lab.modes", t);
            }
            return a.toString();
        }

        @JavascriptInterface
        public void pickFolder(final String id, final String title, final boolean canCreate) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    FolderPicker.choose(LabScreen.this, title == null ? Words.FolderP.PICK_FOLDER : title, canCreate, new FolderPicker.Picked() {
                        @Override public void ok(File folder) { result(id, folder.getAbsolutePath()); }
                    });
                }
            });
        }

        /** קובץ: קודם התיקייה (הבורר הרגיל), ואז הקבצים שבה. */
        @JavascriptInterface
        public void pickFile(final String id, final String title) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    FolderPicker.choose(LabScreen.this, title == null ? Words.Lab.PICK_FILE : title, false, new FolderPicker.Picked() {
                        @Override
                        public void ok(File folder) {
                            File[] k = folder.listFiles();
                            final List<File> fs = new ArrayList<File>();
                            if (k != null) for (File x : k) if (x.isFile()) fs.add(x);
                            java.util.Collections.sort(fs);
                            if (fs.isEmpty()) {
                                Ui.toast(LabScreen.this, Words.Lab.NO_FILES_HERE);
                                return;
                            }
                            List<String> names = new ArrayList<String>();
                            for (File x : fs) names.add(x.getName());
                            Ui.list(LabScreen.this, folder.getName(), names, new DialogInterface.OnClickListener() {
                                @Override public void onClick(DialogInterface d, int w) { result(id, fs.get(w).getAbsolutePath()); }
                            });
                        }
                    });
                }
            });
        }

        /** רשימה מהמסד: {mode, path} — path בשמות, מופרד ב־\. */
        @JavascriptInterface
        public void pickList(final String id, final String title) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    DbPicker.choose(LabScreen.this, title == null ? Words.NEW_LIST : title, new DbPicker.Picked() {
                        @Override
                        public void ok(String mode, long list, List<String> path) {
                            try {
                                JSONObject o = new JSONObject();
                                o.put("mode", mode);
                                StringBuilder sb = new StringBuilder();
                                for (String p : path) sb.append(sb.length() > 0 ? "\\" : "").append(p);
                                o.put("path", sb.toString());
                                result(id, o.toString());
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
                Library lib = Library.get(LabScreen.this);
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
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    File f = resolve(path);
                    if (f == null || !f.isFile()) {
                        Ui.toast(LabScreen.this, Words.Verb.NOT_FOUND + path);
                        return;
                    }
                    Acts.sendReady(LabScreen.this, java.util.Collections.singletonList(f));
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
            for (String m : DbScreen.dbModes(LabScreen.this)) {
                JSONObject o = new JSONObject();
                SqlStore s = null;
                try {
                    File f = DbScreen.dbFile(LabScreen.this, m);
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
                    List<Backups.Entry> bk = DbScreen.backupsOf(LabScreen.this, m).list();
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
                o.put("index", f != null && Library.get(LabScreen.this).find(f.getAbsolutePath()) != null);
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
            runOnUiThread(new Runnable() {
                @Override public void run() { Ui.toast(LabScreen.this, s); }
            });
        }

        @JavascriptInterface
        public void log(String s) {
            Log.i("תוסף " + exp + ": " + s);   // בכונן
        }

        @JavascriptInterface
        public void close() {
            runOnUiThread(new Runnable() {
                @Override public void run() { finish(); }
            });
        }
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

    private SharedPreferences prefs() {
        return getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
