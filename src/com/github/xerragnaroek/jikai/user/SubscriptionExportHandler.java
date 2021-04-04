package com.github.xerragnaroek.jikai.user;

import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.TreeBidiMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 */
public class SubscriptionExportHandler {

	private BidiMap<Long, String> exportKeys = new TreeBidiMap<>();
	private final Logger log = LoggerFactory.getLogger(SubscriptionExportHandler.class);
	private static SubscriptionExportHandler instance;

	private SubscriptionExportHandler() {}

	public synchronized static SubscriptionExportHandler getInstance() {
		return instance == null ? instance = new SubscriptionExportHandler() : instance;
	}

	public String generateExportKey(JikaiUser ju, boolean overwrite) {
		log.debug("Generating export key for JU '{}'", ju.getId());
		String key = exportKeys.get(ju.getId());
		if (key == null || overwrite) {
			key = DigestUtils.shaHex(ju.toString() + String.valueOf(System.currentTimeMillis()));
			exportKeys.put(ju.getId(), key);
		}
		return key;
	}

	public long getJikaiUserIdFromKey(String key) {
		return exportKeys.getKey(key);
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
