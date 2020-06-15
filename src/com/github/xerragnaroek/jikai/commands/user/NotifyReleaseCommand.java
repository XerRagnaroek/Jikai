
package com.github.xerragnaroek.jikai.commands.user;

import com.github.xerragnaroek.jikai.commands.ComUtils;
import com.github.xerragnaroek.jikai.jikai.locale.JikaiLocale;
import com.github.xerragnaroek.jikai.user.JikaiUser;

/**
 * @author XerRagnaroek
 */
public class NotifyReleaseCommand implements JUCommand {

	@Override
	public String getName() {
		return "notify_release";
	}

	@Override
	public String getAlternativeName() {
		return "nr";
	}

	@Override
	public String getDescription() {
		return "Enable/Disable the notification upon release of one of your subscribed anime.";
	}

	@Override
	public void executeCommand(JikaiUser ju, String[] arguments) {
		JikaiLocale loc = ju.getLocale();
		ComUtils.trueFalseCommand(arguments[0], ju, (b) -> {
			ju.setNotifyToRelease(b);
			ju.sendPM(b ? loc.getString("ju_daily_ov_true") : loc.getString("ju_daily_ov_false"));
		});
	}

	@Override
	public String getUsage() {
		return "notify_release <true|false>";
	}
}
