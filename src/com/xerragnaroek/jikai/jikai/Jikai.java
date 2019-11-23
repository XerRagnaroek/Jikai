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
package com.xerragnaroek.jikai.jikai;

import java.time.ZoneId;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xerragnaroek.jikai.anime.alrh.ALRHandler;
import com.xerragnaroek.jikai.anime.schedule.Scheduler;
import com.xerragnaroek.jikai.commands.guild.CommandHandler;
import com.xerragnaroek.jikai.core.Core;
import com.xerragnaroek.jikai.user.JikaiUserManager;
import com.xerragnaroek.jikai.util.BotUtils;
import com.xerragnaroek.jikai.util.Destroyable;
import com.xerragnaroek.jikai.util.prop.MapProperty;

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

public class Jikai implements Destroyable {
	private JikaiData jd;
	private BotData bd;
	private ALRHandler alrh;
	private CommandHandler ch;
	private Scheduler sched;
	private final Logger log;
	private final static Logger sLog = LoggerFactory.getLogger(Jikai.class);
	private static MapProperty<ZoneId, Integer> usedZones = new MapProperty<>();
	private static JikaiUserManager jum = new JikaiUserManager();

	public Jikai(long gId, JikaiManager jm) {
		this.jd = jm.jdm.get(gId);
		this.bd = jm.jdm.getBotData();
		ch = new CommandHandler(gId, this);
		log = LoggerFactory.getLogger(Jikai.class + "#" + gId);
	}

	public TextChannel getInfoChannel() throws Exception {
		log.debug("Getting info channel");
		try {
			return BotUtils.getTextChannelChecked(jd.getGuildId(), jd.getInfoChannelId());
		} catch (Exception e) {
			noInfoCh();
			throw e;
		}
	}

	public TextChannel getScheduleChannel() throws Exception {
		log.debug("Getting Schedule channel");
		try {
			return BotUtils.getTextChannelChecked(jd.getGuildId(), jd.getScheduleChannelId());
		} catch (Exception e) {
			noSchedCh();
			throw e;
		}
	}

	public TextChannel getAnimeChannel() throws Exception {
		log.debug("Getting anime channel");
		try {
			return BotUtils.getTextChannelChecked(jd.getGuildId(), jd.getAnimeChannelId());
		} catch (Exception e) {
			noAnimeCh();
			throw e;
		}
	}

	public TextChannel getListChannel() throws Exception {
		log.debug("Getting Status channel");
		try {
			return BotUtils.getTextChannelChecked(jd.getGuildId(), jd.getScheduleChannelId());
		} catch (Exception e) {
			noListCh();
			throw e;
		}
	}

	public Member getGuildOwner() throws Exception {
		Member m = getGuild().getOwner();
		if (m == null) {
			throw new Exception("Owner has either left the guild or has been deleted.");
		}
		return m;
	}

	public Guild getGuild() throws Exception {
		Guild g = Core.JDA.getGuildById(jd.getGuildId());
		if (g == null) {
			Core.JM.remove(jd.getGuildId());
			throw new Exception("Guild not found");
		}
		return g;
	}

	private boolean sendToOwner(String msg) {
		try {
			User owner = getGuildOwner().getUser();
			log.debug("Sending to owner '{}':\"{}\"", owner.getName(), msg);
			MessageBuilder bob = new MessageBuilder();
			bob.append("Greetings, ").append(owner).append("!\n").append(msg);
			BotUtils.sendPM(owner, bob.build());
			return true;
		} catch (Exception e) {
			log.error("Failed sending the message.");
			return false;
		}
	}

	public BotData getBotData() {
		return bd;
	}

	public JikaiData getJikaiData() {
		return jd;
	}

	public ALRHandler getALRHandler() {
		return alrh;
	}

	public CommandHandler getCommandHandler() {
		return ch;
	}

	public Scheduler getScheduler() {
		return sched;
	}

	private void noInfoCh() {
		try {
			sendToOwner("Your server '" + getGuild().getName() + "' doesn't have an info channel for Jikai set. Please use `" + jd.getTrigger() + "set info_channel <channel>`, otherwise Jikai can't send status updates.");
		} catch (Exception e) {}
	}

	private void noSchedCh() {
		try {
			getInfoChannel().sendMessage("No channel has been set for the schedule or it's been deleted! Set a new one with `" + jd.getTrigger() + "set schedule_channel <channel>`").queue();
		} catch (Exception e) {}
	}

	private void noAnimeCh() {
		try {
			getInfoChannel().sendMessage("No channel has been set for the anime release or it's been deleted! Set a new one with `" + jd.getTrigger() + "set anime_channel <channel>`").queue();
		} catch (Exception e) {}
	}

	private void noListCh() {
		try {
			getInfoChannel().sendMessage("No channel has been set for the anime list or it's been deleted! Set a new one with `" + jd.getTrigger() + "set list_channel <channel>`").queue();
		} catch (Exception e) {}
	}

	@Override
	public void destroy() {
		usedZones.clear();
	}

	public boolean hasCompletedSetup() {
		return jd.hasCompletedSetup();
	}

	public void setALRH(ALRHandler alrh) {
		this.alrh = alrh;
	}

	public void setScheduler(Scheduler sched) {
		this.sched = sched;
	}

	public static void addTimeZone(ZoneId zone) {
		usedZones.compute(zone, (z, c) -> {
			c = (c == null ? 1 : c + 1);
			sLog.debug("{}={}", z, c);
			return c;
		});
	}

	public static void removeTimeZone(ZoneId zone) {
		usedZones.compute(zone, (z, c) -> {
			c--;
			sLog.debug("{}={}", z, c);
			if (c == 0) {
				sLog.debug("Removed time zone");
				return null;
			}
			return c;
		});
	}

	public static MapProperty<ZoneId, Integer> timeZoneMapProperty() {
		return usedZones;
	}

	public static Set<ZoneId> getUsedTimeZones() {
		return usedZones.keySet();
	}

	public static JikaiUserManager getUserManager() {
		return jum;
	}

}
