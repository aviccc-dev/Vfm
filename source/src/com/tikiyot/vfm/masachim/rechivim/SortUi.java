package com.tikiyot.vfm;

// תפריט המיון — החלון (מבנה 17, 13.11.2026). עד כאן ישב בתוך Sort (החומר): המיון עצמו "הכיר" חלון של המסך.
// אותן שורות; מה שהיה של המיון עצמו נקרא עכשיו דרך s. אחראי: להציג את האפשרויות ולמסור את הבחירה ל־Sort.choose.

import android.app.Activity;
import android.content.DialogInterface;

import java.util.ArrayList;
import java.util.List;

final class SortUi {

    private SortUi() { }

    /** תפריט המיון. אחרי בחירה — נשמר, ו-changed רץ. */
    static void menu(Activity a, final Sort s, final Runnable changed) {
        final String[] opts = s.options();
        List<String> all = new ArrayList<String>();
        for (String o : opts) all.add((o.equals(s.by) ? "✓ " : "") + o);
        all.add("↕ " + (s.desc ? Words.SortW.NORMAL_DIR : Words.SortW.REVERSE_DIR));
        if (s.groups) all.add((s.group ? "✓ " : "") + Words.SortW.GROUP_BY_MATCH);
        Ui.options(a, Words.SortW.SORT_NOW + s.by + (s.desc ? " ↑" : " ↓"), all.toArray(new String[0]),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        s.choose(w, opts.length);
                        changed.run();
                    }
                });
    }
}
