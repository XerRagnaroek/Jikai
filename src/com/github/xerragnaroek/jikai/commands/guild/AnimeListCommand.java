package com.github.xerragnaroek.jikai.commands.guild;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jikai.anime.alrh.ALRHandler;
import com.github.xerragnaroek.jikai.anime.db.AnimeDB;
import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.jikai.Jikai;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class AnimeListCommand implements GuildCommand {

	private final Logger log = LoggerFactory.getLogger(AnimeListCommand.class);

	@Override
	public String getName() {
		return "list";
	}

	@Override
	public void executeCommand(GuildMessageReceivedEvent event, String[] arguments) {
		log.debug("Executing ListCommand");
		Jikai j = Core.JM.get(event.getGuild());
		JikaiLocale loc = j.getLocale();
		AnimeDB.waitUntilLoaded();
		ALRHandler h = j.getALRHandler();
		MessageChannel mc = event.getChannel();
		if (!h.isSendingList()) {
			h.sendList();
		} else {
			log.debug("List is already being sent");
			mc.sendMessage(loc.getString("com_g_list_wait")).queue();
		}
	}

	@Override
	public Permission[] getRequiredPermissions() {
		return CommandHandler.MOD_PERMS;
	}

	@Override
	public String getLocaleKey() {
		return "com_g_list";
	}

}
