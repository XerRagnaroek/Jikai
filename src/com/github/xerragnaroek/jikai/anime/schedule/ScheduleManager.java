package com.github.xerragnaroek.jikai.anime.schedule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.xerragnaroek.jasa.Anime;
import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.jikai.Jikai;
import com.github.xerragnaroek.jikai.util.BotUtils;

import net.dv8tion.jda.api.EmbedBuilder;

public class ScheduleManager {

	private static Map<ZoneId, Schedule> schedules = new HashMap<>();
	private static final Logger log = LoggerFactory.getLogger(ScheduleManager.class);
	private static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.yy");

	private ScheduleManager() {}

	public static void init() {
		Core.JM.getUsedTimeZones().forEach(ScheduleManager::getSchedule);
	}

	public static Schedule getSchedule(ZoneId z) {
		return schedules.compute(z, (zone, s) -> {
			if (s == null) {
				s = new Schedule(z);
				s.runOnUpdate(ScheduleManager::sendScheduleToJikais);
				s.init();
			}
			return s;
		});
	}

	public static void sendScheduleToJikai(Jikai j) {
		Schedule sched = getSchedule(j.getJikaiData().getTimeZone());
		sendScheduleImpl(j, sched, LocalDateTime.now(), BotUtils.imageToByteArray(sched.getScheduleImage()));
	}

	private static void sendScheduleToJikais(Schedule sched) {
		if (!Core.INITIAL_LOAD.get()) {
			byte[] data = BotUtils.imageToByteArray(sched.getScheduleImage());
			LocalDateTime now = LocalDateTime.now(sched.getZoneId());
			Core.JM.getJikaisWithTimeZone(sched.getZoneId()).forEach(j -> {
				Core.executeLogException(() -> sendScheduleImpl(j, sched, now, data));
			});
		}
	}

	private static void sendScheduleImpl(Jikai j, Schedule sched, LocalDateTime now, byte[] data) {
		try {
			String fileName = "Schedule_" + dtf.format(now) + "-" + dtf.format(now.with(TemporalAdjusters.next(DayOfWeek.MONDAY))) + ".png";
			j.getScheduleChannel().sendFile(data, fileName).embed(BotUtils.addJikaiMark(new EmbedBuilder()).setDescription(j.getLocale().getString("g_sched_update")).setImage("attachment://" + fileName).build()).queue();
			// j.getScheduleChannel().sendMessage().addFile(data, "Schedule_" + dtf.format(now) +
			// ".png").queue();
		} catch (Exception e) {
			log.error("", e);
		}
	}

	public static void saveSchedule() throws IOException {
		log.debug("Saving {} schedules", schedules.size());
		Map<Anime, Set<String>> animeSchedMap = new HashMap<>();
		schedules.forEach((z, sched) -> {
			sched.getAnimeInSchedule().forEach(a -> {
				animeSchedMap.compute(a, (an, s) -> {
					s = s == null ? new TreeSet<>() : s;
					s.add(z.getId());
					return s;
				});
			});
		});
		Path loc = Paths.get(Core.DATA_LOC.toString(), "/schedules.json");
		ObjectMapper om = new ObjectMapper();
		ArrayNode node = om.createArrayNode();
		animeSchedMap.forEach((a, s) -> {
			ObjectNode on = node.addObject();
			on.set("anime", om.valueToTree(a));
			on.set("schedules", om.valueToTree(s));
		});
		String json = node.toPrettyString();
		Files.writeString(loc, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		log.debug("Saved schedules to '{}'", loc.toAbsolutePath());
	}

	public static void loadSchedules(Path p) {
		log.info("Loading schedules");
		try {
			String content = Files.readString(p);
			ObjectMapper om = new ObjectMapper();
			JsonNode node = om.readTree(content);
			Map<String, Set<Anime>> scheds = new HashMap<>();
			for (JsonNode n : node) {
				Anime a = om.treeToValue(n.get("anime"), Anime.class);
				Set<String> s = om.convertValue(n.get("schedules"), new TypeReference<Set<String>>() {});
				// Set<String> s = om.readerFor(new TypeReference<Set<String>>() {}).readValue(n.get("schedules"));
				s.forEach(str -> {
					scheds.compute(str, (sched, set) -> {
						set = set == null ? new HashSet<>() : set;
						set.add(a);
						return set;
					});
				});
			}
			scheds.forEach((s, anime) -> {
				ZoneId zone = ZoneId.of(s);
				Schedule sched = new Schedule(zone);
				sched.runOnUpdate(ScheduleManager::sendScheduleToJikais);
				sched.init(anime);
				schedules.put(zone, sched);
			});
			log.info("Loaded {} schedules!", schedules.size());
		} catch (IOException e) {
			Core.ERROR_LOG.error("Failed reading schedules!", e);
		}

		// TypeFactory tf = om.getTypeFactory();
		// JavaType inner = tf.constructParametricType(Set.class, Set.class, String.class);
		// JavaType map = tf.constructMapLikeType(HashMap.class, tf.constructType(new TypeReference<Anime>()
		// {}), inner);
		// Map<Anime, Set<String>> animeSchedMap = new ObjectMapper().readValue(content,
		// Map<Anime,Set<String>>.class);

	}
}
