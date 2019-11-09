package com.xerragnaroek.jikai.commands;

import com.xerragnaroek.jikai.jikai.JikaiIO;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class ForceSaveCommand implements Command {

	@Override
	public String getName() {
		return "force_save";
	}

	@Override
	public void executeCommand(CommandHandler chi, MessageReceivedEvent event, String[] arguments) {
		JikaiIO.save(true);
	}

	@Override
	public String getIdentifier() {
		return "fsc";
	}

	@Override
	public String getDescription() {
		return "Forces the immediate saving of everything.";
	}

}
