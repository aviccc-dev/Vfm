package com.tikiyot.vfm;

// רשימת קובצי ההגדרות, כולל שבתוך תיקיות.

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class IniListScreen extends Activity {

    private final List<File> files = new ArrayList<File>();
    private BaseAdapter adapter;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        setTitle(Words.IniList.TITLE);
        LinearLayout root = Ui.screen(this);
        TextView hint = Ui.info(this);
        hint.setText(Words.IniList.ABOUT);
        root.addView(hint);

        ListView list = new ListView(this);
        adapter = new BaseAdapter() {
            @Override public int getCount() { return files.size(); }
            @Override public Object getItem(int i) { return files.get(i); }
            @Override public long getItemId(int i) { return i; }

            @Override
            public View getView(int pos, View convert, ViewGroup parent) {
                View v = Ui.row(IniListScreen.this, convert);
                File f = files.get(pos);
                Ui.bindIcon(v, f.equals(Paths.structure()) ? "⭐" : "📄", Paths.relative(f),
                        f.exists() ? Files.size(f.length()) + " · " + Files.when(f.lastModified())
                                : Words.IniList.MISSING_TAP_CREATES);
                return v;
            }
        };
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> a, View v, int pos, long id) {
                File f = files.get(pos);
                if (f.equals(Paths.structure())) Structure.ini();
                Intent i = new Intent(IniListScreen.this, IniEditScreen.class);
                i.putExtra(IniEditScreen.EXTRA_FILE, f.getAbsolutePath());
                startActivity(i);
            }
        });
        root.addView(list, new LinearLayout.LayoutParams(-1, 0, 1f));
        setContentView(TopRow.wrap(this, root));                   // 1.9.0: שורת הכפתורים העליונה
    }

    @Override
    protected void onResume() {
        super.onResume();
        files.clear();
        files.add(Paths.structure());
        collect(Paths.settings());
        adapter.notifyDataSetChanged();
    }

    private void collect(File dir) {
        File[] all = dir.listFiles();
        if (all == null) return;
        for (File f : all) {
            if (f.isDirectory()) collect(f);
            else if (f.getName().endsWith(".ini") && !contains(f)) files.add(f);
        }
    }

    private boolean contains(File f) {
        for (File x : files) if (x.getAbsolutePath().equals(f.getAbsolutePath())) return true;
        return false;
    }
}
