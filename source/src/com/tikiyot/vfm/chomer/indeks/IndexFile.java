package com.tikiyot.vfm;

// מבנה 187 (סידור קומת החומר): הקובץ של האינדקס — הצורה שבה אינדקס של כונן נשמר ונטען (גרסאות 1–3). עד כאן בתוך Index.
// אחראי: לכתוב את האינדקס לקובץ שלו (לשם זמני, ורק בסוף לשם הסופי) ולקרוא אותו. אסור לו: לשנות את האינדקס שהוא שומר.
// הכניסות נשארו ב־Index (save, load).

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

final class IndexFile {

    private IndexFile() { }

    private static final int MAGIC = 0x56464D31;   // VFM1
    private static final int VERSION = 3;          // 189: "נוסף"; 190: חתימת תוכן (גרסאות 1–2 — נקראות, בלי מה שחסר בהן)

    /**
     * 184: שמירה בלי לשנות את האינדקס — מה שנעלם פשוט לא נכתב, והמזהים מכווצים בדרך. כך אפשר לשמור
     * את התמונה המוצגת עצמה, בלי להעתיק קודם את כל האינדקס (במיליון קבצים — כפל זיכרון בכל שמירה).
     */
    static void save(Index ix) throws IOException {
        int nDirs = ix.dirs.size();
        int[] map = new int[nDirs];
        int keptDirs = 0;
        for (int i = 0; i < nDirs; i++) {
            Index.Dir x = ix.dirs.get(i);
            boolean lost = x.gone || (x.parent >= 0 && (x.parent >= i || map[x.parent] < 0));
            map[i] = lost ? -2 : keptDirs++;
        }
        int keptFiles = 0;
        for (Index.Rec r : ix.files) if (kept(r, map)) keptFiles++;
        File target = Paths.index(ix.key);
        File tmp = new File(target.getParentFile(), "." + target.getName() + "." + System.nanoTime() + ".tmp");
        FileOutputStream fos = new FileOutputStream(tmp);
        DataOutputStream d = new DataOutputStream(new BufferedOutputStream(fos, 1 << 16));
        try {
            d.writeInt(MAGIC);
            d.writeInt(VERSION);
            d.writeUTF(ix.rootPath);
            d.writeUTF(ix.label);
            d.writeLong(ix.built);
            d.writeInt(keptDirs);
            for (int i = 0; i < nDirs; i++) {
                if (map[i] < 0) continue;
                Index.Dir x = ix.dirs.get(i);
                d.writeInt(x.parent < 0 ? x.parent : map[x.parent]);
                d.writeLong(x.mtime);
                d.writeUTF(x.name);
            }
            d.writeInt(keptFiles);
            for (Index.Rec r : ix.files) {
                if (!kept(r, map)) continue;
                d.writeInt(r.dir < 0 ? r.dir : map[r.dir]);
                d.writeLong(r.size);
                d.writeLong(r.mtime);
                d.writeLong(r.added);
                d.writeLong(r.sig);
                d.writeUTF(r.name);
            }
            d.flush();
            fos.getFD().sync();
        } finally {
            d.close();
        }
        if (!tmp.renameTo(target)) {
            target.delete();
            if (!tmp.renameTo(target)) throw new IOException(Words.Idx.CANT_SAVE_INDEX);
        }
    }

    private static boolean kept(Index.Rec r, int[] map) {
        return !r.gone && (r.dir < 0 || (r.dir < map.length && map[r.dir] >= 0));
    }

    /** null אם אין קובץ, אם הגרסה שונה או אם הוא פגום. */
    static Index load(String key) {
        File f = Paths.index(key);
        if (!f.exists()) return null;
        DataInputStream d = null;
        try {
            d = new DataInputStream(new BufferedInputStream(new FileInputStream(f), 1 << 16));
            if (d.readInt() != MAGIC) return null;
            int version = d.readInt();
            if (version < 1 || version > VERSION) return null;
            Index ix = new Index(key, "", d.readUTF());
            ix.noAdded = version < 2;
            ix.label = d.readUTF();
            ix.built = d.readLong();
            int nd = d.readInt();
            ix.dirs.ensureCapacity(nd);
            for (int i = 0; i < nd; i++) {
                Index.Dir x = new Index.Dir();
                x.parent = d.readInt();
                x.mtime = d.readLong();
                x.name = d.readUTF();
                x.trash = x.parent < 0 ? x.name.equals(Index.TRASH)
                        : x.parent < ix.dirs.size() && ix.dirs.get(x.parent).trash;
                x.hidden = x.name.startsWith(".")
                        || (x.parent >= 0 && x.parent < ix.dirs.size() && ix.dirs.get(x.parent).hidden);
                ix.dirs.add(x);
            }
            int nf = d.readInt();
            ix.files.ensureCapacity(nf);
            for (int i = 0; i < nf; i++) {
                Index.Rec r = new Index.Rec();
                r.dir = d.readInt();
                r.size = d.readLong();
                r.mtime = d.readLong();
                if (version >= 2) r.added = d.readLong();
                if (version >= 3) r.sig = d.readLong();
                r.name = d.readUTF();
                ix.files.add(r);
            }
            ix.fillNorms();                                         // 184: בכל הליבות
            return ix;
        } catch (Throwable t) {
            Log.e("טעינת אינדקס " + key, t);
            return null;
        } finally {
            if (d != null) try { d.close(); } catch (IOException ignored) { }
        }
    }
}
