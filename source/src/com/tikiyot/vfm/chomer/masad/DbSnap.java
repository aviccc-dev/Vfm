package com.tikiyot.vfm;

// מבנה 87–88 (המשתמש, 15.11.2026: "הרשימות, ובפרט הידנית שנראית פשוטה — היא ממש לא כזו פשוטה, ובהסתעפויות והעומקים שהיא עשויה
// להכיל יש הרבה מצבים ומצבי מצבים… לאט לאט ובתשומת לב מרובה לכל המצבים האפשריים" · "כל הבדיקות יכולות לרוץ בפקודות, ואולי
// יצטרך עוד פקודות כדי להשוות ולבדוק"): תמונת מצב של רשימה — כטקסט, שורה לכל פריט — כדי לבדוק ששתי רשימות זהות עד הסוף.
//
// מה נכנס לתמונה (כל מה שהמשתמש רואה ברשימה, ומה שקובע איך היא מתנהגת):
//   · הסדר של הפריטים, והעומק (כל רמה — שני רווחים);
//   · הסוג של כל פריט: רשימה (L), קובץ (F), חכמה (S), רעיון (N), כותרת (T) — והשם או הטקסט;
//   · המאפיינים: צלמית, תיאור, עיצוב, "למה", חבורה, ולחכמה — תנאי, מיון, כיוון, תצוגה;
//   · לקובץ — המסלול והגודל שנרשם;
//   · **אותו פריט בשני מקומות**: כל פריט מקבל מספר לפי הפעם הראשונה שפוגשים אותו (L1, L2… F1…); בפעם השנייה נכתב "= L1".
//     כך רשימה משותפת (קישור) שונה משתי רשימות נפרדות עם אותו תוכן, וקובץ שהוא אותו פריט בשתי רשימות שונה משני רישומים של
//     אותו מסלול.
// מה לא נכנס: המספר של הפריט במסד, מתי נוצר ומתי שונה, החתימה של קובץ (מחושבת ברקע), ה"מקור" של פריטי המערכת, והסל.
// השם של הרשימה עצמה אינו בתמונה (משווים את מה שבתוכה ואת המאפיינים שלה) — כך "א" ו"ב" יכולות להיות זהות.
// אחראי: הטקסט בלבד. אסור לו: לכתוב למסד.

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class DbSnap {

    /** המאפיינים שבתמונה, בסדר קבוע. */
    static final String[] PROPS = {Store.P_ICON, Store.P_DESC, Store.P_DESIGN, Store.P_WHY, Store.P_GROUP,
            Store.P_COND, Store.P_SORT, Store.P_DIR, Store.P_VIEW};

    private DbSnap() { }

    /** התמונה של הרשימה root: השורה הראשונה — המאפיינים שלה; אחריה — מה שבתוכה, עד הסוף. */
    static List<String> of(Store s, long root) {
        List<String> out = new ArrayList<String>();
        Map<Long, String> seen = new HashMap<Long, String>();
        int[] n = new int[6];
        out.add("@" + props(s, root));
        seen.put(root, "L0");
        walk(s, root, 1, out, seen, n);
        return out;
    }

    private static void walk(Store s, long list, int depth, List<String> out, Map<Long, String> seen, int[] n) {
        StringBuilder pad = new StringBuilder();
        for (int i = 0; i < depth; i++) pad.append("  ");
        for (Store.Member m : s.members(list)) {
            Store.Item it = m.item;
            String had = seen.get(it.id);
            if (had != null) {                                                   // אותו פריט שוב — לא חוזרים על מה שבתוכו
                out.add(pad + "= " + had);
                continue;
            }
            String kind = it.type == Store.LIST ? "L" : it.type == Store.FILE ? "F" : it.type == Store.SMART ? "S"
                    : it.type == Store.NOTE ? "N" : it.type == Store.TITLE ? "T" : "?";
            String label = kind + (++n["LFSNT?".indexOf(kind)]);
            seen.put(it.id, label);
            if (it.type == Store.FILE) {
                Store.FileInfo fi = s.file(it.id);
                out.add(pad + label + " " + (fi == null ? "?" : fi.path + " | " + (fi.size >= 0 ? String.valueOf(fi.size) : "?")));
            } else if (it.type == Store.NOTE || it.type == Store.TITLE) {
                out.add(pad + label + " \"" + DbExport.one(it.name) + "\"");
            } else {
                out.add(pad + label + " \"" + DbExport.one(it.name) + "\"" + props(s, it.id));
                if (it.type == Store.LIST) walk(s, it.id, depth + 1, out, seen, n);
            }
        }
    }

    /** המאפיינים שיש להם ערך: " {icon=📁; desc=…}", או "". */
    private static String props(Store s, long id) {
        StringBuilder sb = new StringBuilder();
        for (String k : PROPS) {
            String v = s.prop(id, k);
            if (v == null || v.isEmpty()) continue;
            sb.append(sb.length() == 0 ? " {" : "; ").append(k).append('=').append(DbExport.one(v));
        }
        return sb.length() == 0 ? "" : sb + "}";
    }

    /** null — שתי התמונות זהות. אחרת — המקום הראשון שבו הן שונות, כך שמי שקורא יודע איפה לחפש. */
    static String diff(List<String> a, List<String> b) {
        int n = Math.min(a.size(), b.size());
        for (int i = 0; i < n; i++)
            if (!a.get(i).equals(b.get(i))) return Words.DbCmd.snapDiff(i + 1, a.get(i).trim(), b.get(i).trim(), a.size(), b.size());
        if (a.size() == b.size()) return null;
        return Words.DbCmd.snapDiff(n + 1, n < a.size() ? a.get(n).trim() : null, n < b.size() ? b.get(n).trim() : null, a.size(), b.size());
    }
}
