package com.xerragnaroek.jikai.anime.schedule;

import java.awt.Color;
import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.xerragnaroek.jikai.anime.db.AnimeDB;
import com.xerragnaroek.jikai.anime.db.AnimeDayTime;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

public class Schedule {

	private ZoneId zone;
	private List<MessageEmbed> embeds = new LinkedList<>();
	private final static DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm");

	public Schedule(ZoneId z) {
		zone = z;
		embeds = makeEmbedsForWeek(zone);
	}

	private List<MessageEmbed> makeEmbedsForWeek(ZoneId zone) {
		List<MessageEmbed> embeds = new LinkedList<>();
		AnimeDB.getAnimesMappedToDayOfAiring(zone).forEach((day, animes) -> embeds.add(makeEmbedForDay(day, animes)));
		return embeds;
	}

	private MessageEmbed makeEmbedForDay(DayOfWeek day, Set<AnimeDayTime> animes) {
		EmbedBuilder eb = new EmbedBuilder();
		eb.setTitle(day.toString()).setColor(Color.red);
		Map<String, Set<String>> timeMap = new TreeMap<>();
		animes.forEach(adt -> mapToTime(adt, timeMap));
		timeMap.forEach((time, titles) -> {
			eb.addField(time, "**-" + String.join("\n-", titles) + "**", false);
		});
		return eb.build();
	}

	private void mapToTime(AnimeDayTime adt, Map<String, Set<String>> map) {
		map.compute((adt.hasBroadcastTime()) ? timeFormat.format(adt.getBroadcastTime()) : "Unknown", (k, s) -> {
			if (s == null) {
				s = new TreeSet<>();
			}
			s.add(adt.getAnime().title);
			return s;
		});
	}

	void updateEmbeds() {
		embeds = makeEmbedsForWeek(zone);
	}

	public List<MessageEmbed> getEmbeds() {
		return embeds;
	}

}
