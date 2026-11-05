package com.tikiyot.vfm;

// מ0.24: הלוח של המסד — "העתקה" / "העברה" מסמנים, מנווטים בעצמך, ו"הדבקה כאן" ברשימה (claude/yetzia-mipituach.md,
// "לסט התיקונים הבא" פריט 3 — ההחלטות של המשתמש, 5.11.2026):
//   · לוח קל — זוכר מה סומן (מספר זיהוי), ומדביק את מה שיש בזמן ההדבקה; פריט שכבר לא קיים — "המקור כבר לא קיים".
//   · כל פריט שומר את הפעולה שלו: "ההבדל בין העתק להעביר הוא רק מה קורה למקור". כולם נכנסים ליעד; "העבר" יוצאים
//     מהמקור ומהלוח, "העתק" נשארים בלוח (להדביק שוב).
//   · רגיל — הלוח מתחלף; "צירוף" — מוסיף למה שיש (מצב שנשמר — ברירת המחדל).
//   · "העתקה" — תמיד עותק עצמאי (DbListCmd.copyIds); קישור — רק בפקודות.
//   · עולמות נפרדים: הלוח הזה — רק לרשימות; לתוך רשימה אפשר גם קבצים ותיקיות שהועתקו בכונן (Clip, בהעתקה בלבד).
// אחראי: הלוח וההדבקה, בלי ממשק (נבדק במחשב). הממשק — DbListScreen.

import java.util.ArrayList;
import java.util.List;

final class DbClip {

    static final int COPY = 0, MOVE = 1;

    /** פריט בלוח: מספר הזיהוי, הרשימה שממנה סומן (להעברה), הפעולה, ושם וסוג (להצגה). */
    static final class Item {
        final long id, from;
        final int how, type;
        final String name;

        Item(long id, long from, int how, int type, String name) {
            this.id = id;
            this.from = from;
            this.how = how;
            this.type = type;
            this.name = name;
        }
    }

    private static final List<Item> items = new ArrayList<Item>();
    /** האופן שממנו סומן — בין אופנים אין הדבקה מהלוח (יש פקודה: COPY "אופן:X/…"). */
    private static String mode;

    private DbClip() { }

    /** מסמן ללוח. add — מוסיף למה שיש (אותו פריט מאותה רשימה — הפעולה מתעדכנת); אחרת, או מאופן אחר — הלוח מתחלף. */
    static synchronized void put(String fromMode, long from, List<Store.Item> what, int how, boolean add) {
        if (!add || mode == null || !mode.equals(fromMode)) items.clear();
        mode = fromMode;
        for (Store.Item it : what) {
            for (int i = items.size() - 1; i >= 0; i--)
                if (items.get(i).id == it.id && items.get(i).from == from) items.remove(i);
            items.add(new Item(it.id, from, how, it.type, it.name));
        }
    }

    static synchronized void clear() {
        items.clear();
        mode = null;
    }

    static synchronized boolean empty() {
        return items.isEmpty();
    }

    static synchronized List<Item> items() {
        return new ArrayList<Item>(items);
    }

    static synchronized void remove(Item it) {
        items.remove(it);
        if (items.isEmpty()) mode = null;
    }

    static synchronized String mode() {
        return mode;
    }

    static synchronized boolean anyMove() {
        for (Item it : items) if (it.how == MOVE) return true;
        return false;
    }

    /** "3 להעתקה · 1 להעברה". */
    static synchronized String label() {
        int c = 0, m = 0;
        for (Item it : items) if (it.how == MOVE) m++; else c++;
        String s = c > 0 ? c + Words.ClipW.TO_COPY : "";
        if (m > 0) s += (s.isEmpty() ? "" : " · ") + m + Words.ClipW.TO_MOVE;
        return s;
    }

    /** אם אפשר להדביק כאן (null — אפשר); אחרת — למה לא. */
    static synchronized String cannot(String here) {
        if (items.isEmpty()) return null;
        return mode != null && !mode.equals(here) ? Words.DbClipW.otherMode(mode) : null;
    }

    /**
     * ההדבקה לתוך dest, בעסקה של מי שקורא (DbListCmd.now): כל פריט לפי הפעולה שלו. פריט שכבר לא קיים — לא נכנס
     * ("המקור כבר לא קיים"). "העבר" שנכנס — יוצא מהלוח; השאר נשארים.
     */
    static DbListCmd.Copied paste(Store t, long dest) {
        DbListCmd.Copied all = new DbListCmd.Copied();
        List<Item> done = new ArrayList<Item>();
        for (Item it : items()) {
            DbListCmd.Copied one;
            if (t.item(it.id) == null) {
                one = new DbListCmd.Copied();
                one.skipped = 1;
                one.why = Words.DbClipW.GONE;
            } else if (it.how == MOVE) {
                one = DbListCmd.moveTo(t, java.util.Collections.singletonList(it.id), it.from, dest);
                if (one.done == 1) done.add(it);
            } else {
                one = DbListCmd.copyIds(t, java.util.Collections.singletonList(it.id), dest);
            }
            all.done += one.done;
            all.skipped += one.skipped;
            if (all.why == null) all.why = one.why;
        }
        synchronized (DbClip.class) {
            items.removeAll(done);
            if (items.isEmpty()) mode = null;
        }
        return all;
    }

    /** קבצים ותיקיות שהועתקו בכונן (Clip) — נכנסים לרשימה; בהעברה — לא (עולמות נפרדים). */
    static boolean diskForLists() {
        return !Clip.isEmpty() && Clip.isCopy();
    }
}
