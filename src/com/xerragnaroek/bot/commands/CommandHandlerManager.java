package com.xerragnaroek.bot.commands;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xerragnaroek.bot.commands.set.SetCommand;
import com.xerragnaroek.bot.data.GuildDataManager;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class CommandHandlerManager extends ListenerAdapter {

	private static final Map<String, CommandHandlerImpl> impls = Collections.synchronizedMap(new TreeMap<>());
	private static boolean initialized = false;
	private static final Logger log = LoggerFactory.getLogger(CommandHandlerManager.class);
	private static final Map<String, Command> commands = Collections.synchronizedMap(new TreeMap<>());

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

	public static CommandHandlerImpl getCommandHandlerForGuild(Guild g) {
		return getCommandHandlerForGuild(g.getId());
	}

	static Map<String, Command> getCommands() {
		return commands;
	}

	/**
	 * Load the commands
	 */
	private static void initCommands() {
		Command[] coms = new Command[] { new PingCommand(), new SetCommand(), new ScheduleCommand(), new EmbedTestCommand(), new DebugCommand(), new AnimeListCommand(), new HelpCommand(), new WipeRolesCommand() };
		for (Command c : coms) {
			commands.put(c.getCommandName(), c);
			log.debug("Loaded command " + c.getCommandName());
		}
	}

	public static boolean hasManagerForGuild(Guild g) {
		return hasManagerForGuild(g.getId());
	}

	public static boolean hasManagerForGuild(String g) {
		return impls.containsKey(g);
	}

}
