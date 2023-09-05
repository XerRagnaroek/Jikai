package com.github.xerragnaroek.jikai.user;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.TreeBidiMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 *
 */
public class ExportKeyHandler {

    private BidiMap<Long, String> exportKeys = new TreeBidiMap<>();
    private final Logger log = LoggerFactory.getLogger(ExportKeyHandler.class);
    private static ExportKeyHandler instance;

    private ExportKeyHandler() {
    }

    public synchronized static ExportKeyHandler getInstance() {
        return instance == null ? instance = new ExportKeyHandler() : instance;
    }

    public String generateExportKey(JikaiUser ju, boolean overwrite) {
        log.debug("Generating export key for JU '{}'", ju.getId());
        String key = exportKeys.get(ju.getId());
        if (key == null || overwrite) {
            key = DigestUtils.shaHex(ju.toString() + System.currentTimeMillis());
            exportKeys.put(ju.getId(), key);
        }
        return key;
    }

    public long getJikaiUserIdFromKey(String key) {
        if (hasIdForKey(key)) {
            return exportKeys.getKey(key);
        } else {
            return 0;
        }
    }

    public boolean hasIdForKey(String key) {
        return exportKeys.containsValue(key);
    }

    public Map<Long, String> getKeyMap() {
        return exportKeys;
    }

    private void setMap(Map<Long, String> keys) {
        exportKeys = new TreeBidiMap<>(keys);
    }

    public static void loadMap(Map<Long, String> keys) {
        getInstance().setMap(keys);
    }
}
