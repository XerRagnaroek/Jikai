package com.xerragnaroek.bot.util;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;

import net.dv8tion.jda.api.entities.Guild;

public abstract class Manager<T extends Initilizable> implements Initilizable {

	private final Map<String, T> impls = Collections.synchronizedMap(new TreeMap<>());
	private AtomicBoolean init = new AtomicBoolean(false);
	protected Logger log;
	private final String typeName;

	protected abstract void initLogger();

	protected abstract T makeNew(String gId);

	protected Manager(String typeName) {
		this.typeName = typeName;
	}

	protected void initImpls() {
		impls.values().forEach(Initilizable::init);
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
	public void init() {
		initImpls();
		init.set(true);
	}

	@Override
	public boolean isInitialized() {
		return false;
	}

	protected void assertInitialisation() {
		if (!init.get()) {
			log.error("ALRHManager hasn't been initialized yet!");
			throw new IllegalStateException("ALRHManager hasn't been initialized yet!");
		}
	}
}
