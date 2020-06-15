package com.github.xerragnaroek.jikai.commands.guild.set;

import java.util.List;

import com.github.xerragnaroek.jikai.commands.guild.GuildCommand;
import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.jikai.Jikai;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class SetScheduleChannelCommand implements GuildCommand {

	SetScheduleChannelCommand() {}

	@Override
	public String getName() {
		return "schedule_channel";
	}

	@Override
	public String getUsage() {
		return "schedule_channel <textchannel>";
	}

	@Override
	public void executeCommand(MessageReceivedEvent event, String[] arguments) {
		Guild g = event.getGuild();
		TextChannel textC = event.getTextChannel();
		if (arguments.length >= 1) {
			List<TextChannel> tcs = g.getTextChannelsByName(arguments[0], false);
			if (!tcs.isEmpty()) {
				textC = tcs.get(0);
			} else {
				textC.sendMessage('"' + arguments[0] + "\" isn't a channel!").queue();
			}
		}
		Jikai j = Core.JM.get(g);
		j.getJikaiData().setScheduleChannelId(textC.getIdLong());
		try {
			j.getInfoChannel().sendMessage(textC.getAsMention() + " has been set as the new schedule channel!").queue();
		} catch (Exception e) {}
	}

	@Override
	public Permission[] getRequiredPermissions() {
		return new Permission[] { Permission.MANAGE_CHANNEL, Permission.MANAGE_SERVER };
	}

	@Override
	public String getDescription() {
		return "The channel for the schedule.";
	}
}
