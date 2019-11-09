package com.xerragnaroek.jikai.commands;

import com.xerragnaroek.jikai.jikai.Jikai;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class ForceUserUpdateCommand implements Command {

	@Override
	public String getName() {
		return "daily";
	}

	@Override
	public void executeCommand(CommandHandler chi, MessageReceivedEvent event, String[] arguments) {
		Jikai.getUserManager().getUser(event.getAuthor().getIdLong()).sendDailyUpdate();
	}

	@Override
	public String getIdentifier() {
		return "fuuc"; //lol
	}

	@Override
	public String getDescription() {
		return "Sends your daily anime overview.";
	}

}
