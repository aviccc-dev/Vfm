package com.tikiyot.vfm;

// מ0.41: התפריטים של התוכנה — מהרשימות (המשתמש, 12.11.2026: "ברשימה 'שמור לתוכנה' יהיו רשימות — שורה למעלה מספר 1 עד 5,
// שורה למטה 1 עד 5 — ובכל רשימה כזו אפשר יהיה להוסיף פקודות, והם יופיעו בתוכנה" · "כדאי להכין אופן שלם 'שמור לתוכנה', ולא רק
// רשימה בדוגמאות"). הכלל האחד: תפריט = דרך בתוך "שמור לתוכנה" ("שורה למעלה 1", "הבית / שורה למטה 1", "בחירה", "פקודות /
// הפקודות שלי"). מה שבתפריט = מה שבדרך הזו באופן "שמור לתוכנה" (הכללי, בא עם התוכנה — בבית שלו), ואחריו מה שבדרך הזו ברשימה
// "שמור לתוכנה" של האופן שעל המסך, אם יש לו (מוסיפה, לא מחליפה). קובץ פקודות — כפתור; תת־רשימה — תפריט משנה; חכמה — מה שהיא מוצאת.
// שורה של מסך: "<שם המסך> / שורה למעלה N" — אחרי הכפתורים של השורה הכללית N. שמות המסכים — של "פתח מסך", ועוד "רשימה", "רשימה חכמה".
// המשפך: התמונה של התפריטים נקראת מהמסד פעם אחת, ונשמרת עד שהמסד משתנה (חותמת הקובץ); הציור — מהזיכרון.
// אחראי: מה בתפריטים, מאיפה, וברירת המחדל (ההתקנה והמעבר מהישן). אסור לו: לצייר (TopRow, DbCmdMenu) או להריץ.

import android.content.Context;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class Menus {

    /** אופן התפריטים: בא עם התוכנה; בבית שלו — כל התפריטים. */
    static final String MODE = Reserved.ROOT;
    static final String ROW_TOP = "שורה למעלה", ROW_BOTTOM = "שורה למטה", SELECT = "בחירה";   // בכונן
    static final int MAX_ROWS = 5;
    /** ברירת המחדל — קובץ יצוא שמצורף לתוכנה (כמו הדוגמאות). */
    static final String ASSET = "examples/shamur.txt";
    static final List<String> CMDS = Collections.singletonList(Reserved.CMDS), MINE = Arrays.asList(Reserved.CMDS, Reserved.MINE),
            SELECT_PATH = Collections.singletonList(SELECT);

    private Menus() { }

    /** הדרך של שורה: כללית (screen == null) — "שורה למעלה 2"; של מסך — "הבית / שורה למעלה 2". */
    static List<String> rowPath(String screen, String side, int n) {
        List<String> p = new ArrayList<String>();
        if (screen != null) p.add(screen);
        p.add(side + " " + n);
        return p;
    }

    // ---------- הלוגיקה (על Store — נבדקת במחשב) ----------

    /** השורש של התפריטים במסד: באופן התפריטים — הבית; באופן עבודה — הרשימה "שמור לתוכנה" שלו (0 — אין לו). */
    static long root(Store s, String mode) {
        return MODE.equals(mode) ? DbPaths.home(s) : DbPaths.find(s, mode, Reserved.ROOT_PATH);
    }

    /** הרשימה שבדרך הזו מתחת ל־root (0 — אין). */
    static long at(Store s, long root, List<String> path) {
        long cur = root;
        for (int i = 0; i < path.size() && cur != 0; i++) cur = DbPaths.child(s, cur, path.get(i));
        return cur;
    }

    /** דרך כמו שנכתבה ("שמור לתוכנה/פקודות", או "פקודות") — בתוך התפריטים. */
    static List<String> relative(List<String> path) {
        return !path.isEmpty() && path.get(0).trim().equals(Reserved.ROOT) ? new ArrayList<String>(path.subList(1, path.size())) : path;
    }

    static String key(List<String> path) {
        StringBuilder sb = new StringBuilder();
        for (String p : path) sb.append(sb.length() > 0 ? "\n" : "").append(p.trim());
        return sb.toString();
    }

    /** שורה בתפריט, ומאיפה היא: האופן והרשימה שהיא בה (כדי לפתוח את הרשימה לסידור). */
    static final class Item {
        final DbMore.Entry e;
        final String mode;
        final long list;
        /** הדרך של הרשימה הזו, בתוך התפריטים. */
        final List<String> path;

        Item(DbMore.Entry e, String mode, long list, List<String> path) {
            this.e = e;
            this.mode = mode;
            this.list = list;
            this.path = path;
        }
    }

    /** התפריטים של מסד אחד: דרך ← השורות שבה, ודרך ← מספר הרשימה. */
    static final class Tree {
        final String mode;
        final Map<String, List<Item>> lists = new LinkedHashMap<String, List<Item>>();
        final Map<String, Long> ids = new LinkedHashMap<String, Long>();
        final Map<String, List<String>> paths = new LinkedHashMap<String, List<String>>();

        Tree(String mode) {
            this.mode = mode;
        }
    }

    /** כל מה שמתחת לשורש התפריטים של המסד, בכל העומק. אין שורש — ריק. */
    static Tree tree(Store s, String mode) {
        Tree t = new Tree(mode);
        long root = root(s, mode);
        if (root != 0) walk(s, t, root, new ArrayList<String>(), new ArrayList<Long>());
        return t;
    }

    private static void walk(Store s, Tree t, long id, List<String> path, List<Long> chain) {
        if (chain.contains(id) || chain.size() > 12) return;                  // רשימה שבתוך עצמה (קישור) — לא חוזרים
        String k = key(path);
        if (t.ids.containsKey(k)) return;                                     // שתי רשימות באותו שם באותו מקום — הראשונה
        t.ids.put(k, id);
        final List<String> here = new ArrayList<String>(path);
        t.paths.put(k, here);
        List<Item> out = new ArrayList<Item>();
        t.lists.put(k, out);
        chain.add(id);
        for (DbMore.Entry e : DbMore.menu(s, id)) {
            out.add(new Item(e, t.mode, id, here));
            if (e.kind != DbMore.E_LIST) continue;
            List<String> p = new ArrayList<String>(path);
            p.add(e.label);
            walk(s, t, e.id, p, chain);
        }
        chain.remove(chain.size() - 1);
    }

    /** התמונה שהמסכים מציירים ממנה: הכללי, ואחריו של האופן. */
    static final class Snap {
        final Tree general, own;

        Snap(Tree general, Tree own) {
            this.general = general;
            this.own = own;
        }

        /** יש רשימה בדרך הזו (בכללי או באופן). */
        boolean has(List<String> path) {
            String k = key(path);
            return (general != null && general.ids.containsKey(k)) || (own != null && own.ids.containsKey(k));
        }

        /**
         * מה שבתפריט: של הכללי, ואחריו של האופן — בלי מה שכבר יש (קובץ — לפי המסלול; תת־רשימה וחכמה — לפי השם: תת־רשימה
         * באותו שם בשניהם היא תפריט משנה אחד, שגם הוא ממוזג).
         */
        List<Item> at(List<String> path) {
            String k = key(path);
            List<Item> out = new ArrayList<Item>();
            for (Tree t : new Tree[]{general, own}) {
                List<Item> l = t == null ? null : t.lists.get(k);
                if (l == null) continue;
                for (Item it : l) if (!has(out, it.e)) out.add(it);
            }
            return out;
        }

        private static boolean has(List<Item> l, DbMore.Entry e) {
            for (Item x : l)
                if (x.e.kind == e.kind && (e.kind == DbMore.E_FILE ? x.e.path.equals(e.path) : x.e.label.equals(e.label))) return true;
            return false;
        }

        /** הרשימה של הדרך, לסידור: {האופן, המספר} — הכללית אם יש, אחרת של האופן. null — אין. */
        Object[] where(List<String> path) {
            String k = key(path);
            for (Tree t : new Tree[]{general, own})
                if (t != null && t.ids.containsKey(k)) return new Object[]{t.mode, t.ids.get(k)};
            return null;
        }

        /** כל הדרכים שיש בהן תפריט (בלי השורש), לפי הסדר — לבורר "☰ תפריט". */
        List<List<String>> paths() {
            Map<String, List<String>> all = new LinkedHashMap<String, List<String>>();
            for (Tree t : new Tree[]{general, own}) if (t != null) all.putAll(t.paths);
            List<List<String>> out = new ArrayList<List<String>>();
            for (List<String> p : all.values()) if (!p.isEmpty()) out.add(p);
            return out;
        }

        /** השורות של מסך בצד אחד (למעלה / למטה): לכל N — הכפתורים של השורה הכללית N, ואחריהם של המסך. שורה ריקה — לא נכנסת. */
        List<Row> rows(String screen, String side) {
            List<Row> out = new ArrayList<Row>();
            for (int n = 1; n <= MAX_ROWS; n++) {
                Row r = new Row(side, n);
                r.items.addAll(at(rowPath(null, side, n)));
                if (screen != null) for (Item it : at(rowPath(screen, side, n))) if (!has(r.items, it.e)) r.items.add(it);
                if (!r.items.isEmpty()) out.add(r);
            }
            return out;
        }
    }

    /** שורה אחת במסך. */
    static final class Row {
        final String side;
        final int n;
        final List<Item> items = new ArrayList<Item>();

        Row(String side, int n) {
            this.side = side;
            this.n = n;
        }
    }

    // ---------- ברירת המחדל והמעבר מהישן (על Store — נבדק במחשב) ----------

    /** ברירת המחדל לתוך הבית של אופן התפריטים: מה שחסר נכנס; מה שיש — נשאר (ייבוא רגיל של קובץ יצוא). */
    static DbMore.Imported defaults(Store s, String asset, String internal) {
        return DbMore.importText(s, DbPaths.ensureHome(s), asset.replace(DbExamples.IN, internal));
    }

    /**
     * המעבר החד־פעמי: "שמור לתוכנה" של אופן אחר (הדוגמאות) — כל מה שבה נכנס לבית של אופן התפריטים, באותן דרכים (יצוא
     * וייבוא רגילים: רשימה שכבר יש — אותה; קובץ שכבר בה — לא פעמיים), ואז היא יוצאת מהאופן שלה, עם רישומי הקבצים שהיו רק
     * בה. מחזיר כמה רשימות עברו (0 — אין שם רשימה כזו). הגיבוי של המסד שלפני — אצל הקורא.
     */
    static int moveFrom(Store src, String srcMode, Store dst) {
        long id = DbPaths.find(src, srcMode, Reserved.ROOT_PATH);
        if (id == 0) return 0;
        DbExport.Out o = DbExport.list(src, id, true, Reserved.ROOT, System.currentTimeMillis());
        DbMore.importText(dst, DbPaths.ensureHome(dst), o.text);
        final List<Long> files = new ArrayList<Long>();
        filesUnder(src, id, files, new ArrayList<Long>());
        long parent = DbPaths.home(src);
        if (parent != 0 && !src.listsOf(id).contains(parent)) parent = 0;
        if (parent != 0) DbListCmd.removeFrom(src, parent, id);
        else DbListCmd.deleteWhole(src, id);
        for (Long f : files) if (src.item(f) != null && src.listsOf(f).isEmpty()) src.purge(f);
        return o.lists + 1;
    }

    private static void filesUnder(Store s, long list, List<Long> out, List<Long> chain) {
        if (chain.contains(list) || chain.size() > 30) return;
        chain.add(list);
        for (Store.Member m : s.members(list)) {
            if (m.item.type == Store.FILE) out.add(m.item.id);
            else if (m.item.type == Store.LIST) filesUnder(s, m.item.id, out, chain);
        }
        chain.remove(chain.size() - 1);
    }

    /** שם המסך של היום לכל "סוג מסך" של קובץ הכפתורים הישן (כפתורים.txt). מסך שאין לו שם — לא עובר. */
    static List<String> screensOf(String old) {
        if (Words.Top.S_HOME.equals(old)) return Arrays.asList(Verbs.S_HOME, Verbs.S_LIST_SCREEN);   // הישן לא הבחין בין הבית לרשימה
        if (Words.Top.S_SEARCH.equals(old)) return Arrays.asList(Verbs.S_SEARCH, Verbs.S_VIRTUAL);
        if (Words.Top.S_DISK.equals(old)) return Collections.singletonList(Verbs.S_DISK);
        if (Words.Top.S_TRASH.equals(old)) return Collections.singletonList(Verbs.S_TRASH);
        if (Words.Top.S_SETTINGS.equals(old)) return Collections.singletonList(Verbs.S_SETTINGS);
        if (Words.Top.S_BATCH.equals(old)) return Collections.singletonList(Verbs.S_EDITOR);
        return Collections.emptyList();
    }

    /** מי שכותב קובץ פקודות לכפתור שאין לו (המעבר מקובץ הכפתורים). */
    interface Maker {
        /** הקובץ של כפתור: צלמית, שם, שורת הפקודה. null — נכשל. */
        File file(String icon, String name, String line);
    }

    /**
     * המעבר החד־פעמי מקובץ הכפתורים הישן: כל מקום שכתוב בו — הרשימה שלו מקבלת בדיוק את הכפתורים שהיו בו, לפי הסדר (מה
     * שנכנס מברירת המחדל לאותה רשימה — יוצא). [למעלה] ← "שורה למעלה N"; [למעלה: <מסך>] ← "<מסך> / שורה למעלה N".
     * מחזיר כמה כפתורים עברו.
     */
    static int fromButtons(Store s, Buttons.Layout lay, Maker maker) {
        final long home = DbPaths.ensureHome(s);
        int n = 0;
        for (Map.Entry<String, List<List<Buttons.Btn>>> e : lay.places.entrySet()) {
            String place = e.getKey();
            String side = place.startsWith(Buttons.TOP) ? ROW_TOP : place.startsWith(Buttons.BOTTOM) ? ROW_BOTTOM : null;
            if (side == null) continue;
            List<String> screens = new ArrayList<String>();
            if (Buttons.general(place)) screens.add(null);
            else {
                int c = place.indexOf(':');
                if (c > 0) screens.addAll(screensOf(place.substring(c + 1).trim()));
            }
            for (String screen : screens) {
                for (int i = 0; i < e.getValue().size() && i < MAX_ROWS; i++) {
                    List<String> paths = new ArrayList<String>();
                    for (Buttons.Btn b : e.getValue().get(i)) {
                        if (Verbs.isVerb(b.act) && Verbs.hostCode(Verbs.parse(b.act)) != null) continue;   // "פעולה של המסך" — בבניית מ לא הוצגה באף מסך
                        File f = maker.file(b.icon, b.name, b.act);
                        if (f != null && !paths.contains(f.getAbsolutePath())) paths.add(f.getAbsolutePath());
                    }
                    if (paths.isEmpty()) continue;
                    final long list = ensure(s, home, rowPath(screen, side, i + 1));
                    final List<Long> was = new ArrayList<Long>();
                    for (Store.Member m : s.members(list)) if (m.item.type == Store.FILE) was.add(m.item.id);
                    s.atomic(new Store.Work() {
                        @Override public void run(Store t) {
                            for (Long id : was) {
                                t.removeMember(list, id);
                                if (t.listsOf(id).isEmpty()) t.purge(id);
                            }
                        }
                    }, true);
                    n += DbFiles.add(s, list, paths, null, false).added;
                }
            }
        }
        return n;
    }

    /** הרשימה בדרך הזו מתחת ל־root — נוצרת אם חסרה (בכל הדרך). */
    static long ensure(Store s, long root, final List<String> path) {
        long cur = root;
        for (String name : path) {
            long c = DbPaths.child(s, cur, name);
            cur = c != 0 ? c : s.createList(Names.cleanName(name), cur);
        }
        return cur;
    }

    // ---------- במכשיר: התמונה השמורה, וההתקנה ----------

    private static final Map<String, Tree> trees = new HashMap<String, Tree>();
    private static final Map<String, Long> stamps = new HashMap<String, Long>();
    private static boolean ready;
    /** מאפיין בבית של אופן התפריטים: ההתקנה (ברירת המחדל והמעבר מהישן) הסתיימה. */
    static final String P_INSTALLED = "menus";

    /** חותמת המסד של אופן: הקובץ ויומן ה־WAL (אורך ותאריך). שינוי — התמונה נקראת מחדש. */
    static long stamp(Context c, String mode) {
        if (mode == null) return 0;
        File db = Db.dbFile(c, mode), wal = new File(db.getPath() + "-wal");
        return db.lastModified() * 31 + db.length() * 17 + wal.lastModified() * 7 + wal.length();
    }

    /** חותמת אחת למסך: התפריטים הכלליים ושל האופן שעל המסך — כדי לדעת מתי לצייר מחדש. */
    static long version(Context c) {
        String cur = DbListScreen.currentMode;
        return stamp(c, MODE) * 1000003L + (cur == null || cur.equals(MODE) ? 0 : stamp(c, cur));
    }

    /** התמונה של עכשיו — מהזיכרון, אם המסדים לא השתנו. לא זורקת: תקלה — תמונה ריקה (והמסך מצייר את שורת החירום). */
    static Snap get(Context c) {
        return of(c, DbListScreen.currentMode);
    }

    /** התמונה לאופן מסוים (הכללי, ואחריו שלו). */
    static synchronized Snap of(Context c, String cur) {
        try {
            ensure(c);
            return new Snap(load(c, MODE), cur == null || cur.equals(MODE) ? null : load(c, cur));
        } catch (Throwable t) {
            Log.e("Menus.get", t);
            return new Snap(null, null);
        }
    }

    private static Tree load(Context c, String mode) {
        long st = stamp(c, mode);
        Tree had = trees.get(mode);
        Long was = stamps.get(mode);
        if (had != null && was != null && was == st) return had;
        Tree t = new Tree(mode);
        if (Db.dbFile(c, mode).isFile()) {
            SqlStore s = null;
            try {
                s = SqlStore.openReadOnly(Db.dbFile(c, mode));
                t = tree(s, mode);
            } catch (Throwable e) {
                Log.e("Menus.load " + mode, e);
            } finally {
                if (s != null) s.close();
            }
        }
        trees.put(mode, t);
        stamps.put(mode, st);                                                 // החותמת שלפני הקריאה: שינוי תוך כדי — ייקרא שוב
        return t;
    }

    /** אופן התפריטים קיים ויש בו בית — אם לא, מותקן עכשיו (פעם אחת; בהפעלה הראשונה של הגרסה). */
    static synchronized void ensure(Context c) {
        if (ready || !Ui.hasFiles(c)) return;
        ready = true;                                                         // ניסיון אחד בכל הפעלה (נכשל — שורת החירום, ושוב בהפעלה הבאה)
        SqlStore s = null;
        try {
            if (Db.dbFile(c, MODE).isFile()) {
                s = SqlStore.openReadOnly(Db.dbFile(c, MODE));
                long home = DbPaths.home(s);
                boolean done = home != 0 && "1".equals(s.prop(home, P_INSTALLED));
                s.close();
                s = null;
                if (done) return;
            }
            install(c);
        } catch (Throwable t) {
            Log.e("Menus.ensure", t);
        } finally {
            if (s != null) s.close();
        }
    }

    /**
     * ההתקנה: ברירת המחדל מהקובץ שבהתקנה; "שמור לתוכנה" שהייתה באופן הדוגמאות — עוברת (עם גיבוי של המסד שלו לפני);
     * קובץ הכפתורים הישן, אם יש — השורות שבו. הקובץ הישן עצמו נשאר במקומו.
     */
    private static void install(Context c) throws Exception {
        String internal = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
        DbExamples.files(null);                                               // קובצי פקודות המערכת והכלים — בכונן
        SqlStore s = SqlStore.open(Db.dbFile(c, MODE), new IdCounter(Db.counterFile()));
        try {
            DbMore.Imported d = defaults(s, DbExamples.asset(c, ASSET), internal);
            Log.i("התפריטים: ברירת המחדל — " + d.lists + " רשימות, " + d.files + " פקודות");
            if (Db.dbFile(c, DbExamples.MODE).isFile()) {
                SqlStore ex = SqlStore.open(Db.dbFile(c, DbExamples.MODE), new IdCounter(Db.counterFile()));
                try {
                    if (DbPaths.find(ex, DbExamples.MODE, Reserved.ROOT_PATH) != 0) {
                        Db.backupsOf(c, DbExamples.MODE).before(ex, Backups.OP_DELETE, System.currentTimeMillis());
                        Log.i("התפריטים: \"שמור לתוכנה\" עברה מהדוגמאות — " + moveFrom(ex, DbExamples.MODE, s) + " רשימות");
                    }
                } finally {
                    ex.close();
                }
            }
            File old = Buttons.file();
            if (old.isFile()) {
                final Map<String, File> system = systemFiles();
                int n = fromButtons(s, Buttons.parseAll(Files.readUtf8(old)), new Maker() {
                    @Override
                    public File file(String icon, String name, String line) {
                        File f = system.get(line.trim());
                        return f != null ? f : Shortcuts.write(icon, name, line);
                    }
                });
                Log.i("התפריטים: מקובץ הכפתורים הישן עברו " + n + " כפתורים");
            }
            s.setProp(DbPaths.home(s), P_INSTALLED, "1");
        } finally {
            s.close();
        }
    }

    /** שורת פקודה ← הקובץ שלה בין פקודות המערכת והכלים (כדי שכפתור ישן של "בית" יהיה אותו קובץ שבתפריט). */
    static Map<String, File> systemFiles() {
        Map<String, File> out = new HashMap<String, File>();
        List<String[]> all = new ArrayList<String[]>(CmdMenu.system());
        all.addAll(DbCmdMenu.dbTools());
        all.add(new String[]{"➕", Reserved.NEW, Verbs.CREATE + " " + Verbs.CMD_WORD});
        for (String[] x : all) out.put(x[2].trim(), new File(Reserved.sysDir(), x[1] + "." + Batch.EXT));
        return out;
    }

    // ---------- רשימת "בחירה": פקודות על מה שנבחר ----------

    /** בקובץ פקודות שברשימת "בחירה": %נבחרים% — במקום הקובץ; הפקודה רצה על כל קובץ שנבחר. */
    static final String V_SELECTED = "נבחרים";   // תחביר
    private static final String SEL = "%" + V_SELECTED + "%";

    /** הטקסט להרצה: כל שורה שיש בה %נבחרים% — פעם אחת לכל קובץ שנבחר, עם המסלול שלו (במרכאות); שאר השורות — כמו שהן. */
    static String expand(String text, List<String> paths) {
        StringBuilder sb = new StringBuilder();
        String[] lines = (text == null ? "" : text).replace("\r", "").split("\n");
        for (String raw : lines) {
            if (!raw.contains(SEL)) {
                sb.append(raw).append('\n');
                continue;
            }
            for (String p : paths) {
                boolean q = false;
                int i = 0;
                while (i < raw.length()) {
                    if (raw.startsWith(SEL, i)) {
                        sb.append(q ? p : "\"" + p + "\"");                    // השורה כבר שמה מרכאות — רק המסלול
                        i += SEL.length();
                        continue;
                    }
                    char c = raw.charAt(i++);
                    if (c == '"') q = !q;
                    sb.append(c);
                }
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    /**
     * כמה קבצים הפקודה מקבלת במקום של %נבחרים% — מההגדרה האחת של הפקודות (Commands): 0 — אין %נבחרים% בקובץ; 1 — קובץ
     * אחד בלבד (שנה, הפעל, פתח…; או %נבחרים% כיעד); 2 — כמה שנבחרו (המקור של העתק / העבר, מחק, הודעה).
     */
    static int arity(String text) {
        int out = 0;
        for (String raw : Bidi.strip(text == null ? "" : text).replace("\r", "").split("\n")) {
            String l = Commands.normal(raw);
            if (!l.contains(SEL) || l.startsWith("::") || Program.isHeader(l)) continue;
            List<String> tk = Batch.tokens(l);
            Commands.Def d = tk.isEmpty() ? null : Commands.get(tk.get(0));
            if (d != null && d.name.equals("REM")) continue;
            int pos = -1, at = 0;
            for (int i = 1; i < tk.size() && pos < 0; i++) {
                String t = tk.get(i);
                if (Batch.isSwitch(t) || TYPE_WORDS.contains(t)) continue;
                if (t.contains(SEL)) pos = at;
                at++;
            }
            boolean many = d != null && d.family == Commands.Family.FILE && (d.name.equals("ECHO")
                    || (pos >= 0 && pos < d.positional().size() && d.positional().get(pos).type == Commands.Type.FILES));
            out = out == 1 || !many ? 1 : 2;
        }
        return out;
    }

    private static final List<String> TYPE_WORDS = Arrays.asList(ListCmd.MANUAL, ListCmd.SMART, ListCmd.CONTENT, ListCmd.FILES_OF,
            ListCmd.FOLDER, ListCmd.FILE_WORD);
    /** פקודות הכונן הפשוטות — רק אותן מתכננים כדי להחליט אם הכפתור מופיע (התכנון שלהן זול). */
    private static final List<String> PLAIN = Arrays.asList("COPY", "MOVE", "DEL", "REN", "MD", "RD", "START", "ECHO");

    /**
     * "הפקודה קובעת בעצמה מתי היא מופיעה": התכנון הרגיל של הטקסט (אותה בדיקה שלפני כל הרצה) — בלי שגיאות: מתאימה. null —
     * לא נבדק כאן: יש בקובץ מקום פתוח (נשאל רק בהרצה), או שורה שהתכנון שלה יקר (רשימות, אופן, פקודות פתיחה).
     */
    static Boolean planned(String expanded, File here, Map<String, String> vars, Library lib) {
        List<String> lines = Arrays.asList(Bidi.strip(expanded).replace("\r", "").split("\n"));
        if (!CmdTemplates.holes(lines).isEmpty()) return null;
        boolean any = false;
        for (String raw : lines) {
            String l = Commands.normal(raw);
            if (l.isEmpty() || l.startsWith("::") || Program.isHeader(l)) continue;
            List<String> tk = Batch.tokens(l);
            Commands.Def d = tk.isEmpty() ? null : Commands.get(tk.get(0));
            if (d != null && d.name.equals("REM")) continue;
            if (d == null || !PLAIN.contains(d.name) || ListCmd.parse(l, new ArrayList<String>()) != null) return null;
            any = true;
        }
        if (!any) return null;
        Batch.Plan p = Batch.plan(expanded, here, vars, lib);
        return p.errors == 0;
    }

    private static final Map<String, Boolean> fitCache = new HashMap<String, Boolean>();

    /**
     * האם פקודה מרשימת "בחירה" מתאימה למה שנבחר עכשיו. המשפך: קודם כמה נבחרו מול מה שהפקודה מקבלת (זול, תמיד); ורק אז
     * התכנון — על דוגמה (קובץ אחד או שניים), ופעם אחת לכל צורת בחירה (אחד / כמה): התוצאה נשמרת עד שהקובץ משתנה.
     */
    static boolean fits(Context c, File f, String text, List<String> files, List<String> dirs, int lists) {
        int n = files.size() + dirs.size();
        if (lists > 0 || n == 0) return false;                                // רשימות שנבחרו — לא קבצים
        int ar = arity(text);
        if (ar == 0) return true;
        if (ar == 1 && n != 1) return false;
        if (!dirs.isEmpty()) return true;                                     // תיקייה — התכנון שלה סורק את כולה; לא כאן
        String key = f.getPath() + "\n" + f.lastModified() + "\n" + (n == 1 ? 1 : 2);
        Boolean had;
        synchronized (fitCache) {
            had = fitCache.get(key);
        }
        if (had != null) return had;
        boolean ok = true;
        try {
            Boolean p = planned(expand(text, files.subList(0, Math.min(2, files.size()))), f.getParentFile(),
                    Batch.vars(f.getParentFile(), Vols.internalRoot(c), Vols.cardRoot(c)), Library.get(c));
            ok = p == null || p;
        } catch (Throwable t) {
            Log.e("Menus.fits", t);
        }
        synchronized (fitCache) {
            if (fitCache.size() > 300) fitCache.clear();
            fitCache.put(key, ok);
        }
        return ok;
    }

    private static final Map<String, String[]> texts = new HashMap<String, String[]>();

    /** הטקסט של קובץ פקודות — נקרא פעם אחת, עד שהקובץ משתנה (פס הבחירה שואל בכל שינוי של הבחירה). null — לא נקרא. */
    static synchronized String textOf(File f) {
        String k = f.lastModified() + "\n" + f.length();
        String[] had = texts.get(f.getPath());
        if (had != null && had[1].equals(k)) return had[0];
        try {
            String t = Files.readUtf8(f);
            if (texts.size() > 300) texts.clear();
            texts.put(f.getPath(), new String[]{t, k});
            return t;
        } catch (Throwable e) {
            return null;
        }
    }

    // ---------- איך כפתור נראה ----------

    private static final Map<String, String[]> looks = new HashMap<String, String[]>();

    /** הצלמית והשם של קובץ פקודות (מהכותרת שלו) — נקרא פעם אחת, עד שהקובץ משתנה. */
    static synchronized String[] look(File f) {
        String k = f.getPath() + "\n" + f.lastModified() + "\n" + f.length();
        String[] had = looks.get(f.getPath());
        if (had != null && had[2].equals(k)) return had;
        Program p = Program.read(f);
        String[] now = {p.isProgram() ? p.icon : "▶", p.title(f), k};
        looks.put(f.getPath(), now);
        return now;
    }
}
