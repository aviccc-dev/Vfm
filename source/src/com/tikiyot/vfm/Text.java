package com.tikiyot.vfm;

// טקסט וחיפוש. אחראי: נרמול עברית, התאמת תבניות, דירוג.
// אסור לו: לגעת בקבצים.

import java.util.Locale;

public final class Text {

    /** בידוד כיוון כתיבה (80): קטע עברי/לועזי בתוך שורה לא משבש את סדר השורה. במקום אחד. */
    public static String iso(String s) {
        return "\u2068" + s + "\u2069";
    }

    /** פתיחת בידוד וסגירתו — לבניית שורה בחלקים. */
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

    public static final int CONTAINS = 0, PREFIX = 1, SUFFIX = 2, EXACT = 3, GLOB = 4, WORD = 5;

    public static final class Pattern {
        public int kind;
        public String text;
        public char[] glob;

        public boolean matches(String name) {
            switch (kind) {
                case CONTAINS: return name.contains(text);
                case PREFIX: return name.startsWith(text);
                case SUFFIX: return name.endsWith(text);
                case EXACT: return name.equals(text);
                case WORD: return atWordStart(name, text);
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

    /** תווים כלליים מתייחסים להכלה: "מה*אומר" כמו "*מה*אומר*". */
    public static String widen(String q) {
        if (q.indexOf('*') < 0 && q.indexOf('?') < 0) return q;
        if (!q.startsWith("*")) q = "*" + q;
        if (!q.endsWith("*")) q = q + "*";
        return q;
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
