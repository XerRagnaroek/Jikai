package com.xerragnaroek.jikai.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xerragnaroek.jikai.anime.alrh.ALRHandler;
import com.xerragnaroek.jikai.anime.db.AnimeDB;
import com.xerragnaroek.jikai.core.Core;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class AnimeListCommand implements Command {

	private final Logger log = LoggerFactory.getLogger(AnimeListCommand.class);

	@Override
	public String getName() {
		return "list";
	}

	@Override
	public void executeCommand(CommandHandler chi, MessageReceivedEvent event, String[] arguments) {
		log.debug("Executing ListCommand");
		AnimeDB.waitUntilLoaded();
		ALRHandler h = Core.ALRHM.get(event.getGuild());
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
