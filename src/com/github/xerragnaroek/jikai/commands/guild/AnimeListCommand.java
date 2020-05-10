package com.github.xerragnaroek.jikai.commands.guild;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jikai.anime.alrh.ALRHandler;
import com.github.xerragnaroek.jikai.anime.db.AnimeDB;
import com.github.xerragnaroek.jikai.core.Core;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class AnimeListCommand implements GuildCommand {

	private final Logger log = LoggerFactory.getLogger(AnimeListCommand.class);

	@Override
	public String getName() {
		return "list";
	}

	@Override
	public void executeCommand(MessageReceivedEvent event, String[] arguments) {
		log.debug("Executing ListCommand");
		AnimeDB.waitUntilLoaded();
		ALRHandler h = Core.JM.get(event.getGuild()).getALRHandler();
		if (!h.isSendingList()) {
			h.sendList();
		} else {
			log.debug("List is already being sent");
		}
	}

	@Override
	public Permission[] getRequiredPermissions() {
		return CommandHandler.MOD_PERMS;
	}

	@Override
	public String getDescription() {
		return "Sends the reactable list of seasonal animes to the anime_list_channel or general.";
	}

}
