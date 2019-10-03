package com.xerragnaroek.bot.commands;

import com.xerragnaroek.bot.core.Core;
import com.xerragnaroek.bot.util.BotUtils;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class RequestAssistanceCommand implements Command {

	@Override
	public String getName() {
		return "request_assistance";
	}

	@Override
	public void executeCommand(CommandHandler chi, MessageReceivedEvent event, String[] arguments) {
		TextChannel tc = event.getTextChannel();
		String devId = Core.DEV_ID;
		User author = event.getAuthor();
		if (devId == null) {
			tc.sendMessage("I'm sorry " + author.getAsMention() + " but whoever is hosting this bot didn't supply a developer id.").queue();
			return;
		} else {
			User dev = Core.JDA.getUserById(devId);
			if (dev == null) {
				tc.sendMessage("I'm sorry " + author.getAsMention() + " but whoever hosts this bot has supplied an invalid dev id.").queue();
			} else {
				Guild g = event.getGuild();
				BotUtils.sendPM(dev, String.format("%s from guild \"%s\"#%s has an issue:%n%s", author.getAsTag(), g.getName(), g.getId(), String.join(" ", arguments)));
				tc.sendMessage("A message has been sent to dev " + dev.getAsTag()).queue();
			}
		}
	}

	@Override
	public String getIdentifier() {
		return "rac";
	}

	@Override
	public String getUsage() {
		return "request_assistance <message>";
	}

	@Override
	public String getDescription() {
		return "Notifies the dev that you require assistance with your given issue.";
	}

	@Override
	public Permission[] getRequiredPermissions() {
		return CommandHandlerManager.MOD_PERMS;
	}

}
