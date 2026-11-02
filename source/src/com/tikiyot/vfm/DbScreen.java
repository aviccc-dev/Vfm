package com.tikiyot.vfm;

// מסך מנוע המסד (מ0.1–מ0.5): רשימות, רשימות חכמות ("לא משויכים" כרשימה מיוחדת) ופריטים מעל Store, העתקה מהמנוע הישן,
// וגיבוי (מ0.5): עותק יומי ברקע כשנפתח המסד, עותק לפני פעולה מוחקת, "גיבוי עכשיו", שחזור, ומסד פגום שלא נפתח.
// אחראי: להציג ולהפעיל. אסור לו: לדעת איך הנתונים נשמרים (Store, Backups), או לכתוב למנוע הישן (קורא בלבד).

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DbScreen extends Activity {

    /** מסד לכל אופן עבודה, באחסון הפרטי של התוכנה (לא בתיקייה הגלויה): vfm-m-<אופן>.db. */
    static final String DB_PREFIX = "vfm-m-", DB_SUFFIX = ".db";
    private static final String PREFS = "m", PREF_MODE = "mode";

    private volatile SqlStore store;
    /** מ0.5: המסד של האופן פגום — לא נפתח. null: לא פגום. */
    private String damaged;
    /** סגירת המסד מחכה לגיבוי היומי שרץ ברקע (שלא ייסגר באמצע). */
    private final Object storeLock = new Object();
    private String mode = "";
    private Button modeButton;
    private TextView status, crumb, hint;
    private LinearLayout rows, buttons;
    private ScrollView sv;
    /** נתיב הרשימות הפתוחות; ריק = "כל הרשימות". UNASSIGNED_ID = "לא משויכים" (רשימה מחושבת, לא פריט). */
    private final ArrayList<Long> path = new ArrayList<Long>();
    private static final long UNASSIGNED_ID = -1L, TRASH_ID = -2L;
    /** מספר ההצגה הנוכחית; הערכה של רשימה חכמה שהסתיימה אחרי שעזבו אותה — מתעלמים ממנה. */
    private int token;
    private final android.os.Handler ui = new android.os.Handler(android.os.Looper.getMainLooper());
    private static final int SMART_ROWS = 300;

    static File dbFile(android.content.Context c, String mode) {
        return c.getDatabasePath(DB_PREFIX + mode + DB_SUFFIX);
    }

    /** האופנים שיש להם מסד כבר. */
    private List<String> dbModes() {
        List<String> out = new ArrayList<String>();
        File[] k = dbFile(this, "x").getParentFile().listFiles();
        if (k != null)
            for (File f : k) {
                String n = f.getName();
                if (n.startsWith(DB_PREFIX) && n.endsWith(DB_SUFFIX) && n.length() > DB_PREFIX.length() + DB_SUFFIX.length())
                    out.add(n.substring(DB_PREFIX.length(), n.length() - DB_SUFFIX.length()));
            }
        java.util.Collections.sort(out);
        return out;
    }

    /** האופן הפתוח: מה שנבחר בפעם הקודמת, ואם אין — האופן הפעיל במנוע הישן. */
    private String loadMode() {
        String m = getSharedPreferences(PREFS, MODE_PRIVATE).getString(PREF_MODE, "");
        return m.isEmpty() ? Importer.activeMode(oldData()) : m;
    }

    private void switchMode(String m) {
        mode = m;
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(PREF_MODE, m).apply();
        closeStore();
        path.clear();
        refresh();
    }

    private void closeStore() {
        synchronized (storeLock) {
            if (store != null) store.close();
            store = null;
        }
    }

    private void pickMode() {
        java.util.TreeSet<String> all = new java.util.TreeSet<String>(dbModes());
        all.addAll(Importer.modes(oldData()));
        all.add(mode);
        final List<String> names = new ArrayList<String>(all);
        List<String> labels = new ArrayList<String>();
        for (String n : names) labels.add(Words.Db.modeLine(n, n.equals(mode)));
        Ui.list(this, Words.Db.MODE_PICK, labels, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int which) {
                if (!names.get(which).equals(mode)) switchMode(names.get(which));
            }
        });
    }

    /** קובץ המונה — בתיקיית הנתונים הגלויה, כדי ששחזור המסד מגיבוי ישן לא יחזיר מספר זיהוי. */
    static File counterFile() {
        return new File(new File(Paths.data(), Paths.SYSTEM), "מונה-מזהים.txt");  // בכונן
    }

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        setTitle(Words.Db.TITLE);
        LinearLayout root = Ui.screen(this);
        TextView notice = Ui.info(this);
        notice.setText(Words.Db.NOTICE);
        root.addView(notice);
        status = Ui.info(this);
        root.addView(status);
        modeButton = new Button(this);
        modeButton.setAllCaps(false);
        modeButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { pickMode(); }
        });
        root.addView(modeButton);
        crumb = new TextView(this);
        crumb.setTextSize(16);
        crumb.setTypeface(Typeface.DEFAULT_BOLD);
        crumb.setTextColor(Ui.TEXT);
        crumb.setBackgroundColor(Ui.BG_SOFT);
        crumb.setPadding(Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 12), Ui.dp(this, 10));
        root.addView(crumb);
        buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(buttons);
        hint = Ui.info(this);
        hint.setText(Words.Db.HINT);
        root.addView(hint);
        rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        sv = new ScrollView(this);
        sv.addView(rows);
        root.addView(sv, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    /** פותח את מסד האופן הנבחר (אם עוד לא פתוח) ומציג. */
    private void refresh() {
        if (!Ui.hasFiles(this)) {
            status.setText(Words.Db.NEED_ACCESS);
            rows.removeAllViews();
            buttons.removeAllViews();
            buttons.addView(button(Words.Main.GRANT, new Runnable() {
                @Override public void run() { Ui.requestFiles(DbScreen.this); }
            }));
            return;
        }
        if (mode.isEmpty()) mode = loadMode();
        modeButton.setText(Words.Db.modeButton(mode));
        if (store == null) {
            damaged = null;
            try {
                store = SqlStore.open(dbFile(this, mode), new IdCounter(counterFile()));
            } catch (SqlStore.Damaged d) {
                Log.e("DbScreen.open", d);
                damaged = String.valueOf(d.getMessage());
                showDamaged();
                return;
            } catch (Throwable t) {
                Log.e("DbScreen.open", t);
                status.setText(Words.Db.openFailed(String.valueOf(t.getMessage())));
                return;
            }
            startDaily();
        }
        show();
    }

    @Override
    protected void onDestroy() {
        closeStore();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (path.isEmpty() || store == null) {
            super.onBackPressed();
            return;
        }
        path.remove(path.size() - 1);
        show();
    }

    // ---------- תצוגה ----------

    private long current() {
        return path.isEmpty() ? 0 : path.get(path.size() - 1);
    }

    private Button button(String text, final Runnable r) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(13);
        b.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));   // כמה כפתורים — חולקים את הרוחב
        b.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { r.run(); }
        });
        return b;
    }

    private void show() {
        token++;
        try {
            List<Store.Item> all = store.lists();
            status.setText(Words.Db.status(all.size(), store.lastId(), store.systemCode(), store.info()));
            StringBuilder c = new StringBuilder(Words.Db.ALL_LISTS);
            for (Long id : new ArrayList<Long>(path)) {
                if (id == UNASSIGNED_ID || id == TRASH_ID) {
                    c.append(Words.Db.CRUMB_SEP).append(id == TRASH_ID ? Words.Db.TRASH : Words.Db.UNASSIGNED);
                    continue;
                }
                Store.Item it = store.item(id);
                if (it == null) {
                    path.remove(id);
                    continue;
                }
                c.append(Words.Db.CRUMB_SEP).append(it.type == Store.SMART ? "⚡ " + it.name : label(it));
            }
            crumb.setText(c.toString());
            final long cur = current();
            Store.Item curItem = cur > 0 ? store.item(cur) : null;
            final boolean inSmart = curItem != null && curItem.type == Store.SMART;
            buttons.removeAllViews();
            if (path.isEmpty()) {
                buttons.addView(button(Words.Db.NEW_LIST, new Runnable() {
                    @Override public void run() { newList(0); }
                }));
                buttons.addView(button(Words.Db.NEW_SMART, new Runnable() {
                    @Override public void run() { newSmart(0); }
                }));
                buttons.addView(button(Words.Db.IMPORT, new Runnable() {
                    @Override public void run() { importStart(); }
                }));
            } else if (inSmart) {
                final Store.Item sm = curItem;
                buttons.addView(button(Words.Db.EDIT_COND, new Runnable() {
                    @Override public void run() { act(sm, -1, 5); }
                }));
                buttons.addView(button(Words.Db.RENAME, new Runnable() {
                    @Override public void run() { act(sm, -1, 0); }
                }));
                buttons.addView(button(Words.Db.DELETE_YES, new Runnable() {
                    @Override public void run() { act(sm, -1, 4); }
                }));
            } else if (cur > 0) {
                buttons.addView(button(Words.Db.ADD_MENU, new Runnable() {
                    @Override public void run() { addMenu(); }
                }));
            }
            buttons.addView(button(Words.Db.FULL_CHECK, new Runnable() {
                @Override public void run() { fullCheck(); }
            }));
            hint.setText(cur == UNASSIGNED_ID ? Words.Db.UNASSIGNED_HINT : cur == TRASH_ID ? Words.Db.TRASH_HINT : Words.Db.HINT);
            rows.removeAllViews();
            if (path.isEmpty()) {
                rows.addView(specialRow(Words.Db.UNASSIGNED, Words.Db.UNASSIGNED_SUB, store.unassigned().size(), UNASSIGNED_ID));
                rows.addView(specialRow(Words.Db.TRASH, Words.Db.TRASH_SUB, store.trashed().size(), TRASH_ID));
                rows.addView(backupsRow());
                List<Store.Item> smarts = store.smarts();
                if (all.isEmpty() && smarts.isEmpty()) {
                    TextView t = Ui.info(this);
                    t.setText(Words.Db.EMPTY);
                    rows.addView(t);
                }
                for (Store.Item it : all) rows.addView(row(it, store.count(it.id), -1, 0));
                for (Store.Item it : smarts) rows.addView(row(it, -1, -1, 0));
            } else if (cur == UNASSIGNED_ID) {
                List<Store.Item> un = store.unassigned();
                if (un.isEmpty()) {
                    TextView t = Ui.info(this);
                    t.setText(Words.Db.UNASSIGNED_EMPTY);
                    rows.addView(t);
                }
                for (Store.Item it : un) rows.addView(row(it, -1, -1, 0));
            } else if (cur == TRASH_ID) {
                List<Store.Item> tr = store.trashed();
                if (tr.isEmpty()) {
                    TextView t = Ui.info(this);
                    t.setText(Words.Db.TRASH_EMPTY);
                    rows.addView(t);
                }
                for (Store.Item it : tr) rows.addView(row(it, -1, -1, 0));
            } else if (inSmart) {
                showSmart(curItem);
            } else {
                List<Store.Member> ms = store.members(current());
                if (ms.isEmpty()) {
                    TextView t = Ui.info(this);
                    t.setText(Words.Db.EMPTY_LIST);
                    rows.addView(t);
                }
                for (Store.Member m : ms)
                    rows.addView(row(m.item, m.item.type == Store.LIST ? store.count(m.item.id) : -1, m.position, ms.size()));
            }
        } catch (Throwable t) {
            Log.e("DbScreen.show", t);
            status.setText(Words.Db.failed(String.valueOf(t.getMessage())));
        }
    }

    /** "לא משויכים" / סל המחזור: שורה מיוחדת בראש הרשימות — מחושבת בכל פעם, לא נשמרת. */
    private View specialRow(String title, String subText, int count, final long id) {
        return specialRow(title, subText + " · " + Words.items(count), new Runnable() {
            @Override
            public void run() {
                path.add(id);
                show();
            }
        });
    }

    private View specialRow(String title, String subLine, final Runnable tap) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.VERTICAL);
        r.setPadding(Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 12), Ui.dp(this, 10));
        TextView name = new TextView(this);
        name.setText(title);
        name.setTextSize(16);
        name.setTextColor(Ui.TEXT);
        TextView sub = new TextView(this);
        sub.setTextSize(12);
        sub.setTextColor(Ui.SUB);
        sub.setText(subLine);
        r.addView(name);
        r.addView(sub);
        r.setBackgroundColor(Ui.BG);
        r.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tap.run();
            }
        });
        return r;
    }

    /** רשימה חכמה פתוחה: התוצאות מחושבות עכשיו מהאינדקס (ברקע), לקריאה בלבד. */
    private void showSmart(final Store.Item sm) {
        final String cond = store.prop(sm.id, Store.P_COND) == null ? "" : store.prop(sm.id, Store.P_COND);
        final String sort = store.prop(sm.id, Store.P_SORT) == null ? "" : store.prop(sm.id, Store.P_SORT);
        final TextView head = Ui.info(this);
        rows.addView(head);
        if (cond.isEmpty()) {
            head.setText(Words.Db.smartInfo(cond, 0, 0) + "\n" + Words.Db.SMART_NO_COND);
            return;
        }
        final Library lib = Library.get(this);
        lib.start(null);
        final int mine = token;
        head.setText(Words.Db.smartInfo(cond, 0, 0));
        new Thread(new Runnable() {
            @Override
            public void run() {
                Smarts.Result r;
                try {
                    r = Smarts.over(lib).find(cond, sort, SMART_ROWS);
                } catch (Throwable t) {
                    Log.e("DbScreen.smart", t);
                    r = Smarts.Result.notReady();
                }
                final Smarts.Result res = r;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isFinishing() || mine != token) return;
                        if (!res.ready) {
                            head.setText(Words.Db.smartInfo(cond, 0, 0) + "\n" + Words.Db.INDEX_NOT_READY);
                            ui.postDelayed(new Runnable() {
                                @Override public void run() { if (!isFinishing() && mine == token) show(); }
                            }, 1500);
                            return;
                        }
                        head.setText(Words.Db.smartInfo(cond, res.count, res.size)
                                + (res.note.isEmpty() ? "" : "\n" + Words.Db.smartNote(res.note)));
                        if (res.count == 0) {
                            TextView t = Ui.info(DbScreen.this);
                            t.setText(Words.Db.SMART_EMPTY);
                            rows.addView(t);
                            return;
                        }
                        for (Smarts.Row w : res.rows) rows.addView(resultRow(w));
                        TextView more = Ui.info(DbScreen.this);
                        more.setText(res.count > res.rows.size() ? Words.Db.smartMore(res.rows.size(), res.count) : Words.Db.SMART_READONLY);
                        rows.addView(more);
                    }
                });
            }
        }).start();
    }

    private View resultRow(Smarts.Row w) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.VERTICAL);
        r.setPadding(Ui.dp(this, 12), Ui.dp(this, 8), Ui.dp(this, 12), Ui.dp(this, 8));
        TextView name = new TextView(this);
        name.setText((w.dir ? "📁 " : "") + w.name);
        name.setTextSize(15);
        name.setTextColor(Ui.TEXT);
        TextView sub = new TextView(this);
        sub.setTextSize(12);
        sub.setTextColor(Ui.SUB);
        sub.setText(w.folder + (w.size > 0 ? " · " + Files.size(w.size) : ""));
        r.addView(name);
        r.addView(sub);
        r.setBackgroundColor(Ui.BG);
        return r;
    }

    /** שורה: שם, ומתחתיו סוג, מספר זיהוי וכמה פריטים (ברשימה). */
    private View row(final Store.Item it, int count, final int position, final int size) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.VERTICAL);
        r.setPadding(Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 12), Ui.dp(this, 10));
        TextView name = new TextView(this);
        name.setText(it.name);
        name.setTextSize(16);
        name.setTextColor(Ui.TEXT);
        TextView sub = new TextView(this);
        sub.setTextSize(12);
        sub.setTextColor(Ui.SUB);
        if (it.type == Store.SMART) {
            String cond = store.prop(it.id, Store.P_COND) == null ? "" : store.prop(it.id, Store.P_COND);
            sub.setText(Words.Db.smartLine(it.id, cond.length() > 70 ? cond.substring(0, 70) + "…" : cond));
            name.setText("⚡ " + it.name);
        } else {
            boolean group = isGroup(it);
            boolean shared = it.type == Store.LIST && store.listsOf(it.id).size() > 1;
            String kind = group ? Words.Db.KIND_GROUP : shared ? Words.Db.KIND_REF : it.type == Store.LIST ? Words.Db.KIND_LIST
                    : it.type == Store.FILE ? Words.Db.KIND_FILE : it.type == Store.TITLE ? Words.Db.KIND_TITLE : Words.Db.KIND_NOTE;
            sub.setText(Words.Db.kindLine(kind, it.id, count >= 0 && !group ? Words.items(count) : ""));
            name.setText((shared ? "↪ " : "") + label(it));
            if (it.type == Store.TITLE) name.setTypeface(Typeface.DEFAULT_BOLD);
        }
        r.addView(name);
        r.addView(sub);
        r.setBackgroundColor(Ui.BG);
        r.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (it.type == Store.LIST || it.type == Store.SMART) {
                    path.add(it.id);
                    show();
                }
            }
        });
        r.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                more(it, position, size);
                return true;
            }
        });
        return r;
    }

    private boolean isGroup(Store.Item it) {
        return it.type == Store.LIST && "1".equals(store.prop(it.id, Store.P_GROUP));
    }

    /** מה מוצג כשם: חבורה — לפי הכותרת שבה, ואם אין — לפי החבר הראשון; כותרת — בין קווים. */
    private String label(Store.Item it) {
        if (it.type == Store.TITLE) return "── " + it.name + " ──";
        if (!isGroup(it)) return it.name;
        List<Store.Member> ms = store.members(it.id);
        String head = null;
        for (Store.Member m : ms) if (m.item.type == Store.TITLE) { head = m.item.name; break; }
        if (head == null && !ms.isEmpty()) head = ms.get(0).item.name;
        return Words.Db.groupLabel(head == null ? Words.Db.GROUP_EMPTY : head, ms.size());
    }

    // ---------- פעולות ----------

    /** "➕ הוספה…" בתוך רשימה: כל מה שאפשר להוסיף, בתפריט אחד. */
    private void addMenu() {
        final String[] labels = {Words.Db.NEW_SUBLIST, Words.Db.NEW_SMART, Words.Db.NEW_REF, Words.Db.NEW_NOTE, Words.Db.NEW_TITLE, Words.Db.NEW_GROUP};
        Ui.list(this, Words.Db.ADD_MENU, java.util.Arrays.asList(labels), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int which) {
                switch (which) {
                    case 0: newList(current()); break;
                    case 1: newSmart(current()); break;
                    case 2: newRef(); break;
                    case 3: newNote(); break;
                    case 4: newTitle(); break;
                    default: newGroup(); break;
                }
            }
        });
    }

    /** הפניה: רשימה קיימת נכנסת גם לכאן (אותה רשימה, לא עותק). מעגל — נחסם עם הסבר. */
    private void newRef() {
        final long into = current();
        final List<Store.Item> lists = new ArrayList<Store.Item>();
        for (Store.Item l : store.lists()) if (l.id != into) lists.add(l);
        if (lists.isEmpty()) {
            Ui.toast(this, Words.Db.NO_LISTS);
            return;
        }
        List<String> labels = new ArrayList<String>();
        for (Store.Item l : lists) labels.add(l.name + " · " + l.id);
        Ui.list(this, Words.Db.REF_PICK, labels, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int which) {
                addInto(into, lists.get(which));
            }
        });
    }

    /** מוסיף פריט לרשימה, עם ההודעות: כבר שם, מעגל. */
    private void addInto(long list, Store.Item it) {
        try {
            if (store.makesCycle(list, it.id)) Ui.toast(this, Words.Db.cycleBlocked(it.name, store.item(list).name));
            else if (!store.addMember(list, it.id)) Ui.toast(this, Words.Db.ALREADY_THERE);
        } catch (Store.Failure f) {
            Ui.toast(this, Words.Db.failed(f.getMessage()));
        }
        show();
    }

    private void newTitle() {
        Ui.askText(this, Words.Db.NEW_TITLE_TITLE, "", Words.Db.TITLE_TEXT, new Ui.Text1() {
            @Override
            public void ok(String value) {
                try {
                    store.addTitle(current(), value);
                } catch (Store.Failure f) {
                    Ui.toast(DbScreen.this, Words.Db.failed(f.getMessage()));
                }
                show();
            }
        });
    }

    /** חבורה: רשימה מוטמעת בתוך הרשימה הפתוחה — נפתחת מיד כדי להכניס לה פריטים. */
    private void newGroup() {
        try {
            final long into = current();
            final long[] id = new long[1];
            store.atomic(new Store.Work() {                                     // רשימה וסימון — יחד או לא בכלל
                @Override
                public void run(Store s) {
                    id[0] = s.createList(Words.Db.GROUP_NAME, into);
                    s.setProp(id[0], Store.P_GROUP, "1");
                }
            }, true);
            path.add(id[0]);
        } catch (Store.Failure f) {
            Ui.toast(this, Words.Db.failed(f.getMessage()));
        }
        show();
    }

    private void newList(final long parent) {
        Ui.askText(this, Words.Db.NEW_LIST_TITLE, "", Words.Db.LIST_NAME, new Ui.Text1() {
            @Override
            public void ok(String value) {
                try {
                    for (Store.Item l : store.lists())
                        if (l.name.equals(value.trim())) {
                            Ui.toast(DbScreen.this, Words.Db.sameName(value.trim()));
                            break;
                        }
                    store.createList(value, parent);
                } catch (Store.Failure f) {
                    Ui.toast(DbScreen.this, Words.Db.failed(f.getMessage()));
                }
                show();
            }
        });
    }

    private void newSmart(final long parent) {
        Ui.askText(this, Words.Db.NEW_SMART_TITLE, "", Words.Db.SMART_NAME, new Ui.Text1() {
            @Override
            public void ok(final String name) {
                Ui.askText(DbScreen.this, Words.Db.NEW_SMART_TITLE, "", Words.Db.SMART_COND, new Ui.Text1() {
                    @Override
                    public void ok(final String cond) {
                        try {
                            store.atomic(new Store.Work() {                     // הרשימה והתנאי — יחד או לא בכלל
                                @Override
                                public void run(Store s) {
                                    long id = s.createSmart(name, parent);
                                    s.setProp(id, Store.P_COND, cond.trim());
                                }
                            }, true);
                        } catch (Store.Failure f) {
                            Ui.toast(DbScreen.this, Words.Db.failed(f.getMessage()));
                        }
                        show();
                    }
                });
            }
        });
    }

    /** מוסיף פריט (קובץ, רעיון, רשימה חכמה) לרשימה ידנית שבוחרים. */
    private void addToList(final Store.Item it) {
        final List<Store.Item> lists = store.lists();
        if (lists.isEmpty()) {
            Ui.toast(this, Words.Db.NO_LISTS);
            return;
        }
        List<String> labels = new ArrayList<String>();
        for (Store.Item l : lists) labels.add(l.name + " · " + l.id);
        Ui.list(this, Words.Db.ADD_TO_LIST_TITLE, labels, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int which) {
                addInto(lists.get(which).id, it);
            }
        });
    }

    private void newNote() {
        Ui.askText(this, Words.Db.NEW_NOTE_TITLE, "", Words.Db.NOTE_TEXT, new Ui.Text1() {
            @Override
            public void ok(String value) {
                try {
                    store.addNote(current(), value);
                } catch (Store.Failure f) {
                    Ui.toast(DbScreen.this, Words.Db.failed(f.getMessage()));
                }
                show();
            }
        });
    }

    private void more(final Store.Item it, final int position, final int size) {
        final List<String> labels = new ArrayList<String>();
        final List<Integer> codes = new ArrayList<Integer>();
        if (current() == TRASH_ID) {                                 // בסל: רק שחזור או מחיקה לצמיתות
            labels.add(Words.Db.RESTORE);
            codes.add(9);
            labels.add(Words.Db.PURGE);
            codes.add(10);
            Ui.list(this, it.name, labels, new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface d, int which) { act(it, position, codes.get(which)); }
            });
            return;
        }
        if (it.type != Store.FILE) {                                 // שם הקובץ הוא שמו בכונן — לא משנים אותו כאן
            labels.add(Words.Db.RENAME);
            codes.add(0);
        }
        if (position >= 0) {                                         // בתוך רשימה: הזזה והסרה
            if (position > 0) { labels.add(Words.Db.MOVE_UP); codes.add(1); }
            if (position < size - 1) { labels.add(Words.Db.MOVE_DOWN); codes.add(2); }
            labels.add(Words.Db.REMOVE);
            codes.add(3);
        }
        if (it.type == Store.SMART) { labels.add(Words.Db.EDIT_COND); codes.add(5); }
        labels.add(Words.Db.ADD_TO_LIST);
        codes.add(6);
        if (it.type == Store.LIST) { labels.add(isGroup(it) ? Words.Db.TO_LIST : Words.Db.TO_GROUP); codes.add(7); }
        if (it.type == Store.NOTE || it.type == Store.TITLE) { labels.add(Words.Db.TO_TRASH); codes.add(8); }
        if (it.type == Store.LIST || it.type == Store.SMART) { labels.add(Words.Db.DELETE_LIST); codes.add(4); }
        Ui.list(this, it.name, labels, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int which) {
                act(it, position, codes.get(which));
            }
        });
    }

    private void act(final Store.Item it, int position, int code) {
        try {
            if (code == 0) {
                Ui.askText(this, Words.Db.RENAME, it.name, Words.Db.LIST_NAME, new Ui.Text1() {
                    @Override
                    public void ok(String value) {
                        try {
                            store.rename(it.id, value);
                        } catch (Store.Failure f) {
                            Ui.toast(DbScreen.this, Words.Db.failed(f.getMessage()));
                        }
                        show();
                    }
                });
                return;
            }
            if (code == 1) store.move(current(), it.id, position - 1);
            else if (code == 2) store.move(current(), it.id, position + 1);
            else if (code == 3) {
                if ((it.type == Store.NOTE || it.type == Store.TITLE) && store.listsOf(it.id).size() == 1) {
                    final long from = current();
                    new AlertDialog.Builder(this).setMessage(Words.Db.lastListQuestion(it.name))
                            .setPositiveButton(Words.Db.LAST_LIST_KEEP, new DialogInterface.OnClickListener() {
                                @Override public void onClick(DialogInterface d, int w) { store.removeMember(from, it.id); show(); }
                            })
                            .setNeutralButton(Words.Db.LAST_LIST_TRASH, new DialogInterface.OnClickListener() {
                                @Override public void onClick(DialogInterface d, int w) { store.trash(it.id); show(); }
                            })
                            .setNegativeButton(Words.CANCEL, null).show();
                    return;
                }
                store.removeMember(current(), it.id);
            } else if (code == 7) {
                store.setProp(it.id, Store.P_GROUP, isGroup(it) ? null : "1");
            } else if (code == 8) {
                store.trash(it.id);
            } else if (code == 9) {
                store.restore(it.id);
                Ui.toast(this, Words.Db.RESTORED);
            } else if (code == 10) {
                Ui.confirm(this, Words.Db.PURGE, it.name + "\n" + Words.Db.PURGE_DETAIL, Words.Db.PURGE, new Runnable() {
                    @Override
                    public void run() {
                        guarded(Backups.OP_PURGE, new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    store.purge(it.id);
                                } catch (Store.Failure f) {
                                    Ui.toast(DbScreen.this, Words.Db.failed(f.getMessage()));
                                }
                                show();
                            }
                        });
                    }
                });
                return;
            }
            else if (code == 5) {
                Ui.askText(this, Words.Db.EDIT_COND_TITLE, store.prop(it.id, Store.P_COND) == null ? "" : store.prop(it.id, Store.P_COND),
                        Words.Db.SMART_COND, new Ui.Text1() {
                            @Override
                            public void ok(String value) {
                                try {
                                    store.setProp(it.id, Store.P_COND, value.trim());
                                } catch (Store.Failure f) {
                                    Ui.toast(DbScreen.this, Words.Db.failed(f.getMessage()));
                                }
                                show();
                            }
                        });
                return;
            } else if (code == 6) {
                addToList(it);
                return;
            } else if (code == 4) {
                Ui.confirm(this, Words.Db.DELETE_LIST, Words.Db.deleteQuestion(it.name, it.id) + "\n"
                                + (it.type == Store.SMART ? Words.Db.DELETE_SMART_DETAIL : Words.Db.DELETE_DETAIL),
                        Words.Db.DELETE_YES, new Runnable() {
                            @Override
                            public void run() {
                                guarded(Backups.OP_DELETE, new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            store.deleteList(it.id);
                                            path.remove(Long.valueOf(it.id));
                                        } catch (Store.Failure f) {
                                            Ui.toast(DbScreen.this, Words.Db.failed(f.getMessage()));
                                        }
                                        show();
                                    }
                                });
                            }
                        });
                return;
            }
        } catch (Store.Failure f) {
            Ui.toast(this, Words.Db.failed(f.getMessage()));
        }
        show();
    }

    // ---------- מ0.5: גיבוי ושחזור ----------

    /** הגיבוי לפני ההעתקה נכשל — לא מעתיקים. */
    private static final class BackupFailed extends RuntimeException {
        BackupFailed(String m) {
            super(m);
        }
    }

    /** הגיבויים של האופן: בתיקייה הגלויה של התוכנה; עותקים זמניים — באחסון הפרטי. */
    private Backups backups(String m) {
        return new Backups(new File(new File(Paths.data(), Backups.DIR), m), new File(getCacheDir(), "mbackup"));
    }

    /** הגיבוי היומי, ברקע: עותק אם יש שינוי (פעם ביום), טקסט פעם בשבוע, ומחיקת ישנים. */
    private void startDaily() {
        final SqlStore s = store;
        final String m = mode;
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean made = false;
                synchronized (storeLock) {
                    if (s != store) return;                                    // המסד נסגר או הוחלף בינתיים
                    try {
                        Backups.Daily d = backups(m).daily(s, System.currentTimeMillis());
                        made = d.copied || d.texted || !d.deleted.isEmpty();
                    } catch (Throwable t) {
                        Log.e("DbScreen.daily", t);
                    }
                }
                if (!made) return;
                ui.post(new Runnable() {
                    @Override
                    public void run() {
                        if (!isFinishing() && s == store && path.isEmpty()) show();
                    }
                });
            }
        }).start();
    }

    /** לפני פעולה מוחקת: עותק (אם השתנה משהו מאז הגיבוי האחרון). אם הגיבוי נכשל — שואלים אם להמשיך בלי. */
    private void guarded(String op, final Runnable work) {
        try {
            backups(mode).before(store, op, System.currentTimeMillis());
        } catch (Throwable t) {
            Log.e("DbScreen.before", t);
            new AlertDialog.Builder(this).setMessage(Words.Db.backupBeforeFailed(String.valueOf(t.getMessage())))
                    .setPositiveButton(Words.Db.GO_ON_WITHOUT_BACKUP, new DialogInterface.OnClickListener() {
                        @Override public void onClick(DialogInterface d, int w) { work.run(); }
                    })
                    .setNegativeButton(Words.CANCEL, null).show();
            return;
        }
        work.run();
    }

    private View backupsRow() {
        List<Backups.Entry> all = backups(mode).list();
        String sub = all.isEmpty() ? Words.Db.BACKUPS_EMPTY_SUB : Words.Db.backupsSub(all.size(), FullCheck.when(all.get(0)));
        return specialRow(Words.Db.BACKUPS, sub, new Runnable() {
            @Override public void run() { backupMenu(); }
        });
    }

    /** רשימת הגיבויים (כל אחד נבדק מול הרישום), ו"גיבוי עכשיו". נגיעה בגיבוי — שחזור ממנו. */
    private void backupMenu() {
        final AlertDialog wait = new AlertDialog.Builder(this).setMessage(Words.Db.BACKUPS_CHECKING).setCancelable(false).show();
        final Backups bk = backups(mode);
        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<Backups.Entry> all = bk.list();
                final int[] states = new int[all.size()];
                for (int i = 0; i < all.size(); i++) states[i] = bk.verify(all.get(i));
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        wait.dismiss();
                        if (isFinishing()) return;
                        final boolean canBackup = store != null;
                        List<String> labels = new ArrayList<String>();
                        if (canBackup) labels.add(Words.Db.BACKUP_NOW);
                        for (int i = 0; i < all.size(); i++) {
                            Backups.Entry e = all.get(i);
                            labels.add(Words.Db.backupEntry(states[i], Words.Db.backupKind(e.kind, e.op, e.text), FullCheck.when(e),
                                    e.file.length()));
                        }
                        if (labels.isEmpty()) labels.add(Words.Db.BACKUPS_EMPTY_SUB);
                        Ui.list(DbScreen.this, Words.Db.backupsTitle(mode), labels, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int which) {
                                int i = canBackup ? which - 1 : which;
                                if (canBackup && which == 0) backupNow();
                                else if (i >= 0 && i < all.size()) restoreAsk(all.get(i), states[i]);
                            }
                        });
                    }
                });
            }
        }).start();
    }

    private void backupNow() {
        final AlertDialog wait = new AlertDialog.Builder(this).setMessage(Words.Db.BACKUP_SAVING).setCancelable(false).show();
        final Backups bk = backups(mode);
        final SqlStore s = store;
        new Thread(new Runnable() {
            @Override
            public void run() {
                String msg;
                try {
                    StringBuilder names = new StringBuilder();
                    synchronized (storeLock) {
                        if (s != store) throw new IllegalStateException("closed");
                        for (Backups.Entry e : bk.manual(s, System.currentTimeMillis())) names.append(e.name).append('\n');
                    }
                    msg = Words.Db.backupSaved(names.toString().trim(), bk.dir.getPath());
                } catch (Throwable t) {
                    Log.e("DbScreen.backupNow", t);
                    msg = Words.Db.failed(String.valueOf(t.getMessage()));
                }
                final String text = msg;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        wait.dismiss();
                        if (isFinishing()) return;
                        new AlertDialog.Builder(DbScreen.this).setMessage(text).setPositiveButton(Words.OK, null).show();
                        if (store != null) show();
                    }
                });
            }
        }).start();
    }

    /** בונה מסד מהגיבוי (ברקע, בקובץ נפרד), מציג מה יש בו מול מה שיש עכשיו, ורק אחרי אישור מחליף. */
    private void restoreAsk(final Backups.Entry e, int state) {
        if (state == Backups.KEPT_DAMAGED) {
            new AlertDialog.Builder(this).setMessage(Words.Db.KEPT_DAMAGED_INFO).setPositiveButton(Words.OK, null).show();
            return;
        }
        if (state == Backups.BAD) {
            new AlertDialog.Builder(this).setMessage(Words.Db.BACKUP_BAD).setPositiveButton(Words.OK, null).show();
            return;
        }
        if (state == Backups.CHANGED) Ui.toast(this, Words.Db.BACKUP_CHANGED_WARN);
        final String m = mode;
        final File db = dbFile(this, m), built = new File(db.getPath() + ".restore");
        final AlertDialog wait = new AlertDialog.Builder(this).setMessage(Words.Db.RESTORE_BUILDING).setCancelable(false).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                Backups.Built b = null;
                String err = null;
                try {
                    b = Backups.build(e.file, built, new IdCounter(counterFile()), e.content);
                } catch (Throwable t) {
                    Log.e("DbScreen.restore.build", t);
                    err = String.valueOf(t.getMessage());
                }
                final Backups.Built res = b;
                final String error = err;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        wait.dismiss();
                        if (isFinishing() || !m.equals(mode)) {
                            android.database.sqlite.SQLiteDatabase.deleteDatabase(built);
                            return;
                        }
                        if (error != null) {
                            new AlertDialog.Builder(DbScreen.this).setMessage(Words.Db.restoreFailed(error)).setPositiveButton(Words.OK, null).show();
                            return;
                        }
                        SqlStore s = store;
                        String label = Words.Db.backupKind(e.kind, e.op, e.text) + " · " + FullCheck.when(e);
                        String q = Words.Db.restoreQuestion(label, res.lists, res.items, s == null ? 0 : s.lists().size(),
                                s == null ? 0 : s.itemCount(), s == null);
                        new AlertDialog.Builder(DbScreen.this).setMessage(q).setCancelable(false)
                                .setPositiveButton(Words.Db.RESTORE_YES, new DialogInterface.OnClickListener() {
                                    @Override public void onClick(DialogInterface d, int w) { restoreRun(built, db); }
                                })
                                .setNegativeButton(Words.CANCEL, new DialogInterface.OnClickListener() {
                                    @Override public void onClick(DialogInterface d, int w) { android.database.sqlite.SQLiteDatabase.deleteDatabase(built); }
                                }).show();
                    }
                });
            }
        }).start();
    }

    /** קודם שומרים את המסד הנוכחי (או הפגום, כמו שהוא), אחר כך מחליפים ופותחים מחדש. */
    private void restoreRun(File built, File db) {
        try {
            long now = System.currentTimeMillis();
            if (store != null) backups(mode).before(store, Backups.OP_RESTORE, now);
            else if (db.isFile()) backups(mode).keepDamaged(db, now);
        } catch (Throwable t) {
            Log.e("DbScreen.restore.before", t);
            android.database.sqlite.SQLiteDatabase.deleteDatabase(built);
            new AlertDialog.Builder(this).setMessage(Words.Db.restoreFailed(Words.Db.backupBeforeFailed(String.valueOf(t.getMessage()))))
                    .setPositiveButton(Words.OK, null).show();
            return;
        }
        closeStore();
        try {
            Backups.swap(built, db);
        } catch (Throwable t) {
            Log.e("DbScreen.restore.swap", t);
            android.database.sqlite.SQLiteDatabase.deleteDatabase(built);
            new AlertDialog.Builder(this).setMessage(Words.Db.restoreFailed(String.valueOf(t.getMessage()))).setPositiveButton(Words.OK, null).show();
            refresh();
            return;
        }
        path.clear();
        damaged = null;
        refresh();
        Ui.toast(this, Words.Db.RESTORED_DB);
    }

    /** המסד פגום: לא פותחים ולא כותבים; מציעים לשחזר מגיבוי. */
    private void showDamaged() {
        token++;
        status.setText(Words.Db.damaged(mode, damaged));
        crumb.setText(Words.Db.ALL_LISTS);
        hint.setText(Words.Db.DAMAGED_HINT);
        buttons.removeAllViews();
        buttons.addView(button(Words.Db.RESTORE_FROM_BACKUP, new Runnable() {
            @Override public void run() { backupMenu(); }
        }));
        buttons.addView(button(Words.Db.FULL_CHECK, new Runnable() {
            @Override public void run() { fullCheck(); }
        }));
        rows.removeAllViews();
        rows.addView(backupsRow());
    }

    // ---------- מ0.2: העתקה מהמנוע הישן ----------

    private static File oldData() {
        return new File(Environment.getExternalStorageDirectory(), Flavor.oldAppDir());
    }

    private void importStart() {
        final File old = oldData();
        final List<String> modes = Importer.modes(old);
        if (modes.isEmpty()) {
            new AlertDialog.Builder(this).setTitle(Words.Db.IMPORT_TITLE).setMessage(Words.Db.IMPORT_NO_OLD)
                    .setPositiveButton(Words.OK, null).show();
            return;
        }
        // כל אופן נכנס למסד של אותו אופן (באג שנתפס במכשיר: שלושה אופנים במסד אחד התערבבו)
        if (modes.contains(mode)) {
            importPreview(old, mode);
            return;
        }
        final String active = Importer.activeMode(old);
        final List<String> labels = new ArrayList<String>();
        for (String m : modes) labels.add(Words.Db.modeLine(m, m.equals(active)));
        Ui.list(this, Words.Db.IMPORT_PICK, labels, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int which) {
                switchMode(modes.get(which));
                importPreview(old, modes.get(which));
            }
        });
    }

    /** קורא את המקור ומריץ את ההעתקה בלי לשמור: אותה ריצה, אותו דוח. */
    private void importPreview(final File old, final String mode) {
        importRun(old, mode, false, Words.Db.IMPORT_READING);
    }

    private void importRun(final File old, final String mode, final boolean keep, String message) {
        final AlertDialog wait = new AlertDialog.Builder(this).setTitle(Words.Db.IMPORT_TITLE).setMessage(message)
                .setCancelable(false).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                String err = null;
                Importer.Report rep = null;
                try {
                    Importer.Source src = Importer.load(new File(new File(old, Modes.DIR), mode));
                    if (keep) {
                        try {
                            backups(mode).before(store, Backups.OP_IMPORT, System.currentTimeMillis());
                        } catch (Throwable t) {
                            Log.e("DbScreen.import.backup", t);
                            throw new BackupFailed(Words.Db.importBackupFailed(String.valueOf(t.getMessage())));
                        }
                    }
                    rep = Importer.run(store, src, keep);
                } catch (BackupFailed t) {
                    err = t.getMessage();
                } catch (Throwable t) {
                    Log.e("DbScreen.import", t);
                    err = Words.Db.importFailed(String.valueOf(t.getMessage()));
                }
                final String error = err;
                final Importer.Report report = rep;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        wait.dismiss();
                        if (isFinishing()) return;
                        if (error != null) {
                            new AlertDialog.Builder(DbScreen.this).setTitle(Words.Db.IMPORT_TITLE).setMessage(error)
                                    .setPositiveButton(Words.OK, null).show();
                        } else if (keep) {
                            new AlertDialog.Builder(DbScreen.this).setTitle(Words.Db.IMPORT_DONE_TITLE).setMessage(reportText(true, mode, report))
                                    .setPositiveButton(Words.OK, null).show();
                            show();
                        } else if (report.nothingToDo()) {
                            new AlertDialog.Builder(DbScreen.this).setTitle(Words.Db.IMPORT_TITLE)
                                    .setMessage(Words.Db.IMPORT_NOTHING + (report.skippedAny() ? "\n\n" + reportText(false, mode, report) : ""))
                                    .setPositiveButton(Words.OK, null).show();
                        } else {
                            new AlertDialog.Builder(DbScreen.this).setTitle(Words.Db.IMPORT_PREVIEW_TITLE).setMessage(reportText(false, mode, report))
                                    .setPositiveButton(Words.Db.IMPORT_YES, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface d, int w) {
                                            importRun(old, mode, true, Words.Db.IMPORT_COPYING);
                                        }
                                    }).setNegativeButton(Words.CANCEL, null).show();
                        }
                    }
                });
            }
        }).start();
    }

    private static String reportText(boolean done, String mode, Importer.Report r) {
        return Words.Db.importReport(done, mode, r.listsNew, r.listsKnown, r.smartNew, r.smartKnown, r.filesNew, r.filesKnown,
                r.filesUpdated, r.linksAdded, r.refsAdded, r.propsSet, r.rootFiles, r.badLines, r.refCycles, r.refMissing, r.smart, r.nested, r.live);
    }

    // ---------- בדיקה מלאה (מ0.2.2) ----------

    /** כל הבדיקות במקום אחד: דוח טקסט אחד, שמועתק ללוח ואפשר לשלוח. */
    private void fullCheck() {
        final AlertDialog wait = new AlertDialog.Builder(this).setTitle(Words.Db.FULL_CHECK_TITLE)
                .setMessage(Words.Db.FULL_CHECK_RUNNING).setCancelable(false).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                String text;
                try {
                    text = FullCheck.run(oldData(), checkEnv());
                } catch (Throwable t) {
                    Log.e("DbScreen.fullCheck", t);
                    text = Words.Db.failed(String.valueOf(t));
                }
                deleteAll(new File(getCacheDir(), "mfull"));
                final String report = text;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        wait.dismiss();
                        if (!isFinishing()) showReport(report);
                    }
                });
            }
        }).start();
    }

    private void showReport(final String report) {
        copy(report);
        TextView t = new TextView(this);
        t.setText(report);
        t.setTextSize(13);
        t.setTextColor(Ui.TEXT);
        t.setTextIsSelectable(true);
        t.setPadding(Ui.dp(this, 16), Ui.dp(this, 8), Ui.dp(this, 16), Ui.dp(this, 8));
        ScrollView sc = new ScrollView(this);
        sc.addView(t);
        final AlertDialog d = new AlertDialog.Builder(this).setTitle(Words.Db.FULL_CHECK_TITLE).setView(sc)
                .setPositiveButton(Words.Db.COPY_REPORT, null).setNegativeButton(Words.CLOSE, null).show();
        d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { copy(report); }
        });
        if (store != null) show();
        else if (damaged != null) showDamaged();
    }

    private void copy(String text) {
        try {
            android.content.ClipboardManager cm = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            cm.setPrimaryClip(android.content.ClipData.newPlainText(Words.Db.FULL_CHECK_TITLE, text));
            Ui.toast(this, Words.Db.REPORT_COPIED);
        } catch (Throwable t) {
            Log.e("DbScreen.copy", t);
        }
    }

    private FullCheck.Env checkEnv() {
        final File tmp = new File(getCacheDir(), "mfull");
        deleteAll(tmp);
        tmp.mkdirs();
        final String[] selfText = {""};
        final boolean[] selfOk = {false};
        final int[] seq = {0};
        return new FullCheck.Env() {
            @Override
            public String header() {
                String v = "?";
                try {
                    v = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
                } catch (Throwable ignored) {
                }
                return Words.Db.headerLine(v, android.os.Build.VERSION.RELEASE, android.os.Build.MODEL, android.os.Build.VERSION.SDK_INT);
            }

            @Override
            public String selfTest() {
                String text = selfTestText(selfOk);
                selfText[0] = Words.Db.selfTestLine(selfOk[0], text);
                return selfText[0];
            }

            @Override
            public boolean selfTestOk() {
                return selfOk[0];
            }

            @Override
            public Store openTemp() throws Exception {
                int n = seq[0]++;
                return SqlStore.open(new File(tmp, "t" + n + ".db"), new IdCounter(new File(tmp, "t" + n + "-counter.txt")));
            }

            @Override
            public Smarts.Finder finder() {
                Library lib = Library.get(DbScreen.this);
                lib.start(null);
                return Smarts.over(lib);
            }

            @Override
            public Store openReal(String mode) throws Exception {
                File f = dbFile(DbScreen.this, mode);
                return f.exists() ? SqlStore.open(f, new IdCounter(counterFile())) : null;
            }

            @Override
            public File scratch() {
                File w = new File(tmp, "s" + (seq[0]++));
                w.mkdirs();
                return w;
            }

            @Override
            public Backups backups(String mode) {
                return DbScreen.this.backups(mode);
            }
        };
    }

    /** אותן בדיקות שרצות במחשב (StoreContract), על SQLite האמיתי של המכשיר, במסד זמני נפרד. ok[0] — האם עברו. */
    private String selfTestText(boolean[] ok) {
        final File dir = new File(getCacheDir(), "mselftest");
        deleteAll(dir);
        dir.mkdirs();
        long t0 = System.currentTimeMillis();
        try {
            final int[] seq = {0};
            StoreContract.Result r = StoreContract.run(new StoreContract.Opener() {
                @Override
                public Store fresh(File counter) throws Exception {
                    return SqlStore.open(new File(dir, "t" + (seq[0]++) + ".db"), new IdCounter(counter));
                }
            }, dir);
            String persist = SqlStore.persistenceCheck(dir);
            if (persist != null) r.failed.add("persistence: " + persist);
            else r.passed++;
            StoreContract.Result br = BackupContract.run(new File(dir, "backup"));
            r.passed += br.passed;
            for (String f : br.failed) r.failed.add("backup: " + f);
            String info = "?";
            SqlStore s = SqlStore.open(new File(dir, "info.db"), new IdCounter(new File(dir, "info-counter.txt")));
            try {
                info = s.info();
            } finally {
                s.close();
            }
            long ms = System.currentTimeMillis() - t0;
            ok[0] = r.failed.isEmpty();
            return ok[0] ? Words.Db.selfTestOk(r.passed, ms, info) : Words.Db.selfTestBad(r.passed, r.failed, info);
        } catch (Throwable t) {
            Log.e("DbScreen.selfTest", t);
            ok[0] = false;
            return Words.Db.failed(String.valueOf(t));
        } finally {
            deleteAll(dir);
        }
    }

    private static void deleteAll(File f) {
        File[] kids = f.listFiles();
        if (kids != null) for (File k : kids) deleteAll(k);
        f.delete();
    }
}
