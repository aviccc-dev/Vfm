package com.tikiyot.vfm;

// מ0.38 — מסך המעבדה (Lab); מ0.40 — מסך התוספים (המשתמש: "הוא כבר לא אמור להיקרא מעבדה, אלא תוסף עורך אינטראקטיבי").
// בלי תוסף — רשימת התוספים (נגיעה: פתיחה; "גרסאות": החלפה). עם תוסף — העמוד שלו, ובראש: השם, הגרסה, "גרסאות".
// נפתח בפקודה: פתח תוסף ["<תוסף>"] (וגם: פתח מסך תוספים / מעבדה).
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

    static final String EXTRA = "exp";
    /** תשובת saveCommandFile כשיש כבר קובץ — העמוד שואל: להחליף או שם אחר. */
    static final String EXISTS = "exists";
    private static final String PREFS = "lab";

    private String exp;
    private WebView web;
    private Map<String, String> vars;

    static void open(Activity a, String exp) {
        Intent i = new Intent(a, LabScreen.class);
        if (exp != null && !exp.trim().isEmpty()) i.putExtra(EXTRA, exp.trim());
        a.startActivity(i);
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        exp = getIntent().getStringExtra(EXTRA);
        vars = BatchUi.vars(this, Shortcuts.dir());
        if (exp == null) showList();
        else showPage();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (exp == null && web == null) showList();                          // אחרי "הוסף תוסף" — הרשימה מתעדכנת
    }

    // ---------- רשימת הניסויים ----------

    private void showList() {
        final File root = Lab.root();
        LinearLayout box = Ui.screen(this);
        box.setPadding(Ui.dp(this, 14), Ui.dp(this, 10), Ui.dp(this, 14), Ui.dp(this, 10));
        TextView title = Ui.label(this, Words.Lab.TITLE);
        title.setTextSize(18);
        box.addView(title);
        List<String> exps = Lab.experiments(root);
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
            open.setText(Words.Lab.row(e, String.valueOf(Lab.active(root, e)), Lab.versions(root, e).size()));
            open.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { open(LabScreen.this, e); }
            });
            row.addView(open, new LinearLayout.LayoutParams(0, -2, 1f));
            Button vs = new Button(this);
            vs.setAllCaps(false);
            vs.setText(Words.Lab.VERSIONS);
            vs.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { versions(e, null); }
            });
            row.addView(vs);
            box.addView(row);
        }
        setContentView(TopRow.wrap(this, Ui.scroll(this, box)));
    }

    /** בחירת הגרסה הפעילה; אחריה — then (או רענון הרשימה). */
    private void versions(final String e, final Runnable then) {
        final File root = Lab.root();
        final List<String> vs = Lab.versions(root, e);
        String act = Lab.active(root, e);
        final List<String> shown = new ArrayList<String>();
        for (int i = vs.size() - 1; i >= 0; i--) shown.add(Words.Lab.version(vs.get(i)) + (vs.get(i).equals(act) ? " ✓" : ""));
        Ui.list(this, e, shown, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int which) {
                String err = Lab.setActive(root, e, vs.get(vs.size() - 1 - which));
                if (err != null) Ui.toast(LabScreen.this, err);
                if (then != null) then.run();
                else showList();
            }
        });
    }

    // ---------- עמוד הניסוי ----------

    private void showPage() {
        final File root = Lab.root();
        File page = Lab.page(root, exp);
        if (page == null) {
            Ui.toast(this, Words.Lab.NOT_FOUND);
            exp = null;
            showList();
            return;
        }
        File run;                                                              // עותק בתיקייה הפרטית — נטען תמיד, בכל אנדרואיד
        try {
            File dir = new File(getFilesDir(), "lab-run");
            dir.mkdirs();
            run = new File(dir, "page.html");
            Files.writeAtomic(run, Files.readUtf8(page));
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
        TextView t = Ui.label(this, Words.Lab.TITLE + " · " + exp + " · " + Words.Lab.version(String.valueOf(Lab.active(root, exp))));
        bar.addView(t, new LinearLayout.LayoutParams(0, -2, 1f));
        Button vs = new Button(this);
        vs.setAllCaps(false);
        vs.setText(Words.Lab.VERSIONS);
        vs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                versions(exp, new Runnable() {
                    @Override public void run() { web = null; showPage(); }
                });
            }
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
                o.put("labVersion", String.valueOf(Lab.active(Lab.root(), exp)));
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
            return BatchLine.toVar(abs == null ? "" : abs, vars);
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
                String mode = DbListScreen.currentMode;
                for (int pass = 0; pass < 2; pass++) {
                    String m = pass == 0 ? mode : DbCmdMenu.otherMode(LabScreen.this, mode, Reserved.MINE_PATH);
                    if (m == null || !DbScreen.dbFile(LabScreen.this, m).isFile()) continue;
                    SqlStore s = SqlStore.openReadOnly(DbScreen.dbFile(LabScreen.this, m));
                    try {
                        long id = DbPaths.find(s, m, Reserved.MINE_PATH);
                        if (id == 0) continue;
                        for (DbMore.Entry en : DbMore.menu(s, id))
                            if (en.kind == DbMore.E_FILE && new File(en.path).isFile() && seen.add(en.path)) a.put(entry(new File(en.path)));
                        break;
                    } finally {
                        s.close();
                    }
                }
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
