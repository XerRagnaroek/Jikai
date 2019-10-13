package com.xerragnaroek.jikai.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xerragnaroek.jikai.core.Core;
import com.xerragnaroek.jikai.data.GuildData;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class DisableCommandsCommand implements Command {
	private final Logger log = LoggerFactory.getLogger(DisableCommandsCommand.class);

	@Override
	public String getName() {
		return "disable_commands";
	}

	@Override
	public String getAlternativeName() {
		return "disable_coms";
	}

	@Override
	public void executeCommand(CommandHandler chi, MessageReceivedEvent event, String[] arguments) {
		Guild g = event.getGuild();
		log.debug("Executing DisableCommandsCommand on guild {}#{}", g.getName(), g.getId());
		GuildData gd = Core.GDM.get(g);
		if (gd.areCommandsEnabled()) {
			gd.setCommandsEnabled(false);
			String id = gd.getInfoChannelId();
			TextChannel tc = null;
			if (id != null) {
				tc = g.getTextChannelById(id);
			}
			tc = (tc == null) ? event.getTextChannel() : tc;
			tc.sendMessage("Commands have been disabled.\nOnly members with the `MANAGE_SERVER` permission may call `" + gd.getTrigger() + "enable_commands` to reenable them.").queue();
		}
	}

	@Override
	public String getIdentifier() {
		return "ecc";
	}

	@Override
	public Permission[] getRequiredPermissions() {
		return new Permission[] { Permission.MANAGE_SERVER };
	}

	@Override
	public String getDescription() {
		return "Disables all of this bot's commands for everyone.";
	}

}