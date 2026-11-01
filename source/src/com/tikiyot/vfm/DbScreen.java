package com.tikiyot.vfm;

// מסך מנוע המסד (מ0.1): רשימות ופריטים בסיסיים מעל Store. אחראי: להציג ולהפעיל את הפעולות הבסיסיות.
// אסור לו: לדעת איך הנתונים נשמרים (Store), או לגעת במנוע הישן. המסד כאן לבדיקות — עד מ0.2 לא נכנסים אליו נתונים אמיתיים.

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DbScreen extends Activity {

    static final String DB_FILE = "vfm-m.db";                       // בכונן הפרטי של התוכנה, לא בתיקייה הגלויה

    private SqlStore store;
    private TextView status, crumb, hint;
    private LinearLayout rows, buttons;
    private ScrollView sv;
    /** נתיב הרשימות הפתוחות; ריק = "כל הרשימות". */
    private final ArrayList<Long> path = new ArrayList<Long>();

    static File dbFile(android.content.Context c) {
        return c.getDatabasePath(DB_FILE);
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
        if (!Ui.hasFiles(this)) {
            status.setText(Words.Db.NEED_ACCESS);
            rows.removeAllViews();
            buttons.removeAllViews();
            buttons.addView(button(Words.Main.GRANT, new Runnable() {
                @Override public void run() { Ui.requestFiles(DbScreen.this); }
            }));
            return;
        }
        if (store == null) {
            try {
                store = SqlStore.open(dbFile(this), new IdCounter(counterFile()));
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
        try {
            List<Store.Item> all = store.lists();
            status.setText(Words.Db.status(all.size(), store.lastId(), store.systemCode(), store.info()));
            StringBuilder c = new StringBuilder(Words.Db.ALL_LISTS);
            for (Long id : path) c.append(Words.Db.CRUMB_SEP).append(store.item(id).name);
            crumb.setText(c.toString());
            buttons.removeAllViews();
            if (path.isEmpty()) {
                buttons.addView(button(Words.Db.NEW_LIST, new Runnable() {
                    @Override public void run() { newList(0); }
                }));
            } else {
                buttons.addView(button(Words.Db.NEW_SUBLIST, new Runnable() {
                    @Override public void run() { newList(current()); }
                }));
                buttons.addView(button(Words.Db.NEW_NOTE, new Runnable() {
                    @Override public void run() { newNote(); }
                }));
            }
            buttons.addView(button(Words.Db.SELF_TEST, new Runnable() {
                @Override public void run() { selfTest(); }
            }));
            rows.removeAllViews();
            if (path.isEmpty()) {
                if (all.isEmpty()) {
                    TextView t = Ui.info(this);
                    t.setText(Words.Db.EMPTY);
                    rows.addView(t);
                }
                for (Store.Item it : all) rows.addView(row(it, store.count(it.id), -1, 0));
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
        sub.setText(Words.Db.kindLine(it.type == Store.LIST ? Words.Db.KIND_LIST : Words.Db.KIND_NOTE, it.id,
                count >= 0 ? Words.items(count) : ""));
        r.addView(name);
        r.addView(sub);
        r.setBackgroundColor(Ui.BG);
        r.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (it.type == Store.LIST) {
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
        labels.add(Words.Db.RENAME);
        codes.add(0);
        if (position >= 0) {                                         // בתוך רשימה: הזזה והסרה
            if (position > 0) { labels.add(Words.Db.MOVE_UP); codes.add(1); }
            if (position < size - 1) { labels.add(Words.Db.MOVE_DOWN); codes.add(2); }
            labels.add(Words.Db.REMOVE);
            codes.add(3);
        }
        if (it.type == Store.LIST) { labels.add(Words.Db.DELETE_LIST); codes.add(4); }
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
            else if (code == 4) {
                Ui.confirm(this, Words.Db.DELETE_LIST, Words.Db.deleteQuestion(it.name, it.id) + "\n" + Words.Db.DELETE_DETAIL,
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

    // ---------- בדיקה עצמית ----------

    /** אותן בדיקות שרצות במחשב (StoreContract), על SQLite האמיתי של המכשיר, במסד זמני נפרד. */
    private void selfTest() {
        final ProgressHolder p = new ProgressHolder(new AlertDialog.Builder(this).setTitle(Words.Db.SELF_TEST_TITLE)
                .setMessage(Words.Db.RUNNING).setCancelable(false).show());
        new Thread(new Runnable() {
            @Override
            public void run() {
                final String text = selfTestText();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        p.dialog.dismiss();
                        new AlertDialog.Builder(DbScreen.this).setTitle(Words.Db.SELF_TEST_TITLE).setMessage(text)
                                .setPositiveButton(Words.OK, null).show();
                    }
                });
            }
        }).start();
    }

    private static final class ProgressHolder {
        final AlertDialog dialog;

        ProgressHolder(AlertDialog d) {
            dialog = d;
        }
    }

    private String selfTestText() {
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
            return r.failed.isEmpty() ? Words.Db.selfTestOk(r.passed, ms, info) : Words.Db.selfTestBad(r.passed, r.failed, info);
        } catch (Throwable t) {
            Log.e("DbScreen.selfTest", t);
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
