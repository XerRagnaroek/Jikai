package com.github.xerragnaroek.jikai.commands;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jikai.commands.guild.GuildCommand;
import com.github.xerragnaroek.jikai.commands.user.JUCommand;
import com.github.xerragnaroek.jikai.user.JikaiUser;
import com.github.xerragnaroek.jikai.user.JikaiUserManager;
import com.github.xerragnaroek.jikai.util.BotUtils;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/**
 * 
 */
public class BugCommand implements GuildCommand, JUCommand {

	private static AtomicInteger bugs = new AtomicInteger(0);
	private final Logger log = LoggerFactory.getLogger(BugCommand.class);

	@Override
	public String getName() {
		return "bug";
	}

	@Override
	public String getDescription() {
		return "Report a bug. Please try to give a precise explanation as to what you expected to happen (if anything) versus what actually happened.";
	}

	@Override
	public String getUsage() {
		return "bug <message>";
	}

	@Override
	public void executeCommand(MessageReceivedEvent event, String[] arguments) {
		printBug(String.join(" ", arguments), JikaiUserManager.getInstance().getUser(event.getMember().getIdLong()));
	}

	@Override
	public void executeCommand(JikaiUser ju, String[] arguments) {
		printBug(String.join(" ", arguments), ju);
	}

	private void printBug(String msg, JikaiUser ju) {
		String message = ju.getUser().getAsTag() + " reported a bug:\nBUG #" + bugs.incrementAndGet() + ": " + msg;
		log.error(message);
		ju.sendPM("Thank you for reporting a bug! Your bug number is " + bugs.get() + ".\nPlease don't abuse this feature, otherwise you will be banned from the server.");
		BotUtils.sendToDev(message);
	}
}
