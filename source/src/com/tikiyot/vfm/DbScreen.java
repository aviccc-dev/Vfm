package com.tikiyot.vfm;

// מסך מנוע המסד (מ0.1–מ0.3): רשימות, רשימות חכמות ("לא משויכים" כרשימה מיוחדת) ופריטים מעל Store, והעתקה מהמנוע הישן.
// אחראי: להציג ולהפעיל. אסור לו: לדעת איך הנתונים נשמרים (Store), או לכתוב למנוע הישן (קורא בלבד).

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

    private SqlStore store;
    private String mode = "";
    private Button modeButton;
    private TextView status, crumb, hint;
    private LinearLayout rows, buttons;
    private ScrollView sv;
    /** נתיב הרשימות הפתוחות; ריק = "כל הרשימות". UNASSIGNED_ID = "לא משויכים" (רשימה מחושבת, לא פריט). */
    private final ArrayList<Long> path = new ArrayList<Long>();
    private static final long UNASSIGNED_ID = -1L;
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
        if (store != null) store.close();
        store = null;
        path.clear();
        refresh();
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
            try {
                store = SqlStore.open(dbFile(this, mode), new IdCounter(counterFile()));
            } catch (Throwable t) {
                Log.e("DbScreen.open", t);
                status.setText(Words.Db.openFailed(String.valueOf(t.getMessage())));
                return;
            }
        }
        show();
    }

    @Override
    protected void onDestroy() {
        if (store != null) store.close();
        store = null;
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (path.isEmpty()) {
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
                if (id == UNASSIGNED_ID) {
                    c.append(Words.Db.CRUMB_SEP).append(Words.Db.UNASSIGNED);
                    continue;
                }
                Store.Item it = store.item(id);
                if (it == null) {
                    path.remove(id);
                    continue;
                }
                c.append(Words.Db.CRUMB_SEP).append(it.type == Store.SMART ? "⚡ " : "").append(it.name);
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
            } else if (cur != UNASSIGNED_ID) {
                buttons.addView(button(Words.Db.NEW_SUBLIST, new Runnable() {
                    @Override public void run() { newList(current()); }
                }));
                buttons.addView(button(Words.Db.NEW_SMART, new Runnable() {
                    @Override public void run() { newSmart(current()); }
                }));
                buttons.addView(button(Words.Db.NEW_NOTE, new Runnable() {
                    @Override public void run() { newNote(); }
                }));
            }
            buttons.addView(button(Words.Db.FULL_CHECK, new Runnable() {
                @Override public void run() { fullCheck(); }
            }));
            hint.setText(cur == UNASSIGNED_ID ? Words.Db.UNASSIGNED_HINT : Words.Db.HINT);
            rows.removeAllViews();
            if (path.isEmpty()) {
                rows.addView(unassignedRow(store.unassigned().size()));
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

    /** "לא משויכים": שורה מיוחדת בראש הרשימות — מחושבת בכל פעם, לא נשמרת. */
    private View unassignedRow(int count) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.VERTICAL);
        r.setPadding(Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 12), Ui.dp(this, 10));
        TextView name = new TextView(this);
        name.setText(Words.Db.UNASSIGNED);
        name.setTextSize(16);
        name.setTextColor(Ui.TEXT);
        TextView sub = new TextView(this);
        sub.setTextSize(12);
        sub.setTextColor(Ui.SUB);
        sub.setText(Words.Db.UNASSIGNED_SUB + " · " + Words.items(count));
        r.addView(name);
        r.addView(sub);
        r.setBackgroundColor(Ui.BG);
        r.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                path.add(UNASSIGNED_ID);
                show();
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
            sub.setText(Words.Db.kindLine(it.type == Store.LIST ? Words.Db.KIND_LIST : it.type == Store.FILE ? Words.Db.KIND_FILE : Words.Db.KIND_NOTE,
                    it.id, count >= 0 ? Words.items(count) : ""));
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

    // ---------- פעולות ----------

    private void newList(final long parent) {
        Ui.askText(this, Words.Db.NEW_LIST_TITLE, "", Words.Db.LIST_NAME, new Ui.Text1() {
            @Override
            public void ok(String value) {
                try {
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
                    public void ok(String cond) {
                        try {
                            long id = store.createSmart(name, parent);
                            store.setProp(id, Store.P_COND, cond.trim());
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
                try {
                    if (!store.addMember(lists.get(which).id, it.id)) Ui.toast(DbScreen.this, Words.Db.ALREADY_THERE);
                } catch (Store.Failure f) {
                    Ui.toast(DbScreen.this, Words.Db.failed(f.getMessage()));
                }
                show();
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
        if (it.type != Store.LIST) { labels.add(Words.Db.ADD_TO_LIST); codes.add(6); }
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
            else if (code == 3) store.removeMember(current(), it.id);
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
                                try {
                                    store.deleteList(it.id);
                                    path.remove(Long.valueOf(it.id));
                                } catch (Store.Failure f) {
                                    Ui.toast(DbScreen.this, Words.Db.failed(f.getMessage()));
                                }
                                show();
                            }
                        });
                return;
            }
        } catch (Store.Failure f) {
            Ui.toast(this, Words.Db.failed(f.getMessage()));
        }
        show();
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
                    rep = Importer.run(store, src, keep);
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
                r.filesUpdated, r.linksAdded, r.propsSet, r.rootFiles, r.badLines, r.refs, r.smart, r.nested, r.live);
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
        show();
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
