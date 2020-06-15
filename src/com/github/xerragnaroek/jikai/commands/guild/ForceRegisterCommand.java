package com.github.xerragnaroek.jikai.commands.guild;

import com.github.xerragnaroek.jikai.user.JikaiUserManager;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class ForceRegisterCommand implements GuildCommand {

	@Override
	public String getName() {
		return "register";
	}

	@Override
	public void executeCommand(MessageReceivedEvent event, String[] arguments) {
		JikaiUserManager.getInstance().registerUser(event.getAuthor().getIdLong());
	}

	@Override
	public String getDescription() {
		return "Register with Jikai, this'll start the setup so you can subscribe to anime! No need to do it if you've already done it tho!";
	}

}
