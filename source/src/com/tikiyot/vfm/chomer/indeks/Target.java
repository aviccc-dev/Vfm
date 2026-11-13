package com.tikiyot.vfm;

// הגדרות של כלי, כאוסף זוגות. אחראי: להעביר הגדרות בין שורת הפקודה ("פתח כלי"), הטפסים והמסכים.

import java.util.LinkedHashMap;
import java.util.Map;

public final class Target {

    /** שמות הכלים — ההגדרה האחת (מבנה 9, 14: עד כאן היו כתובים בכל כלי, בקומת המסכים, וחלקם פעמיים). */
    static final String KIND_SMART = "חכמה", KIND_DUP = "כפולים", KIND_TRASH = "סל", KIND_BAR = "פס", KIND_APP = "אפליקציה",   // תחביר
            KIND_SETTINGS = "הגדרות", KIND_COMMAND = "פקודה";   // תחביר
    /** כל שמות הכלים, בסדר של רשימת הכלים (Func). כלי חדש — גם כאן; בדיקה שומרת על ההתאמה. */
    static final java.util.List<String> KINDS = java.util.Arrays.asList(KIND_SMART, KIND_DUP, KIND_TRASH, KIND_BAR, KIND_APP,
            KIND_SETTINGS, KIND_COMMAND);

    /** יש כלי בשם הזה — בלי להכיר את הכלים עצמם (הם בקומת המסכים). */
    static boolean isKind(String kind) {
        return KINDS.contains(kind);
    }

    public final String kind;
    public final Map<String, String> params = new LinkedHashMap<String, String>();

    private Target(String kind) {
        this.kind = kind;
    }

    public static Target of(String kind, Map<String, String> params) {
        Target t = new Target(kind);
        if (params != null) t.params.putAll(params);
        return t;
    }

    public static Target of(String kind, String... keyValue) {
        Target t = new Target(kind);
        for (int i = 0; i + 1 < keyValue.length; i += 2) {
            String v = keyValue[i + 1];
            if (v != null && !v.isEmpty()) t.params.put(keyValue[i], v);
        }
        return t;
    }

    public String get(String key, String def) {
        String v = params.get(key);
        return v == null || v.isEmpty() ? def : v;
    }

    public boolean is(String key) {
        return "כן".equals(params.get(key));  // תחביר
    }
}
