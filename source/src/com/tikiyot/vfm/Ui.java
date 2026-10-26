package com.tikiyot.vfm;

// ערכת הממשק היחידה. אחראית: שורות רשימה, חלונות, שדות, פתיחת קבצים, הרשאות.
// אסור לה: להחזיק מצב או לגעת בקובץ הראשי.

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.List;
import java.util.Locale;

public final class Ui {

    // ---------- פלטת הצבעים — במקום אחד (80, תצוגה בסיסית; ובעתיד מצב כהה) ----------
    public static final int TEXT = 0xFF202124, SUB = 0xFF5F6368, MUTED = 0xFF9AA0A6,
            RED = 0xFFC5221F, BG_SOFT = 0xFFF4F5F6, MARK = 0xFFFFF2A8,
            BG = 0xFFFFFFFF, ON_COLOR = 0xFFFFFFFF,
            /** רקע של שורת כותרת/מסלול, ושל כותרת קבוצה. */
            HEADER = 0xFFF1F3F4, HEADER_SOFT = 0xFFE8EAED,
            /** פס הבחירה, ושורה שנבחרה. */
            BAR = 0xFFE8F0FE, SELECTED = 0xFFD2E3FC,
            /** צלמיות רגילות בפס. */
            ICON = 0xFF5F6368,
            /** "בספק" — התאמה לא ודאית (כפולים בשם דומה; כמו כתיב חסר בחיפוש). */
            DOUBT = 0xFFFFF1C2, DOUBT_STRONG = 0xFFF2B52B,
            /** 197: כחול — הבחירה הפעילה, וכפתור טקסט; קו דק בין חלקים. */
            ACCENT = 0xFF1A73E8, ACCENT_DARK = 0xFF174EA6, RULE = 0xFFE0E0E0;

    /** כיוון המסכים — במקום אחד (80): כשיהיה קובץ הודעות בשפה אחרת, ייקבע מכאן. */
    public static final int DIRECTION = View.LAYOUT_DIRECTION_RTL;

    /** כמה פעמים מוצג הסבר קבוע ("נגיעה ארוכה…") עד שהוא נעלם — למתחילים, לא לכל פתיחה. */
    public static final int HINT_TIMES = 5;

    /** האם להציג עכשיו את ההסבר key (בחמש הפתיחות הראשונות של המסך). נקרא פעם אחת בפתיחת המסך. */
    public static boolean firstTimes(Context c, String key) {
        try {
            android.content.SharedPreferences sp = c.getSharedPreferences("vfm", Context.MODE_PRIVATE);
            int n = sp.getInt("hint:" + key, 0);
            if (n >= HINT_TIMES) return false;
            sp.edit().putInt("hint:" + key, n + 1).apply();
            return true;
        } catch (Throwable t) {
            return true;
        }
    }

    private Ui() { }

    public static int dp(Context c, float v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                c.getResources().getDisplayMetrics());
    }

    public static void toast(Context c, String s) {
        Toast.makeText(c, s, Toast.LENGTH_SHORT).show();
    }

    public static LinearLayout screen(Activity a) {
        LinearLayout root = new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(DIRECTION);
        root.setBackgroundColor(BG);
        return root;
    }

    public static TextView info(Context c) {
        TextView t = new TextView(c);
        t.setTextSize(12);
        t.setTextColor(SUB);
        t.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        t.setPadding(dp(c, 12), dp(c, 10), dp(c, 12), dp(c, 6));
        return t;
    }

    // ---------- הרשאות ----------

    public static boolean hasFiles(Context c) {
        if (Build.VERSION.SDK_INT >= 30) return Environment.isExternalStorageManager();
        if (Build.VERSION.SDK_INT >= 23)
            return c.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == 0;
        return true;
    }

    public static void requestFiles(Activity a) {
        try {
            if (Build.VERSION.SDK_INT >= 30)
                a.startActivity(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:" + a.getPackageName())));
            else if (Build.VERSION.SDK_INT >= 23)
                a.requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        } catch (Throwable t) {
            toast(a, Words.UiW.CANT_OPEN_PERMISSION);
        }
    }

    // ---------- שורות ----------

    public static View row(Context c, View convert) {
        if (convert != null && convert.getTag() instanceof TextView[]) return convert;
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutDirection(DIRECTION);
        row.setPadding(dp(c, 12), dp(c, 8), dp(c, 12), dp(c, 8));

        TextView type = new TextView(c);
        type.setTypeface(Typeface.DEFAULT_BOLD);
        type.setGravity(Gravity.CENTER);
        type.setTextColor(ON_COLOR);
        type.setSingleLine(true);
        type.setPadding(dp(c, 3), dp(c, 3), dp(c, 3), dp(c, 3));
        row.addView(type, new LinearLayout.LayoutParams(dp(c, 54), LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout texts = new LinearLayout(c);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setPadding(dp(c, 10), 0, 0, 0);
        TextView title = new TextView(c);
        title.setTextSize(16);
        title.setTextColor(TEXT);
        title.setMaxLines(2);
        title.setEllipsize(TextUtils.TruncateAt.END);
        title.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        TextView sub = new TextView(c);
        sub.setTextSize(11);
        sub.setTextColor(MUTED);
        sub.setSingleLine(true);
        sub.setEllipsize(TextUtils.TruncateAt.START);
        sub.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        TextView more = new TextView(c);
        more.setTextSize(11);
        more.setTextColor(MUTED);
        more.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        more.setVisibility(View.GONE);
        texts.addView(title);
        texts.addView(sub);
        texts.addView(more);
        row.addView(texts, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        row.setTag(new TextView[]{type, title, sub, more});
        return row;
    }

    // ---------- 160: תווית על הצלמית — "תיקייה אמיתית", "רשימה חכמה"… (המשתמש: "הכיתוב חשוב יותר מהצלמית") ----------

    /** הסמלים שמסמנים מכל — והתווית והצבע של כל אחד — מקום אחד. */
    static final String FOLDER = "📁", SMART = "✨", MANUAL = "📋";
    /** 1.17.0: עוד שני סוגים עם תווית — קישור ↪ ופקודה (המילון, 24: "צריך שיהיה אפשר מיידית לזהות כל פריט מה הוא"). */
    static final String LINK = "↪", CMD = "▶";

    /** 1.13.0: צלמיות ברירת המחדל ברשימה ידנית — במקום תווית הטקסט (המשתמש: "מתאים"). */
    static final String LIVE = "🔄";                                        // תיקייה מתעדכנת

    /** התווית (בשתי שורות) — או null, אם זה סמל רגיל. */
    public static String tag(String icon) {
        if (FOLDER.equals(icon)) return Words.Tags.FOLDER;
        if (LINK.equals(icon)) return Words.Tags.LINK;
        if (CMD.equals(icon)) return Words.Tags.CMD;
        if (LIVE.equals(icon)) return Words.Tags.LIVE;
        if (SMART.equals(icon)) return Words.Tags.SMART;
        if (MANUAL.equals(icon)) return Words.Tags.MANUAL;
        if (Drive.icon(true).equals(icon)) return Words.Tags.INTERNAL;
        if (Drive.icon(false).equals(icon)) return Words.Tags.CARD;
        return null;
    }

    public static int tagColor(String icon) {
        if (FOLDER.equals(icon)) return 0xFFBA7517;                         // כתום־צהוב — תמיד ורק תיקייה אמיתית
        if (SMART.equals(icon)) return 0xFF534AB7;                          // סגול
        if (MANUAL.equals(icon)) return 0xFF0F6E56;                         // ירוק
        if (LINK.equals(icon)) return 0xFF993C1D;                           // 1.17.0: כתום־אדמדם
        if (CMD.equals(icon) || Words.Tags.CMD.equals(icon)) return 0xFF185FA5;   // כחול
        if (LIVE.equals(icon)) return 0xFF854F0B;
        return 0xFF5F5E5A;                                                  // כונן — אפור
    }

    /** 1.17.0: המילה הקצרה של הסוג — לפס שבתחתית האריח (ברשת). סוג קובץ — כמו שהוא. */
    static String tagShort(String kind) {
        String t = tag(kind);
        if (t == null) return kind;
        String[] w = t.split("\n");
        if (FOLDER.equals(kind) || w.length == 1) return w[0];                 // "תיקייה", "פקודה"
        return LINK.equals(kind) ? t.replace('\n', ' ') : w[w.length - 1];    // "↪ קישור", "ידנית", "חכמה"
    }

    /** 1.17.0: צבע הסוג — של תווית, או של סוג קובץ. */
    static int kindColor(String kind) {
        return tag(kind) != null ? tagColor(kind) : typeColor(kind);
    }

    /**
     * 1.17.0: הסוג — תמיד, והצלמית של המשתמש — בנוסף (המילון, 24; המשתמש: "כל פעם מישהו אחר משתלט על האייקון").
     * בשורות (א'): התווית במקומה, והצלמית לפני השם. ברשת (ב') — Looks.apply: הצלמית באריח, והתווית כפס בתחתיתו.
     * kind — אחד מסמלי התוויות (MANUAL, SMART, LINK, CMD, FOLDER, LIVE) או סוג קובץ ("PDF").
     */
    static void kind(View row, String kind, String userIcon) {
        TextView[] v = (TextView[]) row.getTag();
        if (tag(kind) != null) icon(v[0], kind);
        v[0].setTag(kind);
        String own = userIcon == null || kind.equals(userIcon) ? "" : userIcon;
        v[1].setTag(own);
        if (!own.isEmpty() && !own.contains(".")) v[1].setText(own + "  " + v[1].getText());
    }

    /** הסוג שנקבע לשורה (או null — שורה בלי סוג). */
    static String kindOf(View row) {
        Object k = ((TextView[]) row.getTag())[0].getTag();
        return k instanceof String ? (String) k : null;
    }

    /** הצלמית של המשתמש בשורה ("" — אין). */
    static String userIconOf(View row) {
        Object k = ((TextView[]) row.getTag())[1].getTag();
        return k instanceof String ? (String) k : "";
    }

    /** שורה שנבנית מחדש (מתאם) — בלי סוג וצלמית של השורה הקודמת. */
    private static void clearKind(TextView[] v) {
        v[0].setTag(null);
        v[1].setTag(null);
        v[1].setCompoundDrawablesRelative(null, null, null, null);
    }

    /** הצלמית שבשורה: תווית צבעונית (מכל), או סמל רגיל. */
    static void icon(TextView t, String icon) {
        String tag = tag(icon);
        t.setTextColor(ON_COLOR);
        if (tag != null) {
            t.setSingleLine(false);
            t.setMaxLines(2);
            t.setText(tag);
            t.setTextSize(10);
            t.setBackgroundColor(tagColor(icon));
        } else {
            t.setSingleLine(true);
            t.setText(icon);
            t.setTextSize(18);
            t.setBackgroundColor(0x00000000);
        }
    }

    /** שורה עם צלמית טקסט, למשל אריח או תיקייה. */
    public static void bindIcon(View row, String icon, String title, String sub) {
        TextView[] v = (TextView[]) row.getTag();
        clearKind(v);
        v[1].setMaxLines(2);
        v[2].setSingleLine(true);
        v[2].setEllipsize(TextUtils.TruncateAt.END);
        v[3].setVisibility(View.GONE);
        icon(v[0], icon);
        if (tag(icon) != null) v[0].setTag(icon);                            // 1.17.0: סמל של סוג — גם הוא סוג
        v[1].setText(title);
        v[1].setTextColor(TEXT);
        v[2].setText(sub == null ? "" : sub);
    }

    /** שורה של קובץ: תווית סוג צבעונית, שם בלי סיומת, ומסלול. */
    /** האם להציג נתונים מלאים בשורות. נזכר לכל התוכנה. */
    private static Boolean fullCache;

    public static boolean full(Context c) {
        if (fullCache == null) fullCache = Conf.is(Conf.GENERAL, Conf.FULL, false);
        return fullCache;
    }

    /** נקרא בכל פתיחת מסך, כדי שעריכה ידנית של הקובץ תיקלט. */
    public static void reloadFull() {
        fullCache = null;
    }

    public static void setFull(Context c, final boolean v) {
        fullCache = v;
        new Thread(new Runnable() {
            @Override public void run() { Conf.setBool(Conf.GENERAL, Conf.FULL, v); }
        }).start();
    }

    /**
     * שורת קובץ אחידה לכל המסכים.
     * רגילה: שם (עד שתי שורות), ושורה קבועה "תאריך · גודל · 📁 תיקייה". מלאה: שם מלא, אותה שורה,
     * המסלול המלא בלי חיתוך, ומצב.
     */
    public static void bindRow(Context c, View row, String name, boolean isDir, long size, long mtime,
                               String path, String status, String queryNorm) {
        boolean full = full(c);
        TextView[] v = (TextView[]) row.getTag();
        clearKind(v);
        String label = typeLabel(name, isDir);
        typeBox(v[0], label, isDir);
        v[0].setTag(isDir ? FOLDER : label);                                  // 1.17.0: הסוג — גם לשורת קובץ
        v[1].setText(highlight(baseName(name, isDir), queryNorm));
        v[1].setTextColor(TEXT);
        v[1].setMaxLines(full ? 20 : 2);
        StringBuilder d = new StringBuilder();
        if (mtime > 0) d.append(date(mtime));
        if (size > 0 && !isDir) d.append(d.length() > 0 ? "   ·   " : "").append(Files.size(size));
        if (!isDir && Batch.isBatch(name)) d.insert(0, Words.UiW.BATCH_LINE + (d.length() > 0 ? "   ·   " : ""));   // 1.13.1
        String folder = parentName(path);
        if (!full && folder != null) d.append(d.length() > 0 ? "   ·   " : "").append("📁 ").append(folder);
        if (!full && status != null) d.insert(0, status + "   ·   ");
        v[2].setText(d);
        v[2].setSingleLine(!full);
        v[2].setEllipsize(full ? null : TextUtils.TruncateAt.END);
        if (full) {
            v[3].setVisibility(View.VISIBLE);
            v[3].setText((status != null ? status + "\n" : "") + (path == null ? "" : path));
        } else {
            v[3].setVisibility(View.GONE);
        }
    }

    public static String date(long t) {
        return new java.text.SimpleDateFormat("d.M.yy", Locale.US).format(new java.util.Date(t));
    }

    private static String parentName(String path) {
        if (path == null) return null;
        int i = path.lastIndexOf('/');
        if (i <= 0) return null;
        String dir = path.substring(0, i);
        int j = dir.lastIndexOf('/');
        return j >= 0 ? dir.substring(j + 1) : dir;
    }

    /** הריבוע שבראש שורת קובץ: סוג הקובץ בצבע שלו, או 📁 לתיקייה (160) — לשתי צורות השורה. */
    private static void typeBox(TextView box, String label, boolean isDir) {
        if (isDir) icon(box, FOLDER);                                       // 160: "תיקייה אמיתית"
        else {
            box.setSingleLine(true);
            box.setText(label);
            box.setTextSize(label.length() > 4 ? 9 : 11);
            box.setBackgroundColor(typeColor(label));
        }
    }

    public static void bindFile(View row, String name, String sub, String queryNorm, boolean isDir) {
        TextView[] v = (TextView[]) row.getTag();
        v[3].setVisibility(View.GONE);
        v[1].setMaxLines(2);
        v[2].setSingleLine(true);
        clearKind(v);
        String label = typeLabel(name, isDir);
        typeBox(v[0], label, isDir);
        v[0].setTag(isDir ? FOLDER : label);
        v[1].setText(highlight(baseName(name, isDir), queryNorm));
        v[1].setTextColor(TEXT);
        v[2].setText(sub == null ? "" : sub);
    }

    public static View header(Context c, View convert, String text, boolean soft) {
        TextView t = convert instanceof TextView ? (TextView) convert : new TextView(c);
        t.setText(text);
        t.setTextSize(12);
        t.setTextColor(SUB);
        t.setBackgroundColor(soft ? HEADER_SOFT : HEADER);
        t.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        t.setPadding(dp(c, 14), dp(c, 6), dp(c, 14), dp(c, 6));
        return t;
    }

    public static String baseName(String name, boolean isDir) {
        if (isDir) return name;
        String st = Text.stem(name);                                          // 124: "פרק 1.17" — שלם
        return st.isEmpty() || name.length() - st.length() > 6 ? name : st;
    }

    private static CharSequence highlight(String shown, String queryNorm) {
        if (queryNorm == null) return shown;
        String q = queryNorm.trim();
        int sp = q.indexOf(' ');
        if (sp > 0) q = q.substring(0, sp);
        if (q.isEmpty() || q.indexOf('*') >= 0 || q.indexOf('?') >= 0 || q.indexOf(':') >= 0) return shown;
        String norm = Text.norm(shown);
        if (norm.length() != shown.length()) return shown;
        int i = norm.indexOf(q);
        if (i < 0) return shown;
        SpannableString sb = new SpannableString(shown);
        sb.setSpan(new BackgroundColorSpan(MARK), i, i + q.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return sb;
    }

    public static String typeLabel(String name, boolean isDir) {
        if (isDir) return "📁";
        if (Batch.isBatch(name)) return Words.Tags.CMD;                       // 1.17.0: פקודה — מזוהה כפקודה (המילון, 24)
        String e = Text.ext(name).toUpperCase(Locale.US);                    // 104
        return e.isEmpty() || e.length() > 5 ? "•" : e;
    }

    public static int typeColor(String label) {
        if (label.equals("📁")) return 0x00000000;
        if (label.equals(Words.Tags.CMD)) return tagColor(CMD);
        if (label.equals("PDF")) return 0xFFC5221F;
        if (label.equals("EPUB") || label.equals("MOBI") || label.equals("AZW3") || label.equals("DJVU"))
            return 0xFF188038;
        if (label.equals("DOC") || label.equals("DOCX") || label.equals("TXT") || label.equals("RTF")
                )
            return 0xFF1A73E8;
        if (label.equals("JPG") || label.equals("JPEG") || label.equals("PNG") || label.equals("WEBP"))
            return 0xFF9334E6;
        if (label.equals("MP3") || label.equals("M4A") || label.equals("OPUS") || label.equals("WAV"))
            return 0xFFE8710A;
        if (label.equals("MP4") || label.equals("MKV") || label.equals("AVI")) return 0xFF12B5CB;
        return MUTED;
    }

    // ---------- קבצים ----------

    public static String mime(String name) {
        String e = Text.ext(name);                                           // 104: "ספר.pdf (1)" נפתח כ-PDF
        if (e.isEmpty()) return null;
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(e);
    }

    public static void open(Context c, File f) {
        if (!f.exists()) {
            toast(c, Words.FILE_NOT_AT_PATH);
            return;
        }
        if (Batch.isBatch(f.getName())) {                                     // 117א: קובץ פקודות (214: גם .vbat.txt)
            if (f.getAbsolutePath().contains("/" + Index.TRASH + "/")) {         // 125: מהסל — לא מריצים
                toast(c, Words.UiW.BATCH_IN_TRASH);
                return;
            }
            if (c instanceof Activity) BatchUi.run((Activity) c, f);           // 1.16.1: נגיעה — הרצה (ארוכה — עריכה)
            else c.startActivity(new Intent(c, BatchEditScreen.class)            // 125: מהפס הצף — לעורך, ומשם הרצה
                    .putExtra(BatchEditScreen.EXTRA_FILE, f.getAbsolutePath()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return;
        }
        if (c instanceof Activity && Text.ext(f.getName()).equals("zip")             // 138: ZIP — גם פריסה
                && !f.getAbsolutePath().contains("/" + Index.TRASH + "/")) {
            final Activity a = (Activity) c;
            final File z = f;
            options(a, "📦 " + f.getName(), new String[]{Words.UiW.UNZIP_HERE, Words.UiW.OPEN_OTHER},
                    new android.content.DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(android.content.DialogInterface d, int w) {
                            if (w == 0) Physical.askUnzip(a, z.getAbsolutePath(), null);
                            else openOutside(a, z);
                        }
                    });
            return;
        }
        if (Text.ext(f.getName()).equals("apk") && !f.getAbsolutePath().contains("/" + Index.TRASH + "/")) {   // 186
            install(c, f);
            return;
        }
        openOutside(c, f);
    }

    /**
     * 186: התקנה (או עדכון) מקובץ apk. אנדרואיד תמיד שואל בעצמו "להתקין / לעדכן?" — אי אפשר בלי האישור.
     * בפעם הראשונה צריך לאשר לתוכנה להתקין (אנדרואיד 8 ומעלה) — נפתח מסך ההרשאה, וחוזרים.
     */
    static void install(Context c, File f) {
        if (android.os.Build.VERSION.SDK_INT >= 26 && !c.getPackageManager().canRequestPackageInstalls()) {
            toast(c, Words.Install.ALLOW_FIRST);
            Intent s = new Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    android.net.Uri.parse("package:" + c.getPackageName()));
            if (!(c instanceof Activity)) s.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                c.startActivity(s);
            } catch (Throwable t) {
                toast(c, Words.Install.NO_SETTINGS);
            }
            return;
        }
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setDataAndType(Provider.uriFor(f), Words.Install.MIME);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (!(c instanceof Activity)) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            c.startActivity(i);
        } catch (Throwable t) {
            Log.e("התקנה " + f, t);
            toast(c, Words.Install.FAILED);
        }
    }

    /** פתיחה בתוכנה חיצונית (ACTION_VIEW). */
    static void openOutside(Context c, File f) {
        String m = mime(f.getName());
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setDataAndType(Provider.uriFor(f), m != null ? m : "*/*");
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (!(c instanceof Activity)) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            c.startActivity(i);
        } catch (ActivityNotFoundException e) {
            toast(c, Words.UiW.NO_APP);
        } catch (Throwable t) {
            Log.e("פתיחת " + f, t);
            toast(c, Words.UiW.OPEN_FAILED);
        }
    }

    /** 1.11.0: פתיחה (או עריכה) עם חלון בחירת אפליקציה — תמיד, גם כשנקבעה ברירת מחדל. */
    static void openChoosing(Activity a, File f, boolean edit) {
        String m = mime(f.getName());
        Intent i = new Intent(edit ? Intent.ACTION_EDIT : Intent.ACTION_VIEW);
        i.setDataAndType(Provider.uriFor(f), m != null ? m : "*/*");
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | (edit ? Intent.FLAG_GRANT_WRITE_URI_PERMISSION : 0));
        try {
            a.startActivity(Intent.createChooser(i, edit ? Words.Act.EDIT : Words.Act.OPEN_WITH));
        } catch (ActivityNotFoundException e) {
            toast(a, Words.UiW.NO_APP);
        } catch (Throwable t) {
            Log.e("פתיחה באמצעות " + f, t);
            toast(a, Words.UiW.OPEN_FAILED);
        }
    }

    public static void editText(Activity a, File f) {
        int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
        Intent edit = new Intent(Intent.ACTION_EDIT);
        edit.setDataAndType(Provider.uriFor(f), "text/plain");
        edit.addFlags(flags);
        try {
            a.startActivity(edit);
            return;
        } catch (Throwable ignored) {
        }
        Intent view = new Intent(Intent.ACTION_VIEW);
        view.setDataAndType(Provider.uriFor(f), "text/plain");
        view.addFlags(flags);
        try {
            a.startActivity(view);
        } catch (Throwable t) {
            toast(a, Words.UiW.NO_TEXT_EDITOR);
        }
    }

    public static void copyText(Context c, String label, String text) {
        ClipboardManager cm = (ClipboardManager) c.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText(label, text));
            toast(c, Words.UiW.COPIED);
        }
    }

    // ---------- חלונות ----------

    public interface Text1 {
        void ok(String value);
    }

    public static void askText(Activity a, String title, String value, String hint, final Text1 cb) {
        final EditText et = field(a, value, hint);
        LinearLayout b = form(a);
        b.addView(et);
        new AlertDialog.Builder(a)
                .setTitle(title)
                .setView(b)
                .setPositiveButton(Words.OK, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        String s = et.getText().toString().trim();
                        if (!s.isEmpty()) cb.ok(s);
                    }
                })
                .setNegativeButton(Words.CANCEL, null)
                .show();
    }

    public static void confirm(Activity a, String title, String message, String yes, final Runnable onYes) {
        new AlertDialog.Builder(a)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) { onYes.run(); }
                })
                .setNegativeButton(Words.CANCEL, null)
                .show();
    }

    /**
     * תבנית קבועה לחלונות אישור, בארבע שורות בגדלים יורדים:
     * כותרת · מה קורה לקבצים (אדום מודגש) · מה בדיוק קורה · (איך חוזרים).
     */
    public static void ask(Activity a, String title, String files, String detail, String undo,
                           String yes, final Runnable onYes) {
        ask(a, title, files, detail, undo, yes, onYes, Words.CANCEL, null);
    }

    /** כמו ask, עם כפתור שני שעושה משהו (onNo). כשיש onNo — החלון לא נסגר בנגיעה מחוצה לו. */
    public static void ask(Activity a, String title, String files, String detail, String undo,
                           String yes, final Runnable onYes, String no, final Runnable onNo) {
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setLayoutDirection(DIRECTION);
        int p = dp(a, 22);
        box.setPadding(p, dp(a, 18), p, dp(a, 4));
        box.addView(line(a, title, 18, true, TEXT, 10));
        if (files != null) box.addView(line(a, files, 16, true, RED, 4));
        if (detail != null) box.addView(line(a, detail, 14, false, TEXT, 4));
        if (undo != null) box.addView(line(a, "(" + undo + ")", 12, false, SUB, 4));
        new AlertDialog.Builder(a)
                .setView(scroll(a, box))                                       // 136: תוכן ארוך — נגלל
                .setPositiveButton(yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) { onYes.run(); }
                })
                .setNegativeButton(no, onNo == null ? null : new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) { onNo.run(); }
                })
                .setCancelable(onNo == null)
                .show();
    }

    private static TextView line(Context c, String text, int sp, boolean bold, int color, int bottomDp) {
        TextView t = new TextView(c);
        t.setText(text);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        t.setPadding(0, 0, 0, dp(c, bottomDp));
        return t;
    }

    public static void options(Activity a, String title, String[] items,
                               DialogInterface.OnClickListener l) {
        new AlertDialog.Builder(a).setTitle(title).setItems(items, l).show();
    }

    /** 136: תוכן חלון שעלול להיות ארוך — בתוך גלילה, כדי שהכפתורים יישארו גלויים והכול ייקרא. */
    static android.widget.ScrollView scroll(Context c, View content) {
        android.widget.ScrollView sv = new android.widget.ScrollView(c);
        sv.addView(content);
        return sv;
    }

    /** בחירה של פריט לפי מקומו (למשל בנגיעה ארוכה). */
    public interface Index1 {
        void ok(int which);
    }

    /** כמו list, ועם נגיעה ארוכה על פריט: החלון נסגר ו-longPress נקרא. */
    public static void list(Activity a, String title, List<String> items,
                            DialogInterface.OnClickListener l, final Index1 longPress) {
        final AlertDialog d = new AlertDialog.Builder(a)
                .setTitle(title)
                .setAdapter(new ArrayAdapter<String>(a, android.R.layout.simple_list_item_1, items), l)
                .create();
        d.show();
        if (longPress != null)
            d.getListView().setOnItemLongClickListener(new android.widget.AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(android.widget.AdapterView<?> p, View v, int pos, long id) {
                    d.dismiss();
                    longPress.ok(pos);
                    return true;
                }
            });
    }

    public static void list(Activity a, String title, List<String> items,
                            DialogInterface.OnClickListener l) {
        new AlertDialog.Builder(a)
                .setTitle(title)
                .setAdapter(new ArrayAdapter<String>(a, android.R.layout.simple_list_item_1, items), l)
                .show();
    }

    // ---------- טפסים ----------

    public static LinearLayout form(Context c) {
        LinearLayout b = new LinearLayout(c);
        b.setOrientation(LinearLayout.VERTICAL);
        b.setLayoutDirection(DIRECTION);
        int p = dp(c, 18);
        b.setPadding(p, dp(c, 6), p, 0);
        return b;
    }

    public static TextView label(Context c, String s) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(12);
        t.setTextColor(SUB);
        t.setPadding(0, dp(c, 10), 0, 0);
        return t;
    }

    public static EditText field(Context c, String value, String hint) {
        EditText e = new EditText(c);
        e.setSingleLine(true);
        e.setText(value == null ? "" : value);
        if (hint != null) e.setHint(hint);
        e.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        return e;
    }

    public static CheckBox check(Context c, String text, boolean on) {
        CheckBox b = new CheckBox(c);
        b.setText(text);
        b.setChecked(on);
        return b;
    }

    public static Spinner choice(Context c, String[] options, int selected) {
        Spinner s = new Spinner(c);
        // 197: השורה הנבחרת — simple_spinner_item (נמדדת נכון, והחץ לא עולה על הטקסט); הרשימה הנפתחת — כמו קודם
        ArrayAdapter<String> ad = new ArrayAdapter<String>(c, android.R.layout.simple_spinner_item, options);
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s.setAdapter(ad);
        if (selected >= 0 && selected < options.length) s.setSelection(selected);
        return s;
    }

    public static int indexOf(String[] options, String value, int def) {
        for (int i = 0; i < options.length; i++) if (options[i].equals(value)) return i;
        return def;
    }

    /** 150: שורה בראש הטופס הבא שנפתח (הלשוניות של "הוספת חיפוש"); close — סוגר את הטופס. */
    public interface Header {
        View build(Runnable close);
    }

    private static Header header;

    public static void formHeader(Header h) {
        header = h;
    }

    /** 197: כאן נכנס הפס שבראש הטופס (formHeader), כשהטופס קובע לו מקום — ולא בשורה הראשונה. */
    public static final String HEADER_SLOT = "header-slot";  // תחביר

    public static void askForm(Activity a, String title, LinearLayout content, final Runnable onOk) {
        askForm(a, title, content, onOk, false);
    }

    /** 197: טופס שהכותרת שלו בתוכו (בשורה אחת עם השם) — בלי כותרת החלון, ורחב יותר. */
    public static void askFormWide(Activity a, LinearLayout content, final Runnable onOk) {
        askForm(a, null, content, onOk, true);
    }

    private static void askForm(Activity a, String title, LinearLayout content, final Runnable onOk, boolean wide) {
        final Header top = header;
        header = null;
        ScrollView sv = new ScrollView(a);
        sv.addView(content);
        AlertDialog.Builder bld = new AlertDialog.Builder(a)
                .setView(sv)
                .setPositiveButton(Words.SAVE, null)
                .setNegativeButton(Words.CANCEL, null);
        if (title != null) bld.setTitle(title);
        final AlertDialog d = bld.create();
        if (d.getWindow() != null)
            d.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        if (top != null) {
            View h = top.build(new Runnable() {
                @Override public void run() { d.dismiss(); }
            });
            View slot = content.findViewWithTag(HEADER_SLOT);
            if (slot instanceof LinearLayout) ((LinearLayout) slot).addView(h);
            else content.addView(h, 0);
        }
        d.show();
        if (wide && d.getWindow() != null) {                                  // 10 מכל צד של המסך; בטאבלט — עד 560
            int w = a.getResources().getDisplayMetrics().widthPixels - dp(a, 20);
            d.getWindow().setLayout(Math.min(w, dp(a, 560)), WindowManager.LayoutParams.WRAP_CONTENT);
        }
        // 98: הטופס נסגר רק אם השמירה עברה. טופס שבדיקת התקינות שלו נכשלה קורא keepFormOpen(),
        // ונשאר פתוח עם כל מה שמולא בו.
        d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                formStays = false;
                onOk.run();
                if (!formStays) d.dismiss();
                formStays = false;
            }
        });
    }

    private static boolean formStays;

    /** 98: נקרא מתוך onOk של askForm כשחסר משהו — הטופס לא נסגר. */
    public static void keepFormOpen() {
        formStays = true;
    }

    // ---------- "מה קרה" (80): מסך שמקשיב לשינויים באינדקס ----------

    /**
     * מסך פתוח מגיב לשינוי באינדקס לפי הכלל שלו (run), בחוט המסך, פעם אחת לכל גל שינויים.
     * on() ב-onResume, off() ב-onPause.
     */
    public static final class Watch implements Library.Listener {
        private final Activity a;
        private final Runnable run;
        private boolean posted;

        public Watch(Activity a, Runnable run) {
            this.a = a;
            this.run = run;
        }

        @Override
        public void onChange(Library.Change c) {
            synchronized (this) {
                if (posted) return;
                posted = true;
            }
            a.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    synchronized (Watch.this) {
                        posted = false;
                    }
                    if (!a.isFinishing()) run.run();
                }
            });
        }

        public void on() {
            Library.get(a).listen(this);
            Library.get(a).returned();                                   // 110
        }

        public void off() {
            Library.get(a).unlisten(this);
            Library.get(a).left();
        }
    }

    /** done ירוץ בחוט המסך אחרי שהשינויים שנשלחו לאינדקס כבר מוצגים (במקום המתנה של רגע). */
    public static void afterIndex(final Activity a, final Runnable done) {
        if (done == null) return;
        Library.get(a).whenApplied(new Runnable() {
            @Override
            public void run() {
                a.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!a.isFinishing()) done.run();
                    }
                });
            }
        });
    }
}
