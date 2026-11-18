package com.tikiyot.vfm;

// 1.12.0: עיצוב עם שם — קובץ טקסט "<שם>.עיצוב.txt" בתיקיית "עיצובים" של האופן (ר' itzuv.md).
// מבנה 231 (המשתמש, 17.11.2026, על הפרדת העיצוב: "כן"): ירד מהמסכים לחומר — העיצוב עצמו, הקריאה והכתיבה; הציור (Looks)
// והחלון (DesignUi) — במסכים. כך גם הפקודות ("עצב") יכולות להגיע אליו.
// אחראי: לקרוא, לכתוב ולמצוא עיצובים. איזה עיצוב לרשימה (שלה; בבית — של מסך הבית) — DbListScreen.designOf.
// החלק הפרטי ([רשימות] — צלמית וצבע לפי מסלול, של המנוע הישן) — רק נקרא ונשמר. בלי ממשק: את המראה מצייר Looks.
// מה שחסר בקובץ — נלקח מ"עיצוב ברירת מחדל"; שורה לא מובנת — מתעלמים ממנה (לא נשבר).

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class Design {

    static final String EXT = ".עיצוב.txt";  // בכונן
    static final String DIR = "עיצובים";  // בכונן
    /** העיצובים המובנים — תמיד זמינים; נכתבים לתיקיית האופן בפעם הראשונה שבוחרים בהם. */
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

    /** מבנה 240: יש עיצוב בשם הזה (מובנה, גם בשם הקודם, או קובץ באופן). */
    static boolean known(String name) {
        return name != null && (builtIn(name) || fileOf(current(name)).isFile());
    }

    /** מבנה 240: הקובץ של מובנה בשמו הקודם (אם נערך פעם) — null אם אין. */
    static File oldFile(String name) {
        for (String[] o : OLD_NAMES) if (o[1].equals(name) && fileOf(o[0]).isFile()) return fileOf(o[0]);
        return null;
    }
    /** בהגדרות הכלליות של האופן: העיצוב של מסך הבית. */
    static final String HOME_KEY = "עיצוב מסך הבית";  // תחביר

    // ---------- התחביר של הקובץ ----------
    static final String S_TITLE = "כותרת המסך", S_MID = "אמצע", S_LISTS = "רשימות";  // תחביר
    static final String K_STYLE = "סגנון", GRID = "רשת", ROWS = "שורות", K_COLS = "טורים", K_ICON = "צלמית",  // תחביר
            K_GAP = "ריווח", K_SHOW_ICON = "הצג צלמית", K_AROUND = "סביב הצלמית", K_TINT = "צבע רקע לפריט",  // תחביר
            K_SHOW_DATA = "הצג נתונים", K_DATA_AT = "מקום הנתונים", BELOW = "מתחת", SIDE = "בצד",  // תחביר
            K_FIT = "התאם למקום", K_MIN = "גודל מזערי", K_LINES = "שורות לשם";  // תחביר
    /**
     * מבנה 256 (מה שעלה מהצילומים, קבוצה ב — "תבנה מה שהכי קל ומתאים ונראה לך"): להסתיר את תווית הסוג (ברשת — הפס; בשורות —
     * הצלמית במקום התווית) · פריט מוסתר (שמו מתחיל בנקודה) — חיוור.
     */
    static final String K_SHOW_TAG = "הצג תווית", K_FADE = "מוסתר חיוור";  // תחביר
    /**
     * מבנה 258 (המשתמש, 18.11.2026: "האם יש דרך לקבוע יחס אחיד לשניהם?" — "כן"): הצלמית והריווח — באחוזים מגודל השם, בשורות
     * וגם ברשת (עד כאן ברשת — מרוחב האריח). קובץ עיצוב שנכתב מעכשיו מסומן "מידות = לפי השם"; קובץ רשת בלי הסימן — מלפני
     * השינוי, ומה שכתוב בו מומר בקריאה (fromTile).
     */
    static final String K_MEASURE = "מידות", BY_NAME = "לפי השם";  // תחביר
    /** רוחב של טלפון רגיל, בנקודות — לפיו מומר קובץ רשת ישן, כך שייראה שם כמו שנראה. */
    private static final int OLD_WIDTH = 380;
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
    static final String P_ICON = "צלמית", P_COLOR = "צבע", P_SEP = "·";  // תחביר

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
    File file;
    /** האזור האמצעי (הרשימות). */
    boolean grid;
    int cols = 1;
    /** רשת: אחוז מרוחב האריח. שורות: אחוז מגודל הטקסט של השם (החלטה 4). */
    int icon = 340;
    /** רשת: אחוז מרוחב האריח. שורות: אחוז מגודל הטקסט של השם — הריווח למעלה ולמטה. */
    int gap = 50;
    boolean showIcon = true;
    String around = FILL;
    boolean tint;
    /** מבנה 256: תווית הסוג נראית (כמו עד כאן); פריט מוסתר — חיוור (לא — כמו עד כאן). */
    boolean showTag = true, fade;
    boolean showData = true;
    boolean dataBelow = true;
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
    boolean fit;
    int min = 11;
    int lines = 2;
    /** החלק הפרטי: מסלול הרשימה באופן ("לימוד/הערות") ← {צלמית, צבע}. */
    final Map<String, String[]> lists = new LinkedHashMap<String, String[]>();

    /** מבנה 234: עותק — לשינוי מעל עיצוב שבזיכרון (המקור נשאר כמו שהוא). */
    Design copy() {
        Design d = new Design();
        d.name = name;
        d.file = file;
        d.grid = grid;
        d.cols = cols;
        d.icon = icon;
        d.gap = gap;
        d.showIcon = showIcon;
        d.around = around;
        d.tint = tint;
        d.showTag = showTag;
        d.fade = fade;
        d.showData = showData;
        d.dataBelow = dataBelow;
        d.title = title.copy();
        d.nameT = nameT.copy();
        d.data = data.copy();
        d.group = group == null ? null : group.copy();
        d.tag = tag == null ? null : tag.copy();
        d.wording.putAll(wording);
        d.fit = fit;
        d.min = min;
        d.lines = lines;
        d.lists.putAll(lists);
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
        List<Long> chain = new ArrayList<Long>();
        java.util.Set<Long> seen = new java.util.HashSet<Long>();
        for (long at = id; at != 0 && seen.add(at); ) {
            if ("1".equals(t.prop(at, Store.P_HOME))) break;                  // הבית — לבד (העיצוב של מסך הבית)
            chain.add(0, at);
            List<Long> up = t.listsOf(at);
            at = up.isEmpty() ? 0 : up.get(0);
        }
        if (!self && !chain.isEmpty()) chain.remove(chain.size() - 1);
        Design d = above(t, null);
        for (long x : chain) d = fold(d, t.prop(x, Store.P_DESIGN), t.prop(x, Store.P_LOOK), null);
        return d;
    }

    /**
     * מבנה 236: מסך הבית — כל התוכנה, האופן, ואז הבית עצמו (העיצוב שלו — מהמסד, או מההגדרה הישנה "עיצוב מסך הבית"). לא נקבע כלום —
     * רשת בלי מסגרת לצלמית (כמו האריחים בבית הישן).
     */
    static Design forHome(Store t, long home) {
        Design start = get(FILES).copy();
        start.around = NONE;
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
        if (look != null && !look.trim().isEmpty()) {
            if (d == null) d = start != null ? start.copy() : get(DEFAULT).copy();
            d.overlay(look);
        }
        return d;
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

    /** מבנה 243: העיצובים שאפשר לבחור באופן הזה — המוכנים, ואחריהם הקבצים שבתיקייה (בלי כפילות; השם הקודם — בשם של היום). */
    static List<String> names() {
        List<String> out = new ArrayList<String>(java.util.Arrays.asList(DEFAULT, FILES, WORDS));
        for (File f : files(dir())) {
            String n = current(nameOf(f));
            if (!out.contains(n)) out.add(n);
        }
        return out;
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
        return grid + "|" + cols + "|" + icon + "|" + gap + "|" + showIcon + "|" + around + "|" + tint + "|" + showData
                + "|" + dataBelow + "|" + nameT.size + nameT.weight + nameT.font + "|" + data.size + data.weight + data.font
                + "|" + fit + "|" + min + "|" + lines + "|" + (group == null ? "" : group.size + "" + group.weight + group.font)
                + "|" + (tag == null ? "" : tag.size + "" + tag.weight + tag.font) + "|" + wording + "|" + showTag + "|" + fade;
    }

    // ---------- קריאה ----------

    static Design parse(String name, String text) {
        Design d = new Design();
        d.name = name;
        String section = "";
        boolean byName = false, hadIcon = false, hadGap = false;                // מבנה 258
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
            if (S_LISTS.equals(section)) {
                d.lists.put(key, privateValue(value));
                continue;
            }
            int sc = value.indexOf(';');                                     // הערה בסוף השורה
            if (sc >= 0) value = value.substring(0, sc).trim();
            if (value.isEmpty()) continue;
            try {
                if (S_TITLE.equals(section)) d.text(d.title, key, value);
                else if (S_MID.equals(section)) {
                    if (key.equals(K_MEASURE)) byName = value.equals(BY_NAME);
                    else {
                        d.mid(key, value);
                        hadIcon |= key.equals(K_ICON);
                        hadGap |= key.equals(K_GAP);
                    }
                }
            } catch (RuntimeException ignored) {                             // ערך לא מובן — נשאר מברירת המחדל
            }
        }
        if (d.grid && !byName) d.fromTile(hadIcon, hadGap);                   // מבנה 258: קובץ רשת מלפני "לפי השם"
        return d;
    }

    /** מבנה 258: מה שנכתב בקובץ רשת ישן באחוזים מרוחב האריח — לאחוזים מגודל השם, כך שבטלפון רגיל ייראה כמו שנראה. */
    private void fromTile(boolean hadIcon, boolean hadGap) {
        float tile = OLD_WIDTH / (float) Math.max(1, cols), n = Math.max(1f, nameT.size);
        if (hadIcon) icon = clamp(Math.round(tile * icon / n), ICON[0], ICON[1]);
        if (hadGap) gap = clamp(Math.round(Math.max(2f, tile * gap / 200f) * 100f / n), GAP[0], GAP[1]);
    }

    /** מבנה 258: הריווח בפיקסלים — אחוז מגודל השם (namePx — גודל השם בפיקסלים), בשורות וברשת; least — המעט שנשאר תמיד. */
    int padPx(float namePx, int least) {
        return Math.max(least, (int) (namePx * gap / 100f));
    }

    /** מבנה 258: הצלמית בפיקסלים — אחוז מגודל השם, בשורות וברשת; ברשת — לא יותר ממה שנכנס באריח (הרוחב, פחות הריווח משני הצדדים). */
    int iconPx(float namePx, int tileW, int pad) {
        int px = (int) (namePx * icon / 100f);
        return grid && tileW > 0 ? Math.max(0, Math.min(px, tileW - 2 * pad)) : px;
    }

    /**
     * מבנה 235: הטווחים — במקום אחד. הקריאה של הקובץ מצמצמת אליהם; הפקודה ("עצב") מסרבת למה שמחוץ להם, ואומרת למה.
     * צלמית וריווח — באחוזים מגודל השם, בשורות וברשת (מבנה 258; עד אז ברשת — מרוחב האריח).
     */
    static final int[] COLS = {1, 8}, ICON = {0, 1000}, GAP = {0, 200}, SIZE = {6, 60}, WEIGHT = {100, 900};
    /** מבנה 254: שורות לשם, והגודל המזערי של שם שמוקטן כדי להיכנס (התאם למקום). */
    static final int[] LINES = {1, 6}, MIN = {6, 40};

    /**
     * מבנה 254 (המשתמש, 18.11.2026, על מה שעלה מהצילומים: "תבנה מה שהכי קל ומתאים ונראה לך" — קבוצה א: מה שכבר בעיצוב ואין לו
     * מתג): המפתחות של "אמצע" שנקבעים ב"עצב" מעבר לטורים, ריווח וצלמית — לפי הסדר. המתג — שם המפתח, עם "-" במקום רווח.
     */
    static final String[] SHAPE_KEYS = {K_STYLE, K_SHOW_ICON, K_AROUND, K_TINT, K_SHOW_DATA, K_DATA_AT, K_LINES, K_FIT, K_MIN,
            K_SHOW_TAG, K_FADE};   // מבנה 256
    /** מבנה 255: לכל מפתח — שם קבוע ללוח העיצוב (מה הוא מצייר), באותו סדר. בערכים — הראשון הוא ה"כן" (רשת, כן, מתחת). */
    static final String[] SHAPE_IDS = {"grid", "showIcon", "around", "tint", "showData", "dataBelow", "lines", "fit", "min",
            "showTag", "fade"};

    /** מבנה 254: הערכים שהמפתח הזה מקבל; null — מספר בטווח (shapeRange). */
    static String[] choices(String key) {
        if (key.equals(K_STYLE)) return new String[]{GRID, ROWS};
        if (key.equals(K_AROUND)) return new String[]{NONE, FRAME, FILL, CIRCLE};
        if (key.equals(K_DATA_AT)) return new String[]{BELOW, SIDE};
        if (key.equals(K_SHOW_ICON) || key.equals(K_TINT) || key.equals(K_SHOW_DATA) || key.equals(K_FIT) || key.equals(K_SHOW_TAG)
                || key.equals(K_FADE)) return new String[]{YES, NO};
        return null;
    }

    /** מבנה 254: הטווח של מפתח מספרי מ־SHAPE_KEYS; null — אין. */
    static int[] shapeRange(String key) {
        return key.equals(K_LINES) ? LINES : key.equals(K_MIN) ? MIN : null;
    }

    /** מבנה 254: הערך של מפתח מ־SHAPE_KEYS כמו שהוא בעיצוב הזה (כמו שנכתב בקובץ); null — לא מפתח כזה. */
    String shapeValue(String key) {
        if (key.equals(K_STYLE)) return grid ? GRID : ROWS;
        if (key.equals(K_SHOW_ICON)) return showIcon ? YES : NO;
        if (key.equals(K_AROUND)) return around;
        if (key.equals(K_TINT)) return tint ? YES : NO;
        if (key.equals(K_SHOW_DATA)) return showData ? YES : NO;
        if (key.equals(K_DATA_AT)) return dataBelow ? BELOW : SIDE;
        if (key.equals(K_LINES)) return String.valueOf(lines);
        if (key.equals(K_FIT)) return fit ? YES : NO;
        if (key.equals(K_MIN)) return String.valueOf(min);
        if (key.equals(K_SHOW_TAG)) return showTag ? YES : NO;
        if (key.equals(K_FADE)) return fade ? YES : NO;
        return null;
    }

    private void mid(String key, String v) {
        if (key.equals(K_STYLE)) grid = v.equals(GRID);
        else if (key.equals(K_COLS)) cols = clamp(num(v), COLS[0], COLS[1]);
        else if (key.equals(K_ICON)) icon = clamp(num(v), ICON[0], ICON[1]);
        else if (key.equals(K_GAP)) gap = clamp(num(v), GAP[0], GAP[1]);
        else if (key.equals(K_SHOW_ICON)) showIcon = !v.equals(NO);
        else if (key.equals(K_AROUND)) around = v.equals(NONE) || v.equals(FRAME) || v.equals(CIRCLE) ? v : FILL;
        else if (key.equals(K_SHOW_TAG)) showTag = !v.equals(NO);
        else if (key.equals(K_FADE)) fade = v.equals(YES);
        else if (key.equals(K_TINT)) tint = v.equals(YES);
        else if (key.equals(K_SHOW_DATA)) showData = !v.equals(NO);
        else if (key.equals(K_DATA_AT)) dataBelow = !v.equals(SIDE);
        else if (key.equals(K_FIT)) fit = v.equals(YES);
        else if (key.equals(K_MIN)) min = clamp(num(v), MIN[0], MIN[1]);
        else if (key.equals(K_LINES)) lines = clamp(num(v), LINES[0], LINES[1]);
        else {
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

    /** "צלמית 📚 · צבע ירוק" ← {"📚", "ירוק"}. */
    static String[] privateValue(String v) {
        String icon = "", color = "";
        for (String part : v.split(P_SEP)) {
            String p = part.trim();
            if (p.startsWith(P_ICON)) icon = p.substring(P_ICON.length()).trim();
            else if (p.startsWith(P_COLOR)) color = p.substring(P_COLOR.length()).trim();
        }
        return new String[]{icon, color};
    }

    static String privateText(String icon, String color) {
        StringBuilder sb = new StringBuilder();
        if (icon != null && !icon.isEmpty()) sb.append(P_ICON).append(' ').append(icon);
        if (color != null && !color.isEmpty()) sb.append(sb.length() > 0 ? " " + P_SEP + " " : "").append(P_COLOR).append(' ').append(color);
        return sb.toString();
    }

    // ---------- החלק הפרטי ----------
    // עד מ0.29 — צלמית וצבע לכל רשימה, לפי המסלול. במסד הצלמית נשמרת ברשימה עצמה; כאן נשאר רק מה ששומר על מה שכתוב
    // בקבצים: הקריאה (load) והשמירה בחזרה לברירת מחדל.

    /** הטקסט אחרי החלפת השורה של path בחלק הפרטי (newLine == null — מחיקה). */
    static String withPrivate(String text, String path, String newLine) {
        return withLine(text, S_LISTS, path, newLine);
    }

    private static String join(List<String> lines) {
        StringBuilder sb = new StringBuilder();
        for (String l : lines) sb.append(l).append('\n');
        return sb.toString();
    }

    // ---------- איפה ----------

    static File dir() {
        return new File(Paths.settings(), DIR);
    }

    static File fileOf(String name) {
        return new File(dir(), name + EXT);
    }

    static String nameOf(File f) {
        String n = f.getName();
        return n.endsWith(EXT) ? n.substring(0, n.length() - EXT.length()) : n;
    }

    static List<File> files(File dir) {
        List<File> out = new ArrayList<File>();
        File[] kids = dir.listFiles();
        if (kids != null) for (File k : kids) if (k.isFile() && k.getName().endsWith(EXT)) out.add(k);
        java.util.Collections.sort(out);
        return out;
    }

    static boolean builtIn(String name) {
        name = current(name);                                                 // מבנה 240: גם בשם הקודם
        return DEFAULT.equals(name) || FILES.equals(name) || WORDS.equals(name);
    }

    /**
     * כל העיצובים שאפשר לבחור: {שם, מאיפה ("" — האופן הזה; שם אופן; או שם תיקייה), מסלול הקובץ ("" — מובנה שעוד לא נכתב)}.
     * found — קובצי עיצוב שהאינדקס מצא בכונן (לפי הסיומת), מחוץ לאופנים.
     */
    static List<String[]> all(List<String> found) {
        List<String[]> out = new ArrayList<String[]>();
        List<String> here = new ArrayList<String>();
        for (File f : files(dir())) {
            out.add(new String[]{nameOf(f), "", f.getAbsolutePath()});
            here.add(nameOf(f));
        }
        for (String b : new String[]{DEFAULT, FILES, WORDS})
            if (!here.contains(b)) out.add(new String[]{b, "", ""});
        String cur = Modes.current();
        for (String m : Modes.all()) {
            if (m.equals(cur)) continue;
            for (File f : files(new File(new File(Modes.dirOf(m), Paths.SETTINGS), DIR)))
                out.add(new String[]{nameOf(f), m, f.getAbsolutePath()});
        }
        String modes = new File(Paths.data(), Modes.DIR).getAbsolutePath() + "/";
        if (found != null)
            for (String p : found) {
                if (p.startsWith(modes)) continue;                           // כבר נמנו למעלה
                File f = new File(p);
                if (f.isFile()) out.add(new String[]{nameOf(f), f.getParentFile().getName(), p});
            }
        return out;
    }

    /**
     * עיצוב מאופן אחר — מועתק לאופן הזה (כל אופן מחזיק את העיצובים שלו). מחזיר את השם שלו כאן.
     * שם תפוס בתוכן אחר — מוסיפים את שם האופן.
     */
    static String adopt(String[] picked) {
        if (picked[1].isEmpty()) {
            if (picked[2].isEmpty()) ensure(picked[0]);
            return picked[0];
        }
        try {
            String text = Files.readUtf8(new File(picked[2]));
            String name = picked[0];
            File f = fileOf(name);
            if (f.isFile() && !Files.readUtf8(f).equals(text)) {
                name = picked[0] + " (" + picked[1] + ")";
                f = fileOf(name);
            }
            if (!f.isFile()) {                                                 // מבנה 232: באזור של התוכנה — רק בהיתר
                String no = Physical.ownDir(dir()) ? Physical.ownWrite(f, text) : Words.SAVE_FAILED;
                if (no != null) { Log.i("העתקת עיצוב מאופן אחר: " + no); return null; }
            }
            return name;
        } catch (Throwable t) {
            Log.e("העתקת עיצוב מאופן אחר", t);
            return null;
        }
    }

    /** קובץ של עיצוב מובנה — נכתב אם חסר. */
    static File ensure(String name) {
        File f = fileOf(name);
        if (!f.isFile() && builtIn(name)) {
            String no = Physical.ownDir(dir()) ? Physical.ownWrite(f, builtInText(name)) : Words.SAVE_FAILED;   // מבנה 232
            if (no != null) Log.i("כתיבת עיצוב מובנה: " + no);
        }
        return f;
    }

    /** "חזרה לברירת מחדל": המראה מהמקור; הצלמיות והצבעים של הרשימות — נשארים. */
    static boolean reset(String name) {
        if (!builtIn(name)) return false;
        try {
            File f = fileOf(name);
            Design old = f.isFile() ? read(f, name) : null;
            String text = builtInText(name);
            if (old != null)
                for (Map.Entry<String, String[]> e : old.lists.entrySet())
                    text = withPrivate(text, e.getKey(), e.getKey() + " = " + privateText(e.getValue()[0], e.getValue()[1]));
            return Physical.ownDir(dir()) && Physical.ownWrite(f, text) == null;   // מבנה 232: רק בהיתר
        } catch (Throwable t) {
            Log.e("חזרה לברירת מחדל — עיצוב", t);
            return false;
        }
    }

    /** "שמירה בשם": עותק של הקובץ (כולל הצלמיות) בשם חדש. null — הצליח; אחרת הודעה. */
    static String saveAs(Design from, String newName) {
        String n = newName.trim().replace('/', '∕');
        if (n.isEmpty()) return Words.NEED_NAME;
        File f = fileOf(n);
        if (f.exists()) return Words.DesignW.EXISTS;
        try {
            String no = Permit.check(Permit.OWN);                              // מבנה 232: רק בהיתר — והסירוב נאמר
            if (no != null) return no;
            File src = from.file != null && from.file.isFile() ? from.file : null;
            String text = src != null ? Files.readUtf8(src) : builtInText(builtIn(from.name) ? from.name : DEFAULT);
            if (!Physical.ownDir(dir())) return Words.SAVE_FAILED;
            return Physical.ownWrite(f, text);                                 // מבנה 232: רק בהיתר; null — נשמר
        } catch (Throwable t) {
            Log.e("שמירת עיצוב בשם", t);
            return Words.SAVE_FAILED;
        }
    }

    // ---------- הטופס (1.12.1): כתיבת המראה הכללי ----------

    /** הטקסט אחרי קביעת key = value בקבוצה section (שורה קיימת — מוחלפת; חסרה — נוספת בסוף הקבוצה). */
    static String withKey(String text, String section, String key, String value) {
        return withLine(text, section, key, key + " = " + value);
    }

    /**
     * 1.14.0: השורה של key בקבוצה section — מוחלפת ב-line, נוספת בסוף הקבוצה, או (line == null) נמחקת. קבוצה חסרה —
     * נוספת לפני [רשימות] (והיא עצמה — בסוף). הערות (";") — לא נוגעים. (עד כאן — שתי העתקות כמעט זהות.)
     */
    private static String withLine(String text, String section, String key, String line) {
        List<String> lines = new ArrayList<String>();
        for (String l : text.replace("\r", "").split("\n", -1)) lines.add(l);
        while (!lines.isEmpty() && lines.get(lines.size() - 1).trim().isEmpty()) lines.remove(lines.size() - 1);
        int start = -1, end = lines.size();
        for (int i = 0; i < lines.size(); i++) {
            String t = lines.get(i).trim();
            if (t.startsWith("[") && t.endsWith("]")) {
                if (start >= 0) {
                    end = i;
                    break;
                }
                if (t.substring(1, t.length() - 1).trim().equals(section)) start = i;
            }
        }
        if (start < 0) {
            if (line == null) return join(lines);
            int at = 0;                                                       // לפני [רשימות], אם יש
            while (at < lines.size() && !lines.get(at).trim().equals("[" + S_LISTS + "]")) at++;
            List<String> add = new ArrayList<String>();
            if (at > 0) add.add("");
            add.add("[" + section + "]");
            add.add(line);
            if (at < lines.size()) add.add("");
            lines.addAll(at, add);
            return join(lines);
        }
        for (int i = start + 1; i < end; i++) {
            String t = lines.get(i);
            int eq = t.indexOf('=');
            if (eq > 0 && !t.trim().startsWith(";") && t.substring(0, eq).trim().equals(key)) {
                if (line == null) lines.remove(i);
                else lines.set(i, line);
                return join(lines);
            }
        }
        if (line != null) {
            int at = end;
            while (at > start + 1 && lines.get(at - 1).trim().isEmpty()) at--;
            lines.add(at, line);
        }
        return join(lines);
    }

    /** המראה הכללי של d — לקובץ שלו (ההערות והחלק הפרטי נשארים). */
    static boolean save(Design d) {
        try {
            File f = d.file != null ? d.file : fileOf(d.name);
            String text = f.isFile() ? Files.readUtf8(f) : "";
            text = withKey(text, S_TITLE, K_SIZE, num(d.title.size));
            text = withKey(text, S_TITLE, K_WEIGHT, weightName(d.title.weight));
            text = withKey(text, S_TITLE, K_FONT, d.title.font);
            String[][] mid = {{K_MEASURE, BY_NAME}, {K_STYLE, d.grid ? GRID : ROWS}, {K_COLS, "" + d.cols}, {K_ICON, d.icon + "%"},
                    {K_GAP, d.gap + "%"}, {K_SHOW_ICON, d.showIcon ? YES : NO}, {K_AROUND, d.around},
                    {K_TINT, d.tint ? YES : NO}, {K_SHOW_DATA, d.showData ? YES : NO}, {K_DATA_AT, d.dataBelow ? BELOW : SIDE},
                    {T_NAME + ": " + K_SIZE, num(d.nameT.size)}, {T_NAME + ": " + K_WEIGHT, weightName(d.nameT.weight)},
                    {T_NAME + ": " + K_FONT, d.nameT.font}, {T_DATA + ": " + K_SIZE, num(d.data.size)},
                    {T_DATA + ": " + K_WEIGHT, weightName(d.data.weight)}, {T_DATA + ": " + K_FONT, d.data.font},
                    {K_FIT, d.fit ? YES : NO}, {K_MIN, "" + d.min}, {K_LINES, "" + d.lines},
                    {K_SHOW_TAG, d.showTag ? YES : NO}, {K_FADE, d.fade ? YES : NO}};   // מבנה 256
            for (String[] kv : mid) text = withKey(text, S_MID, kv[0], kv[1]);
            if (d.group != null) {                                            // מבנה 239
                text = withKey(text, S_MID, T_GROUP + ": " + K_SIZE, num(d.group.size));
                text = withKey(text, S_MID, T_GROUP + ": " + K_WEIGHT, weightName(d.group.weight));
                text = withKey(text, S_MID, T_GROUP + ": " + K_FONT, d.group.font);
            }
            for (Map.Entry<String, String> w : d.wording.entrySet())          // מבנה 250
                text = withKey(text, S_MID, K_WORDING + ": " + w.getKey(), w.getValue());
            if (d.tag != null) {                                              // מבנה 249
                text = withKey(text, S_MID, T_TAG + ": " + K_SIZE, num(d.tag.size));
                text = withKey(text, S_MID, T_TAG + ": " + K_WEIGHT, weightName(d.tag.weight));
                text = withKey(text, S_MID, T_TAG + ": " + K_FONT, d.tag.font);
            }
            return Physical.ownDir(dir()) && Physical.ownWrite(f, text) == null;   // מבנה 232: רק בהיתר
        } catch (Throwable t) {
            Log.e("שמירת עיצוב מהטופס", t);
            return false;
        }
    }

    private static String num(float v) {
        return v == (int) v ? "" + (int) v : "" + v;
    }

    /** "שינוי שם": הקובץ, הרשימות שמפנות אליו ומסך הבית. null — הצליח; אחרת הודעה. */
    static String rename(final String from, String to) {
        final String n = to.trim().replace('/', '∕');
        if (n.isEmpty()) return Words.NEED_NAME;
        if (n.equals(from)) return null;
        File a = fileOf(from), b = fileOf(n);
        if (b.exists()) return Words.DesignW.EXISTS;
        if (!a.isFile() || !Physical.ownMove(a, b)) return Words.SAVE_FAILED;   // מבנה 232: רק בהיתר
        renameRefs(from, n);
        return null;
    }

    /** כל מה שמפנה לעיצוב from — מפנה עכשיו ל־to ("" — יורש). */
    static void renameRefs(final String from, final String to) {
        if (from.equals(homeName())) setHome(to);
        DbFollow.designRenamedEverywhere(from, to);                          // הרשימות — במסד
    }

    /** מחיקת עיצוב: הקובץ לגיבויים; מה שהפנה אליו — יורש (ומגיע בסוף לברירת המחדל). */
    static boolean delete(String name) {
        File f = fileOf(name);
        if (!f.isFile()) return false;
        if (Permit.check(Permit.OWN) != null) return false;                   // מבנה 232: גם הגיבוי — רק בהיתר
        Files.backup(f, DIR);
        if (!Physical.ownDelete(f)) return false;
        if (!builtIn(name)) renameRefs(name, "");
        return true;
    }

    // ---------- טעינה, עם זיכרון ----------

    private static final Map<String, Design> cache = new java.util.HashMap<String, Design>();
    private static final Map<String, Long> stamp = new java.util.HashMap<String, Long>();
    /** בזיכרון: מובנה שנקרא מהמקור (בלי קובץ). */
    private static final String BUILT = "\u0000";

    static synchronized Design read(File f, String name) {
        String k = f.getAbsolutePath();
        Design d = cache.get(k);
        if (d != null && stamp.get(k) == f.lastModified()) return d;
        try {
            d = parse(name, Files.readUtf8(f));
        } catch (Throwable t) {
            Log.e("קריאת עיצוב " + f.getName(), t);
            d = parse(name, builtInText(DEFAULT));
        }
        d.file = f;
        cache.put(k, d);
        stamp.put(k, f.lastModified());
        return d;
    }

    /**
     * העיצוב בשם הזה: מהאופן הזה; מובנה שאין לו קובץ — מהמקור (מבנה 232: בלי לכתוב בזמן ציור — הכתיבה רק בהיתר, דרך המנהל);
     * לא נמצא — "עיצוב ברירת מחדל" (לא נשבר).
     */
    static synchronized Design get(String name) {
        if (name == null || name.trim().isEmpty()) name = DEFAULT;
        name = current(name);                                                 // מבנה 240: שם קודם — השם של היום
        File f = fileOf(name);
        if (f.isFile()) return read(f, name);
        File was = oldFile(name);                                            // מובנה שנערך בשמו הקודם — מה שנערך נשאר
        if (was != null) return read(was, name);
        if (!builtIn(name)) return get(DEFAULT);
        Design d = cache.get(BUILT + name);                                  // מובנה בלי קובץ — פעם אחת, מהמקור
        if (d == null) {
            d = parse(name, builtInText(name));
            cache.put(BUILT + name, d);
        }
        d.file = f;
        return d;
    }

    // ---------- הירושה ----------

    /** העיצוב שנבחר למסך הבית; "" — לא נבחר (מסך הבית נשאר כמו שהיה, והרשימות — "עיצוב ברירת מחדל"). */
    static String homeName() {
        return Conf.get(Conf.GENERAL, HOME_KEY, "");
    }

    static void setHome(String name) {
        Conf.set(Conf.GENERAL, HOME_KEY, name);
    }

    // ---------- העיצובים המובנים ----------

    static String builtInText(String name) {
        StringBuilder sb = new StringBuilder();
        boolean files = FILES.equals(name), words = WORDS.equals(name);
        sb.append("; ").append(name).append(" — ").append(files ? Words.DesignW.WHY_FILES : words ? Words.DesignW.WHY_WORDS
                : Words.DesignW.WHY_DEFAULT).append('\n');
        sb.append("; ").append(Words.DesignW.FILE_NOTE).append('\n');
        sb.append("; ").append(Words.DesignW.FILE_NOTE2).append('\n');
        sb.append('\n').append('[').append(S_TITLE).append("]\n");
        line(sb, K_SIZE, files ? "15" : "12");
        line(sb, K_WEIGHT, files ? W_BOLD : W_REG);
        line(sb, K_FONT, SYSTEM);
        sb.append('\n').append('[').append(S_MID).append("]\n");
        line(sb, K_MEASURE, BY_NAME);                                           // מבנה 258
        line(sb, K_STYLE, files ? GRID : ROWS);
        line(sb, K_COLS, files ? "3" : words ? "2" : "1");
        line(sb, K_ICON, files ? "500%" : "340%");                              // מבנה 258: היו 55% ו־8% מרוחב האריח — אותו מראה בטלפון רגיל
        line(sb, K_GAP, files ? "35%" : words ? "40%" : "50%");
        line(sb, K_SHOW_ICON, words ? NO : YES);
        line(sb, K_AROUND, files ? FRAME : FILL);
        line(sb, K_TINT, words ? YES : NO);
        line(sb, K_SHOW_DATA, YES);
        line(sb, K_DATA_AT, BELOW);
        line(sb, T_NAME + ": " + K_SIZE, files ? "14" : words ? "15" : "16");
        line(sb, T_NAME + ": " + K_WEIGHT, files || words ? W_BOLD : W_REG);
        line(sb, T_NAME + ": " + K_FONT, SYSTEM);
        line(sb, T_DATA + ": " + K_SIZE, "11");
        line(sb, T_DATA + ": " + K_WEIGHT, W_REG);
        line(sb, T_DATA + ": " + K_FONT, SYSTEM);
        line(sb, K_FIT, files || words ? YES : NO);
        line(sb, K_MIN, "11");
        line(sb, K_LINES, files ? "2" : words ? "2" : "2");
        line(sb, K_SHOW_TAG, YES);                                              // מבנה 256
        line(sb, K_FADE, NO);
        sb.append('\n').append('[').append(S_LISTS).append("]\n");
        sb.append("; ").append(Words.DesignW.LISTS_NOTE).append('\n');
        return sb.toString();
    }

    private static void line(StringBuilder sb, String key, String value) {
        sb.append(key).append(" = ").append(value).append('\n');
    }
}
