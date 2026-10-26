import com.tikiyot.lab.Scan;
import java.io.File;
import java.io.FileOutputStream;
import java.util.*;

// בדיקות המנוע: כל צירוף של השיפורים נותן בדיוק את אותן תיקיות ואותם קבצים כמו הבסיס.
public class ScanTest {
    static int ok, bad;

    static void check(String n, boolean c) {
        if (c) ok++;
        else { bad++; System.out.println("  ✗ " + n); }
    }

    static void mk(File d, String name) throws Exception {
        d.mkdirs();
        FileOutputStream o = new FileOutputStream(new File(d, name));
        o.write(1);
        o.close();
    }

    public static void main(String[] a) throws Exception {
        File root = new File(System.getProperty("java.io.tmpdir"), "מעבדה-" + System.nanoTime());
        for (int i = 0; i < 8; i++) for (int j = 0; j < 4; j++) mk(new File(root, "א" + i + "/ב" + j), "ק.pdf");
        File big = new File(root, "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Images");
        for (int i = 0; i < 37; i++) mk(big, "תמונה " + i + ".jpg");
        mk(new File(root, "Android/data/אפליקציה"), "סוד.bin");
        new File(root, "ריקה/פנימית").mkdirs();
        String r = root.getAbsolutePath();
        Scan.CHUNK = 5;
        Scan.Opts base = new Scan.Opts();
        Scan.Result b = Scan.scan(r, base, null);
        check("בסיס: 69 קבצים (בלי Android/data) — " + b.files, b.files == 69);
        check("בסיס: התיקייה הגדולה — WhatsApp Images, 37", b.biggest.endsWith("WhatsApp Images") && b.biggestFiles == 37);
        Set<String> want = new HashSet<String>(b.dirs);
        String[] names = {"fast", "seed", "split", "push", "names"};
        for (int mask = 1; mask < 32; mask++)
            for (int readers : new int[]{1, 4, 16}) {
                Scan.Opts o = new Scan.Opts();
                o.readers = readers;
                o.fast = (mask & 1) != 0; o.seed = (mask & 2) != 0; o.split = (mask & 4) != 0;
                o.push = (mask & 8) != 0; o.names = (mask & 16) != 0;
                Scan.Result x = Scan.scan(r, o, b);
                StringBuilder nm = new StringBuilder();
                for (int k = 0; k < 5; k++) if ((mask & (1 << k)) != 0) nm.append(names[k]).append(' ');
                check(nm + "· " + readers + " קוראים — אותם קבצים ותיקיות (" + x.files + ")",
                        x.files == b.files && new HashSet<String>(x.dirs).equals(want) && x.dirs.size() == b.dirs.size()
                                && x.biggestFiles == 37);
            }
        File gone = new File(root, "א7/ב3");
        new File(gone, "ק.pdf").delete();
        gone.delete();
        Scan.Opts s = new Scan.Opts();
        s.seed = true;
        Scan.Result after = Scan.scan(r, s, b);
        check("מפה ישנה עם תיקייה שנמחקה — בלי שגיאה, בלי התיקייה", after.files == 68 && !after.dirs.contains(gone.getAbsolutePath()));
        check("רענון — התיקייה שמעל השתנתה", Scan.refresh(b, 4).changed >= 1);
        Scan.Result fresh = Scan.scan(r, new Scan.Opts(), null);
        check("רענון אחרי סריקה — 0 השתנו", Scan.refresh(fresh, 4).changed == 0);
        System.out.println("בדיקות: " + ok + " עברו · " + bad + " נכשלו");
        System.exit(bad == 0 ? 0 : 1);
    }
}
