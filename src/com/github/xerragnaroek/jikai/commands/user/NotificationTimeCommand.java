
package com.github.xerragnaroek.jikai.commands.user;

import java.util.Arrays;
import java.util.List;

import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.user.JikaiUser;

/**
 * @author XerRagnaroek
 */
public class NotificationTimeCommand implements JUCommand {

	@Override
	public String getName() {
		return "notif_time";
	}

	@Override
	public List<String> getAlternativeNames() {
		return Arrays.asList("nt");
	}

	@Override
	public String getDescription(JikaiLocale loc) {
		return loc.getString("com_ju_notif_time_desc");
	}

	@Override
	public void executeCommand(JikaiUser ju, String[] arguments) {
		String tmp = arguments[1];
		if (!tmp.contains("d") && !tmp.contains("h") && !tmp.contains("m") && !tmp.contains(",")) {
			ju.sendPMFormat(ju.getLocale().getStringFormatted("com_ju_notif_time_invalid", Arrays.asList("input"), tmp));
			return;
		}
		if (arguments[0].equals("add")) {
			ju.addReleaseSteps(tmp);

		} else if (arguments[0].equals("remove") || arguments[0].equals("rem")) {
			ju.removeReleaseSteps(tmp);
		}
	}

	@Override
	public String getUsage(JikaiLocale loc) {
		return loc.getStringFormatted("com_ju_notif_time_use", Arrays.asList("com"), getName());
	}
}
