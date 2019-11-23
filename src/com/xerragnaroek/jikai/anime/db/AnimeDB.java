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
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.doomsdayrs.jikan4java.types.main.anime.Anime;
import com.xerragnaroek.jikai.core.Core;
import com.xerragnaroek.jikai.util.prop.IntegerProperty;

import net.dv8tion.jda.api.entities.Guild;

public class AnimeDB {

	private static AnimeDBImpl aDB = new AnimeDBImpl();
	private static boolean initialized = false;
	private final static Logger log = LoggerFactory.getLogger(AnimeDB.class);
	private static long updateRate = 6;
	private static ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
	private static IntegerProperty version = new IntegerProperty(0);
	private static Set<Consumer<Set<String>>> updateCon = Collections.synchronizedSet(new HashSet<>());

	public static void init() {
		if (!initialized) {
			aDB.init();
			initialized = true;
		} else {
			log.error("Already initialized!");
			throw new IllegalStateException("Already initialized!");
		}
	}

	public static Set<AnimeDayTime> getAnimesAiringOnWeekday(DayOfWeek day, Guild g) {
		return aDB.getAnimesAiringOnWeekday(day, g);
	}

	public static Set<AnimeDayTime> getAnimesAiringOnWeekday(DayOfWeek day, ZoneId z) {
		return aDB.getAnimesAiringOnWeekday(day, z);
	}

	public static Map<DayOfWeek, Set<AnimeDayTime>> getAnimesMappedToDayOfAiring(ZoneId zone) {
		Map<DayOfWeek, Set<AnimeDayTime>> map = new TreeMap<>();
		for (DayOfWeek day : DayOfWeek.values()) {
			map.put(day, getAnimesAiringOnWeekday(day, zone));
		}
		return map;
	}

	public static Set<AnimeDayTime> getSeasonalAnime() {
		return aDB.getSeasonalAnimes();
	}

	public static void addTimeZone(ZoneId z) {
		aDB.addTimeZone(z, false);
	}

	public static void waitUntilLoaded() {
		while (aDB.isLoading()) {}
	}

	public static Set<AnimeDayTime> getSeasonalAnimesAdjusted(ZoneId tz) {
		return aDB.getSeasonalAnimesAdjusted(tz);
	}

	public static Anime getAnime(String title) {
		return aDB.getAnime(title);
	}

	public static AnimeDayTime getADT(ZoneId zone, String title) {
		return aDB.getADT(zone, title);
	}

	public static int getAnimeDBVersion() {
		return version.get();
	}

	public static IntegerProperty dbVersionProperty() {
		return version;
	}

	public static int incrementAndGetDBVersion() {
		return version.incrementAndGet();
	}

	public static int getAndIncrementDBVersion() {
		return version.getAndIncrement();
	}

	public static void startUpdateThread() {
		exec.scheduleAtFixedRate(aDB::loadSeason, updateRate, updateRate, TimeUnit.HOURS);
	}

	public static void setUpdateRate(long rate) {
		updateRate = rate;
	}

	public static int loadedAnimes() {
		return aDB.size();
	}

	public static void setDBVersionProperty(IntegerProperty prop) {
		version = prop;
	}

	public static Anime getAnimeByNumber(int n) {
		return getAnime(aDB.getATNDB().getTitle(n));
	}

	public static AnimeDayTime getADTByNumber(ZoneId zone, int n) {
		return getADT(zone, aDB.getATNDB().getTitle(n));
	}

	public static int titleToNumber(String title) {
		return aDB.getATNDB().getNumber(title);
	}

	public static void runOnDBUpdate(Consumer<Set<String>> con) {
		updateCon.add(con);
	}

	static void dBUpdated(Set<String> removedAnime) {
		updateCon.forEach(con -> Core.EXEC.execute(() -> con.accept(removedAnime)));
	}
}
