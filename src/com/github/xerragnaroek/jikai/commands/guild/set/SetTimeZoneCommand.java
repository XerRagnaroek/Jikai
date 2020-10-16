package com.github.xerragnaroek.jikai.commands.guild.set;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

import com.github.xerragnaroek.jikai.commands.guild.GuildCommand;
import com.github.xerragnaroek.jikai.core.Core;
import com.github.xerragnaroek.jikai.jikai.Jikai;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class SetTimeZoneCommand implements GuildCommand {
	SetTimeZoneCommand() {}

	@Override
	public String getName() {
		return "timezone";
	}

	@Override
	public String getUsage(JikaiLocale loc) {
		return loc.getStringFormatted("com_g_set_tz_use", Arrays.asList("com"), getName());
	}

	@Override
	public void executeCommand(GuildMessageReceivedEvent event, String[] arguments) {
		String zone = arguments[0];
		Jikai j = Core.JM.get(event.getGuild());
		try {
			ZoneId z = ZoneId.of(zone);
			j.getJikaiData().setTimeZone(z);
			j.getInfoChannel().sendMessage(j.getLocale().getStringFormatted("com_g_set_tz_success", Arrays.asList("tz"), z.getId())).queue();
		} catch (DateTimeException e) {
			event.getChannel().sendMessage(j.getLocale().getStringFormatted("com_g_set_tz_fail", Arrays.asList("user"), event.getAuthor().getAsMention())).queue();
		} catch (Exception e) {
			// infochannel doesn't exist, already handled in getInfoChannel()
		}
	}

	@Override
	public Permission[] getRequiredPermissions() {
		return new Permission[] { Permission.MANAGE_SERVER };
	}

	@Override
	public String getDescription(JikaiLocale loc) {
		return loc.getString("com_g_set_tz_desc");
	}

	@Override
	public List<String> getAlternativeNames() {
		return Arrays.asList("tz");
	}
}
