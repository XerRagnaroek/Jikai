package com.xerragnaroek.bot.core;

import static com.xerragnaroek.bot.core.Core.ALRHM;
import static com.xerragnaroek.bot.core.Core.CHM;
import static com.xerragnaroek.bot.core.Core.GDM;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveAllEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class EventListener extends ListenerAdapter {
	private final Logger log = LoggerFactory.getLogger(EventListener.class);

	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		//ignore bots
		if (!event.getAuthor().isBot() && event.isFromGuild()) {
			if (GDM.hasCompletedSetup(event.getGuild())) {
				runAsync(() -> CHM.get(event.getGuild()).handleMessage(event));
			}
		}
	}

	@Override
	public void onMessageReactionAdd(MessageReactionAddEvent event) {
		if (!event.getUser().isBot()) {
			if (GDM.hasCompletedSetup(event.getGuild())) {
				runAsync(() -> ALRHM.get(event.getGuild()).handleReactionAdded(event));
			}
		}
	}

	@Override
	public void onMessageReactionRemove(MessageReactionRemoveEvent event) {
		if (GDM.hasCompletedSetup(event.getGuild())) {
			runAsync(() -> ALRHM.get(event.getGuild()).handleReactionRemoved(event));
		}
	}

	@Override
	public void onMessageReactionRemoveAll(MessageReactionRemoveAllEvent event) {
		if (GDM.hasCompletedSetup(event.getGuild())) {
			runAsync(() -> ALRHM.get(event.getGuild()).handleReactionRemovedAll(event));
		}
	}

	@Override
	public void onReady(ReadyEvent event) {
		/*log.info("Initializing ALRHs");
		//runAsync(() -> ALRHManager.init());
		ALRHManager.init();
		GDM.startSaveThread(10, TimeUnit.SECONDS);*/
	}

	@Override
	public void onGuildJoin(GuildJoinEvent event) {
		String gId = event.getGuild().getId();
		if (!GDM.isKnownGuild(gId)) {
			onNewGuild(event.getGuild());
		}
	}

	private void onNewGuild(Guild g) {
		log.info("New Guild {}#{}, running setup", g.getName(), g.getId());
		runAsync(() -> SetupHelper.runSetup(g));
	}

	private void runAsync(Runnable r) {
		CompletableFuture.runAsync(r).whenComplete((v, e) -> {
			if (e != null) {
				Core.logThrowable(e);
			}
		});
	}
}
