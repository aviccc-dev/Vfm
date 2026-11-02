package com.tikiyot.vfm;

// עזרים משותפים לכל מימושי Store — כדי שהכללים יהיו זהים בשניהם. אסור לו: לשמור דבר.

import java.security.SecureRandom;

final class Stores {

    private Stores() { }

    /** שם או טקסט תקינים: לא ריקים אחרי קיצוץ רווחים. */
    static String clean(String s, String what) {
        String t = s == null ? "" : s.trim();
        if (t.isEmpty()) throw new Store.Failure("empty " + what);
        return t;
    }

    static void mustBeList(Store.Item it, long id) {
        if (it == null) throw new Store.Failure("no such item " + id);
        if (it.type != Store.LIST) throw new Store.Failure("not a list: " + id);
    }

    /** רשימה ידנית או חכמה (למחיקה). */
    static void mustBeListOrSmart(Store.Item it, long id) {
        if (it == null) throw new Store.Failure("no such item " + id);
        if (it.type != Store.LIST && it.type != Store.SMART) throw new Store.Failure("not a list: " + id);
    }

    /** מה מותר לסל: רעיון וכותרת (פריטים שקיימים רק במסד). קובץ נשאר בכונן; רשימה נמחקת בנפרד. */
    static void mayTrash(Store.Item it, long id) {
        if (it == null) throw new Store.Failure("no such item " + id);
        if (it.type != Store.NOTE && it.type != Store.TITLE) throw new Store.Failure("only notes and titles go to the bin: " + id);
    }

    /** "רשימה:מקום,…" ← זוגות. */
    static java.util.List<long[]> parseFrom(String v) {
        java.util.List<long[]> out = new java.util.ArrayList<long[]>();
        if (v == null) return out;
        for (String p : v.split(",")) {
            int c = p.indexOf(':');
            if (c <= 0) continue;
            try {
                out.add(new long[]{Long.parseLong(p.substring(0, c).trim()), Long.parseLong(p.substring(c + 1).trim())});
            } catch (NumberFormatException ignored) {
            }
        }
        return out;
    }

    static String joinFrom(java.util.List<long[]> l) {
        StringBuilder sb = new StringBuilder();
        for (long[] x : l) sb.append(sb.length() > 0 ? "," : "").append(x[0]).append(':').append(x[1]);
        return sb.toString();
    }

    /** שם הקובץ מהמסלול (החלק האחרון). */
    static String fileName(String path) {
        int i = path.lastIndexOf('/');
        return i >= 0 && i < path.length() - 1 ? path.substring(i + 1) : path;
    }

    static void checkFile(Store.FileInfo f) {
        if (f == null) throw new Store.Failure("no file info");
        clean(f.path, "path");
        clean(f.drive, "drive");
    }

    static String newSystemCode() {
        byte[] b = new byte[4];
        new SecureRandom().nextBytes(b);
        StringBuilder sb = new StringBuilder();
        for (byte x : b) sb.append(String.format("%02x", x & 0xff));
        return sb.toString();
    }
}
