package com.tikiyot.vfm;

// Store בזיכרון — הבדיקות במחשב. אחראי: אותה התנהגות בדיוק כמו SqlStore (StoreContract מוכיח). אסור לו: להישמר.

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
        Stores.noListInList(it);
        if (l.containsKey(item)) return false;
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
    public void rename(long item, String name) {
        String n = Stores.clean(name, "name");
        Item it = items.get(item);
        if (it == null) throw new Failure("no such item " + item);
        items.put(item, new Item(it.id, it.type, n, it.created, System.currentTimeMillis()));
    }

    @Override
    public void deleteList(long list) {
        listOf(list);
        members.remove(list);
        items.remove(list);
        for (LinkedHashMap<Long, Long> l : members.values()) l.remove(list);
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
