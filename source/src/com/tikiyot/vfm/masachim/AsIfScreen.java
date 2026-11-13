package com.tikiyot.vfm;

// 1.10.0: "תוצאת הדמיה" — רשימה זמנית של מה שקובץ פקודות עם "בכאילו" היה עושה: מה יהיה איפה, מה ייצא לסל.
// נמצאת בזיכרון בלבד ונעלמת ביציאה; שום דבר לא נכתב לכונן. אחראי: ההצגה. החישוב — Batch.asIfRows.

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.LinearLayout;

import java.io.File;
import java.util.List;

public class AsIfScreen extends ListScreen<Batch.AsIfRow> {

    private static final int MENU_EDIT = 1;
    /** התוצאה עוברת למסך בזיכרון — לא בקובץ ("רשימה זמנית, נמחקת ביציאה"). */
    private static List<Batch.AsIfRow> pending;
    private static File pendingFile;
    private File file;

    static void show(Activity a, File f, List<Batch.AsIfRow> rows) {
        pending = rows;
        pendingFile = f;
        a.startActivity(new Intent(a, AsIfScreen.class));
    }

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        LinearLayout root = build(saved, Words.AsIf.SCREEN, Words.AsIf.SCREEN);
        file = pendingFile;
        if (pending != null) rows.addAll(pending);
        pending = null;
        pendingFile = null;
        if (file == null) {                                                   // נפתח מחדש בלי תוצאה (אחרי שהמערכת סגרה) — אין מה להציג
            finish();
            return;
        }
        setContentView(TopRow.wrap(this, root));
    }

    @Override
    protected String key(Batch.AsIfRow r) {
        return null;
    }

    @Override
    protected void bind(View v, Batch.AsIfRow r) {
        Ui.bindIcon(v, r.icon, r.title, r.sub);
    }

    @Override
    protected void open(Batch.AsIfRow r) { }

    @Override
    protected void hold(Batch.AsIfRow r) { }

    @Override
    protected void refresh() {
        setTitle(Words.AsIf.SCREEN + (file != null ? " · " + Text.stem(file.getName()) : ""));
        info.setText(Words.AsIf.DONE);
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void menu(Menu m) {
        m.add(0, MENU_EDIT, 3, Words.AsIf.EDIT);
    }

    @Override
    protected boolean item(int id) {
        if (id != MENU_EDIT || file == null) return false;
        BatchUi.edit(this, file);
        return true;
    }

    /** הרשימה הזמנית לא נשמרת לשום מקום. */
    @Override
    protected void onDestroy() {
        rows.clear();
        super.onDestroy();
    }
}
