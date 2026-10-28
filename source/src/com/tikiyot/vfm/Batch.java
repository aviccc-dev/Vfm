package com.tikiyot.vfm;

// קובצי פקודות בסגנון bat (117א, שלב 1 — המנוע). קורא את הקובץ, מתכנן (תצוגה מקדימה: מה ייעשה, בלי לשנות
// דבר), ומבצע — כל שינוי בכונן דרך Physical (אישור, סל המחזור, רשימות עוקבות, אינדקס).
// פקודות: REM ECHO MD/MKDIR RD/RMDIR DEL/ERASE COPY MOVE REN/RENAME START (ו-"@ECHO OFF", "::" כהערה).
// מתגים: /תאריך: /גודל: /תפוס: · ROBOCOPY: /MAXAGE: /MINAGE: /MAX: /MIN: · XCOPY: /D: · COPY: /Y /-Y · RD: /S /Q · DEL: /Q /F
// · /A:H — גם קבצים מוסתרים (בלי — מדלגים עליהם, כמו ב-bat). %~dp0 — כמו %כאן%.
// משתנים קבועים (לקריאה בלבד): %פנימי% %כרטיס% %הורדות% %כאן%. מסלול יחסי — ביחס לתיקייה של הקובץ.
// אסור לו: לשנות משהו בכונן בעצמו (רק דרך Physical), או לפעול בלי תצוגה מקדימה ואישור.

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

final class Batch {

    static final String EXT = "vbat";

    static boolean isBatch(String name) {
        String n = name.toLowerCase(java.util.Locale.ROOT);
        return n.endsWith("." + EXT);
    }

    /** שורה מתוכננת: מה תעשה, על אילו קבצים, ומה נכשל כבר בתכנון. */
    static final class Step {
        int line;
        String text, cmd, error;
        final List<String> files = new ArrayList<String>();
        final List<String> dirs = new ArrayList<String>();
        File dest;
        String newName;
        /** 1.13.0: פקודת פתיחה — השורה, אחרי המשתנים. */
        String open;
        boolean replace, skipTaken, recursive, hidden;
        /** 1.19.0: COPY/MOVE /S — שמירת מסלול: השורש שממנו נמדד המסלול; files — כל הקבצים, dirs — תיקיות ריקות. */
        String keepRoot;
        /** 1.19.0: /יומן: — קובץ היומן (מסלול מלא), או null. */
        String journal;
        /** 1.20.0: מקור מתוך השוואה (%רק-במקור% / %רק-ביעד%) — בהעברה לא מוחקים תיקיות שהתרוקנו (הן קיימות בצד השני). */
        boolean fromCompare;
        /** 1.20.0: /תפוס:לעצור — שם תפוס נרשם ככישלון, בלי לשאול. */
        boolean failTaken;
        /** 1.21.0: COPY … /מצב:<זמן> — שחזור: לכל קובץ — תיקיית היעד שלו (המסלול הלוגי). */
        final List<String> targets = new ArrayList<String>();
        long stateWhen = Long.MIN_VALUE;
        /** 1.22.0: COMP /רשימה:<שם> — המקור הוא רשימה ידנית (עם כל מה שבתוכה). */
        String listName;
        /** 203: COPY … %שיתוף% — נאסף לשליחה אחת בסוף. */
        boolean share;
        /** MD לתיקייה שכבר קיימת — בלי שינוי (ולא שגיאה). */
        boolean noop;
        /** 1.24.0: שורה על רשימות (ListCmd) — מה שיתבצע בהרצה. */
        ListCmd.Op op;
        /** 1.25.0: בתוך לולאה — הלולאות שסביב השורה (מזהים) והסבב בכל אחת; null — מחוץ ללולאה. */
        int[] loops, rounds;
        /** 1.25.0: מדידה — כמה זמן לקחה (אלפיות שנייה). */
        long ms = -1;
        /** 1.26.0: בוצעה בהרצה (לא דולגה, לא נעצרה לפניה) — לסיכום. */
        boolean ran;
        /** 1.26.0: על מה השורה פועלת (רשימה / תיקייה) — לסיכום; null — מתוך describe. */
        String target;

        /** "12" · בלולאה — "12 (סבב 3)". */
        String label() {
            return rounds == null ? String.valueOf(line) : line + Words.Loop.round(rounds[rounds.length - 1]);
        }
        int total;              // לפני הסינון
        String describe;
    }

    /** התוכנית כולה. */
    static final class Plan {
        final List<Step> steps = new ArrayList<Step>();
        final List<String> echoes = new ArrayList<String>();
        File start;
        int errors;
        /** 203: מה שהוכן לשליחה (עותקים ואריזות בתיקיית השליחה) — נשלח פעם אחת, בסוף. */
        final List<File> shared = new ArrayList<File>();
        File stage;
        /** 1.10.0: "בכאילו" בקובץ — שום דבר לא מתבצע; רק ההדמיה (sim) מוצגת כרשימה זמנית. */
        boolean asIf;
        /** 1.10.0: המצב הווירטואלי בסוף התכנון — מה יהיה איפה (לבכאילו). */
        Sim sim;
        /** 1.13.0: פקודת פתיחה בעברית (פתח / מצא / חזור…) — נפתחת בסוף ההרצה, כמו START; null — אין. */
        String open;
        /** 216: משהו בהרצה לא הלך חלק (שורה שדולגה, כשל, עצירה) — אז הדוח מוצג; אחרת — הודעה קצרה בלבד. */
        boolean trouble;
        /** 1.25.0: שורת "מדידה" — זמן לכל שורה, סיכום, ויומן (אם נכתב /יומן:). */
        boolean measure;
        String measureJournal;
        /** 1.25.0: הלולאות — לכל מזהה: הגבול לסבב (אלפיות שנייה; 0 — בלי) והשורה של "חזור". */
        final List<Long> loopLimit = new ArrayList<Long>();
        final List<Integer> loopLine = new ArrayList<Integer>();
        /** 1.25.4: המשתמש עצר בזמן ההכנה — לא מריצים. */
        boolean stopped;
        /** 1.25.0: לולאה שנעצרה בתכנון (הסבב עבר את הגבול) — מוצג בתצוגה המקדימה. */
        final List<String> notes = new ArrayList<String>();

        int changes() {
            int n = 0;
            for (Step s : steps) if (s.error == null && !s.noop && !s.cmd.equals("ECHO") && !s.cmd.equals("START") && !s.cmd.equals(OPEN)
                    && !s.cmd.equals("COMP")) n++;
            return n;
        }
    }

    private Batch() { }

    // ---------- קריאה ----------

    /** מפצל שורה לחלקים כמו cmd: רווחים מפרידים, ומרכאות שומרות רווחים (המרכאות עצמן יורדות). */
    static List<String> tokens(String line) {
        List<String> out = new ArrayList<String>();
        StringBuilder cur = new StringBuilder();
        boolean q = false, any = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                q = !q;
                any = true;
            } else if (!q && Character.isWhitespace(c)) {
                if (any || cur.length() > 0) out.add(cur.toString());
                cur.setLength(0);
                any = false;
            } else {
                cur.append(c);
            }
        }
        if (any || cur.length() > 0) out.add(cur.toString());
        return out;
    }

    // ---------- התחביר בעברית — במקום אחד (131). גם BatchLine והעורך לוקחים מכאן. ----------
    /** משתנים: %פנימי% %כרטיס% %הורדות% %כאן%. */
    static final String V_INTERNAL = Sources.INTERNAL, V_CARD = Sources.CARD, V_DOWNLOADS = "הורדות", V_HERE = "כאן";   // תחביר
    /**
     * 203: "%שיתוף%" — יעד שאינו תיקייה: מה שמועתק אליו נשלח למכשיר אחר בסוף (חלון תוכנות ההעברה, או היעד הקבוע).
     * המסלול שהוא מקבל — סימן שלא ייתכן בכונן.
     */
    static final String V_SHARE = "שיתוף", SHARE_MARK = "/\u0000share";   // תחביר
    /** 1.10.0: שורה "בכאילו" — הקובץ רץ בהדמיה בלבד (המשתמש: "פקודה כללית בכל קובץ פקודות שתאמר שהיא רצה רק בכאילו"). */
    static final String AS_IF = "בכאילו";   // תחביר
    /** 1.13.0: "הדמיה" — שם נרדף לבכאילו, רשמי יותר (המשתמש: "זו מילה נרדפת לבכאילו, וגם יותר רשמית, פחות סלנג"). */
    static final String AS_IF2 = "הדמיה";   // תחביר
    /** 1.25.0: "מדידה" — שורת כותרת: זמן לכל שורה (claude/pkudot-reshimot.md). לולאה: "חזור N [/עד:<זמן>]" … "סוף". */
    static final String MEASURE = "מדידה", LOOP = "חזור", LOOP_END = "סוף", V_ROUND = "סבב";   // תחביר
    /** הגבול לסבבים בלולאה, ולשורות אחרי הפריסה. */
    static final int MAX_ROUNDS = 1000, MAX_LINES = 50000;
    /** 1.13.0: שלב של פקודת פתיחה בעברית (Verbs). */
    static final String OPEN = "OPEN";

    /** 1.10.0: שורה אחת בתוצאת ההדמיה. */
    static final class AsIfRow {
        final String icon, title, sub;

        AsIfRow(String icon, String title, String sub) {
            this.icon = icon;
            this.title = title;
            this.sub = sub;
        }
    }

    /**
     * 1.10.0: תוצאת ההדמיה — המצב בסוף, אחרי כל השורות: מה יופיע ואיפה (ומאיפה בא), תיקיות חדשות, מה ייצא לסל,
     * מה יישלח, מה ייפתח בסוף, ומה לא יתבצע. שום דבר לא נוגע בכונן.
     */
    static List<AsIfRow> asIfRows(Plan plan) {
        List<AsIfRow> out = new ArrayList<AsIfRow>();
        for (String e : plan.echoes) out.add(new AsIfRow("💬", e, Words.AsIf.ECHO));
        for (Step s : plan.steps)
            if (s.error != null) out.add(new AsIfRow("⚠", s.error, s.line + ". " + s.text));
        java.util.Set<String> sources = new java.util.HashSet<String>();
        for (File f : plan.sim.added.values()) sources.add(f.getAbsolutePath());
        java.util.Set<String> movedDirs = new java.util.HashSet<String>();
        for (Step s : plan.steps)
            if (s.error == null && s.cmd.equals("MOVE")) movedDirs.addAll(s.dirs);
        for (String d : new java.util.TreeSet<String>(plan.sim.dirs))
            out.add(new AsIfRow("📁", new File(d).getName(), Words.AsIf.NEW_FOLDER + Bidi.path(d)));
        for (Map.Entry<String, File> e : plan.sim.added.entrySet()) {
            String from = e.getValue().getAbsolutePath();
            out.add(new AsIfRow("📄", new File(e.getKey()).getName(),
                    Words.AsIf.WILL_BE + Bidi.path(new File(e.getKey()).getParent())
                            + (from.equals(e.getKey()) ? "" : Words.AsIf.FROM + Bidi.path(from))));
        }
        for (String g : new java.util.TreeSet<String>(plan.sim.gone))
            if (!sources.contains(g) && !movedDirs.contains(g))
                out.add(new AsIfRow("🗑", new File(g).getName(), Words.AsIf.TO_TRASH + Bidi.path(g)));
        for (Step s : plan.steps)                                               // 1.24.0: רשימות
            if (s.error == null && s.op != null) out.add(new AsIfRow("🗂", s.describe, s.line + ". " + s.text));
        for (Step s : plan.steps)
            if (s.error == null && s.share)
                out.add(new AsIfRow("📤", Words.AsIf.SHARE, s.describe));
        if (plan.open != null) out.add(new AsIfRow("▶", Verbs.label(Verbs.parse(plan.open)), Words.AsIf.AT_END));
        if (plan.start != null)
            out.add(new AsIfRow("▶", isHome(plan.start) ? Words.Cmd.HOME_AT_END : plan.start.getName(), Words.AsIf.AT_END));
        if (out.isEmpty()) out.add(new AsIfRow("·", Words.AsIf.NOTHING, ""));
        return out;
    }

    /** 1.9.0: "%בית%" — רק אחרי START: בסוף ההרצה חוזרים למסך הבית. */
    static final String V_HOME = "בית", HOME_MARK = "/\u0000home";   // תחביר

    /** START %בית% — היעד שבסוף הוא מסך הבית. */
    static boolean isHome(File f) {
        return f != null && HOME_MARK.equals(f.getPath());
    }
    /** מתגי סינון: /תאריך:… /גודל:… */
    static final String SW_DATE = DateSize.DATE, SW_SIZE = DateSize.SIZE;
    /** שם תפוס: /תפוס:לשאול · /תפוס:לדלג · /תפוס:להחליף */
    static final String SW_TAKEN = "/תפוס:", TAKEN_ASK = "לשאול", TAKEN_SKIP = "לדלג", TAKEN_REPLACE = "להחליף";   // תחביר
    /** 1.19.0: שמירת מסלול — /S (כמו XCOPY), ובעברית /מסלול. יומן — /יומן:<קובץ>. */
    static final String SW_KEEP = "/מסלול", SW_JOURNAL = "/יומן:";   // תחביר
    /** 1.19.0: %תאריך% — זמן ההרצה (קבוע לכל הקובץ); %מזהה-כרטיס% — שם הכרטיס הקבוע (למשל ABCD-1234). */
    static final String V_NOW = "תאריך", V_CARD_ID = "מזהה-כרטיס";   // תחביר
    /** 1.20.0: תוצאת COMP האחרונה — מקור לשורות COPY/MOVE שאחריה (תמיד עם שמירת מסלול). /עד:N% — סף. */
    static final String V_ONLY_A = "רק-במקור", V_ONLY_B = "רק-ביעד", ONLY_A_MARK = "/\u0000onlyA", ONLY_B_MARK = "/\u0000onlyB";   // תחביר
    static final String SW_LIMIT = "/עד:", TAKEN_STOP = "לעצור";   // תחביר
    /** 1.21.0: COPY <גיבוי[\\תת־תיקייה][\\תבנית]> <יעד> /מצב:<זמן> — שחזור המצב של אותו זמן. */
    static final String SW_STATE = "/מצב:";   // תחביר
    /** 1.22.0: COMP /רשימה:"<רשימה>" <יעד> — המקור הוא רשימה ידנית (קבצים, תתי־רשימות, חכמות, תיקיות מתעדכנות). */
    static final String SW_LIST = "/רשימה:";   // תחביר
    static final String NOW_FORMAT = "dd.MM.yyyy HH-mm-ss";   // בכונן

    static String var(String name) {
        return "%" + name + "%";
    }

    /** מתג: מתחיל ב-"/" ושמו מוכר (כך "/storage/..." אינו מתג). */
    private static final Pattern SWITCH = Pattern.compile(
            "^/(-?Y|S|Q|F|A(:.*)?|D(:.*)?|" + SW_DATE + ".*|" + SW_SIZE + ".*|" + SW_TAKEN.substring(1)
                    + ".*|" + SW_KEEP.substring(1) + "|" + SW_JOURNAL.substring(1) + ".*|" + SW_LIMIT.substring(1)
                    + ".*|" + SW_STATE.substring(1) + ".*|" + SW_LIST.substring(1) + ".*|MAXAGE:.*|MINAGE:.*|MAX:.*|MIN:.*)$",
            Pattern.CASE_INSENSITIVE);

    static boolean isSwitch(String t) {
        return SWITCH.matcher(t).matches();
    }

    /** משתנים קבועים. */
    static Map<String, String> vars(File here, String internal, String card) {
        Map<String, String> v = new LinkedHashMap<String, String>();
        v.put(V_INTERNAL, internal);
        if (card != null) v.put(V_CARD, card);
        v.put(V_DOWNLOADS, internal + "/Download");
        v.put(V_HERE, here.getAbsolutePath());
        if (card != null) v.put(V_CARD_ID, new File(card).getName());       // 1.19.0
        v.put(V_NOW, new java.text.SimpleDateFormat(NOW_FORMAT, Locale.ROOT).format(new java.util.Date()));   // 1.19.0
        v.put(V_ONLY_A, ONLY_A_MARK);                                         // 1.20.0
        v.put(V_ONLY_B, ONLY_B_MARK);
        v.put(V_SHARE, SHARE_MARK);                                           // 203
        v.put(V_HOME, HOME_MARK);                                             // 1.9.0
        return v;
    }

    /** מחליף משתנים, "\\" ב-"/", ומסלול יחסי — ביחס לתיקייה של הקובץ. null = משתנה לא מוכר. */
    static String path(String raw, File here, Map<String, String> vars, String[] error) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < raw.length()) {
            char c = raw.charAt(i);
            if (raw.startsWith("%~dp0", i)) {                          // 139: התיקייה של הקובץ, כמו ב-bat
                String h = vars.get(V_HERE);
                sb.append(h).append('/');
                i += 5;
                if (i < raw.length() && (raw.charAt(i) == '\\' || raw.charAt(i) == '/')) i++;
                continue;
            }
            if (c == '%') {
                int j = raw.indexOf('%', i + 1);
                if (j < 0) {
                    error[0] = Words.Cmd.MISSING_PERCENT;
                    return null;
                }
                String name = raw.substring(i + 1, j);
                String val = vars.get(name);
                if (val == null) {
                    error[0] = Words.Cmd.unknownVar(name);
                    return null;
                }
                sb.append(val);
                i = j + 1;
            } else {
                sb.append(c == '\\' ? '/' : c);
                i++;
            }
        }
        String p = sb.toString();
        while (p.length() > 1 && p.endsWith("/")) p = p.substring(0, p.length() - 1);
        if (!p.startsWith("/")) p = new File(here, p).getAbsolutePath();
        return p;
    }

    /** תבנית שם בסגנון bat: * ו-? (בלי הבדל בין גדולות לקטנות). */
    static boolean wild(String pattern, String name) {
        StringBuilder re = new StringBuilder();
        for (char c : pattern.toCharArray()) {
            if (c == '*') re.append(".*");
            else if (c == '?') re.append('.');
            else re.append(Pattern.quote(String.valueOf(c)));
        }
        String p = re.toString();
        if (pattern.equals("*.*")) p = ".*";                   // כמו ב-bat: *.* — כל הקבצים, גם בלי סיומת
        return Pattern.compile(p, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(name).matches();
    }

    // ---------- סינון (תאריך וגודל) — אותם תנאים של החיפוש ----------

    static final class Filter {
        final List<Cond.Test> tests = new ArrayList<Cond.Test>();
        String error;

        boolean ok(File f) {
            if (tests.isEmpty()) return true;
            Index.Rec r = new Index.Rec();
            r.name = f.getName();
            r.size = f.length();
            r.mtime = f.lastModified();
            for (Cond.Test t : tests) if (!t.ok(null, r)) return false;
            return true;
        }
    }

    static Filter filter(List<String> switches) {
        Filter f = new Filter();
        for (String sw : switches) {
            String s = sw.substring(1);
            String up = s.toUpperCase(Locale.ROOT);
            Cond.Test t = null;
            boolean known = true;
            if (s.startsWith(SW_DATE)) t = Cond.date(s.substring(SW_DATE.length()));
            else if (s.startsWith(SW_SIZE)) t = Cond.size(s.substring(SW_SIZE.length()));
            else if (up.startsWith("MAXAGE:")) t = Cond.date(s.substring(7).trim() + "d");
            else if (up.startsWith("MINAGE:")) t = Cond.date("<" + s.substring(7).trim() + "d");
            else if (up.startsWith("MAX:")) t = Cond.size("<" + (parseLong(s.substring(4)) + 1));
            else if (up.startsWith("MIN:")) t = Cond.size(">" + (parseLong(s.substring(4)) - 1));
            else if (up.startsWith("D:")) t = Cond.date(">=" + s.substring(2));
            else known = false;
            if (known && t == null) {
                f.error = Words.Cmd.switchUnknown(sw);
                return f;
            }
            if (t != null) f.tests.add(t);
        }
        return f;
    }

    private static long parseLong(String s) {
        try {
            return Long.parseLong(s.trim());
        } catch (Throwable t) {
            return Long.MIN_VALUE / 2;
        }
    }

    // ---------- תכנון ----------

    /** מתכנן את כל הקובץ, בלי לשנות דבר. */
    /**
     * מה שהשורות הקודמות ייצרו או יזיזו — כדי שהתכנון יכיר אותן לפני שבוצעו (118): תיקייה מ-MD,
     * קובץ שהועבר או הועתק ליעד, שם חדש מ-REN, ומה שנמחק או הועבר ממקומו.
     */
    static final class Sim {
        final java.util.Set<String> dirs = new java.util.HashSet<String>();
        final java.util.Set<String> gone = new java.util.HashSet<String>();
        /** קובץ שיהיה ← הקובץ שממנו הוא בא (לגודל ולתאריך). */
        final Map<String, File> added = new LinkedHashMap<String, File>();
        /** 1.20.0: ההשוואה האחרונה (COMP), והאינדקס לחתימות שכבר חושבו (יכול להיות null). */
        Compare.Result compare;
        Library lib;
        /** 1.24.0: הרשימות בזיכרון — מה יהיה בהן אחרי השורות שלפני (ListCmd); null — עוד לא נטען. לא נשמר. */
        Tree tree;
        /** 1.25.4: הקבצים של רשימות חכמות — חיפוש אחד לכל תנאי, לכל הקובץ (גם בעותקים שלהן). */
        ListCmd.Lenient files;

        boolean isDir(File d) {
            String p = d.getAbsolutePath();
            return !gone.contains(p) && (d.isDirectory() || dirs.contains(p));
        }

        boolean isFile(File f) {
            String p = f.getAbsolutePath();
            return added.containsKey(p) || (!gone.contains(p) && f.isFile());
        }

        /** המקור האמיתי (לגודל ולתאריך). */
        File real(File f) {
            File r = added.get(f.getAbsolutePath());
            return r != null ? r : f;
        }

        /** הקבצים שיהיו בתיקייה, ממוינים. */
        List<File> files(File dir) {
            java.util.TreeMap<String, File> out = new java.util.TreeMap<String, File>();
            File[] all = dir.listFiles();
            if (all != null)
                for (File x : all)
                    if (x.isFile() && !gone.contains(x.getAbsolutePath())) out.put(x.getName(), x);
            String d = dir.getAbsolutePath() + "/";
            for (String p : added.keySet())
                if (p.startsWith(d) && p.indexOf('/', d.length()) < 0) out.put(p.substring(d.length()), new File(p));
            return new ArrayList<File>(out.values());
        }

        void move(String from, File to, boolean copy) {
            File src = real(new File(from));
            if (!copy) {
                gone.add(from);
                added.remove(from);
            }
            gone.remove(to.getAbsolutePath());
            added.put(to.getAbsolutePath(), src);
        }
    }

    /** מתכנן את כל הקובץ, בלי לשנות דבר. כל שורה מכירה את מה שהשורות שלפניה יעשו (118). */
    static Plan plan(String text, File here, Map<String, String> vars) {
        return plan(text, here, vars, null);
    }

    // ---------- 1.25.0: לולאה ----------

    /** שורה אחרי פריסת הלולאות: הטקסט (עם %סבב%), המספר בקובץ, והלולאות שסביבה. */
    static final class Ln {
        String text, error;
        int num;
        int[] loops, rounds;
    }

    /** "חזור 6" · "חזור <סבבים>" — לולאה (בלי מספר — הפועל "חזור", חזרה למסך הקודם). */
    static boolean isLoopStart(String line) {
        List<String> tk = tokens(line);
        if (tk.size() < 2 || !tk.get(0).equals(LOOP)) return false;
        char c = tk.get(1).isEmpty() ? ' ' : tk.get(1).charAt(0);
        return Character.isDigit(c) || c == '<' || c == '%';
    }

    static boolean isLoopEnd(String line) {
        return line.equals(LOOP_END);
    }

    private static String clean(String raw) {
        String line = raw.trim();
        if (line.startsWith("@")) line = line.substring(1).trim();
        return line;
    }

    /** הגבול לסבב: "30שניות", "30 שניות", "2דקות", "500ms"; -1 — לא מובן. */
    static long limitMs(String v) {
        String t = v.replace(" ", "").toLowerCase(Locale.ROOT);
        int i = 0;
        while (i < t.length() && (Character.isDigit(t.charAt(i)) || t.charAt(i) == '.')) i++;
        if (i == 0) return -1;
        double n;
        try { n = Double.parseDouble(t.substring(0, i)); } catch (NumberFormatException e) { return -1; }
        String u = t.substring(i);
        if (u.isEmpty() || u.startsWith("שנ") || u.equals("ש") || u.equals("s") || u.equals("sec")) return (long) (n * 1000);   // תחביר
        if (u.startsWith("דק") || u.equals("m") || u.equals("min")) return (long) (n * 60000);                                  // תחביר
        if (u.equals("ms") || u.startsWith("אלפ")) return (long) n;                                                               // תחביר
        return -1;
    }

    /** פורס את הלולאות: כל סבב — השורות שבתוכה, עם %סבב% = מספר הסבב (בלולאה פנימית — שלה). */
    static List<Ln> expand(String[] lines, Plan plan) {
        List<Ln> out = new ArrayList<Ln>();
        expand(lines, 0, lines.length, new int[0], new int[0], null, plan, out);
        return out;
    }

    private static void expand(String[] lines, int from, int to, int[] loops, int[] rounds, String round, Plan plan, List<Ln> out) {
        for (int i = from; i < to; i++) {
            if (out.size() > MAX_LINES) return;
            String line = clean(lines[i]);
            if (isLoopStart(line)) {
                int depth = 0, end = -1;
                for (int j = i + 1; j < to && end < 0; j++) {
                    String x = clean(lines[j]);
                    if (isLoopStart(x)) depth++;
                    else if (isLoopEnd(x)) {
                        if (depth == 0) end = j;
                        else depth--;
                    }
                }
                List<String> tk = tokens(round == null ? line : line.replace(var(V_ROUND), round));
                int n = -1;
                long limit = 0;
                String err = null;
                try { n = Integer.parseInt(tk.get(1)); } catch (NumberFormatException e) { err = Words.Loop.NEED_NUMBER; }
                for (int k = 2; k < tk.size() && err == null; k++) {
                    String x = tk.get(k);
                    if (x.equals(Words.Loop.TIMES_WORD)) continue;
                    if (x.startsWith(SW_LIMIT)) {
                        String v = x.substring(SW_LIMIT.length());
                        if (k + 1 < tk.size() && !tk.get(k + 1).isEmpty() && !Character.isDigit(tk.get(k + 1).charAt(0)) && !tk.get(k + 1).startsWith("/")
                                && limitMs(v + tk.get(k + 1)) > 0) v += tk.get(++k);                 // "/עד:30 שניות"
                        limit = limitMs(v);
                        if (limit <= 0) err = Words.Loop.BAD_LIMIT;
                    } else err = Words.Loop.USAGE;
                }
                if (err == null && (n < 1 || n > MAX_ROUNDS)) err = Words.Loop.tooMany(MAX_ROUNDS);
                if (err == null && end < 0) err = Words.Loop.NO_END;
                if (err != null) {
                    Ln e = new Ln();
                    e.num = i + 1;
                    e.text = line;
                    e.error = err;
                    out.add(e);
                    if (end < 0) continue;
                    i = end;
                    continue;
                }
                int id = plan.loopLimit.size();
                plan.loopLimit.add(limit);
                plan.loopLine.add(i + 1);
                int[] l2 = java.util.Arrays.copyOf(loops, loops.length + 1), r2 = java.util.Arrays.copyOf(rounds, rounds.length + 1);
                l2[loops.length] = id;
                for (int r = 1; r <= n && out.size() <= MAX_LINES; r++) {
                    r2 = r2.clone();
                    r2[rounds.length] = r;
                    expand(lines, i + 1, end, l2, r2, String.valueOf(r), plan, out);
                }
                i = end;
                continue;
            }
            Ln x = new Ln();
            x.num = i + 1;
            x.text = round == null ? lines[i] : lines[i].replace(var(V_ROUND), round);
            if (isLoopEnd(line)) x.error = Words.Loop.END_WITHOUT_START;
            if (loops.length > 0) {
                x.loops = loops;
                x.rounds = rounds;
            }
            out.add(x);
        }
        if (out.size() > MAX_LINES && (out.isEmpty() || out.get(out.size() - 1).error == null)) {
            Ln e = new Ln();
            e.num = from + 1;
            e.text = "";
            e.error = Words.Loop.tooLong(MAX_LINES);
            out.add(e);
        }
    }

    /**
     * עצירה לפי זמן: מעבר לסבב חדש בלולאה עם /עד: — אם הסבב הקודם עבר את הגבול, הלולאה נעצרת. מחזיר את מזהה
     * הלולאה שנעצרה כעת (כדי לכתוב פעם אחת), או -1.
     */
    static final class Clock {
        final Map<Integer, Integer> round = new java.util.HashMap<Integer, Integer>();
        final Map<Integer, Long> start = new java.util.HashMap<Integer, Long>();
        final java.util.Set<Integer> stopped = new java.util.HashSet<Integer>();
        final List<String> notes = new ArrayList<String>();

        /** true — השורה מדולגת (לולאה שסביבה נעצרה). */
        boolean skip(Plan plan, int[] loops, int[] rounds) {
            if (loops == null) return false;
            long now = System.nanoTime();
            for (int k = 0; k < loops.length; k++) {
                int id = loops[k];
                if (stopped.contains(id)) return true;
                Integer cur = round.get(id);
                if (cur == null || cur != rounds[k]) {
                    if (cur != null) {
                        long took = (now - start.get(id)) / 1000000;
                        long lim = plan.loopLimit.get(id);
                        if (lim > 0 && took > lim) {
                            stopped.add(id);
                            notes.add(Words.Loop.stopped(plan.loopLine.get(id), cur, took));
                            return true;
                        }
                    }
                    round.put(id, rounds[k]);
                    start.put(id, now);
                    for (int m = k + 1; m < loops.length; m++) {          // לולאות פנימיות — מתחילות מחדש
                        round.remove(loops[m]);
                        stopped.remove(loops[m]);
                    }
                }
            }
            return false;
        }
    }

    /** 1.20.0: עם האינדקס — ההשוואה לוקחת ממנו חתימות שכבר חושבו. */
    static Plan plan(String text, File here, Map<String, String> vars, Library lib) {
        return plan(text, here, vars, lib, null);
    }

    /** 1.25.4: עם עצירה — ההכנה עצמה יכולה לקחת זמן (לולאות, השוואות של רשימות גדולות). */
    static Plan plan(String text, File here, Map<String, String> vars, Library lib, Physical.Progress progress) {
        Plan plan = new Plan();
        Sim sim = new Sim();
        sim.lib = lib;
        plan.sim = sim;
        String[] lines = Bidi.strip(text).replace("\r", "").split("\n");          // 204
        List<Ln> all = expand(lines, plan);                                    // 1.25.0: לולאות
        Clock clock = new Clock();
        int mark = 0;
        Ln prev = null;
        long t0 = System.nanoTime();
        for (int li = 0; li <= all.size(); li++) {
            for (int k = mark; k < plan.steps.size(); k++) {                   // 1.25.0: השורות שנוספו — מהשורה הקודמת
                Step x = plan.steps.get(k);
                x.loops = prev.loops;
                x.rounds = prev.rounds;
                if (plan.measure) x.ms = (System.nanoTime() - t0) / 1000000;
            }
            mark = plan.steps.size();
            t0 = System.nanoTime();
            if (li == all.size()) break;
            if (progress != null && progress.stopped()) {                      // 1.25.4: עצירה בהכנה
                plan.stopped = true;
                break;
            }
            if (progress != null) progress.step(li + 1, all.size(), 0, 0);    // 1.26.0: "בודק שורה X מתוך Y"
            Ln ln = all.get(li);
            prev = ln;
            if (clock.skip(plan, ln.loops, ln.rounds)) continue;               // לולאה שעברה את הגבול — גם בתכנון
            int n = ln.num - 1;
            String line = clean(ln.text);
            if (ln.error != null) {
                Step s = new Step();
                s.line = ln.num;
                s.text = line;
                s.cmd = LOOP;
                s.error = ln.error;
                plan.errors++;
                plan.steps.add(s);
                continue;
            }
            if (line.isEmpty() || line.startsWith("::")) continue;
            if (line.equals(AS_IF) || line.equals(AS_IF2)) {                  // 1.10.0: שורת כותרת — כל הקובץ בהדמיה
                plan.asIf = true;
                continue;
            }
            if (line.equals(MEASURE) || line.startsWith(MEASURE + " ")) {     // 1.25.0: שורת כותרת — מדידה
                plan.measure = true;
                for (String x : tokens(line))
                    if (x.startsWith(SW_JOURNAL)) {
                        String[] err = {null};
                        plan.measureJournal = path(x.substring(SW_JOURNAL.length()).trim(), here, vars, err);
                    }
                continue;
            }
            if (Program.isHeader(line)) continue;                             // 1.13.0: צלמית / שם / למה — כותרת התוכנה
            if (Verbs.isVerb(line)) {                                         // 1.13.0: פקודות הפתיחה בעברית — נפתחות בסוף
                Step s = new Step();
                s.line = n + 1;
                s.text = line;
                s.cmd = OPEN;
                s.open = Verbs.withVars(line, vars);
                Verbs.Cmd vc = Verbs.parse(s.open);
                s.error = vc.error;
                s.describe = Verbs.describe(vc);
                if (s.error != null) plan.errors++;
                else plan.open = s.open;
                plan.steps.add(s);
                continue;
            }
            List<String> tk = tokens(line);
            if (tk.isEmpty()) continue;
            String cmd = Commands.canonical(tk.get(0));                       // 1.22.5: שמות נרדפים — מההגדרה האחת
            if (cmd.equals("REM")) continue;
            Step s = new Step();
            s.line = n + 1;
            s.text = line;
            if (cmd.equals("ECHO.")) cmd = "ECHO";                   // 139: שורה ריקה, כמו ב-bat
            s.cmd = cmd;
            if (cmd.equals("ECHO")) {
                String rest = line.length() > 4 ? line.substring(4).trim() : "";
                if (rest.equalsIgnoreCase("OFF") || rest.equalsIgnoreCase("ON")) continue;
                plan.echoes.add(rest.startsWith(".") ? rest.substring(1) : rest);
                s.describe = Words.Cmd.echo(rest);
                plan.steps.add(s);
                continue;
            }
            List<String> args = new ArrayList<String>(), sw = new ArrayList<String>();
            List<String> lsw = new ArrayList<String>();
            ListCmd.Parsed lp = ListCmd.parse(line, lsw);                      // 1.24.0: מילות סוג — רשימות
            if (lp != null && lp.error == null && lp.anyList()) {
                planList(s, cmd, lp, lsw, here, vars, sim);
                if (s.error != null) plan.errors++;
                plan.steps.add(s);
                continue;
            }
            if (lp != null && lp.error != null) {
                s.error = lp.error;
                plan.errors++;
                plan.steps.add(s);
                continue;
            }
            if (lp != null) {                                                  // רק "תיקייה" — שורה רגילה בלי המילים
                sw.addAll(lsw);
                for (ListCmd.Side x : lp.sides) args.add(x.raw);
            } else
            for (int i = 1; i < tk.size(); i++) (isSwitch(tk.get(i)) ? sw : args).add(tk.get(i));
            String[] err = {null};
            List<String> paths = new ArrayList<String>();
            for (String a : args) {
                String p = path(a, here, vars, err);
                if (p == null) break;
                paths.add(p);
            }
            if (err[0] == null)                                                // 1.19.0: /יומן:<קובץ>
                for (String x : sw) {
                    if (!x.startsWith(SW_JOURNAL)) continue;
                    String v = x.substring(SW_JOURNAL.length()).trim();
                    if (v.isEmpty() || !(cmd.equals("COPY") || cmd.equals("MOVE"))) {
                        err[0] = Words.Cmd.JOURNAL_USAGE;
                        break;
                    }
                    s.journal = path(v, here, vars, err);
                    if (s.journal == null) break;
                }
            if (err[0] == null) err[0] = setsMisuse(cmd, paths);            // 1.20.0
            if (err[0] == null) err[0] = shareMisuse(cmd, paths);           // 203
            if (err[0] == null) err[0] = homeMisuse(cmd, paths);            // 1.9.0
            if (err[0] != null) s.error = err[0];
            else plan(s, cmd, args, paths, sw, sim);
            if (s.error != null) plan.errors++;
            if (cmd.equals("START") && s.error == null) plan.start = s.dest;
            plan.steps.add(s);
        }
        return plan;
    }

    /** 1.24.0: שורה עם רשימה — הצד שבכונן מפוענח כאן (מסלול, תבנית, סינון), והשאר — ListCmd. */
    private static void planList(Step s, String cmd, ListCmd.Parsed lp, List<String> sw, File here, Map<String, String> vars, Sim sim) {
        List<String> fsFiles = new ArrayList<String>();
        File fsDest = null;
        for (int i = 0; i < lp.sides.size(); i++) {
            ListCmd.Side x = lp.sides.get(i);
            if (x.list()) {
                ListCmd.resolve(x, vars);
                continue;
            }
            String[] err = {null};
            String p = path(x.raw, here, vars, err);
            if (p == null) { s.error = err[0]; return; }
            if (i == lp.sides.size() - 1 && i > 0) fsDest = new File(p);
            else {
                match(s, p, filter(sw), false, sim);
                if (s.error != null) return;
                if (!s.dirs.isEmpty()) { s.error = Words.ListCmdW.FOLDER_TO_LIST; return; }
                fsFiles.addAll(s.files);
                s.files.clear();
            }
        }
        ListCmd.plan(s, cmd, lp, sw, sim, fsFiles, fsDest);
    }

    private static void plan(Step s, String cmd, List<String> args, List<String> paths, List<String> sw, Sim sim) {
        Filter flt = filter(sw);
        if (flt.error != null) {
            s.error = flt.error;
            return;
        }
        for (String x : sw) {
            String u = x.toUpperCase(Locale.ROOT);
            if (u.equals("/Y") || x.equals(SW_TAKEN + TAKEN_REPLACE)) s.replace = true;
            if (x.equals(SW_TAKEN + TAKEN_SKIP)) s.skipTaken = true;
            if (x.equals(SW_TAKEN + TAKEN_STOP)) s.failTaken = true;          // 1.20.0
            if (u.equals("/S") || x.equals(SW_KEEP)) s.recursive = true;
            if (u.equals("/A") || u.startsWith("/A:H") || u.equals("/A:")) s.hidden = true;
            if (x.startsWith(SW_TAKEN) && !x.equals(SW_TAKEN + TAKEN_REPLACE) && !x.equals(SW_TAKEN + TAKEN_SKIP)
                    && !x.equals(SW_TAKEN + TAKEN_ASK) && !x.equals(SW_TAKEN + TAKEN_STOP))
                s.error = Words.Cmd.switchUnknown(x) + " (" + TAKEN_ASK + " · " + TAKEN_SKIP + " · " + TAKEN_REPLACE + " · " + TAKEN_STOP + ")";
            if (x.startsWith(SW_LIMIT) && limit(x) < 0) s.error = Words.Cmd.switchUnknown(x);
            if (x.startsWith(SW_LIST)) {                                      // 1.22.0
                s.listName = x.substring(SW_LIST.length()).trim();
                if (s.listName.isEmpty() || !cmd.equals("COMP")) s.error = Words.Cmd.LIST_ONLY_COMP;
            }
            if (x.startsWith(SW_STATE)) {                                     // 1.21.0
                s.stateWhen = State.when(x.substring(SW_STATE.length()));
                if (s.stateWhen == Long.MIN_VALUE) s.error = Words.Cmd.switchUnknown(x);
                else if (!cmd.equals("COPY")) s.error = Words.Cmd.STATE_ONLY_COPY;
            }
        }
        if (s.error != null) return;
        if (cmd.equals("MD")) {
            if (paths.size() != 1) { s.error = Words.Cmd.usage(cmd); return; }
            File d = new File(paths.get(0));
            if (sim.isDir(d)) {                                                // קיימת — בלי שינוי
                s.noop = true;
                s.dest = d;
                s.describe = Words.Cmd.folderExists(d.getName());
                return;
            }
            if (sim.isFile(d)) { s.error = Words.Cmd.exists(d.getName()); return; }
            s.dest = d;
            s.describe = Words.Cmd.newFolder(Bidi.path(d.getAbsolutePath()));
            sim.dirs.add(d.getAbsolutePath());
        } else if (cmd.equals("RD")) {
            if (paths.size() != 1) { s.error = Words.Cmd.usage(cmd); return; }
            File d = new File(paths.get(0));
            if (!sim.isDir(d)) { s.error = Words.FOLDER_NOT_FOUND + ": " + args.get(0); return; }
            if (!s.recursive && (!sim.files(d).isEmpty() || hasSubdirs(d))) {
                s.error = Words.Cmd.FOLDER_NOT_EMPTY;
                return;
            }
            s.dirs.add(d.getAbsolutePath());
            s.describe = Words.Cmd.folderToTrash(Bidi.path(d.getAbsolutePath()));
            sim.gone.add(d.getAbsolutePath());
        } else if (cmd.equals("DEL")) {
            if (paths.size() != 1) { s.error = Words.Cmd.usage(cmd); return; }
            if (s.recursive) { s.error = Words.Cmd.DEL_S; return; }
            match(s, paths.get(0), flt, false, sim);
            if (s.error == null) s.describe = Words.Cmd.toTrash(Words.files(s.files.size())) + filtered(s);
            for (String f : s.files) {
                sim.gone.add(f);
                sim.added.remove(f);
            }
        } else if (cmd.equals("COPY") && paths.size() == 2 && paths.get(1).equals(SHARE_MARK)) {   // 203: לשיתוף
            s.share = true;
            match(s, paths.get(0), flt, true, sim);                          // תיקייה — כמו שהיא (תיארז בשמה)
            if (s.error != null) return;
            if (!s.dirs.isEmpty() && !flt.tests.isEmpty()) {
                s.error = Words.Cmd.filterOnFolder(cmd + " \"" + args.get(0) + "\\*.*\" …");
                return;
            }
            List<String> what = new ArrayList<String>();
            if (!s.files.isEmpty() || s.dirs.isEmpty()) what.add(Words.files(s.files.size()));
            if (!s.dirs.isEmpty()) what.add(Words.folders(s.dirs.size()));
            s.describe = Words.Cmd.toShare(join(what, Words.Cmd.AND)) + filtered(s);
        } else if (cmd.equals("COPY") && s.stateWhen != Long.MIN_VALUE) {         // 1.21.0: שחזור
            restore(s, args, paths, flt, sim);
        } else if (cmd.equals("COMP")) {                                      // 1.20.0
            comp(s, args, paths, sim);
        } else if ((cmd.equals("COPY") || cmd.equals("MOVE")) && paths.size() == 2
                && (paths.get(0).equals(ONLY_A_MARK) || paths.get(0).equals(ONLY_B_MARK))) {
            fromCompare(s, cmd, paths, sw, sim);
        } else if ((cmd.equals("COPY") || cmd.equals("MOVE")) && s.recursive) {     // 1.19.0: שמירת מסלול
            keep(s, cmd, args, paths, flt, sim);
        } else if (cmd.equals("COPY") || cmd.equals("MOVE")) {
            if (paths.size() != 2) { s.error = Words.Cmd.usage(cmd); return; }
            File dest = new File(paths.get(1));
            if (!sim.isDir(dest)) {
                s.error = Words.Cmd.NO_DEST;
                return;
            }
            s.dest = dest;
            match(s, paths.get(0), flt, cmd.equals("MOVE"), sim);
            if (s.error != null) return;
            if (!s.dirs.isEmpty() && !flt.tests.isEmpty()) {            // 121: סינון לא חל על תיקייה שלמה
                s.error = Words.Cmd.filterOnFolder(cmd + " \"" + args.get(0) + "\\*.*\" …");
                return;
            }
            if (s.skipTaken)
                for (java.util.Iterator<String> it = s.files.iterator(); it.hasNext(); )
                    if (sim.isFile(new File(dest, new File(it.next()).getName()))) it.remove();
            List<String> what = new ArrayList<String>();
            if (!s.files.isEmpty() || s.dirs.isEmpty()) what.add(Words.files(s.files.size()));
            if (!s.dirs.isEmpty()) what.add(Words.folders(s.dirs.size()));
            s.describe = Words.Cmd.transfer(cmd.equals("COPY"), join(what, Words.Cmd.AND), Bidi.path(dest.getAbsolutePath())) + filtered(s);
            for (String f : s.files) sim.move(f, new File(dest, new File(f).getName()), cmd.equals("COPY"));
            for (String d : s.dirs) {
                sim.gone.add(d);
                sim.dirs.add(new File(dest, new File(d).getName()).getAbsolutePath());
            }
        } else if (cmd.equals("REN")) {
            if (paths.size() != 2 || args.get(1).contains("/") || args.get(1).contains("\\")) {
                s.error = Words.Cmd.usage(cmd);
                return;
            }
            File f = new File(paths.get(0));
            if (args.get(0).contains("*") || args.get(0).contains("?")) { s.error = Words.Cmd.REN_WILD; return; }
            if (sim.isDir(f)) { s.error = Words.Cmd.REN_FOLDER; return; }                  // 1.22.5
            if (!sim.isFile(f)) { s.error = Words.FILE_NOT_FOUND + ": " + args.get(0); return; }
            String bad = Physical.badName(args.get(1));
            if (bad != null) { s.error = bad; return; }
            File to = new File(f.getParentFile(), args.get(1));
            if (!to.getName().equalsIgnoreCase(f.getName()) && sim.isFile(to)) { s.error = Words.Cmd.alreadyHas(args.get(1)); return; }
            s.files.add(f.getAbsolutePath());
            s.newName = args.get(1);
            s.describe = Words.Cmd.rename(f.getName(), s.newName);
            sim.move(f.getAbsolutePath(), to, false);
        } else if (cmd.equals("START")) {
            if (paths.isEmpty()) { s.error = Words.Cmd.usage(cmd); return; }
            if (paths.get(paths.size() - 1).equals(HOME_MARK)) {      // 1.9.0: START %בית%
                s.dest = new File(HOME_MARK);
                s.describe = Words.Cmd.HOME_AT_END;
                return;
            }
            File d = new File(paths.get(paths.size() - 1));       // "START "" תיקייה" — הכותרת הריקה יורדת
            String name = d.getName();
            if (name.contains("*") || name.contains("?")) {       // 186: תבנית — החדש ביותר מבין המתאימים
                File dir = d.getParentFile();
                if (dir == null || !sim.isDir(dir)) { s.error = Words.FOLDER_NOT_FOUND + ": " + (dir == null ? name : dir.getAbsolutePath()); return; }
                File best = null;
                int n = 0;
                for (File x : sim.files(dir))
                    if (wild(name, x.getName()) && visible(s, x)) {
                        n++;
                        if (best == null || sim.real(x).lastModified() > sim.real(best).lastModified()) best = x;
                    }
                if (best == null) { s.error = Words.Cmd.noMatch(name); return; }
                s.dest = best;
                s.describe = Words.Cmd.openAtEnd(Bidi.path(best.getAbsolutePath())) + (n > 1 ? Words.Cmd.newestOf(n) : "");
                return;
            }
            if (sim.isFile(d)) {                                   // 186: קובץ — נפתח בסוף (apk — התקנה)
                s.dest = d;
                s.describe = Words.Cmd.openAtEnd(Bidi.path(d.getAbsolutePath()));
                return;
            }
            if (!sim.isDir(d)) { s.error = Words.FOLDER_NOT_FOUND; return; }
            s.dest = d;
            s.describe = Words.Cmd.openAtEnd(Bidi.path(d.getAbsolutePath()));
        } else {
            s.error = Words.Cmd.unknownCommand(cmd) + " (" + join(java.util.Arrays.asList(BatchLine.COMMANDS), " ") + ")";
        }
    }

    /**
     * 1.19.0: COPY/MOVE <תיקייה או תיקייה\תבנית> <יעד> /S — כל הקבצים שבתוכה, גם בתיקיות שבתוכה, נכתבים ביעד
     * באותו מסלול (כמו XCOPY /S). היעד נוצר אם אינו קיים. בלי תבנית ובלי סינון — גם תיקיות ריקות (כמו /E).
     */
    private static void keep(Step s, String cmd, List<String> args, List<String> paths, Filter flt, Sim sim) {
        if (paths.size() != 2) { s.error = Words.Cmd.usage(cmd); return; }
        File dest = new File(paths.get(1));
        if (sim.isFile(dest)) { s.error = Words.Cmd.NO_DEST; return; }
        File src = new File(paths.get(0));
        String mask = null;
        if (src.getName().contains("*") || src.getName().contains("?")) {
            mask = src.getName();
            src = src.getParentFile();
        }
        if (src == null || !sim.isDir(src)) { s.error = Words.FOLDER_NOT_FOUND + ": " + args.get(0); return; }
        if (Physical.inside(dest, src)) { s.error = Words.Disk.NOT_INTO_ITSELF; return; }
        s.dest = dest;
        s.keepRoot = src.getAbsolutePath();
        walk(s, src, mask, flt, sim, mask == null && flt.tests.isEmpty(), true);
        if (s.skipTaken)
            for (java.util.Iterator<String> it = s.files.iterator(); it.hasNext(); )
                if (sim.isFile(new File(dest, Physical.rel(s.keepRoot, it.next())))) it.remove();
        List<String> what = new ArrayList<String>();
        what.add(Words.files(s.files.size()));
        if (!s.dirs.isEmpty()) what.add(Words.Cmd.emptyFolders(s.dirs.size()));
        s.describe = Words.Cmd.transfer(cmd.equals("COPY"), join(what, Words.Cmd.AND), Bidi.path(dest.getAbsolutePath()))
                + Words.Cmd.KEEP_PATH + filtered(s);
        sim.dirs.add(dest.getAbsolutePath());
        for (String f : s.files) {
            File to = new File(dest, Physical.rel(s.keepRoot, f));
            for (File up = to.getParentFile(); up != null && !up.equals(dest); up = up.getParentFile()) sim.dirs.add(up.getAbsolutePath());
            sim.move(f, to, cmd.equals("COPY"));
        }
        for (String d : s.dirs) sim.dirs.add(new File(dest, Physical.rel(s.keepRoot, d)).getAbsolutePath());
    }

    /** הקבצים בתיקייה ובתיקיות שבתוכה (מוסתרים — רק עם /A:H; קבצים זמניים של העתקה — אף פעם). */
    private static boolean walk(Step s, File dir, String mask, Filter flt, Sim sim, boolean empties, boolean top) {
        boolean any = false;
        for (File x : sim.files(dir)) {
            if (!visible(s, x) || Physical.isPartial(x.getName())) continue;
            any = true;
            if (mask != null && !wild(mask, x.getName())) continue;
            s.total++;
            if (flt.ok(sim.real(x))) s.files.add(x.getAbsolutePath());
        }
        File[] kids = dir.listFiles();
        if (kids != null) {
            java.util.Arrays.sort(kids);
            for (File k : kids) {
                if (!k.isDirectory() || !visible(s, k) || sim.gone.contains(k.getAbsolutePath())) continue;
                any = true;
                walk(s, k, mask, flt, sim, empties, false);
            }
        }
        if (!any && !top && empties) s.dirs.add(dir.getAbsolutePath());
        return any;
    }

    /**
     * 1.20.0: COMP <מקור> <יעד> — השוואה לפי מסלול (Compare). מקור שאינו קיים — שגיאה (לא "ריק": אחרת הכול
     * ייראה כנמחק). יעד שאינו קיים — ריק רק אם שורה קודמת יוצרת אותו (MD); אחרת — שגיאה.
     */
    private static void comp(Step s, List<String> args, List<String> paths, Sim sim) {
        sim.compare = null;
        if (s.listName != null) {                                             // 1.22.0
            compList(s, args, paths, sim);
            return;
        }
        if (paths.size() != 2) { s.error = Words.Cmd.usage("COMP"); return; }
        File a = new File(paths.get(0)), b = new File(paths.get(1));
        if (!a.isDirectory()) { s.error = Words.Cmd.compareNoSource(args.get(0)); return; }
        if (!b.isDirectory() && !sim.isDir(b)) { s.error = Words.Cmd.compareNoDest(args.get(1)); return; }
        if (Physical.inside(a, b) || Physical.inside(b, a)) { s.error = Words.Disk.NOT_INTO_ITSELF; return; }
        Compare.Result r = Compare.byPath(sim.lib, a, b, s.hidden);
        sim.compare = r;
        s.dest = b;
        s.describe = Words.Cmd.compared(Bidi.path(r.a), Bidi.path(r.b),
                Compare.Result.files(r.onlyA, r.onlyADirs), Compare.Result.files(r.onlyB, r.onlyBDirs), r.same);
    }

    /**
     * 1.22.1: הרשימה לפי שם — הדרך כמו שנכתבה ("א / ב"); אחרת בתוך "שמור לתוכנה"; אחרת — שם יחיד בכל העץ.
     * ואם אין באופן הזה — באופן שבו נמצא "שמור לתוכנה" (Reserved). כמה רשימות באותו שם — שגיאה שמראה את הדרכים.
     */
    static Tree.Node findList(String name, Tree[] tree, String[] err) {
        return findList(name, tree, err, null);
    }

    /** ini[0] — ההגדרות של האופן שבו נמצאה (null — האופן הפעיל). */
    static Tree.Node findList(String name, Tree[] tree, String[] err, Ini[] ini) {
        List<String> path = Tree.parsePath(name);
        if (path.isEmpty()) { err[0] = Words.Cmd.LIST_ONLY_COMP; return null; }
        List<Tree> trees = new ArrayList<Tree>();
        List<String> modes = new ArrayList<String>();
        List<Ini> inis = new ArrayList<Ini>();
        trees.add(Tree.load());
        modes.add(Modes.current());
        inis.add(null);
        try {                                                                 // 1.22.2: גם באופני העבודה האחרים
            if (Tree.testFile == null)
                for (String m : Modes.all()) {
                    if (m.equals(Modes.current())) continue;
                    File tf = ModeCopy.treeOf(m), mf = ModeCopy.mainOf(m);
                    if (!tf.isFile() || !mf.isFile()) continue;
                    trees.add(Tree.loadFrom(tf));
                    modes.add(m);
                    inis.add(Ini.load(mf));
                }
        } catch (Throwable ignored) {
        }
        for (Tree t : trees) {
            Tree.Node n = t.find(path);
            if (n == null) {
                List<String> under = new ArrayList<String>(Reserved.ROOT_PATH);
                under.addAll(path);
                n = t.find(under);
            }
            if (n == null && path.size() == 1) {
                List<Tree.Node> found = new ArrayList<Tree.Node>();
                byName(t.root, path.get(0), found);
                if (found.size() > 1) {
                    List<String> ways = new ArrayList<String>();
                    for (Tree.Node f : found) ways.add(wayOf(f));
                    err[0] = Words.Cmd.listAmbiguous(name, join(ways, " · "));
                    return null;
                }
                if (found.size() == 1) n = found.get(0);
            }
            if (n != null) {
                tree[0] = t;
                if (ini != null) ini[0] = inis.get(trees.indexOf(t));
                return n;
            }
        }
        // 1.22.2: מה התוכנה כן רואה — כדי שיהיה ברור למה לא נמצא
        StringBuilder seen = new StringBuilder();
        for (int k = 0; k < trees.size(); k++) {
            Tree t = trees.get(k);
            List<String> top = new ArrayList<String>();
            for (Tree.Node c : t.root.children) top.add(c.name);
            Tree.Node res = t.find(Reserved.ROOT_PATH);
            List<String> in = new ArrayList<String>();
            if (res != null) for (Tree.Node c : res.children) in.add(c.name);
            seen.append(Words.Cmd.listsSeen(modes.get(k), join(top, " · "), res == null ? null : join(in, " · ")));
        }
        err[0] = Words.Cmd.listNotFound(name) + seen;
        return null;
    }

    private static void byName(Tree.Node n, String name, List<Tree.Node> out) {
        for (Tree.Node c : n.children) {
            if (c.name.equals(name)) out.add(c);
            byName(c, name, out);
        }
    }

    /** "א / ב / ג" — הדרך לרשימה. */
    private static String wayOf(Tree.Node n) {
        List<String> parts = new ArrayList<String>();
        for (Tree.Node x = n; x != null && x.parent != null; x = x.parent) parts.add(0, x.name);
        return join(parts, Tree.SEP);
    }

    /** 1.22.0: COMP /רשימה:<שם> <יעד> — כל מה שברשימה, לפי <מפתח כונן>/<מסלול>, מול היעד. */
    private static void compList(Step s, List<String> args, List<String> paths, Sim sim) {
        if (paths.size() != 1) { s.error = Words.Cmd.usage("COMP"); return; }
        File b = new File(paths.get(0));
        if (!b.isDirectory() && !sim.isDir(b)) { s.error = Words.Cmd.compareNoDest(args.get(0)); return; }
        if (sim.lib == null || !sim.lib.ready) { s.error = Words.Cmd.NEEDS_INDEX; return; }
        String[] err = {null};
        Tree[] tt = {null};
        Ini[] ini = {null};
        Tree.Node n = findList(s.listName, tt, err, ini);
        if (n == null) { s.error = err[0]; return; }
        Tree t = tt[0];
        File skip = b.getName().equals(State.CURRENT) && b.getParentFile() != null ? b.getParentFile() : b;
        Compare.Source src = Compare.ofList(sim.lib, t, n, s.hidden, skip.getAbsolutePath(), ini[0]);
        if (src.files.isEmpty() && src.emptyDirs.isEmpty()) { s.error = Words.Cmd.listEmpty(s.listName); return; }
        Compare.Result r = Compare.byList(sim.lib, src, b, s.hidden);
        sim.compare = r;
        s.dest = b;
        s.describe = Words.Cmd.compared(s.listName, Bidi.path(r.b),
                Compare.Result.files(r.onlyA, new ArrayList<String>()), Compare.Result.files(r.onlyB, r.onlyBDirs), r.same)
                + (src.outside > 0 ? Words.Cmd.outside(src.outside) : "")
                + (src.skippedSmart > 0 ? Words.Cmd.smartSkipped(src.skippedSmart) : "");
    }

    /**
     * 1.20.0: COPY/MOVE %רק-במקור% | %רק-ביעד% <יעד> — מה שההשוואה מצאה, עם שמירת מסלול (יחסית לשורש של אותו צד).
     * /עד:N% — אם זה יותר מ־N אחוזים מהקבצים של אותו צד — שגיאה, והשורות הבאות על ההשוואה לא ירוצו.
     */
    private static void fromCompare(Step s, String cmd, List<String> paths, List<String> sw, Sim sim) {
        Compare.Result r = sim.compare;
        if (r == null) { s.error = Words.Cmd.NO_COMPARE; return; }
        if (r.broken) { s.error = Words.Cmd.COMPARE_BROKEN; return; }
        boolean a = paths.get(0).equals(ONLY_A_MARK);
        File dest = new File(paths.get(1));
        if (sim.isFile(dest)) { s.error = Words.Cmd.NO_DEST; r.broken = true; return; }
        s.dest = dest;
        s.fromCompare = true;
        s.files.addAll(a ? r.onlyA : r.onlyB);
        s.dirs.addAll(a ? r.onlyADirs : r.onlyBDirs);
        boolean mapped = a && r.relA != null;                                 // 1.22.0: מקור שהוא רשימה
        if (mapped) {
            if (cmd.equals("MOVE")) { s.error = Words.Cmd.LIST_NO_MOVE; r.broken = true; return; }
            s.keepRoot = null;
        } else {
            s.keepRoot = a ? r.a : r.b;
            if (Physical.inside(dest, new File(s.keepRoot))) { s.error = Words.Disk.NOT_INTO_ITSELF; r.broken = true; return; }
        }
        int n = Compare.Result.files(s.files, s.dirs), total = a ? r.filesA : r.filesB;
        for (String x : sw)
            if (x.startsWith(SW_LIMIT)) {
                int pct = limit(x);
                if (total > 0 && (long) n * 100 > (long) pct * total) {
                    s.error = Words.Cmd.overLimit(n, total, pct);
                    r.broken = true;
                    return;
                }
            }
        if (mapped) {                                                          // 1.22.0: כל פריט — לתיקייה שלו
            List<String> items = new ArrayList<String>(s.files);
            items.addAll(s.dirs);
            s.files.clear();
            s.dirs.clear();
            for (String f : items) {
                File to = new File(dest, r.relA.get(f));
                if (s.skipTaken && sim.isFile(to)) continue;
                s.files.add(f);
                s.targets.add(to.getParentFile().getAbsolutePath());
                if (new File(f).isDirectory()) sim.dirs.add(to.getAbsolutePath());
                else sim.move(f, to, true);
            }
            s.describe = Words.Cmd.transfer(true, Words.files(s.files.size()), Bidi.path(dest.getAbsolutePath())) + Words.Cmd.KEEP_PATH;
            sim.dirs.add(dest.getAbsolutePath());
            return;
        }
        if (s.skipTaken)
            for (java.util.Iterator<String> it = s.files.iterator(); it.hasNext(); )
                if (sim.isFile(new File(dest, Physical.rel(s.keepRoot, it.next())))) it.remove();
        List<String> what = new ArrayList<String>();
        what.add(Words.files(s.files.size()));
        if (!s.dirs.isEmpty()) what.add(Words.folders(s.dirs.size()));
        s.describe = Words.Cmd.transfer(cmd.equals("COPY"), join(what, Words.Cmd.AND), Bidi.path(dest.getAbsolutePath()))
                + Words.Cmd.KEEP_PATH;
        sim.dirs.add(dest.getAbsolutePath());
        for (String f : s.files) sim.move(f, new File(dest, Physical.rel(s.keepRoot, f)), cmd.equals("COPY"));
        for (String d : s.dirs) {
            if (cmd.equals("MOVE")) sim.gone.add(d);
            sim.dirs.add(new File(dest, Physical.rel(s.keepRoot, d)).getAbsolutePath());
        }
    }

    /**
     * 1.21.0: שחזור — COPY <גיבוי> <יעד> /מצב:<זמן>. המקור: שורש גיבוי "מראה + היסטוריה", או מסלול לוגי בתוכו
     * ("<שורש>\\מסמכים\\א" = מה שהיה ב־מסמכים/א), ואפשר תבנית בסוף. כל קובץ שהיה במצב באותו זמן (State) — מ"נוכחי"
     * או מתיקיית היסטוריה — נכתב ביעד במסלול הלוגי שלו (יחסית לתת־התיקייה שנבחרה). הגיבוי עצמו — לא משתנה.
     */
    private static void restore(Step s, List<String> args, List<String> paths, Filter flt, Sim sim) {
        if (paths.size() != 2) { s.error = Words.Cmd.usage("COPY"); return; }
        File src = new File(paths.get(0));
        String mask = null;
        if (src.getName().contains("*") || src.getName().contains("?")) {
            mask = src.getName();
            src = src.getParentFile();
        }
        File root = src;
        while (root != null && !State.isBackupRoot(root)) root = root.getParentFile();
        if (root == null) { s.error = Words.Cmd.notBackup(args.get(0)); return; }
        String prefix = Physical.rel(root.getAbsolutePath(), src.getAbsolutePath());
        if (prefix.equals(State.CURRENT) || prefix.startsWith(State.CURRENT + "/") || prefix.equals(State.HISTORY)
                || prefix.startsWith(State.HISTORY + "/")) { s.error = Words.Cmd.STATE_LOGICAL; return; }
        File dest = new File(paths.get(1));
        if (sim.isFile(dest)) { s.error = Words.Cmd.NO_DEST; return; }
        if (Physical.inside(dest, root)) { s.error = Words.Disk.NOT_INTO_ITSELF; return; }
        State st = new State(s.stateWhen);
        java.util.TreeMap<String, State.Where> best = new java.util.TreeMap<String, State.Where>();
        java.util.Map<String, String> phys = new java.util.HashMap<String, String>();
        List<File> places = new ArrayList<File>();
        places.add(new File(root, State.CURRENT));
        File[] hs = new File(root, State.HISTORY).listFiles();
        if (hs != null) for (File h : hs) if (h.isDirectory()) places.add(h);
        for (File place : places) {
            File start = prefix.isEmpty() ? place : new File(place, prefix);
            List<String> found = new ArrayList<String>();
            collect(s, start, mask, found);
            for (String f : found) {
                Library.Hit hit = sim.lib == null ? null : sim.lib.find(f);
                State.Where w = st.version(f, hit == null || hit.rec == null ? 0 : hit.rec.added);
                if (w == null || !(w.came <= st.when && st.when < w.left)) continue;
                s.total++;
                if (!flt.ok(new File(f))) continue;
                State.Where old = best.get(w.rel);
                if (old == null || w.left < old.left) {                     // שתי גרסאות — הקרובה ביותר אחרי הזמן
                    best.put(w.rel, w);
                    phys.put(w.rel, f);
                }
            }
        }
        s.dest = dest;
        for (Map.Entry<String, State.Where> e : best.entrySet()) {
            String rel = e.getKey();
            String sub = prefix.isEmpty() ? rel : rel.substring(prefix.length() + 1);
            File to = new File(dest, sub).getParentFile();
            s.files.add(phys.get(rel));
            s.targets.add(to.getAbsolutePath());
            sim.added.put(new File(dest, sub).getAbsolutePath(), new File(phys.get(rel)));
            for (File up = to; up != null && !up.equals(dest.getParentFile()); up = up.getParentFile()) sim.dirs.add(up.getAbsolutePath());
        }
        if (s.skipTaken)
            for (int k = s.files.size() - 1; k >= 0; k--)
                if (new File(s.targets.get(k), new File(s.files.get(k)).getName()).exists()) {
                    s.files.remove(k);
                    s.targets.remove(k);
                }
        s.describe = Words.Cmd.restore(Words.files(s.files.size()), new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.ROOT)
                .format(new java.util.Date(s.stateWhen)), Bidi.path(dest.getAbsolutePath()));   // בכונן
    }

    /** כל הקבצים מתחת ל־d (מוסתרים — רק עם /A:H; זמניים — אף פעם), לפי תבנית אם יש. */
    private static void collect(Step s, File d, String mask, List<String> out) {
        File[] k = d.listFiles();
        if (k == null) return;
        java.util.Arrays.sort(k);
        for (File x : k) {
            if (!visible(s, x) || Physical.isPartial(x.getName())) continue;
            if (x.isDirectory()) collect(s, x, mask, out);
            else if (mask == null || wild(mask, x.getName())) out.add(x.getAbsolutePath());
        }
    }

    /** /עד:20% או /עד:20 — המספר; -1 אם אינו מספר בין 0 ל־100. */
    static int limit(String sw) {
        String v = sw.substring(SW_LIMIT.length()).trim();
        if (v.endsWith("%")) v = v.substring(0, v.length() - 1).trim();
        try {
            int n = Integer.parseInt(v);
            return n >= 0 && n <= 100 ? n : -1;
        } catch (Throwable t) {
            return -1;
        }
    }

    /** 1.20.0: %רק-במקור% / %רק-ביעד% — רק לבדם, כמקור של COPY או MOVE. */
    private static String setsMisuse(String cmd, List<String> paths) {
        for (int i = 0; i < paths.size(); i++) {
            String p = paths.get(i);
            if (!p.startsWith(ONLY_A_MARK) && !p.startsWith(ONLY_B_MARK)) continue;
            if (!(cmd.equals("COPY") || cmd.equals("MOVE")) || i != 0 || !(p.equals(ONLY_A_MARK) || p.equals(ONLY_B_MARK)))
                return Words.Cmd.SETS_ONLY_SOURCE;
        }
        return null;
    }

    /** 1.9.0: "%בית%" — רק לבדו, אחרי START. */
    private static String homeMisuse(String cmd, List<String> paths) {
        for (String p : paths) {
            if (!p.startsWith(HOME_MARK)) continue;
            if (!cmd.equals("START") || !p.equals(HOME_MARK)) return Words.Cmd.HOME_ONLY_START;
        }
        return null;
    }

    /** 203: "%שיתוף%" — רק כיעד של COPY, ובלי מסלול אחריו. */
    private static String shareMisuse(String cmd, List<String> paths) {
        for (int i = 0; i < paths.size(); i++) {
            String p = paths.get(i);
            if (!p.startsWith(SHARE_MARK)) continue;
            if (!p.equals(SHARE_MARK)) return Words.Cmd.SHARE_NO_PATH;
            if (cmd.equals("MOVE") && i == 1) return Words.Cmd.SHARE_NO_MOVE;
            if (!cmd.equals("COPY") || i != 1) return Words.Cmd.SHARE_ONLY_COPY;
        }
        return null;
    }

    private static boolean hasSubdirs(File d) {
        File[] k = d.listFiles();
        if (k != null) for (File x : k) if (x.isDirectory()) return true;
        return false;
    }

    private static String join(List<String> parts, String sep) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) sb.append(sb.length() > 0 ? sep : "").append(p);
        return sb.toString();
    }

    /** קובץ מוסתר (שם שמתחיל בנקודה) — כמו ב-bat, רק עם /A:H (135). */
    private static boolean visible(Step s, File x) {
        return s.hidden || !x.getName().startsWith(".");
    }

    /** מקור: קובץ, תבנית (*.pdf), או תיקייה (COPY — הקבצים שבתוכה; MOVE — התיקייה עצמה, כמו ב-bat). */
    private static void match(Step s, String src, Filter flt, boolean moveDir, Sim sim) {
        File f = new File(src);
        String name = f.getName();
        if (name.contains("*") || name.contains("?")) {
            File dir = f.getParentFile();
            if (dir == null || !sim.isDir(dir)) { s.error = Words.FOLDER_NOT_FOUND + ": " + (dir == null ? src : dir.getAbsolutePath()); return; }
            for (File x : sim.files(dir))
                if (wild(name, x.getName()) && visible(s, x)) {
                    s.total++;
                    if (flt.ok(sim.real(x))) s.files.add(x.getAbsolutePath());
                }
            return;
        }
        if (sim.isDir(f)) {
            if (moveDir) {
                s.dirs.add(f.getAbsolutePath());
                return;
            }
            for (File x : sim.files(f))
                if (visible(s, x)) {
                    s.total++;
                    if (flt.ok(sim.real(x))) s.files.add(x.getAbsolutePath());
                }
            return;
        }
        if (sim.isFile(f)) {                                      // קובץ שנכתב במפורש — גם אם מוסתר
            s.total++;
            if (flt.ok(sim.real(f))) s.files.add(f.getAbsolutePath());
            return;
        }
        s.error = Words.Cmd.notFound(src);
    }

    private static String filtered(Step s) {
        return s.total > s.files.size() && s.dirs.isEmpty() ? Words.Cmd.filtered(s.files.size(), s.total) : "";
    }

    // ---------- ביצוע ----------

    /** מבצע את התוכנית (ברקע), דרך Physical. מחזיר דוח. */
    static List<String> run(Plan plan, Library lib, Physical.Asker asker, Physical.Progress progress) {
        return run(plan, lib, asker, progress, false);
    }

    /** קובץ שעוד לא באינדקס (למשל הורדה חדשה) — נכנס אליו לפני שמוחקים אותו (134). */
    private static void known(Library lib, List<String> files) {
        boolean any = false;
        for (String p : files)
            if (lib.find(p) == null && new File(p).isFile()) {
                lib.fileAdded(new File(p));
                any = true;
            }
        if (any) lib.flush(5000);
    }

    /** grey: ההגדרה "קובץ שנמחק — השאר באפור" (58), כמו בשאר המסכים (120). */
    /** 1.26.0: מסך ההרצה — לפני כל שורה (מקום, כמה בסך הכול), ואחרי כל שורה (מה יצא). */
    interface Listener {
        void before(int index, int total, Step s);

        void after(Step s, String line);
    }

    static List<String> run(Plan plan, Library lib, Physical.Asker asker, Physical.Progress progress, boolean grey) {
        return run(plan, lib, asker, progress, grey, null);
    }

    static List<String> run(Plan plan, Library lib, Physical.Asker asker, Physical.Progress progress, boolean grey, Listener listen) {
        List<String> report = new ArrayList<String>();
        Clock clock = new Clock();                                               // 1.25.0: לולאה — עצירה לפי זמן
        long all0 = System.nanoTime();
        Step prev = null;
        int mark = 0;
        long t0 = System.nanoTime();
        for (int si = 0; si <= plan.steps.size(); si++) {
            if (prev != null && plan.measure) {                                  // 1.25.0: מדידה — זמן השורה הקודמת
                prev.ms = (System.nanoTime() - t0) / 1000000;
                if (report.size() > mark) report.set(report.size() - 1, report.get(report.size() - 1) + Words.Loop.took(prev.ms));
            }
            if (prev != null && listen != null) listen.after(prev, report.size() > mark ? report.get(report.size() - 1) : null);
            mark = report.size();
            t0 = System.nanoTime();
            prev = null;
            if (si == plan.steps.size()) break;
            Step s = plan.steps.get(si);
            if (progress != null && progress.stopped()) {
                report.add(Words.Cmd.stoppedBefore(s.line));
                plan.trouble = true;
                break;
            }
            if (clock.skip(plan, s.loops, s.rounds)) {
                if (!clock.notes.isEmpty()) {
                    report.addAll(clock.notes);
                    clock.notes.clear();
                }
                continue;
            }
            prev = s;
            s.ran = s.error == null;
            if (listen != null) listen.before(si, plan.steps.size(), s);
            if (s.error != null) {
                report.add(Words.Cmd.line(s.label(), Words.Cmd.skipped(s.error)));
                plan.trouble = true;
                continue;
            }
            String out;
            if (s.cmd.equals("ECHO") || s.cmd.equals("START") || s.cmd.equals(OPEN) || s.noop) continue;
            if (s.op != null) {                                                // 1.24.0: רשימות
                report.add(Words.Cmd.line(s.label(), s.op.run(lib)));
                continue;
            }
            if (s.cmd.equals("COMP")) {                                       // 1.20.0: חושב בתכנון — רק בדוח
                report.add(Words.Cmd.line(s.label(), s.describe));
                continue;
            }
            if (s.cmd.equals("MD")) {
                String why = Physical.makeFolder(lib, s.dest.getParentFile(), s.dest.getName());
                out = why == null ? Words.Cmd.created(s.dest.getName()) : why;
                if (why != null) plan.trouble = true;
            } else if (s.cmd.equals("RD") || s.cmd.equals("DEL")) {
                if (s.files.isEmpty() && s.dirs.isEmpty()) out = Words.Cmd.NO_FILES;
                else {
                    known(lib, s.files);
                    Physical.Result r = Physical.delete(lib, s.files, s.dirs, grey);
                    out = result(r, Words.Cmd.TRASHED);
                    if (r.failed > 0) plan.trouble = true;
                }
            } else if (s.share) {                                          // 203
                if (s.files.isEmpty() && s.dirs.isEmpty()) out = Words.Cmd.NO_FILES;
                else out = stage(plan, s, progress);
            } else if (s.cmd.equals("COPY") || s.cmd.equals("MOVE")) {
                if (s.files.isEmpty() && s.dirs.isEmpty()) out = Words.Cmd.NO_FILES;
                else {
                    boolean copy = s.cmd.equals("COPY");
                    Physical.Journal j = s.journal == null ? null : new Physical.Journal(new File(s.journal), s.text);   // 1.19.0
                    Physical.Asker ask = s.replace ? REPLACE_ALL : s.failTaken ? null : asker;   // 1.20.0: /תפוס:לעצור
                    Physical.Result r = !s.targets.isEmpty()                           // 1.21.0 שחזור, 1.22.0 רשימה
                            ? Physical.transferTo(lib, s.files, s.targets, s.dest, true, ask, progress, grey, j)
                            : s.keepRoot != null
                            ? Physical.transferKeep(lib, s.keepRoot, s.files, s.dirs, s.dest, copy, ask, progress, grey, j,
                                    !s.fromCompare)
                            : Physical.transfer(lib, s.files, s.dirs, s.dest, copy, ask, progress, grey, j);
                    out = result(r, copy ? Words.Cmd.COPIED : Words.Cmd.MOVED);
                    if (j != null) {
                        j.close();
                        if (j.failed()) {
                            out += Words.Cmd.JOURNAL_FAILED;
                            plan.trouble = true;
                        }
                    }
                    if (r.failed > 0) plan.trouble = true;
                }
            } else if (s.cmd.equals("REN")) {
                Physical.Result r = Physical.rename(lib, s.files.get(0), s.newName);
                out = result(r, Words.Cmd.RENAMED);
                if (r.failed > 0) plan.trouble = true;
            } else {
                out = Words.Cmd.NOT_DONE;
                plan.trouble = true;
            }
            report.add(Words.Cmd.line(s.label(), out));
        }
        if (plan.measure) measured(plan, report, (System.nanoTime() - all0) / 1000000);
        return report;
    }

    /** 1.26.0: מה שורה עושה — לתצוגה המקדימה ולסיכום. */
    static final int K_CREATE = 0, K_CHANGE = 1, K_DELETE = 2, K_READ = 3;

    static int kind(Step s) {
        if (s.cmd.equals("MD")) return K_CREATE;
        if (s.cmd.equals("DEL") || s.cmd.equals("RD")) return K_DELETE;
        if (s.cmd.equals("COPY") || s.cmd.equals("MOVE") || s.cmd.equals("REN")) return s.share ? K_READ : K_CHANGE;
        return K_READ;
    }

    /** נוגעת בקבצים בכונן (לא ברשימות). */
    static boolean disk(Step s) {
        return s.op == null && !s.noop && (s.cmd.equals("COPY") || s.cmd.equals("MOVE") || s.cmd.equals("DEL") || s.cmd.equals("RD")
                || s.cmd.equals("MD") || s.cmd.equals("REN"));
    }

    /** 1.26.0: רק פתיחת מסכים ורשימות (פקודות התפריט ⭐) — הרצה מהירה, בלי מסך ההרצה. */
    static boolean onlyOpens(String text) {
        for (String raw : Bidi.strip(text).replace("\r", "").split("\n")) {
            String line = clean(raw);
            if (line.isEmpty() || line.startsWith("::") || Program.isHeader(line)) continue;
            if (isLoopStart(line) || isLoopEnd(line) || line.startsWith(MEASURE) || line.equals(AS_IF) || line.equals(AS_IF2)) return false;
            if (Verbs.isVerb(line)) continue;
            String w = tokens(line).isEmpty() ? "" : Commands.canonical(tokens(line).get(0));
            if (w.equals("ECHO") || w.equals("ECHO.") || w.equals("START") || w.equals("REM")) continue;
            return false;
        }
        return true;
    }

    /** 1.25.0: סיכום המדידה, ויומן (שורה לכל שורה בדוח, נוסף בכל ריצה). */
    private static void measured(Plan plan, List<String> report, long total) {
        Step slow = null;
        for (Step x : plan.steps) if (x.ms >= 0 && (slow == null || x.ms > slow.ms)) slow = x;
        report.add(Words.Loop.total(total, slow == null ? null : slow.label(), slow == null ? 0 : slow.ms));
        if (plan.measureJournal == null) return;
        try {
            File j = new File(plan.measureJournal);
            if (j.getParentFile() != null) j.getParentFile().mkdirs();
            java.io.Writer w = new java.io.OutputStreamWriter(new java.io.FileOutputStream(j, true), "UTF-8");
            try {
                w.write("=== " + new java.text.SimpleDateFormat(NOW_FORMAT, Locale.ROOT).format(new java.util.Date()) + " · "
                        + Log.version + "\n");
                for (String r : report) w.write(r + "\n");
                w.write("\n");
            } finally {
                w.close();
            }
        } catch (Throwable t) {
            report.add(Words.Loop.JOURNAL_FAILED);
            Log.e("מדידה — יומן", t);
        }
    }

    /**
     * 203: מכין לשליחה — עותק של כל קובץ, ואריזה של כל תיקייה, בתיקיית השליחה. כך שורה מאוחרת יותר (DEL, MOVE)
     * לא פוגעת במה שעוד לא נשלח. אין די מקום — לא מכין, ואומר כמה צריך.
     */
    private static String stage(Plan plan, Step s, Physical.Progress progress) {
        File dir = stageDir(plan);
        long need = 0;
        for (String f : s.files) need += new File(f).length();
        for (String d : s.dirs) need += Zip.measure(new File(d))[1];
        long free = dir.getUsableSpace();
        if (free < need) return Words.ActsW.NO_ROOM_PACK + Files.size(need) + Words.ActsW.FREE + Files.size(free);
        int done = 0, failed = 0;
        for (String f : s.files) {
            File src = new File(f);
            File to = new File(dir, Physical.freeName(dir, src.getName(), false));
            try {
                Files.copy(src, to);
                plan.shared.add(to);
                done++;
            } catch (Throwable t) {
                Log.e("203: שיתוף בקובץ פקודות", t);
                failed++;
            }
        }
        for (String d : s.dirs) {                                          // 1.10.0: תיקייה — הקבצים שבה כמו שהם
            File src = new File(d);
            List<File> inside = Acts.filesIn(src);
            if (inside.size() > Acts.MAX_SEND) {                              // יותר מדי לחלון השיתוף — ארוזה בשמה
                File zip = new File(dir, Physical.freeName(dir, src.getName() + ".zip", false));
                Zip.Result r = Zip.pack(src, zip, progress);
                if (r.error == null && !r.stopped) {
                    plan.shared.add(zip);
                    done++;
                } else failed++;
                continue;
            }
            for (File x : inside) {
                File to = new File(dir, Physical.freeName(dir, x.getName(), false));
                try {
                    Files.copy(x, to);
                    plan.shared.add(to);
                    done++;
                } catch (Throwable t) {
                    Log.e("1.10.0: שיתוף תיקייה כקבצים", t);
                    failed++;
                }
            }
        }
        return Words.Cmd.STAGED + " " + done + (failed > 0 ? Words.Cmd.failed(failed) : "");
    }

    /** תיקייה חדשה לכל הרצה, בתוך תיקיית השליחה; ישנות (מעל יום) נמחקות. */
    private static File stageDir(Plan plan) {
        if (plan.stage != null) return plan.stage;
        File root = Acts.sendDir();
        File[] old = root.listFiles();
        if (old != null)
            for (File f : old)
                if (f.isDirectory() && f.getName().startsWith(STAGE) && System.currentTimeMillis() - f.lastModified() > 24L * 3600 * 1000)
                    wipe(f);
        plan.stage = new File(root, STAGE + System.currentTimeMillis());
        plan.stage.mkdirs();
        return plan.stage;
    }

    private static final String STAGE = "batch-";

    /** מוחק הכנה ישנה — רק בתוך תיקיית השליחה של התוכנה (לא קובצי המשתמש). */
    private static void wipe(File f) {
        File[] k = f.listFiles();
        if (k != null) for (File x : k) wipe(x);
        f.delete();
    }

    private static String result(Physical.Result r, String verb) {
        String s = verb + " " + r.done.size();
        if (r.failed > 0) s += Words.Cmd.failed(r.failed) + (r.note.isEmpty() ? "" : " (" + r.note + ")");
        return s;
    }

    /** /Y: שם תפוס — להחליף (הקודם עובר לסל), בלי לשאול. */
    static final Physical.Asker REPLACE_ALL = new Physical.Asker() {
        @Override
        public Physical.Answer ask(File existing, File incoming, String suggested) {
            return new Physical.Answer(Physical.Choice.REPLACE, null, true);
        }
    };

    /** תבנית לקובץ חדש — ההסבר מהמילון, התחביר מכאן. */
    static String template() {
        return Words.Cmd.template(join(Arrays.asList(BatchLine.COMMANDS), "  "),
                var(V_INTERNAL) + "  " + var(V_CARD) + "  " + var(V_DOWNLOADS) + "  " + var(V_HERE) + "  " + var(V_SHARE) + "  " + var(V_HOME)
                        + "  " + var(V_NOW) + "  " + var(V_CARD_ID),
                "/" + SW_DATE + "יום  /" + SW_DATE + "<שנה  /" + SW_SIZE + ">10MB",   // תחביר
                SW_TAKEN + TAKEN_ASK, SW_TAKEN + TAKEN_SKIP,
                "MOVE \"" + var(V_DOWNLOADS) + "\\*.pdf\" \"" + var(V_INTERNAL) + "\\ספרים\" /" + SW_DATE + "שבוע");   // תחביר
    }
}
