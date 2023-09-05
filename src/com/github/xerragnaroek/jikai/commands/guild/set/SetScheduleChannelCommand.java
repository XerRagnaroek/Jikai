package com.github.xerragnaroek.jikai.commands.guild.set;

import com.github.xerragnaroek.jikai.anime.schedule.ScheduleManager;
import com.github.xerragnaroek.jikai.commands.guild.GuildCommand;
import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.jikai.Jikai;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.Arrays;
import java.util.List;


public class SetScheduleChannelCommand implements GuildCommand {

    SetScheduleChannelCommand() {
    }

    @Override
    public String getName() {
        return "schedule_channel";
    }

    @Override
    public void executeCommand(MessageReceivedEvent event, String[] arguments) {
        Guild g = event.getGuild();
        Jikai j = Core.JM.get(g);
        TextChannel textC = event.getChannel().asTextChannel();
        if (arguments.length >= 1) {
            List<TextChannel> tcs = g.getTextChannelsByName(arguments[0], false);
            if (!tcs.isEmpty()) {
                textC = tcs.get(0);
            } else {
                textC.sendMessage(j.getLocale().getStringFormatted("com_g_set_sched_fail", List.of("channel"), arguments[0])).queue();
                return;
            }
        }

        boolean firstTimeSet = !j.hasScheduleChannelSet();
        j.getJikaiData().setScheduleChannelId(textC.getIdLong());
        if (firstTimeSet) {
            ScheduleManager.sendScheduleToJikai(j);
        }
        try {
            j.getInfoChannel().sendMessage(j.getLocale().getStringFormatted("com_g_set_sched_success", List.of("channel"), textC.getAsMention())).queue();
        } catch (Exception e) {
        }
    }

    @Override
    public Permission[] getRequiredPermissions() {
        return new Permission[]{Permission.MANAGE_CHANNEL, Permission.MANAGE_SERVER};
    }

    @Override
    public List<String> getAlternativeNames() {
        return Arrays.asList("sched_chan", "schedule_chan", "s_chan", "sc");
    }

    @Override
    public String getLocaleKey() {
        return "com_g_set_sched";
    }
}
