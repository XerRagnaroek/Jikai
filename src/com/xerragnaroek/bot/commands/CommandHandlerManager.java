package com.xerragnaroek.bot.commands;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xerragnaroek.bot.commands.set.SetCommand;
import com.xerragnaroek.bot.core.Core;
import com.xerragnaroek.bot.data.GuildDataManager;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class CommandHandlerManager extends ListenerAdapter {

	private static final Map<String, CommandHandlerImpl> impls = Collections.synchronizedMap(new TreeMap<>());
	private static boolean initialized = false;
	private static final Logger log = LoggerFactory.getLogger(CommandHandlerManager.class);
	private static final Set<Command> commands = Collections.synchronizedSet(new TreeSet<>());
	private static boolean comsEnabled = false;
	public static Permission[] MOD_PERMS =
			new Permission[] {	Permission.MANAGE_CHANNEL, Permission.MANAGE_ROLES, Permission.MESSAGE_MANAGE,
								Permission.KICK_MEMBERS, Permission.BAN_MEMBERS };

	public static void init() {
		if (!initialized) {
			initCommands();
			initCommandHandlerImpls();
			initialized = true;
		} else {
			throw new IllegalStateException("Already initialized!");
		}
	}

	private static void initCommandHandlerImpls() {
		GuildDataManager.getGuildIds().forEach(CommandHandlerManager::getCommandHandlerForGuild);
	}

	public static CommandHandlerImpl getCommandHandlerForGuild(String g) {
		return impls.compute(g, (gId, chi) -> (chi = (chi == null) ? new CommandHandlerImpl(gId) : chi));
	}

	public static CommandHandlerManager registerNew(Guild g) {

	}

	public static CommandHandlerImpl getCommandHandlerForGuild(Guild g) {
		return getCommandHandlerForGuild(g.getId());
	}

	static Set<Command> getCommands() {
		return commands;
	}

	/**
	 * Load the commands
	 */
	private static void initCommands() {
		Command[] coms =
				new Command[] {	new WhenAnimeCommand(), new PingCommand(), new SetCommand(), new ScheduleCommand(),
								new DebugCommand(), new AnimeListCommand(), new HelpCommand(), new WipeRolesCommand(),
								new EnableCommandsCommand(), new DisableCommandsCommand() };
		commands.addAll(Arrays.asList(coms));
		log.info("Loaded {} commands", commands.size());
	}

	public static boolean hasManagerForGuild(Guild g) {
		return hasManagerForGuild(g.getId());
	}

	public static boolean hasManagerForGuild(String g) {
		return impls.containsKey(g);
	}

	public static void setCommandsEnabledDefault(boolean enabled) {
		comsEnabled = enabled;
	}

	public static boolean areCommandsEnabledByDefault() {
		return comsEnabled;
	}

	public static Command findCommand(Set<Command> data, String content) {
		for (Command c : data) {
			if (c.getName().equals(content)) {
				return c;
			} else if (c.hasAlternativeName()) {
				if (c.getAlternativeName().equals(content)) {
					return c;
				}
			}
		}
		return null;
	}

	public static boolean checkPermissions(Command c, Member m) {
		boolean tmp = false;
		Permission[] perms = c.getRequiredPermissions();
		if (perms.length == 0) {
			tmp = true;
		} else {
			for (Permission p : perms) {
				if (m.hasPermission(p)) {
					tmp = true;
					break;
				}
			}
		}
		if (c.getIdentifier().equals("dec")) {
			tmp = m.getId().equals(Core.getDevId());
		}
		log.debug("Member has {}sufficient permission for command {}", (tmp ? "" : "in"), c.getName());
		return tmp;
	}
}
