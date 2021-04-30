package com.github.xerragnaroek.jikai.commands.user.dev;

import java.util.Arrays;

import com.github.xerragnaroek.jikai.commands.user.JUCommand;
import com.github.xerragnaroek.jikai.user.JikaiUser;
import com.github.xerragnaroek.jikai.util.BotUtils;
import com.github.xerragnaroek.jikai.util.Pair;

/**
 * 
 */
public class TestCommand implements JUCommand {

	@Override
	public String getName() {
		return "test";
	}

	@Override
	public String getLocaleKey() {
		return "";
	}

	@Override
	public void executeCommand(JikaiUser ju, String[] arguments) {
		ju.sendPM(BotUtils.localedEmbed(ju.getLocale(), "setup_greetings_eb", Pair.of(Arrays.asList("name"), new Object[] { ju.getUser().getName() })));
	}

}
