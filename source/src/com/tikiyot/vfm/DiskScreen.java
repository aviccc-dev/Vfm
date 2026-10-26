package com.tikiyot.vfm;

// 1.9.0: מנהל קבצים בקריאה ישירה מהכונן — "מה שרואים הוא מה שיש עכשיו". תיקייה אחת בכל פעם, בלי אינדקס
// ובלי מסננים. פותחים אותו מהכפתור "מנהל קבצים" (מהכוננים) או מתיקייה חיה ברשימה (מהתיקייה שלה).
// אחראי: הצגה ומעבר בין תיקיות. הפעולות על הנבחרים — דרך הפס הרגיל (Actions, Tool.DRIVE) ו־Physical.

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.LinearLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DiskScreen extends ListScreen<File> implements Clip.Holder, TopRow.Here {

    /** 1.11.0: "כאן" של המסך — התיקייה הפתוחה (לכפתור ⭐ ולמשתנה %כאן%). */
    @Override
    public File hereDir() {
        return here();
    }

    static final String EXTRA_DIR = "dir";
    private static final int MENU_HIDDEN = 1, MENU_NEW = 2, MENU_PASTE = 3, MENU_SORT = 4, MENU_UP = 5, MENU_DETAILS = 6, MENU_ZIP = 7;
    /** 1.11.0: המיון נשמר בהגדרות של אופן העבודה. */
    private static final String CONF = "קבצים פיזיים", SORT_KEY = "מיון", DIR_KEY = "כיוון";  // תחביר
    private android.widget.TextView clipBar;

    /** המסלול: "" — רשימת הכוננים; אחרת — תיקיות, מההתחלה ועד הנוכחית. */
    private ArrayList<String> trail = new ArrayList<String>();
    private boolean showHidden;
    private final Map<String, String> drives = new LinkedHashMap<String, String>();

    /** פתיחה: dir == null — מהכוננים. */
    static void open(Activity a, String dir) {
        Intent i = new Intent(a, DiskScreen.class);
        if (dir != null) i.putExtra(EXTRA_DIR, dir);
        a.startActivity(i);
    }

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        if (saved != null && saved.getStringArrayList("trail") != null) {
            trail = saved.getStringArrayList("trail");
            showHidden = saved.getBoolean("hidden");
        } else {
            String d = getIntent() != null ? getIntent().getStringExtra(EXTRA_DIR) : null;
            trail.add(d == null ? "" : d);
        }
        LinearLayout root = build(saved, Words.DiskW.TITLE, Words.DiskW.HINT);
        android.widget.TextView whyBar = Why.bar(this);                       // 1.13.0: מה ההבדל מ"קבצים וירטואליים"
        root.addView(whyBar, 0);
        Why.show(whyBar, Words.WhyW.KEY_SCREEN + Words.DiskW.TITLE, Words.WhyW.DISK_FILES);
        Map<Actions.Action, Runnable> own = new java.util.HashMap<Actions.Action, Runnable>();
        own.put(Actions.ADD_TO_LIST, new Runnable() {
            @Override public void run() { addSelected(); }
        });
        clipBar = new android.widget.TextView(this);                          // 1.11.0: הלוח — הדבקה בתיקייה הפתוחה
        clipBar.setBackgroundColor(Ui.HEADER);
        clipBar.setTextColor(Ui.TEXT);
        clipBar.setTextSize(15);
        int p = Ui.dp(this, 10);
        clipBar.setPadding(p, p, p, p);
        clipBar.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { paste(); }
        });
        clipBar.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Clip.clear();
                showClip();
                return true;
            }
        });
        root.addView(clipBar);
        done(root, Actions.Tool.DRIVE, own);
        showClip();
    }

    /** 1.11.0: גזירה / העתקה בתוך המסך — נשארים כאן, והלוח מוצג למטה. */
    @Override
    public void clipped() {
        showClip();
    }

    private void showClip() {
        if (clipBar == null) return;
        if (Clip.isEmpty()) {
            clipBar.setVisibility(View.GONE);
            return;
        }
        clipBar.setVisibility(View.VISIBLE);
        File d = here();
        boolean can = d != null && Clip.cannotPasteInto(d) == null;
        clipBar.setText(Words.DiskW.CLIP + Clip.label() + (can ? "   ·   " + Words.DiskW.PASTE : Words.DiskW.CLIP_HINT));
    }

    private void paste() {
        File d = here();
        if (d == null) {
            Ui.toast(this, Words.DiskW.CANT_HERE);
            return;
        }
        Physical.paste(this, d, new Runnable() {
            @Override
            public void run() {
                refresh();
                showClip();
            }
        });
    }

    private int sortBy() {
        String v = Conf.get(CONF, SORT_KEY, Words.DiskW.SORTS[0]);
        for (int i = 0; i < Words.DiskW.SORTS.length; i++) if (Words.DiskW.SORTS[i].equals(v)) return i;
        return 0;
    }

    private boolean sortDesc() {
        return Words.DiskW.DOWN_DIR.equals(Conf.get(CONF, DIR_KEY, Words.DiskW.UP_DIR));
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putStringArrayList("trail", trail);
        out.putBoolean("hidden", showHidden);
    }

    /** התיקייה הנוכחית, או null — רשימת הכוננים. */
    private File here() {
        String p = trail.get(trail.size() - 1);
        return p.isEmpty() ? null : new File(p);
    }

    @Override
    protected String key(File f) {
        return here() != null && !f.isDirectory() ? f.getAbsolutePath() : null;
    }

    @Override
    protected String dirKey(File f) {
        return here() != null && f.isDirectory() ? f.getAbsolutePath() : null;
    }

    @Override
    protected void bind(View v, File f) {
        if (here() == null) {
            String label = drives.get(f.getAbsolutePath());
            Ui.bindIcon(v, "💾", label != null ? label : f.getName(), Words.DiskW.NO_INDEX + " · " + f.getAbsolutePath());
            return;
        }
        Ui.bindRow(this, v, f.getName(), f.isDirectory(), f.isDirectory() ? 0 : f.length(), f.lastModified(), null, null, null);
    }

    @Override
    protected void tap(File f) {
        if (sel.on()) {
            if (key(f) != null) sel.toggle(key(f));
            else if (dirKey(f) != null) sel.toggleDir(dirKey(f));
            return;
        }
        open(f);
    }

    @Override
    protected void open(File f) {
        if (f.isDirectory()) {
            trail.add(f.getAbsolutePath());
            refresh();
            list.setSelection(0);
        } else {
            Ui.open(this, f);
        }
    }

    @Override
    protected void hold(File f) {
        if (key(f) != null) sel.longPress(key(f));
        else if (dirKey(f) != null) sel.longPressDir(dirKey(f));
    }

    @Override
    protected void all(Set<String> files, Set<String> lists) {
        for (File f : rows) {
            if (key(f) != null) files.add(key(f));
            else if (dirKey(f) != null) sel.dirs.add(dirKey(f));               // 1.11.0: גם התיקיות
        }
    }

    @Override
    protected void refresh() {
        rows.clear();
        File d = here();
        StringBuilder st = new StringBuilder(Words.DiskW.READ_AT
                + new java.text.SimpleDateFormat("HH:mm", java.util.Locale.US).format(new java.util.Date()));
        if (d == null) {
            drives.clear();
            for (Vols.Vol v : Vols.list(this)) {
                drives.put(v.root.getAbsolutePath(), v.label);
                rows.add(v.root);
            }
            setTitle(Words.DiskW.TITLE);
            st.append("   ·   ").append(Words.DiskW.DRIVES);
        } else {
            String label = drives.get(d.getAbsolutePath());
            setTitle(label != null ? label : d.getName());
            DiskList.Listing l = DiskList.read(d, showHidden, sortBy(), sortDesc());
            rows.addAll(l.dirs);
            rows.addAll(l.files);
            if (l.blocked) st.append("\n").append(Words.DiskW.BLOCKED);
            else if (l.missing) st.append("\n").append(Words.DiskW.NOT_FOUND);
            else if (l.unreadable) st.append("\n").append(Words.DiskW.UNREADABLE);
            else if (rows.isEmpty() && l.hidden == 0) st.append("\n").append(Words.DiskW.EMPTY);
            else {
                st.append("   ·   ").append(l.dirs.size()).append(Words.DiskW.FOLDERS)
                        .append("   ·   ").append(l.files.size()).append(Words.DiskW.FILES);
                if (l.hidden > 0) st.append("   ·   ").append(l.hidden).append(Words.DiskW.HIDDEN_COUNT);
                if (sortBy() != 0 || sortDesc())
                    st.append(Words.DiskW.SORTED).append(Words.DiskW.SORTS[sortBy()]).append(sortDesc() ? " ⬇" : " ⬆");
            }
            st.append("\n").append(Bidi.path(d.getAbsolutePath()));
        }
        info.setText(st);
        showClip();
        keepSelection();
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onBackPressed() {
        if (sel.back()) return;
        if (trail.size() > 1) {
            trail.remove(trail.size() - 1);
            refresh();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void menu(Menu m) {
        boolean in = here() != null;
        if (in) {
            m.add(0, MENU_NEW, 3, Words.DiskW.NEW_FOLDER);
            if (!Clip.isEmpty()) m.add(0, MENU_PASTE, 4, Words.DiskW.PASTE);
            m.add(0, MENU_SORT, 5, Words.DiskW.SORT);
            m.add(0, MENU_UP, 6, Words.DiskW.UP);
            m.add(0, MENU_DETAILS, 7, Words.DiskW.DETAILS);
            m.add(0, MENU_ZIP, 8, Words.DiskW.ZIP);
        }
        m.add(0, MENU_HIDDEN, 9, showHidden ? Words.Here.HIDE_HIDDEN : Words.Here.SHOW_HIDDEN);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu m) {
        m.clear();                                                            // 1.11.0: התפריט לפי התיקייה הפתוחה והלוח
        onCreateOptionsMenu(m);
        return true;
    }

    @Override
    protected boolean item(int id) {
        final File d = here();
        switch (id) {
            case MENU_HIDDEN:
                showHidden = !showHidden;
                refresh();
                return true;
            case MENU_NEW:
                if (d != null) Ui.askText(this, Words.DiskW.NEW_FOLDER, "", Words.DiskW.NEW_FOLDER_NAME, new Ui.Text1() {
                    @Override
                    public void ok(String name) {
                        Physical.askMakeFolder(DiskScreen.this, d, name, new Runnable() {
                            @Override public void run() { refresh(); }
                        });
                    }
                });
                return true;
            case MENU_PASTE:
                paste();
                return true;
            case MENU_SORT:
                sortMenu();
                return true;
            case MENU_UP:
                up();
                return true;
            case MENU_DETAILS:
                if (d != null) details(d);
                return true;
            case MENU_ZIP:
                if (d != null) Physical.askZip(this, d, new Runnable() {
                    @Override public void run() { refresh(); }
                });
                return true;
            default:
                return false;
        }
    }

    private void sortMenu() {
        String[] items = new String[Words.DiskW.SORTS.length + 1];
        for (int i = 0; i < Words.DiskW.SORTS.length; i++) items[i] = (i == sortBy() ? "✓ " : "") + Words.DiskW.SORTS[i];
        items[items.length - 1] = sortDesc() ? Words.DiskW.ASC : Words.DiskW.DESC;
        Ui.options(this, Words.DiskW.SORT, items, new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(android.content.DialogInterface dlg, int w) {
                if (w < Words.DiskW.SORTS.length) Conf.set(CONF, SORT_KEY, Words.DiskW.SORTS[w]);
                else Conf.set(CONF, DIR_KEY, sortDesc() ? Words.DiskW.UP_DIR : Words.DiskW.DOWN_DIR);
                refresh();
            }
        });
    }

    /** לתיקייה שמעל — גם אם לא הגענו ממנה; משורש כונן — לרשימת הכוננים. */
    private void up() {
        File d = here();
        if (d == null) return;
        boolean root = drives.containsKey(d.getAbsolutePath());
        for (Vols.Vol v : Vols.list(this)) if (v.root.getAbsolutePath().equals(d.getAbsolutePath())) root = true;
        trail.add(root ? "" : d.getParent());
        refresh();
        list.setSelection(0);
    }

    private void details(File d) {
        long[] m = Zip.measure(d);
        String when = new java.text.SimpleDateFormat("d.M.yy HH:mm", java.util.Locale.US).format(new java.util.Date(d.lastModified()));
        Ui.ask(this, d.getName(), Bidi.path(d.getAbsolutePath()),
                Words.DiskW.IN_FOLDER + Words.files(m[0]) + " · " + Files.size(m[1]) + "\n" + Words.DiskW.MODIFIED + when,
                null, Words.Disk.GOT_IT, new Runnable() {
                    @Override public void run() { }
                });
    }

    /** "הוספה לרשימה" — קבצים כפריטים; תיקיות — כמו בכל מקום (מתעדכנת / צילום). */
    private void addSelected() {
        List<File> folders = new ArrayList<File>();
        for (String p : sel.dirs) folders.add(new File(p));
        Adder.many(this, folders, new ArrayList<String>(sel.files), null, new Adder.Done() {
            @Override public void done() { sel.stop(); refresh(); }
        });
    }
}
