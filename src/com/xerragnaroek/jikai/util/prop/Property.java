/*
 * MIT License
 *
 * Copyright (c) 2019 github.com/XerRagnaroek
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.xerragnaroek.jikai.util.prop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.xerragnaroek.jikai.util.Destroyable;

/**
 * A simple Value wrapper that allows for unidirectional binding.<br>
 * Should be thread-safe.
 * 
 * @author XerRagnaroek
 *
 * @param <T>
 */
public class Property<T> implements Destroyable {
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
		if (!hasNonNullValue() || !value.equals(newValue)) {
			T tmp = syncSet(newValue);
			synchronized (cons) {
				cons.forEach(biCon -> biCon.accept(tmp, value));
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

	@Override
	public void destroy() {
		cons.clear();
	}
}
