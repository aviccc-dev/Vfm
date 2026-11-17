package com.tikiyot.vfm;

// מ0.9: רשימה מהמסד במראה המוכר — אותו שלד כמו כל מסכי הרשימות (ListScreen): השורות או הרשת לפי העיצוב (Ui.row /
// Looks.row), הצלמית והסוג (Ui.kind), שורות הכפתורים (TopRow). הבית הוא רשימת הבית שבמסד, ברשת.
// נגיעה: רשימה — נפתחת כאן; רשימה חכמה — במסך החיפוש המוכר (לקריאה, כמו כלי חכם); קובץ — נפתח (פקודה — רצה).
// נגיעה ארוכה — בחירה, ופס הבחירה עם הפעולות (כמו בכל מסכי הרשימות). אחראי: להציג. הכתיבה למסד — דרך DbVerbs (חוץ מהעותק ביציאה).
// מ0.9.1 (צילום המשתמש: "מעבר בין האופנים חסר, והנתונים מתחת לצלמיות"): בבית — "אופן עבודה: X ▾" ושורת האינדקס כמו
// בבית הישן; מתחת לכל רשימה — הספירה (DbCount, באותו נוסח כמו ListCount), מחושבת ברקע; הצלמיות בבית — בלי מסגרת.

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DbListScreen extends ListScreen<Store.Member> {

    static final String EXTRA_MODE = "mode", EXTRA_LIST = "list", EXTRA_SORT = "sort";
    /** מ0.22: מיון מהפקודה ("פתח רשימה … מיין:…"); ריק — הסדר של הרשימה. */
    private String sortBy = "";
    private static final int MENU_DESIGN = 2, MENU_SETTINGS = 3, MENU_CMDS = 5, MENU_PLUGIN_DIAG = 9, MENU_EDITOR = 10, MENU_DESIGN_OLD = 11;

    private String mode;
    private long listId;
    private SqlStore store;
    private TextView modeBox;
    private DbPasteBar pasteBar;
    /** הרשימה המוצגת היא הבית. */
    private boolean atHome;
    /** מ0.9.2: "בית" — כל המסכים שמעל הבית נסגרים אחד אחד, עד הבית (מסך המסד שבדרך — פותח אותו). */
    static boolean goingHome;
    /** כמה מסכים כאלה פתוחים — מסך שנפתח מעליהם ("בית" בו) חוזר לבית של המסד. */
    static int alive;
    /** מ0.20: כמה מסכי מסד מוצגים עכשיו — יורד ל־0: יוצאים מהמסד → עותק אם השתנה משהו. */
    private static int visible;

    long listId() {
        return listId;
    }

    @Override
    protected void onDestroy() {
        alive--;
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
        visible++;
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (--visible <= 0) {
            visible = 0;
            exitCopy(getApplicationContext(), mode);
        }
    }

    /** מ0.20: עותק של המסד ביציאה, ברקע — רק אם השתנה משהו מאז העותק האחרון (Backups.before). */
    static void exitCopy(final android.content.Context c, final String m) {
        if (m == null) return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean sys = Permit.systemOpen(Permit.SYS_EXIT);                // מבנה 124: פעולה של המערכת, רשומה אצל המנהל
                try {
                    Db.write(c, m, null, new Db.Use<Void>() {                     // מבנה 33: דרך שער המסד
                        @Override
                        public Void run(SqlStore s) throws Exception {
                            Db.backupsOf(c, m).before(s, Backups.OP_EXIT, System.currentTimeMillis());
                            return null;
                        }
                    });
                } catch (Throwable t) {
                    Log.e("DbListScreen.exitCopy", t);
                } finally {
                    Permit.systemClose(sys);
                }
            }
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Db.currentMode = mode;
        if (!goingHome) return;
        if (atHome) goingHome = false;
        else finish();
    }
    /** הספירה של כל שורה (מספר זיהוי ← טקסט), מהחישוב ברקע; token — טעינה שכבר לא רלוונטית לא כותבת. */
    private final java.util.Map<Long, String> stats = new java.util.HashMap<Long, String>();
    private int token;

    /** פותח רשימה במראה המוכר. */
    static void open(android.app.Activity a, String mode, long list) {
        open(a, mode, list, null);
    }

    /** מ0.22: עם מיון (שם · גודל · תאריך). */
    static void open(android.app.Activity a, String mode, long list, String sort) {
        a.startActivity(intent(a, mode, list, sort));
    }

    static Intent intent(android.content.Context a, String mode, long list, String sort) {
        return new Intent(a, DbListScreen.class).putExtra(EXTRA_MODE, mode).putExtra(EXTRA_LIST, list)
                .putExtra(EXTRA_SORT, sort == null ? "" : sort);
    }

    /** מ0.25: האם המסך הזה הוא הבית. */
    boolean isHomeScreen() {
        return atHome;
    }

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        alive++;
        Db.app = getApplicationContext();
        Manager.boot();                                                        // מבנה 124: הרישום של פעולות המערכת — לפני כל פעולה כזו
        mode = getIntent().getStringExtra(EXTRA_MODE);
        listId = getIntent().getLongExtra(EXTRA_LIST, 0);
        sortBy = getIntent().getStringExtra(EXTRA_SORT);
        if (sortBy == null) sortBy = "";
        Db.currentMode = mode;                                                   // מ0.41: לפני שהשורות מצוירות — התפריטים של האופן הזה
        atHome = homeNow();                                                   // ...ושל מסך הבית, אם זה הבית
        Menus.ensure(this);                                                   // מ0.41: אופן התפריטים — לפני כל כתיבה אחרת למסדים
        if (Files.hasAccess(this)) DbCmdMenu.ensureToolsAsync(this, mode);      // מ0.23: יצוא וייבוא ב־⭐
        LinearLayout root = build(saved, Words.Db.APP_NAME_M, "db-list");
        modeBox = new TextView(this);                                         // כמו בבית הישן (Home, 105)
        modeBox.setTextSize(16);
        modeBox.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        modeBox.setTextColor(Ui.TEXT);
        modeBox.setBackgroundColor(Ui.BG_SOFT);
        modeBox.setPadding(Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 12), Ui.dp(this, 10));
        modeBox.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { modeMenu(); }
        });
        modeBox.setVisibility(View.GONE);
        root.addView(modeBox, 0);
        pasteBar = new DbPasteBar(this, new DbPasteBar.Place() {              // מ0.24: הלוח
            @Override public String mode() { return mode; }

            @Override public void paste() { pasteHere(); }
        });
        root.addView(pasteBar.view);
        done(root, Actions.Tool.LIST, own());                                // מ0.16: פס הבחירה — כמו ברשימה ידנית
    }

    /** מ0.41: האם הרשימה שנפתחת היא הבית — נקרא כבר בפתיחה (השורות של מסך הבית). */
    private boolean homeNow() {
        SqlStore s = null;
        try {
            s = Db.openRead(this, mode);
            return "1".equals(s.prop(listId, Store.P_HOME));
        } catch (Throwable t) {
            return false;
        } finally {
            if (s != null) s.close();
        }
    }

    /** המסד לקריאה בלבד — נפתח בכל טעינה (מסך המסד הוא שכותב). */
    private boolean openStore() {
        closeStore();
        try {
            store = Db.openRead(this, mode);
            return true;
        } catch (Throwable t) {
            Log.e("DbListScreen.open", t);
            info.setText(Words.Db.openFailed(String.valueOf(t.getMessage())));
            return false;
        }
    }

    private void closeStore() {
        if (store != null) store.close();
        store = null;
    }

    @Override
    protected void onPause() {
        super.onPause();
        closeStore();
    }

    @Override
    protected void refresh() {
        rows.clear();
        if (pasteBar != null) pasteBar.refresh();
        if (!openStore()) {
            adapter.notifyDataSetChanged();
            return;
        }
        // מבנה 60 (פתוח 3 בסיכום; ממצא 32): הטעינה קוראת מהמסד כמה פעמים, ומישהו אחר יכול לכתוב בו באמצע (הרצה ברקע, המעקב
        // אחרי הכונן, שחזור הדוגמאות) — רשימה שנמחקה בין קריאה לקריאה זרקה שגיאה, והתוכנה נסגרה. עכשיו: הודעה במסך, ונרשם ביומן.
        dbTrouble = false;
        try {
            load();
        } catch (RuntimeException gone) {
            Log.e("DbListScreen.refresh", gone);
            rows.clear();
            info.setText(gone instanceof Store.Failure ? Words.Db.LIST_GONE : Words.Db.openFailed(String.valueOf(gone.getMessage())));
            adapter.notifyDataSetChanged();
        }
    }

    private void load() {
        Store.Item it = store.item(listId);
        if (it == null || it.type != Store.LIST) {
            info.setText(Words.Db.LIST_GONE);
            look(null);
            adapter.notifyDataSetChanged();
            return;
        }
        rows.addAll(store.members(listId));
        DbMore.sort(store, rows, sortBy);                                     // מ0.22: מיון מהפקודה
        rows.add(PLUS);                                                       // מ0.10.1: ➕ בסוף, כמו בבית הישן
        boolean home = "1".equals(store.prop(listId, Store.P_HOME));
        atHome = home;
        setTitle(home ? Words.Db.APP_NAME_M : it.name);
        modeBox.setVisibility(home ? View.VISIBLE : View.GONE);
        modeBox.setText(Words.Db.modeButton(mode));
        String why = store.prop(listId, Store.P_WHY);
        String index = !lib.ready ? Words.LOADING_INDEX : lib.status;
        info.setText(home ? index : Words.items(rows.size() - 1) + (why == null || why.isEmpty() ? "" : "\n" + why));
        look(designOf(home));
        adapter.notifyDataSetChanged();
        countLater();
        askTwin();
    }

    /** מבנה 105: על אילו זוגות כבר נשאל במסך הזה ("לעצור" — לא שואלים שוב עד שפותחים את הרשימה מחדש). */
    private final java.util.Set<String> askedTwins = new java.util.HashSet<String>();

    /**
     * מבנה 105: רשימות חכמות כפולות שכבר קיימות — אותה שאלה כמו ביצירה בשם קיים (המשתמש: "כמו כאשר מנסים ליצור רשימה חדשה
     * עם אותו שם"). כל תשובה — דרך המנהל, בפקודות שכבר קיימות: "לדרוס" — עריכה של הראשונה (ההגדרות של השנייה) ואז "הסרה
     * מכאן" של השנייה, כל אחת עם גיבוי לפני; "שם אחר" — עריכה של השנייה (שם חדש; שם קיים — שוב השאלה).
     */
    private void askTwin() {
        final long[] twin = Lists.smartTwin(store, listId);
        if (twin == null || !askedTwins.add(twin[0] + ":" + twin[1])) return;
        final String name = store.item(twin[1]).name, cond0 = prop(twin[0], Store.P_COND), cond1 = prop(twin[1], Store.P_COND);
        final java.util.Map<String, String> props1 = Lists.smartProps(store, twin[1]);
        new android.app.AlertDialog.Builder(this).setTitle(Words.ListCmdW.SMART_TAKEN_TITLE).setMessage(Words.ListCmdW.smartTwin(name, cond0, cond1))
                .setPositiveButton(Words.ListCmdW.SMART_REPLACE, new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int w) {
                        Library lib = Library.get(DbListScreen.this);
                        Manager.Act edit = Manager.onSmartEdit(mode, twin[0], name, cond1, props1, Paths.data(), BatchUi.vars(DbListScreen.this, Paths.data()), lib);
                        String said = edit.error() != null ? edit.error() : edit.say(lib);
                        if (edit.error() != null || edit.failed) {
                            done(said);
                            return;
                        }
                        Manager.Act out = Manager.onListRemove(mode, listId, java.util.Collections.singletonList(twin[1]), Paths.data(),
                                BatchUi.vars(DbListScreen.this, Paths.data()), lib);
                        done(out.error() != null ? out.error() : out.say(lib));
                    }
                })
                .setNegativeButton(Words.ListCmdW.OTHER_NAME, new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int w) {
                        final DbSmartForm.Result r = new DbSmartForm.Result();
                        r.name = name;
                        r.cond = cond1;
                        r.sort = props1.get(Store.P_SORT) == null ? "" : props1.get(Store.P_SORT);
                        r.view = props1.get(Store.P_VIEW) == null ? "" : props1.get(Store.P_VIEW);
                        r.icon = props1.get(Store.P_ICON) == null ? "" : props1.get(Store.P_ICON);
                        r.desc = props1.get(Store.P_DESC) == null ? "" : props1.get(Store.P_DESC);
                        r.why = props1.get(Store.P_WHY) == null ? "" : props1.get(Store.P_WHY);
                        Ui.askText(DbListScreen.this, Words.ListCmdW.OTHER_NAME, name, Words.Short.NAME_HINT, new Ui.Text1() {
                            @Override
                            public void ok(String value) {
                                if (value.trim().isEmpty()) return;
                                r.name = value.trim();
                                saveSmart(store.item(twin[1]), r);
                            }
                        });
                    }
                })
                .setNeutralButton(Words.ListCmdW.SMART_STOP, null).show();
    }

    /** הספירות — ברקע, בחיבור קריאה משלו; כשמוכן — השורות מצוירות שוב. */
    private void countLater() {
        final int mine = ++token;
        final java.io.File db = Db.dbFile(this, mode);
        final java.util.List<Store.Item> items = new java.util.ArrayList<Store.Item>();
        for (Store.Member m : rows) items.add(m.item);
        new Thread(new Runnable() {
            @Override
            public void run() {
                final java.util.Map<Long, String> out = new java.util.HashMap<Long, String>();
                SqlStore s = null;
                try {
                    s = Db.openRead(db);
                    final ListCount.FromLibrary fl = new ListCount.FromLibrary(lib);
                    DbCount.Index ix = new DbCount.Index() {
                        @Override public long[] cond(String c) { return fl.byCond(c); }
                        @Override public java.util.Map<String, Long> files(String c) { return fl.filesByCond(c); }
                    };
                    for (Store.Item it : items) {
                        if (mine != token) return;
                        if (it.type == Store.LIST) {
                            if (!"1".equals(s.prop(it.id, Store.P_GROUP))) out.put(it.id, DbCount.of(s, it.id, ix).text());
                        } else if (it.type == Store.SMART) {
                            long[] v = DbCount.smart(s, it.id, ix);
                            if (v != null) out.put(it.id, Files.count(v[0]) + Words.StatsW.FILES + (v[1] > 0 ? " · " + Files.size(v[1]) : ""));
                        } else if (it.type == Store.FILE) {
                            Store.FileInfo fi = s.file(it.id);
                            if (fi != null && Batch.isBatch(fi.path) && new File(fi.path).isFile())
                                out.put(it.id, Program.stat(DbListScreen.this, new File(fi.path)));
                        }
                    }
                } catch (Throwable t) {
                    Log.e("DbListScreen.count", t);
                } finally {
                    if (s != null) s.close();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mine != token || isFinishing()) return;
                        stats.clear();
                        stats.putAll(out);
                        adapter.notifyDataSetChanged();
                    }
                });
            }
        }).start();
    }

    /**
     * מ0.25 (באג 29; המשתמש: "כפתור חזרה — סוכם שבסופו פותח את המעבר בין האופנים ולא סוגר את התוכנה"): בבית — "חזור"
     * (כפתור המכשיר וגם הפקודה) פותח את בחירת האופן, כמו בבית הישן מ־1.17.6. בחירה פעילה — קודם מסתיימת.
     */
    @Override
    public void onBackPressed() {
        if (atHome && isTaskRoot() && !sel.on()) {
            modeMenu();
            return;
        }
        super.onBackPressed();
    }

    /** מעבר בין האופנים שיש להם מסד (כמו בבית הישן — לפי השם). */
    private void modeMenu() {
        final java.util.List<String> all = Db.dbModes(this);
        java.util.List<String> labels = new java.util.ArrayList<String>();
        for (String m : all) labels.add(Words.Db.modeLine(m, m.equals(mode)));
        final int count = all.size();
        labels.add(Words.Db.MODE_NEW);                                        // מ0.29: אופן חדש, ושחזור הדוגמאות (באג 1)
        labels.add(Words.Db.EXAMPLES_RESTORE);
        Ui.list(this, Words.Db.MODE_PICK, labels, new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(android.content.DialogInterface d, int which) {
                if (which == count) { newMode(); return; }
                if (which == count + 1) { askRestoreExamples(); return; }
                String m = all.get(which);
                if (m.equals(mode)) return;
                Db.saveMode(DbListScreen.this, m);
                SqlStore s = null;
                long h = 0;
                try {
                    s = Db.openRead(DbListScreen.this, m);
                    h = DbPaths.home(s);
                } catch (Throwable t) {
                    Log.e("DbListScreen.mode", t);
                } finally {
                    if (s != null) s.close();
                }
                if (h == 0) h = DbVerbsUi.modeHome(DbListScreen.this, m);      // מבנה 56, 123: יש רשימות ואין בית — "צור אופן" מתקן (דרך המנהל)
                if (h == 0) return;
                finish();
                open(DbListScreen.this, m, h);
            }
        });
    }

    /** מ0.29: אופן עבודה חדש — שם, ואז המסד שלו נפתח (נוצר עם בית ריק; "דוגמאות" — עם הדוגמאות). */
    private void newMode() {
        DbVerbsUi.askModeName(this, new DbVerbsUi.ModeReady() {              // מבנה 48: יש גיבוי בשם הזה — שואלים (DbVerbsUi.createMode)
            @Override
            public void open(String name, long home) {
                Db.saveMode(DbListScreen.this, name);
                finish();
                DbListScreen.open(DbListScreen.this, name, home);
            }
        });
    }

    /** פותח אופן: המסד נוצר אם אין ומקבל בית (DbScreen.prepare), הבית נפתח, והמסך הזה נסגר. */
    private void openMode(String name) {
        long h = DbVerbsUi.modeHome(this, name);                              // מבנה 123: דרך המנהל ("צור אופן")
        if (h == 0) return;
        Db.saveMode(this, name);
        finish();
        open(this, name, h);
    }

    /** מ0.29 (באג 1): שחזור הדוגמאות — באישור; רק רשימות הדוגמאות מוחלפות, ולפני כן גיבוי של המסד. */
    private void askRestoreExamples() {
        Ui.ask(this, Words.Db.EXAMPLES_RESTORE, Words.Db.EXAMPLES_RESTORE_WHAT, Words.NO_TOUCH_FILES, Words.Db.EXAMPLES_RESTORE_BACK,
                Words.Db.EXAMPLES_RESTORE_GO, new Runnable() {
                    @Override
                    public void run() {
                        // מבנה 123: דרך המנהל, כפקודה ("שחזר דוגמאות")
                        final Library lib = Library.get(DbListScreen.this);
                        final Manager.Act act = Manager.onVerbLine(Words.Cmd.N_EXAMPLES, Paths.work(), BatchUi.vars(DbListScreen.this, Paths.work()), lib);
                        if (act.error() != null) {
                            Ui.toast(DbListScreen.this, act.error());
                            return;
                        }
                        final PhysicalUi.Busy busy = new PhysicalUi.Busy(DbListScreen.this, Words.Db.EXAMPLES_RESTORE);
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                final String msg = act.say(lib);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        busy.close();
                                        Ui.toast(DbListScreen.this, msg);
                                        if (isFinishing()) return;
                                        if (mode.equals(DbExamples.MODE)) reload();
                                        else openMode(DbExamples.MODE);
                                    }
                                });
                            }
                        }).start();
                    }
                });
    }

    /** העיצוב: של הרשימה, עם מה שירשה ממה שמעליה (מבנה 234, 236); בבית — של מסך הבית (הכלל — ב־Design). */
    private Design designOf(boolean home) {
        return home ? Design.forHome(store, listId) : Design.forList(store, listId);
    }

    /** מ0.16: בחירה מרובה — קובץ לפי המסלול (כך גם פעולות הקבצים הרגילות: פתיחה, שיתוף, מחיקה מהכונן…). */
    @Override
    protected String key(Store.Member row) {
        if (row == PLUS || row.item.type != Store.FILE || store == null) return null;
        Store.FileInfo fi = store.file(row.item.id);
        return fi == null ? null : fi.path;
    }

    /** מ0.16: רשימה, חכמה, רעיון, כותרת — לפי מספר הזיהוי. */
    @Override
    protected String listKey(Store.Member row) {
        return row == PLUS || row.item.type == Store.FILE ? null : "#" + row.item.id;
    }

    @Override
    protected String nameOf(Store.Member row) {
        return row == PLUS ? Words.Main.ADD : row.item.name;
    }

    @Override
    protected String[] ownLook(Store.Member row) {
        if (row == PLUS) return null;
        String icon = store == null ? null : store.prop(row.item.id, Store.P_ICON);
        return icon == null || icon.isEmpty() ? null : new String[]{icon, ""};
    }

    /** מ0.10.1: השורה האחרונה — "➕ הוספה" (לא פריט במסד). */
    private static final Store.Member PLUS = new Store.Member(new Store.Item(-1, 0, "", 0, 0), -1, 0);

    /** טעינה מחדש אחרי שינוי שנעשה מכאן (רשימה חכמה, פקודה). */
    void reload() {
        refresh();
    }

    @Override
    protected void bind(View v, Store.Member row) {
        try {
            bindRow(v, row);
        } catch (RuntimeException gone) {
            // מבנה 60: גם שגיאה של המסד עצמו (android.database.SQLException) או חיבור שכבר נסגר (IllegalStateException) — אותו טיפול,
            // ונרשמת ביומן התקלות עם המסלול שלה. כל שגיאה אחרת — תקלה בקוד, ולא מוסתרת.
            boolean missing = gone instanceof Store.Failure;
            if (!missing) {
                if (!(gone instanceof android.database.SQLException) && !(gone instanceof IllegalStateException)) throw gone;
                if (!dbTrouble) Log.e("DbListScreen.bind", gone);              // פעם אחת לטעינה — לא לכל שורה
                dbTrouble = true;
            }
            // מבנה 37 (ממצא 32): השורה כבר אינה במסד — מישהו שינה אותו מתחת למסך (שחזור הדוגמאות, הרצה ברקע, המעקב אחרי הכונן).
            // עד כאן זה הקריס את התוכנה באמצע הציור. עכשיו: השורה מוצגת בשמה בלבד, נרשם ביומן, והמסך נטען מחדש — פעם אחת.
            Ui.bindIcon(v, "", row.item == null || row.item.name == null ? "" : row.item.name, "");
            if (missing && !stale) {                                           // תקלה של המסד עצמו — בלי טעינה חוזרת (שלא תחזור בלי סוף)
                stale = true;
                Log.i(Words.Db.ROW_GONE);
                new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        stale = false;
                        if (!isFinishing()) reload();
                    }
                });
            }
        }
    }

    /** שורה שנעלמה כבר זוהתה, והטעינה מחדש בדרך (שלא ייטען פעם לכל שורה). */
    private boolean stale;
    /** מבנה 60: המסד זרק שגיאה בציור שורה — נרשם ביומן פעם אחת, עד הטעינה הבאה. */
    private boolean dbTrouble;

    private void bindRow(View v, Store.Member row) {
        Store.Item it = row.item;
        if (row == PLUS) {
            Ui.bindIcon(v, "➕", Words.Main.ADD, "");
            return;
        }
        if (store == null) return;
        if (it.type == Store.LIST) {
            boolean group = "1".equals(store.prop(it.id, Store.P_GROUP));
            String st = stats.get(it.id);
            Ui.bindIcon(v, "", it.name, group ? Words.Db.KIND_GROUP : st != null ? st : Words.items(store.count(it.id)));
            String icon = store.prop(it.id, Store.P_ICON);
            Ui.kind(v, store.listsOf(it.id).size() > 1 && listId != homeOrZero() ? Ui.LINK : Ui.MANUAL, icon == null ? "" : icon);
        } else if (it.type == Store.SMART) {
            String desc = store.prop(it.id, Store.P_DESC), icon = store.prop(it.id, Store.P_ICON), st = stats.get(it.id);
            Ui.bindIcon(v, "", it.name, st != null ? st : desc == null ? "" : desc);
            Ui.kind(v, Ui.SMART, icon == null ? "" : icon);
        } else if (it.type == Store.FILE) {
            Store.FileInfo fi = store.file(it.id);
            File f = fi == null ? null : new File(fi.path);
            boolean there = f != null && f.exists();
            Ui.bindRow(v, it.name, false, there ? f.length() : fi == null ? -1 : fi.size, there ? f.lastModified() : 0,
                    fi == null ? "" : fi.path, there ? null : Words.FILE_NOT_AT_PATH, null);
            String st = stats.get(it.id);                                     // קובץ פקודות — מה שהוא מסנן (כמו בבית הישן)
            if (st != null && !st.isEmpty()) ((TextView[]) v.getTag())[2].setText(st);
            if (!there) ((TextView[]) v.getTag())[1].setTextColor(Ui.MUTED);
        } else if (it.type == Store.TITLE) {
            Ui.bindIcon(v, "", "── " + it.name + " ──", "");
        } else {
            Ui.bindIcon(v, "💡", it.name, Words.Db.KIND_NOTE);
        }
    }

    /** מבנה 239: "צור כותרת" — כותרת של קבוצה בתוך הרשימה. */
    @Override
    protected boolean groupTitle(Store.Member row) {
        return row != PLUS && row.item.type == Store.TITLE;
    }

    private long homeCache = -1;

    private long homeOrZero() {
        if (homeCache < 0) homeCache = DbPaths.home(store);
        return homeCache;
    }

    @Override
    protected void open(Store.Member row) {
        if (row == PLUS) {
            addMenu();
            return;
        }
        Store.Item it = row.item;
        if (it.type == Store.LIST) {
            open(this, mode, it.id);
        } else if (it.type == Store.SMART) {
            String cond = prop(it.id, Store.P_COND), sort = prop(it.id, Store.P_SORT), view = prop(it.id, Store.P_VIEW);
            if (cond.isEmpty()) {
                Ui.toast(this, Words.Db.SMART_NO_COND);
                return;
            }
            openSmart(this, it.name, cond, sort, view);
        } else if (it.type == Store.FILE) {
            Store.FileInfo fi = store.file(it.id);
            if (fi != null) Ui.open(this, new File(fi.path));
        }
    }

    /** רשימה חכמה של המסד במסך החיפוש המוכר — כמו כלי חכם (FnSmart.open), בלי מזהה כלי (לקריאה). */
    static void openSmart(android.app.Activity a, String name, String cond, String sort, String view) {
        if (cond == null || cond.isEmpty()) {
            Ui.toast(a, Words.Db.SMART_NO_COND);
            return;
        }
        Intent i = new Intent(a, SearchScreen.class);
        i.putExtra(SearchScreen.EXTRA_QUERY, cond);
        i.putExtra(SearchScreen.EXTRA_FIXED, true);
        i.putExtra(SearchScreen.EXTRA_TITLE, name);
        i.putExtra(SearchScreen.EXTRA_SORT, sort == null || sort.isEmpty() ? Sort.NAME : sort);
        i.putExtra(SearchScreen.EXTRA_VIEW, view == null ? "" : view);
        a.startActivity(i);
    }

    private String prop(long id, String k) {
        String v = store == null ? null : store.prop(id, k);                  // מבנה 60: המסד סגור (המסך ברקע) — כמו "אין"
        return v == null ? "" : v;
    }

    /**
     * מבנה 99: שמירת הטופס של רשימה חכמה — דרך המנהל, כפקודה ("עדכן רשימה חכמה %נבחרים% %תוכן%"), עם גיבוי "לפני עריכה" ועם
     * עדכון קובצי הפקודות. מבנה 104: שם חדש שכבר קיים — אותה שאלה כמו ביצירה, בלי "לדרוס" (דריסה כאן הייתה מוחקת רשימה
     * אחרת): "שם אחר" / "לעצור".
     */
    private void saveSmart(final Store.Item it, final DbSmartForm.Result r) {
        Library lib = Library.get(this);
        Manager.Act act = Manager.onSmartEdit(mode, it.id, r.name, r.cond, DbSmartForm.props(r), Paths.data(), BatchUi.vars(this, Paths.data()), lib);
        String why = act.error();
        if (why != null && why.equals(Words.ListCmdW.smartTaken(r.name.trim()))) {
            new android.app.AlertDialog.Builder(this).setTitle(Words.ListCmdW.SMART_TAKEN_TITLE).setMessage(why)
                    .setPositiveButton(Words.ListCmdW.OTHER_NAME, new android.content.DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(android.content.DialogInterface d, int w) {
                            Ui.askText(DbListScreen.this, Words.ListCmdW.OTHER_NAME, r.name, Words.Short.NAME_HINT, new Ui.Text1() {
                                @Override
                                public void ok(String value) {
                                    if (value.trim().isEmpty()) return;
                                    r.name = value.trim();
                                    saveSmart(it, r);
                                }
                            });
                        }
                    })
                    .setNeutralButton(Words.ListCmdW.SMART_STOP, null).show();
            return;
        }
        done(why != null ? why : act.say(lib));
    }

    /** מ0.13.1: עריכה — רשימה חכמה בטופס שלה; רשימה: שם, צלמית, תיאור, "למה"; רעיון/כותרת — הטקסט. דרך הליבה. */
    private void edit(final Store.Item it) {
        if (it.type == Store.FILE) {
            Ui.toast(this, Words.DbCmd.FILE_NO_EDIT);
            return;
        }
        if (it.type == Store.SMART) {
            DbSmartForm.show(this, store, it.id, new DbSmartForm.Done() {
                @Override
                public void ok(final DbSmartForm.Result r) {
                    saveSmart(it, r);
                }
            });
            return;
        }
        final boolean list = it.type == Store.LIST;
        LinearLayout form = Ui.form(this);
        form.addView(Ui.label(this, list ? Words.ToolM.NAME : Words.Db.NOTE_TEXT));
        final android.widget.EditText name = Ui.field(this, it.name, null);
        form.addView(name);
        final android.widget.EditText icon = Ui.field(this, prop(it.id, Store.P_ICON), "📁");
        final android.widget.EditText desc = Ui.field(this, prop(it.id, Store.P_DESC), Words.Manual.DESC_HINT);
        final android.widget.EditText why = Ui.field(this, prop(it.id, Store.P_WHY), Words.WhyW.HINT);
        if (list) {
            form.addView(Ui.label(this, Words.DbCmd.ICON));
            form.addView(icon);
            form.addView(Ui.label(this, Words.Manual.DESC));
            desc.setSingleLine(false);
            form.addView(desc);
            form.addView(Ui.label(this, Words.WhyW.FIELD));
            why.setSingleLine(false);
            form.addView(why);
        }
        Ui.askForm(this, Words.DbCmd.EDIT + ": " + it.name, form, new Runnable() {
            @Override
            public void run() {
                final String n = name.getText().toString(), i = icon.getText().toString(), d = desc.getText().toString(),
                        w = why.getText().toString();
                // לא השתנה דבר בטופס — אין מה לשמור (הזול קודם)
                boolean same = n.equals(it.name) && i.equals(prop(it.id, Store.P_ICON)) && d.equals(prop(it.id, Store.P_DESC))
                        && w.equals(prop(it.id, Store.P_WHY));
                if (same) { done(Words.DbCmd.saved(it.name)); return; }
                // מבנה 113: השמירה — דרך המנהל, כפקודה ("עדכן רשימה %נבחרים%"): אותו כלל כמו בשורה כתובה, עם גיבוי "לפני עריכה"
                // (מבנה 58), עם עדכון קובצי הפקודות שפונים אליה (מ0.42), ועם "הפריט כבר לא קיים" (מבנה 60).
                java.util.Map<String, String> props = new java.util.HashMap<String, String>();
                props.put(Store.P_ICON, i);
                props.put(Store.P_DESC, d);
                props.put(Store.P_WHY, w);
                Library lib = Library.get(DbListScreen.this);
                Manager.Act act = Manager.onListEdit(mode, it.id, n, props, Paths.data(), BatchUi.vars(DbListScreen.this, Paths.data()), lib);
                done(act.error() != null ? act.error() : act.say(lib));
            }
        });
    }

    private void done(String msg) {
        Ui.toast(this, msg);
        sel.stop();
        refresh();
    }

    /** הרשימה שמוצגת עכשיו (בבית — רשימת הבית). */
    private long here() {
        return atHome ? homeOrZero() : listId;
    }

    /** מ0.16: הפעולות של פס הבחירה — כולן דרך הליבה של הפקודות (DbListCmd). */
    private java.util.Map<Actions.Action, Runnable> own() {
        java.util.Map<Actions.Action, Runnable> m = new java.util.HashMap<Actions.Action, Runnable>();
        // מ0.24: ללוח (DbClip) — מנווטים ו"הדבקה כאן"; "קישור" — לא במסך (המשתמש: "קישור — בפקודות")
        m.put(Actions.DUPLICATE_LIST, new Runnable() { @Override public void run() { toClip(DbClip.COPY); } });
        m.put(Actions.MOVE_TO_LIST, new Runnable() { @Override public void run() { toClip(DbClip.MOVE); } });
        m.put(Actions.REMOVE_FROM_LIST, new Runnable() { @Override public void run() { removeChosen(); } });
        m.put(Actions.ORDER_LIST, new Runnable() { @Override public void run() { orderChosen(); } });   // מ0.22
        m.put(Actions.SETTINGS_LIST, new Runnable() {
            @Override public void run() {
                List<Long> ids = chosenIds();
                if (ids.size() == 1 && store.item(ids.get(0)) != null) edit(store.item(ids.get(0)));
            }
        });
        return m;
    }

    /** מה שנבחר — מספרי זיהוי (קבצים לפי המסלול, השאר לפי "#מספר"). */
    private List<Long> chosenIds() {
        List<Long> out = new ArrayList<Long>();
        for (String p : sel.files) {
            long id = store.findFile(p);
            if (id != 0) out.add(id);
        }
        for (String k : sel.lists)
            if (k.startsWith("#")) {
                try { out.add(Long.parseLong(k.substring(1))); } catch (NumberFormatException ignored) { }
            }
        return out;
    }

    /** מ0.24: מה שנבחר — ללוח, עם הפעולה (מחליף, או מוסיף אם "צירוף" פועל). */
    private void toClip(int how) {
        List<Store.Item> what = new ArrayList<Store.Item>();
        for (Long id : chosenIds()) {
            Store.Item it = store.item(id);
            if (it != null) what.add(it);
        }
        if (what.isEmpty()) return;
        boolean add = DbPasteBar.adding();
        DbClip.put(mode, here(), what, how, add);
        sel.stop();
        pasteBar.refresh();
        Ui.toast(this, Words.DbClipW.marked(what.size(), add));
    }

    /** מ0.24: "הדבקה כאן" — מה שבלוח של המסד, וקבצים ותיקיות שהועתקו בכונן; תיקיות — רק הקבצים, או עם המבנה. */
    private void pasteHere() {
        final List<String> files = new ArrayList<String>(), dirs = new ArrayList<String>();
        if (DbClip.diskForLists()) {
            files.addAll(Clip.files());
            dirs.addAll(Clip.dirs());
        }
        if (dirs.isEmpty()) {
            pasteNow(files, dirs, false);
            return;
        }
        Ui.options(this, Words.DbClipW.FOLDERS_HOW, new String[]{Words.DbClipW.ALL_FILES, Words.DbClipW.WITH_TREE},
                new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface d, int which) { pasteNow(files, dirs, which == 1); }
                });
    }

    /**
     * מבנה 116: ההדבקה — דרך המנהל, בפקודות: מהלוח של הרשימות — "העתק ידנית %נבחרים% ידנית %נבחרים%" למה שסומן להעתקה, ו"העבר…"
     * למה שסומן להעברה (שורה לכל רשימה שממנה סומן; גיבוי לפני); מהכונן — "העתק %נבחרים% ידנית %נבחרים%" (מבנה 114–115). כאן
     * נשארים הלוח וההודעה. ברקע (תיקיות נסרקות).
     */
    private void pasteNow(final List<String> files, final List<String> dirs, final boolean structure) {
        final long dest = here();
        final Library lib = Library.get(this);
        final java.util.Map<String, String> vars = BatchUi.vars(this, Paths.data());
        final List<Manager.Act> acts = new ArrayList<Manager.Act>();
        final List<Long> movedFrom = new ArrayList<Long>();                     // לכל פעולה: הרשימה שממנה מעבירים; -1 — לא העברה
        if (!DbClip.empty() && DbClip.cannot(mode) == null) {
            List<Long> copy = new ArrayList<Long>();
            java.util.Map<Long, List<Long>> move = new java.util.LinkedHashMap<Long, List<Long>>();
            for (DbClip.Item it : DbClip.items()) {
                if (it.how != DbClip.MOVE) { copy.add(it.id); continue; }
                if (!move.containsKey(it.from)) move.put(it.from, new ArrayList<Long>());
                move.get(it.from).add(it.id);
            }
            if (!copy.isEmpty()) {
                acts.add(Manager.onListPaste(mode, false, 0, copy, dest, Paths.data(), vars, lib));
                movedFrom.add(-1L);
            }
            for (java.util.Map.Entry<Long, List<Long>> e : move.entrySet()) {
                acts.add(Manager.onListPaste(mode, true, e.getKey(), e.getValue(), dest, Paths.data(), vars, lib));
                movedFrom.add(e.getKey());
            }
        }
        if (!files.isEmpty() || !dirs.isEmpty()) {
            acts.add(Manager.onListAdd(mode, dest, files, dirs, structure ? Manager.FOLDER_TREE : Manager.FOLDER_FLAT, Paths.data(), vars, lib));
            movedFrom.add(-1L);
        }
        for (Manager.Act act : acts)
            if (act.error() != null) {
                done(act.error());
                return;
            }
        if (acts.isEmpty()) return;
        if (!dirs.isEmpty()) Ui.toast(this, Words.DbClipW.ADDING);
        new Thread(new Runnable() {
            @Override
            public void run() {
                final StringBuilder said = new StringBuilder();
                for (int i = 0; i < acts.size(); i++) {
                    said.append(said.length() > 0 ? "\n" : "").append(acts.get(i).say(lib));
                    if (movedFrom.get(i) >= 0) DbClip.moved(movedFrom.get(i), acts.get(i).entered);   // "העבר" שנכנס — יוצא מהלוח
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isFinishing()) return;
                        done(said.toString());
                    }
                });
            }
        }).start();
    }

    /** מ0.22: סידור ידני — למעלה · למטה · לראש · לסוף (דרך הליבה); הבחירה נשארת, כדי להזיז שוב. */
    private void orderChosen() {
        final List<Long> ids = chosenIds();
        if (ids.isEmpty()) return;
        final long in = here();
        String[] how = {Words.DbCmd.ORDER_UP, Words.DbCmd.ORDER_DOWN, Words.DbCmd.ORDER_TOP, Words.DbCmd.ORDER_BOTTOM};
        Ui.options(this, Words.DbCmd.ORDER_TITLE, how, new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(android.content.DialogInterface d, final int which) {
                // מבנה 121: דרך המנהל, כפקודה ("עדכן רשימה %נבחרים% /מקום:…")
                Library lib = Library.get(DbListScreen.this);
                Manager.Act act = Manager.onListOrder(mode, in, ids, which, Paths.data(), BatchUi.vars(DbListScreen.this, Paths.data()), lib);
                String said = act.error() != null ? act.error() : act.say(lib);
                if (act.error() != null || act.failed) Ui.toast(DbListScreen.this, said);
                sortBy = "";
                refresh();
            }
        });
    }

    /** הסרה מכאן — של כל מה שנבחר (אישור; גיבוי לפני). */
    private void removeChosen() {
        final List<Long> ids = chosenIds();
        if (ids.isEmpty()) return;
        final long from = here();
        // מבנה 85: "הסרה מכאן" — דרך הפקודה "בטל ידנית %נבחרים%" (המנהל); כאן נשארים חלון האישור וההודעה.
        final Library lib = Library.get(this);
        final Manager.Act act = Manager.onListRemove(mode, from, ids, Paths.data(), BatchUi.vars(this, Paths.data()), lib);
        if (act.error() != null) {
            Ui.toast(this, act.error());
            return;
        }
        Ui.confirm(this, Words.DbCmd.REMOVE_HERE, Words.DbCmd.removeQuestionN(ids.size()), Words.DbCmd.REMOVE_YES, new Runnable() {
            @Override
            public void run() {
                done(act.say(lib));
            }
        });
    }

    /** ➕: בבית — כמו בבית הישן (רשימה ידנית, רשימה חכמה, פקודה); ברשימה — גם קבצים מתיקייה ומחיפוש. */
    private void addMenu() {
        final boolean home = atHome;
        // מ0.25 (באג 27; המשתמש: "הכפולים לא נכנס עדיין בגרסה החדשה… למה לשנות ממה שהיה הקודם?"): כפולים — שוב ב־➕, באותו טופס
        // מ0.28 (המשתמש: "כפולים אמור להיות באותו תפריט של רשימה חכמה בתור עוד אופציה"): לא שורה נפרדת — בטופס הרשימה החכמה
        String[] items = home ? new String[]{Words.Main.ADD_MANUAL, Words.Main.ADD_SMART, Words.Main.ADD_CMD}
                : new String[]{Words.Main.ADD_MANUAL, Words.Main.ADD_SMART, Words.Main.ADD_CMD, Words.Db.ADD_FROM_FOLDER, Words.Db.ADD_FROM_SEARCH};
        Ui.options(this, Words.Main.WHAT_TO_ADD, items, new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(android.content.DialogInterface d, int which) {
                if (which == 0) DbVerbsUi.newList(DbListScreen.this, mode, listId);
                else if (which == 1) DbVerbsUi.newSmart(DbListScreen.this, mode, listId);
                else if (which == 2) DbVerbsUi.addCommand(DbListScreen.this, mode, listId);
                else DbVerbsUi.addFiles(DbListScreen.this, mode, listId, which == 3 ? DbScreen.DO_FOLDER : DbScreen.DO_SEARCH);
            }
        });
    }


    /** מ0.11.1: התפריט ⋮ — פקודות, הגדרות, עיצוב, ותוסף האבחון. */
    @Override
    protected void menu(Menu menu) {
        menu.add(0, MENU_CMDS, 0, Words.BATCH_MENU);
        menu.add(0, MENU_SETTINGS, 0, Words.Main.SETTINGS);
        menu.add(0, MENU_DESIGN, 0, Words.DesignW.MENU);
        menu.add(0, MENU_DESIGN_OLD, 0, Words.DesignW.MENU_OLD);                  // מבנה 245: עד שהלוח יעבוד במכשיר
        // מ0.35 (המשתמש, 11.11.2026, על הגיבויים: "בינתיים שתעבור להגדרות. היא לא צריכה להיות במקום כזה מרכזי") — הכניסה בהגדרות
        // מ0.37: "⚙ מסד" ירדה מהתפריט — במסך המסד נשארו רק הגיבויים, והכניסה אליהם מההגדרות
        // מ0.42: תוסף האבחון — הבדיקות, הדוחות והתקלות (claude/tosaf-bdikot.md). מ0.43: במקום "בדיקת הפקודות" ו"דוח אבחון"
        // מבנה 203 (המשתמש, 16.11.2026: "הרצת העורך אמורה להיות מובנת בתוכנה ולא על ידי פקודות פרוביזוריות"): כניסה קבועה לעורך —
        // ישר, בלי קובץ פקודות באמצע. הכפתורים שבשורות ("עורך הפקודות", והכלי "עורך אינטראקטיבי") — נשארו; יש שם שניים לאותו דבר (ממצא 56).
        menu.add(0, MENU_EDITOR, 0, Words.DbCmd.MENU_EDITOR);
        menu.add(0, MENU_PLUGIN_DIAG, 0, Words.DbCmd.MENU_PLUGIN_DIAG);
    }

    @Override
    protected boolean item(int id) {
        if (id == MENU_CMDS) CmdMenu.show(this, null);
        else if (id == MENU_SETTINGS) startActivity(new Intent(this, SettingsScreen.class));
        else if (id == MENU_DESIGN) panel();
        else if (id == MENU_DESIGN_OLD) design();
        else if (id == MENU_EDITOR) LabScreen.edit(this, null);
        else if (id == MENU_PLUGIN_DIAG) LabScreen.open(this, Words.DbCmd.TOOL_DIAG);
        else return false;
        return true;
    }

    /**
     * מבנה 245: "🎨 עיצוב" — לוח העיצוב (התוסף), על הרשימה שעל המסך; בבית — על האופן (מה שמעל כל הרשימות שלו). הדרך — כמו
     * שהפקודה מוצאת אותה (DbPaths.pathOf); רשימה שהדרך שלה מובילה לאחרת (כמה באותה דרך) — בינתיים החלון הישן.
     */
    private void panel() {
        if (atHome || store == null) {
            LabScreen.design(this, Verbs.MODE_WORD, mode);
            return;
        }
        java.util.List<String> path = DbPaths.pathOf(store, listId);
        if (path.isEmpty() || DbPaths.find(store, mode, path) != listId) {
            Ui.toast(this, Words.DesignW.PANEL_NO_PATH);
            design();
            return;
        }
        path.add(0, mode);                                                       // מבנה 246: הדרך מתחילה באופן
        LabScreen.design(this, Verbs.LIST, Names.pathText(path));
    }

    /** עיצוב (החלון הישן): בבית — של מסך הבית (כמו בבית הישן); ברשימה — שלה (נשמר במסד). */
    private void design() {
        if (atHome) {
            String cur = Design.homeName();
            DesignUi.choose(this, Words.DesignW.TITLE_HOME, cur.isEmpty() ? Design.FILES : cur, null, false, new DesignUi.Done() {
                @Override public void chosen(String name) { Design.setHome(name); refresh(); }

                @Override public void changed() { refresh(); }
            });
            return;
        }
        String cur = store == null ? null : store.prop(listId, Store.P_DESIGN);
        DesignUi.choose(this, Words.DesignW.TITLE_LIST, cur == null || cur.isEmpty() ? Design.DEFAULT : cur,
                cur == null || cur.isEmpty() ? "" : null, true, new DesignUi.Done() {
            @Override
            public void chosen(final String name) {
                // מבנה 121, 233: דרך המנהל, כפקודה ("עצב %נבחרים% /עיצוב:…", מבנה 246); "" — לרשת (המאפיין נמחק)
                Library lib = Library.get(DbListScreen.this);
                Manager.Act act = Manager.onListDesign(mode, listId, name, Paths.data(), BatchUi.vars(DbListScreen.this, Paths.data()), lib);
                String said = act.error() != null ? act.error() : act.say(lib);
                if (act.error() != null || act.failed) Ui.toast(DbListScreen.this, said);
                refresh();
            }

            @Override public void changed() { refresh(); }
        });
    }
}
