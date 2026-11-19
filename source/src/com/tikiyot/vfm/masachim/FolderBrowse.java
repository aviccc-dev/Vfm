package com.tikiyot.vfm;

// מבנה 280 (אפיון החיפוש — החלוקה הרזה; המשתמש, 19.11.2026: "כן"): מתוך SearchScreen, בלי שינוי בהתנהגות — שני חלקים שנוגעים
// בתיקיות של הכונן: התצוגה בתיקיות (148: מקורות, למעלה, תיקייה, "נשאר באפור") ופס הלוח וההדבקה (149).
// המצב של המסך (השורות, התצוגה, התיקייה שבה עומדים, המודל) נשאר במסך; כאן — רק מה ששייך לשני החלקים.

import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

final class FolderBrowse {

    private final SearchScreen s;
    /** 1.11.0: התיקייה שעליה כבר הודענו "לא באינדקס / לא קיימת" — כדי שההודעה לא תקפוץ בכל ריענון. */
    private String warned;
    /** 149: פס הלוח — "הדבקה כאן" (ב"כולל הכול") ו"הדבקה…" (בורר); ומוסתרים ב"כולל הכול". */
    private LinearLayout clipBar;
    private TextView clipText;
    private Button pasteHere;
    final List<String> volRoots = new ArrayList<String>();

    FolderBrowse(SearchScreen s) {
        this.s = s;
    }

    void buildClipBar(LinearLayout root) {
        clipBar = new LinearLayout(s);                                     // 149
        clipBar.setOrientation(LinearLayout.HORIZONTAL);
        clipBar.setGravity(android.view.Gravity.CENTER_VERTICAL);
        clipBar.setBackgroundColor(Ui.HEADER);
        clipBar.setPadding(Ui.dp(s, 8), Ui.dp(s, 4), Ui.dp(s, 8), Ui.dp(s, 4));
        clipText = new TextView(s);
        clipText.setTextColor(Ui.TEXT);
        clipText.setTextSize(14);
        clipBar.addView(clipText, new LinearLayout.LayoutParams(0, -2, 1f));
        pasteHere = clipButton(Words.PASTE_HERE, new Runnable() {
            @Override public void run() { PhysicalUi.paste(s, s.here == null ? null : new File(s.here), afterPaste()); }
        });
        clipButton(Words.Here.PASTE_PICK, new Runnable() {
            @Override public void run() { pastePick(); }
        });
        clipButton(Words.CANCEL, new Runnable() {
            @Override
            public void run() {
                Clip.clear();
                showClip();
            }
        });
        clipBar.setVisibility(View.GONE);
        root.addView(clipBar);
    }

    // ---------- 149: הלוח ברשימה החכמה ----------

    private Button clipButton(String text, final Runnable r) {
        Button b = new Button(s);
        b.setText(text);
        b.setAllCaps(false);
        b.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { r.run(); }
        });
        clipBar.addView(b);
        return b;
    }

    Runnable afterPaste() {
        return new Runnable() {
            @Override
            public void run() {
                s.search();
                showClip();
            }
        };
    }

    /** מה בלוח; "הדבקה כאן" — רק ב"כולל הכול", בתוך תיקייה שאפשר להדביק בה. */
    void showClip() {
        if (clipBar == null) return;
        if (Clip.isEmpty()) {
            clipBar.setVisibility(View.GONE);
            return;
        }
        clipBar.setVisibility(View.VISIBLE);
        boolean canHere = FolderView.ALL.equals(s.view) && s.here != null && Clip.cannotPasteInto(new File(s.here)) == null;
        clipText.setText("📋 " + Clip.label());
        pasteHere.setVisibility(canHere ? View.VISIBLE : View.GONE);
    }

    void pastePick() {
        FolderPicker.choose(s, Words.Here.PICK_TITLE, true, new FolderPicker.Picked() {
            @Override public void ok(File folder) { PhysicalUi.paste(s, folder, afterPaste()); }
        });
    }

    // ---------- 148: רשימה חכמה בתיקיות ----------

    boolean folderMode() {
        return !s.emptyMode && FolderView.folders(s.view);
    }

    /** מאיפה מתחילים: המקורות שבתנאי, או הכוננים. */
    List<String> roots() {
        List<String[]> vols = new ArrayList<String[]>();
        volRoots.clear();
        for (Vols.Vol v : Vols.list(s)) {
            vols.add(new String[]{v.root.getAbsolutePath(), v.internal ? Sources.INTERNAL : Sources.CARD});
            volRoots.add(v.root.getAbsolutePath());
        }
        return FolderView.roots(Sources.fromCond(s.query()), vols);
    }

    /** שם של מקור: כונן — צלמית ושם; תיקייה — השם והכונן. */
    String rootLabel(String r) {
        if (volRoots.contains(r)) return Drive.label(Drive.internalPath(r), Drive.nameOf(r));
        return FolderView.name(r) + " · " + Drive.nameOf(r);
    }

    void up() {
        if (s.fv == null || !s.fv.canGoUp(s.here)) return;
        s.here = s.fv.up(s.here);
        showFolder();
    }

    /** התיקייה הנוכחית: "רק מה שעונה" — מהמודל; "כולל הכול" — מהאינדקס, וההתאמות מסומנות. */
    void showFolder() {
        if (s.fv == null) return;
        s.rows.clear();
        boolean all = FolderView.ALL.equals(s.view);
        List<FolderView.Folder> dirs = new ArrayList<FolderView.Folder>();
        List<Library.Hit> files = new ArrayList<Library.Hit>();
        if (s.here == null) dirs.addAll(s.fv.top(!all));
        else if (!all) {
            dirs.addAll(s.fv.matchDirs(s.here));
            for (String p : s.fv.matchFiles(s.here)) {
                Library.Hit x = s.lib.find(p);
                if (x != null) files.add(x);
            }
        } else {
            List<File> ls = s.lib.listDir(new File(s.here), s.hidden, new Runnable() {   // השתנתה בכונן — מסונכרנת ומוצגת
                @Override
                public void run() {
                    s.runOnUiThread(new Runnable() {
                        @Override public void run() { s.search(); }
                    });
                }
            });
            if (ls == null && !s.here.equals(warned)) {                           // 1.11.0: פעם אחת לתיקייה, ובמילים הנכונות
                warned = s.here;
                Ui.toast(s, new File(s.here).isDirectory() ? Words.Views.NO_ACCESS : Words.Views.NOT_FOUND);
            }
            else for (File f : ls) {
                String p = f.getAbsolutePath();
                if (f.isDirectory()) dirs.add(new FolderView.Folder(p, f.getName(), s.fv.matches(p)));
                else {
                    Library.Hit x = s.lib.find(p);
                    if (x != null) files.add(x);
                }
            }
        }
        if (s.here != null) java.util.Collections.sort(dirs, new java.util.Comparator<FolderView.Folder>() {
            @Override
            public int compare(FolderView.Folder a, FolderView.Folder b) {
                return a.name.compareToIgnoreCase(b.name);
            }
        });
        s.srt.apply(files, SearchScreen.HIT);
        s.rows.addAll(dirs);
        s.rows.addAll(files);
        StringBuilder w = new StringBuilder();
        if (s.here == null) w.append("📂 ").append(Words.Views.SOURCES);
        else {
            if (s.fv.canGoUp(s.here)) w.append(Words.Views.UP).append("  ");
            String root = s.fv.rootOf(s.here);
            w.append(root == null ? s.here : rootLabel(root));
            if (root != null && s.here.length() > root.length())
                for (String part : s.here.substring(root.length() + 1).split("/")) w.append(" › ").append(part);
        }
        if (s.rows.isEmpty()) w.append("   ·   ").append(Words.Views.NO_MATCH_HERE);
        s.where.setText(w.toString());
        s.where.setVisibility(View.VISIBLE);
        s.adapter.notifyDataSetChanged();
        showClip();                                                           // 149: "הדבקה כאן" — לפי התיקייה
    }

    /**
     * 146 בתיקיות: אחרי פעולה מחשבים מחדש. ב"רק מה שעונה" — קובץ שהיה כאן ונשאר בתיקייה (שינוי שם) או נמחק,
     * ואינו עונה עוד — נשאר באפור עם הסיבה. קובץ שעבר לתיקייה אחרת — יוצא ממנה, כמו במנהל קבצים.
     */
    void stayInFolder() {
        List<Library.Hit> before = new ArrayList<Library.Hit>();
        java.util.Map<String, FileItem.State> was = new java.util.HashMap<String, FileItem.State>(s.out);
        for (Object o : s.rows) if (o instanceof Library.Hit) before.add((Library.Hit) o);
        String dir = s.here;
        s.search();
        if (!FolderView.MATCH.equals(s.view) || dir == null || !dir.equals(s.here)) return;
        Set<String> shown = new java.util.HashSet<String>();
        for (Object o : s.rows) if (o instanceof Library.Hit) shown.add(((Library.Hit) o).path());
        for (Library.Hit old : before) {
            String p = old.path();
            FileItem.State prev = was.get(p);
            String now = prev != null && prev != FileItem.State.OUT ? p : Physical.whereNow(p);
            if (shown.contains(now)) continue;
            FileItem.State why = prev != null && prev != FileItem.State.OUT ? prev : FileItem.dropped(s.lib, now, true);
            if (why == FileItem.State.OUT) {
                if (!dir.equals(FolderView.parent(now))) continue;            // עבר לתיקייה אחרת
                Library.Hit at = s.lib.find(now);
                if (at == null) continue;
                s.rows.add(at);
                s.out.put(now, why);
            } else {
                s.rows.add(old);
                s.out.put(p, why);
            }
        }
        s.adapter.notifyDataSetChanged();
    }
}
