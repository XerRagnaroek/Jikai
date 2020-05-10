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
	private JASA jasa = new JASA();

	AnimeDBImpl() {}

	public void init() {
		log.info("Initializing AnimeBase");
		loadAiringAnime();
		initialized.set(true);
	}

	/**
	 * Queries jikan for the current seasonal animes, loads their pages, and stores them with their
	 * broadcast day/time in the list.
	 * 
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	void loadAiringAnime() {
		loading.set(true);
		Core.JDA.getPresence().setPresence(Activity.watching("the anime load!"), false);
		try {
			List<Anime> newAnime = jasa.fetchAllAiringAnime(0, 2020);
			newAnime.addAll(jasa.fetchSeasonalAnime(0));
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
		if (au.hasChange()) {
			//newA.removeIf(a -> !a.hasDataForNextEpisode());
			anime = newA.stream().collect(Collectors.toConcurrentMap(a -> a.getId(), a -> a));
		}
		AnimeDB.dBUpdated(au);
	}

	@Override
	public boolean isInitialized() {
		return initialized.get();
	}
}
