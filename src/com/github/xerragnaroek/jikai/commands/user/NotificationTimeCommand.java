package com.github.xerragnaroek.jikai.commands.user;

import com.github.xerragnaroek.jikai.user.JikaiUser;

import java.util.List;

/**
 * @author XerRagnaroek
 */
public class NotificationTimeCommand implements JUCommand {

    @Override
    public String getName() {
        return "notif_time";
    }

    @Override
    public List<String> getAlternativeNames() {
        return List.of("nt");
    }

    @Override
    public void executeCommand(JikaiUser ju, String[] arguments) {
        if (arguments.length < 2) {
            throw new IllegalArgumentException("Too few arguments!");
        }
        String tmp = arguments[1];
        if (!tmp.contains("d") && !tmp.contains("h") && !tmp.contains("m")) {
            ju.sendPMFormat(ju.getLocale().getStringFormatted("com_ju_notif_time_invalid", List.of("input"), tmp));
            return;
        }
        if (arguments[0].equals("add")) {
            ju.addReleaseSteps(tmp);
        } else if (arguments[0].equals("remove") || arguments[0].equals("rem")) {
            ju.removeReleaseSteps(tmp);
        }
    }

    @Override
    public String getLocaleKey() {
        return "com_ju_notif_time";
    }
}
