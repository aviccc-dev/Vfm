package com.tikiyot.vfm;

// שמות ודרכים של רשימות כטקסט: "א / ב" לתצוגה, "א/ב" בפקודה ובכלי. מ0.29: יצא מ־Tree (המנוע הישן נמחק) — רק הטקסט,
// בלי אחסון. אחראי: פירוק, חיבור וניקוי של שמות. אסור לו: לדעת איפה הרשימות נשמרות.

import java.util.ArrayList;
import java.util.List;

final class Names {

    static final String SEP = " / ";

    private Names() { }

    /**
     * דרך לרשימה מטקסט — "א/ב" וגם "א / ב". שם רשימה אינו מכיל "/" ולא "\" (cleanName מחליף אותם בתווים דומים).
     * מ0.40: גם "א\ב" — עד כאן רק שורות "ידנית …" קיבלו "\" (ListCmd), ו"פתח רשימה" / "צור רשימה" / כפתור — לא, ורשימה
     * בשם "א\ב" "לא נמצאה". מקום אחד — לכל מי ששואל על דרך של רשימה.
     */
    static ArrayList<String> parsePath(String text) {
        ArrayList<String> out = new ArrayList<String>();
        if (text != null) for (String p : text.split("[/\\\\]")) if (!p.trim().isEmpty()) out.add(p.trim());
        return out;
    }

    /** שם רשימה תקין: בלי "/" ו-"\" (מוחלפים בתווים דומים), בלי "#" בהתחלה; ריק — "ללא שם". */
    static String cleanName(String name) {
        String s = name == null ? "" : name.replace('/', '∕').replace('\\', '∖').trim();
        while (s.startsWith("#")) s = s.substring(1).trim();
        return s.isEmpty() ? "ללא שם" : s;  // תחביר
    }

    static String fullLabel(List<String> path) {
        if (path == null || path.isEmpty()) return Words.ROOT;
        return join(path, SEP);
    }

    /** מסלול רשימה כמו שכלי ופקודה כותבים אותו — "מועדים/פסח". */
    static String key(List<String> path) {
        return join(path, "/");
    }

    static String join(List<String> path, String sep) {
        StringBuilder sb = new StringBuilder();
        if (path != null) for (String p : path) sb.append(sb.length() > 0 ? sep : "").append(p);
        return sb.toString();
    }

    static String label(List<String> path) {
        return path == null || path.isEmpty() ? Words.ROOT : path.get(path.size() - 1);
    }
}
