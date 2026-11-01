package com.tikiyot.lab;

// 1.5 — החלק הטהור של מדידת המסד (בלי אנדרואיד, נבדק במחשב): הנתונים, הסכומים הצפויים והעיצוב.

import java.util.Arrays;
import java.util.Locale;

public final class DbPlan {

    private DbPlan() { }

    public static final int AUTO_ROWS = 300;       // שורה-שורה, כל שורה בעסקה משלה
    public static final int BATCHES = 20;          // עסקאות של מאה שורות
    public static final int BATCH_ROWS = 100;
    public static final int BULK_ROWS = 5000;      // עסקה אחת גדולה
    public static final int LOOKUPS = 2000;

    public static final String CREATE = "CREATE TABLE items(id INTEGER PRIMARY KEY, type INTEGER NOT NULL, "
            + "name TEXT NOT NULL, path TEXT NOT NULL, sig TEXT NOT NULL, changed INTEGER NOT NULL)";
    public static final String CREATE_INDEX = "CREATE INDEX items_sig ON items(sig)";
    public static final String INSERT = "INSERT INTO items(id,type,name,path,sig,changed) VALUES(?,?,?,?,?,?)";

    public static final String[] SHAPES = {"שורה-שורה", "20 עסקאות × 100", "עסקה אחת"};

    public static int rows(int shape) {
        return shape == 0 ? AUTO_ROWS : shape == 1 ? BATCHES * BATCH_ROWS : BULK_ROWS;
    }

    // שורה i (מ־1): דומה לפריט קובץ אמיתי — שם עברי, נתיב ארוך, חתימה של 32 תווים.
    public static Object[] row(int i) {
        String sig = String.format(Locale.US, "%016x%016x", i * 0x9E3779B97F4A7C15L, ~(long) i * 0xC2B2AE3D27D4EB4FL);
        return new Object[]{Long.valueOf(i), Long.valueOf(i % 5), "קובץ-" + i + ".pdf",
                "/storage/emulated/0/מסמכים/תיקייה " + (i % 50) + "/קובץ-" + i + ".pdf", sig, Long.valueOf(1700000000000L + i)};
    }

    public static Object[][] rowsFor(int n) {
        Object[][] out = new Object[n][];
        for (int i = 1; i <= n; i++) out[i - 1] = row(i);
        return out;
    }

    public static long sumIds(int n) {
        return (long) n * (n + 1) / 2;
    }

    public static long sumPathLen(int n) {
        long s = 0;
        for (int i = 1; i <= n; i++) s += ((String) row(i)[3]).length();
        return s;
    }

    public static long median(long[] t) {
        long[] c = t.clone();
        Arrays.sort(c);
        return c[c.length / 2];
    }

    public static String ms(long ns) {
        double m = ns / 1e6;
        return m >= 100 ? String.format(Locale.US, "%.0f", m) : String.format(Locale.US, "%.1f", m);
    }

    public static String perRow(long ns, int rows) {
        return String.format(Locale.US, "%.3f", ns / 1e6 / rows);
    }

    public static String ratio(long ns, long base) {
        if (base <= 0) return "";
        return String.format(Locale.US, "×%.1f", (double) ns / base);
    }
}
