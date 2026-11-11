package com.tikiyot.vfm;

// מ0.30: דוח אבחון — פעולה אחת שבודקת במכשיר את מה שעד עכשיו נבדק ביד, וכותבת קובץ אחד לתיקיית ההורדות.
// המשתמש (11.11.2026), על רשימת בדיקות ידניות: "אין דרך שתכניס בפקודות את כל זה? זה מאד מייגע."
// אחראי: לקרוא, למדוד ולדווח. כל ניסוי שכותב — על עותק של המסד (בתיקיית המטמון), עם מונה מספרים בזיכרון.
// אסור לו: לכתוב במסד אמיתי, בקובץ המונה או בתיקיית הגיבויים. (היוצא מן הכלל: "הבדיקה הכללית", שרצה כמו מהתפריט.)
// הטקסט של הדוח הוא יומן לבדיקה, לא הודעות למסך — ולכן אינו במילון (כמו שורות Log).

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

final class Diag {

    static final String FILE = "דוח אבחון";

    private Diag() { }

    /** מהתפריט: רץ ברקע, כותב את הקובץ, ומציע לשלוח אותו. */
    static void run(final Activity a, final String mode) {
        final Physical.Busy busy = new Physical.Busy(a, Words.DbCmd.DIAG_RUNNING);
        new Thread(new Runnable() {
            @Override
            public void run() {
                String ver = "";
                try {
                    ver = " " + a.getPackageManager().getPackageInfo(a.getPackageName(), 0).versionName;
                } catch (Throwable ignored) {
                    // בלי מספר גרסה בשם
                }
                final File out = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), FILE + ver + ".txt");
                StringBuilder sb = new StringBuilder();
                String err = null;
                try {
                    report(a, mode, sb);
                    write(out, sb);
                    modes(a, sb);
                    write(out, sb);
                    general(a, busy, sb);                                         // אחרון: אם ייתקע — הדוח שלפניו כבר כתוב
                    write(out, sb);
                } catch (Throwable t) {
                    Log.e("Diag", t);
                    err = String.valueOf(t);
                    try {
                        sb.append("\n!!! הדוח נקטע: ").append(stack(t));
                        write(out, sb);
                    } catch (Throwable ignored) {
                        // אין לאן לכתוב
                    }
                }
                busy.close();
                final String e = err;
                a.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!out.isFile()) {
                            Ui.toast(a, Words.Db.failed(String.valueOf(e)));
                            return;
                        }
                        Ui.ask(a, Words.DbCmd.MENU_DIAG, Words.DbCmd.diagDone(Bidi.path(out.getAbsolutePath())), null, null, Words.DbCmd.DIAG_SEND,
                                new Runnable() {
                                    @Override public void run() { Acts.sendReady(a, Collections.singletonList(out)); }
                                });
                    }
                });
            }
        }).start();
    }

    private static void write(File out, StringBuilder sb) throws Exception {
        File dir = out.getParentFile();
        if (dir != null && !dir.isDirectory()) dir.mkdirs();
        Files.writeAtomic(out, sb.toString());
    }

    private static String stack(Throwable t) {
        StringWriter w = new StringWriter();
        t.printStackTrace(new PrintWriter(w));
        return w.toString();
    }

    private interface Part {
        void run(StringBuilder sb) throws Throwable;
    }

    /** חלק אחד של הדוח: כותרת, זמן, ותקלה — נכתבת ולא עוצרת את השאר. */
    private static void part(StringBuilder sb, String title, Part p) {
        sb.append("\n=== ").append(title).append(" ===\n");
        long t0 = System.nanoTime();
        try {
            p.run(sb);
        } catch (Throwable t) {
            sb.append("!!! תקלה: ").append(stack(t)).append('\n');
        }
        sb.append("(").append((System.nanoTime() - t0) / 1000000).append(" מ\"ש)\n");
    }

    private static String ok(boolean b) {
        return b ? "✅ " : "❌ ";
    }

    /** עותק של המסד של האופן, פתוח לכתיבה, עם מונה בזיכרון. null — אין מסד. */
    private static SqlStore copy(Activity a, String mode, String name) throws Exception {
        File src = DbScreen.dbFile(a, mode);
        if (!src.isFile()) return null;
        File dir = new File(a.getCacheDir(), "diag");
        dir.mkdirs();
        File to = new File(dir, name + ".db");
        wipe(to);
        try {
            SqlStore live = SqlStore.openReadOnly(src);
            try {
                live.copyTo(to);
            } finally {
                live.close();
            }
        } catch (Throwable t) {                                               // חיבור לקריאה בלבד שלא מעתיק — העתקת הקבצים עצמם
            Log.e("Diag.copy", t);
            wipe(to);
            raw(src, to);
            File wal = new File(src.getPath() + "-wal");
            if (wal.isFile()) raw(wal, new File(to.getPath() + "-wal"));
        }
        return SqlStore.open(to, IdCounter.memory());
    }

    private static void raw(File from, File to) throws Exception {
        java.io.FileInputStream in = new java.io.FileInputStream(from);
        try {
            java.io.FileOutputStream out = new java.io.FileOutputStream(to);
            try {
                byte[] b = new byte[65536];
                for (int n; (n = in.read(b)) > 0; ) out.write(b, 0, n);
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }

    private static SqlStore fresh(Activity a, String name) {
        File dir = new File(a.getCacheDir(), "diag");
        dir.mkdirs();
        File to = new File(dir, name + ".db");
        wipe(to);
        return SqlStore.open(to, IdCounter.memory());
    }

    private static void wipe(File db) {
        for (String x : new String[]{"", "-wal", "-shm", "-journal"}) new File(db.getPath() + x).delete();
    }

    private static List<String> names(Store s, long list) {
        List<String> out = new ArrayList<String>();
        for (Store.Member m : s.members(list))
            out.add((m.item.type == Store.LIST ? "📁" : m.item.type == Store.SMART ? "🔎" : m.item.type == Store.FILE ? "📄" : "·") + m.item.name);
        return out;
    }

    private static List<Long> homes(Store s) {
        List<Long> out = new ArrayList<Long>();
        for (Store.Item it : s.lists()) if ("1".equals(s.prop(it.id, Store.P_HOME))) out.add(it.id);
        return out;
    }

    private static void report(final Activity a, final String mode, StringBuilder sb) {
        final String internal = Environment.getExternalStorageDirectory().getAbsolutePath();
        sb.append("דוח אבחון · ").append(new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.US).format(new Date())).append('\n');

        part(sb, "1. המכשיר והתוכנה", new Part() {
            @Override public void run(StringBuilder sb) throws Throwable {
                android.content.pm.PackageInfo pi = a.getPackageManager().getPackageInfo(a.getPackageName(), 0);
                sb.append("גרסה: ").append(pi.versionName).append(" (").append(pi.versionCode).append(") · ").append(a.getPackageName()).append('\n');
                sb.append("אנדרואיד: ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append(") · ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append('\n');
                if (Build.VERSION.SDK_INT >= 30) sb.append(ok(Environment.isExternalStorageManager())).append("הרשאת גישה לכל הקבצים\n");
                sb.append("מקום פנוי: ").append(new File(internal).getUsableSpace() / (1024 * 1024)).append("MB\n");
                sb.append("תיקיית התוכנה: ").append(Paths.data()).append('\n');
                sb.append("תיקיית המסדים: ").append(DbScreen.dbFile(a, "x").getParent()).append('\n');
                sb.append("האופן שעל המסך: ").append(mode).append(" · האופן של הפקודות: ").append(DbListScreen.currentMode)
                        .append(" · האופן השמור: ").append(DbScreen.savedMode(a)).append('\n');
                Intent send = new Intent(Intent.ACTION_SEND).setType("*/*").setPackage(a.getPackageName());
                int n = a.getPackageManager().queryIntentActivities(send, 0).size();
                sb.append(ok(n > 0)).append("התוכנה רשומה כיעד ל\"שיתוף\" מאפליקציות אחרות (").append(n).append(")\n");
            }
        });

        part(sb, "2. המסדים — לכל אופן", new Part() {
            @Override public void run(StringBuilder sb) throws Throwable {
                for (String m : DbScreen.dbModes(a)) {
                    File f = DbScreen.dbFile(a, m);
                    sb.append("\n— אופן \"").append(m).append("\" · ").append(f.length() / 1024).append("KB\n");
                    long t0 = System.nanoTime();
                    SqlStore s = null;
                    try {
                        s = SqlStore.openReadOnly(f);
                        long tOpen = (System.nanoTime() - t0) / 1000000;
                        String chk = s.check();
                        sb.append(ok(chk == null)).append("תקינות: ").append(chk == null ? "תקין" : chk).append('\n');
                        List<Long> hs = homes(s);
                        t0 = System.nanoTime();
                        long home = DbPaths.home(s);
                        long tHome = (System.nanoTime() - t0) / 1000000;
                        t0 = System.nanoTime();
                        int inHome = home == 0 ? 0 : s.members(home).size();
                        long tMembers = (System.nanoTime() - t0) / 1000000;
                        sb.append(ok(hs.size() == 1)).append("רשימות בית: ").append(hs).append(" · בבית: ").append(inHome).append(" פריטים\n");
                        sb.append("רשימות ").append(s.lists().size()).append(" · חכמות ").append(s.smarts().size())
                                .append(" · לא משויכים ").append(s.unassigned().size()).append('\n');
                        sb.append("זמנים: פתיחה ").append(tOpen).append(" · מציאת הבית ").append(tHome).append(" · קריאת הבית ").append(tMembers).append(" מ\"ש\n");
                        long root = DbPaths.find(s, m, Reserved.ROOT_PATH), cmds = DbPaths.find(s, m, Reserved.CMDS_PATH), mine = DbPaths.find(s, m, Reserved.MINE_PATH);
                        long mineIn = cmds == 0 ? 0 : DbPaths.child(s, cmds, Reserved.MINE);
                        sb.append("\"שמור לתוכנה\": ").append(root).append(root != 0 ? " (בתוך: " + s.listsOf(root) + ")" : "")
                                .append(" · \"פקודות\": ").append(cmds).append(" · \"הפקודות שלי\" לפי הדרך: ").append(mine)
                                .append(" · בתוך \"פקודות\": ").append(mineIn).append('\n');
                        if (mine != mineIn) sb.append("❌ שתי רשימות שונות של \"הפקודות שלי\" — זו הסיבה לסעיף 7\n");
                        int same = 0;
                        for (Store.Item it : s.lists()) if (it.name.equals(Reserved.MINE) || it.name.equals(Reserved.ROOT)) {
                            same++;
                            sb.append("   · ").append(it.name).append(" #").append(it.id).append(" בתוך ").append(s.listsOf(it.id)).append(" · ")
                                    .append(s.count(it.id)).append(" פריטים\n");
                        }
                        if (mineIn != 0) sb.append("ב\"הפקודות שלי\" שהתפריט מציג: ").append(names(s, mineIn)).append('\n');
                        if (mine != 0 && mine != mineIn) sb.append("ב\"הפקודות שלי\" האחרת: ").append(names(s, mine)).append('\n');
                    } catch (Throwable t) {
                        sb.append("❌ לא נפתח: ").append(t).append('\n');
                    } finally {
                        if (s != null) s.close();
                    }
                    List<Backups.Entry> bk = DbScreen.backupsOf(a, m).list();
                    java.util.Map<String, Integer> kinds = new java.util.TreeMap<String, Integer>();
                    for (Backups.Entry e : bk) {
                        String k = e.kind + (e.op == null ? "" : " " + e.op);
                        kinds.put(k, kinds.containsKey(k) ? kinds.get(k) + 1 : 1);
                    }
                    sb.append("גיבויים: ").append(bk.size()).append(' ').append(kinds);
                    if (!bk.isEmpty()) sb.append(" · האחרון: ").append(bk.get(0).name).append(" · הישן: ").append(bk.get(bk.size() - 1).name);
                    sb.append('\n');
                }
                sb.append("\nתפריט ⭐ באופן שעל המסך נלקח מ: ");
                SqlStore s = SqlStore.openReadOnly(DbScreen.dbFile(a, mode));
                try {
                    sb.append(DbPaths.find(s, mode, Reserved.CMDS_PATH) != 0 ? "האופן עצמו" : "אופן אחר — " + DbCmdMenu.otherMode(a, mode, Reserved.CMDS_PATH)).append('\n');
                } finally {
                    s.close();
                }
                File sc = Shortcuts.dir();
                String[] k = sc.list();
                sb.append("תיקיית הפקודות של המשתמש: ").append(sc).append(" · ").append(k == null ? "אין" : Arrays.asList(k).toString()).append('\n');
            }
        });

        part(sb, "3. שחזור הדוגמאות — על עותק (סעיף 5)", new Part() {
            @Override public void run(StringBuilder sb) throws Throwable {
                String text = DbExamples.asset(a, DbExamples.ASSET);
                List<String> tops = DbExamples.tops(text);
                sb.append("בקובץ הדוגמאות, בבית: ").append(tops).append('\n');
                SqlStore s = copy(a, DbExamples.MODE, "restore");
                if (s == null) {
                    sb.append("אין מסד לאופן \"").append(DbExamples.MODE).append("\"\n");
                    return;
                }
                try {
                    long home = DbPaths.home(s);
                    sb.append("בית: ").append(home).append(" · לפני: ").append(names(s, home)).append('\n');
                    long first = 0;
                    List<String> missing = new ArrayList<String>();
                    for (String name : tops) {
                        long id = 0;
                        for (Store.Member m : s.members(home)) if (m.item.type == Store.LIST && m.item.name.equals(name)) id = m.item.id;
                        if (id == 0) missing.add(name);
                        else {
                            if (first == 0 && !name.equals(DbExamples.MINE) && !name.equals(Reserved.ROOT)) first = id;
                            if (s.listsOf(id).size() > 1) sb.append("   · \"").append(name).append("\" נמצאת גם ב: ").append(s.listsOf(id)).append('\n');
                        }
                    }
                    sb.append(ok(missing.isEmpty())).append("רשימות מהקובץ שאינן בבית בשם הזה: ").append(missing).append('\n');
                    long mineHome = s.createList("אבחון-בבית", home);
                    long inner = first == 0 ? 0 : s.createList("אבחון-בפנים", first);
                    int lists0 = s.lists().size();
                    int n = DbExamples.restore(s, text, internal);
                    sb.append("הוחלפו: ").append(n).append(" · רשימות לפני ").append(lists0).append(" · אחרי ").append(s.lists().size()).append('\n');
                    sb.append(ok(s.item(mineHome) != null && s.listsOf(mineHome).contains(home))).append("רשימה שנוספה לבית — נשארה\n");
                    boolean gone = inner != 0 && (s.item(inner) == null || s.listsOf(inner).isEmpty());
                    sb.append(ok(gone)).append("רשימה שנוספה בתוך רשימת דוגמאות — יצאה");
                    if (!gone && inner != 0) sb.append(" (עדיין בתוך ").append(s.listsOf(inner)).append(")");
                    sb.append('\n');
                    sb.append("אחרי: ").append(names(s, DbPaths.home(s))).append('\n');
                    String chk = s.check();
                    sb.append(ok(chk == null)).append("תקינות אחרי השחזור: ").append(chk == null ? "תקין" : chk).append('\n');
                } finally {
                    s.close();
                }
            }
        });

        part(sb, "4. אופן חדש — מסד חדש (סעיף 6)", new Part() {
            @Override public void run(StringBuilder sb) throws Throwable {
                SqlStore s = fresh(a, "newmode");
                try {
                    long home = DbPaths.ensureHome(s);
                    sb.append(ok(home != 0 && s.members(home).isEmpty())).append("בית ריק נוצר\n");
                    DbExamples.install(s, DbExamples.reservedPart(DbExamples.asset(a, DbExamples.ASSET)), internal);
                    long cmds = DbPaths.find(s, "x", Reserved.CMDS_PATH);
                    sb.append(ok(cmds != 0)).append("\"שמור לתוכנה\" נכנסת מקובץ הדוגמאות: ").append(names(s, home))
                            .append(" · בפקודות: ").append(cmds == 0 ? 0 : s.count(cmds)).append('\n');
                } finally {
                    s.close();
                }
            }
        });

        part(sb, "5. פקודה חדשה ל\"הפקודות שלי\", שמירה בשם, הוספת קובץ — על עותק (סעיפים 7, 8, 9)", new Part() {
            @Override public void run(StringBuilder sb) throws Throwable {
                SqlStore s = copy(a, mode, "work");
                if (s == null) return;
                try {
                    long home = DbPaths.home(s);
                    long cmds = DbPaths.find(s, mode, Reserved.CMDS_PATH);
                    long mine = cmds == 0 ? 0 : DbPaths.child(s, cmds, Reserved.MINE);
                    if (mine == 0) sb.append("באופן הזה אין \"הפקודות שלי\" (התפריט מאופן אחר)\n");
                    else {
                        DbFiles.Added r = DbFiles.add(s, mine, Collections.singletonList(internal + "/אבחון-פקודה.vbat"), null, false);
                        boolean in = false;
                        for (Store.Member m : s.members(mine)) if (m.item.name.contains("אבחון-פקודה")) in = true;
                        sb.append(ok(r.added == 1 && in)).append("פקודה חדשה נכנסת ל\"הפקודות שלי\" שבתפריט\n");
                    }
                    DbSmartForm.Result res = new DbSmartForm.Result();
                    res.name = "אבחון-חכמה";
                    res.cond = "סוג:מסמכים";
                    res.sort = "";
                    res.view = "";
                    long sm = DbSmartForm.save(s, 0, home, res);
                    sb.append(ok(sm != 0 && s.listsOf(sm).contains(home) && "סוג:מסמכים".equals(s.prop(sm, Store.P_COND))))
                            .append("\"שמירה בשם\" של חיפוש — רשימה חכמה בבית, עם התנאי\n");
                    long list = s.createList("אבחון-רשימה", home);
                    DbFiles.Added r2 = DbFiles.add(s, list, Arrays.asList(internal + "/אבחון-א.txt", internal + "/אבחון-ב.txt"), null, false);
                    sb.append(ok(r2.added == 2 && s.count(list) == 2)).append("קבצים נכנסים לרשימה\n");
                } finally {
                    s.close();
                }
            }
        });

        part(sb, "6. יצוא וייבוא — על עותק (סעיף 10)", new Part() {
            @Override public void run(StringBuilder sb) throws Throwable {
                SqlStore s = copy(a, mode, "export");
                if (s == null) return;
                try {
                    long home = DbPaths.home(s);
                    long src = s.createList("אבחון-מקור", home), sub = s.createList("תת", src);
                    DbFiles.add(s, src, Collections.singletonList(internal + "/אבחון-א.txt"), null, false);
                    DbFiles.add(s, sub, Collections.singletonList(internal + "/אבחון-ב.txt"), null, false);
                    DbSmartForm.Result res = new DbSmartForm.Result();
                    res.name = "חכמה";
                    res.cond = "סוג:מסמכים";
                    res.sort = "";
                    res.view = "";
                    DbSmartForm.save(s, 0, src, res);
                    DbExport.Out o = DbExport.list(s, src, false, "אבחון", System.currentTimeMillis());
                    File f = new File(new File(a.getCacheDir(), "diag"), "export.txt");
                    Files.writeAtomic(f, o.text);
                    String back = Files.readUtf8(f);
                    sb.append(ok(back.equals(o.text))).append("הקובץ נכתב ונקרא זהה (").append(o.lists).append(" רשימות, ").append(o.files).append(" קבצים)\n");
                    long dest = s.createList("אבחון-יעד", home);
                    DbMore.Imported im = DbMore.importText(s, dest, back);
                    long got = DbPaths.child(s, dest, "אבחון-מקור");
                    boolean smart = false;
                    if (got != 0) for (Store.Member m : s.members(got)) if (m.item.type == Store.SMART && "סוג:מסמכים".equals(s.prop(m.item.id, Store.P_COND))) smart = true;
                    sb.append(ok(got != 0 && smart && DbPaths.child(s, got, "תת") != 0 && im.bad == 0)).append("ייבוא לרשימה ריקה: רשימות ")
                            .append(im.lists).append(" · חכמות ").append(im.smarts).append(" · קבצים ").append(im.files).append(" · לא הובנו ").append(im.bad)
                            .append(" · ביעד: ").append(names(s, dest)).append('\n');
                } finally {
                    s.close();
                }
                // מה שהמשתמש ייצא באמת: הקבצים בתיקיית היצוא, והראש של החדש ביותר
                File dir = DbListCmd.exportDir();
                File[] k = dir.listFiles();
                sb.append("תיקיית היצוא: ").append(dir).append(" · ").append(k == null ? 0 : k.length).append(" קבצים\n");
                File newest = null;
                if (k != null) for (File x : k) {
                    sb.append("   · ").append(x.getName()).append(" · ").append(x.length()).append(" בתים\n");
                    if (x.isFile() && (newest == null || x.lastModified() > newest.lastModified())) newest = x;
                }
                if (newest != null) {
                    String t = Files.readUtf8(newest);
                    String[] lines = t.split("\n");
                    sb.append("החדש ביותר — \"").append(newest.getName()).append("\", ").append(lines.length).append(" שורות. 25 הראשונות:\n");
                    for (int i = 0; i < Math.min(25, lines.length); i++) sb.append("   | ").append(lines[i]).append('\n');
                    SqlStore c = copy(a, mode, "import");
                    if (c != null)
                        try {
                            long dest = c.createList("אבחון-יעד-אמיתי", DbPaths.home(c));
                            DbMore.Imported im = DbMore.importText(c, dest, t);
                            sb.append("ייבוא שלו לרשימה ריקה (בעותק): רשימות ").append(im.lists).append(" · חכמות ").append(im.smarts).append(" · קבצים ")
                                    .append(im.files).append(" · לא הובנו ").append(im.bad).append(" · ביעד: ").append(names(c, dest)).append('\n');
                        } finally {
                            c.close();
                        }
                    // השורה של הכלי, כמו שהיא מתוכננת (בלי לבצע)
                    String target = "אבחון";
                    SqlStore live = SqlStore.openReadOnly(DbScreen.dbFile(a, mode));
                    try {
                        for (Store.Member m : live.members(DbPaths.home(live))) if (m.item.type == Store.LIST) { target = m.item.name; break; }
                    } finally {
                        live.close();
                    }
                    String line = "COPY " + ListCmd.FILE_WORD + " \"" + newest.getAbsolutePath() + "\" " + ListCmd.MANUAL + " \"" + target + "\"";
                    Batch.Plan p = Batch.plan(line, dir, BatchUi.vars(a, dir), Library.get(a), null);
                    for (Batch.Step st : p.steps) sb.append("תכנון השורה: ").append(line).append("\n   ← ").append(st.error != null ? "⚠ " + st.error : st.describe).append('\n');
                }
            }
        });

        part(sb, "7. המבנה של האופן \"דוגמאות\" (יצוא לקריאה)", new Part() {
            @Override public void run(StringBuilder sb) throws Throwable {
                File f = DbScreen.dbFile(a, DbExamples.MODE);
                if (!f.isFile()) return;
                SqlStore s = SqlStore.openReadOnly(f);
                try {
                    long home = DbPaths.home(s);
                    if (home == 0) {
                        sb.append("אין בית\n");
                        return;
                    }
                    for (String line : DbExport.list(s, home, true, DbExamples.MODE, System.currentTimeMillis()).text.split("\n"))
                        if (line.startsWith("#") || line.startsWith("@תת-רשימה") || line.startsWith("@קישור") || line.startsWith("@חכמה")) sb.append(line).append('\n');
                } finally {
                    s.close();
                }
            }
        });

        part(sb, "8. יומן השגיאות (הסוף)", new Part() {
            @Override public void run(StringBuilder sb) {
                String log = Log.read();
                sb.append(log.length() > 12000 ? log.substring(log.length() - 12000) : log).append('\n');
            }
        });
    }

    /** מ0.33 (ממצא 38): באיזה אופן ההרצה רושמת כל רשימה שנוצרת בקובץ עם שני "אופן" — מה ש"ניקוי" ישתמש בו. בלי לבצע. */
    private static void modes(final Activity a, StringBuilder sb) {
        part(sb, "10. קובץ עם שני אופנים — לאיזה אופן שייכת כל רשימה שנוצרת (תכנון בלבד)", new Part() {
            @Override public void run(StringBuilder sb) throws Throwable {
                String text = DbExamples.asset(a, DbTestMode.ASSET_MODES);
                File dir = Modes.dirOf(Words.DbCmd.TEST_MODE);
                Batch.Plan p = Batch.plan(text, dir, BatchUi.vars(a, dir), Library.get(a), null);
                String first = null, second = null;
                for (Batch.Step s : p.steps)
                    if (s.listPath != null && "MD".equals(s.cmd)) {
                        String m = p.modeOf(s);
                        sb.append("   ").append(s.listPath).append(" ← אופן \"").append(m).append("\"").append(s.error != null ? " ⚠ " + s.error : "").append('\n');
                        if (first == null) first = m;
                        else second = m;
                    }
                sb.append(ok(Words.DbCmd.TEST_MODE.equals(first) && DbExamples.MODE.equals(second))).append("הראשונה באופן הבדיקות, השנייה ב\"דוגמאות\" — \"ניקוי\" ימחק כל אחת מהאופן שלה\n");
                sb.append("שגיאות בתכנון: ").append(p.errors).append('\n');
            }
        });
    }

    /** "הבדיקה הכללית" — כמו מהתפריט (באופן "בדיקות מערכת"), והיומן שלה. */
    private static void general(final Activity a, final Physical.Progress progress, StringBuilder sb) {
        part(sb, "9. הבדיקה הכללית", new Part() {
            @Override public void run(StringBuilder sb) throws Throwable {
                File f = DbTestMode.generalFile(a);
                File dir = f.getParentFile();
                if (!dir.isDirectory() || !DbScreen.dbFile(a, Words.DbCmd.TEST_MODE).isFile()) {
                    sb.append("לא רצה: האופן \"בדיקות מערכת\" עוד לא קיים. לפתוח פעם אחת ⋮ ← בדיקות, ואז להריץ שוב את הדוח\n");
                    return;
                }
                DbTestMode.put(a, DbTestMode.ASSET, new File(dir, Words.DbCmd.TEST_FILE + "." + Batch.EXT), null);   // הנוסח של הגרסה הזו
                DbTestMode.put(a, DbTestMode.ASSET_CALL, new File(dir, Words.DbCmd.TEST_CALL_FILE + "." + Batch.EXT), null);
                DbTestMode.put(a, DbTestMode.ASSET_V23, new File(dir, Words.DbCmd.TEST_V23_FILE + "." + Batch.EXT), null);
                DbTestMode.put(a, DbTestMode.ASSET_V24, new File(dir, Words.DbCmd.TEST_V24_FILE + "." + Batch.EXT), null);
                DbTestMode.put(a, DbTestMode.ASSET_ALL, f, null);
                Library lib = Library.get(a);
                Batch.Plan p = Batch.plan(Files.readUtf8(f), f.getParentFile(), BatchUi.vars(a, f.getParentFile()), lib, null);
                sb.append("שורות ").append(p.steps.size()).append(" · שגיאות בתכנון ").append(p.errors).append('\n');
                for (Batch.Step s : p.steps) if (s.error != null) sb.append("   ⚠ ").append(s.label()).append(". ").append(s.error).append(" | ").append(s.text).append('\n');
                List<String> rep = Batch.run(p, lib, new Physical.DialogAsker(a), progress, Acts.deletedStaysGrey());
                for (String line : rep) if (!line.startsWith("✅ \"")) sb.append("   ").append(line).append('\n');   // בלי השורות שעברו; הסיכום — נשאר
            }
        });
    }
}
