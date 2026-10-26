package com.tikiyot.vfm;

// עורך קובץ פקודות (117א, שלב 1): טקסט חופשי, ושורת פקודות שמכניסה פקודה או משתנה במקום הסמן.
// בשלב 2 — טופס לכל פקודה (תוספות, "קיצורי דרך חכמים").

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;

public class BatchEditScreen extends Activity {

    public static final String EXTRA_FILE = "file";

    /** שורת הכפתורים בתחתית: הפקודות, המשתנים ומתגים נפוצים — מהתחביר שב-Batch. */
    private static final String[] CMDS = {"COPY ", "MOVE ", "DEL ", "REN ", "MD ", "RD ", "START ", "ECHO ", "REM ",
            Batch.var(Batch.V_INTERNAL), Batch.var(Batch.V_CARD), Batch.var(Batch.V_DOWNLOADS), Batch.var(Batch.V_HERE),
            Batch.var(Batch.V_SHARE), "*.pdf", "\"\"", "/Y", "/" + Batch.SW_DATE + "יום", "/" + Batch.SW_DATE + "<שנה", "/" + Batch.SW_SIZE + ">10MB"};   // תחביר

    private File file;
    private Editor text;
    private TextView help;
    private String saved = "";

    /** עורך שמודיע כשהסמן זז — כדי להסביר את הפקודה שבשורה. */
    private final class Editor extends EditText {
        Editor() {
            super(BatchEditScreen.this);
        }

        @Override
        protected void onSelectionChanged(int st, int en) {
            super.onSelectionChanged(st, en);
            if (help != null) showHelp();
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
        setTitle("📜 " + file.getName());
        LinearLayout root = Ui.screen(this);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.addView(button(Words.Cmd.SAVE_BTN, new View.OnClickListener() {
            @Override public void onClick(View v) { save(); }
        }));
        top.addView(button(Words.Cmd.LINE_IN_FORM, new View.OnClickListener() {
            @Override public void onClick(View v) { formForLine(); }
        }));
        top.addView(button(Words.Cmd.PREVIEW_AND_RUN, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (save()) BatchUi.run(BatchEditScreen.this, file);
            }
        }));
        root.addView(top);

        text = new Editor();
        text.setTypeface(Typeface.MONOSPACE);
        text.setTextSize(14);
        text.setGravity(Gravity.TOP | Gravity.START);
        text.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);   // 1.17.6: כל שורה לפי האות הראשונה — עברית מימין, אנגלית משמאל
        text.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        text.setHorizontallyScrolling(false);
        try {
            saved = Files.readUtf8(file);
        } catch (Throwable t) {
            saved = "";
        }
        Bidi.watch(text);                                                      // 204: מסלולים בעברית — בלי היפוך
        text.setText(Bidi.mark(saved));
        root.addView(text, new LinearLayout.LayoutParams(-1, 0, 1f));

        help = Ui.info(this);
        root.addView(help);
        showHelp();
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        // 1.13.0: הטופס כותב גם את הפקודות בעברית — "פתח…" (רשימה, רשימה חכמה, תיקייה, מסך, קובץ פקודות),
        // כותרת התוכנה (צלמית / שם / למה), ו"הדמיה"
        row.addView(button(Words.Cmd.OPEN_BTN, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TopRow.pick(BatchEditScreen.this, Words.Top.S_BATCH, new TopRow.Chosen() {
                    @Override public void ok(String icon, String name, String line) { insertLine(line); }
                });
            }
        }));
        row.addView(button(Words.Cmd.PROGRAM_BTN, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String t = text.getText().toString();
                if (Program.parse(t).isProgram()) {
                    Ui.toast(BatchEditScreen.this, Words.Cmd.ALREADY_PROGRAM);
                    return;
                }
                text.getText().insert(0, Program.ICON + " ▶\n" + Program.NAME + " " + Text.stem(file.getName()) + "\n"
                        + Program.WHY + " \n");
                text.setSelection(Program.ICON.length() + 3 + Program.NAME.length() + 1 + Text.stem(file.getName()).length() + 1
                        + Program.WHY.length() + 1);
            }
        }));
        row.addView(button(Batch.AS_IF2, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!text.getText().toString().contains(Batch.AS_IF2)) text.getText().insert(0, Batch.AS_IF2 + "\n");
            }
        }));
        for (final String c : CMDS)
            row.addView(button(c.trim(), new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String cmd = c.trim();
                    if (java.util.Arrays.asList(BatchLine.COMMANDS).contains(cmd)) {   // פקודה — טופס לשורה חדשה
                        BatchLine b = new BatchLine();
                        b.cmd = cmd;
                        form(b, -1, -1);
                    } else {
                        insert(c);                                                   // משתנה או מתג — במקום הסמן
                    }
                }
            }));
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.addView(row);
        root.addView(hs);
        setContentView(TopRow.wrap(this, root));                   // 1.9.0: שורת הכפתורים העליונה
    }

    private Button button(String label, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setOnClickListener(l);
        return b;
    }

    /** 1.13.0: שורה שלמה — אחרי השורה שהסמן בה. */
    private void insertLine(String line) {
        String t = text.getText().toString();
        int at = Math.max(text.getSelectionEnd(), 0);
        int end = t.indexOf('\n', at);
        if (end < 0) end = t.length();
        String ins = (t.isEmpty() || end == 0 && t.isEmpty() ? "" : "\n") + line;
        text.getText().insert(end, ins);
        text.setSelection(end + ins.length());
    }

    private void insert(String s) {
        int st = Math.max(text.getSelectionStart(), 0), en = Math.max(text.getSelectionEnd(), 0);
        text.getText().replace(Math.min(st, en), Math.max(st, en), s);
        if (s.equals("\"\"")) text.setSelection(Math.min(st, en) + 1);   // הסמן בין המרכאות
    }

    // ---------- הטופס (שלב 2) ----------

    /** גבולות השורה שהסמן עומד בה: {התחלה, סוף}. */
    private int[] lineAt() {
        String t = text.getText().toString();
        int pos = Math.max(0, Math.min(text.getSelectionStart(), t.length()));
        int st = t.lastIndexOf('\n', pos - 1) + 1;
        int en = t.indexOf('\n', pos);
        return new int[]{st, en < 0 ? t.length() : en};
    }

    private void showHelp() {
        int[] r = lineAt();
        String line = text.getText().toString().substring(r[0], r[1]).trim();
        if (Verbs.isVerb(line)) {                                             // 1.13.0: פקודה בעברית — מה תפתח
            Verbs.Cmd vc = Verbs.parse(line);
            help.setText(vc.error != null ? "⚠ " + vc.error : Verbs.describe(vc));
            return;
        }
        if (Program.isHeader(line) || line.equals(Batch.AS_IF) || line.equals(Batch.AS_IF2)) {   // כותרת / הדמיה
            help.setText(Words.Cmd.HEADER_HELP);
            return;
        }
        String cmd = line.isEmpty() ? "" : BatchLine.parse(line).cmd;
        String h = BatchLine.help(cmd);
        if (!line.isEmpty() && h.isEmpty() && !line.startsWith("::") && !line.equalsIgnoreCase("@ECHO OFF"))
            h = "⚠ " + Words.Cmd.unknownCommand(Batch.tokens(line).get(0));
        help.setText(h.isEmpty() ? Words.Cmd.EDITOR_HINT : h + Words.Cmd.TO_FORM);
    }

    private void formForLine() {
        int[] r = lineAt();
        String line = text.getText().toString().substring(r[0], r[1]).trim();
        if (line.isEmpty()) {
            Ui.toast(this, Words.Cmd.EMPTY_LINE);
            return;
        }
        BatchLine b = BatchLine.parse(line);
        if (!java.util.Arrays.asList(BatchLine.COMMANDS).contains(b.cmd)) {
            Ui.toast(this, Words.Cmd.NO_FORM);
            return;
        }
        form(b, r[0], r[1]);
    }

    private EditText field(LinearLayout box, String label, String value, String hint) {
        box.addView(Ui.label(this, label));
        EditText e = Ui.field(this, "", hint);
        e.setTextDirection(View.TEXT_DIRECTION_LTR);
        Bidi.watch(e);                                                        // 204
        e.setText(value == null ? "" : value);
        box.addView(e);
        return e;
    }

    /** שדה מסלול, עם כפתור בורר שמכניס מסלול בסגנון bat (%פנימי%\…). */
    private EditText pathField(LinearLayout box, String label, String value, final boolean canCreate, final Runnable changed) {
        return pathField(box, label, value, canCreate, changed, false);
    }

    /** 203/204: share — בבורר, ליד הכוננים, גם "📤 שיתוף": היעד הוא שליחה למכשיר אחר (רק ב־COPY). */
    private EditText pathField(LinearLayout box, String label, String value, final boolean canCreate, final Runnable changed,
                               final boolean share) {
        final EditText e = field(box, label, value, Batch.var(Batch.V_INTERNAL) + "\\" + Words.Cmd.FOLDER);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        box.addView(row);
        row.addView(button(Words.Cmd.PICK, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FolderPicker.choose(BatchEditScreen.this, label, canCreate, new FolderPicker.Picked() {
                    @Override
                    public void ok(File folder) {
                        e.setText(BatchLine.toVar(folder.getAbsolutePath(), BatchUi.vars(BatchEditScreen.this, file.getParentFile())));
                        changed.run();
                    }
                }, share ? Words.Cmd.SHARE_BTN : null, share ? new Runnable() {     // 204: "📤 שיתוף" — ליד הכוננים
                    @Override
                    public void run() {
                        e.setText(Batch.var(Batch.V_SHARE));
                        changed.run();
                    }
                } : null);
            }
        }));
        return e;
    }

    private android.widget.Spinner spinner(LinearLayout box, String label, String[] items, int sel, final Runnable changed) {
        if (label != null) box.addView(Ui.label(this, label));
        android.widget.Spinner sp = Ui.choice(this, items, sel);
        sp.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) { changed.run(); }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) { }
        });
        box.addView(sp);
        return sp;
    }

    /** טופס לפקודה: כל האפשרויות שלה, והשורה מתעדכנת מתחת. start < 0 — שורה חדשה (אחרי השורה של הסמן). */
    private void form(final BatchLine b, final int start, final int end) {
        LinearLayout box = Ui.form(this);
        TextView title = Ui.label(this, BatchLine.help(b.cmd));
        box.addView(title);
        final TextView preview = new TextView(this);
        preview.setTypeface(Typeface.MONOSPACE);
        preview.setTextDirection(View.TEXT_DIRECTION_LTR);
        preview.setTextColor(Ui.TEXT);
        preview.setBackgroundColor(Ui.HEADER);
        preview.setPadding(Ui.dp(this, 8), Ui.dp(this, 8), Ui.dp(this, 8), Ui.dp(this, 8));

        final EditText[] src = new EditText[1], mask = new EditText[1], dest = new EditText[1], name = new EditText[1],
                txt = new EditText[1], dval = new EditText[1], snum = new EditText[1], other = new EditText[1];
        final android.widget.Spinner[] dmode = new android.widget.Spinner[1], smode = new android.widget.Spinner[1],
                sunit = new android.widget.Spinner[1], taken = new android.widget.Spinner[1];
        final android.widget.CheckBox[] rec = new android.widget.CheckBox[1];
        final boolean[] ready = {false};
        final DateSize[] dsh = {null};
        final Runnable update = new Runnable() {
            @Override
            public void run() {
                if (!ready[0]) return;
                if (src[0] != null) b.srcDir = Bidi.strip(src[0].getText().toString()).trim();      // 204
                if (mask[0] != null) b.mask = Bidi.strip(mask[0].getText().toString()).trim();
                if (dest[0] != null) b.dest = Bidi.strip(dest[0].getText().toString()).trim();
                if (name[0] != null) b.newName = Bidi.strip(name[0].getText().toString()).trim();
                if (txt[0] != null) b.text = txt[0].getText().toString().trim();
                if (dsh[0] != null) b.filter(dsh[0]);                            // 128
                if (taken[0] != null) b.taken = taken[0].getSelectedItemPosition();
                if (rec[0] != null) b.recursive = rec[0].isChecked();
                if (other[0] != null) {
                    b.other.clear();
                    for (String o : other[0].getText().toString().trim().split("\\s+")) if (!o.isEmpty()) b.other.add(o);
                }
                preview.setText(Bidi.mark(b.toLine()));
            }
        };
        android.text.TextWatcher w = new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence x, int a, int c, int d) { }
            @Override public void onTextChanged(CharSequence x, int a, int c, int d) { }
            @Override public void afterTextChanged(android.text.Editable e) { update.run(); }
        };
        String c = b.cmd;
        boolean files = c.equals("COPY") || c.equals("MOVE") || c.equals("DEL");
        if (files) {
            src[0] = pathField(box, Words.Cmd.SRC_FOLDER, b.srcDir, false, update);
            mask[0] = field(box, Words.Cmd.MASK, b.mask, "*.pdf");
            LinearLayout chips = new LinearLayout(this);
            for (final String m : new String[]{"*.*", "*.pdf", "*.epub", "*.mp3"})
                chips.addView(button(m, new View.OnClickListener() {
                    @Override public void onClick(View v) { mask[0].setText(m); }
                }));
            box.addView(chips);
            dsh[0] = b.filter();                                             // 128: הרכיב המשותף
            box.addView(FilterForm.build(this, dsh[0], update));
        }
        if (c.equals("COPY") || c.equals("MOVE")) {
            dest[0] = pathField(box, c.equals("COPY") ? Words.Cmd.DEST_OR_SHARE : Words.Cmd.DEST_FOLDER, b.dest, true, update,
                    c.equals("COPY"));                                        // 203: COPY — גם לשיתוף
            taken[0] = spinner(box, Words.NAME_TAKEN_AT_DEST, BatchLine.TAKEN, b.taken, update);
        }
        if (c.equals("REN")) {
            src[0] = field(box, Words.Cmd.THE_FILE, b.srcDir, Batch.var(Batch.V_DOWNLOADS) + "\\ספר.pdf");   // תחביר
            name[0] = field(box, Words.Cmd.NEW_NAME, b.newName, "שם.pdf");   // תחביר
        }
        if (c.equals("MD") || c.equals("RD") || c.equals("START"))
            dest[0] = pathField(box, c.equals("START") ? Words.Cmd.START_TARGET : Words.Cmd.FOLDER, b.dest,
                    c.equals("MD") || c.equals("START"), update);
        if (c.equals("RD")) {
            rec[0] = new android.widget.CheckBox(this);
            rec[0].setText(Words.Cmd.RD_ALL);
            rec[0].setChecked(b.recursive);
            rec[0].setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
                @Override public void onCheckedChanged(android.widget.CompoundButton x, boolean y) { update.run(); }
            });
            box.addView(rec[0]);
        }
        if (c.equals("ECHO") || c.equals("REM")) txt[0] = field(box, c.equals("ECHO") ? Words.Cmd.THE_MESSAGE : Words.Cmd.THE_REMARK, b.text, "");
        if (!c.equals("ECHO") && !c.equals("REM")) {
            StringBuilder o = new StringBuilder();
            for (String x : b.other) o.append(o.length() > 0 ? " " : "").append(x);
            other[0] = field(box, Words.Cmd.OTHER_SWITCHES, o.toString(), "/MAXAGE:7");
        }
        for (EditText e : new EditText[]{src[0], mask[0], dest[0], name[0], txt[0], other[0]})
            if (e != null) e.addTextChangedListener(w);
        box.addView(Ui.label(this, Words.Cmd.THE_LINE));
        box.addView(preview);
        ready[0] = true;
        update.run();
        Ui.askForm(this, Words.Cmd.formTitle(start < 0, c), box, new Runnable() {
            @Override
            public void run() {
                update.run();
                String line = b.toLine();
                android.text.Editable ed = text.getText();
                if (start >= 0) {
                    ed.replace(start, end, line);
                    text.setSelection(start + line.length());
                } else {
                    int[] r = lineAt();
                    boolean empty = ed.toString().substring(r[0], r[1]).trim().isEmpty();
                    if (empty) {
                        ed.replace(r[0], r[1], line);
                        text.setSelection(r[0] + line.length());
                    } else {
                        ed.insert(r[1], "\n" + line);
                        text.setSelection(r[1] + 1 + line.length());
                    }
                }
            }
        });
    }

    /** שמירה של הקובץ שהמשתמש עורך (כפתור מפורש — הוא השינוי שביקש). */
    private boolean save() {
        String now = Bidi.strip(text.getText().toString());   // 204: נשמר בלי סימני הכיוון
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
        if (!Bidi.strip(text.getText().toString()).equals(saved)) {
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
