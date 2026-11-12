package com.tikiyot.vfm;

// מ0.38 — המעבדה. המשתמש (11.11.2026): "יש אפשרות להכין כמו פלאגין לתוכנה, שרק שם אתה מכניס תיקונים, במצב ארגז חול כזה,
// ושם מנסים ומריצים, ורק אחר כך אתה מכניס לקוד — כמו אפיון על רטוב" · "זה לא אישור או פסילה בהכרח, אלא עיבוד" ·
// "המעבדה זה לא רק למצב הזה, אלא גם לפיתוחים אחרים" · "ולהכין פקודה שמחליפה את הקובץ… ואולי אפשר גם לשמור גרסאות ולהחליף
// ביניהם. אז הוסף מעבדה".
// ניסוי = עמוד (html) מחוץ לקוד, שמסך המעבדה פותח (LabScreen). כל ניסוי — תיקייה בתיקיית המעבדה; כל גרסה — קובץ בה;
// הגרסה הפעילה — בקובץ ".פעילה" שבתיקייה.
// מ0.40 — התוספים. המשתמש (12.11.2026): "הוא כבר לא אמור להיקרא מעבדה, אלא תוסף עורך אינטראקטיבי" · "ומקומו בתוך ספריית
// התוכנה, בתיקיית תוספים". אותו מנגנון: תוסף = עמוד; תיקייה לכל תוסף, קובץ לכל גרסה. התיקייה "מעבדה" עוברת ל"תוספים" (פעם
// אחת), והפקודות הישנות (הוסף מעבדה, החלף מעבדה) ממשיכות לעבוד.
// אחראי: התיקיות, השמות, הגרסאות, הגרסה הפעילה, ושתי הפקודות (הוסף / החלף). אסור לו: לכתוב מחוץ לתיקיית התוספים.

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class Lab {

    static final String ADD = "הוסף תוסף", SWITCH = "החלף תוסף";   // תחביר — מ0.40
    /** עד מ0.39 — ממשיכות לעבוד. */
    static final String OLD_ADD = "הוסף מעבדה", OLD_SWITCH = "החלף מעבדה";   // תחביר
    /** שם קובץ של תוסף שהורד: "תוסף <שם> <גרסה>.html" (או "מעבדה …"; בתוך העמוד — meta, והוא קובע). */
    static final String PREFIX = "תוסף", OLD_PREFIX = "מעבדה";   // תחביר
    static final String DIR = "תוספים", OLD_DIR = "מעבדה", ACTIVE = ".פעילה", EXT = ".html";   // בכונן
    static final String META = "vfm-plugin", META_VERSION = "vfm-plugin-version", OLD_META = "vfm-lab", OLD_META_VERSION = "vfm-lab-version";
    static final long MAX = 5L * 1024 * 1024;

    private Lab() { }

    /** תיקיית התוספים — בתיקייה של התוכנה החדשה. מ0.40: תיקיית "מעבדה" שעוד נשארה — עוברת לכאן (פעם אחת). */
    static File root() {
        File r = new File(Paths.dbData(), DIR);
        if (!r.exists()) {
            File old = new File(Paths.dbData(), OLD_DIR);
            if (old.isDirectory() && !old.renameTo(r)) Log.i("Lab.root: התיקייה \"" + OLD_DIR + "\" לא עברה ל\"" + DIR + "\"");   // בכונן
        }
        return r;
    }

    // ---------- שמות ----------

    /** שם שמותר בכונן — כמו בעורך: לא משאירים דבר לא תקין, מתקנים לדומה. */
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

    /** {ניסוי, גרסה}: מה שכתוב בעמוד (meta), ואם אין — משם הקובץ ("מעבדה עורך 3.html" ← עורך, 3). */
    static String[] nameOf(String html, String fileName) {
        String exp = meta(html, META), ver = meta(html, META_VERSION);
        if (exp == null || exp.isEmpty()) exp = meta(html, OLD_META);
        if (ver == null || ver.isEmpty()) ver = meta(html, OLD_META_VERSION);
        String base = fileName == null ? "" : fileName;
        if (base.toLowerCase(Locale.ROOT).endsWith(EXT)) base = base.substring(0, base.length() - EXT.length());
        else if (base.toLowerCase(Locale.ROOT).endsWith(".htm")) base = base.substring(0, base.length() - 4);
        base = base.replaceAll("\\s*\\(\\d+\\)$", "").trim();                       // "(1)" שהדפדפן מוסיף בהורדה חוזרת
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
        if (ver.isEmpty()) ver = new java.text.SimpleDateFormat("yyyy-MM-dd HH-mm", Locale.ROOT).format(new java.util.Date());
        return new String[]{exp, ver};
    }

    // ---------- ניסויים וגרסאות ----------

    static List<String> experiments(File root) {
        List<String> out = new ArrayList<String>();
        File[] ds = root.listFiles();
        if (ds != null) for (File d : ds) if (d.isDirectory() && !versions(root, d.getName()).isEmpty()) out.add(d.getName());
        Collections.sort(out);
        return out;
    }

    /** הגרסאות — לפי הסדר שבו נוספו (הישנה קודם). */
    static List<String> versions(File root, String exp) {
        File[] fs = new File(root, exp).listFiles();
        List<File> l = new ArrayList<File>();
        if (fs != null) for (File f : fs) if (f.isFile() && f.getName().endsWith(EXT)) l.add(f);
        Collections.sort(l, new Comparator<File>() {
            @Override public int compare(File a, File b) {
                int c = Long.compare(a.lastModified(), b.lastModified());
                return c != 0 ? c : a.getName().compareTo(b.getName());
            }
        });
        List<String> out = new ArrayList<String>();
        for (File f : l) out.add(f.getName().substring(0, f.getName().length() - EXT.length()));
        return out;
    }

    /** הגרסה הפעילה; אם לא נקבעה (או נמחקה) — האחרונה שנוספה. null — אין גרסאות. */
    static String active(File root, String exp) {
        List<String> vs = versions(root, exp);
        if (vs.isEmpty()) return null;
        try {
            File a = new File(new File(root, exp), ACTIVE);
            if (a.isFile()) {
                String v = Files.readUtf8(a).trim();
                if (vs.contains(v)) return v;
            }
        } catch (Throwable t) {
            Log.e("Lab.active", t);
        }
        return vs.get(vs.size() - 1);
    }

    static File page(File root, String exp) {
        String v = active(root, exp);
        return v == null ? null : new File(new File(root, exp), v + EXT);
    }

    /** null — הצליח; אחרת השגיאה. */
    static String setActive(File root, String exp, String ver) {
        if (!versions(root, exp).contains(ver)) return experiments(root).contains(exp) ? Words.Lab.noVersion(exp, ver) : Words.Lab.noExperiment(exp);
        try {
            Files.writeAtomic(new File(new File(root, exp), ACTIVE), ver);
            return null;
        } catch (Throwable t) {
            Log.e("Lab.setActive", t);
            return Words.Db.failed(String.valueOf(t.getMessage()));
        }
    }

    /** מוסיף גרסה (ואותה גרסה שוב — מחליפה), והיא נעשית הפעילה. מחזיר {ניסוי, גרסה}, או זורק עם ההודעה. */
    static String[] add(File root, File src) throws Exception {
        if (src == null || !src.isFile()) throw new Exception(Words.Lab.NO_FILE);
        if (!isHtml(src.getName())) throw new Exception(Words.Lab.NOT_HTML);
        if (src.length() > MAX) throw new Exception(Words.Lab.TOO_BIG);
        String html = Files.readUtf8(src);
        String[] nv = nameOf(html, src.getName());
        File dir = new File(root, nv[0]);
        if (!dir.isDirectory() && !dir.mkdirs()) throw new Exception(Words.Lab.NO_DIR);
        File to = new File(dir, nv[1] + EXT);
        Files.writeAtomic(to, html);
        to.setLastModified(System.currentTimeMillis());
        Files.writeAtomic(new File(dir, ACTIVE), nv[1]);
        return nv;
    }

    /**
     * "הוסף מעבדה" בלי קובץ: עמוד המעבדה החדש ביותר בהורדות (וגם בתיקייה אחת פנימה) — לפי מה שכתוב בו (meta), לא לפי השם:
     * מ0.39 — המשתמש: "תראה איזה באג… אין בהורדות קובץ ששמו מתחיל ב'מעבדה'" (השם שההורדה נתנה לקובץ — לא בידינו).
     */
    static File newest(File downloads) {
        List<File> all = new ArrayList<File>();
        collect(downloads, 0, all);
        File best = null;
        for (File f : all)
            if ((best == null || f.lastModified() > best.lastModified())
                    && (f.getName().startsWith(PREFIX) || f.getName().startsWith(OLD_PREFIX) || isPage(f))) best = f;
        return best;
    }

    private static void collect(File d, int depth, List<File> out) {
        File[] fs = d == null ? null : d.listFiles();
        if (fs == null) return;
        for (File f : fs) {
            if (f.isDirectory()) { if (depth < 1 && !f.getName().startsWith(".")) collect(f, depth + 1, out); }
            else if (isHtml(f.getName())) out.add(f);
        }
    }

    static boolean isHtml(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        return n.endsWith(EXT) || n.endsWith(".htm");
    }

    /** עמוד של תוסף — יש בו meta name="vfm-plugin" (או "vfm-lab", עד מ0.39) בתחילת הקובץ. */
    static boolean isPage(File f) {
        if (f.length() > MAX) return false;
        java.io.InputStream in = null;
        try {
            in = new java.io.FileInputStream(f);
            byte[] b = new byte[8192];
            int n = in.read(b);
            String head = n > 0 ? new String(b, 0, n, "UTF-8") : "";
            return head.contains(META + "\"") || head.contains(OLD_META + "\"");
        } catch (Throwable t) {
            return false;
        } finally {
            if (in != null) try { in.close(); } catch (Throwable ignored) { }
        }
    }

    // ---------- הפקודות ----------

    static boolean isLine(String line) {
        for (String w : new String[]{ADD, SWITCH, OLD_ADD, OLD_SWITCH}) if (line.equals(w) || line.startsWith(w + " ")) return true;
        return false;
    }

    /** "הוסף מעבדה …" ← "הוסף תוסף …" (וכך גם החלף). */
    static String current(String line) {
        if (line.startsWith(OLD_ADD)) return ADD + line.substring(OLD_ADD.length());
        if (line.startsWith(OLD_SWITCH)) return SWITCH + line.substring(OLD_SWITCH.length());
        return line;
    }

    /** מתכנן שורה של המעבדה: s.describe ו־s.op, או s.error. */
    static void plan(Batch.Step s, String line, File here, Map<String, String> vars) {
        final File root = root();
        line = current(line);
        if (line.startsWith(ADD)) {
            List<String> tk = Batch.tokens(line.substring(ADD.length()));
            if (tk.size() > 1) { s.error = Words.Lab.ADD_USAGE; return; }
            File src;
            if (tk.isEmpty()) {
                String dl = vars.get(Batch.V_DOWNLOADS);
                src = newest(dl == null ? null : new File(dl));
                if (src == null) { s.error = Words.Lab.NONE_IN_DOWNLOADS; return; }
            } else {
                String[] err = {null};
                String p = Batch.path(tk.get(0), here, vars, err);
                if (p == null) { s.error = err[0]; return; }
                src = new File(p);
                if (!src.isFile()) { s.error = Words.Lab.NO_FILE + ": " + Bidi.path(p); return; }
            }
            final File f = src;
            s.describe = Words.Lab.addWill(f.getName());
            s.op = new ListCmd.Op() {
                @Override public String run(Library lib) {
                    try {
                        String[] nv = add(root, f);
                        // מ0.39 — המשתמש: "הקובץ נעלם, בדקתי בקבצים פיזית" (מההורדות, אחרי ההוספה). המעבדה רק קוראת אותו;
                        // ביומן — שהמקור עוד שם אחרי ההוספה, כדי שדוח האבחון יראה אם משהו אחר מחק אותו.
                        Log.i("הוסף תוסף: " + f.getAbsolutePath() + " · המקור אחרי ההוספה: " + (f.isFile() ? "קיים" : "חסר"));   // בכונן
                        return Words.Lab.added(nv[0], nv[1]);
                    } catch (Throwable t) {
                        DbListCmd.lastFailed = true;
                        return String.valueOf(t.getMessage());
                    }
                }
            };
            return;
        }
        List<String> tk = Batch.tokens(line.substring(SWITCH.length()));
        if (tk.size() != 2) { s.error = Words.Lab.SWITCH_USAGE; return; }
        final String exp = tk.get(0).trim(), ver = tk.get(1).trim();
        s.describe = Words.Lab.switchWill(exp, ver);
        s.op = new ListCmd.Op() {
            @Override public String run(Library lib) {
                String err = setActive(root, exp, ver);
                if (err != null) { DbListCmd.lastFailed = true; return err; }
                return Words.Lab.switched(exp, ver);
            }
        };
    }
}
