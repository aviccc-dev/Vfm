package com.tikiyot.vfm;

// מה שעושים בכלי במסך הבית: הגדרות, הסרה, הזזה. 1.17.1: התפריט עצמו — פס הבחירה (HomeSelect).

import android.app.Activity;
import android.content.DialogInterface;

final class ToolMenu {

    private ToolMenu() { }

    /** "הגדרות" — אחת לכל כלי: רשימה — הטופס של הרשימה; פקודה — העורך; אחר — הטופס של הסוג. */
    static void settings(Activity a, final Structure.Func f, final Runnable changed) {
        if (FnVirtual.KIND.equals(f.kind)) {
            final java.util.List<String> p = FnVirtual.base(FnVirtual.ensureOwn(a, f));
            ListSettings.show(a, p, new ListSettings.Done() {
                @Override
                public void saved(java.util.List<String> path, String name, String desc) {
                    Structure.Func now = Structure.func(f.id);                // שם חדש לרשימה — גם לכלי (המסלול כבר עודכן)
                    if (now != null) Structure.updateFunc(f.id, path.equals(p) ? now.name : name, desc, now.target());
                    changed.run();
                }
            });
            return;
        }
        if (FnCommand.KIND.equals(f.kind) && FnCommand.file(f) != null) {
            BatchUi.edit(a, FnCommand.file(f));
            return;
        }
        configure(a, f, changed);
    }

    /** הטופס של הסוג (רשימה חכמה, כפולים…) — לכלי בבית ולרשימה חכמה שבתוך רשימה (1.16.0). */
    static void configure(Activity a, final Structure.Func f, final Runnable changed) {
        final Func fn = Func.byKind(f.kind);
        if (fn == null) return;
        Func.Done done = new Func.Done() {
            @Override
            public void ok(String name, Target target) {
                Func nf = Func.byKind(target.kind);
                Structure.updateFunc(f.id, name.isEmpty() ? (nf != null ? nf.title : fn.title) : name, f.description, target);
                changed.run();
            }
        };
        Target t = f.target();
        t.params.put("תיאור", f.description);                                // 1.16.0: גם התיאור — בטופס // תחביר
        if (SearchTabs.isSearch(f.kind)) SearchTabs.configure(a, f.kind, f.name, t, done);   // 150
        else fn.configure(a, f.name, t, done);
    }

    /**
     * 1.17.1: הסרת כלים מהבית — אחד או כמה, באישור אחד. כלי של רשימה — הרשימה עצמה מוסרת לסל (אם אין כלי אחר שלה;
     * המילון, 14: "בכל מקום שיש רשימה ממש — הסרה מעבירה לסל, והקישור נשבר"); כלי אחר — רק מהמסך.
     */
    static void remove(Activity a, final java.util.List<Structure.Func> fs, final Runnable changed) {
        boolean lists = false;
        for (Structure.Func f : fs) lists |= f.kind.equals(FnVirtual.KIND);
        String title = fs.size() == 1 ? (fs.get(0).kind.equals(FnVirtual.KIND) ? Words.ToolM.DELETE_LIST_OF : Words.ToolM.DELETE_OF)
                + fs.get(0).name + "\"" : Words.ToolM.DELETE + ": " + fs.size() + Words.ToolM.TOOLS;
        Ui.ask(a, title, Words.NO_TOUCH_FILES, lists ? Words.ToolM.DELETES_TOOL_AND_LIST : Words.ToolM.REMOVES_FROM_SCREEN,
                Words.Box.IN_BIN, Words.ToolM.DELETE, new Runnable() {
                    @Override
                    public void run() {
                        removeNow(fs);
                        changed.run();
                    }
                });
    }

    /** 1.24.0: ההסרה עצמה (בלי לשאול) — גם לפקודות. כל כלי — לסל לפני ההסרה (1.10.0). */
    static void removeNow(java.util.List<Structure.Func> fs) {
        for (Structure.Func f : fs) {
            BinBox.tool(f);
            if (f.kind.equals(FnVirtual.KIND)) FnVirtual.deleteOwnList(f);
            Structure.removeFunc(f.id);
        }
    }

    /** 1.17.1: הזזת כלים בבית — dir ‎-1 ימינה, 1 שמאלה; כמה יחד — כל אחד צעד, בלי לעקוף זה את זה. */
    static boolean shift(java.util.List<Structure.Func> fs, int dir) {
        java.util.List<String> order = new java.util.ArrayList<String>();
        for (Structure.Func f : Structure.load().direct) for (Structure.Func g : fs) if (g.id.equals(f.id)) order.add(f.id);
        if (dir > 0) java.util.Collections.reverse(order);
        boolean any = false;
        for (String id : order) any |= Structure.shiftFunc(id, dir);
        return any;
    }
}
