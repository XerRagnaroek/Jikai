package com.github.xerragnaroek.jikai.commands.guild;

import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.user.JikaiUserManager;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;


public class ForceRegisterCommand implements GuildCommand {

    @Override
    public String getName() {
        return "register";
    }

    @Override
    public void executeCommand(MessageReceivedEvent event, String[] arguments) {
        long id = event.getAuthor().getIdLong();
        if (!JikaiUserManager.getInstance().isKnownJikaiUser(id)) {
            JikaiUserManager.getInstance().registerNewUser(id, Core.JM.get(event.getGuild().getIdLong()));
        }
    }

    @Override
    public boolean isJikaiUserOnly() {
        return false;
    }

    @Override
    public String getLocaleKey() {
        return "com_g_register";
    }
}
