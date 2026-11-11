package com.tikiyot.vfm;

// שכבת האחסון של מנוע המסד (מ0.1). אחראי: להגדיר מה אפשר לעשות ברשימות ובפריטים — לא איך נשמר.
// שני מימושים: MemStore (בזיכרון, לבדיקות במחשב) ו־SqlStore (SQLite, במכשיר). אותן בדיקות (StoreContract) על שניהם.
// אסור לו: מסכים, קבצים, הרשאות.

import java.util.List;

public interface Store {

    /** סוגי פריט. בהמשך יתווספו (כותרת, פקודה…); המספרים נשמרים במסד ולא משתנים. */
    int LIST = 1, NOTE = 2, FILE = 3, SMART = 4, TITLE = 5;

    /** מפתחות המאפיינים של רשימה (מ0.2): צלמית, תיאור, עיצוב, "למה". */
    String P_ICON = "icon", P_DESC = "desc", P_DESIGN = "design", P_WHY = "why";

    /** מאפייני רשימה חכמה (מ0.3): התנאי כטקסט (התחביר של מנוע החיפוש), מיון, כיוון, תצוגה. */
    String P_COND = "cond", P_SORT = "sort", P_DIR = "dir", P_VIEW = "view";

    /** מ0.4: רשימה שהיא חבורה (רשימה מוטמעת, "1"). */
    String P_GROUP = "group";

    /** מ0.6: הרשימה שהיא מסך הבית של האופן ("1"). אחת לכל מסד; החברים שלה — הכלים, לפי הסדר. */
    String P_HOME = "home";

    /** מ0.4, פנימי: פריט בסל המחזור (מתי) ומאיפה הוא יצא ("רשימה:מקום,…"). מנוהלים רק בידי trash/restore. */
    String P_TRASHED = "trashed", P_TRASH_FROM = "trash_from";

    /** שגיאת שימוש או אחסון. ההודעה — לאנשי הפיתוח (אנגלית); המסך מציג הודעה משלו. */
    final class Failure extends RuntimeException {
        public Failure(String m) {
            super(m);
        }

        public Failure(String m, Throwable cause) {
            super(m, cause);
        }
    }

    final class Item {
        public final long id;
        public final int type;
        public final String name;
        public final long created, changed;

        public Item(long id, int type, String name, long created, long changed) {
            this.id = id;
            this.type = type;
            this.name = name;
            this.created = created;
            this.changed = changed;
        }
    }

    /**
     * פרטי פריט קובץ (מ0.2): המסלול והכונן בלבד — התוכן נשאר בקובץ עצמו. size/mtime הם מה שהיה כשהחתימה חושבה
     * (חתימה תקפה = הקובץ היום באותו גודל ובאותו תאריך); בלי חתימה — size מה שנרשם ברשימה (-1: לא ידוע), mtime 0.
     */
    final class FileInfo {
        public final String path, drive, sig;
        public final long size, mtime;

        public FileInfo(String path, String drive, long size, long mtime, String sig) {
            this.path = path;
            this.drive = drive;
            this.size = size;
            this.mtime = mtime;
            this.sig = sig;
        }
    }

    /** עבודה שרצה כולה או לא בכלל (Store.atomic). */
    interface Work {
        void run(Store s);
    }

    /** פריט בתוך רשימה: מקומו (מ־0, צפוף) ומתי נוסף. */
    final class Member {
        public final Item item;
        public final int position;
        public final long added;

        public Member(Item item, int position, long added) {
            this.item = item;
            this.position = position;
            this.added = added;
        }
    }

    /** רשימה חדשה. parent = 0: עומדת לעצמה; אחרת היא נכנסת לסוף הרשימה parent. מחזיר את מספר הזיהוי. */
    long createList(String name, long parent);

    /**
     * רשימה חכמה חדשה (מ0.3): שאילתה ולא אוסף — אין לה חברים; התנאי נקבע ב־P_COND. parent = 0: עומדת לעצמה;
     * אחרת היא נכנסת לסוף הרשימה הידנית parent.
     */
    long createSmart(String name, long parent);

    /** כותרת חדשה בסוף רשימה (מ0.4). */
    long addTitle(long list, String text);

    /** רעיון/הערה חדשים בסוף רשימה. */
    long addNote(long list, String text);

    /** פריט קובץ חדש (בלי רשימה). השם — שם הקובץ מהמסלול. מסלול שכבר יש לו פריט — Failure; מחפשים קודם ב־findFile. */
    long createFile(FileInfo f);

    /** פרטי הקובץ, או null אם הפריט אינו קובץ (או אינו קיים). */
    FileInfo file(long item);

    /** מעדכן גודל, תאריך וחתימה (sig יכול להיות null). */
    void updateFile(long item, long size, long mtime, String sig);

    /**
     * מ0.23: הקובץ זז בכונן (העברה, שינוי שם, שחזור מהסל) — אותו פריט, מסלול חדש; השם — שם הקובץ החדש. החברות ברשימות
     * נשארת. מסלול שכבר יש לו פריט אחר — Failure (המאחד — DbFollow).
     */
    void moveFile(long item, String path, String drive);

    /** מספר הפריט של קובץ במסלול הזה, או 0. */
    long findFile(String path);

    /** מקור הפריט, כשהגיע ממערכת אחרת (מ0.2: "tree:L:מסלול הרשימה" מהמנוע הטקסטואלי הישן). 0 — אין. */
    long findByOrigin(String key);

    void setOrigin(long item, String key);

    /** מ0.12: מפתח המקור של פריט, או null. */
    String origin(long item);

    /** מאפיין של פריט, או null. */
    String prop(long item, String key);

    /** קובע מאפיין; ערך ריק או null מוחק אותו. */
    void setProp(long item, String key, String value);

    /**
     * מריץ את w כעסקה אחת: כל מה שעשה נשמר רק אם סיים בלי חריגה ו־keep אמת; אחרת הכול חוזר כמו שהיה (מוני המזהים
     * שבקובץ לא חוזרים — מספר לא יוונפק פעמיים). משמש להעתקה ולתצוגה המקדימה שלה, שהיא אותה ריצה בדיוק, בלי שמירה.
     */
    void atomic(Work w, boolean keep);

    /**
     * מוסיף פריט קיים לסוף רשימה. false אם כבר שם. מ0.4: גם רשימה לתוך רשימה (הפניה — אותה רשימה בכמה מקומות),
     * אבל לא כשנוצר מעגל לפי מספר זיהוי (Failure; בודקים קודם ב־makesCycle). פריט שבסל — Failure.
     */
    boolean addMember(long list, long item);

    /** האם inner נמצא בתוך outer, בכל עומק (צעד אחד לפחות). */
    boolean contains(long outer, long inner);

    /** האם הוספת item לרשימה list תיצור מעגל: אותו פריט, או ש־list כבר בתוך item. */
    boolean makesCycle(long list, long item);

    /** מעביר רעיון או כותרת לסל המחזור: יוצא מכל הרשימות, ונזכר מאיפה. אחר — Failure. */
    void trash(long item);

    /** מחזיר מהסל לרשימות שהיה בהן (שעדיין קיימות), למקום שהיה בו; אם אין — ל"לא משויכים". */
    void restore(long item);

    /** מוחק לצמיתות פריט שבסל. המספר לא יחזור. */
    void purge(long item);

    /** מה שבסל, לפי מספר זיהוי. */
    List<Item> trashed();

    /** מסיר פריט מרשימה (הפריט עצמו נשאר). false אם לא היה שם. המקומות נסגרים. */
    boolean removeMember(long list, long item);

    /** מזיז למקום (מ־0); מעבר לקצה — לקצה. */
    void move(long list, long item, int position);

    void rename(long item, String name);

    /** מוחק רשימה (או רשימה חכמה) ואת החברויות שלה; הפריטים שהיו בה נשארים (בלי רשימה). המספר לא יחזור. */
    void deleteList(long list);

    /** null אם אין. */
    Item item(long id);

    /** כל הרשימות, לפי מספר זיהוי. */
    List<Item> lists();

    /** כל הרשימות החכמות, לפי מספר זיהוי. */
    List<Item> smarts();

    /** "לא משויכים" (מ0.3): קבצים, רעיונות וכותרות שאינם באף רשימה ואינם בסל, לפי מספר זיהוי. רשימות ורשימות חכמות לא נספרות. */
    List<Item> unassigned();

    /** חברי הרשימה לפי המקום. */
    List<Member> members(long list);

    int count(long list);

    /** הרשימות שהפריט נמצא בהן. */
    List<Long> listsOf(long item);

    /** המספר הגבוה ביותר שייתכן שהונפק (0 אם עוד לא); אחרי שחזור — לא פחות מהתקרה בקובץ המונה. */
    long lastId();

    /** קוד קצר אקראי של המערכת הזו — ישמש לסימון מקור כשנתונים עוברים בין מערכות. */
    String systemCode();

    /** בדיקת תקינות. null = תקין; אחרת תיאור. */
    String check();

    void close();
}
