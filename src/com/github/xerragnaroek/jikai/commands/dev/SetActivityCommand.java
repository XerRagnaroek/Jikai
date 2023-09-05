package com.github.xerragnaroek.jikai.commands.dev;

import com.github.xerragnaroek.jikai.commands.user.JUCommand;
import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.user.JikaiUser;
import net.dv8tion.jda.api.entities.Activity;
import org.apache.commons.lang3.ArrayUtils;

/**
 *
 */
public class SetActivityCommand implements JUCommand {

    @Override
    public String getName() {
        return "set_activity";
    }

    @Override
    public String getLocaleKey() {
        return "";
    }

    @Override
    public String getUsage(JikaiLocale loc) {
        return "set_activity <0-4> <str> <in case of 4: url> OR null";
    }

    @Override
    public String getDescription(JikaiLocale loc) {
        return "Set Jikai's displayed activity or reset it.\nActivity type (0-4)\n0 = competing(str)\n1 = listening(str)\n2 = playing(str)\n3 = watching(str)\n4 = streaming(str,url)";
    }

    @Override
    public void executeCommand(JikaiUser ju, String[] arguments) {
        Activity a = null;
        if (arguments.length < 1) {
            ju.sendPM("Missing arguments\nActivity type (0-4)\n0 = competing(str)\n1 = listening(str)\n2 = playing(str)\n3 = watching(str)\n4 = streaming(str,url)");
            return;
        }
        if (arguments[0].equals("null")) {
            Core.JDA.getPresence().setActivity(null);
            return;
        }
        String str = "";
        String url = "";
        for (String arg : ArrayUtils.subarray(arguments, 1, arguments.length)) {
            if (arg.equals("%%")) {
                url = " ";
            } else if (!url.isEmpty()) {
                url += arg;
            } else {
                str = str + " " + arg;
            }
        }
        str = str.trim();
        url = url.trim();
        if (str.isBlank()) {
            ju.sendPM("Name may not be blank!");
            return;
        }
        try {
            int i = Integer.parseInt(arguments[0]);
            switch (i) {
                case 0 -> a = Activity.competing(str);
                case 1 -> a = Activity.listening(str);
                case 2 -> a = Activity.playing(str);
                case 3 -> a = Activity.watching(str);
                case 4 -> {
                    if (url.isEmpty()) {
                        ju.sendPM("Missing url! The url comes after  \" %% \"");
                        return;
                    }
                    a = Activity.streaming(str, url);
                }
            }
            Core.JDA.getPresence().setActivity(a);
        } catch (NumberFormatException e) {
            ju.sendPM("Missing Activity type (0-4)\n0 = competing(str)\n1 = listening(str)\n2 = playing(str)\n3 = watching(str)\n4 = streaming(str,url)");
        }
    }

    @Override
    public boolean isDevOnly() {
        return true;
    }

}
