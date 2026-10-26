package com.tikiyot.vfm;

// עץ הרשימות. פעולות כאן משנות רשימות בלבד, אלא אם נכתב אחרת. השלד — ListScreen (173).

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public class VirtualScreen extends ListScreen<VirtualScreen.Item> implements TopRow.Host {

    public static final String EXTRA_BASE = "base";      // הרשימה של המופע: אי אפשר לעלות מעליה
    /** 1.13.0: "פתח רשימה … מיון:<שם>" — המיון לפתיחה הזו (לרשימה שנפתחה בלבד; לא נשמר). */
    public static final String EXTRA_SORT = "sort";
    /** 1.13.0: "למה" של הכלי שפתח את הרשימה — מוצג בראש הרשימה שלו, כשלרשימה עצמה אין "למה". */
    public static final String EXTRA_WHY = "why";
    private String sortOnce;
    private ArrayList<String> sortOncePath;
    /** 1.13.0: שורת "למה" של הרשימה. */
    private TextView whyBar;

    private ArrayList<String> base = new ArrayList<String>();

    /**
     * 1.13.1 (באג): פותח רשימה כ"רשימה של המופע" — החזרה ממנה יוצאת למסך שקרא, ולא עולה להורים שלה עד השורש.
     * כל מי שפותח רשימה (כלי, פקודה, הפניה, חיפוש) — דרך כאן. sort / why — אפשר null.
     */
    static void open(android.app.Activity a, List<String> list, String sort, String why) {
        ArrayList<String> p = new ArrayList<String>(list);
        Intent i = new Intent(a, VirtualScreen.class).putStringArrayListExtra(EXTRA_BASE, p);
        if (sort != null && !sort.isEmpty()) i.putExtra(EXTRA_SORT, sort);
        if (why != null) i.putExtra(EXTRA_WHY, why);
        a.startActivity(i);
    }

    private static final int FOLDER = 0, FILE = 1, BAD = 2, SMART = 3, LIVE = 4, REF = 5;   // 161: רשימה חכמה בתוך הרשימה · 1.9.0: תיקייה חיה · 1.13.0: הפניה

    static final class Item {
        int type;
        String label, sub;
        String folder;
        /** 1.11.0: צלמית של רשימה ידנית ("" — הרגילה). */
        String icon = "";
        Tree.Entry entry;
        /** 161: רשימה חכמה בתוך הרשימה. */
        Nested.Item smart;
        /** 1.9.0: תיקייה חיה — המסלול שלה בכונן. */
        String live;
        /** 1.13.0: הפניה לרשימה אחרת — המסלול שלה. */
        ArrayList<String> ref;
        String refText;
        /** מצב הפריט — מחושב במודל הפריט בלבד (80). */
        FileItem.State state = FileItem.State.OK;
        long mtime;
        /** המקום ברשימה ("הסדר שלי"). */
        int order;

        boolean missing() {
            return !state.present();
        }
    }

    private ArrayList<String> path = new ArrayList<String>();
    /** השורות (השלד — rows). */
    private final List<Item> items = rows;
    private TextView empty;
    private java.util.Set<String> selectedLists;
    private TextView crumb, stats;
    /** "⚠ חסרים" בשורת הסיכום מקפיץ אותם לראש (79: סינון, לא מיון). */
    private boolean missingFirst;
    private int missingCount;

    /** פריט ברשימה בעיני המיון (79). "הסדר שלי" — המקום שלו ברשימה. */
    private static final Sort.Key<Item> ITEM = new Sort.Key<Item>() {
        @Override public String name(Item i) { return i.label; }
        @Override public String folder(Item i) { return i.entry == null ? "" : parent(i.entry.path); }
        @Override public long size(Item i) { return i.entry == null ? 0 : i.entry.size; }
        @Override public long mtime(Item i) { return i.mtime; }
        @Override public long added(Item i) { return 0; }
        @Override public boolean dir(Item i) { return false; }
        @Override public long special(Item i) { return i.order; }
        @Override public int section(Item i) { return 0; }
    };
    private java.util.Set<String> selected;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        if (getIntent() != null && getIntent().getStringArrayListExtra(EXTRA_BASE) != null)
            base = getIntent().getStringArrayListExtra(EXTRA_BASE);
        if (saved != null && saved.getStringArrayList("path") != null)
            path = saved.getStringArrayList("path");
        else
            path = new ArrayList<String>(base);
        if (getIntent() != null && getIntent().getStringExtra(EXTRA_SORT) != null) {   // 1.13.0
            sortOnce = getIntent().getStringExtra(EXTRA_SORT);
            sortOncePath = new ArrayList<String>(path);
        }

        LinearLayout root = build(saved, Words.MANUAL_LISTS, Words.Manual.MANUAL_LIST);
        stats = info;
        crumb = Ui.info(this);
        crumb.setTextColor(Ui.TEXT);
        crumb.setBackgroundColor(Ui.HEADER);
        crumb.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { jumpUp(); }
        });
        root.addView(crumb, 0);
        stats.setOnClickListener(new View.OnClickListener() {       // נגיעה על "⚠ חסרים" — לראש, ושוב — חזרה
            @Override
            public void onClick(View v) {
                if (missingCount == 0 && !missingFirst) return;
                missingFirst = !missingFirst;
                refresh();
            }
        });
        empty = new TextView(this);
        empty.setTextColor(Ui.SUB);
        empty.setGravity(android.view.Gravity.CENTER);
        int p = Ui.dp(this, 24);
        empty.setPadding(p, p, p, p);
        root.addView(empty, 2);
        whyBar = Why.bar(this);                                               // 1.13.0
        root.addView(whyBar, 1);

        selected = sel.files;
        selectedLists = sel.lists;
        java.util.Map<Actions.Action, Runnable> own = new java.util.HashMap<Actions.Action, Runnable>();
        own.put(Actions.MOVE_TO_LIST, new Runnable() {
            @Override public void run() { transferSelected(true, false); }
        });
        own.put(Actions.ALSO_TO_LIST, new Runnable() {
            @Override public void run() { transferSelected(false, false); }
        });
        own.put(Actions.DUPLICATE_LIST, new Runnable() {                      // 1.14.0
            @Override public void run() { transferSelected(false, true); }
        });
        own.put(Actions.REMOVE_FROM_LIST, new Runnable() {
            @Override public void run() { removeSelected(); }
        });
        own.put(Actions.SETTINGS_LIST, new Runnable() {                       // 1.16.0: הגדרות אחת
            @Override
            public void run() {
                String n = selectedLists.iterator().next();
                stopSelecting();
                if (n.startsWith(LINE)) {                                     // 1.17.1: קישור — הרשימה המקורית; חכמה — שלה
                    String v = n.substring(2);
                    if (n.charAt(1) == 'r') settings(Tree.parsePath(v), Words.VClipW.ORIGINAL);
                    else if (n.charAt(1) == 's') smartSettings(Nested.resolve(v));
                    else Ui.toast(VirtualScreen.this, Words.Live.NO_SETTINGS);
                    return;
                }
                ArrayList<String> p = new ArrayList<String>(path);
                p.add(n);
                settings(p);
            }
        });
        own.put(Actions.ORDER_LIST, new Runnable() {                          // 1.13.0: הזזה ידנית
            @Override public void run() { orderSelected(); }
        });
        pasteBar = new PasteBar(this, new PasteBar.Place() {                    // 142 · 1.14.0: הלוח האחד
            @Override public String cannot() { return VClip.cannotPasteInto(path); }
            @Override public VClip.Result paste() { return VClip.pasteInto(path); }
        }, new Runnable() {
            @Override public void run() { refresh(); }
        });
        root.addView(pasteBar.view);
        done(root, Actions.Tool.LIST, own);

    }

    private Button button(String label, final Runnable action) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { action.run(); }
        });
        return b;
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putStringArrayList("path", path);
    }

    @Override
    public void onBackPressed() {
        if (sel.back()) return;
        if (path.size() > base.size()) {
            path.remove(path.size() - 1);
            refresh();
        } else {
            super.onBackPressed();
        }
    }

    // ---------- מה שהשלד שואל ----------

    @Override
    protected String key(Item it) {
        return it.entry != null && !it.entry.bad ? it.entry.path : null;
    }

    @Override
    protected String listKey(Item it) {
        if (it.type == FOLDER) return it.folder;
        return it.type == REF || it.type == SMART || it.type == LIVE ? lineKey(it) : null;   // 1.17.1
    }

    /**
     * 1.17.1: בחירה מרובה — גם קישור ↪, רשימה חכמה ותיקייה מתעדכנת (המילון: "אפשרות להמשיך לבחור, ואז להפעיל תפריט
     * שיתאים לבחירה של כמה פריטים"). המפתח שלהן בבחירה — עם סימן שלא יכול להיות בשם רשימה.
     */
    static final String LINE = "\u0001";

    private static String lineKey(Item it) {
        return LINE + (it.type == REF ? "r" + it.refText : it.type == SMART ? "s" + it.smart.ref : "l" + it.live);
    }

    /** פריט בלוח מתוך מפתח של שורה; label — מה שמוצג. */
    private VClip.Item lineItem(String key) {
        char k = key.charAt(1);
        String v = key.substring(2), label = v;
        for (Item it : items) if (listKey(it) != null && listKey(it).equals(key)) label = it.label;
        return new VClip.Item(k == 'r' ? VClip.REF : k == 's' ? VClip.SMART : VClip.LIVE, v, label);
    }

    @Override
    protected void bind(View v, Item it) {
        // 1.17.0: הסוג — תווית תמיד; הצלמית של המשתמש — בנוסף (המילון, 24; עד כאן הצלמית החליפה את הסוג)
        if (it.type == FOLDER) {
            Ui.bindIcon(v, "", it.label, it.sub);
            String[] own = ownLook(it);
            Ui.kind(v, Ui.MANUAL, own != null && !own[0].isEmpty() ? own[0] : it.icon);
        } else if (it.type == SMART) {                                        // 161
            Ui.bindIcon(v, "", it.label, it.sub);
            Ui.kind(v, it.smart.local || it.smart.gone ? Ui.SMART : Ui.LINK,
                    !it.smart.icon.isEmpty() ? it.smart.icon : it.smart.dup != null ? Func.iconOf(FnDup.KIND) : "");
        } else if (it.type == LIVE) {                                         // 1.9.0
            Ui.bindIcon(v, "", it.label, it.sub);
            Ui.kind(v, Ui.LIVE, "");
            if (it.missing()) ((TextView[]) v.getTag())[1].setTextColor(Ui.MUTED);
        } else if (it.type == REF) {                                          // 1.13.0
            Ui.bindIcon(v, "", it.label, it.sub);
            Ui.kind(v, Ui.LINK, it.icon);
            if (it.missing()) ((TextView[]) v.getTag())[1].setTextColor(Ui.MUTED);
        } else if (it.type == FILE) {
            Ui.bindRow(this, v, it.label, false, it.entry.size, it.mtime,
                    it.entry.path, it.missing() ? it.state.note : null, null);
            if (it.missing()) {
                TextView[] tv = (TextView[]) v.getTag();
                tv[0].setText(it.state.icon);
                tv[0].setBackgroundColor(0x00000000);
                tv[1].setTextColor(Ui.MUTED);
            }
        } else {
            Ui.bindIcon(v, "❗", it.label, Words.Manual.BAD_LINE);
        }
    }

    @Override
    protected void open(Item it) {
        click(it);
    }

    @Override
    protected void tap(Item it) {
        click(it);
    }

    @Override
    protected void hold(Item it) {
        longClick(it);
    }

    /** מפתח המיון בהגדרות הכלליות: מסלול הרשימה. */
    private String sortKey() {
        return path.isEmpty() ? Words.ROOT : Tree.key(path);
    }

    private Sort sort() {
        Sort s = Sort.of(Sort.MINE, false, Sort.list(sortKey()));
        if (sortOnce != null && path.equals(sortOncePath)) {                  // 1.13.0: מיון מהפקודה — לפתיחה הזו
            String[] p = sortOnce.trim().split("\\s+");
            String by = sortOnce.trim();
            boolean desc = false;
            if (p.length > 1 && (p[p.length - 1].equals(Words.Manual.DESC_WORD) || p[p.length - 1].equals(Words.Manual.ASC_WORD))) {
                desc = p[p.length - 1].equals(Words.Manual.DESC_WORD);
                by = sortOnce.trim().substring(0, sortOnce.trim().lastIndexOf(' ')).trim();
            }
            if (by.equals(Sort.MINE) || java.util.Arrays.asList(Sort.BASE).contains(by)) {
                s.by = by;
                s.desc = desc;
            }
        }
        return s;
    }

    private PasteBar pasteBar;

    @Override
    protected void refresh() {
        if (pasteBar != null) pasteBar.refresh();
        items.clear();
        Tree t = Tree.load();
        if (t.unreadable) Ui.toast(this, Words.Manual.CANT_READ_TREE);
        Tree.Node node = t.find(path);
        while (node == null && path.size() > base.size()) {
            path.remove(path.size() - 1);
            node = t.find(path);
        }
        if (node == null) node = base.isEmpty() ? t.root : new Tree.Node();  // 1.13.1 (באג): הרשימה נמחקה — ריק, לא כל העץ
        setTitle(path.isEmpty() ? Words.MANUAL_LISTS : path.get(path.size() - 1));   // 1.13.0: בלי "· רשימה" הכפול
        String toolWhy = getIntent() != null ? getIntent().getStringExtra(EXTRA_WHY) : null;
        Why.show(whyBar, Words.WhyW.KEY_LIST + sortKey(),
                !node.why.isEmpty() || toolWhy == null || !path.equals(base) ? node.why : toolWhy);

        StringBuilder cr = new StringBuilder("📋 " + (base.isEmpty() ? Words.MANUAL_LISTS : base.get(base.size() - 1)));
        for (int i = base.size(); i < path.size(); i++) cr.append("  ›  ").append(path.get(i));
        Design look = Design.forList(t, path);                                // 1.12.0: העיצוב של הרשימה (שלה, או בירושה)
        String ownIcon = path.isEmpty() ? node.icon
                : Design.forList(t, path.subList(0, path.size() - 1)).iconOf(path, node.icon);   // הצלמית — מהעיצוב של מקומה
        if (!node.desc.isEmpty()) cr.append("\n").append(ownIcon.isEmpty() || ownIcon.contains(".") ? "" : ownIcon + " ").append(node.desc);   // 1.11.0
        crumb.setText(cr);
        Looks.text(crumb, look.title, look.title.size);
        crumb.setVisibility(path.size() > base.size() || !node.desc.isEmpty() ? View.VISIBLE : View.GONE);

        boolean useIndex = lib.ready && !lib.busy;
        ListCount.FromLibrary ix = new ListCount.FromLibrary(lib);           // 1.14.0: ספירה אחת לכל המקומות
        for (Tree.Node c : node.children) {
            Item it = new Item();
            it.type = FOLDER;
            it.folder = c.name;
            it.label = c.name;
            it.sub = ListCount.of(t, c, ix).text();                           // 161 · 1.13.0 · 1.14.0: הכול, בכל העומק
            it.icon = c.icon;                                                 // 1.11.0
            if (!c.desc.isEmpty()) it.sub = c.desc + "\n" + it.sub;
            items.add(it);
        }
        for (String ref : node.smart) {                                       // 161: רשימות חכמות בתוך הרשימה
            Item it = new Item();
            it.type = SMART;
            it.smart = Nested.resolve(ref);
            it.label = it.smart.gone ? ref : it.smart.name;
            long[] nh = it.smart.gone || it.smart.dup != null ? null : ix.smart(ref);
            it.sub = it.smart.gone ? Words.Inner.GONE                         // 1.13.0: נתון חי — מספר וגודל
                    : it.smart.dup != null ? Func.byKind(FnDup.KIND).title  // 1.15.1: כפולים — נספרים רק בפתיחה
                    : nh == null ? Words.LOADING_INDEX : Words.files(nh[0]) + (nh[1] > 0 ? " · " + Files.size(nh[1]) : "");
            if (!it.smart.gone && !it.smart.local) it.sub = Words.Manual.REF + it.sub;   // 1.16.0: קישור — עם ↪, כמו קישור לרשימה
            items.add(it);
        }
        for (String lp : node.live) {                                        // 1.9.0: תיקיות חיות
            Item it = new Item();
            it.type = LIVE;
            it.live = lp;
            File lf = new File(lp);
            it.label = lf.getName();
            if (!lf.isDirectory()) {
                it.state = FileItem.State.MISSING;
                it.sub = Words.Live.GONE + " · " + lp;
            } else if (lib.ready) {                                           // 1.13.0: נתון חי מהאינדקס
                long[] ts = ix.live(lp);
                it.sub = Words.files(ts[0]) + (ts[1] > 0 ? " · " + Files.size(ts[1]) : "");
            } else {
                it.sub = Words.Live.UPDATING + " · " + lp;
            }
            items.add(it);
        }
        for (String rp : node.refs) {                                         // 1.13.0: הפניות לרשימות אחרות
            Item it = new Item();
            it.type = REF;
            it.refText = rp;
            it.ref = Tree.parsePath(rp);
            Tree.Node target = it.ref.isEmpty() ? null : t.find(it.ref);
            it.label = it.ref.isEmpty() ? rp : it.ref.get(it.ref.size() - 1);
            if (target == null) {
                it.state = FileItem.State.MISSING;
                it.sub = Words.Manual.REF_GONE + rp;
            } else {
                it.icon = Design.forList(t, it.ref.subList(0, it.ref.size() - 1)).iconOf(it.ref, target.icon);
                if (it.icon.contains(".")) it.icon = "";
                it.sub = Words.Manual.REF + rp + " · " + ListCount.of(t, target, ix).text();
            }
            items.add(it);
        }
        int missing = 0, deleted = 0, offline = 0;
        long total = 0, newest = 0;
        List<Item> files = new ArrayList<Item>();
        for (Tree.Entry e : node.entries) {
            Item it = new Item();
            it.entry = e;
            it.order = files.size();
            if (e.bad) {
                it.type = BAD;
                it.label = e.path;
            } else {
                it.type = FILE;
                it.label = e.name();
                FileItem fi = FileItem.of(lib, e.path, e.size, useIndex);
                it.state = fi.state;
                it.mtime = fi.state.present() ? fi.mtime : 0;
                if (it.state == FileItem.State.OFFLINE) offline++;
                else if (it.state == FileItem.State.DELETED) deleted++;
                else if (it.missing()) missing++;
                if (e.size > 0) total += e.size;
                if (it.mtime > newest) newest = it.mtime;
                it.sub = it.missing() ? it.state.line
                        : (e.size > 0 ? Files.size(e.size) + " · " : "") + e.path;
            }
            files.add(it);
        }
        srt = sort();
        srt.apply(files, ITEM);
        if (missingFirst) {                                   // יציב: החסרים לראש, בסדר שלהם
            List<Item> gone = new ArrayList<Item>();
            for (Item x : files) if (x.missing()) gone.add(x);
            files.removeAll(gone);
            files.addAll(0, gone);
        }
        items.addAll(files);
        if (Sort.MINE.equals(srt.by) && !node.order.isEmpty()) freeOrder(items, node.order);   // 1.17.2

        StringBuilder st = new StringBuilder();
        int lists = node.children.size() + node.smart.size() + node.live.size() + node.refs.size();   // 161: גם החכמות שבתוכה · 1.9.0: והחיות · 1.13.0: וההפניות
        if (lists > 0) st.append(lists).append(Words.Manual.LISTS_SEP);
        st.append(node.entries.size()).append(Words.Manual.ITEMS_SUFFIX);
        if (total > 0) st.append("   ·   ").append(Files.size(total));
        StringBuilder st2 = new StringBuilder();
        if (newest > 0) st2.append(Words.Manual.NEW_ONE).append(Files.when(newest));
        if (missing > 0) st2.append(st2.length() > 0 ? "   ·   " : "").append(FileItem.State.MISSING.count(missing))
                .append(missingFirst ? Words.Manual.UP : "");
        if (offline > 0) st2.append(st2.length() > 0 ? "   ·   " : "").append(FileItem.State.OFFLINE.count(offline));
        if (deleted > 0) st2.append(st2.length() > 0 ? "   ·   " : "").append(FileItem.State.DELETED.count(deleted));
        st2.append(st2.length() > 0 ? "   ·   " : "").append(srt.describe());
        missingCount = missing + deleted + offline;
        stats.setText(st + "\n" + st2);

        empty.setText(path.isEmpty() ? Words.Manual.NO_LISTS_YET
                : Words.Manual.LIST_EMPTY);
        empty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        look(look);                                                           // 1.12.0: שורות או רשת
        adapter.notifyDataSetChanged();
    }

    // ---------- 1.12.0: עיצוב ----------

    @Override
    protected String nameOf(Item it) {
        return it.type == FILE ? Ui.baseName(it.label, false) : it.label;
    }

    @Override
    protected String[] ownLook(Item it) {
        if (it.type != FOLDER || design == null) return null;
        List<String> k = new ArrayList<String>(path);
        k.add(it.folder);
        return new String[]{design.iconOf(k, ""), design.colorOf(k)};
    }

    /** 1.16.0: "הגדרות" של רשימה (שם, צלמית, צבע, תיאור, "למה", עיצוב) — טופס אחד (ListSettings). */
    private void settings(List<String> target) {
        settings(target, Words.VClipW.SETTINGS);
    }

    private void settings(final List<String> target, String title) {
        ListSettings.show(this, target, title, new ListSettings.Done() {
            @Override
            public void saved(List<String> to, String name, String desc) {
                if (target.equals(path) && !to.equals(path)) {                // הרשימה הפתוחה — שם חדש
                    path.clear();
                    path.addAll(to);
                }
                refresh();
            }
        });
    }

    private static String parent(String p) {
        int i = p.lastIndexOf('/');
        return i > 0 ? p.substring(0, i) : p;
    }

    private void jumpUp() {
        final List<String> labels = new ArrayList<String>();
        labels.add("📋 " + (base.isEmpty() ? Words.MANUAL_LISTS : base.get(base.size() - 1)));
        for (int i = base.size(); i < path.size() - 1; i++) labels.add(path.get(i));
        Ui.list(this, Words.Manual.GO_TO_LEVEL, labels, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int which) {
                while (path.size() > base.size() + which) path.remove(path.size() - 1);
                refresh();
            }
        });
    }

    // ---------- נגיעות ----------

    private void click(final Item it) {
        if (sel.on() && (it.type == REF || it.type == LIVE || it.type == SMART)) {   // 1.17.1: גם הן נבחרות
            toggleList(lineKey(it));
            return;
        }
        if (it.type == REF) {                                                 // 1.13.0: הפניה — פותחת את הרשימה במקומה
            if (it.missing()) Ui.toast(this, Words.Manual.REF_GONE + it.refText);
            else open(this, it.ref, null, null);
            return;
        }
        if (it.type == LIVE) {                                                // 1.9.0: ישר מהכונן
            if (it.missing()) Ui.toast(this, Words.Live.GONE);
            else DiskScreen.open(this, it.live);
            return;
        }
        if (it.type == SMART) {                                               // 161
            if (sel.on()) return;
            if (it.smart.gone) Ui.toast(this, Words.Inner.GONE);
            else Nested.open(this, it.smart);                                 // 1.15.1: גם כפולים
            return;
        }
        if (sel.on()) {
            if (it.entry != null) toggle(it.entry.path);
            else if (it.type == FOLDER) toggleList(it.folder);
            return;
        }
        if (it.type == FOLDER) {
            path.add(it.folder);
            refresh();
        } else if (it.type == FILE) {
            if (it.missing()) missing(it); else Ui.open(this, new File(it.entry.path));
        } else {
            Ui.options(this, Words.Manual.BAD_LINE, new String[]{Words.Manual.REMOVE_LINE, Words.Manual.EDIT_BY_HAND, Words.CLOSE},
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface d, int w) {
                            if (w == 0) removeEntry(it);
                            else if (w == 1) Ui.editText(VirtualScreen.this, Paths.tree());
                        }
                    });
        }
    }

    /**
     * הכלל האחיד: נגיעה ארוכה מתחילה בחירה עם הפריט הזה, וכל הפעולות בפס הבחירה. 1.17.1: גם קישור ↪, רשימה חכמה
     * ותיקייה מתעדכנת (עד כאן — חלון לפריט אחד, בלי אפשרות להמשיך לבחור).
     */
    private void longClick(final Item it) {
        if (it.type == REF || it.type == SMART || it.type == LIVE) {
            sel.longPressList(lineKey(it));
            return;
        }
        if (it.type != FOLDER && (it.entry == null || it.entry.bad)) return;
        if (it.type == FOLDER) sel.longPressList(it.folder); else sel.longPress(it.entry.path);   // הכלל האחיד (59)
    }

    /** הסרת שורה (קישור, רשימה חכמה, תיקייה מתעדכנת) מהרשימה n. הקבצים לא נוגעים. רשימה חכמה שכולה כאן — לסל. */
    private boolean removeLine(Tree.Node n, String key) {
        char k = key.charAt(1);
        String v = key.substring(2);
        if (k == 'r') return n.refs.remove(v);
        if (k == 'l') return n.live.remove(v);
        return n.smart.remove(v);
    }

    /** 1.16.0: "הגדרות" של רשימה חכמה שבתוך הרשימה — הטופס שלה; הגדרה נפוצה — נפתחת (ההגדרות — שם). */
    private void smartSettings(Nested.Item si) {
        Structure.Func f = Structure.asTool(si.ref);
        if (f == null || si.common) {
            Nested.open(this, si);
            return;
        }
        ToolMenu.configure(this, f, new Runnable() {
            @Override public void run() { refresh(); }
        });
    }

    // ---------- בחירה מרובה ----------

    private void startSelecting() {
        sel.start();
    }

    private void toggleList(String name) {
        sel.toggleList(name);
    }

    private void stopSelecting() {
        sel.stop();
    }

    private void toggle(String p) {
        sel.toggle(p);
    }


    /**
     * 142 · 1.14.0: ללוח; מנווטים לכל מקום (רשימה, בית, אופן אחר) ו"הדבקה כאן". 1.16.0: full — "העתקה" (עותק עצמאי);
     * אחרת — "קישור" (רק רשימות) או "העברה".
     */
    private void transferSelected(final boolean move, boolean full) {
        if (selected.isEmpty() && selectedLists.isEmpty()) {
            Ui.toast(this, Words.NOTHING_SELECTED);
            return;
        }
        List<VClip.Item> what = new ArrayList<VClip.Item>();
        for (String n : selectedLists) what.add(n.startsWith(LINE) ? lineItem(n) : new VClip.Item(VClip.LIST, n, n));
        for (String p : selected) what.add(new VClip.Item(VClip.FILE, p, new File(p).getName()));   // 1.16.0 (באג): גם בהעתקה
        clip(what, move, full);
        stopSelecting();
    }

    private void clip(List<VClip.Item> what, boolean move, boolean full) {
        VClip.set(Modes.current(), path, what, move, full);
        pasteBar.refresh();
        Ui.toast(this, VClip.label() + Words.VClipW.HINT);
    }

    private void removeSelected() {
        if (selected.isEmpty() && selectedLists.isEmpty()) {
            Ui.toast(this, Words.NOTHING_SELECTED);
            return;
        }
        final List<String> paths = new ArrayList<String>(selected);
        final List<String> lists = new ArrayList<String>(), lines = new ArrayList<String>();   // 1.17.1: גם שורות
        for (String n : selectedLists) (n.startsWith(LINE) ? lines : lists).add(n);
        String what = (lists.isEmpty() ? "" : lists.size() + Words.Manual.LISTS_AND) + (paths.size() + lines.size()) + Words.Manual.ITEMS_SUFFIX;
        int subLists = 0, inner = 0;                                   // 94: מה יוצא יחד עם הרשימות שנבחרו
        Tree.Node here = Tree.load().find(path);
        if (here != null) for (String name : lists) {
            Tree.Node c = here.child(name);
            if (c == null) continue;
            int[] t = c.totals();
            subLists += t[0];
            inner += t[1];
        }
        Ui.ask(this, Words.Manual.REMOVE_TITLE + what, Words.NO_TOUCH_FILES,
                lists.isEmpty() ? Words.Manual.REMOVES_ONLY : Words.Manual.removesLists(lists.size(), subLists, inner, paths.size()),
                lists.isEmpty() ? Words.Manual.CAN_ADD_AGAIN : Words.Box.IN_BIN,
                Words.Manual.REMOVE, new Runnable() {
                    @Override
                    public void run() {
                        if (!lists.isEmpty()) {
                            BinBox.lists(path, lists);                   // 1.10.0: לסל — לפני ההסרה
                            Files.backupNamed(Paths.tree(), Words.Manual.BEFORE_DELETING + lists.size() + Words.Manual.LISTS_SUFFIX);
                            synchronized (Tree.LOCK) {                   // 92
                                Tree t = Tree.load();
                                Tree.Node n = t.find(path);
                                if (n != null) for (String name : lists) {
                                    Tree.Node c = n.child(name);
                                    if (c != null) n.children.remove(c);
                                }
                                t.save();
                            }
                        }
                        if (!lines.isEmpty()) {
                            for (String k : lines) {                         // רשימה חכמה שכולה כאן — לסל, ואז נמחקת
                                String ref = k.substring(2);
                                if (k.charAt(1) != 's' || Nested.kindOf(ref) != Nested.Kind.INNER || Nested.uses(ref) > 1) continue;
                                BinBox.smart(path, ref, lineItem(k).name);
                                Structure.removeById(ref);
                            }
                            change(new Change() {
                                @Override
                                public boolean apply(Tree t, Tree.Node node) {
                                    boolean ch = false;
                                    for (String k : lines) ch |= removeLine(node, k);
                                    return ch;
                                }
                            });
                        }
                        Tree.removeFiles(path, paths);
                        stopSelecting();
                        refresh();
                    }
                });
    }

    private void fromSearch() {
        Intent i = new Intent(this, SearchScreen.class);
        i.putStringArrayListExtra(SearchScreen.EXTRA_PICK_INTO, path);
        startActivity(i);
    }

    private void missing(final Item it) {
        if (it.state == FileItem.State.OFFLINE) {
            Ui.toast(this, Words.Manual.MEDIA_OUT);
            return;
        }
        if (it.state == FileItem.State.DELETED) {
            Ui.options(this, FileItem.State.DELETED.line,
                    new String[]{Words.Manual.OPEN_TRASH, Words.REMOVE_FROM_LIST, Words.CLOSE},
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface d, int w) {
                            if (w == 0) startActivity(new Intent(VirtualScreen.this, TrashScreen.class));
                            else if (w == 1) removeEntry(it);
                        }
                    });
            return;
        }
        Ui.options(this, Words.FILE_NOT_FOUND,
                new String[]{Words.Manual.SEARCH_AGAIN, Words.REMOVE_FROM_LIST, Words.CLOSE},
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        if (w == 0) relocate(it, false);
                        else if (w == 1) removeEntry(it);
                    }
                });
    }

    // ---------- שינויים ----------

    private interface Change {
        boolean apply(Tree t, Tree.Node node);
    }

    private void change(Change c) {
        synchronized (Tree.LOCK) {
            Tree t = Tree.load();
            Tree.Node node = t.find(path);
            if (node != null && c.apply(t, node)) t.save();
        }
        refresh();
    }

    private void newFolder() {
        Ui.askText(this, Words.NEW_LIST, "", Words.Manual.LIST_NAME, new Ui.Text1() {
            @Override
            public void ok(final String name) {
                change(new Change() {
                    @Override
                    public boolean apply(Tree t, Tree.Node node) {
                        Tree.newChild(node, node.uniqueChildName(name, null));
                        return true;
                    }
                });
            }
        });
    }

    private void collect(Tree.Node from, Tree.Node moving, List<ArrayList<String>> targets,
                         List<String> labels, int depth) {
        for (Tree.Node c : from.children) {
            if (c == moving) continue;
            StringBuilder indent = new StringBuilder();
            for (int i = 0; i < depth; i++) indent.append("    ");
            if (c == moving.parent) {
                targets.add(null);
                labels.add(indent + "📂 " + c.name + Words.Manual.CURRENT_PLACE);
            } else {
                targets.add(c.path());
                labels.add(indent + "📋 " + c.name);
            }
            collect(c, moving, targets, labels, depth + 1);
        }
    }

    private void removeEntry(final Item it) {
        change(new Change() {
            @Override
            public boolean apply(Tree t, Tree.Node node) {
                return Tree.remove(node.entries, it.entry.path);
            }
        });
    }

    private void pickFiles() {
        startActivity(SearchScreen.fileManager(this, "", null, path));        // 159
    }

    // ---------- איתור קובץ שנעלם ----------

    private void relocate(final Item it, final boolean sizeOnly) {
        final String oldPath = it.entry.path, name = it.entry.name();
        final long size = it.entry.size;
        Ui.toast(this, sizeOnly ? Words.Manual.SEARCHING_SIZE : Words.Manual.SEARCHING_FILE);
        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<String> byNameSize = new ArrayList<String>(), byName = new ArrayList<String>(),
                        bySize = new ArrayList<String>(), bySig = new ArrayList<String>();
                Sig.Entry st = Sig.stored(oldPath);               // 190: יש חתימה — רק מה שהתוכן שלו זהה (גם בשם אחר)
                if (lib.ready && st != null && st.size > 0)
                    for (Library.Hit h : lib.bySize(st.size)) if (st.sig.equals(Sig.of(new File(h.path())))) bySig.add(h.path());
                if (!bySig.isEmpty()) {
                    final String found = Sig.closest(oldPath, bySig);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (isFinishing()) return;
                            Ui.ask(VirtualScreen.this, Words.Manual.UPDATE_PATH_TITLE, Words.NO_TOUCH_FILES,
                                    Words.Manual.FOUND + Words.Manual.BY_SIG + ": " + found, Words.Manual.BACKUP_BEFORE_CHANGE,
                                    Words.Manual.UPDATE, new Runnable() {
                                        @Override public void run() { apply(oldPath, found); }
                                    });
                        }
                    });
                    return;
                }
                if (lib.ready) {                              // קודם באינדקס: מיידי
                    for (Library.Hit h : lib.byName(name, size)) byNameSize.add(h.path());
                    if (byNameSize.isEmpty()) for (Library.Hit h : lib.byName(name, 0)) byName.add(h.path());
                    if (byNameSize.isEmpty() && byName.isEmpty() && sizeOnly)
                        for (Library.Hit h : lib.bySize(size)) if (bySize.size() < 20) bySize.add(h.path());
                }
                boolean indexFound = !byNameSize.isEmpty() || !byName.isEmpty() || !bySize.isEmpty();
                if (!indexFound) for (Vols.Vol v : Vols.list(VirtualScreen.this)) {
                    ArrayDeque<File> q = new ArrayDeque<File>();
                    q.add(v.root);
                    while (!q.isEmpty()) {
                        File[] all = q.poll().listFiles();
                        if (all == null) continue;
                        for (File f : all) {
                            if (Vols.skip(f, false)) continue;
                            if (f.isDirectory()) {
                                q.add(f);
                                continue;
                            }
                            boolean n = f.getName().equals(name), s = size > 0 && f.length() == size;
                            if (n && s) byNameSize.add(f.getAbsolutePath());
                            else if (n) byName.add(f.getAbsolutePath());
                            else if (s && sizeOnly && bySize.size() < 20) bySize.add(f.getAbsolutePath());
                        }
                    }
                }
                final List<String> hits = !byNameSize.isEmpty() ? byNameSize
                        : !byName.isEmpty() ? byName : bySize;
                final String how = !byNameSize.isEmpty() ? Words.Manual.BY_NAME_SIZE
                        : !byName.isEmpty() ? Words.Manual.BY_NAME : Words.Manual.BY_SIZE;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isFinishing()) return;
                        if (hits.isEmpty()) {
                            if (!sizeOnly && size > 0) {
                                Ui.confirm(VirtualScreen.this, Words.Manual.NO_SUCH_NAME,
                                        Words.Manual.SEARCH_SIZE_ASK, Words.Manual.SEARCH_BY_SIZE,
                                        new Runnable() {
                                            @Override public void run() { relocate(it, true); }
                                        });
                            } else {
                                Ui.toast(VirtualScreen.this, Words.Manual.NOT_ON_DEVICE);
                            }
                            return;
                        }
                        if (hits.size() == 1) {
                            final String found = hits.get(0);
                            Ui.ask(VirtualScreen.this, Words.Manual.UPDATE_PATH_TITLE, Words.NO_TOUCH_FILES,
                                    Words.Manual.FOUND + how + ": " + found, Words.Manual.BACKUP_BEFORE_CHANGE, Words.Manual.UPDATE, new Runnable() {
                                @Override public void run() { apply(oldPath, found); }
                            });
                        } else {
                            Ui.list(VirtualScreen.this, how + " · " + hits.size() + Words.Manual.MATCHES, hits,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface d, int which) {
                                            apply(oldPath, hits.get(which));
                                        }
                                    });
                        }
                    }
                });
            }
        }).start();
    }

    private void apply(String oldPath, String newPath) {
        int n;
        synchronized (Tree.LOCK) {                                   // 92
            Tree t = Tree.load();
            n = t.replacePath(oldPath, newPath, new File(newPath).length());
            if (n > 0) t.save();
        }
        Ui.toast(this, n > 0 ? Words.Manual.PATH_UPDATED_IN + n + Words.Manual.LISTS_SUFFIX : Words.Manual.NOTHING_TO_UPDATE);
        refresh();
    }

    // ---------- תפריט ----------

    @Override
    protected void menu(Menu menu) {
        menu.add(0, 3, 0, Words.SELECT);
        menu.add(0, 900, 90, Words.BATCH_MENU);                        // 127
        menu.add(0, 1, 2, Words.Manual.EDIT_TREE_TITLE);
        menu.add(0, 2, 1, Words.Manual.REFRESH);
        menu.add(0, 6, 3, "➕ " + Words.NEW_LIST + "…");                       // 1.13.1 (באג): גם בלי הכפתור שבשורה
        menu.add(0, 8, 5, Words.Bg.RELOCATE_BUTTON);                          // 190
        if (!path.isEmpty()) menu.add(0, 9, 6, Words.Manual.SETTINGS_HERE);     // 1.16.0: הגדרות אחת לרשימה הזו
        else menu.add(0, 10, 7, Words.DesignW.MENU);                           // השורש — רק עיצוב
    }

    // ---------- 1.10.0: הפעולות של הרשימה — בשורת הכפתורים התחתונה (מה שהיה כפתורים קבועים) ----------

    @Override
    public List<String[]> actions() {
        List<String[]> l = new ArrayList<String[]>();
        l.add(new String[]{Buttons.NEW_LIST, "➕", Words.Top.NEW_LIST});
        l.add(new String[]{Buttons.FROM_DRIVE, "💾", Words.Top.FROM_DRIVE});
        l.add(new String[]{Buttons.FROM_SEARCH, "🔎", Words.Top.FROM_SEARCH});
        return l;
    }

    @Override
    public boolean act(String code) {
        if (code.equals(Buttons.NEW_LIST)) newListMenu();
        else if (code.equals(Buttons.FROM_DRIVE)) pickFiles();
        else if (code.equals(Buttons.FROM_SEARCH)) fromSearch();
        else return false;
        return true;
    }

    /** פריט 8: "רשימה חדשה" — ידנית, או חכמה (חדשה, או הגדרה נפוצה קיימת). */
    private void newListMenu() {
        Ui.options(this, Words.NEW_LIST, new String[]{Words.Manual.NEW_MANUAL, Words.Manual.NEW_SMART, Words.Manual.NEW_REF},
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        if (w == 0) newFolder();
                        else if (w == 1) addSmartMenu();
                        else addRef();
                    }
                });
    }

    // ---------- 1.13.0: הפניה לרשימה, גודל, הזזה ידנית ----------

    /** "↪ הפניה לרשימה קיימת" — בלי להעתיק אותה (קבוצה ותגית גם ברשימות עצמן). */
    private void addRef() {
        ListPicker.choose(this, Words.Manual.NEW_REF, null, new ListPicker.Picked() {
            @Override
            public void ok(ArrayList<String> target) {
                if (target.isEmpty()) return;
                if (Tree.inside(path, target)) {
                    Ui.toast(VirtualScreen.this, Words.Manual.REF_SELF);      // הפניה לעצמה או למה שמעליה — לולאה
                    return;
                }
                final ArrayList<String> to = target;
                change(new Change() {
                    @Override
                    public boolean apply(Tree t, Tree.Node node) {
                        return Tree.addRef(node, path, to);
                    }
                });
            }
        });
    }

    /** "↕ הזזה": למעלה · למטה · לראש · לסוף — לפריטים ולרשימות שנבחרו (כל סוג בתוך הסוג שלו). נשמר בסדר הקובץ. */
    /** 1.17.2: מפתח הסדר של שורה (Tree.ORDER_LINE): סוג + ערך. */
    private static String orderKey(Item it) {
        if (it.type == FOLDER) return "c" + it.folder;
        if (it.type == SMART) return "s" + it.smart.ref;
        if (it.type == REF) return "r" + it.refText;
        if (it.type == LIVE) return "l" + it.live;
        return it.entry != null ? "f" + it.entry.path : null;
    }

    /** 1.17.2: הסדר החופשי — מה שבו לפי הסדר שלו; מה שעוד לא בו (נוסף אחר כך) — אחריו, בסדר הרגיל. */
    private static void freeOrder(List<Item> items, List<String> order) {
        final java.util.Map<String, Integer> at = new java.util.HashMap<String, Integer>();
        for (int i = 0; i < order.size(); i++) at.put(order.get(i), i);
        final java.util.Map<Item, Integer> rank = new java.util.HashMap<Item, Integer>();
        for (int i = 0; i < items.size(); i++) {
            Integer r = at.get(orderKey(items.get(i)));
            rank.put(items.get(i), r != null ? r : order.size() + i);
        }
        java.util.Collections.sort(items, new java.util.Comparator<Item>() {
            @Override public int compare(Item x, Item y) { return rank.get(x) - rank.get(y); }
        });
    }

    /**
     * 1.13.0 · 1.17.2: הזזה ידנית של מה שנבחר — למעלה · למטה · לראש · לסוף, בין כל השורות (המילון, 12: סדר חופשי —
     * קובץ יכול לבוא לפני קישור או לפני תת־רשימה). הסדר נשמר ברשימה (@סדר).
     */
    private void orderSelected() {
        if (selected.isEmpty() && selectedLists.isEmpty()) {
            Ui.toast(this, Words.NOTHING_SELECTED);
            return;
        }
        Ui.options(this, Words.Manual.ORDER, new String[]{Words.Manual.ORDER_UP, Words.Manual.ORDER_DOWN,
                Words.Manual.ORDER_TOP, Words.Manual.ORDER_BOTTOM}, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, final int how) {
                final java.util.Set<String> picked = new java.util.HashSet<String>();
                for (String p : selected) picked.add("f" + p);
                for (String n : selectedLists) picked.add(n.startsWith(LINE) ? n.substring(1) : "c" + n);
                final List<String> keys = new ArrayList<String>();           // הסדר כמו שהוא מוצג עכשיו
                for (Item it : items) if (orderKey(it) != null) keys.add(orderKey(it));
                change(new Change() {
                    @Override
                    public boolean apply(Tree t, Tree.Node node) {
                        if (!Order.move(keys, how, new Order.Picked<String>() {
                            @Override public boolean is(String k) { return picked.contains(k); }
                        })) return false;
                        node.order.clear();
                        node.order.addAll(keys);
                        return true;
                    }
                });
                if (srt != null && !Sort.MINE.equals(srt.by)) Ui.toast(VirtualScreen.this, Words.Manual.ORDER_SORTED);
            }
        });
    }

    // ---------- 161: רשימה חכמה בתוך רשימה ידנית ----------

    private void addSmart(final String ref) {
        change(new Change() {
            @Override
            public boolean apply(Tree t, Tree.Node node) {
                if (node.smart.contains(ref)) return false;
                node.smart.add(ref);
                return true;
            }
        });
    }

    /** "➕ רשימה בתוך הרשימה": הגדרה נפוצה קיימת · חדשה (רשימה ידנית בתוך ידנית — "רשימה חדשה", כמו תמיד). */
    private void addSmartMenu() {
        Ui.options(this, Words.Inner.ADD, new String[]{Words.Inner.COMMON, Words.Inner.NEW},
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        if (w == 0) {
                            final List<SmartLists.Named> all = SmartLists.all();
                            if (all.isEmpty()) {
                                Ui.toast(VirtualScreen.this, Words.Inner.NONE_COMMON);
                                return;
                            }
                            List<String> names = new ArrayList<String>();
                            for (SmartLists.Named n : all) names.add(n.name);
                            Ui.list(VirtualScreen.this, Words.Inner.COMMON, names, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface d2, int k) {        // 1.17.3: מתבנית — עותק משלה
                                    String id = Structure.copyInner(all.get(k).id, Tree.label(path) + " › " + all.get(k).name);
                                    if (id != null) addSmart(id);
                                }
                            });
                        } else {
                            SmartForm.show(VirtualScreen.this, null, null, new Func.Done() {
                                @Override
                                public void ok(String name, Target t) {
                                    String link = SmartLists.linkOf(t);
                                    if (!link.isEmpty()) {
                                        addSmart(link);
                                        return;
                                    }
                                    addSmart(Nested.newInner(Tree.label(path), name, t.get(SmartLists.COND, ""),
                                            t.get(SmartLists.SORT, Sort.NAME), t.get(SmartLists.VIEW, FolderView.FLAT)));
                                }
                            });
                        }
                    }
                });
    }

    @Override
    protected boolean item(int id) {
        if (id == 900) {                                       // 127
            CmdMenu.show(this, null);
            return true;
        }
        if (id == 1) {
            Ui.editText(this, Paths.tree());
            return true;
        }
        if (id == 6) {                                          // 161 · 1.13.1: ידנית / חכמה / הפניה
            newListMenu();
            return true;
        }
        if (id == 10) {                                         // 1.12.0: עיצוב
            ListSettings.design(this, path, new Runnable() {
                @Override public void run() { refresh(); }
            });
            return true;
        }
        if (id == 9) {                                          // 1.16.0: הגדרות הרשימה הזו
            settings(path);
            return true;
        }
        if (id == 8) {                                          // 190
            SigUi.relocateAll(this, lib, new Runnable() {
                @Override public void run() { refresh(); }
            });
            return true;
        }
        if (id == 2) {
            refresh();
            return true;
        }
        if (id == 3) {
            sel.flip();
            return true;
        }
        return false;
    }
}
