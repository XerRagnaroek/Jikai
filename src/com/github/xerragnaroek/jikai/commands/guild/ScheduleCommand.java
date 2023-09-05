package com.github.xerragnaroek.jikai.commands.guild;

import com.github.xerragnaroek.jikai.anime.schedule.ScheduleManager;
import com.github.xerragnaroek.jikai.core.Core;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/**
 * The "schedule" command
 *
 * @author XerRagnaroek
 */
public class ScheduleCommand implements GuildCommand {

    @Override
    public String getName() {
        return "schedule";
    }

    @Override
    public void executeCommand(MessageReceivedEvent event, String[] arguments) {
        ScheduleManager.sendScheduleToJikai(Core.JM.get(event.getGuild()));
    }

    @Override
    public Permission[] getRequiredPermissions() {
        return CommandHandler.MOD_PERMS;
    }

    @Override
    public String getLocaleKey() {
        return "com_g_sched";
    }
}
