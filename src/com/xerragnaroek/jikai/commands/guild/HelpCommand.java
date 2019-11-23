/*
 * MIT License
 *
 * Copyright (c) 2019 github.com/XerRagnaroek
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.xerragnaroek.jikai.commands.guild;

import java.time.ZonedDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xerragnaroek.jikai.commands.ComUtils;
import com.xerragnaroek.jikai.core.Core;
import com.xerragnaroek.jikai.util.BotUtils;

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
