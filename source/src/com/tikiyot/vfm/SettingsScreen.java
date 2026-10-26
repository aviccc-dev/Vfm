package com.tikiyot.vfm;

// הגדרות התוכנה: הרשאות, אינדקס, רמת עדכון, קבצי הגדרות, בדיקה ויומן.

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
        readersBtn = button(root, Words.Prefs.READERS_BUTTON, new Runnable() {               // 177
            @Override public void run() { chooseReaders(); }
        });
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
        button(root, Words.Prefs.CHECK_PATH, new Runnable() {
            @Override public void run() { checkPath(); }
        });
        button(root, Words.Prefs.CLEAN_ORPHANS, new Runnable() {
            @Override public void run() { cleanOrphans(); }
        });
        button(root, Words.Prefs.ERROR_LOG, new Runnable() {
            @Override public void run() { showLog(); }
        });

        button(root, Words.Prefs.DEEP_REFRESH, new Runnable() {     // 108, 114
            @Override public void run() { deepRefresh(); }
        });
        button(root, Words.Trial.BUTTON, new Runnable() {                              // 182
            @Override public void run() { showTrial(); }
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

    /** איזה כונן נבנה ראשון. */
    /** 177: כמה קוראים לכל כונן. */
    private void chooseReaders() {
        final String[] vals = {"1", "4", "8", "16"};
        final String[] opts = new String[vals.length];
        for (int i = 0; i < vals.length; i++) opts[i] = readersLabel(Integer.parseInt(vals[i]));
        Ui.options(SettingsScreen.this, Words.Prefs.NOW + readersLabel(Library.readers())
                + (lib.lastBuild().isEmpty() ? "" : Words.Prefs.LAST_BUILD_LINE + lib.lastBuild() + "\n" + lib.lastBuildDrives()), opts,
                new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int w) {
                        Conf.set(Conf.GENERAL, Conf.READERS, vals[w]);
                        labels();
                    }
                });
    }

    /** קובץ שנמחק — מה קורה ברשימות. */
    private void chooseDeleted() {
        final String[] opts = {Conf.DELETED_REMOVE, Conf.DELETED_GREY + Words.Prefs.GREY_EXAMPLE};
        Ui.options(SettingsScreen.this, Words.Prefs.NOW + (Acts.deletedStaysGrey() ? Conf.DELETED_GREY : Conf.DELETED_REMOVE),
                opts, new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int w) {
                        Conf.set(Conf.GENERAL, Conf.DELETED, w == 0 ? Conf.DELETED_REMOVE : Conf.DELETED_GREY);
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
                Conf.set(Conf.GENERAL, Conf.EXCLUDED, sb.toString());
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
                for (int i = 0; i < g.getChildCount(); i++) if (g.getChildAt(i).getId() == id) lib.setUpdateLevel(i + 1);
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
                lib.setPeriodicMinutes(mins[pos]);
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

    private Button deletedBtn, readersBtn, sendBtn, bgBtn;

    private static String readersLabel(int n) {
        return n + (n == 1 ? Words.Prefs.READER : Words.Prefs.READERS) + Words.Prefs.PER_DRIVE
                + (n == Library.READERS_DEFAULT ? Words.Prefs.FASTEST : "");
    }

    /** הבחירה הפעילה מופיעה על הכפתור עצמו (61). */
    private void labels() {
        if (sendBtn != null) sendBtn.setText(Words.Send.FIXED + ": " + Transfer.fixedLabel(this));
        if (readersBtn != null) readersBtn.setText(Words.Prefs.BUILD_IS + readersLabel(Library.readers())
                + (lib.lastBuild().isEmpty() ? "" : Words.Prefs.LAST_SUFFIX + lib.lastBuild()));
        if (bgBtn != null) bgBtn.setText(SigUi.line(lib));                          // 190
        if (deletedBtn != null) deletedBtn.setText(Words.Prefs.DELETED_IS
                + (Acts.deletedStaysGrey() ? Conf.DELETED_GREY : Conf.DELETED_REMOVE));
    }

    @Override
    protected void onResume() {
        super.onResume();
        labels();
        if (Ui.hasFiles(this)) lib.start(new Runnable() {
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
        sb.append(Words.Prefs.ACCESS_IS).append(Ui.hasFiles(this) ? "✓" : "✗").append("\n");
        sb.append(Log.device()).append("\n\n");
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
        if (working) return;
        working = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                lib.build(new Index.Progress() {
                    @Override
                    public void onScan(String label, int items) {
                        lib.status = Words.Prefs.SCANNING + label + "… " + Files.count(items);
                        h.post(new Runnable() {
                            @Override public void run() { show(); }
                        });
                    }
                });
                working = false;
                h.post(new Runnable() {
                    @Override
                    public void run() {
                        show();
                        Ui.toast(SettingsScreen.this, Words.Prefs.INDEX_BUILT);
                    }
                });
            }
        }).start();
    }

    private void refresh() {
        if (working) return;
        working = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                final long t0 = System.currentTimeMillis();
                final int changed = lib.refreshAsked();
                working = false;
                h.post(new Runnable() {
                    @Override
                    public void run() {
                        show();
                        Ui.toast(SettingsScreen.this, changed + Words.Prefs.CHANGES_DOT
                                + (System.currentTimeMillis() - t0) + Words.Prefs.MS);
                    }
                });
            }
        }).start();
    }

    /** רענון מעמיק (108, 114) — ברקע, עם הודעה בסוף. */
    private void deepRefresh() {
        if (working) return;
        working = true;
        Ui.toast(this, Words.Prefs.DEEP_RUNNING);
        new Thread(new Runnable() {
            @Override
            public void run() {
                final long t0 = System.currentTimeMillis();
                final int changed = lib.deepRefresh();
                working = false;
                h.post(new Runnable() {
                    @Override
                    public void run() {
                        show();
                        Ui.toast(SettingsScreen.this, changed + Words.Prefs.CHANGES_DOT + (System.currentTimeMillis() - t0) / 1000.0 + Words.Prefs.SECONDS);
                    }
                });
            }
        }).start();
    }

    /** 182: תקופת הניסיון — הסיכום, והשורות האחרונות מהיומן. */
    private void showTrial() {
        java.util.List<String> lines = Trial.lines();
        StringBuilder sb = new StringBuilder();
        if (!lib.pointsSupported()) sb.append(Words.Trial.NOT_SUPPORTED).append("\n\n");
        sb.append(Trial.text(lines));
        sb.append(Words.Prefs.LAST_FULL_LINE).append(Files.when(lib.lastFullBuild()))
                .append(Words.Prefs.LAST_DAILY_LINE).append(lib.lastDaily() > 0 ? Files.when(lib.lastDaily()) : Words.Prefs.NOT_YET);
        if (!lines.isEmpty()) {
            sb.append(Words.Prefs.LATEST);
            for (int i = Math.max(0, lines.size() - 15); i < lines.size(); i++) sb.append(lines.get(i)).append("\n");
        }
        sb.append(Words.Prefs.FILE_LINE).append(Paths.SYSTEM).append("/").append(Words.Trial.FILE).append(")");
        final String text = sb.toString();
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(13);
        tv.setTextIsSelectable(true);
        int p = Ui.dp(this, 14);
        tv.setPadding(p, p, p, p);
        ScrollView sv = new ScrollView(this);
        sv.addView(tv);
        new AlertDialog.Builder(this).setTitle(Words.Trial.TITLE).setView(sv)
                .setPositiveButton(Words.COPY, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) { Ui.copyText(SettingsScreen.this, Words.Trial.BUTTON, text); }
                })
                .setNegativeButton(Words.CLOSE, null).show();
    }

    private void checkPath() {
        final EditText et = Ui.field(this, "", "/storage/emulated/0/Download/...");
        LinearLayout b = Ui.form(this);
        b.addView(et);
        Ui.askForm(this, Words.Prefs.CHECK_PATH_TITLE, b, new Runnable() {
            @Override
            public void run() {
                String path = et.getText().toString().trim();
                boolean onDisk = new java.io.File(path).exists();
                boolean inIndex = false;
                for (Index ix : lib.indexes) {
                    for (Index.Rec r : ix.files)
                        if (!r.gone && ix.path(r).equals(path)) {
                            inIndex = true;
                            break;
                        }
                    if (inIndex) break;
                }
                new AlertDialog.Builder(SettingsScreen.this)
                        .setTitle(Words.Prefs.CHECK_RESULT)
                        .setMessage(Words.Prefs.ON_DRIVE + (onDisk ? Words.Prefs.EXISTS : Words.Prefs.NOT_FOUND)
                                + Words.Prefs.IN_INDEX_LINE + (inIndex ? Words.Prefs.FOUND : Words.Prefs.NOT_FOUND)
                                + (onDisk && !inIndex ? Words.Prefs.TRY_QUICK : ""))
                        .setPositiveButton(Words.CLOSE, null)
                        .show();
            }
        });
    }

    /** רשימות שנשארו בלי פונקציה (ממחיקות קודמות ומהשורש הישן). מוצגות לפני שנמחקות. */
    private void cleanOrphans() {
        java.util.Set<String> owned = new java.util.HashSet<String>();
        for (java.util.ArrayList<String> b : FnVirtual.allBases().keySet()) if (!b.isEmpty()) owned.add(b.get(0));
        Tree t = Tree.load();
        final java.util.List<String> orphans = new java.util.ArrayList<String>();
        for (Tree.Node c : t.root.children) if (!owned.contains(c.name)) orphans.add(c.name);
        final int loose = t.root.entries.size();
        if (orphans.isEmpty() && loose == 0) {
            Ui.toast(this, Words.Prefs.NO_ORPHANS);
            return;
        }
        StringBuilder names = new StringBuilder();
        for (String n : orphans) names.append(names.length() > 0 ? ", " : "").append(n);
        Ui.ask(this, Words.Prefs.DELETE_ORPHANS, Words.NO_TOUCH_FILES,
                (orphans.isEmpty() ? "" : orphans.size() + Words.Prefs.LISTS_COLON + names)
                        + (loose > 0 ? (orphans.isEmpty() ? "" : " · ") + loose + Words.Prefs.ITEMS_OLD_ROOT : ""),
                Words.BACKUP_SAVED, Words.Prefs.DELETE, new Runnable() {
                    @Override
                    public void run() {
                        synchronized (Tree.LOCK) {
                            Files.backupNamed(Paths.tree(), Words.Prefs.BEFORE_CLEAN);
                            Tree tt = Tree.load();
                            java.util.Iterator<Tree.Node> it = tt.root.children.iterator();
                            while (it.hasNext()) if (orphans.contains(it.next().name)) it.remove();
                            tt.root.entries.clear();
                            tt.save();
                        }
                        Ui.toast(SettingsScreen.this, Words.Prefs.DELETED);
                    }
                });
    }

    private void showLog() {
        final String text = Log.device() + "\n\n" + Log.read();
        TextView tv = new TextView(this);
        tv.setText(text.trim().isEmpty() ? Words.Prefs.LOG_EMPTY : text);
        tv.setTextSize(12);
        tv.setTextIsSelectable(true);
        int p = Ui.dp(this, 14);
        tv.setPadding(p, p, p, p);
        ScrollView sv = new ScrollView(this);
        sv.addView(tv);
        new AlertDialog.Builder(this)
                .setTitle(Words.Prefs.ERROR_LOG)
                .setView(sv)
                .setPositiveButton(Words.COPY, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) { Ui.copyText(SettingsScreen.this, Words.Prefs.LOG, text); }
                })
                .setNeutralButton(Words.Prefs.SHARE, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        Intent i = new Intent(Intent.ACTION_SEND);
                        i.setType("text/plain");
                        i.putExtra(Intent.EXTRA_TEXT, text);
                        startActivity(Intent.createChooser(i, Words.Prefs.SHARE_LOG));
                    }
                })
                .setNegativeButton(Words.Prefs.CLEAR, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        Log.clear();
                        Ui.toast(SettingsScreen.this, Words.Prefs.LOG_CLEARED);
                    }
                })
                .show();
    }
}
