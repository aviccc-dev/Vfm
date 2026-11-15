package com.tikiyot.vfm;

// מבנה 165 (סידור תיקיית הפקודות): שורות של רשימות בתוך קובץ פקודות — התכנון שלהן (planList), והסריקה של תיקייה
// שנכנסת לרשימה (scan). הפקודות עצמן — DbListCmd. עד כאן ישב בתוך Batch. אחראי: לתכנן שורת רשימות. אסור לו: להריץ.

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class ListLines {

    private ListLines() { }

    /** 1.24.0: שורה עם רשימה — הצד שבכונן מפוענח כאן (מסלול, תבנית, סינון), והשאר — ListCmd. */
    static void planList(Batch.Step s, String cmd, ListCmd.Parsed lp, List<String> sw, File here, Map<String, String> vars, Sim sim) {
        if (cmd.equals(Syntax.VERIFY)) {                                               // מ0.14: ודא — הצד הראשון רשימה, השאר תנאים
            if (!DbVerbs.active()) { s.error = Words.DbCmd.VERIFY_DB_ONLY; return; }
            if (lp.sides.size() >= 2 && !lp.sides.get(1).list()
                    && (DbListCmd.W_SAME.equals(lp.sides.get(1).raw) || DbListCmd.W_EQUAL.equals(lp.sides.get(1).raw))) {   // מבנה 87–88: … זהה … · … שווה …
                if (lp.sides.size() != 3 || !sw.isEmpty() || !lp.sides.get(0).list() || !lp.sides.get(2).list()) {
                    s.error = Words.DbCmd.VERIFY_SAME_USAGE;
                    return;
                }
                ListCmd.resolve(lp.sides.get(0), vars);
                ListCmd.resolve(lp.sides.get(2), vars);
                DbListCmd.planVerifySame(s, lp.sides.get(0), lp.sides.get(2), DbListCmd.W_SAME.equals(lp.sides.get(1).raw), sim);
                return;
            }
            List<String> conds = new ArrayList<String>(sw);
            for (int i = 1; i < lp.sides.size(); i++) conds.add(lp.sides.get(i).raw);
            ListCmd.Side a = lp.sides.get(0);
            if (a.list()) ListCmd.resolve(a, vars);
            else {                                                              // מ0.18: ודא "קובץ" קיים / חסר — בכונן
                String[] err = {null};
                String p = Parse.path(a.raw, here, vars, err);
                if (p == null) { s.error = err[0]; return; }
                DbListCmd.planVerifyFile(s, new File(p), conds);
                return;
            }
            DbListCmd.planVerify(s, a, conds, sim);
            return;
        }
        String bad = listSwitch(cmd, lp, sw);                                 // מ0.40 (ממצא 41): מתג שלא שייך לשורה — שגיאה
        if (bad != null) { s.error = bad; return; }
        List<String> fsFiles = new ArrayList<String>();
        List<String> fsDirs = new ArrayList<String>();                         // מבנה 115: תיקייה שלמה מהכונן אל רשימה
        boolean dirToList = cmd.equals("COPY") && lp.sides.size() == 2 && lp.sides.get(1).list();
        File fsDest = null;
        for (int i = 0; i < lp.sides.size(); i++) {
            ListCmd.Side x = lp.sides.get(i);
            if (x.list()) {
                if (Parse.var(Batch.V_SELECTED).equals(x.raw)) x.picked = true;             // מבנה 85: מה שנבחר במסך הרשימות — מספרים, לא דרך של שמות
                else ListCmd.resolve(x, vars);
                continue;
            }
            String[] err = {null};
            String p = Parse.path(x.raw, here, vars, err);
            if (p == null) { s.error = err[0]; return; }
            if (p.equals(Batch.SELECTED_MARK)) {                                      // מבנה 114: הקבצים שנבחרו במסך — אל רשימה
                Batch.Selection sel = sim.selected;
                if (i != 0 || sel == null || (sel.files.isEmpty() && sel.dirs.isEmpty())) { s.error = Words.Cmd.NOTHING_SELECTED; return; }
                if (!sel.dirs.isEmpty() && !dirToList) { s.error = Words.ListCmdW.FOLDER_TO_LIST; return; }
                for (String f : sel.files) {
                    if (!sim.isFile(new File(f))) { s.error = Words.Cmd.notFound(f); return; }
                    fsFiles.add(new File(f).getAbsolutePath());
                }
                for (String d : sel.dirs) {                                     // מבנה 115
                    if (!sim.isDir(new File(d))) { s.error = Words.FOLDER_NOT_FOUND + ": " + d; return; }
                    fsDirs.add(new File(d).getAbsolutePath());
                }
                continue;
            }
            // מבנה 115 (הכלל של מבנה 112): שם של תיקייה הוא התיקייה עצמה — אל רשימה היא נכנסת כרשימה בשמה, עם המבנה שלה
            // (עד כאן: הקבצים שבה). רק הקבצים שבה: "תיקייה\*".
            if (dirToList && i == 0 && !new File(p).getName().contains("*") && !new File(p).getName().contains("?") && sim.isDir(new File(p))) {
                fsDirs.add(new File(p).getAbsolutePath());
                continue;
            }
            if (i == lp.sides.size() - 1 && i > 0) fsDest = new File(p);
            else {
                Filter flt = Filter.of(sw);
                if (flt.error != null) { s.error = flt.error; return; }
                FileCmds.match(s, p, flt, false, sim);
                if (s.error != null) return;
                if (!s.dirs.isEmpty()) { s.error = Words.ListCmdW.FOLDER_TO_LIST; return; }
                fsFiles.addAll(s.files);
                s.files.clear();
            }
        }
        if (DbVerbs.active()) DbListCmd.plan(s, cmd, lp, sw, sim, fsFiles, fsDest, fsDirs);   // מ0.12: עובדים במסד
        else s.error = Words.DbCmd.NO_DB;                                    // מ0.26: המנוע הישן נמחק — רשימות רק במסד
    }

    /**
     * מבנה 115: תיקייה כמו שההדמיה רואה אותה (כולל מה ששורות קודמות יצרו) — לעץ מנותק, כמו Tree.scanFolder: עם המבנה
     * (תת־תיקיות — צמתים), או הקבצים בלבד (כולם, גם מתת־תיקיות, בצומת אחד).
     */
    static Tree.Node scan(Sim sim, Batch.Step s, File dir, boolean structure) {
        Tree.Node n = new Tree.Node();
        n.name = Names.cleanName(dir.getName());
        scanInto(n, n, dir, structure, sim, s, 0);
        return n;
    }

    static void scanInto(Tree.Node node, Tree.Node flat, File dir, boolean structure, Sim sim, Batch.Step s, int depth) {
        for (File x : sim.files(dir))
            if (Batch.visible(s, x)) (structure ? node : flat).entries.add(new Tree.Entry(x.getAbsolutePath(), sim.real(x).length()));
        if (depth > 60) return;
        for (File d : sim.subdirs(dir)) {
            if (Vols.skip(d, false)) continue;
            scanInto(structure ? Tree.newChild(node, node.uniqueChildName(d.getName())) : node, flat, d, structure, sim, s, depth + 1);
        }
    }

    /**
     * מ0.40 (ממצא 41): המתגים שמותרים בשורה עם רשימה — לפי צורת השורה. הקבצים של רשימה אל תיקייה (העתק / העבר): /עמוק,
     * /תפוס:…, /מכיל:; מהכונן אל רשימה: /מכיל:; השוואה: /תנאים. בכל שורה אחרת — אין מתגים. null — תקין.
     */
    static String listSwitch(String cmd, ListCmd.Parsed lp, List<String> sw) {
        List<ListCmd.Side> sd = lp.sides;
        boolean two = sd.size() == 2, transfer = cmd.equals("COPY") || cmd.equals("MOVE");
        boolean toFolder = transfer && two && sd.get(0).list() && !sd.get(1).list() && sd.get(1).kind == ListCmd.FS;
        boolean fromDisk = transfer && two && !sd.get(0).list();
        for (String x : sw) {
            String u = x.toUpperCase(Locale.ROOT);
            if (u.equals("/Q") || u.equals("/F")) continue;
            if (x.startsWith("/" + Syntax.SW_DATE) || x.startsWith("/" + Syntax.SW_SIZE)) return Words.ListCmdW.NO_FILTER;
            if (x.startsWith("/" + Syntax.SW_CONTAINS)) { if (toFolder || fromDisk) continue; return Words.ListCmdW.NO_FILTER; }
            if (x.equals(ListCmd.SW_CONDS) || x.equals(ListCmd.SW_DIFF)) { if (cmd.equals("COMP")) continue; return Words.Cmd.switchNotHere(x, null); }
            boolean take = u.equals("/S") || x.equals(Syntax.SW_KEEP) || u.equals("/Y") || u.equals("/-Y") || x.startsWith(Syntax.SW_TAKEN);
            if (take && toFolder) continue;
            if (fromDisk && cmd.equals("COPY") && sd.get(1).list() && (x.equals(ListCmd.SW_LIVE) || x.equals(ListCmd.SW_FLAT)
                    || x.equals(Syntax.SW_TAKEN + Syntax.TAKEN_SKIP) || x.equals(Syntax.SW_TAKEN + Syntax.TAKEN_RENAME) || x.equals(Syntax.SW_TAKEN + Syntax.TAKEN_STOP)))
                continue;                                                       // מבנה 115: תיקייה מהכונן אל רשימה
            if (cmd.equals("MD") && (x.equals(Syntax.SW_TAKEN + Syntax.TAKEN_SKIP) || x.equals(Syntax.SW_TAKEN + Syntax.TAKEN_RENAME) || x.equals(Syntax.SW_TAKEN + Syntax.TAKEN_STOP)))
                continue;                                                       // מבנה 109: "צור תיקייה ידנית … /תפוס:לדלג"
            return Words.Cmd.switchNotHere(Commands.switchWord(x), null);
        }
        return null;
    }
}
