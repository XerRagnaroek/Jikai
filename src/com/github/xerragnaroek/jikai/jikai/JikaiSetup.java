package com.github.xerragnaroek.jikai.jikai;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

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

	private void startSetup() {
		MDC.put("id", g.getId());
		log.info("Starting setup");
		nextStep();
	}

	private void nextStep() {
		log.debug("Next step");
		listen = false;
		try {
			switch (step) {
				case 0 -> makeSetupChannel();
				case 1 -> greetGuildOwner();
				case 2 -> language(null);
				case 3 -> prefix(null);
				case 4 -> timezone(null);
				case 5 -> confirmChannelCreation(null);
			}
			step++;
		} catch (InterruptedException | ExecutionException e) {
			BotUtils.logAndSendToDev(log, "Error during step" + step + ", notifying user", e);
			if (setTc != null) {
				setTc.sendMessage(j.getLocale().getStringFormatted("g_setup_error", Arrays.asList("error"), e.getMessage())).queue();
			}
		}
	}

	@Override
	public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
		if (listen) {
			if (event.getChannel().getIdLong() == setTc.getIdLong()) {
				try {
					String input = event.getMessage().getContentDisplay();
					if (step > 3 && input.equalsIgnoreCase("back")) {
						step--;
						nextStep();
						return;
					}
					boolean successful = false;
					// offset by 1 cause of the nextStepp step ++
					switch (step) {
						case 3 -> successful = language(input);
						case 4 -> successful = prefix(input);
						case 5 -> successful = timezone(input);
						case 6 -> successful = confirmChannelCreation(null);
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

	private void makeSetupChannel() throws InterruptedException, ExecutionException {
		owner = g.retrieveOwner().submit().get();
		setTc = g.createTextChannel("jikai_setup").addPermissionOverride(g.getPublicRole(), Arrays.asList(Permission.EMPTY_PERMISSIONS), Arrays.asList(Permission.VIEW_CHANNEL)).addPermissionOverride(owner, Arrays.asList(Permission.VIEW_CHANNEL, Permission.MESSAGE_MANAGE), Arrays.asList(Permission.EMPTY_PERMISSIONS)).addPermissionOverride(g.getSelfMember(), Arrays.asList(Permission.VIEW_CHANNEL, Permission.MESSAGE_MANAGE), Arrays.asList(Permission.EMPTY_PERMISSIONS)).submit().get();
		nextStep();
	}

	private void greetGuildOwner() throws InterruptedException, ExecutionException {
		setTc.sendMessage(j.getLocale().getStringFormatted("g_setup_greet", Arrays.asList("owner"), owner.getAsMention())).submit().get();
		nextStep();
	}

	private void makeAndSendEmbed(String title, String desc) throws InterruptedException, ExecutionException {
		EmbedBuilder eb = new EmbedBuilder();
		BotUtils.addJikaiMark(eb);
		eb.setTitle(title);
		eb.setDescription(desc);
		setTc.sendMessage(eb.build()).submit().get();
		listen = true;
	}

	private boolean language(String input) throws InterruptedException, ExecutionException {
		if (input == null) {
			JikaiLocale jl = j.getLocale();
			makeAndSendEmbed(jl.getString("g_setup_lang_title"), jl.getStringFormatted("g_setup_lang_desc", Arrays.asList("langs"), JikaiLocaleManager.getInstance().getAvailableLocales().toString()));
		} else {
			JikaiLocale loc = JikaiLocaleManager.getInstance().getLocale(input);
			if (loc != null) {
				j.getJikaiData().setLocale(loc);
				return true;
			}
		}
		return false;
	}

	private boolean prefix(String input) throws InterruptedException, ExecutionException {
		if (input == null) {
			JikaiLocale jl = j.getLocale();
			makeAndSendEmbed(jl.getString("g_setup_pre_title"), jl.getString("g_setup_pre_desc"));
		} else {
			j.getJikaiData().setPrefix(input);
		}
		return false;
	}

	private boolean timezone(String input) throws InterruptedException, ExecutionException {
		if (input == null) {
			JikaiLocale jl = j.getLocale();
			makeAndSendEmbed(jl.getString("g_setup_tz_title"), jl.getString("g_setup_tz_desc"));
		} else {
			try {
				ZoneId zId = ZoneId.of(input);
				j.getJikaiData().setTimeZone(zId);
			} catch (Exception e) {
				return false;
			}
		}
		return false;
	}

	private boolean confirmChannelCreation(String input) throws InterruptedException, ExecutionException {
		if (input == null) {
			JikaiLocale jl = j.getLocale();
			makeAndSendEmbed(jl.getString("g_setup_channels_title"), jl.getString("g_setup_channels_desc"));
		} else {
			switch (input.toLowerCase()) {
				case "y":
				case "yes": {
					createChannels();
					return true;
				}
				case "n":
				case "no": {
					return true;
				}
			}
		}
		return false;
	}

	private void createChannels() throws InterruptedException, ExecutionException {
		Category cat = g.createCategory("jikai").addPermissionOverride(g.getPublicRole(), Arrays.asList(Permission.VIEW_CHANNEL, Permission.MESSAGE_ADD_REACTION), Arrays.asList(Permission.MESSAGE_WRITE)).addPermissionOverride(g.getSelfMember(), Permission.ALL_CHANNEL_PERMISSIONS, 0l).complete();
		JikaiData jd = j.getJikaiData();
		TextChannel tc = cat.createTextChannel("jikai_list").submit().get();
		log.debug("Made jikai_list channel");
		jd.setListChannelId(tc.getIdLong());
		tc = cat.createTextChannel("jikai_schedule").submit().get();
		log.debug("Made jikai_schedule channel");
		jd.setScheduleChannelId(tc.getIdLong());
		tc = cat.createTextChannel("jikai_anime").submit().get();
		log.debug("Made jikai_anime channel");
		jd.setAnimeChannelId(tc.getIdLong());
		tc = cat.createTextChannel("jikai_info").submit().get();
		log.debug("Made jikai_info channel");
		jd.setInfoChannelId(tc.getIdLong());
	}

	private boolean confirmSettings(String input) {
		if (input != null) {
			JikaiLocale jl = j.getLocale();
			makeAndSendEmbed(jl.getString("g_setup_channels_title"), jl.getString("g_setup_channels_desc"));
		}
	}
}
