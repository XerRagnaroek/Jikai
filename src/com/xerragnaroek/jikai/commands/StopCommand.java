package com.xerragnaroek.jikai.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xerragnaroek.jikai.core.Core;
import com.xerragnaroek.jikai.util.BotUtils;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class StopCommand implements Command {

	private final Logger log = LoggerFactory.getLogger(StopCommand.class);

	@Override
	public String getName() {
		return "stop";
	}

	@Override
	public void executeCommand(CommandHandler chi, MessageReceivedEvent event, String[] arguments) {
		log.info("Shutting the bot down...\n Saving data...");
		Core.GDM.saveNow();
		BotUtils.sendToAllInfoChannels("The dev has shut the bot down. Downtime shouldn't be long. ||(Hopefully)||");
		log.info("Goodbye :)");
		System.exit(1);
	}

	@Override
	public String getIdentifier() {
		return "stc";
	}

	@Override
	public String getDescription() {
		return "Stops the bot.";
	}

	@Override
	public boolean isDevOnly() {
		return true;
	}
}
