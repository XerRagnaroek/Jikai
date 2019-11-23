/*
 * MIT License
 *
 * Copyright (c) 2019 github.com/XerRagnaroek
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.xerragnaroek.jikai.anime.alrh;

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

import com.xerragnaroek.jikai.anime.db.AnimeDB;

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
