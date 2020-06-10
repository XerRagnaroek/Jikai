
package com.github.xerragnaroek.jikai.jikai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jikai.anime.alrh.ALRHandler;
import com.github.xerragnaroek.jikai.commands.guild.CommandHandler;
import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.util.BotUtils;

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

public class Jikai {
	private JikaiData jd;
	private BotData bd;
	private ALRHandler alrh;
	private CommandHandler ch;
	private final Logger log;
	private final static Logger sLog = LoggerFactory.getLogger(Jikai.class);

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
		log.debug("Getting Anime channel");
		try {
			return BotUtils.getTextChannelChecked(jd.getGuildId(), jd.getAnimeChannelId());
		} catch (Exception e) {
			noAnimeCh();
			throw e;
		}
	}

	public TextChannel getListChannel() throws Exception {
		log.debug("Getting List channel");
		try {
			return BotUtils.getTextChannelChecked(jd.getGuildId(), jd.getListChannelId());
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

	public void setALRHandler(ALRHandler alrh) {
		this.alrh = alrh;
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

	public boolean hasCompletedSetup() {
		return jd != null && jd.hasCompletedSetup();
	}

	public void setALRH(ALRHandler alrh) {
		this.alrh = alrh;
	}

}
