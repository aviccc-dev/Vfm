package com.tikiyot.vfm;

// בחירה — רכיב אחד (80.2). אחראי: מצב הבחירה, מה נבחר (קבצים, ורשימות ברשימה ידנית), הפס,
// "בחירת הכול", סיום, וחיבור למרשם הפעולות. מבטיח את הכלל האחיד: נגיעה ארוכה מתחילה בחירה (59).
// אסור לו: לדעת מה מוצג במסך (המסך מוסר "מה זה הכול" ומצייר מחדש).

import android.app.Activity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Select {

    /** צבע שורה נבחרת — אחד לכל המסכים. */
    public static final int HIGHLIGHT = Ui.SELECTED;

    /** "בחירת הכול": המסך ממלא את כל מה שמוצג (קבצים, ורשימות אם יש). */
    public interface All {
        void fill(Set<String> files, Set<String> lists);
    }

    public final Set<String> files = new LinkedHashSet<String>();
    public final Set<String> lists = new LinkedHashSet<String>();
    /** תיקיות בכונן שנבחרו (מנהל קבצים, חיפוש). */
    public final Set<String> dirs = new LinkedHashSet<String>();
    public final SelectBar bar;
    private final Runnable redraw;
    private boolean on;

    /** redraw — ציור המסך מחדש אחרי שינוי בבחירה. */
    public Select(Activity a, final All all, Runnable redraw) {
        this.redraw = redraw;
        bar = new SelectBar(a, new Runnable() {
            @Override public void run() { stop(); }
        });
        if (all != null) bar.add(Words.SELECT_ALL, new Runnable() {
            @Override
            public void run() {
                all.fill(files, lists);
                changed();
            }
        });
    }

    public boolean on() {
        return on;
    }

    public void start() {
        on = true;
        files.clear();
        lists.clear();
        dirs.clear();
        bar.show(true);
        changed();
    }

    public void stop() {
        on = false;
        files.clear();
        lists.clear();
        dirs.clear();
        bar.show(false);
        redraw.run();
    }

    /** מהתפריט "בחירה מרובה": התחלה או סיום. */
    public void flip() {
        if (on) stop(); else start();
    }

    /** כפתור "חזרה": מסיים בחירה אם יש. מחזיר אם טיפל. */
    public boolean back() {
        if (!on) return false;
        stop();
        return true;
    }

    public void toggle(String path) {
        if (!files.remove(path)) files.add(path);
        changed();
    }

    public void toggleDir(String path) {
        if (!dirs.remove(path)) dirs.add(path);
        changed();
    }

    /** נגיעה ארוכה על תיקייה בכונן — כמו על קובץ (59). */
    public void longPressDir(String path) {
        if (!on) start();
        toggleDir(path);
    }

    public boolean hasDir(String path) {
        return dirs.contains(path);
    }

    public void toggleList(String name) {
        if (!lists.remove(name)) lists.add(name);
        changed();
    }

    /** הכלל האחיד (59): נגיעה ארוכה מתחילה בחירה ומסמנת. */
    public void longPress(String path) {
        if (!on) start();
        toggle(path);
    }

    public void longPressList(String name) {
        if (!on) start();
        toggleList(name);
    }

    public boolean has(String path) {
        return files.contains(path);
    }

    /** הקובץ היחיד שנבחר, או null. */
    public String single() {
        return files.size() == 1 && lists.isEmpty() && dirs.isEmpty() ? files.iterator().next() : null;
    }

    /** אחרי רענון: מה שכבר לא מוצג יוצא מהבחירה. */
    public void keep(Collection<String> shown) {
        if (files.retainAll(shown)) bar.setCount(count());
    }

    public static int color(boolean selected) {
        return selected ? HIGHLIGHT : 0x00000000;
    }

    public int count() {
        return files.size() + lists.size() + dirs.size();
    }

    private void changed() {
        bar.setCount(count());
        redraw.run();
    }

    /** הפעולות מהמרשם (80, רמה 2). after — אחרי פעולה שהצליחה: הבחירה מסתיימת, ואז after. */
    public void actions(Activity a, Actions.Tool tool, final String picking, final Runnable after,
                        Map<Actions.Action, Runnable> own) {
        Actions.fill(a, bar, tool, new Actions.Selection() {
            @Override public List<String> files() { return new ArrayList<String>(files); }
            @Override public int lists() { return lists.size(); }
            @Override public int dirs() { return dirs.size(); }
            @Override public List<String> dirPaths() { return new ArrayList<String>(dirs); }
            @Override public String picking() { return picking; }
            @Override
            public void changed() {
                stop();
                if (after != null) after.run();
            }
        }, own);
    }
}
