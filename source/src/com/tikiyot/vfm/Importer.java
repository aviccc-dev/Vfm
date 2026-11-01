package com.tikiyot.vfm;

// מ0.2 — העתקה מהמנוע הישן (מבנה.txt + חתימות.txt) אל המסד. אחראי: לקרוא מקור, ולהעתיק ל־Store בעסקה אחת.
// אסור לו: לשנות את הקבצים הישנים (קורא בלבד), לדעת איך Store שומר, או להציג דבר למשתמש (המסך עושה את זה).
//
// כללים (claude/masad-mipuy-haataka.md):
//  - מה מועתק: רשימות ידניות בעץ (עם הסדר), צלמית/תיאור/עיצוב/"למה", קבצים (מסלול, כונן, גודל ותאריך, חתימה).
//  - לא מועתק בגרסה זו, ונספר בדוח: הפניות בין רשימות (@רשימה — מ0.4), רשימות חכמות (@חכמה — מ0.3), תיקיות חיות
//    (@תיקייה), שורות פגומות. שום דבר לא נמחק או משתנה במקור.
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

    /** מה נקרא מהמקור. */
    static final class Source {
        final String mode;
        final Tree tree;
        final Map<String, Sig.Entry> sigs;

        Source(String mode, Tree tree, Map<String, Sig.Entry> sigs) {
            this.mode = mode;
            this.tree = tree;
            this.sigs = sigs;
        }
    }

    /** הדוח: מה נעשה (או מה היה נעשה, בתצוגה מקדימה). */
    static final class Report {
        int listsNew, listsKnown;
        int filesNew, filesKnown, filesUpdated, filesWithSig;
        int linksAdded, linksKnown, rootFiles;
        int propsSet;
        int badLines, refs, smart, live;

        boolean nothingToDo() {
            return listsNew == 0 && filesNew == 0 && filesUpdated == 0 && linksAdded == 0 && propsSet == 0;
        }

        boolean skippedAny() {
            return badLines + refs + smart + live > 0;
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
        return new Source(modeDir.getName(), tree, Sig.read(new File(modeDir, Sig.FILE)));
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

    private static final class Ctx {
        final Store s;
        final Source src;
        final Report r;
        /** מסלול ← פריט, לריצה הזו (קובץ שבכמה רשימות — פריט אחד). */
        final Map<String, Long> files = new HashMap<String, Long>();

        Ctx(Store s, Source src, Report r) {
            this.s = s;
            this.src = src;
            this.r = r;
        }

        /** השורש: אין לו רשימה — הקבצים שבו נשארים בלי רשימה, והרשימות שלו עומדות לעצמן. */
        void top(Tree.Node root) {
            count(root);
            for (Tree.Node c : root.children) list(c, 0);
            for (Tree.Entry e : root.entries) {
                if (e.bad) {
                    r.badLines++;
                    continue;
                }
                file(e);
                r.rootFiles++;
            }
        }

        void count(Tree.Node n) {
            r.refs += n.refs.size();
            r.smart += n.smart.size();
            r.live += n.live.size();
        }

        void list(Tree.Node n, long parent) {
            count(n);
            String key = originOf(src.mode, Tree.fullLabel(n.path()));
            long id = s.findByOrigin(key);
            if (id == 0) {
                id = s.createList(n.name, parent);
                s.setOrigin(id, key);
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
            for (Object o : order(n)) {
                if (o instanceof Tree.Node) {
                    list((Tree.Node) o, id);
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

        void prop(long id, String k, String v, boolean isNew) {
            if (v == null || v.isEmpty()) return;
            if (!isNew && s.prop(id, k) != null) return;
            s.setProp(id, k, v);
            if (!isNew) r.propsSet++;
        }

        /** פריט הקובץ של הרשומה: קיים — מתעדכן אם החתימה במקור שונה; חדש — נוצר. */
        long file(Tree.Entry e) {
            Long seen = files.get(e.path);
            if (seen != null) return seen;
            Sig.Entry g = src.sigs.get(e.path);
            long size = g != null ? g.size : e.size;
            long mtime = g != null ? g.mtime : 0;
            String sig = g != null ? g.sig : null;
            long id = s.findFile(e.path);
            if (id == 0) {
                id = s.createFile(new Store.FileInfo(e.path, driveOf(e.path), size, mtime, sig));
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
            files.put(e.path, id);
            return id;
        }

        /**
         * סדר הפריטים ברשימה: אם יש סדר חופשי (@סדר) — לפיו (רשימה: "c"+שם, קובץ: "f"+מסלול; מפתחות של סוגים שלא
         * מועתקים מדולגים); מה שלא נזכר בו — אחריו, בסדר הרגיל: תת־רשימות ואז קבצים.
         */
        List<Object> order(Tree.Node n) {
            List<Object> out = new ArrayList<Object>();
            Set<Object> used = new HashSet<Object>();
            for (String k : n.order) {
                if (k.length() < 2) continue;
                String v = k.substring(1);
                Object o = null;
                if (k.charAt(0) == 'c') o = n.child(v);
                else if (k.charAt(0) == 'f') for (Tree.Entry e : n.entries) if (e.path.equals(v)) { o = e; break; }
                if (o != null && used.add(o)) out.add(o);
            }
            for (Tree.Node c : n.children) if (used.add(c)) out.add(c);
            for (Tree.Entry e : n.entries) if (used.add(e)) out.add(e);
            return out;
        }
    }
}
