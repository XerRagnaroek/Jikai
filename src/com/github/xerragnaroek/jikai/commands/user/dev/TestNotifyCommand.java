
package com.github.xerragnaroek.jikai.commands.user.dev;

import com.github.xerragnaroek.jikai.anime.db.AnimeDB;
import com.github.xerragnaroek.jikai.commands.user.JUCommand;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.user.EpisodeTracker;
import com.github.xerragnaroek.jikai.user.EpisodeTrackerManager;
import com.github.xerragnaroek.jikai.user.JikaiUser;
import com.github.xerragnaroek.jikai.user.JikaiUserManager;
import com.github.xerragnaroek.jikai.util.BotUtils;

import net.dv8tion.jda.api.entities.User;

/**
 * @author XerRagnaroek
 */
public class TestNotifyCommand implements JUCommand {

	@Override
	public String getName() {
		return "test_notify";
	}

	@Override
	public String getDescription(JikaiLocale loc) {
		return "Tests the notfication system";
	}

	@Override
	public void executeCommand(JikaiUser ju, String[] arguments) {
		User u = ju.getUser();
		EpisodeTracker et = EpisodeTrackerManager.getTracker(ju);
		ju.getSubscribedAnime().stream().map(AnimeDB::getAnime).forEach(a -> {
			ju.getPreReleaseNotifcationSteps().forEach(step -> BotUtils.sendPM(u, JikaiUserManager.getInstance().getUserUpdater().testNotify(a, step, ju)).get(0).thenAccept(m -> {
				if (step == 0) {
					m.pin().queue();
					et.registerEpisode(a, m.getIdLong());
				}
			}));
		});
	}

	@Override
	public boolean isDevOnly() {
		return true;
	}

	@Override
	public String getLocaleKey() {
		return "";
	}
}
