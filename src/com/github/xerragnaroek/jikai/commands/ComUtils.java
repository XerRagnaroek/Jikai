package com.github.xerragnaroek.jikai.commands;

import java.util.Set;
import java.util.function.Consumer;

import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jikai.commands.guild.CommandHandler;
import com.github.xerragnaroek.jikai.commands.guild.GuildCommand;
import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.user.JikaiUser;
import com.github.xerragnaroek.jikai.user.JikaiUserManager;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;

public class ComUtils {

	public static boolean checkPermissions(Command c, Member m) {
		boolean tmp = false;
		Permission[] perms = c.getRequiredPermissions();
		if (perms.length == 0) {
			tmp = true;
		} else {
			for (Permission p : perms) {
				if (m.hasPermission(p)) {
					return true;
				}
			}
		}
		// this is mostly to stop non jus from using commands but allowing them to still use commands like
		// help and register
		if (c instanceof GuildCommand) {
			GuildCommand gc = (GuildCommand) c;
			// user has the necessary perms
			if (tmp) {
				if (gc.isJikaiUserOnly()) {
					// only known jus can use this command
					tmp = JikaiUserManager.getInstance().isKnownJikaiUser(m.getIdLong());
				}
			}
		}
		if (c.isDevOnly()) {
			tmp = m.getIdLong() == Core.DEV_ID;
		}
		LoggerFactory.getLogger(CommandHandler.class).debug("Member has {}sufficient permission for command {}", (tmp ? "" : "in"), c.getName());
		return tmp;
	}

	public static <T extends Command> T findCommand(Set<T> data, String content) {
		for (T c : data) {
			if (c.getName().equals(content)) {
				return c;
			} else if (c.hasAlternativeNames()) {
				if (c.getAlternativeNames().stream().anyMatch(str -> str.equals(content))) {
					return c;
				}
			}
		}
		return null;
	}

	public static void trueFalseCommand(String input, JikaiUser ju, Consumer<Boolean> con) {
		String str = input;
		switch (str.toLowerCase()) {
			case "true":
			case "false":
				con.accept(Boolean.parseBoolean(str));
				break;
			default:
				ju.sendPMFormat("Invalid input: '%s' !", str);
				return;
		}
	}
}
