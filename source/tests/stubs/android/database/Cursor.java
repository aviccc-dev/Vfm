package android.database;

// תחליף לבדיקות על המחשב: רק מה ש־SqlStore צריך. (במכשיר — הממשק האמיתי של אנדרואיד.)
public interface Cursor {
    boolean moveToFirst();

    boolean moveToNext();

    String getString(int column);

    long getLong(int column);

    int getInt(int column);

    boolean isNull(int column);

    void close();
}
