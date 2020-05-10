package com.github.xerragnaroek.jikai.commands.user;

import com.github.xerragnaroek.jikai.anime.db.AnimeDB;
import com.github.xerragnaroek.jikai.user.JikaiUser;
import com.github.xerragnaroek.jikai.user.JikaiUserUpdater;

/**
 * @author XerRagnaroek
 *
 */
public class TestPostponeCommand implements JUCommand {

	@Override
	public String getName() {
		return "test_postpone";
	}

	@Override
	public String getDescription() {
		return "Sends the postpone embeds";
	}

	@Override
	public void executeCommand(JikaiUser ju, String[] arguments) {
		long delay = Long.parseLong(arguments[0]);
		ju.getSubscribedAnime().stream().map(AnimeDB::getAnime).forEach(a -> ju.sendPM(JikaiUserUpdater.testPostpone(a, delay, ju)));
	}

	@Override
	public boolean isDevOnly() {
		return true;
	}
}
