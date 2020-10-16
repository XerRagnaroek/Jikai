package com.github.xerragnaroek.jikai.commands.guild;

import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.user.JikaiUserManager;

import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class ForceRegisterCommand implements GuildCommand {

	@Override
	public String getName() {
		return "register";
	}

	@Override
	public void executeCommand(GuildMessageReceivedEvent event, String[] arguments) {
		long id = event.getAuthor().getIdLong();
		if (!JikaiUserManager.getInstance().isKnownJikaiUser(id)) {
			JikaiUserManager.getInstance().registerUser(id, Core.JM.get(event.getGuild().getIdLong()));
		}
	}

	@Override
	public String getDescription(JikaiLocale loc) {
		return loc.getString("com_g_register_desc");
	}

	@Override
	public boolean isJikaiUserOnly() {
		return false;
	}
}
