package com.tikiyot.vfm;

// מ0.22: תפריט הפקודות ⭐ ו"פתח תפריט <רשימה>" — מהמסד, כשעובדים במסד. אותה רשימה כמו בישן ("שמור לתוכנה / פקודות"
// כברירת מחדל): באופן שפתוח, ואם אין שם — באופן אחר שיש בו את הרשימה (מ0.23: לפי המסדים — otherMode). אין במסד — התפריט
// הישן, כמו קודם. קובצי הפקודות — פקודות; תתי־רשימות — תפריטי משנה; חכמה — מה שהיא מוצאת. נגיעה ארוכה — עריכה.
// אחראי: התפריט מהמסד (הרשימה — DbMore.menu). אסור לו: לגעת במנוע הישן.

import android.app.Activity;
import android.content.DialogInterface;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

final class DbCmdMenu {

    private DbCmdMenu() { }

    /** false — אין במסד רשימה כזו (התפריט הישן ממשיך). */
    static boolean show(Activity a, List<String> path, File newIn, CmdMenu.Picked pick, String screen, boolean top) {
        String mode = DbListScreen.currentMode;
        long id = find(a, mode, path);
        if (id == 0) {                                                       // מ0.23: האופן "שבשימוש" — לפי המסדים, לא לפי הישן
            String other = otherMode(a, mode, path);
            if (other != null) {
                id = find(a, other, path);
                mode = other;
            }
        }
        if (id == 0) return false;
        open(a, mode, id, path, newIn, pick, screen, top);
        return true;
    }

    /**
     * מ0.23: אופן אחר שיש במסד שלו את הרשימה — קודם הדוגמאות (כמו Reserved.modeInUse בישן), אחר כך השאר לפי הסדר.
     * null — אין.
     */
    static String otherMode(Activity a, String mode, List<String> path) {
        List<String> all = new ArrayList<String>(DbScreen.dbModes(a));
        if (all.remove(Examples.MODE)) all.add(0, Examples.MODE);
        for (String m : all) if (!m.equals(mode) && find(a, m, path) != 0) return m;
        return null;
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

    private static void open(final Activity a, final String mode, final long id, final List<String> path, final File newIn,
                             final CmdMenu.Picked pick, final String screen, final boolean top) {
        List<DbMore.Entry> es;
        SqlStore s = null;
        try {
            s = SqlStore.openReadOnly(DbScreen.dbFile(a, mode));
            es = DbMore.menu(s, id);
        } catch (Throwable t) {
            Log.e("DbCmdMenu.open", t);
            Ui.toast(a, Words.Db.failed(String.valueOf(t.getMessage())));
            return;
        } finally {
            if (s != null) s.close();
        }
        final List<String> labels = new ArrayList<String>();
        final List<Runnable> taps = new ArrayList<Runnable>(), holds = new ArrayList<Runnable>();
        for (final DbMore.Entry e : es) {
            if (e.kind == DbMore.E_FILE) {
                final File f = new File(e.path);
                if (!f.isFile()) continue;
                Program p = Program.read(f);
                labels.add((p.isProgram() ? p.icon : "▶") + "  " + p.title(f));
                taps.add(new Runnable() {
                    @Override public void run() { if (Reserved.isNew(f)) CmdMenu.create(a, newIn, pick); else CmdMenu.useFile(a, f, pick); }
                });
                holds.add(new Runnable() {
                    @Override public void run() { if (Reserved.isSystem(f)) { if (!Reserved.isNew(f)) CmdMenu.saveAs(a, f); } else BatchUi.edit(a, f); }
                });
            } else if (e.kind == DbMore.E_LIST) {
                final List<String> sub = new ArrayList<String>(path);
                sub.add(e.label);
                labels.add(e.label + "  ▸");
                taps.add(new Runnable() {
                    @Override public void run() { open(a, mode, e.id, sub, newIn, pick, screen, false); }
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
        if (top && pick != null) CmdMenu.extras(a, screen, pick, labels, taps, holds);
        labels.add(Words.CmdM.ARRANGE);
        taps.add(new Runnable() {
            @Override public void run() { DbListScreen.open(a, mode, id); }
        });
        holds.add(null);
        Ui.list(a, top && path.equals(Reserved.CMDS_PATH) ? Words.CmdM.TITLE : path.get(path.size() - 1), labels,
                new DialogInterface.OnClickListener() {
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
        final long cmds = DbPaths.find(s, mode, Reserved.CMDS_PATH);
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
