package com.tikiyot.vfm;

// קבלת קבצים ב"שיתוף" והוספתם לרשימה.

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class ShareScreen extends Activity {

    private static final class Row {
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
        root.addView(list, new LinearLayout.LayoutParams(-1, 0, 1f));

        LinearLayout bar = new LinearLayout(this);
        bar.setLayoutDirection(Ui.DIRECTION);
        newFolder = new Button(this);
        newFolder.setText("➕ " + Words.NEW_LIST);
        newFolder.setVisibility(View.GONE);
        newFolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pick();
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
        if (!DbPicker.on(this)) {                                             // מ0.29: רשימות — רק במסד
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
        if (DbPicker.on(this)) {                                              // מ0.23: עובדים במסד — לרשימה במסד
            DbPicker.choose(this, Words.ADD_TO_LIST, new DbPicker.Picked() {
                @Override
                public void ok(String mode, long id, List<String> path) {
                    Library.get(ShareScreen.this).learn(files);
                    DbPicker.addPicked(ShareScreen.this, mode, id, path, files, new Runnable() {
                        @Override public void run() { finish(); }
                    });
                }
            });
            return;
        }
        Ui.toast(this, Words.DbCmd.NO_DB);
    }

}
