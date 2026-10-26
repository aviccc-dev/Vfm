package com.tikiyot.vfm;

// 1.9.0: קריאת תיקייה אחת ישר מהכונן — בלי אינדקס. משמש את מנהל הקבצים הישיר (DiskScreen) ואת התיקייה
// החיה ברשימה. אחראי: מה יש בתיקייה עכשיו, ומה אנדרואיד חוסם. אסור לו: לשנות דבר בכונן.

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

final class DiskList {

    private DiskList() { }

    /** מה שנקרא מתיקייה אחת. */
    static final class Listing {
        final List<File> dirs = new ArrayList<File>();
        final List<File> files = new ArrayList<File>();
        /** כמה פריטים מוסתרים (שם שמתחיל בנקודה) לא הוצגו. */
        int hidden;
        /** אנדרואיד חוסם את התיקייה (Android/data, Android/obb) — אינה בהכרח ריקה. */
        boolean blocked;
        /** התיקייה קיימת ולא נקראה. */
        boolean unreadable;
        /** התיקייה לא קיימת (נמחקה, או שהכרטיס הוצא). */
        boolean missing;
    }

    /** תיקיות שאנדרואיד 11 ומעלה חוסם בפני כל התוכנות — גם עם הרשאה לכל הקבצים. */
    static boolean blocked(File d) {
        String p = d.getAbsolutePath();
        return p.endsWith("/Android/data") || p.endsWith("/Android/obb")
                || p.contains("/Android/data/") || p.contains("/Android/obb/");
    }

    private static final Comparator<File> BY_NAME = new Comparator<File>() {
        @Override
        public int compare(File a, File b) {
            int c = a.getName().compareToIgnoreCase(b.getName());
            return c != 0 ? c : a.getName().compareTo(b.getName());
        }
    };

    /** התיקייה כמו שהיא עכשיו: תיקיות קודם, ואחריהן קבצים — כל קבוצה לפי השם. */
    static Listing read(File dir, boolean showHidden) {
        return read(dir, showHidden, 0, false);
    }

    /**
     * 1.11.0: עם מיון — by: 0 שם · 1 תאריך · 2 גודל · 3 סוג (סיומת); desc — יורד. תיקיות תמיד קודם
     * (תיקיות — לפי שם או תאריך; לגודל ולסוג — לפי שם).
     */
    static Listing read(File dir, boolean showHidden, int by, boolean desc) {
        Listing l = read0(dir, showHidden);
        Comparator<File> c = comparator(by);
        Collections.sort(l.files, desc ? Collections.reverseOrder(c) : c);
        Comparator<File> d = by == 1 ? c : BY_NAME;
        Collections.sort(l.dirs, desc && by <= 1 ? Collections.reverseOrder(d) : d);
        return l;
    }

    static Comparator<File> comparator(final int by) {
        if (by == 0) return BY_NAME;
        return new Comparator<File>() {
            @Override
            public int compare(File a, File b) {
                int c;
                if (by == 1) c = Long.compare(a.lastModified(), b.lastModified());
                else if (by == 2) c = Long.compare(a.length(), b.length());
                else c = Text.tail(a.getName()).compareToIgnoreCase(Text.tail(b.getName()));
                return c != 0 ? c : BY_NAME.compare(a, b);
            }
        };
    }

    private static Listing read0(File dir, boolean showHidden) {
        Listing l = new Listing();
        if (!dir.isDirectory()) {
            l.missing = true;
            return l;
        }
        File[] all = dir.listFiles();
        if (all == null || all.length == 0) {
            if (blocked(dir)) l.blocked = true;
            else if (all == null) l.unreadable = true;
            return l;
        }
        for (File f : all) {
            if (!showHidden && f.getName().startsWith(".")) {
                l.hidden++;
                continue;
            }
            if (f.isDirectory()) l.dirs.add(f); else l.files.add(f);
        }
        Collections.sort(l.dirs, BY_NAME);
        Collections.sort(l.files, BY_NAME);
        return l;
    }
}
