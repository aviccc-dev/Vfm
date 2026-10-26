package com.tikiyot.vfm;

import java.io.File;
import java.util.*;

public final class Probe2 {
    public static void main(String[] a) throws Exception {
        // רשימה חכמה בלי מילים (רק תנאי) על 1,000 קבצים: כמה מוצגים, והאם הגדול ביותר ביניהם
        Index ix = new Index("ס", "ס", "/storage/9999-0000");
        Index.Dir d = new Index.Dir();
        d.parent = -1; d.name = "ספרים"; d.norm = Text.norm(d.name);
        ix.dirs.add(d);
        long biggest = 0; String bigName = null;
        for (int i = 0; i < 1000; i++) {
            Index.Rec r = new Index.Rec();
            r.dir = 0;
            r.name = "ספר " + i + ".pdf";
            r.norm = Text.norm(r.name);
            r.loose = Text.loose(r.norm);
            r.size = 1000L + (i * 7919L) % 1000003L;
            if (r.size > biggest) { biggest = r.size; bigName = r.name; }
            ix.files.add(r);
        }
        Library lib = Library.forTests(new ArrayList<Index>(Collections.singletonList(ix)));
        Library.Result res = lib.search("סוג:pdf", false, false);
        boolean has = false;
        for (Library.Hit h : res.hits) if (h.name().equals(bigName)) has = true;
        System.out.println("רשימה חכמה \"סוג:pdf\" על 1,000 קבצים: נמצאו " + res.total + " · מוצגים " + res.hits.size()
                + " · הקובץ הגדול ביותר בין המוצגים=" + has + " · הערה: \"" + res.note + "\"");
    }
}
