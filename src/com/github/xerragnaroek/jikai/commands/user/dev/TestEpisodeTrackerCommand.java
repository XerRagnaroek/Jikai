package com.github.xerragnaroek.jikai.commands.user.dev;

import java.util.Random;

import com.github.xerragnaroek.jikai.anime.db.AnimeDB;
import com.github.xerragnaroek.jikai.commands.user.JUCommand;
import com.github.xerragnaroek.jikai.user.EpisodeTracker;
import com.github.xerragnaroek.jikai.user.JikaiUser;

/**
 * 
 */
public class TestEpisodeTrackerCommand implements JUCommand {

	@Override
	public String getName() {
		return "test_episode_tracker";
	}

	@Override
	public String getLocaleKey() {
		return "";
	}

	@Override
	public void executeCommand(JikaiUser ju, String[] arguments) {
		AnimeDB.getLoadedAnime().forEach(a -> {
			EpisodeTracker et = EpisodeTracker.getTracker(ju);
			for (int i = 1; i <= a.getEpisodes(); i++) {
				et.registerEpisodeDetailed(a.getId(), Math.abs(new Random().nextLong()), i);
			}
		});
	}

	@Override
	public boolean isDevOnly() {
		return true;
	}
}
