package com.tikiyot.vfm;

// מ0.38 — המעבדה. המשתמש (11.11.2026): "יש אפשרות להכין כמו פלאגין לתוכנה, שרק שם אתה מכניס תיקונים, במצב ארגז חול כזה,
// ושם מנסים ומריצים, ורק אחר כך אתה מכניס לקוד — כמו אפיון על רטוב". ניסוי = עמוד (html) מחוץ לקוד, שהמסך פותח (LabScreen).
// מ0.40 — התוספים. המשתמש (12.11.2026): "הוא כבר לא אמור להיקרא מעבדה, אלא תוסף עורך אינטראקטיבי" · "ומקומו בתוך ספריית
// התוכנה, בתיקיית תוספים" · "בעיקרון התוספים כולם אמורים להיות בספריה, עם אפשרות בחירה לפי גרסה. בתוך הקובץ אמורה להישמר
// מספר הגרסה. המריץ מעביר את כולם, ואם צריך משנה שם. והמריץ מריץ את מה שנראה חדש. ואם יש יותר חדש — הוא מזוהה בתוסף".
// לכן: כל עמוד בתיקיית התוספים הוא גרסה; השם והגרסה — מתוך העמוד (meta). "פתח תוסף" — החדש לפי התאריך; גרסה אחרת — בפקודה
// או במסך. העמודים מגיעים לתיקייה ב"העבר" הרגיל (כלל יסודי, 12.11: "תמיד לחפש בפונקציות קיימות"), ולכן "הוסף תוסף" ו"החלף
// תוסף" ירדו. עמודים מעד מ0.39 (תיקייה לכל תוסף, קובץ לכל גרסה) — עוברים פעם אחת ישר לתיקייה (flatten).
// אחראי: התיקייה, השמות, הגרסאות. אסור לו: לכתוב קבצים (מלבד המעבר החד־פעמי של התיקייה "מעבדה").

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class Lab {

    /** ירדו במ0.40 — שורה כזו היא שגיאה עם הסבר (ולא "הוסף" של הפעלים). */
    static final String ADD = "הוסף תוסף", SWITCH = "החלף תוסף", OLD_ADD = "הוסף מעבדה", OLD_SWITCH = "החלף מעבדה";   // תחביר
    /** שם קובץ של תוסף שהורד: "תוסף <שם> <גרסה>.html" (או "מעבדה …"; בתוך העמוד — meta, והוא קובע). */
    static final String PREFIX = "תוסף", OLD_PREFIX = "מעבדה";   // תחביר
    static final String DIR = "תוספים", OLD_DIR = "מעבדה", EXT = ".html";   // בכונן
    static final String META = "vfm-plugin", META_VERSION = "vfm-plugin-version", OLD_META = "vfm-lab", OLD_META_VERSION = "vfm-lab-version";
    /** כמה מתחילת העמוד נקרא כדי למצוא את השם והגרסה (ה־meta בראש העמוד). */
    private static final int HEAD = 8192;

    private Lab() { }

    /**
     * תיקיית התוספים — בתיקייה של התוכנה החדשה. מ0.40: התיקייה "מעבדה" עוברת לכאן; ואם "תוספים" כבר נוצרה (למשל בשורת
     * "צור תיקייה" של המריץ) — מה שבתוך "מעבדה" עובר פריט פריט, ושם תפוס נשאר במקומו.
     */
    static File root() {
        File r = new File(Paths.dbData(), DIR), old = new File(Paths.dbData(), OLD_DIR);
        if (old.isDirectory()) {
            if (!r.exists()) {
                if (!old.renameTo(r)) Log.i("Lab.root: התיקייה \"" + OLD_DIR + "\" לא עברה ל\"" + DIR + "\"");   // בכונן
            } else {
                File[] in = old.listFiles();
                if (in != null) for (File k : in) if (!new File(r, k.getName()).exists()) k.renameTo(new File(r, k.getName()));
                String[] left = old.list();
                if (left != null && left.length == 0) old.delete();
            }
        }
        flatten(r);
        return r;
    }

    /** קובץ "הגרסה הפעילה" של עד מ0.39 — כבר לא בשימוש. */
    private static final String OLD_ACTIVE = ".פעילה";   // בכונן

    /**
     * מ0.40: עמודים מעד מ0.39 (תיקייה לכל תוסף, "<גרסה>.html") — עוברים ישר לתיקיית התוספים, בשם "תוסף <שם> <גרסה>.html"
     * (אותו עמוד כבר שם — העותק הישן יוצא; שם תפוס עם תוכן אחר — "(2)"); ".פעילה" ותיקייה שהתרוקנה — יוצאות. כך כל התוספים במקום אחד, ו"יש כבר זהה" (העבר /תפוס:לשנות-שם)
     * נבדק מול כולם.
     */
    private static void flatten(File r) {
        File[] ds = r.listFiles();
        if (ds == null) return;
        for (File d : ds) {
            if (!d.isDirectory() || d.getName().startsWith(".")) continue;
            File[] in = d.listFiles();
            if (in == null) continue;
            for (File f : in) {
                if (!f.isFile() || !isHtml(f.getName())) continue;
                Page p = page(f, d.getName());
                String n = safe(PREFIX + " " + p.name + " " + p.ver) + EXT;
                File to = new File(r, n);
                if (to.isFile() && to.length() == f.length() && Sig.of(to) != null && Sig.of(to).equals(Sig.of(f))) {
                    f.delete();                                                  // אותו עמוד כבר בתיקייה (המריץ העביר אותו מההורדות) — לא מכפילים
                    continue;
                }
                if (to.exists()) to = new File(r, Physical.freeName(r, n, false));
                if (!f.renameTo(to)) Log.i("Lab.flatten: לא עבר " + f);   // בכונן
            }
            new File(d, OLD_ACTIVE).delete();
            String[] left = d.list();
            if (left != null && left.length == 0) d.delete();
        }
    }

    // ---------- שמות ----------

    /** שם שמותר בכונן — כמו בעורך: לא משאירים דבר לא תקין, מתקנים לדומה. */
    /** התוספים שבאים עם התוכנה (בקובץ ההתקנה). */
    static final String BUNDLED = "plugins";

    /** מ0.42: הקבצים שתוסף כותב (קובצי עזר, תוצאות) — תיקייה לכל תוסף, מחוץ לתיקיית התוספים (שם כל html הוא גרסה). */
    static final String DATA_DIR = "נתוני תוספים";   // בכונן

    static File dataDir(String plugin) {
        File d = new File(new File(Paths.dbData(), DATA_DIR), safe(plugin == null ? "" : plugin));
        d.mkdirs();
        return d;
    }

    /**
     * מ0.41 (המשתמש: "להוסיף כפתור שמריץ אותו. וכך הוא הופך להיות חלק אינטגרלי מהתוכנה"): תוסף שבא עם התוכנה נכנס לתיקיית
     * התוספים — קובץ רגיל שם, כמו כל גרסה אחרת — אם אין שם כבר אותו תוסף באותה גרסה. html — תוכן העמוד; fileName — שמו
     * בהתקנה (לשם ולגרסה, אם אינם כתובים בעמוד). מחזיר את הקובץ שנכתב, או null — כבר יש, או נכשל.
     */
    static File addBundled(File root, String html, String fileName) {
        String[] nv = nameOf(html, fileName);
        if (version(root, nv[0], nv[1]) != null) return null;
        String n = safe(PREFIX + " " + nv[0] + " " + nv[1]) + EXT;
        File to = new File(root, n);
        if (to.exists()) to = new File(root, Physical.freeName(root, n, false));
        try {
            if (!root.isDirectory() && !root.mkdirs()) return null;
            Files.writeAtomic(to, html);
            return to;
        } catch (Throwable t) {
            Log.e("Lab.addBundled", t);
            return null;
        }
    }

    static String safe(String s) {
        String t = s == null ? "" : s.replace('"', '״').replaceAll("[\\\\/:|]", "-").replace('<', '(').replace('>', ')')
                .replaceAll("[*?\\n\\r\\t]", "").trim();
        while (t.startsWith(".")) t = t.substring(1).trim();
        return t;
    }

    private static String meta(String html, String name) {
        Matcher m = Pattern.compile("<meta\\s+[^>]*name\\s*=\\s*[\"']" + Pattern.quote(name) + "[\"'][^>]*>", Pattern.CASE_INSENSITIVE).matcher(html);
        if (!m.find()) return null;
        Matcher c = Pattern.compile("content\\s*=\\s*[\"']([^\"']*)[\"']", Pattern.CASE_INSENSITIVE).matcher(m.group());
        return c.find() ? c.group(1).trim() : null;
    }

    private static String meta2(String html, String name, String old) {
        String v = meta(html, name);
        return v == null || v.isEmpty() ? meta(html, old) : v;
    }

    /** {תוסף, גרסה}: מה שכתוב בעמוד (meta), ואם אין — משם הקובץ ("תוסף עורך 3.html" ← עורך, 3). */
    static String[] nameOf(String html, String fileName) {
        String exp = meta2(html, META, OLD_META), ver = meta2(html, META_VERSION, OLD_META_VERSION);
        String base = fileName == null ? "" : fileName;
        if (base.toLowerCase(Locale.ROOT).endsWith(EXT)) base = base.substring(0, base.length() - EXT.length());
        else if (base.toLowerCase(Locale.ROOT).endsWith(".htm")) base = base.substring(0, base.length() - 4);
        base = base.replaceAll("\\s*\\(\\d+\\)$", "").trim();                       // "(1)" שהדפדפן מוסיף, "(2)" של שם תפוס
        for (String pre : new String[]{PREFIX, OLD_PREFIX})
            if (base.startsWith(pre)) {
                base = base.substring(pre.length()).replaceAll("^[\\s_-]+", "");
                break;
            }
        if (exp == null || exp.isEmpty()) {
            Matcher m = Pattern.compile("^(.*?)[\\s_-]+(\\d+)$").matcher(base);
            if (m.find()) {
                exp = m.group(1);
                if (ver == null || ver.isEmpty()) ver = m.group(2);
            } else exp = base;
        }
        exp = safe(exp);
        if (exp.isEmpty()) exp = Words.Lab.UNNAMED;
        ver = safe(ver == null ? "" : ver);
        if (ver.isEmpty()) ver = "?";
        return new String[]{exp, ver};
    }

    static boolean isHtml(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        return n.endsWith(EXT) || n.endsWith(".htm");
    }

    /** תחילת העמוד (עד HEAD בתים) — שם נמצאים השם והגרסה. */
    private static String head(File f) {
        java.io.InputStream in = null;
        try {
            in = new java.io.FileInputStream(f);
            byte[] b = new byte[HEAD];
            int n = 0, r;
            while (n < b.length && (r = in.read(b, n, b.length - n)) > 0) n += r;
            return n > 0 ? new String(b, 0, n, "UTF-8") : "";
        } catch (Throwable t) {
            return "";
        } finally {
            if (in != null) try { in.close(); } catch (Throwable ignored) { }
        }
    }

    // ---------- העמודים שבתיקייה ----------

    /** עמוד אחד: הקובץ, ומה שכתוב בו — שם התוסף והגרסה. */
    static final class Page {
        final File file;
        final String name, ver;

        Page(File file, String name, String ver) {
            this.file = file;
            this.name = name;
            this.ver = ver;
        }
    }

    /** הקובץ כעמוד. folder — שם התיקייה שהוא בה (עד מ0.39: התיקייה = התוסף, הקובץ = הגרסה), או null — ישר בתיקיית התוספים. */
    static Page page(File f, String folder) {
        String h = head(f);
        if (folder == null) {
            String[] nv = nameOf(h, f.getName());
            return new Page(f, nv[0], nv[1]);
        }
        String exp = meta2(h, META, OLD_META), ver = meta2(h, META_VERSION, OLD_META_VERSION);
        String stem = f.getName().substring(0, f.getName().lastIndexOf('.'));
        return new Page(f, safe(exp == null || exp.isEmpty() ? folder : exp), safe(ver == null || ver.isEmpty() ? stem : ver));
    }

    /** כל העמודים: ישר בתיקיית התוספים, ובתיקייה אחת פנימה (מה שנשמר עד מ0.39). קבצים ותיקיות שמתחילים בנקודה — לא. */
    static List<Page> pages(File root) {
        List<Page> out = new ArrayList<Page>();
        File[] fs = root.listFiles();
        if (fs == null) return out;
        for (File f : fs) {
            if (f.getName().startsWith(".")) continue;
            if (f.isDirectory()) {
                File[] in = f.listFiles();
                if (in != null) for (File g : in) if (g.isFile() && !g.getName().startsWith(".") && isHtml(g.getName())) out.add(page(g, f.getName()));
            } else if (isHtml(f.getName())) out.add(page(f, null));
        }
        return out;
    }

    /** שמות התוספים, לפי הא״ב. */
    static List<String> names(File root) {
        TreeSet<String> s = new TreeSet<String>();
        for (Page p : pages(root)) s.add(p.name);
        return new ArrayList<String>(s);
    }

    /** הגרסאות של תוסף — החדשה (לפי התאריך) קודם. */
    static List<Page> versions(File root, String name) {
        List<Page> out = new ArrayList<Page>();
        for (Page p : pages(root)) if (p.name.equals(name)) out.add(p);
        Collections.sort(out, new Comparator<Page>() {
            @Override public int compare(Page a, Page b) {
                int c = Long.compare(b.file.lastModified(), a.file.lastModified());
                return c != 0 ? c : -compareVer(a.ver, b.ver);
            }
        });
        return out;
    }

    /** "מה שנראה חדש": הקובץ החדש ביותר לפי התאריך. null — אין תוסף כזה. */
    static Page newest(File root, String name) {
        List<Page> v = versions(root, name);
        return v.isEmpty() ? null : v.get(0);
    }

    /** גרסה מסוימת (כמה קבצים באותה גרסה — החדש לפי התאריך). null — אין. */
    static Page version(File root, String name, String ver) {
        for (Page p : versions(root, name)) if (p.ver.equals(ver)) return p;
        return null;
    }

    /** "ואם יש יותר חדש — הוא מזוהה": הגרסה הגבוהה ביותר של התוסף, אם היא גבוהה מזו שנפתחה; אחרת null. */
    static String higher(File root, Page cur) {
        String best = null;
        for (Page p : versions(root, cur.name)) if (compareVer(p.ver, best == null ? cur.ver : best) > 0) best = p.ver;
        return best;
    }

    /** השוואת גרסאות: מספרים — כמספרים ("10" אחרי "9", "5.10" אחרי "5.2"), והשאר — כטקסט. */
    static int compareVer(String a, String b) {
        Matcher x = Pattern.compile("\\d+|\\D+").matcher(a), y = Pattern.compile("\\d+|\\D+").matcher(b);
        while (true) {
            boolean hx = x.find(), hy = y.find();
            if (!hx || !hy) return hx ? 1 : hy ? -1 : 0;
            String s = x.group(), t = y.group();
            int c;
            if (Character.isDigit(s.charAt(0)) && Character.isDigit(t.charAt(0))) {
                String s2 = s.replaceFirst("^0+(?=\\d)", ""), t2 = t.replaceFirst("^0+(?=\\d)", "");
                c = s2.length() != t2.length() ? Integer.compare(s2.length(), t2.length()) : s2.compareTo(t2);
            } else c = s.compareTo(t);
            if (c != 0) return c;
        }
    }

    // ---------- הפקודות שירדו ----------

    static boolean isLine(String line) {
        for (String w : new String[]{ADD, SWITCH, OLD_ADD, OLD_SWITCH}) if (line.equals(w) || line.startsWith(w + " ")) return true;
        return false;
    }

    /** "הוסף תוסף" / "החלף תוסף" (וגם "… מעבדה"): ירדו במ0.40 — שגיאה שאומרת מה במקומן (ולא "הוסף" של הפעלים, שמבלבל). */
    static void plan(Batch.Step s) {
        s.error = Words.Lab.GONE;
    }
}
