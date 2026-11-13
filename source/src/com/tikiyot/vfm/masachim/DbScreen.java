package com.tikiyot.vfm;

// מ0.8: נגיעה בקובץ פותחת אותו (קובץ פקודות — רץ, כמו בכל התוכנה: Ui.open); הוספת קבצים מתיקייה ומחיפוש לרשימה.
// מסך מנוע המסד (מ0.1–מ0.8): רשימות, רשימות חכמות ("לא משויכים" כרשימה מיוחדת) ופריטים מעל Store,
// וגיבוי (מ0.5): עותק יומי ברקע כשנפתח המסד, עותק לפני פעולה מוחקת, "גיבוי עכשיו", שחזור, ומסד פגום שלא נפתח.
// השירותים לכל התוכנה (איפה המסד, האופן השמור, ההכנה של אופן, שחזור הדוגמאות, בדיקת המנוע) — עברו ל־Db (מבנה 6, 8). נשאר כאן: ההכנה של אופן (prepare / setup), שחזור הדוגמאות.
// אחראי: להציג ולהפעיל. אסור לו: לדעת איך הנתונים נשמרים (Store, Backups).

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

    /** מ0.9: פתיחה מהמראה המוכר — האופן, והרשימה שבה לפתוח (פעולות). בלי EXTRA_AT — נפתח הבית במראה המוכר. */
    static final String EXTRA_MODE = "mode", EXTRA_AT = "at";
    /** מ0.10: מה לעשות מיד ברשימה שנפתחה (פקודה "הוסף מהכונן / מחיפוש" מהמראה המוכר). */
    static final String EXTRA_DO = "do", DO_FOLDER = "folder", DO_SEARCH = "search", DO_BACKUPS = "backups";   // מ0.23: גיבויים מ־⋮
    private String pendingDo;
    private boolean familiarDone;
    /** מ0.20: האופנים שכבר הוצע בהם שחזור למסד ריק (פעם אחת לכל אופן בכל פתיחה של המסך). */
    private final java.util.Set<String> restoreOffered = new java.util.HashSet<String>();

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
    private final android.os.Handler ui = new android.os.Handler(android.os.Looper.getMainLooper());

    /**
     * מ0.11: הפתיחה של בניית מ — הבית של המסד במראה המוכר (האופן שנבחר בפעם הקודמת). אין הרשאה, אין בית, או מסד פגום —
     * מסך המסד (שם מבקשים הרשאה, מעתיקים מהישן, משחזרים).
     */
    static void start(Activity a) {
        start(a, false);
    }

    /** מ0.25 (באג 30): "בית" — הבית נפתח מחדש וכל שאר המסכים נסגרים (המשימה מתנקה). */
    /** מ0.35: הגיבויים של האופן הנוכחי — מההגדרות (הכניסה עברה לשם מ־⋮ של מסך הרשימות). */
    static void openBackups(Activity a) {
        String m = Db.currentMode != null ? Db.currentMode : Db.savedMode(a);
        if (m == null || m.isEmpty()) m = Db.defaultMode(a);
        long home = 0;
        SqlStore s = null;
        try {
            s = Db.openRead(a, m);
            home = DbPaths.home(s);
        } catch (Throwable t) {
            Log.e("DbScreen.openBackups", t);
        } finally {
            if (s != null) s.close();
        }
        android.content.Intent i = new android.content.Intent(a, DbScreen.class).putExtra(EXTRA_MODE, m);
        if (home != 0) i.putExtra(EXTRA_AT, home).putExtra(EXTRA_DO, DO_BACKUPS);   // בלי בית (מסד פגום או ריק) — מסך המסד, שמציע שחזור
        a.startActivity(i);
    }

    static void startFresh(Activity a) {
        DbListScreen.goingHome = false;
        start(a, true);
        a.overridePendingTransition(0, 0);
    }

    private static void start(Activity a, boolean fresh) {
        String m = a.getSharedPreferences(Db.PREFS, MODE_PRIVATE).getString(Db.PREF_MODE, "");
        if (m.isEmpty()) m = Db.defaultMode(a);
        long home = Db.prepare(a, m, Reserved.FILES);                                             // מ0.29: גם התקנה חדשה — ישר לבית (הדוגמאות)
        android.content.Intent i = home != 0 ? DbListScreen.intent(a, m, home, null) : new android.content.Intent(a, DbScreen.class);
        if (fresh) i.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
        a.startActivity(i);
    }

    private void switchMode(String m) {
        mode = m;
        Db.saveMode(this, m);
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
        java.util.TreeSet<String> all = new java.util.TreeSet<String>(Db.dbModes(this));
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
        String m = getIntent().getStringExtra(EXTRA_MODE);                   // מ0.9: מהמראה המוכר
        if (m != null && !m.isEmpty()) mode = m;
        long at = getIntent().getLongExtra(EXTRA_AT, 0);
        if (at > 0) path.add(at);
        familiarDone = at > 0;
        pendingDo = at > 0 ? getIntent().getStringExtra(EXTRA_DO) : null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (DbListScreen.goingHome) {                                          // מ0.9.2: "בית" הגיע עד כאן — פותחים את הבית
            DbListScreen.goingHome = false;
            familiarDone = false;
        }
        refresh();
    }

    /** פותח את מסד האופן הנבחר (אם עוד לא פתוח) ומציג. */
    private void refresh() {
        if (!Files.hasAccess(this)) {
            status.setText(Words.Db.NEED_ACCESS);
            rows.removeAllViews();
            buttons.removeAllViews();
            buttons.addView(button(Words.Main.GRANT, new Runnable() {
                @Override public void run() { Ui.requestFiles(DbScreen.this); }
            }));
            return;
        }
        if (mode.isEmpty()) mode = Db.savedMode(this);                        // מבנה 21: היה loadMode() — אותן שתי שורות בדיוק
        modeButton.setText(Words.Db.modeButton(mode));
        if (store == null) {
            damaged = null;
            try {
                store = Db.open(this, mode);
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
            offerRestoreIfEmpty();
            firstSetup();
        }
        show();
        if (pendingDo != null && store != null && current() > 0) {            // מ0.10
            String d = pendingDo;
            pendingDo = null;
            addingFiles = !DO_BACKUPS.equals(d);
            if (addingFiles) show();
            if (DO_FOLDER.equals(d)) fromFolder(current());
            else if (DO_BACKUPS.equals(d)) backupMenu();
            else fromSearch(current());
        }
        if (!familiarDone) {                                                  // מ0.9: פעם אחת — הבית במראה המוכר
            familiarDone = true;
            openFamiliar();
        }
    }

    /**
     * מ0.29: מסד בלי בית (אופן חדש, או התקנה חדשה) — מקבל בית; באופן הדוגמאות — הדוגמאות מיובאות מקובץ היצוא שבהתקנה
     * (עד כאן הן הגיעו מהמנוע הישן). מסד שיש בו בית — לא נוגעים.
     */
    private void firstSetup() {
        Db.setup(this, store, mode, Reserved.FILES);
    }

    /** מ0.37: המסך נפתח רק כדי להוסיף קבצים לרשימה (מתיקייה או מחיפוש) — מ־➕ שבמסך הרשימות. */
    private boolean addingFiles;

    /** מ0.9: הבית במראה המוכר (אם יש בית במסד). */
    private void openFamiliar() {
        long h = store == null ? 0 : DbPaths.home(store);
        if (h != 0) DbListScreen.open(this, mode, h);
        else Ui.toast(this, Words.Db.NO_HOME);
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
            // מ0.37 (המשתמש, 11.11.2026: "רעיון כותרת חבורה — תרשום להוסיף לפקודות בפעם הבאה. תוריד משם כל מה שמיותר"):
            // המסך מציג רק את מצב המסד, את הגיבויים, ודרך חזרה לבית. רעיון, כותרת וחבורה יחזרו כפקודות
            // (claude/pkudot-muamadot.md). כשהמסך נפתח רק כדי להוסיף קבצים לרשימה (➕ במסך הרשימות) — רק דרך חזרה.
            buttons.removeAllViews();
            rows.removeAllViews();
            if (addingFiles) {
                buttons.addView(button(Words.Db.BACK_TO_LIST, new Runnable() {
                    @Override public void run() { finish(); }
                }));
                hint.setText(Words.Db.ADDING_FILES);
            } else {
                buttons.addView(button(Words.Db.FAMILIAR, new Runnable() {
                    @Override public void run() { openFamiliar(); }
                }));
                hint.setText("");
                rows.addView(backupsRow());
            }
        } catch (Throwable t) {
            Log.e("DbScreen.show", t);
            status.setText(Words.Db.failed(String.valueOf(t.getMessage())));
        }
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

    private boolean isGroup(Store.Item it) {
        return it.type == Store.LIST && "1".equals(store.prop(it.id, Store.P_GROUP));
    }

    /** מה מוצג כשם: חבורה — לפי הכותרת שבה, ואם אין — לפי החבר הראשון; כותרת — בין קווים. */
    private String label(Store.Item it) {
        if (it.type == Store.TITLE) return "── " + it.name + " ──";
        if (it.type == Store.LIST && "1".equals(store.prop(it.id, Store.P_HOME))) return Words.Db.HOME_MARK + it.name;
        if (!isGroup(it)) return it.name;
        List<Store.Member> ms = store.members(it.id);
        String head = null;
        for (Store.Member m : ms) if (m.item.type == Store.TITLE) { head = m.item.name; break; }
        if (head == null && !ms.isEmpty()) head = ms.get(0).item.name;
        return Words.Db.groupLabel(head == null ? Words.Db.GROUP_EMPTY : head, ms.size());
    }

    // ---------- פעולות ----------

    /** מוסיף פריט (קובץ, רעיון, רשימה חכמה) לרשימה ידנית שבוחרים. */
    // ---------- מ0.8: קבצים מהכונן ----------

    /** הוספה בעסקה אחת, והודעה כמה נוספו וכמה כבר היו. */
    private void addFiles(long list, List<String> paths, List<Long> sizes) {
        try {
            DbFiles.Added a = DbFiles.add(store, list, paths, sizes, true);
            Ui.toast(this, Words.Db.filesAdded(a.added, a.already));
            if (addingFiles) {                                                  // מ0.37: נוספו — חוזרים לרשימה
                finish();
                return;
            }
        } catch (Throwable t) {
            Log.e("DbScreen.addFiles", t);
            Ui.toast(this, Words.Db.failed(String.valueOf(t.getMessage())));
        }
        show();
    }

    /** תיקייה מהכונן ← רשימת הקבצים שבה (בלי תתי־תיקיות ובלי נסתרים), לבחירה. */
    private void fromFolder(final long list) {
        FolderPicker.choose(this, new FolderPicker.Picked() {
            @Override
            public void ok(File folder) {
                File[] k = folder.listFiles();
                List<File> fs = new ArrayList<File>();
                if (k != null) for (File f : k) if (f.isFile() && !f.getName().startsWith(".")) fs.add(f);
                java.util.Collections.sort(fs, new java.util.Comparator<File>() {
                    @Override public int compare(File a, File b) { return a.getName().compareToIgnoreCase(b.getName()); }
                });
                if (fs.isEmpty()) {
                    Ui.toast(DbScreen.this, Words.Db.FOLDER_NO_FILES);
                    return;
                }
                List<String> names = new ArrayList<String>(), paths = new ArrayList<String>();
                List<Long> sizes = new ArrayList<Long>();
                for (File f : fs) {
                    if (names.size() >= PICK_MAX) break;
                    names.add(f.getName() + " · " + Files.size(f.length()));
                    paths.add(f.getAbsolutePath());
                    sizes.add(f.length());
                }
                choose(list, folder.getName() + (fs.size() > PICK_MAX ? " " + Words.Db.firstOf(PICK_MAX, fs.size()) : ""), names, paths, sizes);
            }
        });
    }

    /** חיפוש באינדקס (אותו מנוע ואותו תחביר כמו ברשימה חכמה) ← התוצאות, לבחירה. */
    private void fromSearch(final long list) {
        Ui.askText(this, Words.Db.SEARCH_TITLE, "", Words.Db.SEARCH_HINT, new Ui.Text1() {
            @Override
            public void ok(final String query) {
                if (query.trim().isEmpty()) return;
                final Library lib = Library.get(DbScreen.this);
                lib.start(null);
                Ui.toast(DbScreen.this, Words.Db.SEARCHING);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Smarts.Result r;
                        try {
                            r = Smarts.over(lib).find(query.trim(), "", PICK_MAX);
                        } catch (Throwable t) {
                            Log.e("DbScreen.search", t);
                            r = Smarts.Result.notReady();
                        }
                        final Smarts.Result res = r;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (isFinishing()) return;
                                if (!res.ready) {
                                    Ui.toast(DbScreen.this, Words.Db.INDEX_NOT_READY);
                                    return;
                                }
                                List<String> names = new ArrayList<String>(), paths = new ArrayList<String>();
                                List<Long> sizes = new ArrayList<Long>();
                                for (Smarts.Row w : res.rows) {
                                    if (w.dir) continue;
                                    names.add(w.name + " · " + w.folder);
                                    paths.add(w.path);
                                    sizes.add(w.size);
                                }
                                if (names.isEmpty()) {
                                    Ui.toast(DbScreen.this, res.note.isEmpty() ? Words.Db.SEARCH_NONE : Words.Db.smartNote(res.note));
                                    return;
                                }
                                choose(list, query.trim() + (res.count > names.size() ? " " + Words.Db.firstOf(names.size(), res.count) : ""),
                                        names, paths, sizes);
                            }
                        });
                    }
                }).start();
            }
        });
    }

    /** כמה קבצים לכל היותר בחלון הבחירה. */
    private static final int PICK_MAX = 500;

    /** חלון בחירה: תיבות סימון, "הכול", "הוספה". */
    private void choose(final long list, String title, List<String> names, final List<String> paths, final List<Long> sizes) {
        final boolean[] on = new boolean[names.size()];
        final AlertDialog d = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMultiChoiceItems(names.toArray(new String[0]), on, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override public void onClick(DialogInterface x, int which, boolean checked) { on[which] = checked; }
                })
                .setPositiveButton(Words.Db.ADD_CHOSEN, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface x, int w) {
                        List<String> ps = new ArrayList<String>();
                        List<Long> ss = new ArrayList<Long>();
                        for (int i = 0; i < on.length; i++)
                            if (on[i]) {
                                ps.add(paths.get(i));
                                ss.add(sizes.get(i));
                            }
                        if (ps.isEmpty()) Ui.toast(DbScreen.this, Words.Db.NOTHING_CHOSEN);
                        else addFiles(list, ps, ss);
                    }
                })
                .setNeutralButton(Words.Db.ADD_ALL, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface x, int w) { addFiles(list, paths, sizes); }
                })
                .setNegativeButton(Words.CANCEL, null)
                .create();
        d.show();
    }

    // ---------- מ0.5: גיבוי ושחזור ----------

    /** הגיבויים של האופן: בתיקייה הגלויה של התוכנה; עותקים זמניים — באחסון הפרטי. */
    private Backups backups(String m) {
        return Db.backupsOf(this, m);
    }

    /** הגיבוי היומי, ברקע: עותק אם יש שינוי (פעם ביום), טקסט פעם בשבוע, ומחיקת ישנים. */
    /**
     * מ0.20 (המשתמש: "בתוכנה הזו אם ייעלמו נתונים — זה עשוי להיות נזק גדול"): מסד ריק (למשל אחרי התקנה מחדש) ויש
     * גיבויים של האופן — מציעים מיד לשחזר מהאחרון התקין (השחזור עצמו — כרגיל: בנייה בצד, השוואה, אישור).
     */
    private void offerRestoreIfEmpty() {
        final SqlStore s = store;
        final String m = mode;
        if (s == null || !restoreOffered.add(m) || !s.lists().isEmpty() || !s.smarts().isEmpty() || !s.unassigned().isEmpty()) return;
        final Backups bk = backups(m);
        new Thread(new Runnable() {
            @Override
            public void run() {
                Backups.Entry best = null;
                int state = Backups.BAD;
                for (Backups.Entry e : bk.list()) {
                    int st = bk.verify(e);
                    if (st == Backups.OK || st == Backups.OK_UNRECORDED) {
                        best = e;
                        state = st;
                        break;
                    }
                }
                if (best == null) return;
                final Backups.Entry e = best;
                final int st = state;
                ui.post(new Runnable() {
                    @Override
                    public void run() {
                        if (isFinishing() || s != store) return;
                        new AlertDialog.Builder(DbScreen.this).setTitle(Words.Db.EMPTY_TITLE)
                                .setMessage(Words.Db.emptyRestore(m, when(e)))
                                .setPositiveButton(Words.Db.EMPTY_RESTORE, new DialogInterface.OnClickListener() {
                                    @Override public void onClick(DialogInterface d, int w) { restoreAsk(e, st); }
                                })
                                .setNegativeButton(Words.Db.EMPTY_NOT_NOW, null).show();
                    }
                });
            }
        }).start();
    }

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

    private View backupsRow() {
        List<Backups.Entry> all = backups(mode).list();
        String sub = all.isEmpty() ? Words.Db.BACKUPS_EMPTY_SUB : Words.Db.backupsSub(all.size(), when(all.get(0)));
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
                            labels.add(Backups.entryLabel(states[i], Backups.kindLabel(e.kind, e.op, e.text), when(e),
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
        final File db = Db.dbFile(this, m), built = new File(db.getPath() + ".restore");
        final AlertDialog wait = new AlertDialog.Builder(this).setMessage(Words.Db.RESTORE_BUILDING).setCancelable(false).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                Backups.Built b = null;
                String err = null;
                try {
                    b = Backups.build(e.file, built, new IdCounter(Db.counterFile()), e.content);
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
                        String label = Backups.kindLabel(e.kind, e.op, e.text) + " · " + when(e);
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
        Ui.toast(this, Words.Db.RESTORED_DB);
        // מ0.32 (מ13): אחרי שחזור — הכול נפתח מחדש, כמו כפתור "בית"; מסך שנשאר פתוח היה מציג את המסד הישן, וכתיבה בו נעלמה
        Db.saveMode(this, mode);
        startFresh(this);
    }

    /** המסד פגום: לא פותחים ולא כותבים; מציעים לשחזר מגיבוי. */
    private void showDamaged() {
        status.setText(Words.Db.damaged(mode, damaged));
        crumb.setText(Words.Db.ALL_LISTS);
        hint.setText(Words.Db.DAMAGED_HINT);
        buttons.removeAllViews();
        buttons.addView(button(Words.Db.RESTORE_FROM_BACKUP, new Runnable() {
            @Override public void run() { backupMenu(); }
        }));
        rows.removeAllViews();
        rows.addView(backupsRow());
    }

    // ---------- בדיקה מלאה (מ0.2.2) ----------

    /** כל הבדיקות במקום אחד: דוח טקסט אחד, שמועתק ללוח ואפשר לשלוח. */
    /** מתי נעשה גיבוי — לתצוגה. */
    static String when(Backups.Entry e) {
        return new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.US).format(new java.util.Date(e.made > 0 ? e.made : e.when));
    }

}
