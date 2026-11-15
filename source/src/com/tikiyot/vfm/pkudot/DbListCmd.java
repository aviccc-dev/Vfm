package com.tikiyot.vfm;

// מ0.12: פקודות הרשימות על המסד (MD, RD/DEL, COPY, COMP — אותן מילות סוג כמו ListCmd: ידנית · חכמה · תוכן ידנית ·
// קבצי ידנית · קבצי חכמה · תיקייה; "*" — הבית), כשעובדים במסד (DbVerbs.active). אותו נוסח בקובץ הפקודות — רק
// האחסון אחר (המשתמש: "הפקודות פונות לרשימה בשמה ובמקומה במבנה — לא לאחסון שלה").
// ההדמיה: אותה פעולה בדיוק, על עותק של המסד בזיכרון (copyOf) — לא נוגע במסד האמיתי ולא משאיר כלום; הטקסט של ההדמיה
// הוא התוצאה שתהיה. בהרצה: המסד נפתח לרגע, ולפני פעולה שמוציאה — גיבוי "לפני פעולה".
// כללים שאין להם מקבילה בישן (במסד אין "הורה"): רשימה שבתוך רשימה אחת בלבד — "שלה": בהעתקה מועתקת,
// במחיקה נמחקת איתה; רשימה שגם במקום אחר — בהעתקה נכנסת כמו שהיא (אותה רשימה), במחיקה רק יוצאת מכאן.
// רשימה חכמה בהעתקה — עותק עצמאי של ההגדרה (כמו בישן). מחיקה של רשימה שאינה קיימת — בלי שינוי (לא שגיאה), כדי
// שקובץ בדיקה יוכל להתחיל בניקוי.
// אחראי: הלוגיקה (על Store — נבדק במחשב על שני המימושים) והחיבור לתכנון של Batch.

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class DbListCmd {

    private DbListCmd() { }

    /** הקבצים שתנאי מוצא (מהאינדקס); null — האינדקס עוד לא מוכן. */
    interface Finder {
        List<String> find(String cond);
    }

    /** תוצאה של פעולה: טקסט, או שגיאה; noop — בלי שינוי. */
    static final class Res {
        String text, error;
        boolean noop;
        /** מ0.42 (ב8): רשימה או חכמה ששינתה שם — רק בהרצה אמיתית (realRes) קובצי הפקודות שפונים אליה מתעדכנים. */
        long renamed;
        String renamedFrom, renamedTo;
        /** מבנה 91: המספר של מה שנוצר (רשימה חדשה מהמסך). */
        long made;
        /** מבנה 104: נדרס מה שהיה (רשימה חכמה בשם קיים) — בהרצה: גיבוי לפני. */
        boolean replaced;
        /** מבנה 116: הפריטים (מספרים) שהפעולה הכניסה ליעד — כדי שהלוח של המסך ידע מה עבר. */
        List<Long> entered;

        Res renamed(long id, String from, String to) {
            if (id != 0 && from != null && to != null && !from.equals(to)) {
                renamed = id;
                renamedFrom = from;
                renamedTo = to;
            }
            return this;
        }

        static Res err(String e) {
            Res r = new Res();
            r.error = e;
            return r;
        }

        static Res ok(String t) {
            Res r = new Res();
            r.text = t;
            return r;
        }

        static Res same(String t) {
            Res r = ok(t);
            r.noop = true;
            return r;
        }
    }

    /** פעולה אחת על מסד (הדמיה או אמיתי). */
    interface Work {
        Res run(Store s, Finder f);
    }

    private static final String[] ALL_PROPS = {Store.P_ICON, Store.P_DESC, Store.P_DESIGN, Store.P_WHY, Store.P_COND,
            Store.P_SORT, Store.P_DIR, Store.P_VIEW, Store.P_GROUP, Store.P_HOME};

    // ---------- מציאה ----------

    /** הרשימה הידנית של הצד: "*" — הבית; אחרת לפי המסלול (DbPaths). 0 — אין. */
    static long list(Store s, String mode, ListCmd.Side x) {
        if (x.home) return DbPaths.home(s);
        return DbPaths.find(s, mode, x.path);
    }

    /** מבנה 107: למה הצד אינו מוביל לרשימה — "אין", או "יש כמה… לכתוב את המקום". */
    static String listWhy(Store s, String mode, ListCmd.Side x) {
        if (x.home) return Words.DbCmd.NO_HOME;
        DbPaths.Found f = DbPaths.listAt(s, mode, x.path);
        return f.ambiguous ? f.error : Words.ListCmdW.noList(x.raw);
    }

    /** רשימה חכמה: {הרשימה שבה (0 — עומדת לעצמה), החכמה}, או null. בלי דרך — בבית, ואם אין שם — לפי השם בכל המסד. */
    static long[] smart(Store s, String mode, ListCmd.Side x) {
        DbPaths.Found f = DbPaths.smartAt(s, mode, x.path);                       // מבנה 106: הכלל האחד — דרך, או שם בלי ספק
        return f.error != null ? null : new long[]{f.in, f.id};
    }

    /** מבנה 106: למה הצד אינו מוביל לרשימה חכמה — "אין", או "יש כמה… לכתוב את המקום". */
    static String smartWhy(Store s, String mode, ListCmd.Side x) {
        String e = DbPaths.smartAt(s, mode, x.path).error;
        return e != null ? e : Words.ListCmdW.noSmart(x.raw);
    }

    /** הקבצים שהצד מצביע עליהם: ידנית — בכל העומק (כל רשימה פעם אחת); חכמה — מה שהיא מוצאת עכשיו. ממוינים, בלי כפולים. */
    static List<String> files(Store s, String mode, ListCmd.Side x, Finder f, String[] err) {
        Set<String> out = new LinkedHashSet<String>();
        if (x.kind == ListCmd.SMART_LIST) {
            long[] r = smart(s, mode, x);
            if (r == null) {
                err[0] = smartWhy(s, mode, x);
                return null;
            }
            List<String> found = f == null ? null : f.find(s.prop(r[1], Store.P_COND));
            if (found == null) {
                err[0] = Words.ListCmdW.INDEX_NOT_READY;
                return null;
            }
            out.addAll(found);
        } else {
            if (x.home) {
                err[0] = Words.ListCmdW.HOME_FILES;
                return null;
            }
            long id = list(s, mode, x);
            if (id == 0) {
                err[0] = listWhy(s, mode, x);
                return null;
            }
            if (x.mask != null) {
                for (Store.Member m : s.members(id)) {
                    String p = pathOf(s, m.item);
                    if (p != null && Parse.wild(x.mask, new File(p).getName())) out.add(p);
                }
            } else if (!deep(s, id, f, out, new HashSet<Long>())) {
                err[0] = Words.ListCmdW.INDEX_NOT_READY;
                return null;
            }
        }
        List<String> l = new ArrayList<String>(out);
        Collections.sort(l);
        return l;
    }

    private static boolean deep(Store s, long list, Finder f, Set<String> out, Set<Long> done) {
        if (!done.add(list)) return true;
        for (Store.Member m : s.members(list)) {
            Store.Item it = m.item;
            if (it.type == Store.LIST) {
                if (!deep(s, it.id, f, out, done)) return false;
            } else if (it.type == Store.SMART) {
                String c = s.prop(it.id, Store.P_COND);
                if (c == null || c.trim().isEmpty()) continue;
                List<String> found = f == null ? null : f.find(c);
                if (found == null) return false;
                out.addAll(found);
            } else {
                String p = pathOf(s, it);
                if (p != null) out.add(p);
            }
        }
        return true;
    }

    private static String pathOf(Store s, Store.Item it) {
        if (it.type != Store.FILE) return null;
        Store.FileInfo fi = s.file(it.id);
        return fi == null ? null : fi.path;
    }

    // ---------- הפעולות ----------

    /** MD ידנית "א\ב": קיימת — בלי שינוי; חסרה — נוצרת בכל הדרך (חלק ראשון — בבית). */
    /** מבנה 102: "צור תיקייה ידנית" — הכלל האחד של רשימה חדשה (Lists.newListAt): שם קיים — סירוב (עד כאן: "בלי שינוי"). */
    static Res md(Store s, String mode, ListCmd.Side a) {
        return md(s, mode, a, Lists.TAKEN_STOP);
    }

    /** מבנה 109: taken — `/תפוס:לדלג` (קיימת: ממשיכים), `/תפוס:לשנות-שם` ("(2)"); בלי — סירוב. */
    static Res md(Store s, String mode, ListCmd.Side a, int taken) {
        Lists.Made m = Lists.newListAt(s, mode, a.path, taken);
        return listMade(m, a.label());
    }

    /** מבנה 109: ההודעה לפי מה שקרה ברשימה חדשה. */
    static Res listMade(Lists.Made m, String label) {
        if (m.error != null) return Res.err(m.error);
        if (m.skipped) return Res.same(Words.ListCmdW.listSkipped(label));
        Res ok = Res.ok(Words.ListCmdW.created(m.renamed ? m.name : label));
        ok.made = m.id;
        return ok;
    }

    /** מבנה 109: "/תפוס:<ערך>" מתוך המתגים של השורה. */
    static int takenOf(List<String> sw) {
        for (String x : sw) if (x.startsWith(Syntax.SW_TAKEN)) return taken(x.substring(Syntax.SW_TAKEN.length()));
        return Lists.TAKEN_STOP;
    }

    /** RD / DEL על רשימה: רשימה, חכמה, תוכן, או קבצים לפי תבנית — יוצאים מהרשימות (הקבצים בכונן לא נוגעים). */
    static Res remove(Store s, final String mode, final ListCmd.Side a) {
        if (a.kind == ListCmd.LOOSE) {                                        // מ0.41: רישומי קבצים שאינם באף רשימה — מהמסד בלבד
            final int[] n = {0};
            s.atomic(new Store.Work() {
                @Override public void run(Store t) {
                    for (Store.Item it : t.unassigned()) if (it.type == Store.FILE) {
                        t.purge(it.id);
                        n[0]++;
                    }
                }
            }, true);
            return n[0] == 0 ? Res.same(Words.DbCmd.LOOSE_NONE) : Res.ok(Words.DbCmd.looseRemoved(n[0]));
        }
        if (a.kind == ListCmd.SMART_LIST) {
            final long[] r = smart(s, mode, a);
            if (r == null) return Res.err(smartWhy(s, mode, a));
            s.atomic(new Store.Work() {
                @Override public void run(Store t) {
                    if (r[0] != 0) t.removeMember(r[0], r[1]);
                    Lists.dropIfAlone(t, r[1]);
                }
            }, true);
            return Res.ok(Words.DbCmd.removed(Words.ListCmdW.smartList(a.label())));
        }
        if (a.home && a.mask == null && !a.content) return Res.err(Words.ListCmdW.HOME_NO_REMOVE);
        final long id = list(s, mode, a);
        if (id == 0) return Res.same(Words.DbCmd.notThere(a.label()));
        final int[] n = {0};
        if (a.mask != null || a.content) {
            s.atomic(new Store.Work() {
                @Override public void run(Store t) {
                    for (Store.Member m : t.members(id)) {
                        if (a.mask != null) {
                            String p = pathOf(t, m.item);
                            if (p == null || !Parse.wild(a.mask, new File(p).getName())) continue;
                        }
                        n[0]++;
                        boolean idea = m.item.type == Store.NOTE || m.item.type == Store.TITLE;
                        if (idea && t.listsOf(m.item.id).size() == 1) {
                            t.trash(m.item.id);                              // רעיון שהיה רק כאן — לסל (ויוצא מהרשימה)
                            continue;
                        }
                        t.removeMember(id, m.item.id);
                        if (m.item.type == Store.LIST || m.item.type == Store.SMART) Lists.dropIfAlone(t, m.item.id);
                    }
                }
            }, true);
            String what = a.content ? Words.ListCmdW.contentOf(n[0], a.label()) : Words.files(n[0]);
            return Res.ok(Words.DbCmd.removed(what));
        }
        long parent = parentOf(s, mode, a, id);
        if (parent == -1) return Res.err(Words.DbCmd.needFullWay(a.label()));
        Lists.removeFrom(s, parent, id);
        return Res.ok(Words.DbCmd.removed(Words.ListCmdW.list(a.label())));
    }

    /**
     * מ0.30 (ממצא 40): הרשימה שממנה מוציאים את id. נכתבה הדרך — לפיה. שם לבד: הבית, אם היא בו; אחרת הרשימה היחידה
     * שהיא בתוכה; עומדת לעצמה — 0; בכמה רשימות — ‎-1 (צריך לכתוב את הדרך המלאה).
     */
    static long parentOf(Store s, String mode, ListCmd.Side a, long id) {
        List<String> up = new ArrayList<String>(a.path.subList(0, a.path.size() - 1));
        if (!up.isEmpty()) return DbPaths.find(s, mode, up);
        long home = DbPaths.home(s);
        List<Long> in = s.listsOf(id);
        if (in.contains(home)) return home;
        return in.isEmpty() ? 0 : in.size() == 1 ? in.get(0) : -1;
    }

    /** מבנה 114: הכלל האחד של "קבצים מהכונן אל רשימה" — לשורה הכתובה ולמסך (הבורר, ➕, קובץ פקודות לרשימה). */
    private static Res addFiles(Store s, long dest, List<String> ps, String what, String label) {
        DbFiles.Added r = DbFiles.add(s, dest, ps, null, !(s instanceof MemStore));   // מ0.15: חתימה — בהרצה, לא בהדמיה
        return Res.ok(what + " — " + Words.ListCmdW.copied(r.added, ps.size() - r.added, ps.size() > r.added ? Words.DbCmd.ALREADY : null, label) + nowIn(s, dest));
    }

    /**
     * מבנה 115: הכלל האחד של "תיקייה מהכונן אל רשימה" — לשורה הכתובה ולמסך (➕, "לרשימה", הדבקה). שלוש צורות, כמו במסך:
     * בלי מתג — התיקייה עצמה: רשימה בשמה, ובתוכה התת־תיקיות כרשימות והקבצים (DbMore.attach); /קבצים — רק הקבצים, ישר לרשימה;
     * /חיה — רשימה חכמה של התיקייה (DbMore.liveFolder; אותה תיקייה כבר שם — לא נכפלת). **שם קיים** (רשימה, או רשימה חכמה אחרת,
     * בשם של התיקייה) — כמו בכל מקום (ה1, ה2): עוצרים, אלא אם /תפוס:לשנות-שם ("(2)") או /תפוס:לדלג. קודם בודקים הכול, ורק אז כותבים.
     * nodes — התיקיות שנסרקו (לא ב־/חיה), באותו סדר של dirs. files — קבצים שנבחרו לצד התיקיות (מהמסך).
     */
    static Res addFolders(Store s, long dest, String label, List<String> dirs, List<Tree.Node> nodes, List<String> files,
                          boolean live, boolean flat, int taken) {
        boolean[] skip = new boolean[dirs.size()];
        for (int i = 0; i < dirs.size() && !flat; i++) {
            String name = Names.cleanName(new File(dirs.get(i)).getName());
            String why = live ? (DbMore.liveThere(s, dest, dirs.get(i)) || !Lists.smartTaken(s, dest, name) ? null : Words.ListCmdW.smartTaken(name))
                    : Lists.nameError(s, java.util.Collections.singletonList(dest), 0, Store.LIST, name);
            if (why == null || taken == Lists.TAKEN_RENAME) continue;
            if (taken != Lists.TAKEN_SKIP) return Res.err(why);
            skip[i] = true;
        }
        boolean sign = !(s instanceof MemStore);
        int lists = 0, items = 0, smarts = 0, skipped = 0;
        for (int i = 0; i < dirs.size(); i++) {
            if (skip[i]) { skipped++; continue; }
            if (live) {
                if (DbMore.liveFolder(s, dest, dirs.get(i)) != 0) smarts++;
                else skipped++;
                continue;
            }
            int[] r = DbMore.attach(s, dest, nodes.get(i), !flat, sign);
            lists += r[0];
            items += r[1];
        }
        if (files != null && !files.isEmpty()) items += DbFiles.add(s, dest, files, null, sign).added;
        Res ok = Res.ok(Words.ListCmdW.foldersAdded(label, lists, items, smarts, skipped) + nowIn(s, dest));
        ok.made = dest;
        return ok;
    }

    /** COPY: לרשימה ידנית (או לבית). fsFiles — קבצים מהכונן (כשהמקור אינו רשימה); הקבצים עצמם לתיקייה — ב-plan. */
    static Res copy(Store s, String mode, ListCmd.Side a, ListCmd.Side b, final List<String> fsFiles, Finder f) {
        final long dest = b.home ? DbPaths.home(s) : b.mask != null ? 0 : DbPaths.find(s, mode, b.path);
        if (dest == 0) return Res.err(listWhy(s, mode, b));
        if (!a.list() || a.filesOf) {                                        // מסלולים ← לרשימה
            List<String> paths = fsFiles;
            String what = Words.files(fsFiles == null ? 0 : fsFiles.size());
            if (a.filesOf) {
                String[] err = {null};
                paths = files(s, mode, a, f, err);
                if (paths == null) return Res.err(err[0]);
                what = Words.ListCmdW.filesOf(Words.files(paths.size()), a.label());
            }
            return addFiles(s, dest, paths, what, b.label());
        }
        final List<Store.Item> what = new ArrayList<Store.Item>();
        final List<String> self = new ArrayList<String>();
        String label;
        if (a.kind == ListCmd.SMART_LIST) {
            long[] r = smart(s, mode, a);
            if (r == null) return Res.err(smartWhy(s, mode, a));
            what.add(s.item(r[1]));
            label = Words.ListCmdW.smartList(a.label());
        } else {
            long src = list(s, mode, a);
            if (src == 0) return Res.err(listWhy(s, mode, a));
            if (a.home || a.content || a.mask != null) {
                for (Store.Member m : s.members(src)) {
                    if (a.mask != null) {
                        String p = pathOf(s, m.item);
                        if (p == null || !Parse.wild(a.mask, new File(p).getName())) continue;
                    }
                    if (m.item.type == Store.LIST && (m.item.id == dest || s.contains(m.item.id, dest))) {
                        self.add(m.item.name);                               // היעד עצמו (או מה שהוא בתוכו) — לא לתוך עצמו
                        continue;
                    }
                    what.add(m.item);
                }
                label = a.mask != null ? Words.files(what.size()) : a.home ? Words.DbCmd.homeContent(what.size())
                        : Words.ListCmdW.contentOf(what.size(), a.label());
            } else {
                if (src == dest || s.contains(src, dest)) return Res.err(Words.ListCmdW.INTO_ITSELF);
                what.add(s.item(src));
                label = Words.ListCmdW.list(a.label());
            }
        }
        Lists.Copied c = Lists.copyInto(s, what, dest);
        String selfText = self.isEmpty() ? "" : Words.ListCmdW.notIntoSelf(self);
        return Res.ok(Words.ListCmdW.copy(label, b.label()) + names(what) + selfText + " · "
                + Words.ListCmdW.copied(c.done, c.skipped, c.why, b.label()) + nowIn(s, dest));
    }

    private static String nowIn(Store s, long list) {
        DbCount c = DbCount.of(s, list, null);
        return Words.ListCmdW.nowIn(c.lists, c.items);
    }

    private static String names(List<Store.Item> items) {
        if (items.isEmpty()) return "";
        StringBuilder sb = new StringBuilder(" — ");
        for (int i = 0; i < Math.min(5, items.size()); i++) sb.append(i > 0 ? ", " : "").append('"').append(items.get(i).name).append('"');
        if (items.size() > 5) sb.append(" ").append(Words.andMore(items.size() - 5));
        return sb.toString();
    }


    /** מ0.21: REN ידנית/חכמה "א\ב" "שם חדש" — שם תפוס באותה רשימה (אותו סוג) — לא; "/" או "\" בשם — לא. */
    static Res rename(Store s, String mode, final ListCmd.Side a, String name) {
        if (name.contains("/") || name.contains("\\")) return Res.err(Words.DbCmd.BAD_NAME);   // לפני הניקוי (שמוחק אותם)
        final String n = a.kind == ListCmd.SMART_LIST ? name.trim() : Names.cleanName(name);
        if (n.isEmpty()) return Res.err(Words.DbCmd.BAD_NAME);
        long id, parent;
        if (a.kind == ListCmd.SMART_LIST) {
            long[] r = smart(s, mode, a);
            if (r == null) return Res.err(smartWhy(s, mode, a));
            id = r[1];
            parent = r[0];
        } else {
            id = DbPaths.find(s, mode, a.path);
            if (id == 0) return Res.err(listWhy(s, mode, a));
            List<String> up = new ArrayList<String>(a.path.subList(0, a.path.size() - 1));
            parent = up.isEmpty() ? DbPaths.home(s) : DbPaths.find(s, mode, up);
        }
        Store.Item it = s.item(id);
        if (n.equals(it.name)) return Res.same(Words.DbCmd.renamed(a.label(), n));
        // מבנה 86: השם נבדק בכל הרשימות שהפריט נמצא בהן — כמו בעריכה שבמסך (Lists.edit). עד כאן נבדקה רק הרשימה שבדרך שנכתבה,
        // ורשימה שמשותפת לשתי רשימות יכלה לקבל שם שכבר תפוס בשנייה: שתי רשימות באותו שם באותה רשימה, ודרך שאינה חד־משמעית.
        List<Long> parents = new ArrayList<Long>(s.listsOf(id));
        if (!parents.contains(parent)) parents.add(parent);
        String e = Lists.nameError(s, parents, id, it.type, name);              // מ0.23: הבדיקה האחת
        if (e != null) return Res.err(e);
        final long fid = id;
        s.atomic(new Store.Work() {
            @Override public void run(Store t) { t.rename(fid, n); }
        }, true);
        return Res.ok(Words.DbCmd.renamed(a.label(), n)).renamed(fid, it.name, n);
    }

    /**
     * מ0.17: LINK (קשר) — אותו פריט גם ביעד (רשימה משותפת); MOVE (העבר) — נכנס ליעד ויוצא מהמקום שלו. המקור: רשימה
     * (מהרשימה שמעליה, או מהבית), חכמה, תוכן של רשימה, או קבצים לפי תבנית — כמו ב-COPY.
     */
    static Res link(Store s, String mode, ListCmd.Side a, ListCmd.Side b, boolean move) {
        long dest = b.home ? DbPaths.home(s) : b.mask != null ? 0 : DbPaths.find(s, mode, b.path);
        if (dest == 0) return Res.err(listWhy(s, mode, b));
        List<Long> ids = new ArrayList<Long>();
        long from;
        String label;
        if (a.kind == ListCmd.SMART_LIST) {
            long[] r = smart(s, mode, a);
            if (r == null) return Res.err(smartWhy(s, mode, a));
            ids.add(r[1]);
            from = r[0];
            label = Words.ListCmdW.smartList(a.label());
        } else {
            long src = list(s, mode, a);
            if (src == 0) return Res.err(listWhy(s, mode, a));
            if (a.home || a.content || a.mask != null) {
                for (Store.Member m : s.members(src)) {
                    if (a.mask != null) {
                        String p = pathOf(s, m.item);
                        if (p == null || !Parse.wild(a.mask, new File(p).getName())) continue;
                    }
                    ids.add(m.item.id);
                }
                from = src;
                label = a.mask != null ? Words.files(ids.size()) : a.home ? Words.DbCmd.homeContent(ids.size())
                        : Words.ListCmdW.contentOf(ids.size(), a.label());
            } else {
                if (src == dest || s.contains(src, dest)) return Res.err(Words.ListCmdW.INTO_ITSELF);
                from = parentOf(s, mode, a, src);
                if (from == -1) return Res.err(Words.DbCmd.needFullWay(a.label()));
                ids.add(src);
                label = Words.ListCmdW.list(a.label());
            }
        }
        Lists.Copied c = move ? Lists.moveTo(s, ids, from, dest) : Lists.linkInto(s, ids, dest);
        return Res.ok((move ? Words.DbCmd.moved(label, b.label()) : Words.DbCmd.linked(label, b.label())) + " · "
                + Words.ListCmdW.copied(c.done, c.skipped, c.why, b.label()) + nowIn(s, dest));
    }

    /** מ0.18: היצוא — write=false (ההדמיה): רק השם והמספרים; write=true: נכתב. */
    static Res export(Store s, String mode, ListCmd.Side a, ListCmd.Side b, Finder f, boolean write) {
        long when = System.currentTimeMillis();
        DbExport.Out o;
        String name;
        if (a.filesOf) {
            String[] err = {null};
            List<String> ps = files(s, mode, a, f, err);
            if (ps == null) return Res.err(err[0]);
            name = Words.DbCmd.exportFilesName(a.raw);
            o = DbExport.paths(ps, name, when);
        } else {
            long id = list(s, mode, a);
            if (id == 0) return Res.err(listWhy(s, mode, a));
            name = a.home ? Words.DbCmd.exportModeName(mode) : Words.DbCmd.exportListName(s.item(id).name);
            o = DbExport.list(s, id, a.home, name, when);
        }
        File to = DbExport.target(b.raw, name, exportDir());
        if (write) {
            try {
                File dir = to.getParentFile();
                if (dir != null && !dir.isDirectory() && !dir.mkdirs()) return Res.err(Words.CANT_CREATE_FOLDER);
                Files.writeAtomic(to, o.text);
            } catch (Throwable t) {
                Log.e("DbListCmd.export", t);
                return Res.err(Words.Db.failed(String.valueOf(t.getMessage())));
            }
        }
        return Res.ok(Words.DbCmd.exported(name, Bidi.path(to.getAbsolutePath()), o.lists, o.files, o.skipped));
    }

    /** מ0.22: קובץ הייבוא — שם בלי מסלול: בתיקיית היצוא; "\\" כמו "/". */
    static File importFile(String raw) {
        String r = raw == null ? "" : raw.trim().replace('\\', '/');
        File f = new File(r);
        return f.isAbsolute() ? f : new File(exportDir(), r);
    }

    /** מ0.22: ייבוא — הטקסט (בפורמט היצוא) לתוך הרשימה. */
    static Res importFrom(Store s, String mode, File in, ListCmd.Side b) {
        long dest = b.home ? DbPaths.home(s) : DbPaths.find(s, mode, b.path);
        if (dest == 0) return Res.err(listWhy(s, mode, b));
        String text;
        try {
            text = Files.readUtf8(in);
        } catch (Throwable t) {
            return Res.err(Words.DbCmd.cantRead(in.getName()));
        }
        DbMore.Imported r = DbMore.importText(s, dest, text);
        return Res.ok(Words.DbCmd.imported(in.getName(), b.label(), r.lists, r.files, r.bad) + nowIn(s, dest));
    }

    /** מ0.22: העתקה מאופן אחר — המקור נפתח לקריאה בלבד; עותק עצמאי (קבצים — לפי המסלול). */
    static Res copyFromMode(Store t, String mode, String srcMode, ListCmd.Side a, ListCmd.Side b) {
        long dest = b.home ? DbPaths.home(t) : DbPaths.find(t, mode, b.path);
        if (dest == 0) return Res.err(listWhy(t, mode, b));
        SqlStore src = null;
        try {
            src = Db.openRead(Db.app, srcMode);
            return copyAcross(src, srcMode, a, t, dest, b.label());
        } finally {
            if (src != null) src.close();
        }
    }

    /** מ0.22: הליבה של ההעתקה בין מסדים (נבדקת במחשב על שני מסדים). */
    static Res copyAcross(Store src, String srcMode, ListCmd.Side a, Store t, long dest, String destLabel) {
        DbPaths.Found at = DbPaths.listAt(src, srcMode, a.path);                // מבנה 107: גם באופן אחר — ספק עוצר
        long id = at.error != null ? 0 : at.id;
        if (id == 0) return Res.err(at.ambiguous ? at.error : Words.ListCmdW.noList(Words.DbCmd.MODE_PREFIX + srcMode + "/" + ListCmd.pathText(a.path)));
        int n = 0;
        if (a.content) for (Store.Member m : src.members(id)) n += DbMore.copyAcross(src, m.item.id, t, dest);
        else {
            for (Store.Member m : t.members(dest))
                if (m.item.type == Store.LIST && m.item.name.equals(src.item(id).name)) return Res.err(Words.DbCmd.SAME_NAME);
            n = DbMore.copyAcross(src, id, t, dest);
        }
        return Res.ok(Words.DbCmd.fromMode(srcMode, ListCmd.pathText(a.path), destLabel, n) + nowIn(t, dest));
    }

    /** התיקייה "יצוא" בתיקיית התוכנה. */
    static File exportDir() {
        return new File(Paths.data(), Words.DbCmd.EXPORT_DIR);
    }

    /** COMP: לפי המסלולים; /תנאים — גם התנאים של שתי חכמות. */
    static Res comp(Store s, String mode, ListCmd.Side a, ListCmd.Side b, boolean conds, Finder f) {
        String[] err = {null};
        List<String> fa = files(s, mode, a, f, err), fb = fa == null ? null : files(s, mode, b, f, err);
        if (fa == null || fb == null) return Res.err(err[0]);
        Set<String> sa = new LinkedHashSet<String>(fa), sb = new LinkedHashSet<String>(fb);
        List<String> onlyA = new ArrayList<String>(), onlyB = new ArrayList<String>();
        int both = 0;
        for (String x : sa) if (sb.contains(x)) both++; else onlyA.add(x);
        for (String x : sb) if (!sa.contains(x)) onlyB.add(x);
        String text = Words.ListCmdW.compared(a.label(), onlyA.size(), b.label(), onlyB.size(), both) + examples(onlyA) + examples(onlyB);
        if (conds) {
            long[] ra = smart(s, mode, a), rb = smart(s, mode, b);
            text += "\n" + ListCmd.condsDiff(ra == null ? "" : s.prop(ra[1], Store.P_COND), rb == null ? "" : s.prop(rb[1], Store.P_COND),
                    a.label(), b.label());
        }
        return Res.same(text);
    }

    private static String examples(List<String> paths) {
        if (paths.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("\n   ");
        for (int i = 0; i < Math.min(3, paths.size()); i++) sb.append(i > 0 ? " · " : "").append(new File(paths.get(i)).getName());
        if (paths.size() > 3) sb.append(" · ").append(Words.andMore(paths.size() - 3));
        return sb.toString();
    }

    // ---------- עותק בזיכרון להדמיה ----------

    /** עותק של המסד בזיכרון (רשימות, חכמות, קבצים, רעיונות וכותרות, הסדר, המאפיינים והמקורות). לא נשמר. */
    static MemStore copyOf(Store src) {
        final MemStore m = new MemStore(IdCounter.memory());
        final Map<Long, Long> id = new HashMap<Long, Long>();
        List<Store.Item> lists = src.lists();                                // מ0.38: פעם אחת — המסד יכול להשתנות בזמן ההעתקה
        for (Store.Item it : lists) id.put(it.id, m.createList(it.name, 0));
        for (Store.Item it : src.smarts()) id.put(it.id, m.createSmart(it.name, 0));
        for (Map.Entry<Long, Long> e : id.entrySet()) {
            for (String k : ALL_PROPS) {
                String v = src.prop(e.getKey(), k);
                if (v != null) m.setProp(e.getValue(), k, v);
            }
            String o = src.origin(e.getKey());
            if (o != null && !o.isEmpty()) m.setOrigin(e.getValue(), o);
        }
        for (Store.Item it : lists) {
            List<Store.Member> ms;
            try {
                ms = src.members(it.id);
            } catch (Store.Failure f) {                                        // מ0.38 (דוח האבחון): נמחקה תוך כדי — מדלגים
                continue;
            }
            for (Store.Member mb : ms) {
                long into = id.get(it.id);
                Store.Item x = mb.item;
                if (x.type == Store.NOTE) m.addNote(into, x.name);
                else if (x.type == Store.TITLE) m.addTitle(into, x.name);
                else {
                    Long to = id.get(x.id);
                    if (to == null && x.type == Store.FILE) {
                        long fid = fileOf(src, m, x.id);
                        if (fid != 0) id.put(x.id, to = fid);
                    }
                    if (to != null) m.addMember(into, to);
                }
            }
        }
        for (Store.Item it : src.unassigned()) if (it.type == Store.FILE && !id.containsKey(it.id)) fileOf(src, m, it.id);
        return m;
    }

    private static long fileOf(Store src, MemStore m, long item) {
        Store.FileInfo fi;
        try {
            fi = src.file(item);
        } catch (Store.Failure f) {
            return 0;                                                         // מ0.38: נמחק תוך כדי ההעתקה
        }
        if (fi == null) return 0;
        long had = m.findFile(fi.path);
        return had != 0 ? had : m.createFile(fi);
    }

    // ---------- החיבור לתכנון של Batch ----------

    /** האופן שהפקודות עובדות עליו עכשיו (הבית של המסד שפתוח). */
    static String mode() {
        return Db.currentMode;
    }

    /** מ0.15: האופן של הקובץ — מ"אופן" שבו, אחרת זה שפתוח. */
    static String mode(Sim sim) {
        return sim != null && sim.mode != null ? sim.mode : mode();
    }

    /** שורה עם מילות סוג, כשעובדים במסד: אותן בדיקות צורה כמו ListCmd.plan; הפעולה — על העותק בזיכרון, ובהרצה — על המסד. */
    static void plan(final Batch.Step s, final String cmd, ListCmd.Parsed p, List<String> sw, final Sim sim,
                     final List<String> fsFiles, final File fsDest, final List<String> fsDirs) {
        boolean disk = false;                                                  // מ0.40: יש צד בכונן — שם /מכיל: חל
        for (ListCmd.Side x : p.sides) if (!x.list()) disk = true;
        for (String x : sw)
            if (x.startsWith("/" + Syntax.SW_DATE) || x.startsWith("/" + Syntax.SW_SIZE) || (!disk && x.startsWith("/" + Syntax.SW_CONTAINS))) {
                s.error = Words.ListCmdW.NO_FILTER;
                return;
            }
        for (ListCmd.Side x : p.sides)                                        // מ0.41: "לא-משויכים" — רק "בטל לא-משויכים" (ו"ודא")
            if (x.kind == ListCmd.LOOSE && !((cmd.equals("RD") || cmd.equals("DEL")) && p.sides.size() == 1)) {
                s.error = Words.DbCmd.LOOSE_USAGE;
                return;
            }
        if (sim.modeError != null) { s.error = sim.modeError; return; }      // מ0.17.1: "אופן" שנכשל — לא לרוץ על אופן אחר
        for (ListCmd.Side x : p.sides)
            if (x.picked) {                                                    // מבנה 85: מה שנבחר במסך הרשימות
                planPicked(s, cmd, p, sw, sim, fsFiles, fsDirs);
                return;
            }
        final String mode = mode(sim);
        final Store db;
        try {
            db = sim(sim, mode);
        } catch (Throwable t) {
            Log.e("DbListCmd.sim", t);
            s.error = Words.Db.failed(String.valueOf(t.getMessage()));
            return;
        }
        final Finder find = finder(sim);
        final List<ListCmd.Side> sd = p.sides;
        Work work = null;
        boolean destructive = false;
        if (cmd.equals("MD")) {
            if (sd.size() != 1) { s.error = Commands.usage(cmd); return; }
            final ListCmd.Side a = sd.get(0);
            if (a.kind == ListCmd.SMART_LIST) { s.error = Words.ListCmdW.MD_SMART; return; }
            if (a.kind != ListCmd.LIST || a.content || a.filesOf || a.home || a.mask != null) { s.error = Words.ListCmdW.USAGE_MD; return; }
            s.listPath = a.path;
            final int taken = takenOf(sw);                                       // מבנה 109
            work = new Work() {
                @Override public Res run(Store t, Finder f) { return md(t, mode, a, taken); }
            };
        } else if (cmd.equals("RD") || cmd.equals("DEL")) {
            if (sd.size() != 1) { s.error = Commands.usage(cmd); return; }
            final ListCmd.Side a = sd.get(0);
            if (a.filesOf) {                                                 // הקבצים עצמם — לסל (הביצוע הרגיל)
                if (cmd.equals("RD")) { s.error = Words.ListCmdW.RD_FILES; return; }
                String[] err = {null};
                List<String> fs = files(db, mode, a, find, err);
                if (fs == null) { s.error = err[0]; return; }
                for (String x : fs) if (sim.isFile(new File(x))) s.files.add(new File(x).getAbsolutePath());
                s.describe = Words.Cmd.toTrash(Words.files(s.files.size())) + Words.ListCmdW.FROM_DISK;
                for (String x : s.files) {
                    sim.gone.add(x);
                    sim.added.remove(x);
                }
                s.target = a.label();
                return;
            }
            destructive = true;
            work = new Work() {
                @Override public Res run(Store t, Finder f) { return remove(t, mode, a); }
            };
        } else if (cmd.equals("COPY")) {
            if (sd.size() != 2) { s.error = Words.ListCmdW.USAGE; return; }
            final ListCmd.Side a = sd.get(0), b = sd.get(1);
            if (b.content || b.filesOf) { s.error = Words.ListCmdW.USAGE; return; }
            if (b.kind == ListCmd.SMART_LIST) { s.error = Words.ListCmdW.INTO_SMART; return; }
            String fromMode = a.path == null || a.path.isEmpty() || !a.path.get(0).startsWith(Words.DbCmd.MODE_PREFIX) ? null
                    : a.path.get(0).substring(Words.DbCmd.MODE_PREFIX.length()).trim();
            if (fromMode != null) {                                          // מ0.22: מאופן אחר — "אופן:X/א\ב"
                a.path = new ArrayList<String>(a.path.subList(1, a.path.size()));
                if (a.path.isEmpty()) { s.error = Words.DbCmd.USAGE_FROM_MODE; return; }
                if (!fromMode.equals(mode)) {
                    if (!Db.dbModes(Db.app).contains(fromMode)) { s.error = Words.DbCmd.noMode(fromMode, sim.modes()); return; }
                    if (a.kind != ListCmd.LIST || a.filesOf || a.mask != null || b.kind != ListCmd.LIST) { s.error = Words.DbCmd.USAGE_FROM_MODE; return; }
                    final String src = fromMode;
                    work = new Work() {
                        @Override public Res run(Store t, Finder f) { return copyFromMode(t, mode, src, a, b); }
                    };
                    s.target = b.label();
                    Res pr;
                    try { pr = work.run(db, find); } catch (Store.Failure e) { pr = Res.err(Words.Db.failed(e.getMessage())); }
                    if (pr.error != null) { s.error = pr.error; return; }
                    s.describe = Words.DbCmd.inDb(pr.text);
                    final Work w = work;
                    s.op = new ListCmd.Op() {
                        @Override public String run(Library lib) { return real(mode, w, false, lib); }
                    };
                    return;
                }
            }
            if (a.kind == ListCmd.EXPORT) {                                  // מ0.22: ייבוא מקובץ טקסט לרשימה
                final File in = importFile(a.raw);
                work = new Work() {
                    @Override public Res run(Store t, Finder f) { return importFrom(t, mode, in, b); }
                };
                s.target = b.label();
                if (in.isFile()) {
                    Res pr = work.run(db, find);
                    if (pr.error != null) { s.error = pr.error; return; }
                    s.describe = Words.DbCmd.inDb(pr.text);
                } else s.describe = Words.DbCmd.importLater(Bidi.path(in.getAbsolutePath()));   // ייווצר בשורות שלפני
                final Work w = work;
                s.op = new ListCmd.Op() {
                    @Override public String run(Library lib) { return real(mode, w, false, lib); }
                };
                return;
            }
            if (b.kind == ListCmd.EXPORT) {                                  // מ0.18: יצוא לקובץ טקסט
                if (a.kind == ListCmd.SMART_LIST || a.content || a.mask != null || !a.list()) { s.error = Words.DbCmd.USAGE_EXPORT; return; }
                work = new Work() {
                    @Override public Res run(Store t, Finder f) { return export(t, mode, a, b, f, !(t instanceof MemStore)); }
                };
                s.target = b.label();
                Res pr = work.run(db, find);
                if (pr.error != null) { s.error = pr.error; return; }
                s.describe = Words.DbCmd.inDb(pr.text);
                final Work w = work;
                s.op = new ListCmd.Op() {
                    @Override public String run(Library lib) { return real(mode, w, false, lib); }
                };
                return;
            }
            if (b.kind == ListCmd.FS) {                                      // מרשימה לתיקייה — הקבצים עצמם (הביצוע הרגיל)
                toFolder(s, cmd, a, db, mode, find, sw, sim, fsDest);
                return;
            }
            final boolean live = sw.contains(ListCmd.SW_LIVE), flat = sw.contains(ListCmd.SW_FLAT);
            if ((live || flat) && (a.list() || fsDirs.isEmpty() || (live && flat))) { s.error = Words.ListCmdW.FOLDER_SW_ONLY; return; }
            if (!a.list() && !fsDirs.isEmpty()) {                               // מבנה 115: תיקייה שלמה מהכונן אל רשימה
                final List<Tree.Node> nodes = new ArrayList<Tree.Node>();
                if (!live) for (String d : fsDirs) nodes.add(Batch.scan(sim, s, new File(d), !flat));
                final int taken = takenOf(sw);
                work = new Work() {
                    @Override public Res run(Store t, Finder f) {
                        long dest = b.home ? DbPaths.home(t) : b.mask != null ? 0 : DbPaths.find(t, mode, b.path);
                        if (dest == 0) return Res.err(listWhy(t, mode, b));
                        return addFolders(t, dest, b.label(), fsDirs, nodes, fsFiles, live, flat, taken);
                    }
                };
            } else work = new Work() {
                @Override public Res run(Store t, Finder f) { return copy(t, mode, a, b, fsFiles, f); }
            };
        } else if (cmd.equals("COMP")) {
            if (sd.size() != 2) { s.error = Words.ListCmdW.USAGE_COMP; return; }
            final ListCmd.Side a = sd.get(0), b = sd.get(1);
            if (!a.list() || !b.list()) { s.error = Words.ListCmdW.COMP_FS; return; }
            if (sw.contains(ListCmd.SW_DIFF)) {                                  // מבנה 90: כל ההבדלים בין שתי רשימות ידניות
                planDiff(s, a, b, sw, mode);
                return;
            }
            if (a.content || b.content) { s.error = Words.ListCmdW.USAGE_COMP; return; }
            final boolean conds = sw.contains(ListCmd.SW_CONDS);
            if (conds && (a.kind != ListCmd.SMART_LIST || b.kind != ListCmd.SMART_LIST || a.filesOf || b.filesOf)) {
                s.error = Words.ListCmdW.CONDS_SMART;
                return;
            }
            Res r = comp(db, mode, a, b, conds, find);
            if (r.error != null) s.error = r.error;
            else s.describe = Words.DbCmd.inDb(r.text);
            return;
        } else if (cmd.equals("REN")) {                                       // מ0.21: שינוי שם לרשימה — במסד
            if (sd.size() != 2) { s.error = Words.DbCmd.USAGE_REN; return; }
            final ListCmd.Side a = sd.get(0);
            final String name = sd.get(1).raw == null ? "" : sd.get(1).raw.trim();
            if (!a.list() || a.content || a.filesOf || a.home || a.mask != null || sd.get(1).list()) { s.error = Words.DbCmd.USAGE_REN; return; }
            work = new Work() {
                @Override public Res run(Store t, Finder f) { return rename(t, mode, a, name); }
            };
        } else if (cmd.equals("MOVE") || cmd.equals(Syntax.LINK)) {           // מ0.17: העברה וקישור — במסד
            if (sd.size() != 2) { s.error = Words.DbCmd.USAGE_LINK; return; }
            final ListCmd.Side a = sd.get(0), b = sd.get(1);
            if (cmd.equals("MOVE") && a.list() && b.kind == ListCmd.FS && !b.content && !b.filesOf) {   // מ0.40: הקבצים עצמם — לתיקייה
                toFolder(s, cmd, a, db, mode, find, sw, sim, fsDest);
                return;
            }
            if (!a.list() && cmd.equals("MOVE")) { s.error = Words.ListCmdW.MOVE_TO_LIST; return; }
            if (!a.list() || a.filesOf || b.content || b.filesOf || b.kind == ListCmd.FS) { s.error = Words.DbCmd.USAGE_LINK; return; }
            if (b.kind == ListCmd.SMART_LIST) { s.error = Words.ListCmdW.INTO_SMART; return; }
            final boolean move = cmd.equals("MOVE");
            destructive = move;
            work = new Work() {
                @Override public Res run(Store t, Finder f) { return link(t, mode, a, b, move); }
            };
        } else {
            s.error = Words.ListCmdW.notYet(cmd);
            return;
        }
        s.target = sd.get(sd.size() - 1).label();
        Res r;
        try {
            r = work.run(db, find);                                          // ההדמיה — על העותק
        } catch (Store.Failure e) {
            r = Res.err(Words.Db.failed(e.getMessage()));
        }
        if (r.error != null) { s.error = r.error; return; }
        s.describe = Words.DbCmd.inDb(r.text);
        // מ0.42 (בדיקת המכשיר דרך תוסף האבחון: "ודא לא-משויכים /פריטים:0 — בפועל: 2"): "בטל לא-משויכים" נבדק תמיד בהרצה. קובץ
        // שנמחק בכונן בשורות שלפני יוצא מהרשימות שלו במסד (DbFollow) — וההדמיה לא יודעת (ד, 37). עד כאן ההדמיה אמרה "אין", והשורה
        // לא רצה בכלל; זה הוסתר כל עוד נשארו "לא משויכים" מהרצות קודמות.
        boolean atRun = sd.size() == 1 && sd.get(0).kind == ListCmd.LOOSE;
        if (atRun && r.noop) s.describe = Words.DbCmd.LOOSE_AT_RUN;
        s.noop = r.noop && !atRun;
        if (s.noop) return;
        final Work w = work;
        // מבנה 108 (ה4; הוראות סעיף 4 — דריסה רק אחרי גיבוי): שינוי שם של רשימה — גיבוי "לפני עריכה", כמו בטופס שבמסך (עד כאן — בלי)
        final String before = destructive ? Backups.OP_DELETE : cmd.equals("REN") ? Backups.OP_EDIT : null;
        s.op = new ListCmd.Op() {
            @Override public String run(Library lib) {
                Res r = realRes(mode, w, before, lib);
                if (r.error != null) lastFailed = true;
                return r.error != null ? r.error : Words.DbCmd.inDb(r.text);
            }
        };
    }

    /**
     * מבנה 85: פעולה של מסך הרשימות על הפריטים שנבחרו בו — "בטל ידנית %נבחרים%" ("הסרה מכאן"). הבחירה היא מספרי הפריטים במסד
     * והרשימה שהם מוצגים בה (Batch.Selection), לא דרך של שמות: כך בוחרים במסך, ושני פריטים באותו שם אינם מתבלבלים.
     * הפעולה עצמה — אותה פעולה של החומר (Lists) שהמסך קרא לה עד כאן, דרך אותה עטיפה של הפקודות (real: שער המסד, גיבוי לפני).
     * **בלי הדמיה:** התכנון של פקודת רשימה כתובה מעתיק את כל המסד לזיכרון כדי שהשורות הבאות יראו את התוצאה; כאן שורה אחת,
     * והמסך כבר הציג את חלון האישור שלו — העתקה של כל המסד בכל לחיצה הייתה עבודה יקרה לחינם (המשפך).
     */
    private static void planPicked(final Batch.Step s, String cmd, ListCmd.Parsed p, List<String> sw, Sim sim, List<String> fsFiles,
                                   List<String> fsDirs) {
        final Batch.Selection sel = sim.selected;
        ListCmd.Side a = p.sides.get(0);
        boolean make = cmd.equals("MD") && p.sides.size() == 1 && sw.isEmpty() && a.kind == ListCmd.LIST && !a.content && !a.filesOf;
        final ListCmd.Side to = p.sides.size() == 2 ? p.sides.get(1) : null;
        // מבנה 114: "העתק %נבחרים% ידנית %נבחרים%" — הקבצים שנבחרו (מסלולים), אל הרשימה שנבחרה (המספר שלה; 0 — הבית)
        // מבנה 115: גם תיקיות שנבחרו — עם /חיה, /קבצים, /תפוס: (כמו בשורה הכתובה)
        final boolean live = sw.contains(ListCmd.SW_LIVE), flat = sw.contains(ListCmd.SW_FLAT);
        final boolean hasDirs = fsDirs != null && !fsDirs.isEmpty();
        boolean add = cmd.equals("COPY") && to != null && !a.list() && to.picked && to.kind == ListCmd.LIST && !to.content && !to.filesOf;
        if (add && (live || flat) && (!hasDirs || (live && flat))) { s.error = Words.ListCmdW.FOLDER_SW_ONLY; return; }
        if (sel == null || sel.ids == null || sel.mode == null
                || (make ? sel.name == null : add ? (fsFiles == null || fsFiles.isEmpty()) && !hasDirs : sel.ids.isEmpty())) {
            s.error = Words.Cmd.NOTHING_SELECTED;
            return;
        }
        final String mode = sel.mode;
        if (!Db.dbFile(Db.app, mode).isFile()) { s.error = Words.DbCmd.noMode(mode, sim.modes()); return; }
        if (make) {
            // מבנה 91: רשימה חדשה מהמסך (➕, בורר הרשימות) — בתוך הרשימה שמוצגת (לפי המספר שלה; 0 — הבית), בשם שהוקלד כלשונו.
            // שלא כמו "צור תיקייה ידנית" כתובה (דרך של שמות; רשימה קיימת — "בלי שינוי"; יוצרת גם את מה שבדרך) — כאן כמו שהמסך עשה:
            // שם אחד, והבדיקה האחת של שם (Lists.nameError): תפוס, ריק, או עם "/" — סירוב, ושום דבר לא נוצר. בלי גיבוי לפני (רק מוסיפה).
            final long into = sel.in;
            final String name = sel.name;
            s.target = a.label();
            s.describe = Words.DbCmd.inDb(Words.ListCmdW.created(Names.cleanName(name)));
            s.op = new ListCmd.Op() {
                @Override public String run(Library lib) {
                    Res r = realRes(mode, new Work() {
                        @Override public Res run(Store t, Finder f) {
                            if (into != 0 && (t.item(into) == null || t.item(into).type != Store.LIST)) return Res.err(Words.Db.LIST_GONE);
                            Lists.Made m = Lists.newListChecked(t, into, name);
                            if (m.error != null) return Res.err(m.error);
                            Res ok = Res.ok(Words.ListCmdW.created(Names.cleanName(name)));
                            ok.made = m.id;
                            return ok;
                        }
                    }, false, lib);
                    if (r.error != null) lastFailed = true;
                    s.made = r.made;
                    return r.error != null ? r.error : Words.DbCmd.inDb(r.text);
                }
            };
            return;
        }
        // מבנה 116: ההדבקה מהלוח של הרשימות — "העתק / העבר ידנית %נבחרים% ידנית %נבחרים%": הפריטים שנבחרו (מספרים), מהרשימה
        // שסומנו בה (sel.in — להעברה), אל הרשימה sel.to. אותה ליבה כמו בשורה הכתובה (Lists.copyInto / moveTo), ואותה הודעה;
        // מה שנובע מהמקום: לפי מספרים, ולכן כל פריט (גם קובץ, רעיון, רשימה חכמה) — לא רק מה שיש לו דרך של שמות.
        boolean xfer = (cmd.equals("COPY") || cmd.equals("MOVE")) && to != null && sw.isEmpty() && a.picked && a.kind == ListCmd.LIST
                && !a.content && !a.filesOf && to.picked && to.kind == ListCmd.LIST && !to.content && !to.filesOf;
        if (xfer) {
            final boolean move = cmd.equals("MOVE");
            final long from = sel.in, into = sel.to;
            final List<Long> ids = new ArrayList<Long>(sel.ids);
            s.describe = Words.DbCmd.inDb(Words.ListCmdW.copied(ids.size(), 0, null, Words.DbCmd.HERE_LIST));
            s.op = new ListCmd.Op() {
                @Override public String run(Library lib) {
                    Res r = realRes(mode, new Work() {
                        @Override public Res run(Store t, Finder f) {
                            Store.Item li = into == 0 ? null : t.item(into);
                            if (li == null || li.type != Store.LIST) return Res.err(Words.Db.LIST_GONE);
                            Lists.Copied all = new Lists.Copied();
                            List<Long> entered = new ArrayList<Long>();
                            for (Long id : ids) {                               // אחד אחד — כדי לדעת מי נכנס (הלוח)
                                Lists.Copied one;
                                if (t.item(id) == null) {
                                    one = new Lists.Copied();
                                    one.skipped = 1;
                                    one.why = Words.DbClipW.GONE;
                                } else one = move ? Lists.moveTo(t, java.util.Collections.singletonList(id), from, into)
                                        : Lists.copyIds(t, java.util.Collections.singletonList(id), into);
                                if (one.done == 1) entered.add(id);
                                all.done += one.done;
                                all.skipped += one.skipped;
                                if (all.why == null) all.why = one.why;
                            }
                            Res ok = Res.ok(Words.ListCmdW.copied(all.done, all.skipped, all.why, li.name) + nowIn(t, into));
                            ok.made = into;
                            ok.entered = entered;
                            return ok;
                        }
                    }, move, lib);                                              // העברה מוציאה מהמקור — גיבוי לפני
                    if (r.error != null) lastFailed = true;
                    s.made = r.made;
                    s.entered = r.entered;
                    return r.error != null ? r.error : Words.DbCmd.inDb(r.text);
                }
            };
            return;
        }
        if (add) {
            // אותו כלל כמו "העתק "<קובץ>" ידנית "<רשימה>"" כתובה (addFiles); מה שנובע מהמקום: הרשימה — לפי המספר. רק מוסיפה — בלי גיבוי לפני.
            final long into = sel.in;
            final List<String> ps = new ArrayList<String>(fsFiles);
            final List<String> ds = hasDirs ? new ArrayList<String>(fsDirs) : new ArrayList<String>();
            final int taken = takenOf(sw);
            s.describe = Words.DbCmd.inDb((ds.isEmpty() ? "" : Words.folders(ds.size()) + " · ") + Words.files(ps.size()) + " — "
                    + Words.ListCmdW.copied(ps.size() + ds.size(), 0, null, Words.DbCmd.HERE_LIST));
            s.op = new ListCmd.Op() {
                @Override public String run(Library lib) {
                    Res r = realRes(mode, new Work() {
                        @Override public Res run(Store t, Finder f) {
                            long dest = into != 0 ? into : DbPaths.home(t);
                            Store.Item li = dest == 0 ? null : t.item(dest);
                            if (li == null || li.type != Store.LIST) return Res.err(Words.Db.LIST_GONE);
                            if (!ds.isEmpty()) {                                // מבנה 115: התיקיות נסרקות כאן, בהרצה (המסך מריץ ברקע)
                                List<Tree.Node> nodes = new ArrayList<Tree.Node>();
                                if (!live) for (String d : ds) nodes.add(Tree.scanFolder(new File(d), !flat));
                                return addFolders(t, dest, li.name, ds, nodes, ps, live, flat, taken);
                            }
                            Res ok = addFiles(t, dest, ps, Words.files(ps.size()), li.name);
                            ok.made = dest;
                            return ok;
                        }
                    }, false, lib);
                    if (r.error != null) lastFailed = true;
                    s.made = r.made;
                    return r.error != null ? r.error : Words.DbCmd.inDb(r.text);
                }
            };
            return;
        }
        boolean remove = (cmd.equals("RD") || cmd.equals("DEL")) && p.sides.size() == 1 && sw.isEmpty()
                && a.kind == ListCmd.LIST && !a.content && !a.filesOf;
        if (!remove) { s.error = Words.Cmd.SELECTED_IN_LIST; return; }
        final long in = sel.in;
        final List<Long> ids = new ArrayList<Long>(sel.ids);
        final Work w = new Work() {
            @Override public Res run(Store t, Finder f) {
                for (Long id : ids) Lists.removeFrom(t, in, id);
                return Res.ok(Words.DbCmd.removed(Words.DbCmd.items(ids.size())));
            }
        };
        s.target = a.label();
        s.describe = Words.DbCmd.inDb(Words.DbCmd.removed(Words.DbCmd.items(ids.size())));
        s.op = new ListCmd.Op() {
            @Override public String run(Library lib) { return real(mode, w, true, lib); }
        };
    }

    /** מבנה 104: "/תפוס:<ערך>" (Verbs.Cmd.taken) ← מה שהחומר מבין. "" ו"לשאול" — בקובץ אין את מי לשאול: לעצור. */
    static int taken(String v) {
        return Syntax.TAKEN_REPLACE.equals(v) ? Lists.TAKEN_REPLACE : Syntax.TAKEN_RENAME.equals(v) ? Lists.TAKEN_RENAME
                : Syntax.TAKEN_SKIP.equals(v) ? Lists.TAKEN_SKIP : Lists.TAKEN_STOP;
    }

    /** מבנה 104: ההודעה לפי מה שקרה. */
    static Res smartMade(Lists.Made m) {
        if (m.error != null) return Res.err(m.error);
        if (m.skipped) return Res.same(Words.ListCmdW.smartSkipped(m.name));
        Res ok = Res.ok(m.replaced ? Words.ListCmdW.smartReplaced(m.name) : m.renamed ? Words.ListCmdW.smartRenamed(m.name)
                : Words.ListCmdW.createdSmart(m.name));
        ok.made = m.id;
        ok.replaced = m.replaced;
        return ok;
    }

    /**
     * מבנה 97: רשימה חכמה חדשה מהטופס — "צור רשימה חכמה %שם-חדש% %תוכן%". השם, התנאי ושאר מה שבטופס — מהבחירה, ואינם
     * מפוענחים. שלא כמו השורה הכתובה (שם תפוס עם תנאי זהה — אותה רשימה; עם תנאי אחר — שם חדש "(2)"; נוצרת לעצמה, לא בתוך
     * רשימה) — כאן כמו שהמסך עשה: תמיד חדשה, בתוך הרשימה שמוצגת (0 — הבית), בשם שהוקלד (ריק — "רשימה חכמה"). בלי הדמיה ובלי
     * גיבוי לפני (רק מוסיפה). לא נפתחת בסוף ההרצה — המסך מחליט.
     */
    private static String planPickedSmart(final Batch.Step s, final Verbs.Cmd c, Sim sim) {
        final Batch.Selection sel = sim.selected;
        if (sel == null || sel.props == null || sel.mode == null || sel.name == null || sel.content == null) {
            s.error = Words.Cmd.NOTHING_SELECTED;
            return null;
        }
        final String mode = sel.mode;
        if (!Db.dbFile(Db.app, mode).isFile()) { s.error = Words.DbCmd.noMode(mode, sim.modes()); return null; }
        c.noOpen = true;
        final long into = sel.in;
        final String name = sel.name.trim(), cond = sel.content;
        final java.util.Map<String, String> props = sel.props;
        final String shown = name.isEmpty() ? Words.SMART_LIST : name;
        final int taken = taken(c.taken);
        // מבנה 103–104: הכלל האחד — שם קיים באותה רשימה: עוצרים כבר בתכנון, והמסך שואל (לדרוס / שם אחר / לעצור)
        boolean exists = false;
        try {
            exists = Db.read(Db.app, mode, new Db.Use<Boolean>() {
                @Override public Boolean run(SqlStore t) {
                    long in = into != 0 ? into : DbPaths.home(t);
                    return Lists.smartTaken(t, in, shown);
                }
            });
        } catch (Throwable e) {
            Log.e("DbListCmd.smartTaken", e);
        }
        if (exists && taken == Lists.TAKEN_STOP) { s.error = Words.ListCmdW.smartTaken(shown); return null; }
        final String before = exists && taken == Lists.TAKEN_REPLACE ? Backups.OP_EDIT : null;   // דריסה — גיבוי לפני
        s.describe = Words.DbCmd.inDb(exists && taken == Lists.TAKEN_REPLACE ? Words.ListCmdW.smartReplaced(shown) : Words.ListCmdW.createdSmart(shown));
        s.op = new ListCmd.Op() {
            @Override public String run(Library lib) {
                Res r = realRes(mode, new Work() {
                    @Override public Res run(Store t, Finder f) {
                        if (into != 0 && (t.item(into) == null || t.item(into).type != Store.LIST)) return Res.err(Words.Db.LIST_GONE);
                        return smartMade(Lists.newSmart(t, into, name, cond, props, taken));
                    }
                }, before, lib);
                if (r.error != null) lastFailed = true;
                s.made = r.made;
                return r.error != null ? r.error : Words.DbCmd.inDb(r.text);
            }
        };
        return mode;
    }

    /**
     * מבנה 99: עריכה של רשימה חכמה בטופס שלה — "עדכן רשימה חכמה %נבחרים% %תוכן%". הרשימה — לפי המספר שלה (הבחירה); השם החדש
     * (ריק — בלי שינוי), התנאי ושאר מה שבטופס — מהבחירה, ואינם מפוענחים. שלא כמו השורה הכתובה (לפי שם; רק התנאי והמיון) — כאן
     * כמו שהמסך עשה: כל הטופס, בעסקה אחת; **גיבוי "לפני עריכה" לפני** (מבנה 58 — התנאי הקודם נדרס); ושם שהשתנה — קובצי הפקודות
     * שפונים אליה מתעדכנים (מ0.42, ב8). רשימה שנמחקה בזמן שהטופס היה פתוח — "הפריט הזה כבר לא קיים במסד — לא נשמר" (מבנה 60).
     */
    private static String planPickedSmartEdit(final Batch.Step s, final Verbs.Cmd c, Sim sim) {
        final Batch.Selection sel = sim.selected;
        if (sel == null || sel.props == null || sel.mode == null || sel.ids == null || sel.ids.size() != 1 || sel.name == null || sel.content == null) {
            s.error = Words.Cmd.NOTHING_SELECTED;
            return null;
        }
        final String mode = sel.mode;
        if (!Db.dbFile(Db.app, mode).isFile()) { s.error = Words.DbCmd.noMode(mode, sim.modes()); return null; }
        c.noOpen = true;
        final long id = sel.ids.get(0);
        final String name = sel.name.trim(), cond = sel.content;
        final java.util.Map<String, String> props = sel.props;
        try {                                                                    // מבנה 104: שם קיים — כבר בתכנון, כדי שהמסך ישאל
            String taken = Db.read(Db.app, mode, new Db.Use<String>() {
                @Override public String run(SqlStore t) {
                    Store.Item it = t.item(id);
                    if (it == null || name.isEmpty() || name.equals(it.name)) return null;
                    for (Long p : t.listsOf(id)) if (Lists.smartTaken(t, p, name)) return Words.ListCmdW.smartTaken(name);
                    return null;
                }
            });
            if (taken != null) { s.error = taken; return null; }
        } catch (Throwable e) {
            Log.e("DbListCmd.smartTaken", e);
        }
        s.describe = Words.DbCmd.inDb(Words.DbCmd.saved(name.isEmpty() ? Words.DbCmd.HERE_LIST : name));
        s.op = new ListCmd.Op() {
            @Override public String run(Library lib) {
                Res r = realRes(mode, new Work() {
                    @Override public Res run(Store t, Finder f) {
                        Store.Item it = t.item(id);
                        if (it == null || it.type != Store.SMART) return Res.err(Words.Db.ITEM_GONE);
                        String was = it.name;
                        if (!name.isEmpty() && !name.equals(was))                // מבנה 104: שם חדש שכבר קיים באחת הרשימות שהיא בהן — שם קיים
                            for (Long p : t.listsOf(id)) if (Lists.smartTaken(t, p, name)) return Res.err(Words.ListCmdW.smartTaken(name));
                        Lists.saveSmart(t, id, 0, name, cond, props);
                        Res ok = Res.ok(Words.DbCmd.saved(name.isEmpty() ? was : name)).renamed(id, was, t.item(id).name);
                        ok.made = id;
                        return ok;
                    }
                }, Backups.OP_EDIT, lib);
                if (r.error != null) lastFailed = true;
                s.made = r.made;
                return r.error != null ? r.error : Words.DbCmd.inDb(r.text);
            }
        };
        return mode;
    }

    /**
     * מבנה 113: הכלל האחד של עריכת רשימה (שם, צלמית, תיאור, "למה") — Lists.edit, לשורה הכתובה ולטופס שבמסך. null — בלי שינוי.
     * פריט שנמחק בינתיים — "הפריט הזה כבר לא קיים במסד — לא נשמר" (מבנה 60). דורס את מה שהיה — ולכן replaced (גיבוי "לפני עריכה").
     */
    private static Res edited(Store t, long id, String name, String icon, String desc, String why) {
        Store.Item it = t.item(id);
        if (it == null) return Res.err(Words.Db.ITEM_GONE);
        String was = it.name;
        String e = Lists.edit(t, id, name, icon, desc, why);
        if (e != null) return Res.err(e);
        Res ok = Res.ok(Words.DbCmd.saved(t.item(id).name)).renamed(id, was, t.item(id).name);   // מ0.42: קובצי הפקודות שפונים אליה
        ok.made = id;
        ok.replaced = true;
        return ok;
    }

    /**
     * מבנה 113: הטופס של עריכת רשימה במסך — "עדכן רשימה %נבחרים%". הפריט — לפי המספר שלו (הבחירה); השם, הצלמית, התיאור וה"למה" —
     * מהבחירה, ואינם מפוענחים. אותו כלל כמו בשורה הכתובה (edited). מה שנובע מהמקום: במסך — כל הטופס, ושדה שם ריק פירושו "בלי
     * שינוי בשם"; ורעיון / כותרת (שאין להם דרך של שמות) — רק מכאן, והטקסט שלהם הוא ה"שם".
     */
    private static String planPickedEdit(final Batch.Step s, final Verbs.Cmd c, Sim sim) {
        final Batch.Selection sel = sim.selected;
        // מבנה 121: סדר ידני ("/מקום:") של הפריטים שנבחרו, בתוך הרשימה שמוצגת; ועיצוב ("/עיצוב:%תוכן%") לרשימה. בלי גיבוי — כמו במסך.
        final boolean place = c.edit != null && c.edit.containsKey(Verbs.E_PLACE), design = c.edit != null && c.edit.containsKey(Store.P_DESIGN);
        if (place || design) {
            if (sel == null || sel.mode == null || sel.ids == null || sel.ids.isEmpty() || (place && design) || c.edit.size() != 1
                    || (design && (sel.ids.size() != 1 || sel.content == null))) {
                s.error = Words.Cmd.NOTHING_SELECTED;
                return null;
            }
            final String m = sel.mode;
            if (!Db.dbFile(Db.app, m).isFile()) { s.error = Words.DbCmd.noMode(m, sim.modes()); return null; }
            final long in = sel.in;
            final List<Long> ids = new ArrayList<Long>(sel.ids);
            final int how = place ? Verbs.place(c.edit.get(Verbs.E_PLACE)) : 0;
            final String name = design ? sel.content.trim() : null;
            s.describe = Words.DbCmd.inDb(place ? Words.ListCmdW.ORDERED : Words.ListCmdW.DESIGN_SET);
            s.op = new ListCmd.Op() {
                @Override public String run(Library lib) {
                    Res r = realRes(m, new Work() {
                        @Override public Res run(Store t, Finder f) {
                            if (place) {
                                if (in == 0 || t.item(in) == null) return Res.err(Words.Db.LIST_GONE);
                                DbMore.order(t, in, ids, how);
                                return Res.ok(Words.ListCmdW.ORDERED);
                            }
                            Store.Item it = t.item(ids.get(0));
                            if (it == null || it.type != Store.LIST) return Res.err(Words.Db.LIST_GONE);
                            t.setProp(it.id, Store.P_DESIGN, name);              // "" — לרשת (המאפיין נמחק)
                            return Res.ok(Words.ListCmdW.DESIGN_SET);
                        }
                    }, false, lib);
                    if (r.error != null) lastFailed = true;
                    return r.error != null ? r.error : Words.DbCmd.inDb(r.text);
                }
            };
            return m;
        }
        if (sel == null || sel.props == null || sel.mode == null || sel.ids == null || sel.ids.size() != 1 || sel.name == null) {
            s.error = Words.Cmd.NOTHING_SELECTED;
            return null;
        }
        final String mode = sel.mode;
        if (!Db.dbFile(Db.app, mode).isFile()) { s.error = Words.DbCmd.noMode(mode, sim.modes()); return null; }
        final long id = sel.ids.get(0);
        s.describe = Words.DbCmd.inDb(Words.DbCmd.saved(sel.name.trim().isEmpty() ? Words.DbCmd.HERE_LIST : sel.name.trim()));
        s.op = new ListCmd.Op() {
            @Override public String run(Library lib) {
                Res r = realRes(mode, new Work() {
                    @Override public Res run(Store t, Finder f) {
                        Store.Item it = t.item(id);
                        if (it != null && (it.type == Store.SMART || it.type == Store.FILE)) return Res.err(Words.DbCmd.FILE_NO_EDIT);
                        return edited(t, id, sel.name, sel.props.get(Store.P_ICON), sel.props.get(Store.P_DESC), sel.props.get(Store.P_WHY));
                    }
                }, Backups.OP_EDIT, lib);
                if (r.error != null) lastFailed = true;
                s.made = r.made;
                return r.error != null ? r.error : Words.DbCmd.inDb(r.text);
            }
        };
        return mode;
    }

    /**
     * מבנה 93: רעיון או כותרת מהמסך — "צור רעיון %נבחרים%" · "צור כותרת %נבחרים% "דרך"". הטקסט הוא מה שהוקלד (מהבחירה, אינו
     * מפוענח); הרשימה — זו שמוצגת במסך (המספר שלה), או הדרך שנכתבה אחרי. בלי הדמיה (כמו כל פעולה של %נבחרים% ברשימות), ובלי
     * גיבוי לפני (רק מוסיפה) — כמו שהמסך עשה. הסירובים — בנוסח של המסך.
     */
    private static String planPickedNote(final Batch.Step s, final Verbs.Cmd c, final boolean title, Sim sim) {
        final Batch.Selection sel = sim.selected;
        if (sel == null || sel.ids == null || sel.mode == null || sel.name == null || sel.name.trim().isEmpty()) {
            s.error = Words.Cmd.NOTHING_SELECTED;
            return null;
        }
        final String mode = sel.mode;
        if (!Db.dbFile(Db.app, mode).isFile()) { s.error = Words.DbCmd.noMode(mode, sim.modes()); return null; }
        final List<String> path = Names.parsePath(c.cond);
        final long here = sel.in;
        if (path.isEmpty() && here == 0) { s.error = Words.DbCmd.NOTE_NEEDS_LIST; return null; }
        final String text = sel.name, where = path.isEmpty() ? Words.DbCmd.HERE_LIST : Names.label(path);
        s.describe = Words.DbCmd.inDb(Words.DbCmd.noteAdded(title, text, where));
        s.op = new ListCmd.Op() {
            @Override public String run(Library lib) {
                Res r = realRes(mode, new Work() {
                    @Override public Res run(Store t, Finder f) {
                        long in = path.isEmpty() ? here : DbPaths.find(t, mode, path);
                        if (in == 0) return Res.err(Words.Verb.NOT_FOUND + c.cond);
                        Store.Item it = t.item(in);
                        if (it == null || it.type != Store.LIST) return Res.err(Words.Db.LIST_GONE);
                        Res ok = Res.ok(Words.DbCmd.noteAdded(title, text, path.isEmpty() ? it.name : where));
                        ok.made = title ? t.addTitle(in, text) : t.addNote(in, text);
                        return ok;
                    }
                }, false, lib);
                if (r.error != null) lastFailed = true;
                s.made = r.made;
                return r.error != null ? r.error : Words.DbCmd.inDb(r.text);
            }
        };
        return mode;
    }

    /**
     * מ0.25 (באג 7; המשתמש: "כל הפעלים בפקודות רצים לפי הסדר בו הם כתובים"): החלק הכותב של פועל בעברית — "צור רשימה",
     * "צור רשימה חכמה", "עדכן רשימה חכמה" — הוא שלב רגיל: על העותק בתכנון (שורות מאוחרות רואות אותו), ובהרצה על המסד, במקומו.
     * מחזיר את האופן אם זה פועל כותב (ואז בסוף רק פותחים את המסך), null — אינו כותב (נשאר כולו לסוף).
     */
    static String planVerb(final Batch.Step s, final Verbs.Cmd c, final Sim sim) {
        final boolean list = c.kind.equals(Verbs.K_LIST) && c.verb.equals(Verbs.CREATE);
        final boolean smartNew = c.kind.equals(Verbs.K_SMART) && c.verb.equals(Verbs.CREATE);
        final boolean smartUpd = c.kind.equals(Verbs.K_SMART) && c.verb.equals(Verbs.UPDATE);
        final boolean note = c.kind.equals(Verbs.K_NOTE), title = c.kind.equals(Verbs.K_TITLE);   // מ0.37
        final boolean listUpd = c.kind.equals(Verbs.K_LIST) && c.verb.equals(Verbs.UPDATE);   // מבנה 113: עדכן רשימה
        if (!list && !smartNew && !smartUpd && !note && !title && !listUpd) return null;
        if (listUpd) c.noOpen = true;                                           // השם יכול להשתנות — לא פותחים בסוף
        if (listUpd && Parse.var(Batch.V_SELECTED).equals(c.arg)) return planPickedEdit(s, c, sim);                  // מבנה 113: מהטופס
        if ((note || title) && Parse.var(Batch.V_SELECTED).equals(c.arg)) return planPickedNote(s, c, title, sim);   // מבנה 93: מהמסך
        if (smartNew && Parse.var(Batch.V_NEW_NAME).equals(c.arg)) return planPickedSmart(s, c, sim);                // מבנה 97: מהטופס
        if (smartUpd && Parse.var(Batch.V_SELECTED).equals(c.arg)) return planPickedSmartEdit(s, c, sim);            // מבנה 99: מהטופס
        if (sim.modeError != null) { s.error = sim.modeError; return null; }
        final String mode = mode(sim);
        final Store db;
        try {
            db = sim(sim, mode);
        } catch (Throwable t) {
            Log.e("DbListCmd.sim", t);
            s.error = Words.Db.failed(String.valueOf(t.getMessage()));
            return null;
        }
        final List<String> path = list ? Names.parsePath(c.arg) : note || title ? Names.parsePath(c.cond) : null;
        if ((note || title) && path.isEmpty()) { s.error = Words.DbCmd.NOTE_NEEDS_LIST; return null; }
        final Work work = new Work() {
            @Override
            public Res run(Store t, Finder f) {
                if (note || title) {
                    DbPaths.Found at = DbPaths.listAt(t, mode, path);            // מבנה 107
                    long in = at.error != null ? 0 : at.id;
                    if (in == 0) return Res.err(at.ambiguous ? at.error : Words.ListCmdW.noList(Names.label(path)));
                    if (title) t.addTitle(in, c.arg);
                    else t.addNote(in, c.arg);
                    return Res.ok(Words.DbCmd.noteAdded(title, c.arg, Names.label(path)));
                }
                if (listUpd) {                                                   // מבנה 113: הרשימה — לפי הדרך (מבנה 107); רק מה שנכתב משתנה
                    List<String> at = Names.parsePath(c.arg);
                    DbPaths.Found u = DbPaths.listAt(t, mode, at);
                    if (u.error != null || u.id == 0) return Res.err(u.ambiguous ? u.error : Words.ListCmdW.noList(Names.label(at)));
                    if (c.edit.containsKey(Verbs.E_PLACE)) {                     // מבנה 121: סדר ידני — בתוך הרשימה שהיא בה
                        List<String> up = at.subList(0, at.size() - 1);
                        long parent = up.isEmpty() ? DbPaths.home(t) : DbPaths.find(t, mode, new ArrayList<String>(up));
                        List<Long> in = t.listsOf(u.id);
                        if (!in.contains(parent)) parent = in.size() == 1 ? in.get(0) : 0;
                        if (parent == 0) return Res.err(Words.DbCmd.needFullWay(Names.label(at)));
                        DbMore.order(t, parent, java.util.Collections.singletonList(u.id), Verbs.place(c.edit.get(Verbs.E_PLACE)));
                    }
                    if (c.edit.containsKey(Store.P_DESIGN)) t.setProp(u.id, Store.P_DESIGN, c.edit.get(Store.P_DESIGN).trim());   // "" — לרשת
                    return edited(t, u.id, c.edit.get(Verbs.E_NAME), c.edit.get(Store.P_ICON), c.edit.get(Store.P_DESC), c.edit.get(Store.P_WHY));
                }
                // מבנה 100: המספר של מה שנוצר / עודכן חוזר (Res.made ← Step.made) — כפתור שמריץ את השורה פותח בדיוק אותו
                if (list) {                                                      // מבנה 102: "צור רשימה" — הכלל האחד; שם קיים — סירוב
                    if (path == null || path.isEmpty()) return Res.err(Words.Verb.NOT_FOUND + c.arg);
                    return listMade(Lists.newListAt(t, mode, path, taken(c.taken)), Names.label(path));   // מבנה 109: /תפוס:
                }
                if (smartNew) {                                                  // מבנה 103–104: הכלל האחד (Lists.newSmart) — בבית
                    java.util.Map<String, String> props = new java.util.HashMap<String, String>();
                    props.put(Store.P_SORT, c.sort);
                    return smartMade(Lists.newSmart(t, 0, c.arg, c.cond, props, taken(c.taken)));
                }
                DbPaths.Found u = DbPaths.updateSmart(t, mode, Names.parsePath(c.arg), c.cond, c.sort);   // מבנה 106
                if (u.error != null) return Res.err(u.error);
                Res ok = Res.ok(Words.DbCmd.updated(c.arg));
                ok.made = u.id;
                ok.replaced = true;                                              // מבנה 106: דורס את התנאי — גיבוי לפני (הוראות, סעיף 4)
                return ok;
            }
        };
        Res r;
        try {
            r = work.run(db, finder(sim));                                   // ההדמיה — על העותק
        } catch (Throwable e) {
            r = Res.err(Words.Db.failed(String.valueOf(e.getMessage())));
        }
        if (r.error != null) { s.error = r.error; return null; }
        if (r.noop) {                                                            // מבנה 104: גם "בלי שינוי" נאמר בדוח (עד כאן — השורה נעלמה ממנו)
            s.noop = true;
            s.describe = Words.DbCmd.inDb(r.text);
        } else {
            s.describe = Words.DbCmd.inDb(r.text);
            final String before = r.replaced ? Backups.OP_EDIT : null;          // מבנה 104: דריסה — גיבוי לפני
            s.op = new ListCmd.Op() {
                @Override public String run(Library lib) {
                    Res done = realRes(mode, work, before, lib);                 // כמו real(), ועוד המספר של מה שנוצר (מבנה 100)
                    if (done.error != null) lastFailed = true;
                    s.made = done.made;
                    return done.error != null ? done.error : Words.DbCmd.inDb(done.text);
                }
            };
        }
        return mode;
    }

    /**
     * מ0.25 (באג 2): המקור של COMP /רשימה:<שם> — מהמסד (עד עכשיו נקרא עץ הרשימות של המנוע הישן, והרשימה שבמסד לא נמצאה).
     * הרשימה: הדרך כמו שנכתבה ("א/ב"); ואם אין — בתוך "שמור לתוכנה". כל הקבצים שבה, בכל העומק, כולל מה שהרשימות החכמות
     * שבתוכה מוצאות עכשיו. null — שגיאה (ב־err).
     */
    static Compare.Source compSource(Sim sim, String name, String skip, String[] err) {
        if (sim.modeError != null) { err[0] = sim.modeError; return null; }
        final String mode = mode(sim);
        Store db;
        try {
            db = sim(sim, mode);
        } catch (Throwable t) {
            Log.e("DbListCmd.compSource", t);
            err[0] = Words.Db.failed(String.valueOf(t.getMessage()));
            return null;
        }
        List<String> path = Names.parsePath(name == null ? "" : name.replace('\\', '/'));
        DbPaths.Found at = DbPaths.listAt(db, mode, path);
        long id = at.error != null ? 0 : at.id;
        if (id == 0 && !path.isEmpty() && !at.ambiguous) {
            List<String> in = new ArrayList<String>();
            in.add(Reserved.ROOT);
            in.addAll(path);
            id = DbPaths.find(db, mode, in);
        }
        if (id == 0) { err[0] = at.ambiguous ? at.error : Words.ListCmdW.noList(name); return null; }
        Set<String> out = new LinkedHashSet<String>();
        if (!deep(db, id, finder(sim), out, new HashSet<Long>())) { err[0] = Words.ListCmdW.INDEX_NOT_READY; return null; }
        Compare.Source src = new Compare.Source();
        for (String p : out) Compare.addFile(sim.lib, src, new File(p), skip);
        return src;
    }

    // ---------- מ0.14: ודא ----------

    /** מה לוודא: קיימת/חסרה, ומספרי רשימות ופריטים (-1 — לא נבדק). */
    static final class Expect {
        Boolean exists;
        long lists = -1, items = -1;
        String error;

        String text(ListCmd.Side a) {
            StringBuilder sb = new StringBuilder(a.label());
            if (exists != null) sb.append(' ').append(exists ? Words.DbCmd.WORD_EXISTS : Words.DbCmd.WORD_MISSING);
            if (lists >= 0) sb.append(' ').append(Words.DbCmd.SW_LISTS).append(lists);
            if (items >= 0) sb.append(' ').append(Words.DbCmd.SW_ITEMS).append(items);
            return sb.toString();
        }
    }

    static Expect expect(List<String> conds) {
        Expect e = new Expect();
        for (String c : conds) {
            String t = c.trim();
            try {
                if (t.equals(Words.DbCmd.WORD_EXISTS)) e.exists = Boolean.TRUE;
                else if (t.equals(Words.DbCmd.WORD_MISSING)) e.exists = Boolean.FALSE;
                else if (t.startsWith(Words.DbCmd.SW_LISTS)) e.lists = Long.parseLong(t.substring(Words.DbCmd.SW_LISTS.length()).trim());
                else if (t.startsWith(Words.DbCmd.SW_ITEMS)) e.items = Long.parseLong(t.substring(Words.DbCmd.SW_ITEMS.length()).trim());
                else e.error = Words.DbCmd.VERIFY_USAGE;
            } catch (NumberFormatException x) {
                e.error = Words.DbCmd.VERIFY_USAGE;
            }
        }
        if (e.exists == null && e.lists < 0 && e.items < 0) e.exists = Boolean.TRUE;   // בלי תנאי — קיימת
        if (Boolean.FALSE.equals(e.exists) && (e.lists >= 0 || e.items >= 0)) e.error = Words.DbCmd.VERIFY_USAGE;
        return e;
    }

    /** הבדיקה עצמה על המסד: Res.ok — עבר; Res.err — נכשל (הטקסט: מה נמצא בפועל). */
    static boolean verify(Store s, String mode, ListCmd.Side a, Expect e, String[] found) {
        if (a.kind == ListCmd.LOOSE) {                                        // מ0.41: כמה קבצים אינם באף רשימה
            long n = 0;
            for (Store.Item it : s.unassigned()) if (it.type == Store.FILE) n++;
            found[0] = Words.DbCmd.SW_ITEMS + n;
            return !Boolean.FALSE.equals(e.exists) && e.lists <= 0 && (e.items < 0 || e.items == n);
        }
        long id = a.kind == ListCmd.SMART_LIST ? (smart(s, mode, a) == null ? 0 : smart(s, mode, a)[1]) : list(s, mode, a);
        if (id == 0) {
            found[0] = Words.DbCmd.WORD_MISSING;
            return Boolean.FALSE.equals(e.exists);
        }
        if (Boolean.FALSE.equals(e.exists)) {
            found[0] = Words.DbCmd.WORD_EXISTS;
            return false;
        }
        DbCount c = a.kind == ListCmd.SMART_LIST ? null : DbCount.of(s, id, null);
        long lists = c == null ? 0 : c.lists, items = c == null ? 0 : c.items;
        found[0] = Words.DbCmd.SW_LISTS + lists + " " + Words.DbCmd.SW_ITEMS + items;
        return (e.lists < 0 || e.lists == lists) && (e.items < 0 || e.items == items);
    }

    /** שורת ודא: בתכנון — על העותק (מה צפוי), בהרצה — על המסד; ❌ לא עוצרת. */
    static void planVerify(final Batch.Step s, final ListCmd.Side a, List<String> conds, Sim sim) {
        if (!a.list() || a.content || a.filesOf || a.mask != null) { s.error = Words.DbCmd.VERIFY_USAGE; return; }
        final Expect e = expect(conds);
        if (e.error != null) { s.error = e.error; return; }
        if (sim.modeError != null) { s.error = sim.modeError; return; }
        final String mode = mode(sim);
        Store db;
        try {
            db = sim(sim, mode);
        } catch (Throwable t) {
            Log.e("DbListCmd.verify", t);
            s.error = Words.Db.failed(String.valueOf(t.getMessage()));
            return;
        }
        final String what = e.text(a);
        String[] found = {""};
        boolean now = verify(db, mode, a, e, found);
        s.target = a.label();
        s.describe = Words.DbCmd.verifyWill(what) + " · " + Words.DbCmd.inDb(Words.DbCmd.verifyResult(now, what, found[0]));
        s.op = new ListCmd.Op() {
            @Override
            public String run(Library lib) {
                SqlStore st = null;
                try {
                    st = Db.openRead(Db.app, mode);
                    String[] f = {""};
                    boolean ok = verify(st, mode, a, e, f);
                    s.verified = ok;
                    return Words.DbCmd.verifyResult(ok, what, f[0]);
                } catch (Throwable t) {
                    s.verified = Boolean.FALSE;
                    return Words.Db.failed(String.valueOf(t.getMessage()));
                } finally {
                    if (st != null) st.close();
                }
            }
        };
    }

    /**
     * מבנה 90: השווה ידנית "א" ידנית "ב" /הבדלים — הדוח המלא (DbDiff): רק ב"א", רק ב"ב", מקום אחר, שם אחר, מאפיין שונה, קישור
     * שונה. עם "תוכן" בשני הצדדים — רק מה שבתוכן. מחושב **בהרצה**, על המסד עצמו (ולא בתכנון, על העותק): כך הוא רואה את מה
     * שהשורות שלפניו עשו באמת. אינו ✅ / ❌ — זה דוח; מי שרוצה בדיקה כותב "ודא … שווה …".
     */
    private static void planDiff(final Batch.Step s, final ListCmd.Side a, final ListCmd.Side b, List<String> sw, final String mode) {
        for (ListCmd.Side x : new ListCmd.Side[]{a, b})
            if (x.kind != ListCmd.LIST || x.filesOf || x.mask != null) { s.error = Words.ListCmdW.USAGE_DIFF; return; }
        if (a.content != b.content || sw.size() != 1) { s.error = Words.ListCmdW.USAGE_DIFF; return; }
        s.target = a.label();
        s.describe = Words.DbCmd.inDb(Words.DbCmd.diffWill(a.label(), b.label()));
        s.op = new ListCmd.Op() {
            @Override
            public String run(Library lib) {
                SqlStore st = null;
                try {
                    st = Db.openRead(Db.app, mode);
                    return Words.DbCmd.inDb(diffText(st, mode, a, b));
                } catch (Throwable t) {
                    lastFailed = true;
                    return Words.Db.failed(String.valueOf(t.getMessage()));
                } finally {
                    if (st != null) st.close();
                }
            }
        };
    }

    /** הדוח כטקסט: שורת סיכום, ואחריה שורה לכל הבדל. רשימה שאיננה — כישלון. */
    static String diffText(Store s, String mode, ListCmd.Side a, ListCmd.Side b) {
        long x = list(s, mode, a), y = list(s, mode, b);
        if (x == 0 || y == 0) {
            ListCmd.Side gone = x == 0 ? a : b;
            lastFailed = true;
            return listWhy(s, mode, gone);
        }
        DbDiff.Out o = DbDiff.of(s, x, y, a.label(), b.label(), !a.content);
        String relation = x == y ? Words.DbCmd.REL_SAME : a.content ? Words.DbCmd.REL_CONTENT_EQUAL : Words.DbCmd.REL_EQUAL;
        StringBuilder sb = new StringBuilder(Words.DbCmd.diffSummary(a.label(), b.label(), o.total(), o.onlyA, o.onlyB, o.moved, o.renamed,
                o.props, o.links, o.sizes, o.other, o.lines.size(), relation));
        for (String l : o.lines) sb.append("\n   · ").append(l);
        return sb.toString();
    }

    /**
     * מבנה 87–88: היחס בין שתי רשימות, לפי ההגדרה של המשתמש (15.11.2026): "רשימות בעלות אותו שם, אותה תכולה, היושבות בשני
     * מקומות שונים במסד — כלומר עם שני מספרים שונים — הן דומות או שוות, אך לא זהות. אם נשנה באחת מהן פרט אחד — הן כבר יהיו
     * דומות ולא שוות."
     *   זהה  — אותה רשימה ממש (אותו מספר במסד), שהדרכים שנכתבו מגיעות אליה משני מקומות ("קשר");
     *   שווה — רשימה אחרת (מספר אחר), אותו שם ואותו תוכן עד הסוף (DbSnap: סדר, עומק, סוגים, שמות, מאפיינים, קישורים פנימיים);
     *   דומה — רשימה אחרת שחלק ממנה שווה (השם, או התוכן) ויש הבדל — זה נאמר בתוצאה, עם ההבדל הראשון.
     * "ודא ידנית א זהה ידנית ב" · "ודא ידנית א שווה ידנית ב" · "ודא תוכן ידנית א שווה תוכן ידנית ב" (רק מה שבתוכן — השמות של
     * שתי הרשימות והמאפיינים שלהן עצמן אינם נבדקים). "שווה" נכשל גם כשהשתיים זהות: מי שבודק שנוצר עותק — רוצה לדעת אם קיבל קישור.
     */
    static void planVerifySame(final Batch.Step s, final ListCmd.Side a, final ListCmd.Side b, final boolean exact, Sim sim) {
        for (ListCmd.Side x : new ListCmd.Side[]{a, b})
            if (x.kind != ListCmd.LIST || x.filesOf || x.mask != null) { s.error = Words.DbCmd.VERIFY_SAME_USAGE; return; }
        if (a.content != b.content || (exact && a.content)) { s.error = Words.DbCmd.VERIFY_SAME_USAGE; return; }
        if (sim.modeError != null) { s.error = sim.modeError; return; }
        final String mode = mode(sim);
        Store db;
        try {
            db = sim(sim, mode);
        } catch (Throwable t) {
            Log.e("DbListCmd.verifySame", t);
            s.error = Words.Db.failed(String.valueOf(t.getMessage()));
            return;
        }
        final String what = exact ? Words.DbCmd.verifySame(a.label(), b.label()) : Words.DbCmd.verifyEqual(a.label(), b.label(), a.content);
        String[] found = {""};
        boolean now = related(db, mode, a, b, exact, found);
        s.target = a.label();
        s.describe = Words.DbCmd.verifyWill(what) + " · " + Words.DbCmd.inDb(Words.DbCmd.verifyResult(now, what, found[0]));
        s.op = new ListCmd.Op() {
            @Override
            public String run(Library lib) {
                SqlStore st = null;
                try {
                    st = Db.openRead(Db.app, mode);
                    String[] f = {""};
                    boolean ok = related(st, mode, a, b, exact, f);
                    s.verified = ok;
                    return Words.DbCmd.verifyResult(ok, what, f[0]);
                } catch (Throwable t) {
                    s.verified = Boolean.FALSE;
                    return Words.Db.failed(String.valueOf(t.getMessage()));
                } finally {
                    if (st != null) st.close();
                }
            }
        };
    }

    /** exact — "זהה" (אותה רשימה); אחרת "שווה" (רשימה אחרת, שווה). found — מה היחס באמת, כשלא. */
    static boolean related(Store s, String mode, ListCmd.Side a, ListCmd.Side b, boolean exact, String[] found) {
        long x = list(s, mode, a), y = list(s, mode, b);
        if (x == 0 || y == 0) {
            ListCmd.Side gone = x == 0 ? a : b;
            found[0] = listWhy(s, mode, gone);
            return false;
        }
        if (x == y) {
            found[0] = Words.DbCmd.REL_SAME;
            return exact;
        }
        List<String> sa = DbSnap.of(s, x), sb = DbSnap.of(s, y);
        String inside = DbSnap.diff(sa.subList(1, sa.size()), sb.subList(1, sb.size()));     // מה שבתוכן
        String own = sa.get(0).equals(sb.get(0)) ? null : Words.DbCmd.snapDiff(1, sa.get(0), sb.get(0), 1, 1);   // המאפיינים שלהן עצמן
        String na = s.item(x).name, nb = s.item(y).name;
        boolean names = na.equals(nb), equal = a.content ? inside == null : names && own == null && inside == null;
        if (equal) {
            found[0] = a.content ? Words.DbCmd.REL_SEPARATE + ", " + Words.DbCmd.REL_CONTENT_EQUAL : Words.DbCmd.REL_EQUAL;
            return !exact;
        }
        String why = a.content ? inside : !names ? Words.DbCmd.relNames(na, nb) + (inside == null && own == null ? " · " + Words.DbCmd.REL_CONTENT_EQUAL : "")
                : own != null ? own : inside;
        boolean similar = !a.content && (names || (inside == null && own == null));        // חלק שווה: השם, או כל התוכן
        found[0] = (exact ? Words.DbCmd.REL_SEPARATE + " · " : "") + why + (similar ? Words.DbCmd.REL_SIMILAR : "");
        return false;
    }

    /** מ0.18: ודא "קובץ" קיים / חסר — בכונן (גם מה שהשורות שלפני ייצרו, לפי ההדמיה של Batch). */
    static void planVerifyFile(final Batch.Step s, final File f, List<String> conds) {
        Boolean want = null;
        int count = -1;
        String has = null;
        boolean index = false;
        int trash = -2;
        for (int i = 0; i < conds.size(); i++) {
            String t = conds.get(i).trim();
            if (t.equals(W_IN_INDEX) && conds.size() == 1) index = true;          // מ0.42: ודא "<קובץ>" באינדקס
            else if (t.equals(Words.DbCmd.W_IN_TRASH) && conds.size() == 1) trash = -1;   // מ0.45: ודא "<קובץ>" בסל · /בסל:N
            else if (t.startsWith(Words.DbCmd.SW_IN_TRASH) && conds.size() == 1) {
                try { trash = Integer.parseInt(t.substring(Words.DbCmd.SW_IN_TRASH.length()).trim()); } catch (NumberFormatException e) { trash = -3; }
                if (trash < 0) { s.error = Words.DbCmd.VERIFY_USAGE; return; }
            }
            else if (t.equals(Words.DbCmd.WORD_EXISTS) || t.equals(Words.DbCmd.WORD_EXISTS_M)) want = Boolean.TRUE;
            else if (t.equals(Words.DbCmd.WORD_MISSING) || t.equals(Words.DbCmd.WORD_MISSING_M)) want = Boolean.FALSE;
            else if (t.startsWith(SW_FILES_IN)) {                            // מ0.37: /קבצים:N — כמה קבצים בתיקייה (בלי תתי־תיקיות)
                try { count = Integer.parseInt(t.substring(SW_FILES_IN.length()).trim()); } catch (NumberFormatException e) { count = -1; }
                if (count < 0) { s.error = Words.DbCmd.VERIFY_USAGE; return; }
            } else if (t.equals(W_CONTAINS) && i + 1 < conds.size()) has = conds.get(++i);   // מ0.37: מכיל "טקסט"
            else { s.error = Words.DbCmd.VERIFY_USAGE; return; }
        }
        if (index) {
            planVerifyIndex(s, f);
            return;
        }
        if (trash > -2) {
            planVerifyTrash(s, f, trash);
            return;
        }
        final boolean exists = want == null || want;
        final int n = count;
        final String text = has;
        final String what = "\"" + f.getName() + "\" " + (n >= 0 ? Words.DbCmd.verifyFilesIn(n) : text != null ? Words.DbCmd.verifyContains(text)
                : exists ? Words.DbCmd.WORD_EXISTS_M : Words.DbCmd.WORD_MISSING_M);
        s.target = f.getName();
        s.describe = Words.DbCmd.verifyWill(what);
        s.op = new ListCmd.Op() {
            @Override
            public String run(Library lib) {
                boolean there = f.isFile() || f.isDirectory();                   // מ0.37: גם תיקייה
                boolean ok;
                String got;
                if (n >= 0) {
                    int c = 0;
                    File[] k = f.listFiles();
                    if (k != null) for (File x : k) if (x.isFile()) c++;
                    ok = f.isDirectory() && c == n;
                    got = f.isDirectory() ? Words.DbCmd.verifyFilesIn(c) : Words.DbCmd.WORD_MISSING_M;
                } else if (text != null) {
                    String body = null;
                    try {
                        if (f.isFile()) body = Files.readUtf8(f);
                    } catch (Throwable t) {
                        Log.e("DbListCmd.verifyFile", t);
                    }
                    ok = body != null && body.contains(text);
                    got = body == null ? Words.DbCmd.WORD_MISSING_M : ok ? Words.DbCmd.verifyContains(text) : Words.DbCmd.VERIFY_NOT_CONTAINS;
                } else {
                    ok = there == exists;
                    got = there ? Words.DbCmd.WORD_EXISTS_M : Words.DbCmd.WORD_MISSING_M;
                }
                s.verified = ok;
                return Words.DbCmd.verifyResult(ok, what, got);
            }
        };
    }

    /** מ0.37: "ודא "<תיקייה>" /קבצים:N" · "ודא "<קובץ>" מכיל "<טקסט>"". */
    static final String SW_FILES_IN = "/קבצים:", W_CONTAINS = "מכיל";   // תחביר
    /** מ0.42: "ודא "<קובץ>" באינדקס" — מה שהיה "בדיקת מסלול באינדקס" בהגדרות. */
    static final String W_IN_INDEX = "באינדקס";   // תחביר
    /** מבנה 87–88: ודא ידנית "א" זהה ידנית "ב" (אותה רשימה) · ודא ידנית "א" שווה ידנית "ב" (רשימה אחרת, שווה). */
    static final String W_SAME = "זהה", W_EQUAL = "שווה";   // תחביר

    /** מ0.42: האם האינדקס מכיר את הקובץ (ובכונן — קיים?). בהרצה — מול האינדקס של אותו רגע, אחרי שהתור שלו התרוקן. */
    private static void planVerifyIndex(final Batch.Step s, final File f) {
        final String what = Words.DbCmd.verifyInIndex(f.getName());
        s.target = f.getName();
        s.describe = Words.DbCmd.verifyWill(what);
        s.op = new ListCmd.Op() {
            @Override
            public String run(Library lib) {
                if (lib != null) lib.flush(5000);                                  // השינויים שבתור (כתוב, העתק…) — נכנסים קודם
                boolean in = lib != null && lib.find(f.getAbsolutePath()) != null;
                s.verified = in;
                return Words.DbCmd.verifyResult(in, what, Words.DbCmd.indexGot(in, f.isFile()));
            }
        };
    }

    /** מ0.45: "ודא "<קובץ>" בסל" — יש בסל משהו שנמחק מהמסלול הזה; "/בסל:N" — בדיוק N. */
    private static void planVerifyTrash(final Batch.Step s, final File f, final int n) {
        final String what = Words.DbCmd.verifyInTrash(f.getName(), n);
        s.target = f.getName();
        s.describe = Words.DbCmd.verifyWill(what);
        s.op = new ListCmd.Op() {
            @Override
            public String run(Library lib) {
                if (lib != null) lib.flush(5000);
                int c = lib == null ? 0 : SmallCmds.trashed(lib, f.getAbsolutePath(), false).size();
                boolean ok = n < 0 ? c > 0 : c == n;
                s.verified = ok;
                return Words.DbCmd.verifyResult(ok, what, Words.DbCmd.trashGot(c));
            }
        };
    }

    /**
     * מ0.42: "ודא מסד" — מה שהיה חלק 11 של דוח האבחון: המנוע של המסד מול ה־SQLite של המכשיר (StoreContract ו־BackupContract,
     * על מסדים זמניים בצד), ותקינות המסד של כל אופן (לקריאה בלבד). לא נוגע במסדים — ולכן גם לא בהדמיה.
     */
    static void planVerifyDb(final Batch.Step s) {
        if (Db.app == null) { s.error = Words.DbCmd.VERIFY_DB_ONLY; return; }
        final String what = Words.DbCmd.VERIFY_DB_WHAT;
        s.describe = Words.DbCmd.verifyWill(what);
        s.op = new ListCmd.Op() {
            @Override
            public String run(Library lib) {
                android.content.Context c = Db.app;
                boolean[] ok = {false};
                String engine = Db.selfTestText(c, ok).trim().replace("\n", " · ");
                List<String> bad = new ArrayList<String>();
                int good = 0;
                for (String m : Db.dbModes(c)) {
                    SqlStore st = null;
                    try {
                        st = Db.openRead(c, m);
                        String chk = st.check();
                        if (chk == null) good++;
                        else bad.add(m + ": " + chk);
                    } catch (Throwable t) {
                        Log.e("DbListCmd.verifyDb", t);
                        bad.add(m + ": " + t.getMessage());
                    } finally {
                        if (st != null) st.close();
                    }
                }
                boolean all = ok[0] && bad.isEmpty();
                s.verified = all;
                return Words.DbCmd.verifyDbResult(all, engine, good, bad);
            }
        };
    }

    /**
     * מ0.42: "צור אופן "<שם>"" — אופן חדש: מסד עם בית (כמו "אופן חדש" בתפריט האופנים). קיים — בלי שינוי. ההדמיה מכירה אותו
     * מהשורה הזו: שורת "אופן" שאחריה עובדת, על מסד ריק בזיכרון. המשתמש (על הבדיקות): "סתם מצטבר שם שאריות".
     */
    /** מבנה 123: במסד של האופן יש רשימות (או רשימות חכמות) ואין בית. */
    private static boolean noHome(String mode) {
        try {
            return Db.read(Db.app, mode, new Db.Use<Boolean>() {
                @Override public Boolean run(SqlStore s) { return DbPaths.home(s) == 0 && !(s.lists().isEmpty() && s.smarts().isEmpty()); }
            });
        } catch (Throwable t) {
            Log.e("DbListCmd.noHome", t);
            return false;
        }
    }

    static void planMode(final Batch.Step s, String raw, final boolean fromBackup, Sim sim) {
        final String name = raw == null ? "" : raw.trim();
        if (name.isEmpty() || !name.equals(Names.cleanName(name).replaceAll("[:*?\"<>|\n\t]", " ").trim())) {
            s.error = Words.DbCmd.modeBadName(name);
            return;
        }
        if (!DbVerbs.active()) { s.error = Words.DbCmd.VERIFY_DB_ONLY; return; }
        s.target = name;
        // מבנה 47: אופן ש"בטל אופן" שלפני ביטל — אינו "קיים" (עד כאן: "צור אופן" אחריו לא עשה דבר, ו"אופן" אחריהם נכשל בתכנון)
        if ((Db.dbModes(Db.app).contains(name) && !sim.dropped.contains(name)) || sim.created.contains(name)) {
            // מבנה 123: אופן שיש במסד שלו רשימות ואין בו בית (מבנה 56) — "צור אופן" מתקן: הבית נוצר, והרשימות נכנסות אליו. עד כאן
            // תיקן רק תפריט האופנים שבמסך, בעצמו. מסד ריק — לא נוגעים.
            if (!sim.created.contains(name) && noHome(name)) {
                s.describe = Words.Cmd.modeRepairWill(name);
                s.op = new ListCmd.Op() {
                    @Override public String run(Library lib) {
                        if (Db.repairHome(Db.app, name) != 0) return Words.Cmd.modeRepaired(name);
                        lastFailed = true;
                        return Words.DbCmd.modeFailed(name);
                    }
                };
                return;
            }
            s.noop = true;
            s.describe = Words.DbCmd.modeExists(name);
            return;
        }
        // מבנה 48 (המשתמש: "להודיע שקיים גיבוי של אופן באותו שם. האם להמשיך את האופן הישן מהגיבוי, או לפתוח בשם אחר"): בקובץ פקודות
        // אין את מי לשאול — בלי /מגיבוי השורה היא שגיאה שאומרת את שתי הדרכים. אופן ששורה קודמת מבטלת — הגיבוי שלו ייווצר רק
        // בהרצה, ולכן שם זה נבדק בהרצה.
        final boolean has = !sim.dropped.contains(name) && Db.modeState(Db.app, name) == Db.MODE_HAS_BACKUP;
        if (has && !fromBackup) { s.error = Words.DbCmd.modeHasBackup(name); return; }
        sim.dropped.remove(name);
        sim.created.add(name);
        MemStore m = new MemStore(IdCounter.memory());
        DbPaths.ensureHome(m);
        sim.dbs.put(name, m);
        s.describe = has ? Words.DbCmd.modeResumeWill(name) : Words.DbCmd.modeWill(name);
        s.op = new ListCmd.Op() {
            @Override
            public String run(Library lib) {
                int st = Db.modeState(Db.app, name);
                if (st == Db.MODE_EXISTS) return Words.DbCmd.modeExists(name);
                if (st == Db.MODE_HAS_BACKUP) {
                    if (!fromBackup) { lastFailed = true; return Words.DbCmd.modeHasBackup(name); }
                    try {
                        Backups.Entry e = Db.resume(Db.app, name);
                        if (e != null) return Words.WorkModes.resumed(name, Db.when(e));
                        lastFailed = true;
                        return Words.DbCmd.modeNoGoodBackup(name);
                    } catch (Throwable t) {
                        Log.e("DbListCmd.planMode: resume", t);
                        lastFailed = true;
                        return Words.Db.failed(String.valueOf(t.getMessage()));
                    }
                }
                if (Db.prepare(Db.app, name, Reserved.FILES) != 0) return Words.DbCmd.modeCreated(name);
                lastFailed = true;
                return Words.DbCmd.modeFailed(name);
            }
        };
    }

    /**
     * מ0.45: "בטל אופן "<שם>"" (נבנה בשם "מחק אופן", שעדיין נקרא) — המסד של האופן נמחק (עם קובצי היומן שלו). יש בו משהו — גיבוי לפני (לא נשמר — לא נמחק).
     * לא: האופן הפתוח במסך, "שמור לתוכנה", והאופן שהקובץ עובד בו עכשיו. אין אופן כזה — בלי שינוי (ניקוי שאפשר להריץ שוב).
     * ההדמיה: "אופן" אחריו — שגיאה.
     * מבנה 59 (המשתמש, 14.11.2026: "כל עוד האופן קיים יש צורך בגיבוי שלו. כאשר הוא נמחק אין צורך לגיבוי. אם ישתנה בהמשך נראה"):
     * תיקיית הגיבויים של האופן עוברת לסל (לא לצמיתות — כמו תיקיית ההגדרות שלו), ולכן "צור אופן" באותו שם פותח אופן חדש וריק,
     * בלי שאלה. מי שמתחרט — משחזר את התיקייה מהסל, ואז "צור אופן" באותו שם מציע להמשיך מהגיבוי (מבנה 48). אופן שכבר אינו
     * ונשארו ממנו גיבויים (מגרסה קודמת) — "בטל אופן" שולח גם אותם לסל.
     */
    static void planDropMode(final Batch.Step s, final String name, Sim sim) {
        if (!DbVerbs.active()) { s.error = Words.DbCmd.VERIFY_DB_ONLY; return; }
        s.target = name;
        if (!sim.hasMode(name)) {
            if (!sim.dropped.contains(name) && !name.equals(Db.MENUS_MODE) && Db.modeState(Db.app, name) == Db.MODE_HAS_BACKUP) {
                sim.dropped.add(name);                                          // מבנה 59: "צור אופן" שאחריו — נבדק בהרצה (הגיבויים כבר לא יהיו)
                s.describe = Words.DbCmd.dropModeLeftWill(name);
                s.op = new ListCmd.Op() {
                    @Override
                    public String run(Library lib) {
                        if (Db.dbFile(Db.app, name).isFile()) return Words.DbCmd.dropModeNone(name);   // נוצר בינתיים — לא נוגעים
                        int bk = backupsToTrash(lib, name);
                        if (bk < 0) lastFailed = true;
                        return bk == 0 ? Words.DbCmd.dropModeNone(name) : Words.DbCmd.dropModeLeft(name, bk > 0);
                    }
                };
                return;
            }
            s.noop = true;
            s.describe = Words.DbCmd.dropModeNone(name);
            return;
        }
        if (name.equals(Db.MENUS_MODE)) { s.error = Words.DbCmd.dropModeSystem(name); return; }
        if (Db.inUse(name) || name.equals(sim.mode)) { s.error = Words.DbCmd.dropModeOpen(name); return; }   // מבנה 63: גם אופן שמסך המסד מחזיק
        sim.created.remove(name);
        sim.dropped.add(name);
        sim.dbs.remove(name);
        s.describe = Words.DbCmd.dropModeWill(name);
        s.op = new ListCmd.Op() {
            @Override
            public String run(Library lib) {
                android.content.Context c = Db.app;
                File f = Db.dbFile(c, name);
                if (!f.isFile()) {                                               // מבנה 59: אין מסד — ואם נשארו גיבויים, לסל
                    int left = backupsToTrash(lib, name);
                    if (left < 0) lastFailed = true;
                    return left == 0 ? Words.DbCmd.dropModeNone(name) : Words.DbCmd.dropModeLeft(name, left > 0);
                }
                if (Db.inUse(name)) { lastFailed = true; return Words.DbCmd.dropModeOpen(name); }
                boolean kept = false;
                try {
                    kept = Db.write(c, name, null, new Db.Use<Boolean>() {
                        @Override
                        public Boolean run(SqlStore st) throws Exception {
                            long home = DbPaths.home(st);
                            boolean empty = st.lists().isEmpty() && st.smarts().isEmpty() && st.unassigned().isEmpty()
                                    && (home == 0 || st.members(home).isEmpty());
                            if (empty) return false;
                            Db.backupsOf(c, name).before(st, Backups.OP_DROP_MODE, System.currentTimeMillis());   // רק למסד שיש בו משהו
                            return true;
                        }
                    });
                } catch (Throwable t) {
                    Log.e("DbListCmd.dropMode", t);
                    lastFailed = true;
                    return Words.DbCmd.dropModeFailed(name, Words.DbCmd.DROP_MODE_NO_BACKUP);
                }
                if (!android.database.sqlite.SQLiteDatabase.deleteDatabase(f) && f.exists()) {
                    lastFailed = true;
                    return Words.DbCmd.dropModeFailed(name, Words.Db.failed(f.getName()));
                }
                if (name.equals(Db.savedMode(c))) Db.saveMode(c, "");   // הפתיחה הבאה — באופן אחר
                // מ0.45+ (claude/bitul-ofen.md, המשתמש: "למה להשאיר? בלי אופן היא סתם בלבול"): תיקיית ההגדרות שלו — לסל (לא לצמיתות)
                boolean folder = false;
                String note = "";
                File dir = Modes.dirOf(name);
                if (dir.isDirectory() && !name.equals(Modes.current()) && lib != null) {
                    Physical.Result r = Physical.deleteOwn(lib, dir);            // מבנה 64 (ממצא 45): עד כאן המחיקה הרגילה סירבה — "זו תיקיית התוכנה"
                    folder = r.failed == 0 && !r.done.isEmpty();
                    if (!folder) {
                        note = Words.DbCmd.DROP_MODE_FOLDER_KEPT;
                        Log.i("ביטול אופן: תיקיית ההגדרות לא עברה לסל — " + dir + " · " + r.note);
                    }
                }
                int bk = backupsToTrash(lib, name);                              // מבנה 59: אופן שבוטל — אין צורך בגיבויים שלו
                if (bk < 0) note += Words.DbCmd.DROP_MODE_BACKUPS_KEPT;
                Log.i("בוטל אופן: " + name + (kept ? " (גיבוי לפני)" : " (ריק)") + (bk > 0 ? " · הגיבויים לסל" : "") + (folder ? " · התיקייה לסל" : ""));
                return Words.DbCmd.dropModeDone(name, bk > 0, folder) + note;
            }
        };
    }

    /**
     * מבנה 59: תיקיית הגיבויים של אופן שבוטל — לסל של הכונן (שחזור מהסל מחזיר את כולה). 0 — אין תיקייה כזו; 1 — עברה;
     * ‎-1 — לא עברה (נשארת במקומה, ו"צור אופן" באותו שם יגיד שיש גיבוי).
     */
    private static int backupsToTrash(Library lib, String name) {
        File dir = Db.backupsDir(name);
        if (!dir.isDirectory()) return 0;
        if (lib == null) {
            Log.i("ביטול אופן: הגיבויים לא עברו לסל (אין אינדקס) — " + dir);
            return -1;
        }
        Physical.Result r = Physical.deleteOwn(lib, dir);                         // מבנה 64: התיקייה של המסד מוגנת — זו מחיקה של התוכנה עצמה
        if (r.failed == 0 && !r.done.isEmpty()) return 1;
        Log.i("ביטול אופן: הגיבויים לא עברו לסל — " + dir + " · " + r.note);
        return -1;
    }

    /** העותק בזיכרון — פעם אחת לכל קובץ פקודות (השורות הבאות רואות מה שהקודמות עשו). */
    private static Store sim(Sim sim, String mode) {
        if (sim.db != null) return sim.db;
        SqlStore real = Db.openRead(Db.app, mode);
        try {
            sim.db = copyOf(real);
        } finally {
            real.close();
        }
        return sim.db;
    }

    private static Finder finder(final Sim sim) {
        return over(sim.lib, sim.condFiles);
    }

    /** הקבצים לפי תנאי מהאינדקס — חיפוש אחד לכל תנאי (cache). */
    static Finder over(final Library lib, final Map<String, List<String>> cache) {
        return new Finder() {
            @Override
            public List<String> find(String cond) {
                if (cond == null || cond.trim().isEmpty()) return new ArrayList<String>();
                if (lib == null || !lib.ready) return null;
                List<String> had = cache.get(cond);
                if (had != null) return had;
                List<String> out = new ArrayList<String>();
                for (Library.Hit h : lib.list(cond).hits) if (h.rec != null) out.add(h.path());   // מבנה 94: "קבצי חכמה" — רק מה שעונה במלואו
                cache.put(cond, out);
                return out;
            }
        };
    }

    /** מבנה 58: עריכה מהמסך שדורסת את מה שהיה (שם, תיאור, תנאי של רשימה חכמה) — גיבוי "לפני עריכה", ואז הפעולה. */
    static String nowEdit(String mode, Work w) {
        Res r = realRes(mode, w, Backups.OP_EDIT, null);
        if (r.error != null) lastFailed = true;
        return r.error != null ? r.error : Words.DbCmd.inDb(r.text);
    }

    /** ההרצה: המסד נפתח לרגע; לפני פעולה שמוציאה — גיבוי "לפני פעולה" (נכשל — לא מבצעים). */
    /** מ0.37: "גבה" — גיבוי ידני של המסד של האופן (עותק וגם טקסט); גיבוי ידני לא נמחק לבד. */
    static String backupNow(final String mode) {
        try {
            return Db.write(Db.app, mode, null, new Db.Use<String>() {
                @Override
                public String run(SqlStore s) throws Exception {
                    List<Backups.Entry> made = Db.backupsOf(Db.app, mode).manual(s, System.currentTimeMillis());
                    StringBuilder names = new StringBuilder();
                    for (Backups.Entry e : made) names.append(names.length() > 0 ? ", " : "").append(e.name);
                    return Words.DbCmd.backedUp(mode, names.toString());
                }
            });
        } catch (Throwable t) {
            Log.e("DbListCmd.backupNow", t);
            lastFailed = true;
            return Words.Db.failed(String.valueOf(t.getMessage()));
        }
    }

    /** מ0.31: הפעולה האחרונה במסד נכשלה — ההרצה סופרת כישלונות ("המשך"). */
    static volatile boolean lastFailed;

    private static String real(String mode, Work w, boolean before, Library lib) {
        Res r = realRes(mode, w, before, lib);
        if (r.error != null) lastFailed = true;
        return r.error != null ? r.error : Words.DbCmd.inDb(r.text);
    }

    private static Res realRes(String mode, Work w, boolean before, Library lib) {
        return realRes(mode, w, before ? Backups.OP_DELETE : null, lib);
    }

    private static Res realRes(final String mode, final Work w, String beforeOp, final Library lib) {
        try {
            return Db.write(Db.app, mode, beforeOp, new Db.Use<Res>() {   // מבנה 33: דרך שער המסד
                @Override
                public Res run(SqlStore s) {
                    Res r = w.run(s, over(lib, new HashMap<String, List<String>>()));
                    if (r.error == null && r.renamed != 0) follow(s, mode, r);
                    return r;
                }
            });
        } catch (Db.BeforeFailed b) {
            Log.e("DbListCmd.before", b.getCause());
            return Res.err(Words.DbCmd.beforeFailed(String.valueOf(b.getCause().getMessage())));
        } catch (Throwable t) {
            Log.e("DbListCmd.real", t);
            return Res.err(Words.Db.failed(String.valueOf(t.getMessage())));
        }
    }

    /** מ0.42 (ב8): אחרי שינוי שם — קובצי הפקודות שפונים לרשימה מתעדכנים, וההודעה אומרת כמה ואילו. תקלה — נאמרת, לא נבלעת. */
    private static void follow(Store s, String mode, Res r) {
        try {
            List<File> fs = Shortcuts.listRenamed(s, mode, r.renamed, r.renamedFrom, r.renamedTo);
            if (fs.isEmpty()) return;
            List<String> names = new ArrayList<String>();
            for (File f : fs) names.add(Text.stem(f.getName()));
            r.text = (r.text == null ? "" : r.text) + Words.DbCmd.followed(names);
        } catch (Throwable t) {
            Log.e("DbListCmd.follow", t);
            r.text = (r.text == null ? "" : r.text) + Words.DbCmd.FOLLOW_FAILED;
        }
    }

    /**
     * מ0.25: תיקיית היעד של קובץ כשמבקשים /מסלול מרשימה — היעד + המסלול המלא של התיקייה שלו בכונן (בלי שורש הכונן):
     * /storage/emulated/0/Download/א/ב.pdf ← <יעד>/Download/א. כרטיס: /storage/XXXX-XXXX/… — אותו דבר. מסלול אחר — כולו.
     */
    /**
     * הקבצים שברשימה (ידנית, חכמה, "קבצי …") — אל תיקייה בכונן: העתקה, ומ0.40 גם העברה (המשתמש, על התוספים: "המריץ מעביר את
     * כולם, ואם צריך משנה שם"). הביצוע — הרגיל של הכונן (Batch.run); כאן רק התכנון, קובץ אחרי קובץ על ההדמיה.
     */
    private static void toFolder(Batch.Step s, String cmd, ListCmd.Side a, Store db, String mode, Finder find, List<String> sw,
                                 Sim sim, File fsDest) {
        if (fsDest == null || !sim.isDir(fsDest)) { s.error = Words.Cmd.NO_DEST; return; }
        final boolean copy = cmd.equals("COPY");
        String[] err = {null};
        List<String> fs = files(db, mode, a, find, err);
        if (fs == null) { s.error = err[0]; return; }
        s.dest = fsDest;
        // מ0.26: המתגים של שורת רשימה לא נקראים ב־Batch.plan (שם רק שורות כונן) — נקראים כאן. במ0.25 זה הוחמץ,
        // ולכן התיקון לא פעל במכשיר ("הועתקו 1" במקום "דולגו", והקובץ ישר בתיקייה).
        for (String x : sw) {
            String u = x.toUpperCase(java.util.Locale.ROOT);
            if (u.equals("/Y") || x.equals(Syntax.SW_TAKEN + Syntax.TAKEN_REPLACE)) s.replace = true;
            if (x.equals(Syntax.SW_TAKEN + Syntax.TAKEN_SKIP)) s.skipTaken = true;
            if (x.equals(Syntax.SW_TAKEN + Syntax.TAKEN_STOP)) s.failTaken = true;
            if (x.equals(Syntax.SW_TAKEN + Syntax.TAKEN_RENAME)) s.renameTaken = true;
            if (x.startsWith(Syntax.SW_TAKEN) && !Parse.takenValue(x)) { s.error = Parse.takenUnknown(x); return; }
            if (u.equals("/S") || x.equals(Syntax.SW_KEEP)) s.recursive = true;
        }
        Filter flt = Filter.of(sw);                                   // מ0.40: /מכיל:
        if (flt.error != null) { s.error = flt.error; return; }
        if (!flt.contains.isEmpty()) s.contains = flt.contains;
        // מ0.25 (באג 4): /תפוס:לדלג ו־/מסלול גם מרשימה לתיקייה. /מסלול — המסלול המלא של הקובץ מהכונן, בתוך היעד
        // (המשתמש: "אם הבקשה עם מסלול אז עם כל המסלול"); לכל קובץ תיקיית יעד משלו (targets, כמו בשחזור).
        for (String x : fs) {
            File src = new File(x);
            if (!sim.isFile(src)) continue;
            s.total++;
            if (!flt.ok(sim.real(src))) continue;
            File dir = s.recursive ? fullPathUnder(fsDest, src) : fsDest;
            if (!copy && dir.equals(src.getParentFile())) { s.inPlace++; continue; }   // מ0.40: כבר שם — כלום
            File to = new File(dir, src.getName());
            if (s.drop(sim, src, to)) continue;
            s.files.add(src.getAbsolutePath());
            if (s.recursive) {
                s.targets.add(dir.getAbsolutePath());
                for (File up = dir; up != null && !up.equals(fsDest); up = up.getParentFile()) sim.dirs.add(up.getAbsolutePath());
            }
            sim.move(src.getAbsolutePath(), s.arrive(sim, to), copy);
        }
        int passed = s.files.size() + s.skipped + s.same + s.inPlace;
        s.describe = Words.Cmd.transfer(copy, Words.files(s.files.size()), Bidi.path(fsDest.getAbsolutePath()))
                + (s.recursive ? Words.Cmd.KEEP_PATH : "") + (s.total > passed ? Words.Cmd.filtered(passed, s.total) : "") + Batch.notes(s);
        s.noop = s.files.isEmpty() && s.same + s.inPlace > 0;
    }

    static File fullPathUnder(File dest, File src) {
        String p = src.getParentFile() == null ? "" : src.getParentFile().getAbsolutePath();
        String rel = p;
        if (p.startsWith("/storage/emulated/")) {
            int e = p.indexOf('/', "/storage/emulated/".length());
            rel = e < 0 ? "" : p.substring(e + 1);
        } else if (p.startsWith("/storage/")) {
            int e = p.indexOf('/', "/storage/".length());
            rel = e < 0 ? "" : p.substring(e + 1);
        }
        while (rel.startsWith("/")) rel = rel.substring(1);
        return rel.isEmpty() ? dest : new File(dest, rel);
    }
}
