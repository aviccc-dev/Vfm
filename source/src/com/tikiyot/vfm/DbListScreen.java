package com.tikiyot.vfm;

// מ0.9: רשימה מהמסד במראה המוכר — אותו שלד כמו כל מסכי הרשימות (ListScreen): השורות או הרשת לפי העיצוב (Ui.row /
// Looks.row), הצלמית והסוג (Ui.kind), שורות הכפתורים (TopRow). הבית הוא רשימת הבית שבמסד, ברשת.
// נגיעה: רשימה — נפתחת כאן; רשימה חכמה — במסך החיפוש המוכר (לקריאה, כמו כלי חכם); קובץ — נפתח (פקודה — רצה).
// נגיעה ארוכה — מסך המסד באותה רשימה (שם כל הפעולות). אחראי: להציג. אסור לו: לכתוב למסד (נפתח לקריאה בלבד).

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
    private static final int MENU_DB = 1;

    private String mode;
    private long listId;
    private SqlStore store;

    /** פותח רשימה במראה המוכר. */
    static void open(android.app.Activity a, String mode, long list) {
        a.startActivity(new Intent(a, DbListScreen.class).putExtra(EXTRA_MODE, mode).putExtra(EXTRA_LIST, list));
    }

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        mode = getIntent().getStringExtra(EXTRA_MODE);
        listId = getIntent().getLongExtra(EXTRA_LIST, 0);
        LinearLayout root = build(saved, Words.Db.APP_NAME_M, "db-list");
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
        boolean home = "1".equals(store.prop(listId, Store.P_HOME));
        setTitle(home ? Words.Db.APP_NAME_M : it.name);
        String why = store.prop(listId, Store.P_WHY);
        info.setText((home ? Words.Db.homeInfo(mode, rows.size()) : Words.items(rows.size()))
                + (why == null || why.isEmpty() ? "" : "\n" + why));
        look(designOf(home));
        adapter.notifyDataSetChanged();
    }

    /** העיצוב: של הרשימה; בבית — של מסך הבית, ואם לא נבחר — רשת (כמו האריחים בבית). */
    private Design designOf(boolean home) {
        String name = store.prop(listId, Store.P_DESIGN);
        if (name != null && !name.isEmpty()) return Design.get(name);
        if (!home) return null;
        String h = Design.homeName();
        return Design.get(h.isEmpty() ? Design.FILES : h);
    }

    @Override
    protected String key(Store.Member row) {
        return null;                                                          // בלי בחירה כאן
    }

    @Override
    protected String nameOf(Store.Member row) {
        return row.item.name;
    }

    @Override
    protected String[] ownLook(Store.Member row) {
        String icon = store == null ? null : store.prop(row.item.id, Store.P_ICON);
        return icon == null || icon.isEmpty() ? null : new String[]{icon, ""};
    }

    @Override
    protected void bind(View v, Store.Member row) {
        Store.Item it = row.item;
        if (store == null) return;
        if (it.type == Store.LIST) {
            boolean group = "1".equals(store.prop(it.id, Store.P_GROUP));
            Ui.bindIcon(v, "", it.name, group ? Words.Db.KIND_GROUP : Words.items(store.count(it.id)));
            String icon = store.prop(it.id, Store.P_ICON);
            Ui.kind(v, store.listsOf(it.id).size() > 1 && listId != homeOrZero() ? Ui.LINK : Ui.MANUAL, icon == null ? "" : icon);
        } else if (it.type == Store.SMART) {
            String desc = store.prop(it.id, Store.P_DESC), icon = store.prop(it.id, Store.P_ICON);
            Ui.bindIcon(v, "", it.name, desc == null ? "" : desc);
            Ui.kind(v, Ui.SMART, icon == null ? "" : icon);
        } else if (it.type == Store.FILE) {
            Store.FileInfo fi = store.file(it.id);
            File f = fi == null ? null : new File(fi.path);
            boolean there = f != null && f.exists();
            Ui.bindRow(this, v, it.name, false, there ? f.length() : fi == null ? -1 : fi.size, there ? f.lastModified() : 0,
                    fi == null ? "" : fi.path, there ? null : Words.FILE_NOT_AT_PATH, null);
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
        Store.Item it = row.item;
        if (it.type == Store.LIST) {
            open(this, mode, it.id);
        } else if (it.type == Store.SMART) {
            String cond = prop(it.id, Store.P_COND), sort = prop(it.id, Store.P_SORT), view = prop(it.id, Store.P_VIEW);
            if (cond.isEmpty()) {
                Ui.toast(this, Words.Db.SMART_NO_COND);
                return;
            }
            Intent i = new Intent(this, SearchScreen.class);                 // כמו כלי חכם (FnSmart.open), בלי מזהה כלי
            i.putExtra(SearchScreen.EXTRA_QUERY, cond);
            i.putExtra(SearchScreen.EXTRA_FIXED, true);
            i.putExtra(SearchScreen.EXTRA_TITLE, it.name);
            i.putExtra(SearchScreen.EXTRA_SORT, sort.isEmpty() ? Sort.NAME : sort);
            i.putExtra(SearchScreen.EXTRA_VIEW, view);
            startActivity(i);
        } else if (it.type == Store.FILE) {
            Store.FileInfo fi = store.file(it.id);
            if (fi != null) Ui.open(this, new File(fi.path));
        }
    }

    private String prop(long id, String k) {
        String v = store.prop(id, k);
        return v == null ? "" : v;
    }

    @Override
    protected void hold(Store.Member row) {
        actions();
    }

    /** כל הפעולות — במסך המסד, באותה רשימה. */
    private void actions() {
        startActivity(new Intent(this, DbScreen.class).putExtra(DbScreen.EXTRA_MODE, mode).putExtra(DbScreen.EXTRA_AT, listId));
    }

    @Override
    protected void menu(Menu menu) {
        menu.add(0, MENU_DB, 0, Words.Db.MENU_ACTIONS);
    }

    @Override
    protected boolean item(int id) {
        if (id != MENU_DB) return false;
        actions();
        return true;
    }

    @Override
    protected void all(java.util.Set<String> files, java.util.Set<String> lists) { }

    List<Store.Member> shown() {
        return rows;
    }
}
