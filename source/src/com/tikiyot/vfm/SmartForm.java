package com.tikiyot.vfm;

// מסך הגדרה לתיקייה חכמה: שדות פשוטים, ושורת התנאי שנוצרת מהם.

import android.app.Activity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public final class SmartForm {

    /** 99: אפשרויות המיון — מהרכיב האחד (79), עם האפשרות המיוחדת של רשימה חכמה ("דירוג"). */
    private static final String[] SORTS = {Sort.RANK, Sort.NAME, Sort.DATE, Sort.SIZE, Sort.TYPE, Sort.FOLDER};

    private SmartForm() { }

    /** חלקי הטופס — משותפים לרשימה חכמה ולחיפוש: מקורות, סוגים, תאריך, גודל, מילים. */
    public static final class Parts {
        /** 145: סוגי הקבצים — המודל (Types.Choice) והקבוצות מההגדרות. */
        final Types.Choice kinds = new Types.Choice();
        final List<Types.Group> groups = Types.groups();
        /** 198: "הכול" — תיבה ראשונה בין הסוגים (במקום שורת "הכול / רק מה שאבחר"). */
        final CheckBox allKinds;
        final List<CheckBox> groupBoxes = new ArrayList<CheckBox>();
        final CheckBox otherBox;
        /** 175: רק תיקיות ריקות. */
        CheckBox empty;
        final Button more;
        /** 199: התיבות — הכול, הסוגים, "עוד סיומות"; ובטופס הרשימה גם "רק תיקיות ריקות". */
        final Flow cells;
        /** 201: השורה הראשונה — "תיקיות ריקות" ו"הכול"; הסוגים — מתחתיה. */
        final Flow modes;
        final LinearLayout pickArea;
        /** 128: תאריך וגודל — הרכיב המשותף (במקום רשימות קבועות). */
        final DateSize ds = new DateSize();

        {
            ds.allowAdded = true;                                             // 189
        }
        LinearLayout filterView;
        Runnable onChange;
        EditText words;
        Sources src;

        public Parts(final Activity a, String emptySources) {
            empty = Ui.check(a, Words.Empty.TITLE, false);
            allKinds = Ui.check(a, Words.Kinds.ALL, true);
            allKinds.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            pickArea = new LinearLayout(a);
            pickArea.setOrientation(LinearLayout.VERTICAL);
            cells = new Flow(a, 8, 0);                                        // 197: כל תיבה ברוחב שלה; גולש לשורה הבאה
            modes = new Flow(a, 16, 0);
            modes.addView(allKinds);
            for (Types.Group g : groups) {
                CheckBox c = Ui.check(a, g.name, false);
                groupBoxes.add(c);
                cells.addView(c);
            }
            otherBox = Ui.check(a, Types.OTHER, false);
            cells.addView(otherBox);
            int h = Ui.dp(a, 38);                                             // 198: צפוף יותר מ־48 של המערכת
            for (int i = 0; i < cells.getChildCount(); i++) {
                cells.getChildAt(i).setMinimumHeight(h);
                if (cells.getChildAt(i) instanceof TextView) {
                    ((TextView) cells.getChildAt(i)).setMinHeight(h);
                    ((TextView) cells.getChildAt(i)).setTextSize(13);         // 200: "קובצי נתונים" ו"שאר הקבצים" — בשורה אחת
                }
            }
            empty.setMinimumHeight(h);
            empty.setMinHeight(h);
            empty.setTextSize(13);
            allKinds.setMinimumHeight(h);
            allKinds.setMinHeight(h);
            allKinds.setTextSize(13);
            more = new Button(a);
            more.setText(Words.Kinds.MORE);
            more.setAllCaps(false);
            more.setTextSize(13);
            more.setTextColor(Ui.ACCENT);
            more.setBackground(null);
            more.setGravity(android.view.Gravity.CENTER_VERTICAL | android.view.Gravity.START);
            more.setPadding(Ui.dp(a, 4), 0, Ui.dp(a, 4), 0);
            more.setMinWidth(0);
            more.setMinimumWidth(0);
            more.setMinHeight(h);
            more.setMinimumHeight(h);
            more.setSingleLine(true);                                         // 199: הסיומות שנבחרו — על הכפתור עצמו
            more.setEllipsize(android.text.TextUtils.TruncateAt.END);
            cells.addView(more);
            pickArea.addView(modes, new LinearLayout.LayoutParams(-1, -2));
            pickArea.addView(cells, new LinearLayout.LayoutParams(-1, -2));
            more.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    TypePicker.choose(a, kinds.manual, new TypePicker.Picked() {
                        @Override
                        public void ok(java.util.Set<String> chosen) {
                            kinds.manual.clear();
                            kinds.manual.addAll(chosen);
                            changed();
                        }
                    });
                }
            });
            words = Ui.field(a, "", Words.SmartF.WORDS_OPT);
            src = new Sources(a, emptySources);
            show();
        }

        /** מהמסך אל המודל. */
        private void read() {
            kinds.all = allKinds.isChecked();
            kinds.groups.clear();
            for (int i = 0; i < groups.size(); i++) if (groupBoxes.get(i).isChecked()) kinds.groups.add(groups.get(i).name);
            kinds.other = otherBox.isChecked();
        }

        /** מהמודל אל המסך. */
        private void show() {
            allKinds.setChecked(kinds.all);
            for (int i = 0; i < groups.size(); i++) groupBoxes.get(i).setChecked(kinds.groups.contains(groups.get(i).name));
            otherBox.setChecked(kinds.other);
            enable();
        }

        private void enable() {
            // 198: "הכול" מסומן — הסוגים חיוורים (אבל אפשר לגעת: נגיעה בסוג מבטלת את "הכול")
            // 199: "רק תיקיות ריקות" — לתיקייה אין סוג: כל התיבות אפורות, ואי אפשר לבחור בהן
            boolean dirs = empty.isChecked();
            float alpha = dirs || allKinds.isChecked() ? 0.4f : 1f;
            allKinds.setEnabled(!dirs);
            allKinds.setAlpha(dirs ? 0.4f : 1f);
            for (CheckBox c : groupBoxes) {
                c.setAlpha(alpha);
                c.setEnabled(!dirs);
            }
            otherBox.setAlpha(alpha);
            otherBox.setEnabled(!dirs);
            more.setAlpha(alpha);
            more.setEnabled(!dirs);
            StringBuilder m = new StringBuilder();
            for (String e : kinds.manual) m.append(m.length() > 0 ? ", " : "").append(e);
            more.setText(m.length() == 0 ? Words.Kinds.MORE : Words.Kinds.picked(m.toString()));
        }

        private void changed() {
            read();
            enable();
            if (onChange != null) onChange.run();
        }

        /** נבחר "רק הסוגים שאבחר" — ולא נבחר אף אחד. */
        public boolean noKind() {
            read();
            return !empty.isChecked() && !kinds.all && kinds.empty();         // 175: תיקיות — בלי סוג
        }

        /** מקורות וסוגים תמיד; תאריך, גודל ומילים — רק ברשימה חכמה. */
        public void addTo(Activity a, LinearLayout b, boolean full) {
            addTo(a, b, full, true);
        }

        /** 153: בלי שדה המילים — במסך החיפוש המילים בשורת ההקלדה. */
        public void addTo(Activity a, LinearLayout b, boolean full, boolean withWords) {
            b.addView(Ui.label(a, Words.SmartF.FROM_WHERE));
            b.addView(src.view);
            b.addView(Ui.label(a, Words.Kinds.TITLE));
            b.addView(pickArea);
            if (!full) return;
            b.addView(empty);                                                 // 175
            filterView = FilterForm.build(a, ds, new Runnable() {
                @Override public void run() { if (onChange != null) onChange.run(); }
            });
            b.addView(filterView);
            if (!withWords) return;
            b.addView(Ui.label(a, Words.SmartF.WORDS_IN_NAME));
            b.addView(words);
        }

        /** 197: "מאיפה" — שורה עם תווית, מעל "מה מחפשים". */
        public void addFrom(Activity a, LinearLayout b) {
            b.addView(line(a, Words.SmartF.FROM, src.view, true));
        }

        /**
         * 197–199: "מה מחפשים" — התיבות (הכול, ספרים… ו"רק תיקיות ריקות") ברוחב מלא, בשורה שגולשת; ומילים.
         * בלי התווית "סוגים" — כך נכנסות יותר תיבות בשורה.
         */
        public void addGrouped(Activity a, LinearLayout b) {
            modes.addView(empty, 0);                                         // 199/201: תיקיות ריקות — ראשון, ולידו "הכול"
            b.addView(pickArea);
            words.setHint(Words.SmartF.WORDS_HINT);
            LinearLayout w = line(a, Words.SmartF.WORDS_L, words, false);   // 201: "מכיל מילים" — ברוחב שלו (ארוך מהשאר)
            w.getChildAt(0).setLayoutParams(new LinearLayout.LayoutParams(-2, -2));
            ((TextView) w.getChildAt(0)).setPaddingRelative(0, 0, Ui.dp(a, 8), 0);
            b.addView(w);
        }

        /** 196: "עוד סינון" (תאריך, גודל, תיקיות ריקות) מקופל — נפתח בנגיעה, ופתוח מראש כשיש בו משהו. */
        public Fold addMore(Activity a) {
            Fold f = new Fold(a, Words.SmartF.MORE_FILTERS, moreSummary(), false);   // 200: סגור; 202: ומתחת — מה בו
            LinearLayout more = f.inner;
            filterView = FilterForm.build(a, ds, new Runnable() {
                @Override public void run() { if (onChange != null) onChange.run(); }
            });
            more.addView(filterView);
            return f;
        }

        /** 202: "עוד סינון" במילים — מה נבחר בו ("מאז שבוע · גדול מ־10MB"), או שאין בו כלום. */
        String moreSummary() {
            StringBuilder sb = new StringBuilder();
            if (ds.dateMode != 0)
                sb.append(Words.SmartF.phrase(DateSize.DATE_MODES[ds.dateMode], ds.dateValue)).append(ds.added ? Words.SmartF.BY_ADDED : "");
            if (ds.sizeMode != 0) {
                if (sb.length() > 0) sb.append(" · ");
                sb.append(Words.SmartF.phrase(DateSize.SIZE_MODES[ds.sizeMode], ds.sizeNumber.isEmpty() ? "" : ds.sizeNumber + ds.sizeUnit));
            }
            return sb.length() == 0 ? Words.SmartF.MORE_WHAT : sb.toString();
        }

        /** 147: טעינת רשימה אחרת לטופס — מנקה קודם את מה שהיה, ובונה מחדש את שדות התאריך והגודל. */
        public void reload(Activity a, String cond) {
            kinds.all = true;
            kinds.groups.clear();
            kinds.other = false;
            kinds.manual.clear();
            ds.clear();
            words.setText("");
            src.set(new ArrayList<String>());
            load(cond);
            show();
            if (filterView != null && filterView.getParent() instanceof android.view.ViewGroup) {
                android.view.ViewGroup parent = (android.view.ViewGroup) filterView.getParent();
                int at = parent.indexOfChild(filterView);
                parent.removeViewAt(at);
                filterView = FilterForm.build(a, ds, new Runnable() {
                    @Override public void run() { if (onChange != null) onChange.run(); }
                });
                parent.addView(filterView, at);
            }
            if (onChange != null) onChange.run();
        }

        public void load(String cond) {
            if (cond == null || cond.isEmpty()) return;
            src.set(Sources.fromCond(cond));
            StringBuilder rest = new StringBuilder();
            boolean kindTaken = false;
            for (String token : Cond.tokens(Sources.rest(cond))) {
                boolean used = false;
                if (!kindTaken && Cond.keyIs(token, Cond.TYPE)) {                // 145: קבוצות, "שאר", ידני — גם תנאי ישן
                    kinds.take(token.substring(token.indexOf(':') + 1), groups);
                    kindTaken = used = true;
                }
                if (!used && ds.take(token)) used = true;                     // 128
                if (!used && Cond.isEmptyDirs(token)) used = true;            // 175
                if (Cond.isEmptyDirs(token)) empty.setChecked(true);
                if (!used) rest.append(rest.length() > 0 ? " " : "").append(Cond.quoteToken(token));
            }
            words.setText(rest.toString());
            show();
        }

        public String build() {
            read();
            StringBuilder sb = new StringBuilder();
            add(sb, Sources.toCond(src.get()));
            add(sb, kinds.token());
            add(sb, ds.dateToken());
            add(sb, ds.sizeToken());
            if (empty.isChecked()) add(sb, Cond.EMPTY);                       // 175
            String w = words.getText().toString().trim();
            if (!w.isEmpty()) add(sb, w);
            return sb.toString();
        }

        public void watch(final Runnable update) {
            TextWatcher w = new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int i, int j, int k) { }
                @Override public void onTextChanged(CharSequence s, int i, int j, int k) { }
                @Override public void afterTextChanged(Editable s) { update.run(); }
            };
            words.addTextChangedListener(w);
            onChange = update;
            View.OnClickListener click = new View.OnClickListener() {
                @Override public void onClick(View v) { changed(); }
            };
            empty.setOnClickListener(click);
            allKinds.setOnClickListener(click);
            View.OnClickListener kind = new View.OnClickListener() {            // 198: סוג שנבחר — כבר לא "הכול"
                @Override public void onClick(View v) {
                    if (((CheckBox) v).isChecked()) allKinds.setChecked(false);
                    changed();
                }
            };
            for (CheckBox c : groupBoxes) c.setOnClickListener(kind);
            otherBox.setOnClickListener(kind);
            src.onChange(update);
        }
    }

    public static void show(final Activity a, String name, Target t, final Func.Done done) {
        LinearLayout b = Ui.form(a);
        b.setPadding(b.getPaddingLeft(), Ui.dp(a, 12), b.getPaddingRight(), 0);   // 197: אין כותרת חלון מעל
        final EditText nm = Ui.field(a, name != null ? name : "", Words.NAME_OPTIONAL);
        final Parts p = new Parts(a, Words.Src.ALL_DRIVES);                  // 198: קצר — ליד הכפתורים
        // 147: רשימה מוגדרת (עם שם) — לבחור, לשנות, לשמור בשם אחר; או מקומית, כמו קודם
        final List<SmartLists.Named> named = SmartLists.all();
        SmartLists.Named linked = t == null ? null : SmartLists.byId(SmartLists.linkOf(t));
        final String[] shown = SmartLists.start(t);                           // 152: גם לרשימה חדשה — שלושה ערכים
        final String[] dir = {linked != null ? linked.dir : t != null ? t.get(SmartLists.DIR, "") : ""};
        final Spinner sort = Ui.choice(a, SORTS, Ui.indexOf(SORTS, shown[1].isEmpty() ? Sort.RANK : shown[1], 0));
        final Spinner view = Ui.choice(a, FolderView.MODES, Ui.indexOf(FolderView.MODES, shown[2], 0));   // 148
        final TextView shareNote = Ui.info(a);                                // 196: במילים — רק כאן, או משותפת
        final TextView shareTop = Ui.info(a);
        // 1.17.3: "תבנית רשימה חכמה" במקום "הגדרה נפוצה" (המילון, 20: "רק התבנית משתנה") — טעינה מתבנית מעתיקה את
        // ההגדרות לטופס, ושמירה כתבנית שומרת עותק; הרשימה לא מקושרת לתבנית.
        final int[] current = {0};
        final Button unlink = new Button(a);
        final Fold[] advRef = new Fold[1];                                    // 202: מתחת ל"הגדרה נפוצה" — המצב
        final Runnable fillWhich = new Runnable() {
            @Override
            public void run() {
                if (advRef[0] != null) advRef[0].note(current[0] == 0 ? Words.SmartF.ADV_OWN
                        : "🔗 " + named.get(current[0] - 1).name);
                if (current[0] == 0) {
                    shareNote.setText(Words.SmartF.ONLY_HERE);
                    shareTop.setVisibility(View.GONE);
                    unlink.setVisibility(View.GONE);
                } else {
                    SmartLists.Named n = named.get(current[0] - 1);
                    String txt = Words.SmartF.shared(n.name, SmartLists.users(n.id));
                    shareNote.setText(txt);
                    shareTop.setText("🔗 " + txt);
                    shareTop.setVisibility(View.VISIBLE);
                    unlink.setVisibility(View.VISIBLE);
                }
            }
        };
        fillWhich.run();
        final Ui.Index1 pick = new Ui.Index1() {                             // טעינת הגדרה שמורה — ההגדרות שלה לטופס
            @Override
            public void ok(int pos) {
                SmartLists.Named n = named.get(pos);
                p.reload(a, n.cond);
                sort.setSelection(Ui.indexOf(SORTS, n.sort.isEmpty() ? Sort.RANK : n.sort, 0));
                view.setSelection(Ui.indexOf(FolderView.MODES, n.view, 0));
                dir[0] = n.dir;
                if (nm.getText().toString().trim().isEmpty()) nm.setText(n.name);
                fillWhich.run();
            }
        };
        Button load = new Button(a);
        load.setText(Words.SmartF.LOAD_SAVED);
        load.setAllCaps(false);
        load.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (named.isEmpty()) {
                    Ui.toast(a, Words.SmartF.NONE_SAVED);
                    return;
                }
                String[] labels = new String[named.size()];
                for (int i = 0; i < labels.length; i++) labels[i] = "✨ " + named.get(i).name;
                Ui.options(a, Words.SmartF.LOAD_TITLE, labels, new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface d, int w) { pick.ok(w); }
                });
            }
        });
        unlink.setText(Words.SmartF.UNLINK);
        unlink.setAllCaps(false);
        unlink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                current[0] = 0;
                fillWhich.run();
            }
        });
        Button saveAs = new Button(a);
        saveAs.setText(Words.SmartF.SAVE_REUSE);
        saveAs.setAllCaps(false);
        saveAs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (p.noKind()) {
                    Ui.toast(a, Words.Kinds.NONE_PICKED);
                    return;
                }
                String start = current[0] > 0 ? named.get(current[0] - 1).name : nm.getText().toString().trim();
                askSaveAs(a, start, new SaveAs() {
                    @Override
                    public void store(String n, boolean fresh) {
                        String id = SmartLists.save(n, condOf(p), (String) sort.getSelectedItem(), dir[0],
                                (String) view.getSelectedItem());
                        named.clear();
                        named.addAll(SmartLists.all());
                        fillWhich.run();
                        Ui.toast(a, Words.Named.SAVED_AS);
                    }
                });
            }
        });
        final TextView preview = new TextView(a);
        preview.setTextSize(13);
        preview.setTextColor(Ui.SUB);
        preview.setPadding(0, Ui.dp(a, 2), 0, 0);

        // 197: הסידור הדחוס (לפי העיצוב שאושר): "רשימה חכמה" ושם בשורה אחת; סוג; מאיפה; מה מחפשים; איך מציגים;
        // ולמטה — "עוד סינון" ו"הגדרה נפוצה" מקופלים.
        LinearLayout top = new LinearLayout(a);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(android.view.Gravity.CENTER_VERTICAL);
        TextView title = new TextView(a);
        title.setText(Words.SMART_LIST);
        title.setTextSize(14);
        title.setTextColor(Ui.TEXT);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        top.addView(title);
        TextView nameL = tag(a, Words.SmartF.NAME_L);
        nameL.setPaddingRelative(Ui.dp(a, 16), 0, Ui.dp(a, 8), 0);
        nameL.setLayoutParams(new LinearLayout.LayoutParams(-2, -2));
        top.addView(nameL);
        top.addView(nm, new LinearLayout.LayoutParams(0, -2, 1f));
        b.addView(top);
        b.addView(shareTop);
        LinearLayout slot = new LinearLayout(a);                              // הפס "סוג: חכמה | כפולים" — כאן
        slot.setOrientation(LinearLayout.VERTICAL);
        slot.setTag(Ui.HEADER_SLOT);
        b.addView(slot);
        p.load(shown[0]);                                                     // 128: לפני שהשדות נבנים
        p.addFrom(a, b);
        section(a, b, Words.SmartF.WHAT_SEARCH);
        p.addGrouped(a, b);
        section(a, b, Words.SmartF.HOW_SHOW);                                 // מיון ותצוגה — כל אחד בשורה
        if (fitsOneRow(a, SORTS, FolderView.MODES)) {                         // 199: אם נכנס — מיון ותצוגה בשורה אחת
            LinearLayout sv = new LinearLayout(a);
            sv.setOrientation(LinearLayout.HORIZONTAL);
            sv.addView(line(a, Words.SmartF.SORT_L, sort, false), new LinearLayout.LayoutParams(0, -2, 1f));
            TextView vl = tag(a, Words.SmartF.VIEW_L);
            vl.setLayoutParams(new LinearLayout.LayoutParams(-2, -2));
            vl.setPaddingRelative(Ui.dp(a, 8), 0, Ui.dp(a, 6), 0);
            LinearLayout vr = new LinearLayout(a);
            vr.setOrientation(LinearLayout.HORIZONTAL);
            vr.setGravity(android.view.Gravity.CENTER_VERTICAL);
            vr.addView(vl);
            vr.addView(view, new LinearLayout.LayoutParams(0, -2, 1f));
            sv.addView(vr, new LinearLayout.LayoutParams(0, -2, 1f));
            b.addView(sv);
        } else {
            b.addView(line(a, Words.SmartF.SORT_L, sort, false));
            b.addView(line(a, Words.SmartF.VIEW_L, view, false));            // 148
        }
        View rule = new View(a);
        rule.setBackgroundColor(Ui.RULE);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, Math.max(1, Ui.dp(a, 1) / 2));
        rp.topMargin = Ui.dp(a, 6);
        b.addView(rule, rp);
        Fold moreF = p.addMore(a);                                            // 198: שני המקופלים בשורה אחת
        final Fold moreRef = moreF;
        Fold advF = new Fold(a, Words.SmartF.ADVANCED, null, false);
        advRef[0] = advF;
        fillWhich.run();
        LinearLayout heads = new LinearLayout(a);
        heads.setOrientation(LinearLayout.HORIZONTAL);
        heads.addView(moreF.head, new LinearLayout.LayoutParams(0, -2, 1f));
        heads.addView(advF.head, new LinearLayout.LayoutParams(0, -2, 1f));
        b.addView(heads);
        moreF.partner = advF;                                                 // 200: אחד פתוח בכל פעם
        advF.partner = moreF;
        b.addView(moreF.inner);
        b.addView(advF.inner);
        // 1.16.0: "הגדרות" אחת (המילון) — גם הפרטים: צלמית, תיאור, "למה" (עד כאן — "שינוי שם ותיאור" נפרד)
        final String[] detailKeys = {Program.ICON, "תיאור", Program.WHY};  // תחביר
        final String[] detailLabels = {Words.DesignW.ICON_SHORT, Words.Manual.DESC, Words.WhyW.FIELD};
        final EditText[] details = new EditText[detailKeys.length];
        Fold detF = new Fold(a, Words.SmartF.DETAILS, null, false);
        for (int i = 0; i < detailKeys.length; i++) {
            details[i] = Ui.field(a, t == null ? "" : t.get(detailKeys[i], ""), null);
            detF.inner.addView(line(a, detailLabels[i], details[i], false));
        }
        b.addView(detF.head);
        b.addView(detF.inner);
        LinearLayout adv = advF.inner;
        adv.addView(shareNote);
        LinearLayout btns = new LinearLayout(a);
        btns.setOrientation(LinearLayout.HORIZONTAL);
        btns.addView(load, new LinearLayout.LayoutParams(0, -2, 1f));
        btns.addView(saveAs, new LinearLayout.LayoutParams(0, -2, 1f));
        adv.addView(btns);
        adv.addView(unlink);
        adv.addView(small(a, Words.SmartF.COND_IN_FILE));
        adv.addView(preview);
        Runnable update = new Runnable() {
            @Override public void run() {
                preview.setText(p.build());
                moreRef.note(p.moreSummary());                                 // 202: משתנה עם הבחירה
            }
        };
        p.watch(update);
        update.run();

        Ui.askFormWide(a, b, new Runnable() {
            @Override
            public void run() {
                String n = nm.getText().toString().trim();
                if (p.noKind()) {                                                // 145: בחירה מפורשת, לא מצב סמוי
                    Ui.toast(a, Words.Kinds.NONE_PICKED);
                    Ui.keepFormOpen();                                        // 98
                    return;
                }
                String c = condOf(p), s = (String) sort.getSelectedItem(), w = (String) view.getSelectedItem();
                Target out;
                if (current[0] > 0) {                                         // 147: מקושר — השינוי נשמר ברשימה עצמה
                    SmartLists.Named l = named.get(current[0] - 1);
                    if (!c.equals(l.cond) || !s.equals(l.sort) || !w.equals(l.view.isEmpty() ? FolderView.FLAT : l.view))
                        SmartLists.save(l.name, c, s, dir[0], w);
                    if (n.isEmpty()) n = l.name;
                    out = Target.of(FnSmart.KIND, SmartLists.LINK, l.id, SmartLists.SORT, s, SmartLists.DIR, dir[0]);
                } else {
                    if (n.isEmpty()) n = Words.SMART_LIST;
                    out = Target.of(FnSmart.KIND, SmartLists.COND, c, SmartLists.SORT, s, SmartLists.VIEW, w);
                }
                for (int i = 0; i < detailKeys.length; i++) {                 // ריק — רק אם נמחק (לא דורס מה שלא הוצג)
                    String v = details[i].getText().toString().trim().replace('\n', ' ');
                    if (!v.isEmpty() || t != null && !t.get(detailKeys[i], "").isEmpty()) out.params.put(detailKeys[i], v);
                }
                done.ok(n, out);
            }
        });
    }

    /** 197: התוויות שבתחילת השורות — הרחבה מהן קובעת את רוחב העמודה. */
    private static final String[] LABELS = {Words.SmartF.NAME_L, Words.SmartF.FROM,
            Words.SmartF.SORT_L, Words.SmartF.VIEW_L, Words.SearchTabs.KIND_L,
            Words.Filter.DATE_LABEL_ANY, Words.Filter.SIZE_LABEL};

    /**
     * 197: רוחב עמודת התוויות — נמדד לפי גודל הגופן במכשיר (בגופן גדול "מאיפה" נשבר לשתי שורות ברוחב קבוע).
     * כך השדות מתיישרים זה מתחת לזה.
     */
    static int labelW(Activity a) {
        android.text.TextPaint p = new android.text.TextPaint();
        p.setTextSize(android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP, 13,
                a.getResources().getDisplayMetrics()));
        float w = 0;
        for (String s : LABELS) w = Math.max(w, p.measureText(s));
        return (int) Math.ceil(w) + Ui.dp(a, 10);
    }

    /** 196/197: כותרת של חלק בטופס — מודגשת, וקו דק לידה עד הקצה. */
    static void section(Activity a, LinearLayout b, String title) {
        LinearLayout r = new LinearLayout(a);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(android.view.Gravity.CENTER_VERTICAL);
        TextView t = new TextView(a);
        t.setText(title);
        t.setTextSize(14);
        t.setTextColor(Ui.TEXT);
        t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        r.addView(t);
        View line = new View(a);
        line.setBackgroundColor(Ui.RULE);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, Math.max(1, Ui.dp(a, 1) / 2), 1f);
        lp.setMarginStart(Ui.dp(a, 8));
        r.addView(line, lp);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
        rp.topMargin = Ui.dp(a, 6);                                           // 198: פחות רווח — הכול בעמוד אחד
        b.addView(r, rp);
    }

    /**
     * 199: מיון ותצוגה — נכנסים בשורה אחת בלי לחתוך את הבחירה הארוכה? נמדד לפי הגופן ורוחב החלון במכשיר
     * (החלון: המסך פחות 20, עד 560; הטופס: פחות 36; לכל בחירה — עוד כ־48 לחץ ולריווח).
     */
    private static boolean fitsOneRow(Activity a, String[] sorts, String[] views) {
        android.util.DisplayMetrics dm = a.getResources().getDisplayMetrics();
        int win = Math.min(dm.widthPixels - Ui.dp(a, 20), Ui.dp(a, 560)) - Ui.dp(a, 36);
        android.text.TextPaint p = new android.text.TextPaint();
        p.setTextSize(android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP, 16, dm));
        float s = 0, v = 0;
        for (String x : sorts) s = Math.max(s, p.measureText(x));
        for (String x : views) v = Math.max(v, p.measureText(x));
        p.setTextSize(android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP, 13, dm));
        float need = labelW(a) + p.measureText(Words.SmartF.VIEW_L) + Ui.dp(a, 14) + s + v + 2 * Ui.dp(a, 48);
        return need <= win;
    }

    /** 197: התווית שבתחילת שורה — קטנה, אפורה, ברוחב קבוע. */
    static TextView tag(Activity a, String s) {
        TextView t = new TextView(a);
        t.setText(s);
        t.setTextSize(13);
        t.setTextColor(Ui.SUB);
        t.setLayoutParams(new LinearLayout.LayoutParams(labelW(a), -2));
        t.setSingleLine(true);
        return t;
    }

    /** 197: שורה — תווית ולידה הרכיב, עד הקצה. top — רכיב גבוה (כמו המקורות): התווית למעלה. */
    static LinearLayout line(Activity a, String label, View v, boolean top) {
        LinearLayout r = new LinearLayout(a);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(top ? android.view.Gravity.TOP : android.view.Gravity.CENTER_VERTICAL);
        r.setMinimumHeight(Ui.dp(a, 44));
        TextView t = tag(a, label);
        if (top) t.setPadding(0, Ui.dp(a, 12), 0, 0);
        r.addView(t);
        r.addView(v, new LinearLayout.LayoutParams(0, -2, 1f));
        return r;
    }

    /**
     * 198/200: חלק מקופל — "▸ כותרת"; נגיעה פותחת ("▾") וסוגרת. הכותרת והתוכן לחוד, כדי ששתי כותרות יעמדו
     * בשורה אחת. 200: פתיחה — סוגרת את השכן (partner), וגוללת עד שהתוכן נראה (בלי זה נפתח מתחת לקצה המסך,
     * ונראה כאילו לא קרה כלום).
     */
    static final class Fold {
        final TextView head;
        final LinearLayout inner;
        private final String title;
        private CharSequence closed, opened;
        Fold partner;

        Fold(Activity a, String title, String note, boolean open) {
            this.title = title;
            closed = foldText("▸ ", title, note);
            opened = foldText("▾ ", title, note);
            head = new TextView(a);
            head.setTextSize(14);
            head.setTextColor(Ui.SUB);
            head.setMinHeight(Ui.dp(a, 44));                                   // 197: שורה שנוח לגעת בה
            head.setGravity(android.view.Gravity.CENTER_VERTICAL);
            inner = new LinearLayout(a);
            inner.setOrientation(LinearLayout.VERTICAL);
            inner.setPaddingRelative(Ui.dp(a, 8), 0, 0, 0);
            set(open);
            head.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean now = inner.getVisibility() != View.VISIBLE;
                    if (now && partner != null) partner.set(false);
                    set(now);
                    if (now) inner.post(new Runnable() {
                        @Override
                        public void run() {
                            inner.requestRectangleOnScreen(new android.graphics.Rect(0, 0, inner.getWidth(), inner.getHeight()), false);
                        }
                    });
                }
            });
        }

        /** 202: השורה הקטנה שמתחת לכותרת — מה יש בפנים. */
        void note(String note) {
            closed = foldText("▸ ", title, note);
            opened = foldText("▾ ", title, note);
            set(inner.getVisibility() == View.VISIBLE);
        }

        void set(boolean open) {
            inner.setVisibility(open ? View.VISIBLE : View.GONE);
            head.setText(open ? opened : closed);
            head.setTextColor(open ? Ui.TEXT : Ui.SUB);
        }
    }

    private static CharSequence foldText(String arrow, String title, String note) {
        if (note == null) return arrow + title;
        android.text.SpannableString s = new android.text.SpannableString(arrow + title + "\n" + note);   // 202: בשורה מתחת
        int at = s.length() - note.length();
        s.setSpan(new android.text.style.RelativeSizeSpan(0.85f), at, s.length(), 0);
        s.setSpan(new android.text.style.ForegroundColorSpan(Ui.MUTED), at, s.length(), 0);
        return s;
    }

    /** 196: תווית קטנה, עם מעט רווח מעליה. */
    static TextView small(Activity a, String s) {
        TextView t = Ui.label(a, s);
        t.setPadding(0, Ui.dp(a, 4), 0, 0);
        return t;
    }

    /** התנאי שבטופס; "כל הסוגים" בלי מקור ובלי סינון — כל הקבצים בכל הכוננים, במפורש (145). */
    private static String condOf(Parts p) {
        String c = p.build();
        return c.isEmpty() ? Sources.toCond(java.util.Arrays.asList(Sources.INTERNAL, Sources.CARD)) : c;
    }

    private static void add(StringBuilder sb, String part) {
        if (part == null || part.isEmpty()) return;
        if (sb.length() > 0) sb.append(' ');
        sb.append(part);
    }

    // ---------- שמירה כהגדרה נפוצה (צמצום, שלב 4): רכיב אחד לטופס ולמסך הרשימה ----------

    interface SaveAs {
        /** fresh — אין עוד הגדרה נפוצה בשם הזה. */
        void store(String name, boolean fresh);
    }

    /** שואל שם; שם ריק — לא; שם קיים — אישור לעדכן אותה ("שינוי כאן יופיע ב־N מקומות"). */
    static void askSaveAs(final Activity a, String start, final SaveAs s) {
        Ui.askText(a, Words.Named.SAVE_AS_TITLE, start, Words.Named.NAME_HINT, new Ui.Text1() {
            @Override
            public void ok(String value) {
                final String n = value.trim();
                if (n.isEmpty()) {
                    Ui.toast(a, Words.NEED_NAME);
                    return;
                }
                final SmartLists.Named old = SmartLists.byName(n);
                Runnable store = new Runnable() {
                    @Override
                    public void run() {
                        s.store(n, old == null);
                    }
                };
                if (old == null) store.run();
                else Ui.ask(a, Words.Named.exists(n), Words.NO_TOUCH_FILES,
                        Words.Named.overwrite(SmartLists.users(old.id)), null, Words.Named.UPDATE, store);
            }
        });
    }
}
