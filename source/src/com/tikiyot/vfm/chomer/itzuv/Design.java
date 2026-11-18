package com.tikiyot.vfm;

// 1.12.0: עיצוב עם שם.
// מבנה 266 (המשתמש, 18.11.2026: "אולי את העיצובים הקבועים לשים במקום אחד ואת המשתנים בשני" · "תתחיל נקי"): הקבועים — כאן, בקוד
// בלבד (builtInText); אין להם קובץ, ואי אפשר לקלקל אותם. מה שמשתנה — אינו עיצוב שלם: עיצוב קבוע כבסיס, ועליו רק מה ששונה
// ("המראה" — מה ש"עצב" שומר). קובצי העיצוב של פעם ("<שם>.עיצוב.txt") והחלון הישן שערך אותם — ירדו; הקבצים שבכונן לא נמחקו.
// מבנה 231 (המשתמש, 17.11.2026, על הפרדת העיצוב: "כן"): ירד מהמסכים לחומר — העיצוב עצמו, הקריאה והכתיבה; הציור (Looks)
// והחלון (DesignUi) — במסכים. כך גם הפקודות ("עצב") יכולות להגיע אליו.
// אחראי: העיצובים הקבועים, הירושה (התוכנה ← האופן ← הרשימה) והמראה שמעליהם. הציור — Looks (במסכים); הפקודה — ShapeCmds.
// שורה לא מובנת בטקסט של עיצוב — מתעלמים ממנה (לא נשבר).

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class Design {

    /** העיצובים הקבועים — תמיד זמינים, בקוד בלבד (מבנה 266). */
    /** מבנה 240 (המשתמש, 17.11.2026: "מתאים" — שמות שאומרים מה רואים). */
    static final String DEFAULT = "רשימת קבצים", FILES = "מנהל קבצים עם צלמיות", WORDS = "מילים וצבע";  // בכונן
    /** מבנה 240: השמות הקודמים — עדיין מובנים (רשימה, הבית, קובץ שנערך), ומה שנכתב מעכשיו — בשם החדש. */
    static final String[][] OLD_NAMES = {{"עיצוב ברירת מחדל", DEFAULT}, {"תוכנת ניהול קבצים", FILES}};  // בכונן

    /** מבנה 240: השם של היום, לשם שנכתב פעם (שם ישן של מובנה ← החדש; אחר — כמו שהוא). */
    static String current(String name) {
        if (name == null) return null;
        String n = name.trim();
        for (String[] o : OLD_NAMES) if (o[0].equals(n)) return o[1];
        return n;
    }

    /** יש עיצוב בשם הזה (קבוע, גם בשמו הקודם). */
    static boolean known(String name) {
        return name != null && builtIn(name);
    }

    /** בהגדרות הכלליות של האופן: העיצוב של מסך הבית. */
    static final String HOME_KEY = "עיצוב מסך הבית";  // תחביר

    // ---------- התחביר של הקובץ ----------
    static final String S_TITLE = "כותרת המסך", S_MID = "אמצע";  // תחביר
    static final String K_STYLE = "סגנון", GRID = "רשת", ROWS = "שורות", K_COLS = "טורים", K_ICON = "צלמית",  // תחביר
            K_GAP = "ריווח", K_SHOW_ICON = "הצג צלמית", K_AROUND = "סביב הצלמית", K_TINT = "צבע רקע לפריט",  // תחביר
            K_SHOW_DATA = "הצג נתונים", K_DATA_AT = "מקום הנתונים", BELOW = "מתחת", SIDE = "בצד",  // תחביר
            K_FIT = "התאם למקום", K_MIN = "גודל מזערי", K_LINES = "שורות לשם";  // תחביר
    /**
     * מבנה 256 (מה שעלה מהצילומים, קבוצה ב — "תבנה מה שהכי קל ומתאים ונראה לך"): להסתיר את תווית הסוג (ברשת — הפס; בשורות —
     * הצלמית במקום התווית) · פריט מוסתר (שמו מתחיל בנקודה) — חיוור.
     */
    static final String K_SHOW_TAG = "הצג תווית", K_FADE = "מוסתר חיוור";  // תחביר
    /** מבנה 258: הצלמית והריווח — אחוזים מגודל השם, בשורות וברשת (iconPx, padPx). */
    static final String NONE = "בלי", FRAME = "מסגרת", FILL = "ריבוע צבעוני";  // תחביר
    /** מבנה 256: סביב הצלמית — עיגול צבעוני (כמו "ריבוע צבעוני", בפינות עגולות לגמרי). */
    static final String CIRCLE = "עיגול";  // תחביר
    static final String T_NAME = "שם", T_DATA = "נתונים", K_SIZE = "גודל", K_WEIGHT = "משקל", K_FONT = "גופן";  // תחביר
    /** מבנה 239 (המשתמש, 17.11.2026: "כן" — סוג כתב רביעי): כותרת של קבוצה בתוך רשימה ("צור כותרת"). */
    static final String T_GROUP = "קבוצה";  // תחביר
    /** מבנה 249 (המשתמש, 17.11.2026: "להקטין ידנית ולהגדיל את השם"): הכתב של תווית הסוג ("ידנית", "פקודה"…). */
    static final String T_TAG = "תווית";  // תחביר
    /** מבנה 250 (המשתמש, 17.11.2026: "אפשרות לקבוע נוסח — ידנית · סביבת עבודה · ידנית כסביבת עבודה"): הנוסח של תווית הסוג. */
    static final String K_WORDING = "נוסח";  // תחביר

    /** מבנה 250: הסוגים שיש להם תווית — בשמם המלא (כמו בפקודה: "רשימה ידנית"); התווית עצמה — מהמילון. */
    static String[] tagKinds() {
        String[] t = {Words.Tags.MANUAL, Words.Tags.SMART, Words.Tags.FOLDER, Words.Tags.CMD, Words.Tags.LINK, Words.Tags.INTERNAL, Words.Tags.CARD};
        String[] out = new String[t.length];
        for (int i = 0; i < t.length; i++) out[i] = kindName(t[i]);
        return out;
    }

    /** "רשימה\nידנית" ← "רשימה ידנית"; "↪\nקישור" ← "קישור". */
    static String kindName(String tag) {
        return tag.replace("↪", "").replace('\n', ' ').trim();
    }

    /** מבנה 250: נוסח של תווית — עד 30 תווים, בלי "·" ובלי שורה חדשה (הם מפרידים את המראה). null — תקין. */
    static String wordingError(String kind, String v) {
        if (!java.util.Arrays.asList(tagKinds()).contains(kind)) return Words.DesignW.noKind(kind, java.util.Arrays.asList(tagKinds()));
        if (v.length() > 30 || v.contains("·") || v.contains("\n") || v.contains("=")) return Words.DesignW.badWording(v);
        return null;
    }
    static final String YES = "כן", NO = "לא", SYSTEM = "מערכת";  // תחביר
    static final String W_LIGHT = "דק", W_REG = "רגיל", W_MED = "בינוני", W_BOLD = "מודגש";  // תחביר
    static final String P_SEP = "·";  // תחביר

    /** סגנון טקסט: גודל (sp), משקל (100–900), גופן ("מערכת" או שם קובץ בתיקיית "גופנים"). */
    static final class Text {
        float size;
        int weight;
        String font;

        Text(float size, int weight, String font) {
            this.size = size;
            this.weight = weight;
            this.font = font;
        }

        Text copy() {
            return new Text(size, weight, font);
        }
    }

    String name = DEFAULT;
    /** האזור האמצעי (הרשימות). */
    boolean grid;
    int cols = 1;
    /** רשת: אחוז מרוחב האריח. שורות: אחוז מגודל הטקסט של השם (החלטה 4). */
    int icon = 340;
    /** רשת: אחוז מרוחב האריח. שורות: אחוז מגודל הטקסט של השם — הריווח למעלה ולמטה. */
    int gap = 50;
    /** מבנה 267: מראה הפריט — כל המפתחות בטבלה אחת (ItemKeys); כאן רק הערכים. */
    final ItemKeys items = new ItemKeys();
    Text title = new Text(12, 400, SYSTEM), nameT = new Text(16, 400, SYSTEM), data = new Text(11, 400, SYSTEM);
    /** מבנה 239: הכתב של כותרת קבוצה; null — כמו השם (כמו שהיה עד כאן). */
    Text group;

    /**
     * מבנה 249: הכתב של תווית הסוג — בפס שמתחת לצלמית (ברשת) ובתווית שבמקום הצלמית (בשורות); null — לפי גודל הצלמית (כמו
     * שהיה עד כאן).
     */
    Text tag;
    /** מבנה 250: נוסח משלך לתווית — סוג (בשמו המלא) ← הנוסח; מה שאין כאן — מהמילון. */
    final Map<String, String> wording = new LinkedHashMap<String, String>();

    /** מבנה 250: התווית של הסוג (tag — כמו במילון): הנוסח שנקבע, או null (המילון). */
    String wordingOf(String tag) {
        return tag == null ? null : wording.get(kindName(tag));
    }

    /** מבנה 249: הכתב של תווית הסוג כמו שייראה — שלה, או הכתב הקטן שהתווית מתחילה ממנו (לבדיקה ולתוסף). */
    Text tagText() {
        return tag != null ? tag : new Text(11, 700, SYSTEM);
    }

    /** מבנה 239: הכתב של כותרת קבוצה — שלה, או של השם. */
    Text groupText() {
        return group != null ? group : nameT;
    }

    /** מבנה 234: עותק — לשינוי מעל עיצוב שבזיכרון (המקור נשאר כמו שהוא). */
    Design copy() {
        Design d = new Design();
        d.name = name;
        d.grid = grid;
        d.cols = cols;
        d.icon = icon;
        d.gap = gap;
        d.items.copyFrom(items);
        d.title = title.copy();
        d.nameT = nameT.copy();
        d.data = data.copy();
        d.group = group == null ? null : group.copy();
        d.tag = tag == null ? null : tag.copy();
        d.wording.putAll(wording);
        return d;
    }

    // ---------- מבנה 234: המראה של רשימה — שינויים מעל העיצוב שלה, והירושה ----------

    /** בין השורות של המראה (Store.P_LOOK). */
    static final String LOOK_SEP = " " + P_SEP + " ";
    /** בשורות המראה: הכתב של כותרת המסך ("כותרת: גודל = 15"); השאר — כמו בקבוצה "אמצע" של הקובץ. */
    static final String T_TITLE = "כותרת";  // תחביר

    /** השינויים שבמראה (look) — על העיצוב הזה. שורה לא מובנת — מדולגת, כמו בקובץ. */
    void overlay(String look) {
        if (look == null) return;
        for (String part : look.split(java.util.regex.Pattern.quote(P_SEP))) {
            int eq = part.indexOf('=');
            if (eq <= 0) continue;
            String key = part.substring(0, eq).trim(), value = part.substring(eq + 1).trim();
            if (value.isEmpty()) continue;
            try {
                int c = key.indexOf(':');
                if (c > 0 && key.substring(0, c).trim().equals(T_TITLE)) text(title, key.substring(c + 1).trim(), value);
                else mid(key, value);
            } catch (RuntimeException ignored) {                             // ערך לא מובן — נשאר מהעיצוב
            }
        }
    }

    /**
     * המראה של רשימה: מלמעלה למטה — כל התוכנה, האופן, ואז מהרשימה העליונה (שמתחת לבית) עד הרשימה הזו (מבנה 234, 236). מי שבחר
     * עיצוב — מתחילים ממנו מחדש; מה שהוא שינה (המראה) — עליו. null — לא נקבע כלום בדרך (המסך מצייר כמו תמיד). הדרך למעלה —
     * דרך הרשימה הראשונה שהרשימה בה.
     */
    static Design forList(Store t, long id) {
        return forList(t, id, true);
    }

    /** מבנה 243: מה שהרשימה הייתה מקבלת בלי מה שנקבע לה עצמה (לתוסף העיצוב — "כמו שמעליה"); null — לא נקבע כלום בדרך. */
    static Design forListAbove(Store t, long id) {
        return forList(t, id, false);
    }

    private static Design forList(Store t, long id, boolean self) {
        List<Long> chain = chain(t, id);
        if (!self && !chain.isEmpty()) chain.remove(chain.size() - 1);
        Design d = above(t, null);
        for (long x : chain) d = fold(d, t.prop(x, Store.P_DESIGN), t.prop(x, Store.P_LOOK), null);
        return d;
    }

    /**
     * הדרך של הירושה — מהרשימה העליונה (שמתחת לבית) עד הרשימה עצמה; למעלה — דרך הרשימה הראשונה שהרשימה בה. במקום אחד: העיצוב
     * (כאן) והצבעים (Palette, מבנה 269) יורשים באותה דרך.
     */
    static List<Long> chain(Store t, long id) {
        List<Long> chain = new ArrayList<Long>();
        java.util.Set<Long> seen = new java.util.HashSet<Long>();
        for (long at = id; at != 0 && seen.add(at); ) {
            if ("1".equals(t.prop(at, Store.P_HOME))) break;                  // הבית — לבד (העיצוב של מסך הבית)
            chain.add(0, at);
            List<Long> up = t.listsOf(at);
            at = up.isEmpty() ? 0 : up.get(0);
        }
        return chain;
    }

    /**
     * מבנה 236: מסך הבית — כל התוכנה, האופן, ואז הבית עצמו (העיצוב שלו — מהמסד, או מההגדרה הישנה "עיצוב מסך הבית"). לא נקבע כלום —
     * רשת בלי מסגרת לצלמית (כמו האריחים בבית הישן).
     */
    static Design forHome(Store t, long home) {
        Design start = get(FILES).copy();
        start.items.put(K_AROUND, NONE);
        String own = t.prop(home, Store.P_DESIGN);
        if (own == null || own.trim().isEmpty()) own = homeName();
        Design d = fold(above(t, start), own, t.prop(home, Store.P_LOOK), start);
        return d != null ? d : start;
    }

    /** מבנה 241: מה שהאופן נותן לרשימות שלו (כל התוכנה, ואז האופן); null — לא נקבע כלום. */
    static Design forMode(Store t) {
        return above(t, null);
    }

    /** מבנה 241: מה שכל התוכנה נותנת; null — לא נקבע כלום. */
    static Design forProgram() {
        String[] all = programProps();
        return fold(null, all[0], all[1], null);
    }

    /** מה שמעל הרשימות: של כל התוכנה (הבית של אופן התפריטים), ושל האופן (הבית שלו). start — ממה מתחילים כשיש רק מראה. */
    private static Design above(Store t, Design start) {
        String[] all = programProps();
        Design d = fold(null, all[0], all[1], start);
        long h = DbPaths.home(t);
        return h == 0 ? d : fold(d, t.prop(h, Store.P_ALL_DESIGN), t.prop(h, Store.P_ALL_LOOK), start);
    }

    /** שלב אחד בירושה: עיצוב שנבחר — מתחילים ממנו; מראה — עליו (אין עדיין כלום — מ־start, או מברירת המחדל). */
    private static Design fold(Design d, String design, String look, Design start) {
        if (design != null && !design.trim().isEmpty()) d = get(design.trim()).copy();
        if (shapes(look)) {
            if (d == null) d = start != null ? start.copy() : get(DEFAULT).copy();
            d.overlay(look);
        }
        return d;
    }

    /**
     * מבנה 268 (ממצא 64): האם במראה יש משהו חוץ מצבעים. מראה שכולו צבעים ("מראה = כהה", "צבע: רקע = #000000") אינו קובע עיצוב —
     * רק מה שנכתב משתנה: מי שביקש כהה מקבל כהה, והשורות נשארות כמו שהיו. הצבעים הולכים בדרך משלהם (Palette).
     */
    static boolean shapes(String look) {
        if (look == null) return false;
        for (String part : look.split(java.util.regex.Pattern.quote(P_SEP))) {
            int eq = part.indexOf('=');
            if (eq <= 0 || part.substring(eq + 1).trim().isEmpty()) continue;
            String key = part.substring(0, eq).trim();
            if (!key.equals(Palette.K_MODE) && !key.startsWith(Palette.K_COLOR + ":")) return true;
        }
        return false;
    }

    private static String[] program = {null, null};
    private static long programStamp = -1;

    /** אחרי "עצב תוכנה": לקרוא מחדש (המסד יכול להשתנות בלי שתאריך הקובץ זז — יומן הכתיבה של המסד). */
    static synchronized void forgetProgram() {
        programStamp = -1;
    }

    /** העיצוב והמראה של כל התוכנה — מהבית של אופן התפריטים; נקרא מחדש כשהמסד שלו השתנה, או אחרי "עצב תוכנה". */
    static synchronized String[] programProps() {
        try {
            File db = Db.dbFile(Db.app, Db.MENUS_MODE);
            if (Db.app == null || !db.isFile()) return new String[]{null, null};
            if (db.lastModified() != programStamp) {
                program = Db.read(Db.app, Db.MENUS_MODE, new Db.Use<String[]>() {
                    @Override public String[] run(SqlStore s) {
                        long h = DbPaths.home(s);
                        return h == 0 ? new String[]{null, null} : new String[]{s.prop(h, Store.P_ALL_DESIGN), s.prop(h, Store.P_ALL_LOOK)};
                    }
                });
                programStamp = db.lastModified();
            }
            return program;
        } catch (Throwable e) {
            Log.e("Design.programProps", e);
            return new String[]{null, null};
        }
    }

    /**
     * מבנה 235: הערך הזה למפתח הזה של המראה ("טורים", "שם: גודל"…) — null: תקין; אחרת — למה לא, במילים של המשתמש. הטווחים —
     * אלה של הקובץ (למעלה); גופן — "מערכת", או קובץ בתיקיית "גופנים" של האופן (או מסלול מלא).
     */
    static String lookError(String key, String v) {
        int c = key.indexOf(':');
        String what = c > 0 ? key.substring(c + 1).trim() : key;
        if (c > 0 && key.substring(0, c).trim().equals(K_WORDING)) return wordingError(what, v);   // מבנה 250
        String[] one = choices(key);                                            // מבנה 254: ערך אחד מתוך כמה
        if (one != null) return java.util.Arrays.asList(one).contains(v) ? null : Words.DesignW.notOneOf(v, java.util.Arrays.asList(one));
        try {
            if (key.equals(K_COLS)) return range(v, COLS);
            if (key.equals(K_ICON)) return range(v, ICON);
            if (key.equals(K_GAP)) return range(v, GAP);
            if (shapeRange(key) != null) return range(v, shapeRange(key));       // מבנה 254
            if (what.equals(K_SIZE)) return range(v, SIZE);
            if (what.equals(K_WEIGHT))
                return v.equals(W_LIGHT) || v.equals(W_REG) || v.equals(W_MED) || v.equals(W_BOLD) ? null : range(v, WEIGHT);
            if (what.equals(K_FONT)) return v.equals(SYSTEM) || fontFile(v) != null ? null : Words.DesignW.noFont(v);
        } catch (NumberFormatException e) {
            return Words.DesignW.notNumber(v);
        }
        return Words.DesignW.notNumber(v);
    }

    private static String range(String v, int[] r) {
        int n = num(v);
        return n < r[0] || n > r[1] ? Words.DesignW.outOfRange(v, r[0], r[1]) : null;
    }

    /** קובץ הגופן בשם הזה: מסלול מלא, או בתיקיית "גופנים" של האופן; null — אין. */
    static File fontFile(String font) {
        if (font == null || font.trim().isEmpty() || SYSTEM.equals(font)) return null;
        File f = font.startsWith("/") ? new File(font) : new File(new File(Paths.work(), Paths.FONTS), font);
        return f.isFile() ? f : null;
    }

    /** המראה (P_LOOK) אחרי קביעה של key = value; value ריק — השורה יוצאת (חוזרים לרשת). הסדר נשמר. */
    static String withLook(String look, String key, String value) {
        Map<String, String> m = lookMap(look);
        if (value == null || value.trim().isEmpty()) m.remove(key);
        else m.put(key, value.trim());
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : m.entrySet()) {
            if (sb.length() > 0) sb.append(LOOK_SEP);
            sb.append(e.getKey()).append(" = ").append(e.getValue());
        }
        return sb.toString();
    }

    /** מבנה 243: שורות המראה — מפתח ← ערך, לפי הסדר (גם לתוסף העיצוב: מה שנקבע כאן). */
    static Map<String, String> lookMap(String look) {
        Map<String, String> m = new LinkedHashMap<String, String>();
        if (look != null)
            for (String part : look.split(java.util.regex.Pattern.quote(P_SEP))) {
                int eq = part.indexOf('=');
                if (eq > 0) m.put(part.substring(0, eq).trim(), part.substring(eq + 1).trim());
            }
        return m;
    }

    /** העיצובים שאפשר לבחור — הקבועים. */
    static List<String> names() {
        return new ArrayList<String>(java.util.Arrays.asList(DEFAULT, FILES, WORDS));
    }

    /** מבנה 243: הגופנים שבתיקיית "גופנים" של האופן (השמות, כמו שנכתבים ב"עצב"). */
    static List<String> fonts() {
        List<String> out = new ArrayList<String>();
        File[] kids = new File(Paths.work(), Paths.FONTS).listFiles();
        if (kids != null) for (File k : kids) if (k.isFile()) out.add(k.getName());
        java.util.Collections.sort(out);
        return out;
    }

    /** סימן לזיהוי שינוי (המסך נבנה מחדש רק כשהמראה השתנה). */
    String signature() {
        return grid + "|" + cols + "|" + icon + "|" + gap + "|" + items.signature() + nameT.size + nameT.weight + nameT.font + "|" + data.size
                + data.weight + data.font + "|" + (group == null ? "" : group.size + "" + group.weight + group.font)
                + "|" + (tag == null ? "" : tag.size + "" + tag.weight + tag.font) + "|" + wording;
    }

    // ---------- קריאה ----------

    static Design parse(String name, String text) {
        Design d = new Design();
        d.name = name;
        String section = "";
        for (String raw : (text == null ? "" : text).replace("﻿", "").split("\r\n|\r|\n")) {
            String t = raw.trim();
            if (t.isEmpty() || t.startsWith(";") || t.startsWith("#")) continue;
            if (t.startsWith("[") && t.endsWith("]")) {
                section = t.substring(1, t.length() - 1).trim();
                continue;
            }
            int eq = t.indexOf('=');
            if (eq <= 0) continue;
            String key = t.substring(0, eq).trim(), value = t.substring(eq + 1).trim();
            int sc = value.indexOf(';');                                     // הערה בסוף השורה
            if (sc >= 0) value = value.substring(0, sc).trim();
            if (value.isEmpty()) continue;
            try {
                if (S_TITLE.equals(section)) d.text(d.title, key, value);
                else if (S_MID.equals(section)) {
                    d.mid(key, value);
                }
            } catch (RuntimeException ignored) {                             // ערך לא מובן — נשאר מברירת המחדל
            }
        }
        return d;
    }

    /** מבנה 260 (המשתמש, 18.11.2026 — "א"): מספר הטורים הרגיל של סגנון — בא עם החלפת סגנון ב"עצב" (ShapeCmds.shapeOn). */
    static int usualCols(boolean grid) {
        return grid ? 3 : 1;
    }

    /** מבנה 260: הסגנון שנראה בשלב אחד של הירושה — מה שמעל (null — כלום), העיצוב שנבחר, והמראה שעליו. בלי כלום — שורות (כך המסך מצייר). */
    static boolean gridOf(Design above, String design, String look) {
        Design d = fold(above == null ? null : above.copy(), design, look, null);
        return d != null && d.grid;
    }

    /** מבנה 258: הריווח בפיקסלים — אחוז מגודל השם (namePx — גודל השם בפיקסלים), בשורות וברשת; least — המעט שנשאר תמיד. */
    int padPx(float namePx, int least) {
        return Math.max(least, (int) (namePx * gap / 100f));
    }

    /** מבנה 258: הצלמית בפיקסלים — אחוז מגודל השם, בשורות וברשת; ברשת — לא יותר ממה שנכנס באריח (הרוחב, פחות הריווח משני הצדדים). */
    int iconPx(float namePx, int tileW, int pad) {
        int px = (int) (namePx * icon / 100f);
        return grid && tileW > 0 ? Math.max(0, Math.min(px, items.room(tileW, pad))) : px;
    }

    /**
     * מבנה 235: הטווחים — במקום אחד. הקריאה של הקובץ מצמצמת אליהם; הפקודה ("עצב") מסרבת למה שמחוץ להם, ואומרת למה.
     * צלמית וריווח — באחוזים מגודל השם, בשורות וברשת (מבנה 258; עד אז ברשת — מרוחב האריח).
     */
    static final int[] COLS = {1, 8}, ICON = {0, 1000}, GAP = {0, 200}, SIZE = {6, 60}, WEIGHT = {100, 900};
    /** מבנה 254: שורות לשם, והגודל המזערי של שם שמוקטן כדי להיכנס (התאם למקום). */
    static final int[] LINES = ItemKeys.LINES_RANGE, MIN = ItemKeys.MIN_RANGE;

    /**
     * מבנה 254 (המשתמש, 18.11.2026, על מה שעלה מהצילומים: "תבנה מה שהכי קל ומתאים ונראה לך" — קבוצה א: מה שכבר בעיצוב ואין לו
     * מתג): המפתחות של "אמצע" שנקבעים ב"עצב" מעבר לטורים, ריווח וצלמית — לפי הסדר. המתג — שם המפתח, עם "-" במקום רווח.
     */
    static final String[] SHAPE_KEYS = ItemKeys.with(K_STYLE, true);   // מבנה 267: הסגנון, ואחריו הטבלה של ItemKeys
    /** מבנה 255: לכל מפתח — שם קבוע ללוח העיצוב (מה הוא מצייר), באותו סדר. בערכים — הראשון הוא ה"כן" (רשת, כן, מתחת). */
    static final String[] SHAPE_IDS = ItemKeys.with("grid", false);

    /** מבנה 254: הערכים שהמפתח הזה מקבל; null — מספר בטווח (shapeRange). */
    static String[] choices(String key) {
        if (key.equals(K_STYLE)) return new String[]{GRID, ROWS};
        ItemKeys.Def d = ItemKeys.def(key);
        return d == null ? null : d.values;
    }

    /** מבנה 254: הטווח של מפתח מספרי מ־SHAPE_KEYS; null — אין. */
    static int[] shapeRange(String key) {
        ItemKeys.Def d = ItemKeys.def(key);
        return d == null ? null : d.range;
    }

    /** מבנה 254: הערך של מפתח מ־SHAPE_KEYS כמו שהוא בעיצוב הזה (כמו שנכתב בקובץ); null — לא מפתח כזה. */
    String shapeValue(String key) {
        return key.equals(K_STYLE) ? (grid ? GRID : ROWS) : items.get(key);
    }

    private void mid(String key, String v) {
        if (key.equals(K_STYLE)) grid = v.equals(GRID);
        else if (key.equals(K_COLS)) cols = clamp(num(v), COLS[0], COLS[1]);
        else if (key.equals(K_ICON)) icon = clamp(num(v), ICON[0], ICON[1]);
        else if (key.equals(K_GAP)) gap = clamp(num(v), GAP[0], GAP[1]);
        else if (!items.put(key, v)) {
            int c = key.indexOf(':');
            if (c <= 0) return;
            String who = key.substring(0, c).trim(), what = key.substring(c + 1).trim();
            if (who.equals(T_NAME)) text(nameT, what, v);
            else if (who.equals(T_DATA)) text(data, what, v);
            else if (who.equals(T_GROUP)) {                                  // מבנה 239: מתחיל מהשם, ומשתנה רק מה שנכתב
                if (group == null) group = nameT.copy();
                text(group, what, v);
            } else if (who.equals(K_WORDING)) {                             // מבנה 250: הנוסח של תווית
                if (v.isEmpty()) wording.remove(what);
                else wording.put(what, v);
            } else if (who.equals(T_TAG)) {                                  // מבנה 249: מתחיל מכתב קטן, ומשתנה רק מה שנכתב
                if (tag == null) tag = new Text(11, 700, SYSTEM);   // כמו התווית של היום — מודגשת
                text(tag, what, v);
            }
        }
    }

    private void text(Text t, String what, String v) {
        if (what.equals(K_SIZE)) t.size = clamp(num(v), SIZE[0], SIZE[1]);
        else if (what.equals(K_WEIGHT)) t.weight = weight(v);
        else if (what.equals(K_FONT)) t.font = v;
    }

    static int weight(String v) {
        if (v.equals(W_LIGHT)) return 300;
        if (v.equals(W_REG)) return 400;
        if (v.equals(W_MED)) return 500;
        if (v.equals(W_BOLD)) return 700;
        return clamp(num(v), WEIGHT[0], WEIGHT[1]);
    }

    static String weightName(int w) {
        return w <= 300 ? W_LIGHT : w < 500 ? W_REG : w < 600 ? W_MED : W_BOLD;
    }

    private static int num(String v) {
        return Integer.parseInt(v.replace("%", "").trim());
    }

    private static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : v > hi ? hi : v;
    }

    // ---------- החלק הפרטי ----------
    // עד מ0.29 — צלמית וצבע לכל רשימה, לפי המסלול. במסד הצלמית נשמרת ברשימה עצמה; כאן נשאר רק מה ששומר על מה שכתוב
    // בקבצים: הקריאה (load) והשמירה בחזרה לברירת מחדל.

    // ---------- איפה ----------

    static boolean builtIn(String name) {
        name = current(name);                                                 // מבנה 240: גם בשם הקודם
        return DEFAULT.equals(name) || FILES.equals(name) || WORDS.equals(name);
    }

    // ---------- הטופס (1.12.1): כתיבת המראה הכללי ----------

    // ---------- טעינה, עם זיכרון ----------

    private static final Map<String, Design> cache = new java.util.HashMap<String, Design>();

    /** העיצוב הקבוע בשם הזה (גם בשמו הקודם); שם שאין — "רשימת קבצים" (לא נשבר). מה שחוזר — לא משנים: copy(). */
    static synchronized Design get(String name) {
        name = name == null || name.trim().isEmpty() || !builtIn(name) ? DEFAULT : current(name);
        Design d = cache.get(name);
        if (d == null) {
            d = parse(name, builtInText(name));
            cache.put(name, d);
        }
        return d;
    }

    // ---------- הירושה ----------

    /** העיצוב שנבחר למסך הבית; "" — לא נבחר (מסך הבית נשאר כמו שהיה, והרשימות — "עיצוב ברירת מחדל"). */
    static String homeName() {
        return Conf.get(Conf.GENERAL, HOME_KEY, "");
    }

    // ---------- העיצובים המובנים ----------

    static String builtInText(String name) {
        StringBuilder sb = new StringBuilder();
        boolean files = FILES.equals(name), words = WORDS.equals(name);
        sb.append("; ").append(name).append(" — ").append(files ? Words.DesignW.WHY_FILES : words ? Words.DesignW.WHY_WORDS
                : Words.DesignW.WHY_DEFAULT).append('\n');
        sb.append('\n').append('[').append(S_TITLE).append("]\n");
        line(sb, K_SIZE, files ? "15" : "12");
        line(sb, K_WEIGHT, files ? W_BOLD : W_REG);
        line(sb, K_FONT, SYSTEM);
        sb.append('\n').append('[').append(S_MID).append("]\n");
        line(sb, K_STYLE, files ? GRID : ROWS);
        line(sb, K_COLS, files ? "3" : words ? "2" : "1");
        line(sb, K_ICON, files ? "500%" : "340%");                              // מבנה 258: היו 55% ו־8% מרוחב האריח — אותו מראה בטלפון רגיל
        line(sb, K_GAP, files ? "35%" : words ? "40%" : "50%");
        line(sb, K_SHOW_ICON, words ? NO : YES);
        line(sb, K_AROUND, files ? FRAME : FILL);
        line(sb, K_TINT, words ? YES : NO);
        line(sb, T_NAME + ": " + K_SIZE, files ? "14" : words ? "15" : "16");
        line(sb, T_NAME + ": " + K_WEIGHT, files || words ? W_BOLD : W_REG);
        line(sb, T_NAME + ": " + K_FONT, SYSTEM);
        line(sb, T_DATA + ": " + K_SIZE, "11");
        line(sb, T_DATA + ": " + K_WEIGHT, W_REG);
        line(sb, T_DATA + ": " + K_FONT, SYSTEM);
        line(sb, K_FIT, files || words ? YES : NO);
        return sb.toString();
    }

    private static void line(StringBuilder sb, String key, String value) {
        sb.append(key).append(" = ").append(value).append('\n');
    }
}
