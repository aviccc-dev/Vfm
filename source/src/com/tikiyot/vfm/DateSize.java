package com.tikiyot.vfm;

// סינון לפי תאריך וגודל (128) — מקום אחד לתחביר ולשדות: הרשימה החכמה (תנאי: "תאריך:<שנה גודל:>10MB")
// וקובצי הפקודות (מתגים: "/תאריך:<שנה /גודל:>10MB"). בלי ממשק, ולכן נבדק. הממשק — FilterForm.

import java.util.Locale;

final class DateSize {

    /** התחביר: "תאריך:<שנה", "גודל:>10MB" — מכאן לוקחים כולם (גם קובצי הפקודות). */
    static final String DATE = Cond.DATE + ":", SIZE = Cond.SIZE + ":";
    /** 189: "נוסף:שבוע" — לפי מתי הקובץ הגיע (רק ברשימה החכמה ובחיפוש; לא בקובצי הפקודות). */
    static final String ADDED = Cond.ADDED + ":";

    static final String[] DATE_MODES = {Words.DateW.NONE, Words.DateW.SINCE, Words.DateW.BEFORE_MORE_THAN};
    static final String[] DATE_UNITS = {"יום", "שבוע", "חודש", "שנה"};  // תחביר
    static final String[] SIZE_MODES = {Words.DateW.NONE, Words.DateW.BIGGER, Words.DateW.SMALLER};
    /** "B" — בתים (בלי יחידה בתחביר). */
    static final String[] SIZE_UNITS = {"B", "KB", "MB", "GB"};

    /** 0 בלי · 1 מאז · 2 לפני. */
    int dateMode;
    String dateValue = "שבוע";  // תחביר
    /** 189: התאריך — של ההוספה ("נוסף:"), ולא של השינוי. */
    boolean added;
    /** 189: הטופס מציע את הבחירה (הרשימה החכמה); בקובצי הפקודות — לא. */
    boolean allowAdded;
    /** 0 בלי · 1 גדול מ־ · 2 קטן מ־. */
    int sizeMode;
    String sizeNumber = "10", sizeUnit = "MB";

    /** בחזרה לברירת המחדל — בלי תאריך ובלי גודל (147: טעינת רשימה אחרת לטופס). */
    void clear() {
        DateSize d = new DateSize();
        dateMode = d.dateMode;
        dateValue = d.dateValue;
        added = d.added;
        sizeMode = d.sizeMode;
        sizeNumber = d.sizeNumber;
        sizeUnit = d.sizeUnit;
    }

    DateSize copy() {
        DateSize d = new DateSize();
        d.dateMode = dateMode;
        d.dateValue = dateValue;
        d.added = added;
        d.allowAdded = allowAdded;
        d.sizeMode = sizeMode;
        d.sizeNumber = sizeNumber;
        d.sizeUnit = sizeUnit;
        return d;
    }

    /** ערך גודל שהשדות יודעים להציג: מספר, ואולי B/KB/MB/GB. */
    static boolean sizeOk(String v) {
        return v.trim().toUpperCase(Locale.ROOT).matches("\\d+(\\.\\d+)?\\s*(B|KB|MB|GB)?");
    }

    /**
     * קולט חלק של תנאי בתחביר החיפוש ("תאריך:…", "גודל:…"). true — נקלט לשדות; false — לא שלו,
     * או שהשדות לא יודעים להציג אותו (ואז הוא נשאר כמו שהוא, ולא מנחשים — 122).
     */
    boolean take(String tok) {
        boolean byAdded = allowAdded && tok.startsWith(ADDED);
        String key = byAdded ? ADDED : DATE;
        if (tok.startsWith(key) && dateMode == 0 && tok.length() > key.length()) {
            String v = tok.substring(key.length());
            added = byAdded;
            if (v.startsWith("<")) {
                dateMode = 2;
                v = v.substring(1);
            } else {
                dateMode = 1;
                if (v.startsWith(">=")) v = v.substring(2);
                else if (v.startsWith(">")) v = v.substring(1);
            }
            dateValue = v;
            return true;
        }
        int k = SIZE.length();                                          // הסימן (> או <) אחרי "גודל:"
        if (tok.startsWith(SIZE) && sizeMode == 0 && tok.length() > k + 1
                && (tok.charAt(k) == '>' || tok.charAt(k) == '<') && sizeOk(tok.substring(k + 1))) {
            sizeMode = tok.charAt(k) == '>' ? 1 : 2;
            String v = tok.substring(k + 1).toUpperCase(Locale.ROOT).trim();
            sizeUnit = "B";
            for (String unit : new String[]{"KB", "MB", "GB"})
                if (v.endsWith(unit)) {
                    sizeUnit = unit;
                    v = v.substring(0, v.length() - 2);
                }
            if (sizeUnit.equals("B") && v.endsWith("B")) v = v.substring(0, v.length() - 1);
            sizeNumber = v.trim();
            return true;
        }
        return false;
    }

    /** "תאריך:<שנה", או "" כשאין. */
    String dateToken() {
        return dateMode == 0 || dateValue.trim().isEmpty() ? "" : (added && allowAdded ? ADDED : DATE) + (dateMode == 2 ? "<" : "")
                + dateValue.trim().replaceAll("(\\d)\\s+(\\D)", "$1$2");      // "3 ימים" ← "3ימים": בלי רווח, גם בקובץ פקודות
    }

    /** "גודל:>10MB", או "" כשאין. */
    String sizeToken() {
        if (sizeMode == 0 || sizeNumber.trim().isEmpty()) return "";
        return SIZE + (sizeMode == 1 ? ">" : "<") + sizeNumber.trim() + (sizeUnit.equals("B") ? "" : sizeUnit);
    }

    /** בתחביר החיפוש: "תאריך:<שנה גודל:>10MB". */
    String toCond() {
        String d = dateToken(), s = sizeToken();
        return (d + (d.isEmpty() || s.isEmpty() ? "" : " ") + s).trim();
    }
}
