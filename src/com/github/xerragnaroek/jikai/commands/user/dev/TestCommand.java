package com.github.xerragnaroek.jikai.commands.user.dev;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jikai.anime.db.AnimeDB;
import com.github.xerragnaroek.jikai.commands.user.JUCommand;
import com.github.xerragnaroek.jikai.user.JikaiUser;
import com.github.xerragnaroek.jikai.util.DetailedAnimeMessageBuilder;

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
			DetailedAnimeMessageBuilder damb = new DetailedAnimeMessageBuilder(a, ju.getTimeZone(), ju.getLocale());
			if (arguments.length > 0) {
				damb.setDescription(String.join(" ", arguments));
			}
			ju.sendPM(damb.withAll(false).build());
		});
	}

	@Override
	public boolean isDevOnly() {
		return true;
	}
}
