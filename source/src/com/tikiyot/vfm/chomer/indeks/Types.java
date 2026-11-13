package com.tikiyot.vfm;

// קבוצות סוגי הקבצים (145) — מקום אחד: ההגדרה בקובץ ההגדרות ("הגדרות כלליות.ini", סעיף [סוגי קבצים]),
// שהמשתמש יכול לערוך, עם ברירת מחדל; "קובצי נתונים" — כל שאר הסוגים הידועים; "שאר הקבצים" — מה שאינו באף קבוצה.
// בתנאי נכתב שם הקבוצה ("סוג:ספרים|zip"), כך ששינוי קבוצה בקובץ מעדכן את כל הרשימות החכמות.
// בלי ממשק — נבדק. הממשק: SmartForm (הטופס) ו-TypePicker (רשימת הסוגים שבאינדקס).

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class Types {

    static final String SECTION = "סוגי קבצים";  // תחביר
    static final String DATA = "קובצי נתונים", OTHER = "שאר הקבצים";  // תחביר

    /** חמש הקבוצות של הטופס הקודם — כמו שהיו, כדי שתנאים ישנים ייקראו לאותן קבוצות. */
    static final String[][] FIVE = {
            {"ספרים", "pdf|epub|mobi|azw3|djvu"}, {"מסמכים", "doc|docx|txt|rtf|odt"},  // תחביר
            {"תמונות", "jpg|jpeg|png|webp|gif"}, {"שמע", "mp3|m4a|opus|wav|flac"}, {"וידאו", "mp4|mkv|webm|avi"}};  // תחביר

    private Types() { }

    static final class Group {
        final String name;
        final Set<String> exts = new LinkedHashSet<String>();

        Group(String name, String list) {
            this.name = name;
            for (String e : list.split("\\|")) {
                String x = Text.norm(e.trim());
                if (x.startsWith(".")) x = x.substring(1);
                if (!x.isEmpty()) exts.add(x);
            }
        }
    }

    /** ברירת המחדל של "קובצי נתונים": כל הסוגים הידועים שאינם בחמש הקבוצות — גם התקנה (apk) ומכווצים. */
    static String dataDefault() {
        Set<String> five = new LinkedHashSet<String>();
        for (String[] g : FIVE) five.addAll(Arrays.asList(g[1].split("\\|")));
        StringBuilder sb = new StringBuilder();
        for (String k : Text.KNOWN_TYPES) if (!five.contains(k)) sb.append(sb.length() > 0 ? "|" : "").append(k);
        return sb.toString();
    }

    static String[][] defaults() {
        String[][] d = new String[FIVE.length + 1][];
        System.arraycopy(FIVE, 0, d, 0, FIVE.length);
        d[FIVE.length] = new String[]{DATA, dataDefault()};
        return d;
    }

    /** הקבוצות מהקובץ, לפי הסדר שבו. סעיף שאינו קיים — נכתב עם ברירת המחדל. */
    static List<Group> groups() {
        List<Group> out = new ArrayList<Group>();
        for (String[] kv : Conf.section(SECTION, defaults())) out.add(new Group(kv[0], kv[1]));
        return out;
    }

    static Group find(List<Group> all, String name) {
        String n = Text.norm(name.trim());
        for (Group g : all) if (Text.norm(g.name).equals(n)) return g;
        return null;
    }

    static boolean isOther(String name) {
        return Text.norm(name.trim()).equals(Text.norm(OTHER));
    }

    // ---------- הבחירה בטופס, כמודל: מה נבחר ⇄ התנאי ----------

    /** בחירת סוגים: "כל הסוגים", או קבוצות + "שאר הקבצים" + סוגים ידניים. */
    static final class Choice {
        boolean all = true;
        final Set<String> groups = new LinkedHashSet<String>();
        boolean other;
        final Set<String> manual = new LinkedHashSet<String>();

        boolean empty() {
            return groups.isEmpty() && !other && manual.isEmpty();
        }

        /** "סוג:ספרים|zip", "סוג:«קובצי נתונים|שאר הקבצים»", או "" (כל הסוגים). */
        String token() {
            if (all || empty()) return "";
            List<String> parts = new ArrayList<String>(groups);
            if (other) parts.add(OTHER);
            parts.addAll(manual);
            StringBuilder sb = new StringBuilder();
            for (String p : parts) sb.append(sb.length() > 0 ? "|" : "").append(p);
            return Cond.TYPE + ":" + Cond.quote(sb.toString());
        }

        /**
         * קליטת ערך של "סוג:" (בלי המילה). שם קבוצה — לקבוצה; תנאי ישן (הסיומות של קבוצה מהטופס הקודם,
         * גם כמה קבוצות יחד) — לקבוצה שלו; כל השאר — ידני.
         */
        void take(String value, List<Group> all) {
            this.all = false;
            List<String> rest = new ArrayList<String>();
            for (String p : value.split("\\|")) if (!p.trim().isEmpty()) rest.add(p.trim());
            for (String[] old : FIVE) {
                List<String> exts = Arrays.asList(old[1].split("\\|"));
                if (find(all, old[0]) != null && containsAll(rest, exts)) {
                    groups.add(find(all, old[0]).name);
                    removeAll(rest, exts);
                }
            }
            for (String p : rest) {
                Group g = find(all, p);
                if (g != null) groups.add(g.name);
                else if (isOther(p)) other = true;
                else manual.add(p.toLowerCase(java.util.Locale.ROOT));
            }
        }

        private static boolean containsAll(List<String> in, List<String> exts) {
            for (String e : exts) if (!containsIgnoreCase(in, e)) return false;
            return true;
        }

        private static boolean containsIgnoreCase(List<String> in, String e) {
            for (String x : in) if (x.equalsIgnoreCase(e)) return true;
            return false;
        }

        private static void removeAll(List<String> in, List<String> exts) {
            for (java.util.Iterator<String> it = in.iterator(); it.hasNext(); ) {
                String x = it.next();
                for (String e : exts) if (x.equalsIgnoreCase(e)) { it.remove(); break; }
            }
        }
    }
}
