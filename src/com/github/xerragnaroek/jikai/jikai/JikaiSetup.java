package com.github.xerragnaroek.jikai.jikai;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.github.xerragnaroek.jasa.TitleLanguage;
import com.github.xerragnaroek.jikai.anime.schedule.ScheduleManager;
import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocaleManager;
import com.github.xerragnaroek.jikai.util.BotUtils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

/**
 * 
 */
public class JikaiSetup extends ListenerAdapter {

	private Guild g;
	private TextChannel setTc;
	private final Logger log;
	private int step;
	private Member owner;
	private Jikai j;
	private boolean listen = false;

	private JikaiSetup(Guild g) {
		this.g = g;
		Core.JDA.addEventListener(this);
		log = LoggerFactory.getLogger(JikaiSetup.class);
		j = Core.JM.registerNew(g);
	}

	public static void runSetup(Guild g) {
		try {
			Thread.sleep(1000);
			new JikaiSetup(g).startSetup();
		} catch (InterruptedException e) {
			BotUtils.logAndSendToDev(Core.ERROR_LOG, "", e);
		}
	}

	private void startSetup() {
		MDC.put("id", g.getId());
		log.info("Starting setup");
		try {
			makeSetupChannel();
			greetGuildOwner();
			nextStep();
		} catch (InterruptedException | ExecutionException e) {
			BotUtils.logAndSendToDev(log, "Error during step" + step + ", notifying user", e);
			if (setTc != null) {
				setTc.sendMessage(j.getLocale().getStringFormatted("g_setup_error", Arrays.asList("error"), e.getMessage())).queue();
			}
		}
		MDC.remove("id");
	}

	private void nextStep() {
		MDC.put("id", g.getId());
		log.debug("Next step, current step {}", step);
		listen = false;
		try {
			switch (step) {
				case 0 -> language(null);
				case 1 -> prefix(null);
				case 2 -> timezone(null);
				case 3 -> confirmChannelCreation(null);
				case 4 -> confirmSettings(null);
			}
			step++;
		} catch (InterruptedException | ExecutionException e) {
			BotUtils.logAndSendToDev(log, "Error during step" + step + ", notifying user", e);
			if (setTc != null) {
				setTc.sendMessage(j.getLocale().getStringFormatted("g_setup_error", Arrays.asList("error"), e.getMessage())).queue();
			}
		}
		MDC.remove("id");
	}

	@Override
	public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
		MDC.put("id", g.getId());
		if (listen) {
			if (!event.getAuthor().isBot()) {
				if (event.getChannel().getIdLong() == setTc.getIdLong()) {
					try {
						String input = event.getMessage().getContentDisplay();
						log.debug("Received input '{}'", input);
						if (step > 1 && input.equalsIgnoreCase("back")) {
							step -= 2;
							nextStep();
							return;
						}
						boolean successful = false;
						// offset by 1 cause of the nextStep step ++
						switch (step) {
							case 1 -> successful = language(input);
							case 2 -> successful = prefix(input);
							case 3 -> successful = timezone(input);
							case 4 -> successful = confirmChannelCreation(input);
							case 5 -> successful = confirmSettings(input);
						}
						if (successful) {
							nextStep();
						} else {
							setTc.sendMessage(j.getLocale().getString("g_setup_invalid")).submit().get();
						}
					} catch (InterruptedException | ExecutionException e) {
						BotUtils.logAndSendToDev(log, "Error during step" + step + ", notifying user", e);
						setTc.sendMessage(j.getLocale().getStringFormatted("g_setup_error", Arrays.asList("error"), e.getMessage())).queue();
					}
				}
			}
		}
		MDC.remove("id");
	}

	private void makeSetupChannel() throws InterruptedException, ExecutionException {
		MDC.put("id", g.getId());
		log.debug("Creating setup channel");
		owner = g.retrieveOwner().submit().get();
		setTc = g.createTextChannel("jikai_setup").addPermissionOverride(g.getPublicRole(), Arrays.asList(Permission.EMPTY_PERMISSIONS), Arrays.asList(Permission.VIEW_CHANNEL)).addPermissionOverride(owner, Arrays.asList(Permission.VIEW_CHANNEL, Permission.MESSAGE_MANAGE), Arrays.asList(Permission.EMPTY_PERMISSIONS)).addPermissionOverride(g.getSelfMember(), Arrays.asList(Permission.VIEW_CHANNEL, Permission.MESSAGE_MANAGE), Arrays.asList(Permission.EMPTY_PERMISSIONS)).submit().get();
		log.debug("Successfully created setup channel");
		MDC.remove("id");
	}

	private void greetGuildOwner() throws InterruptedException, ExecutionException {
		MDC.put("id", g.getId());
		log.debug("Greeting owner");
		setTc.sendMessage(j.getLocale().getStringFormatted("g_setup_greet", Arrays.asList("owner"), owner.getAsMention())).submit().get();
		MDC.remove("id");
	}

	private void makeAndSendEmbed(String title, String desc) throws InterruptedException, ExecutionException {
		EmbedBuilder eb = new EmbedBuilder();
		BotUtils.addJikaiMark(eb);
		eb.setTitle(title);
		eb.setDescription(desc);
		setTc.sendMessageEmbeds(eb.build()).submit().get();
		listen = true;
	}

	private boolean language(String input) throws InterruptedException, ExecutionException {
		MDC.put("id", g.getId());
		log.debug("Language");
		if (input == null) {
			JikaiLocale jl = j.getLocale();
			makeAndSendEmbed(jl.getString("g_setup_lang_title"), jl.getStringFormatted("g_setup_lang_desc", Arrays.asList("langs"), JikaiLocaleManager.getInstance().getLocaleIdentifiers().toString()));
			log.debug("Sent first language message");
		} else {
			JikaiLocale loc = JikaiLocaleManager.getInstance().getLocale(input);
			if (loc != null) {
				log.debug("Found locale: {}", loc);
				j.getJikaiData().setLocale(loc);
				return true;
			}
		}
		MDC.remove("id");
		return false;
	}

	private boolean prefix(String input) throws InterruptedException, ExecutionException {
		MDC.put("id", g.getId());
		log.debug("Prefix");
		if (input == null) {
			JikaiLocale jl = j.getLocale();
			makeAndSendEmbed(jl.getString("g_setup_pre_title"), jl.getString("g_setup_pre_desc"));
			log.debug("Sent first prefix message");
		} else {
			j.getJikaiData().setPrefix(input);
			return true;
		}
		MDC.remove("id");
		return false;
	}

	private boolean timezone(String input) throws InterruptedException, ExecutionException {
		MDC.put("id", g.getId());
		log.debug("Timezone");
		if (input == null) {
			JikaiLocale jl = j.getLocale();
			makeAndSendEmbed(jl.getString("g_setup_tz_title"), jl.getString("g_setup_tz_desc"));
			log.debug("Sent first timezone message");
		} else {
			try {
				ZoneId zId = ZoneId.of(input);
				j.getJikaiData().setTimeZone(zId);
				return true;
			} catch (Exception e) {
				log.debug("invalid timezone");
				return false;
			}
		}
		MDC.remove("id");
		return false;
	}

	private boolean confirmChannelCreation(String input) throws InterruptedException, ExecutionException {
		MDC.put("id", g.getId());
		log.debug("Confirm channel creation");
		if (input == null) {
			JikaiLocale jl = j.getLocale();
			makeAndSendEmbed(jl.getString("g_setup_channels_title"), jl.getString("g_setup_channels_desc"));
			log.debug("Sent first channel creation message");
		} else {
			switch (input.toLowerCase()) {
				case "y":
				case "yes": {
					log.debug("Channel creation confirmed");
					createChannels();
					return true;
				}
				case "n":
				case "no": {
					log.debug("Owner wants to manually set channels");
					return true;
				}
			}
		}
		MDC.remove("id");
		return false;
	}

	private void createChannels() throws InterruptedException, ExecutionException {
		MDC.put("id", g.getId());
		log.debug("Creating channels...");
		Category cat = g.createCategory("jikai").addPermissionOverride(g.getPublicRole(), Arrays.asList(Permission.VIEW_CHANNEL, Permission.MESSAGE_ADD_REACTION), Arrays.asList(Permission.MESSAGE_WRITE)).addPermissionOverride(g.getSelfMember(), Permission.ALL_CHANNEL_PERMISSIONS, 0l).complete();
		JikaiData jd = j.getJikaiData();
		TextChannel tc = cat.createTextChannel("jikai_list").submit().get();
		log.debug("Made jikai_list channel");
		jd.setListChannelId(tc.getIdLong(), TitleLanguage.ROMAJI);
		tc = cat.createTextChannel("jikai_schedule").submit().get();
		log.debug("Made jikai_schedule channel");
		jd.setScheduleChannelId(tc.getIdLong());
		tc = cat.createTextChannel("jikai_anime").submit().get();
		log.debug("Made jikai_anime channel");
		jd.setAnimeChannelId(tc.getIdLong());
		tc = cat.createTextChannel("jikai_info").submit().get();
		log.debug("Made jikai_info channel");
		jd.setInfoChannelId(tc.getIdLong());
		log.debug("Successfully created all 4 channels");
		MDC.remove("id");
	}

	private boolean confirmSettings(String input) throws InterruptedException, ExecutionException {
		MDC.put("id", g.getId());
		log.debug("Confirm Settings");
		if (input == null) {
			JikaiLocale jl = j.getLocale();
			makeAndSendEmbed(jl.getString("g_setup_settings_title"), jl.getStringFormatted("g_setup_settings_desc", Arrays.asList("lang", "pre", "tz"), j.getLocale().getLanguageName(), j.getJikaiData().getPrefix(), j.getJikaiData().getTimeZone().getId()));
			log.debug("Sent first confirm settings message");
		} else {
			switch (input.toLowerCase()) {
				case "y":
				case "yes": {
					log.debug("Owner confirmed settings");
					finishSetup();
					return true;
				}
				case "n":
				case "no": {
					step = 0;
					return true;
				}
			}
			if (input.equalsIgnoreCase("yes") || input.equalsIgnoreCase("y")) {
				log.debug("Owner confirmed settings");
				finishSetup();
				return true;
			}
		}
		MDC.remove("id");
		return false;
	}

	private void finishSetup() throws InterruptedException, ExecutionException {
		MDC.put("id", g.getId());
		log.debug("Finishing setup...");
		Core.JDA.removeEventListener(this);
		JikaiLocale loc = j.getLocale();
		makeAndSendEmbed(loc.getString("g_setup_finished_title"), loc.getStringFormatted("g_setup_finished_desc", Arrays.asList("pre"), j.getJikaiData().getPrefix()));
		JikaiData jd = j.getJikaiData();
		jd.setSetupCompleted(true);
		jd.save(true);
		j.setupAnimeListHandlers();
		log.info("Setup completed");
		/*
		 * if (j.hasListChannelSet()) {
		 * log.debug("Sending list...");
		 * j.getALRHandler().sendList();
		 * }
		 */
		if (j.hasScheduleChannelSet()) {
			log.debug("Sending schedule...");
			ScheduleManager.sendScheduleToJikai(j);
		}
		MDC.remove("id");
	}
}
