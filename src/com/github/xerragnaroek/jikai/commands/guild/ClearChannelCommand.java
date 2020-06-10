package com.github.xerragnaroek.jikai.commands.guild;

import com.github.xerragnaroek.jikai.util.BotUtils;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/**
 * 
 */
public class ClearChannelCommand implements GuildCommand {

	@Override
	public String getName() {
		return "clear";
	}

	@Override
	public String getDescription() {
		return "Deletes ALL messages in the channel the command was called in!";
	}

	@Override
	public void executeCommand(MessageReceivedEvent event, String[] arguments) {
		BotUtils.clearChannel(event.getTextChannel());
	}

	@Override
	public Permission[] getRequiredPermissions() {
		return CommandHandler.MOD_PERMS;
	}
}
