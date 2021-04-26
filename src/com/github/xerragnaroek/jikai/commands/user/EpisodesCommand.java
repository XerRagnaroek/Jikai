package com.github.xerragnaroek.jikai.commands.user;

import java.util.Arrays;
import java.util.List;

import com.github.xerragnaroek.jikai.user.EpisodeTracker;
import com.github.xerragnaroek.jikai.user.JikaiUser;

/**
 * 
 */
public class EpisodesCommand implements JUCommand {

	@Override
	public String getName() {
		return "episodes";
	}

	@Override
	public String getLocaleKey() {
		return "com_ju_episodes";
	}

	@Override
	public List<String> getAlternativeNames() {
		return Arrays.asList("eps");
	}

	@Override
	public void executeCommand(JikaiUser ju, String[] arguments) {
		EpisodeTracker.getTracker(ju).makeEpisodeList().forEach(eb -> ju.sendPM(eb.build()));
	}

}
