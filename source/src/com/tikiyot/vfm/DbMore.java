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
        int lists, files, bad, smarts;
    }

    /**
     * ההפך של DbExport (פורמט 1 ו־2): "#N א / ב" — רשימה (בתוך into, בדרך שבכותרת; קיימת — אותה), @צלמית/@תיאור/@למה/@עיצוב
     * — מאפיינים, "/מסלול | גודל" — קובץ; ומפורמט 2: @תת-רשימה, @קישור, @חכמה (ואחריה שורות "+…"), @כותרת, @רעיון, @חבורה.
     * שורות לפני הכותרת הראשונה — נכנסות ל-into. רשימה חכמה באותו שם באותה רשימה — אותה (ההגדרות מתעדכנות). בעסקה אחת.
     */
    static Imported importText(Store s, final long into, String text) {
        final Imported r = new Imported();
        final String[] lines = (text == null ? "" : text).split("\r\n|\r|\n");
        s.atomic(new Store.Work() {
            @Override
            public void run(Store t) {
                long cur = into, smart = 0;
                boolean header = false;
                Set<Long> counted = new HashSet<Long>();
                for (String raw : lines) {
                    String line = raw.trim();
                    if (line.isEmpty()) continue;
                    if (line.startsWith("#")) {
                        int i = 1;
                        while (i < line.length() && Character.isDigit(line.charAt(i))) i++;
                        long at = chain(t, into, line.substring(i), true);
                        if (at == 0) { r.bad++; continue; }
                        if (counted.add(at)) r.lists++;
                        cur = at;
                        smart = 0;
                        header = true;
                        continue;
                    }
                    if (line.startsWith("+")) {                                 // מאפיין של הרשימה החכמה שמעל
                        if (smart == 0) continue;
                        for (String[] k : DbExport.SMART_PROPS)
                            if (line.startsWith(k[0].trim())) t.setProp(smart, k[1], line.substring(k[0].trim().length()).trim());
                        continue;
                    }
                    smart = 0;
                    if (line.startsWith("@")) {
                        String rest = line.indexOf(' ') < 0 ? "" : line.substring(line.indexOf(' ') + 1).trim();
                        if (line.startsWith(DbExport.SUB.trim() + " ")) {
                            if (cur != 0 && !rest.isEmpty() && child(t, cur, Names.cleanName(rest), Store.LIST, true) == 0) r.bad++;
                        } else if (line.startsWith(DbExport.LINK.trim() + " ")) {
                            long target = chain(t, into, rest, false);
                            if (target == 0 || cur == 0 || t.makesCycle(cur, target)) r.bad++;
                            else t.addMember(cur, target);
                        } else if (line.startsWith(DbExport.SMART.trim() + " ")) {
                            String name = rest.isEmpty() ? Words.SMART_LIST : rest;
                            smart = cur == 0 ? 0 : child(t, cur, name, Store.SMART, false);
                            if (smart == 0) smart = t.createSmart(name, cur);
                            r.smarts++;
                        } else if (line.startsWith(DbExport.TITLE.trim() + " ")) {
                            if (cur != 0) t.addTitle(cur, rest);
                        } else if (line.startsWith(DbExport.NOTE.trim() + " ")) {
                            if (cur != 0) t.addNote(cur, rest.replace(" ⏎ ", "\n"));
                        } else if (line.equals(DbExport.GROUP)) {
                            if (header) t.setProp(cur, Store.P_GROUP, "1");
                        } else if (header) {                                    // לפני הכותרת הראשונה (@תיאור של הקובץ) — לא מאפיין
                            String key = line.startsWith(DbExport.ICON.trim() + " ") ? Store.P_ICON : line.startsWith(DbExport.DESC.trim() + " ") ? Store.P_DESC
                                    : line.startsWith(DbExport.WHY.trim() + " ") ? Store.P_WHY : line.startsWith(DbExport.DESIGN.trim() + " ") ? Store.P_DESIGN : null;
                            if (key != null) t.setProp(cur, key, rest);
                        }
                        continue;
                    }
                    int bar = line.lastIndexOf(" | ");
                    String path = bar >= 0 ? line.substring(0, bar).trim() : line;
                    long size = -1;
                    if (bar >= 0) try { size = Long.parseLong(line.substring(bar + 3).trim()); } catch (NumberFormatException ignored) { }
                    if (!path.startsWith("/") || cur == 0) { r.bad++; continue; }
                    long f = t.findFile(path);
                    if (f == 0) f = t.createFile(new Store.FileInfo(path, DbPaths.driveOf(path), size, 0, null));
                    if (t.addMember(cur, f)) r.files++;
                }
            }
        }, true);
        return r;
    }

    /** הרשימה בדרך "א / ב" מתוך from (0 — עומדות לעצמן); create — מה שחסר נוצר. 0 — אין (או דרך ריקה). */
    private static long chain(Store t, long from, String way, boolean create) {
        long at = from;
        boolean any = false;
        for (String p : way.split("/")) {
            if (p.trim().isEmpty()) continue;
            any = true;
            long c = child(t, at, Names.cleanName(p.trim()), Store.LIST, create);
            if (c == 0) return 0;
            at = c;
        }
        return any ? at : 0;
    }

    /** החבר בשם הזה ומהסוג הזה ב־in (0 — בין אלה שעומדות לעצמן); create (רק רשימה) — נוצר אם אין. */
    private static long child(Store t, long in, String name, int type, boolean create) {
        if (in != 0) {
            for (Store.Member m : t.members(in)) if (m.item.type == type && m.item.name.equals(name)) return m.item.id;
        } else if (type == Store.LIST) {
            for (Store.Item it : t.lists()) if (it.name.equals(name) && t.listsOf(it.id).isEmpty()) return it.id;
        }
        return create && type == Store.LIST ? t.createList(name, in) : 0;
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

    // ================= מ0.23: תיקייה מהכונן לרשימה ("לרשימה" במסכי הקבצים) =================

    /** שם פנוי בין הרשימות (מאותו סוג) שבתוך list: "שם", ואם תפוס — "שם (2)"… */
    static String freeName(Store s, long list, int type, String name) {
        String base = type == Store.SMART ? name.trim() : Names.cleanName(name);
        if (base.isEmpty()) base = "?";
        java.util.Set<String> taken = new java.util.HashSet<String>();
        for (Store.Member m : s.members(list)) if (m.item.type == type) taken.add(m.item.name);
        String n = base;
        for (int k = 2; taken.contains(n); k++) n = base + " (" + k + ")";
        return n;
    }

    /**
     * תיקייה חיה: רשימה חכמה בתוך list שמוצאת את מה שבתיקייה (התנאי, המיון והתצוגה — כמו בישן).
     * אותה תיקייה כבר שם (אותו תנאי) — בלי כפולה, 0. אחרת — מספר החכמה החדשה.
     */
    static long liveFolder(Store s, final long list, String folder) {
        final java.util.Map<String, String> kv = new java.util.LinkedHashMap<String, String>();   // מ0.29: מה שהיה Unify.folderKeys
        kv.put(SmartLists.COND, Cond.SOURCE + ":" + Cond.quote(folder));
        kv.put(SmartLists.SORT, Sort.NAME);
        kv.put(SmartLists.VIEW, FolderView.ALL);
        final String cond = kv.get(SmartLists.COND);
        for (Store.Member m : s.members(list))
            if (m.item.type == Store.SMART && cond.equals(s.prop(m.item.id, Store.P_COND))) return 0;
        final String name = freeName(s, list, Store.SMART, new java.io.File(folder).getName());
        final long[] out = {0};
        s.atomic(new Store.Work() {
            @Override
            public void run(Store t) {
                long id = t.createSmart(name, list);
                t.setProp(id, Store.P_COND, cond);
                t.setProp(id, Store.P_SORT, kv.get(SmartLists.SORT));
                t.setProp(id, Store.P_VIEW, kv.get(SmartLists.VIEW));
                out[0] = id;
            }
        }, true);
        return out[0];
    }

    /**
     * תיקייה שנסרקה (Tree.scanFolder — צומת מנותק, לא נשמר) לתוך list. structure — רשימה בשם התיקייה (שם פנוי), ובתוכה
     * התת־תיקיות כרשימות והקבצים; אחרת — רק הקבצים, ישר לתוך list. הקבצים — DbFiles.add (אותה ליבה). מחזיר [רשימות, פריטים].
     */
    static int[] attach(Store s, long list, Tree.Node scanned, boolean structure, boolean sign) {
        int[] c = {0, 0};
        if (structure) {
            long sub = DbListCmd.newList(s, list, freeName(s, list, Store.LIST, scanned.name));
            c[0]++;
            graft(s, sub, scanned, sign, c, 0);
        } else {
            c[1] += files(s, list, scanned, sign);
        }
        return c;
    }

    private static void graft(Store s, long into, Tree.Node n, boolean sign, int[] c, int depth) {
        c[1] += files(s, into, n, sign);
        if (depth > 60) return;
        for (Tree.Node ch : n.children) {
            long sub = DbListCmd.newList(s, into, freeName(s, into, Store.LIST, ch.name));
            c[0]++;
            graft(s, sub, ch, sign, c, depth + 1);
        }
    }

    private static int files(Store s, long list, Tree.Node n, boolean sign) {
        if (n.entries.isEmpty()) return 0;
        List<String> ps = new ArrayList<String>();
        List<Long> sz = new ArrayList<Long>();
        for (Tree.Entry e : n.entries) {
            ps.add(e.path);
            sz.add(e.size);
        }
        return DbFiles.add(s, list, ps, sz, sign).added;
    }
}
