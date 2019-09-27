package com.xerragnaroek.bot.commands;

import java.awt.Color;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class EmbedTestCommand implements Command {

	@Override
	public String getCommandName() {
		return "embed";
	}

	@Override
	public String getUsage() {
		return getCommandName();
	}

	@Override
	public void executeCommand(CommandHandlerImpl chi, MessageReceivedEvent event, String[] arguments) {
		EmbedBuilder eb = new EmbedBuilder();
		/*eb.setTitle("Monday").setColor(Color.red);
		for (int i = 0; i < 37; i++) {
			eb.addField("Anime #" + turnIntoEmoji(i), "", true);
		}
		event.getChannel().sendMessage(eb.build()).queue();*/
		eb.setTitle("Test").setColor(Color.MAGENTA);
		eb.addField("Test", "Test \n test on newline", false);
		eb.addField("Test2", "Test \n test on newline", false);
		event.getChannel().sendMessage(eb.build()).queue();
	}

	private String turnIntoEmoji(int i) {
		switch (i) {
		case 0:
			return ":zero:";
		case 1:
			return ":one:";
		case 2:
			return ":two:";
		case 3:
			return ":three:";
		case 4:
			return ":four:";
		case 5:
			return ":five:";
		case 6:
			return ":six:";
		case 7:
			return ":seven:";
		case 8:
			return ":eight:";
		case 9:
			return ":nine:";
		case 10:
			return ":keycap_ten:";
		default:
			return turnIntoRegionalIndicator(i);
		}
	}

	private String turnIntoRegionalIndicator(int i) {
		//i will be min 11, so we set re zero it
		i -= 11;
		//97 - 122
		int ch = 97 + i;
		char c = (char) ch;
		return ":regional_indicator_" + c + ":";
	}
}
