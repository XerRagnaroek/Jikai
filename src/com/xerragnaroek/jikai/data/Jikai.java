package com.xerragnaroek.jikai.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xerragnaroek.jikai.anime.alrh.ALRHandler;
import com.xerragnaroek.jikai.anime.schedule.Scheduler;
import com.xerragnaroek.jikai.commands.CommandHandler;
import com.xerragnaroek.jikai.core.Core;
import com.xerragnaroek.jikai.timer.ReleaseTimeKeeper;
import com.xerragnaroek.jikai.util.BotUtils;
import com.xerragnaroek.jikai.util.Shutdownable;
import com.xerragnaroek.jikai.util.prop.Property;

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

public class Jikai implements Shutdownable {
	private JikaiData jd;
	private BotData bd;
	private ALRHandler alrh;
	private CommandHandler ch;
	private ReleaseTimeKeeper rtk;
	private Scheduler sched;
	private Property<Boolean> shutdown = new Property<>(false);
	private Property<Boolean> forceShutdown = new Property<>(false);
	private final Logger log;

	public Jikai(String gId, JikaiManager jm) {
		this.jd = jm.jdm.get(gId);
		this.bd = jm.jdm.getBotData();
		log = LoggerFactory.getLogger(Jikai.class);
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
			throw new Exception("Guild not found");
		}
		return g;
	}

	private boolean sendToOwner(String msg) {
		try {
			User owner = getGuildOwner().getUser();
			log.debug("Sending to owner '{}':\"{}\"", owner.getName(), msg);
			MessageBuilder bob = new MessageBuilder();
			bob.append("こんにちは, ").append(owner).append("!\n").append(msg);
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

	public ReleaseTimeKeeper getReleaseTimeKeeper() {
		return rtk;
	}

	public Scheduler getScheduler() {
		return sched;
	}

	private void noInfoCh() {

	}

	private void noSchedCh() {

	}

	private void noAnimeCh() {

	}

	private void noListCh() {

	}

	@Override
	public void shutdown(boolean now) {}

	@Override
	public void destroy() {}

	@Override
	public void waitForShutdown() {}

	@Override
	public Property<Boolean> shutdownProperty() {
		return shutdown;
	}

	@Override
	public boolean hasForceShutdownProperty() {
		return true;
	}

	@Override
	public Property<Boolean> forceShutdownProperty() {
		return forceShutdown;
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

	public void setRTK(ReleaseTimeKeeper rtk) {
		this.rtk = rtk;
	}

	public void setCH(CommandHandler ch) {
		this.ch = ch;
	}

}
