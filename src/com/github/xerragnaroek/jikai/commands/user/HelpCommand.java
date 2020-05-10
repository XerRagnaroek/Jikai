
package com.github.xerragnaroek.jikai.commands.user;

import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.user.JikaiUser;

import net.dv8tion.jda.api.EmbedBuilder;

/**
 * @author XerRagnaroek
 *
 */
public class HelpCommand implements JUCommand {

	@Override
	public String getName() {
		return "help";
	}

	@Override
	public String getDescription() {
		return "help";
	}

	@Override
	public void executeCommand(JikaiUser ju, String[] arguments) {
		EmbedBuilder eb = new EmbedBuilder();
		boolean userIsDev = ju.getId() == Core.DEV_ID;
		JUCommandHandler.getCommands().stream().filter(com -> !com.isDevOnly() || userIsDev).forEach(com -> eb.addField("**!" + (com.hasUsage() ? com.getUsage() : com.getName()) + "**", com.getDescription(), false));
		ju.sendPM(eb.build());
	}

}
