package com.xerragnaroek.bot.commands;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/**
 * A recognized command of the bot.
 * 
 * @author XerRagnarök
 *
 */
public interface Command {
	/**
	 * The command's "name", whatever triggers it.
	 * 
	 */
	abstract String getCommandName();

	/**
	 * How to use the command.
	 */
	abstract String getUsage();

	/**
	 * Run the command.
	 * 
	 * @param event
	 *            - Required to get whoever issued the command and to reply
	 * @param arguments
	 *            - what was written after the command
	 */
	abstract void executeCommand(CommandHandlerImpl chi, MessageReceivedEvent event, String arguments);
}
