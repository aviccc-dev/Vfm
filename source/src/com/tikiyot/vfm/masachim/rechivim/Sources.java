package com.tikiyot.vfm;

// רכיב המקורות — אחד לכל התוכנה. אחראי: בחירת תיקיות וכוננים בנגיעה, הצגתם כשורות קריאות
// עם ✕ להסרה, וכתיבתם בתנאי כרכיב אחד: מקור:«פנימי|/storage/…/Download». "או" בין כל המקורות.

import android.app.Activity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class Sources {

    public static final String INTERNAL = Drive.INTERNAL;
    public static final String CARD = Drive.CARD;

    public final LinearLayout view;
    private final LinearLayout rows;
    private final TextView empty;
    private Button clear;
    private final Activity a;
    private final List<String> items = new ArrayList<String>();
    private Runnable changed;

    /** emptyText: מה פירוש "אין מקורות" במקום הזה, במילים ("כל הכוננים"). */
    public Sources(Activity a, String emptyText) {
        this.a = a;
        view = new LinearLayout(a);
        view.setOrientation(LinearLayout.VERTICAL);
        rows = new LinearLayout(a);
        rows.setOrientation(LinearLayout.VERTICAL);
        empty = new TextView(a);
        empty.setText(emptyText);
        empty.setTextColor(Ui.MUTED);
        empty.setTextSize(13);
        empty.setPadding(Ui.dp(a, 4), 0, Ui.dp(a, 8), 0);
        empty.setGravity(android.view.Gravity.CENTER_VERTICAL);
        empty.setMinHeight(Ui.dp(a, 48));
        view.addView(rows);

        Flow buttons = new Flow(a, 6, 0);                                     // 197: כל כפתור ברוחב שלו; גולש
        buttons.addView(empty);                                               // 198: "כל הכוננים" — באותה שורה עם הכפתורים
        buttons.addView(button(Words.Src.FOLDER_BTN, new Runnable() {
            @Override
            public void run() {
                FolderPicker.choose(Sources.this.a, new FolderPicker.Picked() {
                    @Override public void ok(File folder) { add(folder.getAbsolutePath()); }
                });
            }
        }));
        buttons.addView(button(Words.Src.DRIVE_BTN, new Runnable() {
            @Override
            public void run() {
                Ui.options(Sources.this.a, Words.Src.DRIVE, new String[]{Drive.INTERNAL_NAME, Drive.CARD_NAME},
                        new android.content.DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(android.content.DialogInterface d, int w) { add(w == 0 ? INTERNAL : CARD); }
                        });
            }
        }));
        clear = button(Words.Src.CLEAR, new Runnable() {
            @Override
            public void run() {
                items.clear();
                show();
            }
        });
        buttons.addView(clear);
        view.addView(buttons);
        show();
    }

    private Button button(String label, final Runnable r) {
        Button b = new Button(a);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(13);
        b.setMinWidth(0);                                                     // 199: בלי הרוחב המינימלי של המערכת —
        b.setMinimumWidth(0);                                                 // שלושת הכפתורים בשורה אחת
        b.setMinHeight(Ui.dp(a, 40));
        b.setMinimumHeight(Ui.dp(a, 40));
        b.setPadding(Ui.dp(a, 8), 0, Ui.dp(a, 8), 0);
        b.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { r.run(); }
        });
        return b;
    }

    public void onChange(Runnable r) {
        changed = r;
    }

    public void add(String source) {
        if (source == null || source.isEmpty() || items.contains(source)) return;
        items.add(source);
        show();
    }

    public void set(List<String> list) {
        items.clear();
        for (String s : list) if (!items.contains(s)) items.add(s);
        show();
    }

    public List<String> get() {
        return new ArrayList<String>(items);
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    private void show() {
        rows.removeAllViews();
        empty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        if (clear != null) clear.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);   // 197: אין מה לנקות
        for (final String s : new ArrayList<String>(items)) {
            LinearLayout row = new LinearLayout(a);
            row.setLayoutDirection(Ui.DIRECTION);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            TextView t = new TextView(a);
            t.setText(label(s));
            t.setTextColor(Ui.TEXT);
            t.setTextSize(15);
            row.addView(t, new LinearLayout.LayoutParams(0, -2, 1f));
            TextView x = new TextView(a);
            x.setText("  ✕  ");
            x.setTextSize(18);
            x.setTextColor(Ui.MUTED);
            x.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    items.remove(s);
                    show();
                }
            });
            row.addView(x);
            rows.addView(row);
        }
        if (changed != null) changed.run();
    }

    // ---------- מילים ותנאי ----------

    /** שורה קריאה: "📁 Download · כרטיס זיכרון" או "💾 כרטיס זיכרון". */
    public static String label(String s) {
        if (INTERNAL.equals(s)) return Drive.label(true, Drive.INTERNAL_NAME) + Words.Src.WHOLE;
        if (CARD.equals(s)) return Drive.label(false, Drive.CARD_NAME) + Words.Src.WHOLE;
        File f = new File(s);
        String drive = Drive.nameOf(s);
        return "📁 " + f.getName() + " · " + drive;
    }

    /** המקורות כתנאי אחד, או "" כשאין. */
    public static String toCond(List<String> list) {
        if (list.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (String s : list) sb.append(sb.length() > 0 ? "|" : "").append(s);
        return "מקור:" + Cond.quote(sb.toString());  // תחביר
    }

    /** המקורות שבתוך תנאי (כולל תחביר ישן: תיקייה:/מסלול, כונן:). */
    public static List<String> fromCond(String cond) {
        List<String> out = new ArrayList<String>();
        for (String tok : Cond.tokens(cond)) {
            int c = tok.indexOf(':');
            if (c < 0) continue;
            String k = tok.substring(0, c), v = tok.substring(c + 1);
            if (k.equals("מקור")) {  // תחביר
                for (String x : v.split("\\|")) if (!x.trim().isEmpty()) out.add(x.trim());
            } else if ((k.equals("תיקייה") || k.equals("תיקיה")) && oldPath(v)) {  // תחביר
                for (String x : v.split("\\|")) if (x.startsWith("/")) out.add(x.trim());
            } else if (k.equals("כונן")) {  // תחביר
                if (v.contains("פנימי")) out.add(INTERNAL);  // תחביר
                else if (v.contains("כרטיס")) out.add(CARD);  // תחביר
            }
        }
        return out;
    }

    /**
     * "תיקייה:/מסלול/מלא" (הצורה שלפני "מקור:") — מקור. אבל "תיקייה:/dcim/camera/" — שם תיקייה בכל מקום
     * (נגמר ב"/", 1.7.67): נשאר תנאי סינון. בלי זה הדוגמאות "תיקיות בעלות משמעות" הפכו למקור שאינו קיים.
     */
    static boolean oldPath(String v) {
        if (!v.startsWith("/")) return false;
        for (String x : v.split("\\|")) if (x.trim().endsWith("/")) return false;
        return true;
    }

    /** התנאי בלי רכיבי המקורות (מה שנשאר לסינון). */
    public static String rest(String cond) {
        StringBuilder sb = new StringBuilder();
        for (String tok : Cond.tokens(cond)) {
            int c = tok.indexOf(':');
            String k = c < 0 ? "" : tok.substring(0, c), v = c < 0 ? "" : tok.substring(c + 1);
            boolean src = k.equals("מקור") || k.equals("כונן")  // תחביר
                    || ((k.equals("תיקייה") || k.equals("תיקיה")) && oldPath(v));  // תחביר
            if (src) continue;
            sb.append(sb.length() > 0 ? " " : "").append(c < 0 ? tok : k + ":" + Cond.quote(v));
        }
        return sb.toString();
    }

    /** תיאור במילים של תנאי מקורות, לראש מסך. */
    public static String describe(String cond) {
        List<String> l = fromCond(cond);
        if (l.isEmpty()) return Words.Src.ALL_DRIVES;
        StringBuilder sb = new StringBuilder();
        for (String s : l) sb.append(sb.length() > 0 ? " + " : "").append(label(s).replace(Words.Src.WHOLE, ""));
        return sb.toString();
    }
}
