package com.xerragnaroek.jikai.commands;

import net.dv8tion.jda.api.Permission;

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

	public default boolean hasAlternativeName() {
		return false;
	}

	public default String getAlternativeName() {
		return null;
	}

	public String getDescription();

	public default Permission[] getRequiredPermissions() {
		return Permission.EMPTY_PERMISSIONS;
	}

	public default boolean isAlwaysEnabled() {
		return false;
	}

	@Override
	default int compareTo(Command o) {
		return getName().compareTo(o.getName());
	}

	public default boolean isDevOnly() {
		return false;
	}
}
