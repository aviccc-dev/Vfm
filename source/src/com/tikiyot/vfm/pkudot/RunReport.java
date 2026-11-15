package com.tikiyot.vfm;

// מבנה 156 (סידור תיקיית הפקודות): הדוח של הרצה — השורה הקצרה ליומן, כתיבת הדוח לקובץ ("דוח"), ו"ודא שגיאה" (מה שנבדק
// הוא השורה שאחריו). עד כאן ישב בתוך Batch. אחראי: לסכם ולכתוב. אסור לו: להריץ.

import java.io.File;
import java.util.List;

final class RunReport {

    private RunReport() { }

    /**
     * מ0.43: דוח של הרצה — ליומן התקלות בקיצור: כמה שורות, הסיכום של "ודא", וכל שורה עם ❌ או ⚠ (עד 30). עד כאן — כל הדוח, ובדיקה
     * אחת (300 שורות) דחקה את התקלות מהיומן. הדוח המלא — בשורה "דוח" ובמסך ההרצה.
     */
    static String logLine(List<String> report) {
        if (report.isEmpty()) return Words.Cmd.NO_CHANGE;
        if (report.size() <= 12) return report.toString();
        StringBuilder sb = new StringBuilder(Words.DbCmd.logLines(report.size()));
        int shown = 0;
        for (String r : report) {
            boolean bad = r.contains("❌") || r.contains("⚠");
            boolean sum = r.contains(Words.DbCmd.VERIFY_SUM_MARK);
            if ((bad && shown < 30) || sum) {
                sb.append(" · ").append(r);
                if (bad) shown++;
            }
        }
        return sb.toString();
    }

    /** מ0.37: "דוח "<קובץ>"" — ההודעות (ECHO) והיומן נכתבים לקובץ; כישלון בכתיבה — שורה ביומן, לא עוצר. */
    static void write(Batch.Plan plan, List<String> report) {
        if (plan.reportFile == null) return;
        try {
            StringBuilder sb = new StringBuilder();
            for (String e : plan.echoes) sb.append(e).append('\n');
            if (!plan.echoes.isEmpty()) sb.append('\n');
            for (String r : report) sb.append(r).append('\n');
            File f = new File(plan.reportFile);
            File dir = f.getParentFile();
            if (dir != null && !dir.isDirectory()) dir.mkdirs();
            Files.writeAtomic(f, sb.toString());
            report.add(Words.DbCmd.reportWritten(Bidi.path(plan.reportFile)));
        } catch (Throwable t) {
            Log.e("Batch.report", t);
            report.add(Words.DbCmd.reportFailed(String.valueOf(t.getMessage())));
        }
    }

    /** מ0.37: שורת "ודא שגיאה" — סימון בלבד; מה שנבדק היא השורה שאחריה. */
    static final String EXPECT = "EXPECT";

    /** מ0.37: אחרי התכנון — כל "ודא שגיאה" נצמד לשורה שאחריו: נכשלה כבר בתכנון — עברה (ואינה שגיאה); אחרת — תיבדק בהרצה. */
    static void expectErrors(Batch.Plan plan) {
        for (int i = 0; i < plan.steps.size(); i++) {
            Batch.Step m = plan.steps.get(i);
            if (!EXPECT.equals(m.cmd) || m.error != null) continue;
            Batch.Step next = i + 1 < plan.steps.size() ? plan.steps.get(i + 1) : null;
            if (next == null || EXPECT.equals(next.cmd)) {
                m.noop = false;
                m.error = Words.DbCmd.EXPECT_NOTHING;
                plan.errors++;
            } else if (next.error != null) {
                next.describe = Words.DbCmd.expectOk(next.error);
                next.error = null;
                next.noop = true;
                next.op = null;
                next.verified = Boolean.TRUE;
                plan.errors--;
            } else next.expectFail = true;
        }
    }
}
