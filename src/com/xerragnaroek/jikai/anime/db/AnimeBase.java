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

public class AnimeBase {

	private static AnimeBaseImpl aB = new AnimeBaseImpl();
	private static boolean initialized = false;
	private final static Logger log = LoggerFactory.getLogger(AnimeBase.class);
	private static long updateRate = 6;
	private static ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();

	public static void init() {
		if (!initialized) {
			aB.init();
			initialized = true;
			GDM.registerOnUniversalTimeZoneChanged((gId, zone) -> AnimeBase.addTimeZone(zone));
		} else {
			log.error("Already initialized!");
			throw new IllegalStateException("Already initialized!");
		}
	}

	public static Set<AnimeDayTime> getAnimesAiringOnWeekday(DayOfWeek day, Guild g) {
		assertInitialisation();
		return aB.getAnimesAiringOnWeekday(day, g);
	}

	public static Set<AnimeDayTime> getAnimesAiringOnWeekday(DayOfWeek day, ZoneId z) {
		assertInitialisation();
		return aB.getAnimesAiringOnWeekday(day, z);
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
		return aB.getSeasonalAnimes();
	}

	public static void addTimeZone(ZoneId z) {
		assertInitialisation();
		aB.addTimeZone(z, false);
	}

	public static void waitUntilLoaded() {
		assertInitialisation();
		while (aB.isLoading()) {}
	}

	public static Set<AnimeDayTime> getSeasonalAnimesAdjusted(ZoneId tz) {
		assertInitialisation();
		return aB.getSeasonalAnimesAdjusted(tz);
	}

	public static Anime getAnime(String title) {
		assertInitialisation();
		return aB.getAnime(title);
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
		exec.scheduleAtFixedRate(aB::loadSeason, updateRate, updateRate, TimeUnit.HOURS);
	}

	public static void setUpdateRate(long rate) {
		updateRate = rate;
	}
}
