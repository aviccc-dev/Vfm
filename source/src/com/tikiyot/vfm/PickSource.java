package com.tikiyot.vfm;

// מקור העץ של הבורר (106): מה יש בתוך כל צומת. הבורר עצמו אחד — PickerModel ו-PickDialog — לרשימות, לתיקיות
// ולהעתקה מאופן עבודה אחר (171); כל סוג מספק רק את המקור שלו: TreeSource (רשימות), IndexSource (תיקיות בכוננים,
// מהאינדקס). מה שאינו מופשט — ברירת מחדל, ומקור משנה רק מה שלו.

import java.util.List;

abstract class PickSource {

    /** שמות הילדים של צומת, לפי הסדר. path ריק = השורש. null = הצומת לא קיים. */
    abstract List<String> children(List<String> path);

    /** עץ גדול: בתצוגת עץ הענפים סגורים, ונפתחים בנגיעה. */
    abstract boolean large();

    /** שם מלא להצגה (בכותרת ובמסלול שליד תוצאת חיפוש). */
    abstract String fullLabel(List<String> path);

    /** נכנסו לצומת או פתחו אותו — המקור יכול להשלים את מה שבתוכו (137: תיקיות מהכונן שאינן באינדקס). */
    void visit(List<String> path) { }

    /** 130: האם יש למקור תיקיות מוסתרות, ולהציג אותן או לא. */
    boolean canShowHidden() {
        return false;
    }

    void showHidden(boolean show) { }

    /** 171: השם שמוצג לצומת (ברירת מחדל — החלק האחרון של המסלול). */
    String label(List<String> path) {
        return path.get(path.size() - 1);
    }

    /** 171: צלמית לצומת; null — של סוג הבורר. */
    String icon(List<String> path) {
        return null;
    }

    /** 171: שורה שנייה מתחת לשם; null — בלי. */
    String sub(List<String> path) {
        return null;
    }

    /** 171: למה אי אפשר לבחור את הצומת; null — אפשר. */
    String whyNot(List<String> path) {
        return null;
    }

    /** 171: הכותרת כשעומדים בשורש; null — "כל הרשימות". */
    String top() {
        return null;
    }
}
