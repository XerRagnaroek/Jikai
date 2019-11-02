package com.xerragnaroek.jikai.commands;

import com.xerragnaroek.jikai.data.Jikai;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class ForceRegisterCommand implements Command {

	@Override
	public String getName() {
		return "register";
	}

	@Override
	public void executeCommand(CommandHandler chi, MessageReceivedEvent event, String[] arguments) {
		Jikai.getUserManager().registerUser(event.getAuthor().getIdLong());
	}

	@Override
	public String getIdentifier() {
		return "frc";
	}

	@Override
	public String getDescription() {
		return "Only for development purposes.";
	}

}
