package me.xer.bot.commands.set;

import me.xer.bot.commands.Command;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class SetTimeZoneCommand implements Command {

	@Override
	public String getCommandName() {
		return "timezone";
	}

	@Override
	public String getUsage() {
		return "timezone <zoneid>";
	}

	@Override
	public void executeCommand(MessageReceivedEvent event, String arguments) {

	}

}
