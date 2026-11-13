package com.tikiyot.vfm;

// החלונות של הפעולות בכונן (מבנה 10, 13.11.2026): השאלה לפני הפעולה ("למחוק?", שם חדש, "שם תפוס"), ההתקדמות ו"עצור",
// וההודעה בסוף. עד כאן ישבו בתוך Physical — כשליש ממנו — והביצוע עצמו כבר קיבל "שואל" ו"התקדמות" מבחוץ.
// הכלל (המשתמש, 12.11.2026): "הפעולה רק עושה — לא שואלת". אותן שורות; מה שנשאר ב־Physical נקרא מכאן בשמו המלא.
// אחראי: לשאול, להציג התקדמות, להודיע. אסור לו: לגעת בקבצים בעצמו — רק דרך Physical.

import android.app.Activity;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

final class PhysicalUi {

    private PhysicalUi() { }

    // ---------- 142, 149: הדבקה מהלוח — מקום אחד (מנהל הקבצים והרשימה החכמה) ----------

    /** מה שבלוח — אל dir, עם חלון האישור. after — אחרי הביצוע (גם אם נכשל). */
    static void paste(final Activity a, File dir, final Runnable after) {
        if (dir == null || Clip.isEmpty()) return;
        String why = Clip.cannotPasteInto(dir);
        if (why != null) {
            Ui.toast(a, why);
            return;
        }
        final boolean copy = Clip.isCopy();
        if (!copy) {
            String no = Physical.cannotMove(Library.get(a), Clip.files(), Clip.dirs());
            if (no != null) {
                Ui.toast(a, no);
                return;
            }
        }
        askTransferInto(a, new ArrayList<String>(Clip.files()), new ArrayList<String>(Clip.dirs()), dir, copy, Clip.warning(),
                new Physical.Done() {
                    @Override
                    public void done(Physical.Result r) {
                        if (r.failed == 0 || !r.done.isEmpty()) Clip.clear();
                        if (after != null) after.run();
                    }
                });
    }

    /** מחיקה מהכונן, בחלון האישור בתבנית הקבועה. warning — אזהרה נוספת (למשל "לא יישאר אף עותק"). */
    public static void askDelete(final Activity a, final List<String> paths, String warning, final Physical.Done done) {
        askDelete(a, paths, java.util.Collections.<String>emptyList(), warning, done);
    }

    /** מחיקה מהכונן של קבצים ותיקיות (82). */
    public static void askDelete(final Activity a, final List<String> paths, final List<String> dirs,
                                 String warning, final Physical.Done done) {
        final Library lib = Library.get(a);
        for (String p : paths)
            if (Physical.programFile(p) != null) {                             // 90
                Ui.toast(a, Words.Disk.CANT_DELETE + new File(p).getName() + "\": " + Physical.programFile(p));
                return;
            }
        long size = 0;
        int known = 0, inner = 0;
        List<String> all = new ArrayList<String>(paths);
        for (String p : paths) {
            Library.Hit h = lib.find(p);
            if (h != null && h.rec != null) {
                size += h.rec.size;
                known++;
            }
        }
        for (String d : dirs) {
            String why = Physical.forbidden(lib, new File(d));
            if (why != null) {
                Ui.toast(a, Words.Disk.CANT_DELETE + new File(d).getName() + "\": " + why);
                return;
            }
            long[] st = lib.treeStats(d);
            inner += st[0];
            size += st[1];
            known++;
            all.addAll(lib.filesUnder(d));
        }
        if (known == 0) {
            Ui.toast(a, Words.Disk.FILES_NOT_IN_INDEX);
            return;
        }
        final boolean grey = Acts.deletedStaysGrey();
        int inLists = Physical.listsCount(all);
        String lists = inLists == 0 ? "" : grey ? Words.Disk.LISTS_MARK_DELETED : Words.Disk.LISTS_REMOVE_FROM + Words.items(inLists) + Words.Disk.IN_LISTS_SUFFIX;
        String files;
        if (dirs.isEmpty()) files = paths.size() == 1 ? Words.Disk.FILE_TO_TRASH : Words.Disk.FILES_TO_TRASH;
        else if (paths.isEmpty() && dirs.size() == 1)
            files = Words.Disk.FOLDER_AND + (inner == 1 ? Words.Disk.ITS_FILE : inner + Words.Disk.ITS_FILES) + Words.Disk.GO_TO_TRASH;
        else files = Words.Disk.ALL_INCLUDING + Words.files(inner) + Words.Disk.IN_FOLDERS_TO_TRASH;
        String undo = dirs.isEmpty() ? Words.Disk.UNDO_FROM_TRASH : Words.Disk.UNDO_FOLDER_FROM_TRASH;
        Ui.ask(a, Words.Disk.DELETE_TITLE + Physical.whatAll(paths, dirs), files,
                Files.size(size) + Words.Disk.SPACE_ON_EMPTY + (warning == null ? "" : "\n" + warning) + Physical.namesOf(paths, dirs, 12),   // 141; 102: מתי המקום מתפנה
                undo + lists, Words.Disk.DELETE_ON_DRIVE, new Runnable() {
                    @Override
                    public void run() {
                        perform(a, Words.Disk.DELETED, Words.Disk.REMOVED, done, new Op() {
                            @Override public Physical.Result go() { return Physical.delete(lib, paths, dirs, grey); }
                        });
                    }
                });
    }

    /**
     * העתקה או העברה אל תיקייה ידועה — אחרי בחירה בבורר, או "הדבקה כאן" (142). כל הבדיקות של היעד,
     * המקום, ואז חלון האישור בתבנית (5).
     */
    public static void askTransferInto(final Activity a, final List<String> paths, final List<String> dirs,
                                       final File folder, final boolean copy, final String warning, final Physical.Done done) {
        final Library lib = Library.get(a);
        if (Physical.programFile(folder.getAbsolutePath()) != null) {      // 90
            Ui.toast(a, Words.Disk.DEST_APP_NOT);
            return;
        }
        String droot = lib.rootOf(folder.getAbsolutePath());
        if (droot != null && Physical.inside(folder, new File(droot, Index.TRASH))) {
            Ui.toast(a, Words.Disk.DEST_TRASH_NOT);
            return;
        }
        for (String d : dirs)
            if (Physical.inside(folder, new File(d))) {
                Ui.toast(a, Words.Disk.CANT + (copy ? Words.Disk.TO_COPY : Words.Disk.TO_MOVE) + Words.Disk.FOLDER_INTO_ITSELF);
                return;
            }
        long size = 0;
        int inner = 0;
        List<String> all = new ArrayList<String>(paths);
        for (String p : paths) size += new File(p).length();
        for (String d : dirs) {
            long[] st = lib.treeStats(d);
            inner += st[0];
            size += st[1];
            all.addAll(lib.filesUnder(d));
        }
        boolean across = false;
        String to = lib.rootOf(folder.getAbsolutePath());
        for (String p : all.isEmpty() ? dirs : all)
            if (to == null || !to.equals(lib.rootOf(p))) across = true;
        long need = copy || across ? size : 0;
        long free = Physical.free(folder);
        if (need > 0 && free < need) {
            Ui.ask(a, Words.Disk.NO_ROOM_IN + folder.getName() + "\"", Words.NO_TOUCH_FILES,
                    Words.Disk.NEED + Files.size(need) + Words.Disk.FREE + Files.size(free), null, Words.Disk.GOT_IT, new Runnable() {
                        @Override public void run() { }
                    });
            return;
        }
        String what = Physical.whatAll(paths, dirs);
        String files = copy ? Words.Disk.SOURCE_UNCHANGED
                : (paths.size() + dirs.size() == 1 ? (dirs.isEmpty() ? Words.Disk.FILE_MOVES : Words.Disk.FOLDER_MOVES)
                : Words.Disk.ALL_MOVES) + Words.Disk.ON_DRIVE_TO + folder.getName();
        String detail = folder.getAbsolutePath() + " · " + Files.size(size)
                + (inner > 0 ? " · " + inner + Words.Disk.FILES_IN_FOLDERS : "")
                + (across && !copy ? Words.Disk.ACROSS_DRIVES : "")
                + (warning == null ? "" : "\n" + warning)
                + Physical.namesOf(paths, dirs, 12);                                       // 141
        int inLists = copy ? 0 : Physical.listsCount(all);
        String undo = copy ? Words.Disk.COPIES_NOT_IN_LISTS
                : (inLists > 0 ? Words.Disk.LISTS_FOLLOW_OPEN + Words.items(inLists) + ")" : Words.Disk.LISTS_FOLLOW)
                + Words.Disk.TAKEN_ASK;
        final boolean grey = Acts.deletedStaysGrey();
        Ui.ask(a, (copy ? Words.Disk.COPY_TO : Words.Disk.MOVE_TO) + folder.getName() + "\": " + what, files, detail, undo,
                copy ? Words.COPY : Words.MOVE, new Runnable() {
                    @Override
                    public void run() {
                        final Busy busy = new Busy(a, copy ? Words.Disk.COPYING : Words.Disk.MOVING);
                        perform(a, copy ? Words.Disk.COPIED : Words.Disk.MOVED, copy ? Words.Disk.REMOVED : Words.Disk.UPDATED, done, new Op() {
                            @Override
                            public Physical.Result go() {
                                try {
                                    return Physical.transfer(lib, paths, dirs, folder, copy, new DialogAsker(a), busy, grey);
                                } finally {
                                    busy.close();
                                }
                            }
                        });
                    }
                });
    }

    // ---------- חלון "שם תפוס" (82) ----------

    /** שואל במסך וממתין לתשובה בחוט הרקע. */
    static final class DialogAsker implements Physical.Asker {
        final Activity a;

        DialogAsker(Activity a) {
            this.a = a;
        }

        @Override
        public Physical.Answer ask(final File existing, final File incoming, final String suggested) {
            final Physical.Answer[] out = {null};
            final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
            a.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        show(existing, incoming, suggested, out, latch);
                    } catch (Throwable t) {
                        Log.e("חלון שם תפוס", t);
                        latch.countDown();
                    }
                }
            });
            try {
                latch.await();
            } catch (InterruptedException e) {
                return new Physical.Answer(Physical.Choice.CANCEL, null, false);
            }
            return out[0] != null ? out[0] : new Physical.Answer(Physical.Choice.CANCEL, null, false);
        }

        private void show(File existing, File incoming, String suggested, final Physical.Answer[] out,
                          final java.util.concurrent.CountDownLatch latch) {
            final boolean dirs = existing.isDirectory() && incoming.isDirectory();
            android.widget.LinearLayout box = Ui.form(a);
            String where = existing.getParentFile() != null ? existing.getParentFile().getName() : "";
            android.widget.TextView t = Ui.label(a, "\"" + existing.getName() + Words.Disk.ALREADY
                    + (existing.isDirectory() ? Words.Disk.EXISTS_FEM : Words.Disk.EXISTS) + Words.Disk.IN_QUOTE + where + "\"");
            t.setTextSize(17);
            t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            box.addView(t);
            box.addView(Ui.label(a, Words.Disk.EXISTING + describe(existing) + Words.Disk.INCOMING + describe(incoming)));
            box.addView(Ui.label(a, Words.Disk.OTHER_NAME_LABEL));
            final android.widget.EditText name = Ui.field(a, suggested, "");
            box.addView(name);
            final android.widget.CheckBox all = Ui.check(a, Words.Disk.SAME_FOR_REST, false);
            box.addView(all);
            final android.app.AlertDialog[] dlg = {null};
            final String[] labels = dirs ? new String[]{Words.Disk.MERGE, Words.Disk.DELETE_PREVIOUS_FEM, Words.Disk.OTHER_NAME, Words.Disk.CANCEL_SHORT}
                    : new String[]{Words.Disk.DELETE_PREVIOUS, Words.Disk.OTHER_NAME, Words.Disk.CANCEL_SHORT};
            final Physical.Choice[] choices = dirs ? new Physical.Choice[]{Physical.Choice.MERGE, Physical.Choice.REPLACE, Physical.Choice.RENAME, Physical.Choice.CANCEL}
                    : new Physical.Choice[]{Physical.Choice.REPLACE, Physical.Choice.RENAME, Physical.Choice.CANCEL};
            for (int i = 0; i < labels.length; i++) {
                final Physical.Choice c = choices[i];
                android.widget.Button b = new android.widget.Button(a);
                b.setText(labels[i]);
                b.setAllCaps(false);
                if (c == Physical.Choice.REPLACE) b.setTextColor(Actions.WARNING);
                b.setOnClickListener(new android.view.View.OnClickListener() {
                    @Override
                    public void onClick(android.view.View v) {
                        out[0] = new Physical.Answer(c, name.getText().toString(), all.isChecked() && c != Physical.Choice.CANCEL);
                        if (dlg[0] != null) dlg[0].dismiss();
                        latch.countDown();
                    }
                });
                box.addView(b);
            }
            android.widget.ScrollView sv = new android.widget.ScrollView(a);
            sv.addView(box);
            dlg[0] = new android.app.AlertDialog.Builder(a).setView(sv).setCancelable(false).create();
            dlg[0].show();
        }

        private static String describe(File f) {
            if (f.isDirectory()) {
                long[] c = Physical.count(f);
                return Words.Disk.FOLDER_DOT + Files.count(c[0]) + Words.Disk.FILES_DOT + Files.size(c[1]);
            }
            return Files.size(f.length()) + " · " + Ui.date(f.lastModified());
        }
    }

    // ---------- התקדמות ו"עצור" (82) ----------

    /** חלון התקדמות עם "עצור" — להעברה, להעתקה, לפריסה ולאריזה לשליחה. */
    static final class Busy implements Physical.Progress {
        final Activity a;
        volatile boolean stop;
        volatile android.app.AlertDialog dlg;
        android.widget.TextView text;
        long last;

        Busy(final Activity a, final String title) {
            this.a = a;
            a.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        text = Ui.label(a, title);
                        int p = Ui.dp(a, 22);
                        text.setPadding(p, p, p, p);
                        dlg = new android.app.AlertDialog.Builder(a).setView(text).setCancelable(false)
                                .setNegativeButton(Words.STOP, new android.content.DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(android.content.DialogInterface d, int w) {
                                        stop = true;
                                        Ui.toast(a, Words.Disk.STOPPING);
                                    }
                                }).create();
                        dlg.show();
                    } catch (Throwable t) {
                        Log.e("חלון התקדמות", t);
                    }
                }
            });
        }

        @Override
        public void step(final int files, final int total, final long bytes, final long totalBytes) {
            long now = System.currentTimeMillis();
            if (now - last < 250 && files < total) return;
            last = now;
            a.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (text != null) text.setText(files + Words.Disk.OF + total + " · "
                            + Files.size(bytes) + Words.Disk.OF + Files.size(totalBytes));
                }
            });
        }

        @Override
        public boolean stopped() {
            return stop;
        }

        void close() {
            a.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (dlg != null && dlg.isShowing()) dlg.dismiss();
                    } catch (Throwable t) { Log.e("Physical.1244", t); }   // מ0.32: לא בולעים בשקט
                }
            });
        }
    }

    public static void askRestore(final Activity a, final List<Trash.Item> list, final Physical.Done done) {
        if (list.isEmpty()) return;
        int unknown = 0;
        List<String> shown = new ArrayList<String>();
        for (Trash.Item it : list) {
            if (it.original == null) unknown++;
            shown.add(TrashScreen.shownName(it.name()));
        }
        Ui.ask(a, Words.Disk.RESTORE_TITLE + Physical.what(shown), Words.Disk.RESTORE_FILES_BACK,
                list.size() == 1 && list.get(0).original != null ? list.get(0).original
                        : (unknown > 0 ? unknown + Words.Disk.UNKNOWN_STAY : Words.Disk.TAKEN_RESTORED),
                Words.Disk.RESTORE_NOT_LISTS, Words.Disk.RESTORE, new Runnable() {
                    @Override
                    public void run() {
                        perform(a, Words.Disk.RESTORED, Words.Disk.UPDATED, done, new Op() {
                            @Override public Physical.Result go() { return Physical.restore(Library.get(a), list); }
                        });
                    }
                });
    }

    public static void askDeleteForever(final Activity a, final List<Trash.Item> list, final Physical.Done done) {
        if (list.isEmpty()) return;
        long size = 0;
        List<String> shown = new ArrayList<String>();
        for (Trash.Item it : list) {
            size += it.size;
            shown.add(TrashScreen.shownName(it.name()));
        }
        Ui.ask(a, Words.Disk.PURGE_TITLE + Physical.what(shown), Words.Disk.PURGE_FILES,
                Files.size(size) + Words.Disk.PURGE_GREY,
                Words.Disk.NO_UNDO, Words.Disk.PURGE, new Runnable() {
                    @Override
                    public void run() {
                        perform(a, Words.Disk.DELETED, Words.Disk.REMOVED, done, new Op() {
                            @Override public Physical.Result go() { return Physical.deleteForever(Library.get(a), list); }
                        });
                    }
                });
    }

    /** שינוי שם של קובץ בכונן: שם חדש, ואז חלון אישור בתבנית של הפעולות הפיזיות (5). */
    public static void askRename(final Activity a, final String path, final Physical.Done done) {
        final File f = new File(path);
        if (Physical.programFile(path) != null) {
            Ui.toast(a, Words.Disk.CANT_RENAME + f.getName() + "\": " + Physical.programFile(path));
            return;
        }
        if (!f.isFile()) {
            Ui.toast(a, Words.FILE_NOT_AT_PATH);
            return;
        }
        Ui.askText(a, Words.RENAME_ON_DRIVE, f.getName(), Words.Disk.FILE_NAME, new Ui.Text1() {
            @Override
            public void ok(String value) {
                final String name = value.trim();
                String bad = Physical.badName(name);
                if (bad != null) {
                    Ui.toast(a, bad);
                    return;
                }
                if (name.equals(f.getName())) return;
                if (!name.equalsIgnoreCase(f.getName()) && new File(f.getParentFile(), name).exists()) {
                    Ui.toast(a, Words.Disk.ALREADY_IN_FOLDER + name + "\"");
                    return;
                }
                String oldExt = Text.ext(f.getName()), newExt = Text.ext(name);
                int inLists = Physical.listsCount(java.util.Collections.singletonList(path));
                String detail = f.getName() + "\n← " + name + Words.Disk.IN_FOLDER_LINE + f.getParent()
                        + (!oldExt.equals(newExt) ? Words.Disk.EXT_CHANGES + (oldExt.isEmpty() ? Words.Disk.WITHOUT : oldExt)
                        + " ← " + (newExt.isEmpty() ? Words.Disk.WITHOUT : newExt) + Words.Disk.EXT_WARNING : "");
                String undo = (inLists > 0 ? Words.Disk.LISTS_FOLLOW_OPEN + Words.items(inLists) + ")" : Words.Disk.LISTS_FOLLOW)
                        + Words.Disk.CAN_RENAME_AGAIN;
                final Library lib = Library.get(a);
                Ui.ask(a, Words.Disk.RENAME_TITLE + f.getName(), Words.Disk.RENAME_FILE_ITSELF, detail, undo,
                        Words.Disk.RENAME, new Runnable() {
                            @Override
                            public void run() {
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Physical.Result x;
                                        try {
                                            x = Physical.rename(lib, path, name);
                                        } catch (Throwable t) {
                                            Log.e("שינוי שם בכונן", t);
                                            x = new Physical.Result();
                                            x.fail(Words.Disk.ERROR);
                                        }
                                        final Physical.Result r = x;
                                        Ui.afterIndex(a, new Runnable() {
                                            @Override
                                            public void run() {
                                                Ui.toast(a, r.failed > 0 ? Words.Disk.NOT_RENAMED + r.note
                                                        : Words.Disk.RENAMED_TO + name + "\""
                                                        + (r.lists > 0 ? Words.Disk.UPDATED_SUFFIX + Words.items(r.lists) + Words.Disk.IN_LISTS_SUFFIX : ""));
                                                if (done != null) done.done(r);
                                            }
                                        });
                                    }
                                }).start();
                            }
                        });
            }
        });
    }

    /** פריסה של קובץ ZIP לתיקייה חדשה לידו — שינוי בכונן, עם חלון האישור בתבנית (5). */
    public static void askUnzip(final Activity a, final String path, final Physical.Done done) {
        final File zip = new File(path);
        if (!zip.isFile()) {
            Ui.toast(a, Words.FILE_NOT_AT_PATH);
            return;
        }
        final File dest = Zip.target(zip);
        if (Physical.programFile(dest.getAbsolutePath()) != null) {
            Ui.toast(a, Words.Disk.NO_UNPACK_IN_APP);
            return;
        }
        long[] c = Zip.contents(zip);
        if (c == null) {
            Ui.toast(a, Words.Disk.BAD_ZIP);
            return;
        }
        long free = Physical.free(zip.getParentFile());
        if (c[1] > 0 && free < c[1]) {
            Ui.ask(a, Words.Disk.NO_ROOM_UNPACK, Words.NO_TOUCH_FILES,
                    Words.Disk.NEED + Files.size(c[1]) + Words.Disk.FREE + Files.size(free), null, Words.Disk.GOT_IT, new Runnable() {
                        @Override public void run() { }
                    });
            return;
        }
        final Library lib = Library.get(a);
        Ui.ask(a, Words.Disk.UNPACK_TITLE + zip.getName(), Words.Disk.UNPACK_CREATES,
                Words.files(c[0]) + " · " + Files.size(c[1]) + Words.Disk.TO_LINE + dest.getAbsolutePath(),
                Words.Disk.ZIP_STAYS, Words.Disk.UNPACK, new Runnable() {
                    @Override
                    public void run() {
                        final Busy busy = new Busy(a, Words.Disk.UNPACKING);
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                final Zip.Result r = Zip.unpack(zip, dest, busy);
                                busy.close();
                                lib.dirChanged(zip.getParent());           // האינדקס לומד על התיקייה החדשה
                                Log.i("נפרס בכונן: " + zip + " ← " + dest + " · " + r.files + " קבצים");
                                Ui.afterIndex(a, new Runnable() {
                                    @Override
                                    public void run() {
                                        String msg = r.error != null ? r.error
                                                : Words.Disk.UNPACKED + Words.files(r.files) + Words.Disk.TO_QUOTE + dest.getName() + "\""
                                                + (r.skipped > 0 ? " · " + r.skipped + Words.Disk.SKIPPED : "")
                                                + (r.stopped ? Words.Disk.STOPPED_SUFFIX : "");
                                        Ui.toast(a, msg);
                                        if (done != null) done.done(new Physical.Result());
                                    }
                                });
                            }
                        }).start();
                    }
                });
    }

    /** 1.11.0: דחיסת תיקייה ל־ZIP לידה (שם פנוי), עם חלון אישור; התיקייה נשארת. */
    public static void askZip(final Activity a, final File dir, final Runnable after) {
        if (!dir.isDirectory()) return;
        if (Physical.programFile(dir.getAbsolutePath()) != null) {
            Ui.toast(a, Words.Disk.NO_FILE_IN_APP);
            return;
        }
        final File zip = new File(dir.getParentFile(), Physical.freeName(dir.getParentFile(), dir.getName() + ".zip", false));
        long[] m = Zip.measure(dir);
        Ui.ask(a, Words.DiskW.ZIP_TITLE + dir.getName(), Words.DiskW.ZIP_CREATES,
                Words.files(m[0]) + " · " + Files.size(m[1]) + Words.Disk.TO_LINE + zip.getAbsolutePath(),
                null, Words.Disk.CREATE, new Runnable() {
                    @Override
                    public void run() {
                        final Busy busy = new Busy(a, Words.DiskW.ZIPPING);
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                final Zip.Result r = Zip.pack(dir, zip, busy);
                                busy.close();
                                Library.get(a).dirChanged(dir.getParent());
                                Log.i("נדחס: " + dir + " ← " + zip);
                                a.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Ui.toast(a, r.error != null ? r.error : Words.DiskW.ZIPPED + zip.getName());
                                        if (after != null) after.run();
                                    }
                                });
                            }
                        }).start();
                    }
                });
    }

    /** קובץ טקסט חדש בכונן (קובץ פקודות, 117א) — עם חלון אישור בתבנית. לא מחליף קובץ קיים. */
    public static void askNewFile(final Activity a, final File f, final String content, final Runnable onMade) {
        String bad = Physical.badName(f.getName());
        if (bad != null) {
            Ui.toast(a, bad);
            return;
        }
        if (Physical.programFile(f.getAbsolutePath()) != null) {
            Ui.toast(a, Words.Disk.NO_FILE_IN_APP);
            return;
        }
        if (f.exists()) {
            Ui.toast(a, Words.Disk.ALREADY_NAME + f.getName() + "\"");
            return;
        }
        boolean newDir = !f.getParentFile().isDirectory();                     // 140
        Ui.ask(a, Words.Disk.NEW_FILE_TITLE + f.getName(),
                newDir ? Words.Disk.WILL_CREATE_FOLDER + f.getParentFile().getName() + Words.Disk.AND_FILE : Words.Disk.NEW_FILE_CREATED,
                Words.Disk.INSIDE + f.getParent(),
                Words.Disk.CAN_DELETE_IT, Words.Disk.CREATE, new Runnable() {
                    @Override
                    public void run() {
                        try {
                            f.getParentFile().mkdirs();
                            Files.writeAtomic(f, content);
                        } catch (Throwable t) {
                            Log.e("קובץ חדש", t);
                            Ui.toast(a, Words.Disk.CANT_CREATE_FILE);
                            return;
                        }
                        Library.get(a).dirChanged(f.getParent());
                        Log.i("נוצר קובץ בכונן: " + f);
                        onMade.run();
                    }
                });
    }

    /** יצירת תיקייה בכונן, עם חלון אישור בתבנית של הפעולות הפיזיות. onMade — אחרי שנוצרה. */
    public static void askMakeFolder(final Activity a, final File parent, final String name, final Runnable onMade) {
        String bad = Physical.badName(name);
        if (bad != null) {
            Ui.toast(a, bad);
            return;
        }
        final String n = name.trim();
        Ui.ask(a, Words.Disk.NEW_FOLDER_TITLE + n, Words.Disk.NEW_EMPTY_FOLDER,
                Words.Disk.INSIDE + parent.getAbsolutePath(), Words.Disk.CAN_DELETE_IT_FEM, Words.Disk.CREATE, new Runnable() {
                    @Override
                    public void run() {
                        String why = Physical.makeFolder(Library.get(a), parent, n);
                        if (why != null) {
                            Ui.toast(a, why);
                            return;
                        }
                        Ui.toast(a, Words.Disk.FOLDER_CREATED + n + "\"");
                        onMade.run();
                    }
                });
    }

    private interface Op {
        Physical.Result go();
    }

    /** הביצוע ברקע; ההודעה והעדכון במסך — אחרי שהאינדקס כבר מראה את השינוי. */
    private static void perform(final Activity a, final String verb, final String listsVerb, final Physical.Done done, final Op op) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final Physical.Result[] r = {null};
                try {
                    r[0] = op.go();
                } catch (Throwable t) {
                    Log.e("פעולה פיזית", t);
                    r[0] = new Physical.Result();
                    r[0].fail(Words.Disk.ERROR);
                }
                Ui.afterIndex(a, new Runnable() {
                    @Override
                    public void run() {
                        Ui.toast(a, Physical.message(verb, listsVerb, r[0]));
                        if (done != null) done.done(r[0]);
                    }
                });
            }
        }).start();
    }
}
