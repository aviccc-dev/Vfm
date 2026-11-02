package com.tikiyot.vfm;

// מ0.8: הוספת קבצים מהכונן (מתיקייה או מחיפוש) לרשימה במסד. אחראי: פריט קובץ אחד לכל מסלול, וחברות ברשימה — בעסקה אחת.
// אסור לו: מסכים, קריאת הכונן (המסך נותן מסלולים וגדלים), חישוב חתימות (נעשה ברקע, בנפרד).

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class DbFiles {

    private DbFiles() { }

    /** מה קרה: נוספו לרשימה / כבר היו בה / פריטי קובץ חדשים שנוצרו / מסלולים פסולים (לא מלאים) שדולגו. */
    static final class Added {
        int added, already, created, bad;
    }

    /**
     * מוסיף את הקבצים לסוף הרשימה הידנית list, לפי הסדר; מסלול כפול בקלט — פעם אחת. קובץ שכבר יש לו פריט (גם בלי רשימה)
     * — אותו פריט. פריט חדש: הכונן לפי המסלול, הגודל שנמסר, בלי חתימה. הכול בעסקה אחת: כישלון — שום שינוי.
     * sizes — באותו אורך כמו paths, או null (גודל לא ידוע, -1).
     * @throws Store.Failure אם list אינה רשימה ידנית (רשימה חכמה היא שאילתה — אין לה חברים)
     */
    static Added add(Store s, final long list, List<String> paths, List<Long> sizes) {
        Stores.mustBeList(s.item(list), list);                                // לפני העסקה (עסקאות מקוננות)
        final Added r = new Added();
        final Map<String, Long> in = new LinkedHashMap<String, Long>();
        for (int i = 0; i < paths.size(); i++) {
            String p = paths.get(i) == null ? "" : paths.get(i).trim();
            if (!p.startsWith("/") || p.length() < 2) {
                r.bad++;
                continue;
            }
            if (!in.containsKey(p)) in.put(p, sizes == null || sizes.get(i) == null ? -1L : sizes.get(i));
        }
        if (in.isEmpty()) return r;
        s.atomic(new Store.Work() {
            @Override
            public void run(Store t) {
                for (Map.Entry<String, Long> e : in.entrySet()) {
                    long id = t.findFile(e.getKey());
                    if (id == 0) {
                        id = t.createFile(new Store.FileInfo(e.getKey(), Importer.driveOf(e.getKey()), e.getValue(), 0, null));
                        r.created++;
                    }
                    if (t.addMember(list, id)) r.added++;
                    else r.already++;
                }
            }
        }, true);
        return r;
    }
}
