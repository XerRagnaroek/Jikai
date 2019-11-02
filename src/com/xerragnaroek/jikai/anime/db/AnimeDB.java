package com.xerragnaroek.jikai.anime.db;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.Doomsdayrs.Jikan4java.types.Main.Anime.Anime;
import com.xerragnaroek.jikai.util.prop.IntegerProperty;

import net.dv8tion.jda.api.entities.Guild;

public class AnimeDB {

	private static AnimeDBImpl aDB = new AnimeDBImpl();
	private static boolean initialized = false;
	private final static Logger log = LoggerFactory.getLogger(AnimeDB.class);
	private static long updateRate = 6;
	private static ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
	private static IntegerProperty version = new IntegerProperty(0);

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

	public static Set<AnimeDayTime> getSeasonalAnimes() {
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
		return aDB.getAnimeByNum(n);
	}

	public static AnimeDayTime getADTByNumber(ZoneId zone, int n) {
		return aDB.getADTByNum(zone, n);
	}

	public static int titleToNumber(String title) {
		return aDB.titleToNumber(title);
	}
}
