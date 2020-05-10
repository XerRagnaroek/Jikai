package com.github.xerragnaroek.jikai.anime.schedule;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.util.BotUtils;

public class ScheduleManager {

	private static Map<ZoneId, Schedule> schedules = new HashMap<>();
	private static final Logger log = LoggerFactory.getLogger(ScheduleManager.class);
	private static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.yy HH_mm");

	private ScheduleManager() {}

	public static void init() {
		Core.JM.getUsedTimeZones().forEach(ScheduleManager::getSchedule);
	}

	public static Schedule getSchedule(ZoneId z) {
		return schedules.compute(z, (zone, s) -> {
			if (s == null) {
				s = new Schedule(z);
				s.runOnUpdate(ScheduleManager::updateJikais);
				s.init();
			}
			return s;
		});
	}

	static void updateJikais(Schedule sched) {
		if (!Core.INITIAL_LOAD) {
			byte[] data = BotUtils.imageToByteArray(sched.getScheduleImage());
			LocalDateTime now = LocalDateTime.now(sched.getZoneId());
			Core.JM.getJikaisWithTimeZone(sched.getZoneId()).forEach(j -> {
				Core.EXEC.execute(() -> {
					try {
						j.getScheduleChannel().sendMessage("The schedule has updated!").addFile(data, "Schedule_" + dtf.format(now) + ".png").queue();
					} catch (Exception e) {
						log.error("", e);
					}
				});
			});
		}
	}

}
