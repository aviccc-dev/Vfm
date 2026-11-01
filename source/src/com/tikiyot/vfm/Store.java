package com.tikiyot.vfm;

// שכבת האחסון של מנוע המסד (מ0.1). אחראי: להגדיר מה אפשר לעשות ברשימות ובפריטים — לא איך נשמר.
// שני מימושים: MemStore (בזיכרון, לבדיקות במחשב) ו־SqlStore (SQLite, במכשיר). אותן בדיקות (StoreContract) על שניהם.
// אסור לו: מסכים, קבצים, הרשאות.

import java.util.List;

public interface Store {

    /** סוגי פריט. בהמשך יתווספו (קובץ, כותרת, פקודה…); המספרים נשמרים במסד ולא משתנים. */
    int LIST = 1, NOTE = 2;

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

    /** רעיון/הערה חדשים בסוף רשימה. */
    long addNote(long list, String text);

    /** מוסיף פריט קיים לסוף רשימה. false אם כבר שם. בגרסה זו לא מוסיפים רשימה לרשימה אחרת (בדיקת מעגלים — מ0.4). */
    boolean addMember(long list, long item);

    /** מסיר פריט מרשימה (הפריט עצמו נשאר). false אם לא היה שם. המקומות נסגרים. */
    boolean removeMember(long list, long item);

    /** מזיז למקום (מ־0); מעבר לקצה — לקצה. */
    void move(long list, long item, int position);

    void rename(long item, String name);

    /** מוחק רשימה ואת החברויות שלה; הפריטים שהיו בה נשארים (בלי רשימה). המספר לא יחזור. */
    void deleteList(long list);

    /** null אם אין. */
    Item item(long id);

    /** כל הרשימות, לפי מספר זיהוי. */
    List<Item> lists();

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
