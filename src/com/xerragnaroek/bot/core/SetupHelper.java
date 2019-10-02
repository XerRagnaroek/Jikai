package com.xerragnaroek.bot.core;

import com.xerragnaroek.bot.data.GuildData;
import com.xerragnaroek.bot.data.GuildDataManager;

import net.dv8tion.jda.api.entities.Guild;

public class SetupHelper {

	public static void runSetup(Guild g) {
		String gId = g.getId();
		GuildData gd = GuildDataManager.registerNewGuild(g);
	}
}
