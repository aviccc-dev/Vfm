package com.tikiyot.vfm;

// 1.17.1: בחירה מרובה במסך הבית (המילון — "בחירה מרובה"): נגיעה ארוכה על כלי מתחילה בחירה; נגיעה — עוד כלי (או
// ביטול שלו); בפס — רק מה שמתאים לכל מה שנבחר: רשימה ורשימה חכמה — קישור · העתקה · העברה; כלי אחד — הגדרות;
// לכולם — הזזה · הסרה (פקודה — "אפשר להסיר או לערוך; כל השאר לא נחוץ"). עד כאן — חלון לכלי אחד (ToolMenu.show).
// אחראי: הבחירה והפס. הפעולות — ToolMenu ו־VClip.

import android.app.Activity;
import android.content.DialogInterface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class HomeSelect {

    final LinearLayout view;
    private final TextView info;
    private final LinearLayout buttons;
    private final Activity a;
    private final Runnable rebuild;
    /** 1.23.2: סימון הבחירה בלבד — בלי לבנות את המסך מחדש (המשתמש: "סימנתי אחת אחת, וכל פעם המסך קופץ"). */
    private final Runnable marks;
    private final Set<String> ids = new LinkedHashSet<String>();

    HomeSelect(Activity a, Runnable rebuild, Runnable marks) {
        this.a = a;
        this.rebuild = rebuild;
        this.marks = marks;
        view = new LinearLayout(a);
        view.setOrientation(LinearLayout.VERTICAL);
        view.setBackgroundColor(Ui.HEADER);
        view.setPadding(Ui.dp(a, 8), Ui.dp(a, 4), Ui.dp(a, 8), Ui.dp(a, 4));
        info = new TextView(a);
        info.setTextColor(Ui.TEXT);
        info.setTextSize(14);
        view.addView(info);
        HorizontalScrollView sc = new HorizontalScrollView(a);
        buttons = new LinearLayout(a);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.CENTER_VERTICAL);
        sc.addView(buttons);
        view.addView(sc);
        view.setVisibility(View.GONE);
    }

    boolean on() {
        return !ids.isEmpty();
    }

    boolean has(String id) {
        return ids.contains(id);
    }

    void toggle(Structure.Func f) {
        if (!ids.remove(f.id)) ids.add(f.id);
        refresh();
        marks.run();
    }

    void stop() {
        ids.clear();
        refresh();
        rebuild.run();
    }

    private List<Structure.Func> picked() {
        List<Structure.Func> out = new ArrayList<Structure.Func>();
        for (Structure.Func f : Structure.load().direct) if (ids.contains(f.id)) out.add(f);
        return out;
    }

    /** הפס — לפי מה שנבחר עכשיו. */
    void refresh() {
        final List<Structure.Func> fs = picked();
        ids.clear();
        for (Structure.Func f : fs) ids.add(f.id);                            // כלי שהוסר בינתיים — יוצא מהבחירה
        buttons.removeAllViews();
        if (fs.isEmpty()) {
            view.setVisibility(View.GONE);
            return;
        }
        view.setVisibility(View.VISIBLE);
        info.setText(fs.size() + Words.ToolM.PICKED);
        boolean clip = true;
        for (Structure.Func f : fs)
            clip &= FnVirtual.KIND.equals(f.kind) || FnSmart.KIND.equals(f.kind) || FnDup.KIND.equals(f.kind);
        if (clip) {
            button(Words.VClipW.COPY, new Runnable() { @Override public void run() { clip(fs, false, false); } });
            button(Words.VClipW.DUPLICATE, new Runnable() { @Override public void run() { clip(fs, false, true); } });
            button(Words.VClipW.MOVE, new Runnable() { @Override public void run() { clip(fs, true, false); } });
        }
        if (fs.size() == 1) button(Words.VClipW.SETTINGS, new Runnable() {
            @Override
            public void run() {
                Structure.Func f = fs.get(0);
                stop();
                ToolMenu.settings(a, f, rebuild);
            }
        });
        button(Words.Manual.ORDER_DOTS, new Runnable() {
            @Override
            public void run() {
                Ui.options(a, Words.Manual.ORDER, new String[]{Words.Main.MOVE_RIGHT, Words.Main.MOVE_LEFT},
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int w) {
                                if (ToolMenu.shift(fs, w == 0 ? -1 : 1)) rebuild.run();
                            }
                        });
            }
        });
        button(Words.VClipW.REMOVE, new Runnable() {
            @Override
            public void run() {
                ToolMenu.remove(a, fs, new Runnable() {
                    @Override public void run() { stop(); }
                });
            }
        });
        button(Words.CANCEL, new Runnable() { @Override public void run() { stop(); } });
    }

    private void clip(List<Structure.Func> fs, boolean move, boolean full) {
        List<VClip.Item> items = new ArrayList<VClip.Item>();
        for (Structure.Func f : fs) items.add(new VClip.Item(VClip.TOOL, f.id, f.name));
        VClip.set(Modes.current(), null, items, move, full);
        stop();
        Ui.toast(a, VClip.label() + Words.VClipW.HINT);
    }

    private void button(String label, final Runnable r) {
        Button b = new Button(a);
        b.setText(label);
        b.setAllCaps(false);
        b.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { r.run(); }
        });
        buttons.addView(b);
    }
}
