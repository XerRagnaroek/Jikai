package com.xerragnaroek.jikai.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * A simple Value wrapper that allows for unidirectional binding.<br>
 * Should be thread-safe.
 * 
 * @author XerRagnaroek
 *
 * @param <T>
 */
public class Property<T> {
	protected T value;
	protected List<BiConsumer<T, T>> cons = Collections.synchronizedList(new ArrayList<>());
	protected AtomicBoolean changed = new AtomicBoolean(false);

	public Property() {}

	public Property(T value) {
		this.value = value;
	}

	public boolean hasNonNullValue() {
		return value != null;
	}

	public synchronized T get() {
		return value;
	}

	public void set(T newValue) {
		T tmp = syncSet(newValue);
		synchronized (cons) {
			cons.forEach(biCon -> biCon.accept(tmp, value));
		}
	}

	private synchronized T syncSet(T newVal) {
		T tmp = value;
		value = newVal;
		changed.set(true);
		return tmp;
	}

	public void bind(Property<T> prop) {
		cons.add(prop::propChange);
	}

	public synchronized void bindAndSet(Property<T> prop) {
		bind(prop);
		prop.set(value);
	}

	public boolean hasChanged() {
		if (changed.get()) {
			changed.set(false);
			return true;
		}
		return false;

	}

	public void onChange(BiConsumer<T, T> biCon) {
		cons.add(biCon);
	}

	protected void propChange(T oldVal, T newVal) {
		value = newVal;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Property<?>) {
			return ((Property<?>) obj).get().equals(value);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}

	@Override
	public String toString() {
		return value.toString();
	}

	@JsonCreator
	public static <T> Property<T> of(T value) {
		if (value != null) {
			return new Property<>(value);
		} else {
			return null;
		}
	}
}
