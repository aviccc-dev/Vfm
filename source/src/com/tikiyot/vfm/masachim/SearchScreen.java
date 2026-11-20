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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SearchScreen extends Activity implements Clip.Holder, PlaceChip.Place {

    public static final String EXTRA_QUERY = "query";
    public static final String EXTRA_FIXED = "fixed";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_SORT = "sort";
    public static final String EXTRA_VIEW = "view";         // 148: ברצף / בתיקיות — ברירת המחדל של הרשימה
    public static final String EXTRA_DIR = "dir";           // 159: בתיקיות — מאיפה מתחילים
    public static final String EXTRA_HIDDEN = "hidden";     // 159: מוסתרים מוצגים
    /** מבנה 283: מקום החיפוש — התנאי (רשימה: / מקור:) והשם שעל השבב. */
    static final String EXTRA_PLACE = "place", EXTRA_PLACE_NAME = "placeName";
    /** מבנה 287: רשימה חכמה של המסד — האופן והמספר שלה (כדי לשמור אליה מהחלונית). */
    static final String EXTRA_SMART_MODE = "smartMode", EXTRA_SMART_ID = "smartId";
    /** מבנה 291: רשימה חכמה חדשה — האופן והרשימה שבתוכה תיווצר ב"💾 שמירה". */
    static final String EXTRA_NEW_MODE = "newMode", EXTRA_NEW_IN = "newIn";

    /**
     * מבנה 292: "לבחור" — העורך (דרך הגשר) מבקש תנאי לשורה "צור רשימה חכמה": אותה חלונית (פתוחה), ו"✓ לבחור" מחזיר את השם, התנאי,
     * המיון והתצוגה (PICKED_*), בלי לשמור דבר. עד כאן — הטופס הנפרד (DbSmartForm.ask).
     */
    static final String EXTRA_PICK = "pick", PICKED_NAME = "pickedName", PICKED_COND = "pickedCond", PICKED_SORT = "pickedSort",
            PICKED_VIEW = "pickedView";

    static android.content.Intent pick(android.content.Context c, String name, String cond, String sort) {
        android.content.Intent i = new android.content.Intent(c, SearchScreen.class);
        i.putExtra(EXTRA_PICK, true);
        i.putExtra(EXTRA_QUERY, cond == null ? "" : cond.trim());
        if (sort != null && !sort.trim().isEmpty()) i.putExtra(EXTRA_SORT, sort.trim());
        i.putExtra(EXTRA_TITLE, name == null || name.trim().isEmpty() ? Words.SMART_LIST : name.trim());
        return i;
    }

    /** מבנה 291: רשימה חכמה חדשה בתוך in — בחלונית של החיפוש (פתוחה), ו"💾 שמירה" יוצרת אותה. */
    static void newSmart(Activity from, String mode, long in) {
        android.content.Intent i = new android.content.Intent(from, SearchScreen.class);
        i.putExtra(EXTRA_NEW_MODE, mode);
        i.putExtra(EXTRA_NEW_IN, in);
        i.putExtra(EXTRA_TITLE, Words.Find.NEW_SMART_TITLE);
        from.startActivity(i);
    }

    /** מבנה 283: "פתח חיפוש" — מהמסך שעליו עומדים, עם המקום שלו. */
    static void open(Activity from) {
        android.content.Intent i = new android.content.Intent(from, SearchScreen.class);
        String[] p = from instanceof PlaceChip.Place ? ((PlaceChip.Place) from).searchPlace() : null;
        if (p != null && p[0] != null && !p[0].isEmpty()) {
            i.putExtra(EXTRA_PLACE, p[0]);
            i.putExtra(EXTRA_PLACE_NAME, p[1]);
        }
        from.startActivity(i);
    }

    /** מבנה 283: מכאן — התיקייה שעומדים בה (בתצוגת תיקיות). */
    @Override
    public String[] searchPlace() {
        if (!fb.folderMode() || here == null) return null;
        return new String[]{Cond.inFolder(here), PlaceChip.folderLabel(this, here)};
    }

    /**
     * 159 — "מנהל קבצים": רשימה חכמה בלי סינון, "בתיקיות — כולל הכול" (במקום המסך הישן).
     * כל הכוננים; dir — התיקייה שנפתחת (או null).
     */
    static android.content.Intent fileManager(android.content.Context c, String dir) {
        android.content.Intent i = new android.content.Intent(c, SearchScreen.class);
        i.putExtra(EXTRA_QUERY, Sources.toCond(java.util.Arrays.asList(Sources.INTERNAL, Sources.CARD)));
        i.putExtra(EXTRA_VIEW, FolderView.ALL);
        i.putExtra(EXTRA_FIXED, true);
        i.putExtra(EXTRA_SORT, Sort.NAME);
        i.putExtra(EXTRA_TITLE, SmartLists.FILE_MANAGER);
        if (dir != null) i.putExtra(EXTRA_DIR, dir);
        return i;
    }

    Library lib;
    private final Handler h = new Handler(Looper.getMainLooper());
    /** מבנה 280: התצוגה בתיקיות ופס הלוח — בקובץ משלהם. */
    private final FolderBrowse fb = new FolderBrowse(this);
    final List<Object> rows = new ArrayList<Object>();
    private Set<String> selected = new LinkedHashSet<String>();
    private Select sel;
    /** 146: שורות שאחרי פעולה כבר אינן עונות — נשארות באפור עד רענון (מסך יציב). המפתח: המסלול שבשורה. */
    final java.util.Map<String, FileItem.State> out = new java.util.HashMap<String, FileItem.State>();
    private String infoBase = "";
    /** 175: מוצגות תיקיות ריקות (ו"בחירת הכול" בוחרת אותן). */
    boolean emptyMode;
    /** 148: התצוגה (ברירת המחדל מהרשימה; "תצוגה…" בתפריט — לפתיחה הזו בלבד), והתיקייה שבה עומדים. */
    String view = FolderView.FLAT;
    FolderView fv;
    /** התיקייה הנוכחית; null — רשימת המקורות. */
    String here;
    boolean placed;
    /** 159: התיקייה שבה נפתחים (מבחוץ: "פתיחת התיקייה", הדבקה). */
    private String startDir;
    TextView where;
    /** 153 — החיפוש האחד: ההגדרות (מקורות, סוגים, תאריך, גודל) בחלונית במסך, וחלות מיד; המילים — בשורת ההקלדה. */
    SmartForm.Parts parts;
    private android.widget.ScrollView panel;
    private LinearLayout head;                                               // מבנה 273: מה שבין שורת החיפוש לקבצים — מתקפל עם השורות (ScrollRoom.head)
    private TextView settings;
    boolean hidden;
    /** סימון התאמה ב"כולל הכול". */
    private static final int MATCH_BG = 0xFFE3F2E6;
    BaseAdapter adapter;
    EditText input;
    private TextView info;
    private SelectBar bar;
    /** ההסבר "נגיעה ארוכה…" — רק בפתיחות הראשונות. */
    private boolean hint;
    private String queryNorm = "", fixedQuery;
    Sort srt;
    private boolean fixed, paths, loose = true;
    /** מבנה 283: מקום החיפוש (null — אין), ו"בכל המכשיר" במקומו. */
    private String place, placeName;
    private boolean wide;
    /** מבנה 290: השמירה מהמסך (שמירה, שמירה בשם, טעינת תבנית) — בקובץ משלה. */
    final SearchSave saves = new SearchSave(this);
    /** מבנה 284: היסטוריית החיפוש (רק בחיפוש — לא ברשימה חכמה). */
    private HistoryBox history;
    /** מבנה 298: "לא נמצא" — בתוך אזור התוצאות. */
    private TextView nothing;
    /** מבנה 286: המסננים הפעילים — שבבים עם ✕. */
    private FilterChips chips;
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


    /** מ0.41: איזה מסך זה, לשורות שלו — "קבצים וירטואליים", רשימה חכמה (תנאי קבוע), או חיפוש פתוח. מהפתיחה עצמה (לפני onCreate). */
    String rowsScreen() {
        android.content.Intent i = getIntent();
        if (SmartLists.FILE_MANAGER.equals(i.getStringExtra(EXTRA_TITLE))) return Verbs.S_VIRTUAL;
        return i.getBooleanExtra(EXTRA_FIXED, false) ? Verbs.S_SMART_SCREEN : Verbs.S_SEARCH;
    }
    /** 1.13.0: שורת "למה". */
    private String whyText = "", whyKey = "";

    /** הגדרה של המסך — בהגדרות הכלליות של החיפוש (מ0.29: אין עוד סעיף לכל כלי). */
    private String setting(String key, String def) {
        return Conf.get(Conf.SEARCH, key, def);
    }

    private void saveSetting(final String key, final String value) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Conf.set(Conf.SEARCH, key, value);
            }
        }).start();
    }
    private TextView legend;

    @Override
    protected void onCreate(Bundle saved) {
        Ui.begin(this, false);                                     // מבנה 238: הצבעים מהעיצוב
        super.onCreate(saved);
        lib = Library.get(this);
        readIntent();

        if (srt == null) srt = Sort.withAdded(Sort.RANK, true, Sort.tool(Conf.SEARCH, ""));
        LinearLayout root = Ui.screen(this);
        buildQuery(root);
        buildInfo(root);
        buildList(root);
        buildSelection();
        fb.buildClipBar(root);
        root.addView(bar.view);
        setContentView(TopRow.wrap(this, root));                   // 1.9.0: שורת הכפתורים העליונה

        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) {
                if (history != null) history.show(s.toString().trim().isEmpty());   // מבנה 284
                schedule();
            }
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
        String v = getIntent().getStringExtra(EXTRA_VIEW);
        if (v != null) view = v;
        startDir = getIntent().getStringExtra(EXTRA_DIR);
        hidden = getIntent().getBooleanExtra(EXTRA_HIDDEN, false);
        place = fixed ? null : getIntent().getStringExtra(EXTRA_PLACE);       // מבנה 283: רשימה חכמה — התנאי שלה, בלי מקום
        placeName = getIntent().getStringExtra(EXTRA_PLACE_NAME);
        saves.read(getIntent());                                              // מבנה 287: הרשימה החכמה שעל המסך
        wide = "כן".equals(setting(SmartLists.WHOLE, "לא"));  // תחביר
        paths = "כן".equals(setting(SmartLists.PATHS, "לא"));  // תחביר
        loose = !"לא".equals(setting(SmartLists.LOOSE, "כן"));  // תחביר
        Sort.Store st = Sort.tool(Conf.SEARCH, "");
        srt = Sort.withAdded(Sort.RANK, true, st);
        if (extraSort != null && st.get(SmartLists.SORT, "").isEmpty()) srt.prefer(extraSort);
        if (fixed && getWindow() != null)
            getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        String title = getIntent().getStringExtra(EXTRA_TITLE);
        whyText = SmartLists.FILE_MANAGER.equals(title) ? Words.WhyW.VIRTUAL_FILES : "";
        whyKey = Words.WhyW.KEY_SCREEN + title;
        setTitle(title != null && !title.isEmpty() ? title : Words.Look.SEARCH);
    }

    /** שורת החיפוש וחלונית ההגדרות (153). */
    private void buildQuery(LinearLayout root) {
        String[] split = SmartLists.split(fixedQuery);                        // 153: הגדרות לחלונית, מילים לשורה
        input = Ui.field(this, split[1], Words.Find.HINT);
        input.setTextSize(18);
        input.setPadding(Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 12), Ui.dp(this, 10));
        root.addView(SmartForm.fieldRow(this, input));                        // מבנה 281: נגיעה בוחרת, ✕ מנקה
        head = ScrollRoom.head(this);
        root.addView(head, new LinearLayout.LayoutParams(-1, -2));
        if (place != null)                                                    // מבנה 283: "מחפש ב: <המקום> ▾"
            head.addView(new PlaceChip(this, placeName == null ? "" : placeName, wide, wide, new PlaceChip.Host() {
                @Override public void changed(boolean w, boolean always) {
                    wide = w;
                    saveSetting(SmartLists.WHOLE, always ? "כן" : "לא");  // תחביר
                    search();
                }
            }).view);
        if (!fixed) {                                                          // מבנה 284: היסטוריה — מתחת לשורה ריקה
            int max = SmartLists.HISTORY_DEFAULT;
            try {
                max = Integer.parseInt(setting(SmartLists.HISTORY_MAX, String.valueOf(max)).trim());
            } catch (NumberFormatException e) {
                // לא מספר בקובץ ההגדרות — ברירת המחדל
            }
            history = new HistoryBox(this, Search.historyOf(setting(SmartLists.HISTORY, "")), max, new HistoryBox.Host() {
                @Override public void use(String w) {
                    input.setText(w);
                    input.setSelection(w.length());
                }
                @Override public void saved(List<String> list) { saveSetting(SmartLists.HISTORY, Search.historyText(list)); }
            });
            history.show(input.getText().toString().trim().isEmpty());
            head.addView(history.view);
            input.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
            input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override public boolean onEditorAction(TextView v, int action, android.view.KeyEvent e) {
                    remember();
                    ((android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(), 0);
                    return true;
                }
            });
        }
        quick = new QuickBar(this, new QuickBar.Host() {                       // 181: כפתורי תצוגה מהירה
            @Override public String base() { return query(); }
            @Override public int count(String c) { return find(c).total; }
            @Override public List<Library.Hit> hits(String c) { return find(c).hits; }
            @Override public String loadState() {
                String v = setting(Quick.STATE, "");
                return v.isEmpty() ? null : v;
            }
            @Override public void saveState(String s) { saveSetting(Quick.STATE, s.isEmpty() ? Quick.SEP : s); }
            @Override public void changed() { search(); }
        });
        head.addView(quick.view);
        parts = new SmartForm.Parts(this, Words.Find.ALL_DRIVES);
        parts.load(split[0]);
        chips = new FilterChips(this, parts);                                 // מבנה 286
        head.addView(chips.view);
        LinearLayout box = Ui.form(this);
        box.addView(PanelTools.row(this, saves.mode(), new PanelTools.Host() {   // מבנה 289
            @Override public void load(String c, String s, String v) { saves.load(c, s, v); }
            @Override public void sort() {
                SortUi.menu(SearchScreen.this, srt, new Runnable() {
                    @Override public void run() { search(); }
                });
            }
            @Override public void view() { pickView(); }
            @Override public void saveAs() { saves.saveAs(); }
            @Override public String hereLabel() { return saves.picking() ? Words.Find.PICK : Words.Find.SAVE_HERE_B; }   // מבנה 292
            @Override public Runnable saveHere() {
                return !saves.onSmart() ? null : new Runnable() {
                    @Override public void run() { saves.saveHere(); }
                };
            }
        }));
        parts.addTo(this, box);
        if (history != null) box.addView(history.countRow(new Runnable() {    // מבנה 284
            @Override public void run() { saveSetting(SmartLists.HISTORY_MAX, String.valueOf(history.max())); }
        }));
        panel = new android.widget.ScrollView(this);
        panel.addView(box);
        boolean open = saves.isNew();                                         // מבנה 291: רשימה חכמה חדשה — החלונית פתוחה
        panel.setVisibility(open ? View.VISIBLE : View.GONE);
        settings = Ui.info(this);
        settings.setBackgroundColor(Ui.BG_SOFT);
        settings.setText(Words.Find.SETTINGS + (open ? " ▾" : " ▸"));
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean open = panel.getVisibility() != View.VISIBLE;
                panel.setVisibility(open ? View.VISIBLE : View.GONE);
                settings.setText(Words.Find.SETTINGS + (open ? " ▾" : " ▸"));
            }
        });
        head.addView(settings);
        head.addView(panel, new LinearLayout.LayoutParams(-1, Ui.dp(this, 300)));
    }

    /** שורת המידע (נגיעה — רענון, 110), המקרא, ו"איפה עומדים" (148). */
    private void buildInfo(LinearLayout root) {
        info = Ui.info(this);
        ScrollRoom.oneLine(info);                                             // מבנה 273: שורה אחת, עם "…" — נגיעה פותחת את כולה
        info.setOnClickListener(new View.OnClickListener() {                  // 110: רענון נגיש
            @Override
            public void onClick(View v) {
                if (!input.getText().toString().trim().isEmpty()) { ScrollRoom.wide(info); return; }   // מבנה 273: יש חיפוש — פותחת / סוגרת את השורה
                Ui.toast(SearchScreen.this, Words.Look.REFRESHING);
                final java.util.Map<String, String> vars = BatchUi.vars(SearchScreen.this, Paths.work());
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        // מבנה 120: דרך המנהל — "רענן", כמו בקובץ פקודות
                        Manager.Act act = Manager.onIndex(Manager.INDEX_QUICK, Paths.work(), vars, lib);
                        final String said = act.error() != null ? act.error() : act.say(lib);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Ui.toast(SearchScreen.this, said);
                                search();
                            }
                        });
                    }
                }).start();
            }
        });
        hint = Ui.firstTimes(this, Words.Look.SEARCH);
        TextView whyBar = Why.bar(this);                                      // 1.13.0
        head.addView(whyBar);
        Why.show(whyBar, whyKey, whyText);
        head.addView(info);
        legend = Ui.info(this);
        legend.setVisibility(View.GONE);
        head.addView(legend);
        where = Ui.info(this);                                                // 148: איפה עומדים; נגיעה — למעלה
        where.setBackgroundColor(Ui.BG_SOFT);
        where.setVisibility(View.GONE);
        where.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { fb.up(); }
        });
        head.addView(where);
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
                if (o instanceof FolderView.Folder) {                         // 148: תיקייה בתצוגה בתיקיות
                    FolderView.Folder f = (FolderView.Folder) o;
                    boolean top = here == null;
                    boolean missing = top && !fb.volRoots.contains(f.path) && !new File(f.path).isDirectory();   // 1.11.0: מקור שאינו בכונן
                    Ui.bindIcon(v, missing ? "⚠" : top && fb.volRoots.contains(f.path) ? Drive.icon(Drive.internalPath(f.path)) : "📁",
                            top ? fb.rootLabel(f.path) : f.name, missing ? Words.Views.NOT_FOUND : f.matches > 0 ? Words.Views.matches(f.matches) : "");
                    v.setBackgroundColor(sel.hasDir(f.path) ? Ui.SELECTED
                            : FolderView.ALL.equals(view) && fv != null && fv.isMatch(f.path) ? MATCH_BG : 0x00000000);
                    return v;
                }
                Library.Hit hit = (Library.Hit) o;
                FileItem.State gone = out.get(hit.path());
                boolean marked = FolderView.ALL.equals(view) && fv != null && here != null;   // 148: "כולל הכול" — מסמנים התאמות
                boolean match = marked && fv.isMatch(hit.path());
                String status = gone != null ? gone.note : null;
                if (match) status = status == null ? "✓" : "✓ · " + status;
                if (hit.section > 0) status = (status == null ? "" : status + " · ")
                        + Library.MATCH_TYPES[hit.section];
                if (hit.innerEmpty > 0) status = (status == null ? "" : status + " · ") + Words.Empty.inside(hit.innerEmpty);   // 175
                boolean byAdded = Sort.ADDED.equals(srt.by) && hit.rec != null;          // 189: התאריך שבשורה — של ההוספה
                if (byAdded) status = (status == null ? "" : status + " · ")
                        + (hit.rec.added > 0 ? Words.Look.ADDED : Words.Look.ADDED_UNKNOWN);
                else if (hit.rec != null && Ui.full())                       // 189: בנתונים המלאים — תמיד
                    status = (status == null ? "" : status + " · ")
                            + (hit.rec.added > 0 ? Words.Look.ADDED + " " + Ui.date(hit.rec.added) : Words.Look.ADDED_UNKNOWN);
                Ui.bindRow(v, hit.name(), hit.isDir(),
                        hit.rec != null ? hit.rec.size : 0, hit.rec == null ? 0 : byAdded ? hit.rec.added : hit.rec.mtime,
                        hit.path(), status, queryNorm);
                if (selected.contains(hit.path()) || sel != null && sel.hasDir(hit.path()))
                    v.setBackgroundColor(Ui.SELECTED);
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
                if (o instanceof Library.Hit) tap((Library.Hit) o);
                else if (o instanceof FolderView.Folder) {                    // 148: נכנסים לתיקייה
                    FolderView.Folder f = (FolderView.Folder) o;
                    if (sel.on()) sel.toggleDir(f.path);
                    else {
                        here = f.path;
                        fb.showFolder();
                    }
                }
            }
        });
        list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> a, View v, int pos, long id) {
                Object o = rows.get(pos);
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
        nothing = Ui.info(this);                                              // מבנה 298: "לא נמצא" — בתוך אזור התוצאות
        nothing.setGravity(android.view.Gravity.CENTER);
        nothing.setPadding(Ui.dp(this, 16), Ui.dp(this, 24), Ui.dp(this, 16), Ui.dp(this, 24));
        nothing.setVisibility(View.GONE);
        root.addView(nothing, new LinearLayout.LayoutParams(-1, -2));
        root.addView(list, new LinearLayout.LayoutParams(-1, 0, 1f));
    }

    /**
     * מבנה 298 (המשתמש, 20.11, צילום של מ0.52: המסך ריק אחרי vfm*2): כשלא נמצא כלום — נאמר בתוך אזור התוצאות, ולא רק בשורת המידע
     * (שמתקפלת עם הרצועה). עם ההערה של החיפוש, אם יש (רשימה שלא נמצאה, מה שלא הובן).
     */
    private void sayNothing(boolean show, String note) {
        if (!show) {
            nothing.setVisibility(View.GONE);
            return;
        }
        nothing.setText(Words.Find.nothing(input.getText().toString().trim(), note));
        nothing.setVisibility(View.VISIBLE);
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
        sel.actions(this, Actions.Tool.SEARCH, new Runnable() {
            @Override public void run() { stay(); }                            // 146: מסך יציב אחרי פעולה
        }, java.util.Collections.<Actions.Action, Runnable>singletonMap(Actions.ADD_TO_LIST, new Runnable() {
            @Override public void run() { addSelected(); }
        }));
    }

    /** 148: התצוגה — לפתיחה הזו (מהתפריט ומהחלונית — מבנה 289: מקום אחד). */
    private void pickView() {
        Ui.options(this, Words.Views.TITLE, FolderView.MODES, new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(android.content.DialogInterface d, int w) {
                view = FolderView.MODES[w];
                placed = false;
                here = null;
                search();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        lib.start(null);
        lib.returned();                                                  // 110
        search();
        fb.showClip();
    }

    /** אחרי "העתקה/העברה" מתוך הרשימה: נשארים, ובוחרים איך להדביק (במקום לעבור לכלי אחר). */
    @Override
    public void clipped() {
        fb.showClip();
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
                            fb.showClip();
                        } else if (w == 1) fb.pastePick();
                    }
                });
    }

    @Override
    protected void onPause() {
        remember();                                                      // מבנה 284: יצאו מהמסך אחרי חיפוש
        lib.left();                                                      // 110
        super.onPause();
    }

    // ---------- נגיעות ובחירה ----------

    /** מבנה 284: חיפוש שנעשה — המילים שבשורה (בלי המקום וההגדרות) נכנסות להיסטוריה. */
    private void remember() {
        if (history != null) history.remember(input.getText().toString());
    }

    private void tap(Library.Hit hit) {
        remember();                                                           // מבנה 284: פתחו תוצאה
        FileItem.State gone = out.get(hit.path());
        if (gone != null && gone != FileItem.State.OUT) {                     // 146: כבר אינו כאן
            Ui.toast(this, gone.line);
            return;
        }
        if (sel.on()) {
            if (hit.isDir()) sel.toggleDir(hit.path()); else toggle(hit);
        } else if (hit.isDir()) {
            startActivity(fileManager(this, hit.path()));                     // 159
        } else {                                                              // 1.13.1: קובץ פקודות — כקובץ (תפריט: הרצה / עריכה)
            Ui.open(this, new File(hit.path()));
        }
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
        Adder.many(this, folders, new ArrayList<String>(selected), new Adder.Done() {   // (76)
            @Override
            public void done() {
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
    /**
     * מבנה 94: רשימה חכמה שנפתחה כאן (fixed) — רק מה שעונה על התנאי במלואו, כמו הספירה שלידה ופקודות על הקבצים שלה. חיפוש רגיל —
     * כמו שהיה, עם הקבוצות של "אולי התכוונת" (בלי אות השימוש, כתיב חסר, חלק מהמילים) והכותרות שלהן.
     */
    private Library.Result find(String q) {
        return fixed ? lib.list(q) : lib.search(q, paths, loose);
    }

    private String searchQuery() {
        String q = query();
        return q.trim().isEmpty() || quick == null ? q : SmartLists.join(q, quick.cond());
    }

    /** 153: התנאי — ההגדרות שבחלונית ועוד מה שבשורת ההקלדה. */
    String query() {
        String q = SmartLists.withHere(SmartLists.join(parts == null ? "" : parts.build(), input.getText().toString()), hereDir());
        return q.trim().isEmpty() || place == null || wide ? q : SmartLists.join(place, q);   // מבנה 283: המקום — רק כשמחפשים משהו
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

    void search() {
        if (chips != null) chips.refresh();                                   // מבנה 286
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
            sayNothing(false, "");
            info.setText(Files.count(lib.totalFiles()) + Words.Look.FILES_IN_INDEX
                    + Files.when(lib.lastBuilt()));
            return;
        }
        Library.Result r = find(q);
        emptyMode = Cond.emptyDirsIn(r.query);                              // 175: תיקיות ריקות — תמיד ברצף (מבנה 283: בלי לפרק שוב)
        queryNorm = r.query;
        out.clear();                                                          // חיפוש מחדש = רענון: האפורים יורדים
        if (fb.folderMode()) {                                                   // 148
            legend.setVisibility(View.GONE);
            sayNothing(false, "");
            fv = new FolderView(r.hits, fb.roots());
            if (!placed || here != null && fv.rootOf(here) == null) {
                here = startDir != null && fv.rootOf(startDir) != null ? startDir : fv.start();   // 159
                placed = true;
            }
            infoBase = Words.Views.matches(fv.total()) + " · " + srt.describe()
                    + (r.note.isEmpty() ? "" : " · " + r.note) + (r.loading ? Words.Look.STILL_LOADING : "");
            info.setText(infoBase);
            fb.showFolder();
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
        adapter.notifyDataSetChanged();
        sayNothing(rows.isEmpty() && !r.loading, r.note);                     // מבנה 298
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
        if (fb.folderMode()) {                                                   // 148: בתיקיות
            fb.stayInFolder();
            return;
        }
        String q = searchQuery();
        if (q.trim().isEmpty() || !lib.ready || rows.isEmpty()) {
            search();
            return;
        }
        Library.Result r = find(q);
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
        if (!rows.isEmpty()) sayNothing(false, "");                           // מבנה 298: ברענון נוסף משהו
        java.util.Map<FileItem.State, Integer> n = new java.util.LinkedHashMap<FileItem.State, Integer>();
        for (FileItem.State s : grey.values()) n.put(s, n.containsKey(s) ? n.get(s) + 1 : 1);
        StringBuilder sb = new StringBuilder(infoBase);
        for (java.util.Map.Entry<FileItem.State, Integer> e : n.entrySet()) sb.append(" · ").append(e.getKey().count(e.getValue()));
        info.setText(sb.toString());
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
        if (saves.onSmart()) menu.add(0, 9, 5, saves.picking() ? Words.Find.PICK : Words.Find.saveHere(getTitle().toString()));   // מבנה 287, 292
        menu.add(0, 7, 6, Words.Find.SAVE_AS);                                // 153
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 900) {                                       // 127
            CmdMenu.show(this, fb.folderMode() && here != null ? new File(here) : null);   // 149: "פקודה חדשה" — כאן
            return true;
        }
        if (item.getItemId() == 1) {
            sel.flip();
            return true;
        }
        if (item.getItemId() == 8) {                                          // 149: מוסתרים, בפתיחה הזו
            hidden = !hidden;
            fb.showFolder();
            return true;
        }
        if (item.getItemId() == 7) {                                          // 153: "שמירה בשם"
            saves.saveAs();
            return true;
        }
        if (item.getItemId() == 9) {                                          // מבנה 287: "💾 שמירה" — אל הרשימה החכמה שעל המסך
            saves.saveHere();
            return true;
        }
        if (item.getItemId() == 5) {                                          // 148: לפתיחה הזו; ברירת המחדל — בהגדרות הרשימה
            pickView();
            return true;
        }
        if (item.getItemId() == 4) {
            Ui.setFull(!Ui.full());
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
            SortUi.menu(this, srt, new Runnable() {
                @Override public void run() { search(); }
            });
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (sel.back()) return;
        if (fb.folderMode() && fv != null && fv.canGoUp(here)) {                 // 148: בתיקיות — למעלה
            fb.up();
            return;
        }
        super.onBackPressed();
    }
}
