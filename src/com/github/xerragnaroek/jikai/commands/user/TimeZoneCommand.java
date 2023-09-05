package com.github.xerragnaroek.jikai.commands.user;

import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.user.JikaiUser;

import java.time.ZoneId;
import java.util.List;

public class TimeZoneCommand implements JUCommand {

    @Override
    public String getName() {
        return "timezone";
    }

    @Override
    public List<String> getAlternativeNames() {
        return List.of("tz");
    }

    @Override
    public void executeCommand(JikaiUser ju, String[] arguments) {
        if (arguments.length > 0) {
            String id = arguments[0].trim();
            JikaiLocale loc = ju.getLocale();
            try {
                ZoneId z = ZoneId.of(id);
                ju.setTimeZone(z);
                ju.sendPMFormat(loc.getStringFormatted("com_ju_tz_msg", List.of("tz"), z.getId()));
            } catch (Exception e) {
                ju.sendPMFormat(loc.getString("com_ju_tz_invalid"));
            }
        }
    }

    @Override
    public String getLocaleKey() {
        return "com_ju_tz";
    }

}
