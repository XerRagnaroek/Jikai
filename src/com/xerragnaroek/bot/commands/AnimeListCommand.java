package com.xerragnaroek.bot.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xerragnaroek.bot.anime.alrh.ALRHManager;
import com.xerragnaroek.bot.anime.alrh.ALRHandler;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class AnimeListCommand implements Command {

	private final Logger log = LoggerFactory.getLogger(AnimeListCommand.class);

	@Override
	public String getName() {
		return "list";
	}

	@Override
	public void executeCommand(CommandHandlerImpl chi, MessageReceivedEvent event, String[] arguments) {
		log.debug("Executing ListCommand");
		ALRHandler h = ALRHManager.getAnimeListReactionHandlerForGuild(event.getGuild());
		if (!h.isSendingList()) {
			h.sendList();
		} else {
			log.debug("List is already being sent");
		}
	}

	@Override
	public String getIdentifier() {
		return "alc";
	}

	@Override
	public Permission[] getRequiredPermissions() {
		return CommandHandlerManager.MOD_PERMS;
	}

	@Override
	public String getDescription() {
		return "Sends the reactable list of seasonal animes to the anime_list_channel or general.";
	}

}
