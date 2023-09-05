package com.github.xerragnaroek.jikai.commands.user;

import com.github.xerragnaroek.jikai.user.EpisodeTrackerManager;
import com.github.xerragnaroek.jikai.user.JikaiUser;

import java.util.List;

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
        return List.of("eps");
    }

    @Override
    public void executeCommand(JikaiUser ju, String[] arguments) {
        EpisodeTrackerManager.getTracker(ju).makeEpisodeList().forEach(msg -> ju.sendPM(msg));
    }

}
