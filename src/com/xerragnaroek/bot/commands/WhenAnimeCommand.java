package com.xerragnaroek.bot.commands;

import com.xerragnaroek.bot.anime.base.ReleaseTimeKeeper;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class WhenAnimeCommand implements Command {

	@Override
	public String getCommandName() {
		return "when_anime";
	}

	@Override
	public String getUsage() {
		return "when_anime <optional: true> (override threshold)";
	}

	@Override
	public void executeCommand(CommandHandlerImpl chi, MessageReceivedEvent event, String[] arguments) {
		ReleaseTimeKeeper.updateAllAnimesForGuild(event.getGuild(), Boolean.parseBoolean(arguments[0]));
	}

}
