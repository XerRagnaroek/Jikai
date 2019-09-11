package com.xerragnaroek.bot.anime;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.util.List;

import com.github.Doomsdayrs.Jikan4java.types.Main.Anime.Anime;

import net.dv8tion.jda.api.entities.Guild;

public class AnimeBase {

	private static AnimeBaseImpl aB = new AnimeBaseImpl();

	public static void init() {
		aB.init();
	}

	public static List<AnimeDayTime> getAnimesAiringOnWeekday(DayOfWeek day, Guild g) {
		return aB.getAnimesAiringOnWeekday(day, g);
	}

	public static List<Anime> getSeasonalAnimes() {
		return aB.getSeasonalAnimes();
	}

	public static void addTimeZone(ZoneId z) {
		aB.addTimeZone(z, false);
	}

	public static void waitUntilLoaded() {
		while (aB.isLoading()) {}
	}
}
