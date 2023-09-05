package com.github.xerragnaroek.jikai.commands.guild;

import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.jikai.Jikai;
import com.github.xerragnaroek.jikai.jikai.JikaiData;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class EnableCommandsCommand implements GuildCommand {
    private final Logger log = LoggerFactory.getLogger(EnableCommandsCommand.class);

    @Override
    public String getName() {
        return "enable_commands";
    }

    @Override
    public List<String> getAlternativeNames() {
        return List.of("en_coms");
    }

    @Override
    public void executeCommand(MessageReceivedEvent event, String[] arguments) {
        Guild g = event.getGuild();
        log.debug("Executing EnableCommandsCommand on guild {}#{}", g.getName(), g.getId());
        Jikai j = Core.JM.get(g);
        JikaiData jd = j.getJikaiData();
        if (jd.areCommandsEnabled()) {
            jd.setCommandsEnabled(false);
            try {
                j.getInfoChannel().sendMessage(j.getLocale().getStringFormatted("com_g_enable_com_msg", List.of("pre"), jd.getPrefix())).queue();
            } catch (Exception e) {
            }
        }
    }

    @Override
    public Permission[] getRequiredPermissions() {
        return new Permission[]{Permission.MANAGE_SERVER};
    }

    @Override
    public boolean isAlwaysEnabled() {
        return true;
    }

    @Override
    public String getLocaleKey() {
        return "com_g_enable_com";
    }
}
