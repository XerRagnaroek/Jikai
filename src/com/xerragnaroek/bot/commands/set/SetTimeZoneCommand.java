package com.xerragnaroek.bot.commands.set;

import com.xerragnaroek.bot.commands.Command;
import com.xerragnaroek.bot.commands.CommandHandlerImpl;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class SetTimeZoneCommand implements Command {
	SetTimeZoneCommand() {}

	@Override
	public String getCommandName() {
		return "timezone";
	}

	@Override
	public String getUsage() {
		return "timezone <zoneid> (tzdb ids)";
	}

	@Override
	public void executeCommand(CommandHandlerImpl chi, MessageReceivedEvent event, String arguments) {

	}

}
