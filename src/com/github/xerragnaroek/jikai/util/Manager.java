
package com.github.xerragnaroek.jikai.util;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.dv8tion.jda.api.entities.Guild;

public abstract class Manager<T> implements Initilizable, Iterable<T> {

	protected final Map<Long, T> impls = Collections.synchronizedMap(new TreeMap<>());
	protected AtomicBoolean init = new AtomicBoolean(false);
	protected Logger log;
	private final String typeName;

	protected abstract T makeNew(long gId);

	protected Manager(Class<T> clazz) {
		this.typeName = clazz.getTypeName();
		log = LoggerFactory.getLogger(this.getClass());

	}

	public T registerNew(Guild g) {
		log.info("Registered new " + typeName + " for guild {}#{}", g.getName(), g.getId());
		return registerNew(g.getIdLong());
	}

	public T registerNew(long id) {
		T newT = makeNew(id);
		impls.put(id, newT);
		return newT;
	}

	public T get(long gId) {
		if (!hasManagerFor(gId)) {
			return registerNew(gId);
		}
		return impls.get(gId);
	}

	public T get(Guild g) {
		return get(g.getIdLong());
	}

	@Override
	public boolean isInitialized() {
		return init.get();
	}

	protected void assertInitialization() {
		if (!init.get()) {
			log.error(typeName + " hasn't been initialized yet!");
			throw new IllegalStateException(typeName + " hasn't been initialized yet!");
		}
	}

	public boolean hasManagerFor(Guild g) {
		return hasManagerFor(g.getIdLong());
	}

	public boolean hasManagerFor(long g) {
		return impls.containsKey(g);
	}

	public void forEach(BiConsumer<Long, ? super T> con) {
		impls.forEach(con);
	}

	public void remove(long id) {
		impls.remove(id);
	}

	public void remove(Guild g) {
		remove(g.getIdLong());
	}

	@Override
	public Iterator<T> iterator() {
		return impls.values().iterator();
	}

	public T getAny() {
		return impls.values().iterator().next();
	}
}
