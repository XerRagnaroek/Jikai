package com.xerragnaroek.bot.commands;

import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xerragnaroek.bot.responses.PremadeResponses;

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class DebugCommand implements Command {
	private final Logger log = LoggerFactory.getLogger(DebugCommand.class);

	@Override
	public String getCommandName() {
		return "debug";
	}

	@Override
	public String getUsage() {
		return "debug";
	}

	@Override
	public void executeCommand(CommandHandlerImpl chi, MessageReceivedEvent event, String[] arguments) {
		log.debug("Checking if user is the author of the bot...");
		User u = event.getAuthor();
		//my ID
		if (u.getId().equals("129942311663697921")) {
			log.debug("Command was called by the author");
			log.info("Executing debug command");
			MessageBuilder bob = new MessageBuilder();
			bob.append("Hello I am debug command");
			try {
				log.debug("Debug message id = {}", bob.sendTo(event.getChannel()).submit().get().getId());
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
		} else {
			log.debug("Command was called by someone else");
			event.getTextChannel().sendMessage(PremadeResponses.lackingPermissions(u)).queue();
		}
	}

}
