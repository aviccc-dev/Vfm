package com.tikiyot.vfm;

// חיפוש, וגם תצוגת רשימה חכמה (תנאי קבוע).
// מצב הוספה: כשנפתח מתוך רשימה, נגיעה בתוצאה מוסיפה אותה לרשימה.

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SearchScreen extends Activity implements Clip.Holder {

    /** 1.11.0: התיקייה שעליה כבר הודענו "לא באינדקס / לא קיימת" — כדי שההודעה לא תקפוץ בכל ריענון. */
    private String warned;

    public static final String EXTRA_QUERY = "query";
    public static final String EXTRA_FIXED = "fixed";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_SORT = "sort";
    public static final String EXTRA_PICK_INTO = "pick";    // מסלול הרשימה שאליה מוסיפים
    public static final String EXTRA_FUNC = "func";         // מזהה הכלי: ההגדרות נשמרות בסעיף שלו
    public static final String EXTRA_VIEW = "view";         // 148: ברצף / בתיקיות — ברירת המחדל של הרשימה
    public static final String EXTRA_DIR = "dir";           // 159: בתיקיות — מאיפה מתחילים
    public static final String EXTRA_OWNER = "owner";       // 161: מזהה הרשימה (לרשימות שבתוכה)
    public static final String EXTRA_HIDDEN = "hidden";     // 159: מוסתרים מוצגים

    /**
     * 159 — "מנהל קבצים": רשימה חכמה בלי סינון, "בתיקיות — כולל הכול" (במקום המסך הישן).
     * sources — תנאי מקורות ("" — כל הכוננים); dir — התיקייה שנפתחת (או null); pickInto — הוספה לרשימה (או null).
     */
    static android.content.Intent fileManager(android.content.Context c, String sources, String dir,
                                              java.util.ArrayList<String> pickInto) {
        android.content.Intent i = new android.content.Intent(c, SearchScreen.class);
        i.putExtra(EXTRA_QUERY, sources == null || sources.isEmpty()
                ? Sources.toCond(java.util.Arrays.asList(Sources.INTERNAL, Sources.CARD)) : sources);
        i.putExtra(EXTRA_VIEW, FolderView.ALL);
        i.putExtra(EXTRA_FIXED, true);
        i.putExtra(EXTRA_SORT, Sort.NAME);
        i.putExtra(EXTRA_TITLE, SmartLists.FILE_MANAGER);
        if (dir != null) i.putExtra(EXTRA_DIR, dir);
        if (pickInto != null) i.putStringArrayListExtra(EXTRA_PICK_INTO, pickInto);
        return i;
    }

    private Library lib;
    private final Handler h = new Handler(Looper.getMainLooper());
    private final List<Object> rows = new ArrayList<Object>();
    private Set<String> selected = new LinkedHashSet<String>();
    private Select sel;
    private final Set<String> inList = new java.util.HashSet<String>();
    /** 146: שורות שאחרי פעולה כבר אינן עונות — נשארות באפור עד רענון (מסך יציב). המפתח: המסלול שבשורה. */
    private final java.util.Map<String, FileItem.State> out = new java.util.HashMap<String, FileItem.State>();
    private String infoBase = "";
    /** 175: מוצגות תיקיות ריקות (ו"בחירת הכול" בוחרת אותן). */
    private boolean emptyMode;
    /** 148: התצוגה (ברירת המחדל מהרשימה; "תצוגה…" בתפריט — לפתיחה הזו בלבד), והתיקייה שבה עומדים. */
    private String view = FolderView.FLAT;
    private FolderView fv;
    /** התיקייה הנוכחית; null — רשימת המקורות. */
    private String here;
    private boolean placed;
    /** 159: התיקייה שבה נפתחים (מבחוץ: "פתיחת התיקייה", הדבקה). */
    private String startDir;
    /** 161: הרשימה הזו (כלי, הגדרה נפוצה או פנימית) — והרשימות שבתוכה, בראש. */
    private String owner;
    private final List<Nested.Item> inner = new ArrayList<Nested.Item>();
    private TextView where;
    /** 153 — החיפוש האחד: ההגדרות (מקורות, סוגים, תאריך, גודל) בחלונית במסך, וחלות מיד; המילים — בשורת ההקלדה. */
    private SmartForm.Parts parts;
    private android.widget.ScrollView panel;
    private TextView settings;
    /** 149: פס הלוח — "הדבקה כאן" (ב"כולל הכול") ו"הדבקה…" (בורר); ומוסתרים ב"כולל הכול". */
    private LinearLayout clipBar;
    private TextView clipText;
    private Button pasteHere;
    private boolean hidden;
    private final List<String> volRoots = new ArrayList<String>();
    /** סימון התאמה ב"כולל הכול". */
    private static final int MATCH_BG = 0xFFE3F2E6;
    private BaseAdapter adapter;
    private EditText input;
    private TextView info;
    private SelectBar bar;
    /** ההסבר "נגיעה ארוכה…" — רק בפתיחות הראשונות. */
    private boolean hint;
    private String queryNorm = "", fixedQuery;
    private Sort srt;
    private boolean fixed, paths, loose = true;
    private ArrayList<String> pickInto;
    private Runnable pending;
    private static final int[] SECTION_BG = Library.MATCH_COLORS;
    /** שורת תוצאה בעיני המיון (79). */
    static final Sort.Key<Library.Hit> HIT = new Sort.Key<Library.Hit>() {
        @Override public String name(Library.Hit h) { return h.name(); }
        @Override public String folder(Library.Hit h) { return h.parentPath(); }
        @Override public long size(Library.Hit h) { return h.rec == null ? 0 : h.rec.size; }
        @Override public long mtime(Library.Hit h) { return h.rec == null ? 0 : h.rec.mtime; }
        @Override public long added(Library.Hit h) { return h.rec == null ? 0 : h.rec.added; }
        @Override public boolean dir(Library.Hit h) { return h.isDir(); }
        @Override public long special(Library.Hit h) { return h.rank; }
        @Override public int section(Library.Hit h) { return h.section; }
    };


    private String funcId;

    /** 1.17.3: רשימה חכמה (כלי, או רשימה בתוך רשימה) — ולא חיפוש פתוח; לשורות הכפתורים. */
    boolean isList() {
        return funcId != null || owner != null;
    }
    /**
     * 1.15.0: איפה נשמרות ההגדרות של המסך ("למה", הכפתורים הדולקים, המיון…): הכלי — או רשימה חכמה פנימית (מאז שהסביבות
     * הוסבו לרשימות, הכלים החכמים שבהן הם רשימות פנימיות — עם ה"למה" וההגדרות שלהם). null — ההגדרות הכלליות.
     */
    private String settingsId;
    /** 1.13.0: שורת "למה". */
    private String whyText = "", whyKey = "";

    /** הגדרה של המסך: בסעיף הכלי (לפי המזהה), או בהגדרות הכלליות כשלא נפתח מכלי. */
    private String setting(Structure.Func fn, String key, String def) {
        if (fn != null) {
            String v = fn.params.get(key);
            return v == null || v.isEmpty() ? def : v;
        }
        return Conf.get(Conf.SEARCH, key, def);
    }

    private void saveSetting(final String key, final String value) {
        final String id = settingsId;
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (id != null && Structure.byId(id) != null) Structure.setById(id, key, value);
                else Conf.set(Conf.SEARCH, key, value);
            }
        }).start();
    }
    private TextView legend;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        lib = Library.get(this);
        readIntent();

        if (srt == null) srt = Sort.withAdded(Sort.RANK, true, Sort.tool(null, Conf.SEARCH, ""));
        LinearLayout root = Ui.screen(this);
        buildQuery(root);
        buildInfo(root);
        buildList(root);
        buildSelection();
        buildClipBar(root);
        root.addView(bar.view);
        setContentView(TopRow.wrap(this, root));                   // 1.9.0: שורת הכפתורים העליונה

        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { schedule(); }
        });
        parts.watch(new Runnable() {                                          // 153: שינוי בהגדרות — מיד
            @Override public void run() { schedule(); }
        });
    }

    /** הכוונה שבה נפתח המסך: חיפוש קבוע, כלי, הוספה לרשימה, תצוגה, מיון, כותרת. */
    private void readIntent() {
        if (getIntent() == null) return;
        fixed = getIntent().getBooleanExtra(EXTRA_FIXED, false);
        fixedQuery = getIntent().getStringExtra(EXTRA_QUERY);
        String extraSort = getIntent().getStringExtra(EXTRA_SORT);
        pickInto = getIntent().getStringArrayListExtra(EXTRA_PICK_INTO);
        funcId = getIntent().getStringExtra(EXTRA_FUNC);
        String v = getIntent().getStringExtra(EXTRA_VIEW);
        if (v != null) view = v;
        startDir = getIntent().getStringExtra(EXTRA_DIR);
        hidden = getIntent().getBooleanExtra(EXTRA_HIDDEN, false);
        Structure.Func fn = Structure.func(funcId);
        if (fn == null) funcId = null;
        owner = getIntent().getStringExtra(EXTRA_OWNER);                   // 161
        if (owner == null) owner = Nested.ownerOf(fn);
        settingsId = funcId != null ? funcId : owner != null && Nested.kindOf(owner) == Nested.Kind.INNER ? owner : null;
        Structure.Func cfg = settingsId == null ? null : Structure.byId(settingsId);
        paths = "כן".equals(setting(cfg, SmartLists.PATHS, "לא"));  // תחביר
        loose = !"לא".equals(setting(cfg, SmartLists.LOOSE, "כן"));  // תחביר
        Sort.Store st = Sort.tool(settingsId, Conf.SEARCH, "");
        srt = Sort.withAdded(Sort.RANK, true, st);
        if (extraSort != null && st.get(SmartLists.SORT, "").isEmpty()) srt.prefer(extraSort);
        if (fixed && getWindow() != null)
            getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        String title = fn != null ? fn.name : getIntent().getStringExtra(EXTRA_TITLE);
        whyText = cfg != null ? cfg.target().get(Program.WHY, "")              // 1.13.0: "למה" של הכלי
                : SmartLists.FILE_MANAGER.equals(title) ? Words.WhyW.VIRTUAL_FILES : "";
        whyKey = cfg != null ? Words.WhyW.KEY_TOOL + settingsId : Words.WhyW.KEY_SCREEN + title;
        setTitle(pickInto != null ? Words.ADD_TO_LIST + ": " + Tree.label(pickInto)
                : title != null && !title.isEmpty() ? title : Words.Look.SEARCH);
    }

    /** שורת החיפוש וחלונית ההגדרות (153). */
    private void buildQuery(LinearLayout root) {
        if (pickInto != null) {
            TextView hint = Ui.info(this);
            hint.setText(Words.Look.TAP_ADDS + Words.NO_TOUCH);
            hint.setBackgroundColor(Ui.BG_SOFT);
            root.addView(hint);
        }
        String[] split = SmartLists.split(fixedQuery);                        // 153: הגדרות לחלונית, מילים לשורה
        input = Ui.field(this, split[1], Words.Find.HINT);
        input.setTextSize(18);
        input.setPadding(Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 12), Ui.dp(this, 10));
        root.addView(input);
        quick = new QuickBar(this, new QuickBar.Host() {                       // 181: כפתורי תצוגה מהירה
            @Override public String base() { return query(); }
            @Override public int count(String c) { return lib.search(c, paths, loose).total; }
            @Override public List<Library.Hit> hits(String c) { return lib.search(c, paths, loose).hits; }
            @Override public String loadState() {
                String v = setting(settingsId == null ? null : Structure.byId(settingsId), Quick.STATE, "");
                return v.isEmpty() ? null : v;
            }
            @Override public void saveState(String s) { saveSetting(Quick.STATE, s.isEmpty() ? Quick.SEP : s); }
            @Override public void changed() { search(); }
        });
        root.addView(quick.view);
        parts = new SmartForm.Parts(this, Words.Find.ALL_DRIVES);
        parts.load(split[0]);
        LinearLayout box = Ui.form(this);
        parts.addTo(this, box, true, false);
        panel = new android.widget.ScrollView(this);
        panel.addView(box);
        panel.setVisibility(View.GONE);
        settings = Ui.info(this);
        settings.setBackgroundColor(Ui.BG_SOFT);
        settings.setText(Words.Find.SETTINGS + " ▸");
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean open = panel.getVisibility() != View.VISIBLE;
                panel.setVisibility(open ? View.VISIBLE : View.GONE);
                settings.setText(Words.Find.SETTINGS + (open ? " ▾" : " ▸"));
            }
        });
        if (pickInto != null) settings.setVisibility(View.GONE);
        root.addView(settings);
        root.addView(panel, new LinearLayout.LayoutParams(-1, Ui.dp(this, 300)));
    }

    /** שורת המידע (נגיעה — רענון, 110), המקרא, ו"איפה עומדים" (148). */
    private void buildInfo(LinearLayout root) {
        info = Ui.info(this);
        info.setOnClickListener(new View.OnClickListener() {                  // 110: רענון נגיש
            @Override
            public void onClick(View v) {
                if (!input.getText().toString().trim().isEmpty()) return;
                Ui.toast(SearchScreen.this, Words.Look.REFRESHING);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final int n = lib.refreshAsked();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Ui.toast(SearchScreen.this, n == 0 ? Words.Look.INDEX_UP_TO_DATE : n + Words.Look.FOLDERS_REFRESHED);
                                search();
                            }
                        });
                    }
                }).start();
            }
        });
        hint = Ui.firstTimes(this, Words.Look.SEARCH);
        TextView whyBar = Why.bar(this);                                      // 1.13.0
        root.addView(whyBar);
        Why.show(whyBar, whyKey, whyText);
        root.addView(info);
        legend = Ui.info(this);
        legend.setVisibility(View.GONE);
        root.addView(legend);
        where = Ui.info(this);                                                // 148: איפה עומדים; נגיעה — למעלה
        where.setBackgroundColor(Ui.BG_SOFT);
        where.setVisibility(View.GONE);
        where.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { up(); }
        });
        root.addView(where);
    }

    /** רשימת התוצאות: שורה לכל תוצאה, נגיעה ונגיעה ארוכה. */
    private void buildList(LinearLayout root) {
        ListView list = new ListView(this);
        adapter = new BaseAdapter() {
            @Override public int getCount() { return rows.size(); }
            @Override public Object getItem(int i) { return rows.get(i); }
            @Override public long getItemId(int i) { return i; }
            @Override public int getViewTypeCount() { return 2; }
            @Override public int getItemViewType(int i) { return rows.get(i) instanceof String ? 1 : 0; }
            @Override public boolean isEnabled(int i) { return !(rows.get(i) instanceof String); }

            @Override
            public View getView(int pos, View convert, ViewGroup parent) {
                Object o = rows.get(pos);
                if (o instanceof String) return Ui.header(SearchScreen.this, convert, (String) o, true);
                View v = Ui.row(SearchScreen.this, convert);
                if (o instanceof Nested.Item) {                               // 161: רשימה בתוך הרשימה
                    Nested.Item it = (Nested.Item) o;
                    Ui.bindIcon(v, "", it.name, it.sub);
                    Ui.kind(v, it.smart && it.local ? Ui.SMART : Ui.LINK, it.icon);   // 1.17.0: מה שאינו שלה — קישור ↪
                    v.setBackgroundColor(0x00000000);
                    return v;
                }
                if (o instanceof FolderView.Folder) {                         // 148: תיקייה בתצוגה בתיקיות
                    FolderView.Folder f = (FolderView.Folder) o;
                    boolean top = here == null;
                    boolean missing = top && !volRoots.contains(f.path) && !new File(f.path).isDirectory();   // 1.11.0: מקור שאינו בכונן
                    Ui.bindIcon(v, missing ? "⚠" : top && volRoots.contains(f.path) ? Drive.icon(Drive.internalPath(f.path)) : "📁",
                            top ? rootLabel(f.path) : f.name, missing ? Words.Views.NOT_FOUND : f.matches > 0 ? Words.Views.matches(f.matches) : "");
                    v.setBackgroundColor(sel.hasDir(f.path) ? Select.HIGHLIGHT
                            : FolderView.ALL.equals(view) && fv != null && fv.isMatch(f.path) ? MATCH_BG : 0x00000000);
                    return v;
                }
                Library.Hit hit = (Library.Hit) o;
                FileItem.State gone = out.get(hit.path());
                boolean marked = FolderView.ALL.equals(view) && fv != null && here != null;   // 148: "כולל הכול" — מסמנים התאמות
                boolean match = marked && fv.isMatch(hit.path());
                String status = gone != null ? gone.note : inList.contains(hit.path()) ? Words.Look.IN_LIST : null;
                if (match) status = status == null ? "✓" : "✓ · " + status;
                if (hit.section > 0) status = (status == null ? "" : status + " · ")
                        + Library.MATCH_TYPES[hit.section];
                if (hit.innerEmpty > 0) status = (status == null ? "" : status + " · ") + Words.Empty.inside(hit.innerEmpty);   // 175
                boolean byAdded = Sort.ADDED.equals(srt.by) && hit.rec != null;          // 189: התאריך שבשורה — של ההוספה
                if (byAdded) status = (status == null ? "" : status + " · ")
                        + (hit.rec.added > 0 ? Words.Look.ADDED : Words.Look.ADDED_UNKNOWN);
                else if (hit.rec != null && Ui.full(SearchScreen.this))                       // 189: בנתונים המלאים — תמיד
                    status = (status == null ? "" : status + " · ")
                            + (hit.rec.added > 0 ? Words.Look.ADDED + " " + Ui.date(hit.rec.added) : Words.Look.ADDED_UNKNOWN);
                Ui.bindRow(SearchScreen.this, v, hit.name(), hit.isDir(),
                        hit.rec != null ? hit.rec.size : 0, hit.rec == null ? 0 : byAdded ? hit.rec.added : hit.rec.mtime,
                        hit.path(), status, queryNorm);
                if (selected.contains(hit.path()) || sel != null && sel.hasDir(hit.path()))
                    v.setBackgroundColor(Select.HIGHLIGHT);
                else v.setBackgroundColor(gone != null ? 0x00000000 : marked ? (match ? MATCH_BG : 0x00000000)
                        : SECTION_BG[hit.section]);
                if (gone != null) ((TextView[]) v.getTag())[1].setTextColor(Ui.MUTED);    // 146: באפור
                return v;
            }
        };
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> a, View v, int pos, long id) {
                Object o = rows.get(pos);
                if (o instanceof Nested.Item) {                               // 161
                    if (!sel.on()) openInner((Nested.Item) o);
                    return;
                }
                if (o instanceof Library.Hit) tap((Library.Hit) o);
                else if (o instanceof FolderView.Folder) {                    // 148: נכנסים לתיקייה
                    FolderView.Folder f = (FolderView.Folder) o;
                    if (sel.on()) sel.toggleDir(f.path);
                    else {
                        here = f.path;
                        showFolder();
                    }
                }
            }
        });
        list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> a, View v, int pos, long id) {
                Object o = rows.get(pos);
                if (o instanceof Nested.Item) {                               // 161: הוצאה
                    innerActions((Nested.Item) o);
                    return true;
                }
                if (o instanceof FolderView.Folder) {                         // 148: הכלל האחיד (59)
                    sel.longPressDir(((FolderView.Folder) o).path);
                    return true;
                }
                if (!(o instanceof Library.Hit)) return true;
                Library.Hit hit = (Library.Hit) o;
                FileItem.State gone = out.get(hit.path());
                if (gone != null && gone != FileItem.State.OUT) {             // 146
                    Ui.toast(SearchScreen.this, gone.line);
                    return true;
                }
                if (hit.isDir()) sel.longPressDir(hit.path());   // הכלל האחיד (59) — גם תיקייה
                else sel.longPress(hit.path());
                return true;
            }
        });
        root.addView(list, new LinearLayout.LayoutParams(-1, 0, 1f));
    }

    /** בחירה ופעולות על הנבחרים. */
    private void buildSelection() {
        sel = new Select(this, new Select.All() {
            @Override
            public void fill(java.util.Set<String> files, java.util.Set<String> lists) {
                for (Object o : rows)
                    if (o instanceof Library.Hit && !((Library.Hit) o).isDir()) {
                        String p = ((Library.Hit) o).path();
                        FileItem.State gone = out.get(p);
                        if (gone == null || gone == FileItem.State.OUT) files.add(p);   // 146: לא מה שכבר אינו כאן
                    } else if (o instanceof Library.Hit && emptyMode && !out.containsKey(((Library.Hit) o).path()))
                        sel.dirs.add(((Library.Hit) o).path());                // 175: "בחירת הכול" — התיקיות הריקות
            }
        }, new Runnable() {
            @Override public void run() { adapter.notifyDataSetChanged(); }
        });
        bar = sel.bar;
        selected = sel.files;
        sel.actions(this, Actions.Tool.SEARCH, pickInto != null ? Tree.label(pickInto) : null, new Runnable() {
            @Override public void run() { stay(); }                            // 146: מסך יציב אחרי פעולה
        }, java.util.Collections.<Actions.Action, Runnable>singletonMap(Actions.ADD_TO_LIST, new Runnable() {
            @Override public void run() { addSelected(); }
        }));
    }

    /** 149: פס ההדבקה (מה בלוח, הדבקה כאן, בחירת יעד, ביטול). */
    private void buildClipBar(LinearLayout root) {
        clipBar = new LinearLayout(this);                                     // 149
        clipBar.setOrientation(LinearLayout.HORIZONTAL);
        clipBar.setGravity(android.view.Gravity.CENTER_VERTICAL);
        clipBar.setBackgroundColor(Ui.HEADER);
        clipBar.setPadding(Ui.dp(this, 8), Ui.dp(this, 4), Ui.dp(this, 8), Ui.dp(this, 4));
        clipText = new TextView(this);
        clipText.setTextColor(Ui.TEXT);
        clipText.setTextSize(14);
        clipBar.addView(clipText, new LinearLayout.LayoutParams(0, -2, 1f));
        pasteHere = clipButton(Words.PASTE_HERE, new Runnable() {
            @Override public void run() { Physical.paste(SearchScreen.this, here == null ? null : new File(here), afterPaste()); }
        });
        clipButton(Words.Here.PASTE_PICK, new Runnable() {
            @Override public void run() { pastePick(); }
        });
        clipButton(Words.CANCEL, new Runnable() {
            @Override
            public void run() {
                Clip.clear();
                showClip();
            }
        });
        clipBar.setVisibility(View.GONE);
        root.addView(clipBar);
    }

    /**
     * 153: "שמירה" — הכלי הזה (רשימה עם שם — היא; כלי "חיפוש" ישן — הופך לרשימה חכמה).
     * "שמירה בשם" — רשימה עם שם, וכלי חדש שמקשר אליה (באותו מקום כמו זה, או במסך הבית); שם קיים — עדכון.
     */
    private void save(boolean as) {
        final String q = query(), s = srt.by, d = srt.desc ? "יורד" : "", w = view;  // תחביר
        final Structure.Func fn = funcId == null ? null : Structure.func(funcId);
        if (!as && fn == null && owner != null && Nested.kindOf(owner) != Nested.Kind.TOOL) {   // 161: הגדרה נפוצה או פנימית
            SmartLists.Named l = SmartLists.byId(owner);
            if (l == null) l = SmartLists.byId(Nested.linkOf(owner));         // 1.15.0: פנימית שמקושרת להגדרה נפוצה
            if (l != null) SmartLists.save(l.name, q, s, d, w);
            else Nested.saveInner(owner, q, s, w);
            Ui.toast(this, Words.Find.saved(getTitle().toString()));
            return;
        }
        if (!as && fn != null) {
            SmartLists.Named l = SmartLists.byId(SmartLists.linkOf(fn.target()));
            if (l != null) SmartLists.save(l.name, q, s, d, w);
            else Structure.updateFunc(funcId, fn.name, fn.description,
                    Target.of(FnSmart.KIND, SmartLists.COND, q, SmartLists.SORT, s, SmartLists.VIEW, w));
            Ui.toast(this, Words.Find.saved(fn.name));
            return;
        }
        SmartForm.askSaveAs(this, fn != null ? fn.name : input.getText().toString().trim(), new SmartForm.SaveAs() {
            @Override
            public void store(String n, boolean fresh) {
                String id = SmartLists.save(n, q, s, d, w);
                if (fresh) {                                                  // חדשה — וכלי שמקשר אליה
                    Target t = Target.of(FnSmart.KIND, SmartLists.LINK, id, SmartLists.SORT, s, SmartLists.DIR, d);
                    String nid = Structure.addDirectFunc(n, FnSmart.KIND, "", t);
                    if (nid != null) {
                        funcId = nid;
                        setTitle(n);
                        invalidateOptionsMenu();
                    }
                }
                Ui.toast(SearchScreen.this, Words.Find.saved(n));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        lib.start(null);
        lib.returned();                                                  // 110
        loadInList();
        loadInner();
        search();
        showClip();
    }

    // ---------- 161: רשימה בתוך רשימה ----------

    private void loadInner() {
        inner.clear();
        if (owner != null && pickInto == null) inner.addAll(Nested.items(Nested.refs(owner)));
    }

    /** הרשימות הפנימיות — בראש הרשימה (ברצף; ובתיקיות — במקום שבו מתחילים). */
    private void addInner() {
        if (inner.isEmpty()) return;
        if (folderMode() && (fv == null || here != null && !here.equals(fv.start()))) return;
        for (Nested.Item it : inner) it.sub = innerSub(it);                   // פעם אחת להצגה
        rows.addAll(0, inner);
    }

    private String innerSub(Nested.Item it) {
        if (it.gone) return Words.Inner.GONE;
        if (!it.smart) {
            Tree.Node n = Tree.load().find(it.path);
            int[] t = n == null ? new int[]{0, 0} : n.totals();
            return (t[0] > 0 ? t[0] + Words.Look.LISTS_DOT : "") + Words.items(t[1]);
        }
        if (it.dup != null) return Func.byKind(FnDup.KIND).title;
        int n = lib.ready ? lib.search(it.cond, false, true).hits.size() : 0;
        return (it.common ? Words.Named.TITLE + " · " : "") + Words.files(n);
    }

    private void openInner(Nested.Item it) {
        if (it.gone) {
            Ui.toast(this, Words.Inner.GONE);
            return;
        }
        if (!it.smart) {
            VirtualScreen.open(this, it.path, null, null);                    // 1.13.1: החזרה — לכאן
            return;
        }
        Nested.open(this, it);                                                // 1.15.1: גם כפולים
    }

    /** פתיחת רשימה חכמה פנימית — במסך הזה, עם הבעלים שלה. */
    static android.content.Intent innerIntent(android.content.Context c, Nested.Item it) {
        android.content.Intent i = new android.content.Intent(c, SearchScreen.class);
        i.putExtra(EXTRA_QUERY, it.cond);
        i.putExtra(EXTRA_FIXED, true);
        i.putExtra(EXTRA_TITLE, it.name);
        i.putExtra(EXTRA_SORT, it.sort.isEmpty() ? Sort.NAME : it.sort);
        i.putExtra(EXTRA_VIEW, it.view.isEmpty() ? FolderView.FLAT : it.view);
        i.putExtra(EXTRA_OWNER, it.ref);
        return i;
    }

    private void innerActions(final Nested.Item it) {
        Ui.ask(this, Words.Inner.REMOVE + ": " + it.name, Words.NO_TOUCH_FILES, Words.Inner.removeWhat(it.local && Nested.uses(it.ref) <= 1),
                null, Words.Inner.REMOVE, new Runnable() {
                    @Override
                    public void run() {
                        Nested.remove(owner, it.ref);
                        loadInner();
                        search();
                    }
                });
    }

    /** "➕ רשימה בתוך הרשימה": הגדרה נפוצה קיימת · רשימה ידנית קיימת · חדשה. */
    private void addInnerMenu() {
        Ui.options(this, Words.Inner.ADD, new String[]{Words.Inner.COMMON, Words.Inner.MANUAL, Words.Inner.NEW},
                new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int w) {
                        if (w == 0) pickCommon();
                        else if (w == 1) pickManual();
                        else newInner();
                    }
                });
    }

    private void added(boolean ok) {
        if (!ok) Ui.toast(this, Words.Inner.ALREADY);
        loadInner();
        search();
    }

    private void pickCommon() {
        final List<SmartLists.Named> all = SmartLists.all();
        if (all.isEmpty()) {
            Ui.toast(this, Words.Inner.NONE_COMMON);
            return;
        }
        List<String> names = new ArrayList<String>();
        for (SmartLists.Named n : all) names.add(n.name);
        Ui.list(this, Words.Inner.COMMON, names, new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(android.content.DialogInterface d, int w) {    // 1.17.3: מתבנית — עותק משלה
                String id = Structure.copyInner(all.get(w).id, all.get(w).name);
                if (id != null) added(Nested.add(owner, id));
            }
        });
    }

    private void pickManual() {
        ListPicker.choose(this, Words.Inner.MANUAL, null, new ListPicker.Picked() {
            @Override public void ok(ArrayList<String> path) { added(Nested.add(owner, Nested.manual(path))); }
        });
    }

    private void newInner() {
        SmartForm.show(this, null, null, new Func.Done() {
            @Override
            public void ok(String name, Target t) {
                String link = SmartLists.linkOf(t);
                if (!link.isEmpty()) {                                        // בחר הגדרה נפוצה בטופס — קישור
                    added(Nested.add(owner, link));
                    return;
                }
                String id = Nested.newInner(getTitle().toString(), name, t.get(SmartLists.COND, ""),
                        t.get(SmartLists.SORT, Sort.NAME), t.get(SmartLists.VIEW, FolderView.FLAT));
                added(Nested.add(owner, id));
            }
        });
    }

    // ---------- 149: הלוח ברשימה החכמה ----------

    private Button clipButton(String text, final Runnable r) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { r.run(); }
        });
        clipBar.addView(b);
        return b;
    }

    private Runnable afterPaste() {
        return new Runnable() {
            @Override
            public void run() {
                search();
                showClip();
            }
        };
    }

    /** מה בלוח; "הדבקה כאן" — רק ב"כולל הכול", בתוך תיקייה שאפשר להדביק בה. */
    private void showClip() {
        if (clipBar == null) return;
        if (Clip.isEmpty() || pickInto != null) {
            clipBar.setVisibility(View.GONE);
            return;
        }
        clipBar.setVisibility(View.VISIBLE);
        boolean canHere = FolderView.ALL.equals(view) && here != null && Clip.cannotPasteInto(new File(here)) == null;
        clipText.setText("📋 " + Clip.label());
        pasteHere.setVisibility(canHere ? View.VISIBLE : View.GONE);
    }

    private void pastePick() {
        FolderPicker.choose(this, Words.Here.PICK_TITLE, true, new FolderPicker.Picked() {
            @Override public void ok(File folder) { Physical.paste(SearchScreen.this, folder, afterPaste()); }
        });
    }

    /** אחרי "העתקה/העברה" מתוך הרשימה: נשארים, ובוחרים איך להדביק (במקום לעבור לכלי אחר). */
    @Override
    public void clipped() {
        showClip();
        Ui.options(this, Clip.label(), new String[]{Words.Here.TO_ALL, Words.Here.PICK, Words.Here.LATER},
                new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int w) {
                        if (w == 0) {
                            if (!FolderView.ALL.equals(view)) {
                                view = FolderView.ALL;
                                if (here == null) placed = false;
                                search();
                            }
                            showClip();
                        } else if (w == 1) pastePick();
                    }
                });
    }

    @Override
    protected void onPause() {
        lib.left();                                                      // 110
        super.onPause();
    }

    private void loadInList() {
        inList.clear();
        if (pickInto == null) return;
        Tree.Node n = Tree.load().find(pickInto);
        if (n != null) for (Tree.Entry e : n.entries) inList.add(e.path);
    }

    // ---------- נגיעות ובחירה ----------

    private void tap(Library.Hit hit) {
        FileItem.State gone = out.get(hit.path());
        if (gone != null && gone != FileItem.State.OUT) {                     // 146: כבר אינו כאן
            Ui.toast(this, gone.line);
            return;
        }
        if (sel.on()) {
            if (hit.isDir()) sel.toggleDir(hit.path()); else toggle(hit);
        } else if (hit.isDir()) {
            if (pickInto != null) {
                Adder.folder(this, new File(hit.path()), pickInto, new Adder.Done() {
                    @Override public void done() { loadInList(); adapter.notifyDataSetChanged(); }
                });
            } else {
                startActivity(fileManager(this, "", hit.path(), null));         // 159
            }
        } else if (pickInto != null) {
            int n = Tree.addFiles(pickInto, Collections.singletonList(hit.path()));
            Ui.toast(this, n > 0 ? Words.Look.ADDED_TO_LIST : Words.Look.ALREADY_IN_LIST);
            loadInList();
            adapter.notifyDataSetChanged();
        } else {                                                              // 1.13.1: קובץ פקודות — כקובץ (תפריט: הרצה / עריכה)
            Ui.open(this, new File(hit.path()));
        }
    }

    /** הפריט היחיד שנבחר, או null. */
    private Library.Hit single() {
        if (selected.size() != 1) return null;
        String p = selected.iterator().next();
        for (Object o : rows) if (o instanceof Library.Hit && ((Library.Hit) o).path().equals(p)) return (Library.Hit) o;
        return null;
    }

    private void startSelecting() {
        sel.start();
    }

    private void stopSelecting() {
        sel.stop();
    }

    private void toggle(Library.Hit hit) {
        if (!hit.isDir()) sel.toggle(hit.path());
    }


    private void addSelected() {
        if (sel.count() == 0) {
            Ui.toast(this, Words.NOTHING_SELECTED);
            return;
        }
        List<File> folders = new ArrayList<File>();
        for (String d : sel.dirs) folders.add(new File(d));
        Adder.many(this, folders, new ArrayList<String>(selected), pickInto, new Adder.Done() {   // (76)
            @Override
            public void done() {
                loadInList();
                adapter.notifyDataSetChanged();
            }
        });
        stopSelecting();
    }

    // ---------- חיפוש ----------

    private void schedule() {
        if (pending != null) h.removeCallbacks(pending);
        pending = new Runnable() {
            @Override public void run() { search(); }
        };
        h.postDelayed(pending, 30);
    }

    private QuickBar quick;

    /** 181: מה שמחפשים — התנאי, ועוד הכפתורים הדולקים (רק כשיש מה לחפש; הכפתורים לבד אינם חיפוש). */
    private String searchQuery() {
        String q = query();
        return q.trim().isEmpty() || quick == null ? q : SmartLists.join(q, quick.cond());
    }

    /** 153: התנאי — ההגדרות שבחלונית ועוד מה שבשורת ההקלדה. */
    private String query() {
        return SmartLists.withHere(SmartLists.join(parts == null ? "" : parts.build(), input.getText().toString()), hereDir());
    }

    /** 1.11.0: %כאן% — התיקייה שממנה פתחו (EXTRA_DIR), ואם אין — תיקיית אופן העבודה. */
    private String hereDir() {
        return startDir != null ? startDir : Paths.work().getAbsolutePath();
    }

    /** 1.11.0: "קודם כאן, השאר בתיקיות" — מה שבתוך %כאן% ישר, וכל השאר תחת כותרת של התיקייה שלו. */
    private void hereFirst(List<Library.Hit> hits) {
        String h = hereDir() + "/";
        java.util.Map<String, List<Library.Hit>> others = new java.util.TreeMap<String, List<Library.Hit>>();
        for (Library.Hit hit : hits) {
            String p = hit.path();
            if (p.startsWith(h)) {
                rows.add(hit);
                continue;
            }
            String dir = new File(p).getParent();
            List<Library.Hit> l = others.get(dir);
            if (l == null) others.put(dir, l = new ArrayList<Library.Hit>());
            l.add(hit);
        }
        for (java.util.Map.Entry<String, List<Library.Hit>> e : others.entrySet()) {
            rows.add("📁 " + FolderView.name(e.getKey()) + " · " + Drive.nameOf(e.getKey()));
            rows.addAll(e.getValue());
        }
    }

    private void search() {
        String q = searchQuery();
        if (!lib.ready) {
            info.setText(Words.LOADING_INDEX);
            h.postDelayed(new Runnable() {
                @Override public void run() { search(); }
            }, 400);
            return;
        }
        if (q.trim().isEmpty()) {
            rows.clear();
            adapter.notifyDataSetChanged();
            info.setText(Files.count(lib.totalFiles()) + Words.Look.FILES_IN_INDEX
                    + Files.when(lib.lastBuilt()));
            return;
        }
        Library.Result r = lib.search(q, paths, loose);
        emptyMode = Cond.parse(r.query).emptyDirs;                          // 175: תיקיות ריקות — תמיד ברצף
        queryNorm = r.query;
        out.clear();                                                          // חיפוש מחדש = רענון: האפורים יורדים
        if (folderMode()) {                                                   // 148
            legend.setVisibility(View.GONE);
            fv = new FolderView(r.hits, roots());
            if (!placed || here != null && fv.rootOf(here) == null) {
                here = startDir != null && fv.rootOf(startDir) != null ? startDir : fv.start();   // 159
                placed = true;
            }
            infoBase = Words.Views.matches(fv.total()) + " · " + srt.describe()
                    + (r.note.isEmpty() ? "" : " · " + r.note) + (r.loading ? Words.Look.STILL_LOADING : "");
            info.setText(infoBase);
            showFolder();
            if (r.loading) h.postDelayed(new Runnable() {
                @Override public void run() { search(); }
            }, 600);
            return;
        }
        where.setVisibility(View.GONE);
        List<Library.Hit> hits = new ArrayList<Library.Hit>(r.hits);
        boolean ranked = Sort.RANK.equals(srt.by);
        srt.apply(hits, HIT);
        rows.clear();
        int section = -1;
        boolean mixed = false;
        for (Library.Hit hit : hits) if (hit.section > 0) mixed = true;
        legend.setVisibility(mixed ? View.VISIBLE : View.GONE);
        if (FolderView.HERE.equals(view)) hereFirst(hits);
        else for (Library.Hit hit : hits) {
            if ((ranked || srt.group) && hit.section != section) {
                section = hit.section;
                if (section > 0) rows.add(Library.header(section, r));
            }
            rows.add(hit);
        }
        addInner();                                                           // 161
        adapter.notifyDataSetChanged();
        showLegend(r);
        StringBuilder byType = new StringBuilder();
        if (emptyMode) {
            int inner = 0;
            for (Library.Hit x : hits) inner += x.innerEmpty;
            byType.append(Words.Empty.count(hits.size(), inner));
        } else for (int i = 0; i < r.counts.length; i++)
            if (r.counts[i] > 0 || i == 0)
                byType.append(byType.length() > 0 ? " · " : "").append(Files.count(r.counts[i]))
                        .append(" ").append(i == Library.NO_PREFIX ? Library.header(i, r) : Library.MATCH_TYPES[i]);
        info.setText(infoBase = byType + String.format(Locale.US, Words.Look.MS_FORMAT, r.ms)
                + (r.note.isEmpty() ? "" : " · " + r.note)
                + (r.loading ? Words.Look.STILL_LOADING : "")
                + (ranked && !srt.desc ? "" : " · " + srt.describe())
                + (sel.on() || !hint ? "" : Words.Look.LONG_PRESS_HINT));
        if (r.loading) h.postDelayed(new Runnable() {
            @Override public void run() { search(); }
        }, 600);
    }

    /**
     * 146 — מסך יציב: אחרי פעולה מתוך הרשימה (שינוי שם, העברה, מחיקה) השורות נשארות במקומן.
     * מה שעדיין עונה — מתעדכן (שם ומקום חדשים); מה שכבר לא — נשאר באפור עם הסיבה; מה שנוסף (העתקה) — בסוף.
     * רענון, מיון או שינוי החיפוש — מחשבים מחדש, והאפורים יורדים.
     */
    private void stay() {
        if (folderMode()) {                                                   // 148: בתיקיות
            stayInFolder();
            return;
        }
        String q = searchQuery();
        if (q.trim().isEmpty() || !lib.ready || rows.isEmpty()) {
            search();
            return;
        }
        Library.Result r = lib.search(q, paths, loose);
        java.util.Map<String, Library.Hit> now = new java.util.LinkedHashMap<String, Library.Hit>();
        for (Library.Hit hit : r.hits) now.put(hit.path(), hit);
        Set<String> shown = new java.util.HashSet<String>();
        java.util.Map<String, FileItem.State> grey = new java.util.HashMap<String, FileItem.State>();
        for (int i = 0; i < rows.size(); i++) {
            if (!(rows.get(i) instanceof Library.Hit)) continue;
            Library.Hit old = (Library.Hit) rows.get(i);
            String p = old.path();
            FileItem.State was = out.get(p);
            String where = was != null && was != FileItem.State.OUT ? p : Physical.whereNow(p);
            Library.Hit fresh = now.get(where);
            if (fresh != null) {                                              // עדיין עונה — אולי בשם או במקום חדש
                rows.set(i, fresh);
                shown.add(where);
                continue;
            }
            FileItem.State why = was != null && was != FileItem.State.OUT ? was : FileItem.dropped(lib, where, true);
            Library.Hit found = why == FileItem.State.OUT ? lib.find(where) : null;
            Library.Hit at = found == null || found.rec == null ? null : Library.hit(found.ix, found.rec);   // עותק — לא לשנות את המטמון
            if (at != null) {                                                 // קיים, בשם או במקום החדש — אבל לא עונה
                at.section = old.section;
                rows.set(i, at);
                grey.put(where, why);
            } else grey.put(p, why);                                          // נמחק או חסר — השם הקודם, באפור
        }
        for (java.util.Map.Entry<String, Library.Hit> e : now.entrySet())     // חדש שעונה (למשל עותק) — בסוף
            if (!shown.contains(e.getKey()) && !grey.containsKey(e.getKey())) rows.add(e.getValue());
        out.clear();
        out.putAll(grey);
        adapter.notifyDataSetChanged();
        java.util.Map<FileItem.State, Integer> n = new java.util.LinkedHashMap<FileItem.State, Integer>();
        for (FileItem.State s : grey.values()) n.put(s, n.containsKey(s) ? n.get(s) + 1 : 1);
        StringBuilder sb = new StringBuilder(infoBase);
        for (java.util.Map.Entry<FileItem.State, Integer> e : n.entrySet()) sb.append(" · ").append(e.getKey().count(e.getValue()));
        info.setText(sb.toString());
    }

    // ---------- 148: רשימה חכמה בתיקיות ----------

    private boolean folderMode() {
        return !emptyMode && FolderView.folders(view);
    }

    /** מאיפה מתחילים: המקורות שבתנאי, או הכוננים. */
    private List<String> roots() {
        List<String[]> vols = new ArrayList<String[]>();
        volRoots.clear();
        for (Vols.Vol v : Vols.list(this)) {
            vols.add(new String[]{v.root.getAbsolutePath(), v.internal ? Sources.INTERNAL : Sources.CARD});
            volRoots.add(v.root.getAbsolutePath());
        }
        return FolderView.roots(Sources.fromCond(query()), vols);
    }

    /** שם של מקור: כונן — צלמית ושם; תיקייה — השם והכונן. */
    private String rootLabel(String r) {
        if (volRoots.contains(r)) return Drive.label(Drive.internalPath(r), Drive.nameOf(r));
        return FolderView.name(r) + " · " + Drive.nameOf(r);
    }

    private void up() {
        if (fv == null || !fv.canGoUp(here)) return;
        here = fv.up(here);
        showFolder();
    }

    /** התיקייה הנוכחית: "רק מה שעונה" — מהמודל; "כולל הכול" — מהאינדקס, וההתאמות מסומנות. */
    private void showFolder() {
        if (fv == null) return;
        rows.clear();
        boolean all = FolderView.ALL.equals(view);
        List<FolderView.Folder> dirs = new ArrayList<FolderView.Folder>();
        List<Library.Hit> files = new ArrayList<Library.Hit>();
        if (here == null) dirs.addAll(fv.top(!all));
        else if (!all) {
            dirs.addAll(fv.matchDirs(here));
            for (String p : fv.matchFiles(here)) {
                Library.Hit x = lib.find(p);
                if (x != null) files.add(x);
            }
        } else {
            List<File> ls = lib.listDir(new File(here), hidden, new Runnable() {   // השתנתה בכונן — מסונכרנת ומוצגת
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override public void run() { search(); }
                    });
                }
            });
            if (ls == null && !here.equals(warned)) {                           // 1.11.0: פעם אחת לתיקייה, ובמילים הנכונות
                warned = here;
                Ui.toast(this, new File(here).isDirectory() ? Words.Views.NO_ACCESS : Words.Views.NOT_FOUND);
            }
            else for (File f : ls) {
                String p = f.getAbsolutePath();
                if (f.isDirectory()) dirs.add(new FolderView.Folder(p, f.getName(), fv.matches(p)));
                else {
                    Library.Hit x = lib.find(p);
                    if (x != null) files.add(x);
                }
            }
        }
        if (here != null) java.util.Collections.sort(dirs, new java.util.Comparator<FolderView.Folder>() {
            @Override
            public int compare(FolderView.Folder a, FolderView.Folder b) {
                return a.name.compareToIgnoreCase(b.name);
            }
        });
        srt.apply(files, HIT);
        rows.addAll(dirs);
        rows.addAll(files);
        addInner();                                                           // 161
        StringBuilder w = new StringBuilder();
        if (here == null) w.append("📂 ").append(Words.Views.SOURCES);
        else {
            if (fv.canGoUp(here)) w.append(Words.Views.UP).append("  ");
            String root = fv.rootOf(here);
            w.append(root == null ? here : rootLabel(root));
            if (root != null && here.length() > root.length())
                for (String part : here.substring(root.length() + 1).split("/")) w.append(" › ").append(part);
        }
        if (rows.isEmpty()) w.append("   ·   ").append(Words.Views.NO_MATCH_HERE);
        where.setText(w.toString());
        where.setVisibility(View.VISIBLE);
        adapter.notifyDataSetChanged();
        showClip();                                                           // 149: "הדבקה כאן" — לפי התיקייה
    }

    /**
     * 146 בתיקיות: אחרי פעולה מחשבים מחדש. ב"רק מה שעונה" — קובץ שהיה כאן ונשאר בתיקייה (שינוי שם) או נמחק,
     * ואינו עונה עוד — נשאר באפור עם הסיבה. קובץ שעבר לתיקייה אחרת — יוצא ממנה, כמו במנהל קבצים.
     */
    private void stayInFolder() {
        List<Library.Hit> before = new ArrayList<Library.Hit>();
        java.util.Map<String, FileItem.State> was = new java.util.HashMap<String, FileItem.State>(out);
        for (Object o : rows) if (o instanceof Library.Hit) before.add((Library.Hit) o);
        String dir = here;
        search();
        if (!FolderView.MATCH.equals(view) || dir == null || !dir.equals(here)) return;
        Set<String> shown = new java.util.HashSet<String>();
        for (Object o : rows) if (o instanceof Library.Hit) shown.add(((Library.Hit) o).path());
        for (Library.Hit old : before) {
            String p = old.path();
            FileItem.State prev = was.get(p);
            String now = prev != null && prev != FileItem.State.OUT ? p : Physical.whereNow(p);
            if (shown.contains(now)) continue;
            FileItem.State why = prev != null && prev != FileItem.State.OUT ? prev : FileItem.dropped(lib, now, true);
            if (why == FileItem.State.OUT) {
                if (!dir.equals(FolderView.parent(now))) continue;            // עבר לתיקייה אחרת
                Library.Hit at = lib.find(now);
                if (at == null) continue;
                rows.add(at);
                out.put(now, why);
            } else {
                rows.add(old);
                out.put(p, why);
            }
        }
        adapter.notifyDataSetChanged();
    }

    /** מקרא: ריבוע בצבע הסוג, השם והמספר. מוצג רק כשיש יותר מסוג אחד. */
    private void showLegend(Library.Result r) {
        int kinds = 0;
        for (int c : r.counts) if (c > 0) kinds++;
        if (kinds < 2) {
            legend.setVisibility(View.GONE);
            return;
        }
        android.text.SpannableStringBuilder sb = new android.text.SpannableStringBuilder();
        for (int i = 0; i < r.counts.length; i++) {
            if (r.counts[i] == 0) continue;
            int start = sb.length();
            sb.append("■ ");
            sb.setSpan(new android.text.style.ForegroundColorSpan(Library.MATCH_SWATCH[i]), start, start + 1, 0);
            sb.append(i == Library.NO_PREFIX ? Library.header(i, r) : Library.MATCH_TYPES[i]).append(" ")
                    .append(Files.count(r.counts[i])).append("     ");
        }
        legend.setText(sb);
        legend.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    /** 149: התפריט נבנה בכל פתיחה — חלק מהשורות תלויות במצב (תצוגה, תיקייה, מוסתרים). */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        menu.add(0, 1, 0, Words.SELECT);
        menu.add(0, 900, 90, Words.BATCH_MENU);                        // 127
        menu.add(0, 2, 1, Words.SORT);
        menu.add(0, 3, 2, Words.Look.SETTINGS_TITLE);
        menu.add(0, 4, 3, Words.FULL_SHORT);
        menu.add(0, 5, 4, Words.Views.MENU);                                  // 148
        if (FolderView.ALL.equals(view)) menu.add(0, 8, 7, hidden ? Words.Here.HIDE_HIDDEN : Words.Here.SHOW_HIDDEN);   // 149
        if (pickInto != null && folderMode() && here != null) menu.add(0, 9, 8, Words.Here.ADD_FOLDER);
        if (pickInto == null) {                                               // 153
            if (funcId != null || owner != null && Nested.kindOf(owner) != Nested.Kind.TOOL) menu.add(0, 6, 5, Words.Find.SAVE);
            menu.add(0, 7, 6, Words.Find.SAVE_AS);
            if (owner != null) menu.add(0, 10, 9, Words.Inner.ADD);           // 161
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 900) {                                       // 127
            CmdMenu.show(this, folderMode() && here != null ? new File(here) : null);   // 149: "פקודה חדשה" — כאן
            return true;
        }
        if (item.getItemId() == 1) {
            sel.flip();
            return true;
        }
        if (item.getItemId() == 10) {                                         // 161
            addInnerMenu();
            return true;
        }
        if (item.getItemId() == 8) {                                          // 149: מוסתרים, בפתיחה הזו
            hidden = !hidden;
            showFolder();
            return true;
        }
        if (item.getItemId() == 9 && pickInto != null && here != null) {      // 149: התיקייה כולה לרשימה
            Adder.folder(this, new File(here), pickInto, new Adder.Done() {
                @Override
                public void done() {
                    loadInList();
                    adapter.notifyDataSetChanged();
                }
            });
            return true;
        }
        if (item.getItemId() == 6 || item.getItemId() == 7) {                 // 153
            save(item.getItemId() == 7);
            return true;
        }
        if (item.getItemId() == 5) {                                          // 148: לפתיחה הזו; ברירת המחדל — בהגדרות הרשימה
            Ui.options(this, Words.Views.TITLE, FolderView.MODES, new android.content.DialogInterface.OnClickListener() {
                @Override
                public void onClick(android.content.DialogInterface d, int w) {
                    view = FolderView.MODES[w];
                    placed = false;
                    here = null;
                    search();
                }
            });
            return true;
        }
        if (item.getItemId() == 4) {
            Ui.setFull(this, !Ui.full(this));
            adapter.notifyDataSetChanged();
            return true;
        }
        if (item.getItemId() == 3) {
            final boolean[] on = {paths, loose};
            new android.app.AlertDialog.Builder(this)
                    .setTitle(Words.Look.CURRENT_SETTINGS)
                    .setMultiChoiceItems(new String[]{Words.Look.IN_PATH_TOO, Words.Look.LOOSE_SPELLING},
                            on, new android.content.DialogInterface.OnMultiChoiceClickListener() {
                                @Override
                                public void onClick(android.content.DialogInterface d, int w, boolean v) { on[w] = v; }
                            })
                    .setPositiveButton(Words.OK, new android.content.DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(android.content.DialogInterface d, int w) {
                            paths = on[0];
                            loose = on[1];
                            saveSetting(SmartLists.PATHS, paths ? "כן" : "לא");  // תחביר
                            saveSetting(SmartLists.LOOSE, loose ? "כן" : "לא");  // תחביר
                            search();
                        }
                    })
                    .setNegativeButton(Words.CANCEL, null)
                    .show();
            return true;
        }
        if (item.getItemId() == 2) {
            srt.menu(this, new Runnable() {
                @Override public void run() { search(); }
            });
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (sel.back()) return;
        if (folderMode() && fv != null && fv.canGoUp(here)) {                 // 148: בתיקיות — למעלה
            up();
            return;
        }
        super.onBackPressed();
    }
}
