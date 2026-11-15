package com.tikiyot.vfm;

// הגדרות התוכנה: הרשאות, אינדקס, רמת עדכון, קבצי הגדרות, בדיקה ויומן.

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

public class SettingsScreen extends Activity {

    private Library lib;
    private final Handler h = new Handler(Looper.getMainLooper());
    private TextView status;
    private volatile boolean working;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        lib = Library.get(this);
        Log.initVersion(this);
        setTitle("הגדרות · גרסה " + Log.version);

        LinearLayout root = Ui.screen(this);
        int p = Ui.dp(this, 14);
        root.setPadding(p, p, p, p);

        status = new TextView(this);
        status.setTextSize(15);
        status.setTextColor(Ui.TEXT);
        root.addView(status);

        button(root, Words.Prefs.FILE_ACCESS, new Runnable() {
            @Override public void run() { Ui.requestFiles(SettingsScreen.this); }
        });
        button(root, Words.Prefs.REBUILD, new Runnable() {
            @Override public void run() { askRebuild(); }
        });
        sendBtn = button(root, Words.Send.FIXED, new Runnable() {                           // 183
            @Override
            public void run() {
                Transfer.chooseFixed(SettingsScreen.this, new Runnable() {
                    @Override public void run() { labels(); }
                });
            }
        });
        // מ0.45 (ב9): "בניית האינדקס — במקביל" (כמה קוראים) — עבר לתוסף האבחון: שם מודדים כמה ראשים הכי מהיר, וקובעים
        deletedBtn = button(root, Words.Prefs.DELETED_BUTTON, new Runnable() {
            @Override public void run() { chooseDeleted(); }
        });
        button(root, Words.Prefs.EXCLUDED_BUTTON, new Runnable() {
            @Override public void run() { editExcluded(); }
        });
        button(root, Words.Prefs.QUICK_REFRESH, new Runnable() {
            @Override public void run() { refresh(); }
        });
        button(root, Words.DesignW.MANAGE, new Runnable() {                              // 1.12.1
            @Override public void run() { DesignUi.manage(SettingsScreen.this); }
        });
        button(root, Words.Prefs.EDIT_INI, new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(SettingsScreen.this, IniListScreen.class));
            }
        });
        button(root, Words.Db.BACKUPS, new Runnable() {                                 // מ0.35: הגיבויים של המסד — מכאן
            @Override public void run() { DbScreen.openBackups(SettingsScreen.this); }
        });
        // מ0.43: בדיקת מסלול, יומן התקלות ותקופת הניסיון — בתוסף האבחון (⋮ ← "🔬 אבחון"; claude/tosaf-bdikot.md)
        button(root, Words.Prefs.DEEP_REFRESH, new Runnable() {     // 108, 114
            @Override public void run() { deepRefresh(); }
        });
        bgBtn = button(root, Words.Bg.TITLE, new Runnable() {                            // 190
            @Override
            public void run() {
                SigUi.show(SettingsScreen.this, lib, new Runnable() {
                    @Override public void run() { labels(); }
                });
            }
        });
        button(root, Words.Bg.RELOCATE_BUTTON, new Runnable() {                         // 190
            @Override public void run() { SigUi.relocateAll(SettingsScreen.this, lib, null); }
        });

        updateSection(root);

        ScrollView sv = new ScrollView(this);
        sv.addView(root);
        setContentView(TopRow.wrap(this, sv));                   // 1.9.0: שורת הכפתורים העליונה
    }

    /** בניית האינדקס מחדש — אחרי אישור. */
    private void askRebuild() {
        Ui.ask(SettingsScreen.this, Words.Prefs.REBUILD, Words.NO_TOUCH_FILES,
                Words.Prefs.REBUILD_WHAT,
                Words.Prefs.REBUILD_UNDO, Words.Prefs.BUILD, new Runnable() {
                    @Override public void run() { rebuild(); }
                });
    }

    /** קובץ שנמחק — מה קורה ברשימות. */
    private void chooseDeleted() {
        final String[] opts = {Conf.DELETED_REMOVE, Conf.DELETED_GREY + Words.Prefs.GREY_EXAMPLE};
        Ui.options(SettingsScreen.this, Words.Prefs.NOW + (Acts.deletedStaysGrey() ? Conf.DELETED_GREY : Conf.DELETED_REMOVE),
                opts, new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int w) {
                        Acts.setting(SettingsScreen.this, Conf.DELETED, w == 0 ? Conf.DELETED_REMOVE : Conf.DELETED_GREY);   // מבנה 127: דרך המנהל
                        labels();
                    }
                });
    }

    /** 76: תיקיות שלא נכנסות לאינדקס. */
    private void editExcluded() {
        LinearLayout b = Ui.form(SettingsScreen.this);
        final Sources ex = new Sources(SettingsScreen.this, Words.Prefs.EXCLUDED_NONE);
        java.util.List<String> cur = new java.util.ArrayList<String>();
        for (String p : Conf.get(Conf.GENERAL, Conf.EXCLUDED, "").split("\\|"))
            if (p.trim().startsWith("/")) cur.add(p.trim());
        ex.set(cur);
        b.addView(Ui.label(SettingsScreen.this, Words.Prefs.EXCLUDED_ABOUT));
        b.addView(ex.view);
        Ui.askForm(SettingsScreen.this, Words.Prefs.EXCLUDED_TITLE, b, new Runnable() {
            @Override
            public void run() {
                StringBuilder sb = new StringBuilder();
                for (String p : ex.get()) if (p.startsWith("/")) sb.append(sb.length() > 0 ? "|" : "").append(p);
                if (Acts.setting(SettingsScreen.this, Conf.EXCLUDED, sb.toString()))   // מבנה 127: דרך המנהל
                    Ui.toast(SettingsScreen.this, Words.Prefs.EXCLUDED_SAVED);
            }
        });
    }

    /** 110: רמת עדכון האינדקס, ורענון תקופתי. */
    private void updateSection(LinearLayout root) {
        TextView t = new TextView(this);
        t.setText(Words.Prefs.LEVEL_ABOUT);
        t.setTextColor(Ui.SUB);
        root.addView(t);
        final RadioGroup rg = new RadioGroup(this);                          // 110
        for (int i = 0; i < Library.LEVELS.length; i++) {
            RadioButton r = new RadioButton(this);
            r.setText((i + 1) + " · " + Library.LEVELS[i] + (i == 2 ? Words.Prefs.RECOMMENDED : ""));
            rg.addView(r);
        }
        ((RadioButton) rg.getChildAt(lib.updateLevel() - 1)).setChecked(true);
        rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup g, int id) {
                for (int i = 0; i < g.getChildCount(); i++)
                    if (g.getChildAt(i).getId() == id && lib.updateLevel() != i + 1) Acts.setting(SettingsScreen.this, Conf.LEVEL, Conf.LEVELS[i]);   // מבנה 127
            }
        });
        root.addView(rg);
        TextView pt = new TextView(this);
        pt.setText(Words.Prefs.PERIODIC_ABOUT);
        pt.setTextColor(Ui.SUB);
        root.addView(pt);
        final int[] mins = {0, 1, 5, 15};
        int cur = 0;
        for (int i = 0; i < mins.length; i++) if (mins[i] == lib.periodicMinutes()) cur = i;
        android.widget.Spinner sp = Ui.choice(this, new String[]{Words.Prefs.OFF, Words.Prefs.EVERY_MINUTE, Words.Prefs.EVERY_5, Words.Prefs.EVERY_15}, cur);
        sp.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) {
                if (mins[pos] != lib.periodicMinutes()) Acts.setting(SettingsScreen.this, Conf.PERIODIC, String.valueOf(mins[pos]));   // מבנה 127; הבורר קורא לכאן גם כשהמסך נפתח
            }

            @Override public void onNothingSelected(android.widget.AdapterView<?> p) { }
        });
        root.addView(sp);
    }

    private Button button(LinearLayout root, String text, final Runnable action) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { action.run(); }
        });
        root.addView(b);
        return b;
    }

    private Button deletedBtn, sendBtn, bgBtn;

    /** הבחירה הפעילה מופיעה על הכפתור עצמו (61). */
    private void labels() {
        if (sendBtn != null) sendBtn.setText(Words.Send.FIXED + ": " + Transfer.fixedLabel(this));
        if (bgBtn != null) bgBtn.setText(SigUi.line(lib));                          // 190
        if (deletedBtn != null) deletedBtn.setText(Words.Prefs.DELETED_IS
                + (Acts.deletedStaysGrey() ? Conf.DELETED_GREY : Conf.DELETED_REMOVE));
    }

    @Override
    protected void onResume() {
        super.onResume();
        labels();
        if (Files.hasAccess(this)) lib.start(new Runnable() {
            @Override
            public void run() {
                h.post(new Runnable() {
                    @Override public void run() { show(); }
                });
            }
        });
        show();
    }

    private void show() {
        StringBuilder sb = new StringBuilder();
        sb.append(Words.Prefs.ACCESS_IS).append(Files.hasAccess(this) ? "✓" : "✗").append("\n");
        sb.append("\n");
        if (!lib.ready) {
            sb.append(Words.Prefs.INDEX_LOADING);
        } else {
            sb.append(Words.Prefs.IN_INDEX).append(Files.count(lib.totalFiles())).append(Words.Prefs.FILES_DOT)
                    .append(Files.count(lib.totalDirs())).append(Words.Prefs.FOLDERS_LINE);
            sb.append(Words.Prefs.UPDATED_AT).append(Files.when(lib.lastBuilt())).append("\n");
            for (Index ix : lib.indexes)
                sb.append("· ").append(ix.label).append(": ")
                        .append(Files.count(ix.fileCount())).append(Words.Prefs.FILES_LINE);
        }
        if (!lib.status.isEmpty()) sb.append("\n").append(lib.status);
        status.setText(sb.toString());
    }

    private void rebuild() {
        index(Manager.INDEX_FULL, null);
    }

    private void refresh() {
        index(Manager.INDEX_QUICK, null);
    }

    /** רענון מעמיק (108, 114) — ברקע, עם הודעה בסוף. */
    private void deepRefresh() {
        index(Manager.INDEX_DEEP, Words.Prefs.DEEP_RUNNING);
    }

    /**
     * מבנה 120: שלוש פעולות האינדקס — דרך המנהל, כפקודה ("רענן" · "רענן עמוק" · "רענן מלא"), ברקע. כאן נשארות ההודעות; ההתקדמות של
     * בנייה מחדש נקראת מהמצב של האינדקס (lib.status), פעם בחצי שנייה.
     */
    private void index(final int kind, String starting) {
        if (working) return;
        working = true;
        if (starting != null) Ui.toast(this, starting);
        final java.util.Map<String, String> vars = BatchUi.vars(this, Paths.work());
        final Runnable tick = new Runnable() {
            @Override
            public void run() {
                if (!working) return;
                show();
                h.postDelayed(this, 500);
            }
        };
        h.postDelayed(tick, 500);
        new Thread(new Runnable() {
            @Override
            public void run() {
                Manager.Act act = Manager.onIndex(kind, Paths.work(), vars, lib);   // ברקע: "רענן" מרענן כבר בהכנה
                final String said = act.error() != null ? act.error() : act.say(lib);
                working = false;
                h.post(new Runnable() {
                    @Override
                    public void run() {
                        show();
                        Ui.toast(SettingsScreen.this, said);
                    }
                });
            }
        }).start();
    }
}
