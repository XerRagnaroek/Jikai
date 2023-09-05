package com.github.xerragnaroek.jikai.commands.dev;

import com.github.xerragnaroek.jikai.anime.db.AnimeDB;
import com.github.xerragnaroek.jikai.commands.user.JUCommand;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.user.JikaiUser;
import com.github.xerragnaroek.jikai.user.JikaiUserManager;

import java.util.Random;

/**
 * @author XerRagnaroek
 */
public class TestPostponeCommand implements JUCommand {

    @Override
    public String getName() {
        return "test_postpone";
    }

    @Override
    public String getDescription(JikaiLocale loc) {
        return "Sends the postpone embeds";
    }

    @Override
    public void executeCommand(JikaiUser ju, String[] arguments) {
        long delay = new Random().nextInt(10080) + 1;
        ju.getSubscribedAnime().stream().map(AnimeDB::getAnime).forEach(a -> ju.sendPM(JikaiUserManager.getInstance().getUserUpdater().testPostpone(a, delay, ju)));
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
