package com.xerragnaroek.jikai.commands;

import java.util.Set;
import java.util.function.Consumer;

import org.slf4j.LoggerFactory;

import com.xerragnaroek.jikai.commands.guild.CommandHandler;
import com.xerragnaroek.jikai.core.Core;
import com.xerragnaroek.jikai.user.JikaiUser;

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
					tmp = true;
					break;
				}
			}
		}
		if (c.isDevOnly()) {
			tmp = m.getId().equals(Core.DEV_ID);
		}
		LoggerFactory.getLogger(CommandHandler.class).debug("Member has {}sufficient permission for command {}", (tmp ? "" : "in"), c.getName());
		return tmp;
	}

	public static <T extends Command> T findCommand(Set<T> data, String content) {
		for (T c : data) {
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
