package com.tikiyot.vfm;

// מבנה 166 (סידור תיקיית הפקודות): התכנון של פקודות הקבצים — העתק, העבר, מחק, שנה, צור תיקייה, מחק תיקייה, פתח בסוף —
// ושמירת המסלול (/S), ההתאמה של מקור ויעד, ומה שנבחר במסך (%נבחרים%). עד כאן ישב בתוך Batch.
// כאן יושבות ארבע הגרסאות "מהמסך" (makeSelected, renameSelected, fromSelected / selected) — ה8–ה11, מחכות להחלטת המשתמש.
// אחראי: לתכנן שורת קבצים. אסור לו: להריץ.

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class FileCmds {

    private FileCmds() { }

    static void planFile(Batch.Step s, String cmd, List<String> args, List<String> paths, List<String> sw, Sim sim) {
        Filter flt = Filter.of(sw);
        if (flt.error != null) {
            s.error = flt.error;
            return;
        }
        if (!flt.contains.isEmpty()) s.contains = flt.contains;               // מ0.40
        // מ0.40 (ממצא 41; הכלל של המשתמש: "אין מצב שיש שגיאה והקובץ רץ ואף אחד לא יודע"): מתג שלא שייך לפקודה — שגיאה,
        // ולא בליעה בשקט. מה מותר — מההגדרה האחת (Commands). /Q ו־/F — תאימות ל־bat, מתעלמים בכוונה.
        Commands.Def def = Commands.get(cmd);
        boolean fromCmp = (cmd.equals("COPY") || cmd.equals("MOVE")) && paths.size() == 2
                && (paths.get(0).equals(Syntax.ONLY_A_MARK) || paths.get(0).equals(Syntax.ONLY_B_MARK));
        if (def != null) for (String x : sw) {
            String u = x.toUpperCase(Locale.ROOT);
            if (u.equals("/Q") || u.equals("/F")) continue;
            boolean ok = false;
            for (Commands.Param p : def.params) if (p.matches(x)) ok = true;
            if (!ok) { s.error = Words.Cmd.switchNotHere(Commands.switchWord(x), Commands.word(def)); return; }
            if (x.startsWith(Syntax.SW_LIMIT) && !fromCmp) { s.error = Words.Cmd.LIMIT_ONLY_COMPARE; return; }
        }
        if (fromCmp && !flt.none()) { s.error = Words.Cmd.NO_FILTER_ON_COMPARE; return; }
        for (String x : sw) {
            String u = x.toUpperCase(Locale.ROOT);
            if (u.equals("/Y") || x.equals(Syntax.SW_TAKEN + Syntax.TAKEN_REPLACE)) s.replace = true;
            if (x.equals(Syntax.SW_TAKEN + Syntax.TAKEN_SKIP)) s.skipTaken = true;
            if (x.equals(Syntax.SW_TAKEN + Syntax.TAKEN_STOP)) s.failTaken = true;          // 1.20.0
            if (x.equals(Syntax.SW_TAKEN + Syntax.TAKEN_RENAME)) s.renameTaken = true;      // מ0.40
            if (u.equals("/S") || x.equals(Syntax.SW_KEEP)) s.recursive = true;
            if (u.equals("/A") || u.startsWith("/A:H") || u.equals("/A:")) s.hidden = true;
            if (x.startsWith(Syntax.SW_TAKEN) && !Parse.takenValue(x)) s.error = Parse.takenUnknown(x);
            if (x.startsWith(Syntax.SW_LIMIT) && Batch.limit(x) < 0) s.error = Words.Cmd.switchUnknown(x);
            if (x.startsWith(Syntax.SW_LIST)) {                                      // 1.22.0
                s.listName = x.substring(Syntax.SW_LIST.length()).trim();
                if (s.listName.isEmpty() || !cmd.equals("COMP")) s.error = Words.Cmd.LIST_ONLY_COMP;
            }
            if (x.startsWith(Syntax.SW_STATE)) {                                     // 1.21.0
                s.stateWhen = State.when(x.substring(Syntax.SW_STATE.length()));
                if (s.stateWhen == Long.MIN_VALUE) s.error = Words.Cmd.switchUnknown(x);
                else if (!cmd.equals("COPY")) s.error = Words.Cmd.STATE_ONLY_COPY;
            }
        }
        if (s.error != null) return;
        if (cmd.equals("MD")) {
            fileMd(s, cmd, args, paths, sw, flt, sim);
        } else if (cmd.equals("RD")) {
            fileRd(s, cmd, args, paths, sw, flt, sim);
        } else if (cmd.equals("DEL")) {
            fileDel(s, cmd, args, paths, sw, flt, sim);
        } else if ((cmd.equals("COPY") || cmd.equals("MOVE")) && !paths.isEmpty() && paths.get(0).equals(Syntax.SELECTED_MARK)) {
            fromSelected(s, cmd, paths, flt, sim);                               // מבנה 77: מה שנבחר במסך — אל תיקייה
        } else if (cmd.equals("COPY") && paths.size() == 2 && paths.get(1).equals(Syntax.SHARE_MARK)) {   // 203: לשיתוף
            fileShare(s, cmd, args, paths, sw, flt, sim);
        } else if (cmd.equals("COPY") && s.stateWhen != Long.MIN_VALUE) {         // 1.21.0: שחזור
            CompareCmds.restore(s, args, paths, flt, sim);
        } else if (cmd.equals("COMP")) {                                      // 1.20.0
            CompareCmds.comp(s, args, paths, sim);
        } else if ((cmd.equals("COPY") || cmd.equals("MOVE")) && paths.size() == 2
                && (paths.get(0).equals(Syntax.ONLY_A_MARK) || paths.get(0).equals(Syntax.ONLY_B_MARK))) {
            CompareCmds.fromCompare(s, cmd, paths, sw, sim);
        } else if ((cmd.equals("COPY") || cmd.equals("MOVE")) && s.recursive) {     // 1.19.0: שמירת מסלול
            keep(s, cmd, args, paths, flt, sim);
        } else if (cmd.equals("COPY") || cmd.equals("MOVE")) {
            fileCopy(s, cmd, args, paths, sw, flt, sim);
        } else if (cmd.equals("REN")) {
            fileRen(s, cmd, args, paths, sw, flt, sim);
        } else if (cmd.equals("START")) {
            fileStart(s, cmd, args, paths, sw, flt, sim);
        } else {
            s.error = Words.Cmd.unknownCommand(cmd) + " (" + Batch.join(java.util.Arrays.asList(Commands.fileNames()), " ") + ")";
        }
    }

    /** מבנה 176: הענף מתוך planFile. */
    private static void fileMd(Batch.Step s, String cmd, List<String> args, List<String> paths, List<String> sw, Filter flt, Sim sim) {
        if (paths.size() != 1) { s.error = Commands.usage(cmd); return; }
        File d = new File(paths.get(0));
        if (d.getName().equals(Syntax.NAME_TOKEN)) {                               // מבנה 79: "%יעד%\%שם-חדש%" — תיקייה חדשה מהמסך
            d = makeSelected(s, cmd, d, sim);                                  // מבנה 197 (ה9): רק השם מהבחירה; הכלל — אחד, כאן
            if (d == null) return;
        }
        for (String x : sw)                                                 // מבנה 109: "להחליף" / "לשאול" — לא לתיקייה; לא בולעים בשקט
            if (x.toUpperCase(Locale.ROOT).equals("/Y") || x.equals(Syntax.SW_TAKEN + Syntax.TAKEN_REPLACE) || x.equals(Syntax.SW_TAKEN + Syntax.TAKEN_ASK)) {
                s.error = Words.Cmd.MD_TAKEN_ONLY;
                return;
            }
        // מבנה 197 (ה9; המשתמש, 16.11.2026: "שיעצור מההתחלה תמיד, וייתן הסבר"): שם לא תקין, ותיקייה בתוך תיקיית התוכנה — נתפסים
        // כבר כאן, גם בשורה כתובה (עד כאן — רק בהרצה). אותן שתי בדיקות, ובאותו סדר, כמו בביצוע עצמו (Physical.makeFolder).
        String bad = DiskRules.badName(d.getName());
        if (bad != null) { s.error = bad; return; }
        if (DiskRules.programFile(d.getAbsolutePath()) != null) { s.error = Words.Disk.NO_FOLDER_IN_APP; return; }
        if (sim.isDir(d) && s.renameTaken) {                               // מבנה 109: /תפוס:לשנות-שם — בשם פנוי "(2)"
            File free = d;
            for (int k = 2; sim.isDir(free) || sim.isFile(free); k++) free = new File(d.getParentFile(), d.getName() + " (" + k + ")");
            d = free;
        } else if (sim.isDir(d)) {
            if (s.skipTaken) {                                              // מבנה 109: /תפוס:לדלג — "המשך לשורה הבאה"
                s.noop = true;
                s.dest = d;
                s.describe = Words.Cmd.folderSkipped(d.getName());
                return;
            }
            s.error = Words.Cmd.folderTaken(d.getName());                  // מבנה 109 (המשתמש): "סירוב, אלא אם כן יש הוראה להמשיך"
            return;
        }
        if (sim.isFile(d)) { s.error = Words.Cmd.exists(d.getName()); return; }
        s.dest = d;
        s.describe = Words.Cmd.newFolder(Bidi.path(d.getAbsolutePath()));
        sim.dirs.add(d.getAbsolutePath());
        sim.gone.remove(d.getAbsolutePath());                                // מ0.30 (ממצא 35): RD ואחריו MD לאותה תיקייה — היא קיימת
        for (File up = d.getParentFile(); up != null && !sim.isDir(up); up = up.getParentFile()) {   // מ0.24: כמו mkdirs —
            sim.dirs.add(up.getAbsolutePath());                                                    // גם התיקיות שמעל
            sim.gone.remove(up.getAbsolutePath());
        }
    }

    /** מבנה 176: הענף מתוך planFile. */
    private static void fileRd(Batch.Step s, String cmd, List<String> args, List<String> paths, List<String> sw, Filter flt, Sim sim) {
        if (paths.size() != 1) { s.error = Commands.usage(cmd); return; }
        File d = new File(paths.get(0));
        if (!sim.isDir(d)) { s.error = Words.FOLDER_NOT_FOUND + ": " + args.get(0); return; }
        if (!s.recursive && (!sim.files(d).isEmpty() || sim.hasSubdirs(d))) {
            s.error = Words.Cmd.FOLDER_NOT_EMPTY;
            return;
        }
        s.dirs.add(d.getAbsolutePath());
        if (!sourcesAllowed(s, Words.Disk.CANT_DELETE, sim)) { s.dirs.clear(); return; }   // מבנה 201 (ה11): תיקייה מוגנת — כבר בתכנון
        s.describe = Words.Cmd.folderToTrash(Bidi.path(d.getAbsolutePath()));
        sim.wipe(d.getAbsolutePath());
    }

    /** מבנה 176: הענף מתוך planFile. */
    private static void fileDel(Batch.Step s, String cmd, List<String> args, List<String> paths, List<String> sw, Filter flt, Sim sim) {
        if (paths.size() != 1) { s.error = Commands.usage(cmd); return; }
        if (s.recursive) { s.error = Words.Cmd.DEL_S; return; }
        if (paths.get(0).equals(Syntax.SELECTED_MARK)) {                           // מבנה 73: מה שנבחר במסך — קבצים, וגם תיקיות שלמות
            selected(s, flt, sim, cmd);
            if (s.error != null) return;
            List<String> what = new ArrayList<String>();
            if (!s.files.isEmpty() || s.dirs.isEmpty()) what.add(Words.files(s.files.size()));
            if (!s.dirs.isEmpty()) what.add(Words.folders(s.dirs.size()));
            s.describe = Words.Cmd.toTrash(Batch.join(what, Words.Cmd.AND)) + filtered(s);
            for (String d : s.dirs) sim.wipe(d);
        } else {
            match(s, paths.get(0), flt, false, sim);
            if (s.error == null && !sourcesAllowed(s, Words.Disk.CANT_DELETE, sim)) return;   // מבנה 201 (ה11)
            if (s.error == null) s.describe = Words.Cmd.toTrash(Words.files(s.files.size())) + filtered(s);
        }
        for (String f : s.files) {
            sim.gone.add(f);
            sim.added.remove(f);
        }
    }

    /** 203: לשיתוף — מבנה 176: הענף מתוך planFile. */
    private static void fileShare(Batch.Step s, String cmd, List<String> args, List<String> paths, List<String> sw, Filter flt, Sim sim) {
        s.share = true;
        match(s, paths.get(0), flt, true, sim);                          // תיקייה — כמו שהיא (תיארז בשמה)
        if (s.error != null) return;
        if (!s.dirs.isEmpty() && !flt.none()) {
            s.error = Words.Cmd.filterOnFolder(cmd + " \"" + args.get(0) + "\\*.*\" …");
            return;
        }
        List<String> what = new ArrayList<String>();
        if (!s.files.isEmpty() || s.dirs.isEmpty()) what.add(Words.files(s.files.size()));
        if (!s.dirs.isEmpty()) what.add(Words.folders(s.dirs.size()));
        s.describe = Words.Cmd.toShare(Batch.join(what, Words.Cmd.AND)) + filtered(s);
    }

    /** מבנה 176: הענף מתוך planFile. */
    private static void fileCopy(Batch.Step s, String cmd, List<String> args, List<String> paths, List<String> sw, Filter flt, Sim sim) {
        if (paths.size() != 2) { s.error = Commands.usage(cmd); return; }
        File dest = new File(paths.get(1));
        if (!sim.isDir(dest)) {
            s.error = Words.Cmd.NO_DEST;
            return;
        }
        String noDest = sim.lib == null ? null : DiskRules.noDest(sim.lib, dest);   // מבנה 201 (ה11): יעד בתיקיית התוכנה או בסל — כבר בתכנון
        if (noDest != null) { s.error = noDest; return; }
        s.dest = dest;
        // מבנה 112 (ה7, המשתמש: "כמו במסך"): שם של תיקייה הוא התיקייה עצמה — גם בהעתקה (עד כאן: בהעתקה — הקבצים שבתוכה;
        // בהעברה ומהמסך — התיקייה). מה שבתוכה בלבד: "תיקייה\*".
        match(s, paths.get(0), flt, true, sim);
        if (s.error != null) return;
        if (!sourcesAllowed(s, cmd.equals("MOVE") ? Words.Disk.CANT_MOVE : null, sim)) return;   // מבנה 201 (ה11)
        if (!s.dirs.isEmpty() && !flt.none()) {            // 121: סינון לא חל על תיקייה שלמה
            s.error = Words.Cmd.filterOnFolder(cmd + " \"" + args.get(0) + "\\*.*\" …");
            return;
        }
        for (String d : s.dirs)                                             // מבנה 43: תיקייה לתוך עצמה — כבר בתכנון (עד כאן: רק בהרצה)
            if (DiskRules.inside(dest, new File(d))) { s.error = Words.Disk.NOT_INTO_ITSELF; return; }
        arrange(s, cmd.equals("COPY"), dest, sim);
    }

    /** מבנה 176: הענף מתוך planFile. */
    private static void fileRen(Batch.Step s, String cmd, List<String> args, List<String> paths, List<String> sw, Filter flt, Sim sim) {
        if (paths.size() != 2 || args.get(1).contains("/") || args.get(1).contains("\\")) {
            s.error = Commands.usage(cmd);
            return;
        }
        if (paths.get(0).equals(Syntax.SELECTED_MARK) || paths.get(1).equals(Syntax.NAME_MARK)) {   // מבנה 78: מה שנבחר במסך, בשם שהוקלד בו
            renameSelected(s, cmd, paths, sim);
            return;
        }
        if (args.get(0).contains("*") || args.get(0).contains("?")) { s.error = Words.Cmd.REN_WILD; return; }
        rename(s, new File(paths.get(0)), args.get(0), args.get(1), sim);
    }

    /** מבנה 176: הענף מתוך planFile. */
    private static void fileStart(Batch.Step s, String cmd, List<String> args, List<String> paths, List<String> sw, Filter flt, Sim sim) {
        if (paths.isEmpty()) { s.error = Commands.usage(cmd); return; }
        if (paths.get(paths.size() - 1).equals(Syntax.HOME_MARK)) {      // 1.9.0: START %בית%
            s.dest = new File(Syntax.HOME_MARK);
            s.describe = Words.Cmd.HOME_AT_END;
            return;
        }
        File d = new File(paths.get(paths.size() - 1));       // "START "" תיקייה" — הכותרת הריקה יורדת
        String name = d.getName();
        if (name.contains("*") || name.contains("?")) {       // 186: תבנית — החדש ביותר מבין המתאימים
            File dir = d.getParentFile();
            if (dir == null || !sim.isDir(dir)) { s.error = Words.FOLDER_NOT_FOUND + ": " + (dir == null ? name : dir.getAbsolutePath()); return; }
            File best = null;
            int n = 0;
            for (File x : sim.files(dir))
                if (Parse.wild(name, x.getName()) && Batch.visible(s, x)) {
                    n++;
                    if (best == null || sim.real(x).lastModified() > sim.real(best).lastModified()) best = x;
                }
            if (best == null) { s.error = Words.Cmd.noMatch(name); return; }
            s.dest = best;
            s.describe = Words.Cmd.openAtEnd(Bidi.path(best.getAbsolutePath())) + (n > 1 ? Words.Cmd.newestOf(n) : "");
            return;
        }
        if (sim.isFile(d)) {                                   // 186: קובץ — נפתח בסוף (apk — התקנה)
            s.dest = d;
            s.describe = Words.Cmd.openAtEnd(Bidi.path(d.getAbsolutePath()));
            return;
        }
        if (!sim.isDir(d)) { s.error = Words.FOLDER_NOT_FOUND; return; }
        s.dest = d;
        s.describe = Words.Cmd.openAtEnd(Bidi.path(d.getAbsolutePath()));
    }

    /** מבנה 48: היעד הוא התיקייה שהפריט כבר בה — לפי המסלול, ואם לא — לפי הכונן (Physical.sameFile), פעם אחת לכל תיקייה. */
    static boolean here(File dest, File parent, Map<String, Boolean> asked) {
        if (parent == null) return false;
        if (dest.equals(parent)) return true;
        Boolean b = asked.get(parent.getAbsolutePath());
        if (b == null) asked.put(parent.getAbsolutePath(), b = dest.isDirectory() && parent.isDirectory() && DiskRules.sameFile(dest, parent));
        return b;
    }

    /**
     * 1.19.0: COPY/MOVE <תיקייה או תיקייה\תבנית> <יעד> /S — כל הקבצים שבתוכה, גם בתיקיות שבתוכה, נכתבים ביעד
     * באותו מסלול (כמו XCOPY /S). היעד נוצר אם אינו קיים. בלי תבנית ובלי סינון — גם תיקיות ריקות (כמו /E).
     */
    static void keep(Batch.Step s, String cmd, List<String> args, List<String> paths, Filter flt, Sim sim) {
        if (paths.size() != 2) { s.error = Commands.usage(cmd); return; }
        File dest = new File(paths.get(1));
        if (sim.isFile(dest)) { s.error = Words.Cmd.NO_DEST; return; }
        File src = new File(paths.get(0));
        String mask = null;
        if (src.getName().contains("*") || src.getName().contains("?")) {
            mask = src.getName();
            src = src.getParentFile();
        }
        if (src == null || !sim.isDir(src)) { s.error = Words.FOLDER_NOT_FOUND + ": " + args.get(0); return; }
        if (DiskRules.inside(dest, src)) { s.error = Words.Disk.NOT_INTO_ITSELF; return; }
        s.dest = dest;
        s.keepRoot = src.getAbsolutePath();
        walk(s, src, mask, flt, sim, mask == null && flt.none(), true);
        sim.dirs.add(dest.getAbsolutePath());
        for (java.util.Iterator<String> it = s.files.iterator(); it.hasNext(); ) {   // קובץ אחרי קובץ (מ0.40: לשנות-שם)
            String f = it.next();
            File to = new File(dest, DiskRules.rel(s.keepRoot, f));
            if (s.drop(sim, new File(f), to)) { it.remove(); continue; }
            for (File up = to.getParentFile(); up != null && !up.equals(dest); up = up.getParentFile()) sim.dirs.add(up.getAbsolutePath());
            sim.move(f, s.arrive(sim, to), cmd.equals("COPY"));
        }
        List<String> what = new ArrayList<String>();
        what.add(Words.files(s.files.size()));
        if (!s.dirs.isEmpty()) what.add(Words.Cmd.emptyFolders(s.dirs.size()));
        s.describe = Words.Cmd.transfer(cmd.equals("COPY"), Batch.join(what, Words.Cmd.AND), Bidi.path(dest.getAbsolutePath()))
                + Words.Cmd.KEEP_PATH + filtered(s);
        for (String d : s.dirs) sim.dirs.add(new File(dest, DiskRules.rel(s.keepRoot, d)).getAbsolutePath());
    }

    /** הקבצים בתיקייה ובתיקיות שבתוכה (מוסתרים — רק עם /A:H; קבצים זמניים של העתקה — אף פעם). */
    static boolean walk(Batch.Step s, File dir, String mask, Filter flt, Sim sim, boolean empties, boolean top) {
        boolean any = false;
        for (File x : sim.files(dir)) {
            if (!Batch.visible(s, x) || FileCopy.isPartial(x.getName())) continue;
            any = true;
            if (mask != null && !Parse.wild(mask, x.getName())) continue;
            s.total++;
            if (flt.ok(sim.real(x))) s.files.add(x.getAbsolutePath());
        }
        for (File k : sim.subdirs(dir)) {                                   // מ0.38 (הבדיקה הכללית): גם תיקייה ששורה קודמת יצרה
            if (!Batch.visible(s, k)) continue;
            any = true;
            walk(s, k, mask, flt, sim, empties, false);
        }
        if (!any && !top && empties) s.dirs.add(dir.getAbsolutePath());
        return any;
    }

    /** 1.20.0: %רק-במקור% / %רק-ביעד% — רק לבדם, כמקור של COPY או MOVE. */
    static String setsMisuse(String cmd, List<String> paths) {
        for (int i = 0; i < paths.size(); i++) {
            String p = paths.get(i);
            if (p.startsWith(Syntax.SELECTED_MARK)) {                                  // מבנה 73, 77: %נבחרים% כיחידה — רק לבדו, כמקור של "מחק", "העתק", "העבר"
                if (!(cmd.equals("DEL") || cmd.equals("COPY") || cmd.equals("MOVE") || cmd.equals("REN")) || i != 0 || !p.equals(Syntax.SELECTED_MARK))
                    return Words.Cmd.SELECTED_ONLY_SOURCE;
                continue;
            }
            if (!p.startsWith(Syntax.ONLY_A_MARK) && !p.startsWith(Syntax.ONLY_B_MARK)) continue;
            if (!(cmd.equals("COPY") || cmd.equals("MOVE")) || i != 0 || !(p.equals(Syntax.ONLY_A_MARK) || p.equals(Syntax.ONLY_B_MARK)))
                return Words.Cmd.SETS_ONLY_SOURCE;
        }
        return null;
    }

    /** 1.9.0: "%בית%" — רק לבדו, אחרי START. */
    static String homeMisuse(String cmd, List<String> paths) {
        for (String p : paths) {
            if (!p.startsWith(Syntax.HOME_MARK)) continue;
            if (!cmd.equals("START") || !p.equals(Syntax.HOME_MARK)) return Words.Cmd.HOME_ONLY_START;
        }
        return null;
    }

    /** 203: "%שיתוף%" — רק כיעד של COPY, ובלי מסלול אחריו. */
    static String shareMisuse(String cmd, List<String> paths) {
        for (int i = 0; i < paths.size(); i++) {
            String p = paths.get(i);
            if (!p.startsWith(Syntax.SHARE_MARK)) continue;
            if (!p.equals(Syntax.SHARE_MARK)) return Words.Cmd.SHARE_NO_PATH;
            if (cmd.equals("MOVE") && i == 1) return Words.Cmd.SHARE_NO_MOVE;
            if (!cmd.equals("COPY") || i != 1) return Words.Cmd.SHARE_ONLY_COPY;
        }
        return null;
    }

    /** מקור: קובץ, תבנית (*.pdf), או תיקייה (COPY — הקבצים שבתוכה; MOVE — התיקייה עצמה, כמו ב-bat). */
    static void match(Batch.Step s, String src, Filter flt, boolean moveDir, Sim sim) {
        File f = new File(src);
        String name = f.getName();
        if (name.contains("*") || name.contains("?")) {
            File dir = f.getParentFile();
            if (dir == null || !sim.isDir(dir)) { s.error = Words.FOLDER_NOT_FOUND + ": " + (dir == null ? src : dir.getAbsolutePath()); return; }
            for (File x : sim.files(dir))
                if (Parse.wild(name, x.getName()) && Batch.visible(s, x)) {
                    s.total++;
                    if (flt.ok(sim.real(x))) s.files.add(x.getAbsolutePath());
                }
            return;
        }
        if (sim.isDir(f)) {
            if (moveDir) {
                s.dirs.add(f.getAbsolutePath());
                return;
            }
            for (File x : sim.files(f))
                if (Batch.visible(s, x)) {
                    s.total++;
                    if (flt.ok(sim.real(x))) s.files.add(x.getAbsolutePath());
                }
            return;
        }
        if (sim.isFile(f)) {                                      // קובץ שנכתב במפורש — גם אם מוסתר
            s.total++;
            if (flt.ok(sim.real(f))) s.files.add(f.getAbsolutePath());
            return;
        }
        s.error = Words.Cmd.notFound(src);
    }

    /**
     * מבנה 77: סוף התכנון של העתקה או העברה אל תיקייה — מה יוצא מהשורה (כבר במקום, /תפוס:), ההדמיה, והתיאור. מקום אחד לשורה
     * כתובה ול"נבחרים" של המסך.
     */
    static void arrange(Batch.Step s, boolean copy, File dest, Sim sim) {
        // מ0.40: העברה אל התיקייה שהפריט כבר בה — כלום (עד כאן: "קובץ אחד" בתכנון, ו"כבר בתיקייה הזו" ככישלון בהרצה).
        // ובקובץ אחרי קובץ — כדי שקובץ שני זהה לראשון יראה אותו כבר ביעד (לשנות-שם).
        // מבנה 48: "כבר בתיקייה הזו" — גם כשהיעד הוא אותה תיקייה בכתיב אחר (קישור; אותיות גדולות וקטנות בכונן שאינו מבדיל).
        // המשפך: השאלה לכונן — פעם אחת לכל תיקיית מקור, ורק בהעברה.
        Map<String, Boolean> sameDir = new java.util.HashMap<String, Boolean>();
        for (java.util.Iterator<String> it = s.files.iterator(); it.hasNext(); ) {
            File f = new File(it.next());
            if (!copy && here(dest, f.getParentFile(), sameDir)) { it.remove(); s.inPlace++; continue; }
            File to = new File(dest, f.getName());
            if (s.drop(sim, f, to)) { it.remove(); continue; }
            sim.move(f.getAbsolutePath(), s.arrive(sim, to), copy);
        }
        for (java.util.Iterator<String> it = s.dirs.iterator(); it.hasNext(); ) {
            File d = new File(it.next());
            if (!copy && here(dest, d.getParentFile(), sameDir)) { it.remove(); s.inPlace++; continue; }
            File to = new File(dest, d.getName());
            if (s.skipTaken && sim.isDir(to)) { it.remove(); s.skipped++; continue; }
            File at = s.renameTaken && (sim.isDir(to) || sim.isFile(to)) ? sim.free(to) : to;
            if (copy) sim.copyDir(d, at);                                        // מבנה 77: תיקייה שלמה בהעתקה — רק מ"נבחרים"
            else sim.moveDir(d, at, s.replace);                                  // מבנה 44
        }
        List<String> what = new ArrayList<String>();
        if (!s.files.isEmpty() || s.dirs.isEmpty()) what.add(Words.files(s.files.size()));
        if (!s.dirs.isEmpty()) what.add(Words.folders(s.dirs.size()));
        s.describe = Words.Cmd.transfer(copy, Batch.join(what, Words.Cmd.AND), Bidi.path(dest.getAbsolutePath())) + filtered(s);
        s.noop = s.files.isEmpty() && s.dirs.isEmpty() && s.same + s.inPlace > 0;   // הכול כבר במקום — לא שגיאה, ואין מה לעשות
    }

    /**
     * מבנה 79: תיקייה חדשה מהמסך — בתיקייה שנבחרה (%יעד%), בשם שהוקלד (%שם-חדש%, נלקח מהבחירה ואינו מפוענח). שלא כמו
     * "צור תיקייה" כתובה (שבה תיקייה קיימת היא "בלי שינוי", ומסלול יוצר גם את מה שמעליו) — כאן, כמו שהמסך עשה: שם אחד,
     * ושם שכבר קיים הוא סירוב. הסירובים (שם לא תקין, תיקיית התוכנה, כבר קיים) — בתכנון ובנוסח של המסך.
     */
    /** מבנה 79 / 197 (ה9): התיקייה החדשה שנבחרה במסך — התיקייה שמוצגת, והשם שהוקלד כלשונו. null — שימוש שגוי (ונאמר). הכלל עצמו — ב־fileMd. */
    static File makeSelected(Batch.Step s, String cmd, File d, Sim sim) {
        Batch.Selection sel = sim.selected;
        File parent = d.getParentFile();
        if (sel == null || sel.name == null || sel.dest == null || parent == null || !parent.getAbsolutePath().equals(Paths.clean(sel.dest))) {
            s.error = Commands.usage(cmd);
            return null;
        }
        String bad = DiskRules.badName(sel.name);                              // השם שהוקלד, כולו (גם "א/ב", גם ריק) — לפני שהוא נעשה מסלול
        if (bad != null) { s.error = bad; return null; }
        return new File(parent, sel.name.trim());
    }

    /**
     * מבנה 78: שינוי שם של הקובץ שנבחר במסך, לשם שהוקלד בו. אותה הרצה כמו בשורה כתובה; הסירובים של המסך (קובץ של התוכנה, הקובץ
     * כבר איננו, שם לא תקין, שם תפוס) נאמרים בתכנון ובנוסח של המסך. שם זהה לקיים — תקין, ואין מה לעשות: כך המסך שואל
     * "מותר לשנות לקובץ הזה שם?" עוד לפני שהמשתמש מקליד.
     */
    /**
     * מבנה 196 (ה8; הוראות, סעיף 2א — כל דבר נקבע במקום אחד): שינוי שם של קובץ — הכלל האחד, לשורה הכתובה ולמסך.
     * המשתמש (16.11.2026): "שיעצור מההתחלה תמיד, וייתן הסבר" — כל סירוב נאמר כבר כאן, בתכנון (עד כאן, בשורה כתובה: קובץ של
     * התוכנה ושם לא תקין התגלו רק בהרצה); והנוסח — אחד, של קובץ הפקודות ("בסדר"). shown — הקובץ כמו שנכתב (או שמו, מהמסך).
     * אותו שם — אין מה לעשות (עד כאן, בשורה כתובה: תוכנן כשינוי שם).
     */
    static void rename(Batch.Step s, File f, String shown, String name, Sim sim) {
        if (sim.isDir(f)) { s.error = Words.Cmd.REN_FOLDER; return; }                  // 1.22.5
        String own = DiskRules.programFile(f.getAbsolutePath());
        if (own != null) { s.error = Words.Disk.CANT_RENAME + f.getName() + "\": " + own; return; }
        if (!sim.isFile(f)) { s.error = Words.FILE_NOT_FOUND + ": " + shown; return; }
        String bad = DiskRules.badName(name);
        if (bad != null) { s.error = bad; return; }
        File to = new File(f.getParentFile(), name);
        if (!to.getName().equalsIgnoreCase(f.getName()) && (sim.isFile(to) || sim.isDir(to))) {   // מבנה 43: גם תיקייה בשם הזה
            s.error = Words.Cmd.alreadyHas(name);
            return;
        }
        s.files.add(f.getAbsolutePath());
        s.newName = name;
        s.describe = Words.Cmd.rename(f.getName(), name);
        if (name.equals(f.getName())) s.noop = true;
        else sim.move(f.getAbsolutePath(), to, false);
    }

    static void renameSelected(Batch.Step s, String cmd, List<String> paths, Sim sim) {
        Batch.Selection sel = sim.selected;
        if (sel == null || !paths.get(0).equals(Syntax.SELECTED_MARK) || !paths.get(1).equals(Syntax.NAME_MARK) || sel.name == null) {
            s.error = Commands.usage(cmd);
            return;
        }
        if (sel.files.isEmpty() && sel.dirs.isEmpty()) { s.error = Words.Cmd.NOTHING_SELECTED; return; }
        if (!sel.dirs.isEmpty()) { s.error = Words.Cmd.REN_FOLDER; return; }
        if (sel.files.size() != 1) { s.error = Words.Cmd.SELECTED_ONE; return; }
        File f = new File(sel.files.get(0));
        rename(s, f, f.getName(), sel.name.trim(), sim);                       // מבנה 196 (ה8): הכלל האחד — בראשי
    }

    /**
     * מבנה 77: העתקה או העברה של מה שנבחר במסך אל תיקייה. אותה הדמיה ואותה הרצה כמו בשורה כתובה, חוץ משני דברים: תיקייה שנבחרה
     * עוברת שלמה גם בהעתקה (בשורה כתובה, "העתק <תיקייה>" פירושו הקבצים שבה), והסירובים של המסך — יעד בתיקיית התוכנה או בסל,
     * תיקייה לתוך עצמה, קובץ של התוכנה בהעברה — נאמרים כבר בתכנון ובנוסח של המסך, כלומר לפני חלון האישור.
     */
    static void fromSelected(Batch.Step s, String cmd, List<String> paths, Filter flt, Sim sim) {
        boolean copy = cmd.equals("COPY");
        if (paths.size() != 2) { s.error = Commands.usage(cmd); return; }
        if (s.recursive || s.stateWhen != Long.MIN_VALUE || paths.get(1).equals(Syntax.SHARE_MARK)) { s.error = Words.Cmd.SELECTED_PLAIN; return; }
        File dest = new File(paths.get(1));
        if (!sim.isDir(dest)) { s.error = Words.Cmd.NO_DEST; return; }
        String no = sim.lib == null ? null : DiskRules.noDest(sim.lib, dest);
        if (no != null) { s.error = no; return; }
        s.dest = dest;
        selected(s, flt, sim, cmd);
        if (s.error != null) return;
        for (String d : s.dirs)
            if (DiskRules.inside(dest, new File(d))) {
                s.error = Words.Disk.NOT_INTO_ITSELF;                           // מבנה 201 (ה11): הנוסח האחד — של קובץ הפקודות
                return;
            }
        arrange(s, copy, dest, sim);
    }

    /**
     * מבנה 73: המקור הוא מה שנבחר במסך. קובץ או תיקייה שכבר אינם — שגיאה (המסך הראה משהו שאיננו); סינון (גיל, גודל…) חל
     * על הקבצים; תיקייה — שלמה, ולכן סינון עם תיקייה שנבחרה הוא שגיאה, כמו בתיקייה שנכתבת במפורש (121).
     */
    /**
     * מבנה 201 (ה11; הוראות, סעיף 2א): מה שאסור למחוק או להעביר — קובץ של התוכנה, תיקייה מוגנת — כלל אחד, לשורה הכתובה ולמסך.
     * המשתמש (16.11.2026): "שיעצור מההתחלה תמיד, וייתן הסבר" — נאמר כבר בתכנון (עד כאן, בשורה כתובה: רק בהרצה).
     * cant — הפתיח של הסירוב ("אי אפשר למחוק" / "להעביר"); null — העתקה: המקור אינו משתנה, ומותר.
     */
    static boolean allowed(Batch.Step s, String cant, File x, boolean dir, Sim sim) {
        if (cant == null) return true;
        String why = dir ? (sim.lib == null ? null : DiskRules.forbidden(sim.lib, x)) : DiskRules.programFile(x.getAbsolutePath());
        if (why == null) return true;
        s.error = cant + x.getName() + "\": " + why;
        return false;
    }

    /** מבנה 201: אותו כלל — על כל מה שהשורה אספה (קבצים ותיקיות). */
    static boolean sourcesAllowed(Batch.Step s, String cant, Sim sim) {
        if (cant == null) return true;
        for (String p : s.files) if (!allowed(s, cant, new File(p), false, sim)) return false;
        for (String d : s.dirs) if (!allowed(s, cant, new File(d), true, sim)) return false;
        return true;
    }

    static void selected(Batch.Step s, Filter flt, Sim sim, String cmd) {
        // מבנה 77: מה שאסור למחוק אסור גם להעביר; להעתיק — מותר (המקור אינו משתנה).
        String cant = cmd.equals("DEL") ? Words.Disk.CANT_DELETE : cmd.equals("MOVE") ? Words.Disk.CANT_MOVE : null;
        Batch.Selection sel = sim.selected;
        if (sel == null || (sel.files.isEmpty() && sel.dirs.isEmpty())) { s.error = Words.Cmd.NOTHING_SELECTED; return; }
        if (!sel.dirs.isEmpty() && !flt.none()) { s.error = Words.Cmd.SELECTED_FILTER_FOLDER; return; }
        for (String p : sel.files) {
            File x = new File(p);
            if (!sim.isFile(x)) { s.error = Words.Cmd.notFound(p); return; }
            if (!allowed(s, cant, x, false, sim)) return;                       // מבנה 74 / 201: כבר בתכנון; הכלל — אחד
            s.total++;
            if (flt.ok(sim.real(x))) s.files.add(x.getAbsolutePath());
        }
        for (String d : sel.dirs) {
            File x = new File(d);
            if (!sim.isDir(x)) { s.error = Words.FOLDER_NOT_FOUND + ": " + d; return; }
            if (!allowed(s, cant, x, true, sim)) return;
            s.dirs.add(x.getAbsolutePath());
        }
    }

    static String filtered(Batch.Step s) {
        int passed = s.files.size() + s.skipped + s.same + s.inPlace;                 // מ0.40: גם מה שיצא אחרי הסינון — עבר אותו
        String f = s.total > passed && s.dirs.isEmpty() ? Words.Cmd.filtered(passed, s.total) : "";
        return f + Batch.notes(s);
    }
}
