package com.xerragnaroek.jikai.timer;

import java.util.concurrent.TimeUnit;

public class ReleaseTime {
	private long days;
	private long mins;
	private long hours;

	ReleaseTime(long millis) {
		days = TimeUnit.MILLISECONDS.toDays(millis);
		millis -= TimeUnit.DAYS.toMillis(days);
		hours = TimeUnit.MILLISECONDS.toHours(millis);
		millis -= TimeUnit.HOURS.toMillis(hours);
		mins = TimeUnit.MILLISECONDS.toMinutes(millis);
		if (mins == 59) {
			mins = 0;
			hours++;
		}
	}

	public long days() {
		return days;
	}

	public long hours() {
		return hours;
	}

	public long mins() {
		return mins;
	}

	@Override
	public String toString() {
		return String.format("%02d %s, %02d hours and %02d %s", days, (days == 1) ? "day" : "days", hours, mins, (mins == 1) ? "minute" : "minutes");
	}
}
