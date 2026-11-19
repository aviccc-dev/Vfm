package com.tikiyot.vfm;

// מבנה 283 (אפיון החיפוש, 3 — המשתמש, 19.11.2026: "החיפוש בברירת מחדל צריך להיות רק באיזור שבו התוכנה נמצאת… ומתחתיו תהיה
// אפשרות… לקבוע אם רוצים שיהיה בכל המכשיר"): "מחפש ב: <המקום> ▾" מתחת לשורת החיפוש. נגיעה — המקום, כל המכשיר, וברירת המחדל.
// אחראי: מה שרואים ונוגעים. אסור לו: לחפש או לשמור — הוא רק אומר למסך מה נבחר (Host).

import android.app.Activity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;

public final class PlaceChip {

    /** מסך שיש לו "מקום" — החיפוש שנפתח ממנו מחפש שם (בברירת המחדל). {התנאי, השם}, או null — אין מקום. */
    public interface Place {
        String[] searchPlace();
    }

    public interface Host {
        /** wide — בכל המכשיר; always — מעכשיו כך תמיד (ברירת המחדל). */
        void changed(boolean wide, boolean always);
    }

    public final TextView view;
    private final String name;
    private boolean wide, always;

    public PlaceChip(final Activity a, String name, boolean wide, boolean always, final Host host) {
        this.name = name;
        this.wide = wide;
        this.always = always;
        view = Ui.info(a);
        view.setBackgroundColor(Ui.BG_SOFT);
        ScrollRoom.oneLine(view);
        say();
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu pm = new PopupMenu(a, view);
                Menu m = pm.getMenu();
                m.add(0, 1, 0, mark(!PlaceChip.this.wide) + Words.Find.placeHere(PlaceChip.this.name));
                m.add(0, 2, 1, mark(PlaceChip.this.wide) + Words.Find.WHOLE_DEVICE_ITEM);
                m.add(0, 3, 2, (PlaceChip.this.always ? "☑ " : "☐ ") + Words.Find.ALWAYS_WHOLE);
                pm.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem it) {
                        int id = it.getItemId();
                        if (id == 3) {
                            PlaceChip.this.always = !PlaceChip.this.always;
                            PlaceChip.this.wide = PlaceChip.this.always;          // נקבע — חל גם עכשיו
                        } else PlaceChip.this.wide = id == 2;
                        say();
                        host.changed(PlaceChip.this.wide, PlaceChip.this.always);
                        return true;
                    }
                });
                pm.show();
            }
        });
    }

    private static String mark(boolean on) {
        return on ? "✓ " : "   ";
    }

    private void say() {
        view.setText(Words.Find.placeChip(wide ? Words.Find.WHOLE_DEVICE : name));
    }

    /** השם של תיקייה במקום: כונן — שמו (פנימי / כרטיס); אחרת — שם התיקייה. */
    public static String folderLabel(android.content.Context c, String path) {
        for (Vols.Vol v : Vols.list(c))
            if (v.root.getAbsolutePath().equals(path)) return Drive.label(Drive.internalPath(path), Drive.nameOf(path));
        return FolderView.name(path);
    }
}
