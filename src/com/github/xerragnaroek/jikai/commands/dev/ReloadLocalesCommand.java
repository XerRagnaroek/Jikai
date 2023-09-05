package com.github.xerragnaroek.jikai.commands.dev;

import com.github.xerragnaroek.jikai.commands.guild.GuildCommand;
import com.github.xerragnaroek.jikai.commands.user.JUCommand;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocaleManager;
import com.github.xerragnaroek.jikai.user.JikaiUser;
import com.github.xerragnaroek.jikai.util.BotUtils;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.List;

/**
 *
 */
public class ReloadLocalesCommand implements JUCommand, GuildCommand {

    @Override
    public String getName() {
        return "reload_locales";
    }

    @Override
    public List<String> getAlternativeNames() {
        return List.of("rl");
    }

    @Override
    public String getDescription(JikaiLocale loc) {
        return "Reloads all locales.";
    }

    @Override
    public void executeCommand(MessageReceivedEvent event, String[] arguments) {
        JikaiLocaleManager.loadLocales();
        BotUtils.sendPM(event.getAuthor(), "Reloaded locales!");
    }

    @Override
    public void executeCommand(JikaiUser ju, String[] arguments) {
        JikaiLocaleManager.loadLocales();
        ju.sendPM("Reloaded locales!");
    }

    @Override
    public boolean isDevOnly() {
        return true;
    }

    @Override
    public String getLocaleKey() {
        return "";
    }

}
