package com.github.xerragnaroek.jikai.commands;

import com.github.xerragnaroek.jikai.commands.guild.GuildCommand;
import com.github.xerragnaroek.jikai.commands.user.JUCommand;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.user.JikaiUser;
import com.github.xerragnaroek.jikai.user.JikaiUserManager;
import com.github.xerragnaroek.jikai.util.BotUtils;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


/**
 *
 */
public class BugCommand implements GuildCommand, JUCommand {

    private static final AtomicInteger bugs = new AtomicInteger(0);
    private final Logger log = LoggerFactory.getLogger(BugCommand.class);

    @Override
    public String getName() {
        return "bug";
    }

    @Override
    public String getDescription(JikaiLocale loc) {
        return "Report a bug. Please try to give a precise explanation as to what you expected to happen (if anything) versus what actually happened.";
    }

    @Override
    public String getUsage(JikaiLocale loc) {
        return "bug <message>";
    }

    @Override
    public void executeCommand(MessageReceivedEvent event, String[] arguments) {
        printBug(String.join(" ", arguments), JikaiUserManager.getInstance().getUser(event.getMember().getIdLong()));
    }

    @Override
    public void executeCommand(JikaiUser ju, String[] arguments) {
        printBug(String.join(" ", arguments), ju);
    }

    @Override
    public boolean isJikaiUserOnly() {
        return true;
    }

    private void printBug(String msg, JikaiUser ju) {
        String message = ju.getUser().getName() + " reported a bug:\nBUG #" + bugs.incrementAndGet() + ": " + msg;
        log.error(message);
        ju.sendPM(ju.getLocale().getStringFormatted("ju_bug", List.of("bugN"), bugs.get()));
        BotUtils.sendToDev(message);
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public String getLocaleKey() {
        return "";
    }
}
