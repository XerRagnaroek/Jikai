package com.github.xerragnaroek.jikai.commands.guild.set;

import java.util.Arrays;
import java.util.List;

import com.github.xerragnaroek.jikai.commands.guild.GuildCommand;
import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.jikai.Jikai;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

/**
 * Command that sets the channel the bot posts automated stuff in.
 * 
 * @author XerRagnarök
 */
public class SetAnimeChannelCommand implements GuildCommand {

	SetAnimeChannelCommand() {}

	@Override
	public String getName() {
		return "anime_channel";
	}

	@Override
	public void executeCommand(GuildMessageReceivedEvent event, String[] arguments) {
		Guild g = event.getGuild();
		Jikai j = Core.JM.get(g);
		TextChannel textC = event.getChannel();
		if (arguments.length >= 1) {
			List<TextChannel> tcs = g.getTextChannelsByName(arguments[0], false);
			if (!tcs.isEmpty()) {
				textC = tcs.get(0);
			} else {
				textC.sendMessage(j.getLocale().getStringFormatted("com_g_set_ani_fail", Arrays.asList("channel"), arguments[0])).queue();
				return;
			}
		}
		j.getJikaiData().setAnimeChannelId(textC.getIdLong());
		try {
			j.getInfoChannel().sendMessage(j.getLocale().getStringFormatted("com_g_set_ani_success", Arrays.asList("channel"), textC.getAsMention())).queue();
		} catch (Exception e) {}
	}

	@Override
	public Permission[] getRequiredPermissions() {
		return new Permission[] { Permission.MANAGE_CHANNEL, Permission.MANAGE_SERVER };
	}

	@Override
	public List<String> getAlternativeNames() {
		return Arrays.asList("anime_chan", "a_chan", "ac");
	}

	@Override
	public String getLocaleKey() {
		return "com_g_set_ani";
	}
}
