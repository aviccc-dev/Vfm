import com.tikiyot.lab.DbPlan;
import java.util.HashSet;
import java.util.Set;

// בדיקות החלק הטהור של מדידת המסד: שורות ייחודיות ויציבות, סכומים צפויים, עיצוב.
public class DbPlanTest {
    static int ok, bad;

    static void check(String n, boolean c) {
        if (c) ok++;
        else { bad++; System.out.println("  ✗ " + n); }
    }

    public static void main(String[] a) {
        check("מספר שורות לפי צורה", DbPlan.rows(0) == 300 && DbPlan.rows(1) == 2000 && DbPlan.rows(2) == 5000);
        Object[][] r = DbPlan.rowsFor(5000);
        Set<String> sigs = new HashSet<String>();
        long ids = 0, len = 0;
        boolean shape = true;
        for (Object[] x : r) {
            sigs.add((String) x[4]);
            ids += (Long) x[0];
            len += ((String) x[3]).length();
            shape &= x.length == 6 && ((String) x[4]).length() == 32;
        }
        check("חתימות ייחודיות (5000)", sigs.size() == 5000);
        check("כל שורה: 6 שדות וחתימה של 32 תווים", shape);
        check("סכום המספרים", ids == DbPlan.sumIds(5000));
        check("סכום אורכי הנתיבים", len == DbPlan.sumPathLen(5000));
        check("שורה יציבה בין קריאות", DbPlan.row(77)[4].equals(DbPlan.row(77)[4]) && !DbPlan.row(77)[4].equals(DbPlan.row(78)[4]));
        check("חציון", DbPlan.median(new long[]{9, 1, 5}) == 5 && DbPlan.median(new long[]{4, 2, 8, 6}) == 6);
        check("עיצוב מילישניות", DbPlan.ms(1_234_000_000L).equals("1234") && DbPlan.ms(12_340_000L).equals("12.3"));
        check("יחס", DbPlan.ratio(300, 100).equals("×3.0") && DbPlan.ratio(5, 0).equals(""));
        check("לשורה", DbPlan.perRow(300_000_000L, 300).equals("1.000"));
        System.out.println("בדיקות מסד: " + ok + " עברו · " + bad + " נכשלו");
        System.exit(bad == 0 ? 0 : 1);
    }
}
