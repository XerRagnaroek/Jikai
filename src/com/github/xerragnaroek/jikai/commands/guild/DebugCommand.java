package com.github.xerragnaroek.jikai.commands.guild;

import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class DebugCommand implements GuildCommand {
	private final Logger log = LoggerFactory.getLogger(DebugCommand.class);

	@Override
	public String getName() {
		return "debug";
	}

	@Override
	public void executeCommand(MessageReceivedEvent event, String[] arguments) {
		log.debug("Command was called by the author");
		log.info("Executing debug command");
		MessageBuilder bob = new MessageBuilder();
		bob.append("Hello I am debug command");
		try {
			log.debug("Debug message id = {}", bob.sendTo(event.getChannel()).submit().get().getId());
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String getDescription() {
		return "Prints internal debug information. Only usable by the owner of this bot.";
	}

	@Override
	public boolean isAlwaysEnabled() {
		return true;
	}

	@Override
	public boolean isDevOnly() {
		return true;
	}

}
