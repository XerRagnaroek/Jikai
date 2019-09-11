package com.xerragnaroek.bot.anime;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stores ADTs adjusted to a specific timezone.
 * 
 * @author XerRagnaroek
 *
 */
public class ZoneAnimeBase {
	private static ZoneId jst = ZoneId.of("Japan");
	private final Logger log;
	private Map<String, AnimeDayTime> animes = new ConcurrentHashMap<>();
	private Map<DayOfWeek, List<AnimeDayTime>> animeWeek = new ConcurrentHashMap<>();
	private ZoneId zone;

	ZoneAnimeBase(ZoneId z) {
		log = LoggerFactory.getLogger(ZoneAnimeBase.class.getName() + "#" + z.toString());
		zone = z;
		log.debug("Created ZoneAnimeBase");
	}

	/**
	 * Set this ZAB's contents. Overwrites the old data.
	 * 
	 * @param col
	 *            - the Collection containing the new ADTs
	 */
	void setAnimeDayTimes(Collection<AnimeDayTime> col) {
		Stream<AnimeDayTime> stream = col.stream();
		if (!zone.equals(jst)) {
			stream = stream.map(adt -> adjust(adt, zone));
		}
		List<AnimeDayTime> adts = stream.collect(Collectors.toList());
		animes = adts.stream().collect(Collectors.toConcurrentMap(adt -> adt.getAnime().title, Function.identity()));
		animeWeek = makeWeekMap(adts);
		log.debug("Set content: {} adts", col.size());
	}

	/**
	 * Maps the ADTs to the day they air.
	 * 
	 * @param col
	 *            - the ADTs to map
	 * @return a Map with ADTs mapped to their respective DayOfWeek
	 */
	private Map<DayOfWeek, List<AnimeDayTime>> makeWeekMap(Collection<AnimeDayTime> col) {
		Map<DayOfWeek, List<AnimeDayTime>> map = emptyWeekMap();
		for (AnimeDayTime adt : col) {
			map.get(adt.getDayOfWeek()).add(adt);
		}
		return makeUnmodifiable(map);
	}

	/*
	 * Utility method that wraps the lists in a map in an unmodifiable one.
	 */
	private Map<DayOfWeek, List<AnimeDayTime>> makeUnmodifiable(Map<DayOfWeek, List<AnimeDayTime>> map) {
		for (DayOfWeek day : DayOfWeek.values()) {
			map.put(day, Collections.unmodifiableList(map.get(day)));
		}
		return map;
	}

	/**
	 * Adjusts an ADT into the supplied timezone.
	 * 
	 */
	private AnimeDayTime adjust(AnimeDayTime adt, ZoneId z) {
		return new AnimeDayTime(adt.getAnime(), adt.getZonedDateTime().withZoneSameInstant(z));
	}

	/**
	 * Creates a map, with an empty list mapped to each day of the week.
	 * 
	 * @return
	 */
	private Map<DayOfWeek, List<AnimeDayTime>> emptyWeekMap() {
		Map<DayOfWeek, List<AnimeDayTime>> map = new ConcurrentHashMap<>();
		for (DayOfWeek day : DayOfWeek.values()) {
			map.put(day, new ArrayList<>());
		}
		return map;
	}

	/**
	 * Get all animes that air on the given DayOfWeek
	 * 
	 * @param day
	 *            - the day the animes air on
	 * @return - A List of all animes that air on the given day. If none match the returned list is
	 *         empty.
	 */
	List<AnimeDayTime> getAnimesOnDayOfWeek(DayOfWeek day) {
		return animeWeek.get(day);
	}

	/**
	 * Get all stored animes.
	 * 
	 */
	Collection<AnimeDayTime> getAnimes() {
		return Collections.unmodifiableCollection(animes.values());
	}

	/**
	 * Self explanatory.
	 */
	boolean hasEntries() {
		return !animes.isEmpty();
	}
}
