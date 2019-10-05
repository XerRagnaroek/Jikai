package com.xerragnaroek.bot.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xerragnaroek.bot.core.Core;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/**
 * The "schedule" command
 * 
 * @author XerRagnaroek
 *
 */
public class ScheduleCommand implements Command {
	private final Logger log = LoggerFactory.getLogger(ScheduleCommand.class);

	@Override
	public String getName() {
		return "schedule";
	}

	@Override
	public void executeCommand(CommandHandler chi, MessageReceivedEvent event, String[] arguments) {
		Core.SM.get(event.getGuild()).sendScheduleToGuild();
	}

	@Override
	public String getIdentifier() {
		return "scc";
	}

	@Override
	public Permission[] getRequiredPermissions() {
		return CommandHandlerManager.MOD_PERMS;
	}

	@Override
	public String getDescription() {
		return "Sends a schedule of when the animes air in the week, as per the previously set timezone.";
	}
}
