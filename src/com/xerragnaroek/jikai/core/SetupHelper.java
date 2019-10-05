package com.xerragnaroek.jikai.core;

import static com.xerragnaroek.jikai.core.Core.GDM;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xerragnaroek.jikai.anime.alrh.ALRHandler;
import com.xerragnaroek.jikai.anime.schedule.Scheduler;
import com.xerragnaroek.jikai.data.GuildData;
import com.xerragnaroek.jikai.timer.RTKManager;

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class SetupHelper extends ListenerAdapter {

	private Guild g;
	private boolean listen = false;
	private TextChannel setTc;
	private final Logger log;

	private SetupHelper(Guild g) {
		this.g = g;
		Core.JDA.addEventListener(this);
		log = LoggerFactory.getLogger(SetupHelper.class + "#" + g.getId());
	}

	public static void runSetup(Guild g) {
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		new SetupHelper(g).runSetup();
	}

	private TextChannel makeSetupChannel() {
		return g.createTextChannel("jikai_setup").addPermissionOverride(g.getPublicRole(), Arrays.asList(Permission.EMPTY_PERMISSIONS), Arrays.asList(Permission.VIEW_CHANNEL)).addPermissionOverride(g.getOwner(), Arrays.asList(Permission.VIEW_CHANNEL, Permission.MESSAGE_MANAGE), Arrays.asList(Permission.EMPTY_PERMISSIONS)).addPermissionOverride(g.getSelfMember(), Arrays.asList(Permission.VIEW_CHANNEL, Permission.MESSAGE_MANAGE), Arrays.asList(Permission.EMPTY_PERMISSIONS)).complete();
	}

	private void runSetup() {
		log.info("Running setup");
		setTc = makeSetupChannel();
		log.debug("Made setup channel");
		setTc.sendMessage(g.getOwner().getAsMention()).complete();
		MessageBuilder mb = new MessageBuilder("\nThis is where the bot setup will take place.\n");
		mb.append("The bot will create 4 textchannels:\n**jikai_list** - for the anime list\n**jikai_schedule** - for the anime release schedule\n**jikai_anime** - for upcoming release notifications\n**jikai_info** - for status updates and information concerning the bot\nFurthermore also set these settings:\n");
		mb.append("**trigger** = 't' - what needs to be written before a command for the bot to recognize it. E.g. !help\n");
		mb.append("**timezone** = 'Europe/Berlin' - the timezone that will be used to adjust release updates.\n");
		mb.append("You can change the latter settings via the set commands (see !help for more info)!\n");
		mb.append("Feel free to move or rename the channels, the bot is using their unique ID, so the name doesn't matter.");
		mb.append("Once the setup is done, the bot will send the anime list and the schedule.\n");
		mb.append(releaseString());
		mb.append("Commence setup? *yes(y)*");
		setTc.sendMessage(mb.build()).complete();
		listen = true;
	}

	private String releaseString() {
		StringBuilder bob = new StringBuilder("Updates for releases will be sent daily");
		RTKManager rtkm = Core.RTKM;
		int dt = rtkm.getDayThreshold();
		int ht = rtkm.getHourThreshold();
		long rate = rtkm.getUpdateRate();
		if (!(dt == 0 && ht == 0)) {
			bob.append(" or every %3$03d minutes when the anime airs in ");
			if (dt != 0) {
				bob.append("%1$d days");
				if (ht != 0) {
					bob.append(" and ");
				}
			}
			if (ht != 0) {
				bob.append("%2$02d hours");
			}
			bob.append(" or less");
		}
		bob.append(".\n");
		return String.format(bob.toString(), dt, ht, rate);

	}

	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		if (listen) {
			if (event.getGuild().getIdLong() == g.getIdLong()) {
				if (event.getTextChannel().equals(setTc)) {
					if (!event.getAuthor().equals(g.getSelfMember().getUser())) {
						continueSetup(event.getMessage().getContentDisplay());
					}
				}
			}
		}
	}

	private void continueSetup(String msg) {
		if (msg.equalsIgnoreCase("y") || msg.equalsIgnoreCase("yes")) {
			listen = false;
			Core.JDA.removeEventListener(this);
			setTc.sendMessage("No further input is required, feel free to delete this channel.").complete();
			setup();
		}
	}

	private void setup() {
		listen = false;
		log.info("Owner agreed to setup");
		GuildData gd = GDM.registerNew(g);
		ALRHandler alrh = Core.ALRHM.registerNew(g);
		Core.CHM.registerNew(g);
		Core.RTKM.registerNew(g);
		Scheduler sched = Core.SM.registerNew(g);
		Category cat = g.createCategory("jikai").addPermissionOverride(g.getPublicRole(), Arrays.asList(Permission.VIEW_CHANNEL, Permission.MESSAGE_ADD_REACTION), Arrays.asList(Permission.MESSAGE_WRITE)).addPermissionOverride(g.getSelfMember(), Permission.ALL_CHANNEL_PERMISSIONS, 0l).complete();
		TextChannel tc = cat.createTextChannel("jikai_list").complete();
		log.debug("Made jikai_list channel");
		gd.setListChannelId(tc.getId());
		tc = cat.createTextChannel("jikai_schedule").complete();
		log.debug("Made jikai_schedule channel");
		gd.setScheduleChannelId(tc.getId());
		tc = cat.createTextChannel("jikai_anime").complete();
		log.debug("Made jikai_anime channel");
		gd.setAnimeChannelId(tc.getId());
		tc = cat.createTextChannel("jikai_info").complete();
		log.debug("Made jikai_info channel");
		gd.setInfoChannelId(tc.getId());
		gd.setSetupCompleted(true);
		gd.save(true);
		log.info("Setup completed");
		setTc.sendMessage("The setup is complete. Commands are by default " + (gd.areCommandsEnabled() ? "enabled" : "disabled") + ".\nYou can change that by calling !enable/disable_commands").complete();
		setTc.sendMessage("Send `!help` for a list of all commands you have permissions to run (which are all because you're the owner).").complete();
		setTc.sendMessage("Also I ask you to set the bot role ('Jikai') color to #12e5a8 or R18 G229 B168. Thank you!").complete();
		//ForkJoinPool.commonPool().execute(() -> {
		alrh.sendList();
		sched.sendScheduleToGuild();
		//});
	}
}
