
package com.github.xerragnaroek.jikai.util;

public class Pair<U, T> {
	private U left;
	private T right;

	private Pair(U l, T r) {
		left = l;
		right = r;
	}

	public U getLeft() {
		return left;
	}

	public T getRight() {
		return right;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Pair) {
			Pair<?, ?> tmp = (Pair<?, ?>) obj;
			return left.equals(tmp.getLeft()) && right.equals(tmp.getRight());
		} else {
			return false;
		}
	}

	public static <U, T> Pair<U, T> of(U left, T right) {
		return new Pair<>(left, right);
	}
}
