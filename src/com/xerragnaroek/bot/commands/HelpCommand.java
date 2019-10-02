package com.xerragnaroek.bot.commands;

import java.time.ZonedDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xerragnaroek.bot.data.GuildDataManager;
import com.xerragnaroek.bot.util.BotUtils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class HelpCommand implements Command {
	private final Logger log = LoggerFactory.getLogger(HelpCommand.class);

	@Override
	public String getName() {
		return "help";
	}

	@Override
	public void executeCommand(CommandHandlerImpl chi, MessageReceivedEvent event, String[] arguments) {
		log.info("Executing help command");
		Member m = event.getMember();
		/*StringBuilder bob = new StringBuilder();
		bob.append("Commands you " + m.getAsMention() + " have permissions to execute are:%n");
		CommandHandlerManager.getCommands().stream().filter(c -> CommandHandlerManager.checkPermissions(c, m))
				.forEach(com -> bob.append("%1$s" + com.getUsage() + "%n"));
		MessageBuilder mb = new MessageBuilder();
		mb.appendCodeBlock("My commands are:\n" + String.format(bob.toString(), chi.getTrigger()), "css");
		mb.sendTo(event.getChannel()).queue();*/
		String trigger = chi.getTrigger();
		EmbedBuilder eb = new EmbedBuilder();
		eb.setTitle("Commands you have permissions to use are:");
		CommandHandlerManager.getCommands().stream().filter(c -> CommandHandlerManager.checkPermissions(c, m))
				.forEach(com -> eb.addField("**" + trigger + com.getName() + "**", com.getDescription(), false));
		eb.setTimestamp(ZonedDateTime.now(GuildDataManager.getDataForGuild(event.getGuild()).getTimeZone()));
		BotUtils.sendPM(m.getUser(), eb.build());
	}

	@Override
	public String getIdentifier() {
		return "hec";
	}

	@Override
	public String getDescription() {
		return "This should never bee seen.";
	}

}
