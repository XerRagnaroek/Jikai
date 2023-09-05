package com.github.xerragnaroek.jikai.commands.dev;

import com.github.xerragnaroek.jikai.anime.db.AnimeDB;
import com.github.xerragnaroek.jikai.commands.user.JUCommand;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.user.JikaiUser;

import java.util.Arrays;
import java.util.List;

/**
 *
 */
public class UpdateThreadStatusCommand implements JUCommand {

    @Override
    public String getName() {
        return "update_status";
    }

    @Override
    public List<String> getAlternativeNames() {
        return Arrays.asList("uts", "thread_status", "ut_status");
    }

    @Override
    public String getDescription(JikaiLocale loc) {
        return "Checks the status of the anime db update thread.";
    }

    @Override
    public void executeCommand(JikaiUser ju, String[] arguments) {
        ju.sendPM("Update thread is running: " + AnimeDB.isUpdateThreadRunning());
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
