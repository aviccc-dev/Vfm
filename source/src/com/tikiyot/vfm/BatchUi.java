package com.tikiyot.vfm;

// קובצי פקודות — הממשק (117א, שלב 1): תפריט (הרצה / עריכה), תפריט ⭐ של כל קובצי הפקודות, יצירת קובץ חדש,
// ותצוגה מקדימה ← אישור ← ביצוע עם התקדמות ← דוח ← פתיחת התיקייה של START.
// המנוע — Batch. אסור לו: לשנות משהו בכונן בעצמו.

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class BatchUi {

    private BatchUi() { }

    static void edit(Activity a, File f) {
        a.startActivity(new Intent(a, BatchEditScreen.class).putExtra(BatchEditScreen.EXTRA_FILE, f.getAbsolutePath()));
    }

    static Map<String, String> vars(Activity a, File here) {
        String card = null;
        for (Vols.Vol v : Vols.list(a)) if (!v.internal && card == null) card = v.root.getAbsolutePath();
        return Batch.vars(here, internal(a), card);
    }

    /** כל קובצי הפקודות שהאינדקס מכיר (216: גם לכפתור הרצה בשורת הכפתורים). */
    static List<File> all(Activity a) {
        List<File> found = new ArrayList<File>();
        for (Library.Hit h : Library.get(a).search("סוג:" + Batch.EXT, false, false).hits)   // תחביר
            if (h.rec != null) found.add(new File(h.path()));
        return found;
    }

    private static String internal(Activity a) {
        for (Vols.Vol v : Vols.list(a)) if (v.internal) return v.root.getAbsolutePath();
        return "/storage/emulated/0";
    }

    /** הרצה: תצוגה מקדימה, אישור, ביצוע, דוח. */
    static void run(final Activity a, final File f) {
        final String text;
        try {
            text = Files.readUtf8(f);
        } catch (Throwable t) {
            Ui.toast(a, Words.CANT_READ_FILE);
            return;
        }
        final Library lib = Library.get(a);
        final Map<String, String> vars = vars(a, f.getParentFile());
        // 1.25.4 (באג, המשתמש: "אחרי ההרצה — גם אין דוח וגם לא רואים שינוי ברשימות"): ההכנה יכולה לקחת זמן
        // (לולאות, רשימות גדולות) — עד כאן בלי שום סימן על המסך. עכשיו — חלון "מכין…" עם עצירה, ותקלה — הודעה.
        final Physical.Busy busy = new Physical.Busy(a, Words.Cmd.preparing(f.getName()));
        new Thread(new Runnable() {                                      // התכנון — מהכונן; קבצים חדשים נכנסים לאינדקס בביצוע (134)
            @Override
            public void run() {
                Batch.Plan p;
                try {
                    p = Batch.plan(text, f.getParentFile(), vars, lib, busy);
                } catch (final Throwable t) {
                    Log.e("הכנת קובץ פקודות: " + f, t);
                    busy.close();
                    a.runOnUiThread(new Runnable() {
                        @Override public void run() { show(a, Words.Cmd.PLAN_FAILED, String.valueOf(t), null); }
                    });
                    return;
                }
                busy.close();
                final Batch.Plan plan = p;
                a.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (plan.stopped) Ui.toast(a, Words.Cmd.STOPPED_PREPARING);
                        else preview(a, f, plan, lib);
                    }
                });
            }
        }).start();
    }

    private static void preview(final Activity a, final File f, final Batch.Plan plan, final Library lib) {
        if (plan.asIf) {                                                      // 1.10.0: בכאילו — הודעת חובה, ואז רק התוצאה
            Ui.ask(a, Words.AsIf.TITLE, Words.AsIf.ONLY_VIRTUAL, f.getName(), null, Words.AsIf.GO, new Runnable() {
                @Override
                public void run() {
                    Log.i("בכאילו: " + f);
                    AsIfScreen.show(a, f, Batch.asIfRows(plan));
                }
            });
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (Batch.Step s : plan.steps) {
            sb.append(s.label()).append(". ");
            sb.append(s.error != null ? "⚠ " + s.error + "\n     " + s.text : s.describe);
            if (s.error == null && (!s.files.isEmpty() || (s.share && !s.dirs.isEmpty()))
                    && (s.cmd.equals("DEL") || s.cmd.equals("MOVE") || s.cmd.equals("COPY"))) {
                for (String d : s.share ? s.dirs : new ArrayList<String>()) sb.append("\n     · 📁 ").append(new File(d).getName());
                int shown = Math.min(3, s.files.size());
                for (int i = 0; i < shown; i++) sb.append("\n     · ").append(new File(s.files.get(i)).getName());
                if (s.files.size() > shown) sb.append("\n     · ").append(Words.andMore(s.files.size() - shown));
            }
            sb.append("\n");
        }
        for (String n : plan.notes) sb.append(n).append("\n");                // 1.25.0: לולאה שנעצרה כבר בתכנון
        if (plan.steps.isEmpty()) sb.append(Words.Cmd.NOTHING_TO_DO);
        boolean deletes = false;
        for (Batch.Step s : plan.steps) if (s.error == null && (s.cmd.equals("DEL") || s.cmd.equals("RD"))) deletes = true;
        String files = plan.changes() == 0 ? Words.Cmd.NO_CHANGE
                : Words.Cmd.CHANGES + (deletes ? Words.Cmd.DELETES_TO_TRASH : "");
        // 216: "הפיך" — רק כשיש מחיקה (המשתמש: "כאשר אין מחיקה או פעולה בלתי הפיכה — אין צורך שיודיע שהפעולה הפיכה")
        String undo = (plan.errors > 0 ? Words.Cmd.errorLines(plan.errors) : "") + (deletes ? Words.Cmd.UNDO : "");
        if (plan.changes() == 0 && plan.start == null && plan.open == null) {
            show(a, Words.Cmd.preview(f.getName()), sb.toString(), null);
            return;
        }
        final Runnable go = new Runnable() {
            @Override
            public void run() {
                final Physical.Busy busy = new Physical.Busy(a, Words.Cmd.running(f.getName()));
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        List<String> rep;
                        try {
                            rep = Batch.run(plan, lib, new Physical.DialogAsker(a), busy, Acts.deletedStaysGrey());
                        } catch (Throwable t) {                               // 1.25.4: תקלה בהרצה — דוח עם התקלה
                            Log.e("הרצת קובץ פקודות: " + f, t);
                            rep = new ArrayList<String>();
                            rep.add(Words.Cmd.RUN_FAILED + t);
                            plan.trouble = true;
                        }
                        final List<String> report = rep;
                        busy.close();
                        Log.i("קובץ פקודות: " + f + " · " + (report.isEmpty() ? Words.Cmd.NO_CHANGE : report.toString())     // 187
                                + (plan.start != null ? " · " + (Batch.isHome(plan.start) ? Words.Cmd.HOME_AT_END : Words.Cmd.openAtEnd(plan.start.getName())) : ""));
                        Ui.afterIndex(a, new Runnable() {
                            @Override
                            public void run() {
                                StringBuilder r = new StringBuilder();
                                for (String e : plan.echoes) r.append(e).append("\n");
                                if (!plan.echoes.isEmpty()) r.append("\n");
                                for (String line : report) r.append(line).append("\n");
                                // 216: הלך חלק — הודעה קצרה, ו־START נפתח מיד; אחרת (או ECHO) — הדוח
                                if (plan.trouble || !plan.echoes.isEmpty() || plan.measure)   // 1.25.0: מדידה — תמיד הדוח
                                    show(a, Words.Cmd.done(f.getName()), r.length() == 0 ? Words.Cmd.DONE : r.toString(), plan.start);
                                else {
                                    if (plan.changes() > 0) Ui.toast(a, Words.Cmd.doneShort(report.size()));
                                    openStart(a, plan.start);
                                }
                                if (plan.open != null && !plan.trouble) Verbs.run(a, plan.open, f.getParentFile());   // 1.13.0
                                if (plan.trouble && plan.start != null && !Batch.isHome(plan.start) && plan.start.isFile()) Ui.open(a, plan.start);   // 186
                                if (!plan.shared.isEmpty()) {                 // 203: כל מה שלשיתוף — שליחה אחת
                                    Ui.toast(a, Words.Cmd.sending(plan.shared.size()));
                                    Acts.sendReady(a, plan.shared);
                                }
                            }
                        });
                    }
                }).start();
            }
        };
        // 216: רק "פתיחה" (START) בלי שינוי בכונן ובלי שגיאות — ישר, בלי אישור
        if (plan.changes() == 0 && plan.errors == 0 && plan.echoes.isEmpty()) {
            go.run();
            return;
        }
        Ui.ask(a, Words.Cmd.runTitle(f.getName()), files, sb.toString().trim(), undo.isEmpty() ? null : undo, Words.Cmd.RUN, go);
    }

    /** 216: מה ש־START ביקש — קובץ נפתח, תיקייה נפתחת במנהל הקבצים. */
    private static void openStart(Activity a, File start) {
        if (start == null) return;
        if (Batch.isHome(start)) Home.go(a);                                  // 1.9.0
        else if (start.isFile()) Ui.open(a, start);
        else a.startActivity(SearchScreen.fileManager(a, "", start.getAbsolutePath(), null));   // 159
    }

    private static void show(final Activity a, String title, String body, final File start) {
        TextView t = new TextView(a);
        t.setText(body);
        t.setTextSize(14);
        t.setTextColor(Ui.TEXT);
        t.setTextIsSelectable(true);
        t.setPadding(Ui.dp(a, 16), Ui.dp(a, 8), Ui.dp(a, 16), Ui.dp(a, 8));
        ScrollView sv = new ScrollView(a);
        sv.addView(t);
        AlertDialog.Builder b = new AlertDialog.Builder(a).setTitle(title).setView(sv).setNegativeButton(Words.CLOSE, null);
        if (Batch.isHome(start))                                              // 1.9.0
            b.setPositiveButton(Words.Cmd.TO_HOME, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface d, int w) { Home.go(a); }
            });
        else if (start != null && start.isFile())                             // 186: קובץ — פתיחה (apk — התקנה)
            b.setPositiveButton(Words.Cmd.openFile(start.getName()), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface d, int w) { Ui.open(a, start); }
            });
        else if (start != null)
            b.setPositiveButton(Words.Cmd.openFolder(start.getName()), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface d, int w) {
                    a.startActivity(SearchScreen.fileManager(a, "", start.getAbsolutePath(), null));   // 159
                }
            });
        b.show();
    }
}
