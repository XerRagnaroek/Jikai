package com.xerragnaroek.bot.util;

import com.xerragnaroek.bot.core.Core;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;

public class BotUtils {

	public static TextChannel getChannelOrDefault(String channel, String gId) {
		TextChannel tc = null;
		Guild g = Core.getJDA().getGuildById(gId);
		if (channel != null) {
			tc = g.getTextChannelById(channel);
		}
		if (tc == null) {
			tc = g.getTextChannelsByName("general", true).get(0);
		}
		return tc;
	}
}
