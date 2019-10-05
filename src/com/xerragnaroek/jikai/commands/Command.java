package com.xerragnaroek.jikai.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/**
 * A recognized command of the bot.
 * 
 * @author XerRagnarök
 *
 */
public interface Command extends Comparable<Command> {
	/**
	 * The command's "name", whatever triggers it.
	 * 
	 */
	public String getName();

	/**
	 * How to use the command.
	 */
	public default String getUsage() {
		String use = null;
		if (hasAlternativeName()) {
			use = getName() + "|" + getAlternativeName();
		}
		return use;
	}

	public default boolean hasUsage() {
		return getUsage() != null;
	}

	/**
	 * Run the command.
	 * 
	 * @param event
	 *            - Required to get whoever issued the command and to reply
	 * @param arguments
	 *            - what was written after the command
	 */
	public void executeCommand(CommandHandler chi, MessageReceivedEvent event, String[] arguments);

	public default boolean hasAlternativeName() {
		return false;
	}

	public default String getAlternativeName() {
		return null;
	}

	public default boolean isCommand(String identifier) {
		return getIdentifier().equals(identifier);
	}

	public String getIdentifier();

	public String getDescription();

	public default Permission[] getRequiredPermissions() {
		return Permission.EMPTY_PERMISSIONS;
	}

	@Override
	default int compareTo(Command o) {
		return getName().compareTo(o.getName());
	}
}
