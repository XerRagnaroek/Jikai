package com.xerragnaroek.bot.commands.set;

import java.util.List;

import com.xerragnaroek.bot.commands.Command;
import com.xerragnaroek.bot.commands.CommandHandlerImpl;
import com.xerragnaroek.bot.data.GuildDataKey;
import com.xerragnaroek.bot.data.GuildDataManager;

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
	public String getCommandName() {
		return "anime_channel";
	}

	@Override
	public String getUsage() {
		return "anime_channel <textchannel>";
	}

	@Override
	public void executeCommand(CommandHandlerImpl chi, MessageReceivedEvent event, String[] arguments) {
		String chan = arguments[0];
		Guild g = event.getGuild();
		List<TextChannel> tc = g.getTextChannelsByName(chan, false);
		if (!tc.isEmpty()) {
			TextChannel textC = tc.get(0);
			GuildDataManager.getDataForGuild(g.getId()).set(GuildDataKey.ANIME_CHANNEL, textC.getId());
			textC.sendMessage("I'll post anime garbage in here.").queue();
		}
	}

}
