package com.tikiyot.vfm;

// הקובץ הראשי. אחראי: לקרוא ולכתוב את מבנה התוכנה — הכלים במסך הבית והגדרותיהם, הגדרות נפוצות ורשימות פנימיות.
// 1.15.0: סביבות העבודה ([אריח: …]) — נקראות רק כדי להסב אותן לרשימות (Envs); אין יותר יצירה או עריכה שלהן.
// כל פעולה מתבצעת לפי מזהה, לא לפי שם. אסור לו: לדעת מה כל פונקציה עושה.

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Structure {

    public static final String TILE = "אריח: ";  // תחביר
    public static final String FUNC = "פונקציה: ";  // תחביר
    public static final String ID = "מזהה";  // תחביר
    /** הגדרות שנקבעות במסך הכלי עצמו, ונשמרות גם כשמשנים את הגדרות הכלי בטופס. */
    public static final java.util.Set<String> VIEW_KEYS = new java.util.HashSet<String>(
            java.util.Arrays.asList(SmartLists.SORT, SmartLists.DIR, "קיבוץ", SmartLists.PATHS, SmartLists.LOOSE, SmartLists.HIDDEN, Quick.STATE));   // 181: הכפתורים הדולקים; 170: "נסתרים" — מכלי "קבצים" שהומר // תחביר

    /**
     * 93: המפתחות שהתוכנה עצמה כותבת בסעיף של כלי. בעריכה בטופס הם נבנים מחדש;
     * כל שורה אחרת בסעיף (הערה, מפתח שנכתב ביד) נשמרת כמות שהיא.
     */
    static final java.util.Set<String> KNOWN_KEYS = new java.util.HashSet<String>(java.util.Arrays.asList(
            ID, "סוג", "תיאור", "צלמית", "תנאי", FnVirtual.LIST, "חבילה", "כונן", "נסתרים", "מוגבל",  // תחביר
            "אופן", "היקף", "מול", "השוואה", SmartLists.PATHS, SmartLists.LOOSE, SmartLists.SORT, SmartLists.DIR, "קיבוץ",  // תחביר
            "שם", SmartLists.LINK, SmartLists.VIEW, Quick.STATE, "למה", FnCommand.FILE));   // 1.13.0: למה, קובץ (כלי פקודה) // תחביר                       // "שם" — מפתח ישן, שהטופס מחליף // תחביר

    private static final String HEADER =
            "; הקובץ הראשי של התוכנה: הכלים שבמסך הבית, וההגדרות שלהם.\n"  // תחביר
            + "; אפשר לערוך אותו ידנית, והתוכנה בונה ממנו את מסך הבית.\n"  // תחביר
            + ";\n"
            + "; [פונקציה: שם]   כלי במסך הבית. אחריו: סוג, תיאור, והגדרות\n"  // תחביר
            + ";\n"
            + "; לכל כלי יש שורת \"מזהה\". היא הזהות, והשם ניתן לשינוי חופשי.\n"  // תחביר
            + "; שורה שנכתבה ידנית בלי מזהה תקבל אותו אוטומטית.\n"  // תחביר
            + ";\n"
            + "; דוגמה:\n"  // תחביר
            + ";   [פונקציה: ספרים חדשים]\n"  // תחביר
            + ";   סוג = חכמה\n"  // תחביר
            + ";   תנאי = סוג:pdf|epub תאריך:חודש\n";  // תחביר

    /** לבדיקות: הפניית הקובץ הראשי למקום אחר. */
    public static File testFile;

    private Structure() { }

    public static File file() {
        return testFile != null ? testFile : Paths.structure();
    }

    public static synchronized Ini ini() {
        Ini.createIfMissing(file(), HEADER);
        return Ini.load(file());
    }

    // ---------- מודל ----------

    public static final class Func {
        public String id = "", name = "", kind = "", description = "";
        public final Map<String, String> params = new LinkedHashMap<String, String>();

        public Target target() {
            Target t = Target.of(kind, params);
            t.id = id;
            return t;
        }
    }

    /** סביבת עבודה ישנה — רק להסבה (Envs). */
    public static final class Tile {
        public String id = "", name = "", description = "", icon = "", why = "";
        public final List<Func> funcs = new ArrayList<Func>();
    }

    public static final class Model {
        public final List<Func> direct = new ArrayList<Func>();   // פונקציות ישירות במסך הראשי
        public final List<Tile> tiles = new ArrayList<Tile>();

        /** כל הכלים (ואם עוד לא הוסבו — גם אלה שבסביבות) במקום אחד (80.7). */
        public List<Func> all() {
            List<Func> out = new ArrayList<Func>(direct);
            for (Tile t : tiles) out.addAll(t.funcs);
            return out;
        }
    }

    public static synchronized Model load() {
        return parse(ini());
    }

    /** 162: המודל של קובץ ראשי כלשהו (גם של אופן עבודה אחר). */
    static Model parse(Ini ini) {
        Model m = new Model();
        Tile tile = null;
        Func func = null;
        String currentSection = null;
        for (Ini.Line l : ini.lines) {
            if (l.isHeader()) {
                String name = l.headerName();
                currentSection = name;
                if (name.startsWith(TILE)) {
                    tile = new Tile();
                    tile.name = name.substring(TILE.length()).trim();
                    m.tiles.add(tile);
                    func = null;
                } else if (name.startsWith(FUNC)) {
                    func = new Func();
                    func.name = name.substring(FUNC.length()).trim();
                    if (tile == null) m.direct.add(func); else tile.funcs.add(func);
                } else {
                    func = null;
                }
                continue;
            }
            if (l.key == null || currentSection == null) continue;
            if (currentSection.startsWith(TILE) && tile != null) {
                if (l.key.equals(ID)) tile.id = l.value;
                else if (l.key.equals("תיאור")) tile.description = l.value;  // תחביר
                else if (l.key.equals("צלמית")) tile.icon = l.value;  // תחביר
                else if (l.key.equals("למה")) tile.why = l.value;  // תחביר
            } else if (currentSection.startsWith(FUNC) && func != null) {
                if (l.key.equals(ID)) func.id = l.value;
                else if (l.key.equals("סוג")) func.kind = l.value;  // תחביר
                else if (l.key.equals("תיאור")) func.description = l.value;  // תחביר
                else func.params.put(l.key, l.value);
            }
        }
        return m;
    }

    // ---------- עזר ----------

    static String newId(String prefix) {
        return prefix + Long.toString(System.currentTimeMillis(), 36) + two((int) (Math.random() * 1296))
                + Integer.toString(SEQ.incrementAndGet(), 36);                // המונה — בסוף, שלם (לא חוזר על עצמו)
    }

    /** באג (1.15.1): בלי רוחב קבוע "1"+"ab" ו-"1a"+"b" יצאו אותו מזהה, והמונה חזר לאפס אחרי 1296 — תמיד שתי ספרות. */
    private static String two(int n) {
        String s = Integer.toString(n, 36);
        return s.length() < 2 ? "0" + s : s;
    }

    /** 1.15.0 (באג): כמה מזהים באותו אלפית שנייה (העתקה, הסבה) — עד כאן רק אקראי, ולפעמים יצאו שניים זהים. */
    private static final java.util.concurrent.atomic.AtomicInteger SEQ = new java.util.concurrent.atomic.AtomicInteger();

    /** שורת הכותרת של המקטע שבו מזהה כזה. */
    static int indexOfId(Ini ini, String id) {
        if (id == null || id.isEmpty()) return -1;
        int header = -1;
        for (int i = 0; i < ini.lines.size(); i++) {
            Ini.Line l = ini.lines.get(i);
            if (l.isHeader()) {
                header = i;
                continue;
            }
            if (l.key != null && l.key.equals(ID) && id.equals(l.value)) return header;
        }
        return -1;
    }

    static void save(Ini ini) {
        if (ini.saveQuiet()) Mirror.rebuild();
    }

    /** משלים מזהים לשורות שנכתבו ידנית. */
    public static synchronized void ensureIds() {
        Ini ini = ini();
        if (ensureIds(ini)) save(ini);
    }

    /** 1.15.0: אותו דבר בקובץ ראשי כלשהו (גם של אופן אחר); true — נוסף מזהה. */
    static boolean ensureIds(Ini ini) {
        boolean changed = false;
        for (int i = 0; i < ini.lines.size(); i++) {
            Ini.Line l = ini.lines.get(i);
            if (!l.isHeader()) continue;
            String name = l.headerName();
            boolean smart = name.startsWith(SmartLists.SECTION);              // 147
            if (!name.startsWith(TILE) && !name.startsWith(FUNC) && !smart) continue;
            int end = ini.sectionEnd(i);
            boolean has = false;
            for (int k = i + 1; k < end; k++) {
                Ini.Line x = ini.lines.get(k);
                if (x.key != null && x.key.equals(ID) && x.value != null && !x.value.isEmpty()) has = true;
            }
            if (!has) {
                ini.lines.add(i + 1, Ini.line(ID + " = " + newId(name.startsWith(TILE) ? "t" : smart ? "s" : "f"), name));
                changed = true;
                i++;
            }
        }
        return changed;
    }

    /** כלי לפי מזהה, או null. */
    public static synchronized Func func(String id) {
        if (id == null || id.isEmpty()) return null;
        Model m = load();
        for (Func f : m.all()) if (f.id.equals(id)) return f;
        return null;
    }

    /** קובע ערך בסעיף שזה המזהה שלו. */
    public static synchronized void setKey(String id, String key, String value) {
        Ini ini = ini();
        int at = indexOfId(ini, id);
        if (at < 0) return;
        int end = ini.sectionEnd(at);
        for (int i = at + 1; i < end; i++) {
            Ini.Line l = ini.lines.get(i);
            if (l.key != null && l.key.equals(key)) {
                if (value.equals(l.value)) return;
                ini.set(l, key, value);
                save(ini);
                return;
            }
        }
        ini.lines.add(end, Ini.line(key + " = " + value, ini.lines.get(at).headerName()));
        save(ini);
    }

    // ---------- סעיפים שאינם אריח או כלי (147: הגדרות נפוצות) ----------

    // ---------- 161: סעיף לפי מזהה (כלי, הגדרה נפוצה, רשימה פנימית) ----------

    /** הסעיף שזה המזהה שלו: שם (הכותרת כולה), ושאר המפתחות; או null. */
    public static synchronized Func byId(String id) {
        return byId(ini(), id);
    }

    /**
     * 1.16.0: הסעיף כמו כלי — סוג, שם בלי הקידומת (ובפנימית — בלי הבעלים), תיאור, ושאר המפתחות; כך אותו טופס
     * ("הגדרות") עובד גם על רשימה חכמה שבתוך רשימה. null — אין.
     */
    public static synchronized Func asTool(String id) {
        Func f = byId(ini(), id);
        if (f == null) return null;
        String k = f.params.remove("סוג"), d = f.params.remove("תיאור");  // תחביר
        f.kind = k == null || k.isEmpty() ? FnSmart.KIND : k;
        f.description = d == null ? "" : d;
        String n = f.name;
        if (n.startsWith(FUNC)) n = n.substring(FUNC.length());
        else if (n.contains(" › ")) n = n.substring(n.lastIndexOf(" › ") + 3);
        else if (n.indexOf(':') >= 0) n = n.substring(n.indexOf(':') + 1);
        f.name = n.trim();
        return f;
    }

    /** 1.14.0: אותו דבר בכל קובץ ראשי (גם של אופן אחר — ModeCopy). */
    static Func byId(Ini ini, String id) {
        int at = indexOfId(ini, id);
        if (at < 0) return null;
        Func f = new Func();
        f.id = id;
        f.name = ini.lines.get(at).headerName();
        int end = ini.sectionEnd(at);
        for (int i = at + 1; i < end; i++) {
            Ini.Line l = ini.lines.get(i);
            if (l.key != null && !l.key.equals(ID)) f.params.put(l.key, l.value == null ? "" : l.value);
        }
        return f;
    }

    /** מפתח אחד בסעיף לפי מזהה: מעדכן, מוסיף, או (ערך ריק) מוחק. false — הסעיף לא נמצא. */
    public static synchronized boolean setById(String id, String key, String value) {
        Ini ini = ini();
        int at = indexOfId(ini, id);
        if (at < 0) return false;
        int end = ini.sectionEnd(at);
        String sec = ini.lines.get(at).headerName();
        for (int i = at + 1; i < end; i++) {
            Ini.Line l = ini.lines.get(i);
            if (!key.equals(l.key)) continue;
            if (value == null || value.isEmpty()) ini.lines.remove(i);
            else ini.set(l, key, value);
            save(ini);
            return true;
        }
        if (value != null && !value.isEmpty()) {
            ini.lines.add(end, Ini.line(key + " = " + value, sec));
            save(ini);
        }
        return true;
    }

    /** מחיקת סעיף לפי מזהה (רשימה פנימית שהוסרה). */
    public static synchronized boolean removeById(String id) {
        Ini ini = ini();
        if (!cut(ini, id, true)) return false;
        save(ini);
        return true;
    }

    /**
     * 1.14.0: הוצאת סעיף לפי מזהה מקובץ ראשי כלשהו; blanks — גם השורות הריקות אחריו. false — לא נמצא.
     * (עד כאן — אותה לולאה בחמישה מקומות.)
     */
    static boolean cut(Ini ini, String id, boolean blanks) {
        int at = indexOfId(ini, id);
        if (at < 0) return false;
        int end = ini.sectionEnd(at);
        if (blanks) while (end < ini.lines.size() && ini.lines.get(end).raw.trim().isEmpty()) end++;
        for (int i = end - 1; i >= at; i--) ini.lines.remove(i);
        return true;
    }

    /** כל הסעיפים שכותרתם מתחילה ב-prefix, לפי הסדר: שם, מזהה, ושאר המפתחות. */
    public static synchronized List<Func> sections(String prefix) {
        List<Func> out = new ArrayList<Func>();
        Func cur = null;
        for (Ini.Line l : ini().lines) {
            if (l.isHeader()) {
                cur = null;
                if (l.headerName().startsWith(prefix)) {
                    cur = new Func();
                    cur.name = l.headerName().substring(prefix.length()).trim();
                    out.add(cur);
                }
                continue;
            }
            if (cur == null || l.key == null) continue;
            if (l.key.equals(ID)) cur.id = l.value;
            else cur.params.put(l.key, l.value);
        }
        return out;
    }

    /**
     * יוצר או מעדכן סעיף לפי השם. מפתח קיים — מתעדכן; חסר — נוסף; שורה אחרת (הערה, מפתח שנכתב ביד) — נשמרת.
     * סעיף חדש — בראש הקובץ, אחרי סעיפים כמוהו ולפני כל אריח וכלי (כך אינו נבלע באריח האחרון). מחזיר את המזהה.
     */
    public static synchronized String putSection(String prefix, String name, String idPrefix, Map<String, String> kv) {
        Ini ini = ini();
        String sec = prefix + name;
        int at = -1;
        for (int i = 0; i < ini.lines.size(); i++)
            if (ini.lines.get(i).isHeader() && ini.lines.get(i).headerName().trim().equals(sec.trim())) {
                at = i;
                break;
            }
        String id = null;
        if (at < 0) {
            int top = ini.lines.size();                                       // אחרי הסעיפים מאותו סוג שבראש הקובץ
            for (int i = 0; i < ini.lines.size(); i++)
                if (ini.lines.get(i).isHeader() && !ini.lines.get(i).headerName().startsWith(prefix)) { top = i; break; }
            List<Ini.Line> block = new ArrayList<Ini.Line>();
            id = newId(idPrefix);
            block.add(Ini.line("[" + sec + "]", sec));
            block.add(Ini.line(ID + " = " + id, sec));
            for (Map.Entry<String, String> e : kv.entrySet()) block.add(Ini.line(e.getKey() + " = " + e.getValue(), sec));
            block.add(Ini.line("", sec));
            ini.lines.addAll(top, block);
        } else {
            int end = ini.sectionEnd(at);
            Map<String, String> left = new LinkedHashMap<String, String>(kv);
            for (int i = at + 1; i < end; i++) {
                Ini.Line l = ini.lines.get(i);
                if (l.key == null) continue;
                if (l.key.equals(ID)) id = l.value;
                else if (left.containsKey(l.key)) ini.set(l, l.key, left.remove(l.key));
            }
            int put = end;
            while (put > at + 1 && ini.lines.get(put - 1).raw.trim().isEmpty()) put--;
            if (id == null) {
                id = newId(idPrefix);
                ini.lines.add(at + 1, Ini.line(ID + " = " + id, sec));
                put++;
            }
            for (Map.Entry<String, String> e : left.entrySet()) ini.lines.add(put++, Ini.line(e.getKey() + " = " + e.getValue(), sec));
        }
        save(ini);
        return id;
    }

    /** 1.13.0: שינוי שם של סעיף (כותרת בלבד; התוכן והמזהה נשארים). false — לא נמצא, או שהשם החדש תפוס. */
    public static synchronized boolean renameSection(String prefix, String from, String to) {
        Ini ini = ini();
        int at = -1;
        for (int i = 0; i < ini.lines.size(); i++) {
            Ini.Line l = ini.lines.get(i);
            if (!l.isHeader()) continue;
            String h = l.headerName().trim();
            if (h.equals((prefix + to).trim())) return false;
            if (h.equals((prefix + from).trim())) at = i;
        }
        if (at < 0) return false;
        ini.lines.set(at, Ini.line("[" + prefix + to + "]", prefix + to));
        save(ini);
        return true;
    }

    // ---------- פונקציות ----------

    private static List<Ini.Line> funcBlock(String name, String id, String kind,
                                            String description, Target t, boolean blankFirst) {
        return block(FUNC + name, id, kind, description, t, blankFirst);
    }

    /** הסעיף: כותרת, מזהה, סוג, תיאור, והשאר. 1.16.0: "תיאור" שבא מהטופס (בתוך t) — הוא התיאור, לא שורה כפולה. */
    private static List<Ini.Line> block(String sec, String id, String kind, String description, Target t, boolean blankFirst) {
        String fromForm = t == null ? null : t.params.get("תיאור");  // תחביר
        if (fromForm != null) description = fromForm;
        List<Ini.Line> out = new ArrayList<Ini.Line>();
        if (blankFirst) out.add(Ini.line("", sec));
        out.add(Ini.line("[" + sec + "]", sec));
        out.add(Ini.line(ID + " = " + id, sec));
        out.add(Ini.line("סוג = " + kind, sec));  // תחביר
        out.add(Ini.line("תיאור = " + (description == null ? "" : description), sec));  // תחביר
        if (t != null) for (Map.Entry<String, String> e : t.params.entrySet()) {
            if (e.getKey().equals("תיאור")) continue;  // תחביר
            if (e.getValue() != null && !e.getValue().isEmpty())
                out.add(Ini.line(e.getKey() + " = " + e.getValue(), sec));
        }
        return out;
    }

    /** 1.14.0: איפה נכנס כלי חדש במסך הבית — אחרי הכלים (ולפני סביבה שעוד לא הוסבה). */
    static int insertAt(Ini ini) {
        for (int i = 0; i < ini.lines.size(); i++) {
            Ini.Line l = ini.lines.get(i);
            if (l.isHeader() && l.headerName().startsWith(TILE)) return i;
        }
        return ini.lines.size();
    }

    /** כלי חדש במסך הבית. מחזיר את המזהה שלו. */
    public static synchronized String addDirectFunc(String name, String kind, String description, Target t) {
        Ini ini = ini();
        String id = newId("f");
        List<Ini.Line> block = funcBlock(name, id, kind, description, t, true);
        ini.lines.addAll(insertAt(ini), block);
        save(ini);
        return id;
    }

    public static synchronized void updateFunc(String id, String newName, String description, Target t) {
        Ini ini = ini();
        int at = indexOfId(ini, id);
        if (at < 0) return;
        int end = ini.sectionEnd(at);
        String kind = "";
        Map<String, String> kept = new LinkedHashMap<String, String>();
        String why = null;
        for (int i = at + 1; i < end; i++) {
            Ini.Line l = ini.lines.get(i);
            if (l.key != null && l.key.equals("סוג")) kind = l.value;  // תחביר
            if (l.key != null && VIEW_KEYS.contains(l.key)) kept.put(l.key, l.value);
            if (l.key != null && l.key.equals(Program.WHY)) why = l.value;
        }
        // 1.16.0 (באג): "שינוי ההגדרות" של רשימה חכמה איבד את ה"למה" (הטופס לא מחזיר אותו) — נשאר, אלא אם הטופס קבע
        if (t != null && why != null && !t.params.containsKey(Program.WHY)) t.params.put(Program.WHY, why);
        // הגדרות תצוגה שנקבעו במסך (מיון, כיוון...) נשמרות אם טופס ההגדרה לא קבע אותן
        if (t != null) {
            Target merged = Target.of(t.kind, kept);
            merged.params.putAll(t.params);
            for (Map.Entry<String, String> e : kept.entrySet())
                if (!t.params.containsKey(e.getKey()) || t.params.get(e.getKey()).isEmpty())
                    merged.params.put(e.getKey(), e.getValue());
            t = merged;
        }
        // 93: הערות ומפתחות שהתוכנה לא מכירה (נכתבו ביד) — נשמרים כמות שהם, בסוף הסעיף
        List<Ini.Line> extras = new ArrayList<Ini.Line>();
        for (int i = at + 1; i < end; i++) {
            Ini.Line l = ini.lines.get(i);
            String r = l.raw.trim();
            if (r.isEmpty()) continue;
            if (l.key == null || !KNOWN_KEYS.contains(l.key)) extras.add(l);
        }
        if (t != null && t.kind != null && !t.kind.isEmpty()) kind = t.kind;      // 150: בעריכה אפשר להחליף סוג
        String old = ini.lines.get(at).headerName();                         // 1.16.0 (באג): רשימה פנימית נשארת פנימית
        String sec = old.startsWith(Nested.INNER) && old.contains(" › ")
                ? old.substring(0, old.indexOf(" › ") + 3) + newName : FUNC + newName;
        List<Ini.Line> block = block(sec, id, kind, description, t, false);
        block.addAll(extras);
        for (int i = end - 1; i >= at; i--) ini.lines.remove(i);
        ini.lines.addAll(at, block);
        save(ini);
    }

    /**
     * 1.14.0: הזזת כלי במסך הבית — dir: ‎-1 ימינה (קודם), 1 שמאלה (אחר כך). עד כאן — כלי ישיר תמיד לפי סדר ההוספה
     * (המשתמש: "אני לא יכול לקבוע את המיקום שלה בתוך מסך הבית"). false — אין לאן.
     */
    public static synchronized boolean shiftFunc(String id, int dir) {
        List<Func> place = load().direct;
        int pos = -1;
        for (int i = 0; i < place.size(); i++) if (place.get(i).id.equals(id)) pos = i;
        int other = pos + (dir < 0 ? -1 : 1);
        if (pos < 0 || other < 0 || other >= place.size()) return false;
        Ini ini = ini();
        int a = indexOfId(ini, id), b = indexOfId(ini, place.get(other).id);
        if (a < 0 || b < 0) return false;
        int f0 = Math.min(a, b), s0 = Math.max(a, b);
        int f1 = ini.sectionEnd(f0), s1 = ini.sectionEnd(s0);
        if (f1 > s0) return false;
        List<Ini.Line> first = new ArrayList<Ini.Line>(ini.lines.subList(f0, f1)),
                mid = new ArrayList<Ini.Line>(ini.lines.subList(f1, s0)),
                second = new ArrayList<Ini.Line>(ini.lines.subList(s0, s1));
        List<Ini.Line> block = new ArrayList<Ini.Line>(second);
        block.addAll(mid);
        block.addAll(first);
        for (int i = s1 - 1; i >= f0; i--) ini.lines.remove(i);
        ini.lines.addAll(f0, block);
        save(ini);
        return true;
    }

    /**
     * 1.15.1: כלי ← רשימה פנימית, באותו מזהה ועם כל ההגדרות (רשימה חכמה או כפולים שעברו לתוך רשימה ידנית).
     * כך כל הפניה אליו ממשיכה לעבוד. false — לא נמצא.
     */
    public static synchronized boolean toInner(String id, String label) {
        Ini ini = ini();
        if (!toInner(ini, id, label)) return false;
        save(ini);
        return true;
    }

    static boolean toInner(Ini ini, String id, String label) {
        int at = indexOfId(ini, id);
        if (at < 0) return false;
        ini.lines.get(at).raw = "[" + innerHeader(ini, label) + "]";
        return true;
    }

    /** כותרת פנימית פנויה: "[רשימה פנימית: label]", ואם תפוסה — (2), (3)… */
    private static String innerHeader(Ini ini, String label) {
        String h = Nested.INNER + label;
        boolean taken = true;
        for (int i = 2; taken; i++) {
            taken = false;
            for (Ini.Line l : ini.lines) if (l.isHeader() && l.headerName().trim().equals(h.trim())) taken = true;
            if (taken) h = Nested.INNER + label + " (" + i + ")";
        }
        return h;
    }

    /**
     * 1.16.0: "העתקה" של רשימה חכמה (המילון — עותק עצמאי; באג: עד כאן העתקה לתוך רשימה נהייתה קישור): רשימה פנימית
     * חדשה, במזהה חדש, עם כל המפתחות של id (חכמה, כפולים, כלי או הגדרה נפוצה). מחזיר את המזהה החדש, או null.
     */
    public static synchronized String copyInner(String id, String label) {
        Ini ini = ini();
        int at = indexOfId(ini, id);
        if (at < 0) return null;
        String fresh = newId("p"), h = innerHeader(ini, label);
        List<Ini.Line> b = new ArrayList<Ini.Line>();
        b.add(Ini.line("", h));
        b.add(Ini.line("[" + h + "]", h));
        b.add(Ini.line(ID + " = " + fresh, h));
        boolean kind = false;
        for (int i = at + 1, end = ini.sectionEnd(at); i < end; i++) {
            Ini.Line l = ini.lines.get(i);
            if (l.key == null || l.key.equals(ID)) continue;
            if (l.key.equals("סוג")) kind = true;  // תחביר
            b.add(Ini.line(l.key + " = " + l.value, h));
        }
        if (!kind) b.add(2 + 1, Ini.line("סוג = " + FnSmart.KIND, h));  // הגדרה נפוצה — בלי "סוג" // תחביר
        ini.lines.addAll(ini.lines.size(), b);
        save(ini);
        return fresh;
    }

    public static synchronized void removeFunc(String id) {
        Ini ini = ini();
        if (cut(ini, id, false)) save(ini);
    }
}
