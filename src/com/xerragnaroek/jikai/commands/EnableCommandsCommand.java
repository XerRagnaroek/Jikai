package com.xerragnaroek.jikai.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xerragnaroek.jikai.core.Core;
import com.xerragnaroek.jikai.data.Jikai;
import com.xerragnaroek.jikai.data.JikaiData;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class EnableCommandsCommand implements Command {
	private final Logger log = LoggerFactory.getLogger(EnableCommandsCommand.class);

	@Override
	public String getName() {
		return "enable_commands";
	}

	@Override
	public String getAlternativeName() {
		return "enable_coms";
	}

	@Override
	public void executeCommand(CommandHandler chi, MessageReceivedEvent event, String[] arguments) {
		Guild g = event.getGuild();
		log.debug("Executing EnableCommandsCommand on guild {}#{}", g.getName(), g.getId());
		Jikai j = Core.JM.get(g);
		JikaiData jd = j.getJikaiData();
		if (jd.areCommandsEnabled()) {
			jd.setCommandsEnabled(false);
			try {
				j.getInfoChannel().sendMessage("Commands have been enabled. Call `" + jd.getTrigger() + "disable_commands` to disable them.").queue();
			} catch (Exception e) {}
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
		return "Enables this bot's commands.";
	}

}
