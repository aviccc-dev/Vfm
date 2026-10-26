package com.tikiyot.vfm;

// 1.13.0: פקודות הפתיחה בעברית — פועל אחד ומילות סוג קבועות (המשתמש, 15.10: "אם כבר עוברים לעברית — אפשר
// להתחיל"). שלושה פעלים, כל אחד לדבר אחד ("'פתח או צור' — עמום"):
//   פתח   — רק מה שקיים:  פתח מסך הבית · פתח רשימה לימוד/הערות · פתח רשימה חכמה אחרונים · פתח תיקייה <מסלול>
//            (לא קיים — הודעה, בלי יצירה). לרשימה — גם "מיון:<שם המיון>" (למשל מיון:הסדר שלי).
//   צור   — לא משכפל לעולם. הזהות של רשימה חכמה = ההגדרות שלה: יש רשימה עם אותן הגדרות — נפתחת הקיימת;
//            אין — נוצרת ונפתחת; אותו שם עם הגדרות אחרות — רשימה אחרת, והשם מקבל מספר.  צור רשימה חכמה "שם" <תנאי>
//            "צור רשימה" בלי שם — החלון של "רשימה חדשה".
//   עדכן  — שינוי ההגדרות של רשימה חכמה קיימת, רק במפורש:  עדכן רשימה חכמה "שם" <תנאי>
//   מצא   — מה שעונה לתנאים, כרשימה זמנית, בלי לשמור:  מצא סוג:ספרים תאריך:שבוע
//   חזור · הרץ <קובץ פקודות> · הוסף מהכונן / מחיפוש / כלי — פעולות המסך הנוכחי (החלון שהכפתור פותח).
// כפתור = שורת פקודה (המשתמש: "יש כמה פקודות שנמצאות בכפתורים בלי פקודות"); גם קובץ פקודות מכיר אותן — כ־START:
// נפתחות בסוף ההרצה. המסכים: הבית · חיפוש · פקודות · קבצים פיזיים · קבצים וירטואליים · הגדרות · סל.
// אחראי: לפענח ולבצע את פקודות הפתיחה. אסור לו: לשנות קבצים בכונן (זה Batch, דרך Physical).

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class Verbs {

    static final String OPEN = "פתח", CREATE = "צור", UPDATE = "עדכן", FIND = "מצא", BACK = "חזור",   // תחביר
            RUN = "הרץ", ADD = "הוסף";   // תחביר
    static final String SCREEN = "מסך", LIST = "רשימה", SMART_WORD = "חכמה", FOLDER = "תיקייה";   // תחביר
    /** 1.17.0: "פתח תפריט <רשימה ב'שמור לתוכנה'>" · "צור פקודה" (המילון, 23). */
    static final String MENU = "תפריט", CMD_WORD = "פקודה";   // תחביר
    static final String S_HOME = "הבית", S_SEARCH = "חיפוש", S_COMMANDS = "פקודות", S_DISK = "קבצים פיזיים",   // תחביר
            S_VIRTUAL = "קבצים וירטואליים", S_SETTINGS = "הגדרות", S_TRASH = "סל";   // תחביר
    static final String[] SCREENS = {S_HOME, S_SEARCH, S_COMMANDS, S_DISK, S_VIRTUAL, S_SETTINGS, S_TRASH};
    static final String A_DRIVE = "מהכונן", A_SEARCH = "מחיפוש";   // תחביר
    static final String SORT = "מיון:";   // תחביר

    /** סוגי הפקודה אחרי הפענוח. */
    static final String K_SCREEN = "screen", K_LIST = "list", K_SMART = "smart", K_FOLDER = "folder", K_FIND = "find",
            K_BACK = "back", K_RUN = "run", K_NEW_LIST = "newlist", K_ADD = "add", K_MENU = "menu", K_NEW_CMD = "newcmd";

    static final class Cmd {
        String verb = "", kind = "", arg = "", sort = "", cond = "";
        /** שגיאת תחביר — או null. */
        String error;
    }

    private Verbs() { }

    /** האם השורה מתחילה באחד הפעלים (ואז היא שלנו, לא של המנוע הישן). */
    static boolean isVerb(String line) {
        String w = first(line);
        return w.equals(OPEN) || w.equals(CREATE) || w.equals(UPDATE) || w.equals(FIND) || w.equals(BACK)
                || w.equals(RUN) || w.equals(ADD);
    }

    private static String first(String line) {
        String s = line == null ? "" : line.trim();
        int i = s.indexOf(' ');
        return i < 0 ? s : s.substring(0, i);
    }

    private static String after(String s, String word) {
        s = s.trim();
        return s.length() <= word.length() ? "" : s.substring(word.length()).trim();
    }

    private static boolean starts(String s, String word) {
        return s.equals(word) || s.startsWith(word + " ");
    }

    private static String unquote(String s) {
        s = s.trim();
        if (s.length() >= 2 && ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("«") && s.endsWith("»"))))
            return s.substring(1, s.length() - 1).trim();
        return s;
    }

    /** "מיון:…" בסוף השורה — יורד ממנה ונשמר בנפרד. */
    private static String takeSort(Cmd c, String rest) {
        int i = rest.indexOf(SORT);
        if (i < 0 || (i > 0 && rest.charAt(i - 1) != ' ')) return rest;
        c.sort = unquote(rest.substring(i + SORT.length()));
        return rest.substring(0, i).trim();
    }

    /** שם ראשון (במרכאות, או מילה אחת) והשאר. */
    private static String[] nameAndRest(String rest) {
        rest = rest.trim();
        if (rest.startsWith("\"") || rest.startsWith("«")) {
            char close = rest.charAt(0) == '"' ? '"' : '»';
            int j = rest.indexOf(close, 1);
            if (j > 0) return new String[]{rest.substring(1, j).trim(), rest.substring(j + 1).trim()};
        }
        int sp = rest.indexOf(' ');
        return sp < 0 ? new String[]{rest, ""} : new String[]{rest.substring(0, sp), rest.substring(sp + 1).trim()};
    }

    static Cmd parse(String line) {
        Cmd c = new Cmd();
        String s = Bidi.strip(line == null ? "" : line).trim();
        c.verb = first(s);
        String rest = after(s, c.verb);
        if (c.verb.equals(BACK)) {
            c.kind = K_BACK;
        } else if (c.verb.equals(FIND)) {
            c.kind = K_FIND;
            c.cond = takeSort(c, rest);
            if (c.cond.isEmpty()) c.error = Words.Verb.usage(FIND);
        } else if (c.verb.equals(RUN)) {
            c.kind = K_RUN;
            c.arg = unquote(rest);
            if (c.arg.isEmpty()) c.error = Words.Verb.usage(RUN);
        } else if (c.verb.equals(ADD)) {
            c.kind = K_ADD;
            c.arg = rest;
            if (!rest.equals(A_DRIVE) && !rest.equals(A_SEARCH)) c.error = Words.Verb.usage(ADD);
        } else if (c.verb.equals(OPEN)) {
            if (starts(rest, SCREEN)) {
                c.kind = K_SCREEN;
                String r = after(rest, SCREEN);
                for (String sc : SCREENS)
                    if (starts(r, sc)) {
                        c.arg = sc;
                        c.cond = unquote(after(r, sc));                         // קבצים פיזיים / וירטואליים — תיקייה (לא חובה)
                    }
                if (c.arg.isEmpty()) c.error = Words.Verb.unknownScreen(r);
            } else if (starts(rest, MENU)) {                                  // 1.17.0
                c.kind = K_MENU;
                c.arg = unquote(after(rest, MENU));
            } else if (starts(rest, LIST + " " + SMART_WORD)) {
                c.kind = K_SMART;
                c.arg = unquote(takeSort(c, after(rest, LIST + " " + SMART_WORD)));
            } else if (starts(rest, LIST)) {
                c.kind = K_LIST;
                c.arg = unquote(takeSort(c, after(rest, LIST)));
            } else if (starts(rest, FOLDER)) {
                c.kind = K_FOLDER;
                c.arg = unquote(after(rest, FOLDER));
            } else {
                c.error = Words.Verb.usage(OPEN);
            }
            if (c.error == null && !c.kind.equals(K_SCREEN) && c.arg.isEmpty()) c.error = Words.Verb.usage(OPEN);
        } else if (c.verb.equals(CREATE) || c.verb.equals(UPDATE)) {
            if (starts(rest, LIST + " " + SMART_WORD)) {
                c.kind = K_SMART;
                String[] nr = nameAndRest(takeSort(c, after(rest, LIST + " " + SMART_WORD)));
                c.arg = nr[0];
                c.cond = nr[1];
                if (c.arg.isEmpty() || c.cond.isEmpty()) c.error = Words.Verb.usage(c.verb);
            } else if (c.verb.equals(CREATE) && rest.trim().equals(CMD_WORD)) {   // 1.17.0
                c.kind = K_NEW_CMD;
            } else if (c.verb.equals(CREATE) && starts(rest, LIST)) {
                c.kind = c.verb.equals(CREATE) && after(rest, LIST).isEmpty() ? K_NEW_LIST : K_LIST;
                c.arg = unquote(after(rest, LIST));
            } else {
                c.error = Words.Verb.usage(c.verb);
            }
        } else {
            c.error = Words.Verb.usage(OPEN);
        }
        return c;
    }

    // ---------- בונים — כך הטפסים כותבים את הפקודות ----------

    static String openScreen(String screen) {
        return OPEN + " " + SCREEN + " " + screen;
    }

    static String openList(List<String> path) {
        return OPEN + " " + LIST + " " + Tree.key(path);
    }

    /** 1.17.0: רשימה מ"שמור לתוכנה" כתפריט. */
    static String openMenu(List<String> path) {
        return OPEN + " " + MENU + " " + Tree.key(path);
    }

    /**
     * 1.13.1 (באג): רשימה שינתה שם או מקום — שורת "פתח רשימה" / "צור רשימה" שמפנה אליה (או אל תת־רשימה שלה)
     * נכתבת מחדש למסלול החדש; לא נוגעת בה — null.
     */
    static String listMoved(String line, List<String> from, List<String> to) {
        Cmd c = parse(line);
        if (c.error != null || !c.kind.equals(K_LIST)) return null;
        List<String> p = Tree.moved(Tree.parsePath(c.arg), from, to);
        if (p == null) return null;
        return c.verb + " " + LIST + " " + Tree.key(p) + (c.sort.isEmpty() ? "" : " " + SORT + c.sort);
    }

    static String openSmart(String name) {
        return OPEN + " " + LIST + " " + SMART_WORD + " " + name;
    }

    static String openFolder(String path) {
        return OPEN + " " + FOLDER + " " + path;
    }

    static String run(String file) {
        return RUN + " " + file;
    }

    static final String HOME_LINE = openScreen(S_HOME), BACK_LINE = BACK;

    /** כפתור של לפני 1.13.0 ("מסך:בית", "רשימה:…", "קובץ:…", "פקודה:…") — כשורת פקודה. */
    static String fromOld(String act) {
        if (act == null) return "";
        String a = act.trim();
        if (a.startsWith(Buttons.SCREEN)) {
            String s = a.substring(Buttons.SCREEN.length()).trim();
            if (s.equals("בית")) return HOME_LINE;                              // תחביר
            if (s.equals("חזרה")) return BACK_LINE;                             // תחביר
            if (s.equals(S_SEARCH)) return openScreen(S_SEARCH);
            if (s.equals(S_COMMANDS)) return openScreen(S_COMMANDS);
            if (s.equals("מנהל קבצים")) return openScreen(S_DISK);              // תחביר
            if (s.equals(S_SETTINGS)) return openScreen(S_SETTINGS);
            return a;
        }
        if (a.startsWith(Buttons.FOLDER)) return openFolder(a.substring(Buttons.FOLDER.length()).trim());
        if (a.startsWith(Buttons.LIST)) return openList(Tree.parsePath(a.substring(Buttons.LIST.length()).trim()));
        if (a.startsWith(Buttons.SMART)) return openSmart(a.substring(Buttons.SMART.length()).trim());
        if (a.startsWith(Buttons.BATCH)) return run(a.substring(Buttons.BATCH.length()).trim());
        if (a.startsWith(Buttons.LINE)) return a.substring(Buttons.LINE.length()).trim();
        if (a.startsWith(Buttons.ACT)) {
            String s = a.substring(Buttons.ACT.length()).trim();
            if (s.equals("רשימה חדשה")) return CREATE + " " + LIST;             // תחביר
            if (s.equals("מהכונן")) return ADD + " " + A_DRIVE;                 // תחביר
            if (s.equals("מחיפוש")) return ADD + " " + A_SEARCH;                // תחביר
        }
        return a;
    }

    /** הפעולה של המסך (TopRow.Host) שמתאימה לפקודה — או null. */
    static String hostCode(Cmd c) {
        if (c.kind.equals(K_NEW_LIST)) return Buttons.ACT + "רשימה חדשה";       // תחביר
        if (c.kind.equals(K_ADD)) {
            if (c.arg.equals(A_DRIVE)) return Buttons.ACT + "מהכונן";           // תחביר
            if (c.arg.equals(A_SEARCH)) return Buttons.ACT + "מחיפוש";          // תחביר
        }
        return null;
    }

    // ---------- תיאור (לתצוגה המקדימה של קובץ פקודות) ----------

    static String describe(Cmd c) {
        if (c.error != null) return c.error;
        return Words.Verb.atEnd(label(c));
    }

    /** מה הפקודה פותחת, במילים. */
    static String label(Cmd c) {
        if (c.kind.equals(K_SCREEN)) return Words.Verb.SCREEN + c.arg + (c.cond.isEmpty() ? "" : " · " + c.cond);
        if (c.kind.equals(K_LIST)) return Words.Verb.LIST + c.arg;
        if (c.kind.equals(K_SMART)) return Words.Verb.SMART + c.arg;
        if (c.kind.equals(K_FOLDER)) return Words.Verb.FOLDER + c.arg;
        if (c.kind.equals(K_FIND)) return Words.Verb.FIND + c.cond;
        if (c.kind.equals(K_BACK)) return Words.Verb.BACK;
        if (c.kind.equals(K_RUN)) return Words.Verb.RUN + new File(c.arg).getName();
        if (c.kind.equals(K_MENU)) return Words.Verb.MENU + c.arg;
        if (c.kind.equals(K_NEW_CMD)) return Words.CmdM.NEW;
        return c.verb + " " + c.arg;
    }

    // ---------- משתנים ----------

    /** %פנימי% %כרטיס% %הורדות% %כאן% — כמו בקובץ פקודות. */
    static String withVars(String line, Map<String, String> vars) {
        if (line == null || vars == null || line.indexOf('%') < 0) return line;
        String out = line;
        for (Map.Entry<String, String> e : vars.entrySet())
            if (e.getValue() != null && !e.getValue().startsWith("/\u0000")) out = out.replace("%" + e.getKey() + "%", e.getValue());
        return out;
    }

    // ---------- ביצוע ----------

    /** מבצע שורה; false — אינה פקודת פתיחה (אז היא שורה למנוע הרגיל). here — התיקייה של הקובץ (או null). */
    static boolean run(Activity a, String line, File here) {
        if (!isVerb(line)) return false;
        Map<String, String> vars = BatchUi.vars(a, here != null ? here : Paths.work());
        Cmd c = parse(withVars(line, vars));
        if (c.error != null) {
            Ui.toast(a, c.error);
            return true;
        }
        String host = hostCode(c);
        if (host != null) {
            if (!(a instanceof TopRow.Host) || !((TopRow.Host) a).act(host)) Ui.toast(a, Words.Top.notHere(label(c)));
            return true;
        }
        if (c.kind.equals(K_BACK)) {                                          // באג (1.17.6): בבית — יציאה מהתוכנה;
            if (a instanceof Home) ((Home) a).modeMenu();                     // עכשיו — בחירת אופן עבודה (המשתמש)
            else a.onBackPressed();
        }
        else if (c.kind.equals(K_MENU)) CmdMenu.showList(a, Tree.parsePath(c.arg), null, null);   // 1.17.0
        else if (c.kind.equals(K_NEW_CMD)) CmdMenu.create(a, here, null);
        else if (c.kind.equals(K_SCREEN)) screen(a, c);
        else if (c.kind.equals(K_FOLDER)) {
            if (new File(c.arg).isDirectory()) DiskScreen.open(a, c.arg);
            else Ui.toast(a, Words.Verb.NOT_FOUND + c.arg);
        } else if (c.kind.equals(K_LIST)) {
            ArrayList<String> path = Tree.parsePath(c.arg);
            if (c.verb.equals(CREATE)) ensureList(path);
            if (path.isEmpty() || Tree.load().find(path) == null) Ui.toast(a, Words.Verb.NOT_FOUND + c.arg);
            else VirtualScreen.open(a, path, c.sort, null);                    // 1.13.1: החזרה — למסך שקרא
        } else if (c.kind.equals(K_SMART)) {
            SmartLists.Named n;
            if (c.verb.equals(CREATE)) n = SmartLists.ensure(c.arg, c.cond, c.sort);
            else if (c.verb.equals(UPDATE)) n = SmartLists.update(c.arg, c.cond, c.sort);
            else n = SmartLists.byName(c.arg);
            if (n == null) Ui.toast(a, Words.Verb.NOT_FOUND + c.arg);
            else openSmart(a, n, c.sort);
        } else if (c.kind.equals(K_FIND)) {
            Intent i = new Intent(a, SearchScreen.class);
            i.putExtra(SearchScreen.EXTRA_QUERY, c.cond);
            i.putExtra(SearchScreen.EXTRA_FIXED, true);
            i.putExtra(SearchScreen.EXTRA_TITLE, Words.Verb.FIND + c.cond);
            i.putExtra(SearchScreen.EXTRA_SORT, c.sort.isEmpty() ? Sort.NAME : c.sort);
            i.putExtra(SearchScreen.EXTRA_VIEW, FolderView.FLAT);
            a.startActivity(i);
        } else if (c.kind.equals(K_RUN)) {
            File f = new File(c.arg);
            if (!f.isAbsolute() && here != null) f = new File(here, c.arg);
            if (f.isFile()) BatchUi.run(a, f); else Ui.toast(a, Words.Verb.NOT_FOUND + f.getName());
        }
        return true;
    }

    private static void screen(Activity a, Cmd c) {
        String s = c.arg;
        if (s.equals(S_HOME)) Home.go(a);
        else if (s.equals(S_SEARCH)) a.startActivity(new Intent(a, SearchScreen.class));
        else if (s.equals(S_COMMANDS)) CmdMenu.show(a, null);             // 1.16.1: אותו תפריט בכל מקום
        else if (s.equals(S_DISK)) DiskScreen.open(a, c.cond.isEmpty() ? null : c.cond);
        else if (s.equals(S_VIRTUAL)) a.startActivity(SearchScreen.fileManager(a, "", c.cond.isEmpty() ? null : c.cond, null));
        else if (s.equals(S_SETTINGS)) a.startActivity(new Intent(a, SettingsScreen.class));
        else if (s.equals(S_TRASH)) a.startActivity(new Intent(a, TrashScreen.class));
    }

    static void openSmart(Activity a, SmartLists.Named n, String sort) {
        Intent i = new Intent(a, SearchScreen.class);
        i.putExtra(SearchScreen.EXTRA_QUERY, n.cond);
        i.putExtra(SearchScreen.EXTRA_FIXED, true);
        i.putExtra(SearchScreen.EXTRA_TITLE, n.name);
        i.putExtra(SearchScreen.EXTRA_SORT, sort != null && !sort.isEmpty() ? sort : n.sort.isEmpty() ? Sort.NAME : n.sort);
        i.putExtra(SearchScreen.EXTRA_VIEW, n.view.isEmpty() ? FolderView.FLAT : n.view);
        a.startActivity(i);
    }

    /** "צור רשימה <שם>" — רשימה ידנית: קיימת — כמו שהיא; חסרה — נוצרת (בכל הדרך). */
    static void ensureList(final List<String> path) {
        if (path.isEmpty() || Tree.load().find(path) != null) return;
        Tree.edit(new Tree.Edit() {
            @Override
            public boolean apply(Tree t) {
                Tree.Node n = t.root;
                for (String p : path) {
                    Tree.Node c = n.child(Tree.cleanName(p));
                    if (c == null) c = Tree.newChild(n, Tree.cleanName(p));
                    n = c;
                }
                return true;
            }
        });
    }

    // ---------- הנתון החי ----------

    /** הקבצים שפקודת פתיחה מציגה עכשיו (מהאינדקס ומהרשימות) — או null: היא לא מסננת קבצים (מסך, חזור). */
    static List<String> files(Context ctx, Cmd c) {
        if (c == null || c.error != null) return null;
        try {
            Library lib = Library.get(ctx);
            if (c.kind.equals(K_LIST)) {
                Tree.Node n = Tree.load().find(Tree.parsePath(c.arg));
                if (n == null) return new ArrayList<String>();
                List<String> out = new ArrayList<String>();
                collect(n, out);
                return out;
            }
            if (c.kind.equals(K_FOLDER)) return lib.filesUnder(c.arg);
            String cond = null;
            if (c.kind.equals(K_FIND)) cond = c.cond;
            else if (c.kind.equals(K_SMART)) {
                SmartLists.Named n = SmartLists.byName(c.arg);
                cond = n != null ? n.cond : c.cond;
            }
            if (cond == null || cond.isEmpty()) return null;
            List<String> out = new ArrayList<String>();
            for (Library.Hit h : lib.search(cond, false, false).hits) if (h.rec != null) out.add(h.path());
            return out;
        } catch (Throwable t) {
            Log.e("פקודה — נתון חי", t);
            return null;
        }
    }

    private static void collect(Tree.Node n, List<String> out) {
        for (Tree.Entry e : n.entries) if (!e.bad) out.add(e.path);
        for (Tree.Node c : n.children) collect(c, out);
    }
}
