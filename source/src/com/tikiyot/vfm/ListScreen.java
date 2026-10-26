package com.tikiyot.vfm;

// מסך רשימה אחד (173; צמצום, שלב 3) — השלד שכל מסכי הרשימות חולקים: שורת מידע, רשימה, בחירה (59, 80.2)
// והפס (83), מקשיב לשינויי האינדקס (Ui.Watch), "חזרה" שמסיימת בחירה קודם, ושני פריטי תפריט קבועים —
// "נתונים מלאים / מקוצרים" ו"מיון" (79). המסך מספק רק את שלו: מה בשורות, איך שורה נראית, מה נגיעה עושה,
// והפעולות המיוחדות לו. עובר אליו מסך אחרי מסך — ראשון הסל (TrashScreen); החיפוש, הרשימה הידנית והכפולים — בהמשך.

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

abstract class ListScreen<T> extends Activity {

    /** מספרי התפריט של השלד (שלא יתנגשו במספרים של המסך). */
    private static final int MENU_FULL = 9001, MENU_SORT = 9002;

    protected Library lib;
    protected final List<T> rows = new ArrayList<T>();
    protected Select sel;
    protected BaseAdapter adapter;
    protected TextView info;
    /** 1.12.0: רשימה (שורות) או רשת — לפי העיצוב; מתחלפת כשהעיצוב משתנה. */
    protected AbsListView list;
    /** 1.12.0: העיצוב של המסך; null — המראה הרגיל (Ui.row), כמו קודם. */
    protected Design design;
    private String designSig = "";
    private int tileW;
    private float[] fitted;
    /** ההסבר הקבוע — רק בחמש הפתיחות הראשונות של המסך. */
    protected boolean hint;
    /** המיון של המסך, או null — בלי "מיון" בתפריט. */
    protected Sort srt;

    // ---------- מה שכל מסך מספק ----------

    /** המפתח של השורה בבחירה (מסלול); null — שורה שאי אפשר לבחור. */
    protected abstract String key(T row);

    /** ציור שורה (התצוגה כבר נוצרה ב־Ui.row). */
    protected abstract void bind(View v, T row);

    /** 1.12.0: השם שמוצג בשורה (ל"התאם למקום"); null — לא נמדד. */
    protected String nameOf(T row) {
        return null;
    }

    /** 1.12.0: צלמית וצבע מהחלק הפרטי של העיצוב, לשורה שהיא רשימה; null — אין. */
    protected String[] ownLook(T row) {
        return null;
    }

    /** נגיעה, כשאין בחירה. */
    protected abstract void open(T row);

    /** טעינה מחדש של השורות ושל שורת המידע. */
    protected abstract void refresh();

    /** 1.9.0: שורה שהיא תיקייה בכונן (מנהל הקבצים הישיר): המסלול שלה בבחירה; null — לא. */
    protected String dirKey(T row) {
        return null;
    }

    /** שורה שהיא רשימה (בחירת רשימות ברשימה ידנית): השם שלה; null — לא. */
    protected String listKey(T row) {
        return null;
    }

    /** נגיעה. ברירת מחדל: בבחירה — סימון; אחרת — פתיחה. */
    protected void tap(T r) {
        if (sel.on() && key(r) != null) sel.toggle(key(r));
        else if (sel.on() && listKey(r) != null) sel.toggleList(listKey(r));
        else if (!sel.on()) open(r);
    }

    /** נגיעה ארוכה. ברירת מחדל: מתחילה בחירה עם השורה (59). */
    protected void hold(T r) {
        if (key(r) != null) sel.longPress(key(r));
        else if (listKey(r) != null) sel.longPressList(listKey(r));
    }

    /** "בחירת הכול": ברירת מחדל — כל השורות שיש להן מפתח. */
    protected void all(Set<String> files, Set<String> lists) {
        for (T r : rows) {
            if (key(r) != null) files.add(key(r));
            else if (listKey(r) != null) lists.add(listKey(r));
        }
    }

    /** תפריט: פריטים של המסך (הקבועים — בסדר 1 ו־2). */
    protected void menu(Menu menu) { }

    /** פריט של המסך נבחר; true — טופל. */
    protected boolean item(int id) {
        return false;
    }

    // ---------- השלד ----------

    /**
     * הבנייה: המסך, שורת המידע והרשימה. מחזיר את המסך — המסך מוסיף מה שלו, ואז קורא ל־done.
     * hintKey — שם המסך לספירת הפתיחות (Ui.firstTimes).
     */
    protected LinearLayout build(Bundle saved, String title, String hintKey) {
        lib = Library.get(this);
        setTitle(title);
        LinearLayout root = Ui.screen(this);
        info = Ui.info(this);
        hint = Ui.firstTimes(this, hintKey);
        root.addView(info);
        adapter = new BaseAdapter() {
            @Override public int getCount() { return rows.size(); }
            @Override public Object getItem(int i) { return rows.get(i); }
            @Override public long getItemId(int i) { return i; }

            @Override
            public View getView(int pos, View convert, ViewGroup parent) {
                Design d = design;
                View v = d == null ? Ui.row(ListScreen.this, convert) : Looks.row(ListScreen.this, convert, d, tileW);
                T r = rows.get(pos);
                bind(v, r);
                String k = key(r), l = listKey(r), dk = dirKey(r);
                int mark = Select.color(k != null ? sel.files.contains(k)
                        : l != null ? sel.lists.contains(l) : dk != null && sel.dirs.contains(dk));
                if (d == null) {
                    v.setBackgroundColor(mark);
                    return v;
                }
                String[] own = ownLook(r);
                Looks.apply(ListScreen.this, v, d, tileW, own == null ? "" : own[0], own == null ? "" : own[1], fitted);
                Looks.mark(ListScreen.this, v, d, mark);
                return v;
            }
        };
        list = makeList(null);
        root.addView(list, new LinearLayout.LayoutParams(-1, 0, 1f));
        sel = new Select(this, new Select.All() {
            @Override public void fill(Set<String> files, Set<String> lists) { all(files, lists); }
        }, new Runnable() {
            @Override public void run() { adapter.notifyDataSetChanged(); }
        });
        return root;
    }

    /** 1.12.0: רשימה בשורות או רשת (לפי העיצוב), עם המתאם והנגיעות. */
    private AbsListView makeList(Design d) {
        AbsListView l;
        if (d != null && (d.grid || d.cols > 1)) {                          // רשת, או שורות בכמה טורים
            GridView g = new GridView(this);
            g.setNumColumns(Math.max(1, d.cols));
            g.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
            g.setLayoutDirection(Ui.DIRECTION);
            int sp = Ui.dp(this, 4);
            g.setHorizontalSpacing(sp);
            g.setVerticalSpacing(sp);
            g.setPadding(sp, sp, sp, sp);
            g.setClipToPadding(false);
            l = g;
        } else {
            ListView lv = new ListView(this);
            if (d != null && d.tint) {                                        // אריחים צבעוניים — רווח ביניהם, בלי קו
                int sp = Ui.dp(this, 4);
                lv.setDivider(new android.graphics.drawable.ColorDrawable(0));
                lv.setDividerHeight(sp);
                lv.setPadding(sp * 2, sp, sp * 2, sp);
                lv.setClipToPadding(false);
            }
            l = lv;
        }
        l.setAdapter(adapter);
        l.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> a, View v, int pos, long id) {
                tap(rows.get(pos));
            }
        });
        l.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> a, View v, int pos, long id) {
                hold(rows.get(pos));                                          // הכלל האחיד (59)
                return true;
            }
        });
        return l;
    }

    /**
     * 1.12.0: המסך לפי העיצוב — נקרא אחרי שהשורות מולאו (לפני notifyDataSetChanged).
     * עיצוב אחר — הרשימה נבנית מחדש (שורות ↔ רשת), כדי ששורות מהעיצוב הקודם לא ימוחזרו.
     */
    protected void look(Design d) {
        design = d;
        tileW = d == null ? 0 : Looks.tileWidth(this, d);
        String sig = d == null ? "" : d.signature() + "|" + tileW;
        if (!sig.equals(designSig)) {
            designSig = sig;
            ViewGroup parent = (ViewGroup) list.getParent();
            int at = parent.indexOfChild(list);
            ViewGroup.LayoutParams lp = list.getLayoutParams();
            parent.removeView(list);
            list = makeList(d);
            parent.addView(list, at, lp);
        }
        if (d == null || !d.fit) {
            fitted = null;
            return;
        }
        List<String> names = new ArrayList<String>();
        for (T r : rows) names.add(nameOf(r));
        fitted = Looks.fit(this, d, names, tileW);
    }

    /** הפס ומרשם הפעולות (own — הפעולות שהמסך מבצע בעצמו), ואז המסך מוצג. */
    protected void done(LinearLayout root, Actions.Tool tool, Map<Actions.Action, Runnable> own) {
        sel.actions(this, tool, null, new Runnable() {
            @Override public void run() { refresh(); }
        }, own);
        root.addView(sel.bar.view);
        View box = TopRow.wrap(this, root);                                   // 1.9.0: שורות הכפתורים
        setContentView(box);
        sel.bar.hideWhileOn(TopRow.bottomOf(box));                            // 1.10.0: בזמן בחירה — רק פס הבחירה למטה
    }

    /** השורות שנבחרו, לפי הסדר במסך. */
    protected List<T> chosen() {
        List<T> out = new ArrayList<T>();
        for (T r : rows) if (key(r) != null && sel.files.contains(key(r))) out.add(r);
        return out;
    }

    /** הבחירה נשמרת רק על מה שעדיין מוצג (אחרי טעינה מחדש). */
    protected void keepSelection() {
        Set<String> s = new java.util.HashSet<String>();
        for (T r : rows) if (key(r) != null) s.add(key(r));
        sel.keep(s);
    }

    /** אחרי פעולה: סוף הבחירה, וטעינה מחדש. */
    protected final Physical.Done after = new Physical.Done() {
        @Override public void done(Physical.Result r) { sel.stop(); refresh(); }
    };

    private final Ui.Watch watch = new Ui.Watch(this, new Runnable() {
        @Override public void run() { refresh(); }
    });

    @Override
    protected void onResume() {
        super.onResume();
        lib.start(null);
        watch.on();
        refresh();
    }

    @Override
    protected void onPause() {
        watch.off();
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu m) {
        menu(m);
        if (srt != null) m.add(0, MENU_SORT, 1, Words.SORT);
        m.add(0, MENU_FULL, 2, Words.FULL_SHORT);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem it) {
        if (item(it.getItemId())) return true;
        if (it.getItemId() == MENU_FULL) {
            Ui.setFull(this, !Ui.full(this));
            adapter.notifyDataSetChanged();
            return true;
        }
        if (it.getItemId() == MENU_SORT && srt != null) {
            srt.menu(this, new Runnable() {
                @Override public void run() { refresh(); }
            });
            return true;
        }
        return super.onOptionsItemSelected(it);
    }

    @Override
    public void onBackPressed() {
        if (!sel.back()) super.onBackPressed();
    }
}
