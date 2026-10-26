package com.tikiyot.vfm;

// נתונים קצרים על פונקציה: לתצוגה מתחת לצלמית ובשורה בתוך אריח.

import android.content.Context;

public final class Stats {

    private Stats() { }

    /** null אם אין נתון, או אם האינדקס עוד לא מוכן. */
    public static String of(Context c, Structure.Func f) {
        try {
            Library lib = Library.get(c);
            if (f.kind.equals(FnVirtual.KIND)) {
                java.util.ArrayList<String> base = FnVirtual.base(f.target());
                Tree t = Tree.load();
                Tree.Node n = base.isEmpty() ? null : t.find(base);
                if (n == null) return Words.StatsW.ZERO_ITEMS;
                return ListCount.of(t, n, new ListCount.FromLibrary(lib)).text();     // 1.14.0: כמו בתוך הרשימה
            }
            if (!lib.ready) return null;
            if (f.kind.equals(FnSmart.KIND)) {
                String cond = SmartLists.resolve(f.target())[0];               // 147
                Library.Result r = lib.search(cond, false, false);
                if (Cond.parse(r.query).emptyDirs) {                            // 175: תיקיות, לא קבצים
                    int inner = 0;
                    for (Library.Hit x : r.hits) inner += x.innerEmpty;
                    return Words.Empty.count(r.hits.size(), inner);
                }
                long size = 0;
                for (Library.Hit x : r.hits) if (x.rec != null) size += x.rec.size;
                return Files.count(r.total) + Words.StatsW.FILES + (size > 0 ? " · " + Files.size(size) : "");
            }
            if (f.kind.equals(FnCommand.KIND)) {                                // 1.13.0: כמה קבצים התוכנה מסננת
                java.io.File x = FnCommand.file(f);
                return x == null || !x.isFile() ? Words.CmdTool.MISSING : Program.stat(c, x);
            }
            if (f.kind.equals("סל")) {  // תחביר
                java.util.List<Trash.Item> items = Trash.list(lib);
                long size = 0;
                for (Trash.Item it : items) size += it.size;
                return items.isEmpty() ? Words.StatsW.EMPTY : Files.count(items.size()) + Words.StatsW.FILES_DOT + Files.size(size);
            }
        } catch (Throwable t) {
            Log.e("נתונים לפונקציה", t);
        }
        return null;
    }
}
