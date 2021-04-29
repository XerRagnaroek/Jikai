package com.github.xerragnaroek.jikai.commands.user;

import java.util.Arrays;
import java.util.List;

import com.github.xerragnaroek.jikai.user.JikaiUser;

/**
 * 
 */
public class NextEpisodeMsgCommand implements JUCommand {

	@Override
	public String getName() {
		return "next_episode";
	}

	@Override
	public List<String> getAlternativeNames() {
		return Arrays.asList("next_ep");
	}

	@Override
	public String getLocaleKey() {
		return "com_ju_next_ep_msg";
	}

	@Override
	public void executeCommand(JikaiUser ju, String[] arguments) {
		boolean bool = false;
		if (arguments.length >= 1 && ((bool = arguments[0].equalsIgnoreCase("true")) || arguments[0].equalsIgnoreCase("false"))) {
			ju.setSendNextEpMessage(bool);
			ju.sendPM(ju.getLocale().getString("com_ju_next_ep_msg_" + bool));
		} else {
			ju.sendPM(getUsage(ju.getLocale()));
		}
	}

}
