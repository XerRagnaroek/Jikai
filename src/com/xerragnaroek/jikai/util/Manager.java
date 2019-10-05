package com.xerragnaroek.jikai.util;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.dv8tion.jda.api.entities.Guild;

public abstract class Manager<T> implements Initilizable {

	protected final Map<String, T> impls = Collections.synchronizedMap(new TreeMap<>());
	protected AtomicBoolean init = new AtomicBoolean(false);
	protected Logger log;
	private final String typeName;

	protected abstract T makeNew(String gId);

	protected Manager(Class<T> clazz) {
		this.typeName = clazz.getTypeName();
		log = LoggerFactory.getLogger(this.getClass());

	}

	public T registerNew(Guild g) {
		log.info("Registered new " + typeName + " for guild {}#{}", g.getName(), g.getId());
		return registerNew(g.getId());
	}

	public T registerNew(String id) {
		T newT = makeNew(id);
		impls.put(id, newT);
		return newT;
	}

	public T get(String gId) {
		return impls.get(gId);
	}

	public T get(Guild g) {
		return get(g.getId());
	}

	@Override
	public boolean isInitialized() {
		return false;
	}

	protected void assertInitialization() {
		if (!init.get()) {
			log.error(typeName + " hasn't been initialized yet!");
			throw new IllegalStateException(typeName + " hasn't been initialized yet!");
		}
	}

	public boolean hasManagerFor(Guild g) {
		return hasManagerFor(g.getId());
	}

	public boolean hasManagerFor(String g) {
		return impls.containsKey(g);
	}
}
