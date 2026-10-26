package com.tikiyot.vfm;

// זיהוי מסלול אמיתי מקובץ ששותף. חמש שיטות, לפי הסדר שנבדק.

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;

import java.io.File;
import java.util.ArrayDeque;

public final class Resolver {

    private Resolver() { }

    public static String path(Context ctx, Uri uri) {
        if (uri == null) return null;
        String ext = Environment.getExternalStorageDirectory().getAbsolutePath();

        if ("file".equals(uri.getScheme())) {
            String p = exists(uri.getPath());
            if (p != null) return p;
        }
        String p = exists(query(ctx, uri, "_data"));
        if (p != null) return p;
        p = exists(fromDocument(ctx, uri, ext));
        if (p != null) return p;
        p = exists(fromUriPath(uri, ext));
        if (p != null) return p;

        String name = query(ctx, uri, OpenableColumns.DISPLAY_NAME);
        if (name == null) name = uri.getLastPathSegment();
        long size = -1;
        String s = query(ctx, uri, OpenableColumns.SIZE);
        if (s != null) try {
            size = Long.parseLong(s);
        } catch (NumberFormatException ignored) {
        }
        if (name == null || size <= 0) return null;
        try {                                              // קודם באינדקס: מיידי
            Library lib = Library.get(ctx);
            if (lib.ready) for (Library.Hit h : lib.byName(name, size)) return h.path();
        } catch (Throwable ignored) {
        }
        java.util.List<Vols.Vol> vols = Vols.list(ctx);
        for (int pass = 0; pass < 2; pass++)              // הכרטיס קודם
            for (Vols.Vol v : vols) {
                if ((pass == 0) == v.internal) continue;
                String found = search(v.root, name, size, 6000);
                if (found != null) return found;
            }
        return null;
    }

    private static String exists(String path) {
        return path != null && !path.isEmpty() && new File(path).isFile() ? path : null;
    }

    private static String query(Context ctx, Uri uri, String col) {
        Cursor c = null;
        try {
            c = ctx.getContentResolver().query(uri, new String[]{col}, null, null, null);
            if (c != null && c.moveToFirst()) return c.getString(0);
        } catch (Throwable ignored) {
        } finally {
            if (c != null) c.close();
        }
        return null;
    }

    private static String fromDocument(Context ctx, Uri uri, String ext) {
        try {
            if (!DocumentsContract.isDocumentUri(ctx, uri)) return null;
            String id = DocumentsContract.getDocumentId(uri), auth = uri.getAuthority();
            if ("com.android.externalstorage.documents".equals(auth)) {
                String[] s = id.split(":", 2);
                if (s.length == 2)
                    return "primary".equalsIgnoreCase(s[0]) ? ext + "/" + s[1] : "/storage/" + s[0] + "/" + s[1];
            } else if ("com.android.providers.downloads.documents".equals(auth)) {
                if (id.startsWith("raw:")) return id.substring(4);
            } else if ("com.android.providers.media.documents".equals(auth)) {
                String[] s = id.split(":", 2);
                if (s.length != 2) return null;
                Uri base = "image".equals(s[0]) ? MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        : "video".equals(s[0]) ? MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                        : "audio".equals(s[0]) ? MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                        : MediaStore.Files.getContentUri("external");
                return query(ctx, ContentUris.withAppendedId(base, Long.parseLong(s[1])), "_data");
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static String fromUriPath(Uri uri, String ext) {
        String up = uri.getPath();
        if (up == null) return null;
        int idx = up.indexOf("/storage/");
        if (idx >= 0) return up.substring(idx);
        if (up.contains("/sdcard/")) return ext + up.substring(up.indexOf("/sdcard/") + 7);
        int second = up.indexOf('/', 1);
        if (second > 0 && new File(ext + up.substring(second)).isFile()) return ext + up.substring(second);
        return new File(ext + up).isFile() ? ext + up : null;
    }

    private static String search(File top, String name, long size, long maxMs) {
        long end = System.currentTimeMillis() + maxMs;
        ArrayDeque<File> q = new ArrayDeque<File>();
        q.add(top);
        while (!q.isEmpty() && System.currentTimeMillis() < end) {
            File[] all = q.poll().listFiles();
            if (all == null) continue;
            for (File f : all) {
                if (Vols.skip(f, false)) continue;
                if (f.isDirectory()) q.add(f);
                else if (f.getName().equals(name) && f.length() == size) return f.getAbsolutePath();
            }
        }
        return null;
    }
}
