package com.tikiyot.vfm;

// 1.13.0: הזזה ידנית ברשימה (המשתמש, 15.10: "לאפשר מיון ידני ברשימה הידנית") — למעלה · למטה · לראש · לסוף.
// הנבחרים זזים יחד ושומרים על הסדר ביניהם. אחראי: הסדר ברשימה בלבד (בלי ממשק — נבדק).

import java.util.ArrayList;
import java.util.List;

final class Order {

    static final int UP = 0, DOWN = 1, TOP = 2, BOTTOM = 3;

    interface Picked<T> {
        boolean is(T t);
    }

    private Order() { }

    /** מזיז את הנבחרים; true — משהו זז. */
    static <T> boolean move(List<T> l, int how, Picked<T> p) {
        List<T> before = new ArrayList<T>(l);
        if (how == TOP || how == BOTTOM) {
            List<T> in = new ArrayList<T>(), out = new ArrayList<T>();
            for (T t : l) (p.is(t) ? in : out).add(t);
            if (in.isEmpty()) return false;
            l.clear();
            if (how == TOP) {
                l.addAll(in);
                l.addAll(out);
            } else {
                l.addAll(out);
                l.addAll(in);
            }
        } else if (how == UP) {
            for (int i = 1; i < l.size(); i++)
                if (p.is(l.get(i)) && !p.is(l.get(i - 1))) swap(l, i, i - 1);
        } else if (how == DOWN) {
            for (int i = l.size() - 2; i >= 0; i--)
                if (p.is(l.get(i)) && !p.is(l.get(i + 1))) swap(l, i, i + 1);
        }
        return !before.equals(l);
    }

    private static <T> void swap(List<T> l, int i, int j) {
        T x = l.get(i);
        l.set(i, l.get(j));
        l.set(j, x);
    }
}
