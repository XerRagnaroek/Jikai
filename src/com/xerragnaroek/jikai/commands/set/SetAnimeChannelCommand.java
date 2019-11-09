package com.xerragnaroek.jikai.commands.set;

import java.util.List;

import com.xerragnaroek.jikai.commands.Command;
import com.xerragnaroek.jikai.commands.CommandHandler;
import com.xerragnaroek.jikai.core.Core;
import com.xerragnaroek.jikai.jikai.Jikai;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/**
 * Command that sets the channel the bot posts automated stuff in.
 * 
 * @author XerRagnarök
 *
 */
public class SetAnimeChannelCommand implements Command {

	SetAnimeChannelCommand() {}

	@Override
	public String getName() {
		return "anime_channel";
	}

	@Override
	public String getUsage() {
		return "anime_channel <textchannel>";
	}

	@Override
	public void executeCommand(CommandHandler chi, MessageReceivedEvent event, String[] arguments) {
		String chan = arguments[0];
		Guild g = event.getGuild();
		List<TextChannel> tc = g.getTextChannelsByName(chan, false);
		if (!tc.isEmpty()) {
			TextChannel textC = tc.get(0);
			Jikai j = Core.JM.get(g);
			j.getJikaiData().setScheduleChannelId(textC.getIdLong());
			try {
				j.getInfoChannel().sendMessage(textC.getAsMention() + " has been set as the new anime channel!").queue();
			} catch (Exception e) {}
		}
	}

	@Override
	public String getIdentifier() {
		return "sacc";
	}

	@Override
	public Permission[] getRequiredPermissions() {
		return new Permission[] { Permission.MANAGE_CHANNEL, Permission.MANAGE_SERVER };
	}

	@Override
	public String getDescription() {
		return "The channel for anime release updates.";
	}
}
