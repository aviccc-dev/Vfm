package com.tikiyot.vfm;

// מ0.2 — העתקה מהמנוע הישן (מבנה.txt + חתימות.txt) אל המסד. אחראי: לקרוא מקור, ולהעתיק ל־Store בעסקה אחת.
// אסור לו: לשנות את הקבצים הישנים (קורא בלבד), לדעת איך Store שומר, או להציג דבר למשתמש (המסך עושה את זה).
//
// כללים (claude/masad-mipuy-haataka.md):
//  - מה מועתק: רשימות ידניות בעץ (עם הסדר), צלמית/תיאור/עיצוב/"למה", קבצים (מסלול, כונן, גודל ותאריך, חתימה),
//    ומ־מ0.3 גם רשימות חכמות: כל ההגדרות הנפוצות (עצמאיות), וכל @חכמה ברשימה ידנית — חברות של הרשימה החכמה בה
//    (פנימית שמקושרת להגדרה נפוצה = אותו פריט, כמו בישן). התנאי נשמר כטקסט, כפי שהוא.
//    ומ־מ0.4 הפניות בין רשימות (@רשימה): אותה רשימה נכנסת גם לרשימה המפנה (חברות, בלי "הורה").
//    ומ־מ0.7 תיקייה חיה (@תיקייה) — כמו שהמנוע הישן עצמו מסב אותה מאז 1.17.3 (Unify): רשימה חכמה של התיקייה.
//    ומ־מ0.6 מסך הבית: רשימה אחת מסומנת (P_HOME), והכלים בה לפי הסדר — רשימה ידנית (אותה רשימה), חכמה, קובץ פקודות.
//  - לא מועתק, ונספר בדוח: הפניה שהייתה סוגרת מעגל (במנוע הישן ייתכן; במסד לא), הפניה לרשימה שאינה קיימת,
//    @חכמה שאינה מובילה להגדרה (נמחקה, כלי, כפולים), "רשימות בתוך" שבתוך חכמה, שורות פגומות. שום דבר לא נמחק או משתנה במקור.
//  - הסדר: קודם כל הרשימות נוצרות (או נמצאות) עומדות לעצמן, ואז כל רשימה מקבלת את חבריה לפי @סדר — כך הפניה
//    יכולה להצביע גם על רשימה שמופיעה מאוחר יותר בקובץ.
//  - העתקה חוזרת = עדכון, לא כפילות: רשימה מזוהה לפי האופן והמסלול שלה במקור (origin_key), קובץ לפי המסלול שלו.
//    רשימה שכבר הועתקה לא משתנה במבנה; מאפיין ריק בה מתמלא, מאפיין שיש לא נדרס. קבצים חדשים נוספים לסוף.
//    מה שנמחק במקור לא נמחק במסד.
//  - התצוגה המקדימה היא אותה ריצה בדיוק בלי שמירה (Store.atomic עם keep=false) — הדוח שלה זהה לדוח ההעתקה.

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class Importer {

    private Importer() { }

    /**
     * מפתח המקור של רשימה שהועתקה מהמנוע הישן: שם האופן ואחריו המסלול המלא ("א / ב"). שם האופן חלק מהמפתח — שתי רשימות
     * באותו מסלול באופנים שונים הן שתי רשימות (באג שנתפס במכשיר: בלי השם הן התמזגו). מ־מ0.2: מסד נפרד לכל אופן, והמפתח מגן גם אם טעו ביעד.
     */
    static String originOf(String mode, String listPath) {
        return "tree:" + mode + ":L:" + listPath;  // תחביר
    }
    /** כונן שלא זוהה ממסלולו. */
    static final String UNKNOWN_DRIVE = "?";

    /** מפתח המקור של רשימה חכמה: האופן ומזהה ההגדרה בקובץ הראשי (s-… נפוצה, p-… פנימית) — המזהה יציב במנוע הישן. */
    static String smartOrigin(String mode, String key) {
        return "ini:" + mode + ":S:" + key;  // תחביר
    }

    /**
     * מ0.7: מפתח המקור של רשימה חכמה שהייתה תיקייה חיה: האופן, מסלול הרשימה שבה הייתה (השורש — ריק) והתיקייה. אותה תיקייה
     * בשתי רשימות — שתי רשימות חכמות, כמו שהמנוע הישן מסב אותה (רשימה פנימית לכל רשימה).
     */
    static String liveOrigin(String mode, String listPath, String folder) {
        return "tree:" + mode + ":F:" + listPath + "\t" + folder;  // תחביר
    }

    /** מ0.7: התנאי, המיון והתצוגה של רשימה חכמה של תיקייה — אותם ערכים כמו Unify.folderKeys, מאותו מקור. */
    static Map<String, String> liveKeys(String folder) {
        return Unify.folderKeys(folder);
    }

    /** מ0.7: שם הרשימה החכמה של תיקייה חיה — שם התיקייה (כמו בהסבה של המנוע הישן). */
    static String liveName(String folder) {
        String f = folder;
        while (f.length() > 1 && f.endsWith("/")) f = f.substring(0, f.length() - 1);
        return new File(f).getName().isEmpty() ? f : new File(f).getName();
    }

    /** מ0.6: מפתח המקור של רשימת הבית של האופן. */
    static String homeOrigin(String mode) {
        return "ini:" + mode + ":H";  // תחביר
    }

    /** מ0.6: רשימת הבית במסד (לפי הסימון, לא לפי המקור — גם בית שלא הגיע מהעתקה), או 0. */
    static long home(Store s) {
        for (Store.Item it : s.lists()) if ("1".equals(s.prop(it.id, Store.P_HOME))) return it.id;
        return 0;
    }

    /** מ0.6: מסלול הרשימה של כלי רשימה ידנית. כלי ישן בלי רשימה — הרשימה בשמו (כמו FnVirtual.ensureOwn). */
    static List<String> toolList(Structure.Func f) {
        String p = f.params.get(FnVirtual.LIST);
        return Names.parsePath(p == null || p.trim().isEmpty() ? f.name : p);
    }

    /** מ0.6: מסלול קובץ הפקודות של כלי פקודה, או "" (אין / לא מסלול מלא). */
    static String toolFile(Structure.Func f) {
        String p = f.params.get(FnCommand.FILE);
        p = p == null ? "" : p.trim();
        return p.startsWith("/") ? p : "";
    }

    /** הגדרה של רשימה חכמה במנוע הישן: מה שמועתק. nested = כמה "רשימות בתוך" יש בה (לא מועתקות במ0.3). */
    static final class Smart {
        final String key, name, cond, sort, dir, view;
        final int nested;

        Smart(String key, String name, Map<String, String> p) {
            this.key = key;
            this.name = name;
            this.cond = val(p, SmartLists.COND);
            this.sort = val(p, SmartLists.SORT);
            this.dir = val(p, SmartLists.DIR);
            this.view = val(p, SmartLists.VIEW);
            int n = 0;
            for (String x : val(p, Nested.KEY).split("\\|")) if (!x.trim().isEmpty()) n++;
            this.nested = n;
        }

        private static String val(Map<String, String> p, String k) {
            String v = p.get(k);
            return v == null ? "" : v.trim();
        }
    }

    /** מה נקרא מהמקור. */
    static final class Source {
        final String mode;
        final Tree tree;
        final Map<String, Sig.Entry> sigs;
        /** הקובץ הראשי של האופן (הגדרות נפוצות ורשימות פנימיות); null — אין. */
        final Ini ini;
        private final Map<String, Smart> cache = new HashMap<String, Smart>();

        Source(String mode, Tree tree, Map<String, Sig.Entry> sigs) {
            this(mode, tree, sigs, null);
        }

        Source(String mode, Tree tree, Map<String, Sig.Entry> sigs, Ini ini) {
            this.mode = mode;
            this.tree = tree;
            this.sigs = sigs;
            this.ini = ini;
        }

        /** כל ההגדרות הנפוצות, לפי הסדר בקובץ. */
        List<Smart> commons() {
            List<Smart> out = new ArrayList<Smart>();
            if (ini == null) return out;
            String id = "", name = "";
            Map<String, String> params = null;
            for (Ini.Line l : ini.lines) {
                if (l.isHeader()) {
                    if (params != null && !id.isEmpty()) out.add(new Smart(id, name, params));
                    params = null;
                    id = "";
                    if (l.headerName().startsWith(SmartLists.SECTION)) {
                        name = l.headerName().substring(SmartLists.SECTION.length()).trim();
                        params = new java.util.LinkedHashMap<String, String>();
                    }
                    continue;
                }
                if (params == null || l.key == null) continue;
                if (l.key.equals(Structure.ID)) id = l.value == null ? "" : l.value;
                else params.put(l.key, l.value == null ? "" : l.value);
            }
            if (params != null && !id.isEmpty()) out.add(new Smart(id, name, params));
            return out;
        }

        /** ההגדרה שההפניה (@חכמה) מובילה אליה, או null: נמחקה, כלי, כפולים. */
        Smart smart(String ref) {
            if (ini == null || ref == null || ref.isEmpty()) return null;
            if (cache.containsKey(ref)) return cache.get(ref);
            Smart out = resolve(ref);
            cache.put(ref, out);
            return out;
        }

        private Smart resolve(String ref) {
            Structure.Func f = Structure.byId(ini, ref);
            if (f == null) return null;
            if (f.name.startsWith(SmartLists.SECTION))
                return new Smart(ref, f.name.substring(SmartLists.SECTION.length()).trim(), f.params);
            if (!f.name.startsWith(Nested.INNER)) return null;
            if (FnDup.KIND.equals(f.params.get("סוג"))) return null;  // תחביר
            String link = f.params.get(SmartLists.LINK);
            if (link != null && !link.isEmpty()) {
                Structure.Func c = Structure.byId(ini, link);
                if (c != null && c.name.startsWith(SmartLists.SECTION)) return resolve(link);   // מקושרת — אותו פריט
            }
            String h = f.name.substring(Nested.INNER.length());
            int cut = h.lastIndexOf(" › ");
            return new Smart(ref, (cut >= 0 ? h.substring(cut + 3) : h).trim(), f.params);
        }

        /** כל ההגדרות להעתקה: הנפוצות, ואחריהן מה שהרשימות מצביעות עליו — בלי כפילויות, לפי סדר העץ. */
        List<Smart> wanted() {
            Map<String, Smart> out = new java.util.LinkedHashMap<String, Smart>();
            for (Smart c : commons()) out.put(c.key, c);
            wanted(tree.root, out);
            return new ArrayList<Smart>(out.values());
        }

        private void wanted(Tree.Node n, Map<String, Smart> out) {
            for (String ref : n.smart) {
                Smart sm = smart(ref);
                if (sm != null && !out.containsKey(sm.key)) out.put(sm.key, sm);
            }
            for (Tree.Node c : n.children) wanted(c, out);
        }

        /** מ0.6: הכלים שבמסך הבית, לפי הסדר במסך (הסביבות הישנות כבר הוסבו לרשימות בפתיחת התוכנה הישנה). */
        List<Structure.Func> tools() {
            if (ini == null) return new ArrayList<Structure.Func>();
            return Structure.parse(ini).direct;
        }

        /** מ0.6: ההגדרה של כלי רשימה חכמה בבית: מקושר להגדרה נפוצה — אותו פריט כמו הנפוצה; אחרת — ההגדרות שבכלי עצמו. */
        Smart toolSmart(Structure.Func f) {
            String link = f.params.get(SmartLists.LINK);
            if (link != null && !link.isEmpty()) {
                Structure.Func c = Structure.byId(ini, link);
                if (c != null && c.name.startsWith(SmartLists.SECTION)) return resolve(link);
            }
            return new Smart(f.id, f.name, f.params);
        }

        /** כמה הפניות @חכמה אינן מובילות להגדרה (ולכן לא מועתקות). */
        int skippedSmart() {
            return skipped(tree.root);
        }

        private int skipped(Tree.Node n) {
            int k = 0;
            for (String ref : n.smart) if (smart(ref) == null) k++;
            for (Tree.Node c : n.children) k += skipped(c);
            return k;
        }
    }

    /** הדוח: מה נעשה (או מה היה נעשה, בתצוגה מקדימה). */
    static final class Report {
        int listsNew, listsKnown;
        /** רשימות חכמות (מ0.3): הגדרות חדשות / שכבר הועתקו. */
        int smartNew, smartKnown;
        int filesNew, filesKnown, filesUpdated, filesWithSig;
        int linksAdded, linksKnown, rootFiles;
        int propsSet;
        /** smart = הפניות @חכמה שלא הועתקו; nested = "רשימות בתוך" חכמה שלא הועתקו; live (מ0.7) = תיקיות חיות במקור — מועתקות כרשימות חכמות. */
        int badLines, smart, live, nested;
        /** הפניות בין רשימות (מ0.4): נוספו / כבר היו / לא הועתקו כי היו סוגרות מעגל / לרשימה שלא קיימת. */
        int refsAdded, refsKnown, refCycles, refMissing;
        /**
         * מסך הבית (מ0.6): כלים במקור / נכנסו לבית / כבר היו בו / סוג שלא מועתק (כפולים, פס, אפליקציה, סל, הגדרות) /
         * הרשימה או הקובץ שלהם לא קיימים (או שהיו סוגרים מעגל). homeNew — רשימת הבית נוצרה בריצה הזו.
         */
        int homeTools, homeAdded, homeKnown, homeSkipped, homeMissing;
        /** מ0.19: כלים שהפכו לקובץ פקודות קטן. */
        int homeToolFiles;
        boolean homeNew;

        boolean nothingToDo() {
            return listsNew == 0 && smartNew == 0 && refsAdded == 0 && filesNew == 0 && filesUpdated == 0 && linksAdded == 0 && propsSet == 0
                    && homeAdded == 0 && !homeNew;
        }

        boolean skippedAny() {
            return badLines + refCycles + refMissing + smart + nested + homeSkipped + homeMissing > 0;
        }
    }

    // ---------- מקור ----------

    /** כל תיקיות האופנים הישנים שיש בהן מבנה.txt, לפי שם. */
    static List<String> modes(File oldData) {
        List<String> out = new ArrayList<String>();
        File[] k = new File(oldData, Modes.DIR).listFiles();
        if (k != null)
            for (File f : k)
                if (f.isDirectory() && !f.getName().startsWith(".") && new File(f, Paths.TREE).isFile()) out.add(f.getName());
        java.util.Collections.sort(out);
        return out;
    }

    /** שם האופן הפעיל בתיקייה הישנה (כמו Modes.current, אבל לתיקייה נתונה). */
    static String activeMode(File oldData) {
        String m = "";
        try {
            m = Files.readUtf8(Modes.pointer(oldData)).trim();
        } catch (Throwable ignored) {
        }
        m = Modes.clean(m);
        return m.isEmpty() ? Modes.FIRST : m;
    }

    /** קורא מקור מתיקיית אופן. @throws Store.Failure אם אין מבנה.txt או שאי אפשר לקרוא אותו (ואז לא מעתיקים כלום). */
    static Source load(File modeDir) {
        File t = new File(modeDir, Paths.TREE);
        if (!t.isFile()) throw new Store.Failure("no tree file: " + t);
        Tree tree = Tree.loadFrom(t);
        if (tree.unreadable) throw new Store.Failure("tree file unreadable: " + t);
        Ini ini = Ini.load(new File(new File(modeDir, Paths.SETTINGS), Paths.MAIN));
        return new Source(modeDir.getName(), tree, Sig.read(new File(modeDir, Sig.FILE)), ini.unreadable ? null : ini);
    }

    // ---------- ההעתקה ----------

    /** הכונן לפי המסלול: "פנימי", או שם הכרטיס (/storage/XXXX-XXXX/…), או "?". */
    static String driveOf(String path) {
        if (Drive.internalPath(path)) return Drive.INTERNAL;
        if (path.startsWith("/storage/")) {
            int e = path.indexOf('/', 9);
            String v = e < 0 ? path.substring(9) : path.substring(9, e);
            if (!v.isEmpty() && !v.equals("emulated") && !v.equals("self")) return v;
        }
        return UNKNOWN_DRIVE;
    }

    /**
     * מעתיק (keep=true) או רק בודק מה היה קורה (keep=false) — בעסקה אחת: כישלון באמצע = שום שינוי.
     * @throws Store.Failure / RuntimeException אם נכשל (ואז המסד כמו שהיה)
     */
    /** הצלמית של כלי: מה שנכתב בו (פרמטר "צלמית"), ואם אין — של הסוג. */
    static String iconOf(Structure.Func f) {
        String i = f.params.get("צלמית");  // תחביר
        return i != null && !i.trim().isEmpty() ? i.trim() : Func.iconOf(f.kind);
    }

    /** מ0.19: כותב קובץ פקודות לכלי (בהעתקה האמיתית בלבד — לא בבדיקה המלאה). */
    interface ToolFiles {
        java.io.File write(String icon, String name, String line);
    }

    /** null — כלי שאינו רשימה/חכמה/פקודה לא מועתק (נספר), כמו עד מ0.18. */
    static ToolFiles toolFiles;

    static Report run(Store store, final Source src, boolean keep) {
        final Report r = new Report();
        store.atomic(new Store.Work() {
            @Override
            public void run(Store s) {
                Ctx c = new Ctx(s, src, r);
                c.top(src.tree.root);
            }
        }, keep);
        return r;
    }

    /** הפניה ברשימה (מסלול הרשימה שאליה מפנים) — כדי להבדיל ממזהה של רשימה חכמה בסדר. */
    private static final class Ref {
        final String path;

        Ref(String path) {
            this.path = path;
        }

        @Override public boolean equals(Object o) { return o instanceof Ref && ((Ref) o).path.equals(path); }

        @Override public int hashCode() { return path.hashCode() * 31 + 7; }
    }

    /** תיקייה חיה ברשימה (מ0.7). */
    private static final class Live {
        final String folder;

        Live(String folder) {
            this.folder = folder;
        }

        @Override public boolean equals(Object o) { return o instanceof Live && ((Live) o).folder.equals(folder); }

        @Override public int hashCode() { return folder.hashCode() * 31 + 11; }
    }

    private static final class Ctx {
        final Store s;
        final Source src;
        final Report r;
        /** מסלול ← פריט, לריצה הזו (קובץ שבכמה רשימות — פריט אחד). */
        final Map<String, Long> files = new HashMap<String, Long>();
        /** מזהה הגדרה ← פריט, לריצה הזו (פנימית מקושרת ונפוצה — פריט אחד). */
        final Map<String, Long> smarts = new HashMap<String, Long>();
        /** רשימה במקור ← פריט; ואילו נוצרו בריצה הזו (רק הן נכנסות לרשימת האב — רשימה ישנה לא משנה מקום). */
        final Map<Tree.Node, Long> ids = new HashMap<Tree.Node, Long>();
        final Set<Tree.Node> fresh = new HashSet<Tree.Node>();

        Ctx(Store s, Source src, Report r) {
            this.s = s;
            this.src = src;
            this.r = r;
        }

        /** השורש: אין לו רשימה — הקבצים שבו נשארים בלי רשימה, והרשימות שלו עומדות לעצמן. */
        void top(Tree.Node root) {
            count(root);
            for (Smart c : src.commons()) smart(c);                           // הנפוצות עומדות לעצמן
            for (String ref : root.smart) {
                Smart sm = src.smart(ref);
                if (sm == null) r.smart++;
                else smart(sm);
            }
            for (String lp : root.live) live(root, lp);                       // מ0.7: בשורש — עומדות לעצמן
            for (Tree.Node c : root.children) ensure(c);
            for (Tree.Node c : root.children) list(c);
            r.refMissing += root.refs.size();                                 // בשורש אין רשימה מפנה
            for (Tree.Entry e : root.entries) {
                if (e.bad) {
                    r.badLines++;
                    continue;
                }
                file(e);
                r.rootFiles++;
            }
            home();
        }

        /**
         * מ0.6, אחרון — כשכל הרשימות, החכמות והקבצים כבר קיימים: רשימת הבית, והכלים בה לפי הסדר. רשימה ידנית — אותה רשימה
         * (הפניה); חכמה — הפריט שלה; פקודה — פריט הקובץ של קובץ הפקודות. שאר הסוגים לא מועתקים (נספרים). כמו בכל רשימה
         * שכבר הועתקה: מה שכבר בבית נשאר במקומו, וכלי חדש נוסף לסוף.
         */
        void home() {
            long h = s.findByOrigin(homeOrigin(src.mode));
            if (h == 0) h = Importer.home(s);
            for (Structure.Func f : src.tools()) {
                r.homeTools++;
                long id = tool(f);
                if (id == 0) continue;
                if (h == 0) {                                                 // נוצרת רק כשיש מה להכניס — לא בית ריק
                    h = s.createList(Words.Db.HOME_NAME, 0);
                    s.setOrigin(h, homeOrigin(src.mode));
                    s.setProp(h, Store.P_HOME, "1");
                    r.homeNew = true;
                }
                if (s.makesCycle(h, id)) {
                    r.homeMissing++;
                    continue;
                }
                if (s.addMember(h, id)) r.homeAdded++;
                else r.homeKnown++;
            }
        }

        /** הפריט שהכלי מצביע עליו, או 0 (ואז נספר: סוג שלא מועתק, או שאין למה להצביע). */
        long tool(Structure.Func f) {
            if (FnVirtual.KIND.equals(f.kind)) {
                long id = s.findByOrigin(originOf(src.mode, Names.fullLabel(toolList(f))));
                if (id == 0) {
                    r.homeMissing++;
                    return 0;
                }
                prop(id, Store.P_DESC, f.description, false);
                return id;
            }
            if (FnSmart.KIND.equals(f.kind)) {
                long id = smart(src.toolSmart(f));
                prop(id, Store.P_DESC, f.description, false);
                return id;
            }
            if (FnCommand.KIND.equals(f.kind)) {
                String p = toolFile(f);
                if (p.isEmpty()) {
                    r.homeMissing++;
                    return 0;
                }
                return file(p, -1);
            }
            if (toolFiles != null) {                                          // מ0.19: כלי אחר — קובץ פקודות קטן ("פתח כלי …")
                java.io.File cf = toolFiles.write(iconOf(f), f.name, Verbs.toolLine(f.kind, f.params));
                if (cf != null) {
                    r.homeToolFiles++;
                    return file(cf.getAbsolutePath(), -1);
                }
            }
            r.homeSkipped++;
            return 0;
        }

        void count(Tree.Node n) {
            r.live += n.live.size();
        }

        /** שלב א: הרשימה ותתי־הרשימות שלה קיימות במסד (עומדות לעצמן), עם המאפיינים. */
        void ensure(Tree.Node n) {
            String key = originOf(src.mode, Names.fullLabel(n.path()));
            long id = s.findByOrigin(key);
            if (id == 0) {
                id = s.createList(n.name, 0);
                s.setOrigin(id, key);
                fresh.add(n);
                r.listsNew++;
                prop(id, Store.P_ICON, n.icon, true);
                prop(id, Store.P_DESC, n.desc, true);
                prop(id, Store.P_DESIGN, n.design, true);
                prop(id, Store.P_WHY, n.why, true);
            } else {
                r.listsKnown++;
                prop(id, Store.P_ICON, n.icon, false);
                prop(id, Store.P_DESC, n.desc, false);
                prop(id, Store.P_DESIGN, n.design, false);
                prop(id, Store.P_WHY, n.why, false);
            }
            ids.put(n, id);
            for (Tree.Node c : n.children) ensure(c);
        }

        /** שלב ב: החברים של הרשימה, לפי הסדר. */
        void list(Tree.Node n) {
            count(n);
            long id = ids.get(n);
            for (Object o : order(n)) {
                if (o instanceof Tree.Node) {
                    Tree.Node c = (Tree.Node) o;
                    if (fresh.contains(c)) s.addMember(id, ids.get(c));
                    list(c);
                } else if (o instanceof Ref) {
                    ref(id, ((Ref) o).path);
                } else if (o instanceof Live) {
                    if (s.addMember(id, live(n, ((Live) o).folder))) r.linksAdded++;
                    else r.linksKnown++;
                } else if (o instanceof String) {
                    Smart sm = src.smart((String) o);
                    if (sm == null) {
                        r.smart++;
                        continue;
                    }
                    if (s.addMember(id, smart(sm))) r.linksAdded++;
                    else r.linksKnown++;
                } else {
                    Tree.Entry e = (Tree.Entry) o;
                    if (e.bad) {
                        r.badLines++;
                        continue;
                    }
                    if (s.addMember(id, file(e))) r.linksAdded++;
                    else r.linksKnown++;
                }
            }
        }

        /** הפניה: אותה רשימה נכנסת גם לכאן — אלא אם תיסגר כך שרשרת (מעגל), או שהרשימה לא קיימת. */
        void ref(long id, String path) {
            long target = s.findByOrigin(originOf(src.mode, Names.fullLabel(Names.parsePath(path))));
            if (target == 0) r.refMissing++;
            else if (s.makesCycle(id, target)) r.refCycles++;
            else if (s.addMember(id, target)) r.refsAdded++;
            else r.refsKnown++;
        }

        /**
         * מ0.7: תיקייה חיה ← רשימה חכמה של התיקייה (מקור:"…", לפי שם, בתיקיות — כולל הכול), בשם התיקייה. קיימת — רק מאפיינים
         * ריקים מתמלאים (עריכת המשתמש נשמרת), כמו כל רשימה חכמה.
         */
        long live(Tree.Node n, String folder) {
            String key = liveOrigin(src.mode, n.parent == null ? "" : Names.fullLabel(n.path()), folder);
            Long seen = smarts.get(key);
            if (seen != null) return seen;
            long id = s.findByOrigin(key);
            boolean isNew = id == 0;
            if (isNew) {
                id = s.createSmart(liveName(folder), 0);
                s.setOrigin(id, key);
                r.smartNew++;
            } else {
                r.smartKnown++;
            }
            Map<String, String> kv = liveKeys(folder);
            prop(id, Store.P_COND, kv.get(SmartLists.COND), isNew);
            prop(id, Store.P_SORT, kv.get(SmartLists.SORT), isNew);
            prop(id, Store.P_VIEW, kv.get(SmartLists.VIEW), isNew);
            smarts.put(key, id);
            return id;
        }

        /** פריט הרשימה החכמה: קיים — רק מאפיינים ריקים מתמלאים (עריכת המשתמש נשמרת); חדש — נוצר. */
        long smart(Smart sm) {
            Long seen = smarts.get(sm.key);
            if (seen != null) return seen;
            String key = smartOrigin(src.mode, sm.key);
            long id = s.findByOrigin(key);
            boolean isNew = id == 0;
            if (isNew) {
                id = s.createSmart(sm.name, 0);
                s.setOrigin(id, key);
                r.smartNew++;
            } else {
                r.smartKnown++;
            }
            prop(id, Store.P_COND, sm.cond, isNew);
            prop(id, Store.P_SORT, sm.sort, isNew);
            prop(id, Store.P_DIR, sm.dir, isNew);
            prop(id, Store.P_VIEW, sm.view, isNew);
            r.nested += sm.nested;
            smarts.put(sm.key, id);
            return id;
        }

        void prop(long id, String k, String v, boolean isNew) {
            if (v == null || v.isEmpty()) return;
            if (!isNew && s.prop(id, k) != null) return;
            s.setProp(id, k, v);
            if (!isNew) r.propsSet++;
        }

        /** פריט הקובץ של הרשומה: קיים — מתעדכן אם החתימה במקור שונה; חדש — נוצר. */
        long file(Tree.Entry e) {
            return file(e.path, e.size);
        }

        /** כמו file(Entry), למסלול (מ0.6: גם קובץ פקודות שבבית). listedSize — הגודל שנרשם, -1 לא ידוע. */
        long file(String path, long listedSize) {
            Long seen = files.get(path);
            if (seen != null) return seen;
            Sig.Entry g = src.sigs.get(path);
            long size = g != null ? g.size : listedSize;
            long mtime = g != null ? g.mtime : 0;
            String sig = g != null ? g.sig : null;
            long id = s.findFile(path);
            if (id == 0) {
                id = s.createFile(new Store.FileInfo(path, driveOf(path), size, mtime, sig));
                r.filesNew++;
                if (sig != null) r.filesWithSig++;
            } else {
                Store.FileInfo f = s.file(id);
                boolean differs = g != null && (f.sig == null || !f.sig.equals(sig) || f.size != size || f.mtime != mtime);
                if (differs) {
                    s.updateFile(id, size, mtime, sig);
                    r.filesUpdated++;
                } else {
                    r.filesKnown++;
                }
                if (sig != null) r.filesWithSig++;
            }
            files.put(path, id);
            return id;
        }

        /**
         * סדר הפריטים ברשימה: אם יש סדר חופשי (@סדר) — לפיו (רשימה: "c"+שם, קובץ: "f"+מסלול; מפתחות של סוגים שלא
         * מועתקים מדולגים); מה שלא נזכר בו — אחריו, בסדר הרגיל: תת־רשימות, רשימות חכמות, הפניות ואז קבצים.
         */
        List<Object> order(Tree.Node n) {
            List<Object> out = new ArrayList<Object>();
            Set<Object> used = new HashSet<Object>();
            for (String k : n.order) {
                if (k.length() < 2) continue;
                String v = k.substring(1);
                Object o = null;
                if (k.charAt(0) == 'c') o = n.child(v);
                else if (k.charAt(0) == 's') { if (n.smart.contains(v)) o = v; }
                else if (k.charAt(0) == 'r') { if (n.refs.contains(v)) o = new Ref(v); }
                else if (k.charAt(0) == 'l') { if (n.live.contains(v)) o = new Live(v); }
                else if (k.charAt(0) == 'f') for (Tree.Entry e : n.entries) if (e.path.equals(v)) { o = e; break; }
                if (o != null && used.add(o)) out.add(o);
            }
            for (Tree.Node c : n.children) if (used.add(c)) out.add(c);
            for (String ref : n.smart) if (used.add(ref)) out.add(ref);
            for (String lp : n.live) {                                       // מ0.7: אחרי החכמות — כמו בהסבה של הישן
                Live x = new Live(lp);
                if (used.add(x)) out.add(x);
            }
            for (String ref : n.refs) {
                Ref x = new Ref(ref);
                if (used.add(x)) out.add(x);
            }
            for (Tree.Entry e : n.entries) if (used.add(e)) out.add(e);
            return out;
        }
    }
}
