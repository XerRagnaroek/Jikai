package com.github.xerragnaroek.jikai.anime.list;

import java.util.Map;
import java.util.Set;

import com.github.xerragnaroek.jasa.TitleLanguage;
import com.github.xerragnaroek.jikai.util.Pair;

/**
 * 
 */
public record InitData(Set<ALRHData> data, Map<Long, String> msgIdEmbedTitles, Pair<String, Long> seasonMsg, TitleLanguage titleLang) {}
