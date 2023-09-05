package com.github.xerragnaroek.jikai.commands.user;

import com.github.xerragnaroek.jikai.user.JikaiUser;
import com.github.xerragnaroek.jikai.user.JikaiUserManager;

/**
 *
 */
public class UnregisterCommand implements JUCommand {

    @Override
    public String getName() {
        return "unregister";
    }

    @Override
    public void executeCommand(JikaiUser ju, String[] arguments) {
        ju.sendPM(ju.getLocale().getString("ju_unregister"));
        JikaiUserManager.getInstance().removeUser(ju.getId());
    }

    @Override
    public String getLocaleKey() {
        return "com_ju_unregister";
    }

}
