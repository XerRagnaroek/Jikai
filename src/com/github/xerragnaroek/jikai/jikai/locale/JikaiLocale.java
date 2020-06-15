package com.github.xerragnaroek.jikai.jikai.locale;

import java.util.List;
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

	public String getStringFormatted(String key, List<String> str, Object... objs) {
		if (!hasString(key)) {
			throw new IllegalArgumentException("No message for key: '" + key + "'");
		}
		if (str.size() != objs.length) {
			throw new IllegalArgumentException("Placeholder strings do not match objects!");
		}
		String tmp = content.get(key);
		for (int i = 0; i < str.size(); i++) {
			tmp = tmp.replace(str.get(i), objs[i].toString());
		}
		return tmp;
	}

	void registerKey(String key, String str) {
		content.put(key, str);
	}

	@Override
	public String toString() {
		return "JikaiLocale[\"" + identifier + "\"]";
	}
}
