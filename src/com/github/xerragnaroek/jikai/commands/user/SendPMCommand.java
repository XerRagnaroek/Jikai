package com.github.xerragnaroek.jikai.commands.user;

import java.util.Arrays;
import java.util.stream.Collectors;

import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.user.JikaiUser;
import com.github.xerragnaroek.jikai.user.JikaiUserManager;

/**
 * 
 */
public class SendPMCommand implements JUCommand {

	@Override
	public String getName() {
		return "send_pm";
	}

	@Override
	public String getDescription(JikaiLocale loc) {
		return "Send a pm to a user via this bot.";
	}

	@Override
	public void executeCommand(JikaiUser ju, String[] arguments) {
		try {
			JikaiUser target = JikaiUserManager.getInstance().getUser(Long.parseLong(arguments[0]));
			if (target == null) {
				ju.sendPM("Unkown JikaiUser!");
			} else {
				target.sendPM(Arrays.stream(arguments, 1, arguments.length).collect(Collectors.joining(" ")));
			}
		} catch (NumberFormatException e) {
			ju.sendPM("Malformed ID!");
		}
	}

	@Override
	public boolean isDevOnly() {
		return true;
	}

	@Override
	public String getUsage(JikaiLocale loc) {
		return getName() + " <ID> <message>";
	}
}
