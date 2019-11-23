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
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;

import com.github.Doomsdayrs.Jikan4java.types.Main.Anime.Anime;
import com.xerragnaroek.jikai.user.TitleLanguage;

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

	public String getTitle(TitleLanguage tt) {
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
