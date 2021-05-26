package com.github.xerragnaroek.jikai.anime.alrh;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.TreeBidiMap;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jikai.util.Pair;

public class ALRHDataBase {
	private Map<Long, Set<ALRHData>> msgMap = Collections.synchronizedMap(new HashMap<>());
	private Map<Integer, ALRHData> aniIdMap = Collections.synchronizedMap(new TreeMap<>());
	// title of the Embed mapped to its msgid
	private BidiMap<Long, String> msgIdEmbedTitleMap = new TreeBidiMap<Long, String>();
	@SuppressWarnings("rawtypes")
	private MultiKeyMap ucMsgMap = new MultiKeyMap();
	private long sentTcId;
	private Pair<String, Long> seasonMsg;
	private final Logger log = LoggerFactory.getLogger(ALRHDataBase.class);

	ALRHDataBase() {}

	void setDataForMessage(long msgId, Set<ALRHData> data) {
		updateVariables(data.iterator().next());
		msgMap.put(msgId, data);
		data.forEach(alrhd -> {
			alrhd.setMessageId(msgId);
			addALRHData(alrhd);
		});
	}

	long getSentTextChannelId() {
		return sentTcId;
	}

	private void updateVariables(ALRHData data) {
		sentTcId = data.getTextChannelId();
	}

	@SuppressWarnings("unchecked")
	void addALRHData(ALRHData d) {
		aniIdMap.put(d.getAnimeId(), d);
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

	ALRHData getDataForId(int id) {
		return aniIdMap.get(id);
	}

	ALRHData getDataForUnicodeCodePoint(long msgId, String uccp) {
		return (ALRHData) ucMsgMap.get(msgId, uccp);
	}

	Set<ALRHData> getData() {
		return new TreeSet<>(aniIdMap.values());
	}

	BidiMap<Long, String> getMsgIdTitleMap() {
		return msgIdEmbedTitleMap;
	}

	boolean hasDataForMessage(long msgId) {
		return msgMap.containsKey(msgId);
	}

	Map<Long, Set<ALRHData>> getMsgIdDataMap() {
		return msgMap;
	}

	Set<ALRHData> getReactedAnimes() {
		return aniIdMap.values().stream().filter(ALRHData::isReacted).collect(Collectors.toSet());
	}

	void forEachMessage(BiConsumer<Long, Set<ALRHData>> con) {
		msgMap.forEach(con);
	}

	void clearData() {
		msgMap.clear();
		aniIdMap.clear();
		ucMsgMap.clear();
		msgIdEmbedTitleMap.clear();
	}

	boolean hasDataForId(int id) {
		return aniIdMap.containsKey(id);
	}

	void deleteEntry(ALRHData data) {
		aniIdMap.remove(data.getAnimeId());
		log.debug("Deleted entry for {}", data.getAnimeId());
	}

	void clearUcMsgMap() {
		ucMsgMap.clear();
		log.debug("Cleared the unicode-messageId map");
	}

	boolean isReacted(ALRHData data) {
		return getReactedAnimes().contains(data);
	}

	void mapEmbedTitleToId(long id, String t) {
		msgIdEmbedTitleMap.put(id, t);
	}

	void setMsgIdEmbedTitleMap(Map<Long, String> map) {
		msgIdEmbedTitleMap = new TreeBidiMap<>(map);
	}

	long getMsgIdForEmbedTitle(String title) {
		return msgIdEmbedTitleMap.getKey(title);
	}

	String getEmbedTitleForMsgId(long id) {
		return msgIdEmbedTitleMap.get(id);
	}

	boolean hasMsgIdForEmbedTitle(String title) {
		return msgIdEmbedTitleMap.containsValue(title);
	}

	void setSeasonMsg(Pair<String, Long> msg) {
		seasonMsg = msg;
	}

	Pair<String, Long> getSeasonMsg() {
		return seasonMsg;
	}

	void removeDataForMessage(long id, String title) {
		Set<ALRHData> data = getDataForMessage(id);
		if (data != null) {
			log.debug("Deleting {} entries for message {}", data.size(), title);
			msgIdEmbedTitleMap.removeValue(title);
			data.forEach(this::deleteEntry);
		}
	}

	List<String> getAllMessageIdsAsList() {
		List<String> ids = msgMap.keySet().stream().map(String::valueOf).collect(Collectors.toList());
		ids.add(seasonMsg.getRight().toString());
		return ids;
	}
}
