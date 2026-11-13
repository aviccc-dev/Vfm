package com.tikiyot.vfm;

// מ0.22: תפריט הפקודות ⭐ ו"פתח תפריט <רשימה>" — מהמסד. מ0.41: מהתפריטים של התוכנה (Menus) — "פקודות" שבאופן "שמור
// לתוכנה", ואחריה "שמור לתוכנה / פקודות" של האופן שעל המסך, אם יש לו (עד כאן: של האופן, ואם אין — של אופן אחר).
// קובצי הפקודות — פקודות; תתי־רשימות — תפריטי משנה; חכמה — מה שהיא מוצאת. נגיעה ארוכה — עריכה.
// אחראי: החלון של התפריט (מה שבו — Menus; הפריטים של רשימה — DbMore.menu).

import android.app.Activity;
import android.content.DialogInterface;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

final class DbCmdMenu {

    private DbCmdMenu() { }

    /**
     * מ0.41: קודם התפריטים של התוכנה (Menus: הכללי מאופן "שמור לתוכנה", ואחריו של האופן שעל המסך) — הדרך בתוך "שמור
     * לתוכנה" ("שמור לתוכנה/פקודות", או רק "בחירה"); ואם אין — רשימה כלשהי של האופן בדרך הזו, כתפריט (כמו עד כאן).
     * false — אין רשימה כזו.
     */
    static boolean show(Activity a, List<String> path, File newIn, CmdMenu.Picked pick, boolean top) {
        boolean reserved = !path.isEmpty() && path.get(0).trim().equals(Reserved.ROOT);
        List<String> rel = Menus.relative(path);
        Menus.Snap m = Menus.get(a);
        if (reserved) {
            if (!m.has(rel)) return false;
            menu(a, rel, newIn, pick, top);
            return true;
        }
        String mode = DbListScreen.currentMode;
        long id = find(a, mode, path);
        if (id != 0) open(a, mode, id, path, newIn, pick, top);
        else if (m.has(rel)) menu(a, rel, newIn, pick, top);
        else return false;
        return true;
    }

    private static long find(Activity a, String mode, List<String> path) {
        if (mode == null || !DbScreen.dbFile(a, mode).isFile()) return 0;
        SqlStore s = null;
        try {
            s = SqlStore.openReadOnly(DbScreen.dbFile(a, mode));
            return DbPaths.find(s, mode, path);
        } catch (Throwable t) {
            Log.e("DbCmdMenu.find", t);
            return 0;
        } finally {
            if (s != null) s.close();
        }
    }

    /** תפריט מהתפריטים של התוכנה — ממוזג; תת־רשימה נפתחת באותה דרך (ולכן גם היא ממוזגת). "סידור" — הרשימה הכללית. */
    private static void menu(final Activity a, final List<String> rel, final File newIn, final CmdMenu.Picked pick,
                             final boolean top) {
        Menus.Snap m = Menus.get(a);
        final Object[] where = m.where(rel);
        dialog(a, top && rel.equals(Menus.CMDS) ? Words.CmdM.TITLE : rel.isEmpty() ? Reserved.ROOT : rel.get(rel.size() - 1), m.at(rel), new Sub() {
            @Override
            public void open(DbMore.Entry e) {
                List<String> sub = new ArrayList<String>(rel);
                sub.add(e.label);
                menu(a, sub, newIn, pick, false);
            }
        }, where == null ? null : (String) where[0], where == null ? 0 : (Long) where[1], newIn, pick, top);
    }

    /** רשימה של האופן (לפי המספר) כתפריט. */
    private static void open(final Activity a, final String mode, final long id, final List<String> path, final File newIn,
                             final CmdMenu.Picked pick, final boolean top) {
        List<Menus.Item> items = new ArrayList<Menus.Item>();
        SqlStore s = null;
        try {
            s = SqlStore.openReadOnly(DbScreen.dbFile(a, mode));
            for (DbMore.Entry e : DbMore.menu(s, id)) items.add(new Menus.Item(e, mode, id, path));
        } catch (Throwable t) {
            Log.e("DbCmdMenu.open", t);
            Ui.toast(a, Words.Db.failed(String.valueOf(t.getMessage())));
            return;
        } finally {
            if (s != null) s.close();
        }
        dialog(a, path.get(path.size() - 1), items, new Sub() {
            @Override
            public void open(DbMore.Entry e) {
                List<String> sub = new ArrayList<String>(path);
                sub.add(e.label);
                DbCmdMenu.open(a, mode, e.id, sub, newIn, pick, false);
            }
        }, mode, id, newIn, pick, top);
    }

    /** איך נפתחת תת־רשימה (לפי הדרך — בתפריטים של התוכנה; לפי המספר — ברשימה של האופן). */
    private interface Sub {
        void open(DbMore.Entry e);
    }

    /** נגיעה בקובץ פקודות שבתפריט או בשורה: "פקודה חדשה" — יוצרת; אחרת — הרצה (או בחירה, כשמוסיפים כפתור). */
    static void use(Activity a, File f, File newIn, CmdMenu.Picked pick) {
        if (Reserved.isNew(f)) CmdMenu.create(a, newIn, pick);
        else CmdMenu.useFile(a, f, pick);
    }

    private static void dialog(final Activity a, String title, List<Menus.Item> items, final Sub sub, final String arrMode, final long arrId,
                               final File newIn, final CmdMenu.Picked pick, boolean top) {
        final List<String> labels = new ArrayList<String>();
        final List<Runnable> taps = new ArrayList<Runnable>(), holds = new ArrayList<Runnable>();
        for (Menus.Item it : items) {
            final DbMore.Entry e = it.e;
            if (e.kind == DbMore.E_FILE) {
                final File f = new File(e.path);
                if (!f.isFile()) continue;
                Program p = Program.read(f);
                labels.add((p.isProgram() ? p.icon : "▶") + "  " + p.title(f));
                taps.add(new Runnable() {
                    @Override public void run() { use(a, f, newIn, pick); }
                });
                holds.add(new Runnable() {
                    @Override public void run() { if (Reserved.isSystem(f)) { if (!Reserved.isNew(f)) CmdMenu.saveAs(a, f); } else BatchUi.edit(a, f); }
                });
            } else if (e.kind == DbMore.E_LIST) {
                labels.add(e.label + "  ▸");
                taps.add(new Runnable() {
                    @Override public void run() { sub.open(e); }
                });
                holds.add(null);
            } else {
                labels.add("📜 " + e.label + "  ▸");
                taps.add(new Runnable() {
                    @Override public void run() { CmdMenu.found(a, e.label, e.cond == null ? "" : e.cond, pick); }
                });
                holds.add(null);
            }
        }
        if (top && pick != null) CmdMenu.extras(a, pick, labels, taps, holds);
        if (arrMode != null && arrId != 0) {
            labels.add(Words.CmdM.ARRANGE);
            taps.add(new Runnable() {
                @Override public void run() { DbListScreen.open(a, arrMode, arrId); }
            });
            holds.add(null);
        }
        Ui.list(a, title, labels, new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface d, int w) { taps.get(w).run(); }
        }, new Ui.Index1() {
            @Override public void ok(int w) { if (holds.get(w) != null) holds.get(w).run(); }
        });
    }

    // ================= מ0.23: יצוא וייבוא — קובצי פקודות מוכנים ב־⭐ =================

    /** [צלמית, שם, שורת הפקודה] — המקומות הפתוחים נשאלים בהרצה (הטופס — עם בורר הרשימות של המסד). */
    static List<String[]> dbTools() {
        List<String[]> out = new ArrayList<String[]>();
        out.add(new String[]{"📤", Words.DbCmd.TOOL_EXPORT, "COPY " + ListCmd.MANUAL + " \"<" + Words.DbCmd.HOLE_EXPORT + ">\" "
                + ListCmd.FILE_WORD + " \"*\""});   // תחביר
        out.add(new String[]{"📥", Words.DbCmd.TOOL_IMPORT, "COPY " + ListCmd.FILE_WORD + " \"<" + Words.DbCmd.HOLE_FILE + ">\" "
                + ListCmd.MANUAL + " \"<" + Words.DbCmd.HOLE_INTO + ">\""});   // תחביר
        // מ0.25 (באג 28; המשתמש: "יכולים להישאר בפקודות. ותרשום לך להכניס אותם לשמורים לתוכנה"): הכלים שלא עברו לבית
        // (סל והגדרות — כבר בפקודות המערכת). כפולים — גם כאן, עם ברירות המחדל.
        out.add(new String[]{"⧉", Words.DbCmd.TOOL_DUPS, Verbs.toolLine(FnDup.KIND, new java.util.LinkedHashMap<String, String>())});
        out.add(new String[]{"📌", Words.DbCmd.TOOL_BAR, Verbs.toolLine("פס", new java.util.LinkedHashMap<String, String>())});   // תחביר
        out.add(new String[]{"🔗", Words.DbCmd.TOOL_APP, Verbs.toolLine("אפליקציה", new java.util.LinkedHashMap<String, String>())});   // תחביר; בלי חבילה — נשאל
        // מ0.41 (המשתמש, 12.11.2026: "להוסיף כפתור שמריץ אותו. וכך הוא הופך להיות חלק אינטגרלי מהתוכנה"): העורך האינטראקטיבי
        out.add(new String[]{"📝", Words.DbCmd.TOOL_EDITOR, Verbs.OPEN + " " + Verbs.PLUGIN + " \"" + Words.DbCmd.TOOL_EDITOR + "\""});
        return out;
    }

    /** הקבצים בתיקיית פקודות המערכת (כמו Reserved.writeSystem: מה שהשתנה — נכתב מחדש). */
    static List<File> writeTools() {
        List<File> out = new ArrayList<File>();
        File dir = Reserved.sysDir();
        for (String[] t : dbTools()) {
            File f = new File(dir, t[1] + "." + Batch.EXT);
            String text = Program.ICON + " " + t[0] + "\n" + Program.NAME + " " + t[1] + "\n" + t[2] + "\n";
            try {
                if (!f.isFile() || !text.equals(Files.readUtf8(f))) {
                    if (!dir.isDirectory()) dir.mkdirs();
                    Files.writeAtomic(f, text);
                }
                out.add(f);
            } catch (Throwable e) {
                Log.e("DbCmdMenu.writeTools", e);
            }
        }
        return out;
    }

    /**
     * ברשימה "שמור לתוכנה / פקודות" של המסד — הכלים שחסרים נכנסים (מה שהמשתמש הוציא — לא חוזר אחרי הפעם הראשונה:
     * נרשם במאפיין של הרשימה). אין רשימה כזו — כלום. מחזיר את שמות מה שנוסף.
     */
    static List<String> ensureTools(Store s, String mode, List<File> files) {
        final List<String> added = new ArrayList<String>();
        final long cmds = Menus.at(s, Menus.root(s, mode), Menus.CMDS);     // מ0.41: גם באופן התפריטים (שם השורש הוא הבית)
        if (cmds == 0) return added;
        String had = s.prop(cmds, P_TOOLS);
        final java.util.Set<String> seen = new java.util.LinkedHashSet<String>();
        if (had != null) for (String x : had.split("\n")) if (!x.trim().isEmpty()) seen.add(x.trim());
        final List<String> paths = new ArrayList<String>();
        for (File f : files) if (seen.add(f.getName())) {
            paths.add(f.getAbsolutePath());
            added.add(f.getName().replace("." + Batch.EXT, ""));
        }
        if (paths.isEmpty()) return added;
        DbFiles.add(s, cmds, paths, null, false);
        StringBuilder sb = new StringBuilder();
        for (String x : seen) sb.append(sb.length() == 0 ? "" : "\n").append(x);
        final String v = sb.toString();
        s.atomic(new Store.Work() {
            @Override public void run(Store t) { t.setProp(cmds, P_TOOLS, v); }
        }, true);
        return added;
    }

    /** מאפיין ברשימת הפקודות: אילו כלים כבר נוספו פעם (שורה לכל אחד). */
    static final String P_TOOLS = "tools";

    private static final java.util.Set<String> ensured = new java.util.HashSet<String>();

    /** פעם אחת לכל אופן בכל הפעלה, ברקע; מה שנוסף — הודעה. */
    static void ensureToolsAsync(final Activity a, final String mode) {
        if (mode == null || !ensured.add(mode)) return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                SqlStore s = null;
                List<String> added = null;
                try {
                    List<File> fs = writeTools();
                    s = SqlStore.open(DbScreen.dbFile(a, mode), new IdCounter(DbScreen.counterFile()));
                    added = ensureTools(s, mode, fs);
                } catch (Throwable t) {
                    Log.e("DbCmdMenu.ensureTools", t);
                } finally {
                    if (s != null) s.close();
                }
                final List<String> got = added;
                if (got != null && !got.isEmpty())
                    a.runOnUiThread(new Runnable() {
                        @Override public void run() { Ui.toast(a, Words.DbCmd.toolsAdded(got)); }
                    });
            }
        }).start();
    }
}
