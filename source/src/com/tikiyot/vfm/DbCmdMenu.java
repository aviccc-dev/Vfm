package com.tikiyot.vfm;

// מ0.22: תפריט הפקודות ⭐ ו"פתח תפריט <רשימה>" — מהמסד, כשעובדים במסד. אותה רשימה כמו בישן ("שמור לתוכנה / פקודות"
// כברירת מחדל): באופן שפתוח, ואם אין שם — באופן ש"שמור לתוכנה" שלו בשימוש (Reserved.modeInUse). אין במסד — התפריט
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
        if (id == 0) {
            String other = Reserved.modeInUse();
            if (other != null && !other.equals(mode) && DbScreen.dbModes(a).contains(other)) {
                id = find(a, other, path);
                if (id != 0) mode = other;
            }
        }
        if (id == 0) return false;
        open(a, mode, id, path, newIn, pick, screen, top);
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
}
