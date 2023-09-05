package com.github.xerragnaroek.jikai.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Pair<U, T> {
    private final U left;
    private final T right;

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
        if (obj instanceof Pair<?, ?> tmp) {
            return left.equals(tmp.getLeft()) && right.equals(tmp.getRight());
        } else {
            return false;
        }
    }

    @JsonCreator
    public static <U, T> Pair<U, T> of(@JsonProperty("left") U left, @JsonProperty("right") T right) {
        return new Pair<>(left, right);
    }

    @Override
    public String toString() {
        return String.format("Pair{%s,%s}", left, right);
    }
}
