package com.github.xerragnaroek.jikai.commands.user;

import com.github.xerragnaroek.jikai.user.JikaiUser;
import com.github.xerragnaroek.jikai.util.BotUtils;

import java.util.List;

/**
 *
 */
public class UnlinkUserCommand implements JUCommand {

    @Override
    public String getName() {
        return "unlink";
    }

    @Override
    public String getLocaleKey() {
        return "com_ju_unlink";
    }

    @Override
    public void executeCommand(JikaiUser ju, String[] arguments) {
        if (arguments.length < 1) {
            ju.sendPM(getUsage(ju.getLocale()));
        } else {
            JikaiUser tgt = BotUtils.resolveUser(arguments[0]);
            if (tgt != null) {
                if (tgt.unlinkUser(ju)) {
                    ju.sendPM(BotUtils.embedBuilder().setTitle(ju.getLocale().getStringFormatted("com_ju_unlink_suc", List.of("name"), tgt.getUser().getName())).setThumbnail(tgt.getUser().getEffectiveAvatarUrl()).build());
                } else {
                    ju.sendPM(BotUtils.embedBuilder().setTitle(ju.getLocale().getStringFormatted("com_ju_unlink_not_linked", List.of("name"), tgt.getUser().getName())).setThumbnail(tgt.getUser().getEffectiveAvatarUrl()).build());
                }
            } else {
                ju.sendPM(BotUtils.makeSimpleEmbed(ju.getLocale().getString("com_ju_unlink_fail")));
            }
        }
    }

}
