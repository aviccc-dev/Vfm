package com.tikiyot.vfm;

// מבנה 241: "עצב" — במקום אחד (הפועל של העיצוב; המשתמש, 17.11.2026: "אולי עצב"): הכלל (lookErrors — מה מותר ומה אומרים
// כשלא), הקביעה (shapeOn — על פריט אחד: רשימה, או הבית של אופן), "עצב "<אופן>"" / "עצב תוכנה" (planShapeAll), רשימה באופן
// אחר (planShapeList), ו"ודא עיצוב"
// (planVerify — מה שנראה בפועל, אחרי הירושה, מול מה שנכתב). התחביר — ב־Verbs; העיצוב עצמו והירושה — ב־Design ו־Palette.

import java.util.List;
import java.util.Map;

final class ShapeCmds {

    private ShapeCmds() { }

    /** מבנה 235: הערכים של המראה — בטווח, והגופן קיים (הכלל — ב־Design). null — הכול תקין; אחרת — למה לא. */
    static String lookErrors(Verbs.Cmd c) {
        String design = c.edit.get(Store.P_DESIGN);                          // מבנה 240: עיצוב שאין — נאמר (לא נופלים בשקט לברירת המחדל)
        if (design != null && !design.trim().isEmpty() && !Design.known(design)) return Words.DesignW.noDesign(design.trim());
        for (java.util.Map.Entry<String, String> e : c.edit.entrySet()) {
            if (!e.getKey().startsWith(Verbs.E_LOOK)) continue;
            String key = e.getKey().substring(Verbs.E_LOOK.length());
            boolean colors = key.equals(Palette.K_MODE) || key.startsWith(Palette.K_COLOR + ":");   // מבנה 237
            if (colors && key.startsWith(Palette.K_COLOR + ":") && Palette.role(key.substring(key.indexOf(':') + 1)) < 0)
                return Palette.error(key, e.getValue());                         // תפקיד שאין — גם כשמנקים
            if (key.startsWith(Design.K_WORDING + ":") && e.getValue().isEmpty()) {   // מבנה 250: סוג שאין — גם כשמנקים
                String why = Design.wordingError(key.substring(key.indexOf(':') + 1).trim(), "");
                if (why != null) return why;
                continue;
            }
            if (e.getValue().isEmpty()) continue;
            String why = colors ? Palette.error(key, e.getValue()) : Design.lookError(key, e.getValue());
            if (why != null) return why;
        }
        return null;
    }

    /**
     * מבנה 235, 236: "עצב" על פריט אחד — העיצוב (pDesign; "" — לרשת) והמראה (pLook): רק מה שנכתב משתנה.
     * מבנה 260 (המשתמש, 18.11.2026 — "א": "החלפת סגנון מביאה את מספר הטורים הרגיל של הסגנון: 3 ברשת, 1 בשורות; כתבת טורים באותה
     * שורה — נשאר מה שכתבת"): /סגנון: בלי /טורים:, והסגנון שנראה מתחלף (מול מה שנראה עד כאן; ואם באותה שורה נבחר עיצוב — מול
     * הסגנון של העיצוב הזה) ← הטורים הרגילים של הסגנון; /סגנון:"" שמחזיר את הסגנון שמעל ← גם הטורים חוזרים למה שמעל.
     * הכלל האחד — כאן, לכל הדרכים (קובץ פקודות, הלוח). above — מה שנראה מעל הפריט (null — כלום). חוזר: מה שבא עם הסגנון, לדוח; או null.
     */
    static String shapeOn(Store t, long id, java.util.Map<String, String> edit, String pDesign, String pLook, Design above) {
        String style = edit.get(Verbs.E_LOOK + Design.K_STYLE);
        boolean rule = style != null && !edit.containsKey(Verbs.E_LOOK + Design.K_COLS);
        boolean before = rule && Design.gridOf(above, t.prop(id, pDesign), t.prop(id, pLook));
        if (edit.containsKey(Store.P_DESIGN)) t.setProp(id, pDesign, Design.current(edit.get(Store.P_DESIGN)));   // מבנה 240: בשם של היום
        String look = t.prop(id, pLook), was = look, note = null;
        for (java.util.Map.Entry<String, String> e : edit.entrySet())
            if (e.getKey().startsWith(Verbs.E_LOOK)) look = Design.withLook(look, e.getKey().substring(Verbs.E_LOOK.length()), e.getValue());
        if (rule) {
            String design = t.prop(id, pDesign);
            boolean now = Design.gridOf(above, design, look);
            boolean base = edit.containsKey(Store.P_DESIGN) ? Design.gridOf(above, design, Design.withLook(look, Design.K_STYLE, "")) : before;
            if (now != base) {
                String cols = style.trim().isEmpty() ? "" : String.valueOf(Design.usualCols(now));
                String next = Design.withLook(look, Design.K_COLS, cols);
                if (!next.equals(look == null ? "" : look)) {
                    look = next;
                    note = Words.ListCmdW.colsWithStyle(cols);
                }
            }
        }
        if (look != null && !look.equals(was)) t.setProp(id, pLook, look);
        return note;
    }

    /**
     * מבנה 236 (המשתמש, 17.11.2026: "אחת בסיסית על כל התוכנה" · "וניתן לשינוי לכל אופן כמו לכל רשימה ותת רשימה"): "עצב
     * "<אופן>"" (מבנה 246; עד כאן "עצב אופן") — העיצוב והמראה שמעל כל הרשימות של האופן (על הבית שלו); "עצב תוכנה" — מעל כל האופנים (על הבית של אופן התפריטים,
     * "שמור לתוכנה"). הכלל — כמו ברשימה (lookErrors, shapeOn). בהרצה — על המסד של האופן, דרך המנהל.
     */
    static String planShapeAll(final Batch.Step s, final Verbs.Cmd c, Sim sim) {
        final boolean program = c.kind.equals(Verbs.K_PROGRAM);
        final String m = program ? Db.MENUS_MODE : c.arg;
        if (!program && !Db.dbFile(Db.app, m).isFile()) { s.error = Words.DbCmd.noMode(m, sim.modes()); return null; }
        if ((s.error = lookErrors(c)) != null) return null;
        c.noOpen = true;
        final String said = program ? Words.ListCmdW.DESIGN_SET_ALL : Words.ListCmdW.designSetMode(m);
        s.describe = Words.DbCmd.inDb(said);
        s.op = new ListCmd.Op() {
            @Override public String run(Library lib) {
                DbListCmd.Res r = ListRun.realRes(m, new DbListCmd.Work() {
                    @Override public DbListCmd.Res run(Store t, DbListCmd.Finder f) {
                        long h = DbPaths.home(t);
                        if (h == 0) return DbListCmd.Res.err(Words.Db.LIST_GONE);
                        String more = shapeOn(t, h, c.edit, Store.P_ALL_DESIGN, Store.P_ALL_LOOK, program ? null : Design.forProgram());
                        return DbListCmd.Res.ok(more == null ? said : said + more);
                    }
                }, false, lib);
                if (program) Design.forgetProgram();
                Palette.forget();                                               // מבנה 237: הצבעים — לקרוא מחדש
                if (r.error != null) DbListCmd.lastFailed = true;
                return r.error != null ? r.error : Words.DbCmd.inDb(r.text);
            }
        };
        return m;
    }

    /**
     * מבנה 246 (המשתמש, 17.11.2026: "כל המסלול או חלק ממנו"): רשימה באופן אחר מזה של הקובץ — בלי העותק של ההרצה (הוא של האופן
     * של הקובץ): הרשימה נבדקת במסד של האופן שלה, ובהרצה — עליו, דרך המנהל. הכלל — אותו כלל (lookErrors, shapeOn).
     */
    static String planShapeList(final Batch.Step s, final Verbs.Cmd c, Sim sim) {
        final String m = c.cond;
        if (!Db.dbFile(Db.app, m).isFile()) { s.error = Words.DbCmd.noMode(m, sim.modes()); return null; }
        if ((s.error = lookErrors(c)) != null) return null;
        c.noOpen = true;
        final List<String> at = Names.parsePath(c.arg);
        final DbListCmd.Work work = new DbListCmd.Work() {
            @Override public DbListCmd.Res run(Store t, DbListCmd.Finder f) {
                DbPaths.Found u = DbPaths.listAt(t, m, at);
                if (u.error != null || u.id == 0) return DbListCmd.Res.err(u.ambiguous ? u.error : Words.ListCmdW.noList(Names.label(at)));
                String more = shapeOn(t, u.id, c.edit, Store.P_DESIGN, Store.P_LOOK, Design.forListAbove(t, u.id));
                return DbListCmd.Res.ok(more == null ? Words.ListCmdW.DESIGN_SET : Words.ListCmdW.DESIGN_SET + more);
            }
        };
        String no;
        try {
            no = Db.read(Db.app, m, new Db.Use<String>() {                      // בתכנון — רק לבדוק שהרשימה שם
                @Override public String run(SqlStore st) {
                    DbPaths.Found u = DbPaths.listAt(st, m, at);
                    return u.error != null || u.id == 0 ? (u.ambiguous ? u.error : Words.ListCmdW.noList(Names.label(at))) : null;
                }
            });
        } catch (Throwable t) {
            no = Words.Db.failed(String.valueOf(t.getMessage()));
        }
        if (no != null) { s.error = no; return null; }
        s.describe = Words.DbCmd.inDb(Words.ListCmdW.DESIGN_SET);
        s.op = new ListCmd.Op() {
            @Override public String run(Library lib) {
                DbListCmd.Res r = ListRun.realRes(m, work, null, lib);
                if (r.error != null) DbListCmd.lastFailed = true;
                return r.error != null ? r.error : Words.DbCmd.inDb(r.text);
            }
        };
        return m;
    }

    /**
     * מבנה 241 (המשתמש, 17.11.2026: "קבצים שמנים לפי כל הפרמטרים, עם בדיקות"), 246: "ודא עיצוב "<אופן>\<רשימה>…" …" · "ודא
     * עיצוב "<אופן>" …" · "ודא עיצוב תוכנה …" — אותם מתגים כמו ב"עצב", ונבדק מה שנראה בפועל (אחרי הירושה): העיצוב, הטורים,
     * הריווח, הצלמית, הכתב, והצבעים (מבנה 269: גם לרשימה). tk — המילים של השורה (ודא, עיצוב, …).
     */
    static void planVerify(final Batch.Step s, List<String> tk, Map<String, String> vars, Sim sim) {
        if (!DbVerbs.active()) { s.error = Words.DbCmd.VERIFY_DB_ONLY; return; }   // כמו כל "ודא" של המסד
        StringBuilder line = new StringBuilder(Verbs.SHAPE);
        for (int i = 2; i < tk.size(); i++) line.append(' ').append(quoted(tk.get(i), i == 2 && tk.get(i).equals(Verbs.PROGRAM_WORD)));
        final Verbs.Cmd c = Verbs.parse(Verbs.withVars(line.toString(), vars));
        boolean empty = false;
        if (c.edit != null) for (Map.Entry<String, String> e : c.edit.entrySet())   // מבנה 250: נוסח "" — "כמו במילון", ונבדק
            if (e.getValue().trim().isEmpty() && !e.getKey().startsWith(Verbs.E_LOOK + Design.K_WORDING + ":")) empty = true;
        if (c.error != null || c.edit == null || empty || Parse.var(Syntax.V_SELECTED).equals(c.arg)) {
            s.error = Words.DbCmd.VERIFY_SHAPE_USAGE;
            return;
        }
        if ((s.error = lookErrors(c)) != null) return;
        final boolean program = c.kind.equals(Verbs.K_PROGRAM), mode = c.kind.equals(Verbs.K_MODE);
        final String m = program ? Db.MENUS_MODE : mode ? c.arg : c.cond != null ? c.cond : DbListCmd.mode(sim);   // מבנה 246: האופן — מהדרך
        if (!program && (mode || c.cond != null) && !Db.dbFile(Db.app, m).isFile()) { s.error = Words.DbCmd.noMode(m, sim.modes()); return; }
        if (!program && !mode && c.cond == null && sim.modeError != null) { s.error = sim.modeError; return; }
        final String what = Words.DbCmd.shapeWhat(program ? null : mode ? Words.DbCmd.modeOf(m) : Names.label(Names.parsePath(m + "\\" + c.arg)));
        s.target = what;
        s.describe = Words.DbCmd.verifyWill(what);
        s.op = new ListCmd.Op() {
            @Override public String run(Library lib) {
                State st = state(c.kind, c.arg, m);
                if (st.error != null) {
                    s.verified = Boolean.FALSE;
                    return st.error;
                }
                StringBuilder found = new StringBuilder();
                boolean ok = matches(st.shown, st.looks, c.edit, found);
                s.verified = ok;
                return Words.DbCmd.verifyResult(ok, what, found.toString());
            }
        };
    }

    /**
     * מבנה 243: מה שנקבע ומה שנראה — לרשימה (kind = K_LIST, arg — הדרך), לאופן (K_MODE, arg — השם) ולכל התוכנה (K_PROGRAM).
     * במקום אחד: "ודא עיצוב" (מה שנראה), ותוסף העיצוב (מה שנקבע כאן, מה שנראה, ומה שהיה בלי מה שנקבע כאן — "כמו שמעליו").
     */
    static final class State {
        /** null — נמצא; אחרת — למה לא (רשימה שאין, ספק, אופן שאין, תקלה). */
        String error;
        /** מה שנקבע כאן: העיצוב ("" — לא נבחר) והמראה (השורות; "" — כלום). */
        String own = "", look = "";
        /** מה שנראה, ומה שהיה בלי מה שנקבע כאן; לא נקבע כלום — ברירת המחדל (set / setAbove — false). */
        Design shown, above;
        boolean set, setAbove;
        /** המראות שקובעים את הצבעים — עם מה שנקבע כאן, ובלעדיו (מבנה 269: לרשימה — עם הרשימות שמעליה). */
        String[] looks, aboveLooks;
    }

    static State state(String kind, String arg, String m) {
        State r = new State();
        boolean program = Verbs.K_PROGRAM.equals(kind), mode = Verbs.K_MODE.equals(kind);
        String all = Design.programProps()[1];
        if (program) {
            String[] p = Design.programProps();
            r.own = p[0] == null ? "" : p[0];
            r.look = p[1] == null ? "" : p[1];
            fill(r, Design.forProgram(), null, new String[]{all}, new String[0]);
            return r;
        }
        if (m == null || Db.app == null || !Db.dbFile(Db.app, m).isFile()) {
            r.error = Words.DbCmd.noMode(String.valueOf(m), Db.app == null ? new java.util.ArrayList<String>() : Db.dbModes(Db.app));
            return r;
        }
        SqlStore st = null;
        try {
            st = Db.openRead(Db.app, m);
            long h = DbPaths.home(st);
            String modeLook = h == 0 ? null : st.prop(h, Store.P_ALL_LOOK);
            if (mode) {
                r.own = h == 0 ? "" : nz(st.prop(h, Store.P_ALL_DESIGN));
                r.look = nz(modeLook);
                fill(r, Design.forMode(st), Design.forProgram(), new String[]{all, modeLook}, new String[]{all});
                return r;
            }
            List<String> at = Names.parsePath(arg);
            DbPaths.Found u = DbPaths.listAt(st, m, at);
            if (u.error != null || u.id == 0) {
                r.error = u.ambiguous ? u.error : Words.ListCmdW.noList(Names.label(at));
                return r;
            }
            r.own = nz(st.prop(u.id, Store.P_DESIGN));
            r.look = nz(st.prop(u.id, Store.P_LOOK));
            fill(r, Design.forList(st, u.id), Design.forListAbove(st, u.id), Palette.listLooks(st, u.id, true), Palette.listLooks(st, u.id, false));
            return r;
        } catch (Throwable t) {
            r.error = Words.Db.failed(String.valueOf(t.getMessage()));
            return r;
        } finally {
            if (st != null) st.close();
        }
    }

    private static void fill(State r, Design shown, Design above, String[] looks, String[] aboveLooks) {
        r.set = shown != null;
        r.setAbove = above != null;
        r.shown = shown != null ? shown : Design.get(Design.DEFAULT);
        r.above = above != null ? above : Design.get(Design.DEFAULT);
        r.looks = looks;
        r.aboveLooks = aboveLooks;
    }

    private static String nz(String v) {
        return v == null ? "" : v;
    }

    /** מילה מהשורה, בחזרה עם מירכאות כשצריך (שם עם רווח; מתג שהערך שלו עם רווח). first — המילה שאחרי "עיצוב" (רשימה / אופן / תוכנה). */
    private static String quoted(String w, boolean first) {
        if (first) return w;
        int c = w.indexOf(':');
        if (w.startsWith("/") && c > 0) return w.substring(0, c + 1) + "\"" + w.substring(c + 1) + "\"";
        return "\"" + w + "\"";
    }

    /** מה שנראה (d, והצבעים לפי looks) מול מה שנכתב (edit). found — מה שנמצא, לכל מה שנבדק. */
    static boolean matches(Design d, String[] looks, Map<String, String> edit, StringBuilder found) {
        boolean ok = true;
        int[] colors = Palette.resolve(looks);
        for (Map.Entry<String, String> e : edit.entrySet()) {
            String key = e.getKey(), want = e.getValue().trim(), got;
            boolean same;
            if (key.equals(Store.P_DESIGN)) {
                got = d.name;
                same = Design.current(want).equals(got);
                key = Words.DbCmd.SHAPE_DESIGN;
            } else {
                key = key.substring(Verbs.E_LOOK.length());
                int c = key.indexOf(':');
                String who = c > 0 ? key.substring(0, c).trim() : "", what = c > 0 ? key.substring(c + 1).trim() : key;
                if (key.equals(Palette.K_MODE)) {
                    got = Palette.dark(looks) ? Palette.DARK : Palette.LIGHT;
                    same = got.equals(want);
                } else if (who.equals(Palette.K_COLOR)) {
                    got = Palette.hex(colors[Palette.role(what)]);
                    Integer w = Palette.color(want);
                    same = w != null && w == colors[Palette.role(what)];
                } else if (who.equals(Design.K_WORDING)) {                   // מבנה 250: הנוסח שייראה ("" — כמו במילון)
                    String w = d.wording.get(what);
                    got = w == null ? "" : w;
                    same = got.equals(want);
                } else if (d.shapeValue(key) != null) {                       // מבנה 254: סגנון, הצג צלמית, סביב הצלמית…
                    got = d.shapeValue(key);
                    same = got.equals(want);
                } else if (key.equals(Design.K_COLS) || key.equals(Design.K_GAP) || key.equals(Design.K_ICON)) {
                    int v = key.equals(Design.K_COLS) ? d.cols : key.equals(Design.K_GAP) ? d.gap : d.icon;
                    got = String.valueOf(v);
                    same = got.equals(want.replace("%", "").trim());
                } else {
                    Design.Text t = who.equals(Design.T_TITLE) ? d.title : who.equals(Design.T_NAME) ? d.nameT
                            : who.equals(Design.T_DATA) ? d.data : who.equals(Design.T_TAG) ? d.tagText() : d.groupText();
                    if (what.equals(Design.K_SIZE)) {
                        got = t.size == (int) t.size ? String.valueOf((int) t.size) : String.valueOf(t.size);
                        same = got.equals(want);
                    } else if (what.equals(Design.K_WEIGHT)) {
                        got = Design.weightName(t.weight);
                        same = t.weight == Design.weight(want);
                    } else {
                        got = t.font;
                        same = want.equals(t.font);
                    }
                }
            }
            if (found.length() > 0) found.append(" · ");
            found.append(key).append(' ').append(got);
            if (!same) ok = false;
        }
        return ok;
    }
}
