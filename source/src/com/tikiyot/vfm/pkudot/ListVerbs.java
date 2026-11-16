package com.tikiyot.vfm;

// מבנה 169 (סידור תיקיית הפקודות): פעולות המסכים על רשימות, כפקודה — רשימה / רשימה חכמה חדשה, עריכה (הטופס של הרשימה
// ושל הרשימה החכמה), רעיון וכותרת (planVerb והפונקציות שלו). עד כאן ישב בתוך DbListCmd. אחראי: לתכנן את מה שהמסך ביקש.
// אסור לו: להריץ, ולהציג.

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class ListVerbs {

    private ListVerbs() { }

    /** מבנה 104: ההודעה לפי מה שקרה. */
    static DbListCmd.Res smartMade(Lists.Made m) {
        if (m.error != null) return DbListCmd.Res.err(m.error);
        if (m.skipped) return DbListCmd.Res.same(Words.ListCmdW.smartSkipped(m.name));
        DbListCmd.Res ok = DbListCmd.Res.ok(m.replaced ? Words.ListCmdW.smartReplaced(m.name) : m.renamed ? Words.ListCmdW.smartRenamed(m.name)
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
    static String planPickedSmart(final Batch.Step s, final Verbs.Cmd c, Sim sim) {
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
        final int taken = DbListCmd.taken(c.taken);
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
                DbListCmd.Res r = ListRun.realRes(mode, new DbListCmd.Work() {
                    @Override public DbListCmd.Res run(Store t, DbListCmd.Finder f) {
                        if (into != 0 && (t.item(into) == null || t.item(into).type != Store.LIST)) return DbListCmd.Res.err(Words.Db.LIST_GONE);
                        return smartMade(Lists.newSmart(t, into, name, cond, props, taken));
                    }
                }, before, lib);
                if (r.error != null) DbListCmd.lastFailed = true;
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
    static String planPickedSmartEdit(final Batch.Step s, final Verbs.Cmd c, Sim sim) {
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
                DbListCmd.Res r = ListRun.realRes(mode, new DbListCmd.Work() {
                    @Override public DbListCmd.Res run(Store t, DbListCmd.Finder f) {
                        Store.Item it = t.item(id);
                        if (it == null || it.type != Store.SMART) return DbListCmd.Res.err(Words.Db.ITEM_GONE);
                        String was = it.name;
                        if (!name.isEmpty() && !name.equals(was))                // מבנה 104: שם חדש שכבר קיים באחת הרשימות שהיא בהן — שם קיים
                            for (Long p : t.listsOf(id)) if (Lists.smartTaken(t, p, name)) return DbListCmd.Res.err(Words.ListCmdW.smartTaken(name));
                        Lists.saveSmart(t, id, 0, name, cond, props);
                        DbListCmd.Res ok = DbListCmd.Res.ok(Words.DbCmd.saved(name.isEmpty() ? was : name)).renamed(id, was, t.item(id).name);
                        ok.made = id;
                        return ok;
                    }
                }, Backups.OP_EDIT, lib);
                if (r.error != null) DbListCmd.lastFailed = true;
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
    static DbListCmd.Res edited(Store t, long id, String name, String icon, String desc, String why) {
        Store.Item it = t.item(id);
        if (it == null) return DbListCmd.Res.err(Words.Db.ITEM_GONE);
        String was = it.name;
        String e = Lists.edit(t, id, name, icon, desc, why);
        if (e != null) return DbListCmd.Res.err(e);
        DbListCmd.Res ok = DbListCmd.Res.ok(Words.DbCmd.saved(t.item(id).name)).renamed(id, was, t.item(id).name);   // מ0.42: קובצי הפקודות שפונים אליה
        ok.made = id;
        ok.replaced = true;
        return ok;
    }

    /**
     * מבנה 113: הטופס של עריכת רשימה במסך — "עדכן רשימה %נבחרים%". הפריט — לפי המספר שלו (הבחירה); השם, הצלמית, התיאור וה"למה" —
     * מהבחירה, ואינם מפוענחים. אותו כלל כמו בשורה הכתובה (edited). מה שנובע מהמקום: במסך — כל הטופס, ושדה שם ריק פירושו "בלי
     * שינוי בשם"; ורעיון / כותרת (שאין להם דרך של שמות) — רק מכאן, והטקסט שלהם הוא ה"שם".
     */
    static String planPickedEdit(final Batch.Step s, final Verbs.Cmd c, Sim sim) {
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
                    DbListCmd.Res r = ListRun.realRes(m, new DbListCmd.Work() {
                        @Override public DbListCmd.Res run(Store t, DbListCmd.Finder f) {
                            if (place) {
                                if (in == 0 || t.item(in) == null) return DbListCmd.Res.err(Words.Db.LIST_GONE);
                                DbMore.order(t, in, ids, how);
                                return DbListCmd.Res.ok(Words.ListCmdW.ORDERED);
                            }
                            Store.Item it = t.item(ids.get(0));
                            if (it == null || it.type != Store.LIST) return DbListCmd.Res.err(Words.Db.LIST_GONE);
                            t.setProp(it.id, Store.P_DESIGN, name);              // "" — לרשת (המאפיין נמחק)
                            return DbListCmd.Res.ok(Words.ListCmdW.DESIGN_SET);
                        }
                    }, false, lib);
                    if (r.error != null) DbListCmd.lastFailed = true;
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
                DbListCmd.Res r = ListRun.realRes(mode, new DbListCmd.Work() {
                    @Override public DbListCmd.Res run(Store t, DbListCmd.Finder f) {
                        Store.Item it = t.item(id);
                        if (it != null && (it.type == Store.SMART || it.type == Store.FILE)) return DbListCmd.Res.err(Words.DbCmd.FILE_NO_EDIT);
                        return edited(t, id, sel.name, sel.props.get(Store.P_ICON), sel.props.get(Store.P_DESC), sel.props.get(Store.P_WHY));
                    }
                }, Backups.OP_EDIT, lib);
                if (r.error != null) DbListCmd.lastFailed = true;
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
    static String planPickedNote(final Batch.Step s, final Verbs.Cmd c, final boolean title, Sim sim) {
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
                DbListCmd.Res r = ListRun.realRes(mode, new DbListCmd.Work() {
                    @Override public DbListCmd.Res run(Store t, DbListCmd.Finder f) {
                        long in = path.isEmpty() ? here : DbPaths.find(t, mode, path);
                        if (in == 0) return DbListCmd.Res.err(Words.Verb.NOT_FOUND + c.cond);
                        Store.Item it = t.item(in);
                        if (it == null || it.type != Store.LIST) return DbListCmd.Res.err(Words.Db.LIST_GONE);
                        DbListCmd.Res ok = DbListCmd.Res.ok(Words.DbCmd.noteAdded(title, text, path.isEmpty() ? it.name : where));
                        ok.made = title ? t.addTitle(in, text) : t.addNote(in, text);
                        return ok;
                    }
                }, false, lib);
                if (r.error != null) DbListCmd.lastFailed = true;
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
        if (listUpd && Parse.var(Syntax.V_SELECTED).equals(c.arg)) return planPickedEdit(s, c, sim);                  // מבנה 113: מהטופס
        if ((note || title) && Parse.var(Syntax.V_SELECTED).equals(c.arg)) return planPickedNote(s, c, title, sim);   // מבנה 93: מהמסך
        if (smartNew && Parse.var(Syntax.V_NEW_NAME).equals(c.arg)) return planPickedSmart(s, c, sim);                // מבנה 97: מהטופס
        if (smartUpd && Parse.var(Syntax.V_SELECTED).equals(c.arg)) return planPickedSmartEdit(s, c, sim);            // מבנה 99: מהטופס
        if (sim.modeError != null) { s.error = sim.modeError; return null; }
        final String mode = DbListCmd.mode(sim);
        final Store db;
        try {
            db = DbListCmd.sim(sim, mode);
        } catch (Throwable t) {
            Log.e("DbListCmd.sim", t);
            s.error = Words.Db.failed(String.valueOf(t.getMessage()));
            return null;
        }
        final List<String> path = list ? Names.parsePath(c.arg) : note || title ? Names.parsePath(c.cond) : null;
        if ((note || title) && path.isEmpty()) { s.error = Words.DbCmd.NOTE_NEEDS_LIST; return null; }
        final DbListCmd.Work work = new DbListCmd.Work() {
            @Override
            public DbListCmd.Res run(Store t, DbListCmd.Finder f) {
                if (note || title) {
                    DbPaths.Found at = DbPaths.listAt(t, mode, path);            // מבנה 107
                    long in = at.error != null ? 0 : at.id;
                    if (in == 0) return DbListCmd.Res.err(at.ambiguous ? at.error : Words.ListCmdW.noList(Names.label(path)));
                    if (title) t.addTitle(in, c.arg);
                    else t.addNote(in, c.arg);
                    return DbListCmd.Res.ok(Words.DbCmd.noteAdded(title, c.arg, Names.label(path)));
                }
                if (listUpd) {                                                   // מבנה 113: הרשימה — לפי הדרך (מבנה 107); רק מה שנכתב משתנה
                    List<String> at = Names.parsePath(c.arg);
                    DbPaths.Found u = DbPaths.listAt(t, mode, at);
                    if (u.error != null || u.id == 0) return DbListCmd.Res.err(u.ambiguous ? u.error : Words.ListCmdW.noList(Names.label(at)));
                    if (c.edit.containsKey(Verbs.E_PLACE)) {                     // מבנה 121: סדר ידני — בתוך הרשימה שהיא בה
                        List<String> up = at.subList(0, at.size() - 1);
                        long parent = up.isEmpty() ? DbPaths.home(t) : DbPaths.find(t, mode, new ArrayList<String>(up));
                        List<Long> in = t.listsOf(u.id);
                        if (!in.contains(parent)) parent = in.size() == 1 ? in.get(0) : 0;
                        if (parent == 0) return DbListCmd.Res.err(Words.DbCmd.needFullWay(Names.label(at)));
                        DbMore.order(t, parent, java.util.Collections.singletonList(u.id), Verbs.place(c.edit.get(Verbs.E_PLACE)));
                    }
                    if (c.edit.containsKey(Store.P_DESIGN)) t.setProp(u.id, Store.P_DESIGN, c.edit.get(Store.P_DESIGN).trim());   // "" — לרשת
                    return edited(t, u.id, c.edit.get(Verbs.E_NAME), c.edit.get(Store.P_ICON), c.edit.get(Store.P_DESC), c.edit.get(Store.P_WHY));
                }
                // מבנה 100: המספר של מה שנוצר / עודכן חוזר (DbListCmd.Res.made ← DbListCmd.Step.made) — כפתור שמריץ את השורה פותח בדיוק אותו
                if (list) {                                                      // מבנה 102: "צור רשימה" — הכלל האחד; שם קיים — סירוב
                    if (path == null || path.isEmpty()) return DbListCmd.Res.err(Words.Verb.NOT_FOUND + c.arg);
                    return ListOps.listMade(Lists.newListAt(t, mode, path, DbListCmd.taken(c.taken)), Names.label(path));   // מבנה 109: /תפוס:
                }
                if (smartNew) {                                                  // מבנה 103–104: הכלל האחד (Lists.newSmart) — בבית
                    java.util.Map<String, String> props = new java.util.HashMap<String, String>();
                    props.put(Store.P_SORT, c.sort);
                    return smartMade(Lists.newSmart(t, 0, c.arg, c.cond, props, DbListCmd.taken(c.taken)));
                }
                DbPaths.Found u = DbPaths.updateSmart(t, mode, Names.parsePath(c.arg), c.cond, c.sort);   // מבנה 106
                if (u.error != null) return DbListCmd.Res.err(u.error);
                DbListCmd.Res ok = DbListCmd.Res.ok(Words.DbCmd.updated(c.arg));
                ok.made = u.id;
                ok.replaced = true;                                              // מבנה 106: דורס את התנאי — גיבוי לפני (הוראות, סעיף 4)
                return ok;
            }
        };
        DbListCmd.Res r;
        try {
            r = work.run(db, DbListCmd.finder(sim));                                   // ההדמיה — על העותק
        } catch (Throwable e) {
            r = DbListCmd.Res.err(Words.Db.failed(String.valueOf(e.getMessage())));
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
                    DbListCmd.Res done = ListRun.realRes(mode, work, before, lib);                 // כמו real(), ועוד המספר של מה שנוצר (מבנה 100)
                    if (done.error != null) DbListCmd.lastFailed = true;
                    s.made = done.made;
                    return done.error != null ? done.error : Words.DbCmd.inDb(done.text);
                }
            };
        }
        return mode;
    }
}
