package com.tikiyot.vfm;

// חלון הבורר האחד (106), לרשימות ולתיקיות: שתי תצוגות — תיקיות (רמה אחת בכל פעם, כמו מנהל קבצים) ועץ
// (הכול, עם פס מחבר בין הרמות; בעץ גדול — ענפים שנפתחים ונסגרים) — וחיפוש קטן. התצוגה האחרונה שנבחרה
// נפתחת בפעם הבאה, בכל בורר. המבנה — PickerModel; המקור — PickSource; כאן רק ממשק.
// אסור לו: לשנות דבר. יצירה — של מי שקרא לו (Creator).

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

final class PickDialog {

    interface Chosen {
        void ok(ArrayList<String> path);
    }

    /** יצירת צומת חדש בתוך parent: שואל שם, יוצר, וקורא ל-done עם המסלול החדש. */
    interface Creator {
        void create(Activity a, ArrayList<String> parent, Chosen done);
    }

    /** מילים וצלמית של סוג הבורר. */
    static final class Kind {
        final String icon, newLabel, inside, none, searchHint;
        /** 171: מילת הבחירה ("לכאן"), מה אומרים כשאי אפשר לבחור, ושורת הסבר בראש החלון (או null). */
        String pick = Words.PICK_HERE, notTarget = Words.NOT_LIST_TARGET;

        Kind(String icon, String newLabel, String inside, String none, String searchHint) {
            this.icon = icon;
            this.newLabel = newLabel;
            this.inside = inside;
            this.none = none;
            this.searchHint = searchHint;
        }
    }

    static final Kind LISTS = new Kind("📋", Words.NEW_LIST, Words.INSIDE_LIST, Words.NO_LISTS_HERE, Words.SEARCH_LIST);
    static final Kind FOLDERS = new Kind("📁", Words.NEW_FOLDER, Words.INSIDE_FOLDER, Words.NO_FOLDERS_HERE, Words.SEARCH_FOLDER);
    private static final int GUTTER_DP = 20;

    /** creator = null: אי אפשר ליצור. crossTool = null: אין מעבר לרשימה של כלי אחר. */
    static void show(Activity a, String title, PickerModel m, Kind kind, Creator creator, Chosen cb, Runnable crossTool) {
        new PickDialog(a, title, m, kind, creator, cb, crossTool).open();
    }

    /** 204: עם יעד נוסף ליד הכוננים (למשל "📤 שיתוף") — שורה ברמת הכוננים; נגיעה סוגרת ומריצה. */
    static void show(Activity a, String title, PickerModel m, Kind kind, Creator creator, Chosen cb, String extraLabel, Runnable extra) {
        PickDialog d = new PickDialog(a, title, m, kind, creator, cb, null);
        d.extraLabel = extraLabel;
        d.extra = extra;
        d.open();
    }

    private String extraLabel;
    private Runnable extra;

    private final Activity a;
    private final String title;
    private final PickerModel m;
    private final Kind kind;
    private final Creator creator;
    private final Chosen cb;
    private final Runnable crossTool;
    private boolean tree;
    private String query = "";
    private AlertDialog dialog;
    private ScrollView sv;
    private LinearLayout rows;
    private TextView crumb, toggle;
    private EditText search;

    private PickDialog(Activity a, String title, PickerModel m, Kind kind, Creator creator, Chosen cb, Runnable crossTool) {
        this.a = a;
        this.title = title;
        this.m = m;
        this.kind = kind;
        this.creator = creator;
        this.cb = cb;
        this.crossTool = crossTool;
        tree = "tree".equals(a.getSharedPreferences("vfm", Context.MODE_PRIVATE).getString("pickerView", "folders"));
    }

    private void open() {
        LinearLayout root = new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(Ui.DIRECTION);
        int pad = Ui.dp(a, 8);
        root.setPadding(pad, pad, pad, 0);
        root.setFocusable(true);                                  // שהמקלדת לא תיפתח מעצמה על שדה החיפוש
        root.setFocusableInTouchMode(true);

        toggle = new TextView(a);
        toggle.setTextSize(13);
        toggle.setTextColor(Ui.TEXT);
        toggle.setBackgroundColor(Ui.HEADER);
        toggle.setPadding(Ui.dp(a, 12), Ui.dp(a, 8), Ui.dp(a, 12), Ui.dp(a, 8));
        toggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tree = !tree;
                a.getSharedPreferences("vfm", Context.MODE_PRIVATE).edit()
                        .putString("pickerView", tree ? "tree" : "folders").apply();
                sv.scrollTo(0, 0);
                render();
            }
        });
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tp.bottomMargin = Ui.dp(a, 6);
        LinearLayout top = new LinearLayout(a);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setLayoutDirection(Ui.DIRECTION);
        top.addView(toggle, tp);
        if (m.src.canShowHidden()) {                                          // 130
            final TextView hid = new TextView(a);
            hid.setTextSize(13);
            hid.setTextColor(Ui.SUB);
            hid.setPadding(Ui.dp(a, 12), Ui.dp(a, 8), Ui.dp(a, 12), Ui.dp(a, 8));
            hid.setText(Words.Pick.HIDDEN_NO);
            hid.setOnClickListener(new View.OnClickListener() {
                boolean on;

                @Override
                public void onClick(View v) {
                    on = !on;
                    hid.setText(Words.Pick.HIDDEN_IS + (on ? Words.Pick.YES : Words.Pick.NO));
                    m.showHidden(on);
                    render();
                }
            });
            LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            hp.setMarginStart(Ui.dp(a, 8));
            top.addView(hid, hp);
        }
        root.addView(top);

        search = Ui.field(a, "", "🔍 " + kind.searchHint);
        search.setTextSize(14);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int af) { }
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) { }
            @Override
            public void afterTextChanged(Editable s) {
                query = s.toString().trim();
                render();
            }
        });
        root.addView(search, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        crumb = new TextView(a);
        crumb.setTextSize(13);
        crumb.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        crumb.setTextColor(Ui.SUB);
        crumb.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        crumb.setPadding(Ui.dp(a, 4), Ui.dp(a, 10), Ui.dp(a, 4), Ui.dp(a, 4));
        root.addView(crumb);

        rows = new LinearLayout(a);
        rows.setOrientation(LinearLayout.VERTICAL);
        root.addView(rows, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        sv = new ScrollView(a);
        sv.addView(root);

        dialog = new AlertDialog.Builder(a)
                .setTitle(title)
                .setView(sv)
                .setPositiveButton(kind.pick, null)
                .setNeutralButton("➕ " + kind.newLabel, null)
                .setNegativeButton(Words.CANCEL, null)
                .create();
        if (dialog.getWindow() != null)
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                    | WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        dialog.show();
        root.requestFocus();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { done(m.current); }
        });
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { createHere(); }
        });
        render();
    }

    // ---------- ציור ----------

    private void render() {
        boolean searching = !query.isEmpty();
        boolean folders = !tree && !searching;
        toggle.setText(Words.Pick.VIEW_IS + (tree ? "🌳 " + Words.VIEW_TREE : "📁 " + Words.VIEW_FOLDERS) + "   ⇄");
        crumb.setVisibility(folders ? View.VISIBLE : View.GONE);
        if (folders) crumb.setText("📂 " + m.crumb());
        rows.removeAllViews();

        if (folders && m.canUp())
            rows.addView(plain("⬆ " + Words.PICK_UP, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    m.up();
                    sv.scrollTo(0, 0);
                    render();
                }
            }));
        boolean top = m.current == null || (m.roots.size() == 1 && m.current.equals(m.roots.get(0)));
        if (extra != null && !searching && top)                               // 204: ליד הכוננים · 1.13.0: בראש, יחד עם הכוננים — לא אחרי כל התיקיות
            rows.addView(plain(extraLabel, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                    extra.run();
                }
            }));
        List<PickerModel.Row> list = searching ? m.search(query) : tree ? m.treeRows() : m.folderRows();
        for (PickerModel.Row r : list) rows.addView(rowView(r, tree && !searching, folders));
        if (searching && m.truncated)
            rows.addView(note(Words.Pick.SHOWING + PickerModel.SEARCH_MAX + Words.Pick.FIRST_NARROW));
        if (crossTool != null && m.crossTool() && !searching && (tree || m.current.equals(m.scopeBase)))
            rows.addView(plain("📂 " + Words.OTHER_TOOL_LIST, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                    crossTool.run();
                }
            }));
        if (list.isEmpty() && !(extra != null && top)) rows.addView(note(searching ? Words.Pick.NOT_FOUND : kind.none));
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setVisibility(folders && m.canChooseHere() ? View.VISIBLE : View.GONE);
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                .setVisibility(creator != null ? View.VISIBLE : View.GONE);
    }

    private StateListDrawable pressed() {
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Ui.SELECTED));
        s.addState(new int[]{}, new ColorDrawable(0));
        return s;
    }

    private View note(String text) {
        TextView none = new TextView(a);
        none.setText(text);
        none.setTextColor(Ui.MUTED);
        none.setTextSize(14);
        none.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        none.setPadding(Ui.dp(a, 8), Ui.dp(a, 12), Ui.dp(a, 8), Ui.dp(a, 12));
        return none;
    }

    private View plain(String text, View.OnClickListener click) {
        TextView t = new TextView(a);
        t.setText(text);
        t.setTextSize(15);
        t.setTextColor(Ui.SUB);
        t.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        t.setMinHeight(Ui.dp(a, 44));
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setPadding(Ui.dp(a, 10), Ui.dp(a, 8), Ui.dp(a, 10), Ui.dp(a, 8));
        t.setBackground(pressed());
        t.setOnClickListener(click);
        return t;
    }

    private View rowView(final PickerModel.Row r, boolean withGutter, boolean folderMode) {
        LinearLayout row = new LinearLayout(a);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutDirection(Ui.DIRECTION);
        row.setGravity(Gravity.CENTER_VERTICAL);
        if (withGutter && r.depth > 0)
            row.addView(new Gutter(a, r.cont, r.last), new LinearLayout.LayoutParams(
                    Ui.dp(a, GUTTER_DP) * r.depth, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(Ui.dp(a, 8), Ui.dp(a, 10), Ui.dp(a, 8), Ui.dp(a, 10));
        TextView label = new TextView(a);
        String ic = m.src.icon(r.path);                                       // 171: צלמית לכל צומת
        label.setText((ic != null ? ic : kind.icon) + "  " + r.label);
        label.setTextSize(16);
        label.setTextColor(r.pickable ? Ui.TEXT : Ui.MUTED);
        label.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        box.addView(label);
        if (r.sub != null) {
            TextView sub = new TextView(a);
            sub.setText(r.sub);
            sub.setTextSize(12);
            sub.setTextColor(Ui.SUB);
            sub.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
            box.addView(sub);
        }
        row.addView(box, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        if (folderMode && r.kids) {                                 // יש בפנים עוד: נכנסים
            row.addView(arrow("‹", null));
        } else if (withGutter && r.expandable) {                    // עץ גדול: פתיחה וסגירה של הענף
            row.addView(arrow(r.open ? "▾" : "◂", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    m.toggle(r.path);
                    render();
                }
            }));
        }
        row.setMinimumHeight(Ui.dp(a, 44));
        row.setBackground(pressed());
        row.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { tap(r); }
        });
        row.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                popup(r);
                return true;
            }
        });
        return row;
    }

    private TextView arrow(String s, View.OnClickListener click) {
        TextView more = new TextView(a);
        more.setText(s);
        more.setTextSize(22);
        more.setTextColor(Ui.MUTED);
        more.setGravity(Gravity.CENTER);
        more.setMinWidth(Ui.dp(a, 44));
        more.setMinHeight(Ui.dp(a, 44));
        if (click != null) more.setOnClickListener(click);
        return more;
    }

    // ---------- פעולות ----------

    private void tap(PickerModel.Row r) {
        if (!tree && query.isEmpty()) {                           // תיקיות: נכנסים
            m.enter(r.path);
            sv.scrollTo(0, 0);
            render();
            return;
        }
        if (!r.pickable) {
            notTarget(r);
            return;
        }
        if (m.direct || creator == null) done(r.path); else popup(r);
    }

    /** "לכאן" או חדש בתוך הצומת הזה. גם בנגיעה ארוכה, בכל תצוגה. */
    private void popup(final PickerModel.Row r) {
        if (!r.pickable) {
            notTarget(r);
            return;
        }
        if (creator == null) {
            done(r.path);
            return;
        }
        Ui.options(a, r.label, new String[]{kind.pick, "➕ " + kind.newLabel + " " + kind.inside},
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        if (w == 0) done(r.path); else make(r.path);
                    }
                });
    }

    private void notTarget(PickerModel.Row r) {
        String why = m.src.whyNot(r.path);
        Ui.toast(a, why != null ? why : kind.notTarget);
    }

    private void done(ArrayList<String> path) {
        dialog.dismiss();
        cb.ok(path);
    }

    private void make(ArrayList<String> parent) {
        dialog.dismiss();
        creator.create(a, parent, cb);
    }

    private void createHere() {
        ArrayList<String> where = m.createIn(!tree && query.isEmpty());
        if (where != null) {
            make(where);
            return;
        }
        if (m.roots.size() == 1) {                                // באיזו נקודת פתיחה (כלי או כונן)
            make(m.roots.get(0));
            return;
        }
        Ui.list(a, kind.newLabel + Words.Pick.INSIDE, m.rootNames, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int w) { make(m.roots.get(w)); }
        });
    }

    /**
     * הפס המחבר בתצוגת עץ. עמודה לכל רמה: קו אנכי בכל רמה שיש בה עוד אחים, ובעמודה האחרונה
     * גם ענף אל הטקסט. מצויר, ולא באותיות, כדי שיהיה ישר בכל גופן. הכיוון — מימין לשמאל.
     */
    private static final class Gutter extends View {
        private final boolean[] cont;
        private final boolean last;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        Gutter(Context c, boolean[] cont, boolean last) {
            super(c);
            this.cont = cont;
            this.last = last;
            p.setColor(Ui.MUTED);
            p.setStrokeWidth(Ui.dp(c, 1.5f));
        }

        private float stem(int k, float unit, boolean rtl) {
            return rtl ? getWidth() - (k + 0.5f) * unit : (k + 0.5f) * unit;
        }

        @Override
        protected void onDraw(Canvas cv) {
            boolean rtl = getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
            float h = getHeight(), unit = getWidth() / (float) (cont.length + 1);
            for (int k = 0; k < cont.length; k++)
                if (cont[k]) {
                    float x = stem(k, unit, rtl);
                    cv.drawLine(x, 0, x, h, p);
                }
            float x = stem(cont.length, unit, rtl);
            cv.drawLine(x, 0, x, last ? h / 2f : h, p);
            cv.drawLine(x, h / 2f, rtl ? 0 : getWidth(), h / 2f, p);
        }
    }
}
