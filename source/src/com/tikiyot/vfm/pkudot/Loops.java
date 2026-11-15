package com.tikiyot.vfm;

// מבנה 157 (סידור תיקיית הפקודות): לולאות ("חזור N" … "סוף", %סבב%), "כלול" (השורות של קובץ אחר במקום השורה), והשעון
// של ההרצה (גבול לסבב, "/עד:"). עד כאן ישב בתוך Batch. אחראי: לפרוס את השורות לפני התכנון. אסור לו: לתכנן ולהריץ.

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class Loops {

    private Loops() { }

    // ---------- 1.25.0: לולאה ----------

    /** שורה אחרי פריסת הלולאות: הטקסט (עם %סבב%), המספר בקובץ, והלולאות שסביבה. */
    static final class Ln {
        String text, error;
        int num;
        int[] loops, rounds;
    }

    /** "חזור 6" · "חזור <סבבים>" — לולאה (בלי מספר — הפועל "חזור", חזרה למסך הקודם). */
    static boolean isLoopStart(String line) {
        List<String> tk = Batch.tokens(line);
        if (tk.size() < 2 || !tk.get(0).equals(Batch.LOOP)) return false;
        char c = tk.get(1).isEmpty() ? ' ' : tk.get(1).charAt(0);
        return Character.isDigit(c) || c == '<' || c == '%';
    }

    static boolean isLoopEnd(String line) {
        return line.equals(Batch.LOOP_END);
    }

    /** הגבול לסבב: "30שניות", "30 שניות", "2דקות", "500ms"; -1 — לא מובן. */
    static long limitMs(String v) {
        String t = v.replace(" ", "").toLowerCase(Locale.ROOT);
        int i = 0;
        while (i < t.length() && (Character.isDigit(t.charAt(i)) || t.charAt(i) == '.')) i++;
        if (i == 0) return -1;
        double n;
        try { n = Double.parseDouble(t.substring(0, i)); } catch (NumberFormatException e) { return -1; }
        String u = t.substring(i);
        if (u.isEmpty() || u.startsWith("שנ") || u.equals("ש") || u.equals("s") || u.equals("sec")) return (long) (n * 1000);   // תחביר
        if (u.startsWith("דק") || u.equals("m") || u.equals("min")) return (long) (n * 60000);                                  // תחביר
        if (u.equals("ms") || u.startsWith("אלפ")) return (long) n;                                                               // תחביר
        return -1;
    }

    /** פורס את הלולאות: כל סבב — השורות שבתוכה, עם %סבב% = מספר הסבב (בלולאה פנימית — שלה). */
    /**
     * מ0.15: Batch.CALL "קובץ" (בעברית: כלול) — במקום השורה: הודעה "▸ כלול: …" ואחריה שורות הקובץ (גם Batch.CALL שבתוכו, עד
     * Batch.CALL_DEPTH). קובץ שלא נמצא, מעגל, או עומק — השורה נשארת כמו שהיא ונכשלת בתכנון (עם הסבר).
     */
    static String[] calls(String[] lines, File here, Map<String, String> vars, int depth, java.util.Set<String> stack) {
        List<String> out = new ArrayList<String>();
        for (String raw : lines) {
            List<String> tk = Batch.tokens(Batch.clean(raw));
            if (tk.size() != 2 || !Batch.CALL.equals(Commands.canonical(tk.get(0)))) {
                out.add(raw);
                continue;
            }
            String[] err = {null};
            String p = Batch.path(tk.get(1), here, vars, err);
            File f = p == null ? null : new File(p);
            String key = f == null ? "" : f.getAbsolutePath();
            if (f == null || !f.isFile() || depth >= Batch.CALL_DEPTH || stack.contains(key)) {
                out.add(raw);
                continue;
            }
            try {
                String[] inner = Bidi.strip(Files.readUtf8(f)).replace("\r", "").split("\n");
                stack.add(key);
                String[] sub = calls(inner, f.getParentFile(), vars, depth + 1, stack);
                stack.remove(key);
                List<String> keep = new ArrayList<String>();
                for (String x : sub) {
                    String c = Batch.clean(x);
                    if (c.equals(Batch.CHECK) || c.equals(Batch.AS_IF) || c.equals(Batch.AS_IF2)) continue;   // שורות כותרת — רק של הקובץ הראשי
                    keep.add(x);
                }
                out.add("ECHO " + Words.DbCmd.called(f.getName(), keep.size()));
                out.addAll(keep);
            } catch (Throwable t) {
                Log.e("Batch.calls", t);
                out.add(raw);
            }
        }
        return out.toArray(new String[0]);
    }

    static List<Ln> expand(String[] lines, Batch.Plan plan) {
        List<Ln> out = new ArrayList<Ln>();
        expand(lines, 0, lines.length, new int[0], new int[0], null, plan, out);
        return out;
    }

    private static void expand(String[] lines, int from, int to, int[] loops, int[] rounds, String round, Batch.Plan plan, List<Ln> out) {
        for (int i = from; i < to; i++) {
            if (out.size() > Batch.MAX_LINES) return;
            String line = Batch.clean(lines[i]);
            if (isLoopStart(line)) {
                int depth = 0, end = -1;
                for (int j = i + 1; j < to && end < 0; j++) {
                    String x = Batch.clean(lines[j]);
                    if (isLoopStart(x)) depth++;
                    else if (isLoopEnd(x)) {
                        if (depth == 0) end = j;
                        else depth--;
                    }
                }
                List<String> tk = Batch.tokens(round == null ? line : line.replace(Batch.var(Batch.V_ROUND), round));
                int n = -1;
                long limit = 0;
                String err = null;
                try { n = Integer.parseInt(tk.get(1)); } catch (NumberFormatException e) { err = Words.Loop.NEED_NUMBER; }
                for (int k = 2; k < tk.size() && err == null; k++) {
                    String x = tk.get(k);
                    if (x.equals(Words.Loop.TIMES_WORD)) continue;
                    if (x.startsWith(Batch.SW_LIMIT)) {
                        String v = x.substring(Batch.SW_LIMIT.length());
                        if (k + 1 < tk.size() && !tk.get(k + 1).isEmpty() && !Character.isDigit(tk.get(k + 1).charAt(0)) && !tk.get(k + 1).startsWith("/")
                                && limitMs(v + tk.get(k + 1)) > 0) v += tk.get(++k);                 // "/עד:30 שניות"
                        limit = limitMs(v);
                        if (limit <= 0) err = Words.Loop.BAD_LIMIT;
                    } else err = Words.Loop.USAGE;
                }
                if (err == null && (n < 1 || n > Batch.MAX_ROUNDS)) err = Words.Loop.tooMany(Batch.MAX_ROUNDS);
                if (err == null && end < 0) err = Words.Loop.NO_END;
                if (err != null) {
                    Ln e = new Ln();
                    e.num = i + 1;
                    e.text = line;
                    e.error = err;
                    out.add(e);
                    if (end < 0) continue;
                    i = end;
                    continue;
                }
                int id = plan.loopLimit.size();
                plan.loopLimit.add(limit);
                plan.loopLine.add(i + 1);
                int[] l2 = java.util.Arrays.copyOf(loops, loops.length + 1), r2 = java.util.Arrays.copyOf(rounds, rounds.length + 1);
                l2[loops.length] = id;
                for (int r = 1; r <= n && out.size() <= Batch.MAX_LINES; r++) {
                    r2 = r2.clone();
                    r2[rounds.length] = r;
                    expand(lines, i + 1, end, l2, r2, String.valueOf(r), plan, out);
                }
                i = end;
                continue;
            }
            Ln x = new Ln();
            x.num = i + 1;
            x.text = round == null ? lines[i] : lines[i].replace(Batch.var(Batch.V_ROUND), round);
            if (isLoopEnd(line)) x.error = Words.Loop.END_WITHOUT_START;
            if (loops.length > 0) {
                x.loops = loops;
                x.rounds = rounds;
            }
            out.add(x);
        }
        if (out.size() > Batch.MAX_LINES && (out.isEmpty() || out.get(out.size() - 1).error == null)) {
            Ln e = new Ln();
            e.num = from + 1;
            e.text = "";
            e.error = Words.Loop.tooLong(Batch.MAX_LINES);
            out.add(e);
        }
    }

    /**
     * עצירה לפי זמן: מעבר לסבב חדש בלולאה עם /עד: — אם הסבב הקודם עבר את הגבול, הלולאה נעצרת. מחזיר את מזהה
     * הלולאה שנעצרה כעת (כדי לכתוב פעם אחת), או -1.
     */
    static final class Clock {
        final Map<Integer, Integer> round = new java.util.HashMap<Integer, Integer>();
        final Map<Integer, Long> start = new java.util.HashMap<Integer, Long>();
        final java.util.Set<Integer> stopped = new java.util.HashSet<Integer>();
        final List<String> notes = new ArrayList<String>();

        /** true — השורה מדולגת (לולאה שסביבה נעצרה). */
        boolean skip(Batch.Plan plan, int[] loops, int[] rounds) {
            if (loops == null) return false;
            long now = System.nanoTime();
            for (int k = 0; k < loops.length; k++) {
                int id = loops[k];
                if (stopped.contains(id)) return true;
                Integer cur = round.get(id);
                if (cur == null || cur != rounds[k]) {
                    if (cur != null) {
                        long took = (now - start.get(id)) / 1000000;
                        long lim = plan.loopLimit.get(id);
                        if (lim > 0 && took > lim) {
                            stopped.add(id);
                            notes.add(Words.Loop.stopped(plan.loopLine.get(id), cur, took));
                            return true;
                        }
                    }
                    round.put(id, rounds[k]);
                    start.put(id, now);
                    for (int m = k + 1; m < loops.length; m++) {          // לולאות פנימיות — מתחילות מחדש
                        round.remove(loops[m]);
                        stopped.remove(loops[m]);
                    }
                }
            }
            return false;
        }
    }
}
