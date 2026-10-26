package com.tikiyot.vfm;

// קובץ הגדרות. אחראי: קריאה וכתיבה של ini תוך שמירת כל שורה כמות שהיא.
// אסור לו: להבין את המשמעות של השורות.

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public final class Ini {

    public static final class Line {
        public String raw = "";
        public String section = "";
        public String key;        // null כשאינה שורת מפתח
        public String value;

        public boolean isHeader() {
            String t = raw.trim();
            return t.startsWith("[") && t.endsWith("]");
        }

        public String headerName() {
            String t = raw.trim();
            return t.substring(1, t.length() - 1).trim();
        }
    }

    public final File file;
    public final List<Line> lines = new ArrayList<Line>();
    public long readAt;
    /** 90: הקובץ קיים ולא נקרא — אסור לשמור (הייתה נשמרת גרסה ריקה). */
    public boolean unreadable;

    private Ini(File f) {
        file = f;
    }

    // ---------- קריאה ----------

    public static Ini load(File f) {
        Ini ini = new Ini(f);
        if (!f.exists()) return ini;
        String text;
        try {
            text = Files.readUtf8(f);
        } catch (Throwable t) {
            Log.e("קריאת " + f.getName(), t);
            ini.unreadable = true;
            return ini;
        }
        ini.readAt = f.lastModified();
        String section = "";
        for (String raw : text.split("\r\n|\r|\n")) {
            Line l = new Line();
            l.raw = raw.replace("\uFEFF", "");
            String t = l.raw.trim();
            if (t.startsWith("[") && t.endsWith("]")) {
                section = t.substring(1, t.length() - 1).trim();
            } else if (!t.isEmpty() && !t.startsWith(";") && !t.startsWith("#")) {
                int eq = t.indexOf('=');
                if (eq > 0) {
                    l.key = t.substring(0, eq).trim();
                    l.value = t.substring(eq + 1).trim();
                }
            }
            l.section = section;
            ini.lines.add(l);
        }
        return ini;
    }

    public static void createIfMissing(File f, String content) {
        if (f.exists()) return;
        try {
            OutputStream out = new FileOutputStream(f);
            try {
                out.write(content.getBytes("UTF-8"));
            } finally {
                out.close();
            }
        } catch (Throwable t) {
            Log.e("יצירת " + f.getName(), t);
        }
    }

    // ---------- שאילתות ----------

    public List<Line> entries(String section) {
        List<Line> out = new ArrayList<Line>();
        for (Line l : lines) if (l.key != null && l.section.equals(section)) out.add(l);
        return out;
    }

    public List<String> sections() {
        List<String> out = new ArrayList<String>();
        for (Line l : lines) if (l.key != null && !out.contains(l.section)) out.add(l.section);
        return out;
    }

    public String get(String section, String key, String def) {
        for (Line l : entries(section))
            if (l.key.equals(key)) return l.value == null || l.value.isEmpty() ? def : l.value;
        return def;
    }

    /** סוף המקטע שמתחיל בשורה at, בלי שורות ריקות בסופו. */
    public int sectionEnd(int at) {
        int end = lines.size();
        for (int i = at + 1; i < lines.size(); i++) {
            if (lines.get(i).isHeader()) {
                end = i;
                break;
            }
        }
        while (end > at + 1 && lines.get(end - 1).raw.trim().isEmpty()) end--;
        return end;
    }

    // ---------- שינוי, תמיד ברמת השורה ----------

    public static Line line(String raw, String section) {
        Line l = new Line();
        l.raw = raw;
        l.section = section;
        String t = raw.trim();
        int eq = t.indexOf('=');
        if (eq > 0 && !t.startsWith(";") && !t.startsWith("#") && !t.startsWith("[")) {
            l.key = t.substring(0, eq).trim();
            l.value = t.substring(eq + 1).trim();
        }
        return l;
    }

    public void set(Line l, String key, String value) {
        l.key = key;
        l.value = value;
        l.raw = key + " = " + value;
    }

    public Line add(String section, String key, String value) {
        int last = -1, lastKey = -1;
        boolean exists = false;
        for (int i = 0; i < lines.size(); i++) {
            if (!lines.get(i).section.equals(section)) continue;
            exists = true;
            last = i;
            if (lines.get(i).key != null) lastKey = i;
        }
        if (lastKey >= 0) last = lastKey;
        if (!exists) {
            lines.add(line("", ""));
            lines.add(line("[" + section + "]", section));
            last = lines.size() - 1;
        }
        Line l = line(key + " = " + value, section);
        lines.add(last + 1, l);
        return l;
    }

    public void remove(Line l) {
        lines.remove(l);
    }

    public boolean move(Line l, int dir) {
        int from = lines.indexOf(l);
        if (from < 0) return false;
        int to = -1;
        if (dir < 0) {
            for (int i = from - 1; i >= 0; i--)
                if (lines.get(i).key != null && lines.get(i).section.equals(l.section)) { to = i; break; }
        } else {
            for (int i = from + 1; i < lines.size(); i++)
                if (lines.get(i).key != null && lines.get(i).section.equals(l.section)) { to = i; break; }
        }
        if (to < 0) return false;
        lines.set(from, lines.get(to));
        lines.set(to, l);
        return true;
    }

    // ---------- שמירה ----------

    public boolean changedOutside() {
        return file.exists() && readAt > 0 && file.lastModified() != readAt;
    }

    public void save() throws Exception {
        if (unreadable) throw new java.io.IOException(Words.IniW.NOT_READ_SAVE_DELAYED + file.getName() + ")");
        StringBuilder sb = new StringBuilder();
        for (Line l : lines) sb.append(l.raw).append('\n');
        Files.backup(file, "הגדרות");  // בכונן
        Files.writeAtomic(file, sb.toString());
        readAt = file.lastModified();
    }

    public boolean saveQuiet() {
        try {
            save();
            return true;
        } catch (Throwable t) {
            Log.e("שמירת " + file.getName(), t);
            return false;
        }
    }
}
