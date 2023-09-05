package com.github.xerragnaroek.jikai.commands.user;

import com.github.xerragnaroek.jikai.user.JikaiUser;
import com.github.xerragnaroek.jikai.user.token.JikaiUserAniTokenManager;
import com.github.xerragnaroek.jikai.util.BotUtils;
import com.github.xerragnaroek.jikai.util.Pair;

import java.util.List;

/**
 *
 */
public class AniAuthCommand implements JUCommand {

    @Override
    public String getName() {
        return "ani_auth";
    }

    @Override
    public String getLocaleKey() {
        return "com_ju_ani_auth";
    }

    @Override
    public void executeCommand(JikaiUser ju, String[] arguments) {
        if (arguments.length == 0) {
            ju.sendPM(BotUtils.localedEmbed(ju.getLocale(), "ju_eb_ani_auth", Pair.of(List.of("link"), new Object[]{JikaiUserAniTokenManager.getOAuthUrl()})));
        } else {
            if (arguments[0].equals("revoke")) {
                if (ju.getAniId() > 0) {
                    if (JikaiUserAniTokenManager.hasToken(ju)) {
                        JikaiUserAniTokenManager.removeToken(ju);
                        ju.sendPM(BotUtils.localedEmbed(ju.getLocale(), "ju_eb_ani_auth_rev"));
                    } else {
                        ju.sendPM(BotUtils.makeSimpleEmbed(ju.getLocale().getString("ju_eb_ani_auth_rev_fail")));
                    }
                } else {
                    ju.sendPM(BotUtils.makeSimpleEmbed(ju.getLocale().getString("ju_eb_ani_auth_rev_fail_user")));
                }
            }
        }
    }

}
