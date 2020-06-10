package com.github.xerragnaroek.jikai.jikai.locale;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 
 */
public class JikaiLocaleManager {

	private static JikaiLocaleManager instance;
	private Map<String, JikaiLocale> locales = new ConcurrentHashMap<>();
	private final Logger log = LoggerFactory.getLogger(JikaiLocaleManager.class);

	private JikaiLocaleManager() {}

	public static JikaiLocaleManager getInstance() {
		instance = instance == null ? new JikaiLocaleManager() : instance;
		return instance;
	}

	public JikaiLocale getLocale(String identifier) {
		return locales.get(identifier);
	}

	public boolean hasLocale(String identifier) {
		return locales.containsKey(identifier);
	}

	public Set<String> getAvailableLocales() {
		return new HashSet<>(locales.keySet());
	}

	public void loadLocale(Path locale) throws JsonProcessingException, IOException {
		String identifier = StringUtils.substringBefore(locale.getFileName().toString(), ".");
		JikaiLocale jloc = new JikaiLocale(identifier);
		JsonNode loc = new ObjectMapper().readTree(locale.toFile());
		loc.fields().forEachRemaining(e -> jloc.registerKey(e.getKey(), e.getValue().asText()));
		log.info("Loaded locale {}", identifier);
	}

}
