package com.tikiyot.vfm;

// 190: הממשק של החתימות — המצב (בהגדרות), "זרז", ואיתור כל החסרים ברשימות (בהגדרות וברשימה ידנית).
// המנגנון — Sig. אסור לו: לגעת בקבצים.

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import java.util.LinkedHashMap;
import java.util.Map;

final class SigUi {

    private SigUi() { }

    /** "נוסף" ידוע — אחוז מהקבצים (בלי סל). */
    static int addedPercent(Library lib) {
        if (lib == null || !lib.ready) return -1;
        long all = 0, known = 0;
        for (Index ix : lib.indexes)
            for (Index.Rec r : ix.files)
                if (!r.gone && !ix.trash(r)) {
                    all++;
                    if (r.added > 0) known++;
                }
        return all == 0 ? 100 : (int) (100 * known / all);
    }

    /** השורה על הכפתור בהגדרות. */
    static String line(Library lib) {
        int[] ix = Sig.indexStatus(lib);
        return Words.Bg.line(ix[0], ix[1], Sig.lastDone, Sig.lastTotal, Sig.running(), addedPercent(lib));
    }

    /** חלון המצב: מה כבר נעשה, מה רץ, ו"זרז" (עם התקדמות שמתעדכנת). */
    static void show(final Activity a, final Library lib, final Runnable changed) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final Sig.Status s = Sig.status(lib, Tree.load());
                Sig.lastDone = s.done;
                Sig.lastTotal = s.total;
                final int added = addedPercent(lib);
                final int[] ix = Sig.indexStatus(lib);
                a.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (a.isFinishing()) return;
                        final TextView tv = new TextView(a);
                        int p = Ui.dp(a, 20);
                        tv.setPadding(p, p, p, p);
                        tv.setTextSize(15);
                        tv.setTextColor(Ui.TEXT);
                        tv.setText(Words.Bg.detail(ix[0], ix[1], s.done, s.total, Sig.running(), added));
                        AlertDialog.Builder b = new AlertDialog.Builder(a).setTitle(Words.Bg.TITLE).setView(tv)
                                .setNegativeButton(Words.CLOSE, null);
                        if (!s.todo.isEmpty() || ix[0] < ix[1])
                            b.setPositiveButton(Words.Bg.FAST, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface d, int w) {
                                    Sig.fast = true;
                                    Sig.kick(lib);
                                    progress(a, lib, changed);
                                }
                            });
                        b.show();
                    }
                });
            }
        }).start();
    }

    /** "זרז": חלון שמתעדכן עד שהחתימות נגמרות (אפשר לסגור — העבודה ממשיכה). */
    private static void progress(final Activity a, final Library lib, final Runnable changed) {
        final TextView tv = new TextView(a);
        int p = Ui.dp(a, 20);
        tv.setPadding(p, p, p, p);
        tv.setTextSize(15);
        tv.setTextColor(Ui.TEXT);
        tv.setText(Words.Bg.FASTING);
        final AlertDialog d = new AlertDialog.Builder(a).setTitle(Words.Bg.TITLE).setView(tv)
                .setNegativeButton(Words.CLOSE, null).show();
        final Handler h = new Handler(Looper.getMainLooper());
        h.post(new Runnable() {
            @Override
            public void run() {
                if (!d.isShowing()) {
                    if (changed != null) changed.run();
                    return;
                }
                int[] ix = Sig.indexStatus(lib);
                tv.setText(Words.Bg.progress(ix[0], ix[1], Sig.running()));
                if (Sig.running()) h.postDelayed(this, 500);
                else if (changed != null) changed.run();
            }
        });
    }

    /** איתור כל החסרים בכל הרשימות: סיכום, ואז עדכון באישור (עם גיבוי). */
    static void relocateAll(final Activity a, final Library lib, final Runnable after) {
        if (!lib.ready) {
            Ui.toast(a, Words.Bg.INDEX_NOT_READY);
            return;
        }
        Ui.toast(a, Words.Bg.SEARCHING);
        new Thread(new Runnable() {
            @Override
            public void run() {
                Tree t = Tree.load();
                final Sig.Plan pl = Sig.plan(lib, t);
                final Sig.Status s = Sig.status(lib, t);
                a.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (a.isFinishing()) return;
                        if (pl.missing == 0) {
                            Ui.toast(a, Words.Bg.NO_MISSING);
                            return;
                        }
                        final int v = pl.verified.size(), n = pl.byNameSize.size();
                        String detail = Words.Bg.summary(pl.missing, v, n, pl.several, pl.differ, pl.notFound, s.percent());   // הרשימות
                        if (v + n == 0) {
                            new AlertDialog.Builder(a).setTitle(Words.Bg.RELOCATE_TITLE).setMessage(detail)
                                    .setPositiveButton(Words.CLOSE, null).show();
                            return;
                        }
                        Ui.ask(a, Words.Bg.RELOCATE_TITLE, Words.NO_TOUCH_FILES, detail, Words.Manual.BACKUP_BEFORE_CHANGE,
                                v > 0 && n > 0 ? Words.Bg.CONTINUE : Words.Bg.UPDATE, new Runnable() {
                                    @Override
                                    public void run() {
                                        if (v > 0 && n > 0) choose(a, pl, after);
                                        else done(a, apply(pl, n > 0), after);
                                    }
                                });
                    }
                });
            }
        }).start();
    }

    private static void choose(final Activity a, final Sig.Plan pl, final Runnable after) {
        Ui.options(a, Words.Bg.WHICH, new String[]{
                        Words.Bg.allOf(pl.verified.size() + pl.byNameSize.size()),
                        Words.Bg.onlyVerified(pl.verified.size())},
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        done(a, apply(pl, w == 0), after);
                    }
                });
    }

    private static int apply(Sig.Plan pl, boolean withNameSize) {
        Map<String, String> m = new LinkedHashMap<String, String>(pl.verified);
        if (withNameSize) m.putAll(pl.byNameSize);
        int n = Sig.apply(m);
        Log.i("190: איתור החסרים — עודכנו " + n + " (אומתו " + pl.verified.size() + ", לפי שם וגודל "
                + (withNameSize ? pl.byNameSize.size() : 0) + ")");
        return n;
    }

    private static void done(Activity a, int n, Runnable after) {
        Ui.toast(a, n > 0 ? Words.Manual.PATH_UPDATED_IN + n + Words.Manual.LISTS_SUFFIX : Words.Manual.NOTHING_TO_UPDATE);
        if (after != null) after.run();
    }
}
