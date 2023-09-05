package com.github.xerragnaroek.jikai.commands.guild;

import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.util.BotUtils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.utils.FileUpload;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * This is only for the main Jikai server
 */
public class SetupJikaiServerCommand implements GuildCommand {

    private Guild g;

    @Override
    public String getName() {
        return "setup";
    }

    @Override
    public String getDescription(JikaiLocale loc) {
        return "Finish the setup for the central jikai server";
    }

    @Override
    public void executeCommand(MessageReceivedEvent event, String[] arguments) {
        g = event.getGuild();
        g.createTextChannel("welcome").addPermissionOverride(g.getPublicRole(), Arrays.asList(Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY), List.of(Permission.MESSAGE_SEND)).addPermissionOverride(g.getSelfMember(), Permission.ALL_CHANNEL_PERMISSIONS, 0L).setTopic("Welcome to Jikai! Please read the message below.").submit().thenAccept(this::welcome);
    }

    private void welcome(TextChannel tc) {
        try {
            tc.sendMessage("Welcome to my server! Please read the message below and enjoy your stay! :kissing_heart:").addFiles(FileUpload.fromData(BotUtils.imageToByteArray(ImageIO.read(new File("./data/jikai.png"))), "jikai.png")).queue();
        } catch (IOException e) {
            Core.ERROR_LOG.error("Couldn't load jikai image!", e);
        }
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
