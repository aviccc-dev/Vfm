package com.tikiyot.vfm;

// מבנה 186 (סידור קומת החומר): החיפוש באינדקס — פירוק המילים, ההתאמה (מלאה / רק בלי אות השימוש), "בערך" (כתיב חסר),
// חלק מהמילים, ותיקיות ריקות. עד כאן בתוך Library.
// עובד על צילום של רשימת האינדקסים שהוא מקבל, ורק קורא: אין לו מצב משלו, והוא אינו משנה דבר — לא באינדקס ולא בכונן.
// הכניסות נשארו ב־Library (search, list) — הן לוקחות את הצילום וקוראות לכאן.

import java.util.ArrayList;
import java.util.List;

final class Search {

    private Search() { }

    /**
     * מבנה 284 (אפיון החיפוש, 4 — המשתמש: "לשמור היסטוריית חיפוש עד כמות מסויימת"; "א" — רק המילים): החיפוש החדש ראשון; אותן
     * מילים שכבר היו (גם ברווחים אחרים, באותיות גדולות או סופיות) — עולות למעלה בנוסח החדש, ולא נוספות פעמיים; עד max (0 — בלי
     * היסטוריה). words ריק או null — רק החיתוך לכמות. "‖" — המפריד בשמירה — אינו נשמר בתוך חיפוש.
     */
    static List<String> remember(List<String> had, String words, int max) {
        List<String> out = new ArrayList<String>();
        String w = words == null ? "" : words.replace(HISTORY_SEP, " ").trim().replaceAll("\\s+", " ");
        if (!w.isEmpty()) out.add(w);
        String key = Text.norm(w);
        if (had != null)
            for (String h : had) {
                String t = h == null ? "" : h.trim();
                if (t.isEmpty() || (!key.isEmpty() && Text.norm(t.replaceAll("\\s+", " ")).equals(key))) continue;
                boolean twice = false;
                for (String o : out) if (Text.norm(o).equals(Text.norm(t.replaceAll("\\s+", " ")))) twice = true;
                if (!twice) out.add(t);
            }
        while (out.size() > Math.max(0, max)) out.remove(out.size() - 1);
        return out;
    }

    /** מבנה 284: ההיסטוריה בשורה אחת של ההגדרות — מופרדת ב"‖". */
    static final String HISTORY_SEP = "‖";

    static String historyText(List<String> list) {
        StringBuilder sb = new StringBuilder();
        for (String h : list) sb.append(sb.length() > 0 ? " " + HISTORY_SEP + " " : "").append(h);
        return sb.toString();
    }

    static List<String> historyOf(String text) {
        List<String> out = new ArrayList<String>();
        if (text != null) for (String h : text.split(HISTORY_SEP)) if (!h.trim().isEmpty()) out.add(h.trim());
        return out;
    }

    private static final class Term {
        Text.Pattern[] alts;
        /** לכל חלופה: האם היא הגרסה בלי אות השימוש (78). */
        boolean[] stripped;
        String plain;
    }

    /**
     * 175 — תיקיות ריקות: אין בהן אף קובץ, גם לא בתיקיות שבתוכן; רק ".nomedia" (סימון לגלריה) לא נחשב —
     * קובץ מוסתר אחר (הגדרות, מטמון של אפליקציה) — התיקייה אינה ריקה (1.7.65).
     * מוצגת רק החיצונית (כמו בתוכנות "מסיר תיקיות ריקות"), ו־innerEmpty — כמה ריקות בתוכה. בלי שורש הכונן,
     * הסל והתיקיות המוסתרות. מילים — בשם התיקייה; מקורות, כונן ותאריך — לפי Cond.matchesDir.
     */
    static final String NOMEDIA = ".nomedia", ANDROID = "Android";

    private static void emptyDirs(List<Index> snap, Library.Result res, Cond cond, List<Term> terms, boolean paths) {
        for (Index ix : snap) {
            int n = ix.dirs.size();
            int[] files = new int[n], inner = new int[n];
            for (Index.Rec r : ix.files)
                if (!r.gone && !r.name.equalsIgnoreCase(NOMEDIA) && r.dir >= 0 && r.dir < n) files[r.dir]++;
            for (int i = 0; i < n; i++) {                                   // 1.7.69: "Android" בשורש — לעולם לא ריקה:
                Index.Dir d = ix.dirs.get(i);                                   // יש בה data ו־obb, שאינם באינדקס
                if (d.parent < 0 && d.name.equalsIgnoreCase(ANDROID)) files[i]++;
            }
            for (int i = n - 1; i >= 0; i--) {                              // ההורה תמיד לפני הילד: מהסוף להתחלה
                Index.Dir d = ix.dirs.get(i);
                if (d.gone) continue;                                       // נמחקה מהכונן — לא נספרת
                if (d.parent >= 0 && d.parent < i) {
                    files[d.parent] += files[i];
                    if (files[i] == 0) inner[d.parent] += 1 + inner[i];
                }
            }
            for (int i = 0; i < n; i++) {
                Index.Dir d = ix.dirs.get(i);
                if (d.gone || d.trash || d.hidden || files[i] > 0) continue;
                if (d.parent >= 0 && files[d.parent] == 0 && !ix.dirs.get(d.parent).hidden) continue;   // לא החיצונית
                if (!terms.isEmpty() && match(terms, d.norm, ix, d.parent, paths) == 0) continue;
                if (!cond.matchesDir(ix, i)) continue;
                Library.Hit h = new Library.Hit();
                h.ix = ix;
                h.dirId = i;
                h.innerEmpty = inner[i];
                res.hits.add(h);
            }
        }
        res.total = res.hits.size();
        res.counts[Library.FULL] = res.total;
    }

    static Library.Result run(final List<Index> snap, boolean loading, String query, boolean paths, boolean loose, boolean strict) {
        if (query != null && query.indexOf(SmartLists.HERE_VAR) >= 0)          // 1.11.0: ברירת מחדל — תיקיית אופן העבודה
            query = SmartLists.withHere(query, Paths.work().getAbsolutePath());
        Library.Result res = new Library.Result();
        res.loading = loading;
        long t0 = System.nanoTime();
        String q = Text.norm(query).trim();
        res.query = q;
        if (q.isEmpty()) return res;

        Cond cond = Cond.parse(q);
        // 102: תנאי שלא הובן (למשל "גודל:2MB" בלי >) — לא מתבצע, ואומרים זאת במקום לבלוע בשקט
        if (!cond.unknown.isEmpty()) {
            StringBuilder u = new StringBuilder(Words.Lib.NOT_UNDERSTOOD);
            for (int i = 0; i < cond.unknown.size(); i++) u.append(i > 0 ? ", " : "").append(cond.unknown.get(i));
            res.note = u.toString();
        }
        // מבנה 282: רשימה שלא נמצאה — אין תוצאות, והסיבה נאמרת (ולא חיפוש בכל המכשיר במקום ברשימה)
        if (!cond.problems.isEmpty()) {
            List<String> said = Cond.parse(query).problems;                   // רק כאן: שוב, בשמות כמו שנכתבו (המנורמלת — "מסמכימ")
            if (said.isEmpty()) said = cond.problems;
            StringBuilder p = new StringBuilder();
            for (String x : said) p.append(p.length() > 0 ? " · " : "").append(x);
            res.note = p + (res.note == null || res.note.isEmpty() ? "" : " · " + res.note);
            res.ms = (System.nanoTime() - t0) / 1_000_000.0;
            return res;
        }
        List<Term> terms = new ArrayList<Term>();
        for (String w : cond.words) terms.add(term(w));
        if (cond.emptyDirs) {                                               // 175: תיקיות ריקות, לא קבצים
            emptyDirs(snap, res, cond, terms, paths);
            res.ms = (System.nanoTime() - t0) / 1_000_000.0;
            return res;
        }
        if (terms.isEmpty() && cond.tests.isEmpty() && cond.dups == null) return res;   // מבנה 207: "כפולים:" לבד — גם תנאי

        String rankWord = terms.isEmpty() ? "" : terms.get(0).plain;
        boolean onlyFilters = terms.isEmpty();

        @SuppressWarnings("unchecked")
        List<Library.Hit>[] buckets = new List[4];
        for (int i = 0; i < 4; i++) buckets[i] = new ArrayList<Library.Hit>();
        List<Library.Hit> noPrefix = new ArrayList<Library.Hit>();       // נמצא רק בלי אות השימוש — קבוצה נפרדת (78)
        java.util.Set<Object> seen = new java.util.HashSet<Object>();
        StringBuilder letters = new StringBuilder();
        for (Term t : terms)
            for (int i = 0; i < t.alts.length; i++)
                if (t.stripped[i]) {
                    String l = t.plain.substring(0, 1) + "'";
                    if (letters.indexOf(l) < 0) letters.append(letters.length() > 0 ? ", " : "").append(l);
                }
        res.prefix = letters.toString();

        final boolean multi = terms.size() > 1;
        for (Index ix : snap) {
            for (Index.Rec r : ix.files) {
                if (r.gone || Library.hide(ix, r, cond) || !cond.matches(ix, r)) continue;
                int m = onlyFilters ? 1 : match(terms, r.norm, ix, r.dir, paths);
                if (m == 0 || (strict && m == 2)) continue;                 // מבנה 94: רשימה — בלי "רק בלי אות השימוש"
                res.total++;
                if (multi || res.total <= 10) seen.add(r);           // 184: נחוץ רק לכתיב חסר (פחות מ־10) ולכמה מילים
                if (m == 2) {
                    res.counts[Library.NO_PREFIX]++;
                    if (noPrefix.size() < Library.CAP_PER_RANK) {
                        Library.Hit h = Library.hit(ix, r);
                        h.rank = 3;
                        h.section = Library.NO_PREFIX;
                        noPrefix.add(h);
                    }
                    continue;
                }
                res.counts[Library.FULL]++;
                int rank = Text.rank(r.norm, rankWord);
                if (buckets[rank].size() < Library.CAP_PER_RANK) {
                    Library.Hit h = Library.hit(ix, r);
                    h.rank = rank;
                    buckets[rank].add(h);
                }
            }
            if (cond.tests.isEmpty()) {
                for (int i = 0; i < ix.dirs.size(); i++) {
                    Index.Dir d = ix.dirs.get(i);
                    if (d.gone || onlyFilters || d.trash || d.hidden) continue;
                    if (cond.hasNot() && !cond.matchesDir(ix, i)) continue;            // 181: "לא" — גם על תיקיות
                    int m = match(terms, d.norm, ix, d.parent, paths);
                    if (m == 0 || (strict && m == 2)) continue;
                    res.total++;
                    Library.Hit h = new Library.Hit();
                    h.ix = ix;
                    h.dirId = i;
                    if (m == 2) {
                        res.counts[Library.NO_PREFIX]++;
                        h.rank = 3;
                        h.section = Library.NO_PREFIX;
                        if (noPrefix.size() < Library.CAP_PER_RANK) noPrefix.add(h);
                        continue;
                    }
                    res.counts[Library.FULL]++;
                    h.rank = Text.rank(d.norm, rankWord);
                    if (buckets[h.rank].size() < Library.CAP_PER_RANK) buckets[h.rank].add(h);
                }
            }
        }
        for (int i = 0; i < 4; i++) res.hits.addAll(buckets[i]);
        res.hits.addAll(noPrefix);

        if (cond.dups != null) {                                            // מבנה 207: "כפולים:" — מתוך מה שנמצא, רק מה שיש לו כפיל
            onlyDups(res, cond.dups);
            res.ms = (System.nanoTime() - t0) / 1_000_000.0;
            return res;
        }
        if (!strict && loose && res.total < 10 && !terms.isEmpty()) addLoose(res, cond, terms, seen, snap);
        if (!strict && terms.size() > 1) addPartial(res, cond, terms, seen, snap);   // מבנה 94: רשימה — בלי "רק חלק מהמילים"

        res.ms = (System.nanoTime() - t0) / 1_000_000.0;
        return res;
    }

    /**
     * מבנה 207: "כפולים:" — מתוך ההתאמות המלאות, רק קבצים שיש להם כפיל ביניהם, לפי אותו חישוב של מסך הכפולים (Dups.within).
     * קבצים ריקים — אינם עותקים זה של זה (כמו במסך). תיקיות — לא. "אולי התכוונת" — לא (זו רשימה של מה שנמצא).
     */
    private static void onlyDups(Library.Result res, DupMode d) {
        List<Library.Hit> files = new ArrayList<Library.Hit>();
        for (Library.Hit h : res.hits) if (h.rec != null && h.section == Library.FULL && h.rec.size > 0) files.add(h);
        java.util.Set<Index.Rec> twin = new java.util.HashSet<Index.Rec>();
        for (List<Library.Hit> g : Dups.within(d, files)) for (Library.Hit h : g) twin.add(h.rec);
        List<Library.Hit> keep = new ArrayList<Library.Hit>();
        java.util.Set<String> paths = new java.util.HashSet<String>();
        for (Library.Hit h : files) if (twin.contains(h.rec) && paths.add(h.path())) keep.add(h);   // כל קובץ פעם אחת
        res.hits.clear();
        res.hits.addAll(keep);
        res.total = keep.size();
        java.util.Arrays.fill(res.counts, 0);
        res.counts[Library.FULL] = keep.size();
    }

    /** 78: מעט תוצאות — גם בכתיב חסר (בלי יו"ד ווי"ו באמצע מילה), כקבוצה נפרדת. */
    private static void addLoose(Library.Result res, Cond cond, List<Term> terms, java.util.Set<Object> seen, List<Index> snap) {
        List<Term> l = new ArrayList<Term>();
        boolean different = false;
        for (Term t : terms) {
            String x = Text.loose(t.plain);
            if (!x.equals(t.plain)) different = true;
            l.add(term(x));
        }
        if (different) {
            int added = 0;
            for (Index ix : snap)
                for (Index.Rec r : ix.files) {
                    if (r.gone || seen.contains(r) || Library.hide(ix, r, cond) || !cond.matches(ix, r)) continue;
                    if (!matchAll(l, r.loose, ix, r.dir, false)) continue;
                    res.total++;
                    res.counts[Library.LOOSE]++;
                    seen.add(r);
                    if (added < Library.CAP_PER_RANK) {
                        Library.Hit h = Library.hit(ix, r);
                        h.rank = 3;
                        h.section = Library.LOOSE;
                        res.hits.add(h);
                        added++;
                    }
                }
        }
    }

    /** כמה מילים: גם מה שמתאים רק לחלק מהן (עד 150), כקבוצה נפרדת. */
    private static void addPartial(Library.Result res, Cond cond, List<Term> terms, java.util.Set<Object> seen, List<Index> snap) {
        int added = 0;
        for (Index ix : snap)
            for (Index.Rec r : ix.files) {
                if (added >= 150) break;
                if (r.gone || seen.contains(r) || Library.hide(ix, r, cond) || !cond.matches(ix, r)) continue;
                if (!matchAny(terms, r.norm)) continue;
                res.counts[Library.PARTIAL]++;
                Library.Hit h = Library.hit(ix, r);
                h.rank = 3;
                h.section = Library.PARTIAL;
                res.hits.add(h);
                added++;
            }
    }

    private static Term term(String word) {
        Term t = new Term();
        t.plain = word;
        List<Text.Pattern> alts = new ArrayList<Text.Pattern>();
        List<Boolean> strip = new ArrayList<Boolean>();
        for (String part : word.split("\\|")) {
            if (part.isEmpty()) continue;
            alts.add(Text.find(part));                                       // מבנה 297: הכלל האחד של מילה בחיפוש
            strip.add(false);
            String stripped = Text.withoutPrefixLetter(part);
            if (stripped != null && part.indexOf('*') < 0 && part.indexOf('?') < 0 && !Text.quote(part.charAt(part.length() - 1))) {
                alts.add(Text.wordStart(stripped));
                strip.add(true);
            }
        }
        if (alts.isEmpty()) {
            alts.add(Text.compile(word));
            strip.add(false);
        }
        t.alts = alts.toArray(new Text.Pattern[0]);
        t.stripped = new boolean[strip.size()];
        for (int i = 0; i < t.stripped.length; i++) t.stripped[i] = strip.get(i);
        return t;
    }

    /**
     * 0 — לא מתאים; 1 — התאמה מלאה; 2 — מתאים, אבל לפחות מילה אחת נמצאה רק בלי אות השימוש (78).
     */
    private static int match(List<Term> terms, String norm, Index ix, int dirId, boolean paths) {
        boolean onlyStripped = false;
        String path = null;
        for (Term t : terms) {
            int m = termMatch(t, norm);
            if (m == 0 && paths && dirId >= 0) {
                if (path == null) path = ix.normPath(dirId);            // 184: פעם אחת לכל תיקייה, לא לכל קובץ
                m = termMatch(t, path);
            }
            if (m == 0) return 0;
            if (m == 2) onlyStripped = true;
        }
        return onlyStripped ? 2 : 1;
    }

    /** 0 — לא; 1 — בחלופה רגילה; 2 — רק בחלופה בלי אות השימוש. */
    private static int termMatch(Term t, String s) {
        int best = 0;
        for (int i = 0; i < t.alts.length; i++)
            if (t.alts[i].matches(s)) {
                if (!t.stripped[i]) return 1;
                best = 2;
            }
        return best;
    }

    private static boolean matchAll(List<Term> terms, String norm, Index ix, int dirId, boolean paths) {
        return match(terms, norm, ix, dirId, paths) != 0;
    }

    private static boolean matchAny(List<Term> terms, String norm) {
        for (Term t : terms)
            for (Text.Pattern p : t.alts)
                if (p.matches(norm)) return true;
        return false;
    }
}
