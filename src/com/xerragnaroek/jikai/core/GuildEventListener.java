package com.xerragnaroek.jikai.core;

import static com.xerragnaroek.jikai.core.Core.JM;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xerragnaroek.jikai.data.Jikai;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveAllEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class GuildEventListener extends ListenerAdapter {
	private final Logger log = LoggerFactory.getLogger(GuildEventListener.class);
	private final Map<String, ExecutorService> execMap = new TreeMap<>();

	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		//ignore bots
		if (!event.getAuthor().isBot() && event.isFromGuild()) {
			Jikai j = JM.get(event.getGuild());
			if (j.hasCompletedSetup()) {
				runAsync(event.getGuild(), () -> j.getCommandHandler().handleMessage(event));
			}
		}
	}

	@Override
	public void onMessageReactionAdd(MessageReactionAddEvent event) {
		if (!event.getUser().isBot()) {
			Jikai j = JM.get(event.getGuild());
			if (j.hasCompletedSetup()) {
				runAsync(event.getGuild(), () -> j.getALRHandler().handleReactionAdded(event));
			}
		}
	}

	@Override
	public void onMessageReactionRemove(MessageReactionRemoveEvent event) {
		Jikai j = JM.get(event.getGuild());
		if (j.hasCompletedSetup()) {
			runAsync(event.getGuild(), () -> j.getALRHandler().handleReactionRemoved(event));
		}
	}

	@Override
	public void onMessageReactionRemoveAll(MessageReactionRemoveAllEvent event) {
		Jikai j = JM.get(event.getGuild());
		if (j.hasCompletedSetup()) {
			runAsync(event.getGuild(), () -> j.getALRHandler().handleReactionRemovedAll(event));
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
		if (!JM.isKnownGuild(gId)) {
			onNewGuild(event.getGuild());
		}
	}

	private void onNewGuild(Guild g) {
		log.info("New Guild {}#{}, running setup", g.getName(), g.getId());
		runAsync(g, () -> SetupHelper.runSetup(g));
	}

	private void runAsync(Guild g, Runnable r) {
		CompletableFuture.runAsync(r, getExec(g)).whenComplete((v, e) -> {
			if (e != null) {
				Core.logThrowable(e);
			}
		});
	}

	private ExecutorService getExec(Guild g) {
		String id = g.getId();
		if (!execMap.containsKey(id)) {
			execMap.put(id, Executors.newSingleThreadExecutor());
		}
		return execMap.get(id);
	}
}
