package com.github.xerragnaroek.jikai.commands.user;

import com.github.xerragnaroek.jikai.user.JikaiUser;
import com.github.xerragnaroek.jikai.user.token.JikaiUserAniTokenManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class UnlinkAniAccountCommand implements JUCommand {
    private final Logger log = LoggerFactory.getLogger(UnlinkAniAccountCommand.class);

    @Override
    public String getName() {
        return "unlink_ani";
    }

    @Override
    public void executeCommand(JikaiUser ju, String[] arguments) {
        log.debug("{} unlinking ani account", ju.getId());
        JikaiUserAniTokenManager.removeToken(ju);
        ju.setAniId(0);
        ju.sendPM(ju.getLocale().getString("com_ju_unlink_ani_msg"));
    }

    @Override
    public String getLocaleKey() {
        return "com_ju_unlink_ani";
    }

}
