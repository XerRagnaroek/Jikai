package com.xerragnaroek.jikai.commands.set;

import java.time.ZoneId;

import com.xerragnaroek.jikai.commands.Command;
import com.xerragnaroek.jikai.commands.CommandHandler;
import com.xerragnaroek.jikai.core.Core;
import com.xerragnaroek.jikai.data.Jikai;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class SetTimeZoneCommand implements Command {
	SetTimeZoneCommand() {}

	@Override
	public String getName() {
		return "timezone";
	}

	@Override
	public String getUsage() {
		return "timezone <zoneid> (tzdb ids)";
	}

	@Override
	public void executeCommand(CommandHandler chi, MessageReceivedEvent event, String[] arguments) {
		String zone = arguments[0];
		Jikai j = Core.JM.get(event.getGuild());
		try {
			ZoneId z = ZoneId.of(zone);
			j.getJikaiData().setTimeZone(z);
			j.getInfoChannel().sendMessage("Timezone has been changed to '" + z.getId() + "'").queue();
		} catch (Exception e) {
			EmbedBuilder eb = new EmbedBuilder();
			eb.setDescription(event.getAuthor().getAsMention() + " that's an invalid zoneid. \nPlease refer to this [list](https://en.wikipedia.org/wiki/List_of_tz_database_time_zones), specifically the row \"TZ database name\"!");
			event.getTextChannel().sendMessage(eb.build()).queue();
		}
	}

	@Override
	public String getIdentifier() {
		return "stzc";
	}

	@Override
	public Permission[] getRequiredPermissions() {
		return new Permission[] { Permission.MANAGE_SERVER };
	}

	@Override
	public String getDescription() {
		return "The TimeZone of this server, so the release updates are in your time. Default is \"Europe/Berlin\". Please refer to this [list](https://en.wikipedia.org/wiki/List_of_tz_database_time_zones), specifically the row \\\"TZ database name\\\"!.";
	}
}
