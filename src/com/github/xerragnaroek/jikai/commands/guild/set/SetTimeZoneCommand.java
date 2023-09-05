package com.github.xerragnaroek.jikai.commands.guild.set;

import com.github.xerragnaroek.jikai.commands.guild.GuildCommand;
import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.jikai.Jikai;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.List;

public class SetTimeZoneCommand implements GuildCommand {
    SetTimeZoneCommand() {
    }

    @Override
    public String getName() {
        return "timezone";
    }

    @Override
    public void executeCommand(MessageReceivedEvent event, String[] arguments) {
        String zone = arguments[0];
        Jikai j = Core.JM.get(event.getGuild());
        try {
            ZoneId z = ZoneId.of(zone);
            j.getJikaiData().setTimeZone(z);
            j.getInfoChannel().sendMessage(j.getLocale().getStringFormatted("com_g_set_tz_success", List.of("tz"), z.getId())).queue();
        } catch (DateTimeException e) {
            event.getChannel().sendMessage(j.getLocale().getStringFormatted("com_g_set_tz_fail", List.of("user"), event.getAuthor().getAsMention())).queue();
        } catch (Exception e) {
            // infochannel doesn't exist, already handled in getInfoChannel()
        }
    }

    @Override
    public Permission[] getRequiredPermissions() {
        return new Permission[]{Permission.MANAGE_SERVER};
    }

    @Override
    public List<String> getAlternativeNames() {
        return List.of("tz");
    }

    @Override
    public String getLocaleKey() {
        return "com_g_set_tz";
    }
}
