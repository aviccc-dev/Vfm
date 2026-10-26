package com.tikiyot.vfm;

// 1.12.0: מצייר לפי עיצוב (Design) — שורות או רשת, הצלמית וסביבה, הטקסט (גודל, משקל, גופן), "התאם למקום".
// השורה נבנית כאן, ומה שבתוכה ממלאות הפונקציות הקיימות (Ui.bindIcon / bindRow) — אותו מבנה {צלמית, שם, נתונים, עוד}.
// אחר כך apply מתאים את מה שמולא לעיצוב. בלי עיצוב (null) — Ui.row, בדיוק כמו קודם.

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class Looks {

    /** שמות הצבעים בקובץ העיצוב. */
    private static final String[] COLOR_NAMES = {"ירוק", "סגול", "כתום", "אפור", "כחול", "אדום", "צהוב", "ורוד"};  // תחביר
    private static final int[] COLORS = {0xFF0F6E56, 0xFF534AB7, 0xFFBA7517, 0xFF5F5E5A, 0xFF1A73E8, 0xFFC5221F,
            0xFFEF9F27, 0xFFD4537E};
    /** קובצי הגופנים והצלמיות החיצוניים — בתוך תיקיית האופן (עוברים איתו). */
    static final String FONTS = "גופנים", ICONS = "צלמיות";  // בכונן
    /** "התאם למקום": עד כמה מותר להצר את האותיות (החלטה 3: בערך 85%). */
    static final float NARROW = 0.85f;
    /** רוחב הצלמית בשורות היום (54dp) — היחס שלפיו מוגדלים הכיתובים שבתוכה. */
    private static final float ROW_BOX_DP = 54f;

    private Looks() { }

    /** צבע לפי שם או ‎#RRGGBB; לא מובן — def. */
    static int color(String v, int def) {
        if (v == null || v.trim().isEmpty()) return def;
        v = v.trim();
        for (int i = 0; i < COLOR_NAMES.length; i++) if (COLOR_NAMES[i].equals(v)) return COLORS[i];
        try {
            if (v.startsWith("#")) {
                long c = Long.parseLong(v.substring(1), 16);
                return (int) (v.length() <= 7 ? 0xFF000000L | c : c);
            }
        } catch (NumberFormatException ignored) {
        }
        return def;
    }

    private static final Map<String, Typeface> fonts = new HashMap<String, Typeface>();

    /** הגופן והמשקל. גופן חיצוני — מתיקיית "גופנים" של האופן, או מסלול מלא; לא נטען — גופן המערכת. */
    static Typeface face(Design.Text t) {
        Typeface base = Typeface.DEFAULT;
        if (t.font != null && !t.font.isEmpty() && !Design.SYSTEM.equals(t.font)) {
            Typeface f = fonts.get(t.font);
            if (f == null && !fonts.containsKey(t.font)) {
                try {
                    File file = t.font.startsWith("/") ? new File(t.font) : new File(new File(Paths.work(), FONTS), t.font);
                    if (file.isFile()) f = Typeface.createFromFile(file);
                } catch (Throwable e) {
                    Log.e("טעינת גופן " + t.font, e);
                }
                fonts.put(t.font, f);
            }
            if (f != null) base = f;
        }
        if (Build.VERSION.SDK_INT >= 28) return Typeface.create(base, t.weight, false);
        return Typeface.create(base, t.weight >= 600 ? Typeface.BOLD : Typeface.NORMAL);
    }

    static void text(TextView v, Design.Text t, float size) {
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        v.setTypeface(face(t));
    }

    // ---------- הבנייה ----------

    /** רוחב אריח (פיקסלים) לרוחב המסך הזה — ברשת, וגם בשורות בכמה טורים. */
    static int tileWidth(Context c, Design d) {
        int w = c.getResources().getDisplayMetrics().widthPixels;
        return w / Math.max(1, d.cols);                                     // גם שורות בכמה טורים (כמו בשיאומי)
    }

    /** גודל הצלמית בפיקסלים: ברשת — אחוז מרוחב האריח; בשורות — אחוז מגודל השם. */
    static int iconPx(Context c, Design d, int tileW) {
        if (d.grid) return tileW * d.icon / 100;
        return (int) (sp(c, d.nameT.size) * d.icon / 100f);
    }

    static float sp(Context c, float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, c.getResources().getDisplayMetrics());
    }

    /** שורה או אריח — עם אותו מבנה כמו Ui.row: {צלמית, שם, נתונים, עוד}. */
    static View row(Context c, View convert, Design d, int tileW) {
        if (convert != null && convert.getTag() instanceof TextView[]) return convert;
        int icon = iconPx(c, d, tileW);
        LinearLayout row = new LinearLayout(c);
        row.setLayoutDirection(Ui.DIRECTION);
        TextView box = new TextView(c);
        box.setGravity(Gravity.CENTER);
        box.setTextColor(Ui.ON_COLOR);
        int bp = Ui.dp(c, 3);
        box.setPadding(bp, bp, bp, bp);
        TextView title = new TextView(c), sub = new TextView(c), more = new TextView(c), strip = null;
        title.setTextColor(Ui.TEXT);
        sub.setTextColor(Ui.MUTED);
        more.setTextColor(Ui.MUTED);
        more.setVisibility(View.GONE);
        if (d.grid) {
            int pad = Math.max(Ui.dp(c, 2), tileW * d.gap / 200);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setGravity(Gravity.CENTER_HORIZONTAL);
            row.setPadding(pad, pad, pad, pad);
            row.addView(box, new LinearLayout.LayoutParams(icon, icon));
            strip = new TextView(c);                                          // 1.17.0: הסוג — פס בתחתית הצלמית (ב')
            strip.setGravity(Gravity.CENTER);
            strip.setTextColor(Ui.ON_COLOR);
            strip.setSingleLine(true);
            strip.setTypeface(Typeface.DEFAULT_BOLD);
            strip.setVisibility(View.GONE);
            row.addView(strip, new LinearLayout.LayoutParams(icon, LinearLayout.LayoutParams.WRAP_CONTENT));
            for (TextView t : new TextView[]{title, sub}) {
                t.setGravity(Gravity.CENTER);
                t.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                row.addView(t, new LinearLayout.LayoutParams(-1, -2));
            }
            row.addView(more);
        } else {
            int pad = (int) (sp(c, d.nameT.size) * d.gap / 100f);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(Ui.dp(c, 12), pad, Ui.dp(c, 12), pad);
            row.addView(box, new LinearLayout.LayoutParams(icon, LinearLayout.LayoutParams.WRAP_CONTENT));
            LinearLayout texts = new LinearLayout(c);
            texts.setOrientation(LinearLayout.VERTICAL);
            texts.setPadding(Ui.dp(c, 10), 0, 0, 0);
            title.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
            sub.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
            more.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
            texts.addView(title);
            if (d.dataBelow) texts.addView(sub);
            texts.addView(more);
            row.addView(texts, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            if (!d.dataBelow) {                                               // החלטה 2: הנתונים בצד, מול השם
                sub.setPadding(Ui.dp(c, 8), 0, 0, 0);
                sub.setMaxWidth(tileW * 2 / 5);
                row.addView(sub, new LinearLayout.LayoutParams(-2, -2));
            }
        }
        row.setTag(strip == null ? new TextView[]{box, title, sub, more} : new TextView[]{box, title, sub, more, strip});
        return row;
    }

    /** אחרי שהשורה מולאה (Ui.bindIcon / bindRow): התאמה לעיצוב. icon / color — מהחלק הפרטי ("" — אין). */
    static void apply(Context c, View v, Design d, int tileW, String icon, String color, float[] fitted) {
        TextView[] t = (TextView[]) v.getTag();
        TextView box = t[0], title = t[1], sub = t[2], more = t[3];
        int px = iconPx(c, d, tileW);
        int bg = box.getBackground() instanceof ColorDrawable ? ((ColorDrawable) box.getBackground()).getColor() : 0;
        if ((bg >>> 24) == 0) bg = 0;
        box.setCompoundDrawables(null, null, null, null);
        TextView strip = t.length > 4 ? t[4] : null;
        if (strip != null) strip.setVisibility(View.GONE);
        String kind = Ui.kindOf(v);
        int kindBg = 0;
        if (kind != null) {                                                   // 1.17.0: הסוג תמיד, והצלמית בנוסף (המילון, 24)
            String own = Ui.userIconOf(v);
            if (own.isEmpty() && icon != null) own = icon;
            if (d.grid) {                                                     // ב': הצלמית באריח, הסוג בפס מתחת
                String txt = title.getText().toString();
                if (!own.isEmpty() && txt.startsWith(own + "  ")) title.setText(txt.substring(own.length() + 2));
                if (strip != null) {
                    strip.setText(Ui.tagShort(kind));
                    strip.setBackgroundColor(Ui.kindColor(kind));
                    strip.setTextSize(TypedValue.COMPLEX_UNIT_PX, Math.max(sp(c, 11), px * 0.16f));
                    strip.setVisibility(View.VISIBLE);
                }
                icon = !own.isEmpty() ? own : Ui.tag(kind) != null ? kind : "📄";
                box.setBackgroundColor(0x00000000);
                box.setTextColor(Ui.TEXT);
                bg = 0;
            } else {                                                          // א': התווית במקומה, הצלמית לפני השם
                kindBg = bg;
                if (own.contains(".")) {
                    Drawable img = image(c, own, (int) sp(c, d.nameT.size * 1.2f));
                    if (img != null) title.setCompoundDrawablesRelative(img, null, null, null);
                }
                icon = null;
            }
        }
        if (icon != null && !icon.isEmpty()) {
            Drawable img = image(c, icon, px);
            if (img != null) {
                box.setText("");
                box.setCompoundDrawables(null, img, null, null);
            } else {
                box.setSingleLine(true);
                box.setText(icon);
                box.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            }
        }
        int own = color(color, 0);
        bg = kindBg != 0 ? kindBg : color(color, bg);                        // 1.17.0: תווית הסוג — בצבע הסוג, תמיד
        // הכיתוב שבתוך הצלמית — בקנה מידה של הצלמית (בשורות, בגודל של היום: יחס 1)
        float scale = px / (float) Ui.dp(c, ROW_BOX_DP);
        CharSequence label = box.getText();
        boolean emoji = label.length() > 0 && label.length() <= 4 && !Character.isLetterOrDigit(label.charAt(0));   // 🗂️ — עם בורר צורה
        if (emoji && d.grid) box.setTextSize(TypedValue.COMPLEX_UNIT_PX, px * 0.5f);
        else box.setTextSize(TypedValue.COMPLEX_UNIT_PX, box.getTextSize() * scale);
        float r = Ui.dp(c, d.grid ? 12 : 6);
        if (Design.FILL.equals(d.around)) {
            box.setTextColor(Ui.ON_COLOR);
            box.setBackground(bg == 0 ? null : rounded(bg, r, 0, 0));
        } else {
            box.setTextColor(bg == 0 ? Ui.TEXT : bg);
            box.setBackground(Design.FRAME.equals(d.around) ? rounded(0, r, Ui.dp(c, 1), bg == 0 ? Ui.RULE : bg) : null);
        }
        box.setVisibility(d.showIcon ? View.VISIBLE : View.GONE);
        // השם
        boolean full = !d.grid && title.getMaxLines() > d.lines + 2;          // "נתונים מלאים" — השם המלא נשאר
        text(title, d.nameT, fitted != null ? fitted[0] : d.nameT.size);
        title.setTextScaleX(fitted != null ? fitted[1] : 1f);
        if (!full) {
            title.setMaxLines(d.lines);
            title.setEllipsize(TextUtils.TruncateAt.END);
        }
        // הנתונים
        text(sub, d.data, d.data.size);
        text(more, d.data, d.data.size);
        boolean hasData = d.showData && sub.getText().length() > 0;
        if (d.grid || d.cols > 1) {                                           // גובה אחיד לכל אריח (עיצוב 1; 1.12.1 — גם שורות בטורים)
            title.setMinLines(d.lines);
            sub.setSingleLine(true);
            sub.setEllipsize(TextUtils.TruncateAt.END);
            sub.setVisibility(hasData ? View.VISIBLE : d.showData ? View.INVISIBLE : View.GONE);
            more.setVisibility(View.GONE);
        } else {
            title.setMinLines(1);
            sub.setVisibility(hasData ? View.VISIBLE : View.GONE);
        }
        // צבע רקע לפריט
        int tint = kindBg != 0 ? own : bg;                                    // הצבע של המשתמש — לרקע הפריט
        v.setBackground(d.tint ? rounded(tint == 0 ? Ui.BG_SOFT : soft(tint), Ui.dp(c, 10), 0, 0) : null);
        if (d.tint && tint != 0) title.setTextColor(darker(tint));
    }

    /** אחרי apply: הבחירה (או ההתאמה) — גוברת על צבע הרקע של העיצוב, בלי לאבד את הפינות. */
    static void mark(Context c, View v, Design d, int color) {
        if ((color >>> 24) == 0) return;
        v.setBackground(rounded(color, Ui.dp(c, d.tint || d.grid ? 10 : 0), 0, 0));
    }

    static GradientDrawable rounded(int fill, float radius, int stroke, int strokeColor) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(radius);
        if (stroke > 0) g.setStroke(stroke, strokeColor);
        return g;
    }

    /** גוון בהיר של הצבע (רקע לאריח). */
    static int soft(int c) {
        int r = (c >> 16) & 0xFF, g = (c >> 8) & 0xFF, b = c & 0xFF;
        r = r + (255 - r) * 85 / 100;
        g = g + (255 - g) * 85 / 100;
        b = b + (255 - b) * 85 / 100;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    static int darker(int c) {
        int r = (c >> 16) & 0xFF, g = (c >> 8) & 0xFF, b = c & 0xFF;
        return 0xFF000000 | ((r * 7 / 10) << 16) | ((g * 7 / 10) << 8) | (b * 7 / 10);
    }

    private static final Map<String, Drawable> images = new HashMap<String, Drawable>();

    /** צלמית מקובץ תמונה — מתיקיית "צלמיות" של האופן, או מסלול מלא. מוקטנת בטעינה; חסרה — null. */
    static Drawable image(Context c, String icon, int px) {
        String low = icon.toLowerCase(java.util.Locale.ROOT);
        if (!(low.endsWith(".png") || low.endsWith(".jpg") || low.endsWith(".jpeg") || low.endsWith(".webp"))) return null;
        String k = icon + "@" + px;
        if (images.containsKey(k)) return images.get(k);
        Drawable out = null;
        try {
            File f = icon.startsWith("/") ? new File(icon) : new File(Paths.work(), icon);
            if (!f.isFile()) f = new File(new File(Paths.work(), ICONS), icon);
            if (f.isFile()) {
                BitmapFactory.Options o = new BitmapFactory.Options();
                o.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(f.getAbsolutePath(), o);
                int s = 1;
                while (o.outWidth / (s * 2) >= px && o.outHeight / (s * 2) >= px) s *= 2;
                o = new BitmapFactory.Options();
                o.inSampleSize = s;
                Bitmap b = BitmapFactory.decodeFile(f.getAbsolutePath(), o);
                if (b != null) {
                    int size = px * 7 / 10;
                    out = new BitmapDrawable(c.getResources(), b);
                    out.setBounds(0, 0, size, size);
                }
            }
        } catch (Throwable e) {
            Log.e("טעינת צלמית " + icon, e);
        }
        images.put(k, out);
        return out;
    }

    // ---------- "התאם למקום" — גודל אחיד לכל האזור (החלטה 3) ----------

    /**
     * {גודל, הצרה} לכל השמות יחד: הגודל שנקבע; אם הארוך לא נכנס בשורה — הצרה עד 85%, ואז הקטנה עד הגודל המזערי.
     * מה שעדיין לא נכנס — יורד שורה (עד "שורות לשם"), ואז "…". null — בלי התאמה.
     */
    static float[] fit(Context c, Design d, List<String> names, int tileW) {
        if (!d.fit || names == null || names.isEmpty()) return null;
        int avail = d.grid ? tileW - 2 * Math.max(Ui.dp(c, 2), tileW * d.gap / 200) - Ui.dp(c, 6)
                : tileW - iconPx(c, d, tileW) - Ui.dp(c, 34) - (d.dataBelow ? 0 : tileW * 2 / 5);
        if (avail <= 0) return null;
        Paint p = new Paint();
        p.setTypeface(face(d.nameT));
        p.setTextSize(sp(c, d.nameT.size));
        float widest = 0;
        for (String n : names) if (n != null) widest = Math.max(widest, p.measureText(n));   // בשורה אחת — הסדר שהוחלט
        if (widest <= avail) return new float[]{d.nameT.size, 1f};
        float f = avail / widest;
        float narrow = Math.max(NARROW, f);
        float size = Math.max(d.min, d.nameT.size * f / narrow);
        return new float[]{Math.min(size, d.nameT.size), narrow};
    }
    // ---------- מסך הבית (1.12.0) ----------

    /** "22,448 קבצים · 44.71GB" ← כל חלק בשורה משלו (1.12.1: בלי "·" תלוי בשבירה). */
    static String lines(String s) {
        return s == null ? "" : s.replace(" · ", "\n").replace("   ·   ", "\n");
    }

    /** צבע לפי סוג הכלי — כמו התוויות (160): ידנית ירוק, חכמה סגול; השאר אפור. */
    static int kindColor(String kind) {
        if (FnVirtual.KIND.equals(kind)) return Ui.tagColor(Ui.MANUAL);
        if (FnSmart.KIND.equals(kind)) return Ui.tagColor(Ui.SMART);
        return Ui.tagColor(Drive.icon(true));
    }

    /** אריח של מסך הבית לפי העיצוב: ברשת — צלמית למעלה ושם מתחת; בשורות — צלמית בצד. */
    /** 1.17.0: פס הסוג שבתחתית הצלמית (ב') — בצבע הסוג, עם המילה. */
    static TextView strip(Context c, String tag, int width) {
        TextView s = new TextView(c);
        s.setText(Ui.tagShort(tag));
        s.setGravity(Gravity.CENTER);
        s.setTextColor(Ui.ON_COLOR);
        s.setSingleLine(true);
        s.setTypeface(Typeface.DEFAULT_BOLD);
        s.setTextSize(TypedValue.COMPLEX_UNIT_PX, Math.max(sp(c, 11), width * 0.16f));
        s.setBackgroundColor(Ui.kindColor(tag));
        return s;
    }

    static View homeTile(Context c, Design d, String icon, String tag, String name, String kind, int w,
                         View.OnClickListener click, View.OnLongClickListener longClick, TextView stat, int color) {
        LinearLayout t = new LinearLayout(c);
        t.setLayoutDirection(Ui.DIRECTION);
        int px = d.grid ? w * d.icon / 100 : (int) (sp(c, d.nameT.size) * d.icon / 100f);
        TextView ic = new TextView(c);
        ic.setText(icon);
        ic.setGravity(Gravity.CENTER);
        ic.setTextSize(TypedValue.COMPLEX_UNIT_PX, px * 0.5f);
        float r = Ui.dp(c, d.grid ? 12 : 6);
        if (Design.FILL.equals(d.around)) ic.setBackground(rounded(color == 0 ? Ui.BG_SOFT : soft(color), r, 0, 0));
        else if (Design.FRAME.equals(d.around)) ic.setBackground(rounded(0, r, Ui.dp(c, 1), color == 0 ? Ui.RULE : color));
        ic.setVisibility(d.showIcon ? View.VISIBLE : View.GONE);
        TextView nm = new TextView(c);
        nm.setText(name);
        text(nm, d.nameT, d.nameT.size);
        nm.setTextColor(Ui.TEXT);
        nm.setMaxLines(d.lines);
        nm.setEllipsize(TextUtils.TruncateAt.END);
        TextView kl = new TextView(c);
        kl.setText(kind);
        kl.setTextColor(Ui.MUTED);
        text(kl, d.data, d.data.size);
        kl.setVisibility(d.showData && !kind.isEmpty() ? View.VISIBLE : View.GONE);
        stat.setTextColor(Ui.SUB);
        stat.setText(lines(stat.getText().toString()));                       // 1.12.1: בלי "·" תלוי
        text(stat, d.data, d.data.size);
        stat.setVisibility(d.showData ? View.VISIBLE : View.GONE);
        int pad = d.grid ? Math.max(Ui.dp(c, 4), w * d.gap / 200) : (int) (sp(c, d.nameT.size) * d.gap / 100f);
        if (d.grid) {
            t.setOrientation(LinearLayout.VERTICAL);
            t.setGravity(Gravity.CENTER_HORIZONTAL);
            t.setPadding(pad, pad, pad, pad);
            t.addView(ic, new LinearLayout.LayoutParams(px, px));
            if (tag != null) t.addView(strip(c, tag, px), new LinearLayout.LayoutParams(px, -2));   // ב'
            for (TextView x : new TextView[]{nm, kl, stat}) {
                x.setGravity(Gravity.CENTER);
                t.addView(x, new LinearLayout.LayoutParams(-1, -2));
            }
            nm.setMinLines(d.lines);                                          // גובה אחיד
        } else {
            t.setOrientation(LinearLayout.HORIZONTAL);
            t.setGravity(Gravity.CENTER_VERTICAL);
            t.setPadding(Ui.dp(c, 8), pad, Ui.dp(c, 8), pad);
            if (tag != null) {                                                // א': התווית במקום הצלמית, והצלמית לפני השם
                Ui.icon(ic, tag);
                ic.setTextSize(TypedValue.COMPLEX_UNIT_PX, Math.max(sp(c, 9), px * 0.2f));
                ic.setBackground(rounded(Ui.kindColor(tag), r, 0, 0));
                if (icon != null && !icon.isEmpty() && !icon.equals(tag)) nm.setText(icon + "  " + name);
            }
            t.addView(ic, new LinearLayout.LayoutParams(px, px));
            LinearLayout texts = new LinearLayout(c);
            texts.setOrientation(LinearLayout.VERTICAL);
            texts.setPadding(Ui.dp(c, 10), 0, 0, 0);
            for (TextView x : new TextView[]{nm, kl, stat}) {
                x.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
                texts.addView(x);
            }
            t.addView(texts, new LinearLayout.LayoutParams(0, -2, 1f));
        }
        if (d.tint) {
            t.setBackground(rounded(color == 0 ? Ui.BG_SOFT : soft(color), Ui.dp(c, 10), 0, 0));
            if (color != 0) nm.setTextColor(darker(color));
        }
        android.widget.GridLayout.LayoutParams lp = new android.widget.GridLayout.LayoutParams();
        lp.width = w;
        lp.setGravity(Gravity.FILL_VERTICAL);                                 // 1.12.1: גובה אחיד בכל שורה
        int m = Ui.dp(c, 4);
        lp.setMargins(m, m, m, m);
        t.setLayoutParams(lp);
        t.setOnClickListener(click);
        if (longClick != null) t.setOnLongClickListener(longClick);
        return t;
    }
}
