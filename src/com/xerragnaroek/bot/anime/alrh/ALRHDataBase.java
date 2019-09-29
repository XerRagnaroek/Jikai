package com.xerragnaroek.bot.anime.alrh;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.apache.commons.collections4.map.MultiKeyMap;

import com.xerragnaroek.bot.anime.base.AnimeBase;

public class ALRHDataBase {
	private Map<String, Set<ALRHData>> msgMap = Collections.synchronizedMap(new HashMap<>());
	private Map<String, ALRHData> titleMap = Collections.synchronizedMap(new TreeMap<>());
	@SuppressWarnings("rawtypes")
	private MultiKeyMap ucMsgMap = new MultiKeyMap();
	private int sentABVersion;
	private String sentTcId;

	ALRHDataBase() {}

	void setDataForMessage(String msgId, Set<ALRHData> data) {
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

	String getSentTextChannelId() {
		return sentTcId;
	}

	private void updateVariables(ALRHData data) {
		sentABVersion = AnimeBase.getAnimeBaseVersion();
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
	}

	Set<ALRHData> getDataForMessage(String msgId) {
		return msgMap.get(msgId);
	}

	ALRHData getDataForTitle(String title) {
		return titleMap.get(title);
	}

	ALRHData getDataForUnicodeCodePoint(String msgId, String uccp) {
		return (ALRHData) ucMsgMap.get(msgId, uccp);
	}

	Set<ALRHData> getData() {
		return new TreeSet<>(titleMap.values());
	}

	boolean hasDataForMessage(String msgId) {
		return msgMap.containsKey(msgId);
	}

	Set<ALRHData> getReactedAnimes() {
		return titleMap.values().stream().filter(ALRHData::isReacted).collect(Collectors.toSet());
	}

	void forEachMessage(BiConsumer<String, Set<ALRHData>> con) {
		msgMap.forEach(con);
	}

	void clearData() {
		msgMap.clear();
		titleMap.clear();
		ucMsgMap.clear();
	}
}
