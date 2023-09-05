package com.github.xerragnaroek.jikai.util;

public interface Initilizable {
    void init();

    boolean isInitialized();

    default void waitForInitilization() {
        while (!isInitialized()) {
        }
    }

}
