
package com.github.xerragnaroek.jikai.core;

import static com.github.xerragnaroek.jikai.core.Core.JM;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jikai.commands.user.JUCommandHandler;
import com.github.xerragnaroek.jikai.jikai.Jikai;
import com.github.xerragnaroek.jikai.user.JikaiUser;

import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveAllEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class EventListener extends ListenerAdapter {
	private final Logger log = LoggerFactory.getLogger(EventListener.class);

	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		//ignore bots
		if (!event.getAuthor().isBot()) {
			if (event.isFromGuild()) {
				Guild g = event.getGuild();
				if (JM.hasManagerFor(g)) {
					Jikai j = JM.get(g);
					if (j.hasCompletedSetup()) {
						runAsync(() -> j.getCommandHandler().handleMessage(event));
					}
				}
			}
		}
	}

	@Override
	public void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {
		JikaiUser ju = Jikai.getUserManager().getUser(event.getAuthor().getIdLong());
		if (ju != null && ju.isSetupCompleted()) {
			log.debug("Received pm from known jikai user");
			runAsync(() -> JUCommandHandler.handleMessage(ju, event.getMessage().getContentRaw()));
		}
	}

	@Override
	public void onMessageReactionAdd(MessageReactionAddEvent event) {
		if (!event.getUser().isBot() && event.isFromType(ChannelType.TEXT)) {
			Jikai j = JM.get(event.getGuild());
			if (j.hasCompletedSetup()) {
				runAsync(() -> j.getALRHandler().handleReactionAdded(event));
			}
		}
	}

	@Override
	public void onMessageReactionRemove(MessageReactionRemoveEvent event) {
		if (event.isFromGuild()) {
			Jikai j = JM.get(event.getGuild());
			if (j.hasCompletedSetup()) {
				runAsync(() -> j.getALRHandler().handleReactionRemoved(event));
			}
		}
	}

	@Override
	public void onMessageReactionRemoveAll(MessageReactionRemoveAllEvent event) {
		if (event.isFromGuild()) {
			Jikai j = JM.get(event.getGuild());
			if (j.hasCompletedSetup()) {
				runAsync(() -> j.getALRHandler().handleReactionRemovedAll(event));
			}
		}
	}

	@Override
	public void onReady(ReadyEvent event) {
		/*
		 * log.info("Initializing ALRHs"); //runAsync(() -> ALRHManager.init()); ALRHManager.init();
		 * GDM.startSaveThread(10, TimeUnit.SECONDS);
		 */
	}

	@Override
	public void onGuildJoin(GuildJoinEvent event) {
		long gId = event.getGuild().getIdLong();
		if (!JM.isKnownGuild(gId)) {
			onNewGuild(event.getGuild());
		}
	}

	@Override
	public void onGuildLeave(GuildLeaveEvent event) {
		long id = event.getGuild().getIdLong();
		log.info("Guild {} left", event.getGuild().getName());
		JM.remove(id);

	}

	private void onNewGuild(Guild g) {
		log.info("New Guild {}#{}, running setup", g.getName(), g.getId());
		runAsync(() -> SetupHelper.runSetup(g));
	}

	private void runAsync(Runnable r) {
		CompletableFuture.runAsync(r, Core.EXEC).whenComplete((v, e) -> {
			if (e != null) {
				Core.logThrowable(e);
			}
		});
	}

}
