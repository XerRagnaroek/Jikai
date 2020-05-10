
package com.github.xerragnaroek.jikai.commands.user;

import com.github.xerragnaroek.jikai.anime.db.AnimeDB;
import com.github.xerragnaroek.jikai.user.JikaiUser;
import com.github.xerragnaroek.jikai.user.JikaiUserUpdater;

/**
 * @author XerRagnaroek
 *
 */
public class TestNotifyCommand implements JUCommand {

	@Override
	public String getName() {
		return "test_notify";
	}

	@Override
	public String getDescription() {
		return "Tests the notfication system";
	}

	@Override
	public void executeCommand(JikaiUser ju, String[] arguments) {
		ju.getSubscribedAnime().stream().map(AnimeDB::getAnime).forEach(a -> {
			ju.getPreReleaseNotifcationSteps().forEach(step -> {
				ju.sendPM(JikaiUserUpdater.testNotify(a, step, ju));
			});
		});
	}

	@Override
	public boolean isDevOnly() {
		return true;
	}
}
