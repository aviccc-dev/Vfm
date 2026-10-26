package com.tikiyot.vfm;

// עורך ויזואלי לקובץ הגדרות. משנה רק שורות מוכרות; כל השאר נשמר במקומו.

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class IniEditScreen extends Activity {

    public static final String EXTRA_FILE = "file";

    private File file;
    private Ini ini;
    private final List<Object> rows = new ArrayList<Object>();
    private BaseAdapter adapter;
    private TextView info;
    private boolean readOnly;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        String p = getIntent() != null ? getIntent().getStringExtra(EXTRA_FILE) : null;
        if (p == null) {
            finish();
            return;
        }
        file = new File(p);
        readOnly = file.getAbsolutePath().startsWith(Paths.mirror().getAbsolutePath());
        setTitle(file.getName() + (readOnly ? Words.IniEd.READ_ONLY_SUFFIX : ""));

        LinearLayout root = Ui.screen(this);
        info = Ui.info(this);
        root.addView(info);

        ListView list = new ListView(this);
        adapter = new BaseAdapter() {
            @Override public int getCount() { return rows.size(); }
            @Override public Object getItem(int i) { return rows.get(i); }
            @Override public long getItemId(int i) { return i; }
            @Override public int getViewTypeCount() { return 2; }
            @Override public int getItemViewType(int i) { return rows.get(i) instanceof String ? 1 : 0; }
            @Override public boolean isEnabled(int i) { return !(rows.get(i) instanceof String); }

            @Override
            public View getView(int pos, View convert, ViewGroup parent) {
                Object o = rows.get(pos);
                if (o instanceof String) return Ui.header(IniEditScreen.this, convert, (String) o, false);
                View v = Ui.row(IniEditScreen.this, convert);
                Ini.Line l = (Ini.Line) o;
                Ui.bindIcon(v, "•", l.key, l.value);
                return v;
            }
        };
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> a, View v, int pos, long id) {
                Object o = rows.get(pos);
                if (o instanceof Ini.Line) edit((Ini.Line) o);
            }
        });
        list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> a, View v, int pos, long id) {
                Object o = rows.get(pos);
                if (o instanceof Ini.Line) menu((Ini.Line) o);
                return true;
            }
        });
        root.addView(list, new LinearLayout.LayoutParams(-1, 0, 1f));

        Button add = new Button(this);
        add.setText(Words.IniEd.ADD_LINE);
        add.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { addLine(); }
        });
        root.addView(add);
        setContentView(TopRow.wrap(this, root));                   // 1.9.0: שורת הכפתורים העליונה
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        ini = Ini.load(file);
        rebuild();
    }

    private void rebuild() {
        rows.clear();
        String section = null;
        for (Ini.Line l : ini.lines) {
            if (l.key == null) continue;
            if (!l.section.equals(section)) {
                section = l.section;
                rows.add(section.isEmpty() ? Words.IniEd.NO_SECTION : section);
            }
            rows.add(l);
        }
        int unknown = 0;
        for (Ini.Line l : ini.lines) {
            String t = l.raw.trim();
            if (l.key == null && !t.isEmpty() && !t.startsWith(";") && !t.startsWith("#") && !l.isHeader())
                unknown++;
        }
        info.setText(Paths.relative(file)
                + (readOnly ? Words.IniEd.COPY_NOTE
                        + Paths.structure().getName() + "\"." : "")
                + (file.equals(Paths.structure()) ? Words.IniEd.ID_LINES_NOTE : "")
                + (unknown > 0 ? "\n" + unknown + Words.IniEd.UNKNOWN_KEPT : ""));
        adapter.notifyDataSetChanged();
    }

    private void save() {
        if (ini.changedOutside()) {
            Ui.options(this, Words.IniEd.CHANGED_OUTSIDE, new String[]{Words.IniEd.RELOAD, Words.IniEd.OVERWRITE},
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface d, int w) {
                            if (w == 0) load(); else write();
                        }
                    });
            return;
        }
        write();
    }

    private void write() {
        if (ini.saveQuiet()) {
            if (file.equals(Paths.structure())) Mirror.rebuild();
            rebuild();
        } else {
            Ui.toast(this, Words.IniEd.SAVE_FAILED_LOG);
        }
    }

    private boolean locked(Ini.Line l) {
        if (readOnly) {
            Ui.toast(this, Words.IniEd.READ_ONLY_COPY);
            return true;
        }
        if (Structure.ID.equals(l.key) && file.equals(Paths.structure())) {
            Ui.toast(this, Words.IniEd.ID_NOT_EDITABLE);
            return true;
        }
        return false;
    }

    private void edit(final Ini.Line l) {
        if (locked(l)) return;
        LinearLayout b = Ui.form(this);
        final EditText key = Ui.field(this, l.key, null);
        final EditText value = Ui.field(this, l.value, null);
        b.addView(Ui.label(this, Words.IniEd.KEY));
        b.addView(key);
        b.addView(Ui.label(this, Words.IniEd.VALUE));
        b.addView(value);
        Ui.askForm(this, Words.IniEd.EDIT_LINE, b, new Runnable() {
            @Override
            public void run() {
                String k = key.getText().toString().trim();
                if (k.isEmpty()) {
                    Ui.toast(IniEditScreen.this, Words.NEED_NAME);
                    return;
                }
                ini.set(l, k, value.getText().toString().trim());
                save();
            }
        });
    }

    private void menu(final Ini.Line l) {
        if (locked(l)) return;
        Ui.options(this, l.key, new String[]{Words.IniEd.EDIT, Words.IniEd.MOVE_UP, Words.IniEd.MOVE_DOWN, Words.IniEd.DELETE_LINE},
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        if (which == 0) edit(l);
                        else if (which == 3) {
                            ini.remove(l);
                            save();
                        } else if (ini.move(l, which == 1 ? -1 : 1)) save();
                    }
                });
    }

    private void addLine() {
        if (readOnly) {
            Ui.toast(this, Words.IniEd.READ_ONLY_COPY);
            return;
        }
        final List<String> sections = ini.sections();
        if (sections.isEmpty()) sections.add("");
        if (sections.size() == 1) {
            addTo(sections.get(0));
            return;
        }
        Ui.list(this, Words.IniEd.WHICH_SECTION, sections, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int which) { addTo(sections.get(which)); }
        });
    }

    private void addTo(String section) {
        Ini.Line l = ini.add(section, Words.IniEd.NEW_NAME, "");
        edit(l);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, Words.IniEd.EDIT_BY_HAND);
        menu.add(0, 2, 1, Words.IniEd.RELOAD);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 1) {
            Ui.editText(this, file);
            return true;
        }
        if (item.getItemId() == 2) {
            load();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
