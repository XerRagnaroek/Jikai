package com.xerragnaroek.bot.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xerragnaroek.bot.anime.ALRHManager;
import com.xerragnaroek.bot.commands.CommandHandlerManager;

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
			CommandHandlerManager.getCommandHandlerForGuild(event.getGuild()).handleMessage(event);
		}
	}

	@Override
	public void onMessageReactionAdd(MessageReactionAddEvent event) {
		if (!event.getUser().isBot()) {
			ALRHManager.getAnimeListReactionHandlerForGuild(event.getGuild()).handleReactionAdded(event);
		}
	}

	@Override
	public void onMessageReactionRemove(MessageReactionRemoveEvent event) {
		ALRHManager.getAnimeListReactionHandlerForGuild(event.getGuild()).handleReactionRemoved(event);
	}

	@Override
	public void onMessageReactionRemoveAll(MessageReactionRemoveAllEvent event) {
		ALRHManager.getAnimeListReactionHandlerForGuild(event.getGuild()).handleReactionRemovedAll(event);
	}
}
