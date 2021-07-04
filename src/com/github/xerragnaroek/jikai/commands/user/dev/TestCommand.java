package com.github.xerragnaroek.jikai.commands.user.dev;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jikai.anime.db.AnimeDB;
import com.github.xerragnaroek.jikai.anime.list.BigListHandler;
import com.github.xerragnaroek.jikai.commands.user.JUCommand;
import com.github.xerragnaroek.jikai.user.JikaiUser;

/**
 * 
 */
public class TestCommand implements JUCommand {

	private final Logger log = LoggerFactory.getLogger(TestCommand.class);

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
		ju.getSubscribedAnime().stream().map(AnimeDB::getAnime).forEach(a -> {
			ju.sendPM(BigListHandler.makeMessage(a, ju.getTimeZone(), ju.getLocale()));
		});
	}

	@Override
	public boolean isDevOnly() {
		return true;
	}
}
