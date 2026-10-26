package com.tikiyot.vfm;

// קבצים כפולים. אחראי: כפולים בתוך מקום אחד, או השוואה בין שני מקומות (בשניהם · רק בא' · רק בב').
// המסך יציב: מחושב פעם אחת, ופעולות מסמנות את השורות במקום לחשב מחדש (סעיף 69).
// בחירה בתיבות סימון; נגיעה בשורה פותחת את הקובץ (68). המיקום הוא העיקר (56, 67).

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DupScreen extends Activity {

    public static final String EXTRA_SCOPE = "scope";
    public static final String EXTRA_MODE = "mode";
    public static final String EXTRA_AGAINST = "against";
    public static final String EXTRA_FUNC = "func";          // מזהה הכלי: המיון נשמר בסעיף שלו

    private static final String ISO = Text.ISO, PDI = Text.PDI;   // בידוד כיוון כתיבה — ממקום אחד

    private Library lib;
    private String scope, mode, against;
    private boolean between;
    private final Handler h = new Handler(Looper.getMainLooper());

    /** קבוצה: כותרת ועותקים. */
    /** פס מפריד בין קבוצות: "זהים" / "בספק" (85). */
    private static final class Divider {
        final String text;
        final boolean doubt;

        Divider(String text, boolean doubt) {
            this.text = text;
            this.doubt = doubt;
        }
    }

    private static final class Group {
        /** בספק: הגודל זהה והשמות רק דומים — לא זהים (85). */
        boolean doubt;
        String name, type;
        long size;
        final List<Copy> copies = new ArrayList<Copy>();
        int inB;                       // בהשוואה: כמה עותקים בב'
    }

    /** עותק: שורה עם תיבת סימון. */
    private static final class Copy {
        Library.Hit hit;
        Group group;                   // null בלשוניות "רק ב..."
        List<String> folders;          // התיקיות מתחת לכונן
        String drive;
        int common;
        String side;
        boolean newest;
        String done;                   // "🗑 נמחק · בסל המחזור" / "הועבר ל: …"
    }

    private final List<Object> rows = new ArrayList<Object>();
    private final List<Group> groups = new ArrayList<Group>();
    private final List<Copy> onlyA = new ArrayList<Copy>(), onlyB = new ArrayList<Copy>();
    private final Set<Copy> selected = new HashSet<Copy>();
    private final Map<String, List<String>> listsOf = new HashMap<String, List<String>>();
    private int tab;                   // 0 בשניהם · 1 רק בא' · 2 רק בב'
    private String summary = "", headShort = "", headMore = "";
    /** הכותרת: קצרה (המקומות והסיכום); נגיעה עליה פותחת את הפרטים. */
    private boolean details, hint;
    private Sort srt;

    /** המיון האחיד (79): בסיס משותף, ו"מקום לפינוי" כאפשרות המיוחדת של הכפולים. */
    private static final Sort.Key<Group> GROUP = new Sort.Key<Group>() {
        @Override public String name(Group g) { return g.name; }
        @Override public String folder(Group g) { return g.copies.isEmpty() ? "" : folderOf(g.copies.get(0)); }
        @Override public long size(Group g) { return g.size; }
        @Override public long mtime(Group g) {
            long m = 0;
            for (Copy c : g.copies) m = Math.max(m, c.hit.rec.mtime);
            return m;
        }
        @Override public long added(Group g) { return 0; }
        @Override public boolean dir(Group g) { return false; }
        @Override public long special(Group g) { return -g.size * Math.max(1, g.copies.size() - 1); }
        @Override public int section(Group g) { return 0; }
    };
    private static final Sort.Key<Copy> COPY = new Sort.Key<Copy>() {
        @Override public String name(Copy c) { return c.hit.name(); }
        @Override public String folder(Copy c) { return folderOf(c); }
        @Override public long size(Copy c) { return c.hit.rec.size; }
        @Override public long mtime(Copy c) { return c.hit.rec.mtime; }
        @Override public long added(Copy c) { return 0; }
        @Override public boolean dir(Copy c) { return false; }
        @Override public long special(Copy c) { return -c.hit.rec.size; }
        @Override public int section(Copy c) { return 0; }
    };

    private static String folderOf(Copy c) {
        String p = c.hit.path();
        int i = p.lastIndexOf('/');
        return i > 0 ? p.substring(0, i) : p;
    }

    private BaseAdapter adapter;
    private TextView info;
    private SelectBar bar;
    private LinearLayout tabs;
    private Button[] tabButtons = new Button[3];

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        lib = Library.get(this);
        scope = extra(EXTRA_SCOPE);
        mode = extra(EXTRA_MODE);
        if (mode.isEmpty()) mode = Words.Dup.NAME_SIZE;
        against = extra(EXTRA_AGAINST).trim();
        srt = Sort.of(Sort.SPACE, false, Sort.tool(extra(EXTRA_FUNC).isEmpty() ? null : extra(EXTRA_FUNC), FnDup.KIND, ""));
        between = !against.isEmpty();
        setTitle(Words.Dup.DUPLICATES);

        LinearLayout root = Ui.screen(this);
        info = Ui.info(this);
        hint = Ui.firstTimes(this, Words.Dup.DUPLICATES);
        info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                details = !details;
                showHead();
            }
        });
        root.addView(info);

        tabs = new LinearLayout(this);
        tabs.setLayoutDirection(Ui.DIRECTION);
        for (int i = 0; i < 3; i++) {
            final int k = i;
            tabButtons[i] = new Button(this);
            tabButtons[i].setAllCaps(false);
            tabButtons[i].setTextSize(13);
            tabButtons[i].setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { tab = k; show(); }
            });
            tabs.addView(tabButtons[i], new LinearLayout.LayoutParams(0, -2, 1f));
        }
        tabs.setVisibility(between ? View.VISIBLE : View.GONE);
        root.addView(tabs);

        ListView list = new ListView(this);
        adapter = new BaseAdapter() {
            @Override public int getCount() { return rows.size(); }
            @Override public Object getItem(int i) { return rows.get(i); }
            @Override public long getItemId(int i) { return i; }
            @Override public int getViewTypeCount() { return 3; }
            @Override public int getItemViewType(int i) {
                Object o = rows.get(i);
                return o instanceof Group ? 0 : o instanceof Copy ? 1 : 2;
            }

            @Override
            public View getView(int pos, View convert, ViewGroup parent) {
                Object o = rows.get(pos);
                if (o instanceof Group) return groupView((Group) o, convert);
                if (o instanceof Copy) return copyView((Copy) o, convert);
                TextView t = convert instanceof TextView ? (TextView) convert : new TextView(DupScreen.this);
                int p = Ui.dp(DupScreen.this, 16);
                if (o instanceof Divider) {                      // פס ברור בין הזהים לאלה שבספק (85)
                    Divider d = (Divider) o;
                    t.setText(d.text);
                    t.setTextColor(Ui.TEXT);
                    t.setTypeface(null, Typeface.BOLD);
                    t.setTextSize(16);
                    t.setBackgroundColor(d.doubt ? Ui.DOUBT_STRONG : Ui.HEADER_SOFT);
                    t.setPadding(p, p / 2, p, p / 2);
                    return t;
                }
                t.setText((String) o);
                t.setTextColor(Ui.MUTED);
                t.setTypeface(null, Typeface.NORMAL);
                t.setTextSize(14);
                t.setBackgroundColor(0);
                t.setPadding(p, p, p, p);
                return t;
            }
        };
        list.setAdapter(adapter);
        root.addView(list, new LinearLayout.LayoutParams(-1, 0, 1f));

        // הפס האחיד (83), מוצג תמיד: ✕ מנקה את הבחירה
        bar = new SelectBar(this, new Runnable() {
            @Override
            public void run() {
                selected.clear();
                show();
            }
        });
        Map<Actions.Action, Runnable> own = new HashMap<Actions.Action, Runnable>();
        own.put(Actions.DELETE_FROM_DRIVE, new Runnable() {
            @Override public void run() { deleteSelected(); }
        });
        own.put(Actions.MOVE_TO_FOLDER, new Runnable() {
            @Override public void run() { moveSelected(); }
        });
        Actions.fill(this, bar, Actions.Tool.DUP, new Actions.Selection() {
            @Override public List<String> files() { return selectedPaths(new ArrayList<Copy>(selected)); }
            @Override public int lists() { return 0; }
            @Override public int dirs() { return 0; }
            @Override public List<String> dirPaths() { return new ArrayList<String>(); }
            @Override public String picking() { return null; }
            @Override
            public void changed() {
                selected.clear();
                show();
            }
        }, own);
        bar.show(true);
        root.addView(bar.view);
        setContentView(TopRow.wrap(this, root));                   // 1.9.0: שורת הכפתורים העליונה
        compute();
    }

    private String extra(String k) {
        String v = getIntent() != null ? getIntent().getStringExtra(k) : null;
        return v == null ? "" : v;
    }

    // ---------- חישוב (פעם אחת) ----------

    private void compute() {
        if (!lib.ready) {
            info.setText(Words.LOADING_INDEX);
            h.postDelayed(new Runnable() {
                @Override public void run() { compute(); }
            }, 400);
            return;
        }
        if (contentMode() && needSigs()) return;                              // 190: קודם החתימות החסרות (ברקע)
        long t0 = System.currentTimeMillis();
        groups.clear();
        onlyA.clear();
        onlyB.clear();
        selected.clear();
        buildListsOf();
        List<Library.Hit> a = group(scope);
        unsigned = 0;
        if (contentMode()) {                                                   // 194: מה לא נבדק בתוכן
            Map<Long, Integer> sz = new HashMap<Long, Integer>();
            for (Library.Hit x : a) sz.put(x.rec.size, sz.containsKey(x.rec.size) ? sz.get(x.rec.size) + 1 : 1);
            for (Library.Hit x : a) if (x.rec.sig == 0 && sz.get(x.rec.size) > 1) unsigned++;
        }
        boolean same = between && Sources.fromCond(scope).equals(Sources.fromCond(against))
                && Sources.rest(scope).equals(Sources.rest(against));
        if (!between || same) within(a);
        else versus(a, group(against));
        String head = !between || same ? Words.Dup.DUPS_IN + Sources.describe(scope)
                : Words.Dup.A_COLON + Sources.describe(scope) + Words.Dup.B_COLON + Sources.describe(against);
        if (same) head += Words.Dup.A_B_SAME;
        headShort = head + "\n" + summary + (unsigned > 0 ? Words.Dup.unsigned(unsigned) : "");
        headMore = Words.Dup.SAME_BY + mode + " · " + (System.currentTimeMillis() - t0) + Words.Dup.MS
                + Words.Dup.MARK_HINT;
        if (same) between = false;
        tabs.setVisibility(between ? View.VISIBLE : View.GONE);
        tab = 0;
        show();
    }

    private void buildListsOf() {
        listsOf.clear();
        listsOf.putAll(Tree.load().whereListed(Nested.inner(lib)));          // 169
    }

    private List<Library.Hit> group(String spec) {
        List<Library.Hit> out = new ArrayList<Library.Hit>();
        String sp = spec == null ? "" : spec.trim();
        Cond cond = sp.isEmpty() || sp.equals(Words.Dup.WHOLE_DRIVE) ? null : Cond.parse(sp);
        for (Index ix : lib.indexes)
            for (Index.Rec r : ix.files) {
                if (r.gone) continue;
                if (r.size == 0) continue;                        // 96: קבצים ריקים אינם עותקים זה של זה
                if (Library.hide(ix, r, cond)) continue;          // הסל והמוסתרים לא נחשבים עותקים
                if (cond != null && !cond.matches(ix, r)) continue;
                out.add(Library.hit(ix, r));
            }
        return out;
    }

    /** "גודל ושם דומה" (85): מקבצים לפי גודל, ובתוך כל גודל — לפי דמיון השמות (alike). */
    static final String SIMILAR = Words.Dup.SIZE_SIMILAR;

    /** 195: השם, הגודל והתוכן — מתוך "השוואה". */
    private DupMode dm() {
        if (dmCache == null || !mode.equals(dmFor)) {
            dmCache = DupMode.parse(mode);
            dmFor = mode;
        }
        return dmCache;
    }

    private DupMode dmCache;
    private String dmFor;

    private boolean similarMode() {
        return dm().name == DupMode.NAME_SIMILAR;
    }

    /** 190: לפי התוכן — אותו גודל ואותה חתימה. */
    private boolean contentMode() {
        return dm().content;
    }

    private volatile boolean signing;
    /** 194: פעם אחת בכל חישוב — קובץ שלא נחתם (השתנה, לא נקרא) לא יעצור את ההשוואה (באג: "נתקע" על "מחשב חתימות"). */
    private boolean signTried;
    /** 194: כמה קבצים באותו גודל נשארו בלי חתימה — לא נחשבים כפולים "בתוכן". */
    private int unsigned;

    /**
     * 190: לקבצים שיש להם עוד קובץ באותו גודל ואין להם חתימה (העבודה ברקע עוד לא הגיעה אליהם) — מחשבים
     * עכשיו, בחוט נפרד, ואז חוזרים לחישוב. true — יצא לחשב.
     */
    private boolean needSigs() {
        if (signing) return true;
        if (signTried) return false;
        List<Library.Hit> all = new ArrayList<Library.Hit>(group(scope));
        if (between) all.addAll(group(against));
        Map<Long, Integer> sizes = new HashMap<Long, Integer>();
        for (Library.Hit x : all) {
            Integer n = sizes.get(x.rec.size);
            sizes.put(x.rec.size, n == null ? 1 : n + 1);
        }
        final List<Library.Hit> todo = new ArrayList<Library.Hit>();
        Set<Index.Rec> seen = new HashSet<Index.Rec>();
        for (Library.Hit x : all)
            if (x.rec.sig == 0 && sizes.get(x.rec.size) > 1 && seen.add(x.rec)) todo.add(x);
        if (todo.isEmpty()) return false;
        signing = true;
        signTried = true;
        info.setText(Words.Dup.signing(todo.size()));
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (Library.Hit x : todo) {
                    java.io.File f = new java.io.File(x.path());
                    if (f.length() != x.rec.size || f.lastModified() != x.rec.mtime) continue;
                    long v = Sig.ofLong(f);
                    if (v != 0) Sig.write(lib, x.ix, x.rec, x.path(), v);
                }
                for (Index ix : lib.indexes) lib.sigsChanged(ix.key);
                h.post(new Runnable() {
                    @Override
                    public void run() {
                        signing = false;
                        if (!isFinishing()) compute();
                    }
                });
            }
        }, "חתימות לכפולים").start();  // תחביר
        return true;
    }

    private boolean alike(Library.Hit x, Library.Hit y) {
        return !similarMode() || Text.similarNames(x.name(), y.name());
    }

    /** קבוצה של אותו גודל — מפוצלת לקבוצות של שמות דומים (כל מי שדומה לאחד מהקבוצה נכנס אליה). */
    private List<List<Library.Hit>> split(List<Library.Hit> g) {
        List<List<Library.Hit>> out = new ArrayList<List<Library.Hit>>();
        if (!similarMode()) {
            out.add(g);
            return out;
        }
        for (Library.Hit x : g) {
            List<Library.Hit> home = null;
            for (java.util.Iterator<List<Library.Hit>> it = out.iterator(); it.hasNext(); ) {
                List<Library.Hit> c = it.next();
                boolean near = false;
                for (Library.Hit y : c) if (alike(x, y)) { near = true; break; }
                if (!near) continue;
                if (home == null) home = c;
                else {                                    // x מחבר שתי קבוצות — מאחדים
                    home.addAll(c);
                    it.remove();
                }
            }
            if (home == null) out.add(home = new ArrayList<Library.Hit>());
            home.add(x);
        }
        return out;
    }

    private String key(Library.Hit hit) {
        DupMode d = dm();                                                      // 195: כל שילוב
        StringBuilder k = new StringBuilder();
        if (d.name == DupMode.NAME_SAME) k.append(hit.rec.norm);
        k.append('|');
        if (d.size) k.append(hit.rec.size);
        k.append('|');
        if (d.content) k.append(hit.rec.sig != 0 ? Long.toHexString(hit.rec.sig) : "?" + hit.path());   // 190: בלי חתימה — לבד
        return k.toString();
    }

    private void within(List<Library.Hit> all) {
        Map<String, List<Library.Hit>> m = new LinkedHashMap<String, List<Library.Hit>>();
        for (Library.Hit x : all) {
            List<Library.Hit> g = m.get(key(x));
            if (g == null) m.put(key(x), g = new ArrayList<Library.Hit>());
            g.add(x);
        }
        long waste = 0;
        int files = 0;
        for (List<Library.Hit> big : m.values()) {
            if (big.size() < 2) continue;
            for (List<Library.Hit> g : split(big)) {
                if (g.size() < 2) continue;
                groups.add(makeGroup(g, -1));
                files += g.size();
                for (int i = 1; i < g.size(); i++) waste += g.get(i).rec.size;
            }
        }
        sortGroups();
        summary = Files.count(all.size()) + Words.Dup.FILES_CHECKED + groups.size() + Words.Dup.FILES_WITH_COPIES
                + files + Words.Dup.COPIES_FREE + Files.size(waste);
    }

    private void versus(List<Library.Hit> a, List<Library.Hit> b) {
        Map<String, List<Library.Hit>> inA = new HashMap<String, List<Library.Hit>>(), inB = new HashMap<String, List<Library.Hit>>();
        for (Library.Hit x : a) put(inA, x);
        for (Library.Hit y : b) put(inB, y);
        long both = 0;
        for (Library.Hit x : a) {
            List<Library.Hit> copies = new ArrayList<Library.Hit>();
            List<Library.Hit> g = inB.get(key(x));
            if (g != null) for (Library.Hit y : g) if (y.rec != x.rec && alike(x, y)) copies.add(y);   // הקובץ עצמו אינו עותק
            if (copies.isEmpty()) {
                onlyA.add(single(x, Words.Dup.A));
            } else {
                List<Library.Hit> all = new ArrayList<Library.Hit>();
                all.add(x);
                all.addAll(copies);
                Group gr = makeGroup(all, 1);
                gr.inB = copies.size();
                groups.add(gr);
                both += x.rec.size;
            }
        }
        Set<Index.Rec> aRecs = new HashSet<Index.Rec>();
        for (Library.Hit x : a) aRecs.add(x.rec);
        for (Library.Hit y : b) {
            if (aRecs.contains(y.rec)) continue;
            List<Library.Hit> g = inA.get(key(y));
            boolean has = false;
            if (g != null) for (Library.Hit x : g) if (x.rec != y.rec && alike(x, y)) has = true;
            if (!has) onlyB.add(single(y, Words.Dup.B));
        }
        sortGroups();
        summary = Files.count(a.size()) + Words.Dup.IN_A + Files.count(b.size()) + Words.Dup.IN_B_BOTH + groups.size()
                + " (" + Files.size(both) + Words.Dup.FREE_FROM_A;
    }

    private void put(Map<String, List<Library.Hit>> m, Library.Hit x) {
        List<Library.Hit> g = m.get(key(x));
        if (g == null) m.put(key(x), g = new ArrayList<Library.Hit>());
        g.add(x);
    }

    private void sortGroups() {
        Collections.sort(groups, new Comparator<Group>() {
            @Override
            public int compare(Group x, Group y) {
                return Long.compare(y.size * y.copies.size(), x.size * x.copies.size());
            }
        });
    }

    private Copy single(Library.Hit x, String side) {
        Copy c = new Copy();
        c.hit = x;
        c.side = side;
        c.folders = folders(x);
        c.drive = drive(x);
        c.common = 0;
        return c;
    }

    private Group makeGroup(List<Library.Hit> g, int sideSplit) {
        Group gr = new Group();
        gr.name = g.get(0).name();
        if (similarMode())
            for (Library.Hit x : g) if (!x.rec.norm.equals(g.get(0).rec.norm)) gr.doubt = true;
        gr.type = Ui.typeLabel(gr.name, false);
        gr.size = g.get(0).rec.size;
        long newest = 0, oldest = Long.MAX_VALUE;
        for (Library.Hit x : g) {
            newest = Math.max(newest, x.rec.mtime);
            oldest = Math.min(oldest, x.rec.mtime);
        }
        for (int i = 0; i < g.size(); i++) {
            Copy c = single(g.get(i), sideSplit < 0 ? null : i < sideSplit ? Words.Dup.A : Words.Dup.B);
            c.group = gr;
            c.newest = newest != oldest && c.hit.rec.mtime == newest;
            gr.copies.add(c);
        }
        // החלק המשותף — בתיקיות בלבד, בלי הכונן (67ד)
        int common = Integer.MAX_VALUE;
        for (Copy c : gr.copies) common = Math.min(common, c.folders.size());
        for (int k = 0; k < common; k++) {
            String s = gr.copies.get(0).folders.get(k);
            boolean same = true;
            for (Copy c : gr.copies) if (!c.folders.get(k).equals(s)) same = false;
            if (!same) {
                common = k;
                break;
            }
        }
        for (Copy c : gr.copies) c.common = common;
        return gr;
    }

    private static List<String> folders(Library.Hit h) {
        List<String> out = new ArrayList<String>();
        String root = h.ix.rootPath, p = h.parentPath();
        if (p.startsWith(root)) p = p.substring(root.length());
        for (String s : p.split("/")) if (!s.isEmpty()) out.add(s);
        return out;
    }

    private static String drive(Library.Hit h) {
        return Drive.label(h.ix);
    }

    // ---------- תצוגה ----------

    private void show() {
        rows.clear();
        if (between) {
            tabButtons[0].setText(Words.Dup.IN_BOTH + groups.size() + ")");
            tabButtons[1].setText(Words.Dup.ONLY_A + onlyA.size() + ")");
            tabButtons[2].setText(Words.Dup.ONLY_B + onlyB.size() + ")");
            for (int i = 0; i < 3; i++) tabButtons[i].setTypeface(null, i == tab ? Typeface.BOLD : Typeface.NORMAL);
        }
        showHead();
        if (tab == 0) {
            srt.apply(groups, GROUP);
            int doubt = 0;
            for (Group g : groups) if (g.doubt) doubt++;
            int sure = groups.size() - doubt;
            if (doubt > 0 && sure > 0) rows.add(new Divider(Words.Dup.SAME_HEADER + sure + ")", false));
            for (Group g : groups) {
                if (g.doubt) continue;
                rows.add(g);
                rows.addAll(g.copies);
            }
            if (doubt > 0) rows.add(new Divider(Words.Dup.DOUBT_HEADER + doubt + ")", true));
            for (Group g : groups) {
                if (!g.doubt) continue;
                rows.add(g);
                rows.addAll(g.copies);
            }
            if (groups.isEmpty()) rows.add(between ? Words.Dup.NONE_IN_BOTH : Words.Dup.NO_DUPS);
        } else {
            List<Copy> l = tab == 1 ? onlyA : onlyB;
            srt.apply(l, COPY);
            rows.addAll(l);
            if (l.isEmpty()) rows.add(tab == 1 ? Words.Dup.ALL_A_IN_B : Words.Dup.ALL_B_IN_A);
        }
        updateBar();
        adapter.notifyDataSetChanged();
    }

    /** הכותרת: קצרה, ובנגיעה — עם הפרטים. בפתיחות הראשונות ההסבר מוצג גם בקצרה. */
    private void showHead() {
        String more = details ? "\n" + headMore + Words.Dup.LESS
                : (hint ? Words.Dup.MARK_HINT : "") + Words.Dup.DETAILS;
        info.setText(headShort + " · " + srt.describe() + more);
    }

    private View groupView(Group g, View convert) {
        LinearLayout v;
        TextView name, sub;
        if (convert instanceof LinearLayout && convert.getTag() instanceof TextView[]) {
            v = (LinearLayout) convert;
            TextView[] t = (TextView[]) v.getTag();
            name = t[0];
            sub = t[1];
        } else {
            v = new LinearLayout(this);
            v.setOrientation(LinearLayout.VERTICAL);
            v.setBackgroundColor(Ui.HEADER);
            v.setPadding(Ui.dp(this, 12), Ui.dp(this, 8), Ui.dp(this, 12), Ui.dp(this, 8));
            name = new TextView(this);
            name.setTextColor(Ui.TEXT);
            name.setTextSize(16);
            sub = new TextView(this);
            sub.setTextColor(Ui.SUB);
            sub.setTextSize(13);
            v.addView(name);
            v.addView(sub);
            v.setTag(new TextView[]{name, sub});
        }
        v.setBackgroundColor(g.doubt ? Ui.DOUBT : Ui.HEADER);             // בספק — בצבע משלו (85)
        name.setText((g.doubt ? "≈ " : "") + ISO + g.type + " · " + g.name + PDI);  // שורה 1: סוג ושם, לבד (67א, 67ב)
        String copies = between ? (g.inB == 1 ? Words.Dup.ONE_COPY_B : g.inB + Words.Dup.COPIES_B)
                : g.copies.size() + Words.Dup.COPIES;
        sub.setText(ISO + Files.size(g.size) + PDI + " · " + copies);   // שורה 2: במילים
        return v;
    }

    private View copyView(final Copy c, View convert) {
        LinearLayout v;
        final CheckBox box;
        TextView where, sub;
        if (convert instanceof LinearLayout && convert.getTag() instanceof Object[]) {
            v = (LinearLayout) convert;
            Object[] t = (Object[]) v.getTag();
            box = (CheckBox) t[0];
            where = (TextView) t[1];
            sub = (TextView) t[2];
        } else {
            v = new LinearLayout(this);
            v.setLayoutDirection(Ui.DIRECTION);
            v.setGravity(Gravity.CENTER_VERTICAL);
            v.setPadding(Ui.dp(this, 4), Ui.dp(this, 8), Ui.dp(this, 12), Ui.dp(this, 8));
            box = new CheckBox(this);
            v.addView(box);
            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            where = new TextView(this);
            where.setTextSize(16);
            where.setTextColor(Ui.TEXT);
            sub = new TextView(this);
            sub.setTextSize(13);
            sub.setTextColor(Ui.SUB);
            col.addView(where);
            col.addView(sub);
            v.addView(col, new LinearLayout.LayoutParams(0, -2, 1f));
            v.setTag(new Object[]{box, where, sub});
        }
        SpannableStringBuilder sb = new SpannableStringBuilder();
        if (c.side != null) sb.append(c.side).append("  ");
        sb.append(c.drive);
        for (int k = 0; k < c.folders.size(); k++) {
            sb.append(" › ");
            int st = sb.length();
            sb.append(ISO).append(c.folders.get(k)).append(PDI);
            if (c.group != null && k < c.common) sb.setSpan(new ForegroundColorSpan(Ui.MUTED), st, sb.length(), 0);
            else if (c.group != null) sb.setSpan(new StyleSpan(Typeface.BOLD), st, sb.length(), 0);
        }
        if (c.group == null) sb.append(" › ").append(ISO).append(c.hit.name()).append(PDI);
        where.setText(sb);
        StringBuilder d = new StringBuilder(Ui.date(c.hit.rec.mtime));
        if (c.group == null) d.append(" · ").append(Files.size(c.hit.rec.size));
        if (c.newest) d.append(Words.Dup.NEW);
        if (dm().name != DupMode.NAME_SAME && c.group != null) d.append(" · ").append(ISO).append(c.hit.name()).append(PDI);
        List<String> ls = listsOf.get(c.hit.path());
        if (ls != null && !ls.isEmpty()) {
            d.append(Words.Dup.IN_LIST).append(ls.get(0));                // 67ג
            if (ls.size() > 1) d.append(ls.size() == 2 ? ", " + ls.get(1) : " " + Words.andMore(ls.size() - 1));
        }
        if (c.done != null) d.append("\n").append(c.done);
        sub.setText(d);
        boolean alive = c.done == null;
        where.setAlpha(alive ? 1f : 0.4f);
        sub.setAlpha(alive ? 1f : 0.6f);
        box.setOnCheckedChangeListener(null);
        box.setChecked(selected.contains(c));
        box.setEnabled(alive);
        box.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(android.widget.CompoundButton b, boolean on) {
                if (on) selected.add(c); else selected.remove(c);
                updateBar();
            }
        });
        v.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View x) { Ui.open(DupScreen.this, new File(c.hit.path())); }
        });
        return v;
    }

    private void updateBar() {
        long size = 0;
        for (Copy c : selected) size += c.hit.rec.size;
        bar.setCount(selected.isEmpty() ? Words.NOTHING_SELECTED : Words.Dup.SELECTED + selected.size() + " · " + Files.size(size));
    }

    // ---------- פעולות ----------

    /** בכמה קבוצות נבחרו כל העותקים שעוד קיימים. */
    private int wholeGroups() {
        int n = 0;
        for (Group g : groups) {
            boolean all = true, any = false;
            for (Copy c : g.copies) {
                if (c.done != null) continue;
                any = true;
                if (!selected.contains(c)) all = false;
            }
            if (any && all) n++;
        }
        return n;
    }

    /** כל העותקים של כמה קבצים נבחרו — אזהרה, ומותר להמשיך (68). */
    private String wholeWarning(boolean move) {
        int whole = wholeGroups();
        if (whole == 0) return null;
        String files = Words.files(whole);
        return move ? Words.Dup.ALL_COPIES_OF + files + Words.Dup.GO_SAME_PLACE
                : Words.Dup.WARN_ALL_COPIES + files + Words.Dup.NONE_LEFT;
    }

    private List<String> selectedPaths(List<Copy> list) {
        List<String> paths = new ArrayList<String>();
        for (Copy c : list) paths.add(c.hit.path());
        return paths;
    }

    /** מחיקה מהכונן — במחלקת הפעולות הפיזיות. העותק נשאר במקומו עם סימון (69). */
    private void deleteSelected() {
        final List<Copy> list = new ArrayList<Copy>(selected);
        if (list.isEmpty()) return;
        Physical.askDelete(this, selectedPaths(list), wholeWarning(false), new Physical.Done() {
            @Override
            public void done(Physical.Result r) {
                for (Copy c : list) if (r.done.contains(c.hit.path())) c.done = FileItem.State.DELETED.icon + " " + FileItem.State.DELETED.line;
                selected.clear();
                show();
            }
        });
    }

    /** העברה לתיקייה — במחלקת הפעולות הפיזיות. */
    private void moveSelected() {
        final List<Copy> list = new ArrayList<Copy>(selected);
        if (list.isEmpty()) return;
        List<String> paths = selectedPaths(list);                        // 142: ללוח, ומנווטים להדבקה
        String no = Physical.cannotMove(lib, paths, new ArrayList<String>());
        if (no != null) {
            Ui.toast(this, no);
            return;
        }
        Clip.set(paths, new ArrayList<String>(), false, wholeWarning(true));
        selected.clear();
        show();
        startActivity(SearchScreen.fileManager(this, "", Clip.startDir() == null ? null          // 159
                : Clip.startDir().getAbsolutePath(), null));
        Ui.toast(this, Clip.label() + Words.Dup.PASTE_HINT);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 2, 0, Words.Dup.SORT_MENU);
        menu.add(0, 900, 90, Words.BATCH_MENU);                        // 127
        menu.add(0, 1, 0, Words.Dup.RECOMPUTE);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 900) {                                       // 127
            CmdMenu.show(this, null);
            return true;
        }
        if (item.getItemId() == 1) {
            compute();
            return true;
        }
        if (item.getItemId() == 2) {
            srt.menu(this, new Runnable() {
                @Override public void run() { show(); }
            });
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
