package com.tikiyot.vfm;

// 1.16.1: תפריט הפקודות האחד — זהה בכל מקום (המילון, `claude/milon.md`, 8). המשתמש: "שלושה תפריטי משנה: אחד של
// המערכת — יוצג במלואו בכל פתיחת פקודות; השני — שהמשתמש בוחר למקם שם, בתפריט נפרד; השלישי יכיל את כל הפקודות
// הקיימות. ובנוסף — עריכת פקודות, שיאפשר לערוך גם את פקודות המערכת"; "פקודות המערכת לא אמורות להשתנות — השינוי
// נשמר אם בוחרים בשם אחר"; "שלי" — רשימה ידנית ("לקבוע רשימה כתפריט עם תתי תפריטים, רק שיוצג כמו שמקובל בתפריטים").
// נגיעה — הרצה (או בחירה, כשמוסיפים כלי או כפתור); נגיעה ארוכה — עריכה.
// 1.17.0: התפריט נבנה מרשימה ב"שמור לתוכנה" (Reserved), עם כל ההיררכיה שלה: קובצי הפקודות שבה — פקודות; תתי־רשימות
// וקישורים — תפריטי משנה; רשימה חכמה — תפריט משנה של מה שהיא מוצאת. ברירת המחדל: שמור לתוכנה / פקודות.
// עד כאן — חמישה מקומות שונים: "⭐ קובצי פקודות" ב־⋮, "פקודה / קיצור דרך" ב־➕ בבית, "מה הכפתור יעשה?",
// מסך "⭐ פקודות", ונגיעה על קובץ פקודות (תפריט "הרצה / עריכה").
// אחראי: התפריט. ההרצה — Verbs ו־BatchUi; הקבצים — Shortcuts.

import android.app.Activity;
import android.content.DialogInterface;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class CmdMenu {

    /** בחירה (בהוספת כלי או כפתור): צלמית, שם, שורת הפקודה, והקובץ (null — פקודה של שורה, בלי קובץ). */
    interface Picked {
        void ok(String icon, String name, String line, File file);
    }

    /** פקודות המערכת — קבוצה 1: פתיחת מסכים (המשתמש: "יוצגו גם בתור טקסט לעריכה"). {צלמית, שם, שורה}. */
    static List<String[]> system() {
        return Arrays.asList(
                new String[]{"🏠", Words.Top.HOME, Verbs.openScreen(Verbs.S_HOME)},
                new String[]{"→", Words.Top.BACK, Verbs.BACK},
                new String[]{"🔎", Words.Top.SEARCH, Verbs.openScreen(Verbs.S_SEARCH)},
                new String[]{"⭐", Words.Top.COMMANDS, Verbs.openScreen(Verbs.S_COMMANDS)},
                new String[]{"📂", Words.Top.FILES, Verbs.openScreen(Verbs.S_DISK)},
                new String[]{"🗂", Words.Top.VIRTUAL, Verbs.openScreen(Verbs.S_VIRTUAL)},
                new String[]{"⚙", Words.Top.SETTINGS, Verbs.openScreen(Verbs.S_SETTINGS)},
                new String[]{"🗑", Words.Top.TRASH, Verbs.openScreen(Verbs.S_TRASH)});
    }

    private CmdMenu() { }

    /** הרצה. newIn — תיקייה ל"➕ פקודה חדשה" (null — תיקיית הקיצורים). */
    static void show(Activity a, File newIn) {
        showList(a, Reserved.CMDS_PATH, newIn, null, null, true);
    }

    /**
     * pick — בחירה במקום הרצה (null — הרצה); screen — שם המסך, ל"🧩 פעולה של המסך" (רק כשמוסיפים כפתור).
     * בבחירה — גם שורה בכתב ותיקייה במנהל הקבצים (אלה פקודות שאין להן קובץ).
     */
    static void show(Activity a, File newIn, Picked pick, String screen) {
        showList(a, Reserved.CMDS_PATH, newIn, pick, screen, true);
    }

    /** "פתח תפריט <רשימה>" — רשימה מ"שמור לתוכנה" כתפריט. */
    static void showList(Activity a, List<String> path, Picked pick, String screen) {
        showList(a, path, null, pick, screen, true);
    }

    private static void showList(final Activity a, final List<String> path, final File newIn, final Picked pick,
                                 final String screen, final boolean top) {
        ModeCopy.Side side = Reserved.inUse();
        if (side == null) {                                                   // אין "שמור לתוכנה" בשום מקום — באופן הזה
            Reserved.ensureMode(Modes.current());
            side = Reserved.inUse();
        }
        final Tree.Node n = side == null ? null : side.tree.find(path);
        if (n == null) {
            Ui.toast(a, Words.Verb.NOT_FOUND + Tree.label(path));
            return;
        }
        final boolean here = Modes.current().equals(Reserved.modeInUse());
        final List<String> labels = new ArrayList<String>();
        final List<Runnable> taps = new ArrayList<Runnable>(), holds = new ArrayList<Runnable>();
        final List<String> keys = new ArrayList<String>();                    // 1.17.2: לסדר החופשי של הרשימה
        for (Tree.Entry e : n.entries) {                                      // קובצי הפקודות שבה
            final File f = new File(e.path);
            if (!Batch.isBatch(f.getName()) || !f.isFile()) continue;
            Program p = Program.read(f);
            labels.add((p.isProgram() ? p.icon : "▶") + "  " + p.title(f));
            keys.add("f" + e.path);
            taps.add(new Runnable() {
                @Override public void run() { if (Reserved.isNew(f)) create(a, newIn, pick); else useFile(a, f, pick); }
            });
            holds.add(new Runnable() {
                @Override public void run() { if (Reserved.isSystem(f)) { if (!Reserved.isNew(f)) saveAs(a, f); } else BatchUi.edit(a, f); }
            });
        }
        for (final Tree.Node c : n.children) {                                // תתי־רשימות — תפריטי משנה
            labels.add(c.name + "  ▸");
            keys.add("c" + c.name);
            taps.add(new Runnable() {
                @Override public void run() { showList(a, c.path(), newIn, pick, screen, false); }
            });
            holds.add(null);
        }
        for (String r : n.smart) {                                            // רשימה חכמה — מה שהיא מוצאת
            Structure.Func f = Structure.byId(side.ini, r);
            if (f == null) continue;
            final String name = f.name.substring(f.name.lastIndexOf(" › ") >= 0 ? f.name.lastIndexOf(" › ") + 3 : 0);
            final String cond = f.params.containsKey(SmartLists.COND) ? f.params.get(SmartLists.COND) : "";
            labels.add("📜 " + name + "  ▸");
            keys.add("s" + r);
            taps.add(new Runnable() {
                @Override public void run() { found(a, name, cond, pick); }
            });
            holds.add(null);
        }
        for (String r : n.refs) {                                             // קישור ↪ — הרשימה שאליה הוא מקשר
            final List<String> p = Tree.parsePath(r);
            if (side.tree.find(p) == null) continue;
            labels.add("↪ " + p.get(p.size() - 1) + "  ▸");
            keys.add("r" + r);
            taps.add(new Runnable() {
                @Override public void run() { showList(a, p, newIn, pick, screen, false); }
            });
            holds.add(null);
        }
        if (!n.order.isEmpty()) {                                             // 1.17.2: התפריט — בסדר של הרשימה
            final java.util.Map<String, Integer> at = new java.util.HashMap<String, Integer>();
            for (int i = 0; i < n.order.size(); i++) at.put(n.order.get(i), i);
            Integer[] idx = new Integer[keys.size()];
            for (int i = 0; i < idx.length; i++) idx[i] = i;
            java.util.Arrays.sort(idx, new java.util.Comparator<Integer>() {
                @Override
                public int compare(Integer x, Integer y) {
                    Integer rx = at.get(keys.get(x)), ry = at.get(keys.get(y));
                    return (rx != null ? rx : 100000 + x) - (ry != null ? ry : 100000 + y);
                }
            });
            List<String> l2 = new ArrayList<String>();
            List<Runnable> t2 = new ArrayList<Runnable>(), h2 = new ArrayList<Runnable>();
            for (Integer i : idx) {
                l2.add(labels.get(i));
                t2.add(taps.get(i));
                h2.add(holds.get(i));
            }
            labels.clear();
            labels.addAll(l2);
            taps.clear();
            taps.addAll(t2);
            holds.clear();
            holds.addAll(h2);
        }
        if (top && pick != null) extras(a, screen, pick, labels, taps, holds);
        labels.add(Words.CmdM.ARRANGE);
        taps.add(new Runnable() {
            @Override
            public void run() {
                if (here) VirtualScreen.open(a, path, null, null);
                else Ui.toast(a, Words.CmdM.FROM_EXAMPLES);
            }
        });
        holds.add(null);
        Ui.list(a, top && path.equals(Reserved.CMDS_PATH) ? Words.CmdM.TITLE : path.get(path.size() - 1), labels,
                new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) { taps.get(w).run(); }
                }, new Ui.Index1() {
                    @Override public void ok(int w) { if (holds.get(w) != null) holds.get(w).run(); }
                });
    }

    /** בבחירה: שורה בכתב · תיקייה במנהל הקבצים · פעולה של המסך (פקודות בלי קובץ). */
    private static void extras(final Activity a, final String screen, final Picked pick, List<String> labels,
                               List<Runnable> taps, List<Runnable> holds) {
        labels.add(Words.Top.T_LINE);
        taps.add(new Runnable() {
            @Override
            public void run() {
                Ui.askText(a, Words.Top.T_LINE, "", Words.Top.LINE_HINT, new Ui.Text1() {
                    @Override
                    public void ok(String s) { pick.ok("⌨", s.length() > 12 ? s.substring(0, 12) + "…" : s, s.trim(), null); }
                });
            }
        });
        holds.add(null);
        labels.add(Words.CmdM.FOLDER);
        taps.add(new Runnable() {
            @Override
            public void run() {
                FolderPicker.choose(a, new FolderPicker.Picked() {
                    @Override public void ok(File f) { pick.ok("📂", f.getName(), Verbs.openFolder(f.getAbsolutePath()), null); }
                });
            }
        });
        holds.add(null);
        if (screen != null && a instanceof TopRow.Host && !((TopRow.Host) a).actions().isEmpty()) {
            labels.add(Words.Top.T_OWN + screen);
            taps.add(new Runnable() {
                @Override public void run() { own(a, screen, pick); }
            });
            holds.add(null);
        }
    }

    /** נגיעה: הרצה, או בחירה. */
    private static void useFile(Activity a, File f, Picked pick) {
        Program p = Program.read(f);
        if (pick != null) pick.ok(p.isProgram() ? p.icon : "▶", p.title(f), Verbs.run(f.getAbsolutePath()), f);
        else BatchUi.run(a, f);
    }

    /** רשימה חכמה בתפריט — קובצי הפקודות שהיא מוצאת. */
    private static void found(final Activity a, String title, String cond, final Picked pick) {
        final List<File> files = new ArrayList<File>();
        for (Library.Hit h : Library.get(a).search(cond, false, false).hits)
            if (h.rec != null && Batch.isBatch(h.path())) files.add(new File(h.path()));
        if (files.isEmpty()) {
            Ui.toast(a, Words.Top.NO_BATCH);
            return;
        }
        List<String> labels = new ArrayList<String>();
        for (File f : files) {
            Program p = Program.read(f);
            labels.add((p.isProgram() ? p.icon : "▶") + "  " + p.title(f));
        }
        Ui.list(a, title, labels, new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface d, int w) { useFile(a, files.get(w), pick); }
        }, new Ui.Index1() {
            @Override public void ok(int w) { BatchUi.edit(a, files.get(w)); }
        });
    }

    /** "🧩 פעולה של המסך" — הפעולות שהמסך עצמו מכיר (בכפתור בלבד). */
    private static void own(Activity a, String screen, final Picked pick) {
        final List<String[]> acts = ((TopRow.Host) a).actions();
        String[] names = new String[acts.size()];
        for (int i = 0; i < acts.size(); i++) names[i] = acts.get(i)[1] + " " + acts.get(i)[2];
        Ui.options(a, Words.Top.T_OWN + screen, names, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int k) {
                pick.ok(acts.get(k)[1], acts.get(k)[2], Verbs.fromOld(acts.get(k)[0]), null);
            }
        });
    }

    /** פקודת מערכת — "שמירה בשם": שם חדש ← קובץ פקודות ב"הפקודות שלי" (המקור לא משתנה) ← העורך. */
    private static void saveAs(final Activity a, final File sys) {
        final Program p = Program.read(sys);
        Ui.askText(a, Words.CmdM.SAVE_AS, p.title(sys) + Words.CmdM.MINE_SUFFIX, Words.Short.NAME_HINT, new Ui.Text1() {
            @Override
            public void ok(String name) {
                String text;
                try {
                    text = Files.readUtf8(sys);
                } catch (Throwable e) {
                    text = "";
                }
                StringBuilder lines = new StringBuilder();                   // השורות — בלי הכותרת (צלמית, שם)
                for (String l : text.split("\n")) if (!l.startsWith(Program.ICON + " ") && !l.startsWith(Program.NAME + " ") && !l.trim().isEmpty())
                    lines.append(lines.length() > 0 ? "\n" : "").append(l);
                File f = Shortcuts.write(p.isProgram() ? p.icon : "▶", name, lines.toString());
                if (f == null) {
                    Ui.toast(a, Words.SAVE_FAILED);
                    return;
                }
                Reserved.addToMine(f);
                BatchUi.edit(a, f);
            }
        });
    }

    /** "➕ פקודה חדשה" (וגם `צור פקודה`) — שם ← קובץ (בתיקייה newIn, או בקיצורים) ← ב"הפקודות שלי" ← העורך. */
    static void create(final Activity a, final File newIn, final Picked pick) {
        Ui.askText(a, Words.CmdM.NEW, "", Words.Short.NAME_HINT, new Ui.Text1() {
            @Override
            public void ok(final String name) {
                if (newIn != null) {                                          // בתיקייה של המשתמש — שינוי בכונן, באישור
                    String n = name.trim();
                    if (!n.toLowerCase(java.util.Locale.ROOT).endsWith("." + Batch.EXT)) n += "." + Batch.EXT;
                    final File f = new File(newIn, n);
                    Physical.askNewFile(a, f, Batch.template(), new Runnable() {
                        @Override public void run() { made(a, f, name, pick); }
                    });
                    return;
                }
                File f = Shortcuts.write("▶", name, "");
                if (f == null) Ui.toast(a, Words.SAVE_FAILED);
                else made(a, f, name, pick);
            }
        });
    }

    private static void made(Activity a, File f, String name, Picked pick) {
        Reserved.addToMine(f);
        if (pick != null) pick.ok("▶", name, Verbs.run(f.getAbsolutePath()), f);
        BatchUi.edit(a, f);
    }
}
