package com.github.xerragnaroek.jikai.util.prop;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 *
 */
public class LongProperty extends Property<Long> {
    public LongProperty(long val) {
        super(val);
    }

    @JsonCreator
    public static LongProperty of(long value) {
        return new LongProperty(value);
    }
}
