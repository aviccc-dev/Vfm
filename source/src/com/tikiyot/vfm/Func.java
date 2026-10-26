package com.tikiyot.vfm;

// רשימת הפונקציות. אחראית: להכיר את כל הפונקציות ולתת לכל אחת מקום אחד.
// כל פונקציה בקובץ נפרד, בתבנית זו. הוספת פונקציה = קובץ חדש ושורה ברשימה.

import android.app.Activity;

import java.util.ArrayList;
import java.util.List;

public abstract class Func {

    public interface Done {
        void ok(String name, Target target);
    }

    public final String kind, title, icon;

    protected Func(String kind, String title, String icon) {
        this.kind = kind;
        this.title = title;
        this.icon = icon;
    }

    /** פותח את המסך של הפונקציה עם ההגדרות שלה. */
    public abstract void open(Activity a, Target t);

    /** מסך ההגדרה. name ו-t הם הקיימים, או null ליצירה. */
    public abstract void configure(Activity a, String name, Target t, Done done);

    // ---------- הרשימה ----------

    private static List<Func> all;

    public static synchronized List<Func> all() {
        if (all == null) {
            all = new ArrayList<Func>();
            all.add(new FnVirtual());
            all.add(new FnSmart());
            all.add(new FnDup());
            all.add(new FnTrash());
            all.add(new FnBar());
            all.add(new FnApp());
            all.add(new FnSettings());
            all.add(new FnCommand());                                          // 1.13.0
        }
        return all;
    }

    public static Func byKind(String kind) {
        for (Func f : all()) if (f.kind.equals(kind)) return f;
        return null;
    }

    public static String iconOf(String kind) {
        Func f = byKind(kind);
        return f != null ? f.icon : "📄";
    }
}
