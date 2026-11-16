package com.tikiyot.vfm;

// מבנה 179 (סידור קומת החומר): הנוסחים של פעולות הכונן — על מה מאשרים (חלון האישור), ומה נאמר בסוף. עד כאן ב־Physical.
// אחראי: לבנות את המשפט. אסור לו: לגעת בכונן, במסד או במסכים.

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class DiskSay {

    private DiskSay() { }

    static String what(List<String> names) {
        return names.size() == 1 ? "\"" + names.get(0) + "\"" : names.size() + Words.Disk.FILES_SUFFIX;
    }

    /**
     * ההודעה בסוף: "נמחק קובץ אחד" / "נמחקו 3 קבצים · הוסרו 2 פריטים ברשימות".
     * עם תיקיות (82): "נמחקה תיקייה אחת (214 קבצים)" / "הועברו: 2 קבצים · תיקייה אחת (30 קבצים)".
     */
    static String message(String verb, String listsVerb, Physical.Result r) {
        int dirs = r.dirs, n = r.done.size() - dirs;
        String one = verb.substring(0, verb.length() - 1);        // נמחקו → נמחק
        StringBuilder sb = new StringBuilder();
        String inner = r.inner > 0 ? " (" + Words.files(r.inner) + ")" : "";
        String d = Words.folders(dirs);
        if (dirs == 0) sb.append(n == 1 ? one + Words.Disk.ONE_FILE_SUFFIX : verb + " " + n + Words.Disk.FILES_SUFFIX);
        else if (n == 0) sb.append(dirs == 1 ? one + Words.Disk.FEM_SUFFIX : verb + " ").append(d).append(inner);
        else sb.append(verb).append(": ").append(Words.files(n)).append(" · ").append(d).append(inner);
        if (r.lists > 0) sb.append(" · ").append(listsVerb).append(" ").append(Words.items(r.lists)).append(Words.Disk.IN_LISTS_SUFFIX);
        if (r.failed > 0) sb.append(" · ").append(r.failed).append(Words.Disk.NOT_SUFFIX).append(r.note);
        if (r.cancelled) sb.append(Words.Disk.STOPPED_MIDWAY);
        return sb.toString();
    }

    /** "התיקייה ו־214 הקבצים שבה" / "2 קבצים ותיקייה אחת" — לחלונות האישור. */
    static String whatAll(List<String> files, List<String> dirs) {
        List<String> all = new ArrayList<String>(names(files));
        all.addAll(names(dirs));
        if (all.size() == 1) return "\"" + all.get(0) + "\"";
        String d = Words.folders(dirs.size());
        if (files.isEmpty()) return d;
        if (dirs.isEmpty()) return files.size() + Words.Disk.FILES_SUFFIX;
        return Words.files(files.size()) + Words.Disk.AND + d;
    }

    static List<String> names(List<String> paths) {
        List<String> out = new ArrayList<String>();
        for (String p : paths) out.add(new File(p).getName());
        return out;
    }

    /**
     * 141: השמות שבחלון האישור — עד max, ואחריהם "ועוד N". תיקיות קודם (📁). כך רואים על מה מאשרים,
     * כמו בתצוגה המקדימה של קובצי הפקודות.
     */
    static String namesOf(List<String> paths, List<String> dirs, int max) {
        StringBuilder sb = new StringBuilder();
        int shown = 0, total = paths.size() + dirs.size();
        for (String d : dirs) {
            if (shown == max) break;
            sb.append("\n📁 ").append(new File(d).getName());
            shown++;
        }
        for (String p : paths) {
            if (shown == max) break;
            sb.append("\n· ").append(new File(p).getName());
            shown++;
        }
        if (total > shown) sb.append("\n· ").append(Words.andMore(total - shown));
        return sb.toString();
    }
}
