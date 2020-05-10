package com.github.xerragnaroek.jikai.commands.guild;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.jikai.Jikai;
import com.github.xerragnaroek.jikai.jikai.JikaiData;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class DisableCommandsCommand implements GuildCommand {
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
	public void executeCommand(MessageReceivedEvent event, String[] arguments) {
		Guild g = event.getGuild();
		log.debug("Executing DisableCommandsCommand on guild {}#{}", g.getName(), g.getId());
		Jikai j = Core.JM.get(g);
		JikaiData jd = j.getJikaiData();
		if (jd.areCommandsEnabled()) {
			jd.setCommandsEnabled(false);
			try {
				j.getInfoChannel().sendMessage("Commands have been disabled.\nOnly members with the `MANAGE_SERVER` permission may call `" + jd.getTrigger() + "enable_commands` to reenable them.").queue();
			} catch (Exception e) {}
		}
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
