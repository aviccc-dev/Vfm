package com.tikiyot.vfm;

// מ0.24: השורה של הלוח בתחתית מסך הרשימה של המסד (המשתמש: "בלוח: 3 להעתקה, 2 להעברה"; נגיעה — רשימת הפריטים,
// ושם אפשר להוציא פריט). כפתורים: "הדבקה כאן", "צירוף" (מצב שנשמר), "ביטול" (מרוקן את הלוח). מה קורה בהדבקה —
// DbListScreen (דרך DbClip). אחראי: השורה בלבד.

import android.app.Activity;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

final class DbPasteBar {

    /** המקום: האופן, וההדבקה עצמה. */
    interface Place {
        String mode();

        void paste();
    }

    final LinearLayout view;
    private final TextView text;
    private final Button paste, add;
    private final Activity a;
    private final Place place;

    DbPasteBar(Activity a, Place place) {
        this.a = a;
        this.place = place;
        view = new LinearLayout(a);
        view.setOrientation(LinearLayout.HORIZONTAL);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setBackgroundColor(Ui.HEADER);
        view.setPadding(Ui.dp(a, 8), Ui.dp(a, 4), Ui.dp(a, 8), Ui.dp(a, 4));
        text = new TextView(a);
        text.setTextColor(Ui.TEXT);
        text.setTextSize(14);
        text.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { show(); }
        });
        view.addView(text, new LinearLayout.LayoutParams(0, -2, 1f));
        paste = button(Words.PASTE_HERE, new Runnable() {
            @Override public void run() { DbPasteBar.this.place.paste(); }
        });
        add = button(Words.DbClipW.ADD_OFF, new Runnable() {
            @Override
            public void run() {
                boolean on = !adding();
                Conf.setBool(Conf.GENERAL, Conf.CLIP_ADD, on);
                Ui.toast(DbPasteBar.this.a, on ? Words.DbClipW.ADD_ON_TOAST : Words.DbClipW.ADD_OFF_TOAST);
                refresh();
            }
        });
        button(Words.CANCEL, new Runnable() {
            @Override
            public void run() {
                DbClip.clear();
                Clip.clear();
                refresh();
            }
        });
        view.setVisibility(View.GONE);
    }

    /** "העתקה"/"העברה" מוסיפות ללוח (ברירת המחדל שנשמרה). */
    static boolean adding() {
        return Conf.is(Conf.GENERAL, Conf.CLIP_ADD, false);
    }

    private Button button(String label, final Runnable r) {
        Button b = new Button(a);
        b.setText(label);
        b.setAllCaps(false);
        b.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { r.run(); }
        });
        view.addView(b);
        return b;
    }

    /** מה בלוח עכשיו (בכל חזרה למסך — אולי סומן משהו במסך אחר, או הועתק בכונן). */
    void refresh() {
        boolean disk = DbClip.diskForLists();
        if (DbClip.empty() && !disk) {
            view.setVisibility(View.GONE);
            return;
        }
        view.setVisibility(View.VISIBLE);
        String why = DbClip.cannot(place.mode());
        String s = DbClip.label();
        if (disk) s += (s.isEmpty() ? "" : " · ") + Clip.label().replace(Words.ClipW.TO_COPY, "") + Words.DbClipW.FROM_DISK;
        text.setText(Words.DbClipW.IN_CLIP + s + (why != null ? "\n" + why : ""));
        paste.setEnabled(why == null || disk);
        add.setText(adding() ? Words.DbClipW.ADD_ON : Words.DbClipW.ADD_OFF);
    }

    /** רשימת מה שבלוח; נגיעה — הפריט יוצא מהלוח. */
    private void show() {
        final List<DbClip.Item> items = DbClip.items();
        final boolean disk = DbClip.diskForLists();
        List<String> lines = new ArrayList<String>();
        for (DbClip.Item it : items)
            lines.add(icon(it.type) + it.name + (it.how == DbClip.MOVE ? Words.DbClipW.LINE_MOVE : Words.DbClipW.LINE_COPY));
        final List<String> paths = new ArrayList<String>();
        if (disk) {
            paths.addAll(Clip.files());
            paths.addAll(Clip.dirs());
            for (String p : paths) lines.add(Words.DbClipW.DISK_LINE + new java.io.File(p).getName());
        }
        if (lines.isEmpty()) return;
        Ui.list(a, Words.DbClipW.VIEW_TITLE, lines, new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(android.content.DialogInterface d, int which) {
                if (which < items.size()) DbClip.remove(items.get(which));
                else dropDisk(paths.get(which - items.size()));
                refresh();
                show();
            }
        });
    }

    /** הוצאה של קובץ או תיקייה מהלוח של הכונן (שאר מה שבו — נשאר, להעתקה). */
    private static void dropDisk(String path) {
        List<String> f = new ArrayList<String>(Clip.files()), d = new ArrayList<String>(Clip.dirs());
        f.remove(path);
        d.remove(path);
        if (f.isEmpty() && d.isEmpty()) Clip.clear();
        else Clip.set(f, d, true, Clip.warning());
    }

    private static String icon(int type) {
        return type == Store.LIST ? "📁 " : type == Store.SMART ? "✨ " : type == Store.FILE ? "📄 " : "💡 ";
    }
}
