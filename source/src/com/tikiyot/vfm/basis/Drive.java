package com.tikiyot.vfm;

// כונן — צלמית ושם במקום אחד (80.5). כל מסך שמציג כונן לוקח מכאן.

public final class Drive {

    private Drive() { }

    /** המפתח של הזיכרון הפנימי (בתנאים: מקור:פנימי), ושל "כל כרטיס" (מקור:כרטיס). */
    public static final String INTERNAL = "פנימי", CARD = "כרטיס";  // תחביר
    public static final String INTERNAL_NAME = Words.DriveW.INTERNAL_NAME, CARD_NAME = Words.DriveW.CARD_NAME;
    public static final String INTERNAL_ICON = "📱", CARD_ICON = "💾";

    public static boolean internalPath(String path) {
        return path != null && path.startsWith("/storage/emulated/");
    }

    public static String icon(boolean internal) {
        return internal ? INTERNAL_ICON : CARD_ICON;
    }

    /** שם הכונן: "זיכרון פנימי", "כרטיס זיכרון", "כרטיס זיכרון 2". */
    public static String name(boolean internal, int card) {
        return internal ? INTERNAL_NAME : card > 1 ? CARD_NAME + " " + card : CARD_NAME;
    }

    /** "📱 זיכרון פנימי" — צלמית ושם. */
    public static String label(boolean internal, String name) {
        return icon(internal) + " " + name;
    }

    /** לפי מסלול (כשאין אינדקס): "זיכרון פנימי" או "כרטיס זיכרון". */
    public static String nameOf(String path) {
        return internalPath(path) ? INTERNAL_NAME : CARD_NAME;
    }

    /** כונן של אינדקס: צלמית ושם. */
    public static String label(Index ix) {
        return label(INTERNAL.equals(ix.key), ix.label);
    }
}
