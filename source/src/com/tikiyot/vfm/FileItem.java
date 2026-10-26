package com.tikiyot.vfm;

// פריט — קובץ אחד כפי שכל הכלים רואים אותו (80, מודל אחד של פריט).
// אחראי: שם, מקום, גודל, תאריך, ומצב — מחושב כאן בלבד, וכך הסימונים זהים בכל הכלים.
// אסור לו: לשנות קבצים או רשימות.

import java.io.File;

public final class FileItem {

    /** מצבי פריט. הצלמית והניסוח של כל מצב כתובים כאן בלבד. */
    public enum State {
        /** הקובץ במקומו. */
        OK("", "", "", ""),
        /** ⚠ לא נמצא במסלול שלו. */
        MISSING("⚠", Words.Item.MISSING_SHORT, Words.FILE_NOT_AT_PATH, Words.Item.MISSING_MANY),
        /** נמחק בתוכנה ונמצא בסל המחזור (58: "השאר באפור"). */
        DELETED("🗑", Words.Item.DELETED, Words.Item.DELETED, Words.Item.DELETED_MANY),
        /** הכונן שלו אינו זמין: כרטיס שהוצא או מדיה פגומה (66). אינו "חסר". */
        OFFLINE("💾", Words.Item.OFFLINE, Words.Item.OFFLINE, Words.Item.OFFLINE_MANY),
        /** הקובץ עצמו נמצא בתוך ".נמחקו" (55). */
        IN_TRASH("🗑", Words.Item.IN_TRASH, Words.Item.IN_TRASH, Words.Item.IN_TRASH),
        /** שמו מתחיל בנקודה, או שהוא בתיקייה מוסתרת (63). מוצג רק כשמבקשים. */
        HIDDEN("", Words.Item.HIDDEN, Words.Item.HIDDEN, Words.Item.HIDDEN_MANY),
        /** ברשימה חכמה: אחרי פעולה כבר אינו עונה להגדרות — נשאר באפור עד רענון (146). */
        OUT("", Words.Item.OUT, Words.Item.OUT, Words.Item.OUT_MANY);

        public final String icon;
        /** הערה קצרה בשורת הקובץ. */
        public final String note;
        /** השורה האפורה שמתחת לשם. */
        public final String line;
        /** צורת הרבים לשורת הסיכום ("3 חסרים"). */
        public final String many;

        State(String icon, String note, String line, String many) {
            this.icon = icon;
            this.note = note;
            this.line = line;
            this.many = many;
        }

        /** הקובץ נמצא בכונן (גם אם מוסתר או בסל). */
        public boolean present() {
            return this == OK || this == HIDDEN || this == IN_TRASH;
        }

        /** לשורת הסיכום: "⚠ 3 חסרים" · "2 במדיה שהוצאה או פגומה". */
        public String count(int n) {
            return (icon.isEmpty() || this == OFFLINE ? "" : icon + " ") + n + " " + many;
        }
    }

    public final String path;
    public final String name;
    public long size = -1, mtime;
    public State state = State.OK;

    public FileItem(String path) {
        this.path = path;
        int i = path.lastIndexOf('/');
        this.name = i >= 0 && i < path.length() - 1 ? path.substring(i + 1) : path;
    }

    // ---------- יצירה ----------

    /** מתוך האינדקס: הקובץ קיים, והמצב לפי הסימונים שבאינדקס. */
    public static FileItem of(Index ix, Index.Rec r) {
        FileItem it = new FileItem(ix.path(r));
        it.size = r.size;
        it.mtime = r.mtime;
        it.state = ix.trash(r) ? State.IN_TRASH : ix.hidden(r) ? State.HIDDEN : State.OK;
        return it;
    }

    /**
     * לפי מסלול (למשל פריט ברשימה ידנית). useIndex=false כשהאינדקס עוד נבנה —
     * אז בדיקה אחת בכונן. knownSize הוא הגודל השמור ברשימה, או -1.
     */
    public static FileItem of(Library lib, String path, long knownSize, boolean useIndex) {
        FileItem it = new FileItem(path);
        it.size = knownSize;
        Library.Hit h = useIndex && lib != null ? lib.find(path) : null;
        if (h != null && !h.rec.gone) {
            it.mtime = h.rec.mtime;
            if (it.size < 0) it.size = h.rec.size;
            it.state = h.ix.trash(h.rec) ? State.IN_TRASH : h.ix.hidden(h.rec) ? State.HIDDEN : State.OK;
            return it;
        }
        File f = new File(path);
        if (f.isFile()) {
            it.mtime = f.lastModified();
            if (it.size < 0) it.size = f.length();
            return it;
        }
        it.state = absent(lib, path);
        return it;
    }

    /**
     * 146: שורה ברשימה חכמה שאחרי פעולה כבר אינה עונה — למה. `now` — איפה הקובץ עכשיו (Physical.whereNow).
     * נמחק לסל — "נמחק · בסל המחזור"; לא נמצא — חסר או מדיה; קיים — "לא עונה עוד לסינון".
     */
    public static State dropped(Library lib, String now, boolean useIndex) {
        State s = of(lib, now, -1, useIndex).state;
        if (s == State.IN_TRASH) return State.DELETED;
        if (s == State.OK || s == State.HIDDEN) return State.OUT;
        return s;
    }

    /** למה קובץ שאינו במקומו אינו שם: כונן שאינו זמין (66), בסל (58), או חסר. */
    public static State absent(Library lib, String path) {
        if (Files.driveMissing(path)) return State.OFFLINE;
        if (lib != null && Trash.isInTrash(lib, path)) return State.DELETED;
        return State.MISSING;
    }
}
