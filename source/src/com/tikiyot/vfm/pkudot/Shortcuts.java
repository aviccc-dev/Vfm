package com.tikiyot.vfm;

// 1.13.1: קיצורים — קובצי פקודות בתיקיית "קיצורים" של האופן. 1.16.1: התפריט עבר לתפריט הפקודות האחד (CmdMenu);
// כאן — רק הקבצים: התיקייה, מה שבה (mine), כתיבת קובץ קיצור (write), ורשימה ששינתה שם (listMoved — מחכה לחיבור, מ0.42).
// אסור לו: לגעת בקובצי פקודות של המשתמש מחוץ לתיקייה.


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
                Log.e("Shortcuts.54", ignored);   // מ0.32: לא בולעים בשקט
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

    /**
     * מ0.42 (ב8; המשתמש, 12.11.2026: "שיעדכן גם רשימות וכפתורים ועדיף שתהיה הודעה על כך"): רשימה או חכמה ששינתה שם במסד
     * של האופן — קובצי הפקודות שפונים אליה מתעדכנים (Verbs.renamed): מה שבתיקיית הקיצורים, וקובצי פקודות שברשימות של האופן
     * ושל אופן התפריטים (הכפתורים). פקודות המערכת — לא. שורות אחרי "אופן" של אופן אחר — לא. s — המסד אחרי שינוי השם.
     * מחזיר את הקבצים שעודכנו (להודעה).
     */
    static List<File> listRenamed(Store s, String mode, long id, String from, String to) {
        List<File> done = new ArrayList<File>();
        Store.Item it = s.item(id);
        if (it == null || from == null || to == null || from.equals(to) || (it.type != Store.LIST && it.type != Store.SMART)) return done;
        java.util.Set<String> paths = new java.util.LinkedHashSet<String>();
        for (File f : mine()) paths.add(f.getAbsolutePath());
        commandFiles(s, paths);
        android.content.Context c = DbListScreen.app;
        if (c != null && !Menus.MODE.equals(mode) && Db.dbFile(c, Menus.MODE).isFile()) {
            SqlStore m = SqlStore.openReadOnly(Db.dbFile(c, Menus.MODE));
            try {
                commandFiles(m, paths);
            } finally {
                m.close();
            }
        }
        for (String p : paths) {
            File f = new File(p);
            if (!f.isFile() || Reserved.isSystem(f)) continue;
            try {
                String[] lines = Files.readUtf8(f).split("\n", -1);
                boolean changed = false;
                String in = null;                                                  // "אופן" שבקובץ — עד כאן
                for (int i = 0; i < lines.length; i++) {
                    List<String> tk = Batch.tokens(Bidi.strip(lines[i]).trim());
                    if (tk.size() == 2 && Batch.MODE.equals(Commands.canonical(tk.get(0)))) {
                        in = tk.get(1).trim();
                        continue;
                    }
                    if (in != null && !in.equals(mode)) continue;
                    String now = Verbs.renamed(lines[i], s, mode, id, it.type == Store.SMART, from, to);
                    if (now != null) {
                        lines[i] = now;
                        changed = true;
                    }
                }
                if (!changed) continue;
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < lines.length; i++) sb.append(i > 0 ? "\n" : "").append(lines[i]);
                Files.writeAtomic(f, sb.toString());
                done.add(f);
            } catch (Throwable e) {
                Log.e("רשימה ששינתה שם — " + f.getName(), e);   // בכונן
            }
        }
        return done;
    }

    /** קובצי הפקודות שברשימות של המסד (בכל רשימה, פעם אחת). */
    private static void commandFiles(Store s, java.util.Set<String> out) {
        for (Store.Item l : s.lists())
            for (Store.Member m : s.members(l.id)) {
                if (m.item.type != Store.FILE) continue;
                Store.FileInfo fi = s.file(m.item.id);
                if (fi != null && Batch.isBatch(new File(fi.path).getName())) out.add(fi.path);
            }
    }
}
