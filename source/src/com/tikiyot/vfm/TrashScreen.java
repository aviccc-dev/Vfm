package com.tikiyot.vfm;

// סל המחזור. אחראי: הצגת ".נמחקו" מכל הכוננים יחד, שחזור למקום המקורי, ריקון.
// התוכן בא מהאינדקס; שחזור וריקון מאומתים מול הכונן. השלד — ListScreen (173).

import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.LinearLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TrashScreen extends ListScreen<Trash.Item> {

    /** "2026-09-24_13-10-05 שם.pdf" → "שם.pdf" */
    static String shownName(String trashName) {
        return trashName.matches("\\d{4}-\\d\\d-\\d\\d_\\d\\d-\\d\\d-\\d\\d .+") ? trashName.substring(20) : trashName;
    }

    /** זמן המחיקה מתוך השם בסל, כמספר (20260924131005); 0 אם אין. */
    static long deletedStamp(String trashName) {
        if (!trashName.matches("\\d{4}-\\d\\d-\\d\\d_\\d\\d-\\d\\d-\\d\\d .+")) return 0;
        return Long.parseLong(trashName.substring(0, 19).replaceAll("[^0-9]", ""));
    }

    public static final String EXTRA_FUNC = "func";           // מזהה הכלי: המיון נשמר בסעיף שלו (79)

    /** פריט בסל בעיני המיון (79). "תאריך מחיקה" — החדש קודם. */
    private static final Sort.Key<Trash.Item> ITEM = new Sort.Key<Trash.Item>() {
        @Override public String name(Trash.Item i) { return shownName(i.name()); }
        @Override public String folder(Trash.Item i) {
            if (i.original == null) return "";
            int k = i.original.lastIndexOf('/');
            return k > 0 ? i.original.substring(0, k) : "";
        }
        @Override public long size(Trash.Item i) { return i.size; }
        @Override public long mtime(Trash.Item i) { return i.mtime; }
        @Override public long added(Trash.Item i) { return 0; }
        @Override public boolean dir(Trash.Item i) { return i.dir; }
        @Override public long special(Trash.Item i) { return -deletedStamp(i.name()); }
        @Override public int section(Trash.Item i) { return 0; }
    };

    static String deletedAt(String trashName) {
        if (!trashName.matches("\\d{4}-\\d\\d-\\d\\d_.+")) return null;
        String[] d = trashName.substring(0, 10).split("-");
        return Integer.parseInt(d[2]) + "." + Integer.parseInt(d[1]) + "." + d[0].substring(2);
    }

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        LinearLayout root = build(saved, Words.Bin.TRASH_TITLE, Words.Bin.TRASH_SHORT);
        srt = Sort.of(Sort.DELETED_AT, false, Sort.tool(getIntent() != null ? getIntent().getStringExtra(EXTRA_FUNC) : null,
                Conf.GENERAL, Words.Bin.TRASH_DOT));
        java.util.Map<Actions.Action, Runnable> own = new java.util.HashMap<Actions.Action, Runnable>();
        own.put(Actions.RESTORE, new Runnable() {
            @Override public void run() { restore(chosen()); }
        });
        own.put(Actions.DELETE_FOREVER, new Runnable() {
            @Override public void run() { empty(chosen()); }
        });
        done(root, Actions.Tool.TRASH, own);
    }

    @Override
    protected String key(Trash.Item it) {
        return it.path();
    }

    @Override
    protected void bind(View v, Trash.Item it) {
        String when = deletedAt(it.name());
        if (BinBox.isBox(it.original)) {                                     // 1.10.0: רשימה / כלי / אופן שהוסרו
            Ui.bindIcon(v, "📦", shownName(it.name()), Words.Box.REMOVED + (when != null ? " " + when : "") + " · " + it.original.substring(BinBox.MARK.length()));
            return;
        }
        String status = (it.dir ? Words.Bin.DELETED_FEM : Words.Bin.DELETED) + (when != null ? " " + when : "")
                + (it.dir ? " · " + Words.files(it.files) + " · " + Files.size(it.size) : "")
                + (it.original == null ? Words.Bin.NO_ORIGINAL : "");
        Ui.bindRow(this, v, shownName(it.name()), it.dir, it.size,
                it.mtime, it.original != null ? it.original : it.path(), status, null);
    }

    @Override
    protected void open(Trash.Item it) {
        if (BinBox.isBox(it.original)) Ui.toast(this, Words.Box.HOW);
        else if (it.dir) Ui.toast(this, Words.Bin.FOLDER_IN_TRASH);
        else Ui.open(this, new File(it.path()));
    }

    @Override
    protected void refresh() {
        rows.clear();
        rows.addAll(Trash.list(lib));
        srt.apply(rows, ITEM);
        keepSelection();
        long size = 0;
        int dirs = 0;
        for (Trash.Item it : rows) {
            size += it.size;
            if (it.dir) dirs++;
        }
        int files = rows.size() - dirs;
        info.setText(!lib.ready ? Words.LOADING_INDEX
                : rows.isEmpty() ? Words.Bin.TRASH_EMPTY
                : (files > 0 ? Files.count(files) + Words.Bin.FILES_SUFFIX : "") + (files > 0 && dirs > 0 ? " · " : "")
                + (dirs > 0 ? Words.folders(dirs) : "")
                + " · " + Files.size(size) + " · " + srt.describe()
                + (hint ? Words.Bin.TRASH_HINT : ""));
        adapter.notifyDataSetChanged();
    }

    // ---------- פעולות ----------

    private void restore(List<Trash.Item> list) {
        if (list.isEmpty()) {
            Ui.toast(this, Words.NOTHING_SELECTED);
            return;
        }
        Physical.askRestore(this, list, after);
    }

    private void empty(List<Trash.Item> list) {
        if (list.isEmpty()) {
            Ui.toast(this, Words.NOTHING_SELECTED);
            return;
        }
        Physical.askDeleteForever(this, list, after);
    }

    // ---------- תפריט ----------

    @Override
    protected void menu(Menu menu) {
        menu.add(0, 100, 0, Words.Bin.EMPTY_TRASH);
    }

    @Override
    protected boolean item(int id) {
        if (id != 100) return false;
        empty(new ArrayList<Trash.Item>(rows));
        return true;
    }
}
