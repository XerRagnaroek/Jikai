package com.xerragnaroek.bot.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class HelpCommand implements Command {
	private final Logger log = LoggerFactory.getLogger(HelpCommand.class);
	private String helpString = "";

	@Override
	public String getCommandName() {
		return "help";
	}

	@Override
	public String getUsage() {
		return "help";
	}

	@Override
	public void executeCommand(CommandHandlerImpl chi, MessageReceivedEvent event, String[] arguments) {
		log.info("Executing help command");
		synchronized (helpString) {
			if (helpString.isEmpty()) {
				StringBuilder bob = new StringBuilder();
				CommandHandlerManager.getCommands().values().forEach(com -> bob.append("%1$s" + com.getUsage() + "\n"));
				helpString = bob.toString();
			}
		}
		MessageBuilder mb = new MessageBuilder();
		mb.appendCodeBlock("My commands are:\n" + String.format(helpString, chi.getTrigger()), "css");
		mb.sendTo(event.getChannel()).queue();
	}

}
