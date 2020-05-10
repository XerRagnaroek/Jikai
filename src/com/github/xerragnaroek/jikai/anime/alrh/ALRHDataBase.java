package com.github.xerragnaroek.jikai.anime.alrh;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jikai.anime.db.AnimeDB;

public class ALRHDataBase {
	private Map<Long, Set<ALRHData>> msgMap = Collections.synchronizedMap(new HashMap<>());
	private Map<String, ALRHData> titleMap = Collections.synchronizedMap(new TreeMap<>());
	@SuppressWarnings("rawtypes")
	private MultiKeyMap ucMsgMap = new MultiKeyMap();
	private int sentABVersion;
	private long sentTcId;
	private final Logger log = LoggerFactory.getLogger(ALRHDataBase.class);

	ALRHDataBase() {}

	void setDataForMessage(long msgId, Set<ALRHData> data) {
		updateVariables(data.iterator().next());
		msgMap.put(msgId, data);
		data.forEach(alrhd -> {
			alrhd.setMessageId(msgId);
			alrhd.setABVersion(sentABVersion);
			addALRHData(alrhd);
		});
	}

	int getSentABVersion() {
		return sentABVersion;
	}

	long getSentTextChannelId() {
		return sentTcId;
	}

	private void updateVariables(ALRHData data) {
		sentABVersion = AnimeDB.getAnimeDBVersion();
		sentTcId = data.getTextChannelId();
	}

	@SuppressWarnings("unchecked")
	void addALRHData(ALRHData d) {
		titleMap.put(d.getTitle(), d);
		ucMsgMap.put(d.getMessageId(), d.getUnicodeCodePoint(), d);
	}

	void addData(Set<ALRHData> data) {
		if (!data.isEmpty()) {
			updateVariables(data.iterator().next());
			if (data != null) {
				data.forEach(d -> {
					addALRHData(d);
					msgMap.compute(d.getMessageId(), (k, v) -> {
						if (v == null) {
							v = new TreeSet<>();
						}
						v.add(d);
						return v;
					});
				});
			}
		}
		log.debug("Added {} alrh entries to db", data.size());
	}

	Set<ALRHData> getDataForMessage(long msgId) {
		return msgMap.get(msgId);
	}

	ALRHData getDataForTitle(String title) {
		return titleMap.get(title);
	}

	ALRHData getDataForUnicodeCodePoint(long msgId, String uccp) {
		return (ALRHData) ucMsgMap.get(msgId, uccp);
	}

	Set<ALRHData> getData() {
		return new TreeSet<>(titleMap.values());
	}

	boolean hasDataForMessage(long msgId) {
		return msgMap.containsKey(msgId);
	}

	Set<ALRHData> getReactedAnimes() {
		return titleMap.values().stream().filter(ALRHData::isReacted).collect(Collectors.toSet());
	}

	void forEachMessage(BiConsumer<Long, Set<ALRHData>> con) {
		msgMap.forEach(con);
	}

	void clearData() {
		msgMap.clear();
		titleMap.clear();
		ucMsgMap.clear();
	}

	boolean hasDataForTitle(String title) {
		return titleMap.containsKey(title);
	}

	void deleteEntry(ALRHData data) {
		titleMap.remove(data.getTitle());
		log.debug("Deleted entry for {}", data.getTitle());
	}

	void clearUcMsgMap() {
		ucMsgMap.clear();
		log.debug("Cleared the unicode-messageId map");
	}

	boolean isReacted(ALRHData data) {
		return getReactedAnimes().contains(data);
	}
}
