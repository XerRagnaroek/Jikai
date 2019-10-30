package com.xerragnaroek.jikai.commands;

import com.xerragnaroek.jikai.anime.db.AnimeDB;
import com.xerragnaroek.jikai.core.Core;
import com.xerragnaroek.jikai.timer.RTKManager;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class WhenAnimeCommand implements Command {

	@Override
	public String getName() {
		return "when_anime";
	}

	@Override
	public String getUsage() {
		return "when_anime <true> (override thresholds) <true> (all animes)";
	}

	@Override
	public void executeCommand(CommandHandler chi, MessageReceivedEvent event, String[] arguments) {
		Guild g = event.getGuild();
		AnimeDB.waitUntilLoaded();
		RTKManager rtkm = Core.JM.getRTKM();
		if (arguments.length > 1) {
			rtkm.get(g).updateAnimes(Boolean.parseBoolean(arguments[0]), Boolean.parseBoolean(arguments[1]));
		} else if (arguments.length == 1) {
			rtkm.get(g).updateAnimes(Boolean.parseBoolean(arguments[0]), false);
		} else {
			rtkm.get(g).updateAnimes(false, false);
		}
	}

	@Override
	public String getIdentifier() {
		return "wac";
	}

	@Override
	public Permission[] getRequiredPermissions() {
		return CommandHandlerManager.MOD_PERMS;
	}

	@Override
	public String getDescription() {
		return "Forces sending of the release updates. Can be used to ignore time thresholds and send it for all animes, instead of just the reacted ones (**NOT RECOMMENDED** ||but fun||)";
	}
}
