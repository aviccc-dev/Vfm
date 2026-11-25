package com.tikiyot.vfm;

// מבנה 310 (המשתמש, 25.11.2026: "גיבוי יחזור רק לאופן שחזור… תיפתח רשימה חדשה בשם האופן והתאריך" · על הבדיקה: "ומה הבעיה
// שהבדיקה גם תמחק אותה. ועל הדרך תוסיף בפקודה מחיקה סופית"): שתי שורות —
//   שחזר גיבוי "<אופן>" ["<שם הרשימה>" ["<קובץ הגיבוי>"]]   — הגיבוי (האחרון התקין, או הקובץ בשם הזה) לרשימה חדשה באופן "שחזור"
//   בטל שחזור "<שם הרשימה>"                                  — הרשימה הזו יוצאת מהאופן "שחזור", לגמרי (עותק של המסד לפני)
// אחראי: לתכנן את השורות. אסור לו: להריץ; הפעולה עצמה — בחומר (DbRestore).

import java.util.List;
import java.util.Map;

final class RestoreCmd {

    private RestoreCmd() { }

    static void plan(final Batch.Step s, List<String> tk, Map<String, String> vars, Sim sim) {
        if (tk.size() < 2 || tk.size() > 4 || tk.get(1).trim().isEmpty()) { s.error = Words.DbCmd.RESTORE_BK_USAGE; return; }
        if (!DbVerbs.active()) { s.error = Words.DbCmd.VERIFY_DB_ONLY; return; }
        final String mode = Verbs.withVars(tk.get(1).trim(), vars);
        final String name = tk.size() > 2 ? Verbs.withVars(tk.get(2).trim(), vars) : "";
        final String file = tk.size() > 3 ? Verbs.withVars(tk.get(3).trim(), vars) : "";
        if (!name.isEmpty() && !name.equals(Names.cleanName(name))) { s.error = Words.DbCmd.restoreBkBadName(name); return; }
        s.target = DbRestore.MODE;
        sim.dbs.remove(DbRestore.MODE);                                       // מה שההדמיה החזיקה מהאופן הזה — כבר לא תקף
        s.describe = Words.DbCmd.restoreBkWill(mode, DbRestore.MODE);
        s.op = new ListCmd.Op() {                                             // יש גיבוי או אין — בהרצה: שורה קודמת יכולה ליצור אותו
            @Override public String run(Library lib) {
                DbRestore.Res r = DbRestore.toList(Db.app, mode, file.isEmpty() ? null : file, name.isEmpty() ? null : name, Reserved.FILES);
                if (!r.ok) DbListCmd.lastFailed = true;
                return r.text;
            }
        };
    }

    static void planDrop(final Batch.Step s, List<String> tk, Map<String, String> vars, Sim sim) {
        if (tk.size() != 2 || tk.get(1).trim().isEmpty()) { s.error = Words.DbCmd.RESTORE_DROP_USAGE; return; }
        if (!DbVerbs.active()) { s.error = Words.DbCmd.VERIFY_DB_ONLY; return; }
        final String name = Verbs.withVars(tk.get(1).trim(), vars);
        s.target = DbRestore.MODE;
        sim.dbs.remove(DbRestore.MODE);
        s.describe = Words.DbCmd.restoreDropWill(name, DbRestore.MODE);
        s.op = new ListCmd.Op() {
            @Override public String run(Library lib) {
                DbRestore.Res r = DbRestore.drop(Db.app, name);
                if (!r.ok) DbListCmd.lastFailed = true;
                return r.text;
            }
        };
    }
}
