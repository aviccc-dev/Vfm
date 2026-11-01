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

    static void noListInList(Store.Item item) {
        if (item.type == Store.LIST) throw new Store.Failure("list in list: not yet");
    }

    static String newSystemCode() {
        byte[] b = new byte[4];
        new SecureRandom().nextBytes(b);
        StringBuilder sb = new StringBuilder();
        for (byte x : b) sb.append(String.format("%02x", x & 0xff));
        return sb.toString();
    }
}
