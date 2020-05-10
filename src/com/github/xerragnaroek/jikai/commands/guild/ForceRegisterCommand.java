package com.github.xerragnaroek.jikai.commands.guild;

import com.github.xerragnaroek.jikai.jikai.Jikai;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class ForceRegisterCommand implements GuildCommand {

	@Override
	public String getName() {
		return "register";
	}

	@Override
	public void executeCommand(MessageReceivedEvent event, String[] arguments) {
		Jikai.getUserManager().registerUser(event.getAuthor().getIdLong());
	}

	@Override
	public String getDescription() {
		return "Only for development purposes.";
	}

}
