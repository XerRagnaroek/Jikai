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

import com.xerragnaroek.jikai.anime.db.AnimeDB;
import com.xerragnaroek.jikai.anime.db.AnimeDayTime;
import com.xerragnaroek.jikai.core.Core;
import com.xerragnaroek.jikai.jikai.Jikai;
import com.xerragnaroek.jikai.util.Manager;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

public class ScheduleManager extends Manager<Scheduler> {

	private final Map<ZoneId, List<MessageEmbed>> schedEmbeds = Collections.synchronizedMap(new HashMap<>());
	private static final DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm");

	public ScheduleManager() {
		super(Scheduler.class);

	}

	@Override
	public void init() {
		Core.JM.getGuildIds().forEach(this::registerNew);
		updateScheduleEmbeds();
		AnimeDB.dbVersionProperty().onChange((ov, nv) -> {
			if (isInitialized()) {
				updateScheduleEmbeds();
				updateSchedules();
			}
		});
		init.set(true);
	}

	@Override
	protected Scheduler makeNew(long gId) {
		return new Scheduler(this, gId);
	}

	private void updateScheduleEmbeds() {
		Jikai.getUsedTimeZones().forEach(z -> {
			schedEmbeds.put(z, makeEmbedsForWeek(z));
		});
	}

	public static List<MessageEmbed> makeEmbedsForWeek(ZoneId zone) {
		List<MessageEmbed> embeds = new LinkedList<>();
		AnimeDB.getAnimesMappedToDayOfAiring(zone).forEach((day, animes) -> embeds.add(makeEmbedForDay(day, animes)));
		return embeds;
	}

	private static MessageEmbed makeEmbedForDay(DayOfWeek day, Set<AnimeDayTime> animes) {
		EmbedBuilder eb = new EmbedBuilder();
		eb.setTitle(day.toString()).setColor(Color.red);
		Map<String, Set<String>> timeMap = new TreeMap<>();
		animes.forEach(adt -> mapToTime(adt, timeMap));
		timeMap.forEach((time, titles) -> {
			eb.addField(time, "**-" + String.join("\n-", titles) + "**", false);
		});
		return eb.build();
	}

	private static void mapToTime(AnimeDayTime adt, Map<String, Set<String>> map) {
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
		impls.values().forEach(sch -> Core.EXEC.execute(() -> sch.update()));
	}

}
