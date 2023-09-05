package com.github.xerragnaroek.jikai.commands;

import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocaleManager;
import net.dv8tion.jda.api.Permission;

import java.util.List;
import java.util.Objects;

public interface Command extends Comparable<Command> {
    /**
     * The command's "name", whatever triggers it.
     */
    String getName();

    String getLocaleKey();

    /**
     * How to use the command.
     */
    default String getUsage(JikaiLocale loc) {
        return Objects.requireNonNullElse(loc.getString(getLocaleKey() + "_use"), "").trim();
    }

    default boolean hasUsage() {
        return !getUsage(JikaiLocaleManager.getEN()).isEmpty();
    }

    default boolean hasAlternativeNames() {
        return getAlternativeNames() != null;
    }

    default List<String> getAlternativeNames() {
        return null;
    }

    default String getDescription(JikaiLocale loc) {
        return loc.getString(getLocaleKey() + "_desc");
    }

    default Permission[] getRequiredPermissions() {
        return Permission.EMPTY_PERMISSIONS;
    }

    default boolean isAlwaysEnabled() {
        return false;
    }

    @Override
    default int compareTo(Command o) {
        return getName().compareTo(o.getName());
    }

    default boolean isDevOnly() {
        return false;
    }

    default boolean isEnabled() {
        return true;
    }
}
