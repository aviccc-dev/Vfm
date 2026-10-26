package android.os;

// תחליף לבדיקות על המחשב: "הכונן" הוא תיקייה זמנית.
public class Environment {
    public static java.io.File getExternalStorageDirectory() {
        return new java.io.File(System.getProperty("vfm.home"));
    }
}
