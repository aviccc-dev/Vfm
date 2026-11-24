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
                    r.error = Words.Find.scopeMissing(Names.pathText(path), Words.ListCmdW.noList(seg.trim()));
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
                r.error = Words.Find.scopeMissing(Names.pathText(real), f.error != null ? f.error : Words.ListCmdW.noList(Names.label(real)));
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
