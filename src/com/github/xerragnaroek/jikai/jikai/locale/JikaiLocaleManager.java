package com.github.xerragnaroek.jikai.jikai.locale;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.xerragnaroek.jikai.core.Core;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
public class JikaiLocaleManager {

    private static JikaiLocaleManager instance;
    private final Map<String, JikaiLocale> locales = new ConcurrentHashMap<>();
    private final Logger log = LoggerFactory.getLogger(JikaiLocaleManager.class);

    private JikaiLocaleManager() {
    }

    public static JikaiLocaleManager getInstance() {
        instance = instance == null ? new JikaiLocaleManager() : instance;
        return instance;
    }

    public JikaiLocale getLocale(String identifier) {
        return locales.get(identifier.toLowerCase());
    }

    public boolean hasLocale(String identifier) {
        return locales.containsKey(identifier.toLowerCase());
    }

    public Set<String> getLocaleIdentifiers() {
        return new HashSet<>(locales.keySet());
    }

    public Set<JikaiLocale> getLocales() {
        return new HashSet<>(locales.values());
    }

    public JikaiLocale getLocaleViaFlagUnicode(String fu) {
        for (JikaiLocale loc : locales.values()) {
            if (loc.getString("u_flag_uni").equalsIgnoreCase(fu)) {
                return loc;
            }
        }
        return null;
    }

    private void loadLocale(Path locale) {
        log.debug("Loading locale from {}", locale);
        String identifier = StringUtils.substringBefore(locale.getFileName().toString(), ".").toLowerCase();
        JikaiLocale jloc = new JikaiLocale(identifier);
        try {
            JsonNode loc = new ObjectMapper().readTree(locale.toFile());
            loc.fields().forEachRemaining(e -> jloc.registerKey(e.getKey(), e.getValue().asText()));
            locales.put(identifier, jloc);
            log.info("Loaded locale {}", identifier);
        } catch (IOException e) {
            log.error("Failed reading locale {}!", identifier, e);
        }

    }

    private void loadLocalesImpl() {
        log.info("Loading Locales...");
        try {
            Files.walk(Path.of(Core.DATA_LOC.toString(), "/locales/")).filter(p -> Files.isRegularFile(p)).forEach(instance::loadLocale);
        } catch (IOException e) {
            Core.ERROR_LOG.error("Failed walking locale folder", e);
        }
    }

    public static JikaiLocale getEN() {
        return getInstance().getLocale("en");
    }

    public static void loadLocales() {
        getInstance().loadLocalesImpl();
    }

    public static Map<String, Map<String, List<String>>> validateLocales() {
        Map<String, Map<String, List<String>>> map = new HashMap<>();
        getInstance().locales.forEach((ident, loc) -> map.put(ident, loc.validate()));
        return map;
    }

}
