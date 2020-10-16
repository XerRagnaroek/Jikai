
package com.github.xerragnaroek.jikai.commands.user;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.user.JikaiUser;

public class TimeZoneCommand implements JUCommand {

	@Override
	public String getName() {
		return "timezone";
	}

	@Override
	public List<String> getAlternativeNames() {
		return Arrays.asList("tz");
	}

	@Override
	public String getDescription(JikaiLocale loc) {
		return loc.getString("com_ju_tz_desc");
	}

	@Override
	public void executeCommand(JikaiUser ju, String[] arguments) {
		if (arguments.length > 0) {
			String id = arguments[0];
			JikaiLocale loc = ju.getLocale();
			try {
				ZoneId z = ZoneId.of(id);
				ju.setTimeZone(z);
				ju.sendPMFormat(loc.getStringFormatted("com_ju_tz_msg", Arrays.asList("tz"), z.getId()));
			} catch (Exception e) {
				ju.sendPMFormat(loc.getString("com_ju_tz_invalid"));
			}
		}
	}

	@Override
	public String getUsage(JikaiLocale loc) {
		return loc.getStringFormatted("com_ju_tz_use", Arrays.asList("com"), getName());
	}

}
