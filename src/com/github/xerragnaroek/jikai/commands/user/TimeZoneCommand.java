
package com.github.xerragnaroek.jikai.commands.user;

import java.time.ZoneId;

import com.github.xerragnaroek.jikai.user.JikaiUser;

public class TimeZoneCommand implements JUCommand {

	@Override
	public String getName() {
		return "timezone";
	}

	@Override
	public String getAlternativeName() {
		return "tz";
	}

	@Override
	public String getDescription() {
		return "Your timezone. This effects when your notifcations are sent, so set it correctly!";
	}

	@Override
	public void executeCommand(JikaiUser ju, String[] arguments) {
		String id = arguments[0];
		try {
			ZoneId z = ZoneId.of(id);
			ju.setTimeZone(z);
		} catch (Exception e) {
			ju.sendPMFormat("'%s' isn't a known timezone.%nSee the column 'TZ database name' here: https://en.wikipedia.org/wiki/List_of_tz_database_time_zones", id);
		}
	}

	@Override
	public String getUsage() {
		return "timezone <zone id>";
	}

}
