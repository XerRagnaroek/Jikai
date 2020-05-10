
package com.github.xerragnaroek.jikai.commands.user;

import com.github.xerragnaroek.jikai.commands.ComUtils;
import com.github.xerragnaroek.jikai.user.JikaiUser;

/**
 * @author XerRagnaroek
 *
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
		ComUtils.trueFalseCommand(arguments[0], ju, (b) -> {
			ju.setNotifyToRelease(b);
			ju.sendPM(b ? "You will recieve a daily overview of all your subscribed anime releasing on that day!" : "You won't recieve the daily overview.");
		});
	}

	@Override
	public String getUsage() {
		return "notify_release <true|false>";
	}
}
