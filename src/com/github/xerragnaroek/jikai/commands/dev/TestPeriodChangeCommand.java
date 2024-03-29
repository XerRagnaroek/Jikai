package com.github.xerragnaroek.jikai.commands.dev;

import com.github.xerragnaroek.jasa.Anime;
import com.github.xerragnaroek.jikai.anime.db.AnimeDB;
import com.github.xerragnaroek.jikai.commands.user.JUCommand;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.user.JikaiUser;
import com.github.xerragnaroek.jikai.user.JikaiUserManager;
import com.github.xerragnaroek.jikai.user.JikaiUserUpdater;

import java.util.List;
import java.util.Random;

/**
 *
 */
public class TestPeriodChangeCommand implements JUCommand {

    @Override
    public String getName() {
        return "test_period_change";
    }

    @Override
    public List<String> getAlternativeNames() {
        return List.of("tpc");
    }

    @Override
    public String getDescription(JikaiLocale loc) {
        return "Tests the messages being sent when the release period of an anime changes.";
    }

    @Override
    public void executeCommand(JikaiUser ju, String[] arguments) {
        JikaiUserUpdater juu = JikaiUserManager.getInstance().getUserUpdater();
        ju.getSubscribedAnime().forEach(id -> {
            Anime a = AnimeDB.getAnime(id);
            if (a.hasDataForNextEpisode()) {
                ju.sendPM(juu.testPeriodChanged(a, new Random().nextInt(100), ju));
                ju.sendPM(juu.testPeriodChanged(a, Math.negateExact((long) (new Random().nextInt(100))), ju));
            }
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
