package com.github.xerragnaroek.jikai.util;

import com.github.xerragnaroek.jikai.util.prop.Property;

public interface Shutdownable {

    void shutdown(boolean now);

    void destroy();

    void waitForShutdown();

    Property<Boolean> shutdownProperty();

    default boolean hasForceShutdownProperty() {
        return true;
    }

    Property<Boolean> forceShutdownProperty();

    default boolean isShutdown() {
        return shutdownProperty().get();
    }
}
