package com.tikiyot.vfm;

// 195: לפי מה קובץ נחשב "אותו קובץ" בכפולים — שלושה רכיבים, וכל השילובים: שם (לא משנה / זהה / דומה), גודל,
// תוכן (חתימה; כולל את הגודל). נשמר בקובץ הראשי בשם קבוע לכל שילוב (גם השמות שהיו לפני 195).

final class DupMode {

    static final int NAME_ANY = 0, NAME_SAME = 1, NAME_SIMILAR = 2;

    final int name;
    final boolean size, content;

    DupMode(int name, boolean size, boolean content) {
        this.name = name;
        this.content = content;
        this.size = size || content;                                       // החתימה כוללת את הגודל
    }

    /** "שם דומה" — רק בתוך גודל זהה (ההשוואה בין כל שם לכל שם — כבדה מדי). */
    boolean valid() {
        return name == NAME_SAME || size;
    }

    /** השם השמור בקובץ הראשי ("השוואה = …"). */
    String text() {
        if (content) return name == NAME_SAME ? Words.Dup.NAME_CONTENT : name == NAME_SIMILAR ? Words.Dup.CONTENT_SIMILAR : Words.Dup.CONTENT;
        if (size) return name == NAME_SAME ? Words.Dup.NAME_SIZE : name == NAME_SIMILAR ? Words.Dup.SIZE_SIMILAR : Words.Dup.SIZE;
        return Words.Dup.NAME;
    }

    static DupMode parse(String s) {
        if (s == null || s.trim().isEmpty() || s.equals(Words.Dup.NAME_SIZE)) return new DupMode(NAME_SAME, true, false);
        if (s.equals(Words.Dup.SIZE_SIMILAR)) return new DupMode(NAME_SIMILAR, true, false);
        if (s.equals(Words.Dup.SIZE)) return new DupMode(NAME_ANY, true, false);
        if (s.equals(Words.Dup.NAME)) return new DupMode(NAME_SAME, false, false);
        if (s.equals(Words.Dup.CONTENT)) return new DupMode(NAME_ANY, true, true);
        if (s.equals(Words.Dup.NAME_CONTENT)) return new DupMode(NAME_SAME, true, true);
        if (s.equals(Words.Dup.CONTENT_SIMILAR)) return new DupMode(NAME_SIMILAR, true, true);
        return new DupMode(NAME_SAME, true, false);
    }
}
