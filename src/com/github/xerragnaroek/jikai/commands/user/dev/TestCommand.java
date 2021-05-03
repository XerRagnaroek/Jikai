package com.github.xerragnaroek.jikai.commands.user.dev;

import java.time.ZoneId;
import java.util.stream.Collectors;

import com.github.xerragnaroek.jikai.commands.user.JUCommand;
import com.github.xerragnaroek.jikai.user.JikaiUser;
import com.github.xerragnaroek.jikai.util.BotUtils;

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
		BotUtils.sendPM(ju.getUser(), ZoneId.getAvailableZoneIds().stream().sorted().collect(Collectors.joining("\n")));
	}

	@Override
	public boolean isDevOnly() {
		return true;
	}
}
