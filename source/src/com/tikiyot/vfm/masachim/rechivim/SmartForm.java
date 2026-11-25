package com.tikiyot.vfm;

// מסך הגדרה לתיקייה חכמה: שדות פשוטים, ושורת התנאי שנוצרת מהם.

import android.app.Activity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public final class SmartForm {

    /** 99: אפשרויות המיון — מהרכיב האחד (79), עם האפשרות המיוחדת של רשימה חכמה ("דירוג"). */

    private SmartForm() { }

    /**
     * מבנה 281 (אפיון החיפוש, 7–8; המשתמש: "כאשר אני נוגע בשורת החיפוש, מה שקיים בה נצבע" · "הכל"): שורת החיפוש — נגיעה ראשונה
     * בוחרת את כל מה שכתוב (הקלדה מחליפה; נגיעה נוספת — הסמן במקום), ו־✕ בקצה מנקה בנגיעה אחת (רק כשיש מה לנקות).
     */
    /** מבנה 306: כל שינוי בשדה — מריץ. */
    public static void onChange(EditText field, final Runnable run) {
        field.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int af) { }
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) { }
            @Override public void afterTextChanged(Editable e) { run.run(); }
        });
    }

    public static LinearLayout fieldRow(final Activity a, final EditText input) {
        input.setSelectAllOnFocus(true);
        LinearLayout row = new LinearLayout(a);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.addView(input, new LinearLayout.LayoutParams(0, -2, 1f));
        final TextView clear = new TextView(a);
        clear.setText(CLEAR);
        clear.setTextSize(18);
        clear.setTextColor(Ui.SUB);
        clear.setGravity(android.view.Gravity.CENTER);
        clear.setContentDescription(Words.Find.CLEAR);
        clear.setVisibility(input.getText().length() > 0 ? View.VISIBLE : View.GONE);
        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                input.setText("");
                input.requestFocus();
                android.view.inputmethod.InputMethodManager im =
                        (android.view.inputmethod.InputMethodManager) a.getSystemService(Activity.INPUT_METHOD_SERVICE);
                if (im != null) im.showSoftInput(input, 0);
            }
        });
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int af) { }
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) { }
            @Override public void afterTextChanged(Editable e) { clear.setVisibility(e.length() > 0 ? View.VISIBLE : View.GONE); }
        });
        row.addView(clear, new LinearLayout.LayoutParams(Ui.dp(a, 44), Ui.dp(a, 44)));
        return row;
    }

    private static final String CLEAR = "✕";  // תחביר

    /** מבנה 286: התנאי בלי הרכיבים של מסנן אחד (כל רכיב — פעם אחת; השאר — כמו שהיו, עם «» כשיש רווח). */
    static String without(String all, String token) {
        List<String> gone = Cond.tokens(token);
        StringBuilder rest = new StringBuilder();
        for (String t : Cond.tokens(all)) if (!gone.remove(t)) rest.append(rest.length() > 0 ? " " : "").append(Cond.quoteToken(t));
        return rest.toString();
    }

    /** חלקי הטופס — משותפים לרשימה חכמה ולחיפוש: מקורות, סוגים, תאריך, גודל, מילים. */
    public static final class Parts {
        /** 145: סוגי הקבצים — המודל (Types.Choice) והקבוצות מההגדרות. */
        final Types.Choice kinds = new Types.Choice();
        final List<Types.Group> groups = Types.groups();
        /** 198: "הכול" — תיבה ראשונה בין הסוגים (במקום שורת "הכול / רק מה שאבחר"). */
        final CheckBox allKinds;
        final List<CheckBox> groupBoxes = new ArrayList<CheckBox>();
        final CheckBox otherBox;
        /** 175: רק תיקיות ריקות. */
        CheckBox empty;
        final Button more;
        /** 199: התיבות — הכול, הסוגים, "עוד סיומות"; ובטופס הרשימה גם "רק תיקיות ריקות". */
        final Flow cells;
        /** 201: השורה הראשונה — "תיקיות ריקות" ו"הכול"; הסוגים — מתחתיה. */
        final Flow modes;
        final LinearLayout pickArea;
        /** 128: תאריך וגודל — הרכיב המשותף (במקום רשימות קבועות). */
        final DateSize ds = new DateSize();

        {
            ds.allowAdded = true;                                             // 189
        }
        LinearLayout filterView;
        Runnable onChange;
        EditText words;
        Sources src;

        public Parts(final Activity a, String emptySources) {
            empty = Ui.check(a, Words.Empty.TITLE, false);
            allKinds = Ui.check(a, Words.Kinds.ALL, true);
            allKinds.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            pickArea = new LinearLayout(a);
            pickArea.setOrientation(LinearLayout.VERTICAL);
            cells = new Flow(a, 8, 0);                                        // 197: כל תיבה ברוחב שלה; גולש לשורה הבאה
            modes = new Flow(a, 16, 0);
            modes.addView(allKinds);
            for (Types.Group g : groups) {
                CheckBox c = Ui.check(a, g.name, false);
                groupBoxes.add(c);
                cells.addView(c);
            }
            otherBox = Ui.check(a, Types.OTHER, false);
            cells.addView(otherBox);
            int h = Ui.dp(a, 38);                                             // 198: צפוף יותר מ־48 של המערכת
            for (int i = 0; i < cells.getChildCount(); i++) {
                cells.getChildAt(i).setMinimumHeight(h);
                if (cells.getChildAt(i) instanceof TextView) {
                    ((TextView) cells.getChildAt(i)).setMinHeight(h);
                    ((TextView) cells.getChildAt(i)).setTextSize(13);         // 200: "קובצי נתונים" ו"שאר הקבצים" — בשורה אחת
                }
            }
            empty.setMinimumHeight(h);
            empty.setMinHeight(h);
            empty.setTextSize(13);
            allKinds.setMinimumHeight(h);
            allKinds.setMinHeight(h);
            allKinds.setTextSize(13);
            more = new Button(a);
            more.setText(Words.Kinds.MORE);
            more.setAllCaps(false);
            more.setTextSize(13);
            more.setTextColor(Ui.ACCENT);
            more.setBackground(null);
            more.setGravity(android.view.Gravity.CENTER_VERTICAL | android.view.Gravity.START);
            more.setPadding(Ui.dp(a, 4), 0, Ui.dp(a, 4), 0);
            more.setMinWidth(0);
            more.setMinimumWidth(0);
            more.setMinHeight(h);
            more.setMinimumHeight(h);
            more.setSingleLine(true);                                         // 199: הסיומות שנבחרו — על הכפתור עצמו
            more.setEllipsize(android.text.TextUtils.TruncateAt.END);
            cells.addView(more);
            pickArea.addView(modes, new LinearLayout.LayoutParams(-1, -2));
            pickArea.addView(cells, new LinearLayout.LayoutParams(-1, -2));
            more.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    TypePicker.choose(a, kinds.manual, new TypePicker.Picked() {
                        @Override
                        public void ok(java.util.Set<String> chosen) {
                            kinds.manual.clear();
                            kinds.manual.addAll(chosen);
                            changed();
                        }
                    });
                }
            });
            words = Ui.field(a, "", Words.SmartF.WORDS_OPT);
            src = new Sources(a, emptySources);
            show();
        }

        /** מהמסך אל המודל. */
        private void read() {
            kinds.all = allKinds.isChecked();
            kinds.groups.clear();
            for (int i = 0; i < groups.size(); i++) if (groupBoxes.get(i).isChecked()) kinds.groups.add(groups.get(i).name);
            kinds.other = otherBox.isChecked();
        }

        /** מהמודל אל המסך. */
        private void show() {
            allKinds.setChecked(kinds.all);
            for (int i = 0; i < groups.size(); i++) groupBoxes.get(i).setChecked(kinds.groups.contains(groups.get(i).name));
            otherBox.setChecked(kinds.other);
            enable();
        }

        private void enable() {
            // 198: "הכול" מסומן — הסוגים חיוורים (אבל אפשר לגעת: נגיעה בסוג מבטלת את "הכול")
            // 199: "רק תיקיות ריקות" — לתיקייה אין סוג: כל התיבות אפורות, ואי אפשר לבחור בהן
            boolean dirs = empty.isChecked();
            float alpha = dirs || allKinds.isChecked() ? 0.4f : 1f;
            allKinds.setEnabled(!dirs);
            allKinds.setAlpha(dirs ? 0.4f : 1f);
            for (CheckBox c : groupBoxes) {
                c.setAlpha(alpha);
                c.setEnabled(!dirs);
            }
            otherBox.setAlpha(alpha);
            otherBox.setEnabled(!dirs);
            more.setAlpha(alpha);
            more.setEnabled(!dirs);
            StringBuilder m = new StringBuilder();
            for (String e : kinds.manual) m.append(m.length() > 0 ? ", " : "").append(e);
            more.setText(m.length() == 0 ? Words.Kinds.MORE : Words.Kinds.picked(m.toString()));
        }

        private void changed() {
            read();
            enable();
            if (onChange != null) onChange.run();
        }

        /** נבחר "רק הסוגים שאבחר" — ולא נבחר אף אחד. */
        public boolean noKind() {
            read();
            return !empty.isChecked() && !kinds.all && kinds.empty();         // 175: תיקיות — בלי סוג
        }

        /** 153: לחלונית ההגדרות של מסך החיפוש — בלי שדה המילים (שם המילים בשורת ההקלדה). */
        public void addTo(Activity a, LinearLayout b) {
            b.addView(Ui.label(a, Words.SmartF.FROM_WHERE));
            b.addView(src.view);
            b.addView(Ui.label(a, Words.Kinds.TITLE));
            b.addView(pickArea);
            b.addView(empty);                                                 // 175
            filterView = FilterForm.build(a, ds, new Runnable() {
                @Override public void run() { if (onChange != null) onChange.run(); }
            });
            b.addView(filterView);
        }

        /** 147: טעינת רשימה אחרת לטופס — מנקה קודם את מה שהיה, ובונה מחדש את שדות התאריך והגודל. */
        public void reload(Activity a, String cond) {
            kinds.all = true;
            kinds.groups.clear();
            kinds.other = false;
            kinds.manual.clear();
            ds.clear();
            words.setText("");
            src.set(new ArrayList<String>());
            empty.setChecked(false);                                          // מבנה 285: גם "רק תיקיות ריקות" — עד כאן נשאר מסומן מהקודמת
            load(cond);
            show();
            if (filterView != null && filterView.getParent() instanceof android.view.ViewGroup) {
                android.view.ViewGroup parent = (android.view.ViewGroup) filterView.getParent();
                int at = parent.indexOfChild(filterView);
                parent.removeViewAt(at);
                filterView = FilterForm.build(a, ds, new Runnable() {
                    @Override public void run() { if (onChange != null) onChange.run(); }
                });
                parent.addView(filterView, at);
            }
            if (onChange != null) onChange.run();
        }

        public void load(String cond) {
            if (cond == null || cond.isEmpty()) return;
            src.set(Sources.fromCond(cond));
            StringBuilder rest = new StringBuilder();
            boolean kindTaken = false;
            for (String token : Cond.tokens(Sources.rest(cond))) {
                boolean used = false;
                if (!kindTaken && Cond.keyIs(token, Cond.TYPE)) {                // 145: קבוצות, "שאר", ידני — גם תנאי ישן
                    kinds.take(token.substring(token.indexOf(':') + 1), groups);
                    kindTaken = used = true;
                }
                if (!used && ds.take(token)) used = true;                     // 128
                if (!used && Cond.isEmptyDirs(token)) used = true;            // 175
                if (Cond.isEmptyDirs(token)) empty.setChecked(true);
                if (!used) rest.append(rest.length() > 0 ? " " : "").append(Cond.quoteToken(token));
            }
            words.setText(rest.toString());
            show();
        }

        public String build() {
            read();
            StringBuilder sb = new StringBuilder();
            add(sb, Sources.toCond(src.get()));
            add(sb, kinds.token());
            add(sb, ds.dateToken());
            add(sb, ds.sizeToken());
            if (empty.isChecked()) add(sb, Cond.EMPTY);                       // 175
            String w = words.getText().toString().trim();
            if (!w.isEmpty()) add(sb, w);
            return sb.toString();
        }

        /**
         * מבנה 286 (אפיון החיפוש, 8): המסננים שפעילים עכשיו — {מה כתוב על השבב, הרכיבים שלו בתנאי}: מאיפה, סוג, תאריך, גודל,
         * תיקיות ריקות, ושאר הרכיבים (מה שאין לו מקום בחלונית).
         */
        public List<String[]> active() {
            read();
            List<String[]> out = new ArrayList<String[]>();
            String s = Sources.toCond(src.get());
            if (!s.isEmpty()) out.add(new String[]{Sources.describe(s), s});
            String k = kinds.token();
            if (!k.isEmpty() && !empty.isChecked()) {                          // תיקיות ריקות — אין סוג (199)
                String kt = Cond.tokens(k).get(0);
                out.add(new String[]{Words.Find.chipKind(kt.substring(kt.indexOf(':') + 1).replace("|", ", ")), k});
            }
            String d = ds.dateToken();
            if (!d.isEmpty()) out.add(new String[]{Words.SmartF.phrase(DateSize.DATE_MODES[ds.dateMode], ds.dateValue) + (ds.added ? Words.SmartF.BY_ADDED : ""), d});
            String z = ds.sizeToken();
            if (!z.isEmpty()) out.add(new String[]{Words.SmartF.phrase(DateSize.SIZE_MODES[ds.sizeMode], ds.sizeNumber + ds.sizeUnit), z});
            if (empty.isChecked()) out.add(new String[]{Words.Empty.TITLE, Cond.EMPTY});
            String w = words.getText().toString().trim();
            if (!w.isEmpty()) out.add(new String[]{w, w});
            return out;
        }

        /** מבנה 286: בלי המסנן הזה — כל השאר נשאר (כמו טעינה מחדש של התנאי בלי הרכיבים שלו). */
        public void drop(Activity a, String token) {
            reload(a, without(build(), token));
        }

        public void watch(final Runnable update) {
            TextWatcher w = new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int i, int j, int k) { }
                @Override public void onTextChanged(CharSequence s, int i, int j, int k) { }
                @Override public void afterTextChanged(Editable s) { update.run(); }
            };
            words.addTextChangedListener(w);
            onChange = update;
            View.OnClickListener click = new View.OnClickListener() {
                @Override public void onClick(View v) { changed(); }
            };
            empty.setOnClickListener(click);
            allKinds.setOnClickListener(click);
            View.OnClickListener kind = new View.OnClickListener() {            // 198: סוג שנבחר — כבר לא "הכול"
                @Override public void onClick(View v) {
                    if (((CheckBox) v).isChecked()) allKinds.setChecked(false);
                    changed();
                }
            };
            for (CheckBox c : groupBoxes) c.setOnClickListener(kind);
            otherBox.setOnClickListener(kind);
            src.onChange(update);
        }
    }

    private static final String[] LABELS = {Words.SmartF.NAME_L, Words.SmartF.FROM,
            Words.SmartF.SORT_L, Words.SmartF.VIEW_L, Words.SearchTabs.KIND_L,
            Words.Filter.DATE_LABEL_ANY, Words.Filter.SIZE_LABEL};

    /**
     * 197: רוחב עמודת התוויות — נמדד לפי גודל הגופן במכשיר (בגופן גדול "מאיפה" נשבר לשתי שורות ברוחב קבוע).
     * כך השדות מתיישרים זה מתחת לזה.
     */
    static int labelW(Activity a) {
        android.text.TextPaint p = new android.text.TextPaint();
        p.setTextSize(android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP, 13,
                a.getResources().getDisplayMetrics()));
        float w = 0;
        for (String s : LABELS) w = Math.max(w, p.measureText(s));
        return (int) Math.ceil(w) + Ui.dp(a, 10);
    }

    /** 197: התווית שבתחילת שורה — קטנה, אפורה, ברוחב קבוע. */
    static TextView tag(Activity a, String s) {
        TextView t = new TextView(a);
        t.setText(s);
        t.setTextSize(13);
        t.setTextColor(Ui.SUB);
        t.setLayoutParams(new LinearLayout.LayoutParams(labelW(a), -2));
        t.setSingleLine(true);
        return t;
    }

    /** 197: שורה — תווית ולידה הרכיב, עד הקצה. top — רכיב גבוה (כמו המקורות): התווית למעלה. */
    static LinearLayout line(Activity a, String label, View v, boolean top) {
        LinearLayout r = new LinearLayout(a);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(top ? android.view.Gravity.TOP : android.view.Gravity.CENTER_VERTICAL);
        r.setMinimumHeight(Ui.dp(a, 44));
        TextView t = tag(a, label);
        if (top) t.setPadding(0, Ui.dp(a, 12), 0, 0);
        r.addView(t);
        r.addView(v, new LinearLayout.LayoutParams(0, -2, 1f));
        return r;
    }

    private static void add(StringBuilder sb, String part) {
        if (part == null || part.isEmpty()) return;
        if (sb.length() > 0) sb.append(' ');
        sb.append(part);
    }
}
