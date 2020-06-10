package com.github.xerragnaroek.jikai.jikai.locale;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 
 */
public class JikaiLocale {

	private final String identifier;
	private Map<String, String> content = new ConcurrentHashMap<>();

	JikaiLocale(String ident) {
		identifier = ident;
	}

	public String getIdentifier() {
		return identifier;
	}

	public boolean hasString(String key) {
		return content.containsKey(key);
	}

	public String getString(String key) {
		return content.get(key);
	}

	void registerKey(String key, String str) {
		content.put(key, str);
	}
}
