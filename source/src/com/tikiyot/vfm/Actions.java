package com.tikiyot.vfm;

// מרשם הפעולות (80, רמה 2). כל פעולה מוגדרת כאן פעם אחת: על מה היא עובדת, על כמה פריטים,
// באילו כלים יש לה משמעות, והאם היא וירטואלית או פיזית. המסכים לא בונים את הפס — הם שואלים
// את המרשם "מה מתאים לבחירה הזו בכלי הזה". כך הכללים מתקיימים בכל מקום מעצמם:
//   פעולה פיזית שהורסת (מחיקה) — אחרונה בפס, בצבע אזהרה, במילה "מהכונן"/"לצמיתות", ובאישור (64).
// אסור לו: לבצע בעצמו פעולות על קבצים או רשימות (הביצוע ב-Acts ובמסכים).

import android.app.Activity;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class Actions {

    private Actions() { }

    /** הכלי שבו הבחירה נעשית — קובע אילו פעולות יש להן משמעות. */
    public enum Tool {
        /** מנהל הקבצים: קבצים בכונן. */
        DRIVE,
        /** חיפוש ורשימה חכמה: קבצים מהאינדקס. */
        SEARCH,
        /** רשימה ידנית: פריטים ורשימות. */
        LIST,
        /** סל המחזור: קבצים שנמחקו. */
        TRASH,
        /** כפולים: עותקים עם תיבות סימון. */
        DUP
    }

    /** האם הפעולה נוגעת בקובץ עצמו. */
    public enum Kind {
        /** לא נוגעת בקבצים (רשימות, העתקת מסלול, פתיחה). */
        VIRTUAL,
        /** מזיזה קובץ בכונן, ואפשר לחזור ממנה (שחזור). */
        PHYSICAL,
        /** מוציאה קובץ ממקומו (מחיקה): אחרונה, בצבע אזהרה, באישור. */
        DESTRUCTIVE
    }

    public static final int WARNING = Ui.RED;

    public static final class Action {
        public final String id, label;
        public final Kind kind;
        /** רק כשנבחר פריט אחד בדיוק. */
        public final boolean one;
        /** גם רשימות (ולא רק קבצים) יכולות להיות בבחירה. */
        public final boolean lists;
        /** הפעולה עובדת רק על רשימה (שינוי שם). */
        public final boolean onlyLists;
        /** הפעולה לא זמינה בזמן בחירת קבצים לרשימה מסוימת ("➕ מחיפוש"). */
        public final boolean notWhilePicking;
        /** עובדת גם על תיקיות בכונן (הוספה לרשימה, מחיקה, העברה, העתקה — 82). */
        public boolean folders;
        /** בפס (83): שם הצלמית (res/drawable/ic_<שם>.xml — סט אחיד בצבע אחד), ומילה קצרה מתחתיה. */
        public String icon = "•", shortLabel;
        /** באילו כלים הפעולה בין הארבע העיקריות; בשאר — תחת "עוד". */
        private final java.util.Set<Tool> main = java.util.EnumSet.noneOf(Tool.class);
        private final Tool[] tools;

        private Action(String id, String label, Kind kind, boolean one, boolean lists, boolean onlyLists,
                       boolean notWhilePicking, Tool... tools) {
            this.id = id;
            this.label = label;
            this.kind = kind;
            this.one = one;
            this.lists = lists;
            this.onlyLists = onlyLists;
            this.notWhilePicking = notWhilePicking;
            this.tools = tools;
        }

        public boolean mainIn(Tool t) {
            return main.contains(t);
        }

        void bar(String icon, String shortLabel, Tool... mainIn) {
            this.icon = icon;
            this.shortLabel = shortLabel;
            main.addAll(java.util.Arrays.asList(mainIn));
        }

        public boolean in(Tool t) {
            for (Tool x : tools) if (x == t) return true;
            return false;
        }

        /** האם הפעולה מתאימה לבחירה: files קבצים ו-lists רשימות נבחרו. */
        /** dirs — תיקיות בכונן שנבחרו. פעולה שאינה עובדת על תיקיות לא מוצגת כשיש כאלה. */
        public boolean fits(Tool t, int files, int lists, int dirs, boolean picking) {
            if (!in(t)) return false;
            if (dirs > 0 && !folders) return false;
            if (dirs > 0) return !one && files + dirs >= 1;
            if (picking && notWhilePicking) return false;
            if (onlyLists) return files == 0 && (one ? lists == 1 : lists >= 1);
            if (!this.lists && lists > 0) return false;
            int n = files + (this.lists ? lists : 0);
            return one ? n == 1 && files == 1 : n >= 1;
        }

        @Override public String toString() { return label; }
    }

    // ---------- הפעולות — כל אחת פעם אחת ----------

    /** 1.16.0: מחיפוש ומרשימה חכמה — "העתקה" לרשימה ידנית (המילון, `claude/milon.md`). */
    public static final Action ADD_TO_LIST = new Action("add", Words.VClipW.DUPLICATE + " " + Words.TO_LIST_WORD, Kind.VIRTUAL,
            false, false, false, false, Tool.DRIVE, Tool.SEARCH, Tool.DUP);
    public static final Action OPEN = new Action("open", Words.Act.OPEN, Kind.VIRTUAL,
            true, false, false, true, Tool.DRIVE, Tool.SEARCH, Tool.LIST, Tool.TRASH, Tool.DUP);
    /** 1.11.0: פתיחה באמצעות — תמיד חלון בחירת אפליקציה; עריכה — באפליקציה שתבחר (קובץ פקודות — העורך שלנו). */
    public static final Action OPEN_WITH = new Action("openWith", Words.Act.OPEN_WITH, Kind.VIRTUAL,
            true, false, false, true, Tool.DRIVE, Tool.SEARCH, Tool.LIST, Tool.DUP);
    public static final Action EDIT = new Action("edit", Words.Act.EDIT, Kind.VIRTUAL,
            true, false, false, true, Tool.DRIVE, Tool.SEARCH, Tool.LIST, Tool.DUP);
    /** 116: שיתוף — לכל תוכנה (וואטסאפ, ג'ימייל…); גם כמה קבצים, וגם תיקיות (נארזות ל-ZIP, כדי שהמבנה יישמר). */
    public static final Action SHARE = new Action("share", Words.Act.SHARE_DOTS, Kind.PHYSICAL,          // 1.14.0: פיזי
            false, false, false, true, Tool.DRIVE, Tool.SEARCH, Tool.LIST, Tool.DUP);
    /** 117: שליחה למכשיר — כמו שיתוף, אבל רק תוכנות העברה בין מכשירים (Xiaomi Share, שיתוף מהיר, Bluetooth…). */
    public static final Action SEND_DEVICE = new Action("sendDevice", Words.Act.SEND_DOTS, Kind.PHYSICAL,
            false, false, false, true, Tool.DRIVE, Tool.SEARCH, Tool.LIST, Tool.DUP);
    /** 116: פריסת ZIP לתיקייה חדשה לידו — רק כשנבחר קובץ ZIP אחד. */
    public static final Action UNZIP = new Action("unzip", Words.Act.UNZIP_DOTS, Kind.PHYSICAL,
            true, false, false, true, Tool.DRIVE, Tool.SEARCH, Tool.LIST, Tool.DUP);
    public static final Action COPY_PATH = new Action("path", Words.Act.COPY_PATH, Kind.VIRTUAL,
            true, false, false, true, Tool.DRIVE, Tool.SEARCH, Tool.LIST, Tool.DUP);
    /**
     * 1.14.0: ללוח האחד (VClip). 1.16.0 (המילון): "קישור" — רק לרשימות (↪, מתעדכן); "העתקה" — עותק עצמאי של רשימה,
     * ולקובץ — עוד מקום שבו הוא מופיע (המשתמש: "רוצים שהקובץ יופיע בעוד רשימה — כך שזו העתקה"); וירטואלית, לא בתפריט "פיזי".
     */
    public static final Action MOVE_TO_LIST = new Action("move", Words.VClipW.MOVE, Kind.VIRTUAL,
            false, true, false, false, Tool.LIST);
    public static final Action ALSO_TO_LIST = new Action("also", Words.VClipW.COPY, Kind.VIRTUAL,
            false, true, true, false, Tool.LIST);
    public static final Action DUPLICATE_LIST = new Action("duplicate", Words.VClipW.DUPLICATE, Kind.VIRTUAL,
            false, true, false, false, Tool.LIST);
    public static final Action REMOVE_FROM_LIST = new Action("remove", Words.REMOVE_FROM_LIST, Kind.VIRTUAL,
            false, true, false, false, Tool.LIST);
    /** 1.16.0: "הגדרות" לרשימה אחת שנבחרה — שם, צלמית, צבע, תיאור, "למה", עיצוב (ListSettings; עד כאן — שלוש פעולות). */
    public static final Action SETTINGS_LIST = new Action("settings", Words.VClipW.SETTINGS, Kind.VIRTUAL,
            true, true, true, false, Tool.LIST);
    /** 1.13.0: הזזה ידנית ברשימה — פריטים ורשימות שנבחרו (למעלה · למטה · לראש · לסוף). */
    public static final Action ORDER_LIST = new Action("order", Words.Manual.ORDER_DOTS, Kind.VIRTUAL,
            false, true, false, false, Tool.LIST);
    /** 84: שינוי שם של קובץ אחד בכונן, בכל כלי שמציג קבצים — גם ברשימה ידנית. */
    public static final Action RENAME_FILE = new Action("renameFile", Words.RENAME_ON_DRIVE + "…", Kind.PHYSICAL,
            true, false, false, true, Tool.DRIVE, Tool.SEARCH, Tool.LIST, Tool.DUP);
    /** 142: העברה / העתקה כמו באנדרואיד — לוח, ניווט, "הדבקה כאן" (מנהל הקבצים). */
    public static final Action MOVE_TO_FOLDER = new Action("moveTo", Words.Act.MOVE_THEN_PASTE, Kind.PHYSICAL,
            false, false, false, true, Tool.DRIVE, Tool.SEARCH, Tool.LIST, Tool.DUP);
    public static final Action COPY_TO_FOLDER = new Action("copyTo", Words.Act.COPY_THEN_PASTE, Kind.PHYSICAL,
            false, false, false, true, Tool.DRIVE, Tool.SEARCH, Tool.LIST, Tool.DUP);
    public static final Action RESTORE = new Action("restore", Words.Act.RESTORE_ICON, Kind.PHYSICAL,
            false, false, false, false, Tool.TRASH);
    public static final Action DELETE_FROM_DRIVE = new Action("delete", Words.Act.DELETE_ON_DRIVE, Kind.DESTRUCTIVE,
            false, false, false, true, Tool.DRIVE, Tool.SEARCH, Tool.LIST, Tool.DUP);
    public static final Action DELETE_FOREVER = new Action("forever", Words.Act.PURGE, Kind.DESTRUCTIVE,
            false, false, false, false, Tool.TRASH);

    static {
        ADD_TO_LIST.folders = true;
        // הפס (83): ארבע עיקריות לכל כלי, השאר תחת "עוד"; מחיקה תמיד אחרונה ובאדום
        Tool D = Tool.DRIVE, S = Tool.SEARCH, L = Tool.LIST, T = Tool.TRASH, P = Tool.DUP;
        ADD_TO_LIST.bar("list_add", Words.Act.TO_LIST, D, S, P);
        MOVE_TO_FOLDER.bar("folder", Words.MOVE, D, P);
        COPY_TO_FOLDER.bar("copy", Words.COPY, D, P);
        DELETE_FROM_DRIVE.bar("delete", Words.Act.DELETE_ON_DRIVE, D, P);
        OPEN.bar("open", Words.Act.OPEN, T, S);                               // 1.14.0: בחיפוש — במקום הפיזיות
        MOVE_TO_LIST.bar("swap", Words.MOVE, L);
        ALSO_TO_LIST.bar("link", Words.VClipW.COPY_SHORT, L);
        DUPLICATE_LIST.bar("copy", Words.VClipW.DUPLICATE_SHORT, L);
        REMOVE_FROM_LIST.bar("list_remove", Words.REMOVE_FROM_LIST, L);
        RESTORE.bar("restore", Words.Act.RESTORE, T);
        DELETE_FOREVER.bar("close", Words.Act.PURGE, T);
        SHARE.bar("share", Words.Act.SHARE);
        SHARE.folders = true;
        SEND_DEVICE.bar("share", Words.Act.TO_DEVICE);
        SEND_DEVICE.folders = true;
        UNZIP.bar("folder", Words.Act.UNZIP);
        COPY_PATH.bar("link", Words.Act.PATH);
        OPEN_WITH.bar("open", Words.Act.OPEN_WITH_SHORT, S);
        EDIT.bar("edit", Words.Act.EDIT_SHORT);
        SETTINGS_LIST.bar("edit", Words.Top.SETTINGS);
        ORDER_LIST.bar("swap", Words.Manual.ORDER_SHORT);                     // 1.13.0
        RENAME_FILE.bar("edit", Words.Act.NAME_ON_DRIVE);
        DELETE_FROM_DRIVE.folders = true;
        MOVE_TO_FOLDER.folders = true;
        COPY_TO_FOLDER.folders = true;
    }

    /** הסדר בפס: וירטואליות, אחר כך פיזיות, ובסוף ההורסות (64). */
    private static final Action[] ALL = {ADD_TO_LIST, OPEN, OPEN_WITH, EDIT, SEND_DEVICE, SHARE, COPY_PATH, DUPLICATE_LIST, ALSO_TO_LIST, MOVE_TO_LIST,
            REMOVE_FROM_LIST, ORDER_LIST, SETTINGS_LIST, RENAME_FILE, UNZIP, MOVE_TO_FOLDER, COPY_TO_FOLDER, RESTORE, DELETE_FROM_DRIVE, DELETE_FOREVER};

    /**
     * 1.14.0: כלי וירטואלי (חיפוש ורשימה חכמה, רשימה ידנית) — בו כל מה שנוגע בכונן בתפריט "פיזי" (המשתמש: "הכל
     * להפריד. יהיה תפריט פיזי, ומשם כל הפיזי — כולל הפיזי שבווירטואלי"). בכלים שכל עניינם הכונן (מנהל הקבצים,
     * סל, כפולים) — הפיזיות בפס כמו קודם.
     */
    public static boolean virtualTool(Tool t) {
        return t == Tool.SEARCH || t == Tool.LIST;
    }

    /** האם הפעולה בתפריט "פיזי" בכלי הזה. */
    public static boolean inPhysicalMenu(Action a, Tool t) {
        return virtualTool(t) && a.kind != Kind.VIRTUAL;
    }

    /** כל הפעולות שיש להן משמעות בכלי, בסדר הפס. */
    public static List<Action> forTool(Tool t) {
        List<Action> out = new ArrayList<Action>();
        for (Kind k : Kind.values())
            for (Action a : ALL) if (a.kind == k && a.in(t)) out.add(a);
        return out;
    }

    /** הפעולות שמתאימות לבחירה הזו בכלי הזה, בסדר הפס (dirs — תיקיות בכונן שנבחרו; כמו הפס עצמו). */
    public static List<Action> fitting(Tool t, int files, int lists, int dirs, boolean picking) {
        List<Action> out = new ArrayList<Action>();
        for (Action a : forTool(t)) if (a.fits(t, files, lists, dirs, picking)) out.add(a);
        return out;
    }

    public static List<Action> all() {
        List<Action> l = new ArrayList<Action>();
        Collections.addAll(l, ALL);
        return l;
    }

    // ---------- הפס ----------

    /** מה המסך מוסר על הבחירה שלו. */
    public interface Selection {
        /** מסלולי הקבצים שנבחרו. */
        List<String> files();

        /** כמה רשימות נבחרו (רק ברשימה ידנית). */
        int lists();

        /** כמה תיקיות בכונן נבחרו (מנהל קבצים, חיפוש). */
        int dirs();

        /** מסלולי התיקיות שנבחרו (82). */
        List<String> dirPaths();

        /** שם הרשימה שאליה בוחרים קבצים ("➕ מחיפוש"), או null. */
        String picking();

        /** אחרי שינוי: סיום הבחירה ורענון המסך. */
        void changed();
    }

    /**
     * ממלא את הפס בכל הפעולות של הכלי. own — ביצוע שהמסך מספק לפעולות שתלויות בו (רשימות, סל,
     * הוספה לרשימה בזמן בחירה). לשאר יש ביצוע משותף כאן. פעולה שאין לה ביצוע — לא מוצגת.
     */
    public static void fill(final Activity a, SelectBar bar, final Tool tool, final Selection sel,
                            Map<Action, Runnable> own) {
        for (final Action act : forTool(tool)) {
            Runnable run = own != null ? own.get(act) : null;
            if (run == null) run = common(a, act, sel);
            if (run == null) continue;
            boolean pick = act == ADD_TO_LIST && sel.picking() != null;
            String label = pick ? Words.Act.ADD_TO_ICON + sel.picking() : act.label;
            String shortLabel = pick ? Words.Act.ADD_TO + sel.picking() : act.shortLabel;
            bar.addWhen(act.icon, shortLabel, label, run, new SelectBar.When() {
                @Override
                public boolean ok() {
                    return act.fits(tool, sel.files().size(), sel.lists(), sel.dirs(), sel.picking() != null)
                            && (act != UNZIP || isZip(sel.files()));
                }
            }, act.mainIn(tool), act.kind == Kind.DESTRUCTIVE, inPhysicalMenu(act, tool));
        }
    }

    /** האם נבחר בדיוק קובץ ZIP אחד. */
    static boolean isZip(List<String> files) {
        return files.size() == 1 && Text.ext(new File(files.get(0)).getName()).equals("zip");
    }

    /** ביצוע משותף לכל הכלים. */
    private static Runnable common(final Activity a, final Action act, final Selection sel) {
        if (act == OPEN) return new Runnable() {
            @Override
            public void run() {
                File f = new File(sel.files().get(0));
                if (f.isFile()) Ui.open(a, f); else Ui.toast(a, Words.FILE_NOT_AT_PATH);
            }
        };
        if (act == OPEN_WITH || act == EDIT) return new Runnable() {              // 1.11.0
            @Override
            public void run() {
                File f = new File(sel.files().get(0));
                if (!f.isFile()) Ui.toast(a, Words.FILE_NOT_AT_PATH);
                else if (act == EDIT && Batch.isBatch(f.getName())) BatchUi.edit(a, f);
                else Ui.openChoosing(a, f, act == EDIT);
            }
        };
        if (act == SHARE || act == SEND_DEVICE) return new Runnable() {
            @Override
            public void run() {
                Acts.shareMany(a, new ArrayList<String>(sel.files()), new ArrayList<String>(sel.dirPaths()), act == SEND_DEVICE);
                sel.changed();
            }
        };
        if (act == UNZIP) return new Runnable() {
            @Override
            public void run() {
                Physical.askUnzip(a, sel.files().get(0), new Physical.Done() {
                    @Override public void done(Physical.Result r) { sel.changed(); }
                });
            }
        };
        if (act == COPY_PATH) return new Runnable() {
            @Override public void run() { Ui.copyText(a, Words.Act.PATH, sel.files().get(0)); }
        };
        if (act == DELETE_FROM_DRIVE) return new Runnable() {
            @Override
            public void run() {
                Physical.askDelete(a, new ArrayList<String>(sel.files()), sel.dirPaths(), null, new Physical.Done() {
                    @Override public void done(Physical.Result r) { sel.changed(); }
                });
            }
        };
        if (act == RENAME_FILE) return new Runnable() {
            @Override
            public void run() {
                Physical.askRename(a, sel.files().get(0), new Physical.Done() {
                    @Override public void done(Physical.Result r) { sel.changed(); }
                });
            }
        };
        if (act == MOVE_TO_FOLDER || act == COPY_TO_FOLDER) return new Runnable() {
            @Override
            public void run() {
                boolean copy = act == COPY_TO_FOLDER;
                List<String> f = new ArrayList<String>(sel.files()), d = new ArrayList<String>(sel.dirPaths());
                if (!copy) {
                    String no = Physical.cannotMove(Library.get(a), f, d);
                    if (no != null) {
                        Ui.toast(a, no);
                        return;
                    }
                }
                Clip.set(f, d, copy);                                    // 142: ללוח — ומנווטים להדבקה
                sel.changed();
                if (a instanceof Clip.Holder) {                         // 149: הרשימה החכמה — מדביקים בה
                    ((Clip.Holder) a).clipped();
                    return;
                }
                File start = Clip.startDir();                           // מרשימה ידנית — ל"קבצים פיזיים" (1.11.0), בתיקיית הקבצים
                DiskScreen.open(a, start == null ? null : start.getAbsolutePath());
                Ui.toast(a, Clip.label() + Words.Act.PASTE_HINT_FOLDER);
            }
        };
        if (act == ADD_TO_LIST) return new Runnable() {
            @Override
            public void run() {
                Acts.addToList(a, new ArrayList<String>(sel.files()));
                sel.changed();
            }
        };
        return null;
    }
}
