
package com.github.xerragnaroek.jikai.commands.guild;

import com.github.xerragnaroek.jikai.commands.Command;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/**
 * A recognized command of the bot.
 * 
 * @author XerRagnarök
 *
 */
public interface GuildCommand extends Command {
	/**
	 * Run the command.
	 * 
	 * @param event
	 *            - Required to get whoever issued the command and to reply
	 * @param arguments
	 *            - what was written after the command
	 */
	public void executeCommand(MessageReceivedEvent event, String[] arguments);
}
