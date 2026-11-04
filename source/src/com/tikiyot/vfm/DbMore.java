package com.tikiyot.vfm;

// מ0.22: עוד ליבה של המסד, לפקודות ולמסכים — מיון שורות, סידור ידני, ייבוא מטקסט (ההפך של DbExport), העתקה מאופן
// לאופן (בין שני מסדים), ותפריט פקודות מרשימה במסד. הכול על Store, בלי ממשק — נבדק במחשב.
// אחראי: הלוגיקה. אסור לו: לפתוח מסכים.

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class DbMore {

    private DbMore() { }

    // ---------- מיון (פתח רשימה … מיין:) ----------

    /** סדר השורות לפי מיון: שם · גודל · תאריך (קבצים); ריק / לא מוכר — כמו ברשימה. רשימות לפני קבצים במיון. */
    static void sort(final Store s, List<Store.Member> rows, String how) {
        final String h = how == null ? "" : how.trim();
        if (!h.equals(Sort.NAME) && !h.equals(Sort.SIZE) && !h.equals(Sort.DATE)) return;
        Collections.sort(rows, new Comparator<Store.Member>() {
            @Override
            public int compare(Store.Member x, Store.Member y) {
                boolean fx = x.item.type == Store.FILE, fy = y.item.type == Store.FILE;
                if (fx != fy) return fx ? 1 : -1;
                if (fx && !h.equals(Sort.NAME)) {
                    Store.FileInfo a = s.file(x.item.id), b = s.file(y.item.id);
                    long va = a == null ? 0 : h.equals(Sort.SIZE) ? a.size : a.mtime, vb = b == null ? 0 : h.equals(Sort.SIZE) ? b.size : b.mtime;
                    if (va != vb) return va > vb ? -1 : 1;                   // הגדול / החדש ראשון
                }
                return x.item.name.compareToIgnoreCase(y.item.name);
            }
        });
    }

    // ---------- סידור ידני ----------

    static final int UP = 0, DOWN = 1, TOP = 2, BOTTOM = 3;

    /** מזיז את הפריטים שנבחרו ברשימה: מקום אחד למעלה/למטה, או לראש/לסוף (שומר על הסדר ביניהם). בעסקה אחת. */
    static void order(Store s, final long list, final List<Long> ids, final int how) {
        s.atomic(new Store.Work() {
            @Override
            public void run(Store t) {
                List<Long> now = new ArrayList<Long>();
                for (Store.Member m : t.members(list)) now.add(m.item.id);
                List<Long> sel = new ArrayList<Long>();
                for (Long id : now) if (ids.contains(id)) sel.add(id);          // לפי הסדר ברשימה
                if (sel.isEmpty()) return;
                if (how == TOP) for (int i = 0; i < sel.size(); i++) t.move(list, sel.get(i), i);
                else if (how == BOTTOM) for (Long id : sel) t.move(list, id, now.size());
                else if (how == UP) {
                    for (Long id : sel) {
                        List<Long> cur = ids(t, list);
                        int p = cur.indexOf(id);
                        if (p > 0 && !sel.contains(cur.get(p - 1))) t.move(list, id, p - 1);
                    }
                } else {
                    for (int i = sel.size() - 1; i >= 0; i--) {
                        List<Long> cur = ids(t, list);
                        int p = cur.indexOf(sel.get(i));
                        if (p >= 0 && p < cur.size() - 1 && !sel.contains(cur.get(p + 1))) t.move(list, sel.get(i), p + 1);
                    }
                }
            }
        }, true);
    }

    private static List<Long> ids(Store t, long list) {
        List<Long> out = new ArrayList<Long>();
        for (Store.Member m : t.members(list)) out.add(m.item.id);
        return out;
    }

    // ---------- ייבוא מטקסט ----------

    /** מה נכנס. */
    static final class Imported {
        int lists, files, bad;
    }

    /**
     * ההפך של DbExport: "#N א / ב" — רשימה (בתוך into, בדרך שבכותרת; קיימת — אותה), @צלמית/@תיאור/@למה/@עיצוב —
     * מאפיינים, "/מסלול | גודל" — קובץ. שורות לפני הכותרת הראשונה (קובץ "קבצי רשימה") — הקבצים נכנסים ל-into. בעסקה אחת.
     */
    static Imported importText(Store s, final long into, String text) {
        final Imported r = new Imported();
        final String[] lines = (text == null ? "" : text).split("\r\n|\r|\n");
        s.atomic(new Store.Work() {
            @Override
            public void run(Store t) {
                long cur = into;
                boolean header = false;
                Set<Long> counted = new HashSet<Long>();
                for (String raw : lines) {
                    String line = raw.trim();
                    if (line.isEmpty()) continue;
                    if (line.startsWith("#")) {
                        int i = 1;
                        while (i < line.length() && Character.isDigit(line.charAt(i))) i++;
                        List<String> parts = new ArrayList<String>();
                        for (String p : line.substring(i).split("/")) if (!p.trim().isEmpty()) parts.add(Tree.cleanName(p.trim()));
                        if (parts.isEmpty()) { r.bad++; continue; }
                        long at = into;
                        for (String p : parts) {
                            long c = 0;
                            for (Store.Member m : t.members(at)) if (m.item.type == Store.LIST && m.item.name.equals(p)) c = m.item.id;
                            if (c == 0) c = t.createList(p, at);
                            at = c;
                        }
                        if (counted.add(at)) r.lists++;
                        cur = at;
                        header = true;
                        continue;
                    }
                    if (line.startsWith("@")) {
                        if (!header) continue;                                  // השורה הראשונה (@תיאור של השורש) — לא מאפיין
                        String key = line.startsWith(Tree.ICON_LINE.trim()) ? Store.P_ICON : line.startsWith(Tree.DESC_LINE.trim()) ? Store.P_DESC
                                : line.startsWith(Tree.WHY_LINE.trim()) ? Store.P_WHY : line.startsWith(Tree.DESIGN_LINE.trim()) ? Store.P_DESIGN : null;
                        if (key != null) {
                            int sp = line.indexOf(' ');
                            t.setProp(cur, key, sp < 0 ? "" : line.substring(sp + 1).trim());
                        }
                        continue;
                    }
                    int bar = line.lastIndexOf(" | ");
                    String path = bar >= 0 ? line.substring(0, bar).trim() : line;
                    long size = -1;
                    if (bar >= 0) try { size = Long.parseLong(line.substring(bar + 3).trim()); } catch (NumberFormatException ignored) { }
                    if (!path.startsWith("/")) { r.bad++; continue; }
                    long f = t.findFile(path);
                    if (f == 0) f = t.createFile(new Store.FileInfo(path, Importer.driveOf(path), size, 0, null));
                    if (t.addMember(cur, f)) r.files++;
                }
            }
        }, true);
        return r;
    }

    // ---------- העתקה מאופן לאופן ----------

    /**
     * מ"src" (מסד אחר) לתוך dest שב-dst: עותק עצמאי של הרשימה ושל מה שבתוכה — רשימות, חכמות (ההגדרה), רעיונות וכותרות;
     * קובץ — לפי המסלול (קיים ב-dst — אותו פריט; אחרת — חדש, עם החתימה שהייתה). רשימה שמופיעה פעמיים — פעם אחת.
     */
    static int copyAcross(final Store src, final long srcId, Store dst, final long dest) {
        final int[] n = {0};
        dst.atomic(new Store.Work() {
            @Override
            public void run(Store t) {
                n[0] = across(src, srcId, t, dest, new HashMap<Long, Long>());
            }
        }, true);
        return n[0];
    }

    private static int across(Store src, long id, Store t, long into, Map<Long, Long> done) {
        Store.Item it = src.item(id);
        if (it == null) return 0;
        if (it.type == Store.LIST) {
            Long had = done.get(id);
            if (had != null) {
                if (!t.makesCycle(into, had)) t.addMember(into, had);
                return 0;
            }
            long nid = t.createList(it.name, into);
            done.put(id, nid);
            for (String k : new String[]{Store.P_ICON, Store.P_DESC, Store.P_DESIGN, Store.P_WHY, Store.P_GROUP}) copyProp(src, id, t, nid, k);
            int c = 1;
            for (Store.Member m : src.members(id)) c += across(src, m.item.id, t, nid, done);
            return c;
        }
        if (it.type == Store.SMART) {
            long nid = t.createSmart(it.name, into);
            for (String k : new String[]{Store.P_COND, Store.P_SORT, Store.P_DIR, Store.P_VIEW, Store.P_ICON, Store.P_DESC, Store.P_WHY})
                copyProp(src, id, t, nid, k);
            return 1;
        }
        if (it.type == Store.NOTE) { t.addNote(into, it.name); return 1; }
        if (it.type == Store.TITLE) { t.addTitle(into, it.name); return 1; }
        Store.FileInfo fi = src.file(id);
        if (fi == null) return 0;
        long f = t.findFile(fi.path);
        if (f == 0) f = t.createFile(fi);
        return t.addMember(into, f) ? 1 : 0;
    }

    private static void copyProp(Store src, long from, Store t, long to, String k) {
        String v = src.prop(from, k);
        if (v != null && !v.isEmpty()) t.setProp(to, k, v);
    }

    // ---------- תפריט פקודות מרשימה במסד ----------

    /** שורה בתפריט: קובץ פקודות, תת־תפריט (רשימה), או חכמה (מה שהיא מוצאת). */
    static final class Entry {
        final int kind;
        final String label, path, cond;
        final long id;

        Entry(int kind, String label, long id, String path, String cond) {
            this.kind = kind;
            this.label = label;
            this.id = id;
            this.path = path;
            this.cond = cond;
        }
    }

    static final int E_FILE = 0, E_LIST = 1, E_SMART = 2;

    /** מה שבתפריט — לפי הסדר ברשימה: קובצי פקודות (vbat), תתי־רשימות, חכמות. השאר — לא בתפריט. */
    static List<Entry> menu(Store s, long list) {
        List<Entry> out = new ArrayList<Entry>();
        for (Store.Member m : s.members(list)) {
            Store.Item it = m.item;
            if (it.type == Store.FILE) {
                Store.FileInfo fi = s.file(it.id);
                if (fi != null && Batch.isBatch(fi.path)) out.add(new Entry(E_FILE, it.name, it.id, fi.path, null));
            } else if (it.type == Store.LIST) out.add(new Entry(E_LIST, it.name, it.id, null, null));
            else if (it.type == Store.SMART) out.add(new Entry(E_SMART, it.name, it.id, null, s.prop(it.id, Store.P_COND)));
        }
        return out;
    }
}
