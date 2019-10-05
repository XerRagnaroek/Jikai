package com.xerragnaroek.jikai.anime.schedule;

import java.time.ZoneId;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xerragnaroek.jikai.core.Core;
import com.xerragnaroek.jikai.data.GuildData;
import com.xerragnaroek.jikai.util.BotException;
import com.xerragnaroek.jikai.util.BotUtils;

import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;

public class Scheduler {

	private final Logger log;
	private final String gId;
	private final GuildData gd;
	private final ScheduleManager man;

	Scheduler(ScheduleManager man, String guildId) {
		gId = guildId;
		gd = Core.GDM.get(guildId);
		log = LoggerFactory.getLogger(Scheduler.class + "#" + gId);
		gd.timeZoneProperty().onChange((zo, zn) -> sendScheduleToGuild());
		this.man = man;
	}

	public void sendScheduleToGuild() {
		try {
			log.info("Sending schedule");
			GuildData gd = Core.GDM.get(gId);
			ZoneId zone = gd.getTimeZone();
			TextChannel tc = BotUtils.getTextChannelChecked(gId, gd.getScheduleChannelId());
			log.debug("TextChannel=" + tc.getName() + "#" + tc.getId());
			List<MessageEmbed> embeds = man.embedsForTimeZone(zone);
			if (gd.hasScheduleMessageIds()) {
				log.debug("Old schedule is present");
				List<String> ids = gd.getScheduleMessageIds();
				if (ids.size() == embeds.size()) {
					log.debug("Editing old schedule");
					editScheduleMsgs(tc, gd.getScheduleMessageIds(), embeds);
					return;
				} else {
					log.debug("Size difference between new and old schedule, deleting old one");
					deleteSchedule(tc, ids);
				}
			}
			sendSchedImpl(tc, embeds, gd);
			gd.save(true);
		} catch (BotException e) {
			log.error("Exception while sending schedule, passing it on.", e);
			//TODO handle Exceptions
		}
	}

	private void sendSchedImpl(TextChannel tc, List<MessageEmbed> embeds, GuildData gd) {
		AtomicInteger count = new AtomicInteger(0);
		List<String> ids = new LinkedList<>();
		embeds.forEach(me -> {
			try {
				ids.add(tc.sendMessage(me).submit().whenComplete((m, e) -> {
					if (e != null) {
						//TODO handle exception!
					} else {
						log.debug("Sent schedule message " + count.incrementAndGet() + "/" + ids.size());
					}
				}).get().getId());
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
		});
		gd.setScheduleMessageIds(ids);
	}

	private void deleteSchedule(TextChannel tc, List<String> ids) {
		AtomicInteger count = new AtomicInteger(0);
		ids.forEach(id -> {
			tc.deleteMessageById(id).queue(v -> log.debug("Deleted schedule msg " + count.incrementAndGet() + "/" + ids.size()));
		});
	}

	private void editScheduleMsgs(TextChannel tc, List<String> ids, List<MessageEmbed> embeds) {
		AtomicInteger count = new AtomicInteger(0);
		Iterator<MessageEmbed> it = embeds.iterator();
		ids.forEach(id -> {
			tc.retrieveMessageById(id).submit().thenCompose(m -> m.editMessage(it.next()).submit()).whenComplete((m, e) -> {
				if (e != null) {
					//TODO handle exception
				} else {
					log.debug("Edited schedule message " + count.incrementAndGet() + "/" + ids.size());
				}
			});
		});
	}

}
