package com.tikiyot.vfm;

// ממשק הפקודות (1.23.0; האפיון — claude/mimshak-pkudot.md). למעלה — תפריט נגלל: הפקודות שבקובץ, לפי הסדר, ואחריהן
// עוד פקודות (מההגדרה האחת, Commands); שדה כתיבה מסנן אותו ("העתק" או "COPY"); "★ נפוצות" מחליף אותו לפקודות
// הנפוצות (של התוכנה ושלי). בחירה — הפרמטרים נפתחים מתחת: פקודה מהקובץ — עם הערכים, לשינוי; פקודה חדשה — ריקים,
// עם "הכנס לקובץ" אחרי השורה הנבחרת. לכל פרמטר — שם, הסבר, ובוחר לפי סוגו; המתקדמים מקופלים. השורה עצמה —
// לעריכה גם ידנית; "הדמיה" — מה השורה תעשה, מהמנוע עצמו, ברקע. למטה — הקובץ הפתוח, והשורה הנבחרת מודגשת בו.
// הקובץ נשמר רק ב"שמירה" (וביציאה — שאלה).

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.InputType;
import android.text.Spannable;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BatchEditScreen extends Activity {

    public static final String EXTRA_FILE = "file";

    /** שורת הכפתורים בתחתית: משתנים ומתגים נפוצים — נכנסים לשדה שבו הסמן. */
    private static final String[] CHIPS = {Batch.var(Batch.V_INTERNAL), Batch.var(Batch.V_CARD), Batch.var(Batch.V_DOWNLOADS),
            Batch.var(Batch.V_HERE), Batch.var(Batch.V_SHARE), Batch.var(Batch.V_NOW), Batch.var(Batch.V_ONLY_A),
            Batch.var(Batch.V_ONLY_B), "*.pdf", "\"\""};
    private static final String[] MASKS = {"*.*", "*.pdf", "*.epub", "*.mp3"};

    private File file;
    private Editor text;
    private EditText filter;
    private ScrollView menuScroll;
    private LinearLayout menu, panel;
    private Button modeBtn;
    private String saved = "";
    private boolean templates, advOpen, selfEdit, panelStale;
    /** השורה שנבחרה בתפריט (מספר בין השורות שאינן ריקות), או -1. */
    private int sel = -1;
    /** פקודה חדשה שעוד לא בקובץ (קבצים), או ההגדרה שלה (פועל / כותרת). */
    private CmdLine draft;
    private Commands.Def draftDef;
    /** אחרי איזו שורה תיכנס פקודה חדשה (-1 — בסוף). */
    private int anchor = -1;
    private TextView result;
    private EditText lastField;
    private final Handler later = new Handler();
    private Map<String, String> vars;

    /** שורה בקובץ: גבולות בטקסט שבעורך, ומספרה (מ-1, כמו בדוח). */
    private static final class Row {
        int start, end, number;
        String text;
    }

    private List<Row> rows = new ArrayList<Row>();

    private final class Editor extends EditText {
        Editor() {
            super(BatchEditScreen.this);
        }
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        String p = getIntent() != null ? getIntent().getStringExtra(EXTRA_FILE) : null;
        if (p == null) {
            finish();
            return;
        }
        file = new File(p);
        vars = BatchUi.vars(this, file.getParentFile());
        setTitle("📜 " + file.getName());
        LinearLayout root = Ui.screen(this);

        LinearLayout top = new LinearLayout(this);
        top.addView(button(Words.Cmd.SAVE_BTN, new View.OnClickListener() {
            @Override public void onClick(View v) { save(); }
        }));
        top.addView(button(Words.Cmd.PREVIEW_AND_RUN, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (save()) BatchUi.run(BatchEditScreen.this, file);
            }
        }));
        modeBtn = button(Words.Ed.TEMPLATES, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                templates = !templates;
                modeBtn.setText(templates ? Words.Ed.COMMANDS : Words.Ed.TEMPLATES);
                menuScroll.getLayoutParams().height = menuHeight();           // 1.25.1: בנפוצות — התפריט גבוה יותר
                menuScroll.requestLayout();
                menuScroll.scrollTo(0, 0);
                renderMenu();
            }
        });
        top.addView(modeBtn);
        HorizontalScrollView topScroll = new HorizontalScrollView(this);
        topScroll.addView(top);
        root.addView(topScroll);

        filter = Ui.field(this, "", Words.Ed.FILTER_HINT);
        filter.addTextChangedListener(watcher(new Runnable() {
            @Override public void run() { renderMenu(); }
        }));
        root.addView(filter);

        menu = new LinearLayout(this);
        menu.setOrientation(LinearLayout.VERTICAL);
        menuScroll = new ScrollView(this);
        menuScroll.setBackgroundColor(Ui.BG_SOFT);
        menuScroll.setScrollbarFadingEnabled(false);                           // 1.25.1: רואים שיש עוד למטה
        menuScroll.addView(menu);
        root.addView(menuScroll, new LinearLayout.LayoutParams(-1, menuHeight()));

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(Ui.dp(this, 12), Ui.dp(this, 4), Ui.dp(this, 12), Ui.dp(this, 8));
        body.addView(panel);
        TextView fl = Ui.label(this, Words.Ed.THE_FILE);
        fl.setPadding(Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 12), 0);
        body.addView(fl);

        text = new Editor();
        text.setTypeface(Typeface.MONOSPACE);
        text.setTextSize(13);
        text.setGravity(Gravity.TOP | Gravity.START);
        text.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);   // 1.17.6: כל שורה לפי האות הראשונה
        text.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        text.setHorizontallyScrolling(false);
        text.setMinLines(6);
        try {
            saved = Files.readUtf8(file);
        } catch (Throwable t) {
            saved = "";
        }
        Bidi.watch(text);                                                      // 204
        text.setText(Bidi.mark(saved));
        text.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean has) { if (has) lastField = text; }
        });
        text.addTextChangedListener(watcher(new Runnable() {
            @Override
            public void run() {
                if (selfEdit) return;
                later.removeCallbacksAndMessages(null);
                later.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        computeRows();
                        if (sel >= rows.size()) sel = -1;
                        renderMenu();
                        highlight();
                        if (text.hasFocus()) renderPanel();                    // נערך בקובץ עצמו — הפרמטרים מתעדכנים
                    }
                }, 400);
            }
        }));
        body.addView(text);
        ScrollView bodyScroll = new ScrollView(this);
        bodyScroll.addView(body);
        root.addView(bodyScroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        LinearLayout chips = new LinearLayout(this);
        chips.addView(button(Words.Cmd.OPEN_BTN, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TopRow.pick(BatchEditScreen.this, Words.Top.S_BATCH, new TopRow.Chosen() {
                    @Override public void ok(String icon, String name, String line) { insertLines(lines(line)); }
                });
            }
        }));
        chips.addView(button(Words.Cmd.PROGRAM_BTN, new View.OnClickListener() {
            @Override public void onClick(View v) { programHeader(); }
        }));
        for (final String c : CHIPS)
            chips.addView(button(c, new View.OnClickListener() {
                @Override public void onClick(View v) { insertAtCursor(c); }
            }));
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.addView(chips);
        root.addView(hs);
        setContentView(TopRow.wrap(this, root));                   // 1.9.0: שורת הכפתורים העליונה

        computeRows();
        renderMenu();
        renderPanel();
    }

    // ---------- עזר ----------

    private Button button(String label, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setOnClickListener(l);
        return b;
    }

    private static TextWatcher watcher(final Runnable r) {
        return new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable e) { r.run(); }
        };
    }

    private static List<String> lines(String one) {
        List<String> l = new ArrayList<String>();
        l.add(one);
        return l;
    }

    private String plain() {
        return Bidi.strip(text.getText().toString());
    }

    /** השורות שאינן ריקות, עם הגבולות שלהן בעורך. */
    private void computeRows() {
        List<Row> out = new ArrayList<Row>();
        String t = text.getText().toString();
        int st = 0, n = 1;
        while (st <= t.length()) {
            int en = t.indexOf('\n', st);
            if (en < 0) en = t.length();
            String line = Bidi.strip(t.substring(st, en)).trim();
            if (!line.isEmpty()) {
                Row r = new Row();
                r.start = st;
                r.end = en;
                r.number = n;
                r.text = line;
                out.add(r);
            }
            if (en >= t.length()) break;
            st = en + 1;
            n++;
        }
        rows = out;
    }

    private static boolean isHeader(String line) {
        return Program.isHeader(line) || line.equals(Batch.AS_IF) || line.equals(Batch.AS_IF2);
    }

    private static boolean isMeasure(String line) {
        return line.equals(Batch.MEASURE) || line.startsWith(Batch.MEASURE + " ");
    }

    /** ההגדרה של שורה: פקודת קבצים, פועל, או כותרת; null — לא מוכרת. */
    private static Commands.Def defOf(String line) {
        if (Batch.isLoopStart(line)) return Commands.Defs.LOOP;               // 1.25.0
        if (Batch.isLoopEnd(line)) return Commands.Defs.END;
        if (isMeasure(line)) return Commands.Defs.MEASURE;
        if (isHeader(line)) return Commands.get(Batch.AS_IF2);
        if (Verbs.isVerb(line)) return Commands.get(line.split("\\s+")[0]);
        return CmdLine.parse(line).def;
    }

    /** %רק-במקור% בלי COMP לפניו. */
    private boolean needsCompare(int i) {
        if (!CmdLine.parse(rows.get(i).text).usesCompare()) return false;
        for (int k = 0; k < i; k++) {
            Commands.Def d = defOf(rows.get(k).text);
            if (d != null && d.name.equals("COMP")) return false;
        }
        return true;
    }

    /** כמה שורות נפתחות בסוף (START ופעלים) — רק האחרונה נפתחת. */
    private int opens() {
        int n = 0;
        for (Row r : rows) {
            Commands.Def d = defOf(r.text);
            if (d != null && (d.family == Commands.Family.VERB || d.name.equals("START"))) n++;
        }
        return n;
    }

    // ---------- התפריט ----------

    private TextView tag(String s, boolean danger) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(12);
        t.setTextColor(danger ? Ui.RED : Ui.ACCENT_DARK);
        t.setBackgroundColor(danger ? 0xFFFCE8E6 : Ui.BAR);
        t.setPadding(Ui.dp(this, 8), Ui.dp(this, 2), Ui.dp(this, 8), Ui.dp(this, 2));
        t.setSingleLine(true);
        return t;
    }

    private void section(String title) {
        TextView t = Ui.label(this, title);
        t.setPadding(Ui.dp(this, 12), Ui.dp(this, 8), Ui.dp(this, 12), Ui.dp(this, 2));
        menu.addView(t);
    }

    private LinearLayout menuRow(View tagView, String body, boolean mono, boolean selected, View.OnClickListener tap) {
        LinearLayout r = new LinearLayout(this);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(Ui.dp(this, 10), Ui.dp(this, 7), Ui.dp(this, 10), Ui.dp(this, 7));
        r.setBackgroundColor(selected ? Ui.SELECTED : Ui.BG);
        r.addView(tagView);
        TextView b = new TextView(this);
        b.setText(mono ? Bidi.mark(body) : body);
        b.setTextSize(13);
        b.setTextColor(Ui.TEXT);
        if (mono) {
            b.setTypeface(Typeface.MONOSPACE);
            b.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        }
        b.setPadding(Ui.dp(this, 8), 0, 0, 0);
        r.addView(b, new LinearLayout.LayoutParams(0, -2, 1f));
        r.setOnClickListener(tap);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, Ui.dp(this, 1));
        menu.addView(r, lp);
        return r;
    }

    private void renderMenu() {
        menu.removeAllViews();
        String q = filter.getText().toString();
        if (templates) {
            List<CmdTemplates.Tpl> sys = CmdTemplates.system(this), mine = CmdTemplates.mine();
            section(Words.Ed.TPL_SYS + " (" + sys.size() + ")");                 // 1.25.1: כמה יש — גם מה שמתחת לקו
            for (final CmdTemplates.Tpl t : sys) templateRow(t, "★");
            section(Words.Ed.TPL_MINE + " (" + mine.size() + ")");
            for (final CmdTemplates.Tpl t : mine) templateRow(t, "☆");
            return;
        }
        section(Words.Ed.IN_FILE);
        if (rows.isEmpty()) {
            TextView e = Ui.info(this);
            e.setText(Words.Ed.EMPTY_FILE);
            menu.addView(e);
        }
        int depth = 0;
        for (int i = 0; i < rows.size(); i++) {
            final int idx = i;
            String line = rows.get(i).text;
            Commands.Def d = defOf(line);
            boolean start = d == Commands.Defs.LOOP, end = d == Commands.Defs.END;   // 1.25.0: לולאה — מוזחת
            if (end && depth > 0) depth--;
            String indent = "";
            for (int k = 0; k < depth; k++) indent += "│ ";
            if (start) depth++;
            if (!q.trim().isEmpty() && (d == null || !Commands.matches(d, q))) continue;
            String name = d == null ? Words.Ed.TAG_UNKNOWN : d.family == Commands.Family.HEADER && !start && !end && d != Commands.Defs.MEASURE
                    ? Words.Ed.TAG_HEADER : d.he;
            boolean danger = d != null && d.family == Commands.Family.FILE && CmdLine.parse(line).destructive();
            String body = indent + (start ? "↻ " : end ? "└ " : "") + (needsCompare(i) ? "⚠ " : "") + line;
            menuRow(tag(name, danger), body, true, i == sel && draft == null && draftDef == null, new View.OnClickListener() {
                @Override public void onClick(View v) { select(idx); }
            });
        }
        section(Words.Ed.MORE);
        for (final Commands.Def d : Commands.ALL) {
            if (d == Commands.Defs.END || !Commands.matches(d, q)) continue;
            boolean on = (draft != null && draft.def == d) || draftDef == d;
            String head = d.family == Commands.Family.FILE ? d.name + " · " : "";
            menuRow(tag(d.he, false), head + d.what, false, on, new View.OnClickListener() {
                @Override public void onClick(View v) { startNew(d); }
            });
        }
    }

    /**
     * 1.25.1 (באג, המשתמש: "בכוכב אין בדיקת עומס רשימות — רק גיבוי ועוד משהו"): התפריט היה 30% מהמסך, ובנפוצות נראו
     * רק שתיים, בלי סימן שיש עוד. בנפוצות — 60%, ופס הגלילה גלוי תמיד.
     */
    private int menuHeight() {
        int h = getResources().getDisplayMetrics().heightPixels;
        return (int) (h * (templates ? 0.60f : 0.30f));
    }

    private void templateRow(final CmdTemplates.Tpl t, String mark) {
        menuRow(tag(mark, false), t.name + "  ·  " + Words.Ed.lines(t.lines.size()), false, false, new View.OnClickListener() {
            @Override public void onClick(View v) { useTemplate(t); }
        });
    }

    // ---------- בחירה ----------

    private void select(int i) {
        sel = i;
        anchor = i;
        draft = null;
        draftDef = null;
        advOpen = false;
        renderMenu();
        highlight();
        renderPanel();
    }

    private void startNew(Commands.Def d) {
        if (sel >= 0) anchor = sel;
        sel = -1;
        draftDef = d.family == Commands.Family.FILE ? null : d;
        draft = d.family == Commands.Family.FILE ? CmdLine.of(d) : null;
        advOpen = false;
        renderMenu();
        highlight();
        renderPanel();
    }

    private void highlight() {
        Editable e = text.getText();
        for (BackgroundColorSpan s : e.getSpans(0, e.length(), BackgroundColorSpan.class)) e.removeSpan(s);
        if (sel >= 0 && sel < rows.size()) {
            Row r = rows.get(sel);
            e.setSpan(new BackgroundColorSpan(Ui.SELECTED), r.start, r.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    // ---------- הפרמטרים ----------

    private void renderPanel() {
        panelStale = false;
        panel.removeAllViews();
        result = null;
        if (draft == null && draftDef == null && (sel < 0 || sel >= rows.size())) {
            TextView t = Ui.info(this);
            t.setText(rows.isEmpty() ? Words.Ed.EMPTY_FILE : Words.Ed.PICK_FIRST);
            panel.addView(t);
            return;
        }
        boolean isNew = sel < 0;
        String line = isNew ? (draft != null ? draft.text() : draftDef.name) : rows.get(sel).text;
        Commands.Def d = isNew ? (draft != null ? draft.def : draftDef) : defOf(line);
        TextView title = new TextView(this);
        title.setTextSize(15);
        title.setTextColor(Ui.TEXT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setText((d == null ? Words.Ed.TAG_UNKNOWN : d.he + (d.family == Commands.Family.FILE ? " (" + d.name + ")" : ""))
                + "   ·   " + (isNew ? Words.Ed.NEW : Words.Ed.lineN(rows.get(sel).number)));
        panel.addView(title);
        TextView what = Ui.info(this);
        what.setPadding(0, Ui.dp(this, 2), 0, Ui.dp(this, 4));
        what.setText(d == null ? Words.Ed.NOT_UNDERSTOOD : d.what + (d.family == Commands.Family.HEADER && d.name.equals(Batch.AS_IF2)
                ? "\n" + Words.Cmd.HEADER_HELP : ""));
        panel.addView(what);
        if (!isNew && needsCompare(sel)) warn(Words.Ed.NEED_COMPARE);
        if (d != null && (d.family == Commands.Family.VERB || d.name.equals("START")) && opens() > 1) warn(Words.Ed.ONE_OPEN);

        final EditText live = Ui.field(this, line, "");
        if (d != null && d.family == Commands.Family.FILE) fileFields(isNew ? draft : CmdLine.parse(line), live);
        else if (d != null && d.family == Commands.Family.VERB) verbField(d, line, live);

        panel.addView(Ui.label(this, Words.Ed.THE_LINE));
        live.setSingleLine(false);
        live.setTypeface(Typeface.MONOSPACE);
        live.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        Bidi.watch(live);
        live.setText(Bidi.mark(line));
        live.addTextChangedListener(watcher(new Runnable() {
            @Override
            public void run() {
                if (selfEdit) return;
                String now = Bidi.strip(live.getText().toString()).trim();
                if (sel >= 0) {
                    replaceRow(now);
                    panelStale = true;
                } else if (draft != null) {
                    CmdLine c = CmdLine.parse(now);
                    if (c.def != null) draft = c;
                    panelStale = true;
                }
            }
        }));
        live.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean has) {
                if (has) lastField = live;
                else if (panelStale) renderPanel();
            }
        });
        panel.addView(live);

        LinearLayout acts = new LinearLayout(this);
        HorizontalScrollView as = new HorizontalScrollView(this);
        as.addView(acts);
        panel.addView(as);
        if (isNew) {
            final Commands.Def nd = d;
            acts.addView(button(Words.Ed.insertAfter(anchor >= 0 && anchor < rows.size() ? rows.get(anchor).number : 0),
                    new View.OnClickListener() {
                        @Override public void onClick(View v) { insertNew(nd, live); }
                    }));
            return;
        }
        acts.addView(button(Words.Ed.SIMULATE, new View.OnClickListener() {
            @Override public void onClick(View v) { simulate(); }
        }));
        acts.addView(button(Words.Ed.UP, new View.OnClickListener() {
            @Override public void onClick(View v) { move(-1); }
        }));
        acts.addView(button(Words.Ed.DOWN, new View.OnClickListener() {
            @Override public void onClick(View v) { move(1); }
        }));
        acts.addView(button(Words.Ed.REMOVE, new View.OnClickListener() {
            @Override public void onClick(View v) { removeRow(); }
        }));
        acts.addView(button(Words.Ed.SAVE_TPL, new View.OnClickListener() {
            @Override public void onClick(View v) { saveTemplate(); }
        }));
        result = Ui.info(this);
        result.setPadding(0, Ui.dp(this, 4), 0, 0);
        panel.addView(result);
    }

    private void warn(String s) {
        TextView w = Ui.info(this);
        w.setTextColor(Ui.RED);
        w.setPadding(0, 0, 0, Ui.dp(this, 4));
        w.setText(s);
        panel.addView(w);
    }

    /** שינוי בשדה: השורה נבנית מחדש, ונכנסת לקובץ (או לטיוטה). */
    private void changed(CmdLine c, EditText live) {
        String now = c.text();
        selfEdit = true;
        live.setText(Bidi.mark(now));
        selfEdit = false;
        if (sel >= 0) replaceRow(now);
        else draft = c;
    }

    private void fileFields(final CmdLine c, final EditText live) {
        List<Commands.Param> basic = new ArrayList<Commands.Param>(), adv = new ArrayList<Commands.Param>();
        for (Commands.Param p : c.def.params) {
            if (p.compat || (!p.isSwitch() && c.slot(p) < 0)) continue;
            if (p.type == Commands.Type.SIZE) continue;                       // יחד עם התאריך (FilterForm)
            (p.advanced ? adv : basic).add(p);
        }
        for (Commands.Param p : basic) field(c, p, live);
        if (adv.isEmpty()) return;
        int set = 0;
        for (Commands.Param p : adv) if (!c.value(p).isEmpty()) set++;
        final boolean open = advOpen || set > 0;
        Button t = button(Words.Ed.advanced(adv.size()), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                advOpen = !open;
                renderPanel();
            }
        });
        panel.addView(t);
        if (open) for (Commands.Param p : adv) field(c, p, live);
    }

    private void field(final CmdLine c, final Commands.Param p, final EditText live) {
        field(c, p, live, false);
    }

    /** asText — תאריך או גודל שהרכיב לא יודע להציג: שדה טקסט, כדי לא לאבד אותו (בדיקת באגים 5). */
    private void field(final CmdLine c, final Commands.Param p, final EditText live, boolean asText) {
        if (p.type == Commands.Type.DATE && !asText) {
            if (dateSize(c, live)) return;
            field(c, p, live, true);
            field(c, c.def.switchFor("/" + Batch.SW_SIZE), live, true);
            return;
        }
        panel.addView(Ui.label(this, p.label + (p.isSwitch() ? "   " + p.key : "")));
        TextView h = Ui.info(this);
        h.setPadding(0, 0, 0, 0);
        h.setText(p.help);
        panel.addView(h);
        String v = c.value(p);
        if (p.type == Commands.Type.FLAG) {
            CheckBox cb = Ui.check(this, Words.Ed.ON, !v.isEmpty());
            cb.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(android.widget.CompoundButton b, boolean on) {
                    c.set(p, on ? "1" : "");
                    changed(c, live);
                }
            });
            panel.addView(cb);
            return;
        }
        if (p.type == Commands.Type.CHOICE) {
            final String[] items = new String[p.options.size() + 1];
            items[0] = Words.Ed.NONE;
            for (int i = 0; i < p.options.size(); i++) items[i + 1] = p.options.get(i);
            Spinner sp = Ui.choice(this, items, Math.max(0, p.options.indexOf(v) + 1));
            final boolean[] ready = {false};
            sp.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> a, View vw, int pos, long id) {
                    if (!ready[0]) {
                        ready[0] = true;
                        return;
                    }
                    c.set(p, pos == 0 ? "" : items[pos]);
                    changed(c, live);
                }
                @Override public void onNothingSelected(android.widget.AdapterView<?> a) { }
            });
            panel.addView(sp);
            return;
        }
        final EditText e = Ui.field(this, "", p.type == Commands.Type.PERCENT ? "20" : "");
        boolean path = p.type == Commands.Type.PATH || p.type == Commands.Type.FILES;
        e.setTextDirection(path ? View.TEXT_DIRECTION_LTR : View.TEXT_DIRECTION_FIRST_STRONG);
        if (p.type == Commands.Type.PERCENT) e.setInputType(InputType.TYPE_CLASS_NUMBER);
        Bidi.watch(e);
        e.setText(v);
        e.addTextChangedListener(watcher(new Runnable() {
            @Override
            public void run() {
                if (selfEdit) return;
                c.set(p, Bidi.strip(e.getText().toString()));
                changed(c, live);
            }
        }));
        e.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View x, boolean has) { if (has) lastField = e; }
        });
        panel.addView(e);
        if (!path) return;
        LinearLayout row = new LinearLayout(this);
        final boolean dest = p.label.equals(Words.Def.P_DEST) || c.def.name.equals("MD") || c.def.name.equals("START");
        final boolean share = c.def.name.equals("COPY") && p.label.equals(Words.Def.P_DEST);
        row.addView(button(Words.Cmd.PICK, new View.OnClickListener() {
            @Override
            public void onClick(View x) {
                FolderPicker.choose(BatchEditScreen.this, p.label, dest, new FolderPicker.Picked() {
                    @Override
                    public void ok(File folder) {
                        String old = Bidi.strip(e.getText().toString()), mask = maskOf(old);
                        e.setText(BatchLine.toVar(folder.getAbsolutePath(), vars) + (mask.isEmpty() ? "" : "\\" + mask));
                    }
                }, share ? Words.Cmd.SHARE_BTN : null, share ? new Runnable() {
                    @Override public void run() { e.setText(Batch.var(Batch.V_SHARE)); }
                } : null);
            }
        }));
        if (p.type == Commands.Type.FILES)
            for (final String m : MASKS)
                row.addView(button(m, new View.OnClickListener() {
                    @Override
                    public void onClick(View x) {
                        String old = Bidi.strip(e.getText().toString()), base = old;
                        if (!maskOf(old).isEmpty()) base = old.substring(0, Math.max(0, old.length() - maskOf(old).length() - 1));
                        e.setText(base.isEmpty() ? m : base + "\\" + m);
                    }
                }));
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.addView(row);
        panel.addView(hs);
    }

    /** "*.pdf" שבסוף המסלול, או "". */
    private static String maskOf(String s) {
        String n = s.replace('/', '\\');
        int cut = n.lastIndexOf('\\');
        String last = cut >= 0 ? n.substring(cut + 1) : n;
        return last.contains("*") || last.contains("?") ? last : "";
    }

    /** /תאריך: ו/גודל: — הרכיב המשותף (FilterForm), כמו בטופס הקודם. */
    private boolean dateSize(final CmdLine c, final EditText live) {
        final Commands.Param date = c.def.switchFor("/" + Batch.SW_DATE), size = c.def.switchFor("/" + Batch.SW_SIZE);
        final DateSize ds = new DateSize();
        if (!c.value(date).isEmpty() && !ds.take(Batch.SW_DATE + c.value(date))) return false;
        if (!c.value(size).isEmpty() && !ds.take(Batch.SW_SIZE + c.value(size))) return false;
        panel.addView(Ui.label(this, date.label + " · " + size.label));
        TextView h = Ui.info(this);
        h.setPadding(0, 0, 0, 0);
        h.setText(date.help + " · " + size.help);
        panel.addView(h);
        panel.addView(FilterForm.build(this, ds, new Runnable() {
            @Override
            public void run() {
                String d = ds.dateToken(), s = ds.sizeToken();
                String dv = d.startsWith(Batch.SW_DATE) ? d.substring(Batch.SW_DATE.length()) : "",
                        sv = s.startsWith(Batch.SW_SIZE) ? s.substring(Batch.SW_SIZE.length()) : "";
                if (dv.equals(c.value(date)) && sv.equals(c.value(size))) return;   // הרכיב קורא גם בבנייה — שורה שלא שונתה נשארת אות באות
                c.set(date, dv);
                c.set(size, sv);
                changed(c, live);
            }
        }));
        return true;
    }

    /** פועל בעברית: מה שאחריו; "פתח" — גם בחירה (מסך, רשימה, תיקייה, תפריט). */
    private void verbField(final Commands.Def d, String line, final EditText live) {
        panel.addView(Ui.label(this, Words.Def.P_ARG));
        TextView h = Ui.info(this);
        h.setPadding(0, 0, 0, 0);
        h.setText(Words.Verb.usage(d.name));
        panel.addView(h);
        String rest = line.length() > d.name.length() ? line.substring(d.name.length()).trim() : "";
        final EditText e = Ui.field(this, rest, "");
        e.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        e.addTextChangedListener(watcher(new Runnable() {
            @Override
            public void run() {
                if (selfEdit) return;
                String now = (d.name + " " + e.getText().toString().trim()).trim();
                selfEdit = true;
                live.setText(Bidi.mark(now));
                selfEdit = false;
                if (sel >= 0) replaceRow(now);
            }
        }));
        panel.addView(e);
        if (!d.name.equals(Verbs.OPEN)) return;
        panel.addView(button(Words.Ed.PICK, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TopRow.pick(BatchEditScreen.this, Words.Top.S_BATCH, new TopRow.Chosen() {
                    @Override
                    public void ok(String icon, String name, String act) {
                        if (act.startsWith(d.name + " ")) e.setText(act.substring(d.name.length() + 1));
                        else live.setText(Bidi.mark(act));
                    }
                });
            }
        }));
    }

    // ---------- שינויים בקובץ ----------

    /** מחליף את השורה הנבחרת בעורך. */
    private void replaceRow(String now) {
        if (sel < 0 || sel >= rows.size()) return;
        Row r = rows.get(sel);
        selfEdit = true;
        text.getText().replace(r.start, r.end, Bidi.mark(now));
        selfEdit = false;
        computeRows();
        renderMenu();
        highlight();
    }

    private void insertNew(Commands.Def d, EditText live) {
        String line = Bidi.strip(live.getText().toString()).trim();
        if (d == Commands.Defs.LOOP) {                                         // 1.25.0: הלולאה נכנסת עם ה"סוף" שלה
            insertLines(java.util.Arrays.asList(line.isEmpty() ? d.name : line, Batch.LOOP_END));
            return;
        }
        if (d == Commands.Defs.MEASURE) {
            for (Row r : rows)
                if (isMeasure(r.text)) {
                    Ui.toast(this, Words.Ed.ALREADY_MEASURE);
                    return;
                }
            selfEdit = true;
            text.getText().insert(0, Batch.MEASURE + "\n");
            selfEdit = false;
            computeRows();
            select(0);
            return;
        }
        if (d.family == Commands.Family.HEADER) {
            if (plain().contains(Batch.AS_IF2) || plain().contains(Batch.AS_IF)) {
                Ui.toast(this, Words.Ed.ALREADY_AS_IF);
                return;
            }
            selfEdit = true;
            text.getText().insert(0, Batch.AS_IF2 + "\n");
            selfEdit = false;
            computeRows();
            select(0);
            return;
        }
        if (draft != null) {
            List<Commands.Param> miss = CmdLine.parse(line).missing();
            if (!miss.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (Commands.Param p : miss) sb.append(sb.length() > 0 ? ", " : "").append(p.label);
                Ui.toast(this, Words.Ed.fillFirst(sb.toString()));
                return;
            }
        }
        insertLines(lines(line));
    }

    /** שורות אחרי השורה של העוגן (או בסוף), והראשונה נבחרת. */
    private void insertLines(List<String> ls) {
        Editable e = text.getText();
        int at = anchor >= 0 && anchor < rows.size() ? rows.get(anchor).end : e.length();
        StringBuilder sb = new StringBuilder();
        boolean empty = Bidi.strip(e.toString()).trim().isEmpty();
        for (String l : ls) sb.append(sb.length() > 0 || !empty ? "\n" : "").append(Bidi.mark(l));
        if (empty) {
            selfEdit = true;
            e.replace(0, e.length(), sb.toString());
            selfEdit = false;
        } else {
            selfEdit = true;
            e.insert(at, sb.toString());
            selfEdit = false;
        }
        int first = empty ? 0 : at + 1;
        computeRows();
        int idx = 0;
        for (int i = 0; i < rows.size(); i++) if (rows.get(i).start <= first) idx = i;
        draft = null;
        draftDef = null;
        select(idx);
    }

    private void insertAtCursor(String s) {
        EditText e = lastField != null && lastField.isShown() ? lastField : text;
        int st = Math.max(e.getSelectionStart(), 0), en = Math.max(e.getSelectionEnd(), 0);
        e.getText().replace(Math.min(st, en), Math.max(st, en), s);
        if (s.equals("\"\"")) e.setSelection(Math.min(st, en) + 1);
    }

    private void move(int dir) {
        int other = sel + dir;
        if (sel < 0 || other < 0 || other >= rows.size()) return;
        Row a = rows.get(Math.min(sel, other)), b = rows.get(Math.max(sel, other));
        Editable e = text.getText();
        String ta = e.subSequence(a.start, a.end).toString(), tb = e.subSequence(b.start, b.end).toString();
        selfEdit = true;
        e.replace(b.start, b.end, ta);                                         // קודם המאוחרת — הגבולות של הראשונה לא זזים
        e.replace(a.start, a.end, tb);
        selfEdit = false;
        computeRows();
        select(other);
    }

    private void removeRow() {
        if (sel < 0 || sel >= rows.size()) return;
        Row r = rows.get(sel);
        Editable e = text.getText();
        int st = r.start, en = r.end < e.length() ? r.end + 1 : r.end;
        if (en == r.end && st > 0) st--;                                       // השורה האחרונה — גם המעבר שלפניה
        selfEdit = true;
        e.delete(st, en);
        selfEdit = false;
        computeRows();
        sel = -1;
        anchor = Math.min(anchor, rows.size() - 1);
        renderMenu();
        highlight();
        renderPanel();
    }

    private void programHeader() {
        String t = text.getText().toString();
        if (Program.parse(t).isProgram()) {
            Ui.toast(this, Words.Cmd.ALREADY_PROGRAM);
            return;
        }
        selfEdit = true;
        text.getText().insert(0, Program.ICON + " ▶\n" + Program.NAME + " " + Text.stem(file.getName()) + "\n" + Program.WHY + " \n");
        selfEdit = false;
        computeRows();
        renderMenu();
        highlight();
    }

    // ---------- הדמיה ----------

    /** מה השורה תעשה — מהמנוע עצמו (Batch.plan על כל הקובץ), ברקע (בדיקת באגים 9–10). */
    private void simulate() {
        if (sel < 0 || result == null) return;
        final int line = rows.get(sel).number;
        final String all = plain();
        final TextView out = result;
        out.setTextColor(Ui.SUB);
        out.setText(Words.Ed.SIMULATING);
        final Library lib = Library.get(this);
        new Thread(new Runnable() {
            @Override
            public void run() {
                String msg;
                boolean bad = false;
                try {
                    Batch.Plan plan = Batch.plan(all, file.getParentFile(), vars, lib);
                    Batch.Step step = null;
                    for (Batch.Step s : plan.steps) if (s.line == line) step = s;
                    if (step == null) msg = Words.Ed.NO_STEP;
                    else if (step.error != null) {
                        msg = "⚠ " + step.error;
                        bad = true;
                    } else {
                        StringBuilder sb = new StringBuilder(step.describe == null ? "" : step.describe);
                        int shown = Math.min(3, step.files.size());
                        for (int i = 0; i < shown; i++) sb.append("\n · ").append(new File(step.files.get(i)).getName());
                        if (step.files.size() > shown) sb.append("\n · ").append(Words.andMore(step.files.size() - shown));
                        msg = sb.toString();
                    }
                } catch (Throwable t) {
                    Log.e("הדמיה של שורה", t);
                    msg = "⚠ " + t;
                    bad = true;
                }
                final String m = msg;
                final boolean b = bad;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (out != result) return;                            // בינתיים נבחרה שורה אחרת
                        out.setTextColor(b ? Ui.RED : Ui.TEXT);
                        out.setText(m);
                    }
                });
            }
        }).start();
    }

    // ---------- נפוצות ----------

    private void saveTemplate() {
        if (sel < 0) return;
        final String line = rows.get(sel).text;
        Commands.Def d = defOf(line);
        Ui.askText(this, Words.Ed.TPL_NAME, d == null ? "" : d.he, "", new Ui.Text1() {
            @Override
            public void ok(String name) {
                if (CmdTemplates.save(name, lines(line)) != null) Ui.toast(BatchEditScreen.this, Words.Ed.TPL_SAVED);
                else Ui.toast(BatchEditScreen.this, Words.SAVE_FAILED);
            }
        });
    }

    /** תבנית: המקומות הפתוחים נשאלים כולם לפני ההכנסה; ביטול — לא נכנס כלום (בדיקת באגים 16). */
    private void useTemplate(final CmdTemplates.Tpl t) {
        if (sel >= 0) anchor = sel;
        final List<String> holes = CmdTemplates.holes(t.lines);
        if (holes.isEmpty()) {
            templatesOff();
            insertLines(t.lines);
            return;
        }
        LinearLayout box = Ui.form(this);
        final List<EditText> fields = new ArrayList<EditText>();
        for (final String h : holes) {
            box.addView(Ui.label(this, h));
            final EditText e = Ui.field(this, "", "");
            e.setTextDirection(View.TEXT_DIRECTION_LTR);
            Bidi.watch(e);
            box.addView(e);
            fields.add(e);
            box.addView(button(Words.Cmd.PICK, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    FolderPicker.choose(BatchEditScreen.this, h, true, new FolderPicker.Picked() {
                        @Override public void ok(File folder) { e.setText(BatchLine.toVar(folder.getAbsolutePath(), vars)); }
                    });
                }
            }));
        }
        Ui.askForm(this, Words.Ed.FILL + ": " + t.name, box, new Runnable() {
            @Override
            public void run() {
                Map<String, String> v = CmdTemplates.empty(holes);
                for (int i = 0; i < holes.size(); i++) v.put(holes.get(i), Bidi.strip(fields.get(i).getText().toString()));
                templatesOff();
                insertLines(CmdTemplates.fill(t.lines, v));
            }
        });
    }

    private void templatesOff() {
        templates = false;
        modeBtn.setText(Words.Ed.TEMPLATES);
    }

    // ---------- שמירה ----------

    /** שמירה של הקובץ שהמשתמש עורך (כפתור מפורש — הוא השינוי שביקש). */
    private boolean save() {
        String now = plain();                                                  // 204: נשמר בלי סימני הכיוון
        if (now.equals(saved)) return true;
        try {
            Files.writeAtomic(file, now);
            saved = now;
            Library.get(this).fileAdded(file);
            Ui.toast(this, Words.SAVED);
            return true;
        } catch (Throwable t) {
            Log.e("שמירת קובץ פקודות", t);
            Ui.toast(this, Words.SAVE_FAILED);
            return false;
        }
    }

    @Override
    public void onBackPressed() {
        if (!plain().equals(saved)) {
            Ui.ask(this, Words.Cmd.ASK_SAVE, Words.Cmd.fileNamed(file.getName()), null, null, Words.SAVE, new Runnable() {
                @Override
                public void run() {
                    if (save()) finish();
                }
            }, Words.Cmd.DONT_SAVE, new Runnable() {
                @Override public void run() { finish(); }
            });
            return;
        }
        super.onBackPressed();
    }
}
