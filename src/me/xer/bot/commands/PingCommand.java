package me.xer.bot.commands;

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class PingCommand implements Command {

	@Override
	public String getCommandName() {
		return "ping";
	}

	@Override
	public void executeCommand(MessageReceivedEvent event, String content) {
		MessageBuilder mb = new MessageBuilder();
		mb.append(event.getAuthor()).append(" Pong!");
		mb.sendTo(event.getChannel()).queue();
	}

	@Override
	public String getUsage() {
		return getCommandName();
	}

}
