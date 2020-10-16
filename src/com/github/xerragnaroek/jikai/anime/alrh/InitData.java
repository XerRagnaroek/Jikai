package com.github.xerragnaroek.jikai.anime.alrh;

import java.util.Map;
import java.util.Set;

import com.github.xerragnaroek.jikai.util.Pair;

/**
 * 
 */
public class InitData {

	private Set<ALRHData> data;
	private Map<Long, String> msgIdEmbedTitles;
	private Pair<String, Long> seasonMsg;

	InitData(Set<ALRHData> data, Map<Long, String> msgIdEmbedTitles, Pair<String, Long> seasonMsg) {
		this.data = data;
		this.msgIdEmbedTitles = msgIdEmbedTitles;
		this.seasonMsg = seasonMsg;
	}

	Set<ALRHData> getData() {
		return data;
	}

	Map<Long, String> getMsgIdEmbedTitlesMap() {
		return msgIdEmbedTitles;
	}

	Pair<String, Long> getSeasonMsg() {
		return seasonMsg;
	}
}
