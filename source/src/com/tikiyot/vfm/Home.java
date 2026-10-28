package com.tikiyot.vfm;

// מסך הבית. אחראי: להציג את הפונקציות הישירות והאריחים מהקובץ הראשי, ולהוסיף חדשים.

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Home extends Activity {

    /** 1.9.0: חזרה למסך הבית מכל מקום (START %בית%, כפתור 🏠) — המסכים שמעליו נסגרים. */
    static void go(Activity a) {
        if (a instanceof Home) return;
        a.startActivity(new android.content.Intent(a, Home.class)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP));
    }

    private Library lib;
    private final Handler h = new Handler(Looper.getMainLooper());
    private TextView status;
    private TextView mode;                   // 105: אופן העבודה הפעיל
    private GridLayout grid;
    private LinearLayout permission;
    private PasteBar pasteBar;
    private HomeSelect pick;
    private final java.util.Map<String, TextView> statViews = new java.util.HashMap<String, TextView>();
    private final List<Structure.Func> statFuncs = new ArrayList<Structure.Func>();

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        Log.install();
        Log.initVersion(this);
        Examples.install(this, Paths.data(), internalRoot(), false);        // 156: בפעם הראשונה בלבד
        Envs.all();                                                           // 1.15.0: סביבות ← רשימות, בכל האופנים
        Unify.all();                                                          // 1.17.3: תיקייה מתעדכנת ← חכמה; נפוצה ← תבנית
        Reserved.ensureMode(Examples.MODE);                                   // 1.17.0: "שמור לתוכנה" — ברירות המחדל
        Reserved.ensureBackup(Modes.current());                               // 1.22.3: "שמור לתוכנה / גיבוי" גם באופן הפעיל
        Mirror.rebuild();                                                     // 1.17.5: העותק הקריא — תמיד מעודכן בפתיחה
        CmdTemplates.writeTest(this);                                         // 1.23.0: קובץ הבדיקה (בגרסה הבאה — נמחק)
        lib = Library.get(this);
        setTitle(Words.Main.APP_NAME);

        LinearLayout root = Ui.screen(this);
        int p = Ui.dp(this, 12);
        root.setPadding(p, p, p, p);

        mode = new TextView(this);                                   // 105
        mode.setTextSize(16);
        mode.setTypeface(Typeface.DEFAULT_BOLD);
        mode.setTextColor(Ui.TEXT);
        mode.setBackgroundColor(Ui.BG_SOFT);
        mode.setPadding(Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 12), Ui.dp(this, 10));
        mode.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { modeMenu(); }
        });
        mode.setVisibility(View.GONE);
        root.addView(mode);

        status = Ui.info(this);
        root.addView(status);
        pasteBar = new PasteBar(this, new PasteBar.Place() {                    // 1.14.0: הלוח — הדבקה במסך הבית
            @Override public String cannot() { return VClip.cannotPasteHome(); }
            @Override public VClip.Result paste() { return VClip.pasteHome(); }
        }, new Runnable() {
            @Override public void run() { build(); }
        });
        root.addView(pasteBar.view);
        pick = new HomeSelect(this, new Runnable() {                          // 1.17.1: בחירה מרובה בבית
            @Override public void run() { build(); }
        });
        root.addView(pick.view);

        permission = new LinearLayout(this);
        permission.setOrientation(LinearLayout.VERTICAL);
        permission.setVisibility(View.GONE);
        TextView msg = new TextView(this);
        msg.setText(Words.Main.NEED_ACCESS);
        msg.setTextSize(16);
        msg.setTextColor(Ui.TEXT);
        Button grant = new Button(this);
        grant.setText(Words.Main.GRANT);
        grant.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { Ui.requestFiles(Home.this); }
        });
        permission.addView(msg);
        permission.addView(grant);
        root.addView(permission);

        grid = new GridLayout(this);
        grid.setColumnCount(3);
        root.addView(grid);

        ScrollView sv = new ScrollView(this);
        sv.addView(root);
        setContentView(TopRow.wrap(this, sv));                   // 1.9.0: שורת הכפתורים העליונה
    }

    private String internalRoot() {
        for (Vols.Vol v : Vols.list(this)) if (v.internal) return v.root.getAbsolutePath();
        return "/storage/emulated/0";
    }

    /** 156: הדוגמאות — שוב (אחרי מחיקה), או החזרה למצב ההתחלתי. האופן פעיל — התוכנה נפתחת מחדש. */
    private void restoreExamples() {
        boolean active = Examples.MODE.equals(Modes.current());
        if (!Examples.install(this, Paths.data(), internalRoot(), true)) {
            Ui.toast(this, Words.SAVE_FAILED);
            return;
        }
        Ui.toast(this, Words.WorkModes.EXAMPLES_BACK);
        if (active) enter();                                                  // 163: הקבצים נקראים מחדש
        else modeMenu();
    }

    /** 105: מעבר בין אופני עבודה, או יצירת חדש. */
    void modeMenu() {
        final List<String> all = Modes.all();
        final String cur = Modes.current();
        final boolean noExamples = !all.contains(Examples.MODE);
        List<String> items = new ArrayList<String>();
        for (String m : all) items.add(m.equals(cur) ? "✓ " + m : m);
        if (noExamples) items.add(Words.WorkModes.EXAMPLES_AGAIN);
        items.add(Words.Main.NEW_MODE_MENU);
        String title = Words.Main.MODE + (Ui.firstTimes(this, "modeMenu") ? Words.Main.LONG_PRESS_MODE : "");
        Ui.list(this, title, items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int which) {
                if (noExamples && which == all.size()) {
                    restoreExamples();
                    return;
                }
                if (which >= all.size()) {
                    newMode();
                    return;
                }
                String m = all.get(which);
                if (!m.equals(cur)) Modes.switchTo(Home.this, m);
            }
        }, new Ui.Index1() {
            @Override
            public void ok(int which) {
                if (which < all.size()) modeActions(all.get(which));
            }
        });
    }

    /** 105: שינוי שם ומחיקה של אופן עבודה (נגיעה ארוכה ברשימת האופנים). */
    private void modeActions(final String name) {
        final boolean examples = Examples.MODE.equals(name);                  // 156
        String[] items = examples ? new String[]{Words.Main.RENAME, Words.Main.DELETE_MODE, Words.WorkModes.SHARE, Words.WorkModes.EXAMPLES_RESET}
                : new String[]{Words.Main.RENAME, Words.Main.DELETE_MODE, Words.WorkModes.SHARE};
        Ui.options(this, name + (name.equals(Modes.current()) ? Words.Main.ACTIVE : ""),
                items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        if (which == 0) renameMode(name);
                        else if (which == 1) deleteMode(name);
                        else if (which == 2) shareMode(name);
                        else Ui.ask(Home.this, Words.WorkModes.EXAMPLES_RESET, Words.NO_TOUCH_FILES,
                                Words.WorkModes.EXAMPLES_RESET_WHAT, Words.WorkModes.EXAMPLES_RESET_UNDO,
                                Words.WorkModes.EXAMPLES_RESET_OK, new Runnable() {
                                    @Override public void run() { restoreExamples(); }
                                });
                    }
                });
    }

    /** 158: שיתוף ההגדרות של אופן — הקובץ הראשי והרשימות, בחלון השיתוף של המכשיר. */
    private void shareMode(String name) {
        List<String> f = Modes.settingsFiles(name);
        if (f.isEmpty()) {
            Ui.toast(this, Words.FILE_NOT_FOUND);
            return;
        }
        Acts.shareMany(this, f, new ArrayList<String>(), false);
    }

    private void renameMode(final String name) {
        Ui.askText(this, Words.Main.RENAME_MODE, name, Words.Main.NAME, new Ui.Text1() {
            @Override
            public void ok(String value) {
                String neu = Modes.clean(value);
                if (neu.isEmpty()) {
                    Ui.toast(Home.this, Words.NEED_NAME);
                    return;
                }
                if (neu.equals(name)) return;
                boolean active = name.equals(Modes.current());
                if (active) Library.get(Home.this).flush(4000);       // שהאינדקס יישמר לפני שהתיקייה זזה
                String why = Modes.rename(name, neu);
                if (why != null) {
                    Ui.toast(Home.this, why);
                    return;
                }
                if (active) {
                    Ui.toast(Home.this, Words.Main.RENAMED);
                    Modes.reenter(Home.this);                                // 163: בלי לסגור
                } else {
                    Ui.toast(Home.this, Words.Main.RENAMED);
                    modeMenu();
                }
            }
        });
    }

    private void deleteMode(final String name) {
        if (name.equals(Modes.current())) {
            Ui.toast(this, Words.Main.MODE_ACTIVE_CANT);
            return;
        }
        int[] c = Modes.counts(name);
        Ui.ask(this, Words.Main.DELETE_MODE_TITLE + name + "\"", Words.Main.NO_TOUCH_DRIVE,
                c[0] + Words.Main.LISTS_AND + c[1] + Words.Main.ITEMS_LEAVE,
                Words.Box.IN_BIN,
                Words.Main.DELETE_MODE, new Runnable() {
                    @Override
                    public void run() {
                        String why = Modes.delete(name);
                        Ui.toast(Home.this, why == null ? Words.Main.MODE_DELETED : why);
                        modeMenu();
                    }
                });
    }

    private void newMode() {
        Ui.askText(this, Words.Main.NEW_MODE, "", Words.Main.NEW_MODE_HINT, new Ui.Text1() {
            @Override
            public void ok(final String name) {
                final String cur = Modes.current();
                Ui.options(Home.this, "\"" + Modes.clean(name) + Words.Main.START_WITH,
                        new String[]{Words.Main.EMPTY_MODE, Words.Main.COPY_OF + cur + "\""},
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int which) {
                                String why = Modes.create(name, which == 1 ? cur : null);
                                if (why != null) {
                                    Ui.toast(Home.this, why);
                                    return;
                                }
                                Modes.switchTo(Home.this, name);
                            }
                        });
            }
        });
    }

    /**
     * 90: מבנה.txt או "מבנה התוכנה.ini" חסרים, ויש להם גיבוי — כנראה נמחקו או הועברו מבחוץ.
     * לפני שהתוכנה יוצרת קובץ ריק במקומם, שואלים. true = החלון פתוח, ו-onResume ימשיך אחריו.
     */
    private boolean offerRestore() {
        final File[][] pairs = {
                {Paths.tree(), Files.latestBackup(Paths.tree(), Words.Main.STRUCTURE)},
                {Paths.structure(), Files.latestBackup(Paths.structure(), Words.Main.SETTINGS)}};
        for (final File[] p : pairs) {
            if (p[0].exists() || p[1] == null) continue;
            final boolean lists = p[0].equals(Paths.tree());
            Ui.ask(this, Words.Main.THE_FILE + p[0].getName() + Words.Main.MISSING,
                    lists ? Words.Main.ALL_LISTS_IN_IT : Words.Main.ALL_TILES_IN_IT,
                    Words.Main.BACKUP_FROM + Files.when(p[1].lastModified()) + Words.Main.RESTORE_FROM_IT,
                    Words.Main.NEW_FILE_STARTS_EMPTY,
                    Words.Main.RESTORE, new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Files.copy(p[1], p[0]);
                                Ui.toast(Home.this, Words.Main.RESTORED_FROM_BACKUP);
                            } catch (Throwable t) {
                                Log.e("שחזור " + p[0].getName(), t);
                                Ui.toast(Home.this, Words.RESTORE_FAILED);
                            }
                            onResume();
                        }
                    }, Words.Main.NEW_FILE, new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if (lists) Files.writeAtomic(p[0], "");
                                else Structure.ini();
                            } catch (Throwable t) {
                                Log.e("יצירת " + p[0].getName(), t);
                            }
                            onResume();
                        }
                    });
            return true;
        }
        return false;
    }

    @Override
    protected void onPause() {
        if (Ui.hasFiles(this)) Library.get(this).left();                 // 110
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        enter();
    }

    /** הכניסה למסך — גם אחרי מעבר לאופן עבודה אחר (163: בלי לסגור את התוכנה). */
    void enter() {
        if (!Ui.hasFiles(this)) {
            permission.setVisibility(View.VISIBLE);
            grid.removeAllViews();
            status.setText("");
            return;
        }
        permission.setVisibility(View.GONE);
        Library.get(this).returned();                                    // 110
        mode.setText(Words.Main.MODE_LINE + Modes.current() + "  ▾");
        mode.setVisibility(View.VISIBLE);
        if (offerRestore()) return;          // 90: קובץ עיקרי חסר ויש לו גיבוי — שואלים לפני שנוצר ריק
        Envs.mode(Modes.current());                                           // 1.15.0: אופן שהגיע אחר כך (שיתוף, סל)
        Structure.ensureIds();
        Nested.cleanup();                                                     // 168: רשימות פנימיות יתומות
        Ui.reloadFull();
        build();
        lib.start(new Runnable() {
            @Override
            public void run() {
                h.post(new Runnable() {
                    @Override public void run() { showStatus(); }
                });
            }
        });
        showStatus();
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        onResume();
    }

    private void showStatus() {
        if (!lib.ready) status.setText(Words.LOADING_INDEX);
        else if (!lib.status.isEmpty()) status.setText(lib.status);
        else status.setText(Files.count(lib.totalFiles()) + Words.Main.FILES_IN_INDEX
                    + Files.when(lib.lastBuilt())
                    + (lib.lastBuild().isEmpty() ? "" : Words.Main.BUILT_IN + lib.lastBuild()));   // 1.7.72: כמה זמן לקח
    }

    // ---------- אריחים ----------

    private void build() {
        if (pasteBar != null) pasteBar.refresh();
        Structure.ensureIds();
        grid.removeAllViews();
        statViews.clear();
        statFuncs.clear();
        Structure.Model m = Structure.load();
        String hn = Design.homeName();                                        // 1.12.0: רק אם נבחר — אחרת כמו קודם
        look = hn.isEmpty() ? null : Design.get(hn);
        int cols = look == null ? 3 : look.cols;
        grid.setColumnCount(cols);
        int w = getResources().getDisplayMetrics().widthPixels / cols - Ui.dp(this, look == null ? 16 : 10);
        mode.setTextSize(look == null ? 16 : look.title.size + 4);

        Tree tr = Tree.load();
        for (final Structure.Func f : m.direct) {
            Func fn0 = Func.byKind(f.kind);
            statFuncs.add(f);
            currentStatId = f.id;
            tileColor = Looks.kindColor(f.kind);
            boolean cmd = f.kind.equals(FnCommand.KIND);                       // 1.13.0: פקודה — הצלמית ו"למה" שלה
            String why = cmd ? FnCommand.whyOf(f) : "";
            String tag = tagOf(f.kind);                                       // 1.17.0: הסוג — תמיד (המילון, 24)
            View tl = tile(cmd ? FnCommand.iconOf(f) : iconOf(f, tr), tag, cmd ? FnCommand.nameOf(f) : f.name,
                    !why.isEmpty() ? why : tag != null ? "" : fn0 != null ? fn0.title : f.kind, w, new View.OnClickListener() {
                @Override public void onClick(View v) { if (pick.on()) pick.toggle(f); else run(f); }
            }, new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    pick.toggle(f);                                           // 1.17.1: נגיעה ארוכה — בחירה (הכלל האחיד)
                    return true;
                }
            });
            if (pick.has(f.id)) tl.setBackgroundColor(Select.color(true));
            grid.addView(tl);
        }
        currentStatId = null;
        pendingStat = null;
        tileColor = 0;
        computeStats();
        grid.addView(tile("➕", null, Words.Main.ADD, "", w, new View.OnClickListener() {
            @Override public void onClick(View v) { addSomething(); }
        }, null));
    }

    /** 1.15.0: כלי של רשימה — הצלמית של הרשימה (רשימה כסביבת עבודה — 🧰); אחרת — של סוג הכלי. */
    private static String iconOf(Structure.Func f, Tree tr) {
        if (f.kind.equals(FnVirtual.KIND)) {
            java.util.List<String> p = FnVirtual.base(f.target());
            Tree.Node n = p.isEmpty() ? null : tr.find(p);
            if (n != null && !n.icon.isEmpty() && !n.icon.contains(".")) return n.icon;
        }
        String own = f.params.get("צלמית");                                   // 1.16.0: צלמית שנבחרה בהגדרות // תחביר
        return own != null && !own.isEmpty() ? own : Func.iconOf(f.kind);
    }

    /** 1.12.0: העיצוב של מסך הבית; null — לא נבחר (המראה של קודם). */
    private Design look;
    /** 1.12.1: צבע האריח הבא — לפי סוג הכלי (ל"צבע רקע לפריט" ול"ריבוע צבעוני"). */
    private int tileColor;

    @Override
    public void onBackPressed() {                                             // 1.17.1: חזרה — קודם יוצאים מהבחירה
        if (pick != null && pick.on()) pick.stop();
        else super.onBackPressed();
    }

    /** 1.17.0: תווית הסוג של כלי בבית (null — כלי בלי סוג של רשימה או פקודה). */
    static String tagOf(String kind) {
        if (FnVirtual.KIND.equals(kind)) return Ui.MANUAL;
        if (FnSmart.KIND.equals(kind) || FnDup.KIND.equals(kind)) return Ui.SMART;
        if (FnCommand.KIND.equals(kind)) return Ui.CMD;
        return null;
    }

    private View tile(String icon, String tag, String name, String kindLabel, int w, View.OnClickListener click,
                      View.OnLongClickListener longClick) {
        if (look != null) return Looks.homeTile(this, look, icon, tag, name, kindLabel, w, click, longClick, statFor(), tileColor);
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setGravity(Gravity.CENTER);
        t.setPadding(Ui.dp(this, 6), Ui.dp(this, 14), Ui.dp(this, 6), Ui.dp(this, 14));
        TextView ic = new TextView(this);
        ic.setText(icon);
        ic.setTextSize(30);
        ic.setGravity(Gravity.CENTER);
        TextView nm = new TextView(this);
        nm.setText(name);
        nm.setTextSize(name.length() > 14 ? 12 : 14);                         // 176: שם ארוך — כתב קטן מעט
        nm.setTypeface(Typeface.DEFAULT_BOLD);
        nm.setTextColor(Ui.TEXT);
        nm.setGravity(Gravity.CENTER);
        nm.setMaxLines(3);                                                    // 176: בלי לקטוע מילה אחרונה
        nm.setEllipsize(android.text.TextUtils.TruncateAt.END);
        t.addView(ic);
        if (tag != null) t.addView(Looks.strip(this, tag, Ui.dp(this, 64)), new LinearLayout.LayoutParams(Ui.dp(this, 64), -2));
        t.addView(nm);
        TextView stat = new TextView(this);
        stat.setTextSize(10);
        stat.setTextColor(Ui.SUB);
        stat.setGravity(Gravity.CENTER);
        if (pendingStat != null) stat.setText(pendingStat);
        if (currentStatId != null) statViews.put(currentStatId, stat);
        pendingStat = null;
        if (!kindLabel.isEmpty()) {
            TextView kl = new TextView(this);
            kl.setText(kindLabel);
            kl.setTextSize(11);
            kl.setTextColor(Ui.MUTED);
            kl.setGravity(Gravity.CENTER);
            t.addView(kl);
        }
        t.addView(stat);
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = w;
        t.setLayoutParams(lp);
        t.setOnClickListener(click);
        if (longClick != null) t.setOnLongClickListener(longClick);
        return t;
    }

    private String currentStatId, pendingStat;

    /** שורת הנתונים של האריח (נרשמת לעדכון ברקע). */
    private TextView statFor() {
        TextView stat = new TextView(this);
        if (pendingStat != null) stat.setText(pendingStat);
        if (currentStatId != null) statViews.put(currentStatId, stat);
        pendingStat = null;
        return stat;
    }

    /** נתונים מתחת לכל פונקציה ישירה. מחושבים ברקע, אחרי טעינת האינדקס. */
    private void computeStats() {
        final List<Structure.Func> funcs = new ArrayList<Structure.Func>(statFuncs);
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 50 && !lib.ready; i++) try { Thread.sleep(200); } catch (InterruptedException ignored) { }
                for (final Structure.Func f : funcs) {
                    final String text = Stats.of(Home.this, f);
                    if (text == null) continue;
                    h.post(new Runnable() {
                        @Override
                        public void run() {
                            TextView v = statViews.get(f.id);
                            if (v != null) v.setText(look == null ? text : Looks.lines(text));   // 1.12.1: בלי "·" תלוי
                        }
                    });
                }
            }
        }).start();
    }

    private void run(Structure.Func f) {
        Func fn = Func.byKind(f.kind);
        if (fn == null) Ui.toast(this, Words.Main.UNKNOWN_TOOL + f.kind);
        else fn.open(this, f.kind.equals(FnVirtual.KIND) ? FnVirtual.ensureOwn(this, f) : f.target());
    }

    // ---------- הוספה ----------

    /**
     * 1.13.1 — ➕ בבית (הוכרע 16.10, המשתמש: "כשלוחצים על הפלוס במסך הבית מקבלים את פירוט כלי העבודה הקיימים, והם:
     * רשימה ידנית, רשימה חכמה ופקודה; מאופן עבודה אחר — וזהו, כל השאר נמחק"; ו"פקודה / קיצור דרך, כי הם אותו דבר").
     * יצאו: "↪ הוספה לבית", סביבת עבודה חדשה, כפולים, סל, פס, אפליקציה, הגדרות (הקיימים ממשיכים לעבוד).
     */
    private void addSomething() {
        Ui.options(this, Words.Main.WHAT_TO_ADD,
                new String[]{Words.Main.ADD_MANUAL, Words.Main.ADD_SMART, Words.Main.ADD_CMD},
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        if (which == 0) Func.byKind(FnVirtual.KIND).configure(Home.this, null, null, added);
                        else if (which == 1) SearchTabs.configure(Home.this, SearchTabs.KINDS[0], null, null, added);
                        else CmdMenu.show(Home.this, null, new CmdMenu.Picked() {   // 1.16.1: תפריט הפקודות האחד
                            @Override
                            public void ok(String icon, String name, String line, java.io.File file) {
                                Shortcuts.add(Home.this, file != null ? file : Shortcuts.write(icon, name, line), name, new Runnable() {
                                    @Override public void run() { build(); }
                                });
                            }
                        }, null);
                    }
                });
    }

    /** כלי שהטופס החזיר — למסך הבית. */
    private final Func.Done added = new Func.Done() {
        @Override
        public void ok(String name, Target target) {
            Func f = Func.byKind(target.kind);
            Structure.addDirectFunc(name.isEmpty() && f != null ? f.title : name, target.kind, "", target);
            build();
        }
    };

    // ---------- תפריטים ----------

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 2, 0, Words.BATCH_MENU);
        menu.add(0, 1, 1, Words.Main.SETTINGS);
        menu.add(0, 3, 2, Words.DesignW.MENU);                         // 1.12.0
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 1) {
            startActivity(new Intent(this, SettingsScreen.class));
            return true;
        }
        if (item.getItemId() == 3) {                                    // 1.12.0: עיצוב למסך הבית
            String cur = Design.homeName();
            DesignUi.choose(this, Words.DesignW.TITLE_HOME, cur.isEmpty() ? Design.DEFAULT : cur, null, false,
                    new DesignUi.Done() {
                        @Override
                        public void chosen(String name) {
                            Design.setHome(name);
                            build();
                        }

                        @Override public void changed() { build(); }
                    });
            return true;
        }
        if (item.getItemId() == 2) {                                    // 117א
            CmdMenu.show(this, null);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
