package com.github.xerragnaroek.jikai.commands.user;

import com.github.xerragnaroek.jikai.user.JikaiUser;
import com.github.xerragnaroek.jikai.util.BotUtils;

/**
 * @author XerRagnaroek
 */
public class ConfigCommand implements JUCommand {

    @Override
    public String getName() {
        return "config";
    }

    @Override
    public void executeCommand(JikaiUser ju, String[] arguments) {
        ju.sendPM(BotUtils.makeConfigEmbed(ju).build());
    }

    @Override
    public String getLocaleKey() {
        return "com_ju_config";
    }
}
