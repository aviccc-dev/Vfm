package com.tikiyot.vfm;

// מ0.38 — מסך המעבדה (Lab); מ0.40 — מסך התוספים (המשתמש: "הוא כבר לא אמור להיקרא מעבדה, אלא תוסף עורך אינטראקטיבי").
// בלי תוסף — רשימת התוספים (נגיעה: החדש; "גרסאות": בחירה). עם תוסף — העמוד שלו, ובראש: השם, הגרסה, "יש גרסה N" אם יש
// גבוהה יותר בתיקייה, ו"גרסאות". נפתח בפקודה: פתח תוסף ["<תוסף>" ["<גרסה>"]] (וגם: פתח מסך תוספים / מעבדה).
// הגשר (vfm) — מה שעמוד של תוסף יכול לעשות בתוכנה — בקובץ משלו, בתיקיית הלב: lev/Bridge (מבנה 2, 13.11.2026).
// אחראי: התצוגה — הרשימה, העמוד, הגרסאות — ומה שהגשר צריך מהמסך (Bridge.Host). אסור לו: לכתוב או למחוק קבצים אחרים ישירות.

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class LabScreen extends Activity {

    static final String EXTRA = "exp", EXTRA_VER = "ver", EXTRA_FILE = "file", EXTRA_CMD = "cmd";
    /** מ0.45+: קובץ הפקודות שהעורך האינטראקטיבי פותח (startFile — פעם אחת). */
    private String startCmd;

    /**
     * מ0.45+ (המשתמש: "לבדוק ולתקן מה שצריך ולמחוק אותו"): עורך הפקודות — העורך האינטראקטיבי; הישן (BatchEditScreen) נמחק.
     * f — קובץ לפתוח בו, או null (מה שהיה פתוח בו).
     */
    static void edit(android.content.Context c, File f) {
        Intent i = new Intent(c, LabScreen.class).putExtra(EXTRA, Words.DbCmd.TOOL_EDITOR);
        if (f != null) i.putExtra(EXTRA_CMD, f.getAbsolutePath());
        if (!(c instanceof Activity)) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        c.startActivity(i);
    }

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
        startCmd = getIntent().getStringExtra(EXTRA_CMD);
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
            if ((p.getInt("bundled", 0) == vc && !force) || !Files.hasAccess(this)) return;
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
        web.addJavascriptInterface(new Bridge(this, new Bridge.Host() {
            @Override public void result(String id, String value) { LabScreen.this.result(id, value); }

            @Override
            public String startFile() {
                String p = startCmd;
                startCmd = null;
                return p;
            }
        }, exp, page, vars), "vfm");
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


    private SharedPreferences prefs() {
        return Bridge.prefs(this);
    }
}
