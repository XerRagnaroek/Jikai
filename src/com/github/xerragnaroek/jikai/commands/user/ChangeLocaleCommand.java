package com.github.xerragnaroek.jikai.commands.user;

import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocaleManager;
import com.github.xerragnaroek.jikai.user.JikaiUser;

import java.util.Arrays;
import java.util.List;

/**
 *
 */
public class ChangeLocaleCommand implements JUCommand {

    @Override
    public String getName() {
        return "language";
    }

    @Override
    public String getDescription(JikaiLocale loc) {
        return loc.getStringFormatted("com_ju_changeloc_desc", List.of("langs"), JikaiLocaleManager.getInstance().getLocaleIdentifiers().toString());
    }

    @Override
    public void executeCommand(JikaiUser ju, String[] arguments) {
        JikaiLocale loc;
        if (arguments.length > 0 && (loc = JikaiLocaleManager.getInstance().getLocale(arguments[0])) != null) {
            ju.setLocale(loc);
            ju.sendPM(loc.getString("com_ju_changeloc_success"));
        } else {
            ju.sendPM(ju.getLocale().getStringFormatted("com_ju_changeloc_fail", List.of("langs"), JikaiLocaleManager.getInstance().getLocaleIdentifiers().toString()));
        }
    }

    @Override
    public List<String> getAlternativeNames() {
        return Arrays.asList("locale", "lang");
    }

    @Override
    public String getLocaleKey() {
        return "com_ju_changeloc";
    }
}
