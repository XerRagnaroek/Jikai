
package com.github.xerragnaroek.jikai.jikai;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.github.xerragnaroek.jasa.TitleLanguage;
import com.github.xerragnaroek.jikai.anime.alrh.ALRHandler;
import com.github.xerragnaroek.jikai.commands.guild.CommandHandler;
import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.user.JikaiUserManager;
import com.github.xerragnaroek.jikai.util.BotUtils;

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

public class Jikai {
	private JikaiData jd;
	private BotData bd;
	private Map<TitleLanguage, ALRHandler> alrhs = new HashMap<>();
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
		MDC.put("id", String.valueOf(jd.getGuildId()));
		log.debug("Getting info channel");
		try {
			MDC.remove("id");
			return BotUtils.getTextChannelChecked(jd.getGuildId(), jd.getInfoChannelId());
		} catch (Exception e) {
			// noInfoCh();
			log.debug("Info channel either hasn't been set or doesn't exist");
			MDC.remove("id");
			throw e;
		}
	}

	public TextChannel getScheduleChannel() throws Exception {
		MDC.put("id", String.valueOf(jd.getGuildId()));
		log.debug("Getting Schedule channel");
		try {
			MDC.remove("id");
			return BotUtils.getTextChannelChecked(jd.getGuildId(), jd.getScheduleChannelId());
		} catch (Exception e) {
			// noSchedCh();
			log.debug("Schedule channel either hasn't been set or doesn't exist");
			MDC.remove("id");
			throw e;
		}
	}

	public TextChannel getAnimeChannel() throws Exception {
		MDC.put("id", String.valueOf(jd.getGuildId()));
		log.debug("Getting Anime channel");
		try {
			MDC.remove("id");
			return BotUtils.getTextChannelChecked(jd.getGuildId(), jd.getAnimeChannelId());
		} catch (Exception e) {
			// noAnimeCh();
			log.debug("Anime channel either hasn't been set or doesn't exist");
			MDC.remove("id");
			throw e;
		}
	}

	public TextChannel getListChannel(TitleLanguage lang) throws Exception {
		MDC.put("id", String.valueOf(jd.getGuildId()));
		log.debug("Getting List channel");
		try {
			MDC.remove("id");
			return BotUtils.getTextChannelChecked(jd.getGuildId(), jd.getListChannelId(lang));
		} catch (Exception e) {
			// noListCh();
			log.debug("List channel either hasn't been set or doesn't exist");
			MDC.remove("id");
			throw e;
		}
	}

	public Member getGuildOwner() throws Exception {
		Member m = getGuild().retrieveOwner().submit().exceptionally(e -> {
			MDC.put("id", String.valueOf(jd.getGuildId()));
			log.error("", e);
			MDC.remove("id");
			return null;
		}).get();
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
		MDC.put("id", String.valueOf(jd.getGuildId()));
		try {
			User owner = getGuildOwner().getUser();
			log.debug("Sending to owner '{}':\"{}\"", owner.getName(), msg);
			MessageBuilder bob = new MessageBuilder();
			bob.append("Greetings, ").append(owner).append("!\n").append(msg);
			BotUtils.sendPMChecked(owner, bob.build());
			MDC.remove("id");
			return true;
		} catch (Exception e) {
			log.error("Failed sending the message.");
			MDC.remove("id");
			return false;
		}
	}

	public BotData getBotData() {
		return bd;
	}

	public JikaiData getJikaiData() {
		return jd;
	}

	public ALRHandler getALRHandler(TitleLanguage lang) {
		return alrhs.get(lang);
	}

	public ALRHandler getALRHandler(long channelId) {
		for (TitleLanguage lang : TitleLanguage.values()) {
			if (jd.getListChannelId(lang) == channelId) {
				return getALRHandler(lang);
			}
		}
		return null;
	}

	public CommandHandler getCommandHandler() {
		return ch;
	}

	public void setALRHandler(ALRHandler alrh, TitleLanguage lang) {
		alrhs.put(lang, alrh);
	}

	public JikaiLocale getLocale() {
		return jd.getLocale();
	}

	private void noInfoCh() {
		try {
			sendToOwner(getLocale().getStringFormatted("g_error_no_info_ch", Arrays.asList("name", "pre"), getGuild().getName(), jd.getPrefix()));
		} catch (Exception e) {}
	}

	private void noSchedCh() {
		try {
			getInfoChannel().sendMessage(getLocale().getStringFormatted("g_error_no_sched_ch", Arrays.asList("owner", "pre"), getGuildOwner().getAsMention(), jd.getPrefix())).queue();
		} catch (Exception e) {}
	}

	private void noAnimeCh() {
		try {
			getInfoChannel().sendMessage(getLocale().getStringFormatted("g_error_no_anime_ch", Arrays.asList("owner", "pre"), getGuildOwner().getAsMention(), jd.getPrefix())).queue();
		} catch (Exception e) {}
	}

	private void noListCh() {
		try {
			getInfoChannel().sendMessage(getLocale().getStringFormatted("g_error_no_list_ch", Arrays.asList("owner", "pre"), getGuildOwner().getAsMention(), jd.getPrefix())).queue();
		} catch (Exception e) {}
	}

	public boolean hasCompletedSetup() {
		return jd != null && jd.hasCompletedSetup();
	}

	public boolean hasListChannelSet(TitleLanguage lang) {
		return jd.getListChannelId(lang) != 0;
	}

	public boolean hasAnimeChannelSet() {
		return jd.getAnimeChannelId() != 0;
	}

	public boolean hasScheduleChannelSet() {
		return jd.getScheduleChannelId() != 0;
	}

	public boolean hasInfoChannelSet() {
		return jd.getInfoChannelId() != 0;
	}

	public void validateMemberRoles() {
		log.debug("Validating user roles");
		JikaiUserManager jum = JikaiUserManager.getInstance();
		try {
			getGuild().getMembers().stream().map(Member::getIdLong).filter(jum::isKnownJikaiUser).map(jum::getUser).forEach(ju -> BotUtils.validateRoles(this, ju));
		} catch (Exception e) {
			log.error("Couldn't get guild!", e);
		}
	}

}
