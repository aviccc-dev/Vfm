package com.tikiyot.vfm;

// טקסט וחיפוש. אחראי: נרמול עברית, התאמת תבניות, דירוג.
// אסור לו: לגעת בקבצים.

import java.util.Locale;

public final class Text {

    /** בידוד כיוון כתיבה (80): קטע עברי/לועזי בתוך שורה לא משבש את סדר השורה. פתיחה וסגירה — לבניית שורה בחלקים. */
    public static final String ISO = "\u2068", PDI = "\u2069";

    // ---------- שם דומה (85) ----------

    /** מילים שאינן חלק מהשם: "עותק", "copy". */
    private static final java.util.Set<String> NOISE = new java.util.HashSet<String>(java.util.Arrays.asList("עותק", "copy"));  // תחביר

    /**
     * המילים של שם קובץ, לצורך "שם דומה": בלי הסיומת, בלי מה שבסוגריים, בלי סימנים,
     * בלי "עותק"/"copy", בכתיב חסר, באותיות קטנות — וכקבוצה (סדר המילים לא משנה).
     */
    public static java.util.Set<String> nameWords(String name) {
        String n = name;
        int dot = n.lastIndexOf('.');
        if (dot > 0) n = n.substring(0, dot);
        n = n.replaceAll("\\([^)]*\\)|\\[[^\\]]*\\]|\\{[^}]*\\}", " ");
        n = norm(n).toLowerCase(java.util.Locale.ROOT);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n.length(); i++) {
            char c = n.charAt(i);
            sb.append(Character.isLetterOrDigit(c) ? c : ' ');
        }
        java.util.Set<String> out = new java.util.TreeSet<String>();
        for (String w : sb.toString().trim().split("\\s+")) {
            if (w.isEmpty() || NOISE.contains(w)) continue;
            out.add(loose(w));
        }
        return out;
    }

    /** סוגים מוכרים — לזיהוי סיומת משובשת מהורדות (104). הורחב ב־123 (heic, tif, m4b, wmv, fb2, cbz…). */
    static final String[] KNOWN_TYPES = {
            "pdf", "epub", "mobi", "azw3", "djvu", "fb2", "cbz", "cbr", "chm", "lit",
            "doc", "docx", "txt", "rtf", "odt", "ods", "odp", "htm", "html", "mht", "mhtml", "log", "ini", "csv", "json", "xml",
            "jpg", "jpeg", "png", "gif", "webp", "heic", "heif", "bmp", "tif", "tiff", "svg",
            "mp3", "m4a", "m4b", "aac", "opus", "wav", "ogg", "flac", "wma", "mid",
            "mp4", "mkv", "avi", "mov", "wmv", "3gp", "webm",
            "zip", "rar", "7z", "gz", "xz", "tar", "iso", "apk", "xls", "xlsx", "ppt", "pptx", "js", "css", "vbat"};

    /**
     * 124: שם בלי הסיומת — רק כשהשם באמת נגמר בה ("ספר.pdf" ← "ספר"; "פרק 1.17" ו"ספר.pdf (1)" — כמו שהם).
     * מקום אחד לשם מוצג, לשם תפוס ("(2)") ולשחזור מהסל.
     */
    public static String stem(String name) {
        String e = ext(name);
        if (e.isEmpty() || !name.toLowerCase(java.util.Locale.ROOT).endsWith("." + e)) return name;
        return name.substring(0, name.length() - e.length() - 1);
    }

    /** הסיומת כמו שהיא בשם (עם הנקודה), או "" — המשלים של stem. */
    public static String tail(String name) {
        return name.substring(stem(name).length());
    }

    /**
     * סיומת (הסוג) באותיות קטנות, או "". 104: סיומת שכולה ספרות אינה סיומת ("פרק 1.17");
     * סיומת שמתחילה בסוג מוכר ואחריו סימן או רווח — היא אותו סוג ("ספר.pdf (1)", "שיר.mp3-_=1").
     * השם עצמו לא משתנה; רק הזיהוי.
     */
    public static String ext(String name) {
        int dot = name.lastIndexOf('.');
        if (dot <= 0 || dot >= name.length() - 1) return "";
        String e = name.substring(dot + 1).toLowerCase(java.util.Locale.ROOT);
        boolean digits = true, plain = true;
        for (int i = 0; i < e.length(); i++) {
            char ch = e.charAt(i);
            if (!Character.isDigit(ch)) digits = false;
            if (!Character.isLetterOrDigit(ch)) plain = false;
        }
        if (digits) return "";
        if (plain) return e;
        String best = "";
        for (String k : KNOWN_TYPES)
            if (e.length() > k.length() && e.startsWith(k) && !Character.isLetterOrDigit(e.charAt(k.length()))
                    && k.length() > best.length()) best = k;
        return best.isEmpty() ? e : best;
    }

    /**
     * שני שמות "דומים" (85): אותה סיומת, ומילות האחד כולן בתוך מילות השני (או זהות) —
     * בכל סדר. "ספר (1).pdf" · "ספר - עותק.pdf" · "מדען איך להיות.pdf" ~ "איך_להיות-מדען.pdf".
     */
    public static boolean similarNames(String a, String b) {
        if (!ext(a).equals(ext(b))) return false;
        java.util.Set<String> x = nameWords(a), y = nameWords(b);
        if (x.isEmpty() || y.isEmpty()) return a.equalsIgnoreCase(b);
        return x.size() <= y.size() ? y.containsAll(x) : x.containsAll(y);
    }

    private Text() { }

    // ---------- נרמול ----------

    /** אותיות קטנות, בלי ניקוד, אותיות סופיות רגילות, גרשיים ורווחים אחידים. */
    public static String norm(String s) {
        // 184: מעבר אחד על התווים, בלי StringBuilder ובלי toLowerCase של כל המחרוזת; שם שאינו משתנה — חוזר כמו שהוא.
        // תו שאינו אנגלית או עברית ושיש לו אות קטנה — הדרך המלאה הישנה (כדי שהתוצאה תהיה זהה בדיוק).
        int n = s.length();
        char[] out = null;
        int j = 0;
        for (int i = 0; i < n; i++) {
            char c = s.charAt(i), m;
            if (c < 0x80) {
                m = c >= 'A' && c <= 'Z' ? (char) (c + 32) : c == '`' ? '\'' : c;
                if (c < ' ' && c != ' ' && Character.isWhitespace(c)) m = ' ';
            } else if (c >= 0x0591 && c <= 0x05C7) {
                m = 0;
            } else if (c >= 0x05D0 && c <= 0x05EA) {
                switch (c) {
                    case 'ך': m = 'כ'; break;
                    case 'ם': m = 'מ'; break;
                    case 'ן': m = 'נ'; break;
                    case 'ף': m = 'פ'; break;
                    case 'ץ': m = 'צ'; break;
                    default: m = c;
                }
            } else {
                return normSlow(s);
            }
            if (out == null) {
                if (m == c) continue;
                out = new char[n];
                s.getChars(0, i, out, 0);
                j = i;
            }
            if (m != 0) out[j++] = m;
        }
        return out == null ? s : new String(out, 0, j);
    }

    static String normSlow(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 0x0591 && c <= 0x05C7) continue;
            if (c == '\u200E' || c == '\u200F' || c == '\uFEFF') continue;
            if (c != ' ' && (Character.isWhitespace(c) || Character.isSpaceChar(c))) c = ' ';
            switch (c) {
                case 'ך': c = 'כ'; break;
                case 'ם': c = 'מ'; break;
                case 'ן': c = 'נ'; break;
                case 'ף': c = 'פ'; break;
                case 'ץ': c = 'צ'; break;
                case '״': case '”': case '“': c = '"'; break;
                case '׳': case '’': case '‘': case '`': c = '\''; break;
                default: break;
            }
            sb.append(c);
        }
        String out = sb.toString().toLowerCase(Locale.ROOT);
        return out.equals(s) ? s : out;              // 184: בלי שינוי — אותה מחרוזת (במיליון קבצים: פחות זיכרון)
    }

    /** כתיב חסר: בלי יו"ד ווי"ו באמצע מילה, כדי ש"היגיון" ו"הגיון" יתלכדו. */
    public static String loose(String norm) {
        StringBuilder sb = new StringBuilder(norm.length());
        for (int i = 0; i < norm.length(); i++) {
            char c = norm.charAt(i);
            if ((c == 'י' || c == 'ו') && i > 0 && i < norm.length() - 1
                    && heb(norm.charAt(i - 1)) && heb(norm.charAt(i + 1))) continue;
            sb.append(c);
        }
        return sb.length() == norm.length() ? norm : sb.toString();      // 184: בלי שינוי — אותה מחרוזת
    }

    private static boolean heb(char c) {
        return c >= 'א' && c <= 'ת';
    }

    /** אות שימוש בתחילת מילה, להרחבת השאילתה בלבד. */
    public static String withoutPrefixLetter(String q) {
        if (q.length() < 3) return null;
        char c = q.charAt(0);
        return (c == 'ו' || c == 'ה' || c == 'ב' || c == 'כ' || c == 'ל' || c == 'מ' || c == 'ש')
                ? q.substring(1) : null;
    }

    // ---------- תבניות ----------

    public static final int CONTAINS = 0, PREFIX = 1, SUFFIX = 2, EXACT = 3, GLOB = 4, WORD = 5, REGEX = 6;   // REGEX — מבנה 297: מרכאות

    public static final class Pattern {
        public int kind;
        public String text;
        public char[] glob;
        java.util.regex.Pattern rx;

        public boolean matches(String name) {
            switch (kind) {
                case CONTAINS: return name.contains(text);
                case PREFIX: return name.startsWith(text);
                case SUFFIX: return name.endsWith(text);
                case EXACT: return name.equals(text);
                case WORD: return atWordStart(name, text);
                case REGEX: return rx.matcher(name).find();
                default: return globMatch(name, glob);
            }
        }
    }

    /** המחרוזת מופיעה בתחילת מילה בשם (ולא באמצע מילה). */
    static boolean atWordStart(String name, String text) {
        for (int i = name.indexOf(text); i >= 0; i = name.indexOf(text, i + 1))
            if (i == 0 || !Character.isLetterOrDigit(name.charAt(i - 1))) return true;
        return false;
    }

    /** תבנית לגרסה בלי אות שימוש: רק בתחילת מילה, כדי ש"מוגה" לא ימצא "יוגה" (77). */
    public static Pattern wordStart(String q) {
        Pattern p = new Pattern();
        p.kind = WORD;
        p.text = q;
        return p;
    }

    /**
     * מילה בחיפוש — הכלל האחד. מבנה 297 (המשתמש, 20.11.2026, על מ0.52: "כוכבית אומרת כל תו, כך שפירושו של vfm*2 — מצא מחרוזת
     * vfm, אחר כך כל תו שהוא, ואחר כך 2. מה אכפת לי מה הסיום" · "תחילת מילה או סיום — במרכאות מצוין, לא בכוכבית"):
     * הכוכבית רק ממלאת (כל תווים, גם אף אחד), ? — תו אחד; המילה נמצאת בכל מקום בשם. מרכאות בתחילת המילה ("דוח) — רק בתחילת מילה
     * בשם; בסוף (pdf") — רק בסוף מילה; בשני הצדדים — מילה שלמה. מרכאות באמצע (דו"ח) — תו כמו כל תו. מחליף את 279 ("כמו בפקודות":
     * הכוכבית עיגנה את תחילת השם ואת סופו — ו־vfm*2 לא מצא את vfm-m0.52.apk). בפקודות הקבצים — לא השתנה (wild: השם כולו).
     */
    public static Pattern find(String word) {
        String q = word;
        boolean ws = false, we = false;
        if (q.length() > 1 && quote(q.charAt(0))) {
            ws = true;
            q = q.substring(1);
        }
        if (q.length() > 1 && quote(q.charAt(q.length() - 1))) {
            we = true;
            q = q.substring(0, q.length() - 1);
        }
        boolean wild = q.indexOf('*') >= 0 || q.indexOf('?') >= 0;
        if (!ws && !we) return compile(wild ? "*" + q + "*" : q);           // בלי מרכאות — בכל מקום בשם
        if (ws && !we && !wild) return wordStart(q);
        StringBuilder rx = new StringBuilder();
        if (ws) rx.append("(?<![\\p{L}\\p{N}])");
        for (int i = 0; i < q.length(); i++) {
            char c = q.charAt(i);
            if (c == '*') rx.append(".*");
            else if (c == '?') rx.append('.');
            else rx.append(java.util.regex.Pattern.quote(String.valueOf(c)));
        }
        if (we) rx.append("(?![\\p{L}\\p{N}])");
        Pattern p = new Pattern();
        p.kind = REGEX;
        p.rx = java.util.regex.Pattern.compile(rx.toString(), java.util.regex.Pattern.DOTALL);
        return p;
    }

    /** מבנה 297: מרכאות שמסמנות תחילת מילה או סופה — " ומה שהמקלדת שמה במקומן (״ “ ”). */
    public static boolean quote(char c) {
        return c == '"' || c == '\u05F4' || c == '\u201C' || c == '\u201D';
    }

    /**
     * מבנה 279: תבנית שם של קובץ בפקודות (בסגנון bat) — אותה התאמה של החיפוש (compile), במקום אחד: * ו־?, בלי הבדל בין גדולות
     * לקטנות, השם כולו; "*.*" — כל הקבצים, גם בלי סיומת; בלי תו כללי — השם בדיוק. (עד כאן — מימוש שני, בביטוי רגולרי, ב־Parse.)
     */
    public static boolean wild(String pattern, String name) {
        if (pattern.equals("*.*")) return true;  // תחביר
        String p = pattern.toLowerCase(Locale.ROOT), n = name.toLowerCase(Locale.ROOT);
        if (p.indexOf('*') < 0 && p.indexOf('?') < 0) return n.equals(p);
        return compile(p).matches(n);
    }

    public static Pattern compile(String q) {
        Pattern p = new Pattern();
        boolean star = q.indexOf('*') >= 0, question = q.indexOf('?') >= 0;
        if (!star && !question) {
            p.kind = CONTAINS;
            p.text = q;
            return p;
        }
        if (!question) {
            if (q.equals("*")) {
                p.kind = CONTAINS;
                p.text = "";
                return p;
            }
            boolean lead = q.startsWith("*"), trail = q.length() > 1 && q.endsWith("*");
            String core = q.substring(lead ? 1 : 0, trail ? q.length() - 1 : q.length());
            if (core.indexOf('*') < 0) {
                p.text = core;
                p.kind = lead && trail ? CONTAINS : lead ? SUFFIX : trail ? PREFIX : EXACT;
                return p;
            }
        }
        p.kind = GLOB;
        p.glob = q.toCharArray();
        return p;
    }

    /** התאמה איטרטיבית, בלי ביטוי רגולרי. */
    public static boolean globMatch(String s, char[] p) {
        int si = 0, pi = 0, starP = -1, starS = 0;
        while (si < s.length()) {
            if (pi < p.length && (p[pi] == '?' || p[pi] == s.charAt(si))) {
                si++;
                pi++;
            } else if (pi < p.length && p[pi] == '*') {
                starP = pi++;
                starS = si;
            } else if (starP >= 0) {
                pi = starP + 1;
                si = ++starS;
            } else {
                return false;
            }
        }
        while (pi < p.length && p[pi] == '*') pi++;
        return pi == p.length;
    }

    /**
     * מבנה 281 (אפיון החיפוש, 8 — "הכל"): מה לסמן בשם שבשורה — כל המילים שחיפשו, לא רק הראשונה. מילה עם "|" — כל חלופה; עם תו כללי —
     * החלקים שבין התווים ("דוח*2026" — "דוח" ו"2026"); מסננים ("סוג:pdf") ו"בלי" ("-טיוטה") — לא מסמנים. חוזר: זוגות [התחלה, סוף),
     * בסדר, בלי חפיפות. שם שהנרמול משנה את אורכו — כלום (המקומות לא היו נכונים).
     */
    public static int[] marks(String shown, String queryNorm) {
        if (shown == null || queryNorm == null) return new int[0];
        String norm = norm(shown);
        if (norm.length() != shown.length()) return new int[0];
        boolean[] on = new boolean[norm.length()];
        boolean inValue = false;                                            // מבנה 283: "רשימה:«א ב»" — גם המילים שבתוך «» אינן לסימון
        for (String w : norm(queryNorm).trim().split("\\s+")) {             // מנורמל כבר במסך; כאן — ליתר ביטחון (אותיות סופיות)
            if (inValue) {
                if (w.indexOf('»') >= 0) inValue = false;
                continue;
            }
            if (w.isEmpty() || w.startsWith("-") || w.indexOf(':') >= 0) {
                int q = w.indexOf('«');
                if (w.indexOf(':') >= 0 && q >= 0 && w.indexOf('»', q) < 0) inValue = true;
                continue;
            }
            for (String alt : w.split("\\|"))
                for (String piece : alt.split("[*?]")) {
                    while (!piece.isEmpty() && quote(piece.charAt(0))) piece = piece.substring(1);   // מבנה 297: מרכאות בקצה — לא לסימון
                    while (!piece.isEmpty() && quote(piece.charAt(piece.length() - 1))) piece = piece.substring(0, piece.length() - 1);
                    if (piece.isEmpty()) continue;
                    int i = norm.indexOf(piece);
                    if (i >= 0) for (int k = i; k < i + piece.length(); k++) on[k] = true;
                }
        }
        java.util.List<Integer> out = new java.util.ArrayList<Integer>();
        for (int i = 0; i < on.length; i++)
            if (on[i] && (i == 0 || !on[i - 1])) {
                int j = i;
                while (j < on.length && on[j]) j++;
                out.add(i);
                out.add(j);
            }
        int[] r = new int[out.size()];
        for (int i = 0; i < r.length; i++) r[i] = out.get(i);
        return r;
    }

    // ---------- דירוג ----------

    /** 0 מתחיל בשאילתה · 1 מילה מתחילה בה · 2 מכיל · 3 אחר */
    public static int rank(String name, String q) {
        if (q.isEmpty()) return 3;
        if (name.startsWith(q)) return 0;
        int i = name.indexOf(q);
        if (i < 0) return 3;
        char before = name.charAt(i - 1);
        return (before == ' ' || before == '-' || before == '_' || before == '.'
                || before == '(' || before == '[') ? 1 : 2;
    }
}
