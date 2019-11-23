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

import java.time.ZoneId;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xerragnaroek.jikai.core.Core;
import com.xerragnaroek.jikai.jikai.Jikai;
import com.xerragnaroek.jikai.jikai.JikaiData;
import com.xerragnaroek.jikai.util.Destroyable;

import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;

public class Scheduler implements Destroyable {

	private final Logger log;
	private final JikaiData jd;
	private final ScheduleManager man;
	private final Jikai j;

	Scheduler(ScheduleManager man, long guildId) {
		j = Core.JM.get(guildId);
		j.setScheduler(this);
		jd = j.getJikaiData();
		log = LoggerFactory.getLogger(Scheduler.class + "#" + guildId);
		jd.timeZoneProperty().onChange((zo, zn) -> sendScheduleToGuild());
		this.man = man;
	}

	public void sendScheduleToGuild() {
		try {
			log.info("Sending schedule");
			ZoneId zone = jd.getTimeZone();
			TextChannel tc = j.getScheduleChannel();
			log.debug("TextChannel=" + tc.getName() + "#" + tc.getId());
			List<MessageEmbed> embeds = man.embedsForTimeZone(zone);
			if (jd.hasScheduleMessageIds()) {
				log.debug("Old schedule is present");
				List<Long> ids = jd.getScheduleMessageIds();
				if (ids.size() == embeds.size()) {
					log.debug("Editing old schedule");
					editScheduleMsgs(tc, ids, embeds);
					return;
				} else {
					log.debug("Size difference between new and old schedule, deleting old one");
					deleteSchedule(tc, ids);
				}
			}
			sendSchedImpl(tc, embeds);
			jd.save(true);
		} catch (Exception e) {}
	}

	void update() {
		try {
			sendScheduleToGuild();
			j.getInfoChannel().sendMessage("Schedule has been updated!").queue();
		} catch (Exception e) {}
	}

	private void sendSchedImpl(TextChannel tc, List<MessageEmbed> embeds) {
		AtomicInteger count = new AtomicInteger(0);
		List<Long> ids = new LinkedList<>();
		embeds.forEach(me -> {
			try {
				ids.add(tc.sendMessage(me).submit().whenComplete((m, e) -> {
					if (e != null) {
						//TODO handle exception!
					} else {
						log.debug("Sent schedule message " + count.incrementAndGet() + "/" + ids.size());
					}
				}).get().getIdLong());
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
		});
		jd.setScheduleMessageIds(ids);
	}

	private void deleteSchedule(TextChannel tc, List<Long> ids) {
		AtomicInteger count = new AtomicInteger(0);
		ids.forEach(id -> {
			tc.deleteMessageById(id).queue(v -> log.debug("Deleted schedule msg " + count.incrementAndGet() + "/" + ids.size()));
		});
	}

	private void editScheduleMsgs(TextChannel tc, List<Long> ids, List<MessageEmbed> embeds) {
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

	@Override
	public void destroy() {}

}
