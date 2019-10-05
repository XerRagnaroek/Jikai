package com.xerragnaroek.jikai.util;

public interface Initilizable {
	public void init();

	public boolean isInitialized();

	public default void waitForInitilization() {
		while (!isInitialized()) {}
	}

}
