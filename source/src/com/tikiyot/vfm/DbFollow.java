package com.tikiyot.vfm;

// מ0.23: המסד עוקב אחרי פעולות הכונן (Physical, Trash) — כמו Tree.replaceEverywhere / removeEverywhere / countEverywhere בישן.
// קובץ שזז (העברה, שינוי שם, שחזור מהסל לשם אחר) — אותו פריט, המסלול החדש; קובץ שנמחק (לא "באפור") — יוצא מהרשימות;
// ספירה — לאזהרה "הקובץ ב־N רשימות" לפני מחיקה או העברה. בכל אופני העבודה שיש להם מסד (קובץ אחד יכול להיות בכמה).
// אחראי: הלוגיקה (נבדקת במחשב) ופתיחת המסדים. אסור לו: לגעת בכונן או במסכים.

import java.util.Collection;
import java.util.List;
import java.util.Map;

final class DbFollow {

    private DbFollow() { }

    /**
     * כל פריט קובץ במסלול הישן — במסלול החדש. במסלול החדש כבר יש פריט (הקובץ החליף קובץ אחר) — החברות של הישן עוברת אליו,
     * והישן יוצא מהרשימות. מחזיר כמה פריטים עודכנו. בעסקה אחת.
     */
    static int moved(Store s, final Map<String, String> moved) {
        final int[] n = {0};
        if (moved == null || moved.isEmpty()) return 0;
        s.atomic(new Store.Work() {
            @Override
            public void run(Store t) {
                for (Map.Entry<String, String> e : moved.entrySet()) {
                    String from = e.getKey(), to = e.getValue();
                    if (from == null || to == null || from.equals(to)) continue;
                    long src = t.findFile(from);
                    if (src == 0) continue;
                    long dst = t.findFile(to);
                    if (dst == 0) t.moveFile(src, to, DbPaths.driveOf(to));
                    else {
                        for (Long l : t.listsOf(src)) {
                            t.addMember(l, dst);
                            t.removeMember(l, src);
                        }
                    }
                    n[0]++;
                }
            }
        }, true);
        return n[0];
    }

    /** הקבצים יוצאים מכל הרשימות (הפריט נשאר, ב"לא משויכים"). מחזיר כמה הוצאות. בעסקה אחת. */
    static int gone(Store s, final Collection<String> paths) {
        final int[] n = {0};
        if (paths == null || paths.isEmpty()) return 0;
        s.atomic(new Store.Work() {
            @Override
            public void run(Store t) {
                for (String p : paths) {
                    long id = t.findFile(p);
                    if (id == 0) continue;
                    StringBuilder was = new StringBuilder();
                    for (Long l : t.listsOf(id)) if (t.removeMember(l, id)) {
                        n[0]++;
                        was.append(was.length() > 0 ? "," : "").append(l);
                    }
                    if (was.length() > 0) t.setProp(id, Store.P_WAS_IN, was.toString());   // מ0.32 (מ8): לזכור — לשחזור מהסל
                }
            }
        }, true);
        return n[0];
    }

    /**
     * מ0.32 (מ8): קבצים ששוחזרו מהסל של הכונן חוזרים לרשימות שהיו בהן כשנמחקו (רשימה שכבר איננה — מדולגת).
     * מחזיר כמה חזרות. בעסקה אחת.
     */
    static int back(Store s, final Collection<String> paths) {
        final int[] n = {0};
        if (paths == null || paths.isEmpty()) return 0;
        s.atomic(new Store.Work() {
            @Override
            public void run(Store t) {
                for (String p : paths) {
                    long id = t.findFile(p);
                    if (id == 0) continue;
                    String was = t.prop(id, Store.P_WAS_IN);
                    if (was == null) continue;
                    for (String x : was.split(",")) {
                        long l;
                        try {
                            l = Long.parseLong(x.trim());
                        } catch (NumberFormatException e) {
                            continue;
                        }
                        Store.Item it = t.item(l);
                        if (it == null || it.type != Store.LIST || t.prop(l, Store.P_TRASHED) != null) continue;
                        if (!t.listsOf(id).contains(l) && t.addMember(l, id)) n[0]++;
                    }
                    t.setProp(id, Store.P_WAS_IN, null);
                }
            }
        }, true);
        return n[0];
    }

    static int backEverywhere(final Collection<String> paths) {
        if (paths == null || paths.isEmpty()) return 0;
        return each(true, new Each() {
            @Override public int run(Store s) { return back(s, paths); }
        });
    }

    /** בכמה רשימות הקבצים האלה (כל הופעה נספרת). */
    static int count(Store s, Collection<String> paths) {
        int c = 0;
        if (paths == null) return 0;
        for (String p : paths) {
            long id = s.findFile(p);
            if (id != 0) c += s.listsOf(id).size();
        }
        return c;
    }

    /** עיצוב ששמו שונה (או נמחק — to ריק): כל רשימה שהפנתה אליו — לשם החדש (ריק — יורשת). מחזיר כמה. בעסקה אחת. */
    static int designRenamed(Store s, final String from, final String to) {
        final int[] n = {0};
        if (from == null || from.isEmpty()) return 0;
        s.atomic(new Store.Work() {
            @Override
            public void run(Store t) {
                for (Store.Item it : t.lists())
                    if (from.equals(t.prop(it.id, Store.P_DESIGN))) {
                        t.setProp(it.id, Store.P_DESIGN, to == null || to.isEmpty() ? null : to);
                        n[0]++;
                    }
            }
        }, true);
        return n[0];
    }

    // ================= בכל המסדים (במכשיר) =================

    private interface Each {
        int run(Store s);
    }

    /** בבניית מ, אחרי שמסך המסד נפתח פעם אחת (יש הקשר) — על כל מסד של אופן. אחרת — 0, בלי כלום. */
    private static int each(boolean write, Each e) {
        android.content.Context c = DbListScreen.app;
        if (c == null) return 0;
        int sum = 0;
        List<String> modes;
        try {
            modes = DbScreen.dbModes(c);
        } catch (Throwable t) {
            Log.e("DbFollow.modes", t);
            return 0;
        }
        for (String m : modes) {
            SqlStore s = null;
            try {
                s = write ? SqlStore.open(DbScreen.dbFile(c, m), new IdCounter(DbScreen.counterFile()))
                        : SqlStore.openReadOnly(DbScreen.dbFile(c, m));
                sum += e.run(s);
            } catch (Throwable t) {
                Log.e("DbFollow." + m, t);
            } finally {
                if (s != null) s.close();
            }
        }
        return sum;
    }

    static int movedEverywhere(final Map<String, String> moved) {
        if (moved == null || moved.isEmpty()) return 0;
        return each(true, new Each() {
            @Override public int run(Store s) { return moved(s, moved); }
        });
    }

    static int goneEverywhere(final Collection<String> paths) {
        if (paths == null || paths.isEmpty()) return 0;
        return each(true, new Each() {
            @Override public int run(Store s) { return gone(s, paths); }
        });
    }

    static int designRenamedEverywhere(final String from, final String to) {
        return each(true, new Each() {
            @Override public int run(Store s) { return designRenamed(s, from, to); }
        });
    }

    static int countEverywhere(final Collection<String> paths) {
        if (paths == null || paths.isEmpty()) return 0;
        return each(false, new Each() {
            @Override public int run(Store s) { return count(s, paths); }
        });
    }
}
