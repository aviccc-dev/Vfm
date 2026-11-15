package android.content;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

// תחליף לבדיקות על המחשב (מבנה 71): העדפות בזיכרון.
public class MemPrefs implements SharedPreferences, SharedPreferences.Editor {
    private final Map<String, Object> m = new HashMap<String, Object>();

    @Override public Map<String, ?> getAll() { return new HashMap<String, Object>(m); }
    @Override public String getString(String k, String d) { Object v = m.get(k); return v instanceof String ? (String) v : d; }
    @SuppressWarnings("unchecked")
    @Override public Set<String> getStringSet(String k, Set<String> d) { Object v = m.get(k); return v instanceof Set ? (Set<String>) v : d; }
    @Override public int getInt(String k, int d) { Object v = m.get(k); return v instanceof Integer ? (Integer) v : d; }
    @Override public long getLong(String k, long d) { Object v = m.get(k); return v instanceof Long ? (Long) v : d; }
    @Override public float getFloat(String k, float d) { Object v = m.get(k); return v instanceof Float ? (Float) v : d; }
    @Override public boolean getBoolean(String k, boolean d) { Object v = m.get(k); return v instanceof Boolean ? (Boolean) v : d; }
    @Override public boolean contains(String k) { return m.containsKey(k); }
    @Override public Editor edit() { return this; }
    @Override public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l) { }
    @Override public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l) { }

    @Override public Editor putString(String k, String v) { m.put(k, v); return this; }
    @Override public Editor putStringSet(String k, Set<String> v) { m.put(k, v); return this; }
    @Override public Editor putInt(String k, int v) { m.put(k, v); return this; }
    @Override public Editor putLong(String k, long v) { m.put(k, v); return this; }
    @Override public Editor putFloat(String k, float v) { m.put(k, v); return this; }
    @Override public Editor putBoolean(String k, boolean v) { m.put(k, v); return this; }
    @Override public Editor remove(String k) { m.remove(k); return this; }
    @Override public Editor clear() { m.clear(); return this; }
    @Override public boolean commit() { return true; }
    @Override public void apply() { }
}
