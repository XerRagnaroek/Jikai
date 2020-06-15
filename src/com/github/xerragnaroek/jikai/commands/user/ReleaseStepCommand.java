
package com.github.xerragnaroek.jikai.commands.user;

import com.github.xerragnaroek.jikai.user.JikaiUser;

/**
 * @author XerRagnaroek
 */
public class ReleaseStepCommand implements JUCommand {

	@Override
	public String getName() {
		return "notif_time";
	}

	@Override
	public String getAlternativeName() {
		return "nt";
	}

	@Override
	public String getDescription() {
		return "Add or remove times before the release of an anime where you'll be notfied.\nExample: `!notif_time add 2d,12h,45m` to add a step at 2 days, at 12 hours and at 45 minutes before a release.";
	}

	@Override
	public void executeCommand(JikaiUser ju, String[] arguments) {
		String tmp = arguments[1];
		if (!tmp.contains("d") && !tmp.contains("h") && !tmp.contains("m") && !tmp.contains(",")) {
			ju.sendPMFormat("'%s' isn't a valid format string! Seperate your times with a ',' and 'd' = days, 'h' = hours, 'm' = minutes. E.g. `1d,6h,30m`", tmp);
			return;
		}
		if (arguments[0].equals("add")) {
			ju.addReleaseSteps(tmp);

		} else if (arguments[0].equals("remove") || arguments[0].equals("rem")) {
			ju.removeReleaseSteps(tmp);
		}
	}

	@Override
	public String getUsage() {
		return "notif_time <add|remove> <steps> ";
	}
}
