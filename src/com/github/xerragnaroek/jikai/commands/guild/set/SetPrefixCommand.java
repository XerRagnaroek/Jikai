package com.github.xerragnaroek.jikai.commands.guild.set;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.xerragnaroek.jikai.commands.guild.GuildCommand;
import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.jikai.Jikai;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

/**
 * Command that changes the prefix String.
 * 
 * @author XerRagnarök
 */
public class SetPrefixCommand implements GuildCommand {
	private final Logger log = LoggerFactory.getLogger(this.getClass());

	SetPrefixCommand() {}

	@Override
	public String getName() {
		return "prefix";
	}

	@Override
	public void executeCommand(GuildMessageReceivedEvent event, String[] arguments) {
		String content = arguments[0];
		// only high ranking lads can use this command
		// pad it with a whitespace at the end so words can be used better: foo bar instead of foobar
		if (content.length() > 1) {
			content += " ";
		}
		if (content.length() >= 1) {
			Jikai j = Core.JM.get(event.getGuild());
			j.getJikaiData().setPrefix(content);
			try {
				j.getInfoChannel().sendMessage(j.getLocale().getStringFormatted("com_g_set_pre_msg", Arrays.asList("pre"), content)).queue();
			} catch (Exception e) {}
		}

	}

	@Override
	public Permission[] getRequiredPermissions() {
		return new Permission[] { Permission.MANAGE_SERVER };
	}

	@Override
	public List<String> getAlternativeNames() {
		return Arrays.asList("pre");
	}

	@Override
	public String getLocaleKey() {
		return "com_g_set_pre";
	}
}
