package com.xerragnaroek.jikai.util.prop;

import com.fasterxml.jackson.annotation.JsonCreator;

public class IntegerProperty extends Property<Integer> {
	public IntegerProperty(int val) {
		super(val);
	}

	public int incrementAndGet() {
		set(value + 1);
		return value;
	}

	public int getAndIncrement() {
		int tmp = value;
		set(value + 1);
		return tmp;
	}

	@JsonCreator
	public static IntegerProperty of(int value) {
		return new IntegerProperty(value);
	}

}
