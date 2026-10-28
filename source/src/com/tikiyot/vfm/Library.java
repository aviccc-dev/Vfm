package com.tikiyot.vfm;

// שכבת הליבה: כל פעולה עוברת דרכה. אחראית: להחזיק את האינדקסים, להפעיל, לחפש, ולבצע פעולות על קבצים.
// אסור לה: לבנות מסכים או לפרש הגדרות.

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class Library {

    /**
     * 95: בלי תקרה בפועל. עד 1.7.18 הוצגו רק 400 הראשונים בכל דרגה (לפי סדר האינדקס), והמיון חל רק עליהם —
     * מיון לפי גודל לא הראה את הגדולים באמת, וקובץ חדש בתיקייה גדולה לא הופיע. הרשימה במסך עצלה,
     * כך שאלפי תוצאות אינן בעיה. (רק קבוצת "חלק מהמילים" נשארת מוגבלת ל-150 — היא השלמה, לא תוצאה.)
     */
    public static final int CAP_PER_RANK = 1000000;

    /** סוגי ההתאמה. סוג חדש נוסף כאן, עם שם וצבע, והמסכים לוקחים ממנו. */
    public static final String[] MATCH_TYPES = {Words.Lib.MATCH_FULL, Words.Lib.MATCH_NO_PREFIX, Words.Lib.MATCH_LOOSE, Words.Lib.MATCH_PARTIAL};
    /** כותרת הקבוצה ברשימת התוצאות (לקבוצת "בלי אות השימוש" נוספת האות — header()). */
    public static final String[] MATCH_HEADERS = {null, Words.Lib.MATCH_NO_PREFIX, Words.Lib.MATCH_LOOSE, Words.Lib.MATCH_PARTIAL_HEADER};
    public static final int[] MATCH_COLORS = {0x00000000, 0xFFE6F4EA, 0xFFFFF1C2, 0xFFD9E8FB};
    public static final int[] MATCH_SWATCH = {0xFFBDBDBD, 0xFF34A853, 0xFFF2B52B, 0xFF5B9BE0};
    /** מספרי הקבוצות (78): מלאות · בלי אות השימוש · כתיב חסר · חלק מהמילים. */
    public static final int FULL = 0, NO_PREFIX = 1, LOOSE = 2, PARTIAL = 3;

    /** כותרת הקבוצה: "בלי אות השימוש (מ')" — עם האות שהוסרה. */
    public static String header(int section, Result r) {
        String h = MATCH_HEADERS[section];
        if (section == NO_PREFIX && r != null && !r.prefix.isEmpty()) h += " (" + r.prefix + ")";
        return h;
    }

    private static Library instance;

    public final Context app;
    /** תמונת מצב: מוחלפת כולה ברגע אחד, כך שהחיפוש לעולם לא קורא טבלה בזמן שהיא נבנית. */
    public volatile List<Index> indexes = new ArrayList<Index>();
    public volatile boolean ready, busy;
    public volatile String status = "";
    private volatile boolean started;

    private Library(Context c) {
        app = c.getApplicationContext();
    }

    /** לבדיקות בלבד (80, בדיקות): ספרייה בלי אנדרואיד, על אינדקסים נתונים. */
    private Library() {
        app = null;
    }

    static Library forTests(List<Index> ix) {
        Library l = new Library();
        l.indexes = ix;
        l.ready = true;
        return l;
    }

    /** 1.10.0: המופע הקיים, בלי ליצור (למקומות בלי Context); null — עוד לא נוצר. */
    static synchronized Library peek() {
        return instance;
    }

    public static synchronized Library get(Context c) {
        if (instance == null) instance = new Library(c);
        return instance;
    }

    private SharedPreferences prefs() {
        return app.getSharedPreferences("vfm", Context.MODE_PRIVATE);
    }

    /** 110: רמות העדכון — בקובץ ההגדרות של אופן העבודה (ולא בזיכרון אנדרואיד). */
    public static final String LEVEL_KEY = "עדכון האינדקס", PERIODIC_KEY = "רענון תקופתי בדקות";  // תחביר
    public static final String[] LEVELS = {"ידני בלבד", "בכל הפעלה של התוכנה", "בכל חזרה אל התוכנה"};  // תחביר

    /** 1 = ידני · 2 = בכל הפעלה · 3 = בכל חזרה אל התוכנה (ברירת המחדל). */
    public int updateLevel() {
        String v = Conf.get(Conf.GENERAL, LEVEL_KEY, "");
        for (int i = 0; i < LEVELS.length; i++) if (LEVELS[i].equals(v)) return i + 1;
        return prefs().getInt("level", 3) == 1 ? 1 : 3;              // מעבר: מי שבחר ידני — נשאר ידני
    }

    public void setUpdateLevel(int v) {
        Conf.set(Conf.GENERAL, LEVEL_KEY, LEVELS[Math.max(1, Math.min(3, v)) - 1]);
    }

    /** רענון תקופתי כשהתוכנה פתוחה — בדקות; 0 = כבוי (ברירת המחדל). */
    public int periodicMinutes() {
        try {
            return Math.max(0, Integer.parseInt(Conf.get(Conf.GENERAL, PERIODIC_KEY, "0").trim()));
        } catch (Throwable t) {
            return 0;
        }
    }

    public void setPeriodicMinutes(int m) {
        Conf.set(Conf.GENERAL, PERIODIC_KEY, String.valueOf(Math.max(0, m)));
    }

    // ---------- 110: בדיקה בכל חזרה, ורענון תקופתי ----------

    private volatile long lastCheck;
    private int activeScreens;
    /** נוצר רק כשמסך חוזר לחזית (לא בבדיקות במחשב). */
    private android.os.Handler ticker;

    private android.os.Handler ticker() {
        if (ticker == null) ticker = new android.os.Handler(android.os.Looper.getMainLooper());
        return ticker;
    }
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            int m = periodicMinutes();
            if (activeScreens > 0 && m > 0) {
                checkNow(0);
                ticker().postDelayed(this, m * 60_000L);
            }
        }
    };

    /**
     * 1.7.73: הבדיקה הזולה (תאריכי התיקיות, 0.02 שניות) — בכל חזרה ובכל מעבר מסך; מדלגים רק אם בדיקה רצה עכשיו
     * או הסתיימה לפני פחות משנייה ("בודקים תמיד, מרעננים רק כשצריך", 178).
     */
    static final long CHECK_GAP = 1000L;

    /** מסך של התוכנה חזר לחזית. ברמה 3 — בדיקה, ותזמון הרענון התקופתי. */
    public void returned() {
        activeScreens++;
        if (app != null) Sig.kick(this);                                     // 190: חתימות — ברקע, אם יש מה
        if (updateLevel() == 3) checkNow(CHECK_GAP);
        ticker().removeCallbacks(tick);
        int m = periodicMinutes();
        if (m > 0) ticker().postDelayed(tick, m * 60_000L);
    }

    /** מסך יצא מהחזית. */
    public void left() {
        activeScreens = Math.max(0, activeScreens - 1);
        if (activeScreens == 0 && ticker != null) ticker.removeCallbacks(tick);
        if (activeScreens == 0 && app != null && ready && Sig.needed(this)) SigJob.schedule(app);   // 194: להמשיך כשהתוכנה סגורה
    }

    /** בדיקת תיקיות ברקע, אם לא רצה כבר בנייה או רענון, ואם עבר minGap מהבדיקה הקודמת. */
    public void checkNow(long minGap) {
        long now = System.currentTimeMillis();
        if (!ready || busy || now - lastCheck < minGap) return;
        lastCheck = now;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    refresh();
                    scheduled();                                                // 182: גם כשהתוכנה פתוחה ימים
                } catch (Throwable t) {
                    Log.e("רענון בחזרה", t);
                }
            }
        }).start();
    }

    // ---------- הפעלה ----------

    /** פעם אחת לכל ריצה: טעינה, ואם אין אינדקס — בנייה; אחרת רענון לפי הרמה. */
    private final Object startLock = new Object();

    public void start(final Runnable onUpdate) {
        synchronized (startLock) {        // לא על this: בנייה שרצה מחזיקה את this, והמסך היה נתקע
            if (started) {
                if (onUpdate != null) onUpdate.run();
                return;
            }
            started = true;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    load();
                    if (onUpdate != null) onUpdate.run();
                    seedAdded();                                                // 189: אינדקס מגרסה 1 — "נוסף" מהמאגר
                    if (totalFiles() == 0 && totalDirs() == 0) {
                        build(Words.Lib.WHY_FIRST, new Index.Progress() {
                            @Override
                            public void onScan(String label, int items) {
                                status = Words.Lib.SCANNING + label + "… " + Files.count(items);
                                if (onUpdate != null) onUpdate.run();
                            }
                        });
                    } else if (updateLevel() >= 2) {                          // 110: גם ברמה 3 — בהפעלה
                        status = Words.Lib.REFRESHING;
                        if (onUpdate != null) onUpdate.run();
                        refresh();
                        status = "";
                        if (onUpdate != null) onUpdate.run();
                        scheduled();                                            // 182: פעם ביום / פעם בשבוע — ברקע
                    }
                } catch (Throwable t) {
                    Log.e("הפעלת הספרייה", t);
                } finally {
                    status = "";
                    if (onUpdate != null) onUpdate.run();
                }
            }
        }).start();
    }

    /**
     * 163: מעבר לאופן עבודה אחר בלי לסגור את התוכנה — שוכחים את האינדקס של האופן הקודם; ההפעלה הבאה (start)
     * טוענת את של החדש (ובונה, אם אין לו). רק כשאין בנייה באמצע (busy) — אחרת היא הייתה נשמרת לאופן החדש.
     */
    public void forgetMode() {
        synchronized (startLock) {
            started = false;
        }
        ready = false;
        status = "";
        synchronized (swapLock) {
            publish(new ArrayList<Index>());
        }
    }

    public synchronized void load() {
        List<Index> list = new ArrayList<Index>();
        for (Vols.Vol v : Vols.list(app)) {
            Index saved = Index.load(v.key);
            if (saved != null) {
                saved.label = v.label;
                saved.rootPath = v.root.getAbsolutePath();
                list.add(saved);
            } else {
                list.add(new Index(v.key, v.label, v.root.getAbsolutePath()));
            }
        }
        synchronized (swapLock) {
            publish(list);
        }
        ready = true;
    }

    /** מחליף את תמונת המצב. */
    private void publish(List<Index> list) {
        indexes = list;
    }

    /**
     * החלפת כונן אחד בתמונה חדשה שנבנתה בצד (בנייה או רענון). שינויים שעברו בתור בזמן שנבנתה
     * מוחלים גם עליה, כדי שלא יאבדו (80, תור אחד).
     */
    void swapBuilt(Index fresh) {
        synchronized (swapLock) {
            for (Job j : replay) apply(fresh, j, null);
            if (trialDiff != null) {                                        // 182: הבנייה השבועית — מה נמצא
                Index old = current(fresh.key);
                if (old != null && !(old.files.isEmpty() && old.dirs.isEmpty())) {
                    int[] d = Index.diff(old, fresh);
                    for (int i = 0; i < 3; i++) trialDiff[i] += d[i];
                }
            }
            swap(fresh);
        }
        saveLater(fresh.key);
    }

    private void swap(Index fresh) {
        carrySigs(current(fresh.key), fresh);                                // 193
        List<Index> list = new ArrayList<Index>(indexes);
        for (int i = 0; i < list.size(); i++)
            if (list.get(i).key.equals(fresh.key)) {
                list.set(i, fresh);
                publish(list);
                return;
            }
        list.add(fresh);
        publish(list);
    }

    /** סדר הכוננים בבדיקות שעוברות כונן אחרי כונן (היומית, השבועית). הבנייה עצמה — כולם במקביל (188). */
    private List<Index> buildOrder() {
        return new ArrayList<Index>(indexes);
    }

    /**
     * 177: כמה קוראים לכל כונן (1–16; הכוננים תמיד במקביל — 188: "כונן אחרי כונן" יצא, ו־"0" שנשמר נקרא כברירת המחדל).
     * ברירת המחדל — 8 (1.7.73: מעבדת הזירוזים — 8 מהיר מ־4 ומ־16 בטלפון; מספר אחד לכל העבודה עם הכונן).
     */
    static final int READERS_DEFAULT = 8;

    static int readers() {
        try {
            int n = Integer.parseInt(Conf.get(Conf.GENERAL, Conf.READERS, "" + READERS_DEFAULT).trim());
            return n < 1 ? READERS_DEFAULT : Math.min(16, n);
        } catch (NumberFormatException e) {
            return READERS_DEFAULT;
        }
    }

    /** 177: הבנייה האחרונה — כמה זמן, לכל כונן ובסך הכול (מוצג בהגדרות, ונרשם ביומן). */
    public String lastBuild() {
        return prefs().getString("lastBuild", "");
    }

    /** בנייה מלאה בבקשת המשתמש ("בניית האינדקס מחדש"). */
    public void build(final Index.Progress progress) {
        build(Words.Lib.WHY_REQUEST, progress);
    }

    /** 187: למה נבנה — נרשם ביומן (בבקשה · שבועית · יומית באנדרואיד ישן · התחלה). */
    private volatile String why = "";

    /** בנייה מלאה: כל כונן נבנה בצד ומוחלף בסיומו; הכוננים במקביל (177), וכל אחד — readers() קוראים. */
    public synchronized void build(String reason, final Index.Progress progress) {
        why = reason == null ? "" : reason;
        buildParallel(progress, readers());
    }

    /** 182: מפתח בזיכרון אנדרואיד לכל אופן עבודה — לכל אופן אינדקס משלו, ולכן גם יומית ושבועית משלו. */
    private String modeKey(String name) {
        return name + "@" + Paths.work().getAbsolutePath();
    }

    /** 1.7.73: הבנייה המלאה האחרונה (ולא רענון) — של אופן העבודה הפעיל (182; לפני כן — אחת לכולם). */
    public long lastFullBuild() {
        return prefs().getLong(modeKey("lastFullBuild"), 0);
    }

    /** 182: הבדיקה היומית האחרונה (רק מה שהמאגר לא מכיר). */
    public long lastDaily() {
        return prefs().getLong(modeKey("lastDaily"), 0);
    }

    // ---------- 182: סדר הפעולות — מה מגיע עכשיו ----------

    static final int NONE = 0, DAILY = 1, WEEKLY = 2, FULL_DAILY = 3;
    static final long DAY = 24L * 3600_000L, WEEK = 7 * DAY;

    /**
     * 182: מה מגיע עכשיו. עם "מה השתנה מאז" (אנדרואיד 11 ומעלה): פעם בשבוע — בנייה מההתחלה; פעם ביום —
     * בדיקה מלאה רק של מה שהמאגר לא מכיר. בלי (אנדרואיד ישן): פעם ביום — בנייה מלאה, כמו ב־1.7.73.
     */
    static int due(long now, long lastFull, long lastDaily, boolean points) {
        if (!points) return now - lastFull > DAY ? FULL_DAILY : NONE;
        if (now - lastFull > WEEK) return WEEKLY;
        if (now - Math.max(lastFull, lastDaily) > DAY) return DAILY;
        return NONE;
    }

    /** 182: המאגר של אנדרואיד — באנדרואיד; בבדיקות — תחליף (useMedia). */
    private volatile Media.Source media;

    Media.Source media() {
        if (media == null && app != null) media = Media.android(app);
        return media;
    }

    /** לבדיקות. */
    void useMedia(Media.Source m) {
        media = m;
        supported = null;
    }

    /** 182: יש "מה השתנה מאז" — המאגר עונה על הזיכרון הפנימי. */
    boolean pointsSupported() {
        Boolean b = supported;
        if (b == null) {
            Media.Source m = media();
            supported = b = m != null && m.generation("external_primary") >= 0;      // לא משתנה בזמן ריצה
        }
        return b;
    }

    private volatile Boolean supported;

    /** 182: מה שמגיע (יומית / שבועית) — ברקע, אחרי הבדיקה השוטפת. */
    void scheduled() {
        if (app == null) return;
        boolean points = pointsSupported();
        int what = due(System.currentTimeMillis(), lastFullBuild(), lastDaily(), points);
        if (what == WEEKLY) weekly();
        else if (what == FULL_DAILY) build(Words.Lib.WHY_DAILY_FULL, null);
        else if (what == DAILY) daily();
    }

    /** 182, שלב 6: בנייה מההתחלה — ותקופת הניסיון: מה נמצא לעומת האינדקס שהיה (אחרי הבדיקה השוטפת). */
    void weekly() {
        int[] d = new int[3];
        long t0 = System.currentTimeMillis();
        synchronized (swapLock) {
            trialDiff = d;
        }
        try {
            build(Words.Lib.WHY_WEEKLY, null);
        } finally {
            synchronized (swapLock) {
                trialDiff = null;
            }
        }
        Trial.add(Words.Trial.WEEKLY, System.currentTimeMillis() - t0,
                "נוספו " + d[0] + " · נמחקו " + d[1] + " · השתנו " + d[2], d[0] + d[1] + d[2]);   // תחביר
    }

    /** בזמן הבנייה השבועית — ההבדלים מצטברים כאן (בתוך swapBuilt, אחרי השינויים שבתור). */
    private int[] trialDiff;

    /**
     * 182, שלב 5: פעם ביום — גודל ותאריך של כל קובץ שהמאגר לא מכיר (מוסתרים, .nomedia וכל מה שבתוכן), גם
     * בתיקיות שתאריכן לא השתנה. כ־0.3 שניות. נרשם בתקופת הניסיון.
     */
    public synchronized int daily() {
        long t0 = System.currentTimeMillis();
        int changed = 0, checked = 0;
        beginBusy();
        try {
            for (Index old : buildOrder()) {
                boolean[] only = old.unknownToMedia();
                for (boolean b : only) if (b) checked++;
                Index fresh = old.copy();
                int c = fresh.deepRefresh(readers(), only);
                changed += c;
                if (c > 0) swapBuilt(fresh);
            }
        } finally {
            endBusy();
        }
        if (app != null) prefs().edit().putLong(modeKey("lastDaily"), System.currentTimeMillis()).apply();
        Log.i("בדיקה יומית (" + Words.Trial.DAILY_ABOUT + "): נבדקו " + checked + " · " + Words.Lib.changes(changed) + " · " + secs(System.currentTimeMillis() - t0));   // 187
        Trial.add(Words.Trial.DAILY, System.currentTimeMillis() - t0, "נבדקו " + checked, changed);       // תחביר
        if (changed > 0) announce(Change.rebuilt());
        return changed;
    }

    // ---------- 189: "נוסף" — מתי הקובץ הגיע ----------

    /**
     * אחרי בנייה מההתחלה (שבה "נוסף" לא ידוע): מהאינדקס הקודם, לפי המסלול; קובץ שלא היה בו (כשהיה אינדקס) —
     * חדש. מה שעדיין לא ידוע, וגם החדש — מהמאגר של אנדרואיד (מתי הגיע למכשיר), ואם אינו מכיר — חדש: עכשיו,
     * ישן (בבנייה הראשונה, מוסתרים, .nomedia): 0, "לא ידוע". מחזיר כמה קיבלו תאריך.
     */
    int carryAdded(Index old, Index fresh) {
        boolean hadOld = old != null && old != fresh && !(old.files.isEmpty() && old.dirs.isEmpty());
        int unknown = 0;
        for (Index.Rec r : fresh.files) {
            if (r.gone) continue;
            if (hadOld) {
                Index.Rec o = old.find(fresh.path(r));
                r.added = o != null ? o.added : -1;                         // -1: לא היה באינדקס הקודם
                if (o != null && o.size == r.size && o.mtime == r.mtime) r.sig = o.sig;   // 190: אותו קובץ — אותה חתימה
            }
            if (r.added <= 0) unknown++;
        }
        java.util.Map<String, Long> dates = unknown > 0 ? Media.dates(media(), fresh) : null;
        long now = System.currentTimeMillis();
        int n = 0;
        for (Index.Rec r : fresh.files) {
            if (r.gone || r.added > 0) continue;
            Long d = dates == null ? null : dates.get(fresh.path(r));
            if (d != null) r.added = d;
            else if (r.added < 0) r.added = now;
            else continue;
            n++;
        }
        return n;
    }

    /** 190: חתימות שחושבו ברקע — לשמור את האינדקס (בתור השמירה הרגיל). */
    void sigsChanged(String key) {
        saveLater(key);
    }

    /** אינדקס שנשמר בגרסה 1 (לפני 189): "נוסף" מהמאגר, פעם אחת, בלי לבנות מחדש. */
    void seedAdded() {
        List<Index> now = indexes;
        for (Index ix : now) {
            if (!ix.noAdded) continue;
            ix.noAdded = false;
            Index fresh = ix.copy();
            int n = carryAdded(ix, fresh);
            Log.i("189: \"נוסף\" מהמאגר — " + fresh.label + ": " + Words.files(n));
            swapBuilt(fresh);                                               // נשמר בגרסה 2 — גם כשלא נמצא דבר
        }
    }

    /** 1.7.72: בפשטות — "2.1 שניות". */
    static String secs(long ms) {
        return String.format(java.util.Locale.US, Words.Lib.SECONDS_FORMAT, ms / 1000.0);
    }

    /** 1.7.71: על הכפתור — רק הזמן הכולל; הפירוט לכל כונן — בחלון הבחירה (שורה לכל כונן) וביומן. */
    private void measured(StringBuilder times, long all0, int readers) {
        String total = secs(System.currentTimeMillis() - all0);
        prefs().edit().putString("lastBuild", total).putString("lastBuildDrives", times.toString().replace(" · ", "\n"))
                .putLong(modeKey("lastFullBuild"), System.currentTimeMillis()).apply();
        Log.i("בניית אינדקס" + (why.isEmpty() ? "" : " (" + why + ")") + ": " + total + " (" + times + ") · "
                + readers + Words.Lib.READERS_PARALLEL);
    }

    /** 1.7.71: הבנייה האחרונה — שורה לכל כונן. */
    public String lastBuildDrives() {
        return prefs().getString("lastBuildDrives", "");
    }

    /** 177: כל הכוננים במקביל; כל כונן — readers קוראים. מה שמסתיים — מוצג מיד (הכונן המהיר לא מחכה לאיטי). */
    private void buildParallel(final Index.Progress progress, final int readers) {
        loadExcluded();
        beginBusy();
        final long all0 = System.currentTimeMillis();
        StringBuilder times = new StringBuilder();
        final java.util.concurrent.LinkedBlockingQueue<Object[]> done = new java.util.concurrent.LinkedBlockingQueue<Object[]>();
        try {
            List<Index> order = buildOrder();
            final java.util.Map<String, Media.Point> points = new java.util.HashMap<String, Media.Point>();
            for (Index old : order) {
                final Index fresh = new Index(old.key, old.label, old.rootPath);
                points.put(fresh.key, Media.now(media(), fresh));            // 182, שלב 0: הנקודה — לפני הסריקה
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        long t0 = System.currentTimeMillis();
                        try {
                            fresh.scan(new Index.Progress() {
                                @Override
                                public void onScan(String label, int items) {
                                    status = Words.Lib.SCANNING + label + "… " + Files.count(items);
                                    if (progress != null) progress.onScan(label, items);
                                }
                            }, readers);
                        } finally {
                            done.add(new Object[]{fresh, System.currentTimeMillis() - t0});
                        }
                    }
                }, "סריקה " + fresh.label).start();  // תחביר
            }
            status = Words.Lib.SCANNING_ALL;
            for (int i = 0; i < order.size(); i++) {
                Object[] r = done.take();
                Index fresh = (Index) r[0];
                long ms = (Long) r[1];
                carryAdded(current(fresh.key), fresh);                          // 189
                times.append(times.length() > 0 ? " · " : "").append(fresh.label).append(" ").append(secs(ms));
                swapBuilt(fresh);
                Media.Point point = points.get(fresh.key);
                if (point != null) point.save(fresh.key);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            endBusy();
            status = "";
        }
        measured(times, all0, readers);
        announce(Change.rebuilt());
    }

    /** 187: רענון בבקשת המשתמש — נרשם ביומן, עם הזמן ומספר השינויים (הבדיקה השוטפת — לא, כדי שהיומן לא יתמלא). */
    public int refreshAsked() {
        long t0 = System.currentTimeMillis();
        int n = refresh();
        Log.i("רענון מהיר (" + Words.Lib.WHY_REQUEST + "): " + Words.Lib.changes(n) + " · " + (System.currentTimeMillis() - t0) + " מ\"ש");
        return n;
    }

    /** רענון: על עותק בצד, והחלפה בסיום. */
    public synchronized int refresh() {
        loadExcluded();
        beginBusy();
        int changed = 0;
        lastCheck = System.currentTimeMillis();
        try {
            List<Index> order = buildOrder();
            for (Vols.Vol v : Vols.list(app)) {                            // 109: כונן שהוכנס אחרי ההפעלה
                boolean known = false;
                for (Index ix : order) if (ix.key.equals(v.key)) known = true;
                if (!known) {
                    Index saved = Index.load(v.key);
                    order.add(saved != null ? saved : new Index(v.key, v.label, v.root.getAbsolutePath()));
                }
            }
            for (Index old : order) {
                boolean isNew = true;
                for (Index ix : indexes) if (ix.key.equals(old.key)) isNew = false;
                int c;
                Index fresh;
                Media.Point[] next = new Media.Point[1];
                if (old.files.isEmpty() && old.dirs.isEmpty()) {
                    next[0] = Media.now(media(), old);                      // 182, שלב 0: כונן חדש — בנייה, והנקודה לפניה
                    fresh = old.copy();
                    long s0 = System.currentTimeMillis();
                    fresh.scan(null);
                    carryAdded(old, fresh);                                     // 189: כונן חדש — מהמאגר
                    Log.i("בניית אינדקס (" + Words.Lib.WHY_NEW_DRIVE + "): " + fresh.label + " " + secs(System.currentTimeMillis() - s0)
                            + " · " + Words.files(fresh.fileCount()));
                    c = 1;
                } else {
                    long[] times = old.times(readers());       // 1.7.73: התאריכים — בכמה קוראים
                    long m0 = System.currentTimeMillis();
                    List<String> reported = Media.since(media(), old, next);   // 182 (ב): מה השתנה מאז הנקודה
                    List<String> stale = old.stale(reported);
                    long ms = System.currentTimeMillis() - m0;
                    if (!old.differs(times) && stale.isEmpty() && !isNew) {   // 184: בלי שינוי — בלי להעתיק את האינדקס
                        next[0].save(old.key);
                        continue;
                    }
                    fresh = old.copy();
                    c = times == null ? 0 : fresh.apply(times);
                    if (!stale.isEmpty()) {
                        int b = fresh.update(fresh.stale(reported));        // רק מה שבדיקת התאריכים לא תפסה
                        if (b > 0) Trial.add(Words.Trial.SINCE, ms, fresh.label, b);
                        c += b;
                    }
                }
                changed += c;
                if (c > 0 || isNew) swapBuilt(fresh);                       // 113: בלי שינוי — בלי החלפה ושמירה
                else old.rootMtime = fresh.rootMtime;                      // 184: השורש נבדק ולא השתנה בו דבר — לזכור
                next[0].save(fresh.key);
            }
        } finally {
            endBusy();
        }
        if (changed > 0) announce(Change.rebuilt());
        return changed;
    }

    /** רענון מעמיק (108, 114): גודל ותאריך של כל קובץ, גם בתיקיות שלא השתנו. ידני, מההגדרות. */
    public synchronized int deepRefresh() {
        long t0 = System.currentTimeMillis();
        beginBusy();
        int changed = 0;
        try {
            for (Index old : buildOrder()) {
                Index fresh = old.copy();
                int r = readers();
                int c = fresh.deepRefresh(r) + fresh.refresh(r);
                changed += c;
                if (c > 0) swapBuilt(fresh);
            }
        } finally {
            endBusy();
        }
        Log.i("רענון מעמיק (" + Words.Lib.WHY_REQUEST + "): " + Words.Lib.changes(changed) + " · " + secs(System.currentTimeMillis() - t0));   // 187
        if (changed > 0) announce(Change.rebuilt());
        return changed;
    }

    // ---------- חיפוש מהיר לפי מסלול ----------

    /** 184: קובץ לפי מסלול — דרך הכונן שהמסלול בתוכו ו"מי בתוך מי" שלו (בלי מפה של כל המסלולים). */
    public Hit find(String path) {
        if (path == null) return null;
        Index ix = owner(path);
        if (ix == null) return null;
        Index.Rec r = ix.find(path);
        return r == null ? null : hit(ix, r);
    }

    /** האם קובץ קיים, לפי האינדקס. */
    public boolean exists(String path) {
        return find(path) != null;
    }

    /** קבצים לפי שם, ואם ידוע גם לפי גודל. */
    public List<Hit> byName(String name, long size) {
        List<Hit> out = new ArrayList<Hit>();
        for (Index ix : indexes)
            for (Index.Rec r : ix.files)
                if (!r.gone && !ix.trash(r) && !ix.hidden(r) && r.name.equals(name) && (size <= 0 || r.size == size)) out.add(hit(ix, r));
        return out;
    }

    /** 1.12.0: כל הקבצים ששמם מסתיים כך (למשל קובצי עיצוב שהגיעו בשיתוף) — מסלולים מלאים. */
    public List<String> byEnding(String end) {
        List<String> out = new ArrayList<String>();
        for (Index ix : indexes)
            for (Index.Rec r : ix.files)
                if (!r.gone && !ix.trash(r) && r.name.endsWith(end)) out.add(hit(ix, r).path());
        return out;
    }

    public List<Hit> bySize(long size) {
        List<Hit> out = new ArrayList<Hit>();
        if (size <= 0) return out;
        for (Index ix : indexes)
            for (Index.Rec r : ix.files) if (!r.gone && !ix.trash(r) && !ix.hidden(r) && r.size == size) out.add(hit(ix, r));
        return out;
    }

    public int totalFiles() {
        int n = 0;
        for (Index ix : indexes) n += ix.fileCount();
        return n;
    }

    public int totalDirs() {
        int n = 0;
        for (Index ix : indexes) n += ix.dirCount();
        return n;
    }

    public long lastBuilt() {
        long t = 0;
        for (Index ix : indexes) if (ix.built > t) t = ix.built;
        return t;
    }

    // ---------- תוצאות ----------

    public static final class Hit {
        public Index ix;
        public Index.Rec rec;
        public int dirId = -1;
        public int rank, section;    // FULL · NO_PREFIX · LOOSE · PARTIAL (78)
        /** 175: תיקייה ריקה — כמה תיקיות ריקות בתוכה (נמחקות איתה). */
        public int innerEmpty;

        public boolean isDir() {
            return rec == null;
        }

        public String name() {
            return rec != null ? rec.name : ix.dirs.get(dirId).name;
        }

        public String path() {
            return rec != null ? ix.path(rec) : ix.dirPath(dirId);
        }

        public String parentPath() {
            return rec != null ? ix.dirPath(rec.dir) : ix.dirPath(ix.dirs.get(dirId).parent);
        }
    }

    /** מוסתר ותיקיית המחיקות לא מופיעים בתוצאות, חוץ מהסל כשמבקשים אותו במפורש. */
    static boolean hide(Index ix, Index.Rec r, Cond cond) {
        if (ix.trash(r)) return cond == null || !cond.trash;
        if ((cond == null || !cond.backup) && ix.inBackup(r)) return true;     // 1.22.4: קבצי גיבוי — רק כשמבקשים
        return ix.hidden(r);
    }

    /**
     * 145: כל הסוגים שבאינדקס, עם מספר הקבצים — הנפוץ קודם (שווים — לפי השם). בלי הסל והמוסתרים,
     * ובלי קבצים בלי סיומת (הם ב"שאר הקבצים"). מהאינדקס, בלי סריקה.
     */
    public List<Map.Entry<String, Integer>> typeCounts() {
        Map<String, Integer> n = new java.util.HashMap<String, Integer>();
        for (Index ix : indexes)
            for (Index.Rec r : ix.files) {
                if (r.gone || hide(ix, r, null)) continue;
                String e = Text.ext(r.name);
                if (e.isEmpty()) continue;
                Integer c = n.get(e);
                n.put(e, c == null ? 1 : c + 1);
            }
        List<Map.Entry<String, Integer>> out = new ArrayList<Map.Entry<String, Integer>>(n.entrySet());
        java.util.Collections.sort(out, new java.util.Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> a, Map.Entry<String, Integer> b) {
                int c = b.getValue().compareTo(a.getValue());
                return c != 0 ? c : a.getKey().compareTo(b.getKey());
            }
        });
        return out;
    }

    /** תיקיות שהוצאו מהאינדקס, מההגדרות (76). */
    static void loadExcluded() {
        java.util.Set<String> ex = new java.util.HashSet<String>();
        for (String p : Conf.get(Conf.GENERAL, Conf.EXCLUDED, "").split("\\|"))
            if (!p.trim().isEmpty()) ex.add(p.trim());
        Index.EXCLUDED = ex;
    }

    // ---------- מנהל הקבצים מהאינדקס (63) ----------

    /** קובץ או תיקייה כפי שהם באינדקס — בלי לגשת לכונן בכל שורה. */
    public static final class IxFile extends File {
        final boolean dir;
        final long len, mod;
        final boolean hidden;

        IxFile(String path, boolean dir, long len, long mod, boolean hidden) {
            super(path);
            this.dir = dir;
            this.len = len;
            this.mod = mod;
            this.hidden = hidden;
        }

        @Override public boolean isDirectory() { return dir; }
        @Override public boolean isFile() { return !dir; }
        @Override public long length() { return len; }
        @Override public long lastModified() { return mod; }
        @Override public boolean exists() { return true; }
    }

    /**
     * תוכן תיקייה מהאינדקס, או null אם אינה באינדקס. אם התיקייה בכונן השתנתה מאז — היא מסונכרנת
     * ברקע, ו-onSynced נקרא כשהתמונה המעודכנת מוכנה.
     */
    public List<File> listDir(File dir, boolean withHidden, final Runnable onSynced) {
        if (!ready) return null;
        final Index ix = owner(dir.getAbsolutePath() + "/");
        if (ix == null) return null;
        final int id = ix.dirId(dir.getAbsolutePath());
        if (id == -2) return null;
        List<File> out = new ArrayList<File>();
        for (int k : ix.childDirs(id)) {
            Index.Dir d = ix.dirs.get(k);
            if (d.hidden && !withHidden) continue;
            out.add(new IxFile(ix.dirPath(k), true, 0, d.mtime, d.hidden));
        }
        for (Index.Rec r : ix.files(id)) {
            boolean hid = ix.hidden(r);
            if (hid && !withHidden) continue;
            out.add(new IxFile(ix.path(r), false, r.size, r.mtime, hid));
        }
        final File d = dir;
        if (id >= 0 && d.lastModified() != ix.dirMtime(id) && !busy)       // השתנתה בכונן: סנכרון תיקייה אחת
            enqueue(new Job(SYNC, d.getAbsolutePath(), onSynced));
        return out;
    }

    public static Hit hit(Index ix, Index.Rec r) {
        Hit h = new Hit();
        h.ix = ix;
        h.rec = r;
        return h;
    }

    public static final class Result {
        public final List<Hit> hits = new ArrayList<Hit>();
        public int total;
        public double ms;
        public String note = "", query = "";
        public boolean loading;
        /** מספר ההתאמות לכל סוג, לפי MATCH_TYPES. */
        public final int[] counts = new int[MATCH_TYPES.length];
        /** אותיות השימוש שהוסרו, למשל "מ'" (78). */
        public String prefix = "";
    }

    private static final class Term {
        Text.Pattern[] alts;
        /** לכל חלופה: האם היא הגרסה בלי אות השימוש (78). */
        boolean[] stripped;
        String plain;
    }

    /** חיפוש. paths ו-loose: הגדרות המופע, או null לברירת מחדל. */
    /**
     * 175 — תיקיות ריקות: אין בהן אף קובץ, גם לא בתיקיות שבתוכן; רק ".nomedia" (סימון לגלריה) לא נחשב —
     * קובץ מוסתר אחר (הגדרות, מטמון של אפליקציה) — התיקייה אינה ריקה (1.7.65).
     * מוצגת רק החיצונית (כמו בתוכנות "מסיר תיקיות ריקות"), ו־innerEmpty — כמה ריקות בתוכה. בלי שורש הכונן,
     * הסל והתיקיות המוסתרות. מילים — בשם התיקייה; מקורות, כונן ותאריך — לפי Cond.matchesDir.
     */
    static final String NOMEDIA = ".nomedia", ANDROID = "Android";

    private void emptyDirs(Result res, Cond cond, List<Term> terms, boolean paths) {
        for (Index ix : indexes) {
            int n = ix.dirs.size();
            int[] files = new int[n], inner = new int[n];
            for (Index.Rec r : ix.files)
                if (!r.gone && !r.name.equalsIgnoreCase(NOMEDIA) && r.dir >= 0 && r.dir < n) files[r.dir]++;
            for (int i = 0; i < n; i++) {                                   // 1.7.69: "Android" בשורש — לעולם לא ריקה:
                Index.Dir d = ix.dirs.get(i);                                   // יש בה data ו־obb, שאינם באינדקס
                if (d.parent < 0 && d.name.equalsIgnoreCase(ANDROID)) files[i]++;
            }
            for (int i = n - 1; i >= 0; i--) {                              // ההורה תמיד לפני הילד: מהסוף להתחלה
                Index.Dir d = ix.dirs.get(i);
                if (d.gone) continue;                                       // נמחקה מהכונן — לא נספרת
                if (d.parent >= 0 && d.parent < i) {
                    files[d.parent] += files[i];
                    if (files[i] == 0) inner[d.parent] += 1 + inner[i];
                }
            }
            for (int i = 0; i < n; i++) {
                Index.Dir d = ix.dirs.get(i);
                if (d.gone || d.trash || d.hidden || files[i] > 0) continue;
                if (d.parent >= 0 && files[d.parent] == 0 && !ix.dirs.get(d.parent).hidden) continue;   // לא החיצונית
                if (!terms.isEmpty() && match(terms, d.norm, ix, d.parent, paths) == 0) continue;
                if (!cond.matchesDir(ix, i)) continue;
                Hit h = new Hit();
                h.ix = ix;
                h.dirId = i;
                h.innerEmpty = inner[i];
                res.hits.add(h);
            }
        }
        res.total = res.hits.size();
        res.counts[FULL] = res.total;
    }

    public Result search(String query, boolean paths, boolean loose) {
        if (query != null && query.indexOf(SmartLists.HERE_VAR) >= 0)          // 1.11.0: ברירת מחדל — תיקיית אופן העבודה
            query = SmartLists.withHere(query, Paths.work().getAbsolutePath());
        Result res = new Result();
        res.loading = !ready || busy;
        long t0 = System.nanoTime();
        String q = Text.norm(query).trim();
        res.query = q;
        if (q.isEmpty()) return res;

        Cond cond = Cond.parse(q);
        // 102: תנאי שלא הובן (למשל "גודל:2MB" בלי >) — לא מתבצע, ואומרים זאת במקום לבלוע בשקט
        if (!cond.unknown.isEmpty()) {
            StringBuilder u = new StringBuilder(Words.Lib.NOT_UNDERSTOOD);
            for (int i = 0; i < cond.unknown.size(); i++) u.append(i > 0 ? ", " : "").append(cond.unknown.get(i));
            res.note = u.toString();
        }
        List<Term> terms = new ArrayList<Term>();
        for (String w : cond.words) terms.add(term(w));
        if (cond.emptyDirs) {                                               // 175: תיקיות ריקות, לא קבצים
            emptyDirs(res, cond, terms, paths);
            res.ms = (System.nanoTime() - t0) / 1_000_000.0;
            return res;
        }
        if (terms.isEmpty() && cond.tests.isEmpty()) return res;

        String rankWord = terms.isEmpty() ? "" : terms.get(0).plain;
        boolean onlyFilters = terms.isEmpty();

        @SuppressWarnings("unchecked")
        List<Hit>[] buckets = new List[4];
        for (int i = 0; i < 4; i++) buckets[i] = new ArrayList<Hit>();
        List<Hit> noPrefix = new ArrayList<Hit>();       // נמצא רק בלי אות השימוש — קבוצה נפרדת (78)
        java.util.Set<Object> seen = new java.util.HashSet<Object>();
        StringBuilder letters = new StringBuilder();
        for (Term t : terms)
            for (int i = 0; i < t.alts.length; i++)
                if (t.stripped[i]) {
                    String l = t.plain.substring(0, 1) + "'";
                    if (letters.indexOf(l) < 0) letters.append(letters.length() > 0 ? ", " : "").append(l);
                }
        res.prefix = letters.toString();

        final List<Index> snap = indexes;
        final boolean multi = terms.size() > 1;
        for (Index ix : snap) {
            for (Index.Rec r : ix.files) {
                if (r.gone || hide(ix, r, cond) || !cond.matches(ix, r)) continue;
                int m = onlyFilters ? 1 : match(terms, r.norm, ix, r.dir, paths);
                if (m == 0) continue;
                res.total++;
                if (multi || res.total <= 10) seen.add(r);           // 184: נחוץ רק לכתיב חסר (פחות מ־10) ולכמה מילים
                if (m == 2) {
                    res.counts[NO_PREFIX]++;
                    if (noPrefix.size() < CAP_PER_RANK) {
                        Hit h = hit(ix, r);
                        h.rank = 3;
                        h.section = NO_PREFIX;
                        noPrefix.add(h);
                    }
                    continue;
                }
                res.counts[FULL]++;
                int rank = Text.rank(r.norm, rankWord);
                if (buckets[rank].size() < CAP_PER_RANK) {
                    Hit h = hit(ix, r);
                    h.rank = rank;
                    buckets[rank].add(h);
                }
            }
            if (cond.tests.isEmpty()) {
                for (int i = 0; i < ix.dirs.size(); i++) {
                    Index.Dir d = ix.dirs.get(i);
                    if (d.gone || onlyFilters || d.trash || d.hidden) continue;
                    if (cond.hasNot() && !cond.matchesDir(ix, i)) continue;            // 181: "לא" — גם על תיקיות
                    int m = match(terms, d.norm, ix, d.parent, paths);
                    if (m == 0) continue;
                    res.total++;
                    Hit h = new Hit();
                    h.ix = ix;
                    h.dirId = i;
                    if (m == 2) {
                        res.counts[NO_PREFIX]++;
                        h.rank = 3;
                        h.section = NO_PREFIX;
                        if (noPrefix.size() < CAP_PER_RANK) noPrefix.add(h);
                        continue;
                    }
                    res.counts[FULL]++;
                    h.rank = Text.rank(d.norm, rankWord);
                    if (buckets[h.rank].size() < CAP_PER_RANK) buckets[h.rank].add(h);
                }
            }
        }
        for (int i = 0; i < 4; i++) res.hits.addAll(buckets[i]);
        res.hits.addAll(noPrefix);
        int exact = res.counts[FULL];

        if (loose && res.total < 10 && !terms.isEmpty()) addLoose(res, cond, terms, seen, snap);
        if (terms.size() > 1) addPartial(res, cond, terms, seen, snap);

        res.ms = (System.nanoTime() - t0) / 1_000_000.0;
        return res;
    }

    /** 78: מעט תוצאות — גם בכתיב חסר (בלי יו"ד ווי"ו באמצע מילה), כקבוצה נפרדת. */
    private void addLoose(Result res, Cond cond, List<Term> terms, java.util.Set<Object> seen, List<Index> snap) {
        List<Term> l = new ArrayList<Term>();
        boolean different = false;
        for (Term t : terms) {
            String x = Text.loose(t.plain);
            if (!x.equals(t.plain)) different = true;
            l.add(term(x));
        }
        if (different) {
            int added = 0;
            for (Index ix : snap)
                for (Index.Rec r : ix.files) {
                    if (r.gone || seen.contains(r) || hide(ix, r, cond) || !cond.matches(ix, r)) continue;
                    if (!matchAll(l, r.loose, ix, r.dir, false)) continue;
                    res.total++;
                    res.counts[LOOSE]++;
                    seen.add(r);
                    if (added < CAP_PER_RANK) {
                        Hit h = hit(ix, r);
                        h.rank = 3;
                        h.section = LOOSE;
                        res.hits.add(h);
                        added++;
                    }
                }
        }
    }

    /** כמה מילים: גם מה שמתאים רק לחלק מהן (עד 150), כקבוצה נפרדת. */
    private void addPartial(Result res, Cond cond, List<Term> terms, java.util.Set<Object> seen, List<Index> snap) {
        int added = 0;
        for (Index ix : snap)
            for (Index.Rec r : ix.files) {
                if (added >= 150) break;
                if (r.gone || seen.contains(r) || hide(ix, r, cond) || !cond.matches(ix, r)) continue;
                if (!matchAny(terms, r.norm)) continue;
                res.counts[PARTIAL]++;
                Hit h = hit(ix, r);
                h.rank = 3;
                h.section = PARTIAL;
                res.hits.add(h);
                added++;
            }
    }

    private Term term(String word) {
        Term t = new Term();
        t.plain = word;
        List<Text.Pattern> alts = new ArrayList<Text.Pattern>();
        List<Boolean> strip = new ArrayList<Boolean>();
        for (String part : word.split("\\|")) {
            if (part.isEmpty()) continue;
            alts.add(Text.compile(Text.widen(part)));
            strip.add(false);
            String stripped = Text.withoutPrefixLetter(part);
            if (stripped != null && part.indexOf('*') < 0 && part.indexOf('?') < 0) {
                alts.add(Text.wordStart(stripped));
                strip.add(true);
            }
        }
        if (alts.isEmpty()) {
            alts.add(Text.compile(word));
            strip.add(false);
        }
        t.alts = alts.toArray(new Text.Pattern[0]);
        t.stripped = new boolean[strip.size()];
        for (int i = 0; i < t.stripped.length; i++) t.stripped[i] = strip.get(i);
        return t;
    }

    /**
     * 0 — לא מתאים; 1 — התאמה מלאה; 2 — מתאים, אבל לפחות מילה אחת נמצאה רק בלי אות השימוש (78).
     */
    private int match(List<Term> terms, String norm, Index ix, int dirId, boolean paths) {
        boolean onlyStripped = false;
        String path = null;
        for (Term t : terms) {
            int m = termMatch(t, norm);
            if (m == 0 && paths && dirId >= 0) {
                if (path == null) path = ix.normPath(dirId);            // 184: פעם אחת לכל תיקייה, לא לכל קובץ
                m = termMatch(t, path);
            }
            if (m == 0) return 0;
            if (m == 2) onlyStripped = true;
        }
        return onlyStripped ? 2 : 1;
    }

    /** 0 — לא; 1 — בחלופה רגילה; 2 — רק בחלופה בלי אות השימוש. */
    private static int termMatch(Term t, String s) {
        int best = 0;
        for (int i = 0; i < t.alts.length; i++)
            if (t.alts[i].matches(s)) {
                if (!t.stripped[i]) return 1;
                best = 2;
            }
        return best;
    }

    private boolean matchAll(List<Term> terms, String norm, Index ix, int dirId, boolean paths) {
        return match(terms, norm, ix, dirId, paths) != 0;
    }

    private boolean matchAny(List<Term> terms, String norm) {
        for (Term t : terms)
            for (Text.Pattern p : t.alts)
                if (p.matches(norm)) return true;
        return false;
    }

    // ---------- פעולות על קבצים ----------


    // ---------- עדכון נקודתי: כל פעולה שמשנה קובץ מעדכנת את האינדקס ----------

    /**
     * 193: חתימות שנכתבו ברקע בתמונה המוצגת אחרי שהעותק כבר נוצר — עוברות לעותק לפני ההחלפה. עותק שומר על
     * סדר הרשומות (חדשות — רק בסוף), ולכן משווים לפי המקום, עם בדיקה שזו אותה רשומה.
     */
    static void carrySigs(Index old, Index fresh) {
        if (old == null || old == fresh) return;
        int n = Math.min(old.files.size(), fresh.files.size());
        for (int i = 0; i < n; i++) {
            Index.Rec a = old.files.get(i), b = fresh.files.get(i);
            if (b.sig == 0 && a.sig != 0 && a.dir == b.dir && a.size == b.size && a.mtime == b.mtime && a.name.equals(b.name)) b.sig = a.sig;
        }
    }

    /** 193: התמונה המוצגת של כונן (לכתיבת חתימה ברשומה העדכנית). */
    Index published(String key) {
        return current(key);
    }

    private Index current(String key) {
        for (Index ix : indexes) if (ix.key.equals(key)) return ix;
        return null;
    }

    private Index owner(String path) {
        Index best = null;
        for (Index ix : indexes) {
            String root = ix.rootPath.endsWith("/") ? ix.rootPath : ix.rootPath + "/";
            if (path.startsWith(root) && (best == null || ix.rootPath.length() > best.rootPath.length())) best = ix;
        }
        return best;
    }

    /** שורש הכונן (לפי האינדקס) שהמסלול בתוכו, או null. */
    /** 1.22.0: המפתח הקבוע של הכונן שהמסלול בתוכו ("פנימי", או שם הכרטיס כמו ABCD-1234); null — מחוץ לכוננים. */
    public String keyOf(String path) {
        Index ix = owner(path + "/");
        return ix == null ? null : ix.key;
    }

    public String rootOf(String path) {
        Index ix = owner(path + "/");
        return ix == null ? null : ix.rootPath;
    }

    /** תיקייה שהשתנתה בפעולה של התוכנה (נמחקה, הועברה, נוצרה): סנכרון מול הכונן דרך התור (82). */
    public void dirChanged(String path) {
        enqueue(new Job(SYNC, path, null));
    }

    /** מסלולי כל הקבצים שבתוך תיקייה (כולל תיקיות משנה), מהאינדקס. */
    public List<String> filesUnder(String dir) {
        List<String> out = new ArrayList<String>();
        Index ix = owner(dir + "/");
        if (ix == null) return out;
        boolean[] in = subtree(ix, ix.dirId(dir));
        if (in == null) return out;
        for (Index.Rec r : ix.files) if (!r.gone && r.dir >= 0 && in[r.dir]) out.add(ix.path(r));
        return out;
    }

    /** כמה קבצים וכמה בתים בתוך תיקייה, מהאינדקס: {קבצים, בתים}. */
    public long[] treeStats(String dir) {
        long[] s = {0, 0};
        Index ix = owner(dir + "/");
        if (ix == null) return s;
        boolean[] in = subtree(ix, ix.dirId(dir));
        if (in == null) return s;
        for (Index.Rec r : ix.files)
            if (!r.gone && r.dir >= 0 && in[r.dir]) {
                s[0]++;
                s[1] += r.size;
            }
        return s;
    }

    /** 1.23.4: הקבצים שבתיקייה ובכל תתי־התיקיות — מסלול → גודל. */
    public java.util.Map<String, Long> treeFiles(String dir) {
        java.util.Map<String, Long> m = new java.util.HashMap<String, Long>();
        Index ix = owner(dir + "/");
        if (ix == null) return m;
        boolean[] in = subtree(ix, ix.dirId(dir));
        if (in == null) return m;
        for (Index.Rec r : ix.files)
            if (!r.gone && r.dir >= 0 && in[r.dir]) m.put(ix.path(r), r.size);
        return m;
    }

    /** התיקיות שבתת־העץ (תיקייה-אם תמיד לפני בתה באינדקס). null אם אינה באינדקס. */
    static boolean[] subtree(Index ix, int id) {
        if (id == -2) return null;
        boolean[] in = new boolean[ix.dirs.size()];
        for (int k = 0; k < in.length; k++) {
            Index.Dir d = ix.dirs.get(k);
            if (d.gone) continue;
            in[k] = k == id || id == -1 && d.parent < 0 || d.parent >= 0 && d.parent < k && in[d.parent];
        }
        return in;
    }

    /** קובץ שנוצר או הגיע למקום חדש: נוסף לאינדקס דרך התור. */
    public void fileAdded(File f) {
        enqueue(new Job(ADD, f.getAbsolutePath(), null));
    }

    /** קבצים שהתוכנה לומדת עליהם (שיתוף, הוספה מהכונן): מה שאינו באינדקס נוסף מיד. */
    public void learn(final List<String> paths) {
        if (paths == null || paths.isEmpty()) return;
        final List<String> copy = new ArrayList<String>(paths);
        start(null);
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 100 && !ready; i++) try { Thread.sleep(100); } catch (InterruptedException ignored) { }
                if (!ready) return;
                for (String p : copy) {
                    if (find(p) == null && new File(p).isFile()) enqueue(new Job(ADD, p, null));
                }
            }
        }).start();
    }

    /** קובץ שנעלם מהכונן בפעולה של התוכנה (מחיקה, ריקון סל, שחזור, העברה): יורד מהאינדקס דרך התור. */
    public void fileRemoved(String path) {
        enqueue(new Job(REMOVE, path, null));
    }

    // ---------- תור אחד לשינויי האינדקס (80) ----------
    //
    // כל שינוי נקודתי עובר כאן, בסדר שבו נשלח, בחוט אחד. שינויים שהצטברו מוחלים יחד: עותק אחד לכל
    // כונן, החלפה ברגע אחד, הודעה אחת "מה קרה", ושמירה לקובץ אחר כך (62). התמונה המוצגת אינה משתנה
    // לעולם במקומה — כך אין מרוץ בין חוטים.

    private static final int ADD = 0, REMOVE = 1, SYNC = 2, MARK = 3, CARRY = 4;

    private static final class Job {
        final int kind;
        final String path;
        final Runnable done;
        /** 1.19.0 (CARRY): "נוסף", החתימה, ולפי מה החתימה תקפה (גודל ותאריך). */
        long added, sig, size, mtime;

        Job(int kind, String path, Runnable done) {
            this.kind = kind;
            this.path = path;
            this.done = done;
        }
    }

    /**
     * 1.19.0: קובץ שהתוכנה העבירה — "נוסף" שלו (והחתימה, אם אותו גודל ותאריך) נלקחים מהמקור המדויק, אחרי
     * שהוא נכנס לאינדקס במקומו החדש. גובר על הניחוש לפי גודל ותאריך (שנשאר לשינויים מחוץ לתוכנה).
     */
    public void carry(String path, long added, long sig, long size, long mtime) {
        Job j = new Job(CARRY, path, null);
        j.added = added;
        j.sig = sig;
        j.size = size;
        j.mtime = mtime;
        enqueue(j);
    }

    /** "מה קרה" — נמסר לכל מסך שמקשיב, אחרי שהתמונה החדשה כבר מוצגת. */
    public static final class Change {
        public final List<String> added = new ArrayList<String>();
        public final List<String> removed = new ArrayList<String>();
        public final List<String> synced = new ArrayList<String>();
        /** בנייה או רענון של כונן שלם. */
        public boolean all;

        static Change rebuilt() {
            Change c = new Change();
            c.all = true;
            return c;
        }

        public boolean isEmpty() {
            return !all && added.isEmpty() && removed.isEmpty() && synced.isEmpty();
        }
    }

    public interface Listener {
        /** נקרא בחוט של התור, לא בחוט המסך. */
        void onChange(Change c);
    }

    private final java.util.concurrent.LinkedBlockingQueue<Job> queue =
            new java.util.concurrent.LinkedBlockingQueue<Job>();
    private final java.util.concurrent.atomic.AtomicInteger pending = new java.util.concurrent.atomic.AtomicInteger();
    private final java.util.concurrent.CopyOnWriteArrayList<Listener> listeners =
            new java.util.concurrent.CopyOnWriteArrayList<Listener>();
    private final Object swapLock = new Object();
    /** שינויים שעברו בתור בזמן בנייה או רענון — מוחלים גם על התמונה החדשה. */
    private final List<Job> replay = new ArrayList<Job>();
    private Thread worker, saver;
    private final java.util.Set<String> dirty = new java.util.LinkedHashSet<String>();

    public void listen(Listener l) {
        if (!listeners.contains(l)) listeners.add(l);
    }

    public void unlisten(Listener l) {
        listeners.remove(l);
    }

    /** run ירוץ אחרי שכל מה שנשלח לתור עד עכשיו כבר מוצג (בחוט של התור). */
    public void whenApplied(Runnable run) {
        enqueue(new Job(MARK, null, run));
    }

    void beginBusy() {
        synchronized (swapLock) {
            replay.clear();
            busy = true;
        }
    }

    void endBusy() {
        synchronized (swapLock) {
            busy = false;
            replay.clear();
        }
    }

    private void enqueue(Job j) {
        pending.incrementAndGet();
        synchronized (queue) {
            if (worker == null) {
                worker = new Thread(new Runnable() {
                    @Override public void run() { work(); }
                }, "תור האינדקס");  // תחביר
                worker.setDaemon(true);
                worker.start();
            }
        }
        queue.add(j);
    }

    private void work() {
        while (true) {
            List<Job> batch = new ArrayList<Job>();
            try {
                batch.add(queue.take());
            } catch (InterruptedException e) {
                return;
            }
            queue.drainTo(batch);
            Change ch = new Change();
            java.util.Set<String> keys = new java.util.LinkedHashSet<String>();
            try {
                synchronized (swapLock) {
                    java.util.Map<String, Index> fresh = new java.util.LinkedHashMap<String, Index>();
                    for (Job j : batch) {
                        if (j.kind == MARK) continue;
                        Index o = owner(j.path + (j.kind == SYNC ? "/" : ""));
                        if (o == null) continue;
                        Index f = fresh.get(o.key);
                        if (f == null) fresh.put(o.key, f = o.copy());
                        if (apply(f, j, ch)) keys.add(o.key);
                        if (busy) replay.add(j);
                    }
                    for (String k : keys) swap(fresh.get(k));
                }
            } catch (Throwable t) {
                Log.e("תור האינדקס", t);
            }
            for (String k : keys) saveLater(k);
            if (!ch.isEmpty()) announce(ch);
            for (Job j : batch)
                if (j.done != null) try {
                    j.done.run();
                } catch (Throwable t) {
                    Log.e("אחרי עדכון האינדקס", t);
                }
            pending.addAndGet(-batch.size());
        }
    }

    /** שינוי אחד על עותק. ch=null כשמחילים מחדש על תמונה שנבנתה. */
    private static boolean apply(Index f, Job j, Change ch) {
        String root = f.rootPath.endsWith("/") ? f.rootPath : f.rootPath + "/";
        if (j.path == null || !(j.path + "/").startsWith(root)) return false;
        boolean did = false;
        if (j.kind == ADD) {
            did = f.addFile(new File(j.path));
            if (did && ch != null) ch.added.add(j.path);
        } else if (j.kind == REMOVE) {
            did = f.removeFile(j.path);
            if (did && ch != null) ch.removed.add(j.path);
        } else if (j.kind == CARRY) {                    // 1.19.0
            Index.Rec r = f.find(j.path);
            if (r != null && !r.gone) {
                r.added = j.added;
                if (j.sig != 0 && r.size == j.size && r.mtime == j.mtime) r.sig = j.sig;
                did = true;
            }
        } else if (j.kind == SYNC) {
            String p = j.path;                           // תיקייה שעוד אינה באינדקס: הקרובה שמעליה (82)
            int id = f.dirId(p);
            while (id == -2 && p.lastIndexOf('/') > 0 && p.length() > root.length()) {
                p = p.substring(0, p.lastIndexOf('/'));
                id = f.dirId(p);
            }
            if (id != -2 && new File(p).isDirectory()) {
                f.syncDir(id, new File(p));
                did = true;
                if (ch != null) ch.synced.add(j.path);
            }
        }
        return did;
    }

    private void announce(Change c) {
        for (Listener l : listeners)
            try {
                l.onChange(c);
            } catch (Throwable t) {
                Log.e("הודעה על שינוי באינדקס", t);
            }
    }

    /** שמירה לקובץ בחוט נפרד: כמה שינויים רצופים באותו כונן נשמרים פעם אחת. */
    private void saveLater(String key) {
        synchronized (dirty) {
            dirty.add(key);
            dirty.notifyAll();
            if (saver == null) {
                saver = new Thread(new Runnable() {
                    @Override public void run() { saveLoop(); }
                }, "שמירת האינדקס");  // תחביר
                saver.setDaemon(true);
                saver.start();
            }
        }
    }

    private volatile boolean saving;

    private void saveLoop() {
        while (true) {
            List<String> keys;
            synchronized (dirty) {
                while (dirty.isEmpty()) {
                    try {
                        dirty.wait();
                    } catch (InterruptedException e) {
                        return;
                    }
                }
                keys = new ArrayList<String>(dirty);
                dirty.clear();
                saving = true;
            }
            for (String k : keys) {
                Index cur = current(k);
                if (cur == null) continue;
                try {
                    cur.save();                        // 184: השמירה אינה משנה את האינדקס — בלי עותק
                } catch (Throwable t) {
                    Log.e("שמירת אינדקס " + k, t);
                }
            }
            saving = false;
        }
    }

    /** לבדיקות: מחכה עד שהתור ריק והשמירה הסתיימה. */
    boolean flush(long ms) {
        long end = System.currentTimeMillis() + ms;
        while (System.currentTimeMillis() < end) {
            boolean idle;
            synchronized (dirty) {
                idle = pending.get() == 0 && dirty.isEmpty() && !saving;
            }
            if (idle) return true;
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                return false;
            }
        }
        return false;
    }
}
