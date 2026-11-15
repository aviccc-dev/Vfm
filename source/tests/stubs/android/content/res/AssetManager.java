package android.content.res;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

// תחליף לבדיקות על המחשב (מבנה 71): הנכסים שבאים עם התוכנה — ישר מתיקיית assets של המקור.
public class AssetManager {
    private static File dir() { return new File(System.getProperty("vfm.src"), "assets"); }
    public InputStream open(String name) throws IOException { return new FileInputStream(new File(dir(), name)); }
    public String[] list(String path) throws IOException {
        String[] l = new File(dir(), path).list();
        return l == null ? new String[0] : l;
    }
}
