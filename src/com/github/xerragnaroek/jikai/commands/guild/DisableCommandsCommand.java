package com.github.xerragnaroek.jikai.commands.guild;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.jikai.Jikai;
import com.github.xerragnaroek.jikai.jikai.JikaiData;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class DisableCommandsCommand implements GuildCommand {
	private final Logger log = LoggerFactory.getLogger(DisableCommandsCommand.class);

	@Override
	public String getName() {
		return "disable_commands";
	}

	@Override
	public List<String> getAlternativeNames() {
		return Arrays.asList("dis_coms");
	}

	@Override
	public void executeCommand(GuildMessageReceivedEvent event, String[] arguments) {
		Guild g = event.getGuild();
		log.debug("Executing DisableCommandsCommand on guild {}#{}", g.getName(), g.getId());
		Jikai j = Core.JM.get(g);
		JikaiData jd = j.getJikaiData();
		if (jd.areCommandsEnabled()) {
			jd.setCommandsEnabled(false);
			try {
				j.getInfoChannel().sendMessage(j.getLocale().getStringFormatted("com_g_disable_com_msg", Arrays.asList("pre"), jd.getPrefix())).queue();
			} catch (Exception e) {}
		}
	}

	@Override
	public Permission[] getRequiredPermissions() {
		return new Permission[] { Permission.MANAGE_SERVER };
	}

	@Override
	public boolean isAlwaysEnabled() {
		return true;
	}

	@Override
	public String getLocaleKey() {
		return "com_g_disable_com";
	}

}
