package com.tikiyot.vfm;

// עותק קריא של המבנה. אחראי: לבנות קובץ לכל כלי במסך הבית, לקריאה בלבד.
// אסור לו: לשמש מקור. המקור הוא הקובץ הראשי.

import java.io.File;
import java.util.Map;

public final class Mirror {

    private Mirror() { }

    public static void rebuild() {
        if (Structure.testFile != null) return;
        try {
            File root = Paths.mirror();
            clear(root);
            File old = new File(Paths.settings(), "אריחים");                    // 1.17.5: השם הישן — העותק עבר ל"כלים"  // בכונן
            if (old.isDirectory()) {
                clear(old);
                old.delete();
            }
            Structure.Model m = Structure.load();
            int n = 0;
            for (Structure.Func f : m.direct) {
                n++;
                write(new File(root, String.format("%02d %s.ini", n, safe(f.name))), funcText(f, "ישיר"));  // תחביר
            }
        } catch (Throwable t) {
            Log.e("בניית העותק", t);
        }
    }

    private static String head() {
        return "; עותק מהקובץ הראשי \"" + Paths.structure().getName() + "\". לעריכה יש לערוך אותו.\n\n";  // תחביר
    }

    private static String funcText(Structure.Func f, String where) {
        StringBuilder sb = new StringBuilder(head());
        sb.append("[פונקציה]\n");  // תחביר
        sb.append("שם = ").append(f.name).append('\n');  // תחביר
        sb.append("סוג = ").append(f.kind).append('\n');  // תחביר
        sb.append("תיאור = ").append(f.description).append('\n');  // תחביר
        sb.append("שייכת ל = ").append(where).append('\n');  // תחביר
        if (!f.params.isEmpty()) {
            sb.append("\n[הגדרות]\n");  // תחביר
            for (Map.Entry<String, String> e : f.params.entrySet())
                sb.append(e.getKey()).append(" = ").append(e.getValue()).append('\n');
        }
        return sb.toString();
    }

    private static String safe(String name) {
        String s = name.replace('/', '-').replace('\\', '-').trim();
        return s.isEmpty() ? "ללא שם" : s;  // תחביר
    }

    private static void clear(File dir) {
        File[] all = dir.listFiles();
        if (all == null) return;
        for (File f : all) {
            if (f.isDirectory()) {
                clear(f);
                f.delete();
            } else if (f.getName().endsWith(".ini")) {
                f.delete();
            }
        }
    }

    private static void write(File f, String content) {
        try {
            Files.writeAtomic(f, content);
        } catch (Throwable t) {
            Log.e("כתיבת " + f.getName(), t);
        }
    }
}
