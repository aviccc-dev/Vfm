package com.tikiyot.vfm;

// מבנה 90 (המשתמש, 15.11.2026: "כל מה שאתה יכול לחשוב עליו ולהגדיר. זה חשוב בפרט לבדיקות, אבל גם במשך העבודה עשוי מאד לעזור,
// בפרט במקרים של סנכרון קבצים וגיבויים" · "תתחיל בדוח"): **כל ההבדלים** בין שתי רשימות — לא רק הראשון (claude/hashvaat-reshimot.md, 4.2).
//
// איך משווים: רמה אחרי רמה. בכל רמה מתאימים את הפריטים של "א" לפריטים של "ב":
//   · קובץ — לפי המסלול שלו; רשימה וחכמה — לפי השם (באותה רשימה אין שתי רשימות באותו שם); רעיון וכותרת — לפי הטקסט.
//     אותו מפתח כמה פעמים — הראשון מול הראשון, השני מול השני.
//   · מה שנשאר בלי בן זוג: "רק ב־א" / "רק ב־ב". רשימה שנשארה בלי בן זוג, ובצד השני נשארה רשימה שהתוכן שלה שווה — זו אותה
//     רשימה ב**שם אחר** (ולא "נעלמה אחת ונוספה אחרת").
//   · מה שהותאם: **מקום אחר** — אם הסדר ביניהם שונה (מי שנשאר בסדר — הרצף הארוך ביותר שנשמר; השאר "זזו");
//     **מאפיין שונה** — צלמית, תיאור, עיצוב, "למה", חבורה, ולחכמה תנאי / מיון / כיוון / תצוגה; **גודל שונה** — לקובץ;
//     ולרשימה — ממשיכים פנימה.
//   · **קישור שונה** — פריט שב"א" הוא אותו פריט ממש בשני מקומות (זהה), וב"ב" אלה שני פריטים נפרדים; או להפך.
// הכלל ששומר על הכלי ישר: הדוח ריק **אם ורק אם** התוכן של שתי הרשימות שווה לפי תמונת המצב (DbSnap). אם יום אחד יימצא הבדל
// שהדוח אינו יודע לסווג — הוא נאמר בכל זאת, כ"הבדל שלא סווג".
// אחראי: הדוח בלבד. אסור לו: לכתוב למסד.

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class DbDiff {

    /** כמה שורות של הבדלים לכל היותר בדוח; מעבר לזה — "ועוד N". */
    static final int LIMIT = 200;

    /** מה יצא: השורות (עד LIMIT), וכמה מכל סוג. */
    static final class Out {
        final List<String> lines = new ArrayList<String>();
        int onlyA, onlyB, moved, renamed, props, links, sizes, other;

        int total() {
            return onlyA + onlyB + moved + renamed + props + links + sizes + other;
        }
    }

    private DbDiff() { }

    /** היכן פריט נראה לראשונה, ומול מי הותאם. */
    private static final class Seen {
        final long other;
        final String path;

        Seen(long other, String path) {
            this.other = other;
            this.path = path;
        }
    }

    private static final class Run {
        final Store s;
        final String la, lb;
        final Out out;
        final Map<Long, Seen> a = new HashMap<Long, Seen>(), b = new HashMap<Long, Seen>();

        Run(Store s, String la, String lb, Out out) {
            this.s = s;
            this.la = la;
            this.lb = lb;
            this.out = out;
        }

        void say(String line) {
            if (out.lines.size() < LIMIT) out.lines.add(line);
        }
    }

    /**
     * כל ההבדלים בין הרשימה x ("א", בתווית la) לרשימה y ("ב", lb). own — גם השם והמאפיינים של שתי הרשימות עצמן
     * (אחרת — רק מה שבתוכן).
     */
    static Out of(Store s, long x, long y, String la, String lb, boolean own) {
        Run r = new Run(s, la, lb, new Out());
        if (x == y) return r.out;                                                // אותה רשימה ממש — אין מה להשוות
        r.a.put(x, new Seen(y, ""));
        r.b.put(y, new Seen(x, ""));
        if (own) {
            String na = s.item(x).name, nb = s.item(y).name;
            if (!na.equals(nb)) {
                r.out.renamed++;
                r.say(Words.DbCmd.diffRenamed(la, na, lb, nb));
            }
            props(r, x, y, Words.DbCmd.DIFF_ITSELF);
        }
        int ownDiffs = r.out.total();
        walk(r, x, y, "");
        List<String> sa = DbSnap.of(s, x), sb = DbSnap.of(s, y);
        String inside = DbSnap.diff(sa.subList(1, sa.size()), sb.subList(1, sb.size()));
        if ((inside == null) != (r.out.total() == ownDiffs)) {                   // הדוח והתמונה חייבים להסכים
            r.out.other++;
            r.say(Words.DbCmd.diffOther(inside == null ? Words.DbCmd.REL_CONTENT_EQUAL : inside));
        }
        return r.out;
    }

    private static String val(Store s, long id, String key) {
        String v = s.prop(id, key);
        return v == null ? "" : DbExport.one(v);
    }

    private static void props(Run r, long x, long y, String path) {
        for (String k : DbSnap.PROPS) {
            String va = val(r.s, x, k), vb = val(r.s, y, k);
            if (va.equals(vb)) continue;
            r.out.props++;
            r.say(Words.DbCmd.diffProp(path, Words.DbCmd.propName(k), va, vb));
        }
    }

    private static String key(Store s, Store.Item it) {
        if (it.type == Store.FILE) {
            Store.FileInfo fi = s.file(it.id);
            return "F:" + (fi == null ? "?" + it.id : fi.path);
        }
        return it.type + ":" + it.name;
    }

    /** השם שבדרך: לרשימה — השם שלה; לקובץ — שם הקובץ (המסלול המלא נאמר ליד, ב־what). */
    private static String shown(Store s, Store.Item it) {
        if (it.type != Store.FILE) return DbExport.one(it.name);
        Store.FileInfo fi = s.file(it.id);
        return fi == null ? "?" : fi.path.substring(fi.path.lastIndexOf('/') + 1);
    }

    private static String join(String path, String name) {
        return path.isEmpty() ? name : path + Names.SEP + name;
    }

    /** מה שבתוך רשימה: {רשימות, פריטים אחרים} — כל פריט פעם אחת. */
    private static void count(Store s, long list, java.util.Set<Long> seen, int[] n) {
        for (Store.Member m : s.members(list)) {
            if (!seen.add(m.item.id)) continue;
            if (m.item.type == Store.LIST) {
                n[0]++;
                count(s, m.item.id, seen, n);
            } else n[1]++;
        }
    }

    private static String what(Store s, Store.Item it) {
        if (it.type == Store.FILE) {
            Store.FileInfo fi = s.file(it.id);
            return Words.DbCmd.kindName(it.type) + (fi == null ? "" : " · " + fi.path);
        }
        if (it.type != Store.LIST) return Words.DbCmd.kindName(it.type);
        int[] n = {0, 0};
        count(s, it.id, new java.util.HashSet<Long>(), n);
        return Words.DbCmd.kindName(it.type) + (n[0] + n[1] == 0 ? Words.DbCmd.DIFF_EMPTY : Words.DbCmd.diffInside(n[0], n[1]));
    }

    private static boolean sameInside(Store s, long x, long y) {
        List<String> sa = DbSnap.of(s, x), sb = DbSnap.of(s, y);
        return sa.size() > 1 && DbSnap.diff(sa.subList(1, sa.size()), sb.subList(1, sb.size())) == null;
    }

    private static void walk(Run r, long x, long y, String path) {
        Store s = r.s;
        List<Store.Member> ma = s.members(x), mb = s.members(y);
        int na = ma.size(), nb = mb.size();
        int[] toB = new int[na], toA = new int[nb];
        java.util.Arrays.fill(toB, -1);
        java.util.Arrays.fill(toA, -1);
        boolean[] newName = new boolean[na];
        Map<String, ArrayDeque<Integer>> inB = new HashMap<String, ArrayDeque<Integer>>();
        for (int j = 0; j < nb; j++) {
            String k = key(s, mb.get(j).item);
            ArrayDeque<Integer> q = inB.get(k);
            if (q == null) inB.put(k, q = new ArrayDeque<Integer>());
            q.add(j);
        }
        for (int i = 0; i < na; i++) {
            ArrayDeque<Integer> q = inB.get(key(s, ma.get(i).item));
            if (q == null || q.isEmpty()) continue;
            int j = q.poll();
            toB[i] = j;
            toA[j] = i;
        }
        for (int i = 0; i < na; i++) {                                            // אותה רשימה בשם אחר: התוכן שווה (ולא ריק)
            Store.Item ia = ma.get(i).item;
            if (toB[i] >= 0 || ia.type != Store.LIST) continue;
            for (int j = 0; j < nb; j++) {
                Store.Item ib = mb.get(j).item;
                if (toA[j] >= 0 || ib.type != Store.LIST) continue;
                if (ia.id != ib.id && !sameInside(s, ia.id, ib.id)) continue;
                toB[i] = j;
                toA[j] = i;
                newName[i] = true;
                break;
            }
        }
        boolean[] kept = keptOrder(toB);
        for (int i = 0; i < na; i++) {
            Store.Item ia = ma.get(i).item;
            String p = join(path, shown(s, ia));
            if (toB[i] < 0) {
                r.out.onlyA++;
                r.say(Words.DbCmd.diffOnly(r.la, p, what(s, ia)));
                continue;
            }
            Store.Item ib = mb.get(toB[i]).item;
            if (newName[i]) {
                r.out.renamed++;
                r.say(Words.DbCmd.diffRenamed(r.la, p, r.lb, DbExport.one(ib.name)));
            }
            if (!kept[i]) {
                r.out.moved++;
                r.say(Words.DbCmd.diffMoved(p, r.la, i + 1, r.lb, toB[i] + 1));
            }
            pair(r, ia, ib, p);
        }
        for (int j = 0; j < nb; j++) {
            if (toA[j] >= 0) continue;
            Store.Item ib = mb.get(j).item;
            r.out.onlyB++;
            r.say(Words.DbCmd.diffOnly(r.lb, join(path, shown(s, ib)), what(s, ib)));
        }
    }

    /** שני פריטים שהותאמו: האם הם "אותו פריט" באותה צורה בשני הצדדים (קישורים), המאפיינים, ולרשימה — פנימה. */
    private static void pair(Run r, Store.Item ia, Store.Item ib, String p) {
        Seen ha = r.a.get(ia.id), hb = r.b.get(ib.id);
        if (ha != null && ha.other == ib.id && hb != null && hb.other == ia.id) return;   // אותו זוג שוב — כבר הושווה
        boolean linkA = ha != null && ha.other != ib.id, linkB = hb != null && hb.other != ia.id;
        if (linkA) {
            r.out.links++;
            r.say(Words.DbCmd.diffLink(p, r.la, ha.path, r.lb));
        }
        if (linkB) {
            r.out.links++;
            r.say(Words.DbCmd.diffLink(p, r.lb, hb.path, r.la));
        }
        if (ha == null) r.a.put(ia.id, new Seen(ib.id, p));
        if (hb == null) r.b.put(ib.id, new Seen(ia.id, p));
        if (ia.id == ib.id) return;                                              // אותו פריט ממש בשני הצדדים — אין מה להשוות בתוכו
        if (linkA || linkB) {
            // הקישור עצמו כבר נאמר. מה שבפנים מושווה בנפרד, כאילו מההתחלה — אחרת כל רשימה שבתוכו הייתה נרשמת שוב כ"קישור שונה",
            // אף שהסיבה אחת.
            if (ia.type == Store.LIST || ia.type == Store.SMART) {
                Run sub = new Run(r.s, r.la, r.lb, r.out);
                sub.a.put(ia.id, new Seen(ib.id, p));
                sub.b.put(ib.id, new Seen(ia.id, p));
                props(sub, ia.id, ib.id, p);
                if (ia.type == Store.LIST) walk(sub, ia.id, ib.id, p);
            }
            return;
        }
        if (ia.type == Store.FILE) {
            Store.FileInfo fa = r.s.file(ia.id), fb = r.s.file(ib.id);
            long za = fa == null ? -1 : fa.size, zb = fb == null ? -1 : fb.size;
            if (za != zb) {
                r.out.sizes++;
                r.say(Words.DbCmd.diffSize(p, za, zb));
            }
            return;
        }
        if (ia.type == Store.NOTE || ia.type == Store.TITLE) return;
        props(r, ia.id, ib.id, p);
        if (ia.type == Store.LIST) walk(r, ia.id, ib.id, p);
    }

    /**
     * מי מהמותאמים נשאר בסדר: הרצף הארוך ביותר שבו גם המקומות ב"ב" עולים. מי שאינו בו — "זז". (מי שלא הותאם — true, לא נוגע.)
     */
    static boolean[] keptOrder(int[] toB) {
        int n = toB.length;
        boolean[] kept = new boolean[n];
        java.util.Arrays.fill(kept, true);
        int[] tail = new int[n], tailAt = new int[n], prev = new int[n];
        int len = 0;
        for (int i = 0; i < n; i++) {
            if (toB[i] < 0) continue;
            int lo = 0, hi = len;
            while (lo < hi) {
                int mid = (lo + hi) >>> 1;
                if (tail[mid] < toB[i]) lo = mid + 1;
                else hi = mid;
            }
            tail[lo] = toB[i];
            tailAt[lo] = i;
            prev[i] = lo > 0 ? tailAt[lo - 1] : -1;
            if (lo == len) len++;
        }
        boolean[] in = new boolean[n];
        for (int i = len > 0 ? tailAt[len - 1] : -1; i >= 0; i = prev[i]) in[i] = true;
        for (int i = 0; i < n; i++) if (toB[i] >= 0) kept[i] = in[i];
        return kept;
    }
}
