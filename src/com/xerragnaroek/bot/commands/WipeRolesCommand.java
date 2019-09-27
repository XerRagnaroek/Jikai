package com.xerragnaroek.bot.commands;

import java.util.concurrent.atomic.AtomicInteger;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class WipeRolesCommand implements Command {

	@Override
	public String getCommandName() {
		return "wipe";
	}

	@Override
	public String getUsage() {
		return "wipe";
	}

	@Override
	public void executeCommand(CommandHandlerImpl chi, MessageReceivedEvent event, String[] arguments) {
		Guild g = event.getGuild();
		Message m1 = event.getTextChannel().sendMessage("Deleting all anime roles...").complete();
		AtomicInteger count = new AtomicInteger(0);
		g.getRoles().stream().filter(r -> r.getPermissionsRaw() == 0l).peek(r -> count.incrementAndGet()).forEach(r -> r.delete().queue());
		m1.editMessageFormat("Deleted %d roles", count.get()).queue();
	}

}
