package com.tikiyot.vfm;

// מיון אחד לכל התוכנה (79). אחראי: אפשרויות המיון, ההשוואה בין שורות, התפריט, והשמירה.
// בסיס משותף בכל הכלים: שם · תאריך · גודל · סוג · תיקייה בכונן, כיוון (עולה/יורד).
// אפשרות מיוחדת אחת בראש התפריט, רק היכן שיש לה משמעות: חיפוש — "דירוג"; רשימה ידנית —
// "הסדר שלי"; סל — "תאריך מחיקה". קיבוץ לפי סוג ההתאמה — רק היכן שיש סוגי התאמה (חיפוש).
// תיקיות תמיד למעלה. אסור לו: לדעת מאיזה מסך הוא נקרא.

import android.app.Activity;
import android.content.DialogInterface;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class Sort {

    public static final String NAME = "שם", DATE = "תאריך", SIZE = "גודל", TYPE = "סוג", FOLDER = "תיקייה בכונן";  // תחביר
    public static final String[] BASE = {NAME, DATE, SIZE, TYPE, FOLDER};
    /** אפשרויות מיוחדות — כל אחת בכלי אחד. */
    public static final String RANK = "דירוג", MINE = "הסדר שלי", DELETED_AT = "תאריך מחיקה",  // תחביר
            SPACE = "מקום לפינוי";  // תחביר
    /** 189: מתי הקובץ הגיע — רק בכלים שעובדים מהאינדקס (חיפוש, רשימה חכמה). החדש קודם; לא ידוע — בסוף. */
    public static final String ADDED = "תאריך ההוספה";  // תחביר

    /** מה שהמיון צריך לדעת על שורה. כל כלי מספק מתאם לשורות שלו. */
    public interface Key<T> {
        String name(T t);

        /** התיקייה בכונן (לא כולל שם הקובץ). */
        String folder(T t);

        long size(T t);

        long mtime(T t);

        /** 189: "נוסף" (0 — לא ידוע). כלי שאין לו — 0. */
        long added(T t);

        boolean dir(T t);

        /** לאפשרות המיוחדת: דירוג בחיפוש, מספר בסדר השמור, זמן מחיקה בסל. קטן = קודם. */
        long special(T t);

        /** סוג ההתאמה (0 מלאה · 1 כתיב חסר · 2 חלק מהמילים), לקיבוץ. */
        int section(T t);
    }

    /** איפה נשמר המיון (79: בסעיף הכלי בקובץ ההגדרות, או לפי מסלול הרשימה). */
    public interface Store {
        String get(String key, String def);

        void set(String key, String value);
    }

    public final String special;
    public final boolean groups;
    private final Store store;
    public String by;
    public boolean desc, group;

    private Sort(String special, boolean groups, Store store, boolean withAdded) {
        this.withAdded = withAdded;
        this.special = special;
        this.groups = groups;
        this.store = store;
        String def = special != null ? special : NAME;
        String v = store != null ? store.get(SmartLists.SORT, def) : def;
        by = valid(v) ? v : legacy(v, def);
        desc = store != null && "יורד".equals(store.get("כיוון", "עולה"));  // תחביר
        group = groups && store != null && "כן".equals(store.get("קיבוץ", "לא"));  // תחביר
    }

    /** special: האפשרות המיוחדת של הכלי, או null. groups: יש בכלי סוגי התאמה. */
    public static Sort of(String special, boolean groups, Store store) {
        return new Sort(special, groups, store, false);
    }

    /** 189: כמו of, ועם "תאריך ההוספה" (הכלי מספק added). */
    public static Sort withAdded(String special, boolean groups, Store store) {
        return new Sort(special, groups, store, true);
    }

    private final boolean withAdded;

    private boolean valid(String v) {
        if (v == null) return false;
        if (v.equals(special)) return true;
        for (String b : BASE) if (b.equals(v)) return true;
        return ADDED.equals(v) && withAdded;
    }

    /** שמות ישנים מקובצי הגדרות קודמים. */
    private static String legacy(String v, String def) {
        if (v == null) return def;
        if (v.equals("סדר שמור") || v.equals("קובץ חסר קודם")) return MINE.equals(def) ? MINE : def;  // תחביר
        if (v.startsWith("תיקייה")) return FOLDER;  // תחביר
        return def;
    }

    /** ברירת מחדל שהגיעה מבחוץ (כלי בלי סעיף משלו): רק אם היא אפשרות חוקית. */
    public void prefer(String v) {
        if (valid(v)) by = v;
    }

    /** האפשרויות לפי הסדר: המיוחדת קודם, ואחריה הבסיס. */
    public String[] options() {
        List<String> l = new ArrayList<String>();
        if (special != null) l.add(special);
        Collections.addAll(l, BASE);
        if (withAdded) l.add(l.indexOf(DATE) + 1, ADDED);                 // אחרי "תאריך"
        return l.toArray(new String[0]);
    }

    /** לשורת הסיכום: "ממוין לפי שם ↓". */
    public String describe() {
        return Words.SortW.SORTED_BY + by + (desc ? " ↑" : " ↓") + (group ? Words.SortW.BY_MATCH : "");
    }

    /** מיון במקום. יציב: שורות שוות נשארות בסדר שהגיעו. */
    public <T> void apply(List<T> rows, final Key<T> k) {
        final Comparator<T> base = base(k);
        final boolean down = desc, grp = group, rank = RANK.equals(by);
        Collections.sort(rows, new Comparator<T>() {
            @Override
            public int compare(T a, T b) {
                if (grp && k.section(a) != k.section(b)) return k.section(a) - k.section(b);
                if (!rank && k.dir(a) != k.dir(b)) return k.dir(a) ? -1 : 1;    // תיקיות למעלה (בדירוג — לפי הדירוג)
                int c = base.compare(a, b);
                return down ? -c : c;
            }
        });
    }

    private <T> Comparator<T> base(final Key<T> k) {
        final java.text.Collator col = java.text.Collator.getInstance(new Locale("he"));
        final String b = by;
        return new Comparator<T>() {
            @Override
            public int compare(T x, T y) {
                if (b.equals(special)) {
                    int c = Long.compare(k.special(x), k.special(y));
                    return c != 0 || !b.equals(RANK) ? c : k.section(x) - k.section(y);
                }
                if (b.equals(NAME)) return col.compare(k.name(x), k.name(y));
                if (b.equals(DATE)) return Long.compare(k.mtime(y), k.mtime(x));       // החדש קודם
                if (b.equals(ADDED)) return Long.compare(k.added(y), k.added(x));       // 189: החדש קודם, לא ידוע בסוף
                if (b.equals(SIZE)) return Long.compare(k.size(y), k.size(x));         // הגדול קודם
                if (b.equals(TYPE)) {
                    int c = col.compare(type(k.name(x), k.dir(x)), type(k.name(y), k.dir(y)));
                    return c != 0 ? c : col.compare(k.name(x), k.name(y));
                }
                if (b.equals(FOLDER)) {
                    int c = col.compare(k.folder(x), k.folder(y));
                    return c != 0 ? c : col.compare(k.name(x), k.name(y));
                }
                return 0;
            }
        };
    }

    /** תווית הסוג למיון: הסיומת באותיות גדולות, "תיקייה" לתיקייה. */
    static String type(String name, boolean dir) {
        if (dir) return "";
        return Text.ext(name).toUpperCase(Locale.ROOT);                      // 104
    }

    // ---------- התפריט — אחד לכל הכלים ----------

    /** תפריט המיון. אחרי בחירה — נשמר, ו-changed רץ. */
    public void menu(Activity a, final Runnable changed) {
        final String[] opts = options();
        List<String> all = new ArrayList<String>();
        for (String o : opts) all.add((o.equals(by) ? "✓ " : "") + o);
        all.add("↕ " + (desc ? Words.SortW.NORMAL_DIR : Words.SortW.REVERSE_DIR));
        if (groups) all.add((group ? "✓ " : "") + Words.SortW.GROUP_BY_MATCH);
        Ui.options(a, Words.SortW.SORT_NOW + by + (desc ? " ↑" : " ↓"), all.toArray(new String[0]),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        choose(w, opts.length);
                        changed.run();
                    }
                });
    }

    /** בחירה בתפריט לפי המקום: אפשרות, כיוון, או קיבוץ. */
    void choose(int w, int n) {
        if (w < n) {
            by = options()[w];
            save(SmartLists.SORT, by);
        } else if (w == n) {
            desc = !desc;
            save("כיוון", desc ? "יורד" : "עולה");  // תחביר
        } else if (groups) {
            group = !group;
            save("קיבוץ", group ? "כן" : "לא");  // תחביר
        }
    }

    private void save(final String key, final String value) {
        if (store == null) return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    store.set(key, value);
                } catch (Throwable t) {
                    Log.e("שמירת מיון", t);
                }
            }
        }).start();
    }

    // ---------- מקומות שמירה ----------

    /** בסעיף הכלי (לפי המזהה); בלי כלי — בהגדרות הכלליות, בסעיף ובקידומת שניתנו. */
    public static Store tool(final String funcId, final String section, final String prefix) {
        return new Store() {
            @Override
            public String get(String key, String def) {
                Structure.Func fn = funcId != null ? Structure.byId(funcId) : null;      // 1.15.0: גם רשימה פנימית
                if (fn != null) {
                    String v = fn.params.get(key);
                    return v == null || v.isEmpty() ? def : v;
                }
                return Conf.get(section, prefix + key, def);
            }

            @Override
            public void set(String key, String value) {
                if (funcId != null && Structure.byId(funcId) != null) Structure.setById(funcId, key, value);
                else Conf.set(section, prefix + key, value);
            }
        };
    }

    /** רשימה ידנית: לפי מסלול הרשימה, בסעיף "מיון רשימות" (כמו קודם). */
    public static Store list(final String listKey) {
        return new Store() {
            @Override
            public String get(String key, String def) {
                return Conf.get(Conf.LIST_SORT, key.equals(SmartLists.SORT) ? listKey : listKey + " · " + key, def);
            }

            @Override
            public void set(String key, String value) {
                Conf.set(Conf.LIST_SORT, key.equals(SmartLists.SORT) ? listKey : listKey + " · " + key, value);
            }
        };
    }
}
