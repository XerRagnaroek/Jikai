package com.xerragnaroek.jikai.commands;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.LoggerFactory;

import com.xerragnaroek.jikai.commands.set.SetCommand;
import com.xerragnaroek.jikai.core.Core;
import com.xerragnaroek.jikai.util.Manager;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;

public class CommandHandlerManager extends Manager<CommandHandler> {

	public CommandHandlerManager() {
		super(CommandHandler.class);
	}

	private final Set<Command> commands = Collections.synchronizedSet(new TreeSet<>());
	private boolean comsEnabled = false;
	public static Permission[] MOD_PERMS = new Permission[] { Permission.MANAGE_CHANNEL, Permission.MANAGE_ROLES, Permission.MESSAGE_MANAGE, Permission.KICK_MEMBERS, Permission.BAN_MEMBERS };

	public void init() {
		if (!isInitialized()) {
			initCommands();
			initCommandHandlerImpls();
			init.set(true);
		} else {
			throw new IllegalStateException("Already initialized!");
		}
	}

	private void initCommandHandlerImpls() {
		Core.JM.getGuildIds().forEach(this::registerNew);
	}

	Set<Command> getCommands() {
		return commands;
	}

	/**
	 * Load the commands
	 */
	private void initCommands() {
		Command[] coms = new Command[] { new StopCommand(), new StatusCommand(), new WhenAnimeCommand(), new PingCommand(), new SetCommand(), new ScheduleCommand(), new DebugCommand(), new AnimeListCommand(), new HelpCommand(), new WipeRolesCommand(), new EnableCommandsCommand(), new DisableCommandsCommand(), new RequestAssistanceCommand() };
		commands.addAll(Arrays.asList(coms));
		log.info("Loaded {} commands", commands.size());
	}

	public void setCommandsEnabledDefault(boolean enabled) {
		comsEnabled = enabled;
	}

	public boolean areCommandsEnabledByDefault() {
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
		if (c.isDevOnly()) {
			tmp = m.getId().equals(Core.DEV_ID);
		}
		LoggerFactory.getLogger(CommandHandlerManager.class).debug("Member has {}sufficient permission for command {}", (tmp ? "" : "in"), c.getName());
		return tmp;
	}

	@Override
	protected CommandHandler makeNew(String gId) {
		return new CommandHandler(gId);
	}
}
