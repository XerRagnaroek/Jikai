package com.xerragnaroek.jikai.anime.db;

import static com.xerragnaroek.jikai.core.Core.GDM;

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

import net.dv8tion.jda.api.entities.Guild;

public class AnimeDB {

	private static AnimeDBImpl aDB = new AnimeDBImpl();
	private static boolean initialized = false;
	private final static Logger log = LoggerFactory.getLogger(AnimeDB.class);
	private static long updateRate = 6;
	private static ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();

	public static void init() {
		if (!initialized) {
			aDB.init();
			initialized = true;
			GDM.registerOnUniversalTimeZoneChanged((gId, zone) -> AnimeDB.addTimeZone(zone));
		} else {
			log.error("Already initialized!");
			throw new IllegalStateException("Already initialized!");
		}
	}

	public static Set<AnimeDayTime> getAnimesAiringOnWeekday(DayOfWeek day, Guild g) {
		assertInitialisation();
		return aDB.getAnimesAiringOnWeekday(day, g);
	}

	public static Set<AnimeDayTime> getAnimesAiringOnWeekday(DayOfWeek day, ZoneId z) {
		assertInitialisation();
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
		assertInitialisation();
		return aDB.getSeasonalAnimes();
	}

	public static void addTimeZone(ZoneId z) {
		assertInitialisation();
		aDB.addTimeZone(z, false);
	}

	public static void waitUntilLoaded() {
		assertInitialisation();
		while (aDB.isLoading()) {}
	}

	public static Set<AnimeDayTime> getSeasonalAnimesAdjusted(ZoneId tz) {
		assertInitialisation();
		return aDB.getSeasonalAnimesAdjusted(tz);
	}

	public static Anime getAnime(String title) {
		assertInitialisation();
		return aDB.getAnime(title);
	}

	private static void assertInitialisation() {
		if (!initialized) {
			log.error("AnimeBase hasn't been initialized yet!");
			throw new IllegalStateException("AnimeBase hasn't been initialized yet!");
		}
	}

	public static int getAnimeBaseVersion() {
		return GDM.getBotData().getAnimeBaseVersion();
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
}
