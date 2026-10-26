package com.tikiyot.vfm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** פס חיפוש צף בתחתית המסך. */
public class BarService extends Service {

    private WindowManager wm;
    private WindowManager.LayoutParams lp;
    private Root root;
    private EditText input;
    private TextView info, tab;
    private ListView list;
    private LinearLayout barRow;
    private final Handler h = new Handler(Looper.getMainLooper());
    public static final String EXTRA_PATHS = "paths";
    public static final String EXTRA_LOOSE = "loose";

    private Boolean paths, loose;
    private Library engine;
    private final List<Object> rows = new ArrayList<Object>();
    private String queryNorm = "";
    private BaseAdapter adapter;
    private Runnable pending;
    private boolean active;

    private final Runnable dim = new Runnable() {
        @Override public void run() { if (!active && root != null) root.setAlpha(0.5f); }
    };

    private class Root extends LinearLayout {
        Root() { super(BarService.this); }

        @Override public boolean dispatchKeyEvent(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.KEYCODE_BACK && active) {
                if (e.getAction() == KeyEvent.ACTION_UP) deactivate();
                return true;
            }
            return super.dispatchKeyEvent(e);
        }

        @Override public boolean dispatchTouchEvent(MotionEvent e) {
            if (e.getAction() == MotionEvent.ACTION_OUTSIDE) {
                if (active) deactivate();
                return false;
            }
            wake();
            return super.dispatchTouchEvent(e);
        }
    }

    @Override public IBinder onBind(Intent i) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        engine = Library.get(this);
        notice();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        build();
        new Thread(new Runnable() {
            @Override public void run() {
                if (!engine.ready) engine.load();
                h.post(new Runnable() {
                    @Override public void run() { hint(); }
                });
                if (engine.updateLevel() >= 2) {                                   // 110: גם ברמה 3
                    engine.refresh();
                    h.post(new Runnable() {
                        @Override public void run() { hint(); }
                    });
                }
            }
        }).start();
    }

    @Override
    public int onStartCommand(Intent i, int f, int id) {
        android.content.SharedPreferences sp = getSharedPreferences("vfm", MODE_PRIVATE);
        if (i != null && i.hasExtra(EXTRA_PATHS)) {
            paths = i.getBooleanExtra(EXTRA_PATHS, false);
            loose = i.getBooleanExtra(EXTRA_LOOSE, true);
            sp.edit().putBoolean("barPaths", paths).putBoolean("barLoose", loose).apply();
        } else {                                           // הופעל מחדש על ידי המערכת: ההגדרות השמורות
            paths = sp.getBoolean("barPaths", false);
            loose = sp.getBoolean("barLoose", true);
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        h.removeCallbacksAndMessages(null);
        try { if (root != null) wm.removeView(root); } catch (Throwable ignored) { }
        super.onDestroy();
    }

    private int dp(float v) { return Ui.dp(this, v); }

    private void notice() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel("bar", Words.Bar.BAR_CHANNEL, NotificationManager.IMPORTANCE_MIN);
            if (nm != null) nm.createNotificationChannel(ch);
            b = new Notification.Builder(this, "bar");
        } else {
            b = new Notification.Builder(this);
        }
        int fl = Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0;
        b.setSmallIcon(android.R.drawable.ic_menu_search)
                .setContentTitle(Words.Main.APP_NAME)
                .setContentText(Words.Bar.BAR_ACTIVE)
                .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, Home.class), fl))
                .setOngoing(true);
        startForeground(1, b.build());
    }

    private void hint() {
        input.setHint(engine.ready
                ? Words.Bar.SEARCH_IN + Files.count(engine.totalFiles()) + Words.Bar.FILES_DOTS
                : Words.Bar.LOADING_SHORT);
    }

    private void build() {
        root = new Root();
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(Ui.DIRECTION);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xF7FFFFFF);
        bg.setCornerRadii(new float[]{dp(16), dp(16), dp(16), dp(16), 0, 0, 0, 0});
        bg.setStroke(dp(1), 0x22000000);
        root.setBackground(bg);

        info = new TextView(this);
        info.setTextSize(12);
        info.setTextColor(Ui.SUB);
        info.setPadding(dp(14), dp(8), dp(14), dp(2));
        info.setVisibility(View.GONE);
        root.addView(info);

        list = new ListView(this);
        list.setVisibility(View.GONE);
        adapter = new BaseAdapter() {
            @Override public int getCount() { return rows.size(); }
            @Override public Object getItem(int i) { return rows.get(i); }
            @Override public long getItemId(int i) { return i; }
            @Override public int getViewTypeCount() { return 2; }
            @Override public int getItemViewType(int i) { return rows.get(i) instanceof String ? 1 : 0; }
            @Override public boolean isEnabled(int i) { return !(rows.get(i) instanceof String); }
            @Override public View getView(int pos, View convert, ViewGroup parent) {
                Object o = rows.get(pos);
                if (o instanceof String) return Ui.header(BarService.this, convert, (String) o, true);
                View v = Ui.row(BarService.this, convert);
                Library.Hit hh = (Library.Hit) o;
                Ui.bindFile(v, hh.name(), hh.parentPath(), queryNorm, hh.isDir());
                return v;
            }
        };
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> a, View v, int pos, long id) {
                Object o = rows.get(pos);
                if (!(o instanceof Library.Hit)) return;
                Library.Hit hit = (Library.Hit) o;
                deactivate();
                Ui.open(BarService.this, new java.io.File(hit.path()));
            }
        });
        list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> a, View v, int pos, long id) {
                Object o = rows.get(pos);
                if (o instanceof Library.Hit && !((Library.Hit) o).isDir()) {
                    java.io.File f = new java.io.File(((Library.Hit) o).path());
                    Intent i = Acts.shareIntent(f);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    i.setClass(BarService.this, ShareScreen.class);
                    deactivate();
                    startActivity(i);
                }
                return true;
            }
        });
        root.addView(list, new LinearLayout.LayoutParams(-1, 0));

        barRow = new LinearLayout(this);
        barRow.setOrientation(LinearLayout.HORIZONTAL);
        barRow.setGravity(Gravity.CENTER_VERTICAL);
        barRow.setPadding(dp(8), dp(2), dp(8), dp(2));

        input = new EditText(this);
        input.setHint(Words.Bar.LOADING_SHORT);
        input.setSingleLine(true);
        input.setTextSize(16);
        input.setBackground(null);
        input.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        input.setOnTouchListener(new View.OnTouchListener() {
            @Override public boolean onTouch(View v, MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_DOWN && !active) activate();
                return false;
            }
        });
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { schedule(s.toString()); }
        });
        barRow.addView(input, new LinearLayout.LayoutParams(0, dp(46), 1f));

        TextView clear = small("✕");
        clear.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { input.setText(""); }
        });
        barRow.addView(clear);
        TextView min = small("⌄");
        min.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { minimize(); }
        });
        barRow.addView(min);
        root.addView(barRow);

        tab = new TextView(this);
        tab.setText("🔍");
        tab.setTextSize(20);
        tab.setGravity(Gravity.CENTER);
        tab.setPadding(dp(14), dp(6), dp(14), dp(6));
        tab.setVisibility(View.GONE);
        tab.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { restore(); }
        });
        root.addView(tab);

        int type = Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        lp = new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT, type, rest(), PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.BOTTOM;
        lp.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
        try {
            wm.addView(root, lp);
        } catch (Throwable t) {
            stopSelf();
            return;
        }
        wake();
    }

    private TextView small(String s) {
        TextView b = new TextView(this);
        b.setText(s);
        b.setTextSize(18);
        b.setTextColor(Ui.SUB);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(10), dp(6), dp(10), dp(6));
        return b;
    }

    private static int rest() {
        return WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
    }

    private static int focused() {
        return WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
    }

    private void wake() {
        if (root == null) return;
        root.setAlpha(1f);
        h.removeCallbacks(dim);
        h.postDelayed(dim, 4000);
    }

    private void activate() {
        active = true;
        lp.flags = focused();
        lp.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                | WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE;
        try { wm.updateViewLayout(root, lp); } catch (Throwable ignored) { }
        input.requestFocus();
        h.postDelayed(new Runnable() {
            @Override public void run() {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 100);
        wake();
    }

    private void deactivate() {
        active = false;
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
        input.clearFocus();
        show(false);
        lp.flags = rest();
        lp.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
        try { wm.updateViewLayout(root, lp); } catch (Throwable ignored) { }
        wake();
    }

    private void minimize() {
        if (active) deactivate();
        barRow.setVisibility(View.GONE);
        tab.setVisibility(View.VISIBLE);
        lp.width = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.gravity = Gravity.BOTTOM | Gravity.START;
        try { wm.updateViewLayout(root, lp); } catch (Throwable ignored) { }
    }

    private void restore() {
        tab.setVisibility(View.GONE);
        barRow.setVisibility(View.VISIBLE);
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.gravity = Gravity.BOTTOM;
        try { wm.updateViewLayout(root, lp); } catch (Throwable ignored) { }
        wake();
    }

    private void show(boolean visible) {
        info.setVisibility(visible ? View.VISIBLE : View.GONE);
        list.setVisibility(visible ? View.VISIBLE : View.GONE);
        LinearLayout.LayoutParams p = (LinearLayout.LayoutParams) list.getLayoutParams();
        if (visible) {
            DisplayMetrics dm = getResources().getDisplayMetrics();
            int want = rows.size() * dp(62);
            p.height = Math.max(dp(54), Math.min(want, (int) (dm.heightPixels * 0.5)));
        } else {
            p.height = 0;
        }
        list.setLayoutParams(p);
    }

    private void schedule(final String q) {
        if (pending != null) h.removeCallbacks(pending);
        pending = new Runnable() {
            @Override public void run() { search(q); }
        };
        h.postDelayed(pending, 30);
    }

    private void search(String q) {
        if (q.trim().isEmpty()) {
            rows.clear();
            adapter.notifyDataSetChanged();
            show(false);
            return;
        }
        if (!engine.ready) {
            info.setText(Words.LOADING_INDEX);
            show(true);
            h.postDelayed(new Runnable() {
                @Override public void run() { search(input.getText().toString()); }
            }, 400);
            return;
        }
        Library.Result r = engine.search(q, paths != null && paths, loose == null || loose);
        queryNorm = r.query;
        rows.clear();
        int section = -1;
        for (Library.Hit hit : r.hits) {
            if (hit.section != section) {
                section = hit.section;
                if (section > 0) rows.add(Library.header(section, r));
            }
            rows.add(hit);
        }
        adapter.notifyDataSetChanged();
        list.setSelection(0);
        if (r.loading) {
            info.setText(Words.Bar.LOADING_PARTIAL);
            h.postDelayed(new Runnable() {
                @Override public void run() { search(input.getText().toString()); }
            }, 600);
        } else {
            info.setText(String.format(Locale.US, Words.Bar.RESULTS_FORMAT, Files.count(r.total), r.ms)
                    + (r.note.isEmpty() ? "" : " · " + r.note));
        }
        show(true);
        wake();
    }
}
