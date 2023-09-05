package com.github.xerragnaroek.jikai.commands.user;

import com.github.xerragnaroek.jikai.commands.ComUtils;
import com.github.xerragnaroek.jikai.user.JikaiUser;

import java.util.List;

/**
 *
 */
public class NextEpisodeMsgCommand implements JUCommand {

    @Override
    public String getName() {
        return "next_episode";
    }

    @Override
    public List<String> getAlternativeNames() {
        return List.of("next_ep");
    }

    @Override
    public String getLocaleKey() {
        return "com_ju_next_ep_msg";
    }

    @Override
    public void executeCommand(JikaiUser ju, String[] arguments) {
        if (arguments.length >= 1) {
            ComUtils.trueFalseCommand(arguments[0], ju, b -> {
                ju.setSendNextEpMessage(b);
                ju.sendPM(ju.getLocale().getString("com_ju_next_ep_msg_" + b));
            });
        } else {
            ju.sendPM(getUsage(ju.getLocale()));
        }
    }

}
