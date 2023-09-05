package com.github.xerragnaroek.jikai.commands.guild.set;

import com.github.xerragnaroek.jikai.commands.guild.GuildCommand;
import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.jikai.Jikai;
import com.github.xerragnaroek.jikai.jikai.JikaiData;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocaleManager;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.Arrays;
import java.util.List;


public class SetLanguageCommand implements GuildCommand {

    @Override
    public String getName() {
        return "language";
    }

    @Override
    public List<String> getAlternativeNames() {
        return Arrays.asList("lang", "locale");
    }

    @Override
    public void executeCommand(MessageReceivedEvent event, String[] arguments) {
        JikaiLocale loc = JikaiLocaleManager.getInstance().getLocale(arguments[0]);
        Jikai j = Core.JM.get(event.getGuild());
        if (loc != null) {
            JikaiData jd = j.getJikaiData();
            if (!jd.getLocale().equals(loc)) {
                jd.setLocale(loc);
                try {
                    j.getInfoChannel().sendMessage(loc.getStringFormatted("com_g_set_lang_success", List.of("lang"), loc.getLanguageName())).queue();
                } catch (Exception e) {
                }
            }
        } else {
            event.getChannel().sendMessage(j.getLocale().getStringFormatted("com_g_set_lang_fail", List.of("langs"), JikaiLocaleManager.getInstance().getLocaleIdentifiers())).queue();
        }
    }

    @Override
    public String getLocaleKey() {
        return "com_g_set_lang";
    }

}
