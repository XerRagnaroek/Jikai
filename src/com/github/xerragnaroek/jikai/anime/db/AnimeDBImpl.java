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
package com.github.xerragnaroek.jikai.anime.db;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jasa.AniException;
import com.github.xerragnaroek.jasa.Anime;
import com.github.xerragnaroek.jasa.JASA;
import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.util.BotUtils;
import com.github.xerragnaroek.jikai.util.Initilizable;

import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;

/**
 * Does the animes.
 * 
 * @author XerRagnaroek
 *
 */
class AnimeDBImpl implements Initilizable {
	private Map<Integer, Anime> anime = new ConcurrentHashMap<>();
	private static final Logger log = LoggerFactory.getLogger(AnimeDBImpl.class);
	private AtomicBoolean loading = new AtomicBoolean(true);
	private AtomicBoolean initialized = new AtomicBoolean(false);

	AnimeDBImpl() {}

	public void init() {
		log.info("Initializing AnimeBase");
		loadSeason();
		initialized.set(true);
	}

	/**
	 * Queries jikan for the current seasonal animes, loads their pages, and stores them with their
	 * broadcast day/time in the list.
	 * 
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	void loadSeason() {
		loading.set(true);
		Core.JDA.getPresence().setActivity(Activity.watching("the AnimeDataBase load"));
		JASA jasa = new JASA();
		try {
			List<Anime> newAnime = jasa.fetchSeasonalAnime(1);
			List<Anime> old = new ArrayList<>();
			CollectionUtils.addAll(old, anime.values());
			if (old.isEmpty()) {
				anime = newAnime.stream().collect(Collectors.toConcurrentMap(a -> a.getId(), a -> a));
			} else {
				handleUpdate(old, newAnime);
			}

		} catch (AniException | IOException e) {
			BotUtils.logAndSendToDev(log, "Exception while updating AnimeDB", e);
		}
		Core.JDA.getPresence().setActivity(null);
		loading.set(false);
	}

	Set<Anime> getAnimesAiringOnWeekday(DayOfWeek day, Guild g) {
		return getAnimesAiringOnWeekday(day, Core.JM.get(g).getJikaiData().getTimeZone());
	}

	Set<Anime> getAnimesAiringOnWeekday(DayOfWeek day, ZoneId zone) {
		return getAnimeMappedToDayOfWeek(zone).get(day);
	}

	Map<DayOfWeek, Set<Anime>> getAnimeMappedToDayOfWeek(ZoneId zone) {
		Map<DayOfWeek, Set<Anime>> map = new TreeMap<>();
		for (DayOfWeek d : DayOfWeek.values()) {
			map.put(d, new TreeSet<>());
		}
		Collection<Anime> vals = anime.values();
		synchronized (vals) {
			vals.forEach(a -> {
				Optional<LocalDateTime> ldt = a.getNextEpisodeDateTime(zone);
				if (ldt.isPresent()) {
					map.compute(ldt.get().getDayOfWeek(), (d, s) -> {
						s.add(a);
						return s;
					});
				}
			});
		}
		return map;
	}

	Set<Anime> getSeasonalAnime() {
		return anime.values().stream().sorted().collect(Collectors.toSet());
	}

	boolean isLoading() {
		return loading.get();
	}

	int size() {
		return anime.size();
	}

	Anime getAnime(int id) {
		return anime.get(id);
	}

	Anime getAnime(String title) {
		return anime.values().stream().filter(a -> a.getTitleRomaji().equals(title)).findFirst().get();
	}

	private void handleUpdate(List<Anime> old, List<Anime> newA) {
		AnimeUpdate au = AnimeUpdate.categoriseUpdates(old, newA);
		if (au.hasNewAnime()) {
			anime = newA.stream().collect(Collectors.toConcurrentMap(a -> a.getId(), a -> a));
			BotUtils.sendToAllInfoChannels("AnimeDB has updated to version " + AnimeDB.incrementAndGetDBVersion());
			log.info("AnimeDB has updated to version {}", AnimeDB.getAnimeDBVersion());
		}
		AnimeDB.dBUpdated(au);
	}

	@Override
	public boolean isInitialized() {
		return initialized.get();
	}
}
