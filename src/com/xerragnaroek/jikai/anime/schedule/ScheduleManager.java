package com.xerragnaroek.jikai.anime.schedule;

import java.awt.Color;
import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ForkJoinPool;

import com.xerragnaroek.jikai.anime.db.AnimeDB;
import com.xerragnaroek.jikai.anime.db.AnimeDayTime;
import com.xerragnaroek.jikai.core.Core;
import com.xerragnaroek.jikai.util.JikaiManager;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

public class ScheduleManager extends JikaiManager<Scheduler> {

	private final Map<ZoneId, List<MessageEmbed>> schedEmbeds = Collections.synchronizedMap(new HashMap<>());
	private final DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm");

	public ScheduleManager() {
		super(Scheduler.class);

	}

	@Override
	public void init() {
		Core.GDM.getGuildIds().forEach(this::registerNew);
		updateScheduleEmbeds();
		Core.GDM.getBotData().animeBaseVersionProperty().onChange((ov, nv) -> {
			if (isInitialized()) {
				updateScheduleEmbeds();
				updateSchedules();
			}
		});
		init.set(true);
	}

	@Override
	protected Scheduler makeNew(String gId) {
		return new Scheduler(this, gId);
	}

	private void updateScheduleEmbeds() {
		Map<ZoneId, Set<MessageEmbed>> map = new HashMap<>();
		Core.GDM.getUsedTimeZones().forEach(z -> {
			schedEmbeds.put(z, makeEmbedsForWeek(z));
		});
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

	List<MessageEmbed> embedsForTimeZone(ZoneId z) {
		return new LinkedList<>(schedEmbeds.get(z));
	}

	private void updateSchedules() {
		impls.values().forEach(sch -> ForkJoinPool.commonPool().execute(() -> sch.update()));
	}

}
