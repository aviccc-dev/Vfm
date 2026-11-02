package com.tikiyot.vfm;

// מ0.3 — הערכת רשימה חכמה: תנאי (טקסט) ← הקבצים שמנוע החיפוש הקיים מוצא באינדקס. אחראי: להפעיל את החיפוש ולהחזיר תוצאה
// קטנה וברורה (ספירה, מקבץ שורות, הערה אם התנאי לא הובן). אסור לו: לשמור דבר, לגעת במסד, או להציג — המסך עושה זאת.
// האינדקס עצמו נשאר מחוץ למסד (קובץ בינארי לכל כונן, כמו בישן).

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

final class Smarts {

    private Smarts() { }

    /** שורה בתוצאה. */
    static final class Row {
        final String name, folder, path;
        final long size, mtime;
        final boolean dir;

        Row(String name, String folder, String path, long size, long mtime, boolean dir) {
            this.name = name;
            this.folder = folder;
            this.path = path;
            this.size = size;
            this.mtime = mtime;
            this.dir = dir;
        }
    }

    /** תוצאה. ready=false: האינדקס עוד לא מוכן (אז count=0 ואין לדעת). note לא ריק: חלק מהתנאי לא הובן. */
    static final class Result {
        final boolean ready;
        final int count;
        final long size;
        final String note;
        /** עד limit שורות, כבר ממוינות. */
        final List<Row> rows;

        Result(boolean ready, int count, long size, String note, List<Row> rows) {
            this.ready = ready;
            this.count = count;
            this.size = size;
            this.note = note == null ? "" : note;
            this.rows = rows;
        }

        static Result notReady() {
            return new Result(false, 0, 0, "", new ArrayList<Row>());
        }
    }

    interface Finder {
        /** מוצא לפי התנאי; sort — שם מיון מ־Sort (ריק: כפי שנמצא); limit — כמה שורות להחזיר (הספירה תמיד מלאה). */
        Result find(String cond, String sort, int limit);
    }

    /** המנוע האמיתי: החיפוש של Library. lib null או לא מוכן — notReady. */
    static Finder over(final Library lib) {
        return new Finder() {
            @Override
            public Result find(String cond, String sort, int limit) {
                if (lib == null || !lib.ready || cond == null || cond.trim().isEmpty()) return Result.notReady();
                Library.Result r = lib.search(cond, false, true);
                List<Library.Hit> hits = new ArrayList<Library.Hit>(r.hits);
                long total = 0;
                for (Library.Hit h : hits) if (h.rec != null) total += h.rec.size;
                sort(hits, sort);
                List<Row> rows = new ArrayList<Row>();
                for (int i = 0; i < hits.size() && i < limit; i++) {
                    Library.Hit h = hits.get(i);
                    rows.add(new Row(h.name(), h.parentPath(), h.path(), h.rec != null ? h.rec.size : 0,
                            h.rec != null ? h.rec.mtime : 0, h.isDir()));
                }
                return new Result(true, hits.size(), total, r.note, rows);
            }
        };
    }

    private static void sort(List<Library.Hit> hits, String sort) {
        if (Sort.NAME.equals(sort)) {
            Collections.sort(hits, new Comparator<Library.Hit>() {
                @Override public int compare(Library.Hit a, Library.Hit b) { return a.name().compareToIgnoreCase(b.name()); }
            });
        } else if (Sort.DATE.equals(sort) || Sort.SIZE.equals(sort)) {
            final boolean date = Sort.DATE.equals(sort);
            Collections.sort(hits, new Comparator<Library.Hit>() {
                @Override
                public int compare(Library.Hit a, Library.Hit b) {
                    long x = a.rec == null ? 0 : date ? a.rec.mtime : a.rec.size, y = b.rec == null ? 0 : date ? b.rec.mtime : b.rec.size;
                    return x < y ? 1 : x > y ? -1 : 0;                         // החדש / הגדול קודם
                }
            });
        }
    }
}
