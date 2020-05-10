
package com.github.xerragnaroek.jikai.commands.guild;

import com.github.xerragnaroek.jikai.jikai.JikaiIO;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class ForceSaveCommand implements GuildCommand {

	@Override
	public String getName() {
		return "force_save";
	}

	@Override
	public void executeCommand(MessageReceivedEvent event, String[] arguments) {
		JikaiIO.save(true);
	}

	@Override
	public String getDescription() {
		return "Forces the immediate saving of everything.";
	}

}
