package com.github.xerragnaroek.jikai.commands.guild;

import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.util.BotUtils;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

/**
 * 
 */
public class ClearChannelCommand implements GuildCommand {

	@Override
	public String getName() {
		return "clear";
	}

	@Override
	public String getDescription(JikaiLocale loc) {
		return loc.getString("com_g_clear_desc");
	}

	@Override
	public void executeCommand(GuildMessageReceivedEvent event, String[] arguments) {
		BotUtils.clearChannel(event.getChannel());
	}

	@Override
	public Permission[] getRequiredPermissions() {
		return CommandHandler.MOD_PERMS;
	}
}
