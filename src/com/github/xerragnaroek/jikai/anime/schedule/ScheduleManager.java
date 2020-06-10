package com.github.xerragnaroek.jikai.anime.schedule;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.jikai.Jikai;
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
				Core.EXEC.execute(() -> sendScheduleImpl(j, sched, now, data));
			});
		}
	}

	private static void sendScheduleImpl(Jikai j, Schedule sched, LocalDateTime now, byte[] data) {
		try {
			j.getScheduleChannel().sendMessage("The schedule has updated!").addFile(data, "Schedule_" + dtf.format(now) + ".png").queue();
		} catch (Exception e) {
			log.error("", e);
		}
	}

}
