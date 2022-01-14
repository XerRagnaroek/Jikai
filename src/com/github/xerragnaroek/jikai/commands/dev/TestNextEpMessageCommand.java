package com.github.xerragnaroek.jikai.commands.dev;

import com.github.xerragnaroek.jasa.Anime;
import com.github.xerragnaroek.jikai.anime.db.AnimeDB;
import com.github.xerragnaroek.jikai.commands.user.JUCommand;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.user.JikaiUser;
import com.github.xerragnaroek.jikai.user.JikaiUserManager;
import com.github.xerragnaroek.jikai.user.JikaiUserUpdater;

/**
 * 
 */
public class TestNextEpMessageCommand implements JUCommand {

	@Override
	public String getName() {
		return "test_next_ep_msg";
	}

	@Override
	public String getDescription(JikaiLocale loc) {
		return "Test the next episode message";
	}

	@Override
	public void executeCommand(JikaiUser ju, String[] arguments) {
		JikaiUserUpdater juu = JikaiUserManager.getInstance().getUserUpdater();
		ju.getSubscribedAnime().stream().map(AnimeDB::getAnime).filter(Anime::hasDataForNextEpisode).forEach(a -> juu.testNextEpMessage(ju, a));
	}

	@Override
	public String getLocaleKey() {
		return "";
	}

	@Override
	public boolean isDevOnly() {
		return true;
	}
}
