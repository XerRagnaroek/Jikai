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
package com.xerragnaroek.jikai.anime.db;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.Doomsdayrs.Jikan4java.types.Main.Anime.Anime;

/**
 * Stores ADTs adjusted to a specific timezone.
 * 
 * @author XerRagnaroek
 *
 */
public class ZoneAnimeBase {
	private static ZoneId jst = ZoneId.of("Japan");
	private final Logger log;
	private Map<String, AnimeDayTime> animes = Collections.synchronizedSortedMap(new TreeMap<>());
	private Map<DayOfWeek, Set<AnimeDayTime>> animeWeek = Collections.synchronizedSortedMap(new TreeMap<>());
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
	private Map<DayOfWeek, Set<AnimeDayTime>> makeWeekMap(Collection<AnimeDayTime> col) {
		Map<DayOfWeek, Set<AnimeDayTime>> map = emptyWeekMap();
		for (AnimeDayTime adt : col) {
			map.get(adt.getDayOfWeek()).add(adt);
		}
		return makeUnmodifiable(map);
	}

	/*
	 * Utility method that wraps the lists in a map in an unmodifiable one.
	 */
	private Map<DayOfWeek, Set<AnimeDayTime>> makeUnmodifiable(Map<DayOfWeek, Set<AnimeDayTime>> map) {
		for (DayOfWeek day : DayOfWeek.values()) {
			map.put(day, Collections.unmodifiableSet(map.get(day)));
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
	private Map<DayOfWeek, Set<AnimeDayTime>> emptyWeekMap() {
		Map<DayOfWeek, Set<AnimeDayTime>> map = new ConcurrentHashMap<>();
		for (DayOfWeek day : DayOfWeek.values()) {
			map.put(day, new TreeSet<>());
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
	Set<AnimeDayTime> getAnimesOnDayOfWeek(DayOfWeek day) {
		return animeWeek.get(day);
	}

	/**
	 * Get all stored animes.
	 * 
	 */
	Set<AnimeDayTime> getAnimes() {
		return Collections.unmodifiableSet(new TreeSet<>(animes.values()));
	}

	/**
	 * Self explanatory.
	 */
	boolean hasEntries() {
		return !animes.isEmpty();
	}

	int size() {
		return animes.size();
	}

	@Nullable
	Anime getAnime(String title) {
		AnimeDayTime adt = animes.get(title);
		if (adt != null) {
			return adt.getAnime();
		}
		return null;
	}

	AnimeDayTime getADT(String title) {
		return animes.get(title);
	}

	public void clear() {
		animes.clear();
		animeWeek.clear();
	}
}
