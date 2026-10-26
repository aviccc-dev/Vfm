package com.tikiyot.vfm;

// גישה לקבצים עבור אפליקציות אחרות. אחראי: להעביר קובץ שנבחר. אסור לו: לחשוף מסלולים אחרים.

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;

public class Provider extends ContentProvider {

    public static final String AUTHORITY = "com.tikiyot.vfm.files";

    public static Uri uriFor(File f) {
        return new Uri.Builder().scheme("content").authority(AUTHORITY).path(f.getAbsolutePath()).build();
    }

    private static File fileOf(Uri uri) {
        String p = uri.getPath();
        if (p == null) return null;
        return (p.startsWith("/storage/") || p.startsWith("/sdcard") || p.startsWith("/mnt/")
                || p.startsWith("/data/media/")) ? new File(p) : null;
    }

    @Override public boolean onCreate() { return true; }


    @Override
    public String getType(Uri uri) {
        String m = Ui.mime(String.valueOf(uri.getLastPathSegment()));
        return m != null ? m : "application/octet-stream";
    }

    @Override
    public Cursor query(Uri uri, String[] proj, String sel, String[] args, String sort) {
        File f = fileOf(uri);
        if (f == null) return null;
        String[] cols = proj != null ? proj
                : new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE};
        Object[] row = new Object[cols.length];
        for (int i = 0; i < cols.length; i++) {
            if (OpenableColumns.DISPLAY_NAME.equals(cols[i])) row[i] = f.getName();
            else if (OpenableColumns.SIZE.equals(cols[i])) row[i] = f.length();
        }
        MatrixCursor c = new MatrixCursor(cols, 1);
        c.addRow(row);
        return c;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        File f = fileOf(uri);
        if (f == null) throw new FileNotFoundException(String.valueOf(uri));
        return ParcelFileDescriptor.open(f, ParcelFileDescriptor.parseMode(mode));
    }

    @Override public Uri insert(Uri uri, ContentValues v) { return null; }
    @Override public int delete(Uri uri, String s, String[] a) { return 0; }
    @Override public int update(Uri uri, ContentValues v, String s, String[] a) { return 0; }
}
