package com.tikiyot.vfm;

// הגדרות של כלי, כאוסף זוגות. אחראי: להעביר הגדרות בין שורת הפקודה ("פתח כלי"), הטפסים והמסכים.

import java.util.LinkedHashMap;
import java.util.Map;

public final class Target {

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
