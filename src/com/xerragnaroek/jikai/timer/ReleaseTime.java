package com.xerragnaroek.jikai.timer;

public class ReleaseTime {
	private int days;
	private int mins;
	private int hours;

	ReleaseTime(long millis) {
		days = (int) (millis / 86400000);
		millis = millis % 86400000;
		hours = (int) (millis / 3600000);
		millis = millis % 3600000;
		mins = (int) (millis / 60000);
	}

	public int days() {
		return days;
	}

	public int hours() {
		return hours;
	}

	public int mins() {
		return mins;
	}

	@Override
	public String toString() {
		return String.format(	"%02d days, %02d hours and %02d %s", days, hours, mins,
								(mins == 1) ? "minute" : "minutes");
	}
}
