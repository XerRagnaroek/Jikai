package com.github.xerragnaroek.jikai.commands.user.dev;

import java.util.Arrays;
import java.util.stream.Collectors;

import com.github.xerragnaroek.jikai.commands.user.JUCommand;
import com.github.xerragnaroek.jikai.user.JikaiUser;

/**
 * 
 */
public class CodePointTestCommand implements JUCommand {

	@Override
	public String getName() {
		return "code_point";
	}

	@Override
	public String getLocaleKey() {
		return "";
	}

	@Override
	public void executeCommand(JikaiUser ju, String[] arguments) {
		String text = Arrays.stream(arguments[0].split("U+")).filter(s -> !s.isEmpty()).map(s -> Integer.parseInt(s, 16)).map(Character::toString).collect(Collectors.joining());
		ju.sendPM(text);
	}

	@Override
	public boolean isDevOnly() {
		return true;
	}
}
