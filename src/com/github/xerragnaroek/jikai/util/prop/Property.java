
package com.github.xerragnaroek.jikai.util.prop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * A simple Value wrapper that allows for unidirectional binding.<br>
 * Should be thread-safe.
 * 
 * @author XerRagnaroek
 * @param <T>
 */
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class Property<T> {
	protected T value;
	protected List<BiConsumer<T, T>> cons = Collections.synchronizedList(new ArrayList<>());
	protected AtomicBoolean changed = new AtomicBoolean(false);
	protected BooleanSupplier dontRun = () -> false;

	public Property() {}

	public Property(T value) {
		this.value = value;
	}

	public boolean hasNonNullValue() {
		return value != null;
	}

	@JsonValue
	public synchronized T get() {
		return value;
	}

	public synchronized T getOrDefault(T def) {
		return value == null ? def : value;
	}

	public void set(T newValue) {
		if (!Objects.equals(value, newValue)) {
			T tmp = syncSet(newValue);
			runConsumer(tmp, newValue);
		}
	}

	protected void runConsumer(T oldV, T newV) {
		if (!dontRun.getAsBoolean()) {
			synchronized (cons) {
				cons.forEach(biCon -> biCon.accept(oldV, newV));
			}
		}
	}

	private synchronized T syncSet(T newVal) {
		T tmp = value;
		value = newVal;
		changed.set(true);
		return tmp;
	}

	public void bind(Property<T> prop) {
		cons.add(prop::changeProp);
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

	protected void changeProp(T oldVal, T newVal) {
		value = newVal;
	}

	public void clearConsumer() {
		cons.clear();
	}

	public void dontRunConsumerIf(BooleanSupplier dontRun) {
		this.dontRun = dontRun;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Property<?>) {
			return ((Property<?>) obj).get().equals(value);
		} else {
			if (!hasNonNullValue()) {
				return false;
			}
			return value.equals(obj);
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
		return new Property<>(value);
	}

}
