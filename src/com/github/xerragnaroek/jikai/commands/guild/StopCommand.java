
package com.github.xerragnaroek.jikai.commands.guild;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jikai.jikai.JikaiIO;
import com.github.xerragnaroek.jikai.util.BotUtils;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class StopCommand implements GuildCommand {

	private final Logger log = LoggerFactory.getLogger(StopCommand.class);

	@Override
	public String getName() {
		return "stop";
	}

	@Override
	public void executeCommand(MessageReceivedEvent event, String[] arguments) {
		log.info("Shutting the bot down...\n Saving data...");
		JikaiIO.save(true);
		BotUtils.sendToAllInfoChannels("The dev has shut the bot down. Downtime shouldn't be long. ||(Hopefully)||").forEach(CompletableFuture::join);
		log.info("Goodbye :)");
		System.exit(1);
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
