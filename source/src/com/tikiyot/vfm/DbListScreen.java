package com.tikiyot.vfm;

// מ0.9: רשימה מהמסד במראה המוכר — אותו שלד כמו כל מסכי הרשימות (ListScreen): השורות או הרשת לפי העיצוב (Ui.row /
// Looks.row), הצלמית והסוג (Ui.kind), שורות הכפתורים (TopRow). הבית הוא רשימת הבית שבמסד, ברשת.
// נגיעה: רשימה — נפתחת כאן; רשימה חכמה — במסך החיפוש המוכר (לקריאה, כמו כלי חכם); קובץ — נפתח (פקודה — רצה).
// נגיעה ארוכה — מסך המסד באותה רשימה (שם כל הפעולות). אחראי: להציג. אסור לו: לכתוב למסד (נפתח לקריאה בלבד).
// מ0.9.1 (צילום המשתמש: "מעבר בין האופנים חסר, והנתונים מתחת לצלמיות"): בבית — "אופן עבודה: X ▾" ושורת האינדקס כמו
// בבית הישן; מתחת לכל רשימה — הספירה (DbCount, באותו נוסח כמו ListCount), מחושבת ברקע; הצלמיות בבית — בלי מסגרת.

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.List;

public class DbListScreen extends ListScreen<Store.Member> {

    static final String EXTRA_MODE = "mode", EXTRA_LIST = "list";
    private static final int MENU_DB = 1, MENU_DESIGN = 2, MENU_SETTINGS = 3, MENU_OLD = 4, MENU_CMDS = 5;

    private String mode;
    private long listId;
    private SqlStore store;
    private TextView modeBox;
    /** הרשימה המוצגת היא הבית. */
    private boolean atHome;
    /** מ0.9.2: "בית" — כל המסכים שמעל הבית נסגרים אחד אחד, עד הבית (מסך המסד שבדרך — פותח אותו). */
    static boolean goingHome;
    /** כמה מסכים כאלה פתוחים — מסך שנפתח מעליהם ("בית" בו) חוזר לבית של המסד. */
    static int alive;
    /** מ0.10: האופן של המסד שעובדים בו עכשיו (המסך האחרון שהוצג) — לפקודות (DbVerbs). */
    static String currentMode;

    long listId() {
        return listId;
    }

    @Override
    protected void onDestroy() {
        alive--;
        super.onDestroy();
    }

    static void goHome(DbListScreen a) {
        if (a.atHome) return;
        goingHome = true;
        a.finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        currentMode = mode;
        Home.inOld = false;                                                   // מ0.11.1: חזרנו לצד של המסד
        if (!goingHome) return;
        if (atHome) goingHome = false;
        else finish();
    }
    /** הספירה של כל שורה (מספר זיהוי ← טקסט), מהחישוב ברקע; token — טעינה שכבר לא רלוונטית לא כותבת. */
    private final java.util.Map<Long, String> stats = new java.util.HashMap<Long, String>();
    private int token;

    /** פותח רשימה במראה המוכר. */
    static void open(android.app.Activity a, String mode, long list) {
        a.startActivity(new Intent(a, DbListScreen.class).putExtra(EXTRA_MODE, mode).putExtra(EXTRA_LIST, list));
    }

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        alive++;
        mode = getIntent().getStringExtra(EXTRA_MODE);
        listId = getIntent().getLongExtra(EXTRA_LIST, 0);
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
        done(root, Actions.Tool.SEARCH, null);                               // הפס — לא נפתח (אין בחירה כאן); השורות — כן
    }

    /** המסד לקריאה בלבד — נפתח בכל טעינה (מסך המסד הוא שכותב). */
    private boolean openStore() {
        closeStore();
        try {
            store = SqlStore.openReadOnly(DbScreen.dbFile(this, mode));
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
        if (!openStore()) {
            adapter.notifyDataSetChanged();
            return;
        }
        Store.Item it = store.item(listId);
        if (it == null || it.type != Store.LIST) {
            info.setText(Words.Db.LIST_GONE);
            look(null);
            adapter.notifyDataSetChanged();
            return;
        }
        rows.addAll(store.members(listId));
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
    }

    /** הספירות — ברקע, בחיבור קריאה משלו; כשמוכן — השורות מצוירות שוב. */
    private void countLater() {
        final int mine = ++token;
        final java.io.File db = DbScreen.dbFile(this, mode);
        final java.util.List<Store.Item> items = new java.util.ArrayList<Store.Item>();
        for (Store.Member m : rows) items.add(m.item);
        new Thread(new Runnable() {
            @Override
            public void run() {
                final java.util.Map<Long, String> out = new java.util.HashMap<Long, String>();
                SqlStore s = null;
                try {
                    s = SqlStore.openReadOnly(db);
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

    /** מעבר בין האופנים שיש להם מסד (כמו בבית הישן — לפי השם). */
    private void modeMenu() {
        final java.util.List<String> all = DbScreen.dbModes(this);
        java.util.List<String> labels = new java.util.ArrayList<String>();
        for (String m : all) labels.add(Words.Db.modeLine(m, m.equals(mode)));
        Ui.list(this, Words.Db.MODE_PICK, labels, new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(android.content.DialogInterface d, int which) {
                String m = all.get(which);
                if (m.equals(mode)) return;
                DbScreen.saveMode(DbListScreen.this, m);
                SqlStore s = null;
                long h = 0;
                try {
                    s = SqlStore.openReadOnly(DbScreen.dbFile(DbListScreen.this, m));
                    h = Importer.home(s);
                } catch (Throwable t) {
                    Log.e("DbListScreen.mode", t);
                } finally {
                    if (s != null) s.close();
                }
                if (h == 0) {
                    Ui.toast(DbListScreen.this, Words.Db.NO_HOME);
                    return;
                }
                finish();
                open(DbListScreen.this, m, h);
            }
        });
    }

    /** העיצוב: של הרשימה; בבית — של מסך הבית, ואם לא נבחר — רשת (כמו האריחים בבית). */
    private Design designOf(boolean home) {
        String name = store.prop(listId, Store.P_DESIGN);
        if (name != null && !name.isEmpty()) return Design.get(name);
        if (!home) return null;
        String h = Design.homeName();
        if (!h.isEmpty()) return Design.get(h);
        Design d = Design.get(Design.FILES);                                  // כמו האריחים בבית הישן: בלי מסגרת לצלמית
        d.around = Design.NONE;
        return d;
    }

    @Override
    protected String key(Store.Member row) {
        return null;                                                          // בלי בחירה כאן
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
            Ui.bindRow(this, v, it.name, false, there ? f.length() : fi == null ? -1 : fi.size, there ? f.lastModified() : 0,
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

    private long homeCache = -1;

    private long homeOrZero() {
        if (homeCache < 0) homeCache = Importer.home(store);
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
        String v = store.prop(id, k);
        return v == null ? "" : v;
    }

    @Override
    protected void hold(Store.Member row) {
        if (row != PLUS) actions();
    }

    /** ➕: בבית — כמו בבית הישן (רשימה ידנית, רשימה חכמה, פקודה); ברשימה — גם קבצים מתיקייה ומחיפוש. */
    private void addMenu() {
        final boolean home = atHome;
        String[] items = home ? new String[]{Words.Main.ADD_MANUAL, Words.Main.ADD_SMART, Words.Main.ADD_CMD}
                : new String[]{Words.Main.ADD_MANUAL, Words.Main.ADD_SMART, Words.Main.ADD_CMD, Words.Db.ADD_FROM_FOLDER, Words.Db.ADD_FROM_SEARCH};
        Ui.options(this, Words.Main.WHAT_TO_ADD, items, new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(android.content.DialogInterface d, int which) {
                if (which == 0) DbVerbs.newList(DbListScreen.this, mode, listId);
                else if (which == 1) DbVerbs.newSmart(DbListScreen.this, mode, listId);
                else if (which == 2) DbVerbs.addCommand(DbListScreen.this, mode, listId);
                else DbVerbs.addFiles(DbListScreen.this, mode, listId, which == 3 ? DbScreen.DO_FOLDER : DbScreen.DO_SEARCH);
            }
        });
    }

    /** כל הפעולות — במסך המסד, באותה רשימה. */
    private void actions() {
        startActivity(new Intent(this, DbScreen.class).putExtra(DbScreen.EXTRA_MODE, mode).putExtra(DbScreen.EXTRA_AT, listId));
    }

    /** מ0.11.1: התפריט — כמו בבית הישן (פקודות, הגדרות, עיצוב), ועוד: מסך המסד, והצד הישן. */
    @Override
    protected void menu(Menu menu) {
        menu.add(0, MENU_CMDS, 0, Words.BATCH_MENU);
        menu.add(0, MENU_SETTINGS, 0, Words.Main.SETTINGS);
        menu.add(0, MENU_DESIGN, 0, Words.DesignW.MENU);
        menu.add(0, MENU_DB, 0, Words.Db.MENU_ACTIONS);
        menu.add(0, MENU_OLD, 0, Words.Db.MENU_OLD);
    }

    @Override
    protected boolean item(int id) {
        if (id == MENU_DB) actions();
        else if (id == MENU_CMDS) CmdMenu.show(this, null);
        else if (id == MENU_SETTINGS) startActivity(new Intent(this, SettingsScreen.class));
        else if (id == MENU_OLD) Home.openOld(this);
        else if (id == MENU_DESIGN) design();
        else return false;
        return true;
    }

    /** עיצוב: בבית — של מסך הבית (כמו בבית הישן); ברשימה — שלה (נשמר במסד). */
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
                DbVerbs.write(DbListScreen.this, mode, new DbVerbs.Job() {
                    @Override
                    public long run(Store s, java.util.List<String> p) {
                        s.setProp(listId, Store.P_DESIGN, name);              // "" — לרשת (המאפיין נמחק)
                        return listId;
                    }
                }, null);
                refresh();
            }

            @Override public void changed() { refresh(); }
        });
    }

    @Override
    protected void all(java.util.Set<String> files, java.util.Set<String> lists) { }

    List<Store.Member> shown() {
        return rows;
    }
}
