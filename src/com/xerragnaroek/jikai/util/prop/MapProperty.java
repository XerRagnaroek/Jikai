package com.xerragnaroek.jikai.util.prop;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import com.xerragnaroek.jikai.util.Pair;

public class MapProperty<K, V> implements Map<K, V> {

	private Map<K, V> map;
	private AtomicBoolean changed = new AtomicBoolean(false);
	private Set<BiConsumer<K, Pair<V, V>>> putCon = new HashSet<>();
	private Set<BiConsumer<K, V>> remCon = new HashSet<>();

	public MapProperty() {
		map = new ConcurrentHashMap<>();
	}

	public MapProperty(Map<K, V> m) {
		map = m;
	}

	@Override
	public int size() {
		return map.size();
	}

	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return map.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return map.containsValue(value);
	}

	@Override
	public V get(Object key) {
		return map.get(key);
	}

	@Override
	public V put(K key, V value) {
		return putImpl(key, value);
	}

	@Override
	public V remove(Object key) {
		return removeImpl(key);
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		m.forEach((k, v) -> {
			put(k, v);
		});
	}

	@Override
	public void clear() {
		if (!isEmpty()) {
			Set<K> keys = keySet();
			keys.forEach(k -> remove(k));
		}
	}

	@Override
	public Set<K> keySet() {
		return map.keySet();
	}

	@Override
	public Collection<V> values() {
		return map.values();
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		return map.entrySet();
	}

	private V putImpl(K key, V value) {
		V ov = map.put(key, value);
		if (ov == null || ov.equals(value)) {
			changed.set(true);
			runPutComs(key, Pair.of(ov, value));
		}
		return ov;
	}

	private synchronized void runPutComs(K key, Pair<V, V> values) {
		putCon.forEach(c -> c.accept(key, values));
	}

	private V removeImpl(Object key) {
		V val = map.remove(key);
		if (val != null) {
			changed.set(true);
			runRemComs((K) key, val);
		}
		return val;
	}

	private synchronized void runRemComs(K key, V value) {
		remCon.forEach(c -> c.accept(key, value));
	}

	public boolean hasChanged() {
		boolean b = changed.get();
		if (b) {
			changed.set(false);
		}
		return b;
	}

	public void set(Map<? extends K, ? extends V> map) {
		clear();
		putAll(map);
	}

	public void onPut(BiConsumer<K, Pair<V, V>> con) {
		putCon.add(con);
	}

	public void onRemove(BiConsumer<K, V> con) {
		remCon.add(con);
	}

	public void clearConsumer() {
		putCon.clear();
		remCon.clear();
	}
}
