package com.xerragnaroek.bot.anime;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.Doomsdayrs.Jikan4java.types.Main.Anime.Anime;
import com.xerragnaroek.bot.config.ConfigManager;
import com.xerragnaroek.bot.config.ConfigOption;

import net.dv8tion.jda.api.entities.Guild;

public class AnimeBase {

	private static AnimeBaseImpl aB = new AnimeBaseImpl();
	private static boolean initialized = false;
	private final static Logger log = LoggerFactory.getLogger(AnimeBase.class);

	public static void init() {
		if (!initialized) {
			aB.init();
			initialized = true;
			ConfigManager.registerOptionChangedConsumerForAllConfigs(ConfigOption.TIMEZONE, AnimeBase::addTimeZone);
		} else {
			log.error("Already initialized!");
			throw new IllegalStateException("Already initialized!");
		}
	}

	public static List<AnimeDayTime> getAnimesAiringOnWeekday(DayOfWeek day, Guild g) {
		assertInitialisation();
		return aB.getAnimesAiringOnWeekday(day, g);
	}

	public static List<Anime> getSeasonalAnimes() {
		assertInitialisation();
		return aB.getSeasonalAnimes();
	}

	public static void addTimeZone(String z) {
		assertInitialisation();
		aB.addTimeZone(ZoneId.of(z), false);
	}

	public static void waitUntilLoaded() {
		assertInitialisation();
		while (aB.isLoading()) {}
	}

	private static void assertInitialisation() {
		if (!initialized) {
			log.error("AnimeBase hasn't been initialized yet!");
			throw new IllegalStateException("AnimeBase hasn't been initialized yet!");
		}
	}
}
