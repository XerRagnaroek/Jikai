
package com.github.xerragnaroek.jikai.commands.user;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jikai.jikai.JikaiIO;
import com.github.xerragnaroek.jikai.user.JikaiUser;
import com.github.xerragnaroek.jikai.util.BotUtils;

/**
 * @author XerRagnaroek
 *
 */
public class StopCommand implements JUCommand {
	private final Logger log = LoggerFactory.getLogger(StopCommand.class);

	@Override
	public String getName() {
		return "stop";
	}

	@Override
	public String getDescription() {
		return "Stops the bot";
	}

	@Override
	public void executeCommand(JikaiUser ju, String[] arguments) {
		log.info("Shutting the bot down...\n Saving data...");
		JikaiIO.save(true);
		BotUtils.sendToAllInfoChannels("The dev has shut the bot down. Downtime shouldn't be long. ||(Hopefully)||").forEach(CompletableFuture::join);
		log.info("Goodbye :)");
		System.exit(1);
	}

	@Override
	public boolean isDevOnly() {
		return true;
	}
}
