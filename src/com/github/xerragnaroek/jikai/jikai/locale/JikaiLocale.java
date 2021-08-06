package com.github.xerragnaroek.jikai.jikai.locale;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import com.github.xerragnaroek.jikai.util.BotUtils;

import net.dv8tion.jda.api.entities.MessageEmbed;

/**
 * 
 */
public class JikaiLocale implements Comparable<JikaiLocale> {

	private final String identifier;
	private Map<String, String> content = new ConcurrentHashMap<>();
	private Locale loc;

	JikaiLocale(String ident) {
		identifier = ident;
		loc = new Locale(identifier);
	}

	public String getIdentifier() {
		return identifier;
	}

	public boolean hasString(String key) {
		return content.containsKey(key);
	}

	public String getString(String key) {
		if (hasString(key)) {
			return content.get(key);
		} else {
			if (!identifier.equals("en")) {
				return JikaiLocaleManager.getEN().getString(key);
			}
			return null;
		}
	}

	public String getStringFormatted(String key, List<String> str, Object... objs) {
		if (!hasString(key)) {
			if (!identifier.equals("en")) {
				return JikaiLocaleManager.getEN().getStringFormatted(key, str, objs);
			}
			throw new IllegalArgumentException("No message for key: '" + key + "'");
		}
		if (str.size() > objs.length) {
			throw new IllegalArgumentException("Placeholder strings do not match objects! key:" + key);
		}
		String tmp = content.get(key);
		for (int i = 0; i < str.size(); i++) {
			tmp = tmp.replace("%" + str.get(i) + "%", objs[i].toString());
		}
		return tmp;
	}

	public MessageEmbed getAsEmbed(String key) {
		return BotUtils.makeSimpleEmbed(getString(key));
	}

	public MessageEmbed getAsEmbedFormatted(String key, List<String> str, Object... objs) {
		return BotUtils.makeSimpleEmbed(getStringFormatted(key, str, objs));
	}

	public Locale getLocale() {
		return loc;
	}

	public String getLanguageName() {
		return content.get("u_lang_name");
	}

	public String getYesOrNo(boolean yes) {
		return getString(yes ? "u_yes" : "u_no");
	}

	public boolean isFormattedString(String key) {
		String tmp;
		if ((tmp = getString(key)) != null) {
			return Pattern.compile(".*%\\S+?%.*").matcher(tmp).find();
		}
		return false;
	}

	void registerKey(String key, String str) {
		content.put(key, str);
	}

	public Map<String, List<String>> validate() {
		Map<String, List<String>> map = new HashMap<>();
		List<String> missing = new LinkedList<>();
		if (!this.identifier.equalsIgnoreCase("en")) {
			JikaiLocale en = JikaiLocaleManager.getEN();
			missing = en.content.keySet().stream().filter(str -> !content.keySet().contains(str)).toList();
		}
		map.put("MISSING", missing);
		return map;
	}

	@Override
	public String toString() {
		return "JikaiLocale[\"" + identifier + "\"]";
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof JikaiLocale) {
			return identifier.equals(((JikaiLocale) obj).getIdentifier());
		} else {
			return false;
		}
	}

	@Override
	public int compareTo(JikaiLocale loc) {
		return identifier.compareTo(loc.identifier);
	}
}
