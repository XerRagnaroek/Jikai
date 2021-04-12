package com.github.xerragnaroek.jikai.commands.user.dev;

import com.github.xerragnaroek.jikai.commands.user.JUCommand;
import com.github.xerragnaroek.jikai.user.JikaiUser;

/**
 * 
 */
public class UnsubAllCommand implements JUCommand {

	@Override
	public String getName() {
		return "unsub_all";
	}

	@Override
	public String getLocaleKey() {
		return "";
	}

	@Override
	public void executeCommand(JikaiUser ju, String[] arguments) {
		ju.getSubscribedAnime().forEach(id -> ju.unsubscribeAnime(id, "Unsub all command"));
	}

	@Override
	public boolean isDevOnly() {
		return true;
	}
}
