package com.tikiyot.vfm;

// קבלת קבצים ב"שיתוף" והוספתם לרשימה.

import android.app.Activity;
import android.content.ClipData;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ShareScreen extends Activity {

    private static final class Row {
        ArrayList<String> path;
        String label, sub;
    }

    private final List<String> files = new ArrayList<String>();
    private final List<Row> rows = new ArrayList<Row>();
    private BaseAdapter adapter;
    private TextView status;
    private ListView list;
    private Button newFolder;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        Log.install();
        setTitle(Words.ADD_TO_LIST);
        setFinishOnTouchOutside(false);

        LinearLayout root = Ui.screen(this);
        status = Ui.info(this);
        status.setTextSize(15);
        status.setTextColor(Ui.TEXT);
        root.addView(status);

        list = new ListView(this);
        list.setVisibility(View.GONE);
        adapter = new BaseAdapter() {
            @Override public int getCount() { return rows.size(); }
            @Override public Object getItem(int i) { return rows.get(i); }
            @Override public long getItemId(int i) { return i; }

            @Override
            public View getView(int pos, View convert, ViewGroup parent) {
                View v = Ui.row(ShareScreen.this, convert);
                Row r = rows.get(pos);
                Ui.bindIcon(v, "📋", r.label, r.sub);
                return v;
            }
        };
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> a, View v, int pos, long id) {
                addTo(rows.get(pos).path);
            }
        });
        root.addView(list, new LinearLayout.LayoutParams(-1, 0, 1f));

        LinearLayout bar = new LinearLayout(this);
        bar.setLayoutDirection(Ui.DIRECTION);
        newFolder = new Button(this);
        newFolder.setText("➕ " + Words.NEW_LIST);
        newFolder.setVisibility(View.GONE);
        newFolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final java.util.List<ArrayList<String>> bases =
                        new ArrayList<ArrayList<String>>(FnVirtual.allBases().keySet());
                final java.util.List<String> names = new ArrayList<String>(FnVirtual.allBases().values());
                if (bases.isEmpty()) return;
                if (bases.size() == 1) {
                    createFolder(bases.get(0));
                    return;
                }
                Ui.list(ShareScreen.this, Words.NEW_LIST + Words.ShareIn.INSIDE, names,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int w) { createFolder(bases.get(w)); }
                        });
            }
        });
        Button cancel = new Button(this);
        cancel.setText(Words.CANCEL);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });
        bar.addView(newFolder, new LinearLayout.LayoutParams(0, -2, 2f));
        bar.addView(cancel, new LinearLayout.LayoutParams(0, -2, 1f));
        root.addView(bar);
        setContentView(root);                                          // 1.9.0: חלון השיתוף הנכנס — בלי השורה העליונה (חלון קטן, במשימה נפרדת)

        DisplayMetrics dm = getResources().getDisplayMetrics();
        if (getWindow() != null)
            getWindow().setLayout((int) (dm.widthPixels * 0.92), (int) (dm.heightPixels * 0.8));

        if (!Ui.hasFiles(this)) {
            status.setText(Words.ShareIn.NEED_APP_FIRST);
            return;
        }
        resolve();
    }

    private void resolve() {
        List<Uri> uris = new ArrayList<Uri>();
        Intent in = getIntent();
        if (in != null) {
            if (Intent.ACTION_SEND.equals(in.getAction())) {
                Uri u = in.getParcelableExtra(Intent.EXTRA_STREAM);
                if (u != null) uris.add(u);
            } else if (Intent.ACTION_SEND_MULTIPLE.equals(in.getAction())) {
                ArrayList<Uri> l = in.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                if (l != null) uris.addAll(l);
            }
            if (uris.isEmpty() && in.getClipData() != null) {
                ClipData cd = in.getClipData();
                for (int i = 0; i < cd.getItemCount(); i++)
                    if (cd.getItemAt(i).getUri() != null) uris.add(cd.getItemAt(i).getUri());
            }
        }
        if (uris.isEmpty()) {
            status.setText(Words.ShareIn.NO_FILES_RECEIVED);
            return;
        }
        status.setText(Words.ShareIn.IDENTIFYING + uris.size() + Words.ShareIn.FILES_DOTS);
        final List<Uri> all = uris;
        new Thread(new Runnable() {
            @Override
            public void run() {
                int bad = 0;
                for (Uri u : all) {
                    String p = Resolver.path(ShareScreen.this, u);
                    if (p == null) {
                        bad++;
                        Log.i("שיתוף: לא זוהה מסלול · " + u);
                    } else if (!files.contains(p)) {
                        files.add(p);
                    }
                }
                final int unresolved = bad;
                runOnUiThread(new Runnable() {
                    @Override public void run() { ready(unresolved); }
                });
            }
        }).start();
    }

    private void ready(int unresolved) {
        if (files.isEmpty()) {
            status.setText(Words.ShareIn.CANT_LOCATE);
            return;
        }
        status.setText(Words.ShareIn.ADD_N + files.size() + Words.ShareIn.FILES_TO_LIST + Words.NO_TOUCH
                + (unresolved > 0 ? "\n(" + unresolved + Words.ShareIn.NOT_IDENTIFIED : ""));
        // 126: הבורר האחד (תיקיות / עץ, חיפוש, תצוגה אחרונה) — במקום שורות משלו
        if (FnVirtual.allBases().isEmpty()) {
            status.setText(Words.ShareIn.NO_LISTS_YET);
            return;
        }
        list.setVisibility(View.GONE);
        newFolder.setVisibility(View.VISIBLE);
        newFolder.setText(Words.ShareIn.PICK_LIST);
        newFolder.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { pick(); }
        });
        pick();
    }

    private void pick() {
        ListPicker.choose(this, Words.ADD_TO_LIST, null, new ListPicker.Picked() {
            @Override public void ok(ArrayList<String> path) { addTo(path); }
        });
    }

    /** רשימות של פונקציות קיימות בלבד, לפי שם הפונקציה. בלי השורש, שאין אליו כניסה. */
    private void buildRows() {
        rows.clear();
        Tree t = Tree.load();
        for (java.util.Map.Entry<ArrayList<String>, String> e : FnVirtual.allBases().entrySet()) {
            Tree.Node n = t.find(e.getKey());
            if (n == null) continue;
            Row r = new Row();
            r.path = n.path();
            r.label = e.getValue();
            r.sub = n.entries.size() + Words.ShareIn.ITEMS_SUFFIX;
            rows.add(r);
            flatten(n, 1);
        }
        if (rows.isEmpty()) {
            status.setText(Words.ShareIn.NO_LISTS_YET);
            list.setVisibility(View.GONE);
            newFolder.setVisibility(View.GONE);
        }
        adapter.notifyDataSetChanged();
    }

    private void flatten(Tree.Node n, int depth) {
        for (Tree.Node c : n.children) {
            Row r = new Row();
            r.path = c.path();
            StringBuilder indent = new StringBuilder();
            for (int i = 0; i < depth; i++) indent.append("      ");
            r.label = indent + c.name;
            r.sub = c.entries.size() + Words.ShareIn.ITEMS_SUFFIX;
            rows.add(r);
            flatten(c, depth + 1);
        }
    }

    private void addTo(List<String> path) {
        int added = 0;
        synchronized (Tree.LOCK) {                                   // 92
            Tree t = Tree.load();
            Tree.Node n = t.find(path);
            if (n == null) {
                Ui.toast(this, Words.LIST_NOT_FOUND);
                buildRows();
                return;
            }
            for (String p : files) {
                if (Tree.contains(n.entries, p)) continue;
                n.entries.add(new Tree.Entry(p, new File(p).length(), false));
                added++;
            }
            if (added > 0) t.save();
        }
        Library.get(this).learn(files);           // קובץ ששותף ואינו באינדקס — נוסף מיד
        Ui.toast(getApplicationContext(), added > 0 ? Words.ShareIn.ADDED_N + added + Words.ShareIn.TO_LIST : Words.ShareIn.ALREADY_THERE);
        finish();
    }

    /** רשימה חדשה (החלון האחד — ListPicker.create), והקבצים נכנסים אליה. */
    private void createFolder(final List<String> parent) {
        ListPicker.create(this, new java.util.ArrayList<String>(parent), new PickDialog.Chosen() {
            @Override
            public void ok(java.util.ArrayList<String> path) {
                Tree.addFiles(path, files);
                Library.get(ShareScreen.this).learn(files);
                Ui.toast(getApplicationContext(), Words.ShareIn.LIST_CREATED + path.get(path.size() - 1) + Words.ShareIn.AND_FILES_ADDED);
                finish();
            }
        });
    }
}
