package com.tikiyot.vfm;

// מ0.10: פקודות הרשימות (פתח / צור / עדכן רשימה, רשימה חכמה; "רשימה חדשה"; הוספה מהכונן ומחיפוש) — על המסד של
// האופן. מ0.25: תמיד במסד (אין עוד צד ישן). active — יש אופן ויש תוכנה (בבדיקות במחשב — לא, ואז "אין מסד").
// מבנה 13: כאן נשארה רק השאלה "האם הפקודות עכשיו של המסד" (active); הביצוע — לפתוח את המסד, להפעיל את DbPaths ולפתוח
// את המסך — ב־DbVerbsUi (במסכים).

final class DbVerbs {

    private DbVerbs() { }

    /** האם הפקודות עכשיו של המסד. */
    static boolean active() {
        // מ0.25 (מחיקת המנוע הישן, שלב 1): בבניית מ הפקודות תמיד של המסד — גם כשמסך המסד עוד לא הוצג
        // (שיתוף מאפליקציה אחרת, קיצור דרך): האופן — זה שנשמר.
        if (Db.currentMode == null && Db.app != null)
            try {
                Db.currentMode = Db.savedMode(Db.app);
            } catch (Throwable t) {
                Log.e("DbVerbs.active", t);
            }
        return Db.currentMode != null && Db.app != null;
    }
}
