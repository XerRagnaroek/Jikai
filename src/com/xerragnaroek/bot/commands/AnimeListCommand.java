package com.xerragnaroek.bot.commands;

import com.xerragnaroek.bot.anime.ALRHManager;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class AnimeListCommand implements Command {

	@Override
	public String getCommandName() {
		return "list";
	}

	@Override
	public String getUsage() {
		return "list";
	}

	@Override
	public void executeCommand(CommandHandlerImpl chi, MessageReceivedEvent event, String arguments) {
		ALRHManager.getAnimeListReactionHandlerForGuild(event.getGuild()).sendList();
	}

}
