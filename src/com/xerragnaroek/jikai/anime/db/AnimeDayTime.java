package com.xerragnaroek.jikai.anime.db;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;

import com.github.Doomsdayrs.Jikan4java.types.Main.Anime.Anime;
import com.xerragnaroek.jikai.user.TitleType;

/**
 * Stores an anime, it's broadcast day and time.
 * 
 * @author XerRagnaroek
 *
 */
public class AnimeDayTime implements Comparable<AnimeDayTime> {

	private Anime a;
	private ZonedDateTime zdt;
	/**
	 * Unknown broadcast time.
	 */
	static final LocalTime UNKNOWN_TIME = LocalTime.of(12, 12, 12, 12);
	/**
	 * Unknown broadcast.
	 */
	static final AnimeDayTime UNKNOWN = new AnimeDayTime(null, null);
	private static DateTimeFormatter date = DateTimeFormatter.ofPattern("dd.MM.uuuu");
	private static DateTimeFormatter timeF = DateTimeFormatter.ofPattern("HH:mm");

	AnimeDayTime(Anime a, ZonedDateTime zdt) {
		this.a = a;
		this.zdt = zdt;
	}

	public Anime getAnime() {
		return a;
	}

	public DayOfWeek getDayOfWeek() {
		return zdt.getDayOfWeek();
	}

	public LocalTime getBroadcastTime() {
		return zdt.toLocalTime();
	}

	public boolean releasesOnDay(DayOfWeek day) {
		return getDayOfWeek() == day;
	}

	public boolean hasBroadcastTime() {
		LocalTime time = getBroadcastTime();
		return time != null && !time.equals(UNKNOWN_TIME);
	}

	public ZonedDateTime getZonedDateTime() {
		return zdt;
	}

	public void updateZDTToNextAirDate() {
		zdt = zdt.with(TemporalAdjusters.next(getDayOfWeek()));
	}

	public static boolean isKnown(AnimeDayTime adt) {
		return !adt.equals(UNKNOWN);
	}

	public String getTitle(TitleType tt) {
		String str;
		switch (tt) {
		case ENGLISH:
			str = a.title_english;
			if (str == null || str.isEmpty()) {
				str = a.title_synonyms.get(0);
			}
			return str;
		case JAPANESE:
			return a.title_japanese;
		default:
			return a.title;
		}
	}

	public String getReleaseDateTimeFormatted() {
		return String.format("%s, %s at %s\n", getDayOfWeek(), date.format(zdt), timeF.format(zdt));
	}

	@Override
	public int compareTo(AnimeDayTime o) {
		return this.a.title.compareTo(o.a.title);
	}

	@Override
	public String toString() {
		return "[" + String.join(";", a.title, getDayOfWeek().toString(), (!hasBroadcastTime() ? "Unknown" : getBroadcastTime().toString())) + "]";
	}
}
