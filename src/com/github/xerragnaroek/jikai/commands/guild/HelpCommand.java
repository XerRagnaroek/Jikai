
package com.github.xerragnaroek.jikai.commands.guild;

import java.time.ZonedDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jikai.commands.ComUtils;
import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.util.BotUtils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class HelpCommand implements GuildCommand {
	private final Logger log = LoggerFactory.getLogger(HelpCommand.class);

	@Override
	public String getName() {
		return "help";
	}

	@Override
	public void executeCommand(MessageReceivedEvent event, String[] arguments) {
		log.info("Executing help command");
		Member m = event.getMember();
		/*
		 * StringBuilder bob = new StringBuilder(); bob.append("Commands you " + m.getAsMention() +
		 * " have permissions to execute are:%n");
		 * CommandHandlerManager.getCommands().stream().filter(c ->
		 * CommandHandlerManager.checkPermissions(c, m)) .forEach(com -> bob.append("%1$s" +
		 * com.getUsage() + "%n")); MessageBuilder mb = new MessageBuilder();
		 * mb.appendCodeBlock("My commands are:\n" + String.format(bob.toString(),
		 * chi.getTrigger()), "css"); mb.sendTo(event.getChannel()).queue();
		 */
		String trigger = Core.JM.get(event.getGuild()).getCommandHandler().getTrigger();
		EmbedBuilder eb = new EmbedBuilder();
		eb.setTitle("Commands you have permissions to use are:");
		CommandHandler.getCommands().stream().filter(c -> ComUtils.checkPermissions(c, m)).forEach(com -> eb.addField("**" + trigger + (com.hasUsage() ? com.getUsage() : com.getName()) + "**", com.getDescription(), false));
		eb.setTimestamp(ZonedDateTime.now(Core.JM.get(event.getGuild()).getJikaiData().getTimeZone()));
		BotUtils.sendPM(m.getUser(), eb.build());
	}

	@Override
	public String getDescription() {
		return "This should never bee seen.";
	}

}
