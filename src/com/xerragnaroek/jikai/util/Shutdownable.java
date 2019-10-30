package com.xerragnaroek.jikai.util;

import com.xerragnaroek.jikai.util.prop.Property;

public interface Shutdownable {

	public void shutdown(boolean now);

	public void destroy();

	public void waitForShutdown();

	public Property<Boolean> shutdownProperty();

	public default boolean hasForceShutdownProperty() {
		return true;
	}

	public Property<Boolean> forceShutdownProperty();

	public default boolean isShutdown() {
		return shutdownProperty().get();
	}
}
