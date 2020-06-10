
package com.github.xerragnaroek.jikai.core;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jikai.anime.schedule.ScheduleManager;
import com.github.xerragnaroek.jikai.jikai.Jikai;
import com.github.xerragnaroek.jikai.jikai.JikaiData;

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
		return g.createTextChannel("jikai_setup").addPermissionOverride(g.getPublicRole(), Arrays.asList(Permission.EMPTY_PERMISSIONS), Arrays.asList(Permission.VIEW_CHANNEL)).addPermissionOverride(g.retrieveOwner().complete(), Arrays.asList(Permission.VIEW_CHANNEL, Permission.MESSAGE_MANAGE), Arrays.asList(Permission.EMPTY_PERMISSIONS)).addPermissionOverride(g.getSelfMember(), Arrays.asList(Permission.VIEW_CHANNEL, Permission.MESSAGE_MANAGE), Arrays.asList(Permission.EMPTY_PERMISSIONS)).complete();
	}

	private void runSetup() {
		log.info("Running setup");
		setTc = makeSetupChannel();
		log.debug("Made setup channel");
		setTc.sendMessage(g.retrieveOwner().complete().getAsMention()).complete();
		MessageBuilder mb = new MessageBuilder("\nThis is where the bot setup will take place.\n");
		mb.append("The bot will create 4 textchannels:\n**jikai_list** - for the anime list\n**jikai_schedule** - for the anime release schedule\n**jikai_anime** - for upcoming release notifications\n**jikai_info** - for status updates and information concerning the bot\nFurthermore also set these settings:\n");
		mb.append("**trigger** = '!' - what needs to be written before a command for the bot to recognize it. E.g. !help\n");
		mb.append("**timezone** = 'Europe/Berlin' - the timezone that will be used to adjust release updates.\n");
		mb.append("You can change the latter settings via the set commands (see !help for more info)!\n");
		mb.append("Feel free to move or rename the channels, the bot is using their unique ID, so the name doesn't matter.");
		mb.append("Once the setup is done, the bot will send the anime list and the schedule.\n");
		mb.append("Commence setup? *yes(y)*");
		setTc.sendMessage(mb.build()).complete();
		listen = true;
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
			setup();
		}
	}

	private void setup() {
		listen = false;
		log.info("Owner agreed to setup");
		Jikai j = Core.JM.registerNew(g);
		JikaiData jd = j.getJikaiData();
		Category cat = g.createCategory("jikai").addPermissionOverride(g.getPublicRole(), Arrays.asList(Permission.VIEW_CHANNEL, Permission.MESSAGE_ADD_REACTION), Arrays.asList(Permission.MESSAGE_WRITE)).addPermissionOverride(g.getSelfMember(), Permission.ALL_CHANNEL_PERMISSIONS, 0l).complete();
		TextChannel tc = cat.createTextChannel("jikai_list").complete();
		log.debug("Made jikai_list channel");
		jd.setListChannelId(tc.getIdLong());
		tc = cat.createTextChannel("jikai_schedule").complete();
		log.debug("Made jikai_schedule channel");
		jd.setScheduleChannelId(tc.getIdLong());
		tc = cat.createTextChannel("jikai_anime").complete();
		log.debug("Made jikai_anime channel");
		jd.setAnimeChannelId(tc.getIdLong());
		tc = cat.createTextChannel("jikai_info").complete();
		log.debug("Made jikai_info channel");
		jd.setInfoChannelId(tc.getIdLong());
		jd.setSetupCompleted(true);
		jd.save(true);
		j.setALRH(Core.JM.getALHRM().registerNew(g));
		log.info("Setup completed");
		setTc.sendMessage("The setup is complete. Commands are by default " + (jd.areCommandsEnabled() ? "enabled" : "disabled") + ".\nYou can change that by calling !enable/disable_commands").complete();
		setTc.sendMessage("Send `!help` for a list of all commands you have permissions to run (which are all because you're the owner).").complete();
		setTc.sendMessage("Also I ask you to set the bot role ('Jikai') color to #12e5a8 or R18 G229 B168. Thank you!").complete();
		Core.EXEC.execute(() -> j.getALRHandler().sendList());
		Core.EXEC.execute(() -> ScheduleManager.sendScheduleToJikai(j));
	}
}
