package com.tikiyot.vfm;

// מבנה 167 (סידור תיקיית הפקודות): "ודא" של הרשימות והמסד — קיימת / חסרה ומספרים, "ודא זהה" (השוואת רשימות), "ודא קובץ",
// "ודא … באינדקס", "ודא … בסל", "ודא מסד". עד כאן ישב בתוך DbListCmd. אחראי: לתכנן את הבדיקה ולומר אם עברה. אסור לו: לשנות.

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class ListVerify {

    private ListVerify() { }

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
        long id = a.kind == ListCmd.SMART_LIST ? (DbListCmd.smart(s, mode, a) == null ? 0 : DbListCmd.smart(s, mode, a)[1]) : DbListCmd.list(s, mode, a);
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
        final String mode = DbListCmd.mode(sim);
        Store db;
        try {
            db = DbListCmd.sim(sim, mode);
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
    static void planDiff(final Batch.Step s, final ListCmd.Side a, final ListCmd.Side b, List<String> sw, final String mode) {
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
                    DbListCmd.lastFailed = true;
                    return Words.Db.failed(String.valueOf(t.getMessage()));
                } finally {
                    if (st != null) st.close();
                }
            }
        };
    }

    /** הדוח כטקסט: שורת סיכום, ואחריה שורה לכל הבדל. רשימה שאיננה — כישלון. */
    static String diffText(Store s, String mode, ListCmd.Side a, ListCmd.Side b) {
        long x = DbListCmd.list(s, mode, a), y = DbListCmd.list(s, mode, b);
        if (x == 0 || y == 0) {
            ListCmd.Side gone = x == 0 ? a : b;
            DbListCmd.lastFailed = true;
            return DbListCmd.listWhy(s, mode, gone);
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
        final String mode = DbListCmd.mode(sim);
        Store db;
        try {
            db = DbListCmd.sim(sim, mode);
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
        long x = DbListCmd.list(s, mode, a), y = DbListCmd.list(s, mode, b);
        if (x == 0 || y == 0) {
            ListCmd.Side gone = x == 0 ? a : b;
            found[0] = DbListCmd.listWhy(s, mode, gone);
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
            if (t.equals(DbListCmd.W_IN_INDEX) && conds.size() == 1) index = true;          // מ0.42: ודא "<קובץ>" באינדקס
            else if (t.equals(Words.DbCmd.W_IN_TRASH) && conds.size() == 1) trash = -1;   // מ0.45: ודא "<קובץ>" בסל · /בסל:N
            else if (t.startsWith(Words.DbCmd.SW_IN_TRASH) && conds.size() == 1) {
                try { trash = Integer.parseInt(t.substring(Words.DbCmd.SW_IN_TRASH.length()).trim()); } catch (NumberFormatException e) { trash = -3; }
                if (trash < 0) { s.error = Words.DbCmd.VERIFY_USAGE; return; }
            }
            else if (t.equals(Words.DbCmd.WORD_EXISTS) || t.equals(Words.DbCmd.WORD_EXISTS_M)) want = Boolean.TRUE;
            else if (t.equals(Words.DbCmd.WORD_MISSING) || t.equals(Words.DbCmd.WORD_MISSING_M)) want = Boolean.FALSE;
            else if (t.startsWith(DbListCmd.SW_FILES_IN)) {                            // מ0.37: /קבצים:N — כמה קבצים בתיקייה (בלי תתי־תיקיות)
                try { count = Integer.parseInt(t.substring(DbListCmd.SW_FILES_IN.length()).trim()); } catch (NumberFormatException e) { count = -1; }
                if (count < 0) { s.error = Words.DbCmd.VERIFY_USAGE; return; }
            } else if (t.equals(DbListCmd.W_CONTAINS) && i + 1 < conds.size()) has = conds.get(++i);   // מ0.37: מכיל "טקסט"
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

    /** מ0.42: האם האינדקס מכיר את הקובץ (ובכונן — קיים?). בהרצה — מול האינדקס של אותו רגע, אחרי שהתור שלו התרוקן. */
    static void planVerifyIndex(final Batch.Step s, final File f) {
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
    static void planVerifyTrash(final Batch.Step s, final File f, final int n) {
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
}
