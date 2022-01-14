
package com.github.xerragnaroek.jikai.commands.dev;

import com.github.xerragnaroek.jikai.commands.guild.GuildCommand;
import com.github.xerragnaroek.jikai.commands.user.JUCommand;
import com.github.xerragnaroek.jikai.jikai.JikaiIO;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.user.JikaiUser;

import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class ForceSaveCommand implements GuildCommand, JUCommand {

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

	@Override
	public void executeCommand(JikaiUser ju, String[] arguments) {
		JikaiIO.save(true);
	}
}
