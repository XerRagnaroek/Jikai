
package com.github.xerragnaroek.jikai.commands;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jikai.commands.guild.GuildCommand;
import com.github.xerragnaroek.jikai.commands.user.JUCommand;
import com.github.xerragnaroek.jikai.jikai.JikaiIO;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.user.JikaiUser;
import com.github.xerragnaroek.jikai.util.BotUtils;

import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

/**
 */
public class StopCommand implements JUCommand, GuildCommand {
	private final Logger log = LoggerFactory.getLogger(StopCommand.class);

	@Override
	public String getName() {
		return "stop";
	}

	@Override
	public String getDescription(JikaiLocale loc) {
		return "Stops the bot.";
	}

	@Override
	public void executeCommand(JikaiUser ju, String[] arguments) {
		stop();
	}

	@Override
	public boolean isDevOnly() {
		return true;
	}

	@Override
	public void executeCommand(GuildMessageReceivedEvent event, String[] arguments) {
		stop();
	}

	private void stop() {
		log.info("Shutting the bot down...");
		JikaiIO.save(true);
		BotUtils.sendToAllInfoChannelsLocalised("com_dev_stop_msg", false).forEach(CompletableFuture::join);
		log.info("Goodbye :)");
		System.exit(1);
	}

	@Override
	public String getLocaleKey() {
		return "";
	}
}
