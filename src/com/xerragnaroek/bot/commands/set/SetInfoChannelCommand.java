package com.xerragnaroek.bot.commands.set;

import java.util.List;

import com.xerragnaroek.bot.commands.Command;
import com.xerragnaroek.bot.commands.CommandHandler;
import com.xerragnaroek.bot.core.Core;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class SetInfoChannelCommand implements Command {

	@Override
	public String getName() {
		return "info_channel";
	}

	@Override
	public String getUsage() {
		return "info_channel <textchannel name>";
	}

	@Override
	public void executeCommand(CommandHandler chi, MessageReceivedEvent event, String[] arguments) {
		String chan = arguments[0];
		Guild g = event.getGuild();
		List<TextChannel> tc = g.getTextChannelsByName(chan, false);
		if (!tc.isEmpty()) {
			TextChannel textC = tc.get(0);
			Core.GDM.get(g.getId()).setInfoChannelId(textC.getId());
			textC.sendMessage("Channel for bot information messages set.\nFeel free to delete this message").queue();
		}
	}

	@Override
	public String getIdentifier() {
		return "sicc";
	}

	@Override
	public Permission[] getRequiredPermissions() {
		return new Permission[] { Permission.MANAGE_CHANNEL, Permission.MANAGE_SERVER };
	}

	@Override
	public String getDescription() {
		return "The channel for bot updates/status changes.";
	}
}
