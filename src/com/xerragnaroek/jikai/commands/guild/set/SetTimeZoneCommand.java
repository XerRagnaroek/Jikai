/*
 * MIT License
 *
 * Copyright (c) 2019 github.com/XerRagnaroek
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.xerragnaroek.jikai.commands.guild.set;

import java.time.ZoneId;

import com.xerragnaroek.jikai.commands.guild.GuildCommand;
import com.xerragnaroek.jikai.core.Core;
import com.xerragnaroek.jikai.jikai.Jikai;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class SetTimeZoneCommand implements GuildCommand {
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
	public void executeCommand(MessageReceivedEvent event, String[] arguments) {
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
	public Permission[] getRequiredPermissions() {
		return new Permission[] { Permission.MANAGE_SERVER };
	}

	@Override
	public String getDescription() {
		return "The TimeZone of this server, so the release updates are in your time. Default is \"Europe/Berlin\". Please refer to this [list](https://en.wikipedia.org/wiki/List_of_tz_database_time_zones), specifically the row \\\"TZ database name\\\"!.";
	}
}
