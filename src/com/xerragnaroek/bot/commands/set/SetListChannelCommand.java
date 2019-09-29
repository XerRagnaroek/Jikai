package com.xerragnaroek.bot.commands.set;

import java.util.List;

import com.xerragnaroek.bot.commands.Command;
import com.xerragnaroek.bot.commands.CommandHandlerImpl;
import com.xerragnaroek.bot.data.GuildDataManager;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/**
 * Command that sets the channel the bot posts the role list in.
 * 
 * @author XerRagnarök
 *
 */
public class SetListChannelCommand implements Command {
	SetListChannelCommand() {}

	@Override
	public String getCommandName() {
		return "list_channel";
	}

	@Override
	public String getUsage() {
		return "list_channel <textchannel>";
	}

	@Override
	public void executeCommand(CommandHandlerImpl chi, MessageReceivedEvent event, String[] arguments) {
		String chan = arguments[0];
		Guild g = event.getGuild();
		List<TextChannel> tc = g.getTextChannelsByName(chan, false);
		if (!tc.isEmpty()) {
			TextChannel textC = tc.get(0);
			GuildDataManager.getDataForGuild(g.getId()).setListChannelId(textC.getId());
		}
	}

}
