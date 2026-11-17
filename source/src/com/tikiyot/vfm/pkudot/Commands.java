package com.tikiyot.vfm;

// 1.22.5: ההגדרה האחת של שפת הפקודות (המשתמש, 28.10: "זה היה צריך להיות מזמן כבר הסדר בכל התוכנה; אז תעשה לפחות
// בפקודות"). כל פקודה כתובה כאן פעם אחת: השם, השמות הנרדפים, מה היא עושה, והפרמטרים — סוג, שם, הסבר, בסיסי או
// מתקדם, והאפשרויות. מכאן לוקחים: המנוע (השמות הנרדפים), ההסבר והשימוש בעורך ובשגיאות, והממשק לפקודות (claude/mimshak-pkudot.md).
// המילים של התחביר עצמו (/תפוס:, /יומן: …) — הקבועים של Batch; הטקסטים — Words.Def. בלי ממשק — נבדק.
// אחראי: לתאר את השפה. אסור לו: לפענח שורות או להריץ (זה Batch, Verbs).

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
        /** השם הקצר בעברית — לתפריט ולחיפוש (בפעלים — הפועל עצמו). */
        final String he;
        final List<String> aliases;
        final Family family;
        /** מה היא עושה (Words.Def). */
        final String what;
        final List<Param> params;
        /** צורה נוספת של אותה פקודה ("COMP /רשימה:<רשימה> <יעד>") — לשורת השימוש. */
        final String alternative;

        Def(String name, String he, String[] aliases, Family family, String what, Param[] params, String alternative) {
            this.name = name;
            this.he = he;
            this.aliases = Collections.unmodifiableList(Arrays.asList(aliases == null ? new String[0] : aliases));
            this.family = family;
            this.what = what;
            this.params = Collections.unmodifiableList(Arrays.asList(params == null ? new Param[0] : params));
            this.alternative = alternative;
        }

        /** "COPY <מקור> <יעד> [/S] [/תפוס:…] …" — לפקודות הקבצים נבנה מהפרמטרים; לפעלים — מהמילון. */
        String usage() {
            if (family == Family.VERB) return Words.Verb.usage(name);
            StringBuilder sb = new StringBuilder(word(this));                     // מ0.38: בעברית
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
                sw("/" + Syntax.SW_DATE, null, Type.DATE, Words.Def.P_DATE, Words.Def.H_DATE),
                sw("/" + Syntax.SW_SIZE, null, Type.SIZE, Words.Def.P_SIZE, Words.Def.H_SIZE),
                sw("/" + Syntax.SW_CONTAINS, null, Type.TEXT, Words.Def.P_CONTAINS, Words.Def.H_CONTAINS),   // מ0.40
                hidden(),
                compat("/D:", Type.DATE), compat("/MAXAGE:", Type.DATE), compat("/MINAGE:", Type.DATE),
                compat("/MAX:", Type.SIZE), compat("/MIN:", Type.SIZE), compat("/Q", Type.FLAG), compat("/F", Type.FLAG));
    }

    private static Param hidden() {
        return sw("/A:H", new String[]{"/A", "/A:"}, Type.FLAG, Words.Def.P_HIDDEN, Words.Def.H_HIDDEN);
    }

    /** מבנה 109: "צור תיקייה" (ובשורת רשימות — "צור תיקייה ידנית") בשם קיים: לעצור (בלי מתג) · לדלג · לשנות-שם. */
    private static Param takenNew() {
        return sw(Syntax.SW_TAKEN, null, Type.CHOICE, Words.Def.P_TAKEN, Words.Def.H_TAKEN_NEW,
                Syntax.TAKEN_STOP, Syntax.TAKEN_SKIP, Syntax.TAKEN_RENAME);
    }

    /** מבנה 110 (ה6): "כתוב" על קובץ קיים: לעצור (בלי מתג) · להחליף (הקודם לסל) · לדלג · לשנות-שם. */
    private static Param takenWrite() {
        return sw(Syntax.SW_TAKEN, null, Type.CHOICE, Words.Def.P_TAKEN, Words.Def.H_TAKEN_WRITE,
                Syntax.TAKEN_STOP, Syntax.TAKEN_REPLACE, Syntax.TAKEN_SKIP, Syntax.TAKEN_RENAME);
    }

    private static Param taken() {
        return sw(Syntax.SW_TAKEN, new String[]{"/Y", "/-Y"}, Type.CHOICE, Words.Def.P_TAKEN, Words.Def.H_TAKEN,
                Syntax.TAKEN_ASK, Syntax.TAKEN_SKIP, Syntax.TAKEN_REPLACE, Syntax.TAKEN_STOP, Syntax.TAKEN_RENAME);
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
                sw("/S", new String[]{Syntax.SW_KEEP}, Type.FLAG, Words.Def.P_KEEP, Words.Def.H_KEEP),
                taken(),
                sw(Syntax.SW_JOURNAL, null, Type.PATH, Words.Def.P_JOURNAL, Words.Def.H_JOURNAL),
                sw(Syntax.SW_LIMIT, null, Type.PERCENT, Words.Def.P_LIMIT, Words.Def.H_LIMIT)));
        if (state) ps.add(sw(Syntax.SW_STATE, null, Type.TIME, Words.Def.P_STATE, Words.Def.H_STATE));
        ps.addAll(filters());
        return new Def(name, name.equals("COPY") ? Words.Def.N_COPY : Words.Def.N_MOVE,
                name.equals("COPY") ? new String[]{HE_COPY, HE_DUP} : new String[]{HE_MOVE}, Family.FILE,
                name.equals("COPY") ? Words.Def.W_COPY : Words.Def.W_MOVE,
                ps.toArray(new Param[0]), null);
    }

    private static Def verb(String name, String what) {
        return new Def(name, name, null, Family.VERB, what,
                new Param[]{pos(Type.TEXT, Words.Def.P_ARG, Words.Verb.usage(name))}, null);
    }

    /**
     * מ0.38 (המשתמש, 11.11.2026: "לכל הפקודות יהיו תואם עברי, כולל הפרמטרים" · על טבלת השמות: "עזוב, תריץ כך. אחר כך זה לא
     * סיפור לעדכן"): השם העברי של כל פקודת קבצים. האנגלי ממשיך לעבוד; העורך כותב את העברי.
     */
    static final String HE_COPY = "העתק", HE_DUP = "הכפל", HE_MOVE = "העבר", HE_DEL = "מחק", HE_REN = "שנה", HE_UNLIST = "בטל",   // תחביר
            HE_COMP = "השווה", HE_START = "הפעל", HE_ECHO = "הודעה", HE_REM = "הערה",   // תחביר
            HE_MD = "צור תיקייה", HE_RD = "מחק תיקייה", HE_DEEP = "/עמוק", HE_HIDDEN = "/נסתרים";   // תחביר
    private static final String[][] TWO_WORDS = {{HE_MD, "MD"}, {HE_RD, "RD"},
            {Words.DbCmd.N_DROP_MODE, Syntax.DROP_MODE}, {Words.DbCmd.N_DROP_MODE_OLD, Syntax.DROP_MODE}, {Words.DbCmd.N_UNTRASH, Syntax.UNTRASH}, {Words.Cmd.N_EXAMPLES, Syntax.EXAMPLES}, {Words.DbCmd.N_PURGE, Syntax.PURGE}};   // מ0.45
    static final String[][] HE_SWITCHES = {{HE_DEEP, "/S"}, {HE_HIDDEN, "/A:H"}};

    /** השורה בצורה שהמנוע קורא: "צור תיקייה" ← MD, "מחק תיקייה" ← RD, והמתגים העבריים ← האנגליים (מחוץ למרכאות). */
    static String normal(String line) {
        String l = line == null ? "" : line.trim();
        for (String[] m : TWO_WORDS)
            if (l.equals(m[0]) || l.startsWith(m[0] + " ")) {
                l = m[1] + l.substring(m[0].length());
                break;
            }
        if (l.indexOf('/') < 0) return l;
        StringBuilder out = new StringBuilder();
        boolean q = false;
        int i = 0;
        while (i < l.length()) {
            char c = l.charAt(i);
            if (c == '"') q = !q;
            boolean start = !q && c == '/' && (i == 0 || Character.isWhitespace(l.charAt(i - 1)));
            String hit = null, to = null;
            if (start) for (String[] m : HE_SWITCHES) {
                int e = i + m[0].length();
                if (l.startsWith(m[0], i) && (e == l.length() || Character.isWhitespace(l.charAt(e)))) { hit = m[0]; to = m[1]; }
            }
            if (hit != null) {
                out.append(to);
                i += hit.length();
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    /** המילה שהעורך כותב לפקודה: העברית (אם יש), אחרת השם. */
    static String word(Def d) {
        if (d.family != Family.FILE) return d.name;
        for (String[] m : TWO_WORDS) if (d.name.equals(m[1])) return m[0];   // צור תיקייה · מחק תיקייה · מ0.45: מחק אופן · שחזר מהסל · רוקן סל
        for (String a : d.aliases) if (!a.isEmpty() && a.charAt(0) >= 0x5D0 && a.charAt(0) <= 0x5EA) return a;
        return d.name;
    }

    /** המתג כמו שהעורך כותב אותו: העברי (אם יש). */
    static String switchWord(String key) {
        for (String[] m : HE_SWITCHES) if (m[1].equalsIgnoreCase(key)) return m[0];
        return key;
    }

    /** כל הפקודות — בסדר שבו הן מוצגות. */
    static final List<Def> ALL = Collections.unmodifiableList(Arrays.asList(
            transfer("COPY", Words.Def.H_COPY_SRC, true),
            transfer("MOVE", Words.Def.H_MOVE_SRC, false),
            new Def("DEL", Words.Def.N_DEL, new String[]{"ERASE", HE_DEL}, Family.FILE, Words.Def.W_DEL,
                    list(pos(Type.FILES, Words.Def.P_FILES, Words.Def.H_DEL_SRC), filters()), null),
            new Def("REN", Words.Def.N_REN, new String[]{"RENAME", HE_REN}, Family.FILE, Words.Def.W_REN, list(
                    pos(Type.PATH, Words.Def.P_FILE, Words.Def.H_REN_SRC),
                    pos(Type.NAME, Words.Def.P_NEW_NAME, Words.Def.H_NEW_NAME)), null),
            new Def("MD", Words.Def.N_MD, new String[]{"MKDIR"}, Family.FILE, Words.Def.W_MD,
                    list(pos(Type.PATH, Words.Def.P_FOLDER, Words.Def.H_MD), takenNew()), null),
            new Def("RD", Words.Def.N_RD, new String[]{"RMDIR", HE_UNLIST}, Family.FILE, Words.Def.W_RD, list(
                    pos(Type.PATH, Words.Def.P_FOLDER, Words.Def.H_RD),
                    sw("/S", null, Type.FLAG, Words.Def.P_NOT_EMPTY, Words.Def.H_NOT_EMPTY)), null),
            new Def("COMP", Words.Def.N_COMP, new String[]{HE_COMP}, Family.FILE, Words.Def.W_COMP, list(
                    pos(Type.PATH, Words.Def.P_SOURCE, Words.Def.H_COMP_SRC),
                    pos(Type.PATH, Words.Def.P_DEST, Words.Def.H_COMP_DEST),
                    hidden(),
                    sw(Syntax.SW_LIST, null, Type.LIST, Words.Def.P_LIST, Words.Def.H_LIST)),
                    "COMP " + Syntax.SW_LIST + "<" + Words.Def.P_LIST + "> <" + Words.Def.P_DEST + ">"),
            new Def(Syntax.LINK, Words.DbCmd.N_LINK, new String[]{Words.DbCmd.N_LINK}, Family.FILE, Words.DbCmd.W_LINK, list(
                    pos(Type.LIST, Words.Def.P_SOURCE, Words.DbCmd.H_LINK_SRC),
                    pos(Type.LIST, Words.Def.P_DEST, Words.DbCmd.H_LINK_DEST)), null),
            new Def(Syntax.VERIFY, Words.DbCmd.N_VERIFY, new String[]{Words.DbCmd.N_VERIFY}, Family.FILE, Words.DbCmd.W_VERIFY, list(
                    pos(Type.LIST, Words.Def.P_LIST, Words.DbCmd.H_VERIFY),
                    pos(Type.TEXT, Words.DbCmd.P_VERIFY, Words.DbCmd.H_VERIFY_WHAT)), null),
            new Def(Syntax.MODE, Words.DbCmd.N_MODE, new String[]{Words.DbCmd.N_MODE}, Family.FILE, Words.DbCmd.W_MODE,
                    list(pos(Type.NAME, Words.DbCmd.N_MODE, Words.DbCmd.H_MODE)), null),
            new Def(Syntax.CONT, Words.DbCmd.N_CONT, new String[]{Words.DbCmd.N_CONT}, Family.FILE, Words.DbCmd.W_CONT,
                    list(pos(Type.TEXT, Words.DbCmd.P_CONT, Words.DbCmd.H_CONT)), null),
            new Def(Syntax.WRITE, Words.DbCmd.N_WRITE, new String[]{Words.DbCmd.N_WRITE}, Family.FILE, Words.DbCmd.W_WRITE, list(
                    pos(Type.PATH, Words.Def.P_FILE, Words.DbCmd.H_WRITE_FILE),
                    pos(Type.TEXT, Words.DbCmd.P_TEXT_IN, Words.DbCmd.H_WRITE_TEXT),
                    sw(Syntax.SW_APPEND, null, Type.FLAG, Words.DbCmd.P_APPEND, Words.DbCmd.H_APPEND), takenWrite()), null),
            new Def(Syntax.WAIT, Words.DbCmd.N_WAIT, new String[]{Words.DbCmd.N_WAIT}, Family.FILE, Words.DbCmd.W_WAIT,
                    list(pos(Type.TEXT, Words.DbCmd.P_SECONDS, Words.DbCmd.H_SECONDS)), null),
            new Def(Syntax.BACKUP, Words.DbCmd.N_BACKUP, new String[]{Words.DbCmd.N_BACKUP}, Family.FILE, Words.DbCmd.W_BACKUP, new Param[0], null),
            new Def(Syntax.REFRESH, Words.DbCmd.N_REFRESH, new String[]{Words.DbCmd.N_REFRESH}, Family.FILE, Words.DbCmd.W_REFRESH,
                    list(pos(Type.TEXT, Words.DbCmd.P_DEEP, Words.DbCmd.H_DEEP)), null),
            new Def(Syntax.REPORT, Words.DbCmd.N_REPORT, new String[]{Words.DbCmd.N_REPORT}, Family.FILE, Words.DbCmd.W_REPORT,
                    list(pos(Type.PATH, Words.Def.P_FILE, Words.DbCmd.H_REPORT)), null),
            new Def(Syntax.CALL, Words.DbCmd.N_CALL, new String[]{Words.DbCmd.N_CALL}, Family.FILE, Words.DbCmd.W_CALL,
                    list(pos(Type.PATH, Words.Def.P_FILE, Words.DbCmd.H_CALL)), null),
            new Def("START", Words.Def.N_START, new String[]{HE_START}, Family.FILE, Words.Def.W_START,
                    list(pos(Type.PATH, Words.Def.P_WHAT_OPEN, Words.Def.H_START, Words.Def.U_START), hidden()), null),   // מ0.40: /נסתרים — עם תבנית
            new Def("ECHO", Words.Def.N_ECHO, new String[]{HE_ECHO}, Family.FILE, Words.Def.W_ECHO, list(pos(Type.TEXT, Words.Def.P_TEXT, Words.Def.H_TEXT)), null),
            new Def("REM", Words.Def.N_REM, new String[]{HE_REM}, Family.FILE, Words.Def.W_REM, list(pos(Type.TEXT, Words.Def.P_TEXT, Words.Def.H_TEXT)), null),
            new Def(Syntax.SECTION, Words.DbCmd.N_SECTION, new String[]{Words.DbCmd.N_SECTION}, Family.FILE, Words.DbCmd.W_SECTION,   // מ0.44
                    list(pos(Type.TEXT, Words.Def.P_TEXT, Words.DbCmd.H_SECTION)), null),
            new Def(Syntax.DROP_MODE, Words.DbCmd.N_DROP_MODE, null, Family.FILE, Words.DbCmd.W_DROP_MODE,   // מ0.45
                    list(pos(Type.NAME, Words.DbCmd.N_MODE, Words.DbCmd.H_DROP_MODE)), null),
            new Def(Syntax.UNTRASH, Words.DbCmd.N_UNTRASH, null, Family.FILE, Words.DbCmd.W_UNTRASH,
                    list(pos(Type.PATH, Words.Def.P_FILE, Words.DbCmd.H_UNTRASH)), null),
            new Def(Syntax.PURGE, Words.DbCmd.N_PURGE, null, Family.FILE, Words.DbCmd.W_PURGE,
                    list(pos(Type.PATH, Words.Def.P_FILE, Words.DbCmd.H_PURGE)), null),
            new Def(Syntax.EXAMPLES, Words.Cmd.N_EXAMPLES, null, Family.FILE, Words.Cmd.W_EXAMPLES, new Param[0], null),   // מבנה 123
            new Def(Syntax.SET, Words.Cmd.N_SET, new String[]{Words.Cmd.N_SET}, Family.FILE, Words.Cmd.W_SET,   // מבנה 127
                    list(pos(Type.TEXT, Words.Cmd.P_SET_KEY, Words.Cmd.H_SET_KEY), pos(Type.TEXT, Words.Cmd.P_SET_VALUE, Words.Cmd.H_SET_VALUE)), null),
            new Def(Syntax.PACK, Words.Cmd.N_PACK, new String[]{Words.Cmd.N_PACK}, Family.FILE, Words.Cmd.W_PACK,
                    list(pos(Type.PATH, Words.Def.P_FILE, Words.Cmd.H_PACK)), null),
            new Def(Syntax.UNPACK, Words.Cmd.N_UNPACK, new String[]{Words.Cmd.N_UNPACK}, Family.FILE, Words.Cmd.W_UNPACK,
                    list(pos(Type.PATH, Words.Def.P_FILE, Words.Cmd.H_UNPACK)), null),
            verb(Verbs.OPEN, Words.Def.W_OPEN),
            verb(Verbs.CREATE, Words.Def.W_CREATE),
            verb(Verbs.UPDATE, Words.Def.W_UPDATE),
            verb(Verbs.SHAPE, Words.Def.W_SHAPE),
            verb(Verbs.FIND, Words.Def.W_FIND),
            verb(Verbs.BACK, Words.Def.W_BACK),
            verb(Verbs.RUN, Words.Def.W_RUN),
            verb(Verbs.ADD, Words.Def.W_ADD),
            new Def(Syntax.AS_IF2, Syntax.AS_IF2, new String[]{Syntax.AS_IF}, Family.HEADER, Words.Def.W_AS_IF, new Param[0], null),
            Defs.MEASURE, Defs.CHECK, Defs.LOOP, Defs.END));

    /** 1.25.0: מדידה ולולאה — שורות מבנה (לא פקודה עם פרמטרים): בעורך — הסבר, והעריכה בשורה עצמה. */
    static final class Defs {
        static final Def MEASURE = new Def(Syntax.MEASURE, Words.Loop.N_MEASURE, null, Family.HEADER, Words.Loop.WHAT_MEASURE, new Param[0], null);
        static final Def CHECK = new Def(Syntax.CHECK, Words.Loop.N_CHECK, null, Family.HEADER, Words.Loop.WHAT_CHECK, new Param[0], null);
        static final Def LOOP = new Def(Syntax.LOOP + " 3", Words.Loop.N_LOOP, null, Family.HEADER, Words.Loop.WHAT_LOOP, new Param[0], null);
        static final Def END = new Def(Syntax.LOOP_END, Words.Loop.N_END, null, Family.HEADER, Words.Loop.WHAT_END, new Param[0], null);

        private Defs() { }
    }

    // ---------- שאילתות ----------

    /** איך כותבים את הפקודה — נבנה מההגדרה האחת (1.22.5: עם המתגים עצמם). מבנה 24: היה במילון (Words.Cmd.usage), והמילון פנה בשבילו לכאן. */
    static String usage(String cmd) {
        Def d = get(cmd);
        return d == null ? "" : d.usage();
    }

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

    private static String join(List<String> l, String sep) {
        StringBuilder sb = new StringBuilder();
        for (String s : l) sb.append(sb.length() > 0 ? sep : "").append(s);
        return sb.toString();
    }
}
