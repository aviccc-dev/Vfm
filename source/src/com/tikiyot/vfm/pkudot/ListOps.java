package com.tikiyot.vfm;

// מבנה 170 (סידור תיקיית הפקודות): הפעולות של הרשימות על המסד — צור, הסר, העתק / העבר, שנה שם, קשר, יצוא / יבוא, העתקה
// בין אופנים, השוואה, דוגמאות. עד כאן ישב בתוך DbListCmd. אחראי: לעשות את הפעולה על המסד שנמסר לו. אסור לו: להחליט מתי.
// (לפי חמש הקומות מקומן בחומר; הן נשענות על ListCmd.Side ועל Res שבקומת הפקודות — ולכן כאן, עד שיוחלט אחרת.)

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;

final class ListOps {

    private ListOps() { }

    /** MD ידנית "א\ב": קיימת — בלי שינוי; חסרה — נוצרת בכל הדרך (חלק ראשון — בבית). */
    /** מבנה 102: "צור תיקייה ידנית" — הכלל האחד של רשימה חדשה (Lists.newListAt): שם קיים — סירוב (עד כאן: "בלי שינוי"). */
    static DbListCmd.Res md(Store s, String mode, ListCmd.Side a) {
        return md(s, mode, a, Lists.TAKEN_STOP);
    }

    /** מבנה 109: taken — `/תפוס:לדלג` (קיימת: ממשיכים), `/תפוס:לשנות-שם` ("(2)"); בלי — סירוב. */
    static DbListCmd.Res md(Store s, String mode, ListCmd.Side a, int taken) {
        Lists.Made m = Lists.newListAt(s, mode, a.path, taken);
        return listMade(m, a.label());
    }

    /** מבנה 109: ההודעה לפי מה שקרה ברשימה חדשה. */
    static DbListCmd.Res listMade(Lists.Made m, String label) {
        if (m.error != null) return DbListCmd.Res.err(m.error);
        if (m.skipped) return DbListCmd.Res.same(Words.ListCmdW.listSkipped(label));
        DbListCmd.Res ok = DbListCmd.Res.ok(Words.ListCmdW.created(m.renamed ? m.name : label));
        ok.made = m.id;
        return ok;
    }

    /** מבנה 109: "/תפוס:<ערך>" מתוך המתגים של השורה. */
    static int takenOf(List<String> sw) {
        for (String x : sw) if (x.startsWith(Syntax.SW_TAKEN)) return DbListCmd.taken(x.substring(Syntax.SW_TAKEN.length()));
        return Lists.TAKEN_STOP;
    }

    /** RD / DEL על רשימה: רשימה, חכמה, תוכן, או קבצים לפי תבנית — יוצאים מהרשימות (הקבצים בכונן לא נוגעים). */
    static DbListCmd.Res remove(Store s, final String mode, final ListCmd.Side a) {
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
            return n[0] == 0 ? DbListCmd.Res.same(Words.DbCmd.LOOSE_NONE) : DbListCmd.Res.ok(Words.DbCmd.looseRemoved(n[0]));
        }
        if (a.kind == ListCmd.SMART_LIST) {
            final long[] r = DbListCmd.smart(s, mode, a);
            if (r == null) return DbListCmd.Res.err(DbListCmd.smartWhy(s, mode, a));
            s.atomic(new Store.Work() {
                @Override public void run(Store t) {
                    if (r[0] != 0) t.removeMember(r[0], r[1]);
                    Lists.dropIfAlone(t, r[1]);
                }
            }, true);
            return DbListCmd.Res.ok(Words.DbCmd.removed(Words.ListCmdW.smartList(a.label())));
        }
        if (a.home && a.mask == null && !a.content) return DbListCmd.Res.err(Words.ListCmdW.HOME_NO_REMOVE);
        final long id = DbListCmd.list(s, mode, a);
        if (id == 0) return DbListCmd.Res.same(Words.DbCmd.notThere(a.label()));
        final int[] n = {0};
        if (a.mask != null || a.content) {
            s.atomic(new Store.Work() {
                @Override public void run(Store t) {
                    for (Store.Member m : t.members(id)) {
                        if (a.mask != null) {
                            String p = DbListCmd.pathOf(t, m.item);
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
            return DbListCmd.Res.ok(Words.DbCmd.removed(what));
        }
        long parent = parentOf(s, mode, a, id);
        if (parent == -1) return DbListCmd.Res.err(Words.DbCmd.needFullWay(a.label()));
        Lists.removeFrom(s, parent, id);
        return DbListCmd.Res.ok(Words.DbCmd.removed(Words.ListCmdW.list(a.label())));
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
    static DbListCmd.Res addFiles(Store s, long dest, List<String> ps, String what, String label) {
        DbFiles.Added r = DbFiles.add(s, dest, ps, null, !(s instanceof MemStore));   // מ0.15: חתימה — בהרצה, לא בהדמיה
        return DbListCmd.Res.ok(what + " — " + Words.ListCmdW.copied(r.added, ps.size() - r.added, ps.size() > r.added ? Words.DbCmd.ALREADY : null, label) + nowIn(s, dest));
    }

    /**
     * מבנה 115: הכלל האחד של "תיקייה מהכונן אל רשימה" — לשורה הכתובה ולמסך (➕, "לרשימה", הדבקה). שלוש צורות, כמו במסך:
     * בלי מתג — התיקייה עצמה: רשימה בשמה, ובתוכה התת־תיקיות כרשימות והקבצים (DbMore.attach); /קבצים — רק הקבצים, ישר לרשימה;
     * /חיה — רשימה חכמה של התיקייה (DbMore.liveFolder; אותה תיקייה כבר שם — לא נכפלת). **שם קיים** (רשימה, או רשימה חכמה אחרת,
     * בשם של התיקייה) — כמו בכל מקום (ה1, ה2): עוצרים, אלא אם /תפוס:לשנות-שם ("(2)") או /תפוס:לדלג. קודם בודקים הכול, ורק אז כותבים.
     * nodes — התיקיות שנסרקו (לא ב־/חיה), באותו סדר של dirs. files — קבצים שנבחרו לצד התיקיות (מהמסך).
     */
    static DbListCmd.Res addFolders(Store s, long dest, String label, List<String> dirs, List<Tree.Node> nodes, List<String> files,
                          boolean live, boolean flat, int taken) {
        boolean[] skip = new boolean[dirs.size()];
        for (int i = 0; i < dirs.size() && !flat; i++) {
            String name = Names.cleanName(new File(dirs.get(i)).getName());
            String why = live ? (DbMore.liveThere(s, dest, dirs.get(i)) || !Lists.smartTaken(s, dest, name) ? null : Words.ListCmdW.smartTaken(name))
                    : Lists.nameError(s, java.util.Collections.singletonList(dest), 0, Store.LIST, name);
            if (why == null || taken == Lists.TAKEN_RENAME) continue;
            if (taken != Lists.TAKEN_SKIP) return DbListCmd.Res.err(why);
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
        DbListCmd.Res ok = DbListCmd.Res.ok(Words.ListCmdW.foldersAdded(label, lists, items, smarts, skipped) + nowIn(s, dest));
        ok.made = dest;
        return ok;
    }

    /** COPY: לרשימה ידנית (או לבית). fsFiles — קבצים מהכונן (כשהמקור אינו רשימה); הקבצים עצמם לתיקייה — ב-plan. */
    static DbListCmd.Res copy(Store s, String mode, ListCmd.Side a, ListCmd.Side b, final List<String> fsFiles, DbListCmd.Finder f) {
        final long dest = b.home ? DbPaths.home(s) : b.mask != null ? 0 : DbPaths.find(s, mode, b.path);
        if (dest == 0) return DbListCmd.Res.err(DbListCmd.listWhy(s, mode, b));
        if (!a.list() || a.filesOf) {                                        // מסלולים ← לרשימה
            List<String> paths = fsFiles;
            String what = Words.files(fsFiles == null ? 0 : fsFiles.size());
            if (a.filesOf) {
                String[] err = {null};
                paths = DbListCmd.files(s, mode, a, f, err);
                if (paths == null) return DbListCmd.Res.err(err[0]);
                what = Words.ListCmdW.filesOf(Words.files(paths.size()), a.label());
            }
            return addFiles(s, dest, paths, what, b.label());
        }
        final List<Store.Item> what = new ArrayList<Store.Item>();
        final List<String> self = new ArrayList<String>();
        String label;
        if (a.kind == ListCmd.SMART_LIST) {
            long[] r = DbListCmd.smart(s, mode, a);
            if (r == null) return DbListCmd.Res.err(DbListCmd.smartWhy(s, mode, a));
            what.add(s.item(r[1]));
            label = Words.ListCmdW.smartList(a.label());
        } else {
            long src = DbListCmd.list(s, mode, a);
            if (src == 0) return DbListCmd.Res.err(DbListCmd.listWhy(s, mode, a));
            if (a.home || a.content || a.mask != null) {
                for (Store.Member m : s.members(src)) {
                    if (a.mask != null) {
                        String p = DbListCmd.pathOf(s, m.item);
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
                if (src == dest || s.contains(src, dest)) return DbListCmd.Res.err(Words.ListCmdW.INTO_ITSELF);
                what.add(s.item(src));
                label = Words.ListCmdW.list(a.label());
            }
        }
        Lists.Copied c = Lists.copyInto(s, what, dest);
        String selfText = self.isEmpty() ? "" : Words.ListCmdW.notIntoSelf(self);
        return DbListCmd.Res.ok(Words.ListCmdW.copy(label, b.label()) + names(what) + selfText + " · "
                + Words.ListCmdW.copied(c.done, c.skipped, c.why, b.label()) + nowIn(s, dest));
    }

    static String nowIn(Store s, long list) {
        DbCount c = DbCount.of(s, list, null);
        return Words.ListCmdW.nowIn(c.lists, c.items);
    }

    static String names(List<Store.Item> items) {
        if (items.isEmpty()) return "";
        StringBuilder sb = new StringBuilder(" — ");
        for (int i = 0; i < Math.min(5, items.size()); i++) sb.append(i > 0 ? ", " : "").append('"').append(items.get(i).name).append('"');
        if (items.size() > 5) sb.append(" ").append(Words.andMore(items.size() - 5));
        return sb.toString();
    }

    /** מ0.21: REN ידנית/חכמה "א\ב" "שם חדש" — שם תפוס באותה רשימה (אותו סוג) — לא; "/" או "\" בשם — לא. */
    static DbListCmd.Res rename(Store s, String mode, final ListCmd.Side a, String name) {
        if (name.contains("/") || name.contains("\\")) return DbListCmd.Res.err(Words.DbCmd.BAD_NAME);   // לפני הניקוי (שמוחק אותם)
        final String n = a.kind == ListCmd.SMART_LIST ? name.trim() : Names.cleanName(name);
        if (n.isEmpty()) return DbListCmd.Res.err(Words.DbCmd.BAD_NAME);
        long id, parent;
        if (a.kind == ListCmd.SMART_LIST) {
            long[] r = DbListCmd.smart(s, mode, a);
            if (r == null) return DbListCmd.Res.err(DbListCmd.smartWhy(s, mode, a));
            id = r[1];
            parent = r[0];
        } else {
            id = DbPaths.find(s, mode, a.path);
            if (id == 0) return DbListCmd.Res.err(DbListCmd.listWhy(s, mode, a));
            List<String> up = new ArrayList<String>(a.path.subList(0, a.path.size() - 1));
            parent = up.isEmpty() ? DbPaths.home(s) : DbPaths.find(s, mode, up);
        }
        Store.Item it = s.item(id);
        if (n.equals(it.name)) return DbListCmd.Res.same(Words.DbCmd.renamed(a.label(), n));
        // מבנה 86: השם נבדק בכל הרשימות שהפריט נמצא בהן — כמו בעריכה שבמסך (Lists.edit). עד כאן נבדקה רק הרשימה שבדרך שנכתבה,
        // ורשימה שמשותפת לשתי רשימות יכלה לקבל שם שכבר תפוס בשנייה: שתי רשימות באותו שם באותה רשימה, ודרך שאינה חד־משמעית.
        List<Long> parents = new ArrayList<Long>(s.listsOf(id));
        if (!parents.contains(parent)) parents.add(parent);
        String e = Lists.nameError(s, parents, id, it.type, name);              // מ0.23: הבדיקה האחת
        if (e != null) return DbListCmd.Res.err(e);
        final long fid = id;
        s.atomic(new Store.Work() {
            @Override public void run(Store t) { t.rename(fid, n); }
        }, true);
        return DbListCmd.Res.ok(Words.DbCmd.renamed(a.label(), n)).renamed(fid, it.name, n);
    }

    /**
     * מ0.17: LINK (קשר) — אותו פריט גם ביעד (רשימה משותפת); MOVE (העבר) — נכנס ליעד ויוצא מהמקום שלו. המקור: רשימה
     * (מהרשימה שמעליה, או מהבית), חכמה, תוכן של רשימה, או קבצים לפי תבנית — כמו ב-COPY.
     */
    static DbListCmd.Res link(Store s, String mode, ListCmd.Side a, ListCmd.Side b, boolean move) {
        long dest = b.home ? DbPaths.home(s) : b.mask != null ? 0 : DbPaths.find(s, mode, b.path);
        if (dest == 0) return DbListCmd.Res.err(DbListCmd.listWhy(s, mode, b));
        List<Long> ids = new ArrayList<Long>();
        long from;
        String label;
        if (a.kind == ListCmd.SMART_LIST) {
            long[] r = DbListCmd.smart(s, mode, a);
            if (r == null) return DbListCmd.Res.err(DbListCmd.smartWhy(s, mode, a));
            ids.add(r[1]);
            from = r[0];
            label = Words.ListCmdW.smartList(a.label());
        } else {
            long src = DbListCmd.list(s, mode, a);
            if (src == 0) return DbListCmd.Res.err(DbListCmd.listWhy(s, mode, a));
            if (a.home || a.content || a.mask != null) {
                for (Store.Member m : s.members(src)) {
                    if (a.mask != null) {
                        String p = DbListCmd.pathOf(s, m.item);
                        if (p == null || !Parse.wild(a.mask, new File(p).getName())) continue;
                    }
                    ids.add(m.item.id);
                }
                from = src;
                label = a.mask != null ? Words.files(ids.size()) : a.home ? Words.DbCmd.homeContent(ids.size())
                        : Words.ListCmdW.contentOf(ids.size(), a.label());
            } else {
                if (src == dest || s.contains(src, dest)) return DbListCmd.Res.err(Words.ListCmdW.INTO_ITSELF);
                from = parentOf(s, mode, a, src);
                if (from == -1) return DbListCmd.Res.err(Words.DbCmd.needFullWay(a.label()));
                ids.add(src);
                label = Words.ListCmdW.list(a.label());
            }
        }
        Lists.Copied c = move ? Lists.moveTo(s, ids, from, dest) : Lists.linkInto(s, ids, dest);
        return DbListCmd.Res.ok((move ? Words.DbCmd.moved(label, b.label()) : Words.DbCmd.linked(label, b.label())) + " · "
                + Words.ListCmdW.copied(c.done, c.skipped, c.why, b.label()) + nowIn(s, dest));
    }

    /** מ0.18: היצוא — write=false (ההדמיה): רק השם והמספרים; write=true: נכתב. */
    static DbListCmd.Res export(Store s, String mode, ListCmd.Side a, ListCmd.Side b, DbListCmd.Finder f, boolean write) {
        long when = System.currentTimeMillis();
        DbExport.Out o;
        String name;
        if (a.filesOf || a.kind == ListCmd.SMART_LIST) {                     // מבנה 208: רשימה חכמה — מה שהיא מוצאת (קבצים), ישר לקובץ
            String[] err = {null};
            List<String> ps = DbListCmd.files(s, mode, a, f, err);
            if (ps == null) return DbListCmd.Res.err(err[0]);
            name = Words.DbCmd.exportFilesName(a.raw);
            o = DbExport.paths(ps, name, when);
        } else {
            long id = DbListCmd.list(s, mode, a);
            if (id == 0) return DbListCmd.Res.err(DbListCmd.listWhy(s, mode, a));
            name = a.home ? Words.DbCmd.exportModeName(mode) : Words.DbCmd.exportListName(s.item(id).name);
            o = DbExport.list(s, id, a.home, name, when);
        }
        File to = DbExport.target(b.raw, name, exportDir());
        if (write) {
            try {
                File dir = to.getParentFile();
                if (dir != null && !dir.isDirectory() && !dir.mkdirs()) return DbListCmd.Res.err(Words.CANT_CREATE_FOLDER);
                Files.writeAtomic(to, o.text);
            } catch (Throwable t) {
                Log.e("DbListCmd.export", t);
                return DbListCmd.Res.err(Words.Db.failed(String.valueOf(t.getMessage())));
            }
        }
        return DbListCmd.Res.ok(Words.DbCmd.exported(name, Bidi.path(to.getAbsolutePath()), o.lists, o.files, o.skipped));
    }

    /** מ0.22: קובץ הייבוא — שם בלי מסלול: בתיקיית היצוא; "\\" כמו "/". */
    static File importFile(String raw) {
        String r = raw == null ? "" : raw.trim().replace('\\', '/');
        File f = new File(r);
        return f.isAbsolute() ? f : new File(exportDir(), r);
    }

    /** מ0.22: ייבוא — הטקסט (בפורמט היצוא) לתוך הרשימה. */
    static DbListCmd.Res importFrom(Store s, String mode, File in, ListCmd.Side b) {
        long dest = b.home ? DbPaths.home(s) : DbPaths.find(s, mode, b.path);
        if (dest == 0) return DbListCmd.Res.err(DbListCmd.listWhy(s, mode, b));
        String text;
        try {
            text = Files.readUtf8(in);
        } catch (Throwable t) {
            return DbListCmd.Res.err(Words.DbCmd.cantRead(in.getName()));
        }
        DbMore.Imported r = DbMore.importText(s, dest, text);
        return DbListCmd.Res.ok(Words.DbCmd.imported(in.getName(), b.label(), r.lists, r.files, r.bad) + nowIn(s, dest));
    }

    /** מ0.22: העתקה מאופן אחר — המקור נפתח לקריאה בלבד; עותק עצמאי (קבצים — לפי המסלול). */
    static DbListCmd.Res copyFromMode(Store t, String mode, String srcMode, ListCmd.Side a, ListCmd.Side b) {
        long dest = b.home ? DbPaths.home(t) : DbPaths.find(t, mode, b.path);
        if (dest == 0) return DbListCmd.Res.err(DbListCmd.listWhy(t, mode, b));
        SqlStore src = null;
        try {
            src = Db.openRead(Db.app, srcMode);
            return copyAcross(src, srcMode, a, t, dest, b.label());
        } finally {
            if (src != null) src.close();
        }
    }

    /** מ0.22: הליבה של ההעתקה בין מסדים (נבדקת במחשב על שני מסדים). */
    static DbListCmd.Res copyAcross(Store src, String srcMode, ListCmd.Side a, Store t, long dest, String destLabel) {
        DbPaths.Found at = DbPaths.listAt(src, srcMode, a.path);                // מבנה 107: גם באופן אחר — ספק עוצר
        long id = at.error != null ? 0 : at.id;
        if (id == 0) return DbListCmd.Res.err(at.ambiguous ? at.error : Words.ListCmdW.noList(Words.DbCmd.MODE_PREFIX + srcMode + "/" + ListCmd.pathText(a.path)));
        int n = 0;
        if (a.content) for (Store.Member m : src.members(id)) n += DbMore.copyAcross(src, m.item.id, t, dest);
        else {
            for (Store.Member m : t.members(dest))
                if (m.item.type == Store.LIST && m.item.name.equals(src.item(id).name)) return DbListCmd.Res.err(Words.DbCmd.SAME_NAME);
            n = DbMore.copyAcross(src, id, t, dest);
        }
        return DbListCmd.Res.ok(Words.DbCmd.fromMode(srcMode, ListCmd.pathText(a.path), destLabel, n) + nowIn(t, dest));
    }

    /** התיקייה "יצוא" בתיקיית התוכנה. */
    static File exportDir() {
        return new File(Paths.data(), Words.DbCmd.EXPORT_DIR);
    }

    /** COMP: לפי המסלולים; /תנאים — גם התנאים של שתי חכמות. */
    static DbListCmd.Res comp(Store s, String mode, ListCmd.Side a, ListCmd.Side b, boolean conds, DbListCmd.Finder f) {
        String[] err = {null};
        List<String> fa = DbListCmd.files(s, mode, a, f, err), fb = fa == null ? null : DbListCmd.files(s, mode, b, f, err);
        if (fa == null || fb == null) return DbListCmd.Res.err(err[0]);
        Set<String> sa = new LinkedHashSet<String>(fa), sb = new LinkedHashSet<String>(fb);
        List<String> onlyA = new ArrayList<String>(), onlyB = new ArrayList<String>();
        int both = 0;
        for (String x : sa) if (sb.contains(x)) both++; else onlyA.add(x);
        for (String x : sb) if (!sa.contains(x)) onlyB.add(x);
        String text = Words.ListCmdW.compared(a.label(), onlyA.size(), b.label(), onlyB.size(), both) + examples(onlyA) + examples(onlyB);
        if (conds) {
            long[] ra = DbListCmd.smart(s, mode, a), rb = DbListCmd.smart(s, mode, b);
            text += "\n" + ListCmd.condsDiff(ra == null ? "" : s.prop(ra[1], Store.P_COND), rb == null ? "" : s.prop(rb[1], Store.P_COND),
                    a.label(), b.label());
        }
        return DbListCmd.Res.same(text);
    }

    static String examples(List<String> paths) {
        if (paths.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("\n   ");
        for (int i = 0; i < Math.min(3, paths.size()); i++) sb.append(i > 0 ? " · " : "").append(new File(paths.get(i)).getName());
        if (paths.size() > 3) sb.append(" · ").append(Words.andMore(paths.size() - 3));
        return sb.toString();
    }
}
