
package com.github.xerragnaroek.jikai.commands.guild.dev;

import com.github.xerragnaroek.jikai.commands.guild.GuildCommand;
import com.github.xerragnaroek.jikai.jikai.JikaiIO;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;

import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class ForceSaveCommand implements GuildCommand {

	@Override
	public String getName() {
		return "force_save";
	}

	@Override
	public void executeCommand(GuildMessageReceivedEvent event, String[] arguments) {
		JikaiIO.save(true);
	}

	@Override
	public String getDescription(JikaiLocale loc) {
		return "Forces the immediate saving of everything.";
	}

	@Override
	public boolean isDevOnly() {
		return true;
	}

	@Override
	public String getLocaleKey() {
		return "";
	}
}
