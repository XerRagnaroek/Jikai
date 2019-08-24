package me.xer.bot.commands.set;

import java.util.List;

import me.xer.bot.commands.Command;
import me.xer.bot.config.Config;
import me.xer.bot.config.ConfigOption;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/**
 * Command that sets the channel the bot posts automated stuff in.
 * 
 * @author XerRagnarök
 *
 */
public class SetChannelCommand implements Command {

	@Override
	public String getCommandName() {
		return "channel";
	}

	@Override
	public String getUsage() {
		return "";
	}

	@Override
	public void executeCommand(MessageReceivedEvent event, String arguments) {
		String chan = arguments;
		Guild g = event.getGuild();
		List<TextChannel> tc = g.getTextChannelsByName(chan, false);
		if (!tc.isEmpty()) {
			TextChannel textC = tc.get(0);
			Config.setOption(ConfigOption.CHANNEL, textC.getId());
			textC.sendMessage("I'll post anime garbage in here.").queue();
		}
	}

}
