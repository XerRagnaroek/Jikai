package com.github.xerragnaroek.jikai.commands;

import java.util.List;

import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocaleManager;

import net.dv8tion.jda.api.Permission;

public interface Command extends Comparable<Command> {
	/**
	 * The command's "name", whatever triggers it.
	 */
	public String getName();

	/**
	 * How to use the command.
	 */
	public default String getUsage(JikaiLocale loc) {
		String use = null;
		if (hasAlternativeNames()) {
			use = getName();
		}
		return use;
	}

	public default boolean hasUsage() {
		return getUsage(JikaiLocaleManager.getEN()) != null;
	}

	public default boolean hasAlternativeNames() {
		return getAlternativeNames() != null;
	}

	public default List<String> getAlternativeNames() {
		return null;
	}

	public String getDescription(JikaiLocale loc);

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

	public default boolean isEnabled() {
		return true;
	}
}
