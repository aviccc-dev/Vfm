package android.database;

import android.database.sqlite.SQLiteDatabase;

// תחליף לבדיקות על המחשב (מ0.5): מה שאנדרואיד קורא כשהוא מזהה מסד פגום. ברירת המחדל של אנדרואיד מוחקת את הקובץ.
public interface DatabaseErrorHandler {
    void onCorruption(SQLiteDatabase dbObj);
}
