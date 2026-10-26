package com.tikiyot.lab;

// שיפור 1 — שאלה אחת אמיתית לכל פריט: Os.lstat מחזיר סוג, גודל ותאריך יחד (אנדרואיד 5 ומעלה).

import android.os.Build;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructStat;

import java.io.File;

final class Fast {

    private Fast() { }

    static Scan.Stat stat() {
        return new Scan.Stat() {
            @Override
            public long[] read(File dir, String name) {
                try {
                    StructStat s = Os.lstat(dir.getPath() + "/" + name);
                    boolean d = OsConstants.S_ISDIR(s.st_mode);
                    long mt = Build.VERSION.SDK_INT >= 27 ? s.st_mtim.tv_sec * 1000 + s.st_mtim.tv_nsec / 1000000 : s.st_mtime * 1000;
                    return new long[]{d ? 1 : 0, d ? 0 : s.st_size, mt};
                } catch (Exception e) {
                    return Scan.THREE.read(dir, name);
                }
            }
        };
    }
}
