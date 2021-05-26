
package com.github.xerragnaroek.jikai.commands.guild;

import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.util.BotUtils;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class RequestAssistanceCommand implements GuildCommand {

	@Override
	public String getName() {
		return "request_assistance";
	}

	@Override
	public void executeCommand(GuildMessageReceivedEvent event, String[] arguments) {
		TextChannel tc = event.getChannel();
		long devId = Core.DEV_IDS.get(0);
		User author = event.getAuthor();
		if (devId == 0) {
			tc.sendMessage("I'm sorry " + author.getAsMention() + " but whoever is hosting this bot didn't supply a developer id.").queue();
			return;
		} else {
			User dev = Core.JDA.getUserById(devId);
			if (dev == null) {
				tc.sendMessage("I'm sorry " + author.getAsMention() + " but whoever hosts this bot has supplied an invalid dev id.").queue();
			} else {
				Guild g = event.getGuild();
				BotUtils.sendPMChecked(dev, String.format("%s from guild \"%s\"#%s has an issue:%n%s", author.getAsTag(), g.getName(), g.getId(), String.join(" ", arguments)));
				tc.sendMessage("A message has been sent to dev " + dev.getAsTag()).queue();
			}
		}
	}

	@Override
	public String getUsage(JikaiLocale loc) {
		return "request_assistance <message>";
	}

	@Override
	public String getDescription(JikaiLocale loc) {
		return "Notifies the dev that you require assistance with your given issue.";
	}

	@Override
	public Permission[] getRequiredPermissions() {
		return CommandHandler.MOD_PERMS;
	}

	@Override
	public String getLocaleKey() {
		return "";
	}

}
