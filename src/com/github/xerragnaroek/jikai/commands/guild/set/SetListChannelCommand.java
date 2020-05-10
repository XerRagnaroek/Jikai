package com.github.xerragnaroek.jikai.commands.guild.set;

import java.util.List;

import com.github.xerragnaroek.jikai.commands.guild.GuildCommand;
import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.jikai.Jikai;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/**
 * Command that sets the channel the bot posts the role list in.
 * 
 * @author XerRagnarök
 *
 */
public class SetListChannelCommand implements GuildCommand {
	SetListChannelCommand() {}

	@Override
	public String getName() {
		return "list_channel";
	}

	@Override
	public String getUsage() {
		return "list_channel <textchannel>";
	}

	@Override
	public void executeCommand(MessageReceivedEvent event, String[] arguments) {
		String chan = arguments[0];
		Guild g = event.getGuild();
		List<TextChannel> tc = g.getTextChannelsByName(chan, false);
		if (!tc.isEmpty()) {
			TextChannel textC = tc.get(0);
			Jikai j = Core.JM.get(g);
			j.getJikaiData().setListChannelId(textC.getIdLong());
			try {
				j.getInfoChannel().sendMessage(textC.getAsMention() + " has been set as the new list channel!").queue();
			} catch (Exception e) {}
		}
	}

	@Override
	public Permission[] getRequiredPermissions() {
		return new Permission[] { Permission.MANAGE_CHANNEL, Permission.MANAGE_SERVER };
	}

	@Override
	public String getDescription() {
		return "The channel for the anime list.";
	}
}
