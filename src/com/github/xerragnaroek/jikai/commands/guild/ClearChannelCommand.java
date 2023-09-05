package com.github.xerragnaroek.jikai.commands.guild;

import com.github.xerragnaroek.jikai.util.BotUtils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/**
 *
 */
public class ClearChannelCommand implements GuildCommand {

    @Override
    public String getName() {
        return "clear";
    }

    @Override
    public void executeCommand(MessageReceivedEvent event, String[] arguments) {
        BotUtils.clearChannel(event.getChannel().asTextChannel());
    }

    @Override
    public Permission[] getRequiredPermissions() {
        return CommandHandler.MOD_PERMS;
    }

    @Override
    public String getLocaleKey() {
        return "com_g_clear";
    }
}
