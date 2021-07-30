
package com.github.xerragnaroek.jikai.jikai;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.github.xerragnaroek.jasa.Anime;
import com.github.xerragnaroek.jasa.TitleLanguage;
import com.github.xerragnaroek.jikai.anime.list.ALRHandler;
import com.github.xerragnaroek.jikai.anime.list.BigListHandler;
import com.github.xerragnaroek.jikai.commands.guild.CommandHandler;
import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.user.JikaiUserManager;
import com.github.xerragnaroek.jikai.util.BotUtils;

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

public class Jikai {
	private JikaiData jd;
	private BotData bd;
	private Map<TitleLanguage, ALRHandler> alrhs = new HashMap<>();
	private Map<String, BigListHandler> blhs = new HashMap<>();
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

	public void validateGuildRoles() throws Exception {
		log.debug("Validating roles...");
		Guild g = getGuild();
		CompletableFuture.allOf(valTlRoles(g), valAdultRole(g), valJURole(g)).join();
	}

	private CompletableFuture<Void> valTlRoles(Guild g) {
		Map<TitleLanguage, Long> tlRoles = jd.getTitleLanguageRoles();
		List<TitleLanguage> missingLangs = new LinkedList<>();
		for (TitleLanguage tl : TitleLanguage.values()) {
			if (!tlRoles.containsKey(tl)) {
				log.debug("No saved role for {}", tl);
				List<Role> roles = g.getRolesByName(tl.toString(), true);
				if (!roles.isEmpty()) {
					Role r = roles.get(0);
					jd.setTitleLanguageRole(tl, r.getIdLong());
					log.debug("Found role for {}, {}", tl, r.getId());
				} else {
					missingLangs.add(tl);
				}
			} else {
				if (g.getRoleById(tlRoles.get(tl)) == null) {
					log.debug("Saved role for {} is invalid", tl);
					missingLangs.add(tl);
				}
			}
		}
		List<CompletableFuture<Void>> cfs = new LinkedList<>();
		for (TitleLanguage tl : missingLangs) {
			cfs.add(g.createRole().submit().thenAccept(r -> {
				r.getManager().setName(tl.toString().toLowerCase()).queue();
				jd.setTitleLanguageRole(tl, r.getIdLong());
				log.debug("Created role for {}, {}", tl, r.getId());
			}));
		}
		return CompletableFuture.allOf(cfs.toArray(new CompletableFuture<?>[cfs.size()]));
	}

	private CompletableFuture<Void> valAdultRole(Guild g) {
		boolean createAdult = false;
		if (jd.getAdultRoleId() == 0) {
			log.debug("No saved adult role");
			createAdult = true;
		} else {
			if (g.getRoleById(jd.getAdultRoleId()) == null) {
				createAdult = true;
				log.debug("Saved adult role is invalid");
			} else {
				log.debug("Saved adult role is valid");
			}
		}
		if (createAdult) {
			return g.createRole().submit().thenAccept(r -> {
				r.getManager().setName("adult").queue();
				jd.setAdultRoleId(r.getIdLong());
				log.debug("Created adult role {}", r.getId());
			});
		} else {
			return CompletableFuture.completedFuture(null);
		}
	}

	private CompletableFuture<Void> valJURole(Guild g) {
		boolean create = false;
		if (jd.getJikaiUserRole() == 0) {
			log.debug("No saved user role");
			create = true;
		} else {
			if (g.getRoleById(jd.getJikaiUserRole()) == null) {
				log.debug("Saved user role is invalid");
				create = true;
			} else {
				log.debug("Saved user role is valid");
			}
		}
		if (create) {
			return g.createRole().submit().thenAccept(r -> {
				r.getManager().setName("jikai user").queue();
				jd.setJikaiUserRole(r.getIdLong());
				log.debug("Created user role {}", r.getId());
			});
		} else {
			return CompletableFuture.completedFuture(null);
		}
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

	public boolean hasListChannelAdultSet() {
		return jd.getListChannelAdultId() != 0;
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

	public void setupBigListHandlers() {
		BigListHandler adult = new BigListHandler(this, jd.getListChannelAdultId(), "adult");
		blhs.put("adult", adult);
		adult.setAnimeFilter(Anime::isAdult);
		adult.bindToChannelProperty(jd.listChannelAdultIdProperty());
		adult.validateList();
		BigListHandler big = new BigListHandler(this, jd.getListChannelBigId(), "big");
		blhs.put("big", big);
		big.setAnimeFilter(a -> !a.isAdult());
		big.bindToChannelProperty(jd.listChannelBigIdProperty());
		big.validateList();
	}

	public BigListHandler getBigListHandler(String identifier) {
		return blhs.get(identifier);
	}

	public Map<String, BigListHandler> getBigListHandlerMap() {
		return blhs;
	}

}
