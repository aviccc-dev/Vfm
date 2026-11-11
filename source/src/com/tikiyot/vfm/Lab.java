package com.tikiyot.vfm;

// מ0.38 — המעבדה. המשתמש (11.11.2026): "יש אפשרות להכין כמו פלאגין לתוכנה, שרק שם אתה מכניס תיקונים, במצב ארגז חול כזה,
// ושם מנסים ומריצים, ורק אחר כך אתה מכניס לקוד — כמו אפיון על רטוב" · "זה לא אישור או פסילה בהכרח, אלא עיבוד" ·
// "המעבדה זה לא רק למצב הזה, אלא גם לפיתוחים אחרים" · "ולהכין פקודה שמחליפה את הקובץ… ואולי אפשר גם לשמור גרסאות ולהחליף
// ביניהם. אז הוסף מעבדה".
// ניסוי = עמוד (html) מחוץ לקוד, שמסך המעבדה פותח (LabScreen). כל ניסוי — תיקייה בתיקיית המעבדה; כל גרסה — קובץ בה;
// הגרסה הפעילה — בקובץ ".פעילה" שבתיקייה.
// אחראי: התיקיות, השמות, הגרסאות, הגרסה הפעילה, ושתי הפקודות (הוסף / החלף). אסור לו: לכתוב מחוץ לתיקיית המעבדה.

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

    static final String ADD = "הוסף מעבדה", SWITCH = "החלף מעבדה";   // תחביר
    /** שם קובץ של ניסוי שהורד: "מעבדה <ניסוי> <גרסה>.html" (בתוך העמוד — meta, והוא קובע). */
    static final String PREFIX = "מעבדה";   // תחביר
    static final String DIR = "מעבדה", ACTIVE = ".פעילה", EXT = ".html";   // בכונן
    static final String META = "vfm-lab", META_VERSION = "vfm-lab-version";
    static final long MAX = 5L * 1024 * 1024;

    private Lab() { }

    /** תיקיית המעבדה — בתיקייה של התוכנה החדשה (לא של הישנה, שמשנה את שלה). */
    static File root() {
        return new File(Paths.dbData(), DIR);
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
        String base = fileName == null ? "" : fileName;
        if (base.toLowerCase(Locale.ROOT).endsWith(EXT)) base = base.substring(0, base.length() - EXT.length());
        base = base.replaceAll("\\s*\\(\\d+\\)$", "").trim();                       // "(1)" שהדפדפן מוסיף בהורדה חוזרת
        if (base.startsWith(PREFIX)) base = base.substring(PREFIX.length()).replaceAll("^[\\s_-]+", "");
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
        if (!src.getName().toLowerCase(Locale.ROOT).endsWith(EXT)) throw new Exception(Words.Lab.NOT_HTML);
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

    /** "הוסף מעבדה" בלי קובץ: הקובץ החדש ביותר בהורדות ששמו מתחיל ב"מעבדה" ונגמר ב־html. */
    static File newest(File downloads) {
        File best = null;
        File[] fs = downloads == null ? null : downloads.listFiles();
        if (fs != null)
            for (File f : fs)
                if (f.isFile() && f.getName().startsWith(PREFIX) && f.getName().toLowerCase(Locale.ROOT).endsWith(EXT)
                        && (best == null || f.lastModified() > best.lastModified())) best = f;
        return best;
    }

    // ---------- הפקודות ----------

    static boolean isLine(String line) {
        return line.equals(ADD) || line.startsWith(ADD + " ") || line.equals(SWITCH) || line.startsWith(SWITCH + " ");
    }

    /** מתכנן שורה של המעבדה: s.describe ו־s.op, או s.error. */
    static void plan(Batch.Step s, String line, File here, Map<String, String> vars) {
        final File root = root();
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
