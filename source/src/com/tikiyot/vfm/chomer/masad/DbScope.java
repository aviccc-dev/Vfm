package com.tikiyot.vfm;

// מבנה 282 (אפיון החיפוש, נקודה 3 — המשתמש, 19.11.2026: "אם אני ברשימה אז שם בלבד"): התנאי "רשימה:«<אופן>\<רשימה>…»" — כל
// הקבצים שברשימה ובכל הרשימות שבתוכה (רשימות חכמות שבתוכה — לא: הן עצמן חיפוש). רק "<אופן>" — הבית שלו, כלומר כל האופן.
// אחראי: למצוא את הרשימה (בכלל האחד של DbPaths.listAt) ולאסוף את המסלולים. אסור לו: לכתוב (רק Db.read), להציג.

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class DbScope {

    private DbScope() { }

    /** מה שנמצא: המסלולים המנורמלים (Text.norm) של הקבצים ושל התיקיות שלהם; או הסיבה שלא נמצא. */
    static final class Res {
        final Set<String> files = new HashSet<String>(), dirs = new HashSet<String>();
        String error;
    }

    /**
     * הדרך מגיעה מנורמלת (החיפוש מנרמל את כל השאילתה — אותיות סופיות, אותיות קטנות), ולכן האופן והרשימות נמצאים לפי השם
     * המנורמל, ואז — בשמם האמיתי — בכלל האחד של פנייה לרשימה (DbPaths.listAt).
     */
    static synchronized Res of(String ref) {
        Kept k = KEPT.get(ref);                                                // מבנה 301: המסד לא השתנה — אותה תשובה, בלי לפתוח אותו
        if (k != null && Db.app != null && k.stamp == Db.stamp(Db.app, k.mode)) return k.res;
        Res r = new Res();
        final List<String> segs = Names.parsePath(ref);
        if (segs.isEmpty()) {
            r.error = Words.Find.scopeMissing(ref, Words.ListCmdW.noList(""));
            return r;
        }
        if (Db.app == null) {
            r.error = Words.Find.scopeMissing(ref, Words.Find.SCOPE_NO_DB);
            return r;
        }
        String mode = null;
        for (String m : Db.dbModes(Db.app)) if (Text.norm(m).equals(Text.norm(segs.get(0)))) mode = m;
        if (mode == null) {
            r.error = Words.Find.scopeMissing(ref, Words.Find.scopeNoMode(segs.get(0)));
            return r;
        }
        final String m = mode;
        try {
            long writes = Db.writes();
            Res res = Db.read(Db.app, m, new Db.Use<Res>() {
                @Override public Res run(SqlStore s) {
                    return in(s, m, segs.subList(1, segs.size()));
                }
            });
            walks++;
            // החותם — אחרי הקריאה: גם קריאה מזיזה את הקבצים (הסגירה מאחדת את היומן לתוך המסד), ואחרת כל חיפוש היה קורא שוב.
            // כתיבה דרך השער בזמן הקריאה (המונה זז) — לא שומרים. שגיאה — לא נשמרת (אולי הרשימה תיווצר).
            if (res.error == null && writes == Db.writes()) {
                KEPT.remove(ref);
                if (KEPT.size() >= KEEP) KEPT.remove(KEPT.keySet().iterator().next());
                KEPT.put(ref, new Kept(m, Db.stamp(Db.app, m), res));
            }
            return res;
        } catch (Throwable t) {
            r.error = Words.Find.scopeMissing(ref, Words.Db.failed(String.valueOf(t.getMessage())));
            return r;
        }
    }

    /**
     * מבנה 301 (הביקורת אחרי מ0.53, 1): התשובות האחרונות, כל אחת עם החותם של המסד שממנו נקראה (Db.stamp). עד כאן — כל חיפוש
     * (כל הקלדה) פתח את המסד ועבר על כל הרשימה מחדש, על חוט המסך. מה שמוחזר — לקריאה בלבד.
     */
    private static final int KEEP = 4;
    private static final java.util.LinkedHashMap<String, Kept> KEPT = new java.util.LinkedHashMap<String, Kept>();
    /** כמה פעמים המסד נקרא באמת — לבדיקה. */
    static int walks;

    private static final class Kept {
        final String mode;
        final long stamp;
        final Res res;

        Kept(String mode, long stamp, Res res) {
            this.mode = mode;
            this.stamp = stamp;
            this.res = res;
        }
    }

    /**
     * מבנה 307 (המשתמש, 25.11.2026: "א" — חיפוש שנשמר מתוך רשימה נשאר ברשימה; "כל שינוי ומחיקת רשימה — כל מה שקשור אליה מה
     * יעשה"): רשימה ששינתה שם (id: from ← to, באופן mode; s — המסד אחרי השינוי). תנאי שיש בו "רשימה:" שפונה אליה — חוזר עם
     * השם החדש; לא פונה — null. "פונה" — הכלל של קובצי הפקודות (Verbs.renamed, מ0.42): הדרך עם השם החדש מובילה לאותה רשימה,
     * והישנה אינה מובילה לאחרת. מוחלף רק השם; שאר הרכיבים — כמו שנכתבו (גם "-רשימה:").
     */
    static String renamed(String cond, Store s, String mode, long id, String from, String to) {
        if (cond == null || from == null || to == null || from.equals(to) || cond.indexOf(Cond.LIST) < 0) return null;
        StringBuilder out = new StringBuilder();
        boolean changed = false;
        for (String token : Cond.tokens(cond)) {
            String not = token.length() > 1 && token.startsWith(Cond.NOT) && !token.startsWith(Cond.NOT + Cond.NOT) ? Cond.NOT : "";
            String body = token.substring(not.length());
            int c = body.indexOf(':');
            List<String> segs = Cond.keyIs(body, Cond.LIST) ? Names.parsePath(body.substring(c + 1)) : new ArrayList<String>();
            if (segs.size() > 1 && Text.norm(segs.get(0)).equals(Text.norm(mode))) {
                List<String> path = new ArrayList<String>(segs.subList(1, segs.size()));
                boolean mine = false;
                for (int k = 0; k < path.size(); k++) {
                    if (!path.get(k).trim().equals(from)) continue;
                    List<String> q = new ArrayList<String>(path.subList(0, k + 1));
                    long was = DbPaths.find(s, mode, q);
                    if (was != 0 && was != id) continue;                       // הישנה מובילה לרשימה אחרת — היא לא שלנו
                    q.set(k, to);
                    if (DbPaths.find(s, mode, q) != id) continue;
                    path.set(k, to);
                    mine = true;
                }
                if (mine) {
                    path.add(0, segs.get(0));
                    body = body.substring(0, c + 1) + Names.pathText(path);
                    changed = true;
                }
            }
            out.append(out.length() > 0 ? " " : "").append(not).append(Cond.quoteToken(body));
        }
        return changed ? out.toString() : null;
    }

    /** בתוך מסד פתוח: path — הרשימות מתחת לבית (מנורמלות); ריקה — הבית. */
    static Res in(Store s, String mode, List<String> path) {
        Res r = new Res();
        long id;
        if (path.isEmpty()) id = DbPaths.home(s);
        else {
            List<String> real = new ArrayList<String>();
            for (String seg : path) {
                String want = Text.norm(seg.trim()), found = null;
                boolean two = false;
                for (Store.Item it : s.lists())
                    if (Text.norm(it.name).equals(want)) {
                        if (found == null) found = it.name;
                        else if (!found.equals(it.name)) two = true;   // שני שמות שנראים אותו דבר בחיפוש ("Abc", "abc")
                    }
                if (found == null) {
                    // מבנה 308 (המשתמש, 25.11.2026: "צריכים לכתוב רשימה הוסרה"): רשימה שנמחקה אינה נשארת במסד (לא בסל) — ולכן
                    // "הוסרה או שאינה קיימת"; שינוי שם כבר אינו מגיע לכאן (307)
                    r.error = Words.Find.scopeMissing(Names.pathText(path), Words.Find.scopeRemoved(seg.trim()));
                    return r;
                }
                if (two) {
                    r.error = Words.Find.scopeMissing(Names.pathText(path), Words.Find.scopeTwoNames(seg.trim()));
                    return r;
                }
                real.add(found);
            }
            DbPaths.Found f = DbPaths.listAt(s, mode, real);
            if (f.error != null || f.id == 0) {
                r.error = Words.Find.scopeMissing(Names.pathText(real), f.error != null ? f.error : Words.Find.scopeRemoved(Names.label(real)));
                return r;
            }
            id = f.id;
        }
        if (id == 0) {
            r.error = Words.Find.scopeMissing(mode, Words.Find.SCOPE_NO_HOME);
            return r;
        }
        collect(s, id, new HashSet<Long>(), r);
        return r;
    }

    private static void collect(Store s, long list, Set<Long> seen, Res r) {
        if (!seen.add(list)) return;                                            // רשימה שמקושרת בשני מקומות — פעם אחת
        for (Store.Member m : s.members(list)) {
            if (m.item.type == Store.LIST) collect(s, m.item.id, seen, r);
            else if (m.item.type == Store.FILE) {
                Store.FileInfo f = s.file(m.item.id);
                if (f == null || f.path == null) continue;
                String p = trim(Text.norm(f.path));
                int cut = p.lastIndexOf('/');
                if (cut <= 0) continue;
                r.files.add(p);
                r.dirs.add(p.substring(0, cut));
            }
        }
    }

    /** בלי "/" בסוף — כדי שהמסלול מהמסד והמסלול מהאינדקס ייפגשו. */
    static String trim(String p) {
        while (p.length() > 1 && p.endsWith("/")) p = p.substring(0, p.length() - 1);
        return p;
    }
}
