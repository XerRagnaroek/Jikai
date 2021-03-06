package com.github.xerragnaroek.jikai.commands;

import java.util.List;
import java.util.Objects;

import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocaleManager;

import net.dv8tion.jda.api.Permission;

public interface Command extends Comparable<Command> {
	/**
	 * The command's "name", whatever triggers it.
	 */
	public String getName();

	public String getLocaleKey();

	/**
	 * How to use the command.
	 */
	public default String getUsage(JikaiLocale loc) {
		return Objects.requireNonNullElse(loc.getString(getLocaleKey() + "_use"), "").trim();
	}

	public default boolean hasUsage() {
		return !getUsage(JikaiLocaleManager.getEN()).isEmpty();
	}

	public default boolean hasAlternativeNames() {
		return getAlternativeNames() != null;
	}

	public default List<String> getAlternativeNames() {
		return null;
	}

	public default String getDescription(JikaiLocale loc) {
		return loc.getString(getLocaleKey() + "_desc");
	}

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
