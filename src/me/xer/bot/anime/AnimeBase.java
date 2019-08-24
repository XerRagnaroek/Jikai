package me.xer.bot.anime;

import java.time.DayOfWeek;
import java.util.List;

import com.github.Doomsdayrs.Jikan4java.types.Main.Anime.Anime;

public class AnimeBase {

	private static AnimeBaseImpl aB;

	public static void init() {
		if (aB == null) {
			aB = new AnimeBaseImpl();
		}
	}

	public static List<AnimeDayTime> getAnimesAiringOnWeekday(DayOfWeek day) {
		return aB.getAnimesAiringOnWeekday(day);
	}

	public static List<Anime> getSeasonalAnimes() {
		return aB.getSeasonalAnimes();
	}
}
