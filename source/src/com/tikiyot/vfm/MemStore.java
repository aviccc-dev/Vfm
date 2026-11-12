package com.tikiyot.vfm;

// Store בזיכרון — לבדיקות במחשב, ולהדמיה של קובץ פקודות (DbListCmd.copyOf: העותק שהשורות רצות עליו בתכנון).
// אחראי: אותה התנהגות בדיוק כמו SqlStore (StoreContract מוכיח). אסור לו: להישמר.

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class MemStore implements Store {

    private final IdCounter counter;
    private final Map<Long, Item> items = new TreeMap<Long, Item>();
    /** רשימה ← (פריט ← מתי נוסף), בסדר המקומות. */
    private final Map<Long, LinkedHashMap<Long, Long>> members = new TreeMap<Long, LinkedHashMap<Long, Long>>();
    private final Map<Long, FileInfo> files = new TreeMap<Long, FileInfo>();
    private final Map<Long, String> origins = new TreeMap<Long, String>();
    private final Map<Long, Map<String, String>> props = new TreeMap<Long, Map<String, String>>();
    private final String code = Stores.newSystemCode();
    private long next;

    public MemStore(IdCounter counter) {
        this.counter = counter;
        this.next = counter.first(1);
    }

    private long issue() {
        long id = next;
        try {
            counter.reserve(id);
        } catch (IOException e) {
            throw new Failure("counter file", e);
        }
        next++;
        return id;
    }

    private Item make(int type, String name) {
        long now = System.currentTimeMillis();
        Item it = new Item(issue(), type, name, now, now);
        items.put(it.id, it);
        if (type == LIST) members.put(it.id, new LinkedHashMap<Long, Long>());
        return it;
    }

    private LinkedHashMap<Long, Long> listOf(long id) {
        Stores.mustBeList(items.get(id), id);
        return members.get(id);
    }

    @Override
    public long createList(String name, long parent) {
        String n = Stores.clean(name, "name");
        LinkedHashMap<Long, Long> p = parent == 0 ? null : listOf(parent);
        Item it = make(LIST, n);
        if (p != null) p.put(it.id, it.created);
        return it.id;
    }

    @Override
    public long createSmart(String name, long parent) {
        String n = Stores.clean(name, "name");
        LinkedHashMap<Long, Long> p = parent == 0 ? null : listOf(parent);
        Item it = make(SMART, n);
        if (p != null) p.put(it.id, it.created);
        return it.id;
    }

    @Override
    public long addTitle(long list, String text) {
        String t = Stores.clean(text, "text");
        LinkedHashMap<Long, Long> l = listOf(list);
        Item it = make(TITLE, t);
        l.put(it.id, it.created);
        return it.id;
    }

    @Override
    public long addNote(long list, String text) {
        String t = Stores.clean(text, "text");
        LinkedHashMap<Long, Long> l = listOf(list);
        Item it = make(NOTE, t);
        l.put(it.id, it.created);
        return it.id;
    }

    @Override
    public boolean addMember(long list, long item) {
        LinkedHashMap<Long, Long> l = listOf(list);
        Item it = items.get(item);
        if (it == null) throw new Failure("no such item " + item);
        if (prop(item, P_TRASHED) != null) throw new Failure("item is in the bin: " + item);
        if (l.containsKey(item)) return false;
        if (makesCycle(list, item)) throw new Failure("cycle: " + item + " already holds " + list);
        l.put(item, System.currentTimeMillis());
        return true;
    }

    @Override
    public boolean removeMember(long list, long item) {
        return listOf(list).remove(item) != null;
    }

    @Override
    public void move(long list, long item, int position) {
        LinkedHashMap<Long, Long> l = listOf(list);
        Long added = l.get(item);
        if (added == null) throw new Failure("not a member");
        List<Long> order = new ArrayList<Long>(l.keySet());
        order.remove(item);
        order.add(Math.max(0, Math.min(position, order.size())), item);
        Map<Long, Long> old = new LinkedHashMap<Long, Long>(l);
        l.clear();
        for (Long id : order) l.put(id, old.get(id));
    }

    @Override
    public boolean contains(long outer, long inner) {
        java.util.Set<Long> seen = new java.util.HashSet<Long>();
        java.util.ArrayDeque<Long> q = new java.util.ArrayDeque<Long>();
        q.add(outer);
        while (!q.isEmpty()) {
            LinkedHashMap<Long, Long> l = members.get(q.poll());
            if (l == null) continue;
            for (Long id : l.keySet()) {
                if (id == inner) return true;
                if (seen.add(id)) q.add(id);
            }
        }
        return false;
    }

    @Override
    public boolean makesCycle(long list, long item) {
        return list == item || contains(item, list);
    }

    @Override
    public void trash(long item) {
        Item it = items.get(item);
        Stores.mayTrash(it, item);
        if (prop(item, P_TRASHED) != null) return;
        List<long[]> from = new ArrayList<long[]>();
        for (Map.Entry<Long, LinkedHashMap<Long, Long>> e : members.entrySet()) {
            int pos = 0;
            for (Long id : e.getValue().keySet()) {
                if (id == item) from.add(new long[]{e.getKey(), pos});
                pos++;
            }
        }
        for (long[] f : from) members.get(f[0]).remove(item);
        setProp(item, P_TRASH_FROM, Stores.joinFrom(from));
        setProp(item, P_TRASHED, String.valueOf(System.currentTimeMillis()));
    }

    @Override
    public void restore(long item) {
        if (items.get(item) == null) throw new Failure("no such item " + item);
        if (prop(item, P_TRASHED) == null) throw new Failure("not in the bin: " + item);
        List<long[]> from = Stores.parseFrom(prop(item, P_TRASH_FROM));
        setProp(item, P_TRASHED, null);
        setProp(item, P_TRASH_FROM, null);
        for (long[] f : from) {
            Item l = items.get(f[0]);
            if (l == null || l.type != LIST) continue;
            if (addMember(f[0], item)) move(f[0], item, (int) f[1]);
        }
    }

    @Override
    public void purge(long item) {
        Item it = items.get(item);
        if (it == null) throw new Failure("no such item " + item);
        boolean loose = it.type == FILE;                                     // מ0.41: קובץ שאינו באף רשימה
        if (loose) for (LinkedHashMap<Long, Long> l : members.values()) if (l.containsKey(item)) { loose = false; break; }
        if (prop(item, P_TRASHED) == null && !loose) throw new Failure("not in the bin: " + item);
        items.remove(item);
        props.remove(item);
        origins.remove(item);
        files.remove(item);                                                  // מ0.23: כמו המסד (ON DELETE CASCADE)
        for (LinkedHashMap<Long, Long> l : members.values()) l.remove(item);
    }

    @Override
    public List<Item> trashed() {
        List<Item> out = new ArrayList<Item>();
        for (Item it : items.values()) if (prop(it.id, P_TRASHED) != null) out.add(it);
        return out;
    }

    @Override
    public void rename(long item, String name) {
        String n = Stores.clean(name, "name");
        Item it = items.get(item);
        if (it == null) throw new Failure("no such item " + item);
        items.put(item, new Item(it.id, it.type, n, it.created, System.currentTimeMillis()));
    }

    @Override
    public void deleteList(long list) {
        Stores.mustBeListOrSmart(items.get(list), list);
        members.remove(list);
        items.remove(list);
        props.remove(list);
        origins.remove(list);
        for (LinkedHashMap<Long, Long> l : members.values()) l.remove(list);
    }

    @Override
    public long createFile(FileInfo f) {
        Stores.checkFile(f);
        if (findFile(f.path) != 0) throw new Failure("file already exists: " + f.path);
        Item it = make(FILE, Stores.fileName(f.path));
        files.put(it.id, f);
        return it.id;
    }

    @Override
    public FileInfo file(long item) {
        return files.get(item);
    }

    @Override
    public void updateFile(long item, long size, long mtime, String sig) {
        FileInfo f = files.get(item);
        if (f == null) throw new Failure("not a file: " + item);
        files.put(item, new FileInfo(f.path, f.drive, size, mtime, sig));
        touch(item);
    }

    @Override
    public void moveFile(long item, String path, String drive) {
        FileInfo f = files.get(item);
        if (f == null) throw new Failure("not a file: " + item);
        FileInfo n = new FileInfo(path, drive, f.size, f.mtime, f.sig);
        Stores.checkFile(n);
        long had = findFile(path);
        if (had != 0 && had != item) throw new Failure("file already exists: " + path);
        files.put(item, n);
        Item it = items.get(item);
        items.put(item, new Item(it.id, it.type, Stores.fileName(path), it.created, System.currentTimeMillis()));
    }

    @Override
    public long findFile(String path) {
        for (Map.Entry<Long, FileInfo> e : files.entrySet()) if (e.getValue().path.equals(path)) return e.getKey();
        return 0;
    }

    @Override
    public long findByOrigin(String key) {
        for (Map.Entry<Long, String> e : origins.entrySet()) if (e.getValue().equals(key)) return e.getKey();
        return 0;
    }

    @Override
    public void setOrigin(long item, String key) {
        if (items.get(item) == null) throw new Failure("no such item " + item);
        origins.put(item, Stores.clean(key, "origin"));
    }

    @Override
    public String origin(long item) {
        return origins.get(item);
    }

    @Override
    public String prop(long item, String key) {
        Map<String, String> m = props.get(item);
        return m == null ? null : m.get(key);
    }

    @Override
    public void setProp(long item, String key, String value) {
        if (items.get(item) == null) throw new Failure("no such item " + item);
        Stores.clean(key, "key");
        Map<String, String> m = props.get(item);
        if (value == null || value.isEmpty()) {
            if (m != null) m.remove(key);
            return;
        }
        if (m == null) props.put(item, m = new java.util.HashMap<String, String>());
        m.put(key, value);
        touch(item);
    }

    private void touch(long id) {
        Item it = items.get(id);
        items.put(id, new Item(it.id, it.type, it.name, it.created, System.currentTimeMillis()));
    }

    @Override
    public void atomic(Work w, boolean keep) {
        Map<Long, Item> i0 = new TreeMap<Long, Item>(items);
        Map<Long, LinkedHashMap<Long, Long>> m0 = new TreeMap<Long, LinkedHashMap<Long, Long>>();
        for (Map.Entry<Long, LinkedHashMap<Long, Long>> e : members.entrySet())
            m0.put(e.getKey(), new LinkedHashMap<Long, Long>(e.getValue()));
        Map<Long, FileInfo> f0 = new TreeMap<Long, FileInfo>(files);
        Map<Long, String> o0 = new TreeMap<Long, String>(origins);
        Map<Long, Map<String, String>> p0 = new TreeMap<Long, Map<String, String>>();
        for (Map.Entry<Long, Map<String, String>> e : props.entrySet())
            p0.put(e.getKey(), new java.util.HashMap<String, String>(e.getValue()));
        long n0 = next;
        boolean ok = false;
        try {
            w.run(this);
            ok = keep;
        } finally {
            if (!ok) {
                items.clear(); items.putAll(i0);
                members.clear(); members.putAll(m0);
                files.clear(); files.putAll(f0);
                origins.clear(); origins.putAll(o0);
                props.clear(); props.putAll(p0);
                next = n0;
            }
        }
    }

    @Override
    public Item item(long id) {
        return items.get(id);
    }

    @Override
    public List<Item> lists() {
        List<Item> out = new ArrayList<Item>();
        for (Item it : items.values()) if (it.type == LIST) out.add(it);
        return out;
    }

    @Override
    public List<Item> smarts() {
        List<Item> out = new ArrayList<Item>();
        for (Item it : items.values()) if (it.type == SMART) out.add(it);
        return out;
    }

    @Override
    public List<Item> unassigned() {
        List<Item> out = new ArrayList<Item>();
        for (Item it : items.values()) {
            if (it.type != FILE && it.type != NOTE && it.type != TITLE) continue;
            if (prop(it.id, P_TRASHED) != null) continue;
            boolean in = false;
            for (LinkedHashMap<Long, Long> l : members.values()) if (l.containsKey(it.id)) { in = true; break; }
            if (!in) out.add(it);
        }
        return out;
    }

    @Override
    public List<Member> members(long list) {
        LinkedHashMap<Long, Long> l = listOf(list);
        List<Member> out = new ArrayList<Member>();
        int pos = 0;
        for (Map.Entry<Long, Long> e : l.entrySet()) out.add(new Member(items.get(e.getKey()), pos++, e.getValue()));
        return out;
    }

    @Override
    public int count(long list) {
        return listOf(list).size();
    }

    @Override
    public List<Long> listsOf(long item) {
        List<Long> out = new ArrayList<Long>();
        for (Map.Entry<Long, LinkedHashMap<Long, Long>> e : members.entrySet())
            if (e.getValue().containsKey(item)) out.add(e.getKey());
        return out;
    }

    @Override
    public long lastId() {
        return next - 1;
    }

    @Override
    public String systemCode() {
        return code;
    }

    @Override
    public String check() {
        return null;
    }

    @Override
    public void close() { }
}
