package com.tikiyot.vfm;

// 1.13.1: קיצורים — קובצי פקודות בתיקיית "קיצורים" של האופן. 1.16.1: התפריט עבר לתפריט הפקודות האחד (CmdMenu);
// כאן — רק הקבצים: כתיבה, "שלי" הישנים (נכנסים לרשימה "הפקודות שלי"), כלי "פקודה" בבית, ורשימה שעברה.
// אסור לו: לגעת בקובצי פקודות של המשתמש מחוץ לתיקייה.

import android.app.Activity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

final class Shortcuts {

    static final String DIR = "קיצורים";  // בכונן

    private Shortcuts() { }

    static File dir() {
        return new File(Paths.settings(), DIR);
    }

    /** הקיצורים שלי — לפי השם. */
    static List<File> mine() {
        List<File> out = new ArrayList<File>();
        File[] all = dir().listFiles();
        if (all != null) for (File f : all) if (f.isFile() && Batch.isBatch(f.getName())) out.add(f);
        java.util.Collections.sort(out);
        return out;
    }

    /** כלי "פקודה" בבית שמפנה לקובץ. */
    static void add(Activity a, File f, String name, Runnable changed) {
        if (f == null) return;
        Structure.addDirectFunc(name, FnCommand.KIND, "", Target.of(FnCommand.KIND, FnCommand.FILE, f.getAbsolutePath()));
        changed.run();
        Ui.toast(a, Words.CmdTool.added(name));
    }

    /**
     * קיצור: קובץ בתיקייה, עם הצלמית והשם (ושורה, אם יש) — ומחזיר אותו (null — נכשל). כבר יש קובץ בשם הזה ובאותה
     * שורה — הוא (לא משכפלים מוכנה שנוספה פעמיים); שם תפוס בתוכן אחר — מספר (לא דורסים).
     */
    static File write(String icon, String name, String line) {
        return write(dir(), icon, name, line);
    }

    /** 1.15.0: בתיקיית קיצורים של אופן כלשהו (הסבת הסביבות — בכל האופנים). */
    static File write(File dir, String icon, String name, String line) {
        String base = name.replaceAll("[/\\\\:*?\"<>|\n\t]", " ").trim();
        if (base.isEmpty()) base = FnCommand.KIND;
        StringBuilder sb = new StringBuilder();
        sb.append(Program.ICON).append(' ').append(icon.isEmpty() ? "▶" : icon).append('\n');
        sb.append(Program.NAME).append(' ').append(name).append('\n');
        sb.append(line).append('\n');
        String text = sb.toString();
        File f = new File(dir, base + "." + Batch.EXT);
        for (int i = 2; f.exists(); i++) {
            try {
                if (!line.isEmpty() && Files.readUtf8(f).equals(text)) return f;
            } catch (Throwable ignored) {
            }
            f = new File(dir, base + " (" + i + ")." + Batch.EXT);
        }
        try {
            if (!dir.isDirectory() && !dir.mkdirs()) return null;
            Files.writeAtomic(f, text);
            return f;
        } catch (Throwable e) {
            Log.e("קיצור לבית", e);
            return null;
        }
    }

    /** 1.13.1 (באג): רשימה שינתה שם או מקום — הקיצורים שפותחים אותה עוברים איתה. */
    static int listMoved(List<String> from, List<String> to) {
        int n = 0;
        for (File f : mine()) {
            try {
                String[] lines = Files.readUtf8(f).split("\n", -1);
                boolean changed = false;
                for (int i = 0; i < lines.length; i++) {
                    String now = Verbs.listMoved(lines[i], from, to);
                    if (now != null) {
                        lines[i] = now;
                        changed = true;
                    }
                }
                if (changed) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < lines.length; i++) sb.append(i > 0 ? "\n" : "").append(lines[i]);
                    Files.writeAtomic(f, sb.toString());
                    n++;
                }
            } catch (Throwable e) {
                Log.e("קיצור — רשימה שעברה", e);
            }
        }
        return n;
    }
}
