package me.xer.bot.anime;

import java.time.DayOfWeek;
import java.time.LocalTime;

import com.github.Doomsdayrs.Jikan4java.types.Main.Anime.Anime;

/**
 * Stores an anime, it's broadcast day and time.
 * 
 * @author XerRagnaroek
 *
 */
public class AnimeDayTime implements Comparable<AnimeDayTime> {

	private Anime a;
	private DayOfWeek day;
	private LocalTime time;
	/**
	 * Unknown broadcast time.
	 */
	static final LocalTime UNKNOWN_TIME = LocalTime.of(12, 12, 12, 12);
	/**
	 * Unknown broadcast.
	 */
	static final AnimeDayTime UNKNOWN = new AnimeDayTime(null, DayOfWeek.TUESDAY, LocalTime.now());

	AnimeDayTime(Anime a, DayOfWeek day, LocalTime time) {
		this.a = a;
		this.day = day;
		this.time = time;
	}

	public Anime getAnime() {
		return a;
	}

	public DayOfWeek getDayOfWeek() {
		return day;
	}

	public LocalTime getBroadcastTime() {
		return time;
	}

	public boolean releasesOnDay(DayOfWeek day) {
		return this.day == day;
	}

	public boolean hasBroadcastTime() {
		return time != null && !time.equals(UNKNOWN_TIME);
	}

	public static boolean isKnown(AnimeDayTime adt) {
		return !adt.equals(UNKNOWN);
	}

	@Override
	public int compareTo(AnimeDayTime o) {
		return this.a.title.compareTo(o.a.title);
	}

	@Override
	public String toString() {
		return "[" + String.join(";", a.title, day.toString(), (time == null ? "Unknown" : time.toString())) + "]";
	}
}
