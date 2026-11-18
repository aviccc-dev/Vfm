package com.tikiyot.vfm;

// שורות הכפתורים בכל מסכי התוכנה. 1.9.0: שורה עליונה; 1.10.0: למעלה ולמטה, כלליות ושל סוג המסך, עד 5 שורות בכל מקום.
// מ0.41 (המשתמש, 12.11.2026: "ברשימה 'שמור לתוכנה' יהיו רשימות — שורה למעלה 1 עד 5, שורה למטה 1 עד 5 — ובכל רשימה כזו אפשר
// יהיה להוסיף פקודות, והם יופיעו בתוכנה"): השורות נלקחות מהרשימות (Menus) — "שורה למעלה N" בכל המסכים, ואחריה באותה שורה
// "<שם המסך> / שורה למעלה N". כפתור = קובץ פקודות שברשימה (הצלמית והשם — מהכותרת שלו); תת־רשימה — תפריט משנה; חכמה — מה
// שהיא מוצאת. נגיעה — הרצה. העריכה — ברשימה עצמה: ➕ שבסוף השורה פותח את הרשימה שלה (וגם נגיעה ארוכה על כפתור).
// עד מ0.40 — מקובץ "כפתורים.txt", עם עריכה כאן; הקובץ הישן עובר לרשימות פעם אחת (Menus.install).
// שורת חירום: אין אף שורה עליונה (הרשימות רוקנו, או המסד לא נקרא) — בית · פקודות · חיפוש · קבצים · חזרה, כמו ברירת המחדל.
// פס הבחירה — נשאר כמו שהוא; בזמן בחירה השורות התחתונות מוסתרות.
// אחראי: הציור וההרצה. מה בשורות — Menus.

import android.app.Activity;
import android.content.DialogInterface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

final class TopRow {

    static final String BOTTOM_TAG = "rows-bottom";  // תחביר

    private final Activity a;
    private final LinearLayout top, bottom;
    private long built = -1;
    private String screen;
    /** האופן שהמסך נפתח בו — מסך שאינו רשימה חוזר אליו אחרי שנפתחה מעליו רשימה של אופן אחר (סידור תפריט). */
    private final String mode;

    private TopRow(Activity a, LinearLayout top, LinearLayout bottom) {
        this.a = a;
        this.top = top;
        this.bottom = bottom;
        this.mode = Db.currentMode;
    }

    /**
     * שם המסך, לשורות שלו ("הבית / שורה למטה 1"): השמות של "פתח מסך", ועוד "רשימה" ו"רשימה חכמה" (המשתמש: "אם יש להם שמות
     * שמורים, אז אפשר להמשיך באותה שיטה"). null — למסך אין שורות משלו, רק הכלליות.
     */
    static String screenOf(Activity a) {
        if (a instanceof DbListScreen) return ((DbListScreen) a).isHomeScreen() ? Verbs.S_HOME : Verbs.S_LIST_SCREEN;
        if (a instanceof SearchScreen) return ((SearchScreen) a).rowsScreen();
        if (a instanceof DiskScreen) return Verbs.S_DISK;
        if (a instanceof TrashScreen) return Verbs.S_TRASH;
        if (a instanceof SettingsScreen) return Verbs.S_SETTINGS;
        if (a instanceof LabScreen) return Verbs.S_PLUGINS;
        return null;
    }

    /** המסך עם השורות סביבו. כל המסכים קוראים לזה במקום setContentView(content). */
    static View wrap(final Activity a, View content) {
        ScrollRoom box = new ScrollRoom(a);                                   // מבנה 275: רצועה דקה מקפלת ומחזירה את השורות והכותרת
        box.setOrientation(LinearLayout.VERTICAL);
        box.setLayoutDirection(Ui.DIRECTION);
        box.setBackgroundColor(Ui.BG);
        LinearLayout top = new LinearLayout(a);
        top.setOrientation(LinearLayout.VERTICAL);
        LinearLayout bottom = new LinearLayout(a);
        bottom.setOrientation(LinearLayout.VERTICAL);
        bottom.setTag(BOTTOM_TAG);
        box.addView(top, new LinearLayout.LayoutParams(-1, -2));
        box.addView(box.strip(), new LinearLayout.LayoutParams(-1, -2));
        if (content.getParent() instanceof ViewGroup) ((ViewGroup) content.getParent()).removeView(content);
        box.addView(content, new LinearLayout.LayoutParams(-1, 0, 1f));
        LinearLayout hold = new LinearLayout(a);                               // מבנה 272: מחזיק לשורות שלמטה — הקיפול מסתיר אותו,
        hold.setOrientation(LinearLayout.VERTICAL);                            // ופס הבחירה ממשיך להסתיר את השורות עצמן (bottomOf)
        hold.addView(bottom, new LinearLayout.LayoutParams(-1, -2));
        box.addView(hold, new LinearLayout.LayoutParams(-1, -2));
        box.parts(top, content, hold);
        final TopRow t = new TopRow(a, top, bottom);
        t.build();
        // מסך שחזר לקדמה — אם התפריטים שונו (ברשימות), או שהמסך עצמו השתנה (הבית / רשימה) — השורות נבנות מחדש
        box.getViewTreeObserver().addOnWindowFocusChangeListener(new ViewTreeObserver.OnWindowFocusChangeListener() {
            @Override
            public void onWindowFocusChanged(boolean focus) {
                if (!focus) return;
                if (!(a instanceof DbListScreen) && t.mode != null) Db.currentMode = t.mode;
                String now = screenOf(a);
                if (t.built != Menus.version(a) || (now == null ? t.screen != null : !now.equals(t.screen))) t.build();
            }
        });
        return box;
    }

    /** השורות התחתונות של המסך — כדי שפס הבחירה יסתיר אותן בזמן בחירה. */
    static View bottomOf(View box) {
        return box.findViewWithTag(BOTTOM_TAG);
    }

    private void build() {
        built = Menus.version(a);
        screen = screenOf(a);
        top.removeAllViews();
        bottom.removeAllViews();
        Menus.Snap m = Menus.get(a);
        int shown = 0;
        for (Menus.Row r : m.rows(screen, Menus.ROW_TOP)) shown += add(top, r, true);
        if (shown == 0) top.addView(emergency());
        for (Menus.Row r : m.rows(screen, Menus.ROW_BOTTOM)) add(bottom, r, false);
    }

    /** שורה אחת; מחזיר כמה כפתורים יש בה (קובץ שאינו בכונן — לא מוצג; שורה בלי אף כפתור — לא מוצגת). */
    private int add(LinearLayout into, final Menus.Row row, boolean up) {
        LinearLayout r = line();
        int n = 0;
        for (final Menus.Item it : row.items) {
            View v;
            if (it.e.kind == DbMore.E_FILE) {
                final File f = new File(it.e.path);
                if (!f.isFile()) continue;
                String[] look = Menus.look(f);
                v = button(look[0], look[1], Ui.ACCENT);
                v.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View x) { DbCmdMenu.use(a, f, null, null); }
                });
            } else if (it.e.kind == DbMore.E_LIST) {
                v = button("☰", it.e.label, Ui.ACCENT);
                v.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View x) {
                        List<String> p = new ArrayList<String>();
                        p.add(Reserved.ROOT);
                        p.addAll(it.path);                                      // הרשימה של השורה (הכללית, או של המסך)
                        p.add(it.e.label);
                        if (!DbCmdMenu.show(a, p, null, null, false)) Ui.toast(a, Words.Verb.NOT_FOUND + it.e.label);
                    }
                });
            } else {
                v = button("📜", it.e.label, Ui.ACCENT);
                v.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View x) { CmdMenu.found(a, it.e.label, it.e.cond == null ? "" : it.e.cond, null); }
                });
            }
            v.setOnLongClickListener(new View.OnLongClickListener() {              // מבנה 206: העריכה — במקום, דרך הרשימה
                @Override
                public boolean onLongClick(View x) {
                    buttonMenu(row, it);
                    return true;
                }
            });
            r.addView(v);
            n++;
        }
        if (n == 0) return 0;
        r.addView(plus(row.side, row.n));
        into.addView(framed(r, Ui.BAR, up));
        return n;
    }

    /** שורת החירום: כשאין אף שורה עליונה מהרשימות — ברירת המחדל שבקוד, כדי שתמיד יהיו בית וחזרה. */
    private View emergency() {
        LinearLayout r = line();
        for (final Buttons.Btn b : Buttons.defaults()) {
            View v = button(b.icon, b.name, Ui.ACCENT);
            v.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View x) { VerbsUi.run(a, b.act, null); }
            });
            r.addView(v);
        }
        r.addView(plus(Menus.ROW_TOP, 1));
        return framed(r, Ui.BAR, true);
    }

    private LinearLayout line() {
        LinearLayout r = new LinearLayout(a);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setLayoutDirection(Ui.DIRECTION);
        int p = Ui.dp(a, 4);
        r.setPadding(p, p, p, p);
        return r;
    }

    private View framed(LinearLayout r, int color, boolean up) {
        LinearLayout wrap = new LinearLayout(a);
        wrap.setOrientation(LinearLayout.VERTICAL);
        HorizontalScrollView sv = new HorizontalScrollView(a);
        sv.setHorizontalScrollBarEnabled(false);
        sv.setBackgroundColor(color);
        sv.addView(r);
        View rule = new View(a);
        rule.setBackgroundColor(Ui.RULE);
        if (!up) wrap.addView(rule, new LinearLayout.LayoutParams(-1, 1));
        wrap.addView(sv, new LinearLayout.LayoutParams(-1, -2));
        if (up) wrap.addView(rule, new LinearLayout.LayoutParams(-1, 1));
        return wrap;
    }

    /**
     * ➕ בסוף כל שורה: פותח את הרשימה של השורה (באופן "שמור לתוכנה") — שם מוסיפים פקודות, מסירים ומסדרים, כמו בכל רשימה.
     * הבחירה: השורה בכל המסכים · השורה במסך הזה בלבד · שורה חדשה.
     */
    private View plus(final String side, final int n) {
        View add = button("➕", Words.Top.ADD, Ui.SUB);
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View x) {
                final List<String> labels = new ArrayList<String>();
                final List<Runnable> runs = new ArrayList<Runnable>();
                labels.add(Words.Top.rowAddButton(side, n, Words.Top.ALL_SCREENS));   // מבנה 206: כפתור חדש — ישר בשורה
                runs.add(new Runnable() { @Override public void run() { addButton(null, side, n); } });
                if (screen != null) {
                    labels.add(Words.Top.rowAddButton(side, n, screen));
                    runs.add(new Runnable() { @Override public void run() { addButton(screen, side, n); } });
                }
                labels.add(Words.Top.rowAll(side, n));
                runs.add(new Runnable() { @Override public void run() { arrange(null, side, n); } });
                if (screen != null) {
                    labels.add(Words.Top.rowHere(side, n, screen));
                    runs.add(new Runnable() { @Override public void run() { arrange(screen, side, n); } });
                }
                for (final String sd : new String[]{Menus.ROW_TOP, Menus.ROW_BOTTOM}) {
                    final int free = freeRow(sd);
                    if (free == 0) continue;
                    labels.add(Words.Top.rowNew(sd, free));
                    runs.add(new Runnable() { @Override public void run() { arrange(null, sd, free); } });
                }
                Ui.list(a, Words.Top.ROWS_TITLE, labels, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) { runs.get(w).run(); }
                });
            }
        });
        return add;
    }

    /** המספר של השורה הכללית הפנויה הבאה בצד הזה (0 — כבר חמש). */
    private int freeRow(String side) {
        Menus.Snap m = Menus.get(a);
        for (int n = 1; n <= Menus.MAX_ROWS; n++) if (m.at(Menus.rowPath(null, side, n)).isEmpty()) return n;
        return 0;
    }

    /**
     * מבנה 206 (המשתמש, 16.11.2026: "את השורה החדשה והכפתורים העברנו לרשימה בשמור לתוכנה. חשבתי שעדיף להשאיר גם את השיטה הישנה,
     * רק שהיא תעבור בדרך הזו של שמור לתוכנה — היא יותר אינטואיטיבית"). נגיעה ארוכה על כפתור — התפריט של עד מ0.40 (עריכה, ימינה,
     * שמאלה, הסרה), אבל שום דבר לא נשמר כאן: כל שינוי הוא פקודה על הרשימה של השורה, דרך המנהל — המקום האחד נשאר הרשימה.
     */
    private void buttonMenu(Menus.Row row, final Menus.Item it) {
        int at = -1, of = 0;
        for (Menus.Item x : row.items)
            if (x.list == it.list) {
                if (x == it) at = of;
                of++;
            }
        final List<String> labels = new ArrayList<String>();
        final List<Runnable> runs = new ArrayList<Runnable>();
        if (it.e.kind == DbMore.E_FILE) {
            labels.add(Words.Top.B_EDIT);
            runs.add(new Runnable() { @Override public void run() { LabScreen.edit(a, new File(it.e.path)); } });
        }
        if (at > 0) {
            labels.add(Words.Top.B_RIGHT);
            runs.add(new Runnable() { @Override public void run() { order(it, DbMore.UP); } });
        }
        if (at >= 0 && at < of - 1) {
            labels.add(Words.Top.B_LEFT);
            runs.add(new Runnable() { @Override public void run() { order(it, DbMore.DOWN); } });
        }
        labels.add(Words.Top.B_REMOVE);
        runs.add(new Runnable() { @Override public void run() { remove(it); } });
        labels.add(Words.Top.B_LIST);
        runs.add(new Runnable() { @Override public void run() { DbListScreen.open(a, it.mode, it.list); } });
        Ui.list(a, it.e.label, labels, new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface d, int w) { runs.get(w).run(); }
        });
    }

    /** מבנה 206: ימינה / שמאלה — "עדכן רשימה %נבחרים% /מקום:…" על הרשימה של השורה (Manager.onListOrder). */
    private void order(Menus.Item it, int how) {
        Library lib = Library.get(a);
        Manager.Act act = Manager.onListOrder(it.mode, it.list, java.util.Collections.singletonList(it.e.id), how, Paths.data(), BatchUi.vars(a, Paths.data()), lib);
        String said = act.error() != null ? act.error() : act.say(lib);
        if (act.error() != null || act.failed) Ui.toast(a, said);
        build();
    }

    /** מבנה 206: הסרה מהשורה — "בטל ידנית %נבחרים%" על הרשימה של השורה (Manager.onListRemove), אחרי אישור. הקובץ נשאר. */
    private void remove(final Menus.Item it) {
        final Library lib = Library.get(a);
        final Manager.Act act = Manager.onListRemove(it.mode, it.list, java.util.Collections.singletonList(it.e.id), Paths.data(), BatchUi.vars(a, Paths.data()), lib);
        if (act.error() != null) {
            Ui.toast(a, act.error());
            return;
        }
        Ui.confirm(a, Words.Top.REMOVE_TITLE, Words.Top.removeQuestion(it.e.label), Words.Top.REMOVE_YES, new Runnable() {
            @Override
            public void run() {
                String said = act.say(lib);
                if (act.failed) Ui.toast(a, said);
                build();
            }
        });
    }

    /** מבנה 206: ➕ — כפתור חדש בשורה הזו, בלי לפתוח את הרשימה: בוחרים פקודה, והיא נכנסת לרשימה של השורה (דרך המנהל), והשורה נבנית מחדש. */
    private void addButton(final String ofScreen, final String side, final int n) {
        long id = rowList(ofScreen, side, n);
        if (id == 0) return;
        final long in = id;
        CmdMenu.show(a, null, new CmdMenu.Picked() {                             // אותה בחירה כמו ב־➕ שברשימה (DbVerbsUi.addCommand)
            @Override
            public void ok(String icon, String name, String line, File file) {
                File f = file != null ? file : Shortcuts.write(icon, name, line);
                DbVerbsUi.addToList(a, Menus.MODE, in, java.util.Collections.singletonList(f.getAbsolutePath()), false);   // מבנה 114: דרך המנהל
                build();
            }
        });
    }

    /** הרשימה של השורה באופן "שמור לתוכנה" (נוצרת אם חסרה). 0 — לא נוצרה. */
    private long rowList(final String ofScreen, final String side, final int n) {
        long id;
        boolean sys = Permit.systemOpen(Permit.SYS_TOP_ROW);                      // מבנה 124: פעולה של המערכת (התפריטים), רשומה אצל המנהל
        try {
            id = DbVerbsUi.write(a, Menus.MODE, new DbVerbsUi.Job() {
                @Override
                public long run(Store s, List<String> p) {
                    return Menus.ensure(s, DbPaths.ensureHome(s), Menus.rowPath(ofScreen, side, n));
                }
            }, null);
        } finally {
            Permit.systemClose(sys);
        }
        return id;
    }

    /** פותח את הרשימה של השורה באופן "שמור לתוכנה" (נוצרת אם חסרה). */
    private void arrange(final String ofScreen, final String side, final int n) {
        long id = rowList(ofScreen, side, n);
        if (id == 0) return;
        Ui.toast(a, Words.Top.ROW_HINT);
        DbListScreen.open(a, Menus.MODE, id);
    }

    private View button(String icon, String name, int color) {
        LinearLayout b = new LinearLayout(a);
        b.setOrientation(LinearLayout.VERTICAL);
        b.setGravity(Gravity.CENTER);
        int p = Ui.dp(a, 6);
        b.setPadding(p, Ui.dp(a, 2), p, Ui.dp(a, 2));
        b.setMinimumWidth(Ui.dp(a, 52));
        b.setClickable(true);
        b.setFocusable(true);
        android.util.TypedValue tv = new android.util.TypedValue();
        if (a.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true))
            b.setBackgroundResource(tv.resourceId);
        TextView ic = new TextView(a);
        ic.setText(icon);
        ic.setTextSize(18);
        ic.setTextColor(color);
        ic.setGravity(Gravity.CENTER);
        TextView nm = new TextView(a);
        nm.setText(name);
        nm.setTextSize(11);
        nm.setTextColor(color);
        nm.setMaxLines(1);                                                   // מבנה 273 (ממצא 65): שם ארוך — מתקצר עם "…", ולא מגביה את כל השורה (עד כאן: שתי שורות)
        nm.setEllipsize(android.text.TextUtils.TruncateAt.END);
        nm.setMaxWidth(Ui.dp(a, 96));
        nm.setGravity(Gravity.CENTER);
        b.addView(ic);
        b.addView(nm);
        return b;
    }
}
