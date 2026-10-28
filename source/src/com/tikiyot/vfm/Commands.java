package com.tikiyot.vfm;

// 1.22.5: ההגדרה האחת של שפת הפקודות (המשתמש, 28.10: "זה היה צריך להיות מזמן כבר הסדר בכל התוכנה; אז תעשה לפחות
// בפקודות"). כל פקודה כתובה כאן פעם אחת: השם, השמות הנרדפים, מה היא עושה, והפרמטרים — סוג, שם, הסבר, בסיסי או
// מתקדם, והאפשרויות. מכאן לוקחים: המנוע (השמות הנרדפים), ההסבר והשימוש בעורך ובשגיאות, והממשק לפקודות (claude/mimshak-pkudot.md).
// המילים של התחביר עצמו (/תפוס:, /יומן: …) — הקבועים של Batch; הטקסטים — Words.Def. בלי ממשק — נבדק.
// אחראי: לתאר את השפה. אסור לו: לפענח שורות או להריץ (זה Batch, BatchLine, Verbs).

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

final class Commands {

    /** משפחת הפקודה: קבצים (באנגלית, המנוע של Batch), פעלים בעברית (Verbs), או שורת כותרת. */
    enum Family { FILE, VERB, HEADER }

    /** סוג הפרמטר — קובע את הבוחר בממשק. */
    enum Type { PATH, FILES, NAME, TEXT, FLAG, CHOICE, PERCENT, TIME, DATE, SIZE, LIST }

    /** פרמטר אחד: מיקומי (key ריק) או מתג (key = "/S", "/תפוס:" …). */
    static final class Param {
        /** המתג כמו שנכתב ("/S", "/תפוס:"), או ריק — פרמטר לפי מקום. */
        final String key;
        /** שמות נרדפים למתג ("/מסלול", "/Y"…). */
        final List<String> aliases;
        final Type type;
        final String label, help;
        /** מתקדם — מקופל בממשק. */
        final boolean advanced;
        /** תחביר ישן (XCOPY / ROBOCOPY) — מובן, לא מוצג. */
        final boolean compat;
        /** האפשרויות של CHOICE. */
        final List<String> options;
        /** במקום "<שם>" בשורת השימוש (למשל START). */
        final String placeholder;

        Param(String key, String[] aliases, Type type, String label, String help, boolean advanced, boolean compat,
              String[] options, String placeholder) {
            this.key = key;
            this.aliases = Collections.unmodifiableList(Arrays.asList(aliases == null ? new String[0] : aliases));
            this.type = type;
            this.label = label;
            this.help = help;
            this.advanced = advanced;
            this.compat = compat;
            this.options = Collections.unmodifiableList(Arrays.asList(options == null ? new String[0] : options));
            this.placeholder = placeholder;
        }

        boolean isSwitch() {
            return !key.isEmpty();
        }

        /** האם הטוקן הוא המתג הזה (או שם נרדף). מתג עם ערך ("/תפוס:לדלג") — לפי ההתחלה. */
        boolean matches(String token) {
            if (!isSwitch() || token == null) return false;
            for (String k : all())
                if (k.endsWith(":") ? token.regionMatches(true, 0, k, 0, k.length()) : token.equalsIgnoreCase(k)) return true;
            return false;
        }

        List<String> all() {
            List<String> out = new ArrayList<String>();
            out.add(key);
            out.addAll(aliases);
            return out;
        }

        /** "[/תפוס:לשאול|לדלג…]" — לשורת השימוש. */
        String usage() {
            if (!isSwitch()) return placeholder != null ? placeholder : "<" + label + ">";
            String v;
            switch (type) {
                case CHOICE: v = join(options, "|"); break;
                case PERCENT: v = "N%"; break;
                case FLAG: v = ""; break;
                case PATH: v = Words.Def.V_FILE; break;
                case TIME: v = Words.Def.V_TIME; break;
                case LIST: v = Words.Def.V_LIST; break;
                default: v = "…";
            }
            return "[" + key + v + "]";
        }
    }

    /** פקודה אחת. */
    static final class Def {
        final String name;
        final List<String> aliases;
        final Family family;
        /** מה היא עושה (Words.Def). */
        final String what;
        final List<Param> params;
        /** צורה נוספת של אותה פקודה ("COMP /רשימה:<רשימה> <יעד>") — לשורת השימוש. */
        final String alternative;

        Def(String name, String[] aliases, Family family, String what, Param[] params, String alternative) {
            this.name = name;
            this.aliases = Collections.unmodifiableList(Arrays.asList(aliases == null ? new String[0] : aliases));
            this.family = family;
            this.what = what;
            this.params = Collections.unmodifiableList(Arrays.asList(params == null ? new Param[0] : params));
            this.alternative = alternative;
        }

        /** "COPY <מקור> <יעד> [/S] [/תפוס:…] …" — לפקודות הקבצים נבנה מהפרמטרים; לפעלים — מהמילון. */
        String usage() {
            if (family == Family.VERB) return Words.Verb.usage(name);
            StringBuilder sb = new StringBuilder(name);
            for (Param p : params) if (!p.compat) sb.append(' ').append(p.usage());
            if (alternative != null) sb.append("  ·  ").append(alternative);
            return sb.toString();
        }

        List<Param> positional() {
            List<Param> out = new ArrayList<Param>();
            for (Param p : params) if (!p.isSwitch()) out.add(p);
            return out;
        }

        List<Param> switches() {
            List<Param> out = new ArrayList<Param>();
            for (Param p : params) if (p.isSwitch()) out.add(p);
            return out;
        }

        /** המתג שהטוקן שייך לו, או null. */
        Param switchFor(String token) {
            for (Param p : params) if (p.matches(token)) return p;
            return null;
        }
    }

    private Commands() { }

    // ---------- בניית ההגדרות ----------

    private static Param pos(Type t, String label, String help) {
        return new Param("", null, t, label, help, false, false, null, null);
    }

    private static Param pos(Type t, String label, String help, String placeholder) {
        return new Param("", null, t, label, help, false, false, null, placeholder);
    }

    private static Param sw(String key, String[] aliases, Type t, String label, String help, String... options) {
        return new Param(key, aliases, t, label, help, true, false, options, null);
    }

    private static Param compat(String key, Type t) {
        return new Param(key, null, t, key, Words.Def.H_COMPAT, true, true, null, null);
    }

    /** /תאריך: /גודל: /A:H — הסינון של COPY / MOVE / DEL, והתחביר הישן שלו. */
    private static List<Param> filters() {
        return Arrays.asList(
                sw("/" + Batch.SW_DATE, null, Type.DATE, Words.Def.P_DATE, Words.Def.H_DATE),
                sw("/" + Batch.SW_SIZE, null, Type.SIZE, Words.Def.P_SIZE, Words.Def.H_SIZE),
                hidden(),
                compat("/D:", Type.DATE), compat("/MAXAGE:", Type.DATE), compat("/MINAGE:", Type.DATE),
                compat("/MAX:", Type.SIZE), compat("/MIN:", Type.SIZE), compat("/Q", Type.FLAG), compat("/F", Type.FLAG));
    }

    private static Param hidden() {
        return sw("/A:H", new String[]{"/A", "/A:"}, Type.FLAG, Words.Def.P_HIDDEN, Words.Def.H_HIDDEN);
    }

    private static Param taken() {
        return sw(Batch.SW_TAKEN, new String[]{"/Y", "/-Y"}, Type.CHOICE, Words.Def.P_TAKEN, Words.Def.H_TAKEN,
                Batch.TAKEN_ASK, Batch.TAKEN_SKIP, Batch.TAKEN_REPLACE, Batch.TAKEN_STOP);
    }

    private static Param[] list(Object... parts) {
        List<Param> out = new ArrayList<Param>();
        for (Object o : parts) {
            if (o instanceof Param) out.add((Param) o);
            else for (Object x : (List<?>) o) out.add((Param) x);
        }
        return out.toArray(new Param[0]);
    }

    private static Def transfer(String name, String srcHelp, boolean state) {
        List<Param> ps = new ArrayList<Param>(Arrays.asList(
                pos(Type.FILES, Words.Def.P_SOURCE, srcHelp),
                pos(Type.PATH, Words.Def.P_DEST, Words.Def.H_DEST),
                sw("/S", new String[]{Batch.SW_KEEP}, Type.FLAG, Words.Def.P_KEEP, Words.Def.H_KEEP),
                taken(),
                sw(Batch.SW_JOURNAL, null, Type.PATH, Words.Def.P_JOURNAL, Words.Def.H_JOURNAL),
                sw(Batch.SW_LIMIT, null, Type.PERCENT, Words.Def.P_LIMIT, Words.Def.H_LIMIT)));
        if (state) ps.add(sw(Batch.SW_STATE, null, Type.TIME, Words.Def.P_STATE, Words.Def.H_STATE));
        ps.addAll(filters());
        return new Def(name, null, Family.FILE, name.equals("COPY") ? Words.Def.W_COPY : Words.Def.W_MOVE,
                ps.toArray(new Param[0]), null);
    }

    private static Def verb(String name, String what) {
        return new Def(name, null, Family.VERB, what,
                new Param[]{pos(Type.TEXT, Words.Def.P_ARG, Words.Verb.usage(name))}, null);
    }

    /** כל הפקודות — בסדר שבו הן מוצגות. */
    static final List<Def> ALL = Collections.unmodifiableList(Arrays.asList(
            transfer("COPY", Words.Def.H_COPY_SRC, true),
            transfer("MOVE", Words.Def.H_MOVE_SRC, false),
            new Def("DEL", new String[]{"ERASE"}, Family.FILE, Words.Def.W_DEL,
                    list(pos(Type.FILES, Words.Def.P_FILES, Words.Def.H_DEL_SRC), filters()), null),
            new Def("REN", new String[]{"RENAME"}, Family.FILE, Words.Def.W_REN, list(
                    pos(Type.PATH, Words.Def.P_FILE, Words.Def.H_REN_SRC),
                    pos(Type.NAME, Words.Def.P_NEW_NAME, Words.Def.H_NEW_NAME)), null),
            new Def("MD", new String[]{"MKDIR"}, Family.FILE, Words.Def.W_MD,
                    list(pos(Type.PATH, Words.Def.P_FOLDER, Words.Def.H_MD)), null),
            new Def("RD", new String[]{"RMDIR"}, Family.FILE, Words.Def.W_RD, list(
                    pos(Type.PATH, Words.Def.P_FOLDER, Words.Def.H_RD),
                    sw("/S", null, Type.FLAG, Words.Def.P_NOT_EMPTY, Words.Def.H_NOT_EMPTY)), null),
            new Def("COMP", null, Family.FILE, Words.Def.W_COMP, list(
                    pos(Type.PATH, Words.Def.P_SOURCE, Words.Def.H_COMP_SRC),
                    pos(Type.PATH, Words.Def.P_DEST, Words.Def.H_COMP_DEST),
                    hidden(),
                    sw(Batch.SW_LIST, null, Type.LIST, Words.Def.P_LIST, Words.Def.H_LIST)),
                    "COMP " + Batch.SW_LIST + "<" + Words.Def.P_LIST + "> <" + Words.Def.P_DEST + ">"),
            new Def("START", null, Family.FILE, Words.Def.W_START,
                    list(pos(Type.PATH, Words.Def.P_WHAT_OPEN, Words.Def.H_START, Words.Def.U_START)), null),
            new Def("ECHO", null, Family.FILE, Words.Def.W_ECHO, list(pos(Type.TEXT, Words.Def.P_TEXT, Words.Def.H_TEXT)), null),
            new Def("REM", null, Family.FILE, Words.Def.W_REM, list(pos(Type.TEXT, Words.Def.P_TEXT, Words.Def.H_TEXT)), null),
            verb(Verbs.OPEN, Words.Def.W_OPEN),
            verb(Verbs.CREATE, Words.Def.W_CREATE),
            verb(Verbs.UPDATE, Words.Def.W_UPDATE),
            verb(Verbs.FIND, Words.Def.W_FIND),
            verb(Verbs.BACK, Words.Def.W_BACK),
            verb(Verbs.RUN, Words.Def.W_RUN),
            verb(Verbs.ADD, Words.Def.W_ADD),
            new Def(Batch.AS_IF2, new String[]{Batch.AS_IF}, Family.HEADER, Words.Def.W_AS_IF, new Param[0], null)));

    // ---------- שאילתות ----------

    /** הפקודה לפי שם או שם נרדף (אנגלית — בלי תלות באותיות גדולות). null — לא מוכרת. */
    static Def get(String word) {
        if (word == null) return null;
        String w = word.trim(), u = w.toUpperCase(Locale.ROOT);
        for (Def d : ALL) {
            if (d.name.equals(u) || d.name.equals(w)) return d;
            for (String a : d.aliases) if (a.equals(u) || a.equals(w)) return d;
        }
        return null;
    }

    /** השם הקבוע של פקודה ("MKDIR" ← "MD"). מילה לא מוכרת — כמו שהיא, באותיות גדולות. */
    static String canonical(String word) {
        Def d = get(word);
        return d != null && d.family == Family.FILE ? d.name : word.toUpperCase(Locale.ROOT);
    }

    /** שמות פקודות הקבצים, בסדר. */
    static String[] fileNames() {
        List<String> out = new ArrayList<String>();
        for (Def d : ALL) if (d.family == Family.FILE) out.add(d.name);
        return out.toArray(new String[0]);
    }

    static List<Def> of(Family f) {
        List<Def> out = new ArrayList<Def>();
        for (Def d : ALL) if (d.family == f) out.add(d);
        return out;
    }

    private static String join(List<String> l, String sep) {
        StringBuilder sb = new StringBuilder();
        for (String s : l) sb.append(sb.length() > 0 ? sep : "").append(s);
        return sb.toString();
    }
}
